package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class Variables implements ChangeListener {
	/** True initially or when a slider bar moves, false otherwise */
	public boolean isChanged = true;

	/** Applet width and height in pixels */
	public int appletWidth = 1024, appletHeight;
	/** Total number of atoms. */
	public int numAtoms;
	/** Number of different atom types */
	public int numTypes;
	/** Number of subtypes for each atom type. */
	public int[] numSubTypes;
	/** Number of atoms for specific type and subtype. */
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
	public int[][] whichAtomsBond;
	/** Total number of unique parent atom types. */
	public int numUniques;
	/**
	 * Integer index that identifies each parent atom type as one of several unique
	 * parent atom types.
	 */
	public int[] atomTypeUnique;
	/**
	 * Array of colors for slider bars, they correspond to which atoms they move.
	 */
	public Color[] color;
	/**
	 * Array of colors for scalar slider bars, they correspond to which atoms they
	 * move.
	 */
	public Color[] scalarcolor;
	/**
	 * Array of colors for magnetic slider bars, they correspond to which moments
	 * they adjust.
	 */
	public Color[] magcolor;
	/**
	 * Array of colors for rotational slider bars, they correspond to which angles
	 * they adjust.
	 */
	public Color[] rotcolor;
	/**
	 * Array of colors for ellipsoidal slider bars, they correspond to which moments
	 * they adjust.
	 */
	public Color[] ellipcolor;
	/**
	 * If true (when at least two parents have the same element type) show the
	 * simple-color checkbox.
	 */
	public boolean needSimpleColor;

	/** Original unstrained supercell lattice parameters */
	public double[] sLatt0 = new double[6];
	/** Strained supercell parameters */
	public double[] sLatt = new double[6];
	/** Original unstrained parent cell parameters */
	public double[] pLatt0 = new double[6];
	/** Strained parent cell parameters */
	public double[] pLatt = new double[6];
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
	 * Array of atom positions in unitless lattice units. [type][subtype][atom of
	 * subtype][x, y, z]
	 */
	public double[][][][] atomInitCoord, atomFinalCoord;
	/** Array of initial atom occupancies. [type][subtype][atom of subtype] */
	public double[][][] atomInitOcc, atomFinalOcc;
	/**
	 * Array of atom magnetic moment vectors in reciprocal lattice units (muB/Ang).
	 * [type][subtype][atom of subtype][mx, my, mz]
	 */
	public double[][][][] atomInitMag, atomFinalMag;
	/**
	 * Array of atom magnetic moment vectors in reciprocal lattice units (muB/Ang).
	 * [type][subtype][atom of subtype][mx, my, mz]
	 */
	public double[][][][] atomInitRot, atomFinalRot;
	/**
	 * Array of atom ellipsoid params in Angstrom-squared units.
	 * [type][subtype][atom of subtype][xx, yy, zz, yz, xz, xy]
	 */
	public double[][][][] atomInitEllip, atomFinalEllip;

	/** Number of irreps */
	public int numIrreps;
	/** Names of the irreps */
	public String[] irrepName;
	/**
	 * Boolean variable that tracks whether irrep sliders were last set to sliderMax
	 * or to zero.
	 */
	public boolean irrepSlidersOn = true;

	/** Total number of displacement modes */
	public int dispmodeNum;
	/** Number of displacement modes for each type [type] */
	public int[] dispmodePerType;
	/** Array of initial mode amplitudes. [type][mode of type] */
	public double[][] dispmodeInitAmp;
	/** Maximum amplitude of displacement modes/slider bars; [type][mode of type] */
	public double[][] dispmodeMaxAmp;
	/**
	 * Array of displacement mode vectors. [type][mode of type][subtype][atom of
	 * subtype][x, y, z]
	 */
	public double[][][][][] dispmodeVect;
	/** Array of displacement mode names; [type][mode] */
	public String[][] dispmodeName;
	/** Tells which irrep the displacive mode corresponds to */
	public int[][] dispmodeIrrep;

	/** Total number of scalar modes */
	public int scalarmodeNum;
	/** Number of scalar modes for each type [type] */
	public int[] scalarmodePerType;
	/** Array of initial scalar mode amplitudes. [type][mode of type] */
	public double[][] scalarmodeInitAmp;
	/** Maximum amplitude of scalar modes/slider bars; [type][mode of type] */
	public double[][] scalarmodeMaxAmp;
	/**
	 * Array of scalar mode magnitudes. [type][mode of type][subtype][atom of
	 * subtype]
	 */
	public double[][][][] scalarmodeVect;
	/** Array of scalar mode names; [type][mode] */
	public String[][] scalarmodeName;
	/** Tells which irrep the scalar mode corresponds to */
	public int[][] scalarmodeIrrep;

	/** Total number of magnetic modes */
	public int magmodeNum;
	/** Number of magnetic modes for each type [type] */
	public int[] magmodePerType;
	/** Array of initial magnetic mode amplitudes. [type][mode of type] */
	public double[][] magmodeInitAmp;
	/** Maximum amplitude of magnetic modes/slider bars; [type][mode of type] */
	public double[][] magmodeMaxAmp;
	/**
	 * Array of magnetic mode vectors. [type][mode of type][subtype][atom of
	 * subtype][x, y, z]
	 */
	public double[][][][][] magmodeVect;
	/** Array of magnetic mode names; [type][mode] */
	public String[][] magmodeName;
	/** Tells which irrep the magnetic mode corresponds to */
	public int[][] magmodeIrrep;

	/** Total number of rotational modes */
	public int rotmodeNum;
	/** Number of rotational modes for each type [type] */
	public int[] rotmodePerType;
	/** Array of initial rotational mode amplitudes. [type][mode of type] */
	public double[][] rotmodeInitAmp;
	/** Maximum amplitude of rotational modes/slider bars; [type][mode of type] */
	public double[][] rotmodeMaxAmp;
	/**
	 * Array of rotational mode vectors. [type][mode of type][subtype][atom of
	 * subtype][x, y, z]
	 */
	public double[][][][][] rotmodeVect;
	/** Array of rotational mode names; [type][mode] */
	public String[][] rotmodeName;
	/** Tells which irrep the rotational mode corresponds to */
	public int[][] rotmodeIrrep;

	/** Total number of ellipsoidal modes */
	public int ellipmodeNum;
	/** Number of ellipsoidal modes for each type [type] */
	public int[] ellipmodePerType;
	/** Array of initial ellipsoidal mode amplitudes. [type][mode of type] */
	public double[][] ellipmodeInitAmp;
	/** Maximum amplitude of ellipsoidal modes/slider bars; [type][mode of type] */
	public double[][] ellipmodeMaxAmp;
	/**
	 * Array of ellipsoidal mode vectors. [type][mode of type][subtype][atom of
	 * subtype][xx, yy, zz, yz, xz, xy]
	 */
	public double[][][][][] ellipmodeVect;
	/** Array of ellipsoidal mode names; [type][mode] */
	public String[][] ellipmodeName;
	/** Tells which irrep the ellipsoidal mode corresponds to */
	public int[][] ellipmodeIrrep;

	/** number of strain modes */
	public int strainmodeNum;
	/** Array of strain mode amplitudes */
	public double[] strainmodeInitAmp;
	/** Array of strain mode max amplitudes */
	public double[] strainmodeMaxAmp;
	/** Array of strain mode vectors */
	public double[][] strainmodeVect;
	/** Array of strain mode names */
	public String[] strainmodeName;
	/** Tells which irrep the strain mode corresponds to */
	public int[] strainmodeIrrep;

//	Global variables that are part of the Panel.
	/**
	 * Master (top most) slider bar controls all slider bars for superpositioning of
	 * modes.
	 */
	public JSlider masterSlider;
	/** Array of displacement mode slider bars for animating displacement modes. */
	public JSlider[][] dispmodeSlider;
	/** Array of scalar mode slider bars for animating displacement modes. */
	public JSlider[][] scalarmodeSlider;
	/** Array of magnetic mode slider bars for animating magnetic modes. */
	public JSlider[][] magmodeSlider;
	/** Array of rotational mode slider bars for animating magnetic modes. */
	public JSlider[][] rotmodeSlider;
	/** Array of magnetic mode slider bars for animating ellipsoidal modes. */
	public JSlider[][] ellipmodeSlider;
	/** Array of strain mode slider bars. */
	public JSlider[] strainmodeSlider;
	/** Array of irrep mode slider bars. */
	public JSlider[] irrepmodeSlider;

	/** The master slider bar value. */
	public double masterSliderVal;
	/** Array of displacement mode slider bar values. */
	public double[][] dispmodeSliderVal;
	/** Array of scalar mode slider bar values. */
	public double[][] scalarmodeSliderVal;
	/** Array of magnetic mode slider bar values. */
	public double[][] magmodeSliderVal;
	/** Array of rotational mode slider bar values. */
	public double[][] rotmodeSliderVal;
	/** Array of ellipsoidal mode slider bar values. */
	public double[][] ellipmodeSliderVal;
	/** Array of strain slider bar values. */
	public double[] strainmodeSliderVal;
	/** Array of irrep slider bar values. */
	public double[] irrepmodeSliderVal;

	/** Label for master slider bar. */
	public JLabel masterSliderLabel;
	/**
	 * Array of labels for displacement mode slider bars. Label specifies name of
	 * mode and mode bar displacement.
	 */
	public JLabel[][] dispmodeSliderLabel;
	/**
	 * Array of labels for scalar mode slider bars. Label specifies name of mode and
	 * mode bar displacement.
	 */
	public JLabel[][] scalarmodeSliderLabel;
	/**
	 * Array of labels for magnetic mode slider bars. Label specifies name of mode
	 * and mode bar displacement.
	 */
	public JLabel[][] magmodeSliderLabel;
	/**
	 * Array of labels for rotational mode slider bars. Label specifies name of mode
	 * and mode bar displacement.
	 */
	public JLabel[][] rotmodeSliderLabel;
	/**
	 * Array of labels for ellipsoidal mode slider bars. Label specifies name of
	 * mode and mode bar displacement.
	 */
	public JLabel[][] ellipmodeSliderLabel;
	/**
	 * Array of labels for strain mode slider bars. Label specifies name of mode and
	 * mode bar displacement.
	 */
	public JLabel[] strainmodeSliderLabel;
	/**
	 * Array of labels for irrep mode slider bars. Label specifies name of mode and
	 * mode bar displacement.
	 */
	public JLabel[] irrepmodeSliderLabel;
	/** Array of labels for supercell lattice parameters */
	JLabel[] sLattLabel = new JLabel[6];
	/** Array of labels for parent lattice parameters */
	JLabel[] pLattLabel = new JLabel[6];
	/** names for parent and super cell parameter titles */
	JLabel parentLabel, superLabel;
	/** Array of labels for atom types */
	public JLabel typeLabel[];
	/** subTypeLabels */
	public JLabel[][] subTypeLabel;
	/** Array of checkboxes -- one for each atomic subtype */
	public JCheckBox[][] subTypeBox;

	/** Panel which holds master slider bar and it's label; added scrollPanel */
	public JPanel masterSliderPanel;
	/** Panel which holds the parent atom type */
	public JPanel typeNamePanel[];
	/**
	 * Panel which holds the occupancy check boxes for each atom subtype associated
	 * with a type
	 */
	public JPanel typeDataPanel[];
	/**
	 * Temporary Panel which holds a slider bar and it's label; added to scrollPanel
	 */
	public JPanel dispPanel[], scalarPanel[], magPanel[], rotPanel[], ellipPanel[], strainPanel, irrepPanel;

	/**
	 * Maximum slider bar value. The slider bar only takes integer values. So the
	 * sliderMax is the number of notches and therefore the precision of the slider
	 * bar. More notches make for more precise slider movements but slow the
	 * rendering a lot since it renders at every notch
	 */
	public int sliderMax = 100;
	/** double version of sliderMax */
	public double sliderMaxVal = sliderMax;

	private boolean diffraction;

	private IsoApp app;

	public String isoversion;

	public Variables(IsoApp app, String dataString, boolean isDiffraction) {
		this.app = app;
		diffraction = isDiffraction;
		parseDataTags(dataString);
		identifyUniqueAtoms();

		color = new Color[numTypes];// array of colors for slider bars
		scalarcolor = new Color[numTypes];// array of colors for scalar slider bars
		magcolor = new Color[numTypes]; // array of colors for magnetic slider bars
		rotcolor = new Color[numTypes]; // array of colors for magnetic slider bars
		ellipcolor = new Color[numTypes]; // array of colors for ellipsoidal slider bars
		setColors(false);
		

		initPanels(app.sliderPanel, app.controlPanel);
	} // end the constructor

	/**
	 * Listens for moving slider bars.
	 */
	public void stateChanged(ChangeEvent e)// called when a slider bar is moved
	{
		isChanged = true;
		app.updateDisplay();
	}

	/**
	 * Parses the data string that is a tag-based format
	 * 
	 * @param dataString
	 */
	private void parseDataTags(String dataString) {
		double temp1, temp2;
		ArrayList<String> currentData = null;
		String currentTag = "";
		Map<String, ArrayList<String>> myMap = getDataMap(dataString);
		isoversion = myMap.get("isoversion").get(0);
		// find parentcell parameters
		currentTag = "parentcell";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 6) {
				parseError(currentTag, 1);
			} else {
				for (int n = 0; n < 6; n++)
					pLatt0[n] = Double.parseDouble((String) currentData.get(n));
			}
		} else // Crash if parentcell and supercell info are both absent
		{
			parseError(currentTag, 3);
		}

		// find parentCell origin within the superCell basis.
		currentTag = "parentorigin";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 3) {
				parseError(currentTag, 1);
			} else {
				for (int n = 0; n < 3; n++)
					pOriginUnitless[n] = Double.parseDouble((String) currentData.get(n));
			}
		} else // Crash if the parent origin information is missing
		{
			parseError(currentTag, 3);
		}

		// find parentCell parameters within superCell basis.
		currentTag = "parentbasis";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 9) {
				parseError(currentTag, 1);
			} else {
				for (int j = 0; j < 3; j++)
					for (int i = 0; i < 3; i++) {
						Tmat[j][i] = Double.parseDouble((String) currentData.get(3 * j + i));
						Vec.mattranspose(Tmat, TmatTranspose);
						Vec.matinverse(TmatTranspose, TmatInverseTranspose);
					}
			}
		} else // Crash if the parent origin information is missing
		{
			parseError(currentTag, 3);
		}

		// find maximum radius of atoms
		currentTag = "rhombparentsetting";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				isRhombParentSetting = Boolean.parseBoolean((String) currentData.get(0));
			}
		} else // Default assumption
		{
			isRhombParentSetting = false;
		}

		// find maximum radius of atoms
		currentTag = "atommaxradius";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				atomMaxRadius = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set atom max radius to default value
		{
			atomMaxRadius = 0.4;
		}

		// find applet width
		int n = 0;
		currentTag = "appletwidth";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				// ignore
			} else {
				try {
					n = Integer.parseInt((String) currentData.get(0));
				} catch (Exception e) {
					// ignore
				}
			}
		}
		if (n >= 500 && n <= 5000)
			appletWidth = n;
		appletHeight = (int) Math.round((double) appletWidth / 1.6);

		// find angstromspermagneton
		currentTag = "angstromspermagneton";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				angstromsPerMagneton = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set atom max radius to default value
		{
			angstromsPerMagneton = 0.5;
		}

		// find angstromspermagneton
		currentTag = "angstromsperradian";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				angstromsPerRadian = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set atom max radius to default value
		{
			angstromsPerRadian = 0.5;
		}

		// find defaultuiso
		currentTag = "defaultuiso";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				defaultUiso = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set atom max radius to default value
		{
			defaultUiso = Math.pow(atomMaxRadius / 2.0, 2);
		}

		// find maximum length of bonds that can be displayed
		currentTag = "maxbondlength";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				maxBondLength = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set bond max radius to default value
		{
			maxBondLength = 2.5;
		}

		// find minimum atomic occupancy for which bonds should be displayed
		currentTag = "minbondocc";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() != 1) {
				parseError(currentTag, 1);
			} else {
				minBondOcc = Double.parseDouble((String) currentData.get(0));
			}
		} else // Set min bond occupancy to default value
		{
			minBondOcc = 0.5;
		}

		// find atom types
		currentTag = "atomtypelist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 3;
			currentData = myMap.get(currentTag);
			numTypes = currentData.size() / dataPerRow;
			if (currentData.size() != numTypes * dataPerRow) // Make sure it divided evenly
			{
				parseError(currentTag, 1);
			} else {
				atomTypeName = new String[numTypes];
				atomTypeSymbol = new String[numTypes];
				for (int i = 0; i < numTypes; i++) {
					if (Integer.parseInt((String) currentData.get(dataPerRow * i)) != i + 1)
						parseError(currentTag, 2);
					atomTypeName[i] = (String) currentData.get(dataPerRow * i + 1);
					atomTypeSymbol[i] = (String) currentData.get(dataPerRow * i + 2);
				}
			}
		} else {
			parseError(currentTag, 3);
		}

		numSubTypes = new int[numTypes];
		numSubAtoms = new int[numTypes][];
		atomInitCoord = new double[numTypes][][][];
		atomFinalCoord = new double[numTypes][][][];
		atomInitOcc = new double[numTypes][][];
		atomFinalOcc = new double[numTypes][][];
		atomInitMag = new double[numTypes][][][];
		atomFinalMag = new double[numTypes][][][];
		atomInitRot = new double[numTypes][][][];
		atomFinalRot = new double[numTypes][][][];
		atomInitEllip = new double[numTypes][][][];
		atomFinalEllip = new double[numTypes][][][];

		// find atom subtypes
		currentTag = "atomsubtypelist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 3;
			currentData = myMap.get(currentTag);
			int numSubTypeEntries = currentData.size() / dataPerRow;
			if (currentData.size() != numSubTypeEntries * dataPerRow) // Make sure it divided evenly
			{
				parseError(currentTag, 1);
			} else {
				// Find number of subtypes for each type
				int curType = 0;
				int curSubType = 0;
				numSubTypes[0] = 0;
				for (int i = 0; i < numSubTypeEntries; i++) {
					if (Integer.parseInt((String) currentData.get(dataPerRow * i)) != curType) {
						curType = curType + 1;
						curSubType = 0;
						if (Integer.parseInt((String) currentData.get(dataPerRow * i)) != curType)
							parseError("The atom types in the atom subtype list are out of order", 2);
					}
					if (Integer.parseInt((String) currentData.get(dataPerRow * i + 1)) != curSubType) {
						numSubTypes[curType - 1] = numSubTypes[curType - 1] + 1;
						curSubType += 1;
						if (Integer.parseInt((String) currentData.get(dataPerRow * i + 1)) != curSubType)
							parseError("The subtypes in the atom subtype list are out of order", 2);
					}
				}

				// Assign the subtype names
				int curEntry = 0;
				subTypeName = new String[numTypes][];
				for (int t = 0; t < numTypes; t++) {
					subTypeName[t] = new String[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) {
//						subTypeName[t][s]=atomTypeName[t]+"_"+(s+1);
						subTypeName[t][s] = (String) currentData.get(dataPerRow * curEntry + 2);
						curEntry = curEntry + 1;
					}
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

		// find atomic coordinates of parent
		currentTag = "atomcoordlist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			int dataPerRow = 6;
			int ndata = currentData.size();
			numAtoms = ndata / dataPerRow;
			int dataPerRowOld = 10;
			int numAtomsOld = ndata / dataPerRowOld;
			if (numAtomsOld * dataPerRowOld == ndata)
				try {
					// If this item isn't an integer, the old data format is used.
					Integer.parseInt((String) currentData.get(dataPerRow));
				} catch (NumberFormatException e) {
					numAtoms = numAtomsOld; // backwards compatibility
					dataPerRow = dataPerRowOld; // backwards compatibility
//				System.out.println("old atom coords");
				}
			if (currentData.size() != numAtoms * dataPerRow) // Make sure it divided evenly
			{
				parseError(currentTag, 1);
			} else {
				// Find number of subatoms for each subtype
				for (int t = 0; t < numTypes; t++) {
					numSubAtoms[t] = new int[numSubTypes[t]];
					for (int s = 0; s < numSubTypes[t]; s++) // Zero out the array
						numSubAtoms[t][s] = 0;
				}

				int curType = 0;
				int curSubType = 0;
				for (int i = 0; i < numAtoms; i++) {
					if (Integer.parseInt((String) currentData.get(dataPerRow * i)) != curType) {
						curType += 1;
						curSubType = 1;
					}
					if (Integer.parseInt((String) currentData.get(dataPerRow * i + 1)) != curSubType) {
						curSubType += 1;
					}
					numSubAtoms[curType - 1][curSubType - 1] = numSubAtoms[curType - 1][curSubType - 1] + 1;
				}

				for (int t = 0; t < numTypes; t++)// iterate over all the types
				{
					atomInitCoord[t] = new double[numSubTypes[t]][][];
					atomFinalCoord[t] = new double[numSubTypes[t]][][];
					atomInitOcc[t] = new double[numSubTypes[t]][];
					atomFinalOcc[t] = new double[numSubTypes[t]][];
					atomInitMag[t] = new double[numSubTypes[t]][][];
					atomFinalMag[t] = new double[numSubTypes[t]][][];
					atomInitRot[t] = new double[numSubTypes[t]][][];
					atomFinalRot[t] = new double[numSubTypes[t]][][];
					atomInitEllip[t] = new double[numSubTypes[t]][][];
					atomFinalEllip[t] = new double[numSubTypes[t]][][];
					for (int s = 0; s < numSubTypes[t]; s++) {
						atomInitCoord[t][s] = new double[numSubAtoms[t][s]][3];
						atomFinalCoord[t][s] = new double[numSubAtoms[t][s]][3];
						atomInitOcc[t][s] = new double[numSubAtoms[t][s]];
						atomFinalOcc[t][s] = new double[numSubAtoms[t][s]];
						atomInitMag[t][s] = new double[numSubAtoms[t][s]][3];
						atomFinalMag[t][s] = new double[numSubAtoms[t][s]][3];
						atomInitRot[t][s] = new double[numSubAtoms[t][s]][3];
						atomFinalRot[t][s] = new double[numSubAtoms[t][s]][3];
						atomInitEllip[t][s] = new double[numSubAtoms[t][s]][6];
						atomFinalEllip[t][s] = new double[numSubAtoms[t][s]][6];
					}
				}

				// input the unitless atomic coords [type][subtype][atom
				// number][x,y,z][occ][mx,my,mz]
				int q = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							for (int i = 0; i < 3; i++)
								atomInitCoord[t][s][a][i] = Double
										.parseDouble((String) currentData.get(q * dataPerRow + 3 + i));
							q++;
						}
			}
		} else {
			parseError(currentTag, 3);
		}

		// find atomic occupancies of parent
		currentTag = "atomocclist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 1;
			currentData = myMap.get(currentTag);
			if (currentData.size() != numAtoms * dataPerRow) // Make sure it's divided evenly and that numAtoms hasn't
																// changed.
			{
				parseError(currentTag, 1);
			} else {
				// input the occupancy parameters
				int q = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							atomInitOcc[t][s][a] = Double.parseDouble((String) currentData.get(q * dataPerRow));
							q++;
						}

			}
		} else {
			// Set default occupancies to 1.0.
			for (int t = 0; t < numTypes; t++)// iterate over all the types
				for (int s = 0; s < numSubTypes[t]; s++)
					for (int a = 0; a < numSubAtoms[t][s]; a++)
						atomInitOcc[t][s][a] = 1.0;
		}

		// find atomic magnetic moments of parent
		currentTag = "atommaglist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 3;
			currentData = myMap.get(currentTag);
			if (currentData.size() != numAtoms * dataPerRow) // Make sure it's divided evenly and that numAtoms hasn't
																// changed.
			{
				parseError(currentTag, 1);
			} else {
				// input the magnetic-moment parameters
				int q = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							for (int i = 0; i < 3; i++)
								atomInitMag[t][s][a][i] = Double
										.parseDouble((String) currentData.get(q * dataPerRow + i));
							q++;
						}
			}
		} else {
			// Set default magnetic moments to zero.
			for (int t = 0; t < numTypes; t++)// iterate over all the types
				for (int s = 0; s < numSubTypes[t]; s++)
					for (int a = 0; a < numSubAtoms[t][s]; a++)
						for (int i = 0; i < 3; i++)
							atomInitMag[t][s][a][i] = 0.0;
		}

		// find atomic rotations of parent
		currentTag = "atomrotlist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 3;
			currentData = myMap.get(currentTag);
			if (currentData.size() != numAtoms * dataPerRow) // Make sure it's divided evenly and that numAtoms hasn't
																// changed.
			{
				parseError(currentTag, 1);
			} else {
				// input the rotation-angle parameters
				int q = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							for (int i = 0; i < 3; i++)
								atomInitRot[t][s][a][i] = Double
										.parseDouble((String) currentData.get(q * dataPerRow + i));
							q++;
						}
			}
		} else {
			// Set default rotation angles to zero.
			for (int t = 0; t < numTypes; t++)// iterate over all the types
				for (int s = 0; s < numSubTypes[t]; s++)
					for (int a = 0; a < numSubAtoms[t][s]; a++)
						for (int i = 0; i < 3; i++)
							atomInitRot[t][s][a][i] = 0.0;
		}

		// find atomic adp ellipsoids of parent
		currentTag = "atomelplist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 6;
			currentData = myMap.get(currentTag);
			if (currentData.size() != numAtoms * dataPerRow) // Make sure it's divided evenly and that numAtoms hasn't
																// changed.
			{
				parseError(currentTag, 1);
			} else {
				// input the adp parameters [type][subtype][atom number][xx,yy,zz,yz,xz,xy]
				int q = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							for (int i = 0; i < 6; i++)
								atomInitEllip[t][s][a][i] = Double
										.parseDouble((String) currentData.get(q * dataPerRow + i));
							q++;
						}
			}
		} else {
			// Set default ellipsoids to simple spheres.
			for (int t = 0; t < numTypes; t++)// iterate over all the types
				for (int s = 0; s < numSubTypes[t]; s++)
					for (int a = 0; a < numSubAtoms[t][s]; a++)
						for (int i = 0; i < 3; i++) {
							atomInitEllip[t][s][a][i] = defaultUiso;
							atomInitEllip[t][s][a][3 + i] = 0;
						}
		}

		// find bonds
		currentTag = "bondlist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			numBonds = currentData.size() / 6;
			if (currentData.size() != numBonds * 6) // Make sure it divided evenly
			{
				parseError(currentTag, 1);
			}

			whichAtomsBond = new int[numBonds][2];
			int[][][] detailedWhichAtomsBond = new int[numBonds][2][3];
			for (int i = 0; i < numBonds; i++) // Read in the bond information
			{
				detailedWhichAtomsBond[i][0][0] = Integer.parseInt((String) currentData.get(6 * i));
				detailedWhichAtomsBond[i][0][1] = Integer.parseInt((String) currentData.get(6 * i + 1));
				detailedWhichAtomsBond[i][0][2] = Integer.parseInt((String) currentData.get(6 * i + 2));
				detailedWhichAtomsBond[i][1][0] = Integer.parseInt((String) currentData.get(6 * i + 3));
				detailedWhichAtomsBond[i][1][1] = Integer.parseInt((String) currentData.get(6 * i + 4));
				detailedWhichAtomsBond[i][1][2] = Integer.parseInt((String) currentData.get(6 * i + 5));
			}

			// Calculate the overall atom numbers (atom1, atom2) associated with each bond.
			for (int b = 0; b < numBonds; b++)// iterate over all the types
			{
				int atomNum = 0;
				for (int t = 0; t < numTypes; t++)// iterate over all the types
					for (int s = 0; s < numSubTypes[t]; s++)
						for (int a = 0; a < numSubAtoms[t][s]; a++) {
							for (int p = 0; p < 2; p++)
								if ((detailedWhichAtomsBond[b][p][0] == t + 1)
										&& (detailedWhichAtomsBond[b][p][1] == s + 1)
										&& (detailedWhichAtomsBond[b][p][2] == a + 1))
									whichAtomsBond[b][p] = atomNum;
							atomNum += 1;
						}
			}
		} else {
			// There are no bonds. Initialize the array to length zero
			whichAtomsBond = new int[0][];
			numBonds = 0;
		}

		// find irreps
		currentTag = "irreplist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);
			if (currentData.size() % 2 != 0) {
				parseError(currentTag, 1);
			} else {
				numIrreps = currentData.size() / 2;
				irrepName = new String[numIrreps];
				for (int i = 0; i < numIrreps; i++) {
					if (Integer.parseInt((String) currentData.get(2 * i)) != (i + 1))
						parseError("Error: irreps are not passed in ascending order.", 2);
					irrepName[i] = (String) currentData.get(2 * i + 1);
				}
			}
		} else // no irreps
		{
			numIrreps = 0;
		}

		// initialize displacive mode arrays
		int modeTracker[] = new int[numTypes];
		dispmodeNum = 0;
		dispmodePerType = new int[numTypes];
		dispmodeInitAmp = new double[numTypes][];
		dispmodeMaxAmp = new double[numTypes][];
		dispmodeName = new String[numTypes][];
		dispmodeIrrep = new int[numTypes][];
		dispmodeVect = new double[numTypes][][][][];
		for (int i = 0; i < numTypes; i++) {
			dispmodePerType[i] = 0;
		}

		// find displacive modes
		currentTag = "displacivemodelist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);

			int counter = 0;

			// Run through the data once and determine the number of displacive modes
			while (counter < currentData.size()) {
				int atomType = Integer.parseInt((String) currentData.get(counter)) - 1;
				// Counter should be pointing at atom type number
				dispmodePerType[atomType]++;
				dispmodeNum++;
				// Point counter to next atom type number
				counter += 6;
				int atoms = 0;
				for (int i = 0; i < numSubTypes[atomType]; i++)
					atoms += numSubAtoms[atomType][i];
				counter = counter + 3 * atoms;
			}

			// Initialize needed arrays
			for (int t = 0; t < numTypes; t++) {
				dispmodeInitAmp[t] = new double[dispmodePerType[t]];
				dispmodeMaxAmp[t] = new double[dispmodePerType[t]];
				dispmodeName[t] = new String[dispmodePerType[t]];
				dispmodeIrrep[t] = new int[dispmodePerType[t]];
				dispmodeVect[t] = new double[dispmodePerType[t]][][][];
				for (int m = 0; m < dispmodePerType[t]; m++) {
					dispmodeVect[t][m] = new double[numSubTypes[t]][][];
					for (int s = 0; s < numSubTypes[t]; s++)
						dispmodeVect[t][m][s] = new double[numSubAtoms[t][s]][3];
				}
				modeTracker[t] = 0;
			}

			// input displacement mode array [atom type][mode number of that type][atom
			// number of that type][x,y,z]
			counter = 0;
			for (int m = 0; m < dispmodeNum; m++)// iterate over modes
			{
				int thisType = Integer.parseInt((String) currentData.get(counter)) - 1;// first number is the type
																						// number
				counter++;
				if (modeTracker[thisType] + 1 != Integer.parseInt((String) currentData.get(counter)))
					parseError("The displacive modes are not given in ascending order", 2);
				counter++;
				dispmodeInitAmp[thisType][modeTracker[thisType]] = Double
						.parseDouble((String) currentData.get(counter));// amplitude of mode
				counter++;
				dispmodeMaxAmp[thisType][modeTracker[thisType]] = Double.parseDouble((String) currentData.get(counter));// maximum
																														// amplitude
																														// of
																														// mode
				counter++;
				dispmodeIrrep[thisType][modeTracker[thisType]] = Integer.parseInt((String) currentData.get(counter))
						- 1;// irrep of mode
				counter++;
				dispmodeName[thisType][modeTracker[thisType]] = (String) currentData.get(counter);// name of first mode
																									// is made of four
																									// chunks
				counter++;
				for (int s = 0; s < numSubTypes[thisType]; s++)
					for (int a = 0; a < numSubAtoms[thisType][s]; a++)// iterate through the atoms
						for (int i = 0; i < 3; i++) {
							dispmodeVect[thisType][modeTracker[thisType]][s][a][i] = Double
									.parseDouble((String) currentData.get(counter));
							counter++;
						}
				modeTracker[thisType] += 1;
			}
		} else {
			// No displacive modes.
		}

		scalarmodeNum = 0;
		scalarmodePerType = new int[numTypes];
		scalarmodeInitAmp = new double[numTypes][];
		scalarmodeMaxAmp = new double[numTypes][];
		scalarmodeName = new String[numTypes][];
		scalarmodeIrrep = new int[numTypes][];
		scalarmodeVect = new double[numTypes][][][];

		for (int i = 0; i < numTypes; i++) {
			scalarmodePerType[i] = 0;
		}

		// find scalar modes
		currentTag = "scalarmodelist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);

			int counter = 0;

			// Run through the data once and determine the number of displacive modes
			while (counter < currentData.size()) {
				int atomType = Integer.parseInt((String) currentData.get(counter)) - 1;
				// Counter should be pointing at atom type number
				scalarmodePerType[atomType]++;
				scalarmodeNum++;
				// Point counter to next atom type number
				counter += 6;
				int atoms = 0;
				for (int i = 0; i < numSubTypes[atomType]; i++)
					atoms += numSubAtoms[atomType][i];
				counter = counter + atoms;
			}

			// Initialize needed arrays
			for (int t = 0; t < numTypes; t++) {
				scalarmodeInitAmp[t] = new double[scalarmodePerType[t]];
				scalarmodeMaxAmp[t] = new double[scalarmodePerType[t]];
				scalarmodeIrrep[t] = new int[scalarmodePerType[t]];
				scalarmodeName[t] = new String[scalarmodePerType[t]];
				scalarmodeVect[t] = new double[scalarmodePerType[t]][][];
				for (int m = 0; m < scalarmodePerType[t]; m++) {
					scalarmodeVect[t][m] = new double[numSubTypes[t]][];
					for (int s = 0; s < numSubTypes[t]; s++)
						scalarmodeVect[t][m][s] = new double[numSubAtoms[t][s]];
				}
				modeTracker[t] = 0;
			}

			// input displacement mode array [atom type][mode number of that type][atom
			// number of that type][x,y,z]
			counter = 0;
			for (int m = 0; m < scalarmodeNum; m++)// iterate over modes
			{
				int thisType = Integer.parseInt((String) currentData.get(counter)) - 1;// first number is the type
																						// number
				counter++;
				if (modeTracker[thisType] + 1 != Integer.parseInt((String) currentData.get(counter)))
					parseError("The scalar modes are not given in order", 2);
				counter++;
				scalarmodeInitAmp[thisType][modeTracker[thisType]] = Double
						.parseDouble((String) currentData.get(counter));// amplitude of mode
				counter++;
				scalarmodeMaxAmp[thisType][modeTracker[thisType]] = Double
						.parseDouble((String) currentData.get(counter));// maximum amplitude of mode
				counter++;
				scalarmodeIrrep[thisType][modeTracker[thisType]] = Integer.parseInt((String) currentData.get(counter))
						- 1;// irrep of mode
				counter++;
				scalarmodeName[thisType][modeTracker[thisType]] = (String) currentData.get(counter);// name of first
																									// mode is made of
																									// four chunks
				counter++;
				for (int s = 0; s < numSubTypes[thisType]; s++)
					for (int a = 0; a < numSubAtoms[thisType][s]; a++)// iterate through the atoms
					{
						scalarmodeVect[thisType][modeTracker[thisType]][s][a] = Double
								.parseDouble((String) currentData.get(counter));
						counter++;
					}
				modeTracker[thisType] += 1;
			}
		} else {
			// No scalar modes.
		}

		magmodeNum = 0;
		magmodePerType = new int[numTypes];
		magmodeInitAmp = new double[numTypes][];
		magmodeMaxAmp = new double[numTypes][];
		magmodeIrrep = new int[numTypes][];
		magmodeName = new String[numTypes][];
		magmodeVect = new double[numTypes][][][][];
		for (int i = 0; i < numTypes; i++) {
			magmodePerType[i] = 0;
		}

		// find magnetic modes
		currentTag = "magneticmodelist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);

			int counter = 0;

			// Run through the data once and determine the number of magnetic modes
			while (counter < currentData.size()) {
				int atomType = Integer.parseInt((String) currentData.get(counter)) - 1;
				// Counter should be pointing at atom type number
				magmodePerType[atomType]++;
				magmodeNum++;
				// Point counter to next atom type number
				counter += 6;
				int atoms = 0;
				for (int i = 0; i < numSubTypes[atomType]; i++)
					atoms += numSubAtoms[atomType][i];
				counter = counter + 3 * atoms;
			}

			// Initialize needed arrays
			for (int t = 0; t < numTypes; t++) {
				magmodeIrrep[t] = new int[magmodePerType[t]];
				magmodeInitAmp[t] = new double[magmodePerType[t]];
				magmodeMaxAmp[t] = new double[magmodePerType[t]];
				magmodeName[t] = new String[magmodePerType[t]];
				magmodeVect[t] = new double[magmodePerType[t]][][][];
				for (int m = 0; m < magmodePerType[t]; m++) {
					magmodeVect[t][m] = new double[numSubTypes[t]][][];
					for (int s = 0; s < numSubTypes[t]; s++)
						magmodeVect[t][m][s] = new double[numSubAtoms[t][s]][3];
				}
				modeTracker[t] = 0;
			}

			// input magnetic mode array [atom type][mode number of that type][atom number
			// of that type][mx,my,mz]
			counter = 0;
			for (int m = 0; m < magmodeNum; m++)// iterate over modes
			{
				int thisType = Integer.parseInt((String) currentData.get(counter)) - 1;// first number is the type
																						// number
				counter++;
				if (modeTracker[thisType] + 1 != Integer.parseInt((String) currentData.get(counter)))
					parseError("The magnetic modes are not given in ascending order", 2);
				counter++;
				magmodeInitAmp[thisType][modeTracker[thisType]] = Double.parseDouble((String) currentData.get(counter));// amplitude
																														// of
																														// mode
				counter++;
				magmodeMaxAmp[thisType][modeTracker[thisType]] = Double.parseDouble((String) currentData.get(counter));// maximum
																														// amplitude
																														// of
																														// mode
				counter++;
				magmodeIrrep[thisType][modeTracker[thisType]] = Integer.parseInt((String) currentData.get(counter)) - 1;// irrep
																														// of
																														// mode
				counter++;
				magmodeName[thisType][modeTracker[thisType]] = (String) currentData.get(counter);// name of first mode
																									// is made of four
																									// chunks
				counter++;
				for (int s = 0; s < numSubTypes[thisType]; s++)
					for (int a = 0; a < numSubAtoms[thisType][s]; a++)// iterate through the atoms
						for (int i = 0; i < 3; i++) {
							magmodeVect[thisType][modeTracker[thisType]][s][a][i] = Double
									.parseDouble((String) currentData.get(counter));
							counter++;
						}
				modeTracker[thisType] += 1;
			}
		} else {
			// No magnetic modes.
		}

		rotmodeNum = 0;
		rotmodePerType = new int[numTypes];
		rotmodeInitAmp = new double[numTypes][];
		rotmodeMaxAmp = new double[numTypes][];
		rotmodeIrrep = new int[numTypes][];
		rotmodeName = new String[numTypes][];
		rotmodeVect = new double[numTypes][][][][];
		for (int i = 0; i < numTypes; i++) {
			rotmodePerType[i] = 0;
		}

		// find rotational modes
		currentTag = "rotationalmodelist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);

			int counter = 0;

			// Run through the data once and determine the number of rotational modes
			while (counter < currentData.size()) {
				int atomType = Integer.parseInt((String) currentData.get(counter)) - 1;
				// Counter should be pointing at atom type number
				rotmodePerType[atomType]++;
				rotmodeNum++;
				// Point counter to next atom type number
				counter += 6;
				int atoms = 0;
				for (int i = 0; i < numSubTypes[atomType]; i++)
					atoms += numSubAtoms[atomType][i];
				counter = counter + 3 * atoms;
			}

			// Initialize needed arrays
			for (int t = 0; t < numTypes; t++) {
				rotmodeIrrep[t] = new int[rotmodePerType[t]];
				rotmodeInitAmp[t] = new double[rotmodePerType[t]];
				rotmodeMaxAmp[t] = new double[rotmodePerType[t]];
				rotmodeName[t] = new String[rotmodePerType[t]];
				rotmodeVect[t] = new double[rotmodePerType[t]][][][];
				for (int m = 0; m < rotmodePerType[t]; m++) {
					rotmodeVect[t][m] = new double[numSubTypes[t]][][];
					for (int s = 0; s < numSubTypes[t]; s++)
						rotmodeVect[t][m][s] = new double[numSubAtoms[t][s]][3];
				}
				modeTracker[t] = 0;
			}

			// input rotational mode array [atom type][mode number of that type][atom number
			// of that type][mx,my,mz]
			counter = 0;
			for (int m = 0; m < rotmodeNum; m++)// iterate over modes
			{
				int thisType = Integer.parseInt((String) currentData.get(counter)) - 1;// first number is the type
																						// number
				counter++;
				if (modeTracker[thisType] + 1 != Integer.parseInt((String) currentData.get(counter)))
					parseError("The rotational modes are not given in ascending order", 2);
				counter++;
				rotmodeInitAmp[thisType][modeTracker[thisType]] = Double.parseDouble((String) currentData.get(counter));// amplitude
																														// of
																														// mode
				counter++;
				rotmodeMaxAmp[thisType][modeTracker[thisType]] = Double.parseDouble((String) currentData.get(counter));// maximum
																														// amplitude
																														// of
																														// mode
				counter++;
				rotmodeIrrep[thisType][modeTracker[thisType]] = Integer.parseInt((String) currentData.get(counter)) - 1;// irrep
																														// of
																														// mode
				counter++;
				rotmodeName[thisType][modeTracker[thisType]] = (String) currentData.get(counter);// name of first mode
																									// is made of four
																									// chunks
				counter++;
				for (int s = 0; s < numSubTypes[thisType]; s++)
					for (int a = 0; a < numSubAtoms[thisType][s]; a++)// iterate through the atoms
						for (int i = 0; i < 3; i++) {
							rotmodeVect[thisType][modeTracker[thisType]][s][a][i] = Double
									.parseDouble((String) currentData.get(counter));
							counter++;
						}
				modeTracker[thisType] += 1;
			}
		} else {
			// No rotational modes.
		}

//		for (int t=0; t<numTypes; t++)
//			System.out.println(t+" "+rotmodePerType[t]);

		ellipmodeNum = 0;
		ellipmodePerType = new int[numTypes];
		ellipmodeInitAmp = new double[numTypes][];
		ellipmodeMaxAmp = new double[numTypes][];
		ellipmodeIrrep = new int[numTypes][];
		ellipmodeName = new String[numTypes][];
		ellipmodeVect = new double[numTypes][][][][];
		for (int i = 0; i < numTypes; i++) {
			ellipmodePerType[i] = 0;
		}

		// find ellipsoidal modes
		currentTag = "ellipsoidmodelist";
		if (myMap.containsKey(currentTag)) {
			currentData = myMap.get(currentTag);

			int counter = 0;

			// Run through the data once and determine the number of ellptical modes
			while (counter < currentData.size()) {
				int atomType = Integer.parseInt((String) currentData.get(counter)) - 1;
				// Counter should be pointing at atom type number
				ellipmodePerType[atomType]++;
				ellipmodeNum++;
				// Point counter to next atom type number
				counter += 6;
				int atoms = 0;
				for (int i = 0; i < numSubTypes[atomType]; i++)
					atoms += numSubAtoms[atomType][i];
				counter = counter + 6 * atoms;
			}

			// Initialize needed arrays
			for (int t = 0; t < numTypes; t++) {
				ellipmodeIrrep[t] = new int[ellipmodePerType[t]];
				ellipmodeInitAmp[t] = new double[ellipmodePerType[t]];
				ellipmodeMaxAmp[t] = new double[ellipmodePerType[t]];
				ellipmodeName[t] = new String[ellipmodePerType[t]];
				ellipmodeVect[t] = new double[ellipmodePerType[t]][][][];
				for (int m = 0; m < ellipmodePerType[t]; m++) {
					ellipmodeVect[t][m] = new double[numSubTypes[t]][][];
					for (int s = 0; s < numSubTypes[t]; s++)
						ellipmodeVect[t][m][s] = new double[numSubAtoms[t][s]][6];
				}
				modeTracker[t] = 0;
			}

			// input ellptical mode array [atom type][mode number of that type][atom number
			// of that type][xx,yy,zz,xy,xz,xy]
			counter = 0;
			for (int m = 0; m < ellipmodeNum; m++)// iterate over modes
			{
				int thisType = Integer.parseInt((String) currentData.get(counter)) - 1;// first number is the type
																						// number
				counter++;
				if (modeTracker[thisType] + 1 != Integer.parseInt((String) currentData.get(counter)))
					parseError("The ellipsoidal modes are not given in ascending order", 2);
				counter++;
				ellipmodeInitAmp[thisType][modeTracker[thisType]] = Double
						.parseDouble((String) currentData.get(counter));// amplitude of mode
				counter++;
				ellipmodeMaxAmp[thisType][modeTracker[thisType]] = Double
						.parseDouble((String) currentData.get(counter));// maximum amplitude of mode
				counter++;
				ellipmodeIrrep[thisType][modeTracker[thisType]] = Integer.parseInt((String) currentData.get(counter))
						- 1;// irrep of mode
				counter++;
				ellipmodeName[thisType][modeTracker[thisType]] = (String) currentData.get(counter);// name of first mode
																									// is made of four
																									// chunks
				counter++;
				for (int s = 0; s < numSubTypes[thisType]; s++)
					for (int a = 0; a < numSubAtoms[thisType][s]; a++)// iterate through the atoms
						for (int i = 0; i < 6; i++) {
							ellipmodeVect[thisType][modeTracker[thisType]][s][a][i] = Double
									.parseDouble((String) currentData.get(counter));
							counter++;
						}
				modeTracker[thisType] += 1;
			}
		} else {
			// No ellipsoidal modes.
		}

		// Handle strain modes
		strainmodeNum = 0;
		currentTag = "strainmodelist";
		if (myMap.containsKey(currentTag)) {
			int dataPerRow = 11;
			currentData = myMap.get(currentTag);
			strainmodeNum = currentData.size() / dataPerRow;
			if (currentData.size() != strainmodeNum * dataPerRow) // Make sure it divided evenly
			{
				parseError(currentTag, 1);
			}

			strainmodeInitAmp = new double[strainmodeNum];
			strainmodeMaxAmp = new double[strainmodeNum];
			strainmodeIrrep = new int[strainmodeNum];
			strainmodeName = new String[strainmodeNum];
			strainmodeVect = new double[strainmodeNum][6];
			for (int i = 0; i < strainmodeNum; i++) {
				strainmodeInitAmp[i] = Double.parseDouble((String) currentData.get(dataPerRow * i + 1));
				strainmodeMaxAmp[i] = Double.parseDouble((String) currentData.get(dataPerRow * i + 2));
				strainmodeIrrep[i] = Integer.parseInt((String) currentData.get(dataPerRow * i + 3)) - 1;
				strainmodeName[i] = (String) currentData.get(dataPerRow * i + 4);
				for (int j = 0; j < 6; j++) {
					strainmodeVect[i][j] = Double.parseDouble((String) currentData.get(dataPerRow * i + j + 5));
				}
			}
		} else {
			// No strain modes.
		}

		// Calculate pBasisCart0
		if (isRhombParentSetting) {
			// defined row by row
			temp1 = Math.sin(pLatt0[5] / 2);
			temp2 = Math.sqrt(Math.pow(temp1, -2) - (4.0 / 3.0));
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
			temp1 = (Math.cos(pLatt0[3]) - Math.cos(pLatt0[4]) * Math.cos(pLatt0[5])) / Math.sin(pLatt0[5]);
			temp2 = Math.sqrt(1 - Math.pow(Math.cos(pLatt0[4]), 2) - Math.pow(temp1, 2));
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
		Vec.matdotmat(pBasisCart0, TmatInverseTranspose, sBasisCart0);
	}

	private static Map<String, ArrayList<String>> getDataMap(String dataString) {
		Map<String, ArrayList<String>> myMap = new TreeMap<String, ArrayList<String>>();
		StringTokenizer getData = new StringTokenizer(dataString);
		ArrayList<String> currentData = null;
		String currentTag = "";
		while (getData.hasMoreTokens()) {
			String next = getData.nextToken();
//			System.out.println("Next token: "+next);
			if (next.charAt(0) == '#')
				continue;
			if (next.charAt(0) == '!') {
				// We found a new tag, so put the old one in the map
				if (currentTag != "") {
					if (myMap.containsKey(currentTag)) // Duplicate tag
						parseError(currentTag, 0);
					myMap.put(currentTag, currentData); // Put the tag and its associated data in the map
					currentData = null;
				}
				currentTag = next.substring(1); // Remove the ! from the tag
				currentTag = currentTag.toLowerCase(Locale.ENGLISH); // Makes the tag lowercase
			} else // Put the string into the arraylist
			{
				if (currentTag != "") {
					if (currentData == null)
						currentData = new ArrayList<String>();
					currentData.add(next);
				}
			}
		}
		myMap.put(currentTag, currentData); // Put the last tag into the map

		return myMap;
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
	private static void parseError(String error, int type) {
		if (type == 1)
			throw new RuntimeException(("Invalid number of arguments for tag: " + error));
		if (type == 2)
			throw new RuntimeException(error);
		if (type == 3)
			throw new RuntimeException(("Required tag missing: " + error));
		return;
	}

	/** Determines the unique atom type of each non-unique atom type. */
	private void identifyUniqueAtoms() {
		// Determine the unique atoms types.
		boolean unique;
		String uniquetypes[] = new String[numTypes];
		atomTypeUnique = new int[numTypes];
		numUniques = 0;
		for (int t = 0; t < numTypes; t++) {
			unique = true;
			for (int u = 0; u < numUniques; u++)
				if (uniquetypes[u].compareTo(atomTypeSymbol[t]) == 0) // if(same)
				{
					unique = false;
					atomTypeUnique[t] = u;
				}
			if (unique) {
				uniquetypes[numUniques] = atomTypeSymbol[t];
				atomTypeUnique[t] = numUniques;
				numUniques += 1;
			}
		}
		needSimpleColor = (numUniques < numTypes); // if any type has multiple subtypes, we'll enable the "color" button
	}

	/** readSliders reads in the current values of each of the sliders. */
	public void readSliders() {
		double tempval;

		masterSliderVal = masterSlider.getValue() / sliderMaxVal;
		masterSliderLabel.setText(mytoString(masterSliderVal, 2, -8) + "  Master Slider");// change the master label to
																							// display the new amplitude
																							// value

		for (int t = 0; t < numTypes; t++)// iterate through types
		{
			for (int m = 0; m < dispmodePerType[t]; m++)// iterate through types of modes
			{
				dispmodeSliderVal[t][m] = dispmodeMaxAmp[t][m] * (dispmodeSlider[t][m].getValue() / sliderMaxVal);
				tempval = dispmodeSliderVal[t][m] * irrepmodeSliderVal[dispmodeIrrep[t][m]] * masterSliderVal;
				dispmodeSliderLabel[t][m].setText(mytoString(tempval, 2, -8) + "  " + dispmodeName[t][m]);
			}
			for (int m = 0; m < scalarmodePerType[t]; m++)// iterate through types of modes
			{
				scalarmodeSliderVal[t][m] = scalarmodeMaxAmp[t][m] * (scalarmodeSlider[t][m].getValue() / sliderMaxVal);
				tempval = scalarmodeSliderVal[t][m] * irrepmodeSliderVal[scalarmodeIrrep[t][m]] * masterSliderVal;
				scalarmodeSliderLabel[t][m].setText(mytoString(tempval, 2, -8) + "  " + scalarmodeName[t][m]);
			}
			for (int m = 0; m < magmodePerType[t]; m++)// iterate through types of modes
			{
				magmodeSliderVal[t][m] = magmodeMaxAmp[t][m] * (magmodeSlider[t][m].getValue() / sliderMaxVal);
				tempval = magmodeSliderVal[t][m] * irrepmodeSliderVal[magmodeIrrep[t][m]] * masterSliderVal;
				magmodeSliderLabel[t][m].setText(mytoString(tempval, 2, -8) + "  " + magmodeName[t][m]);
			}
			for (int m = 0; m < rotmodePerType[t]; m++)// iterate through types of modes
			{
				rotmodeSliderVal[t][m] = rotmodeMaxAmp[t][m] * (rotmodeSlider[t][m].getValue() / sliderMaxVal);
				tempval = rotmodeSliderVal[t][m] * irrepmodeSliderVal[rotmodeIrrep[t][m]] * masterSliderVal;
				rotmodeSliderLabel[t][m].setText(mytoString(tempval, 2, -8) + "  " + rotmodeName[t][m]);
			}
			for (int m = 0; m < ellipmodePerType[t]; m++)// iterate through types of modes
			{
				ellipmodeSliderVal[t][m] = ellipmodeMaxAmp[t][m] * (ellipmodeSlider[t][m].getValue() / sliderMaxVal);
				tempval = ellipmodeSliderVal[t][m] * irrepmodeSliderVal[ellipmodeIrrep[t][m]] * masterSliderVal;
				ellipmodeSliderLabel[t][m].setText(mytoString(tempval, 2, -8) + "  " + ellipmodeName[t][m]);
			}
		}
		for (int m = 0; m < strainmodeNum; m++)// iterate through strain modes
		{
			strainmodeSliderVal[m] = strainmodeMaxAmp[m] * (strainmodeSlider[m].getValue() / sliderMaxVal);
			tempval = strainmodeSliderVal[m] * irrepmodeSliderVal[strainmodeIrrep[m]] * masterSliderVal;
			strainmodeSliderLabel[m].setText(mytoString(tempval, 3, -8) + "  " + strainmodeName[m]);
		}
		for (int m = 0; m < numIrreps; m++)// iterate through strain modes
		{
			irrepmodeSliderVal[m] = (irrepmodeSlider[m].getValue() / sliderMaxVal);
			tempval = irrepmodeSliderVal[m];
			irrepmodeSliderLabel[m].setText(mytoString(tempval, 3, -8) + "  " + irrepName[m]);
		}
	}

	public void resetSliders() {
		masterSlider.setValue(sliderMax);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < dispmodePerType[t]; m++)
				dispmodeSlider[t][m].setValue((int) ((dispmodeInitAmp[t][m] / dispmodeMaxAmp[t][m]) * sliderMax));
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < scalarmodePerType[t]; m++)
				scalarmodeSlider[t][m].setValue((int) ((scalarmodeInitAmp[t][m] / scalarmodeMaxAmp[t][m]) * sliderMax));
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < magmodePerType[t]; m++)
				magmodeSlider[t][m].setValue((int) ((magmodeInitAmp[t][m] / magmodeMaxAmp[t][m]) * sliderMax));
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < rotmodePerType[t]; m++)
				rotmodeSlider[t][m].setValue((int) ((rotmodeInitAmp[t][m] / rotmodeMaxAmp[t][m]) * sliderMax));
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < ellipmodePerType[t]; m++)
				ellipmodeSlider[t][m].setValue((int) ((ellipmodeInitAmp[t][m] / ellipmodeMaxAmp[t][m]) * sliderMax));
		for (int m = 0; m < strainmodeNum; m++)
			strainmodeSlider[m].setValue((int) ((strainmodeInitAmp[m] / strainmodeMaxAmp[m]) * sliderMax));
		for (int m = 0; m < numIrreps; m++)
			irrepmodeSlider[m].setValue(sliderMax);
	}

	public void zeroSliders() {
		masterSlider.setValue(sliderMax);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < dispmodePerType[t]; m++)
				dispmodeSlider[t][m].setValue(0);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < scalarmodePerType[t]; m++)
				scalarmodeSlider[t][m].setValue(0);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < magmodePerType[t]; m++)
				magmodeSlider[t][m].setValue(0);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < rotmodePerType[t]; m++)
				rotmodeSlider[t][m].setValue(0);
		for (int t = 0; t < numTypes; t++)
			for (int m = 0; m < ellipmodePerType[t]; m++)
				ellipmodeSlider[t][m].setValue(0);
		for (int m = 0; m < strainmodeNum; m++)
			strainmodeSlider[m].setValue(0);
		for (int m = 0; m < numIrreps; m++)
			irrepmodeSlider[m].setValue(sliderMax);
	}

	public void toggleIrrepSliders() {
		if (irrepSlidersOn)
			for (int m = 0; m < numIrreps; m++)
				irrepmodeSlider[m].setValue(0);
		else
			for (int m = 0; m < numIrreps; m++)
				irrepmodeSlider[m].setValue(sliderMax);
		irrepSlidersOn = !irrepSlidersOn;
	}

	/**
	 * recalcDistortion recalculates the positions and occupancies based on current
	 * slider values.
	 */
	public void recalcDistortion() {
		double tempval, tempdisp, maxdisp, tempmag, maxmag, temprot, maxrot, tempellip, maxellip, avgocc;
		double[] tempvec = new double[3];
		double[][] tempmat = new double[3][3];
		double[] deltaCoord = new double[3];
		double[] deltaMag = new double[3];
		double[] deltaRot = new double[3];
		double[] deltaEllip = new double[6];
		double deltaOcc;
		double[][] pBasisCartTranspose = new double[3][3];
		double[][] sBasisCartTranspose = new double[3][3];
		double[] pStrainVoigt = new double[6];
		double[][] pStrainPlusIdentity = new double[3][3];

		// naively calculate strained parent-cell basis vectors in cartesian Angstrom
		// coords
		for (int n = 0; n < 6; n++)
			for (int m = 0; m < strainmodeNum; m++) {
				tempval = strainmodeSliderVal[m] * irrepmodeSliderVal[strainmodeIrrep[m]] * masterSliderVal;
				pStrainVoigt[n] += strainmodeVect[m][n] * tempval;
			}
		// calculate (strain tensor)+(identity matrix) from voigt strain components
		pStrainPlusIdentity[0][0] = pStrainVoigt[0] + 1;
		pStrainPlusIdentity[1][0] = pStrainVoigt[5] / 2;
		pStrainPlusIdentity[2][0] = pStrainVoigt[4] / 2;
		pStrainPlusIdentity[0][1] = pStrainVoigt[5] / 2;
		pStrainPlusIdentity[1][1] = pStrainVoigt[1] + 1;
		pStrainPlusIdentity[2][1] = pStrainVoigt[3] / 2;
		pStrainPlusIdentity[0][2] = pStrainVoigt[4] / 2;
		pStrainPlusIdentity[1][2] = pStrainVoigt[3] / 2;
		pStrainPlusIdentity[2][2] = pStrainVoigt[2] + 1;

		// calculate strained parent basis vectors in cartesian Angstrom coords
		Vec.matdotmat(pStrainPlusIdentity, pBasisCart0, pBasisCart);

		// calculate strained supercell basis vectors in cartesian Angstrom coords
		Vec.matdotmat(pBasisCart, TmatInverseTranspose, sBasisCart);

		// calculate some other useful matrices
		Vec.mattranspose(pBasisCart, pBasisCartTranspose);
		Vec.mattranspose(sBasisCart, sBasisCartTranspose);
		Vec.matinverse(sBasisCart, sBasisCartInverse);

		// calculate the strained parent and supercell lattice parameters
		pLatt[0] = Math.sqrt(Vec.dot(pBasisCartTranspose[0], pBasisCartTranspose[0])); // a
		pLatt[1] = Math.sqrt(Vec.dot(pBasisCartTranspose[1], pBasisCartTranspose[1])); // b
		pLatt[2] = Math.sqrt(Vec.dot(pBasisCartTranspose[2], pBasisCartTranspose[2])); // c
		pLatt[3] = Math
				.acos(Vec.dot(pBasisCartTranspose[1], pBasisCartTranspose[2]) / Math.max(pLatt[1] * pLatt[2], 0.001)); // alpha
		pLatt[4] = Math
				.acos(Vec.dot(pBasisCartTranspose[0], pBasisCartTranspose[2]) / Math.max(pLatt[0] * pLatt[2], 0.001)); // beta
		pLatt[5] = Math
				.acos(Vec.dot(pBasisCartTranspose[0], pBasisCartTranspose[1]) / Math.max(pLatt[0] * pLatt[1], 0.001)); // gamma
		sLatt[0] = Math.sqrt(Vec.dot(sBasisCartTranspose[0], sBasisCartTranspose[0])); // a
		sLatt[1] = Math.sqrt(Vec.dot(sBasisCartTranspose[1], sBasisCartTranspose[1])); // b
		sLatt[2] = Math.sqrt(Vec.dot(sBasisCartTranspose[2], sBasisCartTranspose[2])); // c
		sLatt[3] = Math
				.acos(Vec.dot(sBasisCartTranspose[1], sBasisCartTranspose[2]) / Math.max(sLatt[1] * sLatt[2], 0.001)); // alpha
		sLatt[4] = Math
				.acos(Vec.dot(sBasisCartTranspose[0], sBasisCartTranspose[2]) / Math.max(sLatt[0] * sLatt[2], 0.001)); // beta
		sLatt[5] = Math
				.acos(Vec.dot(sBasisCartTranspose[0], sBasisCartTranspose[1]) / Math.max(sLatt[0] * sLatt[1], 0.001)); // gamma

		boolean testing = false;

		if (testing) {
			double r2d = 180.0 / Math.PI;
			// Diagnostic code
			System.out.print("pBasisCart0: ");
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++)
					System.out.printf("%7.4f ", pBasisCart0[j][i]);
			System.out.println("");
			System.out.println("pCell0: " + pLatt0[0] + ", " + pLatt0[1] + ", " + pLatt0[2] + ", " + pLatt0[3] * r2d
					+ ", " + pLatt0[4] * r2d + ", " + pLatt0[5] * r2d + ", ");
			System.out.print("pBasisCart: ");
			for (int i = 0; i < 3; i++)
				for (int j = 0; j < 3; j++)
					System.out.printf("%7.4f ", pBasisCart[j][i]);
			System.out.println("");
			System.out.println("pCell: " + pLatt[0] + ", " + pLatt[1] + ", " + pLatt[2] + ", " + pLatt[3] * r2d + ", "
					+ pLatt[4] * r2d + ", " + pLatt[5] * r2d + ", ");
			System.out.println("");
//      System.out.println ("sCell: "+sLatt[0]+", "+sLatt[1]+", "+sLatt[2]+", "+sLatt[3]+", "+sLatt[4]+", "+sLatt[5]+", ");
		}

		// set the parent and supercell lattice parameter labels
		for (int n = 0; n < 3; n++) {
			pLattLabel[n].setText(mytoString(pLatt[n], 2, -5));
			pLattLabel[n + 3].setText(mytoString((180 / Math.PI) * pLatt[n + 3], 2, -5));
			sLattLabel[n].setText(mytoString(sLatt[n], 2, -5));
			sLattLabel[n + 3].setText(mytoString((180 / Math.PI) * sLatt[n + 3], 2, -5));
		}

		// calculate the parent cell origin in strained cartesian Angtstrom coords
		Vec.matdotvect(sBasisCart, pOriginUnitless, pOriginCart);

		// calculate the 8 cell vertices in strained cartesian Angstrom coordinates
		for (int ix = 0; ix < 2; ix++)
			for (int iy = 0; iy < 2; iy++)
				for (int iz = 0; iz < 2; iz++)
					for (int i = 0; i < 3; i++) {
						parentCellVertices[ix + 2 * iy + 4 * iz][i] = ix * pBasisCart[i][0] + iy * pBasisCart[i][1]
								+ iz * pBasisCart[i][2] + pOriginCart[i];
						superCellVertices[ix + 2 * iy + 4 * iz][i] = ix * sBasisCart[i][0] + iy * sBasisCart[i][1]
								+ iz * sBasisCart[i][2];
					}

//      Calculate the center of the supercell in strained cartesian Angstrom coords (sOriginCart).
		double[] minloc = new double[3];
		double[] maxloc = new double[3];
		for (int i = 0; i < 3; i++) {
			minloc[i] = 1000000;
			maxloc[i] = -1000000;
		}
		for (int j = 0; j < 8; j++)
			for (int i = 0; i < 3; i++) {
				tempvec[i] = superCellVertices[j][i];
				if (tempvec[i] < minloc[i])
					minloc[i] = tempvec[i];
				if (tempvec[i] > maxloc[i])
					maxloc[i] = tempvec[i];
			}
		for (int t = 0; t < numTypes; t++)
			for (int s = 0; s < numSubTypes[t]; s++)
				for (int a = 0; a < numSubAtoms[t][s]; a++) {
					Vec.matdotvect(sBasisCart, atomInitCoord[t][s][a], tempvec);
					for (int i = 0; i < 3; i++) {
						if (tempvec[i] < minloc[i])
							minloc[i] = tempvec[i];
						if (tempvec[i] > maxloc[i])
							maxloc[i] = tempvec[i];
					}
				}
		for (int i = 0; i < 3; i++)
			sCenterCart[i] = (maxloc[i] + minloc[i]) / 2;

		// Place the center of the supercell at the origin
		for (int j = 0; j < 8; j++)
			for (int i = 0; i < 3; i++) {
				parentCellVertices[j][i] -= sCenterCart[i];
				superCellVertices[j][i] -= sCenterCart[i];
			}

		// calculate the new atomic positions and radii
		for (int t = 0; t < numTypes; t++)
			for (int s = 0; s < numSubTypes[t]; s++) {
				maxdisp = 0;
				maxmag = 0;
				maxrot = 0;
				maxellip = 0;
				avgocc = 0;
				for (int a = 0; a < numSubAtoms[t][s]; a++) {
					deltaOcc = 0;
					for (int m = 0; m < scalarmodePerType[t]; m++)// sum up the scalar vectors for all the modes for
																	// each atom of given type
					{
						tempval = scalarmodeSliderVal[t][m] * irrepmodeSliderVal[scalarmodeIrrep[t][m]]
								* masterSliderVal;
						deltaOcc += scalarmodeVect[t][m][s][a] * tempval;
					}
					atomFinalOcc[t][s][a] = atomInitOcc[t][s][a] + deltaOcc;
//					System.out.println("tsa:"+t+s+a+", deltaOcc:"+deltaOcc);
					for (int i = 0; i < 3; i++) {
						deltaCoord[i] = 0;
						for (int m = 0; m < dispmodePerType[t]; m++)// sum up the displacement vectors for all the modes
																	// for each atom of given type
						{
							tempval = dispmodeSliderVal[t][m] * irrepmodeSliderVal[dispmodeIrrep[t][m]]
									* masterSliderVal;
							deltaCoord[i] += dispmodeVect[t][m][s][a][i] * tempval; // change in unitless position
						}
						atomFinalCoord[t][s][a][i] = atomInitCoord[t][s][a][i] + deltaCoord[i]; // total position in
																								// lattice coords
																								// (undistorted position
																								// + aggregate
																								// displacement).
//						System.out.println("tsai:"+t+s+a+i+", deltaCoord:"+deltaCoord[i]);
					}

					for (int i = 0; i < 3; i++) {
						deltaMag[i] = 0;
						for (int m = 0; m < magmodePerType[t]; m++)// sum up the magnetic moment vectors for all the
																	// modes for each atom of given type
						{
							tempval = magmodeSliderVal[t][m] * irrepmodeSliderVal[magmodeIrrep[t][m]] * masterSliderVal;
							deltaMag[i] += magmodeVect[t][m][s][a][i] * tempval; // change in magnetic moment
						}
						atomFinalMag[t][s][a][i] = atomInitMag[t][s][a][i] + deltaMag[i]; // total magnetic moment
																							// (undistorted + aggregate
																							// change).
//						System.out.println("tsai:"+t+s+a+i+", deltaMag:"+deltaMag[i]);
					}

					for (int i = 0; i < 3; i++) {
						deltaRot[i] = 0;
						for (int m = 0; m < rotmodePerType[t]; m++)// sum up the rotational vectors for all the modes
																	// for each atom of given type
						{
							tempval = rotmodeSliderVal[t][m] * irrepmodeSliderVal[rotmodeIrrep[t][m]] * masterSliderVal;
							deltaRot[i] += rotmodeVect[t][m][s][a][i] * tempval; // change in rotation angle
						}
						atomFinalRot[t][s][a][i] = atomInitRot[t][s][a][i] + deltaRot[i]; // total rotation angle
																							// (undistorted + aggregate
																							// change).
					}

					for (int i = 0; i < 6; i++) {
						deltaEllip[i] = 0;
						for (int m = 0; m < ellipmodePerType[t]; m++)// sum up the ellipsoid vectors for all the modes
																		// for each atom of given type
						{
							tempval = ellipmodeSliderVal[t][m] * irrepmodeSliderVal[ellipmodeIrrep[t][m]]
									* masterSliderVal;
							deltaEllip[i] += ellipmodeVect[t][m][s][a][i] * tempval; // change in ellipsoid
//							System.out.println("ellipcheck: "+t+", "+s+", "+a+", "+i+", "+deltaEllip[i]);
						}
						atomFinalEllip[t][s][a][i] = atomInitEllip[t][s][a][i] + deltaEllip[i]; // total ellipsoid
																								// params (undistorted +
																								// aggregate change).
					}

//					Determine the "size" of the displacement.
					Vec.matdotvect(sBasisCart, deltaCoord, tempvec); // displacement in cartesian coords
					tempdisp = Vec.norm(tempvec); // displacement magnitude in Angstroms
					if (tempdisp > maxdisp)
						maxdisp = tempdisp;

//					Determine the "size" of the magnetic moment.
					Vec.matdotvect(sBasisCart, atomFinalMag[t][s][a], tempvec); // magnetic moment in cartesian coords
					tempmag = Vec.norm(tempvec); // moment magnitude in Angstroms
					if (tempmag > maxmag)
						maxmag = tempmag;

//					Determine the "size" of the rotation angle.
					Vec.matdotvect(sBasisCart, atomFinalRot[t][s][a], tempvec); // rotation in cartesian coords
					temprot = Vec.norm(tempvec); // rotation in Angstroms
					if (temprot > maxrot)
						maxrot = temprot;

					avgocc += atomFinalOcc[t][s][a] / numSubAtoms[t][s];

//					Determine the "size" of the ellipsoid.
					Vec.voigt2matrix(atomFinalEllip[t][s][a], tempmat);
					Vec.matdotmat(sBasisCart, tempmat, tempmat);
					Vec.matdotmat(tempmat, sBasisCartInverse, tempmat);// ellipsoid in cartesian coords
					tempellip = 0;
					for (int i = 0; i < 3; i++)
						for (int j = 0; j < 3; j++)
							tempellip += Math.pow(tempmat[i][j], 2);
					if (tempellip > maxellip)
						maxellip = tempellip;

				}
				subTypeLabel[t][s].setText("      " + subTypeName[t][s] + "  [" + mytoString(maxdisp, 2, -4) + ", "
						+ mytoString(avgocc, 2, -4) + ", " + mytoString(maxmag, 2, -4) + ", "
						+ mytoString(maxrot, 2, -4) + "]");
				subTypeBox[t][s].setText(
						subTypeName[t][s] + "  [" + mytoString(maxdisp, 2, -4) + ", " + mytoString(avgocc, 2, -4) + ", "
								+ mytoString(maxmag, 2, -4) + ", " + mytoString(maxrot, 2, -4) + "]");
			}
	}

	private final static int subTypeWidth = 170;
	private final static int controlPanelHeight = 55, roomForScrollBar = 15, padding = 4, barheight = 20;
	private final static int subTypeBoxWidth = 20, sliderLabelWidth = 36, lattWidth = 60;

	/** Number of rows needed for checkboxes; [type] */
	private int[] numSubRows;
	/** Actual number of check boxes per row */
	private int[] subTypesPerRow;

	/**
	 * instantiates and initializes the scroll and control panels.
	 * 
	 * @param controlPanel
	 * @param sliderPanel
	 */
	public void initPanels(JPanel sliderPanel, JPanel controlPanel) {
//  Divide the applet area with structure on the left and controls on the right.

		sliderPanel.removeAll();

		int controlPaneHeight = controlPanelHeight + padding;
		int sliderPaneHeight = appletHeight - controlPaneHeight;
		int sliderPanelHeight = sliderPaneHeight - padding;
		int controlPaneWidth = appletWidth;
		int controlPanelWidth = controlPaneWidth - padding;
		int sliderPaneWidth = appletWidth - sliderPaneHeight;
		int sliderPanelWidth = sliderPaneWidth - roomForScrollBar - padding;

		// Calculate numRows of each subtype
		/** Number of extra rows above slider bar panel (for view panel) */
		int numExtraRows = 5; // One for master slider bar, one for strainTitle, one for irrepTitle, and 2 for
								// the lattice params.
		/** Minimum number of rows in grid that will keep the rows thin */
		int minRowNumber = (int) Math.floor(sliderPanelHeight / barheight);

		/** Maximum number of check boxes per row the GUI will hold */
		int maxSubTypesPerRow = (int) Math.floor(sliderPanelWidth / subTypeWidth);

		int numSubRowsTotal = 0;
		numSubRows = new int[numTypes];
		subTypesPerRow = new int[numTypes];
		for (int t = 0; t < numTypes; t++)// iterate over types
		{
			subTypesPerRow[t] = Math.min(maxSubTypesPerRow, numSubTypes[t]);
			numSubRows[t] = (int) Math.ceil((double) numSubTypes[t] / subTypesPerRow[t]);
			numSubRowsTotal += numSubRows[t];
		}
		int rowCount = dispmodeNum + scalarmodeNum + magmodeNum + rotmodeNum + ellipmodeNum + strainmodeNum + numIrreps
				+ numTypes + numExtraRows + numSubRowsTotal;
		int rowNumber = Math.max(rowCount, minRowNumber);
		addControls(sliderPanel, controlPanel, sliderPanelWidth, rowNumber);
		controlPanel.setPreferredSize(new Dimension(controlPanelWidth, controlPanelHeight));
		// size of master slider bar's label
		sliderPanel.setPreferredSize(new Dimension(sliderPanelWidth, rowNumber * barheight));
	}

	private void addControls(JPanel sliderPanel, JPanel controlPanel, int sliderPanelWidth, int rowNumber) {
		int sliderWidth = sliderPanelWidth / 2;

		// Master Slider Panel
		masterSliderLabel = new JLabel();// label for master slider bar
		masterSliderLabel.setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of master slider bar's
																						// label
		masterSliderLabel.setBackground(Color.WHITE);
		masterSliderLabel.setForeground(Color.BLACK);
		masterSliderLabel.setHorizontalAlignment(JLabel.CENTER);
		masterSliderLabel.setVerticalAlignment(JLabel.CENTER);

		masterSlider = new JSlider(JSlider.HORIZONTAL, 0, sliderMax, sliderMax);
		masterSlider.setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
		masterSlider.setBackground(Color.WHITE);// color of master slider bar
		masterSlider.addChangeListener(this);// add to it a listener for slider value changes
		masterSlider.setFocusable(false);

		masterSliderPanel = new JPanel();
		masterSliderPanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
		masterSliderPanel.setLayout(new GridLayout(1, 2));
		masterSliderPanel.setBackground(Color.WHITE);
		masterSliderPanel.add(masterSlider);// add master slider bar to left panel
		masterSliderPanel.add(masterSliderLabel);// add master slider bar's label to right panel

		sliderPanel.add(masterSliderPanel);// add master slider bar to top of scrollPanel

		// Initialize type-specific subpanels of scrollPanel
		typeLabel = new JLabel[numTypes];
		typeNamePanel = new JPanel[numTypes];
		typeDataPanel = new JPanel[numTypes];
		subTypeBox = new JCheckBox[numTypes][];
		subTypeLabel = new JLabel[numTypes][];

		dispPanel = new JPanel[numTypes];
		dispmodeSlider = new JSlider[numTypes][];// creates the array of slider bars
		dispmodeSliderLabel = new JLabel[numTypes][];// creates array of labels
		dispmodeSliderVal = new double[numTypes][];

		scalarPanel = new JPanel[numTypes];
		scalarmodeSlider = new JSlider[numTypes][];// creates the array of slider bars
		scalarmodeSliderLabel = new JLabel[numTypes][];// creates array of labels
		scalarmodeSliderVal = new double[numTypes][];

		magPanel = new JPanel[numTypes];
		magmodeSlider = new JSlider[numTypes][];// creates the array of slider bars
		magmodeSliderLabel = new JLabel[numTypes][];// creates array of labels
		magmodeSliderVal = new double[numTypes][];

		rotPanel = new JPanel[numTypes];
		rotmodeSlider = new JSlider[numTypes][];// creates the array of slider bars
		rotmodeSliderLabel = new JLabel[numTypes][];// creates array of labels
		rotmodeSliderVal = new double[numTypes][];

		ellipPanel = new JPanel[numTypes];
		ellipmodeSlider = new JSlider[numTypes][];// creates the array of slider bars
		ellipmodeSliderLabel = new JLabel[numTypes][];// creates array of labels
		ellipmodeSliderVal = new double[numTypes][];

		// The big loop over types
		for (int t = 0; t < numTypes; t++) {
			subTypeBox[t] = new JCheckBox[numSubTypes[t]];// initialize
			subTypeLabel[t] = new JLabel[numSubTypes[t]];
			dispmodeSlider[t] = new JSlider[dispmodePerType[t]];// creates the array of slider bars
			dispmodeSliderLabel[t] = new JLabel[dispmodePerType[t]];// creates array of labels
			dispmodeSliderVal[t] = new double[dispmodePerType[t]];
			scalarmodeSlider[t] = new JSlider[scalarmodePerType[t]];// creates the array of slider bars
			scalarmodeSliderLabel[t] = new JLabel[scalarmodePerType[t]];// creates array of labels
			scalarmodeSliderVal[t] = new double[scalarmodePerType[t]];
			magmodeSlider[t] = new JSlider[magmodePerType[t]];// creates the array of slider bars
			magmodeSliderLabel[t] = new JLabel[magmodePerType[t]];// creates array of labels
			magmodeSliderVal[t] = new double[magmodePerType[t]];
			rotmodeSlider[t] = new JSlider[rotmodePerType[t]];// creates the array of slider bars
			rotmodeSliderLabel[t] = new JLabel[rotmodePerType[t]];// creates array of labels
			rotmodeSliderVal[t] = new double[rotmodePerType[t]];
			ellipmodeSlider[t] = new JSlider[ellipmodePerType[t]];// creates the array of slider bars
			ellipmodeSliderLabel[t] = new JLabel[ellipmodePerType[t]];// creates array of labels
			ellipmodeSliderVal[t] = new double[ellipmodePerType[t]];

			// typeNamePanel
			typeLabel[t] = new JLabel();
			typeLabel[t].setText("" + atomTypeName[t] + " Modes");
			typeLabel[t].setPreferredSize(new Dimension(sliderPanelWidth, barheight));// size of slider labels
			typeLabel[t].setBackground(color[t]);// color labels
			typeLabel[t].setForeground(Color.WHITE);
			typeLabel[t].setHorizontalAlignment(JLabel.CENTER);
			typeLabel[t].setVerticalAlignment(JLabel.CENTER);
			typeNamePanel[t] = new JPanel();// make a new panel to hold color/checkboxes and label
			typeNamePanel[t].setPreferredSize(new Dimension(sliderPanelWidth, barheight));
			typeNamePanel[t].setLayout(new GridLayout(1, 1, 0, 0));
			typeNamePanel[t].setBackground(color[t]);
			typeNamePanel[t].add(typeLabel[t]);
//			System.out.println("t: "+t+", numSubTypes[t]: "+numSubTypes[t]+", boxesPerRow[t]: "+boxesPerRow[t]+", numCheckRows[t]: "+numCheckRows[t]);

			sliderPanel.add(typeNamePanel[t]);

			// typeDataPanel
			typeDataPanel[t] = new JPanel();
			typeDataPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, numSubRows[t] * barheight));
			typeDataPanel[t].setLayout(new GridLayout(numSubRows[t], subTypesPerRow[t], 0, 0));
			typeDataPanel[t].setBackground(color[t]);
			for (int s = 0; s < numSubTypes[t]; s++) {
				subTypeBox[t][s] = new JCheckBox();
				subTypeBox[t][s].setPreferredSize(new Dimension(subTypeBoxWidth, barheight));
				subTypeBox[t][s].setFocusable(false);
				subTypeBox[t][s].setBackground(color[t]);
				subTypeBox[t][s].setForeground(Color.WHITE);
				subTypeBox[t][s].setHorizontalAlignment(JCheckBox.LEFT);
				subTypeBox[t][s].setVerticalAlignment(JCheckBox.CENTER);
				subTypeBox[t][s].addItemListener(app.checkboxListener);
				subTypeLabel[t][s] = new JLabel();
				subTypeLabel[t][s].setPreferredSize(new Dimension(subTypeWidth - subTypeBoxWidth, barheight));
				subTypeLabel[t][s].setBackground(color[t]);
				subTypeLabel[t][s].setForeground(Color.WHITE);
				subTypeLabel[t][s].setHorizontalAlignment(JLabel.LEFT);
				subTypeLabel[t][s].setVerticalAlignment(JLabel.CENTER);
				if (!diffraction)
					typeDataPanel[t].add(subTypeBox[t][s]);
				else
					typeDataPanel[t].add(subTypeLabel[t][s]);
			}
			for (int s = numSubTypes[t]; s < numSubRows[t] * subTypesPerRow[t]; s++)
				typeDataPanel[t].add(new JLabel("")); // Makes sure that each subtype row is full for allignment
														// purposes

			sliderPanel.add(typeDataPanel[t]);

			// dispPanel
			dispPanel[t] = new JPanel();
			dispPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, dispmodePerType[t] * barheight));// size of
																											// slider
																											// labels
			dispPanel[t].setBackground(color[t]);
			dispPanel[t].setLayout(new GridLayout(dispmodePerType[t], 2, 0, 0));
			for (int m = 0; m < dispmodePerType[t]; m++)// iterate through array of slider bars
			{
				dispmodeSliderLabel[t][m] = new JLabel();// add label to left panel
				dispmodeSliderLabel[t][m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of slider
																										// labels
				dispmodeSliderLabel[t][m].setBackground(color[t]);// color labels
				dispmodeSliderLabel[t][m].setForeground(Color.WHITE);// color labels
				dispmodeSliderLabel[t][m].setHorizontalAlignment(JLabel.LEFT);
				dispmodeSliderLabel[t][m].setVerticalAlignment(JLabel.CENTER);

				dispmodeSlider[t][m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
						(int) ((dispmodeInitAmp[t][m] / dispmodeMaxAmp[t][m]) * sliderMax));// put a slider bar in the
																							// ith slot
				dispmodeSlider[t][m].setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
				dispmodeSlider[t][m].setBackground(color[t]);// set the slider bar's color
				dispmodeSlider[t][m].addChangeListener(this);// add to it a listener for slider value changes
				dispmodeSlider[t][m].setFocusable(false);

				dispPanel[t].add(dispmodeSlider[t][m]);// add slider bar to left of modePanel
				dispPanel[t].add(dispmodeSliderLabel[t][m]);// add slider bar to left of modePanel
			}

			sliderPanel.add(dispPanel[t]);// add this modePanel to scrollPanel

			// scalarPanel
			scalarPanel[t] = new JPanel();
			scalarPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, scalarmodePerType[t] * barheight));// size
																												// of
																												// slider
																												// labels
			scalarPanel[t].setBackground(scalarcolor[t]);
			scalarPanel[t].setLayout(new GridLayout(scalarmodePerType[t], 2, 0, 0));
			for (int m = 0; m < scalarmodePerType[t]; m++)// iterate through array of slider bars
			{
				scalarmodeSliderLabel[t][m] = new JLabel();// add label to left panel
				scalarmodeSliderLabel[t][m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of
																											// slider
																											// labels
				scalarmodeSliderLabel[t][m].setBackground(scalarcolor[t]);// color labels
				scalarmodeSliderLabel[t][m].setForeground(Color.WHITE);// color labels
				scalarmodeSliderLabel[t][m].setHorizontalAlignment(JLabel.LEFT);
				scalarmodeSliderLabel[t][m].setVerticalAlignment(JLabel.CENTER);
				scalarmodeSlider[t][m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
						(int) ((scalarmodeInitAmp[t][m] / scalarmodeMaxAmp[t][m]) * sliderMax));// put a slider bar in
																								// the ith slot
				scalarmodeSlider[t][m].setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
				scalarmodeSlider[t][m].setBackground(scalarcolor[t]);// set the slider bar's color
				scalarmodeSlider[t][m].addChangeListener(this);// add to it a listener for slider value changes
				scalarmodeSlider[t][m].setFocusable(false);

				scalarPanel[t].add(scalarmodeSlider[t][m]);// add slider bar to left of modePanel
				scalarPanel[t].add(scalarmodeSliderLabel[t][m]);// add slider bar to right of modePanel
			}

			sliderPanel.add(scalarPanel[t]);// add this modePanel to scrollPanel

			// magPanel
			magPanel[t] = new JPanel();
			magPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, magmodePerType[t] * barheight));// size of
																											// slider
																											// labels
			magPanel[t].setBackground(magcolor[t]);
			magPanel[t].setLayout(new GridLayout(magmodePerType[t], 2, 0, 0));
			for (int m = 0; m < magmodePerType[t]; m++)// iterate through array of slider bars
			{
				magmodeSliderLabel[t][m] = new JLabel();// add label to left panel
				magmodeSliderLabel[t][m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of slider
																										// labels
				magmodeSliderLabel[t][m].setBackground(magcolor[t]);// color labels
				magmodeSliderLabel[t][m].setForeground(Color.WHITE);// color labels
				magmodeSliderLabel[t][m].setHorizontalAlignment(JLabel.LEFT);
				magmodeSliderLabel[t][m].setVerticalAlignment(JLabel.CENTER);

				magmodeSlider[t][m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
						(int) ((magmodeInitAmp[t][m] / magmodeMaxAmp[t][m]) * sliderMax));// put a slider bar in the ith
																							// slot
				magmodeSlider[t][m].setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
				magmodeSlider[t][m].setBackground(magcolor[t]);// set the slider bar's color
				magmodeSlider[t][m].addChangeListener(this);// add to it a listener for slider value changes
				magmodeSlider[t][m].setFocusable(false);

				magPanel[t].add(magmodeSlider[t][m]);// add slider bar to left of modePanel
				magPanel[t].add(magmodeSliderLabel[t][m]);// add slider bar to left of modePanel
			}

			sliderPanel.add(magPanel[t]);// add this modePanel to scrollPanel

			// rotPanel
			rotPanel[t] = new JPanel();
			rotPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, rotmodePerType[t] * barheight));// size of
																											// slider
																											// labels
			rotPanel[t].setBackground(rotcolor[t]);
			rotPanel[t].setLayout(new GridLayout(rotmodePerType[t], 2, 0, 0));
			for (int m = 0; m < rotmodePerType[t]; m++)// iterate through array of slider bars
			{
				rotmodeSliderLabel[t][m] = new JLabel();// add label to left panel
				rotmodeSliderLabel[t][m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of slider
																										// labels
				rotmodeSliderLabel[t][m].setBackground(rotcolor[t]);// color labels
				rotmodeSliderLabel[t][m].setForeground(Color.WHITE);// color labels
				rotmodeSliderLabel[t][m].setHorizontalAlignment(JLabel.LEFT);
				rotmodeSliderLabel[t][m].setVerticalAlignment(JLabel.CENTER);

				rotmodeSlider[t][m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
						(int) ((rotmodeInitAmp[t][m] / rotmodeMaxAmp[t][m]) * sliderMax));// put a slider bar in the ith
																							// slot
				rotmodeSlider[t][m].setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
				rotmodeSlider[t][m].setBackground(rotcolor[t]);// set the slider bar's color
				rotmodeSlider[t][m].addChangeListener(this);// add to it a listener for slider value changes
				rotmodeSlider[t][m].setFocusable(false);

				rotPanel[t].add(rotmodeSlider[t][m]);// add slider bar to left of modePanel
				rotPanel[t].add(rotmodeSliderLabel[t][m]);// add slider bar to left of modePanel
			}

			sliderPanel.add(rotPanel[t]);// add this modePanel to scrollPanel

			// ellipPanel
			ellipPanel[t] = new JPanel();
			ellipPanel[t].setPreferredSize(new Dimension(sliderPanelWidth, ellipmodePerType[t] * barheight));// size of
																												// slider
																												// labels
			ellipPanel[t].setBackground(ellipcolor[t]);
			ellipPanel[t].setLayout(new GridLayout(ellipmodePerType[t], 2, 0, 0));
			for (int m = 0; m < ellipmodePerType[t]; m++)// iterate through array of slider bars
			{
				ellipmodeSliderLabel[t][m] = new JLabel();// add label to left panel
				ellipmodeSliderLabel[t][m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of
																										// slider labels
				ellipmodeSliderLabel[t][m].setBackground(ellipcolor[t]);// color labels
				ellipmodeSliderLabel[t][m].setForeground(Color.WHITE);// color labels
				ellipmodeSliderLabel[t][m].setHorizontalAlignment(JLabel.LEFT);
				ellipmodeSliderLabel[t][m].setVerticalAlignment(JLabel.CENTER);

				ellipmodeSlider[t][m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
						(int) ((ellipmodeInitAmp[t][m] / ellipmodeMaxAmp[t][m]) * sliderMax));// put a slider bar in the
																								// ith slot
				ellipmodeSlider[t][m].setPreferredSize(new Dimension(sliderWidth, barheight));// size of slider labels
				ellipmodeSlider[t][m].setBackground(ellipcolor[t]);// set the slider bar's color
				ellipmodeSlider[t][m].addChangeListener(this);// add to it a listener for slider value changes
				ellipmodeSlider[t][m].setFocusable(false);

				ellipPanel[t].add(ellipmodeSlider[t][m]);// add slider bar to left of modePanel
				ellipPanel[t].add(ellipmodeSliderLabel[t][m]);// add slider bar to left of modePanel
			}

			sliderPanel.add(ellipPanel[t]);// add this modePanel to scrollPanel
		}

		// strainTitlePanel
		JLabel strainTitle = new JLabel("StrainModes");
		strainTitle.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
		strainTitle.setBackground(Color.DARK_GRAY);// color labels
		strainTitle.setForeground(Color.WHITE);
		strainTitle.setHorizontalAlignment(JLabel.CENTER);
		strainTitle.setVerticalAlignment(JLabel.CENTER);

		JPanel strainTitlePanel = new JPanel();// make a new panel to hold color/checkboxes and label
		strainTitlePanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
		strainTitlePanel.setLayout(new GridLayout(1, 1, 0, 0));
		strainTitlePanel.setBackground(Color.DARK_GRAY);
		strainTitlePanel.add(strainTitle);

		sliderPanel.add(strainTitlePanel);

		// strainDataPanel
		for (int n = 0; n < 6; n++) {
			sLattLabel[n] = new JLabel();
			sLattLabel[n].setPreferredSize(new Dimension(lattWidth, barheight));// size of slider labels
			sLattLabel[n].setBackground(Color.DARK_GRAY);// color labels
			sLattLabel[n].setForeground(Color.WHITE);
			sLattLabel[n].setHorizontalAlignment(JLabel.LEFT);
			sLattLabel[n].setVerticalAlignment(JLabel.CENTER);

			pLattLabel[n] = new JLabel();
			pLattLabel[n].setPreferredSize(new Dimension(lattWidth, barheight));// size of slider labels
			pLattLabel[n].setBackground(Color.DARK_GRAY);// color labels
			pLattLabel[n].setForeground(Color.WHITE);
			pLattLabel[n].setHorizontalAlignment(JLabel.LEFT);
			pLattLabel[n].setVerticalAlignment(JLabel.CENTER);
		}
		parentLabel = new JLabel("  Pcell");
		parentLabel.setPreferredSize(new Dimension(lattWidth, barheight));// size of slider labels
		parentLabel.setBackground(Color.DARK_GRAY);// color labels
		parentLabel.setForeground(Color.WHITE);
		parentLabel.setHorizontalAlignment(JLabel.LEFT);
		parentLabel.setVerticalAlignment(JLabel.CENTER);

		superLabel = new JLabel("  Scell");
		superLabel.setPreferredSize(new Dimension(lattWidth, barheight));// size of slider labels
		superLabel.setBackground(Color.DARK_GRAY);// color labels
		superLabel.setForeground(Color.WHITE);
		superLabel.setHorizontalAlignment(JLabel.LEFT);
		superLabel.setVerticalAlignment(JLabel.CENTER);

		JPanel strainDataPanel = new JPanel();// make a new panel to hold color/checkboxes and label
		strainDataPanel.setPreferredSize(new Dimension(sliderPanelWidth, 2 * barheight));
		strainDataPanel.setLayout(new GridLayout(2, 6, 0, 0));
		strainDataPanel.setBackground(Color.DARK_GRAY);
		strainDataPanel.add(parentLabel);
		for (int n = 0; n < 6; n++)
			strainDataPanel.add(pLattLabel[n]);
		strainDataPanel.add(superLabel);
		for (int n = 0; n < 6; n++)
			strainDataPanel.add(sLattLabel[n]);

		sliderPanel.add(strainDataPanel);

		// strainPanel
		strainPanel = new JPanel();
		strainPanel.setPreferredSize(new Dimension(sliderPanelWidth, strainmodeNum * barheight));// size of slider
																									// labels
		strainPanel.setBackground(Color.DARK_GRAY);
		strainPanel.setLayout(new GridLayout(strainmodeNum, 2, 0, 0));
		strainmodeSlider = new JSlider[strainmodeNum];// creates the array of slider bars
		strainmodeSliderLabel = new JLabel[strainmodeNum];// creates array of labels
		strainmodeSliderVal = new double[strainmodeNum];
		for (int m = 0; m < strainmodeNum; m++)// iterate through array of slider bars
		{
			strainmodeSliderLabel[m] = new JLabel();// add label to left panel
			strainmodeSliderLabel[m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of slider
																									// labels
			strainmodeSliderLabel[m].setBackground(Color.DARK_GRAY);// color labels
			strainmodeSliderLabel[m].setForeground(Color.WHITE);// color labels
			strainmodeSliderLabel[m].setHorizontalAlignment(JLabel.LEFT);
			strainmodeSliderLabel[m].setVerticalAlignment(JLabel.CENTER);

			strainmodeSlider[m] = new JSlider(JSlider.HORIZONTAL, -(int) sliderMax, (int) sliderMax,
					(int) ((strainmodeInitAmp[m] / strainmodeMaxAmp[m]) * sliderMax));// put a slider bar in the ith
																						// slot
			strainmodeSlider[m].setPreferredSize(new Dimension(sliderWidth, barheight));
			strainmodeSlider[m].setBackground(Color.DARK_GRAY);// set the slider bar's color
			strainmodeSlider[m].addChangeListener(this);// add to it a listener for slider value changes
			strainmodeSlider[m].setFocusable(false);

			strainPanel.add(strainmodeSlider[m]);// add slider bar to left of modePanel
			strainPanel.add(strainmodeSliderLabel[m]);// add slider bar's label to right of modePanel

			sliderPanel.add(strainPanel);// add this modePanel to scrollPanel
		}

		// irrepTitlePanel
		JLabel irrepTitle = new JLabel("Single-Irrep Master Amplitudes");
		irrepTitle.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
		irrepTitle.setBackground(Color.LIGHT_GRAY);// color labels
		irrepTitle.setForeground(Color.WHITE);
		irrepTitle.setHorizontalAlignment(JLabel.CENTER);
		irrepTitle.setVerticalAlignment(JLabel.CENTER);

		JPanel irrepTitlePanel = new JPanel();// make a new panel to hold color/checkboxes and label
		irrepTitlePanel.setPreferredSize(new Dimension(sliderPanelWidth, barheight));
		irrepTitlePanel.setLayout(new GridLayout(1, 1, 0, 0));
		irrepTitlePanel.setBackground(Color.LIGHT_GRAY);
		irrepTitlePanel.add(irrepTitle);

		sliderPanel.add(irrepTitlePanel);

		// irrepPanel
		irrepPanel = new JPanel();
		irrepPanel.setPreferredSize(new Dimension(sliderPanelWidth, numIrreps * barheight));// size of slider labels
		irrepPanel.setBackground(Color.LIGHT_GRAY);
		irrepPanel.setLayout(new GridLayout(numIrreps, 2, 0, 0));
		irrepmodeSlider = new JSlider[numIrreps];// creates the array of slider bars
		irrepmodeSliderLabel = new JLabel[numIrreps];// creates array of labels
		irrepmodeSliderVal = new double[numIrreps];
		for (int m = 0; m < numIrreps; m++)// iterate through array of slider bars
		{
			irrepmodeSliderLabel[m] = new JLabel();// add label to left panel
			irrepmodeSliderLabel[m].setPreferredSize(new Dimension(sliderLabelWidth, barheight));// size of slider
																									// labels
			irrepmodeSliderLabel[m].setBackground(Color.LIGHT_GRAY);// color labels
			irrepmodeSliderLabel[m].setForeground(Color.WHITE);// color labels
			irrepmodeSliderLabel[m].setHorizontalAlignment(JLabel.LEFT);
			irrepmodeSliderLabel[m].setVerticalAlignment(JLabel.CENTER);

			irrepmodeSlider[m] = new JSlider(JSlider.HORIZONTAL, 0, (int) sliderMax, sliderMax);// put a slider bar in
																								// the ith slot
			irrepmodeSlider[m].setPreferredSize(new Dimension(sliderWidth, barheight));
			irrepmodeSlider[m].setBackground(Color.LIGHT_GRAY);// set the slider bar's color
			irrepmodeSlider[m].addChangeListener(this);// add to it a listener for slider value changes
			irrepmodeSlider[m].setFocusable(false);

			irrepPanel.add(irrepmodeSlider[m]);// add slider bar to left of modePanel
			irrepPanel.add(irrepmodeSliderLabel[m]);// add slider bar's label to right of modePanel

			sliderPanel.add(irrepPanel);// add this modePanel to scrollPanel
		}
	}

	/** calculates the parent atom colors */
	public void setColors(boolean simpleColor) {
		double k;
		for (int t = 0; t < numTypes; t++)// iterate over types of atoms
		{
			if (simpleColor)
				k = 1.0 * atomTypeUnique[t] / numUniques;
			else
				k = 1.0 * t / numTypes;
			color[t] = new Color(Color.HSBtoRGB((float) k, (float) 1.0, (float) 0.95));
			// makes a rainbow of equally spaced colors according to number of atoms
			scalarcolor[t] = new Color(Color.HSBtoRGB((float) k, (float) 1.0, (float) 0.80));
			// makes a rainbow of equally spaced colors according to number of atoms
			magcolor[t] = new Color(Color.HSBtoRGB((float) k, (float) 1.0, (float) 0.65));
			// makes a rainbow of equally spaced colors according to number of atoms
			rotcolor[t] = new Color(Color.HSBtoRGB((float) k, (float) 1.0, (float) 0.55));
			// makes a rainbow of equally spaced colors according to number of atoms
			ellipcolor[t] = new Color(Color.HSBtoRGB((float) k, (float) 1.0, (float) 0.40));
			// makes a rainbow of equally spaced colors according to number of atoms
		}
	}

	/** resets the background colors of the panel components */
	public void recolorPanels() {
		for (int t = 0; t < numTypes; t++) // Reset the SliderBarPanel colors.
		{
			typeLabel[t].setBackground(color[t]);// color labels
			typeNamePanel[t].setBackground(color[t]);
			typeDataPanel[t].setBackground(color[t]);
			for (int s = 0; s < numSubTypes[t]; s++) {
				subTypeBox[t][s].setBackground(color[t]);
				subTypeLabel[t][s].setBackground(color[t]);
			}

			dispPanel[t].setBackground(color[t]);
			for (int m = 0; m < dispmodePerType[t]; m++)// iterate through array of slider bars
			{
				dispmodeSliderLabel[t][m].setBackground(color[t]);// color labels
				dispmodeSlider[t][m].setBackground(color[t]);// set the slider bar's color
			}

			scalarPanel[t].setBackground(scalarcolor[t]);
			for (int m = 0; m < scalarmodePerType[t]; m++)// iterate through array of slider bars
			{
				scalarmodeSliderLabel[t][m].setBackground(scalarcolor[t]);// color labels
				scalarmodeSlider[t][m].setBackground(scalarcolor[t]);// set the slider bar's color
			}

			magPanel[t].setBackground(magcolor[t]);
			for (int m = 0; m < magmodePerType[t]; m++)// iterate through array of slider bars
			{
				magmodeSliderLabel[t][m].setBackground(magcolor[t]);// color labels
				magmodeSlider[t][m].setBackground(magcolor[t]);// set the slider bar's color
			}

			rotPanel[t].setBackground(rotcolor[t]);
			for (int m = 0; m < rotmodePerType[t]; m++)// iterate through array of slider bars
			{
				rotmodeSliderLabel[t][m].setBackground(rotcolor[t]);// color labels
				rotmodeSlider[t][m].setBackground(rotcolor[t]);// set the slider bar's color
			}

			ellipPanel[t].setBackground(ellipcolor[t]);
			for (int m = 0; m < ellipmodePerType[t]; m++)// iterate through array of slider bars
			{
				ellipmodeSliderLabel[t][m].setBackground(ellipcolor[t]);// color labels
				ellipmodeSlider[t][m].setBackground(ellipcolor[t]);// set the slider bar's color
			}
		}

	}

	public static final String ZEROES = "000000000000";
	public static final String BLANKS = "            ";

	public static String mytoString(double val, int n, int w) {
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

	public void updateForApp() {
		// TODO Auto-generated method stub
		
	}

	public void setApp(IsoApp app) {
		this.app = app;
	}

} // end the main class
