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
 
	private boolean below_min_as_;
	/* Cached value of DTA status given current aircraft states. 
	 *  0 : Not in DTA 
	 * -1 : In DTA, but special bands are not enabled yet 
	 *  1 : In DTA and special bands are enabled 
	 */
	private int dta_status_;
	/* Cached lists of aircraft indices, alert_levels, and lookahead times sorted by indices, contributing to conflict (non-peripheral) 
	 * band listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR 
	 */

    public SpecialBandFlags(boolean below_min_as, int dta_status) {
        below_min_as_ = below_min_as;
        dta_status_ = dta_status;
    }

    public boolean get_below_min_as() {
        return below_min_as_;
    }

    public int get_dta_status() {
        return dta_status_;
    }

}
