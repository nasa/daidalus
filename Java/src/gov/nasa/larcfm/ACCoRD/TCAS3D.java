/*
 * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.*;

public class TCAS3D extends Detection3D {
	private String id = "";

	private TCASTable table_;

	/** Constructor that uses the default TCAS RA tables. */
	public TCAS3D() {
		table_ = TCASTable.make_TCASII_Table(true);
	}

	/** Constructor that specifies a particular instance of the TCAS tables. */
	public TCAS3D(TCASTable table) {
		table_ = TCASTable.make_Empty_TCASTable();
		table_.set(table);
	}

	/**
	 * One static TCAS3D
	 */
	public static final TCAS3D A_TCAS3D =
			new TCAS3D();

	/** Make TCAS3D object with empty Table **/
	public static TCAS3D make_Empty() {
		TCAS3D tcas3d = new TCAS3D();
		tcas3d.table_.clear();
		return tcas3d;
	}

	/** Make TCAS3D object with an RA Table **/
	public static TCAS3D make_TCASII_RA() {
		TCAS3D tcas3d = new TCAS3D();
		return tcas3d;
	}

	/** Make TCAS3D objec with a TA Table **/
	public static TCAS3D make_TCASII_TA() {
		TCAS3D tcas3d = new TCAS3D();
		tcas3d.table_.setDefaultTCASIIThresholds(false);
		return tcas3d;
	}

	/** This returns a reference to the internal TCAS table */
	public TCASTable getTCASTable() {
		return table_;
	}

	/**
	 * Set table to TCASII Thresholds (RA Table when ra is true, TA Table when ra is false)
	 */
	public void setDefaultTCASIIThresholds(boolean ra) {
		table_.setDefaultTCASIIThresholds(ra);
	}

	/** This sets the internal table to be a deep copy of the supplied one.  Any previous links will be broken. */
	public void setTCASTable(TCASTable table) {
		table_.set(table); 
	}

	// The methods violation and conflict are inherited from Detection3DSum. This enable a uniform
	// treatment of border cases in the generic bands algorithms

	public ConflictData conflictDetection(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		return RA3D(so,vo,si,vi,B,T);
	}

	public TCAS3D make() {
		return new TCAS3D();
	}

	/**
	 * Returns a deep copy of this TCAS3D object, including any results that have been calculated.  This will duplicate parameter data, but will NOT
	 * link any existing TCASTable.  Call setTCASTables() if linking is necessary.
	 */
	public TCAS3D copy() {
		TCAS3D ret = new TCAS3D();
		ret.table_.set(table_);
		ret.id = id;
		return ret;
	}

	static public boolean vertical_RA(double sz, double vz, double ZTHR, double TCOA) {
		if (Math.abs(sz) <= ZTHR) return true;
		if (Util.almost_equals(vz,0)) return false; // [CAM] Changed from == to almost_equals to mitigate numerical problems 
		double tcoa = Vertical.time_coalt(sz,vz);
		return 0 <= tcoa && tcoa <= TCOA;
	}

	static boolean cd2d_TCAS_after(double HMD, Vect2 s, Vect2 vo, Vect2 vi, double t) {
		Vect2 v = vo.Sub(vi);
		return  
				(vo.almostEquals(vi) && s.sqv() <= Util.sq(HMD)) ||
				(v.sqv() > 0 && Horizontal.Delta(s,v,HMD) >= 0 &&
				Horizontal.Theta_D(s,v,1,HMD) >= t);
	} 

	static boolean cd2d_TCAS(double HMD, Vect2 s, Vect2 vo, Vect2 vi) {
		return cd2d_TCAS_after(HMD,s,vo,vi,0);
	}

	// if true, then ownship has a TCAS resolution advisory at current time
	public boolean TCASII_RA(Vect3 so, Vect3 vo, Vect3 si, Vect3 vi) {

		Vect2 so2 = so.vect2();
		Vect2 si2 = si.vect2();
		Vect2 s2 = so2.Sub(si2);
		Vect2 vo2 = vo.vect2();
		Vect2 vi2 = vi.vect2();
		Vect2 v2 = vo2.Sub(vi2);
		int sl = table_.getSensitivityLevel(so.z);
		boolean usehmdf = table_.getHMDFilter();
		double TAU  = table_.getTAU(sl);
		double TCOA = table_.getTCOA(sl);
		double DMOD = table_.getDMOD(sl);
		double HMD  = table_.getHMD(sl);
		double ZTHR = table_.getZTHR(sl);

		return (!usehmdf || cd2d_TCAS(HMD,s2,vo2,vi2)) &&
				TCAS2D.horizontal_RA(DMOD,TAU,s2,v2) &&
				vertical_RA(so.z-si.z,vo.z-vi.z,ZTHR,TCOA);
	}

	// if true, within lookahead time interval [B,T], the ownship has a TCAS resolution advisory (effectively conflict detection)
	// B must be non-negative and B < T.

	public ConflictData RA3D(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {

		Vect3 s = so.Sub(si);
		Velocity v = vo.Sub(vi);
		Vect2 so2 = so.vect2();
		Vect2 vo2 = vo.vect2();
		Vect2 si2 = si.vect2();
		Vect2 vi2 = vi.vect2();

		int max_sl = table_.getMaxSensitivityLevel();
		double DMOD_max = table_.getDMOD(max_sl);
		double ZTHR_max = table_.getZTHR(max_sl);

		double tin = Double.POSITIVE_INFINITY;
		double tout = Double.NEGATIVE_INFINITY;
		double tmin = Double.POSITIVE_INFINITY;
		int sl_first = table_.getSensitivityLevel(so.z+B*vo.z);
		int sl_last = table_.getSensitivityLevel(so.z+T*vo.z);
		if (sl_first == sl_last || Util.almost_equals(vo.z,0.0)) {
			Triple<Double,Double,Double> ra3dint = RA3D_interval(sl_first,so2,so.z,vo2,vo.z,si2,si.z,vi2,vi.z,B,T);
			tin = ra3dint.first;
			tout = ra3dint.second;
			tmin = ra3dint.third;
		} else {
			int sl = sl_first;
			for (double t_B = B; t_B < T; sl = sl_first < sl_last ? sl+1 : sl-1) {
				if (table_.isValidSensitivityLevel(sl)) {
					double level = sl_first < sl_last ? table_.getLevelAltitudeUpperBound(sl) :table_.getLevelAltitudeLowerBound(sl);
					double t_level = Double.isInfinite(level) ? Double.POSITIVE_INFINITY :(level-so.z)/vo.z; 
					Triple<Double,Double,Double> ra3dint = RA3D_interval(sl,so2,so.z,vo2,vo.z,si2,si.z,vi2,vi.z,t_B,Util.min(t_level,T));
					if (Util.almost_less(ra3dint.first,ra3dint.second)) {
						tin = Util.min(tin,ra3dint.first);
						tout = Util.max(tout,ra3dint.second);
					}
					tmin = Util.min(tmin,ra3dint.third);
					t_B = t_level;
					if (sl == sl_last) {
						break;
					}
				}
			}
		} 
		double dmin = s.linear(v, tmin).cyl_norm(DMOD_max, ZTHR_max);
		return new ConflictData(tin,tout,tmin,dmin,s,v);
	}


	public Triple<Double,Double,Double> RA3D_interval(int sl, Vect2 so2, double soz, Vect2 vo2, double voz,
			Vect2 si2, double siz, Vect2 vi2, double viz, double B, double T) {
		double time_in_     = T;
		double time_out_    = B;
		double time_mintau_ = Double.POSITIVE_INFINITY;
		Vect2 s2 = so2.Sub(si2);
		Vect2 v2 = vo2.Sub(vi2);
		double sz = soz-siz;
		double vz = voz-viz;
		boolean usehmdf = table_.getHMDFilter();
		double TAU  = table_.getTAU(sl);
		double TCOA = table_.getTCOA(sl);
		double DMOD = table_.getDMOD(sl);
		double HMD  = table_.getHMD(sl);
		double ZTHR = table_.getZTHR(sl);

		if (usehmdf && !cd2d_TCAS_after(HMD,s2,vo2,vi2,B)) {
			time_mintau_ = TCAS2D.time_of_min_tau(DMOD,B,T,s2,v2);
		} else {
			if (Util.almost_equals(voz, viz) && Math.abs(sz) > ZTHR) {
				time_mintau_ = TCAS2D.time_of_min_tau(DMOD,B,T,s2,v2);
			} else {
				double tentry = B;
				double texit  = T;
				if (!Util.almost_equals(voz, viz)) {
					double act_H = Util.max(ZTHR,Math.abs(vz)*TCOA);
					tentry = Vertical.Theta_H(sz,vz,-1,act_H);
					texit = Vertical.Theta_H(sz,vz,1,ZTHR);
				}
				Vect2 ventry = v2.ScalAdd(tentry,s2);
				boolean exit_at_centry = ventry.dot(v2) >= 0;
				boolean los_at_centry = ventry.sqv() <= Util.sq(HMD);
				if (texit < B || T < tentry) {
					time_mintau_ = TCAS2D.time_of_min_tau(DMOD,B,T,s2,v2);
				} else {
					double tin = Util.max(B,tentry);
					double tout = Util.min(T,texit);
					TCAS2D tcas2d = new TCAS2D();
					tcas2d.RA2D_interval(DMOD,TAU,tin,tout,s2,vo2,vi2);
					double RAin2D = tcas2d.time_in;
					double RAout2D = tcas2d.time_out;
					double RAin2D_lookahead = Util.max(tin,Util.min(tout,RAin2D));
					double RAout2D_lookahead = Util.max(tin,Util.min(tout,RAout2D));
					if (RAin2D > RAout2D || RAout2D<tin || RAin2D > tout ||
						(usehmdf && HMD < DMOD && exit_at_centry && !los_at_centry)) { 
						time_mintau_ = TCAS2D.time_of_min_tau(DMOD,B,T,s2,v2);
					} else {
						if (usehmdf && HMD < DMOD) {
							double exitTheta = T;
							if (v2.sqv() > 0) 
								exitTheta = Util.max(B,Util.min(Horizontal.Theta_D(s2,v2,1,HMD),T));
							double minRAoutTheta = Util.min(RAout2D_lookahead,exitTheta);
							time_in_ = RAin2D_lookahead;
							time_out_ = minRAoutTheta;
							if (RAin2D_lookahead <= minRAoutTheta) {
								time_mintau_ = TCAS2D.time_of_min_tau(DMOD,RAin2D_lookahead,minRAoutTheta,s2,v2);
							} else {
								time_mintau_ = TCAS2D.time_of_min_tau(DMOD,B,T,s2,v2);
							}
						} else {
							time_in_ = RAin2D_lookahead;
							time_out_ = RAout2D_lookahead;
							time_mintau_ = TCAS2D.time_of_min_tau(DMOD,RAin2D_lookahead,RAout2D_lookahead,s2,v2);
						}
					}
				}
			}
		}
		return Triple.make(time_in_, time_out_, time_mintau_);
	}

	/**
	 * Returns TAU threshold for sensitivity level sl in seconds
	 */
	public double getTAU(int sl)  {
		return table_.getTAU(sl);
	}

	/**
	 * Returns TCOA threshold for sensitivity level sl in seconds
	 */
	public double getTCOA(int sl)  {
		return table_.getTCOA(sl);
	}

	/**
	 * Returns DMOD for sensitivity level sl in internal units.
	 */
	public double getDMOD(int sl)  {
		return table_.getDMOD(sl);
	}

	/**
	 * Returns DMOD for sensitivity level sl in u units.
	 */
	public double getDMOD(int sl, String u)  {
		return table_.getDMOD(sl,u);
	}

	/**
	 * Returns Z threshold for sensitivity level sl in internal units.
	 */
	public double getZTHR(int sl)  {
		return table_.getZTHR(sl);
	}

	/**
	 * Returns Z threshold for sensitivity level sl in u units.
	 */
	public double getZTHR(int sl,String u)  {
		return table_.getZTHR(sl,u);
	}

	/**
	 * Returns HMD for sensitivity level sl in internal units.
	 */
	public double getHMD(int sl)  {
		return table_.getHMD(sl);
	}

	/**
	 * Returns HMD for sensitivity level sl in u units.
	 */
	public double getHMD(int sl, String u)  {
		return table_.getHMD(sl,u);
	}

	/** Modify the value of Tau Threshold for a given sensitivity level (2-8)
	 * Parameter val is given in seconds 
	 */
	public void setTAU(int sl, double val) {
		table_.setTAU(sl,val);
	}

	/** Modify the value of TCOA Threshold for a given sensitivity level (2-8)
	 * Parameter val is given in seconds 
	 */
	public void setTCOA(int sl, double val) {
		table_.setTCOA(sl,val);
	}

	/** Modify the value of DMOD for a given sensitivity level (2-8)
	 * Parameter val is given in internal units
	 */
	public void setDMOD(int sl, double val) { 
		table_.setDMOD(sl, val);
	}

	/** Modify the value of DMOD for a given sensitivity level (2-8)
	 * Parameter val is given in u units
	 */
	public void setDMOD(int sl, double val, String u) { 
		table_.setDMOD(sl,val,u);
	}

	/** Modify the value of ZTHR for a given sensitivity level (2-8)
	 * Parameter val is given in internal units
	 */
	public void setZTHR(int sl, double val) {
		table_.setZTHR(sl,val);
	}

	/** Modify the value of ZTHR for a given sensitivity level (2-8)
	 * Parameter val is given in u units
	 */
	public void setZTHR(int sl, double val, String u) {
		table_.setZTHR(sl,val,u);
	}

	/** 
	 * Modify the value of HMD for a given sensitivity level (2-8)
	 * Parameter val is given in internal units
	 */
	public void setHMD(int sl, double val) {
		table_.setHMD(sl,val);
	}

	/** 
	 * Modify the value of HMD for a given sensitivity level (2-8)
	 * Parameter val is given in u units
	 */
	public void setHMD(int sl, double val, String u) {
		table_.setHMD(sl,val,u);
	}

	public void setHMDFilter(boolean flag) {
		table_.setHMDFilter(flag);
	}

	public boolean getHMDFilter() {
		return table_.getHMDFilter();
	}

	public String toString() {
		return (id.equals("") ? "" : id+" = ")+getSimpleClassName()+": {"+table_.toString()+"}";
	}

	public String toPVS() {
		return getSimpleClassName()+"("+table_.toPVS()+")";
	}

	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p;
	}

	public void updateParameterData(ParameterData p) {
		table_.updateParameterData(p);
		p.set("id",id);
	}

	public void setParameters(ParameterData p) {
		table_.setParameters(p);
		if (p.contains("id")) {
			id = p.getString("id");
		}
	}

	public String getSimpleClassName() {
		return getClass().getSimpleName();
	}

	public String getCanonicalClassName() {
		return getClass().getCanonicalName(); 
	}

	public String getIdentifier() {
		return id;
	}

	public void setIdentifier(String s) {
		id = s;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TCAS3D other = (TCAS3D) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (table_ == null) {
			if (other.table_ != null)
				return false;
		} else if (!table_.equals(other.table_))
			return false;

		return true;
	}

	public boolean contains(Detection3D cd) {
		if (cd instanceof TCAS3D) {
			TCAS3D d = (TCAS3D) cd;
			return table_.contains(d.table_);
		}
		return false;
	}

	public void horizontalHazardZone(List<Position>haz, TrafficState ownship, TrafficState intruder, double T) {
		int sl = table_.getSensitivityLevel(ownship.altitude());
		boolean usehmdf = table_.getHMDFilter();
		double TAUMOD  = table_.getTAU(sl);
		double DMOD = Util.max(table_.getDMOD(sl),table_.getHMD(sl));
		haz.clear();
		Position po = ownship.getPosition();
		Velocity v = ownship.getVelocity().Sub(intruder.getVelocity());
		if (Util.almost_equals(TAUMOD+T,0) || Util.almost_equals(v.norm2D(),0)) {
			CDCylinder.circular_arc(haz,po,Velocity.mkVxyz(DMOD,0,0),2*Math.PI,false);
		} else {
			Vect3 sD = Horizontal.unit_perpL(v).Scal(DMOD);
			Velocity vD = Velocity.make(sD);
			CDCylinder.circular_arc(haz,po,vD,Math.PI,usehmdf);	
			Position TAU_center = WCV_TAUMOD.TAU_center(po,v,TAUMOD,T);
			Vect3 vC = v.Scal(0.5*TAUMOD);     // TAUMOD Center (relative)
			if (usehmdf) {
				Vect3 vDC = vC.Sub(vD); // Far end point opposite to -vD (vC-relative);
				Vect3 nvDC = vC.Add(vD); // Far end point opposite to vD (vC-relative);
				double sqa = vDC.sqv2D();
				double alpha = Util.atan2_safe(vDC.det2D(nvDC)/sqa,vDC.dot2D(nvDC)/sqa);	
				Velocity velDC = Velocity.make(vDC);
				CDCylinder.circular_arc(haz,TAU_center,velDC,alpha,true);				
			} else {
				Vect3 nsCD=sD.Neg().Sub(vC);
				Vect3 sCD=sD.Sub(vC);
				double sqa = sCD.sqv2D();
				Velocity nvCD = Velocity.make(nsCD);
				if (Util.almost_equals(T,0)) { // Two circles: DMOD and TAUMO. They intersect at +/-vD
					double alpha = Util.atan2_safe(nsCD.det2D(sCD)/sqa,nsCD.dot2D(sCD)/sqa);	
					CDCylinder.circular_arc(haz,TAU_center,nvCD,alpha,false);		
				} else { // Two circles: DMOD and TAUMOD. They intersect at +/- vD. 
					Vect3 sT = Horizontal.unit_perpL(v).Scal(Math.sqrt(sqa));
					Velocity vT = Velocity.make(sT);
					Vect3 nsT = sT.Neg();
					Velocity nvT = Velocity.make(nsT);
					double alpha = Util.atan2_safe(nsCD.det2D(nsT)/sqa,nsCD.dot2D(nsT)/sqa);	
					Position TAU_center_0 = WCV_TAUMOD.TAU_center(po,v,TAUMOD,0);
					CDCylinder.circular_arc(haz,TAU_center_0,nvCD,alpha,true);	
					CDCylinder.circular_arc(haz,TAU_center,nvT,Math.PI,true);	
					CDCylinder.circular_arc(haz,TAU_center_0,vT,alpha,false);				}
			} 			
		}
	}

}
