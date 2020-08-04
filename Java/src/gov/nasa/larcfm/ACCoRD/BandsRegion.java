/* 
 * Copyright (c) 2011-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

/**
 * This enumeration provides constants to indicate the type of a
 * region of conflict prevention information.
 */
public enum BandsRegion { 
	/* UNKNOWN : Invalid band
	 * NONE: No band 
	 * RECOVERY: Band for violation recovery
	 * NEAR: Near conflict band 
	 * MID: Mid conflict bands 
	 * FAR: Far conflict band
	 */

	UNKNOWN("UNKNOWN"), NONE("NONE"), RECOVERY("RECOVERY"), NEAR("NEAR"), MID("MID"), FAR("FAR");

	// Number of conflict bands (NEAR, MID, FAR)
	public static final int NUMBER_OF_CONFLICT_BANDS = 3;

	private String name;

	private BandsRegion(String nm) {
		name = nm;
	}

	public String toString() {
		return name;
	}

	/**
	 * @return true if region is not UNKNOWN
	 */
	public boolean isValidBand() {
		return this != UNKNOWN;
	}

	/**
	 * @return true if region is NONE or RECOVERY
	 */
	public boolean isResolutionBand() {
		return this == NONE || this == RECOVERY;
	}

	/**
	 * @return true if region is NEAR, MID, or FAR
	 */
	public boolean isConflictBand() {
		return isValidBand() && !isResolutionBand();  
	}

	/**
	 * @return FAR -> 1, MID -> 2, NEAR -> 3, otherwise -> -1
	 */
	public int orderOfConflictRegion() {
		if (isResolutionBand()) {
			return 0;
		}
		if (this == FAR) {
			return 1;
		}
		if (this == MID) {
			return 2;
		}
		if (this == NEAR) {
			return 3;
		}
		return -1;
	}

	/**
	 * @return 1 -> FAR, 2 -> MID, 3 -> NEAR, otherwise -> UNKNOWN
	 */
	public static BandsRegion conflictRegionFromOrder(int i) {
		switch (i) {
		case 1: return FAR;
		case 2: return MID;
		case 3: return NEAR;
		default: return UNKNOWN;
		}
	}

}
