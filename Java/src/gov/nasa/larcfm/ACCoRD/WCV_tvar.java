/*
 * Copyright (c) 2012-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import java.util.List;

import gov.nasa.larcfm.Util.*;

/* Well Clear Volume concept based on time variable
 * DTHR, ZTHR, and TTHR are distance, altitude, and time thresholds, respectively 
 */

public abstract class WCV_tvar extends Detection3D {

	private String id_;	
	private WCV_Vertical wcv_vertical_;
	private WCVTable table_;

	public WCV_tvar(String id, WCV_Vertical wcv_vertical, WCVTable table) {
		id_ = id;
		wcv_vertical_ = wcv_vertical;
		table_ = table;
	}

	public abstract WCV_tvar copy();

	public abstract WCV_tvar make();

	public WCV_Vertical getWCVVertical() {
		return wcv_vertical_;
	}

	public WCVTable getWCVTable() {
		return table_;
	}

	/** 
	 * Sets the internal table to be a copy of the supplied one. 
	 **/
	public void setWCVTable(WCVTable tab) {
		table_ = tab.copy();
	}

	public double getDTHR()  {
		return table_.getDTHR();
	}

	public double getDTHR(String u)  {
		return table_.getDTHR(u);
	}

	public double getZTHR()  {
		return table_.getZTHR();
	}

	public double getZTHR(String u)  {
		return table_.getZTHR(u);
	}

	public double getTTHR()  {
		return table_.getTTHR();
	}

	public double getTTHR(String u)  {
		return table_.getTTHR(u);
	}

	public double getTCOA()  {
		return table_.getTCOA();
	}

	public double getTCOA(String u)  {
		return table_.getTCOA(u);
	}

	public void setDTHR(double val) {
		table_.setDTHR(val);
	}

	public void setDTHR(double val, String u) {
		table_.setDTHR(val,u);
	}   

	public void setZTHR(double val) {
		table_.setZTHR(val);
	}

	public void setZTHR(double val, String u) {
		table_.setZTHR(val,u);
	}

	public void setTTHR(double val) {
		table_.setTTHR(val);
	}

	public void setTTHR(double val, String u) {
		table_.setTTHR(val,u);
	}

	public void setTCOA(double val) {
		table_.setTCOA(val);
	}

	public void setTCOA(double val, String u) {
		table_.setTCOA(val,u);
	}

	abstract public double horizontal_tvar(Vect2 s, Vect2 v);

	abstract public LossData horizontal_WCV_interval(double T, Vect2 s, Vect2 v);

	public boolean horizontal_WCV(Vect2 s, Vect2 v) {
		if (s.norm() <= table_.DTHR) return true;
		if (Horizontal.dcpa(s,v) <= table_.DTHR) {
			double tvar = horizontal_tvar(s,v);
			return 0  <= tvar && tvar <= table_.TTHR;
		}
		return false;
	}

	// The methods violation and conflict are inherited from Detection3DSum. This enable a uniform
	// treatment of border cases in the generic bands algorithms

	public ConflictData conflictDetection(Vect3 so, Vect3 vo, Vect3 si, Vect3 vi, double B, double T) {    
		return WCV3D(so,vo,si,vi,B,T);
	}

	private ConflictData WCV3D(Vect3 so, Vect3 vo, Vect3 si, Vect3 vi, double B, double T) {
		LossData ld = WCV_interval(so,vo,si,vi,B,T);
		double t_tca = (ld.getTimeIn() + ld.getTimeOut())/2.0;
		double dist_tca = so.linear(vo, t_tca).Sub(si.linear(vi, t_tca)).cyl_norm(table_.DTHR,table_.ZTHR);
		return new ConflictData(ld,t_tca,dist_tca,so.Sub(si),vo.Sub(vi));
	}

	// Assumes 0 <= B < T
	private LossData WCV_interval(Vect3 so, Vect3 vo, Vect3 si, Vect3 vi, double B, double T) {
		double time_in = T;
		double time_out = B;

		Vect2 so2 = so.vect2();
		Vect2 si2 = si.vect2();
		Vect2 s2 = so2.Sub(si2);
		Vect2 vo2 = vo.vect2();
		Vect2 vi2 = vi.vect2();
		Vect2 v2 = vo2.Sub(vi2);
		double sz = so.z-si.z;
		double vz = vo.z-vi.z;

		Interval ii = wcv_vertical_.vertical_WCV_interval(table_.ZTHR,table_.TCOA,B,T,sz,vz);

		if (ii.low > ii.up) {
			return new LossData(time_in, time_out);
		}
		Vect2 step = v2.ScalAdd(ii.low,s2);
		if (Util.almost_equals(ii.low,ii.up)) { // [CAM] Changed from == to almost_equals to mitigate numerical problems
			if (horizontal_WCV(step,v2)) {
				time_in = ii.low;
				time_out = ii.up;
			}
			return new LossData(time_in, time_out);
		}
		LossData ld = horizontal_WCV_interval(ii.up-ii.low,step,v2);
		time_in = ld.getTimeIn() + ii.low;
		time_out = ld.getTimeOut() + ii.low;
		return new LossData(time_in, time_out);
	}

	public boolean containsTable(WCV_tvar wcv) {
		return table_.contains(wcv.table_);
	}

	public String toString() {
		return (id_.equals("") ? "" : id_+" : ")+getSimpleClassName()+" = {"+table_.toString()+"}";
	}

	public String toPVS() {
		return getSimpleClassName()+"("+table_.toPVS()+")";
	}

	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p;
	}

	public  void updateParameterData(ParameterData p) {
		table_.updateParameterData(p);
		p.set("id",id_);
	}

	public void setParameters(ParameterData p) {
		table_.setParameters(p);
		if (p.contains("id")) {
			id_ = p.getString("id");
		}
	}

	public String getSimpleClassName() {
		return getClass().getSimpleName();
	}

	public String getCanonicalClassName() {
		return getClass().getCanonicalName(); 
	}

	public String getIdentifier() {
		return id_;
	}

	public void setIdentifier(String s) {
		id_ = s;
	}

	public void horizontalHazardZone(List<Position> haz, 
			TrafficState ownship, TrafficState intruder, double T) {
		haz.clear();
		Position po = ownship.getPosition();
		Velocity v = ownship.getVelocity().Sub(intruder.getVelocity().vect3());
		if (Util.almost_equals(getTTHR()+T,0) || Util.almost_equals(v.vect3().norm2D(),0)) {
			CDCylinder.circular_arc(haz,po,Velocity.mkVxyz(getDTHR(),0,0),2*Math.PI,false);
		} else {
			Vect3 pu = Horizontal.unit_perpL(v.vect3());
			Velocity vD = Velocity.make(pu.Scal(getDTHR()));
			CDCylinder.circular_arc(haz,po,vD,Math.PI,true);	
			hazard_zone_far_end(haz,po,v,pu,T);
		}
	}

	public void hazard_zone_far_end(List<Position> haz,
			Position po, Velocity v, Vect3 pu, double T) {}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		WCV_tvar other = (WCV_tvar) obj;
		if (id_ == null) {
			if (other.id_ != null) {
				return false;
			}
		} else if (!id_.equals(other.id_)) {
			return false;
		}
		if (!table_.equals(other.table_)) {
			return false;
		}
		return true;
	}

}
