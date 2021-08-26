/* 
 * Copyright (c) 2011-2021 United States Government as represented by
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

	/* 
	 * NONE: No band 
	 * FAR: Far conflict band
	 * MID: Mid conflict bands 
	 * NEAR: Near conflict band 
	 * RECOVERY: Band for violation recovery
	 * UNKNOWN : Invalid band
	 */

	UNKNOWN("UNKNOWN"), NONE("NONE"), FAR("FAR"), MID("MID"), NEAR("NEAR"), RECOVERY("RECOVERY");

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
	 * @return NONE: 0, FAR: 1, MID: 2, NEAR: 3, RECOVERY: 4, UNKNOWN: -1
	 */
	public static int orderOfRegion(BandsRegion region) {
		switch (region) {
		case NONE: return 0;
		case FAR: return 1;
		case MID: return 2;
		case NEAR: return 3;
		case RECOVERY: return 4;
		default: return -1;
		}
	}

	/**
	 * @return NONE/RECOVERY: 0, FAR: 1, MID: 2, NEAR: 3, UNKNOWN: -1
	 */
	public int orderOfConflictRegion() {
		if (isConflictBand()) {
			return orderOfRegion(this);
		}
		if (isResolutionBand()) {
			return 0;
		}
		return -1;
	}

	/**
	 * @return 0: NONE, 1: FAR, 2: MID, 3: NEAR, 4: RECOVERY, otherwise: UNKNOWN
	 */
	public static BandsRegion regionFromOrder(int i) {
		switch (i) {
		case 0: return NONE;
		case 1: return FAR;
		case 2: return MID;
		case 3: return NEAR;
		case 4: return RECOVERY;
		default: return UNKNOWN;
		}
	}

}
