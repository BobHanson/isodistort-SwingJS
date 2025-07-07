package org.byu.isodistort.local;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.StringReader;
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
import org.byu.isodistort.IsoSymmetryApp;
import org.byu.isodistort.local.Cell.ChildCell;
import org.byu.isodistort.server.ServerUtil;

/**
 * 
 * common abstract class for IsoDistortApplet and IsoDiffractApplet
 * 
 * @author Bob Hanson
 *
 */
public abstract class IsoApp {

	final static String minorVersion = ".16_2025.07.06";

	/**
	 * the datafile to use for startup
	 */
	protected String startupDataFile = "data/visual_mno.isoviz";//"data/data29.isoviz"; // varied settings

	/**
	 * The list of menu items in the File.Examples. menu.
	 * 
	 * replace null (even-numbered index) with a label for the File.Examples menu
	 * item
	 * 
	 * or comment out lines to remove them from the menu.
	 * 
	 * Suggest not removing a line completely, as they still could be useful for
	 * debugging.
	 * 
	 */
	public static List<String[]> exampleFiles;
	
	static boolean isJS = (/** @j2sNative true || */
	false);

	protected static boolean addJmol = false;//true; // BH experimental use -jmol
	public static class IsoFrame extends JFrame {
		
	    int sliderPanelWidth;

		String loadingFileName;
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		private JComponent contentPane;
		private JTabbedPane tabbedPane;

		private IsoApp[] apps = new IsoApp[3];

		private int thisType;

		private MenuActions actions;

		IsoFrame(String title) {
			super(title);
			contentPane = (JComponent) getContentPane();
		}

		void addIsoPanel(IsoApp app, JPanel isoPanel) {
			if (actions == null) {
				actions = new MenuActions(app);
				setJMenuBar((JMenuBar) actions.createMenuBar());
			}
			actions.setApp(app);
			int type = app.appType;
			this.apps[type] = app;
			if (tabbedPane == null) {
				tabbedPane = new JTabbedPane();
				tabbedPane.setName("tabbedPane");
				tabbedPane.setFocusable(false);
				JPanel p;
				
				p = new JPanel();
				p.setName("distort");
				p.setBackground(Color.WHITE);
				tabbedPane.addTab("Distortion", p);
	
				
				p = new JPanel();
				p.setName("diffract");
				p.setBackground(Color.WHITE);
				tabbedPane.addTab("Diffraction", p);
				
				
				if (addJmol) {
					p = new JPanel();
					p.setName("jmol");
					p.setBorder(new EmptyBorder(0, 0, 0, 0));
					p.setBackground(Color.WHITE);
					tabbedPane.addTab("Symmetry", p);
				}
				add(tabbedPane);
				tabbedPane.addChangeListener(new ChangeListener() {

					@Override
					public void stateChanged(ChangeEvent e) {
						switchApps();
					}

				});
			}
			((JComponent) tabbedPane.getComponentAt(type)).removeAll();
			((JComponent) tabbedPane.getComponentAt(type)).add(isoPanel);
			tabbedPane.setSelectedIndex(type);
		}

		void enableDiffraction(boolean tf) {
			tabbedPane.setEnabledAt(APP_ISODIFFRACT, tf);
		}

		void disposeApps() {
			for (int i = 0; i < 3; i++) {
				if (apps[i] != null) {
					((JComponent) tabbedPane.getComponentAt(i)).removeAll();
					apps[i].dispose();
					apps[i] = null;
				}
			}
		}

		int getPanelHeight() {
			int h = getContentPane().getHeight();
			return h - 25;
		}

		@Override
		public void repaint() {
			super.repaint();
		}

		protected void switchApps() {
			int type = tabbedPane.getSelectedIndex();
			IsoApp newapp = apps[type];
			IsoApp currentApp = apps[thisType];
			thisType = type;
			if (currentApp == null)
				return;
			if (newapp != null) {
				currentApp.prepareToSwapOut();
				((JComponent) getContentPane()).setTransferHandler(new FileUtil.FileDropHandler(newapp));
				newapp.variables.setValuesFrom(currentApp.variables);
				repaint();
				requestFocus();				
			} else {
				currentApp.openApplication(type, currentApp.isovizData, null, false);
			}
		}

		private ComponentListener componentListener = new ComponentAdapter() {

			Dimension d = new Dimension();
			@Override
			public void componentResized(ComponentEvent e) {
				Dimension d1 = getSize();
				if (d1.equals(d))
					return;
				d = d1;
				resizeMe();
			}
		};

		private WindowListener windowListener = new WindowAdapter() {

			@Override
			public void windowActivated(WindowEvent e) {
			}

			@Override
			public void windowClosed(WindowEvent e) {
			}

			@Override
			public void windowClosing(WindowEvent e) {
				shutDown();
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
			}

			@Override
			public void windowIconified(WindowEvent e) {
			}

		};
		
		void finalizeFrame(IsoApp app) {
			String title = getTitle();
			if (title.indexOf(" ") < 0)
				setName(title);
			setFrameTitle(app.variables);
			setVisible(true);
			addComponentListener(componentListener);
			addWindowListener(windowListener);
			if (actions != null)
				actions.setApp(app);
		}

		protected void resizeMe() {
			//System.out.println("\nIsoframe resized00 "  + getSize() + " " + apps[thisType].drawPanel.getSize());
			resizeAppsPriorToValidation();
			// nothing is happening here.
			apps[thisType].drawPanel.revalidate();
			resizeAppsAfterValidation();
			//System.out.println("Isoframe resized11 " + apps[thisType].drawPanel.getSize() + "\n");
//			repaint();
		}

		void setFrameTitle(Variables variables) {
			setTitle("IsoVIZ ver. " + variables.isoversion + minorVersion
					+ (loadingFileName == null ? "" : " - " + loadingFileName));
		}

		protected void resizeAppsPriorToValidation() {
			for (int i = 0; i < 3; i++) {
				if (apps[i] != null) {
					apps[i].frameResizing();
				}
			}
		}

		protected void resizeAppsAfterValidation() {
			for (int i = 0; i < 3; i++) {
				if (apps[i] != null) {
					apps[i].updateDimensions();
					apps[i].frameResized();
				}
			}
		}

		@Override
		public void dispose() {
			removeComponentListener(componentListener);
			removeWindowListener(windowListener);
		}

		protected void shutDown() {
			for (int i = 0; i < 3; i++) {
				if (apps[i] != null) {
					apps[i].shutDown();
					apps[i].updateDimensions();
					apps[i].frameResized();
				}
			}
			
		}

	}

	private class StatusPanel extends JScrollPane {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		JTextArea area;

		boolean isLockedVisible = false;

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

//		void setDimension(Dimension d) {
//			setPreferredSize(new Dimension(d.width - 2, d.height));
//			area.setPreferredSize(new Dimension(d.width - 10, 1000));
//		}
//
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

	final static protected int APP_ISODISTORT = 0;
	final static protected int APP_ISODIFFRACT = 1;
	final static protected int APP_ISOSYMMETRY = 2;
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
	private static final int padding = 0;
	
	private static final int controlPanelHeight = 75;
	
	private static final int roomForScrollBar = 10;

	private static final int defaultSliderPanelWidth = 450;

	private static int buttonID = 0;

	final static Border controlBorder = new EmptyBorder(0, 0, 0, 0);// BorderFactory.createLineBorder(Color.BLACK, 1);

	final static Border statusBorder = BorderFactory.createLineBorder(Color.RED, 1);

	/**
	 * From application main(String[] args)
	 * 
	 * @param type
	 * @param args
	 */
	protected static void create(String type, String[] args) {
		int appType = ("IsoDiffract".equals(type) ? APP_ISODIFFRACT : APP_ISODISTORT);
		if (!startApp(null, new IsoFrame(type), appType, args, null, null, false)) {
			System.exit(1);
		}
	}

	private static JButton newJButton(String text, ViewListener vl) {
		JButton b = new JButton(text);
		b.setFocusable(false);
		//b.setMargin(new Insets(-3, 3, -2, 4));
		b.setHorizontalAlignment(SwingConstants.LEFT);
		b.setVerticalAlignment(SwingConstants.CENTER);
		b.addActionListener(vl);
		return b;
	}

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
	public IsoFrame frame;
	/**
	 * drawing area width and height
	 */
	public int drawWidth, drawHeight;

	protected JButton applyView;// ,
	// saveImage, saveISOVIZ,
	// openOther;

	protected JCheckBox colorBox;

	public int initializing = 5; // lead needed to get first display. Why?

	/**
	 * Pane that holds the scroll panels
	 */
	private JScrollPane sliderScrollPane;

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
	protected byte[] isovizData = new byte[0];

	/**
	 * the distortion file data associated with this app in byte[] form
	 */
	protected byte[] distortionFileData;

	/**
	 * a list of listeners used to remove listeners by dispose()
	 */
	private List<JToggleButton> listenerList = new ArrayList<>();

	/**
	 * listens for the check boxes that highlight a given atomic subtype.
	 */
	protected ItemListener buttonListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent event) {
			if (event.getSource() == colorBox) {
				variables.updateColorScheme(!colorBox.isSelected());
			}
			if (!isAdjusting)
				updateViewOptions();
			handleButtonEvent(event.getSource());
		}
	};

	private JPanel controlStatusPanel;

	protected ActionListener textBoxListener = new ActionListener() {

		@Override
		public void actionPerformed(ActionEvent e) {
			if (appType == APP_ISODISTORT) {
				applyView();
			} else {
				needsRecalc = true;
				updateDisplay();
			}
			SwingUtilities.invokeLater(() -> {
				((JComponent) e.getSource()).requestFocus();
			});
		}

	};

	protected double getText(JTextField t, double currentValue, int precision) {
		double val = 0;
		try {
			String s = t.getText();
			if (s.length() == 0) {
				t.setText(precision == 2 ? MathUtil.trim00(currentValue) : MathUtil.varToString(currentValue, precision, 0));
				val = currentValue;
			} else {
				val = Double.parseDouble(s);
			}
			t.setForeground(Color.BLACK);
		} catch (NumberFormatException e) {
			val = currentValue;
			t.setForeground(Color.RED);
			SwingUtilities.invokeLater(() -> {
				((JComponent) t).requestFocus();
			});
		}
		controlPanel.repaint();
		return val;
	}

	/**
	 * true if Variables.modDim > 0 based on ISOViz !numberOfModulations > 0 or
	 * FormData "xkparam... presence or DFile "!
	 */
	public Boolean isIncommensurate;

	/**
	 * cell for the child, aka "super" cell
	 * 
	 */
	public ChildCell childCell = new ChildCell();

	/**
	 * cell for the parent
	 */
	public Cell.ParentCell parentCell = new Cell.ParentCell();

	public static boolean testing;

	protected IsoApp(int appType) {
		this.appType = appType;
		statusPanel = new StatusPanel(new JTextArea());

	}

	/**
	 * Add general buttons to the top control panel right side.
	 * 
	 * @param top
	 */
	protected void addTopButtons(JPanel top) {
		colorBox = newJCheckBox("Unique-atom Colors", false);
		colorBox.setVisible(variables.needColorBox);
		top.add(colorBox);
		fillLabel(top);
	}

	/**
	 * Add general buttons to bottom control panel right side.
	 * 
	 * @param bottom
	 */
	protected void addBottomButtons(JPanel bottom) {
		switch (appType) {
		case APP_ISODIFFRACT:
			break;
		case APP_ISODISTORT:
			ViewListener vl = new ViewListener();
			applyView = newJButton("Apply View", vl);
			break;
		}
		fillLabel(bottom);
	}

	private void fillLabel(JPanel p) {
		JLabel fill = new JLabel("X");
		fill.setFont(new Font(Font.DIALOG, 0, 24));
		fill.setOpaque(true);
		//p.add(fill);
	}

	public void addStatus(String status) {
		statusPanel.addText(status);
	}

	/**
	 * The "Apply View" action.
	 * 
	 */
	abstract protected void applyView();

	/**
	 * Center structure in window.
	 */
	abstract public void centerImage();

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

	protected void dispose() {
		if (!isEnabled)
			return;
		isEnabled = false;
		prepareToSwapOut();
		while (listenerList.size() > 0) {
			JToggleButton c = listenerList.remove(listenerList.size() - 1);
			c.removeItemListener(buttonListener);
		}
		frame = null;
		if (variables != null)
			variables.dispose();
		variables = null;
	}

	/**
	 * Process the distortion file data by passing it to the server and retrieving
	 * the ISODISTORT display page HTML data. Scrape that for INPUT tags, and set
	 * the radio button for origintype to "isovizdistortion". Send this to the
	 * server in order to retrieve the ISOVIZ file. Would be a lot simpler just to
	 * get the ISOVIZ file from the server directly.
	 * 
	 * @param f
	 */
	private String distortionFileToISOVIZ(String fileName, byte[] data) {
		// 1) send distortion file data to the isodistort server
		if (appType == APP_ISODIFFRACT && FileUtil.checkIncommensurateDFile(data)) {
			return "Diffraction is not available for an incommensurately modulated structure";
		}
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
				sendFormDataToServer(FileUtil.scrapeHTML(IsoApp.this, new String(b)), false);
			}

		}, 5);
		return null;
	}

	private Map<String, Object> ensureMapData(Object formData, boolean asClone, boolean silent) {
		if (formData == null) {
			formData = this.formData;			
		}
		Map<String, Object> mapData = (formData instanceof String && ((String) formData).length() == 0 ? null
				: ServerUtil.json2Map(formData, asClone));
		if (mapData == null) {
			// if all we have is an isoviz file, how can we update it?
			if (!silent)
				sayNotPossible("No form data to process; open a DISTORTION file first.");
			return null;
		}
		fixFormBondMinMax(mapData);
		this.formData = mapData;
		return mapData;
	}

	private void fixFormBondMinMax(Map<String, Object> map) {
		boolean testing1 = false;
		if (testing1) {
		Object min = map.get("bondlengthmin");
		Object max = map.get("bondlength");
		if (min != null)
			map.put("minbondlength", min);
		if (max != null)
			map.put("maxbondlength", max);
		}
	}

	/**
	 * This method is run prior to frame "validation" (when the layout is done, and
	 * preferred sizes are checked). 
	 * 
	 * All we are doing here is setting the control
	 * (bottom) panel and isoPanel preferred sizes. 
	 * 
	 * This sets the overall width and height preferences. 
	 * SliderPanel width All the other sizes are set automatically from that.
	 */
	public void frameResizing() {
		int w = frame.tabbedPane.getWidth();
		controlPanel.setPreferredSize(new Dimension(w - 5, controlPanelHeight));
		statusPanel.setPreferredSize(new Dimension(w - 5, controlPanelHeight - 5));
		// this next is important because of the scrollbar
		isoPanel.setPreferredSize(new Dimension(w, frame.tabbedPane.getHeight() - 40));
		
	}

	/**
	 * Frame has been resized -- update renderer and display. This will occur
	 * inititially and upon user resize drag.
	 * 
	 */
	protected void frameResized() {
//		System.out.println("frame resized " + frame.getSize());
//		System.out.println("contentPane " + frame.contentPane.getSize());
//		System.out.println("tabbedPane " + frame.tabbedPane.getSize());
//		System.out.println("isopanel " + isoPanel.getSize());
//		System.out.println("sliderScrollPane " + sliderScrollPane.getSize());
//		System.out.println("sliderPanel " + sliderPanel.getSize());
//		System.out.println("controlStatusPanel resized " + controlStatusPanel.getSize());
//		System.out.println("drawPanel now " + drawPanel.getSize());
	
		updateDimensions();
	}

	/**
	 * Get the image from the renderer for saving.
	 * 
	 * @return the current image of drawing frame.
	 */
	abstract protected BufferedImage getImage();

	/**
	 * The click callback comes to IsoApp and is distributed to the applet by this
	 * method.
	 * 
	 * @param src
	 */
	abstract protected void handleButtonEvent(Object src);

	private boolean handleNonIsoVizFile(int fileType, byte[] data) {
		String err = null;
		switch (fileType) {
		case FileUtil.FILE_TYPE_ISOVIZ:
			return false;
		case FileUtil.FILE_TYPE_FORMDATA_JSON:
			Map<String, Object> formData = ensureMapData(new String(data), true, false);
			err = sendFormDataToServer(formData, true);
			break;
		case FileUtil.FILE_TYPE_DISTORTION_TXT:
			err = distortionFileToISOVIZ(startupDataFile == null ? "temp.isoviz" : startupDataFile, data);
			break;
		default:
			err = "File type was not recognized.";
			break;
		}
		if (err == null) {
			setStatusVisible(false);
		} else {
			sayNotPossible(err);
		}
		return true;
	}

	/**
	 * The variables are all read. Time to do any app-specific initialization before
	 * we make the frame visible.
	 */
	abstract protected void init();

	/**
	 * [-tab----tab---------------------------------|
	 * |------------------isoPanel------------------|
	 * |.................................|..........|
	 * |.................................|.slider...|
	 * |..........drawPanel..............|..scroll..|
	 * |............/   \................|...pane/..|
	 * |....RenderPanel..\...............|.slider...|
	 * |................RenderPanel3D....|...panel..|
	 * |.................................|..........|
	 * |.................................|..........|
	 * |.------------------------------------------.|
	 * |..............controlStatusPanel............|
	 * |............................................|
	 * |--------------------------------------------|
	 */

	protected void initializePanels() {
		if (frame.tabbedPane == null) {
			frame.contentPane = (JPanel) frame.getContentPane();
			frame.contentPane.setLayout(new BorderLayout());
		}
		frame.contentPane.setTransferHandler(new FileUtil.FileDropHandler(this));
		controlStatusPanel = new JPanel(new FlowLayout());
		controlStatusPanel.setName("controlStatusPanel");
		controlStatusPanel.setBackground(Color.WHITE);
		controlPanel = new JPanel();
		controlPanel.setName("controlPanel");
		controlPanel.setBackground(Color.WHITE);
		controlPanel.setLayout(new GridLayout(2, 1, 0, -5));
		controlStatusPanel.add(controlPanel);
		controlStatusPanel.add(statusPanel);
		controlStatusPanel.setBorder(controlBorder);
		isoPanel = new JPanel(new BorderLayout());
		isoPanel.setName("isoPanel");
		if (sliderPanel == null) {
			sliderPanel = new JPanel();
			sliderPanel.setName("sliderPanel");
			sliderPanel.setBackground(Color.WHITE);
			sliderPanel.setLayout(new BoxLayout(sliderPanel, BoxLayout.PAGE_AXIS));
			// sets grid length equal to number of rows.
		}
		sliderScrollPane = new JScrollPane(sliderPanel);
		sliderScrollPane.setBackground(Color.YELLOW);
		sliderScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		sliderScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		isoPanel.add(sliderScrollPane, BorderLayout.EAST);
		isoPanel.add(controlStatusPanel, BorderLayout.SOUTH);
		drawPanel = new JPanel(new BorderLayout());
		drawPanel.setName("drawPanel");
		isoPanel.add(drawPanel);
		frame.addIsoPanel(this, isoPanel);
		if (appType == APP_ISODISTORT) {
			boolean isModulated = (variables.modDim != 0);
			frame.enableDiffraction(!isModulated);
			if (isModulated)
				addStatus("NOTE: Diffraction is not available for an incommensurately modulated structure");
		}
	}

	public boolean isStatusVisible() {
		return statusPanel.isVisible();
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

	protected JRadioButton newRadioButton(String label, boolean selected, ButtonGroup g) {
		JRadioButton b = new JRadioButton(label, selected);
		b.setName(++buttonID + ":" + label);
		b.setHorizontalAlignment(JRadioButton.LEFT);
		b.setVerticalAlignment(JRadioButton.CENTER);
		b.setFocusable(false);
		b.setOpaque(false);
		b.setForeground(Color.BLACK);
		b.setVisible(true);
		b.setBorderPainted(false);
		b.addItemListener(buttonListener);
		g.add(b);
		listenerList.add(b);
		return b;
	}

	protected JTextField newTextField(String text, int insetRight) {
		JTextField t = new JTextField(text, 3);
		//t.setMargin(new Insets(-2, 0, -1, insetRight));
		t.addActionListener(textBoxListener);
		t.addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				t.selectAll();
			}

			@Override
			public void focusLost(FocusEvent e) {
			}
		});
		
		return t;
		
	}

	private void openApplication(int appType, byte[] data, Map<String, Object> mapFormData, boolean isDrop) {
		if (data == null)
			return;
		if (!prepareToSwapOut())
			return;
		IsoFrame frame = this.frame;
		SwingUtilities.invokeLater(() -> {
			new Thread(() -> {
				startApp(this, frame, appType, null, data, mapFormData, isDrop);
			}, "isodistort_application").start();

		});
	}

	private static boolean startApp(IsoApp fromApp, IsoFrame frame, int appType, Object[] args, byte[] data,
			Map<String, Object> mapFormData, boolean isDrop) {
		IsoApp app;
		switch (appType) {
		default:
		case APP_ISODISTORT:
			app = new IsoDistortApp();
			break;
		case APP_ISODIFFRACT:
			app = new IsoDiffractApp();
			break;
		case APP_ISOSYMMETRY:
			app = new IsoSymmetryApp();
			break;
		}
		app.frame = frame;
		if (args == null || args.length == 0)
			args = new Object[] { data };
		app.addStatus(null);
		int fileType = app.readStartupFile(args);
		data = app.isovizData;
		if (app.handleNonIsoVizFile(fileType, data))
			return false;
		boolean isSwitch = (!isDrop && fromApp != null);
		IsoTokenizer vt = new IsoTokenizer(data, null, isSwitch ? IsoTokenizer.QUIET : IsoTokenizer.DEBUG_LOW);
		app.variables = new Variables(app);
		try {
			if (!app.variables.parse(vt))
				return false;
			Variables oldVariables = (isSwitch ? fromApp.variables : null);
			if (isDrop) {
				frame.disposeApps();
				app.formData = mapFormData;
				if (fromApp != null) {
					fromApp = null;
				}
			}
			long t = System.currentTimeMillis();
			app.prepareFrame(frame, oldVariables, isDrop);
			app.updateDimensions();
			app.init();
			frame.finalizeFrame(app);
			app.addStatus("Time to load: " + (System.currentTimeMillis() - t) + " ms");
		} catch (Throwable e) {
			JOptionPane.showMessageDialog(frame, "Error reading input data " + e.getMessage()
					+ (app.startupDataFile == null ? "" : " for " + app.startupDataFile));
			e.printStackTrace();
			return false;
		}
		if (fromApp == null) {
			// I can't figure out why IsoDiffract is slightly different size
			// from IsoDistort until after a tweak of the sizing
		} else {
			app.document = fromApp.document;
			app.formData = fromApp.formData;
			app.startupDataFile = fromApp.startupDataFile;
			app.distortionFileData = fromApp.distortionFileData;
			app.isIncommensurate = fromApp.isIncommensurate;
			app.variables.setValuesFrom(fromApp.variables);
		}
		app.frameResized();
		return true;
	}

	private void prepareFrame(IsoFrame frame, Variables oldVariables, boolean isDrop) {
		initializePanels();
		boolean haveFrame = (isDrop || oldVariables != null);
		addStatus(null);
		addStatus(this + " Java " + System.getProperty("java.version"));
		Dimension d = (haveFrame ? frame.contentPane.getSize()
				: new Dimension(variables.appletWidth, variables.appletHeight));
		frame.contentPane.setPreferredSize(d);
		frame.pack();
		resetPanelHeights();
		variables.initSliderPanel(sliderPanel, frame.sliderPanelWidth);
	}

	/**
	 * This method allows the current application to finish up what it is doing
	 * prior to swapping out.
	 * 
	 * @return false to disallow swapping out at this moment.
	 * 
	 */
	abstract protected boolean prepareToSwapOut();

	abstract public void recalcCellColors();

	/**
	 * Reset View.
	 */
	abstract public void reset();

	protected void resetPanelHeights() {
		int sliderPaneHeight = frame.getPanelHeight() - controlPanelHeight - padding;
		if (frame.sliderPanelWidth <= 0)
			frame.sliderPanelWidth = defaultSliderPanelWidth;
		sliderScrollPane.setPreferredSize(new Dimension(frame.sliderPanelWidth + roomForScrollBar, sliderPaneHeight));
		JScrollBar bar = sliderScrollPane.getVerticalScrollBar();
		bar.setSize(new Dimension(bar.getWidth(), sliderPaneHeight));

		controlStatusPanel
				.setPreferredSize(new Dimension(frame.contentPane.getWidth() - roomForScrollBar, controlPanelHeight));
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
					saveDataFileVerbose(b, "cif");
				}

			}, 20);
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
					saveDataFileVerbose(b, "iso.txt");
				}
			}, 20);
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
			saveDataFileVerbose(ServerUtil.toJSON(map), "json");
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
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_FULLPROF_PCR, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}
					saveDataFileVerbose(b, "pcr");
				}
			}, 20);
		}

	}

	public void saveImage() {
		saveDataFileVerbose(getImage(), "png");
	}

	public void saveIsoviz(Map<String, Object> values) {
		// if we loaded some other form of file, we should have formData, and
		// we can create a new isoviz file on the fly.
		Map<String, Object> map = null;
		String options = "";
		if (isovizData != null) {
			// if we loaded an ISOVIZ file we can save it directly, and isovizData will not
			// be null.
			// but then there are no options to save the current settings.
			map = new LinkedHashMap<String, Object>();
			options = "child";
		}
		if (formData != null) {
			map = ensureMapData(formData, true, false);
			options += "new";
		} else if (isovizData == null) {
			map = ensureMapData("", true, false); // error
		}
		if (map == null)
			return;
		if (values == null) {
			map.put("slidersetting", options);
			IsoDialog.openIsovizDialog(this, map);
		} else {
			// after dialog
			map.remove("slidersetting");
			String setting = (String) values.get("slidersetting");
			if (setting.equals("original")) {
				saveDataFileVerbose(isovizData, "isoviz");
				return;
			}
			if (isovizData != null) {
				byte[] data = new byte[isovizData.length];
				System.arraycopy(isovizData, 0, data, 0, data.length);
				variables.setIsovizFileData(data, setting);
				saveDataFileVerbose(data, "isoviz");
				return;
			}
			updateFormData(map, values, "isovizdistortion");
			setStatus("...fetching ISOVIZ file from iso.byu...");
			ServerUtil.fetch(this, FileUtil.FILE_TYPE_ISOVIZ, map, new Consumer<byte[]>() {
				@Override
				public void accept(byte[] b) {
					if (b == null) {
						addStatus("upload failed");
						return;
					}
					saveDataFileVerbose(b, "isoviz");
				}
			}, 20);
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
					saveDataFileVerbose(b, "STR");
				}
			}, 20);
		}

	}

	private void sayNotPossible(String msg) {
		say("This feature is not available: " + msg);
	}

	protected void say(String msg) {
		JOptionPane.showMessageDialog(frame, msg);
	}

	protected String sendFormDataToServer(Map<String, Object> formData, boolean checkIncommensurate) {
		Map<String, Object> mapFormData = setServerFormOriginType(formData, "isovizdistortion");
		if (mapFormData == null) {
			return "The server was not able to read the data necessary to create ISOVIS file data.";
		}
		boolean isIncommensurate = (checkIncommensurate && FileUtil.checkIncommensurateFormData(formData));
		if (isIncommensurate && appType == APP_ISODIFFRACT) {
			return "Diffraction is not available for an incommensurately modulated structure";
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
		return null;
	}

	public void setCursor(int c) {
		frame.setCursor(c == 0 ? Cursor.getDefaultCursor() : Cursor.getPredefinedCursor(c));
	}

	public void setFormData(Map<String, Object> formData, String sliderSetting) {
		variables.setWebFormData(formData, sliderSetting);
	}

	public void setIsovizData(byte[] isovizData, String sliderSetting) {
		variables.setIsovizFileData(isovizData, sliderSetting);
	}

//Save for debugging
//	private void setPackedHeight() {
////		int h = getComponentHeight(sliderPanel, "setPackedHeight");
////		sliderPanel.setPreferredSize(new Dimension(sliderPanelWidth, h));
////		sliderPanel.setMaximumSize(new Dimension(sliderPanelWidth, h));
////		sliderPanel.setMinimumSize(new Dimension(sliderPanelWidth, h));
////		sliderPanel.setSize(new Dimension(sliderPanelWidth, h));
//	}
//
//save for debugging
//
//	private int getComponentHeight(JPanel p, String msg) {
//		int h = 0;
//		for (int i = 0; i < p.getComponentCount(); i++) {
//			Component child = p.getComponent(i);
//			int ch = (child instanceof JComponent ? ((JComponent) child).getPreferredSize().height : child.getHeight());
//			h += ch;
//			System.out.println(msg + " "  + i + " " + ch + " " + h + " ps=" + child.getPreferredSize() + " " + child.getClass().getName() + " "  + child.getName() );
//			if (child instanceof JPanel) {
//				getComponentHeight((JPanel) child, msg + "--");
//			}
//		}
//		return h;
//	}
//
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
				sendFormDataToServer(map, false);
			}
		}
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

	public void setStatus(String status) {
		addStatus(status);
		setStatusVisible(true);
	}

	public void setStatusVisible(boolean b) {
		statusPanel.setVisible(b, false);
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

	public boolean toggleStatusVisible() {
		boolean b = !statusPanel.isVisible();
		statusPanel.setVisible(b, true);
		return b;
	}

	protected void updateDimensions() {
//		System.out.println("\nIsoApp.updateDime " + drawPanel.getSize());
		drawWidth = drawPanel.getWidth();
		drawHeight = drawPanel.getHeight();
//		Component c = drawPanel;
//		while (c != null) {
//			System.out.println(c.getName() + " " + c.getBounds() + " " + (c instanceof JComponent ? ((JComponent) c).getBorder(): ""));
//			c = c.getParent();
//		}
//		System.out.println(controlStatusPanel.getBounds());
//		System.out.println(controlPanel.getBounds());
//		System.out.println(statusPanel.getBounds());
	}

	/**
	 * Something has changed.
	 */
	public void updateDisplay() {
		variables.updateSliderPointers();
	}

	abstract protected void updateViewOptions();

	private void updateFormData(Map<String, Object> map, Map<String, Object> values, String originType) {
		String sliderSetting = "current";
		if (values != null) {
			sliderSetting = (String) values.remove("slidersetting");
			System.out.println(map);
			System.out.println(values);
			map.putAll(values);
			System.out.println(map);
		}
		setFormData(map, sliderSetting);
		setServerFormOriginType(map, originType);
	}

	public void viewPage(String originType) {
		Map<String, Object> map = ensureMapData(null, true, false);
		if (map == null)
			return;
		updateFormData(map, null, originType);
		ServerUtil.displayIsoPage(this, map);
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

	protected void saveDataFileVerbose(Object data, String fileType) {
		FileUtil.saveDataFile(this, data, fileType, false);
	}

	/**
	 * This data reader has two modes of operation.
	 * 
	 * For data files, it can accept byte[], String, or InputStream data.
	 * 
	 * The alternative is to read a single-string data sequence directly from the
	 * html file that calls the app (injected as isoData).
	 * 
	 * The Variables class then has a method that parses the byte[] representation
	 * of the string.
	 */
	private int readStartupFile(Object[] args) {
		this.args = args;
		String tests;
		int n = (args == null || args.length == 0 || args[0] == null ? 0 : args.length);
		if (n > 0 && args[n - 1] instanceof String 
				&& (tests = args[n-1].toString()).startsWith("-")) {
			if (tests.contains("-test"))
				testing = true;
			if (tests.contains("-jmol"))
				addJmol = true;
			n--;
		}
		switch (n) {
		case 3:
			document = args[2];
			// Fall through //
		case 2:
			// note this has "bondlength" and "bondlenghmin"
			
			formData = args[1]; // String or Map
			// Fall through //
		case 1:
			isovizData = (args[0] instanceof byte[] ? (byte[]) args[0] : args[0].toString().getBytes());
			startupDataFile = null;
			break;
		default:
			isovizData = getExampleFileData(startupDataFile);
			break;
		}
		return FileUtil.getIsoFileTypeFromContents(isovizData);
	}

	/**
	 * Asynchronously open a file.
	 */
	public void openFile() {
		SwingUtilities.invokeLater(() -> {
			FileUtil.openFile(this, (File f) -> {
				if (f != null)
					loadFile(f);
			});
		});
	}

	/**
	 * @param f
	 * @return
	 */
	public void loadFile(File f) {
		if (f == null)
			return;
		frame.loadingFileName = f.getName();
		loadFileData(FileUtil.readFileOrResource(this, f.getAbsolutePath()));
	}

	public void openExampleFile(String exampleFile) {
		loadFileData(getExampleFileData(exampleFile));
	}

	/**
	 * 
	 * @param exampleFile
	 * @return
	 */
	private byte[] getExampleFileData(String exampleFile) {
		frame.loadingFileName = new File(exampleFile).getName();
		return FileUtil.readFileOrResource(this, exampleFile);
	}

	/**
	 * Load byte data into the appropriated application.
	 * 
	 * @param data
	 */
	private void loadFileData(byte[] data) {
		if (data == null || handleNonIsoVizFile(FileUtil.getIsoFileTypeFromContents(data), data)) {
			return;
		}
		new Thread(() -> {
			openApplication(appType, data, null, true);
		}, "isodistort_file_opener").start();
	}

	public List<String[]> getExampleFiles() {
		if (exampleFiles == null) {
			exampleFiles = new ArrayList<String[]>();
			try {
				String files = new String(FileUtil.readFileOrResource(this, "data/_menu_examples.txt"));
				if (files != null) {
				}
				BufferedReader br = new BufferedReader(new StringReader(files));
				String line;
				while ((line = br.readLine()) != null) {
					line = line.trim();
					if (line.length() == 0 || line.startsWith("#"))
						continue;
					if (!line.contains("="))
						line = "=" + line;
					String[] info = line.split("=");
					if (info[0].length() == 0)
						info[0] = null;
					exampleFiles.add(info);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return exampleFiles;
	}


}
