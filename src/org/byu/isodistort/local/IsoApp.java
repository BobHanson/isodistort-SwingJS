package org.byu.isodistort.local;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
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
import javax.swing.BoxLayout;
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
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
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

	final static String minorVersion = ".9_2024.01.06";

	static boolean isJS = (/** @j2sNative true || */
	false);

	/**
	 * the datafile to use for startup
	 */
	protected String whichDataFile = // "data/test22.txt";
			//"data/tbmno3-distortion.iso.txt"; // distortion file
	"data/tbmno3-distortion.isoviz"; // isoviz
	// "data/data.isoviz";
	// "data/ZrP2O7-sg205-sg61-distort.isoviz"; // very large file
	// "data/test28.txt"; // small file 

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
	private static final int padding = 0, controlPanelHeight = 75, roomForScrollBar = 20;

	/**
	 * this app's type, either APP_ISODISTORT or APP_ISODIFFRACT
	 */
	protected int appType;

	/**
	 * An instance of the Variables class that holds all the data
	 */
	protected Variables variables;

	/**
	 * waiting for frame to resize
	 * 
	 */
	boolean isResized;

	/**
	 * false when the dispose() method has run
	 */
	protected boolean isEnabled = true;

	/**
	 * flag to indicate that we are adjusting the values; do not update display
	 */
	protected boolean isAdjusting = false;

	/**
	 * flag to indicate that something has changed, and we need to recalculate
	 * everything
	 */
	protected boolean needsRecalc = true;

	/**
	 * the frame holding this app
	 */
	protected IsoFrame frame;

	/**
	 * drawing area width and height
	 */
	protected int drawWidth, drawHeight;

	protected JButton applyView;// ,
	// saveImage, saveISOVIZ,
	// openOther;

	public int initializing = 5; // lead needed to get first display. Why?

	/**
	 * Pane that holds the scroll panels
	 */
	private JScrollPane sliderScrollPane;

	public static class IsoFrame extends JFrame {

		private JComponent contentPane;
		private JTabbedPane tabbedPane;
		private JPanel distortionPanel;
		private JPanel diffractionPanel;

		private IsoApp[] apps = new IsoApp[2];
		
		private int thisType;
		
		private MenuActions actions;

		
		public IsoFrame(String title) {
			super(title);
			contentPane = (JComponent) getContentPane();
		}

	@Override
		public void repaint() {
			super.repaint();
		}
		
		public void addIsoPanel(IsoApp app, JPanel isoPanel) {
			if (actions == null) {
				actions = new MenuActions(app);
				setJMenuBar((JMenuBar) actions.createMenuBar());
			}
			actions.setApp(app);
			int type = app.appType;
			this.apps[type] = app;
			if (tabbedPane == null) {
				tabbedPane = new JTabbedPane();
				tabbedPane.setFocusable(false);
				distortionPanel = new JPanel();
				distortionPanel.setName("DistPan");
				distortionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
				distortionPanel.setBackground(Color.WHITE);
				diffractionPanel = new JPanel();
				diffractionPanel.setName("DiffPan");
				diffractionPanel.setBorder(new EmptyBorder(0, 0, 0, 0));
				diffractionPanel.setBackground(Color.WHITE);
				tabbedPane.addTab("Distortion", distortionPanel);
				tabbedPane.addTab("Diffraction", diffractionPanel);
				add(tabbedPane);
				tabbedPane.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						switchApps();
					}

				});
			}
			((JComponent)tabbedPane.getComponentAt(type)).removeAll();
			((JComponent)tabbedPane.getComponentAt(type)).add(isoPanel);
			tabbedPane.setSelectedIndex(type);
		}

		protected void switchApps() {
			int type = tabbedPane.getSelectedIndex();
			IsoApp app = apps[type];
			IsoApp thisApp = apps[thisType];
			thisType = type;
			if (thisApp == null)
				return;
			if (app != null) {
				thisApp.prepareToSwapOut();
				((JComponent)getContentPane()).setTransferHandler(new FileUtil.FileDropHandler(app));
				app.variables.setValuesFrom(thisApp.variables);
				invalidate();
				repaint();
			} else {
				thisApp.openApplication(type, thisApp.isovisData, null, false);
			}
		}
		
		

		public int getPanelHeight() {
			int h = getContentPane().getHeight();
			return h - 25;
		}

		public void disposeApps() {
			for (int i = 0; i < 2; i++) {
				if (apps[i] != null) {
					((JComponent)tabbedPane.getComponentAt(i)).removeAll();
					apps[i].dispose();
				}
			}
		}

	}

	private class StatusPanel extends JScrollPane {

		JTextArea area;

		StatusPanel(JTextArea area) {
			super(area);
			setBorder(new EmptyBorder(0, 0, 0, 0));
			setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
			setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
			this.area = area;
			area.setAutoscrolls(false);
			area.setEditable(false);
			area.setMargin(new Insets(0, 15, 5, 5));
			((DefaultCaret) area.getCaret()).setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
			setBackground(Color.LIGHT_GRAY);
			area.setBackground(Color.LIGHT_GRAY);
			setVisible(false);
		}

		void setDimension(Dimension d) {
			setPreferredSize(new Dimension(d.width - 2, d.height));
			area.setPreferredSize(new Dimension(d.width - 10, 1000));
		}

		void addText(String status) {
			if (status == null) {
				area.setText("");
			} else {
				System.out.println(status);
				String s = area.getText();
				area.setText((s == null || s.length() == 0 ? "" : s + "\n") + status);
				area.setCaretPosition(0);
				area.setCaretPosition(area.getDocument().getLength());
			}
		}

		boolean isLockedVisible = false;

		/**
		 * 
		 * @param b       true for visible
		 * @param andLock set true to ensure that FALSE is triggered only by
		 *                andLock=true
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
	 * panel that swaps for control panel during server actions.
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
	 * the starting arguments when this app's main method was started, or
	 * information put in that array later
	 * 
	 */
	protected Object[] args;

	/**
	 * the HTML document that called up this app, if on the web
	 */
	protected Object document;

	/**
	 * the form data that were sent to the server by the HTML document retrieving
	 * the ISOVIZ file that was used to start this app
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

	abstract public void recalcCellColors();

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
		whichDataFile = null;
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

	final static Border controlBorder = new EmptyBorder(0, 0, 0, 0);// BorderFactory.createLineBorder(Color.BLACK, 1);
	final static Border statusBorder = BorderFactory.createLineBorder(Color.RED, 1);

	/**
	 * 
	 * |------------------isoPanel------------------|
	 * |.................................|..........|
	 * |.................................|.slider...|
	 * |..........drawPanel..............|..scroll..|
	 * |.................................|...pane/..|
	 * |.................................|.slider...|
	 * |.................................|...panel..|
	 * |.................................|..........|
	 * |.................................|..........|
	 * |.------------------------------------------.|
	 * |..............controlStatusPanel............|
	 * |............................................|
	 * |--------------------------------------------|
	 */

	protected void initializePanels() {
		frame.contentPane.setTransferHandler(new FileUtil.FileDropHandler(this));
//		frame.contentPane.removeAll();
		controlStatusPanel = new JPanel(new FlowLayout());
		controlStatusPanel.setBackground(Color.WHITE);
		controlPanel = new JPanel();
		controlPanel.setBackground(Color.WHITE);
		controlPanel.setLayout(new GridLayout(2, 1, 0, -5));
		statusPanel = new StatusPanel(new JTextArea());
		controlStatusPanel.add(controlPanel);
		controlStatusPanel.add(statusPanel);
		controlStatusPanel.setBorder(controlBorder);
		isoPanel = new JPanel(new BorderLayout())
//		{	// BH testing paints
//			@Override 
//			public void paint(Graphics g) {
//				// only happens initially and on resize
////				System.out.println("IA paint " 
////						+ frame.tabbedPane.getHeight() + " "
////						+ sliderPanel.getSize() + " "
////				);
//				super.paint(g);
//			}
//			
//			@Override
//			public void repaint() {
//				super.repaint();
//			}
////			@Override
////			public void repaint(int x, int y, int width, int height) {
////				// does not happen
////				System.out.println("IA repaint4");
////				super.repaint(x, y, width, height);
////			}
//		}
		;
		if (sliderPanel == null) {
			sliderPanel = new JPanel() 
//			{
//				public void setPreferredSize(Dimension d) {
//					System.out.println(d);
//					super.setPreferredSize(d);
//				}
//				
//				public void paint(Graphics g) {
//					super.paint(g);
//				}
//
//			}
			;
			sliderPanel.setBackground(Color.WHITE);
			sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
			// sets grid length equal to number of rows.
		}

		sliderScrollPane = new JScrollPane(sliderPanel) 
//		{
//			
//			public void doLayout() {
//				super.doLayout();
////				System.out.println("slh" + sliderPanel.getPreferredSize());
////				this.getVerticalScrollBar().setMaximum(sliderPanel.getPreferredSize().height);
////				this.getVerticalScrollBar().getModel().setExtent(sliderPanel.getPreferredSize().height);
////				System.out.println(this.getVerticalScrollBar().getModel().getExtent());
//			}
//			
//		}
		;
		sliderScrollPane.setBackground(Color.YELLOW);

		sliderScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sliderScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		sliderScrollPane.setBorder(BorderFactory.createLineBorder(Color.BLACK, 0));
		isoPanel.add(sliderScrollPane, BorderLayout.EAST);

		isoPanel.add(controlStatusPanel, BorderLayout.SOUTH);

		drawPanel = new JPanel(new BorderLayout());
		isoPanel.add(drawPanel, BorderLayout.CENTER);

		frame.addIsoPanel(this, isoPanel);
	}

	/**
	 * Add general buttons to bottom control panel right side.
	 * 
	 * @param panel
	 */
	protected void addSaveButtons(JComponent panel) {
		ViewListener vl = new ViewListener();
		applyView = newJButton("Apply View", vl);
		// saveImage = newJButton("Save Image", vl);
		// saveISOVIZ = newJButton("Save ISOVIZ", vl);
//		openOther = newJButton((appType == APP_ISODISTORT ? "View Diffraction" : "View Distortion"), vl);
		panel.add(new JLabel("   "));
		panel.add(applyView);
		// panel.add(saveImage);
		// panel.add(saveISOVIZ);
		// panel.add(openOther);
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
	protected int readFile() {
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
		return FileUtil.getIsoFileTypeFromContents(isovisData);
	}

	/**
	 * @param f
	 * @return
	 */
	public void loadDroppedFile(File f) {
		if (f == null)
			return;
		byte[] data = FileUtil.readFileData(this, f.getAbsolutePath());
		if (data == null || handleNonIsoVizFile(FileUtil.getIsoFileTypeFromContents(data), data)) {
			return;
		}
		new Thread(() -> {
			openApplication(appType, data, null, true);
		}, "isodistort_file_dropper").start();
	}

	private boolean handleNonIsoVizFile(int fileType, byte[] data) {
		switch (fileType) {
		case FileUtil.FILE_TYPE_FORMDATA_JSON:
			sendFormDataToServer(ensureMapData(new String(data), true, false));
			return true;
		case FileUtil.FILE_TYPE_DISTORTION_TXT:
			distortionFileToISOVIZ(whichDataFile == null ? "temp.isoviz": whichDataFile, data);
			return true;
		case FileUtil.FILE_TYPE_ISOVIZ:
			return false;
		default:
			sayNotPossible("File type was not recognized.");
			return true;
		}
	}


	private void openApplication(int appType, Object data, Map<String, Object> mapFormData, boolean isDrop) {
		if (data == null)
			return;
		if (!prepareToSwapOut())
			return;
		IsoFrame frame = this.frame;
		SwingUtilities.invokeLater(() -> {
			new Thread(() -> {
				boolean isIsoDistort = (appType == APP_ISODISTORT);
				IsoApp app = (isIsoDistort ? new IsoDistortApp() : new IsoDiffractApp());
				frame.actions.setApp(app);
				Object[] args = new Object[] { data };
				if (isDrop) {
					frame.disposeApps();
					clearSettingsForFileDrop();
					app.formData = mapFormData;
				}
				if (!app.start(frame, args, variables, isDrop))
					return;
				if (!isDrop) {
					app.document = document;
					app.formData = formData;
					app.whichDataFile = whichDataFile;
					app.distortionFileData = distortionFileData;
					app.variables.setValuesFrom(variables);
					app.updateDisplay();
				}
				app.frameResized();
				app.frame.repaint();
			}, "isodistort_application").start();

		});
	}

	/**
	 * Frame has been resized -- update renderer and display. This will occur
	 * inititially and upon user resize drag.
	 * 
	 */
	protected void frameResized() {
//		System.out.println(this.appType + " resized" + isoPanel.getSize());
//		System.out.println("resized" + controlStatusPanel.getBounds());
		Dimension d = new Dimension(controlStatusPanel.getWidth() - 5, controlPanelHeight);
		controlPanel.setPreferredSize(d);
		statusPanel.setDimension(new Dimension(controlStatusPanel.getWidth(), controlPanelHeight - 5));
		setPackedHeight();
		d = new Dimension(frame.getContentPane().getPreferredSize().width 
				- sliderPanelWidth - 20,
				frame.getPanelHeight() - controlPanelHeight);
		drawPanel.setPreferredSize(d);
		sliderScrollPane.setPreferredSize(new Dimension(sliderScrollPane.getWidth(), d.height));
		updateDimensions();
	}

	private void setPackedHeight() {
		JPanel p = sliderPanel;
		int h = 0;
		for (int i = 0; i < p.getComponentCount(); i++) {
			h += p.getComponent(i).getHeight();
		}
		p.setPreferredSize(new Dimension(sliderPanelWidth, h));
		p.setSize(new Dimension(sliderPanelWidth, h));
	}


	protected void dispose() {
		if (!isEnabled)
			return;
		isEnabled = false;
		prepareToSwapOut();
		while (listenerList.size() > 0) {
			JToggleButton c = listenerList.remove(listenerList.size() - 1);
			c.removeItemListener(buttonListener);
		}
		frame.removeComponentListener(componentListener);
		frame.removeWindowListener(windowListener);
		frame = null;
		if (variables != null)
			variables.dispose();
		variables = null;
	}

	private ComponentListener componentListener = new ComponentAdapter() {

		@Override
		public void componentResized(ComponentEvent e) {
			updateDimensions();
			frameResized();
			frame.contentPane.setPreferredSize(frame.contentPane.getSize());
			frame.tabbedPane.setPreferredSize(frame.contentPane.getPreferredSize());
			frame.pack();
			// frame is packed, so OUTER sizes are set now.
			// but we still have to set the sliderPanel width
			// and height. We provide a provisional setting here
			// and let Variables adjust it as necessary.
			resetPanelHeights();
			frame.pack();
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

	private int sliderPanelWidth;

	/**
	 * From application main(String[] args)
	 * 
	 * @param type
	 * @param args
	 */
	protected static void create(String type, String[] args) {
		IsoApp app = null;
		switch (type) {
		case "IsoDistort":
			app = new IsoDistortApp();
			break;
		case "IsoDiffract":
			app = new IsoDiffractApp();
		}
		app.start(new IsoFrame(type), args, null, false);
	}

	private boolean start(IsoFrame frame, Object[] args, Variables oldVariables, boolean isDrop) {
		this.frame = frame;
		this.args = args;
		frame.contentPane = (JPanel) frame.getContentPane();
		if (frame.tabbedPane == null) {
			frame.contentPane.setLayout(new BorderLayout());
		}
		try {
			long t = System.currentTimeMillis();

			initializePanels();
			addStatus(null);
			addStatus(this + " Java " + System.getProperty("java.version"));

			int fileType = readFile();
			byte[] data = isovisData;
			if (handleNonIsoVizFile(fileType, data))
				return false;
			variables = new Variables(this, appType == APP_ISODIFFRACT, oldVariables != null);
					
			isovisData = variables.parse(data);
			if (oldVariables == null) {
				frame.contentPane.setPreferredSize(new Dimension(variables.appletWidth, variables.appletHeight));
			} else {
//				System.out.println("Fps " + frame.contentPane.getSize()  + frame.tabbedPane.getSize()  
//				+ "\n" + frame.distortionPanel.getSize()
//				+ "\n" + frame.diffractionPanel.getSize()
//				);
				frame.contentPane.setPreferredSize(frame.contentPane.getSize());
			}
			frame.tabbedPane.setPreferredSize(frame.contentPane.getPreferredSize());
			frame.pack();
			// frame is packed, so OUTER sizes are set now.
			// but we still have to set the sliderPanel width
			// and height. We provide a provisional setting here
			// and let Variables adjust it as necessary.
			resetPanelHeights();
			variables.initSliderPanel(sliderPanel, sliderPanelWidth);
			frame.pack();
			updateDimensions();
			init();
			frame.setVisible(true); // #3
			String title = frame.getTitle();
			frame.setName(title);
			frame.setTitle("IsoVIZ ver. " + variables.isoversion + minorVersion);
			frame.addComponentListener(componentListener);
			frame.addWindowListener(windowListener);
			addStatus("Time to load: " + (System.currentTimeMillis() - t) + " ms");
			return true;
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(frame, "Error reading input data " + e.getMessage() + (whichDataFile == null ? "" : " for " + whichDataFile));
			e.printStackTrace();
			return false;
		}

	}

	protected void resetPanelHeights() {
		int sliderPaneHeight = frame.getPanelHeight() - controlPanelHeight - padding;
		if (sliderPanelWidth <= 0)
			sliderPanelWidth = frame.contentPane.getWidth() - sliderPaneHeight - roomForScrollBar - padding;
		sliderScrollPane.setPreferredSize(new Dimension(sliderPanelWidth, sliderPaneHeight));
		JScrollBar bar = sliderScrollPane.getVerticalScrollBar();
		bar.setSize(new Dimension(bar.getWidth(), sliderPaneHeight));
		
		controlStatusPanel.setPreferredSize(
				new Dimension(frame.contentPane.getWidth() - roomForScrollBar, controlPanelHeight));
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
		// System.out.println("IsoA updateDIm" + drawWidth + " " + drawHeight);
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
		}
	}

	// <P><FORM ACTION="isodistortuploadfile.php" METHOD="POST"
	// enctype="multipart/form-data">
	// Import an ISODISTORT distortion file:
	// <INPUT TYPE="hidden" NAME="input" VALUE="distort">
	// <INPUT TYPE="hidden" NAME="origintype" VALUE="dfile">
	// <INPUT CLASS="btn btn-primary" TYPE="submit" VALUE="OK">
	// <input name="toProcess" type="file" size="30">
	// </form>

	/**
	 * Process the distortion file data by passing it to the server and retrieving
	 * the ISODISTORT display page HTML data. Scrape that for INPUT tags, and set
	 * the radio button for origintype to "isovizdistortion". Send this to the
	 * server in order to retrieve the ISOVIZ file. Would be a lot simpler just to
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
			JOptionPane.showMessageDialog(frame,
					"The server was not able to read the data necessary to create ISOVIS file data. ");
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
	 * 
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
	 * If we have form data, clone it, adjust its values, and pass the clone to the
	 * server with a request to construct a new ISOVIZ file
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
				formData = FileUtil.readFileData(this,
						path + whichDataFile.replace("txt", "json").replace("isoviz", "json"));
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

	public void saveImage() {
		FileUtil.saveDataFile(frame, getImage(), "png", false);
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
			}
			;
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

	public void setCursor(int c) {
		frame.setCursor(c == 0 ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(c));
	}

}
