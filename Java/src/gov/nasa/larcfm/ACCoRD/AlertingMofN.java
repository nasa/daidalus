/*
 * Implementation of M of N algorithm for alerting
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.LinkedList;
import java.util.Queue;

import gov.nasa.larcfm.Util.Util;

public class AlertingMofN {

	private int m_;
	private int n_;
	private double hysteresis_time_;
	private double persistence_time_;
	private double _init_time_; 
	private double _last_time_; 
	private int _alert_;
	private int _max_;
	private Queue<Integer> _queue_;

	/* 
	 * Creates an empty M of N object
	 */
	public AlertingMofN() {
		m_ = 0;
		n_ = 0;
		hysteresis_time_ = 0;
		persistence_time_ = 0;
		_queue_ = new LinkedList<Integer>();
		reset();
	}

	/*
	 * Creates a M of N object for given values of m, n
	 * Assumes  m >= 1 and m <= n
	 */
	public AlertingMofN(int m, int n, double hysteresis_time, double persistence_time) {
		_queue_ = new LinkedList<Integer>();
		reset(m,n,hysteresis_time,persistence_time);
	}

	/*
	 * Reset M of N object.
	 */
	public void reset() {
		_init_time_ = Double.NaN;
		_last_time_ = Double.NaN;
		_alert_ = -1;
		_max_ = 0;
		_queue_.clear();
		for (int i=0;i<n_;++i) {
			_queue_.add(0);
		}
	}

	/*
	 * Reset M of N object with given parameters
	 */
	public void reset(int m, int n, double hysteresis_time, double persistence_time) {
		m_ = m;
		n_ = n;
		hysteresis_time_ = Util.max(hysteresis_time,persistence_time);
		persistence_time_ = persistence_time;
		reset();
	}

	/*
	 * Set new values for m and n.
	 * Assumes  m >= 1 and m <= n
	 */
	void setMofN(int m, int n) {
		if (m_ != m || n_ != n) {
			m_ = m;
			n_ = n;
			reset();
		}
	}

	public boolean isValid() {
		return m_ > 0 && m_ <= n_;
	}

	/* 
	 * Return M of N value for a given alert level 
	 */
	public int m_of_n(int alert_level) {
		if (_queue_.isEmpty() || alert_level < 0 || !isValid()) {
			return alert_level;
		} 
		if (alert_level > _max_) {
			_max_ = alert_level;
		}
		_queue_.poll();
		_queue_.add(alert_level);
		if (_max_ == 0) {
			return 0;
		}
		int[] count = new int[_max_]; 
		for (int i=0; i < _max_; ++i) {
			count[i] = 0;
		}
		for (int alert : _queue_) {
			for (int i=alert-1;i >= 0;--i) {
				count[i]++;
			}
		}	
		for (int i=_max_-1; i >= 0; --i) {
			if (count[i] >= m_) {
				return i+1;
			}
		}
		return 0;
	}

	/* 
	 * Return M of N value for a given alert level at a current time.
	 * In addition of m_of_n, this also applies hysteresis and persistence
	 */
	public int m_of_n(int alert_level, double current_time) {
		if (!Double.isNaN(_last_time_) &&
				(current_time <= _last_time_ || current_time - _last_time_ > hysteresis_time_)) {
			reset();
		}
		int alert_mofn = m_of_n(alert_level);
		if (alert_mofn < 0) {
			return alert_mofn;
		}
		if (!Double.isNaN(_init_time_) && _alert_ > 0 && alert_mofn < _alert_ && current_time >= _init_time_ &&
				current_time - _init_time_ < persistence_time_) {
			// Do nothing. Keep the previous alert_
		} else {
			if (alert_mofn > 0 && alert_mofn != _alert_) {
				_init_time_ = current_time;
			}
			_alert_ = alert_mofn;
		}
		_last_time_ = current_time;
		return _alert_;
	}

}
