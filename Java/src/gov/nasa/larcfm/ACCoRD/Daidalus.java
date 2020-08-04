/* 
 * Copyright (c) 2011-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Constants;
import gov.nasa.larcfm.Util.ErrorLog;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * <p>Objects of class "Daidalus" compute the conflict bands using 
 * kinematic single-maneuver projections of the ownship and linear preditions 
 * of (multiple) traffic aircraft positions. The bands consist of ranges of 
 * guidance maneuvers: direction angles, horizontal speeds, vertical
 * speeds, and altitude.</p> 
 * 
 * <p>An assumption of the bands information is that the traffic aircraft
 * do not maneuver. If the ownship immediately executes a NONE
 * guidance maneuver, then the new path is conflict free (within a
 * lookahead time of the parameter).  If the ownship immediately executes a
 * NEAR/MID/FAR guidance maneuver and no traffic aircraft maneuvers, then
 * there will corresponding alert within the corresponding alerting level thresholds.</p>
 *
 * <p>If recovery bands are set and the ownship is in
 * a violation path, loss of separation recovery bands and recovery times are
 * computed for each type of maneuver. If the ownship immediately executes a 
 * RECOVERY guidance maneuver, then the new path is conflict-free after the
 * recovery time. Furthermore, the recovery time is the minimum time for which 
 * there exists a kinematic conflict-free maneuver in the future. </p>
 *
 * <p>Note that in the case of geodetic coordinates this version of bands
 * performs an internal projection of the coordinates and velocities
 * into the Euclidean frame (see Util/Projection).  Accuracy may be 
 * reduced if the traffic plans involve any segments longer than
 * Util.Projection.projectionConflictRange(lat,acc), and an error will
 * be logged if the distance between traffic and ownship exceeds 
 * Util.Projection.projectionMaxRange() at any point in the lookahead
 * range.</p>
 *
 * Disclaimers: The formal proofs of the core algorithms use real numbers,
 * however these implementations use floating point
 * numbers, so numerical differences could result. In addition, the
 * geodetic computations include certain inaccuracies, especially near
 * the poles.
 *
 * The basic usage is
 * <pre>
 * Daidalus daa = new Daidalus();
 * daa.loadFromFile({@code <configurationfile>});
 * ...
 * 
 * daa.setOwnshipState(position of ownship, velocity of ownship);
 * daa.addTrafficState(position of (one) traffic aircraft, velocity of traffic);
 * daa.addTrafficState(position of (another) traffic aircraft, velocity of traffic);
 * 
 * ...add other traffic aircraft...
 * 
 * {@code
 * for (int i = 0; i < daa.horizontalDirectionBandsLength(); i++ )} {  
 *    interval = daa.horizontalDirectionIntervalAt(i);
 *    lower_ang = intrval.low;
 *    upper_ang = intrval.up;
 *    regionType = daa.horizontalDirectionRegionAt(i);
 *    ..do something with this information..
 * } 
 * 
 * ...similar for horizontal speed and vertical speed...
 * </pre>
 *
 */

public class Daidalus implements GenericStateBands {

	private DaidalusCore      core_;
	private DaidalusDirBands  hdir_band_; 
	private DaidalusHsBands   hs_band_;  
	private DaidalusVsBands   vs_band_;  
	private DaidalusAltBands  alt_band_; 

	protected ErrorLog error = new ErrorLog("Daidalus");

	/* Constructors */

	/** 
	 * Construct a Daidalus object with a default set of parameters.
	 * NOTE THAT NO ALERTER IS SPECIFIED BY DEFAULT. 
	 * USE set_WC_DO_365() or set_Buffered_WC_DO_365(..) or even better, load a configuration file,
	 * to obtain some predefined configuration.
	 */
	public Daidalus() {
		core_ = new DaidalusCore();
		hdir_band_ = new DaidalusDirBands(core_.parameters);
		hs_band_ = new DaidalusHsBands(core_.parameters);
		vs_band_ = new DaidalusVsBands(core_.parameters);
		alt_band_ = new DaidalusAltBands(core_.parameters);
	}

	/** 
	 * Construct a Daidalus object with initial alerter.
	 */
	public Daidalus(Alerter alerter) {
		core_ = new DaidalusCore(alerter);
		hdir_band_ = new DaidalusDirBands(core_.parameters);
		hs_band_ = new DaidalusHsBands(core_.parameters);
		vs_band_ = new DaidalusVsBands(core_.parameters);
		alt_band_ = new DaidalusAltBands(core_.parameters);
	}

	/** 
	 * Construct a Daidalus object with the default parameters and one alerter with the
	 * given detector and T (in seconds) as the alerting time, early alerting time, and lookahead time.
	 */
	public Daidalus(Detection3D det, double T) {
		core_ = new DaidalusCore(det,T);
		hdir_band_ = new DaidalusDirBands(core_.parameters);
		hs_band_ = new DaidalusHsBands(core_.parameters);
		vs_band_ = new DaidalusVsBands(core_.parameters);
		alt_band_ = new DaidalusAltBands(core_.parameters);
	}

	/**
	 * Construct a Daidalus object from an existing Daidalus object. 
	 * This copies all traffic data and configuration, but not the cache information.
	 */
	public Daidalus(Daidalus daa) {
		core_ = new DaidalusCore(daa.core_);
		hdir_band_ = new DaidalusDirBands(daa.hdir_band_);
		hs_band_ = new DaidalusHsBands(daa.hs_band_);
		vs_band_ = new DaidalusVsBands(daa.vs_band_);
		alt_band_ = new DaidalusAltBands(daa.alt_band_);
	}

	/* Setting for WC Definitions RTCA DO-365 */

	/*  
	 * Set Daidalus object such that 
	 * - Alerting thresholds are unbuffered as defined in RTCA DO-365.
	 * - Maneuver guidance logic assumes instantaneous maneuvers
	 * - Bands saturate at DMOD/ZTHR
	 */
	public void set_WC_DO_365() {
		clearAlerters();
		addAlerter(Alerter.DWC_Phase_I());
		setCorrectiveRegion(BandsRegion.MID);
		setInstantaneousBands();
		disableHysteresis();
		setCollisionAvoidanceBands(false);
		setCollisionAvoidanceBandsFactor(0.0);
		setMinHorizontalRecovery(0.66,"nmi");
		setMinVerticalRecovery(450,"ft");
	}

	/*  
	 * Set DAIDALUS object such that 
	 * - Alerting thresholds are buffered 
	 * - Maneuver guidance logic assumes kinematic maneuvers
	 * - Turn rate is set to 3 deg/s, when type is true, and to  1.5 deg/s
	 *   when type is false.
	 * - Bands don't saturate until NMAC
	 */
	public void set_Buffered_WC_DO_365(boolean type) {
		clearAlerters();
		addAlerter(Alerter.Buffered_DWC_Phase_I());
		setCorrectiveRegion(BandsRegion.MID);
		setKinematicBands(type);
		disableHysteresis();
		setCollisionAvoidanceBands(true);
		setCollisionAvoidanceBandsFactor(0.1);
		setMinHorizontalRecovery(1.0,"nmi");
		setMinVerticalRecovery(450,"ft");
	}


	/* Set DAIDALUS object such that alerting logic and maneuver guidance corresponds to 
	 * ACCoRD's CD3D, i.e.,
	 * - Separation is given by a cylinder of of diameter 5nm and height 1000ft
	 * - Lookahead time and alerting time is 180s
	 * - Only 1 alert level
	 * - Instantaneous maneuvers */
	public void set_CD3D() {
		clearAlerters();
		addAlerter(Alerter.CD3D());
		setCorrectiveRegion(BandsRegion.NEAR);
		setInstantaneousBands();
		setCollisionAvoidanceBands(true);
		setCollisionAvoidanceBandsFactor(0.1);
	}

	/**
	 * Return release version string
	 */
	public static String release() {
		return "DAIDALUSj V-"+DaidalusParameters.VERSION+"-FormalATM-"+Constants.version; 
	}

	/* Ownship and Traffic Setting */

	/**
	 * Returns state of ownship.
	 */
	public TrafficState getOwnshipState() {
		return core_.ownship;
	}

	/**
	 * Returns state of aircraft at index idx 
	 */
	public TrafficState getAircraftStateAt(int idx) {
		if (0 <= idx && idx <= lastTrafficIndex()) {
			if (idx == 0) {
				return core_.ownship;
			} else {
				return core_.traffic.get(idx-1);
			}
		} else {
			error.addError("getAircraftState: aircraft index "+idx+" is out of bounds");
			return TrafficState.INVALID;
		}
	}

	/**
	 * Set ownship state and current time. Clear all traffic. 
	 * @param id Ownship's identified
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 * @param time Time stamp of ownship's state
	 */
	public void setOwnshipState(String id, Position pos, Velocity vel, double time) {
		clear();
		core_.set_ownship_state(id,pos,vel,time);
	}

	/**
	 * Set ownship state at time 0.0. Clear all traffic. 
	 * @param id Ownship's identified
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */
	public void setOwnship(String id, Position pos, Velocity vel) {
		setOwnshipState(id,pos,vel,0.0);
	}

	/**
	 * Add traffic state at given time. 
	 * If time is different from current time, traffic state is projected, past or future, 
	 * into current time. If it's the first aircraft, this aircraft is 
	 * set as the ownship. 
	 * @param id Aircraft's identifier
	 * @param pos Aircraft's position
	 * @param vel Aircraft's ground velocity
	 * @param time Time stamp of aircraft's state
	 * @return Aircraft's index
	 */
	public int addTrafficState(String id, Position pos, Velocity vel, double time) {
		if (lastTrafficIndex() < 0) {
			setOwnshipState(id,pos,vel,time);
			return 0;
		} else {
			int ac_idx = core_.add_traffic_state(id,pos,vel,time);
			stale(false);
			return ac_idx;
		}
	}

	/**
	 * Add traffic state at current time. If it's the first aircraft, this aircraft is 
	 * set as the ownship. 
	 * @param id Aircraft's identifier
	 * @param pos Aircraft's position
	 * @param vel Aircraft's ground velocity
	 * @return Aircraft's index
	 */
	public int addTrafficState(String id, Position pos, Velocity vel) {
		return addTrafficState(id,pos,vel,core_.current_time);
	}

	/**
	 * Add traffic state at current time. If it's the first aircraft, this aircraft is 
	 * set as the ownship. 
	 * @param id Aircraft's identifier
	 * @param pos Aircraft's position
	 * @param vel Aircraft's ground velocity
	 * Same function as addTrafficState, but it doesn't return index of added traffic. This is neeeded
	 * for compatibility with GenericBands
	 */
	public void addTraffic(String id, Position pos, Velocity vel) {
		addTrafficState(id,pos,vel);
	}

	/** 
	 * Get index of aircraft with given name. Return -1 if no such index exists
	 */
	public int aircraftIndex(String name) {
		if (lastTrafficIndex() >= 0) {
			if (core_.ownship.getId().equals(name)) {
				return 0;
			}
			for (int i = 0; i < core_.traffic.size(); ++i) {
				if (core_.traffic.get(i).getId().equals(name))
					return i+1;
			}
		}
		return -1;
	}

	/**
	 * Exchange ownship aircraft with aircraft named id.
	 * DO NOT USE IT, UNLESS YOU KNOW WHAT YOU ARE DOING. EXPERT USE ONLY !!!
	 */
	public void resetOwnship(String id) {
		int ac_idx = aircraftIndex(id);
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			core_.reset_ownship(ac_idx-1);
			stale(true);
		} else {
			error.addError("resetOwnship: aircraft index "+ac_idx+" is out of bounds");
		}
	}

	/**                                                                                                                                                
	 * Remove traffic from the list of aircraft. Returns false if no aircraft was removed.                                                             
	 * Ownship cannot be removed.                                                                                                                      
	 * If traffic is at index i, the indices of aircraft at k > i, are shifted to k-1.                                                                 
	 * DO NOT USE IT, UNLESS YOU KNOW WHAT YOU ARE DOING. EXPERT USE ONLY !!!
	 */
	public boolean removeTrafficAircraft(String name) {
		int ac_idx = aircraftIndex(name);
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			core_.traffic.remove(ac_idx-1);
			stale(true);
			return true;
		}
		return false;
	}

	/**
	 * Project ownship and traffic aircraft offset seconds in the future (if positive) or in the past (if negative)
	 * DO NOT USE IT, UNLESS YOU KNOW WHAT YOU ARE DOING. EXPERT USE ONLY !!!
	 */
	public void linearProjection(double offset) {
		if (offset != 0) {
			core_.ownship = core_.ownship.linearProjection(offset);
			for (int i = 0; i < core_.traffic.size(); i++) {
				core_.traffic.set(i,core_.traffic.get(i).linearProjection(offset));
			}   
			stale(true);
		}
	}

	/**
	 * @return true if ownship has been set
	 */
	public boolean hasOwnship() {
		return core_.has_ownship();
	}

	/**
	 * @return true if at least one traffic has been set
	 */
	public boolean hasTraffic() {
		return core_.has_traffic();
	}

	/**
	 * @return number of aircraft, including ownship.
	 */
	public int numberOfAircraft() {
		if (!hasOwnship()) {
			return 0;
		} else {
			return core_.traffic.size()+1;
		}
	}

	/**
	 * @return last traffic index. Every traffic aircraft has an index between 1 and lastTrafficIndex. 
	 * The index 0 is reserved for the ownship. When lastTrafficIndex is 0, the ownship is set but no
	 * traffic aircraft has been set. When lastTrafficIndex is negative, ownship has not been set.
	 */
	public int lastTrafficIndex() {
		return numberOfAircraft()-1;
	}

	public boolean isLatLon() {
		return hasOwnship() && core_.ownship.isLatLon();
	}

	/* Current Time */

	/**
	 * Return currrent time in seconds. Current time is the time of the ownship.
	 */
	public double getCurrentTime() {
		return core_.current_time;
	}

	/**
	 * Return currrent time in specified units. Current time is the time of the ownship.
	 */
	public double getCurrentTime(String u) {
		return Units.to(u,getCurrentTime());
	}

	/* Wind Setting */

	/**
	 * Get wind velocity specified in the TO direction
	 */
	public Velocity getWindVelocityTo() {
		return core_.wind_vector;
	}

	/**
	 * Get wind velocity specified in the From direction
	 */
	public Velocity getWindVelocityFrom() {
		return core_.wind_vector.Neg();
	}

	/**
	 * Set wind velocity specified in the TO direction
	 * @param wind_velocity: Wind velocity specified in TO direction
	 */
	public void setWindVelocityTo(Velocity wind_vector) {
		core_.set_wind_velocity(wind_vector);
		stale(false);
	}

	/**
	 * Set wind velocity specified in the From direction
	 * @param nwind_velocity: Wind velocity specified in From direction
	 */
	public void setWindVelocityFrom(Velocity nwind_vector) {
		setWindVelocityTo(nwind_vector.Neg());
	}

	/* Alerter Setting */

	/**
	 * Set alerter of the aircraft at ac_idx to alerter_idx
	 * @param ac_idx: Aircraft index between 0 (ownship) and lastTrafficIndex(), inclusive 
	 * @param alerter_idx: Alerter index starting from 1. The value 0 means none.
	 */
	public void setAlerterIndex(int ac_idx, int alerter_idx) {
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (getAircraftStateAt(ac_idx).getAlerterIndex() != alerter_idx) {
				if (ac_idx == 0 && core_.ownship.isValid()) {
					core_.ownship.setAlerterIndex(alerter_idx);
					stale(false);
				} else {
					core_.traffic.get(ac_idx-1).setAlerterIndex(alerter_idx);
					stale(false);
				}
			}
		} else {
			error.addError("setAlerter: aircraft index "+ac_idx+" is out of bounds");
		}
		if (alerter_idx > core_.parameters.numberOfAlerters()) {
			error.addWarning("setAlerter: alerter index "+alerter_idx+" is out of bounds");
		}
	}

	/**
	 * Set alerter of the aircraft at ac_idx to alerter
	 * @param ac_idx: Aircraft index between 0 (ownship) and lastTrafficIndex(), inclusive 
	 * @param alerter: Alerter identifier 
	 */
	public void setAlerter(int ac_idx, String alerter) {
		setAlerterIndex(ac_idx,core_.parameters.getAlerterIndex(alerter));
	}

	/**
	 * Return alert index used for the traffic aircraft at index ac_idx. 
	 * The alert index depends on alerting logic. If ownship centric, it returns the
	 * alert index of ownship. Otherwise, it returns the alert index of the traffic aircraft 
	 * at ac_idx. 
	 */
	public int alerterIndexBasedOnAlertingLogic(int ac_idx) {
		return core_.alerter_index_of(ac_idx-1);
	}

	/** 
	 * Returns most severe alert level for a given aircraft. Returns 0 if either the aircraft or the alerter is undefined.
	 */
	public int mostSevereAlertLevel(int ac_idx) {
		int alerter_idx = alerterIndexBasedOnAlertingLogic(ac_idx);
		if (alerter_idx > 0) {
			Alerter alerter = core_.parameters.getAlerterAt(alerter_idx);
			if (alerter.isValid()) {
				return alerter.mostSevereAlertLevel();
			}
		}
		return 0;
	}

	/* SUM Setting */

	/**
	 * Set horizontal position uncertainty of aircraft at index ac_idx
	 * s_EW_std: East/West position standard deviation in internal units
	 * s_NS_std: North/South position standard deviation in internal units
	 * s_EN_std: East/North position standard deviation in internal units
	 */
	public void setHorizontalPositionUncertainty(int ac_idx, double s_EW_std, double s_NS_std, double s_EN_std) {
		if (0 <= ac_idx && ac_idx <= numberOfAircraft()) {
			if (ac_idx == 0) {
				core_.ownship.setHorizontalPositionUncertainty(s_EW_std,s_NS_std,s_EN_std);
			} else {
				core_.traffic.get(ac_idx-1).setHorizontalPositionUncertainty(s_EW_std,s_NS_std,s_EN_std);
			}
			stale(false);
		}
	}

	/**
	 * Set horizontal position uncertainty of aircraft at index ac_idx
	 * s_EW_std: East/West position standard deviation in given units
	 * s_NS_std: North/South position standard deviation in given units
	 * s_EN_std: East/North position standard deviation in given units
	 */
	public void setHorizontalPositionUncertainty(int ac_idx, double s_EW_std, double s_NS_std, double s_EN_std, String u) {
		setHorizontalPositionUncertainty(ac_idx,Units.from(u,s_EW_std),Units.from(u,s_NS_std),Units.from(u,s_EN_std));
	}

	/**
	 * Set vertical position uncertainty of aircraft at index ac_idx
	 * sz_std : Vertical position standard deviation in internal units
	 */
	public void setVerticalPositionUncertainty(int ac_idx, double sz_std) {
		if (0 <= ac_idx && ac_idx <= numberOfAircraft()) {
			if (ac_idx == 0) {
				core_.ownship.setVerticalPositionUncertainty(sz_std);
			} else {
				core_.traffic.get(ac_idx-1).setVerticalPositionUncertainty(sz_std);
			}
			stale(false);
		}
	}

	/**
	 * Set vertical position uncertainty of aircraft at index ac_idx
	 * sz_std : Vertical position standard deviation in given units
	 */
	public void setVerticalPositionUncertainty(int ac_idx, double sz_std, String u) {
		setVerticalPositionUncertainty(ac_idx,Units.from(u,sz_std));
	}

	/**
	 * Set horizontal velocity uncertainty of aircraft at index ac_idx
	 * v_EW_std: East/West position standard deviation in internal units
	 * v_NS_std: North/South position standard deviation in internal units
	 * v_EN_std: East/North position standard deviation in internal units
	 */
	public void setHorizontalVelocityUncertainty(int ac_idx, double v_EW_std, double v_NS_std,  double v_EN_std) {
		if (0 <= ac_idx && ac_idx <= numberOfAircraft()) {
			if (ac_idx == 0) {
				core_.ownship.setHorizontalVelocityUncertainty(v_EW_std,v_NS_std,v_EN_std);
			} else {
				core_.traffic.get(ac_idx-1).setHorizontalVelocityUncertainty(v_EW_std,v_NS_std,v_EN_std);
			}
			stale(false);
		}
	}

	/**
	 * Set horizontal velocity uncertainty of aircraft at index ac_idx
	 * v_EW_std: East/West position standard deviation in given units
	 * v_NS_std: North/South position standard deviation in given units
	 * v_EN_std: East/North position standard deviation in given units
	 */
	public void setHorizontalVelocityUncertainty(int ac_idx, double v_EW_std, double v_NS_std,  double v_EN_std, String u) {
		setHorizontalVelocityUncertainty(ac_idx,Units.from(u,v_EW_std),Units.from(u,v_NS_std),Units.from(u,v_EN_std));
	}

	/**
	 * Set vertical speed uncertainty of aircraft at index ac_idx
	 * vz_std : Vertical speed standard deviation in internal units
	 */
	public void setVerticalSpeedUncertainty(int ac_idx, double vz_std) {
		if (0 <= ac_idx && ac_idx <= numberOfAircraft()) {
			if (ac_idx == 0) {
				core_.ownship.setVerticalSpeedUncertainty(vz_std);
			} else {
				core_.traffic.get(ac_idx-1).setVerticalSpeedUncertainty(vz_std);
			}
			stale(false);
		}
	}

	/**
	 * Set vertical speed uncertainty of aircraft at index ac_idx
	 * vz_std : Vertical speed standard deviation in given units
	 */
	public void setVerticalSpeedUncertainty(int ac_idx, double vz_std, String u) {
		setVerticalSpeedUncertainty(ac_idx,Units.from(u,vz_std));
	}

	/**
	 * Reset all uncertainties of aircraft at index ac_idx
	 */
	public void resetUncertainty(int ac_idx) {
		if (0 <= ac_idx && ac_idx <= numberOfAircraft()) {
			if (ac_idx == 0) {
				core_.ownship.resetUncertainty();
			} else {
				core_.traffic.get(ac_idx-1).resetUncertainty();
			}
			stale(false);
		}
	}

	/* Urgency strategy for implicitly coordinate bands (experimental) */

	/**
	 * @return strategy for computing most urgent aircraft. 
	 */
	public UrgencyStrategy getUrgencyStrategy() { 
		return core_.urgency_strategy;
	}

	/**
	 * Set strategy for computing most urgent aircraft.
	 */
	public void setUrgencyStrategy(UrgencyStrategy strat) { 
		core_.urgency_strategy = strat.copy();
		stale(true);
	}

	/**
	 * @return most urgent aircraft.
	 */
	public TrafficState mostUrgentAircraft() {
		return core_.mostUrgentAircraft();
	}

	/* Computation of contours, a.k.a. blobs */

	/**
	 * Computes horizontal contours contributed by aircraft at index idx, for 
	 * given alert level. A contour is a non-empty list of points in counter-clockwise 
	 * direction representing a polygon.   
	 * @param blobs list of direction contours returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 */
	public void horizontalContours(List<List<Position>>blobs, int ac_idx, int alert_level) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			int code = core_.horizontal_contours(blobs,ac_idx-1,alert_level);
			switch (code) {
			case 1: 
				error.addError("horizontalContours: detector of traffic aircraft "+ac_idx+" is not set");
				break;
			case 2:
				error.addError("horizontalContours: no corrective alerter level for alerter of "+ac_idx);
				break;
			case 3: 
				error.addError("horizontalContours: alerter of traffic aircraft "+ac_idx+" is out of bounds");
				break;
			}
		} else {
			error.addError("horizontalContours: aircraft index "+ac_idx+" is out of bounds");			
		}
	}

	/**
	 * Computes horizontal contours contributed by aircraft at index ac_idx, the alert level 
	 * corresponding to the corrective region. A contour is a non-empty list of points in 
	 * counter-clockwise direction representing a polygon.   
	 * @param blobs list of direction contours returned by reference.
	 * @param idx is the index of the aircraft used to compute the contours.
	 */
	public void horizontalContours(List<List<Position>>blobs, int ac_idx) {
		horizontalContours(blobs,ac_idx,0);
	}

	/* Setting and getting DaidalusParameters */

	/**
	 * Return number of alerters.
	 */
	public int numberOfAlerters() {
		return core_.parameters.numberOfAlerters();
	}

	/**
	 * Return alerter at index i (starting from 1).
	 */
	public Alerter getAlerterAt(int i) {
		return core_.parameters.getAlerterAt(i);
	}

	/**
	 * Return index of alerter with a given name. Return 0 if it doesn't exist 
	 */
	public int getAlerterIndex(String id) {
		return core_.parameters.getAlerterIndex(id);
	}

	/**
	 * Clear all alert thresholds
	 */
	public void clearAlerters() {
		core_.parameters.clearAlerters();
		stale(true);
	}

	/**
	 * Add alerter (if id of alerter already exists, replaces alerter with new one).
	 * Return index of added alerter
	 */
	public int addAlerter(Alerter alerter) {
		int alert_idx = core_.parameters.addAlerter(alerter);
		stale(true);
		return alert_idx;
	}

	/** 
	 * @return lookahead time in seconds. 
	 */ 
	public double getLookaheadTime() {
		return core_.parameters.getLookaheadTime();
	}

	/** 
	 * @return lookahead time in specified units [u].
	 */
	public double getLookaheadTime(String u) {
		return core_.parameters.getLookaheadTime(u);
	}

	/** 
	 * @return left direction in radians [0 - pi] [rad] from current ownship's direction
	 */
	public double getLeftHorizontalDirection() {
		return core_.parameters.getLeftHorizontalDirection();
	}

	/** 
	 * @return left direction in specified units [0 - pi] [u] from current ownship's direction
	 */
	public double getLeftHorizontalDirection(String u) {
		return Units.to(u,getLeftHorizontalDirection());
	}

	/** 
	 * @return right direction in radians [0 - pi] [rad] from current ownship's direction
	 */
	public double getRightHorizontalDirection() {
		return core_.parameters.getRightHorizontalDirection();
	}

	/** 
	 * @return right direction in specified units [0 - pi] [u] from current ownship's direction
	 */
	public double getRightHorizontalDirection(String u) {
		return Units.to(u,getRightHorizontalDirection());
	}

	/** 
	 * @return minimum horizontal speed for horizontal speed bands in internal units [m/s].
	 */
	public double getMinHorizontalSpeed() {
		return core_.parameters.getMinHorizontalSpeed();
	}

	/** 
	 * @return minimum horizontal speed for horizontal speed bands in specified units [u].
	 */
	public double getMinHorizontalSpeed(String u) {
		return Units.to(u,getMinHorizontalSpeed());
	}

	/** 
	 * @return maximum horizontal speed for horizontal speed bands in internal units [m/s].
	 */
	public double getMaxHorizontalSpeed() {
		return core_.parameters.getMaxHorizontalSpeed();
	}

	/** 
	 * @return maximum horizontal speed for horizontal speed bands in specified units [u].
	 */
	public double getMaxHorizontalSpeed(String u) {
		return Units.to(u,getMaxHorizontalSpeed());
	}

	/** 
	 * @return minimum vertical speed for vertical speed bands in internal units [m/s].
	 */
	public double getMinVerticalSpeed() {
		return core_.parameters.getMinVerticalSpeed();
	}

	/** 
	 * @return minimum vertical speed for vertical speed bands in specified units [u].
	 */
	public double getMinVerticalSpeed(String u) {
		return Units.to(u,getMinVerticalSpeed());
	}

	/** 
	 * @return maximum vertical speed for vertical speed bands in internal units [m/s].
	 */
	public double getMaxVerticalSpeed() {
		return core_.parameters.getMaxVerticalSpeed();
	}

	/** 
	 * @return maximum vertical speed for vertical speed bands in specified units [u].
	 */
	public double getMaxVerticalSpeed(String u) {
		return Units.to(u,getMaxVerticalSpeed());
	}

	/** 
	 * @return minimum altitude for altitude bands in internal units [m]
	 */
	public double getMinAltitude() {
		return core_.parameters.getMinAltitude();
	}

	/** 
	 * @return minimum altitude for altitude bands in specified units [u].
	 */
	public double getMinAltitude(String u) {
		return Units.to(u,getMinAltitude());
	}

	/** 
	 * @return maximum altitude for altitude bands in internal units [m]
	 */
	public double getMaxAltitude() {
		return core_.parameters.getMaxAltitude();
	}

	/** 
	 * @return maximum altitude for altitude bands in specified units [u].
	 */
	public double getMaxAltitude(String u) {
		return Units.to(u,getMaxAltitude());
	}

	/**
	 * @return Horizontal speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeHorizontalSpeed() {
		return core_.parameters.getBelowRelativeHorizontalSpeed();
	}

	/**
	 * @return Horizontal speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeHorizontalSpeed(String u) {
		return Units.to(u,getBelowRelativeHorizontalSpeed());
	}

	/**
	 * @return Horizontal speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeHorizontalSpeed() {
		return core_.parameters.getAboveRelativeHorizontalSpeed();
	}

	/**
	 * @return Horizontal speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeHorizontalSpeed(String u) {
		return Units.to(u,getAboveRelativeHorizontalSpeed());
	}

	/**
	 * @return Vertical speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeVerticalSpeed() {
		return core_.parameters.getBelowRelativeVerticalSpeed();
	}

	/**
	 * @return Vertical speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeVerticalSpeed(String u) {
		return Units.to(u,getBelowRelativeVerticalSpeed());
	}

	/**
	 * @return Vertical speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeVerticalSpeed() {
		return core_.parameters.getAboveRelativeVerticalSpeed();
	}

	/**
	 * @return Vertical speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeVerticalSpeed(String u) {
		return Units.to(u,getAboveRelativeVerticalSpeed());
	}

	/**
	 * @return Altitude in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeAltitude() {
		return core_.parameters.getBelowRelativeAltitude();
	}

	/**
	 * @return Altitude in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public double getBelowRelativeAltitude(String u) {
		return Units.to(u,getBelowRelativeAltitude());
	}

	/**
	 * @return Altitude in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeAltitude() {
		return core_.parameters.getAboveRelativeAltitude();
	}

	/**
	 * @return Altitude in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public double getAboveRelativeAltitude(String u) {
		return Units.to(u,getAboveRelativeAltitude());
	}

	/** 
	 * @return step size for direction bands in internal units [rad]. 
	 */
	public double getHorizontalDirectionStep() {
		return core_.parameters.getHorizontalDirectionStep();
	}

	/** 
	 * @return step size for direction bands in specified units [u]. 
	 */
	public double getHorizontalDirectionStep(String u) {
		return core_.parameters.getHorizontalDirectionStep(u);
	}

	/** 
	 * @return step size for horizontal speed bands in internal units [m/s]. 
	 */
	public double getHorizontalSpeedStep() {
		return core_.parameters.getHorizontalSpeedStep();
	}

	/** 
	 * @return step size for horizontal speed bands in specified units [u]. 
	 */
	public double getHorizontalSpeedStep(String u) {
		return core_.parameters.getHorizontalSpeedStep(u);
	}

	/** 
	 * @return step size for vertical speed bands in internal units [m/s].
	 */
	public double getVerticalSpeedStep() {
		return core_.parameters.getVerticalSpeedStep();
	}

	/** 
	 * @return step size for vertical speed bands in specified units [u].
	 */
	public double getVerticalSpeedStep(String u) {
		return core_.parameters.getVerticalSpeedStep(u);
	}

	/** 
	 * @return step size for altitude bands in internal units [m]
	 */
	public double getAltitudeStep() {
		return core_.parameters.getAltitudeStep();
	}

	/** 
	 * @return step size for altitude bands in specified units [u].
	 */
	public double getAltitudeStep(String u) {
		return core_.parameters.getAltitudeStep(u);
	}

	/** 
	 * @return horizontal acceleration for horizontal speed bands to value in internal units [m/s^2]. 
	 */
	public double getHorizontalAcceleration() {
		return core_.parameters.getHorizontalAcceleration();
	}

	/** 
	 * @return horizontal acceleration for horizontal speed bands to value in specified units [u]. 
	 */
	public double getHorizontalAcceleration(String u) {
		return core_.parameters.getHorizontalAcceleration(u);
	}

	/** 
	 * @return constant vertical acceleration for vertical speed and altitude bands in internal [m/s^2]
	 * units
	 */
	public double getVerticalAcceleration() {
		return core_.parameters.getVerticalAcceleration();
	}

	/** 
	 * @return constant vertical acceleration for vertical speed and altitude bands in specified
	 * units
	 */
	public double getVerticalAcceleration(String u) {
		return core_.parameters.getVerticalAcceleration(u);
	}

	/** 
	 * @return turn rate in internal units [rad/s].
	 */
	public double getTurnRate() {
		return core_.parameters.getTurnRate();
	}

	/** 
	 * @return turn rate in specified units [u].
	 */
	public double getTurnRate(String u) {
		return core_.parameters.getTurnRate(u);
	}

	/** 
	 * @return bank angle in internal units [rad].
	 */
	public double getBankAngle() {
		return core_.parameters.getBankAngle();
	}

	/** 
	 * @return bank angle in specified units [u].
	 */
	public double getBankAngle(String u) {
		return core_.parameters.getBankAngle(u);
	}

	/** 
	 * @return the vertical climb/descend rate for altitude bands in internal units [m/s]
	 */
	public double getVerticalRate() {
		return core_.parameters.getVerticalRate();
	}

	/** 
	 * @return the vertical climb/descend rate for altitude bands in specified units [u].
	 */
	public double getVerticalRate(String u) {
		return core_.parameters.getVerticalRate(u);
	}

	/** 
	 * @return horizontal NMAC distance in internal units [m].
	 */
	public double getHorizontalNMAC() {
		return core_.parameters.getHorizontalNMAC();
	}

	/** 
	 * @return horizontal NMAC distance in specified units [u].
	 */
	public double getHorizontalNMAC(String u) {
		return core_.parameters.getHorizontalNMAC(u);
	}

	/** 
	 * @return vertical NMAC distance in internal units [m].
	 */
	public double getVerticalNMAC() {
		return core_.parameters.getVerticalNMAC();
	}

	/** 
	 * @return vertical NMAC distance in specified units [u].
	 */
	public double getVerticalNMAC(String u) {
		return core_.parameters.getVerticalNMAC(u);
	}

	/**
	 * @return recovery stability time in seconds. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public double getRecoveryStabilityTime() {
		return core_.parameters.getRecoveryStabilityTime();
	}

	/**
	 * @return recovery stability time in specified units. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public double getRecoveryStabilityTime(String u) {
		return core_.parameters.getRecoveryStabilityTime(u);
	}

	/** 
	 * @return hysteresis time in seconds. 
	 */
	public double getHysteresisTime() {
		return core_.parameters.getHysteresisTime();
	}

	/** 
	 * @return hysteresis time in specified units [u].
	 */
	public double getHysteresisTime(String u) {
		return core_.parameters.getHysteresisTime(u);
	}

	/** 
	 * @return alerting persistence time in seconds. 
	 */
	public double getPersistenceTime() {
		return core_.parameters.getPersistenceTime();
	}

	/** 
	 * @return alerting persistence time in specified units [u].
	 */
	public double getPersistenceTime(String u) {
		return core_.parameters.getPersistenceTime(u);
	}

	/** 
	 * @return true if bands persistence is enabled
	 */
	public boolean isEnabledBandsPersistence() {
		return core_.parameters.isEnabledBandsPersistence();
	}

	/** 
	 * Enable/disable bands persistence
	 */ 
	public void setBandsPersistence(boolean flag) {
		core_.parameters.setBandsPersistence(flag);
		stale(true);
	}

	/** 
	 * Enable bands persistence
	 */ 
	public void enableBandsPersistence() {
		setBandsPersistence(true);
	}

	/** 
	 * Disable bands persistence
	 */ 
	public void disableBandsPersistence() {
		setBandsPersistence(false);
	}

	/** 
	 * @return persistence for horizontal direction resolution in internal units
	 */
	public double getPersistencePreferredHorizontalDirectionResolution() {
		return core_.parameters.getPersistencePreferredHorizontalDirectionResolution();
	}

	/** 
	 * @return persistence for horizontal direction resolution in given units
	 */
	public double getPersistencePreferredHorizontalDirectionResolution(String u) {
		return core_.parameters.getPersistencePreferredHorizontalDirectionResolution(u);
	}

	/** 
	 * @return persistence for horizontal speed resolution in internal units
	 */
	public double getPersistencePreferredHorizontalSpeedResolution() {
		return core_.parameters.getPersistencePreferredHorizontalSpeedResolution();
	}

	/** 
	 * @return persistence for horizontal speed resolution in given units
	 */
	public double getPersistencePreferredHorizontalSpeedResolution(String u) {
		return core_.parameters.getPersistencePreferredHorizontalSpeedResolution(u);
	}

	/** 
	 * @return persistence for vertical speed resolution in internal units
	 */
	public double getPersistencePreferredVerticalSpeedResolution() {
		return core_.parameters.getPersistencePreferredVerticalSpeedResolution();
	}

	/** 
	 * @return persistence for vertical speed resolution in given units
	 */
	public double getPersistencePreferredVerticalSpeedResolution(String u) {
		return core_.parameters.getPersistencePreferredVerticalSpeedResolution(u);
	}

	/** 
	 * @return persistence for altitude resolution in internal units
	 */
	public double getPersistencePreferredAltitudeResolution() {
		return core_.parameters.getPersistencePreferredAltitudeResolution();
	}

	/** 
	 * @return persistence for altitude resolution in given units
	 */
	public double getPersistencePreferredAltitudeResolution(String u) {
		return core_.parameters.getPersistencePreferredAltitudeResolution(u);
	}

	/** 
	 * @return Alerting parameter m of "M of N" strategy
	 */
	public int getAlertingParameterM() {
		return core_.parameters.getAlertingParameterM();
	}

	/** 
	 * @return Alerting parameter n of "M of N" strategy
	 */
	public int getAlertingParameterN() {
		return core_.parameters.getAlertingParameterN();
	}

	/**
	 * @return minimum horizontal separation for recovery bands in internal units [m].
	 */
	public double getMinHorizontalRecovery() {
		return core_.parameters.getMinHorizontalRecovery();
	}

	/** 
	 * Return minimum horizontal separation for recovery bands in specified units [u]
	 */
	public double getMinHorizontalRecovery(String u) {
		return core_.parameters.getMinHorizontalRecovery(u);
	}

	/** 
	 * @return minimum vertical separation for recovery bands in internal units [m].
	 */
	public double getMinVerticalRecovery() {
		return core_.parameters.getMinVerticalRecovery();
	}

	/** 
	 * Return minimum vertical separation for recovery bands in specified units [u].
	 */
	public double getMinVerticalRecovery(String u) {
		return core_.parameters.getMinVerticalRecovery(u);
	}

	/** 
	 * Sets lookahead time in seconds. 
	 */ 
	public void setLookaheadTime(double t) {
		core_.parameters.setLookaheadTime(t);
		stale(true);
	}

	/** 
	 * Set lookahead time to value in specified units [u].
	 */
	public void setLookaheadTime(double t, String u) {
		core_.parameters.setLookaheadTime(t,u);
		stale(true);
	}

	/** 
	 * Set left direction to value in internal units [rad]. Value is expected to be in [0 - pi]
	 */
	public void setLeftHorizontalDirection(double val) {
		core_.parameters.setLeftHorizontalDirection(val);
		hdir_band_.set_min_rel(getLeftHorizontalDirection());
	}

	/** 
	 * Set left direction to value in specified units [u]. Value is expected to be in [0 - pi]
	 */
	public void setLeftHorizontalDirection(double val, String u) {
		core_.parameters.setLeftHorizontalDirection(val,u);
		hdir_band_.set_min_rel(getLeftHorizontalDirection());
	}

	/** 
	 * Set right direction to value in internal units [rad]. Value is expected to be in [0 - pi]
	 */
	public void setRightHorizontalDirection(double val) {
		core_.parameters.setRightHorizontalDirection(val);
		hdir_band_.set_max_rel(getRightHorizontalDirection());
	}

	/** 
	 * Set right direction to value in specified units [u]. Value is expected to be in [0 - pi]
	 */
	public void setRightHorizontalDirection(double val, String u) {
		core_.parameters.setRightHorizontalDirection(val,u);
		hdir_band_.set_max_rel(getRightHorizontalDirection());
	}

	/** 
	 * Sets minimum horizontal speed for horizontal speed bands to value in internal units [m/s].
	 */
	public void setMinHorizontalSpeed(double val) {
		core_.parameters.setMinHorizontalSpeed(val);
		hs_band_.set_min_nomod(getMinHorizontalSpeed());
	}

	/** 
	 * Sets minimum horizontal speed for horizontal speed bands to value in specified units [u].
	 */
	public void setMinHorizontalSpeed(double val, String u) {
		core_.parameters.setMinHorizontalSpeed(val,u);
		hs_band_.set_min_nomod(getMinHorizontalSpeed());
	}

	/** 
	 * Sets maximum horizontal speed for horizontal speed bands to value in internal units [m/s].
	 */
	public void setMaxHorizontalSpeed(double val) {
		core_.parameters.setMaxHorizontalSpeed(val);
		hs_band_.set_max_nomod(getMaxHorizontalSpeed());
	}

	/** 
	 * Sets maximum horizontal speed for horizontal speed bands to value in specified units [u].
	 */
	public void setMaxHorizontalSpeed(double val, String u) {
		core_.parameters.setMaxHorizontalSpeed(val,u);
		hs_band_.set_max_nomod(getMaxHorizontalSpeed());
	}

	/** 
	 * Sets minimum vertical speed for vertical speed bands to value in internal units [m/s].
	 */
	public void setMinVerticalSpeed(double val) {
		core_.parameters.setMinVerticalSpeed(val);
		vs_band_.set_min_nomod(getMinVerticalSpeed());
	}

	/** 
	 * Sets minimum vertical speed for vertical speed bands to value in specified units [u].
	 */
	public void setMinVerticalSpeed(double val, String u) {
		core_.parameters.setMinVerticalSpeed(val,u);
		vs_band_.set_min_nomod(getMinVerticalSpeed());
	}

	/** 
	 * Sets maximum vertical speed for vertical speed bands to value in internal units [m/s].
	 */
	public void setMaxVerticalSpeed(double val) {
		core_.parameters.setMaxVerticalSpeed(val);
		vs_band_.set_max_nomod(getMaxVerticalSpeed());
	}

	/** 
	 * Sets maximum vertical speed for vertical speed bands to value in specified units [u].
	 */
	public void setMaxVerticalSpeed(double val, String u) {
		core_.parameters.setMaxVerticalSpeed(val,u);
		vs_band_.set_max_nomod(getMaxVerticalSpeed());	
	}

	/** 
	 * Sets minimum altitude for altitude bands to value in internal units [m]
	 */
	public void setMinAltitude(double val) {
		core_.parameters.setMinAltitude(val);
		alt_band_.set_min_nomod(getMinAltitude());
	}

	/** 
	 * Sets minimum altitude for altitude bands to value in specified units [u].
	 */
	public void setMinAltitude(double val, String u) {
		core_.parameters.setMinAltitude(val,u);
		alt_band_.set_min_nomod(getMinAltitude());
	}

	/** 
	 * Sets maximum altitude for altitude bands to value in internal units [m]
	 */
	public void setMaxAltitude(double val) {
		core_.parameters.setMaxAltitude(val);
		alt_band_.set_max_nomod(getMaxAltitude());
	}

	/** 
	 * Sets maximum altitude for altitude bands to value in specified units [u].
	 */
	public void setMaxAltitude(double val, String u) {
		core_.parameters.setMaxAltitude(val,u);
		alt_band_.set_max_nomod(getMaxAltitude());
	}

	/**
	 * Set horizontal speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeHorizontalSpeed(double val) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val);
		hs_band_.set_min_rel(getBelowRelativeHorizontalSpeed());
	}

	/**
	 * Set horizontal speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeHorizontalSpeed(double val,String u) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val,u);
		hs_band_.set_min_rel(getBelowRelativeHorizontalSpeed());
	}

	/**
	 * Set horizontal speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeHorizontalSpeed(double val) {
		core_.parameters.setAboveRelativeHorizontalSpeed(val);
		hs_band_.set_max_rel(getAboveRelativeHorizontalSpeed());
	}

	/**
	 * Set horizontal speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeHorizontalSpeed(double val, String u) {
		core_.parameters.setAboveRelativeHorizontalSpeed(val,u);
		hs_band_.set_max_rel(getAboveRelativeHorizontalSpeed());
	}

	/**
	 * Set vertical speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeVerticalSpeed(double val) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val);
		vs_band_.set_min_rel(getBelowRelativeHorizontalSpeed());
	}

	/**
	 * Set vertical speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeVerticalSpeed(double val, String u) {
		core_.parameters.setBelowRelativeVerticalSpeed(val,u);
		vs_band_.set_min_rel(getBelowRelativeVerticalSpeed());
	}

	/**
	 * Set vertical speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeVerticalSpeed(double val) {
		core_.parameters.setAboveRelativeVerticalSpeed(val);
		vs_band_.set_max_rel(getAboveRelativeVerticalSpeed());
	}

	/**
	 * Set vertical speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeVerticalSpeed(double val, String u) {
		core_.parameters.setAboveRelativeVerticalSpeed(val,u);
		vs_band_.set_max_rel(getAboveRelativeVerticalSpeed());
	}

	/**
	 * Set altitude in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeAltitude(double val) {
		core_.parameters.setBelowRelativeAltitude(val);
		alt_band_.set_min_rel(getBelowRelativeAltitude());
	}

	/**
	 * Set altitude in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeAltitude(double val, String u) {
		core_.parameters.setBelowRelativeAltitude(val,u);
		alt_band_.set_min_rel(getBelowRelativeAltitude());
	}

	/**
	 * Set altitude in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeAltitude(double val) {
		core_.parameters.setAboveRelativeAltitude(val);
		alt_band_.set_max_rel(getAboveRelativeAltitude());
	}

	/**
	 * Set altitude in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeAltitude(double val, String u) {
		core_.parameters.setAboveRelativeAltitude(val,u);
		alt_band_.set_max_rel(getAboveRelativeAltitude());
	}

	/**
	 * Set below value to min when computing horizontal speed bands
	 */
	public void setBelowToMinRelativeHorizontalSpeed() {
		setBelowRelativeHorizontalSpeed(-1);
	}

	/**
	 * Set above value to max when computing horizontal speed bands
	 */
	public void setAboveToMaxRelativeHorizontalSpeed() {
		setAboveRelativeHorizontalSpeed(-1);
	}

	/**
	 * Set below value to min when computing vertical speed bands
	 */
	public void setBelowToMinRelativeVerticalSpeed() {
		setBelowRelativeVerticalSpeed(-1);
	}

	/**
	 * Set above value to max when computing vertical speed bands
	 */
	public void setAboveToMaxRelativeVerticalSpeed() {
		setAboveRelativeVerticalSpeed(-1);
	}

	/**
	 * Set below value to min when computing altitude bands
	 */
	public void setBelowToMinRelativeAltitude() {
		setBelowRelativeAltitude(-1);
	}

	/**
	 * Set above value to max when computing altitude bands
	 */
	public void setAboveToMaxRelativeAltitude() {
		setAboveRelativeAltitude(-1);
	}

	/**
	 * Disable relative horizontal speed bands
	 */
	public void disableRelativeHorizontalSpeedBands() {
		setBelowRelativeHorizontalSpeed(0);
		setAboveRelativeHorizontalSpeed(0);
	}

	/**
	 * Disable relative vertical speed bands
	 */
	public void disableRelativeVerticalSpeedBands() {
		setBelowRelativeVerticalSpeed(0);
		setAboveRelativeVerticalSpeed(0);
	}

	/**
	 * Disable relative altitude bands
	 */
	public void disableRelativeAltitude() {
		setBelowRelativeAltitude(0);
		setAboveRelativeAltitude(0);
	}

	/** 
	 * Sets step size for direction bands in internal units [rad].
	 */
	public void setHorizontalDirectionStep(double val) {
		core_.parameters.setHorizontalDirectionStep(val);
		hdir_band_.set_step(getHorizontalDirectionStep());
	}

	/** 
	 * Sets step size for direction bands in specified units [u].
	 */
	public void setHorizontalDirectionStep(double val, String u) {
		core_.parameters.setHorizontalDirectionStep(val,u);
		hdir_band_.set_step(getHorizontalDirectionStep());
	}

	/** 
	 * Sets step size for horizontal speed bands to value in internal units [m/s].
	 */
	public void setHorizontalSpeedStep(double val) {
		core_.parameters.setHorizontalSpeedStep(val);
		hs_band_.set_step(getHorizontalSpeedStep());
	}

	/** 
	 * Sets step size for horizontal speed bands to value in specified units [u].
	 */
	public void setHorizontalSpeedStep(double val, String u) {
		core_.parameters.setHorizontalSpeedStep(val,u);
		hs_band_.set_step(getHorizontalSpeedStep());
	}

	/** 
	 * Sets step size for vertical speed bands to value in internal units [m/s].
	 */
	public void setVerticalSpeedStep(double val) {
		core_.parameters.setVerticalSpeedStep(val);
		vs_band_.set_step(getVerticalSpeedStep());
	}

	/** 
	 * Sets step size for vertical speed bands to value in specified units [u].
	 */
	public void setVerticalSpeedStep(double val, String u) {
		core_.parameters.setVerticalSpeedStep(val,u);
		vs_band_.set_step(getVerticalSpeedStep());
	}

	/** 
	 * Sets step size for altitude bands to value in internal units [m]
	 */
	public void setAltitudeStep(double val) {
		core_.parameters.setAltitudeStep(val);
		alt_band_.set_step(getAltitudeStep());
	}

	/** 
	 * Sets step size for altitude bands to value in specified units [u].
	 */
	public void setAltitudeStep(double val, String u) {
		core_.parameters.setAltitudeStep(val,u);
		alt_band_.set_step(getAltitudeStep());
	}

	/** 
	 * Sets horizontal acceleration for horizontal speed bands to value in internal units [m/s^2].
	 */
	public void setHorizontalAcceleration(double val) {
		core_.parameters.setHorizontalAcceleration(val);
		hs_band_.set_horizontal_accel(getHorizontalAcceleration());
	}

	/** 
	 * Sets horizontal acceleration for horizontal speed bands to value in specified units [u].
	 */
	public void setHorizontalAcceleration(double val, String u) {
		core_.parameters.setHorizontalAcceleration(val,u);
		hs_band_.set_horizontal_accel(getHorizontalAcceleration());
	}

	/** 
	 * Sets the constant vertical acceleration for vertical speed and altitude bands
	 * to value in internal units [m/s^2]
	 */
	public void setVerticalAcceleration(double val) {
		core_.parameters.setVerticalAcceleration(val);
		vs_band_.set_vertical_accel(getVerticalAcceleration());
		alt_band_.set_vertical_accel(getVerticalAcceleration());
	}

	/** 
	 * Sets the constant vertical acceleration for vertical speed and altitude bands
	 * to value in specified units [u].
	 */
	public void setVerticalAcceleration(double val, String u) {
		core_.parameters.setVerticalAcceleration(val,u);
		vs_band_.set_vertical_accel(getVerticalAcceleration());
		alt_band_.set_vertical_accel(getVerticalAcceleration());
	}

	/** 
	 * Sets turn rate for direction bands to value in internal units [rad/s]. As a side effect, this method
	 * resets the bank angle.
	 */
	public void setTurnRate(double val) {
		core_.parameters.setTurnRate(val);
		hdir_band_.set_turn_rate(getTurnRate());
		hdir_band_.set_bank_angle(getBankAngle());
	}

	/** 
	 * Sets turn rate for direction bands to value in specified units [u]. As a side effect, this method
	 * resets the bank angle.
	 */
	public void setTurnRate(double val, String u) {
		core_.parameters.setTurnRate(val,u);
		hdir_band_.set_turn_rate(getTurnRate());
		hdir_band_.set_bank_angle(getBankAngle());
	}

	/** 
	 * Sets bank angle for direction bands to value in internal units [rad]. As a side effect, this method
	 * resets the turn rate.
	 */
	public void setBankAngle(double val) {
		core_.parameters.setBankAngle(val);
		hdir_band_.set_turn_rate(getTurnRate());
		hdir_band_.set_bank_angle(getBankAngle());
	}

	/** 
	 * Sets bank angle for direction bands to value in specified units [u]. As a side effect, this method
	 * resets the turn rate.
	 */
	public void setBankAngle(double val, String u) {
		core_.parameters.setBankAngle(val,u);
		hdir_band_.set_turn_rate(getTurnRate());
		hdir_band_.set_bank_angle(getBankAngle());
	}

	/** 
	 * Sets vertical rate for altitude bands to value in internal units [m/s]
	 */
	public void setVerticalRate(double val) {
		core_.parameters.setVerticalRate(val);
		alt_band_.set_vertical_rate(getVerticalRate());
	}

	/** 
	 * Sets vertical rate for altitude bands to value in specified units [u].
	 */
	public void setVerticalRate(double val, String u) {
		core_.parameters.setVerticalRate(val,u);
		alt_band_.set_vertical_rate(getVerticalRate());
	}

	/** 
	 * Set horizontal NMAC distance to value in internal units [m].
	 */
	public void setHorizontalNMAC(double val) {
		core_.parameters.setHorizontalNMAC(val);
		stale(true);
	}

	/** 
	 * Set horizontal NMAC distance to value in specified units [u].
	 */
	public void setHorizontalNMAC(double val, String u) {
		core_.parameters.setHorizontalNMAC(val,u);
		stale(true);
	}

	/** 
	 * Set vertical NMAC distance to value in internal units [m].
	 */
	public void setVerticalNMAC(double val) {
		core_.parameters.setVerticalNMAC(val);
		stale(true);
	}

	/** 
	 * Set vertical NMAC distance to value in specified units [u].
	 */
	public void setVerticalNMAC(double val, String u) {
		core_.parameters.setVerticalNMAC(val,u);
		stale(true);
	}

	/**
	 * Sets recovery stability time in seconds. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public void setRecoveryStabilityTime(double t) {
		core_.parameters.setRecoveryStabilityTime(t);
		stale(true);
	}

	/**
	 * Sets recovery stability time in specified units. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public void setRecoveryStabilityTime(double t, String u) {
		core_.parameters.setRecoveryStabilityTime(t,u);
		stale(true);
	}

	/** 
	 * Set hysteresis time to value in seconds.
	 */
	public void setHysteresisTime(double val) {
		core_.parameters.setHysteresisTime(val);
		stale(true);
	}

	/** 
	 * Set hysteresis time to value in specified units [u].
	 */
	public void setHysteresisTime(double val, String u) {
		core_.parameters.setHysteresisTime(val,u);
		stale(true);
	}

	/** 
	 * Set alerting persistence time to value in seconds.
	 */
	public void setPersistenceTime(double val) {
		core_.parameters.setPersistenceTime(val);
		stale(true);
	}

	/** 
	 * Set alerting persistence time to value in specified units [u].
	 */
	public void setPersistenceTime(double val, String u) {
		core_.parameters.setPersistenceTime(val,u);
		stale(true);
	}

	/** 
	 * Set persistence in horizontal direction resolution in internal units
	 */
	public void setPersistencePreferredHorizontalDirectionResolution(double val) {
		core_.parameters.setPersistencePreferredHorizontalDirectionResolution(val);
		hdir_band_.stale(true);
	}

	/** 
	 * Set persistence in horizontal direction resolution in given units
	 */
	public void setPersistencePreferredHorizontalDirectionResolution(double val, String u) {
		core_.parameters.setPersistencePreferredHorizontalDirectionResolution(val,u);
		hdir_band_.stale(true);
	}

	/** 
	 * Set persistence in horizontal speed resolution in internal units
	 */
	public void setPersistencePreferredHorizontalSpeedResolution(double val) {
		core_.parameters.setPersistencePreferredHorizontalSpeedResolution(val);
		hs_band_.stale(true);
	}

	/** 
	 * Set persistence in horizontal speed resolution in given units
	 */
	public void setPersistencePreferredHorizontalSpeedResolution(double val, String u) {
		core_.parameters.setPersistencePreferredHorizontalSpeedResolution(val,u);
		hs_band_.stale(true);
	}

	/** 
	 * Set persistence in vertical speed resolution in internal units
	 */
	public void setPersistencePreferredVerticalSpeedResolution(double val) {
		core_.parameters.setPersistencePreferredVerticalSpeedResolution(val);
		vs_band_.stale(true);
	}

	/** 
	 * Set persistence in vertical speed resolution in given units
	 */
	public void setPersistencePreferredVerticalSpeedResolution(double val, String u) {
		core_.parameters.setPersistencePreferredVerticalSpeedResolution(val,u);
		vs_band_.stale(true);
	}

	/** 
	 * Set persistence in altitude resolution in internal units
	 */
	public void setPersistencePreferredAltitudeResolution(double val) {
		core_.parameters.setPersistencePreferredAltitudeResolution(val);
		alt_band_.stale(true);
	}

	/** 
	 * Set persistence in altitude resolution in given units
	 */
	public void setPersistencePreferredAltitudeResolution(double val, String u) {
		core_.parameters.setPersistencePreferredAltitudeResolution(val,u);
		alt_band_.stale(true);
	}

	/** 
	 * Set alerting parameters of M of N strategy
	 */
	public void setAlertingMofN(int m, int n) {
		core_.parameters.setAlertingMofN(m,n);
		stale(true);
	}

	/** 
	 * Sets minimum horizontal separation for recovery bands in internal units [m].
	 */
	public void setMinHorizontalRecovery(double val) {
		core_.parameters.setMinHorizontalRecovery(val);
		stale(true);
	}

	/** 
	 * Set minimum horizontal separation for recovery bands in specified units [u].
	 */
	public void setMinHorizontalRecovery(double val, String u) {
		core_.parameters.setMinHorizontalRecovery(val,u);
		stale(true);
	}

	/**
	 * Sets minimum vertical separation for recovery bands in internal units [m].
	 */
	public void setMinVerticalRecovery(double val) {
		core_.parameters.setMinVerticalRecovery(val);
		stale(true);
	}

	/** 
	 * Set minimum vertical separation for recovery bands in units
	 */
	public void setMinVerticalRecovery(double val, String u) {
		core_.parameters.setMinVerticalRecovery(val,u);
		stale(true);
	}

	/** 
	 * @return true if repulsive criteria is enabled for conflict bands.
	 */
	public boolean isEnabledConflictCriteria() {
		return core_.parameters.isEnabledConflictCriteria();
	}

	/** 
	 * Enable/disable repulsive criteria for conflict bands.
	 */
	public void setConflictCriteria(boolean flag) {
		core_.parameters.setConflictCriteria(flag);
		stale(true);
	}

	/** 
	 * Enable repulsive criteria for conflict bands.
	 */
	public void enableConflictCriteria() {
		setConflictCriteria(true);
	}

	/** 
	 * Disable repulsive criteria for conflict bands.
	 */
	public void disableConflictCriteria() {
		setConflictCriteria(false);
	}

	/** 
	 * @return true if repulsive criteria is enabled for recovery bands.
	 */
	public boolean isEnabledRecoveryCriteria() {
		return core_.parameters.isEnabledRecoveryCriteria();
	}

	/** 
	 * Enable/disable repulsive criteria for recovery bands.
	 */
	public void setRecoveryCriteria(boolean flag) {
		core_.parameters.setRecoveryCriteria(flag);
		stale(true);
	}

	/** 
	 * Enable repulsive criteria for recovery bands.
	 */
	public void enableRecoveryCriteria() {
		setRecoveryCriteria(true);
	}

	/** 
	 * Disable repulsive criteria for recovery bands.
	 */
	public void disableRecoveryCriteria() {
		setRecoveryCriteria(false);
	}

	/** 
	 * Enable/disable repulsive criteria for conflict and recovery bands.
	 */
	public void setRepulsiveCriteria(boolean flag) {
		setConflictCriteria(flag);
		setRecoveryCriteria(flag);
	}

	/** 
	 * Enable repulsive criteria for conflict and recovery bands.
	 */
	public void enableRepulsiveCriteria() {
		setRepulsiveCriteria(true);
	}

	/** 
	 * Disable repulsive criteria for conflict and recovery bands.
	 */
	public void disableRepulsiveCriteria() {
		setRepulsiveCriteria(false);
	}

	/**
	 * @return true if recovery direction bands are enabled.
	 */
	public boolean isEnabledRecoveryHorizontalDirectionBands() {
		return core_.parameters.isEnabledRecoveryHorizontalDirectionBands();
	}

	/**
	 * @return true if recovery horizontal speed bands are enabled.
	 */
	public boolean isEnabledRecoveryHorizontalSpeedBands() {
		return core_.parameters.isEnabledRecoveryHorizontalSpeedBands();
	}

	/**
	 * @return true if recovery vertical speed bands are enabled.
	 */
	public boolean isEnabledRecoveryVerticalSpeedBands() {
		return core_.parameters.isEnabledRecoveryVerticalSpeedBands();
	}

	/**
	 * @return true if recovery altitude bands are enabled.
	 */
	public boolean isEnabledRecoveryAltitudeBands() {
		return core_.parameters.isEnabledRecoveryAltitudeBands();
	}

	/** 
	 * Sets recovery bands flag for direction, horizontal speed, and vertical speed bands to specified value.
	 */ 
	public void setRecoveryBands(boolean flag) {
		setRecoveryHorizontalDirectionBands(flag);
		setRecoveryHorizontalSpeedBands(flag);
		setRecoveryVerticalSpeedBands(flag);
		setRecoveryAltitudeBands(flag);
	}

	/** 
	 * Enable all recovery bands for direction, horizontal speed, vertical speed, and altitude.
	 */ 
	public void enableRecoveryBands() {
		setRecoveryBands(true);
	}

	/** 
	 * Disable all recovery bands for direction, horizontal speed, vertical speed, and altitude.
	 */ 
	public void disableRecoveryBands() {
		setRecoveryBands(false);
	}

	/** 
	 * Sets recovery bands flag for direction bands to specified value.
	 */ 
	public void setRecoveryHorizontalDirectionBands(boolean flag) {
		core_.parameters.setRecoveryHorizontalDirectionBands(flag);
		hdir_band_.set_recovery(flag);
	}

	/** 
	 * Sets recovery bands flag for horizontal speed bands to specified value.
	 */ 
	public void setRecoveryHorizontalSpeedBands(boolean flag) {
		core_.parameters.setRecoveryHorizontalSpeedBands(flag);
		hs_band_.set_recovery(flag);
	}

	/** 
	 * Sets recovery bands flag for vertical speed bands to specified value.
	 */ 
	public void setRecoveryVerticalSpeedBands(boolean flag) {
		core_.parameters.setRecoveryVerticalSpeedBands(flag);
		vs_band_.set_recovery(flag);
	}

	/** 
	 * Sets recovery bands flag for altitude bands to specified value.
	 */ 
	public void setRecoveryAltitudeBands(boolean flag) {
		core_.parameters.setRecoveryAltitudeBands(flag);
		alt_band_.set_recovery(flag);
	}

	/** 
	 * @return true if collision avoidance bands are enabled.
	 */
	public boolean isEnabledCollisionAvoidanceBands() {
		return core_.parameters.isEnabledCollisionAvoidanceBands();
	}

	/** 
	 * Enable/disable collision avoidance bands.
	 */ 
	public void setCollisionAvoidanceBands(boolean flag) {
		core_.parameters.setCollisionAvoidanceBands(flag);
		stale(true);
	}

	/** 
	 * Enable collision avoidance bands.
	 */ 
	public void enableCollisionAvoidanceBands() {
		setCollisionAvoidanceBands(true);
	}

	/** 
	 * Disable collision avoidance bands.
	 */ 
	public void disableCollisionAvoidanceBands() {
		setCollisionAvoidanceBands(false);
	}

	/** 
	 * @return get factor for computing collision avoidance bands. Factor value is in (0,1]
	 */
	public double getCollisionAvoidanceBandsFactor() {
		return core_.parameters.getCollisionAvoidanceBandsFactor();
	}

	/** 
	 * @return set factor for computing collision avoidance bands. Factor value is in (0,1]
	 */
	public void setCollisionAvoidanceBandsFactor(double val) {
		core_.parameters.setCollisionAvoidanceBandsFactor(val);
		stale(true);
	}

	/** 
	 * @return get z-score (number of standard deviations) for horizontal position 
	 */
	public double getHorizontalPositionZScore() {
		return core_.parameters.getHorizontalPositionZScore();
	}

	/** 
	 * @return set z-score (number of standard deviations) for horizontal position (non-negative value)
	 */
	public void setHorizontalPositionZScore(double val) {
		core_.parameters.setHorizontalPositionZScore(val);
		stale(true);
	}

	/** 
	 * @return get min z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMin() {
		return core_.parameters.getHorizontalVelocityZScoreMin();
	}

	/** 
	 * @return set min z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMin(double val) {
		core_.parameters.setHorizontalVelocityZScoreMin(val);
		stale(true);
	}

	/** 
	 * @return get max z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMax() {
		return core_.parameters.getHorizontalVelocityZScoreMax();
	}

	/** 
	 * @return set max z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMax(double val) {
		core_.parameters.setHorizontalVelocityZScoreMax(val);
		stale(true);
	}

	/** 
	 * @return Distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public double getHorizontalVelocityZDistance() {
		return core_.parameters.getHorizontalVelocityZDistance();
	}

	/** 
	 * @return Distance (in given units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public double getHorizontalVelocityZDistance(String u) {
		return core_.parameters.getHorizontalVelocityZDistance(u);
	}

	/** 
	 * @return Set distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val) {
		core_.parameters.setHorizontalVelocityZDistance(val);
		stale(true);
	}

	/** 
	 * @return Set distance (in given units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val, String u) {
		core_.parameters.setHorizontalVelocityZDistance(val,u);
		stale(true);
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical position 
	 */
	public double getVerticalPositionZScore() {
		return core_.parameters.getVerticalPositionZScore();
	}

	/** 
	 * @return set z-score (number of standard deviations) for vertical position (non-negative value)
	 */
	public void setVerticalPositionZScore(double val) {
		core_.parameters.setVerticalPositionZScore(val);
		stale(true);
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical velocity 
	 */
	public double getVerticalSpeedZScore() {
		return core_.parameters.getVerticalSpeedZScore();
	}

	/** 
	 * @return set z-score (number of standard deviations) for vertical velocity (non-negative value)
	 */
	public void setVerticalSpeedZScore(double val) {
		core_.parameters.setVerticalSpeedZScore(val);
		stale(true);
	}

	/** 
	 * Get horizontal contour threshold, specified in internal units [rad] as an angle to 
	 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
	 * A value of pi means all contours.
	 */
	public double getHorizontalContourThreshold() {
		return core_.parameters.getHorizontalContourThreshold();
	}

	/** 
	 * Get horizontal contour threshold, specified in given units [u] as an angle to 
	 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
	 * A value of pi means all contours.
	 */
	public double getHorizontalContourThreshold(String u) {
		return core_.parameters.getHorizontalContourThreshold(u);
	}

	/** 
	 * Set horizontal contour threshold, specified in internal units [rad] [0 - pi] as an angle to 
	 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
	 * A value of pi means all contours.
	 */
	public void setHorizontalContourThreshold(double val) {
		core_.parameters.setHorizontalContourThreshold(val);
	}

	/** 
	 * Set horizontal contour threshold, specified in given units [u] [0 - pi] as an angle to 
	 * the left/right of current aircraft direction. A value of 0 means only conflict contours. 
	 * A value of pi means all contours.
	 */
	public void setHorizontalContourThreshold(double val, String u) {
		core_.parameters.setHorizontalContourThreshold(val,u);
	}

	/**
	 * Set alerting logic to the value indicated by ownship_centric.
	 * If ownship_centric is true, alerting and guidance logic will use the alerter in ownship. Alerter 
	 * in every intruder will be disregarded.
	 * If ownship_centric is false, alerting and guidance logic will use the alerter in every intruder. Alerter
	 * in ownship will be disregarded.
	 */
	public void setAlertingLogic(boolean ownship_centric) {
		core_.parameters.setAlertingLogic(ownship_centric);
		stale(true);
	}

	/**
	 * Set alerting and guidance logic to ownship-centric. Alerting and guidance logic will use the alerter in ownship. 
	 * Alerter in every intruder will be disregarded.	 
	 */
	public void setOwnshipCentricAlertingLogic() {
		setAlertingLogic(true);
	}

	/**
	 * Set alerting and guidance logic to intruder-centric. Alerting and guidance logic will use the alerter in every intruder. 
	 * Alerter in ownship will be disregarded.	 
	 */
	public void setIntruderCentricAlertingLogic() {
		setAlertingLogic(false);		
	}

	/**
	 * @return true if alerting/guidance logic is ownship centric. 
	 */
	public boolean isAlertingLogicOwnshipCentric() {
		return core_.parameters.isAlertingLogicOwnshipCentric();
	}

	/** 
	 * Get corrective region for calculation of resolution maneuvers and bands saturation.
	 */
	public BandsRegion getCorrectiveRegion() {
		return core_.parameters.getCorrectiveRegion();
	}

	/** 
	 * Set corrective region for calculation of resolution maneuvers and bands saturation.
	 */
	public void setCorrectiveRegion(BandsRegion val) {
		core_.parameters.setCorrectiveRegion(val);
		stale(true);
	}

	/**
	 * @param alerter_idx Indice of an alerter (starting from 1)
	 * @return corrective level of alerter at alerter_idx. The corrective level 
	 * is the first alert level that has a region equal to or more severe than corrective_region.
	 * Return -1 if alerter_idx is out of range. Return 0 is there is no corrective alert level
	 * for this alerter. 
	 */
	public int correctiveAlertLevel(int alerter_idx) {
		return core_.parameters.correctiveAlertLevel(alerter_idx);
	}

	/**
	 * @return maximum alert level for all alerters. Returns 0 if alerter list is empty.
	 */
	public int maxAlertLevel() {
		return core_.parameters.maxAlertLevel();
	}

	/** 
	 * Set instantaneous bands.
	 */
	public void setInstantaneousBands() {
		core_.parameters.setInstantaneousBands();
		hdir_band_.setDaidalusParameters(core_.parameters);
		hs_band_.setDaidalusParameters(core_.parameters);
		vs_band_.setDaidalusParameters(core_.parameters);
		alt_band_.setDaidalusParameters(core_.parameters);		
	}

	/** 
	 * Set kinematic bands.
	 * Set turn rate to 3 deg/s, when type is true; set turn rate to  1.5 deg/s
	 * when type is false;
	 */
	public void setKinematicBands(boolean type) {
		core_.parameters.setKinematicBands(type);
		hdir_band_.setDaidalusParameters(core_.parameters);
		hs_band_.setDaidalusParameters(core_.parameters);
		vs_band_.setDaidalusParameters(core_.parameters);
		alt_band_.setDaidalusParameters(core_.parameters);
	}

	/** 
	 * Disable hysteresis parameters
	 */
	public void disableHysteresis() {
		core_.parameters.disableHysteresis();
		stale(true);
	}

	/**
	 *  Load parameters from file.
	 */
	public boolean loadFromFile(String file) {
		boolean flag = core_.parameters.loadFromFile(file);
		core_.stale(true);
		hdir_band_.setDaidalusParameters(core_.parameters);
		hs_band_.setDaidalusParameters(core_.parameters);
		vs_band_.setDaidalusParameters(core_.parameters);
		alt_band_.setDaidalusParameters(core_.parameters);
		return flag;
	}

	/**
	 *  Write parameters to file.
	 */
	public boolean saveToFile(String file) {
		return core_.parameters.saveToFile(file);
	}

	/**
	 * Set bands parameters
	 */
	public void setDaidalusParameters(DaidalusParameters parameters) {
		core_.parameters.copyFrom(parameters);
		core_.stale(true);
		hdir_band_.setDaidalusParameters(parameters);
		hs_band_.setDaidalusParameters(parameters);
		vs_band_.setDaidalusParameters(parameters);
		alt_band_.setDaidalusParameters(parameters);
	}

	public void setParameterData(ParameterData p) {
		core_.setParameterData(p);
		hdir_band_.setDaidalusParameters(core_.parameters);
		hs_band_.setDaidalusParameters(core_.parameters);
		vs_band_.setDaidalusParameters(core_.parameters);
		alt_band_.setDaidalusParameters(core_.parameters);
	}

	public ParameterData getParameterData() {
		return core_.parameters.getParameters();
	}

	/** 
	 * Get string units of parameters key
	 */
	public String getUnitsOf(String key) {
		return core_.parameters.getUnitsOf(key);
	}

	/* Direction Bands Settings */

	/** 
	 * Set absolute min/max directions for bands computations. Directions are specified in internal units [rad].
	 * Values are expected to be in [0 - 2pi)
	 */
	public void setAbsoluteHorizontalDirectionBands(double min, double max) {
		min = Util.to_2pi(min);
		max = Util.to_2pi(max);
		hdir_band_.set_min_max_mod(min,max);
	}

	/** 
	 * Set absolute min/max directions for bands computations. Directions are specified in given units [u].
	 * Values are expected to be in [0 - 2pi) [u]
	 */
	public void setAbsoluteHorizontalDirectionBands(double min, double max, String u) {
		setAbsoluteHorizontalDirectionBands(Units.from(u,min),Units.from(u,max));
	}

	/* Utility methods */

	/** 
	 * Return core object of bands. For expert users only
	 * DO NOT USE IT, UNLESS YOU KNOW WHAT YOU ARE DOING. EXPERT USE ONLY !!!
	 */
	public DaidalusCore getCore() {
		return core_;
	}

	/**
	 *  Clear ownship and traffic data from this object.   
	 */
	public void clear() {
		core_.clear();
		hdir_band_.stale(false);
		hs_band_.stale(false);
		vs_band_.stale(false);
		alt_band_.stale(false);
	}

	/**
	 * Set cached values to stale conditions as they are no longer fresh.
	 * If hysteresis is true, it also clears hysteresis variables
	 */
	public void stale(boolean hysteresis) {
		core_.stale(hysteresis);
		hdir_band_.stale(hysteresis);
		hs_band_.stale(hysteresis);
		vs_band_.stale(hysteresis);
		alt_band_.stale(hysteresis);
	}

	/**
	 * Returns true is Daidalus object is fresh
	 */
	public boolean isFresh() {
		return core_.isFresh() && hdir_band_.isFresh() && 
				hs_band_.isFresh() && vs_band_.isFresh() && alt_band_.isFresh();
	}
	
	/**
	 * Refresh Daidalus object
	 */
	public void refresh() {
		core_.refresh();
		hdir_band_.refresh(core_);
		hs_band_.refresh(core_);
		vs_band_.refresh(core_);
		alt_band_.refresh(core_);
	}
	
	/* Main interface methods */

	/**
	 * Compute in acs list of aircraft identifiers contributing to conflict bands for given region.
	 */
	public void conflictBandsAircraft(List<String> acs, BandsRegion region) {
		conflictBandsAircraft(acs,region.orderOfConflictRegion());
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to conflict bands for given region.
	 * 1=FAR, 2=MID, 2=NEAR
	 */
	public void conflictBandsAircraft(List<String> acs, int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			IndexLevelT.toStringList(acs,
					core_.acs_conflict_bands(BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Return time interval of violation for given bands region
	 */
	public Interval timeIntervalOfConflict(BandsRegion region) {
		return timeIntervalOfConflict(region.orderOfConflictRegion());
	}

	/**
	 * Return time interval of conflict for given bands region
	 */
	public Interval timeIntervalOfConflict(int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			return core_.tiov(BandsRegion.NUMBER_OF_CONFLICT_BANDS-region);
		}
		return Interval.EMPTY;
	}

	/** 
	 * @return the number of horizontal direction bands negative if the ownship has not been set
	 */
	public int horizontalDirectionBandsLength() {
		return hdir_band_.length(core_);
	}

	/** 
	 * Force computation of direction bands. Usually, bands are only computed when needed. This method
	 * forces the computation of direction bands (this method is included mainly for debugging purposes). 
	 */
	public void forceHorizontalDirectionBandsComputation() {
		hdir_band_.force_compute(core_);
	}

	/**
	 * @return the i-th interval, in internal units [rad], of the computed direction bands.
	 * @param i index
	 */
	public Interval horizontalDirectionIntervalAt(int i) {
		return hdir_band_.interval(core_,i);
	}

	/**
	 * @return the i-th interval, in specified units [u], of the computed direction bands.
	 * @param i index
	 * @param u units
	 */
	public Interval horizontalDirectionIntervalAt(int i, String u) {
		Interval ia = hdir_band_.interval(core_,i);
		if (ia.isEmpty()) {
			return ia;
		}
		return new Interval(Units.to(u, ia.low), Units.to(u, ia.up));
	}

	/**
	 * @return the i-th region of the computed direction bands.
	 * @param i index
	 */
	public BandsRegion horizontalDirectionRegionAt(int i) {
		return hdir_band_.region(core_,i);
	}

	/**
	 * @return the index of a given direction specified in internal units [rad]
	 * @param dir [rad]
	 */
	public int indexOfHorizontalDirection(double dir) {
		return hdir_band_.indexOf(core_,dir);
	}

	/**
	 * @return the index of a given direction specified in given units [u]
	 * @param dir [u]
	 * @param u Units
	 */
	public int indexOfHorizontalDirection(double dir, String u) {
		return indexOfHorizontalDirection(Units.from(u, dir));
	}

	/**
	 * @return the region of a given direction specified in internal units [rad].
	 * @param dir [rad]
	 */
	public BandsRegion regionOfHorizontalDirection(double dir) {
		return horizontalDirectionRegionAt(indexOfHorizontalDirection(dir));
	}

	/**
	 * @return the region of a given direction specified in given units [u]
	 * @param dir [u]
	 * @param u Units
	 */
	public BandsRegion regionOfHorizontalDirection(double dir, String u) {
		return horizontalDirectionRegionAt(indexOfHorizontalDirection(dir,u));
	}

	/**
	 * Return last time to direction maneuver, in seconds, for ownship with respect to traffic
	 * aircraft ac. Return NaN if the ownship is not in conflict with aircraft ac within 
	 * lookahead time. Return negative infinity if there is no time to maneuver.
	 */
	public double lastTimeToHorizontalDirectionManeuver(TrafficState ac) {
		return hdir_band_.last_time_to_maneuver(core_,ac);
	}

	/**
	 * @return recovery information for horizontal direction bands.
	 */
	public RecoveryInformation horizontalDirectionRecoveryInformation() {
		return hdir_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal direction bands 
	 * for given region.
	 */
	public void peripheralHorizontalDirectionBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralHorizontalDirectionBandsAircraft(acs,region.orderOfConflictRegion());
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal direction bands
	 * for given region. 1=FAR, 2=MID, 3=NEAR
	 */
	public void peripheralHorizontalDirectionBandsAircraft(List<String> acs, int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			IndexLevelT.toStringList(acs,hdir_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute horizontal direction resolution maneuver for a given direction.
	 * @parameter dir is right (true)/left (false) of ownship current direction
	 * @return direction resolution in internal units [rad] in specified direction.
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionResolution(boolean dir) {
		return hdir_band_.resolution(core_,dir);
	}

	/**
	 * Compute horizontal direction resolution maneuver for a given direction.
	 * @parameter dir is right (true)/left (false) of ownship current direction
	 * @parameter u units
	 * @return direction resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionResolution(boolean dir, String u) {
		return Units.to(u,horizontalDirectionResolution(dir));
	}

	/**
	 * Compute preferred horizontal direction based on resolution that is closer to current direction.
	 * @return True: Right. False: Left. 
	 */
	public boolean preferredHorizontalDirectionRightOrLeft() {
		return hdir_band_.preferred_direction(core_);
	}

	/**
	 * Compute horizontal direction resolution region for a given direction.
	 * @parameter dir is right (true)/left (false) of ownship current direction
	 * @return Bands region of horizontal direction resolution for given direction
	 */
	public BandsRegion horizontalDirectionResolutionRegion(boolean dir) {
		return hdir_band_.resolution_region(core_,dir);
	}

	/**
	 * @return the number of horizontal speed band intervals, negative if the ownship has not been set
	 */
	public int horizontalSpeedBandsLength() {
		return hs_band_.length(core_);
	}

	/** 
	 * Force computation of horizontal speed bands. Usually, bands are only computed when needed. This method
	 * forces the computation of horizontal speed bands (this method is included mainly for debugging purposes). 
	 */
	public void forceHorizontalSpeedBandsComputation() {
		hs_band_.force_compute(core_);
	}

	/**
	 * @return the i-th interval, in internal units [m/s], of the computed horizontal speed bands.
	 * @param i index
	 */
	public Interval horizontalSpeedIntervalAt(int i) {
		return hs_band_.interval(core_,i);
	}

	/**
	 * @return the i-th interval, in specified units [u], of the computed horizontal speed bands.
	 * @param i index
	 * @param u units
	 */
	public Interval horizontalSpeedIntervalAt(int i, String u) {
		Interval ia = hs_band_.interval(core_,i);
		if (ia.isEmpty()) {
			return ia;
		}
		return new Interval(Units.to(u, ia.low), Units.to(u, ia.up));
	}

	/**
	 * @return the i-th region of the computed horizontal speed bands.
	 * @param i index
	 */
	public BandsRegion horizontalSpeedRegionAt(int i) {
		return hs_band_.region(core_,i);
	}

	/**
	 * @return the range index of a given horizontal speed specified in internal units [m/s]
	 * @param gs [m/s]
	 */
	public int indexOfHorizontalSpeed(double gs) {
		return hs_band_.indexOf(core_,gs);
	}

	/**
	 * @return the range index of a given horizontal speed specified in given units [u]
	 * @param gs [u]
	 * @param u Units
	 */
	public int indexOfHorizontalSpeed(double gs, String u) {
		return indexOfHorizontalSpeed(Units.from(u,gs));
	}

	/**
	 * @return the region of a given horizontal speed specified in internal units [m/s]
	 * @param gs [m/s]
	 */
	public BandsRegion regionOfHorizontalSpeed(double gs) {
		return horizontalSpeedRegionAt(indexOfHorizontalSpeed(gs));
	}

	/**
	 * @return the region of a given horizontal speed specified in given units [u]
	 * @param gs [u]
	 * @param u Units
	 */
	public BandsRegion regionOfHorizontalSpeed(double gs, String u) {
		return horizontalSpeedRegionAt(indexOfHorizontalSpeed(gs,u));
	}

	/**
	 * Return last time to horizontal speed maneuver, in seconds, for ownship with respect to traffic
	 * aircraft ac. Return NaN if the ownship is not in conflict with aircraft ac within 
	 * lookahead time. Return negative infinity if there is no time to maneuver.
	 */
	public double lastTimeToHorizontalSpeedManeuver(TrafficState ac) {
		return hs_band_.last_time_to_maneuver(core_,ac);
	}

	/**
	 * @return recovery information for horizontal speed bands.
	 */
	public RecoveryInformation horizontalSpeedRecoveryInformation() {
		return hs_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal speed bands 
	 * for given region.
	 */
	public void peripheralHorizontalSpeedBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralHorizontalSpeedBandsAircraft(acs,region.orderOfConflictRegion());
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal speed bands
	 * for given region. 1=FAR, 2=MID, 3=NEAR
	 */
	public void peripheralHorizontalSpeedBandsAircraft(List<String> acs, int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			IndexLevelT.toStringList(acs,hs_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute horizontal speed resolution maneuver.
	 * @parameter dir is up (true)/down (false) of ownship current horizontal speed
	 * @return horizontal speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedResolution(boolean dir) {
		return hs_band_.resolution(core_,dir);
	}

	/**
	 * Compute horizontal speed resolution maneuver for corrective region.
	 * @parameter dir is up (true)/down (false) of ownship current horizontal speed
	 * @parameter u units
	 * @return horizontal speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedResolution(boolean dir, String u) {
		return Units.to(u,horizontalSpeedResolution(dir));
	}

	/**
	 * Compute preferred horizontal speed direction on resolution that is closer to current horizontal speed.
	 * True: Increase speed, False: Decrease speed.
	 */
	public boolean preferredHorizontalSpeedUpOrDown() {
		return hs_band_.preferred_direction(core_);
	}

	/**
	 * Compute horizontal speed resolution region for a given direction.
	 * @parameter dir is up (true)/down (false) of ownship current horizontal speed
	 * @return Bands region of horizontal speed resolution for given direction
	 */
	public BandsRegion horizontalSpeedResolutionRegion(boolean dir) {
		return hs_band_.resolution_region(core_,dir);
	}

	/**
	 * @return the number of vertical speed band intervals, negative if the ownship has not been set
	 */
	public int verticalSpeedBandsLength() {
		return vs_band_.length(core_);
	}

	/** 
	 * Force computation of vertical speed bands. Usually, bands are only computed when needed. This method
	 * forces the computation of vertical speed bands (this method is included mainly for debugging purposes). 
	 */
	public void forceVerticalSpeedBandsComputation() {
		vs_band_.force_compute(core_);
	}

	/**
	 * @return the i-th interval, in internal units [m/s], of the computed vertical speed bands.
	 * @param i index
	 */
	public Interval verticalSpeedIntervalAt(int i) {
		return vs_band_.interval(core_,i);
	}

	/**
	 * @return the i-th interval, in specified units [u], of the computed vertical speed bands.
	 * @param i index
	 * @param u units
	 */
	public Interval verticalSpeedIntervalAt(int i, String u) {
		Interval ia = vs_band_.interval(core_,i);
		if (ia.isEmpty()) {
			return ia;
		}
		return new Interval(Units.to(u, ia.low), Units.to(u, ia.up));
	}

	/**
	 * @return the i-th region of the computed vertical speed bands.
	 * @param i index
	 */
	public BandsRegion verticalSpeedRegionAt(int i) {
		return vs_band_.region(core_,i);
	}

	/**
	 * @return the region of a given vertical speed specified in internal units [m/s]
	 * @param vs [m/s]
	 */
	public int indexOfVerticalSpeed(double vs) {
		return vs_band_.indexOf(core_,vs);
	}

	/**
	 * @return the region of a given vertical speed specified in given units [u]
	 * @param vs [u]
	 * @param u Units
	 */
	public int indexOfVerticalSpeed(double vs, String u) {
		return indexOfVerticalSpeed(Units.from(u, vs));
	}

	/**
	 * @return the region of a given vertical speed specified in internal units [m/s]
	 * @param vs [m/s]
	 */
	public BandsRegion regionOfVerticalSpeed(double vs) {
		return verticalSpeedRegionAt(indexOfVerticalSpeed(vs));
	}

	/**
	 * @return the region of a given vertical speed specified in given units [u]
	 * @param vs [u]
	 * @param u Units
	 */
	public BandsRegion regionOfVerticalSpeed(double vs, String u) {
		return verticalSpeedRegionAt(indexOfVerticalSpeed(vs,u));
	}

	/**
	 * Return last time to vertical speed maneuver, in seconds, for ownship with respect to traffic
	 * aircraft ac. Return NaN if the ownship is not in conflict with aircraft ac within 
	 * lookahead time. Return negative infinity if there is no time to maneuver.
	 */
	public double lastTimeToVerticalSpeedManeuver(TrafficState ac) {
		return vs_band_.last_time_to_maneuver(core_,ac);
	}

	/**
	 * @return recovery information for vertical speed bands.
	 */
	public RecoveryInformation verticalSpeedRecoveryInformation() {
		return vs_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral vertical speed bands 
	 * for given region.
	 */
	public void peripheralVerticalSpeedBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralVerticalSpeedBandsAircraft(acs,region.orderOfConflictRegion());
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral vertical speed bands
	 * for given region. 1=FAR, 2=MID, 3=NEAR
	 */
	public void peripheralVerticalSpeedBandsAircraft(List<String> acs, int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			IndexLevelT.toStringList(acs,vs_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute vertical speed resolution maneuver for given direction.
	 * @parameter dir is up (true)/down (false) of ownship current vertical speed
	 * @return vertical speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedResolution(boolean dir) {
		return vs_band_.resolution(core_,dir);
	}

	/**
	 * Compute vertical speed resolution maneuver for given direction.
	 * @parameter dir is up (true)/down (false) of ownship current vertical speed
	 * @parameter u units
	 * @return vertical speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedResolution(boolean dir, String u) {
		return Units.to(u,verticalSpeedResolution(dir));
	}

	/**
	 * Compute preferred  vertical speed direction based on resolution that is closer to current vertical speed.
	 * True: Increase speed, False: Decrease speed.
	 */
	public boolean preferredVerticalSpeedUpOrDown() {
		return vs_band_.preferred_direction(core_);
	}

	/**
	 * Compute vertical speed resolution region for a given direction.
	 * @parameter dir is up (true)/down (false) of ownship current vertical speed
	 * @return Bands region of vertical speed resolution for given direction
	 */
	public BandsRegion verticalSpeedResolutionRegion(boolean dir) {
		return vs_band_.resolution_region(core_,dir);
	}

	/**
	 * @return the number of altitude band intervals, negative if the ownship has not been set.
	 */
	public int altitudeBandsLength() {
		return alt_band_.length(core_);
	}

	/** 
	 * Force computation of altitude bands. Usually, bands are only computed when needed. This method
	 * forces the computation of altitude bands (this method is included mainly for debugging purposes). 
	 */
	public void forceAltitudeBandsComputation() {
		alt_band_.force_compute(core_);
	}

	/**
	 * @return the i-th interval, in internal units [m], of the computed altitude bands.
	 * @param i index
	 */
	public Interval altitudeIntervalAt(int i) {
		return alt_band_.interval(core_,i);
	}

	/**
	 * @return the i-th interval, in specified units [u], of the computed altitude bands.
	 * @param i index
	 * @param u units
	 */
	public Interval altitudeIntervalAt(int i, String u) {
		Interval ia = alt_band_.interval(core_,i);
		if (ia.isEmpty()) {
			return ia;
		}
		return new Interval(Units.to(u, ia.low), Units.to(u, ia.up));
	}

	/**
	 * @return the i-th region of the computed altitude bands.
	 * @param i index
	 */
	public BandsRegion altitudeRegionAt(int i) {
		return alt_band_.region(core_,i);
	}

	/**
	 * @return the range index of a given altitude specified internal units [m]
	 * @param alt [m]
	 */
	public int indexOfAltitude(double alt) {
		return alt_band_.indexOf(core_,alt);
	}

	/**
	 * @return the range index of a given altitude specified in given units [u]
	 * @param alt [u]
	 * @param u Units
	 */
	public int indexOfAltitude(double alt, String u) {
		return indexOfAltitude(Units.from(u,alt));
	}

	/**
	 * @return the region of a given altitude specified in internal units [m]
	 * @param alt [m]
	 */
	public BandsRegion regionOfAltitude(double alt) {
		return altitudeRegionAt(indexOfAltitude(alt));
	}

	/**
	 * @return the region of a given altitude specified in given units [u]
	 * @param alt [u]
	 * @param u Units
	 */
	public BandsRegion regionOfAltitude(double alt, String u) {
		return altitudeRegionAt(indexOfAltitude(alt,u));
	}

	/**
	 * Return last time to altitude maneuver, in seconds, for ownship with respect to traffic
	 * aircraft ac. Return NaN if the ownship is not in conflict with aircraft ac within 
	 * lookahead time. Return negative infinity if there is no time to maneuver.
	 */
	public double lastTimeToAltitudeManeuver(TrafficState ac) {
		return alt_band_.last_time_to_maneuver(core_,ac);
	}

	/**
	 * @return recovery information for altitude speed bands.
	 */
	public RecoveryInformation altitudeRecoveryInformation() {
		return alt_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral altitude bands 
	 * for given region.
	 */
	public void peripheralAltitudeBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralAltitudeBandsAircraft(acs,region.orderOfConflictRegion());
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral altitude bands
	 * for given region. 1=FAR, 2=MID, 3=NEAR
	 */
	public void peripheralAltitudeBandsAircraft(List<String> acs, int region) {
		if (BandsRegion.FAR.orderOfConflictRegion() <= region && region <= BandsRegion.NEAR.orderOfConflictRegion()) {
			IndexLevelT.toStringList(acs,alt_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute altitude resolution maneuver for given direction.
	 * @parameter dir is up (true)/down (false) of ownship current altitude
	 * @return altitude resolution in internal units [m] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeResolution(boolean dir) {
		return alt_band_.resolution(core_,dir);
	}

	/**
	 * Compute altitude resolution maneuver for given direction.
	 * @parameter dir is up (true)/down (false) of ownship current altitude
	 * @parameter u units
	 * @return altitude resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for early alerting time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeResolution(boolean dir, String u) {
		return Units.to(u,altitudeResolution(dir));
	}

	/**
	 * Compute preferred  altitude direction on resolution that is closer to current altitude.
	 * True: Climb, False: Descend.
	 */
	public boolean preferredAltitudeUpOrDown() {
		return alt_band_.preferred_direction(core_);
	}

	/**
	 * Compute altitude resolution region for a given direction.
	 * @parameter dir is up (true)/down (false) of ownship current altitude
	 * @return Bands region of altitude resolution for given direction
	 */
	public BandsRegion altitudeResolutionRegion(boolean dir) {
		return alt_band_.resolution_region(core_,dir);
	}

	/*
	 * Alerting logic
	 */

	/** 
	 * Computes alerting type of ownship and aircraft at index ac_idx for current 
	 * aircraft states.  The number 0 means no alert. A negative number means
	 * that aircraft index is not valid.
	 * When the alerter object has been configured to consider ownship maneuvers, i.e.,
	 * using spread values, the alerting logic could also use information about the ownship
	 * turning, accelerating, and climbing status as follows:
	 * - turning < 0: ownship is turning left, turning > 0: ownship is turning right, turning = 0: 
	 * do not make any turning assumption about the ownship.
	 * - accelerating < 0: ownship is decelerating, accelerating > 0: ownship is accelerating, 
	 * accelerating = 0: do not make any accelerating assumption about the ownship.
	 * - climbing < 0: ownship is descending, climbing > 0: ownship is climbing, climbing = 0:
	 * do not make any climbing assumption about the ownship.
	 */
	public int alertLevel(int ac_idx, int turning, int accelerating, int climbing) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return core_.alert_level(ac_idx-1,turning,accelerating,climbing);
		} else {
			error.addError("alertLevel: aircraft index "+ac_idx+" is out of bounds");
			return -1;
		} 
	}

	/** 
	 * Computes alert level of ownship and aircraft at index ac_idx. 
	 * The number 0 means no alert. A negative number means
	 * that aircraft index is not valid.
	 */
	public int alertLevel(int ac_idx) {
		return alertLevel(ac_idx,0,0,0);
	}

	@Deprecated
	/**
	 * This method is deprecated. It is replaced by alertLevel 
	 */
	public int alerting(int ac_idx) {
		return alertLevel(ac_idx);
	}

	/**
	 * Detects violation of alert thresholds for a given alert level with an
	 * aircraft at index ac_idx.
	 * Conflict data provides time to violation and time to end of violation 
	 * of alert thresholds of given alert level. 
	 * @param ac_idx is the index of the traffic aircraft 
	 * @param alert_level alert level used to compute detection. The value 0
	 * indicate the alert volume of the corrective region.
	 */
	public ConflictData violationOfAlertThresholds(int ac_idx, int alert_level) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			TrafficState intruder = core_.traffic.get(ac_idx-1);
			int alerter_idx = core_.alerter_index_of(ac_idx-1);
			if (1 <= alerter_idx && alerter_idx <= core_.parameters.numberOfAlerters()) {
				Alerter alerter = core_.parameters.getAlerterAt(alerter_idx);
				if (alert_level == 0) {
					alert_level = core_.parameters.correctiveAlertLevel(alerter_idx);
				}
				if (alert_level > 0) {
					Optional<Detection3D> detector = alerter.getDetector(alert_level);
					if (detector.isPresent()) {
						double s_err = intruder.relativeHorizontalPositionError(core_.ownship,core_.parameters);
						double sz_err = intruder.relativeVerticalPositionError(core_.ownship,core_.parameters);
						double v_err = intruder.relativeHorizontalSpeedError(core_.ownship,s_err,core_.parameters);
						double vz_err = intruder.relativeVerticalSpeedError(core_.ownship,core_.parameters);
						return detector.get().conflictDetectionSUM(core_.ownship.get_s(),core_.ownship.get_v(),intruder.get_s(),intruder.get_v(),
								0,core_.parameters.getLookaheadTime(),
								s_err,sz_err,v_err,vz_err);
					} else {
						error.addError("violationOfAlertThresholds: detector of traffic aircraft "+ac_idx+" is not set");
					}
				} else {
					error.addError("violationOfAlertThresholds: no corrective alerter level for alerter of "+ac_idx);
				}
			} else {
				error.addError("violationOfAlertThresholds: alerter of traffic aircraft "+ac_idx+" is out of bounds");
			}
		} else {
			error.addError("violationOfAlertThresholds: aircraft index "+ac_idx+" is out of bounds");
		}
		return ConflictData.EMPTY;	
	}

	/**
	 * Detects violation of corrective thresholds with an aircraft at index ac_idx.
	 * Conflict data provides time to violation and time to end of violation 
	 * @param ac_idx is the index of the traffic aircraft 
	 */
	public ConflictData violationOfCorrectiveThresholds(int ac_idx) {
		return violationOfAlertThresholds(ac_idx,0);
	}

	/**
	 * @return time to corrective volume, in seconds, between ownship and aircraft at index idx, for the
	 * corrective volume. The returned time is relative to current time. POSITIVE_INFINITY means no
	 * conflict within lookahead time. NaN means aircraft index is out of range.
	 * @param ac_idx is the index of the traffic aircraft
	 */
	public double timeToCorrectiveVolume(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			ConflictData det = violationOfCorrectiveThresholds(ac_idx);
			if (det.conflict()) {
				return det.getTimeIn();
			}
			return Double.POSITIVE_INFINITY;
		} else {
			error.addError("timeToCorrectiveVolume: aircraft index "+ac_idx+" is out of bounds");
			return Double.NaN;
		}
	}

	/* Getting and Setting DaidalusParameters (note that setters stale the Daidalus object) */

	/* Input/Output methods */

	public String outputStringAircraftStates() {
		String ualt = core_.parameters.getUnitsOf("step_alt");
		String uhs = core_.parameters.getUnitsOf("step_hs");
		String uvs = core_.parameters.getUnitsOf("step_vs");
		String uxy = "m";
		if (Units.isCompatible(uhs,"knot")) {
			uxy = "nmi";
		} else if (Units.isCompatible(uhs,"fpm")) {
			uxy = "ft";
		} else if (Units.isCompatible(uhs,"kph")) {
			uxy = "km";
		}
		return core_.ownship.formattedTraffic(core_.traffic, uxy, ualt, uhs, uvs, core_.current_time);
	}

	public String rawString() {
		String s = "";
		s+=core_.rawString();
		s+="## Direction Bands Internals\n";
		s+=hdir_band_.rawString()+"\n";
		s+="## Horizontal Speed Bands Internals\n";
		s+=hs_band_.rawString()+"\n";
		s+="## Vertical Speed Bands Internals\n";
		s+=vs_band_.rawString()+"\n";
		s+="## Altitude Bands Internals\n";
		s+=alt_band_.rawString()+"\n";
		return s;
	}

	public String toString() {
		String s;
		s = "# Daidalus Object\n";
		s += core_.parameters.toString();
		if (core_.ownship.isValid()) {
			s += "###\n"+outputStringAircraftStates();
			if (core_.isFresh()) {
				s+=core_.toString();
				if (hdir_band_.isFresh()) {
					s+=hdir_band_.toString();
				}
				if (hs_band_.isFresh()) {
					s+=hs_band_.toString();
				}
				if (vs_band_.isFresh()) {
					s+=vs_band_.toString();
				}
				if (alt_band_.isFresh()) {
					s+=alt_band_.toString();
				}
			}
		}
		if (hasMessage()) {
			s += "###\n";
			s+= getMessageNoClear();
		}
		return s;
	}

	public String outputStringInfo() {
		String s="";
		String ualt = core_.parameters.getUnitsOf("step_alt");
		String uhs = core_.parameters.getUnitsOf("step_hs");
		String uvs = core_.parameters.getUnitsOf("step_vs");
		String uxy = "m";
		if (Units.isCompatible(uhs,"knot")) {
			uxy = "nmi";
		} else if (Units.isCompatible(uhs,"fpm")) {
			uxy = "ft";
		} else if (Units.isCompatible(uhs,"kph")) {
			uxy = "km";
		} 
		s+="Time: "+Units.str("s",core_.current_time)+"\n";
		s += core_.ownship.formattedTraffic(core_.traffic,uxy, ualt, uhs, uvs, core_.current_time);
		s+="Conflict Criteria: "+(core_.parameters.isEnabledConflictCriteria()?"Enabled":"Disabled")+"\n";
		s+="Recovery Criteria: "+(core_.parameters.isEnabledRecoveryCriteria()?"Enabled":"Disabled")+"\n";
		s+="Most Urgent Aircraft: "+core_.mostUrgentAircraft().getId()+"\n";
		s+="Horizontal Epsilon: "+core_.epsilonH()+"\n";
		s+="Vertical Epsilon: "+core_.epsilonV()+"\n";
		List<String> acs = new ArrayList<String>();
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			conflictBandsAircraft(acs,region);
			s+="Conflict Bands Aircraft ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					TrafficState.listToString(acs)+"\n";
		}	
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			s+="Time Interval of Conflict ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					timeIntervalOfConflict(region).toStringUnits("s")+"\n";
		}	
		return s;
	}

	public String outputStringAlerting() {
		String s = "";
		for (int ac=1; ac <= lastTrafficIndex(); ++ac) {
			ConflictData conf = violationOfCorrectiveThresholds(ac);
			if (conf.conflict()) {
				s += "Time to Corrective Volume with "+getAircraftStateAt(ac).getId()+
						": "+conf.getTimeInterval().toStringUnits("s")+"\n";
			}
		}
		for (int ac=1; ac <= lastTrafficIndex(); ++ac) {
			int alert_ac = alertLevel(ac);
			s += "Alert Level "+alert_ac+" with "+getAircraftStateAt(ac).getId()+"\n";
		}
		return s;
	}

	public String outputStringDirectionBands() {
		String s="";
		String u  = core_.parameters.getUnitsOf("step_hdir");
		String uh = core_.parameters.getUnitsOf("min_horizontal_recovery");
		String uv = core_.parameters.getUnitsOf("min_vertical_recovery");
		double val = core_.ownship.horizontalDirection();
		s+="Ownship Horizontal Direction: "+Units.str(u,val)+"\n";
		s+="Region of Current Horizontal Direction: "+regionOfHorizontalDirection(val).toString()+"\n";
		s+="Horizontal Direction Bands:\n"; 
		for (int i=0; i < horizontalDirectionBandsLength(); ++i) {
			s+="  "+horizontalDirectionIntervalAt(i).toStringUnits(u)+" "+horizontalDirectionRegionAt(i).toString()+"\n";
		} 
		List<String> acs = new ArrayList<String>();
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			peripheralHorizontalDirectionBandsAircraft(acs,region);
			s+="Peripheral Horizontal Direction Bands Aircraft ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					TrafficState.listToString(acs)+"\n";
		}
		s+="Horizontal Direction Resolution (right): "+Units.str(u,horizontalDirectionResolution(true))+"\n";
		s+="Horizontal Direction Resolution (left): "+Units.str(u,horizontalDirectionResolution(false))+"\n";
		s+="Preferred Horizontal Direction: ";
		if (preferredHorizontalDirectionRightOrLeft()) { 
			s+="right\n";
		} else {
			s+="left\n";
		}
		s+="Recovery Information for Horizontal Direction Bands:\n";
		RecoveryInformation recovery = horizontalDirectionRecoveryInformation();
		s+="  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery())+"\n";
		s+="  Recovery Horizontal Distance: "+
				Units.str(uh,recovery.recoveryHorizontalDistance())+"\n";
		s+="  Recovery Vertical Distance: "+
				Units.str(uv,recovery.recoveryVerticalDistance())+"\n";
		s+="  Recovery N Factor: "+ recovery.nFactor()+"\n";
		return s;
	}

	public String outputStringHorizontalSpeedBands() {
		String s="";
		String u = core_.parameters.getUnitsOf("step_hs");
		String uh = core_.parameters.getUnitsOf("min_horizontal_recovery");
		String uv = core_.parameters.getUnitsOf("min_vertical_recovery");
		double val = core_.ownship.horizontalSpeed();
		s+="Ownship Horizontal Speed: "+Units.str(u,val)+"\n";
		s+="Region of Current Horizontal Speed: "+regionOfHorizontalSpeed(val).toString()+"\n";
		s+="Horizontal Speed Bands:\n"; 
		for (int i=0; i < horizontalSpeedBandsLength(); ++i) {
			s+="  "+horizontalSpeedIntervalAt(i).toStringUnits(u)+" "+horizontalSpeedRegionAt(i).toString()+"\n";
		} 
		List<String> acs = new ArrayList<String>();
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			peripheralHorizontalSpeedBandsAircraft(acs,region);
			s+="Peripheral Horizontal Speed Bands Aircraft ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					TrafficState.listToString(acs)+"\n";
		}
		s+="Horizontal Speed Resolution (up): "+Units.str(u,horizontalSpeedResolution(true))+"\n";
		s+="Horizontal Speed Resolution (down): "+Units.str(u,horizontalSpeedResolution(false))+"\n";
		s+="Preferred Horizontal Speed: ";
		if (preferredHorizontalSpeedUpOrDown()) {
			s+="up\n";
		} else {
			s+="down\n";
		}
		s+="Recovery Information for Horizontal Speed Bands:\n";
		RecoveryInformation recovery = horizontalSpeedRecoveryInformation();
		s+="  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery())+"\n";
		s+="  Recovery Horizontal Distance: "+
				Units.str(uh,recovery.recoveryHorizontalDistance())+"\n";
		s+="  Recovery Vertical Distance: "+
				Units.str(uv,recovery.recoveryVerticalDistance())+"\n";
		s+="  Recovery N Factor: "+recovery.nFactor()+"\n";
		return s;
	}

	public String outputStringVerticalSpeedBands() {
		String s="";
		String u = core_.parameters.getUnitsOf("step_vs");
		String uh = core_.parameters.getUnitsOf("min_horizontal_recovery");
		String uv = core_.parameters.getUnitsOf("min_vertical_recovery");
		double val = core_.ownship.verticalSpeed();
		s+="Ownship Vertical Speed: "+Units.str(u,val)+"\n";
		s+="Region of Current Vertical Speed: "+regionOfVerticalSpeed(val).toString()+"\n";
		s+="Vertical Speed Bands:\n";
		for (int i=0; i < verticalSpeedBandsLength(); ++i) {
			s+="  "+verticalSpeedIntervalAt(i).toStringUnits(u)+" "+ verticalSpeedRegionAt(i).toString()+"\n";
		} 
		List<String> acs = new ArrayList<String>();
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			peripheralVerticalSpeedBandsAircraft(acs,region);
			s+="Peripheral Vertical Speed Bands Aircraft ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					TrafficState.listToString(acs)+"\n";
		}
		s+="Vertical Speed Resolution (up): "+Units.str(u,verticalSpeedResolution(true))+"\n";
		s+="Vertical Speed Resolution (down): "+Units.str(u,verticalSpeedResolution(false))+"\n";
		s+="Preferred Vertical Speed: ";
		if (preferredVerticalSpeedUpOrDown()) {
			s+="up\n";
		} else {
			s+="down\n";
		}
		s+="Recovery Information for Vertical Speed Bands:\n";
		RecoveryInformation recovery = verticalSpeedRecoveryInformation();
		s+="  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery())+"\n";
		s+="  Recovery Horizontal Distance: "+
				Units.str(uh,recovery.recoveryHorizontalDistance())+"\n";
		s+="  Recovery Vertical Distance: "+
				Units.str(uv,recovery.recoveryVerticalDistance())+"\n";
		s+="  Recovery N Factor: "+recovery.nFactor()+"\n";
		return s;
	}

	public String outputStringAltitudeBands() {
		String s="";
		String u = core_.parameters.getUnitsOf("step_alt");
		String uh = core_.parameters.getUnitsOf("min_horizontal_recovery");
		String uv = core_.parameters.getUnitsOf("min_vertical_recovery");
		double val = core_.ownship.altitude();
		s+="Ownship Altitude: "+Units.str(u,val)+"\n";
		s+="Region of Current Altitude: "+regionOfAltitude(val).toString()+"\n";
		s+="Altitude Bands:\n";
		for (int i=0; i < altitudeBandsLength(); ++i) {
			s+="  "+altitudeIntervalAt(i).toStringUnits(u)+" "+ altitudeRegionAt(i).toString()+"\n";
		} 
		List<String> acs = new ArrayList<String>();
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			peripheralAltitudeBandsAircraft(acs,region);
			s+="Peripheral Altitude Bands Aircraft ("+BandsRegion.conflictRegionFromOrder(region)+"): "+
					TrafficState.listToString(acs)+"\n";
		}
		s+="Altitude Resolution (up): "+Units.str(u,altitudeResolution(true))+"\n";
		s+="Altitude Resolution (down): "+Units.str(u,altitudeResolution(false))+"\n";
		s+="Preferred Altitude: ";
		if (preferredAltitudeUpOrDown()) {
			s+="up\n";
		} else {
			s+="down\n";
		}
		s+="Recovery Information for Altitude Bands:\n";
		RecoveryInformation recovery = altitudeRecoveryInformation();
		s+="  Time to Recovery: "+
				Units.str("s",recovery.timeToRecovery())+"\n";
		s+="  Recovery Horizontal Distance: "+
				Units.str(uh,recovery.recoveryHorizontalDistance())+"\n";
		s+="  Recovery Vertical Distance: "+
				Units.str(uv,recovery.recoveryVerticalDistance())+"\n";
		s+="  Recovery N Factor: "+recovery.nFactor()+"\n";
		return s;
	}

	public String outputStringLastTimeToManeuver() {
		String s="";
		for (TrafficState ac : core_.traffic) {
			s+="Last Times to Maneuver with Respect to "+ac.getId()+"\n";
			s+="  Last Time to Horizontal Direction Maneuver: "+Units.str("s",lastTimeToHorizontalDirectionManeuver(ac))+"\n";
			s+="  Last Time to Horizontal Speed Maneuver: "+Units.str("s",lastTimeToHorizontalSpeedManeuver(ac))+"\n";
			s+="  Last Time to Vertical Speed Maneuver: "+Units.str("s",lastTimeToVerticalSpeedManeuver(ac))+"\n";
			s+="  Last Time to Altitude Maneuver: "+Units.str("s",lastTimeToAltitudeManeuver(ac))+"\n";
		}
		return s;
	}

	public String outputString() {
		String s="";
		s+=outputStringInfo();
		s+=outputStringAlerting();
		s+=outputStringDirectionBands();
		s+=outputStringHorizontalSpeedBands();
		s+=outputStringVerticalSpeedBands();
		s+=outputStringAltitudeBands();
		s+=outputStringLastTimeToManeuver();
		return s;
	}

	public String toPVS() {
		return toPVS(true);
	}

	public String toPVS(boolean parameters) {
		boolean comma;	
		String s="%%% INPUTS %%%\n";
		if (parameters) {
			s += "%%% Parameters:\n"+core_.parameters.toPVS()+"\n";
		}
		s += "%%% Time:\n"+f.FmPrecision(getCurrentTime())+"\n";
		s += "%%% Aircraft List:\n"+core_.ownship.listToPVSAircraftList(core_.traffic)+"\n";
		s += "%%% Most Urgent Aircraft:\n\""+core_.mostUrgentAircraft().getId()+"\"\n";
		s += "%%% Horizontal Epsilon:\n"+core_.epsilonH()+"\n";
		s += "%%% Vertical Epsilon:\n"+core_.epsilonV()+"\n";
		s += "%%% Bands for Regions:\n";
		s += "(# ";
		comma = false;
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += BandsRegion.conflictRegionFromOrder(region).toString()+"_:= "+core_.bands_for(BandsRegion.NUMBER_OF_CONFLICT_BANDS-region);
		}
		s += " #)\n";
		s += "%%% OUTPUTS %%%\n";
		s += "%%% Conflict Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		List<String> acs = new ArrayList<String>();
		comma = false;	
		for (int region=BandsRegion.FAR.orderOfConflictRegion(); region <= BandsRegion.NEAR.orderOfConflictRegion(); ++region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			conflictBandsAircraft(acs,region);
			s += TrafficState.listToPVSStringList(acs);
		}
		s += " )::[list[string],list[string],list[string]]\n";
		s += "%%% Region of Current Horizontal Direction:\n"+
				horizontalDirectionRegionAt(indexOfHorizontalDirection(getOwnshipState().horizontalDirection()))+"\n";
		s += "%%% Horizontal Direction Bands: "+horizontalDirectionBandsLength()+"\n";
		s += hdir_band_.toPVS()+"\n";
		s += "%%% Peripheral Horizontal Direction Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		comma = false;
		for (int r=1; r <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++r) {
			BandsRegion region = BandsRegion.conflictRegionFromOrder(r);
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			peripheralHorizontalDirectionBandsAircraft(acs,region);
			s += TrafficState.listToPVSStringList(acs);
		}
		s += " )::[list[string],list[string],list[string]]\n";
		s += "%%% Horizontal Direction Resolution:\n";
		s += "("+f.double2PVS(horizontalDirectionResolution(false))+
				","+f.double2PVS(horizontalDirectionResolution(true))+
				","+preferredHorizontalDirectionRightOrLeft()+")\n";
		RecoveryInformation recovery = horizontalDirectionRecoveryInformation();
		s += "%%% Horizontal Recovery Information:\n"+recovery.toPVS()+"\n";
		s += "%%% Last Times to Direction Maneuver wrt Traffic Aircraft:\n(:"; 
		comma = false;
		for (int ac_idx = 0; ac_idx < core_.traffic.size(); ++ac_idx) {
			if (comma) {
				s += ",";
			} else {
				comma = true;
			}
			s += " "+f.double2PVS(lastTimeToHorizontalDirectionManeuver(core_.traffic.get(ac_idx)));
		}
		s += " :)\n";

		s += "%%% Region of Current Horizontal Speed:\n"+
				horizontalSpeedRegionAt(indexOfHorizontalSpeed(getOwnshipState().horizontalSpeed()))+"\n";
		s += "%%% Horizontal Speed Bands: "+horizontalSpeedBandsLength()+"\n";
		s += hs_band_.toPVS()+"\n";
		s += "%%% Peripheral Horizontal Speed Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		comma = false;
		for (int r=1; r <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++r) {
			BandsRegion region = BandsRegion.conflictRegionFromOrder(r);
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			peripheralHorizontalSpeedBandsAircraft(acs,region);
			s += TrafficState.listToPVSStringList(acs);
		}
		s += " )::[list[string],list[string],list[string]]\n";
		s += "%%% Horizontal Speed Resolution:\n";
		s += "("+f.double2PVS(horizontalSpeedResolution(false))+
				","+f.double2PVS(horizontalSpeedResolution(true))+
				","+preferredHorizontalSpeedUpOrDown()+")\n";
		recovery = horizontalSpeedRecoveryInformation();
		s += "%%% Horizontal Speed Information:\n"+recovery.toPVS()+"\n";
		s += "%%% Last Times to Horizontal Speed Maneuver wrt Traffic Aircraft:\n(:"; 
		comma = false;
		for (int ac_idx = 0; ac_idx < core_.traffic.size(); ++ac_idx) {
			if (comma) {
				s += ",";
			} else {
				comma = true;
			}
			s += " "+f.double2PVS(lastTimeToHorizontalSpeedManeuver(core_.traffic.get(ac_idx)));
		}
		s += " :)\n";

		s += "%%% Region of Current Vertical Speed:\n"+
				verticalSpeedRegionAt(indexOfVerticalSpeed(getOwnshipState().verticalSpeed()))+"\n";
		s += "%%% Vertical Speed Bands: "+verticalSpeedBandsLength()+"\n";
		s += vs_band_.toPVS()+"\n";
		s += "%%% Peripheral Vertical Speed Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		comma = false;
		for (int r=1; r <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++r) {
			BandsRegion region = BandsRegion.conflictRegionFromOrder(r);
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			peripheralVerticalSpeedBandsAircraft(acs,region);
			s += TrafficState.listToPVSStringList(acs);
		}
		s += " )::[list[string],list[string],list[string]]\n";
		s += "%%% Vertical Speed Resolution:\n";
		s += "("+f.double2PVS(verticalSpeedResolution(false))+
				","+f.double2PVS(verticalSpeedResolution(true))+
				","+preferredVerticalSpeedUpOrDown()+")\n";
		recovery = verticalSpeedRecoveryInformation();
		s += "%%% Vertical Speed Information:\n"+recovery.toPVS()+"\n";
		s += "%%% Last Times to Vertical Speed Maneuver wrt Traffic Aircraft:\n(:"; 
		comma = false;
		for (int ac_idx = 0; ac_idx < core_.traffic.size(); ++ac_idx) {
			if (comma) {
				s += ",";
			} else {
				comma = true;
			}
			s += " "+f.double2PVS(lastTimeToVerticalSpeedManeuver(core_.traffic.get(ac_idx)));
		}
		s += " :)\n";

		s += "%%% Region of Current Altitude:\n"+
				altitudeRegionAt(indexOfAltitude(getOwnshipState().altitude()))+"\n";
		s += "%%% Altitude Bands: "+altitudeBandsLength()+"\n";
		s += alt_band_.toPVS()+"\n";
		s += "%%% Peripheral Altitude Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		comma = false;
		for (int r=1; r <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++r) {
			BandsRegion region = BandsRegion.conflictRegionFromOrder(r);
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			peripheralAltitudeBandsAircraft(acs,region);
			s += TrafficState.listToPVSStringList(acs);
		}
		s += " )::[list[string],list[string],list[string]]\n";
		s += "%%% Altitude Resolution:\n";
		s += "("+f.double2PVS(altitudeResolution(false))+
				","+f.double2PVS(altitudeResolution(true))+
				","+preferredAltitudeUpOrDown()+")\n";
		recovery = altitudeRecoveryInformation();
		s += "%%% Altitude Information:\n"+recovery.toPVS()+"\n";
		s += "%%% Last Times to Altitude Maneuver wrt Traffic Aircraft:\n(:"; 
		comma = false;
		for (int ac_idx = 0; ac_idx < core_.traffic.size(); ++ac_idx) {
			if (comma) {
				s += ",";
			} else {
				comma = true;
			}
			s += " "+f.double2PVS(lastTimeToAltitudeManeuver(core_.traffic.get(ac_idx)));
		}
		s += " :)\n";

		s += "%%% Time to Corrective Volume:\n"; 
		s += "(: ";
		comma = false;
		for (int ac=1; ac <= lastTrafficIndex(); ++ac) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			ConflictData conf = violationOfCorrectiveThresholds(ac);
			s += "("+f.FmPrecision(conf.getTimeIn())+","+f.FmPrecision(conf.getTimeOut())+")";
		}
		s += " :)\n";

		s += "%%% Alerting:\n";
		s += "(: ";
		comma = false;
		for (int ac=1; ac <= lastTrafficIndex(); ++ac) {
			if (comma) {
				s += ", ";          
			} else {
				comma = true;
			}
			s += "(\""+core_.traffic.get(ac-1).getId()+"\","+f.Fmi(alertLevel(ac))+")";
		}
		s += " :)\n";
		return s;
	}

	// ErrorReporter Interface Methods

	public boolean hasError() {
		return error.hasError();
	}

	public boolean hasMessage() {
		return error.hasMessage();
	}

	public String getMessage() {
		return error.getMessage();
	}

	public String getMessageNoClear() {
		return error.getMessageNoClear();
	}

	/* Deprecated Methods */

	@Deprecated
	/**
	 * Set ownship state at time 0.0. Clear all traffic. Name of ownship will be "Ownship"
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */
	public void setOwnship(Position pos, Velocity vel) {
		setOwnship("Ownship",pos,vel);
	}

	@Deprecated
	/**
	 * Add traffic state at current time. If it's the first aircraft, this aircraft is 
	 * set as the ownship. Name of aircraft is AC_n, where n is the index of the aicraft
	 * @param pos Aircraft's position
	 * @param vel Aircraft's ground velocity
	 * Same function as addTrafficState, but it doesn't return index of added traffic. 
	 */
	public void addTraffic(Position pos, Velocity vel) {
		if (!hasOwnship()) {
			setOwnship(pos,vel);
		} else {
			addTrafficState("AC_"+f.Fmi(core_.traffic.size()+1),pos,vel);
		}
	}

	@Deprecated
	public void setMaxGroundSpeed(double gs, String unit) {
		setMaxHorizontalSpeed(gs,unit);		
	}

	@Deprecated
	public double getMaxGroundSpeed(String unit) {
		return getMaxHorizontalSpeed(unit);
	}

	@Deprecated
	public int trackLength() {
		return horizontalDirectionBandsLength();
	}

	@Deprecated
	public Interval track(int i, String unit) {
		return horizontalDirectionIntervalAt(i,unit);
	}

	@Deprecated
	public BandsRegion trackRegion(int i) {
		return horizontalDirectionRegionAt(i);
	}

	@Deprecated
	public BandsRegion regionOfTrack(double trk, String unit) {
		return regionOfHorizontalDirection(trk,unit);
	}

	@Deprecated
	public int groundSpeedLength() {
		return horizontalSpeedBandsLength();
	}

	@Deprecated
	public Interval groundSpeed(int i, String unit) {
		return horizontalSpeedIntervalAt(i,unit);
	}

	@Deprecated
	public BandsRegion groundSpeedRegion(int i) {
		return horizontalSpeedRegionAt(i);
	}

	@Deprecated
	public BandsRegion regionOfGroundSpeed(double gs, String unit) {
		return regionOfHorizontalSpeed(gs,unit);
	}

	@Deprecated
	public Interval verticalSpeed(int i, String unit) {
		return verticalSpeedIntervalAt(i,unit);
	}

	@Deprecated
	public BandsRegion verticalSpeedRegion(int i) {
		return verticalSpeedRegionAt(i);
	}

}

