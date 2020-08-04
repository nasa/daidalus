/*
 * Implementation of alerting hysteresis logic that includes MofN and persistence.
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2019 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class AlertingHysteresis {

	private MofN   mofn_;
	private double hysteresis_time_;
	private double persistence_time_;
	private double init_time_; 
	private double last_time_; 
	private int last_alert_;

	/* 
	 * Creates an empty object
	 */
	public AlertingHysteresis() {
		mofn_ = new MofN();
		hysteresis_time_ = 0;
		persistence_time_ = 0;
		init_time_ = Double.NaN;
		last_time_ = Double.NaN;
		last_alert_ = -1;
	}

	/*
	 * Creates an object for given values of hysteresis, persistence time, and M of N parameters
	 */
	public AlertingHysteresis(double hysteresis_time, double persistence_time, int m, int n) {
		mofn_ = new MofN(m,n);
		hysteresis_time_ = hysteresis_time;
		persistence_time_ = persistence_time;
		init_time_ = Double.NaN;
		last_time_ = Double.NaN;
		last_alert_ = -1;
	}
	
	/*
	 * Sets hysteresis and persistence time
	 */
	public void setHysteresisPersistence(double hysteresis_time, double persistence_time) {
		hysteresis_time_ = hysteresis_time;
		persistence_time_ = persistence_time;
		reset();		
	}
	
	public double getLastTime() {
		return last_time_;
	}

	public int getLastAlert() {
		return last_alert_;
	}

	/*
	 * Reset object with given value
	 */
	public void reset(int val) {
		mofn_.reset(val);
		init_time_ = Double.NaN;
		last_time_ = Double.NaN;
		last_alert_ = -1;
	}
	
	/*
	 * Reset object with invalid M of N value, e.g., -1 
	 */
	public void reset() {
		reset(-1);
	}

	/*
	 * Reset object if current_time is less than or equal to last_time
	 */
	public void resetIfCurrentTime(double current_time) {
		if (Double.isNaN(last_time_) ||
				current_time <= last_time_ ||
				current_time-last_time_ > hysteresis_time_) { 	
			reset(-1);
		}
	}

	/* 
	 * In addition of m_of_n, this function applies persistence logic
	 */
	public int alertingHysteresis(int alert_level, double current_time) {
		if (Double.isNaN(last_time_) ||
				current_time <= last_time_ || current_time-last_time_ > hysteresis_time_) {
			// Reset hysteresis if current_time is in the past of later than deadline
			reset(alert_level);
		}
		// Add alert level into M of N logic even if negative
		int alert_mofn = mofn_.m_of_n(alert_level);
		if (alert_level < 0) {
			// Return invalid output since input is invalid (negative)
			last_alert_ = alert_level;
			init_time_ = Double.NaN;
		} else if (!Double.isNaN(init_time_) && last_alert_ > 0 && alert_mofn < last_alert_ && 
				current_time >= init_time_ &&
				current_time-init_time_ < persistence_time_) {
			// Do nothing. Keep the previous alert_ (persistence logic prevails)
		} else {
			if (alert_mofn > 0 && alert_mofn > last_alert_) {
				init_time_ = current_time;
			}
			// Return a valid output (i.e., >= 0), since input is valid
			last_alert_ = Util.max(0,alert_mofn);
		}
		last_time_ = current_time;
		return last_alert_;
	}

	public String toString() {
		String s = "<";
		s += "hysteresis_time: "+f.FmPrecision(hysteresis_time_);
		s += ", persistence_time: "+f.FmPrecision(persistence_time_);
		s += ", init_time: "+f.FmPrecision(init_time_);
		s += ", last_time: "+f.FmPrecision(last_time_);
		s += ", last_alert: "+f.Fmi(last_alert_);
		s += ", "+mofn_.toString();
		s += ">";
		return s;
	}

}
