/*
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.ACCoRD;

import gov.nasa.larcfm.Util.Util;
import gov.nasa.larcfm.Util.Vect2;
import gov.nasa.larcfm.Util.Vect3;
//import gov.nasa.larcfm.Util.Units;
import gov.nasa.larcfm.Util.Velocity;
import gov.nasa.larcfm.Util.f;

import static gov.nasa.larcfm.ACCoRD.Consts.*;

/** Vertical solution */
public class Vertical {

	public double z;
	private boolean undef;

	Vertical() {
		z = Double.NaN;
		undef = true;
	}

	Vertical(double vz) {
		z = vz;
		undef = false;
	}

	/** method */
	private Vertical add_this(double vz) {
		if (!undef) {
			z += vz;
		}
		return this;
	}

	/** Is this solution undefined? */
	public boolean undef() {
		return undef;
	}

	/* "Solution" indicating no solution: Warning do not test with == use undef instead! */
	static Vertical NoVerticalSolution = new Vertical();

	/** */
	public static boolean almost_vertical_los(double sz, double H) {
		double absz = Math.abs(sz);
		return !Util.almost_equals(absz,H) && absz < H;
	}

	// Computes times when sz,vz intersects rectangle of half height H
	// eps = -1 : Entry
	// eps =  1 : Exit
	public static double Theta_H(double sz, double vz, int eps, double H) {
		if (vz == 0) return Double.NaN;
		return (eps*Util.sign(vz)*H-sz) / vz;
	}

	public static double time_coalt(double sz, double vz) {
		if (sz == 0) return 0;
		if (vz == 0) return Double.NaN;
		return -sz/vz;
	}

	/* Vertical miss distance within lookahead time */
	public static double vmd(double sz, double vz, double T) {
		if (sz*vz < 0) {
			// aircraft are vertically converging
			if (time_coalt(sz,vz) <= T) {
				return 0;
			} else {
				return Math.abs(sz+T*vz);
			}
		}
		return Math.abs(sz);
	}

	private static Vertical vs_at(double sz,double t, 
			int eps,double H) {
		if (t == 0) 
			return NoVerticalSolution;
		return new Vertical((eps*H-sz) / t);
	}

	private static Vertical vs_only(Vect3 s,Vect2 v,double t,
			int eps,double D, double H) {

		Vect2 s2 = s.vect2();

		if (eps*s.z < H && s2.sqv() > Util.sq(D) && Horizontal.Delta(s2,v,D) > 0)
			return vs_at(s.z,Horizontal.Theta_D(s2,v,Entry,D),eps,H);      
		else if (eps*s.z >= H && t > 0) 
			return vs_at(s.z,t,eps,H);
		return NoVerticalSolution;
	}

	/** Solve the following equation on vz:
	 *   sz+t*vz = eps*H,
	 *   
	 * where t = Theta_D(s,v,eps).
	 * eps determines the bottom, i.e.,-1, or top, i.e., 1, circle.
	 */
	public static Vertical vs_circle(Vect3 s, Vect3 vo, Vect3 vi,
			int eps, double D, double H) {
		Vect2 s2 = s.vect2();
		Vect2 vo2 = vo.vect2();
		Vect2 vi2 = vi.vect2();
		Vect2 v2 = vo2.Sub(vi2);

		if (vo2.almostEquals(vi2) && eps == Util.sign(s.z)) 
			return new Vertical(vi.z);
		if (Horizontal.Delta(s2,v2,D) > 0) 
			return vs_only(s,v2,Horizontal.Theta_D(s2,v2,Exit,D),eps,D,H).add_this(vi.z);
		return NoVerticalSolution;
	}

	/** */
	public static Vertical vs_circle_at(double sz, double viz,
			double t, int eps, int dir, double H) {
		if (t > 0 && dir*eps*sz <= dir*H) 
			return vs_at(sz,t,eps,H).add_this(viz);
		return NoVerticalSolution;
	}

	/** */
	public static Vertical vs_los_recovery(Vect3 s, Vect3 vo, Vect3 vi, double H, double t, int epsv) {
		if (t <= 0)
			return NoVerticalSolution;
		Vect3 v = vo.Sub(vi);
		double nvz = (epsv*H - s.z) / t;
		if (s.z*v.z >= 0 && Math.abs(v.z) >= Math.abs(nvz))
			return new Vertical(vo.z);
		else
			return new Vertical(nvz+vi.z);
	}

	public static boolean hasEpsilonCriticalPointVertical(Vect3 s, Velocity vo, Velocity vi, Double D) {
		Vect2 s2 = s.vect2();
		Vect3 v = vo.Sub(vi);
		Vect2 v2 = v.vect2();
		double delta = Horizontal.Delta(s2, v2, D); 
		double theta = 0.0;
		if (delta >= 0) 
			theta = Horizontal.Theta_D(s2, v2, -1, D);   // entry time
		if (delta >= 0 && theta > 0) return true;
		else return false;
	}

	// returns vertical speed that is a critical point
	// if hasEpsilonCriticalPointVertical is false  the return is meaningless
	public static double epsilonCriticalPointVertical(Vect3 s, Velocity vo, Velocity vi, Double D) {
		Vect2 s2 = s.vect2();
		Vect3 v = vo.Sub(vi);
		Vect2 v2 = v.vect2();
		double delta = Horizontal.Delta(s2, v2, D); 
		double theta = 0.0;
		if (delta >= 0) {
			theta = Horizontal.Theta_D(s2, v2, -1, D);   // entry time
			return -s.z / theta;
		} else
			return 0;
	}

	public String toString() {
		return "[ undef = "+undef+" z = "+f.FmPrecision(z)+"]";
	}


}
