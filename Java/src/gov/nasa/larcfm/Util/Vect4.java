/*
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.Arrays;

/** 
 * <p>A four dimensional mathematical vector. Vect4 is immutable.  Once these objects are created they can never
 * be changed so multiple references to the same object will never cause problems.  However, it does
 * mean that for most vector operations new objects are created.  Methods that start with a capital 
 * letter create a new object, just as a reminder of this behavior. </p>
 * */
public final class Vect4 {

	/** x component */
  public final double x;
	/** y component */
  public final double y;
	/** z component */
  public final double z;
	/** t component */
  public final double t;

  public static final Vect4 ZERO = new Vect4();
  
	/** An invalid Vect4.  Note that this is not necessarily equal to other invalid Vect4s -- use the isInvalid() test instead. */
  public static final Vect4 INVALID = new Vect4(Double.NaN,Double.NaN,Double.NaN,Double.NaN);

  /** Construct a Vect4 */
  private Vect4() {
    x=0;
    y=0;
    z=0;
    t=0;
  }
  
  /** Construct a Vect4 
   * 
   * @param v vector
   * @param t time (4th dimension of vector)
   */
  public Vect4(Vect3 v, double t) {
      this(v.x, v.y, v.z, t);
  }

  /** Construct a Vect4 
   * 
   * @param x 1st component of vector
   * @param y 2nd component of vector
   * @param z 3rd component of vector
   * @param t 4th component of vector
   */
  public Vect4(double x, double y, double z, double t) {
    this.x = x;
    this.y = y;
    this.z = z;
    this.t = t;
  }

  
  @Override
  public int hashCode() {
	  final int prime = 31;
	  int result = 1;
	  long temp;
	  temp = Double.doubleToLongBits(t);
	  result = prime * result + (int) (temp ^ (temp >>> 32));
	  temp = Double.doubleToLongBits(x);
	  result = prime * result + (int) (temp ^ (temp >>> 32));
	  temp = Double.doubleToLongBits(y);
	  result = prime * result + (int) (temp ^ (temp >>> 32));
	  temp = Double.doubleToLongBits(z);
	  result = prime * result + (int) (temp ^ (temp >>> 32));
	  return result;
  }

  @Override
  public boolean equals(Object obj) {
	  if (this == obj)
		  return true;
	  if (obj == null)
		  return false;
	  if (getClass() != obj.getClass())
		  return false;
	  Vect4 other = (Vect4) obj;
	  if (Double.doubleToLongBits(t) != Double.doubleToLongBits(other.t))
		  return false;
	  if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
		  return false;
	  if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
		  return false;
	  if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
		  return false;
	  return true;
  }

  /**
   * Checks if vectors are almost equal.
   * 
   * @param v Vector
   * @param maxUlps unit of least precision
   * 
   * @return <code>true</code>, if <code>this</code> vector is almost equal 
   * to <code>v</code>.
   */
  public boolean almostEquals(Vect4 v, long maxUlps) {
    return Util.almost_equals(x, v.x, maxUlps) && Util.almost_equals(y, v.y, maxUlps) && 
    Util.almost_equals(z, v.z, maxUlps) && Util.almost_equals(t, v.t, maxUlps);
  }

  /**
   * Checks if vectors are almost equal.
   * 
   * @param v Vector
   * 
   * @return <code>true</code>, if <code>this</code> vector is almost equal 
   * to <code>v</code>.
   */
  public boolean almostEquals(Vect4 v) {
    return Util.almost_equals(x, v.x) && Util.almost_equals(y, v.y) && 
    Util.almost_equals(z, v.z) && Util.almost_equals(t, v.t);
  }
  
  /** The dot product of this vector and the parameter 
   * 
   * @param v vector
   * @return dot product
   */
  public double dot(Vect4 v) {
    return x*v.x + y*v.y + z*v.z + t*v.t;
  }

  /** The sum of the square of each component 
   * 
   * @return sum of squares
   */
  public double sqv() {
      return x*x+y*y+z*z+t*t;
  }

  /** The vector norm of this vector, i.e., the Euclidean distance 
   * @return norm
   */
  public double norm() {
    return Util.sqrt_safe(sqv());
  }

  
  /** Return the x,y, and z components of this vector 
   * @return 3D vector
   */
  public Vect3 vect3() {
      return new Vect3(x,y,z);
  }

  /** Return the x and y components of this vector
   * @return 2D vector 
   */
  public Vect2 vect2() {
      return new Vect2(x,y);
  }

  /** Return a new vector that is the addition of this vector and the parameter
   * 
   * @param v vector
   * @return vector sum
   */
  public Vect4 Add(Vect4 v) {
	  return new Vect4(x+v.x, y+v.y, z+v.z, t+v.t);
  }

  /** Return a new vector that is the subtract of this vector and the parameter
   * 
   * @param v vector
   * @return vector subtraction 
   */
  public Vect4 Sub(Vect4 v) {
	  return new Vect4(x-v.x, y-v.y, z-v.z, t-v.t);
  }

  /** Return a new vector that is the negation of this vector
   * 
   * @return negation vector
   */
  public Vect4 Neg() {
      return new Vect4(-x,-y,-z,-t);
  }

  /** Return a new vector that is the scale of this vector by k 
   * @param k scale factor
   * @return scaled vector
   * */
  public Vect4 Scal(double k) {
	  return new Vect4(k*x, k*y, k*z, k*t);
  }

  /** Return a new vector that is this*k+v 
   * 
   * @param k vector scale factor
   * @param v vector
   * @return result of this*k+v
   */
  public Vect4 ScalAdd(double k, Vect4 v) {
	  return new Vect4(k*x+v.x, k*y+v.y, k*z+v.z, k*t+v.t);
  }

  /**
   * Returns true if the current vector has an "invalid" value
   * @return true, if invalid
   */
  public boolean isInvalid() {
	  return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z) || Double.isNaN(t);
  }
  
  /** A string representation of this vector */
  public String toString() {
    return "("+x+","+y+","+z+","+t+")";
  }

  /** A string representation of this vector where the assumed units are [deg] [deg] [ft] [s] 
   * @return string representation
   */
  public String str() {
      return "("+Units.to("deg",x)+","+Units.to("deg",y)+","+Units.to("ft",z)+","+t+")";
}

  /** The latitude component of this vector 
   * @return latitude
   */
  public double lat() {
      return y;
  }

  /** The longitude component of this vector 
   * @return longitude
   */
  public double lon() {
      return x;
  }
  
  /** 
   * This parses a space or comma-separated string as a Vect4 (an inverse to the toString method).  If three bare values are present, then it is interpreted as internal units.
   * If there are 4 value/unit pairs then each values is interpreted wrt the appropriate unit.  If the string cannot be parsed, an INVALID value is
   * returned.
   * 
   * @param str string
   * @return string parsed as a vector
   * */
  public static Vect4 parse(String str) {
		String[] fields = str.split(Constants.wsPatternParens);
		if (fields[0].equals("")) {
			fields = Arrays.copyOfRange(fields,1,fields.length);
		}
		try {
			if (fields.length == 4) {
				return new Vect4(Double.parseDouble(fields[0]),Double.parseDouble(fields[1]),Double.parseDouble(fields[2]),Double.parseDouble(fields[3]));
			} else if (fields.length == 8) {
				return new Vect4(Units.from(Units.clean(fields[1]),Double.parseDouble(fields[0])),
						Units.from(Units.clean(fields[3]),Double.parseDouble(fields[2])),
						Units.from(Units.clean(fields[5]),Double.parseDouble(fields[4])),
						Units.from(Units.clean(fields[7]),Double.parseDouble(fields[6])));
			}
		} catch (Exception e) {
			// ignore exception
		}
		return Vect4.INVALID;
  }

}// Vect4
