package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.byu.isodistort.local.Bspt.CubeIterator;

public class Variables {

	public final static int DIS = Mode.DIS; // displacive
	public final static int OCC = Mode.OCC; // occupancy (aka "scalar")
	public final static int MAG = Mode.MAG; // magnetic
	public final static int ROT = Mode.ROT; // rotational
	public final static int ELL = Mode.ELL; // ellipsoidal
	private final static int MODE_ATOMIC_COUNT = Mode.MODE_ATOMIC_COUNT;

	public final static int STRAIN = Mode.STRAIN;
	public final static int IRREP = Mode.IRREP; // irreducible representations
	private final static int MODE_COUNT = Mode.MODE_COUNT;

	public final static int RX = 3, RY = 4, L_2 = 5;

	private IsoApp app;

	private VariableGUI gui;

	public Atom[] atoms;

	Mode[] modes = new Mode[MODE_COUNT];

	public String isoversion;

	/**
	 * only used to hide the checkboxes
	 * 
	 */
	private boolean isDiffraction;

	/**
	 * True initially or when a slider bar moves, false otherwise
	 */
	public boolean isChanged = true;

	/**
	 * Applet width and height in pixels
	 */
	public int appletWidth = 1024, appletHeight;
	/**
	 * Total number of atoms after filtering for primitive.
	 */
	public int numAtoms;

	/**
	 * Number of different atom types
	 */
	public int numTypes;

	/**
	 * Number of subtypes for each atom type.
	 */
	public int[] numSubTypes;

	/**
	 * the final public number of atoms for specific type and subtype; either
	 * numSubTypeAtomsRead or numPrimitiveSubAtoms
	 * 
	 */
	public int[][] numSubAtoms;

	/**
	 * Names for each atom type; [type]
	 */
	public String[] atomTypeName, atomTypeSymbol;
	/**
	 * Names for each atom type; [type]
	 */
	public String[][] subTypeName;
	/**
	 * Radius of atoms
	 */
	public double atomMaxRadius;
	/**
	 * Angstrom per Bohr Magneton for moment visualization
	 */
	public double angstromsPerMagneton;
	/**
	 * Angstrom per radian for rotation visualization
	 */
	public double angstromsPerRadian;
	/**
	 * Angstrom per Angstrom for ellipsoid visualization
	 */
	public double defaultUiso;
	/**
	 * Half of the maximum bond length in Angstroms beyond which bonds are not drawn
	 */
	public double halfMaxBondLength;
	/**
	 * Minimum atomic occupancy below which bonds are not drawn
	 */
	public double minBondOcc;
	/**
	 * Total number of bonds
	 */
	public int numBonds;
	/**
	 * bondInfo[bond index] = {avx, avy, avz, angleX, angleY, len^2, atom1 index, atom2 index, bond index}
	 * reference atomInfo (below)
	 * 
	 */
	public double[][] bondInfo; // [numBonds][9]

	/**
	 * If true (when at least two parents have the same element type) show the
	 * simple-color checkbox.
	 * 
	 */
	public boolean needSimpleColor;

	/**
	 * Original unstrained supercell lattice parameters
	 */
	public double[] sLatt0 = new double[6];
	/**
	 * Strained supercell parameters
	 */
	public double[] sLatt1 = new double[6];
	/**
	 * Original unstrained parent cell parameters
	 */
	public double[] pLatt0 = new double[6];
	/**
	 * Strained parent cell parameters
	 */
	public double[] pLatt1 = new double[6];
	/**
	 * Parent cell origin relative to supercell origin on the unitless superlattice
	 * basis. [x, y, z]
	 * 
	 */
	public double[] pOriginUnitless = new double[3];
	/**
	 * Row matrix of parent basis vectors on the unitless superlattice basis [basis
	 * vector][x,y,z]
	 * 
	 */
	public double[][] Tmat = new double[3][3];
	/**
	 * Transpose of Tmat: sBasisCart.Tmat^t = pBasisCart
	 */
	public double[][] TmatTranspose = new double[3][3];
	/**
	 * InverseTranspose of Tmat: pBasisCart.Tmat^t*i = sBasisCart
	 */
	public double[][] TmatInverseTranspose = new double[3][3];
	/**
	 * Strained parent cell origin relative to strained supercell origin in
	 * cartesian Angstrom coords. [x, y, z]
	 * 
	 */
	public double[] pOriginCart = new double[3];
	/**
	 * Center of supercell in strained cartesian Angstrom coords. [x, y, z]
	 */
	public double[] sCenterCart = new double[3];
	/**
	 * Matrix of unstrained super basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless unstrained direct-lattice supercell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] sBasisCart0 = new double[3][3];
	/**
	 * Matrix of strained super basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless strained direct-lattice supercell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] sBasisCart = new double[3][3];
	/**
	 * Inverse of sBasisCart in Inverse-Angstrom units [basis vector][x,y,z]
	 */
	public double[][] sBasisCartInverse = new double[3][3];
	/**
	 * Matrix of unstrained parent basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless unstrained direct-lattice parentcell
	 * coords into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] pBasisCart0 = new double[3][3];
	/**
	 * Matrix of strained parent basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless strained direct-lattice parentcell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] pBasisCart = new double[3][3];
	/**
	 * Matrix of strained super-cell crystal-axis basis vectors in unitless coords
	 * [basis vector][x,y,z] Transforms unitless strained direct-lattice supercell
	 * crystalaxis coords into cartesian coords [basis vector][x,y,z]
	 * 
	 */
	public double[][] sCrystalAxisBasisCart = new double[3][3];
	/**
	 * Array of Cartesian vertices of strained window-centered parent cell. [edge number][x,
	 * y, z]
	 * 
	 */
	public double[][] parentCellCartesianVertices = new double[8][3];
	/**
	 * Array of Cartesian vertices of strained window-centered super cell. [edge number][x, y,
	 * z]
	 * 
	 */
	public double[][] superCellCartesianVertices = new double[8][3];

	/**
	 * Is the parent expressed in a rhombohedral setting?
	 */
	public boolean isRhombParentSetting;

	/**
	 * 
	 * Number of irreps
	 * 
	 */
	public int numIrreps;

	/**
	 * Number of strains (could be 0)
	 */

	public int numStrains;

	/**
	 * Boolean variable that tracks whether irrep sliders were last set to sliderMax
	 * or to zero.
	 * 
	 */
	public boolean irrepSlidersOn = true;

	/**
	 * The master slider bar value.
	 */
	double superSliderVal;

	private boolean isAdjusting;

	private transient JPanel sliderPanel;

	/**
	 * just switching apps; fewer println calls
	 * 
	 */
	private boolean isSwitch;

	public Variables(IsoApp app, boolean isDiffraction, boolean isSwitch) {
		this.app = app;
		this.isSwitch = isSwitch;
		this.isDiffraction = isDiffraction;
	}

	/**
	 * This method accepts String, byte[] or InputStream and delivers back the
	 * byte[] for passing to a switched in app (ISODistort to ISODiffract, for
	 * example). If it is an InputStream it will close the stream.
	 * 
	 * @param datan
	 * @return byte[]
	 */
	public byte[] parse(Object data) {
		long t = System.currentTimeMillis();
		byte[] bytes = new VariableParser().parse(data);
		System.out.println("Variables parsed in " + (System.currentTimeMillis() - t) + " ms");
		gui = new VariableGUI();
		return bytes;
	}

	/**
	 * instantiates and initializes the scroll and control panels.
	 * 
	 * @param sliderPanel
	 * 
	 */
	public void initSliderPanel(JPanel sliderPanel) {
		this.sliderPanel = sliderPanel;
		gui.initPanels();
		sliderPanel = null;
	}

	public void setAnimationAmplitude(double animAmp) {
		gui.superSlider.setValue((int) Math.round(animAmp * gui.sliderMaxVal));
	}

	public boolean isSubTypeSelected(int t, int s) {
		return gui.subTypeBoxes[t][s].isSelected();
	}

	public void setSubtypeSelected(int t, int s, boolean b) {
		gui.subTypeBoxes[t][s].setSelected(b);
	}

	public Atom getAtom(int ia) {
		return atoms[ia];
	}

	public double[][] getAtomInfo(int a) {
		return atoms[a].info;
	}

	public void selectAllSubtypes(boolean b) {
		for (int t = 0; t < numTypes; t++)
			for (int s = 0; s < numSubTypes[t]; s++)
				setSubtypeSelected(t, s, b);
	}

	public double getSetSuperSliderValue(double newVal) {
		double d = superSliderVal;
		superSliderVal = newVal;
		return d;
	}

	public int getStrainModesCount() {
		return (modes[STRAIN] == null ? 0 : modes[STRAIN].count);
	}

	public double[] getStrainmodeSliderValues() {
		return (modes[STRAIN] == null ? new double[0] : modes[STRAIN].sliderValsTM[0]);
	}

	public void getColors(int t, double[] rgb) {
		Color c = getDefaultModeColor(t);
		rgb[0] = c.getRed() / 255.0;
		rgb[1] = c.getGreen() / 255.0;
		rgb[2] = c.getBlue() / 255.0;
	}

	/**
	 * calculates the parent atom colors and associated slideres
	 * 
	 * @param simpleColor true if subtypes are not differentiated with shading
	 */
	public void setColors(boolean simpleColor) {
		gui.setColors(simpleColor);
	}

	public void recolorPanels() {
		gui.recolorPanels();
	}

	/**
	 * readSliders reads in the current values of each of the sliders.
	 */
	public void readSliders() {
		gui.readSliders();
	}

	public void setValuesFrom(Variables v) {
		isAdjusting = true;
		gui.setComponentValuesFrom(v);
		gui.readSliders();
		isChanged = true;
		isAdjusting = false;
		app.updateDisplay();
	}

	public void resetSliders() {
		gui.resetSliders();
	}

	public void zeroSliders() {
		gui.zeroSliders();
	}

	public void toggleIrrepSliders() {
		gui.toggleIrrepSliders();
	}

	/**
	 * recalcDistortion recalculates the positions and occupancies based on current
	 * slider values.
	 * 
	 */
	public void recalcDistortion() {
		calculateStrain();
		calculateDistortions();
	}

	public void saveModeValues() {
		modes[DIS].saveMode();
		modes[OCC].saveMode();
		modes[MAG].saveMode();
	}

	public void randomizeGM1Values(Random rval) {
		modes[DIS].randomizeModes(rval, true);
		modes[OCC].randomizeModes(rval, true);
		modes[MAG].randomizeModes(rval, true);
	}

	public void randomizeNonGM1Values(Random rval) {
		modes[DIS].randomizeModes(rval, false);
		modes[OCC].randomizeModes(rval, false);
		modes[MAG].randomizeModes(rval, false);
	}

	public void restoreModeValues() {
		modes[DIS].restoreMode();
		modes[OCC].restoreMode();
		modes[MAG].restoreMode();
	}

	/**
	 * Naively calculate strained parent-cell basis vectors in cartesian Angstrom
	 * coords.
	 * 
	 * 
	 */
	private void calculateStrain() {

		// Calculate strained parent and supercell basis vectors in cartesian Angstrom
		// coordinates.

		double[][] pStrainPlusIdentity = (modes[STRAIN] == null
				? MathUtil.voigt2matrix(new double[6], new double[3][3], 1)
				: modes[STRAIN].getVoigtStrainTensor(superSliderVal, modes[IRREP]));
		MathUtil.mat3product(pStrainPlusIdentity, pBasisCart0, pBasisCart);
		MathUtil.mat3product(pBasisCart, TmatInverseTranspose, sBasisCart);
		MathUtil.recalculateLattice(pLatt1, pBasisCart);

		// calculate the strained parent and supercell lattice parameters [a, b, c,
		// alpha, beta, gamma]

		MathUtil.recalculateLattice(sLatt1, sBasisCart);
		MathUtil.mat3inverse(sBasisCart, sBasisCartInverse);

		// calculate the parent cell origin in strained cartesian Angtstrom coords
		MathUtil.mat3mul(sBasisCart, pOriginUnitless, pOriginCart);

		// calculate the 8 cell vertices in strained cartesian Angstrom coordinates
		for (int ix = 0; ix < 2; ix++) {
			for (int iy = 0; iy < 2; iy++) {
				for (int iz = 0; iz < 2; iz++) {
					for (int i = 0; i < 3; i++) {
						parentCellCartesianVertices[ix + 2 * iy + 4 * iz][i] = ix * pBasisCart[i][0]
								+ iy * pBasisCart[i][1] + iz * pBasisCart[i][2] + pOriginCart[i];
						superCellCartesianVertices[ix + 2 * iy + 4 * iz][i] = ix * sBasisCart[i][0]
								+ iy * sBasisCart[i][1] + iz * sBasisCart[i][2];
					}
				}
			}
		}
		recenterLattice();
		gui.setLattLabels(pLatt1, sLatt1);
	}

	private void calculateDistortions() {
		double[][] tempmat = new double[3][3];

		// pass an array to the modes that tracks the
		// max values that will be used in the labels
		// just easier to use a single oversize array

		int numSubMax = 0;
		for (int t = 0; t < numTypes; t++) {
			int n = numSubTypes[t];
			if (n > numSubMax)
				numSubMax = n;
		}
		double[][][] max = new double[numTypes][numSubMax][MODE_ATOMIC_COUNT];

		modes[DIS].calcDistortion(this, max, tempvec, null);
		modes[OCC].calcDistortion(this, max, null, null);
		modes[ROT].calcDistortion(this, max, tempvec, null);
		modes[MAG].calcDistortion(this, max, tempvec, null);
		modes[ELL].calcDistortion(this, max, tempvec, tempmat);

		for (int t = 0; t < numTypes; t++) {
			for (int s = 0; s < numSubTypes[t]; s++) {
				gui.setSubTypeText(t, s, max[t][s]);
			}
		}

	}

	/**
	 * Binary Space Partitioning Treeg
	 * 
	 */
	private Bspt bspt;

	private void recenterLattice() {
//      Calculate the center of the supercell in strained cartesian Angstrom coords (sOriginCart).
		double[][] minmax = new double[2][3];
		MathUtil.set3(minmax[0], 1E6, 1e6, 1e6);
		MathUtil.set3(minmax[1], -1E6, -1e6, -1e6);
		for (int i = 8; --i >= 0;) {
			MathUtil.rangeCheck(superCellCartesianVertices[i], minmax);
		}
		double[] tempvec = new double[3];
		for (int i = numAtoms; --i >= 0;) {
			Atom a = atoms[i];
			MathUtil.mat3mul(sBasisCart, a.vector0[DIS], tempvec);
			MathUtil.rangeCheck(tempvec, minmax);
		}
		for (int i = 0; i < 3; i++)
			sCenterCart[i] = (minmax[0][i] + minmax[1][i]) / 2;

		// Place the center of the supercell at the origin
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 3; i++) {
				parentCellCartesianVertices[j][i] -= sCenterCart[i];
				superCellCartesianVertices[j][i] -= sCenterCart[i];
			}
		}
	}

	/*
	 * A class to collect all atom-related content.
	 * 
	 */
	public static class Atom {

		/**
		 * The index of this atom in the filtered array.
		 * 
		 */

		int index;

		/**
		 * zero-based atomType index "t"
		 * 
		 */
		public int type;

		/**
		 * zero-based subAtomType index "s"
		 * 
		 */
		public int subType;

		/**
		 * 
		 * zero-based index within the subType
		 * 
		 */
		int subTypeIndex;

		/**
		 * the initial parameter vector, by mode type; may be of length 1, 3, or 6;
		 * fractional, untransformed values
		 * 
		 */
		final double[][] vector0 = new double[MODE_COUNT][];

		/**
		 * the final paramater vector, by mode type; may be of length 1, 3, or 6;
		 * fractional, untransformed values
		 * 
		 */
		final double[][] vector1 = new double[MODE_COUNT][];

		/**
		 * the IR component symmetry mode IR coefficients, originally by atomType and
		 * subtype, but because now we have this atom object, we don't need to run
		 * through those lists. We can just target an atom directly.
		 * 
		 * 
		 */
		final double[][][] modes = new double[MODE_COUNT][][];

		/**
		 * info holds a vector of information intrinsic to each mode
		 * transformed into Cartesian coordinates.
		 * 
		 * 
		 * DIS: [cx, cy, cz, index] Cartesian coordinates plus index (for bspt)
		 * 
		 * OCC: [occ] fractional occupation
		 * 
		 * MAG: [mx, my, mz] The magnetic moment
		 * 
		 * ROT: [X-angle, Y-angle, Length]
		 * 
		 * ELL: [widthX, widthY, widthZ, axisX, axisY, axisZ, angle]
		 * 
		 * 
		 */
		final double[][] info = new double[MODE_COUNT][];

		private String sym;

		/**
		 * 
		 * @param index
		 * @param t
		 * @param s
		 * @param a
		 * @param coord
		 * @param elementSymbol 
		 * 
		 */
		private Atom(int index, int t, int s, int a, double[] coord, String elementSymbol) {
			this.index = index;
			this.type = t;
			this.subType = s;
			this.subTypeIndex = a;
			this.sym = elementSymbol;
			vector0[DIS] = coord;
		}

		public double[] getFinalFractionalCoord() {
			return vector1[DIS];
		}
		
		public double[] get(int mode) {
			return vector1[mode];
		}

		public double getInitialOccupancy() {
			return vector0[OCC][0];
		}

		public double getOccupancy() {
			return vector1[OCC][0];
		}

		public double[] getMagneticMoment() {
			return get(MAG);
		}
		/**
		 * Get the transformed Cartesian coordinate. The return value
		 * includes a fourth index value that is used after binary partition tree
		 * sorting to return the atom value. 
		 * 
		 * Care should be taken to not
		 * transform this value with a Matrix object (which is 4x4)
		 * 
		 * @return the FOUR-vector [cx, cy, cz, index]
		 */
		public double[] getCartesianCoord() {
			return info[DIS];
		}

		public String getAtomTypeSymbol() {
			return sym;
		}

		@Override
		public String toString() {
			return "[Atom " + index + " " + type + "," + subType + "," + subTypeIndex + "]";
		}

	}

	public class Bond {

		int[] ab = new int[2];

		double d2;

		Bond(int a, int b) {
			ab[0] = a;
			ab[1] = b;
		}
	}

	public class VariableParser {

		private VariableTokenizer vt;

		/**
		 * Used only for connecting bonds with atoms
		 */
		private Map<String, Atom> atomMap = new HashMap<>();

		/**
		 * from parseAtoms()
		 * 
		 * @param t
		 * @param s
		 * @param a
		 * @return "t_s_a" key
		 */
		private String getKeyTSA(int t, int s, int a) {
			return t + "_" + s + "_" + a;
		}

		/**
		 * from parseBonds()
		 * 
		 * @param t
		 * @param s
		 * @param a
		 * @return "t_s_a" key
		 */
		private String getKeyTSA(String t, String s, String a) {
			return t + "_" + s + "_" + a;
		}

		private int getAtomTSA(String tsa) {
			Atom a = atomMap.get(tsa);
			return (a == null ? -1 : a.index);
		}

		/**
		 * Parses the data byte array for isoviz data blocks
		 * 
		 * !key val val val...
		 * 
		 * !key
		 * 
		 * val val val
		 * 
		 * val val val
		 * 
		 * ...
		 * 
		 * 
		 * @param data may be a String, byte[], or InputStream
		 * @return
		 * 
		 */
		byte[] parse(Object data) {
			try {
				boolean skipBondList = (isDiffraction || app.bondsUseBSPT);
				String ignore = (skipBondList ? ";bondlist;" : null);
				vt = new VariableTokenizer(data, ignore,
						isSwitch ? VariableTokenizer.QUIET : VariableTokenizer.DEBUG_LOW);

				isoversion = getOneString("isoversion", null);

				parseAppletSettings();
				parseCrystalSettings();

				parseAtoms();
				parseBonds(skipBondList);
				System.out.println("Variables: " + numAtoms + " atoms and " + numBonds + " bonds were read");

				parseStrainModes();
				parseIrrepList();

			} catch (Throwable t) {
				t.printStackTrace();
				parseError("Java error", 2);
			}

			byte[] bytes = vt.getBytes();
			vt.dispose();
			vt = null;
			return bytes;

		}

		private void parseError(int size, int n) {
			parseError("found " + size + "; expected " + n, 1);
		}

		/**
		 * A method that centralizes error reporting
		 * 
		 * @param error The string that caused the error
		 * @param type  An int corresponding to the type of error:<br>
		 *              <li>0 = Duplicate key<br>
		 *              <li>1 = Incorrect number of arguments for the key<br>
		 *              <li>2 = Invalid input<br>
		 *              <li>3 = Missing required key
		 * 
		 */
		void parseError(String message, int type) {
			String currentTag = vt.getCurrentTag();
			switch (type) {
			case 1:
				throw new RuntimeException(
						"Variables: Invalid number of arguments for key " + currentTag + ": " + message);
			case 2:
				throw new RuntimeException("Variables: " + message + " processing " + currentTag);
			default:
				throw new RuntimeException("Variables: Required key missing: " + currentTag);
			}
		}

		/**
		 * Load a Type/SubType, SubAtom [t][s][a] list with ncol or a default
		 * 
		 * @param def          a default value or NaN to skip if key is not present
		 * @param numAtomsRead
		 * @param bsPrimitive
		 * @param bsPrimitive
		 * @param string
		 * @param atomProp
		 * @param ncol
		 * 
		 */
		boolean getAtomTSAn(String key, int mode, double def, int numAtomsRead, BitSet bsPrimitive) {
			boolean isDefault = (vt.setData(key) == 0);
			if (isDefault && Double.isNaN(def))
				return false;
			int ncol = modes[mode].columnCount;
			for (int q = 0, ia = 0, iread = 0, n = numAtomsRead; iread < n; iread++, q += ncol) {
				if (bsPrimitive != null && !bsPrimitive.get(iread))
					continue;
				double[] data = atoms[ia++].vector0[mode] = new double[ncol];
				for (int i = 0; i < ncol; i++)
					data[i] = (isDefault ? def : vt.getDouble(q + i));
			}
			return true;
		}

		/**
		 * Read occupancy or magnetic moment for older 10-wide format
		 * 
		 * @param mode
		 * @param nAtomsRead
		 * @param bsPrimitive
		 */
		void getAtomsOccMagOld(int mode, int nAtomsRead, BitSet bsPrimitive) {
			int ncol = modes[mode].columnCount;
			int offset = (mode == OCC ? 6 : 7);
			for (int pt = 0, ia = 0, iread = 0; iread < nAtomsRead; iread++, pt += 10) {
				if (bsPrimitive != null && !bsPrimitive.get(iread))
					continue;
				double[] data = atoms[ia++].vector0[mode] = new double[ncol];
				for (int i = 0; i < ncol; i++)
					data[i] = vt.getDouble(pt + offset + i);
			}
		}

		boolean checkSize(String key, int n) {
			int nData = vt.setData(key);
			if (nData == 0)
				parseError(null, 3);
			if (nData != n)
				parseError(nData, n);
			return true;
		}

		/**
		 * Ensure the data are the right size for ncol.
		 * 
		 * @param string
		 * 
		 * @param ncol   number of columns
		 * @return number of rows
		 * 
		 */

		int checkSizeN(String key, int ncol, boolean isRequired) {
			int nData = vt.setData(key);
			if (nData == 0 && !isRequired)
				return 0;
			int numRows = nData / ncol;
			int n = numRows * ncol;
			if (nData != n)
				parseError(nData, n);
			return numRows;
		}

		/**
		 * Parse into an array a sequence of data values of.
		 * 
		 * @param key the data block key, or null to continue with the current block
		 * @param a   target array
		 * @param pt  starting pointer in data block from which n data are to be read
		 * @param n   number of elements of array to fill
		 * @return
		 */
		private int getDoubleArray(String key, double[] a, int pt, int n) {
			if (key != null) {
				int nData = vt.setData(key);
				if (nData != n)
					parseError(nData, n);
			}
			for (int i = 0; i < n; i++)
				a[i] = getDouble(pt++);
			return pt;
		}

		double getDouble(int pt) {
			double d = vt.getDouble(pt);
			if (Double.isNaN(d))
				parseError("double value Expected " + vt.getString(pt), 1);
			return d;
		}

		/**
		 * Just check for one int value.
		 * 
		 * @param def the default value, or Intger.MIN_VALUE if required
		 * @return the value read or default value
		 * @throws RuntineException if required and not found
		 * 
		 */
		public int getOneInt(String key, int def) {
			int nData = vt.setData(key);
			switch (nData) {
			case 1:
				def = vt.getInt(0);
				if (def == Integer.MIN_VALUE)
					parseError(nData, 1);
				// fall through
			case 0:
				if (def != Integer.MIN_VALUE)
					return def;
				// fall through
			default:
				parseError(nData, 1);
				return 0;
			}
		}

		/**
		 * Just check for one String value.
		 * 
		 * @param def the default value, or null if required
		 * @return the value read or default value
		 * @throws RuntineException if required and not found
		 * 
		 */
		private String getOneString(String key, String def) {
			int nData = vt.setData(key);
			switch (nData) {
			case 1:
				def = vt.getString(0);
				// fall through
			case 0:
				// fall through
				if (def != null)
					return def;
			default:
				parseError(nData, 1);
				return null;
			}
		}

		/**
		 * Just check for one double value.
		 * 
		 * @param def the default value, or Double.NaN if required
		 * @return the value read or default value
		 * @throws RuntineException if required and not found
		 * 
		 */
		double getOneDouble(String key, double def) {
			int nData = vt.setData(key);
			switch (nData) {
			case 1:
				def = vt.getDouble(0);
				// fall through
			case 0:
				if (!Double.isNaN(def))
					return def;
				// fall through
			default:
				parseError(nData, 1);
				break;
			}
			return Double.NaN;
		}

		/**
		 * Just check for one Boolean value.
		 * 
		 * @param def the default value, or null if required
		 * @return the value read or default value
		 * @throws RuntineException if required and not found
		 * 
		 */
		public boolean getOneBoolean(String key, Boolean def) {
			int nData = vt.setData(key);
			switch (nData) {
			case 1:
				def = vt.getBoolean(0);
				// fall through
			case 0:
				if (def != null)
					return def.booleanValue();
				// fall through
			default:
				parseError(nData, 1);
				break;
			}
			return false;
		}

		private void parseAppletSettings() {
			// find applet width
			int n = getOneInt("appletwidth", 0);
			if (n >= 500 && n <= 5000)
				appletWidth = n;
			appletHeight = (int) Math.round((double) appletWidth / 1.6);

		}

		private void parseCrystalSettings() {
			getDoubleArray("parentcell", pLatt0, 0, 6);
			getDoubleArray("parentorigin", pOriginUnitless, 0, 3);
			checkSize("parentbasis", 9);
			for (int pt = 0, j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++, pt++) {
					Tmat[j][i] = vt.getDouble(pt);
				}
			}
			MathUtil.mat3transpose(Tmat, TmatTranspose);
			MathUtil.mat3inverse(TmatTranspose, TmatInverseTranspose);
			isRhombParentSetting = getOneBoolean("rhombparentsetting", false);

			// Calculate pBasisCart0
			if (isRhombParentSetting) {
				// defined row by row
				double temp1 = Math.sin(pLatt0[5] / 2);
				double temp2 = Math.sqrt(1 / temp1 / temp1 - 4.0 / 3.0);
				pBasisCart0[0][0] = pLatt0[0] * temp1 * (1);
				pBasisCart0[0][1] = pLatt0[0] * temp1 * (-1);
				pBasisCart0[0][2] = pLatt0[0] * temp1 * (0);
				pBasisCart0[1][0] = pLatt0[0] * temp1 * (1 / Math.sqrt(3));
				pBasisCart0[1][1] = pLatt0[0] * temp1 * (1 / Math.sqrt(3));
				pBasisCart0[1][2] = pLatt0[0] * temp1 * (-2 / Math.sqrt(3));
				pBasisCart0[2][0] = pLatt0[0] * temp1 * temp2;
				pBasisCart0[2][1] = pLatt0[0] * temp1 * temp2;
				pBasisCart0[2][2] = pLatt0[0] * temp1 * temp2;
			} else {
				// defined column by column
				double d = Math.cos(pLatt0[4]);
				double temp1 = (Math.cos(pLatt0[3]) - d * Math.cos(pLatt0[5])) / Math.sin(pLatt0[5]);
				d = 1 - d * d - temp1 * temp1;
				double temp2 = Math.sqrt(d < 0 ? 0 : d);
				pBasisCart0[0][0] = pLatt0[0];
				pBasisCart0[1][0] = 0;
				pBasisCart0[2][0] = 0;
				pBasisCart0[0][1] = pLatt0[1] * Math.cos(pLatt0[5]);
				pBasisCart0[1][1] = pLatt0[1] * Math.sin(pLatt0[5]);
				pBasisCart0[2][1] = 0;
				pBasisCart0[0][2] = pLatt0[2] * Math.cos(pLatt0[4]);
				pBasisCart0[1][2] = pLatt0[2] * temp1;
				pBasisCart0[2][2] = pLatt0[2] * temp2;
			}

			// calculate the unstrained parent basis in cartesian Angstrom coords
			MathUtil.mat3product(pBasisCart0, TmatInverseTranspose, sBasisCart0);

		}

		private void parseAtoms() {

			angstromsPerMagneton = getOneDouble("angstromspermagneton", 0.5);
			angstromsPerRadian = getOneDouble("angstromsperradian", 0.5);

			atomMaxRadius = getOneDouble("atommaxradius", 0.4);

			defaultUiso = getOneDouble("defaultuiso", 0);
			if (defaultUiso <= 0) {
				double d = atomMaxRadius / 2.0;
				defaultUiso = d * d;
			}

			/**
			 * this temporary bitset only for diffraction. It is read first and then used to
			 * filter atoms and atom properties
			 * 
			 */
			BitSet bsPrimitive = null;

			if (isDiffraction) {
				bsPrimitive = vt.getBitSet("atomsinunitcell");
				numAtoms = (bsPrimitive == null ? 0 : bsPrimitive.cardinality());
			}

			// Get all the atom type information and return the number of subtype atoms for
			// each type.
			// Just for reading the file.

			int[][] numSubTypeAtomsRead = parseAtomTypes();

			// the number of primitive atoms,
			// if this is for IsoDifrract; in the end, we
			// will replace numSubAtoms with numSubPrimitiveAtoms
			int[][] numPrimitiveSubAtoms = null;

			if (bsPrimitive != null) {
				numPrimitiveSubAtoms = new int[numTypes][];
				for (int i = 0; i < numTypes; i++) {
					numPrimitiveSubAtoms[i] = new int[numSubTypeAtomsRead[i].length];
				}
			}

			boolean haveBonds = (vt.setData("bondlist") > 0);

			// find atomic coordinates of parent
			int nData = vt.setData("atomcoordlist");
			if (nData == 0) {
				parseError("atomcoordlist is missing", 3);
			}

			// number of atoms in the file, before filtering for primitives
			int numAtomsRead = getNumberOfAtomsRead(nData);
			if (numSubTypeAtomsRead.length == 1)
				numSubTypeAtomsRead[0] = new int[] { numAtomsRead };

			int ncol = nData / numAtomsRead;

			// numAtoms may be the number of primitive atoms only
			if (numAtoms == 0)
				numAtoms = numAtomsRead;
			atoms = new Atom[numAtoms];

			// Find number of subatoms for each subtype
			// Set up numSubAtom and numPrimitiveSubAtoms if this is for IsoDiffract

			// BH 2023.12
			// firstAtomOfType is an array containing pointers in the overall list of atoms
			// read
			// (element 0) and the filtered primitive list (element 1)
			// by type. firstAtomOfType[0] is always [0, 0], and we add an addition element
			// at the
			// end that is [numAtomsRead, numAtoms]. These are useful in the mode listings,
			// where we
			// need to catalog atom mode vectors from lists involving type indices only
			int[][] firstAtomOfType = new int[numTypes + 1][2];
			firstAtomOfType[0] = new int[2];
			firstAtomOfType[numTypes] = new int[] { numAtomsRead, numAtoms };

			readAtomCoordinates(ncol, numAtomsRead, numSubTypeAtomsRead, bsPrimitive, numPrimitiveSubAtoms,
					firstAtomOfType, haveBonds);

			numSubAtoms = (bsPrimitive == null ? numSubTypeAtomsRead : numPrimitiveSubAtoms);
			for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
				// (STRAIN and IRRED are handled later)
				modes[i] = new Mode(i, numTypes, numSubTypes, numSubAtoms);
			}
			modes[DIS].isActive = true;

			// find atomic occupancies, magnetic moments, atomic rotations, and adp
			// ellipsoids of parent

			if (ncol == 10) {
				// old format t s a x y z occ mx my mz
				getAtomsOccMagOld(OCC, numAtomsRead, bsPrimitive);
				getAtomsOccMagOld(MAG, numAtomsRead, bsPrimitive);
			} else {
				getAtomTSAn("atomocclist", OCC, 1.0, numAtomsRead, bsPrimitive);
				getAtomTSAn("atommaglist", MAG, 0.0, numAtomsRead, bsPrimitive);
			}
			getAtomTSAn("atomrotlist", ROT, 0.0, numAtomsRead, bsPrimitive);
			if (!getAtomTSAn("atomelplist", ELL, Double.NaN, numAtomsRead, bsPrimitive)) {
				// default to [0.04 0.04 0.04 0 0 0]
				double[] def = new double[] { defaultUiso, defaultUiso, defaultUiso, 0, 0, 0 };
				for (int ia = 0; ia < atoms.length; ia++)
					atoms[ia].vector0[ELL] = def;
			}

			if (bsPrimitive != null)
				System.out.println("Variables: primitive: " + bsPrimitive);

			// now get all the symmetry-related arrays
			parseAtomicMode(DIS, "displacivemodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(OCC, "scalarmodelist", 1, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(MAG, "magneticmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(ROT, "magneticmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(ELL, "ellipmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);

		}

		private void readAtomCoordinates(int ncol, int numAtomsRead, int[][] numSubTypeAtomsRead, BitSet bsPrimitive,
				int[][] numPrimitiveSubAtoms, int[][] firstAtomOfType, boolean haveBonds) {
			int lastT = 0;
			for (int i = 0, ia = 0; i < numAtomsRead; i++) {
				int pt = ncol * i;
				int t = vt.getInt(pt++) - 1;
				if (t != lastT) {
					firstAtomOfType[t] = new int[] { i, ia };
					lastT = t;
				}
				int s = vt.getInt(pt++) - 1;
				boolean isOK = (bsPrimitive == null || bsPrimitive.get(i));
				if (isOK) {
					int a = vt.getInt(pt++) - 1;
					double[] coord = new double[] { vt.getDouble(pt++), vt.getDouble(pt++), vt.getDouble(pt++) };
					if (bsPrimitive != null) {
						numPrimitiveSubAtoms[t][s]++;
					}
					Atom atom = atoms[ia] = new Atom(ia, t, s, a, coord, atomTypeSymbol[t]);
					if (haveBonds)
						atomMap.put(getKeyTSA(t + 1, s + 1, a + 1), atom);
					ia++;
				}
				numSubTypeAtomsRead[t][s]++;
			}

		}

		private int getNumberOfAtomsRead(int nData) {
			int ncol = 6;
			int n = nData / ncol;
			int nOld = nData / 10;
			if (nOld * 10 == nData) {
				// could be 60n atoms (as in test28)
				// get the first number on the second line (if new format)
				// if that is a float, then we must have the old format.
				if (vt.getInt(ncol) == Integer.MIN_VALUE) {
					ncol = 10;
					n = nOld;
				}
			}
			if (nData != n * ncol) {
				// Make sure it divided evenly
				parseError(nData, n * ncol);
			}
			return n;
		}

		/**
		 * may return [1]{null} if there are no subtypes
		 * 
		 * @return
		 */
		private int[][] parseAtomTypes() {
			// find atom types
			int n = numTypes = checkSizeN("atomtypelist", 3, true);

			atomTypeName = new String[n];
			atomTypeSymbol = new String[n];
			for (int i = 0; i < n; i++) {
				if (vt.getInt(3 * i) != i + 1)
					parseError("atom types are not in sequential order", 2);
				atomTypeName[i] = vt.getString(3 * i + 1);
				atomTypeSymbol[i] = vt.getString(3 * i + 2);
			}

			numSubTypes = new int[n];
			int[][] numSubTypeAtomsRead = new int[n][];

			// find atom subtypes
			int numSubTypeEntries = checkSizeN("atomsubtypelist", 3, false);
			if (numSubTypeEntries > 0) {
				int curType = 0, curSubType = 0;
				for (int i = 0; i < numSubTypeEntries; i++) {
					int itype = vt.getInt(3 * i);
					if (itype != curType) {
						if (curSubType != 0) {
							// close out previous type's pointers
							numSubTypeAtomsRead[curType - 1] = new int[curSubType];
						}
						curType++;
						curSubType = 0;
						if (itype != curType)
							parseError("The atom types in the atom subtype list are out of order", 2);
					}
					int subtype = vt.getInt(3 * i + 1);
					if (subtype != curSubType) {
						numSubTypes[curType - 1]++;
						curSubType++;
						if (subtype != curSubType)
							parseError("The subtypes in the atom subtype list are out of order", 2);
					}
				}
				numSubTypeAtomsRead[curType - 1] = new int[curSubType];

				// Assign the subtype names
				subTypeName = new String[n][];
				for (int pt = 0, t = 0; t < n; t++) {
					subTypeName[t] = new String[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) {
						subTypeName[t][s] = vt.getString(3 * pt++ + 2);
					}
				}
			} else {
				// This code is used in ISOCIF, where there are no subtypes defined.
				subTypeName = new String[n][];
				for (int t = 0; t < n; t++) {
					numSubTypes[t] = 1;
					subTypeName[t] = new String[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) {
						subTypeName[t][s] = atomTypeName[t] + "_" + (s + 1);
					}
				}
			}
			return numSubTypeAtomsRead;
		}

		/**
		 * Just create int[][] variables.bonds
		 */
		private void parseBonds(boolean skipBondList) {
			if (isDiffraction)
				return;
			halfMaxBondLength = getOneDouble("maxbondlength", 2.5) / 2;
			// find minimum atomic occupancy for which bonds should be displayed
			minBondOcc = getOneDouble("minbondocc", 0.5);
			if (skipBondList)
				return;
			int numBondsRead = checkSizeN("bondlist", 6, false);
			double[][] bondsTemp = null;
			if (numBondsRead > 0) {
				// find maximum length of bonds that can be displayed
				bondsTemp = new double[numBondsRead][9];
				for (int b = 0, pt = 0; b < numBondsRead; b++) {
					String keyA = parseTSA(pt);
					pt += 3;
					String keyB = parseTSA(pt);
					pt += 3;
					int ia = getAtomTSA(keyA);
					if (ia >= 0) {
						int ib = getAtomTSA(keyB);
						if (ib >= 0) {
							bondsTemp[b][6] = ia;
							bondsTemp[b][7] = ib;
							bondsTemp[b][8] = numBonds++;
						}
					}

				}
			}
			if (numBonds == 0) {
				// There are no bonds. Initialize the array to length zero
				bondInfo = new double[0][];
			} else if (numBonds == numBondsRead) {
				bondInfo = bondsTemp;
			} else {
				bondInfo = new double[numBonds][8];
				for (int i = 0; i < numBonds; i++)
					bondInfo[i] = bondsTemp[i];
			}
		}

		/**
		 * Create an atom map key int the form "t_s_a"
		 * 
		 * @param pt
		 * @return
		 * 
		 */
		private String parseTSA(int pt) {
			return getKeyTSA(vt.getString(pt++), vt.getString(pt++), vt.getString(pt++));
		}

		/**
		 * parse the DIS OCC MAG ROT ELL modes, filling the information into the related
		 * Atom object.
		 * 
		 * @param mode
		 * @param key
		 * @param ncol
		 * @param firstAtomOfType
		 */
		private void parseAtomicMode(int mode, String key, int ncol, int[][] firstAtomOfType,
				int[][] numSubTypeAtomsRead, BitSet bsPrimitive) {
			int[] perType = new int[numTypes];
			int n = getAtomModeNumbers(key, mode, perType, ncol, firstAtomOfType);
			if (n == 0)
				return;
			modes[mode].initArrays(perType, n);
			getAtomModeData(modes[mode], firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
		}

		/**
		 * read the mode numbers for any of the five mode types.
		 * 
		 * @param key
		 * 
		 * @param perType
		 * @param ncol
		 * @param firstAtomOfType
		 * @return number of modes
		 * 
		 */
		private int getAtomModeNumbers(String key, int mode, int[] perType, int ncol, int[][] firstAtomOfType) {
			int nData = vt.setData(key);
			// Run through the data once and determine the number of modes
			if (nData == 0)
				return 0;
			int n = 0;
			for (int pt = 0; pt < nData;) {
				int atomType = vt.getInt(pt) - 1;
				perType[atomType]++;
				n++;
				// advance pt to next atom type number
				int nAtoms = firstAtomOfType[atomType + 1][0] - firstAtomOfType[atomType][0];
				pt += 6 + nAtoms * ncol;
			}
			// initialize the atom mode[] arrays now that we have the perType information.
			for (int ia = numAtoms; --ia >= 0;) {
				Atom a = atoms[ia];
				a.modes[mode] = (perType[a.type] == 0 ? null : new double[perType[a.type]][]);
			}
			return n;
		}

		/**
		 * Parse and save all mode-related data.
		 * 
		 * @param firstAtomOfType
		 * @param numSubTypeAtomsRead
		 * 
		 * @param variableParser
		 * @param bsPrimitive
		 * @param key
		 * 
		 * 
		 * @param parser
		 * 
		 */
		private void getAtomModeData(Mode mode, int[][] firstAtomOfType, int[][] numSubTypeAtomsRead,
				BitSet bsPrimitive) {
			// input mode array [atom type][mode number of that type][atom
			// number of that type][column data]
			int type = mode.type;
			int ncol = mode.columnCount;
			int[] modeTracker = new int[numTypes];
			for (int pt = 0, m = 0; m < mode.count; m++) {
				int atomType = vt.getInt(pt++) - 1;
				int iread = firstAtomOfType[atomType][0];
				int ia = firstAtomOfType[atomType][1];
				int mt = modeTracker[atomType]++;
				if (mt + 1 != vt.getInt(pt++))
					parseError("The modes are not given in ascending order", 2);
				mode.initAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.maxAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.irrepTM[atomType][mt] = vt.getInt(pt++) - 1;
				mode.nameTM[atomType][mt] = vt.getString(pt++);
				for (int s = 0; s < numSubTypes[atomType]; s++) {
					for (int a = 0; a < numSubTypeAtomsRead[atomType][s]; a++, iread++) {
						if (bsPrimitive != null && !bsPrimitive.get(iread))
							continue;
						double[] array = atoms[ia++].modes[type][mt] = new double[ncol];
						pt = getDoubleArray(null, array, pt, ncol);
					}
				}
			}
		}

		private void parseStrainModes() {
			// Handle strain modes
			int n = 0;
			int nData = vt.setData("strainmodelist");
			if (nData == 0)
				return;
			int ncol = 11;
			n = nData / ncol;
			if (nData != n * ncol) {
				// Make sure it divided evenly
				parseError(nData, n * ncol);
			}
			modes[STRAIN] = new Mode(STRAIN, 1, null, null);
			modes[STRAIN].initArrays(null, n);
			for (int m = 0; m < n; m++) {
				modes[STRAIN].nameTM[0][m] = vt.getString(ncol * m + 4);
				modes[STRAIN].irrepTM[0][m] = vt.getInt(ncol * m + 3) - 1;
				modes[STRAIN].initAmpTM[0][m] = vt.getDouble(ncol * m + 1);
				modes[STRAIN].maxAmpTM[0][m] = vt.getDouble(ncol * m + 2);
				getDoubleArray(null, modes[STRAIN].vector[m], ncol * m + 5, 6);
			}
		}

		private void parseIrrepList() {
			// just getting the list of irrep names
			int nData = vt.setData("irreplist");
			if (nData == 0)
				return;
			if (nData % 2 != 0)
				parseError("expected two columns of data (number and name); found " + nData, 1);
			int n = nData / 2;
			modes[IRREP] = new Mode(IRREP, 1, null, null);
			modes[IRREP].initArrays(null, n);
			for (int i = 0; i < n; i++) {
				if (vt.getInt(2 * i) != i + 1)
					parseError("Error: irreps are not in ascending order.", 2);
				modes[IRREP].nameTM[0][i] = vt.getString(2 * i + 1);
			}
		}

	}

	Color getDefaultModeColor(int t) {
		int i = getFirstActiveMode();
		return i < 0 ? Color.RED : modes[i].colorT[t];
	}

	int getFirstActiveMode() {
		for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
			if (isModeActive(modes[i])) {
				return i;
			}
		}
		return -1;
	}

	private class VariableGUI implements ChangeListener {

		private final static int subTypeWidth = 170;
		private final static int barheight = 22;
		private final static int subTypeBoxWidth = 15, superSliderLabelWidth = 66, sliderLabelWidth = 36,
				lattWidth = 60;

		/**
		 * Master (top most) slider bar controls all slider bars for superpositioning of
		 * modes.
		 * 
		 */
		private JSlider superSlider;
		/**
		 * Array of strain mode slider bars.
		 */
		private JLabel superSliderLabel;

		/**
		 * Array of labels for supercell lattice parameters
		 */
		private JLabel[] sLattLabel = new JLabel[6];
		/**
		 * Array of labels for parent lattice parameters
		 */
		private JLabel[] pLattLabel = new JLabel[6];
		/**
		 * names for parent and super cell parameter titles
		 */
		private JLabel parentLabel, superLabel;
		/**
		 * Array of labels for atom types
		 */
		private JLabel typeLabel[];
		/**
		 * subTypeLabels
		 */
		private JLabel[][] subTypeLabels;
		/**
		 * Array of checkboxes -- one for each atomic subtype
		 */
		private JCheckBox[][] subTypeBoxes;

		/**
		 * Panel which holds master slider bar and it's label; added scrollPanel
		 */
		private JPanel masterSliderPanel;
		/**
		 * Panel which holds the parent atom type
		 */
		private JPanel typeNamePanels[];
		/**
		 * Panel which holds the occupancy check boxes for each atom subtype associated
		 * with a type
		 * 
		 */
		private JPanel typeDataPanels[];

		/**
		 * Maximum slider bar value. The slider bar only takes integer values. So the
		 * sliderMax is the number of notches and therefore the precision of the slider
		 * bar. More notches make for more precise slider movements but slow the
		 * rendering a lot since it renders at every notch
		 * 
		 */
		private int sliderMax = 100;
		/**
		 * double version of sliderMax
		 */
		private double sliderMaxVal = sliderMax;

		private int sliderWidth;
		private int sliderPanelWidth;

		/**
		 * Total number of unique parent atom types.
		 */
		public int numUniques;

		/**
		 * Integer index that identifies each parent atom type as one of several unique
		 * parent atom types.
		 * 
		 */
		public int[] atomTypeUnique;

		/**
		 * resets the background colors of the panel components
		 */
		void recolorPanels() {
			for (int t = 0; t < numTypes; t++) {
				// Reset the SliderBarPanel colors.
				Color c = getDefaultModeColor(t);
				typeLabel[t].setBackground(c);
				typeNamePanels[t].setBackground(c);
				typeDataPanels[t].setBackground(c);
				for (int s = 0; s < numSubTypes[t]; s++) {
					subTypeBoxes[t][s].setBackground(c);
					subTypeLabels[t][s].setBackground(c);
				}
				for (int i = 0; i < MODE_ATOMIC_COUNT; i++)
					modes[i].colorPanels();
			}

		}

		void setColors(boolean simpleColor) {
			for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
				if (isModeActive(modes[i])) {
					modes[i].setColors(simpleColor ? null : atomTypeUnique, numUniques);
				}
			}
		}

		void toggleIrrepSliders() {
			irrepSlidersOn = !irrepSlidersOn;
			int val = (irrepSlidersOn ? sliderMax : 0);
			modes[IRREP].setSliders(val);
		}

		void zeroSliders() {
			superSlider.setValue(sliderMax);
			for (int i = 0; i < MODE_COUNT; i++) {
				modes[i].setSliders(0);
			}
		}

		void resetSliders() {
			superSlider.setValue(sliderMax);
			for (int i = 0; i < MODE_COUNT; i++) {
				modes[i].resetSliders(sliderMax);
			}
		}

		void setComponentValuesFrom(Variables v) {

			for (int t = subTypeBoxes.length; --t >= 0;) {
				JCheckBox[] boxes = subTypeBoxes[t];
				for (int s = boxes.length; --s >= 0;) {
					boxes[s].setSelected(v.gui.subTypeBoxes[t][s].isSelected());
				}
			}

			superSlider.setValue(v.gui.superSlider.getValue());
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i]))
					modes[i].setValues(v.modes[i]);
			}
		}

		/**
		 * Copy slider settings to fields and set the slider values text.
		 * 
		 * 
		 */
		void readSliders() {
			superSliderVal = superSlider.getValue() / sliderMaxVal;
			superSliderLabel.setText(MathUtil.varToString(superSliderVal, 2, -6) + " child");
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i]))
					modes[i].readSlider(superSliderVal, sliderMaxVal, modes[IRREP]);
			}
		}

		void initPanels() {

			// Divide the applet area with structure on the left and controls on the right.

			Dimension dim = sliderPanel.getPreferredSize();

			sliderPanelWidth = dim.width;
			sliderWidth = sliderPanelWidth / 2;

			/**
			 * Maximum number of check boxes per row the GUI will hold
			 */
			int maxSubTypesPerRow = (int) Math.floor(dim.width / subTypeWidth);

			int[] numSubRows = new int[numTypes];
			int[] subTypesPerRow = new int[numTypes];
			for (int t = 0; t < numTypes; t++) {
				// iterate over types
				subTypesPerRow[t] = Math.min(maxSubTypesPerRow, numSubTypes[t]);
				numSubRows[t] = (int) Math.ceil((double) numSubTypes[t] / subTypesPerRow[t]);
			}
			needSimpleColor = identifyUniqueAtoms(atomTypeSymbol);

			setColors(false);

			masterSliderPanel = new JPanel();
			masterSliderPanel.setLayout(new BoxLayout(masterSliderPanel, BoxLayout.LINE_AXIS));
			masterSliderPanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
			masterSliderPanel.setBackground(Color.WHITE);

			// Master Slider Panel
			superSliderLabel = new JLabel("child");
			superSliderLabel.setPreferredSize(new Dimension(superSliderLabelWidth, barheight));
			superSliderLabel.setForeground(Color.BLACK);
			superSliderLabel.setHorizontalAlignment(JLabel.CENTER);
			superSliderLabel.setVerticalAlignment(JLabel.CENTER);
			superSlider = newSlider("child", 0, sliderMax, sliderMax, Color.WHITE);

			masterSliderPanel.add(Box.createHorizontalGlue());
			masterSliderPanel.add(new JLabel("parent "));
			masterSliderPanel.add(superSlider);
			masterSliderPanel.add(Box.createRigidArea(new Dimension(1, 1)));
			masterSliderPanel.add(superSliderLabel);
			masterSliderPanel.add(Box.createHorizontalGlue());
			sliderPanel.add(masterSliderPanel);

			// Initialize type-specific subpanels of scrollPanel
			typeLabel = new JLabel[numTypes];
			typeNamePanels = new JPanel[numTypes];
			typeDataPanels = new JPanel[numTypes];
			subTypeBoxes = new JCheckBox[numTypes][];
			subTypeLabels = new JLabel[numTypes][];

			// The big loop over types
			for (int t = 0; t < numTypes; t++) {
				Color c = getDefaultModeColor(t);
				JPanel tp = new JPanel();
				tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
				tp.setBorder(new EmptyBorder(2, 2, 5, 2));
				tp.setBackground(c);
				tp.setOpaque(true);
				subTypeBoxes[t] = new JCheckBox[numSubTypes[t]];
				subTypeLabels[t] = new JLabel[numSubTypes[t]];
				typeLabel[t] = newLabel("" + atomTypeName[t] + " Modes", sliderPanelWidth, c, JLabel.CENTER);
				typeNamePanels[t] = new JPanel(new GridLayout(1, 1, 0, 0));
				typeNamePanels[t].setPreferredSize(new Dimension(sliderPanelWidth, barheight / 2));
				typeNamePanels[t].add(typeLabel[t]);
				typeNamePanels[t].setBackground(c);
				// typeDataPanel
				typeDataPanels[t] = new JPanel(new GridLayout(numSubRows[t], subTypesPerRow[t], 0, 0));
				typeDataPanels[t].setPreferredSize(new Dimension(sliderPanelWidth, numSubRows[t] * barheight));
				typeDataPanels[t].setBackground(c);

				for (int s = 0; s < numSubTypes[t]; s++) {
					subTypeBoxes[t][s] = newCheckbox("subType_" + t + "_" + s, c);
					subTypeLabels[t][s] = newLabel("", subTypeWidth - subTypeBoxWidth, c, JLabel.LEFT);
					JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.setPreferredSize(new Dimension(sliderPanelWidth, barheight / 2));
					p.setBackground(c);
					p.add(subTypeBoxes[t][s]);
					p.add(subTypeLabels[t][s]);
					typeDataPanels[t].add(p);
				}
				// Makes sure that each subtype row is full for alignment purposes
				for (int s = numSubTypes[t]; s < numSubRows[t] * subTypesPerRow[t]; s++)
					typeDataPanels[t].add(new JLabel(""));
				tp.add(typeNamePanels[t]);
				tp.add(typeDataPanels[t]);
				tp.setPreferredSize(new Dimension(sliderPanelWidth, (numSubRows[t] + 1) * barheight));
				sliderPanel.add(tp);
 				for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
					initModeGUI(modes[i], t, sliderPanel);
				}

			}
			Color c = Color.DARK_GRAY;
			JLabel strainTitle = newLabel("Strain Modes", sliderPanelWidth, c, JLabel.CENTER);
			JPanel strainTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
			strainTitlePanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
			strainTitlePanel.setBackground(c);
			strainTitlePanel.add(strainTitle);
			sliderPanel.add(strainTitlePanel);

			// strainDataPanel
			for (int n = 0; n < 6; n++) {
				sLattLabel[n] = newLabel("", lattWidth, c, JLabel.LEFT);
				pLattLabel[n] = newLabel("", lattWidth, c, JLabel.LEFT);
			}
			parentLabel = newLabel("  Pcell", lattWidth, c, JLabel.LEFT);
			superLabel = newLabel("  Scell", lattWidth, c, JLabel.LEFT);
			JPanel strainDataPanel = new JPanel(new GridLayout(2, 6, 0, 0));
			strainDataPanel.setPreferredSize(new Dimension(sliderPanelWidth, 2 * barheight));
			strainDataPanel.setBackground(c);
			strainDataPanel.add(parentLabel);
			for (int n = 0; n < 6; n++)
				strainDataPanel.add(pLattLabel[n]);
			strainDataPanel.add(superLabel);
			for (int n = 0; n < 6; n++)
				strainDataPanel.add(sLattLabel[n]);
			sliderPanel.add(strainDataPanel);
			initNonAtomGUI(modes[STRAIN], c);

			c = Color.LIGHT_GRAY;
			JLabel irrepTitle = newLabel("Single-Irrep Master Amplitudes", sliderPanelWidth, Color.LIGHT_GRAY,
					JLabel.CENTER);
			JPanel irrepTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
			irrepTitlePanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
			irrepTitlePanel.setBackground(c);
			irrepTitlePanel.add(irrepTitle);
			sliderPanel.add(irrepTitlePanel);
			initNonAtomGUI(modes[IRREP], c);
		}

		/**
		 * Determines the unique atom type of each non-unique atom type.
		 * 
		 * If any type has multiple subtypes, we'll enable the "color" button
		 * 
		 * @return true if we need to enable the "color" button to allow "simple" colors
		 * 
		 */
		private boolean identifyUniqueAtoms(String[] symT) {
			// Determine the unique atoms types.
			boolean unique;
			String uniquetypes[] = new String[numTypes];
			int[] uniqueT = new int[numTypes];
			int n = 0;
			for (int t = 0; t < numTypes; t++) {
				unique = true;
				for (int u = 0; u < n; u++) {
					if (uniquetypes[u].equals(symT[t])) {
						unique = false;
						uniqueT[t] = u;
					}
				}
				if (unique) {
					uniquetypes[n] = symT[t];
					uniqueT[t] = n++;
				}
			}
			if (n == numTypes)
				return false;
			atomTypeUnique = uniqueT;
			numUniques = n;
			return (n < numTypes);
		}

		private JLabel newLabel(String text, int width, Color c, int hAlign) {
			JLabel l = new JLabel(text);
			l.setPreferredSize(new Dimension(width, barheight));
			l.setForeground(Color.WHITE);
			l.setHorizontalAlignment(hAlign);
			l.setVerticalAlignment(JLabel.CENTER);
			return l;
		}

		private JCheckBox newCheckbox(String name, Color c) {
			JCheckBox b = new JCheckBox("");
			b.setName(name);
			b.setPreferredSize(new Dimension(subTypeBoxWidth, barheight));
			b.setFocusable(false);
			b.setBackground(c);
			b.setForeground(Color.WHITE);
			b.setHorizontalAlignment(JCheckBox.LEFT);
			// b.setVerticalAlignment(JCheckBox.CENTER);
			b.addItemListener(app.buttonListener);
			b.setVisible(!isDiffraction);
			return b;
		}

		private JSlider newSlider(String name, int min, int max, int val, Color c) {
			JSlider s = new JSlider(JSlider.HORIZONTAL, min, max, val);
			s.setName(name);
			s.setPreferredSize(new Dimension(sliderWidth, barheight));
			if (c != null)
				s.setBackground(c);
			s.addChangeListener(this);
			s.setFocusable(false);
			return s;
		}

		/**
		 * Set up the upper mode-related panels.
		 * 
		 * @param mode
		 * @param t
		 * @param sliderPanel
		 * 
		 */
		private void initModeGUI(Mode mode, int t, JPanel sliderPanel) {
			if (!isModeActive(mode))
				return;
			if (t == 0) {
				mode.typePanels = new JPanel[numTypes];
				mode.sliderTM = new JSlider[numTypes][];
				mode.sliderLabelsTM = new JLabel[numTypes][];
				mode.sliderValsTM = new double[numTypes][];
			}
			Color c = (mode.colorT == null ? Color.PINK : mode.colorT[t]);
			int nModes = mode.modesPerType[t];
			mode.sliderTM[t] = new JSlider[nModes];
			mode.sliderLabelsTM[t] = new JLabel[nModes];
			mode.sliderValsTM[t] = new double[nModes];
			mode.typePanels[t] = new JPanel(new GridLayout(nModes, 2, 0, 0));
			mode.typePanels[t].setPreferredSize(new Dimension(sliderPanelWidth, mode.modesPerType[t] * barheight));
			mode.typePanels[t].setBackground(c);
			for (int m = 0; m < nModes; m++) {
				mode.sliderLabelsTM[t][m] = newLabel("", sliderLabelWidth, c, JLabel.LEFT);
				mode.sliderTM[t][m] = newSlider("mode[" + mode.type + "]" + t + "_" + m, -(int) sliderMax,
						(int) sliderMax, (int) ((mode.initAmpTM[t][m] / mode.maxAmpTM[t][m]) * sliderMax), c);
				mode.typePanels[t].add(mode.sliderTM[t][m]);
				mode.typePanels[t].add(mode.sliderLabelsTM[t][m]);
			}
			if (sliderPanel != null)
				sliderPanel.add(mode.typePanels[t]);

		}

		/**
		 * Set up the strain and irreducible representation panels.
		 * 
		 * @param mode
		 * @param c
		 * 
		 */
		private void initNonAtomGUI(Mode mode, Color c) {
			if (mode == null)
				return;
			initModeGUI(mode, 0, null);
			int min = (mode.type == STRAIN ? -(int) sliderMax : 0);
			mode.typePanels[0] = new JPanel(new GridLayout(mode.count, 2, 0, 0));
			mode.typePanels[0].setPreferredSize(new Dimension(sliderPanelWidth, mode.count * barheight));
			mode.typePanels[0].setBackground(c);
			mode.sliderTM = new JSlider[1][mode.count];
			mode.sliderLabelsTM[0] = new JLabel[mode.count];
			mode.sliderValsTM[0] = new double[mode.count];
			for (int m = 0; m < mode.count; m++) {
				mode.sliderLabelsTM[0][m] = newLabel("", sliderLabelWidth, c, JLabel.LEFT);
				mode.sliderTM[0][m] = newSlider("mode[" + mode.type + "]" + m, min, (int) sliderMax,
						(int) ((mode.initAmpTM[0][m] / mode.maxAmpTM[0][m]) * sliderMax), c);
				mode.typePanels[0].add(mode.sliderTM[0][m]);
				mode.typePanels[0].add(mode.sliderLabelsTM[0][m]);
				sliderPanel.add(mode.typePanels[0]);
			}
		}

		/**
		 * Listens for moving slider bars. called when a slider bar is moved.
		 * 
		 * 
		 */
		@Override
		public void stateChanged(ChangeEvent e) {
			if (isAdjusting)
				return;
			isChanged = true;
			app.updateDisplay();
		}

		void setLattLabels(double[] pLatt, double[] sLatt) {
			// set the parent and supercell lattice parameter labels
			for (int n = 0; n < 3; n++) {
				pLattLabel[n].setText(MathUtil.varToString(pLatt[n], 2, -5));
				pLattLabel[n + 3].setText(MathUtil.varToString((180 / Math.PI) * pLatt[n + 3], 2, -5));
				sLattLabel[n].setText(MathUtil.varToString(sLatt[n], 2, -5));
				sLattLabel[n + 3].setText(MathUtil.varToString((180 / Math.PI) * sLatt[n + 3], 2, -5));
			}
		}

		/**
		 * Indicate max values (average for occupation) in the subtype label.
		 * 
		 * @param t
		 * @param s
		 * @param   max[]
		 * 
		 */
		void setSubTypeText(int t, int s, double[] max) {
			subTypeLabels[t][s].setText(" " + subTypeName[t][s] + "  [" + MathUtil.varToString(max[DIS], 2, -4) + ", "
					+ MathUtil.varToString(max[OCC], 2, -4) + ", " + MathUtil.varToString(max[MAG], 2, -4) + ", "
					+ MathUtil.varToString(max[ROT], 2, -4) + "]");
		}

	}

	public void updateFormData(Map<String, Object> mapFormData, Object document) {

		// working here

		Object val;
		if (isModeActive(modes[STRAIN])) {
			val = mapFormData.get("strain1");
			if (val != null)
				mapFormData.put("strain1", modes[STRAIN].sliderValsTM[0][0]);
			val = mapFormData.get("strain2");
			if (val != null)
				mapFormData.put("strain2", modes[STRAIN].sliderValsTM[0][1]);
		}
		// now what about mode00t00m ? Need examples.

		// also change in document?

	}

	public boolean isModeActive(Mode mode) {
		return (mode != null && mode.isActive());
	}

	private double[] tempvec = new double[3];
	private double[][] tempmat = new double[3][3];
	private double[][] tempmat2 = new double[3][3];

	public CubeIterator getCubeIterator() {
		return bspt.allocateCubeIterator();
	}

	public void setAtomInfo() {
		boolean getBspt = (app.bondsUseBSPT && bspt == null);
		if (getBspt) {
			bspt = new Bspt();
		}
		double[] t = new double[3];
		double[] te = new double[6];
		for (int ia = 0, n = numAtoms; ia < n; ia++) {
			Atom a = atoms[ia];
			double[][] info = a.info;
			if (info[OCC] == null) {
				info[OCC] = new double[1];
				info[DIS] = new double[] { 0, 0, 0, ia };
				info[ROT] = new double[3];
				info[MAG] = new double[3];
				info[ELL] = new double[7];
			}
			info[OCC][0] = a.getOccupancy();
			double[] coord = setDisplacementInfo(a, t);
			if (getBspt) {
				bspt.addTuple(coord);
			}
			setArrowInfo(a, MAG, t);
			setArrowInfo(a, ROT, t);
			setEllipsoidInfo(a, te);
		}
	}

	/**
	 * Set atom.info[DIS] as [cartX, cartY, cartZ, index] 
	 * 
	 * @param a
	 * @param t
	 * @return [cartX, cartY, cartZ, index]
	 */
	private double[] setDisplacementInfo(Atom a, double[] t) {
		MathUtil.mat3mul(sBasisCart, a.get(DIS), tempvec);
		MathUtil.vecaddN(tempvec, -1, sCenterCart, a.info[DIS]);
		return a.info[DIS];
	}

	/**
	 * Calculates the description used to render a ROT or MAG arrow -- Branton Campbell
	 * 
	 * @param xyz       is the vector input.
	 * @param info is the [X-angle, Y-angle, Length] output.
	 */
	public void setArrowInfo(Atom a, int type, double[] t) {
		double[] info = a.info[type];		
		double[] xyz = a.get(type);
		MathUtil.set3(xyz, t);
		MathUtil.mat3mul(sBasisCart, t, tempvec);
		double lensq = MathUtil.lenSq3(xyz);
		if (lensq < 0.000000000001) {
			info[0] = info[1] = info[2] = 0;
			return;
		}
		double d = Math.sqrt(lensq);

// BH Q! scaling the input array??	
//
//		for (int i = 0; i < 3; i++)
//			xyz[i] /= d;
//
		// BH: added / d in asin only. Why normalize this? 
		info[0] = -Math.asin(xyz[1] / d); // X rotation
		info[1] = Math.atan2(xyz[0], xyz[2]); // Y rotation
		info[2] = d; // Length
	}

	/**
	 * Calculates the description used to render an ellipsoid -- Branton Campbell
	 * 
	 * Currently only for isotropic ellipsoids.
	 * 
	 * @param mat       is the real-symmetric matrix input.
	 * @param ellipsoid is the [widthx, widthy, widthz, rotaxisx, rotaxisy,
	 *                  rotaxisz, angle] output.
	 */
	public void setEllipsoidInfo(Atom a, double[] te) {

		MathUtil.copyN(a.get(ELL), te);
		MathUtil.voigt2matrix(te, tempmat, 0);
		MathUtil.mat3product(sBasisCart, tempmat, tempmat2);
		MathUtil.mat3copy(tempmat2, tempmat);
		MathUtil.mat3product(tempmat, sBasisCartInverse, tempmat2);
		double[] info = a.info[ELL];
		double trc = MathUtil.mat3trace(tempmat2);
//		det = matdeterminant(tempmat2);
//		for (int i = 0; i < 3; i++)
//			for (int j = 0; j < 3; j++)
//				lensq += tempmat2[i][j]* tempmat2[i][j];
//
//		if ((Math.sqrt(lensq) < 0.000001) || (det < 0.000001) || true) // "true" temporarily bypasses the ellipoidal
		// analysis.
//		{
			double avgrad = Math.sqrt(Math.abs(trc) / 3.0);
			double widths[] = new double[] { avgrad, avgrad, avgrad };
			double rotangle = 0;
			double rotaxis[] = new double[] { 0, 0, 1};
			rotangle = 0;
//		} else {
//			Matrix jamat = new Matrix(tempmat2);
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
//		}
//	System.out.println(ND[0][0]+" "+ND[0][1]+" "+ND[0][2]+" "+ND[1][0]+" "+ND[1][1]+" "+ND[1][2]+" "+ND[2][0]+" "+ND[2][1]+" "+ND[2][2]);
//	System.out.println(NV[0][0]+" "+NV[0][1]+" "+NV[0][2]+" "+NV[1][0]+" "+NV[1][1]+" "+NV[1][2]+" "+NV[2][0]+" "+NV[2][1]+" "+NV[2][2]);
//	System.out.println("lensq="+lensq+", det="+det+", w0="+widths[0]+", w1="+widths[1]+", w2="+widths[2]+", r0="+rotaxis[0]+", r1="+rotaxis[1]+", r2="+rotaxis[2]);

		info[0] = widths[0];
		info[1] = widths[1];
		info[2] = widths[2];
		info[3] = rotaxis[0];
		info[4] = rotaxis[1];
		info[5] = rotaxis[2];
		info[6] = rotangle % (2 * Math.PI) - Math.PI;
	}


	/**
	 * Uses two xyz points to a calculate a bond. -- Branton Campbell
	 * 
	 * @param bond return [x, y, z, theta, phi, len, okflag(1 or 0)]
	 * @return true if OK
	 */
	public boolean setCylinderInfo(double[] atom1, double[] atom2, double[] bond, double lensq) {
		MathUtil.vecaddN(atom2, -1, atom1, tempvec);
		if (lensq < 0) {
			lensq = MathUtil.lenSq3(tempvec);
		}
		if (lensq < 0.000000000001) {
			lensq = 0;
		} else {
			MathUtil.scale3(tempvec, 1 / Math.sqrt(lensq));
			for (int i = 0; i < 3; i++) {
				bond[i] = (atom2[i] + atom1[i]) / 2.0;
			}
		}
		bond[RX] = -Math.asin(tempvec[1]);
		bond[RY] = Math.atan2(tempvec[0], tempvec[2]);
		bond[L_2] = Math.sqrt(lensq) / 2;
		return true;
	}

	public void updateIsoVizData(Object isoData) {
		// now what?
	}

	private Map<String, double[]> knownBonds;

	public double[] getBondinfoFromKey(String key12) {
		if (knownBonds == null)
			return null;
		return knownBonds.get(key12);

	}

	public double[] addBond(String key12, int a1, int a2) {
		if (numBonds == bondInfo.length) {
			double[][] bi = new double[numBonds * 2][];
			for (int j = numBonds; --j >= 0;)
				bi[j] = bondInfo[j];
			bondInfo = bi;
		}
		double[] ab = new double[9];
		ab[6] = a1;
		ab[7] = a2;
		ab[8] = numBonds;
		bondInfo[numBonds] = ab;
		if (knownBonds == null)
			knownBonds = new HashMap<>();
		knownBonds.put(key12, ab);
		numBonds++;
		return ab;
	}

	public boolean recalcBond(int b) {
		// calculate bondInfo(x-cen, y-cen, z-cen, thetaX, thetaY,
		// length)
		double[] info = bondInfo[b];
		int a0 = (int) info[6];
		int a1 = (int) info[7];

		double[][] info0 = atoms[a0].info;
		double[][] info1 = atoms[a1].info;
		boolean ok = (info[Variables.L_2] > 0 && info[Variables.L_2] < halfMaxBondLength
				&& info0[OCC][0] > minBondOcc && info1[OCC][0] > minBondOcc);
		setCylinderInfo(info0[DIS], info1[DIS], info, -1);
		return ok;
	}

	/**
	 * Pack size is sum of total of JPanel component heights.
	 * (Prior to this it is set to the full page height.
	 */
	public void setPackedSize() {
		JComponent p= sliderPanel;
		int h = 0;
		for (int i = 0; i < p.getComponentCount(); i++) {
			h += p.getComponent(i).getHeight();  
		}
		sliderPanel.setPreferredSize(new Dimension(sliderPanel.getWidth(), h));
		sliderPanel.setSize(new Dimension(sliderPanel.getWidth(), h));
	}

}
