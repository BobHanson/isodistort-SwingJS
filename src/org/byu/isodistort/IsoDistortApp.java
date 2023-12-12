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
import java.awt.Insets;
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
	double fl = 10;
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
	double[][] atomcoordInfo;
	/** Array (not by type) of atom radii. [Atom number][radius] */
	double[] atomradInfo;
	/**
	 * Array (not by type) of atom magnetic moments in Bohr magnetons. [Atom
	 * number][magnitude, X-angle, Y-angle]
	 */
	double[][] atommagInfo;
	/**
	 * Array (not by type) of pivot-atom rotations in radians. [Atom
	 * number][magnitude, X-angle, Y-angle]
	 */
	double[][] atomrotInfo;
	/**
	 * Final information needed to render bonds [atom number][xwidth, ywidth,
	 * zwidth, rotaxis-x, rotaxis-y, rotaxis-z, theta]
	 */
	double[][] atomellipInfo;
	/**
	 * Final information needed to render bonds [bond number][x, y, z, x-angle
	 * orientation, y-angle orientation, length, displayflag]
	 */
	double[][] bondInfo;
	/**
	 * Final information needed to render parent cell. [edge number][x, y, z,
	 * x-angle orientation, y-angle orientation, length, displayflag]
	 */
	double[][] parentCellInfo;
	/**
	 * Final information needed to render super cell. [edge number][x, y, z, x-angle
	 * orientation, y-angle orientation, length, displayflag]
	 */
	double[][] superCellInfo;
	/**
	 * Final information needed to render bonds [bond number][x, y, z, x-angle ,
	 * y-angle, length, displayflag]
	 */
	double[][] axesInfo;

	// Global variables that are related to material properties.
	/** Materials for coloring bonds and cells. */
	Material bondMaterial, parentCellMaterial, superCellMaterial, xMaterial, yMaterial, zMaterial;
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
		double fov = 2 * perspectivescaler * scdSize / fl;
		fov0 = fov;
		rp.setBgColor(1, 1, 1);// background color: white
		rp.setFOV(fov);// field of view
		rp.setFL(fl);// focal length: zoomed way out
		rp.changeFOV(fov);
		rp.setCamera(0, 0);
		// Define position and color of light source (x, y, z, r, g, b)
		double bbb = 0.38;
		rp.addLight(Double.NaN, 0,0,0,0,0);
		rp.addLight(.5, .5, .5, 1.7 * bbb, 1.7 * bbb, 1.7 * bbb);
		rp.addLight(-.5, .5, .5, bbb, bbb, bbb);
		rp.addLight(.5, -.5, .5, bbb, bbb, bbb);
		rp.addLight(-.5, -.5, .5, bbb, bbb, bbb);

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
		isRecalc = true;
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
	boolean isRecalc = true;
	boolean isRecalcMat = false;
	boolean isAnimate = false;
	boolean isSimpleColor = false;
	double animPhase = Math.PI / 2;
	double animAmp = 1;
//	boolean isFocused = true; // is the Applet in focus? If not, stop updating the display.
//	boolean viewFocused = false; // are the viewDir fields being accessed?

	private boolean isAdjusting = false;

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
			isRecalc = true;
			variables.isChanged = false;
		}

//		System.out.println("IDA is focused " + isFocused);
//		// BH no longer necessary?
//		if (isFocused) // if in focus
//		{
			if (isAnimate) {
				animPhase += 2 * Math.PI / (5 * rp.getFrameRate());
				animPhase = animPhase % (2 * Math.PI);
				animAmp = Math.pow(Math.sin(animPhase), 2);
				variables.masterSlider.setValue((int) Math.round(animAmp * variables.sliderMaxVal));
				isRecalc = true;
			}

			if (isRecalcMat) {
				recalcMaterials();
				isRecalcMat = false;
			}
			if (isRecalc) {
				// virtually all the time is here:
				recalcABC();
				renderAtoms();
				renderBonds();
				renderCells();
				renderAxes();
				isRecalc = false;
			}
			rp.updateForDisplay();
//		}
		isAdjusting = false;
		drawPanel.repaint();
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
		atomcoordInfo = new double[variables.numAtoms][3];
		atomradInfo = new double[variables.numAtoms];
		atommagInfo = new double[variables.numAtoms][3];
		atomrotInfo = new double[variables.numAtoms][3];
		atomellipInfo = new double[variables.numAtoms][7];
		spheres.clear(0);
//		for (int qq = 0; qq < variables.numAtoms; qq++)// iterate over all the types
//			spheres.delete(0);
		int q = 0;
		for (int t = 0; t < variables.numTypes; t++)// iterate over all the types
			for (int s = 0; s < variables.numSubTypes[t]; s++)
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++) {
					spheres.add();
					spheres.child(q).add().ball(ballRes).setMaterial(subMaterial[t][s]);
					spheres.child(q).add().arrow(numArrowSides).setMaterial(subMaterial[t][s]);
					spheres.child(q).add().arrow(numArrowSides).setMaterial(subMaterial[t][s]);
					q++;
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
		parentCellInfo = new double[12][7];
		superCellInfo = new double[12][7];
		cells.clear(0);
		for (int c = 0; c < 12; c++)
			cells.add().cylinder(numCellSides).setMaterial(parentCellMaterial);
		for (int c = 0; c < 12; c++)
			cells.add().cylinder(numCellSides).setMaterial(superCellMaterial);
	}

	/**
	 * initAxes initializes the axes array.
	 */
	private void initAxes() {
		showAxes = showAxes0;
		axesInfo = new double[6][7];
		axes.add().arrow(numArrowSides).setMaterial(xMaterial);
		axes.add().arrow(numArrowSides).setMaterial(yMaterial);
		axes.add().arrow(numArrowSides).setMaterial(zMaterial);
		axes.add().arrow(numArrowSides).setMaterial(xMaterial);
		axes.add().arrow(numArrowSides).setMaterial(yMaterial);
		axes.add().arrow(numArrowSides).setMaterial(zMaterial);
	}

	/**
	 * setMaterials instantiates an array of colors for the atoms. These methods are
	 * from the render package
	 */
	private void initMaterials() {
		parentCellMaterial = rp.newMaterial();
		superCellMaterial = rp.newMaterial();
		bondMaterial = rp.newMaterial();
		xMaterial = rp.newMaterial();
		yMaterial = rp.newMaterial();
		zMaterial = rp.newMaterial();
		subMaterial = new Material[variables.numTypes][];

		// Create the subMaterial array;
		for (int t = 0, nt = variables.numTypes; t < nt; t++)// iterate over types of atoms
		{
			subMaterial[t] = new Material[variables.numSubTypes[t]];
			for (int s = 0, nst = variables.numSubTypes[t]; s < nst; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = rp.newMaterial();
		}
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
		int q = 0, X = 0, Y = 1, L = 2;
		double temp, tempr, tempx, tempy, tempz;
		for (int t = 0; t < variables.numTypes; t++)
			for (int s = 0; s < variables.numSubTypes[t]; s++)// iterate over all subtypes of this type
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++)// iterate over all atoms of that type
				{
					rp.push();
					rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);
					rp.translate(atomcoordInfo[q]);
					tempr = variables.atomMaxRadius * atomradInfo[q];
					rp.scale(tempr, tempr, tempr);
					rp.transform(spheres.child(q));
					rp.pop();

					rp.push();
					tempx = (atomellipInfo[q][0] / Math.sqrt(variables.defaultUiso));
					tempy = (atomellipInfo[q][1] / Math.sqrt(variables.defaultUiso));
					tempz = (atomellipInfo[q][2] / Math.sqrt(variables.defaultUiso));
//					System.out.println("widths: "+q+", "+tempx+", "+tempy+", "+tempz);
					rp.scale(tempx, tempy, tempz);
					rp.transform(spheres.child(q).child(0));
					rp.pop();

					rp.push();
					rp.rotateY(atommagInfo[q][Y]);// y-angle orientation of arrow number q
					rp.rotateX(atommagInfo[q][X]);// x-angle orientation of arrow number q
					if (Math.abs(atommagInfo[q][L]) > 0.1)
						temp = momentMultiplier;
					else
						temp = 0;
					rp.scale(temp, temp, 0.62 + atommagInfo[q][L] * variables.angstromsPerMagneton); // The factor of
																										// 0.62
																										// hides
					// the zero-length moments
					// just inside the surface
					// of the spheres.
					rp.transform(spheres.child(q).child(1));
					rp.pop();

					rp.push();
					rp.rotateY(atomrotInfo[q][Y]);// y-angle orientation of arrow number q
					rp.rotateX(atomrotInfo[q][X]);// x-angle orientation of arrow number q
					if (Math.abs(atomrotInfo[q][L]) > 0.1)
						temp = rotationMultiplier;
					else
						temp = 0;
					rp.scale(temp, temp, 0.62 + atomrotInfo[q][L] * variables.angstromsPerRadian); 
					// The factor of 0.62																				
					// hides the
					// zero-length rotations
					// just inside the surface
					// of the spheres.
					rp.transform(spheres.child(q).child(2));
					rp.pop();

					q++;
				}
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
		int x = 0, y = 1, z = 2;
		double temp;
		for (int c = 0; c < 12; c++) {
			rp.push();
			{
				rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);// make everything smaller to take out
																					// // perspective
				rp.translate(parentCellInfo[c][x], parentCellInfo[c][y], parentCellInfo[c][z]);
				rp.rotateY(parentCellInfo[c][4]);
				rp.rotateX(parentCellInfo[c][3]);
				temp = variables.atomMaxRadius * cellMultiplier;
				rp.scale(temp, temp, parentCellInfo[c][5] / 2.0);
				rp.transform(cells.child(c));
			}
			rp.pop();
			rp.push();
			{
				rp.scale(perspectivescaler, perspectivescaler, perspectivescaler);// make everything smaller to take out
																					// perspective
				rp.translate(superCellInfo[c][x], superCellInfo[c][y], superCellInfo[c][z]);
				rp.rotateY(superCellInfo[c][4]);
				rp.rotateX(superCellInfo[c][3]);
				rp.scale(temp, temp, superCellInfo[c][5] / 2.0);
				rp.transform(cells.child(c + 12));
			}
			rp.pop();
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
		double tempscalar;
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
		for (int c = 0; c < 12; c++) {
			Vec.pairtobond(variables.parentCellVertices[buildCell[c][0]], variables.parentCellVertices[buildCell[c][1]],
					parentCellInfo[c]);
			Vec.pairtobond(variables.superCellVertices[buildCell[c][0]], variables.superCellVertices[buildCell[c][1]],
					superCellInfo[c]);
		}

		int q = 0;
		for (int t = 0; t < variables.numTypes; t++)
			for (int s = 0; s < variables.numSubTypes[t]; s++)
				for (int a = 0; a < variables.numSubAtoms[t][s]; a++) {
					atomradInfo[q] = variables.atomFinalOcc[t][s][a];
					Vec.set3(newcoord, variables.atomFinalCoord[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newcoord, tempvec); // typeless atom in cartesian coords
				
					for (int i = 0; i < 3; i++) // recenter in Applet coordinates
						atomcoordInfo[q][i] = tempvec[i] - variables.sCenterCart[i];

					Vec.set3(newmag, variables.atomFinalMag[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newmag, tempvec);
					Vec.calculatearrow(tempvec, atommagInfo[q]);

					Vec.set3(newrot, variables.atomFinalRot[t][s][a]);
					Vec.matdotvect(variables.sBasisCart, newrot, tempvec);
					Vec.calculatearrow(tempvec, atomrotInfo[q]);

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
					Vec.calculateellipstuff(tempmat, atomellipInfo[q]); // ellipsoidInfo array filled
					q++;
				}

//		for (q=0; q<rd.numAtoms; q++)
//	        System.out.println ("elliplist: "+q+", "+atomellipInfo[q][0]+", "+atomellipInfo[q][1]+", "+atomellipInfo[q][2]+", "+atomellipInfo[q][3]+", "+atomellipInfo[q][4]+", "+atomellipInfo[q][5]+", "+atomellipInfo[q][6]);

		// calculate the new bondInfo in bond format
		for (int b = 0; b < variables.numBonds; b++) // calculate bondInfo(x-cen, y-cen, z-cen, thetaX, thetaY, length)
		{
			Vec.pairtobond(atomcoordInfo[variables.whichAtomsBond[b][0]], atomcoordInfo[variables.whichAtomsBond[b][1]],
					bondInfo[b]);
			if (bondInfo[b][5] >= variables.maxBondLength
					|| atomradInfo[variables.whichAtomsBond[b][0]] <= variables.minBondOcc
					|| atomradInfo[variables.whichAtomsBond[b][1]] <= variables.minBondOcc) {
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
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
			Vec.set3(tempvec, variables.parentCellVertices[j]);
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (q = 0; q < variables.numAtoms; q++) {
			Vec.set3(tempvec, atomcoordInfo[q]);
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (int axis = 0; axis < 3; axis++) {
			Vec.set3(tempvec, paxesends[axis]);
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (int axis = 0; axis < 3; axis++) {
			Vec.set3(tempvec, saxesends[axis]);
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}

		scdSize += 2 * variables.atomMaxRadius; // this includes the width of atoms that might be at the extremes of the
		// longest cell diagonal.
	}

	/** recalculates the atom colors after a checkbox has been set. */
	private void recalcMaterials() {
		double rrr, ggg, bbb, k;

		variables.setColors(isSimpleColor);
		variables.recolorPanels();
//		Reset the atom colors.			
		for (int t = 0; t < variables.numTypes; t++)// iterate over types of atoms
		{
			rrr = variables.color[t].getRed() / 255.0;
			ggg = variables.color[t].getGreen() / 255.0;
			bbb = variables.color[t].getBlue() / 255.0;
			for (int s = 0; s < variables.numSubTypes[t]; s++)// iterate over number-of-subtypes-selected; 0 means 1 is
			// selected
			{
				if (!variables.subTypeBox[t][s].isSelected())
					subMaterial[t][s].setColor(rrr, ggg, bbb, 0.3, 0.3, 0.3, 1, .0001, .0001, .0001);// makes the atom
																										// color same as
																										// above type
																										// color
				else {
					k = (double) 0.1 + 0.8 * s / variables.numSubTypes[t];
//					subMaterial[t][s].setColor(rrr, ggg, bbb, k, k, k, 1, k, k, k); // graduated color scheme
					subMaterial[t][s].setColor(0, 0, 0, k, k, k, 1, k, k, k);
				}
				if (showAtoms)
					subMaterial[t][s].setTransparency(0.0);
				else
					subMaterial[t][s].setTransparency(1.0);
			}
		}

		parentCellMaterial.setColor(.8, .5, .5, 1.5, 1.5, 1.5, 20, .30, .30, .30);// parent cell slightly red
		superCellMaterial.setColor(.5, .5, .8, 1.5, 1.5, 1.5, 20, .30, .30, .30);// super cell slightly blue
		if (showCells) {
			parentCellMaterial.setTransparency(0.0);// make the cells translucent
			superCellMaterial.setTransparency(0.0);// make the cells translucent
		} else {
			parentCellMaterial.setTransparency(1.0);// make the cells translucent
			superCellMaterial.setTransparency(1.0);// make the cells translucent
		}

		bondMaterial.setColor(0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 20, 0.2, 0.2, 0.2);// bonds are black
		if (showBonds)
			bondMaterial.setTransparency(0.0);// make the cells translucent
		else
			bondMaterial.setTransparency(1.0);// make the cells translucent

		xMaterial.setColor(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20, 0.0, 0.0, 0.0);// bonds are black
		yMaterial.setColor(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 20, 0.5, 0.5, 0.5);// bonds are black
		zMaterial.setColor(0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 20, 0.25, 0.25, 0.25);// bonds are black
		if (showAxes) {
			xMaterial.setTransparency(0.0);// make the cells translucent
			yMaterial.setTransparency(0.0);// make the cells translucent
			zMaterial.setTransparency(0.0);// make the cells translucent
		} else {
			xMaterial.setTransparency(1.0);// make the cells translucent
			yMaterial.setTransparency(1.0);// make the cells translucent
			zMaterial.setTransparency(1.0);// make the cells translucent
		}
	}

	/** resets the viewing direction without changing anything else */
	public void resetViewDirection() {
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

		if (viewType == 1)
			Vec.mattranspose(recipsuperCell, tempmat);
		if (viewType == 2)
			Vec.mattranspose(variables.sBasisCart, tempmat);
		if (viewType == 3)
			Vec.mattranspose(recipparentCell, tempmat);
		if (viewType == 4)
			Vec.mattranspose(variables.pBasisCart, tempmat);

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
			rp.setCamera(tY, tX);// y-angle rotation first then x-angle rotation
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

	public void keyPressed(KeyEvent e) {
	}

	public void keyReleased(KeyEvent e) {
	}

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

			// resetViewDirection();
			rp.setCamera(0, 0);

			rp.setFOV(fov0);
			for (int t = 0; t < variables.numTypes; t++)
				for (int s = 0; s < variables.numSubTypes[t]; s++)
					variables.subTypeBox[t][s].setSelected(false);

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
//			viewFocused = true; // Record the fact that we have focus.
//			updateDisplay();
//		}
//
//		public void focusLost(FocusEvent event) {
//			viewFocused = false; // Record the fact that we don't have focus.
//		}
//	} // end class focusIt

	public BufferedImage getImage() {
		return rp.getImage();
	}

	@Override
	protected void handleCheckBoxEvent(Object src) {
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
			viewType = 1;
		} else if (src == superUVW) {
			viewType = 2;
		} else if (src == parentHKL) {
			viewType = 3;
		} else if (src == parentUVW) {
			viewType = 4;
		}
		updateDisplay();

	}

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
		nButton = newJRadioButton("Normal", true, xyzButtons);
		xButton = newJRadioButton("Xrot", false, xyzButtons);
		yButton = newJRadioButton("Yrot", false, xyzButtons);
		zButton = newJRadioButton("Zrot", false, xyzButtons);
		zoomButton = newJRadioButton("Zoom", false, xyzButtons);

		viewType = 1; // initial view type

		ButtonGroup cellButtons = new ButtonGroup();
		superHKL = newJRadioButton("SupHKL", true, cellButtons); // initial view type
		superUVW = newJRadioButton("SupUVW", false, cellButtons);
		parentHKL = newJRadioButton("ParHKL", false, cellButtons);
		parentUVW = newJRadioButton("ParUVW", false, cellButtons);

		uView = new JTextField(3);
		uView.setText("0");
		uView.setMargin(new Insets(-2, 0, -1, -10));
//		uView.addFocusListener(new focusListener());

		vView = new JTextField(3);
		vView.setText("0");
		vView.setMargin(new Insets(-2, 0, -1, -10));
//		vView.addFocusListener(new focusListener());

		wView = new JTextField(3);
		wView.setText("1");
		wView.setMargin(new Insets(-2, 0, -1, -10));
//		wView.addFocusListener(new focusListener());

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
		for (int t = 0; t < variables.numTypes; t++)
			for (int s = 0; s < variables.numSubTypes[t]; s++)
				variables.subTypeBox[t][s].addItemListener(checkboxListener);
	}

	private JRadioButton newJRadioButton(String label, boolean selected, ButtonGroup g) {
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
		cb.addItemListener(checkboxListener);
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

}
