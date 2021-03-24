/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

/* 
 * Generic interface for most urgent aircraft strategy.
 */
public interface UrgencyStrategy {


	/**
	 * @param ownship Ownhsip aircraft
	 * @param traffic List of traffic aircraft
	 * @param T Lookeahead time 
	 * @return index of most urgent traffic aircraft for given ownship, traffic, and lookahead time T.
	 * If {@code index <= -1}, then no aircraft is the most urgent.
	 */

	public int mostUrgentAircraft(TrafficState ownship, List<TrafficState> traffic, 
			double T);

	public UrgencyStrategy copy();

}
