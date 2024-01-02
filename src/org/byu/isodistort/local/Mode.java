package org.byu.isodistort.local;

import java.awt.Color;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JPanel;

import org.byu.isodistort.local.Variables.Atom;
import org.byu.isodistort.local.Variables.VariableGUI.IsoSlider;

/**
 * A class to hold the arrays relating to individual types of symmetry modes,
 * specifically: displacement, magnetic, rotational, anisotroic ("ellipsoidal"),
 * strain, and irreducible representations.
 * 
 * STRAIN and IRREP are special in that they are not atom-specific. Thus,
 * sometimes
 * 
 * @author Bob Hanson
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

//	/**
//	 * This [atomtype][mode][subtype][subatom][value]
//	 */
//	double[][][][][] fileModeCoeffsTMSA; // was xxxmodeVect

	/**
	 * [atomtype][modefortype]
	 */
	double[][] calcAmpTM;
	double[][] maxAmpTM;
	int[][] irrepTM;
	String[][] nameTM;
	IsoSlider[][] sliderTM;
	JLabel[][] sliderLabelTM;
	double[][] sliderValTM;

	private double[][] savedSliderValues;

	/**
	 * strain and irrep only [mode][value]
	 */
	double[][] vector;

	Color[] colorT;

	boolean isActive;

	public boolean isActive() {
		return isActive && count > 0;
	}


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

	/**
	 * Prepare the arrays for holding parsed data of the sort
	 * [atomType][modeIndex][...].
	 * 
	 * For STRAIN and IRREP, these are just single-type [1][count] arrays.
	 * 
	 * @param perType
	 * @param count
	 */
	void initArrays(int[] perType, int count) {
		if (perType == null) {
			// STRAIN or IRREP
			numTypes = 1; // already done in Mode(), but just pointing this out here
			perType = new int[] { count };
		}
		isActive = true;
		this.count = count;
		this.modesPerType = perType;

		nameTM = new String[numTypes][];
		calcAmpTM = new double[numTypes][];
		maxAmpTM = new double[numTypes][];
		irrepTM = new int[numTypes][];
		for (int t = 0; t < numTypes; t++) {
			int nmodes = modesPerType[t];
			nameTM[t] = new String[nmodes];
			calcAmpTM[t] = new double[nmodes];
			maxAmpTM[t] = new double[nmodes];
			irrepTM[t] = new int[nmodes];
		}

		switch (type) {
		case STRAIN:
			columnCount = 6;
			irrepTM = new int[1][count];
			vector = new double[count][6];
			break;
		case IRREP:
			for (int m = 0; m < count; m++) {
				calcAmpTM[0][m] = maxAmpTM[0][m] = 1;
			}
			break;
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
		double[] irrepVals = (v.modes[IRREP] == null ? null : v.modes[IRREP].sliderValTM[0]);
		double superVal = v.superSliderVal;
		for (int ia = 0, n = v.numAtoms; ia < n; ia++) {
			Atom a = v.atoms[ia];
			MathUtil.vecfill(delta, 0);
			int t = a.type;
			int s = a.subType;
			if (isActive()) {
				// accumulate distortion deltas
				for (int m = 0; m < modesPerType[t]; m++) {
					double d = irrepVals[irrepTM[t][m]] * sliderValTM[t][m] * superVal;
					MathUtil.vecaddN(delta, d, a.modes[type][m], delta);
				}
			}

			// apply the distortion to the atom's vector data
			double[] v1 = a.vector1[type];
			if (v1 == null) {
				v1 = a.vector1[type] = new double[columnCount];
			}
			MathUtil.vecaddN(a.vectorBest[type], 1, delta, v1);

			// next is just for the labels
			switch (columnCount) {
			case 1: // OCC
				max[t][s][type] += v1[0] / numSubAtoms[t][s];
				break;
			case 3: // DIS, MAG, ROT
				MathUtil.mat3mul(v.sBasisCart, v1, tempvec);
				double d = MathUtil.len3(tempvec);
				if (d > max[t][s][type])
					max[t][s][type] = d;
				break;
			case 6: // ELL
				MathUtil.voigt2matrix(v1, tempmat, 0);
				MathUtil.mat3product(v.sBasisCart, tempmat, tempmat);
				MathUtil.mat3product(tempmat, v.sBasisCartInverse, tempmat);
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
		if (!isActive())
			return;
		if (savedSliderValues == null) {
			savedSliderValues = new double[numTypes][];
			for (int t = 0; t < numTypes; t++) {
				savedSliderValues[t] = new double[modesPerType[t]];
			}
		}
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				savedSliderValues[t][m] = sliderValTM[t][m];
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
		if (!isActive())
			return;
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				String name = nameTM[t][m];
				boolean isGM1 = (name.startsWith("GM1") && !name.startsWith("GM1-"));
				if (isGM1 == isGM) {
					sliderValTM[t][m] = (2 * rval.nextFloat() - 1) * maxAmpTM[t][m];
				} else if (isGM) {
					sliderValTM[t][m] = 0;
				}
			}
		}
	}

	/**
	 * Return mode to pre-randomized settings.
	 * 
	 */
	void restoreMode() {
		if (!isActive())
			return;
		for (int t = 0; t < numTypes; t++) {
			for (int m = 0; m < modesPerType[t]; m++) {
				sliderValTM[t][m] = savedSliderValues[t][m];
			}
		}
	}

	/**
	 * Set the color of this mode's panels to their designated colors
	 */
	void colorPanels() {
		if (typePanels == null || !isActive())
			return;
		for (int t = 0; t < numTypes; t++) {
			Color c = colorT[t];
			typePanels[t].setBackground(c);
			for (int m = 0; m < modesPerType[t]; m++) {
				sliderLabelTM[t][m].setBackground(c);
//				sliderTM[t][m].setBackground(c);
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
		if (!isActive())
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
		if (!isActive())
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
					sliderTM[t][m].setValue((int) (calcAmpTM[t][m] / maxAmpTM[t][m] * sliderMax));
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
				double sval = sliderTM[t][m].getValue();
				double f = sval / sliderMaxVal;
				double max = maxAmpTM[t][m];
				sliderValTM[t][m] = max * f;
				double val = sliderValTM[t][m];
				double irval = (isIrrep ? 1 : irreps.sliderValTM[0][irrepTM[t][m]]);
				double d = val * irval * superSliderVal;
				sliderLabelTM[t][m].setText(MathUtil.varToString(d, prec, -8) + "  " + nameTM[t][m]);
				if (!isIrrep) {
//					System.out.println("mode " + type + " " + sliderTM[t][m].getName() 
//							+ " d=" + d
//							+ " f=" + f
//							+ " sval=" + sval
//							+ " val=" + val
//							+ " irval=" + irval
//							);
					sliderTM[t][m].setPointer(d / max);
				}
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
		for (int t = 0; t < n; t++) {
			for (int m = 0; m < otherMode.modesPerType[t]; m++) {
				sliderTM[t][m].setValue((int) otherMode.sliderTM[t][m].getValue());
			}
		}
	}

	/**
	 * Set colors for the different mode types.
	 * 
	 * @param simpleColor
	 */
	void setColors(int[] atomTypeUnique, int numUniques) {
		float brightness;
		if (colorT == null) {
			colorT = new Color[numTypes];
		}
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
		double[] irrepVals = irreps.sliderValTM[0];
		double[] mySliderVals = sliderValTM[0];
		int[] myIrreps = irrepTM[0];
		double[] v = new double[6];
		for (int n = 0; n < 6; n++) {
			for (int m = 0; m < count; m++) {
				v[n] += vector[m][n] * irrepVals[myIrreps[m]] * mySliderVals[m] * superSliderVal;
			}
		}
		return MathUtil.voigt2matrix(v, new double[3][3], 1);
	}
	
	@Override
	public String toString() {
		return "[Mode " + type + " count=" + count + "]";
	}


}

