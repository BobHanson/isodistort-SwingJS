/**
 *  Branton Campbell, David Tanner Andrew Zimmerman, 
 *  June 2006
 * 
 * This applet takes the data from paramReader (which has been read in from isoDistort)
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

package org.byu.isodistort0;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.JCheckBox;

//Stuff used for early version of the image saver.
//import java.awt.image.BufferedImage;
//import javax.imageio.ImageIO;
//import javax.swing.JFileChooser;
//import javax.swing.filechooser.FileFilter;
//import org.byu.isodistort0.local.BufferedImageBuilder;
//import java.io.File;

import org.byu.isodistort0.render.Vec;
import org.byu.isodistort0.local.Dash;
import org.byu.isodistort0.local.CommonStuff;
import org.byu.isodistort0.local.Circle;
import org.byu.isodistort0.local.ElementParser;
import org.byu.isodistort0.local.ImageSaver;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.applet.Applet;
import java.util.Arrays;
import java.util.Random;

public class IsoDiffractApplet extends Applet implements Runnable
{private static final long serialVersionUID = 1L;

// Variables that the user may want to adjust
	/**False for datafile and True for html file*/
	boolean readMode = true;
	/**The datafile to use when readMode is false*/
	String whichdatafile = "data/test28.txt";
	/**Maximum number of diffraction peaks and tickmarks in the display*/
	int maxpeaks = 1000, maxvisiblepeaks = 200, maxticks = 100;
	/**Axis & tick line thickness, tick length */
	double linethickness = 2, ticklength = 10;
	/**Heights of powder display components */
	double powderaxisheight = 30, powderstickheight = 60, powderpatternheight = 80, powderlabelheight = 5;
	/**restabit rests the thread for the indicated number of milliseconds.*/
	int restabit = 10;
	/**Factor that determines the lowest observable intensities */
	double logfac = 4.0;
	/**Number of points in the powder pattern */
	int powderpoints = 1024;
	/**Anisotropic thermal parameter for simulating high-q intensity decay */
	double uiso = 0.15;
	

	//Other global variables.
	/** An instance of the LoadVariables class that holds all the input data  */
	CommonStuff rd;
	/**A string containing all of the input data*/
	String dataString="";
	/** Which type of cell: (1)parent or (2) super */
	int hklType = 1;
	/** Horizontal axis choice for powder pattern: (1) 2theta, (2) d-space, (3) q */
	int patternType = 1;
	/** X-ray vs neutron diffraction */
	public boolean isXray = true;
	/** contains x-ray and neutron scattering factors for each element */
	ElementParser elemParser = new ElementParser();
	/** radius of the display in pixels */
	double pixelrange;
	/** True means that the applet is running */
	boolean isRunning;
	/** True means to make all atoms of the same element the same color */
 	boolean isSimpleColor = false;
	/** True means powder pattern.  False means single-crystal pattern */
	boolean isPowder = false; // start in crystal rather than powder mode
	boolean isMouseOver = false;
	boolean isReset = false;


//	Global variables related to the display of peaks and axes.
	/** distance (in pixels) to the nearest peak from the center */
	double closest;
	/** maximum peak radius */
	double maxrad;
	/** number of superHKL peaks contained within the display */
	int peaknum;
	/** List of superHKLs contained within the display */
	double[][] peakhklList = new double[maxpeaks][3];
	/** List of peak XY coords contained within the single-crystal display */
	double[][] peakcartList = new double[maxpeaks][2];
	/** List of positions of powder peaks in either 2th, d or q units */
	double[] peakpowderposList = new double[maxpeaks];
	/** List of positions of powder peaks in scaled pixel units */
	double[] peakpowderpixList = new double[maxpeaks];
	/** List of peak radii contained within the single-crystal display */
	double[] peakintList = new double[maxpeaks];
	/** List of peak radii contained within the single-crystal display */
	double[] peakradList = new double[maxpeaks];
	/** List of types of peaks contained within the display */
	int[] peaktypeList = new int[maxpeaks];
	/** List of peak multiplicities for powder pattern */
	double[] peakmultList = new double[maxpeaks];
	/** List of dinverse values for peaks in either single-crystal or powder display */
	double[] peakdinvList = new double[maxpeaks];
	/** Largest powder peak multiplicity */
	double powderScaleFactor; // initial value of 1
	/**The hkl horizonal and upper directions, */
	double[][] hklHVdirs = new double[2][3];
	/**The hkl horizonal direction, upper direction, and center, */
	double[] hklCenter = new double[3];
	/** unstrained dinverse-space metric tensor */
	double[][] metric0 = new double[3][3];
	/** strained dinverse-space metric tensor */
	double[][] metric = new double[3][3];
	/** transforms superHKL to properly-rotated XYZ cartesian*/
	double[][] slatt2rotcart = new double[3][3];
	/** transforms properly-rotated XYZ cartesian to superHKL*/
	double[][] rotcart2slatt = new double[3][3];
	/** number of single-crystal tickmarks to be displayed */
	int[] ticknum = new int[2];
	/** List of tickmark supHKLs to be displayed */
	double[][][] tickhklList = new double[2][maxticks][3];
	/** List of tickmark XY coords to be displayed  */
	double[][][] tickcartList = new double[2][maxticks][4];
	/** Plot axes for crystal display */
	double[][] axes = new double[2][5];
	/** Number of powder tick marks and tick labels to be displayed */
	int powderticknum;
	/** List of powder tick marks to be displayed in window pixel units  */
	double[] powdertickpixList = new double[maxticks];
	/** List of powder tick labels to be displayed in window pixel units  */
	String[] powderticklabelList = new String[maxticks];
	
	
	/** radius of the range of Q/2Pi contained within the crystal display */
	double dinvrange;
	/** wavelength for computing 2theta scale in the powder display */
	double wav;
	/** horizontal range minimum value in powder display */
	double min;
	/** horizontal range maximum value in powder display */
	double max;
	/** horizontal resolution in the powder display */
	double fwhm;
	/** vertical zoom factor in the powder display */
	double zoom;
	/** dinvmin and dinvmax are the maximum and minimum values in the powder display in d-inverse units */
	double dinvmin, dinvmax, dinvres; 
	/** the array that holds the powder pattern*/
	double[] powderpattern = new double[powderpoints];
	
	JLabel horizLabel, upperLabel, centerLabel, qrangeLabel, wavLabel, minLabel, maxLabel, fwhmLabel, zoomLabel, mouseoverLabel;

	JCheckBox colorBox;
	/**Radio buttons*/
	JRadioButton parentButton, superButton, xrayButton, neutronButton, crystalButton, powderButton, dButton, qButton, tButton;
	/**Radio button groups -- only one element from a group can be selected*/
	ButtonGroup xnButtons, hklButtons, pscButtons, rangeButtons;
	/**Button for imputing view*/
	JButton applyView = new JButton("Apply View");
	/**Button for saving the current applet image*/
	JButton saveImage = new JButton("Save Image");
	/**Text fields for inputting viewing region*/
	JTextField hHTxt = new JTextField(3), kHTxt = new JTextField(3), lHTxt = new JTextField(3);
	JTextField hVTxt = new JTextField(3), kVTxt = new JTextField(3), lVTxt = new JTextField(3);
	JTextField hOTxt = new JTextField(3), kOTxt = new JTextField(3), lOTxt = new JTextField(3);
	JTextField qTxt = new JTextField(3);
	JTextField wavTxt = new JTextField(3);
	JTextField minTxt = new JTextField(3);
	JTextField maxTxt = new JTextField(3);
	JTextField fwhmTxt = new JTextField(3);
	JTextField zoomTxt = new JTextField(3);

	/** Image framebuffer */
	protected Image im; // IMAGE OF MEMORY SOURCE OBJECT

/**
 222222222222222222222222222222222222222222222222222222222222222222222222222222
 2																			  2
 2 In the this section, we initialize everything on the applet that we want.  2
 2																			  2
 222222222222222222222222222222222222222222222222222222222222222222222222222222
 */
		
	public void init()
	{	
		/**Import the variables from paramReader.  Method is below.*/
		this.addKeyListener(new keyinputListener());
		this.addMouseMotionListener(new mouseoverListener());
		readFile();
		rd = new CommonStuff(dataString, true);
		pixelrange = rd.renderingWidth/2;
		initView();
		resetCrystalPeaks();
	}
	
	private Thread t;

	public void start()
	{
		this.setVisible(true);
		this.requestFocus();
		this.requestFocusInWindow();
		this.requestFocus();
		if (t == null)
		{
			t = new Thread(this);
			isRunning = true;
			t.start();  // starts the thread and executes its run() method
		}
	}
	
	public void stop()
	{
		if (t != null)
			isRunning = false;
	}
	
	/**
	 * This code loops continuously.
	 */
	public void run()
	{
		while (isRunning)
		{
	    	try
	    	{
	    		Thread.sleep(restabit);
	    	}
	    	catch (InterruptedException ie)
	    	{
	    		;
	    	}
	    	
	    	if (isReset)
	    	{
	    		rd.readSliders();
	    		if (isPowder)
	    			resetPowderPeaks();
	    		else
	    			resetCrystalPeaks();
	    		repaint();
	    		requestFocus(); // trick for recovering from loss of focus -- otherwise keyboard listeners don't work.
	    		isReset = false;
	    	}
	    	else if (rd.isChanged)
	    	{
	    		rd.readSliders();
	    		if (isPowder)
	    			recalcPowder();
	    		else
	    			recalcCrystal();
	    		repaint();
	    		requestFocus(); // trick for recovering from loss of focus -- otherwise keyboard listeners don't work.

	    		rd.isChanged = false;
	    	}

		}
	}
		
		
	/**
	 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 2 These methods do all the drawing.  update() and paint() are overridden     2
	 2 methods of the Applet class that repaint() calls on.                       2
	 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 */
	
	/** Updates the components that appear in the applet.	 */
	public void update(Graphics gr) {
		im = createImage(rd.renderingWidth, rd.renderingHeight);
		Graphics temp = im.getGraphics();
		temp.setColor(Color.BLACK);
		temp.fillRect(0, 0, rd.renderingWidth, rd.renderingHeight);
		if (isPowder)
		{
			drawSticks(temp);
			drawPattern(temp);
		}
		else
		{
			drawCrystPeaks(temp);
			drawCrystAxes(temp);
		}
		gr.drawImage(im, 0, 0, this);
	}
	
	public void paint(Graphics gr) {
		update(gr);
		rd.scrollPane.repaint();
		rd.controlPane.repaint();
	}
	
	private void drawCrystPeaks(Graphics gr) {
		for (int index = 0; index < peaknum; index++) {
			Circle circle = new Circle(peakcartList[index][0], peakcartList[index][1],
					peakradList[index], maxrad, peaktypeList[index]);
			circle.draw(gr);
		}
	}
	
	private void drawCrystAxes(Graphics gr) {
		for (int i = 0; i < 2; i++) {
			Dash dash = new Dash(axes[i][0], axes[i][1], axes[i][2], axes[i][3], axes[i][4], linethickness, 0);
			dash.draw(gr);
			for (int j = 0; j < ticknum[i]; j++) {
				dash = new Dash(tickcartList[i][j][0], tickcartList[i][j][1], tickcartList[i][j][2], tickcartList[i][j][3],
						ticklength, linethickness, 0);
				dash.draw(gr);
			}
		}
	}

	private void drawSticks(Graphics gr) {
		for (int i = 0; i < 2; i++) {
			Dash dash = new Dash(pixelrange, 2*pixelrange - powderaxisheight, 1, 0, pixelrange, linethickness, 0);
			dash.draw(gr);
			for (int peakcolor = 4; peakcolor>0; peakcolor--) // draw one color at at time with red and green last and on top
			{
				for (int p = 0; p < peaknum; p++)
				{
					if (peaktypeList[p] == peakcolor)
					{
						dash = new Dash(peakpowderpixList[p], 2*pixelrange - powderstickheight, 0, 1, ticklength, linethickness, peaktypeList[p]);
						dash.draw(gr);
					}
				}
			}
			for (int n = 0; n < powderticknum; n++)
			{
				dash = new Dash(powdertickpixList[n], 2*pixelrange - powderaxisheight, 0, 1, ticklength, linethickness, 0);
				dash.draw(gr);
				gr.drawString(powderticklabelList[n], (int) powdertickpixList[n]-15,(int)(2*pixelrange - powderlabelheight));
			}
		}
	}

	private void drawPattern(Graphics gr) {
		int x0, y0, x1, y1;
		gr.setColor(Color.white);
		for (int i = 1; i < powderpoints; i++) 
		{
			x0 = (int)((2*pixelrange)*((double)i-1)/((double)powderpoints));
			x1 = (int)((2*pixelrange)*((double)i)/((double)powderpoints));
			y0 = (int)((2*pixelrange - powderpatternheight)*(1-0.8*powderpattern[i-1]*zoom/powderScaleFactor));
			y1 = (int)((2*pixelrange - powderpatternheight)*(1-0.8*powderpattern[i]*zoom/powderScaleFactor));
			gr.drawLine(x0, y0, x1, y1);
		}
	}

	/**
	* mousePeak determines which Bragg peak, if any, is under the mouse.
	  @param x is the x-coordinate of the mouse in the Applet window
	  @param y is the y-coordinate of the mouse in the Applet window
	  @param radius is the radius of tolerance for mouseover on a peaks
	  @return which peak is under the mouse
	*/
	private void mousePeak(double x, double y, double tolerance) {
		int currentpeak = -1, currentcolor = 5;
		String mouseovertext, valuestring="", specifictext="";
		
		for (int p=0; p<peaknum; p++)
		{	
			if (isPowder)
			{
				if ((Math.abs(x - peakpowderpixList[p]) < tolerance) && (Math.abs(y - (2*pixelrange-powderstickheight)) < 1.25*ticklength) && (peaktypeList[p] < currentcolor))
				{
					currentpeak = p;
					currentcolor = peaktypeList[p];
					valuestring = CommonStuff.mytoString(peakpowderposList[p],2,-2);
					if (patternType == 1)
						specifictext = "2theta = "+valuestring+" Degrees";
					else if (patternType == 2)
						specifictext = "d = "+valuestring+" Angstroms";
					else if (patternType == 3)
						specifictext = "q = "+valuestring+" 1/Angstroms";
				}
			}
			else
			{
				if (Math.sqrt(Math.pow(x - peakcartList[p][0],2)+Math.pow(y - peakcartList[p][1],2)) <= tolerance)
				{
					currentpeak = p;
					specifictext = "";
					break;
				}
			}
		}
		if (currentpeak >= 0)
		{
			double[] hklp = new double[3];
			double[] hkls = new double[3];
			for (int n=0; n<3; n++)
				hkls[n] = peakhklList[currentpeak][n];
			Vec.matdotvect(rd.Tmat,hkls,hklp);
			
			mouseovertext = "Parent HKL = ("+CommonStuff.mytoString(hklp[0],2,-2)+", "
				+CommonStuff.mytoString(hklp[1],2,-2)+", "+CommonStuff.mytoString(hklp[2],2,-2)
				+")       Super HKL = ("+CommonStuff.mytoString(hkls[0],2,-2)+", "
				+CommonStuff.mytoString(hkls[1],2,-2)+", "+CommonStuff.mytoString(hkls[2],2,-2)
				+")     "+specifictext;
			isMouseOver = true;
		}
		else
		{
			mouseovertext = "";
			isMouseOver = false;
		}
		mouseoverLabel.setText(mouseovertext);
		showControls();
	}		

	/**
	 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 2 These methods do the diffraction-related calculations                      2
	 222222222222222222222222222222222222222222222222222222222222222222222222222222
	 */


	/**Recalculates strain info (e.g. metric tensor and slatt2rotcart).
	 * Called by resetCrystalPeaks, resetPowerPeaks, recalcCrystal, recalcPowder.
	 */
	private void recalcStrainstuff()
	{
		double[][] tempmat = new double[3][3];
		double[][] slatt2cart = new double[3][3];  // transforms superHKL to XYZ cartesian
		double[][] rotmat = new double[3][3];//Rotates cartesian q-space so as to place axes1 along +x and axes2 in +y hemi-plane

		//Determine the unstrained metric tensor
		Vec.matinverse(rd.sBasisCart0,tempmat);
		Vec.mattranspose(tempmat,slatt2cart);// B* = Transpose(Inverse(B))
		Vec.matdotmat(tempmat,slatt2cart,metric0); //G* = Transpose(B*).(B*)

		//Determine the new metric tensor
		Vec.matinverse(rd.sBasisCart,tempmat);
		Vec.mattranspose(tempmat,slatt2cart);// B* = Transpose(Inverse(B))
		Vec.matdotmat(tempmat,slatt2cart,metric); //G* = Transpose(B*).(B*)

		// Create an orthonormal rotation matrix that moves axis 1 to the +x direction,
		// while keeping axis 2 in the +y quadrant.  First transform both axes into cartesian
		// coords.  Then take 1.cross.2 to get axis 3, and 3.cross.1 to get the new axis 2.
		// Normalize all three and place them in the rows of the transformation matrix.
		Vec.matdotvect(slatt2cart,hklHVdirs[0],tempmat[0]); 
		Vec.matdotvect(slatt2cart,hklHVdirs[1],tempmat[1]);
		Vec.mycross(tempmat[0],tempmat[1],tempmat[2]);
		Vec.mycross(tempmat[2],tempmat[0],tempmat[1]);
		Vec.normalize(tempmat[0]);
		Vec.normalize(tempmat[1]);
		Vec.normalize(tempmat[2]);
		Vec.matcopy(tempmat,rotmat);
		
		// Combine the rotation and the cartesian conversion to get the overall superHKL to cartesian transformation.
		Vec.matdotmat(rotmat,slatt2cart,slatt2rotcart);
		
		// Invert to get the overall cartesian to superHKL tranformation.
		Vec.matinverse(slatt2rotcart,rotcart2slatt);

	}

	/**
	 * Called by recalcCrystal, recalcPowder, assignPeakTypes.
	 */
	public void recalcIntensities()
	{
		double zzzNR, zzzNI, pppNR, pppNI, scatNR, scatNI, scatM;
		double[] zzzM = new double[3], pppM = new double[3];
		double[] qhat = new double[3], mucart = new double[3], supxyz = new double[3];
		double phase, thermal;
		double[] atomScatFac = new double[2];
		double Intensity000; //The total scattering factor of the unit cell

		for (int p=0; p<peaknum; p++)
		{	
	        zzzNR = 0;
	        zzzNI = 0;
			pppNR = 0;
			pppNI = 0;
			for (int i=0; i<3; i++)
			{
				zzzM[i] = 0;
				pppM[i] = 0;
			}
			Vec.matdotvect(rd.sBasisCart,peakhklList[p],qhat);
			Vec.normalize(qhat);
			thermal = Math.exp(-0.5*uiso*Math.pow(2*Math.PI*peakdinvList[p],2));
			
			for (int t=0; t<rd.numTypes; t++)
				for (int s=0; s<rd.numSubTypes[t]; s++)
					for (int a=0; a<rd.numSubAtoms[t][s]; a++)
					{
	        			for (int i=0; i<3; i++)
	        			{
	        				supxyz[i] = rd.atomFinalCoord[t][s][a][i];
	        			}
	        			if (supxyz[0] >= 0 && supxyz[0] < 1 && supxyz[1] >= 0 && supxyz[1] < 1 && supxyz[2] >= 0 && supxyz[2] < 1)
	        			{
	        				atomScatFac = elemParser.parseElement(rd.atomTypeSymbol[t], isXray);
	        				phase = 2*(Math.PI)*Vec.dot(peakhklList[p],supxyz);
	        				scatNR = rd.atomFinalOcc[t][s][a]*atomScatFac[0];
	        				scatNI = rd.atomFinalOcc[t][s][a]*atomScatFac[1];
	        				zzzNR += rd.atomInitOcc[t][s][a]*atomScatFac[0];
	        				zzzNI += rd.atomInitOcc[t][s][a]*atomScatFac[1];
	        				pppNR += scatNR*Math.cos(phase)-scatNI*Math.sin(phase);
	        				pppNI += scatNI*Math.cos(phase)+scatNR*Math.sin(phase);
//	        				System.out.format("t:%d,s:%d,a:%d, pos:(%.2f,%.2f,%.2f), scatNR/NI:%.3f/%.3f, phase:%.3f%n", t, s, a, supxyz[0], supxyz[1], supxyz[2], scatNR, scatNI, phase);
	        				//remember that magnetic mode vectors (magnetons/Angstrom) were predefined to transform this way.
	        				Vec.matdotvect(rd.sBasisCart, rd.atomFinalMag[t][s][a], mucart);
	        				if (isXray)
	        				{
	        					scatM = 0.0;
	        					for (int i=0; i<3; i++)
	        						zzzM[i] += 0;
	        				}
	        				else
	        				{
	        					scatM = rd.atomFinalOcc[t][s][a]*(5.4);
	        					for (int i=0; i<3; i++)
	        						zzzM[i] += rd.atomInitOcc[t][s][a]*(5.4)*mucart[i];
	        				}
	        				for (int i=0; i<3; i++)
	        					pppM[i] += scatM*(mucart[i]-Vec.dot(mucart,qhat)*qhat[i])*Math.cos(phase);
	        			}
					}
			Intensity000 = Math.pow(zzzNR,2)+Math.pow(zzzNI,2)+Vec.dot(zzzM,zzzM);
			peakintList[p]=thermal*(Math.pow(pppNR,2)+Math.pow(pppNI,2)+Vec.dot(pppM,pppM))/Intensity000;
//			double[] parenthkl = new double[3];
//			Vec.matdotvect(rd.Tmat, peakhklList[p], parenthkl); //transform super hkl into parent hkl
//			System.out.format("peak:%d, parhkl:(%.2f,%.2f,%.2f), intensity:%.2f%n", p, parenthkl[0], parenthkl[1], parenthkl[2], peakintList[p]);
		}
	}
		

	/**Recalculates peak positions and intensities for single-crystal pattern.
	 * Called by resetCrystalPeaks and run().
	 */
	private void recalcCrystal()
	{
		double axislength, dirX, dirY, slope, X, Y, Z, dinv;
		double[] tempvec0 = new double[3], tempvec = new double[3];

	   	// update the structure based on current slider values
		rd.recalcDistortion();

		recalcStrainstuff();
		
    	// update the peak cartesian coordinates
		for (int p=0; p<peaknum; p++)
		{	
			Vec.vecadd(peakhklList[p],1.0,hklCenter,-1.0,tempvec0);
			Vec.matdotvect(slatt2rotcart,tempvec0,tempvec);
			X = tempvec[0]*(pixelrange/dinvrange)+pixelrange;
			Y = -tempvec[1]*(pixelrange/dinvrange)+pixelrange; // minus sign turns the picture upside right.
			peakcartList[p][0] = X;
			peakcartList[p][1] = Y;
//			System.out.println("peak: "+p+", "+peaktypeList[p]+", supHKL: ("+peakhklList[p][0]+" "+peakhklList[p][1]+" "+peakhklList[p][2]+"), Cart: ("+tempvec[0]/dinvrange+" "+tempvec[1]/dinvrange+" "+tempvec[2]+"), Int: "+peakintList[p]);
		}

		// Update the dinverse list
		for (int p=0; p<peaknum; p++)
		{	
			Vec.matdotvect(metric,peakhklList[p],tempvec);
			dinv = Math.sqrt(Vec.dot(peakhklList[p],tempvec));
			peakdinvList[p] = dinv;
		}
		
    	// Update the axis and tickmark plot parameters
		for (int n=0; n<2; n++) // cycle over the two axes
		{
			Vec.matdotvect(slatt2rotcart,hklHVdirs[n],tempvec);
			Vec.normalize(tempvec);
			dirX=tempvec[0]; dirY=tempvec[1];
			slope = -1;
			if (Math.abs(dirX) > 0.001)
			{
				slope = Math.abs(dirY/dirX);
				if (slope < 1) axislength = pixelrange/Math.abs(dirX);
				else axislength = pixelrange/Math.abs(dirY);
			}
			else axislength = pixelrange;

			axes[n][0]=pixelrange;
			axes[n][1]=pixelrange;
			axes[n][2]=dirX;
			axes[n][3]=-dirY; // minus sign turns the picture upside right.
			axes[n][4]=axislength;

			for (int m=0; m<ticknum[n]; m++)
			{
				Vec.matdotvect(slatt2rotcart,tickhklList[n][m],tempvec);
				X = tempvec[0]*(pixelrange/dinvrange)+pixelrange;
				Y = -tempvec[1]*(pixelrange/dinvrange)+pixelrange; // minus sign turns the picture upside right.
				Z = tempvec[2]*(pixelrange/dinvrange);
				tickcartList[n][m][0] = X;
				tickcartList[n][m][1] = Y;
				tickcartList[0][m][2] = axes[1][2];
				tickcartList[0][m][3] = axes[1][3];
				tickcartList[1][m][2] = axes[0][2];
				tickcartList[1][m][3] = axes[0][3];
				if (Z==0){} // No need for Z now, but might use later.
//				System.out.println("Axis: "+n+", Tick: "+m+", supHKL: "+tickhklList[n][m][0]+" "+tickhklList[n][m][1]+" "+tickhklList[n][m][2]+", Cart: "+X+" "+Y+" "+Z);
			}
		}
		
		// update the peak intensities based on current structure
		recalcIntensities();

	   	// update the peak radii
		for (int p=0; p<peaknum; p++)
			peakradList[p] = maxrad*(Math.max(Math.log(peakintList[p])/2.3025, -logfac) + logfac)/logfac;

	}
	

	/** Creates the list of single-crystal peaks and their types 
	 * Called by init() and run().
	 */
	public void resetCrystalPeaks()
	{
		double[] hklH = new double[3], hklV = new double[3], hklO = new double[3];
		double[] superhkl = new double[3], superhklcart = new double[3];
		double tempscalar, tempmin, tempmax, mag, inplanetest;
		double[] tempvec0 = new double[3], tempvec = new double[3], uvw = new double[3];
		double[][] limits = new double[8][3]; // HKL search limits
		double[][] slatt2platt = new double[3][3];  // transforms superHKL to parentHKL		
		double[][] platt2slatt = new double[3][3]; // transforms parentHKL to superHKL
		double ztolerance = 0.001; // z-axis tolerance that determines whether or not a peak is in the display plane

		boolean rangeQ, planeQ;
		int[][] hklrange = new int[3][2];
		int tempint, count;
		
    	Vec.matcopy(rd.Tmat,slatt2platt);
		Vec.matinverse(slatt2platt,platt2slatt);
		
		hklO[0]=Double.parseDouble(hOTxt.getText());
		hklO[1]=Double.parseDouble(kOTxt.getText());
		hklO[2]=Double.parseDouble(lOTxt.getText());
		hklH[0]=Double.parseDouble(hHTxt.getText());
		hklH[1]=Double.parseDouble(kHTxt.getText());
		hklH[2]=Double.parseDouble(lHTxt.getText());
		hklV[0]=Double.parseDouble(hVTxt.getText());
		hklV[1]=Double.parseDouble(kVTxt.getText());
		hklV[2]=Double.parseDouble(lVTxt.getText());
		
		dinvrange = Double.parseDouble(qTxt.getText())/(2*Math.PI);

		//Decide that user input is either of parentHKL or superHKL type
		//Either way, the input directions are passed as superHKL vectors.
		//Note that the platt2slatt matrix is slider-bar independent.
		if (hklType == 2)
		{
			Vec.copy(hklH,hklHVdirs[0]);
			Vec.copy(hklV,hklHVdirs[1]);
			Vec.copy(hklO,hklCenter);
		}
		else if (hklType == 1)
		{
			Vec.matdotvect(platt2slatt,hklH,hklHVdirs[0]);
			Vec.matdotvect(platt2slatt,hklV,hklHVdirs[1]);
			Vec.matdotvect(platt2slatt,hklO,hklCenter);
		}
		
		// Identify the direct-space direction perpendicular to display plane.
		Vec.mycross(hklHVdirs[0],hklHVdirs[1],uvw);

		rd.readSliders();  // Get the latest strain information
		rd.recalcDistortion(); // Update the distortion
		recalcStrainstuff();  // Get updated slatt2rotcart transformation

		//Find out the superHKL range covered within the Qrange specified.
		Vec.set(tempvec0,dinvrange,0,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[0]);
		
		Vec.set(tempvec0,-dinvrange,0,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[1]);

		Vec.set(tempvec0,0,dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[2]);
		
		Vec.set(tempvec0,0,-dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[3]);
		
		Vec.set(tempvec0,dinvrange,dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[4]);
		
		Vec.set(tempvec0,-dinvrange,dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[5]);
		
		Vec.set(tempvec0,dinvrange,-dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[6]);
		
		Vec.set(tempvec0,-dinvrange,-dinvrange,0);
		Vec.matdotvect(rotcart2slatt,tempvec0,tempvec);
		Vec.vecadd(tempvec,1.0,hklCenter,1.0,limits[7]);
		
		for (int ii=0; ii<3; ii++)
		{
			tempmin = 1000;
			tempmax = -1000;
			for (int nn=0; nn<8; nn++)
			{
				if (limits[nn][ii] > tempmax) tempmax = limits[nn][ii];
				if (limits[nn][ii] < tempmin) tempmin = limits[nn][ii];
			}
				hklrange[ii][0] = (int) Math.floor(tempmin);
				hklrange[ii][1] = (int) Math.ceil(tempmax);
		}
//		for (int nn=0; nn<8; nn++)
//			System.out.println(nn+" ("+limits[nn][0]+" "+limits[nn][1]+" "+limits[nn][2]+")");
//		System.out.println("H: "+hklrange[0][0]+" "+hklrange[0][1]);
//		System.out.println("K: "+hklrange[1][0]+" "+hklrange[1][1]);
//		System.out.println("L: "+hklrange[2][0]+" "+hklrange[2][1]);


		//Identify the peaks to display.
		peaknum = 0;
		closest = pixelrange;
		for (int H=hklrange[0][0]; H<=hklrange[0][1]; H++)
			for (int K=hklrange[1][0]; K<=hklrange[1][1]; K++)
				for (int L=hklrange[2][0]; L<=hklrange[2][1]; L++)
				{
					planeQ = false;
					rangeQ = false;
					Vec.set(superhkl,H,K,L);
					Vec.vecadd(superhkl,1.0,hklCenter,-1.0,tempvec0);
					Vec.matdotvect(slatt2rotcart,tempvec0,superhklcart);
					
					inplanetest = Math.abs(Vec.dot(tempvec0,uvw));
					if (inplanetest < ztolerance) planeQ = true; //HKL point lies in the display plane
					if((Math.abs(superhklcart[0])<dinvrange)&&(Math.abs(superhklcart[1])<dinvrange)) rangeQ = true; //HKL point lies in q range of display
					if(planeQ && rangeQ)
					{
						// Save the XY coords of a good peak.
						Vec.copy(superhkl,peakhklList[peaknum]);
						peakmultList[peaknum] = 1;

						tempscalar = Math.sqrt(Math.pow(superhklcart[0],2)+Math.pow(superhklcart[1],2))*(pixelrange/dinvrange);
						if ((Math.abs(tempscalar)>0.1)&&(tempscalar < closest))
							closest = tempscalar;
								
//						System.out.println(peaknum+", "+peaktypeList[peaknum]+", sHKL=("+superhklvec[0]+" "+superhklvec[1]+" "+superhklvec[2]+"), pHKL=("+parenthklvec[0]+" "+parenthklvec[1]+" "+parenthklvec[2]+"), xyz=("+tempvec[0]+" "+tempvec[1]+" "+tempvec[2]+"), int="+peakintList[peaknum]);
						peaknum++;
					}
				}
		
		// Set the max peak radius
		maxrad = Math.min(closest/2,40);
		
		// Identify the tickmark locations along axis0.
		for (int n=0; n<2; n++) // cycle over the two axes
		{
			Vec.matdotvect(slatt2rotcart,hklHVdirs[n],tempvec);
			mag = Vec.norm(tempvec);
			tempint = (int) Math.floor(dinvrange/mag);
			ticknum[n]=2*tempint+1;
			count=0;
			for (int m=-tempint; m<tempint+1; m++)
			{
				for (int i=0; i<3; i++)
					tickhklList[n][count][i] = m*hklHVdirs[n][i];
//				System.out.println("Axis: "+n+", Tick: "+count+", "+tickhklList[n][count][0]+" "+tickhklList[n][count][1]+" "+tickhklList[n][count][2]);
				count++;
			}
		}

		assignPeakTypes();
		recalcCrystal(); // recalculate intensities and positions
	}		

	public static String a2s(double[] a) {
		return Arrays.toString(a);
	}
	

	
	/**Recalculates structural parameters and peak positions and intensities for powder pattern.
	 * Called by resetPowderPeaks and run().
	 */
	private void recalcPowder()
	{
		double[] tempvec = new double[3];
		double dinv, dval, qval, tth;
		
	   	// update the structure based on current slider values
		rd.recalcDistortion();

		// update the peak positions
		recalcStrainstuff();
		for (int p=0; p<peaknum; p++)
		{	
			Vec.matdotvect(metric,peakhklList[p],tempvec);
			dinv = Math.sqrt(Vec.dot(peakhklList[p],tempvec));
			peakdinvList[p] = dinv;
			if (dinv > 0)
				dval = 1/dinv; 
			else
				dval = 0;
			qval = 2*Math.PI*dinv;
			tth = 2*Math.asin(0.5*wav*dinv)*(180/Math.PI);
			if (patternType == 1)
			{
				peakpowderposList[p]=tth;
				peakpowderpixList[p]=(2*pixelrange)*((tth-min)/(max-min));
			}
			else if (patternType == 2)
			{
				peakpowderposList[p]=dval;
				peakpowderpixList[p]=(2*pixelrange)*((dval-min)/(max-min));
			}
			else if (patternType == 3)
			{
				peakpowderposList[p]=qval;
				peakpowderpixList[p]=(2*pixelrange)*((qval-min)/(max-min));
			}
		}

		// update the peak intensities based on current structure
		recalcIntensities();
		
		// recalculate the powder pattern
		double center, sigmapix;
		int left, right;
		sigmapix = Math.ceil(powderpoints*(fwhm/Math.sqrt(8*Math.log(2)))/(max-min));
		for (int i=0; i<powderpoints; i++)
			powderpattern[i]=0;
		for (int p=0; p<peaknum; p++)
		{	
			center = ((double)powderpoints)*(peakpowderpixList[p]/(2*pixelrange));
			left = Math.max((int) Math.floor(center-5*sigmapix), 0);
			right = Math.min((int) Math.ceil(center+5*sigmapix), powderpoints-1);
			for (int i=left; i<=right; i++)
				powderpattern[i] += Math.exp(-Math.pow(((double)i - center)/sigmapix,2)/2)*peakintList[p]*peakmultList[p];
		}		

	}
	
		
	/** Creates the list of powder peaks and their types 
	 * Called by run(). 
	 */
	private void resetPowderPeaks()
	{
		int limH, limK, limL;
		double temp, dinv0, dinv1, dinv2, metricdet, tol = 0.0001;
		double[] dinvlist0 = new double[maxpeaks];//unstrained list of dinverse values
		double[] dinvlist1 = new double[maxpeaks];//randomly strained list of dinverse values
		double[] dinvlist2 = new double[maxpeaks];//randomly strained list of dinverse values
		double[] superhkl = new double[3], parenthkl = new double[3];
		boolean createnewpeak = false, isXrayTemp;
		Random rval = new Random();
		double masterTemp;
		double[] strainTemp = new double[rd.strainmodeNum];
		double[][] randommetric1 = new double[3][3], randommetric2 = new double[3][3];
		double[][] slatt2platt = new double[3][3];  // transforms superHKL to parentHKL		
		double[] tempvec = new double[3];

		//Diagnostic code
		Vec.matcopy(rd.Tmat,slatt2platt);
		for (int i=0; i<3; i++)
			for (int j=0; j<3; j++)
				//System.out.format("[%d][%d] = %.4f, ", i, j, slatt2platt[i][j]);
		//System.out.println("");
		
    	wav=Double.parseDouble(wavTxt.getText());
		min=Double.parseDouble(minTxt.getText());
		max=Double.parseDouble(maxTxt.getText());
		fwhm=Double.parseDouble(fwhmTxt.getText());
		zoom=Double.parseDouble(zoomTxt.getText());
		if ((max <= min)||(patternType == 2)&&((Math.abs(min)<tol)||(Math.abs(max)<tol)))
			return;
		
		if (patternType == 1)
		{
			dinvmin = (2/wav)*Math.sin((Math.PI/180)*min/2);
			dinvmax = (2/wav)*Math.sin((Math.PI/180)*max/2);
			dinvres = fwhm*(1/wav)*(Math.PI/180);
		}
		else if (patternType == 2)
		{
			dinvmin = 1/max;
			dinvmax = 1/min;
			dinvres = fwhm*4/Math.pow((min+max),2);
		}
		else if (patternType == 3)
		{
			dinvmin = min/(2*Math.PI);
			dinvmax = max/(2*Math.PI);
			dinvres = fwhm*((min+max)/2)/(2*Math.PI);
		}
		
		// Save the old strain slider values
		rd.readSliders();
		masterTemp = rd.masterSliderVal;
		rd.masterSliderVal = 1.0;
		for (int m=0; m<rd.strainmodeNum; m++)
			strainTemp[m]=rd.strainmodeSliderVal[m];
		
		// randomize the strains to avoid accidental degeneracies
		for (int m=0; m<rd.strainmodeNum; m++)
			rd.strainmodeSliderVal[m] = (2*rval.nextFloat()-1);//*rd.strainmodeMaxAmp[m];
		rd.recalcDistortion();// Update the distortion parameters
		recalcStrainstuff();  // Build the randomized dinvmetric tensor
		Vec.matcopy(metric,randommetric1);
		
		// randomize the strains again to be extra careful
		for (int m=0; m<rd.strainmodeNum; m++)
			rd.strainmodeSliderVal[m] = (2*rval.nextFloat()-1);//*rd.strainmodeMaxAmp[m];
		rd.recalcDistortion();// Update the distortion parameters
		recalcStrainstuff();  // Build the randomized dinvmetric tensor
		Vec.matcopy(metric,randommetric2);

		// restore the strains to their original values
		rd.masterSliderVal = masterTemp;
		for (int m=0; m<rd.strainmodeNum; m++)
			rd.strainmodeSliderVal[m] = strainTemp[m];

		//use metric tensor to determine h,k,l ranges
		//for each new peak sampled, compare against all previous peaks and accumulate multiplicities
		//	if dinv inside ellipsoid
		//		for each existing peak
		// 			if dinv1 equal dinv2
		//				if peaks are equivalent
		//					keep the nicest one and break
		// 		end loop
		//		if we made it to the end of the loop, add peak to the end of the list
		    	
    	Vec.cross(metric0[0], metric0[1],tempvec);
	    metricdet = Vec.dot(tempvec,metric0[2]); // Calculate the metric determinant
	    limH = (int) Math.ceil(dinvmax*Math.sqrt(Math.abs((metric0[1][1]*metric0[2][2]-metric0[1][2]*metric0[1][2])/metricdet)));
	    limK = (int) Math.ceil(dinvmax*Math.sqrt(Math.abs((metric0[0][0]*metric0[2][2]-metric0[0][2]*metric0[0][2])/metricdet)));
	    limL = (int) Math.ceil(dinvmax*Math.sqrt(Math.abs((metric0[0][0]*metric0[1][1]-metric0[0][1]*metric0[0][1])/metricdet)));
//	    System.out.println("Limits = ("+limH+","+limK+","+limL+")");

	    peaknum = 0;
		for (int H=-limH; H<=limH; H++)
			for (int K=-limK; K<=limK; K++)
				for (int L=-limL; L<=limL; L++)
				{
					Vec.set(superhkl, (double)H,(double)K,(double)L);
					
					//Diagnostic code
					Vec.matdotvect(slatt2platt, superhkl, parenthkl);
					//Diagnostic code					
					double[] ttt = new double[3];
					for (int i=0; i<3; i++)
					{
						ttt[i]= Math.abs(parenthkl[i]);
					}
					for (int j=0; j<2; j++)
					{
						for (int i=0; i<2; i++)
						{
							if (ttt[i]>ttt[i+1])
							{
								temp = ttt[i+1];
								ttt[i+1]=ttt[i];
								ttt[i] = temp;
							}
						}
					}
/* 
 					//Diagnostic code
					boolean diagnosticflag = Math.abs(ttt[0]-0)<tol && Math.abs(ttt[1]-0)<tol && Math.abs(ttt[2]-2)<tol;
					if (diagnosticflag)
						System.out.format("orig suphkl:(%.4f,%.4f,%.4f), parhkl:(%.4f,%.4f,%.4f), ttt:(%.4f,%.4f,%.4f)%n", superhkl[0], superhkl[1], superhkl[2], parenthkl[0], parenthkl[1], parenthkl[2], ttt[0], ttt[1], ttt[2]);
*/					
					//Generate the standard metric and two randomized metrics.
					Vec.matdotvect(metric0,superhkl,tempvec);
					dinv0 = Math.sqrt(Vec.dot(superhkl,tempvec));
					Vec.matdotvect(randommetric1,superhkl,tempvec);
					dinv1 = Math.sqrt(Vec.dot(superhkl,tempvec));
					Vec.matdotvect(randommetric2,superhkl,tempvec);
					dinv2 = Math.sqrt(Vec.dot(superhkl,tempvec));
					
					boolean isinrange, isrobustlycoincident, isequivalent, isnicer;
					isinrange = (dinvmin <= dinv0)&&(dinv0 <= dinvmax);
					if (isinrange)
					{
						createnewpeak = true;
						for (int p=0; p<peaknum; p++)
						{
							isrobustlycoincident = Math.abs(dinv0 - dinvlist0[p])<tol && Math.abs(dinv1 - dinvlist1[p])<tol && Math.abs(dinv2 - dinvlist2[p])<tol;
							if (isrobustlycoincident)
							{
								isequivalent = checkPeakEquiv(superhkl, peakhklList[p]);
								if (isequivalent)
								{
/* 
									//Diagnostic code
									if (diagnosticflag)
									{
										Vec.matdotvect(slatt2platt, peakhklList[p], tempvec);
										System.out.format("comp suphkl:(%.4f,%.4f,%.4f), parhkl:(%.4f,%.4f,%.4f)%n", peakhklList[p][0], peakhklList[p][1], peakhklList[p][2], tempvec[0], tempvec[1], tempvec[2]);   
										System.out.println("");
									}
*/									
									isnicer = aNicerThanb(superhkl,peakhklList[p]);
									if (isnicer)
									{
										Vec.copy(superhkl,peakhklList[p]);
									}
									peakmultList[p] += 1;
									createnewpeak = false;
									break;
								}
							}
						}
						
						if (createnewpeak)
						{
							peakhklList[peaknum][0] = H;
							peakhklList[peaknum][1] = K;
							peakhklList[peaknum][2] = L;
							dinvlist0[peaknum] = dinv0;
							dinvlist1[peaknum] = dinv1;
							dinvlist2[peaknum] = dinv2;
							peakmultList[peaknum] = 1;
							peaknum++;
							if (peaknum >= maxpeaks) return;
						}
					}
				}
				
		//Sort the peak according to increasing dinverse and decreasing niceness
		for (int p1=0; p1<peaknum; p1++)
			for (int p=1; p<peaknum; p++)
			{
				boolean cond1 = dinvlist0[p-1]>dinvlist0[p]+tol; // first peak has higher dinv0 than second peak
				boolean cond2 = Math.abs(dinvlist0[p-1]-dinvlist0[p]) < tol; // first peak has same dinv0 as second peak
				boolean cond3 = !aNicerThanb(peakhklList[p-1],peakhklList[p]); // first HKL is not nicer than second HKL
				if (cond1 || (cond2 && cond3))
				{
					for (int i=0; i<3; i++)
					{
						temp = peakhklList[p][i];
						peakhklList[p][i] = peakhklList[p-1][i];
						peakhklList[p-1][i] = temp;
					}
					temp = dinvlist0[p];
					dinvlist0[p] = dinvlist0[p-1];
					dinvlist0[p-1] = temp;
					temp = peakmultList[p];
					peakmultList[p] = peakmultList[p-1];
					peakmultList[p-1] = temp;
				}
			}
		
		// keep only a practical number of peaks
		if (peaknum > maxvisiblepeaks) peaknum = maxvisiblepeaks;
		
		// Calculate the x-ray powder-pattern scale factor
		masterTemp = rd.masterSliderVal;
		rd.masterSliderVal = 0;
		isXrayTemp = isXray;
		isXray = true;
		recalcPowder();
		isXray = isXrayTemp;
		rd.masterSliderVal = masterTemp;
		
		powderScaleFactor = 0;
		for (int i=0; i<powderpoints; i++)
			if (powderpattern[i] > powderScaleFactor) powderScaleFactor = powderpattern[i];
		if (Math.abs(powderScaleFactor)<=0.001) //This should never drop to zero, but prevent it just in case. 
			powderScaleFactor = 1;
		
		// calculate the list of horizontal-axis tick-mark positions
		double powdertickpreference = 15; // max number of ticks
		double[] tickspacecandidates = new double[10];
		double tickbest, tickspacing = 0, tickbadness, tickmin, tickmax;
		
		tickbest = 1000;
		tickspacecandidates[0] = 0.01;
		tickspacecandidates[1] = 0.02;
		tickspacecandidates[2] = 0.05;
		tickspacecandidates[3] = 0.10;
		tickspacecandidates[4] = 0.20;
		tickspacecandidates[5] = 0.50;
		tickspacecandidates[6] = 1.00;
		tickspacecandidates[7] = 2.00;
		tickspacecandidates[8] = 5.00;
		tickspacecandidates[9] = 10.0;
		for (int i=0; i<10; i++)
		{
			tickbadness = powdertickpreference-(max-min)/tickspacecandidates[i];
			if ((tickbadness < tickbest)&& (tickbadness>0))
			{
				tickbest = tickbadness;
				tickspacing = tickspacecandidates[i];
			}
		}
		tickmin = Math.ceil(min/tickspacing)*tickspacing;
//		tickmin = tickmin+(1-Math.round((tickmin-min)/tickspacing))*tickspacing;
		tickmax = Math.floor(max/tickspacing)*tickspacing;
//		tickmax = tickmax-(1-Math.round((max-tickmax)/tickspacing))*tickspacing;
//		System.out.println(tickspacing+"  "+tickmin+"  "+tickmax);

		powderticknum = 0;
		for (double t = tickmin; t < tickmax+tol; t += tickspacing)
		{
			powdertickpixList[powderticknum] = (2*pixelrange)*((t-min)/(max-min));
			powderticklabelList[powderticknum] = CommonStuff.mytoString(t,2,-5);
			powderticknum++;
		}
		
		assignPeakTypes();  // Type the peaks
		recalcPowder();  // recalculate intensities and positions
		
//		for (int p=0; p<peaknum; p++)
//			System.out.println("peak: "+p+" ("+peakhklList[p][0]+" "+peakhklList[p][1]+" "+peakhklList[p][2]+") type = "+peaktypeList[p]+"   dinv = "+dinvlist0[p]+"   int = "+peakintList[p]);

	}
	
	/** 
	 * Called by resetPowderPeaks. 
	 */
	public boolean aNicerThanb (double[] hkla, double[] hklb)
	{
		boolean anicerthanb = true;
		int[] ta = new int[3], tb = new int[3];
		int za, zb, dza, dzb, ma, mb, dma, dmb, sa, sb, dsa, dsb;
		
		za = 0; zb = 0; // number of zeros
		dza = 0; dzb = 0; // distribution of zeros
		ma = 0; mb = 0; // number of minus signs
		dma = 0; dmb = 0; // distribution of minus signs
		sa = 0; sb = 0; // sum of absolute-valued indices
		dsa = 0; dsb = 0; // distribution of absolute-valued indices
		for (int i=0; i<3; i++)
		{
			ta[i]=(int) hkla[i];
			tb[i]=(int) hklb[i];
			
			if (ta[i]==0) za++; dza += i;
			if (tb[i]==0) zb++; dzb += i;
			if (ta[i]<0) ma++; dma += i;
			if (tb[i]<0) mb++; dmb += i;
			sa += Math.abs(ta[i]); dsa += i*Math.abs(ta[i]);
			sb += Math.abs(tb[i]); dsb += i*Math.abs(tb[i]);
		}
		
		if (za < zb)
			anicerthanb = false;
		else if (za == zb)
		{
			if (sa > sb)
				anicerthanb = false;
			else if (sa == sb)
			{
				if (ma > mb)
					anicerthanb = false;
				else if (ma == mb)
				{
					if (dza < dzb)
						anicerthanb = false;
					else if (dza == dzb)
					{
						if (dsa > dsb)
							anicerthanb = false;
						else if (dsa == dsb)
						{
							if (dma < dmb)
								anicerthanb = false;
						}
					}
				}
			}
		}
		
		return anicerthanb;
	}
	
	
	/**This test is only run if two peaks have the same d-spacing.  
	 * Called by resetPowderPeaks.
	 * The only reason to do this test is to increase rendering efficiency (fewer peaks is faster).
	 * The current version is a poor hack -- symmetry is needed soon.
	 * As of Apr 2015, the hexcheck was ignored and the samecheck was weakened since it was finding
	 * false equivalences in a hexagonal child structure.
	 */
	public boolean checkPeakEquiv(double[] suphkla, double[] suphklb)
	{
		boolean hexCheck = false, sameCheck = false, fullCheck;
		int[] sa = new int[3], sb = new int[3], comp = new int[6];
		double[] parhkla = new double[3], parhklb = new double[3], pa = new double[3], pb = new double[3];
		int tempi;
		double tempr, tol = 0.0001;
		
		Vec.matdotvect(rd.Tmat, suphkla, parhkla);
		Vec.matdotvect(rd.Tmat, suphklb, parhklb);
		for (int i=0; i<3; i++)
		{
			sa[i]=(int) Math.abs(suphkla[i]);
			sb[i]=(int) Math.abs(suphklb[i]);
			pa[i]= Math.abs(parhkla[i]);
			pb[i]= Math.abs(parhklb[i]);
		}
		
		for (int j=0; j<2; j++)
			for (int i=0; i<2; i++)
			{
				if (sa[i]>sa[i+1])
				{
					tempi = sa[i+1];
					sa[i+1]=sa[i];
					sa[i] = tempi;
				}
				if (sb[i]>sb[i+1])
				{
					tempi = sb[i+1];
					sb[i+1]=sb[i];
					sb[i] = tempi;
				}
				if (pa[i]>pa[i+1])
				{
					tempr = pa[i+1];
					pa[i+1]=pa[i];
					pa[i] = tempr;
				}
				if (pb[i]>pb[i+1])
				{
					tempr = pb[i+1];
					pb[i+1]=pb[i];
					pb[i] = tempr;
				}
			}

		if ((sa[0]==sb[0])&&(sa[1]==sb[1])&&(sa[2]==sb[2])&&(Math.abs(pa[0]-pb[0])<tol)&&(Math.abs(pa[1]-pb[1])<tol)&&(Math.abs(pa[2]-pb[2])<tol)) 
			sameCheck = true;
		
		if (!sameCheck)
		{
			comp[0] = Math.abs(sa[0]-sa[1]);
			comp[1] = Math.abs(sa[0]+sa[1]);
			comp[2] = Math.abs(sa[1]-sa[2]);
			comp[3] = Math.abs(sa[1]+sa[2]);
			comp[4] = Math.abs(sa[0]-sa[2]);
			comp[5] = Math.abs(sa[0]+sa[2]);
			for (int j=0; j<3; j++)
				for (int jj=j+1; jj<3; jj++)
					for (int i=0; i<3; i++)
						for (int ii=i+1; ii<3; ii++)
							if ((sa[i]==sb[j]) && (sa[ii]==sb[jj]))
								for (int jjj=0; jjj<3; jjj++)
									if ((jjj!=j)&&(jjj!=jj))
										for (int k=0; k<6; k++)
											if (sb[jjj]==comp[k]){
												//System.out.println(i+","+ii+","+j+","+jj+": "+jjj+","+tb[jjj]+","+comp[k]);
												hexCheck = true;
											}
		}

		fullCheck = sameCheck; // used to be (hexCheck || sameCheck)
		//System.out.println("("+ta[0]+","+ta[1]+","+ta[2]+") ("+tb[0]+","+tb[1]+","+tb[2]+"): "+fullCheck);

		return fullCheck;
	}
	

	/** identifies parent and super-lattice peaks that are systemtically absent 
	 * Called by resetCrystalPeaks and resetPowderPeaks.
	 */
	public void assignPeakTypes()
	{
		Random rval = new Random();
		double tempscalar, masterTemp;
		double[][] dispTemp = new double[rd.numTypes][];
		double[][] scalarTemp = new double[rd.numTypes][];
		double[][] magTemp = new double[rd.numTypes][];
		double[] parenthkl = new double[3], superhkl = new double[3];
		double ptolerance = 0.01; //Determines whether or not a peak is a parent Bragg peak
		for (int t=0; t<rd.numTypes; t++)
		{
			dispTemp[t] = new double[rd.dispmodePerType[t]];
			scalarTemp[t] = new double[rd.scalarmodePerType[t]];
			magTemp[t] = new double[rd.magmodePerType[t]];
		}
		
		// Set the peak type to 1 for parent Bragg peaks, and 3 otherwise.
		for (int p=0; p<peaknum; p++)
		{
			for (int j=0; j<3; j++)
				superhkl[j] = peakhklList[p][j];
			Vec.matdotvect(rd.Tmat,superhkl,parenthkl); //transform super hkl into parent hkl
			peaktypeList[p] = 3;
			tempscalar = 0;
			for (int j=0; j<3; j++)
				tempscalar += Math.abs(parenthkl[j] - Math.rint(parenthkl[j]));
			if (tempscalar < ptolerance) peaktypeList[p] = 1;
		}
		
		// Save old masterSliderVal and set it to 1.0
		masterTemp = rd.masterSliderVal;
		rd.masterSliderVal = 1.0;

		// Save and zero all displacive, scalar and magnetic mode values
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.dispmodePerType[t]; m++)
			{
				dispTemp[t][m] = rd.dispmodeSliderVal[t][m];
				rd.dispmodeSliderVal[t][m] = 0;
			}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.scalarmodePerType[t]; m++)
			{
				scalarTemp[t][m] = rd.scalarmodeSliderVal[t][m];
				rd.scalarmodeSliderVal[t][m] = 0;
			}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.magmodePerType[t]; m++)
			{
				magTemp[t][m] = rd.magmodeSliderVal[t][m];
				rd.magmodeSliderVal[t][m] = 0;
			}

		// Randomize all GM1 mode values
		// Calculate all peak intensities and set zero-intensity peaks to type 2.
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.dispmodePerType[t]; m++)
				if(rd.dispmodeName[t][m].startsWith("GM1") && !rd.dispmodeName[t][m].startsWith("GM1-"))
				{
					rd.dispmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.dispmodeMaxAmp[t][m];
//					System.out.println(rd.dispmodeName[t][m]+" "+dispTemp[t][m]);
				}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.scalarmodePerType[t]; m++)
				if(rd.scalarmodeName[t][m].startsWith("GM1") && !rd.scalarmodeName[t][m].startsWith("GM1-"))
				{
					rd.scalarmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.scalarmodeMaxAmp[t][m];
//					System.out.println(scalarmodeName[t][m]+" "+scalarTemp[t][m]);
				}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.magmodePerType[t]; m++)
				if(rd.magmodeName[t][m].startsWith("GM1") && !rd.magmodeName[t][m].startsWith("GM1-"))
				{
					rd.magmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.magmodeMaxAmp[t][m];
//					System.out.println(rd.magmodeName[t][m]+" "+magTemp[t][m]);
				}
		rd.recalcDistortion();
		recalcIntensities();
		for (int p=0; p<peaknum; p++)
			if ((peaktypeList[p]==1) && (Math.abs(peakintList[p]) < 0.00000001)) peaktypeList[p]=2;


		// Randomize all other (non-GM1) displacive and scalar mode values.
		// Calculate all the peak intensities and then set the zero-intensity superlattice peaks to type 4.
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.dispmodePerType[t]; m++)
				if(!(rd.dispmodeName[t][m].startsWith("GM1") && !rd.dispmodeName[t][m].startsWith("GM1-")))
				{
					rd.dispmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.dispmodeMaxAmp[t][m];
//					System.out.println(rd.dispmodeName[t][m]+" "+dispTemp[t][m]);
				}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.scalarmodePerType[t]; m++)
				if(!(rd.scalarmodeName[t][m].startsWith("GM1") && !rd.scalarmodeName[t][m].startsWith("GM1-")))
				{
					rd.scalarmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.scalarmodeMaxAmp[t][m];
//					System.out.println(rd.scalarmodeName[t][m]+" "+scalarTemp[t][m]);
				}
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.magmodePerType[t]; m++)
				if(!(rd.magmodeName[t][m].startsWith("GM1") && !rd.magmodeName[t][m].startsWith("GM1-")))
				{
					rd.magmodeSliderVal[t][m] = (2*rval.nextFloat()-1)*rd.magmodeMaxAmp[t][m];
//					System.out.println(rd.magmodeName[t][m]+" "+magTemp[t][m]);
				}
		rd.recalcDistortion();
		recalcIntensities();
		for (int p=0; p<peaknum; p++)
			if ((peaktypeList[p]==3) && (Math.abs(peakintList[p]) < 0.00000001)) peaktypeList[p]=4;


		// Restore all displacement and scalar mode values to their original values.
		rd.masterSliderVal = masterTemp;
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.dispmodePerType[t]; m++)
				rd.dispmodeSliderVal[t][m] = dispTemp[t][m];
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.scalarmodePerType[t]; m++)
				rd.scalarmodeSliderVal[t][m] = scalarTemp[t][m];
		for (int t=0; t<rd.numTypes; t++)
			for (int m=0; m<rd.magmodePerType[t]; m++)
				rd.magmodeSliderVal[t][m] = magTemp[t][m];
		rd.recalcDistortion();
		recalcIntensities();

//		for (int p=0; p<peaknum; p++)
//			if (peaktypeList[p]==3)
//				System.out.println("hkl = ("+peakhklList[p][0]+","+peakhklList[p][1]+","+peakhklList[p][2]+"), Int = "+peakintList[p]);
	}
	
	


/**
 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
 5																										   5
 5 The fifth section has the methods that listen for the sliderbars, the keyboard and the viewing buttons. 5
 5																										   5
 55555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555555
 */

			
/**
 * buttonListener class listens for the applet buttons and the inside methods
 * specify the viewing angles.
 */
		private class buttonListener implements ItemListener
		{
			public void itemStateChanged (ItemEvent event)
			{						
            	if (event.getSource()==tButton)
				{
					patternType = 1;
					min = (180/Math.PI)*2*Math.asin(dinvmin*wav/2);
					max = (180/Math.PI)*2*Math.asin(dinvmax*wav/2);
					fwhm = dinvres*wav/(Math.PI/180);
					minTxt.setText(CommonStuff.mytoString(min,2,-5));
					maxTxt.setText(CommonStuff.mytoString(max,2,-5));
					fwhmTxt.setText(CommonStuff.mytoString(fwhm,3,-5));
					isReset = true;
				}
				if (event.getSource()==dButton)
				{
					patternType = 2;
					max = 1/dinvmin;
					min = 1/dinvmax;
					fwhm = dinvres*Math.pow((min+max),2)/4;
					minTxt.setText(CommonStuff.mytoString(min,2,-5));
					maxTxt.setText(CommonStuff.mytoString(max,2,-5));
					fwhmTxt.setText(CommonStuff.mytoString(fwhm,3,-5));
					isReset = true;
				}
				if (event.getSource()==qButton)
				{
					patternType = 3;
					min = dinvmin*(2*Math.PI);
					max = dinvmax*(2*Math.PI);
					fwhm = dinvres*(2*Math.PI)/((min+max)/2);
					minTxt.setText(CommonStuff.mytoString(min,2,-5));
					maxTxt.setText(CommonStuff.mytoString(max,2,-5));
					fwhmTxt.setText(CommonStuff.mytoString(fwhm,3,-5));
					isReset = true;
				}
				if (event.getSource()==parentButton)
				{
					hklType = 1;
					isReset = true;
				}
				if (event.getSource()==superButton)
				{
					hklType = 2;
					isReset = true;
				}
				if (event.getSource()==xrayButton)
				{
					isXray = true;
					rd.readSliders();
					isReset = true;
				}
				if (event.getSource()==neutronButton)
				{
					isXray = false;
					rd.readSliders();
					isReset = true;
				}
				if (event.getSource()==crystalButton)
				{
					isPowder = false;
					showControls();
					isReset = true;
				}
				if (event.getSource()==powderButton)
				{
					isPowder = true;
					showControls();
					isReset = true;
				}
			}				   
		}
		
		/**
		 * DisorderListener listens for the check boxes that highlight a given atomic subtype.
		 */
			private class checkboxListener implements ItemListener
			{
				public void itemStateChanged (ItemEvent event)
				{
					isSimpleColor = colorBox.isSelected();
					rd.setColors(isSimpleColor);
					rd.recolorPanels();
				}
			}


		/**
	 	* keyinputListener responds to keyboard commands.
	 	*/
		private class keyinputListener implements KeyListener
		{
			public void keyPressed(KeyEvent e) {}
			public void keyReleased(KeyEvent e) {}
			public void keyTyped(KeyEvent e)
			{
				char key = e.getKeyChar();
//	            System.out.println("key character = '" + key + "'");
	            
	            if(key == 'r' || key == 'R')
            	{
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
					rd.resetSliders();
					isReset = true;
            	}
            	else if(key == 'z' || key == 'Z')
            	{
            		rd.zeroSliders();
            		rd.isChanged = true;
            	}
            	else if(key == 'i' || key == 'I')
            	{
            		rd.resetSliders();
            		rd.isChanged = true;
            	}
       			else if(key == 's' || key == 'S')
       			{
       				rd.toggleIrrepSliders();
       				rd.isChanged = true;
       			}
			}
		}
			

		/**
	 	* mouseoverListener responds to mouseover-peak events.
	 	*/
		private class mouseoverListener implements MouseMotionListener
		{
			public void mouseDragged(MouseEvent e) {}
			public void mouseMoved(MouseEvent e)
			{
		    	double x = e.getX();
		    	double y = e.getY();
		    	
		    	if (isPowder)
		    		mousePeak(x, y, 2);
		    	else
		    		mousePeak(x, y, 6);
//			 	System.out.println("x = "+x+", y = "+y);
			}
		}	

		/**
		* viewListener class listens for the applet buttons and the inside methods
		* specify the viewing angles.
		*/
		private class viewListener implements ActionListener
		{
			public void actionPerformed (ActionEvent event)
			{
				if (event.getSource()==applyView)
					isReset = true;
				if (event.getSource()==saveImage)
					ImageSaver.saveImageFile(im, IsoDiffractApplet.this);
			}
		}	


/**
 6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666
 6																						   6
 6 The sixth (last) section has the methods called from initialize() which create the GUI. 6
 6																						   6
 6666666666666666666666666666666666666666666666666666666666666666666666666666666666666666666
 **/

		/** Sets up the applet window and scroll and control panels*/
		private void initView()
		{
			setSize(rd.appletWidth, rd.appletHeight);//the total area of the applet will be 1000x500 leaving 500x500 for the slider bar half
			setLayout(new BorderLayout());//allows for adding slider panel to the side (east or west) of rendering area
			setBackground(Color.WHITE);
			rd.initPanels();
			loadControls();
			showControls();
			this.add(rd.scrollPane,BorderLayout.EAST);//add to east of Applet
			this.add(rd.controlPane,BorderLayout.SOUTH);//add to east of Applet
		}

		/** creates the components of the control panel */
		private void loadControls()
		{
			dButton = new JRadioButton("d", false);
			dButton.setHorizontalAlignment(JRadioButton.LEFT);
			dButton.setVerticalAlignment(JRadioButton.CENTER);
			dButton.setFocusable(false);
			dButton.addItemListener(new buttonListener());
			dButton.setBackground(Color.WHITE);
			dButton.setForeground(Color.BLACK);
			dButton.setBorderPainted(false);
			qButton = new JRadioButton("q", false);
			qButton.setHorizontalAlignment(JRadioButton.LEFT);
			qButton.setVerticalAlignment(JRadioButton.CENTER);
			qButton.setFocusable(false);
			qButton.addItemListener(new buttonListener());
			qButton.setBackground(Color.WHITE);
			qButton.setForeground(Color.BLACK);
			qButton.setBorderPainted(false);
			tButton = new JRadioButton("2th", true);
			tButton.setHorizontalAlignment(JRadioButton.LEFT);
			tButton.setVerticalAlignment(JRadioButton.CENTER);
			tButton.setFocusable(false);
			tButton.addItemListener(new buttonListener());
			tButton.setBackground(Color.WHITE);
			tButton.setForeground(Color.BLACK);
			tButton.setBorderPainted(false);
			rangeButtons = new ButtonGroup();
			rangeButtons.add(dButton);
			rangeButtons.add(qButton);
			rangeButtons.add(tButton);

			parentButton = new JRadioButton("Parent", true);
			parentButton.setHorizontalAlignment(JRadioButton.LEFT);
			parentButton.setVerticalAlignment(JRadioButton.CENTER);
			parentButton.setFocusable(false);
			parentButton.addItemListener(new buttonListener());
			parentButton.setBackground(Color.WHITE);
			parentButton.setForeground(Color.BLACK);
			parentButton.setBorderPainted(false);
			superButton = new JRadioButton("Super", false);
			superButton.setHorizontalAlignment(JRadioButton.LEFT);
			superButton.setVerticalAlignment(JRadioButton.CENTER);
			superButton.setFocusable(false);
			superButton.addItemListener(new buttonListener());
			superButton.setBackground(Color.WHITE);
			superButton.setForeground(Color.BLACK);
			superButton.setBorderPainted(false);
			hklButtons = new ButtonGroup();
			hklButtons.add(parentButton);
			hklButtons.add(superButton);
			
			crystalButton = new JRadioButton("Crystal", true);
			crystalButton.setHorizontalAlignment(JRadioButton.LEFT);
			crystalButton.setVerticalAlignment(JRadioButton.CENTER);
			crystalButton.setFocusable(false);
			crystalButton.addItemListener(new buttonListener());
			crystalButton.setBackground(Color.WHITE);
			crystalButton.setForeground(Color.BLACK);
			crystalButton.setBorderPainted(false);
			powderButton = new JRadioButton("Powder", false);
			powderButton.setHorizontalAlignment(JRadioButton.LEFT);
			powderButton.setVerticalAlignment(JRadioButton.CENTER);
			powderButton.setFocusable(false);
			powderButton.addItemListener(new buttonListener());
			powderButton.setBackground(Color.WHITE);
			powderButton.setForeground(Color.BLACK);
			powderButton.setBorderPainted(false);
			pscButtons = new ButtonGroup();
			pscButtons.add(crystalButton);
			pscButtons.add(powderButton);

			mouseoverLabel = new JLabel("");
			
			xrayButton = new JRadioButton("Xray", true);
			xrayButton.setHorizontalAlignment(JRadioButton.LEFT);
			xrayButton.setVerticalAlignment(JRadioButton.CENTER);
			xrayButton.setFocusable(false);
			xrayButton.addItemListener(new buttonListener());
			xrayButton.setBackground(Color.WHITE);
			xrayButton.setForeground(Color.BLACK);
			xrayButton.setBorderPainted(false);
			neutronButton = new JRadioButton("Neut", false);
			neutronButton.setHorizontalAlignment(JRadioButton.LEFT);
			neutronButton.setVerticalAlignment(JRadioButton.CENTER);
			neutronButton.setFocusable(false);
			neutronButton.addItemListener(new buttonListener());
			neutronButton.setBackground(Color.WHITE);
			neutronButton.setForeground(Color.BLACK);
			neutronButton.setBorderPainted(false);
			xnButtons = new ButtonGroup();
			xnButtons.add(xrayButton);
			xnButtons.add(neutronButton);
			
			colorBox = new JCheckBox("Color", false);
			colorBox.setHorizontalAlignment(JCheckBox.LEFT);
			colorBox.setVerticalAlignment(JCheckBox.CENTER);
			colorBox.setFocusable(false);
			colorBox.setBackground(Color.WHITE);
			colorBox.setForeground(Color.BLACK);
			colorBox.addItemListener(new checkboxListener());
			
			hOTxt.setText("0");
			hOTxt.setMargin(new Insets(-2,0,-1,-10));
			kOTxt.setText("0");
			kOTxt.setMargin(new Insets(-2,0,-1,-10));
			lOTxt.setText("0");
			lOTxt.setMargin(new Insets(-2,0,-1,-10));
			hHTxt.setText("1");
			hHTxt.setMargin(new Insets(-2,0,-1,-10));
			kHTxt.setText("0");
			kHTxt.setMargin(new Insets(-2,0,-1,-10));
			lHTxt.setText("0");
			lHTxt.setMargin(new Insets(-2,0,-1,-10));
			hVTxt.setText("0");
			hVTxt.setMargin(new Insets(-2,0,-1,-10));
			kVTxt.setText("1");
			kVTxt.setMargin(new Insets(-2,0,-1,-10));
			lVTxt.setText("0");
			lVTxt.setMargin(new Insets(-2,0,-1,-10));
			qTxt.setText("4");
			qTxt.setMargin(new Insets(-2,0,-1,-10));
			
			wavTxt.setText("1.54");
			wavTxt.setMargin(new Insets(-2,0,-1,0));
			minTxt.setText("5.00");
			minTxt.setMargin(new Insets(-2,0,-1,0));
			maxTxt.setText("60.00");
			maxTxt.setMargin(new Insets(-2,0,-1,0));
			fwhmTxt.setText("0.200");
			fwhmTxt.setMargin(new Insets(-2,0,-1,0));
			zoomTxt.setText("1.0");
			zoomTxt.setMargin(new Insets(-2,0,-1,0));

			horizLabel = new JLabel("   Horiz");
			upperLabel = new JLabel("   Upper");
			centerLabel = new JLabel("Center");
			qrangeLabel = new JLabel("   Q Range");
			wavLabel = new JLabel("   Wave");
			minLabel = new JLabel("   Min");
			maxLabel = new JLabel("   Max");
			fwhmLabel = new JLabel("   Res");
			zoomLabel = new JLabel("   Zoom");

			applyView.setFocusable(false);
			applyView.setMargin(new Insets(-3,0,-2,0));
			applyView.setHorizontalAlignment(JButton.LEFT);
			applyView.setVerticalAlignment(JButton.CENTER);
			applyView.addActionListener(new viewListener());
			
			saveImage.setFocusable(false);
			saveImage.setMargin(new Insets(-3,0,-2,0));
			saveImage.setHorizontalAlignment(JButton.LEFT);
			saveImage.setVerticalAlignment(JButton.CENTER);
			saveImage.addActionListener(new viewListener());

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
			botControlPanel.add(new JLabel("      "));
			botControlPanel.add(applyView);
			botControlPanel.add(saveImage);
			
			rd.controlPanel.add(topControlPanel);
			rd.controlPanel.add(botControlPanel);
		}
		
		
		/** loads the control components into the control panel */
		private void showControls()
		{
			if (isPowder)
			{
				hOTxt.setVisible(false);
				kOTxt.setVisible(false);
				lOTxt.setVisible(false);
				hHTxt.setVisible(false);
				kHTxt.setVisible(false);
				lHTxt.setVisible(false);
				hVTxt.setVisible(false);
				kVTxt.setVisible(false);
				lVTxt.setVisible(false);
				qTxt.setVisible(false);
				horizLabel.setVisible(false);
				upperLabel.setVisible(false);
				centerLabel.setVisible(false);
				qrangeLabel.setVisible(false);
				wavTxt.setVisible(true);
				minTxt.setVisible(true);
				maxTxt.setVisible(true);
				fwhmTxt.setVisible(true);
				zoomTxt.setVisible(true);
				wavLabel.setVisible(true);
				minLabel.setVisible(true);
				maxLabel.setVisible(true);
				fwhmLabel.setVisible(true);
				zoomLabel.setVisible(true);
				applyView.setVisible(true);
				
				if (isMouseOver)
				{
					mouseoverLabel.setVisible(true);
					parentButton.setVisible(false);
					superButton.setVisible(false);
					dButton.setVisible(false);
					qButton.setVisible(false);
					tButton.setVisible(false);
					xrayButton.setVisible(false);
					neutronButton.setVisible(false);
					colorBox.setVisible(false);
					crystalButton.setVisible(false);
					powderButton.setVisible(false);
				}
				else
				{
					mouseoverLabel.setVisible(false);
					parentButton.setVisible(false);
					superButton.setVisible(false);
					dButton.setVisible(true);
					qButton.setVisible(true);
					tButton.setVisible(true);
					xrayButton.setVisible(true);
					neutronButton.setVisible(true);
					colorBox.setVisible(rd.needSimpleColor);
					crystalButton.setVisible(true);
					powderButton.setVisible(true);
				}
			}
			else
			{
				hOTxt.setVisible(true);
				kOTxt.setVisible(true);
				lOTxt.setVisible(true);
				hHTxt.setVisible(true);
				kHTxt.setVisible(true);
				lHTxt.setVisible(true);
				hVTxt.setVisible(true);
				kVTxt.setVisible(true);
				lVTxt.setVisible(true);
				qTxt.setVisible(true);
				horizLabel.setVisible(true);
				upperLabel.setVisible(true);
				centerLabel.setVisible(true);
				qrangeLabel.setVisible(true);
				wavTxt.setVisible(false);
				minTxt.setVisible(false);
				maxTxt.setVisible(false);
				fwhmTxt.setVisible(false);
				zoomTxt.setVisible(false);
				wavLabel.setVisible(false);
				minLabel.setVisible(false);
				maxLabel.setVisible(false);
				fwhmLabel.setVisible(false);
				zoomLabel.setVisible(false);
				applyView.setVisible(true);
				
				if (isMouseOver)
				{
					mouseoverLabel.setVisible(true);
					parentButton.setVisible(false);
					superButton.setVisible(false);
					dButton.setVisible(false);
					qButton.setVisible(false);
					tButton.setVisible(false);
					xrayButton.setVisible(false);
					neutronButton.setVisible(false);
					colorBox.setVisible(false);
					crystalButton.setVisible(false);
					powderButton.setVisible(false);
				}
				else
				{
					parentButton.setVisible(true);
					superButton.setVisible(true);
					dButton.setVisible(false);
					qButton.setVisible(false);
					tButton.setVisible(false);
					xrayButton.setVisible(true);
					neutronButton.setVisible(true);
					colorBox.setVisible(rd.needSimpleColor);
					crystalButton.setVisible(true);
					powderButton.setVisible(true);
					
				}
			}
		}

	public String getParameter(String key) {
		switch (key) {
		case "isoData":
			break;
		}
		return "";
	}
	
	/** This data reader has two modes of operation.  For data files, it uses a buffered reader to get the
	 * data into a single string.  The alternative is to read a single-string data sequence directly from
	 * the html file that calls the applet (injected as isoData).  The LoadVariables class then has a method that
	 * parses this string. 
	 */
		private void readFile()
		{			
			if (false && readMode)
				dataString = getParameter("isoData");
			else
			{
				try
				{
					File f = new File(whichdatafile);
					System.out.println (f.getAbsoluteFile());
					
					BufferedReader br = new BufferedReader(new FileReader(f));// this reads the data
					dataString = br.readLine()+"\n";//scrap the first data line of text
					while (br.ready())  // previously used `for (int i=1;br.ready();i++)
						dataString += br.readLine()+"\n";
					br.close();
				} //close try
				catch (IOException exception)
				{
					System.out.println("Oops. File not found.");
				}
			}
		}
} // end isoDiffractApplet.class
