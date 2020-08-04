/* A single position represented in either Euclidean or Lat/Lon coordinates
 * 
 * Authors:  George Hagen              NASA Langley Research Center  
 *           Ricky Butler              NASA Langley Research Center
 *           Jeff Maddalon             NASA Langley Research Center
 *
 *
 * Copyright (c) 2011-2018 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * This class captures a single position represented in either Euclidean or Lat/Lon coordinates.  
 * This class is immutable.<p>
 * 
 * This class is designed to be resilient; The methods do not return errors when a position in the "wrong" geometry 
 * is provided.  So the method x() will return a value even when the original position was provided in LatLonAlt.  
 * The correspondence is as follows:
 * <ul>
 * <li> latitude corresponds to Y
 * <li> longitude corresponds to X
 * <li> altitude corresponds to altitude (obviously)
 * </ul>
 *  
 */
public final class Position implements OutputList {
	// This class is a disjoint union on LatLonAlt and Point
	private final boolean latlon;    // indicates whether point is geodesic or Euclidean
	private final LatLonAlt ll;      // only valid if latlon = true
	private final Point s3;          // only valid if latlon = false

	/** Construct a new Position object from a LatLonAlt object. The position will be a Lat/Lon position. 
	 * 
	 * @param lla a latitude/longitude/altitude object
	 * */
	public Position(LatLonAlt lla) {
		ll = lla;
		s3 = Point.mk(lla.lon(), lla.lat(), lla.alt());
		this.latlon = true;
	}

	private Position(double x, double y, double z) {
		s3 = Point.mk(x, y, z);
		ll = LatLonAlt.mk(y, x, z);
		this.latlon = false;
	}

	/** Construct a new Position object from a Vect3 object. This method
	 * assumes the Vect3 is in internal units. 
	 * @param v three dimensional vector
	 */
	public Position(Vect3 v) {
		s3 = Point.mk(v);
		ll = LatLonAlt.mk(v.y, v.x, v.z);
		this.latlon = false;
	}

	/** Construct a new Position object from a Point object.  The position will be a Euclidean position. 
	 * @param v a Point object
	 * */
	public Position(Point v) {
		s3 = v;
		ll = LatLonAlt.mk(v.y, v.x, v.z);
		this.latlon = false;
	}

	/** Copy this Position object 
	 * @return a copy of this Position object
	 * */
	public Position copy() {
		if (latlon) return new Position(ll);
		else return new Position(s3);
	}

	/** A zero position in lat/lon */
	public static final Position ZERO_LL = new Position(LatLonAlt.ZERO);
	/** A zero position in Euclidean */
	public static final Position ZERO_XYZ = new Position(Point.mk(Vect3.Zero()));  
	/** An invalid position.  Note that this is not necessarily equal to other invalid positions -- use the isInvalid() test instead. */
	public static final Position INVALID = new Position(Point.mk(Vect3.INVALID));

	public static final double minDist = 1E-9;    // TODO:  this should probably go into GreatCircle.initial_course, and should probably be increased, as something like 4E-9 causes problems in PlanTest.test_UAM_nel3

	/**
	 * Creates a new lat/lon position with coordinates (<code>lat</code>,<code>lon</code>,<code>alt</code>).
	 * 
	 * @param lat latitude [deg north latitude]
	 * @param lon longitude [deg east longitude]
	 * @param alt altitude [ft]
	 * @return new position
	 */
	public static Position makeLatLonAlt(double lat, double lon, double alt) {
		return new Position(LatLonAlt.make(lat,lon,alt));
	}


	/**
	 * Creates a new lat/lon position with coordinates (<code>lat</code>,<code>lon</code>,<code>alt</code>).
	 * 
	 * @param lat latitude [radians]
	 * @param lon longitude [radians]
	 * @param alt altitude [m]
	 * @return new position
	 */
	public static Position mkLatLonAlt(double lat, double lon, double alt) {
		return new Position(LatLonAlt.mk(lat,lon,alt));
	}


	/**
	 * Creates a new lat/lon position with coordinates (<code>lat</code>,<code>lon</code>,<code>alt</code>).
	 * 
	 * @param lat latitude [lat_unit north latitude]
	 * @param lat_unit units of latitude
	 * @param lon longitude [lon_unit east longitude]
	 * @param lon_unit units of latitude
	 * @param alt altitude [alt_unit]
	 * @param alt_unit units of altitude
	 * @return new position
	 */
	public static Position makeLatLonAlt(double lat, String lat_unit, double lon, String lon_unit, double alt, String alt_unit) {
		return new Position(LatLonAlt.make(lat, lat_unit, lon, lon_unit, alt, alt_unit));
	}

	/**
	 * Creates a new Euclidean position with coordinates (<code>x</code>,<code>y</code>,<code>z</code>).
	 * 
	 * @param x coordinate [nmi]
	 * @param y coordinate [nmi]
	 * @param z altitude [ft]
	 * @return new position
	 */
	public static Position makeXYZ(double x, double y, double z) {
		return new Position(Units.from(Units.NM, x), Units.from(Units.NM, y), Units.from(Units.ft,z));
	}


	/**
	 * Creates a new Euclidean position with coordinates (<code>x</code>,<code>y</code>,<code>z</code>).
	 * 
	 * @param x coordinate [m]
	 * @param y coordinate [m]
	 * @param z altitude [m]
	 * @return new position
	 */
	public static Position mkXYZ(double x, double y, double z) {
		return new Position(x,y,z);
	}

	/**
	 * Creates a new Euclidean position with coordinates (<code>x</code>,<code>y</code>,<code>z</code>).
	 * 
	 * @param x coordinate [x_unit]
	 * @param x_unit units of x coordinate
	 * @param y coordinate [y_unit]
	 * @param y_unit units of y coordinate
	 * @param z altitude [z_unit]
	 * @param z_unit units of z coordinate
	 * @return new position
	 */
	public static Position makeXYZ(double x, double x_unit, double y, double y_unit, double z, double z_unit) {
		return new Position(Units.from(x_unit, x), Units.from(y_unit, y), Units.from(z_unit,z));
	}

	/**
	 * Creates a new Euclidean position with coordinates (<code>x</code>,<code>y</code>,<code>z</code>).
	 * 
	 * @param x coordinate [x_unit]
	 * @param x_unit units of x coordinate
	 * @param y coordinate [y_unit]
	 * @param y_unit units of y coordinate
	 * @param z altitude [z_unit]
	 * @param z_unit units of z coordinate
	 * @return new position
	 */
	public static Position makeXYZ(double x, String x_unit, double y, String y_unit, double z, String z_unit) {
		return new Position(Units.from(x_unit, x), Units.from(y_unit, y), Units.from(z_unit,z));
	}


	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (latlon ? 1231 : 1237);
		result = prime * result + ((ll == null) ? 0 : ll.hashCode());
		result = prime * result + ((s3 == null) ? 0 : s3.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		//f.pln(" $$$ equals: this = "+this+" obj = "+obj);
		//Debug.halt();
		if (this == INVALID) return false; // because C++ conventions for testing NaN
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Position other = (Position) obj;
		if (latlon != other.latlon)
			return false;
		if (ll == null) {
			if (other.ll != null)
				return false;
		} else if (!ll.equals(other.ll))
			return false;
		if (s3 == null) {
			if (other.s3 != null)
				return false;
		} else if (!s3.equals(other.s3))
			return false;
		return true;
	}

	/**
	 * Checks if two Positions are almost the same.
	 * This is based on values in gov.nasa.larcfm.Constants
	 * 
	 * @param pp Position for comparison
	 * 
	 * @return <code>true</code>, if <code>this</code> Position is almost equal 
	 * to <code>v</code>.
	 */
	public boolean almostEquals(Position pp) {
		if (latlon) {
			return GreatCircle.almostEquals(lla(),pp.lla());
		} else
			return Constants.almost_equals_xy(s3.x,s3.y,pp.s3.x,pp.s3.y) 
					&& Constants.almost_equals_alt(s3.z,pp.s3.z);
	}


	/**
	 * Checks if two Positions are almost the same.
	 * 
	 * @param pp Position for comparison
	 * @param epsilon_horiz allowable horizontal deviation [m] 
	 * @param epsilon_vert allowable vertical deviation [m] 
	 * 
	 * @return <code>true</code>, if <code>this</code> Position is almost equal 
	 * to <code>v</code>.
	 */
	public boolean almostEquals(Position pp, double epsilon_horiz, double epsilon_vert) {
		if (latlon) {
			return GreatCircle.almostEquals(lla(), pp.lla(),epsilon_horiz,epsilon_vert);
		} else {
			return s3.within_epsilon(pp.vect3(),epsilon_vert);
		}
	}


	public boolean almostEquals2D(Position pp, double epsilon_horiz) {
		if (latlon) {
			return GreatCircle.almostEquals2D(lla(), pp.lla(), epsilon_horiz);
		} else {
			return s3.almostEquals2D(pp.vect3(),epsilon_horiz);
		}
	}


	/** Return the horizontal position as a standard vect2().  This returns either (x,y), or, equivalently, (lon, lat). 
	 * @return 2D vector
	 * */
	public Vect2 vect2() {
		return new Vect2(s3.x,s3.y);
	}

	/** Return the three dimensional position vector.  This returns either (x,y,z), or, equivalently, (lon,lat,alt). 
	 * @return 3D vector
	 * */
	public Point vect3() {
		return s3;
	}

	/** Return the associated LatLonAlt object 
	 * @return LatLonAlt object
	 * */
	public LatLonAlt lla() {
		return ll;
	}

	/** Return the x coordinate 
	 * @return x coordinate
	 * */
	public double x() {
		return s3.x;
	}

	/** Return the y coordinate 
	 * @return y coordinate
	 * */
	public double y() {
		return s3.y;
	}

	/** Return the z coordinate 
	 * @return z coordinate
	 * */
	public double z() {
		return s3.z;
	}

	/** Return the latitude 
	 * @return latitude [internal]
	 * */
	public double lat() {
		return ll.lat();
	}

	/** Return the longitude 
	 * @return longitude [internal]
	 * */
	public double lon() {
		return ll.lon();
	}

	/** Return the altitude
	 * @return altitude [internal] */
	public double alt() {
		return ll.alt();
	}

	/** Return the latitude in degrees north 
	 * @return latitude
	 * */
	public double latitude() {
		return ll.latitude();
	}

	/** Return the longitude in degrees east 
	 * @return longitude
	 * */
	public double longitude() {
		return ll.longitude();
	}

	/** Return the altitude in feet 
	 * @return altitude [ft]
	 * */
	public double altitude() {
		return ll.altitude();
	}

	/** Return the x coordinate in [NM] 
	 * @return x coordinate [NM]
	 * */ 
	public double xCoordinate() {
		return Units.to(Units.NM, s3.x);
	}

	/** Return the y coordinate in [NM] 
	 * @return y coordinate [NM]
	 * */
	public double yCoordinate() {
		return Units.to(Units.NM, s3.y);
	}

	/** Return the z coordinate in [ft] 
	 * @return z coordinate [ft]
	 * */
	public double zCoordinate() {
		return Units.to(Units.ft, s3.z);
	}


	/** Return if this Position is a latitude or longitude 
	 * @return true if latitude/longitude
	 * */
	public boolean isLatLon() {
		return latlon;
	}

	/** Returns true if this Position is invalid 
	 * @return true, if invalid position
	 * */
	public boolean isInvalid() {
		return s3.isInvalid() || ll.isInvalid();
	}

	/** Make a new Position from the current one with the X coordinate changed 
	 * @param x new x coordinate
	 * @return a new Position
	 * */
	public Position mkX(double x) {
		if (latlon) {
			return new Position(LatLonAlt.mk(ll.lat(), x, ll.alt()));
		} else {
			return new Position(x, s3.y, s3.z);
		}
	}

	/** Make a new Position from the current one with the longitude changed 
	 * @param lon new longitude value
	 * @return a new Position
	 */
	public Position mkLon(double lon) {
		return mkX(lon);
	}

	/** Make a new Position from the current one with the Y coordinate changed 
	 * @param y new y coordinate
	 * @return a new Position
	 */
	public Position mkY(double y) {
		if (latlon) {
			return new Position(LatLonAlt.mk(y, ll.lon(), ll.alt()));
		} else {
			return new Position(s3.x, y, s3.z);
		}
	}

	/** Make a new Position from the current one with the latitude changed 
	 * @param lat new latitude value
	 * @return a new Position
	 */
	public Position mkLat(double lat) {
		return mkY(lat);
	}

	/** Make a new Position from the current one with the Z coordinate changed 
	 * @param z new Z coordinate
	 * @return a new Position
	 * */
	public Position mkZ(double z) {
		if (latlon) {
			return new Position(LatLonAlt.mk(ll.lat(), ll.lon(), z));
		} else {
			return new Position(s3.x, s3.y, z);
		}
	}

	/** Make a new Position from the current one with the altitude changed (internal units) 
	 * @param alt new altitude value
	 * @return a new Position
	 */
	public Position mkAlt(double alt) {
		return mkZ(alt);
	}

	/** Make a new Position from the current one with an altitude of zero 
	 * @return a new Position
	 */
	public Position zeroAlt() {
		return mkZ(0);
	}

	/** Return the horizontal distance between the current Position and the given Position
	 * @param p another position 
	 * @return horizontal distance
	 * */
	public double distanceH(Position p) {
		if (latlon) {
			return GreatCircle.distance(ll,p.ll);
		} else {
			return s3.vect2().Sub(p.vect2()).norm(); 
		}
	}

	/** Return the vertical distance between the current Position and the given Position. 
	 * 
	 * @param p another position
	 * @return vertical distance (absolute distance)
	 */
	public double distanceV(Position p) {
		return Math.abs(s3.z - p.s3.z);
	}

	/** Return the vertical distance between the current Position and the given Position. Positive values mean 
	 * the current Position is above the given Position 
	 * 
	 * @param p another position
	 * @return vertical distance (positive values mean current position is above the given position
	 */
	public double signedDistanceV(Position p) {
		return s3.z - p.s3.z;
	}

	//  /**
	//   * Return a quickly calculated approximation for the distance between two points.  
	//   * This assumes the area in question is fairly small and does not overlap with a pole.
	//   * @param p second point
	//   * @param d distance to compare to.
	//   * @return
	//   */
	//  public double distanceHApprox(Position p) {
	//	  if (isLatLon()) {
	//		  double raddist = 6360000; // m per degree
	//          double latdiff = (lla().lat() - p.lla().lat());
	//          double londiff = (lla().lon() - p.lla().lon())*Math.cos(p.lla().lat());
	//          return raddist*Math.sqrt(latdiff*latdiff + londiff*londiff);
	//	  }
	//	  return distanceH(p);
	//  }

	/** 
	 * Perform a linear projection of the current Position with given velocity and time.  
	 * If isLatLon() is true, then a great circle route is followed and the velocity 
	 * represents the initial velocity along the great circle.
	 * 
	 * Reminder: If this is used in a stepwise fashion over lat/lon, be careful when passing 
	 * over or near the poles and keep the velocity track in mind.
	 * 
	 *  @param v the velocity
	 *  @param time the time from the current point
	 *  Note: using a negative time value is the same a velocity moving in the opposite direction (along the great circle, if appropriate)
	 * @return linear projection of the position
	 */
	public Position linear(Velocity v, double time) {
		if (time == 0 || v.isZero()) {
			return latlon ? new Position(ll) : new Position(s3);
		}
		if (latlon) {
			return new Position(GreatCircle.linear_initial(ll,v,time));
		} else {
			return new Position(s3.linear(v, time)); 
		}
	}

	/**
	 * Calculate a new position that is offset from the current position by (dn,de)
	 * @param dn  offset in north direction (m)
	 * @param de  offset in east direction  (m)
	 * @return    linear projection of the position
	 */
	public Position linearEst(double dn, double de) {
		Position newNP;
		if (latlon) {
			newNP = new Position(lla().linearEst(dn,de));		
		} else {
			return new Position(Point.mk(s3.x + de, s3.y + dn, s3.z));
		}
		return newNP;
	}

	/**
	 * Returns a new position in the given direction at the given distance from this point 
	 * The altitude is not changed!
	 * 
	 * @param track   track
	 * @param d       distance
	 * @return        position 
	 */
	public Position linearDist2D(double track, double d) {
		if (latlon) {
			LatLonAlt sEnd = GreatCircle.linear_initial(ll,track,d);
			return new Position(sEnd);
		} else {
			Vect3 sEnd = s3.linearByDist2D(track, d);
			return new Position(sEnd);
		}
	}



	//  /** 
	//   * This computes the horizontal (2D) position that is a distance (d) away from
	//   * the this position along the track angle from the given velocity (v). 
	//   * Neither the ground speed nor the vertical speed of the returned velocity is computed.
	//   * 
	//   * @param v    velocity (only track angle is used)
	//   * @param d    distance
	//   * @return pair representing the position and velocity
	//   */
	//  public Pair<Position,Velocity> linearDist2D(Velocity v, double d) {
	//	  double track = v.trk();
	//	  //f.pln(" $$$$$$+++++++++++++++++++++++++ ENTER linearDist: v = "+v);
	//	  if (latlon) {
	//		  LatLonAlt sEnd = GreatCircle.linear_initial(ll,track,d);
	//		  //sEnd = sEnd.mkAlt(altAtd);
	//		  double finalTrk = track;
	//		  if (d > minDist) {  // final course has problems if no distance between points (USE 1E-9), 1E-10 NOT GOOD
	//		     finalTrk = GreatCircle.final_course(ll, sEnd);
	//		  }
	//		  //f.pln(" $$$$$$+++++++++++++++++++++++++++ linearDist: v = "+v+" finalTrk = "+Units.str("deg",finalTrk)+" d = "+Units.str("ft",d));
	//		  Velocity vEnd = v.mkTrk(finalTrk);
	//		  //f.pln(" $$$$$$+++++++++++++++++++++++++++ linearDist: vEnd = "+vEnd);
	//		  return new Pair<Position,Velocity>(new Position(sEnd),vEnd);
	//	  } else {
	//		  //Velocity vEnd = Velocity.mkTrkGsVs(track,fakeGs,0.0);
	//		  //f.pln(" $$$$$$$$ linearDist: v = "+v);
	//		  Vect3 sNew = s3.linearByDist2D(track,d);
	//		  //sNew = sNew.mkZ(altAtd);
	//		  return new Pair<Position,Velocity>(new Position(sNew),v); 
	//	  }
	//  }  

	/** 
	 * This computes the horizontal (2D) position that is a distance (d) away from
	 * this position along the track angle from the given track. 
	 * Neither the ground speed nor the vertical speed of the returned velocity is computed.
	 * 
	 * @param track    track angle at start
	 * @param d        distance
	 * @param gsAt_d   ground speed at the end 
	 * @return pair    representing the 2D position and 2D velocity
	 */
	public Pair<Position,Velocity> linearDist2D(double track, double d, double gsAt_d) {
		//f.pln("$$ Position.linearDist2D: track="+Util.to_2pi(track)+" d="+d+" gsAt_d="+gsAt_d);		
		//f.pln(" $$$$$$+++++++++++++++++++++++++ ENTER linearDist: v = "+v);
		if (latlon) {
			LatLonAlt sNew = lla();
			//f.pln("$$ Position.linearDist2D: this="+this.lat()+" "+this.lon()+" snew="+sNew.lat()+" "+sNew.lon());			
			double finalTrk = track;
			if (d > minDist) {  // final course has problems if no distance between points (USE 1E-9), 1E-10 NOT GOOD -- should probably be increased
				sNew = GreatCircle.linear_initial(ll,track,d);
				finalTrk = GreatCircle.final_course(ll, sNew); // we use final course here because we are advancing to the new point
			}
			Velocity vNew = Velocity.mkTrkGsVs(finalTrk,gsAt_d,0.0);
			//f.pln("$$ Position.linearDist2D: vNew = "+vNew+" finalTrk = "+Util.to_2pi(finalTrk)+" d = "+Units.str("ft",d));
			return new Pair<Position,Velocity>(new Position(sNew),vNew);
		} else {
			Vect3 sNew = s3.linearByDist2D(track,d);
			Velocity vNew = Velocity.mkTrkGsVs(track,gsAt_d,0.0);
			return new Pair<Position,Velocity>(new Position(sNew),vNew); 
		}
	}

	/**
	 * Perform a estimation of a linear projection of the current Position with the given velocity and time.
	 * @param vo the velocity
	 * @param time the time from the current point
	 * @return linear projection of the position
	 */
	public Position linearEst(Velocity vo, double time) {
		Position newNP;
		if (latlon) {
			if (lat() > Units.from("deg",85) || lat() < Units.from("deg",-85)) {
				newNP = new Position (GreatCircle.linear_initial(ll,vo,time));
			} else {
				newNP = new Position(lla().linearEst(vo,time));
			}
		} else {
			newNP = linear(vo,time);
		}
		return newNP;
	}


	//  public Position linearEstPerp(Velocity vo, double dist) {
	//    double t = dist/vo.gs();
	//    Velocity voPerp = Velocity.make(vo.PerpR());
	//    return linearEst(voPerp,t);
	//  }



	/**
	 * Return the mid point between the current position and the given position
	 * @param p2 the other position
	 * @return the midpoint
	 */
	public Position midPoint(Position p2) {
		if (latlon) {
			return new Position(GreatCircle.interpolate(ll,p2.lla(),0.5));
		} else {
			return new Position(Point.mk(VectFuns.midPoint(s3,Point.mk(p2.vect3())))); 
		}
	}

	public Position interpolate(Position p2, double f) {
		if (latlon) {
			return new Position(GreatCircle.interpolate(ll,p2.lla(),f));
		} else {
			return new Position(Point.mk(VectFuns.interpolate(s3,Point.mk(p2.vect3()),f))); 
		}
	}

	public Position interpolateEst(Position p2, double f) {
		if (latlon) {
			return new Position(GreatCircle.interpolate(ll,p2.lla(),f));
		} else {
			return new Position(Point.mk(VectFuns.interpolate(s3,Point.mk(p2.vect3()),f))); 
		}
	}


	/** Return the track angle of the vector from the current Position to the given Position, based on initial course 
	 * @param p another position
	 * @return track angle within [0..2PI]
	 * */
	public double track(Position p) {  
		Debug.checkError(p.latlon == latlon, "Position.track call given an inconsistent argument: "+toString()+" "+p.toString());	
		if (latlon) {
			return GreatCircle.initial_course(ll,p.ll);
		} else {
			Vect2 v = p.s3.Sub(s3).vect2();
			return v.compassAngle();
		}
	}

	/** return the velocity going from this to p over dt seconds.
	 * Returns a ZERO velocity if dt &lt;= 0 
	 * @param p another position
	 * @param dt delta time
	 * @return velocity
	 * */
	public Velocity initialVelocity(Position p, double dt) {
		if (dt<=0) {
			return Velocity.ZERO;
		} else {			
			if (isLatLon()) {
				return GreatCircle.velocity_initial(lla(), p.lla(), dt);
			} else {
				return Velocity.make((p.vect3().Sub(vect3())).Scal(1.0/dt));
			}
		}
	}

	/** return the velocity going from this to p over dt seconds.
	 * Returns a ZERO velocity if dt &lt;= 0 
	 * @param p another position
	 * @param dt delta time
	 * @return velocity
	 * */
	public Velocity finalVelocity(Position p, double dt) {
		if (dt<=0) {
			return Velocity.ZERO;
		} else {			
			if (isLatLon()) {
				return GreatCircle.velocity_final(lla(), p.lla(), dt);
			} else {
				return Velocity.make((p.vect3().Sub(vect3())).Scal(1.0/dt));
			}
		}
	}


	/** Return the track angle of the vector from the current Position to the given Position, based on representative course 
	 * @param p another position
	 * @return representative course
	 * */
	public double representativeTrack(Position p) {  
		Debug.checkError(p.latlon == latlon, "Position.representativeTrack call given an inconsistent argument.");	
		if (latlon) {
			return GreatCircle.representative_course(ll,p.ll);
		} else {
			Vect2 v = p.s3.Sub(s3).vect2();
			return v.compassAngle();
		}
	}

	/** Given two great circles defined by so, vo and si, vi return the intersection point that is closest to so.
	 *  
	 * @param so           first point of line o 
	 * @param vo           velocity from point so
	 * @param si           first point of line i
	 * @param vi           velocity from point si
	 * @return Position and time of intersection. The returned altitude is so.alt().
	 * Note: a negative time indicates that the intersection occurred in the past (relative to directions of travel of so1)
	 */
	public static Pair<Position,Double> intersection(Position so, Velocity vo, Position si, Velocity vi) {
		if (so.latlon != si.latlon) {
			Debug.error("Position.intersection call was given an inconsistent argument.");	
			return new Pair<Position,Double>(Position.INVALID,-1.0);
		}
		if (so.latlon) {
			Pair<LatLonAlt,Double> pgc = GreatCircle.intersection(so.lla(),vo, si.lla(),vi);
			return new Pair<Position,Double>(new Position(pgc.first),pgc.second );
		} else {
			Pair<Vect3,Double> pvt = VectFuns.intersection(so.vect3(),vo,si.vect3(),vi);
			return new Pair<Position,Double>(new Position(pvt.first),pvt.second );
		}
	}

	/** Returns intersection point 
	 * @param so     first point of infinite line A 
	 * @param so2    second point of infinite line A 
	 * @param si     first point of infinite line B
	 * @param si2    second point of infinite line B 
	 * @return       the intersection point,  if parallel returns INVALID
	 */
	public static Position intersection2D(Position so, Position so2, Position si, Position si2) {
		if (so.latlon != si.latlon && so2.latlon != si2.latlon && so.latlon != so2.latlon) {
			Debug.error("Position.intersection call was given an inconsistent argument.");	
			return Position.INVALID;
		}
		if (so.latlon) {
			LatLonAlt lgc = GreatCircle.intersection(so.lla(),so2.lla(), si.lla(), si2.lla());
			return new Position(lgc);
		} else {
			Pair<Vect2,Double> pvt = VectFuns.intersection2D(so.vect2(),so2.vect2(),1.0,si.vect2(),si2.vect2());
			Vect3 p3 = new Vect3(pvt.first,so.z());
			return new Position(p3);
		}
	}

	/** Returns intersection point 
	 * @param so     first point of segment A 
	 * @param so2    second point of segment A 
	 * @param si     first point of segment B
	 * @param si2    second point of segment B 
	 * @return       the intersection point,  if parallel returns INVALID
	 */
	public static Position intersectSegments2D(Position so, Position so2, Position si, Position si2) {
		if (so.latlon != si.latlon && so2.latlon != si2.latlon && so.latlon != so2.latlon) {
			Debug.error("Position.intersection call was given an inconsistent argument.");	
			return Position.INVALID;
		}
		if (so.latlon) {
			LatLonAlt interSec = GreatCircle.intersectSegments(so.lla(), so2.lla(), si.lla(), si2.lla());
			return new Position(interSec);
		} else {
			Pair<Vect2,Double> pvt = VectFuns.intersectSegments(so.vect2(),so2.vect2(),si.vect2(),si2.vect2());
			Vect3 p3 = new Vect3(pvt.first,so.z());
			return new Position(p3);
		}
	}

	/** EXPERIMENTAL -- does not work for large T values or polygons near the poles
	 * 
	 *  Returns "relative" intersection of aircraft and moving segment, that is the place on the polygon
	 *  where the intersection will occur.  This is not the location in space/time where the actual
	 *  intersection will occur.   To obtain that position, a translation back to the original coordinate
	 *  system would be needed.  This is not done in this method to make the check for intersection
	 *  more efficient.
	 *  
	 *  Note.  If large values of T are used, in the Lat/Lon mode, these paths could circum-navigate the earth
	 *  causes inconsistencies.  The method issues a warning if the path length exceed R*PI
	 *  
	 * @param so     current position of ownship
	 * @param vo     velocity of ownship
	 * @param si     end point of segment 
	 * @param si2    end point of segment
	 * @param vi     velocity of segment
	 * @param T      lookahead time:  intersections further than this are ignored (important for LatLon)
	 * @return       the "relative" intersection point,  if parallel returns INVALID
	 */
	public static Position intersectionMovingSegment(Position so, Velocity vo, Position si, 
			Position si2, Velocity vi, double T) {
		//f.pln("\n $$ intersectionMovingSegment: vo = "+vo+" vi = "+vi+" T = "+T);	  
		if (so.isLatLon()) {
			Position so2 = so.linear(vo,T);
			double angular_distance = GreatCircle.angular_distance(so.lla(),so2.lla());
			//f.pln(" $$$ angular_distance = "+Units.str("deg",angular_distance));
			if (angular_distance > Math.PI/2) {
				f.p(" !!!! Position.intersectMovingSegment: WARNING, T = "+T+" is too large");
				f.pln(" --> angular_distance = "+Units.str("deg",angular_distance));
			}
		}  
		Velocity relVel = vo.Sub(vi);
		Position so2 = so.linear(relVel,T);	  
		//Plan debug1 = Plan.make(so,so2,Units.from("kn",250));
		//Plan debug2 = Plan.make(si,si2,Units.from("kn",250));
		//DebugSupport.dumpPlan(debug1,"debug1");
		//DebugSupport.dumpPlan(debug2,"debug2");
		Position rtn = intersectSegments2D(so,so2,si,si2);
		//f.pln(" $$$$ intersectionMovingSegment: si = "+si+" rtn = "+rtn);
		return rtn;
	}


	public static boolean intersectsMovingSegment(Position so, Velocity vo, Position si, 
			Position si2, Velocity vi) {
		double T = Units.from("hr",10.0);
		return ! intersectionMovingSegment(so,vo,si,si2,vi,T).isInvalid();
	}


	/** Returns intersection point and time of intersection relative to the time of position so
	 *  for time return value, it assumes that an aircraft travels from so1 to so2 in dto seconds and the other aircraft from si to si2
	 *  a negative time indicates that the intersection occurred in the past (relative to directions of travel of so1). 
	 *  This method computes an altitude which is the average of the altitudes of the nearest points.
	 * @param so     first point of line A 
	 * @param so2    second point of line A 
	 * @param dto    the delta time between point so and point so2.
	 * @param si     first point of line B
	 * @param si2    second point of line B 
	 * @return a pair: intersection point and the delta time from point "so" to the intersection, can be negative if intersect
	 *                 point is in the past
	 *                if intersection point is invalid then the returned delta time is -1
	 */
	public static Pair<Position,Double> intersection(Position so, Position so2, double dto, Position si, Position si2) {
		if (so.latlon != si.latlon && so2.latlon != si2.latlon && so.latlon != so2.latlon) {
			Debug.error("Position.intersection call was given an inconsistent argument.");	
			return new Pair<Position,Double>(Position.INVALID,-1.0);
		}
		if (so.latlon) {
			Pair<LatLonAlt,Double> lgc = GreatCircle.intersectionAvgAlt(so.lla(),so2.lla(), dto, si.lla(), si2.lla());
			return new Pair<Position,Double>(new Position(lgc.first),lgc.second);
		} else {
			Pair<Vect3,Double> pvt = VectFuns.intersectionAvgZ(so.vect3(),so2.vect3(),dto,si.vect3(),si2.vect3());
			return new Pair<Position,Double>(new Position(pvt.first),pvt.second );
		}
	}


	/**
	 * This function considers the line from p1 to p2 and computes 
	 * the shortest distance (i.e. perpendicular) of another point (offCircle) to that line.  This is the 
	 * cross track distance.
	 *  
	 * @param p1 the starting point of the line
	 * @param p2 another point on the line
	 * @param offCircle the point through which the perpendicular distance is desired
	 * @return the cross track distance [m]
	 */
	public static double perp_distance(Position p1, Position p2, Position offCircle) {
		if (p1.latlon != p2.latlon) {
			Debug.error("Position.perp_distance call was given inconsistent arguments.");	
			return -1;
		}
		if (p1.isLatLon()) {
			return Math.abs(GreatCircle.cross_track_distance(p1.lla(), p2.lla(), offCircle.lla()));
		} else {
			Vect2 v = p2.vect2().Sub(p1.vect2());
			return Vect2.distPerp(p1.vect2(), v, offCircle.vect2());
		}
	}



	/** Return the average velocity between the current position and the given position, with the given speed [internal units]. 
	 * 
	 * @param p2 another position
	 * @param speed the ground speed going from this Position to the given position
	 * @return velocity (3D)
	 */
	public Velocity averageVelocity(Position p2, double speed) {
		if (p2.latlon != latlon) {
			Debug.error("Position.averageVelocity call given an inconsistent argument.");	
			return Velocity.ZERO;
		}
		if (latlon) {
			return GreatCircle.velocity_average_speed(ll,p2.ll,speed);
		} else {
			return Velocity.mkVel(s3,p2.s3,speed);
		}
	}

	/** Return the average velocity between the current position and the given position, with the given delta time dt. 
	 * 
	 * @param p2 another position
	 * @param dt delta time
	 * @return average velocity
	 */
	public Velocity avgVelocity(Position p2, double dt) {
		if (p2.latlon != latlon) {
			Debug.error("Position.averageVelocity call given an inconsistent argument.");	
			return Velocity.ZERO;
		}
		if (latlon) {
			return GreatCircle.velocity_average(ll,p2.ll,dt);
		} else {
			return Velocity.genVel(s3,p2.s3,dt);
		}

	}

	/**
	 * Return true if this point is west of the given point (this.x &lt; a.x)
	 * @param a other point
	 * @return true if this.x &lt; a.x (or this.lon &lt; a.lon, accounting for wraparound)
	 */
	public boolean isWest(Position a) {
		if (isLatLon()) {
			return lla().isWest(a.lla());
		} else {
			return x() < a.x();
		}
	}

	/** Determine if a loss of separation has occurred (using either geodesic or Euclidean calculations)
	 * 
	 * @param p2 the position of the other aircraft
	 * @param D horizontal distance to specify loss of separation 
	 * @param H vertical distance to specify loss of separation
	 * @return true if there is a loss of separation
	 */
	public boolean LoS(Position p2, double D, double H) {
		if (p2.isInvalid()) return false;  
		if (p2.latlon != latlon) {
			Debug.error("Position.LoS call given an inconsistent argument: "+toString()+" "+p2.toString());	
			return false;
		}
		double distH = distanceH(p2);
		double distV = distanceV(p2);
		//f.pln ("distH "+distH+"  distV "+distV+" D "+D+" H "+H);
		return (distH < D && distV < H);
	}
	
	
	public String toUnitTest() {
		if (latlon) {
			return "Position.makeLatLonAlt("+ f.Fm12(Units.to("deg",lat()))
			       +", "+f.Fm16(Units.to("deg",lon()))+", "+f.Fm12(Units.to("ft",alt()))+")";
		} else {
			return "Position.makeXYZ("+(f.Fm8(Units.to("NM",x()))+", "+f.Fm8(Units.to("NM",y()))
			       +", "	+f.Fm8(Units.to("ft",z()))+")");
		}
	}

	public String toUnitTestSI() {
		if (latlon) {
			return "Position.mkLatLonAlt("+ f.Fm12(lat())+", "+f.Fm12(lon())+", "+f.Fm12(alt())+")";
		} else {
			return "Position.mkXYZ("+(f.Fm8(x())+", "+f.Fm8(y())+", "+f.Fm8(z())+")");
		}
	}


	/** Return a string representation */
	public String toString() {
		return toString(Constants.get_output_precision());
	}

	/** Return a string representation 
	 * @param prec digits of precision
	 * @return string representation
	 * */
	public String toString(int prec) {
		if (latlon) {
			return "("+Units.str("deg",ll.lat(),prec)+", "+Units.str("deg",ll.lon(),prec)+", "+Units.str("ft",ll.alt(),prec)+")";
		} else {
			return "("+Units.str("NM",s3.x,prec)+", "+Units.str("NM",s3.y,prec)+", "+Units.str("ft",s3.z,prec)+")";
		}
	}

	/** Return a string representation 
	 * @param prec digits of precision
	 * @return string representation
	 * */
	public String toString2D(int prec) {
		if (latlon)
			return "("+Units.str("deg",ll.lat(),prec)+", "+Units.str("deg",ll.lon(),prec)+")";
		else
			return "("+Units.str("NM",s3.x,prec)+", "+Units.str("NM",s3.y,prec)+")";
	}



	/** Return a string representation 
	 * @param prec digits of precision
	 * @return string representation
	 * */
	public String toStringUnicode(int prec) {
		if (latlon) {
			return ll.toStringUnicode(prec);
		} else {
			return toString(prec); //"("+Units.str("NM",s3.x,prec)+", "+Units.str("NM",s3.y,prec)+", "+Units.str("ft",s3.z,prec)+")";
		}
	}

	/**
	 * Return a string representation using the given unit conversions (latitude and longitude, if 
	 * appropriate, are always in degrees, so only the z unit is used in that case)
	 * @return string representation
	 */
	public String toStringUnits() {
		return toStringUnits("NM", "NM", "ft");
	}

	/**
	 * Return a string representation using the given unit conversions (latitude and longitude, if appropriate, are always in degrees, 
	 * so only the z unit is used in that case).
	 * 
	 * @param xUnits units of x dimension
	 * @param yUnits units of y dimension
	 * @param zUnits units of z dimension
	 * @return string representation 
	 */
	public String toStringUnits(String xUnits, String yUnits, String zUnits) {
		if (latlon)
			return "("+Units.str("deg",ll.lat())+", "+Units.str("deg",ll.lon())+", "+Units.str(zUnits,ll.alt())+")";
		else
			return "("+Units.str(xUnits,s3.x)+", "+Units.str(yUnits,s3.y)+", "+Units.str(zUnits,s3.z)+")";
	}

	/** Return a string representation, with a user-specified digits of precision (0-15) without parentheses.
	 * @param precision digits of precision 
	 * @return string representation
	 * */
	public String toStringNP(int precision) {
		if (latlon) {
			return ll.toString(precision); // use internal units
		} else {
			return s3.toStringNP(precision);
		}
	}

	/** Return a string representation with a default precision but without parentheses. 
	 * @return string representation
	 * */
	public String toStringNP() {
		return toStringNP(Constants.get_output_precision());
	}

	/** Return a string representation, for Euclidean use the units [NM,NM,ft] and for LatLon use the units [deg,deg,ft] */
	public List<String> toStringList() {
		ArrayList<String> ret = new ArrayList<String>(3);
		if (isInvalid()) {
			ret.add("-");
			ret.add("-");
			ret.add("-");
		} else if (latlon) {
			ret.add(Double.toString(ll.latitude()));
			ret.add(Double.toString(ll.longitude()));
			ret.add(Double.toString(ll.altitude()));
		} else {
			ret.add(Double.toString(Units.to("NM",s3.x)));
			ret.add(Double.toString(Units.to("NM",s3.y)));
			ret.add(Double.toString(Units.to("ft",s3.z)));
		}
		return ret;
	}


	/** Return a string representation, for Euclidean use the units [NM,NM,ft] and for LatLon use the units [deg,deg,ft] 
	 * 
	 * @param precision the number of digits to display
	 * @return a string representation of the position
	 */
	public List<String> toStringList(int precision) {
		return toStringList(precision, false);
	}
	public List<String> toStringList(int precision, boolean internalUnits) {
		ArrayList<String> ret = new ArrayList<String>(3);
		if (isInvalid()) {
			ret.add("-");
			ret.add("-");
			ret.add("-");
		} else if (latlon) {
			if (internalUnits) {
				ret.add(f.FmPrecision(ll.lat(),precision));
				ret.add(f.FmPrecision(ll.lon(),precision));
				ret.add(f.FmPrecision(ll.alt(),precision));
			} else {
				ret.add(f.FmPrecision(ll.latitude(),precision));
				ret.add(f.FmPrecision(ll.longitude(),precision));
				ret.add(f.FmPrecision(ll.altitude(),precision));
			}
		} else {
			if (internalUnits) {
				ret.add(f.FmPrecision(s3.x,precision));
				ret.add(f.FmPrecision(s3.y,precision));
				ret.add(f.FmPrecision(s3.z,precision));
			} else {
				ret.add(f.FmPrecision(Units.to("NM",s3.x),precision));
				ret.add(f.FmPrecision(Units.to("NM",s3.y),precision));
				ret.add(f.FmPrecision(Units.to("ft",s3.z),precision));
			}
		}
		return ret;
	}


	/** This interprets a string as a LatLonAlt position with units in deg/deg/ft or the specified units (inverse of toString()) 
	 * 
	 * @param s string to parse
	 * @return position
	 */
	public static Position parseLL(String s) {
		return new Position(LatLonAlt.parse(s));
	}

	/** This interprets a string as a XYZ position with units in NM/NM/ft or the specified units (inverse of toString()) 
	 * 
	 * @param s string to parse
	 * @return position
	 */
	public static Position parseXYZ(String s) {
		Point v = Point.parse(s);
		return new Position(v);
	}

	/**
	 * This interprets a string into a LatLonAlt or XYZ position, if appropriate units are given.
	 * If no units are present, it returns an invalid Position.
	 * @param s string to parse
	 * @return position
	 */
	public static Position parse(String s) {
		String[] fields = s.split(Constants.wsPatternParens);
		if (fields[0].equals("")) {
			fields = Arrays.copyOfRange(fields,1,fields.length);
		}
		if (fields.length == 6) {
			if (Units.isCompatible(Units.clean(fields[1]), "deg") && Units.isCompatible(Units.clean(fields[3]), "deg") && Units.isCompatible(Units.clean(fields[5]), "ft")) return parseLL(s); // latlonalt
			if (Units.isCompatible(Units.clean(fields[1]), "m") && Units.isCompatible(Units.clean(fields[3]), "m") && Units.isCompatible(Units.clean(fields[5]), "m")) return parseXYZ(s); // Euclidean
		}
		return Position.INVALID;
	}
}
