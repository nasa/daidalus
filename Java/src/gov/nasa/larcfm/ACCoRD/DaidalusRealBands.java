/*
 * Copyright (c) 2015-2021 United States Government as represented by
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

	/*** PRIVATE VARIABLES */

	private double mod_;  // If mod_ > 0, bands are circular modulo this value
	// When the following values are different from 0, they overwrite the values in the
	// parameters.
	// min_rel (max_rel) is the positive distance from current value to minimum (maximum) value. 
	// When mod_ > 0, min_rel, max_rel in [0,mod_/2]		
	private double min_rel_; // Relative min value. A negative value represents val-min, i.e., from val to min 
	private double max_rel_; // Relative max value. A negative value represents max-val, i.e., from val to max

	/**** CACHED VARIABLES ****/

	private boolean outdated_; // Boolean to control re-computation of cached values
	private int checked_;  // Cached status of input values. Negative unchecked, 0 invalid, 1 valid

	/* Cached lists of aircraft indices, alert_levels, and lookahead times, sorted by indices, contributing to peripheral 
	 * bands listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR */
	private List<List<IndexLevelT>> acs_peripheral_bands_; 

	/* Cached lists of aircraft indices, alert_levels, and lookahead times, sorted by indices, contributing to any type
	 * of bands listed per conflict bands, where 0th:NEAR, 1th:MID, 2th:FAR.
	 * These lists are computed as the concatenation of acs_conflict_bands and acs_peripheral_bands. */
	private List<List<IndexLevelT>> acs_bands_; 

	private List<BandsRange> ranges_;     // Cached list of bands ranges

	/* 
	 * recovery_time_ is the time to recovery from violation. 
	 * Negative infinity means no possible recovery.
	 * NaN means no recovery bands are computed (either because there is no conflict or
	 * because they are disabled)
	 */		
	private double recovery_time_; // Cached recovery time
	private int recovery_nfactor_; // Cached number of times the recovery volume was reduced

	/*
	 * recovery_horizontal_distance and recovery_vertical_distance is the 
	 * distance guaranteed by the recovery bands. Negative infinity means no possible recovery.
	 * NaN means no recovery bands are computed (either because there is no conflict of
	 * because they are disabled)
	 */
	private double recovery_horizontal_distance_; // Cached recovery horizontal_separation
	private double recovery_vertical_distance_; // Cached recovery horizontal_separation

	private double min_val_; // Absolute min value (min_val= min when mod == 0 && !rel)
	private double max_val_; // Absolute max value (max_val = max when mod == 0 && !rel)
	private double min_relative_; // Computed relative min value 
	private double max_relative_; // Computed relative max value 
	private boolean circular_; // True if bands is fully circular


	/**** HYSTERESIS VARIABLES ****/

	private BandsHysteresis bands_hysteresis_;

	public DaidalusRealBands(double mod) {	
		// Private variables are initialized
		mod_ = Math.abs(mod);
		min_rel_ = 0;
		max_rel_ = 0;

		// Cached arrays_ are initialized
		acs_peripheral_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		acs_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_peripheral_bands_.add(new ArrayList<IndexLevelT>());
			acs_bands_.add(new ArrayList<IndexLevelT>());
		}
		ranges_ = new ArrayList<BandsRange>();
		bands_hysteresis_ = new BandsHysteresis();
		bands_hysteresis_.setMod(mod_);

		// Cached_ variables are cleared
		outdated_ = false; // Force stale
		stale();
	}

	public DaidalusRealBands() {
		this(0);
	}

	public DaidalusRealBands(DaidalusRealBands b) {

		// Private variables are copied
		mod_ = b.mod_;
		min_rel_ = b.min_rel_;
		max_rel_ = b.max_rel_;

		// Cached arrays_ are initialized
		acs_peripheral_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		acs_bands_ = new ArrayList<List<IndexLevelT>>(BandsRegion.NUMBER_OF_CONFLICT_BANDS);
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			acs_peripheral_bands_.add(new ArrayList<IndexLevelT>());
			acs_bands_.add(new ArrayList<IndexLevelT>());
		}
		ranges_ = new ArrayList<BandsRange>();
		bands_hysteresis_ = new BandsHysteresis();
		bands_hysteresis_.setMod(mod_);

		// Cached_ variables are cleared
		outdated_ = false; // Force stale
		stale();
	}

	abstract public boolean get_recovery(DaidalusParameters parameters);

	abstract public double get_step(DaidalusParameters parameters);

	abstract public double get_min(DaidalusParameters parameters);

	abstract public double get_max(DaidalusParameters parameters);

	abstract public double get_min_rel(DaidalusParameters parameters);

	abstract public double get_max_rel(DaidalusParameters parameters);

	abstract public double own_val(TrafficState ownship);

	abstract public double time_step(DaidalusParameters parameters, TrafficState ownship);

	abstract public boolean instantaneous_bands(DaidalusParameters parameters);

	abstract public double max_delta_resolution(DaidalusParameters parameters);

	private double min_rel(DaidalusParameters parameters) {
		if (min_rel_ == 0 && max_rel_ == 0) {
			return get_min_rel(parameters);
		} else {
			return min_rel_;
		}
	}

	private double max_rel(DaidalusParameters parameters) {
		if (min_rel_ == 0 && max_rel_ == 0) {
			return get_max_rel(parameters);
		} else {
			return max_rel_;
		}
	}

	public boolean saturate_corrective_bands(DaidalusParameters parameters, int dta_status) {
		return false;
	}

	public void set_special_configuration(DaidalusParameters parameters, int dta_status) {	
		// If necessary to be defined by the subclasses
	}

	public double get_min_val_() {
		return min_val_;
	}

	public double get_max_val_() {
		return max_val_;
	}

	public double get_mod() {
		return mod_;
	}

	/**
	 * Overwrite relative values from those in the parameters. When {@code mod_ > 0}, requires min_rel and max_rel 
	 * to be in [0,mod/2]. When mod_ == 0, a negative min_rel value represents val-min and a negative value 
	 * max_rel value represents max-val.
	 */
	public void set_min_max_rel(double min_rel, double max_rel) {
		min_rel_ = min_rel;
		max_rel_ = max_rel;
		// This method doesn't stale data. Use with care.
	}

	private boolean set_input(DaidalusParameters parameters, TrafficState ownship, int dta_status) {
		if (checked_ < 0) {
			checked_ = 0;
			set_special_configuration(parameters,dta_status);
			if (ownship.isValid() && get_step(parameters) > 0) { 
				double val = own_val(ownship);
				// When mod_ == 0, min_val <= max_val. When mod_ > 0, min_val, max_val in [0,mod_]. 
				// In the later case, min_val may be greater than max_val. Furthermore, min_val = max_val means 
				// a range of values from 0 to mod, i.e., a circular band.
				if (min_rel(parameters) == 0 && max_rel(parameters) == 0) {
					min_val_ = get_min(parameters);
					max_val_ = get_max(parameters);
				} else {
					if (min_rel(parameters) >= 0) {
						min_val_ = Util.safe_modulo(val-min_rel(parameters),mod_);
					} else {
						min_val_ = get_min(parameters);
					} 
					if (max_rel(parameters) >= 0) {
						max_val_ = Util.safe_modulo(val+max_rel(parameters),mod_);
					} else {
						max_val_ = get_max(parameters);
					} 
					if (mod_ == 0) {
						min_val_ = Util.max(min_val_,get_min(parameters));
						max_val_ = Util.min(max_val_,get_max(parameters));
					}
				}
				circular_ = mod_ > 0 && Util.within_epsilon(min_val_,max_val_,get_step(parameters)*0.25);
				// changed from alomst_equals to within_epsilon to avoid numeric instability in some cases
				if (circular_) {
					min_relative_ = mod_/2.0;
					max_relative_ = mod_/2.0;
				} else {
					if (min_rel(parameters) > 0) {
						min_relative_ = min_rel(parameters);
					} else {
						min_relative_ = Util.safe_modulo(val-min_val_,mod_);
					} 
					if (max_rel(parameters) > 0) {
						max_relative_ = max_rel(parameters);
					} else {
						max_relative_ = Util.safe_modulo(max_val_-val,mod_);
					}
				}
				if ((min_val_ <= val && val <= max_val_ && min_val_ != max_val_) || 
						(mod_ > 0 && (circular_ ||
								(0 <= val && val <= max_val_) || (min_val_ <= val && val <= mod_)))) {
					checked_	 = 1;
				}	
			}
		}
		return checked_ > 0;
	}

	public boolean kinematic_conflict(DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			Detection3D detector, int epsh, int epsv, double alerting_time,
			int dta_status) {
		return set_input(parameters,ownship,dta_status) && 
				any_red(detector,Detection3D.NoDetector,epsh,epsv,0.0,alerting_time,parameters,ownship,traffic);
	}

	public int length(DaidalusCore core) {   
		refresh(core);
		return ranges_.size();
	}

	public Interval interval(DaidalusCore core, int i) {
		if (i < 0 || i >= length(core)) {
			return Interval.EMPTY;
		}
		return ranges_.get(i).interval;
	}

	public BandsRegion region(DaidalusCore core, int i) {
		if (i < 0 || i >= length(core)) {
			return BandsRegion.UNKNOWN;
		} else {
			return ranges_.get(i).region;
		}
	}

	/** 
	 * Return index in ranges_ where val is found, -1 if invalid input or not found 
	 */
	public int indexOf(DaidalusCore core, double val) {
		if (set_input(core.parameters,core.ownship,core.DTAStatus())) {
			refresh(core);
			return BandsRange.index_of(ranges_,val,mod_);
		} else {
			return -1;
		}
	}

	/**
	 * Set cached values to stale conditions as they are no longer fresh 
	 */
	public void stale() {
		if (!outdated_) {
			outdated_ = true;
			checked_ = -1;
			for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
				acs_peripheral_bands_.get(conflict_region).clear();
				acs_bands_.get(conflict_region).clear();
			}
			ranges_.clear();
			recovery_time_ = Double.NaN;
			recovery_nfactor_ = -1;
			recovery_horizontal_distance_ = Double.NaN;
			recovery_vertical_distance_ = Double.NaN;
			min_rel_ = 0;
			max_rel_ = 0;
			min_val_ = 0;
			max_val_ = 0;
			min_relative_ = 0;
			max_relative_ = 0;
			circular_ = false;
		}
	}

	/**
	 * clear hysteresis 
	 */
	public void clear_hysteresis() {
		bands_hysteresis_.reset();
		stale();
	}

	/**
	 * Returns true is object is fresh
	 */
	public boolean isFresh() {
		return !outdated_;
	}

	/**
	 * Refresh cached values 
	 */
	public void refresh(DaidalusCore core) {
		if (outdated_) {
			if (set_input(core.parameters,core.ownship,core.DTAStatus())) {
				for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
					acs_bands_.get(conflict_region).addAll(core.acs_conflict_bands(conflict_region));
					if (core.bands_for(conflict_region)) {
						peripheral_aircraft(core,conflict_region);
						acs_bands_.get(conflict_region).addAll(acs_peripheral_bands_.get(conflict_region));
					}
				}
				compute(core);
			} 
			outdated_ = false;
		}
	}

	/**
	 *  Force computation of kinematic bands
	 */
	public void force_compute(DaidalusCore core) {
		stale();
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
			int alerter_idx = core.alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= core.parameters.numberOfAlerters()) {
				Alerter alerter = core.parameters.getAlerterAt(alerter_idx);
				// Assumes that thresholds of severe alerts are included in the volume of less severe alerts
				BandsRegion region = BandsRegion.regionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region);
				int alert_level = alerter.alertLevelForRegion(region);
				if (alert_level > 0) {
					Detection3D detector = alerter.getLevel(alert_level).getCoreDetection();
					double alerting_time = Util.min(core.parameters.getLookaheadTime(),
							alerter.getLevel(alert_level).getAlertingTime());
					ConflictData det = detector.conflictDetectionWithTrafficState(core.ownship,intruder,0.0,core.parameters.getLookaheadTime());
					if (!det.conflictBefore(alerting_time) && kinematic_conflict(core.parameters,core.ownship,intruder,detector,
							core.epsilonH(false,intruder),core.epsilonV(false,intruder),alerting_time,
							core.DTAStatus())) {
						acs_peripheral_bands_.get(conflict_region).add(new IndexLevelT(ac,alert_level,alerting_time));
					}
				}	
			}
		}
	}

	/**
	 * Requires {@code 0 <= conflict_region < CONFICT_BANDS}
	 * @return sorted list of aircraft indices and alert_levels contributing to peripheral bands
	 * for given conflict region.
	 */
	public List<IndexLevelT> acs_peripheral_bands(DaidalusCore core, int conflict_region) {
		refresh(core);
		return acs_peripheral_bands_.get(conflict_region);
	}

	/**
	 * Return recovery information. 
	 */
	public RecoveryInformation recoveryInformation(DaidalusCore core) {   
		refresh(core);
		return new RecoveryInformation(recovery_time_,recovery_nfactor_, recovery_horizontal_distance_,recovery_vertical_distance_);
	}

	/**
	 * Return list of bands ranges 
	 */
	public List<BandsRange> ranges(DaidalusCore core) {
		refresh(core);
		return ranges_;
	}

	/** 
	 * Compute list of color values in lcvs from sets of none bands
	 * Ensure that the intervals are "complete", filling in missing intervals and ensuring the 
	 * bands end at the proper bounds. 
	 */
	private void color_values(List<ColorValue> lcvs, IntervalSet[] none_sets, DaidalusCore core, boolean recovery,
			int last_region) {
		if (mod_ == 0) {
			ColorValue.init(lcvs,get_min(core.parameters),get_max(core.parameters),min_val_,max_val_,BandsRegion.NONE); 
		} else {
			ColorValue.init_with_mod(lcvs,min_val_,max_val_,mod_,BandsRegion.NONE);
		}
		for (int conflict_region = 0; conflict_region <= last_region; ++conflict_region) {
			if (core.bands_for(conflict_region)) {
				ColorValue.insertNoneSetToColorValues(lcvs, none_sets[conflict_region], 
						BandsRegion.regionFromOrder(BandsRegion.NUMBER_OF_CONFLICT_BANDS-conflict_region));
				if (none_sets[conflict_region].isEmpty()) {
					break;
				}
			}
		}
		if (recovery) {
			ColorValue.insertRecoverySetToColorValues(lcvs,none_sets[last_region]);
		}
	}

	/**
	 * Create an IntervalSet that represents a satured NONE band
	 */
	private void saturateNoneIntervalSet(IntervalSet noneset) {
		noneset.clear();
		if (mod_ == 0) {
			noneset.almost_add(min_val_,max_val_,DaidalusParameters.ALMOST_);
		} else {
			if (circular_) {
				noneset.almost_add(0,mod_,DaidalusParameters.ALMOST_);
			} else if (min_val_ < max_val_) {
				noneset.almost_add(min_val_,max_val_,DaidalusParameters.ALMOST_);
			} else {
				noneset.almost_add(min_val_,mod_,DaidalusParameters.ALMOST_);
				noneset.almost_add(0,max_val_,DaidalusParameters.ALMOST_);
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
			TrafficState intruder = core.traffic.get(ilt.index);
			int alerter_idx = core.alerter_index_of(intruder);
			if (1 <= alerter_idx && alerter_idx <= core.parameters.numberOfAlerters()) {
				Alerter alerter = core.parameters.getAlerterAt(alerter_idx);
				Detection3D detector = det.orElse(alerter.getLevel(ilt.level).getCoreDetection());
				IntervalSet noneset2 = new IntervalSet();
				double T = ilt.time_horizon;
				if (B > T) {
					// This case corresponds to recovery bands, where B is a recovery time.
					// If recovery time is greater than lookahead time for aircraft, then only
					// the internal cylinder is checked until this time.
					if (recovery.isPresent()) {
						none_bands(noneset2,recovery.get(),Detection3D.NoDetector,
								core.epsilonH(recovery_case,intruder),core.epsilonV(recovery_case,intruder),0,T,
								core.parameters,core.ownship,intruder);
					} else {
						saturateNoneIntervalSet(noneset2);
					}
				} else if (B <= T) {
					none_bands(noneset2,detector,recovery,
							core.epsilonH(recovery_case,intruder),core.epsilonV(recovery_case,intruder),B,T,
							core.parameters,core.ownship,intruder);
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
		recovery_time_ = Double.NEGATIVE_INFINITY;
		recovery_nfactor_ = 0;
		recovery_horizontal_distance_ = Double.NEGATIVE_INFINITY;
		recovery_vertical_distance_ = Double.NEGATIVE_INFINITY;
		double T = core.parameters.getLookaheadTime();
		CDCylinder cd3d = CDCylinder.mk(core.parameters.getHorizontalNMAC(),core.parameters.getVerticalNMAC());
		Optional<Detection3D> ocd3d = Optional.of((Detection3D)cd3d);
		compute_none_bands(none_set_region,ilts,ocd3d,Detection3D.NoDetector,true,0.0,core);
		if (none_set_region.isEmpty()) {
			// If solid red, nothing to do. No way to kinematically escape using vertical speed without intersecting the
			// NMAC cylinder
			return false;
		} else {
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
						compute_none_bands(none_set_region,ilts,Detection3D.NoDetector,ocd3d,true,pivot,core);
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
					compute_none_bands(none_set_region,ilts,Detection3D.NoDetector,ocd3d,true,
							recovery_time,core);
					solidred = none_set_region.isEmpty();
					if (!solidred) {
						recovery_time_ = recovery_time;
						recovery_horizontal_distance_ = cd3d.getHorizontalSeparation();
						recovery_vertical_distance_ = cd3d.getVerticalSeparation();
						return true;
					} else if (!core.parameters.isEnabledCollisionAvoidanceBands()) {
						// Nothing else to do. Collision avoidance bands are not enabled.
						return false;
					}
				}
				++recovery_nfactor_;
				cd3d.setHorizontalSeparation(Math.max(core.parameters.getHorizontalNMAC(),cd3d.getHorizontalSeparation()*factor));
				cd3d.setVerticalSeparation(Math.max(core.parameters.getVerticalNMAC(),cd3d.getVerticalSeparation()*factor));
			}
		}
		return false;
	}

	/** 
	 * Requires: compute_bands(conflict_region) = true && 0 <= conflict_region < CONFLICT_BANDS
	 * Compute bands for one region. Return true iff recovery bands were computed.
	 */
	private boolean compute_region(IntervalSet[] none_sets, int conflict_region, int corrective_region,
			DaidalusCore core) {  
		if (saturate_corrective_bands(core.parameters,core.DTAStatus()) && conflict_region <= corrective_region) {
			none_sets[conflict_region].clear();
			return false;
		}
		compute_none_bands(none_sets[conflict_region],acs_bands_.get(conflict_region),
				Detection3D.NoDetector,Detection3D.NoDetector,false,0.0,core);
		if (get_recovery(core.parameters)) {
			if  (none_sets[conflict_region].isEmpty() && conflict_region <= corrective_region) {
				// Compute recovery bands
				compute_recovery_bands(none_sets[corrective_region],acs_bands_.get(corrective_region),core);
				return true;
			} else if (instantaneous_bands(core.parameters) && conflict_region == corrective_region &&
					core.tiov(conflict_region).low == 0) {
				// Recovery bands for instantaneous bands saturate when internal volume is violated
				recovery_time_ = 0;
				recovery_nfactor_ = 0;
				recovery_horizontal_distance_ = core.parameters.getMinHorizontalRecovery();
				recovery_vertical_distance_ = core.parameters.getMinVerticalRecovery();
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
		recovery_time_ = Double.NaN;
		recovery_horizontal_distance_ = Double.NaN;
		recovery_vertical_distance_ = Double.NaN;
		IntervalSet[] none_sets = new IntervalSet[BandsRegion.NUMBER_OF_CONFLICT_BANDS];
		for (int conflict_region=0;conflict_region<BandsRegion.NUMBER_OF_CONFLICT_BANDS;++conflict_region) {
			none_sets[conflict_region] = new IntervalSet();
		}
		boolean recovery = false;
		boolean saturated = false;
		int conflict_region = 0;
		double val = own_val(core.ownship);
		int corrective_region = BandsRegion.NUMBER_OF_CONFLICT_BANDS-BandsRegion.orderOfRegion(core.parameters.getCorrectiveRegion());
		// From most severe to least severe
		while (conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS && !saturated)  {
			if (core.bands_for(conflict_region)) {
				recovery = compute_region(none_sets,conflict_region,corrective_region,core);
				saturated = recovery || none_sets[conflict_region].isEmpty();
			} 
			++conflict_region;
		} 
		if (recovery) {
			conflict_region = corrective_region;
		} else {
			--conflict_region; 
		} 
		// At this point conflict_region has the last region for which bands are computed
		// Compute list of color values (primitive representation of bands)
		List<ColorValue> lcvs = new ArrayList<ColorValue>();
		color_values(lcvs,none_sets,core,recovery,conflict_region);

		// From this point of hysteresis logic, including M of N and persistence, is applied.
		if (Double.isNaN(bands_hysteresis_.getLastTime())) {
			bands_hysteresis_.initialize(core.parameters.getHysteresisTime(), 
					core.parameters.getPersistenceTime(), 
					core.parameters.isEnabledBandsPersistence(),
					core.parameters.getAlertingParameterM(),
					core.parameters.getAlertingParameterN());
		} 
		bands_hysteresis_.resetIfCurrentTime(core.current_time);
		bands_hysteresis_.m_of_n(lcvs);			
		int idx = bands_hysteresis_.bandsPersistence(ranges_,lcvs,recovery,val);
		// Compute resolutions and preferred direction using persistence logic 
		bands_hysteresis_.bandsHysteresis(ranges_,core.parameters.getCorrectiveRegion(),
				max_delta_resolution(core.parameters),recovery_nfactor_,val,idx);
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
			return bands_hysteresis_.getLastResolutionUp();
		} else {
			return bands_hysteresis_.getLastResolutionLow();
		}
	}

	/** 
	 * Returns raw resolution maneuver (no hysteresis, no persistence).
	 * Return NaN if there is no conflict or if input is invalid.
	 * Return positive/negative infinity if there is no resolution to the 
	 * right/up and negative infinity if there is no resolution to the left/down.
	 */
	public double raw_resolution(DaidalusCore core, boolean dir) {
		refresh(core);
		if (dir) {
			return bands_hysteresis_.getRawResolutionUp();
		} else {
			return bands_hysteresis_.getRawResolutionLow();
		}
	}
	
	/**
	 * Compute preferred direction based on resolution that is closer
	 * to current value.
	 */
	public boolean preferred_direction(DaidalusCore core) {
		refresh(core);
		return bands_hysteresis_.getLastPreferredDirection();
	}

	/**
	 * Return last time to maneuver, in seconds, for ownship with respect to traffic
	 * aircraft at ac_idx for conflict alert level. Return NaN if the ownship is not 
	 * in conflict with aircraft within lookahead time. Return negative infinity if 
	 * there is no time to maneuver.
	 * Note: {@code 1 <= alert_level <= alerter.size()}
	 */
	public double last_time_to_maneuver(DaidalusCore core, TrafficState intruder) {
		int alert_idx = core.alerter_index_of(intruder);
		int alert_level = core.parameters.correctiveAlertLevel(alert_idx);
		if (set_input(core.parameters,core.ownship,core.DTAStatus()) && alert_level > 0) {
			AlertThresholds alertthr = core.parameters.getAlerterAt(alert_idx).getLevel(alert_level);
			Detection3D detector = alertthr.getCoreDetection();
			ConflictData det = detector.conflictDetectionWithTrafficState(core.ownship,intruder,0.0,core.parameters.getLookaheadTime());
			if (det.conflict()) {
				double pivot_red = det.getTimeIn();
				if (pivot_red == 0) {
					return Double.NEGATIVE_INFINITY;
				} 
				double pivot_green = 0;
				double pivot = pivot_green;    
				while ((pivot_red-pivot_green) > 0.5) {
					TrafficState ownship_at_pivot  = core.ownship.linearProjection(pivot); 
					TrafficState intruder_at_pivot = intruder.linearProjection(pivot);
					if (detector.violationAtWithTrafficState(ownship_at_pivot,intruder_at_pivot,0.0) ||
							all_red(detector,Detection3D.NoDetector,0,0,0.0,core.parameters.getLookaheadTime(),
									core.parameters,ownship_at_pivot,intruder_at_pivot)) {
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

	private int maxdown(DaidalusParameters parameters, TrafficState ownship) {
		int down = (int)Math.ceil(min_relative_/get_step(parameters));
		if (mod_ > 0 && Util.almost_greater(down*get_step(parameters),mod_/2.0,DaidalusParameters.ALMOST_)) {
			--down;
		}
		return down;
	}

	private int maxup(DaidalusParameters parameters, TrafficState ownship) {
		int up = (int)Math.ceil(max_relative_/get_step(parameters));
		if (mod_ > 0 && Util.almost_greater(up*get_step(parameters),mod_/2.0,DaidalusParameters.ALMOST_)) {
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
			lb = Util.max(min_val_,lb);
			ub = Util.min(max_val_,ub);
			if (Util.almost_less(lb,ub,DaidalusParameters.ALMOST_)) {
				noneset.almost_add(lb,ub,DaidalusParameters.ALMOST_);
			}
			return;
		}
		lb = Util.safe_modulo(lb,mod_);
		ub = Util.safe_modulo(ub,mod_);
		IntervalSet minmax_noneset = new IntervalSet();
		if (circular_) {
		} else if (min_val_ < max_val_) {
			minmax_noneset.almost_add(min_val_,max_val_,DaidalusParameters.ALMOST_);
		} else {
			minmax_noneset.almost_add(0,max_val_,DaidalusParameters.ALMOST_);
			minmax_noneset.almost_add(min_val_,mod_,DaidalusParameters.ALMOST_);
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
			if (ii.lb == ii.ub) {
				continue;
			}
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
			int epsh, int epsv, double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		List<Integerval> bands_int = new ArrayList<Integerval>();
		int mino = maxdown(parameters,ownship);
		int maxo = maxup(parameters,ownship);
		double tstep = instantaneous_bands(parameters) ?  0.0 : time_step(parameters,ownship);	
		integer_bands_combine(bands_int,conflict_det,recovery_det,tstep,
				B,T,mino,maxo,parameters,ownship,traffic,epsh,epsv);  
		toIntervalSet(noneset,bands_int,get_step(parameters),own_val(ownship));
	}

	public boolean any_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		int mino = maxdown(parameters,ownship);
		int maxo = maxup(parameters,ownship);
		double tstep = instantaneous_bands(parameters) ?  0.0 : time_step(parameters,ownship);	
		return any_integer_red(conflict_det,recovery_det,tstep,
				B,T,mino,maxo,parameters,ownship,traffic,epsh,epsv,0);
	}

	public boolean all_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		int mino = maxdown(parameters,ownship);
		int maxo = maxup(parameters,ownship);
		double tstep = instantaneous_bands(parameters) ?  0.0 : time_step(parameters,ownship);	
		return all_integer_red(conflict_det,recovery_det,tstep,
				B,T,mino,maxo,parameters,ownship,traffic,epsh,epsv,0);
	}

	public boolean all_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return !any_red(conflict_det,recovery_det,epsh,epsv,B,T,parameters,ownship,traffic);
	}

	public boolean any_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			int epsh, int epsv, double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return !all_red(conflict_det,recovery_det,epsh,epsv,B,T,parameters,ownship,traffic);
	}

	public String rawString() {
		String s = "";
		s+="# Private variables\n";
		s+="mod_ = "+f.FmPrecision(mod_)+"\n";
		s+="min_rel_ = "+f.FmPrecision(min_rel_)+"\n";
		s+="max_rel_ = "+f.FmPrecision(max_rel_)+"\n";
		s+="# Cached variables\n";
		s+="outdated_ = "+outdated_+"\n";		
		s+="checked_ = "+f.Fmi(checked_)+"\n";		
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_peripheral_bands_["+f.Fmi(conflict_region)+"] = "+
					IndexLevelT.toString(acs_peripheral_bands_.get(conflict_region))+"\n";
		}
		for (int conflict_region=0; conflict_region < BandsRegion.NUMBER_OF_CONFLICT_BANDS; ++conflict_region) {
			s+="acs_bands_["+f.Fmi(conflict_region)+"] = "+
					IndexLevelT.toString(acs_bands_.get(conflict_region))+"\n";
		}
		for (int i = 0; i < ranges_.size(); ++i) {
			s+="ranges_["+f.Fmi(i)+"] = ";
			s+=ranges_.get(i).toString()+"\n";
		} 
		s+="recovery_time_ = "+f.FmPrecision(recovery_time_)+"\n";
		s+="recovery_nfactor_ = "+f.Fmi(recovery_nfactor_)+"\n";
		s+="recovery_horizontal_distance_ = "+f.FmPrecision(recovery_horizontal_distance_)+"\n";
		s+="recovery_vertical_distance_ = "+f.FmPrecision(recovery_vertical_distance_)+"\n";
		s+="min_val_ = "+f.FmPrecision(min_val_)+"\n";
		s+="max_val_ = "+f.FmPrecision(max_val_)+"\n";
		s+="min_relative_ = "+f.FmPrecision(min_relative_)+"\n";
		s+="max_relative_ = "+f.FmPrecision(max_relative_)+"\n";
		s+="circular_ = "+circular_+"\n";
		s+=bands_hysteresis_.toString();
		return s;
	}

	public String toString() {
		String s = "";
		for (int i = 0; i < ranges_.size(); ++i) {
			s+="ranges["+f.Fmi(i)+"] = ";
			s+=ranges_.get(i).toString()+"\n";
		} 
		s+="recovery_time = "+f.FmPrecision(recovery_time_)+" [s]\n";
		s+="recovery_nfactor = "+f.Fmi(recovery_nfactor_)+"\n";
		s+="recovery_horizontal_distance = "+f.FmPrecision(recovery_horizontal_distance_)+ " [m]\n";
		s+="recovery_vertical_distance = "+f.FmPrecision(recovery_vertical_distance_)+ " [m]\n";
		s+="preferred_dir = "+bands_hysteresis_.getLastPreferredDirection()+"\n";
		s+="resolution_low = "+f.FmPrecision(bands_hysteresis_.getLastResolutionLow())+"\n";
		s+="resolution_up = "+f.FmPrecision(bands_hysteresis_.getLastResolutionUp())+"\n";
		return s;
	}

	public String toPVS() {
		String s = "(:";
		for (int i = 0; i < ranges_.size(); ++i) {
			if (i > 0) { 
				s+=", ";
			} else {
				s+=" ";
			}
			s += ranges_.get(i).toPVS();
		} 
		s+=" :)";
		return s;
	}

}
