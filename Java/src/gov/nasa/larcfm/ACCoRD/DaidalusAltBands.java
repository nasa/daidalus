/*
 * Copyright (c) 2015-2019 United States Government as represented by
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
		return 1;
	}

	public Pair<Vect3, Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, double time, boolean dir) {
		double target_alt = get_min_val_()+j_step_*get_step(parameters);
		Pair<Position,Velocity> posvel;
		if (instantaneous_bands(parameters)) {
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
	private boolean conflict_free_traj_step(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, double B2, double T2,
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		boolean trajdir = true;
		if (instantaneous_bands(parameters)) {
			return no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,0.0,parameters,ownship,traffic);
		} else {
			double tstep = 1.0;
			double target_alt = get_min_val_()+j_step_*get_step(parameters);
			Tuple5<Double,Double,Double,Double,Double> tsqj = Kinematics.vsLevelOutTimes(ownship.positionXYZ().alt(),ownship.velocityXYZ().vs(),
					parameters.getVerticalRate(),target_alt,parameters.getVerticalAcceleration(),-parameters.getVerticalAcceleration(),true);
			double tsqj1 = tsqj.first+0.0;
			double tsqj2 = tsqj.second+0.0;
			double tsqj3 = tsqj.third+tstep;
			for (int i=0; i<=Math.floor(tsqj1/tstep);++i) {
				double tsi = i*tstep;
				if ((B<=tsi && tsi<=T && LOS_at(conflict_det,trajdir,tsi,parameters,ownship,traffic)) ||
						(recovery_det.isPresent() && B2 <= tsi && tsi <= T2 && 
						LOS_at(recovery_det.get(),trajdir,tsi,parameters,ownship,traffic))) { 
					return false;
				}
			}
			if ((tsqj2>=B && 
					CD_future_traj(conflict_det,B,Util.min(T,tsqj2),trajdir,Util.max(tsqj1,0),parameters,ownship,traffic)) || 
					(recovery_det.isPresent() && tsqj2>=B2 && 
					CD_future_traj(recovery_det.get(),B2,Util.min(T2,tsqj2),trajdir,Util.max(tsqj1,0),parameters,ownship,traffic))) {
				return false;
			}
			for (int i=(int)Math.ceil(tsqj2/tstep); i<=Math.floor(tsqj3/tstep);++i) {
				double tsi = i*tstep;
				if ((B<=tsi && tsi<=T && LOS_at(conflict_det,trajdir,tsi,parameters,ownship,traffic)) ||
						(recovery_det.isPresent() && B2 <= tsi && tsi <= T2 && 
						LOS_at(recovery_det.get(),trajdir,tsi,parameters,ownship,traffic))) { 
					return false;
				}
			}
			return no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,Util.max(tsqj3,0),parameters,ownship,traffic);
		}
	}

	// In PVS: alt_bands@alt_bands_generic
	private void alt_bands_generic(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, double B2, double T2,
			int maxup, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= maxup; ++k) {
			j_step_ = k;
			if (d >=0 && conflict_free_traj_step(conflict_det,recovery_det,B,T,B2,T2,parameters,ownship,traffic)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (conflict_free_traj_step(conflict_det,recovery_det,B,T,B2,T2,parameters,ownship,traffic)) {
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
		alt_bands_generic(altint,conflict_det,recovery_det,B,T,0,B,maxup,parameters,ownship,traffic);
		toIntervalSet(noneset,altint,get_step(parameters),get_min_val_());
	}

	public boolean any_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return first_band_alt_generic(conflict_det,recovery_det,B,T,0,B,parameters,ownship,traffic,true,false) >= 0 ||
				first_band_alt_generic(conflict_det,recovery_det,B,T,0,B,parameters,ownship,traffic,false,false) >= 0;
	}

	public boolean all_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		return first_band_alt_generic(conflict_det,recovery_det,B,T,0,B,parameters,ownship,traffic,true,true) < 0 &&
				first_band_alt_generic(conflict_det,recovery_det,B,T,0,B,parameters,ownship,traffic,false,true) < 0;
	}

	private int first_nat(int mini, int maxi, boolean dir, Detection3D conflict_det, Optional<Detection3D> recovery_det,
			double B, double T, double B2, double T2, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean green) {
		while (mini <= maxi) {
			j_step_ = mini;
			if (dir && green == conflict_free_traj_step(conflict_det,recovery_det,B,T,B2,T2,parameters,ownship,traffic)) {
				return j_step_; 
			} else if (dir) {
				++mini;
			} else {
				j_step_ = maxi;
				if (green == conflict_free_traj_step(conflict_det,recovery_det,B,T,B2,T2,parameters,ownship,traffic)) {
					return j_step_;
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
			double B, double T, double B2, double T2,
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean dir, boolean green) {
		int upper = (int)(dir ? Math.floor((get_max_val_()-get_min_val_())/get_step(parameters))+1 : 
			Math.floor((ownship.positionXYZ().alt()-get_min_val_())/get_step(parameters)));
		int lower = dir ? (int)(Math.ceil(ownship.positionXYZ().alt()-get_min_val_())/get_step(parameters)) : 0;
		if (ownship.positionXYZ().alt() < get_min_val_() || ownship.positionXYZ().alt() > get_max_val_()) {
			return -1;
		} else {
			return first_nat(lower,upper,dir,conflict_det,recovery_det,B,T,B2,T2,parameters,ownship,traffic,green);
		}
	}

	// dir=false is down, dir=true is up
	public double resolution(Detection3D conflict_det, Optional<Detection3D> recovery_det, int epsh, int epsv,
			double B, double T, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, boolean dir) {
		int ires = first_band_alt_generic(conflict_det,recovery_det,B,T,0,B,parameters,ownship,traffic,dir,true);
		if (ires < 0) {
			return (dir ? 1 : -1)*Double.POSITIVE_INFINITY;
		} else {
			return get_min_val_()+ires*get_step(parameters);
		}
	}

	public double max_delta_resolution(DaidalusParameters parameters) {
		return parameters.getPersistencePreferredAltitudeResolution();
	}

}
