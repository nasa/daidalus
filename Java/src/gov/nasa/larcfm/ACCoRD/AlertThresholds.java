/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.HashMap;
import java.util.Map;

import gov.nasa.larcfm.Util.ParameterAcceptor;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

public class AlertThresholds implements ParameterAcceptor, Detection3DAcceptor {

	private Detection3D detector_; // State-based detector  
	private double alerting_time_; // Alerting_time
	// If alerting_time > 0, alert is based on detection
	// If alerting_time = 0, alert is based on violation
	private double early_alerting_time_; // Early alerting time (for maneuver guidance). If zero, same as alerting_time
	private BandsRegion region_; // Guidance region for this alert
	private double spread_hdir_;  // Alert when direction band within spread (non-negative value)
	private double spread_hs_;   // Alert when horizontal speed band within spread (non-negative value)
	private double spread_vs_;   // Alert when vertical speed band within speed (non-negative value)
	private double spread_alt_;  // Alert when altitude  band within spread (non-negative value)
	private Map<String,String> units_;

	/** 
	 * Creates an alert threholds object. Parameter det is a detector,
	 * alerting_time is a non-negative alerting time (possibly positive infinity),
	 * early_alerting_time is a early alerting time {@code >=} at (for maneuver guidance),
	 * region is the type of guidance
	 */
	public AlertThresholds(Detection3D detector, double alerting_time, double early_alerting_time, 
			BandsRegion region) {
		detector_ = detector == null ? null : detector.copy();
		alerting_time_ = Math.abs(alerting_time);
		early_alerting_time_ = Util.max(alerting_time_,early_alerting_time);
		region_ = region;
		spread_hdir_ = 0;
		spread_hs_ = 0;
		spread_vs_ = 0;
		spread_alt_ = 0;
		units_ = new HashMap<String,String>();
		units_.put("alerting_time","s");
		units_.put("early_alerting_time","s");
		units_.put("spread_hdir","deg");
		units_.put("spread_hs","knot");
		units_.put("spread_vs","fpm");
		units_.put("spread_alt","ft");
	}

	public void copyFrom(AlertThresholds athr) {
		detector_ = athr.isValid() ? athr.detector_.copy() : null;
		alerting_time_ = athr.alerting_time_;
		early_alerting_time_ = athr.early_alerting_time_;
		region_ = athr.region_;
		spread_hdir_ = athr.spread_hdir_;
		spread_hs_ = athr.spread_hs_;
		spread_vs_ = athr.spread_vs_;
		spread_alt_ = athr.spread_alt_; 
		units_.clear();
		units_.putAll(athr.units_);
	}

	public AlertThresholds(AlertThresholds athr) {
		units_ = new HashMap<String,String>();
		copyFrom(athr);
	}

	public AlertThresholds() {
		detector_ = null;
		alerting_time_ = 0;
		early_alerting_time_ = 0;
		region_ = BandsRegion.UNKNOWN;
		spread_hdir_ = 0;
		spread_hs_ = 0;
		spread_vs_ = 0;
		spread_alt_ = 0;
		units_ = new HashMap<String,String>();
		units_.put("alerting_time","s");
		units_.put("early_alerting_time","s");
		units_.put("spread_hdir","deg");
		units_.put("spread_hs","m/s");
		units_.put("spread_vs","m/s");
		units_.put("spread_alt","m");
	}

	public static final AlertThresholds INVALID = new AlertThresholds();

	public boolean isValid() {
		return detector_ != null && region_ != BandsRegion.UNKNOWN;
	}

	/**
	 * Return detector.
	 */
	public Detection3D getCoreDetection() {
		return detector_;
	}

	/**
	 * Set detector.
	 */
	public void setCoreDetection(Detection3D det) { 
		detector_ = det != null ? det.copy() : null;
	}

	/**
	 * Return alerting time in seconds.
	 */
	public double getAlertingTime() {
		return alerting_time_;
	}

	/**
	 * Return alerting time in specified units.
	 */
	public double getAlertingTime(String u) {
		return Units.to(u,alerting_time_);
	}

	/**
	 * Set alerting time in seconds. Alerting time is non-negative number.
	 */
	public void setAlertingTime(double t) {
		alerting_time_ = Math.abs(t);
	}

	/**
	 * Set alerting time in specified units. Alerting time is non-negative number.
	 */
	public void setAlertingTime(double t, String u) {
		setAlertingTime(Units.from(u,t));
		units_.put("alerting_time",u);
	}

	/**
	 * Return early alerting time in seconds.
	 */
	public double getEarlyAlertingTime() {
		return early_alerting_time_;
	}

	/**
	 * Return early alerting time in specified units.
	 */
	public double getEarlyAlertingTime(String u) {
		return Units.to(u,early_alerting_time_);
	}

	/**
	 * Set early alerting time in seconds. Early alerting time is a positive number {@code >=} alerting time
	 */
	public void setEarlyAlertingTime(double t) {
		early_alerting_time_ = Math.abs(t);
	}

	/**
	 * Set early alerting time in specified units. Early alerting time is a positive number {@code >=} alerting time
	 */
	public void setEarlyAlertingTime(double t, String u) {
		setEarlyAlertingTime(Units.from(u,t));
		units_.put("early_alerting_time",u);
	}

	/**
	 * Return guidance region.
	 */
	public BandsRegion getRegion() {
		return region_;
	}

	/** 
	 * Set guidance region.
	 */
	public void setRegion(BandsRegion region) {
		region_ = region;
	}

	/**
	 * Get horizontal direction spread in internal units [rad]. Spread is relative to ownship's direction in the range [0,pi]
	 */
	public double getHorizontalDirectionSpread() {
		return spread_hdir_;
	}

	/**
	 * Get horizontal direction spread in given units [u]. Spread is relative to ownship's direction in the range [0,pi]
	 */
	public double getHorizontalDirectionSpread(String u) {
		return Units.to(u,spread_hdir_);
	}  

	/** 
	 * Set horizontal direction spread in internal units. Spread is relative to ownship's direction and is expected 
	 * to be in [0,pi].
	 */
	public void setHorizontalDirectionSpread(double spread) {
		spread_hdir_ = Math.abs(Util.to_pi(spread));
	}

	/** 
	 * Set direction spread in given units. Spread is relative to ownship's direction and is expected 
	 * to be in [0,pi] [u].
	 */
	public void setHorizontalDirectionSpread(double spread, String u) {
		setHorizontalDirectionSpread(Units.from(u,spread));
		units_.put("spread_hdir",u);
	}

	/**
	 * Get horizontal speed spread in internal units [m/s]. Spread is relative to ownship's horizontal speed
	 */
	public double getHorizontalSpeedSpread() {
		return spread_hs_;
	}

	/**
	 * Get horizontal speed spread in given units. Spread is relative to ownship's horizontal speed
	 */
	public double getHorizontalSpeedSpread(String u) {
		return Units.to(u,spread_hs_);
	}  

	/** 
	 * Set horizontal speed spread in internal units [m/s]. Spread is relative to ownship's horizontal speed and is expected 
	 * to be non-negative
	 */
	public void setHorizontalSpeedSpread(double spread) {
		spread_hs_ = Math.abs(spread);
	}

	/** 
	 * Set horizontal speed spread in given units. Spread is relative to ownship's horizontal speed and is expected 
	 * to be non-negative
	 */
	public void setHorizontalSpeedSpread(double spread, String u) {
		setHorizontalSpeedSpread(Units.from(u,spread));
		units_.put("spread_hs",u);
	}

	/**
	 * Get vertical speed spread in internal units [m/s]. Spread is relative to ownship's vertical speed
	 */
	public double getVerticalSpeedSpread() {
		return spread_vs_;
	}

	/**
	 * Get vertical speed spread in given units. Spread is relative to ownship's vertical speed
	 */
	public double getVerticalSpeedSpread(String u) {
		return Units.to(u,spread_vs_);
	}  

	/** 
	 * Set vertical speed spread in internal units [m/s]. Spread is relative to ownship's vertical speed and is expected 
	 * to be non-negative
	 */
	public void setVerticalSpeedSpread(double spread) {
		spread_vs_ = Math.abs(spread);
	}

	/** 
	 * Set vertical speed spread in given units. Spread is relative to ownship's vertical speed and is expected 
	 * to be non-negative
	 */
	public void setVerticalSpeedSpread(double spread, String u) {
		setVerticalSpeedSpread(Units.from(u,spread));
		units_.put("spread_vs",u);
	}

	/**
	 * Get altitude spread in internal units [m]. Spread is relative to ownship's altitude
	 */
	public double getAltitudeSpread() {
		return spread_alt_;
	}

	/**
	 * Get altitude spread in given units. Spread is relative to ownship's altitude
	 */
	public double getAltitudeSpread(String u) {
		return Units.to(u,spread_alt_);
	}  

	/** 
	 * Set altitude spread in internal units [m]. Spread is relative to ownship's altitude and is expected 
	 * to be non-negative
	 */
	public void setAltitudeSpread(double spread) {
		spread_alt_ = Math.abs(spread);
	}

	/** 
	 * Set altitude spread in given units. Spread is relative to ownship's altitude and is expected 
	 * to be non-negative
	 */
	public void setAltitudeSpread(double spread, String u) {
		setAltitudeSpread(Units.from(u,spread));
		units_.put("spread_alt",u);
	}

	public String toString() {
		return "volume = "+(detector_ == null ? "INVALID_DETECTOR" : detector_.toString())+
				", alerting_time = "+Units.str(getUnits("alerting_time"),alerting_time_)+
				", early_alerting_time = "+Units.str(getUnits("early_alerting_time"),early_alerting_time_)+
				", region = "+region_.toString()+
				", spread_hdir = "+Units.str(getUnits("spread_hdir"),spread_hdir_)+
				", spread_hs = "+Units.str(getUnits("spread_hs"),spread_hs_)+
				", spread_vs = "+Units.str(getUnits("spread_vs"),spread_vs_)+
				", spread_alt = "+Units.str(getUnits("spread_alt"),spread_alt_);
	}

	public String toPVS() {
		return "(# volume:= "+(detector_ == null ? "INVALID_DETECTOR" : detector_.toPVS())+
				", alerting_time:= "+f.FmPrecision(alerting_time_)+
				", early_alerting_time:= "+f.FmPrecision(early_alerting_time_)+
				", region:= "+region_+
				", spread_hdir:= ("+f.FmPrecision(spread_hdir_)+","+f.FmPrecision(spread_hdir_)+")"+
				", spread_hs:= ("+f.FmPrecision(spread_hs_)+","+f.FmPrecision(spread_hs_)+")"+
				", spread_vs:= ("+f.FmPrecision(spread_vs_)+","+f.FmPrecision(spread_vs_)+")"+
				", spread_alt:= ("+f.FmPrecision(spread_alt_)+","+f.FmPrecision(spread_alt_)+")"+
				" #)"; 
	}

	@Override
	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p;
	}

	@Override
	public void updateParameterData(ParameterData p) {
		p.set("region",region_.toString());
		p.setInternal("alerting_time",alerting_time_,getUnits("alerting_time"));
		p.setInternal("early_alerting_time",early_alerting_time_,getUnits("early_alerting_time"));
		p.setInternal("spread_hdir",spread_hdir_,getUnits("spread_hdir"));
		p.setInternal("spread_hs",spread_hs_,getUnits("spread_hs"));
		p.setInternal("spread_vs",spread_vs_,getUnits("spread_vs"));
		p.setInternal("spread_alt",spread_alt_,getUnits("spread_alt"));
		if (detector_ != null) {
			p.set("detector",detector_.getIdentifier());
		}
	}

	@Override
	public void setParameters(ParameterData p) {
		if (p.contains("region")) {
			setRegion(BandsRegion.valueOf(p.getString("region")));
		}
		if (p.contains("alerting_time")) {
			setAlertingTime(p.getValue("alerting_time"));
			units_.put("alerting_time",p.getUnit("alerting_time"));
		}
		if (p.contains("early_alerting_time")) {
			setEarlyAlertingTime(p.getValue("early_alerting_time"));
			units_.put("early_alerting_time",p.getUnit("early_alerting_time"));
		}
		if (p.contains("spread_hdir")) {
			setHorizontalDirectionSpread(p.getValue("spread_hdir"));
			units_.put("spread_hdir",p.getUnit("spread_hdir"));
		} else if (p.contains("spread_trk")) {
			setHorizontalDirectionSpread(p.getValue("spread_trk"));
			units_.put("spread_hdir",p.getUnit("spread_trk"));
		} 
		if (p.contains("spread_hs")) {
			setHorizontalSpeedSpread(p.getValue("spread_hs"));
			units_.put("spread_hs",p.getUnit("spread_hs"));
		} else if (p.contains("spread_gs")) {
			setHorizontalSpeedSpread(p.getValue("spread_gs"));
			units_.put("spread_hs",p.getUnit("spread_gs"));
		}
		if (p.contains("spread_vs")) {
			setVerticalSpeedSpread(p.getValue("spread_vs"));
			units_.put("spread_vs",p.getUnit("spread_vs"));
		}
		if (p.contains("spread_alt")) {
			setAltitudeSpread(p.getValue("spread_alt"));
			units_.put("spread_alt",p.getUnit("spread_alt"));
		}
	}

	public String getUnits(String key) {
		String u = units_.get(key);
		if (u == null) {
			return "unspecified";
		}
		return u;
	}

}
