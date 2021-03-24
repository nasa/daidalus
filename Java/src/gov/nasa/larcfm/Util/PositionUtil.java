/*
 * Copyright (c) 2016-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

/**
 * GreatCircle and VectFuns functions lifted to Positions
 */
public final class PositionUtil {

	// 2D collinear
	public static boolean collinear(Position p0, Position p1, Position p2) {
		if (p0.isLatLon()) {
			return GreatCircle.collinear(p0.lla(), p1.lla(), p2.lla());
		} else {
			return VectFuns.collinear(p0.vect2(), p1.vect2(), p2.vect2());
		}
	}

	// f should be between 0 and 1 to interpolate
	public static Position interpolate(Position v1, Position v2, double f) {
		if (v1.isLatLon()) {
			return Position.make(GreatCircle.interpolate(v1.lla(), v2.lla(), f));
		} else {
			return Position.make(VectFuns.interpolate(v1.vect3(), v2.vect3(), f));
		}
	}
	

	/**
	 * Return angle between P1-P2 and P2-P3
	 * @param p1 point 1
	 * @param p2 point 2 (intersection of two lines)
	 * @param p3 point 3
	 * @return angle between two lines
	 */
	public static double angle_between(Position p1, Position p2, Position p3) {
		if (p1.isLatLon()) {
			//double ang1 = GreatCircle.initial_course(p2.lla(),p1.lla());
			//double ang2 = GreatCircle.initial_course(p2.lla(),p3.lla());
			//return Util.turnDelta(ang1,ang2);			
			return GreatCircle.angle_between(p1.lla(), p2.lla(), p3.lla());
		} else {
			return VectFuns.angle_between(p1.vect2(), p2.vect2(), p3.vect2());
		}
	}
	

	/**
	 * Calculate the 2D intersection of two lines
	 * @param so current position
	 * @param vo current velocity
	 * @param si a point on the line to connect to
	 * @param vi velocity (direction) of the line to connect to
	 * @return position of intersection and relative time of intersection.  
	 * The time of intersection will be negative if it is "behind" so, and positive if it is "ahead" of so, as defined by vo.
	 * If the two lines are parallel or collinear, this returns an INVALID position  
	 */
	public static Pair<Position,Double> intersection(Position so, Velocity vo, Position si, Velocity vi) {
		if (so.isLatLon()) {
			Pair<LatLonAlt, Double> p = GreatCircle.intersection(so.lla(), vo, si.lla(), vi);
			return Pair.make(Position.make(p.first), p.second);
		} else {
			Pair<Vect3, Double> p = VectFuns.intersection(so.vect3(), vo, si.vect3(), vi);
			return Pair.make(Position.make(p.first), p.second);
		}
	}


	/**
	 * Calculate the 2D intersection of two lines
	 * @param so1 current position
	 * @param so2 a future position
	 * @param dto the time needed to reach so2 from so1
	 * @param si1 a point on the line to connect to
	 * @param si2 a future point (relative to si1) on the line to connect to
	 * @return position of intersection and relative time of intersection.  
	 * The time of intersection will be negative if it is "behind" so, and positive if it is "ahead" of so, as defined by vo.
	 * The returned position's altitude will not have any particular significance.
	 * The returned position will be INVALID if the two segments are on the same great circle.
	 * The returned time will be NaN if the segments are parallel (and Euclidean)
	 */
	public static Pair<Position,Double> intersection(Position so1, Position so2, double dto, Position si1, Position si2) {
		if (so1.isLatLon()) {
			Pair<LatLonAlt, Double> p = GreatCircle.intersectionAvgAlt(so1.lla(), so2.lla(), dto, si1.lla(), si2.lla());
			return Pair.make(Position.make(p.first), p.second);
		} else {
			Pair<Vect3, Double> p = VectFuns.intersectionAvgZ(so1.vect3(), so2.vect3(), dto, si1.vect3(), si2.vect3());
			return Pair.make(Position.make(p.first), p.second);
		}
	}
	
	/**
	 * Return the (2D) intersection point between two segments, or INVALID, if none exists
	 * If one segment overlaps another, return INVALID
	 * @param a1
	 * @param a2
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static Position intersectionSegments(Position a1, Position a2, Position b1, Position b2) {
		if (a1.isLatLon()) {
			LatLonAlt i = GreatCircle.intersectSegments(a1.lla(), a2.lla(), b1.lla(), b2.lla());
			return Position.make(i);
		} else {
			Vect2 i = VectFuns.intersectSegments(a1.vect2(), a2.vect2(), b1.vect2(), b2.vect2()).first;
			return Position.make(new Vect3(i, (a1.alt()+a2.alt())/2));
		}
	}
	
	
//	/**
//   * EXPERIMENTAL
//	 * Return the (2D) intersection point between two segments, or INVALID, if none exists.  
//	 * If there is an overlap between the segments, also return the other end of the overlap.
//	 * @param a1
//	 * @param a2
//	 * @param b1
//	 * @param b2
//	 * @return
//	 */
//	public static Pair<Position,Position> intersectSegmentsWithOverlap(Position a1, Position a2, Position b1, Position b2) {
//		if (a1.isLatLon()) {
//			Pair<LatLonAlt,LatLonAlt> i = GreatCircle.intersectSegmentsWithOverlap(a1.lla(), a2.lla(), b1.lla(), b2.lla());
//			if (i.second != null) {
//				return Pair.make(new Position(i.first), new Position(i.second));
//			} else {
//				return Pair.make(new Position(i.first), null);
//			}
//		} else {
//			Triple<Vect2,Double,Vect2> i = VectFuns.intersectSegmentsWithOverlap(a1.vect2(), a2.vect2(), b1.vect2(), b2.vect2());
//			if (i.third != null) {
//				return Pair.make(new Position(new Vect3(i.first, (a1.alt()+a2.alt())/2)), new Position(new Vect3(i.third, (a1.alt()+a2.alt())/2)));
//			} else {
//				return Pair.make(new Position(new Vect3(i.first, (a1.alt()+a2.alt())/2)), null);
//			}
//		}
//	}
	
	public static Position closestPoint(Position a, Position b, Position x) {
		if (a.isLatLon()) {
			return Position.make(GreatCircle.closest_point_circle(a.lla(), b.lla(), x.lla()));
		} else {
			return Position.make(VectFuns.closestPoint(a.vect3(), b.vect3(), x.vect3()));
		}
	}
	
	
	public static Position closestPointOnSegment(Position a, Position b, Position x) {
		if (a.isLatLon()) {
			return Position.make(GreatCircle.closest_point_segment(a.lla(), b.lla(), x.lla()));
		} else {
			return Position.make(VectFuns.closestPointOnSegment(a.vect3(), b.vect3(), x.vect3()));
		}
	}

	public static Position behind(Position a, Position b, Position x) {
		if (a.isLatLon()) {
			return Position.make(GreatCircle.closest_point_segment(a.lla(), b.lla(), x.lla()));
		} else {
			return Position.make(VectFuns.closestPointOnSegment(a.vect3(), b.vect3(), x.vect3()));
		}
	}


	public static boolean behind(Position p1, Position p2, Velocity vo) {
		if (p1.isLatLon()) {
			return GreatCircle.behind(p1.lla(), p2.lla(), vo);
		} else {
			return VectFuns.behind(p1.vect2(), p2.vect2(), vo.vect2());
		}
	}
	
	public static int passingDirection(Position so, Velocity vo, Position si, Velocity vi) {
		if (so.isLatLon()) {
			return GreatCircle.passingDirection(so.lla(), vo, si.lla(), vi);
		} else {
			return VectFuns.passingDirection(so.vect3(), vo, si.vect3(), vi);
		}
		
	}
	


	public static int dirForBehind(Position so, Velocity vo, Position si, Velocity vi) {
		if (so.isLatLon()) {
			return GreatCircle.dirForBehind(so.lla(), vo, si.lla(), vi);
		} else {
			return VectFuns.dirForBehind(so.vect3(),vo,si.vect3(),vi);
		}
	}
	

}
