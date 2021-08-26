/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

/* 
 * Most urgent aircraft strategy where the aircraft is fixed
 */
public class FixedAircraftUrgencyStrategy implements UrgencyStrategy {

	private String ac_;

	public FixedAircraftUrgencyStrategy() {
		ac_ = TrafficState.INVALID.getId();
	}

	public FixedAircraftUrgencyStrategy(String id) {
		ac_ = id;
	}

	public String getFixedAircraftId() {
		return ac_;
	}

	public void setFixedAircraftId(String id) {
		ac_ = id;
	}

	/**
	 * @return index of aircraft id
	 */
	public int mostUrgentAircraft(TrafficState ownship, List<TrafficState> traffic, double T) {
		return TrafficState.findAircraftIndex(traffic,ac_);
	}

	public UrgencyStrategy copy() {
		return new FixedAircraftUrgencyStrategy(ac_);
	}


}
