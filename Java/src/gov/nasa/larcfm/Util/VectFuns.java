/*
 * VectFuns.java 
 * 
 * Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 
 * 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** 
 * A collection of functions on vectors 
 *
 */
public final class VectFuns {

	// prohibit construction of this object
	private VectFuns() {
	}
	
	/**
	 * Returns true if aircraft are in loss of separation at time 0.
	 * 
	 * @param so the relative position of the ownship aircraft
	 * @param si the relative position of the traffic aircraft
	 * @param D the minimum horizontal distance
	 * @param H the minimum vertical distance
	 * 
	 * @return true, if aircraft are in loss of separation
	 */
	public static boolean LoS(Vect3 so, Vect3 si, double D, double H) {
		Vect3 s = so.Sub(si);
		return s.x*s.x + s.y*s.y < D*D && Math.abs(s.z) < H;
	}

	/**
	 * Returns true if si is on the right side of the line (so,vo)
	 * @param so ownship position
	 * @param vo ownship velocity
	 * @param si traffic aircraft position
	 * @return true if is to the right of line (so,vo)
	 */
	public static boolean rightOfLine(Vect2 so, Vect2 vo, Vect2 si) {
		return si.Sub(so).dot(vo.PerpR()) >= 0;
	}

	/**
	 * Return if point p is to the right or left of the line from A to B
	 * @param a point A
	 * @param b point B
	 * @param p point P
	 * @return 1 if to the right or collinear, -1 if to the left.
	 */	
	public static int rightOfLinePoints(Vect2 a, Vect2 b, Vect2 p) {
		Vect2 AB = b.Sub(a);
		Vect2 AP = p.Sub(a);
		//The calculation below is the 2-D cross product.
		return Util.sign(AP.x()*AB.y() - AP.y()*AB.x());
	}


	public static boolean collinear(Vect2 p0, Vect2 p1, Vect2 p2) {
		// area of triangle = 0? (same as det of sides = 0?)
		return  p1.Sub(p0).det(p2.Sub(p0)) == 0;
	}

	public static boolean collinear(Vect3 p0, Vect3 p1, Vect3 p2) {
		Vect3 v01 = p0.Sub(p1);
		Vect3 v02 = p1.Sub(p2);
		return v01.parallel(v02);
	}

	public static Vect2 midPoint(Vect2 p1, Vect2 p2) {
		return p1.Add(p2).Scal(0.5);
	}

	public static Vect3 midPoint(Vect3 v1, Vect3 v2) {
		return v1.Add(v2).Scal(0.5);
	}

	/**
	 * Interpolate between two vectors.  
	 * @param v1 first position
	 * @param v2 second position
	 * @param f a fraction.  Should be between 0 and 1 to interpolate. If 0, then v1 is returned, if 1 then v2 is returned.
	 * @return interpolated vector
	 */
	public static Vect3 interpolate(Vect3 v1, Vect3 v2, double f) {
		Vect3 dv = v2.Sub(v1);
		return v1.Add(dv.Scal(f));
	}

	/**
	 * Interpolate between two velocity vectors.  
	 * @param v1 first position
	 * @param v2 second position
	 * @param f a fraction.  Should be between 0 and 1 to interpolate. If 0, then v1 is returned, if 1 then v2 is returned.
	 * @return interpolated vector
	 */
	public static Velocity interpolateVelocity(Velocity v1, Velocity v2, double f) {
		double newtrk = v1.trk() + f*(v2.trk() - v1.trk());
		double newgs = v1.gs() + f*(v2.gs() - v1.gs());
		double newvs = v1.vs() + f*(v2.vs() - v1.vs());
		return Velocity.mkTrkGsVs(newtrk,newgs,newvs);
	}


	// This appears to use the right-hand rule to determine it returns the inside or outside angle
	public static double angle_between(Vect2 v1, Vect2 v2) {
		Vect2 VV1 = v1.Scal(1.0/v1.norm());
		Vect2 VV2 = v2.Scal(1.0/v2.norm());
		return Util.atan2_safe(VV2.y,VV2.x)-Util.atan2_safe(VV1.y,VV1.x);
	}

	/**
	 * Return angle between P1-P2 and P2-P3
	 * @param a point 1
	 * @param b point 2 (intersection of two lines)
	 * @param c point 3
	 * @return angle between two lines
	 */	
	public static double angle_between(Vect2 a, Vect2 b, Vect2 c) {
		Vect2 A = b.Sub(a);
		Vect2 B = b.Sub(c);
		double d = A.norm()*B.norm();
		if (d == 0.0) return Math.PI;
		return Util.acos_safe(A.dot(B)/d);
	}


	/**
	 * determines if divergent and relative speed is greater than a specified minimum relative speed
	 * 
	 * @param s  relative position of ownship with respect to traffic
	 * @param vo initial velocity of ownship
	 * @param vi initial velocity of traffic
	 * @param minRelSpeed  the desired minimum relative speed
	 * @return   true iff divergent and relative speed is greater than a specified minimum relative speed
	 */
	public static boolean  divergentHorizGt(Vect2 s, Vect2 vo, Vect2 vi, double minRelSpeed) {
		Vect2 v = (vo.Sub(vi));
		return s.dot(v) > 0 && v.norm() > minRelSpeed;
	}

	public static boolean  divergentHorizGt(Vect3 s, Vect3 vo, Vect3 vi, double minRelSpeed) {
		return divergentHorizGt(s.vect2(), vo.vect2(), vi.vect2(), minRelSpeed);
	}

	/**
	 * Return if two aircraft in the given state are divergent in the horizontal plane
	 * 
	 * @param so ownship position
	 * @param vo ownship velocity
	 * @param si intruder position
	 * @param vi intruder velocity
	 * @return true, if divergent
	 */
	public static boolean divergent(Vect2 so, Vect2 vo, Vect2 si, Vect2 vi) {
		return so.Sub(si).dot(vo.Sub(vi)) > 0;
	}


	/**
	 * Return if two aircraft in the given state are divergent in a 3D sense
	 * 
	 * @param so ownship position
	 * @param vo ownship velocity
	 * @param si intruder position
	 * @param vi intruder velocity
	 * @return true, if divergent
	 */
	public static boolean divergent(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return so.Sub(si).dot(vo.Sub(vi)) > 0;
	}

	/**
	 * Return the horizontal rate of closure of two aircraft in the given state
	 * @param so position of first aircraft
	 * @param vo velocity of first aircraft
	 * @param si position of second aircraft
	 * @param vi velocity of second aircraft
	 * @return rate of closure
	 */
	public static double rateOfClosureHorizontal(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return -so.Sub(si).vect2().Hat().dot(vo.Sub(vi).vect2());
	}

	/**
	 * Return the vertical rate of closure of two aircraft in the given state
	 * @param so position of first aircraft
	 * @param vo velocity of first aircraft
	 * @param si position of second aircraft
	 * @param vi velocity of second aircraft
	 * @return rate of closure
	 */
	public static double rateOfClosureVertical(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return Util.sign(si.z-so.z)*(vo.z-vi.z);
	}


	/** time of closest approach, if parallel return Double.MAX_VALUE
	 * @param s    relative position of aircraft
	 * @param vo   velocity of ownship
	 * @param vi   velocity of traffic
	 * @return     time of closest approach (can be negative)
	 */
	public static double tau(Vect3 s, Vect3 vo, Vect3 vi) {
		double rtn;
		Vect3 v = vo.Sub(vi);
		double nv = v.norm();
		if (Util.almost_equals(nv,0.0)) {
			rtn = Double.MAX_VALUE;                    // pseudo infinity
		} else
			rtn = -s.dot(v)/(nv*nv);
		return rtn;
	}// tau

	
	/**
	 * Tau calculation, restricted to the horizontal.
	 * @param s  position
	 * @param vo velocity of first aircraft
	 * @param vi velocity of second aircraft
	 * @return tau
	 */
	public static double tau2D(Vect2 s, Vect2 vo, Vect2 vi) {
		double rtn;
		Vect2 v = vo.Sub(vi);
		double nv = v.norm();
		if (Util.almost_equals(nv,0.0)) {
			rtn = Double.MAX_VALUE;                    // pseudo infinity
		} else
			rtn = -s.dot(v)/(nv*nv);
		return rtn;
	}
	
	
	/**  distance at time of closest approach
	 * @param s    relative position of aircraft
	 * @param vo   velocity of ownship
	 * @param vi   velocity of traffic
	 * @param futureOnly  if true then in divergent cases use distance now  
	 * @return     distance at time of closest approach
	 */
	public static double distAtTau(Vect3 s, Vect3 vo, Vect3 vi, boolean futureOnly) {   
		double tau = tau(s,vo,vi);
		if (tau < 0 && futureOnly) 
			return s.norm();                 // return distance now
		else {
			Vect3 v = vo.Sub(vi);
			Vect3 sAtTau = s.Add(v.Scal(tau));
			return sAtTau.norm();
		}
	}


	/** Q function (relative tangent point) 
	 * @param s point outside circle with origin center
	 * @param D radius of circle
	 * @param eps direction (+/-1)
	 * @return point on origin-centered circle that is tangent and passes through s, or INVALID if s is within the circle
	 */
	public static Vect2 Q(Vect2 s, double D, int eps) {
		double sq_s = s.sqv();
		double sq_D = Util.sq(D);
		double delta = sq_s-sq_D;
		if (delta >= 0) { 
			double alpha = sq_D/sq_s;
			double beta  = D*Util.sqrt_safe(delta)/sq_s;
			return new Vect2(alpha*s.x+eps*beta*s.y,
					alpha*s.y-eps*beta*s.x);   
		}
		return Vect2.INVALID;
	}


	/**
	 * Distance between two points.  Effectively soA.Sub(soB).norm();
	 * 
	 * @param soA
	 * @param soB
	 * @return distance
	 */
	public static double distance(Vect2 soA, Vect2 soB) {
		return Util.sqrt_safe((soA.x-soB.x)*(soA.x-soB.x)+(soA.y-soB.y)*(soA.y-soB.y));
	}
	
   /**
    * Calculate the horizontal distance between two points.
    * 
    * @param soA   point A 
    * @param soB   point B 
    * @return horizontal distance between points.
    */
	public static double distanceH(Vect3 soA, Vect3 soB) {
		return soA.Sub(soB).vect2().norm();
	}

	
	/**
	 * Computes 2D intersection point of two lines, but also finds z component (projected by time from line o)
	 * This z-component is constrained to be within the z components of the defining points.
	 * @param so3 starting point of line o
	 * @param vo3 direction vector for line o
	 * @param si3 starting point of line i
	 * @param vi3 direction vector of line i
	 * @return Pair (2-dimensional point of intersection, relative time of intersection, relative to the so3)
	 * Note the intersection may be in the past (i.e. negative time)
	 * If the lines are 2-D parallel (including collinear) this returns the pair (0,NaN).
	 */
	public static Pair<Vect3,Double> intersection(Vect3 so3, Velocity vo3, Vect3 si3, Velocity vi3) {
		Vect2 so = so3.vect2();
		Vect2 vo = vo3.vect2();
		Vect2 si = si3.vect2();
		Vect2 vi = vi3.vect2();
		Vect2 ds = si.Sub(so);
		if (vo.det(vi) == 0) {
			return new Pair<>(Vect3.ZERO, Double.NaN); //Don't change this to (so,0.0) 
		}
		double tt = ds.det(vi)/vo.det(vi);
		Vect3 intersec = so3.AddScal(tt,vo3); 
		double nZ = intersec.z();
		double maxZ = Util.max(so3.z,si3.z);
		double minZ = Util.min(so3.z,si3.z);			
		if (nZ > maxZ) nZ = maxZ;
		if (nZ < minZ) nZ = minZ;	
		return new Pair<>(intersec.mkZ(nZ),tt);
	}

	/**
	 * Intersection of 2 (2D) lines
	 * @param so current position on line 1 
	 * @param vo velocity of so along line 1
	 * @param si current position on line 2
	 * @param vi velocity of si on line 2
	 * @return position and time along line 1, or (0,NaN) if there is no intersection (or the lines are the same).
	 */
	public static Pair<Vect2,Double> intersection2D(Vect2 so, Vect2 vo, Vect2 si, Vect2 vi) {
		Vect2 ds = si.Sub(so);
		double det = vo.det(vi);
		if (det == 0) {
			return new Pair<>(Vect2.ZERO, Double.NaN);
		}
		double tt = ds.det(vi)/det;
		Vect2 intersec = so.AddScal(tt,vo);
		return new Pair<>(intersec,tt);
	}

	/**   NOT FULLY DEBUGGED !!
	 * Return the distance at which si, traveling in direction vi, will intersect with segment ab (inclusive)
	 * @param si  position of vehicle
	 * @param vi  velocity of vehicle
	 * @param a   an end point of segment
	 * @param b   an end point of segment
	 * @return distance of intersection, or negative if no intersection
	 */
	public static double intersectSegment(Vect2 si, Vect2 vi, Vect2 a, Vect2 b) {
		double theta = vi.trk();
		// rotate segment so vi is "straight up" and si is at the origin
		Vect2 A = rotate(a.Sub(si), -theta);
		Vect2 B = rotate(b.Sub(si), -theta);
		if ( ! ((A.x >= 0 && B.x <= 0) || (A.x <= 0 && B.x >= 0))) {
			return -1.0;
		}
			
		if (A.x == B.x) {
			if (A.y >= 0 || B.y >= 0) {
				return Util.max(0.0, Util.min(A.y, B.y)); // first point of intersection
			} 
		} else if (A.y == B.y) {
			if (A.y >= 0) {
				return A.y;
			}
		} else if (A.y >= si.y || B.y >= si.y) {
			double m = (B.x-A.x)/(B.y-A.y);
			double y0 = A.y-m*A.x;
			if (y0 >= 0) {
				return y0;
			}
		}
		return -1.0;
	}

	/**
	 * Computes 2D intersection point of two infinite lines
	 * @param so3 starting point of line 1
	 * @param vo3 direction vector for line 1
	 * @param si3 starting point of line 2
	 * @param vi3 direction vector of line 2
	 * @return time the OWNSHIP (so3) will reach the point.  Note that the intruder (si3) may have already passed this point.
	 * If the lines are parallel, this returns NaN.
	 */
	public static double timeOfIntersection(Vect3 so3, Velocity vo3, Vect3 si3, Velocity vi3) {
		Vect2 so = so3.vect2();
		Vect2 vo = vo3.vect2();
		Vect2 si = si3.vect2();
		Vect2 vi = vi3.vect2();
		Vect2 ds = si.Sub(so);
		if (vo.det(vi) == 0) {
			//f.pln(" $$$ intersection: lines are parallel");
			return Double.NaN;
		}
		return ds.det(vi)/vo.det(vi);
	}



	/**
	 * Computes 2D intersection point of two lines, but also finds z component as average of the closest end point of each line)
	 * This z-component is constrained to be within the z components of the defining points.
	 * @param so1 starting point of line o
	 * @param so2 ending point of line o
	 * @param dto delta time
	 * @param si1 starting point of line i
	 * @param si2 ending point of line i
	 * @return Pair (2-dimensional point of intersection, relative time of intersection, relative to the so1)
	 * This includes the average altitude between the *endpoints* closest to the point of intersection
	 * Note the intersection may be in the past (i.e. negative time)
	 * If the lines are parallel, this returns the pair (0,NaN).
	 */
	public static Pair<Vect3,Double> intersectionAvgZ(Vect3 so1, Vect3 so2, double dto, Vect3 si1, Vect3 si2) {
		Velocity vo3 = Velocity.genVel(so1, so2, dto); //along line 1
		Velocity vi3 = Velocity.genVel(si1, si2, dto); //along line 2     // its ok to use any time here,  all times are relative to so
		Pair<Vect3,Double> iP = intersection(so1,vo3,si1,vi3); // 2d intersection along line 1 (includes line 1 altitude)
		Vect3 interSec = iP.first;
		double do1 = distanceH(so1,interSec);
		double do2 = distanceH(so2,interSec);
		double alt_o = so1.z();                   // chose z from end point of line 1 closest to interSec
		if (do2 < do1) alt_o = so2.z();
		double di1 = distanceH(si1,interSec);
		double di2 = distanceH(si2,interSec);
		double alt_i = si1.z();                   //  chose z from end point of line 2 closest to interSec
		if (di2 < di1) alt_i = si2.z();
		double nZ = (alt_o + alt_i)/2.0;       
		return new Pair<>(interSec.mkZ(nZ),iP.second); 
	}

	/** 2D intersection of two infinite lines
	 * 
	 * @param so1    first point of line o 
	 * @param so2    second point of line o
	 * @param dto    the delta time between point so and point so2.
	 * @param si1    first point of line i
	 * @param si2    second point of line i 
	 * @return a pair: intersection point and the delta time from point "so" to the intersection, can be negative if intersect
	 *                 point is in the past.  Returns (0,NaN) if there is no intersection (or the lines are the same).
	 */
	public static Pair<Vect2,Double> intersection2D(Vect2 so1, Vect2 so2, double dto, Vect2 si1, Vect2 si2) {
		Vect2 vo = so2.Sub(so1).Scal(1/dto);
		Vect2 vi = si2.Sub(si1).Scal(1/dto);
		return intersection2D(so1,vo,si1,vi);
	}
	
	/** 2D intersection of two segments (finite length) at a single point
	 * This treats overlapping segments as no intersection 
	 * @param so    first point of line o 
	 * @param so2    second point of line o
	 * @param si    first point of line i
	 * @param si2    second point of line i 
	 * @return a pair: intersection point and the fraction of the distance from point "so" to the intersection.
	 *                 Returns (INVALID,fractDist) if there is no intersection (or the lines are the same).
	 */
	public static Pair<Vect2,Double> intersectSegments(Vect2 so, Vect2 so2, Vect2 si, Vect2 si2) {
		//Pair<Vect2,Double> int2D = intersection2D( so,  so2, dto,  si,  si2);
		Vect2 vo = so2.Sub(so);
		Vect2 vi = si2.Sub(si);
		Pair<Vect2,Double> int2D = intersection2D(so,vo,si,vi);
		double fractDist = int2D.second;
		if (int2D.first.isInvalid()) return int2D;
		if (Double.isNaN(fractDist) || fractDist < 0 || fractDist > 1.0) return new Pair<>(Vect2.INVALID, fractDist);			
		Vect2 w = so.Sub(si);
		double D = vo.det(vi);
		double tI = vo.det(w)/D;
		//f.pln(" $$$$ intersectSegments: fractDist = "+fractDist+" tI = "+tI);
		if (tI < 0 || tI > 1) return new Pair<>(Vect2.INVALID, tI);
		//f.pln(" $$$$ intersectSegments: alternate = "+so.Add(u.Scal(sI)));
		return int2D;
	}

	
	/**
	 * Return true if, a,b,c, are collinear (assumed) and b is between a and c (inclusive) 
	 */
	private static boolean collinearBetween(Vect2 a, Vect2 b, Vect2 c) {
		return a.distance(b) <= a.distance(c) && c.distance(b) <= c.distance(a);
	}
	

	/**
	 * returns the perpendicular time and distance between line defined by s,v and point q.
	 * @param  s point on line
	 * @param  v velocity vector of line
	 * @param  q a point not on the line
	 * 
	 * @return time to perpendicular location on line (relative to s) and perpendicular distance
	 */
	public static Pair<Double,Double> distPerp(Vect2 s, Vect2 v, Vect2 q) {
		double tp = q.Sub(s).dot(v)/v.sqv();
		double dist = s.Add(v.Scal(tp)).Sub(q).norm();
		return new Pair<>(tp,dist);
	}

	// horizontal only
	public static Pair<Double,Double> distPerp(Vect3 s, Vect3 v, Vect3 q) {
		return distPerp(s.vect2(),v.vect2(),q.vect2());
	}

	/**
	 * Return the closest point on the line a-b to point so as a 3D norm
	 * EXPERIMENTAL
	 * 
	 * @param a a point to define a line 
	 * @param b another point to define a line
	 * @param so a point
	 * @return the closest point
	 */
	public static Vect3 closestPoint3(Vect3 a, Vect3 b, Vect3 so) {
		// translate a to origin, then project so onto the line defined by ab, then translate back to a
		// this is closest point in 3-space
		// note there may be precision errors if dealing with large numbers!
		Vect3 ab = b.Sub(a);
		return ab.Scal(so.Sub(a).dot(ab)/ab.dot(ab)).Add(a);
	}

	/**
	 * Return the closest (preference to horizontal) point along line a-b to point so
	 * EXPERIMENTAL
	 * 
	 * @param a a point to define a line 
	 * @param b another point to define a line
	 * @param so a point
	 * @return the closest point
	 */
	public static Vect3 closestPoint(Vect3 a, Vect3 b, Vect3 so) {
		if (a.almostEquals(b)) return Vect3.INVALID;
		Vect2 c = closestPoint(a.vect2(), b.vect2(), so.vect2());
		Vect3 v = b.Sub(a);
		double d1 = v.vect2().norm();
		double d2 = c.Sub(a.vect2()).norm();
		double d3 = c.Sub(b.vect2()).norm();
		double f = d2/d1;
		if (d3 > d1 && d3 > d2) { // negative direction
			f = -f; 
		}
		return a.AddScal(f, v);
	}

	/**
	 * Return the closest (horizontal) point along line a-b to point so
	 * EXPERIMENTAL
	 * 
     * @param a a point to define a line 
	 * @param b another point to define a line
	 * @param so a point
	 * @return the closest point
	 */
	public static Vect2 closestPoint(Vect2 a, Vect2 b, Vect2 so) {
		// translate a to origin, then project so onto the line defined by ab, then translate back to a
		Vect2 ab = b.Sub(a);
		return ab.Scal(so.Sub(a).dot(ab)/ab.dot(ab)).Add(a);
	}

	/**
	 * Return the closest (horizontal) point on segment a-b to point so
	 * EXPERIMENTAL
	 * 
     * @param a a point to define a line 
	 * @param b another point to define a line
	 * @param so a point
	 * @return the closest point
	 */
	public static Vect3 closestPointOnSegment(Vect3 a, Vect3 b, Vect3 so) {
		Vect3 i = closestPoint(a,b,so);
		double d1 = a.distanceH(b);
		double d2 = a.distanceH(i);
		double d3 = b.distanceH(i);
		if (d2 <= d1 && d3 <= d1) {
			return i;
		} else if (d2 < d3) {
			return a;
		} else {
			return b;
		}
	}

	public static Vect2 closestPointOnSegment(Vect2 a, Vect2 b, Vect2 so) {
		Vect2 i = closestPoint(a,b,so);
		// a, b, i are all on line
		double d1 = a.distance(b);
		double d2 = a.distance(i);
		double d3 = b.distance(i);
		if (d2 <= d1 && d3 <= d1) { // i is between a and b
			return i;
		} else if (d2 < d3) {
			return a;
		} else {
			return b;
		}
	}

	public static Vect3 closestPointOnSegment3(Vect3 a, Vect3 b, Vect3 so) {
		Vect3 i = closestPoint3(a,b,so);
		if (a.almostEquals(b)) return a;
		double d1 = a.Sub(b).norm();
		double d2 = a.Sub(i).norm();
		double d3 = b.Sub(i).norm();
		if (d2 <= d1 && d3 <= d1) {
			return i;
		} else if (d2 < d3) {
			return a;
		} else {
			return b;
		}
	}

	/**
	 * return point and ratio of distance between a and b (0=a, 1=b) 
     * @param a a point to define a line 
	 * @param b another point to define a line
	 * @param so a point
	 * @return point and ratio (0=a, 1=b) 
	 */
	public static Pair<Vect3,Double> closestPointOnSegment3Extended(Vect3 a, Vect3 b, Vect3 so) {
		Vect3 i = closestPoint3(a,b,so);
		if (a.almostEquals(b)) return Pair.make(a,  0.0);
		double d1 = a.Sub(b).norm();
		double d2 = a.Sub(i).norm();
		double d3 = b.Sub(i).norm();
		if (d2 <= d1 && d3 <= d1) {
			double r = d2/d1;
			return Pair.make(i, r);
		} else if (d2 < d3) {
			return Pair.make(a, 0.0);
		} else {
			return Pair.make(b, 1.0);
		}
	}

	public static double distanceToSegment(Vect2 a, Vect2 b, Vect2 so) {
		return so.distance(closestPointOnSegment(a,b,so));
	}



	/**
	 * Rotate p around origin.
	 * @param p point to move
	 * @param angle angle of rotation (positive is clockwise)
	 * @return new position for p
	 */
	public static Vect2 rotate(Vect2 p, double angle) {
		double x1 = Math.cos(angle)*p.x + Math.sin(angle)*p.y;
		double y1 = -Math.sin(angle)*p.x + Math.cos(angle)*p.y;
		return new Vect2(x1,y1);
	}



	/**
	 * Returns true if x is "behind" so , that is, x is within the region behind the perpendicular line to vo through so.  
	 * 
	 * @param x   a position
	 * @param so  aircraft position
	 * @param vo  aircraft velocity
	 * @return true, if x is behind so
	 */
	public static boolean behind(Vect2 x, Vect2 so, Vect2 vo) {
		return Util.turnDelta(vo.trk(), x.Sub(so).trk()) > Math.PI/2.0;
	}

	/**
	 * Returns values indicating whether the ownship state will pass in front of or behind the intruder (from a horizontal perspective)
	 * @param so ownship position
	 * @param vo ownship velocity
	 * @param si intruder position
	 * @param vi intruder velocity
	 * @return 1 if ownship will pass in front (or collide, from a horizontal sense), -1 if ownship will pass behind, 0 if divergent or parallel
	 */
	public static int passingDirection(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		double toi = timeOfIntersection(so,vo,si,vi);
		double tii = timeOfIntersection(si,vi,so,vo); // these values may have opposite sign!
		if (Double.isNaN(toi) || toi < 0 || tii < 0) return 0;
		Vect3 so3 = so.linear(vo, toi);
		Vect3 si3 = si.linear(vi, toi);
		if (behind(so3.vect2(), si3.vect2(), vi.vect2())) return -1;
		return 1;
	}


	public static int dirForBehind(Vect2 so, Vect2 vo, Vect2 si, Vect2 vi) {
		if (divergent(so,vo,si,vi)) return 0;
		return (rightOfLine(si, vi, so) ? -1 : 1);
	}

	public static int dirForBehind(Vect3 so, Velocity vo, Vect3 si, Velocity vi) {
		return dirForBehind(so.vect2(),vo.vect2(),si.vect2(),vi.vect2());	
	}



	/**
	 * Calculate the normal (perpendicular vector) of a plane defined by 3 points.  This is not necessarily a unit vector.
	 * @param a point 1
	 * @param b point 2 
	 * @param c point 3
	 * @return the normal vector
	 */
	public static Vect3 planeNormal(Vect3 a, Vect3 b, Vect3 c) {
		Vect3 ab = a.Sub(b);
		Vect3 ac = a.Sub(c);
		return ab.cross(ac);

	}

	/**
	 * Return the side (+1 right, -1 left) of the ray s(point),v(velocity) that point p is on.
	 * If value is zero, p is on the line (given no floating point issues)
	 * 
	 * @param s position vector
	 * @param v velocity vector
	 * @param p another position vector
	 * @return (+1 right, -1 left)
	 */
	public static int onSide(Vect2 s, Vect2 v, Vect2 p) {
		return Util.sign(p.Sub(s).det(v));
	}


	/**
	 * Return the interior point where the lines between two circles' tangents intersect (from Wolfram Mathworld).
	 * A line tangent to both circles will pass through this point
	 * @param c1 center of circle 1
	 * @param r1 radius of circle 1
	 * @param c2 center of circle 2
	 * @param r2 radius of circle 2
	 * @return interior center of similitude.  This version will test if the circles overlap and return INVALID in that case. 
	 */
	public static Vect2 internalCenterOfSimilitude(Vect2 c1, double r1, Vect2 c2, double r2) {
		if (c1.distance(c2) < r1+r2) return Vect2.INVALID;
		return c1.Scal(r1).Add(c2.Scal(r2)).Scal(1/(r1+r2));
	}

	/**
	 * Return the interior point where the lines between two circles' tangents intersect, if they have different radii (from Wolfram Mathworld)
	 * A line tangent to both circles will pass through this point
	 * @param c1 center of circle 1
	 * @param r1 radius of circle 1
	 * @param c2 center of circle 2
	 * @param r2 radius of circle 2
	 * @return exterior center of similitude.  This version will test if the circles overlap and return INVALID in that case.  It will also return INVALID if they have the same radii. 
	 */
	public static Vect2 externalCenterOfSimilitude(Vect2 c1, double r1, Vect2 c2, double r2) {
		if (Util.almost_equals(r1,  r2) || c1.distance(c2) < r1+r2) return Vect2.INVALID;
		return c2.Scal(r1).Sub(c1.Scal(r2)).Scal(1/(r2-r1));
	}

	/**
	 * Return a list of segments that are tangent to two circles
	 * @param c1 center of circle 1
	 * @param r1 radius of circle 1
	 * @param c2 center of circle 2
	 * @param r2 radius of circle 2
	 * @return endpoints of segments (from c1 to c2) tangent to both circles.  This list will be empty if the circles overlap.
	 */
	public static List<Pair<Vect2,Vect2>> tangentSegments(Vect2 c1, double r1, Vect2 c2, double r2) {
		ArrayList<Pair<Vect2,Vect2>> ret = new ArrayList<>();
		Vect2 icos = internalCenterOfSimilitude(c1,r1,c2,r2);
		if (icos.isInvalid()) return ret; // overlaps, nothing to do
		Vect2 ecos = externalCenterOfSimilitude(c1,r1,c2,r2);
		if (Util.almost_equals(r1, r2)) { // parallel
			Vect2 t1 = c1.AddScal(r1, c1.Sub(c2).PerpL().Hat());
			Vect2 t2 = c2.AddScal(r2, c1.Sub(c2).PerpL().Hat());
			Vect2 t3 = c1.AddScal(r1, c1.Sub(c2).PerpR().Hat());
			Vect2 t4 = c2.AddScal(r2, c1.Sub(c2).PerpR().Hat());
			ret.add(Pair.make(t1, t2));
			ret.add(Pair.make(t3, t4));
		} else { // converging external
			Vect2 s1 = ecos.Sub(c1);
			Vect2 s2 = ecos.Sub(c2);
			Vect2 t1 = Q(s1, r1, +1).Add(c1);
			Vect2 t2 = Q(s1, r1, -1).Add(c1);
			Vect2 t3 = Q(s2, r2, +1).Add(c2);
			Vect2 t4 = Q(s2, r2, -1).Add(c2);
			ret.add(Pair.make(t1, t3));
			ret.add(Pair.make(t2, t4));
		}
		// internal
		Vect2 s1 = icos.Sub(c1);
		Vect2 s2 = icos.Sub(c2);
		Vect2 t1 = Q(s1, r1, +1).Add(c1);
		Vect2 t2 = Q(s1, r1, -1).Add(c1);
		Vect2 t3 = Q(s2, r2, +1).Add(c2);
		Vect2 t4 = Q(s2, r2, -1).Add(c2);
		ret.add(Pair.make(t1, t3));
		ret.add(Pair.make(t2, t4));
		return ret;
	}

	/** 
	 * This parses a space or comma-separated string as a Vect3 (an inverse to the toString 
	 * method).  If three bare values are present, then it is interpreted as the default units for 
	 * a Vect3: [NM,NM,ft].  If there are 3 value/unit pairs then each values is interpreted with regard 
	 * to the appropriate unit.  If the string cannot be parsed, an INVALID Vect3 is
	 * returned. 
	 * 
	 * @param str string to parse
	 * @return point
	 */
	public static Vect3 parse(String str) {
		String[] fields = str.split(Constants.wsPatternParens);
		if (fields[0].equals("")) {
			fields = Arrays.copyOfRange(fields,1,fields.length);
		}
		try {
			if (fields.length == 3) {
				return Vect3.make(
						Double.parseDouble(fields[0]),
						Double.parseDouble(fields[1]),
						Double.parseDouble(fields[2]));
			} else if (fields.length == 6) {
				return Vect3.makeXYZ(
						Double.parseDouble(fields[0]), Units.clean(fields[1]),
						Double.parseDouble(fields[2]), Units.clean(fields[3]),
						Double.parseDouble(fields[4]), Units.clean(fields[5]));
			}
		} catch (Exception e) {
			// ignore exceptions
		}
		return Vect3.INVALID;	
	}


}
