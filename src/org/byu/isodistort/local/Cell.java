package org.byu.isodistort.local;

import java.awt.Color;
import java.util.List;

import javax.swing.JLabel;

import org.byu.isodistort.local.Variables.SymopData;

/**
 * The superclass of ParentCell and ChildCell
 * 
 * @author hanso
 *
 */
public abstract class Cell {

	/**
	 * A subclass of Cell that handles all parent-specific aspects.
	 * 
	 * @author hanso
	 *
	 */
	public static class ParentCell extends Cell {
	
		/**
		 * InverseTranspose of ChildCell.conv2convParentTransposeP:
		 * 
		 * ChildCell.conv2convParentTransposeP is the transpose of P_convchild2convparent
		 * from parsed value of "parentbasis". 
		 * 
		 * ChildCell.conv2convParentTransposeP^t*i is
		 * P_convparent2convchild.
		 * 
		 * parentCell.basisCart * ChildCell.conv2convParentTransposeP^t*i = childCell.basisCart
		 * 
		 */
		double[][] conv2convChildTransposeP;
	
		/**
		 * cell origin relative to child cell origin on the unitless child lattice basis.
		 * [x, y, z];
		 * 
		 * for parent, from ISOVIZ file "parentorigin"
		 * 
		 */
		double[] originUnitless = new double[3];
	
		/**
		 * Original unstrained cell parameters [A, B, C, ALPHA, BETA, GAMMA];
		 * 
		 * set by parser for parent, from ISOVIZ file "parentcell"
		 */
		double[] latt0 = new double[6];
	
		ParentCell() {
			labelText = "  Pcell";
			color = Variables.COLOR_PARENT_CELL;
		}
	
		/**
		 * Generate the Cartesian basis matrix from the 3x3 matrix
		 * childCell.conv2convParentTransposeP.
		 * 
		 * From parser.
		 * 
		 * parent cell only
		 * 
		 * @param isRhomb
		 * @param convChild2Parent
		 */
		void setUnstrainedCartsianBasis(boolean isRhomb, double[][] convChild2Parent) {
			// parent only
			conv2convChildTransposeP = new double[3][3];
			MathUtil.mat3transpose(convChild2Parent, t);
			MathUtil.mat3inverse(t, conv2convChildTransposeP, t3, t2);
			if (isRhomb) {
				double temp1 = Math.sin(latt0[GAMMA] / 2);
				double temp2 = Math.sqrt(1 / temp1 / temp1 - 4.0 / 3.0);
				temp1 *= latt0[A];
				basisCart0[0][0] = temp1 * (1);
				basisCart0[0][1] = temp1 * (-1);
				basisCart0[0][2] = temp1 * (0);
				basisCart0[1][0] = temp1 * (1 / Math.sqrt(3));
				basisCart0[1][1] = temp1 * (1 / Math.sqrt(3));
				basisCart0[1][2] = temp1 * (-2 / Math.sqrt(3));
				basisCart0[2][0] = temp1 * temp2;
				basisCart0[2][1] = temp1 * temp2;
				basisCart0[2][2] = temp1 * temp2;
			} else {
				// defined column by column
				double d = Math.cos(latt0[BETA]);
				double temp1 = (Math.cos(latt0[ALPHA]) - d * Math.cos(latt0[GAMMA])) / Math.sin(latt0[GAMMA]);
				d = 1 - d * d - temp1 * temp1;
				double temp2 = Math.sqrt(d < 0 ? 0 : d);
				basisCart0[0][0] = latt0[A];
				basisCart0[1][0] = 0;
				basisCart0[2][0] = 0;
				basisCart0[0][1] = latt0[B] * Math.cos(latt0[GAMMA]);
				basisCart0[1][1] = latt0[B] * Math.sin(latt0[GAMMA]);
				basisCart0[2][1] = 0;
				basisCart0[0][2] = latt0[C] * Math.cos(latt0[BETA]);
				basisCart0[1][2] = latt0[C] * temp1;
				basisCart0[2][2] = latt0[C] * temp2;
			}
		}
	
		@Override
		public String toString() {
			return "[ParentCell]";
		}
	
	}

	/**
	 * A subclass of Cell that handles all child-specific aspects.
	 * 
	 * @author hanso
	 *
	 */
	public static class ChildCell extends Cell {
	
		
		/**
		 * row matrix of conventional parent basis vectors on the conventional child unitless-lattice basis 
		 * [basis vector][x,y,z]; 
		 * 
		 * set by parser from ISOVIZ "parentbasis" value
		 * 
		 * used throughout IsoDiffractApp
		 * 
		 */
		public double[][] conv2convParentTransposeP = new double[3][3];
	
		/**
		 * Inverse of basisCart in Inverse-Angstrom units [basis vector][x,y,z]
		 */
		double[][] basisCartInverse = new double[3][3];
	
	
		ChildCell() {
			labelText = "  Ccell";
			color = Variables.COLOR_CHILD_CELL; 
		}
	
		/**
		 * Calculate the strained or unstrained metric tensor along
		 * with the lattice-to-cartesian matrix.
		 * 
		 * Also fill the reciprocol-to-Cartesian matrix
		 * 
		 * child only
		 * 
		 * @param matChildReciprocal2cart
		 * @param metric
		 * @param isStrained
		 * @param tempmat
		 */
	
		public void setMetricTensor(double[][] matChildReciprocal2cart, double[][] metric, boolean isStrained) {
			// Determine the unstrained or strained metric tensor
			MathUtil.mat3inverse((isStrained ? basisCart : basisCart0), t, t3, t2);
			// cart^-1 -> t
			MathUtil.mat3transpose(t, matChildReciprocal2cart);				
			// B* = Transpose(Inverse(B))
			MathUtil.mat3product(t, matChildReciprocal2cart, metric, t4); // G* = Transpose(B*).(B*)
		}
	
		/**
		 * Convert the fractional coords to cartesian, placing the result in a temporary
		 * variable.
		 * 
		 * child only
		 * 
		 * @param xyz fractional coordinates, unchanged
		 * @return TEMPORARY [x,y,z]
		 */
		public double[] toTempCartesian(double[] fxyz) {
			MathUtil.mat3mul(basisCart, fxyz, t3);
			return t3;
		}
	
		/**
		 * returns a TEMPORARY matrix
		 * 
		 * child only
		 * 
		 * @param info
		 * @return
		 */
		double[][] getTempStrainedCartesianBasis(double[] info) {
			MathUtil.voigt2matrix(info, t, 0);
			MathUtil.mat3product(basisCart, t, t2, t4);
			MathUtil.copyNN(t2, t);
			MathUtil.mat3product(t, basisCartInverse, t2, t4);
			return t2;
		}
	
		
		/**
		 * Used only for labels in the String panel. 
		 * 
		 * Convert a Voigt array to matrix format (V). Then apply 
		 * 
		 * StrainedCartBasis * V * StrainedCartBasis^-1
		 * 
		 * and return the sum of the squares of the nine elements of this matrix.
		 * 
		 * child cell only
		 * 
		 * @param v1
		 * @return isotropic parameter
		 */
		public double getIsotropicParameter(double[] v1) {
			MathUtil.voigt2matrix(v1, t, 0);
			MathUtil.mat3product(basisCart, t, t, t2);
			MathUtil.mat3product(t, basisCartInverse, t, t2);
			// ellipsoid in cartesian coords
			double d = 0;
			for (int i = 0; i < 3; i++) {
				for (int j = 0; j < 3; j++) {
					d += t[i][j] * t[i][j];
				}
			}
			return d;
		}
	
		@Override
		public String toString() {
			return "[ChildCell]";
		}

	}

	protected final static int A = 0, B = 1, C = 2, ALPHA = 3, BETA = 4, GAMMA = 5;

	final static int[] buildCell = new int[] { 0, 1, //
			2, 3, //
			4, 5, //
			6, 7, //
			0, 2, //
			1, 3, //
			4, 6, //
			5, 7, //
			0, 4, //
			1, 5, //
			2, 6, //
			3, 7 };

	JLabel title;

	JLabel[] labels = new JLabel[6];

	/**
	 * row matrix of primitive basis vectors 
	 * on the conventional unitless-lattice basis
	 * [basis vector][x,y,z];
	 * 
	 * set by parser from ISOVIZ "conv2primparentbasis"
	 * and "conv2primchildbasis" values, respectively. 
	 *
	 * used in IsoDiffractApp.assignPeakTypes()
	 * 
	 */
	public double[][] conv2primTransposeP;

	/**
	 * The list of non-point-group symmetry operations for this space group,
	 * including nCenteringOps at the end of the list.
	 * 
	 * Note that if every we wanted to actually list all of the 
	 * operations, we would need to expand the list by multiplying
	 * the N-nCenteringOps operations by each centering op.
	 * 
	 */
	public List<SymopData>  symopData;
	
	/**
	 * Array of Cartesian vertices of strained window-centered cell. [edge
	 * number][x, y, z]. 
	 * 
	 * The first four are [0 0 0], [1 0 0], [0 1 0], and [0 0 1]
	 * 
	 * Set in setVertices, from Vaiables.recalDdistortion().
	 * 
	 */
	double[][] cartesianVertices = new double[8][3];

    public double[] getCartesianVertex(int i) {
		return cartesianVertices[i];
	}

    /**
     * return the origin (i== 0) x, y, or z axis (1,2,4)
     * @param i
     * @return axis vector
     */
	public double[] getCartesianAxisTemp(int i) {
		MathUtil.vecaddN(cartesianVertices[i], -1, cartesianVertices[0], t3);
		return t3;
	}



	/**
	 * Strained cell origin relative to strained child cell origin in cartesian
	 * Angstrom coords. [x, y, z]; will be (0,0,0) for child
	 * 
	 */
	protected double[] originCart = new double[3];

	/**
	 * Matrix of unstrained basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless unstrained direct-lattice child cell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 * set by parser
	 * 
	 */
	protected double[][] basisCart0 = new double[3][3];

	/**
	 * Matrix of strained basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless strained direct-lattice child cell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] basisCart = new double[3][3];

	/**
	 * Final information needed to render the cell (last 12). [edge number][x, y, z,
	 * x-angle orientation, y-angle orientation, length]
	 * 
	 */
	private double[][] cellInfo = new double[12][6];

	/**
	 * Final information needed to render unit cell axes [axis number][x, y, z,
	 * x-angle , y-angle, length, pt1x, pt1y, pt1z, p2x, pt2y, pt2z]
	 * 
	 */
	private double[][] axesInfo = new double[3][12];

	public String labelText;

	public Color color;;

	// temporary vector and matrices

	protected double[] t3 = new double[3];

	protected double[][] t = new double[3][3], t2 = new double[3][3], t4 = new double[3][3];

	Cell() {
	}
	
	/**
	 * Query the cell for its vertices, adding them to max variable;
	 * used for determining the field of view that keeps the full unit cell in view
	 * when rotating with the mouse.
	 * 
	 * parent and child
	 * 
	 * @param d2 current max
	 * @return  adjusted max
	 */
	public double addRange2(double d2) {
		for (int i = 8; --i >= 0;) {
			d2 = MathUtil.maxlen2(cartesianVertices[i], d2);
		}
		return d2;
	}

	/**
	 * Get the transpose of the strained Cartesian basis.
	 * The product of this matrix with [u v w] provides the desired camera direction.
	 * 
	 * parent and child
	 * 
	 * @return transpose of strained Cartesian basis
	 */
	public double[][] getTempTransposedCartesianBasis() {
		MathUtil.mat3transpose(basisCart, t);
		return t;
	}

	/**
	 * Get the transpose of the inverse of the strained Cartesian basis.
	 * The product of this matrix with [h k l] provides the desired camera direction.
	 * 
	 * parent and child
	 * 
	 * @return transpose of the inverse of the strained Cartesian basis
	 */
	public double[][] getTempTransposedReciprocalBasis() {
		MathUtil.mat3inverse(basisCart, t, t3, t2);
		MathUtil.mat3transpose(t, t2);
		return t2;
	}

	/**
	 * parent and child
	 * 
	 * @param i
	 * @return cellInfo[i]
	 */
	public double[] getCellInfo(int i) {
		return cellInfo[i];
	}

	/**
	 * Get parent or child axis info:
	 * 
	 * [0] [axis number]
	 * 
	 * [1] [x1, y1, z1]
	 * 
	 * [2] [x2, y2, z2]
	 * 
	 * [3] [x-angle , y-angle, length]

	 * 
	 * [start end] Cartesian coordinates.
	 * 
	 * @param i 
	 * @return start or end coordinate
	 */
	public double[] getAxisInfo(int i) {
		return axesInfo[i];
	}

	/**
	 * parent and child
	 */
	protected void setVertices() {
		for (int ix = 0; ix < 2; ix++) {
			for (int iy = 0; iy < 2; iy++) {
				for (int iz = 0; iz < 2; iz++) {
					for (int i = 0; i < 3; i++) {
						cartesianVertices[ix + 2 * iy + 4 * iz][i] = originCart[i] + ix * basisCart[i][0]
								+ iy * basisCart[i][1] + iz * basisCart[i][2];
					}
				}
			}
		}
	}

	/**
	 * Place the center of the cell at the origin.
	 * 
	 * parent and child
	 * 
	 */
	void setRelativeTo(double[] c) {
		for (int j = 0; j < 8; j++) {
			double[] v = cartesianVertices[j];
			MathUtil.scaleAdd3(v, -1, c, v);
		}
	}

	/**
	 * parent and child
	 * 
	 * @param minmax
	 */
	void rangeCheckVertices(double[][] minmax) {
		for (int i = 8; --i >= 0;) {
			MathUtil.rangeCheck(cartesianVertices[i], minmax);
		}
	}

	/**
	 * parent and child
	 */
	protected void setLatticeParameterLabels() {
		double[][] t = new double[3][3];
		double[] v = new double[6];
		MathUtil.mat3transpose(basisCart, t);
		v[A] = MathUtil.len3(t[A]);
		v[B] = MathUtil.len3(t[B]);
		v[C] = MathUtil.len3(t[C]);
		v[ALPHA] = Math.acos(MathUtil.dot3(t[B], t[C]) / Math.max(v[B] * v[C], 0.001));
		v[BETA] = Math.acos(MathUtil.dot3(t[A], t[C]) / Math.max(v[A] * v[C], 0.001));
		v[GAMMA] = Math.acos(MathUtil.dot3(t[A], t[B]) / Math.max(v[A] * v[B], 0.001));
		for (int n = 0; n < 3; n++) {
			labels[n].setText(MathUtil.varToString(v[n], 2, -5));
			labels[n + 3].setText(MathUtil.varToString((180 / Math.PI) * v[n + 3], 2, -5));
		}
	}

	/**
	 * Calculate positions for the axes of this cell.
	 * Fills in [6]-[11] of axisInfo[x|y|z]
	 * 
	 * parent and child
	 * 
	 * @param axis x,y,z (0,1,2)
	 * @param center Cartesian center
	 * @param d1
	 * @param tA    start pt
	 * @param d2
	 * @param tB    end pt
	 * @param tempPt
	 */
	double[] getAxisExtents(int axis, double[] center, double d1, double[] tA, double d2, double[] tB,
			double[] tempPt) {
		// -----o----------a-----======>
		// ----------------|-----|tA
		// ----------------|-d1->
		// ----------------|-----d2---->
		// ----------------|-----------|tB
		//
		// center
		//
		// t3 = a
		// tempPt = o + a - c
		for (int i = 0; i < 3; i++) {
			t3[i] = basisCart[i][axis];
			tempPt[i] = originCart[i] + t3[i] - center[i];
		}
		MathUtil.norm3(t3);
		MathUtil.vecaddN(tempPt, d1, t3, tA);
		MathUtil.vecaddN(tempPt, d2, t3, tB);
		int pt = 6;
		double[] info = axesInfo[axis];
		info[pt++] = tA[0];
		info[pt++] = tA[1];
		info[pt++] = tA[2];
		info[pt++] = tB[0];
		info[pt++] = tB[1];
		info[pt++] = tB[2];
		return info;
	}

	public SymopData getSystematicallAbsentOp(double[] hkl) {
		for (int i = 0, n = symopData.size(); i < n; i++) {
			SymopData data = symopData.get(i);
			if (data.isSystematicAbsence(hkl)) {
				return data;
			}
		}
		return null;
	}

}