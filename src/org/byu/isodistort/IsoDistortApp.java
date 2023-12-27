/**
 *  David Tanner
 *  April 2005
 * 
 * 
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
import java.util.BitSet;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import org.byu.isodistort.local.Bspt.CubeIterator;
import org.byu.isodistort.local.IsoApp;
import org.byu.isodistort.local.MathUtil;
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.Variables.Atom;
import org.byu.isodistort.render.Geometry;
import org.byu.isodistort.render.Material;
// import org.byu.isodistort.render.Matrix;
import org.byu.isodistort.render.RenderPanel3D;

public class IsoDistortApp extends IsoApp implements Runnable, KeyListener {

	/**
	 * initial value for indicating time required from loading to rendering.
	 */
	public long t0 = System.currentTimeMillis();

	protected boolean isAnimationRunning;

	
	// variables that the user may want to adjust
	/**
	 * Cell-edge tube parameters
	 */
	final static int numBondSides = 6, numCellSides = 6, numArrowSides = 8, ballRes = 2;
	/**
	 * Focal length for renderer.
	 */
	final static double fl = 10;
	/**
	 * Decimal multiplier to make bond radius and cell radius fractions of atom
	 * radius
	 * 
	 */
	double bondMultiplier = 0.1, axesMultiplier1 = 0.2, axesMultiplier2 = 0.3;
	double cellMultiplier = 0.25, momentMultiplier = 0.4, rotationMultiplier = 0.35;

//	Other global variables.
	/**
	 * Initially show bonds or not
	 */
	boolean showBonds0 = true, showAtoms0 = true, showCells0 = true, showAxes0 = false;
	/**
	 * Currently show bonds or not
	 */
	boolean showBonds, showAtoms, showCells, showAxes;
	/**
	 * Which type of view direction: superHKL, superUVW, parentHKL, parentUVW
	 */
	int viewType;
	/**
	 * Longest diagonal of super cell.
	 */
	double scdSize;
	/**
	 * Field of view for renderer. It is a function of size.
	 */
	double fov;
	/**
	 * Rounds mode amplitudes to three decimal places.
	 */
	DecimalFormat round = new DecimalFormat("0.###");
	/**
	 * A geometry for rendering a part of the crystal. Each geometry can hold one or
	 * more shapes. spheres and atoms hold all the atoms; cells holds all 24
	 * cylinders for the parent and super cell.
	 * 
	 */
	Geometry atomObjects, bondObjects, cellObjects, axisObjects;

// Global variables that pertain to atom, cell and bond properties	

	BitSet bsBondsEnabled;

	/**
	 * Final information needed to render parent cell (first 12) and super cell
	 * (last 12). [edge number][x, y, z, x-angle orientation, y-angle orientation,
	 * length, displayflag]
	 * 
	 */
	double[][] cellInfo;
	/**
	 * Final information needed to render bonds [bond number][x, y, z, x-angle ,
	 * y-angle, length, displayflag]
	 * 
	 */
	double[][] axesInfo;

	// Global variables that are related to material properties.
	/**
	 * Materials for coloring bonds and cells.
	 * 
	 */
	public static Material bondMaterial, parentCellMaterial, superCellMaterial, xAxisMaterial, yAxisMaterial, zAxisMaterial;
	/**
	 * Array of materials for coloring atoms[type][subtype][regular,highlighted]
	 * 
	 */
	Material[][] subMaterial;

	/**
	 * Check boxes for zoom, spin, anim toggles
	 * 
	 */
	JCheckBox aBox, bBox, cBox, spinBox, colorBox, animBox, axesBox;
	/**
	 * Buttons for mouse controls
	 * 
	 */
	JRadioButton nButton, xButton, yButton, zButton, zoomButton;
	/**
	 * Buttons to use super or parent cell for view vectors
	 * 
	 */
	JRadioButton superHKL, superUVW, parentHKL, parentUVW;
	/**
	 * Text fields for inputting viewing angles
	 * 
	 */
	JTextField uView, vView, wView;
	private RenderPanel3D rp;

	public IsoDistortApp() {
		super(APP_ISODISTORT);
	}

	@Override
	protected void init() {
		rp = new RenderPanel3D(this);
		rp.setPreferredSize(drawPanel.getSize());
		rp.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add(rp, BorderLayout.CENTER);
		rp.addKeyListener(this);
		initMaterials();
		Geometry world = rp.getWorld();
		atomObjects = world.add();
		bondObjects = world.add();
		cellObjects = world.add();
		axisObjects = world.add();
		initAtoms();
		initBonds();
		initCells();
		initAxes();
		buildControls();
		recalcABC();
		recalcMaterials();
		rp.initializeSettings(// persepeciveScalar,
				scdSize);
	}

	@Override
	protected void frameResized() {
		super.frameResized();
		needsRecalc = true;
	}

	/**
	 * Animation amplitude and phase.
	 * 
	 */
	boolean isRecalcMat = false;
	boolean isAnimate = false;
	boolean isSimpleColor = false;
	double animPhase = Math.PI / 2;
	double animAmp = 1;

	private void initAtoms() {
		showAtoms = showAtoms0;
		int n = variables.numAtoms;
		atomObjects.clear(0);
		for (int ia = 0; ia < n; ia++) {
			atomObjects.add();
			Atom a = variables.atoms[ia];
			Material m = subMaterial[a.t][a.s];
			atomObjects.child(ia).add().ball(ballRes).setMaterial(m);
			atomObjects.child(ia).add().arrow(numArrowSides).setMaterial(m);
			atomObjects.child(ia).add().arrow(numArrowSides).setMaterial(m);
		}
	}

	/**
	 * initBonds initializes the bond array.
	 * 
	 */
	private void initBonds() {
		showBonds = showBonds0;
		int n = variables.numBonds;
		bsBondsEnabled = new BitSet();
		bondObjects.clear(0);
		if (n > 0) {
			for (int b = 0; b < n; b++)
				addBondObject("bond_" + b);
		}
	}

	/**
	 * For dynamic addition of bonds by variables using the Binary 
	 */
	public void addBondObject(String name) {
		bondObjects.add().tube(numBondSides).setMaterial(bondMaterial).setName(name);
	}

	/**
	 * initCells initializes the cell array.
	 * 
	 */
	private void initCells() {
		showCells = showCells0;
		cellInfo = new double[24][7];
		cellObjects.clear(0);
		for (int c = 0; c < 24; c++) {
			cellObjects.add().cylinder(numCellSides).setMaterial(c < 12 ? parentCellMaterial : superCellMaterial);
		}
	}

	/**
	 * initAxes initializes the axes array.
	 * 
	 */
	private void initAxes() {
		showAxes = showAxes0;
		axesInfo = new double[6][6];
		axisObjects.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
	}

	/**
	 * initMaterials instantiates an array of colors for the atoms. These methods
	 * are from the render package
	 * 
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
		for (int t = 0, nt = variables.numTypes; t < nt; t++) {
			subMaterial[t] = new Material[variables.numSubTypes[t]];
			for (int s = 0, nst = variables.numSubTypes[t]; s < nst; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = rp.newMaterial();
		}
	}

	private static final int VIEW_TYPE_SUPER_HKL = 1;
	private static final int VIEW_TYPE_SUPER_UVW = 2;
	private static final int VIEW_TYPE_PARENT_HKL = 3;
	private static final int VIEW_TYPE_PARENT_UVW = 4;

	@Override
	public void updateDisplay() {
		if (isAdjusting)
			return;
		isAdjusting = true;
		if (variables.isChanged) {
			needsRecalc = true;
			variables.isChanged = false;
		}
		if (rp == null)
			return;
		rp.updateForDisplay(false);
		if (isRecalcMat) {
			recalcMaterials();
		}
		if (needsRecalc) {
			// virtually all the time is here:
			recalcABC();
			if (showAtoms) {
				for (int i = 0, n = variables.numAtoms; i < n; i++) {
					double[][] info = variables.getAtomInfo(i);
					Geometry a = atomObjects.child(i);
					renderScaledAtom(a, info[DIS], info[OCC][0] * variables.atomMaxRadius);
					renderEllipsoid(a.child(0), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
					renderAtomChild(a.child(1), info[MAG], momentMultiplier, variables.angstromsPerMagneton);
					renderAtomChild(a.child(2), info[ROT], rotationMultiplier, variables.angstromsPerRadian);
				}
			}
			double r;
			if (showBonds) {
				r = variables.atomMaxRadius * bondMultiplier;
				for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
					transformCylinder(r, variables.bondInfo[b], bondObjects.child(b));
				}
			}
			if (showCells) {
				r = variables.atomMaxRadius * cellMultiplier;
				for (int c = 0; c < 24; c++) {
					transformCylinder(r, cellInfo[c], cellObjects.child(c));
				}
			}
			if (showAxes) {
				for (int a = 0; a < 6; a++) {
					r = variables.atomMaxRadius * (a > 2 ? axesMultiplier1 : axesMultiplier2);
					transformCylinder(r, axesInfo[a], axisObjects.child(a));
				}
			}
			needsRecalc = false;
		}
		isAdjusting = false;
		rp.updateForDisplay(true);
	}

	private void transformCylinder(double r, double[] info, Geometry child) {
		rp.push();
		{
			rp.translate(info);
			rp.rotateY(info[Variables.RY]);
			rp.rotateX(info[Variables.RX]);
			rp.scale(r, r, info[Variables.L_2]);
			rp.transform(child);
		}
		rp.pop();
	}

	private void renderScaledAtom(Geometry child, double[] xyz, double r) {
		rp.push();
		{
			rp.translate(xyz);
			rp.scale(r, r, r);
			rp.transform(child);
		}
		rp.pop();
	}

	private void renderEllipsoid(Geometry child, double[] info, double f) {
		rp.push();
		{
			rp.scale(info[0] * f, info[1] * f, info[2] * f);
			rp.transform(child);
		}
		rp.pop();
	}

	private void renderAtomChild(Geometry child, double[] info, double r, double scale) {
		boolean isOK = (Math.abs(info[2]) > 0.1);
		child.setEnabled(isOK);
		if (!isOK)
			return;
		rp.push();
		{
			rp.rotateY(info[Variables.RY]);// y-angle orientation of arrow number q
			rp.rotateX(info[Variables.RX]);// x-angle orientation of arrow number q
			rp.scale(r, r, 0.62 + info[2] * scale);

			// The factor
			// of
			// 0.62
			// hides
			// the zero-length moments
			// just inside the surface
			// of the spheres.
			rp.transform(child);
		}
		rp.pop();
	}

	private final static int[] buildCell = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 0, 2, 1, 3, 4, 6, 5, 7, 0, 4, 1, 5, 2, 6,
			3, 7 };

	private double[] t3 = new double[3];

	/**
	 * recalculates structural distortions and bond configurations.
	 */
	private void recalcABC() {
		variables.readSliders();
		variables.recalcDistortion();

		enableRendering();

		if (showAtoms || showBonds) {
			variables.setAtomInfo();
		}

		if (showBonds) {
			// calculate the new bondInfo in bond format
			long t = System.currentTimeMillis();

			if (bondsUseBSPT) {
				checkBonding();
			} else {
				bsBondsEnabled.set(0, variables.numBonds);
				for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
					// calculate bondInfo(x-cen, y-cen, z-cen, thetaX, thetaY,
					// length)
					double[] info = variables.bondInfo[b];
					int a0 = (int) info[6];
					int a1 = (int) info[7];

					double[][] info0 = variables.atoms[a0].info;
					double[][] info1 = variables.atoms[a1].info;
					boolean ok = (info[Variables.L_2] > 0 && info[Variables.L_2] < variables.halfMaxBondLength
							&& info0[OCC][0] > variables.minBondOcc && info1[OCC][0] > variables.minBondOcc);
					variables.setBondInfo(info0[DIS], info1[DIS], info, -1);
					bondObjects.child(b).setEnabled(ok);
					bsBondsEnabled.set(b, ok);
				}
//				if (ok)
//					System.out.println("bond " + Math.min(a0,  a1) + " " + Math.max(a0,  a1) + " " + (info[L_2] * 2));
			}
			System.out.println("IsoDistortApp bonds enabled=" + bsBondsEnabled.cardinality() + " t=" + (System.currentTimeMillis() - t));

		}

		// calculate cellInfo (in bond format) associated with each of 12 edges
		/**
		 * Hard coded array specifying which parent cell verticies should be connected
		 * to render unit cell. [link number][vertex 1, vertex 2]
		 * 
		 */
		if (showCells) {
			for (int pt = 0, c = 0; c < 24; c++) {
				double[][] vertices = (c < 12 ? variables.parentCellVertices : variables.superCellCartesianVertices);
				variables.setBondInfo(vertices[buildCell[(pt++) % 24]], vertices[buildCell[(pt++) % 24]], cellInfo[c], -1);
			}
		}

		double[][] paxesbegs = new double[3][3];
		double[][] paxesends = new double[3][3];
		double[][] saxesbegs = new double[3][3];
		double[][] saxesends = new double[3][3];
		double[] extent = new double[3];

		// calculate the parent and supercell coordinate axes in bond format

		double[] pOrigin = variables.pOriginCart;
		double[] sCenter = variables.sCenterCart;
		double[][] sBasisCart = variables.sBasisCart;
		double[][] pBasisCart = variables.pBasisCart;
		double rmax = variables.atomMaxRadius;
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < 3; i++) {
				extent[i] = pOrigin[i] + (t3[i] = pBasisCart[i][axis]) - sCenter[i];
			}
			MathUtil.norm3(t3);
			MathUtil.vecaddN(extent, 2.0 * rmax, t3, paxesbegs[axis]);
			MathUtil.vecaddN(extent, 3.5 * rmax, t3, paxesends[axis]);
			variables.setBondInfo(paxesbegs[axis], paxesends[axis], axesInfo[axis], -1);
			for (int i = 0; i < 3; i++) {
				// BH Q: unless sCenter is [0 0 0], this will fail.
				extent[i] = (t3[i] = sBasisCart[i][axis]) - sCenter[i];
			}
			MathUtil.norm3(t3);
			MathUtil.vecaddN(extent, 1.5 * rmax, t3, saxesbegs[axis]);
			MathUtil.vecaddN(extent, 4.0 * rmax, t3, saxesends[axis]);
			variables.setBondInfo(saxesbegs[axis], saxesends[axis], axesInfo[axis + 3], -1);
		}

		// Calculate the maximum distance from applet center (used to determine FOV).
		double d2 = 0;
		double[][] sVertices = variables.superCellCartesianVertices;
		double[][] pVertices = variables.parentCellVertices;
		for (int j = 0; j < 8; j++) {
			d2 = MathUtil.maxlen2(sVertices[j], d2);
			d2 = MathUtil.maxlen2(pVertices[j], d2);
		}
		for (int i = 0, n = variables.numAtoms; i < n; i++) {
			d2 = MathUtil.maxlen2(variables.atoms[i].info[DIS], d2);
		}
		for (int axis = 0; axis < 3; axis++) {
			d2 = MathUtil.maxlen2(paxesends[axis], d2);
			d2 = MathUtil.maxlen2(saxesends[axis], d2);
		}

		scdSize = Math.sqrt(d2) + 2 * rmax;
		// this includes the width of atoms that might be at the extremes of the
		// longest cell diagonal.
	}

	public void checkBonding() {
		if (variables.bondInfo == null)
			variables.bondInfo = new double[64][];
		bsBondsEnabled.clear();
		int numBonds = variables.numBonds;
		for (int i = 0; i < numBonds; i++)
			bondObjects.child(i).setEnabled(false);
		CubeIterator iterator = variables.getCubeIterator();
		double r = variables.halfMaxBondLength * 2;
		double r2 = r * r;
		double range = variables.halfMaxBondLength * 2;
		double minBondOcc = variables.minBondOcc;
		for (int a1 = 0, n = variables.numAtoms; a1 < n; a1++) {
			double[][] info1 = variables.getAtomInfo(a1);
			if (info1[OCC][0] < minBondOcc)
				continue;
			double[] center = info1[DIS];
			String key1 = a1 + "_";
			iterator.initialize(center, range, false);
			while (iterator.hasNext()) {
				// we store the atom index in the "coordinate" of the atom as the fourth element
				// of that array.
				int a2 = (int) iterator.next()[3];
				double[][] info2;
				double d2 = iterator.foundDistance2(); 
//				System.out.println(a1 + " " + a2 + " " + d2);
				if (a2 <= a1 || d2 < 0.000000000001 || d2 > r2
						|| (info2 = variables.getAtomInfo(a2))[OCC][0] < minBondOcc)
					continue;
				String key12 = key1 + a2;
				double[] ab = variables.getBondinfoFromKey(key12);
				if (ab == null) {
					ab = variables.addBond(key12, a1, a2);
					addBondObject("bond_" + (int) (ab[8]));
				}
				variables.setBondInfo(center, info2[DIS], ab, d2);
				int b = (int) ab[8];
				bsBondsEnabled.set(b);
				bondObjects.child(b).setEnabled(true);
			}
		}
	}

	private void enableRendering() {
		atomObjects.setEnabled(showAtoms);
		cellObjects.setEnabled(showCells);
		bondObjects.setEnabled(showBonds);
		axisObjects.setEnabled(showAxes);
	}

	/**
	 * recalculates the atom colors after a checkbox has been set.
	 */
	private void recalcMaterials() {
		variables.setColors(isSimpleColor);
		variables.recolorPanels();
		enableRendering();
		if (showAtoms)
			recalcAtomMaterials();
		isRecalcMat = false;
	}

	private void recalcAtomMaterials() {
		// Reset the atom colors.
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
					// subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], k, k, k, 1, k, k, k);
					// graduated color scheme
					subMaterial[t][s].setColor(0, 0, 0, k, k, k, 1, k, k, k);
				}
			}
		}
	}

	/**
	 * resets the viewing direction without changing anything else
	 */
	void resetViewDirection(int type) {
		if (type >= 0)
			viewType = type;
		/**
		 * The hkl or uvw view direction indices in lattice coordinates
		 */
		double[] viewIndices = new double[3];
		/**
		 * The view direction in cartesian coordinates
		 */
		double[] viewDir = new double[3];
		/**
		 * variables for viewer computation
		 */
		double xV = 0, yV = 0, zV = 0, tX, tY;
		/**
		 * Temporary 3x3 matrix
		 */
		double[][] tempmat = new double[3][3];
		/**
		 * Array of vectors defining recip parent cell basis. [vector number][x, y, z]
		 * 
		 */
		double[][] recipparentCell = new double[3][3];
		/**
		 * Array of vectors defining recip super cell basis. [vector number][x, y, z]
		 * 
		 */
		double[][] recipsuperCell = new double[3][3];

		viewIndices[0] = Double.parseDouble(uView.getText());
		viewIndices[1] = Double.parseDouble(vView.getText());
		viewIndices[2] = Double.parseDouble(wView.getText());

		MathUtil.mat3inverse(variables.sBasisCart, tempmat);
		MathUtil.mat3transpose(tempmat, recipsuperCell);
		MathUtil.mat3inverse(variables.pBasisCart, tempmat);
		MathUtil.mat3transpose(tempmat, recipparentCell);

		switch (viewType) {
		case VIEW_TYPE_SUPER_HKL:
			MathUtil.mat3transpose(recipsuperCell, tempmat);
			break;
		case VIEW_TYPE_SUPER_UVW:
			MathUtil.mat3transpose(variables.sBasisCart, tempmat);
			break;
		case VIEW_TYPE_PARENT_HKL:
			MathUtil.mat3transpose(recipparentCell, tempmat);
			break;
		case VIEW_TYPE_PARENT_UVW:
			MathUtil.mat3transpose(variables.pBasisCart, tempmat);
			break;
		}
		MathUtil.mat3mul(tempmat, viewIndices, viewDir);
		double l2 = MathUtil.lenSq3(viewDir);
		if (l2 > 0.000000000001) {
			MathUtil.scale3(viewDir, 1 / Math.sqrt(l2));
			xV = viewDir[0];
			yV = viewDir[1];
			zV = viewDir[2];
			tX = Math.asin(yV);
			tY = (Math.abs(Math.cos(tX)) < 0.000001 ? 0
					: Math.abs(zV) < 0.000001 ? -Math.PI / 2 * (xV / Math.abs(xV)) : -Math.atan2(xV, zV));
			rp.clearAngles();
			rp.setCamera(tY, tX);
			updateDisplay();
		}
	}

	@Override
	protected void dispose() {
		if (rp != null)
			rp.removeKeyListener(this);
//		rp.dispose();
//		rp = null;
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
			variables.selectAllSubtypes(false);
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

	@Override
	public BufferedImage getImage() {
		return rp.getImage();
	}

	@Override
	protected void handleButtonEvent(Object src) {
		if (src instanceof JCheckBox) {
			if (!isAdjusting)
				updateViewOptions();
			return;
		}
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

	private void updateViewOptions() {
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
	 * 
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

	}

	@Override
	protected void applyView() {
		resetViewDirection(-1);
	}

	/**
	 * Starts the renderer thread.
	 * 
	 */

	private Timer animationTimer;

	public void start() {
		if (animationTimer == null) {
			int delay = (/**
							 * @j2sNative true ? 50 :
							 */
			100);

			animationTimer = new Timer(delay, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					run();
				}

			});
			animationTimer.setRepeats(true);
		}
		isAnimationRunning = true;
		ttime = System.currentTimeMillis();
		animationTimer.start();
	}

	long ttime = 0;

	@Override
	public void run() {
		long t1 = System.currentTimeMillis();
		long dt = t1 - ttime;
		ttime = t1;
		// System.out.println("IDA timer " + (t1 - ttime));
		if (!isAnimationRunning)
			return;
		boolean animating = (isAnimate || rp.isSpinning());
		if (!animating && --initializing < 0)
			return;
		if (isAnimate) {
			animPhase += 2 * Math.PI / (5000 / dt);
			animPhase = animPhase % (2 * Math.PI);
			double d = Math.sin(animPhase);
			animAmp = d * d;
			variables.setAnimationAmplitude(animAmp);
			needsRecalc = true;
		}
		updateDisplay();
		if (initializing == 0 && !animating)
			stop();
	}

	/**
	 * Stops the renderer thread.
	 * 
	 */
	public void stop() {
		if (animationTimer != null) {
			isAnimationRunning = false;
			animationTimer.stop();
		}
		animationTimer = null;
	}

	@Override
	protected boolean prepareToSwapOut() {
		isAnimate = false;
		if (rp != null) // could be blank window
			rp.setSpinning(false);
		return true;
	}

	@Override
	protected void setControlsFrom(IsoApp a) {
		if (a == null)
			return;
		IsoDistortApp app = (IsoDistortApp) a;
		rp.setPerspective(app.rp.getPerspective());
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
		updateDisplay();
	}

	public static void main(String[] args) {
		create("IsoDistort", args);
	}

}
