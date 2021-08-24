/*
> * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;

/* Vertical Well Clear Volume concept */

public interface WCV_Vertical {

	// Vertical Not Well Clear Violation
	// ZTHR and TTHR are altitude and time thresholds 

	public boolean vertical_WCV(double ZTHR, double TCOA, double sz, double vz);

	public Interval vertical_WCV_interval(double ZTHR, double TTHR, double B, double T, double sz, double vz);

	/**
	 * Returns a deep copy of this WCV_Vertical object, including any results that have been calculated.  
	 * This will duplicate parameter data, but will NOT
	 * reference any external objects -- their data, if any, will be copied as well.
	 */
	public WCV_Vertical copy();

}
