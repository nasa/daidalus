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
import gov.nasa.larcfm.Util.Vect3;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

public class ConflictData extends LossData {

	final protected double time_crit; // relative time to critical point
	final protected double dist_crit; // distance or severity at critical point (0 is most critical, +inf is least severe)
	final private Vect3  s_; // Relative position
	final private Velocity v_; // Relative velocity

	public ConflictData(double t_in, double t_out, double t_crit, double d_crit, Vect3 s, Velocity v) {
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
		v_ = Velocity.INVALID;
	}

	public static final ConflictData EMPTY = new ConflictData();

	public boolean isValid() {
		return !s_.isInvalid() && !v_.isInvalid();
	}

	public ConflictData(LossData ld, double t_crit, double d_crit, Vect3 s, Velocity v) {
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
	public Velocity get_v() {
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
		return Vect3.tcpa(s_,Vect3.ZERO,v_,Velocity.ZERO);
	}

	/**
	 * Time to co-altitude.
	 * @return time to co-altitude in seconds. Returns NaN is v_.z is zero.
	 */
	public double tcoa() {
		return Vertical.time_coalt(s_.z,v_.z);
	}

	/**
	 * Horizontal closure rate
	 * @return Horizontal closure rate in internal units at current time
	 */
	public double horizontalClosureRate() {
		return v_.norm2D();
	}

	/**
	 * Horizontal closure rate 
	 * @param u units
	 * @return Horizontal closure rate in specified units u at current time
	 */
	public double horizontalClosureRate(String u) {
		return Units.to(u,horizontalClosureRate());
	}

	/**
	 * Vertical closure rate
	 * @return Vertical closure rate in internal units at current time
	 */
	public double verticalClosureRate() {
		return Math.abs(v_.z);
	}

	/**
	 * Vertical closure rate
	 * @param u units
	 * @return Vertical closure rate in specified units at current time
	 */
	public double verticalClosureRate(String u) {
		return Units.to(u,verticalClosureRate());
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
