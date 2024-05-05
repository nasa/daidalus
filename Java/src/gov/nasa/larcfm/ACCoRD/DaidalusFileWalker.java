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
import java.util.Optional;

public class DaidalusFileWalker implements ErrorReporter {

	private String filename_;
	private SequenceReader sr_;
	private ParameterData p_;
	private List<Double> times_;
	private int index_;
	private Optional<String> ownship_; // Set ownship to a given value
	private List<String> traffic_; // Only consider the traffic aircraft in this list
	private Velocity wind_; // Wind velocity vector
	private List<String> columns_; // Columns of DAA file

	public DaidalusFileWalker(String filename) {
		filename_ = filename;
		sr_ = new SequenceReader(filename);
		traffic_ = new ArrayList<>();
		init();
	}

	public void resetInputFile(String filename) {
		filename_ = filename;
		sr_ = new SequenceReader(filename);
		init();
	}

	public String getFileName() {
		return filename_;
	}

	private void init() {
		sr_.setWindowSize(1);
		index_ = 0;
		times_ = sr_.sequenceKeys();
		p_ = sr_.getParameters();
		if (times_.size() > 0) {
			sr_.setActive(times_.get(0));
		}
		ownship_ = Optional.empty();
		traffic_.clear();
		wind_ = Velocity.INVALID;
		columns_ = sr_.getExtraColumnList();
	}

	/** 
	 * By default ownship is the first aircraft in the daa file.
	 * This method allows for the selection of a different aircraft as the ownship
	 * If aircraft with given name doesn't exist at a time step, no ownship or traffic 
	 * is added to the Daidalus object at that particular time step.
	 */
	public void setOwnship(String name) {
		ownship_ = Optional.of(name);
	}

	/** 
	 * Reset the ownship value so that the first aircraft in the daa are considered
	 * the ownship.
	 */
	public void resetOwnship() {
		ownship_ = Optional.empty();
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
	 * Reset the list of selected aircraft so that all aircraft that are not ownship are
	 * considered traffic.
	 */
	public void resetSelectedTraffic() {
		traffic_.clear();
	}

	/**
	 * Set wind velocity (TO direction, i.e., direction the wind blows to)
	 * This setting will override wind information available in the DAA file
	 * @param wind Wind (TO direction)
	 */
	public void setWindVelocityTo(Velocity wind) {
		wind_ = wind;
	}

	/**
	 * Set wind velocity (FROM direction, i.e., direction the wind comes from)
	 * This setting will override wind information available in the DAA file
	 * @param wind Wind (FROM direction)
	 */
	public void setWindVelocityFrom(Velocity wind) {
		if (wind.isInvalid()) {
			wind_ = wind;
		} else {
			wind_ = wind.Neg();
		}
	}

	/**
	 * Reset wind. Wind from DAA file will be used.
	 */
	public void resetWind() {
		wind_ = Velocity.INVALID;
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

	private ParameterData extraColumnsToParameters(double time, String ac_name) {
		ParameterData pd = new ParameterData();
		for (String col : columns_) {
			if (sr_.hasExtraColumnData(time, ac_name, col)) {
				String units = sr_.getExtraColumnUnits(col);
				if (units.equals("unitless") || units.equals("unspecified")) {
					pd.set(col,sr_.getExtraColumnString(time, ac_name, col));
				} else {
					pd.setInternal(col,sr_.getExtraColumnValue(time, ac_name, col), units);
				}
			}
		}
		return pd;
	}

	private void readExtraColumns(Daidalus daa, int ac_idx, boolean read_wind) {
		ParameterData pcol = extraColumnsToParameters(daa.getCurrentTime(),daa.getAircraftStateAt(ac_idx).getId());
		if (pcol.size() > 0) {
			if (ac_idx == 0) {
				// Note that daidalus parameters (in column format) and air velocity are ignored for traffic aircraft
				// The index of the aircraft below is the index in daa
				daa.setParameterData(pcol);
				// Read wind unless is overriden 
				if (pcol.contains("heading") && pcol.contains("airspeed") && read_wind) {
					double heading = pcol.getValue("heading");
					double airspeed = pcol.getValue("airspeed");
					daa.setOwnshipAirVelocity(heading,airspeed);
				}
			}
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

	public void setDAAFormat(DAAFormatter formatter) {
		formatter.sxyz = columns_.contains("sx") || columns_.contains("sy");
		formatter.vxyz = columns_.contains("vx") || columns_.contains("vy");
		formatter.groundvel = columns_.contains("heading") || columns_.contains("airspeed");
		formatter.alerter = columns_.contains("alerter");
		formatter.sum = columns_.contains("s_EW_std") || columns_.contains("s_NS_std") || columns_.contains("s_EN_std") 
			|| columns_.contains("sz_std") || columns_.contains("v_EW_std") || columns_.contains("v_NS_std") 
			|| columns_.contains("v_EN_std") || columns_.contains("vz_std");
	}

	public void setDAAUnits(Daidalus daa) {
		for (String col : columns_) {
			String units = sr_.getExtraColumnUnits(col);
			daa.setUnitsOf(col,units);
		}
	}

	public void readState(Daidalus daa) {
		if (p_.size() > 0) {
			daa.setParameterData(p_);
			daa.reset();
		}
		int own = 0; // By default onwship is 0
		if (ownship_.isPresent()) {
			own = -1;
			for (int ac = 0; ac < sr_.size();++ac) {
				if (sr_.getName(ac).equals(ownship_.get())) {
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
			readExtraColumns(daa,0,wind_.isInvalid());
			if (!wind_.isInvalid() && !wind_.isZero()) {
				// Override wind
				daa.setWindVelocityTo(wind_);
			}
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
					readExtraColumns(daa,idx,false);
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
