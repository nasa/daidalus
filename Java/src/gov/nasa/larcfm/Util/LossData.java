/*
 * Copyright (c) 2014-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

public class LossData {

	/* 
	 * [CAM] (time_in,time_out) is the time interval of loss of separation. Every point in the open interval represents
	 * a time where the aircraft are in violation. Whether or not the bounds of the interval are violation points or not
	 * depends on the detector, e.g., for CD3D the points time_in and time_out are not necessarily violations points, 
	 * for WCV_tvar the points time_in and time_out violation times. Furthermore, time_in and time_out are always 
	 * between the lookahead time interval [B,T], where 0 <= B < T. It is always the case that if time_in < time_out 
	 * is true, then there is a conflict. 
	 */ 
	final public double  time_in;                   // relative time to loss of separation
	final public double  time_out;                  // relative time to the exit from loss of separation	

	public static final LossData EMPTY = new LossData();

	public LossData(double tin, double tout) {
		time_in = tin;
		time_out = tout;
	}

	public LossData() {
		time_in = Double.POSITIVE_INFINITY;
		time_out = Double.NEGATIVE_INFINITY;
	}

	/**
	 * Returns true if loss
	 */
	public boolean conflict() {
		return Util.almost_less(time_in,time_out); 
	}

	/**
	 * Returns true if loss occurs before t in seconds
	 */
	public boolean conflictBefore(double t) {
		return // Zero is special since loss intervals are cut at 0
				(time_in == 0 || Util.almost_less(time_in,t)) &&
				Util.almost_less(time_in,time_out);
	}

	/**
	 * Returns true if loss last more than thr in seconds
	 */
	public boolean conflictLastMoreThan(double thr) {
		return conflict() && (time_out - time_in >= thr);
	}

	/**
	 * DEPRECATED -- Use conflictLastMoreThan instead
	 */
	@Deprecated
	public boolean conflict(double thr) {
		return conflictLastMoreThan(thr);
	}

	/**
	 * Returns time to first loss in seconds.
	 * Note: this returns positive infinity if there is not a conflict!
	 */
	public double getTimeIn() {
		return conflict() ? time_in : Double.POSITIVE_INFINITY;
	}

	/**
	 * Returns time to last loss in seconds.
	 * Note: this returns negative infinity if there is not a conflict!
	 */
	public double getTimeOut() {
		return conflict() ? time_out : Double.NEGATIVE_INFINITY;
	}

	/**
	 * Returns time interval to loss in seconds.
	 */
	public Interval getTimeInterval() {
		return new Interval(time_in,time_out);
	}

	public String toString() {
		String str = "[time_in: " + f.Fm2(time_in) + ", time_out: " + f.Fm2(time_out)+"]";
		return str;
	}

}
