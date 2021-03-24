/*
 * Copyright (c) 2014-2021 United States Government as represented by
 * the National Aeronautics and Space Administration.  No copyright
 * is claimed in the United States under Title 17, U.S.Code. All Other
 * Rights Reserved.
 */
package gov.nasa.larcfm.Util;

import java.util.Arrays;
import java.util.List;


/**
 * This is a basic (and naive) implementation of an MxN (row x column) matrix of doubles, with associated functions.
 * Vectors are treated as Mx1 matrices.
 */
public class Matrix2d {
	public final int m, n;   // num of rows, num of columns
	/*package*/ final double[][] d;
	
	/**
	 * An MxN matrix, pre-populated with zeros.
	 * @param m rows
	 * @param n columns
	 */
	public Matrix2d(int m, int n) {
		this.m = m;
		this.n = n;
		d = new double[m][n];
	}
	
	/**
	 * Matrix2d based on the array a
	 * @param a array of values
	 */
	public Matrix2d(double[][] a) {
		d = a;
		m = a.length;
		n = a[0].length; 
	}

	/**
	 * Matrix2d based on the array a
	 * @param a array of values
	 */
	public Matrix2d(int[][] a) {
		m = a.length;
		n = a[0].length; 
		d = new double[m][n];
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				d[i][j] = a[i][j];
			}
		}
	}

	/**
	 * Matrix2d based on the linear array A, transformed into an M x N Matrix2d
	 * @param m rows
	 * @param n columns
	 * @param a array of values
	 */
	public Matrix2d(int m, int n, double[] a) {
		this.m = m;
		this.n = n;
		d = new double[m][n];
		assert (a.length == m*n);
		for (int i = 0; i < a.length; i++) {
			d[i/n][i%n] = a[i];
		}
	}

	/**
	 * Matrix2d based on the list of lists of Doubles a
	 * @param a matrix of values
	 */
	public Matrix2d(List<List<Double>> a) {
		m = a.size();
		n = a.get(0).size();
		d = new double[m][n];
		for (int i = 0; i < m; i++) {
			assert(a.get(i).size() == n);
			for (int j = 0; j < n; j++) {
				d[i][j] = a.get(i).get(j);
			}
		}
	}

	/**
	 * Matrix2d based on the list A, transformed into an M x N Matrix2d
	 * @param m rows
	 * @param n columns
	 * @param a array of values
	 */
	public Matrix2d(int m, int n, List<Double> a) {
		this.m = m;
		this.n = n;
		d = new double[m][n];
		assert (a.size() == m*n);
		for (int i = 0; i < a.size(); i++) {
			d[i%m][i/m] = a.get(i);
		}
	}

	public Matrix2d(Vect2 v) {
		m = 2;
		n = 1; 
		d = new double[m][n];
		d[0][0] = v.x;
		d[1][0] = v.y;
	}

	public Matrix2d(Vect3 v) {
		m = 3;
		n = 1; 
		d = new double[m][n];
		d[0][0] = v.x;
		d[1][0] = v.y;
		d[2][0] = v.z;
	}

//	public Matrix2d(Vect4 v) {
//		m = 4;
//		n = 1; 
//		d = new double[m][n];
//		d[0][0] = v.x;
//		d[1][0] = v.y;
//		d[2][0] = v.z;
//		d[3][0] = v.t;
//	}

	public static boolean equals(Matrix2d a, Matrix2d b) {
		if (a.m != b.m || a.n != b.n) return false;
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				if (a.d[i][j] != b.d[i][j]) return false;
			}
		}
		return true;
	}
	
//	public boolean equals(Matrix2d a) {
//		return Matrix2d.equals(this, a);
//	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(d);
		result = prime * result + m;
		result = prime * result + n;
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
		Matrix2d other = (Matrix2d) obj;
		return equals(this, other);
//		if (!Arrays.deepEquals(d, other.d))
//			return false;
//		if (m != other.m)
//			return false;
//		if (n != other.n)
//			return false;
//		return true;
	}

	/**
	 * Returns entry (i,j) in this Matrix2d
	 * @param i row index
	 * @param j column index
	 * @return value
	 */
	public double get(int i, int j) {
		return d[i][j]; 
	}
	
	/**
	 * Set entry (i,j) to value v.  Note that this modifies the underlying data structures and changes may propagate to derived objects.
	 * @param i row index
	 * @param j column index
	 * @param v value
	 */
	public void set(int i, int j, double v) {
		d[i][j] = v;
	}

	/**
	 * Return the Matrix2d that is row i of this Matrix2d.
	 * @param i row index
	 * @return new matrix
	 */
	public Matrix2d row(int i) {
		return new Matrix2d(m, 1 ,d[i]);
	}

	/**
	 * Return the Matrix2d that is column j of this Matrix2d.
	 * @param j column index
	 * @return new matrix
	 */
	public Matrix2d col(int j) {
		return new Matrix2d(1, n ,d[j]);
	}

	/**
	 * Return a submatrix of this matrix.
	 * @param i row coordinate start
	 * @param j col coordinate start
	 * @param szm rows in new matrix
	 * @param szn cols in new matrix
	 * @return new matrix
	 */
	public Matrix2d submatrix(int i, int j, int szm, int szn) {
		Matrix2d a = new Matrix2d(szm,szn);
		for (int t = 0; t < szm; t++) {
			for (int u = 0; u < szn; u++) {
				a.d[t][u] = d[t+i][u+j];
			}
		}
		return a;
	}

	/**
	 * Concatenate matrix A:B into a new matrix [A B]
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d catHoriz(Matrix2d a, Matrix2d b) {
		assert(a.m == b.m);
		Matrix2d c = new Matrix2d(a.m, a.n+b.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[i][j] = a.d[i][j];
			}
		}
		for (int i = 0; i < b.m; i++) {
			for (int j = 0; j < b.n; j++) {
				c.d[i][j+a.n] = b.d[i][j];
			}
		}
		return c;
	}
	
	public Matrix2d catHoriz(Matrix2d a) {
		return Matrix2d.catHoriz(this, a);
	}
	
	public Matrix2d catVert(Matrix2d a) {
		return Matrix2d.catVert(this, a);
	}

	/**
	 * Concatenate matrix A:B into a new matrix 
	 * [A]
	 * [B]
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d catVert(Matrix2d a, Matrix2d b) {
		assert(a.n == b.n);
		Matrix2d c = new Matrix2d(a.m+b.m, a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[i][j] = a.d[i][j];
			}
		}
		for (int i = 0; i < b.m; i++) {
			for (int j = 0; j < b.n; j++) {
				c.d[i+a.m][j] = b.d[i][j];
			}
		}
		return c;
	}

	/**
	 * Returns a double[m*n] array representing this matrix. 
	 * @return array of values
	 */
	public double[] toArray() {
		double[] a = new double[n*m];
		int k = 0;
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				a[k] = d[i][j];
				k++;
			}
		}
		return a;
	}
	
	public Vect2 vect2() {
		assert(m > 1);
		assert(n > 0);
		return new Vect2(d[0][0], d[1][0]);
	}

	public Vect3 vect3() {
		assert(m > 2);
		assert(n > 0);
		return new Vect3(d[0][0], d[1][0], d[2][0]);
	}

//	public Vect4 vect4() {
//		assert(m > 3);
//		assert(n > 0);
//		return new Vect4(d[0][0], d[1][0], d[2][0], d[3][0]);
//	}

	/**
	 * Returns the double[m][n] array underlying this Matrix2d.  Changes to this data structure will be reflected in the patent Matrix2d. 
	 * @return array of values
	 */
	public double[][] toArray2() {
		return d;
	}

	/**
	 * Zero matrix.
	 * @param a rows
	 * @param b columns
	 * @return matrix
	 */
	public static Matrix2d ZERO(int a, int b) {
		return new Matrix2d(a,b);
	}

	/**
	 * Identity matrix of size n
	 * @param n number of rows and columns
	 * @return matrix
	 */
	public static Matrix2d I(int n) {
		Matrix2d c = new Matrix2d(n,n);
		for (int i = 0; i < c.m; i++) {
			for (int j = 0; j < c.n; j++) {
				if (i==j) c.d[i][j] = 1.0;
				else c.d[i][j] = 0.0;
			}
		}
		return c;
	}

	/**
	 * Matrix addition (explicit).
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d add(Matrix2d a, Matrix2d b) {
		assert(a.m == b.m);
		assert(a.n == b.n);
		Matrix2d c = new Matrix2d(a.m,a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[i][j] = a.d[i][j] + b.d[i][j];
			}
		}
		return c;
	}
	
	/**
	 * Returns the result of this + a
	 * @param a matrix
	 * @return resulting matrix
	 */
	public Matrix2d add(Matrix2d a) {
		return Matrix2d.add(this,a);
	}
	
	/**
	 * Matrix subtraction, A-B (explicit).
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d sub(Matrix2d a, Matrix2d b) {
		assert(a.m == b.m);
		assert(a.n == b.n);
		Matrix2d c = new Matrix2d(a.m,a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[i][j] = a.d[i][j] - b.d[i][j];
			}
		}
		return c;
	}
	
	/**
	 * Returns the result of this - a
	 * @param a matrix
	 * @return resulting matrix
	 */
	public Matrix2d sub(Matrix2d a) {
		return Matrix2d.sub(this,a);
	}
	
	/**
	 * Matrix multiplication (explicit)
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d mult(Matrix2d a, Matrix2d b) {
		assert(a.n == b.m);
		Matrix2d c = new Matrix2d(a.m,b.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < b.n; j++) {
				double s = 0.0;
				for (int k = 0; k < a.n	; k++) {
					s +=  a.d[i][k] * b.d[k][j];
				}
				c.d[i][j] = s; 
			}
		}
		return c;
	}

	/**
	 * Returns the result of this * a
	 * @param a matrix
	 * @return resulting matrix
	 */
	public Matrix2d mult(Matrix2d a) {
		return Matrix2d.mult(this,a);
	}

	/**
	 * Inner product (explicit)
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d innerProduct(Matrix2d a, Matrix2d b) {
		return Matrix2d.mult(a.trans(), b);
	}

	/**
	 * 1xN vector dot product
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static double dot(Matrix2d a, Matrix2d b) {
		assert(a.m == 1);
		return innerProduct(a,b).get(0, 0);
	}
	
	/**
	 * Outer product (explicit)
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d outerProduct(Matrix2d a, Matrix2d b) {
		return Matrix2d.mult(a,b.trans());
	}

	/**
	 * 1xN vector cross product
	 * @param a matrix
	 * @param b matrix
	 * @return resulting matrix
	 */
	public static Matrix2d cross(Matrix2d a, Matrix2d b) {
		assert(a.m == 1);
		return Matrix2d.mult(a,b.trans());
	}
	
	/**
	 * Matrix multiplication with a constant (explicit)
	 * @param a matrix
	 * @param x matrix
	 * @return resulting matrix
	 */
	public static Matrix2d mult(Matrix2d a, double x) {
		Matrix2d c = new Matrix2d(a.m,a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[i][j] = a.d[i][j] * x; 
			}
		}
		return c;
	}
	
	
	/**
	 * Returns the result of this * x
	 * @param x value
	 * @return resulting matrix
	 */
	public Matrix2d mult(double x) {
		return Matrix2d.mult(this,x);
	}

	private Matrix2d minor(int i, int j) {
		Matrix2d out = new Matrix2d(m-1, n-1);
		for (int k = 0; k < m-1; k++) {
			for (int l = 0; l < n-1; l++) {
				if (k < i) {
					if (l < j) {
						out.d[k][l] = d[k][l];
					} else {
						out.d[k][l] = d[k][l+1];						
					}
				} else {
					if (l < j) {
						out.d[k][l] = d[k+1][l];
					} else {
						out.d[k][l] = d[k+1][l+1];						
					}
				}
			}
		}
		return out;
	}
	
	public static double det(Matrix2d a) {
		assert(a.m == a.n);
		if (a.m < 2) {
			return 0.0;
		}
		if (a.m == 2) {
			return a.d[0][0] * a.d[1][1] - a.d[0][1] * a.d[1][0];
		}
		double r = 0;
		for (int i = 0; i < a.n; i++) {
			int sgn = 1;
			if (i%2 == 1) sgn = -1;
			double x = a.d[0][i];
			if (x != 0.0) {
//				double dsub; 
//				if (i == 0) {
//					dsub = Matrix2d.det(a.submatrix(1, 1, a.m-1, a.m-1));
//				} else if (i == a.m-1) {
//					dsub = Matrix2d.det(a.submatrix(1, 0, a.m-1, a.m-1));				
//				} else {
//					Matrix2d b1 = a.submatrix(1, 0, a.m-1, i);
//					Matrix2d b2 = a.submatrix(1, i+1, a.m-1, a.m-1-i);
//					dsub = Matrix2d.det(Matrix2d.catHoriz(b1, b2));
//				}			
//				r = r + sgn * x * dsub;

				r = r + sgn * x * Matrix2d.det(a.minor(0, i));
			}
		}
		return r;
	}


	public double det() {
		return Matrix2d.det(this);
	}

	public static Matrix2d inverse(Matrix2d a) {
		assert(a.m == a.n);
		double da = Matrix2d.det(a);
		if (da == 0.0) return null;
		Matrix2d b = new Matrix2d(a.m, a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				double sgn = 1;
				if (i%2 != j%2) sgn = -1;
				b.d[i][j] = sgn * Matrix2d.det(a.minor(i, j));
			}
		}
		return Matrix2d.mult(b.trans(), 1.0/da);
	}
	
	public Matrix2d inverse() {
		return Matrix2d.inverse(this);
	}
	
	/**
	 * Matrix transpose (explicit)
	 * @param a matrix
	 * @return resulting matrix
	 */
	public static Matrix2d trans(Matrix2d a) {
		Matrix2d c = new Matrix2d(a.m,a.n);
		for (int i = 0; i < a.m; i++) {
			for (int j = 0; j < a.n; j++) {
				c.d[j][i] = a.d[i][j]; 
			}
		}
		return c;
	}
	
	/**
	 * Returns the transpose of this matrix.
	 * @return resulting matrix
	 */
	public Matrix2d trans() {
		return Matrix2d.trans(this);
	}

	public String toString() {
		String s = "[ ";
		for (int i = 0; i < m; i++) {
			if (i != 0) s = s +"  ";
			s = s + "[";
			for (int j = 0; j < n; j++) {
				s = s + "\t" + f.Fm3(d[i][j]);
			}
			
			if (i == m-1) s = s +" ]";
			s = s + " ]\n";
		}
		return s;
	}
	
}
