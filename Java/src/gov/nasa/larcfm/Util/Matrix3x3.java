/*
 * Copyright (c) 2014-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.util.Arrays;


/**
 * This is a basic (and naive) implementation of an 3x3 (row x column) matrix of doubles, with associated functions.
 * Vectors are treated as 3x1 matrices.  These matrices are mutable.
 */
public class Matrix3x3 {
	/** Number of rows */
	public static final int M = 3;
	/** Number of columns */
	public static final int N = 3;
	
	private final double[][] d = new double[M][N];
	
	/**
	 * An matrix, pre-populated with zeros.
	 */
	private Matrix3x3() {
	}
	
	
	//
	// Factory methods
	//
	
	/**
	 * Matrix based on the array a.  If a is not 3x3, then ignore.
	 * @param a two dimensional array of values
	 * @return a 3x3 matrix
	 */
	public static Matrix3x3 make(double[][] a) {
		Matrix3x3 t = new Matrix3x3();
		if (a.length != 3) return zero();
		for (int i = 0; i < a.length; i++) {
			if (a[i].length != 3) return zero();
		}
		
		for (int i = 0; i < a.length; i++) {
			t.d[i] = Arrays.copyOf(a[i], a[i].length);
		}
		return t;
	}

	/**
	 * Matrix3x3 based on the array a
	 * @param a two dimensional array of values 
	 * @return a 3x3 matrix
	 */
	public static Matrix3x3 make(int[][] a) {
		Matrix3x3 t = new Matrix3x3();
		if (a.length != 3) return zero();
		for (int i = 0; i < a.length; i++) {
			if (a[i].length != 3) return zero();
		}
		
		for (int i = 0; i < a.length; i++) {
			for (int j = 0; j < a[i].length; j++) {
				t.d[i][j] = 1.0 * a[i][j];
			}
		}
		return t;
	}

	/**
	 * Matrix3x3 based on values
	 * 
	 * @param a00 value at 0,0
	 * @param a01 value at 0,1
	 * @param a02 value at 0,2
	 * @param a10 value at 1,0
	 * @param a11 value at 1,1
	 * @param a12 value at 1,2
	 * @param a20 value at 2,0
	 * @param a21 value at 2,1
	 * @param a22 value at 2,2
	 * @return a 3x3 matrix
	 */
	public static Matrix3x3 make(double a00, double a01, double a02, double a10, double a11, double a12, double a20, double a21, double a22) {
		Matrix3x3 t = new Matrix3x3();

		t.d[0][0] = a00;
		t.d[0][1] = a01;
		t.d[0][2] = a02;

		t.d[1][0] = a10;
		t.d[1][1] = a11;
		t.d[1][2] = a12;

		t.d[2][0] = a20;
		t.d[2][1] = a21;
		t.d[2][2] = a22;
		
		return t;
	}
	

	/** 
	 * Copy this Matrix3x3 
	 * @return a copy
	 * */
	public Matrix3x3 make() {
		Matrix3x3 t = new Matrix3x3();
		
		for (int i = 0; i < 3; i++) {
			t.d[i] = Arrays.copyOf(this.d[i], this.d[i].length);
		}
		
		return t;
	}

	/**
	 * Zero matrix.
	 * @return a zero matrix
	 */
	public static Matrix3x3 zero() {
		return new Matrix3x3();
	}

	/**
	 * Identity matrix of size n
	 * @return an identity matrix
	 */
	public static Matrix3x3 identity() {
		return make(1.0,0.0,0.0, 0.0,1.0,0.0, 0.0,0.0,1.0);
	}
	
	//
	// Utility methods
	//
	
	public static boolean equals(Matrix3x3 a, Matrix3x3 b) {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				if (a.d[i][j] != b.d[i][j]) return false;
			}
		}
		return true;
	}
	
	@Override
	public int hashCode() {
		return Arrays.hashCode(d);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Matrix3x3 other = (Matrix3x3) obj;
		return equals(this, other);
	}

	/**
	 * Returns entry (i,j) in this Matrix3x3
	 * @param i row index
	 * @param j column index
	 * @return m(i,j)
	 */
	public double get(int i, int j) {
		if (i > 2 || j > 2) return 0.0;
		return d[i][j]; 
	}
	
	/**
	 * Set entry (i,j) to value v.  Note that this modifies the underlying data structures and changes may propagate to derived objects.
	 * @param i row index
	 * @param j column index
	 * @param v value
	 */
	public void set(int i, int j, double v) {
		if (i > 2 || j > 2) return;
		d[i][j] = v;
	}

	/**
	 * Return the Vect3 that is row i of this Matrix3x3.
	 * @param i row index
	 * @return a Vect3
	 */
	public Vect3 row(int i) {
		if (i > 2) return Vect3.ZERO;
		return new Vect3(d[i][0], d[i][1], d[i][2]);
	}

	/**
	 * Return the Vect3 that is column j of this Matrix3x3.
	 * @param j column index
	 * @return a Vect3
	 */
	public Vect3 col(int j) {
		if (j > 2) return Vect3.ZERO;
		return new Vect3(d[0][j], d[1][j], d[2][j]);
	}
	
	
	
	//
	// Matrix Operations
	//


	/**
	 * Perform the matrix addition a = a + b.  This is a slightly higher
	 * performance version of add(a,b).
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return the parameter a
	 */
	public static Matrix3x3 Add(Matrix3x3 a, Matrix3x3 b) {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				a.d[i][j] += b.d[i][j];
			}
		}
		return a;
	}

	/**
	 * Matrix addition (explicit).
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return result of a+b 
	 */
	public static Matrix3x3 add(Matrix3x3 a, Matrix3x3 b) {
		Matrix3x3 c = zero();
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				c.d[i][j] = a.d[i][j] + b.d[i][j];
			}
		}
		return c;
	}
	
	/**
	 * Returns the result of this + a
	 * @param a a 3x3 matrix
	 * @return result of this + a
	 */
	public Matrix3x3 add(Matrix3x3 a) {
		return Matrix3x3.add(this,a);
	}
	
	/**
	 * Matrix subtraction, a-b (explicit).
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return result of a-b
	 */
	public static Matrix3x3 sub(Matrix3x3 a, Matrix3x3 b) {
		Matrix3x3 c = new Matrix3x3();
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				c.d[i][j] = a.d[i][j] - b.d[i][j];
			}
		}
		return c;
	}
	
	/**
	 * Returns the result of this - a
	 * @param a a 3x3 matrix
	 * @return result of this - a
	 */
	public Matrix3x3 sub(Matrix3x3 a) {
		return Matrix3x3.sub(this,a);
	}
	
	/**
	 * Matrix multiplication (explicit)
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return result of a * b
	 */
	public static Matrix3x3 mult(Matrix3x3 a, Matrix3x3 b) {
		Matrix3x3 c = new Matrix3x3();
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				c.d[i][j] = a.d[i][0] * b.d[0][j] + a.d[i][1] * b.d[1][j] + a.d[i][2] * b.d[2][j];
			}
		}
		return c;
	}

	/**
	 * Matrix multiplication (explicit).  a * b.
	 * @param a a 3x3 matrix
	 * @param b a Vect3
	 * @return a Vect3 result
	 */
	public static Vect3 mult(Matrix3x3 a, Vect3 b) {
		double c0;
		double c1;
		double c2;
		c0 = a.d[0][0]*b.x() + a.d[0][1]*b.y() + a.d[0][2]*b.z();
		c1 = a.d[1][0]*b.x() + a.d[1][1]*b.y() + a.d[1][2]*b.z();
		c2 = a.d[2][0]*b.x() + a.d[2][1]*b.y() + a.d[2][2]*b.z();
		return new Vect3(c0,c1,c2);
	}

	/**
	 * Returns the result of this * a
	 * @param a a 3x3 matrix
	 * @return a Vect3
	 */
	public Matrix3x3 mult(Matrix3x3 a) {
		return Matrix3x3.mult(this,a);
	}

	/**
	 * Returns the result of 'this * a'
	 * @param a a Vect3
	 * @return a Vect3 result
	 */
	public Vect3 mult(Vect3 a) {
		return Matrix3x3.mult(this,a);
	}

	/**
	 * Inner product (explicit)
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return inner product
	 */
	public static Matrix3x3 innerProduct(Matrix3x3 a, Matrix3x3 b) {
		return Matrix3x3.mult(a.trans(), b);
	}

	
	/**
	 * Outer product (explicit)
	 * @param a a 3x3 matrix
	 * @param b a 3x3 matrix
	 * @return outer product
	 */
	public static Matrix3x3 outerProduct(Matrix3x3 a, Matrix3x3 b) {
		return Matrix3x3.mult(a,b.trans());
	}

	/**
	 * Matrix multiplication with a constant (modify the parameter)
	 * @param a a 3x3 matrix
	 * @param x a constant value
	 * @return the results, and also the parameter a
	 */
	public static Matrix3x3 Mult(Matrix3x3 a, double x) {
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				a.d[i][j] *= x; 
			}
		}
		return a;
	}
	
	/**
	 * Matrix multiplication with a constant (explicit)
	 * @param a a 3x3 matrix
	 * @param x value
	 * @return result
	 */
	public static Matrix3x3 mult(Matrix3x3 a, double x) {
		Matrix3x3 c = new Matrix3x3();
		for (int i = 0; i < M; i++) {
			for (int j = 0; j < N; j++) {
				c.d[i][j] = a.d[i][j] * x; 
			}
		}
		return c;
	}
	
	/**
	 * Matrix multiplication with a constant (explicit)
	 * @param x value
	 * @return result
	 */
	public Matrix3x3 Mult(double x) {
		return mult(this,x);
	}


	public static double det(Matrix3x3 aa) {
		double a00 = aa.d[0][0];
		double a01 = aa.d[0][1];
		double a02 = aa.d[0][2];
		double a10 = aa.d[1][0];
		double a11 = aa.d[1][1];
		double a12 = aa.d[1][2];
		double a20 = aa.d[2][0];
		double a21 = aa.d[2][1];
		double a22 = aa.d[2][2];
		return a00*a11*a22+a01*a12*a20+a02*a10*a21-a02*a11*a20-a01*a10*a22-a00*a12*a21;
	}


	public double det() {
		return Matrix3x3.det(this);
	}
	

	public static Matrix3x3 inverse(Matrix3x3 aa) {
		double a = aa.get(0, 0);
		double b = aa.get(0, 1);
		double c = aa.get(0, 2);
		double d = aa.get(1, 0);
		double e = aa.get(1, 1);
		double f = aa.get(1, 2);
		double g = aa.get(2, 0);
		double h = aa.get(2, 1);
		double i = aa.get(2, 2);
		// explicit adjoint transposed matrix
		Matrix3x3 n = make(
				(e*i-h*f), -(b*i-h*c), (b*f-e*c),
				-(d*i-g*f), (a*i-g*c), -(a*f-c*d),
				(d*h-g*e), -(a*h-g*b), (a*e-d*b));
		return n.Mult(1/aa.det());
	}
	
	public Matrix3x3 inverse() {
		return Matrix3x3.inverse(this);
	}
	
	/**
	 * Transpose the given matrix (explicit)
	 * @param a a 3x3 matrix
	 */
	public static void Trans(Matrix3x3 a) {
		double t;
		t = a.d[0][1];
		a.d[0][1] = a.d[1][0];
		a.d[1][0] = t;
		
		t = a.d[0][2];
		a.d[0][2] = a.d[2][0];
		a.d[2][0] = t;
		
		t = a.d[1][2];
		a.d[1][2] = a.d[2][1];
		a.d[2][1] = t;
	}
	
	/**
	 * Make a new matrix that this the transpose of this matrix.
	 * @return transposed matrix 
	 */
	public Matrix3x3 trans() {
		Matrix3x3 c = make(); // copy
		Trans(c);
		return c;
	}

	/**
	 * Transpose this matrix
	 * @return transposed matrix
	 */
	public Matrix3x3 Trans() {
		Trans(this);
		return this;
	}
	
	/**
	 * Rotation matrix around x axis
	 * @param angle rotation angle
	 * @return Rotation matrix around x axis
	 */
	public static Matrix3x3 rotateX(double angle) {
		return make(1, 0,          0,
		 		 0, Math.cos(angle),Math.sin(angle),
		 		 0,-Math.sin(angle),Math.cos(angle));
	}

	/**
	 * Rotation matrix around y axis
	 * 
	 * @param angle rotation angle
	 * @return Rotation matrix around y axis
	 */
	public static Matrix3x3 rotateY(double angle) {
		return make(Math.cos(angle),0,-Math.sin(angle),
				 0,          1, 0,
				 Math.sin(angle),0, Math.cos(angle));
	}
	
	/**
	 * Rotation matrix around z axis
	 * 
	 * @param angle rotation angle
	 * @return Rotation matrix around z axis
	 */
	public static Matrix3x3 rotateZ(double angle) {
		return make(Math.cos(angle),Math.sin(angle),0,
				 -Math.sin(angle),Math.cos(angle),0,
				 0,           0,          1);
	}
	
	
}
