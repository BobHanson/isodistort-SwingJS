package org.byu.isodistort;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
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
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.Variables.IsoAtom;
import org.byu.isodistort.local.Variables.SymopData;

import javajs.util.PT;

/**
*  
*  Branton Campbell, David Tanner Andrew Zimmerman, 
*  June 2006
*  
*  Refactored by Bob Hanson, 2023, 2025
* 
* This applet takes the data from the IsoDistort web page 
* and uses it to render atoms and bonds and cells that represent various atomic crystal structures.
* 
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
* and adds GUI inner class
*  
*/

//http://stokes.byu.edu/isodistort.html is isodistort website

public class IsoDiffractApp extends IsoApp {

	
	protected static boolean usePrimitiveAssignmentMethod = false;


	private static class PeakData {

		/**
		 * peak multiplicities for powder pattern
		 */
		double peakMultiplicity;
		
		/**
		 * dinverse value for peak in either single-crystal or powder display
		 * 
		 */
		double peakDInv;

		/**
		 * childHKL contained within the display
		 */
		private double[] crystalPeakHKL = new double[3];
		
		/**
		 * parentHKL contained within the display
		 */
		private double[] parentPeakHKL = new double[3];
		
		/**
		 * string of childHKL contained within the display
		 */
		protected String crystalPeakHKLString;

		/**
		 * string of parentHKL contained within the display
		 */
		protected String parentPeakHKLString;

		/**
		 * peak XY coord contained within the single-crystal display
		 */
		double[] crystalPeakXY = new double[2];
		
		/**
		 * peak radius contained within the single-crystal display
		 */

		double crystalPeakRadius;

		/**
		 * position of powder peaks in either 2th, d or q units
		 */
		double powderPeakY;

		/**
		 * position of powder peaks in scaled pixel units
		 */
		double powderPeakX;
		
		/**
		 * peak intensity contained within the single-crystal display
		 */
		double peakIntensity;
		
		/**
		 * peak type/color (1-4)
		 */
		protected int peakType;
		@SuppressWarnings("unused")
		private SymopData op;

		protected void setPeakType(int peakType, SymopData op) {
			this.peakType = peakType;
			this.op = op;
		}

		public void updateDinverse(double[][] metric, double[] t3) {
			MathUtil.mat3mul(metric, crystalPeakHKL, t3);
			peakDInv = MathUtil.dot3Length(crystalPeakHKL, t3);
		}

		/**
		 * Set the crystal peak HKL string for the child used in display and also
		 * assignment when it is asssigned. Do this once for every peak.
		 * 
		 * @param p
		 * @param hkl
		 */
		public void setPeakHKLStrings(double[][] mat) {
				if (crystalPeakHKLString == null) {
					crystalPeakHKLString = MathUtil.roundVec00(crystalPeakHKL);
				}
				if (parentPeakHKLString == null) {
					MathUtil.mat3mul(mat, crystalPeakHKL, parentPeakHKL);
					parentPeakHKLString = MathUtil.roundVec00(parentPeakHKL);
			    }
		}

	}
	
	/**
	 * An alternative panel to the RenderPanel3D of IsoDistortApp
	 * 
	 * @author Bob Hanson
	 *
	 * 
	 */
	private class RenderPanel extends JPanel {

		private MouseAdapter adapter;
		
		RenderPanel() {
			adapter = new Adapter();
			addMouseMotionListener(adapter);
			addMouseListener(adapter);
		}

		private BufferedImage im;

		@Override
		public void paint(Graphics gr) {
//			super.paint(gr);
			Dimension d = getSize();
			gr.setColor(Color.black);
			gr.fillRect(0,  0,  d.width, d.height);
			//System.out.println("RenderPanel paint " + drawWidth + "," + drawHeight + " d=" + d);
			if (needsRecalc || drawWidth != d.width || drawHeight != d.height) {
				IsoDiffractApp.this.updateForPaint();
				return;
			}
			if (im == null || im.getWidth() != d.width || im.getHeight() != d.height)
				im = (BufferedImage) createImage(d.width, d.height);
			Graphics g = im.getGraphics();
		    IsoDiffractApp.this.render(g);
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
				mousePeak(e.getX(), e.getY());
			}

			@Override
			public void mouseDragged(MouseEvent e) {
				int mx = e.getX();
				int my = e.getY();
				IsoDiffractApp.this.mouseDrag(mx - mouseX, my - mouseY);
				mouseX = mx;
				mouseY = my;
			}

			@Override
			public void mousePressed(MouseEvent e) {
				mouseX = e.getX(); mouseY = e.getY();
				setStatusVisible(false);
			}

		}

	}

	public static final int HKL_TYPE_PARENT = 1;
	public static final int HKL_TYPE_CHILD =  2;

	private final static int PEAK_TYPE_ERROR = 0;
	private final static int PEAK_TYPE_PARENT_BRAGG = 1;
	private final static int PEAK_TYPE_PARENT_SYSABS = 2;
	private final static int PEAK_TYPE_CHILD_BRAGG = 3;
	private final static int PEAK_TYPE_CHILD_SYSABS = 4;
	
		
	private static final Color[] colorMap = new Color[5];
	static {
		colorMap[PEAK_TYPE_ERROR]         = Color.PINK;
		colorMap[PEAK_TYPE_PARENT_BRAGG]  = Color.GREEN;
		colorMap[PEAK_TYPE_PARENT_SYSABS] = Color.RED;
		colorMap[PEAK_TYPE_CHILD_BRAGG]   = Color.BLUE;
		colorMap[PEAK_TYPE_CHILD_SYSABS]  = Color.ORANGE; 
	}

	private static String colorOf(int peakType) {
		switch (peakType) {
		default:
		case PEAK_TYPE_ERROR:
			return "pink";
		case PEAK_TYPE_PARENT_BRAGG:
			return "green";
		case PEAK_TYPE_PARENT_SYSABS:
			return "red";
		case PEAK_TYPE_CHILD_BRAGG:
			return "blue";
		case PEAK_TYPE_CHILD_SYSABS:
			return "orange"; 
		}
	}

	private static String typeOf(int peakType) {
		switch (peakType) {
		default:
		case PEAK_TYPE_ERROR:
			return "ERROR";
		case PEAK_TYPE_PARENT_BRAGG:
			return "PARENT_BRAG";
		case PEAK_TYPE_PARENT_SYSABS:
			return "PARENT_SYSABS";
		case PEAK_TYPE_CHILD_BRAGG:
			return "CHILD_BRAGG";
		case PEAK_TYPE_CHILD_SYSABS:
			return "CHILD_SYSABS"; 
		}
	}


	private final static int POWDER_PATTERN_TYPE_2THETA = 1;
	private final static int POWDER_PATTERN_TYPE_DSPACE = 2;
	private final static int POWDER_PATTERN_TYPE_Q = 3;

	private static final double MIN_PEAK_INTENSITY = 1e-25;
	
	private static final double powderPeakTolerance = 0.0001;

	// Variables that the user may want to adjust
	/**
	 * Maximum number of diffraction peaks and tick marks in the display
	 */
	private int maxPeaks = 1000;
	
	private int powderMaxVisiblePeaks = 200;

	private int maxTicks = 100;
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

	// global objects
	
	/**
	 * array of peak data for peaks contained in the display
	 */
	PeakData[] peakData = new PeakData[maxPeaks];

	
	// Other global variables.
	

	int hklType = HKL_TYPE_PARENT;
	
	/**
	 * Horizontal axis choice for powder pattern: (1) 2theta, (2) d-space, (3) q
	 */
	int powderPatternType = POWDER_PATTERN_TYPE_2THETA;

	/**
	 * X-ray vs neutron diffraction
	 */
	public boolean isXray = true;
	
	/**
	 * half the always-square display width and height in pixels
	 */
	double drawHalfWidthHeight;
	
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
	double[][] matChildReciprocal2rotatedCartesian = new double[3][3];
	/**
	 * transforms properly-rotated XYZ cartesian to childHKL
	 */
	double[][] matRotatedCartesian2ChildReciprocal = new double[3][3];

	/**
	 * maximum peak radius
	 */
	private double crystalMaxPeakRadius;
	/**
	 * The hkl horizonal direction, upper direction, and center,
	 */
	private double[] crystalHklCenter = new double[3];
	/**
	 * number of single-crystal tickmarks to be displayed
	 */
	private int[] crystalTickCount = new int[2];
	/**
	 * List of tickmark supHKLs to be displayed
	 */
	private double[][][] crystalTickHKL = new double[2][maxTicks][3];
	/**
	 * List of tickmark (X1Y1, X2Y2) coords to be displayed
	 */
	private double[][][] crystalTickXY2 = new double[2][maxTicks][4];

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

	/**
	 * BH EXPERIMENTAL allows selective removal of atoms from powder pattern
	 */
	private boolean allowSubtypeSelection = true;

	private IsoDiffractGUI gui;

	public IsoDiffractApp() {
		super(APP_ISODIFFRACT);
		gui = new IsoDiffractGUI();
	}

	protected void updateForPaint() {
		updateDimensions();
		needsRecalc = true;
		updateDisplay();
	}

	@Override
	protected void dispose() {
		if (gui != null)
			gui.dispose();
		gui = null;
		super.dispose();
	}

	@Override
	protected void init() {
		gui.init();
		drawPanel.add(gui.rp);
	}

	@Override
	protected void frameResized() {
		super.frameResized();
		needsRecalc = true;
		if (gui != null && gui.frameResized())
			updateDisplay();
	}

	@Override
	protected void updateDimensions() {
		super.updateDimensions();
		int drawWH2 = Math.min(drawWidth, drawHeight)/ 2;
		drawHalfWidthHeight = drawWH2;

	}

	@Override
	public synchronized void updateDisplay() {
		if (isAdjusting || drawHeight < 20 || variables == null)
			return;
		if (needsRecalc || variables.isChanged) {
			isAdjusting = true;
			variables.readSliders();
			variables.enableSubtypeSelection(allowSubtypeSelection);//  && (isBoth || isPowder));
			if (isBoth || isPowder)
				resetPowderPeaks();
			if (isBoth || !isPowder)
				resetCrystalPeaks();
			needsRecalc = false;
			variables.isChanged = false;
			gui.repaint();
			isAdjusting = false;
		}
		super.updateDisplay();
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
		int max = (int) Math.rint(crystalMaxPeakRadius);
		for (int p = 0; p < peakCount; p++) {
			drawCrystalCircle(gr, peakData[p], max);
		}
	}

	private void drawCrystalCircle(Graphics gr, PeakData p, int max) {
		double dx = p.crystalPeakXY[0];
		double dy = p.crystalPeakXY[1];
		double dradius = p.crystalPeakRadius;
		int color = p.peakType;	
		boolean tooBig = false;
		int radius = (int) Math.rint(dradius);
		if (radius >= max) {
			radius = max;
			tooBig = true;
		}
		int x = (int) (dx);
		int y = (int) (dy);
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

		double y = drawHeight - stickYOffset;
		gr.setColor(colorMap[0]);
		drawDash(gr, drawWidth/2, drawHeight - axisYOffset, 1, 0, drawWidth/2, lineThickness, 0);
		for (int p = 0; p < peakCount; p++) {
			if (peakData[p].peakIntensity < MIN_PEAK_INTENSITY)
				drawDash(gr, peakData[p].powderPeakX, y, 0, 1, tickLength, lineThickness, peakData[p].peakType);
		}
		for (int c = 4; c > 0; c--) {
			// draw one color at at time with
			// red and green last and on top
			for (int p = 0; p < peakCount; p++) {
				if (peakData[p].peakType != c || peakData[p].peakIntensity < MIN_PEAK_INTENSITY)
					continue;
				drawDash(gr, peakData[p].powderPeakX, y, 0, 1, tickLength, lineThickness, c);
			}
		}
		gr.setColor(colorMap[0]);
		int ytick = (int) (drawHeight - powderLabelOffset);
		for (int n = 0; n < powderAxisTickCount; n++) {
			drawDash(gr, powderAxisTickX[n], drawHeight - axisYOffset, 0, 1, tickLength, lineThickness, 0);
			gr.drawString(powderAxisTickLabel[n], (int) powderAxisTickX[n] - 15, ytick);
		}
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
	 * @param x is the x-coordinate of the mouse in the Applet window
	 * @param y is the y-coordinate of the mouse in the Applet window
	 * @return which peak is under the mouse
	 * 
	 */
	private void mousePeak(int x, int y) {

		double tol2 = (isPowder ? 2 * 2 : 6 * 6);
		int thisPeak = -1, currentcolor = 5;
		String mouseovertext, valuestring = "", specifictext = "";
		boolean isPowder = this.isPowder && (!isBoth || y > drawHeight - shortPowderHeight);
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			if (isPowder) {
				if (MathUtil.approxEqual(x, pd.powderPeakX, tol2)
						&& (!isBoth || (Math.abs(y - (drawHeight - powderStickYOffset)) < 1.25 * normalTickLength))
						&& (pd.peakType < currentcolor)) {
					thisPeak = p;
					currentcolor = pd.peakType;
					valuestring = MathUtil.trim00(pd.powderPeakY);
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
				double dx = x - pd.crystalPeakXY[0];
				double dy = y - pd.crystalPeakXY[1];
				if (dx * dx + dy * dy <= tol2) {
					thisPeak = p;
					specifictext = "";
					break;
				}
			}
		}
		if (thisPeak >= 0) {
			PeakData p = peakData[thisPeak];
			String intensity = toIntensityString(p.peakIntensity);
			mouseovertext = "Parent HKL = " + p.parentPeakHKLString
					+ "       Child HKL = " + p.crystalPeakHKLString 
					+ "     " + specifictext
					+ (p.peakIntensity < MIN_PEAK_INTENSITY ? ""
							: "   I = " + intensity + (isPowder ? "   M = " + (int) p.peakMultiplicity : ""));

			isMouseOver = true;
		} else {
			mouseovertext = "";
			isMouseOver = false;
		}
		gui.setMouseOverText(mouseovertext);
		gui.showControls();
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
		gui.setZoom(powderZoom, dy);
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
		/**
		 * transforms childReciprocol to XYZ cartesian
		 */
		double[][] reciprocol2cartesian = new double[3][3];
		variables.childCell.setMetricTensor(reciprocol2cartesian, metric0, false);
		variables.childCell.setMetricTensor(reciprocol2cartesian, metric, true);

		// Create an orthonormal rotation matrix that moves axis 1 to the +x 
		// direction, while keeping axis 2 in the +y quadrant.
		double[][] rotmat = getNormalRotation(reciprocol2cartesian);

		// Combine the rotation and the cartesian conversion 
		// to get the overall childHKL to cartesian transformation.
		MathUtil.mat3product(rotmat, reciprocol2cartesian, matChildReciprocal2rotatedCartesian, tempmat);
		// and its inverse
		MathUtil.mat3inverse(matChildReciprocal2rotatedCartesian, matRotatedCartesian2ChildReciprocal, tempvec, tempmat);

	}

	/**
	 * Create an orthonormal rotation matrix that moves axis 1 to the +x direction,
	 * while keeping axis 2 in the +y quadrant.
	 * 
	 * First transform both axes into cartesian coords. Then take 1.cross.2 to get
	 * axis 3, and 3.cross.1 to get the new axis 2.
	 * 
	 * Normalize all three and place them in the rows of the transformation matrix.
	 * 
	 * Rotates cartesian q-space so as to place axes1 along +x and axes2 in +y
	 * hemi-plane.
	 */
	private double[][] getNormalRotation(double[][] slatt2cart) {
		double[][] rotmat = new double[3][3];

		MathUtil.mat3mul(slatt2cart, crystalHkldirections[0], rotmat[0]);
		MathUtil.mat3mul(slatt2cart, crystalHkldirections[1], rotmat[1]);
		MathUtil.cross3(rotmat[0], rotmat[1], rotmat[2]);
		MathUtil.cross3(rotmat[2], rotmat[0], rotmat[1]);
		MathUtil.norm3(rotmat[0]);
		MathUtil.norm3(rotmat[1]);
		MathUtil.norm3(rotmat[2]);
		return rotmat;
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
			PeakData pd = peakData[p];
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
			MathUtil.set3(variables.childCell.toTempCartesian(pd.crystalPeakHKL), qhat);
			MathUtil.norm3(qhat);
			double d = 2 * Math.PI * pd.peakDInv;
			double thermal = Math.exp(-0.5 * uiso * d * d);
			
			for (int ia = 0, n = variables.nAtoms; ia < n; ia++) {
				IsoAtom a = variables.getAtom(ia);
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
				double phase = 2 * Math.PI * MathUtil.dot3(pd.crystalPeakHKL, supxyz);
				double cos = Math.cos(phase);
				if (Math.abs(cos) < 1e-13) {
					// BH correcting for cos (Math.PI/2) == 6.123233995736766E-17, not zero
					cos = 0;
				}
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
			pd.peakIntensity = (f > 0 && f < MIN_PEAK_INTENSITY ? MIN_PEAK_INTENSITY / 2 : thermal * f);
		}
	}

	/**
	 * Recalculates peak positions and intensities for single-crystal pattern.
	 * Called by resetCrystalPeaks and run().
	 * 
	 */
	private void recalcCrystal() {
		double[] t03 = new double[3], t3 = new double[3];

		int dw = (int) (drawWidth/2-drawHalfWidthHeight);
		int dh = (int) (drawHeight/2-drawHalfWidthHeight);

		// update the structure based on current slider values
		variables.recalcDistortion();

		recalcStrainMetrics();

		// update the peak drawing coordinates
		double minWH = Math.min(drawHalfWidthHeight, drawHalfWidthHeight);
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			MathUtil.vecaddN(pd.crystalPeakHKL, -1.0, crystalHklCenter, t03);
			MathUtil.mat3mul(matChildReciprocal2rotatedCartesian, t03, t3);
			pd.crystalPeakXY[0] = (1 + t3[0] / crystalDInvRange) * minWH + dw;
			pd.crystalPeakXY[1] = (1 - t3[1] / crystalDInvRange) * minWH + dh; 
		}

		// Update the dinverse list
		for (int p = 0; p < peakCount; p++) {
			peakData[p].updateDinverse(metric, t3);
		}

		// Update the axis and tickmark plot parameters
		for (int n = 0; n < 2; n++) // cycle over the two axes
		{
			MathUtil.mat3mul(matChildReciprocal2rotatedCartesian, crystalHkldirections[n], t3);
			MathUtil.norm3(t3);
			double dirX = t3[0];
			double dirY = t3[1];
			crystalAxesXYDirXYLen[n][0] = drawHalfWidthHeight + dw;
			crystalAxesXYDirXYLen[n][1] = drawHalfWidthHeight + dh;
			crystalAxesXYDirXYLen[n][2] = dirX;
			crystalAxesXYDirXYLen[n][3] = -dirY; // minus sign turns the picture upside right.
			crystalAxesXYDirXYLen[n][4] = (Math.abs(dirX) <= 0.001 ? drawHalfWidthHeight 
					: Math.abs(dirX) > Math.abs(dirY) ? drawHalfWidthHeight / Math.abs(dirX) 
					: drawHalfWidthHeight / Math.abs(dirY));

			for (int m = 0; m < crystalTickCount[n]; m++) {
				MathUtil.mat3mul(matChildReciprocal2rotatedCartesian, crystalTickHKL[n][m], t3);
				
				crystalTickXY2[n][m][0] = (1 + t3[0] / crystalDInvRange) * drawHalfWidthHeight + dw;
				crystalTickXY2[n][m][1] = (1 - t3[1] / crystalDInvRange) * drawHalfWidthHeight + dh; 

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
			peakData[p].crystalPeakRadius = crystalMaxPeakRadius
					* (Math.max(Math.log(peakData[p].peakIntensity) / 2.3025, -logfac) + logfac) / logfac;

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

		double[][] parent2childLattice = new double[3][3]; // transforms parentHKL to childHKL
		MathUtil.mat3inverse(variables.childCell.conv2convParentTransposeP, parent2childLattice, tempvec, tempmat);

		gui.setHKL(hklO, hklH, hklV);

		crystalDInvRange = getTextValue(gui.qTxt, crystalDInvRange * (2 * Math.PI), 2) / (2 * Math.PI);

		// Decide that user input is either of parentHKL or childHKL type
		// Either way, the input directions are passed as childHKL vectors.
		// Note that the platt2slatt matrix is slider-bar independent.
		switch (hklType) {
		case HKL_TYPE_CHILD:
			MathUtil.copy3(hklH, crystalHkldirections[0]);
			MathUtil.copy3(hklV, crystalHkldirections[1]);
			MathUtil.copy3(hklO, crystalHklCenter);
			break;
		case HKL_TYPE_PARENT:
			MathUtil.mat3mul(parent2childLattice, hklH, crystalHkldirections[0]);
			MathUtil.mat3mul(parent2childLattice, hklV, crystalHkldirections[1]);
			MathUtil.mat3mul(parent2childLattice, hklO, crystalHklCenter);
			break;
		}

		// Identify the direct-space direction perpendicular to display plane.
		double[] uvw = new double[3];
		MathUtil.cross3(crystalHkldirections[0], crystalHkldirections[1], uvw);
		variables.readSliders(); // Get the latest strain information
		variables.recalcDistortion(); // Update the distortion
		recalcStrainMetrics(); // Get updat ed slatt2rotcart transformation

		int[] min = new int[] { 1000, 1000, 1000 };
		int[] max = new int[] { -1000, -1000, -1000 };
		// Find out the childHKL range covered within the Qrange specified.

		checkDisplayRange(crystalDInvRange, 0, min, max);
		checkDisplayRange(0, crystalDInvRange, min, max);
		checkDisplayRange(crystalDInvRange, crystalDInvRange, min, max);
		checkDisplayRange(-crystalDInvRange, crystalDInvRange, min, max);

		// Identify the peaks to display.
		for (int i = 0; i < peakCount; i++)
			peakData[i] = null;
		peakCount = 0;
		double r2max = drawHalfWidthHeight * drawHalfWidthHeight;
		double f2 = r2max / crystalDInvRange / crystalDInvRange;
		double[] childHKL = new double[3], childHKLcart = new double[3];
		for (int H = min[0]; H <= max[0]; H++)
			for (int K = min[1]; K <= max[1]; K++)
				for (int L = min[2]; L <= max[2]; L++) {
					MathUtil.set3(childHKL, H, K, L);
					MathUtil.vecaddN(childHKL, -1.0, crystalHklCenter, tempvec0);
					MathUtil.mat3mul(matChildReciprocal2rotatedCartesian, tempvec0, childHKLcart);
					if (Math.abs(MathUtil.dot3(tempvec0, uvw)) >= ztolerance
							|| Math.abs(childHKLcart[0]) >= crystalDInvRange
							|| Math.abs(childHKLcart[1]) >= crystalDInvRange)
						continue;
					// HKL point lies in q range of display
					// HKL point lies in the display plane
					// Save the XY coords of a good peak.
					peakData[peakCount] = new PeakData();
					MathUtil.copy3(childHKL, peakData[peakCount].crystalPeakHKL);
					peakData[peakCount].peakMultiplicity = 1;
					double r2 = (childHKLcart[0] * childHKLcart[0] + childHKLcart[1] * childHKLcart[1]) * f2;
					if ((Math.abs(r2) > 0.01) && (r2 < r2max)) {
						r2max = r2;
					}
					peakCount++;
				}

		// Set the max peak radius
		crystalMaxPeakRadius = Math.min(Math.sqrt(r2max) / 2, 40);

		// Identify the tickmark locations along axis0.
		// cycle over the two axes
		for (int n = 0; n < 2; n++) {
			MathUtil.mat3mul(matChildReciprocal2rotatedCartesian, crystalHkldirections[n], tempvec);
			int t = (int) Math.floor(crystalDInvRange / MathUtil.len3(tempvec));
			crystalTickCount[n] = 2 * t + 1;
			for (int count = 0, m = -t; m < t + 1; m++) {
				MathUtil.scale3(crystalHkldirections[n], m, crystalTickHKL[n][count++]);
			}
		}

		assignPeakTypes();
		recalcCrystal(); // recalculate intensities and positions
	}

	private void checkDisplayRange(double x, double y, int[] min, int[] max) {
		double[] limit = tempvec0;
		MathUtil.set3(limit, x, y, 0);
		MathUtil.mat3mul(matRotatedCartesian2ChildReciprocal, limit, tempvec);
		MathUtil.scaleAdd3(tempvec, 1, crystalHklCenter, limit);
		for (int ii = 0; ii < 3; ii++) {
			double d = limit[ii];
			if (d < min[ii])
				min[ii] = (int) Math.floor(d);
			else if (-d < min[ii])
				min[ii] = (int) Math.floor(-d);
			if (d > max[ii])
				max[ii] = (int) Math.ceil(d);
			else if (-d > max[ii])
				max[ii] = (int) Math.ceil(-d);
		}
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
			PeakData pd = peakData[p];
			MathUtil.mat3mul(metric, pd.crystalPeakHKL, tempvec);
			double dinv = MathUtil.dot3Length(pd.crystalPeakHKL, tempvec);
			pd.peakDInv = dinv;
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
			pd.powderPeakX = drawWidth * ((v - powderXMin) / (powderXMax - powderXMin));
			pd.powderPeakY = v;
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
			PeakData pd = peakData[p];
			double center = pd.powderPeakX * f;
			int left = Math.max((int) Math.floor(center - 5 * sigmapix), 0);
			int right = Math.min((int) Math.ceil(center + 5 * sigmapix), powderXRange - 1);
			double pmi = pd.peakIntensity * pd.peakMultiplicity;
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
		double tol = powderPeakTolerance;
		powderWavelength = getTextValue(gui.wavTxt, powderWavelength, 2);
		powderXMin = getTextValue(gui.minTxt, powderXMin, 2);
		powderXMax = getTextValue(gui.maxTxt, powderXMax, 2);
		powderResolution = getTextValue(gui.fwhmTxt, powderResolution, 3);
		powderZoom = getTextValue(gui.zoomTxt, powderZoom, 2);
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
		MathUtil.copyNN(metric, randommetric1);

		// randomize the strains again to be extra careful
		for (int m = 0; m < nStrains; m++)
			strainVals[m] = (2 * rval.nextFloat() - 1);// *rd.strainmodeMaxAmp[m];
		variables.recalcDistortion();// Update the distortion parameters
		recalcStrainMetrics(); // Build the randomized dinvmetric tensor
		MathUtil.copyNN(metric, randommetric2);

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
		double[][] tconv = variables.childCell.conv2convParentTransposeP;
		peakCount = 0;
		double[] dinvlist0 = new double[maxPeaks];// unstrained list of dinverse values
		double[] dinvlist1 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] dinvlist2 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] testHKL = new double[3];
		double[] ta = new double[3], tb = new double[3];
		for (int h = -limH; h <= limH; h++) {
			for (int k = -limK; k <= limK; k++) {
				for (int l = -limL; l <= limL; l++) {
					MathUtil.set3(testHKL, h, k, l);
					// Generate the standard metric and two randomized metrics.
					MathUtil.mat3mul(metric0, testHKL, tempvec);
					double dinv0 = MathUtil.dot3Length(testHKL, tempvec);
					MathUtil.mat3mul(randommetric1, testHKL, tempvec);
					double dinv1 = MathUtil.dot3Length(testHKL, tempvec);
					MathUtil.mat3mul(randommetric2, testHKL, tempvec);
					double dinv2 = MathUtil.dot3Length(testHKL, tempvec);

					boolean isinrange = (powderDinvmin <= dinv0) && (dinv0 <= powderDinvmax);
					if (isinrange) {
						boolean createNewPeak = true;
						for (int p = 0; p < peakCount; p++) {
							PeakData pd = peakData[p];
							double[] peakHKL = pd.crystalPeakHKL;
							boolean isrobustlycoincident = MathUtil.approxEqual(dinv0, dinvlist0[p], tol)
									&& MathUtil.approxEqual(dinv1, dinvlist1[p], tol)
									&& MathUtil.approxEqual(dinv2, dinvlist2[p], tol);
							if (isrobustlycoincident // 
									&& checkPowderPeakEquiv(testHKL, peakHKL, null, ta, tb) // child
									&& checkPowderPeakEquiv(testHKL, peakHKL, tconv, ta, tb) // parent
									) {
								if (comparePowderHKL(testHKL, peakHKL)) {
									MathUtil.copy3(testHKL, peakHKL);
								}
								pd.peakMultiplicity += 1;
								createNewPeak = false;
								break;
							}
						}
						if (createNewPeak) {
							PeakData pd = peakData[peakCount] = new PeakData();
							MathUtil.set3(pd.crystalPeakHKL, h, k, l);
							dinvlist0[peakCount] = dinv0;
							dinvlist1[peakCount] = dinv1;
							dinvlist2[peakCount] = dinv2;
							pd.peakMultiplicity = 1;
							if (++peakCount >= maxPeaks)
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

		// temporariy set to parent and xray
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
	 * @author Bob Hanson
	 *
	 */
	private class PowderPeakSorter implements Comparator<Integer> {

		private double tol;
		private double[] dinvList;

		PowderPeakSorter(double[] dinvlist, double tol) {
			this.dinvList = dinvlist;
			this.tol = tol;
		}

		@Override
		public int compare(Integer o1, Integer o2) {
			int i = o1.intValue();
			int j = o2.intValue();
			double dinvi = dinvList[i];
			double dinvj = dinvList[j];
			boolean sameDinv = MathUtil.approxEqual(dinvi, dinvj, tol);
			boolean firstHigher = !sameDinv && dinvi > dinvj;
			boolean firstHKLnicerThanSecond = comparePowderHKL(peakData[i].crystalPeakHKL, peakData[j].crystalPeakHKL);
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
		Arrays.sort(sortList, new PowderPeakSorter(dinvlist0, tol));
		PeakData[] newPeakData = new PeakData[maxPeaks];
		for (int i = 0; i < peakCount; i++) {
			newPeakData[i] = peakData[sortList[i]];
		}
		peakData = newPeakData;
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
	 * This test is only run if two conventional peaks have the same d-spacing both
	 * for child and parent.
	 * 
	 * Called by resetPowderPeaks. The only reason to do this test is to increase
	 * rendering efficiency (fewer peaks is faster). The current version is a poor
	 * hack -- symmetry is needed soon. As of Apr 2015, the hexcheck was ignored and
	 * the samecheck was weakened since it was finding false equivalences in a
	 * hexagonal child structure.
	 * 
	 * @param a first childHKL
	 * @param b second childHKL
	 * @param tconv child.conv2convParent, or null if comparing child
	 * @param ta temporary vector
	 * @param tb temporary vector
	 * @return true if approximately equivalent
	 */
	private boolean checkPowderPeakEquiv(double[] a, double[] b, double[][] tconv, double[] ta, double[] tb) {
		double tol = 0.0001;
		if (tconv != null) {
			MathUtil.mat3mul(tconv, a, ta);
			MathUtil.mat3mul(tconv, b, tb);
			a = ta;
			b = tb;
		}
		MathUtil.abs3(a, ta);
		MathUtil.abs3(b, tb);
		Arrays.sort(ta);
		Arrays.sort(tb);
		return MathUtil.approxEqual3(ta, tb, tol);
	}

	/**
	 * identifies parent and super-lattice peaks that are systematically absent
	 * Called by resetCrystalPeaks and resetPowderPeaks.
	 * 
	 */
	public void assignPeakTypes() {
		for (int p = 0; p < peakCount; p++) {
			peakData[p].setPeakHKLStrings(variables.childCell.conv2convParentTransposeP);
		}
		if (variables.childCell.conv2primTransposeP == null) {
			assignPeakTypesByRandomizing();
		} else if (variables.parentCell.symopData == null || usePrimitiveAssignmentMethod) {
			assignPeakTypesPrimitive();
		} else {
			assignPeakTypesUsingOperations();
		}
	}

	/**
	 * This is the newer, preferred method, using operations
	 */
	private void assignPeakTypesUsingOperations() {
//		System.out.println("IsoDiffractApp assignmentMethod = assignPeakTypesUsingOperations for hklType " 
//				+ (hklType == HKL_TYPE_PARENT ? "parent" : "child"));
		double ptolerance = 0.01;
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
//			ParentZ test:  HKL is integral in conventional parent setting.
//			ParentA test:	HKL is systematically absent for at least one conventional parent
//			operation/centering in the conventional parent setting.
//			ChildA test:	HKL is systematically absent for at least one conventional child
//			operation/centering in the conventional child setting.
//
//			Color		ParentZ	ParentA	ChildA
//			GREEN(1)	T	    F	      -
//			RED(2)		T    	T	      -
//			BLUE(3)		F	    -	      F
//			ORANGE(4)	F	    -	      T

			boolean testZ = MathUtil.isIntegral3(pd.parentPeakHKL, ptolerance);
			Variables.SymopData op = (testZ ? variables.parentCell : variables.childCell)
					.getSystematicallAbsentOp(testZ ? pd.parentPeakHKL : pd.crystalPeakHKL);
			boolean testA = (op != null);

//			System.out.println(pd.parentPeakHKLString + "\t" + pd.crystalPeakHKLString
//					+ "\t" + testZ + " " + testA);
			if (testZ & !testA) {
				pd.setPeakType(PEAK_TYPE_PARENT_BRAGG, op); // GREEN
			} else if (testZ & testA) {
				pd.setPeakType(PEAK_TYPE_PARENT_SYSABS, op); // RED
			} else if (!testZ && !testA) {
				pd.setPeakType(PEAK_TYPE_CHILD_BRAGG, op); // BLUE
			} else {// !testZ && testA
				pd.setPeakType(PEAK_TYPE_CHILD_SYSABS, op); // ORANGE
			}
			if (op != null)
				showLine(35, p + " " + typeOf(pd.peakType) + " " + op.opXYZ, 
						20, MathUtil.roundVec00(op.vi), 25, pd.parentPeakHKLString, 15, pd.crystalPeakHKLString, 10, colorOf(pd.peakType));
			else
				showLine(35, p + " " + typeOf(pd.peakType), 20, "", 25, pd.parentPeakHKLString, 15, pd.crystalPeakHKLString, 10, colorOf(pd.peakType));
		}
	}				
	
	final static String linew = "                                   ";

	private void showLine(Object... parts) {
		String s = "";
		int w;
		for (int i = 0; i < parts.length;) {
			w = ((Number) parts[i++]).intValue();
			String p = (String) parts[i++];
			s += (p.length() > w ? p + " " : (p + linew).substring(0, w));
		}
		System.out.println(s);
	}

	/**
	 * Fallback to first attempt in 2023 to do something better than random,
	 * using additional information found in newer ISOVIS files, but not operation
	 * information. It is retained here for ISOVIZ files that might still be around
	 * that do not have such information.
	 */
	@Deprecated
	private void assignPeakTypesPrimitive() {
		System.out.println("IsoDiffractApp assignmentMethod = assignPeakTypesPrimitive");
		double[] conventionalHKL = new double[3];
		double[] primitiveHKL = new double[3];
		double ptolerance = 0.01;
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			double[] convChildHKL = pd.crystalPeakHKL;
			MathUtil.mat3mul(variables.childCell.conv2convParentTransposeP, convChildHKL, conventionalHKL);
			MathUtil.mat3mul(variables.parentCell.conv2primTransposeP, conventionalHKL, primitiveHKL);
			int itype;
			if (MathUtil.isIntegral3(primitiveHKL, ptolerance)) {
				itype = PEAK_TYPE_PARENT_BRAGG; // 1
			} else if (MathUtil.isIntegral3(conventionalHKL, ptolerance)) {
				itype = PEAK_TYPE_PARENT_SYSABS; // 2
			} else {
				MathUtil.mat3mul(variables.childCell.conv2primTransposeP, convChildHKL, primitiveHKL);
				if (MathUtil.isIntegral3(primitiveHKL, ptolerance)) {
					itype = PEAK_TYPE_CHILD_BRAGG; // 3
				} else if (MathUtil.isIntegral3(convChildHKL, ptolerance)) {
					itype = PEAK_TYPE_CHILD_SYSABS; // 4
				} else {
					itype = PEAK_TYPE_ERROR;
					System.err.println("IsoDiffractApp.assignPeakTypesPrimitive error for " + peakData[p].crystalPeakHKLString);
				}
			}
			pd.setPeakType(itype, null);
		}
	}

	/**
	 * fall back to randomizing for older ISOVIZ files
	 */
	@Deprecated
	private void assignPeakTypesByRandomizing() {
		System.out.println("IsoDiffractApp assignmentMethod = assignPeakTypesByRandomizing");
		double[] parentHKL = new double[3];
		double ptolerance = 0.01; 
		// Set the peak type to 1 for parent Bragg peaks, and 3 otherwise.
		// Determines whether or not a peak is a parent Bragg peak (h k l all integral)
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			// transform super hkl into parent hkl
			MathUtil.mat3mul(variables.childCell.conv2convParentTransposeP, peakData[p].crystalPeakHKL, parentHKL); 
			pd.setPeakType(PEAK_TYPE_CHILD_BRAGG, null); // 3
			if (MathUtil.isIntegral3(parentHKL, ptolerance)) {
				pd.setPeakType(PEAK_TYPE_PARENT_BRAGG, null); // 1
			}
		}
		// Save and zero all displacive, scalar and magnetic mode values
		double child0 = variables.getSetChildFraction(1);
		Random rval = new Random();
		variables.saveModeValues();
		// Randomize all GM1 mode values to remove them from the peak
		variables.randomizeGM1Values(rval);
		variables.recalcDistortion();
		recalcIntensities();
		// Calculate all peak intensities and set (now) zero-intensity Bragg peaks to type 2.
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			if ((pd.peakType == PEAK_TYPE_PARENT_BRAGG) && (Math.abs(pd.peakIntensity) < 0.00000001))
				pd.peakType = PEAK_TYPE_PARENT_SYSABS; // 2
		}
		// Now randomize all Non-GM1 mode values to remove them from the peaks
		variables.randomizeNonGM1Values(rval);
		variables.recalcDistortion();
		recalcIntensities();
		for (int p = 0; p < peakCount; p++) {
			PeakData pd = peakData[p];
			if ((pd.peakType == PEAK_TYPE_CHILD_BRAGG) && (Math.abs(peakData[p].peakIntensity) < 0.00000001))
				pd.peakType = PEAK_TYPE_CHILD_SYSABS; // 4
		}
		// Restore all displacement and scalar mode values to their original values.
		variables.restoreModeValues();
		variables.getSetChildFraction(child0);
		variables.recalcDistortion();
		recalcIntensities();
	}

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
		gui.reset();
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

	@Override
	protected BufferedImage getImage() {
		return gui.getImage();
	}

	@Override
	protected void handleButtonEvent(Object src) {
		gui.handleButtonEvent(src);
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

	@Override
	protected void updateViewOptions() {
		// n/a
	}

	private class IsoDiffractGUI {

		protected JLabel horizLabel, upperLabel, centerLabel, qrangeLabel, wavLabel, minLabel, maxLabel, fwhmLabel, zoomLabel,
				mouseoverLabel;

		/**
		 * Radio buttons
		 */
		protected JRadioButton parentButton, childButton, xrayButton, neutronButton, crystalButton, powderButton, bothButton,
				dButton, qButton, tButton;
		/**
		 * Text fields for inputting viewing region
		 */
		protected JTextField hHTxt, kHTxt, lHTxt, hVTxt, kVTxt, lVTxt, hOTxt, kOTxt, lOTxt, qTxt;
		protected JTextField wavTxt, minTxt, maxTxt, fwhmTxt, zoomTxt;

		protected RenderPanel rp;

		public void init() {
			buildControls();
			showControls();
			rp = new RenderPanel();
			rp.addKeyListener(frame.keyListener);
		}


		public void handleButtonEvent(Object src) {
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
				hklType = HKL_TYPE_PARENT;
				needsRecalc = true;
			} else if (src == childButton) {
				hklType = HKL_TYPE_CHILD;
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
				minTxt.setText(MathUtil.trim00(powderXMin));
				maxTxt.setText(MathUtil.trim00(powderXMax));
				fwhmTxt.setText(MathUtil.varToString(powderResolution, 3, 0));
			}
		}

		public BufferedImage getImage() {
			return rp.im;
		}

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
		}

		public void setHKL(double[] hklO, double[] hklH, double[] hklV) {
			hklO[0] = getTextValue(hOTxt, hklO[0], 2);
			hklO[1] = getTextValue(kOTxt, hklO[1], 2);
			hklO[2] = getTextValue(lOTxt, hklO[2], 2);
			hklH[0] = getTextValue(hHTxt, hklH[0], 2);
			hklH[1] = getTextValue(kHTxt, hklH[1], 2);
			hklH[2] = getTextValue(lHTxt, hklH[2], 2);
			hklV[0] = getTextValue(hVTxt, hklV[0], 2);
			hklV[1] = getTextValue(kVTxt, hklV[1], 2);
			hklV[2] = getTextValue(lVTxt, hklV[2], 2);
		}

		public void setZoom(double powderZoom, double dy) {
			double zoom = getTextValue(zoomTxt, powderZoom, 2);
			zoom += -dy / 100.0;
			if (zoom < 0)
				zoom = 0;
			zoomTxt.setText(MathUtil.trim00(zoom));
		}

		public void setMouseOverText(String mouseovertext) {
			mouseoverLabel.setText(mouseovertext);
		}

		public void repaint() {
			rp.repaint();
			rp.requestFocus();
		}

		public boolean frameResized() {
			if (rp == null)
				return false;
			rp.im = null;
			return true;
		}

		public void dispose() {
			rp.dispose();
			rp.removeKeyListener(frame.keyListener);
		}

		/**
		 * creates the components of the control panel
		 */
		protected void buildControls() {
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

			addBottomButtons(botControlPanel); // applyView

			controlPanel.add(topControlPanel);
			controlPanel.add(botControlPanel);

		}

		/**
		 * loads the control components into the control panel
		 */
		protected void showControls() {

			// powder-only

			boolean bPowder = (isBoth && !wasPowder || isPowder && !isBoth);
			wasPowder = false;
			wavTxt.setVisible(bPowder);
			minTxt.setVisible(bPowder);
			maxTxt.setVisible(bPowder);
			fwhmTxt.setVisible(bPowder);
			zoomTxt.setVisible(bPowder);
			wavLabel.setVisible(bPowder);
			minLabel.setVisible(bPowder);
			maxLabel.setVisible(bPowder);
			fwhmLabel.setVisible(bPowder);
			zoomLabel.setVisible(bPowder);

			dButton.setVisible(bPowder && !isMouseOver);
			qButton.setVisible(bPowder && !isMouseOver);
			tButton.setVisible(bPowder && !isMouseOver);

			// crystal-only

			hOTxt.setVisible(!bPowder);
			kOTxt.setVisible(!bPowder);
			lOTxt.setVisible(!bPowder);
			hHTxt.setVisible(!bPowder);
			kHTxt.setVisible(!bPowder);
			lHTxt.setVisible(!bPowder);
			hVTxt.setVisible(!bPowder);
			kVTxt.setVisible(!bPowder);
			lVTxt.setVisible(!bPowder);
			qTxt.setVisible(!bPowder);

			horizLabel.setVisible(!bPowder);
			upperLabel.setVisible(!bPowder);
			centerLabel.setVisible(!bPowder);
			qrangeLabel.setVisible(!bPowder);

			parentButton.setVisible(!bPowder && !isMouseOver);
			childButton.setVisible(!bPowder && !isMouseOver);

			// mouseOver-only

			mouseoverLabel.setVisible(isMouseOver);

			// no-mouseOver-only

			xrayButton.setVisible(!isMouseOver);
			neutronButton.setVisible(!isMouseOver);
			crystalButton.setVisible(!isMouseOver);
			powderButton.setVisible(!isMouseOver);
			bothButton.setVisible(!isMouseOver);
			colorBox.setVisible(!isMouseOver && variables.needColorBox);
		}

	}

	public static void main(String[] args) {
		if (args != null && String.join(";",args).toLowerCase().indexOf("-assignprimitive") >= 0)
			usePrimitiveAssignmentMethod = true;
		create("IsoDiffract", args);
	}

	@Override
	protected void takeFocus() {
		gui.rp.requestFocusInWindow();
	}

	@Override
	protected boolean prepareToSwapIn() {
		return true;
	}



}
