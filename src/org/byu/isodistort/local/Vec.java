//
//Copyright 2001 Ken Perlin

package org.byu.isodistort.local;

/**
 * Provides functionality to manipulate vectors.
 */

public class Vec {
//----- SIMPLE CLASS TO HANDLE BASIC VECTOR OPERATIONS -----

	/**
	 * Normalizes vector v to unit-length.
	 * 
	 * @param v a vector
	 */
	public static void normalize(double[] v) {
		double s = norm(v);
		if (s != 0)
			for (int i = v.length; --i >= 0;)
				v[i] /= s;
	}	

	public static double len3(double[] xyz) {
		return Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
	}
	
	public static double lenSq3(double[] xyz) {
		return xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2];
	}
	
	/**
	 * Computes the magnitude of the vector.
	 * 
	 * @param v a vector
	 * @return the magnitude of vector v
	 */
	public static double norm(double[] v) {
		return Math.sqrt(dot(v, v));
	}

	/**
	 * Computes the dot product of vectors a and b. Vectors a and b must be of the
	 * same length.
	 * 
	 * @param a source vector
	 * @param b source vector
	 * @return the result of a dot b
	 */
	public static double dot(double[] a, double[] b) {
		double sum = 0;
		for (int i = b.length; --i >= 0;)
			sum += a[i] * b[i];
		return sum;
	}

	/**
	 * Computes the cross-product of two vectors a and b and stores the result in
	 * dst. a, b, and dst must be 3 dimensional vectors.
	 * 
	 * @param a   source vector 1
	 * @param b   source vector 2
	 * @param dst resulting vector from a cross b Branton: this actually computes
	 *            bxa, use with care.
	 */
	public static void cross(double[] a, double[] b, double[] dst) {
		dst[0] = b[1] * a[2] - b[2] * a[1];
		dst[1] = b[2] * a[0] - b[0] * a[2];
		dst[2] = b[0] * a[1] - b[1] * a[0];
	}

	/**
	 * Computes the cross-product of two vectors a and b and stores the result in
	 * dst. a, b, and dst must be 3 dimensional vectors. I created this because
	 * Perlin's version actually calculates bxa = -axb. -- Branton Campbell
	 * 
	 * @param a   source vector 1
	 * @param b   source vector 2
	 * @param dst resulting vector from a cross b
	 */
	public static void mycross(double[] a, double[] b, double[] dst) {
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
	public static void copy(double[] src, double[] dst) {
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
	public static void matcopy(double[][] src, double[][] dst) {
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
	public static void set(double[] dst, double x, double y, double z) {
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
	 * Add two vectors, while multiplying the 2nd by a constant -- Branton Campbell
	 * 
	 * @param src1 original vector 1
	 * @param src2 original vector 2
	 * @param dst  output vector
	 */
	public static void vecadd(double[] src1, double const1, double[] src2, double const2, double[] dst) {
		for (int i = src1.length; --i >= 0;)
			dst[i] = const1 * src1[i] + const2 * src2[i];
	}

	/**
	 * Tranpose a matrix -- Branton Campbell
	 * 
	 * @param dst transposed output matrix
	 * @param mat input matrix
	 */
	public static void mattranspose(double mat[][], double dst[][]) {
		for (int j = mat.length, inlen = mat[0].length; --j >= 0;)
			for (int i = inlen; --i >= 0;)
				dst[i][j] = mat[j][i];
	}

	/**
	 * Apply a matrix transformation to a vector.
	 * 
	 * @param dst  transformed vector
	 * @param mat  transformation matrix
	 * @param vect input vector Branton Campbell
	 */
	public static void matdotvect(double mat[][], double vect[], double dst[]) {
		for (int i = mat.length; --i >= 0;)
			dst[i] = dot(mat[i], vect);
	}

	/**
	 * Invert a 3x3 matrix -- Branton Campbell
	 * 
	 * @param dst inverted output matrix
	 * @param mat input matrix Accounts for the fact that "cross" is backwards; two
	 *            wrongs make a right.
	 */
	public static void matinverse(double mat[][], double dst[][]) {
		double determinant;
		double[] tempvec = new double[3];
		double[][] tempmat = new double[3][3];

		cross(mat[0], mat[1], tempvec);
		determinant = dot(tempvec, mat[2]);

		for (int i = 0; i < 3; i++)
			cross(mat[i], mat[(i + 1) % 3], tempmat[(i + 2) % 3]);

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
	public static double matdeterminant(double mat[][]) {
		double[] tempvec = new double[3];
		double determinant;

		cross(mat[0], mat[1], tempvec);
		determinant = -dot(tempvec, mat[2]);
		return determinant;
	}

	/**
	 * Trace a 3x3 matrix -- Branton Campbell
	 * 
	 * @param trace sum of diagonal elements
	 * @param mat   input matrix
	 */
	public static double mattrace(double mat[][]) {
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
	public static void matdotmat(double mat1[][], double mat2[][], double dst[][]) {
		int mlen = mat1.length;
		double[][] tmat2 = new double[mlen][mlen];
		mattranspose(mat2, tmat2);
		for (int j = mlen; --j >= 0;)
			for (int i = mlen; --i >= 0;)
				dst[i][j] = dot(mat1[i], tmat2[j]);
	}

	/**
	 * Convert a symmetric tensor in Voigt form into matrix form -- Branton Campbell
	 * 
	 * @param voigt is the voigt form of the tensor
	 * @param mat   input matrix
	 */
	public static void voigt2matrix(double voigt[], double mat[][]) {
		mat[0][0] = voigt[0];
		mat[1][1] = voigt[1];
		mat[2][2] = voigt[2];
		mat[1][2] = 0.5 * voigt[3];
		mat[2][1] = 0.5 * voigt[3];
		mat[0][2] = 0.5 * voigt[4];
		mat[2][0] = 0.5 * voigt[4];
		mat[0][1] = 0.5 * voigt[5];
		mat[1][0] = 0.5 * voigt[5];
	}

	/**
	 * Uses two xyz points to a calculate a bond. -- Branton Campbell
	 */
	public static void pairtobond(double[] atom1, double[] atom2, double[] bond) {
		int x = 0, y = 1, z = 2, X = 3, Y = 4;
		double lensq = 0;
		double orien[] = new double[3];

		for (int i = 0; i < 3; i++) {
			bond[i] = (atom2[i] + atom1[i]) / 2.0;
			orien[i] = (atom2[i] - atom1[i]);
			lensq += orien[i] * orien[i];
		}

		if (lensq < 0.000000000001) // BH Q: this is REALLY small!!! was sqrt() <= 0.000001
			return;

		for (int i = 0; i < 3; i++)
			orien[i] = orien[i] / Math.sqrt(lensq);
		bond[X] = -Math.asin(orien[y]);
		bond[Y] = Math.atan2(orien[x], orien[z]);
		bond[5] = Math.sqrt(lensq);
		bond[6] = 1.0;
	}

	/**
	 * Calculates the description used to render an arrow -- Branton Campbell
	 * 
	 * @param orien      is the vector input.
	 * @param arrowstuff is the [X-angle, Y-angle, Length] output.
	 */
	public static void calculatearrow(double[] orien, double[] arrowstuff) {
		int x = 0, y = 1, z = 2, X = 0, Y = 1, L = 2;
		double lensq = 0;

		for (int i = 0; i < 3; i++)
			lensq += orien[i] * orien[i];

		if (lensq < 0.000000000001) {
			arrowstuff[X] = 0;
			arrowstuff[Y] = 0;
			arrowstuff[L] = 0;
			return;
		}

		double d = Math.sqrt(lensq);
		for (int i = 0; i < 3; i++)
			orien[i] /= d;

		arrowstuff[X] = -Math.asin(orien[y]); // X rotation
		arrowstuff[Y] = Math.atan2(orien[x], orien[z]); // Y rotation
		arrowstuff[L] = d; // Length
	}

	/**
	 * Calculates the description used to render an ellipsoid -- Branton Campbell
	 * 
	 * @param mat       is the real-symmetric matrix input.
	 * @param ellipsoid is the [widthx, widthy, widthz, rotaxisx, rotaxisy,
	 *                  rotaxisz, angle] output.
	 */
	public static void calculateellipstuff(double[][] matrixform, double[] ellipstuff) {
		int wx = 0, wy = 1, wz = 2, dx = 3, dy = 4, dz = 5, ang = 6;
		double trc = 0, 
				//det = 0, lensq = 0, 
				rotangle = 0;
		double rotaxis[] = new double[3];
		double widths[] = new double[3];
//		double NV[][] = new double[3][3];
//		double ND[][] = new double[3][3];

//    System.out.println ("ellipmat: "+matrixform[0][0]+", "+matrixform[0][1]+", "+matrixform[0][2]+", "+matrixform[1][0]+", "+matrixform[1][1]+", "+matrixform[1][2]+", "+matrixform[2][0]+", "+matrixform[2][1]+", "+matrixform[2][2]);

		trc = mattrace(matrixform);
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

		ellipstuff[wx] = widths[0];
		ellipstuff[wy] = widths[1];
		ellipstuff[wz] = widths[2];
		ellipstuff[dx] = rotaxis[0];
		ellipstuff[dy] = rotaxis[1];
		ellipstuff[dz] = rotaxis[2];
		ellipstuff[ang] = rotangle % (2 * Math.PI) - Math.PI;

	}

	/**
	 * Copies 3-vector from second parameter triad to first
	 * 
	 * @param dest
	 * @param src
	 * 
	 * @author Bob Hanson
	 */
	public static void set3(double[] dest, double[] src) {
		dest[0] = src[0];
		dest[1] = src[1];
		dest[2] = src[2];
	}

} // end Vec class
