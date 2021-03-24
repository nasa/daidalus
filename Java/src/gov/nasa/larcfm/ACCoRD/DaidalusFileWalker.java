/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.IO.SequenceReader;
import gov.nasa.larcfm.Util.ErrorReporter;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Velocity;

import java.util.ArrayList;
import java.util.List;


public class DaidalusFileWalker implements ErrorReporter {

	private SequenceReader sr_;
	private ParameterData p_;
	private List<Double> times_;
	private int index_;
	private String ownship_; // Set ownship to a given value
	private List<String> traffic_; // Only consider the traffic aircraft

	public DaidalusFileWalker(String filename) {
		sr_ = new SequenceReader(filename);
		init();
	}

	public void resetInputFile(String filename) {
		sr_ = new SequenceReader(filename);
		init();
	}

	private void init() {
		sr_.setWindowSize(1);
		index_ = 0;
		times_ = sr_.sequenceKeys();
		p_ = sr_.getParameters();
		if (times_.size() > 0) {
			sr_.setActive(times_.get(0));
		}
		ownship_ = "";
		traffic_ = new ArrayList<String>();
	}

	/** 
	 * By default ownship is the first aircraft in the daa file.
	 * This method allows for the selection of a different aircraft as the ownship
	 * If aircraft with given name doesn't exist at a time step, no ownship or traffic 
	 * is added to the Daidalus object at that particular time step.
	 */
	public void setOwnship(String name) {
		ownship_ = name;
	}

	/** 
	 * Returns the name of the ownship.
	 * An empty string refers to the aircraft that is the first in the daa file
	 */
	public String getOwnship() {
		return ownship_;
	}

	/** 
	 * Reset the ownship value so that the first aircraft in the daa are considered
	 * the ownship.
	 */
	public void resetOwnship() {
		setOwnship("");
	}

	/** 
	 * By default all aircraft that are not the ownship are considered to be traffic.
	 * This method add a particular aircraft to the list of selected aircraft.
	 * Several aircraft can be selected, but if the list of selected aircraft is non empty, 
	 * only the aircraft in the list are considered traffic.  
	 */
	public void selectTraffic(String name) {
		traffic_.add(name);
	}

	/** 
	 * By default all aircraft that are not the ownship are considered to be traffic.
	 * This method add a list of aircraft to the list of selected aircraft.
	 * Several aircraft can be selected, but if the list of selected aircraft is non empty, 
	 * only the aircraft in the list are considered traffic.  
	 */
	public void selectTraffic(List<String> names) {
		traffic_.addAll(names);
	}

	/**
	 * Returns the list of selected traffic. An empty list means that all aircraft that are 
	 * not the ownship are considered traffic.
	 */
	public List<String> getSelectedTraffic() {
		return traffic_;
	}

	/**
	 * Reset the list of selected aircraft so that all aircraft that are not ownship are
	 * considered traffic.
	 */
	public void resetSelectedTraffic() {
		traffic_.clear();
	}

	/**
	 * Returns the name of the traffic aircraft. An empty list 
	 * @return
	 */

	public double firstTime() {
		if (!times_.isEmpty()) {
			return times_.get(0);
		} 
		return Double.POSITIVE_INFINITY;
	}

	public double lastTime() {
		if (!times_.isEmpty()) {
			return times_.get(times_.size()-1);
		}
		return Double.NEGATIVE_INFINITY;
	}

	public int getIndex() {
		return index_;
	}

	public double getTime() {
		if (0 <= index_ && index_ < times_.size()) {
			return times_.get(index_);
		} else {
			return Double.NaN;
		}
	}

	public boolean atBeginning() {
		return index_ == 0;
	}

	public boolean atEnd() {
		return index_ == times_.size();
	}

	public boolean goToTime(double t) {
		return goToTimeStep(indexOfTime(t));
	}

	public boolean goToTimeStep(int i) {
		if (0 <= i && i < times_.size()) {
			index_ = i;
			sr_.setActive(times_.get(index_));
			return true;
		}
		return false;
	}

	public void goToBeginning() {
		goToTimeStep(0);
	}

	public void goToEnd() {
		goToTimeStep(times_.size());
	}

	public void goNext() {
		boolean ok = goToTimeStep(index_+1);
		if (!ok) {
			index_ = times_.size();
		}
	}

	public void goPrev() {
		if (!atBeginning()) {
			goToTimeStep(index_-1);
		}
	}

	public int indexOfTime(double t) {
		int i = -1;
		if (t >= firstTime() && t <= lastTime()) {
			i = 0;
			for (; i < times_.size()-1; ++i) {
				if (t >= times_.get(i) && t < times_.get(i+1)) {
					break;
				}
			}
		}
		return i;
	}

	private static ParameterData extraColumnsToParameters(SequenceReader sr, double time, String ac_name) {
		ParameterData pd = new ParameterData();
		List<String> columns = sr.getExtraColumnList();
		for (String col : columns) {
			if (sr.hasExtraColumnData(time, ac_name, col)) {
				String units = sr.getExtraColumnUnits(col);
				if (units.equals("unitless") || units.equals("unspecified")) {
					pd.set(col, sr.getExtraColumnString(time, ac_name, col));
				} else {
					pd.setInternal(col, sr.getExtraColumnValue(time, ac_name, col), units);
				}
			}
		}
		return pd;
	}

	public static void readExtraColumns(Daidalus daa, SequenceReader sr, int ac_idx) {
		ParameterData pcol = extraColumnsToParameters(sr,daa.getCurrentTime(),daa.getAircraftStateAt(ac_idx).getId());
		if (pcol.size() > 0) {
			daa.setParameterData(pcol);
			if (pcol.contains("alerter")) {
				daa.setAlerterIndex(ac_idx,pcol.getInt("alerter"));
			}
			double s_EW_std = 0.0;
			if (pcol.contains("s_EW_std")) {
				s_EW_std = pcol.getValue("s_EW_std");     	
			}
			double s_NS_std = 0.0;
			if (pcol.contains("s_NS_std")) {
				s_NS_std = pcol.getValue("s_NS_std");     	
			}
			double s_EN_std = 0.0;
			if (pcol.contains("s_EN_std")) {
				s_EN_std = pcol.getValue("s_EN_std");     	
			}
			daa.setHorizontalPositionUncertainty(ac_idx,s_EW_std,s_NS_std,s_EN_std);
			double sz_std = 0.0;
			if (pcol.contains("sz_std")) {
				sz_std = pcol.getValue("sz_std");     	
			}
			daa.setVerticalPositionUncertainty(ac_idx,sz_std);
			double v_EW_std = 0.0;
			if (pcol.contains("v_EW_std")) {
				v_EW_std = pcol.getValue("v_EW_std");     	
			}
			double v_NS_std = 0.0;
			if (pcol.contains("v_NS_std")) {
				v_NS_std = pcol.getValue("v_NS_std");     	
			}
			double v_EN_std = 0.0;
			if (pcol.contains("v_EN_std")) {
				v_EN_std = pcol.getValue("v_EN_std");     	
			}
			daa.setHorizontalVelocityUncertainty(ac_idx,v_EW_std,v_NS_std,v_EN_std);
			double vz_std = 0.0;
			if (pcol.contains("vz_std")) {
				vz_std = pcol.getValue("vz_std");     	
			}
			daa.setVerticalSpeedUncertainty(ac_idx,vz_std);
		}
	}

	public void readState(Daidalus daa) {
		if (p_.size() > 0) {
			daa.setParameterData(p_);
			daa.reset();
		}
		int own = 0; // By default onwship is 0
		if (!ownship_.isEmpty()) {
			own = -1;
			for (int ac = 0; ac < sr_.size();++ac) {
				if (sr_.getName(ac).equals(ownship_)) {
					own = ac;
					break;
				}
			}
		}
		if (own >= 0) {
			String ido = sr_.getName(own);
			Position so = sr_.getPosition(own);
			Velocity vo = sr_. getVelocity(own);
			daa.setOwnshipState(ido,so,vo,getTime());
			readExtraColumns(daa,sr_,0);
			for (int ac = 0; ac < sr_.size();++ac) {
				if (ac == own) {
					continue;
				}
				String ida = sr_.getName(ac);
				if (traffic_.isEmpty() || traffic_.contains(ida)) {
					Position sa = sr_.getPosition(ac);
					Velocity va = sr_. getVelocity(ac);
					// Notice that idx may be different from ac because of traffic
					int idx=daa.addTrafficState(ida,sa,va);
					readExtraColumns(daa,sr_,idx);
				}
			}
		}
		goNext();
	}

	public boolean hasError() {
		return sr_.hasError();
	}

	public boolean hasMessage() {
		return sr_.hasMessage();
	}

	public String getMessage() {
		return sr_.getMessage();
	}

	public String getMessageNoClear() {
		return sr_.getMessageNoClear();
	}

}
