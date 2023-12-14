/**
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
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JToggleButton;

import org.byu.isodistort.local.Elements;
import org.byu.isodistort.local.IsoApp;
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.MathUtil;

public class IsoDiffractApp extends IsoApp implements KeyListener, MouseMotionListener {

	/**
	 * An alternative panel to the RenderPanel3D of IsoDistortApp
	 * 
	 * @author hanso
	 *
	 */
	private static class RenderPanel extends JPanel {
		
		private IsoDiffractApp app;

		RenderPanel(IsoDiffractApp app) {
			this.app = app;
		}
		private BufferedImage im;

		@Override
		public void paint(Graphics gr) {
			super.paint(gr);
			int drawWidth = getWidth();
			int drawHeight = getHeight();
			if (im == null)
				im = (BufferedImage) createImage(drawWidth, drawHeight);
			Graphics g = im.getGraphics();
			app.render(g);
			g.dispose();
			gr.drawImage(im, 0, 0, drawWidth, drawHeight, this);
		}

	}

	private static final Color[] colorMap = new Color[] { Color.PINK, Color.GREEN, Color.RED, Color.BLUE,
			Color.ORANGE };

	// Variables that the user may want to adjust
	/** Maximum number of diffraction peaks and tick marks in the display */
	private int maxPeaks = 1000, powderMaxVisiblePeaks = 200, maxTicks = 100;
	/** Axis & tick line thickness, tick length */
	private double lineThickness = 2, normalTickLength = 10;
	/** Heights of powder display components */
	private final double powderAxisYOffset = 30, powderStickYOffset = 60, powderScaleAreaHeight = 80,
			powderLabelOffset = 5;

	private final double shortPowderAxisYOffset = 22, shortPowderStickYOffset = 22, shortPowderScaleAreaHeight = 30,
			shortTickLength = 5;

	private int shortPowderHeight = 100;

	/** Factor that determines the lowest observable intensities */
	double logfac = 4.0;
	/** Number of points in the powder pattern */
	int powderXRange = 1024;
	/** Anisotropic thermal parameter for simulating high-q intensity decay */
	double uiso = 0.15;

	private final static int POWDER_PATTERN_TYPE_2THETA = 1;
	private final static int POWDER_PATTERN_TYPE_DSPACE = 2;
	private final static int POWDER_PATTERN_TYPE_Q = 3;
	
	// Other global variables.
	int hklType = 1;
	/** Horizontal axis choice for powder pattern: (1) 2theta, (2) d-space, (3) q */
	int powderPatternType = POWDER_PATTERN_TYPE_2THETA;
	/** X-ray vs neutron diffraction */
	public boolean isXray = true;
	/** contains x-ray and neutron scattering factors for each element */
	/** half the display width in pixels */
	double drawHalfWidth;
	double drawHalfHeight;
	/** True means to make all atoms of the same element the same color */
	boolean isSimpleColor;

	boolean isPowder;
	boolean isBoth;
	boolean wasPowder; // for entry to "both"

	boolean isMouseOver;
	
	/** number of superHKL peaks contained within the display */
	int peakCount;
	/** List of peak multiplicities for powder pattern */
	double[] peakMultiplicities = new double[maxPeaks];
	/**
	 * List of dinverse values for peaks in either single-crystal or powder display
	 */
	double[] peakDInv = new double[maxPeaks];
	/** List of types of peaks contained within the display */
	int[] peakColor = new int[maxPeaks];

	/** unstrained dinverse-space metric tensor */
	double[][] metric0 = new double[3][3];
	/** strained dinverse-space metric tensor */
	double[][] metric = new double[3][3];
	/** transforms superHKL to properly-rotated XYZ cartesian */
	double[][] slatt2rotcart = new double[3][3];
	/** transforms properly-rotated XYZ cartesian to superHKL */
	double[][] rotcart2slatt = new double[3][3];


	/** distance (in pixels) to the nearest peak from the center */
	double crystalNearestDistanceToOrigin;
	/** maximum peak radius */
	double crystalMaxPeakRadius;	/** The hkl horizonal direction, upper direction, and center, */
	double[] crystalHklCenter = new double[3];
	/** number of single-crystal tickmarks to be displayed */
	int[] crystalTickCount = new int[2];
	/** List of tickmark supHKLs to be displayed */
	double[][][] crystalTickHKL = new double[2][maxTicks][3];
	/** List of tickmark (X1Y1, X2Y2) coords to be displayed */
	double[][][] crystalTickXY2 = new double[2][maxTicks][4];
	/** List of superHKLs contained within the display */
	double[][] crystalPeakHKL = new double[maxPeaks][3];
	/** List of peak XY coords contained within the single-crystal display */
	double[][] crystalPeakXY = new double[maxPeaks][2];
	/** List of peak radii contained within the single-crystal display */
	double[] crystalPeakRadius = new double[maxPeaks];
	/** Plot axes for crystal display */
	double[][] crystalAxesXYDirXYLen = new double[2][5];
	/** radius of the range of Q/2Pi contained within the crystal display */
	double crystalDInvRange;
	/** The hkl horizonal and upper directions, */
	double[][] crystalHkldirections = new double[2][3];
	

	/** List of positions of powder peaks in either 2th, d or q units */
	double[] powderPeakY = new double[maxPeaks];
	/** List of positions of powder peaks in scaled pixel units */
	double[] powderPeakX = new double[maxPeaks];
	/** List of peak radii contained within the single-crystal display */
	double[] peakIntensity = new double[maxPeaks];
	/** Largest powder peak multiplicity */
	double powderScaleFactor; // initial value of 1
	/** Number of powder tick marks and tick labels to be displayed */
	int powderAxisTickCount;
	/** List of powder tick marks to be displayed in window pixel units */
	double[] powderAxisTickX = new double[maxTicks];
	/** List of powder tick labels to be displayed in window pixel units */
	String[] powderAxisTickLabel = new String[maxTicks];
	/** wavelength for computing 2theta scale in the powder display */
	double powderWavelength;
	/** horizontal range minimum value in powder display */
	double powderXMin;
	/** horizontal range maximum value in powder display */
	double powderXMax;
	/** horizontal resolution in the powder display */
	double powderResolution;
	/** vertical zoom factor in the powder display */
	double powderZoom;
	/**
	 * dinvmin and dinvmax are the maximum and minimum values in the powder display
	 * in d-inverse units
	 */
	double powderDinvmin, powderDinvmax, powderDinvres;
	/** the array that holds the powder pattern */
	double[] powderY = new double[powderXRange];

	JLabel horizLabel, upperLabel, centerLabel, qrangeLabel, wavLabel, minLabel, maxLabel, fwhmLabel, zoomLabel,
			mouseoverLabel;
	JCheckBox colorBox;
	/** Radio buttons */
	JRadioButton parentButton, superButton, xrayButton, neutronButton, crystalButton, powderButton, bothButton, dButton,
			qButton, tButton;
	/** Radio button groups -- only one element from a group can be selected */
	ButtonGroup xnButtons, hklButtons, pscButtons, rangeButtons;
	
	/** Text fields for inputting viewing region */
	JTextField hHTxt, kHTxt, lHTxt, hVTxt, kVTxt, lVTxt, hOTxt, kOTxt, lOTxt, qTxt;
	JTextField wavTxt, minTxt, maxTxt, fwhmTxt, zoomTxt;

	private RenderPanel rp;

	public IsoDiffractApp() {
		super(APP_ISODIFFRACT);
	}

	@Override
	protected void init() {
		initializePanels();
		drawPanel.addKeyListener(this);
		drawPanel.addMouseMotionListener(this);
		setVariables(readFile());
	}
	
	@Override
	protected void dispose() {
		drawPanel.removeKeyListener(this);
		drawPanel.removeMouseMotionListener(this);
		super.dispose();
	}

	@Override
	protected boolean setVariables(String dataString) {
		try {
			if (variables == null) {
				variables = new Variables(this, dataString, true);
				variables.initPanels(sliderPanel, controlPanel);
//				sliderPanel.setBackground(Color.BLACK);
			}
			variables.updateForApp();
			buildControls();
			showControls();
		} catch (Exception e) {
			JOptionPane.showMessageDialog(frameContentPane, "Error reading input data " + e.getMessage());
			return false;
		}
		return true;
	}

	@Override
	protected void setRenderer() {
		drawHalfWidth = drawWidth / 2;
		drawHalfHeight = drawHeight / 2;
		rp = new RenderPanel(this);
		rp.setPreferredSize(drawPanel.getSize());
		rp.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add(rp);
	}

	@Override
	public void updateDisplay() {
		if (isAdjusting)
			return;
		if (needsRecalc || variables.isChanged) {
			isAdjusting = true;
			variables.readSliders();
			if (isBoth || isPowder)
				resetPowderPeaks();
			if (isBoth || !isPowder)
				resetCrystalPeaks();
			needsRecalc = false;
			variables.isChanged = false;
			drawPanel.repaint();
			drawPanel.requestFocus();
			isAdjusting = false;
		}

	}

// BH now taken care of by RenderPanel	
//	public void paint(Graphics gr) {
//	super.paint(gr);
//}

	/**
	 * From RenderPanel.paint(Graphics).
	 * 
	 * @param g
	 */
    void render(Graphics g) {
		g.setColor(Color.BLACK);
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
		for (int index = 0; index < peakCount; index++) {
			drawCrystalCircle(gr, crystalPeakXY[index][0], crystalPeakXY[index][1], crystalPeakRadius[index],
					crystalMaxPeakRadius, peakColor[index]);
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
		for (int i = 0; i < 2; i++) {
			drawDash(gr, crystalAxesXYDirXYLen[i][0], crystalAxesXYDirXYLen[i][1], crystalAxesXYDirXYLen[i][2], crystalAxesXYDirXYLen[i][3],
					crystalAxesXYDirXYLen[i][4], lineThickness, 0);
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
		double fy =  0.8 * (isBoth ? 1d * (shortPowderHeight - shortPowderScaleAreaHeight) / (drawHeight - powderScaleAreaHeight) : 1) * powderZoom / powderScaleFactor;
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
		drawDash(gr, drawHalfWidth, drawHeight - axisYOffset, 1, 0, drawHalfWidth, lineThickness, 0);
		for (int peakcolor = 4; peakcolor > 0; peakcolor--) {
			// draw one color at at time with
			// red and green last and on top
			for (int p = 0; p < peakCount; p++) {
				if (peakColor[p] == peakcolor) {
					drawDash(gr, powderPeakX[p], y, 0, 1, tickLength, lineThickness, peakcolor);
				}
			}
		}
		int ytick = (int) (drawHeight - powderLabelOffset);
		for (int n = 0; n < powderAxisTickCount; n++) {
			drawDash(gr, powderAxisTickX[n], drawHeight - axisYOffset, 0, 1, tickLength, lineThickness, 0);
			gr.drawString(powderAxisTickLabel[n], (int) powderAxisTickX[n] - 15, ytick);
		}
		// }
	}

	private static void drawDash(Graphics gr, double cx, double cy, double dirx, double diry, double halflength,
			double thickness, int color) {
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
	 * @param radius is the radius of tolerance for mouseover on a peaks
	 * @return which peak is under the mouse
	 */
	private void mousePeak(double x, double y) {

		double tol2 = (isPowder ? 2*2 : 6*6);
		int currentpeak = -1, currentcolor = 5;
		String mouseovertext, valuestring = "", specifictext = "";
		boolean isPowder = (this.isPowder || isBoth && y > drawHeight - shortPowderHeight);
		for (int p = 0; p < peakCount; p++) {
			if (isPowder) {
				if (approxEqual(x, powderPeakX[p], tol2)
						&& (Math.abs(y - (drawHeight - powderStickYOffset)) < 1.25 * normalTickLength)
						&& (peakColor[p] < currentcolor)) {
					currentpeak = p;
					currentcolor = peakColor[p];
					valuestring = Variables.varToString(powderPeakY[p], 2, -2);
					switch (powderPatternType) {
					case POWDER_PATTERN_TYPE_2THETA:
						specifictext = "2\u0398 = " + valuestring + " Degrees";
						break;
					case POWDER_PATTERN_TYPE_DSPACE:
						specifictext = "d = " + valuestring + " Angstroms";
						break;
					case POWDER_PATTERN_TYPE_Q:
						specifictext = "q = " + valuestring + " 1/Angstroms";
						break;
					}
				}
			} else {
				double dx = x - crystalPeakXY[p][0];
				double dy = y - crystalPeakXY[p][1];
				if (dx * dx + dy * dy <= tol2) {
					currentpeak = p;
					specifictext = "";
					break;
				}
			}
		}
		if (currentpeak >= 0) {
			double[] hklp = new double[3];
			double[] hkls = new double[3];
			for (int n = 0; n < 3; n++)
				hkls[n] = crystalPeakHKL[currentpeak][n];
			MathUtil.matdotvect(variables.Tmat, hkls, hklp);

			mouseovertext = "Parent HKL = (" + Variables.varToString(hklp[0], 2, -2) + ", "
					+ Variables.varToString(hklp[1], 2, -2) + ", " + Variables.varToString(hklp[2], 2, -2)
					+ ")       Super HKL = (" + Variables.varToString(hkls[0], 2, -2) + ", "
					+ Variables.varToString(hkls[1], 2, -2) + ", " + Variables.varToString(hkls[2], 2, -2) + ")     "
					+ specifictext;
			isMouseOver = true;
		} else {
			mouseovertext = "";
			isMouseOver = false;
		}
		mouseoverLabel.setText(mouseovertext);
		showControls();
	}

	/**
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 * 2 These methods do the diffraction-related calculations 2
	 * 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 */

	/**
	 * Recalculates strain info (e.g. metric tensor and slatt2rotcart). Called by
	 * resetCrystalPeaks, resetPowerPeaks, recalcCrystal, recalcPowder.
	 */
	private void recalcStrainstuff() {
		double[][] tempmat = new double[3][3];
		double[][] slatt2cart = new double[3][3]; // transforms superHKL to XYZ cartesian
		double[][] rotmat = new double[3][3];// Rotates cartesian q-space so as to place axes1 along +x and axes2 in +y
												// hemi-plane

		// Determine the unstrained metric tensor
		MathUtil.matinverse(variables.sBasisCart0, tempmat);
		MathUtil.mattranspose(tempmat, slatt2cart);// B* = Transpose(Inverse(B))
		MathUtil.matdotmat(tempmat, slatt2cart, metric0); // G* = Transpose(B*).(B*)

		// Determine the new metric tensor
		MathUtil.matinverse(variables.sBasisCart, tempmat);
		MathUtil.mattranspose(tempmat, slatt2cart);// B* = Transpose(Inverse(B))
		MathUtil.matdotmat(tempmat, slatt2cart, metric); // G* = Transpose(B*).(B*)

		// Create an orthonormal rotation matrix that moves axis 1 to the +x direction,
		// while keeping axis 2 in the +y quadrant. First transform both axes into
		// cartesian
		// coords. Then take 1.cross.2 to get axis 3, and 3.cross.1 to get the new axis
		// 2.
		// Normalize all three and place them in the rows of the transformation matrix.
		MathUtil.matdotvect(slatt2cart, crystalHkldirections[0], tempmat[0]);
		MathUtil.matdotvect(slatt2cart, crystalHkldirections[1], tempmat[1]);
		MathUtil.mycross(tempmat[0], tempmat[1], tempmat[2]);
		MathUtil.mycross(tempmat[2], tempmat[0], tempmat[1]);
		MathUtil.normalize(tempmat[0]);
		MathUtil.normalize(tempmat[1]);
		MathUtil.normalize(tempmat[2]);
		MathUtil.matcopy(tempmat, rotmat);

		// Combine the rotation and the cartesian conversion to get the overall superHKL
		// to cartesian transformation.
		MathUtil.matdotmat(rotmat, slatt2cart, slatt2rotcart);

		// Invert to get the overall cartesian to superHKL tranformation.
		MathUtil.matinverse(slatt2rotcart, rotcart2slatt);

	}

	/**
	 * Called by recalcCrystal, recalcPowder, assignPeakTypes.
	 */
	public void recalcIntensities() {
		double zzzNR, zzzNI, pppNR, pppNI, scatNR, scatNI, scatM;
		double[] zzzM = new double[3], pppM = new double[3];
		double[] qhat = new double[3], mucart = new double[3], supxyz = new double[3];
		double phase, thermal;
		double[] atomScatFac = new double[2];
		double Intensity000; // The total scattering factor of the unit cell

		for (int p = 0; p < peakCount; p++) {
			zzzNR = 0;
			zzzNI = 0;
			pppNR = 0;
			pppNI = 0;
			for (int i = 0; i < 3; i++) {
				zzzM[i] = 0;
				pppM[i] = 0;
			}
			MathUtil.matdotvect(variables.sBasisCart, crystalPeakHKL[p], qhat);
			MathUtil.normalize(qhat);
			double d = 2 * Math.PI * peakDInv[p];
			thermal = Math.exp(-0.5 * uiso * d * d);

			for (int t = 0; t < variables.numTypes; t++)
				for (int s = 0; s < variables.numSubTypes[t]; s++)
					for (int a = 0; a < variables.numSubAtoms[t][s]; a++) {
						for (int i = 0; i < 3; i++) {
							supxyz[i] = variables.atomFinalCoord[t][s][a][i];
						}
						if (supxyz[0] >= 0 && supxyz[0] < 1 && supxyz[1] >= 0 && supxyz[1] < 1 && supxyz[2] >= 0
								&& supxyz[2] < 1) {
							// just [atomicNumber, 0] for xray
							atomScatFac = Elements.getScatteringFactor(variables.atomTypeSymbol[t], isXray);
							phase = 2 * (Math.PI) * MathUtil.dot(crystalPeakHKL[p], supxyz);
							scatNR = variables.atomFinalOcc[t][s][a] * atomScatFac[0];
							scatNI = variables.atomFinalOcc[t][s][a] * atomScatFac[1];
							zzzNR += variables.atomInitOcc[t][s][a] * atomScatFac[0];
							zzzNI += variables.atomInitOcc[t][s][a] * atomScatFac[1];
							pppNR += scatNR * Math.cos(phase) - scatNI * Math.sin(phase);
							pppNI += scatNI * Math.cos(phase) + scatNR * Math.sin(phase);
//	        				System.out.format("t:%d,s:%d,a:%d, pos:(%.2f,%.2f,%.2f), scatNR/NI:%.3f/%.3f, phase:%.3f%n", t, s, a, supxyz[0], supxyz[1], supxyz[2], scatNR, scatNI, phase);
							// remember that magnetic mode vectors (magnetons/Angstrom) were predefined to
							// transform this way.
							MathUtil.matdotvect(variables.sBasisCart, variables.atomFinalMag[t][s][a], mucart);
							if (isXray) {
								scatM = 0.0;
								for (int i = 0; i < 3; i++)
									zzzM[i] += 0;
							} else {
								scatM = variables.atomFinalOcc[t][s][a] * (5.4);
								for (int i = 0; i < 3; i++)
									zzzM[i] += variables.atomInitOcc[t][s][a] * (5.4) * mucart[i];
							}
							for (int i = 0; i < 3; i++)
								pppM[i] += scatM * (mucart[i] - MathUtil.dot(mucart, qhat) * qhat[i]) * Math.cos(phase);
						}
					}
			Intensity000 = zzzNR * zzzNR + zzzNI * zzzNI + MathUtil.lenSq3(zzzM);
			peakIntensity[p] = thermal * (pppNR * pppNR + pppNI * pppNI + MathUtil.lenSq3(pppM)) 
					/ Intensity000;
		}
	}

	/**
	 * Recalculates peak positions and intensities for single-crystal pattern.
	 * Called by resetCrystalPeaks and run().
	 */
	private void recalcCrystal() {
		double[] tempvec0 = new double[3], tempvec = new double[3];

		// update the structure based on current slider values
		variables.recalcDistortion();

		recalcStrainstuff();

		// update the peak cartesian coordinates
		for (int p = 0; p < peakCount; p++) {
			MathUtil.vecadd(crystalPeakHKL[p], 1.0, crystalHklCenter, -1.0, tempvec0);
			MathUtil.matdotvect(slatt2rotcart, tempvec0, tempvec);
			double x = tempvec[0] * (drawHalfWidth / crystalDInvRange) + drawHalfWidth;
			double y = -tempvec[1] * (drawHalfHeight / crystalDInvRange) + drawHalfHeight; // minus sign turns the picture
																					// upside right.
			crystalPeakXY[p][0] = x;
			crystalPeakXY[p][1] = y;
//			System.out.println("peak: "+p+", "+peaktypeList[p]+", supHKL: ("+peakhklList[p][0]+" "+peakhklList[p][1]+" "+peakhklList[p][2]+"), Cart: ("+tempvec[0]/dinvrange+" "+tempvec[1]/dinvrange+" "+tempvec[2]+"), Int: "+peakintList[p]);
		}

		// Update the dinverse list
		for (int p = 0; p < peakCount; p++) {
			MathUtil.matdotvect(metric, crystalPeakHKL[p], tempvec);
			peakDInv[p] = Math.sqrt(MathUtil.dot(crystalPeakHKL[p], tempvec));
		}

		// Update the axis and tickmark plot parameters
		for (int n = 0; n < 2; n++) // cycle over the two axes
		{
			MathUtil.matdotvect(slatt2rotcart, crystalHkldirections[n], tempvec);
			MathUtil.normalize(tempvec);
			double dirX = tempvec[0];
			double dirY = tempvec[1];
			double slope = -1;
			double axisLength;
			crystalAxesXYDirXYLen[n][0] = drawHalfWidth;
			crystalAxesXYDirXYLen[n][1] = drawHalfHeight;
			crystalAxesXYDirXYLen[n][2] = dirX;
			crystalAxesXYDirXYLen[n][3] = -dirY; // minus sign turns the picture upside right.
			if (Math.abs(dirX) > 0.001) {
				slope = Math.abs(dirY / dirX);
				if (slope < 1)
					axisLength = drawHalfWidth / Math.abs(dirX);
				else
					axisLength = drawHalfHeight / Math.abs(dirY);
			} else {
				axisLength = drawHalfWidth;
			}
			crystalAxesXYDirXYLen[n][4] = axisLength;

			for (int m = 0; m < crystalTickCount[n]; m++) {
				MathUtil.matdotvect(slatt2rotcart, crystalTickHKL[n][m], tempvec);
				double x = tempvec[0] * (drawHalfWidth / crystalDInvRange) + drawHalfWidth;
				double y = -tempvec[1] * (drawHalfHeight / crystalDInvRange) + drawHalfHeight; // minus sign turns the picture
																						// upside right.
				// z = tempvec[2] * (drawHalfWidth / crystalDInvRange); // BH I guess...
				crystalTickXY2[n][m][0] = x;
				crystalTickXY2[n][m][1] = y;
				crystalTickXY2[0][m][2] = crystalAxesXYDirXYLen[1][2];
				crystalTickXY2[0][m][3] = crystalAxesXYDirXYLen[1][3];
				crystalTickXY2[1][m][2] = crystalAxesXYDirXYLen[0][2];
				crystalTickXY2[1][m][3] = crystalAxesXYDirXYLen[0][3];
				// if (z == 0) {
				// } // No need for Z now, but might use later.
//				System.out.println("Axis: "+n+", Tick: "+m+", supHKL: "+tickhklList[n][m][0]+" "+tickhklList[n][m][1]+" "+tickhklList[n][m][2]+", Cart: "+X+" "+Y+" "+Z);
			}
		}

		// update the peak intensities based on current structure
		recalcIntensities();

		// update the logarithmic peak radii
		for (int p = 0; p < peakCount; p++)
			crystalPeakRadius[p] = crystalMaxPeakRadius * (Math.max(Math.log(peakIntensity[p]) / 2.3025, -logfac) + logfac)
					/ logfac;

	}

	/**
	 * Creates the list of single-crystal peaks and their types Called by init() and
	 * run().
	 */
	public void resetCrystalPeaks() {
		double[] hklH = new double[3], hklV = new double[3], hklO = new double[3];
		double[] superhkl = new double[3], superhklcart = new double[3];
		double tempscalar, tempmin, tempmax, mag, inplanetest;
		double[] tempvec0 = new double[3], tempvec = new double[3], uvw = new double[3];
		double[][] limits = new double[8][3]; // HKL search limits
		double[][] slatt2platt = new double[3][3]; // transforms superHKL to parentHKL
		double[][] platt2slatt = new double[3][3]; // transforms parentHKL to superHKL
		double ztolerance = 0.001; // z-axis tolerance that determines whether or not a peak is in the display
									// plane

		boolean rangeQ, planeQ;
		int[][] hklrange = new int[3][2];
		int tempint, count;

		MathUtil.matcopy(variables.Tmat, slatt2platt);
		MathUtil.matinverse(slatt2platt, platt2slatt);

		hklO[0] = Double.parseDouble(hOTxt.getText());
		hklO[1] = Double.parseDouble(kOTxt.getText());
		hklO[2] = Double.parseDouble(lOTxt.getText());
		hklH[0] = Double.parseDouble(hHTxt.getText());
		hklH[1] = Double.parseDouble(kHTxt.getText());
		hklH[2] = Double.parseDouble(lHTxt.getText());
		hklV[0] = Double.parseDouble(hVTxt.getText());
		hklV[1] = Double.parseDouble(kVTxt.getText());
		hklV[2] = Double.parseDouble(lVTxt.getText());

		crystalDInvRange = Double.parseDouble(qTxt.getText()) / (2 * Math.PI);

		// Decide that user input is either of parentHKL or superHKL type
		// Either way, the input directions are passed as superHKL vectors.
		// Note that the platt2slatt matrix is slider-bar independent.
		if (hklType == 2) {
			MathUtil.copy(hklH, crystalHkldirections[0]);
			MathUtil.copy(hklV, crystalHkldirections[1]);
			MathUtil.copy(hklO, crystalHklCenter);
		} else if (hklType == 1) {
			MathUtil.matdotvect(platt2slatt, hklH, crystalHkldirections[0]);
			MathUtil.matdotvect(platt2slatt, hklV, crystalHkldirections[1]);
			MathUtil.matdotvect(platt2slatt, hklO, crystalHklCenter);
		}

		// Identify the direct-space direction perpendicular to display plane.
		MathUtil.mycross(crystalHkldirections[0], crystalHkldirections[1], uvw);

		variables.readSliders(); // Get the latest strain information
		variables.recalcDistortion(); // Update the distortion
		recalcStrainstuff(); // Get updated slatt2rotcart transformation

		// Find out the superHKL range covered within the Qrange specified.
		MathUtil.set(tempvec0, crystalDInvRange, 0, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[0]);

		MathUtil.set(tempvec0, -crystalDInvRange, 0, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[1]);

		MathUtil.set(tempvec0, 0, crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[2]);

		MathUtil.set(tempvec0, 0, -crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[3]);

		MathUtil.set(tempvec0, crystalDInvRange, crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[4]);

		MathUtil.set(tempvec0, -crystalDInvRange, crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[5]);

		MathUtil.set(tempvec0, crystalDInvRange, -crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[6]);

		MathUtil.set(tempvec0, -crystalDInvRange, -crystalDInvRange, 0);
		MathUtil.matdotvect(rotcart2slatt, tempvec0, tempvec);
		MathUtil.vecadd(tempvec, 1.0, crystalHklCenter, 1.0, limits[7]);

		for (int ii = 0; ii < 3; ii++) {
			tempmin = 1000;
			tempmax = -1000;
			for (int nn = 0; nn < 8; nn++) {
				if (limits[nn][ii] > tempmax)
					tempmax = limits[nn][ii];
				if (limits[nn][ii] < tempmin)
					tempmin = limits[nn][ii];
			}
			hklrange[ii][0] = (int) Math.floor(tempmin);
			hklrange[ii][1] = (int) Math.ceil(tempmax);
		}
//		for (int nn=0; nn<8; nn++)
//			System.out.println(nn+" ("+limits[nn][0]+" "+limits[nn][1]+" "+limits[nn][2]+")");
//		System.out.println("H: "+hklrange[0][0]+" "+hklrange[0][1]);
//		System.out.println("K: "+hklrange[1][0]+" "+hklrange[1][1]);
//		System.out.println("L: "+hklrange[2][0]+" "+hklrange[2][1]);

		// Identify the peaks to display.
		peakCount = 0;
		crystalNearestDistanceToOrigin = drawHalfWidth;
		for (int H = hklrange[0][0]; H <= hklrange[0][1]; H++)
			for (int K = hklrange[1][0]; K <= hklrange[1][1]; K++)
				for (int L = hklrange[2][0]; L <= hklrange[2][1]; L++) {
					planeQ = false;
					rangeQ = false;
					MathUtil.set(superhkl, H, K, L);
					MathUtil.vecadd(superhkl, 1.0, crystalHklCenter, -1.0, tempvec0);
					MathUtil.matdotvect(slatt2rotcart, tempvec0, superhklcart);

					inplanetest = Math.abs(MathUtil.dot(tempvec0, uvw));
					if (inplanetest < ztolerance)
						planeQ = true; // HKL point lies in the display plane
					if ((Math.abs(superhklcart[0]) < crystalDInvRange)
							&& (Math.abs(superhklcart[1]) < crystalDInvRange))
						rangeQ = true; // HKL point lies in q range of display
					if (planeQ && rangeQ) {
						// Save the XY coords of a good peak.
						MathUtil.copy(superhkl, crystalPeakHKL[peakCount]);
						peakMultiplicities[peakCount] = 1;

						tempscalar = (superhklcart[0] * superhklcart[0]
								 + superhklcart[1] * superhklcart[1])
								* (drawHalfWidth / crystalDInvRange);
						if ((Math.abs(tempscalar) > 0.1) && (tempscalar < crystalNearestDistanceToOrigin))
							crystalNearestDistanceToOrigin = tempscalar;

//						System.out.println(peaknum+", "+peaktypeList[peaknum]+", sHKL=("+superhklvec[0]+" "+superhklvec[1]+" "+superhklvec[2]+"), pHKL=("+parenthklvec[0]+" "+parenthklvec[1]+" "+parenthklvec[2]+"), xyz=("+tempvec[0]+" "+tempvec[1]+" "+tempvec[2]+"), int="+peakintList[peaknum]);
						peakCount++;
					}
				}

		// Set the max peak radius
		crystalMaxPeakRadius = Math.min(crystalNearestDistanceToOrigin / 2, 40);

		// Identify the tickmark locations along axis0.
		for (int n = 0; n < 2; n++) // cycle over the two axes
		{
			MathUtil.matdotvect(slatt2rotcart, crystalHkldirections[n], tempvec);
			mag = MathUtil.len3(tempvec);
			tempint = (int) Math.floor(crystalDInvRange / mag);
			crystalTickCount[n] = 2 * tempint + 1;
			count = 0;
			for (int m = -tempint; m < tempint + 1; m++) {
				for (int i = 0; i < 3; i++)
					crystalTickHKL[n][count][i] = m * crystalHkldirections[n][i];
				count++;
			}
		}

		assignPeakTypes();
		recalcCrystal(); // recalculate intensities and positions
	}

	/**
	 * Recalculates structural parameters and peak positions and intensities for
	 * powder pattern. Called by resetPowderPeaks and run().
	 */
	private void recalcPowder() {

		// update the structure based on current slider values
		variables.recalcDistortion();

		// update the peak positions and values
		recalcStrainstuff();

		recalcPowderPeakPositionsAndValues();
		// update the peak intensities based on current structure
		recalcIntensities();
		recalcPowderPattern();
	}

	private void recalcPowderPeakPositionsAndValues() {
		double[] tempvec = new double[3];
		for (int p = 0; p < peakCount; p++) {
			MathUtil.matdotvect(metric, crystalPeakHKL[p], tempvec);
			double dinv = Math.sqrt(MathUtil.dot(crystalPeakHKL[p], tempvec));
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
		for (int p = 0; p < peakCount; p++) {
			double center = powderXRange * powderPeakX[p] / drawWidth;
			int left = Math.max((int) Math.floor(center - 5 * sigmapix), 0);
			int right = Math.min((int) Math.ceil(center + 5 * sigmapix), powderXRange - 1);
			for (int i = left; i <= right; i++) {
				double d = (i - center) / sigmapix;
				powderY[i] += Math.exp(-d * d / 2) * peakIntensity[p]
						* peakMultiplicities[p];
			}
		}
	}

	/**
	 * Creates the list of powder peaks and their types.
	 * 
	 * Called updateDisplay()
	 */
	private void resetPowderPeaks() {
		int limH, limK, limL;
		double dinv0, dinv1, dinv2, metricdet, tol = 0.0001;
		double[] dinvlist0 = new double[maxPeaks];// unstrained list of dinverse values
		double[] dinvlist1 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] dinvlist2 = new double[maxPeaks];// randomly strained list of dinverse values
		double[] superhkl = new double[3];
		//double[] parenthkl = new double[3];
		boolean createNewPeak = false, isXrayTemp;
		Random rval = new Random();
		double masterTemp;
		double[] strainTemp = new double[variables.strainmodeNum];
		double[][] randommetric1 = new double[3][3], randommetric2 = new double[3][3];
		double[] tempvec = new double[3];

		// Diagnostic code
		// double[][] slatt2platt = new double[3][3]; // transforms superHKL to
		// parentHKL
		// Vec.matcopy(variables.Tmat, slatt2platt);
		// for (int i = 0; i < 3; i++) {
		// for (int j = 0; j < 3; j++)
		// System.out.format("[%d][%d] = %.4f, ", i, j, slatt2platt[i][j]);
		// System.out.println("");
		// }

		powderWavelength = Double.parseDouble(wavTxt.getText());
		powderXMin = Double.parseDouble(minTxt.getText());
		powderXMax = Double.parseDouble(maxTxt.getText());
		powderResolution = Double.parseDouble(fwhmTxt.getText());
		powderZoom = Double.parseDouble(zoomTxt.getText());
		if ((powderXMax <= powderXMin) || (powderPatternType == POWDER_PATTERN_TYPE_DSPACE)
				&& ((Math.abs(powderXMin) < tol) || (Math.abs(powderXMax) < tol)))
			return;
		setPowderDinvMinMaxRes();
		
		// Save the old strain slider values
		variables.readSliders();
		masterTemp = variables.masterSliderVal;
		variables.masterSliderVal = 1;
		for (int m = 0; m < variables.strainmodeNum; m++)
			strainTemp[m] = variables.strainmodeSliderVal[m];

		// randomize the strains to avoid accidental degeneracies
		for (int m = 0; m < variables.strainmodeNum; m++)
			variables.strainmodeSliderVal[m] = (2 * rval.nextFloat() - 1);// *rd.strainmodeMaxAmp[m];
		variables.recalcDistortion();// Update the distortion parameters
		recalcStrainstuff(); // Build the randomized dinvmetric tensor
		MathUtil.matcopy(metric, randommetric1);

		// randomize the strains again to be extra careful
		for (int m = 0; m < variables.strainmodeNum; m++)
			variables.strainmodeSliderVal[m] = (2 * rval.nextFloat() - 1);// *rd.strainmodeMaxAmp[m];
		variables.recalcDistortion();// Update the distortion parameters
		recalcStrainstuff(); // Build the randomized dinvmetric tensor
		MathUtil.matcopy(metric, randommetric2);

		// restore the strains to their original values
		variables.masterSliderVal = masterTemp;
		for (int m = 0; m < variables.strainmodeNum; m++)
			variables.strainmodeSliderVal[m] = strainTemp[m];

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

		MathUtil.cross(metric0[0], metric0[1], tempvec);
		metricdet = MathUtil.dot(tempvec, metric0[2]); // Calculate the metric determinant
		limH = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[1][1] * metric0[2][2] - metric0[1][2] * metric0[1][2]) / metricdet)));
		limK = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[0][0] * metric0[2][2] - metric0[0][2] * metric0[0][2]) / metricdet)));
		limL = (int) Math.ceil(powderDinvmax
				* Math.sqrt(Math.abs((metric0[0][0] * metric0[1][1] - metric0[0][1] * metric0[0][1]) / metricdet)));
//	    System.out.println("Limits = ("+limH+","+limK+","+limL+")");

		double[][] tmat = variables.Tmat;
		peakCount = 0;
		for (int h = -limH; h <= limH; h++)
			for (int k = -limK; k <= limK; k++)
				for (int l = -limL; l <= limL; l++) {
					MathUtil.set(superhkl, h, k, l);

//					// Diagnostic code
//					Vec.matdotvect(slatt2platt, superhkl, parenthkl);
//					// Diagnostic code
//					double[] ttt = new double[3];
//					for (int i = 0; i < 3; i++) {
//						ttt[i] = Math.abs(parenthkl[i]);
//					}
//					for (int j = 0; j < 2; j++) {
//						for (int i = 0; i < 2; i++) {
//							if (ttt[i] > ttt[i + 1]) {
//								temp = ttt[i + 1];
//								ttt[i + 1] = ttt[i];
//								ttt[i] = temp;
//							}
//						}
//					}
					//
					// //Diagnostic code boolean diagnosticflag = Math.abs(ttt[0]-0)<tol &&
					// Math.abs(ttt[1]-0)<tol && Math.abs(ttt[2]-2)<tol; if (diagnosticflag)
					// System.out.
					// format("orig suphkl:(%.4f,%.4f,%.4f), parhkl:(%.4f,%.4f,%.4f),
					// ttt:(%.4f,%.4f,%.4f)%n"
					// , superhkl[0], superhkl[1], superhkl[2], parenthkl[0], parenthkl[1],
					// parenthkl[2], ttt[0], ttt[1], ttt[2]);

					// Generate the standard metric and two randomized metrics.
					MathUtil.matdotvect(metric0, superhkl, tempvec);
					dinv0 = Math.sqrt(MathUtil.dot(superhkl, tempvec));
					MathUtil.matdotvect(randommetric1, superhkl, tempvec);
					dinv1 = Math.sqrt(MathUtil.dot(superhkl, tempvec));
					MathUtil.matdotvect(randommetric2, superhkl, tempvec);
					dinv2 = Math.sqrt(MathUtil.dot(superhkl, tempvec));

					boolean isinrange = (powderDinvmin <= dinv0) && (dinv0 <= powderDinvmax);
					if (isinrange) {
						createNewPeak = true;
						for (int p = 0; p < peakCount; p++) {
							boolean isrobustlycoincident = approxEqual(dinv0, dinvlist0[p], tol)
									&& approxEqual(dinv1, dinvlist1[p], tol) 
									&& approxEqual(dinv2, dinvlist2[p], tol);
							if (isrobustlycoincident) {
								boolean isequivalent = checkPowderPeakEquiv(superhkl, crystalPeakHKL[p], tmat);
								if (isequivalent) {
									//
									// //Diagnostic code if (diagnosticflag) { Vec.matdotvect(slatt2platt,
									// peakhklList[p], tempvec);
									// System.out.format("comp suphkl:(%.4f,%.4f,%.4f), parhkl:(%.4f,%.4f,%.4f)%n",
									// peakhklList[p][0], peakhklList[p][1], peakhklList[p][2], tempvec[0],
									// tempvec[1], tempvec[2]); System.out.println(""); }
									//
									boolean isnicer = comparePowderHKL(superhkl, crystalPeakHKL[p]);
									if (isnicer) {
										MathUtil.copy(superhkl, crystalPeakHKL[p]);
									}
									peakMultiplicities[p] += 1;
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
							peakMultiplicities[peakCount] = 1;
							peakCount++;
							if (peakCount >= maxPeaks)
								return;
						}
					}
				}

		sortPowderPeaks(dinvlist0, tol);
		// keep only a practical number of peaks
		if (peakCount > powderMaxVisiblePeaks)
			peakCount = powderMaxVisiblePeaks;

		masterTemp = variables.masterSliderVal;
		variables.masterSliderVal = 0;
		isXrayTemp = isXray;
		isXray = true;
		recalcPowder();
		isXray = isXrayTemp;
		variables.masterSliderVal = masterTemp;

		// Calculate the x-ray powder-pattern scale factor
		setPowderScaleFactor();
		setPowderAxisTicks(tol);
		assignPeakTypes(); // Type the peaks
		recalcPowder(); // recalculate intensities and positions
	}

	private static class PowderPeakSorter implements Comparator<Integer>{

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
	 */
	private void sortPowderPeaks(double[] dinvlist0, double tol) {
		Integer[] sortList = new Integer[peakCount];
		for (int i = 0; i < peakCount; i++)
			sortList[i] = i;
		Arrays.sort(sortList, new PowderPeakSorter(dinvlist0, crystalPeakHKL, tol));
		double[][] hkls = new double[maxPeaks][3];
		double[] mults = new double[maxPeaks];
		// for testing only 
		// double[] dlist = new double[peakCount];
		for (int i = 0; i < peakCount; i++) {
			int j = sortList[i];
			hkls[i] = crystalPeakHKL[j];
			mults[i] = peakMultiplicities[j];
			//testing only
			//dlist[i] = dinvlist0[j];
		}
		crystalPeakHKL = hkls;
		peakMultiplicities = mults;
		//testing only 
		//dinvlist0 = dlist;
		
//BH let Java do the sort in the most efficient way.
// test is that the above, with dlist uncommented, 
// results in no changes below.
// 
//		double temp;	
//		for (int p1 = 0; p1 < peakCount; p1++) {
//			for (int p = 1; p < peakCount; p++) {
//				boolean firstSameDinv0AsSecond = approxEqual(dinvlist0[p - 1], dinvlist0[p], tol);
//				boolean firstHigher = !firstSameDinv0AsSecond && dinvlist0[p - 1] > dinvlist0[p];
//				// cond1: first peak has higher dinv0 than second peak
//				// cond2: first peak has same dinv0 as second peak
//				boolean firstHKLnotNicerThanSecond = !comparePowderHKL(crystalPeakHKL[p - 1], crystalPeakHKL[p]); 
//				// cond3: first HKL is not nicer than second HKL
//				if (firstHigher || (firstSameDinv0AsSecond && firstHKLnotNicerThanSecond)) {
//					for (int i = 0; i < 3; i++) {
//						temp = crystalPeakHKL[p][i];
//						crystalPeakHKL[p][i] = crystalPeakHKL[p - 1][i];
//						crystalPeakHKL[p - 1][i] = temp;
//					}
//					temp = dinvlist0[p];
//					dinvlist0[p] = dinvlist0[p - 1];
//					dinvlist0[p - 1] = temp;
//					temp = peakMultiplicities[p];
//					peakMultiplicities[p] = peakMultiplicities[p - 1];
//					peakMultiplicities[p - 1] = temp;
//				}
//			}
//		}
	}

	private void setPowderDinvMinMaxRes() {
		System.out.println("PDMMR " + powderPatternType + " " + powderXMax + " " + powderXMin);
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
		if (Math.abs(powderScaleFactor) <= 0.001) // This should never drop to zero, but prevent it just in case.
			powderScaleFactor = 1;
	}

	private final static double[] tickspacecandidates = new double[] {
		0.01, 0.02, 0.05, .10, .2, .5, 1, 2, 5, 10, 20, 50 	
	};

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
			powderAxisTickLabel[powderAxisTickCount++] = Variables.varToString(t, 2, -5);
		}

	}

	/**
	 * Comparison of hkl
	 * 
	 * "Nicer" in order of check:
	 * 
	 *  1) more zeros   [100] better than [110]
	 *  
	 *  2) smaller sum of absolute values [011] better than [012]
	 *  
	 *  3) fewer negative values  [-111] better than [1-1-1]
	 *  
	 *  4) later zeros [100] better than [001]; [110] better than [101]
	 *  
	 *  5) earlier high absolute values in second and third digits:
	 *  
	 *   	[0-12] better than [0-21]; [341] better than [242]
	 *  
	 *  6) earlier non-negative values in second and third digits  [01-2] better than [0-12]
	 *  
	 * 
	 * Called by resetPowderPeaks.
	 * 
	 * @return true if hkla is better than or equal to hklb
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
		
		
//		was:
//		boolean anicerthanb = true;
//			if (za < zb)
//				anicerthanb = false;
//			else if (za == zb) {
//				if (sa > sb)
//					anicerthanb = false;
//				else if (sa == sb) {
//					if (ma > mb)
//						anicerthanb = false;
//					else if (ma == mb) {
//						if (dza < dzb)
//							anicerthanb = false;
//						else if (dza == dzb) {
//							if (dsa > dsb)
//								anicerthanb = false;
//							else if (dsa == dsb) {
//								if (dma < dmb)
//									anicerthanb = false;
//							}
//						}
//					}
//				}
//			}
//
//			return anicerthanb;

	}

	/**
	 * This test is only run if two peaks have the same d-spacing. Called by
	 * resetPowderPeaks. The only reason to do this test is to increase rendering
	 * efficiency (fewer peaks is faster). The current version is a poor hack --
	 * symmetry is needed soon. As of Apr 2015, the hexcheck was ignored and the
	 * samecheck was weakened since it was finding false equivalences in a hexagonal
	 * child structure.
	 */
	private boolean checkPowderPeakEquiv(double[] suphkla, double[] suphklb, double[][] tmat) {
		int[] sa = new int[3];
		int[] sb = new int[3];
		double[] parhkla = new double[3];
		double[] parhklb = new double[3];
		double[] pa = new double[3];
		double[] pb = new double[3];
		double tol = 0.0001;

		MathUtil.matdotvect(tmat, suphkla, parhkla);
		MathUtil.matdotvect(tmat, suphklb, parhklb);
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
//		int tempi;
//		double tempr;
//		for (int j = 0; j < 2; j++)
//			for (int i = 0; i < 2; i++) {
//				if (sa[i] > sa[i + 1]) {
//					tempi = sa[i + 1];
//					sa[i + 1] = sa[i];
//					sa[i] = tempi;
//				}
//				if (sb[i] > sb[i + 1]) {
//					tempi = sb[i + 1];
//					sb[i + 1] = sb[i];
//					sb[i] = tempi;
//				}
//				if (pa[i] > pa[i + 1]) {
//					tempr = pa[i + 1];
//					pa[i + 1] = pa[i];
//					pa[i] = tempr;
//				}
//				if (pb[i] > pb[i + 1]) {
//					tempr = pb[i + 1];
//					pb[i + 1] = pb[i];
//					pb[i] = tempr;
//				}
//			}
//
		return ((sa[0] == sb[0]) && (sa[1] == sb[1]) && (sa[2] == sb[2]) 
				&& approxEqual(pa[0], pb[0], tol)
				&& approxEqual(pa[1], pb[1], tol)
				&& approxEqual(pa[2], pb[2], tol));

//		if (!sameCheck) {
//		boolean fullCheck;
//			int[] comp = new int[6];
//			comp[0] = Math.abs(sa[0] - sa[1]);
//			comp[1] = Math.abs(sa[0] + sa[1]);
//			comp[2] = Math.abs(sa[1] - sa[2]);
//			comp[3] = Math.abs(sa[1] + sa[2]);
//			comp[4] = Math.abs(sa[0] - sa[2]);
//			comp[5] = Math.abs(sa[0] + sa[2]);
//			for (int j = 0; j < 3; j++)
//				for (int jj = j + 1; jj < 3; jj++)
//					for (int i = 0; i < 3; i++)
//						for (int ii = i + 1; ii < 3; ii++)
//							if ((sa[i] == sb[j]) && (sa[ii] == sb[jj]))
//								for (int jjj = 0; jjj < 3; jjj++)
//									if ((jjj != j) && (jjj != jj))
//										for (int k = 0; k < 6; k++)
//											if (sb[jjj] == comp[k]) {
//												// System.out.println(i+","+ii+","+j+","+jj+":
//												// "+jjj+","+tb[jjj]+","+comp[k]);
//												hexCheck = true;
//											}
//		}
//
//		fullCheck = sameCheck; // used to be (hexCheck || sameCheck)
//		// System.out.println("("+ta[0]+","+ta[1]+","+ta[2]+")
//		// ("+tb[0]+","+tb[1]+","+tb[2]+"): "+fullCheck);
//
//		return fullCheck;
	}

	private static boolean approxEqual(double a, double b, double tol) {
		return (Math.abs(a - b) < tol); 	
	}

	/**
	 * identifies parent and super-lattice peaks that are systemtically absent
	 * Called by resetCrystalPeaks and resetPowderPeaks.
	 */
	public void assignPeakTypes() {
		Random rval = new Random();
		double tempscalar, masterTemp;
		double[][] dispTemp = new double[variables.numTypes][];
		double[][] scalarTemp = new double[variables.numTypes][];
		double[][] magTemp = new double[variables.numTypes][];
		double[] parenthkl = new double[3], superhkl = new double[3];
		double ptolerance = 0.01; // Determines whether or not a peak is a parent Bragg peak
		for (int t = 0; t < variables.numTypes; t++) {
			dispTemp[t] = new double[variables.dispmodePerType[t]];
			scalarTemp[t] = new double[variables.scalarmodePerType[t]];
			magTemp[t] = new double[variables.magmodePerType[t]];
		}

		// Set the peak type to 1 for parent Bragg peaks, and 3 otherwise.
		for (int p = 0; p < peakCount; p++) {
			for (int j = 0; j < 3; j++)
				superhkl[j] = crystalPeakHKL[p][j];
			MathUtil.matdotvect(variables.Tmat, superhkl, parenthkl); // transform super hkl into parent hkl
			peakColor[p] = 3;
			tempscalar = 0;
			for (int j = 0; j < 3; j++)
				tempscalar += Math.abs(parenthkl[j] - Math.rint(parenthkl[j]));
			if (tempscalar < ptolerance)
				peakColor[p] = 1;
		}

		// Save old masterSliderVal and set it to 1.0
		masterTemp = variables.masterSliderVal;
		variables.masterSliderVal = 1.0;

		// Save and zero all displacive, scalar and magnetic mode values
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.dispmodePerType[t]; m++) {
				dispTemp[t][m] = variables.dispmodeSliderVal[t][m];
				variables.dispmodeSliderVal[t][m] = 0;
			}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.scalarmodePerType[t]; m++) {
				scalarTemp[t][m] = variables.scalarmodeSliderVal[t][m];
				variables.scalarmodeSliderVal[t][m] = 0;
			}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.magmodePerType[t]; m++) {
				magTemp[t][m] = variables.magmodeSliderVal[t][m];
				variables.magmodeSliderVal[t][m] = 0;
			}

		// Randomize all GM1 mode values
		// Calculate all peak intensities and set zero-intensity peaks to type 2.
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.dispmodePerType[t]; m++)
				if (variables.dispmodeName[t][m].startsWith("GM1")
						&& !variables.dispmodeName[t][m].startsWith("GM1-")) {
					variables.dispmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.dispmodeMaxAmp[t][m];
//					System.out.println(rd.dispmodeName[t][m]+" "+dispTemp[t][m]);
				}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.scalarmodePerType[t]; m++)
				if (variables.scalarmodeName[t][m].startsWith("GM1")
						&& !variables.scalarmodeName[t][m].startsWith("GM1-")) {
					variables.scalarmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.scalarmodeMaxAmp[t][m];
//					System.out.println(scalarmodeName[t][m]+" "+scalarTemp[t][m]);
				}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.magmodePerType[t]; m++)
				if (variables.magmodeName[t][m].startsWith("GM1") && !variables.magmodeName[t][m].startsWith("GM1-")) {
					variables.magmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.magmodeMaxAmp[t][m];
//					System.out.println(rd.magmodeName[t][m]+" "+magTemp[t][m]);
				}
		variables.recalcDistortion();
		recalcIntensities();
		for (int p = 0; p < peakCount; p++)
			if ((peakColor[p] == 1) && (Math.abs(peakIntensity[p]) < 0.00000001))
				peakColor[p] = 2;

		// Randomize all other (non-GM1) displacive and scalar mode values.
		// Calculate all the peak intensities and then set the zero-intensity
		// superlattice peaks to type 4.
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.dispmodePerType[t]; m++)
				if (!(variables.dispmodeName[t][m].startsWith("GM1")
						&& !variables.dispmodeName[t][m].startsWith("GM1-"))) {
					variables.dispmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.dispmodeMaxAmp[t][m];
//					System.out.println(rd.dispmodeName[t][m]+" "+dispTemp[t][m]);
				}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.scalarmodePerType[t]; m++)
				if (!(variables.scalarmodeName[t][m].startsWith("GM1")
						&& !variables.scalarmodeName[t][m].startsWith("GM1-"))) {
					variables.scalarmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.scalarmodeMaxAmp[t][m];
//					System.out.println(rd.scalarmodeName[t][m]+" "+scalarTemp[t][m]);
				}
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.magmodePerType[t]; m++)
				if (!(variables.magmodeName[t][m].startsWith("GM1")
						&& !variables.magmodeName[t][m].startsWith("GM1-"))) {
					variables.magmodeSliderVal[t][m] = (2 * rval.nextFloat() - 1) * variables.magmodeMaxAmp[t][m];
//					System.out.println(rd.magmodeName[t][m]+" "+magTemp[t][m]);
				}
		variables.recalcDistortion();
		recalcIntensities();
		for (int p = 0; p < peakCount; p++)
			if ((peakColor[p] == 3) && (Math.abs(peakIntensity[p]) < 0.00000001))
				peakColor[p] = 4;

		// Restore all displacement and scalar mode values to their original values.
		variables.masterSliderVal = masterTemp;
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.dispmodePerType[t]; m++)
				variables.dispmodeSliderVal[t][m] = dispTemp[t][m];
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.scalarmodePerType[t]; m++)
				variables.scalarmodeSliderVal[t][m] = scalarTemp[t][m];
		for (int t = 0; t < variables.numTypes; t++)
			for (int m = 0; m < variables.magmodePerType[t]; m++)
				variables.magmodeSliderVal[t][m] = magTemp[t][m];
		variables.recalcDistortion();
		recalcIntensities();

//		for (int p=0; p<peaknum; p++)
//			if (peaktypeList[p]==3)
//				System.out.println("hkl = ("+peakhklList[p][0]+","+peakhklList[p][1]+","+peakhklList[p][2]+"), Int = "+peakintList[p]);
	}

	/**
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 * 5 5 5 The fifth section has the methods that listen for the sliderbars, the
	 * keyboard and the viewing buttons. 5 5 5
	 * 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
	 */

	/**
	 * keyinputListener responds to keyboard commands.
	 */
	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	private void resetControls() {
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
		minTxt.setText("5.00");
		maxTxt.setText("60.00");
		fwhmTxt.setText("0.200");
		zoomTxt.setText("1.0");
		tButton.setSelected(true);
		parentButton.setSelected(true);
		xrayButton.setSelected(true);
		colorBox.setSelected(false);
		variables.resetSliders();
		needsRecalc = true;
	}
	
	@Override
	protected void setControlsFrom(IsoApp a) {
		if (a == null)
			return;
		isAdjusting = true;
		IsoDiffractApp app = (IsoDiffractApp) a;
		hOTxt.setText(app.hOTxt.getText());
		kOTxt.setText(app.kOTxt.getText());
		lOTxt.setText(app.lOTxt.getText());
		hHTxt.setText(app.hHTxt.getText());
		kHTxt.setText(app.kHTxt.getText());
		lHTxt.setText(app.lHTxt.getText());
		hVTxt.setText(app.hVTxt.getText());
		kVTxt.setText(app.kVTxt.getText());
		lVTxt.setText(app.lVTxt.getText());
		qTxt.setText(app.qTxt.getText());
		wavTxt.setText(app.wavTxt.getText());
		minTxt.setText(app.minTxt.getText());
		maxTxt.setText(app.maxTxt.getText());
		fwhmTxt.setText(app.fwhmTxt.getText());
		zoomTxt.setText(app.zoomTxt.getText());

		xrayButton.setSelected(app.xrayButton.isSelected());
		neutronButton.setSelected(app.neutronButton.isSelected());
		
		superButton.setSelected(app.superButton.isSelected());
		parentButton.setSelected(app.parentButton.isSelected());

		tButton.setSelected(app.tButton.isSelected());
		dButton.setSelected(app.dButton.isSelected());
		qButton.setSelected(app.qButton.isSelected());
				
		crystalButton.setSelected(app.crystalButton.isSelected());
		powderButton.setSelected(app.powderButton.isSelected());
		bothButton.setSelected(app.bothButton.isSelected());

		colorBox.setSelected(app.colorBox.isSelected());

		needsRecalc = true;
		isAdjusting = false;
		updateDisplay();
	}




	@Override
	public void keyTyped(KeyEvent e) {
		switch (e.getKeyChar()) {
		case 'r':
		case 'R':
			resetControls();
			break;
		case 'z':
		case 'Z':
			variables.zeroSliders();
			variables.isChanged = true;
			break;
		case 'i':
		case 'I':
			variables.resetSliders();
			variables.isChanged = true;
			break;
		case 's':
		case 'S':
			variables.toggleIrrepSliders();
			variables.isChanged = true;
			break;
		}
	}

	/**
	 * mouseoverListener responds to mouseover-peak events.
	 */
	@Override
	public void mouseDragged(MouseEvent e) {
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		mousePeak(e.getX(), e.getY());
	}

	/** creates the components of the control panel */
	private void buildControls() {
		rangeButtons = new ButtonGroup();
		tButton = newRadioButton("2\u0398", true, rangeButtons);
		dButton = newRadioButton("d", false, rangeButtons);
		qButton = newRadioButton("q", false, rangeButtons);

		hklButtons = new ButtonGroup();
		parentButton = newRadioButton("Parent", true, hklButtons);
		superButton = newRadioButton("Super", false, hklButtons);

		pscButtons = new ButtonGroup();
		crystalButton = newRadioButton("Crystal", true, pscButtons);
		powderButton = newRadioButton("Powder", false, pscButtons);
		bothButton = newRadioButton("Both", false, pscButtons);

		xnButtons = new ButtonGroup();
		xrayButton = newRadioButton("Xray", true, xnButtons);
		neutronButton = newRadioButton("Neut", false, xnButtons);
		neutronButton.setHorizontalAlignment(JRadioButton.LEFT);

		colorBox = new JCheckBox("Color", false);
		colorBox.setHorizontalAlignment(JCheckBox.LEFT);
		colorBox.setVerticalAlignment(JCheckBox.CENTER);
		colorBox.setFocusable(false);
		colorBox.setBackground(Color.WHITE);
		colorBox.setForeground(Color.BLACK);
		colorBox.addItemListener(buttonListener);

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
		minTxt = newTextField("5.00", 0);
		maxTxt = newTextField("60.00", 0);
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
		topControlPanel.add(colorBox);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(xrayButton);
		topControlPanel.add(neutronButton);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(parentButton);
		topControlPanel.add(superButton);
		topControlPanel.add(tButton);
		topControlPanel.add(dButton);
		topControlPanel.add(qButton);
		topControlPanel.add(new JLabel("      "));
		topControlPanel.add(crystalButton);
		topControlPanel.add(powderButton);
		topControlPanel.add(bothButton);

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

		addSaveButtons(botControlPanel);

		controlPanel.add(topControlPanel);
		controlPanel.add(botControlPanel);
		
		// clear checkbox listeners
		variables.setApp(this);
	}

	/** loads the control components into the control panel */
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
		superButton.setVisible(!isPowder && !isMouseOver);

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
	protected void frameResized() {
		if (rp == null)
			return;
		rp.im = null;
		needsRecalc = true;
		updateDisplay();
	}

	public static void main(String[] args) {
		create("IsoDiffract", args);
	}

	@Override
	protected void handleCheckBoxEvent(Object src) {
		isSimpleColor = colorBox.isSelected();
		variables.setColors(isSimpleColor);
		variables.recolorPanels();
		drawPanel.repaint();
	}

	@Override
	protected void handleRadioButtonEvent(Object source) {
		if (!((JToggleButton) source).isSelected())
			return;
		System.out.println(
				"UB1 " + powderXMax + " " + powderXMin + " " + powderDinvmin + " " + powderDinvmax + " " + source);
		boolean setTextBoxes = false;
		if (source == tButton) {
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
		} else if (source == dButton) {
			powderPatternType = 2;
			powderXMax = 1 / powderDinvmin;
			powderXMin = 1 / powderDinvmax;
			double d = powderXMin + powderXMax;
			powderResolution = powderDinvres * d * d / 4;
			setTextBoxes = true;
			needsRecalc = true;
		} else if (source == qButton) {
			powderPatternType = 3;
			powderXMin = powderDinvmin * (2 * Math.PI);
			powderXMax = powderDinvmax * (2 * Math.PI);
			powderResolution = powderDinvres * (2 * Math.PI) / ((powderXMin + powderXMax) / 2);
			setTextBoxes = true;
			needsRecalc = true;
		} else if (source == parentButton) {
			hklType = 1;
			needsRecalc = true;
		} else if (source == superButton) {
			hklType = 2;
			needsRecalc = true;
		} else if (source == xrayButton) {
			isXray = true;
			variables.readSliders();
			needsRecalc = true;
		} else if (source == neutronButton) {
			isXray = false;
			variables.readSliders();
			needsRecalc = true;
		} else if (source == crystalButton) {
			isBoth = isPowder = false;
			showControls();
			needsRecalc = true;
		} else if (source == powderButton) {
			isBoth = !(isPowder = true);
			showControls();
			needsRecalc = true;
		} else if (source == bothButton) {
			wasPowder = isPowder;
			isBoth = isPowder = true;
			showControls();
			needsRecalc = true;
		}
		if (setTextBoxes && !isAdjusting) {
			minTxt.setText(Variables.varToString(powderXMin, 2, -5));
			maxTxt.setText(Variables.varToString(powderXMax, 2, -5));
			fwhmTxt.setText(Variables.varToString(powderResolution, 3, -5));
		}
		updateDisplay();
	}
	

	@Override
	protected double[][] getPerspective() {
		// n/a
		return null;
	}

	@Override
	protected void setPerspective(double[][] params) {
		// n/a
	}

	@Override
	protected void stopSpin() {
		// n/a
	}

	@Override
	protected void applyView() {
		// TODO Auto-generated method stub
		
	}

}
