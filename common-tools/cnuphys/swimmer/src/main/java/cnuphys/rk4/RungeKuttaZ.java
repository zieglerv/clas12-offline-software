package cnuphys.rk4;

/**
 * Integrators used by the Z swimmer
 * @author heddle
 *
 */

import java.util.List;

/**
 * Static methods for Runge-Kutta 4 integration, including a constant stepsize
 * method and an adaptive stepsize method. This is used by SwimZ
 * 
 * @author heddle
 * 
 */
public class RungeKuttaZ {

	// for adaptive stepsize, this is how much h will grow
	private static final double HGROWTH = 1.5;

	//think in cm
	public static double DEFMINSTEPSIZE = 1.0e-3;
	public static double DEFMAXSTEPSIZE = 40;
	
	private double _minStepSize = DEFMINSTEPSIZE;
	private double _maxStepSize = DEFMAXSTEPSIZE;

	// the dimension is 4 [x, y, tx, ty]
	private static int DIM = 4; // we'll know if this fails!
	
	double yt[] = new double[DIM];
	double yt2[] = new double[DIM];
	double dydt[] = new double[DIM];
	double error[] = new double[DIM];

	
	// use a simple half-step advance
	private IAdvance _advancer = new HalfStepAdvance();
	
	/**
	 * Create a RungeKutta object that can be used for integration
	 */
	public RungeKuttaZ() {
	}


	/**
	 * Integrator that uses the RungeKutta advance with a HalfStep and
	 * adaptive stepsize and a tolerance vector.
	 * 	 * 
	 * @param yo
	 *            initial values. (xo, yo, txo, tyo).
	 * @param zo
	 *            the initial value of the independent variable z.
	 * @param zf
	 *            the maximum value of the independent variable z.
	 * @param h
	 *            the starting steps size
	 * @param z
	 *            a list of the values of z at each step
	 * @param y
	 *            a list of the values of the state vector at each step
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param relTolerance
	 *            the error tolerance as fractional diffs. Note it is a vector,
	 *            the same dimension of the problem, i.e. 4
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throws RungeKuttaException
	 */
	public int adaptiveStepZoZf(double yo[],
			double zo,
			double zf,
			double h,
			final List<Double> z,
			final List<double[]> y,
			IDerivative deriv,
			IStopper stopper,
			double relTolerance[],
			double hdata[]) throws RungeKuttaException {

		// put starting step in
		z.add(zo);
		y.add(copy(yo));

		IRkListener listener = new IRkListener() {

			@Override
			public void nextStep(double tNext, double yNext[], double h) {
				z.add(tNext);
				y.add(copy(yNext));
			}

		};
		return adaptiveStepZoZf(yo, zo, zf, h, deriv, stopper, listener, relTolerance, hdata);
	}


	

	/**
	 * Integrator that uses the RungeKutta advance with a Half Step advancer and
	 * adaptive stepsize
	 * 
	 * This version uses an IRk4Listener to notify the listener that the next
	 * step has been advanced.
	 * 
	 * @param yo
	 *            initial values. (xo, yo, tx0, ty0)
	 * @param zo
	 *            the initial value of the independent variable
	 * @param zf
	 *            the maximum value of the independent variable.
	 * @param h
	 *            the starting steps size
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param listener
	 *            listens for each step * @param tableau the Butcher Tableau
	 * @param relTolerance
	 *            the error tolerance as fractional diffs. Note it is a vector,
	 *            the same
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throws RungeKuttaException
	 */
	public int adaptiveStepZoZf(double yo[],
			double zo, double zf, double h, IDerivative deriv, IStopper stopper, IRkListener listener,
			double relTolerance[], double hdata[]) throws RungeKuttaException {

		// use a simple half-step advance
	//	IAdvance advancer = new ButcherTableauAdvance(ButcherTableau.FEHLBERG_ORDER5);

		int nStep = 0;
		try {
			nStep = driverZoZf(yo, zo, zf, h, deriv, stopper, listener, _advancer, relTolerance, hdata);
		} catch (RungeKuttaException e) {
//			System.err.println("Trying to integrate from " + to + " to " + tf);
			throw e;
		}
		return nStep;
	}


	// copy a vector
	private double[] copy(double v[]) {
		double w[] = new double[v.length];
		System.arraycopy(v, 0, w, 0, v.length);

		// for (int i = 0; i < v.length; i++) {
		// w[i] = v[i];
		// }
		return w;
	}

	/**
	 * Driver that uses the RungeKutta advance with an adaptive step size
	 * 
	 * This version uses an IRk4Listener to notify the listener that the next
	 * step has been advanced.
	 * 
	 * A very typical case is a 2nd order ODE converted to a 1st order where the
	 * dependent variables are x, y, z, vx, vy, vz and the independent variable
	 * is time.
	 * 
	 * @param yo
	 *            initial values. Probably something like (xo, yo, zo, vxo, vyo,
	 *            vzo).
	 * @param zo
	 *            the initial value of the independent variable, e.g., time.
	 * @param zf
	 *            the maximum value of the independent variable.
	 * @param h
	 *            the step size
	 * @param deriv
	 *            the derivative computer (interface). This is where the problem
	 *            specificity resides.
	 * @param stopper
	 *            if not <code>null</code> will be used to exit the integration
	 *            early because some condition has been reached.
	 * @param listener
	 *            listens for each step
	 * @param advancer
	 *            takes the next step
	 * @param absError
	 *            the absolute tolerance for eact of the state variables. Note
	 *            it is a vector, the same
	 * @param hdata
	 *            if not null, should be double[3]. Upon return, hdata[0] is the
	 *            min stepsize used, hdata[1] is the average stepsize used, and
	 *            hdata[2] is the max stepsize used
	 * @return the number of steps used.
	 * @throw(new RungeKuttaException("Step size too small in Runge Kutta
	 *            driver" ));
	 */
	private int driverZoZf(double yo[],
			double zo,
			double zf,
			double h,
			IDerivative deriv,
			IStopper stopper,
			IRkListener listener,
			IAdvance advancer,
			double absError[],
			double hdata[]) throws RungeKuttaException {

		//going in the normal direction?
		boolean normalDir = (zf > zo);
		
		// if our advancer does not compute error we can't use adaptive stepsize
		if (!advancer.computesError()) {
			return 0;
		}

		// capture stepsize data?
		if (hdata != null) {
			hdata[0] = h;
			hdata[1] = h;
			hdata[2] = h;
		}

		// the dimensionality of the problem
		int nDim = 4;

		double z = zo;
		for (int i = 0; i < nDim; i++) {
			yt[i] = yo[i];
		}

		int nstep = 0;
		boolean keepGoing = true;

		while (keepGoing) {
			// use derivs at previous t
			deriv.derivative(z, yt, dydt);
			// System.out.println("curr y: [" + yt[0] + ", " + yt[1] + "]");

			//we might be going backwards
			double newZ = (normalDir ? z + h: z - h);

			int oldSign = ((zf - z) < 0) ? -1 : 1;
			int newSign = ((zf - newZ) < 0) ? -1 : 1;

			if (oldSign != newSign) { // crossed zf
				h = Math.abs(zf - z);  //h always positive
				keepGoing = false;
			}

			if (normalDir) {
				advancer.advance(z, yt, dydt, h, deriv, yt2, error);
			} else {
				advancer.advance(z, yt, dydt, -h, deriv, yt2, error);
			}

			boolean decreaseStep = false;
			if (keepGoing) {
				for (int i = 0; i < nDim; i++) {
					decreaseStep = error[i] > absError[i];
					if (decreaseStep) {
						break;
					}
				}
			}

			if (decreaseStep) {
				h = h / 2;
				if (h < _minStepSize) {
					keepGoing = false;
				}
			}
			else { // accepted this step

				if (hdata != null) {
					hdata[0] = Math.min(hdata[0], h);
					hdata[1] += h;
					hdata[2] = Math.max(hdata[2], h);
				}

				for (int i = 0; i < nDim; i++) {
					yt[i] = yt2[i];
				}

				if (normalDir) {
					z += h;
				}
				else {
					z -= h;
				}
				
				nstep++;

				// someone listening?
				if (listener != null) {
					listener.nextStep(z, yt, h);
				}

				// premature termination? Skip if stopper is null.
				if (stopper != null) {
					stopper.setFinalT(z);
					if (stopper.stopIntegration(z, yt)) {
						if ((hdata != null) && (nstep > 0)) {
							hdata[1] = hdata[1] / nstep;
						}
						return nstep; // actual number of steps taken
					}
				}
				h *= HGROWTH;
				h = Math.min(h, _maxStepSize);

			} // accepted this step max error < tolerance
		} // while (keepgoing)

		// System.err.println("EXCEEDED MAX PATH: pl = " + t + " MAX: " + tf);

		if ((hdata != null) && (nstep > 0)) {
			hdata[1] = hdata[1] / nstep;
		}
		return nstep;
	}


//	// a Butcher Tableau advancer
//	class ButcherTableauAdvance implements IAdvance {
//
//		private ButcherTableau tableau;
//
//		public ButcherTableauAdvance(ButcherTableau tableau) {
//			this.tableau = tableau;
//		}
//
//		@Override
//		public void advance(double t,
//				double[] y,
//				double[] dydt,
//				double h,
//				IDerivative deriv,
//				double[] yout,
//				double[] error) {
//
//			// System.err.println("TABLEAU ADVANCE");
//			int nDim = y.length;
//			int numStage = tableau.getS();
//
//			double ytemp[] = getWorkArrayFromCache();
//			double k[][] = new double[numStage + 1][];
//			k[0] = null; // not used
//
//			// k1 is just h*dydt
//			k[1] = getWorkArrayFromCache();
//			for (int i = 0; i < nDim; i++) {
//				k[1][i] = h * dydt[i];
//			}
//
//			// fill the numStage k vectors
//			for (int s = 2; s <= numStage; s++) {
//				k[s] = getWorkArrayFromCache();
//
//				double ts = t + tableau.c(s);
//				for (int i = 0; i < nDim; i++) {
//					ytemp[i] = y[i];
//					for (int ss = 1; ss < s; ss++) {
//						ytemp[i] += tableau.a(s, ss) * k[ss][i];
//					}
//				}
//				deriv.derivative(ts, ytemp, k[s]);
//				for (int i = 0; i < nDim; i++) {
//					k[s][i] *= h;
//				}
//			}
//
//			for (int i = 0; i < nDim; i++) {
//				double sum = 0.0;
//				for (int s = 1; s <= numStage; s++) {
//					sum += tableau.b(s) * k[s][i];
//				}
//				yout[i] = y[i] + sum;
//			}
//
//			// compute error?
//
//			if (tableau.isAugmented() && (error != null)) {
//
//				// absolute error
//				for (int i = 0; i < nDim; i++) {
//					error[i] = 0.0;
//					// error diff 4th and 5th order
//					for (int s = 1; s <= numStage; s++) {
//						error[i] += tableau.bdiff(s) * k[s][i]; // abs error
//					}
//				}
//
//				// relative error
//				// for (int i = 0; i < nDim; i++) {
//				// double sum = 0.0;
//				// for (int s = 1; s <= numStage; s++) {
//				// sum += tableau.bstar(s)*k[s][i];
//				// }
//				// double ystar = y[i] + sum;
//				// error[i] = relativeDiff(yout[i], ystar);
//				// }
//
//				// for (int i = 0; i < nDim; i++) {
//				// System.out.print(String.format("[%-12.5e] ", error[i]));
//				// }
//				// System.out.println();
//
//			}
//
//			// //return the work arrays
//			_workArrayCache.push(ytemp);
//			for (int s = 1; s <= numStage; s++) {
//				_workArrayCache.push((k[s]));
//			}
//		}
//
//		@Override
//		public boolean computesError() {
//			return tableau.isAugmented();
//		}
//
//	}

	/**
	 * Set the maximum step size
	 * 
	 * @param maxSS
	 *            the maximum stepsize is whatever units you are using
	 */
	public void setMaxStepSize(double maxSS) {
		_maxStepSize = maxSS;
	}

	/**
	 * Set the minimum step size
	 * 
	 * @param maxSS
	 *            the minimum stepsize is whatever units you are using
	 */
	public void setMinStepSize(double minSS) {
		_minStepSize = minSS;
	}

	/**
	 * Get the maximum step size
	 * 
	 * @return the maximum stepsize is whatever units you are using
	 */
	public double getMaxStepSize() {
		return _maxStepSize;
	}
	
	/**
	 * Get the minimum step size
	 * 
	 * @return the minimum stepsize is whatever units you are using
	 */
	public double getMinStepSize() {
		return _minStepSize;
	}


}
