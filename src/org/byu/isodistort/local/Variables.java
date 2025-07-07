package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
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
import org.byu.isodistort.local.Cell.ChildCell;

/**
 * A class to maintain all aspects of variables that must be maintained for both
 * ISODISTORT and ISODIFFRACT.
 * 
 * Contains the following inner classes:
 * 
 * 
 * Atom (inner static)
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

	public final static String parentText = "Zero";

	public final static String childText = "Initial";

	protected static final String mainSliderParentName = "parent"; // "undistorted"
	protected static final String mainSliderChildName = "child"; // "distorted"

	public final static int ATOMS = -1; // for colors
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

	static final Color COLOR_CHILD_CELL = new Color(128, 128, 204);
	static final Color COLOR_PARENT_CELL = new Color(204, 128, 128);

	static final Color COLOR_STRAIN = Color.DARK_GRAY;
	static final Color COLOR_IRREP = new Color(0xA0A0A0);

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

	public IsoAtom[] atoms;

	/**
	 * a bitset used in IsoDiffractApp to filter atoms and atom properties. It is
	 * provides an option to display only primitive atoms in IsoDistortApp.
	 * 
	 */
	BitSet bsPeriodic = null;

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
	public int nAtoms;

	/**
	 * Number of different atom types
	 */
	public int nTypes;

	/**
	 * Number of subtypes for each atom type.
	 */
	public int[] nSubTypes;

	/**
	 * the final public number of atoms for specific type and subtype; either
	 * nSubTypeAtomsRead or nPrimitiveSubAtoms
	 * 
	 */
	int[][] nSubAtoms;

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
	 * maximum and minimum bond lengths in Angstroms beyond which bonds are not
	 * drawn
	 */
	private double maxBondLength, minBondLength;

	public double getMaxBondLength() {
		return maxBondLength;
	}

	public double getMinBondLength() {
		return minBondLength;
	}

	public void setMaxBondLength(double angstroms) {
		maxBondLength = angstroms;
	}

	public void setMinBondLength(double angstroms) {
		minBondLength = angstroms;
	}

	/**
	 * Minimum atomic occupancy below which bonds are not drawn
	 */

	public double minBondOcc;

	/**
	 * cell for the child, which is missing some number of operations relative to
	 * the parent.
	 * 
	 */
	public ChildCell childCell = new ChildCell();

	/**
	 * cell for the parent, which contains more symmetry than the child.
	 */
	public Cell.ParentCell parentCell = new Cell.ParentCell();

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
	private double mainSliderChildFraction;

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
	 * flag to indicate that this is an incommensurately modulated structure; 0
	 * means not incommensurate
	 */
	public int modDim;
	public boolean needColorBox;

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

	public IsoAtom getAtom(int ia) {
		return atoms[ia];
	}

	public double[][] getAtomInfo(int a) {
		return atoms[a].info;
	}

	public void enableSubtypeSelection(boolean tf) {
		for (int t = 0; t < nTypes; t++)
			for (int s = 0; s < nSubTypes[t]; s++)
				gui.subTypeCheckBoxes[t][s].setEnabled(tf);// was false
	}

	public void selectAllSubtypes() {
		for (int t = 0; t < nTypes; t++)
			for (int s = 0; s < nSubTypes[t]; s++)
				gui.subTypeCheckBoxes[t][s].setSelected(true);// was false
	}

	public double getSetChildFraction(double newVal) {
		double d = mainSliderChildFraction;
		if (!Double.isNaN(newVal))
			mainSliderChildFraction = newVal;
		return d;
	}

	public int getStrainModesCount() {
		return (modes[STRAIN] == null ? 0 : modes[STRAIN].count);
	}

	public double[] getStrainmodeSliderValues() {
		return (modes[STRAIN] == null ? new double[0] : modes[STRAIN].valuesTM[0]);
	}

	public void getColors(int t, double[] rgb) {
		Color c = gui.getAtomTypeColor(t);
		rgb[0] = c.getRed() / 255.0;
		rgb[1] = c.getGreen() / 255.0;
		rgb[2] = c.getBlue() / 255.0;
	}

	public void updateColorScheme(boolean byElement) {
		gui.updateColorScheme(byElement);
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
		gui.updateColorScheme(v.gui.colorByElement);
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

		// Calculate strained parent and child unit cell basis vectors in cartesian
		// Angstrom
		// coordinates.

		double[][] pStrainPlusIdentity = (modes[STRAIN] == null
				? MathUtil.voigt2matrix(new double[6], new double[3][3], 1)
				: modes[STRAIN].getVoigtStrainTensor(mainSliderChildFraction, modes[IRREP]));
		double[][] temp = new double[3][3];
		MathUtil.mat3product(pStrainPlusIdentity, parentCell.basisCart0, parentCell.basisCart, temp);
		transformParentToChild(true);
		MathUtil.set3(childCell.toTempCartesian(parentCell.originUnitless), parentCell.originCart);
		parentCell.setVertices();
		parentCell.setLatticeParameterLabels();
		childCell.setVertices();
		childCell.setLatticeParameterLabels();
		recenterLattice();

		// pass an array to the modes that tracks the
		// max values that will be used in the labels
		// just easier to use a single oversize array

		int nSubMax = 0;
		for (int t = 0; t < nTypes; t++) {
			int n = nSubTypes[t];
			if (n > nSubMax)
				nSubMax = n;
		}
		double[][][] max = new double[nTypes][nSubMax][MODE_ATOMIC_COUNT];
		double f = mainSliderChildFraction;
		modes[DIS].calcDistortion(this, max, tempvec, null, f);
		modes[OCC].calcDistortion(this, max, null, null, f);
		modes[MAG].calcDistortion(this, max, tempvec, null, f);
		modes[ROT].calcDistortion(this, max, tempvec, null, f);
		modes[ELL].calcDistortion(this, max, tempvec, tempmat, f);

		for (int t = 0; t < nTypes; t++) {
			for (int s = 0; s < nSubTypes[t]; s++) {
				gui.setSubTypeText(t, s, max[t][s]);
			}
		}

	}

	/**
	 * Right-multiply the child basis with parentCell.conv2convChildTransposeP.
	 * unstrained uses the original bases; strained uses current bases.
	 * 
	 * parent only
	 * 
	 * @param child
	 * @param isStrained
	 */
	private void transformParentToChild(boolean isStrained) {
		double[][] t33 = new double[3][3];
		if (isStrained) {
			double[] t3 = new double[3];
			MathUtil.mat3product(parentCell.basisCart, parentCell.conv2convChildTransposeP, childCell.basisCart, t33);
			MathUtil.mat3inverse(childCell.basisCart, childCell.basisCartInverse, t3, t33);
		} else {
			MathUtil.mat3product(parentCell.basisCart0, parentCell.conv2convChildTransposeP, childCell.basisCart0, t33);
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
		for (int i = nAtoms; --i >= 0;) {
			MathUtil.rangeCheck(childCell.toTempCartesian(atoms[i].vectorBest[DIS]), minmax);
		}
		MathUtil.average3(minmax[0], minmax[1], cartesianCenter);
		childCell.setRelativeTo(cartesianCenter);
		parentCell.setRelativeTo(cartesianCenter);
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

	public void setWebFormData(Map<String, Object> mapFormData, String sliderSetting) {
		gui.replaceData(mapFormData, null, sliderSetting, mainSliderChildFraction);
	}

	public void setIsovizFileData(byte[] isovizData, String sliderSetting) {
		gui.replaceData(null, isovizData, sliderSetting, mainSliderChildFraction);
	}

	public void updateModeFormData(Map<String, Object> mapFormData, Object document) {
		for (int mode = 0; mode < MODE_ATOMIC_COUNT; mode++) {
			if (isModeActive(modes[mode])) {
				double[][] vals = modes[mode].valuesTM;
				for (int t = vals.length; --t >= 0;) {
					for (int m = vals[t].length; --m >= 0;) {
						String name = getInputName(mode, t, m);
						setModeFormValue(name, vals[t][m], mapFormData, document);
					}
				}
			}
		}
		if (isModeActive(modes[STRAIN])) {
			double[] vals = modes[STRAIN].valuesTM[0];
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
			// System.out.println("V.setModeFormVal " + name + "=" + s);
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
		for (int ia = 0, n = nAtoms; ia < n; ia++) {
			IsoAtom a = atoms[ia];
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
		prefs.put("maxbondlength", "" + maxBondLength);
		prefs.put("minbondlength", "" + minBondLength);
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
			double r = atomMaxRadius;
			double lMax = maxBondLength;
			double lMin = minBondLength;
			boolean isLocal = (prefs.remove("LOCAL") != null);
			Object o = values.remove("atomicradius");
			if (o != null) {
				r = (o instanceof Double ? ((Double) o).doubleValue() : Double.parseDouble(o.toString()));
				changed = (r != atomMaxRadius);
				atomMaxRadius = r;
				prefs.put("atomicradius", o);
			}
			o = values.remove("maxbondlength");
			if (o == null)
				o = values.remove("bondlengthmax");
			if (o != null) {
				lMax = (o instanceof Double ? ((Double) o).doubleValue() : Double.parseDouble(o.toString()));
				changed |= (lMax != maxBondLength);
				maxBondLength = lMax;
				prefs.put("maxbondlength", o);
			}
			o = values.remove("minbondlength");
			if (o == null)
				o = values.remove("bondlengthmin");
			if (o != null) {
				lMax = (o instanceof Double ? ((Double) o).doubleValue() : Double.parseDouble(o.toString()));
				changed |= (lMin != minBondLength);
				minBondLength = lMin;
				prefs.put("minbondlength", o);
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
	public static class IsoAtom {

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
		public int subTypeIndex;

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

		public String sym;

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
		private IsoAtom(int index, int t, int s, int a, double[] coord, String elementSymbol) {
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
		private double[] setDisplacementInfo(ChildCell child, double[] center) {
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
		public void setArrowInfo(int type, ChildCell child) {
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
		public void setEllipsoidInfo(ChildCell child) {
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

	public static class SymopData {
		final static double ptol = 0.001;

		/**
		 * the 4x4 matrix representation for this operator; null for a centering
		 * translation
		 */
		protected double[][] op;

		/**
		 * We need
		 * 
		 * r3t = mat3transpose(op)
		 * 
		 * to check that
		 * 
		 * r3t * [h,k,l] == [h,k,l]
		 * 
		 * Note that r3t is unnecessary for a centering translation, where m00 == m11 =
		 * m22 = 1 and all other terms are 0. In that case, [h,k,l] - [h,k,l] ==
		 * [0,0,0].
		 * 
		 * Bob Hanson 2025.07.06
		 * 
		 */
		protected double[][] r3t;

		/**
		 * the intrinsic translation for screw axes and glide planes
		 */
		public double[] vi;

		/**
		 * potentially useful but not implemented Jones-Faithful description of the
		 * operation
		 */
		public String opXYZ;

		public String type;

		protected SymopData(String type, double[] vi) {
			// centering translation
			this.type = type;
			this.vi = vi;
			this.op = new double[][] { 
				new double[] { 1, 0, 0, vi[0] }, 
				new double[] { 0, 1, 0, vi[1] },
				new double[] { 0, 0, 1, vi[2] }, 
				new double[] { 0, 0, 0, 1 } };
			opXYZ = getXYZFromMatrixFrac(op, false, false, true, false);
		}

		protected SymopData(String type, double[][] op, double[] vi) {
			this.type = type;
			this.op = op;
			opXYZ = getXYZFromMatrixFrac(op, false, false, true, false);
			this.vi = vi;
			r3t = new double[3][3];
			MathUtil.mat3transpose(op, r3t);
			System.out.println(type + " adding operator " + this);
		}

		private static double[] v3 = new double[3];

		public boolean isSystematicAbsence(double[] hkl) {
			return (isAbsentVi(hkl) && isAbsentR(hkl));
		}

		/**
		 * Check that vi.dot.hkl is not integral in order to
		 * ensure that K.dot.R = K must hold
		 * 
		 * @param hkl
		 * @return true if vi.dot.hkl is nonintegral
		 */
		private boolean isAbsentVi(double[] hkl) {
			double d = MathUtil.dot3(vi, hkl);
			return !MathUtil.approxEqual(d, Math.rint(d), ptol);
		}

		/**
		 * Check that R_t * K == K
		 * @param hkl
		 * @return
		 */
		private boolean isAbsentR(double[] hkl) {
			if (r3t == null)
				return true;
			MathUtil.mat3mul(r3t, hkl, v3);
			return MathUtil.approxEqual3(v3, hkl, ptol);
		}

		@Override
		public String toString() {
			return type + ":" + (op == null ? "centering " : opXYZ + "\n" + MathUtil.matToString(op) + "\n with vi=") + Arrays.toString(vi);
		}

		final static double[][] t1 = new double[4][4];
		final static double[][] t2 = new double[4][4];
		final static double[][] t3 = new double[4][4];

		/**
		 * given a 4x4 matrix, see if it is a point group operation
		 * 
		 * @param op
		 * @return translation or null
		 */
		protected static double[] getIntrinsicTranslation(double[][] op) {
			// check to see if an operation is a point group operation or not.
			MathUtil.copyNN(op, t1);
			int n = 1;
			while (!MathUtil.mat3isUnit(t1)) {
				MathUtil.mat4product(t1, op, t2, t3);
				MathUtil.copyNN(t2, t1);
				n++;
			}
			double[] v = new double[] { t1[0][3] / n, t1[1][3] / n, t1[2][3] / n };
			return (MathUtil.isIntegral3(v, 0.0001d) ? null : v);
		}

		static String[] labels_ = new String[] { "x", "y", "z" };

		/**
		 * 
		 * @param mat
		 * @param allPositive
		 * @param halfOrLess
		 * @param allowFractions
		 * @param fractionAsRational
		 * @return string row-form of matrix with the given labels
		 */
		static String getXYZFromMatrixFrac(double[][] mat, boolean allPositive, boolean halfOrLess,
				boolean allowFractions, boolean fractionAsRational) {
			String str = "";
			int denom = 12;
			for (int i = 0; i < 3; i++) {
				int lpt = (i < 3 ? 0 : 3);
				double[] row = mat[i];
				String term = "";
				for (int j = 0; j < 3; j++) {
					double x = row[j];
					if (MathUtil.approx(x) != 0) {
						term += plusMinus(term, x, labels_[j + lpt], allowFractions, fractionAsRational);
					}
				}

				if ((MathUtil.approx(row[3])) != 0) {
					String f = (fractionAsRational ? plusMinus(term, row[3], "", true, true)
							: xyzFraction12((row[3] * denom), denom, allPositive, halfOrLess));
					if (term == "")
						f = (f.charAt(0) == '+' ? f.substring(1) : f);
					term += f;
				}
				str += "," + (term == "" ? "0" : term);
			}
			return str.substring(1);
		}

		private final static int DIVISOR_MASK = 0xFF;
		private final static int DIVISOR_OFFSET = 8;

		private final static String xyzFraction12(double n12ths, int denom, boolean allPositive, boolean halfOrLess) {
			if (n12ths == 0)
				return "";
			double n = n12ths;
			if (denom != 12) {
				int in = (int) n;
				denom = (in & DIVISOR_MASK);
				n = in >> DIVISOR_OFFSET;
			}
			int half = (denom / 2);
			if (allPositive) {
				while (n < 0)
					n += denom;
			} else if (halfOrLess) {
				while (n > half)
					n -= denom;
				while (n < -half)
					n += denom;
			}
			String s = (denom == 12 ? twelfthsOf(n) : n == 0 ? "0" : n + "/" + denom);
			return (s.charAt(0) == '0' ? "" : n > 0 ? "+" + s : s);
		}

		private final static String[] twelfths = { "0", "1/12", "1/6", "1/4", "1/3", "5/12", "1/2", "7/12", "2/3",
				"3/4", "5/6", "11/12" };

		// private final static String[] fortyeigths = { "0",
		// "1/48", "1/24", "1/16", "1/12",
		// "5/48", "1/8", "7/48", "1/6",
		// "3/16", "5/24", "11/48", "1/4",
		// "13/48", "7/24", "5/16", "1/3",
		// "17/48", "3/8", "19/48", "5/12",
		// "7/16", "11/24", "23/48", "1/2",
		// "25/48", "13/24", "9/16", "7/12",
		// "29/48", "15/24", "31/48", "2/3",
		// "11/12", "17/16", "35/48", "3/4",
		// "37/48", "19/24", "13/16", "5/6",
		// "41/48", "7/8", "43/48", "11/12",
		// "15/16", "23/24", "47/48"
		// };
		//
		private static String plusMinus(String strT, double x, String sx, boolean allowFractions,
				boolean fractionAsRational) {
			double a = Math.abs(x);
			double afrac = a % 1; // -1.3333 and 1.3333 become 0.3333
			if (a < 0.0001d) {
				return "";
			}
			String s = (a > 0.9999d && a <= 1.0001d ? ""
					: afrac <= 0.001d && !allowFractions ? "" + (int) a
							: fractionAsRational ? "" + a : twelfthsOf(a * 12));
			return (x < 0 ? "-" : strT.length() == 0 ? "" : "+") + (s.equals("1") ? "" : s) + sx;
		}

		final static String twelfthsOf(double n12ths) {
			String str = "";
			if (n12ths < 0) {
				n12ths = -n12ths;
				str = "-";
			}
			int m = 12;
			int n = Math.round((float) n12ths);
			if (Math.abs(n - n12ths) > 0.01f) {
				// fifths? sevenths? eigths? ninths? sixteenths?
				// Juan Manuel suggests 10 is large enough here
				double f = n12ths / 12;
				int max = 20;
				for (m = 3; m < max; m++) {
					double fm = f * m;
					n = (int) Math.round(fm);
					if (Math.abs(n - fm) < 0.01f)
						break;
				}
				if (m == max)
					return str + f;
			} else {
				if (n == 12)
					return str + "1";
				if (n < 12)
					return str + twelfths[n % 12];
				switch (n % 12) {
				case 0:
					return str + n / 12;
				case 2:
				case 10:
					m = 6;
					break;
				case 3:
				case 9:
					m = 4;
					break;
				case 4:
				case 8:
					m = 3;
					break;
				case 6:
					m = 2;
					break;
				default:
					break;
				}
				n = (n * m / 12);
			}
			return str + n + "/" + m;
		}

	}// end of SymopData

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
				System.out.println("Variables: " + nAtoms + " atoms were read");

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
		 * @param key
		 * @param mode
		 * @param def        a default value or NaN to skip if key is not present
		 * @param nAtomsRead
		 * @param bsPeriodic
		 * 
		 */
		boolean getAtomTSAn(String key, int mode, double def, int nAtomsRead, BitSet bsPeriodic) {
			boolean isDefault = (vt.setData(key) == 0);
			if (isDefault && Double.isNaN(def))
				return false;
			int ncol = modes[mode].columnCount;
			for (int pt = 0, ia = 0, iread = 0, n = nAtomsRead; iread < n; iread++, pt += ncol) {
				if (bsPeriodic != null && !bsPeriodic.get(iread))
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
		 * @param bsPeriodic
		 */
		void getAtomsOccMag10Line(int mode, int nAtomsRead, BitSet bsPeriodic) {
			int ncol = modes[mode].columnCount;
			int offset = (mode == OCC ? 6 : 7);
			for (int pt = 0, ia = 0, iread = 0; iread < nAtomsRead; iread++, pt += 10) {
				if (bsPeriodic != null && !bsPeriodic.get(iread))
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
			int nRows = nData / ncol;
			int n = nRows * ncol;
			if (nData != n)
				parseError(nData, n);
			return nRows;
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
			childCell.conv2convParentTransposeP = getTransform("parentbasis", true);
			childCell.conv2primTransposeP = getTransform("conv2primchildbasis", false);
			parentCell.conv2primTransposeP = getTransform("conv2primparentbasis", false);
			parentCell.setUnstrainedCartsianBasis(isRhombParentSetting, childCell.conv2convParentTransposeP);
			parseSymmetryOperations("child");
			parseSymmetryOperations("parent");
			transformParentToChild(false);
		}

		private void parseSymmetryOperations(String type) {
			Cell cell = (type == "child" ? childCell : parentCell);
//			!convchildcenteringvecs 
//			   0.00000   0.00000   0.00000
//			   0.50000   0.50000   0.50000
			int nData = vt.setData("conv" + type + "centeringvecs");
			int nCenteringOps = nData / 3;
			int ptCenterings = 0;
			List<SymopData> list = cell.symopData = new ArrayList<SymopData>();
			double[] dataC = null;
			if (nCenteringOps != 0) {
				if (nData % 3 != 0)
					parseError("expected three columns of data (cx,cy,cz); found " + nData, 1);
				dataC = new double[nData];
				getDoubleArray(null, dataC, 0, nData);
				if (dataC[0] == 0 && dataC[1] == 0 && dataC[2] == 0) {
					nCenteringOps--;
					ptCenterings = 3;
				}
				for (int ic = 0, pt = ptCenterings; ic < nCenteringOps; ic++) {
					list.add(new SymopData(type, new double[] { dataC[pt++], dataC[pt++], dataC[pt++] }));
				}

			}
			nData = vt.setData("conv" + type + "spacegroupops");
			if (nData == 0)
				return;
			if (nData % 16 != 0)
				parseError("expected 16 columns of data for operations; found " + nData, 1);
			int nops = nData / 16;
			double[] dataO = new double[nData];
			getDoubleArray(null, dataO, 0, nData);
			for (int pt = 0, iop = 0; iop < nops; iop++) {
				double[][] op = new double[4][4];
				for (int r = 0; r < 4; r++) {
					for (int c = 0; c < 4; c++) {
						op[r][c] = dataO[pt++];
					}
				}
				double[] vi = SymopData.getIntrinsicTranslation(op);
				if (vi != null) {
					// screw axes and glide planes only
					list.add(new SymopData(type, op, vi));
				}
			}
			System.out.println(list.size() + " operations as SymData, including " + nCenteringOps + " centerings");
		}

		private double[][] getTransform(String key, boolean isRequired) {
			if (isRequired) {
				checkSize("parentbasis", 9);
			} else if (vt.setData(key) == 0) {
				return null;
			}
			double[][] t = new double[3][3];
			for (int pt = 0, j = 0; j < 3; j++) {
				for (int i = 0; i < 3; i++, pt++) {
					t[j][i] = vt.getDouble(pt);
				}
			}
			return t;
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

			BitSet bsPeriodic = vt.getBitSet("atomsinunitcell");

			int nData = vt.setData("atomcoordlist");
			// number of atoms in the file, before filtering for primitives
			int nAtomsRead = getNumberOfAtomsRead(nData);
			int ncol = nData / nAtomsRead;

//			// testing only here -- remove this if satisfied
//			BitSet bs = createBSPeriodic(nAtomsRead, ncol);
//			System.out.println(bsPeriodic);
//			System.out.println(bs);

			if (isDiffraction && bsPeriodic == null)
				bsPeriodic = createBSPeriodic(nAtomsRead, ncol);
			Variables.this.bsPeriodic = bsPeriodic;
			if (!isDiffraction)
				bsPeriodic = null;
			nAtoms = (bsPeriodic == null ? 0 : bsPeriodic.cardinality());

			// Get all the atom type information and return the number of subtype atoms for
			// each type.
			// Just for reading the file.

			int[][] nSubTypeAtomsRead = parseAtomTypes();

			// the number of primitive atoms,
			// if this is for IsoDifrract; in the end, we
			// will replace nSubAtoms with nSubPrimitiveAtoms
			int[][] nPrimitiveSubAtoms = null;

			if (nAtoms > 0) {
				nPrimitiveSubAtoms = new int[nTypes][];
				for (int i = 0; i < nTypes; i++) {
					nPrimitiveSubAtoms[i] = new int[nSubTypeAtomsRead[i].length];
				}
			}
			// find atomic coordinates of parent
			if (nData == 0) {
				parseError("atomcoordlist is missing", 3);
			}

			// nAtoms may be the number of primitive atoms only
			if (nAtoms == 0)
				nAtoms = nAtomsRead;
			atoms = new IsoAtom[nAtoms];

			// Find number of subatoms for each subtype
			// Set up nSubAtom (and nPrimitiveSubAtoms if this is for IsoDiffract)

			// BH 2023.12
			// firstAtomOfType is an array containing pointers in the overall list of atoms
			// read (element 0) and the filtered primitive list (element 1)
			// by type. firstAtomOfType[0] is always [0, 0], and we add an addition element
			// at the end that is [nAtomsRead, nAtoms]. These are useful in the mode
			// listings,
			// where we need to catalog atom mode vectors from lists involving type indices
			// only
			int[][] firstAtomOfType = new int[nTypes + 1][2];
			firstAtomOfType[0] = new int[2];
			firstAtomOfType[nTypes] = new int[] { nAtomsRead, nAtoms };

			vt.setData("atomcoordlist");
			readAtomCoordinates(ncol, nAtomsRead, nSubTypeAtomsRead, bsPeriodic, nPrimitiveSubAtoms, firstAtomOfType);

			nSubAtoms = (bsPeriodic == null ? nSubTypeAtomsRead : nPrimitiveSubAtoms);
			for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
				// (STRAIN and IRRED are handled later)
				modes[i] = new Mode(i, nTypes, nSubTypes, nSubAtoms);
			}
			modes[DIS].isActive = true;

			// find atomic occupancies, magnetic moments, atomic rotations, and adp
			// ellipsoids of parent

			if (ncol == 10) {
				// old format t s a x y z occ mx my mz
				getAtomsOccMag10Line(OCC, nAtomsRead, bsPeriodic);
				getAtomsOccMag10Line(MAG, nAtomsRead, bsPeriodic);
			} else {
				getAtomTSAn("atomocclist", OCC, 1.0, nAtomsRead, bsPeriodic);
				getAtomTSAn("atommaglist", MAG, 0.0, nAtomsRead, bsPeriodic);
			}
			getAtomTSAn("atomrotlist", ROT, 0.0, nAtomsRead, bsPeriodic);
			if (!getAtomTSAn("atomelplist", ELL, Double.NaN, nAtomsRead, bsPeriodic)) {
				// default to [0.04 0.04 0.04 0 0 0]
				double[] def = new double[] { defaultUiso, defaultUiso, defaultUiso, 0, 0, 0 };
				for (int ia = 0; ia < atoms.length; ia++)
					atoms[ia].vectorBest[ELL] = def;
			}

			// now get all the symmetry-related arrays
			parseAtomicMode(DIS, "displacivemodelist", 3, firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);
			parseAtomicMode(OCC, "scalarmodelist", 1, firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);
			parseAtomicMode(MAG, "magneticmodelist", 3, firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);
			parseAtomicMode(ROT, "rotationalmodelist", 3, firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);
			parseAtomicMode(ELL, "ellipmodelist", 3, firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);

		}

		/**
		 * Just filtering out atoms with coord 1.000
		 * 
		 * @param nAtomsRead
		 * @param ncol
		 * @return
		 */
		private BitSet createBSPeriodic(int nAtomsRead, int ncol) {
			BitSet bs = new BitSet(nAtomsRead);
			bs.set(0, nAtomsRead);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < nAtomsRead; i++) {
				int pt = ncol * i + 3;
				double[] p = new double[] { vt.getDouble(pt), vt.getDouble(pt + 1), vt.getDouble(pt + 2) + 0.25 };
				// String s0= MathUtil.a2s(p);
				String spt = MathUtil.a2s(MathUtil.unitize3(p, 0.001d));
				if (sb.indexOf(spt) >= 0) {
					bs.clear(i);
					MathUtil.scale3(p, 0.001, p);
					// System.out.println("- " + s0);
				} else {
					sb.append(spt);
					// System.out.println("+ " + s0);
				}
			}
			System.out.println("bsPeriodic " + bs.cardinality() + "/" + nAtomsRead);
			return bs;
		}

		private void readAtomCoordinates(int ncol, int nAtomsRead, int[][] nSubTypeAtomsRead, BitSet bsPeriodic,
				int[][] nPrimitiveSubAtoms, int[][] firstAtomOfType) {
			int lastT = 0;
			for (int i = 0, ia = 0; i < nAtomsRead; i++) {
				int pt = ncol * i;
				int t = vt.getInt(pt++) - 1;
				if (t != lastT) {
					firstAtomOfType[t] = new int[] { i, ia };
					lastT = t;
				}
				int s = vt.getInt(pt++) - 1;
				boolean isOK = (bsPeriodic == null || bsPeriodic.get(i));
				if (isOK) {
					int a = vt.getInt(pt++) - 1;
					double[] coord = new double[] { vt.getDouble(pt++), vt.getDouble(pt++), vt.getDouble(pt++) };
					if (bsPeriodic != null) {
						nPrimitiveSubAtoms[t][s]++;
					}
					atoms[ia] = new IsoAtom(ia, t, s, a, coord, atomTypeSymbol[t]);
					setAtomType(t, s, ia);
//					if (haveBonds)
//						atomMap.put(getKeyTSA(t + 1, s + 1, a + 1), atom);
					ia++;
				}
				nSubTypeAtomsRead[t][s]++;
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
			int n = nTypes = checkSizeN("atomtypelist", 3, true);

			atomTypeName = new String[n];
			atomTypeSymbol = new String[n];
			for (int i = 0; i < n; i++) {
				if (vt.getInt(3 * i) != i + 1)
					parseError("atom types are not in sequential order", 2);
				atomTypeName[i] = vt.getString(3 * i + 1);
				// no reason to believe this would be XX or xx, but
				// this guarantees Xx for Elements.getScatteringFactor.
				String s = vt.getString(3 * i + 2);
				atomTypeSymbol[i] = s.substring(0, 1).toUpperCase()
						+ (s.length() == 1 ? "" : s.substring(1, 2).toLowerCase());
			}

			nSubTypes = new int[n];
			// start this off with one implicit subtype.
			int[][] nSubTypeAtomsRead = new int[n][1];

			// find atom subtypes (optional)
			int nSubTypeEntries = checkSizeN("atomsubtypelist", 3, false);
			if (nSubTypeEntries > 0) {
				int curType = 0, curSubType = 0;
				for (int i = 0; i < nSubTypeEntries; i++) {
					int itype = vt.getInt(3 * i);
					if (itype != curType) {
						if (curSubType != 0) {
							// close out previous type's pointers
							nSubTypeAtomsRead[curType - 1] = new int[curSubType];
						}
						curType++;
						curSubType = 0;
						if (itype != curType)
							parseError("The atom types in the atom subtype list are out of order", 2);
					}
					int subtype = vt.getInt(3 * i + 1);
					if (subtype != curSubType) {
						nSubTypes[curType - 1]++;
						curSubType++;
						if (subtype != curSubType)
							parseError("The subtypes in the atom subtype list are out of order", 2);
					}
				}
				nSubTypeAtomsRead[curType - 1] = new int[curSubType];

				// Assign the subtype names
				subTypeName = new String[n][];
				for (int pt = 0, t = 0; t < n; t++) {
					subTypeName[t] = new String[nSubTypes[t]];
					for (int s = 0; s < nSubTypes[t]; s++) {
						subTypeName[t][s] = vt.getString(3 * pt++ + 2);
					}
				}
			} else {
				// This code is used in ISOCIF, where there are no subtypes defined.
				subTypeName = new String[n][];
				for (int t = 0; t < n; t++) {
					nSubTypes[t] = 1;
					subTypeName[t] = new String[nSubTypes[t]];
					for (int s = 0; s < nSubTypes[t]; s++) {
						subTypeName[t][s] = atomTypeName[t] + "_" + (s + 1);
					}
				}
			}
			return nSubTypeAtomsRead;
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
			setMaxBondLength(Math.max(0, d));
			d = getOneDouble("minbondlength", 0);
			setMinBondLength(Math.max(0, d));
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
		private void parseAtomicMode(int mode, String key, int ncol, int[][] firstAtomOfType, int[][] nSubTypeAtomsRead,
				BitSet bsPeriodic) {
			int[] perType = new int[nTypes];
			int n = getAtomModeNumbers(key, mode, perType, ncol, firstAtomOfType);
			if (n == 0)
				return;
			modes[mode].initArrays(perType, n);
			getAtomModeData(modes[mode], firstAtomOfType, nSubTypeAtomsRead, bsPeriodic);
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
			for (int ia = nAtoms; --ia >= 0;) {
				IsoAtom a = atoms[ia];
				a.modes[mode] = (perType[a.type] == 0 ? null : new double[perType[a.type]][]);
			}
			return n;
		}

		/**
		 * Parse and save all mode-related data.
		 * 
		 * @param firstAtomOfType
		 * @param nSubTypeAtomsRead
		 * 
		 * @param isoParser
		 * @param bsPeriodic
		 * @param key
		 * 
		 * 
		 * @param parser
		 * 
		 */
		private void getAtomModeData(Mode mode, int[][] firstAtomOfType, int[][] nSubTypeAtomsRead, BitSet bsPeriodic) {

//        t    m   calcAmp   maxAmp  irrep  name
//		    1    1   0.00030   2.82843    3 GM1+[Sr:b:occ]A1g(a) 

			int type = mode.type;
			int ncol = mode.columnCount;
			int[] modeTracker = new int[nTypes];
			for (int pt = 0, m = 0; m < mode.count; m++) {
				int atomType = vt.getInt(pt++) - 1;
				int iread = firstAtomOfType[atomType][0];
				int ia = firstAtomOfType[atomType][1];
				int mt = modeTracker[atomType]++;
				if (mt + 1 != vt.getInt(pt++))
					parseError("The modes are not given in ascending order", 2);
				// pick up location of the calculated value start-to-end
				mode.isovizPtrTM[atomType][mt] = vt.getPosition(pt);
				mode.calcAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.maxAmpTM[atomType][mt] = vt.getDouble(pt++);
				mode.irrepTM[atomType][mt] = vt.getInt(pt++) - 1;
				mode.setModeName(atomType, mt, vt.getString(pt++));

				for (int s = 0; s < nSubTypes[atomType]; s++) {
					for (int a = 0; a < nSubTypeAtomsRead[atomType][s]; a++, iread++, pt += ncol) {
						if (bsPeriodic != null && !bsPeriodic.get(iread))
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
			Mode mode = modes[STRAIN] = new Mode(STRAIN, 1, null, null);
			modes[STRAIN].initArrays(null, n);
			for (int m = 0; m < n; m++) {
				mode.setModeName(0, m, vt.getString(ncol * m + 4));
				mode.irrepTM[0][m] = vt.getInt(ncol * m + 3) - 1;
				mode.isovizPtrTM[0][m] = vt.getPosition(ncol * m + 1);
				mode.calcAmpTM[0][m] = vt.getDouble(ncol * m + 1);
				mode.maxAmpTM[0][m] = vt.getDouble(ncol * m + 2);
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
			Mode mode = modes[IRREP] = new Mode(IRREP, 1, null, null);
			mode.initArrays(null, n);
			for (int i = 0; i < n; i++) {
				if (vt.getInt(2 * i) != i + 1)
					parseError("Error: irreps are not in ascending order.", 2);
				mode.setModeName(0, i, vt.getString(2 * i + 1));
			}
		}

	} // end of IsoParser

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

		private final float[] hsb = new float[3];

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
		private JPanel typeTitlePanel[];
		/**
		 * Panel which holds the occupancy check boxes for each atom subtype associated
		 * with a type
		 * 
		 */
		private JPanel subTypePanel[];

		private int sliderWidth;
		private int sliderPanelWidth;

		/**
		 * Set this web input form map's xxxmode00t00m, scalar00t00m, and strainN values
		 * to the desired values or set the isovizData to this information.
		 * 
		 * @param mapFormData
		 * @param isovizData
		 * @param sliderID      "current", "parent", or "child"
		 * @param childFraction
		 */
		void replaceData(Map<String, Object> mapFormData, byte[] isovizData, String sliderID, double childFraction) {
			boolean toZero = "parent".equals(sliderID);
			boolean toInitial = "child".equals(sliderID);
			// otherwise "current"
			for (int mode = 0; mode < MODE_ATOMIC_COUNT; mode++) {
				if (isModeActive(modes[mode])) {
					IsoSlider[][] sliders = this.sliderTM[mode];
					for (int t = sliders.length; --t >= 0;) {
						for (int m = sliders[t].length; --m >= 0;) {
							String name = getInputName(mode, t, m);
							IsoSlider sm = sliders[t][m];
							// Here we check to see if the slider label reads its initial value.
							// In that case, we use the "initial" value
							double d = (toZero ? 0
									: toInitial || childFraction == 1 && sm.childLabelValue == sm.childLabelValue0
											? sm.calcAmp
											: sm.childLabelValue * childFraction);
							// System.out.println("VAR " + name + "=" + d);
							if (isovizData == null)
								setModeFormValue(name, d, mapFormData, null);
							else
								IsoTokenizer.replaceIsovizFileValue(isovizData, modes[mode].isovizPtrTM[t][m], d);
						}
					}
				}
			}
			if (isModeActive(modes[STRAIN])) {
				double[] vals = modes[STRAIN].valuesTM[0];
				for (int m = vals.length; --m >= 0;) {
					String name = getInputName(STRAIN, 0, m);
					double d = (toZero ? 0 : toInitial ? modes[STRAIN].calcAmpTM[0][m] : vals[m]);
					if (isovizData == null)
						setModeFormValue(name, d, mapFormData, null);
					else
						IsoTokenizer.replaceIsovizFileValue(isovizData, modes[STRAIN].isovizPtrTM[0][m], d);
				}
			}
		}

		/**
		 * Set the shade of gray for subtype atoms.
		 * 
		 * @param t
		 * @param s
		 * @return a number between low and high
		 */
		double getSelectedSubTypeShade(int t, int s) {
			// adjust these parameters as needed
			double low = 0.0;
			double high = 0.8;
			double step = 0.0;
			if (nSubTypes[t] > 1) {
				step = (high - low) / (nSubTypes[t] - 1);
			}
			return low + s * step;
		}

		/**
		 * current static of (no) shading in slider panel
		 */
		boolean colorByElement;

		Color[] atomTypeColors;

		Color getAtomTypeColor(int t) {
			return atomTypeColors[t];
		}

		void setColors() {
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i])) {
					Color[] colors = modes[i].getColorT();
					setColorScheme(colors, modes[i].type);
				}
			}
			if (atomTypeColors == null)
				atomTypeColors = new Color[nTypes];
			setColorScheme(atomTypeColors, -1);
		}

		void updateColorScheme(boolean isByElmeent) {
			if (app.colorBox == null)
				return;
			colorByElement = isByElmeent;
			app.colorBox.setEnabled(false);
			app.colorBox.setSelected(!isByElmeent);
			app.colorBox.setEnabled(true);
			setColors();
			for (int t = 0; t < nTypes; t++) {
				Color c = getAtomTypeColor(t);
				typeTitlePanel[t].setBackground(c);
				typeTitlePanel[t].getParent().setBackground(c);
				JPanel p = subTypePanel[t];
				p.setBackground(c);
				// recolor the checkbox/label panels as well as
				// the column-filler backgrounds
				for (int i = p.getComponentCount(); --i >= 0;) {
					p.getComponent(i).setBackground(c);
				}
				for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
					if (isModeActive(modes[i]))
						modeSliderPanelsT[i][t].setBackground(modes[i].colorT[t]);
				}
			}
		}

		private static final float COLOR_BRIGHTNESS_ATOMS = 0.90f;
		private static final float COLOR_BRIGHTNESS_DIS = 0.80f;
		private static final float COLOR_BRIGHTNESS_OCC = 0.65f;
		private static final float COLOR_BRIGHTNESS_ROT = 0.50f;
		private static final float COLOR_BRIGHTNESS_MAG = 0.35f;
		private static final float COLOR_BRIGHTNESS_ELL = 0.20f;

		/**
		 * Set the colors of mode slider and title/checkbox backgrounds.
		 * 
		 * @param colors
		 * @param type
		 */
		private void setColorScheme(Color[] colors, int type) {
			float saturation = 1.00f;
			float brightness;
			switch (type) {
			default:
			case ATOMS:
				brightness = COLOR_BRIGHTNESS_ATOMS; // for titles and checkboxes
				break;
			case DIS:
				brightness = COLOR_BRIGHTNESS_DIS;
				break;
			case OCC:
				brightness = COLOR_BRIGHTNESS_OCC;
				break;
			case MAG:
				brightness = COLOR_BRIGHTNESS_MAG;
				break;
			case ROT:
				brightness = COLOR_BRIGHTNESS_ROT;
				break;
			case ELL:
				brightness = COLOR_BRIGHTNESS_ELL;
				break;
			case STRAIN:
				colors[0] = COLOR_STRAIN;
				return;
			case IRREP:
				colors[0] = COLOR_IRREP; // BH a bit darker than LIGHT_GRAY C0C0C0
				return;
			}
			if (colorByElement) {
				for (int t = 0; t < nTypes; t++) {
					int argb = Elements.getCPKColor(atomTypeSymbol[t]);
					Color.RGBtoHSB((argb & 0xFF0000) >> 16, (argb & 0xFF00) >> 8, argb & 0xFF, hsb);
					colors[t] = new Color(Color.HSBtoRGB(hsb[0], hsb[1], brightness));
//					System.out.println("Variables.scs " + colorByElement 
//							+ " " + atomTypeSymbol[t] 
//							+ " rgb=" 
//							+ Integer.toHexString(argb) 
//							+ " h=" + hsb[0] + " s=" + hsb[1]+ " b=" + hsb[2] 
//							+ " br=" + brightness);
				}
			} else {
				for (int t = 0; t < nTypes; t++) {
					float hue = 1f * t / nTypes;
					colors[t] = new Color(Color.HSBtoRGB(hue, saturation, brightness));
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
				if (isModeActive(modes[i]))
					modes[i].setSliders(sliderTM[i], 0);
			}
		}

		void resetSliders() {
			mainSlider.setValue(sliderMax);
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i]))
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
					int n = (i >= MODE_ATOMIC_COUNT ? 1 : nTypes);
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
			double f0 = mainSliderChildFraction;
			mainSliderChildFraction = mainSlider.getValue() / maxJSliderIntVal;
			if (mainSliderChildFraction != f0) {
				app.recalcCellColors();
			}
			String main = MathUtil.varToString(mainSliderChildFraction, 2, -6) + " child";
			mainSliderLabel.setText(main);
			for (int i = MODE_COUNT; --i >= 0;) {
				if (isModeActive(modes[i]))
					modes[i].readSliders(sliderTM[i], maxJSliderIntVal, modes[IRREP]);
			}
		}

		void initPanels(JPanel sliderPanel, int width) {

			// this.sliderPanel = sliderPanel;

			// Divide the applet area with structure on the left and controls on the right.

			sliderPanelWidth = width;
			sliderWidth = (int) (sliderPanelWidth * 0.6);

			needColorBox = true;// haveElementSubtypes();
			colorByElement = true;
			setColors();

			createMasterSliderPanel(sliderPanel, width);
		}

		private void createMasterSliderPanel(JPanel sliderPanel, int width) {
			/**
			 * Maximum number of check boxes per row the GUI will hold
			 */
			int maxSubTypesPerRow = width / subTypeWidth;

			int[] nSubRows = new int[nTypes];
			int[] subTypesPerRow = new int[nTypes];
			for (int t = 0; t < nTypes; t++) {
				// iterate over types
				subTypesPerRow[t] = Math.min(maxSubTypesPerRow, nSubTypes[t]);
				nSubRows[t] = (int) Math.ceil((double) nSubTypes[t] / subTypesPerRow[t]);
			}
			masterSliderPanel = new JPanel();
			masterSliderPanel.setLayout(new BoxLayout(masterSliderPanel, BoxLayout.LINE_AXIS));
			masterSliderPanel.setBorder(new EmptyBorder(2, 2, 5, 80));
			masterSliderPanel.setBackground(Color.WHITE);

			// Master Slider Panel
			mainSliderLabel = new JLabel("child");
			mainSliderLabel.setForeground(COLOR_CHILD_CELL);
			mainSliderLabel.setHorizontalAlignment(JLabel.CENTER);
			mainSliderLabel.setVerticalAlignment(JLabel.CENTER);
			mainSlider = new IsoSlider(-1, mainSliderChildName, 0, 1, 1, Color.WHITE);

			masterSliderPanel.add(Box.createHorizontalGlue());
			JLabel pl = new JLabel(mainSliderParentName + " ");
			pl.setForeground(COLOR_PARENT_CELL);
			masterSliderPanel.add(pl);
			masterSliderPanel.add(mainSlider);
			masterSliderPanel.add(Box.createRigidArea(new Dimension(1, 1)));
			masterSliderPanel.add(mainSliderLabel);
			masterSliderPanel.add(Box.createHorizontalGlue());
			addPanel(sliderPanel, masterSliderPanel, "masterPanel");

			// Initialize type-specific subpanels of scrollPanel
			typeLabel = new JLabel[nTypes];
			typeTitlePanel = new JPanel[nTypes];
			subTypePanel = new JPanel[nTypes];
			subTypeCheckBoxes = new JCheckBox[nTypes][];
			subTypeLabels = new JLabel[nTypes][];

			// The big loop over types
			for (int t = 0; t < nTypes; t++) {
				Color c = getAtomTypeColor(t);
				JPanel typeInfoPanel = new JPanel();
				typeInfoPanel.setName("typeOuterPanel_" + t);
				typeInfoPanel.setLayout(new BoxLayout(typeInfoPanel, BoxLayout.Y_AXIS));
				typeInfoPanel.setBorder(new EmptyBorder(2, 2, 5, 60));
				typeInfoPanel.setBackground(c); // important
				typeLabel[t] = newWhiteLabel("" + atomTypeName[t] + " Modes", JLabel.CENTER);
				typeTitlePanel[t] = new JPanel(new GridLayout(1, 1, 0, 0));
				typeTitlePanel[t].setName("typeNamePanel_" + t + " " + atomTypeName[t]);
				typeTitlePanel[t].add(typeLabel[t]);
				typeTitlePanel[t].setBackground(c); // important
				typeInfoPanel.add(typeTitlePanel[t]);

				// typeDataPanel
				subTypePanel[t] = new JPanel(new GridLayout(nSubRows[t], subTypesPerRow[t], 0, 0));
				subTypePanel[t].setName("typeDataPanel_" + t);
				int nst = nSubTypes[t];
				subTypeCheckBoxes[t] = new JCheckBox[nSubTypes[t]];
				subTypeLabels[t] = new JLabel[nSubTypes[t]];
				for (int s = 0; s < nst; s++) {
					JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.setName("subtypePanel_" + s);
					p.setBackground(c);
					JCheckBox b = subTypeCheckBoxes[t][s] = newSubtypeCheckbox("subType_" + t + "_" + s, c);
					// BH testing removal b.setEnabled(!isDiffraction);
					subTypeLabels[t][s] = newWhiteLabel("", JLabel.LEFT);
					p.add(b);
					p.add(subTypeLabels[t][s]);
					subTypePanel[t].add(p);
				}
				// Makes sure that each subtype row is full for alignment purposes
				for (int s = nst; s < nSubRows[t] * subTypesPerRow[t]; s++) {
					JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.setName("subtypeFillerPanel_" + s);
					p.setBackground(c);
					// p.add(new JLabel(""));
					subTypePanel[t].add(p);
				}
				typeInfoPanel.add(subTypePanel[t]);
				addPanel(sliderPanel, typeInfoPanel, "typeInfoPanel_" + t + " " + typeLabel[t].getText());
				int n = 0;
				for (int i = 0; i < MODE_ATOMIC_COUNT; i++) {
					n += initModeGUI(sliderPanel, modes[i], t);
				}
				if (n == 0) {
					typeInfoPanel = new JPanel(new GridLayout(1, 1, 0, 0));
					typeInfoPanel.setBackground(c);
					addPanel(sliderPanel, typeInfoPanel, "typeFiller_" + t);
				}
			}
			Color c = COLOR_STRAIN;
			JPanel strainTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
			strainTitlePanel.setBorder(new EmptyBorder(2, 2, 5, 80));
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
				c = COLOR_IRREP;
				JPanel irrepTitlePanel = new JPanel(new GridLayout(1, 1, 0, 0));
				irrepTitlePanel.setBorder(new EmptyBorder(2, 2, 5, 80));
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
			cell.title = newWhiteLabel(cell.labelText, JLabel.LEFT);
			cell.title.setForeground(cell.color);
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
		private JPanel[][] modeSliderPanelsT = new JPanel[MODE_COUNT][];

		class IsoSlider extends JSlider implements ChangeListener {

			private int min; // 0 or -SliderMax

			private int type;

			private double calcAmp, maxAmp;

			/**
			 * zero value
			 */
			JLabel blackCalcAmpPointer;

			/**
			 * A small orange triangle pointer on atom mode and strain sliders only that
			 * indicates the zero point.
			 */
			JLabel orangeZeroPointer;

			/**
			 * Currently displayed value
			 */
			JLabel whiteCurrentAmpValuePointer;

			JLabel sliderLabel;

			public double childLabelValue, childLabelValue0 = Double.NaN;

			void setSliderLabel(String text) {
				sliderLabel.setText(text);
			}

			IsoSlider(int type, String name, int min, double calcAmp, double maxAmp, Color c) {
				super(JSlider.HORIZONTAL, min, sliderMax, (int) (calcAmp / maxAmp * sliderMax));
				setName(name);
				this.type = type;
				// this setting is important; without it, everything looks shrunk
				setPreferredSize(new Dimension(sliderWidth, barheight));
				// System.out.println("Variables.IsoSlider.setpref " + name + " " +
				// sliderWidth);
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
						blackCalcAmpPointer = getPointer(Color.BLACK);
						orangeZeroPointer = getPointer(Color.ORANGE);
						whiteCurrentAmpValuePointer = getPointer(Color.WHITE);
					} else {
						whiteCurrentAmpValuePointer = getPointer(Color.WHITE);
						if (min != 0) {
							orangeZeroPointer = getPointer(Color.ORANGE);
							blackCalcAmpPointer = getPointer(Color.BLACK);
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
			void setLabelValue(double val, String name) {
				childLabelValue = val;
				if (Double.isNaN(childLabelValue0))
					childLabelValue0 = childLabelValue;
				// System.out.println("V.setLabelValue " + name + " childValue=" + val);
				int prec = (type < MODE_ATOMIC_COUNT ? 2 : 3);
				setSliderLabel(MathUtil.varToString(childLabelValue, prec, -8) + "  " + name);
				setSliderPointerPositions();
			}

			private void setSliderPointerPositions() {
				if (whiteCurrentAmpValuePointer == null) {
					return;
				}
				int w = getWidth();
				setPointerPosition(whiteCurrentAmpValuePointer, childLabelValue * mainSliderChildFraction / maxAmp, w);
				if (orangeZeroPointer != null) {
					setPointerPosition(orangeZeroPointer, 0, w);
					setPointerPosition(blackCalcAmpPointer, calcAmp / maxAmp, w);
				}
			}

			private void setPointerPosition(JLabel pointer, double d, int w) {
				w -= (/** @j2sNative 1 ? 20 : */
				15);
				int off = (/** @j2sNative 1 ? 8 : */
				6);
				int x = (int) ((d * sliderMax - min) / (sliderMax - min) * w);
				pointer.setBounds(x + off, 12, 6, 8);
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
				int nTypes = mode.nTypes;
				modeSliderPanelsT[type] = new JPanel[nTypes];
				sliderTM[type] = new IsoSlider[nTypes][];
			}
			Color c = (mode.colorT == null ? Color.PINK : mode.colorT[t]);
			int min = (mode.type == IRREP ? 0 : -sliderMax);
			int nModes = mode.modesPerType[t];
			sliderTM[type][t] = new IsoSlider[nModes];
			JPanel modeSliderPanel = modeSliderPanelsT[type][t] = new JPanel(new GridLayout(nModes, 2, 0, 0));
			modeSliderPanel.setBackground(c);
			for (int m = 0; m < nModes; m++) {
				sliderTM[type][t][m] = new IsoSlider(mode.type, getInputName(mode.type, t, m), min,
						mode.calcAmpTM[t][m], mode.maxAmpTM[t][m], c);
				sliderTM[type][t][m].sliderLabel = newWhiteLabel("", JLabel.LEFT);
				modeSliderPanel.add(sliderTM[type][t][m]);
				modeSliderPanel.add(sliderTM[type][t][m].sliderLabel);
			}
			addPanel(sliderPanel, modeSliderPanel, "modeSliderPanelType_" + t);
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

		void updateSliderPointers() {
			for (int i = 0; i < MODE_COUNT; i++) {
				if (isModeActive(modes[i])) {
					int n = (i >= MODE_ATOMIC_COUNT ? 1 : nTypes);
					for (int t = 0; t < n; t++) {
						for (int m = 0; m < modes[i].modesPerType[t]; m++) {
							sliderTM[i][t][m].setSliderPointerPositions();
						}
					}
				}
			}
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
		return gui.getSelectedSubTypeShade(t, s);
	}

	private Map<Integer, BitSet> mapTStoAtoms = new HashMap<>();
	
	public void setAtomType(int t, int s, int ia) {
		Integer key = Integer.valueOf(t << 10 + s);
		BitSet bs = mapTStoAtoms.get(key);
		if (bs == null)
			mapTStoAtoms.put(key, bs = new BitSet());
		bs.set(ia);
	}
	
	public BitSet getAtomsFromTS(int t, int s) {
		Integer key = Integer.valueOf(t << 10 + s);
		return mapTStoAtoms.get(key);
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
		nSubAtoms = null;
		nSubTypes = null;
	}

	/**
	 * Checks for the presence of primitive atoms and also for
	 * 
	 * @param i
	 * @return
	 */
	public boolean isPrimitive(int i) {
		return (i < 0 || bsPeriodic == null ? bsPeriodic != null : bsPeriodic.get(i));
	}

	public void updateSliderPointers() {
		gui.updateSliderPointers();
	}

}
