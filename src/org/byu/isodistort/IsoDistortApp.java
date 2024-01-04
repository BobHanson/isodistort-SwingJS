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
import java.util.BitSet;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
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

/**
 * 
 * The ISODISTORT app (formerly "applet")
 * 
 * This class handles all GUI associated with rendering and control. It does not 
 * deal with the Variables "sliderPanel". 
 * 
 * 
 * 
 * @author Bob Hanson(mostly just refactoring)
 *
 */
public class IsoDistortApp extends IsoApp implements Runnable, KeyListener {

	/**
	 * the rendering panel (center)
	 * 
	 */
	private RenderPanel3D rp;

	/**
	 * initial value for indicating time required from loading to rendering.
	 */
	public long t0 = System.currentTimeMillis();

	/**
	 * flag to indicate that colors may need adjusting
	 */
	private boolean isTaintedMaterial;

	/**
	 * flag to indicate animation is in progress
	 */
	protected boolean isAnimationRunning;

	/**
	 * true if the animate checkbox is checked
	 */
	private boolean isAnimateSelected;

	private double animPhase = Math.PI / 2;
	private double animAmp = 1;

	/**
	 * no shading in slider panel
	 */
	private boolean isSimpleColor;

	// variables that the user may want to adjust
	/**
	 * Cell-edge tube parameters
	 */
	private final static int numBondSides = 6, numCellSides = 6, numArrowSides = 8;// , ballRes = 8;

	/**
	 * Decimal multiplier to make bond radius and cell radius fractions of atom
	 * radius
	 * 
	 */
	private double bondMultiplier = 0.1, axesMultiplier1 = 0.2, axesMultiplier2 = 0.3;
	private double cellMultiplier = 0.25, momentMultiplier = 0.4, rotationMultiplier = 0.35;

//	Other global variables.
	/**
	 * Initially show bonds or not
	 */
	private boolean showBonds0 = true, showAtoms0 = true, showCells0 = true, showAxes0 = false;
	/**
	 * Currently show bonds or not
	 */
	private boolean showBonds, showAtoms, showCells, showAxes;
	/**
	 * Which type of view direction: superHKL, superUVW, parentHKL, parentUVW
	 */
	private int viewType;

	/**
	 * A geometry for rendering a part of the crystal. Each geometry can hold one or
	 * more shapes. spheres and atoms hold all the atoms; cells holds all 24
	 * cylinders for the parent and super cell.
	 * 
	 */
	private Geometry atomObjects, bondObjects, cellObjects, axisObjects;

// Global variables that pertain to atom, cell and bond properties	

	/**
	 * a BitSet indictating true (bit==1) for bond enabled; false (bit==0) for
	 * disabled
	 */
	private BitSet bsBondsEnabled;

	// Global variables that are related to material properties.
	/**
	 * Materials for coloring bonds and cells.
	 * 
	 */
	private static Material bondMaterial, parentCellMaterial, superCellMaterial, xAxisMaterial, yAxisMaterial,
			zAxisMaterial;

	/**
	 * Array of materials for coloring atoms[type][subtype][regular,highlighted]
	 * 
	 */
	private Material[][] subMaterial;

	/**
	 * Check boxes for zoom, spin, anim toggles
	 * 
	 */
	private JCheckBox aBox, bBox, cBox, spinBox, colorBox, animBox, axesBox;
	/**
	 * Buttons for mouse controls
	 * 
	 */
	private JRadioButton nButton, xButton, yButton, zButton, zoomButton;
	/**
	 * Buttons to use super or parent cell for view vectors
	 * 
	 */
	private JRadioButton superHKL, superUVW, parentHKL, parentUVW;
	/**
	 * Text fields for inputting viewing angles
	 * 
	 */
	private JTextField uView, vView, wView;

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
		double modelRadius = initFieldOfView();
		initAtoms(modelRadius);
		initBonds();
		initCells();
		initAxes();
		buildControls();
		recalcMaterials();
	}

	@Override
	protected void frameResized() {
		super.frameResized();
		needsRecalc = true;
	}

	private void initAtoms(double modelRadius) {
		// Adjustable resolution based on field of view
		int res = (int) Math.min(8, Math.max(2, 50 / modelRadius));
		addStatus("atom shape resolution set to " + res);
		showAtoms = showAtoms0;
		int n = variables.numAtoms;
		atomObjects.clear(0);
		for (int ia = 0; ia < n; ia++) {
			atomObjects.add();
			Atom a = variables.atoms[ia];
			Material m = subMaterial[a.type][a.subType];
			// [MAG,ROT,ELL]
			atomObjects.child(ia).add().arrow(numArrowSides).setMaterial(m); // MAG - 2
			atomObjects.child(ia).add().arrow(numArrowSides).setMaterial(m); // ROT - 2
			atomObjects.child(ia).add().ball(res).setMaterial(m); // ELL - 2
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
		axisObjects.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(xAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(yAxisMaterial);
		axisObjects.add().arrow(numArrowSides).setMaterial(zAxisMaterial);
	}

	/**
	 * Done once, only during initialization.
	 */
	private double initFieldOfView() {
		variables.readSliders();
		variables.recalcDistortion();
		variables.setAtomInfo();

		// Calculate the maximum distance from applet center (used to determine FOV).
		double d2 = variables.parentCell.addRange2(variables.childCell.addRange2(0.0));
		
		for (int i = 0, n = variables.numAtoms; i < n; i++) {
			d2 = MathUtil.maxlen2(variables.atoms[i].getCartesianCoord(), d2);
		}
// BH: this does not have to be so exact. Just adding 2 * variables.atomMaxRadius for this.
//		for (int axis = 0; axis < 3; axis++) {
//			d2 = MathUtil.maxlen2(paxesends[axis], d2);
//			d2 = MathUtil.maxlen2(saxesends[axis], d2);
//		}

		// the 4 here takes care of axes
		double radius = Math.sqrt(d2) + 4 * variables.atomMaxRadius;
		// this includes the width of atoms that might be at the extremes of the
		// longest cell diagonal.

		rp.initializeSettings(radius);
		return radius;
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
		if (isTaintedMaterial) {
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
					renderArrow(a.child(MAG - 2), info[MAG], momentMultiplier, variables.angstromsPerMagneton);
					renderArrow(a.child(ROT - 2), info[ROT], rotationMultiplier, variables.angstromsPerRadian);
					renderEllipsoid(a.child(ELL - 2), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
				}
			}
			double r;
			if (showBonds) {
				r = Math.max(variables.atomMaxRadius * bondMultiplier, 0.05);
				for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
					transformCylinder(r, variables.bondInfo[b], bondObjects.child(b));
				}
			}
			if (showCells) {
				r = variables.atomMaxRadius * cellMultiplier;
				for (int c = 0, i = 0; i < 12; i++, c++) {
					transformCylinder(r, variables.parentCell.getCellInfo(i), cellObjects.child(c));
				}
				for (int c = 12, i = 0; i < 12; i++, c++) {
					transformCylinder(r, variables.childCell.getCellInfo(i), cellObjects.child(c));
				}
			}
			if (showAxes) {
				r = variables.atomMaxRadius * axesMultiplier2;
				for (int a = 0, i = 0; i < 3; i++, a++) {
					transformCylinder(r, variables.parentCell.getAxisInfo(i), axisObjects.child(a));
				}
				r = variables.atomMaxRadius * axesMultiplier1;
				for (int a = 3, i = 0; i < 3; i++, a++) {
					transformCylinder(r, variables.childCell.getAxisInfo(i), axisObjects.child(a));
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

	private void renderArrow(Geometry child, double[] info, double r, double scale) {
		boolean isOK = (Math.abs(info[2]) > 0.1);
		child.setEnabled(isOK);
		if (!isOK)
			return;
		rp.push();
		{
			rp.rotateY(info[1]);// y-angle orientation of arrow number q
			rp.rotateX(info[0]);// x-angle orientation of arrow number q
			rp.scale(r, r, 0.62 + info[2] * scale);
			// BH TODO may be true for standard ball size, but...
			// The factor of 0.62 hides the zero-length moments
			// just inside the surface of the spheres.
			rp.transform(child);
		}
		rp.pop();
	}

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
			checkBonding();
		}
		if (showCells) {
			variables.setCellInfo();
		}
		for (int axis = 0; axis < 3; axis++) {
			variables.setAxisExtents(axis, variables.parentCell, 2.0, 3.5);
			variables.setAxisExtents(axis, variables.childCell, 1.5, 4.0);
		}
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
			Atom a = variables.getAtom(a1);
			if (a.getOccupancy() < minBondOcc)
				continue;
			double[] center = a.getCartesianCoord();
			String key1 = a1 + "_";
			iterator.initialize(center, range, false);
			while (iterator.hasNext()) {
				// we store the atom index in the "coordinate" of the atom as the fourth element
				// of that array.
				int a2 = (int) iterator.next()[3];
				Atom b = variables.getAtom(a2);
				double d2;
				if (a2 <= a1 || (d2 = iterator.foundDistance2()) < 0.000000000001 || d2 > r2
						|| b.getOccupancy() < minBondOcc)
					continue;
				String key12 = key1 + a2;
				double[] ab = variables.getBondinfoFromKey(key12);
				if (ab == null) {
					ab = variables.addBond(key12, a1, a2);
					addBondObject("bond_" + (int) (ab[8]));
				}
				variables.setCylinderInfo(center, b.getCartesianCoord(), ab, d2);
				int bondIndex = (int) ab[8];
				bsBondsEnabled.set(bondIndex);
				bondObjects.child(bondIndex).setEnabled(true);
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
		isTaintedMaterial = false;
	}

	private void recalcAtomMaterials() {
		// Reset the atom colors.
		double[] rgb = new double[3];
		for (int t = 0; t < variables.numTypes; t++) {
			variables.getColors(t, rgb);
			for (int s = 0; s < variables.numSubTypes[t]; s++) {
				if (!variables.isSubTypeSelected(t, s)) {
					// makes the atom color same as above type color
					subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], 0.3, 0.3, 0.3, 1, .0001, .0001, .0001);
				} else {
					// graduated color scheme // BH not quite so black
					double k = (double) 0.2 + 0.3 * s / (variables.numSubTypes[t] - 1);
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

		viewIndices[0] = Double.parseDouble(uView.getText());
		viewIndices[1] = Double.parseDouble(vView.getText());
		viewIndices[2] = Double.parseDouble(wView.getText());

		double[][] tempmat = null;
		switch (viewType) {
		case VIEW_TYPE_SUPER_HKL:
			tempmat = variables.childCell.getTempTransposedReciprocalBasis();
			break;
		case VIEW_TYPE_SUPER_UVW:
			tempmat = variables.childCell.getTempTransposedCartesianBasis();
			break;
		case VIEW_TYPE_PARENT_HKL:
			tempmat = variables.parentCell.getTempTransposedReciprocalBasis();
			break;
		case VIEW_TYPE_PARENT_UVW:
			tempmat = variables.parentCell.getTempTransposedCartesianBasis();
			break;
		}
		MathUtil.mat3mul(tempmat, viewIndices, viewDir);
		double l2 = MathUtil.lenSq3(viewDir);
		if (l2 > 0.000000000001) {
			MathUtil.scale3(viewDir, 1 / Math.sqrt(l2));
			double xV = viewDir[0];
			double yV = viewDir[1];
			double zV = viewDir[2];
			double tX = Math.asin(yV);
			double tY = (Math.abs(Math.cos(tX)) < 0.000001 ? 0
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
			reset();
			break;
		case 'n':
		case 'N':
			// BH Q: Why would one ever want to do this?
			rp.reversePanningAction();
			updateDisplay();
			break;
		case 'c':
		case 'C':
			centerImage();
			updateDisplay();
			break;
		default:
			variables.keyTyped(e);
			break;
		}
	}

	@Override
	public void reset() {
		rp.clearAngles();
		rp.resetView();
		centerImage();
		animPhase = Math.PI / 2;
		animAmp = 1;
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
		variables.resetSliders();
		variables.readSliders();
		variables.recalcDistortion();
		variables.clearSubtypeSelection();
		updateDisplay();
	}

	@Override
	public void centerImage() {
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

	protected void updateViewOptions() {
		showAtoms = aBox.isSelected();
		showBonds = bBox.isSelected();
		showCells = cBox.isSelected();
		showAxes = axesBox.isSelected();
		boolean spin = spinBox.isSelected();
		isSimpleColor = colorBox.isSelected();
		isAnimateSelected = animBox.isSelected();
		isTaintedMaterial = true;
		rp.setSpinning(spin);
		if (isAnimateSelected || spin) {
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

		JPanel top = new JPanel();
		top.setBackground(Color.WHITE);
		top.add(nButton);
		top.add(xButton);
		top.add(yButton);
		top.add(zButton);
		top.add(zoomButton);
		top.add(new JLabel("       "));
		top.add(aBox);
		top.add(bBox);
		top.add(cBox);
		top.add(axesBox);
		top.add(spinBox);
		top.add(animBox);
		top.add(colorBox);

		JPanel bottom = new JPanel();
		bottom.setBackground(Color.WHITE);
		bottom.add(superHKL);
		bottom.add(superUVW);
		bottom.add(parentHKL);
		bottom.add(parentUVW);
		bottom.add(new JLabel("          Direction: "));
		bottom.add(uView);
		bottom.add(vView);
		bottom.add(wView);

		addSaveButtons(bottom);

		controlPanel.add(top);
		controlPanel.add(bottom);

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
		boolean animating = (isAnimateSelected || rp.isSpinning());
		if (!animating && --initializing < 0)
			return;
		if (isAnimateSelected) {
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
		isAnimateSelected = false;
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
