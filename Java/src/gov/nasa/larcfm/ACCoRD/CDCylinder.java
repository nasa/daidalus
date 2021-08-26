/*
 * CD3D.java 
 * Release: ACCoRDj-2.b (08/22/10) 
 *
 * CD3D is an algorithm for 3-D conflict *detection*.
 *
 * Unit Convention
 * ---------------
 * All units in this file are *internal*:
 * - Units of distance are denoted [d]
 * - Units of time are denoted     [t]
 * - Units of speed are denoted    [d/t]
 *
 * REMARK: X Vect3s to East, Y Vect3s to North. 
 *
 * Naming Convention
 * -----------------
 *   The intruder is fixed at the origin of the coordinate system.
 * 
 *   D  : Diameter of the protected zone [d]
 *   H  : Height of the protected zone [d]
 *   B  : Lower bound of lookahead time interval [t] (B >= 0)
 *   T  : Upper bound of lookahead time interval [t] (T < 0 means infinite lookahead time)
 *   s  : Relative 3-D position of the ownship [d,d,d]
 *   vo : Ownship velocity vector [d/t,d/t,d/t]
 *   vi : Traffic velocity vector [d/t,d/t,d/t]
 * 
 * Functions
 * ---------
 * violation : Check for 3-D loss of separation
 * detection : 3-D conflict detection with calculation of conflict interval 
 * cd3d      : Check for predicted conflict
 * 
 * Global variables (modified by detection)
 * ----------------
 * t_in  : Time to loss of separation
 * t_out : Time to recovery of loss of separation
 * 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 * 
 * 
 * Note: The B and T parameters also affect the time in and time out of loss values.
 * 
 */

package gov.nasa.larcfm.ACCoRD;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.ParameterData;
import gov.nasa.larcfm.Util.Position;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class CDCylinder extends Detection3D {

	private double D_;
	private double H_;
	private Map<String,String> units_;

	private String id = "";

	/**
	 * Instantiates a new CDCylinder object.
	 */
	public CDCylinder(String s) {
		D_ = Units.from("nmi",5.0);
		H_ = Units.from("ft",1000.0);
		units_ = new HashMap<String,String>();
		units_.put("D","nmi");
		units_.put("H","ft");
		id = s;
	}

	/**
	 * Instantiates a new CDCylinder object.
	 */
	public CDCylinder() {
		this("");
	}

	public CDCylinder(CDCylinder cdc) {
		D_ = cdc.D_;
		H_ = cdc.H_;
		units_ = new HashMap<String,String>();
		units_.putAll(cdc.units_);
		id = cdc.id;
	}

	public CDCylinder(double d, double h) {
		this(d,"m",h,"m");
	}

	public CDCylinder(double d, String dunit, double h, String hunit) {
		D_ = Units.from(dunit,Math.abs(d));
		H_ = Units.from(hunit,Math.abs(h));
		units_ = new HashMap<String,String>();
		units_.put("D",dunit);
		units_.put("H",hunit);
		id = "";
	}

	/**
	 * Create a new state-based conflict detection object using specified units.
	 * 
	 * @param distance the minimum horizontal separation distance in specified units
	 * @param dUnits units for the distance
	 * @param height the minimum vertical separation height in specified units.
	 * @param hUnits units for the height
	 * @return 
	 */
	public static CDCylinder make(double distance, String dUnits, double height, String hUnits) {
		return new CDCylinder(distance,dUnits,height,hUnits);  
	}

	/**
	 * Create a new state-based conflict detection object using internal units.
	 * 
	 * @param distance the minimum horizontal separation distance [m]
	 * @param height the minimum vertical separation height [m].
	 */
	public static CDCylinder mk(double distance, double height) {
		return new CDCylinder(distance, "m", height, "m");  
	}

	/**
	 * One static CDCylinder
	 */
	public static final CDCylinder A_CDCylinder =
			new CDCylinder();

	/**
	 * CDCylinder thresholds, i.e., D=5nmi, H=1000ft.
	 */
	public static final CDCylinder CD3DCylinder = A_CDCylinder;

	public String getUnits(String key) {
		String u = units_.get(key);
		if (u == null) {
			return "unspecified";
		}
		return u;
	}

	public double getHorizontalSeparation() {
		return D_;
	}

	public void setHorizontalSeparation(double d) {
		D_ = Math.abs(d);
	}

	public double getVerticalSeparation() {
		return H_;
	}

	public void setVerticalSeparation(double h) {
		H_ = Math.abs(h);
	}

	public double getHorizontalSeparation(String u) {
		return Units.to(u, D_);
	}

	public void setHorizontalSeparation(double d, String u) {
		setHorizontalSeparation(Units.from(u,d));
		units_.put("D",u);
	}

	public double getVerticalSeparation(String u) {
		return Units.to(u, H_);
	}

	public void setVerticalSeparation(double h, String u) {
		setVerticalSeparation(Units.from(u,h));
		units_.put("H",u);
	}

	/**
	 * Computes the conflict time interval in [B,T].
	 * 
	 * @param s the relative position of the aircraft
	 * @param vo the ownship's velocity
	 * @param vi the intruder's velocity
	 * @param D the minimum horizontal distance
	 * @param H the minimum vertical distance
	 * @param B the the lower bound of the lookahead time ({@code B >= 0})
	 * @param T the upper bound of the lookahead time ({@code B < T})
	 * 
	 * @return true, if the conflict time interval (t_in,t_out) is in [B,T].
	 */
	public LossData detection(Vect3 s, Vect3 vo, Vect3 vi, double D, double H, double B, double T) { 
		return CD3D.detection(s,vo,vi,D,H,B,T);
	}

	/**
	 * Computes the conflict time interval in [0,T].
	 * 
	 * @param s the relative position of the aircraft
	 * @param vo the ownship's velocity
	 * @param vi the intruder's velocity
	 * @param D the minimum horizontal distance
	 * @param H the minimum vertical distance
	 * @param T the the lookahead time ({@code T > 0})
	 * 
	 * @return true, if the conflict time interval (t_in,t_out) is in [0,T].
	 */
	public LossData detection(Vect3 s, Vect3 vo, Vect3 vi, double D, double H, double T) {
		return detection(s,vo,vi,D,H,0,T);
	}

	/**
	 * Computes the conflict time interval in [0,...).
	 * 
	 * @param s the relative position of the aircraft
	 * @param vo the ownship's velocity
	 * @param vi the intruder's velocity
	 * @param D the minimum horizontal distance
	 * @param H the minimum vertical distance
	 * 
	 * @return true, if the conflict time interval (t_in,t_out) is in [0,...)
	 */
	public LossData detection(Vect3 s, Vect3 vo, Vect3 vi, double D, double H) {
		return detection(s,vo,vi,D,H,0,Double.POSITIVE_INFINITY);
	}

	public String toString() {
		return (id.equals("") ? "" : id+" : ")+getSimpleClassName()+" = {D = "+
				Units.str(getUnits("D"),D_)+", H = "+Units.str(getUnits("H"),H_)+"}";
	}

	public String toPVS() {
		return getSimpleClassName()+"((# D:= "+f.FmPrecision(D_)+", H:= "+f.FmPrecision(H_)+" #))";
	}

	@Deprecated
	public static boolean violation(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double D, double H) {
		return CD3D.lossOfSep(so,si,D,H);
	}

	@Deprecated
	public static boolean conflict(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double D, double H, double B, double T) {
		return CD3D.cd3d(so.Sub(si), vo, vi, D, H, B, T); 
	}

	// The non-static methods violation and conflict are
	// inherited from Detection3DSum. This enable a uniform
	// treatment of border cases in the generic bands algorithms

	public static ConflictData conflict_detection(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double D, double H, double B, double T) {
		Vect3 s = so.Sub(si);
		Velocity v = vo.Sub(vi);
		double t_tca = CD3D.tccpa(s, vo, vi, D, H, B, T);
		double dist_tca = s.linear(v,t_tca).cyl_norm(D, H);
		LossData ld = CD3D.detection(s,vo,vi,D,H,B,T);
		return new ConflictData(ld,t_tca,dist_tca,s,v);
	}

	public ConflictData conflictDetection(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		return conflict_detection(so,vo,si,vi,D_,H_,B,T); 
	}

	public static double time_of_closest_approach(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double D, double H, double B, double T) {
		return CD3D.tccpa(so.Sub(si),vo,vi,D,H,B,T);
	}

	public double timeOfClosestApproach(Vect3 so, Velocity vo, Vect3 si, Velocity vi, double B, double T) {
		return time_of_closest_approach(so,vo,si,vi,D_,H_,B,T); 
	}

	public ParameterData getParameters() {
		ParameterData p = new ParameterData();
		updateParameterData(p);
		return p;
	}

	public void updateParameterData(ParameterData p) {
		p.setInternal("D",D_,getUnits("D"));
		p.setInternal("H",H_,getUnits("H"));
		p.set("id",id);
	}

	public void setParameters(ParameterData p) {
		if (p.contains("D")) {
			setHorizontalSeparation(p.getValue("D"));
			units_.put("D",p.getUnit("D"));
		}
		if (p.contains("H")) {
			setVerticalSeparation(p.getValue("H"));
			units_.put("H",p.getUnit("H"));
		}
		if (p.contains("id")) {
			id = p.getString("id");
		}
	}

	/**
	 * Returns a fresh instance of this type of Detection3D with default parameter data.
	 */
	public CDCylinder make() {
		return new CDCylinder();
	}

	/**
	 * Returns a deep copy of this CDCylinder object, including any results that have been calculated.  This will duplicate parameter data, but will NOT
	 * link any existing CD3DTable.  Call setCD3DTable() if linking is necessary.
	 */
	public CDCylinder copy() {
		CDCylinder cd = new CDCylinder(this);
		return cd;
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
		CDCylinder other = (CDCylinder) obj;
		if (id == null) {
			if (other.id != null)
				return false;
		} else if (!id.equals(other.id))
			return false;
		if (Double.doubleToLongBits(D_) != Double.doubleToLongBits(other.D_))
			return false;
		if (Double.doubleToLongBits(H_) != Double.doubleToLongBits(other.H_))
			return false;
		return true;
	}

	public boolean contains(Detection3D det) {
		if (det instanceof CDCylinder) {
			CDCylinder cd = (CDCylinder) det;
			return D_ >= cd.D_ && H_ >= cd.H_; 
		}
		return false;
	}

	/* Return a list of point representing a counter-clockwise circular arc centered at pc, 
	 * whose first point is pc+v(0), and the last one is pc+v(alpha), where alpha is 
	 * in [0,2*pi]. 
	 */
	public static void circular_arc(List<Position> arc, Position pc, Velocity v, 
			double alpha, boolean include_last) {
		alpha = Util.almost_equals(alpha,2*Math.PI,DaidalusParameters.ALMOST_) ? alpha :  Util.to_2pi(alpha);
		double step = Math.PI/180;
		arc.add(pc.linear(v,1));
		double current_trk = v.trk();
		for (double a = step; Util.almost_less(a,alpha,DaidalusParameters.ALMOST_); a += step) {
			arc.add(pc.linear(v.mkTrk(current_trk-a),1));
		}
		if (include_last) {
			arc.add(pc.linear(v.mkTrk(current_trk-alpha),1));
		}
	}

	public void horizontalHazardZone(List<Position> haz, 
			TrafficState ownship, TrafficState intruder, double T) {
		haz.clear();
		Position po = ownship.getPosition();
		Velocity v = ownship.getVelocity().Sub(intruder.getVelocity());
		if (Util.almost_equals(T,0) || Util.almost_equals(v.norm2D(),0)) {
			circular_arc(haz,po,Velocity.mkVxyz(D_,0,0),2*Math.PI,false);
		} else {
			Vect3 sD = Horizontal.unit_perpL(v).Scal(D_);
			Velocity vD = Velocity.make(sD);
			circular_arc(haz,po,vD,Math.PI,true);	
			circular_arc(haz,po.linear(v,T),vD.Neg(),Math.PI,true);
		}
	}

}
