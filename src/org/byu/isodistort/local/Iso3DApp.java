package org.byu.isodistort.local;

import java.awt.Graphics;
import java.awt.image.BufferedImage;

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

	protected static final int PARENT = 0;
	protected static final int CHILD = 1;
	
	protected static final double DEFAULT_DISTANCE = 0.4;


	public interface IsoRenderPanel {

		public static final int ROTATE_NORMAL = 0;
		public static final int ROTATE_X      = 1;
		public static final int ROTATE_Y      = 2;
		public static final int ROTATE_Z      = 3;
		public static final int ROTATE_ZOOM   = 4;

		void centerImage();
		void clearAngles();
		BufferedImage getImage();
		double[][] getPerspective();
		void initializeSettings(double radius);
		boolean isSpinning();
		void resetView();
		void reversePanningAction();
		void setCamera(double theta, double phi, double sigma);
		void setPerspective(double[][] params);
//		void setPreferredSize(Dimension size);
//		void setSize(Dimension size);
		void setSpinning(boolean spin);
		void updateForDisplay(boolean b);
	    void paint(Graphics g);
		double[] getCameraMatrix();
		void setCameraMatrixAndZoom(double[] cameraMatrix, double zoom);
		double getZoom();
	
	}

	protected IsoRenderPanel  rp;	

	/**
	 * Check boxes for zoom, spin, anim toggles
	 * 
	 */
	protected JCheckBox aBox, apBox, bBox, cpBox, ccBox, spinBox, animBox, axesBox;

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
	
	/////////// global variables /////////
	
	/**
	 * [0] parent; [1] child
	 */
	final protected static double[] AXIS_RADII = 
			new double[] { DEFAULT_DISTANCE * 0.35, DEFAULT_DISTANCE * 0.25 };
	
	/**
	 * various arrow/line radia
	 */
	final protected static double BOND_RADIUS = DEFAULT_DISTANCE * 0.1;
	final protected static double CELL_RADIUS = DEFAULT_DISTANCE * 0.25;
	final protected static double MOMENT_MULTIPLIER = 0.4;
	final protected static double ROTATION_MULTIPLIER = 0.35;

	/**
	 * flag to indicate animation is in progress
	 */
	protected boolean isAnimationRunning;

	protected double animPhase = Math.PI / 2;
	protected double animAmp = 1;

	/**
	 * Initial show settings
	 */
	protected boolean showBonds0 = true, showAtoms0 = true, showPrimitiveAtoms0 = false,
			showParentCell0 = true, showChildCell0 = true, 
			showAxes0 = false;
	/**
	 * CheckBox propertiers
	 */
	protected boolean showBonds, showAtoms, showPrimitiveAtoms,
		showParentCell, showChildCell, showAxes;
	/**
	 * true if the animate checkbox is checked
	 */
	protected boolean isAnimateSelected;
	

	/**
	 * Which type of view direction: childHKL, childUVW, parentHKL, parentUVW
	 */
	protected int viewType;

	/**
	 * A class to allow common methods to any 3D app.
	 * 
	 * @param appType
	 */
	protected Iso3DApp(int appType) {
		super(appType);
	}

	///////////////////// one-time initialization //////////////
	
	protected double initFieldOfView() {

		// Calculate the maximum distance from applet center (used to determine FOV).
		double d2 = variables.parentCell.addRange2(variables.childCell.addRange2(0.0));
		
		for (int i = 0, n = variables.nAtoms; i < n; i++) {
			d2 = MathUtil.maxlen2(variables.atoms[i].getCartesianCoord(), d2);
		}

		// the 4 here takes care of axes
		double radius = Math.sqrt(d2) + 4 * variables.atomMaxRadius;
		// this includes the width of atoms that might be at the extremes of the
		// longest cell diagonal.

		return radius;
	}

	protected void initGUI(Iso3DApp app) {
		childHKL.setSelected(app.childHKL.isSelected());
		childUVW.setSelected(app.childUVW.isSelected());
		parentHKL.setSelected(app.parentHKL.isSelected());
		parentUVW.setSelected(app.parentUVW.isSelected());
		uView.setText(app.uView.getText());
		vView.setText(app.vView.getText());
		wView.setText(app.wView.getText());
		aBox.setSelected(app.aBox.isSelected());
		apBox.setSelected(app.apBox.isSelected());
		bBox.setSelected(app.bBox.isSelected());
		cpBox.setSelected(app.cpBox.isSelected());
		ccBox.setSelected(app.ccBox.isSelected());
		axesBox.setSelected(app.axesBox.isSelected());
		spinBox.setSelected(app.spinBox.isSelected());
		animBox.setSelected(app.animBox.isSelected());
		colorBox.setSelected(app.colorBox.isSelected());
		frame.from3DApp = this;
	}

	///////////// updating /////////////
	
	protected boolean isAtomColorChanged;

	@Override
	public void updateDisplay() {		
		if (isAdjusting)
			return;
		updateSelectedObjects();
		isAdjusting = true;
		if (variables.isChanged) {
			needsRecalc = true;
			variables.isChanged = false;
		}
		if (rp == null)
			return;
		rp.updateForDisplay(false);		
		updateAtomColors();
		if (needsRecalc) {
			// virtually all the time is here:
			updateAtomsBondsCells();
			if (showAtoms) {
				updateAtoms();
			}
			if (showBonds) {
				updateBonds();
			}
			if (showParentCell || showChildCell) {
				updateCells();
			}
			if (showAxes) {
				updateAxes();
			}
			needsRecalc = false;
		}
		isAdjusting = false;
		rp.updateForDisplay(true);
		super.updateDisplay();
	}


	@Override
	protected void frameResized() {
		if (variables == null)
			return;
		super.frameResized();
		needsRecalc = true;
		updateDisplay();
	}

	abstract protected void updateBonding();
	abstract protected void updateSelectedObjects();
	abstract protected void updateAtomColors();
	abstract protected void updateAtoms();
	abstract protected void updateBonds();
	abstract protected void updateCells();
	abstract protected void updateAxes();

	
	
	/**
	 * update for atoms, bonds, and cells
	 */
	protected void updateAtomsBondsCells() {
		variables.readSliders();
		variables.recalcDistortion();
		if (showAtoms || showBonds) {
			variables.setAtomInfo();
			if (showAtoms && isAtomColorChanged) {
				updateAtomColors();
				isAtomColorChanged = false;
			}
		}
		if (showBonds) {
			updateBonding();
		}
		
		for (int axis = 0; axis < 3; axis++) {
			variables.setAxisExtents(axis, variables.parentCell, 2.0 * DEFAULT_DISTANCE, 3.5 * DEFAULT_DISTANCE);
			variables.setAxisExtents(axis, variables.childCell, 1.5 * DEFAULT_DISTANCE, 4.0 * DEFAULT_DISTANCE);
		}
		if (showParentCell || showChildCell) {
			variables.setCellInfo();
		}
	}
	
}
