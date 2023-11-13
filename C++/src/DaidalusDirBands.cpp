/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#include "DaidalusDirBands.h"
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

DaidalusDirBands::DaidalusDirBands() : DaidalusRealBands(2*Pi), inst_below_min_as(false) {}

DaidalusDirBands::DaidalusDirBands(const DaidalusDirBands& b) : DaidalusRealBands(b), inst_below_min_as(false) {}

bool DaidalusDirBands::do_recovery(const DaidalusParameters& parameters) const {
  return parameters.isEnabledRecoveryHorizontalDirectionBands();
}

double DaidalusDirBands::get_step(const DaidalusParameters& parameters) const {
  return parameters.getHorizontalDirectionStep();
}

double DaidalusDirBands::get_min(const DaidalusParameters& parameters) const {
  return 0.0;
}

double DaidalusDirBands::get_max(const DaidalusParameters& parameters) const {
  return get_mod();
}

double DaidalusDirBands::get_min_rel(const DaidalusParameters& parameters) const {
  return parameters.getLeftHorizontalDirection();
}

double DaidalusDirBands::get_max_rel(const DaidalusParameters& parameters) const {
  return parameters.getRightHorizontalDirection();
}

bool DaidalusDirBands::saturate_corrective_bands(const DaidalusParameters& parameters, const SpecialBandFlags& special_flags) const {
  return special_flags.get_dta_status() > 0.0 && parameters.getDTALogic() < 0.0;
}

void DaidalusDirBands::set_special_configuration(const DaidalusParameters& parameters, const SpecialBandFlags& special_flags) {
  inst_below_min_as = special_flags.get_below_min_as() && parameters.getHorizontalDirBandsBelowMinAirspeed() > 0;
}

bool DaidalusDirBands::instantaneous_bands(const DaidalusParameters& parameters) const {
  return (parameters.getTurnRate() == 0.0 && parameters.getBankAngle() == 0.0) || inst_below_min_as;
}

double DaidalusDirBands::own_val(const TrafficState& ownship) const {
  return ownship.velocityXYZ().compassAngle();
}

double DaidalusDirBands::time_step(const DaidalusParameters& parameters, const TrafficState& ownship) const {
	double gso = std::max(parameters.getHorizontalSpeedStep(),std::max(parameters.getMinAirSpeed(),ownship.velocityXYZ().gs()));
  double omega = parameters.getTurnRate() == 0.0 ? Kinematics::turnRate(gso,parameters.getBankAngle()) : parameters.getTurnRate();
  	if (omega == 0.0) {
			return 0.0;
		} else {
      return get_step(parameters)/omega;
    }
}

Velocity DaidalusDirBands::ownship_vel(const DaidalusParameters& parameters, const TrafficState& ownship) const {
 	double gso = std::max(parameters.getHorizontalSpeedStep(),std::max(parameters.getMinAirSpeed(),ownship.velocityXYZ().gs()));
	return ownship.velocityXYZ().mkGs(gso);
}

std::pair<Vect3, Vect3> DaidalusDirBands::trajectory(const DaidalusParameters& parameters, const TrafficState& ownship, double time, bool dir, int target_step, bool instantaneous) const {
  std::pair<Position,Velocity> posvel;
  Velocity ownship_velocityXYZ = ownship_vel(parameters,ownship);
  if (time == 0.0 && target_step == 0.0) {
    return std::pair<Vect3, Vect3>(ownship.get_s(),ownship_velocityXYZ.vect3());
  } else if (instantaneous) {
    double trk = ownship_velocityXYZ.compassAngle()+(dir?1:-1)*target_step*get_step(parameters);
    posvel = std::pair<Position, Velocity>(ownship.positionXYZ(),ownship_velocityXYZ.mkTrk(trk));
  } else {
    double gso = ownship_velocityXYZ.gs();
    double bank = parameters.getTurnRate() == 0.0 ? parameters.getBankAngle() : std::abs(Kinematics::bankAngle(gso,parameters.getTurnRate()));
    double R = Kinematics::turnRadius(gso,bank);
    posvel = ProjectedKinematics::turn(ownship.positionXYZ(),ownship_velocityXYZ,time,R,dir);
  }
  return std::pair<Vect3, Vect3>(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
}

double DaidalusDirBands::max_delta_resolution(const DaidalusParameters& parameters) const {
  return parameters.getPersistencePreferredHorizontalDirectionResolution();
}

std::string DaidalusDirBands::rawString() const {
  std::string s(DaidalusRealBands::rawString());
	s += "inst_below_min_as = "+Fmb(inst_below_min_as)+"\n";
	return s;
}

}

