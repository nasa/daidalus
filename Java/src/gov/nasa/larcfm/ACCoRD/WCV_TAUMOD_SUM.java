/*
> * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

/* Horizontal Well Clear Volume concept based on Modified TAU
 * DTHR and TAUMOD are distance and time thresholds, respectively 
 */

public class WCV_TAUMOD_SUM extends WCV_TAUMOD {

	private double  h_pos_z_score_;             // Number of horizontal position standard deviations
	private boolean h_pos_z_score_enabled_;     // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter
	private double  h_vel_z_score_min_;         // Minimum number of horizontal velocity standard deviations
	private boolean h_vel_z_score_min_enabled_; // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter
	private double  h_vel_z_score_max_;         // Maximum number of horizontal velocity standard deviations
	private boolean h_vel_z_score_max_enabled_; // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter
	private double  h_vel_z_distance_;          // Distance at which h_vel_z_score scales from min to max as range decreases
	private String  h_vel_z_distance_units_;    // Units of distance at which h_vel_z_score scales from min to max as range decreases
	private boolean h_vel_z_distance_enabled_;  // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter
	private double  v_pos_z_score_;             // Number of vertical position standard deviations
	private boolean v_pos_z_score_enabled_;     // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter
	private double  v_vel_z_score_;             // Number of vertical velocity standard deviations
	private boolean v_vel_z_score_enabled_;     // True if value has been set independently. Otherwise,
	// value will be overwritten using global parameter

	private static double MinError = 0.001;

	private void initSUM() {
		h_pos_z_score_ = 0.0;
		h_pos_z_score_enabled_ = false;
		h_vel_z_score_min_ = 0.0;
		h_vel_z_score_min_enabled_ = false;
		h_vel_z_score_max_ = 0.0;
		h_vel_z_score_max_enabled_ = false;
		h_vel_z_distance_ = 0.0;
		h_vel_z_distance_enabled_ = false;
		h_vel_z_distance_units_ = "nmi";
		v_pos_z_score_ = 0.0;
		v_pos_z_score_enabled_ = false;
		v_vel_z_score_ = 0.0;
		v_vel_z_score_enabled_ = false;		
	}

	/** Constructor that a default instance of the WCV tables. */
	public WCV_TAUMOD_SUM() {
		table = new WCVTable();
		wcv_vertical = new WCV_TCOA();
		initSUM();
	}

	/** Constructor that specifies a particular instance of the WCV tables. */
	public WCV_TAUMOD_SUM(WCVTable tab) {
		super(tab);
		initSUM();
	}

	/**
	 * DO-365 Phase I (en-route) preventive thresholds, i.e., DTHR=0.66nmi, ZTHR=700ft,
	 * TTHR=35s, TCOA=0, with SUM
	 */
	public static final WCV_TAUMOD_SUM DO_365_Phase_I_preventive =
			new WCV_TAUMOD_SUM(WCVTable.DO_365_Phase_I_preventive);

	/**
	 * DO-365 Well-Clear thresholds Phase I (en-route), i.e., DTHR=0.66nmi, ZTHR=450ft,
	 * TTHR=35s, TCOA=0, with SUM
	 */
	public static final WCV_TAUMOD_SUM DO_365_DWC_Phase_I = 
			new WCV_TAUMOD_SUM(WCVTable.DO_365_DWC_Phase_I);

	/**
	 * DO-365 Well-Clear thresholds Phase II (DTA), i.e., DTHR=1500 [ft], ZTHR=450ft,
	 * TTHR=0s, TCOA=0, with SUM
	 */
	public static final WCV_TAUMOD_SUM DO_365_DWC_Phase_II = 
			new WCV_TAUMOD_SUM(WCVTable.DO_365_DWC_Phase_II);

	/**
	 * DO-365 Well-Clear thresholds Non-Cooperative, i.e., DTHR=2200 [ft], ZTHR=450ft,
	 * TTHR=0s, TCOA=0.
	 */
	public static final WCV_TAUMOD_SUM DO_365_DWC_Non_Coop = 
			new WCV_TAUMOD_SUM(WCVTable.DO_365_DWC_Non_Coop);

	private void copyFrom(WCV_TAUMOD_SUM wcv) {
		id = wcv.id;
		table = wcv.table.copy();
		wcv_vertical = wcv.wcv_vertical.copy();
		h_pos_z_score_ = wcv.h_pos_z_score_;
		h_pos_z_score_enabled_ = wcv.h_pos_z_score_enabled_;
		h_vel_z_score_min_ = wcv.h_vel_z_score_min_;
		h_vel_z_score_min_enabled_ = wcv.h_vel_z_score_min_enabled_;
		h_vel_z_score_max_ = wcv.h_vel_z_score_max_;
		h_vel_z_score_max_enabled_ = wcv.h_vel_z_score_max_enabled_;
		h_vel_z_distance_ = wcv.h_vel_z_distance_;
		h_vel_z_distance_enabled_ = wcv.h_vel_z_distance_enabled_;
		h_vel_z_distance_units_ = wcv.h_vel_z_distance_units_;
		v_pos_z_score_ = wcv.v_pos_z_score_;
		v_pos_z_score_enabled_ = wcv.v_pos_z_score_enabled_;
		v_vel_z_score_ = wcv.v_vel_z_score_;
		v_vel_z_score_enabled_ = wcv.v_vel_z_score_enabled_;
	}

	/** Constructor that specifies a particular instance of the WCV_TAUMOD_SUM. */
	public WCV_TAUMOD_SUM(WCV_TAUMOD_SUM wcv) {
		copyFrom(wcv);
	}

	/**
	 * Returns a deep copy of this WCV_TAUMOD object, including any results that have been calculated.  
	 */
	public WCV_TAUMOD_SUM copy() {
		WCV_TAUMOD_SUM ret = new WCV_TAUMOD_SUM(this);
		return ret;
	}
	
	/**
	 * One static WCV_TAUMOD_SUM
	 */
	public static final WCV_TAUMOD_SUM A_WCV_TAUMOD_SUM =
			new WCV_TAUMOD_SUM();

	private boolean sumof(Vect2 v1, Vect2 v2, Vect2 w) {
		double detv2v1 = v2.det(v1);
		return w.det(v1)*detv2v1>=0 && w.det(v2)*detv2v1 <=0;
	}

	private Vect2 average_direction(Vect2 v1, Vect2 v2) {
		return v1.Add(v2).Hat();
	}

	private Pair<Vect2,Vect2> optimal_pair(Vect2 v1, Vect2 v2, Vect2 w1, Vect2 w2) {
		if (sumof(v1,v2,w1) && sumof(v1,v2,w2)) {
			Vect2 avg_dir = average_direction(w1,w2);
			return Pair.make(avg_dir,avg_dir);
		} else if (sumof(w1,w2,v1) && sumof(w1,w2,v2)) {
			Vect2 avg_dir = average_direction(v1,v2);
			return Pair.make(avg_dir,avg_dir);
		} else if (sumof(w1,w2,v1) && sumof(v1,v2,w1)) {
			Vect2 avg_dir = average_direction(v1,w1);
			return Pair.make(avg_dir,avg_dir);
		} else if (sumof(w1,w2,v1) && sumof(v1,v2,w2)) {
			Vect2 avg_dir = average_direction(v1,w2);
			return Pair.make(avg_dir,avg_dir);
		} else if (sumof(w1,w2,v2) && sumof(v1,v2,w1)) {
			Vect2 avg_dir = average_direction(v2,w1);
			return Pair.make(avg_dir,avg_dir);			
		} else if (sumof(w1,w2,v2) && sumof(v1,v2,w2)) {
			Vect2 avg_dir = average_direction(v2,w2);
			return Pair.make(avg_dir,avg_dir);			
		} else { 
			double d11 = v1.dot(w1);
			double d12 = v1.dot(w2); 
			double d21 = v2.dot(w1);
			double d22 = v2.dot(w2);
			if (d11>=d12 && d11>=d21 && d11>=d22) {
				return Pair.make(v1,w1);
			} else if (d12>=d11 && d12>=d21 && d12>=d22) {
				return Pair.make(v1,w2);
			} else if (d21>=d11 && d21>=d12 && d21>=d22) {
				return Pair.make(v2,w1);
			} else {
				return Pair.make(v2, w2);
			}
		}
	}

	private Pair<Vect2,Vect2> optimal_wcv_pair_comp_init(Vect2 s, Vect2 v, double s_err, double v_err) {
		Vect2 v1 = new TangentLine(s,s_err,-1).Hat();
		Vect2 v2 = new TangentLine(s,s_err,1).Hat();
		Vect2 w = v.Neg();
		Vect2 w1 = new TangentLine(w,v_err,-1).Hat();
		Vect2 w2 = new TangentLine(w,v_err,1).Hat();
		Pair<Vect2,Vect2> op = optimal_pair(v1,v2,w1,w2);
		return Pair.make(op.first.Neg(),op.second);
	}

	private Pair<Vect2,Vect2> optimal_wcv_pair(Vect2 s, Vect2 v, double s_err, double v_err, int eps1, int eps2) {
		Pair<Vect2,Vect2> owpci = optimal_wcv_pair_comp_init(s,v,s_err,v_err); 
		return Pair.make(owpci.first.Scal(s.norm()+eps1*s_err),owpci.second.Scal(v.norm()-eps2*v_err));
	}

	private  boolean horizontal_wcv_taumod_uncertain(Vect2 s, Vect2 v, double s_err, double v_err) {
		if (horizontal_WCV(s,v)) {
			return true;
		}
		if (s.sqv()<=Util.sq(table.DTHR+s_err)) {
			return true;
		}
		if (v.sqv()<=Util.sq(v_err)) {
			Vect2 s_hat = s.Hat();
			return horizontal_WCV(s_hat.Scal(s.norm()-s_err),s_hat.Scal(-(v.norm()+v_err)));
		}
		Pair<Vect2,Vect2> owp = optimal_wcv_pair(s,v,s_err,v_err,-1,-1);
		return horizontal_WCV(owp.first,owp.second);
	}

	private boolean vertical_WCV_uncertain(double sz, double vz, double sz_err, double vz_err) {
		int ssign = Util.sign(sz);
		double snew = sz-ssign*Util.min(sz_err,Math.abs(sz));
		double vnew = vz-ssign*vz_err;
		return wcv_vertical.vertical_WCV(table.ZTHR,table.TCOA,snew,vnew);
	}

	public boolean WCV_taumod_uncertain(Vect3 s, Vect3 v, double s_err, double sz_err, double v_err, double vz_err) {
		return horizontal_wcv_taumod_uncertain(s.vect2(),v.vect2(),s_err,v_err) && 
				vertical_WCV_uncertain(s.z,v.z,sz_err,vz_err);
	}

	private double horizontal_wcv_taumod_uncertain_entry(Vect2 s, Vect2 v, double s_err, double v_err, double T) {
		if (horizontal_WCV(s,v) || s.sqv()<=Util.sq(table.DTHR+s_err)) { 
			return 0;
		} 
		if (v.sqv() <= Util.sq(v_err)) { 
			Vect2 s_hat = s.Hat();
			LossData ee = horizontal_WCV_interval(T,s_hat.Scal(s.norm()-s_err),s_hat.Scal(-(v.norm()+v_err)));
			if (ee.getTimeOut() < ee.getTimeIn()) { 
				return T+1;
			} else { 
				return ee.getTimeIn(); 
			} 
		} else {
			Pair<Vect2,Vect2> op = optimal_wcv_pair(s,v,s_err,v_err,-1,-1);
			if (op.first.dot(op.second) < 0) { 
				return  Util.min(horizontal_WCV_interval(T,s,v).getTimeIn(),
						horizontal_WCV_interval(T,op.first,op.second).getTimeIn());
			} else {
				return T+1;
			}
		}
	}

	private double Theta_D_uncertain(Vect2 s, Vect2 v, double s_err, double v_err, int eps) {
		if (v.sqv() <= Util.sq(v_err)) { 
			return -1;
		}
		else {
			double rt = Util.root(v.sqv()-Util.sq(v_err),2*(s.dot(v)-v_err*(table.DTHR+s_err)),s.sqv()-Util.sq(table.DTHR+s_err),eps);
			if (Double.isFinite(rt)) {
				return rt;
			} 
			return -1;
		}			
	}

	private double horizontal_wcv_taumod_uncertain_exit(Vect2 s, Vect2 v,double s_err, double v_err, double T) {
		if (v.sqv() <= Util.sq(v_err) && s.sqv() <= Util.sq(table.DTHR+s_err)) { 
			return T;
		} else if (v.sqv() <= Util.sq(v_err)) { 
			Vect2 s_hat = s.Hat();
			LossData ee = horizontal_WCV_interval(T,s_hat.Scal(s.norm()-s_err),s_hat.Scal(-(v.norm()+v_err)));
			if (ee.getTimeOut() < ee.getTimeIn()) {
				return -1;
			} else {
				return T; 
			}
		} else {
			return Theta_D_uncertain(s,v,s_err,v_err,1);
		}
	}

	private LossData horizontal_wcv_taumod_uncertain_interval(Vect2 s, Vect2 v,double s_err, double v_err, double T) {
		double entrytime = horizontal_wcv_taumod_uncertain_entry(s,v,s_err,v_err,T);
		double exittime = horizontal_wcv_taumod_uncertain_exit(s,v,s_err,v_err,T);
		if (entrytime > T || exittime < 0 || entrytime > exittime) { 
			return LossData.EMPTY;
		} 
		return new LossData(Util.max(0,entrytime),Util.min(T,exittime));
	}

	private LossData vertical_WCV_uncertain_full_interval_szpos_vzpos(double T, double minsz/*,double maxsz*/, double minvz/*, double maxvz*/) {
		Interval ii = wcv_vertical.vertical_WCV_interval(table.ZTHR,table.TCOA,0,T,minsz,minvz);
		return new LossData(ii.low,ii.up);
	}

	private LossData vertical_WCV_uncertain_full_interval_szpos_vzneg(double T, double minsz,double maxsz, double minvz, double maxvz) {
		Interval entryint = wcv_vertical.vertical_WCV_interval(table.ZTHR,table.TCOA,0,T,minsz,minvz);
		Interval exitint = wcv_vertical.vertical_WCV_interval(table.ZTHR,table.TCOA,0,T,maxsz,maxvz);
		if (entryint.low > entryint.up) {
			return LossData.EMPTY;
		} else if (exitint.low > exitint.up) { 
			return new LossData(entryint.low,T);
		} else {
			return new LossData(entryint.low,exitint.up);
		}
	}

	private LossData vertical_WCV_uncertain_full_interval_szpos(double T, double minsz,double maxsz, double minvz, double maxvz) {
		boolean vel_only_pos = minvz >= 0;
		boolean vel_only_neg = !vel_only_pos && maxvz <= 0;
		LossData intp = vel_only_neg ? LossData.EMPTY : vertical_WCV_uncertain_full_interval_szpos_vzpos(T,minsz/*,maxsz*/,Util.max(minvz,0)/*,maxvz*/);
		LossData intn = vel_only_pos ? LossData.EMPTY : vertical_WCV_uncertain_full_interval_szpos_vzneg(T,minsz,maxsz,minvz,Util.min(maxvz,0));
		if (vel_only_pos || intn.getTimeIn() > intn.getTimeOut()) {
			return intp;
		} else if (vel_only_neg || intp.getTimeIn() > intp.getTimeOut()) {
			return intn;
		} else {
			return new LossData(Util.min(intp.getTimeIn(),intn.getTimeIn()),Util.max(intp.getTimeOut(),intn.getTimeOut()));
		}
	}

	private LossData vertical_WCV_uncertain_full_interval_split(double T, double minsz,double maxsz, double minvz, double maxvz) {
		boolean pos_only_pos = minsz >= 0;
		boolean pos_only_neg = !pos_only_pos && maxsz <= 0;
		LossData intp = pos_only_neg ? LossData.EMPTY : vertical_WCV_uncertain_full_interval_szpos(T,Util.max(minsz,0),maxsz,minvz,maxvz);
		LossData intn = pos_only_pos ? LossData.EMPTY : vertical_WCV_uncertain_full_interval_szpos(T,-Util.min(maxsz,0),-minsz,-maxvz,-minvz);
		if (pos_only_pos || intn.getTimeIn() > intn.getTimeOut()) {
			return intp;
		} else if (pos_only_neg || intp.getTimeIn() > intp.getTimeOut()) {
			return intn;
		} else {
			return new LossData(Util.min(intp.getTimeIn(),intn.getTimeIn()),Util.max(intp.getTimeOut(),intn.getTimeOut()));
		}
	}

	private LossData vertical_WCV_uncertain_interval(double B, double T, double sz, double vz, double sz_err, double vz_err) {
		LossData posint = vertical_WCV_uncertain_full_interval_split(T,sz-sz_err,sz+sz_err,vz-vz_err,vz+vz_err);
		if (posint.getTimeIn() > posint.getTimeOut() || posint.getTimeOut() < B) {
			return LossData.EMPTY;
		} else {
			return new LossData(Util.max(B,posint.getTimeIn()),Util.min(T,posint.getTimeOut()));
		}
	}

	public LossData WCV_taumod_uncertain_interval(double B, double T, Vect3 s, Vect3 v, 
			double s_err, double sz_err, double v_err, double vz_err) {
		LossData vint = vertical_WCV_uncertain_interval(B,T,s.z,v.z,sz_err,vz_err);
		if (vint.getTimeIn() > vint.getTimeOut()) { 
			return vint; // Empty interval
		}
		LossData hint = horizontal_wcv_taumod_uncertain_interval(s.vect2(),v.vect2(),s_err,v_err,T);
		if (hint.getTimeIn() > hint.getTimeOut()) {
			return hint; // Empty interval
		}		
		if (hint.getTimeOut() < B) { 
			return LossData.EMPTY;
		}
		return new LossData(Util.max(vint.getTimeIn(),hint.getTimeIn()),Util.min(vint.getTimeOut(),hint.getTimeOut()));
	}

	public boolean WCV_taumod_uncertain_detection(double B, double T, Vect3 s, Vect3 v, 
			double s_err, double sz_err, double v_err, double vz_err) {
		if (B > T) { 
			return false;
		}

		LossData interval = B == T ?  WCV_taumod_uncertain_interval(B,B+1,s,v,s_err,sz_err,v_err,vz_err):
			WCV_taumod_uncertain_interval(B,T,s,v,s_err,sz_err,v_err,vz_err);
		if (B == T) {
			return interval.conflict() && interval.getTimeIn()<=B;
		} 
		return interval.conflict();
	}

	/**
	 * This functional call returns a ConflictData object detailing the conflict between times B and T from now (relative), if any.  
	 * @param ownship   ownship state
	 * @param intruder  intruder state
	 * @param B   beginning of detection time ({@code >=0})
	 * @param T   end of detection time (if {@code T < 0} then use an "infinite" lookahead time)
	 * @return a ConflictData object detailing the conflict
	 */
	public ConflictData conflictDetectionWithTrafficState(TrafficState ownship, TrafficState intruder, 
			double B, double T) {
		double s_err = relativeHorizontalPositionError(ownship,intruder);
		double sz_err = relativeVerticalPositionError(ownship,intruder);
		double v_err = relativeHorizontalSpeedError(ownship,intruder,s_err);
		double vz_err = relativeVerticalSpeedError(ownship,intruder);

		Vect3 so = ownship.get_s();
		Velocity vo = ownship.get_v();
		Vect3 si = intruder.get_s();
		Velocity vi = intruder.get_v();


		if (s_err == 0.0 && sz_err == 0.0 && v_err == 0.0 && vz_err == 0.0) {
			return conflictDetection(so,vo,si,vi,B,T);
		}		

		s_err = Util.max(s_err, MinError);
		sz_err = Util.max(sz_err, MinError);
		v_err = Util.max(v_err, MinError);
		vz_err = Util.max(vz_err, MinError);

		Vect3 s = so.Sub(si);
		Velocity v = vo.Sub(vi);
		LossData ld = WCV_taumod_uncertain_interval(B,T,s,v,s_err,sz_err,v_err,vz_err);
		double t_tca = (ld.getTimeIn() + ld.getTimeOut())/2.0;
		double dist_tca = s.linear(v, t_tca).cyl_norm(table.DTHR,table.ZTHR);
		return new ConflictData(ld,t_tca,dist_tca,s,v);
	}

	public WCV_TAUMOD_SUM make() {
		return new WCV_TAUMOD_SUM();
	}

	private boolean containsSUM(WCV_TAUMOD_SUM wcv) {
		return h_pos_z_score_ == wcv.h_pos_z_score_ &&
				h_vel_z_score_min_ == wcv.h_vel_z_score_min_ &&
				h_vel_z_score_max_ == wcv.h_vel_z_score_max_ &&
				h_vel_z_distance_ == wcv.h_vel_z_distance_ &&
				v_pos_z_score_ == wcv.v_pos_z_score_ &&
				v_vel_z_score_ == wcv.v_vel_z_score_;
	}

	public boolean contains(Detection3D cd) {
		if (cd instanceof WCV_TAUMOD_SUM &&
				!containsSUM((WCV_TAUMOD_SUM)cd)) {
			return false;
		}
		if (cd instanceof WCV_TAUMOD_SUM || cd instanceof WCV_TAUMOD || cd instanceof WCV_TCPA) {
			return containsTable((WCV_tvar)cd);
		}
		return false;
	}

	private double relativeHorizontalPositionError(TrafficState own, TrafficState ac) {
		return h_pos_z_score_*
				(own.sum().getHorizontalPositionError()+ac.sum().getHorizontalPositionError());
	}

	private double relativeVerticalPositionError(TrafficState own, TrafficState ac) {
		return v_pos_z_score_*
				(own.sum().getVerticalPositionError()+ac.sum().getVerticalPositionError());
	}

	private double weighted_z_score(double range) {
		if (range>=h_vel_z_distance_) {
			return h_vel_z_score_min_;
		} else { 
			double perc = range/h_vel_z_distance_;
			return (1-perc)*h_vel_z_score_max_+perc*h_vel_z_score_min_;
		}
	}

	private double relativeHorizontalSpeedError(TrafficState own,TrafficState ac, double s_err) {
		double range = own.get_s().distanceH(ac.get_s());
		double  z_score = weighted_z_score(Util.max(range-s_err,0.0));
		return z_score*
				(own.sum().getHorizontalSpeedError()+ac.sum().getHorizontalSpeedError());
	}

	private double relativeVerticalSpeedError(TrafficState own, TrafficState ac) {
		return v_vel_z_score_*
				(own.sum().getVerticalSpeedError()+ac.sum().getVerticalSpeedError());
	}

	public void updateParameterData(ParameterData p) {
		super.updateParameterData(p);
		if (h_pos_z_score_enabled_) {
			p.setInternal("h_pos_z_score", h_pos_z_score_, "unitless");
		}
		if (h_vel_z_score_min_enabled_) {
			p.setInternal("h_vel_z_score_min", h_vel_z_score_min_, "unitless");
		}
		if (h_vel_z_score_max_enabled_) {
			p.setInternal("h_vel_z_score_max", h_vel_z_score_max_, "unitless");
		}
		if (h_vel_z_distance_enabled_) {
			p.setInternal("h_vel_z_distance", h_vel_z_distance_, h_vel_z_distance_units_);
		}
		if (v_pos_z_score_enabled_) {
			p.setInternal("v_pos_z_score", v_pos_z_score_, "unitless");
		}
		if (v_vel_z_score_enabled_) {
			p.setInternal("v_vel_z_score", v_vel_z_score_, "unitless");
		}
	}

	public void setParameters(ParameterData p) {
		super.setParameters(p);
		if (p.contains("h_pos_z_score")) {
			setHorizontalPositionZScore(p.getValue("h_pos_z_score"));
		}
		if (p.contains("h_vel_z_score_min")) {
			setHorizontalVelocityZScoreMin(p.getValue("h_vel_z_score_min"));
		}
		if (p.contains("h_vel_z_score_max")) {
			setHorizontalVelocityZScoreMax(p.getValue("h_vel_z_score_max"));
		}
		if (p.contains("h_vel_z_distance")) {
			setHorizontalVelocityZDistance(p.getValue("h_vel_z_distance"));
			h_vel_z_distance_units_ = p.getUnit("h_vel_z_distance");
		}
		if (p.contains("v_pos_z_score")) {
			setVerticalPositionZScore(p.getValue("v_pos_z_score"));
		}
		if (p.contains("v_vel_z_score")) {
			setVerticalSpeedZScore(p.getValue("v_vel_z_score"));
		}
	}

	protected void set_global_SUM_parameters(DaidalusParameters p) {
		if (!h_pos_z_score_enabled_) {
			h_pos_z_score_ = p.getHorizontalPositionZScore();
		}
		if (!h_vel_z_score_min_enabled_) {
			h_vel_z_score_min_ = p.getHorizontalVelocityZScoreMin();
		}
		if (!h_vel_z_score_max_enabled_) {
			h_vel_z_score_max_ = p.getHorizontalVelocityZScoreMax();
		}
		if (!h_vel_z_distance_enabled_) {
			h_vel_z_distance_ = p.getHorizontalVelocityZDistance();
			h_vel_z_distance_units_ = p.getUnitsOf("h_vel_z_distance");
		}
		if (!v_pos_z_score_enabled_) {
			v_pos_z_score_ = p.getVerticalPositionZScore();
		}
		if (!v_vel_z_score_enabled_) {
			v_vel_z_score_ = p.getVerticalSpeedZScore();
		}
	}

	/** 
	 * @return get z-score (number of standard deviations) for horizontal position 
	 */
	public double getHorizontalPositionZScore() {
		return h_pos_z_score_;
	}

	/** 
	 * Set z-score (number of standard deviations) for horizontal position (non-negative value)
	 */
	public void setHorizontalPositionZScore(double val) {
		h_pos_z_score_enabled_ = true;
		h_pos_z_score_ = Math.abs(val);
	}

	/** 
	 * @return get min z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMin() {
		return h_vel_z_score_min_;
	}

	/** 
	 * Set min z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMin(double val) {
		h_vel_z_score_min_enabled_ = true;
		h_vel_z_score_min_ = Math.abs(val);
	}

	/** 
	 * @return get max z-score (number of standard deviations) for horizontal velocity 
	 */
	public double getHorizontalVelocityZScoreMax() {
		return h_vel_z_score_max_;
	}

	/** 
	 * Set max z-score (number of standard deviations) for horizontal velocity (non-negative value)
	 */
	public void setHorizontalVelocityZScoreMax(double val) {
		h_vel_z_score_max_enabled_ = true;
		h_vel_z_score_max_ = Math.abs(val);
	}

	/** 
	 * @return Distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public double getHorizontalVelocityZDistance() {
		return h_vel_z_distance_;
	}

	/** 
	 * Set distance (in internal units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val) {
		h_vel_z_distance_enabled_ = true;
		h_vel_z_distance_ = Math.abs(val);
	}

	/** 
	 * @return Distance (in given units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public double getHorizontalVelocityZDistance(String u) {
		return Units.to(u,h_vel_z_distance_);
	}

	/** 
	 * Set distance (in given units) at which h_vel_z_score scales from min to max as range decreases
	 */
	public void setHorizontalVelocityZDistance(double val, String u) {
		setHorizontalVelocityZDistance(Units.from(u,val));
		h_vel_z_distance_units_ = u;
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical position 
	 */
	public double getVerticalPositionZScore() {
		return v_pos_z_score_;
	}

	/** 
	 * Set z-score (number of standard deviations) for vertical position (non-negative value)
	 */
	public void setVerticalPositionZScore(double val) {
		v_pos_z_score_enabled_ = true;
		v_pos_z_score_ = Math.abs(val);
	}

	/** 
	 * @return get z-score (number of standard deviations) for vertical velocity 
	 */
	public double getVerticalSpeedZScore() {
		return v_vel_z_score_;
	}

	/** 
	 * Set z-score (number of standard deviations) for vertical velocity (non-negative value)
	 */
	public void setVerticalSpeedZScore(double val) {
		v_vel_z_score_enabled_ = true;
		v_vel_z_score_ = Math.abs(val);
	}

	public String toString() {
		String str = super.toString();
		str += ", {h_pos_z_score = "+f.FmPrecision(h_pos_z_score_);
		str += ", h_vel_z_score_min = "+f.FmPrecision(h_vel_z_score_min_);
		str += ", h_vel_z_score_max = "+f.FmPrecision(h_vel_z_score_max_);
		str += ", h_vel_z_distance = "+Units.str(h_vel_z_distance_units_,h_vel_z_distance_);
		str += ", v_pos_z_score = "+ f.FmPrecision(v_pos_z_score_);
		str += ", v_vel_z_score = "+ f.FmPrecision(v_vel_z_score_)+"}";
		return str;
	}

	public String toPVS() {
		String str = getSimpleClassName()+"((# "+table.toPVS_();
		str += ", h_pos_z_score := "+f.FmPrecision(h_pos_z_score_);
		str += ", h_vel_z_score_min := "+f.FmPrecision(h_vel_z_score_min_);
		str += ", h_vel_z_score_max := "+f.FmPrecision(h_vel_z_score_max_);
		str += ", h_vel_z_distance := "+f.FmPrecision(h_vel_z_distance_);
		str += ", v_pos_z_score := "+ f.FmPrecision(v_pos_z_score_);
		str += ", v_vel_z_score := "+ f.FmPrecision(v_vel_z_score_);
		str += " #))";
		return str;
	}

}
