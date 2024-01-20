package org.byu.isodistort.local;

import java.util.HashMap;

/**
 * The following classes were created by Andrew Zimmerman on May 25, 2006.
 * Modified by Branton Campbell on May 14, 2009.
 *
 * 
 * Elements stores and access a periodic table with scattering factors.
 * 
 * The parameters passed to each newElement are atomic number and 0 for xray
 * and the real and imaginary parts of the neutron scat factor for neutron scattering.
 */
	public class Elements {	

		private Elements() {
			// static access only
		}

		private static double[][] newElement(double xray, double real, double imaginary) {
					return new double[][] { new double[] { xray, 0 }, new double[] { real, imaginary } };
		}
			

		private static double[] NO_SUCH_ELEMENT = new double[2];
		
		private static HashMap<String, double[][]> periodicTable = new HashMap<>();

		static {
			
			periodicTable.put("H", newElement(1.0, -3.739, 0.0));
			periodicTable.put("He", newElement(2.0, 3.26, 0.0));
			periodicTable.put("Li", newElement(3.0, -1.9, 0.0));
			periodicTable.put("Be", newElement(4.0, 7.79, 0.0));
			periodicTable.put("B", newElement(5.0, 5.3, -0.213));
			periodicTable.put("C", newElement(6.0, 6.646, 0.0));
			periodicTable.put("N", newElement(7.0, 9.36, 0.0));
			periodicTable.put("O", newElement(8.0, 5.803, 0.0));
			periodicTable.put("F", newElement(9.0, 5.654, 0.0));
			periodicTable.put("Ne", newElement(10.0, 4.566, 0.0));
			periodicTable.put("Na", newElement(11.0, 3.63, 0.0));
			periodicTable.put("Mg", newElement(12.0, 5.375, 0.0));
			periodicTable.put("Al", newElement(13.0, 3.449, 0.0));
			periodicTable.put("Si", newElement(14.0, 4.1491, 0.0));
			periodicTable.put("P", newElement(15.0, 5.13, 0.0));
			periodicTable.put("S", newElement(16.0, 2.847, 0.0));
			periodicTable.put("Cl", newElement(17.0, 9.577, 0.0));
			periodicTable.put("Ar", newElement(18.0, 1.909, 0.0));
			periodicTable.put("K", newElement(19.0, 3.67, 0.0));
			periodicTable.put("Ca", newElement(20.0, 4.7, 0.0));
			periodicTable.put("Sc", newElement(21.0, 12.29, 0.0));
			periodicTable.put("Ti", newElement(22.0, -3.438, 0.0));
			periodicTable.put("V", newElement(23.0, -0.3824, 0.0));
			periodicTable.put("Cr", newElement(24.0, 3.635, 0.0));
			periodicTable.put("Mn", newElement(25.0, -3.73, 0.0));
			periodicTable.put("Fe", newElement(26.0, 9.45, 0.0));
			periodicTable.put("Co", newElement(27.0, 2.49, 0.0));
			periodicTable.put("Ni", newElement(28.0, 10.3, 0.0));
			periodicTable.put("Cu", newElement(29.0, 7.718, 0.0));
			periodicTable.put("Zn", newElement(30.0, 5.68, 0.0));
			periodicTable.put("Ga", newElement(31.0, 7.288, 0.0));
			periodicTable.put("Ge", newElement(32.0, 8.185, 0.0));
			periodicTable.put("As", newElement(33.0, 6.58, 0.0));
			periodicTable.put("Se", newElement(34.0, 7.97, 0.0));
			periodicTable.put("Br", newElement(35.0, 6.795, 0.0));
			periodicTable.put("Kr", newElement(36.0, 7.81, 0.0));
			periodicTable.put("Rb", newElement(37.0, 7.09, 0.0));
			periodicTable.put("Sr", newElement(38.0, 7.02, 0.0));
			periodicTable.put("Y", newElement(39.0, 7.75, 0.0));
			periodicTable.put("Zr", newElement(40.0, 7.16, 0.0));
			periodicTable.put("Nb", newElement(41.0, 7.054, 0.0));
			periodicTable.put("Mo", newElement(42.0, 6.715, 0.0));
			periodicTable.put("Tc", newElement(43.0, 6.8, 0.0));
			periodicTable.put("Ru", newElement(44.0, 7.03, 0.0));
			periodicTable.put("Rh", newElement(45.0, 5.88, 0.0));
			periodicTable.put("Pd", newElement(46.0, 5.91, 0.0));
			periodicTable.put("Ag", newElement(47.0, 5.922, 0.0));
			periodicTable.put("Cd", newElement(48.0, 4.87, -0.7));
			periodicTable.put("In", newElement(49.0, 4.065, -0.0539));
			periodicTable.put("Sn", newElement(50.0, 6.225, 0.0));
			periodicTable.put("Sb", newElement(51.0, 5.57, 0.0));
			periodicTable.put("Te", newElement(52.0, 5.8, 0.0));
			periodicTable.put("I", newElement(53.0, 5.28, 0.0));
			periodicTable.put("Xe", newElement(54.0, 4.92, 0.0));
			periodicTable.put("Cs", newElement(55.0, 5.42, 0.0));
			periodicTable.put("Ba", newElement(56.0, 5.07, 0.0));
			periodicTable.put("La", newElement(57.0, 8.24, 0.0));
			periodicTable.put("Ce", newElement(58.0, 4.84, 0.0));
			periodicTable.put("Pr", newElement(59.0, 4.58, 0.0));
			periodicTable.put("Nd", newElement(60.0, 7.69, 0.0));
			periodicTable.put("Pm", newElement(61.0, 12.6, 0.0));
			periodicTable.put("Sm", newElement(62.0, 0.8, -1.65));
			periodicTable.put("Eu", newElement(63.0, 7.22, -1.26));
			periodicTable.put("Gd", newElement(64.0, 6.5, -13.82));
			periodicTable.put("Tb", newElement(65.0, 7.38, 0.0));
			periodicTable.put("Dy", newElement(66.0, 16.9, -0.276));
			periodicTable.put("Ho", newElement(67.0, 8.01, 0.0));
			periodicTable.put("Er", newElement(68.0, 7.79, 0.0));
			periodicTable.put("Tm", newElement(69.0, 7.07, 0.0));
			periodicTable.put("Yb", newElement(70.0, 12.43, 0.0));
			periodicTable.put("Lu", newElement(71.0, 7.21, 0.0));
			periodicTable.put("Hf", newElement(72.0, 7.7, 0.0));
			periodicTable.put("Ta", newElement(73.0, 6.91, 0.0));
			periodicTable.put("W", newElement(74.0, 4.86, 0.0));
			periodicTable.put("Re", newElement(75.0, 9.2, 0.0));
			periodicTable.put("Os", newElement(76.0, 10.7, 0.0));
			periodicTable.put("Ir", newElement(77.0, 10.6, 0.0));
			periodicTable.put("Pt", newElement(78.0, 9.6, 0.0));
			periodicTable.put("Au", newElement(79.0, 7.63, 0.0));
			periodicTable.put("Hg", newElement(80.0, 12.692, 0.0));
			periodicTable.put("Tl", newElement(81.0, 8.776, 0.0));
			periodicTable.put("Pb", newElement(82.0, 9.405, 0.0));
			periodicTable.put("Bi", newElement(83.0, 8.532, 0.0));
			periodicTable.put("Po", newElement(84.0, 0.0, 0.0));
			periodicTable.put("At", newElement(85.0, 0.0, 0.0));
			periodicTable.put("Rn", newElement(86.0, 0.0, 0.0));
			periodicTable.put("Fr", newElement(87.0, 0.0, 0.0));
			periodicTable.put("Ra", newElement(88.0, 10.0, 0.0));
			periodicTable.put("Ac", newElement(89.0, 0.0, 0.0));
			periodicTable.put("Th", newElement(90.0, 10.31, 0.0));
			periodicTable.put("Pa", newElement(91.0, 9.1, 0.0));
			periodicTable.put("U", newElement(92.0, 8.417, 0.0));
			periodicTable.put("Np", newElement(93.0, 10.55, 0.0));
			periodicTable.put("Pu", newElement(94.0, 0.0, 0.0));
			periodicTable.put("Am", newElement(95.0, 8.3, 0.0));
			periodicTable.put("Cm", newElement(96.0, 0.0, 0.0));
			periodicTable.put("Bk", newElement(97.0, 0.0, 0.0));
			periodicTable.put("Cf", newElement(98.0, 0.0, 0.0));
			periodicTable.put("Es", newElement(99.0, 0.0, 0.0));
			periodicTable.put("Fm", newElement(100.0, 0.0, 0.0));
			periodicTable.put("Md", newElement(101.0, 0.0, 0.0));
			periodicTable.put("No", newElement(102.0, 0.0, 0.0));
		}
		
		public static double[] getScatteringFactor(String symbol, boolean isXray) {
			double[][] value = periodicTable.get(symbol);
			return (value == null ? NO_SUCH_ELEMENT : value[isXray ? 0 : 1]);
		}	
	}	
