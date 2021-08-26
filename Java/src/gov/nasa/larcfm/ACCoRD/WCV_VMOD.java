/*
> * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.Util;

/* Non-Hazard Zone VMOD concept */

public class WCV_VMOD implements WCV_Vertical {
	
  // Vertical Not Well Clear Violation
  // ZTHR and T_star are altitude and time thresholds 
  public boolean vertical_WCV(double ZTHR, double T_star, double sz, double vz) {
    return Math.abs(sz) <= ZTHR ||
        (!Util.almost_equals(vz,0) && sz*vz <= 0 && 
        Math.abs(sz) <= ZTHR + Math.abs(vz)*T_star); // [CAM] Changed from != to !almost_equals to mitigate numerical problems 
  }

  // ZTHR and T_star are altitude and time thresholds   
  public Interval vertical_WCV_interval(double ZTHR, double T_star, double B, double T, double sz, double vz) {
    double time_in = B;
    double time_out = T;
    if (Util.almost_equals(vz,0) && Math.abs(sz) <= ZTHR) // [CAM] Changed from == to almost_equals to mitigate numerical problems
      return new Interval(time_in,time_out);
    if (Util.almost_equals(vz,0)) { // [CAM] Changed from == to almost_equals to mitigate numerical problems
      time_in = T;   
      time_out = B;   
      return new Interval(time_in,time_out);
    }   
    double act_H = Util.max(ZTHR,ZTHR -Util.sign(sz*vz)* Math.abs(vz)*T_star);
    double tentry = Vertical.Theta_H(sz,vz,-1,act_H);
    double texit = Vertical.Theta_H(sz,vz,1,ZTHR);
    if (T < tentry || texit < B) {
      time_in = T;
      time_out = B;
      return new Interval(time_in,time_out);
    }
    time_in = Util.max(B,tentry);
    time_out = Util.min(T,texit);
    return new Interval(time_in,time_out);
  }
	/**
	 * Returns a deep copy of this WCV_Vertical object, including any results that have been calculated.  
	 * This will duplicate parameter data, but will NOT
	 * reference any external objects -- their data, if any, will be copied as well.
	 */
	public WCV_VMOD copy() {
		return new WCV_VMOD();
	}

}
