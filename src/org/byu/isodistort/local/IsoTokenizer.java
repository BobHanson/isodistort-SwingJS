package org.byu.isodistort.local;

import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * The IsoTokenizer class reads an ISOVIZ file or a distortion file as a
 * monolithic byte array and then reads through that as a state machine,
 * creating a map of keys and just marking start and stop signals for data
 * items. The data items are not parsed until a request is made for a value.
 * Then they are parsed as int, double, boolean, String, or BitSet 1/0 values.
 * 
 * IsoTokenizer extends TreeMap. This means that it inherits all the fields
 * of TreeMap, such as getKeySet(), and it collects and delivers keys entries in
 * sequential order based on their addition. In this way, it can be used to read
 * and then later write entries in order.
 * 
 * Reading ISOVIZ files:
 * 
 * The first entry in the file should be !isoversion. This may be preceded by
 * comments, though a reader may require the !isoversion key to be within some
 * numbe of bytes from the beginning of the file. (IsoApp allows for 200
 * characters of comments.)
 * 
 * Each data block in an isoviz file starts with a new line followed by '!xxx',
 * where xxx is the "key". Following the key is a whitespace-separated set of
 * values. Double values are simple numbers optionally containing a '.'.
 * Currently no exponential notation is allowed.
 * 
 * Whitespace is generally defined as [SPACE], [TAB], \n, or \r.
 * 
 * Comments start with a '#' and end with a new-line character (\n or \r), and
 * are treated as whitespace.
 * 
 * IsoTokenizer.readBlocks() scans the byte array for keys, values, and
 * whitespace. For each key that it finds that has values, it creates an entry
 * in the blockData map. This integer array has length (nvalues * 2 + 1) and
 * takes the form:
 * 
 * [n, s1,e1, s2,e2, s3,e3, ... sn,en, 0, 0...]
 * 
 * where s1 and e1 are the start and (end + 1) pointers to the byte array, and n
 * is the number of s/e pairs (i.e. the data item count). The presence of an
 * initial count allows for a less efficient but perhaps slightly faster use of
 * arrays that may not have been length-reduced. Thus, the array have one or
 * more trailing zeros.
 * 
 * Empty keys are not recorded.
 * 
 * 
 * Reading DISTORTION files:
 * 
 * Rules for reading are the same as for ISOVIZ files, with two caveats:
 * 
 * 1) If the key ends with "String" (for example, "wyckoffString"), then only \n
 * and \r will be considered to be whitespace. ' ' and '\t' will be considered
 * part of the value.
 * 
 * 2) The syntax
 * 
 * !begin xxx
 * 
 * !isodistortVersion
 * 
 * 6.12.1
 * 
 * ...
 * 
 * !end xxx
 * 
 * will be read by prepending all keys found between begin and end statements
 * with "xxx."
 * 
 * 
 * 
 * Parsing:
 * 
 * Parsing is done on the fly, upon request for a value. No new arrays or
 * buffers are involved. The process starts with a call to setData(key), which
 * sets the parser for a given data block and returns the number of values in
 * that block. Calls are then made to request a single Boolean, int, double,
 * String, for a specified value. In addition, the getBitSet() method will parse
 * for single-character '1' or '0' values. The tokenizer then simply extracts
 * the value from the specified range of bytes in the byte array.
 * 
 * My tests suggest that the simple integer and double parsers provided here are
 * about 4x faster than converting the bytes to a String and then using
 * Integer.parseInt(String) or Double.parseDouble(String).
 * 
 * The following nine public methods are available:
 * 
 * 
 * IsoTokenizer(Object data, int verbosity)
 * 
 * void dispose()
 * 
 * BitSet getBitSet(String key)
 * 
 * byte[] getBytes()
 * 
 * String getCurrentTag()
 * 
 * double getDouble(i)
 * 
 * KeySet<String> getKeySet()
 * 
 * int getInt(i)
 * 
 * String getString(i)
 * 
 * int setData(String key)
 * 
 * 
 * 
 * @author Bob Hanson
 *
 */
@SuppressWarnings("serial")
public class IsoTokenizer extends TreeMap<String, int[]> {

	/**
	 * the file data
	 */
	private byte[] bytes;

	/**
	 * Get the raw byte data.
	 * 
	 * @return the data as a byte array
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * current key after setData(key)
	 */
	private String currentKey;

	/**
	 * current block pointers after setData(key)
	 */
	private int[] currentBlock;

	/**
	 * clear the objects in this class to speed garbage collection
	 */
	public void dispose() {
		bytes = null;
		currentBlock = null;
	}

	public final static int QUIET = 0;
	public final static int DEBUG_LOW = 1;
	public final static int DEBUG_HIGH = 2;

	/**
	 * Initiate the tokenizer.
	 * 
	 * @param data      String, byte[], or InputStream data
	 * @param ignore    semicolon-separated list of blocks to ignore
	 * @param verbosity QUIET (0) no reporting; DEBUG_LOW (1) key and number of
	 *                  values only; DEBUG_HIGH (2) full report for each byte parsed
	 */
	public IsoTokenizer(byte[] data, String ignore, int verbosity) {
		//ignore = null;
		if (ignore == null)
			ignore = ";bondlist;";
		// verbosity = DEBUG_HIGH;
		bytes = data;
		readBlocks(bytes, ignore, this, verbosity);
	}

	/**
	 * Retrieve the pointer array for the given key and set up .
	 * 
	 * @param key
	 * @return
	 */
	public int setData(String key) {
		if (key == currentKey)
			return len();
		currentBlock = null;
		currentKey = null;
		if (!containsKey(key))
			return 0;
		currentKey = key;
		currentBlock = get(key);
		int len = len();
		System.out.println(currentKey + " " + len);
		return len;
	}

	/**
	 * Get the name of the current key, probably for error messages.
	 * 
	 * @return current key
	 */
	public String getCurrentTag() {
		return currentKey;
	}

	/**
	 * Get a string value
	 * 
	 * @param pt zero-based nonnegative index into data values
	 * @return the string, or null if pt past the end of the data
	 */
	public String getString(int pt) {
		return (pt < len() ? asString(bytes, from(pt), to(pt)) : null);
	}

	/**
	 * Get an integer value
	 * 
	 * @param pt zero-based nonnegative index into data values
	 * @return the value, or Integer.MIN_VALUE if this element is not an integer
	 */
	public int getInt(int pt) {
		return (pt < len() ? bytesToInt(pt) : Integer.MIN_VALUE);
	}

	/**
	 * Get an double value
	 * 
	 * @param pt zero-based nonnegative index into data values
	 * @return the value, or Double.NaN if this element is not an integer
	 */
	public double getDouble(int pt) {
		return (pt < len() ? bytesToDouble(pt) : Double.NaN);
	}

	/**
	 * Return a Boolean object (Boolean.TRUE or Boolean.FALSE) determined from
	 * whether the first byte is 't' or 'T', or not.
	 * 
	 * @param i
	 * @return Boolean.TRUE (first byte is 't' or 'T') or Boolean.FALSE (anything
	 *         else)
	 */
	public Boolean getBoolean(int i) {
		if (i >= len())
			return null;
		byte b = bytes[from(i)];
		return (b == 't' || b == 'T' ? Boolean.TRUE : Boolean.FALSE);
	}

	/**
	 * Interpret the data for this key to as a series of 1s and 0s as bits in a
	 * BitSet.
	 * 
	 * @param key
	 * @return a BitSet, or null if the values were not of the form '0' or '1'
	 */
	public BitSet getBitSet(String key) {
		int count = 0;
		BitSet bs = null;
		int n = setData(key);
		if (n > 0) {
			bs = new BitSet();
			for (int i = 0; i < n; i++) {
				if (len(i) != 1) {
					return null; // not a bitset
				}
				switch (bytes[from(i)]) {
				case '0':
					break;
				case '1':
					bs.set(i);
					count++;
					break;
				default:
					return null;
				}
			}
		}
		return (count > 0 ? bs : null);
	}

	/**
	 * Get the number of values in the current data block.
	 * 
	 * @return the first element of the data pointer array or 0 if there is no
	 *         current data.
	 */
	private int len() {
		return (currentBlock == null ? 0 : currentBlock[0]);
	}

	/**
	 * Return the starting byte pointer for the given index value
	 * 
	 * @param i
	 * @return 2 * i + 1
	 */
	private int from(int i) {
		return currentBlock[(i << 1) + 1];
	}

	private int to(int i) {
		return currentBlock[(++i << 1)];
	}

	private int len(int i) {
		return to(i) - from(i);
	}

	private static String asString(byte[] bytes, int from, int to) {
		return new String(bytes, from, to - from); 
	}

	private int bytesToInt(int i) {
		int from = from(i);
		int to = to(i);
		byte b = bytes[from];
		int sign;
		if (b == '-') {
			sign = -1;
			from++;
		} else {
			sign = 1;
		}
		int ival = 0;
		for (i = from; i < to; i++) {
			b = bytes[i];
			if (b < '0' || b > '9')
				return Integer.MIN_VALUE;
			ival = ival * 10 + (b - '0');
		}
		return sign * ival;

	}

	/**
	 * A very simple ASCII-to-double conversion. Just accumulates bytes as an
	 * integer with a left-shift and then divides by the necessary number of powers
	 * of 10. Does NOT allow for exponential notation.
	 * 
	 * Special check for 0 and 0.0 or 0.0000 for faster processing.
	 * 
	 * @param i
	 * @return
	 */
	private double bytesToDouble(int i) {
		int from = from(i);
		byte b = bytes[from];
		int sign = 1;
		switch (b) {
		case '-':
			sign = -1;
			from++;
			break;
		case '0':
			// ignore first leading 0
			from++;
			break;
		}

		// ignore trailing zeros

		int to = to(i);
		int ptDot = -1;
		for (i = to; --i >= from;) {
			switch (bytes[i]) {
			case '.':
				if (ptDot >= 0)
					return Double.NaN;
				ptDot = i;
				// fall through
			case '0':
				to = i;
				break;
			default:
				i = 0;
				break;
			}
		}
		if (from == to)
			return 0;
		if (ptDot > to)
			to = ptDot;

		// now scan what is left of the number
		double dval = 0;
		boolean haveDot = false;
		int tens = 0;
		for (i = from; i < to; i++) {
			b = bytes[i];
			if (b == '.' && !haveDot) {
				haveDot = true;
				continue;
			}
			if (b < '0' || b > '9') {
				return Double.NaN;
			}
			if (haveDot)
				tens++;
			else if (dval == 0 && b == '0')
				continue;
			dval = dval * 10 + (b - '0');
		}
		return (dval == 0 ? 0 : sign * dval * MathUtil.decimalScale[tens]);
	}

	private final static int STATE_NONE = 0;
	private final static int STATE_COMMENT = 1;
	private final static int STATE_EOL = 2;
	private final static int STATE_KEY = 3;
	private final static int STATE_VALUE = 4;
	private final static int STATE_DONE = 5;

	private static void readBlocks(byte[] bytes, String ignore, Map<String, int[]> map, int verbosity) {
		int dataStart = -1;
		int keyStart = -1;
		int lineStart = 0;
		int[] pointers = null; // first point is reserved for count
		int pt = 0;
		int n = bytes.length;
		int state = STATE_EOL;
		String key = null;
		String filePrefix = "";
		boolean isString = false;
		boolean verboseHigh = (verbosity == DEBUG_HIGH);
		int endState = -1;
		boolean ignoringData = false;
		for (int i = 0; i < n; i++) {
			byte b = bytes[i];
//				System.out.println("i=" + i + " b=" + esc(b) + " state=" + state);
			switch (b) {
			case '!':
				if (state == STATE_EOL) {
					state = STATE_KEY;
					keyStart = i + 1;
				}
				continue;
			case ' ':
			case '\t':
				if (isString)
					continue;
				switch (state) {
				case STATE_COMMENT:
					continue;
				case STATE_KEY:
				case STATE_VALUE:
					endState = STATE_NONE;
					break;
				default:
					state = STATE_NONE;
					break;
				}
				break;
			case '\n':
			case '\r':
				if (verboseHigh && lineStart < i) {
					System.out.println(">>" + asString(bytes, lineStart, i));
				}
				lineStart = i + 1;
				isString = false;
				switch (state) {
				case STATE_KEY:
				case STATE_VALUE:
					endState = STATE_EOL;
					break;
				default:
					state = STATE_EOL;
					break;
				}
				break;
			case '#':
				if (isString)
					continue;
				switch (state) {
				case STATE_KEY:
				case STATE_VALUE:
					endState = STATE_COMMENT;
					break;
				default:
					state = STATE_COMMENT;
					break;
				}
				break;
			default:
				switch (state) {
				case STATE_KEY:
					// could check here for proper key alphanumeric syntax
					continue;
				case STATE_EOL:
				case STATE_NONE:
					if (ignoringData)
						continue;
					// start value
					state = STATE_VALUE;
					dataStart = i;
					continue;
				case STATE_VALUE:
					// check for last char in stream closing value
					if (i == n - 1) {
						endState = STATE_DONE;
						i++; // will break out
						break;
					}
					continue;
				default:
					continue;
				}
			}
			if (endState >= 0) {
				// process end-of-key or end-of-value
				switch (state) {
				case STATE_KEY:
					if (keyStart != i) {
						// key must length or it is ignored
						key = asString(bytes, keyStart, i);
						ignoringData = (ignore != null && ignore.indexOf(";" + key + ";")>=0);
						isString = key.endsWith("String");
						if (ignoringData)
							state = STATE_NONE;
					}
					pointers = null;
					break;
				case STATE_VALUE:
					if (key.equals("begin")) {
						filePrefix = asString(bytes, dataStart, i) + ".";
					} else if (key.equals("end")) {
						filePrefix = "";
					} else {
						String filekey = filePrefix + key;
						// check for first value
						if (pointers == null) {
							pointers = new int[65]; // capacity for 32 values
							pt = 1;
							map.put(filekey, pointers);
						}

						if (verboseHigh)
							System.out.println(filekey + "[" + pointers[0] + "](" + dataStart + "," + i + ")=>"
									+ asString(bytes, dataStart, i));

						// check for need a larger array install it
						if (pt + 2 == pointers.length) {
							int[] p = new int[(pointers.length << 1) - 1];
							System.arraycopy(pointers, 0, p, 0, pointers.length);
							pointers = p;
							map.put(filekey, pointers);
						}
						// increment and set pointers
						pointers[0]++;
						pointers[pt++] = dataStart;
						pointers[pt++] = i;
					}
					break;
				}
				// end of key or value
				state = endState;
				endState = -1;
			}
		}
	}

	/**
	 * for debugging only
	 * 
	 * @param b
	 * @return
	 */
	@SuppressWarnings("unused")
	private static String esc(byte b) {
		switch (b) {
		case '\n':
			return "\\n";
		case '\r':
			return "\\r";
		case '\t':
			return "\\t";
		default:
			return " " + (char) b;
		}
	}

	/**
	 * For debugging only
	 * 
	 * @param bytes
	 * @param map
	 */
	private static void dumpMap(byte[] bytes, Map<String, int[]> map) {
		// String s = new String(bytes);
		// System.out.println("String was \n" + s + "<");
		System.out.println("------IsoTokenizer.dumpMap------");
		for (String s1 : map.keySet()) {
			int[] data = map.get(s1);
			for (int i = 1, p = 0; p < data[0]; p++) {
				int a = data[i++];
				int b = data[i++];
				System.out.println(s1 + "[" + p + "]=>" + asString(bytes, a, b) + "<");
			}
			System.out.println("-------------------------------------");
		}
	}

	//// testing
	
	
	/**
	 * Test distortionFile prototype
	 */
	static void testDistort() {

		String s = "!begin distortionFile\r\n" + 
				"#version of isodistort\r\n" + 
				"!isodistortVersion\r\n" + 
				"6.12.1              \r\n#make CIF movie\r\n" + 
				"!makeCIFMovie\r\n" + 
				" F\r\n" + 
				"!end distortionFile\r\n" + 
				"\r\n" + 
				"#modes file\r\n" + 
				"!begin modesFile\r\n" + 
				"#maximum number of atoms in applet for each wyckoff position\r\n" + 
				"!maxAtomsApplet\r\n" + 
				"      12\r\n" + 
				"!end modesFile\r\n" + 
				"";
		IsoTokenizer vt = new IsoTokenizer(s.getBytes(), null, DEBUG_LOW);
		dumpMap(vt.bytes, vt);

	}

	/**
	 * Test numerical parsers.
	 *   
	 */
	static void testViz() {
		String s = "!test1 0 0.0 2.5 -3.2 4.0001 1234567 123456.7890123456 123456789.01234567890123 3.56000000 #3 adf4\n 5 \n\n\n#!test2 testing  \n\n!test3 OK";
		IsoTokenizer vt = new IsoTokenizer(s.getBytes(), null, QUIET);// DEBUG_HIGH);
		dumpMap(vt.bytes, vt);
		int nData = vt.setData("test1");
		for (int i = 0; i < nData; i++) {
			System.out.println(vt.getDouble(i));
		}
		parseSpeedTest(vt);
	}

	/**
	 * Speed test for 1M numerical parsings relative to Integer.parseInt and Double.parseDouble
	 *  
	 * @param vt
	 */
	private static void parseSpeedTest(IsoTokenizer vt) {
		long t = System.currentTimeMillis();

		for (int i = 0; i < 1000000; i++)
			vt.getDouble(6);

		System.out.println("This double parser: " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();

		for (int i = 0; i < 1000000; i++)
			Double.parseDouble(new String(vt.bytes, vt.from(6), vt.to(6) - vt.from(6)));

		System.out.println("Double.parseDouble: " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();

		for (int i = 0; i < 1000000; i++)
			vt.getInt(5);

		System.out.println("This int parser: " + (System.currentTimeMillis() - t));

		t = System.currentTimeMillis();

		for (int i = 0; i < 1000000; i++)
			Integer.parseInt(new String(vt.bytes, vt.from(5), vt.to(5) - vt.from(5)));

		System.out.println("Integer.parseInt: " + (System.currentTimeMillis() - t));
	}

	/**
	 * For debugging.
	 * 
	 * @param args
	 * 
	 * @j2sIgnore
	 */
	public static void main(String args[]) {
		testViz();
		testDistort();
	}


}
