/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
/*
 * DaidalusFileWalker.cpp
 *
 */

#include <algorithm>
#include "DaidalusFileWalker.h"

namespace larcfm {

DaidalusFileWalker::DaidalusFileWalker(const std::string& filename) : filename_(filename) {
  if (filename.empty()) {
    sr_.open(&std::cin);
  } else {
      sr_.open(filename);
  }
  init();
}

// Reset input file (do nothing if filename is empty)
void DaidalusFileWalker::resetInputFile(const std::string& filename) {
  if (!filename.empty()) {
    filename_ = filename;
    sr_ = SequenceReader(filename);
    init();
  }
}

void DaidalusFileWalker::init() {
  sr_.setWindowSize(1);
  index_ = 0;
  times_ = sr_.sequenceKeys();
  if (times_.size() > 0) {
    sr_.setActive(times_[0]);
  }
  ownship_.clear();
  traffic_.clear();
  wind_ = Velocity::INVALID();
  columns_ = sr_.getColumnList();
}

/**
 * By default ownship is the first aircraft in the daa file.
 * This method allows for the selection of a different aircraft as the ownship
 * If aircraft with given name doesn't exist at a time step, no ownship or traffic
 * is added to the Daidalus object at that particular time step.
 */
void DaidalusFileWalker::setOwnship(const std::string& name) {
  ownship_ = name;
}

/**
 * Reset the ownship value so that the first aircraft in the daa are considered
 * the ownship.
 */
void DaidalusFileWalker::resetOwnship() {
  ownship_.clear();
}

/**
 * By default all aircraft that are not the ownship are considered to be traffic.
 * This method add a particular aircraft to the list of selected aircraft.
 * Several aircraft can be selected, but if the list of selected aircraft is non empty,
 * only the aircraft in the list are considered traffic.
 */
void DaidalusFileWalker::selectTraffic(const std::string& name) {
  traffic_.push_back(name);
}

/**
 * By default all aircraft that are not the ownship are considered to be traffic.
 * This method add a list of aircraft to the list of selected aircraft.
 * Several aircraft can be selected, but if the list of selected aircraft is non empty,
 * only the aircraft in the list are considered traffic.
 */
void DaidalusFileWalker::selectTraffic(const std::vector<std::string>& names) {
  traffic_.insert(traffic_.end(),names.begin(),names.end());
}

/**
 * Reset the list of selected aircraft so that all aircraft that are not ownship are
 * considered traffic.
 */
void DaidalusFileWalker::resetSelectedTraffic() {
  traffic_.clear();
}

/**
 * Set wind velocity (TO direction, i.e., direction the wind blows to)
 * This setting will override wind information available in the DAA file
 * @param wind Wind (TO direction)
 */
void DaidalusFileWalker::setWindVelocityTo(const Velocity& wind) {
  wind_ = wind;
}

/**
 * Set wind velocity (FROM direction, i.e., direction the wind comes from)
 * This setting will override wind information available in the DAA file
 * @param wind Wind (FROM direction)
 */
void DaidalusFileWalker::setWindVelocityFrom(const Velocity& wind) {
  if (wind.isInvalid()) {
    wind_ = wind;
  } else {
    wind_ = wind.Neg();
  }
}

/**
 * Reset wind. Wind from DAA file will be used.
 */
void DaidalusFileWalker::resetWind() {
  wind_ = Velocity::INVALID();
}

double DaidalusFileWalker::firstTime() const {
  if (!times_.empty()) {
    return times_[0];
  }
  return PINFINITY;
}

double DaidalusFileWalker::lastTime() const {
  if (!times_.empty()) {
    return times_[times_.size()-1];
  }
  return NINFINITY;
}

int DaidalusFileWalker::getIndex() const {
  return index_;
}

double DaidalusFileWalker::getTime() const {
  if (0 <= index_ && index_ < static_cast<int>(times_.size())) {
    return times_[index_];
  } else {
    return NaN;
  }
}

bool DaidalusFileWalker::atBeginning() const {
  return index_ == 0;
}

bool DaidalusFileWalker::atEnd() const {
  return index_ >=0 && index_ == static_cast<int>(times_.size());
}

bool DaidalusFileWalker::goToTime(double t) {
  return goToTimeStep(indexOfTime(t));
}

bool DaidalusFileWalker::goToTimeStep(int i) {
  if (0 <= i && i < static_cast<int>(times_.size())) {
    index_ = i;
    sr_.setActive(times_[index_]);
    return true;
  }
  return false;
}

void DaidalusFileWalker::goToBeginning() {
  goToTimeStep(0);
}

void DaidalusFileWalker::goToEnd() {
  goToTimeStep(times_.size());
}

void DaidalusFileWalker::goNext() {
  bool ok = goToTimeStep(index_+1);
  if (!ok) {
    index_ = times_.size();
  }
}

void DaidalusFileWalker::goPrev() {
  if (!atBeginning()) {
    goToTimeStep(index_-1);
  }
}

int DaidalusFileWalker::indexOfTime(double t) const {
  int i = -1;
  if (t >= firstTime() && t <= lastTime()) {
    i = 0;
    for (; i < static_cast<int>(times_.size())-1; ++i) {
      if (t >= times_[i] && t < times_[i+1]) {
        break;
      }
    }
  }
  return i;
}

ParameterData DaidalusFileWalker::extraColumnsToParameters(double time, const std::string& ac_name) {
  ParameterData pd;
  for (std::vector<std::string>::const_iterator col_ptr = columns_.begin(); col_ptr != columns_.end(); ++col_ptr) {
    if (sr_.hasExtraColumnData(time, ac_name,*col_ptr)) {
      std::string units = sr_.getExtraColumnUnits(*col_ptr);
      if (units == "unitless" || units == "unspecified") {
        pd.set(*col_ptr, sr_.getExtraColumnString(time,ac_name,*col_ptr));
      } else {
        pd.setInternal(*col_ptr,sr_.getExtraColumnValue(time, ac_name,*col_ptr), units);
      }
    }
  }
  return pd;
}


void DaidalusFileWalker::readExtraColumns(Daidalus& daa, int ac_idx, bool read_wind) {
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

void DaidalusFileWalker::setDAAFormat(DAAFormatter& formatter) const {
  formatter.sxyz = false;
  formatter.vxyz = false;
  formatter.groundvel = false;
  formatter.alerter = false;
  formatter.sum = false;
  for (std::vector<std::string>::const_iterator col_ptr = columns_.begin(); col_ptr != columns_.end(); ++col_ptr) {
    const std::string& col = *col_ptr;
    if ( col == "sx" || col == "sy" ) {
      formatter.sxyz = true;
    } else if ( col == "vx" || col == "vy" ) {
      formatter.vxyz = true;
    } else if ( col == "heading" || col == "airspeed" ) {
      formatter.groundvel = true;
    } else if ( col == "alerter") {
      formatter.alerter = true;
    } else if ( col == "s_EW_std" || col == "s_NS_std" || col == "s_EN_std" 
      || col == "sz_std" || col == "v_EW_std" || col == "v_NS_std" 
      || col == "v_EN_std" || col == "vz_std") {
        formatter.sum = true;
    }
  }
  if (!wind_.isInvalid()) {
    formatter.groundvel = true;
  }
}

void DaidalusFileWalker::setDAAUnits(Daidalus& daa) const {
  for (std::vector<std::string>::const_iterator col_ptr = columns_.begin(); col_ptr != columns_.end(); ++col_ptr) {
    const std::string& col = *col_ptr;
		std::string units = sr_.getExtraColumnUnits(col);
		daa.setUnitsOf(col,units);
	}
}

void DaidalusFileWalker::readState(Daidalus& daa) {
  if (p_.size() > 0) {
    daa.setParameterData(p_);
    daa.reset();
  }
  int own = 0; // By default onwship is 0
  if (!ownship_.empty()) {
    own = -1;
    for (int ac = 0; ac < sr_.size();++ac) {
      if (sr_.getName(ac) == ownship_) {
        own = ac;
        break;
      }
    }
  }
  if (own >= 0) {
    std::string ido = sr_.getName(own);
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
      std::string ida = sr_.getName(ac);
      if (traffic_.empty() ||
          std::find(traffic_.begin(),traffic_.end(),ida) != traffic_.end()) {
        Position sa = sr_.getPosition(ac);
        Velocity va = sr_. getVelocity(ac);
        // Notice that idx may be different from ac because of traffic
        int idx=daa.addTrafficState(ida,sa,va);
        if (idx >= 0) {
          readExtraColumns(daa,idx,false);
        }
      }
    }
  }
  goNext();
}

// ErrorReporter Interface Methods

bool DaidalusFileWalker::hasError() const {
  return sr_.hasError();
}

bool DaidalusFileWalker::hasMessage() const {
  return sr_.hasMessage();
}

std::string DaidalusFileWalker::getMessage() {
  return sr_.getMessage();
}

std::string DaidalusFileWalker::getMessageNoClear() const {
  return sr_.getMessageNoClear();
}

}
