/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import gov.nasa.larcfm.Util.IntervalSet;
import gov.nasa.larcfm.Util.Kinematics;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.ProjectedKinematics;
import gov.nasa.larcfm.Util.Tuple5;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

public class DaidalusAltBands extends DaidalusRealBands {

	public boolean get_recovery(DaidalusParameters parameters) {
		return parameters.isEnabledRecoveryAltitudeBands();
	}

	public DaidalusAltBands() {
	}

	public DaidalusAltBands(DaidalusAltBands b) {
		super(b);
	}

	public double get_step(DaidalusParameters parameters) {
		return parameters.getAltitudeStep();
	}

	public double get_min(DaidalusParameters parameters) {
		return parameters.getMinAltitude();
	}

	public double get_max(DaidalusParameters parameters) {
		return parameters.getMaxAltitude();
	}

	public double get_min_rel(DaidalusParameters parameters) {
		return parameters.getBelowRelativeAltitude();
	}

	public double get_max_rel(DaidalusParameters parameters) {
		return parameters.getAboveRelativeAltitude();
	}

	public void set_special_configuration(DaidalusParameters parameters, int dta_status) {	
		if (dta_status > 0) { 
			set_min_max_rel(0,-1);
		}
	}

	public boolean instantaneous_bands(DaidalusParameters parameters) {
		return parameters.getVerticalRate() == 0 || 
				parameters.getVerticalAcceleration() == 0;
	}

	public double own_val(TrafficState ownship) {
		return ownship.positionXYZ().alt();
	}

	public double time_step(DaidalusParameters parameters, TrafficState ownship) {
		return 1.0;
	}

	public Pair<Vect3, Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir, int target_step, boolean instantaneous) {
		double target_alt = get_min_val_()+target_step*get_step(parameters);
		Pair<Position,Velocity> posvel;
		if (instantaneous) {
			posvel = Pair.make(ownship.positionXYZ().mkZ(target_alt),ownship.velocityXYZ().mkVs(0));
		} else {
			double tsqj = ProjectedKinematics.vsLevelOutTime(ownship.positionXYZ(),ownship.velocityXYZ(),parameters.getVerticalRate(),
					target_alt,parameters.getVerticalAcceleration())+time_step(parameters,ownship);
			if (time <= tsqj) {
				posvel = ProjectedKinematics.vsLevelOut(ownship.positionXYZ(), ownship.velocityXYZ(), time, parameters.getVerticalRate(), target_alt, parameters.getVerticalAcceleration());
			} else {
				Position npo = ownship.positionXYZ().linear(ownship.velocityXYZ(),time);
				posvel = Pair.make(npo.mkZ(target_alt),ownship.velocityXYZ().mkVs(0));
			}
		}
		return Pair.make(ownship.pos_to_s(posvel.first),ownship.vel_to_v(posvel.first,posvel.second));
	}

	// In PVS: alt_bands@conflict_free_traj_step
	private boolean conflict_free_traj_step(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T,
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int target_step, boolean instantaneous) {
		boolean trajdir = true;
		if (instantaneous) {
			return no_CD_future_traj(conflict_det,recovery_det,B,T,trajdir,0.0,parameters,ownship,traffic,target_step,instantaneous);
		} else {
			double tstep = time_step(parameters,ownship);
			double target_alt = get_min_val_()+target_step*get_step(parameters);
			Tuple5<Double,Double,Double,Double,Double> tsqj = Kinematics.vsLevelOutTimes(ownship.positionXYZ().alt(),ownship.velocityXYZ().vs(),
					parameters.getVerticalRate(),target_alt,parameters.getVerticalAcceleration(),-parameters.getVerticalAcceleration(),true);
			double tsqj1 = tsqj.first;
			double tsqj2 = tsqj.second;
			double tsqj3 = tsqj.third+tstep;
			for (int i=0; i <= Math.floor(tsqj1/tstep);++i) {
				double tsi = i*tstep;
				if ((B <= tsi && LOS_at(conflict_det,trajdir,tsi,parameters,ownship,traffic,target_step,instantaneous)) ||
						(recovery_det.isPresent() && 0 <= tsi && tsi <= B && 
						LOS_at(recovery_det.get(),trajdir,tsi,parameters,ownship,traffic,target_step,instantaneous))) { 
					return false;
				}
			}
			double tsk1 = Util.max(tsqj1,0.0);
			if ((tsqj2 >= B && 
					CD_future_traj(conflict_det,B,Util.min(T+tsk1,tsqj2),trajdir,tsk1,parameters,ownship,traffic,target_step,instantaneous)) || 
					(recovery_det.isPresent() && tsqj2 >= 0 && 
					CD_future_traj(recovery_det.get(),0,Util.min(B,tsqj2),trajdir,Util.max(tsqj1,0),parameters,ownship,traffic,target_step,instantaneous))) {
				return false;
			}
			for (int i=(int)Math.ceil(tsqj2/tstep); i<=Math.floor(tsqj3/tstep);++i) {
				double tsi = i*tstep;
				if ((B <= tsi && LOS_at(conflict_det,trajdir,tsi,parameters,ownship,traffic,target_step,instantaneous)) ||
						(recovery_det.isPresent() && 0 <= tsi && tsi <= B && 
						LOS_at(recovery_det.get(),trajdir,tsi,parameters,ownship,traffic,target_step,instantaneous))) { 
					return false;
				}
			}
			double tsk3 = Util.max(tsqj3,0.0);
			return no_CD_future_traj(conflict_det,recovery_det,B,T+tsk3,trajdir,tsk3,parameters,ownship,traffic,target_step,instantaneous);
		}
	}

	// In PVS: alt_bands@alt_bands_generic
	private void alt_bands_generic(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T,
			int maxup, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean instantaneous) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= maxup; ++k) {
			if (d >=0 && conflict_free_traj_step(conflict_det,recovery_det,B,T,parameters,ownship,traffic,k,instantaneous)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (conflict_free_traj_step(conflict_det,recovery_det,B,T,parameters,ownship,traffic,k,instantaneous)) {
				d = k;
			}
		}
		if (d >= 0) {
			l.add(new Integerval(d,maxup));
		}
	}

	public void none_bands(IntervalSet noneset, Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {	
		int maxup = (int)Math.floor((get_max_val_()-get_min_val_())/get_step(parameters))+1;
		List<Integerval> altint = new ArrayList<Integerval>();
		alt_bands_generic(altint,conflict_det,recovery_det,B,T,maxup,parameters,ownship,traffic,instantaneous_bands(parameters));
		toIntervalSet(noneset,altint,get_step(parameters),get_min_val_());
	}

	public boolean any_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return first_band_alt_generic(conflict_det,recovery_det,B,T,parameters,ownship,traffic,true,false,instantaneous_bands(parameters)) >= 0 ||
				first_band_alt_generic(conflict_det,recovery_det,B,T,parameters,ownship,traffic,false,false,instantaneous_bands(parameters)) >= 0;
	}

	public boolean all_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return first_band_alt_generic(conflict_det,recovery_det,B,T,parameters,ownship,traffic,true,true,instantaneous_bands(parameters)) < 0 &&
				first_band_alt_generic(conflict_det,recovery_det,B,T,parameters,ownship,traffic,false,true,instantaneous_bands(parameters)) < 0;
	}

	private int first_nat(int mini, int maxi, boolean dir, Detection3D conflict_det, Optional<Detection3D> recovery_det,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean green, boolean instantaneous) {
		while (mini <= maxi) {
			if (dir && green == conflict_free_traj_step(conflict_det,recovery_det,B,T,parameters,ownship,traffic,mini,instantaneous)) {
				return mini; 
			} else if (dir) {
				++mini;
			} else {
				if (green == conflict_free_traj_step(conflict_det,recovery_det,B,T,parameters,ownship,traffic,maxi,instantaneous)) {
					return maxi;
				} else if (maxi == 0) {
					return -1;
				} else {
					--maxi;
				}
			}
		}
		return -1;
	}

	private int first_band_alt_generic(Detection3D conflict_det, Optional<Detection3D> recovery_det,
			double B, double T,
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean dir, boolean green, boolean instantaneous) {
		int upper = (int)(dir ? Math.floor((get_max_val_()-get_min_val_())/get_step(parameters))+1 : 
			Math.floor((ownship.positionXYZ().alt()-get_min_val_())/get_step(parameters)));
		int lower = dir ? (int)(Math.ceil(ownship.positionXYZ().alt()-get_min_val_())/get_step(parameters)) : 0;
		if (ownship.positionXYZ().alt() < get_min_val_() || ownship.positionXYZ().alt() > get_max_val_()) {
			return -1;
		} else {
			return first_nat(lower,upper,dir,conflict_det,recovery_det,B,T,parameters,ownship,traffic,green,instantaneous);
		}
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredAltitudeResolution();
	}

}
