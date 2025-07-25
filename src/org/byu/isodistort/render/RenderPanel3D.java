//
// Copyright 2001 Ken Perlin

package org.byu.isodistort.render;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

import javax.swing.JPanel;

import org.byu.isodistort.IsoDistortApp;
import org.byu.isodistort.local.Iso3DApp.IsoRenderPanel;

/**
 * Provides an applet interface to the {@link Renderer}. It also implements
 * control features dealing with mouse and keyboard interaction. Extend this
 * class to create an interactive web applet.
 * 
 * Bob Hanson 2023.12.10 refactored to be its own JPanel, adapting dynamically
 * to its size.
 * 
 * Was "RenderPanel", but it's really only for 3D rendering (IsoDistort).
 * IsoDiffractApp has its own private 2D RenderPanel
 * 
 * 
 * @see Renderer
 * @author Ken Perlin 2001
 */

public class RenderPanel3D extends JPanel implements IsoRenderPanel {

	/**
	 * Just testing to see if it is ok to use Jmol zoom direction
	 */
	private static boolean useJmolZoomDirection = true;
	
	public static class IsoMaterial extends Material {

		public IsoMaterial(Renderer renderer) {
			super(renderer);
		}

		public IsoMaterial setColor(Color c) {
			setDiffuse(c.getRed() / 255, c.getGreen() / 255, c.getBlue() / 255);
			return this;
		}

	}

	// APS (April 2009): edits thanks to:
	// http://www.dgp.toronto.edu/~mjmcguff/learn/java/04-mouseInput/

	/**
	 * Flag chooses x,y,z-Rotate modes.
	 */
	private int rotAxis = ROTATE_XYZ; // Branton Campbell

	/**
	 * Flag controls continuous spin mode.
	 */
	private boolean spin = false; // Branton Campbell

	private boolean isMouseZooming;

	private double fov0;

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

	private Matrix3D matrix[] = new Matrix3D[10]; // THE MATRIX STACK

	private int top = 0; // MATRIX STACK POINTER

	// private String notice = "Copyright 2001 Ken Perlin. All rights reserved.";

	/**
	 * {@link Renderer} object
	 */
	private Renderer renderer;

	/**
	 * root of the scene {@link Geometry}
	 */
	private Geometry world;

	public Geometry getWorld() {
		return world;
	}

	/**
	 * Flag that determines whether to display current frame rate.
	 */
	private boolean showFPS = true;

	/** These doubles determine how far off center the image is */
	protected double xOff = 0, yOff = 0, zOff = 0;

	/** This int determines whether or not the panning is normal or inverted */
	@Deprecated
	final protected int invert = 1;

	MouseAdapter adapter;

	public RenderPanel3D(IsoDistortApp app) {
		this.app = app;
		adapter = new Adapter();
		addKeyListener(app.frame.keyListener);
		addMouseListener(adapter);
		addMouseWheelListener(adapter);
		addMouseMotionListener(adapter);
		initialize();
	}

	public void dispose() {
		removeKeyListener(app.frame.keyListener);
		removeMouseListener(adapter);
		removeMouseMotionListener(adapter);
		renderer = null;
		app = null;
	}

	/**
	 * Sets the field of view value.
	 * 
	 * @param value
	 * @see Renderer#setFOV(double value)
	 */
	private void setFOV(double value) {
		renderer.setFOV(value);
	}

	/**
	 * Sets the camera's focal length.
	 * 
	 * @param value focal length
	 * @see Renderer#setFL(double value)
	 */
	private void setFL(double value) {
		renderer.setFL(value);
	}

	/**
	 * Sets the background color ( RGB values range: 0..1).
	 * 
	 * @param r red component 0..1
	 * @param g green component 0..1
	 * @param b blue component 0..1
	 */
	private void setBgColor(double r, double g, double b) {
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
	private void addLight(double x, double y, double z, // ADD A LIGHT SOURCE
			double r, double g, double b) {
		renderer.addLight(x, y, z, r, g, b);
	}

	// private METHODS TO LET THE PROGRAMMER MANIPULATE A MATRIX STACK

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
	private Matrix3D m() {
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

	// private METHODS TO LET THE PROGRAMMER DEFORM AN OBJECT

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
//	private int pull(Geometry s, double x0, double x1, double x2, double y0, double y1, double y2, double z0, double z1,
//			double z2) {
//		return s.pull(m(), x0, x1, x2, y0, y1, y2, z0, z1, z2);
//	}

	// --- SYSTEM LEVEL private METHODS ---

	private IsoDistortApp app;

	private BufferedImage im;

	private void initialize() {
		renderer = new Renderer();
		world = renderer.getWorld(); // GET ROOT OF GEOMETRY
		for (int i = 0; i < matrix.length; i++)
			matrix[i] = new Matrix3D();
		identity();
	}

	/**
	 * Euler angles for camera positioning (horizontal and vertical view rotation).
	 */
	private double theta = 0;
	private double phi = 0;
	private double sigma = 0;

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
		return (getWidth() * (isAntialiased ? 2 : 1) == renderer.W
				&& getHeight() * (isAntialiased ? 2 : 1) == renderer.H);
	}

//long lastt = 0;

	/**
	 * Check for a resize prior to rendering, and then carry out the rendering into
	 * the image raster int[] rgba data buffer
	 */
	@Override
	public synchronized void updateForDisplay(boolean doPaint) {
		if (!isInSync()) {
			resync();
		}
		if (doPaint) {
			render();
			repaint();
		}
	}

	private void render() {
		// write into the image data buffer
		identity();
		renderer.rotateView(theta, phi, sigma);
		if (!spin)
			theta = phi = sigma = 0;
		renderer.render();
	}

	private void resync() {
		// no obvious effect on my screen
//	long t = System.currentTimeMillis();
//	if (t > lastt + 2000) {
//		isAntialiased = (Math.random() > 0.5);
//		lastt = t;
//	}
		// prior to rendering, check for a resize
		int width = Math.max(1, getWidth()) * (isAntialiased ? 2 : 1);
		int height = Math.max(1, getHeight() * (isAntialiased ? 2 : 1));
		im = newBufferedImage(width, height);
		int[] pixels = ((DataBufferInt) im.getRaster().getDataBuffer()).getData();
		clearPixels(pixels);
		renderer.reinit(width, height, pixels);
		setBgColor(1, 1, 1);// background color: white

	}

//	long ttime = 0;

	private void clearPixels(int[] pixels) {
		for (int i = pixels.length; --i > 0;)
			pixels[i] = -1;
	}

	private BufferedImage newBufferedImage(int w, int h) {
		return new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
	}

	// BH this did not have any particular effect that I could see.
	boolean isAntialiased = false;

	@Override
	public synchronized void paint(Graphics g) {
		// long t1 = System.currentTimeMillis();
		// System.out.println("RP timer " + (t1 - ttime));
		// ttime = t1;

		if (!isInSync()) {
			updateForDisplay(true);
			return;
		}

		// System.out.println("RenderPanel3D paint " + isInSync() + " " + app.drawWidth
		// + " " + app.drawHeight + " " + getSize());

		super.paint(g);
		int dw = im.getWidth();
		int dh = im.getHeight();
		int sw = isAntialiased ? dw << 1 : dw;
		int sh = isAntialiased ? dh << 1 : dh;
		if (isAntialiased) {
			g.drawImage(im, 0, 0, dw, dh, 0, 0, sw, sh, null);
		} else {
			g.drawImage(im, 0, 0, null);
		}
		// debugging, testing ....

		double dt = (getCurrentTime() - currentTime);
		elapsed += dt;
		currentTime = getCurrentTime();
		if (elapsed > 0.00001)
			frameRate = 0.9 * frameRate + 0.1 / elapsed;
		elapsed = 0.0;
		if (showFPS) {
			@SuppressWarnings("unused")
			String x = "" + // dt;//
					(int) frameRate + "." + ((int) (frameRate * 10) % 10);
			// in JavaScript we just put this up in the tab
//			/**
//			 * @j2sNative
//			 * 
//			 * 			document.title = ""+x
//			 */
//			{
//				g.setColor(Color.white);
//				g.fillRect(0, renderer.H - 14, 80, 14);
//				g.setColor(Color.black);
//				g.drawString(x + " fps ", 1, renderer.H - 1);
//			}
		}

		if (app.t0 != 0)
			System.out.println("Time to load, render, and paint: " + (System.currentTimeMillis() - app.t0) + " ms");
		app.t0 = 0;
	}

//	/**
//	 * Returns xyz world coords of the frontmost object at pixel (x,y)
//	 * 
//	 * @param x   x pixel coordinate
//	 * @param y   y pixel coordinate
//	 * @param xyz output point in world coords
//	 * @return true iff not a background pixel
//	 */
//	private boolean getPoint(int x, int y, double xyz[]) {
//		return renderer.getPoint(x, y, xyz);
//	}
//
//	/**
//	 * Returns the Geometry of the frontmost object at the point (x, y) in the image
//	 * (like a z-buffer value of geometries).
//	 * 
//	 * @param x x coordinate in the image
//	 * @param y y coordinate in the image
//	 * @return the geometry of the foremost object at that location
//	 */
//	private Geometry getGeometry(int x, int y) {
//		if (renderer.bufferg == false) {
//			renderer.bufferg = true;
////         isDamage = true;
//		}
//		return renderer.getGeometry(x, y);
//	}

	/**
	 * tracks whether, between z-key_down and z-key_up
	 * the mouse was used
	 */
	protected boolean zRotated;

	protected boolean mouseMoveActive;
	
	

	private class Adapter extends MouseAdapter {
	

	    @Override
		public void mouseWheelMoved(MouseWheelEvent e){
	    	doMouseWheel(e.getPreciseWheelRotation());
	    }
	    
		@Override
		public void mouseMoved(MouseEvent e) {
			if (mouseMoveActive) {
				if (dragging)
					doMouseDragged(e, true);
				else
					doMousePressed(e, true);
				return;
			}
			if (isMouseZooming && !e.isShiftDown()) {
				isMouseZooming = false;
				clearAngles();
				setRotationAxis(ROTATE_XYZ);
			}
		}

		@Override
		public void mousePressed(MouseEvent e) {
			doMousePressed(e, false);
		}

		@Override
		public void mouseDragged(MouseEvent e) {
			doMouseDragged(e, false);
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			dragging = false;
			if (isMouseZooming) {
				isMouseZooming = false;
				clearAngles();
			}
			setRotationAxis(ROTATE_XYZ);
		}

	}
	
	protected boolean setRotationAxis(int mode) {
		if (dragging)
			return false;
		rotAxis = mode;
		return true;
	}

	public void doMouseWheel(double d) {
    	double fov = renderer.getFOV();
    	double newfov = fov * (1 - d * 0.1);
		setFOV(newfov);
		app.updateDisplay();
	}

	private boolean dragging = false;

	public void doMouseDragged(MouseEvent e, boolean isMove) {
		if (rotAxis == ROTATE_Z)
			zRotated = true;

		int x = e.getX();
		int y = e.getY();
		/**
		 * LEFT-DRAG but not CTRL-LEFT-DRAG for rotation
		 */
		boolean isRotation = isMove || ((e.getModifiersEx() & InputEvent.BUTTON1_DOWN_MASK) != 0 && !e.isControlDown());

		if (isRotation) {

			double spinrate;
			if (spin)
				spinrate = 0.0003;
			else
				spinrate = 0.006;
			
			System.out.println("R3d mouse " + dragging);
			if (dragging) {
				switch (rotAxis) {
				case ROTATE_XYZ:
					phi += spinrate * (double) (y - my); // VERTICAL VIEW ROTATION
					theta += spinrate * (double) (x - mx); // HORIZONTAL VIEW ROTATION
					sigma = 0;
					break;
				case ROTATE_X:
					phi += spinrate * (double) (y - my);
					theta = 0;
					sigma = 0;
					break;
				case ROTATE_Y:
					phi = 0;
					theta += spinrate * (double) (x - mx);
					sigma = 0;
					break;
				case ROTATE_Z:
					phi = 0;
					theta = 0;
					sigma += -spinrate * (double) ((x - mx) * (256 - y) - (y - my) * (256 - x))
							/ (double) (1 + Math.sqrt((256 - x) * (256 - x) + (256 - y) * (256 - y)));
					break;
				case ROTATE_ZOOM:
					// BH I reversed the sense of the zoom. Just testing!
					if (useJmolZoomDirection ) {
						setFOV(renderer.getFOV() * (1 + (my - y) * 0.004));
					} else {
						setFOV(renderer.getFOV() * (1 + (y - my) * 0.004));
					}
					// y-direction motion changes field of view (zoom).
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

			double shiftRate = 0.01 * renderer.getFOV();
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

	protected void doMousePressed(MouseEvent e, boolean isMove) {
		int x = e.getX();
		int y = e.getY();
		mx = x;
		my = y;
		app.setStatusVisible(false);
		requestFocus();
		if (!isMove && e.isShiftDown()) {
			clearAngles();
			setRotationAxis(ROTATE_ZOOM);
			isMouseZooming = true;
		}
		dragging = true;
	}


	// --- PRIVATE METHODS

	private double getCurrentTime() {
		return System.currentTimeMillis() / 1000.;
	}

	// additional IsoPanel-public methods

	@Override
	public void setCamera(double t, double p, double s) {
		renderer.setCamera(t, p, s);
	}

	@Override
	public void setSpinning(boolean spin) {
		this.spin = spin;
	}

	@Override
	public boolean isSpinning() {
		return spin;
	}

//	private double getFrameRate() {
//		return frameRate;
//	}
//
//	private Renderer getRenderer() {
//		return renderer;
//	}
//
	@Override
	public void clearAngles() {
		theta = phi = sigma = 0;
	}

	public IsoMaterial newMaterial() {
		return new IsoMaterial(renderer);
	}

//	@Override
//	public void reversePanningAction() {
//		invert = -invert;
//	}

	public void transformWorld() {
		for (int i = world.child.length; --i >= 0;)
			if (world.child(i) != null)
				transform(world.child(i));
	}

	@Override
	public BufferedImage getImage() {
		int sw = im.getWidth(null);
		int sh = im.getHeight(null);
		int dw = isAntialiased ? sw >> 1 : sw;
		int dh = isAntialiased ? sh >> 1 : sh;

		BufferedImage bi = newBufferedImage(dw, dh);
		Graphics2D g = bi.createGraphics();
		if (isAntialiased) {
			g.drawImage(im, 0, 0, dw, dh, 0, 0, sw, sh, null);
		} else {
			g.drawImage(im, 0, 0, null);
		}
		g.dispose();
		return bi;
	}

	@Override
	public void initializeSettings(double scdSize) {
		double fl = 10;
		double fov = 2 * scdSize / fl;
		fov0 = fov;
		setBgColor(1, 1, 1);// background color: white
		setFOV(fov);// field of view
		setFL(fl);// focal length: zoomed way out
		setCamera(0, 0, 0);
		// Define position and color of light source (x, y, z, r, g, b)
		double intensity = 0.38;
		addLight(Double.NaN, 0, 0, 0, 0, 0);
		addLight(.5, .5, .5, 1.7 * intensity, 1.7 * intensity, 1.7 * intensity);
		addLight(-.5, .5, .5, intensity, intensity, intensity);
		addLight(.5, -.5, .5, intensity, intensity, intensity);
		addLight(-.5, -.5, .5, intensity, intensity, intensity);
	}

	@Override
	public void resetView() {
		setCamera(0, 0, 0);
		setFOV(fov0);
	}

	@Override
	public double[][] getPerspective() {
		double[] m = new double[16];
		System.arraycopy(renderer.getCamera().getUnsafe(), 0, m, 0, 16);
		return new double[][] { new double[] { fov0, renderer.getFOV(), renderer.isOrthographic() ? 0 : 1 }, m };
	}

	@Override
	public void setPerspective(double[][] params) {
		double[] v = params[0];
		fov0 = v[0];
		setFOV(v[1]);
		setPerspective(v[2] != 0);
		System.arraycopy(params[1], 0, renderer.getCamera().getUnsafe(), 0, 16);
	}

	/**
	 * Turn perspective on or off
	 * 
	 * @param b true for perspective
	 */
	private void setPerspective(boolean b) {
		renderer.setPerspective(b);
	}

	@Override
	public void centerImage() {
		xOff = 0;
		yOff = 0;
		zOff = 0;
		//invert = 1;
		push();
		{
			identity();
			translate(0, 0, 0);
			transformWorld();
		}
		pop();
	}

	@Override
	public double[] getCameraMatrix() {
		return renderer == null ? null : renderer.getCamera().getUnsafe();
	}

	@Override
	public void setCameraMatrixAndZoom(double[] m, double zoom) {
		clearAngles();
		double[] cameraMatrix = getCameraMatrix();
		if (m != cameraMatrix) {
			System.arraycopy(m, 0, cameraMatrix, 0, 16);
			setFOV(fov0 / (zoom / 100));			
		}
	}

	@Override
	public double getZoom() {
		return (renderer == null ? 100 : fov0 / renderer.getFOV() * 100);
	}

	@Override
	public boolean ignoreKeyRelease(char c) {
		switch (c) {
		case 'Z':
		case 'z':
			if (zRotated) {
				// if z was already used for rotate, 
				// ignore its release and clear the flag
				zRotated = false;
				return true;				
			}
			break;
		}
		return false;
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// n/a
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// called from IsoDistortApp
		System.out.println("\n" + e);
		checkRotateKeyPressed(e.getKeyChar());
	}

	private void checkRotateKeyPressed(char c) {
		int mode;
		switch (c) {
		case 'x':
		case 'X':
			mode = IsoRenderPanel.ROTATE_X;
			break;
		case 'y':
		case 'Y':
			mode = IsoRenderPanel.ROTATE_Y;
			break;
		case 'z':
		case 'Z': // "normal to screen"
			mode = IsoRenderPanel.ROTATE_Z;
			break;
		default:
			// any other key puts us back in xyz mode
			setRotationAxis(IsoRenderPanel.ROTATE_XYZ);
			return;
		}
		setRotationAxis(mode);
		mouseMoveActive = true;
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// called from IsoDistortApp
		if (mouseMoveActive) {
			adapter.mouseReleased(null);
			mouseMoveActive = false;
		}
	}

}
