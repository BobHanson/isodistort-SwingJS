package org.byu.isodistort.local;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;

import org.byu.isodistort.IsoDiffractApp;
import org.byu.isodistort.IsoDistortApp;
import org.byu.isodistort.server.ServerUtil;

/**
 * 
 * common abstract class for IsoDistortApplet and IsoDiffractApplet
 * 
 * @author Bob Hanson
 *
 */
public abstract class IsoApp {

	final static String minorVersion = ".8";

	final static protected int APP_ISODISTORT = 0;
	final static protected int APP_ISODIFFRACT = 1;

	public final static int DIS = Mode.DIS; // displacive
	public final static int OCC = Mode.OCC; // occupancy (aka "scalar")
	public final static int MAG = Mode.MAG; // magnetic
	public final static int ROT = Mode.ROT; // rotational
	public final static int ELL = Mode.ELL; // ellipsoidal

	/**
	 * when saving data across switches between IsoDistort and IsoDiffract
	 */
	final static int SETTINGS_PERSPECTIVE = 0;
	final static int SETTINGS_APP = 1;

	/**
	 * a few common parameters for initializing the GUI
	 */
	private static final int padding = 4, controlPanelHeight = 55, roomForScrollBar = 15;

	/**
	 * this app's type, either APP_ISODISTORT or APP_ISODIFFRACT
	 */
	protected int appType;

	/**
	 * An instance of the Variables class that holds all the data
	 */
	protected Variables variables;

	/**
	 * the datafile to use for startup
	 */
	protected String whichdatafile = "data/data.isoviz";// "data/test28.txt";//"data/ZrP2O7-sg205-sg61-distort.isoviz";////"data/test28.txt";

	boolean asBytes = true;
	
	/**
	 * the file that was dropped
	 * 
	 */
	private File droppedFile;

	/**
	 * false when the dispose() method has run
	 */
	protected boolean isEnabled = true;

	/**
	 * flag to indicate that we are adjusting the values; do not update display
	 */
	protected boolean isAdjusting = false;

	/**
	 * flag to indicate that something has changed, and we need to recalculate everything
	 */
	protected boolean needsRecalc = true;

	/**
	 * the frame holding this app
	 */
	protected JFrame frame;

	/**
	 * The panel displaying this application
	 */
	protected JPanel frameContentPane;

	/**
	 * drawing area width and height
	 */
	protected int drawWidth, drawHeight;

	protected JButton applyView, saveImage, saveISOVIZ, openOther;

	public int initializing = 5; // lead needed to get first display. Why?

	/**
	 * Pane that holds the scroll panels
	 */
	private JScrollPane sliderPane;

	/**
	 * Panel that holds all control components
	 */
	public JPanel controlPanel;
	/**
	 * Holds all the slider bars but not the viewPanel above it; added to
	 * controlPanel
	 */
	public JPanel sliderPanel;

	/**
	 * the central space where the drawing is done; holds the rendering panel
	 */

	protected JPanel drawPanel;

	/**
	 * the JPanel used by this app for its rendering panel
	 */
	private JPanel isoPanel;

	/**
	 * the starting arguments when this app's main method was started,
	 * or information put in that array later
	 * 
	 */
	protected Object[] args;

	/**
	 * the HTML document that called up this app, if on the web 
	 */
	protected Object document;

	/**
	 * the form data that were sent to the server by the HTML document 
	 * retrieving the ISOVIZ file that was used to start this app
	 */
	protected Object formData;

	/**
	 * the ISOVIZ data associated with this app, in String or byte[] form
	 * 
	 */
	protected Object isoData = "";

	/**
	 * the settings for perspective and sliders used to pass information
	 * between IsoDistort and IsoDiffract
	 * 
	 */
	private IsoApp[] appSettings = new IsoApp[2];

	/**
	 * a list of listeners used to remove listeners by dispose()
	 */
	private List<JToggleButton> listenerList = new ArrayList<>();

	/**
	 * The variables are all read. Time to do any app-specific initialization before
	 * we make the frame visible.
	 */
	abstract protected void init();

	/**
	 * frame has been resized -- update renderer and display
	 */
	abstract protected void frameResized();

	/**
	 * Get the image from the renderer for saving.
	 * 
	 * @return the current image of drawing frame.
	 */
	abstract protected BufferedImage getImage();

	/**
	 * Something has changed.
	 */
	abstract public void updateDisplay();

	/**
	 * The "Apply View" action.
	 * 
	 */
	abstract protected void applyView();

	/**
	 * The click callback comes to IsoApp and is distributed to the applet by this
	 * method.
	 * 
	 * @param src
	 */
	abstract protected void handleButtonEvent(Object src);

	/**
	 * When an app is swapped back in, this method allows the app to reload its
	 * settings.
	 * 
	 * @param app
	 */
	abstract protected void setControlsFrom(IsoApp app);

	/**
	 * This method allows the current application to finish up what it is doing
	 * prior to swapping out.
	 * 
	 * @return false to disallow swapping out at this moment.
	 * 
	 */
	abstract protected boolean prepareToSwapOut();

	protected IsoApp(int appType) {
		this.appType = appType;
	}

	private void clearSettingsForFileDrop() {
		appSettings = new IsoApp[2];
	}

	/**
	 * Preserve just the app itself and its current perspective
	 */
	void preserveAppData() {
		appSettings[appType] = this;
	}

	/**
	 * listens for the check boxes that highlight a given atomic subtype.
	 */
	protected ItemListener buttonListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent event) {
			handleButtonEvent(event.getSource());
		}
	};

	private static int buttonID = 0;

	protected JRadioButton newRadioButton(String label, boolean selected, ButtonGroup g) {
		JRadioButton b = new JRadioButton(label, selected);
		b.setName(++buttonID + ":" + label);
		b.setHorizontalAlignment(JRadioButton.LEFT);
		b.setVerticalAlignment(JRadioButton.CENTER);
		b.setFocusable(false);
		b.setBackground(Color.WHITE);
		b.setForeground(Color.BLACK);
		b.setVisible(true);
		b.setBorderPainted(false);
		b.addItemListener(buttonListener);
		g.add(b);
		listenerList.add(b);
		return b;
	}

	protected JCheckBox newJCheckBox(String label, boolean selected) {
		JCheckBox cb = new JCheckBox(label, selected);
		cb.setName(++buttonID + ":" + label);
		cb.setHorizontalAlignment(JCheckBox.LEFT);
		cb.setVerticalAlignment(JCheckBox.CENTER);
		cb.setFocusable(false);
		cb.setVisible(true);
		cb.setBackground(Color.WHITE);
		cb.setForeground(Color.BLACK);
		cb.addItemListener(buttonListener);
		listenerList.add(cb);
		return cb;
	}

	protected void initializePanels() {
		frameContentPane.setTransferHandler(new FileUtil.FileDropHandler(this));
		frameContentPane.removeAll();
		controlPanel = new JPanel();
		controlPanel.setBackground(Color.WHITE);
		controlPanel.setLayout(new GridLayout(2, 1, 0, -5));
		isoPanel = new JPanel(new BorderLayout())
//		{	// BH testing paints
////			@Override 
////			public void paint(Graphics g) {
////				// only happens initially and on resize
////				System.out.println("IA paint");
////				super.paint(g);
////			}
//			
////			@Override
////			public void repaint() {
////				// only initially and upon resize
////				System.out.println("IA repaint");
////				super.repaint();
////			}
////			@Override
////			public void repaint(int x, int y, int width, int height) {
////				// does not happen
////				System.out.println("IA repaint4");
////				super.repaint(x, y, width, height);
////			}
//		}
		;
		if (sliderPanel == null) {
			sliderPanel = new JPanel();
			sliderPanel.setBackground(Color.WHITE);
			sliderPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			// sets grid length equal to number of rows.
		}

		sliderPane = new JScrollPane(sliderPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sliderPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
		isoPanel.add(sliderPane, BorderLayout.EAST);// add to east of Applet

		controlPanel.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		isoPanel.add(controlPanel, BorderLayout.SOUTH);// add to east of Applet

		drawPanel = new JPanel(new BorderLayout());
		isoPanel.add(drawPanel, BorderLayout.CENTER);
		frame.add(isoPanel);
	}

	/**
	 * Add general buttons to bottom control panel right side.
	 * 
	 * @param panel
	 */
	protected void addSaveButtons(JComponent panel) {
		ViewListener vl = new ViewListener();
		applyView = newJButton("Apply View", vl);
		saveImage = newJButton("Save Image", vl);
		saveISOVIZ = newJButton("Save ISOVIZ", vl);
		openOther = newJButton("Open " + (appType == APP_ISODISTORT ? "ISODIFFRACT" : "ISODISTORT"), vl);
		panel.add(new JLabel("   "));
		panel.add(applyView);
		panel.add(saveImage);
		panel.add(saveISOVIZ);
		panel.add(openOther);
	}

	private static JButton newJButton(String text, ViewListener vl) {
		JButton b = new JButton(text);
		b.setFocusable(false);
		b.setMargin(new Insets(-3, 3, -2, 4));
		b.setHorizontalAlignment(SwingConstants.LEFT);
		b.setVerticalAlignment(SwingConstants.CENTER);
		b.addActionListener(vl);
		return b;
	}

	protected ActionListener textBoxListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			needsRecalc = true;
			updateDisplay();
		}

	};

	protected JTextField newTextField(String text, int insetRight) {
		JTextField t = new JTextField(text, 3);
		t.setMargin(new Insets(-2, 0, -1, insetRight));
		t.addActionListener(textBoxListener);
		return t;
	}

	/**
	 * This data reader has two modes of operatinon.
	 * 
	 * For data files, it can accept byte[], String, or InputStream data.
	 * 
	 * The alternative is to read a single-string data sequence directly from the
	 * html file that calls the app (injected as isoData).
	 * 
	 * The Variables class then has a method that parses the byte[] representation
	 * of the string.
	 */
	protected Object readFile() {
		switch (args == null ? 0 : args.length) {
		case 3:
			document = args[2];
			// Fall through //
		case 2:
			formData = args[1];
			// Fall through //
		case 1:
			isoData = args[0];
			break;
		default:
			String path = getClass().getName();
			path = path.substring(0, path.lastIndexOf('.') + 1).replace('.', '/');
			isoData = readFileData(path + whichdatafile);
		}
		return isoData;
	}

	// boolean asInputStream = true;
	private Object readFileData(String path) {
		System.out.println("Reading file " + path);
		try {
			long t = System.currentTimeMillis();
			// 33 ms to read 8 MB
			BufferedInputStream bis = null;
			try {

				File f = new File(path);
				bis = new BufferedInputStream(new FileInputStream(f));
			} catch (Exception e) {
				bis = new BufferedInputStream((InputStream) getClass().getResource("/" + path).getContent());
			}
			bis.mark(200);
			byte[] header = new byte[200];
			bis.read(header);
			String s = new String(header);
			if (s.indexOf("!isoversion") < 0) {
				bis.close();
				return null;
			}
			bis.reset();

			byte[] bytes = FileUtil.getLimitedStreamBytes(bis, Integer.MAX_VALUE, true);
			System.out.println(
					"IsoApp.readFileData " + (System.currentTimeMillis() - t) + " ms for " + bytes.length + " bytes");
			return bytes;

//			// String concatenation (200K+ lines) was 600000+ ms (over 10 minutes) to read 8 MB
//			// StringBuffer was 121 ms to read 8 MB
//			// getLimitedStreamReader was 33 ms to read 8 MB
		} catch (IOException exception) {
			exception.printStackTrace();
			System.out.println("Oops. File not found.");
		}
		return null;
	}

	/**
	 * Currently only isoviz and isovizq files.
	 * 
	 * @param f
	 * @return
	 */
	public boolean loadDroppedFile(File f) {
		if (f == null)
			return false;
		droppedFile = f;

		String ext = FileUtil.getExtension(f, "isoviz*");
		boolean known = (ext != null);
		Object data = readFileData(f.getAbsolutePath());
		if (data == null)
			return false;
		boolean isIsoDistort = (appType == APP_ISODISTORT);
		boolean isMyType = (isIsoDistort == (!known || !ext.equals("isovizq")));
		boolean doSwitch = !isMyType;
		if (known && !isMyType) {
			String msg = "This file is intended for " + (appType == APP_ISODISTORT ? "ISODIFFRACT" : "ISODISTORT")
					+ ". Would you like to switch to that application?";
			doSwitch = (JOptionPane.showConfirmDialog(null, msg, "File Drop Application",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
		}
		if (doSwitch) {
			isIsoDistort = !isIsoDistort;
		}

		int appType = isIsoDistort ? APP_ISODISTORT : APP_ISODIFFRACT;
		new Thread(() -> {
			openApplication(appType, data, true);
		}, "isodistort_file_dropper").start();
		return true;

	}

	private void openApplication(int appType, Object data, boolean isDrop) {
		if (data == null)
			return;
		if (!prepareToSwapOut())
			return;
		IsoApp me = this;
		new Thread(() -> {
			boolean isIsoDistort = (appType == APP_ISODISTORT);
			IsoApp app = (isIsoDistort ? new IsoDistortApp() : new IsoDiffractApp());
			if (args == null || args.length == 0)
				args = new Object[1];
			args[0] = data;
			JFrame frame = me.frame;
			dispose();
			app.start(frame, args, variables, isDrop);
			if (isDrop) {
				clearSettingsForFileDrop();
			} else {
				app.appSettings = appSettings;
				app.droppedFile = droppedFile;
				app.document = document;
				app.formData = formData;
				app.variables.setValuesFrom(variables);
				app.setControlsFrom((IsoApp) appSettings[app.appType]);
			}
			app.frameResized();
		}, "isodistort_application").start();
	}

	protected void dispose() {
		isEnabled = false;
		prepareToSwapOut();
		while (listenerList.size() > 0) {
			JToggleButton c = listenerList.remove(listenerList.size() - 1);
			c.removeItemListener(buttonListener);
		}
		frame.removeComponentListener(componentListener);
		frame.removeWindowListener(windowListener);
		frame.getContentPane().removeAll();
		frame = null;
	}

	private ComponentListener componentListener = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {
			updateDimensions();
			frameResized();
		}
	};

	private WindowListener windowListener = new WindowAdapter() {

		@Override
		public void windowClosing(WindowEvent e) {
			shutDown();
		}

		@Override
		public void windowClosed(WindowEvent e) {
		}

		@Override
		public void windowIconified(WindowEvent e) {
		}

		@Override
		public void windowDeiconified(WindowEvent e) {
		}

		@Override
		public void windowActivated(WindowEvent e) {
		}

		@Override
		public void windowDeactivated(WindowEvent e) {
		}

	};

	private void start(JFrame frame, Object[] args, Variables oldVariables, boolean isDrop) {
		if (oldVariables == null && !isDrop)
			frame.setJMenuBar((JMenuBar) new MenuActions(this).createMenuBar());
		this.frame = frame;
		this.args = args;
		frameContentPane = (JPanel) frame.getContentPane();
		frameContentPane.setLayout(new BorderLayout());
		try {
			long t = System.currentTimeMillis();

			initializePanels();
			variables = new Variables(this, appType == APP_ISODIFFRACT, oldVariables != null);
			isoData = variables.parse(readFile());
			if (oldVariables == null) {
				frameContentPane.setPreferredSize(new Dimension(variables.appletWidth, variables.appletHeight));
			} else {
				frameContentPane.setPreferredSize(frameContentPane.getSize());
			}
			frame.pack();
			// frame is packed, so OUTER sizes are set now.
			// but we still have to set the sliderPanel width
			// and height. We provide a provisional setting here
			// and let Variables adjust it as necessary.
			int sliderPaneHeight = frameContentPane.getHeight() - controlPanelHeight - padding;
			int sliderPanelWidth = frameContentPane.getWidth() - sliderPaneHeight - roomForScrollBar - padding;
			int sliderPanelHeight = sliderPaneHeight - padding;
			sliderPanel.setPreferredSize(new Dimension(sliderPanelWidth, sliderPanelHeight));
			variables.initSliderPanel(sliderPanel);
			frame.pack();
			updateDimensions();
			init();
			frame.setVisible(true);
			String title = frame.getTitle();
			frame.setName(title);
			frame.setTitle("IsoVIZ ver. " + variables.isoversion + minorVersion);
			frame.addComponentListener(componentListener);
			frame.addWindowListener(windowListener);
			System.out.println("Time to load: " + (System.currentTimeMillis() - t) + " ms");
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(frame, "Error reading input data " + e.getMessage());
			e.printStackTrace();
		}

	}

	protected void shutDown() {
		dispose();
		/**
		 * @j2sNative
		 */
		{
			System.exit(0);
		}
	}

	protected void updateDimensions() {
		drawWidth = drawPanel.getWidth();
		drawHeight = drawPanel.getHeight();
	}

	/**
	 * viewListener class listens for the applet buttons and the inside methods
	 * specify the viewing angles.
	 */
	private class ViewListener implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent event) {
			Object source = event.getSource();
			if (source == applyView) {
				applyView();
				return;
			}
			if (source == saveImage) {
				FileUtil.saveDataFile(frame, getImage(), "png", false);
				return;
			}
			if (source == saveISOVIZ) {
				saveOriginal();
				return;
			}
			if (source == openOther) {
				preserveAppData();
				openApplication(appType == APP_ISODISTORT ? APP_ISODIFFRACT : APP_ISODISTORT, isoData, false);
				return;
			}
		}
	}

	protected static void create(String type, String[] args) {
		IsoApp app = null;
		switch (type) {
		case "IsoDistort":
			app = new IsoDistortApp();
			break;
		case "IsoDiffract":
			app = new IsoDiffractApp();
		}
		app.start(new JFrame(type), args, null, false);
	}

	public void saveCurrent() {
		// not available if dropped?
		if (droppedFile != null) {
			// if all we have is an isoviz file, how can we update it?
			variables.updateIsoVizData(isoData);
			sayNotPossible("after a file is dropped");
			return;
		}

		if (formData == null)
			formData = ServerUtil.testFormData;
		Map<String, Object> mapFormData = ServerUtil.json2Map(formData);
		variables.updateFormData(mapFormData, document);

		ServerUtil.getData(this, ServerUtil.ISOVIZ, mapFormData, new Consumer<String>() {

			@Override
			public void accept(String data) {
				FileUtil.saveDataFile(frame, data, "isoviz", false);
			}

		});
	}

	private void sayNotPossible(String msg) {
		JOptionPane.showMessageDialog(frame, "This feature is not available " + msg);
	}

	public void saveOriginal() {
		FileUtil.saveDataFile(frame, isoData, "isoviz", false);
	}

	public void saveDistortion() {
		sayNotPossible("yet");
	}

	public void saveCIF() {
		sayNotPossible("yet");
	}

	public void viewPrimaryOrderParameters() {
		sayNotPossible("yet");
	}

	public void viewModeDetails() {
		sayNotPossible("yet");
	}

	public void viewCompleteModeDetails() {
		sayNotPossible("yet");
	}

	public void saveTOPASstr() {
		sayNotPossible("yet");
	}

	public void saveFULLPROFcpr() {
		sayNotPossible("yet");
	}

	public void saveIRMatrices() {
		sayNotPossible("yet");
	}

	public void viewSubgroupTree() {
		sayNotPossible("yet");
	}

}
