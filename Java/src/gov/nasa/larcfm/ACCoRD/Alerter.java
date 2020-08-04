/*
 * Copyright (c) 2015-2019 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.ParameterAcceptor;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** 
 * It is assumed that for all alert level {@code i >= 1: detection(i+1) => detection(i)} and that there is at least one
 * guidance level whose region is different from NONE. 
 */

public class Alerter implements ParameterAcceptor {

	private List<AlertThresholds> levels_; // This list is 1-indexed at the user level. 0 means none.
	private String id_;

	public Alerter() {
		id_ = "default";
		levels_ = new ArrayList<AlertThresholds>();
	}

	public static final Alerter INVALID = new Alerter();

	public boolean isValid() {
		return !levels_.isEmpty();
	}

	public Alerter(String id) {
		id_ = id.equals("") ? "default" : id;
		levels_ = new ArrayList<AlertThresholds>();
	}

	/**
	 * Copy alert levels from the given parameter.
	 * Detectors are deeply copied.
	 * 
	 */	
	public void copyFrom(Alerter alerter) {
		id_ = alerter.id_;
		levels_.clear();
		for (int i=1; i <= alerter.mostSevereAlertLevel(); ++i) {
			levels_.add(new AlertThresholds(alerter.getLevel(i)));
		}
	}

	public Alerter(Alerter alerter) {
		levels_ = new ArrayList<AlertThresholds>();
		copyFrom(alerter);
	}

	/**
	 * Set alerter identifier.
	 * @param id
	 */
	public void setId(String id) {
		if (!id.equals("")) {
			id_ = id;
		}
	}

	/**
	 * @return alerter identifier.
	 */
	public String getId() {
		return id_;
	}

	/**
	 * @return DO-365 HAZ preventive thresholds, i.e., DTHR=0.66nmi, ZTHR=700ft, 
	 * TTHR=35s, TCOA=0, alerting time = 55s, early alerting time = 75s,
	 * bands region = NONE
	 */
	public static final AlertThresholds DO_365_Phase_I_HAZ_preventive =
			new AlertThresholds(WCV_TAUMOD.DO_365_Phase_I_preventive,55,75,BandsRegion.NONE);

	/**
	 * @return DO-365 HAZ corrective thresholds, i.e., DTHR=0.66nmi, ZTHR=450ft, 
	 * TTHR=35s, TCOA=0, alerting time = 55s, early alerting time = 75s, 
	 * bands region = MID
	 */
	public static final AlertThresholds DO_365_Phase_I_HAZ_corrective =
			new AlertThresholds(WCV_TAUMOD.DO_365_DWC_Phase_I,55,75,BandsRegion.MID);

	/**
	 * @return DO-365 HAZ warning thresholds, i.e., DTHR=0.66nmi, ZTHR=450ft, 
	 * TTHR=35s, TCOA=0, alerting time = 25s, early alerting time = 55s, 
	 * bands region = NEAR
	 */
	public static final AlertThresholds DO_365_Phase_I_HAZ_warning =
			new AlertThresholds(WCV_TAUMOD.DO_365_DWC_Phase_I,25,55,BandsRegion.NEAR);

	/** 
	 * @return alerting thresholds (unbuffered) as defined in RTCA DO-365.
	 * Maneuver guidance logic produces multilevel bands:
	 * MID: Corrective
	 * NEAR: Warning
	 */
	public static Alerter get_DWC_Phase_I() {
		Alerter alerter = new Alerter("DWC_Phase_I");
		alerter.addLevel(DO_365_Phase_I_HAZ_preventive); 
		alerter.addLevel(DO_365_Phase_I_HAZ_corrective); 
		alerter.addLevel(DO_365_Phase_I_HAZ_warning);
		return alerter;
	}

	/** 
	 * @return alerting thresholds (unbuffered) as defined in RTCA DO-365.
	 * Maneuver guidance logic produces multilevel bands:
	 * MID: Corrective
	 * NEAR: Warning
	 */
	public static final Alerter DWC_Phase_I = get_DWC_Phase_I();

	/**
	 * @return buffered HAZ preventive thresholds, i.e., DTHR=1nmi, ZTHR=750ft, 
	 * TTHR=35s, TCOA=20s, alerting time = 60s, early alerting time = 75s,
	 * bands region = NONE
	 */
	public static final AlertThresholds Buffered_Phase_I_HAZ_preventive =
			new AlertThresholds(WCV_TAUMOD.Buffered_Phase_I_preventive,60,75,BandsRegion.NONE);

	/**
	 * @return buffered HAZ corrective thresholds, i.e., DTHR=1nmi, ZTHR=450ft, 
	 * TTHR=35s, TCOA=20s, alerting time = 60s, early alerting time = 75s, 
	 * bands region = MID
	 */
	public static final AlertThresholds Buffered_Phase_I_HAZ_corrective =
			new AlertThresholds(WCV_TAUMOD.Buffered_DWC_Phase_I,60,75,BandsRegion.MID);

	/**
	 * @return buffered HAZ warning thresholds, i.e., DTHR=1nmi, ZTHR=450ft, 
	 * TTHR=35s, TCOA=20s, alerting time = 30s, early alerting time = 55s, 
	 * bands region = NEAR
	 */
	public static final AlertThresholds Buffered_Phase_I_HAZ_warning =
			new AlertThresholds(WCV_TAUMOD.Buffered_DWC_Phase_I,30,55,BandsRegion.NEAR);

	/** 
	 * @return alerting thresholds (buffered) as defined in RTCA DO-365.
	 * Maneuver guidance logic produces multilevel bands:
	 * MID: Corrective
	 * NEAR: Warning
	 */
	public static Alerter get_Buffered_DWC_Phase_I() {
		Alerter alerter = new Alerter("Buffered_DWC_Phase_I");
		alerter.addLevel(Buffered_Phase_I_HAZ_preventive); 
		alerter.addLevel(Buffered_Phase_I_HAZ_corrective); 
		alerter.addLevel(Buffered_Phase_I_HAZ_warning);
		return alerter;
	}

	public static final Alerter Buffered_DWC_Phase_I =
			get_Buffered_DWC_Phase_I();

	/** 
	 * @return alerting thresholds for single bands given by detector,
	 * alerting time, and lookahead time. The single bands region is NEAR
	 */
	public static Alerter SingleBands(Detection3D detector, 
			double alerting_time, double lookahead_time) {
		Alerter alerter = new Alerter("");
		alerter.addLevel(new AlertThresholds(detector,
				alerting_time,lookahead_time,BandsRegion.NEAR));
		return alerter;
	}

	/** 
	 * @return alerting thresholds for ACCoRD's CD3D, i.e.,
	 * separation is given by cylinder of 5nmi/1000ft. Lookahead time is 180s.
	 */
	public static Alerter get_CD3D_SingleBands() {
		Alerter alerter = SingleBands(CDCylinder.CD3DCylinder,180,180);
		alerter.setId("CD3D");
		return alerter;		
	}

	/** 
	 * @return alerting thresholds for ACCoRD's CD3D, i.e.,
	 * separation is given by cylinder of 5nmi/1000ft. Alerting time is 
	 * 180s.
	 */
	public static final Alerter CD3D_SingleBands = get_CD3D_SingleBands();

	/** 
	 * @return alerting thresholds for DAIDALUS single bands WCV_TAUMOD , i.e.,
	 * separation is given by cylinder of DTHR=0.66nmi, ZTHR=450ft, TTHR=35s, TCOA=0, 
	 * alerting time = 55s, early alerting time = 75s.
	 */
	public static Alerter get_WCV_TAUMOD_SingleBands() {
		Alerter alerter = SingleBands(WCV_TAUMOD.DO_365_DWC_Phase_I,55,75);
		alerter.setId("WCV_TAUMOD");
		return alerter;		
	}

	/** 
	 * @return alerting thresholds for DAIDALUS single bands WCV_TAUMOD , i.e.,
	 * separation is given by cylinder of DTHR=0.66nmi, ZTHR=450ft, TTHR=35s, TCOA=0, 
	 * alerting time = 55s, early alerting time = 75s,
	 */
	public static final Alerter WCV_TAUMOD_SingleBands = get_WCV_TAUMOD_SingleBands();
	
	/** 
	 * Clears alert levels
	 **/
	public void clear() {
		levels_.clear();
	}

	/**
	 * @return most severe alert level.
	 */
	public int mostSevereAlertLevel() {
		return levels_.size();
	}

	/**
	 * @return first alert level whose region is equal to given region. Returns 0 if none.
	 */   
	public int alertLevelForRegion(BandsRegion region) {  
		for (int i=0; i < levels_.size(); ++i) {
			if (levels_.get(i).getRegion() == region) {
				return i+1;
			}
		}
		return 0;
	}

	/**
	 * @return detector for given alert level starting from 1.
	 */   
	public Optional<Detection3D> getDetector(int alert_level) {
		if (1 <= alert_level && alert_level <= levels_.size()) {
			return Optional.of(levels_.get(alert_level-1).getCoreDetection());
		} else {
			return Detection3D.NoDetector;
		}
	}

	/**
	 * Set the threshold values of a given alert level. 
	 */
	public void setLevel(int level, AlertThresholds thresholds) {
		if (1 <= level && level <= levels_.size()) {
			levels_.set(level-1,new AlertThresholds(thresholds));
		} 
	}

	/**
	 * Add an alert level and returns its numerical type, which is a positive number.
	 */
	public int addLevel(AlertThresholds thresholds) {
		AlertThresholds th = new AlertThresholds(thresholds);
		levels_.add(th);
		int size = levels_.size();
		th.getCoreDetection().setIdentifier("det_"+f.Fmi(size));
		return size;
	}

	/** 
	 * @return threshold values of a given alert level
	 */
	public AlertThresholds getLevel(int alert_level) {
		if (1 <= alert_level && alert_level <= levels_.size()) {
			return levels_.get(alert_level-1);
		} else {
			return AlertThresholds.INVALID;
		}
	}

	@Override
	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p;
	}

	@Override
	public void updateParameterData(ParameterData p) {
		// create the base parameterdata object storing the detector information
		// this also ensures they each have a unique identifier
		ParameterData pdmain = new ParameterData();
		// add parameters for each alerter, ensuring they have an ordered set of identifiers
		for (int i = 0; i < levels_.size(); i++) {
			ParameterData pd = levels_.get(i).getParameters();
			//make sure each instance has a unique, ordered name
			String prefix = "alert_"+f.Fmi(i+1)+"_";
			pdmain.copy(pd.copyWithPrefix(prefix),true);
			Detection3D det = levels_.get(i).getCoreDetection();
			pdmain.copy(det.getParameters().copyWithPrefix(det.getIdentifier()+"_"),true);
			pdmain.set("load_core_detection_"+det.getIdentifier()+" = "+det.getCanonicalClassName());
			pdmain.remove(det.getIdentifier()+"_id");
		}
		p.copy(pdmain,true);
	}

	@Override
	public void setParameters(ParameterData p) {
		// read in all detector information
		List<Detection3D> dlist = Detection3DParameterReader.readCoreDetection(p).first;
		// put in map for easy lookup
		Map<String,Detection3D> dmap = new HashMap<String,Detection3D>();
		dlist.stream().forEach(x -> dmap.put(x.getIdentifier(),x));
		// extract parameters for each alertlevel:
		int counter = 1;
		String prefix = "alert_"+f.Fmi(counter)+"_";
		ParameterData pdsub = p.extractPrefix(prefix);
		if (pdsub.size() > 0) {
			levels_.clear();
		}
		while (pdsub.size() > 0) {
			// build the alertlevel
			AlertThresholds al = new AlertThresholds();
			al.setCoreDetection(dmap.get(pdsub.getString("detector")));
			al.setParameters(pdsub);
			// modify or add the alertlevel (this cannot remove levels)
			if (counter <= levels_.size()) {
				setLevel(counter,al);
			} else {
				addLevel(al);
			}
			// next set
			counter++;
			prefix = "alert_"+f.Fmi(counter)+"_";
			pdsub = p.extractPrefix(prefix);
		}
	}

	public String toString() {
		String s = "Alerter: ";
		s += id_+"\n";
		for (int i=0; i < levels_.size(); ++i) {
			s += "Level "+(i+1)+": "+levels_.get(i).toString()+"\n";
		}
		return s;
	}

	public String toPVS() {
		String s = "(: ";
		boolean first = true;
		for (int i=0; i < levels_.size(); ++i) {
			if (first) {
				first = false;
			} else {
				s += ",";
			}
			s += levels_.get(i).toPVS();
		}
		return s+" :)";
	}

	public static String listToPVS(List<Alerter> alerters) {
		String s = "(: ";
		boolean first = true;
		for (int i=0; i < alerters.size(); ++i) {
			if (first) {
				first = false;
			} else {
				s += ",";
			}
			s += alerters.get(i).toPVS();
		}

		return s+" :)";
	}

}
