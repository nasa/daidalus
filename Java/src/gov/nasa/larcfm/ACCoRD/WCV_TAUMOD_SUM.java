/*
> * Copyright (c) 2012-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Interval;
import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.Pair;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;

/* Horizontal Well Clear Volume concept based on Modified TAU
 * DTHR and TAUMOD are distance and time thresholds, respectively 
 */

public class WCV_TAUMOD_SUM extends WCV_TAUMOD {

	/** Constructor that a default instance of the WCV tables. */
	public WCV_TAUMOD_SUM() {
		table = new WCVTable();
		wcv_vertical = new WCV_TCOA();
	}

	/** Constructor that specifies a particular instance of the WCV tables. */
	public WCV_TAUMOD_SUM(WCVTable tab) {
		table = tab.copy();
		wcv_vertical = new WCV_TCOA();
	}

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

	private boolean WCV_taumod_uncertain(Vect3 s, Vect3 v, double s_err, double sz_err, double v_err, double vz_err) {
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

	private LossData vertical_WCV_uncertain_full_interval_szpos_vzpos(double T, double minsz,double maxsz, double minvz, double maxvz) {
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
		LossData intp = vel_only_neg ? LossData.EMPTY : vertical_WCV_uncertain_full_interval_szpos_vzpos(T,minsz,maxsz,Util.max(minvz,0),maxvz);
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

	private LossData WCV_taumod_uncertain_interval(double B, double T, Vect3 s, Vect3 v, 
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

	private boolean WCV_taumod_uncertain_detection(double B, double T, Vect3 s, Vect3 v, 
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

	public boolean violation(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return super.violation(so,vo,si,vi);
	}

	public boolean violationSUMAt(Vect3 so, Velocity vo, Vect3 si, Velocity vi, 
			double s_err, double sz_err, double v_err, double vz_err, double t) {
		if (s_err == 0.0 && sz_err == 0.0 && v_err == 0.0 && vz_err == 0.0) {
			return violation(so.AddScal(t,vo),vo,si.AddScal(t,vi),vi);
		} 
		Vect3 s = so.Sub(si);
		Vect3 v = vo.Sub(vi);
		Vect3 st = t == 0 ? s : v.ScalAdd(t,s);
		return WCV_taumod_uncertain(st,v,s_err+t*v_err,sz_err+t*vz_err,v_err,vz_err);
	}

	public boolean conflict(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		return super.conflict(so,vo,si,vi,B,T);
	}

	public boolean conflictSUM(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T,
			double s_err, double sz_err, double v_err, double vz_err) {
		if (s_err == 0.0 && sz_err == 0.0 && v_err == 0.0 && vz_err == 0.0) {
			return conflict(so,vo,si,vi,B,T);
		}		
		Vect3 s = so.Sub(si);
		Vect3 v = vo.Sub(vi);
		return WCV_taumod_uncertain_detection(B,T,s,v,s_err,sz_err,v_err,vz_err);
	}

	public ConflictData conflictDetection(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		return super.conflictDetection(so,vo,si,vi,B,T);
	}

	public ConflictData conflictDetectionSUM(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T,
			double s_err, double sz_err, double v_err, double vz_err) {
		if (s_err == 0.0 && sz_err == 0.0 && v_err == 0.0 && vz_err == 0.0) {
			return conflictDetection(so,vo,si,vi,B,T);
		}		
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

	/**
	 * Returns a deep copy of this WCV_TAUMOD object, including any results that have been calculated.  
	 */
	public WCV_TAUMOD_SUM copy() {
		WCV_TAUMOD_SUM ret = new WCV_TAUMOD_SUM(table);
		ret.id = id;
		return ret;
	}


	public boolean contains(Detection3D cd) {
		if (cd instanceof WCV_TAUMOD_SUM || cd instanceof WCV_TAUMOD || cd instanceof WCV_TCPA) {
			return containsTable((WCV_tvar)cd);
		}
		return false;
	}

}
