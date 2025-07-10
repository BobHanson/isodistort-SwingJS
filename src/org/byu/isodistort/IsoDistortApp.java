/**
 *  David Tanner
 *  April 2005
 * 
 * 
 */

package org.byu.isodistort;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
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
import org.byu.isodistort.local.Variables.IsoAtom;
import org.byu.isodistort.render.Geometry;
// import org.byu.isodistort.render.Matrix;
import org.byu.isodistort.render.RenderPanel3D;
import org.byu.isodistort.render.RenderPanel3D.IsoMaterial;

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
public class IsoDistortApp extends Iso3DApp implements Runnable {

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
	private Geometry atomObjects;

	protected Geometry bondObjects;
	
	private Geometry[] cellObjects = new Geometry[2], axisObjects = new Geometry[2];

	/**
	 * Materials for coloring bonds and cells.
	 * 
	 */
	private IsoMaterial bondMaterial, parentCellMaterial, childCellMaterial, aAxisMaterial, bAxisMaterial,
			cAxisMaterial;

	/**
	 * Array of materials for coloring atoms[type][subtype][regular,highlighted]
	 * 
	 */
	protected IsoMaterial[][] subMaterial;

	/**
	 * a BitSet indictating true (bit==1) for bond enabled; false (bit==0) for
	 * disabled
	 */
	protected BitSet bsBondsEnabled;

	protected Iso3DApp fromApp;

	public IsoDistortApp() {
		super(APP_ISODISTORT);
	}

	//////////////// initialize ///////////////////
	
	/**
	 * once-run, from AsoApp.startApp
	 */
	@Override
	protected void init() {
		fromApp = frame.from3DApp;
		rp = rp3 = new RenderPanel3D(this);
		drawPanel.removeAll();
		drawPanel.add((JPanel) rp);
		initWorld(rp3.getWorld());
	}

	protected void initWorld(Geometry world) {
		initMaterials();
		atomObjects = world.add();
		bondObjects = world.add();
		cellObjects[0] = world.add();
		cellObjects[1] = world.add();
		axisObjects[0] = world.add();
		axisObjects[1] = world.add();
		
		variables.initCellsAndAxes();
		double modelRadius = initFieldOfView();
		initAtoms(modelRadius);
		initBonds();
		initCells();
		initAxes();
		initControls();
		updateGUI();
		updateSelectedObjects();
		updateAtomColors();
	}

	protected void initAtoms(double modelRadius) {
		// Adjustable resolution based on field of view
		rp.initializeSettings(modelRadius);
		showAtoms = showAtoms0;
		int n = variables.nAtoms;
		atomObjects.clear(0);
		int res =  (n < 200 ? 7 : (int) Math.min(8, Math.max(2, 50 / modelRadius)));
		addStatus("atom shape resolution set to " + res);
		for (int ia = 0; ia < n; ia++) {
			atomObjects.add();
			IsoAtom a = variables.atoms[ia];
			IsoMaterial m = subMaterial[a.type][a.subType];
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
		int n = nBonds;
		bsBondsEnabled = new BitSet();
		bondObjects.clear(0);
		if (n > 0) {
			for (int b = 0; b < n; b++)
				addBondObject("bond_" + b);
		}
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
		axisObjects[0].add().arrow(numArrowSides).setMaterial(aAxisMaterial);
		axisObjects[0].add().arrow(numArrowSides).setMaterial(bAxisMaterial);
		axisObjects[0].add().arrow(numArrowSides).setMaterial(cAxisMaterial);
		axisObjects[1].add().arrow(numArrowSides).setMaterial(aAxisMaterial);
		axisObjects[1].add().arrow(numArrowSides).setMaterial(bAxisMaterial);
		axisObjects[1].add().arrow(numArrowSides).setMaterial(cAxisMaterial);
	}

	protected IsoMaterial newMaterial() {
		return (IsoMaterial) rp3.newMaterial();
	}

	/**
	 * initMaterials instantiates an array of colors for the atoms. These methods
	 * are from the render package
	 * 
	 */
	protected void initMaterials() {
		// parent cell slightly red
		// note that this color is also in Variable.GUI.COLOR_PARENT_CELL
		// child cell slightly blue
		// note that this color is also in Variable.GUI.COLOR_CHILD_CELL
		parentCellMaterial = (IsoMaterial) newMaterial().setColor(.8, .5, .5, 1.5, 1.5, 1.5, 20, .30, .30, .30);
		childCellMaterial = (IsoMaterial) newMaterial().setColor(.5, .5, .8, 1.5, 1.5, 1.5, 20, .30, .30, .30);
		// bonds are black
		bondMaterial = (IsoMaterial) newMaterial().setGrayScale(0.2, 0.2, 20, 0.2);
		
		aAxisMaterial = (IsoMaterial) newMaterial().setColor(Color.RED);
		bAxisMaterial = (IsoMaterial) newMaterial().setColor(Color.GREEN);
		cAxisMaterial = (IsoMaterial) newMaterial().setColor(Color.BLUE);
		
		subMaterial = new IsoMaterial[variables.nTypes][];

		// Create the subMaterial array;
		for (int t = 0, nt = variables.nTypes; t < nt; t++) {
			subMaterial[t] = new IsoMaterial[variables.nSubTypes[t]];
			for (int s = 0, nst = variables.nSubTypes[t]; s < nst; s++)// iterate over number-of-subtypes
				subMaterial[t][s] = newMaterial();
		}
	}

	/**
	 * creates the components of the control panel
	 * 
	 */
	protected void initControls() {

		aBox = newJCheckBox("Atoms", showAtoms);
		apBox = newJCheckBox("Primitive Only", showPrimitiveAtoms);
		apBox.setVisible(showPrimitiveBox);
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
		top.add(apBox);
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
			rp3.scale(r, r, ARROW_MIN_LENGTH + info[2] * scale);
			// BH TODO may be true for standard ball size, but...
			// The factor of 0.62 hides the zero-length moments
			// just inside the surface of the spheres.
			rp3.transform(child);
		}
		rp3.pop();
	}

	@Override
	protected void updateBonding() {
		if (bondInfo == null)
			bondInfo = new double[64][];
		bsBondsEnabled.clear();
		for (int i = 0; i < nBonds; i++)
			bondObjects.child(i).setEnabled(false);
		CubeIterator iterator = variables.getCubeIterator();
		double rMax = variables.getMaxBondLength();
		double rMax2 = rMax * rMax;
		double rMin = variables.getMinBondLength();
		double rMin2 = rMin * rMin;
		double minBondOcc = variables.minBondOcc;
		for (int a1 = 0, n = variables.nAtoms; a1 < n; a1++) {
			IsoAtom a = variables.getAtom(a1);
			if (a.getOccupancy() < minBondOcc || showPrimitiveAtoms && !variables.isPrimitive(a1))
				continue;
			double[] center = a.getCartesianCoord();
			String key1 = a1 + "_";
			iterator.initialize(center, rMax, false);
			while (iterator.hasNext()) {
				// we store the atom index in the "coordinate" of the atom as the fourth element
				// of that array.
				int a2 = (int) iterator.next()[3];
				IsoAtom b = variables.getAtom(a2);
				double d2;
				if (a2 <= a1 || 
						(d2 = iterator.foundDistance2()) < 0.000000000001 
						|| d2 > rMax2 || d2 < rMin2		
						|| b.getOccupancy() < minBondOcc 
						|| showPrimitiveAtoms && !variables.isPrimitive(a2))
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
	 * For dynamic addition of bonds by variables using the Binary
	 */
	public void addBondObject(String name) {
		bondObjects.add().tube(numBondSides).setMaterial(bondMaterial).setName(name);
	}

	
	/**
	 * Total number of bonds
	 */
	public int nBonds;
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
		if (nBonds == bondInfo.length) {
			double[][] bi = new double[nBonds * 2][];
			for (int j = nBonds; --j >= 0;)
				bi[j] = bondInfo[j];
			bondInfo = bi;
		}
		double[] ab = new double[9];
		ab[6] = a1;
		ab[7] = a2;
		ab[8] = nBonds;
		bondInfo[nBonds] = ab;
		if (knownBonds == null)
			knownBonds = new HashMap<>();
		knownBonds.put(key12, ab);
		nBonds++;
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
	protected void updateAtomColors() {
		double[] rgb = new double[3];
		for (int t = variables.nTypes; --t >= 0;) {
			variables.getColors(t, rgb);
			for (int s = variables.nSubTypes[t]; --s >= 0;) {
				// BH reversing this action
				if (!variables.isSubTypeSelected(t, s)) {
					// a darkish shade of gray
					double k = variables.getSelectedSubTypeShade(t, s);
					subMaterial[t][s].setGrayScale(0, k, 1, k);
				} else {
					// makes the atom color same its type color
					subMaterial[t][s].setColor(rgb[0], rgb[1], rgb[2], 0.3, 0.3, 0.3, 1, 0.0001, 0.0001, 0.0001);
				}
			}
		}
	}

	/**
	 * Not really of any interest if it is just atoms in the unit cell and not 
	 * actually a primitive atom set.
	 */
	private boolean showPrimitiveBox = false;

	/**
	 * resets the viewing direction without changing anything else
	 */
	void resetViewDirection(int type) {
		/**
		 * The view direction in cartesian coordinates
		 */
		double[] viewDir = new double[3];

		double[] viewIndices = variables.getViewIndices();
		viewIndices[0] = getTextValue(uView, viewIndices[0], 2);
		viewIndices[1] = getTextValue(vView, viewIndices[1], 2);
		viewIndices[2] = getTextValue(wView, viewIndices[2], 2);

		double[][] tempmat = null;
		if (type >= 0)
			viewType = type;
		switch (viewType) {
		case VIEW_TYPE_CHILD_UVW:
			tempmat = variables.childCell.basisCart;
			break;
		case VIEW_TYPE_PARENT_UVW:
			tempmat = variables.parentCell.basisCart;
			break;
		case VIEW_TYPE_CHILD_HKL:
			tempmat = variables.childCell.getTempTransposedReciprocalBasis();
			break;
		default:
		case VIEW_TYPE_PARENT_HKL:
			tempmat = variables.parentCell.getTempTransposedReciprocalBasis();
			break;
		}
		MathUtil.mat3mul(tempmat, viewIndices, viewDir);
		double l2 = MathUtil.lenSq3(viewDir);
		if (l2 > 0.000000000001) {
			MathUtil.norm3(viewDir);
			setViewDir(viewDir);
			centerImage();
		}
	}

	protected void setViewDir(double[] viewDir) {
		double xV = viewDir[0];
		double yV = viewDir[1];
		double zV = viewDir[2];
		double phi = Math.asin(yV);
		double theta = (Math.abs(Math.cos(phi)) < 0.000001 ? 0
				: Math.abs(zV) < 0.000001 ? -Math.PI / 2 * (xV / Math.abs(xV)) 
				: -Math.atan2(xV, zV));
		rp3.clearAngles();
		double sigma = 0;
		rp3.setCamera(theta, phi, sigma);
	}

	@Override
	protected void dispose() {
		if (rp3 != null) {
			rp3.dispose();
			rp3 = null;
		}
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
		case 'x':
		case 'X':
		case 'y':
		case 'Y':
		case 'n':
		case 'N':
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
		showPrimitiveAtoms = showPrimitiveAtoms0;
		apBox.setSelected(showPrimitiveAtoms);
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
		colorBox.setSelected(false);
//		nButton.setSelected(true);
		uView.setText("0");
		vView.setText("0");
		wView.setText("1");
		childHKL.setSelected(true);
		variables.resetSliders();
		variables.readSliders();
		variables.recalcDistortion();
		variables.selectAllSubtypes();
		updateDisplay();
	}

	@Override
	final public void centerImage() {
		rp.centerImage();
		updateDisplay();
	}

	@Override
	public BufferedImage getImage() {
		return rp.getImage();
	}

	@Override
	protected void handleButtonEvent(Object src) {
		if (!((JToggleButton) src).isSelected())
			return;
		if (src == apBox) {
			needsRecalc = true;
		} else if (src == childHKL) {
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
		showPrimitiveAtoms = apBox.isSelected();
		showBonds = bBox.isSelected();
		showParentCell = cpBox.isSelected();
		showChildCell = ccBox.isSelected();
		showAxes = axesBox.isSelected();
		boolean spin = spinBox.isSelected();
		isAnimateSelected = animBox.isSelected();
		colorBox.setVisible(variables.needColorBox);
		isAtomColorChanged = true;
		rp.setSpinning(spin);
		if (isAnimateSelected || spin) {
			start();
		} else {
			variables.isChanged = true;
			updateDisplay();
		}
	}

	//////////// update display from changes ///////////
	
	@Override
	protected void updateSelectedObjects() {
		apBox.setEnabled(variables.isPrimitive(-1));
		atomObjects.setEnabled(showAtoms);
		cellObjects[0].setEnabled(showParentCell);
		cellObjects[1].setEnabled(showChildCell);
		bondObjects.setEnabled(showBonds);
		axisObjects[0].setEnabled(showAxes && (showParentCell || !showChildCell));
		axisObjects[1].setEnabled(showAxes && (showChildCell || !showParentCell));
	}

	protected static final int VIEW_TYPE_CHILD_HKL = 1;
	protected static final int VIEW_TYPE_CHILD_UVW = 2;
	protected static final int VIEW_TYPE_PARENT_HKL = 3;
	protected static final int VIEW_TYPE_PARENT_UVW = 4;

	@Override
	protected void updateCells() {
		for (int i = 0; i < 12; i++) {
			transformCylinder(CELL_RADIUS, variables.parentCell.getCellInfo(i), cellObjects[0].child(i));
		}
		for (int i = 0; i < 12; i++) {
			transformCylinder(CELL_RADIUS, variables.childCell.getCellInfo(i), cellObjects[1].child(i));
		}
	}

	@Override
	protected void updateAxes() {
		for (int i = 0; i < 3; i++) {
			transformCylinder(AXIS_RADII[PARENT], variables.parentCell.getAxisInfo(i), axisObjects[0].child(i));
			transformCylinder(AXIS_RADII[CHILD], variables.childCell.getAxisInfo(i), axisObjects[1].child(i));
		}
	}

	@Override
	protected void updateAtoms() {
		for (int i = 0, n = variables.nAtoms; i < n; i++) {
			double[][] info = variables.getAtomInfo(i);
			boolean isEnabled = !showPrimitiveAtoms || variables.isPrimitive(i);
			Geometry a = atomObjects.child(i);
			a.setEnabled(isEnabled);
			renderScaledAtom(a, info[DIS], info[OCC][0] * variables.atomMaxRadius);
			renderArrow(a.child(MAG - 2), info[MAG], MOMENT_FACTOR, variables.angstromsPerMagneton);
			renderArrow(a.child(ROT - 2), info[ROT], ROTATION_FACTOR, variables.angstromsPerRadian);
			renderEllipsoid(a.child(ELL - 2), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
		}
	}

	@Override
	protected void updateBonds() {
		for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
			transformCylinder(BOND_RADIUS, bondInfo[b], bondObjects.child(b));
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
		if (!isAnimationRunning || rp3 == null)
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
	protected boolean prepareToSwapIn() {
		double[] cameraMatrix = frame.cameraMatrix;
		if (cameraMatrix != null) {
			rp.setCameraMatrixAndZoom(cameraMatrix, frame.zoom);
		}
		return true;
	}

	@Override
	protected boolean prepareToSwapOut() {
		isAnimateSelected = false;
		if (rp != null) {
			// could be blank window
			rp.setSpinning(false); 
			frame.cameraMatrix = rp.getCameraMatrix();
			frame.zoom = rp.getZoom();
		}
		return true;
	}

	public static void main(String[] args) {
		create("IsoDistort", args);
	}

	@Override
	protected void takeFocus() {
		rp3.requestFocusInWindow();
	}

}
