/*
 * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.f;

/**
 * TCAS table with user-defined numbers of sensitivity levels.
 * 
 * Note: this new version should be mostly compatible with the previous version from an API standpoint, 
 * with the exception of the getSensitivityLevel() methods.  These had previously been static and are 
 * now instance methods.  New methods allow users to define an arbitrary number of sensitivity levels, not
 * just 8. 
 * * 
 */
public class TCASTable implements ParameterTable {

	/* Default levels in internal units */
	static private double[] default_levels_ = {
			Units.from(Units.ft,0),     // Upper bound of SL 1
			Units.from(Units.ft,1000),  // Upper bound of SL 2
			Units.from(Units.ft,2350),  // Upper bound of SL 3
			Units.from(Units.ft,5000),  // Upper bound of SL 4
			Units.from(Units.ft,10000), // Upper bound of SL 5
			Units.from(Units.ft,20000), // Upper bound of SL 6
			Units.from(Units.ft,42000)  // Upper bound of SL 7
	}; // Note that this array has only 7 entries.  
	//It is understood that there is one additional level with an infinite upper bound.

	/* TA TAU Threshold in seconds *///1  2  3  4  5  6  7  8 (0 i N/A)
	static private double[] TA_TAU_ = {0,20,25,30,40,45,48,48};

	/* RA TAU Threshold in seconds *///1  2  3  4  5  6  7  8 (0 is N/A)
	static private double[] RA_TAU_ = {0,0,15,20,25,30,35,35};

	/* TA DMOD in internal units (0 if N/A) */
	static private double[] TA_DMOD_ = {
			0, 									                     // SL 1
			Units.from(Units.NM,0.30),   				     // SL 2
			Units.from(Units.NM,0.33),   				     // SL 3
			Units.from(Units.NM,0.48),   				     // SL 4
			Units.from(Units.NM,0.75),   				     // SL 5
			Units.from(Units.NM,1.0),   				     // SL 6
			Units.from(Units.NM,1.3),   				     // SL 7
			Units.from(Units.NM,1.3)};   				     // SL 8

	/* RA DMOD in internal units (0 if N/A) */
	static private double[] RA_DMOD_ = {
			0,								                      // SL 1
			0,    				                    			// SL 2
			Units.from(Units.NM,0.2),               // SL 3
			Units.from(Units.NM,0.35),              // SL 4
			Units.from(Units.NM,0.55),              // SL 5
			Units.from(Units.NM,0.8),               // SL 6
			Units.from(Units.NM,1.1),               // SL 7
			Units.from(Units.NM,1.1)};              // SL 8

	/* TA ZTHR in internal units (0 if N/A) */
	static private double[] TA_ZTHR_ = {
			0,               						            // SL 1
			Units.from(Units.ft,850),               // SL 2
			Units.from(Units.ft,850),               // SL 3
			Units.from(Units.ft,850),               // SL 4
			Units.from(Units.ft,850),               // SL 5
			Units.from(Units.ft,850),               // SL 6
			Units.from(Units.ft,850),               // SL 7
			Units.from(Units.ft,1200)};             // SL 8

	/* RA ZTHR in internal units (0 if N/A) */
	static private double[] RA_ZTHR_ = {
			0,    									                // SL 1
			0,    									                // SL 2
			Units.from(Units.ft,600),               // SL 3
			Units.from(Units.ft,600),               // SL 4
			Units.from(Units.ft,600),               // SL 5
			Units.from(Units.ft,600),               // SL 6
			Units.from(Units.ft,700),               // SL 7
			Units.from(Units.ft,800)};              // SL 8

	/* RA HMD in internal units (0 if N/A) */
	static private double[] RA_HMD_ = {
			0,									                    // SL 1
			0,									                    // SL 2
			Units.from(Units.ft,1215),              // SL 3
			Units.from(Units.ft,2126),              // SL 4
			Units.from(Units.ft,3342),              // SL 5
			Units.from(Units.ft,4861),              // SL 6
			Units.from(Units.ft,6683),              // SL 7
			Units.from(Units.ft,6683)};             // SL 8

	private Map<String,String> units_;
	private boolean HMDFilter_;
	private List<Double> TAU_;  
	private List<Double> TCOA_;  
	private List<Double> DMOD_;  
	private List<Double> ZTHR_;
	private List<Double> HMD_;
	private List<Double> levels_; // this is the upper end for each level, indexed from 1.
	// This list has one less element than the other lists.

	private void add_zeros() {
		DMOD_.add(0.0);
		HMD_.add(0.0);
		ZTHR_.add(0.0);
		TAU_.add(0.0);
		TCOA_.add(0.0);		
	}

	private void default_units() {
		units_.put("TCAS_DMOD","nmi");
		units_.put("TCAS_HMD","ft");
		units_.put("TCAS_ZTHR","ft");
		units_.put("TCAS_TAU","s");
		units_.put("TCAS_TCOA","s");
		units_.put("TCAS_level","ft");
	}

	// Returns an empty TCASTable
	private TCASTable() {
		HMDFilter_ = false;
		levels_ = new ArrayList<Double>(7);  
		TAU_ = new ArrayList<Double>(8);  
		TCOA_ = new ArrayList<Double>(8);  
		DMOD_ = new ArrayList<Double>(8);  
		ZTHR_ = new ArrayList<Double>(8);
		HMD_ = new ArrayList<Double>(8);
		units_ = new HashMap<String,String>();
		add_zeros();
		default_units();
	}

	// TCASII RA Table
	public final static TCASTable TCASII_RA = make_TCASII_Table(true);

	// TCASII RA Table
	public final static TCASTable TCASII_TA = make_TCASII_Table(false);

	public static TCASTable make_TCASII_Table(boolean ra) {
		TCASTable tcas_table = new TCASTable();
		tcas_table.setDefaultTCASIIThresholds(ra);
		return tcas_table;
	}

	/** Make empty TCASTable
	 * 
	 * @return  This returns a zeroed table with one unbounded level.
	 * That level has value 0 for DMOD,HMD,ZTHR,TAUMOD,TCOA
	 */
	public static TCASTable make_Empty_TCASTable() {
		TCASTable tcas_table = new TCASTable();
		return tcas_table;
	}

	// Clear all inputs in TCASTable
	public void clear() {
		levels_.clear();
		TAU_.clear();  
		TCOA_.clear();  
		DMOD_.clear();  
		ZTHR_.clear();
		HMD_.clear();	
		add_zeros();
		default_units();
	}

	/** Copy constructor */
	public TCASTable(TCASTable t) {
		levels_ = new ArrayList<Double>(7);  
		TAU_ = new ArrayList<Double>(8);  
		TCOA_ = new ArrayList<Double>(8);  
		DMOD_ = new ArrayList<Double>(8);  
		ZTHR_ = new ArrayList<Double>(8);
		HMD_ = new ArrayList<Double>(8);
		HMDFilter_ = t.HMDFilter_;
		levels_.addAll(t.levels_);
		TAU_.addAll(t.TAU_);
		TCOA_.addAll(t.TCOA_);
		DMOD_.addAll(t.DMOD_);
		HMD_.addAll(t.HMD_);
		ZTHR_.addAll(t.ZTHR_);
		units_ = new HashMap<String,String>();
		units_.putAll(t.units_);
	}

	public TCASTable copy() {
		return new TCASTable(this);
	}

	/** Set to this object a copy of parameter */
	public void set(TCASTable t) {
		levels_.clear();
		TAU_.clear();  
		TCOA_.clear();  
		DMOD_.clear();  
		ZTHR_.clear();
		HMD_.clear();	
		HMDFilter_ = t.HMDFilter_;
		levels_.addAll(t.levels_);
		TAU_.addAll(t.TAU_);
		TCOA_.addAll(t.TCOA_);
		DMOD_.addAll(t.DMOD_);
		HMD_.addAll(t.HMD_);
		ZTHR_.addAll(t.ZTHR_);
		units_.putAll(t.units_);
	}

	/** 
	 * Return sensitivity level from alt, specified in internal units.
	 * Sensitivity levels are indexed from 1.
	 */
	public int getSensitivityLevel(double alt) {
		int i;
		for (i = 0; i < levels_.size(); i++) {
			if (alt <= levels_.get(i)) {
				return i+1;
			}
		}
		return i+1; // Returns last sensitivity level
	}

	/** Return sensitivity level from alt specified in u units */
	public int getSensitivityLevel(double alt, String u) {
		return getSensitivityLevel(Units.from(u,alt));
	}

	/**
	 * Return true if the sensitivity level is between 1 and levels.size().
	 */
	public boolean isValidSensitivityLevel(int sl) {
		return 1 <= sl && sl <= levels_.size()+1 && 
				(sl > levels_.size() || levels_.size() != 0.0);
	}

	/**
	 * Returns the maximum defined sensitivity level (indexed from 1).
	 */
	public int getMaxSensitivityLevel() {
		return levels_.size()+1;
	}

	/**
	 * Returns altitude lower bound for a given sensitivity level sl, in internal units.
	 * Note this is an open bound (sl is valid for altitudes strictly greater than the return value)
	 * This returns a negative value if an invalid level is input.
	 */
	public double getLevelAltitudeLowerBound(int sl) {
		if (isValidSensitivityLevel(sl)) { 
			for (--sl; sl > 0 && levels_.get(sl-1) == 0.0; --sl); 
			if (sl > 0) {
				return levels_.get(sl-1);
			} else {
				return 0;
			}
		}
		return -1;
	}

	/**
	 * Returns altitude lower bound for a given sensitivity level sl, in given units.
	 * Note this is an open bound (sl is valid for altitudes strictly greater than the return value)
	 * This returns a negative value if an invalid level is input.
	 */
	public double getLevelAltitudeLowerBound(int sl, String u) {
		return Units.to(u,getLevelAltitudeLowerBound(sl));
	}

	/**
	 * Returns altitude upper bound for a given sensitivity level sl, in internal units.
	 * Note this is a closed bound (sl is valid for altitudes less than or equal to the return value)
	 * This returns a negative value if an invalid level is input.
	 */
	public double getLevelAltitudeUpperBound(int sl) {
		if (isValidSensitivityLevel(sl)) { 
			if (sl == getMaxSensitivityLevel()) {
				return Double.POSITIVE_INFINITY;
			} else {
				return levels_.get(sl-1);
			}
		}
		return -1;
	}

	/**
	 * Returns altitude upper bound for a given sensitivity level sl, in given units.
	 * Note this is a closed bound (sl is valid for altitudes less than or equal to the return value)
	 * This returns a negative value if an invalid level is input.
	 */
	public double getLevelAltitudeUpperBound(int sl, String u) {
		return Units.to(u,getLevelAltitudeUpperBound(sl));
	}

	private void setTCASIILevels() {
		clear();
		for (int i=0; i < 7; ++i) {
			addSensitivityLevel(default_levels_[i]);
		}
	}

	/**
	 * Set table to TCASII Thresholds (RA Table when ra is true, TA Table when ra is false)
	 */
	public void setDefaultTCASIIThresholds(boolean ra) {
		HMDFilter_ = ra;
		setTCASIILevels();
		for (int i=0; i < 8; ++i) {
			if (ra) {
				TAU_.set(i,RA_TAU_[i]);
				TCOA_.set(i,RA_TAU_[i]);
				DMOD_.set(i,RA_DMOD_[i]);
				ZTHR_.set(i,RA_ZTHR_[i]); 
				HMD_.set(i,RA_HMD_[i]);
			} else {
				TAU_.set(i,TA_TAU_[i]);
				TCOA_.set(i,TA_TAU_[i]);
				DMOD_.set(i,TA_DMOD_[i]);
				ZTHR_.set(i,TA_ZTHR_[i]); 
				HMD_.set(i,TA_DMOD_[i]);
			}
		}
		default_units();
	}

	/**
	 * Returns TAU threshold for sensitivity level sl in seconds.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getTAU(int sl)  {
		if (isValidSensitivityLevel(sl)) {
			return TAU_.get(sl-1);
		}
		return -1;
	}

	/**
	 * Returns TCOA threshold for sensitivity level sl in seconds
	 * This returns a negative value if an invalid level is input.
	 */
	public double getTCOA(int sl)  {
		if (isValidSensitivityLevel(sl)) {
			return TCOA_.get(sl-1);
		}
		return -1;
	}

	/**
	 * Returns DMOD for sensitivity level sl in internal units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getDMOD(int sl)  {
		if (isValidSensitivityLevel(sl)) { 
			return DMOD_.get(sl-1);
		}
		return -1;
	}

	/**
	 * Returns DMOD for sensitivity level sl in u units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getDMOD(int sl, String u)  {
		return Units.to(u,getDMOD(sl));
	}

	/**
	 * Returns Z threshold for sensitivity level sl in internal units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getZTHR(int sl)  {
		if (isValidSensitivityLevel(sl)) { 
			return ZTHR_.get(sl-1);
		}
		return -1;
	}

	/**
	 * Returns Z threshold for sensitivity level sl in u units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getZTHR(int sl,String u)  {
		return Units.to(u,getZTHR(sl));
	}

	/**
	 * Returns HMD for sensitivity level sl in internal units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getHMD(int sl)  {
		if (isValidSensitivityLevel(sl)) { 
			return HMD_.get(sl-1);
		}
		return -1;
	}

	/**
	 * Returns HMD for sensitivity level sl in u units.
	 * This returns a negative value if an invalid level is input.
	 */
	public double getHMD(int sl, String u)  {
		return Units.to(u,getHMD(sl));
	}

	/** Modify the value of Tau Threshold for a given sensitivity level 
	 * Parameter val is given in seconds. Val is {@code >= 0}. Return true is value was set.
	 */
	public boolean setTAU(int sl, double val) {
		if (isValidSensitivityLevel(sl)) { 
			TAU_.set(sl-1,Util.max(0,val));
			return true;
		}
		return false;
	}

	/** Modify the value of Tau Threshold for a given sensitivity level 
	 * Parameter val is given in given units. Val is {@code >= 0}
	 */
	public void setTAU(int sl, double val, String u) {
		if (setTAU(sl,Units.from(u,val))) {
			units_.put("TCAS_TAU",u);		
		}
	}

	/** Modify the value of TCOA Threshold for a given sensitivity level
	 * Parameter val is given in seconds. Val is {@code >= 0}. Return true is value was set.
	 */
	public boolean setTCOA(int sl, double val) {
		if (isValidSensitivityLevel(sl)) { 
			TCOA_.set(sl-1,Util.max(0,val));
			return true;
		}
		return false;
	}

	/** Modify the value of TCOA Threshold for a given sensitivity level 
	 * Parameter val is given in given units. Val is {@code >= 0}
	 */
	public void setTCOA(int sl, double val, String u) {
		if (setTCOA(sl,Units.from(u,val))) {
			units_.put("TCAS_TCOA",u);		
		}	
	}

	/** Modify the value of DMOD for a given sensitivity level 
	 * Parameter val is given in internal units. Val is {@code >= 0}. Return true is value was set.
	 */
	public boolean setDMOD(int sl, double val) { 
		if (isValidSensitivityLevel(sl)) { 
			DMOD_.set(sl-1,Util.max(0,val));
			return true;
		}
		return false;
	}

	/** Modify the value of DMOD for a given sensitivity level 
	 * Parameter val is given in u units. Val is {@code >= 0}.
	 */
	public void setDMOD(int sl, double val, String u) { 
		if (setDMOD(sl,Units.from(u,val))) {
			units_.put("TCAS_DMOD",u);				
		}
	}

	/** Modify the value of ZTHR for a given sensitivity level 
	 * Parameter val is given in internal units. Val is {@code >= 0}. Return true is value was set.
	 */
	public boolean setZTHR(int sl, double val) {
		if (isValidSensitivityLevel(sl)) { 
			ZTHR_.set(sl-1,Util.max(0,val));
			return true;
		}
		return false;
	}

	/** Modify the value of ZTHR for a given sensitivity level 
	 * Parameter val is given in u units. Val is {@code >= 0}.
	 */
	public void setZTHR(int sl, double val, String u) {
		if (setZTHR(sl,Units.from(u,val))) {
			units_.put("TCAS_ZTHR",u);							
		}
	}

	/** 
	 * Modify the value of HMD for a given sensitivity level 
	 * Parameter val is given in internal units. Val is {@code >= 0}. Return true is value was set.
	 */
	public boolean setHMD(int sl, double val) {
		if (isValidSensitivityLevel(sl)) { 
			HMD_.set(sl-1,Util.max(0,val));
			return true;
		}
		return false;
	}

	/** 
	 * Modify the value of HMD for a given sensitivity level 
	 * Parameter val is given in u units. Val is {@code >= 0}.
	 */
	public void setHMD(int sl, double val, String u) {
		if (setHMD(sl,Units.from(u,val))) {
			units_.put("TCAS_HMD",u);									
		}
	}

	/** 
	 * Add sensitivity level with upper bound altitude alt (in internal units).
	 * Requires: {@code alt > levels_.get(size(levels_)-1)} or an empty table
	 * Add value 0 to DMOD,HMD,ZTHR,TAUMOD,TCOA
	 * Either returns index of new maximum sensitivity level or 0 (if requires is false) 
	 */
	public int addSensitivityLevel(double alt) {
		if (levels_.isEmpty() || alt > levels_.get(levels_.size()-1)) {
			levels_.add(alt);
			add_zeros();
			return levels_.size()+1;
		}
		return 0;
	}

	/** 
	 * Add empty sensitivity level 
	 * Add value 0 to DMOD,HMD,ZTHR,TAUMOD,TCOA
	 * Either returns index of new maximum sensitivity level 
	 */
	public int addSensitivityLevel() {
		levels_.add(0.0);
		add_zeros();
		return levels_.size()+1;
	}

	/** 
	 * Add sensitivity level with upper bound altitude alt (in given units).
	 * Requires: {@code alt > levels_.get(size(levels_)-1)} or an empty table
	 * Add value 0 to DMOD,HMD,ZTHR,TAUMOD,TCOA
	 * Either returns new sensitivity level or 0 (if requires is false) 
	 */
	public int addSensitivityLevel(double alt,String u) {
		int sl=addSensitivityLevel(Units.from(u,alt));
		if (sl > 0) {
			units_.put("TCAS_level",u);
		}
		return sl;
	}

	public void setHMDFilter(boolean flag) {
		HMDFilter_ = flag;
	}

	public boolean getHMDFilter() {
		return HMDFilter_;
	}

	/** Return true if the values in the table correspond to the standard RA values */
	public boolean isRAStandard() {
		if (levels_.size() != 7) return false;
		boolean ra = HMDFilter_;
		for (int i=0; ra && i < 8; ++i) {
			ra &= (i == 7 || Util.almost_equals(levels_.get(i),default_levels_[i],DaidalusParameters.ALMOST_)) && 
					Util.almost_equals(TAU_.get(i),RA_TAU_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(TCOA_.get(i),RA_TAU_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(DMOD_.get(i),RA_DMOD_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(ZTHR_.get(i),RA_ZTHR_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(HMD_.get(i),RA_HMD_[i],DaidalusParameters.ALMOST_);
		}
		return ra;
	}

	/** Return true if the values in the table correspond to the standard TA values */
	public boolean isTAStandard() {
		boolean ta = !HMDFilter_;
		if (levels_.size() != 7) return false;
		for (int i=0; ta && i < 8; ++i) {
			ta &= (i == 7 || Util.almost_equals(levels_.get(i),default_levels_[i],DaidalusParameters.ALMOST_)) && 
					Util.almost_equals(TAU_.get(i),TA_TAU_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(TCOA_.get(i),TA_TAU_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(DMOD_.get(i),TA_DMOD_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(ZTHR_.get(i),TA_ZTHR_[i],DaidalusParameters.ALMOST_) && 
					Util.almost_equals(HMD_.get(i),TA_DMOD_[i],DaidalusParameters.ALMOST_);
		}
		return ta;
	}

	private String list_units(String units, List<Double> v) {
		String s="";
		boolean comma = false;
		for (int i=0; i<v.size(); ++i) {
			if (comma) {
				s+=", ";
			} else {
				comma = true;
			}
			s+=Units.str(units,v.get(i));
		}
		return s;
	}

	public String toString() {
		String s = "HMDFilter: "+HMDFilter_;
		if (isRAStandard()) {
			s = s+"; (RA vals) ";
		} else if (isTAStandard()) {
			s = s+"; (TA vals) ";
		}
		//TODO: made this next bit optional?
		s= s+"; levels: "+list_units(units_.get("TCAS_level"),levels_)+
				"; TAU: "+list_units(units_.get("TCAS_TAU"),TAU_)+"; TCOA: "+list_units(units_.get("TCAS_TCOA"),TCOA_)+
				"; DMOD: "+list_units(units_.get("TCAS_DMOD"),DMOD_)+"; ZTHR: "+list_units(units_.get("TCAS_ZTHR"),ZTHR_)+
				"; HMD: "+list_units(units_.get("TCAS_HMD"),HMD_);
		return s;
	}

	private String pvs_list(List<Double> v) {
		String s="(: ";
		boolean comma = false;
		for (int i=0; i<v.size(); ++i) {
			if (comma) {
				s+=", ";
			} else {
				comma = true;
			}
			s+=f.FmPrecision(v.get(i));
		}
		s += " :)";
		return s;
	}

	public String toPVS() {
		String s = "(# ";
		s += "levels := "+pvs_list(levels_);
		s += ", TAU := "+pvs_list(TAU_);
		s += ", TCOA := "+pvs_list(TCOA_);
		s += ", DMOD := "+pvs_list(DMOD_);
		s += ", HMD := "+pvs_list(HMD_);
		s += ", ZTHR := "+pvs_list(ZTHR_);
		s += ", HMDFilter := " + (HMDFilter_ ? "TRUE" : "FALSE");
		return s + " #)";
	}

	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p; 
	}

	public String getUnits(String key) {
		String u = units_.get(key);
		if (u == null) {
			return "unspecified";
		}
		return u;
	}

	public void updateParameterData(ParameterData p) {
		p.setBool("TCAS_HMDFilter",HMDFilter_);
		String u = getUnits("TCAS_level");
		for (int i = 0; i < levels_.size(); ++i) {
			p.setInternal("TCAS_level_"+(i+1),levels_.get(i),u); 
		}
		u = getUnits("TCAS_TAU");
		for (int i = 0; i < TAU_.size(); ++i) {
			p.setInternal("TCAS_TAU_"+(i+1),TAU_.get(i),u);
		}
		u = getUnits("TCAS_TCOA");
		for (int i = 0; i < TCOA_.size(); ++i) {
			p.setInternal("TCAS_TCOA_"+(i+1),TCOA_.get(i),u);
		}
		u = getUnits("TCAS_DMOD");
		for (int i = 0; i < DMOD_.size(); ++i) {
			p.setInternal("TCAS_DMOD_"+(i+1),DMOD_.get(i),u);
		}
		u = getUnits("TCAS_ZTHR");
		for (int i = 0; i < ZTHR_.size(); ++i) {
			p.setInternal("TCAS_ZTHR_"+(i+1),ZTHR_.get(i),u);
		}
		u = getUnits("TCAS_HMD");
		for (int i = 0; i < HMD_.size(); ++i) {
			p.setInternal("TCAS_HMD_"+(i+1),HMD_.get(i),u);
		}
	}

	public void setParameters(ParameterData p) {
		if (p.contains("TCAS_HMDFilter")) {
			HMDFilter_ = p.getBool("TCAS_HMDFilter");
		}
		ParameterData subp = p.extractPrefix("TCAS_level_"); 
		// determine maximum level
		int max_level = (subp.getKeyList().stream().mapToInt(x -> Integer.parseInt(x)).max()).orElse(0); 
		clear();
		for (int sl = 1; sl <= max_level; ++sl) {
			if (p.contains("TCAS_level_"+sl)) { 
				addSensitivityLevel(p.getValue("TCAS_level_"+sl));
				units_.put("TCAS_level",p.getUnit("TCAS_level_"+sl));
			} else {
				addSensitivityLevel();
			}
		}
		for (int sl = 1; sl <= getMaxSensitivityLevel(); ++sl) {
			if (p.contains("TCAS_TAU_"+sl)) {
				setTAU(sl,p.getValue("TCAS_TAU_"+sl));
				units_.put("TCAS_TAU",p.getUnit("TCAS_TAU_"+sl));
			}
			if (p.contains("TCAS_TCOA_"+sl)) {
				setTCOA(sl,p.getValue("TCAS_TCOA_"+sl));
				units_.put("TCAS_TCOA",p.getUnit("TCAS_TCOA_"+sl));
			}
			if (p.contains("TCAS_DMOD_"+sl)) {
				setDMOD(sl,p.getValue("TCAS_DMOD_"+sl));
				units_.put("TCAS_DMOD",p.getUnit("TCAS_DMOD_"+sl));
			}
			if (p.contains("TCAS_HMD_"+sl)) {
				setHMD(sl,p.getValue("TCAS_HMD_"+sl));
				units_.put("TCAS_HMD",p.getUnit("TCAS_HMD_"+sl));
			}
			if (p.contains("TCAS_ZTHR_"+sl)) {
				setZTHR(sl,p.getValue("TCAS_ZTHR_"+sl));
				units_.put("TCAS_ZTHR",p.getUnit("TCAS_ZTHR_"+sl));
			}
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (levels_.hashCode());
		result = prime * result + (DMOD_.hashCode());
		result = prime * result + (HMD_.hashCode());
		result = prime * result + (HMDFilter_ ? 1231 : 1237);
		result = prime * result + (TAU_.hashCode());
		result = prime * result + (TCOA_.hashCode());
		result = prime * result + (ZTHR_.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TCASTable other = (TCASTable) obj;
		if (!levels_.equals(other.levels_)) return false;
		if (!DMOD_.equals(other.DMOD_)) return false;
		if (!HMD_.equals(other.HMD_)) return false;
		if (HMDFilter_ != other.HMDFilter_) return false;
		if (!TAU_.equals(other.TAU_)) return false;
		if (!TCOA_.equals(other.TCOA_)) return false;
		if (!ZTHR_.equals(other.ZTHR_)) return false;
		return true;
	}

	public boolean contains(TCASTable tab) {
		if (!levels_.equals(tab.levels_)) return false;
		if (HMDFilter_ != tab.HMDFilter_) return false;
		for (int i=0; i <= levels_.size(); i++) {
			if (i < levels_.size() && !Util.almost_equals(levels_.get(i),tab.levels_.get(i))) return false;
			if (!(TAU_.get(i) >= tab.TAU_.get(i) && TCOA_.get(i) >= tab.TCOA_.get(i) && 
					DMOD_.get(i) >= tab.DMOD_.get(i) && ZTHR_.get(i) >= tab.ZTHR_.get(i) && HMD_.get(i) >= tab.HMD_.get(i))) 
				return false;
		}
		return true;
	}

}
