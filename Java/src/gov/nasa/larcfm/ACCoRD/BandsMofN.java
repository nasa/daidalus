/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.f;

/*
 * A value point with MofNs to the left and to the right.
 */

public class BandsMofN {

	public double val;
	public MofN colors_left;
	public MofN colors_right;

	public BandsMofN(ColorValue cv, int m, int n) {
		val = cv.val;
		int code = BandsRegion.orderOfRegion(cv.color_left);
		colors_left = new MofN(m,n,code);
		code = BandsRegion.orderOfRegion(cv.color_right);
		colors_right = new MofN(m,n,code);
	}

	public BandsMofN(double value, MofN mofn) {
		val = value;
		colors_left = new MofN(mofn);
		colors_right = new MofN(mofn);
	}

	public BandsRegion left_m_of_n(BandsRegion region) {
		int code = colors_left.m_of_n(BandsRegion.orderOfRegion(region));
		return BandsRegion.regionFromOrder(code);
	}

	public BandsRegion right_m_of_n(BandsRegion region) {
		int code = colors_right.m_of_n(BandsRegion.orderOfRegion(region));
		return BandsRegion.regionFromOrder(code);
	}

	public boolean same_colors() {
		return colors_left.sameAs(colors_right);
	}
	
	public String toString() {
		String s = "<"+colors_left.toString()+", "+f.FmPrecision(val)+", "+colors_right.toString()+">";
		return s;
	}

}
