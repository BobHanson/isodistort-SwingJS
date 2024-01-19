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
import java.util.HashMap;
import java.util.Map;

import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.Timer;

import org.byu.isodistort.local.Bspt.CubeIterator;
import org.byu.isodistort.local.Iso3DApp;
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
 * This class handles all 3D GUI associated with rendering and control. It does not 
 * deal with the Variables "sliderPanel". 
 * 
 * 
 * 
 * 
 * @author Bob Hanson(mostly just refactoring)
 *
 */
public class IsoDistortApp extends Iso3DApp implements Runnable, KeyListener {

	private RenderPanel3D rp3;
	

	/**
	 * initial value for indicating time required from loading to rendering.
	 */
	public long t0 = System.currentTimeMillis();

	// variables that the user may want to adjust
	/**
	 * Cell-edge tube parameters
	 */
	private final static int numBondSides = 6, numCellSides = 6, numArrowSides = 8;// , ballRes = 8;

	/**
	 * A geometry for rendering a part of the crystal. Each geometry can hold one or
	 * more shapes. spheres and atoms hold all the atoms; cells holds all 24
	 * cylinders for the parent and child cell.
	 * 
	 */
	private Geometry atomObjects, bondObjects, axisObjects;
	
	private Geometry[] cellObjects = new Geometry[2];

	/**
	 * Materials for coloring bonds and cells.
	 * 
	 */
	private Material bondMaterial, parentCellMaterial, childCellMaterial, xAxisMaterial, yAxisMaterial,
			zAxisMaterial;

	/**
	 * Array of materials for coloring atoms[type][subtype][regular,highlighted]
	 * 
	 */
	private Material[][] subMaterial;

	/**
	 * a BitSet indictating true (bit==1) for bond enabled; false (bit==0) for
	 * disabled
	 */
	private BitSet bsBondsEnabled;

	public IsoDistortApp() {
		super(APP_ISODISTORT);
	}

	@Override
	protected void init() {
		setRenderPanel(rp3 = new RenderPanel3D(this));
		rp3.setPreferredSize(drawPanel.getSize());
		rp3.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add((JPanel) rp3, BorderLayout.CENTER);
		rp3.addKeyListener(this);
		initMaterials();
		Geometry world = rp3.getWorld();
		atomObjects = world.add();
		bondObjects = world.add();
		cellObjects[0] = world.add();
		cellObjects[1] = world.add();
		axisObjects = world.add();
		
		double modelRadius = initFieldOfView();
		initAtoms(modelRadius);
		initBonds();
		initCells();
		initAxes();
		buildControls();
		enableSelectedObjects();
		recalcMaterials();
	}

	@Override
	protected void enableSelectedObjects() {
		atomObjects.setEnabled(showAtoms);
		cellObjects[0].setEnabled(showParentCell);
		cellObjects[1].setEnabled(showChildCell);
		bondObjects.setEnabled(showBonds);
		axisObjects.setEnabled(showAxes);
	}

	private void initAtoms(double modelRadius) {
		// Adjustable resolution based on field of view
		rp3.initializeSettings(modelRadius);
		showAtoms = showAtoms0;
		int n = variables.numAtoms;
		atomObjects.clear(0);
		int res =  (n < 200 ? 7 : (int) Math.min(8, Math.max(2, 50 / modelRadius)));
		addStatus("atom shape resolution set to " + res);
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
		int n = numBonds;
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
		showChildCell = showChildCell0;
		showParentCell = showParentCell0;
		cellObjects[0].clear(0);
		cellObjects[1].clear(0);
		for (int c = 0; c < 12; c++) {
			cellObjects[0].add().cylinder(numCellSides).setMaterial(parentCellMaterial);
		}
		for (int c = 0; c < 12; c++) {
			cellObjects[1].add().cylinder(numCellSides).setMaterial(childCellMaterial);
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
	 * initMaterials instantiates an array of colors for the atoms. These methods
	 * are from the render package
	 * 
	 */
	protected void initMaterials() {
		parentCellMaterial = rp3.newMaterial();
		childCellMaterial = rp3.newMaterial();
		// parent cell slightly red
		parentCellMaterial.setColor(.8, .5, .5, 1.5, 1.5, 1.5, 20, .30, .30, .30);
		// child cell slightly blue
		childCellMaterial.setColor(.5, .5, .8, 1.5, 1.5, 1.5, 20, .30, .30, .30);

		bondMaterial = rp3.newMaterial();
		bondMaterial.setColor(0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 20, 0.2, 0.2, 0.2);// bonds are black

		xAxisMaterial = rp3.newMaterial();
		yAxisMaterial = rp3.newMaterial();
		zAxisMaterial = rp3.newMaterial();
		xAxisMaterial.setColor(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 20, 0.0, 0.0, 0.0);// bonds are black
		yAxisMaterial.setColor(0.5, 0.5, 0.5, 0.5, 0.5, 0.5, 20, 0.5, 0.5, 0.5);// bonds are black
		zAxisMaterial.setColor(0.25, 0.25, 0.25, 0.25, 0.25, 0.25, 20, 0.25, 0.25, 0.25);// bonds are black

		subMaterial = new Material[variables.numTypes][];

		// Create the subMaterial array;
		for (int t = 0, nt = variables.numTypes; t < nt; t++) {
			subMaterial[t] = new Material[variables.numSubTypes[t]];
			for (int s = 0, nst = variables.numSubTypes[t]; s < nst; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = rp3.newMaterial();
		}
	}

	private static final int VIEW_TYPE_CHILD_HKL = 1;
	private static final int VIEW_TYPE_CHILD_UVW = 2;
	private static final int VIEW_TYPE_PARENT_HKL = 3;
	private static final int VIEW_TYPE_PARENT_UVW = 4;

	@Override
	protected void renderCells() {
		double r = variables.atomMaxRadius * cellMultiplier;
		for (int i = 0; i < 12; i++) {
			transformCylinder(r, variables.parentCell.getCellInfo(i), cellObjects[0].child(i));
		}
		for (int i = 0; i < 12; i++) {
			transformCylinder(r, variables.childCell.getCellInfo(i), cellObjects[1].child(i));
		}
	}

	@Override
	protected void renderAxes() {
		double r = variables.atomMaxRadius * axesMultiplier2;
		for (int a = 0, i = 0; i < 3; i++, a++) {
			transformCylinder(r, variables.parentCell.getAxisInfo(i), axisObjects.child(a));
		}
		r = variables.atomMaxRadius * axesMultiplier1;
		for (int a = 3, i = 0; i < 3; i++, a++) {
			transformCylinder(r, variables.childCell.getAxisInfo(i), axisObjects.child(a));
		}
	}

	@Override
	protected void renderAtoms() {
		for (int i = 0, n = variables.numAtoms; i < n; i++) {
			double[][] info = variables.getAtomInfo(i);
			Geometry a = atomObjects.child(i);
			renderScaledAtom(a, info[DIS], info[OCC][0] * variables.atomMaxRadius);
			renderArrow(a.child(MAG - 2), info[MAG], momentMultiplier, variables.angstromsPerMagneton);
			renderArrow(a.child(ROT - 2), info[ROT], rotationMultiplier, variables.angstromsPerRadian);
			renderEllipsoid(a.child(ELL - 2), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
		}
	}

	@Override
	protected void renderBonds() {
		double r = Math.max(variables.atomMaxRadius * bondMultiplier, 0.05);
		for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
			transformCylinder(r, bondInfo[b], bondObjects.child(b));
		}
	}

	private void transformCylinder(double r, double[] info, Geometry child) {
		rp3.push();
		{
			rp3.translate(info);
			rp3.rotateY(info[Variables.RY]);
			rp3.rotateX(info[Variables.RX]);
			rp3.scale(r, r, info[Variables.L_2]);
			rp3.transform(child);
		}
		rp3.pop();
	}

	private void renderScaledAtom(Geometry child, double[] xyz, double r) {

		rp3.push();
		{
			rp3.translate(xyz);
			rp3.scale(r, r, r);
			rp3.transform(child);
		}
		rp3.pop();
	}

	private void renderEllipsoid(Geometry child, double[] info, double f) {
		rp3.push();
		{
			rp3.scale(info[0] * f, info[1] * f, info[2] * f);
			rp3.transform(child);
		}
		rp3.pop();
	}

	private void renderArrow(Geometry child, double[] info, double r, double scale) {
		boolean isOK = (Math.abs(info[2]) > 0.1);
		child.setEnabled(isOK);
		if (!isOK)
			return;
		rp3.push();
		{
			rp3.rotateY(info[1]);// y-angle orientation of arrow number q
			rp3.rotateX(info[0]);// x-angle orientation of arrow number q
			rp3.scale(r, r, 0.62 + info[2] * scale);
			// BH TODO may be true for standard ball size, but...
			// The factor of 0.62 hides the zero-length moments
			// just inside the surface of the spheres.
			rp3.transform(child);
		}
		rp3.pop();
	}

	@Override
	protected void checkBonding() {
		if (bondInfo == null)
			bondInfo = new double[64][];
		bsBondsEnabled.clear();
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
				double[] ab = getBondinfoFromKey(key12);
				if (ab == null) {
					ab = addBond(key12, a1, a2);
					addBondObject("bond_" + (int) (ab[8]));
				}
				variables.setCylinderInfo(center, b.getCartesianCoord(), ab, d2);
				int bondIndex = (int) ab[8];
				bsBondsEnabled.set(bondIndex);
				bondObjects.child(bondIndex).setEnabled(true);
			}
		}
	}
	
	/**
	 * Total number of bonds
	 */
	public int numBonds;
	/**
	 * bondInfo[bond index] = {avx, avy, avz, angleX, angleY, len^2, atom1 index,
	 * atom2 index, bond index} reference atomInfo (below)
	 * 
	 */
	public double[][] bondInfo; // [numBonds][9]

	/**
	 * Map to retrieve bonds by atom1-atom2 number string key
	 */
	private Map<String, double[]> knownBonds;

	public double[] getBondinfoFromKey(String key12) {
		if (knownBonds == null)
			return null;
		return knownBonds.get(key12);

	}


	public double[] addBond(String key12, int a1, int a2) {
		if (numBonds == bondInfo.length) {
			double[][] bi = new double[numBonds * 2][];
			for (int j = numBonds; --j >= 0;)
				bi[j] = bondInfo[j];
			bondInfo = bi;
		}
		double[] ab = new double[9];
		ab[6] = a1;
		ab[7] = a2;
		ab[8] = numBonds;
		bondInfo[numBonds] = ab;
		if (knownBonds == null)
			knownBonds = new HashMap<>();
		knownBonds.put(key12, ab);
		numBonds++;
		return ab;
	}

	@Override
	public void recalcCellColors() {
		// parent cell slightly red
		parentCellMaterial.setColor(.8, .5, .5, 1.5, 1.5, 1.5, 20, .30, .30, .30);
		// child cell slightly blue
		childCellMaterial.setColor(.5, .5, .8, 1.5, 1.5, 1.5, 20, .30, .30, .30);
//
//	nah!
//
//		double c = variables.getSetChildSliderFraction(Double.NaN);
//		double p = 1 - c;
//		// f = f/.8;
//		// parent cell slightly red
//		parentCellMaterial.setColor(.8 + .2 * c, .5 + 0.5*  c, .5 + 0.5 * c, 1.5, 1.5, 1.5, 20, .30, .30, .30);
//		// child cell slightly blue
//		childCellMaterial.setColor(.5 + 0.6 * p, .5 + 0.5 * p, .8 + 0.2 * p, 1.5, 1.5, 1.5, 20, .30, .30, .30);
	}

	@Override
	protected void recalcAtomColors() {
		double[] rgb = new double[3];
		for (int t = variables.numTypes; --t >= 0;) {
			variables.getColors(t, rgb);
			for (int s = variables.numSubTypes[t]; --s >= 0;) {
				if (variables.isSubTypeSelected(t, s)) {
					// a darkish shade of gray
					double k = variables.getSelectedSubTypeShade(t, s);
					subMaterial[t][s].setColor(0, 0, 0, k, k, k, 1, k, k, k);
				} else {
					// makes the atom color same its type color
					subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], 0.3, 0.3, 0.3, 1, 0.0001, 0.0001, 0.0001);
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
		case VIEW_TYPE_CHILD_HKL:
			tempmat = variables.childCell.getTempTransposedReciprocalBasis();
			break;
		case VIEW_TYPE_CHILD_UVW:
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
			rp3.clearAngles();
			rp3.setCamera(tY, tX);
			updateDisplay();
		}
	}

	@Override
	protected void dispose() {
		if (rp3 != null)
			rp3.removeKeyListener(this);
		rp3.dispose();
		rp3 = null;
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
			rp3.reversePanningAction();
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
		rp3.clearAngles();
		rp3.resetView();
		centerImage();
		animPhase = Math.PI / 2;
		animAmp = 1;
		showAtoms = showAtoms0;
		aBox.setSelected(showAtoms);
		showBonds = showBonds0;
		bBox.setSelected(showBonds);
		showParentCell = showParentCell0;
		showChildCell = showChildCell0;
		cpBox.setSelected(showParentCell);
		ccBox.setSelected(showChildCell);
		showAxes = showAxes0;
		axesBox.setSelected(showAxes);
		animBox.setSelected(false);
		spinBox.setSelected(false);
		clrBox.setSelected(false);
//		nButton.setSelected(true);
		uView.setText("0");
		vView.setText("0");
		wView.setText("1");
		childHKL.setSelected(true);
		variables.resetSliders();
		variables.readSliders();
		variables.recalcDistortion();
		variables.clearSubtypeSelection();
		updateDisplay();
	}

	@Override
	public void centerImage() {
		rp3.centerImage();
		updateDisplay();
	}

	@Override
	public BufferedImage getImage() {
		return rp3.getImage();
	}

	@Override
	protected void handleButtonEvent(Object src) {
		if (!((JToggleButton) src).isSelected())
			return;
//		if (src == nButton) {
//			rp3.clearAngles();
//			rp3.setRotationAxis(0);
//		} else if (src == xButton) {
//			rp3.clearAngles();
//			rp3.setRotationAxis(1);
//		} else if (src == yButton) {
//			rp3.clearAngles();
//			rp3.setRotationAxis(2);
//		} else if (src == zButton) {
//			rp3.clearAngles();
//			rp3.setRotationAxis(3);
//		} else if (src == zoomButton) {
//			rp3.clearAngles();
//			rp3.setRotationAxis(4);
//		} else 
			if (src == childHKL) {
			resetViewDirection(VIEW_TYPE_CHILD_HKL);
		} else if (src == childUVW) {
			resetViewDirection(VIEW_TYPE_CHILD_UVW);
		} else if (src == parentHKL) {
			resetViewDirection(VIEW_TYPE_PARENT_HKL);
		} else if (src == parentUVW) {
			resetViewDirection(VIEW_TYPE_PARENT_UVW);
		}
		updateDisplay();

	}

	@Override
	protected void updateViewOptions() {
		showAtoms = aBox.isSelected();
		showBonds = bBox.isSelected();
		showParentCell = cpBox.isSelected();
		showChildCell = ccBox.isSelected();
		showAxes = axesBox.isSelected();
		boolean spin = spinBox.isSelected();
		isAnimateSelected = animBox.isSelected();
		clrBox.setVisible(variables.needSimpleColor);
		isMaterialTainted = true;
		rp3.setSpinning(spin);
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
		cpBox = newJCheckBox("Parent Cell", showParentCell);
		ccBox = newJCheckBox("Child Cell", showChildCell);
		axesBox = newJCheckBox("Axes", showAxes);
		animBox = newJCheckBox("Animate", false);
		spinBox = newJCheckBox("Spin", false);

//		ButtonGroup xyzButtons = new ButtonGroup();
//		nButton = newRadioButton("Normal", true, xyzButtons);
//		xButton = newRadioButton("Xrot", false, xyzButtons);
//		yButton = newRadioButton("Yrot", false, xyzButtons);
//		zButton = newRadioButton("Zrot", false, xyzButtons);
//		zoomButton = newRadioButton("Zoom", false, xyzButtons);

		viewType = VIEW_TYPE_CHILD_HKL;

		ButtonGroup cellButtons = new ButtonGroup();
		childHKL = newRadioButton("SupHKL", true, cellButtons);
		childUVW = newRadioButton("SupUVW", false, cellButtons);
		parentHKL = newRadioButton("ParHKL", false, cellButtons);
		parentUVW = newRadioButton("ParUVW", false, cellButtons);
		uView = newTextField("0", -10);
		vView = newTextField("0", -10);
		wView = newTextField("1", -10);

		JPanel top = new JPanel();
		top.setBackground(Color.WHITE);
//		top.add(nButton);
//		top.add(xButton);
//		top.add(yButton);
//		top.add(zButton);
//		top.add(zoomButton);
		top.add(new JLabel("       "));
		top.add(aBox);
		top.add(bBox);
		top.add(cpBox);
		top.add(ccBox);
		top.add(axesBox);
		top.add(spinBox);
		top.add(animBox);

		addTopButtons(top);

		JPanel bottom = new JPanel();
		bottom.setBackground(Color.WHITE);
		bottom.add(childHKL);
		bottom.add(childUVW);
		bottom.add(parentHKL);
		bottom.add(parentUVW);
		bottom.add(new JLabel("          Direction: "));
		bottom.add(uView);
		bottom.add(vView);
		bottom.add(wView);

		addBottomButtons(bottom);

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
		boolean animating = (isAnimateSelected || rp3.isSpinning());
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
		if (rp3 != null) // could be blank window
			rp3.setSpinning(false);
		return true;
	}

	public static void main(String[] args) {
		create("IsoDistort", args);
	}


}
