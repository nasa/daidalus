/*
 * Copyright (c) 2015-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.IntervalSet;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

abstract public class DaidalusRealBands extends DaidalusIntegerBands {

	/*** PRIVATE VARIABLES_ */

	private double mod_;  // If mod_ > 0, bands are circular modulo this value
	private double step_; // Value step
	private double min_param_; // Min value in parameters
	private double max_param_; // Max value in parameters
	// min_rel (max_rel) is the positive distance from current value to minimum (maximum) value. 
	// When mod_ > 0, min_rel, max_rel in [0,mod_/2]	
	private double min_rel_param_; // Relative min value in parameters. A negative value represents val-min
	private double max_rel_param_; // Relative max value in parameters. A negative value represents max-val

	/* Parameters for recovery bands */
	private boolean recovery_; 

	/**** CACHED VARIABLES__ ****/

	private boolean outdated__; // Boolean to control re-computation of cached values
	private int checked__;  // Cached status of input values. Negative unchecked, 0 invalid, 1 valid

	/* Cached lists of aircraft indices, alert_levels, and lookahead times, sorted by indices, contributing to peripheral 
	 * bands listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private List<List<IndexLevelT>> acs_peripheral_bands__; 

	/* Cached lists of aircraft indices, alert_levels, and lookahead times, sorted by indices, contributing to any type
	 * of bands listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR.
	 * These lists are computed as the concatenation of acs_conflict_bands and acs_peripheral_bands. */
	private List<List<IndexLevelT>> acs_bands__; 

	private List<BandsRange> ranges__;     // Cached list of bands ranges

	/* 
	 * recovery_time_ is the time to recovery from violation. 
	 * Negative infinity means no possible recovery.
	 * NaN means no recovery bands are computed (either because there is no conflict or
	 * because they are disabled)
	 */		
	private double recovery_time__; // Cached recovery time
	private int recovery_nfactor__; // Cached number of times the recovery volume was reduced

	/*
	 * recovery_horizontal_distance and recovery_vertical_distance is the 
	 * distance guaranteed by the recovery bands. Negative infinity means no possible recovery.
	 * NaN means no recovery bands are computed (either because there is no conflict of
	 * because they are disabled)
	 */
	private double recovery_horizontal_distance__; // Cached recovery horizontal_separation
	private double recovery_vertical_distance__; // Cached recovery horizontal_separation

	private double min_val__; // Absolute min value (min_val= min when mod == 0 && !rel)
	private double max_val__; // Absolute max value (max_val = max when mod == 0 && !rel)
	private double min_relative__; // Computed relative min value 
	private double max_relative__; // Computed relative max value 
	private boolean circular__; // True if bands is fully circular


	/**** HYSTERESIS _VARIABLES_ ****/

	/* Parameters for hysteresis of preferred direction */
	private double  _last_time_;     // Last data time
	private double  _time_of_dir_;   // Time of current preferred direction
	private boolean _actual_dir_;    // Actual preferred direction before hysteresis
	private boolean _preferred_dir_; // Returned preferred direction after hysteresis

	/*<
	 * resolution_up_,resolution_low_ are the resolution interval computed from all regions that are at least 
	 * as severe as the corrective region. Negative/positive infinity means no possible resolutions
	 * NaN means no resolutions are computed (either because there is no conflict or
	 * because of invalid inputs)
	 */
	private double _resolution_up_;
	private double _resolution_low_;

	/*
	 * Conflict region of the up/low resolutions 
	 */
	private BandsRegion _resolution_region_up_;
	private BandsRegion _resolution_region_low_;

	public DaidalusRealBands(double mod) {	
		// Private variables_ are initialized
		mod_ = Math.abs(mod);
		step_ = 0;
		min_param_ = 0;
		max_param_ = 0;
		min_rel_param_ = 0;
		max_rel_param_ = 0;
		recovery_ = false;

		// Cached arrays__ are initialized
		acs_peripheral_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		acs_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_peripheral_bands__.add(new ArrayList<IndexLevelT>());
			acs_bands__.add(new ArrayList<IndexLevelT>());
		}
		ranges__ = new ArrayList<BandsRange>();

		// Cached__ variables are cleared
		outdated__ = false; // Force stale
		stale(true);
	}

	public DaidalusRealBands() {
		this(0);
	}

	public DaidalusRealBands(DaidalusRealBands b) {

		// Private variables_ are copied
		mod_ = b.mod_;
		step_ = b.step_;
		min_param_ = b.min_param_;
		max_param_ = b.max_param_;
		min_rel_param_ = b.min_rel_param_;
		max_rel_param_ = b.max_rel_param_;
		recovery_ = b.recovery_;

		// Cached arrays__ are initialized
		acs_peripheral_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		acs_bands__ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_peripheral_bands__.add(new ArrayList<IndexLevelT>());
			acs_bands__.add(new ArrayList<IndexLevelT>());
		}
		ranges__ = new ArrayList<BandsRange>();

		// Cached__ variables are cleared
		outdated__ = false; // Force stale
		stale(true);
	}

	abstract public void setDaidalusParameters(DaidalusParameters parameters);

	abstract public double own_val(TrafficState ownship);

	abstract public double time_step(TrafficState ownship);

	abstract public boolean instantaneous_bands();

	abstract public double max_delta_resolution(DaidalusParameters parameters);

	protected double get_min_val__() {
		return min_val__;
	}

	protected double get_max_val__() {
		return max_val__;
	}

	protected double get_mod() {
		return mod_;
	}

	public double get_step() {
		return step_;
	}

	public boolean get_recovery() {
		return recovery_;
	}

	public void set_step(double val) {
		if (val > 0 && val != step_) {
			step_ = val;
			stale(true);
		}
	}

	public void set_recovery(boolean flag) {
		if (flag != recovery_) {
			recovery_ = flag;
			stale(true);
		}
	}

	/**
	 * Set min_rel. When mod_ > 0, requires min_rel to be in [0,mod/2]. Otherwise, a
	 * negative value represents min.
	 */
	public void set_min_rel(double min_rel) {
		if (min_rel_param_ != min_rel) {
			min_rel_param_ = min_rel;
			stale(true);
		}
	}

	/**
	 * Set min_rel. When mod_ > 0, requires max_rel to be in [0,mod/2]. Otherwise, a
	 * negative value represents max.
	 */
	public void set_max_rel(double max_rel) {
		if (max_rel_param_ != max_rel) {
			max_rel_param_ = max_rel;
			stale(true);
		}
	}

	// Set min when mod == 0. 
	public void set_min_nomod(double min) {
		if (mod_ == 0 && min_param_ != min) {
			min_param_ = min;
			stale(true);
		}
	}

	// Set max when mod == 0.
	public void set_max_nomod(double max) {
		if (mod_ == 0 && max_param_ != max) {
			max_param_ = max;
			stale(true);
		}
	}

	// Set min and max when mod_ > 0. Requires min_val and max_val to be in range [0,mod)
	public void set_min_max_mod(double min, double max) {
		if (mod_ > 0 && (min_param_ != min || max_param_ != max)) {
			min_param_ = min;
			max_param_ = max;
			min_rel_param_ = 0;
			max_rel_param_ = 0;
			stale(true);
		}
	}

	/** 
	 * Return val modulo mod_, when mod_ > 0. Otherwise, returns val. 
	 */
	private double mod_val(double val) {
		return mod_ > 0 ? Util.modulo(val,mod_) : val;
	}

	private boolean check_input(TrafficState ownship) {
		if (checked__ < 0) {
			checked__ = 0;
			if (ownship.isValid() && step_ > 0) { 
				double val = own_val(ownship);
				// When mod_ == 0, min_val <= max_val. When mod_ > 0, min_val, max_val in [0,mod_]. 
				// In the later case, min_val may be greater than max_val. Furthermore, min_val = max_val means 
				// a range of values from 0 to mod, i.e., a circular band.
				if (min_rel_param_ == 0 && max_rel_param_ == 0) {
					min_val__ = min_param_;
					max_val__ = max_param_;
				} else {
					if (min_rel_param_ >= 0) {
						min_val__ = mod_val(own_val(ownship)-min_rel_param_);
					} else {
						min_val__ = min_param_;
					} 
					if (max_rel_param_ >= 0) {
						max_val__ = mod_val(own_val(ownship)+max_rel_param_);
					} else {
						max_val__ = max_param_;
					} 
					if (mod_ == 0) {
						min_val__ = Util.max(min_val__,min_param_);
						max_val__ = Util.min(max_val__,max_param_);
					}
				}
				circular__ = mod_ > 0 && Util.almost_equals(min_val__,max_val__,DaidalusParameters.ALMOST_);
				if (circular__) {
					min_relative__ = mod_/2.0;
					max_relative__ = mod_/2.0;
				} else {
					if (min_rel_param_ > 0) {
						min_relative__ = min_rel_param_;
					} else {
						min_relative__ = mod_val(own_val(ownship)-min_val__);
					} 
					if (max_rel_param_ > 0) {
						max_relative__ = max_rel_param_;
					} else {
						max_relative__ = mod_val(max_val__-own_val(ownship));
					}
				}
				if ((min_val__ <= val && val <= max_val__ && min_val__ != max_val__) || 
						(mod_ > 0 && (circular__ ||
								(0 <= val && val <= max_val__) || (min_val__ <= val && val <= mod_)))) {
					checked__	 = 1;
				}	
			}
		}
		return checked__ > 0;
	}

	public boolean kinematic_conflict(TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			Detection3D detector, int epsh, int epsv, double alerting_time) {
		return check_input(ownship) && 
				any_red(detector,Detection3D.NoDetector,epsh,epsv,0.0,alerting_time,ownship,traffic,parameters);
	}

	public int length(DaidalusCore core) {   
		refresh(core);
		return ranges__.size();
	}

	public Interval interval(DaidalusCore core, int i) {
		if (i < 0 || i >= length(core)) {
			return Interval.EMPTY;
		}
		return ranges__.get(i).interval;
	}

	public BandsRegion region(DaidalusCore core, int i) {
		if (i < 0 || i >= length(core)) {
			return BandsRegion.UNKNOWN;
		} else {
			return ranges__.get(i).region;
		}
	}

	/** 
	 * Return index where val is found, -1 if invalid input, >= length if not found 
	 */
	public int indexOf(DaidalusCore core, double val) {
		if (check_input(core.ownship)) {
			val = mod_val(val);
			int last_index = length(core)-1;
			for (int i=0; i <= last_index; ++i) {
				boolean none = ranges__.get(i).region.isResolutionBand();	
				int order_i = ranges__.get(i).region.orderOfConflictRegion();
				boolean lb_close = none ||
						(i > 0 && order_i <= ranges__.get(i-1).region.orderOfConflictRegion()) ||
						(i == 0 && order_i <= ranges__.get(last_index).region.orderOfConflictRegion());
				boolean ub_close = none ||
						(i < last_index && order_i <= ranges__.get(i+1).region.orderOfConflictRegion()) ||
						(i == last_index && order_i <= ranges__.get(0).region.orderOfConflictRegion());
				if (ranges__.get(i).interval.almost_in(val,lb_close,ub_close,DaidalusParameters.ALMOST_)) {
					return i;
				} 
			}
			if (mod_ > 0) {
				if (Util.almost_equals(val,0,DaidalusParameters.ALMOST_)) {
					return 0;
				}
			} else {
				if (Util.almost_equals(val,min_val__,DaidalusParameters.ALMOST_)) {
					return 0;
				}
				if (Util.almost_equals(val,max_val__,DaidalusParameters.ALMOST_)) {
					return last_index;
				}			
			}
		}
		return -1;
	}

	/**
	 * Set cached values to stale conditions as they are no longer fresh 
	 */
	public void stale(boolean hysteresis) {
		if (!outdated__) {
			outdated__ = true;
			checked__ = -1;
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				acs_peripheral_bands__.get(conflict_region).clear();
				acs_bands__.get(conflict_region).clear();
			}
			ranges__.clear();
			recovery_time__ = Double.NaN;
			recovery_nfactor__ = 0;
			recovery_horizontal_distance__ = Double.NaN;
			recovery_vertical_distance__ = Double.NaN;
			min_val__ = 0;
			max_val__ = 0;
			min_relative__ = 0;
			max_relative__ = 0;
			circular__ = false;
		}
		if (hysteresis) {
			reset_hysteresis();
		}
	}

	/**
	 * Returns true is object is fresh
	 */
	public boolean isFresh() {
		return !outdated__;
	}

	/**
	 * Refresh cached values 
	 */
	public void refresh(DaidalusCore core) {
		if (outdated__) {
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				acs_bands__.get(conflict_region).addAll(core.acs_conflict_bands(conflict_region));
				if (core.bands_for(conflict_region)) {
					peripheral_aircraft(core,conflict_region);
					acs_bands__.get(conflict_region).addAll(acs_peripheral_bands__.get(conflict_region));
				}
			}
			if (check_input(core.ownship)) {
				compute(core);
			} 
			outdated__ = false;
		}
	}

	/**
	 *  Force computation of kinematic bands
	 */
	public void force_compute(DaidalusCore core) {
		stale(true);
		refresh(core);
	}

	/**
	 * Requires 0 <= conflict_region < CONFICT_BANDS and acs_peripheral_bands_ is empty
	 * Put in acs_peripheral_bands_ the list of aircraft predicted to have a peripheral band for the given region.
	 */
	private void peripheral_aircraft(DaidalusCore core, int conflict_region) {
		// Iterate on all traffic aircraft
		for (int ac = 0; ac < core.traffic.size(); ++ac) {
			TrafficState intruder = core.traffic.get(ac);
			Alerter alerter = core.alerter_of(ac);
			// Assumes that thresholds of severe alerts are included in the volume of less severe alerts
			BandsRegion region = BandsRegion.conflictRegionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
			int alert_level = alerter.alertLevelForRegion(region);
			if (alert_level > 0) {
				Detection3D detector = alerter.getLevel(alert_level).getCoreDetection();
				double alerting_time = Util.min(core.parameters.getLookaheadTime(),
						alerter.getLevel(alert_level).getAlertingTime());
				double s_err = intruder.relativeHorizontalPositionError(core.ownship,core.parameters);
				double sz_err = intruder.relativeVerticalPositionError(core.ownship,core.parameters);
				double v_err = intruder.relativeHorizontalSpeedError(core.ownship,s_err,core.parameters);
				double vz_err = intruder.relativeVerticalSpeedError(core.ownship,core.parameters);
				ConflictData det = detector.conflictDetectionSUM(core.ownship.get_s(),core.ownship.get_v(),
						intruder.get_s(),intruder.get_v(),0,alerting_time,
						s_err,sz_err,v_err,vz_err);
				if (!det.conflict() && kinematic_conflict(core.ownship,intruder,core.parameters,detector,
						core.epsilonH(false,intruder),core.epsilonV(false,intruder),alerting_time)) {
					acs_peripheral_bands__.get(conflict_region).add(new IndexLevelT(ac,alert_level,alerting_time,false));
				}
			}	
		}
	}

	/**
	 * Requires 0 <= conflict_region < CONFICT_BANDS
	 * @return sorted list of aircraft indices and alert_levels contributing to peripheral bands
	 * for given conflict region.
	 */
	public List<IndexLevelT> acs_peripheral_bands(DaidalusCore core, int conflict_region) {
		refresh(core);
		return acs_peripheral_bands__.get(conflict_region);
	}

	/**
	 * Return recovery information. 
	 */
	public RecoveryInformation recoveryInformation(DaidalusCore core) {   
		refresh(core);
		return new RecoveryInformation(recovery_time__,recovery_nfactor__, recovery_horizontal_distance__,recovery_vertical_distance__);
	}

	/**
	 * Return list of bands ranges 
	 */
	public List<BandsRange> ranges(DaidalusCore core) {
		refresh(core);
		return ranges__;
	}

	/** 
	 * Compute list of colored values in lcvs from sets of none bands
	 * Ensure that the intervals are "complete", filling in missing intervals and ensuring the 
	 * bands end at the proper bounds. 
	 */
	private void color_values(List<ColoredValue> lcvs, IntervalSet[] none_sets, DaidalusCore core, boolean recovery,
			int last_region) {

		BandsRegion green = recovery ? BandsRegion.RECOVERY : BandsRegion.NONE;
		if (mod_ == 0) {
			ColoredValue.init(lcvs,min_param_,max_param_,min_val__,max_val__,green); 
		} else {
			ColoredValue.init(lcvs,min_val__,max_val__,mod_,green);
		}

		for (int conflict_region = 0; conflict_region <= last_region; ++conflict_region) {
			if (core.bands_for(conflict_region)) {
				ColoredValue.insertNoneSetToColoredValues(lcvs, none_sets[conflict_region], 
						BandsRegion.conflictRegionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region));
				if (none_sets[conflict_region].isEmpty()) {
					break;
				}
			}
		}
	}

	/**
	 * Create an IntervalSet that represents a satured NONE band
	 */
	private void saturateNoneIntervalSet(IntervalSet noneset) {
		noneset.clear();
		if (mod_ == 0) {
			noneset.almost_add(min_val__,max_val__,DaidalusParameters.ALMOST_);
		} else {
			if (circular__) {
				noneset.almost_add(0,mod_,DaidalusParameters.ALMOST_);
			} else if (min_val__ < max_val__) {
				noneset.almost_add(min_val__,max_val__,DaidalusParameters.ALMOST_);
			} else {
				noneset.almost_add(min_val__,mod_,DaidalusParameters.ALMOST_);
				noneset.almost_add(0,max_val__,DaidalusParameters.ALMOST_);
			}
		}
	}

	/** 
	 * Compute none bands for a list ilts of IndexLevelT in none_set_region. 
	 * The none_set_region is initiated as a saturated green band.
	 * Uses aircraft detector if parameter detector is none.
	 * The epsilon parameters for coordinations are handled according to the recovery_case flag.
	 */
	private void compute_none_bands(IntervalSet none_set_region, List<IndexLevelT> ilts,
			Optional<Detection3D> det, Optional<Detection3D> recovery, 
			boolean recovery_case, double B, DaidalusCore core) {   
		saturateNoneIntervalSet(none_set_region);
		// Compute bands for given region
		for (IndexLevelT ilt : ilts) {
			TrafficState intruder = core.traffic.get(ilt.index_);
			Alerter alerter = core.alerter_of(ilt.index_);
			if (alerter.isValid()) {
				Detection3D detector = det.orElse(alerter.getLevel(ilt.level_).getCoreDetection());
				IntervalSet noneset2 = new IntervalSet();
				double T = //recovery_case && ilt.conflict_ac_ ? core.parameters.getLookaheadTime() : 
						ilt.T_;
				if (B > T) {
					// This case corresponds to recovery bands, where B is a recovery time.
					// If recovery time is greater than lookahead time for aircraft, then only
					// the internal cylinder is checked until this time.
					if (recovery.isPresent()) {
						none_bands(noneset2,recovery.get(),Detection3D.NoDetector,
								core.epsilonH(recovery_case,intruder),core.epsilonV(recovery_case,intruder),0,T,core.ownship,intruder,core.parameters);
					} else {
						saturateNoneIntervalSet(noneset2);
					}
				} else if (B <= T) {
					none_bands(noneset2,detector,recovery,
							core.epsilonH(recovery_case,intruder),core.epsilonV(recovery_case,intruder),B,T,core.ownship,intruder,core.parameters);
				} 
				none_set_region.almost_intersect(noneset2,DaidalusParameters.ALMOST_);
				if (none_set_region.isEmpty()) {
					break; // No need to compute more bands. This region is currently saturated.
				}
			} 
		}
	}

	/** 
	 * Compute recovery bands. Class variables recovery_time_, recovery_horizontal_distance_,
	 * and recovery_vertical_distance_ are set.
	 * Return true if non-saturated recovery bands where computed
	 */ 
	private boolean compute_recovery_bands(IntervalSet none_set_region, List<IndexLevelT> ilts,
			DaidalusCore core) {
		recovery_time__ = Double.NEGATIVE_INFINITY;
		recovery_nfactor__ = 0;
		recovery_horizontal_distance__ = Double.NEGATIVE_INFINITY;
		recovery_vertical_distance__ = Double.NEGATIVE_INFINITY;
		double T = core.parameters.getLookaheadTime();
		CDCylinder cd3d = CDCylinder.mk(core.parameters.getHorizontalNMAC(),core.parameters.getVerticalNMAC());
		Optional<Detection3D> ocd3d = Optional.of((Detection3D)cd3d);
		compute_none_bands(none_set_region,ilts,ocd3d,Detection3D.NoDetector,true,0.0,core);
		if (none_set_region.isEmpty()) {
			// If solid red, nothing to do. No way to kinematically escape using vertical speed without intersecting the
			// NMAC cylinder
			return false;
		} else {
			//CDCylinder cdmax = CDCylinder.mk(core.minHorizontalRecovery(),core.minVerticalRecovery());
			//Optional<Detection3D> ocdmax = Optional.of((Detection3D)cdmax);
			cd3d = CDCylinder.mk(core.minHorizontalRecovery(),core.minVerticalRecovery());
			ocd3d = Optional.of((Detection3D)cd3d);
			double factor = 1-core.parameters.getCollisionAvoidanceBandsFactor();
			while (cd3d.getHorizontalSeparation()  > core.parameters.getHorizontalNMAC() || 
					cd3d.getVerticalSeparation() > core.parameters.getVerticalNMAC()) {
				compute_none_bands(none_set_region,ilts,ocd3d,Detection3D.NoDetector,true,0.0,core);
				boolean solidred = none_set_region.isEmpty();
				if (solidred && !core.parameters.isEnabledCollisionAvoidanceBands()) {
					// Saturated band and collision avoidance is not enabled. Nothing to do here.
					return false;
				} else if (!solidred) {
					// Find first green band
					double pivot_red = 0;
					double pivot_green = T+1;
					double pivot = pivot_green-1;
					while ((pivot_green-pivot_red) > 0.5) {
						compute_none_bands(none_set_region,ilts,//recovery_nfactor__== 0 ? 
								Detection3D.NoDetector//:ocdmax
								,ocd3d,true,pivot,core);
						solidred = none_set_region.isEmpty();
						if (solidred) {
							pivot_red = pivot;
						} else {
							pivot_green = pivot;
						}
						pivot = (pivot_red+pivot_green)/2.0;
					}
					double recovery_time;
					if (pivot_green <= T) {
						recovery_time = Util.min(T,
								pivot_green+core.parameters.getRecoveryStabilityTime());
					} else {
						recovery_time = pivot_red;
					}			
					compute_none_bands(none_set_region,ilts,//recovery_nfactor__== 0 ? 
							Detection3D.NoDetector//:ocdmax
							,ocd3d,true,
							recovery_time,core);
					solidred = none_set_region.isEmpty();
					if (!solidred) {
						recovery_time__ = recovery_time;
						recovery_horizontal_distance__ = cd3d.getHorizontalSeparation();
						recovery_vertical_distance__ = cd3d.getVerticalSeparation();
						return true;
					} else if (!core.parameters.isEnabledCollisionAvoidanceBands()) {
						// Nothing else to do. Collision avoidance bands are not enabled.
						return false;
					}
				}
				++recovery_nfactor__;
				cd3d.setHorizontalSeparation(Math.max(core.parameters.getHorizontalNMAC(),cd3d.getHorizontalSeparation()*factor));
				cd3d.setVerticalSeparation(Math.max(core.parameters.getVerticalNMAC(),cd3d.getVerticalSeparation()*factor));
			}
		}
		return false;
	}

	// Return index of corrective region: 0: NEAR, 1: MID, 2: FAR	
	private static int corrective_region_index(DaidalusCore core) {
		return BandsRegion.NUMBER_OF_CONFLICT_BANDS-core.parameters.getCorrectiveRegion().orderOfConflictRegion();
	}

	/** 
	 * Requires: compute_bands(conflict_region) = true && 0 <= conflict_region < CONFLICT_BANDS
	 * Compute bands for one region. Return true iff recovery bands were computed.
	 */
	private boolean compute_region(IntervalSet[] none_sets, int conflict_region, DaidalusCore core) {  
		compute_none_bands(none_sets[conflict_region],acs_bands__.get(conflict_region),
				Detection3D.NoDetector,Detection3D.NoDetector,false,0.0,core);
		if (recovery_) {
			int corrective_region = corrective_region_index(core);
			if  (none_sets[conflict_region].isEmpty() && conflict_region <= corrective_region) {
				// Compute recovery bands
				if (compute_recovery_bands(none_sets[corrective_region],acs_bands__.get(corrective_region),core)) {
					CDCylinder cd3d = CDCylinder.mk(recovery_horizontal_distance__,recovery_vertical_distance__);
					Optional<Detection3D> ocd3d = Optional.of((Detection3D)cd3d);
					//CDCylinder cdmax = CDCylinder.mk(core.minHorizontalRecovery(),core.minVerticalRecovery());
					//Optional<Detection3D> ocdmax = Optional.of((Detection3D)cdmax);
					// Re-compute bands for regions that are more severe than conflict_region
					for (int i=0;i<corrective_region;++i){
						compute_none_bands(none_sets[i],acs_bands__.get(i),//recovery_nfactor__== 0 ? 
								Detection3D.NoDetector//:ocdmax
								,ocd3d,true,
								recovery_time__,core);
					}
				}
				return true;
			} else if (instantaneous_bands() && conflict_region == corrective_region &&
					core.tiov(conflict_region).low == 0) {
				// Recovery bands for instantaneous bands saturate when internal volume is violated
				recovery_time__ = 0;
				recovery_horizontal_distance__ = core.parameters.getMinHorizontalRecovery();
				recovery_vertical_distance__ = core.parameters.getMinVerticalRecovery();
				return true;
			}
		}
		// Normal conflict bands (recovery bands are not computed)
		return false;
	}

	/** 
	 * Compute all bands.
	 */
	private void compute(DaidalusCore core) {
		recovery_time__ = Double.NaN;
		recovery_horizontal_distance__ = Double.NaN;
		recovery_vertical_distance__ = Double.NaN;
		IntervalSet[] none_sets = new IntervalSet[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		for (int conflict_region=0;conflict_region<BandsRegion.NUMBER_OF_CONFLICT_BANDS;++conflict_region) {
			none_sets[conflict_region] = new IntervalSet();
		}
		boolean recovery = false;
		boolean saturated = false;
		int conflict_region = 0;
		for (;conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS && !saturated;++conflict_region)  {
			if (core.bands_for(conflict_region)) {
				recovery = compute_region(none_sets,conflict_region,core);
				saturated = recovery || none_sets[conflict_region].isEmpty();
			}
		} 
		--conflict_region;
		int corrective_region = corrective_region_index(core);
		// From this point on, hysteresis parameters are considered and they may affect the 
		// actual output.
		double delta = max_delta_resolution(core.parameters);
		if (!Double.isNaN(_last_time_) &&
				(core.current_time <= _last_time_ || 
				core.current_time - _last_time_ > core.parameters.getHysteresisTime())) {
			reset_hysteresis();
		}
		// Check if previous resolution is still OK and keep it by adding a yellow band to the left/right of
		// the resolution.
		double val = own_val(core.ownship);
		if (core.parameters.isEnabledBandsPersistence() &&
				!Double.isNaN(_last_time_) && !Double.isNaN(_resolution_low_) && !Double.isNaN(_resolution_up_) &&
				in_range(val,_resolution_low_,_resolution_up_) && 
				!core.acs_conflict_bands(corrective_region).isEmpty()) {
			IntervalSet res_set = new IntervalSet();
			if (Double.isFinite(_resolution_up_)) {
				double lb_max = in_range(_resolution_low_,_resolution_up_,max_val__)?_resolution_low_ : max_val__;
				add_noneset(res_set,_resolution_up_,lb_max);
			} 
			if (Double.isFinite(_resolution_low_)) {
				double ub_min = in_range(_resolution_up_,min_val__,_resolution_low_)?_resolution_up_ : min_val__;
				add_noneset(res_set,ub_min,_resolution_low_);
			}
			none_sets[corrective_region].almost_intersect(res_set,DaidalusParameters.ALMOST_);
		}
		List<ColoredValue> lcvs = new ArrayList<ColoredValue>();
		color_values(lcvs,none_sets,core,recovery,recovery?corrective_region:conflict_region);
		// Color bands
		ColoredValue.fromColoredValuestoBandsRanges(ranges__, lcvs);
		find_resolutions(core.ownship,none_sets[corrective_region],core.current_time); 
		preferred_direction_hysteresis(core,delta);
	}

	// Return true if val is in in range [lb,ub]. When mod_ > 0, lb may be greater than ub. In this case, 
	// mod logic is taken into account.
	private boolean in_range(double val, double lb, double ub) {
		if (Double.isFinite(val)) {
			lb = Double.isFinite(lb) ? lb : min_val__;
			ub = Double.isFinite(ub) ? ub : max_val__;
			if ((mod_ == 0 || Util.almost_less(lb,ub,DaidalusParameters.ALMOST_)) && 
					Util.almost_leq(lb,val,DaidalusParameters.ALMOST_) &&
					Util.almost_leq(val,ub,DaidalusParameters.ALMOST_)) {
				return true;			
			} else if (mod_ > 0) {
				if (Util.almost_equals(lb,ub,DaidalusParameters.ALMOST_) ||
						Util.almost_leq(val,ub,DaidalusParameters.ALMOST_) || 	
						Util.almost_leq(lb,val,DaidalusParameters.ALMOST_)) {
					return true;
				}
			}
		}
		return false;
	}

	// Find a resolution interval closest to the current value of the ownship (takes into account 
	// circular bands, i.e., when mod_ > 0)
	private void find_resolutions(TrafficState ownship, IntervalSet noneset, double time) {
		double l = Double.NEGATIVE_INFINITY;
		double u = Double.POSITIVE_INFINITY;
		double val = own_val(ownship);
		boolean conflict = true;
		if (!noneset.isEmpty()) {
			// There is a resolution
			for (int i=0; i < noneset.size(); ++i) {
				if (noneset.getInterval(i).almost_in(val,true,true,DaidalusParameters.ALMOST_)) {
					// There is no conflict
					l = Double.NaN;
					u = Double.NaN;
					conflict = false;
					break;
				} else if (noneset.getInterval(i).up < val) {
					if (i+1==noneset.size()) {
						l = noneset.getInterval(i).up;
						if (mod_ > 0) {
							u = noneset.getInterval(0).low;
						}
						break;
					} else if (val < noneset.getInterval(i+1).low) {
						l = noneset.getInterval(i).up;
						u = noneset.getInterval(i+1).low;
						break;
					}
				} else if (val < noneset.getInterval(i).low) {
					if (i==0) {
						if (mod_ > 0) {
							l = noneset.getInterval(noneset.size()-1).up;
						}
						u = noneset.getInterval(i).low;
						break;
					}
				}
			}
		}
		if (conflict) {
			if (Util.almost_geq(mod_val(val-l),min_relative__,DaidalusParameters.ALMOST_)) {
				l = Double.NEGATIVE_INFINITY; 
			}
			if (Util.almost_geq(mod_val(u-val),max_relative__,DaidalusParameters.ALMOST_)) {
				u = Double.POSITIVE_INFINITY; 
			}
		}
		_last_time_ = time;
		_resolution_up_ = u;
		_resolution_region_up_ = find_region_of_value(u);
		_resolution_low_ = l;
		_resolution_region_low_ = find_region_of_value(l);
	}

	// Reset values that control and that depend on hysteresis
	private void reset_hysteresis() {
		_last_time_ = Double.NaN;
		_time_of_dir_ = Double.NaN;
		_actual_dir_ = false;
		_preferred_dir_ = false;
		_resolution_up_ = Double.NaN;
		_resolution_low_ = Double.NaN;
		_resolution_region_up_ = BandsRegion.UNKNOWN;
		_resolution_region_low_ = BandsRegion.UNKNOWN;
	}

	/**
	 * Compute preferred direction based on resolution that is closer
	 * to current value.
	 */
	public void preferred_direction_hysteresis(DaidalusCore core, double delta) {
		double up = _resolution_up_;
		double low = _resolution_low_;
		if (!Double.isFinite(up) && !Double.isFinite(low)) {
			_time_of_dir_ = Double.NaN;
			_actual_dir_ = false;
			_preferred_dir_ = _actual_dir_;
		} else if (!Double.isFinite(up)) {
			_time_of_dir_ = core.current_time;
			_actual_dir_ = false;
			_preferred_dir_ = _actual_dir_;
		} else if (!Double.isFinite(low)) {
			_time_of_dir_ = core.current_time;
			_actual_dir_ = true;
			_preferred_dir_ = _actual_dir_;
		} else {
			double val = own_val(core.ownship);
			double mod_up = mod_val(up-val);
			double mod_down = mod_val(val-low);
			_actual_dir_ = Util.almost_leq(mod_up,mod_down,DaidalusParameters.ALMOST_);
			if (_actual_dir_ == _preferred_dir_ || Double.isNaN(_time_of_dir_) || core.current_time <= _time_of_dir_ ||
					(Math.abs(mod_up - mod_down) >= delta && core.current_time - _time_of_dir_ > core.parameters.getPersistenceTime())) {
				_time_of_dir_ = core.current_time;
				_preferred_dir_ = _actual_dir_;
			} 
			// In the else case preferred_dir remains the same
		}
	}


	private BandsRegion find_region_of_value(double val) {
		if (Double.isFinite(val)) {
			for (BandsRange br : ranges__) {
				if (br.region.isConflictBand() && br.interval.almost_in(val,true,true,DaidalusParameters.ALMOST_)) {
					return br.region;
				} else if (br.interval.low > val) {
					return BandsRegion.UNKNOWN;
				}
			}
		}
		return BandsRegion.UNKNOWN;
	}

	/** 
	 * Returns resolution maneuver.
	 * Return NaN if there is no conflict or if input is invalid.
	 * Return positive/negative infinity if there is no resolution to the 
	 * right/up and negative infinity if there is no resolution to the left/down.
	 */
	public double resolution(DaidalusCore core, boolean dir) {
		refresh(core);
		if (dir) {
			return _resolution_up_;
		} else {
			return _resolution_low_;
		}
	}

	/** 
	 * Returns resolution region for each direction (true=up/right, false=low/left)
	 */
	public BandsRegion resolution_region(DaidalusCore core, boolean dir) {
		refresh(core);
		if (dir) {
			return _resolution_region_up_;
		} else {
			return _resolution_region_low_;
		}
	}

	/**
	 * Compute preferred direction based on resolution that is closer
	 * to current value.
	 */
	public boolean preferred_direction(DaidalusCore core) {
		refresh(core);
		return _preferred_dir_;
	}

	/**
	 * Return last time to maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at ac_idx for conflict alert level. Return NaN if the ownship is not in conflict with aircraft within 
	 * early alerting time. Return negative infinity if there is no time to maneuver.
	 * Note: 1 <= alert_level <= alerter.size()
	 */
	public double last_time_to_maneuver(DaidalusCore core, TrafficState intruder) {
		int alert_idx = core.parameters.isAlertingLogicOwnshipCentric() ? core.ownship.getAlerterIndex() : intruder.getAlerterIndex();
		int alert_level = core.parameters.correctiveAlertLevel(alert_idx);
		TrafficState ownship = core.ownship;
		if (check_input(ownship) && alert_level > 0) {
			AlertThresholds alertthr = core.parameters.getAlerterAt(alert_idx).getLevel(alert_level);
			Detection3D detector = alertthr.getCoreDetection();
			double T = Util.min(core.parameters.getLookaheadTime(),alertthr.getEarlyAlertingTime());
			double s_err = intruder.relativeHorizontalPositionError(core.ownship,core.parameters);
			double sz_err = intruder.relativeVerticalPositionError(core.ownship,core.parameters);
			double v_err = intruder.relativeHorizontalSpeedError(core.ownship,s_err,core.parameters);
			double vz_err = intruder.relativeVerticalSpeedError(core.ownship,core.parameters);
			ConflictData det = detector.conflictDetectionSUM(ownship.get_s(),ownship.get_v(),intruder.get_s(),intruder.get_v(),0,T,
					s_err,sz_err,v_err,vz_err);
			if (det.conflict()) {
				double pivot_red = det.getTimeIn();
				if (pivot_red == 0) {
					return Double.NEGATIVE_INFINITY;
				} 
				double pivot_green = 0;
				double pivot = pivot_green;    
				while ((pivot_red-pivot_green) > 0.5) {
					TrafficState ownship_at_pivot  = ownship.linearProjection(pivot); 
					TrafficState intruder_at_pivot = intruder.linearProjection(pivot);
					if (detector.violationSUMAt(ownship_at_pivot.get_s(),ownship_at_pivot.get_v(),intruder_at_pivot.get_s(),intruder_at_pivot.get_v(),
							s_err,sz_err,v_err,vz_err,0.0) ||
							all_red(detector,Detection3D.NoDetector,0,0,0.0,T,ownship_at_pivot,intruder_at_pivot,core.parameters)) {
						pivot_red = pivot;
					} else {
						pivot_green = pivot;
					}
					pivot = (pivot_red+pivot_green)/2.0;
				}
				if (pivot_green == 0) {
					return Double.NEGATIVE_INFINITY;
				} else {
					return pivot_green;
				}
			}
		}
		return Double.NaN;
	}

	private int maxdown(TrafficState ownship) {
		int down = (int)Math.ceil(min_relative__/get_step());
		if (mod_ > 0 && Util.almost_greater(down*get_step(),mod_/2.0,DaidalusParameters.ALMOST_)) {
			--down;
		}
		return down;
	}

	private int maxup(TrafficState ownship) {
		int up = (int)Math.ceil(max_relative__/get_step());
		if (mod_ > 0 && Util.almost_greater(up*get_step(),mod_/2.0,DaidalusParameters.ALMOST_)) {
			--up;
		}    
		return up;
	}

	/** Add (lb,ub) to noneset. In the case of mod_ > 0, lb can be greater than ub. This function takes 
	 * care of the mod logic. This function doesn't do anything when lb and ub are almost equals. 
	 * @param noneset: Interval set where (lb,ub) will be added
	 * @param lb: lower bound
	 * @param ub: upper bound
	 * When mod_ = 0, lb <= ub. 
	 */
	private void add_noneset(IntervalSet noneset, double lb, double ub) {
		if (Util.almost_equals(lb,ub,DaidalusParameters.ALMOST_)) {
			return;
		}
		if (mod_ == 0)  {
			lb = Util.max(min_val__,lb);
			ub = Util.min(max_val__,ub);
			if (Util.almost_less(lb,ub,DaidalusParameters.ALMOST_)) {
				noneset.almost_add(lb,ub,DaidalusParameters.ALMOST_);
			}
			return;
		}
		lb = mod_val(lb);
		ub = mod_val(ub);
		IntervalSet minmax_noneset = new IntervalSet();
		if (circular__) {
		} else if (min_val__ < max_val__) {
			minmax_noneset.almost_add(min_val__,max_val__,DaidalusParameters.ALMOST_);
		} else {
			minmax_noneset.almost_add(0,max_val__,DaidalusParameters.ALMOST_);
			minmax_noneset.almost_add(min_val__,mod_,DaidalusParameters.ALMOST_);
		}
		IntervalSet lbub_noneset = new IntervalSet();
		if (Util.almost_equals(lb,ub,DaidalusParameters.ALMOST_)) {
		} else if (lb < ub) {
			lbub_noneset.almost_add(lb,ub,DaidalusParameters.ALMOST_);
		} else {
			lbub_noneset.almost_add(0,ub,DaidalusParameters.ALMOST_);
			lbub_noneset.almost_add(lb,mod_,DaidalusParameters.ALMOST_);
		}
		if (minmax_noneset.isEmpty() && lbub_noneset.isEmpty()) {
			// In this case, the band if all green from 0 to mod
			noneset.almost_add(0,mod_,DaidalusParameters.ALMOST_);
		} else if (lbub_noneset.isEmpty()) {
			// In this case return minmax_noneset
			noneset.almost_union(minmax_noneset,DaidalusParameters.ALMOST_);
		} else {
			if (!minmax_noneset.isEmpty()) {
				lbub_noneset.almost_intersect(minmax_noneset,DaidalusParameters.ALMOST_);
			}
			noneset.almost_union(lbub_noneset,DaidalusParameters.ALMOST_);
		}
	}

	/**
	 *  This function scales the interval, add a constant, and constraint the intervals to min and max.
	 *  The function takes care of modulo logic, in the case of circular bands.
	 */
	public void toIntervalSet(IntervalSet noneset, List<Integerval> l, double scal, double add) {
		noneset.clear();
		for (int i=0; i < (int) l.size(); ++i) {
			Integerval ii = l.get(i);
			double lb = scal*ii.lb+add;
			double ub = scal*ii.ub+add;
			add_noneset(noneset,lb,ub);
		}
	}

	/** 
	 * The output parameter noneset has a list of non-conflict ranges orderd within [min,max] 
	 * values (or [0,mod] in the case of circular bands, i.e., when mod == 0).
	 */
	public void none_bands(IntervalSet noneset, Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		List<Integerval> bands_int = new ArrayList<Integerval>();
		int mino = maxdown(ownship);
		int maxo = maxup(ownship);
		if (instantaneous_bands()) {
			instantaneous_bands_combine(bands_int,conflict_det,recovery_det,B,T,0.0,B,
					mino,maxo,ownship,traffic,parameters,epsh,epsv); 
		} else {
			kinematic_bands_combine(bands_int,conflict_det,recovery_det,time_step(ownship),B,T,0.0,B,
					mino,maxo,ownship,traffic,parameters,epsh,epsv); 
		}		
		toIntervalSet(noneset,bands_int,get_step(),own_val(ownship));
	}

	public boolean any_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		return instantaneous_bands() ? 
				any_instantaneous_red(conflict_det,recovery_det,B,T,0.0,B,
						maxdown(ownship),maxup(ownship),ownship,traffic,parameters,epsh,epsv,0):
							any_int_red(conflict_det,recovery_det,time_step(ownship),B,T,0.0,B,
									maxdown(ownship),maxup(ownship),ownship,traffic,parameters,epsh,epsv,0);
	}

	public boolean all_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		return instantaneous_bands() ? 
				all_instantaneous_red(conflict_det,recovery_det,B,T,0,B,
						maxdown(ownship),maxup(ownship),ownship,traffic,parameters,epsh,epsv,0):
							all_int_red(conflict_det,recovery_det,time_step(ownship),B,T,0.0,B,
									maxdown(ownship),maxup(ownship),ownship,traffic,parameters,epsh,epsv,0);
	}

	public boolean all_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		return !any_red(conflict_det,recovery_det,epsh,epsv,B,T,ownship,traffic,parameters);
	}

	public boolean any_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		return !all_red(conflict_det,recovery_det,epsh,epsv,B,T,ownship,traffic,parameters);
	}

	/**
	 * This function returns a resolution maneuver that is valid from B to T. 
	 * It returns NaN if there is no conflict and +/- infinity, depending on dir, if there 
	 * are no resolutions. 
	 * The value dir=false is down and dir=true is up. 
	 */
	public double resolution(Detection3D conflict_det, Optional<Detection3D> recovery_det, TrafficState repac, 
			int epsh, int epsv, double B, double T, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			boolean dir) {
		int maxn;
		int sign;
		if (dir) {
			maxn = maxup(ownship);
			sign = 1;
		} else {
			maxn = maxdown(ownship);
			sign = -1;
		}
		int ires = first_green(conflict_det,recovery_det,time_step(ownship),B,T,0.0,B,
				dir,maxn,ownship,traffic,parameters,epsh,epsv);
		if (ires == 0) {
			return Double.NaN;
		} else if (ires < 0) {
			return sign*Double.POSITIVE_INFINITY;
		} else {
			return mod_val(own_val(ownship)+sign*ires*get_step());
		}
	}

	public String rawString() {
		String s = "";
		s+="# Private variables_\n";
		s+="mod_ = "+f.FmPrecision(mod_)+"\n";
		s+="step_ = "+f.FmPrecision(step_)+"\n";
		s+="min_param_ = "+f.FmPrecision(min_param_)+"\n";
		s+="max_param_ = "+f.FmPrecision(max_param_)+"\n";
		s+="min_rel_param_ = "+f.FmPrecision(min_rel_param_)+"\n";
		s+="max_rel_param_ = "+f.FmPrecision(max_rel_param_)+"\n";
		s+="recovery_ = "+recovery_+"\n";
		s+="# Cached variables__\n";
		s+="outdated__ = "+outdated__+"\n";		
		s+="checked__ = "+f.Fmi(checked__)+"\n";		
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_peripheral_bands__["+f.Fmi(conflict_region)+"] = "+
					IndexLevelT.toString(acs_peripheral_bands__.get(conflict_region))+"\n";
		}
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_bands__["+f.Fmi(conflict_region)+"] = "+
					IndexLevelT.toString(acs_bands__.get(conflict_region))+"\n";
		}
		for (int i = 0; i < ranges__.size(); ++i) {
			s+="ranges__["+f.Fmi(i)+"] = ";
			s+=ranges__.get(i).toString()+"\n";
		} 
		s+="recovery_time__ = "+f.FmPrecision(recovery_time__)+"\n";
		s+="recovery_nfactor__ = "+f.Fmi(recovery_nfactor__)+"\n";
		s+="recovery_horizontal_distance__ = "+f.FmPrecision(recovery_horizontal_distance__)+ " [m]\n";
		s+="recovery_vertical_distance__ = "+f.FmPrecision(recovery_vertical_distance__)+ " [m]\n";
		s+="min_val__ = "+f.FmPrecision(min_val__)+"\n";
		s+="max_val__ = "+f.FmPrecision(max_val__)+"\n";
		s+="min_relative__ = "+f.FmPrecision(min_relative__)+"\n";
		s+="max_relative__ = "+f.FmPrecision(max_relative__)+"\n";
		s+="circular__ = "+circular__+"\n";
		s+="# Hysteresis _variables_\n";
		s+="_last_time_ = "+f.FmPrecision(_last_time_)+"\n";
		s+="_time_of_dir_ = "+f.FmPrecision(_time_of_dir_)+"\n";
		s+="_actual_dir_ = "+_actual_dir_+"\n";
		s+="_preferred_dir_ = "+_preferred_dir_+"\n";
		s+="_resolution_low_ = "+f.FmPrecision(_resolution_low_)+"\n";
		s+="_resolution_up_ = "+f.FmPrecision(_resolution_up_)+"\n";
		s+="_resolution_region_low_ = "+_resolution_region_low_+"\n";
		s+="_resolution_region_up_ = "+_resolution_region_up_+"\n";
		return s;
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < ranges__.size(); ++i) {
			s+="ranges["+f.Fmi(i)+"] = ";
			s+=ranges__.get(i).toString()+"\n";
		} 
		s+="recovery_time = "+f.FmPrecision(recovery_time__)+" [s]\n";
		s+="recovery_nfactor = "+f.Fmi(recovery_nfactor__)+"\n";
		s+="recovery_horizontal_distance = "+f.FmPrecision(recovery_horizontal_distance__)+ " [m]\n";
		s+="recovery_vertical_distance = "+f.FmPrecision(recovery_vertical_distance__)+ " [m]\n";
		s+="preferred_dir = "+_preferred_dir_+"\n";
		s+="resolution_low = "+f.FmPrecision(_resolution_low_)+"\n";
		s+="resolution_up = "+f.FmPrecision(_resolution_up_)+"\n";
		s+="resolution_region_low = "+_resolution_region_low_.toString()+"\n";
		s+="resolution_region_up = "+_resolution_region_up_.toString()+"\n";
		return s;
	}

	public String toPVS() {
		String s = "(:";
		for (int i = 0; i < ranges__.size(); ++i) {
			if (i > 0) { 
				s+=", ";
			} else {
				s+=" ";
			}
			s += ranges__.get(i).toPVS();
		} 
		s+=" :)";
		return s;
	}

}
