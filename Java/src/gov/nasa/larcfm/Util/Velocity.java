  /*
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

  import java.util.List;
  import java.util.ArrayList;

  /**
   * This class represents a 3-dimensional velocity vector.  The components of
   * the vector represent East-North-Up (ENU) coordinates; thus x is positive
   * in the east direction, y is positive in the north, and z is positive up.
   * The other right handed coordinate system, East-North-Down can probably
   * be used without error, but this has not be tested very well.
   * The track angle is defined by the "true north, clockwise convention."
   * Track and gs (norm of vector) are cached so that a zero velocity vector
   * keeps its track, even when the norm is 0 (physically, a vehicle has always a heading, even if the
   * norm of the velocity is zero)
   *
   */
  public final class Velocity implements OutputList {

    private final double trk_; // Track (true north, clockwise, radians)
    private final double gs_;  // Norm of velicity vector (internal units)
    private final Vect3 v_;    // 3-D vector

    /**
	 * Instantiates a new velocity in internal units.
	 *
	 * @param trk the trk (clockwise, true north)
	 * @param gs the ground speed
	 * @param vx the vx
	 * @param vy the vy
	 * @param vz the vz
	 */
	   private Velocity(double trk, double gs, double vx, double vy, double vz) {
      trk_ = trk;
      gs_ = gs;
      v_ = new Vect3(vx,vy,vz);
    } 

   /**
     * Instantiates a new velocity in internal units.
     * 
     * @param vx the vx
     * @param vy the vy
     * @param vz the vz
     */
    private Velocity(double vx, double vy, double vz) {
      trk_ = Util.atan2_safe(vx,vy);
      gs_ = Util.sqrt_safe(vx*vx+vy*vy);
      v_ = new Vect3(vx,vy,vz);
    } 

    public Velocity() {
      trk_ = 0.0;
      gs_ = 0.0;
      v_ = Vect3.ZERO;
    }

    public Vect3 vect3() {
      return v_;
    }

    public Vect2 vect2() {
      return v_.vect2();
    }

    public boolean isZero() {
      return v_.isZero();
    }

    public boolean isInvalid() {
      return v_.isInvalid();
    }

    public double x() {
      return v_.x;
    }

    public double y() {
      return v_.y;
    }

    public double z() {
      return v_.z;
    }

    /** A zero velocity */
    public final static Velocity ZERO = new Velocity();

    /** An invalid velocity.  Note that this is not necessarily equal to other invalid velocities -- use the isInvalid() test instead. */
    public final static Velocity INVALID = new Velocity(Double.NaN,Double.NaN,Double.NaN); 
    
    /**
     * New velocity from Vect3.
     * 
     * @param v the 3-D velocity vector (in internal units).
     * 
     * @return the velocity
     */
    public static Velocity make(Vect3 v) {
      return new Velocity(v.x,v.y,v.z);
    }  
    
    /**
     * New velocity from Vect2 (setting the vertical speed to 0.0).
     * 
     * @param v the 2-D velocity vector (in internal units).
     * 
     * @return the velocity
     */
    public static Velocity make(Vect2 v) {
      return new Velocity(v.x,v.y,0.0);
    }     

    /**
     * New velocity from Euclidean coordinates in internal units.
     * 
     * @param vx the x-velocity [internal]
     * @param vy the y-velocity [internal]
     * @param vz the z-velocity [internal]
     * 
     * @return the velocity
     * 
     */
    public static Velocity mkVxyz(double vx, double vy, double vz) {
      return new Velocity(vx,vy,vz);
    }     

    /**
     * New velocity from Euclidean coordinates in explicit units.
     * 
     * @param vx the x-velocity [uvxy]
     * @param vy the y-velocity [uvzy]
     * @param uvxy the units of vx and vy
     * @param vz the z-velocity [uvz]
     * @param uvz the units of vz
     * 
     * @return the velocity
     */
    public static Velocity makeVxyz(
        double vx, double vy, String uvxy,
        double vz, String uvz) {
      return new Velocity(Units.from(uvxy,vx), Units.from(uvxy, vy), Units.from(uvz,vz));
    }     

    /**
     * New velocity from Track, Ground Speed, and Vertical speed in internal units.
     * Note that this uses trigonometric functions, and may introduce numeric instability.
     * 
     * @param trk the track angle [internal]
     * @param gs the ground speed [internal]
     * @param vs the vs [internal]
     * 
     * @return the velocity
     */
    public static Velocity mkTrkGsVs(double trk, double gs, double vs) {
      return new Velocity(trk,gs,trkgs2vx(trk,gs),trkgs2vy(trk,gs),vs);
    }

    /**
     * New velocity from Track, Ground Speed, and Vertical speed in explicit units.
     * Note that this uses trigonometric functions, and may introduce numeric instability.
     * 
     * @param trk the track angle [utrk]
     * @param utrk the units of trk
     * @param gs the ground speed [ugs]
     * @param ugs the units of gs
     * @param vs the vertical speed [uvs]
     * @param uvs the units of vs
     * 
     * @return the velocity
     */
    public static Velocity makeTrkGsVs(
        double trk, String utrk,
        double gs, String ugs,
        double vs, String uvs) {
      return mkTrkGsVs(Units.from(utrk,trk), Units.from(ugs,gs), Units.from(uvs,vs));
    }

    /**
     * Return the velocity along the line from p1 to p2 at the given speed
     * @param p1 first point
     * @param p2 second point
     * @param speed speed [internal units] (composite 3 dimensional speed, Not ground or vertical speed!)
     * @return the velocity
     */
    public static Velocity mkVel(Vect3 p1, Vect3 p2, double speed) {
      return make(p2.Sub(p1).Hat().Scal(speed));
    }
    
    /**
     * Return the track along the line from p1 to p2 at the given speed
     * @param p1 first point
     * @param p2 second point
     * @return the track
     */
    public static double track(Vect3 p1, Vect3 p2) {
      return Util.atan2_safe(p2.x-p1.x,p2.y-p1.y);
    }
    
    public Velocity Neg()  {
      return new Velocity(Util.to_pi(trk_+Math.PI),gs_,-v_.x,-v_.y,-v_.z);
    }

    public Velocity Add(Vect3 v)  {
      if (Util.almost_equals(v_.x,-v.x) && Util.almost_equals(v_.y,-v.y)) {
        // Set to zero but maintain the original track
        return new Velocity(trk_,0.0,0.0,0.0,v_.z+v.z);
      }
      return mkVxyz(v_.x+v.x,v_.y+v.y,v_.z+v.z);
    }

    public Velocity Sub(Vect3 v)  {
      if (Util.almost_equals(v_.x,v.x) && Util.almost_equals(v_.y,v.y)) {
        // Maintain the original track
        return new Velocity(trk_,0.0,0.0,0.0,v_.z-v.z);
      }
      return mkVxyz(v_.x-v.x,v_.y-v.y,v_.z-v.z);
    }

    /**
     * Make a unit 2D vector from the velocity vector. 
  	 * @return the unit 2D vector
     */
	  public Vect2 Hat2D() {
      return new Vect2(Math.sin(trk_),Math.cos(trk_));
    }

    /**
     * Return the velocity if moving from p1 to p2 over the given time
     * @param p1 first point
     * @param p2 second point
     * @param dt time 
     * @return the velocity
     */
    public static Velocity genVel(Vect3 p1, Vect3 p2, double dt) {
      return make(p2.Sub(p1).Scal(1/dt));
    }

    /**
     * New velocity from existing velocity by adding the given track angle to this 
     * vector's track angle.  Essentially, this rotates the vector, a positive
     * angle means a clockwise rotation.
     * @param atrk track angle [rad]
     * @return new velocity
     */
    public Velocity mkAddTrk(double atrk) {
      double s = Math.sin(atrk);
      double c = Math.cos(atrk);
      return new Velocity(Util.to_pi(trk_+atrk),gs_,v_.x*c+v_.y*s,-v_.x*s+v_.y*c,v_.z);
    }

    /**
     * New velocity from existing velocity, changing only the track
     * @param trk track angle [rad]
     * @return new velocity
     */
    public Velocity mkTrk(double trk) {
      return mkTrkGsVs(trk,gs_,v_.z);
    }

    /**
     * New velocity from existing velocity, changing only the track
     * @param trk track angle [u]
     * @param u  units
     * @return new velocity
     */
    public Velocity mkTrk(double trk, String u) {
      return mkTrk(Units.from(u,trk));
    }

    /**
     * New velocity from existing velocity, changing only the ground speed
     * @param ags ground speed [m/s]
     * @return new velocity
     */
    public Velocity mkGs(double ags) {
      if (ags < 0) {
        return INVALID;
      }
      if (gs_ > 0.0) {
        double scal = ags/gs_;
        return new Velocity(trk_,ags,v_.x*scal, v_.y*scal, v_.z);
      }
      return mkTrkGsVs(trk_,ags,v_.z);  
    }

    /**
     * New velocity from existing velocity, changing only the ground speed
     * @param ags ground speed [u]
     * @param u  units
     * @return new velocity
     */
    public Velocity mkGs(double ags, String u) {
      return mkGs(Units.from(u, ags));
    }

    /**
     * New velocity from existing velocity, changing only the vertical speed
     * @param vs vertical speed [m/s]
     * @return new velocity
     */
    public Velocity mkVs(double vs) {
      return new Velocity(trk_,gs_,v_.x,v_.y,vs);
    }

    /**
     * New velocity from existing velocity, changing only the vertical speed
     * @param vs vertical speed [u]
     * @param u  units
     * @return new velocity
     */
    public Velocity mkVs(double vs, String u) {
      return mkVs(Units.from(u,vs));
    }

    /**
     * If the z component of the velocity vector is smaller than the threshold, return a new vector with this component set to 0.  
     * Return the original vector if the vertical speed is greater than the threshold.
     * @param threshold   level of vertical speed below which the vector is altered
     * @return the new velocity
     */
    public Velocity zeroSmallVs(double threshold) {
      if (Math.abs(v_.z) < Math.abs(threshold)) {
        return mkVs(0.0);
      }
      return this;
    }

    /**
     * Angle in radians in the range [-<code>Math.PI</code>, <code>Math.PI</code>].
     * Convention is counter-clockwise with respect to east.
     * 
     * @return the track angle [rad]
     */
    public double angle() {
      return Util.to_pi(Math.PI/2.0-trk_);
    }

    /**
     * Angle in explicit units in corresponding range [-<code>Math.PI</code>, <code>Math.PI</code>].
     * Convention is counter-clockwise with respect to east.
     * 
     * @param uangle the explicit units of track angle
     * 
     * @return the track angle [rad]
     */
    public double angle(String uangle) {
      return Units.to(uangle,angle());
    }

    /**
     * Track angle in radians in the range [-<code>Math.PI</code>, <code>Math.PI</code>].
     * Convention is clockwise with respect to north.
     * 
     * @return the track angle [rad]
     */
    public double trk() {
      return trk_;
    }

    /**
     * Track angle in explicit units in the corresponding range [-<code>Math.PI</code>, <code>Math.PI</code>]. 
     * Convention is clockwise with respect to north.
     * 
     * @param utrk the explicit units of track angle
     * 
     * @return the track angle [utrk]
     */
    public double track(String utrk) {
      return Units.to(utrk,trk_);
    }

    /**
     * Compass angle in radians in the range [<code>0</code>, <code>2*Math.PI</code>).
     * Convention is clockwise with respect to north.
     * 
     * @return the compass angle [rad]
     */
    public double compassAngle() {
      return Util.to_2pi(trk_);
    }

    /**
     * Compass angle in explicit units in corresponding range [<code>0</code>, <code>2*Math.PI</code>).
     * Convention is clockwise with respect to north.
     *  
     *  @param u the explicit units of compass angle
     *  
     *  @return the compass angle [u]
     */
    public double compassAngle(String u) {
      return Units.to(u,compassAngle());
    }

    /**
     * Ground speed in internal units.
     * 
     * @return the ground speed
     */
    public double gs() {
      return gs_;
    }

    /**
     * Ground speed in explicit units.
     * 
     * @param ugs the explicit units of ground speed
     * 
     * @return the ground speed [ugs]
     */
    public double groundSpeed(String ugs) {
      return Units.to(ugs,gs_); 
    }

    /**
     * Vertical speed in internal units.
     * 
     * @return the vertical speed
     */
    public double vs() {
      return v_.z;
    }

    /**
     * Vertical speed in explicit units.
     * 
     * @param uvs the explicit units of vertical speed
     * @return the vertical speed [uvs]
     */
    public double verticalSpeed(String uvs) {
      return Units.to(uvs,v_.z);
    }

    /** 
     * Compare Velocities: return true iff delta is within specified limits 
     * 
     * @param v       the other velocity
     * @param maxTrk  maximum track
     * @param maxGs   maximum gs
     * @param maxVs   maximum vs
     * @return true, if the velocities compare correctly
     */
    public boolean compare(Velocity v, double maxTrk, double maxGs, double maxVs) {
      if (Util.turnDelta(v.trk(),trk()) > maxTrk) return false;
      if (Math.abs(v.gs() - gs()) > maxGs) return false;
      if (Math.abs(v.vs() - vs()) > maxVs) return false;
      return true;
    }

    /**
     * Compare two velocities based on horizontal and vertical components.  This could be used against a set of nacV ADS-B limits, for example.
     * @param v other Velocity
     * @param horizDelta horizontal tolerance (absolute value)
     * @param vertDelta vertical tolerance (absolute value)
     * @return true if the velocities are within both horizontal and vertical tolerances of each other.
     */
    public boolean compare(Velocity v, double horizDelta, double vertDelta) {
      return Math.abs(v_.z-v.z()) <= vertDelta && vect2().Sub(v.vect2()).norm() <= horizDelta;
    }

    // Utilities

    /**
     * Difference of vectors, then scale result.  Compute: <code>k*(v1 - v2)</code>
     * 
     * @param k real value
     * @param v1 vector
     * @param v2 vector
     * 
     * @return <code>k*(v1 - v2)</code>
     */
    public static Velocity Diff_Scal(Vect3 v1, Vect3 v2, double k) {
      return new Velocity(k*(v1.x-v2.x), k*(v1.y-v2.y), k*(v1.z-v2.z));
    }
    
    /** Return the x component of velocity given the track and ground
     * speed.  The track angle is assumed to use the radians from true
     * North-clockwise convention.
     * 
     * @param trk track angle
     * @param gs  ground speed
     * @return x component of velocity
     */
    public static double trkgs2vx(double trk, double gs) {
      return gs * Math.sin(trk);
    }

    /** Return the y component of velocity given the track and ground
     *	speed.  The track angle is assumed to use the radians from
    *	true North-clockwise convention. 
    * 
    * @param trk track
    * @param gs  ground speed
    * @return y component of velocity
    */
    public static double trkgs2vy(double trk, double gs) {
      return gs * Math.cos(trk);
    }

    /** Return the 2-dimensional Euclidean vector for velocity given the track and ground
     *	speed.  The track angle is assumed to use the radians from
    *	true North-clockwise convention. 
    * 
    * @param trk track
    * @param gs  ground speed
    * @return 2-D velocity
    */
    public static Vect2 trkgs2v(double trk, double gs) {
      return new Vect2(trkgs2vx(trk,gs), trkgs2vy(trk,gs));
    }
    
    /** String representation of the velocity in polar coordinates (compass angle and groundspeed)
     * @return a string representation
     * */
    public String toString() {
      return toString(Constants.get_output_precision());
    }

    /** String representation of the velocity in polar coordinates (compass angle and groundspeed) in [deg, knot, fpm].  This 
     * method does not output units. 
     * @param prec precision (0-15)
     * @return a string representation
     */
    public String toString(int prec) {
      return "("+Units.str("deg",compassAngle(),prec)+", "+Units.str("knot",gs_,prec)+", "+Units.str("fpm",v_.z,prec)+")";
    }

    /** String representation of the velocity in polar coordinates (compass angle and groundspeed) 
     * @return a string representation
     * */
    public String toStringUnits() {
      return toStringUnits("deg","knot","fpm");
    }

    /** String representation (trk,gs,vs) with the given units 
     * 
     * @param trkUnits units for track 
     * @param gsUnits  units for ground speed
     * @param vsUnits  units for vertical speed
     * @return a string representation
     */
    public String toStringUnits(String trkUnits, String gsUnits, String vsUnits) {
      return "("+Units.str(trkUnits,compassAngle())+", "+ Units.str(gsUnits,gs_)+", "+ Units.str(vsUnits,v_.z)+")";
    }

    /** String representation, default number of decimal places, without parentheses 
     * @return a string representation
     * */
    public String toStringNP() {
      return toStringNP(Constants.get_output_precision());
    }

    /**
     * String representation, with user-specified precision
     * @param precision number of decimal places (0-15)
     * @return a string representation
     */
    public String toStringNP(int precision) {
      return f.FmPrecision(Units.to("deg", compassAngle()), precision)+", "+f.FmPrecision(Units.to("knot", gs_), precision)+", "+f.FmPrecision(Units.to("fpm", v_.z), precision);	
    }
    
    /**
     * String representation, with user-specified precision
     * @param precision number of decimal places (0-15)
     * @param utrk units of track
     * @param ugs units of ground speed
     * @param uvs units of vertical speed
     * @return a string representation
     */
    public String toStringNP(String utrk, String ugs, String uvs, int precision) {
      return f.FmPrecision(Units.to(utrk, compassAngle()), precision)+", "+f.FmPrecision(Units.to(ugs, gs_), precision)+", "+f.FmPrecision(Units.to(uvs, v_.z), precision);	
    }

    public String toStringNP(String utrk, String ugs, String uvs) {
      return toStringNP(utrk,ugs,uvs,Constants.get_output_precision());
    }

    /**
     * Euclidean vector representation to arbitrary precision, in [knot,knot,fpm]
     * @param prec precision (0-15)
     * @return a string representation
     */
    public String toXYZ(int prec) {
        return "("+f.FmPrecision(Units.to("knot", v_.x),prec)+", "+f.FmPrecision(Units.to("knot", v_.y),prec)+", "+f.FmPrecision(Units.to("fpm", v_.z),prec)+")";
      }

    public String toXYZ(String xu, String yu, String zu, int prec) {
        return "("+f.FmPrecision(Units.to(xu, v_.x),prec)+", "+f.FmPrecision(Units.to(yu, v_.y),prec)+", "+f.FmPrecision(Units.to(zu, v_.z),prec)+")";
      }

    /**
     * Euclidean vector representation to arbitrary precision, in [knot,knot,fpm]
     * @return a string representation
     */
    public String toStringXYZ() {
      return toXYZ(Constants.get_output_precision());
    }

    /**
     * Euclidean vector representation to arbitrary precision, in [knot,knot,fpm]
     * @return a string representation
     */
    public String toStringXYZUnits() {
      return "("+Units.str("knot", v_.x)+", "+Units.str("knot", v_.y)+", "+Units.str("fpm", v_.z)+")";
    }

    /**
     * Return an array of string representing each value of the velocity in the units deg, knot, fpm.
     * @return array of strings
     */
    public List<String> toStringList() {
      ArrayList<String> ret = new ArrayList<String>(3);
      if (v_.isInvalid()) {
        ret.add("-");
        ret.add("-");
        ret.add("-");
      } else {
        ret.add(Double.toString(Units.to("deg", compassAngle())));
        ret.add(Double.toString(Units.to("knot", gs_)));
        ret.add(Double.toString(Units.to("fpm", v_.z)));
      }
      return ret;
    }

    /**
     * Return an array of string representing each value of the velocity in the units deg, knot, fpm.
     * @param precision the number of digits to display
     * @return array of strings
     */
    public List<String> toStringList(int precision) {
      ArrayList<String> ret = new ArrayList<String>(3);
      if (v_.isInvalid()) {
        ret.add("-");
        ret.add("-");
        ret.add("-");
      } else {
        ret.add(f.FmPrecision(Units.to("deg", compassAngle()),precision));
        ret.add(f.FmPrecision(Units.to("knot", gs_),precision));
        ret.add(f.FmPrecision(Units.to("fpm", v_.z),precision));
      }
      return ret;
    }

    /**
     * Return an array of string representing each value of the velocity in terms of its Cartesian dimensions in units knot, knot, fpm.
     * @return array of strings
     */
    public List<String> toStringXYZList() {
      ArrayList<String> ret = new ArrayList<String>(3);
      if (v_.isInvalid()) {
        ret.add("-");
        ret.add("-");
        ret.add("-");

      } else {
        ret.add(Double.toString(Units.to("knot", v_.x)));
        ret.add(Double.toString(Units.to("knot", v_.y)));
        ret.add(Double.toString(Units.to("fpm", v_.z)));
      }
      return ret;
    }

    /**
     * Return an array of string representing each value of the velocity in terms of its Cartesian dimensions in units knot, knot, fpm.
     * 
     * @param precision the number of digits to display
     * @return array of strings
     */
    public List<String> toStringXYZList(int precision) {
      ArrayList<String> ret = new ArrayList<String>(3);
      if (v_.isInvalid()) {
        ret.add("-");
        ret.add("-");
        ret.add("-");
      } else {
        ret.add(f.FmPrecision(Units.to("knot", v_.x),precision));
        ret.add(f.FmPrecision(Units.to("knot", v_.y),precision));
        ret.add(f.FmPrecision(Units.to("fpm", v_.z),precision));
      }
      return ret;
    }

    /** 
     * This parses a space or comma-separated string as a XYZ Velocity (an inverse to the toStringXYZ method).  If three bare values are present, then it is interpreted as internal units.
     * If there are 3 value/unit pairs then each values is interpreted wrt the appropriate unit.  If the string cannot be parsed, an INVALID value is
     * returned. 
     * 
     * @param str string to parse
     * @return Velocity object
     */
    public static Velocity parseXYZ(String str) {
      return Velocity.make(Vect3.parse(str));
    }

  }
