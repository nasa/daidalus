/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 * 
 */

package gov.nasa.larcfm.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * <p>The IntervalSet class represents a set of "double" values.  Ranges
 * of doubles are maintained as intervals (Interval).  These Intervals
 * are ordered consecutively from lowest to highest. Standard set
 * operations of <code>in</code> (membership), <code>union</code> (set union), 
 * <code>intersect</code> (set intersection), and <code>diff</code> (set difference) 
 * are provided.</p>
 *
 * <p>Within the IntervalSet, intervals are generally considered closed (including end-points), and the
 * results off operations are closed intervals.  This implies that the interval difference
 * is between a set of closed intervals and one or more open intervals.</p>
 * 
 * <p>The intervals are numbered 0 to size()-1.  To cycle through the
 * intervals one may:</p>
 *
 * <pre><code>
 * IntervalSet set;
 *
 * for( int i = 0; i &lt; set.size(); i++) {
 *   Interval r;
 *   r = set.getInterval(i);
 *   ... work with r ...
 * }
 * </code></pre><p>
 *
 * The current implementation does not allocate any dynamic (heap) memory.
 */
public class IntervalSet implements Iterable<Interval> {

	private Interval[] r;
	private int length;

	/** The initial length for the number of intervals */
	private static final int INITIAL_LENGTH = 400;

	/** Construct an empty IntervalSet */
	public IntervalSet() {
		length = 0;
		r = new Interval[INITIAL_LENGTH];
	}

	public boolean isEmpty() {
		return size()==0; 
	}

	/** Copy the IntervalSet into a new set 
	 * @param l IntervalSet to copy
	 * */
	public IntervalSet(IntervalSet l) {
		length = l.length;
		r = new Interval[l.length];
		System.arraycopy(l.r, 0, r, 0, length);
	}

	/**
	 * Build an IntervalSet containing copies of the first sz intervals in the given ArrayList.
	 * @param ar list of intervals
	 */
	public IntervalSet(List<Interval> ar) {
		r = new Interval[ar.size()];
		for (int i = 0; i < ar.size(); i++) {
			r[i] = new Interval(ar.get(i));
		}

	}

	/**
	 * Return an ArrayList representation of this set
	 * @return the ArrayList
	 */
	public ArrayList<Interval> toArrayList() {
		ArrayList<Interval> ar = new ArrayList<>(length);
		for (int i = 0; i < length; i++) {
			ar.add(new Interval(r[i]));
		}
		return ar;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + length;
		result = prime * result + Arrays.hashCode(r);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		IntervalSet other = (IntervalSet) obj;
		if (length != other.length)
			return false;
		for (int i = 0; i < length; i++) {
			if (!r[i].equals(other.r[i])) return false;
		}
		return true;
	}

	@Override
	/** Print the contents of this IntervalSet */
	public String toString() {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < length; i++) {
			if (i != 0) {
				s.append(", ");
			}
			s.append("Interval [");
			s.append(i);
			s.append("]: ");
			s.append(r[i].toString());
		}

		return s.toString();
	}

	public String toString(String unit) {
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < length; i++) {
			if (sb.length() != 0) {
				sb.append(", ");
			}
			sb.append("Interval ["+i+"]: ");
			sb.append(r[i].toStringUnits(unit));
		}

		return sb.toString();
	}


	/** Empty this IntervalSet */
	public void clear() {
		length = 0;
	}

	public Iterator<Interval> iterator() {
		final IntervalSet rl = this;

		return new Iterator<Interval>() {
			private int count = 0;

			public boolean hasNext() {
				return count < rl.size();
			}

			public Interval next() {
				if (count == rl.size()) {
					throw new NoSuchElementException();
				}
				return rl.getInterval(count++);
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
	}

	/**
	 * Return Interval i from this set. The Intervals are numbered 0..size()-1.
	 * If i is invalid, then an empty interval is returned.
	 * 
	 * @param i
	 *            the index of the desired Interval.
	 * @return interval
	 */
	public Interval getInterval(int i) {
		if (i >= length || i < 0) {
			return Interval.EMPTY;
		}

		return r[i];
	}

	/** Return the total number of intervals 
	 * @return number of intervals
	 * */
	public int size() {
		return length;
	}

	/** Is the given value a member of this set? 
	 * 
	 * @param x value
	 * @return true, if in set
	 * */
	public boolean in(double x) {
		return order(x) >= 0;
	}

	/**
	 * Returns true if i is a subset of an interval in this set
	 * @param i interval to check
	 * @return true if i is a 
	 */
	public boolean includes(Interval i) {
		for (int j = 0; j < length; j++) {
			if (r[j].includes(i)) return true;
		}
		return false;
	}

	/**
	 * Returns true if all intervals in i are included in this set
	 * @param i intervalSet to check
	 * @return true if includes
	 */
	public boolean includes(IntervalSet i) {
		for (int j = 0; j < i.size(); j++) {
			if (!includes(i.getInterval(j))) return false;
		}
		return true;
	}

	/**
	 * Return true if there is a non-empty intersection between i and this set
	 * @param i interval
	 * @return true if overlap
	 */
	public boolean overlap(Interval i) {
		for (int j = 0; j < length; j++) {
			if (i.overlap(r[j])) return true;
		}
		return false;
	}

	/**
	 * Return true if there is a non-empty intersection between i and this set
	 * @param i interval
	 * @return true if overlap
	 */
	public boolean overlap(IntervalSet i) {
		for (int j = 0; j < length; j++) {
			if (i.overlap(r[j])) return true;
		}
		return false;
	}

	/**
	 * Add the given interval into this set. If this interval overlaps any
	 * interval in the set, then the intervals are merged.
	 * 
	 * @param rn interval
	 */
	public void union(Interval rn) {
		if (rn.isEmpty()) {
			return; // nothing to add
		}

		int iLow = order(rn.low);
		int iHigh = order(rn.up);

		double low;
		double high;
		int start;
		int end;

		if (iLow < 0) {
			low = rn.low;
			start = -(iLow + 1);
		} else {
			low = r[iLow].low;
			start = iLow;
		}

		if (iHigh < 0) {
			high = rn.up;
			end = -(iHigh + 1) - 1;
		} else {
			high = r[iHigh].up;
			end = iHigh;
		}

		remove(start, end - start + 1); // start to end inclusive
		insert(start, new Interval(low, high));

	} // union

	/**
	 * Union the given IntervalSet into the current IntervalSet. Set s is
	 * unmodified.
	 * 
	 * @param s set
	 */
	public void union(IntervalSet s) {
		for (int i = 0; i < s.length; i++) {
			union(s.r[i]);
		}
	}

	/**
	 * Union the given IntervalSet into the current IntervalSet. Set n is
	 * unmodified. This method uses "almost" inequalities to compute the union.
	 * 
	 * @param n set
	 * @param maxUlps tolerance
	 */
	public void almost_union(IntervalSet n, long maxUlps) {   
		for (int i=0; i < n.size(); ++i) {
			almost_add(n.getInterval(i).low,n.getInterval(i).up,maxUlps);
		}
	}

	/**
	 * Add the given interval into this set. If this interval overlaps any
	 * interval in the set, then the intervals are merged. 
	 * This method uses "almost" inequalities to compute the addition.
	 * 
	 * @param l lower bound of interval
	 * @param u upper bound of interval
	 */
	public void almost_add(double l, double u) { 
		almost_add(l,u,Util.PRECISION_DEFAULT);
	}

	/**
	 * Add the given interval into this set. If this interval overlaps any
	 * interval in the set, then the intervals are merged. 
	 * This method uses "almost" inequalities to compute the addition.
	 * 
	 * @param l lower bound of interval
	 * @param u upper bound of interval
	 * @param maxUlps units of least precision
	 */
	public void almost_add(double l, double u, long maxUlps) {   
		if (Util.almost_less(l,u,maxUlps)) {
			IntervalSet m = new IntervalSet(this);
			clear();
			boolean go = false;
			for (int i=0; i < m.size(); ++i) {
				Interval ii = m.getInterval(i);
				if (go) {
					union(ii);
				} else if (Util.almost_leq(ii.low,l,maxUlps) && Util.almost_leq(l,ii.up,maxUlps) ||
						Util.almost_leq(l,ii.low,maxUlps) && Util.almost_leq(ii.low,u,maxUlps)) {
					l = Util.min(ii.low,l);
					u = Util.max(ii.up,u);
				} else if (Util.almost_less(u,ii.low,maxUlps)) {
					union(new Interval(l,u));
					union(ii);
					go = true;
				} else {
					union(ii);
				}
			}
			if (!go) {
				union(new Interval(l,u));
			}
		}
	}

	/**
	 * Intersect the given IntervalSet into the current IntervalSet. Set n is
	 * unmodified. This method uses "almost" inequalities to compute the intersection.
	 * 
	 * @param n set
	 */
	public void almost_intersect(IntervalSet n) {   
		almost_intersect(n,Util.PRECISION_DEFAULT);
	}

	/**
	 * Intersect the given IntervalSet into the current IntervalSet. Set n is
	 * unmodified. This method uses "almost" inequalities to compute the intersection.
	 * 
	 * @param n set
	 * @param maxUlps units of least precision
	 */
	public void almost_intersect(IntervalSet n, long maxUlps) {   
		IntervalSet m = new IntervalSet(this);
		clear();
		if (!m.isEmpty() && !n.isEmpty()) {
			int i=0;
			int j=0;
			while (i < m.size() && j < n.size()) {
				Interval ii = m.getInterval(i);
				Interval jj = n.getInterval(j);
				if (Util.almost_leq(jj.low,ii.low,maxUlps) &&
						Util.almost_less(ii.low,jj.up,maxUlps)) {
					if (Util.almost_leq(ii.up,jj.up,maxUlps)) {
						union(ii);
						++i;
					} else {
						union(new Interval(ii.low,jj.up));
						++j;
					}
				} else if (Util.almost_leq(ii.low,jj.low,maxUlps) &&
						Util.almost_less(jj.low,ii.up,maxUlps)) {
					if (Util.almost_leq(jj.up,ii.up,maxUlps)) {
						union(jj);
						++j;
					} else {
						union(new Interval(jj.low,ii.up));
						++i;
					}
				} else if (Util.almost_leq(ii.up,jj.low,maxUlps)){
					++i;
				} else if (Util.almost_leq(jj.up,ii.low,maxUlps)){
					++j;
				}
			}
		}
	}

	/**
	 * Return intersection of this interval set and interval n
	 * This may contain singletons
	 * @param n
	 * @return
	 */
	public IntervalSet intersection(Interval n) {
		IntervalSet rLocal = new IntervalSet();
		IntervalSet m = new IntervalSet(this);
		for (int i = 0; i < m.size(); i++) {
			Interval iv = m.getInterval(i);
			Interval j = iv.intersect(n);
			if (!j.isEmpty()) {
				rLocal.union(j);
			}
		}
		return rLocal;
	}

	/**
	 * Return intersection of this interval set and interval set n
	 * This may contain singletons
	 * @param n
	 * @return
	 */
	public IntervalSet intersection(IntervalSet n) {
		IntervalSet rLocal = new IntervalSet();
		for (int i = 0; i < n.size(); i++) {
			Interval iv = n.getInterval(i);
			IntervalSet m = intersection(iv);
			rLocal.union(m);
		}
		return rLocal;
	}

	/**
	 * Remove the given open interval from the given set of closed intervals.
	 * Note: the semantics of this method mean that [1,2] - (1,2) = [1,1] and
	 * [2,2]. To get rid of the extraneous singletons use methods like
	 * removeSingle() or sweepSingle().
	 * 
	 * @param rn interval
	 */
	public void diff(Interval rn) {
		if (rn.isEmpty()) {
			return; // nothing for set difference
		}
		if (rn.isSingle()) {
			return; // rn is assumed to be an open interval,
			// if it is a singleton, then there is nothing to remove
		}

		int iLow = order(rn.low);
		int iHigh = order(rn.up);

		if (iLow >= 0 && iLow == iHigh) {
			double r_iHigh_up = r[iHigh].up;
			r[iLow] = new Interval(r[iLow].low, rn.low);
			insert(iLow + 1, new Interval(rn.up, r_iHigh_up));

			return;
		}

		int start;
		int end;

		if (iLow < 0) {
			start = -(iLow + 1);
		} else {
			r[iLow] = new Interval(r[iLow].low, rn.low);
			start = iLow + 1;
		}

		if (iHigh < 0) {
			end = -(iHigh + 1) - 1;
		} else {
			r[iHigh] = new Interval(rn.up, r[iHigh].up);
			end = iHigh - 1;
		}

		remove(start, end - start + 1); // start to end inclusive

	} // diff

	/** 
	 * Perform a set difference between these two IntervalSets.  The
	 * parameter is interpreted as a set of open intervals.
	 * 
	 * @param n set
	 */
	public void diff(IntervalSet n) {
		for (int i = 0; i < n.length; i++) {
			diff(n.r[i]);
		}
	}

	/**
	 * Remove the single-valued interval x from this IntervalSet.  If x is
	 * not a single-valued interval (of width or less), then this method does nothing.
	 * 
	 * @param x value
	 * @param width space around value
	 */
	public void removeSingle(double x, double width) {
		int i = order(x);
		if (i >= 0 && r[i].isSingle(width)) {
			remove(i);
		}
	}

	/**
	 * Remove the single-valued interval x from this IntervalSet.  If x is
	 * not a single-valued interval, then this method does nothing.
	 * 
	 * @param x value
	 */
	public void removeSingle(double x) {
		removeSingle(x, 0.0);
	}

	/** Remove all intervals less than given width 
	 * 
	 * @param width maximum space to remove
	 * */
	public void removeLessThan(double width) {
		if (width == 0) return;
		int i = 0;
		while (i < length) {
			if (r[i].width() < width) {
				remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Remove all the single-valued intervals (of width or less) from this IntervalSet.
	 * 
	 * @param width space around single values
	 */
	public void sweepSingle(double width) {
		int i = 0;
		while (i < length) {
			if (r[i].isSingle(width)) {
				remove(i);
			} else {
				i++;
			}
		}
	}

	/**
	 * Remove all the single-valued intervals from this IntervalSet.
	 */
	public void sweepSingle() {
		sweepSingle(0.0);
	}

	/**
	 * Remove all breaks of less than width from this IntervalSet
	 * 
	 * @param width max open space to remove
	 */
	public void sweepBreaks(double width) {
		int i = 0; 
		while (i < length-1) {
			if (r[i].up+width > r[i+1].low) {
				union(new Interval(r[i].low, r[i+1].up));
			} else {
				i++;
			}
		}
	}

	/*
	 * Insert the given interval at point i. If the point i is greater than the
	 * number of Intervals in the list, then add this interval to the end.
	 */
	private void insert(int i, Interval region) {
		if (region.isEmpty()) {
			return;
		}

		if (i < 0) {
			i = 0;
		}

		if (i > length) {
			i = length;
		}

		if (i == length && length < r.length) {
			r[length] = region;
			length++;
		} else {
			length++;
			int c = length;

			if (length >= r.length) {
				Interval[] newarray = new Interval[r.length + INITIAL_LENGTH];
				System.arraycopy(r, 0, newarray, 0, length - 1);
				r = newarray;
			}

			while (i < c) {
				r[c] = r[c - 1];
				c--;
			}

			r[i] = region;
		}
	} // insert


	/*
	 * Remove Interval i from the set and return it
	 */
	private Interval remove(int i) {
		if (i < 0 || i >= length) {
			return Interval.EMPTY;
		}

		Interval t = r[i];

		while (i + 1 < length) {
			r[i] = r[i + 1];
			i++;
		}

		length--;

		return t;
	}

	/*
	 * Remove the len number of intervals starting at i.
	 */
	private void remove(int i, int len) {
		for (int j = 0; j < len; j++) {
			remove(i);
		}
	}

	/*
	 * Find the point where point n fits into the list. If x is a member, then
	 * the returned value is the index of the interval. If x is not a member,
	 * then -index-1 is returned, where index is the index of the interval after
	 * x.
	 */
	private int order(double x) {
		for (int i = 0; i < length; i++) {
			if (r[i].in(x)) {
				return i;
			}
			if (x < r[i].low) {
				return -i - 1;
			}

		}

		return -length - 1;
	}

	/**
	 * Return the negation of this interval, exactly: ( [-inf.+inf] - this )
	 * @return IntervalSet
	 */
	public IntervalSet negate() {
		Interval rLocal = new Interval(Double.NEGATIVE_INFINITY,Double.POSITIVE_INFINITY);
		IntervalSet is = new IntervalSet();
		is.union(rLocal);
		is.diff(this);
		return is;
	}

	/**
	 * Return an IntervalSet from an ordered list of integers.  "Adjacent" numbers will be considered part of the same interval.
	 * @param list list of integers
	 * @return IntervalSet
	 */
	public static IntervalSet fromList(List<Integer> list) {
		IntervalSet set = new IntervalSet();
		if ( ! list.isEmpty()) {
			set.union(new Interval(list.get(0), list.get(0)));
			set.union(new Interval(list.get(list.size()-1), list.get(list.size()-1)));
		}
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i-1)+1 == list.get(i)) {
				set.union(new Interval(list.get(i-1), list.get(i)));
			}
		}
		return set;
	}

	/**
	 * Return an IntervalSet from an ordered list of discretized numbers.  "Adjacent" numbers (within epsilon, inclusive) will be considered part of the same interval.
	 * @param list list of doubles
	 * @param epsilon tolerance
	 * @return IntervalSet
	 */
	public static IntervalSet fromList(List<Double> list, double epsilon) {
		IntervalSet set = new IntervalSet();
		if ( ! list.isEmpty()) {
			set.union(new Interval(list.get(0), list.get(0)));
			set.union(new Interval(list.get(list.size()-1), list.get(list.size()-1)));
		}
		for (int i = 1; i < list.size(); i++) {
			if (list.get(i) - list.get(i-1) <= epsilon) {
				set.union(new Interval(list.get(i-1), list.get(i)));
			}
		}
		return set;
	}

	/**
	 * Return the interval in this set that contains v, or the EMPTY interval.
	 * @param v
	 * @return
	 */
	public Interval intervalContaining(double v) {
		int i = order(v);
		if (i >= 0) return r[i];
		else return Interval.EMPTY;
	}

	/**
	 * Shift all intervals in this set by a given amount
	 * @param d amount to shift
	 */
	public void shift(double d) {
		if (d > 0) {
			for (int i = size()-1; i >= 0; i--) {
				r[i] = r[i].shift(d);
			}
		} else if (d < 0) {
			for (int i = 0; i < size(); i++) {
				r[i] = r[i].shift(d);
			}
		}
	}
	
}
