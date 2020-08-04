/*
 * Copyright (c) 2015-2018 United States Government as represented by
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

	private double horizontal_accel_; // Horizontal acceleration

	public DaidalusHsBands(DaidalusParameters parameters) {
		setDaidalusParameters(parameters);
	}

	public DaidalusHsBands(DaidalusHsBands b) {
		super(b);
		horizontal_accel_ = b.horizontal_accel_;
	}

	/**
	 * Set DaidalusParmaeters 
	 */
	public void setDaidalusParameters(DaidalusParameters parameters) {
		set_step(parameters.getHorizontalSpeedStep());
		set_recovery(parameters.isEnabledRecoveryHorizontalSpeedBands());
		set_min_rel(parameters.getBelowRelativeHorizontalSpeed());
		set_max_rel(parameters.getAboveRelativeHorizontalSpeed());
		set_min_nomod(parameters.getMinHorizontalSpeed());
		set_max_nomod(parameters.getMaxHorizontalSpeed());
		set_horizontal_accel(parameters.getHorizontalAcceleration()); 
	}

	public boolean instantaneous_bands() {
		return horizontal_accel_ == 0;
	}

	public double get_horizontal_accel() {
		return horizontal_accel_;
	}

	public void set_horizontal_accel(double val) {
		if (val != horizontal_accel_) {
			horizontal_accel_ = val;
			stale(true);
		}
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().gs();
	}

	public double time_step(TrafficState ownship) {
		return get_step()/horizontal_accel_;
	}

	public Pair<Vect3, Velocity> trajectory(TrafficState ownship, double time, boolean dir) {    
		Pair<Position,Velocity> posvel;
		if (instantaneous_bands()) {
			double gs = ownship.velocityXYZ().gs()+(dir?1:-1)*j_step_*get_step(); 
			posvel = Pair.make(ownship.positionXYZ(),ownship.velocityXYZ().mkGs(gs));
		} else {
			posvel = ProjectedKinematics.gsAccel(ownship.positionXYZ(),ownship.velocityXYZ(),time,
					(dir?1:-1)*horizontal_accel_);
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredHorizontalSpeedResolution();
	}
	
}
