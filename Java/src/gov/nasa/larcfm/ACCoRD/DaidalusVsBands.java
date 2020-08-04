/*
 * Copyright (c) 2015-2019 United States Government as represented by
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

public class DaidalusVsBands extends DaidalusRealBands {

	private double vertical_accel_; // Climb/descend acceleration

	public DaidalusVsBands(DaidalusParameters parameters) {
		setDaidalusParameters(parameters);
	}

	public DaidalusVsBands(DaidalusVsBands b) {
		super(b);
		vertical_accel_ = b.vertical_accel_;
	}

	/**
	 * Set DaidalusParmaeters 
	 */
	public void setDaidalusParameters(DaidalusParameters parameters) {
		set_step(parameters.getVerticalSpeedStep()); 
		set_recovery(parameters.isEnabledRecoveryVerticalSpeedBands());   
		set_min_rel(parameters.getBelowRelativeVerticalSpeed());
		set_max_rel(parameters.getAboveRelativeVerticalSpeed());
		set_min_nomod(parameters.getMinVerticalSpeed());
		set_max_nomod(parameters.getMaxVerticalSpeed());
		set_vertical_accel(parameters.getVerticalAcceleration());
	}

	public boolean instantaneous_bands() {
		return vertical_accel_ == 0;
	}

	public double get_vertical_accel() {
		return vertical_accel_;
	}

	public void set_vertical_accel(double val) {
		if (val != vertical_accel_) {
			vertical_accel_ = val;
			stale(true);
		}
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().vs();
	}

	public double time_step(TrafficState ownship) {
		return get_step()/vertical_accel_;
	}

	public Pair<Vect3, Velocity> trajectory(TrafficState ownship, double time, boolean dir) {    
		Pair<Position,Velocity> posvel;
		if (instantaneous_bands()) {
			double vs = ownship.velocityXYZ().vs()+(dir?1:-1)*j_step_*get_step(); 
			posvel = Pair.make(ownship.positionXYZ(),ownship.velocityXYZ().mkVs(vs));
		} else {
			posvel = ProjectedKinematics.vsAccel(ownship.positionXYZ(),
					ownship.velocityXYZ(),time,(dir?1:-1)*vertical_accel_);
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredVerticalSpeedResolution();
	}

}
