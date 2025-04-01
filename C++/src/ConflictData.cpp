/*
 * Copyright (c) 2014-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
/*
 * LossData.cpp
 *
 *  Created on: Nov 21, 2014
 *      Author: rbutler
 */

#include "ConflictData.h"
#include "format.h"
#include "Units.h"

namespace larcfm {

ConflictData::ConflictData(double t_in, double t_out, double t_crit, double d_crit, const Vect3& s, const Vect3& v) : 
              LossData(t_in, t_out),
              time_crit(t_crit), 
              dist_crit(d_crit), 
              s_(s), 
              v_(v) {}

ConflictData::ConflictData() : 
              time_crit(PINFINITY), 
              dist_crit(PINFINITY), 
              s_(Vect3::INVALID()), 
              v_(Vect3::INVALID()) {}


const ConflictData& ConflictData::EMPTY() {
  static ConflictData tmp;
  return tmp;
}

ConflictData::ConflictData(const LossData& ld, double t_crit, double d_crit, const Vect3& s, const Vect3& v) : 
              LossData(ld), 
              time_crit(t_crit), 
              dist_crit(d_crit), 
              s_(s), 
              v_(v) {}

/**
 * Returns internal vector representation of relative aircraft position.
 */
Vect3 const & ConflictData::get_s() const {
  return s_;
}

/**
 * Returns internal vector representation of relative aircraft velocity.
 */
Vect3 const & ConflictData::get_v() const {
  return v_;
}

/**
 * Returns HMD, in internal units, within lookahead time t, in seconds, assuming straight line trajectory.
 */
double ConflictData::HMD(double T) const {
  return Horizontal::hmd(s_.vect2(),v_.vect2(),T);
}

/**
 * Returns HMD, in specified units, within lookahead time t, in seconds, assuming straight line trajectory.
 */
double ConflictData::HMD(const std::string& u, double T) const {
  return Units::to(u,HMD(T));
}

/**
 * Returns VMD, in internal units, within lookahead time t, in seconds, assuming straight line trajectory.
 */
double ConflictData::VMD(double T) const {
  return Vertical::vmd(s_.z(),v_.z(),T);
}

/**
 * Returns VMD, in specified units, within lookahead time t, in seconds, assuming straight line trajectory.
 */
double ConflictData::VMD(const std::string& u, double T) const {
  return Units::to(u,VMD(T));
}

/**
 * Horizontal separation
 * @return Horizontal separation in internal units at current time
 */
 double ConflictData::horizontalSeparation() const {
    return s_.norm2D();
}

/**
 * Horizontal separation
 * @param u units
 * @return Horizontal separation in specified units u at current time
 */
 double ConflictData::horizontalSeparation(const std::string& u) const {
    return Units::to(u,horizontalSeparation());
}

/**
 * Horizontal separation
 * @param time in seconds
 * @return Horizontal separation in internal units at given time
 */
 double ConflictData::horizontalSeparationAtTime(double time) const {
    return s_.AddScal(time,v_).norm2D();
}

/**
 * Horizontal separation at given time
 * @param time time in seconds
 * @param u units
 * @return Horizontal separation in specified units at given time
 */
 double ConflictData::horizontalSeparationAtTime(const std::string& u, double time) const {
    return Units::to(u,horizontalSeparationAtTime(time));
}

/**
 * Vertical separation
 * @return Vertical separation in internal units at current time
 */
 double ConflictData::verticalSeparation() const {
    return std::abs(s_.z());
}

/**
 * Vertical separation
 * @param u units
 * @return Vertical separation in specified units at current time
 */
 double ConflictData::verticalSeparation(const std::string& u) const {
    return Units::to(u,verticalSeparation());
}

/**
 * Vertical separation at given time
 * @param time time in seconds
 * @return Vertical separation in internal units at given time
 */
 double ConflictData::verticalSeparationAtTime(double time) const {
    return std::abs(s_.AddScal(time,v_).z());
}

/**
 * Vertical separation at given time
 * @param time time in seconds
 * @param u units
 * @return Vertical separation in specified units at given time
 */
 double ConflictData::verticalSeparationAtTime(const std::string& u, double time) const {
    return Units::to(u,verticalSeparationAtTime(time));
}

/**
 * Time to horizontal closest point of approach in seconds.
 * When aircraft are diverging, tcpa is defined as 0.
 */
 double ConflictData::tcpa2D() const {
    return Util::max(0.0,Horizontal::tcpa(s_.vect2(),v_.vect2()));
}

/**
 * Time to 3D closest point of approach in seconds.
 * When aircraft are diverging, tcpa is defined as 0
 */
 double ConflictData::tcpa3D() const {
    return Vect3::tcpa(s_,Vect3::ZERO(),v_,Vect3::ZERO());
}

/**
 * Time to co-altitude.
 * @return time to co-altitude in seconds. Returns NaN is v_.z is zero.
 */
 double ConflictData::tcoa() const {
    return Vertical::time_coalt(s_.z(),v_.z());
}

/**
 * Horizontal relative speed
 * @return Horizontal relative speed in internal units at current time
 */
 double ConflictData::horizontalRelativeSpeed() const {
    return v_.norm2D();
}

/**
 * Horizontal relative speed
 * @param u units
 * @return Horizontal relative speed in specified units u at current time
 */
 double ConflictData::horizontalRelativeSpeed(const std::string& u) const {
    return Units::to(u,horizontalRelativeSpeed());
}

/**
 * Vertical relative speed
 * @return Vertical relative speed in internal units at current time
 */
 double ConflictData::verticalRelativeSpeed() const {
    return std::abs(v_.z());
}

/**
 * Vertical relative speed
 * @param u units
 * @return Vertical relative speed in specified units at current time
 */
 double ConflictData::verticalRelativeSpeed(const std::string& u) const {
    return Units::to(u,verticalRelativeSpeed());
}

/** 
 * return SLoWC classification for SC-228 SLoWC value 
 **/
ConflictData::SLoWCClassification ConflictData::classifySLoWC(double slowc) {
  if (Util::almost_equals(slowc,0.0)) return ConflictData::SLoWC_None; 
  else if (slowc <= 17) return ConflictData::SLoWC_Minimal; 
  else if (slowc <= 33) return ConflictData::SLoWC_Minor; 
  else if (slowc <= 47) return ConflictData::SLoWC_Major; 
  else if (slowc <= 94) return ConflictData::SLoWC_Hazardous; 
  else return ConflictData::SLoWC_Catastrophic; 
}

std::string ConflictData::SLoWCClassificationToString(ConflictData::SLoWCClassification slowc_class) {
  switch (slowc_class) {
    case 0: return "SLoWC_None"; 
    case 1: return "SLoWC_Minimal"; 
    case 2: return "SLoWC_Minor"; 
    case 3: return "SLoWC_Major"; 
    case 4: return "SLoWC_Hazardous"; 
    default: return "SLoWC_Catastrophic"; 
  }
}

double ConflictData::squircle(double x, double y) {
  double sqx = Util::sq(x);
  double sqy = Util::sq(y);
  return Util::sqrt_safe(sqx + (1-sqx)*sqy);
}

/**
 * Severity of Loss of Well Clear calculation for one time instant, 0.0 (minimal) - 100.0 (catastrophic). 
 * For an encounter, it should take max of every instance (SC-228)
 **/ 
double ConflictData::SLoWC(double DMOD, double TAUMOD, double ZTHR) const {
  Vect2 s2 = s_.vect2();
  Vect2 v2 = v_.vect2();
  
  double r_i = s2.norm();
  double rdot_i = Util::almost_equals(r_i,0.0) ? 0.0 : s2.dot(v2)/r_i;

  double rdotTAUMOD = rdot_i*TAUMOD;
  double S_i = Util::max(DMOD, (Util::sqrt_safe(Util::sq(rdotTAUMOD)+4*Util::sq(DMOD))-rdotTAUMOD)/2.0);
  double RangePen_i = Util::almost_equals(r_i,0.0) ? 0 : Util::min(r_i/S_i, 1.0);
  
  double sqv2 = v2.sqv();
  double t_CPA = Util::almost_equals(sqv2,0.0) ? 0.0 : -s2.dot(v2)/v2.sqv();
  double HMD_i = s2.AddScal(t_CPA, v2).norm();
  double HMDPen_i = Util::almost_equals(DMOD,0.0) ? 0 : Util::min(HMD_i/DMOD, 1.0);
  
  double dh_i = std::abs(s_.z());
  double VertPen_i = Util::almost_equals(ZTHR,0.0) ? 0.0 : Util::min(dh_i/ZTHR, 1.0);
  
  double slowc = (1.0 - squircle(RangePen_i, squircle(HMDPen_i, VertPen_i))) * 100.0;

  return slowc;
}

/**
 * @return A time in seconds that can be used to compare severity of conflicts for arbitrary
 * well-clear volumes. This time is not necessarily TCPA. ** Don't use it as TCPA. **
 */
double ConflictData::getCriticalTimeOfConflict() const {
  return time_crit;
}

double ConflictData::getDistanceAtCriticalTime() const {
  return dist_crit;
}

/**
 * @return A non-negative scalar that can be used to compare severity of conflicts for arbitrary
 * well-clear volumes. This scalar is a distance in the mathematical way. It is 0 when aircraft are
 * at the same poistion, but it isn't a distance in the physical way. In particular, this distance
 * is unitless. ** Don't use as CPA **
 */
std::string ConflictData::toString() const {
  std::string str = LossData::toString()+" [time_crit: " + FmPrecision(time_crit) +
      ", dist_crit: " + FmPrecision(dist_crit) +"]";
  return str;
}

} /* namespace larcfm */
