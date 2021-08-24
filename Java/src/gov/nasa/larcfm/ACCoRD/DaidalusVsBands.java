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

public class DaidalusVsBands extends DaidalusRealBands {

	public DaidalusVsBands() {
	}

	public DaidalusVsBands(DaidalusVsBands b) {
		super(b);
	}

	public boolean get_recovery(DaidalusParameters parameters) {
		return parameters.isEnabledRecoveryVerticalSpeedBands();
	}

	public double get_step(DaidalusParameters parameters) {
		return parameters.getVerticalSpeedStep();
	}

	public double get_min(DaidalusParameters parameters) {
		return parameters.getMinVerticalSpeed();
	}

	public double get_max(DaidalusParameters parameters) {
		return parameters.getMaxVerticalSpeed();
	}

	public double get_min_rel(DaidalusParameters parameters) {
		return parameters.getBelowRelativeVerticalSpeed();
	}

	public double get_max_rel(DaidalusParameters parameters) {
		return parameters.getAboveRelativeVerticalSpeed();
	}

	public void set_special_configuration(DaidalusParameters parameters, int dta_status) {	
		if (dta_status > 0) { 
			set_min_max_rel(0,-1);
		}
	}

	public boolean instantaneous_bands(DaidalusParameters parameters) {
		return parameters.getVerticalAcceleration() == 0;
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().vs();
	}

	public double time_step(DaidalusParameters parameters,TrafficState ownship) {
		return get_step(parameters)/parameters.getVerticalAcceleration();
	}

	public Pair<Vect3, Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir, int target_step, boolean instantaneous) {    
		Pair<Position,Velocity> posvel;
		if (time == 0 && target_step == 0) {
			return Pair.make(ownship.get_s(),ownship.get_v());
		} else if (instantaneous) {
			double vs = ownship.velocityXYZ().vs()+(dir?1:-1)*target_step*get_step(parameters); 
			posvel = Pair.make(ownship.positionXYZ(),ownship.velocityXYZ().mkVs(vs));
		} else {
			posvel = ProjectedKinematics.vsAccel(ownship.positionXYZ(),
					ownship.velocityXYZ(),time,(dir?1:-1)*parameters.getVerticalAcceleration());
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredVerticalSpeedResolution();
	}

}
