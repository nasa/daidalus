/*
 * Copyright (c) 2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.f;

public class IndexLevelT {
	public final int index_; // Aircraft 0-Index
	public final int level_; // Alert level
	public final double T_;  // Lookahead time
	public final boolean conflict_ac_; // True if information is from a conflict aircraft

	public IndexLevelT(int index, int level, double T, boolean conflict_ac) {
		index_ = index;
		level_ = level;
		T_ = T;
		conflict_ac_ = conflict_ac;
	}

	public String toString() {
		String s="(index: "+index_+", level: "+level_+", T: "+f.FmPrecision(T_)+", conflict_ac: "+conflict_ac_+")";
		return s;
	}

	/**
	 * @return acs the list of aircraft identifiers from list of IndexLevelTs
	 */
	public static void toStringList(List<String> acs, List<IndexLevelT> idxs, List<TrafficState> traffic) {
		acs.clear();
		for (IndexLevelT ilt : idxs) {
			acs.add(traffic.get(ilt.index_).getId());
		}
	}

	/**
	 * @return string representation of a list of IndexLevelTs
	 */
	public static String toString(List<IndexLevelT> ilts) {
		String s = "{";
		boolean comma=false;
		for (IndexLevelT ilt : ilts) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += ilt.toString();
		}	
		s += "}";
		return s;
	}
}
