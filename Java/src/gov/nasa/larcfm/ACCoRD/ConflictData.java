/*
 * Copyright (c) 2015-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.LossData;
import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.f;

public class ConflictData extends LossData {

	final protected double time_crit; // relative time to critical point
	final protected double dist_crit; // distance or severity at critical point (0 is most critical, +inf is least severe)
	final private Vect3  s_; // Relative position
	final private Vect3 v_; // Relative velocity

	public ConflictData(double t_in, double t_out, double t_crit, double d_crit, Vect3 s, Vect3 v) {
		super(t_in, t_out);
		time_crit = t_crit;
		dist_crit = d_crit;
		s_ = s;
		v_ = v;
	}

	public ConflictData() {
		super();
		time_crit = Double.POSITIVE_INFINITY;
		dist_crit = Double.POSITIVE_INFINITY;
		s_ = Vect3.INVALID;
		v_ = Vect3.INVALID;
	}

	public static final ConflictData EMPTY = new ConflictData();

	public boolean isValid() {
		return !s_.isInvalid() && !v_.isInvalid();
	}

	public ConflictData(LossData ld, double t_crit, double d_crit, Vect3 s, Vect3 v) {
		super(ld.getTimeIn(), ld.getTimeOut());
		time_crit = t_crit;
		dist_crit = d_crit;
		s_ = s;
		v_ = v;
	}

	/**
	 * Returns internal vector representation of relative aircraft position.
	 */
	public Vect3 get_s() {
		return s_;
	}

	/**
	 * Returns internal vector representation of relative aircraft velocity.
	 */
	public Vect3 get_v() {
		return v_;
	}

	/**
	 * Returns HMD, in internal units, within lookahead time t, in seconds,
	 * assuming straight line trajectory.
	 */
	public double HMD(double T) {
		return Horizontal.hmd(s_.vect2(),v_.vect2(),T);
	}

	/**
	 * Returns HMD, in specified units, within lookahead time t, in seconds,
	 * assuming straight line trajectory.
	 */
	public double HMD(String u, double T) {
		return Units.to(u,HMD(T));
	}

	/**
	 * Returns VMD, in internal units, within lookahead time t, in seconds,
	 * assuming straight line trajectory.
	 */
	public double VMD(double T) {
		return Vertical.vmd(s_.z,v_.z,T);
	}

	/**
	 * Returns VMD, in specified units, within lookahead time t, in seconds,
	 * assuming straight line trajectory.
	 */
	public double VMD(String u, double T) {
		return Units.to(u,VMD(T));
	}

	/**
	 * Horizontal separation
	 * @return Horizontal separation in internal units at current time
	 */
	public double horizontalSeparation() {
		return s_.norm2D();
	}

	/**
	 * Horizontal separation
	 * @param u units
	 * @return Horizontal separation in specified units u at current time
	 */
	public double horizontalSeparation(String u) {
		return Units.to(u,horizontalSeparation());
	}

	/**
	 * Horizontal separation
	 * @param time in seconds
	 * @return Horizontal separation in internal units at given time
	 */
	public double horizontalSeparationAtTime(double time) {
		return s_.AddScal(time,v_).norm2D();
	}

	/**
	 * Horizontal separation at given time
	 * @param time time in seconds
	 * @param u units
	 * @return Horizontal separation in specified units at given time
	 */
	public double horizontalSeparationAtTime(String u, double time) {
		return Units.to(u,horizontalSeparationAtTime(time));
	}

	/**
	 * Vertical separation
	 * @return Vertical separation in internal units at current time
	 */
	public double verticalSeparation() {
		return Math.abs(s_.z);
	}

	/**
	 * Vertical separation
	 * @param u units
	 * @return Vertical separation in specified units at current time
	 */
	public double verticalSeparation(String u) {
		return Units.to(u,verticalSeparation());
	}

	/**
	 * Vertical separation at given time
	 * @param time time in seconds
	 * @return Vertical separation in internal units at given time
	 */
	public double verticalSeparationAtTime(double time) {
		return Math.abs(s_.AddScal(time,v_).z);
	}

	/**
	 * Vertical separation at given time
	 * @param time time in seconds
	 * @param u units
	 * @return Vertical separation in specified units at given time
	 */
	public double verticalSeparationAtTime(String u, double time) {
		return Units.to(u,verticalSeparationAtTime(time));
	}

	/**
	 * Time to horizontal closest point of approach in seconds.
	 * When aircraft are diverging, tcpa is defined as 0.
	 */
	public double tcpa2D() {
		return Util.max(0.0,Horizontal.tcpa(s_.vect2(),v_.vect2()));
	}

	/**
	 * Time to 3D closest point of approach in seconds.
	 * When aircraft are diverging, tcpa is defined as 0
	 */
	public double tcpa3D() {
		return Vect3.tcpa(s_,Vect3.ZERO,v_,Vect3.ZERO);
	}

	/**
	 * Time to co-altitude.
	 * @return time to co-altitude in seconds. Returns NaN is v_.z is zero.
	 */
	public double tcoa() {
		return Vertical.time_coalt(s_.z,v_.z);
	}

	/**
	 * Horizontal relative speed (ownship wrt intruder)
	 * @return Horizontal relative speed in internal units at current time
	 */
	public double horizontalRelativeSpeed() {
		return v_.norm2D();
	}

	/**
	 * Horizontal relative speed (ownship wrt intruder)
	 * @param u units
	 * @return Horizontal relative speed in specified units u at current time
	 */
	public double horizontalRelativeSpeed(String u) {
		return Units.to(u,horizontalRelativeSpeed());
	}

	/**
	 * Horizontal closure rate
	 * @return Horizontal closure rate in internal units at current time.
	 * If positive, aircraft are horizontally converging.
	 */
	public double horizontalClosureRate() {
		double sdotv = s_.dot2D(v_);
		if (Util.almost_equals(sdotv,0.0)) {
			return 0.0;
		}
		return -sdotv/s_.norm2D();
	}

	/**
	 * Horizontal closure rate speed
	 * @param u units
	 * @return Horizontal closure rate in specified units u at current time
	 * If positive, aircraft are horizontally converging.
	 */
	public double horizontalClosureRate(String u) {
		return Units.to(u,horizontalClosureRate());
	}

	/**
	 * Vertical relative speed (ownship wrt intruder)
	 * @return Vertical relative speed in internal units at current time
	 */
	public double verticalRelativeSpeed() {
		return v_.z;
	}

	/**
	 * Vertical relative speed (ownship wrt intruder)
	 * @param u units
	 * @return Vertical relative speed in specified units at current time
	 */
	public double verticalRelativeSpeed(String u) {
		return Units.to(u,verticalRelativeSpeed());
	}

	/**
	 * Vertical closure rate
	 * @return Vertical relative speed in internal units at current time
	 * If positive, aircraft are vertically converging.
	 */
	public double verticalClosureRate() {
		if (Util.almost_equals(s_.z*v_.z,0.0)) {
			return 0.0;
		}
		return -Util.sign(s_.z)*v_.z;
	}

	/**
	 * Vertical closure rate
	 * @param u units
	 * @return Vertical closure rate in specified units at current time
	 * If positive, aircraft are vertically converging.
	 */
	public double verticalClosureRate(String u) {
		return Units.to(u,verticalClosureRate());
	}

	// numeric value can be gotten via ordinal()+1, with 6 being "none"
	enum SLoWCClassification {
		SLoWC_Catastrophic, SLoWC_Hazardous, SLoWC_Major, SLoWC_Minor, SLoWC_Minimal, SLoWC_None
	}

	/**
	 * return SLoWC classification for SC-228 SLoWC value
	 **/
	public static SLoWCClassification classifySLoWC(double slowc) {
		if (slowc <= 0.0) return SLoWCClassification.SLoWC_None;
		else if (slowc <= 17) return SLoWCClassification.SLoWC_Minimal;
		else if (slowc <= 33) return SLoWCClassification.SLoWC_Minor;
		else if (slowc <= 47) return SLoWCClassification.SLoWC_Major;
		else if (slowc <= 94) return SLoWCClassification.SLoWC_Hazardous;
		else return SLoWCClassification.SLoWC_Catastrophic;
	}

	private static double squircle(double x, double y) {
		double sqx = Util.sq(x);
		double sqy = Util.sq(y);
		return Util.sqrt_safe(sqx + (1-sqx)*sqy);
	}

	/**
	 * Severity of Loss of Well Clear calculation for one time instant, 0.0 (minimal) - 100.0 (catastrophic).
	 * For an encounter, it should take max of every instance (SC-228)
	 **/
	public double SLoWC(double DMOD, double TAUMOD, double ZTHR) {
		Vect2 s2 = s_.vect2();
		Vect2 v2 = v_.vect2();

		double r_i = s2.norm();
		double rdot_i = Util.almost_equals(r_i,0.0) ? 0.0 : s2.dot(v2)/r_i;

		double rdotTAUMOD = rdot_i*TAUMOD;
		double S_i = Util.max(DMOD, (Util.sqrt_safe(Util.sq(rdotTAUMOD)+4*Util.sq(DMOD))-rdotTAUMOD)/2.0);
		double RangePen_i = Util.almost_equals(r_i,0.0) ? 0 : Util.min(r_i/S_i, 1.0);

		double sqv2 = v2.sqv();
		double t_CPA = Util.almost_equals(sqv2,0.0) ? 0.0 : -s2.dot(v2)/v2.sqv();
		double HMD_i = s2.AddScal(t_CPA, v2).norm();
		double HMDPen_i = Util.almost_equals(DMOD,0.0) ? 0 : Util.min(HMD_i/DMOD, 1.0);

		double dh_i = Math.abs(s_.z);
		double VertPen_i = Util.almost_equals(ZTHR,0.0) ? 0.0 : Util.min(dh_i/ZTHR, 1.0);

		double slowc = (1.0 - squircle(RangePen_i, squircle(HMDPen_i, VertPen_i))) * 100.0;

		return slowc;
	}

	/**
	 * @return A time in seconds that can be used to compare severity of conflicts for arbitrary
	 * well-clear volumes. This time is not necessarily TCPA. ** Don't use it as TCPA. **
	 */
	public double getCriticalTimeOfConflict() {
		return time_crit;
	}

	/**
	 * @return A non-negative scalar that can be used to compare severity of conflicts for arbitrary
	 * well-clear volumes. This scalar is a distance in the mathematical way. It is 0 when aircraft are
	 * at the same poistion, but it isn't a distance in the physical way. In particular, this distance
	 * is unitless. ** Don't use as CPA **
	 */
	public double getDistanceAtCriticalTime() {
		return dist_crit;
	}

	public String toString() {
		String str = super.toString()+" [time_crit: "+f.FmPrecision(time_crit)+
				", dist_crit: "+f.FmPrecision(dist_crit)+"]";
		return str;
	}

}
