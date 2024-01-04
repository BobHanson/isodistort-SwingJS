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
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.text.DefaultCaret;

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

	final static String minorVersion = ".9_2024.01.03";

	static boolean isJS = (/** @j2sNative true || */false);
	
	/**
	 * the datafile to use for startup
	 */
	protected String whichDataFile = //"data/test22.txt";
									"data/data.isoviz";
									//"data/ZrP2O7-sg205-sg61-distort.isoviz";
									//"data/test28.txt";


	
	final static protected int APP_ISODISTORT = 0;
	final static protected int APP_ISODIFFRACT = 1;

	protected final static int DIS = Mode.DIS; // displacive
	protected final static int OCC = Mode.OCC; // occupancy (aka "scalar")
	protected final static int MAG = Mode.MAG; // magnetic
	protected final static int ROT = Mode.ROT; // rotational
	protected final static int ELL = Mode.ELL; // ellipsoidal

	/**
	 * when saving data across switches between IsoDistort and IsoDiffract
	 */
	final static int SETTINGS_PERSPECTIVE = 0;
	final static int SETTINGS_APP = 1;

	/**
	 * a few common parameters for initializing the GUI
	 */
	private static final int padding = 4, controlPanelHeight = 75, roomForScrollBar = 15;

	/**
	 * this app's type, either APP_ISODISTORT or APP_ISODIFFRACT
	 */
	protected int appType;

	/**
	 * An instance of the Variables class that holds all the data
	 */
	protected Variables variables;

	boolean asBytes = true;
	
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
	private JScrollPane sliderScrollPane;

	private class StatusPanel extends JTextArea {

		StatusPanel() {
			setEditable(false);
			setMargin(new Insets(5,5,5,5));
			setAutoscrolls(true);
			((DefaultCaret)getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			setBackground(Color.LIGHT_GRAY);
			setVisible(false);
		}
		
		void addText(String status) {
			if (status == null) {
				setText("");
			} else {
				System.out.println(status);
				String s = getText();
				setText((s.length() == 0 ? "" : s + "\n") + status);
			}
		}

		boolean isLockedVisible = false;

		/**
		 * 
		 * @param b true for visible
		 * @param andLock set true to ensure that FALSE is triggered only by andLock=true
		 */
		void setVisible(boolean b, boolean andLock) {
			if (andLock) {
				isLockedVisible = b;
			} 
			if (b == isVisible())
				return;
			if (b || !isLockedVisible) {
					controlPanel.setVisible(!b);
					super.setVisible(b);
				}
			controlStatusPanel.setBackground(b ? Color.LIGHT_GRAY : Color.WHITE);
			controlStatusPanel.setBorder(b ? statusBorder : controlBorder);
		}
	}
	/**
	 * Panel that swaps for control panel during server actions.
	 * 
	 * @author Bob Hanson
	 *
	 */
	private StatusPanel statusPanel;


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
	 * the ISOVIZ data associated with this app in byte[] form
	 * 
	 */
	protected byte[] isovisData = new byte[0];

	/**
	 * the distortion file data associated with this app in byte[] form
	 */
	protected byte[] distortionFileData;
	
	/**
	 * the settings for perspective and sliders used to pass information
	 * between IsoDistort and IsoDiffract; they are reinitialized upon 
	 * initial file loading or a file drop
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
	 * Center structure in window.
	 */
	abstract public void centerImage();

	/**
	 * Reset View.
	 */
	abstract public void reset();


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
		whichDataFile = null;
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


	private JPanel controlStatusPanel;


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

	final static Border controlBorder = BorderFactory.createLineBorder(Color.BLACK, 1);
	final static Border statusBorder = BorderFactory.createLineBorder(Color.RED, 1);
	
	protected void initializePanels() {
		frameContentPane.setTransferHandler(new FileUtil.FileDropHandler(this));
		frameContentPane.removeAll();
		controlPanel = new JPanel();
		controlPanel.setBackground(Color.WHITE);
		controlPanel.setLayout(new GridLayout(2, 1, 0, -5));
		statusPanel = new StatusPanel();
		controlStatusPanel = new JPanel(new FlowLayout());
		controlStatusPanel.setBackground(Color.WHITE);
		controlStatusPanel.add(controlPanel);
		controlStatusPanel.add(statusPanel);
		controlStatusPanel.setBorder(controlBorder);
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

		sliderScrollPane = new JScrollPane(sliderPanel, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
				ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		sliderScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
		isoPanel.add(sliderScrollPane, BorderLayout.EAST);// add to east of Applet

		isoPanel.add(controlStatusPanel, BorderLayout.SOUTH);// add to east of Applet

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
			formData = args[1]; // String or Map
			// Fall through //
		case 1:
			isovisData = (args[0] instanceof byte[] ? (byte[]) args[0] : args[0].toString().getBytes());
			whichDataFile = null;
			break;
		default:
			String path = getClass().getName();
			path = path.substring(0, path.lastIndexOf('.') + 1).replace('.', '/');
			isovisData = FileUtil.readFileData(this, path + whichDataFile);
		}
		return isovisData;
	}

	/**
	 * @param f
	 * @return
	 */
	public void loadDroppedFile(File f) {
		if (f == null)
			return;
		byte[] data = FileUtil.readFileData(this, f.getAbsolutePath());
		if (data == null)
			return;
		switch (FileUtil.getIsoFileTypeFromContents(data)) {
		case FileUtil.FILE_TYPE_FORMDATA_JSON:
			sendFormDataToServer(ensureMapData(new String(data), true, false));
			return;
		case FileUtil.FILE_TYPE_DISTORTION_TXT:
			distortionFileToISOVIZ(f.getName(), data);
			return; 
		case FileUtil.FILE_TYPE_ISOVIZ:
			String ext = FileUtil.getExtension(f, "isoviz*");
			int newType = ((ext == null) || !ext.equals("isovizq") ? APP_ISODISTORT : APP_ISODIFFRACT);
			boolean isIsoDistort = (appType == APP_ISODISTORT);
			boolean isMyType = (isIsoDistort == (newType == APP_ISODISTORT));
			boolean doSwitch = !isMyType;
			if (ext != null && !isMyType) {
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
				openApplication(appType, data, null, true);
			}, "isodistort_file_dropper").start();
			return;
		default:
			sayNotPossible("File type was not recognized.");
			return;
		}

	}

	private void openApplication(int appType, Object data, Map<String, Object> mapFormData, boolean isDrop) {
		if (data == null)
			return;
		if (!prepareToSwapOut())
			return;
		IsoApp me = this;
		new Thread(() -> {
			setStatus(null);
			boolean isIsoDistort = (appType == APP_ISODISTORT);
			IsoApp app = (isIsoDistort ? new IsoDistortApp() : new IsoDiffractApp());
			if (args == null || args.length == 0)
				args = new Object[1];
			args[0] = data;
			JFrame frame = me.frame;
			dispose();
			app.start(frame, args, variables, isDrop);
			app.actions = actions.setApp(app);
			if (isDrop) {
				clearSettingsForFileDrop();
				app.formData = mapFormData;
			} else {
				app.appSettings = appSettings;
				//app.droppedFile = droppedFile;
				app.document = document;
				app.formData = formData;
				app.whichDataFile = whichDataFile;
				app.distortionFileData = distortionFileData;
				app.variables.setValuesFrom(variables);
				app.setControlsFrom((IsoApp) appSettings[app.appType]);
			}
			app.frameResized();
		}, "isodistort_application").start();
	}

	/**
	 * Frame has been resized -- update renderer and display.
	 * This will occur inititially and upon user resize drag. 
	 * 
	 */
	 protected void frameResized() {
		 	Dimension d = new Dimension(controlStatusPanel.getWidth() - 5, controlPanelHeight);
			controlPanel.setPreferredSize(d);
			statusPanel.setPreferredSize(d);
			variables.setPackedHeight(sliderPanel);
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
			updateDisplay();
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

	private MenuActions actions;
	
	private void start(JFrame frame, Object[] args, Variables oldVariables, boolean isDrop) {
		if (oldVariables == null && !isDrop) {
			actions = new MenuActions(this);
			frame.setJMenuBar((JMenuBar) actions.createMenuBar());
		}
		this.frame = frame;
		this.args = args;
		frameContentPane = (JPanel) frame.getContentPane();
		frameContentPane.setLayout(new BorderLayout());
		try {
			long t = System.currentTimeMillis();

			initializePanels();
			variables = new Variables(this, appType == APP_ISODIFFRACT, oldVariables != null);
			isovisData = variables.parse(readFile());
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
			controlStatusPanel.setPreferredSize(new Dimension(controlStatusPanel.getWidth(), controlPanelHeight));
			frame.pack();
			updateDimensions();
			
			init();
			frame.setVisible(true);
			String title = frame.getTitle();
			frame.setName(title);
			frame.setTitle("IsoVIZ ver. " + variables.isoversion + minorVersion);
			frame.addComponentListener(componentListener);
			frame.addWindowListener(windowListener);
			addStatus("Time to load: " + (System.currentTimeMillis() - t) + " ms");
			if (isDrop)
				updateDisplay();
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
				openApplication(appType == APP_ISODISTORT ? APP_ISODIFFRACT : APP_ISODISTORT, isovisData, null, false);
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

	//<P><FORM ACTION="isodistortuploadfile.php" METHOD="POST"  
	//enctype="multipart/form-data">
	//Import an ISODISTORT distortion file:
	//<INPUT TYPE="hidden" NAME="input" VALUE="distort">
	//<INPUT TYPE="hidden" NAME="origintype" VALUE="dfile">
	//<INPUT CLASS="btn btn-primary" TYPE="submit" VALUE="OK">
	//<input name="toProcess" type="file" size="30">
	//</form>		

	/**
	 * Process the distortion file data by passing it to the server
	 * and retrieving the ISODISTORT display page HTML data. Scrape
	 * that for INPUT tags, and set the radio button for origintype
	 * to "isovizdistortion". Send this to the server in order to 
	 * retrieve the ISOVIZ file. Would be a lot simpler just to 
	 * get the ISOVIZ file from the server directly.
	 * 
	 * @param f
	 */
	private void distortionFileToISOVIZ(String fileName, byte[] data) {
		// 1) send distortion file data to the isodistort server
		Map<String, Object> mapFormData = new LinkedHashMap<String, Object>();
		mapFormData.put("input", "distort");
		mapFormData.put("origintype", "dfile");
		mapFormData.put("toProcess", data);
		if (fileName != null)
			mapFormData.put("fileName", fileName);
		setStatus("...uploading " + data.length + " bytes of distortion file data to iso.byu...");
		ServerUtil.fetch(this, FileUtil.FILE_TYPE_DISTORTION_UPLOAD, mapFormData, new Consumer<byte[]>() {
			@Override
			public void accept(byte[] b) {
				if (b == null) {
					addStatus("upload failed");
					return;
				}		
				distortionFileData = b;
				sendFormDataToServer(FileUtil.scrapeHTML(IsoApp.this, new String(b)));
			}

		}, 5);
	}

	protected void sendFormDataToServer(Map<String, Object> formData) {
		Map<String, Object> mapFormData = setServerFormOriginType(formData, "isovizdistortion");
		if (mapFormData == null) {
			JOptionPane.showMessageDialog(frame, "The server was not able to read the data necessary to create ISOVIS file data. ");
			return;
		} 
		createIsovizFromFormData(mapFormData, new Consumer<byte[]>() {

			@Override
			public void accept(byte[] data) {
				setStatus("...opening ISODISTORT for " + data.length + " bytes of data...");
				new Thread(() -> {
					openApplication(appType, data, mapFormData, true);
				}, "isodistort_from_server").start();
				
			}
			
		});
	}

	private void createIsovizFromFormData(Object formData, Consumer<byte[]> consumer) {
		// not available if dropped?
		boolean isSwitch = (formData == null);
		if (isSwitch)
			formData = this.formData;
		Map<String, Object> mapData = ensureMapData(formData, isSwitch, false);
		if (mapData == null)
			return;
		if (isSwitch)
			variables.updateModeFormData(mapData, document);
		setStatus("...fetching ISOVIZ file from iso.byu...");
		ServerUtil.fetch(this, FileUtil.FILE_TYPE_ISOVIZ, mapData, consumer, 20);
	}	

	/**
	 * "Press" the radio button for the page
	 * @param formData
	 * @param type
	 * @return
	 */
	protected Map<String, Object> setServerFormOriginType(Map<String, Object> formData, String type) {
		if (formData == null || formData.get("origintype") == null)
			return null;
		formData.put("origintype", type);
		this.formData = formData;
		return formData;
	}
	

	public void addStatus(String status) {
		statusPanel.addText(status);
	}
	
	public void setStatus(String status) {
		addStatus(status);
		setStatusVisible(true);
	}

	public void setStatusVisible(boolean b) {
		statusPanel.setVisible(b, false);
	}

	public boolean toggleStatusVisible() {
		boolean b = !statusPanel.isVisible();
		statusPanel.setVisible(b, true);
		return b;
	}

	public boolean isStatusVisible() {
		return statusPanel.isVisible();
	}

	public void setFormData(Map<String, Object> formData, String sliderSetting) {
		variables.setModeFormData(formData, sliderSetting);
	}

	private void sayNotPossible(String msg) {
		JOptionPane.showMessageDialog(frame, "This feature is not available " + msg);
	}

	/**
	 * If we have form data, clone it, adjust its values, and pass the clone to
	 * the server with a request to construct a new ISOVIZ file
	 */
	public void saveCurrent() {
		createIsovizFromFormData(null, new Consumer<byte[]>() {

			@Override
			public void accept(byte[] data) {
				FileUtil.saveDataFile(frame, data, "isoviz", false);
			}

		});
	}

	private Map<String, Object> ensureMapData(Object formData, boolean asClone, boolean silent) {
		if (formData == null) {
			formData = this.formData;
			if (formData == null && whichDataFile != null) {
				String path = getClass().getName();
				path = path.substring(0, path.lastIndexOf('.') + 1).replace('.', '/');
				formData = FileUtil.readFileData(this, path + whichDataFile.replace("txt", "json").replace("isoviz", "json"));
			}
		}
		Map<String, Object> mapData = ServerUtil.json2Map(formData, asClone);
		if (mapData == null) {
			// if all we have is an isoviz file, how can we update it?
			if (!silent)
				sayNotPossible("no form data to process; open a DISTORTION file first.");
			return null;
		}
		this.formData = mapData;
		return mapData;
	}

	private void updateFormData(Map<String, Object> map, Map<String, Object> values, String originType) {
		String sliderSetting = "current";
		if (values != null) {
			sliderSetting = (String) values.remove("slidersetting");
			map.putAll(values);
		}
		setFormData(map, sliderSetting);
		setServerFormOriginType(map, originType);
	}

	public void saveOriginal() {
		FileUtil.saveDataFile(frame, isovisData, "isoviz", false);
	}

	public void setPreferences(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, true);
		if (map == null) {
			map = variables.getPreferences();
		}
		if (values == null) {
			IsoDialog.openPreferencesDialog(this, map);
		} else {
			// after dialog
			if (variables.setPreferences(map, values)) {
				// we have a non-local change that needs servicing
				sendFormDataToServer(map);
			};
		}
	}

	public void saveFormData(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openFormDialog(this, map);
		} else {
			// after dialog
			String sliderSetting = (String) values.remove("slidersetting");
			map.putAll(values);
			setFormData(map, sliderSetting);	
			FileUtil.saveDataFile(frame, ServerUtil.toJSON(map), "json", false);
		}
	}

	public void saveTOPAS(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openTOPASDialog(this, map);
		} else {
			// after dialog
			updateFormData(map, values, "topas");
			setStatus("...fetching TOPAS.STR file from iso.byu...");
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_TOPAS_STR, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}		
					FileUtil.saveDataFile(frame, b, "STR", false);
				}
			}, 20);
		}

	}

	public void saveDistortionFile(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openDistortionDialog(this, map);
		} else {
			// after dialog
			updateFormData(map, values, "distortionfile");
			setStatus("...fetching DISTORTION file from iso.byu...");
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_DISTORTION_TXT, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}		
					FileUtil.saveDataFile(frame, b, "iso.txt", false);
				}
			}, 20);
		}
	}

	public void saveFULLPROF(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openFULLPROFDialog(this, map);
		} else {
			// after dialog
			updateFormData(map, values, "fullprof");
			setStatus("...fetching FULLPROF file from iso.byu...");
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_FULLPROF_CPR, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}		
					FileUtil.saveDataFile(frame, b, "cpr", false);
				}
			}, 20);
		}

	}

	public void saveCIF(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openCIFDialog(this, map);
		} else {
			// after dialog
			updateFormData(map, values, "structurefile");
			setStatus("...fetching CIF file from iso.byu...");
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_CIF, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}		
					FileUtil.saveDataFile(frame, b, "cif", false);
				}
			}, 20);
		}
	}

	public void viewSubgroupTree(Map<String, Object> values) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		if (values == null) {
			IsoDialog.openSubgroupTreeDialog(this, map);
		} else {
			// after dialog
			updateFormData(map, values, "tree");
			ServerUtil.displayIsoPage(this, map);
		}
	}
	
	public void viewPage(String originType) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		updateFormData(map, null, originType);
		ServerUtil.displayIsoPage(this, map);
	}

}
