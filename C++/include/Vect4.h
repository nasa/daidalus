/*
 * Vect4.h
 *
 * 4-D vectors.
 *
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

#ifndef VECT4_H_
#define VECT4_H_

#include "Vect3.h"

namespace larcfm {

/** 
 * <p>A four dimensional mathematical vector. Vect4 is immutable.  Once these objects are created they can never
 * be changed so multiple references to the same object will never cause problems.  However, it does
 * mean that for most vector operations new objects are created.  Methods that start with a capital 
 * letter create a new object, just as a reminder of this behavior. </p>
 * */
class Vect4 {
public:

	// This class should be immutable and therefore
	// data members should be "const."/ However, if
	// they were const, the there cannot be an
	// assignment operator interfers with the default
	// assignment operator.  Hopefully, no one
	// will take advantage of the non-constness of
	// these members.

    /** x component */
  double  x;
	/** y component */
  double  y;
	/** z component */
  double  z;
	/** t component */
  double  t;

  /** Construct a Vect4 */
  Vect4(const double xx=0, const double yy=0, const double zz=0, const double tt=0);
  /** Construct a Vect4 
   * 
   * @param v vector
   * @param t time (4th dimension of vector)
   */
  Vect4(const Vect3& v, double t);

  /** Is this vector zero? */
  bool isZero() const;
  /**
   * Checks if vectors are almost equal.
   * 
   * @param v Vector
   * 
   * @return <code>true</code>, if <code>this</code> vector is almost equal 
   * to <code>v</code>.
   */
  bool almostEquals(const Vect4& v) const;
  /**
   * Checks if vectors are almost equal.
   * 
   * @param v Vector
   * @param maxUlps unit of least precision
   * 
   * @return <code>true</code>, if <code>this</code> vector is almost equal 
   * to <code>v</code>.
   */
  bool almostEquals(const Vect4& v, INT64FM maxUlps) const;
  /* Add the parameter to this vector */

  /** Return a new vector that is the addition of this vector and the parameter */
  Vect4  operator + (const Vect4& v) const;
  /** Return a new vector that is the subtract of this vector and the parameter */
  Vect4  operator - (const Vect4& v) const;
  /** Return a new vector that is the negation of this vector  */
  Vect4  operator - () const;
  /** Return a new vector that is the scale of this vector by k */
  Vect4  operator * (const double k) const;
  /** The dot product of this vector and the parameter */
  double operator * (const Vect4& v) const; // Dot product
  
  /** Are these two vectors equal? */
  bool operator == (const Vect4& v) const;
  /** Are these two vectors unequal? */
  bool operator != (const Vect4& v) const;

  /** Return the x,y, and z components of this vector 
   * @return 3D vector
   */
  Vect3  vect3() const;
  /** Return the x and y components of this vector
   * @return 2D vector 
   */
  Vect2  vect2() const;
  /** Return a new vector that is the addition of this vector and the parameter
   * 
   * @param v vector
   * @return vector sum
   */
  Vect4  Add(const Vect4& v) const;
  /** Return a new vector that is the subtract of this vector and the parameter
   * 
   * @param v vector
   * @return vector subtraction 
   */
  Vect4  Sub(const Vect4& v) const;
  /** Return a new vector that is the negation of this vector
   * 
   * @return negation vector
   */
  Vect4  Neg() const ;
  /** Return a new vector that is the scale of this vector by k 
   * @param k scale factor
   * @return scaled vector
   * */
  Vect4  Scal(double k) const;
  /** Return a new vector that is this*k+v 
   * 
   * @param k vector scale factor
   * @param v vector
   * @return result of this*k+v
   */
  Vect4  ScalAdd(const double k, const Vect4& v) const;

  /** A symmetry calculation */
  int    breakSymmetry() const;

  /** The dot product of this vector and the parameter */
  double dot(const double x, const double y, const double z, const double t) const;
  /** The sum of the square of each component 
   * 
   * @return sum of squares
   */
  double sqv() const;
  /** The vector norm of this vector, i.e., the Euclidean distance 
   * @return norm
   */
  double norm() const;

  /** The latitude component of this vector 
   * @return latitude
   */
  double lat() const;
  /** The longitude component of this vector 
   * @return longitude
   */
  double lon() const;

  /** A string representation of this vector */
  std::string toString() const;

  /** Return a zero vector */
  static const Vect4& ZERO();

};

}

#endif /* VECT4_H_ */
