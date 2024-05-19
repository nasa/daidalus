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
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

public class DaidalusDirBands extends DaidalusRealBands {

	/** 
	 * 0: Special horizontal dir bands logic when airspeed is below airspeed_thr is disabled
	 * 1: Instantaneous horizontal direction bands computed assuming airspeed_thr
	 * -1; Kinematic horizontal direction bands computed assumming airspeed_thr
	 */
	private int below_as_thr_;

	// min/max is left/right relative to ownship's direction
	public DaidalusDirBands() {
		super(2*Math.PI);
	}

	public DaidalusDirBands(DaidalusDirBands b) {
		super(b);
	}

	public boolean do_recovery(DaidalusParameters parameters) {
		return parameters.isEnabledRecoveryHorizontalDirectionBands();
	}

	public double get_step(DaidalusParameters parameters) {
		return parameters.getHorizontalDirectionStep();
	}

	public double get_min(DaidalusParameters parameters) {
		return 0.0;
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

	public boolean saturate_corrective_bands(DaidalusParameters parameters, SpecialBandFlags special_flags) {
		return special_flags.dta_status > 0 && parameters.getDTALogic() < 0;
	}

	public void set_special_configuration(DaidalusParameters parameters, SpecialBandFlags special_flags) {
		if (special_flags.below_as_thr) {
			below_as_thr_ = parameters.getHorizontalDirBandsBelowAirspeedThr();
		} else {
			below_as_thr_ = 0;
		}
	}

	public boolean instantaneous_bands(DaidalusParameters parameters) {
		return (parameters.getTurnRate() == 0.0 && parameters.getBankAngle() == 0.0) || below_as_thr_ > 0;
	}

	public double own_val(TrafficState ownship) {
		return ownship.velocityXYZ().compassAngle();
	}

	public double time_step(DaidalusParameters parameters, TrafficState ownship) {
		double gso = ownship_gs(parameters,ownship);
		double omega = parameters.getTurnRate() == 0.0 ? Kinematics.turnRate(gso,parameters.getBankAngle()) : parameters.getTurnRate();
		if (omega == 0.0) {
			return 0.0;
		} else {
			return get_step(parameters)/omega;
		}
	}

	private double ownship_gs(DaidalusParameters parameters, TrafficState ownship) {
		double gso = below_as_thr_ == 0 ? ownship.velocityXYZ().gs() : 
			Util.max(parameters.getAirspeedThreshold(),ownship.velocityXYZ().gs());
		return  Util.max(parameters.getHorizontalSpeedStep(),gso);
	}

	private Velocity ownship_vel(DaidalusParameters parameters, TrafficState ownship) {
		return ownship.velocityXYZ().mkGs(ownship_gs(parameters,ownship));
	}

	public Pair<Vect3, Vect3> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir, int target_step, boolean instantaneous) {  
		Pair<Position,Velocity> posvel;
		Velocity ownship_velocityXYZ = ownship_vel(parameters,ownship);
		if (time == 0.0 && target_step == 0.0) {
			return Pair.make(ownship.get_s(),ownship_velocityXYZ.vect3());
		} else if (instantaneous) {
			double trk = ownship_velocityXYZ.compassAngle()+(dir?1:-1)*target_step*get_step(parameters); 
			posvel = Pair.make(ownship.positionXYZ(),ownship_velocityXYZ.mkTrk(trk));
		} else {
			double gso = ownship_velocityXYZ.gs();
			double bank = parameters.getTurnRate() == 0.0 ? parameters.getBankAngle() : Math.abs(Kinematics.bankAngle(gso,parameters.getTurnRate()));
			double R = Kinematics.turnRadius(gso,bank);
			posvel = ProjectedKinematics.turn(ownship.positionXYZ(),ownship_velocityXYZ,time,R,dir);
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredHorizontalDirectionResolution();
	}

	public String rawString() {
		String s = super.rawString();
		s += "below_as_thr_ = "+below_as_thr_+"\n";
		return s;
	}

}
