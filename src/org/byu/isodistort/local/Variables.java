package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.byu.isodistort.local.Bspt.CubeIterator;

/**
 * A class to maintain all aspects of variables for ISODISTORT and ISODIFFRACT.
 * 
 * Contains the following inner classes:
 * 
 * 
 * Atom (inner static)
 * 
 * Cell (inner static)
 * 
 * IsoParser (inner)
 * 
 * SliderPanelGUI (inner)
 * 
 * 
 * @author Bob Hanson
 *
 */
public class Variables {

	public final static int DIS = Mode.DIS; // displacive
	public final static int OCC = Mode.OCC; // occupancy (aka "scalar")
	public final static int MAG = Mode.MAG; // magnetic
	public final static int ROT = Mode.ROT; // rotational
	public final static int ELL = Mode.ELL; // ellipsoidal
	private final static int MODE_ATOMIC_COUNT = Mode.ELL + 1; // 5 (same as STRAIN)

	public final static int STRAIN = Mode.STRAIN;
	public final static int IRREP = Mode.IRREP; // irreducible representations
	private final static int MODE_COUNT = Mode.MODE_COUNT;

	public final static int RX = 3, RY = 4, L_2 = 5;

	/**
	 * BH added -- allows showing of zero-value and IRREP-adjusted slider pointers
	 */
	final static boolean showSliderPointers = true;
	/**
	 * Maximum slider bar value. The slider bar only takes integer values. So the
	 * sliderMax is the number of notches and therefore the precision of the slider
	 * bar. More notches make for more precise slider movements but slow the
	 * rendering a lot since it renders at every notch
	 * 
	 * BH: changed from 100 to 1000 for precision in pointer positions. Does not
	 * seem to negatively impact rendering, as repaint() automatically buffers.
	 * 
	 */
	final static int sliderMax = 1000;
	/**
	 * double version of sliderMax
	 */
	final static double maxJSliderIntVal = sliderMax;

	private IsoApp app;

	private SliderPanelGUI gui;

	public Atom[] atoms;

	/**
	 * a bitset used in IsoDiffractApp to
	 * filter atoms and atom properties. It is provides an option to display
	 * only primitive atoms in IsoDistortApp.
	 * 
	 */
	BitSet bsPrimitive = null;

	Mode[] modes = new Mode[MODE_COUNT];

	public String isoversion;

	/**
	 * used to flag issues about incommensurately modulated structures;
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
	int[][] numSubAtoms;

	/**
	 * Names for each atom type; [type]
	 */
	String[] atomTypeName, atomTypeSymbol;
	/**
	 * Names for each atom type; [type]
	 */
	String[][] subTypeName;
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
	 * If true (when at least two parents have the same element type) show the
	 * simple-color checkbox.
	 * 
	 */
	public boolean needSimpleColor;
	
	/**
	 * cell for the child, aka "super" cell
	 * 
	 */
	public Cell childCell = new Cell(true);

	/**
	 * cell for the parent
	 */
	public Cell parentCell = new Cell(false);

	/**
	 * Row matrix of parent basis vectors on the unitless childlattice basis [basis
	 * vector][x,y,z]; set by parser from ISOVIZ "parentbasis" value
	 * 
	 */
	public double[][] Tmat = new double[3][3];

	/**
	 * Center of cell in strained cartesian Angstrom coords. [x, y, z]
	 */
	private double[] cartesianCenter = new double[3];

	/**
	 * temporary variables
	 */
	private double[] tempvec = new double[3];
	private double[][] tempmat = new double[3][3];
	private double[] tA = new double[3];
	private double[] tB = new double[3];

	/**
	 * Boolean variable that tracks whether irrep sliders were last set to sliderMax
	 * or to zero.
	 * 
	 */
	boolean irrepSlidersOn = true;

	/**
	 * The master slider bar value.
	 */
	private double childFraction;

	/**
	 * ignore events; we are adjusting and will update when done
	 * 
	 */
	private boolean isAdjusting;

	/**
	 * Binary Space Partitioning Tree for speedy determination of bonded atoms
	 * 
	 */
	private Bspt bspt;
	
	
	/**
	 * flag to indicate that this is an incommensurately modulated structure;
	 * 0 means not incommensurate
	 */
	public int modDim;

	public Variables(IsoApp app) {
		this.app = app;
		isDiffraction = (app.appType == IsoApp.APP_ISODIFFRACT);
	}

	/**
	 * This method accepts String, byte[] or InputStream and delivers back the
	 * byte[] for passing to a switched in app (ISODistort to ISODiffract, for
	 * example). If it is an InputStream it will close the stream.
	 * 
	 * @param datan
	 * @return byte[]
	 */
	public boolean parse(IsoTokenizer vt) throws RuntimeException {
		if (!new IsoParser().parse(vt))
			return false;
		gui = new SliderPanelGUI();
		return true;
	}

	/**
	 * instantiates and initializes the scroll and control panels.
	 * 
	 * @param sliderPanel
	 * @param d 
	 * 
	 */
	public void initSliderPanel(JPanel sliderPanel, int width) {
		gui.initPanels(sliderPanel, width);
	}

	public void setAnimationAmplitude(double animAmp) {
		gui.mainSlider.setValue((int) Math.round(animAmp * maxJSliderIntVal));
	}

	public boolean isSubTypeSelected(int t, int s) {
		return gui.subTypeCheckBoxes[t][s].isSelected();
	}

	public boolean allAtomsSelected() {
		for (int t = gui.subTypeCheckBoxes.length; --t >= 0;) {
			JCheckBox[] boxes = gui.subTypeCheckBoxes[t];
			for (int s = boxes.length; --s >= 0;) {
				if (!boxes[s].isSelected())
					return false;
			}
		}
		return true;
	}
	
	public Atom getAtom(int ia) {
		return atoms[ia];
	}

	public double[][] getAtomInfo(int a) {
		return atoms[a].info;
	}

	public void enableSubtypeSelection(boolean tf) {
		for (int t = 0; t < numTypes; t++)
			for (int s = 0; s < numSubTypes[t]; s++)
				gui.subTypeCheckBoxes[t][s].setEnabled(tf);// was false
	}

	public void selectAllSubtypes() {
		for (int t = 0; t < numTypes; t++)
			for (int s = 0; s < numSubTypes[t]; s++)
				gui.subTypeCheckBoxes[t][s].setSelected(true);// was false
	}

	public double getSetChildFraction(double newVal) {
		double d = childFraction;
		if (!Double.isNaN(newVal))
			childFraction = newVal;
		return d;
	}

	public int getStrainModesCount() {
		return (modes[STRAIN] == null ? 0 : modes[STRAIN].count);
	}

	public double[] getStrainmodeSliderValues() {
		return (modes[STRAIN] == null ? new double[0] : modes[STRAIN].values[0]);
	}

	public void getColors(int t, double[] rgb) {
		Color c = getDefaultModeColor(t);
		rgb[0] = c.getRed() / 255.0;
		rgb[1] = c.getGreen() / 255.0;
		rgb[2] = c.getBlue() / 255.0;
	}

	public void updateColorScheme(boolean simpleColor) {
		gui.updateColorScheme(simpleColor);
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
		gui.updateColorScheme(v.gui.simpleColor);
		isAdjusting = false;
		app.updateDimensions();
		app.updateDisplay();
	}

	public void saveModeValues() {
		// Save old childSliderVal and set it to 1.0
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
	 * recalcDistortion recalculates the positions and occupancies based on current
	 * slider values.
	 * 
	 */
	public void recalcDistortion() {

		// Calculate strained parent and child unit cell basis vectors in cartesian Angstrom
		// coordinates.

		double[][] pStrainPlusIdentity = (modes[STRAIN] == null
				? MathUtil.voigt2matrix(new double[6], new double[3][3], 1)
				: modes[STRAIN].getVoigtStrainTensor(childFraction, modes[IRREP]));
		parentCell.setStrainedCartesianBasis(pStrainPlusIdentity);
		parentCell.transformParentToChild(childCell, true);
		parentCell.setStrainedCartesianOrigin(childCell.toTempCartesian(parentCell.originUnitless));
		parentCell.setStrainedVertices();
		parentCell.setLatticeParameterLabels();
		childCell.setStrainedVertices();
		childCell.setLatticeParameterLabels();
		recenterLattice();

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
		modes[MAG].calcDistortion(this, max, tempvec, null);
		modes[ROT].calcDistortion(this, max, tempvec, null);
		modes[ELL].calcDistortion(this, max, tempvec, tempmat);

		for (int t = 0; t < numTypes; t++) {
			for (int s = 0; s < numSubTypes[t]; s++) {
				gui.setSubTypeText(t, s, max[t][s]);
			}
		}

	}

	/**
	 * Calculate the center of the child cell in strained cartesian Angstrom coords.
	 * 
	 */
	private void recenterLattice() {
		double[][] minmax = new double[2][3];
		MathUtil.set3(minmax[0], 1E6, 1e6, 1e6);
		MathUtil.set3(minmax[1], -1E6, -1e6, -1e6);
		childCell.rangeCheckVertices(minmax);
		for (int i = numAtoms; --i >= 0;) {
			MathUtil.rangeCheck(childCell.toTempCartesian(atoms[i].vectorBest[DIS]), minmax);
		}
		MathUtil.average3(minmax[0], minmax[1], cartesianCenter);
		childCell.setRelativeTo(cartesianCenter);
		parentCell.setRelativeTo(cartesianCenter);
	}

	Color getDefaultModeColor(int t) {
		int i = getFirstActiveMode();
		return i < 0 ? Color.RED : modes[i].colorT[t];
	}

	/**
	 * Active modes are ones that have ISOVIZ nonzero data values.
	 * 
	 * @return first active mode, or -1 if there are none
	 */
	int getFirstActiveMode() {
		for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
			if (isModeActive(modes[i])) {
				return i;
			}
		}
		return -1;
	}

	public void setModeFormData(Map<String, Object> mapFormData, String sliderSetting) {
		boolean toZero = "parent".equals(sliderSetting);
		boolean toBest = "child".equals(sliderSetting);
		// otherwise current
		for (int mode = 0; mode < MODE_ATOMIC_COUNT; mode++) {
			if (isModeActive(modes[mode])) {
				double[][] vals = modes[mode].values;
				for (int t = vals.length; --t >= 0;) {
					for (int m = vals[t].length; --m >= 0;) {
						String name = getInputName(mode, t, m);
						double d = (toZero ? 0 : toBest ? modes[mode].calcAmpTM[t][m] : vals[t][m]);
						setModeFormValue(name, d, mapFormData, null);
					}
				}
			}
		}
		if (isModeActive(modes[STRAIN])) {
			double[] vals = modes[STRAIN].values[0];
			for (int m = vals.length; --m >= 0;) {
				String name = getInputName(STRAIN, 0, m);
				double d = (toZero ? 0 : toBest ? modes[STRAIN].calcAmpTM[0][m] : vals[m]);
				setModeFormValue(name, d, mapFormData, null);
			}
		}
	}

	public void updateModeFormData(Map<String, Object> mapFormData, Object document) {

		// working here

		for (int mode = 0; mode < MODE_ATOMIC_COUNT; mode++) {
			if (isModeActive(modes[mode])) {
				double[][] vals = modes[mode].values;
				for (int t = vals.length; --t >= 0;) {
					for (int m = vals[t].length; --m >= 0;) {
						String name = getInputName(mode, t, m);
						setModeFormValue(name, vals[t][m], mapFormData, document);
					}
				}
			}
		}
		if (isModeActive(modes[STRAIN])) {
			double[] vals = modes[STRAIN].values[0];
			for (int m = vals.length; --m >= 0;) {
				String name = getInputName(STRAIN, 0, m);
				setModeFormValue(name, vals[m], mapFormData, document);
			}
		}
	}

	/**
	 * Set the form value as currently specified. Also sets the value in the
	 * document if we are online in JavaScript. Note that this method is only for
	 * text fields.
	 * 
	 * @param name
	 * @param val
	 * @param mapFormData
	 * @param document
	 */
	private void setModeFormValue(String name, double val, Map<String, Object> mapFormData, Object document) {
		String err = null;
		if (mapFormData.containsKey(name)) {
			String s = MathUtil.varToString(val, 5, 0);
			mapFormData.put(name, s);
			if (document != null) {
				/**
				 * @j2sNative var d = document.getElementsByName(name)[0]; if (d) { d.value = s;
				 *            } else { err= "Variable " + name + " was not found in the
				 *            document"; }
				 */
			}
		} else {
			err = "Variable " + name + " was not found in form data";
		}
		if (err != null)
			app.addStatus(err);
	}

	static String getInputName(int type, int t, int m) {
		String name = "";
		switch (type) {
		case STRAIN:
			return "strain" + (m + 1);
		case IRREP:
			return "irrep" + (m + 1);
		case DIS:
			name = "mode";
			break;
		case OCC:
			name = "scalar";
			break;
		case MAG:
			name = "magmode";
			break;
		case ROT:
			name = "rotmode";
			break;
		case ELL:
			name = "ellipmode"; // BH guessing here
			break;
		}
		String sm = "000" + (m + 1);
		String st = "000" + (t + 1);
		name += st.substring(st.length() - 3) + sm.substring(sm.length() - 3);
		return name;
	}

	public boolean isModeActive(Mode mode) {
		return (mode != null && mode.isActive());
	}

	public CubeIterator getCubeIterator() {
		return bspt.allocateCubeIterator();
	}

	public void setAtomInfo() {
		boolean getBspt = (bspt == null);
		if (getBspt) {
			bspt = new Bspt();
		}
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
			double[] coord = a.setDisplacementInfo(childCell, cartesianCenter);
			if (getBspt) {
				// This coordinate is saved, not copied.
				// The binary space partition tree thus.
				// technically we should add these fresh every time.
				// but I think this is close enough. Prove me wrong! BH
				bspt.addTuple(coord);
			}
			a.setArrowInfo(MAG, childCell);
			a.setArrowInfo(ROT, childCell);
			a.setEllipsoidInfo(childCell);
		}
	}

	/**
	 * Uses two xyz points to a calculate a bond. -- Branton Campbell
	 * 
	 * @param info return [x, y, z, theta, phi, len, okflag(1 or 0)]
	 */
	public void setCylinderInfo(double[] pt1, double[] pt2, double[] info, double lensq) {
		MathUtil.vecaddN(pt2, -1, pt1, tempvec);
		if (lensq < 0) {
			lensq = MathUtil.lenSq3(tempvec);
		}
		MathUtil.scale3(tempvec, 1 / Math.sqrt(lensq), tempvec);
		MathUtil.average3(pt1, pt2, info);
		info[RX] = -Math.asin(tempvec[1]);
		info[RY] = Math.atan2(tempvec[0], tempvec[2]);
		info[L_2] = Math.sqrt(lensq) / 2;
	}

	public void keyTyped(KeyEvent e) {
		gui.keyTyped(e);
	}

	public void resetSliders() {
		gui.resetSliders();
	}

	/**
	 * Local only; there was no formData object.
	 * 
	 * @return prefs map
	 */
	public Map<String, Object> getPreferences() {
		Map<String, Object> prefs = new HashMap<>();
		prefs.put("atomicradius", "" + atomMaxRadius);
		prefs.put("bondlength", "" + (halfMaxBondLength * 2));
		for (int i = 0; i < serverParams.length; i++) {
			prefs.put(serverParams[i], Double.valueOf(serverParams[++i]));
		}
		prefs.put("LOCAL", "true");
		return prefs;
	}

	private static String[] serverParams = { //
			"supercellxmin", "0.0", //
			"supercellymin", "0.0", //
			"supercellzmin", "0.0", //
			"supercellxmax", "1.0", //
			"supercellymax", "1.0", //
			"supercellzmax", "1.0", //
			"modeamplitude", "1.0", //
			"strainamplitude", "0.1", //
	};

	/**
	 * Map could be a full page formData map, or it could be just the preferences
	 * map.
	 * 
	 * @param prefs
	 * @param values
	 */
	public boolean setPreferences(Map<String, Object> prefs, Map<String, Object> values) {
		boolean changed = false;
		try {
			double r = atomMaxRadius, l = halfMaxBondLength;
			boolean isLocal = (prefs.remove("LOCAL") != null);
			Object o = values.remove("atomicradius");
			if (o != null) {
				r = (o instanceof Double ? ((Double) o).doubleValue() : Double.parseDouble(o.toString()));
				changed = (r != atomMaxRadius);
				atomMaxRadius = r;
				prefs.put("atomicradius", o);
			}
			o = values.remove("bondlength");
			if (o != null) {
				l = (o instanceof Double ? ((Double) o).doubleValue() : Double.parseDouble(o.toString())) / 2;
				changed |= (l != halfMaxBondLength);
				halfMaxBondLength = l;
				prefs.put("bondlength", o);
			}
			if (changed) {
				isChanged = true;
				changed = false;
			}
			for (int i = 0; i < serverParams.length; i += 2) {
				String key = serverParams[i];
				Object v = values.get(key);
				Object vp = prefs.get(key);
				if (vp instanceof String)
					vp = Double.valueOf((String) vp);
				if (v != null && !v.equals(vp)) {
					prefs.put(key, v);
					changed = true;
				}
			}
			if (isChanged && (isLocal || !changed))
				app.updateDisplay();
			return changed && !isLocal;
		} catch (Exception e) {
			return false;
		}
	}

	public void setCellInfo() {
		setCellInfo(parentCell);
		setCellInfo(childCell);
	}

	private void setCellInfo(Cell cell) {
		for (int pt = 0, i = 0; i < 12; i++) {
			setCylinderInfo(cell.cartesianVertices[Cell.buildCell[pt++]], cell.cartesianVertices[Cell.buildCell[pt++]],
					cell.getCellInfo(i), -1);
		}
	}

	public void setAxisExtents(int axis, Cell cell, double d1, double d2) {
		double[] axesInfo = cell.getAxisExtents(axis, cartesianCenter, d1 * atomMaxRadius, tA, d2 * atomMaxRadius, tB,
				tempvec);
		setCylinderInfo(tA, tB, axesInfo, -1);
	}

	/////////////// INNER CLASSES ////////////

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
		final double[][] vectorBest = new double[MODE_COUNT][];

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
		 * info holds a vector of information intrinsic to each mode transformed into
		 * Cartesian coordinates.
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
			vectorBest[DIS] = coord;
		}

		public double[] getFinalFractionalCoord() {
			return vector1[DIS];
		}

		public double[] get(int mode) {
			return vector1[mode];
		}

		public double getInitialOccupancy() {
			return vectorBest[OCC][0];
		}

		public double getOccupancy() {
			return vector1[OCC][0];
		}

		public double[] getMagneticMoment() {
			return get(MAG);
		}

		/**
		 * Get the transformed Cartesian coordinate. The return value includes a fourth
		 * index value that is used after binary partition tree sorting to return the
		 * atom value.
		 * 
		 * Care should be taken to not transform this value with a Matrix object (which
		 * is 4x4)
		 * 
		 * @return the FOUR-vector [cx, cy, cz, index]
		 */
		public double[] getCartesianCoord() {
			return info[DIS];
		}

		public String getAtomTypeSymbol() {
			return sym;
		}

		/**
		 * Set info[DIS] as [cartX, cartY, cartZ, index]
		 * 
		 * @param a
		 * @param t
		 * @return [cartX, cartY, cartZ, index]
		 */
		private double[] setDisplacementInfo(Cell child, double[] center) {
			double[] t3 = child.toTempCartesian(vector1[DIS]);
			MathUtil.scaleAdd3(t3, -1, center, info[DIS]);
			return info[DIS];
		}

		/**
		 * Calculates the description used to render a ROT or MAG arrow -- Branton
		 * Campbell
		 * 
		 * sets info[type] as [X-angle, Y-angle, Length]
		 * 
		 * @param type  is MAG or ROT
		 * @param child is the child cell
		 */
		public void setArrowInfo(int type, Cell child) {
			double[] info = this.info[type];
			double[] t = child.toTempCartesian(vector1[type]);
			double lensq = MathUtil.lenSq3(t);
			if (lensq < 0.000000000001) {
				info[0] = info[1] = info[2] = 0;
				return;
			}
			double d = Math.sqrt(lensq);
			info[0] = -Math.asin(t[1] / d); // X rotation
			info[1] = Math.atan2(t[0], t[2]); // Y rotation
			info[2] = d; // Length
		}

		/**
		 * Calculates the description used to render an ellipsoid -- Branton Campbell
		 * 
		 * TODO: anisotropic change
		 * 
		 * Currently only for isotropic ellipsoids.
		 * 
		 * sets info[ELL] as [widthX, widthY, widthZ, rotaxisX, rotaxisY, rotaxisZ,
		 * angle]
		 * 
		 * @param child is the child cell.
		 */
		public void setEllipsoidInfo(Cell child) {
			double[] info = this.info[ELL];

			double[][] mat = child.getTempStrainedCartesianBasis(get(ELL));

			// TODO -- Branton -- apply strain to anisotropic ellipsoid

//			det = matdeterminant(tempmat2);
//			for (int i = 0; i < 3; i++)
//				for (int j = 0; j < 3; j++)
//					lensq += tempmat2[i][j]* tempmat2[i][j];
			//
//			if ((Math.sqrt(lensq) < 0.000001) || (det < 0.000001) || true) // "true" temporarily bypasses the ellipoidal
			// analysis.
//			{
			double trace = MathUtil.mat3trace(mat);
			double avgrad = Math.sqrt(Math.abs(trace) / 3.0);
			double widths[] = new double[] { avgrad, avgrad, avgrad };
			double rotangle = 0;
			double rotaxis[] = new double[] { 0, 0, 1 };
			rotangle = 0;
//			} else {
//				Matrix jamat = new Matrix(tempmat2);
//				EigenvalueDecomposition E = new EigenvalueDecomposition(jamat, true);
//				Matrix D = E.getD();
//				Matrix V = E.getV();
//				NV = V.getArray();
//				ND = D.getArray();
			//
//				widths[0] = Math.sqrt((ND[0][0]));
//				widths[1] = Math.sqrt((ND[1][1]));
//				widths[2] = Math.sqrt((ND[2][2]));
//				rotangle = Math.acos(.5 * (NV[0][0] + NV[1][1] + NV[2][2] - 1));
//				rotaxis[0] = (NV[2][1] - NV[1][2]) / (2 * Math.sin(rotangle));
//				rotaxis[1] = (NV[0][2] - NV[2][0]) / (2 * Math.sin(rotangle));
//				rotaxis[2] = (NV[1][0] - NV[0][1]) / (2 * Math.sin(rotangle));
//			}
//		System.out.println(ND[0][0]+" "+ND[0][1]+" "+ND[0][2]+" "+ND[1][0]+" "+ND[1][1]+" "+ND[1][2]+" "+ND[2][0]+" "+ND[2][1]+" "+ND[2][2]);
//		System.out.println(NV[0][0]+" "+NV[0][1]+" "+NV[0][2]+" "+NV[1][0]+" "+NV[1][1]+" "+NV[1][2]+" "+NV[2][0]+" "+NV[2][1]+" "+NV[2][2]);
//		System.out.println("lensq="+lensq+", det="+det+", w0="+widths[0]+", w1="+widths[1]+", w2="+widths[2]+", r0="+rotaxis[0]+", r1="+rotaxis[1]+", r2="+rotaxis[2]);

			info[0] = widths[0];
			info[1] = widths[1];
			info[2] = widths[2];
			info[3] = rotaxis[0];
			info[4] = rotaxis[1];
			info[5] = rotaxis[2];
			info[6] = rotangle % (2 * Math.PI) - Math.PI;
		}

		@Override
		public String toString() {
			return "[Atom " + index + " " + type + "," + subType + "," + subTypeIndex + "]";
		}

	} // end of Atom

	public static class Cell {

		private final static int[] buildCell = new int[] { 0, 1, //
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

		/**
		 * Array of Cartesian vertices of strained window-centered cell. [edge
		 * number][x, y, z]
		 * 
		 */
		double[][] cartesianVertices = new double[8][3];

		/**
		 * Strained cell origin relative to strained child cell origin in cartesian
		 * Angstrom coords. [x, y, z]; will be (0,0,0) for child
		 * 
		 */
		private double[] originCart = new double[3];

		/**
		 * Matrix of unstrained basis vectors in cartesian Angstrom coords [basis
		 * vector][x,y,z] Transforms unitless unstrained direct-lattice child cell coords
		 * into cartesian Angstrom coords [basis vector][x,y,z]
		 * 
		 * set by parser
		 * 
		 */
		private double[][] basisCart0 = new double[3][3];

		/**
		 * Matrix of strained basis vectors in cartesian Angstrom coords [basis
		 * vector][x,y,z] Transforms unitless strained direct-lattice child cell coords
		 * into cartesian Angstrom coords [basis vector][x,y,z]
		 * 
		 */
		private double[][] basisCart = new double[3][3];

		/**
		 * InverseTranspose of Tmat:
		 * 
		 * parentCell.basisCart * Tmat^t*i = childCell.basisCart
		 * 
		 * only for the parent; based on the parsed value of Tmat ("parentbasis")
		 * 
		 */
		private double[][] TmatInverseTranspose;

		/**
		 * Inverse of basisCart in Inverse-Angstrom units [basis vector][x,y,z]
		 */
		private double[][] basisCartInverse = new double[3][3];

		// temporary vector and matrices

		private double[] t3 = new double[3];

		private double[][] t = new double[3][3], t2 = new double[3][3], t4 = new double[3][3];

		/**
		 * Final information needed to render the cell (last 12). [edge number][x, y, z,
		 * x-angle orientation, y-angle orientation, length]
		 * 
		 */
		private double[][] cellInfo = new double[12][6];

		/**
		 * Final information needed to render unit cell axes [axis number][x, y, z,
		 * x-angle , y-angle, length]
		 * 
		 */
		private double[][] axesInfo = new double[3][6];

		private boolean isChild;;

		Cell(boolean isChild) {
			this.isChild = isChild;
			if (!isChild) {
				// parent only
				TmatInverseTranspose = new double[3][3];
			}
		}

		/**
		 * Convert the fractional coords to cartesian, placing the result in a temporary
		 * variable.
		 * 
		 * @param xyz fractional coordinates, unchanged
		 * @return TEMPORARY [x,y,z]
		 */
		public double[] toTempCartesian(double[] fxyz) {
			MathUtil.mat3mul(basisCart, fxyz, t3);
			return t3;
		}

		/**
		 * Calculate the strained or unstrained metric tensor along with the
		 * lattice-to-cartesian matrix.
		 * 
		 * @param isStrained
		 * @param metric
		 * @param tempmat
		 * @param slatt2cart
		 */

		public void setMetricTensor(boolean isStrained, double[][] slatt2cart, double[][] metric) {
			double[][] cart = (isStrained ? basisCart : basisCart0);
			// Determine the unstrained metric tensor
			MathUtil.mat3inverse(cart, t, t3, t2);
			MathUtil.mat3transpose(t, slatt2cart);// B* = Transpose(Inverse(B))
			MathUtil.mat3product(t, slatt2cart, metric, t4); // G* = Transpose(B*).(B*)
		}

		public double addRange2(double d2) {
			for (int i = 8; --i >= 0;) {
				d2 = MathUtil.maxlen2(cartesianVertices[i], d2);
			}
			return d2;
		}

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

		public double[][] getTempTransposedCartesianBasis() {
			MathUtil.mat3transpose(basisCart, t);
			return t;
		}

		public double[][] getTempTransposedReciprocalBasis() {
			MathUtil.mat3inverse(basisCart, t, t3, t2);
			MathUtil.mat3transpose(t, t2);
			return t2;
		}

		public double[] getCellInfo(int i) {
			return cellInfo[i];
		}

		public double[] getAxisInfo(int i) {
			return axesInfo[i];
		}

		private final static int A = 0, B = 1, C = 2, ALPHA = 3, BETA = 4, GAMMA = 5;

		void setStrainedCartesianOrigin(double[] o) {
			MathUtil.set3(originCart, o);
		}

		void setStrainedCartesianBasis(double[][] strain) {
			MathUtil.mat3product(strain, basisCart0, basisCart, t);
		}

		/**
		 * returns a TEMPORARY matrix
		 * 
		 * @param info
		 * @return
		 */
		double[][] getTempStrainedCartesianBasis(double[] info) {
			MathUtil.voigt2matrix(info, t, 0);
			MathUtil.mat3product(basisCart, t, t2, t4);
			MathUtil.mat3copy(t2, t);
			MathUtil.mat3product(t, basisCartInverse, t2, t4);
			return t2;
		}

		void setUnstrainedCartsianBasis(boolean isRhomb, double[][] Tmat) {
			// parent only
			MathUtil.mat3transpose(Tmat, t);
			MathUtil.mat3inverse(t, TmatInverseTranspose, t3, t2);
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

		void transformParentToChild(Cell child, boolean isStrained) {
			double[][] cart = (isStrained ? basisCart : basisCart0);
			double[][] dest = (isStrained ? child.basisCart : child.basisCart0);
			MathUtil.mat3product(cart, TmatInverseTranspose, dest, t4);
			if (isStrained)
				MathUtil.mat3inverse(child.basisCart, child.basisCartInverse, t3, t2);
		}

		void setStrainedVertices() {
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
		 */
		void setRelativeTo(double[] c) {
			for (int j = 0; j < 8; j++) {
				double[] v = cartesianVertices[j];
				MathUtil.scaleAdd3(v, -1, c, v);
			}
		}

		void rangeCheckVertices(double[][] minmax) {
			for (int i = 8; --i >= 0;) {
				MathUtil.rangeCheck(cartesianVertices[i], minmax);
			}
		}

		void setLatticeParameterLabels() {
			double[][] t = new double[3][3];
			double[] v = new double[6];
			MathUtil.mat3transpose(basisCart, t);
			v[A] = Math.sqrt(MathUtil.dot3(t[A], t[A]));
			v[B] = Math.sqrt(MathUtil.dot3(t[B], t[B]));
			v[C] = Math.sqrt(MathUtil.dot3(t[C], t[C]));
			v[ALPHA] = Math.acos(MathUtil.dot3(t[B], t[C]) / Math.max(v[B] * v[C], 0.001));
			v[BETA] = Math.acos(MathUtil.dot3(t[A], t[C]) / Math.max(v[A] * v[C], 0.001));
			v[GAMMA] = Math.acos(MathUtil.dot3(t[A], t[B]) / Math.max(v[A] * v[B], 0.001));
			for (int n = 0; n < 3; n++) {
				labels[n].setText(MathUtil.varToString(v[n], 2, -5));
				labels[n + 3].setText(MathUtil.varToString((180 / Math.PI) * v[n + 3], 2, -5));
			}
		}

		/**
		 * Measure distance from center for the axes of this cell.
		 * 
		 * @param axis
		 * @param center
		 * @param tempvec
		 * @param tB
		 * @param d2
		 * @param tA
		 * @param d1
		 */
		double[] getAxisExtents(int axis, double[] center, double d1, double[] tA, double d2, double[] tB,
				double[] tempvec) {
			for (int i = 0; i < 3; i++) {
				tempvec[i] = originCart[i] + (t3[i] = basisCart[i][axis]) - center[i];
			}
			MathUtil.norm3(t3);
			MathUtil.vecaddN(tempvec, d1, t3, tA);
			MathUtil.vecaddN(tempvec, d2, t3, tB);
			return axesInfo[axis];
		}

		@Override
		public String toString() {
			return "[" + (isChild ? "childCell" : "parentCell") + "]";
		}

	} // end of Cell

	/**
	 * The IsoParser class is an inner class of Variables that loads Variable and
	 * Mode data from an ISOVIZ file. It passes a byte array to IsoTokenizer.
	 * IsoTokenizer recognizes all data blocks; IsoParser then directs IsoTokenizer
	 * to convert (ASCII) byte sequences to text and numbers as needed.
	 * 
	 * @author Bob Hanson
	 *
	 */
	class IsoParser {

		private IsoTokenizer vt;

//		/**
//		 * Used only for connecting bonds with atoms
//		 */
//		private Map<String, Atom> atomMap = new HashMap<>();
//
//		/**
//		 * from parseAtoms()
//		 * 
//		 * @param t
//		 * @param s
//		 * @param a
//		 * @return "t_s_a" key
//		 */
//		private String getKeyTSA(int t, int s, int a) {
//			return t + "_" + s + "_" + a;
//		}
//
//		/**
//		 * from parseBonds()
//		 * 
//		 * @param t
//		 * @param s
//		 * @param a
//		 * @return "t_s_a" key
//		 */
//		private String getKeyTSA(String t, String s, String a) {
//			return t + "_" + s + "_" + a;
//		}
//
//		private int getAtomTSA(String tsa) {
//			Atom a = atomMap.get(tsa);
//			return (a == null ? -1 : a.index);
//		}

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
		 * @return true if successful
		 * 
		 */
		boolean parse(IsoTokenizer vt) throws RuntimeException {
			this.vt = vt;
			modDim = getOneInt("numberOfModulations", 0);
			app.isIncommensurate = Boolean.valueOf(modDim > 0);
			if (isDiffraction && modDim > 0)
				parseError("Incommensurately modulated structures cannot be analyzed using ISODIFFRACT", 2);
			try {
				isoversion = getOneString("isoversion", null);
				parseAppletSettings();
				parseCrystalSettings();

				parseAtoms();
				parseBonds();
				System.out.println("Variables: " + numAtoms + " atoms were read");

				parseStrainModes();
				parseIrrepList();

			} catch (Throwable t) {
				t.printStackTrace();
				parseError("Java error", 2);
				return false;
			}
			vt.dispose();
			this.vt = null;
			return true;
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
			for (int pt = 0, ia = 0, iread = 0, n = numAtomsRead; iread < n; iread++, pt += ncol) {
				if (bsPrimitive != null && !bsPrimitive.get(iread))
					continue;
				double[] data = atoms[ia++].vectorBest[mode] = new double[ncol];
				for (int i = 0; i < ncol; i++) {
					data[i] = (isDefault ? def : vt.getDouble(pt + i));
				}
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
		void getAtomsOccMag10Line(int mode, int nAtomsRead, BitSet bsPrimitive) {
			int ncol = modes[mode].columnCount;
			int offset = (mode == OCC ? 6 : 7);
			for (int pt = 0, ia = 0, iread = 0; iread < nAtomsRead; iread++, pt += 10) {
				if (bsPrimitive != null && !bsPrimitive.get(iread))
					continue;
				double[] data = atoms[ia++].vectorBest[mode] = new double[ncol];
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
		 */
		private void getDoubleArray(String key, double[] a, int pt, int n) {
			if (key != null) {
				int nData = vt.setData(key);
				if (nData != n)
					parseError(nData, n);
			}
			for (int i = 0; i < n; i++)
				a[i] = getDouble(pt++);
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
		private int getOneInt(String key, int def) {
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
		private boolean getOneBoolean(String key, Boolean def) {
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
			getDoubleArray("parentcell", parentCell.latt0, 0, 6);
			getDoubleArray("parentorigin", parentCell.originUnitless, 0, 3);
			boolean isRhombParentSetting = getOneBoolean("rhombparentsetting", false);
			checkSize("parentbasis", 9);
			for (int pt = 0, j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++, pt++) {
					Tmat[j][i] = vt.getDouble(pt);
				}
			}
			parentCell.setUnstrainedCartsianBasis(isRhombParentSetting, Tmat);
			parentCell.transformParentToChild(childCell, false);
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

				Variables.this.bsPrimitive = vt.getBitSet("atomsinunitcell");
				BitSet bsPrimitive = (isDiffraction ? Variables.this.bsPrimitive : null);
				numAtoms = (bsPrimitive == null ? 0 : bsPrimitive.cardinality());

			// Get all the atom type information and return the number of subtype atoms for
			// each type.
			// Just for reading the file.

			int[][] numSubTypeAtomsRead = parseAtomTypes();

			// the number of primitive atoms,
			// if this is for IsoDifrract; in the end, we
			// will replace numSubAtoms with numSubPrimitiveAtoms
			int[][] numPrimitiveSubAtoms = null;

			if (numAtoms > 0) {
				numPrimitiveSubAtoms = new int[numTypes][];
				for (int i = 0; i < numTypes; i++) {
					numPrimitiveSubAtoms[i] = new int[numSubTypeAtomsRead[i].length];
				}
			}

			//boolean haveBonds = (vt.setData("bondlist") > 0);

			// find atomic coordinates of parent
			int nData = vt.setData("atomcoordlist");
			if (nData == 0) {
				parseError("atomcoordlist is missing", 3);
			}

			// number of atoms in the file, before filtering for primitives
			int numAtomsRead = getNumberOfAtomsRead(nData);
			int ncol = nData / numAtomsRead;

			// numAtoms may be the number of primitive atoms only
			if (numAtoms == 0)
				numAtoms = numAtomsRead;
			atoms = new Atom[numAtoms];

			// Find number of subatoms for each subtype
			// Set up numSubAtom (and numPrimitiveSubAtoms if this is for IsoDiffract)

			// BH 2023.12
			// firstAtomOfType is an array containing pointers in the overall list of atoms
			// read (element 0) and the filtered primitive list (element 1)
			// by type. firstAtomOfType[0] is always [0, 0], and we add an addition element
			// at the end that is [numAtomsRead, numAtoms]. These are useful in the mode listings,
			// where we need to catalog atom mode vectors from lists involving type indices only
			int[][] firstAtomOfType = new int[numTypes + 1][2];
			firstAtomOfType[0] = new int[2];
			firstAtomOfType[numTypes] = new int[] { numAtomsRead, numAtoms };

			readAtomCoordinates(ncol, numAtomsRead, numSubTypeAtomsRead, bsPrimitive, numPrimitiveSubAtoms,
					firstAtomOfType);

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
				getAtomsOccMag10Line(OCC, numAtomsRead, bsPrimitive);
				getAtomsOccMag10Line(MAG, numAtomsRead, bsPrimitive);
			} else {
				getAtomTSAn("atomocclist", OCC, 1.0, numAtomsRead, bsPrimitive);
				getAtomTSAn("atommaglist", MAG, 0.0, numAtomsRead, bsPrimitive);
			}
			getAtomTSAn("atomrotlist", ROT, 0.0, numAtomsRead, bsPrimitive);
			if (!getAtomTSAn("atomelplist", ELL, Double.NaN, numAtomsRead, bsPrimitive)) {
				// default to [0.04 0.04 0.04 0 0 0]
				double[] def = new double[] { defaultUiso, defaultUiso, defaultUiso, 0, 0, 0 };
				for (int ia = 0; ia < atoms.length; ia++)
					atoms[ia].vectorBest[ELL] = def;
			}

			if (bsPrimitive != null)
				System.out.println("Variables: primitive: " + bsPrimitive);

			// now get all the symmetry-related arrays
			parseAtomicMode(DIS, "displacivemodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(OCC, "scalarmodelist", 1, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(MAG, "magneticmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(ROT, "rotmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);
			parseAtomicMode(ELL, "ellipmodelist", 3, firstAtomOfType, numSubTypeAtomsRead, bsPrimitive);

		}

		private void readAtomCoordinates(int ncol, int numAtomsRead, int[][] numSubTypeAtomsRead, BitSet bsPrimitive,
				int[][] numPrimitiveSubAtoms, int[][] firstAtomOfType) {
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
					atoms[ia] = new Atom(ia, t, s, a, coord, atomTypeSymbol[t]);
//					if (haveBonds)
//						atomMap.put(getKeyTSA(t + 1, s + 1, a + 1), atom);
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
				// no reason to believe this would be XX or xx, but 
				// this guarantees Xx for Elements.getScatteringFactor. 
				String s = vt.getString(3 * i + 2);
				atomTypeSymbol[i] = s.substring(0, 1).toUpperCase() + (s.length() == 1 ? "" : s.substring(1,2).toLowerCase());
			}

			numSubTypes = new int[n];
			// start this off with one implicit subtype.
			int[][] numSubTypeAtomsRead = new int[n][1];

			// find atom subtypes (optional)
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
		 * Just get max/min information
		 */
		private void parseBonds() {
			if (isDiffraction)
				return;
			double d = getOneDouble("maxbondlength", 2.5);
			// figure 1 minimum for a real number, not one that was adjusted
			if (d < 0.5)
				d *= 10;
			halfMaxBondLength = d / 2;
			// find minimum atomic occupancy for which bonds should be displayed
			minBondOcc = getOneDouble("minbondocc", 0.5);
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

//      !scalarmodelist  			
//		    1    1   0.00030   2.82843    3 GM1+[Sr:b:occ]A1g(a) 
//		    1    2   0.00021   2.00000    8 M4+[Sr:b:occ]A1g(a) 
//		    1    3   0.00022   2.82843    8 M4+[Sr:b:occ]A1g(b) 
//		    2    1   0.00040   2.82843    3 GM1+[Ti:a:occ]A1g(a) 
//		    2    2   0.00017   2.82843    7 M1+[Ti:a:occ]A1g(a) 
//		    3    1   0.00050   4.89898    3 GM1+[O:d:occ]A1g(a) 
//		    3    2   0.00070   3.46410    4 GM3+[O:d:occ]A1g(a) 
//		    3    3   0.00018   2.82843    7 M1+[O:d:occ]A1g(a) 

			// will result in [3, 2, 3]

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
		 * @param isoParser
		 * @param bsPrimitive
		 * @param key
		 * 
		 * 
		 * @param parser
		 * 
		 */
		private void getAtomModeData(Mode mode, int[][] firstAtomOfType, int[][] numSubTypeAtomsRead,
				BitSet bsPrimitive) {

//        t    m   calcAmp   maxAmp  irrep  name
//		    1    1   0.00030   2.82843    3 GM1+[Sr:b:occ]A1g(a) 

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
				mode.calcAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.maxAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.irrepTM[atomType][mt] = vt.getInt(pt++) - 1;
				mode.setModeName(atomType, mt, vt.getString(pt++));
				
				for (int s = 0; s < numSubTypes[atomType]; s++) {
					for (int a = 0; a < numSubTypeAtomsRead[atomType][s]; a++, iread++, pt += ncol) {
						if (bsPrimitive != null && !bsPrimitive.get(iread))
							continue;
						double[] array = atoms[ia++].modes[type][mt] = new double[ncol];
						getDoubleArray(null, array, pt, ncol);
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
				modes[STRAIN].setModeName(0, m, vt.getString(ncol * m + 4));
				modes[STRAIN].irrepTM[0][m] = vt.getInt(ncol * m + 3) - 1;
				modes[STRAIN].calcAmpTM[0][m] = vt.getDouble(ncol * m + 1);
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
				modes[IRREP].setModeName(0, i, vt.getString(2 * i + 1));
			}
		}

	} // end of IsoParser

	static final Color COLOR_CHILD_CELL = new Color(128, 128, 204);
	static final Color COLOR_PARENT_CELL = new Color(204, 128, 128);

	/**
	 * The SliderPanelGUI inner class of Variables creates all GUI elements for
	 * IsoApp.sliderPanel and handles all synchronization of JSlider and JCheckbox
	 * components with Variables and Mode values.
	 * 
	 * @author Bob Hanson
	 *
	 */
	class SliderPanelGUI {

		private final static int subTypeWidth = 210;
		private final static int barheight = 22;

		/**
		 * Master (top most) slider bar controls all slider bars for superpositioning of
		 * modes.
		 * 
		 */
		private JSlider mainSlider;
		/**
		 * Array of strain mode slider bars.
		 */
		private JLabel mainSliderLabel;

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
		private JCheckBox[][] subTypeCheckBoxes;

		/**
		 * mode sliders and their labels and pointers
		 */
		IsoSlider[][][] sliderTM = new IsoSlider[MODE_COUNT][][];

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

		private int sliderWidth;
		private int sliderPanelWidth;

		/**
		 * Total number of unique parent atom types.
		 */
		int numUniques;

		/**
		 * Integer index that identifies each parent atom type as one of several unique
		 * parent atom types.
		 * 
		 */
		int[] atomTypeUnique;

		/**
		 * current static of (no) shading in slider panel
		 */
		public boolean simpleColor;

		void updateColorScheme(boolean isSimple) {
			simpleColor = isSimple;
			app.colorBox.setEnabled(false);
			app.colorBox.setSelected(isSimple);
			app.colorBox.setEnabled(true);
			setColors(isSimple);
			for (int t = 0; t < numTypes; t++) {
				Color c = getDefaultModeColor(t);
				typeNamePanels[t].setBackground(c);
				typeNamePanels[t].getParent().setBackground(c);
				for (int s = 0; s < numSubTypes[t]; s++) {
					Component p;
					while ((p  = subTypeLabels[t][s].getParent()) != null && !p.isOpaque())
					{}
					p.setBackground(c);
				}
				typeDataPanels[t].setBackground(c);
				for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
					if (modes[i].isActive())
						typePanels[i][t].setBackground(modes[i].colorT[t]);
				}
			}
		}

		void setColors(boolean simpleColor) {
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i])) {
					modes[i].setColors(simpleColor ? atomTypeUnique : null, numUniques);
				}
			}
		}

		void toggleIrrepSliders() {
			irrepSlidersOn = !irrepSlidersOn;
			int val = (irrepSlidersOn ? sliderMax : 0);
			modes[IRREP].setSliders(sliderTM[IRREP], val);
		}

		void zeroSliders() {
			mainSlider.setValue(sliderMax);
			for (int i = 0; i < MODE_COUNT; i++) {
				modes[i].setSliders(sliderTM[i], 0);
			}
		}

		void resetSliders() {
			mainSlider.setValue(sliderMax);
			for (int i = 0; i < MODE_COUNT; i++) {
				modes[i].resetSliders(sliderTM[i], sliderMax);
			}
		}

		void setComponentValuesFrom(Variables v) {

// BH letting the two apps be independent wrt checkboxes in sliderPanel
//
//			for (int t = subTypeBoxes.length; --t >= 0;) {
//				JCheckBox[] boxes = subTypeBoxes[t];
//				for (int s = boxes.length; --s >= 0;) {
//					boxes[s].setSelected(v.gui.subTypeBoxes[t][s].isSelected());
//				}
//			}

			mainSlider.setValue(v.gui.mainSlider.getValue());
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i])) {
					int n = (i >= MODE_ATOMIC_COUNT ? 1 : numTypes);
					for (int t = 0; t < n; t++) {
						for (int m = 0; m < v.modes[i].modesPerType[t]; m++) {
							sliderTM[i][t][m].setValue((int) v.gui.sliderTM[i][t][m].getValue());
						}
					}
				}
			}
		}

		/**
		 * Copy slider settings to fields and set the slider values text.
		 * 
		 * 
		 */
		void readSliders() {
			double f0 = childFraction;
			childFraction = mainSlider.getValue() / maxJSliderIntVal;
			if (childFraction != f0) {
				app.recalcCellColors();
			}
			String main = MathUtil.varToString(childFraction, 2, -6) + " child";
			mainSliderLabel.setText(main);
			for (int i = MODE_COUNT; --i >= 0;) {
				if (isModeActive(modes[i]))
					modes[i].readSliders(sliderTM[i], childFraction, maxJSliderIntVal, modes[IRREP]);
			}
		}

		void initPanels(JPanel sliderPanel, int width) {

			// this.sliderPanel = sliderPanel;

			// Divide the applet area with structure on the left and controls on the right.

			sliderPanelWidth = width;
			sliderWidth = sliderPanelWidth / 2;

			/**
			 * Maximum number of check boxes per row the GUI will hold
			 */
			int maxSubTypesPerRow = width / subTypeWidth;

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
			masterSliderPanel.setBackground(Color.WHITE);

			// Master Slider Panel
			mainSliderLabel = new JLabel("child");
			mainSliderLabel.setForeground(COLOR_CHILD_CELL);
			mainSliderLabel.setHorizontalAlignment(JLabel.CENTER);
			mainSliderLabel.setVerticalAlignment(JLabel.CENTER);
			mainSlider = new IsoSlider(-1, "child", 0, 1, 1, Color.WHITE);

			masterSliderPanel.add(Box.createHorizontalGlue());
			JLabel pl = new JLabel("parent ");
			pl.setForeground(COLOR_PARENT_CELL);
			masterSliderPanel.add(pl);
			masterSliderPanel.add(mainSlider);
			masterSliderPanel.add(Box.createRigidArea(new Dimension(1, 1)));
			masterSliderPanel.add(mainSliderLabel);
			masterSliderPanel.add(Box.createHorizontalGlue());
			addPanel(sliderPanel, masterSliderPanel, "masterPanel");

			// Initialize type-specific subpanels of scrollPanel
			typeLabel = new JLabel[numTypes];
			typeNamePanels = new JPanel[numTypes];
			typeDataPanels = new JPanel[numTypes];
			subTypeCheckBoxes = new JCheckBox[numTypes][];
			subTypeLabels = new JLabel[numTypes][];

			// The big loop over types
			for (int t = 0; t < numTypes; t++) {
				Color c = getDefaultModeColor(t);
				JPanel tp = new JPanel();
				tp.setName("typeOuterPanel_" + t);
				tp.setLayout(new BoxLayout(tp, BoxLayout.Y_AXIS));
				tp.setBorder(new EmptyBorder(2, 2, 5, 2));
				tp.setBackground(c); // important
				subTypeCheckBoxes[t] = new JCheckBox[numSubTypes[t]];
				subTypeLabels[t] = new JLabel[numSubTypes[t]];
				typeLabel[t] = newWhiteLabel("" + atomTypeName[t] + " Modes", JLabel.CENTER);
				typeNamePanels[t] = new JPanel(new GridLayout(1, 1, 0, 0));
				typeNamePanels[t].setName("typeNamePanel_" + t + " " + atomTypeName[t]);
				typeNamePanels[t].add(typeLabel[t]);
				typeNamePanels[t].setBackground(c); // important
				tp.add(typeNamePanels[t]);

				// typeDataPanel
				typeDataPanels[t] = new JPanel(new GridLayout(numSubRows[t], subTypesPerRow[t], 0, 0));
				typeDataPanels[t].setName("typeDataPanel_" + t);
				int nst = numSubTypes[t];
				for (int s = 0; s < nst; s++) {
					JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.setName("subtypePanel_" + s);
					p.setBackground(c);
					JCheckBox b = subTypeCheckBoxes[t][s] = newSubtypeCheckbox("subType_" + t + "_" + s, c);
					// BH testing removal b.setEnabled(!isDiffraction);
					subTypeLabels[t][s] = newWhiteLabel("", JLabel.LEFT);
					p.add(b);
					p.add(subTypeLabels[t][s]);
					typeDataPanels[t].add(p);
				}
				// Makes sure that each subtype row is full for alignment purposes
				for (int s = nst; s < numSubRows[t] * subTypesPerRow[t]; s++) {
					JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.setName("subtypeFillerPanel_" + s);
					p.setBackground(c);
					// p.add(new JLabel(""));
					typeDataPanels[t].add(p);
				}
				tp.add(typeDataPanels[t]);
				addPanel(sliderPanel, tp, "typePanel_" + t + " " + typeLabel[t].getText());
				int n = 0;
				for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
					n += initModeGUI(sliderPanel, modes[i], t);
				}
				if (n == 0) {
					tp = new JPanel(new GridLayout(1, 1, 0, 0));
					tp.setBackground(c);
					addPanel(sliderPanel, tp, "typeFiller_" + t);
				}
			}
			Color c = Mode.COLOR_STRAIN;
			JPanel strainTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
			strainTitlePanel.setBackground(c);
			JLabel strainTitle = newWhiteLabel("Strain Modes", JLabel.CENTER);
			strainTitlePanel.add(strainTitle);
			addPanel(sliderPanel, strainTitlePanel, "strainTitlePanel");

			// strainDataPanel
			initCell(childCell, c);
			initCell(parentCell, c);
			JPanel strainDataPanel = new JPanel(new GridLayout(2, 6, 0, 0));
			strainDataPanel.setBackground(c);
			strainDataPanel.add(parentCell.title);
			for (int n = 0; n < 6; n++)
				strainDataPanel.add(parentCell.labels[n]);
			strainDataPanel.add(childCell.title);
			for (int n = 0; n < 6; n++)
				strainDataPanel.add(childCell.labels[n]);
			addPanel(sliderPanel, strainDataPanel, "strainDataPanel");
			initModeGUI(sliderPanel, modes[STRAIN], 0);

			if (isModeActive(modes[IRREP])) {
				c = modes[IRREP].colorT[0];
				JPanel irrepTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
				irrepTitlePanel.setBackground(c);
				JLabel irrepTitle = newWhiteLabel("Single-Irrep Master Amplitudes", JLabel.CENTER);
				irrepTitlePanel.add(irrepTitle);
				addPanel(sliderPanel, irrepTitlePanel, "irrepTitlePanel");
				initModeGUI(sliderPanel, modes[IRREP], 0);
			}
			// this Glue is nice, because it adds just a bit of
			// vertical padding around the sliders
			sliderPanel.add(Box.createVerticalGlue());
		}

		private void initCell(Cell cell, Color c) {
			for (int n = 0; n < 6; n++) {
				cell.labels[n] = newWhiteLabel("", JLabel.LEFT);
				cell.labels[n] = newWhiteLabel("", JLabel.LEFT);
			}
			cell.title = newWhiteLabel(cell.isChild ? "  Ccell" : "  Pcell", JLabel.LEFT);
			cell.title.setForeground(cell.isChild ? COLOR_CHILD_CELL : COLOR_PARENT_CELL);
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

		private JLabel newWhiteLabel(String text, int hAlign) {
			JLabel l = new JLabel(text);
			l.setForeground(Color.WHITE);
			l.setHorizontalAlignment(hAlign);
			l.setVerticalAlignment(JLabel.TOP);
			return l;
		}

		private JCheckBox newSubtypeCheckbox(String name, Color c) {
			JCheckBox b = new JCheckBox("");
			b.setName(name);
			b.setSelected(true);
			b.setFocusable(false);
			b.setOpaque(false);
//			b.setBackground(c);
			b.addItemListener(app.buttonListener);
			return b;
		}

		final Font pointerFont = new Font(Font.SANS_SERIF, Font.PLAIN, 8);
		private JPanel[][] typePanels = new JPanel[MODE_COUNT][];

		class IsoSlider extends JSlider implements ChangeListener {

			private int min; // 0 or -SliderMax

			private int type;

			private double calcAmp, maxAmp;

			JLabel whitePointer, orangePointer, blackPointer;

			JLabel sliderLabel;

			void setSliderLabel(String text) {
				sliderLabel.setText(text);
			}

			IsoSlider(int type, String name, int min, double calcAmp, double maxAmp, Color c) {
				super(JSlider.HORIZONTAL, min, sliderMax, (int) (calcAmp / maxAmp * sliderMax));
				setName(name);
				// this setting is important; without it, everything looks shrunk
				setPreferredSize(new Dimension(sliderWidth, barheight));
				if (type >= 0 && showSliderPointers) {
					this.min = min;
					this.calcAmp = calcAmp;
					this.maxAmp = maxAmp;
					// Note that JavaDoc with @j2sNative inserts JavaScript.
					boolean isJS = (/** @j2sNative 1 ? true : */
					false);
					// In JavaScript this will be (1 ? true : false) -- evaluating to true,
					// while in Java it will read (false). Cool, huh??
					if (isJS && min != 0) {
						// This check is required because for overlapping components
						// JavaScript paints them first to last CREATED,
						// while Java paints them last to first PAINTED.
						// This is not something that can be fixed in SwingJS.
						blackPointer = getPointer(Color.BLACK);
						orangePointer = getPointer(Color.ORANGE);
						whitePointer = getPointer(Color.WHITE);
					} else {
						whitePointer = getPointer(Color.WHITE);
						if (min != 0) {
							orangePointer = getPointer(Color.ORANGE);
							blackPointer = getPointer(Color.BLACK);
						}
					}
				}
//				if (c != null)
//					setBackground(c);
				setOpaque(false);
				addChangeListener(this);
				setFocusable(false);
			}

			private JLabel getPointer(Color c) {
				JLabel p = new JLabel("\u25B2");
				p.setFont(pointerFont);
				p.setBorder(new EmptyBorder(-2, -2, 0, -2));
				p.setForeground(c);
				add(p);
				return p;
			}

			/**
			 * from Mode.readSliders;
			 * 
			 * @param val
			 * @param max
			 * @param name
			 */
			void setLabelValue(double val, double max, String name) {
				double d = val * childFraction;
				int prec = (type < MODE_ATOMIC_COUNT ? 2 : 3);
				setSliderLabel(MathUtil.varToString(d, prec, -8) + "  " + name);
				if (whitePointer == null) {
					return;
				}
				d /= max;

				// slight adjustments for JavaScript Look and Feel
				// maybe for MacOS as well?
				double w = getWidth() - (/** @j2sNative 1 ? 20 : */
				15);
				int off = (/** @j2sNative 1 ? 8 : */
				6);
				int x = (int) ((d * sliderMax - min) / (sliderMax - min) * w);
				whitePointer.setBounds(x + off, 12, 6, 8);
				if (orangePointer != null) {
					x = (int) (0.5 * w);
					orangePointer.setBounds(x + off, 12, 6, 8);
					d = calcAmp / maxAmp;
					x = (int) ((d * sliderMax - min) / (sliderMax - min) * w);
					blackPointer.setBounds(x + off, 12, 6, 8);
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

		}

		/**
		 * Set up the upper mode-related panels.
		 * 
		 * @param sliderPanel
		 * 
		 * @param mode
		 * @param t
		 * 
		 */
		private int initModeGUI(JPanel sliderPanel, Mode mode, int t) {
			if (!isModeActive(mode))
				return 0;
			int type = mode.type;
			if (t == 0) {
				int numTypes = mode.numTypes;
				typePanels[type] = new JPanel[numTypes];
				sliderTM[type] = new IsoSlider[numTypes][];
				mode.values = new double[numTypes][];
			}
			Color c = (mode.colorT == null ? Color.PINK : mode.colorT[t]);
			int min = (mode.type == IRREP ? 0 : -sliderMax);
			int nModes = mode.modesPerType[t];
			sliderTM[type][t] = new IsoSlider[nModes];
			mode.values[t] = new double[nModes];
			JPanel tp = typePanels[type][t] = new JPanel(new GridLayout(nModes, 2, 0, 0));
			tp.setBackground(c);
			for (int m = 0; m < nModes; m++) {
				sliderTM[type][t][m] = new IsoSlider(mode.type, getInputName(mode.type, t, m), min,
						mode.calcAmpTM[t][m], mode.maxAmpTM[t][m], c);
				sliderTM[type][t][m].sliderLabel = newWhiteLabel("", JLabel.LEFT);
				tp.add(sliderTM[type][t][m]);
				tp.add(sliderTM[type][t][m].sliderLabel);
			}
			addPanel(sliderPanel, tp, "modeSliderPanelType_" + t);
			return nModes;
		}

		int test = 0;
		private void addPanel(JPanel sliderPanel, Component c, String name) {
			c.setName(name); // this is for debugging
			// https://stackoverflow.com/questions/18405660/how-to-set-component-size-inside-container-with-boxlayout
			c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getMinimumSize().height));
			sliderPanel.add(c);			
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
			String text = " " + subTypeName[t][s] + "  [" + MathUtil.varToString(max[DIS], 2, 0) + ", "
					+ MathUtil.varToString(max[OCC], 2, 0) + ", " + MathUtil.varToString(max[MAG], 2, 0) + ", "
					+ MathUtil.varToString(max[ROT], 2, 0) + "]";
			subTypeLabels[t][s].setText(text);
		}

		void keyTyped(KeyEvent e) {
			isAdjusting = true;
			switch (e.getKeyChar()) {
			case 'z':
			case 'Z':
				zeroSliders();
				isChanged = true;
				break;
			case 'i':
			case 'I':
				resetSliders();
				isChanged = true;
				break;
			case 's':
			case 'S':
				toggleIrrepSliders();
				isChanged = true;
				break;
			}
			isAdjusting = false;
			if (isChanged)
				app.updateDisplay();
		}

	}

	/**
	 * Create a relatively dark shade of gray related to the subtype index.
	 * 
	 * @param t
	 * @param s
	 * @return a gray shade between 0.2 and 0.5, roughly
	 */
	public double getSelectedSubTypeShade(int t, int s) {
		return 0.2 + 0.3 * s / numSubTypes[t];
	}

	public void dispose() {
		gui = null;
		app = null;
		atoms = null;
		parentCell = null;
		childCell = null;
		for (int i = 0; i < MODE_COUNT; i++)
			modes[i] = null;
		atomTypeName = null;
		subTypeName = null;
		numSubAtoms = null;
		numSubTypes = null;
	}

	/**
	 * Checks for the presence of primitive atoms and also for 
	 * @param i
	 * @return
	 */
	public boolean isPrimitive(int i) {
		return (i < 0 || bsPrimitive == null ? bsPrimitive != null 
				: bsPrimitive.get(i));
	}

}
