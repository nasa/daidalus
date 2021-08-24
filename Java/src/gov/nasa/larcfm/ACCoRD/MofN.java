/*
 * Implementation of M of N filtering logic
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import gov.nasa.larcfm.Util.f;

public class MofN {

	private int m_;
	private int n_;
	private int max_;
	private Queue<Integer> queue_;

	/*
	 * Set M of N with a given initial value
	 */
	public void setMofN(int m, int n, int val) {
		m_ = m;
		n_ = n;
		reset(val);
	}

	/*
	 * Set M of N, where the initial value is set to -1 (undefined)
	 */
	public void setMofN(int m, int n) {
		setMofN(m,n,-1);
	}
	
	/*
	 * Creates an M of N object, with a given initial value
	 */
	public MofN(int m, int n, int val) {
		queue_ = new LinkedList<Integer>();
		setMofN(m,n,val);
	}

	/*
	 * Creates an M of N object, where the initial value is set to -1 (undefined)
	 */
	public MofN(int m, int n) {
		this(m,n,-1);
	}

	/* 
	 * Creates an empty M of N object, with no parameter initialization. 
	 * Without further use of setMofN on this object, it's considered invalid and
	 * doesn't perform M of N logic. 
	 */
	public MofN() {
		this(0,0,-1);
	}

	/*
	 * Creates a copy of M of N object
	 */
	public MofN(MofN mofn) {
		m_ = mofn.m_;
		n_ = mofn.n_;
		max_ = mofn.max_;
		queue_ = new LinkedList<Integer>(mofn.queue_); 
	}

	/*
	 * Reset M of N object with a given initial value 
	 */
	public void reset(int val) {
		queue_.clear();
		max_ = val;
		for (int i=0;i<n_;++i) {
			queue_.add(i < m_ ? val : -1);
		}
	}

	/*
	 * Reset M of N object, where the initial value is set to -1 (undefined)
	 */
	public void reset() {
		reset(-1);
	}

	/*
	 * Returns true if this object is able to perform M of N logic.
	 */
	public boolean isValid() {
		return !queue_.isEmpty() && m_ > 0 && m_ <= n_; 
	}

	/* 
	 * Return M of N value for a given value. 
	 * Counts maximum occurrences of value assuming that that value represents all the
	 * values val such that 0 <= val <= value. Return -1 if none of the values satisfies
	 * M of N logic.
	 */
	public int m_of_n(int value) {
		if (!isValid()) {
			return value;
		} 
		if (value > max_) {
			max_ = value;
		}
		queue_.poll();
		queue_.add(value);
		if (max_ < 0) {
			return max_;
		}
		int[] count = new int[max_+1]; 
		for (int i=0; i <= max_; ++i) {
			count[i] = 0;
		}
		for (int val : queue_) {
			for (int i=val;i >= 0;--i) {
				count[i]++;
			}
		}	
		for (int i=max_; i >= 0; --i) {
			if (count[i] >= m_) {
				return i;
			}
		}
		return -1;
	}
	
	public boolean sameAs(MofN mofn) {
		if (max_ != mofn.max_  && queue_.size() != mofn.queue_.size()) {
			return false;
		}
		Iterator<Integer> it1 = queue_.iterator();
		Iterator<Integer> it2 = mofn.queue_.iterator();
		while (it1.hasNext() && it2.hasNext()) {
			if (it1.next().compareTo(it2.next()) != 0) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		String s=f.Fmi(m_)+" of "+f.Fmi(n_)+": [";
		boolean comma = false;
		for (int val : queue_) {
			if (comma) {
				s+=",";
			} else {
				comma = true;
			}
			s += f.Fmi(val);
		}
		s+="]";
		return s;
	}

}
