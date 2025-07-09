/**
 *  David Tanner
 *  April 2005
 * 
 * 
 */

package org.byu.isodistort;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.BitSet;
import java.util.Map;

import javax.swing.JPanel;

import org.byu.isodistort.local.Cell;
import org.byu.isodistort.local.FileUtil;
import org.byu.isodistort.local.MathUtil;
import org.byu.isodistort.local.Variables;
import org.byu.isodistort.local.Variables.IsoAtom;
import org.byu.isodistort.render.Geometry;
import org.byu.isodistort.render.RenderPanel3D.IsoMaterial;
import org.jmol.adapter.smarter.SmarterJmolAdapter;
import org.jmol.api.JmolStatusListener;
import org.jmol.api.JmolViewer;
import org.jmol.c.CBK;
import org.jmol.viewer.Viewer;

import javajs.util.BS;
import javajs.util.M3d;
import javajs.util.M4d;
import javajs.util.Qd;

/**
 * 
 * The IsoSymmetry class is purely experimental. I could be used to display
 * symmetry elements using a Jmol application display panel. It is set up for
 * that, but this app is only visible if boolean IsoApp.addJmol is set to
 * true.
 * 
 * This class extends IsoDistortApp for now but probably does not need to be. 
 * 
 * 
 * @author Bob Hanson(mostly just refactoring)
 *
 */
public class IsoSymmetryApp extends IsoDistortApp {

	Viewer viewer;
	private IsoJmolPanel rpJmol;
	private Geometry world;

	class IsoJmolPanel extends JPanel implements IsoRenderPanel {

		private static final String jmolStartupScript = "frank off;set antialiasdisplay true;background lightgray;set showUnitCellInfo false;set defaultdrawArrowScale -0.66";

		private final Dimension currentSize = new Dimension();

		boolean spin;
		
		IsoJmolPanel() {
			viewer = (Viewer) JmolViewer.allocateViewer(this, new SmarterJmolAdapter(), 
					null, null, 
					null, null, null);
			
			viewer.setJmolStatusListener(new JmolStatusListener() {

					@Override
					public void setCallbackFunction(String callbackType, String callbackObject) {
//						// TODO Auto-generated method stub
//						System.out.println(">?");
					}
			
					@Override
					public void notifyCallback(CBK message, Object[] data) {
//						System.out.println(">? " + message);
						
					}
			
					@Override
					public boolean notifyEnabled(CBK type) {
//						System.out.println(">?" + type);
						return false;
					}
			
					@Override
					public String eval(String strEval) {
//						System.out.println(">?");
						return null;
					}
			
					@Override
					public double[][] functionXY(String functionName, int x, int y) {
						return null;
					}
			
					@Override
					public double[][][] functionXYZ(String functionName, int nx, int ny, int nz) {
						return null;
					}
			
					@Override
					public String createImage(String fileName, String type, Object text_or_bytes, int quality) {
//						System.out.println(">?");
						return null;
					}
			
					@Override
					public Map<String, Object> getRegistryInfo() {
//						System.out.println(">?");
						return null;
					}
			
					@Override
					public void showUrl(String url) {
						FileUtil.showUrl(url);
					}

					@Override
					public int[] resizeInnerPanel(String data) {
						return null;
					}
		
					@Override
					public Map<String, Object> getJSpecViewProperty(String type) {
						return null;
					}
			});
			
			jmolScriptWait(jmolStartupScript);
		}

		@Override
		public void paint(Graphics g) {
			getSize(currentSize);
			viewer.renderScreenImage(g, currentSize.width, currentSize.height);
		}

		@Override
		public void updateForDisplay(boolean b) {
			// unnecessary, probably
		}

		@Override
		public void setSpinning(boolean spin) {
			this.spin = spin;
		}

		@Override
		public boolean isSpinning() {
			return spin;
		}

		@Override
		public void clearAngles() {
			// TODO Auto-generated method stub

		}

		@Override
		public void setCamera(double theta, double phi, double sigma) {
			// TODO Auto-generated method stub

		}

		@Override
		public void reversePanningAction() {
			// TODO Auto-generated method stub

		}

		@Override
		public void resetView() {
			// TODO Auto-generated method stub

		}

		@Override
		public void centerImage() {
			viewer.script("center {0 0 0}");
		}

		@Override
		public BufferedImage getImage() {
			return (BufferedImage) viewer.getScreenImage();
		}

		@Override
		public void setPerspective(double[][] params) {
			// TODO Auto-generated method stub

		}

		@Override
		public double[][] getPerspective() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void initializeSettings(double radius) {
		}

		@Override
		public double[] getCameraMatrix() {
			M3d m = viewer.tm.getRotationQ().getMatrix();
			return new double[] { //
					m.m00, m.m01, m.m02, 0, // 
					m.m10, m.m11, m.m12, 0, //
					m.m20, m.m21, m.m22, 0, //
					0, 0, 0, 1 };
		}
		
		@Override
		public double getZoom() {
			return viewer.tm.getZoomSetting();
		}

		@Override
		public void setCameraMatrixAndZoom(double[] m, double zoom) {
			M3d m3 = new M3d();
			M4d.newA16(m).getRotationScale(m3);
			Qd q = Qd.newM(m3);
			String script = "moveto 0 QUATERNION " + q + ";zoom " + zoom;
			jmolScriptWait(script);
		}

	}

	public IsoSymmetryApp() {
		super();
		this.appType = APP_ISOSYMMETRY;
	}

	@Override
	protected void init() {
		fromApp = frame.from3DApp;
		rp = rpJmol = new IsoJmolPanel();
//	    JPanel panel2 = new JPanel();
//	    AppConsole console = new AppConsole(rp.viewer, panel2,
//	        "History State Clear");
//	    rp.viewer.setJmolCallbackListener(console);
		rpJmol.setPreferredSize(drawPanel.getSize());
		rpJmol.setSize(drawPanel.getSize());
		drawPanel.removeAll();
		drawPanel.add((JPanel) rpJmol, BorderLayout.CENTER);		
		rpJmol.addKeyListener(this);		
		world = new Geometry();
		initWorld(world);	
		updateCells();
		jmolInitAxes();
		updateSelectedObjects();
		resetViewDirection(VIEW_TYPE_CHILD_HKL);
	}

	@Override
	protected void initAtoms(double radius) {
		super.initAtoms(radius);
		createJmolModel();		
	}
	private void createJmolModel() {
		StringBuffer xyzFile = new StringBuffer();
		int nAtoms = variables.nAtoms;
		xyzFile.append("zap;load data 'xyz'\n");
		xyzFile.append(nAtoms).append("\n\n");
		for (int i = 0; i < nAtoms; i++) {
			IsoAtom a = variables.getAtom(i);
			xyzFile.append(a.getAtomTypeSymbol()).append(" ");
			double[] coord = a.getCartesianCoord();
			xyzFile.append(coord[0]).append(' ');
			xyzFile.append(coord[1]).append(' ');
			xyzFile.append(coord[2]).append('\n');
		}
		xyzFile.append("end 'xyz'\n");
		xyzFile.append("modelkit spacegroup P1;center {0 0 0}");
		jmolScriptWait(xyzFile.toString());		
	}

	private String jmolPoint(double[] p) {
		return "{" + p[0] + " " + p[1] + " " + p[2] + "}";
	}

	@Override
	public void reset() {
		super.reset();
//		viewer.script("restore orientation");
	}

	@Override
	protected boolean prepareToSwapOut() {
		super.prepareToSwapOut();
//		viewer.script("animation off;vibration off;spin off");
		return true;
	}

	@Override
	protected void handleButtonEvent(Object src) {
		super.handleButtonEvent(src);
	}

	@Override
	protected void applyView() {
		super.applyView();
	}


	@Override
	public void recalcCellColors() {
		super.recalcCellColors();
		// apply colors
	}

	@Override
	protected void updateBonding() {
		super.updateBonding();
	}

	double bondRadius = 0.15;
	
	@Override
	protected void updateSelectedObjects() {
		apBox.setEnabled(variables.isPrimitive(-1));
		String script = "";
		script += "display " + (showAtoms ? "all;" : "none;");
		script += "draw cell_parent* " + (showParentCell ? "on;" : "off;");
		script += "draw cell_child* " + (showChildCell ? "on;" : "off;");
		script += "wireframe " + (showBonds ? 0 : bondRadius) + ";";
		script += "draw axis_parent* " + (showAxes && (showParentCell || !showChildCell) ? "on;" : "off;");
		script += "draw axis_child* " + (showAxes && (showChildCell || !showParentCell) ? "on;" : "off;");
		jmolScriptWait(script);
	}

	private void jmolScriptWait(String script) {
		System.out.println ("SYMAPP runScript " + script);
		viewer.scriptWait(script);
	}

	@Override
	protected void updateAtomColors() {
		super.updateAtomColors();
		double[] diff = new double[4];
		double[] spec = new double[4];
		double[] amb = new double[3];
		String script = "";
		for (int t = variables.nTypes; --t >= 0;) {
			for (int s = variables.nSubTypes[t]; --s >= 0;) {
				subMaterial[t][s].getDiffuse(diff);
				subMaterial[t][s].getSpecular(spec);
				subMaterial[t][s].getAmbient(amb);
				BS bsAtoms = newBS(variables.getAtomsFromTS(t, s));
				String c = getJmolColor(diff, spec, amb);
				script += "color " + bsAtoms + " " + c + ";";
			}
		}
		viewer.scriptWait(script);
	}

	@Override
	protected IsoMaterial newMaterial() {
		return new IsoMaterial(null);
	}

	@Override
	protected void updateCells() {
		jmolSetCell(PARENT);
		jmolSetCell(CHILD);
	}

	@Override
	protected void updateAxes() {
		// n/a
	}

	@Override
	protected void updateAtoms() {
		for (int i = 0, n = variables.nAtoms; i < n; i++) {
			double[][] info = variables.getAtomInfo(i);
			boolean isEnabled = !showPrimitiveAtoms || variables.isPrimitive(i);
//			Geometry a = atomObjects.child(i);
//			a.setEnabled(isEnabled);
//			renderScaledAtom(a, info[DIS], info[OCC][0] * variables.atomMaxRadius);
//			renderArrow(a.child(MAG - 2), info[MAG], momentMultiplier, variables.angstromsPerMagneton);
//			renderArrow(a.child(ROT - 2), info[ROT], rotationMultiplier, variables.angstromsPerRadian);
//			renderEllipsoid(a.child(ELL - 2), info[ELL], 1 / Math.sqrt(variables.defaultUiso));
		}
	}

	@Override
	protected void updateBonds() {
//		double r = Math.max(variables.atomMaxRadius * bondMultiplier, 0.05);
//		for (int b = bsBondsEnabled.nextSetBit(0); b >= 0; b = bsBondsEnabled.nextSetBit(b + 1)) {
//			transformCylinder(r, bondInfo[b], bondObjects.child(b));
//		}
	}

	private void renderArrow(Geometry child, double[] info, double r, double scale) {
		boolean isOK = (Math.abs(info[2]) > 0.1);
//		child.setEnabled(isOK);
//		if (!isOK)
//			return;
	}

	@Override
	protected void updateViewOptions() {
		super.updateViewOptions();
		updateSelectedObjects();
	}

	@Override
	public void stop() {
		super.stop();

	}
	
	@Override
	protected void takeFocus() {
		rpJmol.requestFocusInWindow();
	}

	@Override
	protected void setViewDir(double[] v) {
			String s = "";
			switch (viewType) {
			case VIEW_TYPE_CHILD_UVW:
				break;
			case VIEW_TYPE_PARENT_UVW:
				break;
			case VIEW_TYPE_CHILD_HKL:
				s = "rotate z -90;";
				break;
			default:
			case VIEW_TYPE_PARENT_HKL:	
				s = "rotate z -90;";
				break;
			}
			s = "reset view; moveto 0 plane {" + v[0] + " " + v[1] + " " + v[2] + " 0};" + s;
			jmolScriptWait(s);		
			rp.centerImage();
	}
	
	private String parentCellScript, childCellScript;
	private String[] jmolCellColors = new String[2];

	private String jmolGetAxisScript(int itype) {
		Cell cell;
		String type;
		switch (itype) {
		case PARENT:
			type = "parent";
			cell = variables.parentCell;
			break;
		default:
		case CHILD:
			type = "child";
			cell = variables.childCell;
			break;
		}
		double d = AXIS_RADII[itype] * 2;
		String script = getJmolDrawAxis(type, cell, 0, d, Color.red)
		 				+ getJmolDrawAxis(type, cell, 1, d, Color.green)
		 				+ getJmolDrawAxis(type, cell, 2, d, Color.blue);
		return script;
	}
	private String getJmolDrawAxis(String type, Cell cell, int i, double d, Color c) {
		
		double[] info = cell.getAxisInfo(i);
		return "draw axis_" + type + "_" + i + " diameter " + d 
				+ " vector " + jmolPoint(info, 6) + jmolPoint(info, 9) 
				+ " color " + getJmolColor(c) +";";
	}

	private String jmolPoint(double[] info, int pt) {
		return "{" + info[pt++] + " " + info[pt++] + " " + info[pt++] + "}";
	}

	private void jmolSetCell(int itype) {
		Cell cell;
		String type;
		String currentCellScript;
		switch (itype) {
		case PARENT:
			type = "parent";
			cell = variables.parentCell;
			currentCellScript = parentCellScript;
			break;
		default:
		case CHILD:
			type = "child";
			cell = variables.childCell;
			currentCellScript = childCellScript;
			break;
		}
		String s = "unitcell [";
		s += jmolPoint(cell.getCartesianVertex(0)) + jmolPoint(cell.getCartesianAxisTemp(1))
				+ jmolPoint(cell.getCartesianAxisTemp(2)) + jmolPoint(cell.getCartesianAxisTemp(4)) + "]; ";
		if (s.equals(currentCellScript))
			return;
		if (itype == PARENT)
			parentCellScript = s;
		else
			childCellScript = s;
		String color = jmolCellColors[itype];
		if (color == null) {
			Color col = (itype == PARENT ? Variables.COLOR_PARENT_CELL : Variables.COLOR_CHILD_CELL);
			color = jmolCellColors[itype] = getJmolColor(col);
		}
		s += "draw cell_" + type + " diameter " + CELL_RADIUS*2 + " unitcell color " + jmolCellColors[itype] + ";";
		s += jmolGetAxisScript(itype);
		viewer.scriptWait(s);
	}
	
	private void jmolInitAxes() {
		String s = jmolGetAxisScript(PARENT) + jmolGetAxisScript(CHILD);
		viewer.scriptWait(s);
	}
	
	/**
	 * Java BitSet to java2script BS
	 * 
	 * @param a
	 * @return
	 */
	private static BS newBS(BitSet a) {
		BS bs = new BS();
		for (int i = a.nextSetBit(0); i >= 0; i = a.nextSetBit(i + 1)) {
			bs.set(i);
		}
		return bs;
	}

	private static String getJmolColor(double[] diff, double[] spec, double[] amb) {
		int r = (int) Math.max(0, Math.min(255, 255 * diff[0]));
		int g = (int) Math.max(0, Math.min(255, 255 * diff[1]));
		int b = (int) Math.max(0, Math.min(255, 255 * diff[2]));
		return "[" + r + " " + g + " " + b + "]";
	}

	private static String getJmolColor(Color col) {
		return "[" + col.getRed() + " " + col.getGreen() + " " + col.getBlue() + "]";
	}



}
