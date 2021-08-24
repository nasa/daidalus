/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

/* Kinematic Horizontal Speed Bands */

public class DaidalusHsBands extends DaidalusRealBands {

	public DaidalusHsBands() {
	}

	public DaidalusHsBands(DaidalusHsBands b) {
		super(b);
	}

	public boolean get_recovery(DaidalusParameters parameters) {
		return parameters.isEnabledRecoveryHorizontalSpeedBands();
	}

	public double get_step(DaidalusParameters parameters) {
		return parameters.getHorizontalSpeedStep();
	}

	public double get_min(DaidalusParameters parameters) {
		return parameters.getMinHorizontalSpeed();
	}

	public double get_max(DaidalusParameters parameters) {
		return parameters.getMaxHorizontalSpeed();
	}

	public double get_min_rel(DaidalusParameters parameters) {
		return parameters.getBelowRelativeHorizontalSpeed();
	}

	public double get_max_rel(DaidalusParameters parameters) {
		return parameters.getAboveRelativeHorizontalSpeed();
	}

	public boolean instantaneous_bands(DaidalusParameters parameters) {
		return parameters.getHorizontalAcceleration() == 0;
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().gs();
	}

	public double time_step(DaidalusParameters parameters, TrafficState ownship) {
		return get_step(parameters)/parameters.getHorizontalAcceleration();
	}

	public Pair<Vect3, Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir, int target_step, boolean instantaneous) {    
		Pair<Position,Velocity> posvel;
		if (time == 0 && target_step == 0) {
			return Pair.make(ownship.get_s(),ownship.get_v());
		} else if (instantaneous) {
			double gs = ownship.velocityXYZ().gs()+(dir?1:-1)*target_step*get_step(parameters); 
			posvel = Pair.make(ownship.positionXYZ(),ownship.velocityXYZ().mkGs(gs));
		} else {
			posvel = ProjectedKinematics.gsAccel(ownship.positionXYZ(),ownship.velocityXYZ(),time,
					(dir?1:-1)*parameters.getHorizontalAcceleration());
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredHorizontalSpeedResolution();
	}

}
