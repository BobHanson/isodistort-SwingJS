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

import javax.swing.JPanel;

import org.byu.isodistort.local.Iso3DApp;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolViewer;
import org.jmol.viewer.Viewer;

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
public class IsoSymmetryApp extends Iso3DApp {

	Viewer viewer;
	private JmolPanel rpJmol;

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
		public void initializeSettings(double radius) {
			// TODO Auto-generated method stub

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

	}

	public IsoSymmetryApp() {
		super(APP_ISOSYMMETRY);
	}

	@Override
	protected void init() {
		setRenderPanel(rpJmol = new JmolPanel());
//	    JPanel panel2 = new JPanel();
//	    AppConsole console = new AppConsole(rp.viewer, panel2,
//	        "History State Clear");
//	    rp.viewer.setJmolCallbackListener(console);
		rpJmol.setPreferredSize(drawPanel.getSize());
		rpJmol.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add((JPanel) rpJmol, BorderLayout.CENTER);
	}

	@Override
	public void centerImage() {
		viewer.script("center {*}");
	}

	@Override
	protected BufferedImage getImage() {
		return rpJmol.getImage();
	}

	@Override
	public void reset() {
		viewer.script("restore orientation");
	}

	@Override
	protected boolean prepareToSwapOut() {
		viewer.script("animation off;vibration off;spin off");
		return true;
	}

	@Override
	protected void handleButtonEvent(Object src) {
		// n/a
	}

	@Override
	protected void applyView() {
		// n/a
	}


	@Override
	public void recalcCellColors() {
		// TODO Auto-generated method stub
	}

	@Override
	protected void checkBonding() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void enableSelectedObjects() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void recalcAtomColors() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderAtoms() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderBonds() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderCells() {
		// TODO Auto-generated method stub
		
	}

	@Override
	protected void renderAxes() {
		// TODO Auto-generated method stub
		
	}

}
