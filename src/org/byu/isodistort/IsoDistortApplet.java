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
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.text.DecimalFormat;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

import org.byu.isodistort.local.CommonStuff;
import org.byu.isodistort.local.ImageSaver;
import org.byu.isodistort.render.Geometry;
import org.byu.isodistort.render.Material;
// import org.byu.isodistort.render.Matrix;
import org.byu.isodistort.render.RenderApplet;
import org.byu.isodistort.render.Vec;

public class IsoDistortApplet extends RenderApplet {
	private static final long serialVersionUID = 1L;

	public IsoDistortApplet() {
		super();
	}
	
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
	/** Button for inputting the view direction */
	JButton applyView;
	/** Button for saving the current applet image */
	JButton saveImage;
	/** Text fields for inputting viewing angles */
	JTextField uView, vView, wView;
	
	/**
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 * 2 2 2 In the second section we initialize everything on the applet that we
	 * want. 2 2 2
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 */

	public void initialize() {
		this.addKeyListener(new keyinputListener());
		readFile();
		rd = new CommonStuff(this, dataString, false);
		initMaterials();
		spheres = world.add();
		bonds = world.add();
		cells = world.add();
		axes = world.add();
		initAtoms();
		initBonds();
		initCells();
		initAxes();
		initView();

//and below we initialize the applet part of the panel.  Because crystalAppletHTML
//extends renderApplet (in the render package) the remainning code in this initialization
//section actually refers to renderApplet.*/

		recalcABC(); // do this first to get size
//		System.out.println("numBonds =  "+rd.numBonds);
//		System.out.println("maxBondLength =  "+rd.maxBondLength);
//		for(int b = 0; b < rd.numBonds; b++)
//		{	
//			System.out.println(b+"  "+bondInfo[b][0]+"  "+bondInfo[b][1]+"  "+bondInfo[b][2]+"  "+bondInfo[b][3]+"  "+bondInfo[b][4]+"  "+bondInfo[b][5]+"  "+bondInfo[b][6]);
//		}

		recalcMaterials();

		// Set background color, field of view and focal length of renderer
		fov = 2 * perspectivescaler * scdSize / fl;
		fov0 = fov;
		setBgColor(1, 1, 1);// background color: white
		setFOV(fov);// field of view
		setFL(fl);// focal length: zoomed way out
		changeFOV(fov);
		renderer.setCamera(0, 0);

//		System.out.println("size: "+size);
//		System.out.println("focal length: "+fl);
//		System.out.println("scalar: "+scalar);
//		System.out.println("field of view: "+fov);

		// Define position and color of light source (x, y, z, r, g, b)
		double bbb = 0.38;
		addLight(.5, .5, .5, 1.7 * bbb, 1.7 * bbb, 1.7 * bbb);
		addLight(-.5, .5, .5, bbb, bbb, bbb);
		addLight(.5, -.5, .5, bbb, bbb, bbb);
		addLight(-.5, -.5, .5, bbb, bbb, bbb);

		// Add the atoms and bonds and cells to applet
		renderAtoms();// add the atoms to the renderer
		renderBonds();// add the bonds to the renderer
		renderCells();// add the cells to the renderer
		renderAxes();// add the axes to the renderer
		setTimer();
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
	boolean isFocused = true; // is the Applet in focus? If not, stop updating the display.
	boolean viewFocused = false; // are the viewDir fields being accessed?

	protected void runImpl() {
		if (!isAnimate && !spin && --initializing < 0)
			return;
		updateDisplay();
		if (initializing == 0 && !isAnimate && !spin)
			stop();
	}

	private boolean isAdjusting = false;
	
	public void updateDisplay() {
		if (isAdjusting)
			return;
		isAdjusting = true;
		boolean v = viewFocused;
		/**
		 * @j2sNative v = true;
		 */
		{
		}
		isFocused = v || this.hasFocus();
		// BH: SwingJS isn't reporting this properly

		// Stops display updates not in focus.
		// Includes the applet focus and the focusable viewDir fields
		// which would otherwise stop the display when accessed.

		if (rd.isChanged) {
			isRecalc = true;
			rd.isChanged = false;
		}

		if (isFocused) // if in focus
		{
			if (isAnimate) {
				animPhase += 2 * Math.PI / (5 * frameRate);
				animPhase = animPhase % (2 * Math.PI);
				animAmp = Math.pow(Math.sin(animPhase), 2);
				rd.masterSlider.setValue((int) Math.round(animAmp * rd.sliderMaxVal));
				isRecalc = true;
			}

			if (isRecalcMat) {
				recalcMaterials();
				isRecalcMat = false;
			}
			if (isRecalc) {
				recalcABC();
				renderAtoms();
				renderBonds();
				renderCells();
				renderAxes();
				isRecalc = false;
			}
			moreRunStuff();
		}
		isAdjusting = false;
		repaint();
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
		atomcoordInfo = new double[rd.numAtoms][3];
		atomradInfo = new double[rd.numAtoms];
		atommagInfo = new double[rd.numAtoms][3];
		atomrotInfo = new double[rd.numAtoms][3];
		atomellipInfo = new double[rd.numAtoms][7];
		for (int qq = 0; qq < rd.numAtoms; qq++)// iterate over all the types
			spheres.delete(0);
		int q = 0;
		for (int t = 0; t < rd.numTypes; t++)// iterate over all the types
			for (int s = 0; s < rd.numSubTypes[t]; s++)
				for (int a = 0; a < rd.numSubAtoms[t][s]; a++) {
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
		bondInfo = new double[rd.numBonds][7];
		for (int b = 0; b < rd.numBonds; b++)
			bonds.delete(0);
		for (int b = 0; b < rd.numBonds; b++)
			bonds.add().tube(numBondSides).setMaterial(bondMaterial);
	}

	/**
	 * initCells initializes the cell array.
	 */
	private void initCells() {
		showCells = showCells0;
		parentCellInfo = new double[12][7];
		superCellInfo = new double[12][7];
		for (int c = 0; c < 24; c++)
			cells.delete(0);
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
		parentCellMaterial = new Material(renderer);
		superCellMaterial = new Material(renderer);
		bondMaterial = new Material(renderer);
		xMaterial = new Material(renderer);
		yMaterial = new Material(renderer);
		zMaterial = new Material(renderer);
		subMaterial = new Material[rd.numTypes][];

		// Create the subMaterial array;
		for (int t = 0; t < rd.numTypes; t++)// iterate over types of atoms
		{
			subMaterial[t] = new Material[rd.numSubTypes[t]];
			for (int s = 0; s < rd.numSubTypes[t]; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = new Material(renderer);
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
		for (int t = 0; t < rd.numTypes; t++)
			for (int s = 0; s < rd.numSubTypes[t]; s++)// iterate over all subtypes of this type
				for (int a = 0; a < rd.numSubAtoms[t][s]; a++)// iterate over all atoms of that type
				{
					push();
					scale(perspectivescaler, perspectivescaler, perspectivescaler);
					translate(atomcoordInfo[q]);
					tempr = rd.atomMaxRadius * atomradInfo[q];
					scale(tempr, tempr, tempr);
					transform(spheres.child(q));
					pop();

					push();
					tempx = (atomellipInfo[q][0] / Math.sqrt(rd.defaultUiso));
					tempy = (atomellipInfo[q][1] / Math.sqrt(rd.defaultUiso));
					tempz = (atomellipInfo[q][2] / Math.sqrt(rd.defaultUiso));
//					System.out.println("widths: "+q+", "+tempx+", "+tempy+", "+tempz);
					scale(tempx, tempy, tempz);
					transform(spheres.child(q).child(0));
					pop();

					push();
					rotateY(atommagInfo[q][Y]);// y-angle orientation of arrow number q
					rotateX(atommagInfo[q][X]);// x-angle orientation of arrow number q
					if (Math.abs(atommagInfo[q][L]) > 0.1)
						temp = momentMultiplier;
					else
						temp = 0;
					scale(temp, temp, 0.62 + atommagInfo[q][L] * rd.angstromsPerMagneton); // The factor of 0.62 hides
																							// the zero-length moments
																							// just inside the surface
																							// of the spheres.
					transform(spheres.child(q).child(1));
					pop();

					push();
					rotateY(atomrotInfo[q][Y]);// y-angle orientation of arrow number q
					rotateX(atomrotInfo[q][X]);// x-angle orientation of arrow number q
					if (Math.abs(atomrotInfo[q][L]) > 0.1)
						temp = rotationMultiplier;
					else
						temp = 0;
					scale(temp, temp, 0.62 + atomrotInfo[q][L] * rd.angstromsPerRadian); // The factor of 0.62 hides the
																							// zero-length rotations
																							// just inside the surface
																							// of the spheres.
					transform(spheres.child(q).child(2));
					pop();

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
		for (int b = 0; b < rd.numBonds; b++)// iterate over bonds
		{
			push();// new matrix
			scale(perspectivescaler, perspectivescaler, perspectivescaler);// make everything smaller to take out
																			// perspective
			translate(bondInfo[b][x], bondInfo[b][y], bondInfo[b][z]);// position the atom at (x,y,z)
			rotateY(bondInfo[b][Y]);// y-angle orientation of bond number b
			rotateX(bondInfo[b][X]);// x-angle orientation of bond number b
			temp = rd.atomMaxRadius * bondMultiplier;
			if (bondInfo[b][6] <= 0.5)
				hide = 0.001;
			else
				hide = 1.00;
			scale(hide * temp, hide * temp, hide * bondInfo[b][5] / 2.0); // The first two indicies are the
																			// cross-section of the bond which is a
																			// fraction of the atomRadius whereas, the
																			// third index is the length of the tube and
																			// it is calculated in paramReader.
			transform(bonds.child(b));// transform and add this bond
			pop();
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
			push();
			scale(perspectivescaler, perspectivescaler, perspectivescaler);// make everything smaller to take out
																			// perspective
			translate(parentCellInfo[c][x], parentCellInfo[c][y], parentCellInfo[c][z]);
			rotateY(parentCellInfo[c][4]);
			rotateX(parentCellInfo[c][3]);
			temp = rd.atomMaxRadius * cellMultiplier;
			scale(temp, temp, parentCellInfo[c][5] / 2.0);
			transform(cells.child(c));
			pop();

			push();
			scale(perspectivescaler, perspectivescaler, perspectivescaler);// make everything smaller to take out
																			// perspective
			translate(superCellInfo[c][x], superCellInfo[c][y], superCellInfo[c][z]);
			rotateY(superCellInfo[c][4]);
			rotateX(superCellInfo[c][3]);
			scale(temp, temp, superCellInfo[c][5] / 2.0);
			transform(cells.child(c + 12));
			pop();
		}
	}

	private void renderAxes() {
		int x = 0, y = 1, z = 2, X = 3, Y = 4;
		double temp;
		for (int axis = 0; axis < 6; axis++) {
			push();
			scale(perspectivescaler, perspectivescaler, perspectivescaler);
			translate(axesInfo[axis][x], axesInfo[axis][y], axesInfo[axis][z]);// position the atom at (x,y,z)
			rotateY(axesInfo[axis][Y]);// y-angle orientation of bond number b
			rotateX(axesInfo[axis][X]);// x-angle orientation of bond number b
			if (axis > 2)
				temp = rd.atomMaxRadius * axesMultiplier1;
			else
				temp = rd.atomMaxRadius * axesMultiplier2;
			scale(temp, temp, axesInfo[axis][5] / 2.0); // The first two indices are the cross-section, which is a
														// fraction of the atomRadius, whereas the third index is the
														// length.
			transform(axes.child(axis));
			pop();
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

		rd.readSliders();
		rd.recalcDistortion();

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
			Vec.pairtobond(rd.parentCellVertices[buildCell[c][0]], rd.parentCellVertices[buildCell[c][1]],
					parentCellInfo[c]);
			Vec.pairtobond(rd.superCellVertices[buildCell[c][0]], rd.superCellVertices[buildCell[c][1]],
					superCellInfo[c]);
		}

		int q = 0;
		for (int t = 0; t < rd.numTypes; t++)
			for (int s = 0; s < rd.numSubTypes[t]; s++)
				for (int a = 0; a < rd.numSubAtoms[t][s]; a++) {
					atomradInfo[q] = rd.atomFinalOcc[t][s][a];

					for (int i = 0; i < 3; i++)
						newcoord[i] = rd.atomFinalCoord[t][s][a][i];
					Vec.matdotvect(rd.sBasisCart, newcoord, tempvec); // typeless atom in cartesian coords
					for (int i = 0; i < 3; i++) // recenter in Applet coordinates
						atomcoordInfo[q][i] = tempvec[i] - rd.sCenterCart[i];

					for (int i = 0; i < 3; i++)
						newmag[i] = rd.atomFinalMag[t][s][a][i];
					Vec.matdotvect(rd.sBasisCart, newmag, tempvec);
					Vec.calculatearrow(tempvec, atommagInfo[q]);

					for (int i = 0; i < 3; i++)
						newrot[i] = rd.atomFinalRot[t][s][a][i];
					Vec.matdotvect(rd.sBasisCart, newrot, tempvec);
					Vec.calculatearrow(tempvec, atomrotInfo[q]);

//					Ellipsoid work
					for (int i = 0; i < 6; i++)
						newellip[i] = rd.atomFinalEllip[t][s][a][i];
//			        System.out.println ("ellipmat1: "+t+", "+s+", "+a+", "+newellip[0]+", "+newellip[1]+", "+newellip[2]+", "+newellip[3]+", "+newellip[4]+", "+newellip[5]);
					Vec.voigt2matrix(newellip, tempmat);
//			        System.out.println ("ellipmat2: "+t+", "+s+", "+a+", "+tempmat[0][0]+", "+tempmat[0][1]+", "+tempmat[0][2]+", "+tempmat[1][0]+", "+tempmat[1][1]+", "+tempmat[1][2]+", "+tempmat[2][0]+", "+tempmat[2][1]+", "+tempmat[2][2]);
					Vec.matdotmat(rd.sBasisCart, tempmat, tempmat2);
					Vec.matcopy(tempmat2, tempmat);
					Vec.matdotmat(tempmat, rd.sBasisCartInverse, tempmat2);// ellipsoid in cartesian coords -- not safe
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
		for (int b = 0; b < rd.numBonds; b++) // calculate bondInfo(x-cen, y-cen, z-cen, thetaX, thetaY, length)
		{
			Vec.pairtobond(atomcoordInfo[rd.whichAtomsBond[b][0]], atomcoordInfo[rd.whichAtomsBond[b][1]], bondInfo[b]);
			if (bondInfo[b][5] >= rd.maxBondLength)
				bondInfo[b][6] = 0.0;
			if (atomradInfo[rd.whichAtomsBond[b][0]] <= rd.minBondOcc
					|| atomradInfo[rd.whichAtomsBond[b][1]] <= rd.minBondOcc)
				bondInfo[b][6] = 0.0;
		}

		// calculate the parent and supercell coordinate axes in bond format
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = rd.pBasisCart[i][axis];
			Vec.normalize(tempvec);
			for (int i = 0; i < 3; i++) {
				extent[i] = rd.pOriginCart[i] - rd.sCenterCart[i] + rd.pBasisCart[i][axis];
				paxesbegs[axis][i] = extent[i] + 2.0 * rd.atomMaxRadius * tempvec[i];
				paxesends[axis][i] = extent[i] + 3.5 * rd.atomMaxRadius * tempvec[i];
				;
			}
			Vec.pairtobond(paxesbegs[axis], paxesends[axis], axesInfo[axis]);

			for (int i = 0; i < 3; i++)
				tempvec[i] = rd.sBasisCart[i][axis];
			Vec.normalize(tempvec);
			for (int i = 0; i < 3; i++) {
				extent[i] = -rd.sCenterCart[i] + rd.sBasisCart[i][axis];
				saxesbegs[axis][i] = extent[i] + 1.5 * rd.atomMaxRadius * tempvec[i];
				saxesends[axis][i] = extent[i] + 4.0 * rd.atomMaxRadius * tempvec[i];
			}
			Vec.pairtobond(saxesbegs[axis], saxesends[axis], axesInfo[axis + 3]);
		}

		// Calculate the maximum distance from applet center (used to determine FOV).
		scdSize = 0;
		for (int j = 0; j < 8; j++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = rd.superCellVertices[j][i];
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
			for (int i = 0; i < 3; i++)
				tempvec[i] = rd.parentCellVertices[j][i];
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (q = 0; q < rd.numAtoms; q++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = atomcoordInfo[q][i];
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = paxesends[axis][i];
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < 3; i++)
				tempvec[i] = saxesends[axis][i];
			tempscalar = Vec.norm(tempvec);
			if (tempscalar > scdSize)
				scdSize = tempscalar;
		}

		scdSize += 2 * rd.atomMaxRadius; // this includes the width of atoms that might be at the extremes of the
											// longest cell diagonal.
	}

	/** recalculates the atom colors after a checkbox has been set. */
	private void recalcMaterials() {
		double rrr, ggg, bbb, k;

		rd.setColors(isSimpleColor);
		rd.recolorPanels();
//		Reset the atom colors.			
		for (int t = 0; t < rd.numTypes; t++)// iterate over types of atoms
		{
			rrr = rd.color[t].getRed() / 255.0;
			ggg = rd.color[t].getGreen() / 255.0;
			bbb = rd.color[t].getBlue() / 255.0;
			for (int s = 0; s < rd.numSubTypes[t]; s++)// iterate over number-of-subtypes-selected; 0 means 1 is
														// selected
			{
				if (!rd.subTypeBox[t][s].isSelected())
					subMaterial[t][s].setColor(rrr, ggg, bbb, 0.3, 0.3, 0.3, 1, .0001, .0001, .0001);// makes the atom
																										// color same as
																										// above type
																										// color
				else {
					k = (double) 0.1 + 0.8 * s / rd.numSubTypes[t];
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

		Vec.matinverse(rd.sBasisCart, tempmat);
		Vec.mattranspose(tempmat, recipsuperCell);
		Vec.matinverse(rd.pBasisCart, tempmat);
		Vec.mattranspose(tempmat, recipparentCell);

		if (viewType == 1)
			Vec.mattranspose(recipsuperCell, tempmat);
		if (viewType == 2)
			Vec.mattranspose(rd.sBasisCart, tempmat);
		if (viewType == 3)
			Vec.mattranspose(recipparentCell, tempmat);
		if (viewType == 4)
			Vec.mattranspose(rd.pBasisCart, tempmat);

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

			theta = phi = sigma = 0;
			renderer.setCamera(tY, tX);// y-angle rotation first then x-angle rotation
//			System.out.println("viewDir: "+viewDir[0]+", "+viewDir[1]+", "+viewDir[2]);
//			System.out.println("xV: "+xV+", yV: "+yV+", zV: "+zV);
//			System.out.println("sigma(tx): "+tX+", theta(ty): "+tY);
			requestFocus(); // trick for recovering from loss of applet focus
		}
	}

	/**
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 * 5 5 5 The fifth section has the methods that listen for the sliderbars, the
	 * keyboard and the viewing buttons. 5 5 5
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 */

	/** keyinputListener responds to keyboard commands. */
	private class keyinputListener implements KeyListener {
		public void keyPressed(KeyEvent e) {
		}

		public void keyReleased(KeyEvent e) {
		}

		public void keyTyped(KeyEvent e) {
			char key = e.getKeyChar();

			switch (key) {
			case 'r':
			case 'R':
				theta = phi = sigma = 0;
				animPhase = Math.PI / 2;
				animAmp = 1;
				rd.resetSliders();
				rd.readSliders();
				rd.recalcDistortion();
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
				renderer.setCamera(0, 0);

				setFOV(fov0);
				for (int t = 0; t < rd.numTypes; t++)
					for (int s = 0; s < rd.numSubTypes[t]; s++)
						rd.subTypeBox[t][s].setSelected(false);

				centerImage();
				break;
			case 'z':
			case 'Z':
				rd.zeroSliders();
				break;
			case 'i':
			case 'I':
				rd.resetSliders();
				break;
			case 's':
			case 'S':
				rd.toggleIrrepSliders();
				break;
			case 'n':
			case 'N':
				invert *= -1;
				break;
			case 'c':
			case 'C':
				centerImage();
				break;
			}
			updateDisplay();
		}

		private void centerImage() {
			xOff = 0;
			yOff = 0;
			zOff = 0;
			invert = 1;

			push();
			identity();
			translate(0, 0, 0);
			for (int i = 0; i < 16; i++)
				if (world.child(i) != null)
					transform(world.child(i));
			pop();
		}
	}

	/** listens for changes in focus in the ViewDirection fields. */
	private class focusListener implements FocusListener {
		public void focusGained(FocusEvent event) {
			viewFocused = true; // Record the fact that we have focus.
			updateDisplay();
		}

		public void focusLost(FocusEvent event) {
			viewFocused = false; // Record the fact that we don't have focus.
		}
	} // end class focusIt

	/** listens for the check boxes that highlight a given atomic subtype. */
	private class checkboxListener implements ItemListener {
		public void itemStateChanged(ItemEvent event) {
			showAtoms = aBox.isSelected();
			showBonds = bBox.isSelected();
			showCells = cBox.isSelected();
			showAxes = axesBox.isSelected();
			spin = spinBox.isSelected();
			isSimpleColor = colorBox.isSelected();
			isAnimate = animBox.isSelected();
			isRecalcMat = true;
			if (isAnimate || spin) {
				start();
			} else {
				updateDisplay();
			}
		}
	}

	/** listens for the applet buttons, which specify the viewing angles. */
	private class buttonListener implements ItemListener {
		public void itemStateChanged(ItemEvent event) {
			if (event.getSource() == nButton) {
				theta = phi = sigma = 0;
				rotAxis = 0;
			}
			if (event.getSource() == xButton) {
				theta = phi = sigma = 0;
				rotAxis = 1;
			}
			if (event.getSource() == yButton) {
				theta = phi = sigma = 0;
				rotAxis = 2;
			}
			if (event.getSource() == zButton) {
				theta = phi = sigma = 0;
				rotAxis = 3;
			}
			if (event.getSource() == zoomButton) {
				theta = phi = sigma = 0;
				rotAxis = 4;
			}

			if (event.getSource() == superHKL)
				viewType = 1;
			if (event.getSource() == superUVW)
				viewType = 2;
			if (event.getSource() == parentHKL)
				viewType = 3;
			if (event.getSource() == parentUVW)
				viewType = 4;
			updateDisplay();

		}
	}

	/** Resets the view when the applyView button is pushed. */
	private class viewListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (event.getSource() == applyView)
				resetViewDirection();
			if (event.getSource() == saveImage)
				ImageSaver.saveImageFile(im, IsoDistortApplet.this);
			updateDisplay();
		}
	}

	/**
	 * 6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666
	 * 6 6 6 The sixth (last) section has the methods called from initialize() which
	 * create the GUI. 6 6 6
	 * 6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666
	 **/

	/**
	 * Sets up the applet window and scroll and control panels
	 */
	private void initView() {
		setSize(rd.appletWidth, rd.appletHeight);// the total area of the applet (e.g. 1024x512)
		
		// BH: On a page, an applet dimension is not settable
		
		setLayout(new BorderLayout());// allows for adding slider panel to the side (east or west) of rendering area
		setBackground(Color.WHITE);
		rd.initPanels();
		buildControls();
		add(rd.controlPane, BorderLayout.SOUTH);// add to south of Applet
		add(rd.scrollPane, BorderLayout.EAST);// add to east of Applet
		setRenderArea(rd.renderingWidth, rd.renderingHeight);// the rendering area of the applet (e.g. 512x512)
		updateDisplay();
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
		colorBox.setVisible(rd.needSimpleColor);
		
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
		uView.addFocusListener(new focusListener());
		
		vView = new JTextField(3); 
		vView.setText("0");
		vView.setMargin(new Insets(-2, 0, -1, -10));
		vView.addFocusListener(new focusListener());

		wView = new JTextField(3);
		wView.setText("1");
		wView.setMargin(new Insets(-2, 0, -1, -10));
		wView.addFocusListener(new focusListener());

		applyView = new JButton("Apply View");
		applyView.setFocusable(false);
		applyView.setMargin(new Insets(-3, 3, -2, 4));
		applyView.setHorizontalAlignment(JButton.LEFT);
		applyView.setVerticalAlignment(JButton.CENTER);
		applyView.addActionListener(new viewListener());

		saveImage = new JButton("Save Image");		 
		saveImage.setFocusable(false);
		saveImage.setMargin(new Insets(-3, 3, -2, 4));
		saveImage.setHorizontalAlignment(JButton.LEFT);
		saveImage.setVerticalAlignment(JButton.CENTER);
		saveImage.addActionListener(new viewListener());

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
		botControlPanel.add(new JLabel("                          Direction: "));
		botControlPanel.add(uView);
		botControlPanel.add(vView);
		botControlPanel.add(wView);
		botControlPanel.add(applyView);
		botControlPanel.add(saveImage);
		botControlPanel.add(new JLabel("                           "));

		rd.controlPanel.add(topControlPanel);
		rd.controlPanel.add(botControlPanel);

		// Add listeners to a the subtype checkboxes -- don't know where else to put it.
		for (int t = 0; t < rd.numTypes; t++)
			for (int s = 0; s < rd.numSubTypes[t]; s++)
				rd.subTypeBox[t][s].addItemListener(new checkboxListener());
	}

	private JRadioButton newJRadioButton(String label, boolean selected, ButtonGroup g) {
		JRadioButton b = new JRadioButton(label, selected);
		b.setHorizontalAlignment(JRadioButton.LEFT);
		b.setVerticalAlignment(JRadioButton.CENTER);
		b.setFocusable(false);
		b.setBackground(Color.WHITE);
		b.setForeground(Color.BLACK);
		b.setVisible(true);
		b.setBorderPainted(false);
		b.addItemListener(new buttonListener());
		g.add(b);
		return b;
	}

	private JCheckBox newJCheckBox(String label, boolean selected) {
		JCheckBox cb = new JCheckBox(label, selected);
		cb.setHorizontalAlignment(JCheckBox.LEFT);
		cb.setVerticalAlignment(JCheckBox.CENTER);
		cb.setFocusable(false);
		cb.setVisible(true);
		cb.setBackground(Color.WHITE);
		cb.setForeground(Color.BLACK);
		cb.addItemListener(new checkboxListener());
		return cb;
	}

} // end isoDistortApplet.class

