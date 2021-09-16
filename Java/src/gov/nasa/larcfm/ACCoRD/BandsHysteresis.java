/*
 * Implementation of bands hysteresis logic that includes MofN and persistence.
 * Contact: Cesar A. Munoz
 * Organization: NASA/Langley Research Center
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.ArrayList;
import java.util.List;

import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class BandsHysteresis {

	private double  mod_;
	private double  mod2_;
	private double  hysteresis_time_;
	private double  persistence_time_;
	private boolean bands_persistence_;
	private double  last_time_;
	private int     m_;
	private int     n_;
	private List<BandsMofN> bands_mofn_; // Color values for computation of MofN bands logic	

	/* Parameters for hysteresis of preferred direction */
	private boolean preferred_dir_; // Returned preferred direction after hysteresis
	private double  time_of_dir_;   // Time of current preferred direction

	// Following variables are used in bands persistence
	private BandsRegion conflict_region_;  // Last conflict region
	private double conflict_region_low_; // If last region is conflict band, lower bound of conflict band
	private double conflict_region_up_;  // If last region is conflict band, upper bound of conflict band
	private double time_of_conflict_region_; // Time of last region

	/*
	 * resolution_up_,resolution_low_ are the resolution interval computed from all regions that are at least 
	 * as severe as the corrective region. Negative/positive infinity means no possible resolutions
	 * NaN means no resolutions are computed (either because there is no conflict or
	 * because of invalid inputs)
	 */
	private double resolution_up_;
	private double resolution_low_;
	private double raw_up_; // Resolution up without hysteresis
	private double raw_low_; // Resolution low without hysteresis
	// nfactor >= 0 means recovery bands. It's the reduction factor for the internal cylinder
	private int    nfactor_up_; 
	private int    nfactor_low_; 

	/* 
	 * Creates an empty object
	 */
	public BandsHysteresis() {
		mod_ = 0.0;
		mod2_ = 0.0;
		hysteresis_time_ = 0;
		persistence_time_ = 0;
		bands_persistence_ = false;
		m_ = 0;
		n_ = 0;
		bands_mofn_ = new ArrayList<BandsMofN>();
		reset();
	}

	public void setMod(double mod) {
		mod_ = mod;
		mod2_ = mod/2.0;
	}

	/*
	 * Sets hysteresis and persistence time
	 */
	public void initialize(double hysteresis_time, 
			double persistence_time, boolean bands_persistence,
			int m, int n) {
		hysteresis_time_ = hysteresis_time;
		persistence_time_ = persistence_time;
		bands_persistence_ = bands_persistence;
		m_ = m;
		n_ = n;
	}

	public double getLastTime() {
		return last_time_;
	}

	public boolean getLastPreferredDirection() {
		return preferred_dir_;
	}

	public double getLastResolutionLow() {
		return resolution_low_;
	}

	public double getLastResolutionUp() {
		return resolution_up_;
	}

	public double getRawResolutionLow() {
		return raw_low_;
	}

	public double getRawResolutionUp() {
		return raw_up_;
	}

	public int getLastNFactorLow() {
		return nfactor_low_;
	}

	public int getLastNFactorUp() {
		return nfactor_up_;
	}

	public List<BandsMofN> bands_mofn() {
		return bands_mofn_;
	}

	/*
	 * Reset object 
	 */
	public void reset() {
		bands_mofn_.clear();
		last_time_ = Double.NaN;

		preferred_dir_ = false;
		time_of_dir_ = Double.NaN;

		conflict_region_ = BandsRegion.UNKNOWN;
		conflict_region_low_ = Double.NaN;
		conflict_region_up_ = Double.NaN;
		time_of_conflict_region_ = Double.NaN;

		resolution_up_ = Double.NaN;
		resolution_low_ = Double.NaN;
		
		raw_up_ = Double.NaN;
		raw_low_ = Double.NaN;
		
		nfactor_up_ = 0;
		nfactor_low_ = 0;
	}

	/*
	 * Reset object if current time is older than hysteresis time with
	 * respect to last time. This method update last_time
	 */
	public void resetIfCurrentTime(double current_time) {
		if (Double.isNaN(last_time_) ||
				current_time <= last_time_ ||
				current_time-last_time_ > hysteresis_time_) { 	
			reset();	
		}
		last_time_ = current_time;
	}

	// Implement MofN logic for bands
	public void m_of_n(List<ColorValue> lcvs) {
		if (hysteresis_time_ > 0 && m_ > 0 && m_ <= n_) {
			if (bands_mofn_.isEmpty()) {
				// Initialize list of MofN values
				for(int j=0; j < lcvs.size(); ++j) {
					bands_mofn_.add(new BandsMofN(lcvs.get(j),m_,n_));
				}
			} 
			// Produce new list of color values in place
			int i = 0; // Index in bands_mofn_
			int j = 0; // Index in lcvs
			while (i < bands_mofn_.size() && j < lcvs.size()) {
				if (Util.almost_equals(bands_mofn_.get(i).val,lcvs.get(j).val,DaidalusParameters.ALMOST_)) {
					lcvs.get(j).color_left=bands_mofn_.get(i).left_m_of_n(lcvs.get(j).color_left);
					lcvs.get(j).color_right=bands_mofn_.get(i).right_m_of_n(lcvs.get(j).color_right);
					++i;
					++j;
				} else if (Util.almost_less(bands_mofn_.get(i).val,lcvs.get(j).val,DaidalusParameters.ALMOST_)) {
					lcvs.add(j,new ColorValue(lcvs.get(j).color_left,bands_mofn_.get(i).val,
							lcvs.get(j).color_left));
				} else {
					bands_mofn_.add(i,new BandsMofN(lcvs.get(j).val,
							bands_mofn_.get(i).colors_left));	
				}
			}
			// Remove values whose of mofns queues are the same both sides, except
			// those in the extremes
			i=1;
			while (i < bands_mofn_.size()-1) {
				if (bands_mofn_.get(i).same_colors()) {
					bands_mofn_.remove(i);
				} else {
					++i;
				}
			}
		}
	}

	private void conflict_region_persistence(List<BandsRange> ranges, int idx) {
		// Set conflict region for bands persistence
		if (0 <= idx && idx < ranges.size() && ranges.get(idx).region.isConflictBand()) {
			conflict_region_ = ranges.get(idx).region;
			if (mod_ == 0 || (0 < idx && idx < ranges.size()-1)) {
				conflict_region_low_ = ranges.get(idx).interval.low;
				conflict_region_up_ = ranges.get(idx).interval.up;
			} else if (idx == 0) {
				// In this case mod > 0
				conflict_region_up_ = ranges.get(0).interval.up;
				if (ranges.get(0).region ==
						ranges.get(ranges.size()-1).region) {
					conflict_region_low_ = ranges.get(ranges.size()-1).interval.low;
				} else {
					conflict_region_low_ = ranges.get(0).interval.low;
				}
			} else {
				// In this case mod > 0 && idx == ranges.size()-1
				conflict_region_low_ = ranges.get(idx).interval.low;
				if (ranges.get(0).region == ranges.get(idx).region) {
					conflict_region_up_ = ranges.get(0).interval.up;
				} else {
					conflict_region_up_ = ranges.get(idx).interval.up;
				}
			}
		} else {
			time_of_conflict_region_ = Double.NaN;
			conflict_region_ = BandsRegion.UNKNOWN;
			conflict_region_low_ = Double.NaN;
			conflict_region_up_ = Double.NaN;
		}		
	}

	// Implement persistence logic for bands. Return index in ranges of current value
	public int bandsPersistence(List<BandsRange> ranges, List<ColorValue> lcvs, boolean recovery,
			double val) {
		if (hysteresis_time_ > 0 && persistence_time_ > 0 && bands_persistence_) {
			// last_time is never NaN
			if (Double.isFinite(conflict_region_low_) && Double.isFinite(conflict_region_up_)) { 
				// current val is still between conflict_region_low and conflict_region_up
				BandsRegion current_region = ColorValue.region_of(lcvs,val);
				// In the following condition is important that RECOVERY is the higher value, 
				// since it overwrites all other colors. 
				// Therefore, orderOfRegion is used instead of orderOfConflictRegion.
				if (current_region.isValidBand() && conflict_region_.isConflictBand()) {
					if (!Double.isNaN(time_of_conflict_region_) && 
							BandsRegion.orderOfRegion(current_region) < BandsRegion.orderOfRegion(conflict_region_) &&
							last_time_ >= time_of_conflict_region_ &&
							last_time_-time_of_conflict_region_ < persistence_time_) {
						// Keep the previous conflict band (persistence logic prevails)
						ColorValue.insert_with_mod(lcvs,conflict_region_low_,conflict_region_up_,mod_,conflict_region_);
					} else {
						if (current_region.isConflictBand() &&
								BandsRegion.orderOfRegion(current_region) > BandsRegion.orderOfRegion(conflict_region_)) {
							time_of_conflict_region_ = last_time_;
						} 
					}
				}
			} else {
				time_of_conflict_region_ = last_time_;
			}
		}
		// Compute Color bands 
		BandsRange.makeRangesFromColorValues(ranges,lcvs,recovery);
		int idx = BandsRange.index_of(ranges,val,mod_);
		if (!Double.isNaN(time_of_conflict_region_)) {
			// Persistence is enabled
			conflict_region_persistence(ranges,idx);
		}
		return idx;
	}

	// check if region is below corrective region  
	private static boolean is_below_corrective_region(BandsRegion corrective_region, BandsRegion region) {
		// In the following core is important that RECOVERY and NONE both have the order 0, 
		// since this function is used to find a resolution.
		// For that reason, orderOfConflictRegion is used instead of orderOfRegion
		return region.isValidBand() && 
				region.orderOfConflictRegion() < 
				corrective_region.orderOfConflictRegion();
	}

	// check if regions from idx_from to idx_to are resolutions
	private boolean contiguous_resolution_region(List<BandsRange> ranges, 
			BandsRegion corrective_region, boolean dir, int idx_from, int idx_to) {
		int idx=idx_from; 
		while (idx != idx_to && 0 <= idx && idx < ranges.size() &&
				is_below_corrective_region(corrective_region,ranges.get(idx).region)) {
			if (dir) {
				++idx;
				if (mod_ > 0 &&  idx == ranges.size()) {
					idx=0;
				}
			} else {
				--idx;
				if (mod_ > 0 && idx == -1) {
					idx = ranges.size()-1;
				}
			}
		}
		return 0 <= idx && idx < ranges.size() &&
				is_below_corrective_region(corrective_region,ranges.get(idx).region);
	}

	// Returns true if a is to the left of b (modulo mod). If mod is 0, this is the same a < b
	// Note that, by definition, if a is almost equal to b, then a is to the left of b.
	private boolean to_the_left(double a, double b) {
		if (Util.almost_equals(a,b,DaidalusParameters.ALMOST_)) {
			return true;
		}
		if (mod_ == 0) {
			return Util.almost_less(a,b,DaidalusParameters.ALMOST_);
		} else {
			return Util.almost_less(Util.modulo(b-a,mod_),mod2_,DaidalusParameters.ALMOST_);
		}	
	}

	public void resolutionsHysteresis(List<BandsRange> ranges, 
			BandsRegion corrective_region, double delta, int nfactor, 
			double val, int idx_l, int idx_u) {
		// last_time is never NaN
		if (hysteresis_time_ <= 0 || Double.isNaN(time_of_dir_) || 
				last_time_ <= time_of_dir_ || delta <= 0) {
			// No hysteresis at this time
			resolution_low_ = raw_low_;
			nfactor_low_ = nfactor;
			resolution_up_ = raw_up_;
			nfactor_up_ = nfactor;
			time_of_dir_ = Double.NaN;
		} else {
			// Make sure that old resolutions are still valid. If not, reset them.
			// persistence of up/right resolutions
			int idx = -1;
			if (Double.isFinite(raw_up_) && // Exists a current resolution
					Double.isFinite(resolution_up_) && // Previous resolution exists 
					// Previous resolution is in the same direction as new one
					to_the_left(val,resolution_up_) &&
					// Before persistence time or nfactor is greater or within delta of new resolution
					(last_time_-time_of_dir_ < persistence_time_ || 
							nfactor_up_ < nfactor ||
							Util.almost_less(Util.safe_modulo(resolution_up_-raw_up_,mod_),delta,
									DaidalusParameters.ALMOST_))) {				
				idx=BandsRange.index_of(ranges,resolution_up_,mod_);
			}	
			if (0 <= idx && idx < ranges.size() && 
					contiguous_resolution_region(ranges,corrective_region,preferred_dir_,
							idx_u,idx)) {
				// Do nothing: keep old up/right resolution
			} else {
				resolution_up_ = raw_up_;
				nfactor_up_ = nfactor;
			}
			// persistence of low/left resolutions
			idx = -1;
			if (Double.isFinite(raw_low_) && // Exists a current resolution
					Double.isFinite(resolution_low_) && // Previous resolution exists 
					// Previous resolution is in the same direction as new one
					to_the_left(resolution_low_,val) &&
					// Before persistence time or nfactor is greater or within delta of new resolution
					(last_time_-time_of_dir_ < persistence_time_ ||
							nfactor_low_ < nfactor ||
							Util.almost_less(Util.safe_modulo(raw_low_-resolution_low_,mod_),delta,
									DaidalusParameters.ALMOST_))) {				
				idx=BandsRange.index_of(ranges,resolution_low_,mod_);
			}	
			if (0 <= idx && idx < ranges.size() && 
					contiguous_resolution_region(ranges,corrective_region,preferred_dir_,
							idx_l,idx)) {
				// Do nothing: keep old low/left resolution
			} else {
				resolution_low_ = raw_low_;
				nfactor_low_ = nfactor;
			}
		}
	}

	private void switch_dir(boolean dir, double nfactor) {
		preferred_dir_ = dir;
		time_of_dir_ = last_time_;
		if (nfactor <= nfactor_low_) {
			resolution_low_ = raw_low_;
		}
		if (nfactor <= nfactor_up_) {
			resolution_up_ = raw_up_;
		}		
	}

	public void preferredDirectionHysteresis(double delta, double nfactor, double val) {
		if (!Double.isFinite(raw_up_) && !Double.isFinite(raw_low_)) {
			time_of_dir_ = Double.NaN;
			preferred_dir_ = false;
		} else if (!Double.isFinite(raw_up_)) {
			if (preferred_dir_) {
				switch_dir(false,nfactor);
			}
		} else if (!Double.isFinite(raw_low_)) {
			if (!preferred_dir_) {
				switch_dir(true,nfactor);
			}
		} else {
			double mod_up = Util.safe_modulo(raw_up_-val,mod_);
			double mod_down = Util.safe_modulo(val-raw_low_,mod_);
			boolean actual_dir = Util.almost_leq(mod_up,mod_down,DaidalusParameters.ALMOST_);
			if (hysteresis_time_ <= 0 || Double.isNaN(time_of_dir_) || 
					last_time_ < time_of_dir_ || delta <= 0) {
				preferred_dir_ = actual_dir;			
				time_of_dir_ = last_time_;
			} else if (last_time_-time_of_dir_ < persistence_time_ ||
					Math.abs(mod_up-mod_down) < delta) {
				// Keep the previous direction (persistence logic prevails)
			} else if ((preferred_dir_ && nfactor_low_ > nfactor_up_) ||
					(!preferred_dir_ && nfactor_up_ > nfactor_low_)) {
				// Keep the previous direction (do not change to a greater nfactor)
			} else if (preferred_dir_ != actual_dir) {
				// Change direction, update time_of_dir
				switch_dir(actual_dir,nfactor);
			}
		} 
	}

	// check if region is above corrective region (corrective or above)
	private static boolean is_up_from_corrective_region(BandsRegion corrective_region, BandsRegion region) {
		// In the following core is important that RECOVERY and NONE both have the order 0, 
		// since this function is used to find a resolution.
		// For that reason, orderOfConflictRegion is used instead of orderOfRegion
		return region.isConflictBand() && 
				region.orderOfConflictRegion() >= 
				corrective_region.orderOfConflictRegion();
	}

	public void bandsHysteresis(List<BandsRange> ranges, 
			BandsRegion corrective_region,
			double delta, int nfactor, double val, int idx) {
		raw_low_ = Double.NaN;
		raw_up_ = Double.NaN;
		int idx_l = idx;
		int idx_u = idx;
		// Find actual resolutions closest to current value
		if (0 <= idx && idx < ranges.size() && is_up_from_corrective_region(corrective_region,ranges.get(idx).region)) {
			// There is a conflict
			int last_index = ranges.size()-1;
			// Find low/left resolution 
			raw_low_ = Double.NEGATIVE_INFINITY;
			while (idx_l >= 0 && is_up_from_corrective_region(corrective_region,ranges.get(idx_l).region)) {
				if (to_the_left(ranges.get(idx_l).interval.low,val)) {
					//if (Util.almost_less(Util.safe_modulo(val-ranges.get(idx_l).interval.low,mod),min_relative,
					//		DaidalusParameters.ALMOST_)) {
					if (idx_l == 0 && mod_ > 0) {
						idx_l = last_index;
					} else {
						--idx_l;
					}
				} else {
					idx_l = -1;
				}
				if (idx_l == idx) {
					// Already went around
					idx_l = -1;
				}
			}
			if (idx_l >= 0 && ranges.get(idx_l).region.isValidBand()) {
				if (idx_l == last_index && mod_ > 0) {
					raw_low_ = 0;
				} else {
					raw_low_ = ranges.get(idx_l).interval.up;
				}	
			} 
			// Find up/right resolution 
			raw_up_ = Double.POSITIVE_INFINITY;
			while (idx_u <= last_index && is_up_from_corrective_region(corrective_region,ranges.get(idx_u).region)) {
				if (to_the_left(val,ranges.get(idx_u).interval.up)) {
					//if (Util.almost_less(Util.safe_modulo(ranges.get(idx_u).interval.up-val,mod),max_relative,
					//		DaidalusParameters.ALMOST_)) {
					if (idx_u == last_index && mod_ > 0) {
						idx_u = 0;
					} else {
						++idx_u;
					}
				} else {
					idx_u = last_index+1;
				}
				if (idx_u == idx) {
					// Already went around
					idx_u = last_index+1;
				}
			}
			if (idx_u <= last_index && ranges.get(idx_u).region.isValidBand()) {
				raw_up_ = ranges.get(idx_u).interval.low;
			}
		}
		resolutionsHysteresis(ranges,corrective_region,delta,nfactor,val,idx_l,idx_u);
		preferredDirectionHysteresis(delta,nfactor,val);
	}

	public String toString() {
		String s = "# Hysteresis variables\n";
		s += "hysteresis_time_ = "+f.FmPrecision(hysteresis_time_)+"\n";
		s += "persistence_time_ = "+f.FmPrecision(persistence_time_)+"\n";
		s += "bands_persistence_ = "+bands_persistence_+"\n";
		s += "last_time_ = "+f.FmPrecision(last_time_)+"\n";
		s += "m_ = "+f.Fmi(m_)+"\n";
		s += "n_ = "+f.Fmi(n_)+"\n";
		for (int i = 0; i < bands_mofn_.size(); ++i) {
			s+="bands_mofn_["+f.Fmi(i)+"] = ";
			s+=bands_mofn_.get(i).toString()+"\n";
		} 
		s += "preferred_dir_ = "+preferred_dir_+"\n";
		s += "time_of_dir_ = "+f.FmPrecision(time_of_dir_)+"\n";
		s += "resolution_low_ = "+f.FmPrecision(resolution_low_)+"\n";
		s += "resolution_up_ = "+f.FmPrecision(resolution_up_)+"\n";
		s += "raw_low_ = "+f.FmPrecision(raw_low_)+"\n";
		s += "raw_up_ = "+f.FmPrecision(raw_up_)+"\n";
		s += "nfactor_low_ = "+f.Fmi(nfactor_low_)+"\n";
		s += "nfactor_up_ = "+f.Fmi(nfactor_up_)+"\n";
		s += "conflict_region_ = "+conflict_region_.toString()+"\n";
		s += "conflict_region_low_ = "+f.FmPrecision(conflict_region_low_)+"\n";
		s += "conflict_region_up_ = "+f.FmPrecision(conflict_region_up_)+"\n";
		s += "time_of_conflict_region_ = "+f.FmPrecision(time_of_conflict_region_)+"\n";
		return s;
	}

}
