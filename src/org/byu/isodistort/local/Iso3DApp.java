package org.byu.isodistort.local;

import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import javax.swing.JTextField;

/**
 * Iso3DApp subclasses IsoApp for a 3D application that has atoms and 
 * bonds, arrows, cells, and axes. 
 *  
 * @author hanso
 *
 */
public abstract class Iso3DApp extends IsoApp {

	

	/**
	 * Decimal multiplier to make bond radius and cell radius fractions of atom
	 * radius
	 * 
	 */
	protected double bondMultiplier = 0.1, axesMultiplier1 = 0.2, axesMultiplier2 = 0.3;
	protected double cellMultiplier = 0.25, momentMultiplier = 0.4, rotationMultiplier = 0.35;

	/**
	 * flag to indicate animation is in progress
	 */
	protected boolean isAnimationRunning;

	/**
	 * true if the animate checkbox is checked
	 */
	protected boolean isAnimateSelected;

	protected double animPhase = Math.PI / 2;
	protected double animAmp = 1;

	/**
	 * no shading in slider panel
	 */
	protected boolean isSimpleColor;

	/**
	 * Initially show bonds or not
	 */
	protected boolean showBonds0 = true, showAtoms0 = true, showCells0 = true, showAxes0 = false;
	/**
	 * Currently show bonds or not
	 */
	protected boolean showBonds, showAtoms, showCells, showAxes;
	/**
	 * Which type of view direction: childHKL, childUVW, parentHKL, parentUVW
	 */
	protected int viewType;

	/**
	 * Check boxes for zoom, spin, anim toggles
	 * 
	 */
	protected JCheckBox aBox, bBox, cBox, spinBox, colorBox, animBox, axesBox;

	/**
	 * Buttons to use child or parent cell for view vectors
	 * 
	 */
	protected JRadioButton childHKL, childUVW, parentHKL, parentUVW;
	/**
	 * Text fields for inputting viewing angles
	 * 
	 */
	protected JTextField uView, vView, wView;

	private IsoRenderPanel  rp;
	
	protected void setRenderPanel(IsoRenderPanel rp) {
		this.rp = rp;
	}

	/**
	 * A class to allow common methods to any 3D app.
	 * 
	 * @param appType
	 */
	protected Iso3DApp(int appType) {
		super(appType);
	}

	@Override
	protected void frameResized() {
		super.frameResized();
		needsRecalc = true;
	}

	/**
	 * Done once, only during initialization.
	 */
	protected double initFieldOfView() {
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

		return radius;
	}

	/**
	 * recalculates structural distortions and bond configurations.
	 */
	protected void recalcABC() {
		variables.readSliders();
		variables.recalcDistortion();
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

	abstract protected void checkBonding();

	abstract protected void enableSelectedObjects();
	
	protected boolean isMaterialTainted;
	
	/**
	 * recalculates the atom colors after a checkbox has been set.
	 */
	protected void recalcMaterials() {
		variables.setColors(isSimpleColor);
		variables.recolorPanels();
		if (showAtoms)
			recalcAtomColors();
		isMaterialTainted = false;
	}

	abstract protected void recalcAtomColors();
	abstract protected void renderAtoms();
	abstract protected void renderBonds();
	abstract protected void renderCells();
	abstract protected void renderAxes();

	
	@Override
	public void updateDisplay() {		
		if (isAdjusting)
			return;
		enableSelectedObjects();
		isAdjusting = true;
		if (variables.isChanged) {
			needsRecalc = true;
			variables.isChanged = false;
		}
		if (rp == null)
			return;
		rp.updateForDisplay(false);		
		if (isMaterialTainted) {
			recalcMaterials();
		}
		if (needsRecalc) {
			// virtually all the time is here:
			recalcABC();
			if (showAtoms) {
				renderAtoms();
			}
			if (showBonds) {
				renderBonds();
			}
			if (showCells) {
				renderCells();
			}
			if (showAxes) {
				renderAxes();
			}
			needsRecalc = false;
		}
		isAdjusting = false;
		rp.updateForDisplay(true);
	}


}
