/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DaidalusCore {

	/*** PUBLIC VARIABLES */

	/* Absolute ownship state */
	public TrafficState ownship;
	/* Absolute list of traffic states */
	public List<TrafficState> traffic;
	/* Current time */
	public double current_time;   
	/* Wind vector in TO direction */
	public Velocity wind_vector;  
	/* Kinematic bands parameters */
	public DaidalusParameters parameters;
	/* Strategy for most urgent aircraft */
	public UrgencyStrategy urgency_strategy; 
	
	/**** CACHED VARIABLES ****/

	/* Variable to control re-computation of cached values */
	private int cache_; // -1: outdated, 1:updated, 0: updated only most_urgent_ac and eps values
	/* Most urgent aircraft */
	public TrafficState most_urgent_ac_; 
	/* Cached horizontal epsilon for implicit coordination */
	private int epsh_; 
	/* Cached vertical epsilon for implicit coordination */
	private int epsv_; 
	/* Cached value of DTA status given current aircraft states. 
	 *  0 : Not in DTA 
	 * -1 : In DTA, but special bands are not enabled yet 
	 *  1 : In DTA and special bands are enabled 
	 */
	private int dta_status_;
	/* Cached lists of aircraft indices, alert_levels, and lookahead times sorted by indices, contributing to conflict (non-peripheral) 
	 * band listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private List<List<IndexLevelT>> acs_conflict_bands_; 
	/* Cached list of time to violation per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private Interval[] tiov_; 
	/* Cached list of boolean alues indicating which bands should be computed, where 0th:NEAR, 1th:MID, 2th:FAR.
	 * NaN means that bands are not computed for that region*/
	private boolean[] bands4region_;

	/**** HYSTERESIS VARIABLES ****/

	// Alerting and DTA hysteresis per aircraft's ids
	private Map<String,HysteresisData> alerting_hysteresis_acs_; 
	private Map<String,HysteresisData> dta_hysteresis_acs_; 

	private void init() {

		// Public variables are initialized
		ownship = TrafficState.INVALID;
		traffic = new ArrayList<TrafficState>(); 
		current_time = 0;
		wind_vector = Velocity.ZERO;
		parameters = new DaidalusParameters();
		urgency_strategy = NoneUrgencyStrategy.NONE_URGENCY_STRATEGY;

		// Cached arrays_ are initialized
		acs_conflict_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_conflict_bands_.add(new ArrayList<IndexLevelT>());
		}
		tiov_ = new Interval[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		bands4region_ = new boolean[BandsRegion.NUMBER_OF_CONFLICT_BANDS];

		// Hysteresis variables are initialized
		alerting_hysteresis_acs_ = new HashMap<String,HysteresisData>();
		dta_hysteresis_acs_ = new HashMap<String,HysteresisData>();

		// Cached_ variables are cleared
		cache_ = 0; 
		stale();
	}

	public DaidalusCore() {
		init();
	}

	public DaidalusCore(Alerter alerter) {
		init();
		parameters.addAlerter(alerter);
	}

	public DaidalusCore(Detection3D det, double T) {
		init();
		parameters.addAlerter(Alerter.SingleBands(det,T,T));
		parameters.setLookaheadTime(T);
	}

	public DaidalusCore(DaidalusCore core) {
		// Public arrays are initialized
		traffic = new ArrayList<TrafficState>(); 

		// Cached arrays_ are initialized
		acs_conflict_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_conflict_bands_.add(new ArrayList<IndexLevelT>());
		}
		tiov_ = new Interval[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		bands4region_ = new boolean[BandsRegion.NUMBER_OF_CONFLICT_BANDS];

		// Hysteresis variables are initialized
		alerting_hysteresis_acs_ = new HashMap<String,HysteresisData>();
		dta_hysteresis_acs_ = new HashMap<String,HysteresisData>();

		// Public variables are copied
		ownship = core.ownship;
		traffic.addAll(core.traffic);
		current_time = core.current_time;
		wind_vector = core.wind_vector;
		parameters = new DaidalusParameters(core.parameters);
		urgency_strategy = core.urgency_strategy != null ? core.urgency_strategy.copy() : NoneUrgencyStrategy.NONE_URGENCY_STRATEGY;

		// Cached_ variables are cleared
		cache_ = 0; 
		stale();
	}

	/**
	 *  Clear ownship and traffic data from this object.   
	 *  IMPORTANT: This method reset cache and hysteresis parameters. 
	 */
	public void clear() {
		ownship = TrafficState.INVALID;
		traffic.clear();
		current_time = 0; 
		clear_hysteresis();
	}

	/**
	 *  Clear wind vector from this object.   
	 */
	public void clear_wind() {
		set_wind_velocity(Velocity.ZERO);
	}

	public boolean set_alerter_ownship(int alerter_idx) {
		if (ownship.isValid()) {
			ownship.setAlerterIndex(alerter_idx);
			stale();
			return true;
		}
		return false;
	}

	// idx is zero-based
	public boolean set_alerter_traffic(int idx, int alerter_idx) {
		if (0 <= idx && idx < traffic.size()) {
			traffic.get(idx).setAlerterIndex(alerter_idx);
			stale();
			return true;
		}
		return false;
	}

	/**
	 *  Clear hysteresis information from this object.   
	 */
	public void clear_hysteresis() {
		alerting_hysteresis_acs_.clear();
		dta_hysteresis_acs_.clear();
		stale();
	}

	/**
	 * Set cached values to stale conditions as they are no longer fresh.
	 */
	public void stale() {
		if (cache_ >= 0) {
			cache_ = -1;
			most_urgent_ac_ = TrafficState.INVALID;
			epsh_ = 0;
			epsv_ = 0;
			dta_status_ = 0;
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				acs_conflict_bands_.get(conflict_region).clear();
				tiov_[conflict_region] = Interval.EMPTY;
				bands4region_[conflict_region] = false;
			}
			for (HysteresisData alerting_hysteresis: alerting_hysteresis_acs_.values()) {
				alerting_hysteresis.outdateIfCurrentTime(current_time);
			}
			for (HysteresisData dta_hysteresis: dta_hysteresis_acs_.values()) {
				dta_hysteresis.outdateIfCurrentTime(current_time);
			}
		}
	}

	/**
	 * Returns true is object is fresh
	 */
	public boolean isFresh() {
		return cache_ > 0;
	}

	/**
	 *  Refresh cached values 
	 */
	public void refresh() {
		if (cache_ <= 0) {
			for (int ac=0; ac < traffic.size(); ++ac) {
				alert_level(ac,0,0,0);
			}
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				conflict_aircraft(conflict_region);
				BandsRegion region = BandsRegion.regionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
				for (int alerter_idx=1;  alerter_idx <= parameters.numberOfAlerters(); ++alerter_idx) {
					Alerter alerter = parameters.getAlerterAt(alerter_idx);
					int alert_level = alerter.alertLevelForRegion(region);
					if (alert_level > 0) {
						bands4region_[conflict_region] = true;
					}
				}
			}
			dta_status_ = 0; // Not active 
			if (parameters.getDTALogic() != 0 && parameters.getDTAAlerter() != 0) {
				if (parameters.isAlertingLogicOwnshipCentric()) {
					if (alerter_index_of(ownship) == parameters.getDTAAlerter()) {
						dta_status_ = -1; // Inside DTA
					}
				} else {
					for (int ac=0; ac < traffic.size() && dta_status_ == 0; ++ac) {
						if (alerter_index_of(traffic.get(ac)) == parameters.getDTAAlerter()) {
							dta_status_ = -1; // Inside DTA
						}
					}
				}
				if (dta_status_  < 0 && greater_than_corrective()) {
					dta_status_ = 1; //Inside DTA and special bands enabled
				}
			}
			refresh_mua_eps();
			cache_ = 1;
		} 
	}

	private boolean greater_than_corrective() {
		int corrective_idx = parameters.getCorrectiveRegion().orderOfConflictRegion();
		if (corrective_idx > 0){
			for (int region_idx = 0; region_idx < BandsRegion.NUMBER_OF_CONFLICT_BANDS-corrective_idx; ++region_idx) {
				if (!acs_conflict_bands_.get(region_idx).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	private void refresh_mua_eps() {
		if (cache_ < 0) {
			int muac = -1;
			if (!traffic.isEmpty()) {
				muac = urgency_strategy.mostUrgentAircraft(ownship,traffic,parameters.getLookaheadTime());
			} 
			if (muac >= 0) {
				most_urgent_ac_ = traffic.get(muac);
			} else {
				most_urgent_ac_ = TrafficState.INVALID;
			}
			epsh_ = epsilonH(ownship,most_urgent_ac_);
			epsv_ = epsilonV(ownship,most_urgent_ac_);
			cache_ = 0;
		}
	}

	/**
	 * Returns DTA status:
	 * 	0 : DTA is not active 
	 * -1 : DTA is active, but special bands are not enabled yet 
	 *  1 : DTA is active and special bands are enabled 
	 */
	public int DTAStatus() {
		refresh();
		return dta_status_;
	}

	/**
	 * @return most urgent aircraft for implicit coordination 
	 */
	public TrafficState mostUrgentAircraft() {
		refresh_mua_eps();
		return most_urgent_ac_;
	}

	/**
	 * Returns horizontal epsilon for implicit coordination with respect to criteria ac.
	 * 
	 */
	public int epsilonH() {
		refresh_mua_eps();
		return epsh_;
	}

	/**
	 * Returns vertical epsilon for implicit coordination with respect to criteria ac.
	 */
	public int epsilonV() {
		refresh_mua_eps();
		return epsv_;
	}

	/**
	 * Returns horizontal epsilon for implicit coordination with respect to criteria ac.
	 * 
	 */
	public int epsilonH(boolean recovery_case, TrafficState traffic) {
		refresh_mua_eps();
		if ((recovery_case? parameters.isEnabledRecoveryCriteria() : parameters.isEnabledConflictCriteria()) &&
				traffic.sameId(most_urgent_ac_)) {
			return epsh_;
		}
		return 0;
	}

	/**
	 * Returns vertical epsilon for implicit coordination with respect to criteria ac.
	 */
	public int epsilonV(boolean recovery_case, TrafficState traffic) {
		refresh_mua_eps();
		if ((recovery_case? parameters.isEnabledRecoveryCriteria() : parameters.isEnabledConflictCriteria()) &&
				traffic.sameId(most_urgent_ac_)) {
			return epsv_;
		}
		return 0;
	}

	/** 
	 * Return true if bands are computed for this particular region (0:NEAR, 1:MID, 2: FAR)
	 */
	public boolean bands_for(int region) {
		refresh();
		return bands4region_[region]; 
	}

	/**
	 * Returns actual minimum horizontal separation for recovery bands in internal units. 
	 */
	public double minHorizontalRecovery() {
		double min_horizontal_recovery = parameters.getMinHorizontalRecovery();
		if (min_horizontal_recovery > 0) 
			return min_horizontal_recovery;
		int sl = !has_ownship() ? 3 : Util.max(3,TCASTable.TCASII_RA.getSensitivityLevel(ownship.altitude()));
		return TCASTable.TCASII_RA.getHMD(sl);
	}

	/** 
	 * Returns actual minimum vertical separation for recovery bands in internal units. 
	 */
	public double minVerticalRecovery() {
		double min_vertical_recovery = parameters.getMinVerticalRecovery();
		if (min_vertical_recovery > 0) 
			return min_vertical_recovery;
		int sl = !has_ownship() ? 3 : Util.max(3,TCASTable.TCASII_RA.getSensitivityLevel(ownship.altitude()));
		return TCASTable.TCASII_RA.getZTHR(sl);
	}

	public void set_ownship_state(String id, Position pos, Velocity vel, double time) {
		traffic.clear();
		ownship = TrafficState.makeOwnship(id,pos,vel);
		ownship.applyWindVector(wind_vector);
		current_time = time;
		stale(); 
	}

	// Return 0-based index in traffic list (-1 if aircraft doesn't exist)
	public int find_traffic_state(String id) {
		for (int i = 0; i < traffic.size(); ++i) {
			if (traffic.get(i).getId().equals(id)) {
				return i;
			}
		}
		return -1;
	}

	// Return 0-based index in traffic list where aircraft was added. Return -1 if 
	// nothing is done (e.g., id is the same as ownship's)
	public int set_traffic_state(String id, Position pos, Velocity vel, double time) {
		if (ownship.isValid() && ownship.getId().equals(id)) {
			return -1;
		}
		double dt = current_time-time;
		Position pt = dt == 0 ? pos : pos.linear(vel,dt);    
		TrafficState ac = ownship.makeIntruder(id,pt,vel);
		if (ac.isValid()) {
			ac.applyWindVector(wind_vector);
			int idx = find_traffic_state(id);
			if (idx >= 0) {
				traffic.set(idx,ac);
			} else {
				idx = traffic.size();
				traffic.add(ac);
			}
			stale();
			return idx;
		} else {
			return -1;
		}
	}

	// idx is 0-based index in traffic list
	public void reset_ownship(int idx) {
		TrafficState old_own = new TrafficState(ownship);
		ownship = new TrafficState(traffic.get(idx));
		ownship.setAsOwnship();		
		old_own.setAsIntruderOf(ownship);
		for (int i = 0; i < traffic.size(); ++i) {
			if (i == idx) {
				traffic.set(i,old_own);
			} else {
				TrafficState ac = new TrafficState(traffic.get(i));
				ac.setAsIntruderOf(ownship);
				traffic.set(i,ac);
			}
		}
		stale();
	}

	// idx is 0-based index in traffic list
	public boolean remove_traffic(int idx) {
		if (0 <= idx && idx < traffic.size()) {
			dta_hysteresis_acs_.remove(traffic.get(idx).getId());
			alerting_hysteresis_acs_.remove(traffic.get(idx).getId());
			traffic.remove(idx);
			stale();
			return true;
		}
		return false;
	}

	public void set_wind_velocity(Velocity wind) {
		if (has_ownship()) {
			ownship.applyWindVector(wind);
			for (TrafficState ac : traffic) {
				ac.applyWindVector(wind);
			}
		}
		wind_vector = wind;
		stale();
	}

	public boolean linear_projection(double offset) {
		if (offset != 0 && has_ownship()) {
			ownship = ownship.linearProjection(offset);
			for (int i = 0; i < traffic.size(); i++) {
				traffic.set(i,traffic.get(i).linearProjection(offset));
			}  
			current_time += offset;
			stale();
			return true;
		} 
		return false; 
	}

	public boolean has_ownship() {
		return ownship.isValid();
	}

	public boolean has_traffic() {
		return traffic.size() > 0;
	}

	/* idx is a 0-based index in the list of traffic aircraft
	 * returns 1 if detector of traffic aircraft
	 * returns 2 if corrective alerter level is not set
	 * returns 3 if alerter of traffic aircraft is out of bands
	 * otherwise, if there are no errors, returns 0 and the answer is in blobs
	 */
	public int horizontal_contours(List<List<Position>>blobs, int idx, int alert_level) {
		TrafficState intruder = traffic.get(idx);
		int alerter_idx = alerter_index_of(intruder);
		if (1 <= alerter_idx && alerter_idx <= parameters.numberOfAlerters()) {
			Alerter alerter = parameters.getAlerterAt(alerter_idx);
			if (alert_level == 0) {
				alert_level = parameters.correctiveAlertLevel(alerter_idx);
			}
			if (alert_level > 0) {
				Optional<Detection3D> detector = alerter.getDetector(alert_level);
				if (detector.isPresent()) {
					detector.get().horizontalContours(blobs,ownship,intruder,
							parameters.getHorizontalContourThreshold(), 
							parameters.getLookaheadTime());
				} else {
					return 1;
				}
			} else {
				return 2;
			}
		} else {
			return 3;
		}
		return 0;
	} 

	/* idx is a 0-based index in the list of traffic aircraft
	 * returns 1 if detector of traffic aircraft
	 * returns 2 if corrective alerter level is not set
	 * returns 3 if alerter of traffic aircraft is out of bands
	 * otherwise, if there are no errors, returns 0 and the answer is in blobs
	 */
	public int horizontal_hazard_zone(List<Position> haz, int idx, int alert_level, 
			boolean loss, boolean from_ownship) {
		TrafficState intruder = traffic.get(idx);
		int alerter_idx = alerter_index_of(intruder);
		if (1 <= alerter_idx && alerter_idx <= parameters.numberOfAlerters()) {
			Alerter alerter = parameters.getAlerterAt(alerter_idx);
			if (alert_level == 0) {
				alert_level = parameters.correctiveAlertLevel(alerter_idx);
			}
			if (alert_level > 0) {
				Optional<Detection3D> detector = alerter.getDetector(alert_level);
				if (detector.isPresent()) {
					detector.get().horizontalHazardZone(haz,
							(from_ownship ? ownship : intruder),
							(from_ownship ? intruder : ownship),
							(loss ? 0 : alerter.getLevel(alert_level).getAlertingTime()));
				} else {
					return 1;
				}
			} else {
				return 2;
			}
		} else {
			return 3;
		}
		return 0;
	}

	/**
	 * Requires 0 <= conflict_region < CONFICT_BANDS
	 * Put in acs_conflict_bands_ the list of aircraft predicted to be in conflict for the given region.
	 * Put compute_bands_ a flag indicating if bands for given region are computed for some aircraft
	 * Put in tiov_ the time interval of violation for given region
	 */
	private void conflict_aircraft(int conflict_region) {
		double tin  = Double.POSITIVE_INFINITY;
		double tout = Double.NEGATIVE_INFINITY;
		// Iterate on all traffic aircraft
		for (int ac = 0; ac < traffic.size(); ++ac) {
			TrafficState intruder = traffic.get(ac);
			int alerter_idx = alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= parameters.numberOfAlerters()) {
				Alerter alerter = parameters.getAlerterAt(alerter_idx);
				// Assumes that thresholds of severe alerts are included in the volume of less severe alerts
				BandsRegion region = BandsRegion.regionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
				int alert_level = alerter.alertLevelForRegion(region);
				if (alert_level > 0) {
					Detection3D detector = alerter.getLevel(alert_level).getCoreDetection();
					HysteresisData alerting_hysteresis = alerting_hysteresis_acs_.get(intruder.getId());
					double alerting_time = alerter.getLevel(alert_level).getAlertingTime();
					if (alerting_hysteresis != null && 
							!Double.isNaN(alerting_hysteresis.getInitTime()) &&
							alerting_hysteresis.getInitTime() < current_time &&
							alerting_hysteresis.getLastValue() == alert_level) {
						alerting_time = alerter.getLevel(alert_level).getEarlyAlertingTime();
					}
					ConflictData det = detector.conflictDetectionWithTrafficState(ownship,intruder,0,parameters.getLookaheadTime());
					if (det.conflict()) {
						if (det.conflictBefore(alerting_time)) {
							acs_conflict_bands_.get(conflict_region).add(new IndexLevelT(ac,alert_level,parameters.getLookaheadTime()));
						} 
						tin = Util.min(tin,det.getTimeIn());
						tout = Util.max(tout,det.getTimeOut());
					} 
				}
			}
		} 
		tiov_[conflict_region]=new Interval(tin,tout);
	}

	/**
	 * Requires {@code 0 <= conflict_region < CONFICT_BANDS}
	 * @return sorted list of aircraft indices and alert_levels contributing to conflict (non-peripheral)
	 * bands for given conflict region.
	 * INTERNAL USE ONLY
	 */
	protected List<IndexLevelT> acs_conflict_bands(int conflict_region) {
		refresh();
		return acs_conflict_bands_.get(conflict_region);
	}

	/**
	 * Requires {@code 0 <= conflict_region < CONFICT_BANDS}
	 * @return Return time interval of conflict for given conflict region
	 * INTERNAL USE ONLY
	 */
	protected Interval tiov(int conflict_region) {
		refresh();
		return tiov_[conflict_region];
	}

	private int dta_hysteresis_current_value(TrafficState ac) {
		if (parameters.getDTALogic() != 0 && parameters.getDTAAlerter() != 0 &&
				parameters.getDTARadius() > 0 && parameters.getDTAHeight() > 0) {
			HysteresisData dta_hysteresis = dta_hysteresis_acs_.get(ac.getId());
			if (dta_hysteresis == null) {
				dta_hysteresis = new HysteresisData(
						parameters.getHysteresisTime(),
						parameters.getPersistenceTime(),
						parameters.getAlertingParameterM(),
						parameters.getAlertingParameterN());
				int raw_dta = Util.almost_leq(ac.getPosition().distanceH(parameters.getDTAPosition()),parameters.getDTARadius()) &&
						Util.almost_leq(ac.getPosition().alt(),parameters.getDTAHeight()) ? 1 : 0;	
				int actual_dta = dta_hysteresis.applyHysteresisLogic(raw_dta,current_time);
				dta_hysteresis_acs_.put(ac.getId(),dta_hysteresis);
				return actual_dta;
			} else if (dta_hysteresis.isUpdatedAtCurrentTime(current_time)) {
				return dta_hysteresis.getLastValue();
			} else {
				int raw_dta = Util.almost_leq(ac.getPosition().distanceH(parameters.getDTAPosition()),parameters.getDTARadius()) &&
						Util.almost_leq(ac.getPosition().alt(),parameters.getDTAHeight()) ? 1 : 0;	
				return dta_hysteresis.applyHysteresisLogic(raw_dta,current_time);
			}
		} else {
			return 0;
		}
	}

	/**
	 * Return alert index used for intruder aircraft. 
	 * The alert index depends on alerting logic and DTA logic. 
	 * If ownship centric, it returns the alert index of ownship. 
	 * Otherwise, returns the alert index of the intruder. 
	 * If the DTA logic is enabled, the alerter of an aircraft is determined by
	 * its dta status.
	 */
	public int alerter_index_of(TrafficState intruder) {
		if (parameters.isAlertingLogicOwnshipCentric()) {
			if (dta_hysteresis_current_value(ownship) == 1) {
				return parameters.getDTAAlerter();
			} else {
				return ownship.getAlerterIndex();
			}
		} else {
			if (dta_hysteresis_current_value(intruder) == 1) {
				return parameters.getDTAAlerter();
			} else {
				return intruder.getAlerterIndex();
			}
		}
	}

	public static int epsilonH(TrafficState ownship, TrafficState ac) {
		if (ownship.isValid() && ac.isValid()) {
			Vect2 s = ownship.get_s().Sub(ac.get_s()).vect2();
			Vect2 v = ownship.get_v().Sub(ac.get_v()).vect2();   
			return CriteriaCore.horizontalCoordination(s,v);
		} else {
			return 0;
		}
	}

	public static int epsilonV(TrafficState ownship, TrafficState ac) {
		if (ownship.isValid() && ac.isValid()) {
			Vect3 s = ownship.get_s().Sub(ac.get_s());
			return CriteriaCore.verticalCoordinationLoS(s,ownship.get_v(),ac.get_v(),
					ownship.getId(), ac.getId());
		} else {
			return 0;
		}
	}

	public TrafficState criteria_ac() {
		return parameters.isEnabledConflictCriteria() ? mostUrgentAircraft() : TrafficState.INVALID;
	}

	public TrafficState recovery_ac() {
		return parameters.isEnabledRecoveryCriteria() ? mostUrgentAircraft() : TrafficState.INVALID;
	}

	/** 
	 * Return true if and only if threshold values, defining an alerting level, are violated.
	 */ 
	private boolean check_alerting_thresholds(Alerter alerter, int alert_level, TrafficState intruder, int turning, int accelerating, int climbing) {
		AlertThresholds athr = alerter.getLevel(alert_level);
		if (athr.isValid()) {
			Detection3D detector = athr.getCoreDetection();	
			HysteresisData alerting_hysteresis = alerting_hysteresis_acs_.get(intruder.getId());
			double alerting_time = athr.getAlertingTime();
			if (alerting_hysteresis != null && 
					!Double.isNaN(alerting_hysteresis.getLastTime()) &&
					alerting_hysteresis.getLastTime() < current_time &&
					alerting_hysteresis.getLastValue() == alert_level) {
				alerting_time = athr.getEarlyAlertingTime();
			}
			int epsh = epsilonH(false,intruder);
			int epsv = epsilonV(false,intruder);
			ConflictData det = detector.conflictDetectionWithTrafficState(ownship,intruder,0,parameters.getLookaheadTime());
			if (det.conflictBefore(alerting_time)) {
				return true;
			}
			if (athr.getHorizontalDirectionSpread() > 0 || athr.getHorizontalSpeedSpread() > 0 || 
					athr.getVerticalSpeedSpread() > 0 || athr.getAltitudeSpread() > 0) {
				if (athr.getHorizontalDirectionSpread() > 0) {
					DaidalusDirBands dir_band = new DaidalusDirBands();
					dir_band.set_min_max_rel(turning <= 0 ? athr.getHorizontalDirectionSpread() : 0,
							turning >= 0 ? athr.getHorizontalDirectionSpread() : 0);
					if (dir_band.kinematic_conflict(parameters,ownship,intruder,detector,epsh,epsv,alerting_time,DTAStatus())) {
						return true;
					}
				}
				if (athr.getHorizontalSpeedSpread() > 0) {
					DaidalusHsBands hs_band = new DaidalusHsBands();
					hs_band.set_min_max_rel(accelerating <= 0 ? athr.getHorizontalSpeedSpread() : 0,
							accelerating >= 0 ? athr.getHorizontalSpeedSpread() : 0);
					if (hs_band.kinematic_conflict(parameters,ownship,intruder,detector,epsh,epsv,alerting_time,DTAStatus())) {
						return true;
					}
				}
				if (athr.getVerticalSpeedSpread() > 0) {
					DaidalusVsBands vs_band = new DaidalusVsBands();
					vs_band.set_min_max_rel(climbing <= 0 ? athr.getVerticalSpeedSpread() : 0,
							climbing >= 0 ? athr.getVerticalSpeedSpread() : 0);
					if (vs_band.kinematic_conflict(parameters,ownship,intruder,detector,epsh,epsv,alerting_time,DTAStatus())) {
						return true;
					}
				}
				if (athr.getAltitudeSpread() > 0) {
					DaidalusAltBands alt_band = new DaidalusAltBands();
					alt_band.set_min_max_rel(climbing <= 0 ? athr.getAltitudeSpread() : 0,
							climbing >= 0 ? athr.getAltitudeSpread() : 0);
					if (alt_band.kinematic_conflict(parameters,ownship,intruder,detector,epsh,epsv,alerting_time,DTAStatus())) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private int alerting_hysteresis_current_value(TrafficState intruder, int turning, int accelerating, int climbing) {
		int alerter_idx = alerter_index_of(intruder);
		if (1 <= alerter_idx && alerter_idx <= parameters.numberOfAlerters()) {
			HysteresisData alerting_hysteresis = alerting_hysteresis_acs_.get(intruder.getId());
			if (alerting_hysteresis == null) {
				alerting_hysteresis = new HysteresisData(
						parameters.getHysteresisTime(),
						parameters.getPersistenceTime(),
						parameters.getAlertingParameterM(),
						parameters.getAlertingParameterN());
				Alerter alerter = parameters.getAlerterAt(alerter_idx);
				int raw_alert = raw_alert_level(alerter,intruder,turning,accelerating,climbing);
				int actual_alert = alerting_hysteresis.applyHysteresisLogic(raw_alert,current_time);
				alerting_hysteresis_acs_.put(intruder.getId(),alerting_hysteresis);
				return actual_alert;
			} else if (alerting_hysteresis.isUpdatedAtCurrentTime(current_time)) {
				return alerting_hysteresis.getLastValue();
			} else {
				Alerter alerter = parameters.getAlerterAt(alerter_idx);
				int raw_alert = raw_alert_level(alerter,intruder,turning,accelerating,climbing);
				return alerting_hysteresis.applyHysteresisLogic(raw_alert,current_time);
			}
		} else {
			return -1;
		}
	}

	/** 
	 * Computes alerting type of ownship and an the idx-th aircraft in the traffic list 
	 * The number 0 means no alert. A negative number means
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
	 * NOTES: 
	 * 1. This method uses a 0-based traffic index.
	 * 2. This methods applies MofN alerting strategy
	 */
	public int alert_level(int idx, int turning, int accelerating, int climbing) {
		if (0 <= idx && idx < traffic.size()) {
			return alerting_hysteresis_current_value(traffic.get(idx),turning,accelerating,climbing);
		} else {
			return -1;
		}
	}

	private int raw_alert_level(Alerter alerter, TrafficState intruder, int turning, int accelerating, int climbing) {
		for (int alert_level=alerter.mostSevereAlertLevel(); alert_level > 0; --alert_level) {
			if (check_alerting_thresholds(alerter,alert_level,intruder,turning,accelerating,climbing)) {
				return alert_level;
			}
		}	
		return 0;
	}

	public String outputStringAircraftStates(boolean internal) {
		String ualt = internal ? "m" : parameters.getUnitsOf("step_alt");
		String uhs = internal ? "m/s" : parameters.getUnitsOf("step_hs");
		String uvs = internal ? "m/s" : parameters.getUnitsOf("step_vs");
		String uxy = "m";
		String utrk = "deg";
		if (!internal) {
			if (Units.isCompatible(uhs,"knot")) {
				uxy = "nmi";
			} else if (Units.isCompatible(uhs,"fpm")) {
				uxy = "ft";
			} else if (Units.isCompatible(uhs,"kph")) {
				uxy = "km";
			}
		} else {
			utrk="rad";
		}
		return ownship.formattedTraffic(traffic,utrk,uxy,ualt,uhs,uvs,current_time);
	}

	public String rawString() {
		String s="## Daidalus Core\n"; 
		s+="current_time = "+f.FmPrecision(current_time)+"\n";
		s+="## Daidalus Parameters\n";
		s+=parameters.toString();
		s+="## Cached variables\n";
		s+="cache_ = "+f.Fmi(cache_)+"\n";		
		s+="most_urgent_ac_ = "+most_urgent_ac_.getId()+"\n";
		s+="epsh_ = "+f.Fmi(epsh_)+"\n";
		s+="epsv_ = "+f.Fmi(epsv_)+"\n";
		s+="dta_status_ = "+dta_status_+"\n";
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_conflict_bands_["+conflict_region+"] = "+
					IndexLevelT.toString(acs_conflict_bands_.get(conflict_region))+"\n";
		}
		boolean comma=false;
		s+="tiov_ = {";
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += tiov_[conflict_region].toString();
		}
		s += "}\n";
		comma=false;
		s+="bands4region_ = {";
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += bands4region_[conflict_region];
		}
		s += "}\n";
		for (Map.Entry<String,HysteresisData> entry : alerting_hysteresis_acs_.entrySet()) {
			s+="alerting_hysteresis_acs_["+entry.getKey()+"] = "+
					entry.getValue().toString();
		}
		if (!alerting_hysteresis_acs_.isEmpty()) {
			s+="\n";
		}
		for (Map.Entry<String,HysteresisData> entry : dta_hysteresis_acs_.entrySet()) {
			s+="dta_hysteresis_acs_["+entry.getKey()+"] = "+
					entry.getValue().toString();
		}
		if (!dta_hysteresis_acs_.isEmpty()) {
			s+="\n";
		}

		s+="wind_vector = "+wind_vector.toString()+"\n";
		s+="## Ownship and Traffic Relative to Wind\n";
		s+=outputStringAircraftStates(true);
		s+="##\n";
		return s;
	}

	public String toString() {
		String s="##\n";
		s+="current_time = "+f.FmPrecision(current_time)+"\n";
		s+="wind_vector = "+wind_vector.toString()+"\n";
		s+="##\n";
		return s;
	}

}
