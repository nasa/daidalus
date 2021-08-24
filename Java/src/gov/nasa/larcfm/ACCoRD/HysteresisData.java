/*
 * Implementation of hysteresis logic that includes MofN and persistence.
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class HysteresisData {

	private MofN    mofn_;
	private double  hysteresis_time_;
	private double  persistence_time_;
	private double  init_time_; 
	private double  last_time_; 
	private int     last_value_;
	// When this flag is true, setting a value at current time
	// resets hysteresis values
	private boolean outdated_; 

	/* 
	 * Creates an empty object
	 */
	public HysteresisData() {
		mofn_ = new MofN();
		hysteresis_time_  = 0;
		persistence_time_ = 0;
		init_time_  = Double.NaN;
		last_time_  = Double.NaN;
		last_value_ = -1;
		outdated_ = true;
	}

	/*
	 * Creates an object for given values of hysteresis, persistence time, and M of N parameters
	 */
	public HysteresisData(double hysteresis_time, double persistence_time, int m, int n) {
		mofn_ = new MofN(m,n);
		hysteresis_time_ = hysteresis_time;
		persistence_time_ = persistence_time;
		init_time_ = Double.NaN;
		last_time_ = Double.NaN;
		last_value_ = -1;
		outdated_ = true;
	}

	public void outdateIfCurrentTime(double current_time) {
		if (!Double.isNaN(last_time_) && last_time_ >= current_time) {
			outdated_ = true;
		}
	}

	public boolean isUpdatedAtCurrentTime(double current_time) {
		return last_time_ == current_time && !outdated_;
	}

	public double getInitTime() {
		return init_time_;
	}

	public double getLastTime() {
		return last_time_;
	}

	public int getLastValue() {
		return last_value_;
	}

	/*
	 * Reset object with given value
	 */
	public void reset(int val) {
		mofn_.reset(val);
		init_time_  = Double.NaN;
		last_time_  = Double.NaN;
		last_value_ = -1;
		outdated_   = true;
	}

	/* 
	 * In addition of m_of_n, this function applies persistence logic
	 */
	public int applyHysteresisLogic(int current_value, double current_time) {
		if (Double.isNaN(last_time_) ||
				current_time <= last_time_ || current_time-last_time_ > hysteresis_time_) {
			// Reset hysteresis if current_time is in the past of later than deadline
			reset(current_value);
		}
		// Add current value into M of N logic even if negative
		int value_mofn = mofn_.m_of_n(current_value);
		if (current_value < 0) {
			// Return invalid output since input is invalid (negative)
			last_value_ = current_value;
			init_time_ = Double.NaN;
		} else if (!Double.isNaN(init_time_) && last_value_ > 0 && value_mofn < last_value_ && 
				current_time >= init_time_ &&
				current_time-init_time_ < persistence_time_) {
			// Do nothing. Keep the previous value (persistence logic prevails)
		} else {
			if (value_mofn > 0 && value_mofn > last_value_) {
				init_time_ = current_time;
			}
			// Return a valid output (i.e., >= 0), since input is valid
			last_value_ = Util.max(0,value_mofn);
		}
		last_time_ = current_time;
		outdated_ = false;
		return last_value_;
	}

	public String toString() {
		String s = "<";
		s += "hysteresis_time: "+f.FmPrecision(hysteresis_time_);
		s += ", persistence_time: "+f.FmPrecision(persistence_time_);
		s += ", init_time: "+f.FmPrecision(init_time_);
		s += ", last_time: "+f.FmPrecision(last_time_);
		s += ", last_value: "+f.Fmi(last_value_);
		s += ", outdated: "+outdated_;
		s += ", "+mofn_.toString();
		s += ">";
		return s;
	}

}
