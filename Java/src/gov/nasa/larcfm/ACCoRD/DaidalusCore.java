/*
 * Copyright (c) 2015-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
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
	public UrgencyStrategy urgency_strategy; // Strategy for most urgent aircraft

	/**** CACHED VARIABLES__ ****/

	/* Variable to control re-computation of cached values */
	private int cache__; // -1: outdated, 1:updated, 0: updated only most_urgent_ac and eps
	/* Most urgent aircraft */
	public TrafficState most_urgent_ac__; 
	/* Cached horizontal epsilon for implicit coordination */
	private int epsh__; 
	/* Cached vertical epsilon for implicit coordination */
	private int epsv__; 
	/* Cached lists of aircraft indices, alert_levels, and lookahead times sorted by indices, contributing to conflict (non-peripheral) 
	 * band listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private List<List<IndexLevelT>> acs_conflict_bands__; 
	/* Cached list of time to violation per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private Interval[] tiov__; 
	/* Cached list of boolean alues indicating which bands should be computed, where 0th:NEAR, 1th:MID, 2th:FAR.
	 * NaN means that bands are not computed for that region*/
	private boolean[] bands4region__;

	/**** HYSTERESIS _VARIABLES_ ****/
	//Alerting MofNs and times per aircraft's ids
	private Map<String,AlertingMofN> _alerting_mofns_; 

	private void init() {

		// Public variables_ are initialized
		ownship = TrafficState.INVALID;
		traffic = new ArrayList<TrafficState>(); 
		current_time = 0;
		wind_vector = Velocity.ZERO;
		parameters = new DaidalusParameters();
		urgency_strategy = NoneUrgencyStrategy.NONE_URGENCY_STRATEGY;

		// Cached arrays__ are initialized
		acs_conflict_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_conflict_bands__.add(new ArrayList<IndexLevelT>());
		}
		tiov__ = new Interval[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		bands4region__ = new boolean[BandsRegion.NUMBER_OF_CONFLICT_BANDS];

		// Hysteresis variables are initialized
		_alerting_mofns_ = new HashMap<String,AlertingMofN>();

		// Cached__ variables are cleared
		cache__ = -1; 
		stale(true,true);
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

		// Cached arrays__ are initialized
		acs_conflict_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_conflict_bands__.add(new ArrayList<IndexLevelT>());
		}
		tiov__ = new Interval[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		bands4region__ = new boolean[BandsRegion.NUMBER_OF_CONFLICT_BANDS];

		// Hysteresis variables are initialized
		_alerting_mofns_ = new HashMap<String,AlertingMofN>();

		// Public variables_ are copied
		ownship = core.ownship;
		traffic.addAll(core.traffic);
		current_time = core.current_time;
		wind_vector = core.wind_vector;
		parameters = new DaidalusParameters(core.parameters);
		urgency_strategy = core.urgency_strategy.copy();

		// Cached__ variables are cleared
		cache__ = -1; 
		stale(true,true);
	}

	/**
	 *  Clear ownship and traffic data from this object.   
	 */
	public void clear() {
		ownship = TrafficState.INVALID;
		traffic.clear();
		current_time = 0;
		wind_vector = Velocity.ZERO;
		stale(false);
	}

	/**
	 * Set cached values to stale conditions as they are no longer fresh.
	 * If hysteresis is true, it also clears hysteresis variables
	 */
	private void stale(boolean hysteresis,boolean forced) {
		if (forced || cache__ >= 0) {
			cache__ = -1;
			most_urgent_ac__ = TrafficState.INVALID;
			epsh__ = 0;
			epsv__ = 0;
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				acs_conflict_bands__.get(conflict_region).clear();
				tiov__[conflict_region] = Interval.EMPTY;
				bands4region__[conflict_region] = false;
			}
		}
		if (hysteresis) {
			_alerting_mofns_.clear();
		}
	}
	
	/**
	 * Set cached values to stale conditions as they are no longer fresh.
	 * If hysteresis is true, it also clears hysteresis variables
	 */
	public void stale(boolean hysteresis) {
		stale(hysteresis,false);
	}

	/**
	 * Returns true is object is fresh
	 */
	public boolean isFresh() {
		return cache__ > 0;
	}

	/**
	 *  Refresh cached values 
	 */
	public void refresh() {
		if (cache__ <= 0) {
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				conflict_aircraft(conflict_region);
				BandsRegion region = BandsRegion.conflictRegionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
				for (int alerter_idx=1;  alerter_idx <= parameters.numberOfAlerters(); ++alerter_idx) {
					Alerter alerter = parameters.getAlerterAt(parameters.isAlertingLogicOwnshipCentric() ? 
							ownship.getAlerterIndex() :alerter_idx);
					int alert_level = alerter.alertLevelForRegion(region);
					if (alert_level > 0) {
						bands4region__[conflict_region] = true;
					}
				}
			}
			refresh_mua_eps();
			cache__ = 1;
		} 
	}

	private void refresh_mua_eps() {
		if (cache__ < 0) {
			int muac = -1;
			if (!traffic.isEmpty()) {
				muac = urgency_strategy.mostUrgentAircraft(ownship,traffic,parameters.getLookaheadTime());
			} 
			if (muac >= 0) {
				most_urgent_ac__ = traffic.get(muac);
			} else {
				most_urgent_ac__ = TrafficState.INVALID;
			}
			epsh__ = epsilonH(ownship,most_urgent_ac__);
			epsv__ = epsilonV(ownship,most_urgent_ac__);
			cache__ = 0;
		}
	}

	/**
	 * @return most urgent aircraft for implicit coordination 
	 */
	public TrafficState mostUrgentAircraft() {
		refresh_mua_eps();
		return most_urgent_ac__;
	}

	/**
	 * Returns horizontal epsilon for implicit coordination with respect to criteria ac.
	 * 
	 */
	public int epsilonH() {
		refresh_mua_eps();
		return epsh__;
	}

	/**
	 * Returns vertical epsilon for implicit coordination with respect to criteria ac.
	 */
	public int epsilonV() {
		refresh_mua_eps();
		return epsv__;
	}

	/**
	 * Returns horizontal epsilon for implicit coordination with respect to criteria ac.
	 * 
	 */
	public int epsilonH(boolean recovery_case, TrafficState traffic) {
		refresh_mua_eps();
		if ((recovery_case? parameters.isEnabledRecoveryCriteria() : parameters.isEnabledConflictCriteria()) &&
				traffic.sameId(most_urgent_ac__)) {
			return epsh__;
		}
		return 0;
	}

	/**
	 * Returns vertical epsilon for implicit coordination with respect to criteria ac.
	 */
	public int epsilonV(boolean recovery_case, TrafficState traffic) {
		refresh_mua_eps();
		if ((recovery_case? parameters.isEnabledRecoveryCriteria() : parameters.isEnabledConflictCriteria()) &&
				traffic.sameId(most_urgent_ac__)) {
			return epsv__;
		}
		return 0;
	}

	/** 
	 * Return true if bands are computed for this particular region (0:NEAR, 1:MID, 2: FAR)
	 */
	public boolean bands_for(int region) {
		refresh();
		return bands4region__[region];
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
		ownship = TrafficState.makeOwnship(id,pos,vel);
		ownship.applyWindVector(wind_vector);
		current_time = time;
	}

	public int add_traffic_state(String id, Position pos, Velocity vel, double time) {
		double dt = current_time-time;
		Position pt = dt == 0 ? pos : pos.linear(vel,dt);    
		TrafficState ac = ownship.makeIntruder(id,pt,vel);
		if (ac.isValid()) {
			ac.applyWindVector(wind_vector);
			traffic.add(ac);
			return traffic.size();
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
	}

	public void set_wind_velocity(Velocity wind) {
		if (has_ownship()) {
			ownship.applyWindVector(wind);
			for (TrafficState ac : traffic) {
				ac.applyWindVector(wind);
			}
		}
		wind_vector = wind;
	}

	public boolean has_ownship() {
		return ownship.isValid();
	}

	public boolean has_traffic() {
		return traffic.size() > 0;
	}

	private static void add_blob(List<List<Position>> blobs, Deque<Position> vin, Deque<Position> vout) {
		if (vin.isEmpty() && vout.isEmpty()) {
			return;
		}
		// Add conflict contour
		List<Position> blob = new ArrayList<Position>(vin);
		blob.addAll(vout);
		blobs.add(blob);
		vin.clear();
		vout.clear();
	}

	/* idx is a 0-based index in the list of traffic aircraft
	 * returns 1 if detector of traffic aircraft
	 * returns 2 if corrective alerter level is not set
	 * returns 3 if alerter of traffic aircraft is out of bands
	 * otherwise, if there are no errors, returns 0 and the answer is in blobs
	 */
	public int horizontal_contours(List<List<Position>>blobs, int idx, int alert_level) {
		TrafficState intruder = traffic.get(idx);
		int alerter_idx = alerter_index_of(idx);
		if (1 <= alerter_idx && alerter_idx <= parameters.numberOfAlerters()) {
			Alerter alerter = parameters.getAlerterAt(alerter_idx);
			if (alert_level == 0) {
				alert_level = parameters.correctiveAlertLevel(alerter_idx);
			}
			if (alert_level > 0) {
				Optional<Detection3D> detector = alerter.getDetector(alert_level);
				if (detector.isPresent()) {
					blobs.clear();
					Deque<Position> vin = new ArrayDeque<Position>();
					Position po = ownship.getPosition();
					Velocity vo = ownship.getAirVelocity();
					Vect3 si = intruder.get_s();
					Velocity vi = intruder.get_v();
					double current_trk = vo.trk();
					Deque<Position> vout = new ArrayDeque<Position>();
					/* First step: Computes conflict contour (contour in the current path of the aircraft).
					 * Get contour portion to the right.  If los.getTimeIn() == 0, a 360 degree
					 * contour will be computed. Otherwise, stops at the first non-conflict degree.
					 */
					double right = 0; // Contour conflict limit to the right relative to current direction  [0-2pi rad]
					double two_pi = 2*Math.PI;
					double s_err = intruder.relativeHorizontalPositionError(ownship,parameters);
					double sz_err = intruder.relativeVerticalPositionError(ownship,parameters);
					double v_err = intruder.relativeHorizontalSpeedError(ownship,s_err,parameters);
					double vz_err = intruder.relativeVerticalSpeedError(ownship,parameters);
					for (; right < two_pi; right += parameters.getHorizontalDirectionStep()) {
						Velocity vop = vo.mkTrk(current_trk+right);
						LossData los = detector.get().conflictDetectionSUM(ownship.get_s(),ownship.vel_to_v(po,vop),si,vi,
								0,parameters.getLookaheadTime(),
								s_err,sz_err,v_err,vz_err);
						if ( !los.conflict() ) {
							break;
						}
						if (los.getTimeIn() != 0 ) {
							// if not in los, add position at time in (counter clock-wise)
							vin.addLast(po.linear(vop,los.getTimeIn()));
						}
						// in any case, add position ad time out (counter clock-wise)
						vout.addFirst(po.linear(vop,los.getTimeOut()));
					}
					/* Second step: Compute conflict contour to the left */
					double left = 0;  // Contour conflict limit to the left relative to current direction [0-2pi rad]
					if (0 < right && right < two_pi) {
						/* There is a conflict contour, but not a violation */
						for (left = parameters.getHorizontalDirectionStep(); left < two_pi; left += parameters.getHorizontalDirectionStep()) {
							Velocity vop = vo.mkTrk(current_trk-left);
							LossData los = detector.get().conflictDetectionSUM(ownship.get_s(),ownship.vel_to_v(po,vop),si,vi,
									0,parameters.getLookaheadTime(),
									s_err,sz_err,v_err,vz_err);
							if ( !los.conflict() ) {
								break;
							}
							vin.addFirst(po.linear(vop,los.getTimeIn()));
							vout.addLast(po.linear(vop,los.getTimeOut()));
						}
					}
					add_blob(blobs,vin,vout);
					// Third Step: Look for other blobs to the right within direction threshold
					if (right < parameters.getHorizontalContourThreshold()) {
						for (; right < two_pi-left; right += parameters.getHorizontalDirectionStep()) {
							Velocity vop = vo.mkTrk(current_trk+right);
							LossData los = detector.get().conflictDetectionSUM(ownship.get_s(),ownship.vel_to_v(po,vop),si,vi,
									0,parameters.getLookaheadTime(),
									s_err,sz_err,v_err,vz_err);
							if (los.conflict()) {
								vin.addLast(po.linear(vop,los.getTimeIn()));
								vout.addFirst(po.linear(vop,los.getTimeOut()));
							} else {
								add_blob(blobs,vin,vout);
								if (right >= parameters.getHorizontalContourThreshold()) {
									break;
								}
							}
						}
						add_blob(blobs,vin,vout);
					}
					// Fourth Step: Look for other blobs to the left within direction threshold
					if (left < parameters.getHorizontalContourThreshold()) {
						for (; left < two_pi-right; left += parameters.getHorizontalDirectionStep()) {
							Velocity vop = vo.mkTrk(current_trk-left);
							LossData los = detector.get().conflictDetectionSUM(ownship.get_s(),ownship.vel_to_v(po,vop),si,vi,
									0,parameters.getLookaheadTime(),
									s_err,sz_err,v_err,vz_err);
							if (los.conflict()) {
								vin.addFirst(po.linear(vop,los.getTimeIn()));
								vout.addLast(po.linear(vop,los.getTimeOut()));
							} else {
								add_blob(blobs,vin,vout);
								if (left >= parameters.getHorizontalContourThreshold()) {
									break;
								}
							}
						}
						add_blob(blobs,vin,vout);
					} 
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
			Alerter alerter = alerter_of(ac);
			// Assumes that thresholds of severe alerts are included in the volume of less severe alerts
			BandsRegion region = BandsRegion.conflictRegionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
			int alert_level = alerter.alertLevelForRegion(region);
			if (alert_level > 0) {
				Detection3D detector = 	alerter.getLevel(alert_level).getCoreDetection();
				double alerting_time = Util.min(parameters.getLookaheadTime(),
						alerter.getLevel(alert_level).getAlertingTime());
				double early_alerting_time = Util.min(parameters.getLookaheadTime(),
						alerter.getLevel(alert_level).getEarlyAlertingTime());
				double s_err = intruder.relativeHorizontalPositionError(ownship,parameters);
				double sz_err = intruder.relativeVerticalPositionError(ownship,parameters);
				double v_err = intruder.relativeHorizontalSpeedError(ownship,s_err,parameters);
				double vz_err = intruder.relativeVerticalSpeedError(ownship,parameters);
				ConflictData det = detector.conflictDetectionSUM(ownship.get_s(),ownship.get_v(),
						intruder.get_s(),intruder.get_v(),0,parameters.getLookaheadTime(),
						s_err,sz_err,v_err,vz_err);
				boolean lowc = detector.violationSUMAt(ownship.get_s(),ownship.get_v(),intruder.get_s(),intruder.get_v(),
						s_err,sz_err,v_err,vz_err,0.0);
				if (lowc || det.conflict()) {
					if (lowc || det.getTimeIn() < alerting_time) {
						acs_conflict_bands__.get(conflict_region).add(new IndexLevelT(ac,alert_level,early_alerting_time,true));
					} 
					tin = Util.min(tin,det.getTimeIn());
					tout = Util.max(tout,det.getTimeOut());
				} 
			}
		}
		tiov__[conflict_region]=new Interval(tin,tout);
	}

	/**
	 * Requires 0 <= conflict_region < CONFICT_BANDS
	 * @return sorted list of aircraft indices and alert_levels contributing to conflict (non-peripheral)
	 * bands for given conflict region.
	 * INTERNAL USE ONLY
	 */
	protected List<IndexLevelT> acs_conflict_bands(int conflict_region) {
		refresh();
		return acs_conflict_bands__.get(conflict_region);
	}

	/**
	 * Requires 0 <= conflict_region < CONFICT_BANDS
	 * @return Return time interval of conflict for given conflict region
	 * INTERNAL USE ONLY
	 */
	protected Interval tiov(int conflict_region) {
		refresh();
		return tiov__[conflict_region];
	}

	/**
	 * Return alert index used for the traffic aircraft at 0 <= idx < traffic.size(). 
	 * The alert index depends on alerting logic. If ownship centric, it returns the
	 * alert index of ownship. Otherwise, returns the alert index of the traffic aircraft 
	 * at idx. Return 0 if idx is out of range
	 */
	public int alerter_index_of(int idx) {
		if (0 <= idx && idx < traffic.size()) {
			if (parameters.isAlertingLogicOwnshipCentric()) {
				return ownship.getAlerterIndex();
			} else {
				return traffic.get(idx).getAlerterIndex();
			}
		}
		return 0;
	}

	/**
	 * Return alerter used for the traffic aircraft at 0 <= idx < traffic.size(). 
	 * The alert index depends on alerting logic. If ownship centric, it returns the
	 * alerter of the ownship. Otherwise, returns the alerter of the traffic aircraft 
	 * at idx. 
	 */
	public Alerter alerter_of(int idx) {
		int alerter_idx = alerter_index_of(idx);
		return parameters.getAlerterAt(alerter_idx);
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
	private boolean check_alerting_thresholds(AlertThresholds athr, TrafficState intruder, int turning, int accelerating, int climbing) {
		if (athr.isValid()) {
			Vect3 so = ownship.get_s();
			Velocity vo = ownship.get_v();
			Vect3 si = intruder.get_s();
			Velocity vi = intruder.get_v();
			Detection3D detector = athr.getCoreDetection();	
			double alerting_time = Util.min(parameters.getLookaheadTime(),athr.getAlertingTime());
			int epsh = epsilonH(false,intruder);
			int epsv = epsilonV(false,intruder);
			double s_err = intruder.relativeHorizontalPositionError(ownship,parameters);
			double sz_err = intruder.relativeVerticalPositionError(ownship,parameters);
			double v_err = intruder.relativeHorizontalSpeedError(ownship,s_err,parameters);
			double vz_err = intruder.relativeVerticalSpeedError(ownship,parameters);
			if (detector.violationSUMAt(so,vo,si,vi,s_err,sz_err,v_err,vz_err,0.0)) {
				return true;
			}
			ConflictData det = detector.conflictDetectionSUM(so,vo,si,vi,0,alerting_time,
					s_err,sz_err,v_err,vz_err);
			if (det.conflict()) {
				return true;
			}
			if (athr.getHorizontalDirectionSpread() > 0 || athr.getHorizontalSpeedSpread() > 0 || 
					athr.getVerticalSpeedSpread() > 0 || athr.getAltitudeSpread() > 0) {
				if (athr.getHorizontalDirectionSpread() > 0) {
					DaidalusDirBands dir_band = new DaidalusDirBands(parameters);
					dir_band.set_min_rel(turning <= 0 ? athr.getHorizontalDirectionSpread() : 0);
					dir_band.set_max_rel(turning >= 0 ? athr.getHorizontalDirectionSpread() : 0);
					dir_band.set_step(parameters.getHorizontalDirectionStep());  
					dir_band.set_turn_rate(parameters.getTurnRate()); 
					dir_band.set_bank_angle(parameters.getBankAngle()); 
					dir_band.set_recovery(parameters.isEnabledRecoveryHorizontalDirectionBands());
					if (dir_band.kinematic_conflict(ownship,intruder,parameters,detector,epsh,epsv,alerting_time)) {
						return true;
					}
				}
				if (athr.getHorizontalSpeedSpread() > 0) {
					DaidalusHsBands hs_band = new DaidalusHsBands(parameters);
					hs_band.set_min_rel(accelerating <= 0 ? athr.getHorizontalSpeedSpread() : 0);
					hs_band.set_max_rel(accelerating >= 0 ? athr.getHorizontalSpeedSpread() : 0);
					hs_band.set_step(parameters.getHorizontalSpeedStep());
					hs_band.set_horizontal_accel(parameters.getHorizontalAcceleration()); 
					hs_band.set_recovery(parameters.isEnabledRecoveryHorizontalSpeedBands());
					if (hs_band.kinematic_conflict(ownship,intruder,parameters,detector,epsh,epsv,alerting_time)) {
						return true;
					}
				}
				if (athr.getVerticalSpeedSpread() > 0) {
					DaidalusVsBands vs_band = new DaidalusVsBands(parameters);
					vs_band.set_min_rel(climbing <= 0 ? athr.getVerticalSpeedSpread() : 0);
					vs_band.set_max_rel(climbing >= 0 ? athr.getVerticalSpeedSpread() : 0);
					vs_band.set_step(parameters.getVerticalSpeedStep()); 
					vs_band.set_vertical_accel(parameters.getVerticalAcceleration());
					vs_band.set_recovery(parameters.isEnabledRecoveryVerticalSpeedBands());   
					if (vs_band.kinematic_conflict(ownship,intruder,parameters,detector,epsh,epsv,alerting_time)) {
						return true;
					}
				}
				if (athr.getAltitudeSpread() > 0) {
					DaidalusAltBands alt_band = new DaidalusAltBands(parameters);
					alt_band.set_min_rel(climbing <= 0 ? athr.getAltitudeSpread() : 0);
					alt_band.set_max_rel(climbing >= 0 ? athr.getAltitudeSpread() : 0);
					alt_band.set_step(parameters.getAltitudeStep()); 
					alt_band.set_vertical_rate(parameters.getVerticalRate()); 
					alt_band.set_vertical_accel(parameters.getVerticalAcceleration());
					alt_band.set_recovery(parameters.isEnabledRecoveryAltitudeBands());  
					if (alt_band.kinematic_conflict(ownship,intruder,parameters,detector,epsh,epsv,alerting_time)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	/** 
	 * Computes alerting type of ownship and an the idx-th aircraft in the traffic list 
	 * The number 0 means no alert. A negative number means
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
	 * NOTES: 
	 * 1. This method uses a 0-based traffic index.
	 * 2. This methods applies MofN alerting strategy
	 */
	public int alert_level(int idx, int turning, int accelerating, int climbing) {
		if (0 <= idx && idx < traffic.size()) {
			TrafficState intruder = traffic.get(idx);
			int alerter_idx = alerter_index_of(idx);
			if (alerter_idx > 0) {
				String id = intruder.getId();
				AlertingMofN mofn = _alerting_mofns_.get(id);
				Alerter alerter = parameters.getAlerterAt(alerter_idx);
				if (mofn == null) {
					mofn = new AlertingMofN(parameters.getAlertingParameterM(),
							parameters.getAlertingParameterN(),
							parameters.getHysteresisTime(),
							parameters.getPersistenceTime());
					int alert = mofn.m_of_n(alert_level(alerter,intruder,turning,accelerating,climbing),current_time);
					_alerting_mofns_.put(id,mofn);
					return alert;
				} else {
					return mofn.m_of_n(alert_level(alerter,intruder,turning,accelerating,climbing),current_time);
				}
			} 
		} 
		return -1;
	}

	private int alert_level(Alerter alerter, TrafficState intruder, int turning, int accelerating, int climbing) {
		for (int alert_level=alerter.mostSevereAlertLevel(); alert_level > 0; --alert_level) {
			AlertThresholds athr = alerter.getLevel(alert_level);
			if (check_alerting_thresholds(athr,intruder,turning,accelerating,climbing)) {
				return alert_level;
			}
		}	
		return 0;
	}

	public void setParameterData(ParameterData p) {
		if (parameters.setParameterData(p)) {
			stale(true);
		}
	}

	public String rawString() {
		String s="## Daidalus Core\n"; 
		s+="current_time = "+f.FmPrecision(current_time)+"\n";
		s+="## DaidalusBandsCore Parameters\n";
		s+=parameters.toString();
		s+="## Cached variables__\n";
		s+="cache__ = "+f.Fmi(cache__)+"\n";		
		s+="most_urgent_ac__ = "+most_urgent_ac__.getId()+"\n";
		s+="epsh__ = "+f.Fmi(epsh__)+"\n";
		s+="epsv__ = "+f.Fmi(epsv__)+"\n";		
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_conflict_bands__["+conflict_region+"] = "+
					IndexLevelT.toString(acs_conflict_bands__.get(conflict_region))+"\n";
		}
		boolean comma=false;
		s+="tiov__ = {";
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += tiov__[conflict_region].toString();
		}
		s += "}\n";
		comma=false;
		s+="bands4region__ = {";
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			if (comma) {
				s += ", ";
			} else {
				comma = true;
			}
			s += bands4region__[conflict_region];
		}
		s += "}\n";
		s+="wind_vector = "+wind_vector.toString()+"\n";
		s+="## Ownship and Traffic Relative to Wind\n";
		ownship.formattedTraffic(traffic, "m","m","m/s","m/s",current_time);
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
