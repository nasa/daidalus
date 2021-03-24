/* 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 * 
 */

package gov.nasa.larcfm.Util;

/** Interval represents a interval of double's from a lower bound to
 * an upper bound.  This class is immutable.
 * 
 *  Whether the interval is interpreted as open or closed is context-dependent.
 *  There are various membership tests allowing for the different interpretations.
 */
public class Interval {

  /** The lower bound of this interval */
  public final double low;
  /** The upper bound of this interval */
  public final double up;

  /** An empty interval. */
  public static final Interval EMPTY = new Interval(0.0, -1.0);

  /** Interval with bounds (low,up) 
   * @param low lower value 
   * @param up  upper value
   * */
  public Interval(double low, double up){ 
    this.low = low;
    this.up = up;
  }

  /** Construct a new Interval which is a copy of the given Interval. 
 * @param i interval*/
  public Interval(Interval i) { 
    this.low = i.low;
    this.up = i.up;
  }

  /** Returns interval width 
   * @return width
   * */
  public double width() {
    return up-low;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    long temp;
    temp = Double.doubleToLongBits(low);
    result = prime * result + (int) (temp ^ (temp >>> 32));
    temp = Double.doubleToLongBits(up);
    result = prime * result + (int) (temp ^ (temp >>> 32));
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
    Interval other = (Interval) obj;
    if (Double.doubleToLongBits(low) != Double.doubleToLongBits(other.low))
      return false;
    if (Double.doubleToLongBits(up) != Double.doubleToLongBits(other.up))
      return false;
    return true;
  }

  /** String representation (as a closed interval) */
  public String toString() {
    return toString(Constants.get_output_precision());
  }

  public String toString(int precision) {
    if (isEmpty()) {
      return "[]";
    } else {
      return "["+f.FmPrecision(low, precision)+", "+f.FmPrecision(up, precision)+"]";
    }
  }

  public String toStringUnits(String unit) {
    if (isEmpty()) {
      return "[]";
    } else {
      return "["+Units.str(unit,low)+", "+Units.str(unit,up)+"]";
    }
  }

  public String toPVS() {
	  return toPVS(Constants.get_output_precision());
  }

  public String toPVS(int precision) {
    return "(# lb:= "+f.FmPrecision(low,precision)+", ub:= "+f.FmPrecision(up,precision)+" #)";
  }

  /** Return true if the interval is empty, or otherwise ill-formed. 
   * @return true if empty 
   * */
  public boolean isEmpty() {
    return low > up;
  }

  /** Is this interval a single value? 
   * @return true if single value
   *  */
  public boolean isSingle() {
    return low == up;
  }

  /** Is this interval a single value? (with intervals of the indicated width or smaller counting) 
   * @param width width of interval 
   * @return true if single
   * */
  public boolean isSingle(double width) {
    return low+width >= up;
  }	

  /** Is the element in this closed/closed interval? 
   * @param x value 
   * @return true if value in interval
   * */
  public boolean in(double x) {
    return low <= x && x <= up;
  }

  /** Is the element in this closed/closed interval? 
   * @param x value 
   * @return true if value in interval
   * */
  public boolean inCC(double x) {
    return low <= x && x <= up;
  }

  /** Is the element in this closed/open interval? 
   * @param x value 
   * @return true if value in interval
   * */
  public boolean inCO(double x) {
    return low <= x && x < up;
  }

  /** Is the element in this open/closed interval? 
   * @param x value 
   * @return true if value in interval
   * */
  public boolean inOC(double x) {
    return low < x && x <= up;
  }

  /** Is the element in this open/open interval? 
   * @param x value 
   * @return true if value in interval
   * */
  public boolean inOO(double x) {
    return low < x && x < up;
  }

  /** Is the element (almost) in this interval, where close/open conditions are given as parameters 
   * @param x   value 
   * @param lb_close lower bound
   * @param ub_close  upper bound
   * @return true if in interval
   * */
  public boolean almost_in(double x, boolean lb_close, boolean ub_close) {
	  return almost_in(x,lb_close,ub_close,Util.PRECISION_DEFAULT);
  }
  
  /** Is the element (almost) in this interval, where close/open conditions are given as parameters 
   * @param x value 
   * @param lb_close lower bound 
   * @param ub_close upper bound
   * @param maxUlps tolerance
   * @return true if value in interval
   * */
  public boolean almost_in(double x, boolean lb_close, boolean ub_close, long maxUlps) {
  	boolean in_lb = low < x ? lb_close || !Util.almost_equals(low,x, maxUlps) : 
  		lb_close && Util.almost_equals(low,x,maxUlps); 
  	boolean in_ub = x < up ? ub_close || !Util.almost_equals(up,x,maxUlps) : 
  		ub_close && Util.almost_equals(up,x,maxUlps); 
  	return in_lb && in_ub;
  }
  
  /** Returns a new interval which is the intersection of the current
   * Interval and the given Interval.  If the two regions do not
   * overlap, then an empty region is returned. 
   * @param r interval
   * @return intersection or empty
   */
  public Interval intersect(Interval r) {
    if ( equals(r) ) {
      return this;
    }

    if ( ! overlap(r)) {
      return EMPTY;
    }

    return new Interval(Util.max(low, r.low), Util.min(up, r.up));
  }

  /** 
   * Does the given Interval overlap with this Interval.  Intervals that only
   * share an endpoint do not overlap, for example, (1.0,2.0) and
   * (2.0,3.0) do not overlap.
   * @param r interval
   * @return true if overlap
   */
  public boolean overlap(Interval r) {
    if (isEmpty()) return false;
    if (r.isEmpty()) return false;

    if ( low <= r.low && r.up <= up ) return true;
    if ( r.low <= low && low <  r.up ) return true;
    if ( r.low <= low && up <= r.up ) return true;
    if ( r.low <  up && up <= r.up ) return true;

    return false;
  }

  /**
   * Return true if r is a subset of this interval
   * @param r interval
   * @return true if inside
   */
  public boolean includes(Interval r) {
	  return low <= r.low && up >= r.up;
  }
    
  /**
   * Shift this interval by a given amount
   * @param d amount to shift (add to both bounds)
   * @return new interval
   */
  public Interval shift(double d) {
	  return new Interval(low+d, up+d);
  }
  
}

