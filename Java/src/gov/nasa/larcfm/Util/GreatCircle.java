/* 
 *  Author:  
 *         Jeff Maddalon             NASA Langley Research Center
 *
 * 
 * Copyright (c) 2011-2021 United States Government as represented by 
 * the National Aeronautics and Space Administration.  No copyright 
 * is claimed in the United States under Title 17, U.S.Code. All Other 
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

/**
 * <p>This class contains common formulas used for modeling a spherical Earth, 
 * in particular, Great Circle calculations. Many of the formulas are based on the
 * Aviation Formulary (v1.44) by Ed Williams.
 * </p>
 * 
 * Notes:
 * <ul>
 * <li>The Earth does not rotate. This may change.
 * <li>Positive latitude is north and positive longitude is east.
 * <li>All course angles (i.e., desired chord angles) are in radians clockwise
 * from true north.
 * <li>For any of these calculations that rely on a radius for the earth, the
 * value used is <code>GreatCircle.spherical_earth_radius</code>.
 * <li>Great circles cannot be defined by antipodal points. (e.g., the north pole and the 
 * south pole)
 * </ul>
 */
public final class GreatCircle {

	private static final double PI = Math.PI;

	private static final double EPS = 1.0e-15; // small number, about machine precision
	public static final double MIN_DT = 1E-5;

	private GreatCircle() { } // never construct one of these

	/**
	 * Convert the distance (in internal length units) across the <b>surface</b> of the
	 * (spherical) Earth into an angle.
	 */
	private static double angle_from_distance(double distance) {
		return Units.to(Units.NM, distance) * PI / (180.0 * 60.0);
	}

	/**
	 * The radius of a spherical "Earth" assuming 1 nautical mile is 1852 meters
	 * (as defined by various international committees) and that one nautical
	 * mile is equal to one minute of arc (traditional definition of a nautical
	 * mile) on the Earth's surface. This value lies between the major and minor 
	 * axis as defined by the reference ellipsoid by the 1984 World Geodetic
	 * Service (WGS84).
	 */
	public static final double spherical_earth_radius = Units.from(Units.m,
			1.0 / angle_from_distance(1.0));

	/**
	 * Convert the distance (in internal length units) across a sphere at a
	 * height of h (in internal length units) above surface of the (spherical)
	 * Earth into an angle.
	 * 
	 * @param distance distance [m]
	 * @param h height above surface of spherical Earth
	 * @return angular distance [radian]
	 */
	public static double angle_from_distance(double distance, double h) {
		return angle_from_distance(distance * spherical_earth_radius
				/ (spherical_earth_radius + h));
	}

	/**
	 * Convert the given angle into a distance across a (spherical) Earth at
	 * height above the surface of h.
	 * 
	 * @param angle angular distance [radian]
	 * @param h height above surface of spherical Earth
	 * @return linear distance [m]
	 */
	public static double distance_from_angle(double angle, double h) {
		return (spherical_earth_radius + h) * angle;
	}

	/**
	 * Determines if two points are close to each other, see
	 * Constants.get_horizontal_accuracy().
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return true, if almost equals
	 */
	public static boolean almost_equals(double lat1, double lon1, double lat2,
			double lon2) {
		return Constants.almost_equals_radian(angular_distance(lat1, lon1,
				lat2, lon2));
	}

	/**
	 * Determines if two points are close to each other, where 'close'
	 * is defined by the distance value given in meters.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @param epsilon maximum difference in meters
	 * @return true, if almost equals
	 */
	public static boolean almost_equals(double lat1, double lon1, double lat2,
			double lon2, double epsilon) {
		return Util.within_epsilon(distance(lat1, lon1, lat2, lon2), epsilon);
	}
	
	/** Are these two LatLonAlt almost equal horizontally? 
	 * 
	 * @param b LatLonAlt object
	 * @param a LatLonAlt object
	 * @param horizEps horizontal epsilon in meters
	 * @return true if the two are almost equals horizontally
	 */
	public static boolean almostEquals2D(LatLonAlt b, LatLonAlt a, double horizEps) {
		return almost_equals(b.lat(), b.lon(), a.lat(), a.lon(), horizEps);
	}
	
	/** Are these two LatLonAlt almost equal, where 'almost' is defined by the given distances [m]
	 * 
	 * @param b LatLonAlt object
	 * @param a LatLonAlt object
	 * @param horizEps allowed difference in horizontal dimension
	 * @param vertEps allowed difference in vertical dimension
	 * @return true if the two are almost equals
	 */
	public static boolean almostEquals(LatLonAlt b, LatLonAlt a, double horizEps, double vertEps) {
		return almost_equals(b.lat(), b.lon(), a.lat(), a.lon(), horizEps)
				&& Util.within_epsilon(b.alt(), a.alt(), vertEps);
	}
	
	/** Are these two LatLonAlt almost equal? 
	 * 
	 * @param b LatLonAlt object
	 * @param a LatLonAlt object
	 * @return true if the two are almost equals
	 */
	public static boolean almostEquals(LatLonAlt b, LatLonAlt a) {
		return almost_equals(b.lat(), b.lon(), a.lat(), a.lon())
				&& Constants.almost_equals_alt(b.alt(), a.alt());
	}



	/**
	 * Compute the great circle distance in radians between the two points. The
	 * calculation applies to any sphere, not just a spherical Earth. The
	 * current implementation uses the haversine formula.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return angular distance
	 */
	public static double angular_distance(double lat1, double lon1,
			double lat2, double lon2) {
		/*
		 * This function uses the haversine formula. A "versed sine" is a sine that is
		 * always positive. A haversine is a "half versed sine."  Haversines were developed
		 * hundreds of years ago for navigators. A navigator with a haversine table 
		 * and its inverse (and standard trig and log tables) could compute the 
		 * distance between two lat/lon points without squaring or square roots.
		 * 
		 * Clearly with modern computers, we do not need haversines for this
		 * reason. Instead, the standard distance formula in spherical
		 * trigonometry is the "law of cosines." This formula has some quirks
		 * for Earth navigation. Specifically, if the two points are close to
		 * each other (within a nautical mile), then this formula involves
		 * taking an arccosine of a number very close to one, which will lead to
		 * inaccurate results. Haversines, since they use sines, do not suffer
		 * from this problem.
		 */

		return Util.asin_safe(Util.sqrt_safe(Util.sq(Math.sin((lat1 - lat2) / 2))
				+ Math.cos(lat1)
				* Math.cos(lat2)
				* Util.sq(Math.sin((lon1-lon2) / 2)))) * 2.0;
	}

	/**
	 * Compute the great circle distance in radians between the two points. The
	 * calculation applies to any sphere, not just a spherical Earth. This implementation
	 * is based on the law of cosines for spherical trigonometry. This version should be both
	 * less accurate when finding small distances and it should be faster, than
	 * the version in angular_distance().  It is less
	 * accurate for small distances; however, it is not faster, in fact, 
	 * it takes about twice the time.  It seems that sin's are 
	 * faster than cos's?  Investigations are continuing.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return angular distance
	 */
	public static double angular_distance_alt(double lat1, double lon1,
			double lat2, double lon2) {
		return Util.acos_safe(Math.cos(lat1 - lat2) +
				(Math.cos(lon1 - lon2) - 1.0) *
				Math.cos(lat2) * Math.cos(lat1));
	}

	/**
	 * Compute the great circle distance in radians between the two points. The
	 * calculation applies to any sphere, not just a spherical Earth. The
	 * current implementation uses the haversine formula.
	 * 
	 * @param p1 one point
	 * @param p2 another point
	 * @return angular distance
	 */
	public static double angular_distance(LatLonAlt p1, LatLonAlt p2) {
		return angular_distance(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}

	/**
	 * Compute the great circle distance between the two given points. The
	 * calculation assumes the Earth is a sphere
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return distance in meters
	 */
	public static double distance(double lat1, double lon1, double lat2,
			double lon2) {
		return distance_from_angle(angular_distance(lat1, lon1, lat2, lon2),
				0.0);
	}

	/**
	 * Compute the great circle distance between the two given points. The
	 * calculation assumes the Earth is a sphere. This ignores the altitudes.
	 * 
	 * @param p1 one point
	 * @param p2 another point
	 * @return angular distance
	 */
	public static double distance(LatLonAlt p1, LatLonAlt p2) {
		return distance_from_angle(angular_distance(p1.lat(), p1.lon(), p2.lat(), p2.lon()), 0.0);
	}



	// parameter d is the angular distance between lat/lon #1 and lat/lon #2
	static double initial_course_impl2(LatLonAlt p1, LatLonAlt p2, double d) {
		double lat1 = p1.lat();
		double lon1 = p1.lon();
		double lat2 = p2.lat();
		double lon2 = p2.lon();

		if (Math.cos(lat1) < EPS) { 
			// EPS a small positive number, about machine precision
			if (lat1 > 0) {
				return PI; // starting from North pole, all directions are south
			} else {
				return 2.0 * PI; // starting from South pole, all directions are
				// north. JMM: why not 0?
			}
		}
		if (Constants.almost_equals_radian(d)) {
			return 0.0;
			// if the two points are almost the same, then any course is valid
			// returning 0.0 here avoids a 0/0 division (NaN) in the
			// calculations below.
			//
			// This check is used to guard if sin(d) is ever 0.0 in the 
			// division below.  sin(d) for d=PI is never zero given a floating
			// point representation of PI.  Therefore, we only need
			// to check if d == 0.0.  Thank you PVS!
		}

		// Different than Aviation Formulary due to +East convention we use.
		double acos1 = (Math.sin(lat2) - Math.sin(lat1) * Math.cos(d))
				/ (Math.sin(d) * Math.cos(lat1));
		double tc1 = Util.acos_safe(acos1);
		if (! Util.almost_equals(lon2, lon1, Util.PRECISION13) && (Math.sin(lon2 - lon1) <= 0)) {
			tc1 = 2 * PI - tc1; //Util.acos_safe(acos1)
		}
		return tc1;
	}

	private static double initial_course_impl1(LatLonAlt p1, LatLonAlt p2) {
		double lat1 = p1.lat();
		double lon1 = p1.lon();
		double lat2 = p2.lat();
		double lon2 = p2.lon();
		if (Math.cos(lat1) < EPS) { 
			// EPS a small positive number, about machine precision
			if (lat1 > 0) {
				return PI; // starting from North pole, all directions are south
			} else {
				return 2.0 * PI; // starting from South pole, all directions are
				// north. JMM: why not 0?
			}
		}
		// Different than Aviation Formulary due to +East convention we use.
		return Util.to_2pi(Util.atan2_safe(Math.sin(lon2-lon1)*Math.cos(lat2), Math.cos(lat1)*Math.sin(lat2)-Math.sin(lat1)*Math.cos(lat2)*Math.cos(lon2-lon1)));
	}

	/**
	 * The initial true course (course relative to true north) at lat/long #1 on
	 * the great circle route from lat/long #1 to lat/long #2. The value is in
	 * internal units of angles (radians), and is a compass angle [0..2*Pi]:
	 * clockwise from true north.
	 * <p>
	 * 
	 * Usage Note: If lat/long #1 and #2 are close to each other, then the
	 * initial course may become unstable. In the extreme case when lat/long #1
	 * equals lat/long #2, then the initial course is undefined.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return initial course
	 */
	public static double initial_course(double lat1, double lon1, double lat2, double lon2) {
		LatLonAlt p1 = LatLonAlt.mk(lat1, lon1, 0.0);
		LatLonAlt p2 = LatLonAlt.mk(lat2, lon2, 0.0);
		return initial_course(p1, p2);
	}

	/**
	 * <p>The initial true course (course relative to true north) at point #1 on
	 * the great circle route from point #1 to point #2. The value is in
	 * internal units of angles (radians), and is a compass angle [0..2*Pi]:
	 * clockwise from true north.
	 * </p>
	 * 
	 * <p>Usage Note: If point #1 and #2 are close to each other, then the 
	 * course may become unstable. In the extreme case when point #1 equals
	 * point #2, then the course is undefined.</p>
	 * 
	 * @param p1 a point
	 * @param p2 another point
	 * @return initial course
	 */
	public static double initial_course(LatLonAlt p1, LatLonAlt p2) {
		return initial_course_impl1(p1, p2); 
		// Here is an alternate implementation with small numerical issues:
		//double d = angular_distance(p1.lat(), p1.lon(), p2.lat(), p2.lon())
		//return initial_course_impl(p1, p2, d)
	}

	/**
	 * <p>Course of the great circle coming in from point #1 to point #2.  This
	 * value is NOT a compass angle (in the 0 to 2 Pi range), but is in radians 
	 * from clockwise from true north.</p>
	 * 
	 * <p>Usage Note: If point #1 and #2 are close to each other, then the 
	 * course may become unstable. In the extreme case when point #1 equals
	 * point #2, then the course is undefined.</p>
	 * 
	 * @param p1 point #1
	 * @param p2 point #2
	 * @return final course
	 */
	public static double final_course(LatLonAlt p1, LatLonAlt p2) {
		return initial_course(p2, p1)+PI;
	}

	/**
	 * A representative course (course relative to true north) for the entire
	 * arc on the great circle route from lat/long #1 to lat/long #2. The value
	 * is in internal units of angles (radians), and is a compass angle
	 * [0..2*Pi]: clockwise from true north. This is currently calculated as the
	 * initial course from the midpoint of the arc to its endpoint.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return representative course
	 */
	public static double representative_course(double lat1, double lon1, double lat2, double lon2) {
		LatLonAlt p1 = LatLonAlt.mk(lat1, lon1, 0.0);
		LatLonAlt p2 = LatLonAlt.mk(lat2, lon2, 0.0);
		double d = angular_distance(lat1, lon1, lat2, lon2);
		LatLonAlt midPt = interpolate_impl(p1, p2, d, 0.5, 0.0);
		return initial_course_impl1(midPt, p2); 
		// Here is an alternate implementation with numerical problems
		//return initial_course_impl2(midPt, p2, d / 2.0)
	}

	public static double representative_course(LatLonAlt p1, LatLonAlt p2) {
		return representative_course(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}

	/**
	 * Estimate the maximum latitude of the great circle defined by the 
	 * two points.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return maximum latitude
	 */
	public static double max_latitude_gc(double lat1, double lon1, double lat2, double lon2) {
		return max_latitude_gc_course(lat1, initial_course(lat1,lon1,lat2,lon2));
	}

	private static double max_latitude_gc_course(double lat1, double trk) {
		return Util.acos_safe(Math.abs(Math.sin(trk)*Math.cos(lat1)));
	}

	/**
	 * Estimate the maximum latitude of the great circle defined by the 
	 * two points.
	 * 
	 * @param p1 point 1
	 * @param p2 point 2
	 * @return maximum latitude
	 */
	public static double max_latitude_gc(LatLonAlt p1, LatLonAlt p2) {
		return max_latitude_gc(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}

	/**
	 * Estimate the minimum latitude of the great circle defined by the 
	 * two points.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return minimum latitude
	 */
	public static double min_latitude_gc(double lat1, double lon1, double lat2, double lon2) {
		return -max_latitude_gc_course(lat1, initial_course(lat1,lon1,lat2,lon2));
	}

	/**
	 * Estimate the minimum latitude of the great circle defined by the 
	 * two points.
	 * 
	 * @param p1 point 1
	 * @param p2 point 2
	 * @return minimum latitude
	 */
	public static double min_latitude_gc(LatLonAlt p1, LatLonAlt p2) {
		return min_latitude_gc(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}

	// Given a great circle defined by Point 1 and 2, find the longitude of where it 
	// crosses the latitude defined by lat3.
	//
	// from Aviation Formulary
	// longitude sign is reversed from the formulary!
	protected static double lonCross(double lat1, double lon1, double lat2, double lon2, double lat3) {
		double tc = initial_course(lat1,lon1,lat2,lon2);
		boolean NW = (tc > Math.PI/2 && tc <= Math.PI) || tc >= 3*Math.PI/2;
		double l12;

		if (NW) l12 = lon1-lon2;
		else l12 = lon2-lon1;

		double A = Math.sin(lat1)*Math.cos(lat2)*Math.cos(lat3)*Math.sin(l12);
		double B = Math.sin(lat1)*Math.cos(lat2)*Math.cos(lat3)*Math.cos(l12) - Math.cos(lat1)*Math.sin(lat2)*Math.cos(lat3);
		double lon;

		if (NW) lon = lon1 + Util.atan2_safe(B,A) + Math.PI;
		else lon = lon1 - Util.atan2_safe(B,A) - Math.PI;

		if (lon >= 2*Math.PI) lon = lon-2*Math.PI;

		if (NW) {
			lon = lon-Math.PI;
		} else {
			lon = Math.PI+lon;
		}

		if (lon < -Math.PI) lon = 2*Math.PI+lon;
		if (lon > Math.PI) lon = -2*Math.PI+lon;

		return lon;
	}



	/**
	 * Find the minimum (southern-most) latitude of the line segment defined by the two points
	 * along a great circle path.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return minimum latitude
	 */
	public static double min_latitude(double lat1, double lon1, double lat2, double lon2) {
		// special case: if both lat1 & lat2 are above the equator, then min_lat will be either lat1 or lat2
		// special case: if the segment is less than 1/4 earth circumference and the points are on opposite sides of the equator, choose the smallest
		if (lat1 >= 0 && lat2 >= 0) return Math.min(lat1, lat2);  // the segment will, at most, curve north
		double dist = distance(lat1,lon1,lat2,lon2);
		if (Util.sign(lat1) != Util.sign(lat2) && dist < GreatCircle.spherical_earth_radius*Math.PI/2) return Math.min(lat1, lat2); // things are only this simple if we can't hit the inflection point
		double minLat;
		// this block is new --- GEH
		if (dist < GreatCircle.spherical_earth_radius*Math.PI) {
			// special case: we are in the southern hemisphere and we are heading north, then the first point is the max
			if (lat1 <= lat2) {
				double trk = initial_course(lat1, lon1, lat2, lon2);
				if (lat1 < 0 && (trk <= 0.5*Math.PI || trk >= 1.5*Math.PI)) {
					return lat1;
				} else {
					minLat = -max_latitude_gc_course(lat1,trk); // negative max is a min
				}
			} else {
				double trk = initial_course(lat2, lon2, lat1, lon1);
				if (lat2 < 0 && (trk <= 0.5*Math.PI || trk >= 1.5*Math.PI)) {
					return lat2;
				} else {
					minLat = -max_latitude_gc_course(lat2,trk); // negative max is a min
				}
			}
			// END BLOCK
		} else {
			minLat = min_latitude_gc(lat1,lon1,lat2,lon2);
		}
		double minLon = lonCross(lat1,lon1,lat2,lon2,minLat);
		if (Util.max(distance(lat1,lon1,minLat,minLon),distance(lat2,lon2,minLat,minLon)) < dist) {
			return minLat;
		} 
		return Util.min(lat1, lat2);
	}

	public static double min_latitude(LatLonAlt p1, LatLonAlt p2) {
		return min_latitude(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}

	/**
	 * Find the maximum (northern-most) latitude of the line segment defined by the two points
	 * along a great circle path.
	 * 
	 * @param lat1 latitude of point 1
	 * @param lon1 longitude of point 1
	 * @param lat2 latitude of point 2
	 * @param lon2 longitude of point 2
	 * @return maximum latitude
	 */
	public static double max_latitude(double lat1, double lon1, double lat2, double lon2) {
		// special case: if both lat1 & lat2 are below the equator, then max_lat will be either lat1 or lat2
		// special case: if the segment is less than 1/4 earth circumference and the points are on opposite sides of the equator, choose the biggest
		if (lat1 <= 0 && lat2 <= 0) return Math.max(lat1, lat2); // the segment will, at most, curve south
		double dist = distance(lat1,lon1,lat2,lon2);
		if (Util.sign(lat1) != Util.sign(lat2) && dist < GreatCircle.spherical_earth_radius*Math.PI/2) return Math.max(lat1, lat2); // things are only this simple if we can't hit the inflection point
		double maxLat;
		// this block is new --- GEH
		if (dist < GreatCircle.spherical_earth_radius*Math.PI) {
			// special case: we are in the northern hemisphere and we are heading south, then the first point is the max
			if (lat1 >= lat2) {
				double trk = initial_course(lat1, lon1, lat2, lon2);
				if (lat1 > 0 && (trk >= 0.5*Math.PI && trk <= 1.5*Math.PI)) {
					return lat1;
				} else {
					maxLat = max_latitude_gc_course(lat1,trk);
				}
			} else {
				double trk = initial_course(lat2, lon2, lat1, lon1);
				if (lat2 > 0 && (trk >= 0.5*Math.PI && trk <= 1.5*Math.PI)) {
					return lat2;
				} else {
					maxLat = max_latitude_gc_course(lat2,trk);
				}
			}
			// END BLOCK
		} else {
			maxLat = max_latitude_gc(lat1,lon1,lat2,lon2);
		}
		double maxLon = lonCross(lat1,lon1,lat2,lon2,maxLat);
		if (Util.max(distance(lat1,lon1,maxLat,maxLon),distance(lat2,lon2,maxLat,maxLon)) < dist) {
			return maxLat;
		} 
		return Util.max(lat1, lat2);
	}

	public static double max_latitude(LatLonAlt p1, LatLonAlt p2) {
		return max_latitude(p1.lat(), p1.lon(), p2.lat(), p2.lon());
	}



	/**
	 * Return the relative time a great circle will next cross the given latitude.
	 * Returns a negative value if it will never cross that latitude (or velocity has zero gs).
	 * Returns zero for time to cross the equator if you are on the equator.
	 * @param lla starting position
	 * @param v initial velocity at starting position
	 * @param lat target latitude
	 * @return time to reach target latitude, or negative if never
	 */
	public static double latitude_cross_time_gs(LatLonAlt lla, Velocity v, double lat) {
		LatLonAlt lla2 = GreatCircle.linear_initial(lla, v, 100.0);
		double maxLat = max_latitude_gc(lla,lla2); //this is an estimate
		if (maxLat > Math.abs(lat) || v.gs() == 0.0) return -1.0; // fail, will never cross
		if (Util.almost_equals(lat, 0.0) && Util.almost_equals(lla.lat(), 0.0) && (Util.almost_equals(v.trk(), Math.PI/2) || Util.almost_equals(v.trk(), -Math.PI/2))) return 0.0; // on equator
		double targetlon = GreatCircle.lonCross(lla.lat(), lla.lon(), lla2.lat(), lla2.lon(), lat);
		LatLonAlt targ = LatLonAlt.mk(lat, targetlon, 0.0);
		return lla.distanceH(targ)/v.gs();
	}

	// parameter d is the angular distance between lat/long #1 and #2
	// parameter f is a fraction between 0.0 and 1.0
	private static LatLonAlt interpolate_impl(LatLonAlt p1, LatLonAlt p2, double d, double f, double alt) {
		if (Constants.almost_equals_radian(d)) {
			return p1.mkAlt(alt);
			// if the two points are almost the same, then consider the two
			// points the same and arbitrarily return one of them (in this case
			// p1) with the altitude that was provided
		}
		double lat1 = p1.lat();
		double lon1 = p1.lon();
		double lat2 = p2.lat();
		double lon2 = p2.lon();
		double a = Math.sin((1 - f) * d) / Math.sin(d);
		double b = Math.sin(f * d) / Math.sin(d);
		double x = a * Math.cos(lat1) * Math.cos(lon1) + b * Math.cos(lat2)	* Math.cos(lon2);
		double y = a * Math.cos(lat1) * Math.sin(lon1) + b * Math.cos(lat2)	* Math.sin(lon2);
		double z = a * Math.sin(lat1) + b * Math.sin(lat2);
		return LatLonAlt.mk(Util.atan2_safe(z, Math.sqrt(x * x + y * y)), // lat
				Util.atan2_safe(y, x), // longitude
				alt); // alt
	}

	/**
	 * Find the position (latitude, longitude, and altitude) of a point on the
	 * great circle from point #1 to point #2 as a fraction of the distance
	 * between the two points. If the fraction is 0.0 then point #1 is returned,
	 * if the fraction is 1.0 then point #2 is returned. If a fraction less than
	 * zero or greater than one is used, then this function will extrapolate
	 * along the great circle.
	 * <p>
	 * 
	 * Usage Notes:
	 * <ul>
	 * <li>The return value r has r.x as latitude and r.y as longitude. This is
	 * different than in the Vect4 class.
	 * <li>Behavior of this function is undefined if the two points are
	 * antipodal (i.e. lat1+lat2=0 and abs(lon1-lon2)=pi) because a unique great
	 * circle line is undefined (there are infinitely many of them).
	 * <li>if lat/long #1 is almost the same as #2, then #1 is returned
	 * </ul>
	 * 
	 * @param p1 point #1
	 * @param p2 point #1
	 * @param f decimal fraction
	 * @return a new point between p1 and p2
	 */
	public static LatLonAlt interpolate(LatLonAlt p1, LatLonAlt p2, double f) {
		double d = angular_distance(p1, p2);
		return interpolate_impl(p1, p2, d, f, (p2.alt() - p1.alt()) * f + p1.alt());
	}

	/**
	 * This is a fast but crude way of interpolating between relatively close geodesic points
	 * 
	 * @param p1 point #1
	 * @param p2 point #1
	 * @param f decimal fraction
	 * @return a new point between p1 and p2
	 */
	public static LatLonAlt interpolateEst(LatLonAlt p1, LatLonAlt p2, double f) {
		return LatLonAlt.mk((p2.lat() - p1.lat()) * f + p1.lat(),
				(p2.lon() - p1.lon()) * f + p1.lon(),
				(p2.alt() - p1.alt()) * f + p1.alt());
	}

	/**
	 * Return the initial velocity at a point on the great circle that is between p1 and p2.
	 * @param p1 starting point
	 * @param p2 ending point
	 * @param time total time between p1 and p2
	 * @param f fraction of total time to place midpoint
	 * @return velocity at midpoint 
	 */
	public static Velocity interpolate_velocity(LatLonAlt p1, LatLonAlt p2, double time, double f) {
		LatLonAlt mid = interpolate(p1,p2,f);
		return velocity_initial(mid,p2,(1-f)*time);
	}



	/**
	 * <p>Solve the spherical triangle when one has a side (in angular distance), another side, and an angle between sides.
	 * The angle is <b>not</b> between the sides.  The sides are labeled a, b, and c.  The angles are labelled A, B, and
	 * C.  Side a is opposite angle A, and so forth.</p>
	 * 
	 * <p>Given these constraints, in some cases two solutions are possible.  To
	 * get one solution set the parameter firstSolution to true, to get the other set firstSolution to false.  
	 * A firstSolution == true
	 * will return a smaller angle, B, than firstSolution == false.</p>
	 * 
	 * <p>Note: we are not completely confident about the numerical stability of this implementation; it may 
	 * change in the future as we understand the problem better.</p>
	 * 
	 * @param b one side (in angular distance)
	 * @param a another side (in angular distance)
	 * @param A the angle opposite the side a 
	 * @param firstSolution select which solution to use
	 * @return a Triple of angles B and C, and the side c.
	 */
	public static Triple<Double,Double,Double> side_side_angle(double b, double a, double A, boolean firstSolution) {
		// This function follows the convention of "Spherical Trigonometry" by Todhunter, Macmillan, 1886
		//   Note, angles are labeled counter-clockwise a, b, c

		// Law of sines
		double B = Util.asin_safe(Math.sin(b)*Math.sin(A)/Math.sin(a));  // asin returns [-pi/2,pi/2]
		if ( ! firstSolution) {
			B = Math.PI - B; 
		}

		// one of Napier's analogies
		double c = 2 * Util.atan2_safe(Math.sin(0.5*(a+b))*Math.cos(0.5*(A+B)),Math.cos(0.5*(a+b))*Math.cos(0.5*(A-B)));

		// Law of cosines
		double C = Util.acos_safe(-Math.cos(A)*Math.cos(B)+Math.sin(A)*Math.sin(B)*Math.cos(c));

		if ( gauss_check(a,b,c,A,B,C)) {
			return Triple.make(Util.to_pi(B),C,Util.to_2pi(c));
		} else {
			return Triple.make(0.0,0.0,0.0);
		}
	}


	/**
	 * <p>Solve the spherical triangle when one has a side (in angular distance), and two angles.
	 * The side is <b>not</b> between the angles.  The sides are labeled a, b, and c.  The angles are labelled A, B, and
	 * C.  Side a is opposite angle A, and so forth.</p>
	 * 
	 * <p>Given these constraints, in some cases two solutions are possible.  To
	 * get one solution set the parameter firstSolution to true, to get the other set firstSolution to false.  A firstSolution == true
	 * will return a smaller side, b, than firstSolution == false.</p>
	 * 
	 * <p>Note: we are not completely confident about the numerical stability of this implementation; it may 
	 * change in the future as we understand the problem better.</p>
	 * 
	 * @param a one side (in angular distance)
	 * @param A the angle opposite the side a 
	 * @param B another angle
	 * @param firstSolution select which solution to use
	 * @return a Triple of side b, angle C, and the side c.
	 */
	public static Triple<Double,Double,Double> side_angle_angle(double a, double A, double B, boolean firstSolution) {
		// This function follows the convention of "Spherical Trigonometry" by Todhunter, Macmillan, 1886
		//   Note, angles are labeled counter-clockwise a, b, c

		// Law of sines
		double b = Util.asin_safe(Math.sin(a)*Math.sin(B)/Math.sin(A));  // asin returns [-pi/2,pi/2]
		if ( ! firstSolution) {
			b = Math.PI - b;  
		}

		// one of Napier's analogies
		double c = 2 * Util.atan2_safe(Math.sin(0.5*(a+b))*Math.cos(0.5*(A+B)),Math.cos(0.5*(a+b))*Math.cos(0.5*(A-B)));

		// Law of cosines
		double C = Util.acos_safe(-Math.cos(A)*Math.cos(B)+Math.sin(A)*Math.sin(B)*Math.cos(c));

		if ( gauss_check(a,b,c,A,B,C)) {
			return Triple.make(Util.to_2pi(b),Util.to_2pi(C),Util.to_2pi(c));
		} else {
			return Triple.make(0.0,0.0,0.0);
		}
	}

	/**
	 * <p>This implements the spherical cosine rule to complete a triangle on the unit sphere</p>
	 * 
	 * <p>Note: we are not completely confident about the numerical stability of this implementation; it may 
	 * change in the future as we understand the problem better.</p>
	 * 
	 * @param a side a (angular distance)
	 * @param C angle between sides a and b
	 * @param b side b (angular distance)
	 * @return triple of A,B,c (angle opposite a, angle opposite b, side opposite C)
	 */
	public static Triple<Double,Double,Double>side_angle_side(double a, double C, double b) {
		double c = Util.acos_safe(Math.cos(a)*Math.cos(b)+Math.sin(a)*Math.sin(b)*Math.cos(C));
		double cRatio = Math.sin(C)/Math.sin(c);
		double A = Util.asin_safe(Math.sin(a)*cRatio);
		double B = Util.asin_safe(Math.sin(b)*cRatio);
		return Triple.make(A,B,c);
	}

	private static boolean gauss_check(double a, double b, double c, double A, double B, double C) {
		// This function follows the convention of "Spherical Trigonometry" by Todhunter, Macmillan, 1886
		//   Note, angles are labeled counter-clockwise a, b, c
		A = Util.to_pi(A);
		B = Util.to_pi(B);
		C = Util.to_pi(C);
		a = Util.to_2pi(a);
		b = Util.to_2pi(b);
		c = Util.to_2pi(c);
		if (A==0.0 || A==Math.PI || B==0.0 || B==Math.PI || C==0.0 || C==Math.PI) return false;
		if (a==0.0 || b==0.0 || c==0.0) return false;
		return Util.almost_equals(Math.cos(0.5*(A+B))*Math.cos(0.5*c),Math.cos(0.5*(a+b))*Math.sin(0.5*C),Util.PRECISION13);
	}


	/**
	 * Find a point on the great circle route from point #1 to point #2,
	 * traveling at the given velocity (only ground speed and vertical speed,
	 * not track angle) for the given amount of time. If points #1 and #2 are
	 * essentially the same, then the direction between these two points is
	 * undefined, so the first point is returned.
	 * <p>
	 * 
	 * This calculation ignores altitude. Small errors (typically less than
	 * 0.5%) will be introduced at typical aircraft altitudes.
	 * 
	 * @param p1 a point
	 * @param p2 another point
	 * @param v velocity
	 * @param t time
	 * @return end point of a linear extrapolation
	 */
	public static LatLonAlt linear_gcgs(LatLonAlt p1, LatLonAlt p2, Velocity v,
			double t) {
		double d = angular_distance(p1, p2);
		if (Constants.almost_equals_radian(d)) {
			// If the two points are about 1 meter apart, then count them as the
			// same.
			return p1;
		}
		double f = angle_from_distance(v.gs() * t) / d;
		return interpolate_impl(p1, p2, d, f, p1.alt() + v.z * t);
	}

	/**
	 * Return a new location on the great circle path from p1 to p2 that is
	 * distance d from p1
	 * 
	 * @param p1   the first point to define the great circle
	 * @param p2   the second point to define the great circle
	 * @param d    distance from point #1 [m]
	 * @return a new position that is distance d from point #1
	 */
	public static LatLonAlt linear_gc(LatLonAlt p1, LatLonAlt p2, double d) {
		double dist = angular_distance(p1, p2);
		double f = angle_from_distance(d) / dist;
		return interpolate_impl(p1, p2, dist, f,
				(p2.alt() - p1.alt()) * f + p1.alt());
	}

	/**
	 * Find a point from the given lat/lon when traveling at the given velocity
	 * for the given amount of time. This calculation follows the rhumb line
	 * (loxodrome or line of constant track).
	 * <p>
	 * 
	 * Modern aircraft (and most ships) usually travel great circles not rhumb
	 * lines, therefore linear_initial() is usually the preferred over this
	 * function.
	 * <p>
	 * 
	 * At "normal" latitudes, rhumb lines are usually within a few percent of
	 * the great circle route. However, near the poles the behavior of rhumb
	 * lines is not intuitive: if the destination is a point near the pole, then
	 * the rhumb line may spiral around the pole to get to the destination. In
	 * fact, if you maintain a constant track angle along a rhumb line for a
	 * long enough distance, gradually the line will spiral in towards one of
	 * the poles.
	 * <p>
	 * 
	 * Rhumb lines are not defined at the exact north and south poles, therefore
	 * if the origin or destination is precisely at a pole, this function will
	 * choose a point near the pole.
	 * <p>
	 * 
	 * This calculation is approximate: small errors (typically less than 0.5%)
	 * will be introduced at typical aircraft altitudes.
	 * 
	 * @param s position
	 * @param v velocity
	 * @param t time
	 * @return linear extrapolation along a rhumb line
	 */
	public static LatLonAlt linear_rhumb(LatLonAlt s, Velocity v, double t) {
		return linear_rhumb_impl(s, v.trk(), angle_from_distance(v.gs() * t),
				v.z * t);
	}

	/**
	 * Find a point from the given lat/lon at an angle of 'track' at a distance
	 * of 'dist'. This calculation follows the rhumb line (loxodrome or line of
	 * constant track).
	 * <p>
	 * 
	 * Modern aircraft (and most ships) usually travel great circles not rhumb
	 * lines, therefore linear_initial() is usually preferred over this
	 * function.
	 * <p>
	 * 
	 * At "normal" latitudes, rhumb lines are usually within a few percent of
	 * the great circle route. However, near the poles the behavior of rhumb
	 * lines is not intuitive: if the destination is a point near the pole, then
	 * the rhumb line may spiral around the pole to get to the destination. In
	 * fact, if you maintain a constant track angle along a rhumb line for a
	 * long enough distance, gradually the line will spiral in towards one of
	 * the poles.
	 * <p>
	 * 
	 * Rhumb lines are not defined at the exact north and south poles, therefore
	 * if the origin or destination is precisely at a pole, this function will
	 * choose a point near the pole.
	 * <p>
	 * 
	 * This calculation is approximate: small errors (typically less than 0.5%)
	 * will be introduced at typical aircraft altitudes.
	 * 
	 * @param s position
	 * @param track track angle
	 * @param dist distance
	 * @return linear extrapolation along a rhumb line
	 */
	public static LatLonAlt linear_rhumb(LatLonAlt s, double track, double dist) {
		return linear_rhumb_impl(s, track, angle_from_distance(dist), 0.0);
	}

	/**
	 * Find the final point from the given lat/lon when traveling along the great circle
	 * with the given <b>final</b> velocity for the given amount of time.
	 * <p>
	 * 
	 * This calculation is approximate: small errors (typically less than 0.5%)
	 * will be introduced at typical aircraft altitudes.
	 * 
	 * @param s position
	 * @param v velocity
	 * @param t time
	 * @param firstSolution if true, return first solution.  Most of the time there is no difference between the solutions, so this arbitrary.
	 * @return linear extrapolation of point
	 */
	public static LatLonAlt linear_final(LatLonAlt s, Velocity v, double t, boolean firstSolution) {
		double c = GreatCircle.angle_from_distance(v.gs() * t);  // angular distance between initial and final point

		double B;
		double trk = v.trk();
		if (Math.abs(trk) > Math.PI/2 && Math.abs(trk) < Math.PI) {
			B = trk - Math.PI;
		} else {
			B = 2*Math.PI - trk;			
		}

		double b;
		if (s.lat() > 0) {
			b = Math.PI/2 - s.lat(); // use north pole in northern hemisphere
		} else { 
			b = Math.PI/2 + s.lat(); // use south pole in southern hemisphere
		}

		// Solution #1
		Triple<Double,Double,Double> triple = side_side_angle(c,b,B,true);
		double C = triple.first;
		double A = triple.second;
		double a = triple.third;
		boolean one_valid = (C != 0.0) || (A != 0.0) || (a != 0.0); 
		//f.pln("1: a="+Units.str("deg",a,10)+" A="+Units.str("deg",A,10)+" b="+Units.str("deg",b,10)+" B="+Units.str("deg",B,10)+" c="+Units.str("deg",c,10)+" C="+Units.str("deg",C,10)+" s.lon="+Units.str("deg",s.lon(),10));	
		double lat2;
		if (s.lat() > 0) {
			lat2 = Util.to_pi2_cont(Math.PI/2 - a);
		} else {
			lat2 = Util.to_pi2_cont(a - Math.PI/2);			
		}
		double lon2 = Util.to_pi(s.lon() - C);
		LatLonAlt p1 = LatLonAlt.mk(lat2,lon2,s.alt()+v.vs()*t);

		// Solution #2
		triple = side_side_angle(c,b,B,false);
		C = triple.first;
		A = triple.second;
		a = triple.third;
		boolean two_valid = (C != 0.0) || (A != 0.0) || (a != 0.0); 
		//f.pln("2: a="+Units.str("deg",a,10)+" A="+Units.str("deg",A,10)+" b="+Units.str("deg",b,10)+" B="+Units.str("deg",B,10)+" c="+Units.str("deg",c,10)+" C="+Units.str("deg",C,10)+" s.lon="+Units.str("deg",s.lon(),10));
		if (s.lat() > 0) {
			lat2 = Util.to_pi2_cont(Math.PI/2 - a);
		} else {
			lat2 = Util.to_pi2_cont(a - Math.PI/2);
		}
		lon2 = Util.to_pi(s.lon() - C);
		LatLonAlt p2 = LatLonAlt.mk(lat2,lon2,s.alt()+v.vs()*t);

		if (one_valid && two_valid) {
			if (firstSolution) {
				return p1;
			} else {
				return p2;
			}
		} else if (one_valid) {
			return p1;
		} else if (two_valid) {
			return p2;
		}
		return null; // no valid solution
	}

	/**
	 * Find a point from the given lat/lon ('s') when traveling along the great circle
	 * with the given initial velocity for the given amount of time.
	 * <p>
	 * 
	 * This calculation is approximate: small errors (typically less than 0.5%)
	 * will be introduced at typical aircraft altitudes.
	 * 
	 * @param s a position
	 * @param v velocity
	 * @param t time
	 * @return position that is t seconds from s going velocity v
	 */
	public static LatLonAlt linear_initial(LatLonAlt s, Velocity v, double t) {
		return linear_initial_impl(s, v.trk(), angle_from_distance(v.gs() * t),
				v.z * t);
	}

	/**
	 * Find a point from the given lat/lon ('s') with an initial 'track' angle at a distance
	 * of 'dist'. This calculation follows the great circle.
	 * <p>
	 * 
	 * Note: this method does not compute an accurate altitude<p>
	 * 
	 * 
	 * @param s     a position
	 * @param track the initial course coming from point s, assuming a great circle is followed
	 * @param dist  distance from point #1 over the surface of the Earth [m], for very small distances, this 
	 *              method returns inaccurate results.
	 * @return a new position that is distance d from point #1
	 */
	public static LatLonAlt linear_initial(LatLonAlt s, double track, double dist) {
		return linear_initial_impl(s, track, angle_from_distance(dist), 0.0);
	}


	private static LatLonAlt linear_initial_impl(LatLonAlt s, double track, double d, double vertical) {
		double cosd = Math.cos(d);
		double sind = Math.sin(d);
		double sinslat = Math.sin(s.lat());
		double cosslat = Math.cos(s.lat());
		double lat = Util.asin_safe(sinslat * cosd + cosslat * sind * Math.cos(track));
		double dlon = Util.atan2_safe(Math.sin(track) * sind * cosslat, cosd - sinslat * Math.sin(lat));
		// slightly different from aviation formulary because we use
		// "east positive" convention
		double lon = Util.to_pi(s.lon() + dlon);
		return LatLonAlt.mk(lat, lon, s.alt() + vertical);
	}

	private static LatLonAlt linear_rhumb_impl(LatLonAlt s, double track,
			double d, double vertical) {
		// -- Based on the calculation in the "Rhumb line" section of the
		// Aviation Formulary v1.44
		// -- Weird things happen to rhumb lines that go to the poles, therefore
		// force any polar latitudes to be "near" the pole

		final double eps = 1e-15;
		double s_lat = Util.max(Util.min(s.lat(), PI / 2 - eps), -PI / 2 + eps);
		double lat = s_lat + d * Math.cos(track);
		lat = Util.max(Util.min(lat, PI / 2 - eps), -PI / 2 + eps);

		double q;
		if (Constants.almost_equals_radian(lat, s_lat)) {
			q = Math.cos(s_lat);
		} else {
			double dphi = Math.log(Math.tan(lat / 2 + PI / 4)
					/ Math.tan(s_lat / 2 + PI / 4));
			q = (lat - s_lat) / dphi;
		}
		double dlon = -d * Math.sin(track) / q;

		// slightly different from aviation formulary because I use
		// "east positive" convention
		double lon = Util.to_pi(s.lon() - dlon);
		return LatLonAlt.mk(lat, lon, s.alt() + vertical);
	}

	/**
	 * <p>This function forms a great circle from p1 to p2, then computes 
	 * the shortest distance of another point (offCircle) to the great circle.  This is the 
	 * cross track distance. A positive 
	 * value means offCircle is to the right of the path from p1 to p2.  A 
	 * negative value means offCircle is to the left of the path from p1 to p2.</p>
	 *  
	 * @param p1 the starting point of the great circle
	 * @param p2 another point on the great circle
	 * @param offCircle the point to measure the cross track distance
	 * @return the signed cross track distance [m]
	 */
	public static double cross_track_distance(LatLonAlt p1, LatLonAlt p2, LatLonAlt offCircle) {
		double dist_p1oc = angular_distance(p1,offCircle);
		//double trk_p1oc = initial_course_impl(p1,offCircle,dist_p1oc);  // removed because it introduced some numerical instabilities
		double trk_p1oc = initial_course_impl1(p1,offCircle); 
		double trk_p1p2 = initial_course_impl1(p1,p2);
		// This is a direct application of the "spherical law of sines"
		return distance_from_angle(Util.asin_safe(Math.sin(dist_p1oc)*Math.sin(trk_p1oc-trk_p1p2)), (p1.alt()+p2.alt()+offCircle.alt())/3.0);
	}


	/**
	 * Determines if the three points are on the same great circle.
	 * @param p1 One point
	 * @param p2 Second point
	 * @param p3 Third point
	 * @param epsilon allowed cross track variance (in m)
	 * @return true, if the three points are collinear
	 */
	public static boolean collinear(LatLonAlt p1, LatLonAlt p2, LatLonAlt p3, double epsilon) {
//		if (Util.within_epsilon(cross_track_distance(p1,p2,p3),epsilon)) {
//			f.pln("dist "+cross_track_distance(p1,p2,p3));
//			f.pln("ad "+angular_distance(p1,p3));
//			f.pln("ic "+initial_course_impl1(p1,p3)); 
//			f.pln("ic "+initial_course_impl1(p1,p2));
//			f.pln("asin "+Util.asin_safe(Math.sin(angular_distance(p1,p3))*Math.sin(0.0)));
//		}
		return Util.within_epsilon(cross_track_distance(p1,p2,p3),epsilon);
	}

	/**
	 * Determines if the three points are on the same great circle.
	 * @param p1 One point
	 * @param p2 Second point
	 * @param p3 Third point
	 * @return true, if the three points are collinear (2d)
	 */
	public static boolean collinear(LatLonAlt p1, LatLonAlt p2, LatLonAlt p3) {
		double epsilon = 1E-7; // meters
		return collinear(p1,p2,p3,epsilon);
	}

	
	/**
	 * Return true of p2 is on the same great circle as p1/v
	 * @param p1 point one
	 * @param v velocity
	 * @param p2 point two
	 * @return true, if points are collinear (in a great-circle sense)
	 */
	public static boolean collinear(LatLonAlt p1, Velocity v, LatLonAlt p2) {
		LatLonAlt p3 = linear_initial(p1, v, 100.0);
		return collinear(p1, p2, p3);
	}

	/**
	 * This returns the point on the great circle running through p1 and p2 that is closest to point x.
	 * The altitude of the output is the same as x.<p>
	 * If p1 and p2 are the same point, then every great circle runs through them, thus x is on one of these great circles.  In this case, x will be returned.  
	 * @param p1 the starting point of the great circle
	 * @param p2 another point on the great circle
	 * @param x point to determine closest segment point to.
	 * @return the LatLonAlt point on the segment that is closest (horizontally) to x
	 */
	public static LatLonAlt closest_point_circle(LatLonAlt p1, LatLonAlt p2, LatLonAlt x) {
		// almost same point or antipode:
		if ((Util.almost_equals(p1.lat(),p2.lat()) && Util.almost_equals(p1.lon(),p2.lon())) || 
				(Util.almost_equals(p1.lat(),-p2.lat()) && Util.almost_equals(p1.lon(),Util.to_pi(p2.lon()+Math.PI)))) return x; 
		Vect3 a = spherical2ecef_noalt(p1.lat(), p1.lon());
		Vect3 b = spherical2ecef_noalt(p2.lat(), p2.lon());
		Vect3 c = a.cross(b);
		Vect3 p = spherical2ecef_noalt(x.lat(), x.lon());
		Vect3 g = p.Sub(c.Scal(p.dot(c)/c.sqv()));
		double v = spherical_earth_radius/g.norm();
		return ecef_noalt2spherical(g.Scal(v)).mkAlt(x.alt()); // return to x's altitude
	}

	/**
	 * This returns the point on the great circle segment running through p1 and p2 that is closest to point x.
	 * This will return either p1 or p2 if the actual closest point is outside the segment.
	 * @param p1 the starting point of the great circle
	 * @param p2 another point on the great circle
	 * @param x point to determine closest segment point to.
	 * @return the LatLonAlt point on the segment that is closest (horizontally) to x
	 */
	public static LatLonAlt closest_point_segment(LatLonAlt p1, LatLonAlt p2, LatLonAlt x) {
		LatLonAlt c = closest_point_circle(p1,p2,x);
		double d12 = distance(p1,p2);
		double d1c = distance(p1,c);
		double d2c = distance(p2,c);
		if (d1c < d12 && d2c < d12) {
			return c;
		}
		if (d1c < d2c) {
			return p1;
		} else {
			return p2;
		}
	}



	/**
	 * Given two great circles defined by a1,a2 and b1,b2, return the intersection point that is closest a1.  Use LatLonAlt.antipode() to get the other value.
	 * This assumes that the arc distance between a1,a2 &lt; 90 and b1,b2 &lt; 90
	 * The altitude of the return value is equal to a1.alt()
	 * This returns an INVALID value if both segments are collinear
	 * 
	 * @param a1 point #1 to form great circle #1
	 * @param a2 point #2 to form great circle #1
	 * @param b1 point #1 to form great circle #2
	 * @param b2 point #2 to form great circle #2
	 * @return the point that intersects the two great circles
	 */
	public static LatLonAlt intersection(LatLonAlt a1, LatLonAlt a2, LatLonAlt b1, LatLonAlt b2) {
		// define 2 planes based on the GCs
		Vect3 va = spherical2ecef_noalt(a1.lat(), a1.lon()).cross(spherical2ecef_noalt(a2.lat(), a2.lon()));
		Vect3 vb = spherical2ecef_noalt(b1.lat(), b1.lon()).cross(spherical2ecef_noalt(b2.lat(), b2.lon()));
		double r = GreatCircle.spherical_earth_radius;
		Vect3 vavb = va.cross(vb);
		if (vavb.almostEquals(Vect3.ZERO)) {
			return LatLonAlt.INVALID;
		}
		// find the line of the intersection
		Vect3 v1 = vavb.Scal(r / vavb.norm());
		Vect3 v2 = vavb.Scal(-r / vavb.norm());
		LatLonAlt x1 = ecef_noalt2spherical(v1).mkAlt(a1.alt());
		LatLonAlt x2 = ecef_noalt2spherical(v2).mkAlt(a1.alt());
		// return the closest point to a1
		if (distance(a1,x1) < distance(a1,x2)) {
			return x1;
		} else {
			return x2;
		}
	}

	/**  EXPERIMENTAL
	 * Given two great circle segments defined by a1,a2 and b1,b2, return the intersection point that is closest a1.  
	 * This assumes that the arc distance between a1,a2 &lt; 90 and b1,b2 &lt; 90
	 * The altitude of the return value is equal to a1.alt()
	 * This returns an INVALID value if both segments are collinear or there is no intersection
	 * 
	 * Note: This is very slow compared to the equivalent Vect3 method.
	 * 
	 * @param so  starting point of segment [so,so2]
	 * @param so2 ending point of segment [so,so2]
	 * @param si  starting point of segment [si,si2]
	 * @param si2 ending point of segment [si,si2]
	 * @return the point that intersects the two "great circle" segments
	 */
	public static LatLonAlt intersectSegments(LatLonAlt so, LatLonAlt so2, LatLonAlt si, LatLonAlt si2) {
		LatLonAlt interSec = GreatCircle.intersection(so,so2, si, si2);
		if (interSec.isInvalid()) return LatLonAlt.INVALID;
		double fco = GreatCircle.final_course(so,so2);
		double fco_int = GreatCircle.final_course(so,interSec);
		double turnDelta_o = Util.turnDelta(fco,fco_int);
		boolean before_o = turnDelta_o > Math.PI/2;
		if (before_o) return LatLonAlt.INVALID;                      // BEFORE [so,so2]
		double gso = GreatCircle.distance(so,so2);
		//f.pln(" $$$ intersectSegments: gso = "+Units.str("NM",gso));
		//f.pln(" $$$ intersectSegments: Math.PI*spherical_earth_radius/2 = "+Units.str("NM",spherical_earth_radius*Math.PI/2));
		double oFrac = GreatCircle.distance(so,interSec)/gso;  
		//f.pln(" $$$ intersectSegments: oFrac = "+oFrac);
		if (oFrac > 1) return LatLonAlt.INVALID;                     // AFTER [so,so2]
		double fci = GreatCircle.final_course(si,si2);
		double fci_int = GreatCircle.final_course(si,interSec);
		double turnDelta_i = Util.turnDelta(fci,fci_int);
		boolean before_i = turnDelta_i > Math.PI/2;                 // BEFORE [si,si2]
		if (before_i) return LatLonAlt.INVALID;
		double gsi = GreatCircle.distance(si,si2);
		double iFrac = GreatCircle.distance(si,interSec)/gsi; 
		//f.pln(" $$$ intersectSegments: iFrac = "+iFrac);
		if (iFrac > 1) return LatLonAlt.INVALID;                    // AFTER [si,si2]
		return interSec;
	}

	public static LatLonAlt intersectionSegment(double T, LatLonAlt so, Velocity vo, LatLonAlt si, LatLonAlt si2) {
		LatLonAlt so2 = linear_initial(so,vo,T);
		return intersectSegments(so, so2, si, si2);
	}

	
	/**
	 * Given two great circles defined by so, so2 and si, si2 return the intersection point that is closest to so.
	 * (Note: because on a sphere there are two intersection points, we have to choose one of them, we choose the
	 * one closest to so.)
	 * Calculate altitude of intersection using the average of the altitudes of the two closest points to the
	 * intersection.
	 * 
	 * @param so     first point of line o 
	 * @param so2    second point of line o
	 * @param dto    the delta time between point so and point so2.
	 * @param si     first point of line i
	 * @param si2    second point of line i 
	 * @return a pair: intersection point and the delta time from point "so" to the intersection, can be negative if intersect
	 *                 point is in the past. If intersection point is invalid then the returned delta time is -1
	 */
	public static Pair<LatLonAlt,Double> intersectionAvgAlt(LatLonAlt so, LatLonAlt so2, double dto, LatLonAlt si, LatLonAlt si2) {
		LatLonAlt interSec = GreatCircle.intersection(so, so2, si, si2);
		//f.pln(" %%% GreatCircle.intersection: lgc = "+lgc.toString(15));       
		if (interSec.isInvalid()) return new Pair<LatLonAlt,Double>(interSec,-1.0);
		double gso = distance(so,so2)/dto;
		double intTm = distance(so,interSec)/gso;  // relative to so 
		//f.pln(" ## gso = "+Units.str("kn", gso)+" distance(so,lgc) = "+Units.str("NM",distance(so,lgc)));
		boolean behind = GreatCircle.behind(interSec, so, GreatCircle.velocity_average(so, so2, 1.0)); 
		//		f.pln("behind="+behind+" interSec="+interSec+" so="+so+" vo="+GreatCircle.velocity_average(so, so2, 1.0));
		if (behind) intTm = -intTm;			
		// compute a better altitude using average of nearest points        
		double do1 = distance(so,interSec);
		double do2 = distance(so2,interSec);
		double alt_o = so.alt();
		if (do2 < do1) alt_o = so2.alt();
		double di1 = distance(si,interSec);
		double di2 = distance(si2,interSec);
		double alt_i = si.alt();
		if (di2 < di1) alt_i = si2.alt();
		double nAlt = (alt_o + alt_i)/2.0;       
		//    	f.pln(" $$ LatLonAlt.intersection: so.alt() = "+Units.str("ft",so.alt())+" so2.alt() = "+Units.str("ft",so2.alt())+
		//    			" si.alt() = "+Units.str("ft",si.alt())+" si2.alt() = "+Units.str("ft",si2.alt())+
		//    			" nAlt() = "+Units.str("ft",nAlt));
		//f.pln(" $$ LatLonAlt.intersection: intTm = "+intTm+" vs = "+Units.str("fpm",vs)+" nAlt = "+Units.str("ft",nAlt)+" "+behind);			 
		LatLonAlt pgc = LatLonAlt.mk(interSec.lat(),interSec.lon(),nAlt);
		return new Pair<>(pgc,intTm);
	}

	/**
	 * Given two great circles defined by so, so2 and si, si2 return the intersection point that is closest to so.
	 * (Note. because on a sphere there are two intersection points)
	 *  Calculate altitude of intersection using linear extrapolation from line (so,so2)
	 * 
	 * @param so     first point of line o 
	 * @param so2    second point of line o
	 * @param dto    the delta time between point so and point so2.
	 * @param si     first point of line i
	 * @param si2    second point of line i 
	 * @return a pair: intersection point and the delta time from point "so" to the intersection, can be negative if intersect
	 *                 point is in the past. If intersection point is invalid then the returned delta time is -1
	 */
	public static Pair<LatLonAlt,Double> intersectionExtrapAlt(LatLonAlt so, LatLonAlt so2, double dto, LatLonAlt si, LatLonAlt si2) {
		LatLonAlt lgc = GreatCircle.intersection(so, so2, si, si2);
		if (lgc.isInvalid()) return new Pair<>(lgc,-1.0);
		double gso = distance(so,so2)/dto;
		double intTm = distance(so,lgc)/gso;  // relative to so
		boolean behind = GreatCircle.behind(lgc, so, GreatCircle.velocity_average(so, so2, 1.0));
		if (behind) intTm = -intTm;
		// compute a better altitude
		double vs = (so2.alt() - so.alt())/dto;
		double nAlt = so.alt() + vs*(intTm);
		LatLonAlt pgc = LatLonAlt.mk(lgc.lat(),lgc.lon(),nAlt);
		return new Pair<>(pgc,intTm);
	}

	/** Given two great circles defined by (so, vo) and (si, vi) return the intersection point that is closest to so.
	 *  
	 * @param so           first point of line o 
	 * @param vo           velocity from point so
	 * @param si           first point of line i
	 * @param vi           velocity from point si
	 * @return Position and time of intersection. The returned altitude is so.alt().
	 * If the lines are the same, return an INVALID position.  If the intersection is behind, return a negative time
	 */
	public static Pair<LatLonAlt,Double> intersection(LatLonAlt so, Velocity vo, LatLonAlt si, Velocity vi) {
		LatLonAlt so2 = linear_initial(so, vo, 1000);
		LatLonAlt si2 = linear_initial(si, vi, 1000);
		LatLonAlt i = intersection(so, so2, si, si2);
		//f.pln(" %%% GreatCircle.intersection: i = "+i.toString(15));
		//		if (i.isInvalid() || behind(i, so,vo) || behind(i, si,vi)) return new Pair<LatLonAlt,Double>(LatLonAlt.INVALID,-1.0); // collinear or (nearly) same position or cross in the past
		if (i.isInvalid()) return new Pair<LatLonAlt,Double>(LatLonAlt.INVALID,-1.0); // collinear or (nearly) same position or cross in the past
		double dt = distance(so,i)/vo.gs();
		//f.pln(" ## GreatCircle.intersection: dt = "+dt+" vo.gs() = "+Units.str("kn",vo.gs())+" distance(so,i) = "+Units.str("nm",distance(so,i)));
		if (behind(i, so, vo)) dt = -dt;   // make negative if behind
		return new Pair<>(i,dt);
	}


	/**
	 * <p>Given two great circles defined by a1, a2 and b1, b2 return the angle between them at the intersection point.  
	 * This is the same as the dihedral angle, or angle between the two GC planes. 
	 * This may not be the same angle as the one projected into the Euclidean (unless the projection point is the intersection point). 
	 * and will generally not be the same as the (non-projected) track angle difference between them (though that can be very close).
	 * This will always return a value between 0 and PI.</p>
	 * 
	 * <p>Note: When two great circles intersect, they form supplementary angles, that is, two angles that sum to 180.  This 
	 * method can return either the smaller (less than 90) or the larger (greater than 90).  It is a little
	 * complicated to determine which one will be returned.  Imagine a 'right-handed' vector going from a1 to a2 and another one going
	 * from b1 to b2.  If these two vectors point mostly in the same direction, then the smaller supplementary angle will be returned.
	 * If they point in mostly opposite directions, then the larger supplementary angle will be returned.  As an example.  Imagine three points,
	 * two points (a and c) on the equator, less than 90 degrees apart and point b at the north pole. angleBetween(a,b,c,b) will
	 * return the smaller supplementary angle, and angleBetween(a,b,b,c) will return the larger supplementary angle. </p>
	 * 
	 * @param a1 one point on the first great circle
	 * @param a2 second point on the first great circle
	 * @param b1 one point on the second great circle
	 * @param b2 second point on the second great circle
	 * @return angle between two great circles
	 */
	public static double angleBetween(LatLonAlt a1, LatLonAlt a2, LatLonAlt b1, LatLonAlt b2) {
		Vect3 va = spherical2ecef_noalt(a1.lat(), a1.lon()).cross(spherical2ecef_noalt(a2.lat(), a2.lon())).Hat(); // normal 1
		Vect3 vb = spherical2ecef_noalt(b1.lat(), b1.lon()).cross(spherical2ecef_noalt(b2.lat(), b2.lon())).Hat(); // normal 2
		double cosx = va.dot(vb);
		return Util.acos_safe(cosx);
	}

	/**
	 * Return the turn angle between great circles (this will return a value between 0 and PI)
	 * (uses coordinate transformation)
	 * @param a point on gc1
	 * @param b intersection of gc1 and gc2
	 * @param c point on gc2
	 * @return magnitude of angle between the two great circles, from a-b to b-c
	 */
	public static double angle_between(LatLonAlt a, LatLonAlt b, LatLonAlt c) {
		return angleBetween(b,a,b,c); // Anthony suggests
	}

	/**
	 * Return the turn angle between two great circles, measured in the indicated direction.  This can return a value larger than PI.
	 * @param a first point
	 * @param b turn point
	 * @param c last point
	 * @param dir +1 is right (clockwise), -1 is left (counterclockwise)
	 * @return Value of angle of turn from a-b to b-c
	 */
	public static double angle_between(LatLonAlt a, LatLonAlt b, LatLonAlt c, int dir) {
		double trk1 = GreatCircle.final_course(a, b);
		double trk2 = GreatCircle.initial_course(b,c);
		int calcdir = Util.turnDir(trk1, trk2);
		double theta = angleBetween(b,a,b,c);
		if (dir==calcdir) return theta;
		return 2*Math.PI - theta; // note that this is not quite the same as theta+PI
	}


	/**
	 * Return true if x is "behind" ll, considering its current direction of travel, v.
	 * "Behind" here refers to the hemisphere aft of ll.
	 * That is, x is within the region behind the perpendicular line to v through ll.
	 * @param ll aircraft position
	 * @param v aircraft velocity
	 * @param x intruder position
	 * @return true, if x is behind ll
	 */
	public static boolean behind(LatLonAlt x, LatLonAlt ll, Velocity v) {
		Velocity v2 = velocity_initial(ll, x, 100);
		return Util.turnDelta(v.trk(), v2.trk()) > Math.PI/2.0;
	}

	/**
	 * Returns values describing if the ownship state will pass in front of or behind the intruder (from a horizontal perspective)
	 * @param so ownship position
	 * @param vo ownship velocity
	 * @param si intruder position
	 * @param vi intruder velocity
	 * @return 1 if ownship will pass in front (or collide, from a horizontal sense), -1 if ownship will pass behind, 0 if collinear or parallel or closest intersection is behind you
	 */
	public static int passingDirection(LatLonAlt so, Velocity vo, LatLonAlt si, Velocity vi) {
		Pair<LatLonAlt,Double> p = intersection(so,vo,si,vi);
		if (p.second < 0) return 0;
		LatLonAlt si3 = linear_initial(si,vi,p.second); // intruder position at time of intersection
		if (behind(p.first, si3, vi)) return -1;
		return 1;
	}



	public static int dirForBehind(LatLonAlt so, Velocity vo, LatLonAlt si, Velocity vi) {
		LatLonAlt so2 = linear_initial(so, vo, 1000);
		LatLonAlt si2 = linear_initial(si, vi, 1000);
		LatLonAlt i = intersection(so, so2, si, si2);
		if (i.isInvalid() || behind(i,so,vo) || behind(i,si,vi)) return 0; // collinear or (nearly) same position or cross in the past
		int onRight = Util.sign(cross_track_distance(si,si2,so));
		return -onRight;
	}

	/**
	 * Compute the initial velocity on the great circle from lat/lon #1 to
	 * lat/lon #2 with the given amount of time. If points #1 and #2 are
	 * essentially the same (about 1 meter apart), then a zero vector is
	 * returned. Also if the absolute value of time is less than 1 [ms], then a
	 * zero vector is returned.
	 * <p>
	 * 
	 * If the time is negative, then the velocity is along the great circle
	 * formed by #1 and #2, but in the opposite direction from #2.
	 * <p>
	 * 
	 * This calculation ignores altitude when calculating great circle distance.
	 * Small errors (typically less than 0.5%) will be introduced at typical
	 * aircraft altitudes.
	 * 
	 * @param p1 point 1
	 * @param p2 point 2
	 * @param t time
	 * @return velocity from point 1 to point 2, taking time t
	 */
	public static Velocity velocity_initial(LatLonAlt p1, LatLonAlt p2, double t) {
		// p1 is the source position, p2 is another point to form a great circle
		// positive time is moving from p1 toward p2
		// negative time is moving from p1 away from p2
		if (Math.abs(t) < MIN_DT || Util.almost_equals(Math.abs(t) + MIN_DT, MIN_DT, Util.PRECISION7)) {
			// time is negative or very small (less than 1 ms)
			return Velocity.ZERO;
		}
		double d = angular_distance(p1, p2);
		if (Constants.almost_equals_radian(d)) {
			if (Constants.almost_equals_alt(p1.alt(), p2.alt())) {
				// If the two points are about 1 meter apart, then count them as  the same.
				return Velocity.ZERO;
			} else {
				return Velocity.ZERO.mkVs((p2.alt() - p1.alt()) / t);
			}
		}
		double gs = distance_from_angle(d, 0.0) / t;
		double crs = initial_course_impl1(p1, p2); //, d);  // d removed because of numerical errors introduced
		return Velocity.mkTrkGsVs(crs, gs, (p2.alt() - p1.alt()) / t);
	}

	/**
	 * Estimate the velocity on the great circle from lat/lon #1 to lat/lon #2
	 * with the given amount of time. Essentially, the velocity at the mid point
	 * between lat/lon #1 and lat/lon #2. If points #1 and #2 are essentially
	 * the same (about 1 meter apart), then a zero vector is returned. Also if
	 * the absolute value of time is less than 1 [ms], then a zero vector is
	 * returned.
	 * <p>
	 * 
	 * If the time is negative, then the velocity is along the great circle
	 * formed by #1 and #2, but in the opposite direction from #2.<p>
	 * 
	 * This is an estimate of the velocity. This calculation ignores altitude
	 * when calculating great circle distance. Small errors (typically less than
	 * 0.5%) will be introduced at typical aircraft altitudes.
	 * 
	 * @param p1 a point
	 * @param p2 another point
	 * @param t time
	 * @return average velocity
	 */
	public static Velocity velocity_average(LatLonAlt p1, LatLonAlt p2, double t) {
		// p1 is the source position, p2 is another point on that circle
		// positive time is moving from p1 toward p2
		// negative time is moving from p1 away from p2
		if (t >= 0.0) {
			return velocity_initial(interpolate(p1, p2, 0.5), p2, t / 2.0);
		} else {
			return velocity_average(p1, interpolate(p1, p2, -1.0), -t);
		}
	}

	/**
	 * Estimate the velocity on the great circle from lat/lon #1 to lat/lon #2
	 * with the given speed. 
	 * If the time is negative, then the velocity is along the great circle
	 * formed by #1 and #2, but in the opposite direction from #2.<p>
	 * 
	 * This is an estimate of the velocity. This calculation ignores altitude
	 * when calculating great circle distance. Small errors (typically less than
	 * 0.5%) will be introduced at typical aircraft altitudes.
	 * 
	 * @param s1 a point
	 * @param s2 another point
	 * @param speed speed between point
	 * @return average velocity
	 */
	public static Velocity velocity_average_speed(LatLonAlt s1, LatLonAlt s2, double speed) {
		double dist = GreatCircle.distance(s1, s2);
		double dt = dist/speed;
		return GreatCircle.velocity_average(s1, s2, dt);
	}




	/**
	 * Estimate the final velocity on the great circle from lat/lon #1 to
	 * lat/lon #2 with the given amount of time. The track angle of the velocity
	 * is the course from point #1 to #2 roughly at point #2. If points #1 and
	 * #2 are essentially the same (about 1 meter apart), then a zero vector is
	 * returned. Also if the absolute value of time is less than 1 [ms], then a
	 * zero vector is returned.
	 * <p>
	 *  
	 * If the time is negative, then the velocity is along the great circle
	 * formed by #1 and #2, but in the opposite direction from #2.
	 * <p>
	 * 
	 * This is an estimate of the velocity. This calculation ignores altitude
	 * when calculating great circle distance. Small errors (typically less than
	 * 0.5%) will be introduced at typical aircraft altitudes.
	 * 
	 * @param p1 a point
	 * @param p2 another point
	 * @param t time
	 * @return final velocity 
	 */
	public static Velocity velocity_final(LatLonAlt p1, LatLonAlt p2, double t) {
		// p1 is the source position, p2 is another point on that circle
		// positive time is moving from p1 toward p2
		// negative time is moving from p1 away from p2 (final velocity is the
		// velocity at that time)
		if (t >= 0.0) {
			return velocity_initial(p2, p1, -t);
		} else {
			return velocity_initial(interpolate(p1, p2, -1.0), p1, t);
		}
	}


	/**
	 * Transforms a lat/lon position to a point in R3 (on a sphere)
	 * This is an Earth-Centered, Earth-Fixed translation (assuming earth-surface altitude).
	 * From Wikipedia: en.wikipedia.org/wiki/Curvilinear_coordinates (contents apparently moved to Geodetic datum entry)
	 * 
	 * The x-axis intersects the sphere of the earth at 0 latitude (the equator) and 0 longitude (Greenwich). 
	 * 	
	 * @param lat latitude
	 * @param lon longitude
	 * @return point in R3 on surface of the earth (zero altitude)
	 */
	public static Vect3 spherical2ecef_noalt(double lat, double lon) {
		//return unit_spherical2xyz(lat,lon).Scal(spherical_earth_radius);
		double r = GreatCircle.spherical_earth_radius;
		// convert latitude to 0-PI
		double theta = PI/2 - lat;
		double phi = lon; 
		double x = r*Math.sin(theta)*Math.cos(phi);
		double y = r*Math.sin(theta)*Math.sin(phi);
		double z = r*Math.cos(theta);
		return new Vect3(x,y,z);
	}

	public static Vect3 unit_spherical2ecef(double lat, double lon) {
		// convert latitude to 0-PI
		double theta = PI/2 - lat;
		double phi = lon; 
		double x = Math.sin(theta)*Math.cos(phi);
		double y = Math.sin(theta)*Math.sin(phi);
		double z = Math.cos(theta);
		return new Vect3(x,y,z);
	}
	
	
	/**
	 * Transforms a R3 position on the earth surface into lat/lon coordinates
	 * This is an Earth-Centered, Earth-Fixed translation (ECEF, assuming earth-surface altitude).
	 * From Wikipedia: en.wikipedia.org/wiki/Curvilinear_coordinates (contents apparently moved to Geodetic datum entry)
	 * We take a standard radius of the earth as defined in GreatCircle, and treat altitude as 0. 
	 * @param v position in R3, with ECEF origin
	 * @return LatLonAlt point on surface of the earth (zero altitude)
	 */
	public static LatLonAlt ecef_noalt2spherical(Vect3 v) {
		double r = GreatCircle.spherical_earth_radius;
		double theta = Util.acos_safe(v.z/r);
		double phi = Util.atan2_safe(v.y, v.x);
		double lat = PI/2 - theta;
		double lon = Util.to_pi(phi); 
		return LatLonAlt.mk(lat, lon, 0.0);
	}

	public static LatLonAlt unit_ecef2spherical(Vect3 v) {
		double theta = Util.acos_safe(v.z);
		double phi = Util.atan2_safe(v.y, v.x);
		double lat = PI/2 - theta;
		double lon = Util.to_pi(phi); 
		return LatLonAlt.mk(lat, lon, 0.0);
	}

	/**
	 * Transforms a lat/lon/alt position to a R3 position with the origin being the center of the (spherical) earth.
	 * Unlike the spherical2xyz function, this accounts for the altitude above the earth.
	 * @param lla point
	 * @return vector position
	 */
	public static Vect3 spherical2ecef(LatLonAlt lla) {
		return unit_spherical2ecef(lla.lat(),lla.lon()).Scal(GreatCircle.spherical_earth_radius+lla.alt());
	}
	
	/**
	 * Transforms a R3 position with the origin being the center of the (spherical) earth to a lat/lon/alt position above the surface of the earth
	 * Unlike the xyz2spherical function, this accounts for the altitude above the earth.
	 * @param v vector
	 * @return latitude/longitude position
	 */
	public static LatLonAlt ecef2spherical(Vect3 v) {
		double r = v.norm();
		double alt = r - GreatCircle.spherical_earth_radius;
		return unit_ecef2spherical(v.Scal(1.0/r)).mkAlt(alt);
	}

	
	/**
	 * Return if point P is to the right or left of the line from A to B
	 * @param a point A
	 * @param b point B
	 * @param p point P
	 * @return 1 if to the right or collinear, -1 if to the left.
	 */	
	public static int rightOfLinePoints(LatLonAlt a, LatLonAlt b, LatLonAlt p) {
		Vect3 v1 = spherical2ecef_noalt(a.lat(),a.lon());
		Vect3 v2 = spherical2ecef_noalt(b.lat(),b.lon());
		Vect3 v3 = spherical2ecef_noalt(p.lat(),p.lon());
		return -Util.sign(v3.dot(v1.cross(v2)));
	}

	/**
	 * Return the straight-line chord distance (through a spherical earth) from 
	 * two points on the surface of the earth. 
	 * 
	 * @param lat1 latitude of first point
	 * @param lon1 longitude of first point
	 * @param lat2 latitude of second point
	 * @param lon2 longitude of second point
	 * @return the chord distance
	 */
	public static double chord_distance(double lat1, double lon1, double lat2, double lon2) {
		Vect3 v1 = spherical2ecef_noalt(lat1,lon1);
		Vect3 v2 = spherical2ecef_noalt(lat2,lon2);
		return v1.Sub(v2).norm();
	}

	/**
	 * Return the straight-line chord distance (through a spherical earth) from 
	 * two points on the surface of the earth. 
	 * 
	 * @param lla1 first point
	 * @param lla2 second point
	 * @return the chord distance
	 */
	public static double chord_distance(LatLonAlt lla1, LatLonAlt lla2) {
		return chord_distance(lla1.lat(), lla1.lon(), lla2.lat(), lla2.lon());
	}

	/**
	 * Return the chord distance (through the earth) corresponding to a given surface distance (at the nominal earth radius).
	 * This is the distance of a direct line between two surface points.
	 * @param surface_dist distance across surface
	 * @return chord distance
	 */
	public static double chord_distance(double surface_dist) {
		double theta = angle_from_distance(surface_dist,0.0);
		return 2.0*Math.sin(theta/2.0)*spherical_earth_radius;
	}

	/**
	 * Return the surface distance (at the nominal earth radius) corresponding to a given chord distance (through the earth).
	 * @param chord_distance cordal distance
	 * @return surface distance
	 */
	public static double surface_distance(double chord_distance) {
		double theta = 2.0*Util.asin_safe(chord_distance*0.5 / spherical_earth_radius);
		return distance_from_angle(theta,0.0);
	}
	
	public static double small_circle_radius(double chord) {
		return Util.sqrt_safe(chord*chord*(1-(chord*chord/(4*spherical_earth_radius*spherical_earth_radius))));
	}

	/**
	 * Return the tangent point to a circle based on a given track heading.
	 * Note that if the circle overlaps a pole, not all track headings may be well-defined.
	 * This uses the spherical sine rules and Napier's analogies for the half-angle/half-side formulas.
	 * @param center center point of circle
	 * @param R radius of circle (great-circle distance)
	 * @param track track at point of tangency
	 * @param right ture if clockwise, false if counterclockwise
	 * @return tangent point on circle, or INVALID if a well-defined tangent does not exist (may happen if a pole is within the circle)
	 */
	public static LatLonAlt tangent(LatLonAlt center, double R, double track, boolean right) {
		double PI = Math.PI;
		double D = GreatCircle.distance_from_angle(PI/2 - center.lat(), 0.0);
		//  shortcut failure cases
		if (Util.almost_equals(D, 0.0)) {
			return LatLonAlt.INVALID;
		}
		double trk = Util.to_2pi(track);
		double alpha;
		if (!right) { // if counterclockwise, work with the opposite track.
			trk = Util.to_2pi(trk+PI);
		}
		if (trk >= 0 && trk < PI/2) {
			alpha = trk+PI/2;
		} else if (trk >= PI/2 && trk < 3*PI/2) {
			alpha = 3*PI/2-trk;
		} else {
			alpha = -(3*PI/2-trk);
		}

		// angle from center to tangent point
		double theta = Util.asin_safe(Math.sin(Math.PI-alpha)*Math.sin(R/spherical_earth_radius)/Math.sin(D/spherical_earth_radius));
		// angular dist from pole to tangent point
		double dist = 2*spherical_earth_radius*Math.atan(Math.cos(0.5*(alpha+theta))*Math.tan(0.5*(D+R)/spherical_earth_radius)/Math.cos(0.5*(alpha-theta)));
		// special case when we are directly north or south
		if (Util.almost_equals(trk, PI/2)) {
			dist = D-R;
		} else if (Util.almost_equals(trk,3*PI/2)) {
			dist = D+R;
		}
		double lon;
		if (right == (track >= PI/2 && track <= 3*PI/2)) {
			lon = Util.to_pi(center.lon()+theta);
		} else {
			lon = Util.to_pi(center.lon()-theta);
		}
		return linear_gc(LatLonAlt.mk(PI/2.0, lon, 0),LatLonAlt.mk(0, lon, 0), dist);
	}



	/**
	 * EXPERIMENTAL
	 * Determine the point on the great circle a,b that has the given track.
	 * Direction of travel is assumed to be from a to b.
	 * This will return the point closest to b if two such points exist.
	 * 
	 * @param lla1 point 1
	 * @param lla2 point 2
	 * @param track track angle
	 * @return tangent point on great circle, or INVALID if no such unique point exists (e.g. on a longitude line)
	 */
	public static LatLonAlt tangent(LatLonAlt lla1, LatLonAlt lla2, double track) {
		// infinite points
		if (Util.almost_equals(lla1.lon(),lla2.lon())) {
			return LatLonAlt.INVALID;
		}
		double PI = Math.PI;
		double trk = Util.to_2pi(track);
		LatLonAlt np = LatLonAlt.mk(PI/2,0,0);
		double A = angle_between(lla1, lla2, np); 
		double b = PI/2 - lla2.lat(); // dist from pole to b
		double B = trk;
		double a = Math.sin(b)/(Math.sin(A)*Math.sin(B));
		double abdist = angular_distance(lla1,lla2);
		// unit sphere distance from b to tangent point:
		double c = 2*Math.atan(Math.tan(0.5*(a+b)*Math.cos(0.5*(A+B))/Math.cos(0.5*(A-B))));
		return interpolate(lla2,lla1,c/abdist);
	}


	/**
	 * EXPERIMENTAL
	 * Given a small circle, rotate a point
	 * @param so point on circle
	 * @param center center of circle
	 * @param angle angle of rotation around center (positive is clockwise)
	 * @return another position on the circle
	 */
	public static LatLonAlt small_circle_rotation(LatLonAlt so, LatLonAlt center, double angle) {
		if (Util.almost_equals(angle, 0)) return so;
		//f.pln("R1="+distance(so,center));		
		double R = angular_distance(so, center);
		Triple<Double,Double,Double>ABc = side_angle_side(R, angle, R);
		double A = ABc.first;
		double c = distance_from_angle(ABc.third, 0.0);
		double crs = initial_course(so, center);
		if (crs > Math.PI) crs = crs-2*Math.PI;
		double trk = Util.to_2pi(crs - A);
		return linear_initial(so, trk, c);
	}

	/**
	 * Accurately calculate the linear distance of an arc on a small circle (turn) on the sphere.
	 * @param radius along-surface radius of small circle
	 * @param arcAngle angular (radian) length of the arc.  This is the angle between two great circles that intersect at the small circle's center.
	 * @return linear distance of the small circle arc
	 * Note: A 100 km radius turn over 60 degrees produces about 4.3 m error.
	 */
	public static double small_circle_arc_length(double radius, double arcAngle) {
		// use the chord of the diameter to determine the radius in the ECEF Euclidean frame
		double r2 = chord_distance(radius*2)/2;
		// because this is a circle in a Euclidean frame, use the normal calculations
		return arcAngle*r2;
	}

	/**
	 * Accurately calculate the angular distance of an arc on a small circle (turn) on the sphere.
	 * @param radius along-surface radius of small circle
	 * @param arcLength linear (m) length of the arc.  This is the along-line length of the arc.
	 * @return Angular distance of the arc around the small circle (from 0 o 2pi)
	 * Note: A 100 km radius turn over 100 km of turn produces about 0.0024 degrees of error.
	 */
	public static double small_circle_arc_angle(double radius, double arcLength) {
		// use the chord of the diameter to determine the radius in the ECEF Euclidean frame
		double r2 = chord_distance(radius*2)/2;
		if (r2 == 0.0) return 0.0;
		// because this is a circle in a Euclidean frame, use the normal calculations
		return arcLength/r2;
	}



	// this is hard-coded to stop after at most 50 iterations
	private static double spherical_newton_raphson(double t_est, double epsilon, double w1, double w2, Vect3 c1_0, Vect3 c2_0, Vect3 x1_0, Vect3 x2_0) {
		double deltaT = Double.MAX_VALUE;  //initially big to start things going
		double ti = t_est; // current time estimate

		double A = -(x1_0.Scal(w1).dot(c2_0) + x2_0.Scal(w2).dot(c1_0));
		double B = c1_0.Scal(w1).dot(x2_0) + c2_0.Scal(w2).dot(x1_0);
		double C = -(x1_0.Scal(w1).dot(x2_0) - c2_0.Scal(w2).dot(c1_0));
		double D = c1_0.Scal(w1).dot(c2_0) - x2_0.Scal(w2).dot(x1_0);
		
		int i = 0;
		while (Math.abs(deltaT) > epsilon) {
			double fi = A*Math.sin(w1*ti)*Math.sin(w2*ti) + B*Math.cos(w1*ti)*Math.cos(w2*ti) 
					  + C*Math.sin(w1*ti)*Math.cos(w2*ti) + D*Math.cos(w1*ti)*Math.sin(w2*ti); 

			double fiPrime = -(C*w2 + D*w1)*Math.sin(w1*ti)*Math.sin(w2*ti) + (D*w2 + C*w1)*Math.cos(w1*ti)*Math.cos(w2*ti) 
					   + (A*w2 - B*w1)*Math.sin(w1*ti)*Math.cos(w2*ti) - (B*w2 - A*w1)*Math.cos(w1*ti)*Math.sin(w2*ti);

			deltaT = fi/fiPrime;
			ti = ti-deltaT;
			i++;
			if (i > 50) { // hard coded to stop after 50 iterations, 50 iterations is A LOT for newton-raphson
				return Double.NaN; // not converging
			}
		}
		return ti;
	}

	private static Vect3 course_vector(double lat, double lon, double trk) {
		Matrix3x3 rz = Matrix3x3.rotateZ(-lon);
		Matrix3x3 ry = Matrix3x3.rotateY(lat);
		Matrix3x3 rx = Matrix3x3.rotateX(trk);
		Matrix3x3 c = rz.mult(ry).mult(rx);
		return c.col(2); 
	}

	/** 
	 * Find the time of horizontal closest point of approach for two craft moving along great circles.  This
	 * method is based on mathematics in <i>Some Tactical Algorithms for Spherical Geometry</i> by Rex Shudde 
	 * (Monterey Naval Postgraduate School NPS55-86-008, 1986).  This algorithm relies on numerical techniques
	 * so an initial estimate for the time (see parameter <i>t_estimate</i>) must be provided and a convergence
	 * criteria (see parameter <i>epsilon</i>) must be provided.<p>
	 * 
	 * @param lla1 position of first aircraft
	 * @param v1 velocity of first aircraft
	 * @param lla2 position of second aircraft
	 * @param v2 velocity of second aircraft
	 * @param t_estimate initial estimate for a convergence time.  We have had good luck with 0.0.  
	 * @param epsilon convergence criteria, or how close do you want the value returned to be to the actual result?  The smaller this
	 *    value, the longer the compute time.  
	 * @return time of closest point of approach.  The time returned is relative to the time of the two 
	 *    initial positions.  The time returned may be negative value (indicating they've already passed CPA) 
	 *    or NaN (indicating the numerical approximation technique is not converging).
	 */
	public static double time_cpa(LatLonAlt lla1, Velocity v1, LatLonAlt lla2, Velocity v2, double t_estimate, double epsilon) {
		double avgt = t_estimate;
		double gs1 = v1.gs();
		double gs2 = v2.gs();
		double w1 = gs1/spherical_earth_radius; // omega for aircraft 1
		double w2 = gs2/spherical_earth_radius; // omega for aircraft 2
		
		// if the two positions are nearly identical, 
		// or both groundspeeds are zero, 
		// or they are on the same speed and course,
		// the time of closest point is now
		double gs_epsilon = 0.001;	// within 1mm/sec
		double trk_epsilon = Units.from("deg", 0.001);	// within one thousandth of a degree
		if (almost_equals(lla1.lat(), lla1.lon(), lla2.lat(), lla2.lon()) || // overlapping points
				(Util.within_epsilon(0.0, gs1, gs_epsilon) && Util.within_epsilon(0.0, gs2, gs_epsilon)) || // ground speed zero
				(Util.within_epsilon(gs2, gs1, gs_epsilon) && Util.within_epsilon(v1.trk(), v2.trk(), trk_epsilon) && collinear(lla1, v1, lla2))) { // same velocities and on same great circle
			return 0.0;
		}
		
		// vectors representing points on unit sphere
		Vect3 x1_0 = GreatCircle.unit_spherical2ecef(lla1.lat(), lla1.lon()); 
		Vect3 x2_0 = GreatCircle.unit_spherical2ecef(lla2.lat(), lla2.lon()); 

		// initial course vectors for each aircraft
		Vect3 c1_0 = course_vector(lla1.lat(), lla1.lon(), v1.compassAngle()); 
		Vect3 c2_0 = course_vector(lla2.lat(), lla2.lon(), v2.compassAngle()); 

		return spherical_newton_raphson(avgt, epsilon, w1, w2, c1_0, c2_0, x1_0, x2_0);
	}


}
