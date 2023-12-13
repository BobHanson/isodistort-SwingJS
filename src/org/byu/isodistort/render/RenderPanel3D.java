//
// Copyright 2001 Ken Perlin

package org.byu.isodistort.render;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.MemoryImageSource;

import javax.swing.JPanel;

import org.byu.isodistort.local.IsoApp;
import org.byu.isodistort.local.Matrix;

/**
 * Provides an applet interface to the {@link Renderer}. It also implements
 * control features dealing with mouse and keyboard interaction. Extend this
 * class to create an interactive web applet.
 * 
 * Bob Hanson 2023.12.10 refactored to be its own JPanel, adapting dynamically to its size.
 * 
 * Was "RenderPanel", but it's really only for 3D rendering (IsoDistort).
 * IsoDiffractApp has its own private 2D RenderPanel
 * 
 * 
 * @see Renderer
 * @author Ken Perlin 2001
 */

public class RenderPanel3D extends JPanel
// APS (April 2009): edits thanks to: http://www.dgp.toronto.edu/~mjmcguff/learn/java/04-mouseInput/
		implements MouseListener, MouseMotionListener {

	/**
	 * Image memory source object
	 */
	protected MemoryImageSource mis;

//	/**
//	 * Secondary frambuffer for displaying additional information
//	 */
//	protected BufferedImage bufferIm;

	protected double fov;

	public RenderPanel3D(IsoApp app) {
		this.app = app;
		addMouseListener(this);
		addMouseMotionListener(this);
		initialize();
	}

	public void dispose() {
		app = null;
		removeMouseListener(this);
		removeMouseMotionListener(this);
		renderer = null;
	}

	public void changeFOV(double FOV) {
		fov = FOV;
	}

	// --- PRIVATE DATA FIELDS

	/**
	 * Current mouse position
	 */
	private int mx, my;

	/**
	 * Flag to force a renderer refresh when true.
	 */
//   protected boolean isDamage = true; // WHETHER WE NEED TO RECOMPUTE IMAGE

	/**
	 * Holds current system time. Used to compute time elapsed between frames.
	 */
	private double currentTime = 0;

	/**
	 * Measures time elapsed from initialization.
	 */
	private double elapsed = 0;

	/**
	 * Contains current frame rate of the renderer
	 */
	protected double frameRate = 20;

	private Matrix matrix[] = new Matrix[10]; // THE MATRIX STACK

	private int top = 0; // MATRIX STACK POINTER
	
	// public String notice = "Copyright 2001 Ken Perlin. All rights reserved.";

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
		setBackground(new Color((int) (r * 255), (int) (g * 255), (int) (b * 255)));
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

//	/**
//	 * Deforms a geometric shape according to the beginning, middle, and end
//	 * parameters in each dimension. For each dimesion the three parameters indicate
//	 * the amount of deformation at each position. 0 - beginning, 1 - middle, 2 -
//	 * end. To indicate infinity (a constant transformation) set two adjacent
//	 * parameters to the same value. Setting all three parameters to the same value
//	 * transforms the shape geometry consistently across the entire axis of the
//	 * parameters.
//	 * 
//	 * @param s  shape object to be deformed
//	 * @param x0 location of beginning of deformation along the x axis
//	 * @param x1 location of beginning of deformation along the x axis
//	 * @param x2 location of beginning of deformation along the x axis
//	 * @param y0 location of beginning of deformation along the y axis
//	 * @param y1 location of beginning of deformation along the y axis
//	 * @param y2 location of beginning of deformation along the y axis
//	 * @param z0 location of beginning of deformation along the z axis
//	 * @param z1 location of beginning of deformation along the z axis
//	 * @param z2 location of beginning of deformation along the z axis
//	 * @return 1 if pull operation was successful, 0 otherwise
//	 * @see Geometry#pull
//	 */
//	public int pull(Geometry s, double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1,
//			double z2) {
//		return s.pull(m(), x0, x1, x2, y0, y1, y2, z0, z1, z2);
//	}

	// --- SYSTEM LEVEL PUBLIC METHODS ---

	private IsoApp app;

	private BufferedImage im;

	public void initialize() {
		renderer = new Renderer();
		world = renderer.getWorld(); // GET ROOT OF GEOMETRY
		for (int i = 0; i < matrix.length; i++)
			matrix[i] = new Matrix();
		identity();
	}

	/**
	 * Euler angles for camera positioning (horizontal and vertical view rotation).
	 */
	public double theta = 0;
	public double phi = 0;
	public double sigma = 0;

//	private synchronized void recalculateSize(int width, int height) {
//		// BH we are now just directly tapping the int[] raster of the
//		// buffered image that will be used to draw into the graphics
//		// Reinitialize the renderer
//		// by passing the off-screen image raster to the renderer
//		im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//		int[] pixels = ((DataBufferInt)im.getRaster().getDataBuffer()).getData();
//		renderer.reinit(width, height, pixels);		
//		// older method was to create an image source
//		// that triggers a copy to an image upon Component.createImage();
////		mis = new MemoryImageSource(width, height, renderer.getPix(), 0, width);
////		mis.setAnimated(true);
////		im = createImage(mis);
//		//bufferIm = (BufferedImage) createImage(width, height);
//	}
	
	/**
	 * Check to see if the renderer is in sync with Java's layout manager. 
	 * 
	 * @return true if dimensions are unchanged
	 */
	private boolean isInSync() {
		return (getWidth() == renderer.W && getHeight() == renderer.H);
	}

	/**
	 * Check for a resize prior to rendering, and then carry out the rendering into
	 * the image raster int[] rgba data buffer
	 */
	public synchronized void updateForDisplay(Component c) {
		if (c != null) {
			// write into the image data buffer
			identity();
			renderer.rotateView(theta, phi, sigma);
			if (!spin)
				theta = phi = sigma = 0;
			renderer.render();
//// BH this parameter is always 1 here
//			if (renderer.meshLevelOfDetail > 1)
//				renderer.meshLevelOfDetail--;
			c.repaint();
		} else if (!isInSync()) {
			// prior to rendering, check for a resize
			int width = getWidth();
			int height = getHeight();
			im = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			int[] pixels = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
			renderer.reinit(width, height, pixels);
		}
	}

	@Override
	public synchronized void paint(Graphics g) {
		super.paint(g);
		if (!isInSync()) {
			// don't paint if we are not ready.
			return;
		}
		g.drawImage(im, 0, 0, null);

		// debugging, testing ....

		double dt = (getCurrentTime() - currentTime);
		elapsed += dt;
		currentTime = getCurrentTime();
		if (elapsed > 0.00001)
			frameRate = 0.9 * frameRate + 0.1 / elapsed;
		elapsed = 0.0;
		if (showFPS) {
			String x = "" + // dt;//
					(int) frameRate + "." + ((int) (frameRate * 10) % 10);
			// in JavaScript we just put this up in the tab
			/**
			 * @j2sNative
			 * 
			 * 			document.title = ""+x
			 */
			{
				g.setColor(Color.white);
				g.fillRect(0, renderer.H - 14, 80, 14);
				g.setColor(Color.black);
				g.drawString(x + " fps ", 1, renderer.H - 1);
			}
		}
	}

//	/**
//	 * Returns xyz world coords of the frontmost object at pixel (x,y)
//	 * 
//	 * @param x   x pixel coordinate
//	 * @param y   y pixel coordinate
//	 * @param xyz output point in world coords
//	 * @return true iff not a background pixel
//	 */
//	public boolean getPoint(int x, int y, double xyz[]) {
//		return renderer.getPoint(x, y, xyz);
//	}
//
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

	private boolean isMouseZooming;

	private double fov0;

	@Override
	public void mouseClicked(MouseEvent e) {
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}

	@Override
	public void mouseExited(MouseEvent e) {
	}

	@Override
	public void mousePressed(MouseEvent e) {
		// necessary for SwingJS if mouse has cliked on the page outside of applet
		
		//System.out.println("RP mouseDown " + e.getButton() + " " + e.getModifiers());

		requestFocus();
		int x = e.getX();
		int y = e.getY();
		renderer.setDragging(true);
		mx = x;
		my = y;
		if (e.isShiftDown()) {
			clearAngles();
			setRotationAxis(4);
			isMouseZooming = true;
		}

	}

	@Override
	public void mouseReleased(MouseEvent e) {
		renderer.setDragging(false);
		if (isMouseZooming) {
			isMouseZooming = false;
			clearAngles();
			setRotationAxis(0);
		}
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		if (isMouseZooming && !e.isShiftDown()) {
			isMouseZooming = false;
			clearAngles();
			setRotationAxis(0);
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		
		
		int x = e.getX();
		int y = e.getY();
		
		//System.out.println("RP mouse " + e.getButton() + " " + e.getModifiers() + " " + e.getMouseModifiersText(e.getModifiers()) + " " + Integer.toHexString(e.getModifiersEx()) + " " + e.getModifiersExText(e.getModifiersEx()));

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
		} else {
			// panning
			
			// If we want this to be only at right click, then
			// we would need to check e.getModifiers with
			// MouseEvent.BUTTON2_MASK or MouseEvent.BUTTON3_MASK
			theta = phi = sigma = 0;

			double shiftRate = 0.01 * fov;
			double shiftX = shiftRate * (x - mx);
			double shiftY = shiftRate * (y - my);

			mx = x;
			my = y;

			push();
			{
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
			}
			pop();
		}
		app.updateDisplay();
	}

	// --- PRIVATE METHODS

	private double getCurrentTime() {
		return System.currentTimeMillis() / 1000.;
	}

	// additional IsoPanel-public methods
	
	public void setCamera(double t, double p) {
		renderer.setCamera(t, p);
	}

	public boolean isSpinning() {
		return spin;
	}

	public double getFrameRate() {
		return frameRate;
	}

	public Renderer getRenderer() {
		return renderer;
	}

	public void clearAngles() {
		theta = phi = sigma = 0;
	}

	public Material newMaterial() {
		return new Material(renderer);
	}

	public void invert() {
		invert = -invert;
	}

	public void clearOffsets() {
		xOff = 0;
		yOff = 0;
		zOff = 0;
		invert = 1;
	}

	public void transformWorld() {
		for (int i = world.child.length; --i >= 0;)
			if (world.child(i) != null)
				transform(world.child(i));
	}

	public void setRotationAxis(int i) {
		rotAxis = i;
	}

	public void setSpinning(boolean spin) {
		this.spin = spin;
	}

	public BufferedImage getImage() {
		BufferedImage bi = new BufferedImage(im.getWidth(null), im.getHeight(null), BufferedImage.TYPE_INT_ARGB); 
//		for(int i = 0; i < image.getWidth(null);i++) 
//		for(int j = 0; j < image.getHeight(null);j++) 
//		bufferedImage.setRGB(i, j, Color.WHITE.getRGB()); 
		Graphics2D g = bi.createGraphics();
		//g.setBackground(Color.WHITE); 
		g.drawImage(im, null, null); 
		g.dispose();
		// how can this take any time? It's a standard image
		//waitForImage(bufferedImage, im.getWidth(null), im.getHeight(null)); 
		return bi; 
	}

	public void initializeSettings(double perspectivescaler, double scdSize) {
		double fl = 10;
		double fov = 2 * perspectivescaler * scdSize / fl;
		fov0 = fov;
		setBgColor(1, 1, 1);// background color: white
		setFOV(fov);// field of view
		setFL(fl);// focal length: zoomed way out
		changeFOV(fov);
		setCamera(0, 0);
		// Define position and color of light source (x, y, z, r, g, b)
		double intensity = 0.38;
		addLight(Double.NaN, 0,0,0,0,0);
		addLight(.5, .5, .5, 1.7 * intensity, 1.7 * intensity, 1.7 * intensity);
		addLight(-.5, .5, .5, intensity, intensity, intensity);
		addLight(.5, -.5, .5, intensity, intensity, intensity);
		addLight(-.5, -.5, .5, intensity, intensity, intensity);
	}

	public void resetView() {
		setCamera(0, 0);
		setFOV(fov0);
	}

//	private static void waitForImage(BufferedImage buf, int width, int height)
//	{
//		while ((buf.getHeight() == 0)&&(buf.getWidth() == 0))
//		{
//			try
//			{
//				Thread.sleep(300);
//			} catch (InterruptedException e)
//			{
//
//			}
//		}
//	}
//

}
