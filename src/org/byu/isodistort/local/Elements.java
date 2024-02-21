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
		
		public static int getCPKColor(String symbol) {
			double[] value = getScatteringFactor(symbol, true);
			return argbsCpk[value == null ? 0 : (int) value[0]];
		}

		// from org.jmol.c.PAL
		private final static int[] argbsCpk = { 0xFFFF1493, // Xx 0
			      0xFFFFFFFF, // H  1
			      0xFFD9FFFF, // He 2
			      0xFFCC80FF, // Li 3
			      0xFFC2FF00, // Be 4
			      0xFFFFB5B5, // B  5
			      0xFF909090, // C  6 - changed from ghemical
			      0xFF3050F8, // N  7 - changed from ghemical
			      0xFFFF0D0D, // O  8
			      0xFF90E050, // F  9 - changed from ghemical
			      0xFFB3E3F5, // Ne 10
			      0xFFAB5CF2, // Na 11
			      0xFF8AFF00, // Mg 12
			      0xFFBFA6A6, // Al 13
			      0xFFF0C8A0, // Si 14 - changed from ghemical
			      0xFFFF8000, // P  15
			      0xFFFFFF30, // S  16
			      0xFF1FF01F, // Cl 17
			      0xFF80D1E3, // Ar 18
			      0xFF8F40D4, // K  19
			      0xFF3DFF00, // Ca 20
			      0xFFE6E6E6, // Sc 21
			      0xFFBFC2C7, // Ti 22
			      0xFFA6A6AB, // V  23
			      0xFF8A99C7, // Cr 24
			      0xFF9C7AC7, // Mn 25
			      0xFFE06633, // Fe 26 - changed from ghemical
			      0xFFF090A0, // Co 27 - changed from ghemical
			      0xFF50D050, // Ni 28 - changed from ghemical
			      0xFFC88033, // Cu 29 - changed from ghemical
			      0xFF7D80B0, // Zn 30
			      0xFFC28F8F, // Ga 31
			      0xFF668F8F, // Ge 32
			      0xFFBD80E3, // As 33
			      0xFFFFA100, // Se 34
			      0xFFA62929, // Br 35
			      0xFF5CB8D1, // Kr 36
			      0xFF702EB0, // Rb 37
			      0xFF00FF00, // Sr 38
			      0xFF94FFFF, // Y  39
			      0xFF94E0E0, // Zr 40
			      0xFF73C2C9, // Nb 41
			      0xFF54B5B5, // Mo 42
			      0xFF3B9E9E, // Tc 43
			      0xFF248F8F, // Ru 44
			      0xFF0A7D8C, // Rh 45
			      0xFF006985, // Pd 46
			      0xFFC0C0C0, // Ag 47 - changed from ghemical
			      0xFFFFD98F, // Cd 48
			      0xFFA67573, // In 49
			      0xFF668080, // Sn 50
			      0xFF9E63B5, // Sb 51
			      0xFFD47A00, // Te 52
			      0xFF940094, // I  53
			      0xFF429EB0, // Xe 54
			      0xFF57178F, // Cs 55
			      0xFF00C900, // Ba 56
			      0xFF70D4FF, // La 57
			      0xFFFFFFC7, // Ce 58
			      0xFFD9FFC7, // Pr 59
			      0xFFC7FFC7, // Nd 60
			      0xFFA3FFC7, // Pm 61
			      0xFF8FFFC7, // Sm 62
			      0xFF61FFC7, // Eu 63
			      0xFF45FFC7, // Gd 64
			      0xFF30FFC7, // Tb 65
			      0xFF1FFFC7, // Dy 66
			      0xFF00FF9C, // Ho 67
			      0xFF00E675, // Er 68
			      0xFF00D452, // Tm 69
			      0xFF00BF38, // Yb 70
			      0xFF00AB24, // Lu 71
			      0xFF4DC2FF, // Hf 72
			      0xFF4DA6FF, // Ta 73
			      0xFF21F4D6, // W  74
			      0xFF267DAB, // Re 75
			      0xFF266696, // Os 76
			      0xFF175487, // Ir 77
			      0xFFD0D0E0, // Pt 78 - changed from ghemical
			      0xFFFFD123, // Au 79 - changed from ghemical
			      0xFFB8B8D0, // Hg 80 - changed from ghemical
			      0xFFA6544D, // Tl 81
			      0xFF575961, // Pb 82
			      0xFF9E4FB5, // Bi 83
			      0xFFAB5C00, // Po 84
			      0xFF754F45, // At 85
			      0xFF428296, // Rn 86
			      0xFF420066, // Fr 87
			      0xFF007D00, // Ra 88
			      0xFF70ABFA, // Ac 89
			      0xFF00BAFF, // Th 90
			      0xFF00A1FF, // Pa 91
			      0xFF008FFF, // U  92
			      0xFF0080FF, // Np 93
			      0xFF006BFF, // Pu 94
			      0xFF545CF2, // Am 95
			      0xFF785CE3, // Cm 96
			      0xFF8A4FE3, // Bk 97
			      0xFFA136D4, // Cf 98
			      0xFFB31FD4, // Es 99
			      0xFFB31FBA, // Fm 100
			      0xFFB30DA6, // Md 101
			      0xFFBD0D87, // No 102
			      0xFFC70066, // Lr 103
			      0xFFCC0059, // Rf 104
			      0xFFD1004F, // Db 105
			      0xFFD90045, // Sg 106
			      0xFFE00038, // Bh 107
			      0xFFE6002E, // Hs 108
			      0xFFEB0026, // Mt 109
			  };

	}	
