package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

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

	private IsoApp app;

	private VariableGUI gui;

	Atom[] atoms;

	Mode[] modes = new Mode[MODE_COUNT];

	public String isoversion;

	/**
	 * only used to hide the checkboxes
	 */
	private boolean isDiffraction;

	/** True initially or when a slider bar moves, false otherwise */
	public boolean isChanged = true;

	/** Applet width and height in pixels */
	public int appletWidth = 1024, appletHeight;
	/** Total number of atoms after filtering for primitive. */
	public int numAtoms;

	/** Number of different atom types */
	public int numTypes;

	/** Number of subtypes for each atom type. */
	public int[] numSubTypes;

	/**
	 * the final public number of atoms for specific type and subtype; either
	 * numSubAtomsRead or numPrimitiveSubAtoms
	 */
	public int[][] numSubAtoms;

	/** Names for each atom type; [type] */
	public String[] atomTypeName, atomTypeSymbol;
	/** Names for each atom type; [type] */
	public String[][] subTypeName;
	/** Radius of atoms */
	public double atomMaxRadius;
	/** Angstrom per Bohr Magneton for moment visualization */
	public double angstromsPerMagneton;
	/** Angstrom per radian for rotation visualization */
	public double angstromsPerRadian;
	/** Angstrom per Angstrom for ellipsoid visualization */
	public double defaultUiso;
	/** Maximum bond length in Angstroms beyond which bonds are not drawn */
	public double maxBondLength;
	/** Minimum atomic occupancy below which bonds are not drawn */
	public double minBondOcc;
	/** Total number of bonds */
	public int numBonds;
	/**
	 * Specifies which atoms bond together; [bond #][atom1, atom2] Indicies
	 * reference atomInfo (below)
	 */
	public int[][] bonds;

	/**
	 * If true (when at least two parents have the same element type) show the
	 * simple-color checkbox.
	 */
	public boolean needSimpleColor;

	/** Original unstrained supercell lattice parameters */
	public double[] sLatt0 = new double[6];
	/** Strained supercell parameters */
	public double[] sLatt1 = new double[6];
	/** Original unstrained parent cell parameters */
	public double[] pLatt0 = new double[6];
	/** Strained parent cell parameters */
	public double[] pLatt1 = new double[6];
	/**
	 * Parent cell origin relative to supercell origin on the unitless superlattice
	 * basis. [x, y, z]
	 */
	public double[] pOriginUnitless = new double[3];
	/**
	 * Row matrix of parent basis vectors on the unitless superlattice basis [basis
	 * vector][x,y,z]
	 */
	public double[][] Tmat = new double[3][3];
	/** Transpose of Tmat: sBasisCart.Tmat^t = pBasisCart */
	public double[][] TmatTranspose = new double[3][3];
	/** InverseTranspose of Tmat: pBasisCart.Tmat^t*i = sBasisCart */
	public double[][] TmatInverseTranspose = new double[3][3];
	/**
	 * Strained parent cell origin relative to strained supercell origin in
	 * cartesian Angstrom coords. [x, y, z]
	 */
	public double[] pOriginCart = new double[3];
	/** Center of supercell in strained cartesian Angstrom coords. [x, y, z] */
	public double[] sCenterCart = new double[3];
	/**
	 * Matrix of unstrained super basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless unstrained direct-lattice supercell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 */
	public double[][] sBasisCart0 = new double[3][3];
	/**
	 * Matrix of strained super basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless strained direct-lattice supercell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 */
	public double[][] sBasisCart = new double[3][3];
	/** Inverse of sBasisCart in Inverse-Angstrom units [basis vector][x,y,z] */
	public double[][] sBasisCartInverse = new double[3][3];
	/**
	 * Matrix of unstrained parent basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless unstrained direct-lattice parentcell
	 * coords into cartesian Angstrom coords [basis vector][x,y,z]
	 */
	public double[][] pBasisCart0 = new double[3][3];
	/**
	 * Matrix of strained parent basis vectors in cartesian Angstrom coords [basis
	 * vector][x,y,z] Transforms unitless strained direct-lattice parentcell coords
	 * into cartesian Angstrom coords [basis vector][x,y,z]
	 */
	public double[][] pBasisCart = new double[3][3];
	/**
	 * Matrix of strained super-cell crystal-axis basis vectors in unitless coords
	 * [basis vector][x,y,z] Transforms unitless strained direct-lattice supercell
	 * crystalaxis coords into cartesian coords [basis vector][x,y,z]
	 */
	public double[][] sCrystalAxisBasisCart = new double[3][3];
	/**
	 * Array of vertices of strained window-centered parent cell. [edge number][x,
	 * y, z]
	 */
	public double[][] parentCellVertices = new double[8][3];
	/**
	 * Array of vertices of strained window-centered super cell. [edge number][x, y,
	 * z]
	 */
	public double[][] superCellVertices = new double[8][3];
	/** Is the parent expressed in a rhombohedral setting? */
	public boolean isRhombParentSetting;

	/**
	 * 
	 * /** Number of irreps
	 */
	public int numIrreps;
	/**
	 * Boolean variable that tracks whether irrep sliders were last set to sliderMax
	 * or to zero.
	 */
	public boolean irrepSlidersOn = true;

	/** The master slider bar value. */
	double superSliderVal;

	/** Number of rows needed for checkboxes; [type] */
	private int[] numSubRows;
	/** Actual number of check boxes per row */
	private int[] subTypesPerRow;

	private boolean isAdjusting;

	Map<String, ArrayList<String>> myMap;

	private transient JPanel sliderPanel;

	/**
	 * just switching apps; fewer println calls
	 */
	private boolean isSwitch;

	public Variables(IsoApp app, String dataString, boolean isDiffraction, boolean isSwitch) {
		this.app = app;
		this.isSwitch = isSwitch;
		this.isDiffraction = isDiffraction;
		new VariableParser().parse(dataString);
		gui = new VariableGUI();
	}

	/**
	 * instantiates and initializes the scroll and control panels.
	 * 
	 * @param sliderPanel
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
		return modes[STRAIN].count;
	}

	public double[] getStrainmodeSliderValues() {
		return modes[STRAIN].sliderValsTM[0];
	}

	public double[] get(int mode, int i) {
		return atoms[i].vector1[mode];
	}

	public double getInitialOccupancy(int i) {
		return atoms[i].vector0[OCC][0];
	}

	public double getOccupancy(int i) {
		return atoms[i].vector1[OCC][0];
	}

	public String getAtomTypeSymbol(int i) {
		return atomTypeSymbol[atoms[i].t];
	}

	public void getColors(int t, double[] rgb) {
		rgb[0] = modes[DIS].colorT[t].getRed() / 255.0;
		rgb[1] = modes[DIS].colorT[t].getGreen() / 255.0;
		rgb[2] = modes[DIS].colorT[t].getBlue() / 255.0;
	}

	/** calculates the parent atom colors */
	public void setColors(boolean simpleColor) {
		gui.setColors(simpleColor);
	}

	public void recolorPanels() {
		gui.recolorPanels();
	}

	/** readSliders reads in the current values of each of the sliders. */
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

	public static final String ZEROES = "000000000000";
	public static final String BLANKS = "            ";

	public static String varToString(double val, int n, int w) {
		// rounding
		double incr = 0.5;
		for (int j = n; j > 0; j--)
			incr /= 10;

		if (Math.abs(val) > 0.00001)
			val += incr * (val / Math.abs(val));
		else
			val = 0;
		String s = Double.toString(val);
		int n1 = s.indexOf('.');
		int n2 = s.length() - n1 - 1;

		if (n > n2)
			s = s + ZEROES.substring(0, n - n2);
		else if (n2 > n)
			s = s.substring(0, n1 + n + 1);

		if (w > 0 & w > s.length())
			s = BLANKS.substring(0, w - s.length()) + s;
		else if (w < 0 & (-w) > s.length()) {
			w = -w;
			s = s + BLANKS.substring(0, w - s.length());
		}
		return s;
	}

	/**
	 * Naively calculate strained parent-cell basis vectors in cartesian Angstrom
	 * coords.
	 * 
	 */
	private void calculateStrain() {

		calculateParentBasisAndLattice();
		calculateChildBasisAndLattice();

		// calculate the parent cell origin in strained cartesian Angtstrom coords
		MathUtil.mul(sBasisCart, pOriginUnitless, pOriginCart);

		// calculate the 8 cell vertices in strained cartesian Angstrom coordinates
		for (int ix = 0; ix < 2; ix++) {
			for (int iy = 0; iy < 2; iy++) {
				for (int iz = 0; iz < 2; iz++) {
					for (int i = 0; i < 3; i++) {
						parentCellVertices[ix + 2 * iy + 4 * iz][i] = ix * pBasisCart[i][0] + iy * pBasisCart[i][1]
								+ iz * pBasisCart[i][2] + pOriginCart[i];
						superCellVertices[ix + 2 * iy + 4 * iz][i] = ix * sBasisCart[i][0] + iy * sBasisCart[i][1]
								+ iz * sBasisCart[i][2];
					}
				}
			}
		}
		recenterLattice();
		gui.setLattLabels(pLatt1, sLatt1);
	}

	private void calculateDistortions() {
		double[][] tempmat = new double[3][3];
		double[] tempvec = new double[3];

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

	private void recenterLattice() {
//      Calculate the center of the supercell in strained cartesian Angstrom coords (sOriginCart).
		double[][] minmax = new double[2][3];
		MathUtil.set(minmax[0], 1E6, 1e6, 1e6);
		MathUtil.set(minmax[1], -1E6, -1e6, -1e6);
		for (int i = 8; --i >= 0;) {
			MathUtil.rangeCheck(superCellVertices[i], minmax);
		}
		double[] tempvec = new double[3];
		for (int i = numAtoms; --i >= 0;) {
			Atom a = atoms[i];
			MathUtil.mul(sBasisCart, a.vector0[DIS], tempvec);
			MathUtil.rangeCheck(tempvec, minmax);
		}
		for (int i = 0; i < 3; i++)
			sCenterCart[i] = (minmax[0][i] + minmax[1][i]) / 2;

		// Place the center of the supercell at the origin
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 3; i++) {
				parentCellVertices[j][i] -= sCenterCart[i];
				superCellVertices[j][i] -= sCenterCart[i];
			}
		}
	}

	private void calculateChildBasisAndLattice() {
		// calculate the strained parent and supercell lattice parameters [a, b, c,
		// alpha, beta, gamma]

		MathUtil.recalculateLattice(sLatt1, sBasisCart);
		MathUtil.matinverse(sBasisCart, sBasisCartInverse);
	}

	private void calculateParentBasisAndLattice() {

		double[][] pStrainPlusIdentity = modes[STRAIN].getVoigtStrainTensor(superSliderVal, modes[IRREP]);
		// calculate strained parent basis vectors in cartesian Angstrom coords
		MathUtil.mul(pStrainPlusIdentity, pBasisCart0, pBasisCart);

		// calculate strained supercell basis vectors in cartesian Angstrom coords
		MathUtil.mul(pBasisCart, TmatInverseTranspose, sBasisCart);

		MathUtil.recalculateLattice(pLatt1, pBasisCart);
	}

	/**
	 * An ordered list
	 * 
	 */
	final static String[] knownTags = new String[] { //
			"isoversion", //
			"atommaxradius", //
			"angstromspermagneton", //
			"angstromsperradian", //
			"defaultuiso", //
			"maxbondlength", //
			"appletwidth", //
			"parentcell", //
			"parentorigin", //
			"parentbasis", //
			"atomtypelist", //
			"atomsubtypelist", //
			"atomcoordlist", //
			"atomocclist", //
			"atommaglist", //
			"atomrotlist", //
			"bondlist", //
			"irreplist", //
			"modes[STRAIN].list", //
			"displacivemodelist"//
	};

	public static class Atom {

		/**
		 * The index of this atom in the filtered array.
		 */

		int index;

		/**
		 * atomType, subAtomType, and atomNumber in the subtype; zero based, so one less
		 * than what we see in atom and bond lists.
		 * 
		 */
		public int t, s, a;

		/**
		 * the initial parameter vector, by mode type
		 */
		double[][] vector0 = new double[MODE_COUNT][];

		/**
		 * the final paramater vector, by mode type
		 */
		double[][] vector1 = new double[MODE_COUNT][];

		/**
		 * the IR component symmetry mode IR coefficients, originally by atomType and
		 * subtype, but because now we have this atom object, we don't need to run
		 * through those lists. We can just target an atom directly.
		 * 
		 */
		double[][][] modes = new double[MODE_COUNT][][];

		/**
		 * A class to collect all atom-related content.
		 * 
		 * @param index
		 * @param t
		 * @param s
		 * @param a
		 * @param coord
		 */
		private Atom(int index, int t, int s, int a, double[] coord) {
			this.index = index;
			this.t = t;
			this.s = s;
			this.a = a;
			vector0[DIS] = coord;
		}

		@Override
		public String toString() {
			return "[Atom " + index + " " + t + "," + s + "," + a + "]";
		}
	}

	public class VariableParser {

		private String currentTag;
		private ArrayList<String> currentData;
		private int currentDataSize;

		/**
		 * the actual sub atom numbers in the file data; only used for parsing
		 */
		int[][] numSubAtomsRead;


		private Map<String, Atom> atomMap = new HashMap<>();

		private String getKeyTSA(int t, int s, int a) {
			return t + "_" + s + "_" + a;
		}

		private String getKeyTSA(String t, String s, String a) {
			return t + "_" + s + "_" + a;
		}

		private int getAtomTSA(String tsa) {
			Atom a = atomMap.get(tsa);
			return (a == null ? -1 : a.index);
		}

		private boolean getCurrentData() {
			currentData = null;
			currentDataSize = 0;
			if (!myMap.containsKey(currentTag))
				return false;
			currentData = myMap.get(currentTag);
			currentDataSize = currentData.size();
			if (!isSwitch)
				System.out.println(currentTag + " " + currentDataSize);
			return currentDataSize > 0;
		}

		/**
		 * Parses the data string that is a tag-based format
		 * 
		 * @param dataString
		 */
		void parse(String dataString) {
			currentTag = "";
			currentData = null;

			try {
				myMap = getDataMap(dataString);
				isoversion = myMap.get("isoversion").get(0);
				parseAppletSettings();
				parseCrystalSettings();

				int[] numAtomsByTypeRead = parseAtoms();
				parseBonds();

				System.out.println("Variables: " + numAtoms + " atoms and " + numBonds + " bonds were read");

				// now get all the symmetry-related arrays
				parseAtomicMode(DIS, "displacivemodelist", 3, numAtomsByTypeRead);
				parseAtomicMode(OCC, "scalarmodelist", 1, numAtomsByTypeRead);
				parseAtomicMode(MAG, "magneticmodelist", 3, numAtomsByTypeRead);
				parseAtomicMode(ROT, "magneticmodelist", 3, numAtomsByTypeRead);
				parseAtomicMode(ELL, "ellipmodelist", 3, numAtomsByTypeRead);
				parseStrainModes();
				parseIrreps();

			} catch (Throwable t) {
				t.printStackTrace();
				parseError("Java error", 2);
			}

		}

		private void parseError(int size, int n) {
			parseError("found " + size + "; expected " + n, 1);
		}

		/**
		 * A method that centralizes error reporting
		 * 
		 * @param error The string that caused the error
		 * @param type  An int corresponding to the type of error:<br>
		 *              <li>0 = Duplicate tag<br>
		 *              <li>1 = Incorrect number of arguments for the tag<br>
		 *              <li>2 = Invalid input<br>
		 *              <li>3 = Missing required tag
		 */
		void parseError(String currentTagOrMessage, int type) {
			switch (type) {
			case 1:
				throw new RuntimeException(
						"Variables: Invalid number of arguments for tag " + currentTag + ": " + currentTagOrMessage);
			case 2:
				throw new RuntimeException("Variables: " + currentTagOrMessage + " processing " + currentTag);
			default:
				throw new RuntimeException("Variables: Required tag missing: " + currentTag);
			}
		}

		String getItem(int pt) {
			String item = currentData.get(pt);
//			System.out.println(pt + " " + item);
			return item;
		}

		int getInt(int pt) {
			return Integer.parseInt(getItem(pt));
		}

		double getDouble(int pt) {
			return Double.parseDouble(getItem(pt));
		}

		/**
		 * Just check for one value.
		 * 
		 * @param def the default value, or Double.NaN if required
		 * @return the value read or default value
		 * @throws RuntineException if required and not found
		 */
		private double getOne(double def) {
			ArrayList<String> data = myMap.get(currentTag);
			switch (data == null ? 0 : data.size()) {
			case 1:
				return Double.parseDouble(data.get(0));
			case 0:
				if (!Double.isNaN(def))
					return def;
			default:
				parseError(data.size(), 1);
				return Double.NaN;
			}
		}

		/**
		 * Get n values.
		 * 
		 * @param a
		 * @param n
		 */
		private void get(double[] a, int n) {
			checkSize(n);
			for (int i = 0; i < n; i++)
				a[i] = getDouble(i);
		}

		private boolean checkSize(int n) {
			getCurrentData();
			if (currentDataSize == 0)
				parseError(null, 3);
			if (currentDataSize != n)
				parseError(currentDataSize, n);
			return true;
		}

		/**
		 * Ensure the data are the right size for dataPerRow.
		 * 
		 * @param nCol number of columns
		 * @return number of rows
		 */

		private int checkSizeN(int nCol, boolean isRequired) {
			if (!getCurrentData() && !isRequired)
				return 0;
			int numRows = currentDataSize / nCol;
			int n = numRows * nCol;
			if (currentDataSize != n)
				parseError(currentDataSize, n);
			return numRows;
		}

//		/**
//		 * Load a Type/SubType, SubAtom [t][s][a] single value or a default
//		 * 
//		 * @param atomProp
//		 * @param def      default value or NaN to read and parse, or Double.MIN_VALUE
//		 *                 for already present and checked
//		 */
//		private void getDataTSA(double[][][] atomProp, double def) {
//			boolean isDefault = !Double.isNaN(def);
//			if (!isDefault && def != Double.MIN_VALUE) {
//				checkSize(numAtoms);
//			}
//			for (int iaRead = 0, q = 0, t = 0; t < numTypes; t++) {
//				for (int s = 0; s < numSubTypes[t]; s++) {
//					for (int pa = -1, a = 0; a < numSubAtomsRead[t][s]; a++, q++, iaRead++) {
//						if (numPrimitive > 0) {
//							if (!bsPrimitive.get(iaRead))
//								continue;
//							pa++;
//						} else {
//							pa = a;
//						}
//						atomProp[t][s][pa] = (isDefault ? def : getDouble(q));
//					}
//				}
//			}
//		}
//
		/**
		 * Load a Type/SubType, SubAtom [t][s][a] list with dataPerRow or a default
		 * 
		 * @param atomProp
		 * @param ncol
		 * @param def          a nonnegative default value or NaN to read and parse, or
		 *                     -n for already present and checked and skipping -def
		 *                     columns; so 6, -3 means six columns, skip the first three
		 *                     (as for reading atom coordinates)
		 * @param numAtomsRead
		 * @param bsPrimitive
		 */
		private void getAtomTSAn(int mode, int ncol, double def, int numAtomsRead, BitSet bsPrimitive) {
			boolean isDefault = !Double.isNaN(def);
			if (!isDefault) {
				checkSize(numAtomsRead * ncol);
			}
			for (int q = 0, ia = 0, iread = 0, n = numAtomsRead; iread < n; iread++, q += ncol) {
				if (bsPrimitive != null && !bsPrimitive.get(iread))
					continue;
				double[] data = atoms[ia++].vector0[mode] = new double[ncol];
				for (int i = 0; i < ncol; i++)
					data[i] = (isDefault ? def : getDouble(q + i));
			}
		}

		private void parseAppletSettings() {
			// find applet width
			currentTag = "appletwidth";
			int n = (int) getOne(0);
			if (n >= 500 && n <= 5000)
				appletWidth = n;
			appletHeight = (int) Math.round((double) appletWidth / 1.6);

		}

		private void parseCrystalSettings() {
			// find parentcell parameters
			currentTag = "parentcell";
			get(pLatt0, 6);
			// find parentCell origin within the superCell basis.
			currentTag = "parentorigin";
			get(pOriginUnitless, 3);

			// find parentCell parameters within superCell basis.
			currentTag = "parentbasis";
			checkSize(9);
			for (int j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++) {
					Tmat[j][i] = getDouble(3 * j + i);
					MathUtil.mattranspose(Tmat, TmatTranspose);
					MathUtil.matinverse(TmatTranspose, TmatInverseTranspose);
				}
			}
			currentTag = "rhombparentsetting";
			isRhombParentSetting = false;
			if (myMap.containsKey(currentTag)) {
				checkSize(1);
				isRhombParentSetting = Boolean.parseBoolean(getItem(0));
			}

			currentTag = "angstromspermagneton";
			angstromsPerMagneton = getOne(0.5);

			currentTag = "angstromsperradian";
			angstromsPerRadian = getOne(0.5);

		}

		private int[] parseAtoms() {

			/**
			 * number of atoms in the file, before filtering for primitives
			 */
			int numAtomsRead = 0;

			/**
			 * this bitset is read first and then used to filter atoms and atom properties
			 */
			BitSet bsPrimitive = null;

			/**
			 * the number of primitive atoms, if this is for IsoDifrract; in the end, we
			 * will replace numSubAtoms with numSubPrimitiveAtoms
			 */
			int[][] numPrimitiveSubAtoms = null;

			/**
			 * number of atoms of a given type
			 */
			int[] numAtomsByTypeRead = null;

			currentTag = "atommaxradius";
			atomMaxRadius = getOne(0.4);

			currentTag = "defaultuiso";
			defaultUiso = getOne(0);
			if (defaultUiso <= 0) {
				double d = atomMaxRadius / 2.0;
				defaultUiso = d * d;
			}

			if (isDiffraction) {
				// flag all atoms in the primitive cell.
				currentTag = "!atomsinunitcell";
				getCurrentData();
				int n = currentDataSize;
				if (n > 0) {
					bsPrimitive = new BitSet();
					for (int i = 0; i < n; i++) {
						if ("1".equals(getItem(i))) {
							bsPrimitive.set(i);
						}
					}
					numAtoms = bsPrimitive.cardinality();
				}
			}

			// create an atom map from t_s_a to ia (actual, not read)
			currentTag = "atomtypelist";
			numTypes = checkSizeN(3, true);
			atomTypeName = new String[numTypes];
			atomTypeSymbol = new String[numTypes];
			numAtomsByTypeRead = new int[numTypes];
			if (bsPrimitive != null) {
				numPrimitiveSubAtoms = new int[numTypes][];
			}
			parseAtomTypes(numAtomsByTypeRead);
			// find atomic coordinates of parent
			currentTag = "atomcoordlist";
			if (!getCurrentData()) {
				parseError(currentTag, 3);
			}
			int dataPerRow = 6;
			int ndata = currentDataSize;
			numAtomsRead = ndata / dataPerRow;
			int dataPerRowOld = 10;
			int numAtomsOld = ndata / dataPerRowOld;
			if (numAtomsOld * dataPerRowOld == ndata) {
				// could be 60n atoms (as in test28)
				// get the first number on the second line (if new format)
				// if that is a float, then we must have the old format.
				String s = getItem(dataPerRow);
				if (s.indexOf('.') >= 0) {
					dataPerRow = dataPerRowOld;
					numAtomsRead = numAtomsOld;
				}
			}
			if (ndata != numAtomsRead * dataPerRow) {
				// Make sure it divided evenly
				parseError(currentTag, 1);
			}
			// numAtoms may be the number of primitive atoms only
			if (numAtoms == 0)
				numAtoms = numAtomsRead;
			atoms = new Atom[numAtoms];

			// Find number of subatoms for each subtype
			// Set up numSubAtom and numPrimitiveSubAtoms if this is for IsoDiffract

			int nType = 0, lastt = 0;
			for (int i = 0, ia = 0; i < numAtomsRead; i++) {
				int pt = dataPerRow * i;
				int t = getInt(pt++) - 1;
				if (t != lastt) {
					numAtomsByTypeRead[lastt] = nType;
					nType = 0;
					lastt = t;
				}
				int s = getInt(pt++) - 1;
				boolean isOK = (bsPrimitive == null || bsPrimitive.get(i));
				if (isOK) {
					int a = getInt(pt++) - 1;
					double[] coord = new double[] { getDouble(pt++), getDouble(pt++), getDouble(pt++) };
					if (bsPrimitive != null) {
						numPrimitiveSubAtoms[t][s]++;
					}
					Atom atom = atoms[ia] = new Atom(ia, t, s, a, coord);
					atomMap.put(getKeyTSA(t + 1, s + 1, a + 1), atom);
					ia++;
				}
				nType++;
				numSubAtomsRead[t][s]++;
			}
			numAtomsByTypeRead[lastt] = nType;
			numSubAtoms = (bsPrimitive == null ? numSubAtomsRead : numPrimitiveSubAtoms);
			for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
				// (STRAIN and IRRED are handled later)
				(modes[i] = new Mode(i, numTypes, numSubTypes, numSubAtoms)).initAtoms();
			}
			modes[DIS].isActive = true;

			// find atomic occupancies of parent
			currentTag = "atomocclist";
			getAtomTSAn(OCC, 1, (myMap.containsKey(currentTag) ? Double.NaN : 1), numAtomsRead, bsPrimitive);

			// find atomic magnetic moments of parent
			currentTag = "atommaglist";
			getAtomTSAn(MAG, 3, (myMap.containsKey(currentTag) ? Double.NaN : 0), numAtomsRead, bsPrimitive);

			// find atomic rotations of parent
			currentTag = "atomrotlist";
			getAtomTSAn(ROT, 3, (myMap.containsKey(currentTag) ? Double.NaN : 0), numAtomsRead, bsPrimitive);

			// find atomic adp ellipsoids of parent
			currentTag = "atomelplist";
			if (myMap.containsKey(currentTag)) {
				getAtomTSAn(ELL, 6, Double.NaN, numAtomsRead, bsPrimitive);
			} else {
				// default to [0.04 0.04 0.04 0 0 0]
				double[] def = new double[] { defaultUiso, defaultUiso, defaultUiso, 0, 0, 0 };
				for (int ia = 0; ia < atoms.length; ia++)
					atoms[ia].vector0[ELL] = def;
			}

			if (bsPrimitive != null)
				System.out.println("Variables: primitive: " + bsPrimitive);

			return numAtomsByTypeRead;
		}

		private void parseAtomTypes(int[] numAtomsByTypeRead) {
			// find atom types

			for (int i = 0; i < numTypes; i++) {
				if (getInt(3 * i) != i + 1)
					parseError(currentTag, 2);
				atomTypeName[i] = getItem(3 * i + 1);
				atomTypeSymbol[i] = getItem(3 * i + 2);
			}

			numSubTypes = new int[numTypes];
			numSubAtomsRead = new int[numTypes][];

			// find atom subtypes
			currentTag = "atomsubtypelist";
			int numSubTypeEntries = checkSizeN(3, false);
			if (numSubTypeEntries > 0) {
				int curType = 0, curSubType = 0, nType = 0;
				for (int i = 0; i < numSubTypeEntries; i++) {
					int itype = getInt(3 * i);
					if (itype != curType) {
						if (curSubType != 0) {
							numSubAtomsRead[curType - 1] = new int[curSubType];
							numAtomsByTypeRead[curType - 1] = nType;
						}
						curType++;
						curSubType = 0;
						if (itype != curType)
							parseError("The atom types in the atom subtype list are out of order", 2);
					}
					int subtype = getInt(3 * i + 1);
					if (subtype != curSubType) {
						numSubTypes[curType - 1]++;
						curSubType++;
						if (subtype != curSubType)
							parseError("The subtypes in the atom subtype list are out of order", 2);
					}
				}
				numSubAtomsRead[curType - 1] = new int[curSubType];

				// Assign the subtype names
				subTypeName = new String[numTypes][];
				for (int pt = 0, t = 0; t < numTypes; t++) {
					subTypeName[t] = new String[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) {
						subTypeName[t][s] = getItem(3 * pt++ + 2);
					}
				}
			} else {
				// This code is used in ISOCIF, where there are no subtypes defined.
				subTypeName = new String[numTypes][];
				for (int t = 0; t < numTypes; t++) {
					numSubTypes[t] = 1;
					subTypeName[t] = new String[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) {
						subTypeName[t][s] = atomTypeName[t] + "_" + (s + 1);
					}
				}
			}

		}

		private void parseBonds() {
			currentTag = "maxbondlength";
			maxBondLength = getOne(2.5);
			// find minimum atomic occupancy for which bonds should be displayed
			currentTag = "minbondocc";
			minBondOcc = getOne(0.5);
			currentTag = "bondlist";
			int numBondsRead = checkSizeN(6, false);
			int[][] bondsTemp = null;
			if (numBondsRead > 0) {
				// find maximum length of bonds that can be displayed
				bondsTemp = new int[numBondsRead][2];
				for (int b = 0, pt = 0; b < numBondsRead; b++) {
					String keyA = parseTSA(pt);
					pt += 3;
					String keyB = parseTSA(pt);
					pt += 3;
					int ia = getAtomTSA(keyA);
					if (ia >= 0) {
						int ib = getAtomTSA(keyB);
						if (ib >= 0) {
							bondsTemp[b][0] = ia;
							bondsTemp[b][0] = ib;
							numBonds++;
						}
					}

				}
			}
			if (numBonds == 0) {
				// There are no bonds. Initialize the array to length zero
				bonds = new int[0][];
			} else if (numBonds == numBondsRead) {
				bonds = bondsTemp;
			} else {
				bonds = new int[numBonds][2];
				for (int i = 0; i < numBonds; i++)
					bonds[i] = bondsTemp[i];
			}
		}

		/**
		 * Create an atom map key int the form "t_s_a"
		 * 
		 * @param pt
		 * @return
		 */
		private String parseTSA(int pt) {
			return getKeyTSA(getItem(pt++), getItem(pt++), getItem(pt++));
		}

		/**
		 * Parse "irreplist" for
		 */
		private void parseIrreps() {
			// find irreps
			currentTag = "irreplist";
			if (!getCurrentData())
				return;
			if (currentDataSize % 2 != 0)
				parseError(currentTag, 1);
			int numIrreps = currentDataSize / 2;
			modes[IRREP] = new Mode(IRREP, numTypes, numSubTypes, numSubAtoms);
			modes[IRREP].initArraysNonAtom(numIrreps);
			modes[IRREP].nameTM[0] = new String[numIrreps];
			for (int i = 0; i < numIrreps; i++) {
				if (getInt(2 * i) != i + 1)
					parseError("Error: irreps are not passed in ascending order.", 2);
				modes[IRREP].nameTM[0][i] = getItem(2 * i + 1);
			}
		}

		private void parseAtomicMode(int mode, String tag, int columnCount, int[] numAtomsByTypeRead) {
			currentTag = tag;
			int[] perType = new int[numTypes];
			int n = getModeNumbers(perType, 3, numAtomsByTypeRead);
			if (n == 0)
				return;
			modes[mode].initArraysMode(perType, n);
			modes[mode].getModeData(this);
		}

		/**
		 * read the mode numbers for any of the five mode types.
		 * 
		 * @param perType
		 * @param ncol
		 * @param numAtomsByTypeRead
		 * @return
		 */
		private int getModeNumbers(int[] perType, int ncol, int[] numAtomsByTypeRead) {
			getCurrentData();
			// Run through the data once and determine the number of modes
			int n = 0;
			for (int pt = 0; pt < currentDataSize;) {
				int atomType = getInt(pt) - 1;
				perType[atomType]++;
				n++;
				// advance pt to next atom type number
				pt += 6 + numAtomsByTypeRead[atomType] * ncol;
			}
			return n;
		}

		private void parseStrainModes() {
			// Handle strain modes
			int n = 0;
			currentTag = "strainmodelist";
			if (getCurrentData()) {
				int dataPerRow = 11;
				n = currentDataSize / dataPerRow;
				if (currentDataSize != n * dataPerRow) // Make sure it divided evenly
				{
					parseError(currentTag, 1);
				}
				modes[STRAIN] = new Mode(STRAIN, numTypes, numSubTypes, numSubAtoms);
				modes[STRAIN].initArraysNonAtom(n);
				for (int m = 0; m < n; m++) {
					modes[STRAIN].initAmpTM[0][m] = getDouble(dataPerRow * m + 1);
					modes[STRAIN].maxAmpTM[0][m] = getDouble(dataPerRow * m + 2);
					modes[STRAIN].irrepTM[0][m] = getInt(dataPerRow * m + 3) - 1;
					modes[STRAIN].nameTM[0][m] = getItem(dataPerRow * m + 4);
					for (int i = 0; i < 6; i++) {
						modes[STRAIN].vector[m][i] = getDouble(dataPerRow * m + i + 5);
					}
				}
			}

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
			MathUtil.mul(pBasisCart0, TmatInverseTranspose, sBasisCart0);
		}

		private Map<String, ArrayList<String>> getDataMap(String dataString) {
			Map<String, ArrayList<String>> myMap = new TreeMap<String, ArrayList<String>>();
			StringTokenizer getData = new StringTokenizer(dataString);
			currentData = new ArrayList<String>();
			String currentTag = "";
			while (getData.hasMoreTokens()) {
				String next = getData.nextToken();
				if (next.charAt(0) == '!') {
					// We found a new tag, so put the old one in the map
					if (currentTag != "") {
						if (myMap.containsKey(currentTag)) // Duplicate tag
							parseError("Duplicate tag ", 0);
						myMap.put(currentTag, currentData);
						currentData = new ArrayList<String>();
					}
					currentTag = next.substring(1).toLowerCase();
				} else if (currentTag != "") {
					currentData.add(next);
				}
			}
			myMap.put(currentTag, currentData);
			return myMap;
		}

	}

	private class VariableGUI implements ChangeListener {

		private final static int subTypeWidth = 170;
		private final static int barheight = 20;
		private final static int subTypeBoxWidth = 15, superSliderLabelWidth = 66, sliderLabelWidth = 36,
				lattWidth = 60;

		/**
		 * Master (top most) slider bar controls all slider bars for superpositioning of
		 * modes.
		 */
		private JSlider superSlider;
		/** Array of strain mode slider bars. */
		private JLabel superSliderLabel;

		/** Array of labels for supercell lattice parameters */
		private JLabel[] sLattLabel = new JLabel[6];
		/** Array of labels for parent lattice parameters */
		private JLabel[] pLattLabel = new JLabel[6];
		/** names for parent and super cell parameter titles */
		private JLabel parentLabel, superLabel;
		/** Array of labels for atom types */
		private JLabel typeLabel[];
		/** subTypeLabels */
		private JLabel[][] subTypeLabels;
		/** Array of checkboxes -- one for each atomic subtype */
		private JCheckBox[][] subTypeBoxes;

		/** Panel which holds master slider bar and it's label; added scrollPanel */
		private JPanel masterSliderPanel;
		/** Panel which holds the parent atom type */
		private JPanel typeNamePanels[];
		/**
		 * Panel which holds the occupancy check boxes for each atom subtype associated
		 * with a type
		 */
		private JPanel typeDataPanels[];

		/**
		 * Maximum slider bar value. The slider bar only takes integer values. So the
		 * sliderMax is the number of notches and therefore the precision of the slider
		 * bar. More notches make for more precise slider movements but slow the
		 * rendering a lot since it renders at every notch
		 */
		private int sliderMax = 100;
		/** double version of sliderMax */
		private double sliderMaxVal = sliderMax;

		private int sliderWidth;
		private int sliderPanelWidth;

		/** Total number of unique parent atom types. */
		public int numUniques;

		/**
		 * Integer index that identifies each parent atom type as one of several unique
		 * parent atom types.
		 */
		public int[] atomTypeUnique;
		
		/** resets the background colors of the panel components */
		void recolorPanels() {
			for (int t = 0; t < numTypes; t++) {
				// Reset the SliderBarPanel colors.
				Color c = modes[DIS].colorT[t];
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
				if (modes[i].isActive) {
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
				if (modes[i].isActive)
					modes[i].setValues(v.modes[i]);
			}
		}

		/**
		 * Copy slider settings to fields and set the slider values text.
		 * 
		 */
		void readSliders() {
			superSliderVal = superSlider.getValue() / sliderMaxVal;
			superSliderLabel.setText(varToString(superSliderVal, 2, -6) + " child");
			for (int i = 0; i < MODE_COUNT; i++) {
				if (modes[i].isActive)
					modes[i].readSlider(superSliderVal, sliderMaxVal, modes[IRREP]);
			}
		}

		/**
		 * Set the sizes of panels based on
		 * 
		 * @param sliderPanel
		 * @param controlPanel
		 */
		void initPanels() {

			// Divide the applet area with structure on the left and controls on the right.

			Dimension dim = sliderPanel.getPreferredSize();

			sliderPanelWidth = dim.width;
			sliderWidth = sliderPanelWidth / 2;

			// width - height to create a roughly square panel

			// Calculate numRows of each subtype
			/** Number of extra rows above slider bar panel (for view panel) */
			int numExtraRows = 5;
			// One for master slider bar, one for strainTitle, one for irrepTitle, and 2 for
			// the lattice params.
			/** Minimum number of rows in grid that will keep the rows thin */
			int minRowNumber = (int) Math.floor(dim.height / barheight);

			/** Maximum number of check boxes per row the GUI will hold */
			int maxSubTypesPerRow = (int) Math.floor(dim.width / subTypeWidth);

			int numSubRowsTotal = 0;
			numSubRows = new int[numTypes];
			subTypesPerRow = new int[numTypes];
			for (int t = 0; t < numTypes; t++) {
				// iterate over types
				subTypesPerRow[t] = Math.min(maxSubTypesPerRow, numSubTypes[t]);
				numSubRows[t] = (int) Math.ceil((double) numSubTypes[t] / subTypesPerRow[t]);
				numSubRowsTotal += numSubRows[t];
			}
			int rowCount = modes[DIS].count + modes[OCC].count + modes[MAG].count + modes[ROT].count + modes[ELL].count
					+ modes[STRAIN].count + numIrreps + numTypes + numExtraRows + numSubRowsTotal;
			int rowLength = Math.max(rowCount, minRowNumber);
			sliderPanel.setPreferredSize(new Dimension(dim.width, rowLength * barheight));
			addControls();
		}

		/**
		 * Create the full slider panel.
		 * 
		 */
		private void addControls() {

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
				Color c = modes[DIS].colorT[t];
				JPanel tp = new JPanel(new GridLayout(2, 1));
				tp.setBorder(new EmptyBorder(2, 2, 5, 2));
				tp.setBackground(c);
				subTypeBoxes[t] = new JCheckBox[numSubTypes[t]];
				subTypeLabels[t] = new JLabel[numSubTypes[t]];
				typeLabel[t] = newLabel("" + atomTypeName[t] + " Modes", sliderPanelWidth, c, JLabel.CENTER);
				typeNamePanels[t] = new JPanel(new GridLayout(1, 1, 0, 0));
				typeNamePanels[t].setPreferredSize(new Dimension(sliderPanelWidth, barheight));
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
					p.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
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
			b.setVerticalAlignment(JCheckBox.CENTER);
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
		 */
		private void initModeGUI(Mode mode, int t, JPanel sliderPanel) {
			if (!mode.isActive)
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
		 */
		private void initNonAtomGUI(Mode mode, Color c) {
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
				pLattLabel[n].setText(varToString(pLatt[n], 2, -5));
				pLattLabel[n + 3].setText(varToString((180 / Math.PI) * pLatt[n + 3], 2, -5));
				sLattLabel[n].setText(varToString(sLatt[n], 2, -5));
				sLattLabel[n + 3].setText(varToString((180 / Math.PI) * sLatt[n + 3], 2, -5));
			}
		}

		/**
		 * Indicate max values (average for occupation) in the subtype label.
		 * 
		 * @param t
		 * @param s
		 * @param   max[]
		 */
		void setSubTypeText(int t, int s, double[] max) {
			subTypeLabels[t][s].setText(
					" " + subTypeName[t][s] + "  [" + varToString(max[DIS], 2, -4) + ", " + varToString(max[OCC], 2, -4)
							+ ", " + varToString(max[MAG], 2, -4) + ", " + varToString(max[ROT], 2, -4) + "]");
		}

	}

	public void updateFormData(Object mapFormData) {
//		+ "\"mode001001\":\".1\"," 
//		+ "\"mode002001\":\".2\"," 
//		+ "\"mode003001\":\".05\","
//		+ "\"mode003002\":\".15\"," 
//		+ "\"mode003003\":\"0\"," 

		// TODO
	}

}
