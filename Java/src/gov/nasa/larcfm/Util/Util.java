/*
 * Copyright (c) 2011-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */

package gov.nasa.larcfm.Util;

import java.text.SimpleDateFormat;  // Next three for strDate
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** A broad collection of utility functions */
public final class Util {

	/**
	 * The maxUlps (see almostEqual() method) for (approximately) 13 digits of
	 * decimal precision.
	 * 
	 * Please note this value is used for internal floating point comparisons and is not itself a floating point value.
	 * It should only be used as a parameter for the various almost_* functions in this file.
	 * 
	 */
	public static final long PRECISION13 = 16348;

	/**
	 * The maxUlps (see almostEqual() method) for (approximately) 5 digits of
	 * decimal precision.
	 * 
	 * Please note this value is a bit pattern used for internal floating point comparisons and is not itself a floating point value.
	 * It should only be used as a parameter for the various almost_* functions in this file.
	 */
	public static final long PRECISION5 = 1l << 40;

	/**
	 * The maxUlps (see almostEqual() method) for (approximately) 7 digits of
	 * decimal precision.
	 * 
	 * Please note this value is a bit pattern used for internal floating point comparisons and is not itself a floating point value.
	 * It should only be used as a parameter for the various almost_* functions in this file.
	 */
	public static final long PRECISION7 = 1l << 34;

	/**
	 * The maxUlps (see almostEqual() method) for (approximately) 9 digits of
	 * decimal precision.
	 * 
	 * Please note this value is a bit pattern used for internal floating point comparisons and is not itself a floating point value.
	 * It should only be used as a parameter for the various almost_* functions in this file.
	 */
	public static final long PRECISION9 = 1l << 27;

	/** A default precision for the almostEquals method 
	 * 
	 * Please note this value is a bit pattern used for internal floating point comparisons and is not itself a floating point value.
	 * It should only be used as a parameter for the various almost_* functions in this file.
	 */
	public static final long PRECISION_DEFAULT = PRECISION13;


	// Do not allow one of these to be created
	private Util() {
	}

	/**
	 * Determines if a &lt; b, without being almost equal, according to the
	 * definition of the almostEquals() method..
	 * 
	 * @param a one number
	 * @param b another number
	 * @return true, if almost_less
	 */
	public static boolean almost_less(double a, double b) {
		if (almost_equals(a, b)) {
			return false;
		}
		return a < b;
	}

	/**
	 * Determines if a &lt; b, without being almost equal, according to the
	 * definition of the almostEquals() method..
	 * 
	 * @param a one number
	 * @param b another number
	 * @param maxUlps maximum units of least precision
	 * @return true, if almost_less
	 */
	public static boolean almost_less(double a, double b, long maxUlps) {
		if (almost_equals(a, b, maxUlps)) {
			return false;
		}
		return a < b;
	}

	/**
	 * Determines if a &gt; b, without being almost equal, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @return true, if almost_greater
	 */
	public static boolean almost_greater(double a, double b) {
		if (almost_equals(a,b)) {
			return false;
		}
		return a > b;
	}

	/**
	 * Determines if a &gt; b, without being almost equal, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @param maxUlps maximum units of least precision
	 * @return true, if almost_greater
	 */
	public static boolean almost_greater(double a, double b, long maxUlps) {
		if (almost_equals(a, b, maxUlps)) {
			return false;
		}
		return a > b;
	}

	/**
	 * Determines if a is greater than or almost equal to b, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @param maxUlps maximum units of least precision
	 * @return true, if almost greater or equal
	 */
	public static boolean almost_geq(double a, double b, long maxUlps) {
		return a >= b || almost_equals(a, b, maxUlps);
	}

	/**
	 * Determines if a is greater than or almost equal to b, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @return true, if almost great or equal
	 */
	public static boolean almost_geq(double a, double b) {
		return almost_geq(a, b, PRECISION_DEFAULT);
	}

	/**
	 * Determines if a is less than or almost equal to b, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @param maxUlps maximum units of least precision
	 * @return true, if almost less or equal
	 */
	public static boolean almost_leq(double a, double b, long maxUlps) {
		return a <= b || almost_equals(a, b, maxUlps);
	}

	/**
	 * Determines if a is less than or almost equal to b, according to the
	 * definition of the almostEquals() method.
	 * 
	 * @param a one number
	 * @param b another number
	 * @return true, if almost less or equal
	 */
	public static boolean almost_leq(double a, double b) {
		return almost_leq(a, b, PRECISION_DEFAULT);
	}

	/** Are these two numbers almost equal, given the PRECISION_DEFAULT
	 * 
	 * @param a one number
	 * @param b another number 
	 * @return true, if almost equals
	 */
	public static boolean almost_equals(double a, double b) {
		return almost_equals(a, b, PRECISION_DEFAULT);
	}

	/**
	 * Determines if these two doubles, relative to each other, are almost
	 * equal. The "nearness" metric is captured in maxUlps.
	 * Mathematically, a == b is the same as a - b == 0.  Due to quirks
	 * in floating point, generally almostEquals(a, b) is not the same
	 * as almostEquals(a - b, 0).  The form without the subtraction is
	 * preferred.  <p>
	 * 
	 * Consistent with the IEEE-754 floating point specification, "not a number"
	 * (NaN) won't compare as equal to anything (including itself or another
	 * NaN).
	 * <p>
	 * 
	 * If two doubles are almost_equals() with a maxUlps parameter of 16348, then
	 * this means there can be at most 16347 floating point
	 * numbers between them. A value of 16348 for "units of least
	 * precision" (ulps) corresponds to a and b agreeing to about 13
	 * decimal places.  Said another way, the two numbers have an
	 * absolute difference of (approximately) 1e-13 if the two floating
	 * point numbers are near 1.  <p>
	 * 
	 * The maxUlps parameter must be positive and smaller than 2^50
	 * <p>
	 * 
	 * The implementation is based on the discussion (but not the code) in
	 * (google: comparing floats cygnus)
	 * 
	 * @param a one number
	 * @param b another number
	 * @param maxUlps the precision, or more specifically, the maximum units of least precision
	 * @return true, if almost equals
	 */
	public static boolean almost_equals(double a, double b, long maxUlps) {
		if (a == b) { // if two numbers are equal, then the are almost_equal
			return true;
		}

		// special case of comparing to zero.
		if (a == 0.0 || b == 0.0) {
			double comp = 1.0e-13;  // should correspond to PRECISION_DEFAULT
			if (maxUlps == PRECISION5) comp = 1.0e-5;
			if (maxUlps == PRECISION7) comp = 1.0e-7;
			if (maxUlps == PRECISION9) comp = 1.0e-9;
			if (maxUlps == PRECISION13) comp = 1.0e-13;
			if (Math.abs(a) < comp && Math.abs(b) < comp) {
				return true;
			}
		}

		if (!(a < b || b < a)) { // idiom to filter out NaN's
			return false;
		}
		if (Double.isInfinite(a) || Double.isInfinite(b)) {
			return false;
		}


		long aInt = Double.doubleToLongBits(a);

		// Make aInt lexicographically ordered as a twos-complement long
		if (aInt < 0)
			aInt = 0x8000000000000000l - aInt;

		// Make bInt lexicographically ordered as a twos-complement long
		long bInt = Double.doubleToLongBits(b);

		if (bInt < 0)
			bInt = 0x8000000000000000l - bInt;

		long intDiff = Math.abs(aInt - bInt); // This is valid because IEEE-754
		// doubles are required to be
		// lexically ordered

		return (intDiff <= maxUlps);
	}

	/** 
	 * Comparison of two values to determine if their absolute difference is within a value 
	 * epsilon.  If the epsilon value is too small relative to the 
	 * a and b values in question, then this is essentially the same as ==.  Epsilon must be positive.
	 * 
	 * @param a one number
	 * @param b another number
	 * @param epsilon maximum difference
	 * @return true, if values are within epsilon
	 */
	public static boolean within_epsilon(double a, double b, double epsilon) {
		return Math.abs(a-b) < epsilon;
	}

	/**
	 * Returns true if the magnitude of a is less than epsilon. Epsilon must be positive.
	 * 
	 * @param a a number
	 * @param epsilon maximum value
	 * @return true, if value is within epsilon
	 */
	public static boolean within_epsilon(double a, double epsilon) {
		return Math.abs(a) < epsilon;
	}

	/**
	 * Discretize the value of nvoz in the direction from voz in units of discreteUnits
	 * @param voz    The value nvoz was derived from
	 * @param nvoz   The value to be discretized
	 * @param discreteUnits     the size of discretization, e.g. 0.1, 1.0, 10.0, 100.0  etc
	 * @return       nvoz discretized to units if discreteUnits
	 */
	public static double  discretizeDir(double voz, double nvoz, double discreteUnits) {
		int sgn = -1;
		if (nvoz >= 0) sgn = 1;
		double rtn;
		if (sgn*nvoz >= sgn*voz) 
			rtn =  sgn*Math.ceil(Math.abs((nvoz)/discreteUnits))*discreteUnits;
		else
			rtn =  sgn*Math.floor(Math.abs((nvoz)/discreteUnits))*discreteUnits;
		return rtn;
	}


	/** Square 
	 * 
	 * @param x value
	 * @return square of value
	 */
	public static double sq(double x) {
		return x * x;
	}

	/** a safe (won't return NaN or throw exceptions) version of square root 
	 * 
	 * @param x value
	 * @return square root of value
	 */
	public static double sqrt_safe(double x) {
		return Math.sqrt(Util.max(x, 0));
	}

	/** a safe (won't return NaN or throw exceptions) version of arc-tangent 
	 * 
	 * @param y ordinate coordinate
	 * @param x abscissa coordinate
	 * @return arc-tangent
	 */
	public static double atan2_safe(double y, double x) {
		if (y == 0 && x == 0)
			return 0;
		return Math.atan2(y, x);
	}

	/** a safe (won't return NaN or throw exceptions) version of arc-sine 
	 * @param x value
	 * @return arc-sine of value
	 * */
	public static double asin_safe(double x) {
		return Math.asin(Util.max(-1.0,Util.min(x,1.0)));
	}

	/** a safe (won't return NaN or throw exceptions) version of arc-cosine
	 * 
	 * @param x angle
	 * @return the arc-cosine of x, between [0,pi)
	 */
	public static double acos_safe(double x) {
		return Math.acos(Util.max(-1.0,Util.min(x,1.0)));
	}

	/** Discriminant of a quadratic
	 * 
	 * @param a a coefficient of quadratic
	 * @param b b coefficient of quadratic
	 * @param c c coefficient of quadratic
	 * @return discriminant
	 */
	public static double discr(double a, double b, double c) {
		return sq(b) - 4 * a * c;
	}

	/** Quadratic equation, eps = -1 or +1 
	 * 
	 * @param a a coefficient of quadratic
	 * @param b b coefficient of quadratic
	 * @param c c coefficient of quadratic
	 * @param eps -1 or +1 (to indicate which solution you want)
	 * @return root of quadratic
	 */
	public static double root(double a, double b, double c, int eps) {
		if (a == 0 && b == 0)
			return Double.NaN;
		else if (a == 0)
			return -c / b;
		else {
			double sqb = sq(b);
			double ac  = 4*a*c;
			if (almost_equals(sqb,ac) || sqb > ac) 
				return (-b + eps * sqrt_safe(sqb-ac)) / (2 * a);
			return Double.NaN; 
		}
	}
	
	/** Assumes {@code c < 0}, {@code b > 0}
	 * 
	 * @param a     a coefficient of quadratic
	 * @param b     b coefficient of quadratic
	 * @param c     c coefficient of quadratic
	 * @return      positive root {@code >= 0} if quadratic discriminant is non-negative, -1 otherwise
	 * 
	 * NOTE: {@code c < 0}, {@code b > 0} and use of positive root (eps == 1) insures that the returned root is non-negative when discriminant is non-negative!
	 */
	public static double rootNegC(double a, double b, double c) {
		if (a == 0) return -c / b;
		double sqb = sq(b);
		double ac  = 4*a*c;
		if (almost_equals(sqb,ac) || sqb > ac) 
			return (-b + sqrt_safe(sqb-ac)) / (2 * a);
		return -1; 
	}


	/** root2b(a,b,c,eps) = root(a,2*b,c,eps) , eps = -1 or +1 
	 * 
	 * @param a a coefficient of quadratic
	 * @param b b coefficient of quadratic
	 * @param c c coefficient of quadratic
	 * @param eps -1 or +1 (to indicate which solution you want)
	 * @return root of quadratic
	 */
	public static double root2b(double a, double b, double c,int eps) {
		if (a == 0 && b == 0) 
			return Double.NaN; 
		else if (a == 0) 
			return -c/(2*b);
		else {
			double sqb = sq(b);
			double ac  = a*c;
			if (almost_equals(sqb,ac) || sqb > ac) 
				return (-b + eps*sqrt_safe(sqb-ac))/a;
			return Double.NaN; 
		}     
	}

	/** 
	 * Returns +1 if the argument is positive or 0, -1 otherwise.  Note:
	 * This is not the classic signum function from mathematics that 
	 * returns 0 when a 0 is supplied. 
	 * 
	 * @param x value
	 * @return sign of value
	 */
	public static int sign(double x) {
		// A true signum could be implemented in C++ as below.
		// template <typename T> int sgn(T val) 
		//   return (T(0) < val) - (val < T(0))
		if (x >= 0)
			return 1;
		return -1;
	}

	/** Returns +1 if the argument is true, -1 otherwise
	 * 
	 * @param b boolean value
	 * @return sign as a number
	 */
	public static int sign(boolean b) {
		if (b)
			return 1;
		return -1;
	}

	/**
	 * Return +1 if the value is greater than 0, -1 if it is less than 0, or 0.
	 * @param x value to be checked
	 * @return sign as a number (including 0)
	 */
	public static int signTriple(double x) {
		if (x > 0) {
			return 1;
		} else if (x < 0) {
			return -1;
		}
		return 0;
	}


	/** Returns +1 if the argument is positive or 0, -1 otherwise 
	 * 
	 * @param x value
	 * @return sign as a boolean
	 */
	public static boolean bsign(double x) {
		return (x >= 0);
	}

	public static double min(double x, double y) {
		return Math.min(x, y);
	}

	public static float min(float x, float y) {
		return Math.min(x, y);
	}

	public static int min(int x, int y) {
		return Math.min(x, y);
	}

	public static long min(long x, long y) {
		return Math.min(x, y);
	}

	public static double max(double x, double y) {
		return Math.max(x, y);
	}

	public static float max(float x, float y) {
		return Math.max(x, y);
	}

	public static int max(int x, int y) {
		return Math.max(x, y);
	}

	public static long max(long x, long y) {
		return Math.max(x, y);
	}

	/**
	 * minimum of an arbitrary number (3+) of double variables
	 * @param a1
	 * @param a2
	 * @param an
	 * @return
	 */
	public static double min(double a1, double a2, double a3, double ...an) {
		double res = Math.min(a1, a2);
		res = Math.min(res, a3);
		for (double a : an) {
			res = Math.min(res,  a);
		}
		return res;
	}

	/**
	 * maximum of an arbitrary number (3+) of double variables
	 * @param a1
	 * @param a2
	 * @param an
	 * @return
	 */
	public static double max(double a1, double a2, double a3, double ...an) {
		double res = Math.max(a1, a2);
		res = Math.max(res, a3);
		for (double a : an) {
			res = Math.max(res,  a);
		}
		return res;
	}


	private static final double TWOPI = 2 * Math.PI;

	/**
	 * Converts <code>rad</code> radians to the range
	 * (-<code>pi</code>, <code>pi</code>].
	 * <p>Note: this should not be used for argument reduction for trigonometric functions (Math.sin(to_pi(x))
	 *
	 * @param rad Radians
	 *
	 * @return <code>rad</code> in the range
	 * (-<code>pi</code>, <code>pi</code>].
	 */
	public static double to_pi(double rad) {
		double r = to_2pi(rad);
		if (r > Math.PI) 
			return r-TWOPI;
		else {
			return r;
		}
	}

	/**
	 * Computes the modulo of val and mod. The returned value is in the range [0,mod)
	 * 
	 * @param val numerator
	 * @param mod denominator, assumed/required to be non-zero
	 * @return modulo value
	 */
	public static double modulo(double val, double mod) {
		double n = Math.floor(val / mod);
		double r = val - n * mod;
		return Util.almost_equals(r,mod) ? 0.0 : r;
	}

	/**
	 * Computes the modulo of val and mod. If {@code mod > 0}, the returned value is in the r
	 * ange [0,mod). Otherwise, returns val.
	 * 
	 * @param val numerator
	 * @param mod denominator
	 * @return modulo value
	 */
	public static double safe_modulo(double val, double mod) {
		return mod > 0 ? modulo(val,mod) : val;  
	}

	/**
	 * Converts <code>deg</code> degrees to the range 
	 * [<code>0</code>, <code>360</code>).
	 * 
	 * @param deg Degrees
	 * 
	 * @return <code>deg</code> in the range [<code>0</code>, <code>360</code>).
	 */
	public static double to_360(double deg) {
		return modulo(deg,360);
	}

	/**
	 * Converts <code>rad</code> radians to the range
	 * [<code>0</code>, <code>2*pi</code>]. 
	 * <p>Note: this should <b>not</b> be used for argument reduction for trigonometric functions (Math.sin(to_2pi(x)).  Bad
	 * roundoff errors could occur.</p>
	 *
	 * @param rad Radians
	 *
	 * @return <code>rad</code> in the range
	 * [<code>0</code>, <code>2*pi</code>).
	 */
	public static double to_2pi(double rad) {
		return modulo(rad,TWOPI);
	}

	/**
	 * Converts <code>rad</code> radians to the range
	 * [<code>-pi/2</code>, <code>pi/2</code>]. 
	 * <p>Note: this should <b>not</b> be used for argument reduction for trigonometric functions (Math.sin(to_2pi(x)).  Bad
	 * roundoff errors could occur.</p>
	 *
	 * @param rad Radians
	 *
	 * @return <code>rad</code> in the range
	 * [<code>-pi/2</code>, <code>pi/2</code>).
	 */
	public static double to_pi2(double rad) {
		double pi2 = Math.PI/2.0;
		rad = rad + pi2;
		rad = to_pi(rad) - pi2;
		return rad;
	}

	/**
	 * Converts <code>deg</code> degrees to the range 
	 * (<code>-90</code>, <code>90</code>].
	 * 
	 * @param deg Degrees
	 * 
	 * @return <code>deg</code> in the range (<code>-90</code>, <code>90</code>].
	 */
	public static double to_90(double deg) {
		double pi2 = 90;
		deg = deg + pi2;
		deg = to_180(deg);
		if (deg < 0) {
			deg = 0.0;
		}
		deg = deg - pi2;
		return deg;
	}

	/**
	 * Converts <code>rad</code> radians to the range 
	 * [<code>-Math.PI/2</code>, <code>Math.PI/2</code>).
	 * This function is continuous, so to_pi2_cont(PI/2+eps) equals PI/2-eps.
	 * 
	 * @param rad Radians
	 * 
	 * @return <code>rad</code> in the range [<code>-Math.PI/2</code>, <code>Math.PI/2</code>).
	 */
	public static double to_pi2_cont(double rad) {
		double r = to_pi(rad);
		if (r < -Math.PI / 2) {
			return -Math.PI - r;
		} else if (r < Math.PI / 2) {
			return r;
		} else {
			return Math.PI - r;
		}
	}


	/**
	 * Converts <code>deg</code> degrees to the range 
	 * (<code>-180</code>, <code>180</code>].
	 * 
	 * @param deg Degrees
	 * 
	 * @return <code>deg</code> in the range (<code>-180</code>, <code>180</code>].
	 */
	public static double to_180(double deg) {
		double d = to_360(deg);
		if (d > 180) {
			return d-360.0;
		} else {
			return d;
		}
	}


	/**
	 * Returns true if a turn from track angle alpha to track angle beta is 
	 * clockwise (by the shortest path).  If the two angles are equal, then 
	 * this function returns true.
	 * 
	 * @param alpha one angle
	 * @param beta another angle
	 * @return true, if clockwise
	 */
	public static boolean clockwise(double alpha, double beta) {
		double a = Util.to_2pi(alpha);
		double b = Util.to_2pi(beta);
		if (Math.abs(a-b) <= Math.PI) {
			return b >= a;
		}
		return a > b;
	}


	/**
	 * Returns 1 if the minimal turn to goalTrack (i.e. less than pi) is to the right, else -1
	 * @param initTrack   initial track [rad]
	 * @param goalTrack   target track [rad]
	 * @return +1 for right, -1 for left
	 */
	public static int turnDir(double initTrack, double goalTrack) {
		return sign(Util.clockwise(initTrack,goalTrack));
	}


	/**
	 * Returns the smallest angle between two track angles [0,PI].
	 * Reminder: This is also the angle from BOT to EOT from the point of view of the turn center.  
	 * It is not angle between two segments from the vertex's point of view.  
	 * 
	 * @param alpha one angle
	 * @param beta another angle
	 * @return non-negative difference in angle
	 */
	public static double turnDelta(double alpha, double beta) {
		double a = Util.to_2pi(alpha);
		double b = Util.to_2pi(beta);
		double delta = Math.abs(a-b);
		if (delta <= Math.PI) {
			return delta;
		}
		return 2.0*Math.PI - delta;
	}

	/**
	 * Returns the smallest angle between two track angles [0,PI].
	 * 
	 * @param before starting velocity vector
	 * @param after  ending velocity vector
	 * @return difference in track angles
	 */
	public static double trackDelta(Velocity before, Velocity after) {
		return turnDelta(before.trk(), after.trk());  
	}

	public static double gsDelta(Velocity before, Velocity after) {
		return before.gs() - after.gs();  
	}

	public static double vsDelta(Velocity before, Velocity after) {
		return before.vs() - after.vs();  
	}

	/**
	 * Returns the smallest angle between two track angles [-PI,PI]. The sign indicates the direction of 
	 * the turn, positive is clockwise, negative counterclockwise.
	 * 
	 * @param alpha one angle
	 * @param beta another angle
	 * @return angle difference
	 */
	public static double signedTurnDelta(double alpha, double beta) {
		return turnDir(alpha,beta)*turnDelta(alpha,beta);
	}


	/**
	 * Returns the angle between two tracks when turning in direction indicated by turnRight flag [0,2PI]
	 * Note: this function can return an angle larger than PI!
	 * 
	 * @param alpha one angle
	 * @param beta  another angle
	 * @param turnRight when true, measure angles from the right
	 * @return angle difference
	 */
	public static double turnDelta(double alpha, double beta, boolean turnRight) {
		if (Constants.almost_equals_radian(alpha,beta)) return 0.0;      // do not want 2 PI returned
		boolean clkWise = clockwise(alpha,beta);
		double rtn = turnDelta(alpha,beta);
		if (turnRight != clkWise)   // go the long way around
			rtn = 2.0*Math.PI - rtn;
		return rtn;
	}


	/**
	 * Returns the angle between two tracks when turning in direction indicated by turnRight flag [0,2PI]
	 * Note: this function can return an angle larger than PI!
	 * 
	 * @param alpha one angle
	 * @param beta another angle
	 * @param dir +1 = right, -1 = left
	 * @return angle difference
	 */
	public static double turnDelta(double alpha, double beta, int dir) {
		return turnDelta(alpha, beta, dir > 0);
	}

	/**
	 * Convert a track angle (zero is up, clockwise is increasing) to a math angle (zero is to the right, counter clockwise is increasing).
	 * @param alpha the track angle
	 * @return the math angle
	 */ 
	public static double track2math(double alpha) {
		return Math.PI/2 - alpha;
	}


	/**
	 * Returns a double value which is a representation of the given string.  If the string does 
	 * not represent a number, false is returned.  If one wants to know
	 * the value of the string use {@link #parse_double} method.
	 * 
	 * @param s string value
	 * @return true, if the string is a double
	 */
	public static boolean is_double(String s) {
		try {
			double v = Double.parseDouble(s);
			return v <= v;  // strange, but this filters out NaN's
		} catch (Exception e) { // both NullPointerException and NumberFormatException
			return false;
		}
	}

	/**
	 * Returns a double value which is a representation of the given string.  If the string does 
	 * not represent a number, an arbitrary value is returned. 
	 * In many cases, but not all, if the string is not a number then 0.0 is returned.  However,
	 * on some platforms, "1abc" will return 1.  If one wants to know
	 * the fact that the string is not a number, then use {@link #is_double} method.
	 * @param s a string
	 * @return double value representing a string
	 */ 
	public static double parse_double(String s) {
		try {
			return Double.parseDouble(s);
		} catch (Exception e) { // both NullPointerException and NumberFormatException
			return 0.0;
		}
	}


	/**
	 * Checks if the string is an integer.
	 * @param s string
	 * @return true, if the string is the representation of an integer.
	 */ 
	public static boolean is_int(String s) {
		try {
			Integer.parseInt(s);
			return true;
		} catch (NumberFormatException e) {
			return false;
		}
	}

	/**
	 * Returns an integer value which is represented by the given string.  If the string does 
	 * not represent a number, a zero is returned.  If one wants to know
	 * the fact that the string is not a number, then use {@link #is_int} method.
	 * @param s a string
	 * @return integer value of the string
	 */ 
	public static int parse_int(String s) {
		try {
			return Integer.parseInt(s);
		} catch (NumberFormatException e) {
			return 0;
		}
	}


	/**
	 * Returns an integer value which is represented by the first numeric portion of the given string (with an optional initial minus sign).  If the string does 
	 * not contain a number, a zero is returned.
	 * @param s a string
	 * @return integer value of the string
	 */
	public static int parse_first_int(String s) {
		Pattern p = Pattern.compile("-?\\d+");
		Matcher m = p.matcher(s);
		if(m.find()) {
			return parse_int(m.group());  
		}
		return 0;
	}

	/**
	 * Returns true if the stored value for key is likely a boolean
	 * @param s name
	 * @return true if string value is true/false/t/f, false otherwise
	 */
	public static boolean is_boolean(String s) {
		return (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("T") || s.equalsIgnoreCase("false") || s.equalsIgnoreCase("F"));
	}

	/** Converts a string into a Boolean value.  This is more permissive than 
	 * Boolean.parseBoolean, it accepts "true" and "T" (ignoring case).
	 * @param value a string
	 * @return boolean value
	 */
	public static boolean parse_boolean(String value) {
		return value.equalsIgnoreCase("true") || value.equalsIgnoreCase("T");
	}


	/** Reads in a clock string and converts it to seconds.  
	 * Accepts hh:mm:ss, mm:ss, and ss.
	 * 
	 * @param s string value
	 * @return time in [s]
	 */
	public static double parse_time(String s) {
		double tm = -1;
		String patternStr = "[:]";
		String [] fields2 = s.split(patternStr);
		if (fields2.length >= 3) {
			tm = parse_double(fields2[2]) + 60 * parse_double(fields2[1]) + 3600 * parse_double(fields2[0]); // hrs:min:sec
		} else if (fields2.length == 2) {
			tm = parse_double(fields2[1]) + 60 * parse_double(fields2[0]); // min:sec
		} else if (fields2.length == 1){
			tm = parse_double(fields2[0]); 
		}
		return tm;
	}

	/**
	 * Convert the decimal time (in seconds) into a 00:00:00 string.  This 
	 * always prints two digits for the hours.  See {@link #hoursMinutesSeconds(double)} for an
	 * alternate format.
	 * 
	 * @param t time in seconds
	 * @return String of hours:mins:secs
	 */
	public static String time_str(double t) {
		int hours = (int) t/3600;
		int rem = (int) t - hours*3600;
		int mins = rem / 60;
		int secs = rem - mins*60;
		return String.format("%02d:%02d:%02d", hours, mins, secs);
	}

	/**
	 * Convert the decimal time (in seconds) into a 00:00:00 string (supports epoch time).
	 * This method only considers the time for a single day.
	 * 
	 * @param t time in seconds
	 * @return String of hours:mins:secs
	 */
	public static String time_str_UTC(double t) {
		double t2 = t % 86400.0;	// get the seconds left in the current day
		return time_str(t2);
	}

	/**
	 * Convert the decimal time (in seconds) into a 0:00:00 string. This prints as many 
	 * digits as necessary for the hours (one or two, typically).  See {@link #time_str(double)} for an
	 * alternate format.
	 * 
	 * @param t time in seconds
	 * @return String of hours:mins:secs
	 */
	public static String hoursMinutesSeconds(double t) {
		int hours = (int) t/3600;
		int rem = (int) t - hours*3600;
		int mins = rem / 60;
		int secs = rem - mins*60;
		return String.format("%d:%02d:%02d", hours, mins, secs);
	}

	/** Return a string representing this list 
	 * 
	 * @param l list of objects
	 * @return string
	 */
	public static String list2str(List<? extends Object> l) {
		StringBuilder rtn =  new StringBuilder(100);
		rtn.append("{");
		for (Object o : l) {
			rtn.append(o.toString());
			rtn.append(", ");
		}
		rtn.append("}");
		return rtn.toString();
	}

	/** Returns true if string s1 is less than or equal to string s2.
	 * 
	 * @param s1 one string
	 * @param s2 another string
	 * @return true, if s1 is less or equals to s2
	 */
	public static boolean less_or_equal(String s1, String s2) {
		return (s1.compareTo(s2) >= 0);
	}

	public static String strDate() {
		Calendar today = new GregorianCalendar();
		SimpleDateFormat df = new SimpleDateFormat();
		df.applyPattern("dd/MM/yyyy");
		return df.format(today.getTime());

	}

	/**
	 * The behavior of the x%y operator is different between Java and C++ if either x or y is negative.  Use this to always return a value between 0 and y. 
	 * @param x value
	 * @param y range
	 * @return x mod y, having the same sign as y (Java behavior)
	 */
	public static int mod(int x, int y) {
		return x % y;
	}

	/**
	 * Return the closest flight level value for a given altitude
	 * @param alt altitude
	 * @return Flight level in feet
	 */
	public static int flightLevel(double alt) {
		//Note: foot conversion is hard-coded to avoid reference to Units class.
		return (int)Math.round(alt/0.3048/100.0); 
	}

	/**
	 * Generate the histogram of n bins.
	 * 
	 * @param data the data points.
	 * @param k the number of bins.
	 * @return a 3-by-k bins array of which first row is the lower bound of bins,
	 * second row is the upper bound of bins, and the third row is the frequency count.
	 * Returns a triple (histogram array, minimum element, max element)
	 */
	public static Triple<int[],Double,Double> histogram(List<Double> data, int k) {
		double min = Double.MAX_VALUE;
		double max = Double.MIN_VALUE;
		for (int i = 0; i < data.size(); i++) {
			Double val_i = data.get(i);
			if (val_i < min) min = val_i;
			if (val_i > max) max = val_i;
		}
		double range = max - min;
		if (range == 0) {
			range = k;
		}
		double width = range / k;
		double[] endPts = new double[k + 1];
		endPts[0] = min;
		for (int i = 1; i < k; i++) {
			endPts[i] = endPts[i - 1] + width;
		}
		endPts[k] = max;
		return new Triple<>(histogram(data, endPts),min,max);
	}

	/**
	 * Generate the histogram of n bins.
	 * 
	 * @param data the data points.
	 * @param endpoints an array of size k+1 giving the breakpoints between
	 * histogram cells. Must be in ascending order.
	 * @return an array containing the count for each bin
	 */
	public static int[] histogram(List<Double> data, double[] endpoints) {
		int k = endpoints.length - 1;
		int[] count = new int[k];
		for (int i = 0; i < k; i++) {
			count[i] = 0;
		}
		for (double d : data) {
			int j = Arrays.binarySearch(endpoints, d);
			if (j >= k) {
				j = k - 1;
			}
			if (j < -1 && j >= -endpoints.length) {
				j = -j - 2;
			}
			if (j >= 0) {
				count[j]++;
			}
		}
		return count;
	}

	/**
	 * Return true if the given list is ordered based on the given Comparator (repetitions allowed)
	 * @param <T> type of elements in list
	 * @param ls object to check
	 * @param comp Comparator to use to determine ordering
	 * @param increasing if true, check for increasing ordering, if false, decreasing
	 * @param strict if true, do not allow duplication
	 * @return true if list is ordered
	 */
	public static <T> boolean isOrdered(List<T> ls, Comparator<T> comp, boolean increasing, boolean strict) {
		if (increasing) {
			if (strict) {
				for (int i = 0; i < ls.size()-1; ++i) {
					if (comp.compare(ls.get(i), ls.get(i+1)) >= 0) return false;
				}
			} else {
				for (int i = 0; i < ls.size()-1; ++i) {
					if (comp.compare(ls.get(i), ls.get(i+1)) > 0) return false;
				}
			}
		} else { // not increasing
			if (strict ) {
				for (int i = 0; i < ls.size()-1; ++i) {
					if (comp.compare(ls.get(i), ls.get(i+1)) <= 0) return false;
				}
			} else {
				for (int i = 0; i < ls.size()-1; ++i) {
					if (comp.compare(ls.get(i), ls.get(i+1)) < 0) return false;
				}
			}
		}
		return true;
	}


	/**
	 * Make sure an interval set that should range over 0-2pi actually does.
	 * This will also wrap out of bounds values back into 0-2pi.
	 * @param iv
	 * @return
	 */
	public static IntervalSet to2pi(IntervalSet iv) {
		IntervalSet r = new IntervalSet(iv);
		if (r.size() > 0 && (Double.isInfinite(r.getInterval(0).low) || Double.isInfinite(r.getInterval(r.size()-1).up))) {
			Interval i = new Interval(0,Math.PI*2);
			IntervalSet i2 = new IntervalSet();
			i2.union(i);
			return i2;
		}
		Interval lb = new Interval(Double.NEGATIVE_INFINITY, 0.0);
		Interval ub = new Interval(Math.PI*2, Double.POSITIVE_INFINITY);
		boolean cont = false;
		do {
			IntervalSet lower = r.intersection(lb);
			IntervalSet upper = r.intersection(ub);
			r.diff(lb);
			r.diff(ub);
			cont = false;
			if (lower.size() > 1 || (lower.size() == 1 && lower.getInterval(0).low != 0)) {
				lower.shift(Math.PI*2);
				r.union(lower);
				cont = true;
			}
			if (upper.size() > 0 || (upper.size() == 1 && upper.getInterval(0).up != Math.PI*2)) {
				upper.shift(-Math.PI*2);
				r.union(upper);
				cont = true;
			}
		} while (cont);
		return r;
	}

	/**
	 * Basic relative error test for double float values
	 * @param testValue 
	 * @param baseValue
	 * @param acceptibleError The percentage (1.0 = 100%) of error allowable before failure
	 * @return True if error is less than error threshold.
	 */
	public static boolean relErrorTest(double testValue, double baseValue, double acceptibleError) {
		return ((Math.abs(testValue-baseValue) / baseValue) < acceptibleError);
	}

} // Util.java
