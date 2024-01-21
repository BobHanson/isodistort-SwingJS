/**
	@Override
	protected void frameResized() {
		if (rp == null)
			return;
		rp.im = null;
		drawPanel.setBackground(Color.red);
		needsRecalc = true;
		updateDisplay();
	}

 * Bob Hanson 2023.12.08
 * 
 * Reconfigured java.applet.Applet as Swing JPanel
 * allowing for simpler creation of external (draggable) JFrame.
 * 
 * Updated rendering to not use thread sleep and instead
 * use javax.swing.Timer so as to be compatible with JavaScript.
 * 
 * Refactored to have the simple hierarchy:
 * 
 *   IsoDistortApp > IsoPanel
 *  
 *  
 *  Branton Campbell, David Tanner Andrew Zimmerman, 
 *  June 2006
 * 
 * This applet takes the data from the IsoDistort web page 
 * and uses it to render atoms and bonds and cells that represent various atomic crystal structures.
 * 
 
*/

//	http://stokes.byu.edu/isodistort.html is isodistort website

//	import all the needed java classes for this program

/**
 11111111111111111111111111111111111111111111111111111111111111111111111111111111
 1																				1
 1 In the first section we import needed classes and instantiate the variables. 1
 1																				1
 11111111111111111111111111111111111111111111111111111111111111111111111111111111
 
*/

package org.byu.isodistort;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.byu.isodistort.local.Elements;
import org.byu.isodistort.local.IsoApp;
import org.byu.isodistort.local.MathUtil;
import org.byu.isodistort.local.Variables.Atom;

import javajs.util.PT;

public class IsoDiffractApp extends IsoApp implements KeyListener {

	/**
	 * An alternative panel to the RenderPanel3D of IsoDistortApp
	 * 
	 * @author Bob Hanson
	 *
	 * 
	 */
	private static class RenderPanel extends JPanel {

		private IsoDiffractApp app;

		private MouseAdapter adapter;
		
		RenderPanel(IsoDiffractApp app) {
			this.app = app;
			adapter = new Adapter();
			addMouseMotionListener(adapter);
			addMouseListener(adapter);
		}

		private BufferedImage im;

		@Override
		public void paint(Graphics gr) {
			super.paint(gr);
			Dimension d = getSize();
			if (app.needsRecalc || app.drawWidth != d.width || app.drawHeight != d.height) {
				app.updateDimensions();
				app.needsRecalc = true;
				app.updateDisplay();
				return;
			}
			if (im == null || im.getWidth() != d.width || im.getHeight() != d.height)
				im = (BufferedImage) createImage(d.width, d.height);
			Graphics g = im.getGraphics();
			app.render(g);
			g.dispose();
			gr.drawImage(im, 0, 0, d.width, d.height, this);
		}

		public void dispose() {
			removeMouseListener(adapter);
			removeMouseMotionListener(adapter);
		}

		private int mouseX, mouseY;
		
		private class Adapter extends MouseAdapter {

			@Override
			public void mouseMoved(MouseEvent e) {
				app.mousePeak(e.getX(), e.getY());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int mx = e.getX();
				int my = e.getY();
				app.mouseDrag(mx - mouseX, my - mouseY);
				mouseX = mx;
				mouseY = my;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				mouseX = e.getX(); mouseY = e.getY();
				app.setStatusVisible(false);
			}

		}

	}

	private static final Color[] colorMap = new Color[] { Color.PINK, Color.GREEN, Color.RED, Color.BLUE,
			Color.ORANGE };

	// Variables that the user may want to adjust
	/**
	 * Maximum number of diffraction peaks and tick marks in the display
	 */
	private int maxPeaks = 1000, powderMaxVisiblePeaks = 200, maxTicks = 100;
	/**
	 * Axis & tick line thickness, tick length
	 */
	private double lineThickness = 2, normalTickLength = 10;
	/**
	 * Heights of powder display components
	 */
	private final double powderAxisYOffset = 30, powderStickYOffset = 60, powderScaleAreaHeight = 80,
			powderLabelOffset = 5;

	private final double shortPowderAxisYOffset = 22, shortPowderStickYOffset = 22, shortPowderScaleAreaHeight = 30,
			shortTickLength = 5;

	private int shortPowderHeight = 100;

	/**
	 * Factor that determines the lowest observable intensities
	 */
	double logfac = 4.0;
	/**
	 * Number of points in the powder pattern
	 */
	int powderXRange = 1024;
	/**
	 * Anisotropic thermal parameter for simulating high-q intensity decay
	 */
	double uiso = 0.15;

	private final static int POWDER_PATTERN_TYPE_2THETA = 1;
	private final static int POWDER_PATTERN_TYPE_DSPACE = 2;
	private final static int POWDER_PATTERN_TYPE_Q = 3;

	private static final double MIN_PEAK_INTENSITY = 1e-25;

	// Other global variables.
	int hklType = 1;
	/**
	 * Horizontal axis choice for powder pattern: (1) 2theta, (2) d-space, (3) q
	 */
	int powderPatternType = POWDER_PATTERN_TYPE_2THETA;
	/**
	 * X-ray vs neutron diffraction
	 */
	public boolean isXray = true;
	/**
	 * contains x-ray and neutron scattering factors for each element
	 */
	/**
	 * half the display width in pixels
	 */
	double drawHalfWidth;
	double drawHalfHeight;
	/**
	 * True means to make all atoms of the same element the same color
	 */
	boolean isSimpleColor;

	boolean isPowder;
	boolean isBoth;
	boolean wasPowder; // for entry to "both"

	boolean isMouseOver;

	/**
	 * number of childHKL peaks contained within the display
	 */
	int peakCount;
	/**
	 * List of peak multiplicities for powder pattern
	 */
	double[] peakMultiplicity = new double[maxPeaks];
	/**
	 * List of dinverse values for peaks in either single-crystal or powder display
	 * 
	 */
	double[] peakDInv = new double[maxPeaks];
	/**
	 * List of types of peaks contained within the display
	 */
	int[] peakColor = new int[maxPeaks];

	/**
	 * unstrained dinverse-space metric tensor
	 */
	double[][] metric0 = new double[3][3];
	/**
	 * strained dinverse-space metric tensor
	 */
	double[][] metric = new double[3][3];
	/**
	 * transforms childHKL to properly-rotated XYZ cartesian
	 */
	double[][] slatt2rotcart = new double[3][3];
	/**
	 * transforms properly-rotated XYZ cartesian to childHKL
	 */
	double[][] rotcart2slatt = new double[3][3];

	/**
	 * distance (in pixels) to the nearest peak from the center
	 */
	double crystalNearestDistanceToOrigin;
	/**
	 * maximum peak radius
	 */
	double crystalMaxPeakRadius;
	/**
	 * The hkl horizonal direction, upper direction, and center,
	 */
	double[] crystalHklCenter = new double[3];
	/**
	 * number of single-crystal tickmarks to be displayed
	 */
	int[] crystalTickCount = new int[2];
	/**
	 * List of tickmark supHKLs to be displayed
	 */
	double[][][] crystalTickHKL = new double[2][maxTicks][3];
	/**
	 * List of tickmark (X1Y1, X2Y2) coords to be displayed
	 */
	double[][][] crystalTickXY2 = new double[2][maxTicks][4];
	/**
	 * List of childHKLs contained within the display
	 */
	double[][] crystalPeakHKL = new double[maxPeaks][3];
	/**
	 * List of peak XY coords contained within the single-crystal display
	 */
	double[][] crystalPeakXY = new double[maxPeaks][2];
	/**
	 * List of peak radii contained within the single-crystal display
	 */
	double[] crystalPeakRadius = new double[maxPeaks];
	/**
	 * Plot axes for crystal display
	 */
	double[][] crystalAxesXYDirXYLen = new double[2][5];
	/**
	 * radius of the range of Q/2Pi contained within the crystal display
	 */
	double crystalDInvRange;
	/**
	 * The hkl horizonal and upper directions,
	 */
	double[][] crystalHkldirections = new double[2][3];

	/**
	 * List of positions of powder peaks in either 2th, d or q units
	 */
	double[] powderPeakY = new double[maxPeaks];
	/**
	 * List of positions of powder peaks in scaled pixel units
	 */
	double[] powderPeakX = new double[maxPeaks];
	/**
	 * List of peak radii contained within the single-crystal display
	 */
	double[] peakIntensity = new double[maxPeaks];
	/**
	 * Largest powder peak multiplicity
	 */
	double powderScaleFactor; // initial value of 1
	/**
	 * Number of powder tick marks and tick labels to be displayed
	 */
	int powderAxisTickCount;
	/**
	 * List of powder tick marks to be displayed in window pixel units
	 */
	double[] powderAxisTickX = new double[maxTicks];
	/**
	 * List of powder tick labels to be displayed in window pixel units
	 */
	String[] powderAxisTickLabel = new String[maxTicks];
	/**
	 * wavelength for computing 2theta scale in the powder display
	 */
	double powderWavelength;
	/**
	 * horizontal range minimum value in powder display
	 */
	double powderXMin;
	/**
	 * horizontal range maximum value in powder display
	 */
	double powderXMax;
	/**
	 * horizontal resolution in the powder display
	 */
	double powderResolution;
	/**
	 * vertical zoom factor in the powder display
	 */
	double powderZoom;
	/**
	 * dinvmin and dinvmax are the maximum and minimum values in the powder display
	 * in d-inverse units
	 * 
	 */
	double powderDinvmin, powderDinvmax, powderDinvres;
	/**
	 * the array that holds the powder pattern
	 */
	double[] powderY = new double[powderXRange];

	JLabel horizLabel, upperLabel, centerLabel, qrangeLabel, wavLabel, minLabel, maxLabel, fwhmLabel, zoomLabel,
			mouseoverLabel;

	/**
	 * Radio buttons
	 */
	JRadioButton parentButton, childButton, xrayButton, neutronButton, crystalButton, powderButton, bothButton, dButton,
			qButton, tButton;
	/**
	 * Text fields for inputting viewing region
	 */
	JTextField hHTxt, kHTxt, lHTxt, hVTxt, kVTxt, lVTxt, hOTxt, kOTxt, lOTxt, qTxt;
	JTextField wavTxt, minTxt, maxTxt, fwhmTxt, zoomTxt;

	private RenderPanel rp;

	/**
	 * BH EXPERIMENTAL allows selective removal of atoms from powder pattern
	 */
	private boolean allowSubtypeSelection = true;

	public IsoDiffractApp() {
		super(APP_ISODIFFRACT);
	}

	@Override
	protected void dispose() {
		rp.removeKeyListener(this);
		rp.dispose();
		super.dispose();
	}

	@Override
	protected void init() {
		buildControls();
		showControls();
		rp = new RenderPanel(this);
		rp.addKeyListener(this);
		drawPanel.add(rp);
	}

	@Override
	protected void frameResized() {
		super.frameResized();
		needsRecalc = true;
		if (rp == null)
			return;
		rp.im = null;
	}

	@Override
	protected void updateDimensions() {
		super.updateDimensions();
		drawHalfWidth = drawWidth / 2;
		drawHalfHeight = drawHeight / 2;

	}

	@Override
	public synchronized void updateDisplay() {
		if (isAdjusting || drawHeight < 20)
			return;
		if (needsRecalc || variables.isChanged) {
			isAdjusting = true;
			variables.readSliders();
			variables.enableSubtypeSelection(allowSubtypeSelection  && (isBoth || isPowder));
			if (isBoth || isPowder)
				resetPowderPeaks();
			if (isBoth || !isPowder)
				resetCrystalPeaks();
			needsRecalc = false;
			variables.isChanged = false;
			rp.repaint();
			rp.requestFocus();
			isAdjusting = false;
		}

	}

	/**
	 * From RenderPanel.paint(Graphics).
	 * 
	 * @param g
	 * 
	 */
	void render(Graphics g) {
		if (isAdjusting || drawHeight < 20) {
			needsRecalc = true;
			variables.isChanged = true;
			return;
		}
		g.setColor(Color.BLACK);
		// shortPowderHeight);
		if (isBoth || !isPowder) {
			g.fillRect(0, 0, drawWidth, drawHeight);
			drawCrystPeaks(g);
			drawCrystalAxes(g);
		}
		if (isBoth || isPowder) {
			g.setColor(Color.BLACK);
			int y0 = (isBoth ? drawHeight - shortPowderHeight : 0);
			g.fillRect(0, y0, drawWidth, drawHeight);
			drawPowderSticks(g);
			drawPowderPattern(g);
		}
	}

	void drawCrystPeaks(Graphics gr) {
		for (int p = 0; p < peakCount; p++) {
			if (peakIntensity[p] < MIN_PEAK_INTENSITY)
				continue;
			drawCrystalCircle(gr, crystalPeakXY[p][0], crystalPeakXY[p][1], crystalPeakRadius[p], crystalMaxPeakRadius,
					peakColor[p]);
		}
	}

	private void drawCrystalCircle(Graphics gr, double dx, double dy, double dradius, double max, int color) {
		boolean tooBig = false;
		int radius = (int) Math.rint(dradius);
		if (radius >= (int) Math.rint(max)) {
			radius = (int) Math.rint(max);
			tooBig = true;
		}
		int x = (int) dx;
		int y = (int) dy;
		gr.setColor(tooBig ? Color.yellow : Color.white);
		gr.fillOval(x - radius, y - radius, radius * 2, radius * 2);
		gr.setColor(colorMap[color]);
		gr.drawOval(x - 5, y - 5, 10, 10);
		gr.drawOval(x - 6, y - 6, 12, 12);
	}

	void drawCrystalAxes(Graphics gr) {
		gr.setColor(colorMap[0]);
		for (int i = 0; i < 2; i++) {
			drawDash(gr, crystalAxesXYDirXYLen[i][0], crystalAxesXYDirXYLen[i][1], crystalAxesXYDirXYLen[i][2],
					crystalAxesXYDirXYLen[i][3], crystalAxesXYDirXYLen[i][4], lineThickness, 0);
			for (int j = 0; j < crystalTickCount[i]; j++) {
				drawDash(gr, crystalTickXY2[i][j][0], crystalTickXY2[i][j][1], crystalTickXY2[i][j][2],
						crystalTickXY2[i][j][3], normalTickLength, lineThickness, 0);
			}
		}
	}

	void drawPowderPattern(Graphics gr) {
		// ........----------------.....
		// ................|......|.....
		// ................|......|.....
		// ................|......|.....
		// ................|......|height
		// ................|......|.....
		// ......-----------------|.....
		// ................|......|.....
		// scaleAreaHeight.|......|.....
		// ................|......|.....
		// ......-------------__---.....
		int x0, y0, x1, y1;
		gr.setColor(Color.white);
		double scaleAreaHeight = (isBoth ? shortPowderScaleAreaHeight : powderScaleAreaHeight);
		double fy = 0.8
				* (isBoth ? 1d * (shortPowderHeight - shortPowderScaleAreaHeight) / (drawHeight - powderScaleAreaHeight)
						: 1)
				* powderZoom / powderScaleFactor;
		double yrange = (drawHeight - scaleAreaHeight);
		int ymin = (isBoth ? drawHeight - shortPowderHeight : 0);
		double toXPix = 1.0 * drawWidth / powderXRange;
		x0 = 0;
		y0 = (int) Math.max(ymin, yrange * (1 - powderY[0] * fy));
		for (int i = 1; i < powderXRange; i++) {
			x1 = (int) (i * toXPix);
			y1 = (int) Math.max(ymin, yrange * (1 - powderY[i] * fy));
			gr.drawLine(x0, y0, x1, y1);
			x0 = x1;
			y0 = y1;
		}
	}
	

	void drawPowderSticks(Graphics gr) {
		double axisYOffset = (isBoth ? shortPowderAxisYOffset : powderAxisYOffset);
		double stickYOffset = (isBoth ? shortPowderStickYOffset : powderStickYOffset);
		double tickLength = (isBoth ? shortTickLength : normalTickLength);

		// BH Q: Why the duplicate ? Maybe older screens had trouble?

//		for (int J = 0; J < (Math.random() > 0.5 ? 1 : 2); J++) {
		double y = drawHeight - stickYOffset;
		gr.setColor(colorMap[0]);
		drawDash(gr, drawHalfWidth, drawHeight - axisYOffset, 1, 0, drawHalfWidth, lineThickness, 0);
		for (int c = 4; c > 0; c--) {
			// draw one color at at time with
			// red and green last and on top
			for (int p = 0; p < peakCount; p++) {
				if (peakColor[p] != c || peakIntensity[p] < MIN_PEAK_INTENSITY)
					continue;
				drawDash(gr, powderPeakX[p], y, 0, 1, tickLength, lineThickness, c);
			}
		}
		gr.setColor(colorMap[0]);
		int ytick = (int) (drawHeight - powderLabelOffset);
		for (int n = 0; n < powderAxisTickCount; n++) {
			drawDash(gr, powderAxisTickX[n], drawHeight - axisYOffset, 0, 1, tickLength, lineThickness, 0);
			gr.drawString(powderAxisTickLabel[n], (int) powderAxisTickX[n] - 15, ytick);
		}
		// }
	}

	private static void drawDash(Graphics gr, double cx, double cy, double dirx, double diry, double halflength,
			double thickness, int color) {
		if (color != 0)
			gr.setColor(colorMap[color]);
		double deltx = halflength * dirx;
		double delty = halflength * diry;
		int x1 = (int) (cx - deltx);
		int y1 = (int) (cy - delty);
		int x2 = (int) (cx + deltx);
		int y2 = (int) (cy + delty);
		boolean vertical = Math.abs(deltx) < Math.abs(delty);
		for (int count = 0, sign = 1, offset = 0; count < thickness; count++, sign *= -1, offset += count * sign)
			if (vertical)
				gr.drawLine(x1 + offset, y1, x2 + offset, y2);
			else
				gr.drawLine(x1, y1 + offset, x2, y2 + offset);
	}

	/**
	 * mousePeak determines which Bragg peak, if any, is under the mouse.
	 * 
	 * @param x      is the x-coordinate of the mouse in the Applet window
	 * @param y      is the y-coordinate of the mouse in the Applet window
	 * @return which peak is under the mouse
	 * 
	 */
	private void mousePeak(int x, int y) {

		double tol2 = (isPowder ? 2 * 2 : 6 * 6);
		int thisPeak = -1, currentcolor = 5;
		String mouseovertext, valuestring = "", specifictext = "";
		boolean isPowder = (this.isPowder || isBoth && y > drawHeight - shortPowderHeight);
		for (int p = 0; p < peakCount; p++) {
			if (peakIntensity[p] < MIN_PEAK_INTENSITY)
				continue;
			if (isPowder) {
				if (approxEqual(x, powderPeakX[p], tol2)
						&& (Math.abs(y - (drawHeight - powderStickYOffset)) < 1.25 * normalTickLength)
						&& (peakColor[p] < currentcolor)) {
					thisPeak = p;
					currentcolor = peakColor[p];
					valuestring = trim00(powderPeakY[p]);
					switch (powderPatternType) {
					case POWDER_PATTERN_TYPE_2THETA:
						specifictext = "2\u0398 = " + valuestring + " \u00b0 ";
						break;
					case POWDER_PATTERN_TYPE_DSPACE:
						specifictext = "d = " + valuestring + " \u212b ";
						break;
					case POWDER_PATTERN_TYPE_Q:
						specifictext = "q = " + valuestring + " \u212b\u207b\u00b9 ";
						break;
					}
				}
			} else {
				double dx = x - crystalPeakXY[p][0];
				double dy = y - crystalPeakXY[p][1];
				if (dx * dx + dy * dy <= tol2) {
					thisPeak = p;
					specifictext = "";
					break;
				}
			}
		}
		if (thisPeak >= 0) {
			double[] hklp = new double[3];
			double[] hkls = crystalPeakHKL[thisPeak];
			MathUtil.mat3mul(variables.Tmat, crystalPeakHKL[thisPeak], hklp);
			String intensity = toIntensityString(peakIntensity[thisPeak]);
			mouseovertext = "Parent HKL = (" + trim00(hklp[0]) + ", "
					+ trim00(hklp[1]) + ", " + trim00(hklp[2])
					+ ")       Child HKL = (" + trim00(hkls[0]) + ", "
					+ trim00(hkls[1]) + ", " + trim00(hkls[2]) + ")     "
					+ specifictext
					+ "   I = " + intensity
					+ (isPowder ? "   M = " + (int) peakMultiplicity[thisPeak] : "");
			isMouseOver = true;
		} else {
			mouseovertext = "";
			isMouseOver = false;
		}
		mouseoverLabel.setText(mouseovertext);
		showControls();
	}
	private String toIntensityString(double d) {
		if (d > 0.001) {
			return MathUtil.varToString(d, 3, 0);
		}
		return PT.formatD(d, 0, -3, false, false);
	}

	public void mouseDrag(int dx, int dy) {
		if (!isPowder && !isBoth)
			return;
		double zoom = getText(zoomTxt, powderZoom, 2);
		zoom += -dy / 100.0;
		if (zoom < 0)
			zoom = 0;
		zoomTxt.setText(trim00(zoom));
		needsRecalc = true;
		updateDisplay();
	}



	/*
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 * 2 These methods do the diffraction-related calculations 2
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 * 
	 */

	/**
	 * Recalculates strain info (e.g. metric tensor and slatt2rotcart). Called by
	 * resetCrystalPeaks, resetPowerPeaks, recalcCrystal, recalcPowder.
	 * 
	 */
	private void recalcStrainMetrics() {
		double[][] slatt2cart = new double[3][3]; // transforms childHKL to XYZ cartesian
		double[][] rotmat = new double[3][3];// Rotates cartesian q-space so as to place axes1 along +x and axes2 in +y
												// hemi-plane

		variables.childCell.setMetricTensor(false, slatt2cart, metric0);
		variables.childCell.setMetricTensor(true, slatt2cart, metric);
		
		// Create an orthonormal rotation matrix that moves axis 1 to the +x direction,
		// while keeping axis 2 in the +y quadrant. First transform both axes into
		// cartesian
		// coords. Then take 1.cross.2 to get axis 3, and 3.cross.1 to get the new axis
		// 2.
		// Normalize all three and place them in the rows of the transformation matrix.
		MathUtil.mat3mul(slatt2cart, crystalHkldirections[0], rotmat[0]);
		MathUtil.mat3mul(slatt2cart, crystalHkldirections[1], rotmat[1]);
		MathUtil.cross3(rotmat[0], rotmat[1], rotmat[2]);
		MathUtil.cross3(rotmat[2], rotmat[0], rotmat[1]);
		MathUtil.norm3(rotmat[0]);
		MathUtil.norm3(rotmat[1]);
		MathUtil.norm3(rotmat[2]);

		// Combine the rotation and the cartesian conversion to get the overall childHKL
		// to cartesian transformation.
		MathUtil.mat3product(rotmat, slatt2cart, slatt2rotcart, tempmat);

		// Invert to get the overall cartesian to childHKL tranformation.
		MathUtil.mat3inverse(slatt2rotcart, rotcart2slatt, tempvec, tempmat);

	}

	/**
	 * Called by recalcCrystal, recalcPowder, assignPeakTypes.
	 * 
	 */
	public void recalcIntensities() {
		double[] zzzM, pppM;
		if (isXray) {
			zzzM = pppM = null;
		} else {
			zzzM = new double[3]; 
			pppM = new double[3];
		}
		double[] qhat = new double[3], supxyz = new double[3];

		for (int p = 0; p < peakCount; p++) {
			double zzzNR = 0;
			double zzzNI = 0;
			double pppNR = 0;
			double pppNI = 0;
			if (!isXray) {
			for (int i = 0; i < 3; i++) {
				zzzM[i] = 0;
				pppM[i] = 0;
			}
			}
			MathUtil.set3(variables.childCell.toTempCartesian(crystalPeakHKL[p]), qhat);
			MathUtil.norm3(qhat);
			double d = 2 * Math.PI * peakDInv[p];
			double thermal = Math.exp(-0.5 * uiso * d * d);
			
			for (int ia = 0, n = variables.numAtoms; ia < n; ia++) {
				Atom a = variables.getAtom(ia);
				if (allowSubtypeSelection && !variables.isSubTypeSelected(a.type, a.subType))
					continue;
				MathUtil.set3(a.getFinalFractionalCoord(), supxyz);

				// BH 2023.01.15 This was causing large jumps in intensities.
				// I believe this is no longer necessary
				// now that we are using only primitives.
				// It should not matter if an atom moves out of the unit cell.
				// Correct?

				// if (supxyz[0] >= 0 && supxyz[0] < 1 && supxyz[1] >= 0 && supxyz[1] < 1 &&
				// supxyz[2] >= 0 && supxyz[2] < 1) {
				
				
				// just [atomicNumber, 0] for xray
				double[] atomScatFac = Elements.getScatteringFactor(a.getAtomTypeSymbol(), isXray);
				double phase = 2 * Math.PI * MathUtil.dot3(crystalPeakHKL[p], supxyz);
				double cos = Math.cos(phase);
				double sin = Math.sin(phase);

				double occ0 = a.getInitialOccupancy();
				double occ = a.getOccupancy();

				double scatNR = occ * atomScatFac[0];
				double scatNI = occ * atomScatFac[1];
				zzzNR += occ0 * atomScatFac[0];
				zzzNI += occ0 * atomScatFac[1];
				pppNR += scatNR * cos - scatNI * sin;
				pppNI += scatNR * sin + scatNI * cos;				
				if (!isXray) {
					// remember that magnetic mode vectors (magnetons/Angstrom) were predefined to
					// transform this way.
					// mucart is temporary only
					double[] mucart = variables.childCell.toTempCartesian(a.getMagneticMoment());
					double scatM = occ * 5.4;
					// zzz += scatM*mu
					MathUtil.scaleAdd3(zzzM, scatM, mucart, zzzM); 
					// t = mu-(mu.qhat)*qhat
					MathUtil.scaleAdd3(mucart, -MathUtil.dot3(mucart, qhat), qhat, tempvec); 
					// ppp += scatM*cos*t
					MathUtil.scaleAdd3(pppM, cos * scatM, tempvec, pppM);
				}
			}
			double f = (pppNR == 0 ? 0 
					: (pppNR * pppNR + pppNI * pppNI + (isXray ? 0 : MathUtil.lenSq3(pppM)))
						/ (zzzNR * zzzNR + zzzNI * zzzNI + (isXray ? 0 : MathUtil.lenSq3(zzzM))));			
			peakIntensity[p] = (f < MIN_PEAK_INTENSITY ? MIN_PEAK_INTENSITY / 2 : thermal * f);
		}
	}

	/**
	 * Recalculates peak positions and intensities for single-crystal pattern.
	 * Called by resetCrystalPeaks and run().
	 * 
	 */
	private void recalcCrystal() {
		double[] t03 = new double[3], t3 = new double[3];

		// update the structure based on current slider values
		variables.recalcDistortion();

		recalcStrainMetrics();

		// update the peak drawing coordinates
		for (int p = 0; p < peakCount; p++) {
			MathUtil.vecaddN(crystalPeakHKL[p], -1.0, crystalHklCenter, t03);
			MathUtil.mat3mul(slatt2rotcart, t03, t3);
			crystalPeakXY[p][0] = (1 + t3[0] / crystalDInvRange) * drawHalfWidth;
			crystalPeakXY[p][1] = (1 - t3[1] / crystalDInvRange) * drawHalfHeight; 
		}

		// Update the dinverse list
		for (int p = 0; p < peakCount; p++) {
			MathUtil.mat3mul(metric, crystalPeakHKL[p], t3);
			peakDInv[p] = Math.sqrt(MathUtil.dot3(crystalPeakHKL[p], t3));
		}

		// Update the axis and tickmark plot parameters
		for (int n = 0; n < 2; n++) // cycle over the two axes
		{
			MathUtil.mat3mul(slatt2rotcart, crystalHkldirections[n], t3);
			MathUtil.norm3(t3);
			double dirX = t3[0];
			double dirY = t3[1];
			crystalAxesXYDirXYLen[n][0] = drawHalfWidth;
			crystalAxesXYDirXYLen[n][1] = drawHalfHeight;
			crystalAxesXYDirXYLen[n][2] = dirX;
			crystalAxesXYDirXYLen[n][3] = -dirY; // minus sign turns the picture upside right.
			crystalAxesXYDirXYLen[n][4] = (Math.abs(dirX) <= 0.001 ? drawHalfWidth 
					: Math.abs(dirX) > Math.abs(dirY) ? drawHalfWidth / Math.abs(dirX) 
					: drawHalfHeight / Math.abs(dirY));

			for (int m = 0; m < crystalTickCount[n]; m++) {
				MathUtil.mat3mul(slatt2rotcart, crystalTickHKL[n][m], t3);
				
				crystalTickXY2[n][m][0] = (1 + t3[0] / crystalDInvRange) * drawHalfWidth;
				crystalTickXY2[n][m][1] = (1 - t3[1] / crystalDInvRange) * drawHalfHeight; 

				// z = tempvec[2] * (drawHalfWidth / crystalDInvRange); // BH I guess...
				crystalTickXY2[0][m][2] = crystalAxesXYDirXYLen[1][2];
				crystalTickXY2[0][m][3] = crystalAxesXYDirXYLen[1][3];
				crystalTickXY2[1][m][2] = crystalAxesXYDirXYLen[0][2];
				crystalTickXY2[1][m][3] = crystalAxesXYDirXYLen[0][3];
				// if (z == 0) {
				// } // No need for Z now, but might use later.
			}
		}

		// update the peak intensities based on current structure
		recalcIntensities();

		// update the logarithmic peak radii
		for (int p = 0; p < peakCount; p++)
			crystalPeakRadius[p] = crystalMaxPeakRadius
					* (Math.max(Math.log(peakIntensity[p]) / 2.3025, -logfac) + logfac) / logfac;

	}

	private double[][] tempmat = new double[3][3];
	private double[] tempvec0 = new double[3], tempvec = new double[3];

	double[] hklH = new double[3], hklV = new double[3], hklO = new double[3];

	/**
	 * Creates the list of single-crystal peaks and their types Called by init() and
	 * run().
	 * 
	 */
	public void resetCrystalPeaks() {
		double ztolerance = 0.001; // z-axis tolerance that determines whether or not a peak is in the display
									// plane

		int[][] hklrange = new int[3][2];

		double[][] platt2slatt = new double[3][3]; // transforms parentHKL to childHKL
		double[][] slatt2platt = new double[3][3]; // transforms childHKL to parentHKL
		MathUtil.mat3copy(variables.Tmat, slatt2platt);
		MathUtil.mat3inverse(slatt2platt, platt2slatt, tempvec, tempmat);

		hklO[0] = getText(hOTxt,hklO[0], 2);
		hklO[1] = getText(kOTxt,hklO[1], 2);
		hklO[2] = getText(lOTxt,hklO[2], 2);
		hklH[0] = getText(hHTxt,hklH[0], 2);
		hklH[1] = getText(kHTxt,hklH[1], 2);
		hklH[2] = getText(lHTxt,hklH[2], 2);
		hklV[0] = getText(hVTxt,hklV[0], 2);
		hklV[1] = getText(kVTxt,hklV[1], 2);
		hklV[2] = getText(lVTxt,hklV[2], 2);

		crystalDInvRange = getText(qTxt, crystalDInvRange * (2 * Math.PI), 2) / (2 * Math.PI);

		// Decide that user input is either of parentHKL or childHKL type
		// Either way, the input directions are passed as childHKL vectors.
		// Note that the platt2slatt matrix is slider-bar independent.
		if (hklType == 2) {
			MathUtil.copy3(hklH, crystalHkldirections[0]);
			MathUtil.copy3(hklV, crystalHkldirections[1]);
			MathUtil.copy3(hklO, crystalHklCenter);
		} else if (hklType == 1) {
			MathUtil.mat3mul(platt2slatt, hklH, crystalHkldirections[0]);
			MathUtil.mat3mul(platt2slatt, hklV, crystalHkldirections[1]);
			MathUtil.mat3mul(platt2slatt, hklO, crystalHklCenter);
		}

		// Identify the direct-space direction perpendicular to display plane.
		double[] uvw = new double[3];
		MathUtil.cross3(crystalHkldirections[0], crystalHkldirections[1], uvw);

		variables.readSliders(); // Get the latest strain information
		variables.recalcDistortion(); // Update the distortion
		recalcStrainMetrics(); // Get updat ed slatt2rotcart transformation

		double[][] limits = new double[8][3]; // HKL search limits
		// Find out the childHKL range covered within the Qrange specified.
		MathUtil.set3(tempvec0, crystalDInvRange, 0, 0);
		MathUtil.mat3mul(rotcart2slatt, tempvec0, tempvec);
		MathUtil.scaleAdd3(tempvec, 1, crystalHklCenter, limits[0]);
		MathUtil.scale3(limits[0], -1, limits[1]);
		
		MathUtil.set3(tempvec0, 0, crystalDInvRange, 0);
		MathUtil.mat3mul(rotcart2slatt, tempvec0, tempvec);
		MathUtil.scaleAdd3(tempvec, 1, crystalHklCenter, limits[2]);
		MathUtil.scale3(limits[2], -1, limits[3]);

		MathUtil.set3(tempvec0, crystalDInvRange, crystalDInvRange, 0);
		MathUtil.mat3mul(rotcart2slatt, tempvec0, tempvec);
		MathUtil.scaleAdd3(tempvec, 1, crystalHklCenter, limits[4]);
		MathUtil.scale3(limits[4], -1, limits[7]);

		MathUtil.set3(tempvec0, -crystalDInvRange, crystalDInvRange, 0);
		MathUtil.mat3mul(rotcart2slatt, tempvec0, tempvec);
		MathUtil.scaleAdd3(tempvec, 1, crystalHklCenter, limits[5]);
		MathUtil.scale3(limits[5], -1, limits[6]);

		for (int ii = 0; ii < 3; ii++) {
			double tempmin = 1000;
			double tempmax = -1000;
			for (int nn = 0; nn < 8; nn++) {
				double d = limits[nn][ii]; 
				if (d > tempmax)
					tempmax = d;
				if (d < tempmin)
					tempmin = d;
			}
			hklrange[ii][0] = (int) Math.floor(tempmin);
			hklrange[ii][1] = (int) Math.ceil(tempmax);
		}

		// Identify the peaks to display.
		peakCount = 0;
		crystalNearestDistanceToOrigin = drawHalfWidth;
		double[] childHKL = new double[3], childHKLcart = new double[3];
		for (int H = hklrange[0][0]; H <= hklrange[0][1]; H++)
			for (int K = hklrange[1][0]; K <= hklrange[1][1]; K++)
				for (int L = hklrange[2][0]; L <= hklrange[2][1]; L++) {
					boolean planeQ = false;
					boolean rangeQ = false;
					MathUtil.set3(childHKL, H, K, L);
					MathUtil.vecaddN(childHKL, -1.0, crystalHklCenter, tempvec0);
					MathUtil.mat3mul(slatt2rotcart, tempvec0, childHKLcart);

					double inplanetest = Math.abs(MathUtil.dot3(tempvec0, uvw));
					if (inplanetest < ztolerance)
						planeQ = true; // HKL point lies in the display plane
					if ((Math.abs(childHKLcart[0]) < crystalDInvRange)
							&& (Math.abs(childHKLcart[1]) < crystalDInvRange))
						rangeQ = true; // HKL point lies in q range of display
					if (planeQ && rangeQ) {
						// Save the XY coords of a good peak.
						MathUtil.copy3(childHKL, crystalPeakHKL[peakCount]);
						peakMultiplicity[peakCount] = 1;
						double tempscalar = (childHKLcart[0] * childHKLcart[0] + childHKLcart[1] * childHKLcart[1])
								* (drawHalfWidth / crystalDInvRange);
						if ((Math.abs(tempscalar) > 0.1) && (tempscalar < crystalNearestDistanceToOrigin))
							crystalNearestDistanceToOrigin = tempscalar;
						peakCount++;
					}
				}

		// Set the max peak radius
		crystalMaxPeakRadius = Math.min(crystalNearestDistanceToOrigin / 2, 40);

		// Identify the tickmark locations along axis0.
		// cycle over the two axes
		for (int n = 0; n < 2; n++) {
			MathUtil.mat3mul(slatt2rotcart, crystalHkldirections[n], tempvec);
			int t = (int) Math.floor(crystalDInvRange / MathUtil.len3(tempvec));
			crystalTickCount[n] = 2 * t + 1;
			for (int count = 0, m = -t; m < t + 1; m++) {
				MathUtil.scale3(crystalHkldirections[n], m, crystalTickHKL[n][count++]);
			}
		}

		assignPeakTypes();
		recalcCrystal(); // recalculate intensities and positions
	}

	/**
	 * Recalculates structural parameters and peak positions and intensities for
	 * powder pattern. Called by resetPowderPeaks and run().
	 * 
	 */
	private void recalcPowder() {

		// update the structure based on current slider values
		// update the peak positions and values
		// update the peak intensities based on current structure

		variables.recalcDistortion();
		recalcStrainMetrics();
		recalcPowderPeakPositionsAndValues();
		recalcIntensities();
		recalcPowderPattern();
	}

	private void recalcPowderPeakPositionsAndValues() {
		for (int p = 0; p < peakCount; p++) {
			MathUtil.mat3mul(metric, crystalPeakHKL[p], tempvec);
			double dinv = Math.sqrt(MathUtil.dot3(crystalPeakHKL[p], tempvec));
			peakDInv[p] = dinv;
			double dval = (dinv > 0 ? 1 / dinv : 0);
			double v = 0;
			switch (powderPatternType) {
			case POWDER_PATTERN_TYPE_2THETA:
				v = 2 * Math.asin(0.5 * powderWavelength * dinv) * (180 / Math.PI);
				break;
			case POWDER_PATTERN_TYPE_DSPACE:
				v = dval;
				break;
			case POWDER_PATTERN_TYPE_Q:
				v = 2 * Math.PI * dinv;
				break;
			}
			powderPeakX[p] = drawWidth * ((v - powderXMin) / (powderXMax - powderXMin));
			powderPeakY[p] = v;
		}
	}

	private void recalcPowderPattern() {
		// recalculate the powder pattern
		for (int i = 0; i < powderXRange; i++)
			powderY[i] = 0;
		double sigmapix = Math
				.ceil(powderXRange * (powderResolution / Math.sqrt(8 * Math.log(2))) / (powderXMax - powderXMin));
		double f = 1.0 * powderXRange / drawWidth;
		for (int p = 0; p < peakCount; p++) {
			double center = powderPeakX[p] * f;
			int left = Math.max((int) Math.floor(center - 5 * sigmapix), 0);
			int right = Math.min((int) Math.ceil(center + 5 * sigmapix), powderXRange - 1);
			double pmi = peakIntensity[p] * peakMultiplicity[p];
			for (int i = left; i <= right; i++) {
				double d = (i - center) / sigmapix;
				double v = Math.exp(-d * d / 2) * pmi;
				powderY[i] += v;				
			}
		}
	}

	/**
	 * Creates the list of powder peaks and their types.
	 * 
	 * Called updateDisplay()
	 * 
	 */
	private void resetPowderPeaks() {
		double tol = 0.0001;
		powderWavelength = getText(wavTxt, powderWavelength, 2);
		powderXMin = getText(minTxt, powderXMin, 2);
		powderXMax = getText(maxTxt, powderXMax, 2);
		powderResolution = getText(fwhmTxt, powderResolution, 3);
		powderZoom = getText(zoomTxt, powderZoom, 2);
		if ((powderXMax <= powderXMin) || (powderPatternType == POWDER_PATTERN_TYPE_DSPACE)
				&& ((Math.abs(powderXMin) < tol) || (Math.abs(powderXMax) < tol)))
			return;
		setPowderDinvMinMaxRes();

		// Save the old strain slider values
		variables.readSliders();
		double child0 = variables.getSetChildFraction(1);
		int nStrains = variables.getStrainModesCount();
		double[] strainVals = variables.getStrainmodeSliderValues();
		double[] strainTemp = new double[nStrains];
		for (int m = 0; m < nStrains; m++)
			strainTemp[m] = strainVals[m];

		// randomize the strains to avoid accidental degeneracies
		Random rval = new Random();
		double[][] randommetric1 = new double[3][3];
		double[][] randommetric2 = new double[3][3];
		for (int m = 0; m < nStrains; m++)
			strainVals[m] = (2 * rval.nextFloat() - 1);// *rd.strainmodeMaxAmp[m];
		variables.recalcDistortion();// Update the distortion parameters
		recalcStrainMetrics(); // Build the randomized dinvmetric tensor
		MathUtil.mat3copy(metric, randommetric1);

		// randomize the strains again to be extra careful
		for (int m = 0; m < nStrains; m++)
			strainVals[m] = (2 * rval.nextFloat() - 1);// *rd.strainmodeMaxAmp[m];
		variables.recalcDistortion();// Update the distortion parameters
		recalcStrainMetrics(); // Build the randomized dinvmetric tensor
		MathUtil.mat3copy(metric, randommetric2);

		// restore the strains to their original values
		variables.getSetChildFraction(child0);
		for (int m = 0; m < nStrains; m++)
			strainVals[m] = strainTemp[m];
		// use metric tensor to determine h,k,l ranges
		// for each new peak sampled, compare against all previous peaks and accumulate
		// multiplicities
		// if dinv inside ellipsoid
		// for each existing peak
		// if dinv1 equal dinv2
		// if peaks are equivalent
		// keep the nicest one and break
		// end loop
		// if we made it to the end of the loop, add peak to the end of the list

		MathUtil.cross3(metric0[1], metric0[0], tempvec); // BH m1 x m0
		double metricdet = MathUtil.dot3(tempvec, metric0[2]); // Calculate the metric determinant
		int limH = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[1][1] * metric0[2][2] - metric0[1][2] * metric0[1][2]) / metricdet)));
		int limK = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[0][0] * metric0[2][2] - metric0[0][2] * metric0[0][2]) / metricdet)));
		int limL = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[0][0] * metric0[1][1] - metric0[0][1] * metric0[0][1]) / metricdet)));
		double[][] tmat = variables.Tmat;
		peakCount = 0;
		double[] dinvlist0 = new double[maxPeaks];// unstrained list of dinverse values
		double[] dinvlist1 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] dinvlist2 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] childHKL = new double[3];

		for (int h = -limH; h <= limH; h++) {
			for (int k = -limK; k <= limK; k++) {
				for (int l = -limL; l <= limL; l++) {
					MathUtil.set3(childHKL, h, k, l);
					// Generate the standard metric and two randomized metrics.
					MathUtil.mat3mul(metric0, childHKL, tempvec);
					double dinv0 = Math.sqrt(MathUtil.dot3(childHKL, tempvec));
					MathUtil.mat3mul(randommetric1, childHKL, tempvec);
					double dinv1 = Math.sqrt(MathUtil.dot3(childHKL, tempvec));
					MathUtil.mat3mul(randommetric2, childHKL, tempvec);
					double dinv2 = Math.sqrt(MathUtil.dot3(childHKL, tempvec));

					boolean isinrange = (powderDinvmin <= dinv0) && (dinv0 <= powderDinvmax);
					if (isinrange) {
						boolean createNewPeak = true;
						for (int p = 0; p < peakCount; p++) {
							double[] hkl = crystalPeakHKL[p];
							boolean isrobustlycoincident = approxEqual(dinv0, dinvlist0[p], tol)
									&& approxEqual(dinv1, dinvlist1[p], tol) && approxEqual(dinv2, dinvlist2[p], tol);
							if (isrobustlycoincident) {
								boolean isequivalent = checkPowderPeakEquiv(childHKL, hkl, tmat);
								if (isequivalent) {
									if (comparePowderHKL(childHKL, hkl)) {
										MathUtil.copy3(childHKL, hkl);
									}
									peakMultiplicity[p] += 1;
									createNewPeak = false;
									break;
								}
							}
						}

						if (createNewPeak) {
							crystalPeakHKL[peakCount][0] = h;
							crystalPeakHKL[peakCount][1] = k;
							crystalPeakHKL[peakCount][2] = l;
							dinvlist0[peakCount] = dinv0;
							dinvlist1[peakCount] = dinv1;
							dinvlist2[peakCount] = dinv2;
							peakMultiplicity[peakCount] = 1;
							peakCount++;
							if (peakCount >= maxPeaks)
								return;
						}
					}
				}
			}
		}

		sortPowderPeaks(dinvlist0, tol);
		// keep only a practical number of peaks
		if (peakCount > powderMaxVisiblePeaks)
			peakCount = powderMaxVisiblePeaks;

		child0 = variables.getSetChildFraction(0);
		boolean isXrayTemp = isXray;
		isXray = true;
		recalcPowder();
		isXray = isXrayTemp;
		variables.getSetChildFraction(child0);

		// Calculate the x-ray powder-pattern scale factor
		setPowderScaleFactor();
		setPowderAxisTicks(tol);
		assignPeakTypes(); // Type the peaks
		recalcPowder(); // recalculate intensities and positions
	}

	/**
	 * A class that takes care of picking the "best" [h k l] that will 
	 * be displayed on a mouse-over in powder mode. Arrays.sort is passed
	 * an ordered list of integers -- [0, 1, 2, 3, 4, ...], which are 
	 * indices into the HKLs array. 
	 * 
	 * @author hanso
	 *
	 */
	private static class PowderPeakSorter implements Comparator<Integer> {

		private double[] dinv;
		private double tol;
		private double[][] hkls;

		PowderPeakSorter(double[] dinv, double[][] HKLs, double tol) {
			this.dinv = dinv;
			this.hkls = HKLs;
			this.tol = tol;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			int i = o1.intValue();
			int j = o2.intValue();
			boolean sameDinv = approxEqual(dinv[i], dinv[j], tol);
			boolean firstHigher = !sameDinv && dinv[i] > dinv[j];
			boolean firstHKLnicerThanSecond = comparePowderHKL(hkls[i], hkls[j]);
			// we must return -1 if order is OK, 0 if equal, and 1 if they need switching
			return (firstHigher ? 1 : !sameDinv || !firstHKLnicerThanSecond ? -1 : 0);
		}
	}

	/**
	 * Sort the peak according to increasing dinverse and decreasing niceness
	 * 
	 * @param dinvlist0
	 * @param tol
	 * 
	 */
	private void sortPowderPeaks(double[] dinvlist0, double tol) {
		Integer[] sortList = new Integer[peakCount];
		for (int i = 0; i < peakCount; i++)
			sortList[i] = i;
		Arrays.sort(sortList, new PowderPeakSorter(dinvlist0, crystalPeakHKL, tol));
		double[][] hkls = new double[maxPeaks][3];
		double[] mults = new double[maxPeaks];
		for (int i = 0; i < peakCount; i++) {
			int j = sortList[i];
			hkls[i] = crystalPeakHKL[j];
			mults[i] = peakMultiplicity[j];
		}
		crystalPeakHKL = hkls;
		peakMultiplicity = mults;
	}

	private void setPowderDinvMinMaxRes() {
		switch (powderPatternType) {
		case POWDER_PATTERN_TYPE_2THETA:
			powderDinvmin = (2 / powderWavelength) * Math.sin((Math.PI / 180) * powderXMin / 2);
			powderDinvmax = (2 / powderWavelength) * Math.sin((Math.PI / 180) * powderXMax / 2);
			powderDinvres = powderResolution * (1 / powderWavelength) * (Math.PI / 180);
			break;
		case POWDER_PATTERN_TYPE_DSPACE:
			powderDinvmin = 1 / powderXMax;
			powderDinvmax = 1 / powderXMin;
			double d = powderXMin + powderXMax;
			powderDinvres = powderResolution * 4 / d / d;
			break;
		case POWDER_PATTERN_TYPE_Q:
			powderDinvmin = powderXMin / (2 * Math.PI);
			powderDinvmax = powderXMax / (2 * Math.PI);
			powderDinvres = powderResolution * ((powderXMin + powderXMax) / 2) / (2 * Math.PI);
			break;
		}
	}

	
	private void setPowderScaleFactor() {
		powderScaleFactor = 0;
		for (int i = 0; i < powderXRange; i++) {
			if (powderY[i] > powderScaleFactor)
				powderScaleFactor = powderY[i];
		}
		if (Math.abs(powderScaleFactor) <= 0.001) {
			// This should never drop to zero, but prevent it 
			// just in case.
			// BH: this is fine -- no atoms selected
			powderScaleFactor = 0;
		}
	}

	private final static double[] tickspacecandidates = new double[] { 0.01, 0.02, 0.05, .10, .2, .5, 1, 2, 5, 10, 20,
			50 };

	private void setPowderAxisTicks(double tol) {
		// calculate the list of horizontal-axis tick-mark positions
		double powdertickpreference = 15; // max number of ticks
		double tickbest, tickspacing = 0, tickbadness, tickmin, tickmax;

		tickbest = 1000;
		for (int i = 0; i < tickspacecandidates.length; i++) {
			tickbadness = powdertickpreference - (powderXMax - powderXMin) / tickspacecandidates[i];
			if ((tickbadness < tickbest) && (tickbadness > 0)) {
				tickbest = tickbadness;
				tickspacing = tickspacecandidates[i];
			}
		}
		tickmin = Math.ceil(powderXMin / tickspacing) * tickspacing;
		tickmax = Math.floor(powderXMax / tickspacing) * tickspacing;

		powderAxisTickCount = 0;
		for (double t = tickmin; t < tickmax + tol; t += tickspacing) {
			powderAxisTickX[powderAxisTickCount] = drawWidth * ((t - powderXMin) / (powderXMax - powderXMin));
			powderAxisTickLabel[powderAxisTickCount++] = MathUtil.varToString(t, 2, -5);
		}

	}

	/**
	 * Comparison of hkl
	 * 
	 * "Nicer" in order of check:
	 * 
	 * 1) more zeros [100] better than [110]
	 * 
	 * 2) smaller sum of absolute values [011] better than [012]
	 * 
	 * 3) fewer negative values [-111] better than [1-1-1]
	 * 
	 * 4) later zeros [100] better than [001]; [110] better than [101]
	 * 
	 * 5) earlier high absolute values in second and third digits:
	 * 
	 * [0-12] better than [0-21]; [341] better than [242]
	 * 
	 * 6) earlier non-negative values in second and third digits [01-2] better than
	 * [0-12]
	 * 
	 * 
	 * Called by resetPowderPeaks.
	 * 
	 * @return true if hkla is better than or equal to hklb
	 * 
	 */
	private static boolean comparePowderHKL(double[] hkla, double[] hklb) {
		// dist calcalulated as count-sum or product-sum:
		// x.. 0 or 0
		// .x. 1 or x
		// ..x 2 or 2x

		// xy. 1 or y (x ignored)
		// x.y 2 or 2y (x ignored)
		// .xy 5 or x + 2y

		// xyz 6 or y + 2z (x ignored)

		int nZerosA = 0;
		int nZerosB = 0; // number of zeros
		int distZerosA = 0;
		int distZerosB = 0; // distribution of zeros
		int nMinusA = 0;
		int nMinusB = 0; // number of minus signs
		int distMinusA = 0;
		int distMinusB = 0; // distribution of minus signs
		int sumAbsValA = 0;
		int sumAbsValB = 0; // sum of absolute-valued indices
		int distAbsValA = 0;
		int distAbsValB = 0; // distribution of absolute-valued indices
		for (int i = 0; i < 3; i++) {
			int a = (int) hkla[i];
			int b = (int) hklb[i];

			if (a == 0) {
				nZerosA++;
				distZerosA += i;
			}
			if (b == 0) {
				nZerosB++;
				distZerosB += i;
			}

			sumAbsValA += Math.abs(a);
			sumAbsValB += Math.abs(b);

			// BH note: first digit is being thrown away
			// so this is just between 2nd and 3rd digits
			// e.g. [333] (3*1 + 2*3) < (better than) [234] (3*1 + 2*4) here
			// and [341] < [242]

			distAbsValA += i * Math.abs(a);
			distAbsValB += i * Math.abs(b);

			if (a < 0) {
				nMinusA++;
				distMinusA += i;
			}
			if (b < 0) {
				nMinusB++;
				distMinusB += i;
			}
		}

		if (nZerosA != nZerosB)
			return (nZerosA > nZerosB);
		if (sumAbsValA != sumAbsValB)
			return (sumAbsValA < sumAbsValB);
		if (nMinusA != nMinusB)
			return (nMinusA < nMinusB);
		if (distZerosA != distZerosB)
			return (distZerosA > distZerosB);
		if (distAbsValA != distAbsValB)
			return (distAbsValA < distAbsValB); // [042] better than [024]
		return (distMinusA >= distMinusB);
	}

	/**
	 * This test is only run if two peaks have the same d-spacing. Called by
	 * resetPowderPeaks. The only reason to do this test is to increase rendering
	 * efficiency (fewer peaks is faster). The current version is a poor hack --
	 * symmetry is needed soon. As of Apr 2015, the hexcheck was ignored and the
	 * samecheck was weakened since it was finding false equivalences in a hexagonal
	 * child structure.
	 * 
	 */
	private boolean checkPowderPeakEquiv(double[] suphkla, double[] suphklb, double[][] tmat) {
		double tol = 0.0001;
		int[] sa = new int[3];
		int[] sb = new int[3];
		double[] parhkla = new double[3];
		double[] parhklb = new double[3];
		double[] pa = new double[3];
		double[] pb = new double[3];
		MathUtil.mat3mul(tmat, suphkla, parhkla);
		MathUtil.mat3mul(tmat, suphklb, parhklb);
		for (int i = 0; i < 3; i++) {
			sa[i] = (int) Math.abs(suphkla[i]);
			sb[i] = (int) Math.abs(suphklb[i]);
			pa[i] = Math.abs(parhkla[i]);
			pb[i] = Math.abs(parhklb[i]);
		}

		// BH: let Java do the sort
		Arrays.sort(sa);
		Arrays.sort(sb);
		Arrays.sort(pa);
		Arrays.sort(pb);
		return ((sa[0] == sb[0]) && (sa[1] == sb[1]) && (sa[2] == sb[2]) && approxEqual(pa[0], pb[0], tol)
				&& approxEqual(pa[1], pb[1], tol) && approxEqual(pa[2], pb[2], tol));
	}

	private static boolean approxEqual(double a, double b, double tol) {
		return (Math.abs(a - b) < tol);
	}

	/**
	 * identifies parent and super-lattice peaks that are systematically absent
	 * Called by resetCrystalPeaks and resetPowderPeaks.
	 * 
	 */
	public void assignPeakTypes() {
		// Set the peak type to 1 for parent Bragg peaks, and 3 otherwise.

		double[] parenthkl = new double[3];
		double ptolerance = 0.01; // Determines whether or not a peak is a parent Bragg peak
		for (int p = 0; p < peakCount; p++) {
			MathUtil.mat3mul(variables.Tmat, crystalPeakHKL[p], parenthkl); // transform super hkl into parent hkl
			peakColor[p] = 3;
			double d = 0;
			for (int j = 0; j < 3; j++)
				d += Math.abs(parenthkl[j] - Math.rint(parenthkl[j]));
			if (d < ptolerance)
				peakColor[p] = 1;
		}

		// Calculate all peak intensities and set zero-intensity peaks to type 2.
		// Save and zero all displacive, scalar and magnetic mode values
		// Randomize all GM1 mode values
		double child0 = variables.getSetChildFraction(1);
		variables.saveModeValues();
		Random rval = new Random();
		variables.randomizeGM1Values(rval);
		variables.recalcDistortion();
		recalcIntensities();
		for (int p = 0; p < peakCount; p++)
			if ((peakColor[p] == 1) && (Math.abs(peakIntensity[p]) < 0.00000001))
				peakColor[p] = 2;
		variables.randomizeNonGM1Values(rval);
		variables.recalcDistortion();
		recalcIntensities();
		for (int p = 0; p < peakCount; p++)
			if ((peakColor[p] == 3) && (Math.abs(peakIntensity[p]) < 0.00000001))
				peakColor[p] = 4;

		// Restore all displacement and scalar mode values to their original values.

		
		variables.restoreModeValues();
		variables.getSetChildFraction(child0);
		variables.recalcDistortion();
		recalcIntensities();

	}

	/**
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 * 5 5 5 The fifth section has the methods that listen for the sliderbars, the
	 * keyboard and the viewing buttons. 5 5 5
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 * 
	 */

	/**
	 * keyinputListener responds to keyboard commands.
	 * 
	 */
	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void reset() {
		hOTxt.setText("0");
		kOTxt.setText("0");
		lOTxt.setText("0");
		hHTxt.setText("1");
		kHTxt.setText("0");
		lHTxt.setText("0");
		hVTxt.setText("0");
		kVTxt.setText("1");
		lVTxt.setText("0");
		qTxt.setText("4");
		wavTxt.setText("1.54");
		minTxt.setText("5");
		maxTxt.setText("60");
		fwhmTxt.setText("0.200");
		zoomTxt.setText("1.0");
		tButton.setSelected(true);
		parentButton.setSelected(true);
		xrayButton.setSelected(true);
		variables.resetSliders();
		variables.selectAllSubtypes();
		needsRecalc = true;
		colorBox.setSelected(false);
		updateDisplay();
	}

	@Override
	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {
		case 'r':
		case 'R':
			reset();
			break;
		default:
			variables.keyTyped(e);
			break;
		}
	}

	/**
	 * creates the components of the control panel
	 */
	private void buildControls() {
		ButtonGroup g = new ButtonGroup();
		tButton = newRadioButton("2\u0398", true, g);
		dButton = newRadioButton("d", false, g);
		qButton = newRadioButton("q", false, g);

		g = new ButtonGroup();
		parentButton = newRadioButton("Parent", true, g);
		childButton = newRadioButton("Child", false, g);

		g = new ButtonGroup();
		crystalButton = newRadioButton("Crystal", true, g);
		powderButton = newRadioButton("Powder", false, g);
		bothButton = newRadioButton("Both", false, g);

		g = new ButtonGroup();
		xrayButton = newRadioButton("Xray", true, g);
		neutronButton = newRadioButton("Neut", false, g);
		neutronButton.setHorizontalAlignment(JRadioButton.LEFT);

		hOTxt = newTextField("0", -10);
		kOTxt = newTextField("0", -10);
		lOTxt = newTextField("0", -10);
		hHTxt = newTextField("1", -10);
		kHTxt = newTextField("0", -10);
		lHTxt = newTextField("0", -10);
		hVTxt = newTextField("0", -10);
		kVTxt = newTextField("1", -10);
		lVTxt = newTextField("0", -10);
		qTxt = newTextField("4", -10);

		wavTxt = newTextField("1.54", 0);
		minTxt = newTextField("5", 0);
		maxTxt = newTextField("60", 0);
		fwhmTxt = newTextField("0.200", 0);
		zoomTxt = newTextField("1.0", 0);

		horizLabel = new JLabel("   Horiz");
		upperLabel = new JLabel("   Upper");
		centerLabel = new JLabel("Center");
		qrangeLabel = new JLabel("   Q Range");
		wavLabel = new JLabel("   Wave");
		minLabel = new JLabel("   Min");
		maxLabel = new JLabel("   Max");
		fwhmLabel = new JLabel("   Res");
		zoomLabel = new JLabel("   Zoom");

		mouseoverLabel = new JLabel("");

		JPanel topControlPanel = new JPanel();
		JPanel botControlPanel = new JPanel();
		topControlPanel.setBackground(Color.WHITE);
		botControlPanel.setBackground(Color.WHITE);

		topControlPanel.add(mouseoverLabel);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(xrayButton);
		topControlPanel.add(neutronButton);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(parentButton);
		topControlPanel.add(childButton);
		topControlPanel.add(tButton);
		topControlPanel.add(dButton);
		topControlPanel.add(qButton);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(crystalButton);
		topControlPanel.add(powderButton);
		topControlPanel.add(bothButton);
		
		addTopButtons(topControlPanel); // color box

		botControlPanel.add(centerLabel);
		botControlPanel.add(hOTxt);
		botControlPanel.add(kOTxt);
		botControlPanel.add(lOTxt);
		botControlPanel.add(horizLabel);
		botControlPanel.add(hHTxt);
		botControlPanel.add(kHTxt);
		botControlPanel.add(lHTxt);
		botControlPanel.add(upperLabel);
		botControlPanel.add(hVTxt);
		botControlPanel.add(kVTxt);
		botControlPanel.add(lVTxt);
		botControlPanel.add(qrangeLabel);
		botControlPanel.add(qTxt);
		botControlPanel.add(wavLabel);
		botControlPanel.add(wavTxt);
		botControlPanel.add(minLabel);
		botControlPanel.add(minTxt);
		botControlPanel.add(maxLabel);
		botControlPanel.add(maxTxt);
		botControlPanel.add(fwhmLabel);
		botControlPanel.add(fwhmTxt);
		botControlPanel.add(zoomLabel);
		botControlPanel.add(zoomTxt);

		//addBottomButtons(botControlPanel); // applyView

		controlPanel.add(topControlPanel);
		controlPanel.add(botControlPanel);

	}

	/**
	 * loads the control components into the control panel
	 */
	private void showControls() {

		// powder-only

		boolean isPowder = (isBoth && !wasPowder || this.isPowder && !isBoth);
		wasPowder = false;
		wavTxt.setVisible(isPowder);
		minTxt.setVisible(isPowder);
		maxTxt.setVisible(isPowder);
		fwhmTxt.setVisible(isPowder);
		zoomTxt.setVisible(isPowder);
		wavLabel.setVisible(isPowder);
		minLabel.setVisible(isPowder);
		maxLabel.setVisible(isPowder);
		fwhmLabel.setVisible(isPowder);
		zoomLabel.setVisible(isPowder);

		dButton.setVisible(isPowder && !isMouseOver);
		qButton.setVisible(isPowder && !isMouseOver);
		tButton.setVisible(isPowder && !isMouseOver);

		// crystal-only

		hOTxt.setVisible(!isPowder);
		kOTxt.setVisible(!isPowder);
		lOTxt.setVisible(!isPowder);
		hHTxt.setVisible(!isPowder);
		kHTxt.setVisible(!isPowder);
		lHTxt.setVisible(!isPowder);
		hVTxt.setVisible(!isPowder);
		kVTxt.setVisible(!isPowder);
		lVTxt.setVisible(!isPowder);
		qTxt.setVisible(!isPowder);

		horizLabel.setVisible(!isPowder);
		upperLabel.setVisible(!isPowder);
		centerLabel.setVisible(!isPowder);
		qrangeLabel.setVisible(!isPowder);

		parentButton.setVisible(!isPowder && !isMouseOver);
		childButton.setVisible(!isPowder && !isMouseOver);

		// mouseOver-only

		mouseoverLabel.setVisible(isMouseOver);

		// no-mouseOver-only

		xrayButton.setVisible(!isMouseOver);
		neutronButton.setVisible(!isMouseOver);
		crystalButton.setVisible(!isMouseOver);
		powderButton.setVisible(!isMouseOver);
		bothButton.setVisible(!isMouseOver);
		colorBox.setVisible(!isMouseOver && variables.needSimpleColor);
	}

	@Override
	protected BufferedImage getImage() {
		return rp.im;
	}

	@Override
	protected void handleButtonEvent(Object src) {
		if (src instanceof JCheckBox) {
			needsRecalc = true;
			rp.repaint();
			return;
		}
		if (!((JToggleButton) src).isSelected())
			return;
		boolean setTextBoxes = false;
		if (src == tButton) {
			powderPatternType = 1;
			double t = powderDinvmin * powderWavelength / 2;
			if (t < -1)
				t = -1;
			if (t > 1)
				t = 1;
			powderXMin = (180 / Math.PI) * 2 * Math.asin(t);
			t = powderDinvmax * powderWavelength / 2;
			if (t < -1)
				t = -1;
			if (t > 1)
				t = 1;
			powderXMax = (180 / Math.PI) * 2 * Math.asin(t);
			powderResolution = powderDinvres * powderWavelength / (Math.PI / 180);
			setTextBoxes = true;
			needsRecalc = true;
		} else if (src == dButton) {
			powderPatternType = 2;
			powderXMax = 1 / powderDinvmin;
			powderXMin = 1 / powderDinvmax;
			double d = powderXMin + powderXMax;
			powderResolution = powderDinvres * d * d / 4;
			setTextBoxes = true;
			needsRecalc = true;
		} else if (src == qButton) {
			powderPatternType = 3;
			powderXMin = powderDinvmin * (2 * Math.PI);
			powderXMax = powderDinvmax * (2 * Math.PI);
			powderResolution = powderDinvres * (2 * Math.PI) / ((powderXMin + powderXMax) / 2);
			setTextBoxes = true;
			needsRecalc = true;
		} else if (src == parentButton) {
			hklType = 1;
			needsRecalc = true;
		} else if (src == childButton) {
			hklType = 2;
			needsRecalc = true;
		} else if (src == xrayButton) {
			isXray = true;
			variables.readSliders();
			needsRecalc = true;
		} else if (src == neutronButton) {
			isXray = false;
			variables.readSliders();
			needsRecalc = true;
		} else if (src == crystalButton) {
			isBoth = isPowder = false;
			showControls();
			needsRecalc = true;
		} else if (src == powderButton) {
			isBoth = !(isPowder = true);
			showControls();
			needsRecalc = true;
		} else if (src == bothButton) {
			wasPowder = isPowder;
			isBoth = isPowder = true;
			showControls();
			needsRecalc = true;
		}
		if (setTextBoxes && !isAdjusting) {
			minTxt.setText(trim00(powderXMin));
			maxTxt.setText(trim00(powderXMax));
			fwhmTxt.setText(MathUtil.varToString(powderResolution, 3, 0));
		}
		updateDisplay();
	}

	@Override
	protected boolean prepareToSwapOut() {
		return true;
	}

	@Override
	protected void applyView() {
		updateDisplay();
	}

	@Override
	public void centerImage() {
		// n/a
	}

	@Override
	public void recalcCellColors() {
		// na/a
	}

	public static void main(String[] args) {
		create("IsoDiffract", args);
	}

	@Override
	protected void updateViewOptions() {
		// n/a
	}


}
