/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.IntervalSet;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

import java.util.List;

/*
 * A color value if a point in a line that has a color to the left and a color to the right.
 * A list of color values is a sorted list, without repetition of values, that is consistent 
 * in the sense that two consecutive values has the same interior color, i.e., if v1 < v2, then 
 * right color of v1 is the same as left color of v2. When a color interval is inserted in a 
 * color value list, a color can only override a weaker color. Unknown color cannot be overridden. 
 */

public class ColorValue {
	public double val;
	public BandsRegion color_left;
	public BandsRegion color_right;

	public ColorValue(BandsRegion l, double v, BandsRegion r) {
		val = v;
		color_left = l;
		color_right = r;
	}

	public String toString() {
		String s = "<"+color_left+", "+f.FmPrecision(val)+", "+color_right+">";
		return s;
	}

	/*
	 * Initialize a list of color values, with min < max values, and interior color.
	 * Initial list is has two color values: (unknown,min,int_color) and
	 * (int_color,max,unknown). 
	 */
	public static void init(List<ColorValue> l, double min, double max, BandsRegion int_color) {
		init(l,min,max,min,max,int_color);
	}

	/*
	 * Initialize a list of color values, with min <= min_val < max_val <= max values, and interior color.
	 * Initial list is has at most four color values: (unknown,min,unknown),
	 * (unknown,min_val,int_color),(int_color,max_val,unknown), and
	 * (unknown,max,unknown). 
	 */
	public static void init(List<ColorValue> l, double min, double max, double min_val, double max_val, BandsRegion int_color) {
		l.clear();
		if (Util.almost_less(min,min_val,DaidalusParameters.ALMOST_)) {
			l.add(new ColorValue(BandsRegion.UNKNOWN,min,BandsRegion.UNKNOWN));
		}
		l.add(new ColorValue(BandsRegion.UNKNOWN,min_val,int_color));
		l.add(new ColorValue(int_color,max_val,BandsRegion.UNKNOWN));
		if (Util.almost_less(max_val,max,DaidalusParameters.ALMOST_)) {
			l.add(new ColorValue(BandsRegion.UNKNOWN,max,BandsRegion.UNKNOWN));
		}
	}

	/*
	 * Initialize a list of color values with two color values:
	 * (int_color,0,int_color), (int_color,mod,int_color)
	 */
	public static void init_with_mod(List<ColorValue> l, double mod, BandsRegion int_color) {
		l.clear();
		l.add(new ColorValue(int_color,0,int_color));
		l.add(new ColorValue(int_color,mod,int_color));
	}

	/*
	 * Initialize a list of color values, with min != max, and 0 <= min < max <= mod values, 
	 * and interior color. 
	 * If min = max: Initial list has two color values (unknown,0,int_color) and  (int_color,max,unknown)
	 * If min < max: Initial list has four color values: (unknown,0,unknown),
	 * (unknown,min,int_color), (int_color,max,unknown), and (unknown,mod,unknown).
	 * If min > max: Initial list is has four color values: (unknown,0,int_color),
	 * (int_color,max,unknown), (unknown,min,int_color), and (int_color,mod,unknown).
	 */
	public static void init_with_mod(List<ColorValue> l, double min, double max, double mod, BandsRegion int_color) {
		if (mod > 0 && Util.almost_equals(Util.modulo(max-min,mod),0,DaidalusParameters.ALMOST_)) {
			init_with_mod(l,mod,int_color);
		} else {
			l.clear();
			if (Util.almost_equals(min,max,DaidalusParameters.ALMOST_)) {
				l.add(new ColorValue(BandsRegion.UNKNOWN,0,int_color));
				l.add(new ColorValue(int_color,mod,BandsRegion.UNKNOWN));
			} else if (min < max) {
				if (!Util.almost_equals(0,min,DaidalusParameters.ALMOST_)) {
					l.add(new ColorValue(BandsRegion.UNKNOWN,0,BandsRegion.UNKNOWN));
				}
				l.add(new ColorValue(BandsRegion.UNKNOWN,min,int_color));
				l.add(new ColorValue(int_color,max,BandsRegion.UNKNOWN));
				if (!Util.almost_equals(max,mod,DaidalusParameters.ALMOST_)) {
					l.add(new ColorValue(BandsRegion.UNKNOWN,mod,BandsRegion.UNKNOWN));
				}
			} else {
				l.add(new ColorValue(int_color,0,int_color));
				l.add(new ColorValue(int_color,max,BandsRegion.UNKNOWN));
				l.add(new ColorValue(BandsRegion.UNKNOWN,min,int_color));
				l.add(new ColorValue(int_color,mod,int_color));
			}
		}
	}

	/*
	 * List l has been initialized and it's not empty. The bound l(0).val <= lb < ub <= l(n-1).val, where 
	 * n is the length of l. This function inserts (lb,ub) with the interior color int_color.
	 */
	public static void insert(List<ColorValue> l, double lb, double ub, BandsRegion int_color) {
		if (l.isEmpty() || Util.almost_geq(lb,ub,DaidalusParameters.ALMOST_) ||
				Util.almost_leq(ub,l.get(0).val,DaidalusParameters.ALMOST_) || 
				Util.almost_geq(lb,l.get(l.size()-1).val,DaidalusParameters.ALMOST_)) {
			return;
		}
		lb = Util.max(lb,l.get(0).val);
		ub = Util.min(ub,l.get(l.size()-1).val);
		int pivotl,pivotr;
		// Find a place to insert the lower bound of the interval
		pivotl = 0;
		pivotr = l.size()-1;
		while (pivotl+1 < pivotr) {
			int mid = (pivotl+pivotr)/2;
			if (Util.almost_less(lb,l.get(mid).val,DaidalusParameters.ALMOST_)) {
				pivotr = mid;
			} else if (Util.almost_greater(lb,l.get(mid).val,DaidalusParameters.ALMOST_)) {
				pivotl = mid;
			} else {
				pivotl = mid;
				pivotr = mid+1;
			} 
		}
		int i = pivotl;
		// Insert lower bound as the color value (color,lb,int_color)
		if (i < l.size()-1 && !Util.almost_equals(l.get(i).val,lb,DaidalusParameters.ALMOST_) &&
				l.get(i).color_right != BandsRegion.UNKNOWN && 
				BandsRegion.orderOfRegion(l.get(i).color_right) < BandsRegion.orderOfRegion(int_color)) {
			// Insert the color value (ext_color,ii.low,color) to the right of the i-th point
			BandsRegion ext_color = l.get(i).color_right;
			l.add(i+1,new ColorValue(ext_color,lb,ext_color));
			// The right color of the lb is set to ext_color to avoid breaking the color invariant.
			// This color will be repainted in the next loop.
			++i;
		}
		// Find a place j where to insert the upper bound of the interval
		// Everything from the right of i to the left of j that can be overridden 
		// by ext_color is re-painted
		for (; i < l.size()-1 && Util.almost_leq(l.get(i+1).val,ub,DaidalusParameters.ALMOST_); ++i) {
			if (l.get(i).color_right != BandsRegion.UNKNOWN &&
					BandsRegion.orderOfRegion(l.get(i).color_right) < BandsRegion.orderOfRegion(int_color)) {
				l.get(i).color_right = int_color;
				l.get(i+1).color_left = int_color;
			}
		}
		// Insert upper bound as the color value (int_color,ub,color)
		if (i < l.size()-1 && !Util.almost_equals(l.get(i).val,ub,DaidalusParameters.ALMOST_) &&
				l.get(i).color_right != BandsRegion.UNKNOWN && 
				BandsRegion.orderOfRegion(l.get(i).color_right) < BandsRegion.orderOfRegion(int_color)) {
			// Insert the color value (color,ii.up,ext_color) to the right of the i-th point
			BandsRegion ext_color = l.get(i).color_right;
			l.get(i).color_right = int_color;
			l.add(i+1,new ColorValue(int_color,ub,ext_color));
			++i;
		}
		// Take care of circular bands, e.g., those that do not have UNKNOWN in 
		// the extremes.
		if (l.get(0).color_left != BandsRegion.UNKNOWN) {
			l.get(0).color_left = l.get(l.size()-1).color_left;
		}
		if (l.get(l.size()-1).color_right != BandsRegion.UNKNOWN) {
			l.get(l.size()-1).color_right = l.get(0).color_right;
		}
	}

	/*
	 * List l has been initialized and it's not empty. Insert with mod logic. lb doesn't have to be
	 * less than up
	 */
	public static void insert_with_mod(List<ColorValue> l, double lb, double ub, double mod, BandsRegion int_color) {
		if (lb < ub) {
			insert(l,lb,ub,int_color);
		} else if (mod > 0){
			insert(l,0,ub,int_color);
			insert(l,lb,mod,int_color);
		}
	}

	/*
	 *  Insert a "none set" of intervals into consistent list of color values. A "none set" is a
	 *  sorted list of intervals, where values OUTSIDE the intervals have a given background color.
	 *  The color inside the intervals is "transparent".
	 *  The DAIDALUS core banding algorithm computes a none set for each bands region.
	 */
	public static void insertNoneSetToColorValues(List<ColorValue> l,IntervalSet none_set, BandsRegion bg_color) {
		double min = l.get(0).val;
		double max = l.get(l.size()-1).val;
		for (int i=0; i < none_set.size();i++) {
			insert(l,min,none_set.getInterval(i).low,bg_color);
			min = none_set.getInterval(i).up;
		}
		insert(l,min,max,bg_color);
	}

	/*
	 *  Insert a "recovery set" of intervals into consistent list of recovery color values. A "none set" is a
	 *  sorted list of intervals, where values INSIDE the intervals have a RECOVERY color.
	 *  The color outside the intervals is "transparent".
	 *  The DAIDALUS core recovery banding algorithm computes a recovery set for the corrective region.
	 */
	public static void insertRecoverySetToColorValues(List<ColorValue> l,IntervalSet recovery_set) {
		for (int i=0; i < recovery_set.size();i++) {
			insert(l,recovery_set.getInterval(i).low,recovery_set.getInterval(i).up,BandsRegion.RECOVERY);
		}
	}

	/*
	 * Return region given value in a list of color values. If the value is one of the color points, 
	 * return the smallest color according to orderOfConflictRegion.
	 */
	public static BandsRegion region_of(List<ColorValue> l, double val) {
		// In this function is important that RECOVERY and NONE both have the order 0, 
		// since they represent close intervals. 
		// For that reason, orderOfConflictRegion is used instead of orderOfRegion
		int i;
		for (i=0; i < l.size() && Util.almost_less(l.get(i).val,val,DaidalusParameters.ALMOST_); ++i);
		if (i < l.size()) {
			if (Util.almost_equals(l.get(i).val,val,DaidalusParameters.ALMOST_)) {
				if (l.get(i).color_right.isResolutionBand() || !l.get(i).color_left.isValidBand()) {
					return l.get(i).color_right;	
				} else if (l.get(i).color_left.isResolutionBand() || !l.get(i).color_right.isValidBand() ||
						l.get(i).color_left.orderOfConflictRegion() < l.get(i).color_right.orderOfConflictRegion()) {
					return l.get(i).color_left;							
				} else { 
					return l.get(i).color_right;
				}					
			} else {
				return l.get(i).color_left;
			}
		}
		return BandsRegion.UNKNOWN;
	}

}
