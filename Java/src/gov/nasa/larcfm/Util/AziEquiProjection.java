/* AziEquiProjection
 * 
 * project spherical coordinates (lat/lon) into a Euclidean frame and the inverse.
 * 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;


/**
 * This class creates a local Euclidean projection around a given point.  This projection may be used to
 * transform geodesic coordinates (LatLonAlt objects) into this Euclidean frame, using the project() method.  Also points
 * within this frame, may be found in geodesic coordinates with the inverse() method.   As long as the points are
 * close to the projection point, the errors will be very small.
 * 
 * This is equivalent to a truncated azmuthal equidistant projection, and is similar to the ENU/tangent plane, 
 * but preserves distance from the reference point (along great circles), while distorting distances along great 
 * circles lines perpendicular to these.  This is the same basic projection used on the UN logo.
 * This version is truncated and so only projects over one hemisphere, giving a max range of 1/4 the (spherical) Earth's 
 * circumference.  
 * 
 * Note: projection objects should never be made directly, and instead should be retrieved via Projection.getProjection() 
 * 
 */
public final class AziEquiProjection implements EuclideanProjection {

    private final double projAlt;
 	private final Vect3 ref;
 	private final Vect3 refHat;
 	private final Vect3 refOrthoHat;
 	private final Vect3 refZMult;
 	private final LatLonAlt llaRef;
    
    /** Create a projection around the given reference point.
 	 * 
 	 * @param lla reference point
 	 */
    public AziEquiProjection(LatLonAlt lla) {
        projAlt = lla.alt();
        ref = spherical2xyz(lla.lat(), lla.lon());
        refHat = ref.Hat();
        refOrthoHat = vect3_orthog_toy(ref).Hat();
       	refZMult = refHat.cross(refOrthoHat);   // since refHat and refOrthoHat are both unit vectors, the cross is one too

        llaRef = lla;
    }
    
    /** Return a new projection with the given reference point */
    public EuclideanProjection makeNew(LatLonAlt lla) {
        return make(lla);
    }
 
    /** Return a new projection with the given reference point */
    public EuclideanProjection makeNew(double lat, double lon, double alt) {
        return mk(lat, lon, alt);
    }    
    
    public static EuclideanProjection make(LatLonAlt lla) {
    	return new AziEquiProjection(lla);
    }
    
    public static EuclideanProjection mk(double lat, double lon, double alt) {
    	return make(LatLonAlt.mk(lat,lon,alt));
    }
    
    public double conflictRange(double lat, double accuracy) {
//    	if (accuracy < Units.from(_NM, 0.01)) { //~0.001 nm accuracy
//    		return Units.from(_NM, 21); // 30?
//    	} else if (accuracy < Units.from(_NM, 0.1)) {	//0.01 nm accuracy
//    		return Units.from(_NM, 65); // 70?
//    	} else if (accuracy < Units.from(_NM, 0.5)) { //0.1 nm accuracy
//    		return Units.from(_NM, 150);
//    	} else {  //0.5 nm accuracy
//    		return Units.from(_NM, 260);
//    	}
    	// the following is based on a fitting of 1000000 random scenarios at each distance 1..500 NMI, with error at intersection
    	// (ownship is projection center)
    	double dist = Units.from("NM", Math.floor(329.2*Math.pow(Units.to("NM",accuracy),1.0/3.0)));
//    	if (dist < 10000) return 10000; // error shoud be < 0.01 m
    	return dist;
    	
    	// here is a new function:
    	// accuracy = 1E(-13.9)*distance_to_intercept^(2.968)  (in meters)
    	// (accurcy/1E-13.9)^(1/2.968) = distance
    	// distance = 

    	// curve fit for distance-to-accuracy function (from NM to NM) is 2.8*10^(-8)*x^3 - 2.4*10^(-7)*x^2 + 3*10^(-5)*x - 0.00078
    }
    
  
    // this seems to break horribly at distances greater than 3400 nmi
	/**
	 *  What is the maximum effective horizontal range of this projection? [m] 
	 */
    public double maxRange() {
    	return GreatCircle.spherical_earth_radius*Math.PI/2;
//    	return Units.from("nmi",2700.0);
    }
    
	/** Get the projection point for this projection */
    public LatLonAlt getProjectionPoint() {
      return llaRef;
    }
    
    /** Return a projection of a lat/lon(/alt) point in Euclidean 2-space */
    public Vect2 project2(LatLonAlt lla) {
    	Vect3 p2 = spherical2xyz(lla.lat(),lla.lon());
    	Vect2 p = new Vect2(refOrthoHat.dot(p2), -refZMult.dot(p2));
		
		double n = p.norm();
		if (n <= 0.0) {
			return Vect2.ZERO;
		} else {
			return p.Scal(GreatCircle.distance(lla, llaRef)/n); // scale into projection
		}
    }
    
    /** Return a projection of a lat/lon(/alt) point in Euclidean 3-space */
    public Vect3 project(LatLonAlt lla) {
        return new Vect3(project2(lla),lla.alt() - projAlt);
    }

    
    // does a projection if lat/lon else identity
    /** Return a projection of a Position in Euclidean 3-space (if already in Euclidian coordinate, this is the identity function) */
	public Vect3 project(Position sip) {
		Vect3 si;
		if (sip.isLatLon()) {
			si = project(sip.lla());
		} else {
			si = sip.vect3();
		}
		return si;
	}

    /** Return a LatLonAlt value corresponding to the given Euclidean position */
    public LatLonAlt inverse(Vect3 xyz) {
    	return inverse(xyz.vect2(), xyz.z);
    }


    /** Return a LatLonAlt value corresponding to the given Euclidean position */
    public LatLonAlt inverse(Vect2 xy, double alt) {
		double d = Math.sin(GreatCircle.angle_from_distance(xy.norm(), 0.0))*GreatCircle.spherical_earth_radius; // scale out of projection
		return xyz2spherical(equator_map_inv(plane_to_sphere(xy.Hat().Scal(d))), alt + projAlt);
    }

	
    private static final double TIME_STEP = 10.0; // s
      
    /** Given a velocity from a point in geodetic coordinates, return a projection of this velocity in Euclidean 3-space */
    public Velocity projectVelocity(LatLonAlt lla, Velocity v) {
   	    LatLonAlt ll2 = GreatCircle.linear_initial(lla,v,TIME_STEP);
	    Vect3 se = project(lla);
	    Vect3 s2 = project(ll2);
	    //Vect3 vn = s2.Sub(se).Scal(1/TIME_STEP);
	    Velocity vn = Velocity.Diff_Scal(s2,se,1/TIME_STEP); 
	    return vn;
    }
    
    /** Given a velocity from a point, return a projection of this velocity in Euclidean 3-space  (if already in Euclidian coordinate, this is the identity function) */
    public Velocity projectVelocity(Position ss, Velocity v) {
    	if (ss.isLatLon()) {
          return projectVelocity(ss.lla(),v);
    	} else {
    		return v;
    	}
    }
    
    /** Given a velocity from a point in Euclidean 3-space, return a projection of this velocity.  If toLatLon is true, the velocity is projected into the geodetic coordinate space */
    public Velocity inverseVelocity(Vect3 s, Velocity v, boolean toLatLon){
    	if (toLatLon) {
    	  Vect3 s2 = s.linear(v,TIME_STEP);
    	  return GreatCircle.velocity_initial(inverse(s),inverse(s2),TIME_STEP);
    	} else {
    		return v;
    	}
    }
    
    private static Vect3 spherical2xyz(double lat, double lon) {
    	double r = GreatCircle.spherical_earth_radius;
    	// convert latitude to 0-PI
    	double theta = Math.PI/2 - lat;
    	double phi = Math.PI - lon;
    	double x = r*Math.sin(theta)*Math.cos(phi);
    	double y = r*Math.sin(theta)*Math.sin(phi);
    	double z = r*Math.cos(theta);
    	return new Vect3(x,y,z);
    }

    private static LatLonAlt xyz2spherical(Vect3 v, double alt) {
    	double r = GreatCircle.spherical_earth_radius;
    	double theta = Util.acos_safe(v.z/r);
    	double phi = Util.atan2_safe(v.y, v.x);
    	double lat = Math.PI/2 - theta;
    	double lon = Util.to_pi(Math.PI - phi);
    	return LatLonAlt.mk(lat, lon, alt);
    }


    private static Vect3 vect3_orthog_toy(Vect3 v) {
    	if (!Util.within_epsilon(v.x, Constants.GPS_LIMIT_HORIZONTAL) || !Util.within_epsilon(v.y, Constants.GPS_LIMIT_HORIZONTAL)) {
    		return new Vect3(v.y, -v.x, 0);
    	} else {
    		return new Vect3(1,0,0);
    	}
    }

//    // takes a reference point in R3 that is on the sphere (at radius r) and rotates it to (r, 0, 0), with point p rotated relatively
//    private Vect3 equator_map(Vect3 p) {
//    	//Vect3 xmult = ref.Hat();
//    	//Vect3 ymult = vect3_orthog_toy(ref).Hat();
//    	//Vect3 zmult = ref.cross(vect3_orthog_toy(ref)).Hat();
//    	//return new Vect3(xmult.dot(p), ymult.dot(p), zmult.dot(p));
//    	
//    	Vect3 xmult = refHat;
//    	Vect3 ymult = vect3_orthog_toy(ref).Hat(); // since refHat is a unit vector this is a unit vector.
//    	Vect3 zmult = refHat.cross(ymult);      // since refHat and ymult are both unit vectors, the cross is one too
//    	return new Vect3(xmult.dot(p), ymult.dot(p), zmult.dot(p));
//    }
//    
//    // projects points near (r,0,0) onto tangent plane
//    private Vect2 sphere_to_plane(Vect3 p) {
//    	Vect3 v = equator_map(p);
//    	return new Vect2(v.y, -v.z);
//    }
    
    // projects points on plane onto a sphere in the region of (r,0,0)
    private Vect3 plane_to_sphere(Vect2 v) {
    	double r = GreatCircle.spherical_earth_radius;
    	double x = Math.sqrt(r*r - v.x*v.x - v.y*v.y);
    	return new Vect3(x, v.x, -v.y);
    }
    
    // rotate sphere back to original position
    private Vect3 equator_map_inv(Vect3 p) {
    	Vect3 xmult = refHat;
    	Vect3 ymult = refOrthoHat; //vect3_orthog_toy(ref).Hat();
    	Vect3 zmult = refHat.cross(ymult); //.Hat();
    	Vect3 xmultInv = new Vect3(xmult.x, ymult.x, zmult.x);
    	Vect3 ymultInv = new Vect3(xmult.y, ymult.y, zmult.y);
    	Vect3 zmultInv = new Vect3(xmult.z, ymult.z, zmult.z);
    	return new Vect3(xmultInv.dot(p), ymultInv.dot(p), zmultInv.dot(p));
    }


   
    /**
     * Project both a position and a velocity at that position into a Euclidean reference frame 
     */
    public Pair<Vect3,Velocity> project(Position p, Velocity v) {
		if (p.isLatLon()) {
			return project(p.lla(), v);
		} else {
			return Pair.make(p.vect3(), v);
		}
    }
//    public Pair<Vect3,Velocity> project(Position p, Velocity v) {
//    	return new Pair<Vect3,Velocity>(project(p),projectVelocity(p,v));
//    }

    @Override
    public Pair<Vect3,Velocity> project(LatLonAlt lla, Velocity v) {
	    //Vect3 vec3 = project(lla);
    	Vect3 s3 = project(lla); // by calling project() and projectVelocity() separately, the called would performed twice
   	    LatLonAlt nlla = GreatCircle.linear_initial(lla,v,TIME_STEP);
	    Vect3 ns3 = project(nlla);
	    //Vect3 nv3 = s2.Sub(vec3).Scal(1/TIME_STEP);
	    Velocity nv3 = Velocity.Diff_Scal(ns3,s3,1.0/TIME_STEP);
    	return new Pair<Vect3,Velocity>(s3, nv3);
    }

     /**
      * Find the lat/lon/alt position and velocity from a given Euclidean reference frame.  
      */
    public Pair<Position,Velocity> inverse(Vect3 p, Velocity v, boolean toLatLon) {
    	if (toLatLon) {
    		return new Pair<Position,Velocity>(Position.make(inverse(p)),inverseVelocity(p,v,true));
    	} else {
    		return new Pair<Position,Velocity>(Position.make(p),v);
    	}
    }
    
    /** String representation */
    public String toString() {
    	return "AziEquiProjection: projAlt = "+Units.str("ft",projAlt)+" refHat = "+refHat;
    }

    
}
