/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
/*
 * LossData.h
 *
 *  Created on: Nov 21, 2014
 *      Author: rbutler
 */

#ifndef CONFLICTDATA_H_
#define CONFLICTDATA_H_

#include "LossData.h"
#include "Vect3.h"
#include "Horizontal.h"
#include "Vertical.h"
#include "Interval.h"

#include <string>

namespace larcfm {

class ConflictData : public LossData {

public:

  double  time_crit;   // relative time to critical point
  double  dist_crit;   // distance or severity at critical point (0 is most critical, +inf is least severe)

private:
  Vect3 s_; // Relative position
  Vect3 v_; // Relative velocity

  static double squircle(double x, double y);

public:

	enum SLoWCClassification {
    SLoWC_None, SLoWC_Minimal,SLoWC_Minor, SLoWC_Major, SLoWC_Hazardous, SLoWC_Catastrophic
	};

  std::string toString() const;

  ConflictData(double t_in, double t_out, double t_crit, double d_crit, const Vect3& s, const Vect3& v);
  ConflictData(const LossData& ld, double t_crit, double d_crit, const Vect3& s, const Vect3& v);
  ConflictData();

  static const ConflictData& EMPTY();

  /**
   * Returns internal vector representation of relative aircraft position.
   */
  Vect3 const & get_s() const;

  /**
   * Returns internal vector representation of relative aircraft velocity.
   */
  Vect3 const & get_v() const;

  /**
   * Returns HMD, in internal units, within lookahead time t, in seconds, assuming straight line trajectory.
   */
  double HMD(double T) const;

  /**
   * Returns HMD, in specified units, within lookahead time t, in seconds, assuming straight line trajectory.
   */
  double HMD(const std::string& u, double T) const;

  /**
   * Returns VMD, in internal units, within lookahead time t, in seconds, assuming straight line trajectory.
   */
  double VMD(double T) const;

  /**
   * Returns VMD, in specified units, within lookahead time t, in seconds, assuming straight line trajectory.
   */
  double VMD(const std::string& u, double T) const;

  /**
   * Horizontal separation
   * @return Horizontal separation in internal units at current time
   */
  double horizontalSeparation() const;

  /**
   * Horizontal separation
   * @param u units
   * @return Horizontal separation in specified units u at current time
   */
  double horizontalSeparation(const std::string& u) const;

  /**
   * Horizontal separation
   * @param time in seconds
   * @return Horizontal separation in internal units at given time
   */
  double horizontalSeparationAtTime(double time) const;

  /**
   * Horizontal separation at given time
   * @param time time in seconds
   * @param u units
   * @return Horizontal separation in specified units at given time
   */
  double horizontalSeparationAtTime(const std::string& u, double time) const;

  /**
   * Vertical separation
   * @return Vertical separation in internal units at current time
   */
  double verticalSeparation() const;

  /**
   * Vertical separation
   * @param u units
   * @return Vertical separation in specified units at current time
   */
  double verticalSeparation(const std::string& u) const;

  /**
   * Vertical separation at given time
   * @param time time in seconds
   * @return Vertical separation in internal units at given time
   */
  double verticalSeparationAtTime(double time) const;

  /**
   * Vertical separation at given time
   * @param time time in seconds
   * @param u units
   * @return Vertical separation in specified units at given time
   */
  double verticalSeparationAtTime(const std::string& u, double time) const;

  /**
   * Time to horizontal closest point of approach in seconds.
   * When aircraft are diverging, tcpa is defined as 0.
   */
  double tcpa2D() const;

  /**
   * Time to 3D closest point of approach in seconds.
   * When aircraft are diverging, tcpa is defined as 0
   */
  double tcpa3D() const;

  /**
   * Time to co-altitude.
   * @return time to co-altitude in seconds. Returns NaN is v_.z is zero.
   */
  double tcoa() const;

  /**
   * Horizontal relative speed
   * @return Horizontal relative speed in internal units at current time
   */
  double horizontalRelativeSpeed() const;

  /**
   * Horizontal relative speed
   * @param u units
   * @return Horizontal relative speed in specified units u at current time
   */
  double horizontalRelativeSpeed(const std::string& u) const;

  /**
   * Vertical relative speed
   * @return Vertical relative speed in internal units at current time
   */
  double verticalRelativeSpeed() const;

  /**
   * Vertical relative speed
   * @param u units
   * @return Vertical relative speed in specified units at current time
   */
  double verticalRelativeSpeed(const std::string& u) const;

  /** 
   * return SLoWC classification for SC-228 SLoWC value 
   **/
  static SLoWCClassification classifySLoWC(double slowc);

  static std::string SLoWCClassificationToString(SLoWCClassification slowc_class);

  /**
	 * Severity of Loss of Well Clear calculation for one time instant, 0.0 (minimal) - 100.0 (catastrophic). 
	 * For an encounter, it should take max of every instance (SC-228)
	 **/ 
	double SLoWC(double DMOD, double TAUMOD, double ZTHR) const;

  /**
   * @return A time in seconds that can be used to compare severity of conflicts for arbitrary
   * well-clear volumes. This time is not necessarily TCPA. ** Don't use it as TCPA. **
   */
  double getCriticalTimeOfConflict() const;

  /**
   * @return A non-negative scalar that can be used to compare severity of conflicts for arbitrary
   * well-clear volumes. This scalar is a distance in the mathematical way. It is 0 when aircraft are
   * at the same poistion, but it isn't a distance in the physical way. In particular, this distance
   * is unitless. ** Don't use as CPA **
   */
  double getDistanceAtCriticalTime() const;

};

} /* namespace larcfm */

#endif /* SRC_LOSSDATA_H_ */
