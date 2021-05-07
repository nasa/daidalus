/*
 * Vect3.java 
 * 
 * 3-D vectors.
 * 
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.util.Arrays;

/**
 * <p>3-Dimensional mathematical vectors. Vect3 is immutable.  Once these objects are created they can never
 * be changed so multiple references to the same object will never cause problems.  However, it does
 * mean that for most vector operations new objects are created.  Methods that start with a capital 
 * letter create a new object, just as a reminder of this behavior.</p>
 */
public class Vect3 {

	/** A zero vector */
	public static final Vect3 ZERO = new Vect3();

	/** An invalid Vect3.  Note that this is not necessarily equal to other invalid Vect3s -- use the isInvalid() test instead. */
	public static final Vect3 INVALID = new Vect3(Double.NaN,Double.NaN,Double.NaN);

	/** The x-component */
	public final double x;

	/** The y-component */
	public final double y;

	/** The z-component */
	public final double z;

	/**
	 * Creates a zero vector.
	 */
	private Vect3() {
		this(0.0,0.0,0.0);
	}

	/**
	 * Creates a vector that is an extension of v
	 * @param v 2D vector 
	 * @param z z component
	 */
	public Vect3(Vect2 v, double z) {
		this(v.x,v.y,z);
	}

	/**
	 * Creates a new vector with coordinates (<code>x</code>,<code>y</code>,<code>z</code>).
	 * 
	 * @param xx Real value [internal units]
	 * @param yy Real value [internal units]
	 * @param zz Real value [internal units]
	 */
	public Vect3(double xx, double yy, double zz) {
		x = xx;
		y = yy;
		z = zz;
	}

	/**
	 * Creates a new vector with coordinates (<code>x</code>,<code>y</code>,<code>z</code>) in specified units.
	 * 
	 * @param x  Real value [ux]
	 * @param ux Units x
	 * @param y  Real value [uy]
	 * @param uy Units y
	 * @param z  Real value [uz]
	 * @param uz Units z
	 * @return a new vector 
	 */
	public static Vect3 makeXYZ(double x, String ux, double y, String uy, double z, String uz) {
		return new Vect3(Units.from(ux,x),Units.from(uy,y),Units.from(uz,z));
	}

	/**
	 * Creates a new vector with coordinates (<code>x</code>,<code>y</code>,<code>z</code>) in internal units.
	 * 
	 * @param x Real value [internal units]
	 * @param y Real value [internal units]
	 * @param z Real value [internal units]
	 * @return a new vector 
	 */
	public static Vect3 mkXYZ(double x, double y, double z) {
		return new Vect3(x,y,z);
	}
	
	/**
	 * Creates a new vector with coordinates (<code>x</code>,<code>y</code>,<code>z</code>) in internal units.
	 * 
	 * @param x Real value [internal units]
	 * @param y Real value [internal units]
	 * @param z Real value [internal units]
	 * @return a new vector 
	 */
	public static Vect3 mk(double x, double y, double z) {
		return new Vect3(x,y,z);
	}

	/**
	 * Creates a new vector with coordinates (<code>x</code>,<code>y</code>,<code>z</code>) in external units.
	 * 
	 * @param x Real value [NM]
	 * @param y Real value [NM]
	 * @param z Real value [ft]
	 * @return a new vector
	 */
	public static Vect3 make(double x, double y, double z) {
		return new Vect3(Units.from("NM",x),Units.from("NM",y),Units.from("ft",z));
	}


	public Vect3 mkX(double nx) {
		return mkXYZ(nx, y ,z);
	}

	public Vect3 mkY(double ny) {
		return mkXYZ(x, ny ,z);
	}

	public Vect3 mkZ(double nz) {
		return mkXYZ(x, y , nz);
	}

	/** The x coordinate 
	 * @return x coordinate */
	public double x() {
		return x;
	}

	/** The y coordinate 
	 * @return y coordinate */
	public double y() {
		return y;
	}

	/** The z coordinate 
	 * @return z coordinate */
	public double z() {
		return z;
	}

	/**
	 * Zero constant.
	 * @return a zero vector 
	 */
	public static Vect3 newZero() {
		return new Vect3();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
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
		if (!(obj instanceof Vect3))
			return false;
		Vect3 other = (Vect3) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		if (Double.doubleToLongBits(z) != Double.doubleToLongBits(other.z))
			return false;
		return true;
	}

	/**
	 * Checks if vector is zero.
	 * 
	 * @return <code>true</code>, if <code>this</code> vector is zero.
	 */
	public boolean isZero() {
		return x == 0.0 && y == 0.0 && z == 0.0;
	}

	/**
	 * Checks if vectors are almost equal.
	 * 
	 * @param v Vector
	 * 
	 * @return <code>true</code>, if <code>this</code> vector is almost equal 
	 * to <code>v</code>.
	 */
	public boolean almostEquals(Vect3 v) {
		return Util.almost_equals(x,v.x) && Util.almost_equals(y,v.y) && 
				Util.almost_equals(z,v.z);
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
	public boolean almostEquals(Vect3 v, long maxUlps) {
		return Util.almost_equals(x, v.x, maxUlps) && Util.almost_equals(y, v.y, maxUlps) && 
				Util.almost_equals(z, v.z, maxUlps);
	}

	public boolean almostEquals2D(Vect3 v, double horizEps) {
		return (vect2().Sub(v.vect2())).norm() < horizEps;
	}


	public boolean within_epsilon(Vect3 v2, double epsilon) {
		if (Math.abs(x - v2.x) > epsilon) return false;
		if (Math.abs(y - v2.y) > epsilon) return false;
		if (Math.abs(z - v2.z) > epsilon) return false;
		return true;  
	}

	/**
	 * Dot product.
	 * 
	 * @param v Vector
	 * 
	 * @return the dot product of <code>this</code> vector and <code>v</code>.
	 */
	public double dot(Vect3 v) {
		return dot(v.x,v.y,v.z);
	}


	/**
	 * Dot product.
	 * 
	 * @param xx Real value
	 * @param yy Real value
	 * @param zz Real value
	 * 
	 * @return the dot product of <code>this</code> vector and (<code>x</code>,<code>y</code>,<code>z</code>).
	 */
	public double dot(double xx, double yy, double zz) {
		return x*xx + y*yy + z*zz;
	}

	/**
	 * Square.
	 * 
	 * @return the dot product of <code>this</code> vector with itself.
	 */
	public double sqv() {
		return dot(x,y,z); // dot product of this vector with itself
	}

	/**
	 * Norm.
	 * 
	 * @return the norm of of <code>this</code> vector.
	 */
	public double norm() {
		return Math.sqrt(sqv());
	}

	/**
	 * Make a unit vector from the current vector.  If it is a zero vector, then a copy is returned.
	 * @return the unit vector
	 */
	public Vect3 Hat() {
		double n = norm();
		if (n == 0.0) { // this is only checking the divide by zero case, so an exact comparison is correct.
			return this;
		}
		return new Vect3(x/n, y/n, z/n);
	}


	/**
	 * Cross product.
	 * 
	 * @param v Vector
	 * 
	 * @return the cross product of <code>this</code> vector and <code>v</code>.
	 */
	public Vect3 cross(Vect3 v) {
		return new Vect3(y*v.z - z*v.y, z*v.x - x*v.z, x*v.y - y*v.x);
	}

	public boolean parallel(Vect3 v) {
		return cross(v).almostEquals(ZERO); 
	}

	/**
	 * 2-Dimensional projection.
	 *
	 * @return the 2-dimensional projection of <code>this</code>.
	 */
	public Vect2 vect2() {
		return new Vect2(x,y);
	}

	/**
	 * Vector addition.
	 * 
	 * @param v Vector
	 * 
	 * @return the vector addition of <code>this</code> vector and <code>v</code>.
	 */
	public Vect3 Add(Vect3 v) {
		return new Vect3(x+v.x, y+v.y, z+v.z);
	}

	/**
	 * Vector subtraction.
	 * 
	 * @param v Vector
	 * 
	 * @return the vector subtraction of <code>this</code> vector and <code>v</code>.
	 */
	public Vect3 Sub(Vect3 v) {
		return new Vect3(x-v.x, y-v.y, z-v.z);
	}

	/**
	 * Vector negation.
	 * 
	 * @return the vector negation of of <code>this</code> vector.
	 */
	public Vect3 Neg() {
		return new Vect3(-x,-y,-z);
	}

	/**
	 * Scalar multiplication.
	 * 
	 * @param k Real value
	 * 
	 * @return the vector scalar multiplication of <code>this</code> vector and <code>k</code>.
	 */
	public Vect3 Scal(double k) {
		return new Vect3(k*x, k*y, k*z);
	}

	/**
	 * Scalar and addition multiplication. Compute: k*<code>this</code> + v
	 * 
	 * @param k Real value
	 * @param v Vector
	 * 
	 * @return the scalar multiplication <code>this</code> vector and <code>k</code>, followed by an
	 * addition to vector <code>v</code>.
	 */
	public Vect3 ScalAdd(double k, Vect3 v) {
		return new Vect3(k*x+v.x, k*y+v.y, k*z+v.z);
	}

	/**
	 * Addition and scalar multiplication.  Compute: <code>this + k*v</code>;
	 * 
	 * @param k real value
	 * @param v vector
	 * 
	 * @return the addition of <code>this</code> vector to <code>v</code> scaled by <code>k</code>.
	 */
	public Vect3 AddScal(double k, Vect3 v) {
		return new Vect3(x+k*v.x, y+k*v.y, z+k*v.z);
	}
	

	/**
	 * Right perpendicular, z-component set to 0
	 * 
	 * @return the right perpendicular of <code>this</code> vector, i.e., (<code>y</code>, <code>-x</code>).
	 */
	public Vect3 PerpR() {
		return new Vect3(y,-x,0);
	}

	/**
	 * Left perpendicular, z-component set to 0
	 * 
	 * @return the left perpendicular of <code>this</code> vector, i.e., (<code>-y</code>, <code>x</code>).
	 */
	public Vect3 PerpL() {
		return new Vect3(-y,x,0);
	}

	/**
	 * Calculates position after t time units in direction and magnitude of velocity v
	 * @param v    velocity
	 * @param t    time
	 * @return the new position
	 */
	public Vect3 linear(Vect3 v, double t) {
		return new Vect3(x+v.x*t, y+v.y*t, z+v.z*t);
	}

	/** Calculates position after moving distance d in the direction "track"
	 * @param track   the direction
	 * @param d       distance
	 * @return the new position (horizontal only)
	 */
	public Vect3 linearByDist2D(double track, double d) {
		return new Vect3(x+d*Math.sin(track), y+d*Math.cos(track), z);
	}

	/** 3-D time of closest point of approach 
	 * if time is negative or velocities are parallel returns 0
	 * @param so position of one
	 * @param vo velocity of one
	 * @param si position of two
	 * @param vi velocity of two
	 * @return time of closest point of approach 
	 */
	public static double tcpa (Vect3 so, Vect3 vo, Vect3 si, Vect3 vi) {
		double t;
		Vect3 s = so.Sub(si);
		Vect3 v = vo.Sub(vi);
		double nv = v.sqv();
		if (nv > 0) 
			t = Util.max(0.0,-s.dot(v)/nv);
		else 
			t = 0;
		return t;
	}// tcpa

	/** 3-D distance at closest point of approach
	 * @param so position of one
	 * @param vo velocity of one
	 * @param si position of two
	 * @param vi velocity of two
	 * @return distance at closest point of approach
	 */
	public static double dcpa(Vect3 so, Vect3 vo, Vect3 si, Vect3 vi) {
		double t = tcpa(so,vo,si,vi);
		Vect3 s = so.Sub(si);
		Vect3 v = vo.Sub(vi);
		Vect3 st = s.AddScal(t,v);
		return st.norm();
	}// dcpa

	/**
	 * Check if the current vector is valid
	 * @return true if the current vector has an "invalid" value
	 */
	public boolean isInvalid() {
		return Double.isNaN(x) || Double.isNaN(y) || Double.isNaN(z);
	}

	/**
	 * Cylindrical norm.
	 * @param d Radius of cylinder
	 * @param h Half-height of cylinder
	 * @return the cylindrical distance of <code>this</code>. The cylindrical distance is
	 * 1 when <code>this</code> is at the boundaries of the cylinder. 
	 */
	public double cyl_norm(double d, double h) {
		return Util.max(vect2().sqv()/Util.sq(d),Util.sq(z/h));
	}

	/**
	 * Compare two vectors: return true iff delta is within specified limits 
	 * 
	 * @param v a Vect3
	 * @param maxX maximum x
	 * @param maxY maximum y
	 * @param maxZ maximum z
	 * @return true iff each component of the vector is within the given bound.
	 */
	public boolean compare(Vect3 v, double maxX, double maxY, double maxZ) {
		if (Math.abs(v.x - x) > maxX) return false;
		if (Math.abs(v.y - y) > maxY) return false;
		if (Math.abs(v.z - z) > maxZ) return false;
		return true;
	}

	/** The horizontal distance between this vector and the given vector, essentially same as v.Sub(w).vect2().norm() 
	 * @param w vector
	 * @return horizontal distance */
	public double distanceH(Vect3 w) {
		Vect2 v = new Vect2(x,y);
		return v.Sub(w.vect2()).norm(); 
	}

	/** The vertical distance between this vector and the given vector, essentially same as v.z - w.z 
	 * @param w vector
	 * @return vertical distance */
	public double distanceV(Vect3 w) {
		return z - w.z;
	}

	/** A string representation of this vector */
	public String toString() {
		return toString(Constants.get_output_precision());
	}

	/** A string representation of this vector 
	 * @param precision number of digits of precision
	 * @return a string */
	public String toString(int precision) {
		return formatXYZ(precision,"(",", ",")");
	}

	/** A string representation of this vector */
	public String toStringNP(String xunit, String yunit, String zunit) {
		return toStringNP(xunit,yunit,zunit,Constants.get_output_precision());
	}

	public String toStringNP(String xunit, String yunit, String zunit, int prec) {
		return f.FmPrecision(Units.to(xunit, x), prec) + ", " + f.FmPrecision(Units.to(yunit, y), prec) + ", " 	+ f.FmPrecision(Units.to(zunit, z), prec);
	}

	public String formatXYZ(String pre, String mid, String post) {
		return formatXYZ(Constants.get_output_precision(),pre,mid,post);
	}

	public String formatXYZ(int prec, String pre, String mid, String post) {
		return pre+f.FmPrecision(x,prec)+mid+f.FmPrecision(y,prec)+mid+f.FmPrecision(z,prec)+post;
	}

	public String toPVS() {
		return toPVS(Constants.get_output_precision());
	}
	
	public String toPVS(int precision) {
		return "(# x:= "+f.FmPrecision(x,precision)+", y:= "+f.FmPrecision(y,precision)+", z:= "+f.FmPrecision(z,precision)+" #)";
	}

	/** 
	 * This parses a space or comma-separated string as a Vect3 (an inverse to the toString method).  If three 
	 * bare values are present, then it is interpreted as internal units.
	 * If there are 3 value/unit pairs then each values is interpreted wrt the appropriate unit.  If the string 
	 * cannot be parsed, an INVALID value is returned. 
	 * @param str a string
	 * @return a vector
	 * */
	public static Vect3 parse(String str) {
		String[] fields = str.split(Constants.wsPatternParens);
		if (fields[0].equals("")) {
			fields = Arrays.copyOfRange(fields,1,fields.length);
		}
		try {
			if (fields.length == 3) {
				return new Vect3(Double.parseDouble(fields[0]),Double.parseDouble(fields[1]),Double.parseDouble(fields[2]));
			} else if (fields.length == 6) {
				return new Vect3(Units.from(Units.clean(fields[1]),Double.parseDouble(fields[0])),
						Units.from(Units.clean(fields[3]),Double.parseDouble(fields[2])),
						Units.from(Units.clean(fields[5]),Double.parseDouble(fields[4])));
			}
		} catch (Exception e) {
			// ignore exception
		}
		return INVALID;
	}

	
	public String toUnitTest() {
		return "Vect3.make("+(f.Fm8(Units.to("NM",x()))+", "+f.Fm8(Units.to("NM",y())) +", "	+f.Fm8(Units.to("ft",z()))+")");
	}

	public String toUnitTestSI() {
		return "Vect3.mkXYZ("+(f.Fm8(x())+", "+f.Fm8(y()) +", "	+f.Fm8(z())+")");
	}
	
	/*
	 * Two dimensional calculations on Vect3s.  z components will be ignored or set to zero.
	 */

	public double det2D(Vect3 v) {
		return x*v.y - y*v.x;
	}

	public double dot2D(Vect3 v) {
		return x*v.x + y*v.y;
	}

	public double sqv2D() {
		return x*x+y*y;
	}

	public double norm2D() {
		return Math.sqrt(sqv2D());
	}

	public Vect3 Hat2D() {
		double n = norm2D();
		if (n == 0.0) { // this is only checking the divide by zero case, so an exact comparison is correct.
			return ZERO;
		}
		return new Vect3(x/n, y/n, 0.0);
	}

}
