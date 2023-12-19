package org.byu.isodistort.local;

import java.awt.Color;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

import org.byu.isodistort.local.Variables.Atom;
import org.byu.isodistort.local.Variables.VariableParser;

/**
 * A class to hold the arrays relating to individual types of symmetry modes,
 * specifically: displacement, magnetic, rotational, anisotroic ("ellipsoidal"),
 * strain, and irreducible representations.
 * 
 * STRAIN and IRREP are special in that they are not atom-specific. Thus,
 * sometimes
 * 
 * @author hanso
 *
 */
class Mode {
	
	final static int DIS = 0; // displacive
	final static int OCC = 1; // occupancy (aka "scalar")
	final static int MAG = 2; // magnetic
	final static int ROT = 3; // rotational
	final static int ELL = 4; // ellipsoidal
	final static int MODE_ATOMIC_COUNT = 5;

	final static int STRAIN = 5;
	final static int IRREP = 6; // irreducible representations
	final static int MODE_COUNT = 7;


	@SuppressWarnings("unused")
	/**
	 * DISP, IRREP, ....
	 */
	int type;

	/**
	 * the number of total number of entries for this mode in the isoviz file.
	 */
	int count;

	/**
	 * number of columns of data for this type of symmetry mode
	 */
	int columnCount;

	/**
	 * [atomtype][mode]
	 */
	int[] modesPerType;

	/**
	 * GUI panels for each type, holding sliders for the type's relevant modes.
	 */
	JPanel[] typePanels;

	/**
	 * This [atomtype][mode][subtype][subatom][value]
	 */
	double[][][][][] fileModeCoeffsTMSA; // was xxxmodeVect

	/**
	 * [atomtype][modefortype]
	 */
	double[][] initAmpTM;
	double[][] maxAmpTM;
	int[][] irrepTM;
	String[][] nameTM;
	JSlider[][] sliderTM;
	JLabel[][] sliderLabelsTM;
	double[][] sliderValsTM;

	private double[][] savedSliderValues;

	/**
	 * strain and irrep only [mode][value]
	 */
	double[][] vector;

	Color[] colorT;

	boolean isActive;

	final private double[] delta;

	int numTypes;
	int[] numSubTypes;
	int[][] numSubAtoms;

	Mode(int type, int numTypes, int[] numSubTypes, int[][] numSubAtoms) {
		this.numTypes = numTypes;
		this.numSubTypes = numSubTypes;
		this.numSubAtoms = numSubAtoms;

		this.type = type;
		switch (type) {
		case DIS:
		case MAG:
		case ROT:
			columnCount = 3;
			break;
		case OCC:
		case IRREP:
			columnCount = 1;
			break;
		case ELL:
		case STRAIN:
			columnCount = 6;
			break;
		}
		delta = new double[columnCount];
	}

	void initAtoms() {
		// nothing to do -- now saved in Atom.vector0, Atom.vector1, and Atom.mode
	}

	/**
	 * Prepare the STRAIN and IRREP array data, which do not involve specific atoms.
	 * 
	 * @param num
	 */
	void initArraysNonAtom(int num) {
		isActive = true;
		count = num;
		nameTM = new String[1][num];
		modesPerType = new int[] { num };
		initAmpTM = new double[1][num];
		maxAmpTM = new double[1][num];

		switch (type) {
		case STRAIN:
			columnCount = 6;
			irrepTM = new int[1][num];
			vector = new double[num][6];
			break;
		case IRREP:
			for (int m = 0; m < count; m++) {
				initAmpTM[0][m] = maxAmpTM[0][m] = 1;
			}
			break;
		}
	}

	/**
	 * Prepare the arrays for holding parsed data.
	 * 
	 * @param perType
	 * @param count
	 */
	void initArraysMode(int[] perType, int count) {
		isActive = true;
		this.count = count;
		this.modesPerType = perType;
		initAmpTM = new double[numTypes][];
		maxAmpTM = new double[numTypes][];
		irrepTM = new int[numTypes][];
		nameTM = new String[numTypes][];
		fileModeCoeffsTMSA = new double[numTypes][][][][];
		for (int t = 0; t < numTypes; t++) {
			int nmodes = modesPerType[t];
			irrepTM[t] = new int[nmodes];
			initAmpTM[t] = new double[nmodes];
			maxAmpTM[t] = new double[nmodes];
			nameTM[t] = new String[nmodes];
			fileModeCoeffsTMSA[t] = new double[nmodes][][][];
			int nsub = numSubTypes[t];
			int[] natom = numSubAtoms[t];
			for (int m = 0; m < nmodes; m++) {
				fileModeCoeffsTMSA[t][m] = new double[nsub][][];
				for (int s = 0; s < nsub; s++)
					fileModeCoeffsTMSA[t][m][s] = new double[natom[s]][columnCount];
			}
		}
	}

	/**
	 * Parse and save all mode-related data.
	 * 
	 * 
	 * @param parser
	 */
	void getModeData(VariableParser parser) {
		// input mode array [atom type][mode number of that type][atom
		// number of that type][column data]
		int[] modeTracker = new int[numTypes];
		int[][] subAtoms = parser.numSubAtomsRead;
		for (int pt = 0, m = 0; m < count; m++) {
			int thisType = parser.getInt(pt++) - 1;
			int mode = modeTracker[thisType]++;
			if (mode + 1 != parser.getInt(pt++))
				parser.parseError("The modes are not given in ascending order", 2);
			initAmpTM[thisType][mode] = parser.getDouble(pt++);
			maxAmpTM[thisType][mode] = parser.getDouble(pt++);
			irrepTM[thisType][mode] = parser.getInt(pt++) - 1;
			nameTM[thisType][mode] = parser.getItem(pt++);
			for (int s = 0; s < numSubTypes[thisType]; s++) {
				for (int a = 0; a < subAtoms[thisType][s]; a++) {
					for (int i = 0; i < columnCount; i++) {
						fileModeCoeffsTMSA[thisType][mode][s][a][i] = parser.getDouble(pt++);
					}
				}
			}
		}
	}

	/**
	 * The heart of the entire operation.
	 * 
	 * First ensure that all atoms have the proper list of mode irrep parameters
	 * from the this mode's list, which are initially cataloged only by atom type
	 * only.
	 * 
	 * Then attenuate the irrep[t][m] by the slider values
	 * 
	 * distortion = sum{modeCoefs[m] * irrep[t][m] * sliderValue * superSliderVal}
	 * 
	 * Finally, apply that distortion to the atom's parameter vector (coord,
	 * occupation, magnetic moment, rotational displacement, etc.)
	 * 
	 * @param max
	 * @param tempvec
	 * @param tempmat
	 */
	void calcDistortion(Variables v, double[][][] max, double[] tempvec, double[][] tempmat) {
		double[] irrepVals = v.modes[IRREP].sliderValsTM[0];
		double superVal = v.superSliderVal;
		for (int ia = 0, n = v.numAtoms; ia < n; ia++) {
			Atom a = v.atoms[ia];
			MathUtil.vecfill(delta, 0);
			int t = a.t;
			int s = a.s;
			if (isActive) {
				// prepare atom modes
				if (a.modes[type] == null) {
					a.modes[type] = new double[modesPerType[t]][];
					for (int m = 0; m < modesPerType[t]; m++) {
						a.modes[type][m] = fileModeCoeffsTMSA[t][m][s][a.a];
						// fileModeCoeffsTMSA[t][m][s][a.a] = null; // no longe
					}
				}
				// accumulate distortion deltas
				for (int m = 0; m < modesPerType[t]; m++) {
					double d = irrepVals[irrepTM[t][m]] * sliderValsTM[t][m] * superVal;
					MathUtil.vecadd(delta, a.modes[type][m], d, delta);
				}
			}

			// apply the distortion to the atom's vector data
			double[] v1 = a.vector1[type];
			if (v1 == null) {
				v1 = a.vector1[type] = new double[columnCount];
			}
			MathUtil.vecadd(a.vector0[type], delta, 1, v1);

			// next is just for the labels
			switch (columnCount) {
			case 1: // OCC
				max[t][s][type] += v1[0] / numSubAtoms[t][s];
				break;
			case 3: // DIS, MAG, ROT
				MathUtil.mul(v.sBasisCart, v1, tempvec);
				double d = MathUtil.len3(tempvec);
				if (d > max[t][s][type])
					max[t][s][type] = d;
				break;
			case 6: // ELL
				MathUtil.voigt2matrix(v1, tempmat);
				MathUtil.mul(v.sBasisCart, tempmat, tempmat);
				MathUtil.mul(tempmat, v.sBasisCartInverse, tempmat);
				// ellipsoid in cartesian coords
				d = 0;
				for (int i = 0; i < 3; i++) {
					for (int j = 0; j < 3; j++) {
						d += tempmat[i][j] * tempmat[i][j];
					}
				}
				if (d > max[t][s][ELL])
					max[t][s][ELL] = d;

			}

		}
	}

	/**
	 * Save mode data in preparation for going random.
	 * 
	 */
	void saveMode() {
		if (!isActive)
			return;
		if (savedSliderValues == null) {
			savedSliderValues = new double[numTypes][];
			for (int t = 0; t < numTypes; t++) {
				savedSliderValues[t] = new double[modesPerType[t]];
			}
		}
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				savedSliderValues[t][m] = sliderValsTM[t][m];
			}
		}
	}

	/**
	 * Randomize values, taking care not to futz with GM1.
	 * 
	 * @param rval
	 * @param isGM
	 */
	void randomizeModes(Random rval, boolean isGM) {
		if (!isActive)
			return;
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				String name = nameTM[t][m];
				boolean isGM1 = (name.startsWith("GM1") && !name.startsWith("GM1-"));
				if (isGM1 == isGM) {
					sliderValsTM[t][m] = (2 * rval.nextFloat() - 1) * maxAmpTM[t][m];
				} else if (isGM) {
					sliderValsTM[t][m] = 0;
				}
			}
		}
	}

	/**
	 * Return mode to pre-randomized settings.
	 * 
	 */
	void restoreMode() {
		if (!isActive)
			return;
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				sliderValsTM[t][m] = savedSliderValues[t][m];
			}
		}
	}

	/**
	 * Set the color of this mode's panels to their designated colors
	 */
	void colorPanels() {
		if (typePanels == null || !isActive)
			return;
		for (int t = 0; t < numTypes; t++) {
			Color c = colorT[t];
			typePanels[t].setBackground(c);
			for (int m = 0; m < modesPerType[t]; m++) {
				sliderLabelsTM[t][m].setBackground(c);
				sliderTM[t][m].setBackground(c);
			}
		}
	}

	/**
	 * Set sliders to the given value -- used for Toggle IRREP and zeroing all
	 * values.
	 * 
	 * @param n
	 */
	void setSliders(int n) {
		if (!isActive)
			return;
		switch (type) {
		case STRAIN:
		case IRREP:
			for (int m = 0; m < count; m++)
				sliderTM[0][m].setValue(n);
			break;
		default:
			for (int t = 0; t < numTypes; t++) {
				for (int m = 0; m < modesPerType[t]; m++)
					sliderTM[t][m].setValue(n);
			}
			break;
		}
	}

	/**
	 * Reset all sliders to their maximum values.
	 * 
	 * @param sliderMax
	 */
	void resetSliders(int sliderMax) {
		if (!isActive)
			return;
		switch (type) {
		case STRAIN:
			numTypes = count;
			break;
		case IRREP:
			for (int m = 0; m < count; m++)
				sliderTM[0][m].setValue(sliderMax);
			break;
		default:
			for (int t = 0; t < numTypes; t++) {
				for (int m = 0; m < modesPerType[t]; m++)
					sliderTM[t][m].setValue((int) (initAmpTM[t][m] / maxAmpTM[t][m] * sliderMax));
			}
			break;
		}
	}

	/**
	 * Iterate over types and modes, setting sliderValsTM and sliderLabelsTM.
	 * 
	 * @param superSliderVal TODO
	 * @param sliderMaxVal
	 */
	void readSlider(double superSliderVal, double sliderMaxVal, Mode irreps) {
		boolean isAtomic = (type < MODE_ATOMIC_COUNT);
		boolean isIrrep = (type == IRREP);
		int prec = (isAtomic ? 2 : 3);
		int n = (isAtomic ? numTypes : 1);
		for (int t = 0; t < n; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				sliderValsTM[t][m] = maxAmpTM[t][m] * (sliderTM[t][m].getValue() / sliderMaxVal);
				double d = sliderValsTM[t][m] * (isIrrep ? 1 : irreps.sliderValsTM[0][irrepTM[t][m]]) * superSliderVal;
				sliderLabelsTM[t][m].setText(Variables.varToString(d, prec, -8) + "  " + nameTM[t][m]);
			}
		}
	}

	/**
	 * Set slider values when switching modes.
	 * 
	 * @param otherMode
	 */
	void setValues(Mode otherMode) {
		int n = (type >= MODE_ATOMIC_COUNT ? 1 : numTypes);
		for (int t = 0; t < n; t++)
			for (int m = 0; m < otherMode.modesPerType[t]; m++)
				sliderTM[t][m].setValue((int) otherMode.sliderTM[t][m].getValue());
	}

	/**
	 * Set colors for the different mode types.
	 * 
	 * @param simpleColor
	 */
	void setColors(int[] atomTypeUnique, int numUniques) {
		float brightness;
		switch (type) {
		default:
		case DIS:
			brightness = 0.95f;
			break;
		case OCC:
			brightness = 0.80f;
			break;
		case MAG:
			brightness = 0.65f;
			break;
		case ROT:
			brightness = 0.55f;
			break;
		case ELL:
			brightness = 0.40f;
			break;
		case STRAIN:
			colorT[0] = Color.DARK_GRAY;
			return;
		case IRREP:
			colorT[0] = Color.LIGHT_GRAY;
			return;
		}
		if (colorT == null) {
			colorT = new Color[numTypes];
		}
		boolean simpleColor = (atomTypeUnique != null);
		for (int t = 0; t < numTypes; t++) {
			float k = (simpleColor ? 1f * atomTypeUnique[t] / numUniques : 1f * t / numTypes);
			colorT[t] = new Color(Color.HSBtoRGB(k, 1, brightness));
		}
	}

	/**
	 * See https://en.wikipedia.org/wiki/Voigt_notation
	 * 
	 * Accumulate the mode components, as adjusted by sliders.
	 * 
	 * @return the Voigt strain tensor plus Identity
	 */
	double[][] getVoigtStrainTensor(double superSliderVal, Mode irreps) {
		double[] v = new double[6];
		for (int n = 0; n < 6; n++) {
			for (int m = 0; m < count; m++) {
				v[n] += vector[m][n] * irreps.sliderValsTM[0][irrepTM[0][m]] * sliderValsTM[0][m] * superSliderVal;
			}
		}

		// [ v0 + 1 v5 / 2 v4 / 2
		//
		// v5 / 2 v1 + 1 v3 / 2
		//
		// v4 / 2 v3 / 2 v2 + 1 ]
		//
		double[][] pStrainPlusIdentity = new double[3][3];
		pStrainPlusIdentity[0][0] = v[0] + 1;
		pStrainPlusIdentity[1][0] = v[5] / 2;
		pStrainPlusIdentity[2][0] = v[4] / 2;
		pStrainPlusIdentity[0][1] = v[5] / 2;
		pStrainPlusIdentity[1][1] = v[1] + 1;
		pStrainPlusIdentity[2][1] = v[3] / 2;
		pStrainPlusIdentity[0][2] = v[4] / 2;
		pStrainPlusIdentity[1][2] = v[3] / 2;
		pStrainPlusIdentity[2][2] = v[2] + 1;
		return pStrainPlusIdentity;
	}
	
	@Override
	public String toString() {
		return "[Mode " + type + " count=" + count + "]";
	}


}

