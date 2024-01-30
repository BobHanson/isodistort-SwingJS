//
//Copyright 2001 Ken Perlin

package org.byu.isodistort.local;

import java.util.Arrays;

/**
 * Provides functionality to manipulate vectors.
 */

public class MathUtil {

	/**
	 * static access only
	 */

	private MathUtil() {
	}

	/**
	 * Normalizes vector v to unit-length.
	 * 
	 * @param v a vector
	 */
	public static void norm3(double[] v) {
		scale3(v, 1/ len3(v), v);
	}

	public static double len3(double[] xyz) {
		return Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
	}

	public static double lenSq3(double[] xyz) {
		return xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2];
	}


	/**
	 * Computes the dot product of vectors a and b. Vectors a and b must be of the
	 * same length.
	 * 
	 * @param a source vector
	 * @param b source vector
	 * @return the result of a dot b
	 */
	public static double dot3(double[] a, double[] b) {
		double sum = 0;
		for (int i = 3; --i >= 0;)
			sum += a[i] * b[i];
		return sum;
	}

//	/**
//	 * Computes the cross-product of two vectors a and b and stores the result in
//	 * dst. a, b, and dst must be 3 dimensional vectors.
//	 * 
//	 * @param a   source vector 1
//	 * @param b   source vector 2
//	 * @param dst resulting vector from a cross b Branton: this actually computes
//	 *            bxa, use with care.
//	 */
//	public static void cross(double[] a, double[] b, double[] dst) {
//		dst[0] = b[1] * a[2] - b[2] * a[1];
//		dst[1] = b[2] * a[0] - b[0] * a[2];
//		dst[2] = b[0] * a[1] - b[1] * a[0];
//	}
//
	/**
	 * Computes the cross-product of two vectors a and b and stores the result in
	 * dst. a, b, and dst must be 3 dimensional vectors. I created this because
	 * Perlin's version actually calculates bxa = -axb. -- Branton Campbell
	 * 
	 * @param a   source vector 1
	 * @param b   source vector 2
	 * @param dst resulting vector from a cross b
	 */
	public static void cross3(double[] a, double[] b, double[] dst) {
		dst[0] = b[2] * a[1] - b[1] * a[2];
		dst[1] = b[0] * a[2] - b[2] * a[0];
		dst[2] = b[1] * a[0] - b[0] * a[1];
	}

	/**
	 * Copies contents of the src vector to the dst vector. Both vectors must be of
	 * the same length.
	 * 
	 * @param src original vector
	 * @param dst copy of original vector
	 */
	public static void copyN(double[] src, double[] dst) {
		for (int i = src.length; --i >= 0;)
			dst[i] = src[i];
	}

	/**
	 * Copies contents of the src matrix to the dst matrix. Both matrices must be of
	 * the same dimensions. -- Branton Campbell
	 * 
	 * @param src original matrix
	 * @param dst copy of original matrix
	 */
	public static void mat3copy(double[][] src, double[][] dst) {
		for (int j = src.length, inlen = src[0].length; --j >= 0;)
			for (int i = inlen; --i >= 0;)
				dst[i][j] = src[i][j];
	}

	/**
	 * Populates the dst vector with values x, y, z.
	 * 
	 * @param dst vector to be populated
	 * @param x   component 0
	 * @param y   component 1
	 * @param z   component 2
	 */
	public static void set3(double[] dst, double x, double y, double z) {
		dst[0] = x;
		dst[1] = y;
		dst[2] = z;
	}

	/**
	 * Rotates a vector about x or y or z axis
	 * 
	 * @param dst   vector to be rotated
	 * @param axis  of rotation: 0=x, 1=y, 2=z
	 * @param angle in radians
	 */
	public static void rotate(double dst[], int axis, double angle) {
		int i = (axis + 1) % 3, j = (axis + 2) % 3;
		double c = Math.cos(angle), s = Math.sin(angle);
		double u = dst[i], v = dst[j];
		dst[i] = c * u - s * v;
		dst[j] = s * u + c * v;
	}

	/**
	 * Add two vectors, or apply a scalar while multiplying the 2nd by a constant -- Branton Campbell
	 * 
	 * @param src1 original vector 1
	 * @param src2 original vector 2; if null, then this is a scalar addition
	 * @param dst  output vector
	 */
	public static void vecaddN(double[] src1, double const2, double[] src2, double[] dst) {
		if (src2 == null) {
			for (int i = src1.length; --i >= 0;)
				dst[i] = src1[i] + const2;			
		} else {
			for (int i = Math.min(dst.length,  src2.length); --i >= 0;)
				dst[i] = src1[i] + const2 * src2[i];
		}
	}

	/**
	 * Tranpose a matrix -- Branton Campbell
	 * 
	 * @param dst transposed output matrix
	 * @param mat input matrix
	 */
	public static void mat3transpose(double mat[][], double dst[][]) {
		for (int j = 3; --j >= 0;)
			for (int i = 3; --i >= 0;)
				dst[i][j] = mat[j][i];
	}

	/**
	 * Apply a matrix transformation to a vector.
	 * 
	 * @param dst  transformed vector
	 * @param mat  transformation matrix
	 * @param vect input vector Branton Campbell
	 */
	public static void mat3mul(double mat[][], double vect[], double dst[]) {
		for (int i = 3; --i >= 0;)
			dst[i] = dot3(mat[i], vect);
	}


	public static void mat3inverse(double mat[][], double dst[][], double[] tempvec, double[][] tempmat) {
		double determinant = mat3determinant(mat, tempvec);
		for (int i = 0; i < 3; i++)
			cross3(mat[i], mat[(i + 1) % 3], tempmat[(i + 2) % 3]);
		for (int i = 0; i < 3; i++)
			for (int j = 0; j < 3; j++)
				dst[i][j] = tempmat[j][i] / determinant;
	}

	/**
	 * Determinant a 3x3 matrix -- Branton Campbell
	 * 
	 * @param determinant matrix determinant
	 * @param mat         input matrix Accounts for the fact that "cross" is
	 *                    backwards.
	 */
	public static double mat3determinant(double mat[][], double[] tempvec) {
		cross3(mat[0], mat[1], tempvec);
		return dot3(tempvec, mat[2]);
	}

	/**
	 * Trace a 3x3 matrix -- Branton Campbell
	 * 
	 * @param trace sum of diagonal elements
	 * @param mat   input matrix
	 */
	public static double mat3trace(double mat[][]) {
		double trace;

		trace = mat[0][0] + mat[1][1] + mat[2][2];
		return trace;
	}

	/**
	 * Multiply two square matrices.
	 * 
	 * @param dst  output matrix
	 * @param mat1 original matrix 1
	 * @param mat2 original matrix 2 Branton Campbell
	 */
	public static void mat3product(double mat1[][], double mat2[][], double dst[][], double[][] tempmat) {
		mat3transpose(mat2, tempmat);
		for (int j = 3; --j >= 0;)
			for (int i = 3; --i >= 0;)
				dst[i][j] = dot3(mat1[i], tempmat[j]);
	}

	/**
	 * Convert a symmetric tensor in Voigt form into matrix form -- Branton Campbell
	 * 
	 * @param voigt is the voigt form of the tensor
	 * @param mat   input matrix
	 * @param mXX   set to one for "plus identity"
	 * @param return mat[][]
	 */
	public static double[][] voigt2matrix(double voigt[], double mat[][], int mXX) {
		mat[0][0] = voigt[0] + mXX;
		mat[0][1] = voigt[5] / 2;
		mat[0][2] = voigt[4] / 2;
		mat[1][0] = voigt[5] / 2;
		mat[1][1] = voigt[1] + mXX;
		mat[1][2] = voigt[3] / 2;
		mat[2][0] = voigt[4] / 2;
		mat[2][1] = voigt[3] / 2;
		mat[2][2] = voigt[2] + mXX;
		return mat;
	}

	/**
	 * Copies 3-vector from second parameter triad to first
	 * @param src
	 * @param dest
	 * 
	 * @author Bob Hanson
	 */
	public static void set3(double[] src, double[] dest) {
		dest[0] = src[0];
		dest[1] = src[1];
		dest[2] = src[2];
	}

	/**
	 * Calculate srca + s * srcb and place the result in dest
	 * 
	 * @param srca
	 * @param s scaling factor to multiply srcb by
	 * @param srcb
	 * @param dest
	 */
	public static void scaleAdd3(double[] srca, double s, double[] srcb, double[] dest) {
		dest[0] = srca[0] + s * srcb[0];
		dest[1] = srca[1] + s * srcb[1];
		dest[2] = srca[2] + s * srcb[2];
	}
	
	/**
	 * Calculate (src1 + src2)/2 and place the result in dest.
	 * 
	 * @param src1
	 * @param src2
	 * @param dest
	 */
	public static void average3(double[] src1, double[] src2, double[] dest) {
		for (int i = 0; i < 3; i++)
			dest[i] = (src1[i] + src2[i]) / 2;
	}

	/**
	 * Set an arbitrarily long vector to a scalar (0, probably).
	 * 
	 * @param vec
	 * @param val
	 */
	public static void vecfill(double[] vec, double val) {
		for (int i = vec.length; --i >= 0;)
			vec[i] = val;
	}

	/**
	 * Expands the minimum (minmax[0]) and maximum (minmax[1]) to contain vec.
	 * 
	 * @param vec
	 * @param minmax
	 */
	public static void rangeCheck(double[] vec, double[][] minmax) {
		for (int i = 0; i < 3; i++) {
			if (vec[i] < minmax[0][i])
				minmax[0][i] = vec[i];
			if (vec[i] > minmax[1][i])
				minmax[1][i] = vec[i];
		}
	}

	/**
	 * Round to the specified number of decimal places, right-filling with zeros if
	 * necessary, and then also left- or right-align within the given width with
	 * spaces, if necessary.
	 * 
	 * 
	 * @param val
	 * @param nDec number of post-decimal values
	 * @param w    width; 0 for no spaces, positive for right-alignment, negative
	 *             for left-alignment, from -12 to 12
	 * @return aligned and rounded value
	 */
	public static String varToString(double val, int nDec, int w) {
		// rounding
		boolean leftAlign = (w < 0);
		w = Math.min(Math.abs(w), 12);		
		val = Math.round (val / decimalScale[nDec]) * decimalScale[nDec];
		String s = Double.toString(val);
		// ---0.234; len = 5, nInt = 2, nFrac = 3
		int nInt = s.indexOf('.') + 1;  // includes decimal point
		int nFrac = s.length() - nInt;
		if (nDec > nFrac) {
			// right-pad with zeros
			s += "000000000000".substring(0, nDec - nFrac);
		} else if (nFrac > nDec) {
			// 0.12345678 rounded already
			s = s.substring(0, nInt + nDec);	
		}		
		if (w > s.length()) {
			String b = "            ".substring(0, w - s.length());
			s = (leftAlign ? s + b : b + s);
		}
		return s;
	}

	final static double[] decimalScale = { //
			1., //
			0.1, //
			0.01, //
			0.001, //
			0.0001, //
			0.00001, //
			0.000001, //
			0.0000001, //
			0.00000001, //
			0.000000001, //
			0.0000000001, //
			0.00000000001, //
			0.000000000001, //
			0.0000000000001, //
			0.00000000000001, //
			0.000000000000001, //
	};


//	static {
//		for (int n = 0; n < 6; n++) {
//			System.out.println(">" + varToString(-0.12345678, n, -8) + "<");
//		}
//		System.out.println("XI");
//	}
//
	public static void copy3(double[] src, double[] dest) {
		dest[0] = src[0];
		dest[1] = src[1];
		dest[2] = src[2];
	}

	/**
	 * Compare the length of v (or distance from origin) squared to d2 and return the largest 
	 * @param p
	 * @param d2
	 * @return
	 */
	public static double maxlen2(double[] p, double d2) {
		double d = MathUtil.lenSq3(p);
		return (d > d2 ? d : d2);
	}

	public static void scale3(double[] src, double d, double[] dest) {
		dest[0] = src[0] * d;
		dest[1] = src[1] * d;
		dest[2] = src[2] * d;
	}

	public static double dist3(double[] a, double[] b) {
		double dx = a[0] - b[0];
		double dy = a[1] - b[1];
		double dz = a[2] - b[2];
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public static String vecToString(double[] v) {
		String s = "[";
		String sep = "";
		for (int i = 0; i < v.length; i++) {
			s += sep + v[i];
			sep = ",";
		}
		return s+"]";
	}

	public static boolean approxEqual(double a, double b, double tol) {
		return (Math.abs(a - b) < tol);
	}

	public static String a2s(double[] a) {
		return Arrays.toString(a);
	}

	/**
	 * Unitize a point IN PLACE and return the point
	 * integerized and scaled by 1/tol. 
	 * For example, [-0.4, 0.0, 1.0] becomes [600, 0, 0]
	 * @param p
	 * @param tol
	 * @return point with range [0,1/tol)
	 */
	public static double[] unitize3(double[] p, double tol) {
		for (int i = 0; i < 3; i++) {
			double x = (p[i] - Math.floor(p[i]));
		    if (x > 1 - tol || x < tol)
			      x = 0;
			p[i] = Math.round(x/tol);
		}
		return p;
	}

//	  static {
//		  System.out.println(a2s(unitize3(new double[] {-0.4, 1.001, 1.1}, 0.001)));
//		  System.out.println(a2s(unitize3(new double[] {-0.0001, 0.999, 1.1}, 0.001)));
//		  System.out.println(a2s(unitize3(new double[] {-1, 0, 1}, 0.001)));
//		  System.out.println(a2s(unitize3(new double[] {-0.4, 1.001, 1.1}, 0.001)));
//	  }

}

