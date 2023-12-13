package org.byu.isodistort2.local;

import java.util.HashMap;

/*
 * The following classes were created by Andrew Zimmerman on May 25, 2006.
 * Modified by Branton Campbell on May 14, 2009.
 */

	/**
	 * ElementParser stores and access a periodic table with scattering factors.
	 * The parameters passed to each new element are atomic number and the real and imaginary parts of the neutron scat factor.
	 */
	public class ElementParser {	

		private HashMap<String,Element> periodicTable = new HashMap<String,Element>();
		
		public ElementParser() {
			periodicTable.put("h", new Element(1.0, -3.739, 0.0));
			periodicTable.put("he", new Element(2.0, 3.26, 0.0));
			periodicTable.put("li", new Element(3.0, -1.9, 0.0));
			periodicTable.put("be", new Element(4.0, 7.79, 0.0));
			periodicTable.put("b", new Element(5.0, 5.3, -0.213));
			periodicTable.put("c", new Element(6.0, 6.646, 0.0));
			periodicTable.put("n", new Element(7.0, 9.36, 0.0));
			periodicTable.put("o", new Element(8.0, 5.803, 0.0));
			periodicTable.put("f", new Element(9.0, 5.654, 0.0));
			periodicTable.put("ne", new Element(10.0, 4.566, 0.0));
			periodicTable.put("na", new Element(11.0, 3.63, 0.0));
			periodicTable.put("mg", new Element(12.0, 5.375, 0.0));
			periodicTable.put("al", new Element(13.0, 3.449, 0.0));
			periodicTable.put("si", new Element(14.0, 4.1491, 0.0));
			periodicTable.put("p", new Element(15.0, 5.13, 0.0));
			periodicTable.put("s", new Element(16.0, 2.847, 0.0));
			periodicTable.put("cl", new Element(17.0, 9.577, 0.0));
			periodicTable.put("ar", new Element(18.0, 1.909, 0.0));
			periodicTable.put("k", new Element(19.0, 3.67, 0.0));
			periodicTable.put("ca", new Element(20.0, 4.7, 0.0));
			periodicTable.put("sc", new Element(21.0, 12.29, 0.0));
			periodicTable.put("ti", new Element(22.0, -3.438, 0.0));
			periodicTable.put("v", new Element(23.0, -0.3824, 0.0));
			periodicTable.put("cr", new Element(24.0, 3.635, 0.0));
			periodicTable.put("mn", new Element(25.0, -3.73, 0.0));
			periodicTable.put("fe", new Element(26.0, 9.45, 0.0));
			periodicTable.put("co", new Element(27.0, 2.49, 0.0));
			periodicTable.put("ni", new Element(28.0, 10.3, 0.0));
			periodicTable.put("cu", new Element(29.0, 7.718, 0.0));
			periodicTable.put("zn", new Element(30.0, 5.68, 0.0));
			periodicTable.put("ga", new Element(31.0, 7.288, 0.0));
			periodicTable.put("ge", new Element(32.0, 8.185, 0.0));
			periodicTable.put("as", new Element(33.0, 6.58, 0.0));
			periodicTable.put("se", new Element(34.0, 7.97, 0.0));
			periodicTable.put("br", new Element(35.0, 6.795, 0.0));
			periodicTable.put("kr", new Element(36.0, 7.81, 0.0));
			periodicTable.put("rb", new Element(37.0, 7.09, 0.0));
			periodicTable.put("sr", new Element(38.0, 7.02, 0.0));
			periodicTable.put("y", new Element(39.0, 7.75, 0.0));
			periodicTable.put("zr", new Element(40.0, 7.16, 0.0));
			periodicTable.put("nb", new Element(41.0, 7.054, 0.0));
			periodicTable.put("mo", new Element(42.0, 6.715, 0.0));
			periodicTable.put("tc", new Element(43.0, 6.8, 0.0));
			periodicTable.put("ru", new Element(44.0, 7.03, 0.0));
			periodicTable.put("rh", new Element(45.0, 5.88, 0.0));
			periodicTable.put("pd", new Element(46.0, 5.91, 0.0));
			periodicTable.put("ag", new Element(47.0, 5.922, 0.0));
			periodicTable.put("cd", new Element(48.0, 4.87, -0.7));
			periodicTable.put("in", new Element(49.0, 4.065, -0.0539));
			periodicTable.put("sn", new Element(50.0, 6.225, 0.0));
			periodicTable.put("sb", new Element(51.0, 5.57, 0.0));
			periodicTable.put("te", new Element(52.0, 5.8, 0.0));
			periodicTable.put("i", new Element(53.0, 5.28, 0.0));
			periodicTable.put("xe", new Element(54.0, 4.92, 0.0));
			periodicTable.put("cs", new Element(55.0, 5.42, 0.0));
			periodicTable.put("ba", new Element(56.0, 5.07, 0.0));
			periodicTable.put("la", new Element(57.0, 8.24, 0.0));
			periodicTable.put("ce", new Element(58.0, 4.84, 0.0));
			periodicTable.put("pr", new Element(59.0, 4.58, 0.0));
			periodicTable.put("nd", new Element(60.0, 7.69, 0.0));
			periodicTable.put("pm", new Element(61.0, 12.6, 0.0));
			periodicTable.put("sm", new Element(62.0, 0.8, -1.65));
			periodicTable.put("eu", new Element(63.0, 7.22, -1.26));
			periodicTable.put("gd", new Element(64.0, 6.5, -13.82));
			periodicTable.put("tb", new Element(65.0, 7.38, 0.0));
			periodicTable.put("dy", new Element(66.0, 16.9, -0.276));
			periodicTable.put("ho", new Element(67.0, 8.01, 0.0));
			periodicTable.put("er", new Element(68.0, 7.79, 0.0));
			periodicTable.put("tm", new Element(69.0, 7.07, 0.0));
			periodicTable.put("yb", new Element(70.0, 12.43, 0.0));
			periodicTable.put("lu", new Element(71.0, 7.21, 0.0));
			periodicTable.put("hf", new Element(72.0, 7.7, 0.0));
			periodicTable.put("ta", new Element(73.0, 6.91, 0.0));
			periodicTable.put("w", new Element(74.0, 4.86, 0.0));
			periodicTable.put("re", new Element(75.0, 9.2, 0.0));
			periodicTable.put("os", new Element(76.0, 10.7, 0.0));
			periodicTable.put("ir", new Element(77.0, 10.6, 0.0));
			periodicTable.put("pt", new Element(78.0, 9.6, 0.0));
			periodicTable.put("au", new Element(79.0, 7.63, 0.0));
			periodicTable.put("hg", new Element(80.0, 12.692, 0.0));
			periodicTable.put("tl", new Element(81.0, 8.776, 0.0));
			periodicTable.put("pb", new Element(82.0, 9.405, 0.0));
			periodicTable.put("bi", new Element(83.0, 8.532, 0.0));
			periodicTable.put("po", new Element(84.0, 0.0, 0.0));
			periodicTable.put("at", new Element(85.0, 0.0, 0.0));
			periodicTable.put("rn", new Element(86.0, 0.0, 0.0));
			periodicTable.put("fr", new Element(87.0, 0.0, 0.0));
			periodicTable.put("ra", new Element(88.0, 10.0, 0.0));
			periodicTable.put("ac", new Element(89.0, 0.0, 0.0));
			periodicTable.put("th", new Element(90.0, 10.31, 0.0));
			periodicTable.put("pa", new Element(91.0, 9.1, 0.0));
			periodicTable.put("u", new Element(92.0, 8.417, 0.0));
			periodicTable.put("np", new Element(93.0, 10.55, 0.0));
			periodicTable.put("pu", new Element(94.0, 0.0, 0.0));
			periodicTable.put("am", new Element(95.0, 8.3, 0.0));
			periodicTable.put("cm", new Element(96.0, 0.0, 0.0));
			periodicTable.put("bk", new Element(97.0, 0.0, 0.0));
			periodicTable.put("cf", new Element(98.0, 0.0, 0.0));
			periodicTable.put("es", new Element(99.0, 0.0, 0.0));
			periodicTable.put("fm", new Element(100.0, 0.0, 0.0));
			periodicTable.put("md", new Element(101.0, 0.0, 0.0));
			periodicTable.put("no", new Element(102.0, 0.0, 0.0));
		}
		
		public double[] parseElement(String string, boolean isXray) {
			String name = "";
			if (!Character.isLetter(string.charAt(0)))
				return ((Element)periodicTable.get("pu")).getValue(false); // returns zero
			else
				name += Character.toLowerCase(string.charAt(0));
			if (string.length() > 1 && Character.isLetter(string.charAt(1))) {
				name += Character.toLowerCase(string.charAt(1));
			}
			Element element = (Element)periodicTable.get(name);
			if (element == null)
				return ((Element)periodicTable.get("pu")).getValue(false); // returns zero
			return element.getValue(isXray);
		}
		
		/*
		 * The Element class is for use by ElementParser
		 */
		private class Element {
			private double xray, real, imaginary;
			
			public Element(double xray, double real, double imaginary) {
				this.xray = xray;
				this.real = real;
				this.imaginary = imaginary;
			}
			
			public double[] getValue(boolean isXray) {
				double[] value = new double[2];
				if (isXray) {
					value[0] = xray;
					value[1] = 0.0;
				}
				else {
					value[0] = real;
					value[1] = imaginary;
				}
				return value;
			}
		} // end of Element class
		
	} // end of ElementParser class

	
