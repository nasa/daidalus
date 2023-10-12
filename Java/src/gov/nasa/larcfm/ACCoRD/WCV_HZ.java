/*
 * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

/* Horizontal Well Clear Volume concept based on Modified TAU,
 * and, in the vertical dimension, VMOD
 */

public class WCV_HZ extends WCV_TAUMOD {

	/** Constructor that a default instance of the WCV tables. */
	public WCV_HZ() {
		super(new WCV_VMOD());
	}

	public WCV_HZ(WCV_HZ wcv) {
		super(wcv);
	}  

	/**
	 * One static WCV_HZ
	 */
	public static final WCV_HZ A_WCV_HZ =
			new WCV_HZ();

	public WCV_HZ make() {
		return new WCV_HZ();
	}

	/**
	 * Returns a deep copy of this WCV_HZ object, including any results that have been calculated.  
	 */
	public WCV_HZ copy() {
		return new WCV_HZ(this);
	}

	public boolean contains(Detection3D cd) {
		if (cd instanceof WCV_HZ) {
			return containsTable((WCV_tvar)cd);
		}
		return false;
	}

}
