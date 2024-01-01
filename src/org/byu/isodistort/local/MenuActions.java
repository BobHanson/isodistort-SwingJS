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
 * A class to maintain a map of actions keyed by name. These actions are
 * called by menus or buttons. 
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

		String id;
		String label;
		String tip;
		private int shortcut;

		public IsoAction(String id, String label, String tip, int shortcut) {
			super(label);
			this.id	= id;
			this.label = label;
			this.tip = tip;
			this.shortcut = shortcut;
		}

	}
		
	private Map<String, JMenuItem> menuMap = new HashMap<>(); 
	
	private Map<String, IsoAction> getActions() {
		

		actions.put("File.", null);
		actions.put("View.", null);
		actions.put("Set.", new IsoAction("set", "Set", null, 0) {

			@Override
			public void actionPerformed(ActionEvent e) {
				menuMap.get("Set.center").setEnabled(app.appType == IsoApp.APP_ISODISTORT);
			}
			
		});
		actions.put("Help.", new IsoAction("help", "Help", null, 0) {
			// Because this is a menu, not just an item, this action will be connected to the mousePressed action. 

			@Override
			public void actionPerformed(ActionEvent e) {
				menuMap.get("Help.showStatus").setText(app.isStatusVisible() ? "Hide Status Bar" : "Show Status Bar");
			}
			
		});

		// this list is in the order of how the menu will be created.
		actions.put("File.saveOriginal",
				new IsoAction("saveOriginal", "Save original", "Save original values as an isoviz file.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveOriginal();
					}
				});
	
		actions.put("File.saveCurrent",
				new IsoAction("saveCurrent", "Save current", "Save current values as an isoviz file.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveCurrent();
					}
				});
		
		actions.put("File.saveCIF",
				new IsoAction("saveCIF", "Save CIF", "Save current configuration as CIF file.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						new Thread(() ->{
						app.saveCIF(null);
						}).start();
					}
				});
		
		actions.put("File.saveDistortionFile",
				new IsoAction("saveDistortionFile", "Save Distortion File", "Save current configuration as a distortion file.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						app.saveDistortion();
					}
				});

		actions.put("View.domains",
				new IsoAction("viewDomains", "View Domains", "View domains for this distortion.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
					}
					
//					ISODISTORT: domains
//
//					Space Group: 221 Pm-3m Oh-1, Lattice parameters: a=4.2
//					Default space-group preferences: monoclinic axes a(b)c, monoclinic cell choice 1, orthorhombic axes abc, origin choice 2, hexagonal axes, SSG standard setting
//					Sr 1b (1/2,1/2,1/2), Ti 1a (0,0,0), O 3d (1/2,0,0)
//					Include strain, displacive ALL distortions
//					Subgroup: 161 R3c, basis={(1,0,1),(0,1,-1),(-2,2,2)}, origin=(1.99028,-1.99028,-1.99028), s=2, i=16
//
//					Domains #, lattice orientation #, internal orientation #, origin shift #, parent symmetry relation to domain #1, space group, basis, origin
//
//					#1 1 1 1 (x,y,z) 161 R3c, basis={(1,0,1),(0,1,-1),(-2,2,2)}, origin=(0,0,0)
//					#9 1 1 2 (x-1,y+1,z+1) 161 R3c, basis={(1,0,1),(0,1,-1),(-2,2,2)}, origin=(-1,1,1)
//					#5 1 2 1 (y,x,-z) 161 R3c, basis={(0,1,-1),(1,0,1),(2,-2,-2)}, origin=(0,0,0)
//					#13 1 2 2 (y+1,x-1,-z-1) 161 R3c, basis={(0,1,-1),(1,0,1),(2,-2,-2)}, origin=(1,-1,-1)
//					#8 2 1 1 (x,-y,-z) 161 R3c, basis={(1,0,-1),(0,-1,1),(-2,-2,-2)}, origin=(0,0,0)
//					#16 2 1 2 (x-1,-y-1,-z-1) 161 R3c, basis={(1,0,-1),(0,-1,1),(-2,-2,-2)}, origin=(-1,-1,-1)
//					#4 2 2 1 (y,-x,z) 161 R3c, basis={(0,-1,1),(1,0,-1),(2,2,2)}, origin=(0,0,0)
//					#12 2 2 2 (y+1,-x+1,z+1) 161 R3c, basis={(0,-1,1),(1,0,-1),(2,2,2)}, origin=(1,1,1)
//					#2 3 1 1 (-x,y,-z) 161 R3c, basis={(-1,0,-1),(0,1,1),(2,2,-2)}, origin=(0,0,0)
//					#10 3 1 2 (-x+1,y+1,-z-1) 161 R3c, basis={(-1,0,-1),(0,1,1),(2,2,-2)}, origin=(1,1,-1)
//					#6 3 2 1 (-y,x,z) 161 R3c, basis={(0,1,1),(-1,0,-1),(-2,-2,2)}, origin=(0,0,0)
//					#14 3 2 2 (-y-1,x-1,z+1) 161 R3c, basis={(0,1,1),(-1,0,-1),(-2,-2,2)}, origin=(-1,-1,1)
//					#7 4 1 1 (-x,-y,z) 161 R3c, basis={(-1,0,1),(0,-1,-1),(2,-2,2)}, origin=(0,0,0)
//					#15 4 1 2 (-x+1,-y-1,z+1) 161 R3c, basis={(-1,0,1),(0,-1,-1),(2,-2,2)}, origin=(1,-1,1)
//					#3 4 2 1 (-y,-x,-z) 161 R3c, basis={(0,-1,-1),(-1,0,1),(-2,2,-2)}, origin=(0,0,0)
//					#11 4 2 2 (-y-1,-x+1,-z-1) 161 R3c, basis={(0,-1,-1),(-1,0,1),(-2,2,-2)}, origin=(-1,1,-1)
//
//
//					Permutations of domains
//					*1:(x,y,z) 1 2 3 4 5 6 7 8 9 10 11 12 13 14 15 16
//					1:(x+1,y,z) 9 10 11 12 13 14 15 16 1 2 3 4 5 6 7 8
//					1:(x,y+1,z) 9 10 11 12 13 14 15 16 1 2 3 4 5 6 7 8
//					1:(x,y,z+1) 9 10 11 12 13 14 15 16 1 2 3 4 5 6 7 8
//					2[100]:(x,-y,-z) 8 7 6 5 4 3 2 1 16 15 14 13 12 11 10 9
//					2[010]:(-x,y,-z) 2 1 4 3 6 5 8 7 10 9 12 11 14 13 16 15
//					2[001]:(-x,-y,z) 7 8 5 6 3 4 1 2 15 16 13 14 11 12 9 10
//					3[111]:(z,x,y) 7 1 6 4 3 5 2 8 15 9 14 12 11 13 10 16
//					3[-1-1-1]:(y,z,x) 2 7 5 4 6 3 1 8 10 15 13 12 14 11 9 16
//					*3[-111]:(-y,z,-x) 1 8 6 3 5 4 2 7 9 16 14 11 13 12 10 15
//					*3[1-1-1]:(-z,-x,y) 1 7 4 6 5 3 8 2 9 15 12 14 13 11 16 10
//					3[1-11]:(-y,-z,x) 8 1 3 6 4 5 7 2 16 9 11 14 12 13 15 10
//					3[-11-1]:(z,-x,-y) 2 8 3 5 6 4 7 1 10 16 11 13 14 12 15 9
//					3[11-1]:(y,-z,-x) 7 2 4 5 3 6 8 1 15 10 12 13 11 14 16 9
//					3[-1-11]:(-z,x,-y) 8 2 5 3 4 6 1 7 16 10 13 11 12 14 9 15
//					4[100]:(x,-z,y) 6 4 1 7 2 8 5 3 14 12 9 15 10 16 13 11
//					4[-100]:(x,z,-y) 3 5 8 2 7 1 4 6 11 13 16 10 15 9 12 14
//					4[010]:(z,y,-x) 4 3 1 2 8 7 5 6 12 11 9 10 16 15 13 14
//					4[0-10]:(-z,y,x) 3 4 2 1 7 8 6 5 11 12 10 9 15 16 14 13
//					4[001]:(-y,x,z) 6 3 8 1 2 7 4 5 14 11 16 9 10 15 12 13
//					4[00-1]:(y,-x,z) 4 5 2 7 8 1 6 3 12 13 10 15 16 9 14 11
//					2[110]:(y,x,-z) 5 4 7 2 1 8 3 6 13 12 15 10 9 16 11 14
//					2[-110]:(-y,-x,-z) 3 6 1 8 7 2 5 4 11 14 9 16 15 10 13 12
//					2[101]:(z,-y,x) 5 6 8 7 1 2 4 3 13 14 16 15 9 10 12 11
//					2[-101]:(-z,-y,-x) 6 5 7 8 2 1 3 4 14 13 15 16 10 9 11 12
//					2[011]:(-x,z,y) 4 6 7 1 8 2 3 5 12 14 15 9 16 10 11 13
//					2[0-11]:(-x,-z,-y) 5 3 2 8 1 7 6 4 13 11 10 16 9 15 14 12
//					-1:(-x,-y,-z) 13 14 15 16 9 10 11 12 5 6 7 8 1 2 3 4
//					-2[100]:(-x,y,z) 12 11 10 9 16 15 14 13 4 3 2 1 8 7 6 5
//					-2[010]:(x,-y,z) 14 13 16 15 10 9 12 11 6 5 8 7 2 1 4 3
//					-2[001]:(x,y,-z) 11 12 9 10 15 16 13 14 3 4 1 2 7 8 5 6
//					-3[111]:(-z,-x,-y) 11 13 10 16 15 9 14 12 3 5 2 8 7 1 6 4
//					-3[-1-1-1]:(-y,-z,-x) 14 11 9 16 10 15 13 12 6 3 1 8 2 7 5 4
//					-3[-111]:(y,-z,x) 13 12 10 15 9 16 14 11 5 4 2 7 1 8 6 3
//					-3[1-1-1]:(z,x,-y) 13 11 16 10 9 15 12 14 5 3 8 2 1 7 4 6
//					-3[1-11]:(y,z,-x) 12 13 15 10 16 9 11 14 4 5 7 2 8 1 3 6
//					-3[-11-1]:(-z,x,y) 14 12 15 9 10 16 11 13 6 4 7 1 2 8 3 5
//					-3[11-1]:(-y,z,x) 11 14 16 9 15 10 12 13 3 6 8 1 7 2 4 5
//					-3[-1-11]:(z,-x,y) 12 14 9 15 16 10 13 11 4 6 1 7 8 2 5 3
//					-4[100]:(-x,z,-y) 10 16 13 11 14 12 9 15 2 8 5 3 6 4 1 7
//					-4[-100]:(-x,-z,y) 15 9 12 14 11 13 16 10 7 1 4 6 3 5 8 2
//					-4[010]:(-z,-y,x) 16 15 13 14 12 11 9 10 8 7 5 6 4 3 1 2
//					-4[0-10]:(z,-y,-x) 15 16 14 13 11 12 10 9 7 8 6 5 3 4 2 1
//					-4[001]:(y,-x,-z) 10 15 12 13 14 11 16 9 2 7 4 5 6 3 8 1
//					-4[00-1]:(-y,x,-z) 16 9 14 11 12 13 10 15 8 1 6 3 4 5 2 7
//					*-2[110]:(-y-1,-x+1,z+1) 1 8 3 6 5 4 7 2 9 16 11 14 13 12 15 10
//					-2[-110]:(y,x,z) 15 10 13 12 11 14 9 16 7 2 5 4 3 6 1 8
//					*-2[101]:(-z-1,y+1,-x+1) 1 2 4 3 5 6 8 7 9 10 12 11 13 14 16 15
//					-2[-101]:(z,y,x) 10 9 11 12 14 13 15 16 2 1 3 4 6 5 7 8
//					-2[011]:(x,-z,-y) 16 10 11 13 12 14 15 9 8 2 3 5 4 6 7 1
//					*-2[0-11]:(x-1,z+1,y+1) 1 7 6 4 5 3 2 8 9 15 14 12 13 11 10 16
//
//					Normal-core: 1 P1, basis={(-1,0,1),(-1,1,0),(0,-1,-1)}, origin=(0,0,0), s=2, i=96
				});

		actions.put("View.primaryOrderParameters",
				new IsoAction("viewPrimary", "View Primary Order Parameters", "View a list of the primary order parameters.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.viewPrimaryOrderParameters();
//						 Default space-group preferences: monoclinic axes a(b)c, monoclinic cell choice 1, orthorhombic axes abc, origin choice 2, hexagonal axes, SSG standard setting
//
//						 Order parameters capable of displacements, or strain:
//
//						 Pm-3m[0,0,0]GM1+(a)
//						 Pm-3m[0,0,0]GM5+(a,-a,a)
//						 Pm-3m[0,0,0]GM4-(a,-a,-a)
//						 Pm-3m[1/2,1/2,1/2]R4+(a,-a,a)
//
//						 Possible sets of primary order parameters:
//						 Sets which allow the phase transition to be continuous are marked with an asterick (*)
//
//						 GM4- R4+

					}
				});
		
		actions.put("View.modesDetails",
				new IsoAction("viewModesDetails", "View Modes Details", "View a detailed description of the symmetry modes.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.viewModeDetails();


//ISODISTORT: modes details
//
//Parent structure (221 Pm-3m)
//a=4.20000, b=4.20000, c=4.20000, alpha=90.00000, beta=90.00000, gamma=90.00000
//atom site    x         y         z         occ   
//Sr   1b      0.50000    0.50000    0.50000    1.00000
//Ti   1a      0.00000    0.00000    0.00000    1.00000
//O    3d      0.50000    0.00000    0.00000    1.00000
//
//Subgroup details
//161 R3c, basis={(1,0,1),(0,1,-1),(-2,2,2)}, origin=(1.99028,-1.99028,-1.99028), s=2, i=16
//
//Undistorted superstructure 
//a=5.93970, b=5.93970, c=14.54923, alpha=90.00000, beta=90.00000, gamma=120.00000
//atom site    x         y         z         occ      displ  
//Sr_1 6a      0.00000    0.00000    0.74514    1.00000    0.00000  
//Ti_1 6a      0.00000    0.00000    0.99514    1.00000    0.00000  
//O_1  18b     0.66667    0.83333    0.07847    1.00000    0.00000  
//
//Distorted superstructure 
//a=5.93970, b=5.93970, c=14.54923, alpha=90.00000, beta=90.00000, gamma=120.00000
//atom site    x         y         z         occ      displ  
//Sr_1 6a      0.00000    0.00000    0.74514    1.00000    0.00000  
//Ti_1 6a      0.00000    0.00000    0.99028    1.00000    0.07071  
//O_1  18b     0.66303    0.83152    0.07909    1.00000    0.02075  
//
//Displacive mode definitions
//
//atom    x        y        z        dx      dy      dz   
//Pm-3m[0,0,0]GM4-(a,-a,-a)[Sr:b:dsp]T1u(a) normfactor =  0.04860
//Sr_1  0.00000  0.00000  0.74514  0.0000  0.0000 -1.0000 
//
//Pm-3m[0,0,0]GM4-(a,-a,-a)[Ti:a:dsp]T1u(a) normfactor =  0.04860
//Ti_1  0.00000  0.00000  0.99514  0.0000  0.0000 -1.0000 
//
//Pm-3m[0,0,0]GM4-(a,-a,-a)[O:d:dsp]A2u(a) normfactor =  0.06480
//O_1   0.66667  0.83333  0.07847  1.0000  0.5000 -0.2500 
//
//Pm-3m[0,0,0]GM4-(a,-a,-a)[O:d:dsp]Eu(a) normfactor =  0.04582
//O_1   0.66667  0.83333  0.07847 -1.0000 -0.5000 -0.5000 
//
//Pm-3m[1/2,1/2,1/2]R4+(a,-a,a)[O:d:dsp]Eu(a) normfactor =  0.06873
//O_1   0.66667  0.83333  0.07847  0.0000 -1.0000  0.0000 
//
//Displacive mode amplitudes
//
//mode                                      As        Ap       dmax
//[0,0,0]GM4-(a,-a,-a)[Sr:b:dsp]T1u(a)      0.00000    0.00000   0.00000
//[0,0,0]GM4-(a,-a,-a)[Ti:a:dsp]T1u(a)      0.10000    0.07071   0.07071
//[0,0,0]GM4-(a,-a,-a)[O:d:dsp]A2u(a)     -0.05010  -0.03543   0.02045
//[0,0,0]GM4-(a,-a,-a)[O:d:dsp]Eu(a)        0.00851    0.00602   0.00347
//[0,0,0]GM4-(a,-a,-a)  all                 0.11217   0.07932
//
//[1/2,1/2,1/2]R4+(a,-a,a)[O:d:dsp]Eu(a)  -0.00007  -0.00005   0.00003
//[1/2,1/2,1/2]R4+(a,-a,a)  all             0.00007   0.00005
//
//Overall                                   0.11217   0.07932
//
//Parent-cell strain mode definitions
//
//    e1      e2      e3      e4      e5      e6
//Pm-3m[0,0,0]GM1+(a)strain(a) normfactor =  0.57735
//  1.0000  1.0000  1.0000  0.0000  0.0000  0.0000
//Pm-3m[0,0,0]GM5+(a,-a,a)strain(a) normfactor =  0.81650
// -0.0000 -0.0000 -0.0000  1.0000 -1.0000 -1.0000
//
//Parent-cell strain mode amplitudes
//
//mode                          amplitude
//[0,0,0]GM1+(a)strain(a)        0.00000
//[0,0,0]GM5+(a,-a,a)strain(a)   0.00000
//
					}
				});
				

		actions.put("View.viewCompleteMode",
				new IsoAction("viewComplete", "View Complete Mode Details","View the complete listing of symmetry mode details.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.viewCompleteModeDetails();

					}
				});
				

		actions.put("File.TopasStr",
				new IsoAction("saveTOPAS", "Save TOPAS str", "Save distortion as a TOPAS str file", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.saveTOPASstr();
//
//						'Topas .str file generated by ISODISTORT
//						'Remember to add the appropriate peak shape line when passing this into an input file
//
//							str
//								'R3c 
//								space_group 161 'transformPp a,b,c;0,0,0
//								a     5.93970
//								b     5.93970
//								c    14.54923
//								al   90.00000
//								be   90.00000
//								ga  120.00000
//								scale @ 0.00001
//

					}
				});


		actions.put("File.saveFULLPROF",
				new IsoAction("saveFULLPROF", "Save FULLPROF CPR Input", "Save ", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.saveFULLPROFcpr();


//						COMM  AMPLIMODES for FullProf
//						! Files => DAT-file: myDAT_file PCR-file: myPCR_file
//						!Job Npr Nph Nba Nex Nsc Nor Dum Iwg Ilo Ias Res Ste Nre Cry Uni Cor Opt Aut
//						  3  7   1   0   2   0   0   0   0   0   0   0   0   0   0   0   0   0   1
//						!
//						!Ipr Ppl Ioc Mat Pcr Ls1 Ls2 Ls3 NLI Prf Ins Rpa Sym Hkl Fou Sho Ana
//						  0   0   1   0   1   0   4   0   0   3   0  0   0   0   0   0   0
//						!
//						! lambda1 Lambda2    Ratio    Bkpos    Wdt    Cthm     muR   AsyLim   Rpolarz ->Patt# 1
//						1.227200 1.227200  0.0000  50.000 10.0000  0.0000  0.0000  170.00    0.0000
//						!
					}
				});


		
		
		
		actions.put("File.saveIRMatrices",
				new IsoAction("saveIRMatrices", "Save IR Matrices", "Save the full list of irreducible representation matrices.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.saveIRMatrices();

//						ISODISTORT: IR matrices
//
//						Space Group 221 Pm-3m
//
//						For each representative symmetry element ofthe parent space group, we display (1) the space-group operator, (2) the character of the IR of the little group of k if the operator is contained in the little group of k, and (3) the IR matrix.
//						IR GM1+
//
//						Star of k: (0,0,0),
//
//						(1)	(2)	(3)
//						1:(x,y,z) 	1 	(1)
//						2[100]:(x,-y,-z) 	1 	(1)
//						2[010]:(-x,y,-z) 	1 	(1) 
					}
				});

		
		actions.put("View.subgroupTree",
				new IsoAction("viewSubgroupTree", "View Subgroup Tree", "View the subgroup tree listing for this distortion.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {
						
						app.viewSubgroupTree();

//						ISODISTORT: subgroup tree
//
//						Subgroup 1
//						221 Pm-3m, basis={(0,-1,0),(-1,0,0),(0,0,-1)}, origin=(0,0,0), s=1, i=1
//						Maximal subgroups: 2, 3, 5, 6
//						a=4.20000,b=4.20000,c=4.20000,alpha=90.00000,beta=90.00000,gamma=90.00000
//						Order parameters: GM1+ (a)
//						Active k vectors: (0,0,0)
//						This subgroup does not produce any selected distortions
//
//						Subgroup 2
//						215 P-43m, basis={(0,-1,0),(-1,0,0),(0,0,-1)}, origin=(0,0,0), s=1, i=2
//						Maximal subgroups: 4, 7
//						a=4.20000,b=4.20000,c=4.20000,alpha=90.00000,beta=90.00000,gamma=90.00000
//						Order parameters: GM1+ (a) GM2- (a)
//						Active k vectors: (0,0,0)
//						This subgroup does not produce any selected distortions
					}
				});

		
		actions.put("Help.showStatus",
				new IsoAction("helpShowStatus", "Show Status Bar", "Show the status bar.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						app.toggleStatusVisible();
					}
				});

		actions.put("Help.isodistortHome",
				new IsoAction("helpIsodistortHome", "ISODISTORT Home Page", "Show the ISODISTORT Home Page", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						FileUtil.openURL(app, "https://iso.byu.edu/iso/isodistort.php");
					}
				});
		
		actions.put("Help.isodistortHelp",
				new IsoAction("helpIsodistortHelp", "ISODISTORT Help", "Show the ISODISTORT Help page", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						FileUtil.openURL(app, "https://stokes.byu.edu/iso/isodistorthelp.php");
					}
				});

		
		actions.put("Help.isotropyHome",
				new IsoAction("helpIsotropyHome", "ISOTROPY Software Suite Home Page", "Show the ISOTROPY home page", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						FileUtil.openURL(app, "https://iso.byu.edu/iso/isotropy.php");
					}
				});
		
		actions.put("Help.swingjs",
				new IsoAction("helpSwingjs", "About java2script/SwingJS", "Go to the java2script/SwingJS GitHub page", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						FileUtil.openURL(app, "https://github.com/BobHanson/java2script");
					}
				});

		actions.put("Set.center",
				new IsoAction("setCenter", "Center Structure", "Center the strucure in the window.", 0) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						app.centerImage();
					}
				});


		actions.put("Set.zero",
				new IsoAction("setZero", "Set Variables to 0", "Set the sliders to their 0 'parent' positions.", KeyEvent.VK_Z) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 'z', 0));
					}
				});

		actions.put("Set.resetView",
				new IsoAction("setResetView", "Reset View", "Reset the view and sliders to their original settings.", KeyEvent.VK_R) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						app.reset();		
						}
				});

		actions.put("Set.resetVariables",
				new IsoAction("setResetVariables", "Reset Variables", "Reset the sliders to their original 'child' positions.", KeyEvent.VK_I) {
					@Override
					public void actionPerformed(ActionEvent e) {
						app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 'i', 0));
					}
				});

		actions.put("Set.toggle",
				new IsoAction("setToggle", "Toggle Irrep Sliders", "Toggle Irrep sliders between full off and full on.", KeyEvent.VK_S) {
					@Override
					public void actionPerformed(ActionEvent e) {	
						app.variables.keyTyped(new KeyEvent(app.frame, KeyEvent.KEY_TYPED, 0, 0, 0, 's', 0));
					}
				});


		
//		actions.put("",
//				new IsoAction("viewPrimary", "View Primary Order Parameters", "View.") {
//					@Override
//					public void actionPerformed(ActionEvent e) {
//						
////						app.viewPrimaryOrderParameters();
//
//					}
//				});


		return actions;
	}

	
	
	
	/**
	 * Create a menu for a JPopupMenu or for a JMenuBar. 
	 * 
	 * @param menuBar if null, then just return a JMenu object; if not null, use the high-level business.
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
				JMenu thisMenu = new JMenu(action == null ? thisName : action.label);
				add(menuID, thisMenu, menuBar, menu, action);
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
		item.setName(menuID);
		if (action != null) {
			item.setText(action.label);
			if (item instanceof JMenu) {
				item.addMouseListener(new MouseAdapter() {
					@Override
					public void mousePressed(MouseEvent e) {
						action.actionPerformed(new ActionEvent(e.getSource(), ActionEvent.ACTION_PERFORMED, action.id + ".open"));
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
		if (menuBar != null && item instanceof JMenu && menuID.lastIndexOf(".", menuID.length() - 2) < 0) {
			// top-level JMenu has only a single "."
			menuBar.add(item);
		} else {
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

// parameters

//Parameters:
//"Save interactive distortion":
//Atomic radius: Angstroms
//Maximum bond length: Angstroms
//Applet width: pixels
//Viewing range: xmin xmax ymin ymax zmin zmax
//"Save interactivie distortion" and "Save interactive diffraction":
//Maximum displacement per mode: Angstroms
//Maximum strain per mode: 


// Include strain modes in TOPAS.STR

// these are buttons at the bottom of subgroup tree
// Generate TOPAS.STR output for subgroup tree
// Generate CIF output for subgroup tree

