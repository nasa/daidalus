/* 
 * Copyright (c) 2011-2021 United States Government as represented by
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
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
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
	 * Construct an empty Daidalus object.
	 * NOTE: This object doesn't have any alert configured. Alerters can be
	 * configured either programmatically, set_DO_365B() or
	 * via a configuration file with the method loadFromFile(configurationfile)
	 **/
	public Daidalus() {
		core_ = new DaidalusCore();
		hdir_band_ = new DaidalusDirBands();
		hs_band_ = new DaidalusHsBands();
		vs_band_ = new DaidalusVsBands();
		alt_band_ = new DaidalusAltBands();
	}

	/** 
	 * Construct a Daidalus object with initial alerter.
	 */
	public Daidalus(Alerter alerter) {
		core_ = new DaidalusCore(alerter);
		hdir_band_ = new DaidalusDirBands();
		hs_band_ = new DaidalusHsBands();
		vs_band_ = new DaidalusVsBands();
		alt_band_ = new DaidalusAltBands();
	}

	/** 
	 * Construct a Daidalus object with the default parameters and one alerter with the
	 * given detector and T (in seconds) as the alerting time, early alerting time, and lookahead time.
	 */
	public Daidalus(Detection3D det, double T) {
		core_ = new DaidalusCore(det,T);
		hdir_band_ = new DaidalusDirBands();
		hs_band_ = new DaidalusHsBands();
		vs_band_ = new DaidalusVsBands();
		alt_band_ = new DaidalusAltBands();
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
	 * - Configure two alerters (Phase I and Phase II) as defined as in RTCA DO-365A
	 * - Maneuver guidance logic assumes kinematic maneuvers
	 * - Turn rate is set to 3 deg/s
	 * - Configure Sensor Uncertainty Migitation (SUM) 
	 * - Bands don't saturate until NMAC
	 */
	public void set_DO_365A() {
		set_DO_365A(true,true);
	}

	/*  
	 * Set Daidalus object such that 
	 * - Configure two alerters (Phase I and Phase II) as defined as in RTCA DO-365A
	 * - Maneuver guidance logic assumes kinematic maneuvers
	 * - Turn rate is set to 3 deg/s, when type is true, and to  1.5 deg/s
	 *   when type is false.
	 * - Configure Sensor Uncertainty Migitation (SUM) when sum is true  
	 * - Bands don't saturate until NMAC
	 */
	public void set_DO_365A(boolean type, boolean sum) {
		clearAlerters();
		if (sum) {
			addAlerter(Alerter.DWC_Phase_I_SUM);
			addAlerter(Alerter.DWC_Phase_II_SUM);
		} else {
			addAlerter(Alerter.DWC_Phase_I);
			addAlerter(Alerter.DWC_Phase_II);			
		}
		setCorrectiveRegion(BandsRegion.MID);
		setKinematicBands(type);
		setCollisionAvoidanceBands(true);
		setCollisionAvoidanceBandsFactor(0.1);
		setMinHorizontalRecovery(0.66,"nmi");
		setMinVerticalRecovery(450,"ft");
		//setVerticalRate(1000,"fpm");
		setIntruderCentricAlertingLogic();
		/** Set hysteresis parameters **/
		setHysteresisTime(5.0);
		setPersistenceTime(4.0);
		setBandsPersistence(true);
		setPersistencePreferredHorizontalDirectionResolution(15,"deg");
		setPersistencePreferredHorizontalSpeedResolution(100,"knot");
		setPersistencePreferredVerticalSpeedResolution(250,"fpm");
		setPersistencePreferredAltitudeResolution(250,"ft");
		setAlertingMofN(2,4);
		/** Set SUM parameters **/
		setHorizontalPositionZScore(1.5);
		setHorizontalVelocityZScoreMin(0.5);
		setHorizontalVelocityZScoreMax(1.0);
		setHorizontalVelocityZDistance(5,"nmi");
		setVerticalPositionZScore(0.75);
		setVerticalSpeedZScore(1.5);
		/** Set DTA parameters **/
		setDTARadius(4.2,"nmi");
		setDTAHeight(2000,"ft");
		setDTAAlerter(2);
	}

	/*  
	 * Set Daidalus object such that 
	 * - Configure two alerters (Phase I, Phase II, and Non-Cooperative) as defined as in RTCA DO-365B
	 * - Maneuver guidance logic assumes kinematic maneuvers
	 * - Turn rate is set to 3 deg/s
	 * - Configure Sensor Uncertainty Migitation (SUM) 
	 * - Bands don't saturate until NMAC
	 */
	public void set_DO_365B() {
		set_DO_365B(true,true);
	}

	/*  
	 * Set Daidalus object such that 
	 * - Configure two alerters (Phase, Phase II, and Non-Cooperative) as defined as in RTCA DO-365B
	 * - Maneuver guidance logic assumes kinematic maneuvers
	 * - Turn rate is set to 3 deg/s, when type is true, and to  1.5 deg/s
	 *   when type is false.
	 * - Configure Sensor Uncertainty Migitation (SUM) when sum is true  
	 * - Bands don't saturate until NMAC
	 */
	public void set_DO_365B(boolean type, boolean sum) {
		set_DO_365A(type,sum);
		if (sum) {
			addAlerter(Alerter.DWC_Non_Coop_SUM);
		} else {
			addAlerter(Alerter.DWC_Non_Coop);
		}
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
		addAlerter(Alerter.Buffered_DWC_Phase_I);
		setCorrectiveRegion(BandsRegion.MID);
		setKinematicBands(type);
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
		addAlerter(Alerter.CD3D_SingleBands);
		setCorrectiveRegion(BandsRegion.NEAR);
		setInstantaneousBands();
		setCollisionAvoidanceBands(true);
		setCollisionAvoidanceBandsFactor(0.1);
		setMinHorizontalRecovery(5.0,"nmi");
		setMinVerticalRecovery(1000.0,"ft");
	}

	/* Set DAIDALUS object such that alerting logic and maneuver guidance corresponds to 
	 * ideal TCASII */
	public void set_TCASII() {
		clearAlerters();
		addAlerter(Alerter.TCASII);
		setLookaheadTime(60);
		setCorrectiveRegion(BandsRegion.NEAR);
		setKinematicBands(true);
		setVerticalRate(1500,"fpm");
		setCollisionAvoidanceBands(true);
		setCollisionAvoidanceBandsFactor(0.1);
		setMinHorizontalRecovery(0);
		setMinVerticalRecovery(0);
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
			error.addError("getAircraftStateAt: aircraft index "+idx+" is out of bounds");
			return TrafficState.INVALID;
		}
	}

	/**
	 * Set ownship state and current time. Clear all traffic. 
	 * @param id Ownship's identifier
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 * @param time Time stamp of ownship's state
	 */
	public void setOwnshipState(String id, Position pos, Velocity vel, double time) {
		if (!hasOwnship() || !core_.ownship.getId().equals(id) || 
				time < getCurrentTime() ||
				time-getCurrentTime() > getHysteresisTime()){
			// Full reset (including hysteresis) if adding a different ownship or time is
			// in the past. Note that wind is not clear.
			clearHysteresis();
			core_.set_ownship_state(id,pos,vel,time);
		} else {
			// Otherwise, reset cache values but keeps hysteresis.
			core_.set_ownship_state(id,pos,vel,time);
			stale_bands();
		}
	}

	/**
	 * Set ownship state at time 0.0. Clear all traffic. 
	 * @param id Ownship's identifier
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */
	public void setOwnshipState(String id, Position pos, Velocity vel) {
		setOwnshipState(id,pos,vel,0.0);
	}

	/**
	 * Add traffic state at given time. 
	 * If time is different from current time, traffic state is projected, past or future, 
	 * into current time. If it's the first aircraft, this aircraft is 
	 * set as the ownship. If a traffic state with the same id already exists,
	 * the traffic state is overwritten. If id is ownship's, nothing is done and 
	 * the value -1 is returned. 
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
			int idx = core_.set_traffic_state(id,pos,vel,time);
			if (idx >= 0) {
				++idx;
				stale_bands();
			}
			return idx;
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
	 * Get index of aircraft with given name. Return -1 if no such index exists
	 */
	public int aircraftIndex(String name) {
		int idx = -1;
		if (lastTrafficIndex() >= 0) {
			if (core_.ownship.getId().equals(name)) {
				return 0;
			}
			idx = core_.find_traffic_state(name);
			if (idx >= 0) {
				++idx;
			}
		}
		return idx;
	}

	/**
	 * Exchange ownship aircraft with aircraft named id.
	 * EXPERT USE ONLY !!!
	 */
	public void resetOwnship(String id) {
		int ac_idx = aircraftIndex(id);
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			clearHysteresis();
			core_.reset_ownship(ac_idx-1);
		} else {
			error.addError("resetOwnship: aircraft index "+ac_idx+" is out of bounds");
		}
	}

	/**                                                                                                                                                
	 * Remove traffic from the list of aircraft. Returns false if no aircraft was removed.                                                             
	 * Ownship cannot be removed.                                                                                                                      
	 * If traffic is at index i, the indices of aircraft at {@code k > i}, are shifted to k-1.                                                                 
	 * EXPERT USE ONLY !!!
	 */
	public boolean removeTrafficAircraft(String name) {
		int ac_idx = aircraftIndex(name);
		if (core_.remove_traffic(ac_idx-1)) {
			stale_bands();
			return true;
		}
		return false;
	}

	/** 
	 * Project ownship and traffic aircraft offset seconds in the future (if positive) or in the past (if negative)
	 * EXPERT USE ONLY !!!
	 */
	public void linearProjection(double offset) {
		if (core_.linear_projection(offset)) {
			stale_bands();
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
	 * @param wind_vector: Wind velocity specified in TO direction
	 */
	public void setWindVelocityTo(Velocity wind_vector) {
		core_.set_wind_velocity(wind_vector);
		stale_bands();
	}

	/**
	 * Set wind velocity specified in the From direction
	 * @param nwind_vector: Wind velocity specified in From direction
	 */
	public void setWindVelocityFrom(Velocity nwind_vector) {
		setWindVelocityTo(nwind_vector.Neg());
	}

	/**
	 * Set no wind velocity 
	 */
	public void setNoWind() {
		core_.clear_wind();
		stale_bands();
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
				if (ac_idx == 0 && core_.set_alerter_ownship(alerter_idx)) {
					stale_bands();
				} else if (ac_idx > 0 && core_.set_alerter_traffic(ac_idx-1,alerter_idx)) {
					stale_bands();
				}
			}
		} else {
			error.addError("setAlerterIndex: aircraft index "+ac_idx+" is out of bounds");
		}
		if (alerter_idx > core_.parameters.numberOfAlerters()) {
			error.addWarning("setAlerterIndex: alerter index "+alerter_idx+" is out of bounds");
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
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return core_.alerter_index_of(getAircraftStateAt(ac_idx));
		}
		return 0;
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
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (ac_idx == 0) {
				core_.ownship.setHorizontalPositionUncertainty(s_EW_std,s_NS_std,s_EN_std);
			} else {
				core_.traffic.get(ac_idx-1).setHorizontalPositionUncertainty(s_EW_std,s_NS_std,s_EN_std);
			}
			reset();
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
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (ac_idx == 0) {
				core_.ownship.setVerticalPositionUncertainty(sz_std);
			} else {
				core_.traffic.get(ac_idx-1).setVerticalPositionUncertainty(sz_std);
			}
			reset();
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
	 * v_EW_std: East/West speed standard deviation in internal units
	 * v_NS_std: North/South speed standard deviation in internal units
	 * v_EN_std: East/North speed standard deviation in internal units
	 */
	public void setHorizontalVelocityUncertainty(int ac_idx, double v_EW_std, double v_NS_std,  double v_EN_std) {
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (ac_idx == 0) {
				core_.ownship.setHorizontalVelocityUncertainty(v_EW_std,v_NS_std,v_EN_std);
			} else {
				core_.traffic.get(ac_idx-1).setHorizontalVelocityUncertainty(v_EW_std,v_NS_std,v_EN_std);
			}
			reset();
		}
	}

	/**
	 * Set horizontal velocity uncertainty of aircraft at index ac_idx
	 * v_EW_std: East/West speed standard deviation in given units
	 * v_NS_std: North/South speed standard deviation in given units
	 * v_EN_std: East/North speed standard deviation in given units
	 */
	public void setHorizontalVelocityUncertainty(int ac_idx, double v_EW_std, double v_NS_std,  double v_EN_std, String u) {
		setHorizontalVelocityUncertainty(ac_idx,Units.from(u,v_EW_std),Units.from(u,v_NS_std),Units.from(u,v_EN_std));
	}

	/**
	 * Set vertical speed uncertainty of aircraft at index ac_idx
	 * vz_std : Vertical speed standard deviation in internal units
	 */
	public void setVerticalSpeedUncertainty(int ac_idx, double vz_std) {
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (ac_idx == 0) {
				core_.ownship.setVerticalSpeedUncertainty(vz_std);
			} else {
				core_.traffic.get(ac_idx-1).setVerticalSpeedUncertainty(vz_std);
			}
			reset();
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
		if (0 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			if (ac_idx == 0) {
				core_.ownship.resetUncertainty();
			} else {
				core_.traffic.get(ac_idx-1).resetUncertainty();
			}
			reset();
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
		if (strat != null) {
			core_.urgency_strategy = strat.copy();
			reset();
		}
	}

	/**
	 * @return most urgent aircraft.
	 */
	public TrafficState mostUrgentAircraft() {
		return core_.mostUrgentAircraft();
	}

	/* Computation of contours, a.k.a. blobs, and hazard zones */

	/**
	 * Computes horizontal contours contributed by aircraft at index idx, for 
	 * given alert level. A contour is a list of points in counter-clockwise 
	 * direction representing a polygon. Last point should be connected to first one.
	 * The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual contours defined by the violation and detection methods.
	 * @param blobs list of horizontal contours returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 * @param alert_level is the alert level used to compute detection. The value 0
	 * indicate the alert level of the corrective region.
	 */
	public void horizontalContours(List<List<Position>>blobs, int ac_idx, int alert_level) {
		blobs.clear();
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
	 * Computes horizontal contours contributed by aircraft at index ac_idx, for the alert level 
	 * corresponding to the corrective region. A contour is a list of points in 
	 * counter-clockwise direction representing a polygon. Last point should be connected to 
	 * first one.  
	 * The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual contours defined by the violation and detection methods.
	 * @param blobs list of horizontal contours returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 */
	public void horizontalContours(List<List<Position>>blobs, int ac_idx) {
		horizontalContours(blobs,ac_idx,0);
	}

	/**
	 * Computes horizontal contours contributed by aircraft at index idx, for 
	 * given region. A contour is a list of points in counter-clockwise 
	 * direction representing a polygon. Last point should be connected to first one.
	 * The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual contours defined by the violation and detection methods.
	 * @param blobs list of horizontal contours returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 * @param region is the region used to compute detection. 
	 */
	public void horizontalContours(List<List<Position>>blobs, int ac_idx, BandsRegion region) {
		horizontalContours(blobs,ac_idx,alertLevelOfRegion(ac_idx,region));
	}

	/**
	 * Computes horizontal hazard zone around aircraft at index ac_idx, for given alert level. 
	 * A hazard zone is a list of points in counter-clockwise 
	 * direction representing a polygon. Last point should be connected to first one.
	 * @param haz hazard zone returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 * @param loss true means that the polygon represents the hazard zone. Otherwise, 
	 * the polygon represents the hazard zone with an alerting time. 
	 * @param from_ownship true means ownship point of view. Otherwise, the hazard zone is computed 
	 * from the intruder's point of view.
	 * @param alert_level is the alert level used to compute detection. The value 0
	 * indicate the alert level of the corrective region.
	 * NOTE: The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual hazard zone defined by the violation and detection methods.
	 */
	public void horizontalHazardZone(List<Position> haz, int ac_idx, boolean loss, boolean from_ownship,
			int alert_level) {
		haz.clear();
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			int code = core_.horizontal_hazard_zone(haz,ac_idx-1,alert_level,loss,from_ownship);
			switch (code) {
			case 1: 
				error.addError("horizontalHazardZone: detector of traffic aircraft "+ac_idx+" is not set");
				break;
			case 2:
				error.addError("horizontalHazardZone: no corrective alerter level for alerter of "+ac_idx);
				break;
			case 3: 
				error.addError("horizontalHazardZone: alerter of traffic aircraft "+ac_idx+" is out of bounds");
				break;
			}
		} else {
			error.addError("horizontalHazardZone: aircraft index "+ac_idx+" is out of bounds");			
		}
	}

	/**
	 * Computes horizontal hazard zone around aircraft at index ac_idx, for corrective alert level. 
	 * A hazard zone is a list of points in counter-clockwise 
	 * direction representing a polygon. Last point should be connected to first one.
	 * @param haz hazard zone returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 * @param loss true means that the polygon represents the hazard zone. Otherwise, 
	 * the polygon represents the hazard zone with an alerting time. 
	 * @param from_ownship true means ownship point of view. Otherwise, the hazard zone is computed 
	 * from the intruder's point of view.
	 * NOTE: The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the hazard zone defined by the violation and detection methods.
	 */
	public void horizontalHazardZone(List<Position> haz, int ac_idx, 
			boolean loss, boolean from_ownship) {
		horizontalHazardZone(haz,ac_idx,loss,from_ownship,0);
	}

	/**
	 * Computes horizontal hazard zone around aircraft at index ac_idx, for given region. 
	 * A hazard zone is a list of points in counter-clockwise 
	 * direction representing a polygon. Last point should be connected to first one.
	 * @param haz hazard zone returned by reference.
	 * @param ac_idx is the index of the aircraft used to compute the contours.
	 * @param loss true means that the polygon represents the hazard zone. Otherwise, 
	 * the polygon represents the hazard zone with an alerting time. 
	 * @param from_ownship true means ownship point of view. Otherwise, the hazard zone is computed 
	 * from the intruder's point of view.
	 * @param region is the region used to compute detection. 
	 * NOTE: The computed polygon should only be used for display purposes since it's merely an
	 * approximation of the actual hazard zone defined by the violation and detection methods.
	 */
	public void horizontalHazardZone(List<Position> haz, int ac_idx, boolean loss, boolean from_ownship,
			BandsRegion region) {
		horizontalHazardZone(haz,ac_idx,loss,from_ownship,alertLevelOfRegion(ac_idx,region));
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
		reset();
	}

	/**
	 * Add alerter (if id of alerter already exists, replaces alerter with new one).
	 * Return index of added alerter
	 */
	public int addAlerter(Alerter alerter) {
		int alert_idx = core_.parameters.addAlerter(alerter);
		reset();
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
		reset();
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
		reset();
	}

	/** 
	 * Set lookahead time to value in specified units [u].
	 */
	public void setLookaheadTime(double t, String u) {
		core_.parameters.setLookaheadTime(t,u);
		reset();
	}

	/** 
	 * Set left direction to value in internal units [rad]. Value is expected to be in [0 - pi]
	 */
	public void setLeftHorizontalDirection(double val) {
		core_.parameters.setLeftHorizontalDirection(val);
		reset();
	}

	/** 
	 * Set left direction to value in specified units [u]. Value is expected to be in [0 - pi]
	 */
	public void setLeftHorizontalDirection(double val, String u) {
		core_.parameters.setLeftHorizontalDirection(val,u);
		reset();
	}

	/** 
	 * Set right direction to value in internal units [rad]. Value is expected to be in [0 - pi]
	 */
	public void setRightHorizontalDirection(double val) {
		core_.parameters.setRightHorizontalDirection(val);
		reset();
	}

	/** 
	 * Set right direction to value in specified units [u]. Value is expected to be in [0 - pi]
	 */
	public void setRightHorizontalDirection(double val, String u) {
		core_.parameters.setRightHorizontalDirection(val,u);
		reset();
	}

	/** 
	 * Sets minimum horizontal speed for horizontal speed bands to value in internal units [m/s].
	 */
	public void setMinHorizontalSpeed(double val) {
		core_.parameters.setMinHorizontalSpeed(val);
		reset();
	}

	/** 
	 * Sets minimum horizontal speed for horizontal speed bands to value in specified units [u].
	 */
	public void setMinHorizontalSpeed(double val, String u) {
		core_.parameters.setMinHorizontalSpeed(val,u);
		reset();
	}

	/** 
	 * Sets maximum horizontal speed for horizontal speed bands to value in internal units [m/s].
	 */
	public void setMaxHorizontalSpeed(double val) {
		core_.parameters.setMaxHorizontalSpeed(val);
		reset();
	}

	/** 
	 * Sets maximum horizontal speed for horizontal speed bands to value in specified units [u].
	 */
	public void setMaxHorizontalSpeed(double val, String u) {
		core_.parameters.setMaxHorizontalSpeed(val,u);
		reset();
	}

	/** 
	 * Sets minimum vertical speed for vertical speed bands to value in internal units [m/s].
	 */
	public void setMinVerticalSpeed(double val) {
		core_.parameters.setMinVerticalSpeed(val);
		reset();
	}

	/** 
	 * Sets minimum vertical speed for vertical speed bands to value in specified units [u].
	 */
	public void setMinVerticalSpeed(double val, String u) {
		core_.parameters.setMinVerticalSpeed(val,u);
		reset();
	}

	/** 
	 * Sets maximum vertical speed for vertical speed bands to value in internal units [m/s].
	 */
	public void setMaxVerticalSpeed(double val) {
		core_.parameters.setMaxVerticalSpeed(val);
		reset();
	}

	/** 
	 * Sets maximum vertical speed for vertical speed bands to value in specified units [u].
	 */
	public void setMaxVerticalSpeed(double val, String u) {
		core_.parameters.setMaxVerticalSpeed(val,u);
		reset();
	}

	/** 
	 * Sets minimum altitude for altitude bands to value in internal units [m]
	 */
	public void setMinAltitude(double val) {
		core_.parameters.setMinAltitude(val);
		reset();
	}

	/** 
	 * Sets minimum altitude for altitude bands to value in specified units [u].
	 */
	public void setMinAltitude(double val, String u) {
		core_.parameters.setMinAltitude(val,u);
		reset();
	}

	/** 
	 * Sets maximum altitude for altitude bands to value in internal units [m]
	 */
	public void setMaxAltitude(double val) {
		core_.parameters.setMaxAltitude(val);
		reset();
	}

	/** 
	 * Sets maximum altitude for altitude bands to value in specified units [u].
	 */
	public void setMaxAltitude(double val, String u) {
		core_.parameters.setMaxAltitude(val,u);
		reset();
	}

	/**
	 * Set horizontal speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeHorizontalSpeed(double val) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val);
		reset();
	}

	/**
	 * Set horizontal speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeHorizontalSpeed(double val,String u) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val,u);
		reset();
	}

	/**
	 * Set horizontal speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeHorizontalSpeed(double val) {
		core_.parameters.setAboveRelativeHorizontalSpeed(val);
		reset();
	}

	/**
	 * Set horizontal speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeHorizontalSpeed(double val, String u) {
		core_.parameters.setAboveRelativeHorizontalSpeed(val,u);
		reset();
	}

	/**
	 * Set vertical speed in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeVerticalSpeed(double val) {
		core_.parameters.setBelowRelativeHorizontalSpeed(val);
		reset();
	}

	/**
	 * Set vertical speed in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeVerticalSpeed(double val, String u) {
		core_.parameters.setBelowRelativeVerticalSpeed(val,u);
		reset();
	}

	/**
	 * Set vertical speed in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeVerticalSpeed(double val) {
		core_.parameters.setAboveRelativeVerticalSpeed(val);
		reset();
	}

	/**
	 * Set vertical speed in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeVerticalSpeed(double val, String u) {
		core_.parameters.setAboveRelativeVerticalSpeed(val,u);
		reset();
	}

	/**
	 * Set altitude in internal units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeAltitude(double val) {
		core_.parameters.setBelowRelativeAltitude(val);
		reset();
	}

	/**
	 * Set altitude in given units (below current value) for the 
	 * computation of relative bands 
	 */
	public void setBelowRelativeAltitude(double val, String u) {
		core_.parameters.setBelowRelativeAltitude(val,u);
		reset();
	}

	/**
	 * Set altitude in internal units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeAltitude(double val) {
		core_.parameters.setAboveRelativeAltitude(val);
		reset();
	}

	/**
	 * Set altitude in given units (above current value) for the 
	 * computation of relative bands 
	 */
	public void setAboveRelativeAltitude(double val, String u) {
		core_.parameters.setAboveRelativeAltitude(val,u);
		reset();
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
		reset();
	}

	/** 
	 * Sets step size for direction bands in specified units [u].
	 */
	public void setHorizontalDirectionStep(double val, String u) {
		core_.parameters.setHorizontalDirectionStep(val,u);
		reset();
	}

	/** 
	 * Sets step size for horizontal speed bands to value in internal units [m/s].
	 */
	public void setHorizontalSpeedStep(double val) {
		core_.parameters.setHorizontalSpeedStep(val);
		reset();
	}

	/** 
	 * Sets step size for horizontal speed bands to value in specified units [u].
	 */
	public void setHorizontalSpeedStep(double val, String u) {
		core_.parameters.setHorizontalSpeedStep(val,u);
		reset();
	}

	/** 
	 * Sets step size for vertical speed bands to value in internal units [m/s].
	 */
	public void setVerticalSpeedStep(double val) {
		core_.parameters.setVerticalSpeedStep(val);
		reset();
	}

	/** 
	 * Sets step size for vertical speed bands to value in specified units [u].
	 */
	public void setVerticalSpeedStep(double val, String u) {
		core_.parameters.setVerticalSpeedStep(val,u);
		reset();
	}

	/** 
	 * Sets step size for altitude bands to value in internal units [m]
	 */
	public void setAltitudeStep(double val) {
		core_.parameters.setAltitudeStep(val);
		reset();
	}

	/** 
	 * Sets step size for altitude bands to value in specified units [u].
	 */
	public void setAltitudeStep(double val, String u) {
		core_.parameters.setAltitudeStep(val,u);
		reset();
	}

	/** 
	 * Sets horizontal acceleration for horizontal speed bands to value in internal units [m/s^2].
	 */
	public void setHorizontalAcceleration(double val) {
		core_.parameters.setHorizontalAcceleration(val);
		reset();
	}

	/** 
	 * Sets horizontal acceleration for horizontal speed bands to value in specified units [u].
	 */
	public void setHorizontalAcceleration(double val, String u) {
		core_.parameters.setHorizontalAcceleration(val,u);
		reset();
	}

	/** 
	 * Sets the constant vertical acceleration for vertical speed and altitude bands
	 * to value in internal units [m/s^2]
	 */
	public void setVerticalAcceleration(double val) {
		core_.parameters.setVerticalAcceleration(val);
		reset();
	}

	/** 
	 * Sets the constant vertical acceleration for vertical speed and altitude bands
	 * to value in specified units [u].
	 */
	public void setVerticalAcceleration(double val, String u) {
		core_.parameters.setVerticalAcceleration(val,u);
		reset();
	}

	/** 
	 * Sets turn rate for direction bands to value in internal units [rad/s]. As a side effect, this method
	 * resets the bank angle.
	 */
	public void setTurnRate(double val) {
		core_.parameters.setTurnRate(val);
		reset();
	}

	/** 
	 * Sets turn rate for direction bands to value in specified units [u]. As a side effect, this method
	 * resets the bank angle.
	 */
	public void setTurnRate(double val, String u) {
		core_.parameters.setTurnRate(val,u);
		reset();
	}

	/** 
	 * Sets bank angle for direction bands to value in internal units [rad]. As a side effect, this method
	 * resets the turn rate.
	 */
	public void setBankAngle(double val) {
		core_.parameters.setBankAngle(val);
		reset();
	}

	/** 
	 * Sets bank angle for direction bands to value in specified units [u]. As a side effect, this method
	 * resets the turn rate.
	 */
	public void setBankAngle(double val, String u) {
		core_.parameters.setBankAngle(val,u);
		reset();
	}

	/** 
	 * Sets vertical rate for altitude bands to value in internal units [m/s]
	 */
	public void setVerticalRate(double val) {
		core_.parameters.setVerticalRate(val);
		reset();
	}

	/** 
	 * Sets vertical rate for altitude bands to value in specified units [u].
	 */
	public void setVerticalRate(double val, String u) {
		core_.parameters.setVerticalRate(val,u);
		reset();
	}

	/** 
	 * Set horizontal NMAC distance to value in internal units [m].
	 */
	public void setHorizontalNMAC(double val) {
		core_.parameters.setHorizontalNMAC(val);
		reset();
	}

	/** 
	 * Set horizontal NMAC distance to value in specified units [u].
	 */
	public void setHorizontalNMAC(double val, String u) {
		core_.parameters.setHorizontalNMAC(val,u);
		reset();
	}

	/** 
	 * Set vertical NMAC distance to value in internal units [m].
	 */
	public void setVerticalNMAC(double val) {
		core_.parameters.setVerticalNMAC(val);
		reset();
	}

	/** 
	 * Set vertical NMAC distance to value in specified units [u].
	 */
	public void setVerticalNMAC(double val, String u) {
		core_.parameters.setVerticalNMAC(val,u);
		reset();
	}

	/**
	 * Sets recovery stability time in seconds. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public void setRecoveryStabilityTime(double t) {
		core_.parameters.setRecoveryStabilityTime(t);
		reset();
	}

	/**
	 * Sets recovery stability time in specified units. Recovery bands are computed at time of 
	 * first conflict-free region plus this time.
	 */
	public void setRecoveryStabilityTime(double t, String u) {
		core_.parameters.setRecoveryStabilityTime(t,u);
		reset();
	}

	/** 
	 * Set hysteresis time to value in seconds.
	 */
	public void setHysteresisTime(double val) {
		core_.parameters.setHysteresisTime(val);
		clearHysteresis();
	}

	/** 
	 * Set hysteresis time to value in specified units [u].
	 */
	public void setHysteresisTime(double val, String u) {
		core_.parameters.setHysteresisTime(val,u);
		clearHysteresis();
	}

	/** 
	 * Set alerting persistence time to value in seconds.
	 */
	public void setPersistenceTime(double val) {
		core_.parameters.setPersistenceTime(val);
		clearHysteresis();
	}

	/** 
	 * Set alerting persistence time to value in specified units [u].
	 */
	public void setPersistenceTime(double val, String u) {
		core_.parameters.setPersistenceTime(val,u);
		clearHysteresis();
	}

	/** 
	 * Set persistence in horizontal direction resolution in internal units
	 */
	public void setPersistencePreferredHorizontalDirectionResolution(double val) {
		core_.parameters.setPersistencePreferredHorizontalDirectionResolution(val);
		reset();
	}

	/** 
	 * Set persistence in horizontal direction resolution in given units
	 */
	public void setPersistencePreferredHorizontalDirectionResolution(double val, String u) {
		core_.parameters.setPersistencePreferredHorizontalDirectionResolution(val,u);
		reset();
	}

	/** 
	 * Set persistence in horizontal speed resolution in internal units
	 */
	public void setPersistencePreferredHorizontalSpeedResolution(double val) {
		core_.parameters.setPersistencePreferredHorizontalSpeedResolution(val);
		reset();
	}

	/** 
	 * Set persistence in horizontal speed resolution in given units
	 */
	public void setPersistencePreferredHorizontalSpeedResolution(double val, String u) {
		core_.parameters.setPersistencePreferredHorizontalSpeedResolution(val,u);
		reset();
	}

	/** 
	 * Set persistence in vertical speed resolution in internal units
	 */
	public void setPersistencePreferredVerticalSpeedResolution(double val) {
		core_.parameters.setPersistencePreferredVerticalSpeedResolution(val);
		reset();
	}

	/** 
	 * Set persistence in vertical speed resolution in given units
	 */
	public void setPersistencePreferredVerticalSpeedResolution(double val, String u) {
		core_.parameters.setPersistencePreferredVerticalSpeedResolution(val,u);
		reset();
	}

	/** 
	 * Set persistence in altitude resolution in internal units
	 */
	public void setPersistencePreferredAltitudeResolution(double val) {
		core_.parameters.setPersistencePreferredAltitudeResolution(val);
		reset();
	}

	/** 
	 * Set persistence in altitude resolution in given units
	 */
	public void setPersistencePreferredAltitudeResolution(double val, String u) {
		core_.parameters.setPersistencePreferredAltitudeResolution(val,u);
		reset();
	}

	/** 
	 * Set alerting parameters of M of N strategy
	 */
	public void setAlertingMofN(int m, int n) {
		core_.parameters.setAlertingMofN(m,n);
		clearHysteresis();
	}

	/** 
	 * Sets minimum horizontal separation for recovery bands in internal units [m].
	 */
	public void setMinHorizontalRecovery(double val) {
		core_.parameters.setMinHorizontalRecovery(val);
		reset();
	}

	/** 
	 * Set minimum horizontal separation for recovery bands in specified units [u].
	 */
	public void setMinHorizontalRecovery(double val, String u) {
		core_.parameters.setMinHorizontalRecovery(val,u);
		reset();
	}

	/**
	 * Sets minimum vertical separation for recovery bands in internal units [m].
	 */
	public void setMinVerticalRecovery(double val) {
		core_.parameters.setMinVerticalRecovery(val);
		reset();
	}

	/** 
	 * Set minimum vertical separation for recovery bands in units
	 */
	public void setMinVerticalRecovery(double val, String u) {
		core_.parameters.setMinVerticalRecovery(val,u);
		reset();
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
		reset();
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
		reset();
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
		reset();
	}

	/** 
	 * Sets recovery bands flag for horizontal speed bands to specified value.
	 */ 
	public void setRecoveryHorizontalSpeedBands(boolean flag) {
		core_.parameters.setRecoveryHorizontalSpeedBands(flag);
		reset();
	}

	/** 
	 * Sets recovery bands flag for vertical speed bands to specified value.
	 */ 
	public void setRecoveryVerticalSpeedBands(boolean flag) {
		core_.parameters.setRecoveryVerticalSpeedBands(flag);
		reset();
	}

	/** 
	 * Sets recovery bands flag for altitude bands to specified value.
	 */ 
	public void setRecoveryAltitudeBands(boolean flag) {
		core_.parameters.setRecoveryAltitudeBands(flag);
		reset();
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
		reset();
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
	 * Set factor for computing collision avoidance bands. Factor value is in (0,1]
	 */
	public void setCollisionAvoidanceBandsFactor(double val) {
		core_.parameters.setCollisionAvoidanceBandsFactor(val);
		reset();
	}

	/** 
	 * @return get z-score (number of standard deviations) for horizontal position 
	 */
	public double getHorizontalPositionZScore() {
		return core_.parameters.getHorizontalPositionZScore();
	}

	/** 
	 * Set z-score (number of standard deviations) for horizontal position (non-negative value)
	 */
	public void setHorizontalPositionZScore(double val) {
		core_.parameters.setHorizontalPositionZScore(val);
		reset();
	}

	/** 
	 * @return get min z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMin() {
		return core_.parameters.getHorizontalVelocityZScoreMin();
	}

	/** 
	 * Set min z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMin(double val) {
		core_.parameters.setHorizontalVelocityZScoreMin(val);
		reset();
	}

	/** 
	 * @return get max z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMax() {
		return core_.parameters.getHorizontalVelocityZScoreMax();
	}

	/** 
	 * Set max z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMax(double val) {
		core_.parameters.setHorizontalVelocityZScoreMax(val);
		reset();
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
	 * Set distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val) {
		core_.parameters.setHorizontalVelocityZDistance(val);
		reset();
	}

	/** 
	 * Set distance (in given units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val, String u) {
		core_.parameters.setHorizontalVelocityZDistance(val,u);
		reset();
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical position 
	 */
	public double getVerticalPositionZScore() {
		return core_.parameters.getVerticalPositionZScore();
	}

	/** 
	 * Set z-score (number of standard deviations) for vertical position (non-negative value)
	 */
	public void setVerticalPositionZScore(double val) {
		core_.parameters.setVerticalPositionZScore(val);
		reset();
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical velocity 
	 */
	public double getVerticalSpeedZScore() {
		return core_.parameters.getVerticalSpeedZScore();
	}

	/** 
	 * Set z-score (number of standard deviations) for vertical velocity (non-negative value)
	 */
	public void setVerticalSpeedZScore(double val) {
		core_.parameters.setVerticalSpeedZScore(val);
		reset();
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
	 * Return true if DTA logic is active at current time
	 */
	public boolean isActiveDTALogic() {
		return core_.DTAStatus() != 0;
	}

	/**
	 * Return true if DTA special maneuver guidance is active at current time
	 */
	public boolean isActiveDTASpecialManeuverGuidance() {
		return core_.DTAStatus() > 0;
	}

	/** 
	 * Return true if DAA Terminal Area (DTA) logic is disabled.
	 */ 
	public boolean isDisabledDTALogic() {
		return core_.parameters.getDTALogic() == 0;
	}

	/** 
	 * Return true if DAA Terminal Area (DTA) logic is enabled with horizontal 
	 * direction recovery guidance. If true, horizontal direction recovery is fully enabled, 
	 * but vertical recovery blocks down resolutions when alert is higher than corrective.
	 * NOTE:
	 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
	 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
	 * intruder-centric logic).
	 */ 
	public boolean isEnabledDTALogicWithHorizontalDirRecovery() {
		return core_.parameters.getDTALogic() > 0;
	}

	/** 
	 * Return true if DAA Terminal Area (DTA) logic is enabled without horizontal 
	 * direction recovery guidance. If true, horizontal direction recovery is disabled and 
	 * vertical recovery blocks down resolutions when alert is higher than corrective.
	 * NOTE:
	 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
	 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
	 * intruder-centric logic).
	 */ 
	public boolean isEnabledDTALogicWithoutHorizontalDirRecovery() {
		return core_.parameters.getDTALogic() < 0;
	}

	/** 
	 * Disable DAA Terminal Area (DTA) logic   
	 */ 
	public void disableDTALogic() {
		core_.parameters.setDTALogic(0);
		reset();
	}

	/** 
	 * Enable DAA Terminal Area (DTA) logic with horizontal direction recovery guidance, i.e.,
	 * horizontal direction recovery is fully enabled, but vertical recovery blocks down 
	 * resolutions when alert is higher than corrective.
	 * NOTE:
	 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
	 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
	 * intruder-centric logic).
	 */ 
	public void enableDTALogicWithHorizontalDirRecovery() {
		core_.parameters.setDTALogic(1);
		reset();
	}

	/** 
	 * Enable DAA Terminal Area (DTA) logic withou horizontal direction recovery guidance, i.e.,
	 * horizontal direction recovery is disabled and vertical recovery blocks down 
	 * resolutions when alert is higher than corrective.
	 * NOTE:
	 * When DTA logic is enabled, DAIDALUS automatically switches to DTA alerter and to
	 * special maneuver guidance, when aircraft enters DTA volume (depending on ownship- vs
	 * intruder-centric logic).
	 */ 
	public void enableDTALogicWithoutHorizontalDirRecovery() {
		core_.parameters.setDTALogic(-1);
		reset();
	}

	/** 
	 * Get DAA Terminal Area (DTA) position (lat/lon)
	 */ 
	public Position getDTAPosition() {
		return core_.parameters.getDTAPosition();
	}

	/** 
	 * Set DAA Terminal Area (DTA) latitude (internal units)
	 */ 
	public void setDTALatitude(double lat) {
		core_.parameters.setDTALatitude(lat);
		reset();
	}

	/** 
	 * Set DAA Terminal Area (DTA) latitude in given units
	 */ 
	public void setDTALatitude(double lat, String ulat) {
		core_.parameters.setDTALatitude(lat,ulat);
		reset();
	}

	/** 
	 * Set DAA Terminal Area (DTA) longitude (internal units)
	 */ 
	public void setDTALongitude(double lon) {
		core_.parameters.setDTALongitude(lon);
		reset();
	}

	/** 
	 * Set DAA Terminal Area (DTA) longitude in given units
	 */ 
	public void setDTALongitude(double lon, String ulon) {
		core_.parameters.setDTALongitude(lon,ulon);
		reset();
	}

	/** 
	 * Get DAA Terminal Area (DTA) radius (internal units)
	 */ 
	public double getDTARadius() {
		return core_.parameters.getDTARadius();
	}

	/** 
	 * Get DAA Terminal Area (DTA) radius in given units
	 */ 
	public double getDTARadius(String u) {
		return core_.parameters.getDTARadius(u);
	}

	/** 
	 * Set DAA Terminal Area (DTA) radius (internal units)
	 */ 
	public void setDTARadius(double val) {
		core_.parameters.setDTARadius(val);
		reset();
	}

	/** 
	 * Set DAA Terminal Area (DTA) radius in given units
	 */ 
	public void setDTARadius(double val, String u) {
		core_.parameters.setDTARadius(val,u);
		reset();
	}

	/** 
	 * Get DAA Terminal Area (DTA) height (internal units)
	 */ 
	public double getDTAHeight() {
		return core_.parameters.getDTAHeight();
	}

	/** 
	 * Get DAA Terminal Area (DTA) height in given units
	 */ 
	public double getDTAHeight(String u) {
		return core_.parameters.getDTAHeight(u);
	}

	/** 
	 * Set DAA Terminal Area (DTA) height (internal units)
	 */ 
	public void setDTAHeight(double val) {
		core_.parameters.setDTAHeight(val);
		reset();
	}

	/** 
	 * Set DAA Terminal Area (DTA) height in given units
	 */ 
	public void setDTAHeight(double val, String u) {
		core_.parameters.setDTAHeight(val,u);
		reset();
	}

	/** 
	 * Get DAA Terminal Area (DTA) alerter
	 */ 
	public int getDTAAlerter() {
		return core_.parameters.getDTAAlerter();
	}

	/** 
	 * Set DAA Terminal Area (DTA) alerter
	 */ 
	public void setDTAAlerter(int alerter) {
		core_.parameters.setDTAAlerter(alerter);
		reset();
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
		reset();
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
		reset();
	}

	/**
	 * @param alerter_idx Indice of an alerter (starting from 1)
	 * @return corrective level of alerter at alerter_idx. The corrective level 
	 * is the first alert level that has a region equal to or more severe than corrective_region.
	 * Return -1 if alerter_idx is out of range of if there is no corrective alert level
	 * for this alerter. 
	 */
	public int correctiveAlertLevel(int alerter_idx) {
		return core_.parameters.correctiveAlertLevel(alerter_idx);
	}

	/**
	 * @return maximum number of alert levels for all alerters. Returns 0 if alerter list is empty.
	 */
	public int maxNumberOfAlertLevels() {
		return core_.parameters.maxNumberOfAlertLevels();
	}

	/** 
	 * Set instantaneous bands.
	 */
	public void setInstantaneousBands() {
		core_.parameters.setInstantaneousBands();
		reset();
	}

	/** 
	 * Set kinematic bands.
	 * Set turn rate to 3 deg/s, when type is true; set turn rate to  1.5 deg/s
	 * when type is false;
	 */
	public void setKinematicBands(boolean type) {
		core_.parameters.setKinematicBands(type);
		reset();
	}

	/** 
	 * Disable hysteresis parameters
	 */
	public void disableHysteresis() {
		core_.parameters.disableHysteresis();
		clearHysteresis();
	}

	/**
	 *  Load parameters from file.
	 */
	public boolean loadFromFile(String file) {
		boolean flag = core_.parameters.loadFromFile(file);
		clearHysteresis();
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
		clearHysteresis();
	}

	public void setParameterData(ParameterData p) {
		if (core_.parameters.setParameterData(p)) {
			clearHysteresis();
		}
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

	/* Utility methods */

	/** 
	 * Return core object of bands. For expert users only
	 * DO NOT USE IT, UNLESS YOU KNOW WHAT YOU ARE DOING. EXPERT USE ONLY !!!
	 */
	public DaidalusCore getCore() {
		return core_;
	}

	/**
	 *  Clear hysteresis data 
	 */
	public void clearHysteresis() {
		core_.clear_hysteresis();
		hdir_band_.clear_hysteresis();
		hs_band_.clear_hysteresis();
		vs_band_.clear_hysteresis();
		alt_band_.clear_hysteresis();
	}

	/**
	 *  Clear ownship and traffic state data from this object. This method also
	 *  clears hysteresis data.
	 */
	public void clear() {
		core_.clear();
		hdir_band_.clear_hysteresis();
		hs_band_.clear_hysteresis();
		vs_band_.clear_hysteresis();
		alt_band_.clear_hysteresis();
	}

	private void stale_bands() {
		hdir_band_.stale();
		hs_band_.stale();
		vs_band_.stale();
		alt_band_.stale();
	}

	/**
	 * Set cached values to stale conditions and clear hysteresis variables.
	 */
	public void reset() {
		core_.stale();
		stale_bands();
	}

	/* Main interface methods */

	/**
	 * Compute in acs list of aircraft identifiers contributing to conflict bands for given
	 * conflict bands region.
	 * 1 = FAR, 2 = MID, 3 = NEAR.
	 */
	public void conflictBandsAircraft(List<String> acs, int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			IndexLevelT.toStringList(acs,
					core_.acs_conflict_bands(BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),
					core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to conflict bands for given
	 * conflict bands region.
	 */
	public void conflictBandsAircraft(List<String> acs, BandsRegion region) {
		conflictBandsAircraft(acs,BandsRegion.orderOfRegion(region));
	}

	/**
	 * Return time interval of violation for given conflict bands region
	 * 1 = FAR, 2 = MID, 3 = NEAR
	 */
	public Interval timeIntervalOfConflict(int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			return core_.tiov(BandsRegion.NUMBER_OF_CONFLICT_BANDS-region);
		}
		return Interval.EMPTY;
	}

	/**
	 * Return time interval of violation for given conflict bands region
	 */
	public Interval timeIntervalOfConflict(BandsRegion region) {
		return timeIntervalOfConflict(BandsRegion.orderOfRegion(region));
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
	 * Return last time to horizontal direction maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToHorizontalDirectionManeuver(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  lt2m = hdir_band_.last_time_to_maneuver(core_,core_.traffic.get(ac_idx-1));
			if (Double.isNaN(lt2m)) {
				return Double.POSITIVE_INFINITY;
			}
			return lt2m;
		} else {
			error.addError("lastTimeToHorizontalDirectionManeuver: aircraft index "+ac_idx+" is out of bounds");
			return Double.NaN;
		} 
	}

	/**
	 * Return last time to horizontal direction maneuver, in given units, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToHorizontalDirectionManeuver(int ac_idx, String u) {
		double lt2m = lastTimeToHorizontalDirectionManeuver(ac_idx);
		if (Double.isFinite(lt2m)) {
			return Units.to(u,lt2m);
		} else {
			return lt2m;
		}
	}

	/**
	 * @return recovery information for horizontal direction bands.
	 */
	public RecoveryInformation horizontalDirectionRecoveryInformation() {
		return hdir_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal direction bands 
	 * for given conflict bands region.
	 * 1 = FAR, 2 = MID, 3 = NEAR
	 */
	public void peripheralHorizontalDirectionBandsAircraft(List<String> acs, int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			IndexLevelT.toStringList(acs,hdir_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal direction bands 
	 * for given conflict bands region.
	 */
	public void peripheralHorizontalDirectionBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralHorizontalDirectionBandsAircraft(acs,BandsRegion.orderOfRegion(region));
	}

	/**
	 * Compute horizontal direction resolution maneuver for a given direction.
	 * @param dir is right (true)/left (false) of ownship current direction
	 * @return direction resolution in internal units [rad] in specified direction.
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionResolution(boolean dir) {
		return hdir_band_.resolution(core_,dir);
	}

	/**
	 * Compute horizontal direction resolution maneuver for a given direction.
	 * @param dir is right (true)/left (false) of ownship current direction
	 * @param u units
	 * @return direction resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionResolution(boolean dir, String u) {
		return Units.to(u,horizontalDirectionResolution(dir));
	}

	/**
	 * Compute horizontal direction *raw* resolution maneuver for a given direction.
	 * Raw resolution is the resolution without persistence
	 * @param dir is right (true)/left (false) of ownship current direction
	 * @return direction resolution in internal units [rad] in specified direction.
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionRawResolution(boolean dir) {
		return hdir_band_.raw_resolution(core_,dir);
	}

	/**
	 * Compute horizontal direction *raw* resolution maneuver for a given direction.
	 * Raw resolution is the resolution without hysteresis or persistence
	 * @param dir is right (true)/left (false) of ownship current direction
	 * @param u units
	 * @return direction resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in time seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no resolution to the right, and negative infinity if there 
	 * is no resolution to the left.
	 */
	public double horizontalDirectionRawResolution(boolean dir, String u) {
		return Units.to(u,horizontalDirectionRawResolution(dir));
	}

	/**
	 * Compute preferred horizontal direction based on resolution that is closer to current direction.
	 * @return True: Right. False: Left. 
	 */
	public boolean preferredHorizontalDirectionRightOrLeft() {
		return hdir_band_.preferred_direction(core_);
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
	 * Return last time to horizontal speed maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToHorizontalSpeedManeuver(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  lt2m = hs_band_.last_time_to_maneuver(core_,core_.traffic.get(ac_idx-1));
			if (Double.isNaN(lt2m)) {
				return Double.POSITIVE_INFINITY;
			}
			return lt2m;
		} else {
			error.addError("lastTimeToHorizontalSpeedManeuver: aircraft index "+ac_idx+" is out of bounds");
			return Double.NaN;
		} 
	}

	/**
	 * Return last time to horizontal speed maneuver, in given units, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToHorizontalSpeedManeuver(int ac_idx, String u) {
		double lt2m = lastTimeToHorizontalSpeedManeuver(ac_idx);
		if (Double.isFinite(lt2m)) {
			return Units.to(u,lt2m);
		} else {
			return lt2m;
		}
	}

	/**
	 * @return recovery information for horizontal speed bands.
	 */
	public RecoveryInformation horizontalSpeedRecoveryInformation() {
		return hs_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal speed bands 
	 * for given conflict bands region.
	 * 1 = FAR, 2 = MID, 3 = NEAR
	 */
	public void peripheralHorizontalSpeedBandsAircraft(List<String> acs, int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			IndexLevelT.toStringList(acs,hs_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral horizontal speed bands 
	 * for given conflict bands region.
	 */
	public void peripheralHorizontalSpeedBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralHorizontalSpeedBandsAircraft(acs,BandsRegion.orderOfRegion(region));
	}

	/**
	 * Compute horizontal speed resolution maneuver.
	 * @param dir is up (true)/down (false) of ownship current horizontal speed
	 * @return horizontal speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedResolution(boolean dir) {
		return hs_band_.resolution(core_,dir);
	}

	/**
	 * Compute horizontal speed resolution maneuver for corrective region.
	 * @param dir is up (true)/down (false) of ownship current horizontal speed
	 * @param u units
	 * @return horizontal speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedResolution(boolean dir, String u) {
		return Units.to(u,horizontalSpeedResolution(dir));
	}

	/**
	 * Compute horizontal speed *raw* resolution maneuver.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current horizontal speed
	 * @return horizontal speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedRawResolution(boolean dir) {
		return hs_band_.raw_resolution(core_,dir);
	}

	/**
	 * Compute horizontal speed *raw* resolution maneuver for corrective region.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current horizontal speed
	 * @param u units
	 * @return horizontal speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double horizontalSpeedRawResolution(boolean dir, String u) {
		return Units.to(u,horizontalSpeedRawResolution(dir));
	}

	/**
	 * Compute preferred horizontal speed direction on resolution that is closer to current horizontal speed.
	 * True: Increase speed, False: Decrease speed.
	 */
	public boolean preferredHorizontalSpeedUpOrDown() {
		return hs_band_.preferred_direction(core_);
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
	 * Return last time to vertical speed maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToVerticalSpeedManeuver(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  lt2m = vs_band_.last_time_to_maneuver(core_,core_.traffic.get(ac_idx-1));
			if (Double.isNaN(lt2m)) {
				return Double.POSITIVE_INFINITY;
			}
			return lt2m;
		} else {
			error.addError("lastTimeToVerticalSpeedManeuver: aircraft index "+ac_idx+" is out of bounds");
			return Double.NaN;
		} 
	}

	/**
	 * Return last time to vertical speed maneuver, in given units, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToVerticalSpeedManeuver(int ac_idx, String u) {
		double lt2m = lastTimeToVerticalSpeedManeuver(ac_idx);
		if (Double.isFinite(lt2m)) {
			return Units.to(u,lt2m);
		} else {
			return lt2m;
		}
	}

	/**
	 * @return recovery information for vertical speed bands.
	 */
	public RecoveryInformation verticalSpeedRecoveryInformation() {
		return vs_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral vertical speed bands 
	 * for conflict bands region.
	 * 1 = FAR, 2 = MID, 3 = NEAR
	 */
	public void peripheralVerticalSpeedBandsAircraft(List<String> acs, int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			IndexLevelT.toStringList(acs,vs_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral vertical speed bands 
	 * for conflict bands region.
	 */
	public void peripheralVerticalSpeedBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralVerticalSpeedBandsAircraft(acs,BandsRegion.orderOfRegion(region));
	}

	/**
	 * Compute vertical speed resolution maneuver for given direction.
	 * @param dir is up (true)/down (false) of ownship current vertical speed
	 * @return vertical speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedResolution(boolean dir) {
		return vs_band_.resolution(core_,dir);
	}

	/**
	 * Compute vertical speed resolution maneuver for given direction.
	 * @param dir is up (true)/down (false) of ownship current vertical speed
	 * @param u units
	 * @return vertical speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedResolution(boolean dir, String u) {
		return Units.to(u,verticalSpeedResolution(dir));
	}

	/**
	 * Compute vertical speed *raw* resolution maneuver for given direction.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current vertical speed
	 * @return vertical speed resolution in internal units [m/s] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedRawResolution(boolean dir) {
		return vs_band_.raw_resolution(core_,dir);
	}

	/**
	 * Compute vertical speed *raw* resolution maneuver for given direction.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current vertical speed
	 * @param u units
	 * @return vertical speed resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double verticalSpeedRawResolution(boolean dir, String u) {
		return Units.to(u,verticalSpeedRawResolution(dir));
	}

	/**
	 * Compute preferred  vertical speed direction based on resolution that is closer to current vertical speed.
	 * True: Increase speed, False: Decrease speed.
	 */
	public boolean preferredVerticalSpeedUpOrDown() {
		return vs_band_.preferred_direction(core_);
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
	 * Return last time to altitude maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToAltitudeManeuver(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  lt2m = alt_band_.last_time_to_maneuver(core_,core_.traffic.get(ac_idx-1));
			if (Double.isNaN(lt2m)) {
				return Double.POSITIVE_INFINITY;
			}
			return lt2m;
		} else {
			error.addError("lastTimeToAltitudeManeuver: aircraft index "+ac_idx+" is out of bounds");
			return Double.NaN;
		} 
	}

	/**
	 * Return last time to altitude maneuver, in given units, for ownship with respect to traffic
	 * aircraft at index ac_idx. Return positive infinity if the ownship is not in conflict with 
	 * aircraft within lookahead time. Return negative infinity if there is no time to maneuver.
	 * Return NaN if ac_idx is not a valid index.
	 */
	public double lastTimeToAltitudeManeuver(int ac_idx, String u) {
		double lt2m = lastTimeToAltitudeManeuver(ac_idx);
		if (Double.isFinite(lt2m)) {
			return Units.to(u,lt2m);
		} else {
			return lt2m;
		}
	}

	/**
	 * @return recovery information for altitude speed bands.
	 */
	public RecoveryInformation altitudeRecoveryInformation() {
		return alt_band_.recoveryInformation(core_);
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral altitude bands 
	 * for conflict bands region.
	 * 1 = FAR, 2 = MID, 3 = NEAR
	 */
	public void peripheralAltitudeBandsAircraft(List<String> acs, int region) {
		if (0 < region && region <= BandsRegion.NUMBER_OF_CONFLICT_BANDS) {
			IndexLevelT.toStringList(acs,alt_band_.acs_peripheral_bands(core_,
					BandsRegion.NUMBER_OF_CONFLICT_BANDS-region),core_.traffic);
		} else {
			acs.clear();
		}
	}

	/**
	 * Compute in acs list of aircraft identifiers contributing to peripheral altitude bands 
	 * for conflict bands region.
	 */
	public void peripheralAltitudeBandsAircraft(List<String> acs, BandsRegion region) {
		peripheralAltitudeBandsAircraft(acs,BandsRegion.orderOfRegion(region));
	}

	/**
	 * Compute altitude resolution maneuver for given direction.
	 * @param dir is up (true)/down (false) of ownship current altitude
	 * @return altitude resolution in internal units [m] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeResolution(boolean dir) {
		return alt_band_.resolution(core_,dir);
	}

	/**
	 * Compute altitude resolution maneuver for given direction.
	 * @param dir is up (true)/down (false) of ownship current altitude
	 * @param u units
	 * @return altitude resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeResolution(boolean dir, String u) {
		return Units.to(u,altitudeResolution(dir));
	}

	/**
	 * Compute altitude *raw* resolution maneuver for given direction.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current altitude
	 * @return altitude resolution in internal units [m] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeRawResolution(boolean dir) {
		return alt_band_.raw_resolution(core_,dir);
	}

	/**
	 * Compute altitude *raw* resolution maneuver for given direction.
	 * Raw resolution is the resolution without persistence
	 * @param dir is up (true)/down (false) of ownship current altitude
	 * @param u units
	 * @return altitude resolution in specified units [u] in specified direction. 
	 * Resolution maneuver is valid for lookahead time in seconds. Return NaN if there is no conflict, 
	 * positive infinity if there is no up resolution, and negative infinity if there 
	 * is no down resolution.
	 */
	public double altitudeRawResolution(boolean dir, String u) {
		return Units.to(u,altitudeRawResolution(dir));
	}

	/**
	 * Compute preferred  altitude direction on resolution that is closer to current altitude.
	 * True: Climb, False: Descend.
	 */
	public boolean preferredAltitudeUpOrDown() {
		return alt_band_.preferred_direction(core_);
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
	 * - {@code turning < 0}: ownship is turning left, {@code turning > 0}: ownship is turning right, turning = 0: 
	 * do not make any turning assumption about the ownship.
	 * - {@code accelerating < 0}: ownship is decelerating, {@code accelerating > 0}: ownship is accelerating, 
	 * accelerating = 0: do not make any accelerating assumption about the ownship.
	 * - {@code climbing < 0}: ownship is descending, {@code climbing > 0}: ownship is climbing, climbing = 0:
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

	/**
	 * Return the most severe alert level with respect to all traffic aircraft
	 * The number 0 means no alert. A negative number means no traffic aircraft
	 */
	public int alertLevelAllTraffic() {
		int max = -1;
		for (int ac_idx=1; ac_idx <= lastTrafficIndex(); ++ac_idx) {
			int alert = alertLevel(ac_idx);
			if (alert > max) {
				max = alert;
			}
		}
		return max;
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
			int alerter_idx = core_.alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= core_.parameters.numberOfAlerters()) {
				Alerter alerter = core_.parameters.getAlerterAt(alerter_idx);
				if (alert_level == 0) {
					alert_level = core_.parameters.correctiveAlertLevel(alerter_idx);
				}
				if (alert_level > 0) {
					Optional<Detection3D> detector = alerter.getDetector(alert_level);
					if (detector.isPresent()) {
						return detector.get().conflictDetectionWithTrafficState(core_.ownship,intruder,0.0,core_.parameters.getLookaheadTime());
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
	 * Detects violation of alert thresholds for a given region with an
	 * aircraft at index ac_idx.
	 * Conflict data provides time to violation and time to end of violation 
	 * of alert thresholds of given alert level. 
	 * @param ac_idx is the index of the traffic aircraft 
	 * @param region region used to compute detection.
	 */
	public ConflictData violationOfAlertThresholds(int ac_idx, BandsRegion region) {
		return violationOfAlertThresholds(ac_idx,alertLevelOfRegion(ac_idx,region));
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

	/** 
	 * @return region corresponding to a given alert level for a particular aircraft.
	 * This function first finds the alerter for this aircraft, based on ownship/intruder-centric 
	 * logic, then returns the configured region for the alerter level. It returns
	 * UNKNOWN if the aircraft or the alert level are invalid.
	 */
	public BandsRegion regionOfAlertLevel(int ac_idx, int alert_level) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			TrafficState intruder = core_.traffic.get(ac_idx-1);
			int alerter_idx = core_.alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= core_.parameters.numberOfAlerters()) {
				if (alert_level == 0) {
					return BandsRegion.NONE;
				}
				Alerter alerter = core_.parameters.getAlerterAt(alerter_idx);
				if (0 < alert_level && alert_level <= alerter.mostSevereAlertLevel()) {
					return alerter.getLevel(alert_level).getRegion();
				} else {
					error.addError("regionOfAlertLevel: alert_level "+alert_level+" for  "+ac_idx+" is not set");
				}
			} else {
				error.addError("regionOfAlertLevel: alerter of traffic aircraft "+ac_idx+" is out of bounds");
			}
		} else {
			error.addError("regionOfAlertLevel: aircraft index "+ac_idx+" is out of bounds");
		}
		return BandsRegion.UNKNOWN;	
	}

	/** 
	 * @return alert_level corresponding to a given region for a particular aircraft.
	 * This function first finds the alerter for this aircraft, based on ownship/intruder-centric
	 * logic, then returns the configured region for the region. It returns -1
	 * if the aircraft or the alert level are invalid.
	 * 0 = NONE, 1 = FAR, 2 = MID, 3 = NEAR.
	 */
	public int alertLevelOfRegion(int ac_idx, int region) {
		return alertLevelOfRegion(ac_idx,BandsRegion.regionFromOrder(region));		
	}

	/** 
	 * @return alert_level corresponding to a given region for a particular aircraft.
	 * This function first finds the alerter for this aircraft, based on ownship/intruder-centric
	 * logic, then returns the configured region for the region. It returns -1
	 * if the aircraft or its alerter are invalid. 
	 */
	public int alertLevelOfRegion(int ac_idx, BandsRegion region) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			TrafficState intruder = core_.traffic.get(ac_idx-1);
			int alerter_idx = core_.alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= core_.parameters.numberOfAlerters()) {
				Alerter alerter = core_.parameters.getAlerterAt(alerter_idx);
				return alerter.alertLevelForRegion(region);
			} else {
				error.addError("alertLevelOfRegion: alerter of traffic aircraft "+ac_idx+" is out of bounds");
			}
		} else {
			error.addError("alertLevelOfRegion: aircraft index "+ac_idx+" is out of bounds");
		}
		return -1;	
	}

	/* DAA Performance Metrics */

	/** 
	 * Returns current horizontal separation, in internal units, with aircraft at index ac_idx.
	 * Returns NaN if aircraft index is not valid
	 */
	public double currentHorizontalSeparation(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect3 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s());
			return s.norm2D();
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns current horizontal separation, in given units, with aircraft at index ac_idx. 
	 * Returns NaN if aircraft index is not valid
	 */
	public double currentHorizontalSeparation(int ac_idx,String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,currentHorizontalSeparation(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns current vertical separation, in internal units, with aircraft at index ac_idx.
	 * Returns NaN if aircraft index is not valid
	 */
	public double currentVerticalSeparation(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  sz = core_.ownship.get_s().z - core_.traffic.get(ac_idx-1).get_s().z;
			return Math.abs(sz);
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns current vertical separation, in given units, with aircraft at index ac_idx. 
	 * Returns NaN if aircraft index is not valid
	 */
	public double currentVerticalSeparation(int ac_idx,String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,currentVerticalSeparation(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns horizontal closure rate, in internal units, with aircraft at index ac_idx.
	 * Returns NaN if aircraft index is not valid
	 */
	public double horizontalClosureRate(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Velocity v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v());
			return v.norm2D();
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns current closure rate, in given units, with aircraft at index ac_idx. 
	 * Returns NaN if aircraft index is not valid
	 */
	public double horizontalClosureRate(int ac_idx,String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,horizontalClosureRate(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns vertical closure rate, in internal units, with aircraft at index ac_idx.
	 * Returns NaN if aircraft index is not valid
	 */
	public double verticalClosureRate(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double  vz = core_.ownship.get_v().z - core_.traffic.get(ac_idx-1).get_v().z;
			return Math.abs(vz);
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns vertical closure rate, in given units, with aircraft at index ac_idx. 
	 * Returns NaN if aircraft index is not valid
	 */
	public double verticalClosureRate(int ac_idx,String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,verticalClosureRate(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns predicted HMD, in internal units, with aircraft at index ac_idx (up to lookahead time), 
	 * assuming straight line trajectory. Returns NaN if aircraft index is not valid
	 */
	public double predictedHorizontalMissDistance(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect3 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s());
			Velocity v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v());
			return Horizontal.hmd(s.vect2(),v.vect2(),getLookaheadTime());
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns predicted HMD, in provided units, with aircraft at index ac_idx (up to lookahead time), 
	 * assuming straight line trajectory. Returns NaN if aircraft index is not valid
	 */
	public double predictedHorizontalMissDistance(int ac_idx, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,predictedHorizontalMissDistance(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns predicted VMD, in internal units, with aircraft at index ac_idx (up to lookahead time), 
	 * assuming straight line trajectory. Returns NaN if aircraft index is not valid
	 */
	public double predictedVerticalMissDistance(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect3 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s());
			Velocity v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v());
			return Vertical.vmd(s.z,v.z,getLookaheadTime());
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns predicted VMD, in provided units, with aircraft at index ac_idx (up to lookahead time), 
	 * assuming straight line trajectory. Return NaN if aircraft index is not valid
	 */
	public double predictedVerticalMissDistance(int ac_idx, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,predictedVerticalMissDistance(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns time, in seconds, to horizontal closest point of approach with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, the returned time is 0.
	 * Returns NaN if aircraft index is not valid
	 */
	public double timeToHorizontalClosestPointOfApproach(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect3 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s());
			Velocity v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v());
			return Util.max(0.0,Horizontal.tcpa(s.vect2(),v.vect2()));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns time, in given units, to horizontal closest point of approach with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, the returned time is 0.
	 * Returns NaN if aircraft index is not valid
	 */
	public double timeToHorizontalClosestPointOfApproach(int ac_idx, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,timeToHorizontalClosestPointOfApproach(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns distance, in internal units, at horizontal closest point of approach with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, the returned distance is current horizontal separation.
	 * Returns NaN if aircraft index is not valid
	 */
	public double distanceAtHorizontalClosestPointOfApproach(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect3 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s());
			Velocity v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v());
			double tcpa = Horizontal.tcpa(s.vect2(),v.vect2());
			if (tcpa <= 0) {
				return s.norm2D();
			} else {
				return s.AddScal(tcpa,v).norm2D();
			}
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns distance, in given units, at horizontal closest point of approach with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, the returned distance is current horizontal separation.
	 * Returns NaN if aircraft index is not valid
	 */
	public double distanceAtHorizontalClosestPointOfApproach(int ac_idx, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,distanceAtHorizontalClosestPointOfApproach(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns time, in seconds, to co-altitude with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, returns negative time. If 
	 * vertical closure is 0, returns negative infinite.
	 * Returns NaN if aircraft index is not valid or if vertical closure is 0.
	 */
	public double timeToCoAltitude(int ac_idx) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			double sz = core_.ownship.get_s().z-core_.traffic.get(ac_idx-1).get_s().z;
			double vz = core_.ownship.get_v().z-core_.traffic.get(ac_idx-1).get_v().z;
			if (Util.almost_equals(vz,0.0)) {
				return Double.NEGATIVE_INFINITY;
			}
			return Vertical.time_coalt(sz,vz);
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns time, in given units, to co-altitude with aircraft 
	 * at index ac_idx, assuming straight line trajectory. 
	 * If aircraft are diverging, returns negative value.
	 * Returns NaN if aircraft index is not valid or if vertical closure is 0
	 */
	public double timeToCoAltitude(int ac_idx, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,timeToCoAltitude(ac_idx));
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns modified tau time, in seconds, for distance DMOD (given in internal units), 
	 * with respect to aircraft at index ac_idx. 
	 * If aircraft are diverging or DMOD is greater than current range, returns -1.
	 * Returns NaN if aircraft index is not valid or if vertical closure is 0
	 */
	public double modifiedTau(int ac_idx, double DMOD) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			Vect2 s = core_.ownship.get_s().Sub(core_.traffic.get(ac_idx-1).get_s()).vect2();
			Vect2 v = core_.ownship.get_v().Sub(core_.traffic.get(ac_idx-1).get_v()).vect2();
			double sdotv = s.dot(v);
			double dmod2 = Util.sq(DMOD)-s.sqv();
			if (dmod2 < 0 && sdotv < 0) {
				return dmod2/sdotv;
			}
			return -1;
		} else {
			return Double.NaN;
		}
	}

	/** 
	 * Returns modified tau time, in given units, for distance DMOD (given in DMODu units), 
	 * with respect to aircraft at index ac_idx. 
	 * If aircraft are diverging or DMOD is greater than current range, returns -1.
	 * Returns NaN if aircraft index is not valid or if vertical closure is 0
	 */
	public double modifiedTau(int ac_idx, double DMOD, String DMODu, String u) {
		if (1 <= ac_idx && ac_idx <= lastTrafficIndex()) {
			return Units.to(u,modifiedTau(ac_idx,Units.from(DMODu,DMOD)));
		} else {
			return Double.NaN;
		}
	}

	/* Input/Output methods */

	public String outputStringAircraftStates() {
		return core_.outputStringAircraftStates(false);
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
					s+="## Direction Bands\n";
					s+=hdir_band_.toString();
				}
				if (hs_band_.isFresh()) {
					s+="## Horizontal Speed Bands\n";
					s+=hs_band_.toString();
				}
				if (vs_band_.isFresh()) {
					s+="## Vertical Speed Bands\n";
					s+=vs_band_.toString();
				}
				if (alt_band_.isFresh()) {
					s+="## Altitude Bands\n";
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
		s+="Time: "+Units.str("s",core_.current_time)+"\n";
		s+= outputStringAircraftStates();
		s+="Conflict Criteria: "+(core_.parameters.isEnabledConflictCriteria()?"Enabled":"Disabled")+"\n";
		s+="Recovery Criteria: "+(core_.parameters.isEnabledRecoveryCriteria()?"Enabled":"Disabled")+"\n";
		s+="Most Urgent Aircraft: "+core_.mostUrgentAircraft().getId()+"\n";
		s+="Horizontal Epsilon: "+core_.epsilonH()+"\n";
		s+="Vertical Epsilon: "+core_.epsilonV()+"\n";
		List<String> acs = new ArrayList<String>();
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			conflictBandsAircraft(acs,region);
			s+="Conflict Bands Aircraft ("+region.toString()+"): "+TrafficState.listToString(acs)+"\n";
		}	
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			s+="Time Interval of Conflict ("+region.toString()+"): "+
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			peripheralHorizontalDirectionBandsAircraft(acs,region);
			s+="Peripheral Horizontal Direction Bands Aircraft ("+region.toString()+"): "+
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			peripheralHorizontalSpeedBandsAircraft(acs,region);
			s+="Peripheral Horizontal Speed Bands Aircraft ("+region.toString()+"): "+
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			peripheralVerticalSpeedBandsAircraft(acs,region);
			s+="Peripheral Vertical Speed Bands Aircraft ("+region.toString()+"): "+
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			peripheralAltitudeBandsAircraft(acs,region);
			s+="Peripheral Altitude Bands Aircraft ("+region.toString()+"): "+
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += region.toString()+"_:= "+core_.bands_for(BandsRegion.NUMBER_OF_CONFLICT_BANDS-regidx);
		}
		s += " #)\n";
		s += "%%% OUTPUTS %%%\n";
		s += "%%% Conflict Bands Aircraft (FAR,MID,NEAR):\n";
		s += "( ";
		List<String> acs = new ArrayList<String>();
		comma = false;	
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
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
		for (int regidx=1; regidx <= BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++regidx) {
			BandsRegion region = BandsRegion.regionFromOrder(regidx);
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
		return error.hasError() || core_.parameters.hasError();
	}

	public boolean hasMessage() {
		return error.hasMessage() || core_.parameters.hasMessage();
	}

	public String getMessage() {
		return error.getMessage()+"; "+core_.parameters.getMessage();
	}

	public String getMessageNoClear() {
		return error.getMessageNoClear()+"; "+core_.parameters.getMessageNoClear();
	}

	/* Deprecated Methods */

	@Deprecated
	/**
	 * Use setOwnshipState instead.
	 * Set ownship state at time 0.0. Clear all traffic. 
	 * @param id Ownship's identified
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */
	public void setOwnship(String id, Position pos, Velocity vel) {
		setOwnshipState(id,pos,vel);
	}

	@Deprecated
	/**
	 * Use setOwnshipState instead.
	 * Set ownship state at time 0.0. Clear all traffic. Name of ownship will be "Ownship"
	 * @param pos Ownship's position
	 * @param vel Ownship's ground velocity
	 */
	public void setOwnship(Position pos, Velocity vel) {
		setOwnshipState("Ownship",pos,vel);
	}

	@Deprecated
	/**
	 * Use addTrafficState instead.
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

	@Deprecated
	/**
	 * Use addTrafficState instead
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
	/**
	 * Use alertLevel instead
	 */
	public int alerting(int ac_idx) {
		return alertLevel(ac_idx);
	}

	@Deprecated
	/**
	 * Use setMaxHorizontalSpeed instead
	 */
	public void setMaxGroundSpeed(double gs, String unit) {
		setMaxHorizontalSpeed(gs,unit);		
	}

	@Deprecated
	/**
	 * Use getMaxHorizontalSpeed instead
	 */
	public double getMaxGroundSpeed(String unit) {
		return getMaxHorizontalSpeed(unit);
	}

	@Deprecated
	/**
	 * Use horizontalDirectionBandsLength instead
	 */
	public int trackLength() {
		return horizontalDirectionBandsLength();
	}

	@Deprecated
	/**
	 * Use horizontalDirectionIntervalAt instead
	 */
	public Interval track(int i, String unit) {
		return horizontalDirectionIntervalAt(i,unit);
	}

	@Deprecated
	/**
	 * Use horizontalDirectionRegionAt instead
	 */
	public BandsRegion trackRegion(int i) {
		return horizontalDirectionRegionAt(i);
	}

	@Deprecated
	/**
	 * Use regionOfHorizontalDirection instead
	 */
	public BandsRegion regionOfTrack(double trk, String unit) {
		return regionOfHorizontalDirection(trk,unit);
	}

	@Deprecated
	/**
	 * Use horizontalSpeedBandsLength instead
	 */
	public int groundSpeedLength() {
		return horizontalSpeedBandsLength();
	}

	@Deprecated
	/**
	 * Use horizontalSpeedIntervalAt instead
	 */
	public Interval groundSpeed(int i, String unit) {
		return horizontalSpeedIntervalAt(i,unit);
	}

	@Deprecated
	/**
	 * Use horizontalSpeedRegionAt instead
	 */
	public BandsRegion groundSpeedRegion(int i) {
		return horizontalSpeedRegionAt(i);
	}

	@Deprecated
	/**
	 * Use regionOfHorizontalSpeed instead
	 */
	public BandsRegion regionOfGroundSpeed(double gs, String unit) {
		return regionOfHorizontalSpeed(gs,unit);
	}

	@Deprecated
	/**
	 * Use verticalSpeedBandsLength instead
	 */
	public int verticalSpeedLength() {
		return verticalSpeedBandsLength();
	}

	@Deprecated
	/**
	 * Use verticalSpeedIntervalAt instead
	 */
	public Interval verticalSpeed(int i, String unit) {
		return verticalSpeedIntervalAt(i,unit);
	}

	@Deprecated
	/**
	 * Use verticalSpeedRegionAt instead
	 */
	public BandsRegion verticalSpeedRegion(int i) {
		return verticalSpeedRegionAt(i);
	}

	/**
	 * @return maximum alert level for all alerters. Returns 0 if alerter list is empty.
	 */
	@Deprecated
	public int maxAlertLevelxxx() {
		return maxNumberOfAlertLevels();
	}

}

