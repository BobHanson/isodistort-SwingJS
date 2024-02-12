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
	final static int STRAIN = 5;
	final static int IRREP = 6; // irreducible representations
	final static int MODE_COUNT = 7;
	
	static final Color COLOR_STRAIN = Color.DARK_GRAY;
	static final Color COLOR_IRREP = new Color(0xA0A0A0);

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
	 * [atomtype][modefortype]
	 */
	double[][] calcAmpTM;
	double[][] maxAmpTM;
	int[][] irrepTM;
	String[][] nameTM;
	boolean[][] isGM1TM;
	int[][][] isovizPtrTM;
	/**
	 * These values are before multiplying by childFraction
	 */
	double[][] valuesTM;

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

	int nTypes;
	int[] nSubTypes;
	int[][] nSubAtoms;

	Mode(int type, int nTypes, int[] nSubTypes, int[][] nSubAtoms) {
		this.nTypes = nTypes;
		this.nSubTypes = nSubTypes;
		this.nSubAtoms = nSubAtoms;

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
			nTypes = 1; // already done in Mode(), but just pointing this out here
			perType = new int[] { count };
		}
		isActive = true;
		this.count = count;
		this.modesPerType = perType;

		nameTM = new String[nTypes][];
		isGM1TM = new boolean[nTypes][];
		calcAmpTM = new double[nTypes][];
		maxAmpTM = new double[nTypes][];
		valuesTM = new double[nTypes][];
		isovizPtrTM = new int[nTypes][][];
		irrepTM = new int[nTypes][];
		for (int t = 0; t < nTypes; t++) {
			int nmodes = modesPerType[t];
			nameTM[t] = new String[nmodes];
			isGM1TM[t] = new boolean[nmodes];
			calcAmpTM[t] = new double[nmodes];
			maxAmpTM[t] = new double[nmodes];
			valuesTM[t] = new double[nmodes];
			isovizPtrTM[t] = new int[nmodes][];
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
	void calcDistortion(Variables v, double[][][] max, double[] tempvec, double[][] tempmat, double childFraction) {
		double[] irrepVals = (v.modes[IRREP] == null ? null : v.modes[IRREP].valuesTM[0]);
		for (int ia = 0, n = v.nAtoms; ia < n; ia++) {
			Atom a = v.atoms[ia];
			MathUtil.vecfill(delta, 0);
			int t = a.type;
			int s = a.subType;
			if (isActive()) {
				// accumulate distortion deltas
				for (int m = modesPerType[t]; --m >= 0;) {
					double d = valuesTM[t][m] * irrepVals[irrepTM[t][m]] * childFraction;
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
				m[type] += v1[0] / nSubAtoms[t][s];
				break;
			case MAG:
			case ROT:
				d = MathUtil.len3(v.childCell.toTempCartesian(v1));
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
			savedValues = new double[nTypes][];
			for (int t = 0; t < nTypes; t++) {
				savedValues[t] = new double[modesPerType[t]];
			}
		}
		for (int t = nTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				savedValues[t][m] = valuesTM[t][m];
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
		for (int t = nTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				valuesTM[t][m] = (isGM1TM[t][m] == isGM ? (2 * rval.nextFloat() - 1) * maxAmpTM[t][m] : 0);
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
		for (int t = nTypes; --t >= 0;) {
			for (int m = modesPerType[t]; --m >= 0;) {
				valuesTM[t][m] = savedValues[t][m];
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
		for (int t = nTypes; --t >= 0;) {
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
		for (int t = nTypes; --t >= 0;) {
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
	void readSliders(IsoSlider[][] sliders, double dddchildFraction, double maxJSliderIntVal, Mode irreps) {
		boolean isIrrep = (type == IRREP);
		for (int t = 0; t < nTypes; t++) {
			for (int m = modesPerType[t]; --m >= 0;) {
				double maxAmp = maxAmpTM[t][m];
				double f = sliders[t][m].getValue() / maxJSliderIntVal;
				double val = valuesTM[t][m] = maxAmp * f;
				double irrepValue = (isIrrep ? 1 : irreps.valuesTM[0][irrepTM[t][m]]);
				sliders[t][m].setLabelValue(val * irrepValue, nameTM[t][m]);
			}
		}
	}

	/**
	 * Set colors for the different mode types.
	 * 
	 */
	void setColors(int[] atomTypeUnique, int nUniques) {
		float brightness;
		if (colorT == null) {
			colorT = new Color[nTypes];
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
			colorT[0] = COLOR_STRAIN;
			return;
		case IRREP:
			colorT[0] = COLOR_IRREP ; // BH a bit darker than LIGHT_GRAY C0C0C0
			return;
		}
		boolean simpleColor = (atomTypeUnique != null);
		for (int t = 0; t < nTypes; t++) {
			float k = (simpleColor ? 1f * atomTypeUnique[t] / nUniques : 1f * t / nTypes);
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
		double[] irrepVals = irreps.valuesTM[0];
		double[] mySliderVals = valuesTM[0];
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

	void setModeName(int t, int s, String name) {
		nameTM[t][s] = name;
		isGM1TM[t][s] = (name.startsWith("GM1") && !name.startsWith("GM1-"));
	}

}
