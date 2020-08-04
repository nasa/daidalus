/*
 * Copyright (c) 2015-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.IntervalSet;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

import java.util.List;

/*
 * A colored value if a point in a line that has a color to the left and a color to the right.
 * A list of colored values is a sorted list, without repetition of values, that is consistent 
 * in the sense that two consecutive values has the same interior color, i.e., if v1 < v2, then 
 * right color of v1 is the same as left color of v2. When a colored interval is inserted in a 
 * color value list, a color can only override a weaker color. Unknown color cannot be overridden. 
 */

public class ColoredValue {
	public double val;
	public BandsRegion color_left;
	public BandsRegion color_right;

	public ColoredValue(BandsRegion l, double v, BandsRegion r) {
		val = v;
		color_left = l;
		color_right = r;
	}

	public String toString() {
		String s = "<"+color_left+", "+f.FmPrecision(val)+", "+color_right+">";
		return s;
	}

	/*
	 * Initialize a list of colored values, with min < max values, and interior color.
	 * Initial list is has two colored values: (unknown,min,int_color) and
	 * (int_color,max,unknown). Assumes l is empty.
	 */
	public static void init(List<ColoredValue> l, double min, double max, BandsRegion int_color) {
		init(l,min,max,min,max,int_color);
	}
	
	/*
	 * Initialize a list of colored values, with min <= min_val < max_val <= max values, and interior color.
	 * Initial list is has two colored values: (unknown,min,int_color) and
	 * (int_color,max,unknown). Assumes l is empty.
	 */
	public static void init(List<ColoredValue> l, double min, double max, double min_val, double max_val, BandsRegion int_color) {
		l.clear();
		if (Util.almost_less(min,min_val,DaidalusParameters.ALMOST_)) {
			l.add(new ColoredValue(BandsRegion.UNKNOWN,min,BandsRegion.UNKNOWN));
		}
		l.add(new ColoredValue(BandsRegion.UNKNOWN,min_val,int_color));
		l.add(new ColoredValue(int_color,max_val,BandsRegion.UNKNOWN));
		if (Util.almost_less(max_val,max,DaidalusParameters.ALMOST_)) {
			l.add(new ColoredValue(BandsRegion.UNKNOWN,max,BandsRegion.UNKNOWN));
		}
	}

	/*
	 * Initialize a list of colored values, with min != max, and 0 <= min < max <= mod values, 
	 * and interior color. Assumes l is empty.
	 * If min = max: Initilis list has two colored values (unknown,0,int_color) and  (int_color,max,unknown)
	 * If min < max: Initial list has four colored values: (unknown,0,unknown),
	 * (unknown,min,int_color), (int_color,max,unknown), and (unknown,mod,unknown).
	 * If min > max: Initial list is has four colored values: (unknown,0,int_color),
	 * (int_color,max,unknown), (unknown,min,int_color), and (int_color,mod,unknown).
	 */
	public static void init(List<ColoredValue> l, double min, double max, double mod, BandsRegion int_color) {
		l.clear();
		if (Util.almost_equals(min,max,DaidalusParameters.ALMOST_)) {
			l.add(new ColoredValue(BandsRegion.UNKNOWN,0,int_color));
			l.add(new ColoredValue(int_color,mod,BandsRegion.UNKNOWN));
		} else if (min < max) {
			if (!Util.almost_equals(0,min,DaidalusParameters.ALMOST_)) {
				l.add(new ColoredValue(BandsRegion.UNKNOWN,0,BandsRegion.UNKNOWN));
			}
			l.add(new ColoredValue(BandsRegion.UNKNOWN,min,int_color));
			l.add(new ColoredValue(int_color,max,BandsRegion.UNKNOWN));
			if (!Util.almost_equals(max,mod,DaidalusParameters.ALMOST_)) {
				l.add(new ColoredValue(BandsRegion.UNKNOWN,mod,BandsRegion.UNKNOWN));
			}
		} else {
			l.add(new ColoredValue(BandsRegion.UNKNOWN,0,int_color));
			l.add(new ColoredValue(int_color,max,BandsRegion.UNKNOWN));
			l.add(new ColoredValue(BandsRegion.UNKNOWN,min,int_color));
			l.add(new ColoredValue(int_color,mod,BandsRegion.UNKNOWN));
		}
	}

	/*
	 * List l has been initialized and it's not empty. The bound l(0).val <= lb < ub <= l(n-1), where 
	 * n is the length of l. This function inserts (lb,ub) with the interior color int_color.
	 */
	public static void insert(List<ColoredValue> l, double lb, double ub, BandsRegion int_color) {
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
		// Insert lower bound as the colored value (color,lb,int_color)
		if (i < l.size()-1 && !Util.almost_equals(l.get(i).val,lb,DaidalusParameters.ALMOST_) &&
				l.get(i).color_right != BandsRegion.UNKNOWN && 
				l.get(i).color_right.orderOfConflictRegion() < int_color.orderOfConflictRegion()) {
			// Insert the colored value (ext_color,ii.low,color) to the right of the i-th point
			BandsRegion ext_color = l.get(i).color_right;
			l.add(i+1,new ColoredValue(ext_color,lb,ext_color));
			// The right color of the lb is set to ext_color to avoid breaking the color invariant.
			// This color will be repainted in the next loop.
			++i;
		}
		// Find a place j where to insert the upper bound of the interval
		// Everything from the right of i to the left of j that can be overridden 
		// by ext_color is re-painted
		for (; i < l.size()-1 && Util.almost_leq(l.get(i+1).val,ub,DaidalusParameters.ALMOST_); ++i) {
			if (l.get(i).color_right != BandsRegion.UNKNOWN &&
					l.get(i).color_right.orderOfConflictRegion() < int_color.orderOfConflictRegion()) {
				l.get(i).color_right = int_color;
				l.get(i+1).color_left = int_color;
			}
		}
		// Insert upper bound as the colored value (int_color,ub,color)
		if (i < l.size()-1 && !Util.almost_equals(l.get(i).val,ub,DaidalusParameters.ALMOST_) &&
				l.get(i).color_right != BandsRegion.UNKNOWN && 
				l.get(i).color_right.orderOfConflictRegion() < int_color.orderOfConflictRegion()) {
			// Insert the colored value (color,ii.up,ext_color) to the right of the i-th point
			BandsRegion ext_color = l.get(i).color_right;
			l.get(i).color_right = int_color;
			l.add(i+1,new ColoredValue(int_color,ub,ext_color));
			++i;
		}
	}

	/*
	 * Transforms a list of colored values into a list of BandRanges. Assume ranges is empty.
	 * This function avoids adding colored points where the left and right colors are the same.
	 */
	public static void fromColoredValuestoBandsRanges(List<BandsRange> ranges, List<ColoredValue> l) { 
		ranges.clear();
		int next=0;
		for (int i = 1; i < l.size(); ++i) {
			if (l.get(i).color_left != l.get(i).color_right ||
					i == l.size()-1) {
				ranges.add(new BandsRange(new Interval(l.get(next).val,l.get(i).val),l.get(i).color_left));
				next = i;
			}
		}
	}

	/*
	 *  Insert a "none set" of intervals into consistent list of colored values. A "none set" is a
	 *  sorted list of intervals, where values OUTSIDE the intervals have a given background color.
	 *  The color inside the intervals is NONE, which means "transparent".
	 *  The DAIDALUS core banding algorithm computes a none set for each bands region.
	 */
	public static void insertNoneSetToColoredValues(List<ColoredValue> l,IntervalSet none_set, BandsRegion bg_color) {
		double min = l.get(0).val;
		double max = l.get(l.size()-1).val;
		for (int i=0; i < none_set.size();i++) {
			insert(l,min,none_set.getInterval(i).low,bg_color);
			min = none_set.getInterval(i).up;
		}
		insert(l,min,max,bg_color);
	}

}
