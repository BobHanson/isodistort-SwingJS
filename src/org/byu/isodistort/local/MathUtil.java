//
//Copyright 2001 Ken Perlin

package org.byu.isodistort.local;

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
		scale3(v, 1/ len3(v));
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

//	/**
//	 * Invert a 3x3 matrix -- Branton Campbell
//	 * 
//	 * @param dst inverted output matrix
//	 * @param mat input matrix Accounts for the fact that "cross" is backwards; two
//	 *            wrongs make a right.
//	 */
//	public static void xxxmatinverse(double mat[][], double dst[][]) {
//		
////		
////		matinverse2(mat, dst);
////		
////		System.out.println(toString(dst));
////
//		
//		double determinant;
//		double[] tempvec = new double[3];
//		double[][] tempmat = new double[3][3];
//
//		cross(mat[0], mat[1], tempvec);
//		determinant = dot3(tempvec, mat[2]);
//
//		for (int i = 0; i < 3; i++)
//			cross(mat[i], mat[(i + 1) % 3], tempmat[(i + 2) % 3]);
//
//		for (int i = 0; i < 3; i++)
//			for (int j = 0; j < 3; j++)
//				dst[i][j] = tempmat[j][i] / determinant;
//		
//		System.out.println(toString(dst));
//		return;
//		
//	}
	
//	  public static String toString(double[][] m) {
//		    String s = "[\n";
//		    for (int i = 0; i < 3; i++) {
//		      s += "  [";
//		      for (int j = 0; j < 3; j++)
//		        s += " " + m[i][j];
//		      s += "]\n";
//		    }
//		    s += "]";
//		    return s;
//		  }
//

	public static void mat3inverse(double mat[][], double dst[][]) {
		double determinant;
		double[] tempvec = new double[3];
		double[][] tempmat = new double[3][3];

		cross3(mat[0], mat[1], tempvec);
		determinant = dot3(tempvec, mat[2]);

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
	public static double mat3determinant(double mat[][]) {
		double[] tempvec = new double[3];
		double determinant;

		cross3(mat[0], mat[1], tempvec);
		determinant = dot3(tempvec, mat[2]);
		return determinant;
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
	public static void mat3product(double mat1[][], double mat2[][], double dst[][]) {
		int mlen = mat1.length;
		double[][] tmat2 = new double[mlen][mlen];
		mat3transpose(mat2, tmat2);
		for (int j = mlen; --j >= 0;)
			for (int i = mlen; --i >= 0;)
				dst[i][j] = dot3(mat1[i], tmat2[j]);
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
	 * Calculates the description used to render an arrow -- Branton Campbell
	 * 
	 * @param xyz       is the vector input.
	 * @param info is the [X-angle, Y-angle, Length] output.
	 */
	public static void calculateArrow(double[] xyz, double[] info) {
		int x = 0, y = 1, z = 2, X = 0, Y = 1, L = 2;
		double lensq = MathUtil.lenSq3(xyz);

		if (lensq < 0.000000000001) {
			info[X] = 0;
			info[Y] = 0;
			info[L] = 0;
			return;
		}

		double d = Math.sqrt(lensq);

// BH Q! scaling the input array??	
//
//		for (int i = 0; i < 3; i++)
//			xyz[i] /= d;
//
		// BH: added / d in asin. Why normalize this? 
		info[X] = -Math.asin(xyz[y] / d); // X rotation
		info[Y] = Math.atan2(xyz[x], xyz[z]); // Y rotation
		info[L] = d; // Length
	}

	/**
	 * Calculates the description used to render an ellipsoid -- Branton Campbell
	 * 
	 * @param mat       is the real-symmetric matrix input.
	 * @param ellipsoid is the [widthx, widthy, widthz, rotaxisx, rotaxisy,
	 *                  rotaxisz, angle] output.
	 */
	public static void calculateEllipsoid(double[][] matrixform, double[] info) {
		int wx = 0, wy = 1, wz = 2, dx = 3, dy = 4, dz = 5, ang = 6;
		double trc = 0,
				// det = 0, lensq = 0,
				rotangle = 0;
		double rotaxis[] = new double[3];
		double widths[] = new double[3];
//		double NV[][] = new double[3][3];
//		double ND[][] = new double[3][3];

//    System.out.println ("ellipmat: "+matrixform[0][0]+", "+matrixform[0][1]+", "+matrixform[0][2]+", "+matrixform[1][0]+", "+matrixform[1][1]+", "+matrixform[1][2]+", "+matrixform[2][0]+", "+matrixform[2][1]+", "+matrixform[2][2]);

		trc = mat3trace(matrixform);
//		det = matdeterminant(matrixform);
//		for (int i = 0; i < 3; i++)
//			for (int j = 0; j < 3; j++)
//				lensq += matrixform[i][j]* matrixform[i][j];
//
//		if ((Math.sqrt(lensq) < 0.000001) || (det < 0.000001) || true) // "true" temporarily bypasses the ellipoidal
		// analysis.
		{
			double avgrad = Math.sqrt(Math.abs(trc) / 3.0);
			widths[0] = avgrad;
			widths[1] = avgrad;
			widths[2] = avgrad;
			rotaxis[0] = 0;
			rotaxis[1] = 0;
			rotaxis[2] = 1;
			rotangle = 0;
//		} else {
//			Matrix jamat = new Matrix(matrixform);
//			EigenvalueDecomposition E = new EigenvalueDecomposition(jamat, true);
//			Matrix D = E.getD();
//			Matrix V = E.getV();
//			NV = V.getArray();
//			ND = D.getArray();
//
//			widths[0] = Math.sqrt((ND[0][0]));
//			widths[1] = Math.sqrt((ND[1][1]));
//			widths[2] = Math.sqrt((ND[2][2]));
//			rotangle = Math.acos(.5 * (NV[0][0] + NV[1][1] + NV[2][2] - 1));
//			rotaxis[0] = (NV[2][1] - NV[1][2]) / (2 * Math.sin(rotangle));
//			rotaxis[1] = (NV[0][2] - NV[2][0]) / (2 * Math.sin(rotangle));
//			rotaxis[2] = (NV[1][0] - NV[0][1]) / (2 * Math.sin(rotangle));
		}
//	System.out.println(ND[0][0]+" "+ND[0][1]+" "+ND[0][2]+" "+ND[1][0]+" "+ND[1][1]+" "+ND[1][2]+" "+ND[2][0]+" "+ND[2][1]+" "+ND[2][2]);
//	System.out.println(NV[0][0]+" "+NV[0][1]+" "+NV[0][2]+" "+NV[1][0]+" "+NV[1][1]+" "+NV[1][2]+" "+NV[2][0]+" "+NV[2][1]+" "+NV[2][2]);
//	System.out.println("lensq="+lensq+", det="+det+", w0="+widths[0]+", w1="+widths[1]+", w2="+widths[2]+", r0="+rotaxis[0]+", r1="+rotaxis[1]+", r2="+rotaxis[2]);

		info[wx] = widths[0];
		info[wy] = widths[1];
		info[wz] = widths[2];
		info[dx] = rotaxis[0];
		info[dy] = rotaxis[1];
		info[dz] = rotaxis[2];
		info[ang] = rotangle % (2 * Math.PI) - Math.PI;

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
	 * add srca and srcb; place the result in dest
	 * @param srca
	 * @param srcb
	 * @param dest
	 */
	public static void add3(double[] srca, double[] srcb, double[] dest) {
		dest[0] = srca[0] + srcb[0];
		dest[1] = srca[1] + srcb[1];
		dest[2] = srca[2] + srcb[2];
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

	public static void scale3(double[] a, double d) {
		a[0] *= d;
		a[1] *= d;
		a[2] *= d;
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


}
