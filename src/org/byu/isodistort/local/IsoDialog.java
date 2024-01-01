package org.byu.isodistort.local;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public abstract class IsoDialog extends JDialog {

	static CIFDialog cifDialog;
	
	protected IsoApp app;

	private Map<String, Object> formData;

	//private Map<String, JTextField> textFields;
	
	protected abstract void createGUI();

	public static void openCIFDialog(IsoApp app, Map<String, Object> formData) {
		if (cifDialog == null || true) {
			cifDialog = new CIFDialog(app, formData, () -> {
				app.saveCIF(cifDialog.getValues());				
			});
		}
		cifDialog.setApp(app);
		cifDialog.setVisible(true);
	}

	private static class CIFDialog extends IsoDialog {

		CIFDialog(IsoApp app, Map<String, Object> formData, Runnable okCallback) {
			super(app, formData, "Save CIF", true, okCallback);
			init();
		}
		

		String[] cif = {
				"label", "\nCoordinates:", //
				"rslidersetting", "current", "current",//
				"rslidersetting", "parent", "parent", //
				"rslidersetting", "child", "child", //
				
				"label", "\nNumber of decimal places in CIF file:", //
				"scifdec", " 5, 6, 7, 8, 9,10,11,12,13,14,15,16",//
				
				"label", "\n",
				"cnonstandardsetting", //
				"label", "Use alternate (possibly nonstandard) setting in CIF output (matrix S\u207b\u00b9)",// 
				"label", "\n             with respect to:",//
				"rsettingwrt", "parent", "parent", // 
				"rsettingwrt", "subgroup", "subgroup", // 
				"label","\n      Basis vectors of subgroup lattice (rational numbers):", //
				"grid", "3", "8", //
				
				"label"," ", //
				"label", "  a'    =", // 
				"fbasist11", //
				"label", "  a     +", // 
				"fbasist12", //
				"label", "  b     +", // 
				"fbasist13", //
				"label", "  c", // 
				
				"label"," ", //
				"label", "  b'    =",  //
				"fbasist21", //
				"label", "  a     +", // 
				"fbasist22", //
				"label", "  b     +", // 
				"fbasist23",  //
				"label", "  c", // 
				
				"label"," ", //
				"label", "  c'    =",   //
				"fbasist31",  //
				"label", "  a     +",  // 
				"fbasist32",  //
				"label", "  b     +",  // 
				"fbasist33",  //
				"label", "  c", // 
				
				"label", "\n      Origin of subgroup (either rational or decimal numbers):", // 
				"grid", "1", "8",   //
				"label", " ",
				"Forigint1",   //
				"label", "  a     +",   //
				"Forigint2",   //
				"label", "  b     +",   // 
				"Forigint3",   //
				"label", "  c",   //
				// end of grid
				"label", "\n", //
				"ccifmovie", //
				"label", "Make CIF movie:", //
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
		};
		
		@Override
		protected void createGUI() {
			addCenterPanel(cif);
			addLowerPanel();
		}

	}

	protected Runnable callback;
	
	IsoDialog(IsoApp app, Map<String, Object> formData, String title, boolean isModal, Runnable okCallback) {
		super(app.frame, title);
		setSize(500,600);
		setPreferredSize(new Dimension(500, 600));
		setMaximumSize(new Dimension(500, 600));
		this.app = app;
		this.formData = formData;
		this.callback = okCallback;

		addWindowListener(new WindowListener() {

			@Override
			public void windowOpened(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosing(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowClosed(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowIconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeiconified(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowActivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void windowDeactivated(WindowEvent e) {
				// TODO Auto-generated method stub
				
			}
			
		});
		setLayout(new BorderLayout());
		setModal(isModal);
	}

	protected void init() {
		createGUI();
		pack();
		setLocationRelativeTo(app.frame);  
	}
	protected void addLowerPanel() {
		JPanel p = new JPanel(new FlowLayout());
		p.setBackground(Color.DARK_GRAY);
		JButton b;
		b = new JButton("OK");
		b.setPreferredSize(new Dimension(80, 20));
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				okAction();
			}
			
		});
		p.add(b);
		b = new JButton("Cancel");
		b.setPreferredSize(new Dimension(80, 20));
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				cancelAction();
			}
			
		});
		p.add(b);
		add(p, BorderLayout.SOUTH);
	}
	
	protected void okAction() {
		setVisible(false);
		callback.run();
	}

	protected void cancelAction() {
		setVisible(false);
	}

	void setApp(IsoApp app) {
		this.app = app;
	}

	private List<Object> objects = new ArrayList<>();
	
	void addCenterPanel(String[] page) {
		Box box = new Box(BoxLayout.PAGE_AXIS);
		ButtonGroup group = null;
		String groupName = null;
		Object radioValue = null;
		Map<String, Object> formData = this.formData;
		JPanel p = null, rp = null;
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
				pad.setBorder(new EmptyBorder(0, 50, 0, 50));
				p = new JPanel(new GridLayout(rows, cols, 0, 0));
				pad.add(p);
				box.add(pad);				
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
					JLabel l = new JLabel(s);
					l.setBackground(Color.blue);
					p.add(l);
				}
				break;
			case 'r':
				// "rxxx" value text
				if (!key.equals(groupName)) {
					group = new ButtonGroup();
					groupName = key;
					radioValue = null;
					rp = new JPanel(new FlowLayout(FlowLayout.LEFT));
					p.add(rp);
				}
				String val = page[++i];
				String rtext = page[++i];
				JRadioButton r = new JRadioButton(rtext);
				group.add(r);
				r.setName(val);
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
				
				break;
			case 'c':
				// "cxxxx"
				JCheckBox c = new JCheckBox("");
				c.setSelected("true".equals(v));
				p.add(c);
				c.setName(key);
				objects.add(item);
				objects.add(c);
				break;
			case 's':
				// "sxxx", "5,6,7,8,9,10,11,12,13,14,15,16",//
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
				JTextField t = new JTextField(5);
				t.setPreferredSize(new Dimension(30, 10));
				t.setMaximumSize(new Dimension(30, 10));
				if (v != null) {
					t.setText(v.toString());
				}
				p.add(t);
				objects.add(item);
				objects.add(t);
				break;
			}
		}
		add(box, BorderLayout.CENTER);
	}

	Map<String, Object> getValues() throws RuntimeException {
		Map<String, Object> map = new HashMap<>();
		for (int i = 0, n = objects.size(); i < n; i++) {
			String key = (String) objects.get(i);
			JComponent c = (JComponent) objects.get(++i);
			Object value = null;
			switch (key.charAt(0)) {
			case 'r':
				if (((JRadioButton)c).isSelected()) {
					value = c.getName();
				}
				break;
			case 'c':
				value = "" + ((JCheckBox)c).isSelected();
				break;
			case 's':
				value =((JComboBox<?>) c).getSelectedItem();
				break;
			case 'i':
				value = validateInt(((JTextField)c).getText());
				break;
			case 'f':
				value = validateFractionalOrInt(((JTextField)c).getText());
				break;
			case 'F':
				value = validateFractionalOrDouble(((JTextField)c).getText());
				break;
			case 'd':
				value = validateDouble(((JTextField)c).getText());
				break;
			}
			if (value != null)
				map.put(key.substring(1), value);
		}
		return map;
	}
	
	Object validateInt(String text) {
		try {
			return Integer.parseInt(text.toString());
		} catch (Exception e) {
			throw new RuntimeException(text + " must be an integer");
		}
	}

	Object validateDouble(String text) {
		try {
			return Double.parseDouble(text.toString());
		} catch (Exception e) {
			throw new RuntimeException(text + " must be an decimal number");
		}
	}

	Object validateFractionalOrDouble(String s) {
		int pt = s.indexOf("/");
		if (pt > 1) {
			return validateFractionalOrInt(s);
		}
		return validateDouble(s);
	}

	Object validateFractionalOrInt(String s) {
		int pt = s.indexOf("/");
		if (pt > 1) {
			return validateInt(s.substring(0, pt)) + "/" + validateInt(s.substring(0, pt + 1)); 
		}
		return validateInt(s);
	}

	String test = "<br><input TYPE=\"checkbox\" NAME=\"zeromodes\" VALUE=\"true\"> Zero all mode and strain amplitudes for all output from this page\r\n" + 
			"<p>Parameters:\r\n" + 
			"<a href=\"isodistorthelp.php#modeparams\" target=\"_blank\"><img src=help.jpg></a><br>\r\n" + 
			"\"Save interactive distortion\":<br>\r\n" + 
			"Atomic radius:\r\n" + 
			"<input type=\"text\" name=\"atomicradius\" value=\"0.400\" class=\"span1\" size=5> Angstroms<br>\r\n" + 
			"Maximum bond length:\r\n" + 
			"<input type=\"text\" name=\"bondlength\" value=\"2.500\" class=\"span1\" size=5> Angstroms<br>\r\n" + 
			"Applet width:\r\n" + 
			"<input type=\"text\" name=\"appletwidth\" value=\" 1024\" class=\"span1\" size=5> pixels<br>\r\n" + 
			"Viewing range:\r\n" + 
			"xmin\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellxmin\" value=\"0.000\" size=5>\r\n" + 
			"xmax\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellxmax\" value=\"1.000\" size=5>\r\n" + 
			"ymin\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellymin\" value=\"0.000\" size=5>\r\n" + 
			"ymax\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellymax\" value=\"1.000\" size=5>\r\n" + 
			"zmin\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellzmin\" value=\"0.000\" size=5>\r\n" + 
			"zmax\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"supercellzmax\" value=\"1.000\" size=5><br>\r\n" + 
			"\"Save interactivie distortion\" and \"Save interactive diffraction\":<br>\r\n" + 
			"Maximum displacement per mode:\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"modeamplitude\" value=\"1.000\" size=5> Angstroms<br>\r\n" + 
			"Maximum strain per mode:\r\n" + 
			"<input type=\"text\" class=\"span1\" name=\"strainamplitude\" value=\"0.100\" size=5><br>\r\n" + 
			"<br><input TYPE=\"checkbox\" NAME=\"topasstrain\" VALUE=\"true\">\r\n" + 
			"Include strain modes in TOPAS.STR<br>\r\n" + 
			"<br><input TYPE=\"checkbox\" NAME=\"treetopas\" VALUE=\"true\">\r\n" + 
			"Generate TOPAS.STR output for subgroup tree<br>\r\n" + 
			"<br><input TYPE=\"checkbox\" NAME=\"treecif\" VALUE=\"true\">\r\n" + 
			"Generate CIF output for subgroup tree<br>\r\n" + 
			"";
	

}