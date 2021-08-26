/**
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class BandsRange {
	public Interval    interval;
	public BandsRegion region;

	public BandsRange(Interval i, BandsRegion r) {
		interval = i;
		region = r;
	}

	// NONE's when in recovery, become RECOVERY
	private static BandsRegion resolution_region(BandsRegion region, boolean recovery) {
		if (region.isResolutionBand()) {
			return recovery ? BandsRegion.RECOVERY : BandsRegion.NONE;
		}
		return region;
	}

	/*
	 * Transforms a list of color values into a list of BandRanges. 
	 * This function avoids adding color points where the left and right colors are the same.
	 */
	public static void makeRangesFromColorValues(List<BandsRange> ranges, List<ColorValue> l, boolean recovery) { 
		ranges.clear();
		int next=0;
		for (int i = 1; i < l.size(); ++i) {
			BandsRegion color_left = resolution_region(l.get(i).color_left,recovery);
			BandsRegion color_right = resolution_region(l.get(i).color_right,recovery);
			if (color_left != color_right || i == l.size()-1) {
				ranges.add(new BandsRange(new Interval(l.get(next).val,l.get(i).val),color_left));
				next = i;
			}
		}
	}

	/** 
	 * Return index in ranges_ where val is found, -1 if invalid input or not found. Notice that values at
	 * the limits may be included or not depending on the regions. 
	 */
	public static int index_of(List<BandsRange> ranges, double val, double mod) {
		val = Util.safe_modulo(val,mod);
		int last_index = ranges.size()-1;
		// In the following loop is important that RECOVERY and NONE both have the order 0, 
		// since they represent close intervals.
		// For that reason, orderOfConflictRegion is used instead of orderOfRegion
		for (int i=0; i <= last_index; ++i) {
			if (ranges.get(i).interval.almost_in(val,true,true,DaidalusParameters.ALMOST_) ||
					(i==last_index && 	
					Util.almost_equals(Util.safe_modulo(ranges.get(i).interval.up-val,mod),0,
							DaidalusParameters.ALMOST_))) {
				if (ranges.get(i).region.isResolutionBand()) {
					return i;
				} else if (Util.almost_equals(val,ranges.get(i).interval.low,DaidalusParameters.ALMOST_)) {
					int prev_index = -1;
					if (i > 0) {
						prev_index = i-1;
					} else if (mod > 0) {
						prev_index = last_index;
					}
					if (prev_index > 0 && ranges.get(i).region.isValidBand()) {
						return !ranges.get(prev_index).region.isValidBand() ||
								ranges.get(i).region.orderOfConflictRegion() <= 
								ranges.get(prev_index).region.orderOfConflictRegion() ? i : prev_index;
					}
					return prev_index;
				} else if (Util.almost_less(val,ranges.get(i).interval.up,DaidalusParameters.ALMOST_)) {
					return i;
				} else {
					// val is equal to upper bound
					int next_index = -1;
					if (i < last_index) {
						next_index = i+1;
					} else if (mod > 0) {
						next_index = 0;
					}
					if (next_index > 0 && ranges.get(i).region.isValidBand()) {
						return !ranges.get(next_index).region.isValidBand() ||
								ranges.get(i).region.orderOfConflictRegion() <= 
								ranges.get(next_index).region.orderOfConflictRegion() ? i : next_index;
					}
					return next_index;
				}
			} 
		}
		return -1;
	}

	public String toString() {
		String s = "";
		s+=interval.toString()+" "+region;
		return s;
	}

	public String toPVS() {
		String s = "";
		s += "(# lb:= "+f.FmPrecision(interval.low)+
				", ub:= "+f.FmPrecision(interval.up)+
				", region:= "+region+" #)";
		return s;
	}


}

