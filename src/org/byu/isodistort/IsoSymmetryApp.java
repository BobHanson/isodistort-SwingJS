/**
 *  David Tanner
 *  April 2005
 * 
 * 
 */

package org.byu.isodistort;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.BitSet;

import javax.swing.JPanel;

import org.byu.isodistort.local.Iso3DApp;
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.Variables.IsoAtom;
import org.byu.isodistort.render.Geometry;
import org.byu.isodistort.render.Material;
import org.byu.isodistort.render.RenderPanel3D;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.modelset.Atom;
import org.jmol.viewer.Viewer;

import javajs.util.BS;

/**
 * 
 * The IsoSymmetry class is purely experimental. I could be used to display
 * symmetry elements using a Jmol application display panel. It is set up for
 * that, but this app is only visible if boolean IsoApp.addJmol is set to
 * true.
 * 
 * This class extends IsoDistortApp for now but probably does not need to be. 
 * 
 * 
 * @author Bob Hanson(mostly just refactoring)
 *
 */
public class IsoSymmetryApp extends IsoDistortApp {

	Viewer viewer;
	private JmolPanel rpJmol;
	private Geometry world;

	class JmolPanel extends JPanel implements IsoRenderPanel {

		private final Dimension currentSize = new Dimension();

		JmolPanel() {
			viewer = (Viewer) JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), null, null, null, null, null);
		}

		@Override
		public void paint(Graphics g) {
			getSize(currentSize);
			viewer.renderScreenImage(g, currentSize.width, currentSize.height);
		}

		@Override
		public void updateForDisplay(boolean b) {
			// TODO Auto-generated method stub

		}

		@Override
		public boolean isSpinning() {
			return false;
		}

		@Override
		public void clearAngles() {
			// TODO Auto-generated method stub

		}

		@Override
		public void setCamera(double tY, double tX) {
			// TODO Auto-generated method stub

		}

		@Override
		public void reversePanningAction() {
			// TODO Auto-generated method stub

		}

		@Override
		public void resetView() {
			// TODO Auto-generated method stub

		}

		@Override
		public void clearOffsets() {
			// TODO Auto-generated method stub

		}

		@Override
		public void centerImage() {
			// TODO Auto-generated method stub

		}

		@Override
		public BufferedImage getImage() {
			return (BufferedImage) viewer.getScreenImage();
		}

		@Override
		public void setPerspective(double[][] params) {
			// TODO Auto-generated method stub

		}

		@Override
		public double[][] getPerspective() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setSpinning(boolean spin) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void initializeSettings(double radius) {
		}

	}

	public IsoSymmetryApp() {
		super();
		this.appType = APP_ISOSYMMETRY;
	}

	@Override
	protected void init() {
		setRenderPanel(irp = rpJmol = new JmolPanel());
//	    JPanel panel2 = new JPanel();
//	    AppConsole console = new AppConsole(rp.viewer, panel2,
//	        "History State Clear");
//	    rp.viewer.setJmolCallbackListener(console);
		rpJmol.setPreferredSize(drawPanel.getSize());
		rpJmol.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add((JPanel) rpJmol, BorderLayout.CENTER);		
		rpJmol.addKeyListener(this);		
		createJmolModel();
		world = new Geometry();
		super.initWorld(world);	
	}

	private void createJmolModel() {
		StringBuffer xyzFile = new StringBuffer();
		int nAtoms = variables.nAtoms;
		xyzFile.append(nAtoms).append('\n');
		for (int i = 0; i < nAtoms; i++) {
			IsoAtom a = variables.getAtom(i);
			xyzFile.append(a.getAtomTypeSymbol()).append(" ");
			
		}
		// TODO Auto-generated method stub
		
	}

	@Override
	public void centerImage() {
		viewer.script("center {*}");
	}

	@Override
	public void reset() {
		super.reset();
//		viewer.script("restore orientation");
	}

	@Override
	protected boolean prepareToSwapOut() {
		super.prepareToSwapOut();
//		viewer.script("animation off;vibration off;spin off");
		return true;
	}

	@Override
	protected void handleButtonEvent(Object src) {
		super.handleButtonEvent(src);
	}

	@Override
	protected void applyView() {
		super.applyView();
	}


	@Override
	public void recalcCellColors() {
		super.recalcCellColors();
		// apply colors
	}

	@Override
	protected void checkBonding() {
		super.checkBonding();
	}

	double bondRadius = 0.15;
	
	@Override
	protected void enableSelectedObjects() {
		apBox.setEnabled(variables.isPrimitive(-1));
		String script = "";
		script += "display " + (showAtoms ? "all;" : "none;");
		script += "draw cell0* " + (showParentCell ? "on;" : "off;");
		script += "draw cell1* " + (showChildCell ? "on;" : "off;");
		script += "wireframe " + (showBonds ? 0 : bondRadius) + ";";
		script += "draw axes0* " + (showAxes && (showParentCell || !showChildCell) ? "on;" : "off;");
		script += "draw axes1* " + (showAxes && (showChildCell || !showParentCell) ? "on;" : "off;");
		runScript(script);
	}

	private void runScript(String script) {
		System.out.println ("SYMAPP runScript " + script);
		viewer.scriptWait(script);
	}

	@Override
	protected void recalcAtomColors() {
		super.recalcAtomColors();
		double[] spec = new double[4];
		double[] amb = new double[3];
		String script = "";
		for (int t = variables.nTypes; --t >= 0;) {
			for (int s = variables.nSubTypes[t]; --s >= 0;) {
				subMaterial[t][s].getSpecular(spec);
				subMaterial[t][s].getAmbient(amb);
				BS bsAtoms = newBS(variables.getAtomsFromTS(t, s));
				int c = getJmolColor(spec, amb);
				script += "color " + bsAtoms + " " + c + ";";
			}
		}
		viewer.scriptWait(script);
	}

	private BS newBS(BitSet a) {
		BS bs = new BS();
		for (int i = a.nextSetBit(0); i >= 0; i = a.nextSetBit(i + 1)) {
			bs.set(i);
		}
		return bs;
	}

	private int getJmolColor(double[] spec, double[] amb) {
		int r = (int) Math.max(0, Math.min(255, 255 * amb[0]));
		int g = (int) Math.max(0, Math.min(255, 255 * amb[1]));
		int b = (int) Math.max(0, Math.min(255, 255 * amb[2]));
		return (0xFF << 24) | (b << 16) | (g << 8) | r;
	}

	@Override
	protected void renderCells() {
		double r = variables.atomMaxRadius * cellMultiplier;
//		for (int i = 0; i < 12; i++) {
//			transformCylinder(r, variables.parentCell.getCellInfo(i), cellObjects[0].child(i));
//		}
//		for (int i = 0; i < 12; i++) {
//			transformCylinder(r, variables.childCell.getCellInfo(i), cellObjects[1].child(i));
//		}
	}

	@Override
	protected void renderAxes() {
		double rParent = variables.atomMaxRadius * axesMultipliers[0];
		double rChild = variables.atomMaxRadius * axesMultipliers[1];
//		for (int i = 0; i < 3; i++) {
//			transformCylinder(rParent, variables.parentCell.getAxisInfo(i), axisObjects[0].child(i));
//			transformCylinder(rChild, variables.childCell.getAxisInfo(i), axisObjects[1].child(i));
//		}
	}

	@Override
	protected void renderAtoms() {
		for (int i = 0, n = variables.nAtoms; i < n; i++) {
			double[][] info = variables.getAtomInfo(i);
			boolean isEnabled = !showPrimitiveAtoms || variables.isPrimitive(i);
//			Geometry a = atomObjects.child(i);
//			a.setEnabled(isEnabled);
//			renderScaledAtom(a, info[DIS], info[OCC][0] * variables.atomMaxRadius);
//			renderArrow(a.child(MAG - 2), info[MAG], momentMultiplier, variables.angstromsPerMagneton);
//			renderArrow(a.child(ROT - 2), info[ROT], rotationMultiplier, variables.angstromsPerRadian);
//			renderEllipsoid(a.child(ELL - 2), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
		}
	}

	@Override
	protected void renderBonds() {
//		double r = Math.max(variables.atomMaxRadius * bondMultiplier, 0.05);
//		for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
//			transformCylinder(r, bondInfo[b], bondObjects.child(b));
//		}
	}

	private void renderArrow(Geometry child, double[] info, double r, double scale) {
		boolean isOK = (Math.abs(info[2]) > 0.1);
//		child.setEnabled(isOK);
//		if (!isOK)
//			return;
	}

	@Override
	protected void updateViewOptions() {
		super.updateViewOptions();
	}

	@Override
	public void stop() {
		super.stop();

	}
}
