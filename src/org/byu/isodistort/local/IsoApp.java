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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
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
import javax.swing.SwingUtilities;

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

	public final static int DIS = Mode.DIS; // displacive
	public final static int OCC = Mode.OCC; // occupancy (aka "scalar")
	public final static int MAG = Mode.MAG; // magnetic
	public final static int ROT = Mode.ROT; // rotational
	public final static int ELL = Mode.ELL; // ellipsoidal
	
	final static String minorVersion = ".7b"; 
	
	/**
	 * The variables are all read. Time to do any app-specific 
	 * initialization before we make the frame visible. 
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
	 * The click callback comes to IsoApp and is distributed
	 * to the applet by this method.
	 * 
	 * @param src
	 */
	abstract protected void handleButtonEvent(Object src);
	
	/**
	 * When an app is swapped back in, this method allows the 
	 * app to reload its settings. 
	 * 
	 * @param app
	 */
	abstract protected void setControlsFrom(IsoApp app);

	/**
	 * This method allows the current application to finish up 
	 * what it is doing prior to swapping out. 
	 * 
	 * @return false to disallow swapping out at this moment.
	 * 
	 */
	abstract protected boolean prepareToSwapOut();
	

	final static int SETTINGS_PERSPECTIVE = 0;
	final static int SETTINGS_APP = 1;

	private static final int padding = 4;
	private static final int controlPanelHeight = 55;
	private final static int roomForScrollBar = 15;
	
	protected Object[] args;

	private IsoApp[] appSettings = new IsoApp[2];

	/**
	 * Preserve just the app itself and its current perspective
	 */
	void preserveAppData() {
		appSettings[appType] = this;
	}

	private void clearSettings() {
		appSettings = new IsoApp[2];
	}

	
	
	private List<JToggleButton> listenerList = new ArrayList<>();
	
	/** listens for the check boxes that highlight a given atomic subtype. */
	protected ItemListener buttonListener = new ItemListener() {
		@Override
		public void itemStateChanged(ItemEvent event) {
			handleButtonEvent(event.getSource());
		}
	};
	
	static int buttonID = 0;

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


	/**
	 * The panel displaying this application
	 */
	protected JPanel frameContentPane;

	protected boolean isEnabled = true;

	protected boolean isAdjusting = false;

	protected boolean needsRecalc = true;

	protected JButton applyView, saveImage, saveISOVIZ, openOther;
	

	public int initializing = 5; // lead needed to get first display. Why?

	/** Pane that holds the scroll panels */
	private JScrollPane sliderPane;

	/** Panel that holds all control components */
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
	
	/** An instance of the Variables class that holds all the input data */
	protected Variables variables;
	/** A string containing all of the input data */
	protected Object isoData = "";
	/** False for datafile and True for html file */
	protected boolean readMode = false;
	/** The datafile to use when readMode is false */
	protected String whichdatafile = "data/test28.txt";//"data/test28.txt";

	protected JFrame frame;

	protected Object document;

	protected Object formData;

	/**
	 * Image width
	 */
	protected int drawWidth;

	/**
	 * Image height
	 */
	protected int drawHeight;

	protected int appType;


	private JPanel isoPanel;
	
	final static protected int APP_ISODISTORT = 0;
	final static protected int APP_ISODIFFRACT = 1;
	
	protected IsoApp(int appType) {
		this.appType = appType;
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
	 * The Variables class then has a method that parses the byte[] representation of the string.
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

	boolean asBytes = true;
	//boolean asInputStream = true;
	private Object readFileData(String path) {

		try {
			if (asBytes) {
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
				return FileUtil.getLimitedStreamBytes(bis, Integer.MAX_VALUE, true);
			}
					
			BufferedReader br = null;
			try {
				br = new BufferedReader(new FileReader(path));
			} catch (Exception e) {
				Object data = getClass().getResource("/" + path).getContent();
				br = new BufferedReader(new InputStreamReader((FilterInputStream) data));

			}
			long t = System.currentTimeMillis();
			StringBuffer dataString = new StringBuffer();
			dataString.append(readLineSkipComments(br));
			if (dataString.indexOf("!isoversion") != 0) {
				br.close();
				return null;
			}
			dataString.append('\n');
			String s;
			while ((s = readLineSkipComments(br)) != null) {
				dataString.append(s).append('\n');
			}
			br.close();
			System.out.println("File " + path + "\nBytes read: " + dataString.length() + " in "+(System.currentTimeMillis() - t)+" ms");
			return dataString.toString();
		} catch (IOException exception) {
			exception.printStackTrace();
			System.out.println("Oops. File not found.");
			return null;
		}
	}

	private String readLineSkipComments(BufferedReader br) throws IOException {
		String s = null;
		while ((s = br.readLine()) != null) {
			if (s.trim().length() != 0 && s.charAt(0) != '#')
				break;
			}
		return (s == null ? null : s);
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
		String ext = FileUtil.getExtension(f, "isoviz*");
		if (ext == null)
			return false;
		switch (ext) {
		case "isoviz":
		case "isovizq":
			break;
		default:
			return false;
		}
		Object data = readFileData(f.getAbsolutePath());
		if (data == null)
			return false;
		boolean isIsoDistort = (appType == APP_ISODISTORT);
		boolean isMyType = (isIsoDistort == (ext.equals("isoviz")));
		boolean doSwitch = false;
		if (!isMyType) {
			String msg = "This file is intended for " + (appType == APP_ISODISTORT ? "ISODIFFRACT" : "ISODISTORT")
					+ ". Would you like to switch to that application?";
			doSwitch = (JOptionPane.showConfirmDialog(null, msg, "File Drop Application",
					JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION);
		}
		if (doSwitch) {
			isIsoDistort = !isIsoDistort;
		}

		int appType = isIsoDistort ? APP_ISODISTORT : APP_ISODIFFRACT;
		SwingUtilities.invokeLater(() -> {
			openApplication(appType, data, true);
		});
		return true;

	}

	private void openApplication(int appType, Object data, boolean isDrop) {
		if (data == null)
			return;
		if (!prepareToSwapOut())
			return;
		IsoApp me = this;
		SwingUtilities.invokeLater(() -> {
			boolean isIsoDistort = (appType == APP_ISODISTORT);
			IsoApp app = (isIsoDistort ? new IsoDistortApp() : new IsoDiffractApp());
			if (args == null || args.length == 0)
				args = new Object[1];
			args[0] = data;
			JFrame frame = me.frame;
			dispose();
			app.start(frame, args, variables, isDrop);
			if (isDrop) {
				clearSettings();
			} else {
				app.appSettings = appSettings;
				app.variables.setValuesFrom(variables);
				app.setControlsFrom((IsoApp) appSettings[app.appType]);				
			}
			app.frameResized();
		});
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
	
	private WindowListener windowListener  = new WindowAdapter() {

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
			System.out.println("Time to load: "+(System.currentTimeMillis() - t)+" ms");			
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

	/**
	 * 
	 * @param consumer in case we need to go asynchronous on this.
	 */
	public void getData(Consumer<Object> consumer) {
		Object data = isoData;
		// just the original data so far. 
		consumer.accept(data);
	}

	String testData = "#isodistort_version_number \r\n" + "!isoversion 6.12\r\n" + "\r\n"
			+ "#atom_sphere_radius_in_angstroms \r\n" + "!atommaxradius    0.40000\r\n" + "\r\n"
			+ "#angstroms_per_magneton \r\n" + "!angstromspermagneton    0.50000\r\n" + "\r\n"
			+ "#angstroms_per_radian \r\n" + "!angstromsperradian    4.00000\r\n" + "\r\n"
			+ "#default_isotropy_uiso_parameter \r\n" + "!defaultuiso    0.04000\r\n" + "\r\n"
			+ "#maximum_bond_length_in_angstroms \r\n" + "!maxbondlength    2.50000\r\n" + "\r\n"
			+ "#view_width_in_pixels \r\n" + "!appletwidth   1024\r\n" + "\r\n"
			+ "#parentcell_parameters_in_angstroms_and_radians \r\n" + "!parentcell \r\n"
			+ "        4.20000        4.20000        4.20000        1.57080        1.57080        1.57080\r\n" + "\r\n"
			+ "#parent_origin_in_supercell_units \r\n" + "!parentorigin \r\n" + "   0.00000   0.00000   0.00000\r\n"
			+ "\r\n" + "#rows_are_parent_basis_vectors_in_supercell_units \r\n" + "!parentbasis \r\n"
			+ "   0.50000  -0.50000   0.00000\r\n" + "   0.50000   0.50000   0.00000\r\n"
			+ "   0.00000   0.00000   0.50000\r\n" + "\r\n" + "#parentatom/label/element \r\n" + "!atomtypelist \r\n"
			+ "   1 Sr Sr \r\n" + "   2 Ti Ti \r\n" + "   3 O O \r\n" + "\r\n" + "#parentatom/subatom/label \r\n"
			+ "!atomsubtypelist \r\n" + "   1   1 Sr_1 \r\n" + "   2   1 Ti_1 \r\n" + "   3   1 O_1 \r\n"
			+ "   3   2 O_2 \r\n" + "\r\n" + "#parentatom/type/subatom/x/y/z/_for_each_subatom \r\n"
			+ "!atomcoordlist \r\n" + "    1    1    1   0.50000   0.00000   0.25000 \r\n"
			+ "    1    1    2   0.50000   1.00000   0.25000 \r\n"
			+ "    1    1    3   0.50000   0.00000   0.75000 \r\n"
			+ "    1    1    4   0.50000   1.00000   0.75000 \r\n"
			+ "    1    1    5   0.00000   0.50000   0.25000 \r\n"
			+ "    1    1    6   1.00000   0.50000   0.25000 \r\n"
			+ "    1    1    7   0.00000   0.50000   0.75000 \r\n"
			+ "    1    1    8   1.00000   0.50000   0.75000 \r\n"
			+ "    2    1    1   0.00000   0.00000   0.00000 \r\n"
			+ "    2    1    2   0.00000   0.00000   1.00000 \r\n"
			+ "    2    1    3   0.00000   1.00000   0.00000 \r\n"
			+ "    2    1    4   0.00000   1.00000   1.00000 \r\n"
			+ "    2    1    5   1.00000   0.00000   0.00000 \r\n"
			+ "    2    1    6   1.00000   0.00000   1.00000 \r\n"
			+ "    2    1    7   1.00000   1.00000   0.00000 \r\n"
			+ "    2    1    8   1.00000   1.00000   1.00000 \r\n"
			+ "    2    1    9   0.00000   0.00000   0.50000 \r\n"
			+ "    2    1   10   0.00000   1.00000   0.50000 \r\n"
			+ "    2    1   11   1.00000   0.00000   0.50000 \r\n"
			+ "    2    1   12   1.00000   1.00000   0.50000 \r\n"
			+ "    2    1   13   0.50000   0.50000   0.00000 \r\n"
			+ "    2    1   14   0.50000   0.50000   1.00000 \r\n"
			+ "    2    1   15   0.50000   0.50000   0.50000 \r\n"
			+ "    3    1    1   0.25000   0.75000   0.00000 \r\n"
			+ "    3    1    2   0.25000   0.75000   1.00000 \r\n"
			+ "    3    1    3   0.25000   0.75000   0.50000 \r\n"
			+ "    3    1    4   0.75000   0.25000   0.00000 \r\n"
			+ "    3    1    5   0.75000   0.25000   1.00000 \r\n"
			+ "    3    1    6   0.75000   0.25000   0.50000 \r\n"
			+ "    3    1    7   0.25000   0.25000   0.00000 \r\n"
			+ "    3    1    8   0.25000   0.25000   1.00000 \r\n"
			+ "    3    1    9   0.25000   0.25000   0.50000 \r\n"
			+ "    3    1   10   0.75000   0.75000   0.00000 \r\n"
			+ "    3    1   11   0.75000   0.75000   1.00000 \r\n"
			+ "    3    1   12   0.75000   0.75000   0.50000 \r\n"
			+ "    3    2    1   0.00000   0.00000   0.25000 \r\n"
			+ "    3    2    2   0.00000   1.00000   0.25000 \r\n"
			+ "    3    2    3   1.00000   0.00000   0.25000 \r\n"
			+ "    3    2    4   1.00000   1.00000   0.25000 \r\n"
			+ "    3    2    5   0.00000   0.00000   0.75000 \r\n"
			+ "    3    2    6   0.00000   1.00000   0.75000 \r\n"
			+ "    3    2    7   1.00000   0.00000   0.75000 \r\n"
			+ "    3    2    8   1.00000   1.00000   0.75000 \r\n"
			+ "    3    2    9   0.50000   0.50000   0.25000 \r\n"
			+ "    3    2   10   0.50000   0.50000   0.75000 \r\n" + "\r\n"
			+ "#occupation_for_each_subatom_(same_order_as_atomcoordlist) \r\n" + "!atomocclist \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n"
			+ "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "   1.00000 \r\n" + "\r\n"
			+ "#magnetic_moment_mx/my/mz_for_each_subatom_(same_order_as_atomcoordlist) \r\n" + "!atommaglist \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "\r\n"
			+ "#rotation_rx/ry/rz_for_each_subatom_(same_order_as_atomcoordlist) \r\n" + "!atomrotlist \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "\r\n"
			+ "#parentatom1/type1/subatom1/parentatom2/type2/subatom2_for_each_bond \r\n" + "!bondlist \r\n"
			+ "    1    1    1      1    1    2 \r\n" + "    1    1    1      2    1    1 \r\n"
			+ "    1    1    1      2    1    2 \r\n" + "    1    1    1      2    1    3 \r\n"
			+ "    1    1    1      2    1    4 \r\n" + "    1    1    1      2    1    5 \r\n"
			+ "    1    1    1      2    1    6 \r\n" + "    1    1    1      2    1    7 \r\n"
			+ "    1    1    1      2    1    8 \r\n" + "    1    1    1      2    1    9 \r\n"
			+ "    1    1    1      2    1   10 \r\n" + "    1    1    1      2    1   11 \r\n"
			+ "    1    1    1      2    1   12 \r\n" + "    1    1    1      2    1   13 \r\n"
			+ "    1    1    1      2    1   14 \r\n" + "    1    1    1      2    1   15 \r\n"
			+ "    1    1    1      3    1    4 \r\n" + "    1    1    1      3    1    5 \r\n"
			+ "    1    1    1      3    1    6 \r\n" + "    1    1    1      3    1    7 \r\n"
			+ "    1    1    1      3    1    8 \r\n" + "    1    1    1      3    1    9 \r\n"
			+ "    1    1    1      3    2    1 \r\n" + "    1    1    1      3    2    2 \r\n"
			+ "    1    1    1      3    2    3 \r\n" + "    1    1    1      3    2    4 \r\n"
			+ "    1    1    1      3    2    9 \r\n" + "    1    1    2      2    1    1 \r\n"
			+ "    1    1    2      2    1    2 \r\n" + "    1    1    2      2    1    3 \r\n"
			+ "    1    1    2      2    1    4 \r\n" + "    1    1    2      2    1    5 \r\n"
			+ "    1    1    2      2    1    6 \r\n" + "    1    1    2      2    1    7 \r\n"
			+ "    1    1    2      2    1    8 \r\n" + "    1    1    2      2    1    9 \r\n"
			+ "    1    1    2      2    1   10 \r\n" + "    1    1    2      2    1   11 \r\n"
			+ "    1    1    2      2    1   12 \r\n" + "    1    1    2      2    1   13 \r\n"
			+ "    1    1    2      2    1   14 \r\n" + "    1    1    2      2    1   15 \r\n"
			+ "    1    1    2      3    1    1 \r\n" + "    1    1    2      3    1    3 \r\n"
			+ "    1    1    2      3    1    4 \r\n" + "    1    1    2      3    1    5 \r\n"
			+ "    1    1    2      3    1    6 \r\n" + "    1    1    2      3    1    7 \r\n"
			+ "    1    1    2      3    1    8 \r\n" + "    1    1    2      3    1    9 \r\n"
			+ "    1    1    2      3    1   10 \r\n" + "    1    1    2      3    1   12 \r\n"
			+ "    1    1    2      3    2    1 \r\n" + "    1    1    2      3    2    2 \r\n"
			+ "    1    1    2      3    2    3 \r\n" + "    1    1    2      3    2    4 \r\n"
			+ "    1    1    2      3    2    9 \r\n" + "    1    1    3      1    1    4 \r\n"
			+ "    1    1    3      2    1    2 \r\n" + "    1    1    3      2    1    6 \r\n"
			+ "    1    1    3      2    1    9 \r\n" + "    1    1    3      2    1   10 \r\n"
			+ "    1    1    3      2    1   11 \r\n" + "    1    1    3      2    1   12 \r\n"
			+ "    1    1    3      2    1   14 \r\n" + "    1    1    3      2    1   15 \r\n"
			+ "    1    1    3      3    1    5 \r\n" + "    1    1    3      3    1    6 \r\n"
			+ "    1    1    3      3    1    8 \r\n" + "    1    1    3      3    1    9 \r\n"
			+ "    1    1    3      3    2    5 \r\n" + "    1    1    3      3    2    6 \r\n"
			+ "    1    1    3      3    2    7 \r\n" + "    1    1    3      3    2    8 \r\n"
			+ "    1    1    3      3    2   10 \r\n" + "    1    1    4      2    1    4 \r\n"
			+ "    1    1    4      2    1    8 \r\n" + "    1    1    4      2    1    9 \r\n"
			+ "    1    1    4      2    1   10 \r\n" + "    1    1    4      2    1   11 \r\n"
			+ "    1    1    4      2    1   12 \r\n" + "    1    1    4      2    1   14 \r\n"
			+ "    1    1    4      2    1   15 \r\n" + "    1    1    4      3    1    2 \r\n"
			+ "    1    1    4      3    1    3 \r\n" + "    1    1    4      3    1    6 \r\n"
			+ "    1    1    4      3    1    9 \r\n" + "    1    1    4      3    1   11 \r\n"
			+ "    1    1    4      3    1   12 \r\n" + "    1    1    4      3    2    5 \r\n"
			+ "    1    1    4      3    2    6 \r\n" + "    1    1    4      3    2    7 \r\n"
			+ "    1    1    4      3    2    8 \r\n" + "    1    1    4      3    2   10 \r\n"
			+ "    1    1    5      1    1    6 \r\n" + "    1    1    5      2    1    1 \r\n"
			+ "    1    1    5      2    1    2 \r\n" + "    1    1    5      2    1    3 \r\n"
			+ "    1    1    5      2    1    4 \r\n" + "    1    1    5      2    1    5 \r\n"
			+ "    1    1    5      2    1    6 \r\n" + "    1    1    5      2    1    7 \r\n"
			+ "    1    1    5      2    1    8 \r\n" + "    1    1    5      2    1    9 \r\n"
			+ "    1    1    5      2    1   10 \r\n" + "    1    1    5      2    1   11 \r\n"
			+ "    1    1    5      2    1   12 \r\n" + "    1    1    5      2    1   13 \r\n"
			+ "    1    1    5      2    1   14 \r\n" + "    1    1    5      2    1   15 \r\n"
			+ "    1    1    5      3    1    1 \r\n" + "    1    1    5      3    1    2 \r\n"
			+ "    1    1    5      3    1    3 \r\n" + "    1    1    5      3    1    7 \r\n"
			+ "    1    1    5      3    1    8 \r\n" + "    1    1    5      3    1    9 \r\n"
			+ "    1    1    5      3    2    1 \r\n" + "    1    1    5      3    2    2 \r\n"
			+ "    1    1    5      3    2    3 \r\n" + "    1    1    5      3    2    4 \r\n"
			+ "    1    1    5      3    2    9 \r\n" + "    1    1    6      2    1    1 \r\n"
			+ "    1    1    6      2    1    2 \r\n" + "    1    1    6      2    1    3 \r\n"
			+ "    1    1    6      2    1    4 \r\n" + "    1    1    6      2    1    5 \r\n"
			+ "    1    1    6      2    1    6 \r\n" + "    1    1    6      2    1    7 \r\n"
			+ "    1    1    6      2    1    8 \r\n" + "    1    1    6      2    1    9 \r\n"
			+ "    1    1    6      2    1   10 \r\n" + "    1    1    6      2    1   11 \r\n"
			+ "    1    1    6      2    1   12 \r\n" + "    1    1    6      2    1   13 \r\n"
			+ "    1    1    6      2    1   14 \r\n" + "    1    1    6      2    1   15 \r\n"
			+ "    1    1    6      3    1    1 \r\n" + "    1    1    6      3    1    2 \r\n"
			+ "    1    1    6      3    1    3 \r\n" + "    1    1    6      3    1    4 \r\n"
			+ "    1    1    6      3    1    6 \r\n" + "    1    1    6      3    1    7 \r\n"
			+ "    1    1    6      3    1    8 \r\n" + "    1    1    6      3    1    9 \r\n"
			+ "    1    1    6      3    1   10 \r\n" + "    1    1    6      3    1   12 \r\n"
			+ "    1    1    6      3    2    1 \r\n" + "    1    1    6      3    2    2 \r\n"
			+ "    1    1    6      3    2    3 \r\n" + "    1    1    6      3    2    4 \r\n"
			+ "    1    1    6      3    2    9 \r\n" + "    1    1    7      1    1    8 \r\n"
			+ "    1    1    7      2    1    2 \r\n" + "    1    1    7      2    1    4 \r\n"
			+ "    1    1    7      2    1    9 \r\n" + "    1    1    7      2    1   10 \r\n"
			+ "    1    1    7      2    1   11 \r\n" + "    1    1    7      2    1   12 \r\n"
			+ "    1    1    7      2    1   14 \r\n" + "    1    1    7      2    1   15 \r\n"
			+ "    1    1    7      3    1    2 \r\n" + "    1    1    7      3    1    3 \r\n"
			+ "    1    1    7      3    1    8 \r\n" + "    1    1    7      3    1    9 \r\n"
			+ "    1    1    7      3    2    5 \r\n" + "    1    1    7      3    2    6 \r\n"
			+ "    1    1    7      3    2    7 \r\n" + "    1    1    7      3    2    8 \r\n"
			+ "    1    1    7      3    2   10 \r\n" + "    1    1    8      2    1    6 \r\n"
			+ "    1    1    8      2    1    8 \r\n" + "    1    1    8      2    1    9 \r\n"
			+ "    1    1    8      2    1   10 \r\n" + "    1    1    8      2    1   11 \r\n"
			+ "    1    1    8      2    1   12 \r\n" + "    1    1    8      2    1   14 \r\n"
			+ "    1    1    8      2    1   15 \r\n" + "    1    1    8      3    1    3 \r\n"
			+ "    1    1    8      3    1    5 \r\n" + "    1    1    8      3    1    6 \r\n"
			+ "    1    1    8      3    1    9 \r\n" + "    1    1    8      3    1   11 \r\n"
			+ "    1    1    8      3    1   12 \r\n" + "    1    1    8      3    2    5 \r\n"
			+ "    1    1    8      3    2    6 \r\n" + "    1    1    8      3    2    7 \r\n"
			+ "    1    1    8      3    2    8 \r\n" + "    1    1    8      3    2   10 \r\n"
			+ "    2    1    1      2    1    2 \r\n" + "    2    1    1      2    1    3 \r\n"
			+ "    2    1    1      2    1    4 \r\n" + "    2    1    1      2    1    5 \r\n"
			+ "    2    1    1      2    1    6 \r\n" + "    2    1    1      2    1    7 \r\n"
			+ "    2    1    1      2    1    8 \r\n" + "    2    1    1      3    1    7 \r\n"
			+ "    2    1    1      3    1    8 \r\n" + "    2    1    1      3    2    1 \r\n"
			+ "    2    1    1      3    2    2 \r\n" + "    2    1    1      3    2    3 \r\n"
			+ "    2    1    1      3    2    4 \r\n" + "    2    1    2      2    1    3 \r\n"
			+ "    2    1    2      2    1    4 \r\n" + "    2    1    2      2    1    5 \r\n"
			+ "    2    1    2      2    1    6 \r\n" + "    2    1    2      2    1    7 \r\n"
			+ "    2    1    2      2    1    8 \r\n" + "    2    1    2      3    1    7 \r\n"
			+ "    2    1    2      3    1    8 \r\n" + "    2    1    2      3    2    1 \r\n"
			+ "    2    1    2      3    2    2 \r\n" + "    2    1    2      3    2    3 \r\n"
			+ "    2    1    2      3    2    4 \r\n" + "    2    1    2      3    2    5 \r\n"
			+ "    2    1    3      2    1    4 \r\n" + "    2    1    3      2    1    5 \r\n"
			+ "    2    1    3      2    1    6 \r\n" + "    2    1    3      2    1    7 \r\n"
			+ "    2    1    3      2    1    8 \r\n" + "    2    1    3      3    1    1 \r\n"
			+ "    2    1    3      3    1    7 \r\n" + "    2    1    3      3    1    8 \r\n"
			+ "    2    1    3      3    2    1 \r\n" + "    2    1    3      3    2    2 \r\n"
			+ "    2    1    3      3    2    3 \r\n" + "    2    1    3      3    2    4 \r\n"
			+ "    2    1    4      2    1    5 \r\n" + "    2    1    4      2    1    6 \r\n"
			+ "    2    1    4      2    1    7 \r\n" + "    2    1    4      2    1    8 \r\n"
			+ "    2    1    4      3    1    2 \r\n" + "    2    1    4      3    1    7 \r\n"
			+ "    2    1    4      3    1    8 \r\n" + "    2    1    4      3    2    1 \r\n"
			+ "    2    1    4      3    2    2 \r\n" + "    2    1    4      3    2    3 \r\n"
			+ "    2    1    4      3    2    4 \r\n" + "    2    1    4      3    2    6 \r\n"
			+ "    2    1    5      2    1    6 \r\n" + "    2    1    5      2    1    7 \r\n"
			+ "    2    1    5      2    1    8 \r\n" + "    2    1    5      3    1    4 \r\n"
			+ "    2    1    5      3    1    7 \r\n" + "    2    1    5      3    1    8 \r\n"
			+ "    2    1    5      3    2    1 \r\n" + "    2    1    5      3    2    2 \r\n"
			+ "    2    1    5      3    2    3 \r\n" + "    2    1    5      3    2    4 \r\n"
			+ "    2    1    6      2    1    7 \r\n" + "    2    1    6      2    1    8 \r\n"
			+ "    2    1    6      3    1    5 \r\n" + "    2    1    6      3    1    7 \r\n"
			+ "    2    1    6      3    1    8 \r\n" + "    2    1    6      3    2    1 \r\n"
			+ "    2    1    6      3    2    2 \r\n" + "    2    1    6      3    2    3 \r\n"
			+ "    2    1    6      3    2    4 \r\n" + "    2    1    6      3    2    7 \r\n"
			+ "    2    1    7      2    1    8 \r\n" + "    2    1    7      3    1    7 \r\n"
			+ "    2    1    7      3    1    8 \r\n" + "    2    1    7      3    1   10 \r\n"
			+ "    2    1    7      3    2    1 \r\n" + "    2    1    7      3    2    2 \r\n"
			+ "    2    1    7      3    2    3 \r\n" + "    2    1    7      3    2    4 \r\n"
			+ "    2    1    8      3    1    7 \r\n" + "    2    1    8      3    1    8 \r\n"
			+ "    2    1    8      3    1   11 \r\n" + "    2    1    8      3    2    1 \r\n"
			+ "    2    1    8      3    2    2 \r\n" + "    2    1    8      3    2    3 \r\n"
			+ "    2    1    8      3    2    4 \r\n" + "    2    1    8      3    2    8 \r\n"
			+ "    2    1    9      2    1   10 \r\n" + "    2    1    9      2    1   11 \r\n"
			+ "    2    1    9      2    1   12 \r\n" + "    2    1    9      3    1    9 \r\n"
			+ "    2    1    9      3    2    1 \r\n" + "    2    1    9      3    2    2 \r\n"
			+ "    2    1    9      3    2    3 \r\n" + "    2    1    9      3    2    4 \r\n"
			+ "    2    1    9      3    2    5 \r\n" + "    2    1    9      3    2    6 \r\n"
			+ "    2    1    9      3    2    7 \r\n" + "    2    1    9      3    2    8 \r\n"
			+ "    2    1   10      2    1   11 \r\n" + "    2    1   10      2    1   12 \r\n"
			+ "    2    1   10      3    1    3 \r\n" + "    2    1   10      3    1    9 \r\n"
			+ "    2    1   10      3    2    1 \r\n" + "    2    1   10      3    2    2 \r\n"
			+ "    2    1   10      3    2    3 \r\n" + "    2    1   10      3    2    4 \r\n"
			+ "    2    1   10      3    2    5 \r\n" + "    2    1   10      3    2    6 \r\n"
			+ "    2    1   10      3    2    7 \r\n" + "    2    1   10      3    2    8 \r\n"
			+ "    2    1   11      2    1   12 \r\n" + "    2    1   11      3    1    6 \r\n"
			+ "    2    1   11      3    1    9 \r\n" + "    2    1   11      3    2    1 \r\n"
			+ "    2    1   11      3    2    2 \r\n" + "    2    1   11      3    2    3 \r\n"
			+ "    2    1   11      3    2    4 \r\n" + "    2    1   11      3    2    5 \r\n"
			+ "    2    1   11      3    2    6 \r\n" + "    2    1   11      3    2    7 \r\n"
			+ "    2    1   11      3    2    8 \r\n" + "    2    1   12      3    1    9 \r\n"
			+ "    2    1   12      3    1   12 \r\n" + "    2    1   12      3    2    1 \r\n"
			+ "    2    1   12      3    2    2 \r\n" + "    2    1   12      3    2    3 \r\n"
			+ "    2    1   12      3    2    4 \r\n" + "    2    1   12      3    2    5 \r\n"
			+ "    2    1   12      3    2    6 \r\n" + "    2    1   12      3    2    7 \r\n"
			+ "    2    1   12      3    2    8 \r\n" + "    2    1   13      2    1   14 \r\n"
			+ "    2    1   13      3    1    1 \r\n" + "    2    1   13      3    1    2 \r\n"
			+ "    2    1   13      3    1    4 \r\n" + "    2    1   13      3    1    5 \r\n"
			+ "    2    1   13      3    1    7 \r\n" + "    2    1   13      3    1    8 \r\n"
			+ "    2    1   13      3    1   10 \r\n" + "    2    1   13      3    1   11 \r\n"
			+ "    2    1   13      3    2    9 \r\n" + "    2    1   14      3    1    1 \r\n"
			+ "    2    1   14      3    1    2 \r\n" + "    2    1   14      3    1    4 \r\n"
			+ "    2    1   14      3    1    5 \r\n" + "    2    1   14      3    1    7 \r\n"
			+ "    2    1   14      3    1    8 \r\n" + "    2    1   14      3    1   10 \r\n"
			+ "    2    1   14      3    1   11 \r\n" + "    2    1   14      3    2    9 \r\n"
			+ "    2    1   14      3    2   10 \r\n" + "    2    1   15      3    1    3 \r\n"
			+ "    2    1   15      3    1    6 \r\n" + "    2    1   15      3    1    9 \r\n"
			+ "    2    1   15      3    1   12 \r\n" + "    2    1   15      3    2    9 \r\n"
			+ "    2    1   15      3    2   10 \r\n" + "    3    1    1      3    1    2 \r\n"
			+ "    3    1    1      3    1    7 \r\n" + "    3    1    1      3    1    8 \r\n"
			+ "    3    1    1      3    1   10 \r\n" + "    3    1    1      3    1   11 \r\n"
			+ "    3    1    1      3    2    2 \r\n" + "    3    1    1      3    2    9 \r\n"
			+ "    3    1    2      3    1    7 \r\n" + "    3    1    2      3    1    8 \r\n"
			+ "    3    1    2      3    1   10 \r\n" + "    3    1    2      3    1   11 \r\n"
			+ "    3    1    2      3    2    6 \r\n" + "    3    1    2      3    2    9 \r\n"
			+ "    3    1    2      3    2   10 \r\n" + "    3    1    3      3    1    9 \r\n"
			+ "    3    1    3      3    1   12 \r\n" + "    3    1    3      3    2    2 \r\n"
			+ "    3    1    3      3    2    6 \r\n" + "    3    1    3      3    2    9 \r\n"
			+ "    3    1    3      3    2   10 \r\n" + "    3    1    4      3    1    5 \r\n"
			+ "    3    1    4      3    1    7 \r\n" + "    3    1    4      3    1    8 \r\n"
			+ "    3    1    4      3    1   10 \r\n" + "    3    1    4      3    1   11 \r\n"
			+ "    3    1    4      3    2    3 \r\n" + "    3    1    4      3    2    9 \r\n"
			+ "    3    1    5      3    1    7 \r\n" + "    3    1    5      3    1    8 \r\n"
			+ "    3    1    5      3    1   10 \r\n" + "    3    1    5      3    1   11 \r\n"
			+ "    3    1    5      3    2    7 \r\n" + "    3    1    5      3    2    9 \r\n"
			+ "    3    1    5      3    2   10 \r\n" + "    3    1    6      3    1    9 \r\n"
			+ "    3    1    6      3    1   12 \r\n" + "    3    1    6      3    2    3 \r\n"
			+ "    3    1    6      3    2    7 \r\n" + "    3    1    6      3    2    9 \r\n"
			+ "    3    1    6      3    2   10 \r\n" + "    3    1    7      3    1    8 \r\n"
			+ "    3    1    7      3    2    1 \r\n" + "    3    1    7      3    2    2 \r\n"
			+ "    3    1    7      3    2    3 \r\n" + "    3    1    7      3    2    4 \r\n"
			+ "    3    1    7      3    2    9 \r\n" + "    3    1    8      3    2    1 \r\n"
			+ "    3    1    8      3    2    2 \r\n" + "    3    1    8      3    2    3 \r\n"
			+ "    3    1    8      3    2    4 \r\n" + "    3    1    8      3    2    5 \r\n"
			+ "    3    1    8      3    2    9 \r\n" + "    3    1    8      3    2   10 \r\n"
			+ "    3    1    9      3    2    1 \r\n" + "    3    1    9      3    2    2 \r\n"
			+ "    3    1    9      3    2    3 \r\n" + "    3    1    9      3    2    4 \r\n"
			+ "    3    1    9      3    2    5 \r\n" + "    3    1    9      3    2    6 \r\n"
			+ "    3    1    9      3    2    7 \r\n" + "    3    1    9      3    2    8 \r\n"
			+ "    3    1    9      3    2    9 \r\n" + "    3    1    9      3    2   10 \r\n"
			+ "    3    1   10      3    1   11 \r\n" + "    3    1   10      3    2    4 \r\n"
			+ "    3    1   10      3    2    9 \r\n" + "    3    1   11      3    2    8 \r\n"
			+ "    3    1   11      3    2    9 \r\n" + "    3    1   11      3    2   10 \r\n"
			+ "    3    1   12      3    2    4 \r\n" + "    3    1   12      3    2    8 \r\n"
			+ "    3    1   12      3    2    9 \r\n" + "    3    1   12      3    2   10 \r\n"
			+ "    3    2    1      3    2    2 \r\n" + "    3    2    1      3    2    3 \r\n"
			+ "    3    2    1      3    2    4 \r\n" + "    3    2    2      3    2    3 \r\n"
			+ "    3    2    2      3    2    4 \r\n" + "    3    2    3      3    2    4 \r\n"
			+ "    3    2    5      3    2    6 \r\n" + "    3    2    5      3    2    7 \r\n"
			+ "    3    2    5      3    2    8 \r\n" + "    3    2    6      3    2    7 \r\n"
			+ "    3    2    6      3    2    8 \r\n" + "    3    2    7      3    2    8 \r\n"
			+ "#irrepnum/irreplabel_for_each_contributing_irrep \r\n" + "!irreplist \r\n" + "  1 R4+      \r\n"
			+ "  2 GM1+     \r\n" + "  3 GM3+     \r\n" + "\r\n"
			+ "#strainmodenum/amp/maxamp/irrepnum/modelabel/modevector_for_each_mode \r\n" + "!strainmodelist \r\n"
			+ "  1    0.00000   0.10000    2 GM1+strain(a) \r\n"
			+ "   0.57735   0.57735   0.57735   0.00000   0.00000   0.00000 \r\n"
			+ "  2    0.00000   0.10000    3 GM3+strain(a) \r\n"
			+ "  -0.40825  -0.40825   0.81650  -0.00000  -0.00000  -0.00000 \r\n" + "\r\n"
			+ "#parentatom/dispmodenum/amp/maxamp/irrepnum/modelabel/(modevector_for_each_subatom)_for_each_mode \r\n"
			+ "!displacivemodelist \r\n" + "    3    1   0.00000   2.00000    1 R4+[O:d:dsp]Eu(a) \r\n"
			+ "   0.05952   0.05952   0.00000 \r\n" + "   0.05952   0.05952   0.00000 \r\n"
			+ "  -0.05952  -0.05952   0.00000 \r\n" + "  -0.05952  -0.05952   0.00000 \r\n"
			+ "  -0.05952  -0.05952   0.00000 \r\n" + "   0.05952   0.05952   0.00000 \r\n"
			+ "  -0.05952   0.05952   0.00000 \r\n" + "  -0.05952   0.05952   0.00000 \r\n"
			+ "   0.05952  -0.05952   0.00000 \r\n" + "   0.05952  -0.05952   0.00000 \r\n"
			+ "   0.05952  -0.05952   0.00000 \r\n" + "  -0.05952   0.05952   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n"
			+ "   0.00000   0.00000   0.00000 \r\n" + "   0.00000   0.00000   0.00000 \r\n" + "\r\n" + "";

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

	public void updateViewOptions() {
		// TODO Auto-generated method stub
		
	}

	public void saveCurrent() {
		if (formData == null)
			formData = ServerUtil.testFormData;
		Object mapFormData = ServerUtil.json2Map(formData);
		variables.updateFormData(mapFormData);
		
		getData(new Consumer<Object>() {

			@Override
			public void accept(Object data) {
				FileUtil.saveDataFile(frame, data, "isoviz", false);
				
			}
			
		});
	}

	public void saveOriginal() {
		getData(new Consumer<Object>() {

			@Override
			public void accept(Object data) {
				FileUtil.saveDataFile(frame, data, "isoviz", false);
				
			}
			
		});
	}

	public void saveDistortion() {
		// TODO Auto-generated method stub
		
	}

	public void saveCIF() {
		// TODO Auto-generated method stub
		
	}

	public void viewPrimaryOrderParameters() {
		// TODO Auto-generated method stub
		
	}

	public void viewModeDetails() {
		// TODO Auto-generated method stub
		
	}

	public void viewCompleteModeDetails() {
		// TODO Auto-generated method stub
		
	}

	public void saveTOPASstr() {
		// TODO Auto-generated method stub
		
	}

	public void saveFULLPROFcpr() {
		// TODO Auto-generated method stub
		
	}

	public void saveIRMatrices() {
		// TODO Auto-generated method stub
		
	}

	public void viewSubgroupTree() {
		// TODO Auto-generated method stub
		
	}

	
}
