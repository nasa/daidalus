/*
 * Copyright (c) 2015-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class DaidalusIntegerBands {

	// This class computes NONE bands. trajdir == false is left

	/* Used in definition of kinematic trajectory */
	protected int j_step_;

	public abstract Pair<Vect3,Velocity> trajectory(TrafficState ownship, double time, boolean dir);

	/** In PVS: 
   LET kts = k*ts,
      sot=traj(kts)`1,
      vot=traj(kts)`2 IN
    LOS_at(sot-kts*vot,vot,si,vi,kts)
	 **/ 
	public boolean LOS_at(Detection3D det, boolean trajdir, double tsk, 
			TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		Pair<Vect3,Velocity> sovot = trajectory(ownship,tsk,trajdir);
		Vect3 sot = sovot.first;
		Velocity vot = sovot.second;
		Vect3 sat = vot.ScalAdd(-tsk,sot);
		Vect3 si = traffic.get_s();
		Velocity vi = traffic.get_v();
		double s_err = traffic.relativeHorizontalPositionError(ownship,parameters);
		double sz_err = traffic.relativeVerticalPositionError(ownship,parameters);
		double v_err = traffic.relativeHorizontalSpeedError(ownship,s_err,parameters);
		double vz_err = traffic.relativeVerticalSpeedError(ownship,parameters);
		return det.violationSUMAt(sat,vot,si,vi,
				s_err,sz_err,v_err,vz_err,tsk); 
	}

	// In PVS: int_bands@first_los_step
	private int first_los_step(Detection3D det, double ts,boolean trajdir,
			int min, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		for (int k=min; k<=max; ++k) {
			if (LOS_at(det,trajdir,k*ts,ownship,traffic,parameters)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: kinematic_bands@first_los_search_index
	private int first_los_search_index(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2, boolean trajdir, int max,
			TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) { 
		int FirstLosK = (int)Math.ceil(B/tstep); // first k such that k*ts>=B
		int FirstLosN = Util.min((int)Math.floor(T/tstep),max); // last k<=MaxN such that k*ts<=T
		int FirstLosK2 = (int)Math.ceil(B2/tstep);
		int FirstLosN2 = Util.min((int)Math.floor(T2/tstep),max); 
		int FirstLosInit = recovery_det.isPresent() ? first_los_step(recovery_det.get(),tstep,trajdir,FirstLosK2,FirstLosN2,ownship,traffic,parameters) : -1;
		int FirstLos = first_los_step(conflict_det,tstep,trajdir,FirstLosK,FirstLosN,ownship,traffic,parameters);
		int LosInitIndex = FirstLosInit < 0 ? max+1 : FirstLosInit;
		int LosIndex = FirstLos < 0 ? max+1 : FirstLos;
		return Util.min(LosInitIndex,LosIndex);
	} 

	// In PVS: kinematic_bands@bands_search_index
	// epsh == epsv == 0, if traffic is not the repulsive aircraft
	private int bands_search_index(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2, 
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;
		int FirstLos = first_los_search_index(conflict_det,recovery_det,tstep,B,T,B2,T2,trajdir,max,ownship,traffic,parameters);
		int FirstNonHRep = !usehcrit || FirstLos == 0 ? FirstLos :
			first_nonrepulsive_step(tstep,trajdir,FirstLos-1,ownship,traffic,epsh);
		int FirstProbHcrit = FirstNonHRep < 0 ? max+1 : FirstNonHRep;
		int FirstProbHL = Util.min(FirstLos,FirstProbHcrit);
		int FirstNonVRep = !usevcrit || FirstProbHL == 0 ? FirstProbHL :
			first_nonvert_repul_step(tstep,trajdir,FirstProbHL-1,ownship,traffic,epsv);
		int FirstProbVcrit = FirstNonVRep < 0 ? max+1 : FirstNonVRep;
		return Util.min(FirstProbHL,FirstProbVcrit);  
	}

	/** In PVS: 
	 * NOT CD_future_traj(CD,B,T,traj,kts,si,vi)) AND (NOT (useLOS2 AND CD_future_traj(CD2,B2,T2,traj,kts,si,vi)
	 */ 
	public boolean no_CD_future_traj(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, double B2,
			double T2, boolean trajdir, double tsk, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		return !CD_future_traj(conflict_det,B,T,trajdir,tsk,ownship,traffic,parameters) &&
				!(recovery_det.isPresent() && CD_future_traj(recovery_det.get(),B2,T2,trajdir,tsk,ownship,traffic,parameters));
	}

	// In PVS: int_bands@traj_conflict_only_band, int_bands@nat_bands, and int_bands@nat_bands_rec
	private void traj_conflict_only_bands(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= max; ++k) {
			double tsk = tstep*k;
			if (d >=0 && no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,tsk,ownship,traffic,parameters)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,tsk,ownship,traffic,parameters)) {
				d = k;
			}
		}
		if (d >= 0 && d != max) {
			l.add(new Integerval(d,max));
		}
	}

	// In PVS: kinematic_bands@kinematic_bands
	private void kinematic_bands(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, 
			double B, double T, double B2, double T2, 
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		l.clear();
		int bsi = bands_search_index(conflict_det,recovery_det,tstep,B,T,B2,T2,trajdir,max,ownship,traffic,parameters,epsh,epsv);
		if  (bsi != 0) {
			traj_conflict_only_bands(l,conflict_det,recovery_det,tstep,B,T,B2,T2,trajdir,bsi-1,ownship,traffic,parameters);
		}
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

	// INTERFACE FUNCTION
	// In PVS: combine_bands@kinematic_bands_combine
	public void kinematic_bands_combine(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		kinematic_bands(l,conflict_det,recovery_det,tstep,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv); 
		List<Integerval> r = new ArrayList<Integerval>();
		kinematic_bands(r,conflict_det,recovery_det,tstep,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv);
		neg(l);
		append_intband(l,r);
	}

	// INTERFACE FUNCTION
	// In PVS: kinematic_bands_exist@first_green
	public int first_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;    
		for (int k=0; k <= max; ++k) {
			double tsk = tstep*k;
			if ((tsk >= B && tsk <= T && LOS_at(conflict_det,trajdir,tsk,ownship,traffic,parameters)) ||
					(recovery_det.isPresent() && tsk >= B2 && tsk <= T2 &&
					LOS_at(recovery_det.get(),trajdir,tsk,ownship,traffic,parameters)) ||
					(usehcrit && !repulsive_at(tstep,trajdir,k,ownship,traffic,epsh)) ||
					(usevcrit && !vert_repul_at(tstep,trajdir,k,ownship,traffic,epsv))) {
				return -1;
			} else if (no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,tsk,ownship,traffic,parameters)) {
				return k;
			} 
		}      
		return -1;
	}

	public boolean all_int_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv, int dir) {
		boolean leftans = dir > 0 || first_green(conflict_det,recovery_det,tstep,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv) < 0;
		boolean rightans = dir < 0 || first_green(conflict_det,recovery_det,tstep,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv) < 0;
		return leftans && rightans; 
	}

	private int first_instantaneous_green(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		for (int k = 0; k <= max; ++k) {
			j_step_ = k;
			if (no_instantaneous_conflict(conflict_det,recovery_det,B,T,B2,T2,trajdir,ownship,traffic,parameters,epsh,epsv)) {
				return k;
			}
		}
		return -1;
	}

	public boolean all_instantaneous_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv, int dir) {
		boolean leftans = dir > 0 || first_instantaneous_green(conflict_det,recovery_det,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv) < 0;
		boolean rightans = dir < 0 || first_instantaneous_green(conflict_det,recovery_det,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv) < 0;
		return leftans && rightans; 
	}

	private Vect3 linvel(TrafficState ownship, double tstep, boolean trajdir, int k) {
		Vect3 s1 = trajectory(ownship,(k+1)*tstep,trajdir).first; 
		Vect3 s0 = trajectory(ownship,k*tstep,trajdir).first; 
		return s1.Sub(s0).Scal(1/tstep);
	}

	// In PVS: int_bands@repulsive_at
	private boolean repulsive_at(double tstep, boolean trajdir, int k, TrafficState ownship, TrafficState traffic, int epsh) {
		// k >= 0
		if (k==0) {
			return true;
		}
		Pair<Vect3,Velocity> sovo = trajectory(ownship,0,trajdir);
		Vect2 so = sovo.first.vect2();
		Vect2 vo = sovo.second.vect2();
		Vect2 si = traffic.get_s().vect2();
		Vect2 vi = traffic.get_v().vect2();
		boolean rep = true;
		if (k==1) { 
			rep = CriteriaCore.horizontal_new_repulsive_criterion(so.Sub(si),vo,vi,linvel(ownship,tstep,trajdir,0).vect2(),epsh);
		}
		if (rep) {
			Pair<Vect3,Velocity> sovot = trajectory(ownship,k*tstep,trajdir);
			Vect2 sot = sovot.first.vect2();
			Vect2 vot = sovot.second.vect2();
			Vect2 sit = vi.ScalAdd(k*tstep,si);
			Vect2 st = sot.Sub(sit);
			Vect2 vop = linvel(ownship,tstep,trajdir,k-1).vect2();
			Vect2 vok = linvel(ownship,tstep,trajdir,k).vect2();
			return CriteriaCore.horizontal_new_repulsive_criterion(st,vop,vi,vot,epsh) &&
					CriteriaCore.horizontal_new_repulsive_criterion(st,vot,vi,vok,epsh) &&
					CriteriaCore.horizontal_new_repulsive_criterion(st,vop,vi,vok,epsh);    
		}
		return false;
	}

	// In PVS: int_bands@first_nonrepulsive_step
	private int first_nonrepulsive_step(double tstep, boolean trajdir, int max, TrafficState ownship, TrafficState traffic, int epsh) {
		for (int k=0; k <= max; ++k) {
			if (!repulsive_at(tstep,trajdir,k,ownship,traffic,epsh)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: int_bands@vert_repul_at
	private boolean vert_repul_at(double tstep, boolean trajdir, int k, TrafficState ownship, TrafficState traffic, int epsv) {
		// traffic is valid and k >= 0
		if (k==0) {
			return true;
		}
		Pair<Vect3,Velocity> sovo = trajectory(ownship,0,trajdir);
		Vect3 so = sovo.first;
		Vect3 vo = sovo.second;
		Vect3 si = traffic.get_s();
		Vect3 vi = traffic.get_v();
		boolean rep = true;
		if (k==1) { 
			rep = CriteriaCore.vertical_new_repulsive_criterion(so.Sub(si),vo,vi,linvel(ownship,tstep,trajdir,0),epsv);
		}
		if (rep) {
			Pair<Vect3,Velocity> sovot = trajectory(ownship,k*tstep,trajdir);
			Vect3 sot = sovot.first;
			Vect3 vot = sovot.second;
			Vect3 sit = vi.ScalAdd(k*tstep,si);
			Vect3 st = sot.Sub(sit);
			Vect3 vop = linvel(ownship,tstep,trajdir,k-1);
			Vect3 vok = linvel(ownship,tstep,trajdir,k);
			return CriteriaCore.vertical_new_repulsive_criterion(st,vop,vi,vot,epsv) &&
					CriteriaCore.vertical_new_repulsive_criterion(st,vot,vi,vok,epsv) &&
					CriteriaCore.vertical_new_repulsive_criterion(st,vop,vi,vok,epsv);    
		}
		return false;
	}

	// In PVS: int_bands@first_nonvert_repul_step
	private int first_nonvert_repul_step(double tstep, boolean trajdir, int max, TrafficState ownship, TrafficState traffic, int epsv) {
		for (int k=0; k <= max; ++k) {
			if (!vert_repul_at(tstep,trajdir,k,ownship,traffic,epsv)) {
				return k;
			}
		}
		return -1;
	}

	// In PVS: CD(B,T,so,vo,si,vi)
	private static boolean conflict(Detection3D det, Vect3 so, Velocity vo, Vect3 si, Velocity vi, 
			double B, double T, double s_err, double sz_err, double v_err, double vz_err) {
		if (Util.almost_equals(B,T)) {
			return det.violationSUMAt(so,vo,si,vi,s_err,sz_err,v_err,vz_err,B); 
		}
		return det.conflictSUM(so,vo,si,vi,B,T,s_err,sz_err,v_err,vz_err);
	}

	// In PVS: int_bands@CD_future_traj
	public boolean CD_future_traj(Detection3D det, double B, double T, boolean trajdir, double tsk, 
			TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		if (tsk > T || B > T) return false;
		Pair<Vect3,Velocity> sovot = trajectory(ownship,tsk,trajdir);
		Vect3 sot = sovot.first;
		Velocity vot = sovot.second;
		Vect3 sat = tsk == 0.0 ? sot : vot.ScalAdd(-tsk,sot);
		Vect3 si = traffic.get_s();
		Velocity vi = traffic.get_v();
		double s_err = traffic.relativeHorizontalPositionError(ownship,parameters);
		double sz_err = traffic.relativeVerticalPositionError(ownship,parameters);
		double v_err = traffic.relativeHorizontalSpeedError(ownship,s_err,parameters);
		double vz_err = traffic.relativeVerticalSpeedError(ownship,parameters);
		return conflict(det,sat,vot,si,vi,Util.max(B,tsk),T,
				s_err,sz_err,v_err,vz_err);
	}

	// In PVS: first_conflict_step(CD,B,T,traj,0,ts,si,vi,MaxN) >= 0
	private boolean any_conflict_step(Detection3D det, double tstep, double B, double T, boolean trajdir, int max, 
			TrafficState ownship, TrafficState traffic, DaidalusParameters parameters) {
		for (int k=0; k <= max; ++k) {
			if (CD_future_traj(det,B,T,trajdir,tstep*k,ownship,traffic,parameters)) {
				return true;
			}
		}
		return false;
	}

	// In PVS: kinematic_bands_exist@red_band_exist
	public boolean red_band_exist(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep, 
			double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters,
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;
		return (usehcrit && first_nonrepulsive_step(tstep,trajdir,max,ownship,traffic,epsh) >= 0) ||
				(usevcrit && first_nonvert_repul_step(tstep,trajdir,max,ownship,traffic,epsv) >= 0) ||
				any_conflict_step(conflict_det,tstep,B,T,trajdir,max,ownship,traffic,parameters) ||
				(recovery_det.isPresent() && any_conflict_step(recovery_det.get(),tstep,B2,T2,trajdir,max,ownship,traffic,parameters));
	}

	private boolean instantaneous_red_band_exist(Detection3D conflict_det, Optional<Detection3D> recovery_det,  
			double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		for (int k = 0; k <= max; ++k) {
			j_step_ = k;
			if (!no_instantaneous_conflict(conflict_det,recovery_det,B,T,B2,T2,trajdir,ownship,traffic,parameters,epsh,epsv)) {
				return true;
			}
		}
		return false;
	}

	// INTERFACE FUNCTION
	public boolean any_int_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, double tstep,
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters,
			int epsh, int epsv, int dir) {
		boolean leftred = dir <= 0 && red_band_exist(conflict_det,recovery_det,tstep,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv);
		boolean rightred = dir >= 0 && red_band_exist(conflict_det,recovery_det,tstep,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv);
		return leftred || rightred; 
	}

	public boolean any_instantaneous_red(Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv, int dir) {
		boolean leftred = dir <= 0 && instantaneous_red_band_exist(conflict_det,recovery_det,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv);
		boolean rightred = dir >= 0 && instantaneous_red_band_exist(conflict_det,recovery_det,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv);
		return leftred || rightred; 
	}

	// INTERFACE FUNCTION
	// In PVS: inst_bands@instant_track_bands, inst_bands@instant_gs_bands, inst_bands@instant_vs_bands
	public void instantaneous_bands_combine(List<Integerval> l, Detection3D conflict_det, Optional<Detection3D> recovery_det, 
			double B, double T, double B2, double T2,
			int maxl, int maxr, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters, 
			int epsh, int epsv) {
		instantaneous_bands(l,conflict_det,recovery_det,B,T,B2,T2,false,maxl,ownship,traffic,parameters,epsh,epsv); 
		List<Integerval> r = new ArrayList<Integerval>();
		instantaneous_bands(r,conflict_det,recovery_det,B,T,B2,T2,true,maxr,ownship,traffic,parameters,epsh,epsv);
		neg(l);
		append_intband(l,r);
	}

	//In PVS: inst_bands@conflict_free_track_step, inst_bands@conflict_free_gs_step, inst_bands@conflict_free_vs_step
	public boolean no_instantaneous_conflict(Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, double B2, double T2,
			boolean trajdir, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters,
			int epsh, int epsv) {
		boolean usehcrit = epsh != 0;
		boolean usevcrit = epsv != 0;    
		Pair<Vect3,Velocity> nsovo = trajectory(ownship,0,trajdir);
		Vect3 so = ownship.get_s();
		Velocity vo = ownship.get_v();
		Vect3 si = traffic.get_s();
		Velocity vi = traffic.get_v();
		Velocity nvo = nsovo.second;
		Vect3 s = so.Sub(si);
		return 
				(!usehcrit || CriteriaCore.horizontal_new_repulsive_criterion(s.vect2(),vo.vect2(),vi.vect2(),nvo.vect2(),epsh)) &&
				(!usevcrit || CriteriaCore.vertical_new_repulsive_criterion(s,vo,vi,nvo,epsv)) && 
				no_CD_future_traj(conflict_det,recovery_det,B,T,B2,T2,trajdir,0.0,ownship,traffic,parameters);
	}

	//In PVS: int_bands@nat_bands, int_bands@nat_bands_rec 
	private void instantaneous_bands(List<Integerval> l,
			Detection3D conflict_det, Optional<Detection3D> recovery_det, double B, double T, double B2, double T2,
			boolean trajdir, int max, TrafficState ownship, TrafficState traffic, DaidalusParameters parameters,
			int epsh, int epsv) {
		int d = -1; // Set to the first index with no conflict
		for (int k = 0; k <= max; ++k) {
			j_step_ = k;
			if (d >=0 && no_instantaneous_conflict(conflict_det,recovery_det,B,T,B2,T2,trajdir,ownship,traffic,parameters,epsh,epsv)) {
				continue;
			} else if (d >=0) {
				l.add(new Integerval(d,k-1));
				d = -1;
			} else if (no_instantaneous_conflict(conflict_det,recovery_det,B,T,B2,T2,trajdir,ownship,traffic,parameters,epsh,epsv)) {
				d = k;
			}
		}
		if (d >= 0 && d != max) {
			l.add(new Integerval(d,max));
		}
	}

}
