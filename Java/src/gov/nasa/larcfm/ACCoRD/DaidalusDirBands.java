/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Kinematics;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

public class DaidalusDirBands extends DaidalusRealBands {

	// min/max is left/right relative to ownship's direction
	public DaidalusDirBands() {
		super(2*Math.PI);
	}

	public DaidalusDirBands(DaidalusDirBands b) {
		super(b);
	}

	public boolean get_recovery(DaidalusParameters parameters) {
		return parameters.isEnabledRecoveryHorizontalDirectionBands();
	}

	public double get_step(DaidalusParameters parameters) {
		return parameters.getHorizontalDirectionStep();
	}

	public double get_min(DaidalusParameters parameters) {
		return 0;
	}

	public double get_max(DaidalusParameters parameters) {
		return get_mod();
	}

	public double get_min_rel(DaidalusParameters parameters) {
		return parameters.getLeftHorizontalDirection();
	}

	public double get_max_rel(DaidalusParameters parameters) {
		return parameters.getRightHorizontalDirection();
	}

	public boolean saturate_corrective_bands(DaidalusParameters parameters, int dta_status) {
		return dta_status > 0 && parameters.getDTALogic() < 0;
	}

	public boolean instantaneous_bands(DaidalusParameters parameters) {
		return parameters.getTurnRate() == 0 && parameters.getBankAngle() == 0;
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().compassAngle();
	}

	public double time_step(DaidalusParameters parameters, TrafficState ownship) {
		double gso = ownship.velocityXYZ().gs();
		double omega = parameters.getTurnRate() == 0 ? Kinematics.turnRate(gso,parameters.getBankAngle()) : parameters.getTurnRate();
		return get_step(parameters)/omega;
	}

	public Pair<Vect3, Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir, int target_step, boolean instantaneous) {  
		Pair<Position,Velocity> posvel;
		if (time == 0 && target_step == 0) {
			return Pair.make(ownship.get_s(),ownship.get_v());
		} else if (instantaneous) {
			double trk = ownship.velocityXYZ().compassAngle()+(dir?1:-1)*target_step*get_step(parameters); 
			posvel = Pair.make(ownship.positionXYZ(),ownship.velocityXYZ().mkTrk(trk));
		} else {
			double gso = ownship.velocityXYZ().gs();
			double bank = parameters.getTurnRate() == 0 ? parameters.getBankAngle() : Math.abs(Kinematics.bankAngle(gso,parameters.getTurnRate()));
			double R = Kinematics.turnRadius(gso,bank);
			posvel = ProjectedKinematics.turn(ownship.positionXYZ(),ownship.velocityXYZ(),time,R,dir);
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredHorizontalDirectionResolution();
	}

}
