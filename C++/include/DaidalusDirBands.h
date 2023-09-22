/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
#ifndef DAIDALUSDIRBANDS_H_
#define DAIDALUSDIRBANDS_H_

#include "Velocity.h"
#include "Position.h"
#include "Detection3D.h"
#include "DaidalusRealBands.h"
#include "IntervalSet.h"
#include "Tuple5.h"
#include "Kinematics.h"
#include "ProjectedKinematics.h"
#include <vector>

namespace larcfm {

class DaidalusDirBands : public DaidalusRealBands {

private:

	// Alternative gs for computation of horizontal direction bands when value is greater than 0. 
	// This alternative gs is used when aircraft is flying below min horizontal airspeed 
	double alt_gs_;

  Velocity ownship_vel(const DaidalusParameters& parameters, const TrafficState& ownship) const;

public:
  DaidalusDirBands();

  DaidalusDirBands(const DaidalusDirBands& b);

  virtual bool get_recovery(const DaidalusParameters& parameters) const;

  virtual double get_step(const DaidalusParameters& parameters) const;

  virtual double get_min(const DaidalusParameters& parameters) const;

  virtual double get_max(const DaidalusParameters& parameters) const;

  virtual double get_min_rel(const DaidalusParameters& parameters) const;

  virtual double get_max_rel(const DaidalusParameters& parameters) const;

  virtual bool saturate_corrective_bands(const DaidalusParameters& parameters, const SpecialBandFlags& special_flags) const;

  virtual void set_special_configuration(const DaidalusParameters& parameters, const SpecialBandFlags& special_flags);

  virtual bool instantaneous_bands(const DaidalusParameters& parameters) const;

  virtual double own_val(const TrafficState& ownship) const;

  virtual double time_step(const DaidalusParameters& parameters, const TrafficState& ownship) const;

  virtual std::pair<Vect3, Vect3> trajectory(const DaidalusParameters& parameters, const TrafficState& ownship, double time, bool dir, int target_step, bool instantaneous) const;

  virtual double max_delta_resolution(const DaidalusParameters& parameters) const;

};

}

#endif
