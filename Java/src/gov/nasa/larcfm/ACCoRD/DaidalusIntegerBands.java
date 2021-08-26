/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DaidalusIntegerBands {

	// This class computes NONE bands

	// trajdir == false is left/down
	// target_step is used by instantaneous_bands and altitude_bands
	public abstract Pair<Vect3,Velocity> trajectory(DaidalusParameters parameters, TrafficState ownship, 
			double time, boolean dir, int target_step, boolean instantaneous);

	/**
	 *  In PVS: int_bands@CD_future_traj
	 */
	public boolean CD_future_traj(Detection3D det, double B, double T, boolean trajdir, double tsk, 
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int target_step, boolean instantaneous) {
		T = Util.min(parameters.getLookaheadTime(),T);
		if (tsk > T || B > T) return false;
		Pair<Vect3,Velocity> sovot = trajectory(parameters,ownship,tsk,trajdir,target_step,instantaneous);
		Vect3 sot = sovot.first;
		Velocity vot = sovot.second;
		Vect3 sat = tsk == 0.0 ? sot : vot.ScalAdd(-tsk,sot);
		TrafficState own = new TrafficState(ownship);
		own.setPosition(Position.make(sat));
		own.setAirVelocity(vot);
		return det.conflictWithTrafficState(own,traffic,Util.max(B,tsk),T);
	}

	/** 
	 * In PVS: 
	 * NOT CD_future_traj(CD,B,T,traj,kts,si,vi)) AND (NOT (useLOS2 AND CD_future_traj(CD2,traj,kts,si,vi)
	 */ 
	public boolean no_CD_future_traj(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, boolean trajdir, double tsk, 
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int target_step, boolean instantaneous) {
		return !CD_future_traj(conflict_det,B,T,trajdir,tsk,parameters,ownship,traffic,target_step,instantaneous) &&
				!(recovery_det.isPresent() && CD_future_traj(recovery_det.get(),0,B,trajdir,tsk,parameters,ownship,traffic,target_step,instantaneous));
	}

	/** 
	 * In PVS: 
	 * LET kts = k*ts,
	 * sot=traj(kts)`1,
	 * vot=traj(kts)`2 IN
	 * LOS_at(sot-kts*vot,vot,si,vi,kts)
	 */ 
	public boolean LOS_at(Detection3D det, boolean trajdir, double tsk, 
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int target_step, boolean instantaneous) {
		if (tsk >= parameters.getLookaheadTime()) {
			return false;
		}
		Pair<Vect3,Velocity> sovot = trajectory(parameters,ownship,tsk,trajdir,target_step,instantaneous);
		Vect3 sot = sovot.first;
		Velocity vot = sovot.second;
		Vect3 sat = vot.ScalAdd(-tsk,sot);
		TrafficState own = new TrafficState(ownship);
		own.setPosition(Position.make(sat));
		own.setAirVelocity(vot);
		return det.violationAtWithTrafficState(own,traffic,tsk); 
	}

	// In PVS: int_bands@first_los_step
	private int kinematic_first_los_step(Detection3D det, double tstep, boolean trajdir,
			int min, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		for (int k=min; k<=max; ++k) {
			if (LOS_at(det,trajdir,k*tstep,parameters,ownship,traffic,0,false)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: kinematic_bands@first_los_search_index
	private int kinematic_first_los_search_index(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) { 
		int FirstLosK = (int)Math.ceil(B/tstep); // first k such that k*ts>=B
		int FirstLosN = Util.min((int)Math.floor(parameters.getLookaheadTime()/tstep),max); // last k<=MaxN such that k*ts<=T
		int FirstLosK2 = 0;
		int FirstLosN2 = Util.min((int)Math.floor(B/tstep),max); 
		int FirstLosInit = recovery_det.isPresent() ? kinematic_first_los_step(recovery_det.get(),tstep,trajdir,FirstLosK2,FirstLosN2,parameters,ownship,traffic) : -1;
		int FirstLos = kinematic_first_los_step(conflict_det,tstep,trajdir,FirstLosK,FirstLosN,parameters,ownship,traffic);
		int LosInitIndex = FirstLosInit < 0 ? max+1 : FirstLosInit;
		int LosIndex = FirstLos < 0 ? max+1 : FirstLos;
		return Util.min(LosInitIndex,LosIndex);
	} 

	// In PVS: kinematic_bands@bands_search_index
	// epsh == epsv == 0, if traffic is not the repulsive aircraft
	private int kinematic_bands_search_index(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;
		int FirstLos = kinematic_first_los_search_index(conflict_det,recovery_det,tstep,B,trajdir,max,parameters,ownship,traffic);
		int FirstNonHRep = !usehcrit || FirstLos == 0 ? FirstLos :
			kinematic_first_nonrepulsive_step(tstep,trajdir,FirstLos-1,parameters,ownship,traffic,epsh);
		int FirstProbHcrit = FirstNonHRep < 0 ? max+1 : FirstNonHRep;
		int FirstProbHL = Util.min(FirstLos,FirstProbHcrit);
		int FirstNonVRep = !usevcrit || FirstProbHL == 0 ? FirstProbHL :
			kinematic_first_nonvert_repul_step(tstep,trajdir,FirstProbHL-1,parameters,ownship,traffic,epsv);
		int FirstProbVcrit = FirstNonVRep < 0 ? max+1 : FirstNonVRep;
		return Util.min(FirstProbHL,FirstProbVcrit);  
	}

	// In PVS: int_bands@traj_conflict_only_band, int_bands@nat_bands, and int_bands@nat_bands_rec
	private void kinematic_traj_conflict_only_bands(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= max; ++k) {
			double tsk = tstep*k;
			if (d >=0 && no_CD_future_traj(conflict_det,recovery_det,B,T+tsk,trajdir,tsk,parameters,ownship,traffic,0,false)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (no_CD_future_traj(conflict_det,recovery_det,B,T+tsk,trajdir,tsk,parameters,ownship,traffic,0,false)) {
				d = k;
			}
		}
		if (d >= 0 && d != max) {
			l.add(new Integerval(d,max));
		}
	}

	// In PVS: kinematic_bands@kinematic_bands
	private void kinematic_bands(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, 
			double B, double T, 
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		l.clear();
		int bsi = kinematic_bands_search_index(conflict_det,recovery_det,tstep,B,trajdir,max,parameters,ownship,traffic,epsh,epsv);
		if  (bsi != 0) {
			kinematic_traj_conflict_only_bands(l,conflict_det,recovery_det,tstep,B,T,trajdir,bsi-1,parameters,ownship,traffic);
		}
	}

	// In PVS: kinematic_bands_exist@first_green
	private int first_kinematic_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;    
		for (int k=0; k <= max; ++k) {
			double tsk = tstep*k;
			if ((B <= tsk && LOS_at(conflict_det,trajdir,tsk,parameters,ownship,traffic,0,false)) ||
					(recovery_det.isPresent() && 0 <= tsk && tsk <= B &&
					LOS_at(recovery_det.get(),trajdir,tsk,parameters,ownship,traffic,0,false)) ||
					(usehcrit && !kinematic_repulsive_at(tstep,trajdir,k,parameters,ownship,traffic,epsh)) ||
					(usevcrit && !kinematic_vert_repul_at(tstep,trajdir,k,parameters,ownship,traffic,epsv))) {
				return -1;
			} else if (no_CD_future_traj(conflict_det,recovery_det,B,T+tsk,trajdir,tsk,parameters,ownship,traffic,0,false)) {
				return k;
			} 
		}      
		return -1;
	}

	private Vect3 kinematic_linvel(DaidalusParameters parameters, TrafficState ownship, double tstep, boolean trajdir, int k) {
		Vect3 s1 = trajectory(parameters,ownship,(k+1)*tstep,trajdir,0,false).first; 
		Vect3 s0 = trajectory(parameters,ownship,k*tstep,trajdir,0,false).first; 
		return s1.Sub(s0).Scal(1/tstep);
	}

	// In PVS: int_bands@repulsive_at
	private boolean kinematic_repulsive_at(double tstep, boolean trajdir, int k, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int epsh) {
		// k >= 0
		if (k==0) {
			return true;
		}
		Pair<Vect3,Velocity> sovo = trajectory(parameters,ownship,0,trajdir,0,false);
		Vect2 so = sovo.first.vect2();
		Vect2 vo = sovo.second.vect2();
		Vect2 si = traffic.get_s().vect2();
		Vect2 vi = traffic.get_v().vect2();
		boolean rep = true;
		if (k==1) { 
			rep = CriteriaCore.horizontal_new_repulsive_criterion(so.Sub(si),vo,vi,kinematic_linvel(parameters,ownship,tstep,trajdir,0).vect2(),epsh);
		}
		if (rep) {
			Pair<Vect3,Velocity> sovot = trajectory(parameters,ownship,k*tstep,trajdir,0,false);
			Vect2 sot = sovot.first.vect2();
			Vect2 vot = sovot.second.vect2();
			Vect2 sit = vi.ScalAdd(k*tstep,si);
			Vect2 st = sot.Sub(sit);
			Vect2 vop = kinematic_linvel(parameters,ownship,tstep,trajdir,k-1).vect2();
			Vect2 vok = kinematic_linvel(parameters,ownship,tstep,trajdir,k).vect2();
			return CriteriaCore.horizontal_new_repulsive_criterion(st,vop,vi,vot,epsh) &&
					CriteriaCore.horizontal_new_repulsive_criterion(st,vot,vi,vok,epsh) &&
					CriteriaCore.horizontal_new_repulsive_criterion(st,vop,vi,vok,epsh);    
		}
		return false;
	}

	// In PVS: int_bands@first_nonrepulsive_step
	private int kinematic_first_nonrepulsive_step(double tstep, boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int epsh) {
		for (int k=0; k <= max; ++k) {
			if (!kinematic_repulsive_at(tstep,trajdir,k,parameters,ownship,traffic,epsh)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: int_bands@vert_repul_at
	private boolean kinematic_vert_repul_at(double tstep, boolean trajdir, int k, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int epsv) {
		// traffic is valid and k >= 0
		if (k==0) {
			return true;
		}
		Pair<Vect3,Velocity> sovo = trajectory(parameters,ownship,0,trajdir,0,false);
		Vect3 so = sovo.first;
		Vect3 vo = sovo.second;
		Vect3 si = traffic.get_s();
		Vect3 vi = traffic.get_v();
		boolean rep = true;
		if (k==1) { 
			rep = CriteriaCore.vertical_new_repulsive_criterion(so.Sub(si),vo,vi,kinematic_linvel(parameters,ownship,tstep,trajdir,0),epsv);
		}
		if (rep) {
			Pair<Vect3,Velocity> sovot = trajectory(parameters,ownship,k*tstep,trajdir,0,false);
			Vect3 sot = sovot.first;
			Vect3 vot = sovot.second;
			Vect3 sit = vi.ScalAdd(k*tstep,si);
			Vect3 st = sot.Sub(sit);
			Vect3 vop = kinematic_linvel(parameters,ownship,tstep,trajdir,k-1);
			Vect3 vok = kinematic_linvel(parameters,ownship,tstep,trajdir,k);
			return CriteriaCore.vertical_new_repulsive_criterion(st,vop,vi,vot,epsv) &&
					CriteriaCore.vertical_new_repulsive_criterion(st,vot,vi,vok,epsv) &&
					CriteriaCore.vertical_new_repulsive_criterion(st,vop,vi,vok,epsv);    
		}
		return false;
	}

	// In PVS: int_bands@first_nonvert_repul_step
	private int kinematic_first_nonvert_repul_step(double tstep, boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, int epsv) {
		for (int k=0; k <= max; ++k) {
			if (!kinematic_vert_repul_at(tstep,trajdir,k,parameters,ownship,traffic,epsv)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: first_conflict_step(CD,B,T,traj,0,ts,si,vi,MaxN) >= 0
	private boolean kinematic_any_conflict_step(Detection3D det, double tstep, double B, double T, boolean trajdir, int max, 
			DaidalusParameters parameters, TrafficState ownship, TrafficState traffic) {
		for (int k=0; k <= max; ++k) {
			double tsk = tstep*k;
			if (CD_future_traj(det,B,T+tsk,trajdir,tsk,parameters,ownship,traffic,0,false)) {
				return true;
			}
		}
		return false;
	}

	// In PVS: kinematic_bands_exist@red_band_exist
	private boolean kinematic_red_band_exist(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, 
			double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic,
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;
		return (usehcrit && kinematic_first_nonrepulsive_step(tstep,trajdir,max,parameters,ownship,traffic,epsh) >= 0) ||
				(usevcrit && kinematic_first_nonvert_repul_step(tstep,trajdir,max,parameters,ownship,traffic,epsv) >= 0) ||
				kinematic_any_conflict_step(conflict_det,tstep,B,T,trajdir,max,parameters,ownship,traffic) ||
				(recovery_det.isPresent() && kinematic_any_conflict_step(recovery_det.get(),tstep,0,B,trajdir,max,parameters,ownship,traffic));
	}

	private int first_instantaneous_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		for (int k = 0; k <= max; ++k) {
			if (no_instantaneous_conflict(conflict_det,recovery_det,B,T,trajdir,parameters,ownship,traffic,epsh,epsv,k)) {
				return k;
			}
		}
		return -1;
	}

	//In PVS: inst_bands@conflict_free_track_step, inst_bands@conflict_free_gs_step, inst_bands@conflict_free_vs_step
	private boolean no_instantaneous_conflict(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T,
			boolean trajdir, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic,
			int epsh, int epsv, int target_step) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;    
		Pair<Vect3,Velocity> nsovo = trajectory(parameters,ownship,0,trajdir,target_step,true);
		Vect3 so = ownship.get_s();
		Velocity vo = ownship.get_v();
		Vect3 si = traffic.get_s();
		Velocity vi = traffic.get_v();
		Velocity nvo = nsovo.second;
		Vect3 s = so.Sub(si);
		return 
				(!usehcrit || CriteriaCore.horizontal_new_repulsive_criterion(s.vect2(),vo.vect2(),vi.vect2(),nvo.vect2(),epsh)) &&
				(!usevcrit || CriteriaCore.vertical_new_repulsive_criterion(s,vo,vi,nvo,epsv)) && 
				no_CD_future_traj(conflict_det,recovery_det,B,T,trajdir,0.0,parameters,ownship,traffic,target_step,true);
	}

	//In PVS: int_bands@nat_bands, int_bands@nat_bands_rec 
	private void instantaneous_bands(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic,
			int epsh, int epsv) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= max; ++k) {
			if (d >=0 && no_instantaneous_conflict(conflict_det,recovery_det,B,T,trajdir,parameters,ownship,traffic,epsh,epsv,k)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (no_instantaneous_conflict(conflict_det,recovery_det,B,T,trajdir,parameters,ownship,traffic,epsh,epsv,k)) {
				d = k;
			}
		}
		if (d >= 0 && d != max) {
			l.add(new Integerval(d,max));
		}
	}

	private boolean instantaneous_red_band_exist(Detection3D conflict_det, Optional<Detection3D> recovery_det,  
			double B, double T,
			boolean trajdir, int max, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		for (int k = 0; k <= max; ++k) {
			if (!no_instantaneous_conflict(conflict_det,recovery_det,B,T,trajdir,parameters,ownship,traffic,epsh,epsv,k)) {
				return true;
			}
		}
		return false;
	}

	public static void append_intband(List<Integerval> l, List<Integerval> r) {
		// Append in place
		int last = l.size()-1;
		if (!l.isEmpty() && !r.isEmpty() && r.get(0).lb-l.get(last).ub <= 1) {
			l.get(last).ub = r.get(0).ub;
			r.remove(0);
		}
		l.addAll(r);
	}

	public static void neg(List<Integerval> l) {
		// Negate, flip, and reverse in place
		if (l.isEmpty()) return;
		int mid = (l.size()-1)/2;
		boolean odd = l.size() % 2 != 0;
		for (int i=0; i <= mid; ++i) {
			if (i == mid && odd) {
				int x = l.get(i).lb;
				l.get(i).lb = -l.get(i).ub;
				l.get(i).ub = -x;
			} else {
				int i2 = l.size()-i-1;
				int x = l.get(i).lb;
				l.get(i).lb = -l.get(i2).ub;
				l.get(i2).ub = -x;
				x = l.get(i).ub;
				l.get(i).ub = -l.get(i2).lb;
				l.get(i2).lb = -x;
			}
		}
	}

	// In PVS: combine_bands@kinematic_bands_combine
	private void kinematic_bands_combine(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		kinematic_bands(l,conflict_det,recovery_det,tstep,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv); 
		List<Integerval> r = new ArrayList<Integerval>();
		kinematic_bands(r,conflict_det,recovery_det,tstep,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv);
		neg(l);
		append_intband(l,r);
	}

	// In PVS: inst_bands@instant_track_bands, inst_bands@instant_gs_bands, inst_bands@instant_vs_bands
	private void instantaneous_bands_combine(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		instantaneous_bands(l,conflict_det,recovery_det,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv); 
		List<Integerval> r = new ArrayList<Integerval>();
		instantaneous_bands(r,conflict_det,recovery_det,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv);
		neg(l);
		append_intband(l,r);
	}

	private boolean all_kinematic_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv, int dir) {
		boolean leftans = dir > 0 || first_kinematic_green(conflict_det,recovery_det,tstep,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv) < 0;
		boolean rightans = dir < 0 || first_kinematic_green(conflict_det,recovery_det,tstep,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv) < 0;
		return leftans && rightans; 
	}

	private boolean all_instantaneous_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv, int dir) {
		boolean leftans = dir > 0 || first_instantaneous_green(conflict_det,recovery_det,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv) < 0;
		boolean rightans = dir < 0 || first_instantaneous_green(conflict_det,recovery_det,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv) < 0;
		return leftans && rightans; 
	}

	private boolean any_kinematic_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic,
			int epsh, int epsv, int dir) {
		boolean leftred = dir <= 0 && kinematic_red_band_exist(conflict_det,recovery_det,tstep,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv);
		boolean rightred = dir >= 0 && kinematic_red_band_exist(conflict_det,recovery_det,tstep,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv);
		return leftred || rightred; 
	}

	private boolean any_instantaneous_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv, int dir) {
		boolean leftred = dir <= 0 && instantaneous_red_band_exist(conflict_det,recovery_det,B,T,false,maxl,parameters,ownship,traffic,epsh,epsv);
		boolean rightred = dir >= 0 && instantaneous_red_band_exist(conflict_det,recovery_det,B,T,true,maxr,parameters,ownship,traffic,epsh,epsv);
		return leftred || rightred; 
	}

	// INTERFACE FUNCTIONS
	public void integer_bands_combine(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv) {
		if (tstep == 0) {
			instantaneous_bands_combine(l,conflict_det,recovery_det,
					B,T,maxl,maxr,parameters,ownship,traffic,
					epsh,epsv);
		} else {
			kinematic_bands_combine(l,conflict_det,recovery_det,tstep,
					B,T,maxl,maxr,parameters,ownship,traffic,
					epsh,epsv);
		}
	}

	public boolean all_integer_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv, int dir) {
		return tstep == 0 ?
				all_instantaneous_red(conflict_det,recovery_det, 
						B,T,maxl,maxr, parameters,ownship,traffic, 
						epsh,epsv,dir)
				: all_kinematic_red(conflict_det,recovery_det,tstep, 
						B,T,maxl,maxr, parameters,ownship,traffic, 
						epsh,epsv,dir);
	}

	public boolean any_integer_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T,
			int maxl, int maxr, DaidalusParameters parameters, TrafficState ownship, TrafficState traffic, 
			int epsh, int epsv, int dir) {
		return tstep == 0 ?
				any_instantaneous_red(conflict_det,recovery_det, 
						B,T,maxl,maxr, parameters,ownship,traffic, 
						epsh,epsv,dir)
				: any_kinematic_red(conflict_det,recovery_det,tstep, 
						B,T,maxl,maxr, parameters,ownship,traffic, 
						epsh,epsv,dir);
	}

}
