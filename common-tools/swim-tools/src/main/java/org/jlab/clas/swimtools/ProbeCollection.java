/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.jlab.clas.swimtools;

import cnuphys.magfield.CompositeProbe;
import cnuphys.magfield.MagneticFields;
import cnuphys.magfield.RotatedCompositeProbe;
import cnuphys.swimS.SwimS;
import cnuphys.swimZ.SwimZ;

/**
 *
 * @author ziegler, heddle
 */
public class ProbeCollection {
    
    public final SwimS RCF_s;  //  rotated composite field 
    public final SwimS CF_s;   //  composite field 
    public final SwimZ RCF_z;  //  rotated composite field 
    public final SwimZ CF_z;   //  composite field 
    public final cnuphys.swim.Swimmer RCF;   //  rotated composite field 
    public final cnuphys.swim.Swimmer CF;    //  composite field 
    //Probes:
    public final RotatedCompositeProbe RCP;
    public final CompositeProbe CP; 
    
    public ProbeCollection() {
        RCP =   new RotatedCompositeProbe(MagneticFields.getInstance().getRotatedCompositeField());
        CP  =   new CompositeProbe(MagneticFields.getInstance().getCompositeField());
        
        RCF_s   =   new SwimS(MagneticFields.getInstance().getRotatedCompositeField());
        CF_s    =   new SwimS(MagneticFields.getInstance().getCompositeField());
        RCF_z   =   new SwimZ(MagneticFields.getInstance().getRotatedCompositeField());
        CF_z    =   new SwimZ(MagneticFields.getInstance().getCompositeField());
        RCF     =   new cnuphys.swim.Swimmer(MagneticFields.getInstance().getRotatedCompositeField());
        CF      =   new cnuphys.swim.Swimmer(MagneticFields.getInstance().getCompositeField());
    }
}
