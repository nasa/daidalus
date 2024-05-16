/*
 * Data structure to keep special bands flags
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

public class SpecialBandFlags {
 
    /** 
     * Ground speed is below min air speed
     */
    public boolean below_min_as;

	/** 
     * Value of DTA status given current aircraft states. 
	 *  0 : Not in DTA 
	 * -1 : In DTA, but special bands are not enabled yet 
	 *  1 : In DTA and special bands are enabled 
	 */
    public int dta_status;

    public SpecialBandFlags() {
        below_min_as = false;
        dta_status = 0;
    }
    
    public void reset() {
        below_min_as = false;
        dta_status = 0;
    }

}
