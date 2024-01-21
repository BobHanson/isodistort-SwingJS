/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2021 Douglas Brown, Wolfgang Christian, Robert M. Hanson
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.byu.isodistort.local;

import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

/**
 * A class to maintain a map of actions keyed by name. These actions are called
 * by menus or buttons.
 * 
 * @author Bob Hanson
 * 
 */
public class MenuActions {

	protected IsoApp app;

	Map<String, IsoAction> actions = new LinkedHashMap<String, IsoAction>();

	MenuActions(IsoApp app) {
		this.app = app;
	}

	public JMenuBar createMenuBar() {
		JMenuBar jm = new JMenuBar();
		createMenu(jm);
		return jm;
	}

	public MenuActions() {

	}

	public static Map<String, IsoAction> createActions(IsoApp app) {
		return new MenuActions(app).getActions();
	}

	abstract class IsoAction extends AbstractAction {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		String id;
		String label;
		String tip;
		boolean hasSpacer = false;
		private int shortcut;

		public IsoAction(String id, String label, String tip, int shortcut) {
			super(label.startsWith("-") ? label.substring(1) : label);
			this.id = id;
			hasSpacer = label.startsWith("-");
			this.label = (hasSpacer ? label.substring(1) : label);
			this.tip = tip;
			this.shortcut = shortcut;
		}

	}

	private Map<String, JMenuItem> menuMap = new HashMap<>();

	private Map<String, IsoAction> getActions() {

		// this list is in the order of how the menu will be created.
		actions.put("File.", null);
		actions.put("File.Save.", null);
		actions.put("View.", null);
		actions.put("Set.", new IsoAction("set", "Set", null, 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			// we can catch menu openings and adjust as necessary just before opening
			@Override
			public void actionPerformed(ActionEvent e) {
				menuMap.get("Set.center").setEnabled(app.appType == IsoApp.APP_ISODISTORT);
			}

		});

		actions.put("Help.", new IsoAction("help", "Help", null, 0) {
			// Because this is a menu, not just an item, this action will be connected to
			// the mousePressed action.

			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				menuMap.get("Help.showStatus").setText(app.isStatusVisible() ? "Hide Status Bar" : "Show Status Bar");
			}

		});

		actions.put("File.Save.saveOriginal",
				new IsoAction("saveOriginal", "Original ISOVIZ", "Save original values as an isoviz file.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveOriginal();
					}
				});

		actions.put("File.Save.saveCurrent",
				new IsoAction("saveCurrent", "Current ISOVIZ", "Save current values as an isoviz file.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveCurrent();
					}
				});

		actions.put("File.Save.saveDistortionFile", new IsoAction("saveDistortionFile", "Distortion file",
				"Save current configuration as a distortion file.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.saveDistortionFile(null);
			}
		});

//		actions.put("File.Save.saveFormData", new IsoAction("saveFormData", "ISODISTORT Form Data JSON",
//				"Save ISODISTORT form data in JSON format.", 0) {
//			@Override
//			public void actionPerformed(ActionEvent e) {
//				app.saveFormData(null);
//			}
//		});
//
		actions.put("File.Save.saveCIF",
				new IsoAction("saveCIF", "-CIF file", "Save current configuration as CIF file.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(() -> {
							app.saveCIF(null);
						}).start();
					}
				});

		actions.put("File.Save.saveTOPAS",
				new IsoAction("saveTOPAS", "TOPAS.str file", "Save ISODISTORT section of TOPAS.STR.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveTOPAS(null);
					}
				});

		actions.put("File.Save.saveFULLPROF", new IsoAction("saveFULLPROF", "FullProf.pcr file", "Save ", 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.saveFULLPROF(null);
			}

		});
		
		actions.put("File.Save.saveImage",
				new IsoAction("saveImage", "-Save Image", "Save current PNG image.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveImage();
					}
				});



		actions.put("View.domains", new IsoAction("viewDomains", "Domains", "View domains for this distortion.", 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewPage("domains");
			}

		});

		actions.put("View.primaryOrderParameters", new IsoAction("viewPrimary", "Primary Order Parameters",
				"View a list of the primary order parameters.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewPage("primary");
			}
		});

		actions.put("View.modesDetails", new IsoAction("viewModesDetails", "-Modes Details",
				"View a detailed description of the symmetry modes.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewPage("modesdetails");
			}
		});

		actions.put("View.viewCompleteMode", new IsoAction("viewComplete", "Complete Mode Details",
				"View the complete listing of symmetry mode details.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewPage("completemodesdetails");
			}
		});

		actions.put("View.viewIRMatrices", new IsoAction("viewIRMatrices", "IR Matrices",
				"View the full list of irreducible representation matrices.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewPage("irreps");
			}
		});
		

		actions.put("View.subgroupTree", new IsoAction("viewSubgroupTree", "-Subgroup Tree",
				"View the subgroup tree listing for this distortion.", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.viewSubgroupTree(null);
			}
		});

		actions.put("Set.zero", new IsoAction("setZero", "Reset to Parent",
				"Set the sliders to their 0 'parent' positions.", KeyEvent.VK_Z) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 'z', 0));
			}
		});

		actions.put("Set.resetVariables", new IsoAction("setResetVariables", "Reset to Child",
				"Reset the sliders to their original 'child' positions.", KeyEvent.VK_I) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 'i', 0));
			}
		});

		actions.put("Set.toggle", new IsoAction("setToggle", "Toggle Irrep Sliders",
				"Toggle Irrep sliders between parent and child.", KeyEvent.VK_S) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 's', 0));
			}
		});

		actions.put("Set.resetView", new IsoAction("setResetView", "-Reset View",
				"Reset the view and sliders to their original settings.", KeyEvent.VK_R) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.reset();
			}
		});

		actions.put("Set.center",
				new IsoAction("setCenter", "Recenter Structure", "Center the strucure in the window.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						// IsoDistortApp only
						app.centerImage();
					}
				});

		actions.put("Set.preferences",
				new IsoAction("setPreferences", "-Preferences", "Set various atom and bond parameters.", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						app.setPreferences(null);
					}
				});

		actions.put("Help.showStatus", new IsoAction("helpShowStatus", "Show Status Bar", "Show the status bar.", 0) {
			/**
			 * 
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				app.toggleStatusVisible();
			}
		});

		actions.put("Help.isodistortHome",
				new IsoAction("helpIsodistortHome", "ISODISTORT Home Page", "Show the ISODISTORT Home Page", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						FileUtil.openURL(app, "https://iso.byu.edu/iso/isodistort.php");
					}
				});

		actions.put("Help.isodistortHelp",
				new IsoAction("helpIsodistortHelp", "ISODISTORT Help", "Show the ISODISTORT Help page", 0) {
					/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

					@Override
					public void actionPerformed(ActionEvent e) {
						FileUtil.openURL(app, "https://stokes.byu.edu/iso/isodistorthelp.php");
					}
				});

		actions.put("Help.isotropyHome", new IsoAction("helpIsotropyHome", "ISOTROPY Software Suite Home Page",
				"Show the ISOTROPY home page", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FileUtil.openURL(app, "https://iso.byu.edu/iso/isotropy.php");
			}
		});

		actions.put("Help.swingjs", new IsoAction("helpSwingjs", "About java2script/SwingJS",
				"Go to the java2script/SwingJS GitHub page", 0) {
			/**
					 * 
					 */
					private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent e) {
				FileUtil.openURL(app, "https://github.com/BobHanson/java2script");
			}
		});

		return actions;
	}

	/**
	 * Create a menu for a JPopupMenu or for a JMenuBar.
	 * 
	 * @param menuBar if null, then just return a JMenu object; if not null, use the
	 *                high-level business.
	 * @return
	 */
	private JComponent createMenu(JMenuBar menuBar) {
		JMenu menu = (menuBar == null ? new JMenu() : null);

//		if (true)return menuBar;

		Map<String, IsoAction> actionMap = getActions();
		for (String menuID : actionMap.keySet()) {
			IsoAction action = actionMap.get(menuID);
			boolean isMenu = menuID.endsWith(".");
			String thisName = (isMenu ? menuID.substring(0, menuID.length() - 1) : menuID);
			if (isMenu) {
				int pt = menuID.lastIndexOf(".", menuID.length() - 2);
				JMenu thisMenu = new JMenu(action == null ? thisName.substring(pt + 1) : action.label);
				JMenu parentMenu = null;
				if (pt > 0) {
					String parentName = menuID.substring(0, pt + 1);
					parentMenu = (JMenu) menuMap.get(parentName);
				} else {
					parentMenu = menu;
				}
				add(menuID, thisMenu, (parentMenu == null ? menuBar : null), parentMenu, action);
				continue;
			}
			// JMenuItem
			int pt = menuID.lastIndexOf(".");
			String parentName = menuID.substring(0, pt + 1);
			JMenu parentMenu = (JMenu) menuMap.get(parentName);
			JMenuItem thisItem = new JMenuItem(action.label);
			add(menuID, thisItem, null, parentMenu, action);
		}
		return (menuBar == null ? menu : menuBar);
	}

	private static final int SHORTCUT = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();

	private void add(String menuID, JMenuItem item, JMenuBar menuBar, JMenu menu, IsoAction action) {
		boolean isTopLevelMenuBarMenu = (menuBar != null && item instanceof JMenu && menuID.lastIndexOf(".", menuID.length() - 2) < 0);
		boolean hasSpacer = (action != null && action.hasSpacer);
		item.setName(menuID);
		if (action != null) {
			item.setText(action.label);
			if (item instanceof JMenu) {
				item.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						action.actionPerformed(
								new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, action.id + ".open"));
					}
				});
			} else {
				item.setAction(action);
				if (action.shortcut != 0) {
					item.setAccelerator(KeyStroke.getKeyStroke(action.shortcut, SHORTCUT, true));
				}
			}
		}
		if (menuID.equals("Help.") && menuBar != null) {
			menuBar.add(Box.createHorizontalGlue());
		}
		if (isTopLevelMenuBarMenu) {
			// top-level JMenu has only a single "."
			// adding some space for mouse to traverse to popup without closing it
			// turned out not to be necessary
			item.setText(item.getText());// + "    ");
			menuBar.add(item);
		} else {
			if (hasSpacer)
				menu.addSeparator();
			menu.add(item);
		}
		item.setName(menuID);
		menuMap.put(menuID, item);
	}

	public MenuActions setApp(IsoApp app) {
		this.app = app;
		return this;
	}

}

//Parameters: TODO

//Applet width: pixels
//Viewing range: xmin xmax ymin ymax zmin zmax
//"Save interactivie distortion" and "Save interactive diffraction":
//Maximum displacement per mode: Angstroms
//Maximum strain per mode: 

// Include strain modes in TOPAS.STR

// these are buttons at the bottom of subgroup tree
// Generate TOPAS.STR output for subgroup tree
// Generate CIF output for subgroup tree
