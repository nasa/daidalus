/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

/* 
 * This strategy always returns an INVALID aircraft.
 */
public class NoneUrgencyStrategy implements UrgencyStrategy {

	/**
	 * @return -1, which is not a valid aircraft index
	 */
	
	private NoneUrgencyStrategy() {}
	
	public final static NoneUrgencyStrategy NONE_URGENCY_STRATEGY = new NoneUrgencyStrategy();

	public int mostUrgentAircraft(TrafficState ownship, List<TrafficState> traffic, double T) {
		return -1;  
	}

	public UrgencyStrategy copy() {
		return new NoneUrgencyStrategy();
	}

}
