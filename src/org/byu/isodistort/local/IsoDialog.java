package org.byu.isodistort.local;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

/**
 * The IsoDialog sets up asynchronous modal dialogs that offer options for
 * saving files, viewing ISODISTORT web pages, and setting preferences.
 * 
 * Each dialog is a singleton instance of its class so that choices made are
 * returned to later if viewed again.
 * 
 * In all cases, the goal is to replace form data input values that will be
 * passed to isodistortform.php.
 * 
 * The layout of the dialog is set by a simple coding of a String array. Strings
 * are sequential keys / value sets. Keys are of two types -- data and non-data.
 * 
 * Data Keys
 * 
 * Data keys are of the form "x...", where x is a single character specifying
 * the type of component used:
 * 
 * c... JCheckBox
 * 
 * r... JRadioButton; next two elements are value and text
 * 
 * b... JComboBox; next element is comma-separated list of allowed values
 * 
 * i... JTextField Integer
 * 
 * d... JTextField Double
 * 
 * f... JTextField a/b fraction or Integer
 * 
 * F... JTextField a/b fraction or Double
 * 
 * The rest of the key is the name of the data element in the form data.
 * 
 * For example, the first element of the CIF basis matrix is "fbasist11".
 * 
 * Non-Data Keys
 * 
 * Keys also include the following four non-data strings, along with their
 * associated values. They are recognized by their first character: 'l', 'g', or
 * 'e'.
 * 
 * "label", [text]
 * 
 * If starting with "\n" then, this starts a new JPanel; starting with "\n<b>"
 * starts a new Jpanel with bold, slightly larger text.
 * 
 * "grid", [#rows], [#cols]
 * 
 * Indicates a GridLayout panel. The next (#rows)x(#cols) elements go in this
 * grid
 * 
 * "enable" and "endenable"
 * 
 * These surround a JPanel that will be visualized based on a the state of a
 * checkbox, which should be immediately prior to "enable".
 * 
 * It's a little funky, but actually very easy to adapt to future needs, I
 * think, and much clearer than a huge block of Swing library calls.
 * 
 * @author Bob Hanson
 *
 */
public abstract class IsoDialog extends JDialog {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private static CIFDialog cifDialog;
	private static DistortionDialog distortionDialog;
	private static FormDialog formDialog;
	private static FULLPROFDialog fullProfDialog;
	private static PrefsDialog prefsDialog;
	private static TOPASDialog topasDialog;
	private static TreeDialog treeDialog;
	private static ISOVIZDialog isovizDialog;

	protected IsoApp app;

	protected Map<String, Object> formData;

	private String[] args;

	public static void openIsovizDialog(IsoApp app, Map<String, Object> formData) {
		ISOVIZDialog.openDialog(app, formData);
	}

	private static class ISOVIZDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			// we do not save the old values, because 
			isovizDialog = new ISOVIZDialog(app, formData);
			isovizDialog.setApp(app, () -> {
				app.saveIsoviz(isovizDialog.getValues());
			});
			isovizDialog.setVisible(true);
		}

		ISOVIZDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, isoviz, "Save ISOVIZ file", true);
			init();
		}
		
		@Override
		protected void init() {
			String options = "" + formData.get("slidersetting");
			boolean isOriginal = (options.indexOf("original") >= 0);
			boolean isNew = (options.indexOf("new") >= 0);
			formData.put("slidersetting",(isNew ? "current" : "original"));
			super.init();
			for (int i = 0, n = this.items.size(); i < n; i++) {
				JComponent c = items.get(i);
				if (c instanceof JLabel) {
					String text = ((JLabel) c).getText();
					switch (text) {
					case "TEXT":
						((JLabel) c).setText(!isNew ? "Only the original ISOVIZ file can be saved." : "");
						continue;
					default:
						if (text.equals("original")) {
							c.setVisible(isNew && isOriginal);
						} else {
							c.setVisible(isNew);
						}
						break;
					}
				} else if (c instanceof JRadioButton) {
					if (c.getName().equals("original")) {
						c.setVisible(isNew && isOriginal);
					} else {
						c.setVisible(isNew);
					}
				}
			}

		}
		
		private static String[] isoviz = { //
				"label", "\nTEXT",//
				"label", "\nCoordinates:", //
				"rslidersetting", "original", "original", //
				"rslidersetting", "current", "current", //
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //
		};

	}

	public static void openCIFDialog(IsoApp app, Map<String, Object> formData) {
		CIFDialog.openDialog(app, formData);
	}

	private static class CIFDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			if (cifDialog == null) {
				cifDialog = new CIFDialog(app, formData);
			}
			cifDialog.setApp(app, () -> {
				app.saveCIF(cifDialog.getValues());
			});
			cifDialog.setVisible(true);
		}

		CIFDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, cif, "Save CIF", true);
			init();
		}

		private static String[] cif = { //
				"label", "\nCoordinates:", //
				"rslidersetting", "current", "current", //
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //

				"label", "\nNumber of decimal places in CIF file:", //
				"bcifdec", " 5, 6, 7, 8, 9,10,11,12,13,14,15,16", //

				"label", "\n", "cnonstandardsetting", //
				"enable", //e1, //
				"label", "Use alternate (possibly nonstandard) setting in CIF output (matrix S\u207b\u00b9)", //
				"label", "\n             with respect to:", //
				"rsettingwrt", "parent", "parent", //
				"rsettingwrt", "subgroup", "subgroup", //
				"label", "\n      Basis vectors of subgroup lattice (rational numbers):", //
				"grid", "3", "8", //

				"label", " ", //
				"label", "  a'    =", //
				"fbasist11", //
				"label", "  a     +", //
				"fbasist12", //
				"label", "  b     +", //
				"fbasist13", //
				"label", "  c", //

				"label", " ", //
				"label", "  b'    =", //
				"fbasist21", //
				"label", "  a     +", //
				"fbasist22", //
				"label", "  b     +", //
				"fbasist23", //
				"label", "  c", //

				"label", " ", //
				"label", "  c'    =", //
				"fbasist31", //
				"label", "  a     +", //
				"fbasist32", //
				"label", "  b     +", //
				"fbasist33", //
				"label", "  c", //

				"label", "\n      Origin of subgroup (either rational or decimal numbers):", //
				"grid", "1", "8", //
				"label", " ", "Forigint1", //
				"label", "  a     +", //
				"Forigint2", //
				"label", "  b     +", //
				"Forigint3", //
				"label", "  c", //
				// end of grid
				"endenable", //e1, //
				"label", "\n", //
				"ccifmovie", //
				"label", "Make CIF movie", //
				"enable", //e1,  //
				"grid", "5", "2", //
				"label", "        minimum amplitude:", //
				"dampmincifmovie", //
				"label", "        maximum amplitude:", //
				"dampmaxcifmovie", //
				"label", "        number of frames:", //
				"inframescifmovie", //
				"label", "        fractional # of periods:", //
				"fperiodscifmovie", //
				"label", "        amplitude variation:", //
				"rvarcifmovie", "linear", "linear", //
				"rvarcifmovie", "sine", "sine-wave", //
				"endenable", //e1, //
			};

	}

	public static void openDistortionDialog(IsoApp app, Map<String, Object> formData) {
		DistortionDialog.openDisDialog(app, formData);
	}

	private static class DistortionDialog extends FormDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDisDialog(IsoApp app, Map<String, Object> formData) {
			if (distortionDialog == null) {
				distortionDialog = new DistortionDialog(app, formData);
			}
			distortionDialog.setApp(app, () -> {
				app.saveDistortionFile(distortionDialog.getValues());
			});
			distortionDialog.setVisible(true);
		}

		DistortionDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, null, "Save DISTORTION txt");
		}

	}

	public static void openFULLPROFDialog(IsoApp app, Map<String, Object> formData) {
		FULLPROFDialog.openFPDialog(app, formData);
	}

	public static void openFormDialog(IsoApp app, Map<String, Object> formData) {
		FormDialog.openDialog(app, formData);
	}

	private static class FormDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			if (formDialog == null) {
				formDialog = new FormDialog(app, formData);
			}
			formDialog.setApp(app, () -> {
				app.saveFormData(formDialog.getValues());
			});
			formDialog.setVisible(true);
		}

		FormDialog(IsoApp app, Map<String, Object> formData) {
			this(app, formData, generic, "Save Form Data");
		}

		FormDialog(IsoApp app, Map<String, Object> formData, String[] args, String title) {
			super(app, formData, (args == null ? generic : args), title, true);
			init();
		}

		private static String[] generic = { 
				"label", "\nCoordinates:", //
				"rslidersetting", "current", "current", //
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //
		};

	}

	private static class FULLPROFDialog extends FormDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openFPDialog(IsoApp app, Map<String, Object> formData) {
			if (fullProfDialog == null) {
				fullProfDialog = new FULLPROFDialog(app, formData);
			}
			fullProfDialog.setApp(app, () -> {
				app.saveFULLPROF(fullProfDialog.getValues());
			});
			fullProfDialog.setVisible(true);
		}

		FULLPROFDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, null, "Save FullProf.pcr");
		}

	}

	public static void openPreferencesDialog(IsoApp app, Map<String, Object> formData) {
		PrefsDialog.openDialog(app, formData);
	}

	private static class PrefsDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			if (prefsDialog == null) {
				prefsDialog = new PrefsDialog(app, formData);
			}
			prefsDialog.setApp(app, () -> {
				app.setPreferences(prefsDialog.getValues());
			});
			prefsDialog.setVisible(true);
		}

		PrefsDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, prefs, "Save Preferences", true);
			init();
		}

		private static String[] prefs = { //
				"label", "\n<b>Local ISODISTORT values", //
				"label", "\nNominal atom radius (Angstroms)", //
				"datomicradius", //
				"label", "\nMaxium bond length (Angstroms)", //
				"dbondlength", //
				"label", "\n<b>Server ISODISTORT values", //
				"label", "\nViewing range:", //
				"label", "\nxmin", "dsupercellxmin", "label", "xmax", "dsupercellxmax", //
				"label", "\nymin", "dsupercellymin", "label", "ymax", "dsupercellymax", //
				"label", "\nzmin", "dsupercellzmin", "label", "zmax", "dsupercellzmax", //
				"label", "\n<b>Server ISODISTORT/ISODIFFRACT values", //
				"label", "\nMaximum displacement per mode:", "dmodeamplitude", "label", "Angstroms", //
				"label", "\nMaximum strain per mode:", "dstrainamplitude", //
		};

	}

	public static void openSubgroupTreeDialog(IsoApp app, Map<String, Object> formData) {
		TreeDialog.openDialog(app, formData);
	}

	private static class TreeDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			if (treeDialog == null) {
				treeDialog = new TreeDialog(app, formData);
			}
			treeDialog.setApp(app, () -> {
				app.viewSubgroupTree(treeDialog.getValues());
			});
			treeDialog.setVisible(true);
		}

		TreeDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, tree, "Open Subgroup Tree", true);
			init();
		}

		private static String[] tree = { //
				"label", "\nCoordinates:", //
				"rslidersetting", "current", "current", //
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //
				"label", "\n", //
				"ctreetopas", "label", "Generate TOPAS.STR output for subgroup tree", //
				"enable", //e1, //
				"label", "\nRemember to add the appropriate peak profile ",
				"label", "\nwhen adding this to a TOPAS input file", //
				"label", "\n", "ctopasstrain", "label", "Include strain modes in TOPAS.STR", //
				"endenable", //e1, //
				"label", "\n",//
				"ctreecif", "label", "Generate CIF output for subgroup tree", //
		};

	}

	public static void openTOPASDialog(IsoApp app, Map<String, Object> formData) {
		TOPASDialog.openDialog(app, formData);
	}

	private static class TOPASDialog extends IsoDialog {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		static void openDialog(IsoApp app, Map<String, Object> formData) {
			if (topasDialog == null) {
				topasDialog = new TOPASDialog(app, formData);
			}
			topasDialog.setApp(app, () -> {
				app.saveTOPAS(topasDialog.getValues());
			});
			topasDialog.setVisible(true);
		}

		TOPASDialog(IsoApp app, Map<String, Object> formData) {
			super(app, formData, topas, "Save TOPAS.str", true);
			init();
		}

		private static String[] topas = { //
				"label", "\nCoordinates:", //
				"rslidersetting", "current", "current", //
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //

				"label", "\nRemember to add the appropriate peak shape line ",
				"label", "\nwhen passing this into an input file", //
				"label", "\n", "ctopasstrain", "label", "Include strain modes in TOPAS.STR", //
		};

	}

	protected Runnable callback;

	protected List<JComponent> items = new ArrayList<JComponent>();
	
	IsoDialog(IsoApp app, Map<String, Object> formData, String[] args, String title, boolean isModal) {
		super(app.frame, title);
		this.app = app;
		this.formData = formData;
		this.args = args;
		setModal(isModal);
	}

	protected void init() {
		createGUI();
		pack();
		setLocation(100, 100);
//		setLocationRelativeTo(app.frame);
	}

	protected void createGUI() {
		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		addCenterPanel(args);
		addLowerPanel();
	}

	private void addCenterPanel(String[] page) {
		ButtonGroup group = null;
		String groupName = null;
		Object radioValue = null;
		Map<String, Object> formData = this.formData;
		JPanel p = null, rp = null;
		JCheckBox cb = null;
		Stack<JComponent> estack = new Stack<>();
		JComponent box = (JComponent) getContentPane();
		boolean cbEnabled = true;
		for (int i = 0; i < page.length; i++) {
			String item = page[i];
			String key = (item == "label" ? null : item.substring(1));
			Object v = (key == null ? null : formData.get(key));
			switch (item.charAt(0)) {
			case 'g':
				// "grid" rows cols
				int rows = Integer.parseInt(page[++i]);
				int cols = Integer.parseInt(page[++i]);
				JPanel pad = new JPanel();
				pad.setLayout(new BoxLayout(pad, BoxLayout.PAGE_AXIS));
				p = new JPanel(new GridLayout(rows, cols, 0, 0));
				pad.add(p);
				box.add(pad);
				break;
			case 'e':
				if (item.equals("enable")) {
					JComponent savedBox = box;
					estack.push(box);
					box = new Box(BoxLayout.PAGE_AXIS);
					box.setVisible(cbEnabled);
					savedBox.add(box);
					JCheckBox c = cb;
					JComponent thisBox = box;
					cb.addActionListener(new ActionListener() {

						@Override
						public void actionPerformed(ActionEvent e) {
							thisBox.setVisible(c.isSelected());
							pack();
						}

					});
					cb = null;
				} else {
					box.add(new JLabel());
					box = estack.pop();
				}
				break;
			case 'l':
				// "label" "text"
				String s = page[++i];
				if (s.charAt(0) == '\n') {
					s = s.substring(1);
					p = new JPanel(new FlowLayout(FlowLayout.LEFT));
					box.add(p);
				}
				if (s.length() > 0) {
					boolean isBold = s.startsWith("<b>");
					if (isBold)
						s = s.substring(3);
					JLabel l = new JLabel(s);
					if (isBold) {
						l.setFont(new Font(l.getFont().getFamily(), Font.BOLD, (int) (l.getFont().getSize() * 1.2)));
					}
					p.add(l);
					items.add(l);
				}
				break;
			case 'r':
				// "rxxx" value text
				if (!key.equals(groupName)) {
					group = new ButtonGroup();
					groupName = key;
					radioValue = null;
					JPanel pad1 = new JPanel();
					pad1.setLayout(new BoxLayout(pad1, BoxLayout.PAGE_AXIS));
					rp = new JPanel(new FlowLayout(FlowLayout.LEFT));
					pad1.add(rp);
					p.add(pad1);
				}
				String val = page[++i];
				String rtext = page[++i];
				JRadioButton r = new JRadioButton();
				r.setSize(20, 20);
				group.add(r);
				r.setName(val);
				items.add(r);
				objects.add(item);
				objects.add(r);
				if (radioValue != null) {
					// ignore setting
					v = radioValue;
				} else if (v == null) {
					v = val;
				}
				if (radioValue == null)
					radioValue = v;
				r.setSelected(val.equals(radioValue));
				rp.add(r);
				JLabel rl = new JLabel(rtext);
				rp.add(rl);
				items.add(rl);
				break;
			case 'c':
				// "cxxxx"
				cb = new JCheckBox("");
				cbEnabled = "true".equals(v);
				cb.setSelected(cbEnabled);
				cb.setSize(20, 20);
				p.add(cb);
				cb.setName(key);
				objects.add(item);
				objects.add(cb);
				items.add(cb);
				break;
			case 'b':
				// "bXXX", "5,6,7,8,9,10,11,12,13,14,15,16",//
				JComboBox<String> jc = new JComboBox<>(page[++i].split(","));
				if (v != null)
					jc.setSelectedItem(v);
				p.add(jc);
				objects.add(item);
				objects.add(jc);
				break;
			case 'i':
			case 'f':
			case 'F':
			case 'd':
				JPanel pad2 = new JPanel(new FlowLayout(FlowLayout.LEFT));
				JTextField t = new JTextField();
				t.setHorizontalAlignment(SwingConstants.RIGHT);
				t.setPreferredSize(new Dimension(50, 20));
				t.setMaximumSize(new Dimension(50, 20));
				t.setMinimumSize(new Dimension(50, 20));
				t.setSize(new Dimension(30, 20));
				if (v != null) {
					t.setText(v.toString());
				}
				pad2.add(t);
				p.add(pad2);
				objects.add(item);
				objects.add(t);
				items.add(t);
				break;
			}
		}
	}

	private void addLowerPanel() {
		JPanel p = new JPanel();
		p.setLayout(new FlowLayout());
//        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.setPreferredSize(new Dimension(500, 40));
        p.setMaximumSize(new Dimension(500, 40));
        p.setBorder(new EmptyBorder(2,0,0,0));
		p.setBackground(Color.DARK_GRAY);
		JButton b = new JButton("OK");
		b.setAlignmentY(CENTER_ALIGNMENT);
		// need both of these
//		b.setPreferredSize(new Dimension(80, 20));
		b.setMaximumSize(new Dimension(80, 20));
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}

		});
		p.add(b);
		b = new JButton("Cancel");
//		b.setPreferredSize(new Dimension(80, 20));
		b.setMaximumSize(new Dimension(80, 20));
		b.setAlignmentY(CENTER_ALIGNMENT);
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancelAction();
			}

		});
		p.add(b);
		add(p);
	}

	protected void okAction() {
		if (getValues() == null) {
			return;
		}
		setVisible(false);
		callback.run();
	}

	protected void cancelAction() {
		setVisible(false);
	}

	void setApp(IsoApp app, Runnable okCallback) {
		this.app = app;
		this.values = null;
		this.callback = okCallback;
	}

	protected List<Object> objects = new ArrayList<>();
	private Map<String, Object> values;

	Map<String, Object> getValues() throws RuntimeException {
		if (this.values != null)
			return this.values;
		Map<String, Object> map = new HashMap<>();
		for (int i = 0, n = objects.size(); i < n; i++) {
			String key = (String) objects.get(i);
			JComponent c = (JComponent) objects.get(++i);
			Object value = null;
			char ch = key.charAt(0);
			switch (ch) {
			case 'e':
			case 'l':
			case 'g':
				break;
			case 'r':
				if (((JRadioButton) c).isSelected()) {
					value = c.getName();
				}
				break;
			case 'c':
				value = "" + ((JCheckBox) c).isSelected();
				break;
			case 's':
				value = ((JComboBox<?>) c).getSelectedItem();
				break;
			case 'i':
			case 'f':
			case 'F':
			case 'd':
				try {
					switch (ch) {
					case 'i':
						value = validateInt(((JTextField) c).getText());
						break;
					case 'f':
						value = validateFractionalOrInt(((JTextField) c).getText());
						break;
					case 'F':
						value = validateFractionalOrDouble(((JTextField) c).getText());
						break;
					case 'd':
						value = validateDouble(((JTextField) c).getText());
						break;
					}
				} catch (Exception e) {
					c.setBackground(new Color(0xFFA0A0));
					JOptionPane.showMessageDialog(this, "Error on input: " + e);
					return null;
				}
				c.setBackground(Color.white);
				break;
			}
			if (value != null)
				map.put(key.substring(1), value);
		}
		return values = map;
	}

	Object validateInt(String text) throws RuntimeException  {
		try {
			return Integer.parseInt(text.toString().trim());
		} catch (Exception e) {
			throw new RuntimeException(text + " must be an integer");
		}
	}

	Object validateDouble(String text) throws RuntimeException {
		try {
			return Double.parseDouble(text.toString().trim());
		} catch (Exception e) {
			throw new RuntimeException(text + " must be an decimal number");
		}
	}

	Object validateFractionalOrDouble(String s) throws RuntimeException  {
		int pt = s.indexOf("/");
		if (pt > 1) {
			return validateFractionalOrInt(s);
		}
		return validateDouble(s);
	}

	Object validateFractionalOrInt(String s) throws RuntimeException  {
		int pt = s.indexOf("/");
		if (pt > 1) {
			return validateInt(s.substring(0, pt)) + "/" + validateInt(s.substring(0, pt + 1));
		}
		return validateInt(s);
	}

}