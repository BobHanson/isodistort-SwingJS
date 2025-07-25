//
// Copyright 2001 Ken Perlin

package org.byu.isodistort2.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.MemoryImageSource;

import org.byu.isodistort2.ISOApplet;

/**
 * Provides an applet interface to the {@link Renderer}. It also implements
 * control features dealing with mouse and keyboard interaction. Extend this
 * class to create an interactive web applet.
 * 
 * @see Renderer
 * @author Ken Perlin 2001
 */

public abstract class RenderApplet extends ISOApplet
// APS (April 2009): edits thanks to: http://www.dgp.toronto.edu/~mjmcguff/learn/java/04-mouseInput/
		implements MouseListener, MouseMotionListener {

	private static final long serialVersionUID = 1L;

	
	abstract protected void initialize();


	/** restabit rests the thread for the indicated number of milliseconds. */
	int restabit = 10;

	/**
	 * Dimensions of renderer area, so that the render area can be independent of
	 * the applet size. -David Tanner
	 */
	public int renderAreaX = 600, renderAreaY = 585;

	public double fov, fov0;

	public void changeFOV(double FOV) {
		fov = FOV;
	}

	public String notice = "Copyright 2001 Ken Perlin. All rights reserved.";

	// --- PUBLIC DATA FIELDS

	/**
	 * {@link Renderer} object
	 */
	public Renderer renderer;

	/**
	 * root of the scene {@link Geometry}
	 */
	public Geometry world;

	/**
	 * Flag that determines whether to display current frame rate.
	 */
	public boolean showFPS = true;

	/** These doubles determine how far off center the image is */
	protected double xOff = 0, yOff = 0, zOff = 0;

	/** This int determines whether or not the panning is normal or inverted */
	protected int invert = 1;

//--- PUBLIC METHODS

//   /**
//     Forces a refresh of the renderer. Sets isDamage true.
//   */
//   public void damage()
//   {
//      renderer.refresh();
//      isDamage = true;
//   }

	/**
	 * Sets the field of view value.
	 * 
	 * @param value
	 * @see Renderer#setFOV(double value)
	 */
	public void setFOV(double value) {
		renderer.setFOV(value);
	}

	/**
	 * Sets the camera's focal length.
	 * 
	 * @param value focal length
	 * @see Renderer#setFL(double value)
	 */
	public void setFL(double value) {
		renderer.setFL(value);
	}

	/**
	 * Sets the background color ( RGB values range: 0..1).
	 * 
	 * @param r red component 0..1
	 * @param g green component 0..1
	 * @param b blue component 0..1
	 */
	public void setBgColor(double r, double g, double b) {
		renderer.setBgColor(r, g, b);
	}

	/**
	 * Adds light source with direction (x, y, z) & color (r, g, b).
	 * 
	 * Arguments x,y,z indicate light direction. Arguments r,g,b indicate light
	 * direction.
	 * 
	 * @see Renderer#addLight(double x,double y,double z, double r,double g,double
	 *      b)
	 */
	public void addLight(double x, double y, double z, // ADD A LIGHT SOURCE
			double r, double g, double b) {
		renderer.addLight(x, y, z, r, g, b);
	}

	// PUBLIC METHODS TO LET THE PROGRAMMER MANIPULATE A MATRIX STACK

	/**
	 * Sets current matrix to the identity matrix.
	 */
	public void identity() {
		m().identity();
	}

	/**
	 * Returns the matrix at the top of the stack.
	 * 
	 * @return the top matrix on the stack
	 */
	public Matrix m() {
		return matrix[top];
	}

	/**
	 * Pops the top matrix from the stack.
	 */
	public void pop() {
		top--;
	}

	/**
	 * Pushes a copy of the top matrix onto the stack.
	 */
	public void push() {
		matrix[top + 1].copy(matrix[top]);
		top++;
	}

	/**
	 * Rotates the top matrix around the X axis by angle t (radians).
	 * 
	 * @param t angle in radians
	 */
	public void rotateX(double t) {
		m().rotateX(t);
	}

	/**
	 * Rotates the top matrix around the Y axis by angle t (radians).
	 * 
	 * @param t angle in radians
	 */
	public void rotateY(double t) {
		m().rotateY(t);
	}

	/**
	 * Rotates the top matrix around the Z axis by angle t (radians).
	 * 
	 * @param t angle in radians
	 */
	public void rotateZ(double t) {
		m().rotateZ(t);
	}

	/**
	 * Scales the top matrix by x, y, z in their respective dimensions.
	 * 
	 * @param x x scale factor
	 * @param y y scale factor
	 * @param z z scale factor
	 */
	public void scale(double x, double y, double z) {
		m().scale(x, y, z);
	}

	public String getScale() {
		return m().getScale();
	}

	/**
	 * Applies the top transformation matrix to {@link Geometry} s.
	 * 
	 * @param s Geometry object
	 */
	public void transform(Geometry s) {
		s.setMatrix(m());
	}

	/**
	 * Translates the top matrix by vector v.
	 * 
	 * @param v an array of three doubles representing translations in the x,y,z
	 *          directions.
	 */
	public void translate(double v[]) {
		translate(v[0], v[1], v[2]);
	}

	/**
	 * Translates the top matrix by x, y, z.
	 * 
	 * @param x - translation in the x direction.
	 * @param y - translation in the y direction.
	 * @param z - translation in the z direction.
	 */
	public void translate(double x, double y, double z) {
		m().translate(x, y, z);
	}

	// PUBLIC METHODS TO LET THE PROGRAMMER DEFORM AN OBJECT

	/**
	 * Deforms a geometric shape according to the beginning, middle, and end
	 * parameters in each dimension. For each dimesion the three parameters indicate
	 * the amount of deformation at each position. 0 - beginning, 1 - middle, 2 -
	 * end. To indicate infinity (a constant transformation) set two adjacent
	 * parameters to the same value. Setting all three parameters to the same value
	 * transforms the shape geometry consistently across the entire axis of the
	 * parameters.
	 * 
	 * @param s  shape object to be deformed
	 * @param x0 location of beginning of deformation along the x axis
	 * @param x1 location of beginning of deformation along the x axis
	 * @param x2 location of beginning of deformation along the x axis
	 * @param y0 location of beginning of deformation along the y axis
	 * @param y1 location of beginning of deformation along the y axis
	 * @param y2 location of beginning of deformation along the y axis
	 * @param z0 location of beginning of deformation along the z axis
	 * @param z1 location of beginning of deformation along the z axis
	 * @param z2 location of beginning of deformation along the z axis
	 * @return 1 if pull operation was successful, 0 otherwise
	 * @see Geometry#pull
	 */
	public int pull(Geometry s, double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1,
			double z2) {
		return s.pull(m(), x0, x1, x2, y0, y1, y2, z0, z1, z2);
	}

	// --- SYSTEM LEVEL PUBLIC METHODS ---

	int pix[];

	public int[] getPix() {
		return pix;
	}

	/** Set size of renderer within applet. [width, height] -David Tanner */
	public void setRenderArea(int inputX, int inputY) {
		renderAreaX = inputX;
		renderAreaY = inputY;
	}

	/**
	 * Initializes the applet and internal variables. To initialize components of
	 * the application program use {@link #initialize()}.
	 * 
	 * @see #initialize()
	 */
	public void init() {
		addMouseListener(this);
		addMouseMotionListener(this);
		W = renderAreaX;
		H = renderAreaY;
		renderer = new Renderer();
		pix = renderer.init(W, H);
		mis = new MemoryImageSource(W, H, pix, 0, W);// subtracting one doubles the immages?
		mis.setAnimated(true);
		im = createImage(mis);
		bufferIm = createImage(W, H);// subtracting from here did work until spinning
		startTime = getCurrentTime();
		world = renderer.getWorld(); // GET ROOT OF GEOMETRY
		for (int i = 0; i < matrix.length; i++)
			matrix[i] = new Matrix();
		identity();
		initialize();
	}

	/**
	 * Euler angles for camera positioning (horizontal and vertical view rotation).
	 */
	public double theta = 0;
	public double phi = 0;
	public double sigma = 0;

	protected synchronized void moreRunStuff() {
		// LET THE APPLICATION PROGRAMMER MOVE THINGS INTO PLACE
		identity(); // APPLIC. MATRIX STARTS UNTRANSFORMED
		
		renderer.rotateView(theta, phi, sigma);

		if (!spin)
			theta = phi = sigma = 0;

		// SHADE AND SCAN CONVERT GEOMETRY INTO FRAME BUFFER
		renderer.render();

		// KEEP REFINING LEVEL OF DETAIL UNTIL PERFECT (WHEN LOD=1)
		if (renderer.lod > 1)
			renderer.lod--;

		// WRITE RESULTS TO THE SCREEN
		mis.newPixels(0, 0, W, H, true);
	}

	/**
	 * Updates the image buffer to output device.
	 * 
	 * @param g Specifies the output device.
	 */
	public synchronized void update(Graphics g) {
		int currentWidth = renderAreaX;// originally "=bounds().width;" -David Tanner
		int currentHeight = renderAreaY;// originally "=bounds().height;" -David Tanner
		if (currentWidth != W || currentHeight != H) // allow for dynamic resizing of the applet
		{
			recalculateSize(currentWidth, currentHeight);// subtracting here makes things phase in and out
		}

		// MEASURE ELAPSED TIME AND FRAMERATE
		elapsed += getCurrentTime() - currentTime;
		currentTime = getCurrentTime();
		if (elapsed > 0.00001)
			frameRate = 0.9 * frameRate + 0.1 / elapsed;
		elapsed = 0.0;

		Graphics G = bufferIm.getGraphics();
		G.drawImage(im, 0, 0, null);
		if (showFPS) {
			G.setColor(Color.white);
			G.fillRect(0, H - 14, 80, 14);
			G.setColor(Color.black);
			G.drawString((int) frameRate + "." + ((int) (frameRate * 10) % 10) + " fps ", 1, H - 1);
		}
		g.drawImage(bufferIm, 0, 0, null);

	
		if (t0 != 0)
			System.out.println("Time to load, render, and paint: "+(System.currentTimeMillis() - t0)+" ms");			
		t0 = 0;

}

	private synchronized void recalculateSize(int currentWidth, int currentHeight) {
		// Change the size
		W = currentWidth;
		H = currentHeight;
		// Reinitialize the renderer
		pix = renderer.reinit(W, H);

		mis = new MemoryImageSource(W, H, pix, 0, W);
		mis.setAnimated(true);
		im = createImage(mis);
		bufferIm = createImage(W, H);
	}

	/**
	 * Returns xyz world coords of the frontmost object at pixel (x,y)
	 * 
	 * @param x   x pixel coordinate
	 * @param y   y pixel coordinate
	 * @param xyz output point in world coords
	 * @return true iff not a background pixel
	 */
	public boolean getPoint(int x, int y, double xyz[]) {
		return renderer.getPoint(x, y, xyz);
	}

	/**
	 * Returns the Geometry of the frontmost object at the point (x, y) in the image
	 * (like a z-buffer value of geometries).
	 * 
	 * @param x x coordinate in the image
	 * @param y y coordinate in the image
	 * @return the geometry of the foremost object at that location
	 */
	public Geometry getGeometry(int x, int y) {
		if (renderer.bufferg == false) {
			renderer.bufferg = true;
//         isDamage = true;
		}
		return renderer.getGeometry(x, y);
	}

	/**
	 * Flag chooses x,y,z-Rotate modes.
	 */
	public int rotAxis = 0; // Branton Campbell

	/**
	 * Flag controls continuous spin mode.
	 */
	public boolean spin = false; // Branton Campbell

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseEntered(MouseEvent e) {
	}

	public void mouseExited(MouseEvent e) {
	}

	public void mousePressed(MouseEvent e) {
		// necessary for SwingJS if mouse has cliked on the page outside of applet
		requestFocus();
		int x = e.getX();
		int y = e.getY();
		renderer.setDragging(true);
		mx = x;
		my = y;
	}

	public void mouseReleased(MouseEvent e) {
		renderer.setDragging(false);
	}

	public void mouseMoved(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		int x = e.getX();
		int y = e.getY();
		// Compare the int representing which buttons are down
		// with the representation of button 1 being down
		if ((e.getModifiers() & MouseEvent.BUTTON1_MASK) != 0) {

			double spinrate;
			if (spin)
				spinrate = 0.0003;
			else
				spinrate = 0.006;
			if (renderer.isDragging()) {
				switch (rotAxis) {
				case 0:
					phi += spinrate * (double) (y - my); // VERTICAL VIEW ROTATION
					theta += spinrate * (double) (x - mx); // VERTICAL VIEW ROTATION
					sigma = 0;
					break;
				case 1:
					phi += spinrate * (double) (y - my);
					theta = 0;
					sigma = 0;
					break;
				case 2:
					phi = 0;
					theta += spinrate * (double) (x - mx);
					sigma = 0;
					break;
				case 3:
					phi = 0;
					theta = 0;
					sigma += -spinrate * (double) ((x - mx) * (256 - y) - (y - my) * (256 - x))
							/ (double) (1 + Math.sqrt((256 - x) * (256 - x) + (256 - y) * (256 - y)));
					break;
				case 4:
					setFOV(fov = fov * (1 + (y - my) * 0.004));// y-direction motion changes field of view (zoom).
																// -David Tanner
					phi = 0;
					theta = 0;
					sigma = 0;
					break;
				}
				mx = x;
				my = y;
			}
		}
		// If we want this to be only at right click, then
		// we would need to check e.getModifiers with
		// MouseEvent.BUTTON2_MASK or MouseEvent.BUTTON3_MASK
		else {
			theta = phi = sigma = 0;

			double shiftRate = 0.01 * fov;
			double shiftX = shiftRate * (x - mx);
			double shiftY = shiftRate * (y - my);

			mx = x;
			my = y;

			push();
			identity();
			xOff += invert * shiftX * renderer.getCamera().get(0, 0);
			yOff += invert * shiftX * renderer.getCamera().get(0, 1);
			zOff += invert * shiftX * renderer.getCamera().get(0, 2);

			xOff -= invert * shiftY * renderer.getCamera().get(1, 0);
			yOff -= invert * shiftY * renderer.getCamera().get(1, 1);
			zOff -= invert * shiftY * renderer.getCamera().get(1, 2);
			translate(xOff, yOff, zOff);
			for (int i = 0; i < 16; i++)
				if (world.child(i) != null)
					transform(world.child(i));
			pop();
		}
		updateDisplay();
	}

	// --- PRIVATE METHODS

	// GET THE CURRENT TIME IN SECONDS
	private double getCurrentTime() {
		return System.currentTimeMillis() / 1000.;
	}

	// --- PRIVATE DATA FIELDS

	/**
	 * Current mouse position
	 */
	protected int mx, my;

	/**
	 * Image memory source object
	 */
	private MemoryImageSource mis;

	/**
	 * Image width
	 */
	protected int W;

	/**
	 * Image height
	 */
	protected int H;

	/**
	 * Image framebuffer
	 */
	protected Image im; // IMAGE OF MEMORY SOURCE OBJECT

	/**
	 * Secondary frambuffer for displaying additional information
	 */
	protected Image bufferIm;

	/**
	 * Flag to force a renderer refresh when true.
	 */
//   protected boolean isDamage = true; // WHETHER WE NEED TO RECOMPUTE IMAGE

	/**
	 * Holds actual time of initialization.
	 */
	protected double startTime = 0;

	/**
	 * Holds current system time. Used to compute time elapsed between frames.
	 */
	protected double currentTime = 0;

	/**
	 * Measures time elapsed from initialization.
	 */
	protected double elapsed = 0;

	/**
	 * Contains current frame rate of the renderer
	 */
	protected double frameRate = 20;

	private Matrix matrix[] = new Matrix[10]; // THE MATRIX STACK
	private int top = 0; // MATRIX STACK POINTER

	
	public void repaint() {
		super.repaint();
	}
}
