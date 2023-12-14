/**
 *  David Tanner
 *  April 2005
 * 

/**
 11111111111111111111111111111111111111111111111111111111111111111111111111111111
 1																				1
 1 In the first section we import needed classes and instantiate the variables. 1
 1																				1
 11111111111111111111111111111111111111111111111111111111111111111111111111111111
 */

package org.byu.isodistort;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import org.byu.isodistort.local.IsoApp;
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.Vec;
import org.byu.isodistort.render.Geometry;
import org.byu.isodistort.render.Material;
// import org.byu.isodistort.render.Matrix;
import org.byu.isodistort.render.RenderPanel3D;

public class IsoDistortApp extends IsoApp implements Runnable, KeyListener {

	public IsoDistortApp() {
		super(APP_ISODISTORT);
	}
	
	protected boolean isRunning;
	
	// Variables that the user may want to adjust
	/** Cell-edge tube parameters */
	int numBondSides = 6, numCellSides = 6, numArrowSides = 8, ballRes = 4;
	/** Focal length for renderer. */
	final static double fl = 10;
	/** scale everything down to take out perspective */
	double perspectivescaler = (double) 1 / 100;
	/**
	 * Decimal multiplier to make bond radius and cell radius fractions of atom
	 * radius
	 */
	double bondMultiplier = 0.1, axesMultiplier1 = 0.2, axesMultiplier2 = 0.3;
	double cellMultiplier = 0.25, momentMultiplier = 0.4, rotationMultiplier = 0.35;

//	Other global variables.
	/** Initially show bonds or not */
	boolean showBonds0 = true, showAtoms0 = true, showCells0 = true, showAxes0 = false;
	/** Currently show bonds or not */
	boolean showBonds, showAtoms, showCells, showAxes;
	/** Which type of view direction: superHKL, superUVW, parentHKL, parentUVW */
	int viewType;
	/** Longest diagonal of super cell. */
	double scdSize;
	/** Field of view for renderer. It is a function of size. */
	double fov;
	/** Rounds mode amplitudes to three decimal places. */
	DecimalFormat round = new DecimalFormat("0.###");
	/**
	 * A geometry for rendering a part of the crystal. Each geometry can hold one or
	 * more shapes. spheres and atoms hold all the atoms; cells holds all 24
	 * cylinders for the parent and super cell.
	 */
	Geometry spheres, bonds, cells, axes;

// Global variables that pertain to atom, cell and bond properties	
	/**
	 * Array (not by type) of atom coordinates for whichAtomsBond to refer to. [atom
	 * number][x, y, z]
	 */
	double[][] atomCoordInfo;
	/** Array (not by type) of atom radii. [Atom number][radius] */
	double[] atomRadiusInfo;
	/**
	 * Array (not by type) of atom magnetic moments in Bohr magnetons. [Atom
	 * number][magnitude, X-angle, Y-angle]
	 */
	double[][] atomMagneticMomentInfo;
	/**
	 * Array (not by type) of pivot-atom rotations in radians. [Atom
	 * number][magnitude, X-angle, Y-angle]
	 */
	double[][] atomRotatonInfo;
	/**
	 * Final information needed to render bonds [atom number][xwidth, ywidth,
	 * zwidth, rotaxis-x, rotaxis-y, rotaxis-z, theta]
	 */
	double[][] atomEllipseInfo;
	/**
	 * Final information needed to render bonds [bond number][x, y, z, x-angle
	 * orientation, y-angle orientation, length, displayflag]
	 */
	double[][] bondInfo;
	/**
	 * Final information needed to render parent cell (first 12) and super cell (last 12). 
	 * [edge number][x, y, z, x-angle orientation, y-angle orientation, length, displayflag]
	 */
	double[][] cellInfo;
	/**
	 * Final information needed to render bonds [bond number][x, y, z, x-angle ,
	 * y-angle, length, displayflag]
	 */
	double[][] axesInfo;

	// Global variables that are related to material properties.
	/** Materials for coloring bonds and cells. */
	Material bondMaterial, parentCellMaterial, superCellMaterial, xAxisMaterial, yAxisMaterial, zAxisMaterial;
	/** Array of materials for coloring atoms[type][subtype][regular,highlighted] */
	Material[][] subMaterial;

	/** Check boxes for zoom, spin, anim toggles */
	JCheckBox aBox, bBox, cBox, spinBox, colorBox, animBox, axesBox;
	/** Buttons for mouse controls */
	JRadioButton nButton, xButton, yButton, zButton, zoomButton;
	/** Buttons to use super or parent cell for view vectors */
	JRadioButton superHKL, superUVW, parentHKL, parentUVW;
	/** Text fields for inputting viewing angles */
	JTextField uView, vView, wView;
	private RenderPanel3D rp;

	/**
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 * 2 2 2 In the second section we initialize everything on the applet that we
	 * want. 2 2 2
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 */

	@Override
	protected void init() {
		initializePanels();
		setVariables(readFile());
	}
	

	@Override
	protected void setRenderer() {
		rp = new RenderPanel3D(this);
		rp.setPreferredSize(drawPanel.getSize());
		rp.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add(rp, BorderLayout.CENTER);
		rp.addKeyListener(this);
		initMaterials();
		spheres = rp.world.add();
		bonds = rp.world.add();
		cells = rp.world.add();
		axes = rp.world.add();
		initAtoms();
		initBonds();
		initCells();
		initAxes();
		buildControls();
		//viewFocused = true;
		// the total area of the applet																						// // (e.g. 1024x512)
		variables.initPanels(sliderPanel, controlPanel);

//and below we initialize the applet part of the panel.  Because crystalAppletHTML
//extends renderApplet (in the render package) the remainning code in this initialization
//section actually refers to renderApplet.*/

		recalcABC();
		// do this first to get size
		
		recalcMaterials();

		// Set background color, field of view and focal length of renderer
		rp.initializeSettings(perspectivescaler, scdSize);

// BH this is taken care of in updateDisplay now
// that Variables.isChanged is set TRUE initially.
//
//		// Add the atoms and bonds and cells to applet
//		renderAtoms();// add the atoms to the renderer
//		renderBonds();// add the bonds to the renderer
//		renderCells();// add the cells to the renderer
//		renderAxes();// add the axes to the renderer
	}

	@Override
	protected void frameResized() {
		needsRecalc = true;
		updateDisplay();
	}

	@Override
	protected boolean setVariables(String dataString) {
		try {
			if (dataString != null)
				variables = new Variables(this, dataString, false);

		} catch (Exception e) {
			JOptionPane.showMessageDialog(drawPanel, "Error reading input data " + e.getMessage());
			return false;
		}
		return true;
	}

	/**
	 * Animation amplitude and phase.
	 */
	boolean isRecalcMat = false;
	boolean isAnimate = false;
	boolean isSimpleColor = false;
	double animPhase = Math.PI / 2;
	double animAmp = 1;
//	boolean isFocused = true; // is the Applet in focus? If not, stop updating the display.
//	boolean viewFocused = false; // are the viewDir fields being accessed?

	@Override
	public void updateDisplay() {
		if (isAdjusting)
			return;
		isAdjusting = true;
// BH if we are here, the frame must have focus
// this was for when there was a brute-force 20-ms thread running the painting
// now Java does it for us
//
//		boolean v = viewFocused;
//		/**
//		 * @j2sNative v = true;
//		 */
//		{
//		}
//		isFocused = v || this.hasFocus();
//		// BH: SwingJS isn't reporting this properly
//
//		// Stops display updates not in focus.
//		// Includes the applet focus and the focusable viewDir fields
//		// which would otherwise stop the display when accessed.

		if (variables.isChanged) {
			needsRecalc = true;
			variables.isChanged = false;
		}

//		System.out.println("IDA is focused " + isFocused);
//		// BH no longer necessary?
//		if (isFocused) // if in focus
//		{
		rp.updateForDisplay(null);
		if (isAnimate) {
			animPhase += 2 * Math.PI / (5 * rp.getFrameRate());
			animPhase = animPhase % (2 * Math.PI);
			animAmp = Math.pow(Math.sin(animPhase), 2);
			variables.setAnimationAmplitude(animAmp);
			needsRecalc = true;
		}

		if (isRecalcMat) {
			recalcMaterials();
			isRecalcMat = false;
		}
		if (needsRecalc) {
			// virtually all the time is here:
			recalcABC();
			renderAtoms();
			renderBonds();
			renderCells();
			renderAxes();
			needsRecalc = false;
		}
//		}
		isAdjusting = false;
		rp.updateForDisplay(drawPanel);
	}

	/**
	 * 333333333333333333333333333333333333333333333333333333333333333333333333333 3
	 * 3 3 The third section contains all the methods that instantiate the shapes. 3
	 * 3 3
	 * 333333333333333333333333333333333333333333333333333333333333333333333333333
	 */

	/**
	 * initAtoms initializes the typeless atom array.
	 */
	private void initAtoms() {
		showAtoms = showAtoms0;
		atomCoordInfo = new double[variables.numAtoms][3];
		atomRadiusInfo = new double[variables.numAtoms];
		atomMagneticMomentInfo = new double[variables.numAtoms][3];
		atomRotatonInfo = new double[variables.numAtoms][3];
		atomEllipseInfo = new double[variables.numAtoms][7];
		spheres.clear(0);
		for (int q = 0, t = 0; t < variables.numTypes; t++)
			for (int s = 0; s < variables.numSubTypes[t]; s++)
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++, q++) {
					spheres.add();
					spheres.child(q).add().ball(ballRes).setMaterial(subMaterial[t][s]);
					spheres.child(q).add().arrow(numArrowSides).setMaterial(subMaterial[t][s]);
					spheres.child(q).add().arrow(numArrowSides).setMaterial(subMaterial[t][s]);
				}
	}

	/**
	 * initBonds initializes the bond array.
	 */
	private void initBonds() {
		showBonds = showBonds0;
		int n = variables.numBonds;
		bondInfo = new double[n][7];
		bonds.clear(0);
		for (int b = 0; b < n; b++)
			bonds.add().tube(numBondSides).setMaterial(bondMaterial);
	}

	/**
	 * initCells initializes the cell array.
	 */
	private void initCells() {
		showCells = showCells0;
		cellInfo = new double[24][7];
		cells.clear(0);
		for (int c = 0; c < 24; c++) {
			cells.add().cylinder(numCellSides).setMaterial(c < 12 ? parentCellMaterial : superCellMaterial);
		}
	}

	/**
	 * initAxes initializes the axes array.
	 */
	private void initAxes() {
		showAxes = showAxes0;
		axesInfo = new double[6][7];
		axes.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axes.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axes.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
		axes.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axes.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axes.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
	}

	/**
	 * setMaterials instantiates an array of colors for the atoms. These methods are
	 * from the render package
	 */
	private void initMaterials() {
		parentCellMaterial = rp.newMaterial();
		superCellMaterial = rp.newMaterial();
		// parent cell slightly red
		parentCellMaterial.setColor(.8, .5, .5, 1.5, 1.5, 1.5, 20, .30, .30, .30);
		// super cell slightly blue
		superCellMaterial.setColor(.5, .5, .8, 1.5, 1.5, 1.5, 20, .30, .30, .30);

		bondMaterial = rp.newMaterial();
		bondMaterial.setColor(0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 20, 0.2, 0.2, 0.2);// bonds are black
		
		xAxisMaterial = rp.newMaterial();
		yAxisMaterial = rp.newMaterial();
		zAxisMaterial = rp.newMaterial();
		xAxisMaterial.setColor(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20, 0.0, 0.0, 0.0);// bonds are black
		yAxisMaterial.setColor(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 20, 0.5, 0.5, 0.5);// bonds are black
		zAxisMaterial.setColor(0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 20, 0.25, 0.25, 0.25);// bonds are black		

		subMaterial = new Material[variables.numTypes][];

		// Create the subMaterial array;
		for (int t = 0, nt = variables.numTypes; t < nt; t++)// iterate over types of atoms
		{
			subMaterial[t] = new Material[variables.numSubTypes[t]];
			for (int s = 0, nst = variables.numSubTypes[t]; s < nst; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = rp.newMaterial();
		}
	}

	final static int X = 0, Y = 1, L = 2, Z = 2, RX = 3, RY = 4, ZSCALE = 5;



	private static final int VIEW_TYPE_SUPER_HKL  = 1;
	private static final int VIEW_TYPE_SUPER_UVW  = 2;
	private static final int VIEW_TYPE_PARENT_HKL = 3;
	private static final int VIEW_TYPE_PARENT_UVW = 4;


	public void renderCells(Geometry child, double[] cellInfo, double xyScale) {
		rp.push();
		rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);
		// make everything smaller to take out
		// perspective
		rp.translate(cellInfo[X], cellInfo[Y], cellInfo[Z]);
		rp.rotateY(cellInfo[RY]);
		rp.rotateX(cellInfo[RX]);
		rp.scale(xyScale, xyScale, cellInfo[ZSCALE] / 2.0);
		rp.transform(child);
		rp.pop();
	}

	/**
	 * 44444444444444444444444444444444444444444444444444444444444444444444 4 4 4
	 * The fourth section contains the methods for animating the modes. 4 4 4
	 * 44444444444444444444444444444444444444444444444444444444444444444444
	 */

	/**
	 * renderAtoms iterates through all the atoms and puts them into the geometry
	 * which will be rendered as our crystal.
	 */
	private void renderAtoms() {
		for (int t = 0, q = 0; t < variables.numTypes; t++) {
			for (int s = 0; s < variables.numSubTypes[t]; s++) {
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++) {					
					renderAtom(t, s, a, q++);
				}
			}
		}
	}

	private void renderAtom(int t, int s, int a, int q) {
		renderScaledAtom(spheres.child(q), atomCoordInfo[q], atomRadiusInfo[q] * variables.atomMaxRadius);
		renderEllipsoid(spheres.child(q).child(0), atomEllipseInfo[q], 1/Math.sqrt(variables.defaultUiso));
		renderAtomChild(spheres.child(q).child(1), atomMagneticMomentInfo[q], momentMultiplier, variables.angstromsPerMagneton);
		renderAtomChild(spheres.child(q).child(2), atomRotatonInfo[q], rotationMultiplier, variables.angstromsPerRadian);
	}

	private void renderScaledAtom(Geometry child, double[] xyz, double r) {
		rp.push();
		rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);
		rp.translate(xyz);
		rp.scale(r, r, r);
		rp.transform(child);
		rp.pop();
	}

	private void renderEllipsoid(Geometry child, double[] info, double f) {
		rp.push();
		rp.scale(info[0] * f, info[1] * f, info[2] * f);
		rp.transform(child);
		rp.pop();
	}

	private void renderAtomChild(Geometry child, double[] info, double multiplier, double scale) {
		rp.push();
		rp.rotateY(info[Y]);// y-angle orientation of arrow number q
		rp.rotateX(info[X]);// x-angle orientation of arrow number q
		double d = (Math.abs(info[L]) > 0.1 ? multiplier : 0);
		rp.scale(d, d, 0.62 + info[L] * scale);
		// The factor
		// of
		// 0.62
		// hides
		// the zero-length moments
		// just inside the surface
		// of the spheres.
		rp.transform(child);
		rp.pop();
	}


	/**
	 * renderBonds iterates through all the bonds and puts them into the geometry.
	 * It deletes an old bond and then creates a new one.
	 */
	private void renderBonds() {
		/** Convenient constants for accessing specified element of array */
		int x = 0, y = 1, z = 2, X = 3, Y = 4;
		double temp, hide = 1;
		for (int b = 0; b < variables.numBonds; b++) {
			// iterate over bonds
			rp.push();// new matrix
			{
				rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);
				// make everything smaller to take out perspective
				rp.translate(bondInfo[b][x], bondInfo[b][y], bondInfo[b][z]);
				// position the atom at (x,y,z)
				rp.rotateY(bondInfo[b][Y]);
				// y-angle orientation of bond number b
				rp.rotateX(bondInfo[b][X]);
				// x-angle orientation of bond number b
				temp = variables.atomMaxRadius * bondMultiplier;
				hide = (bondInfo[b][6] <= 0.5 ? 0.001 : 1);
				rp.scale(hide * temp, hide * temp, hide * bondInfo[b][5] / 2.0);
				// The first two indicies are the
				// cross-section of the bond which is a
				// fraction of the atomRadius whereas,
				// the
				// third index is the length of the tube
				// and
				// it is calculated in paramReader.
				rp.transform(bonds.child(b));
				// transform and add this bond
			}
			rp.pop();
		}
	}

	/**
	 * renderCells iterates through both the parent and super cell and puts them
	 * into the geometry which will be rendered as our crystal. It works the same as
	 * renderBonds() but adds all sides of both cells as one single shape: "cells".
	 */
	private void renderCells() {
		double xyScale = variables.atomMaxRadius * cellMultiplier;
		for (int c = 0; c < 24; c++) {
			renderCells(cells.child(c), cellInfo[c], xyScale);
		}
	}

	private void renderAxes() {
		int x = 0, y = 1, z = 2, X = 3, Y = 4;
		double temp;
		for (int axis = 0; axis < 6; axis++) {
			rp.push();
			{
				rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);
				rp.translate(axesInfo[axis][x], axesInfo[axis][y], axesInfo[axis][z]);
				// position the atom at (x,y,z)
				rp.rotateY(axesInfo[axis][Y]);
				// y-angle orientation of bond number b
				rp.rotateX(axesInfo[axis][X]);
				// x-angle orientation of bond number b
				if (axis > 2)
					temp = variables.atomMaxRadius * axesMultiplier1;
				else
					temp = variables.atomMaxRadius * axesMultiplier2;
				rp.scale(temp, temp, axesInfo[axis][5] / 2.0);
				// The first two indices are the cross-section, which is a
				// fraction of the atomRadius, whereas the third index is the
				// length.
				rp.transform(axes.child(axis));

			}
			rp.pop();
		}
	}

	/** recalculates structural distortions and bond configurations. */
	private void recalcABC() {
		double[][] paxesbegs = new double[3][3];
		double[][] paxesends = new double[3][3];
		double[][] saxesbegs = new double[3][3];
		double[][] saxesends = new double[3][3];
		double[] extent = new double[3];
		double[] tempvec = new double[3];
		double[] newcoord = new double[3];
		double[] newmag = new double[3];
		double[] newrot = new double[3];
		double[] newellip = new double[6];
		double[][] tempmat = new double[3][3];
		double[][] tempmat2 = new double[3][3];

		variables.readSliders();
		variables.recalcDistortion();

		// calculate cellInfo (in bond format) associated with each of 12 edges
		/**
		 * Hard coded array specifying which parent cell verticies should be connected
		 * to render unit cell. [link number][vertex 1, vertex 2]
		 */
		int[][] buildCell = new int[12][2];
		buildCell[0][0] = 0;
		buildCell[0][1] = 1;
		buildCell[1][0] = 2;
		buildCell[1][1] = 3;
		buildCell[2][0] = 4;
		buildCell[2][1] = 5;
		buildCell[3][0] = 6;
		buildCell[3][1] = 7;
		buildCell[4][0] = 0;
		buildCell[4][1] = 2;
		buildCell[5][0] = 1;
		buildCell[5][1] = 3;
		buildCell[6][0] = 4;
		buildCell[6][1] = 6;
		buildCell[7][0] = 5;
		buildCell[7][1] = 7;
		buildCell[8][0] = 0;
		buildCell[8][1] = 4;
		buildCell[9][0] = 1;
		buildCell[9][1] = 5;
		buildCell[10][0] = 2;
		buildCell[10][1] = 6;
		buildCell[11][0] = 3;
		buildCell[11][1] = 7;
		for (int c = 0; c < 24; c++) {
			double[][] vertices = (c < 12 ? variables.parentCellVertices : variables.superCellVertices);
			Vec.pairtobond(vertices[buildCell[c % 12][0]], vertices[buildCell[c % 12][1]], cellInfo[c]);
		}

		for (int q = 0, t = 0; t < variables.numTypes; t++)
			for (int s = 0; s < variables.numSubTypes[t]; s++)
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++, q++) {
					atomRadiusInfo[q] = variables.atomFinalOcc[t][s][a];
					Vec.set3(newcoord, variables.atomFinalCoord[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newcoord, tempvec); // typeless atom in cartesian coords

					for (int i = 0; i < 3; i++) // recenter in Applet coordinates
						atomCoordInfo[q][i] = tempvec[i] - variables.sCenterCart[i];

					Vec.set3(newmag, variables.atomFinalMag[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newmag, tempvec);
					Vec.calculatearrow(tempvec, atomMagneticMomentInfo[q]);

					Vec.set3(newrot, variables.atomFinalRot[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newrot, tempvec);
					Vec.calculatearrow(tempvec, atomRotatonInfo[q]);

//					Ellipsoid work
					Vec.copy(variables.atomFinalEllip[t][s][a], newellip);
					Vec.voigt2matrix(newellip, tempmat);
					Vec.matdotmat(variables.sBasisCart, tempmat, tempmat2);
					Vec.matcopy(tempmat2, tempmat);
					Vec.matdotmat(tempmat, variables.sBasisCartInverse, tempmat2);// ellipsoid in cartesian coords --
																					// not safe
					// to use tempmat in 1st and 3rd arguments
					// simultaneously.
					Vec.matcopy(tempmat2, tempmat);
//			        System.out.println ("ellipmat3: "+t+", "+s+", "+a+", "+tempmat[0][0]+", "+tempmat[0][1]+", "+tempmat[0][2]+", "+tempmat[1][0]+", "+tempmat[1][1]+", "+tempmat[1][2]+", "+tempmat[2][0]+", "+tempmat[2][1]+", "+tempmat[2][2]);
					Vec.calculateellipstuff(tempmat, atomEllipseInfo[q]); // ellipsoidInfo array filled
				}

//		for (q=0; q<rd.numAtoms; q++)
//	        System.out.println ("elliplist: "+q+", "+atomellipInfo[q][0]+", "+atomellipInfo[q][1]+", "+atomellipInfo[q][2]+", "+atomellipInfo[q][3]+", "+atomellipInfo[q][4]+", "+atomellipInfo[q][5]+", "+atomellipInfo[q][6]);

		// calculate the new bondInfo in bond format
		for (int b = 0; b < variables.numBonds; b++) // calculate bondInfo(x-cen, y-cen, z-cen, thetaX, thetaY, length)
		{
			Vec.pairtobond(atomCoordInfo[variables.whichAtomsBond[b][0]], atomCoordInfo[variables.whichAtomsBond[b][1]],
					bondInfo[b]);
			if (bondInfo[b][5] >= variables.maxBondLength
					|| atomRadiusInfo[variables.whichAtomsBond[b][0]] <= variables.minBondOcc
					|| atomRadiusInfo[variables.whichAtomsBond[b][1]] <= variables.minBondOcc) {
				bondInfo[b][6] = 0.0;
				bonds.child(b).setEnabled(false);
			} else {
				bonds.child(b).setEnabled(true);
			}
		}

		// calculate the parent and supercell coordinate axes in bond format
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = variables.pBasisCart[i][axis];
			Vec.normalize(tempvec);
			for (int i = 0; i < 3; i++) {
				extent[i] = variables.pOriginCart[i] - variables.sCenterCart[i] + variables.pBasisCart[i][axis];
				paxesbegs[axis][i] = extent[i] + 2.0 * variables.atomMaxRadius * tempvec[i];
				paxesends[axis][i] = extent[i] + 3.5 * variables.atomMaxRadius * tempvec[i];
				;
			}
			Vec.pairtobond(paxesbegs[axis], paxesends[axis], axesInfo[axis]);

			for (int i = 0; i < 3; i++)
				tempvec[i] = variables.sBasisCart[i][axis];
			Vec.normalize(tempvec);
			for (int i = 0; i < 3; i++) {
				extent[i] = -variables.sCenterCart[i] + variables.sBasisCart[i][axis];
				saxesbegs[axis][i] = extent[i] + 1.5 * variables.atomMaxRadius * tempvec[i];
				saxesends[axis][i] = extent[i] + 4.0 * variables.atomMaxRadius * tempvec[i];
			}
			Vec.pairtobond(saxesbegs[axis], saxesends[axis], axesInfo[axis + 3]);
		}

		// Calculate the maximum distance from applet center (used to determine FOV).
		scdSize = 0;
		for (int j = 0; j < 8; j++) {
			Vec.set3(tempvec, variables.superCellVertices[j]);
			double d = Vec.norm(tempvec);
			if (d > scdSize)
				scdSize = d;
			Vec.set3(tempvec, variables.parentCellVertices[j]);
			d = Vec.norm(tempvec);
			if (d > scdSize)
				scdSize = d;
		}
		for (int i = 0; i < variables.numAtoms; i++) {
			Vec.set3(tempvec, atomCoordInfo[i]);
			double d = Vec.norm(tempvec);
			if (d > scdSize)
				scdSize = d;
		}
		for (int axis = 0; axis < 3; axis++) {
			Vec.set3(tempvec, paxesends[axis]);
			double d = Vec.norm(tempvec);
			if (d > scdSize)
				scdSize = d;
		}
		for (int axis = 0; axis < 3; axis++) {
			Vec.set3(tempvec, saxesends[axis]);
			double d = Vec.norm(tempvec);
			if (d > scdSize)
				scdSize = d;
		}

		scdSize += 2 * variables.atomMaxRadius; 
		// this includes the width of atoms that might be at the extremes of the
		// longest cell diagonal.
	}

	/** recalculates the atom colors after a checkbox has been set. */
	private void recalcMaterials() {
		variables.setColors(isSimpleColor);
		variables.recolorPanels();
		recalcSpheres();
		recalcCells();
		recalcBonds();
		recalcAxes();
	}

	private void recalcAxes() {
		if (showAxes) {
			xAxisMaterial.setTransparency(0.0);// make the cells translucent
			yAxisMaterial.setTransparency(0.0);// make the cells translucent
			zAxisMaterial.setTransparency(0.0);// make the cells translucent
		} else {
			xAxisMaterial.setTransparency(1.0);// make the cells translucent
			yAxisMaterial.setTransparency(1.0);// make the cells translucent
			zAxisMaterial.setTransparency(1.0);// make the cells translucent
		}
	}


	private void recalcBonds() {
		if (showBonds)
			bondMaterial.setTransparency(0.0);// make the cells translucent
		else
			bondMaterial.setTransparency(1.0);// make the cells translucent
	}


	private void recalcCells() {
		if (showCells) {
			parentCellMaterial.setTransparency(0.0);// make the cells translucent
			superCellMaterial.setTransparency(0.0);// make the cells translucent
		} else {
			parentCellMaterial.setTransparency(1.0);// make the cells translucent
			superCellMaterial.setTransparency(1.0);// make the cells translucent
		}

	}


	private void recalcSpheres() {		
//		Reset the atom colors.
		double[] rgb = new double[3];
		for (int t = 0; t < variables.numTypes; t++) {
			variables.getColors(t, rgb);
			for (int s = 0; s < variables.numSubTypes[t]; s++) {
				// 1 means selected
				if (!variables.isSubTypeSelected(t, s))
					subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], 0.3, 0.3, 0.3, 1, .0001, .0001, .0001);
				// makes the atom
				// color same as
				// above type
				// color
				else {
					double k = (double) 0.1 + 0.8 * s / variables.numSubTypes[t];
//					subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], k, k, k, 1, k, k, k); 
					// graduated color scheme
					subMaterial[t][s].setColor(0, 0, 0, k, k, k, 1, k, k, k);
				}
				subMaterial[t][s].setTransparency(showAtoms ? 0 : 1);
			}
		}
	}


	/** resets the viewing direction without changing anything else */
	void resetViewDirection(int type) {
		if (type >= 0)
			viewType = type;
		/** The hkl or uvw view direction indices in lattice coordinates */
		double[] viewIndices = new double[3];
		/** The view direction in cartesian coordinates */
		double[] viewDir = new double[3];
		/** Variables for viewer computation */
		double xV = 0, yV = 0, zV = 0, tX, tY;
		/** Temporary 3x3 matrix */
		double[][] tempmat = new double[3][3];
		/**
		 * Array of vectors defining recip parent cell basis. [vector number][x, y, z]
		 */
		double[][] recipparentCell = new double[3][3];
		/**
		 * Array of vectors defining recip super cell basis. [vector number][x, y, z]
		 */
		double[][] recipsuperCell = new double[3][3];

		viewIndices[0] = Double.parseDouble(uView.getText());
		viewIndices[1] = Double.parseDouble(vView.getText());
		viewIndices[2] = Double.parseDouble(wView.getText());

		Vec.matinverse(variables.sBasisCart, tempmat);
		Vec.mattranspose(tempmat, recipsuperCell);
		Vec.matinverse(variables.pBasisCart, tempmat);
		Vec.mattranspose(tempmat, recipparentCell);

		switch (viewType) {
		case VIEW_TYPE_SUPER_HKL:
			Vec.mattranspose(recipsuperCell, tempmat);
			break;
		case VIEW_TYPE_SUPER_UVW:
			Vec.mattranspose(variables.sBasisCart, tempmat);
			break;
		case VIEW_TYPE_PARENT_HKL:
			Vec.mattranspose(recipparentCell, tempmat);
			break;
		case VIEW_TYPE_PARENT_UVW:
			Vec.mattranspose(variables.pBasisCart, tempmat);
			break;
		}
		Vec.matdotvect(tempmat, viewIndices, viewDir);

		if (Vec.norm(viewDir) > 0.000001) {
			Vec.normalize(viewDir);
			xV = viewDir[0];
			yV = viewDir[1];
			zV = viewDir[2];

			tX = Math.asin(yV);
			if (Math.abs(Math.cos(tX)) < 0.000001)
				tY = 0.0;
			else {
				if (Math.abs(zV) < 0.000001)
					tY = -Math.PI / 2 * (xV / Math.abs(xV)); // I would prefer to use signum here , but some OS's have
																// trouble with it.
				else
					tY = -Math.atan2(xV, zV);
			}

			rp.clearAngles();
			rp.setCamera(tY, tX);
			
			updateDisplay();
			// y-angle rotation first then x-angle rotation
//			System.out.println("viewDir: "+viewDir[0]+", "+viewDir[1]+", "+viewDir[2]);
//			System.out.println("xV: "+xV+", yV: "+yV+", zV: "+zV);
//			System.out.println("sigma(tx): "+tX+", theta(ty): "+tY);
//			requestFocus(); // trick for recovering from loss of applet focus
		}
	}

	/**
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 * 5 5 5 The fifth section has the methods that listen for the sliderbars, the
	 * keyboard and the viewing buttons. 5 5 5
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 */

	@Override
	protected void dispose() {
		rp.removeKeyListener(this);
		rp.dispose();
		rp = null;
		super.dispose();
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {
		case 'r':
		case 'R':
			rp.clearAngles();
			animPhase = Math.PI / 2;
			animAmp = 1;
			variables.resetSliders();
			variables.readSliders();
			variables.recalcDistortion();
			showAtoms = showAtoms0;
			aBox.setSelected(showAtoms);
			showBonds = showBonds0;
			bBox.setSelected(showBonds);
			showCells = showCells0;
			cBox.setSelected(showCells);
			showAxes = showAxes0;
			axesBox.setSelected(showAxes);
			animBox.setSelected(false);
			spinBox.setSelected(false);
			colorBox.setSelected(false);
			nButton.setSelected(true);
			uView.setText("0");
			vView.setText("0");
			wView.setText("1");
			superHKL.setSelected(true);

			rp.resetView();
			variables.setSubtypesSelected(false);
			centerImage();
			break;
		case 'z':
		case 'Z':
			variables.zeroSliders();
			break;
		case 'i':
		case 'I':
			variables.resetSliders();
			break;
		case 's':
		case 'S':
			variables.toggleIrrepSliders();
			break;
		case 'n':
		case 'N':
			rp.invert();
			break;
		case 'c':
		case 'C':
			centerImage();
			break;
		}
		updateDisplay();
	}

	private void centerImage() {
		rp.clearOffsets();
		rp.push();
		{
			rp.identity();
			rp.translate(0, 0, 0);
			rp.transformWorld();
		}
		rp.pop();
	}

//	/** listens for changes in focus in the ViewDirection fields. */
//	private class focusListener implements FocusListener {
//		public void focusGained(FocusEvent event) {
//			viewFocusedouble d =true; // Record the fact that we have focus.
//			updateDisplay();
//		}
//
//		public void focusLost(FocusEvent event) {
//			viewFocused = false; // Record the fact that we don't have focus.
//		}
//	} // end class focusIt

	@Override
	public BufferedImage getImage() {
		return rp.getImage();
	}

	@Override
	protected void handleCheckBoxEvent(Object src) {
		if (isAdjusting)
			return;
		updateViewOptions();
	}

	@Override
	protected void handleRadioButtonEvent(Object src) {
		if (!((JToggleButton) src).isSelected())
			return;
		if (src == nButton) {
			rp.clearAngles();
			rp.setRotationAxis(0);
		} else if (src == xButton) {
			rp.clearAngles();
			rp.setRotationAxis(1);
		} else if (src == yButton) {
			rp.clearAngles();
			rp.setRotationAxis(2);
		} else if (src == zButton) {
			rp.clearAngles();
			rp.setRotationAxis(3);
		} else if (src == zoomButton) {
			rp.clearAngles();
			rp.setRotationAxis(4);
		} else if (src == superHKL) {
			resetViewDirection(VIEW_TYPE_SUPER_HKL);
		} else if (src == superUVW) {
			resetViewDirection(VIEW_TYPE_SUPER_UVW);
		} else if (src == parentHKL) {
			resetViewDirection(VIEW_TYPE_PARENT_HKL);
		} else if (src == parentUVW) {
			resetViewDirection(VIEW_TYPE_PARENT_UVW);
		}
		updateDisplay();

	}

	@Override
	public void updateViewOptions() {
		showAtoms = aBox.isSelected();
		showBonds = bBox.isSelected();
		showCells = cBox.isSelected();
		showAxes = axesBox.isSelected();
		boolean spin = spinBox.isSelected();
		isSimpleColor = colorBox.isSelected();
		isAnimate = animBox.isSelected();
		isRecalcMat = true;
		rp.setSpinning(spin);
		if (isAnimate || spin) {
			start();
		} else {
			variables.isChanged = true;
			updateDisplay();
		}
	}

	/**
	 * creates the components of the control panel
	 */
	private void buildControls() {

		aBox = newJCheckBox("Atoms", showAtoms);
		bBox = newJCheckBox("Bonds", showBonds);
		cBox = newJCheckBox("Cells", showCells);
		axesBox = newJCheckBox("Axes", showAxes);
		animBox = newJCheckBox("Animate", false);
		spinBox = newJCheckBox("Spin", false);
		colorBox = newJCheckBox("Color", false);
		colorBox.setVisible(variables.needSimpleColor);

		ButtonGroup xyzButtons = new ButtonGroup();
		nButton = newRadioButton("Normal", true, xyzButtons);
		xButton = newRadioButton("Xrot", false, xyzButtons);
		yButton = newRadioButton("Yrot", false, xyzButtons);
		zButton = newRadioButton("Zrot", false, xyzButtons);
		zoomButton = newRadioButton("Zoom", false, xyzButtons);

		viewType = VIEW_TYPE_SUPER_HKL;

		ButtonGroup cellButtons = new ButtonGroup();
		superHKL = newRadioButton("SupHKL", true, cellButtons);
		superUVW = newRadioButton("SupUVW", false, cellButtons);
		parentHKL = newRadioButton("ParHKL", false, cellButtons);
		parentUVW = newRadioButton("ParUVW", false, cellButtons);
		uView = newTextField("0", -10);
		vView = newTextField("0", -10);
		wView = newTextField("1", -10);

		JPanel topControlPanel = new JPanel();
		topControlPanel.setBackground(Color.WHITE);
		topControlPanel.add(nButton);
		topControlPanel.add(xButton);
		topControlPanel.add(yButton);
		topControlPanel.add(zButton);
		topControlPanel.add(zoomButton);
		topControlPanel.add(new JLabel("       "));
		topControlPanel.add(aBox);
		topControlPanel.add(bBox);
		topControlPanel.add(cBox);
		topControlPanel.add(axesBox);
		topControlPanel.add(spinBox);
		topControlPanel.add(animBox);
		topControlPanel.add(colorBox);

		JPanel botControlPanel = new JPanel();
		botControlPanel.setBackground(Color.WHITE);
		botControlPanel.add(superHKL);
		botControlPanel.add(superUVW);
		botControlPanel.add(parentHKL);
		botControlPanel.add(parentUVW);
		botControlPanel.add(new JLabel("          Direction: "));
		botControlPanel.add(uView);
		botControlPanel.add(vView);
		botControlPanel.add(wView);

		addSaveButtons((JComponent) botControlPanel);

		controlPanel.add(topControlPanel);
		controlPanel.add(botControlPanel);

		// Add listeners to a the subtype checkboxes -- don't know where else to put it.
		variables.setApp(this);
	}

	private JRadioButton newRadioButton(String label, boolean selected, ButtonGroup g) {
		JRadioButton b = new JRadioButton(label, selected);
		b.setName(++buttonID + ":" + label);
		b.setHorizontalAlignment(JRadioButton.LEFT);
		b.setVerticalAlignment(JRadioButton.CENTER);
		b.setFocusable(false);
		b.setBackground(Color.WHITE);
		b.setForeground(Color.BLACK);
		b.setVisible(true);
		b.setBorderPainted(false);
		b.addItemListener(buttonListener);
		g.add(b);
		return b;
	}

	static int buttonID = 0;
	private JCheckBox newJCheckBox(String label, boolean selected) {
		JCheckBox cb = new JCheckBox(label, selected);
		cb.setName(++buttonID + ":" + label);
		cb.setHorizontalAlignment(JCheckBox.LEFT);
		cb.setVerticalAlignment(JCheckBox.CENTER);
		cb.setFocusable(false);
		cb.setVisible(true);
		cb.setBackground(Color.WHITE);
		cb.setForeground(Color.BLACK);
		cb.addItemListener(buttonListener);
		return cb;
	}

	/**
	 * Starts the renderer thread.
	 */

	private Timer timer;

	protected void setTimer() {
		if (timer == null) {
			int delay = (/** @j2sNative true ? 50 : */
					100);

			timer = new Timer(delay, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					run();
				}

			});
			timer.setRepeats(true);
		}
	}

	public void start() {
		setTimer();
		isRunning = true;
		timer.start();
	}

	@Override
	public void run() {
		if (!isRunning)
			return;
		boolean animating = (isAnimate || rp.isSpinning());
		if (!animating && --initializing < 0)
			return;
		updateDisplay();
		if (initializing == 0 && !animating)
			stop();
	}

	/**
	 * Stops the renderer thread.
	 */
	public void stop() {
		if (timer != null) {
			isRunning = false;
			timer.stop();
		}
		timer = null;
	}

	public static void main(String[] args) {
		create("IsoDistort", args);
	}


	@Override
	protected double[][] getPerspective() {
		return rp.getPerspective();
	}


	@Override
	protected void setPerspective(double[][] params) {
		if (params == null)
			return;
		rp.setPerspective(params);
		updateDisplay();
	}


	@Override
	protected void stopSpin() {
		isAnimate = false;
		rp.setSpinning(false);
	}

	@Override
	protected void setControlsFrom(IsoApp a) {
		if (a == null)
			return;
		IsoDistortApp app = (IsoDistortApp) a;
		aBox.setSelected(app.showAtoms);
		bBox.setSelected(app.showBonds);
		cBox.setSelected(app.showCells);
		axesBox.setSelected(app.showAxes);
		colorBox.setSelected(isSimpleColor);
		// skipping spin and animation
		// skipping rotation state buttons
		superHKL.setSelected(app.superHKL.isSelected());
		superUVW.setSelected(app.superUVW.isSelected());
		parentHKL.setSelected(app.parentHKL.isSelected());
		parentUVW.setSelected(app.parentUVW.isSelected());
		uView.setText(app.uView.getText());
		vView.setText(app.vView.getText());
		wView.setText(app.wView.getText());
		variables.isChanged = true;
		updateViewOptions();
	}


	@Override
	protected void applyView() {
		resetViewDirection(-1);
	}

	
}
