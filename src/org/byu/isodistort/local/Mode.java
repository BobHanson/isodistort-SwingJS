package org.byu.isodistort.local;

import java.awt.Color;
import java.util.Random;

import org.byu.isodistort.local.Variables.Atom;
import org.byu.isodistort.local.Variables.SliderPanelGUI.IsoSlider;

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
	double[][] values;

	private double[][] savedValues;

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
			for (int m = count; --m >= 0;) {
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
	 * distortion = sum{modeCoefs[m] * irrep[t][m] * sliderValue * childFraction}
	 * 
	 * Finally, apply that distortion to the atom's parameter vector (coord,
	 * occupation, magnetic moment, rotational displacement, etc.)
	 * 
	 * @param max
	 * @param tempvec
	 * @param tempmat
	 */
	void calcDistortion(Variables v, double[][][] max, double[] tempvec, double[][] tempmat) {
		double[] irrepVals = (v.modes[IRREP] == null ? null : v.modes[IRREP].values[0]);
		double f = v.getSetChildSliderFraction(Double.NaN);
		for (int ia = 0, n = v.numAtoms; ia < n; ia++) {
			Atom a = v.atoms[ia];
			MathUtil.vecfill(delta, 0);
			int t = a.type;
			int s = a.subType;
			if (isActive()) {
				// accumulate distortion deltas
				for (int m = modesPerType[t]; --m >= 0;) {
					double d = values[t][m] * irrepVals[irrepTM[t][m]] * f;
					MathUtil.vecaddN(delta, d, a.modes[type][m], delta);
				}
			}

			// apply the distortion to the atom's vector data
			double[] v1 = a.vector1[type];
			if (v1 == null) {
				v1 = a.vector1[type] = new double[columnCount];
			}
			MathUtil.vecaddN(a.vectorBest[type], 1, delta, v1);

			// max is just for the labels
			double[] m = max[t][s];
			double d;
			switch (type) {
			case DIS:
				d = MathUtil.len3(v.childCell.toTempCartesian(delta));
				if (d > m[type])
					m[type] = d;
				break;
			case OCC:
				m[type] += v1[0] / numSubAtoms[t][s];
				break;
			case MAG:
			case ROT:
				d = MathUtil.len3(v.childCell.toTempCartesian(v1));
				if (type == 0)
					System.out.println(
							"Mode " + ia + " " + t + " " + s + " " + v1[0] + " " + v1[1] + " " + v1[2] + " " + d);
				if (d > m[type])
					m[type] = d;
				break;
			case ELL:
				d = v.childCell.getIsotropicParameter(v1);
				if (d > m[type])
					m[type] = d;
				break;
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
		if (savedValues == null) {
			savedValues = new double[numTypes][];
			for (int t = 0; t < numTypes; t++) {
				savedValues[t] = new double[modesPerType[t]];
			}
		}
		for (int t = numTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				savedValues[t][m] = values[t][m];
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
		for (int t = numTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				String name = nameTM[t][m];
				boolean isGM1 = (name.startsWith("GM1") && !name.startsWith("GM1-"));
				if (isGM1 == isGM) {
					values[t][m] = (2 * rval.nextFloat() - 1) * maxAmpTM[t][m];
				} else if (isGM) {
					values[t][m] = 0;
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
		for (int t = numTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				values[t][m] = savedValues[t][m];
			}
		}
	}

	/**
	 * Set sliders to the given value -- used for Toggle IRREP and zeroing all
	 * values.
	 * 
	 * @param n
	 */
	void setSliders(IsoSlider[][] sliders, int n) {
		if (!isActive())
			return;
		for (int t = numTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				sliders[t][m].setValue(n);
			}
		}
	}

	/**
	 * Reset all sliders to their maximum values.
	 * 
	 * @param sliderMax
	 */
	void resetSliders(IsoSlider[][] sliders, int sliderMax) {
		if (!isActive())
			return;
		for (int t = numTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				sliders[t][m].setValue((int) (calcAmpTM[t][m] / maxAmpTM[t][m] * sliderMax));
			}
		}
	}

	/**
	 * Iterate over types and modes, reading and processing their associated slider
	 * values.
	 * 
	 * @param sliders
	 * @param childFraction
	 * @param maxJSliderIntVal
	 * @param irreps
	 */
	void readSliders(IsoSlider[][] sliders, double childFraction, double maxJSliderIntVal, Mode irreps) {
		boolean isIrrep = (type == IRREP);
		boolean isAtomic = (type < MODE_ATOMIC_COUNT);
		for (int t = 0, n = (isAtomic ? numTypes : 1); t < n; t++) {
			for (int m = modesPerType[t]; --m >= 0;) {
				double max = maxAmpTM[t][m];
				double val = values[t][m] = max * sliders[t][m].getValue() / maxJSliderIntVal;
				sliders[t][m].setLabelValue(val * (isIrrep ? 1 : irreps.values[0][irrepTM[t][m]]), max, nameTM[t][m]);
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
			brightness = 0.80f;
			break;
		case OCC:
			brightness = 0.70f;
			break;
		case MAG:
			brightness = 0.60f;
			break;
		case ROT:
			brightness = 0.50f;
			break;
		case ELL:
			brightness = 0.40f;
			break;
		case STRAIN:
			colorT[0] = Color.DARK_GRAY;
			return;
		case IRREP:
			colorT[0] = new Color(0xA0A0A0); // BH a bit darker than LIGHT_GRAY C0C0C0
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
	double[][] getVoigtStrainTensor(double childFraction, Mode irreps) {
		double[] irrepVals = irreps.values[0];
		double[] mySliderVals = values[0];
		int[] myIrreps = irrepTM[0];
		double[] v = new double[6];
		for (int n = 6; --n >= 0;) {
			for (int m = count; --m >= 0;) {
				v[n] += vector[m][n] * irrepVals[myIrreps[m]] * mySliderVals[m] * childFraction;
			}
		}
		return MathUtil.voigt2matrix(v, new double[3][3], 1);
	}

	@Override
	public String toString() {
		return "[Mode " + type + " count=" + count + "]";
	}

}
