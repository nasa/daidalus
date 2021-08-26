/*
 * Copyright (c) 2018-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.f;

public class IndexLevelT {
	public final int index; // Aircraft 0-Index
	public final int level; // Alert level
	public final double time_horizon;  
	// Time horizon for computation of bands, either lookahead time for conflict bands
	// or alerting time for peripheral bands

	public IndexLevelT(int idx, int lvl, double th) {
		index = idx;
		level = lvl;
		time_horizon = th;
	}

	public String toString() {
		String s="(index: "+index+", level: "+level+", time_horizon: "+f.FmPrecision(time_horizon)+")";
		return s;
	}

	/**
	 * Returns in acs the list of aircraft identifiers from list of IndexLevelTs
	 */
	public static void toStringList(List<String> acs, List<IndexLevelT> idxs, List<TrafficState> traffic) {
		acs.clear();
		for (IndexLevelT ilt : idxs) {
			acs.add(traffic.get(ilt.index).getId());
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
