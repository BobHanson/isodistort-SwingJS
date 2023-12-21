package org.byu.isodistort.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Hashtable;
import java.util.Map;

/**
 * The VariableTokenier class reads an ISOVIZ file as a monolithic byte array
 * and then reads through that as a state machine, creating a map of keys and
 * just marking start and stop signals for data items. The data items are not
 * parsed until a request is made for a value. Then they are parsed as int,
 * double, boolean, String, or BitSet 1/0 values.
 * 
 * Reading:
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
 * Comments are started with a '#' and end with a new-line character (\n or \r),
 * and are treated as whitespace.
 * 
 * VariableTokenizer.readBlocks() scans the byte array for keys, values, and
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
 * VariableTokenizer(Object data, int verbosity)
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
public class VariableTokenizer {

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
	 * a map relating
	 */
	private Map<String, int[]> blockMap = new Hashtable<>();

	/**
	 * clear the objects in this class to speed garbage collection
	 */
	public void dispose() {
		bytes = null;
		blockMap = null;
		currentBlock = null;
	}

	public final static int QUIET = 0;
	public final static int DEBUG_LOW = 1;
	public final static int DEBUG_HIGH = 2;

	/**
	 * Initiate the tokenizer.
	 * 
	 * @param data      String, byte[], or InputStream data
	 * @param verbosity QUIET (0) no reporting; DEBUG_LOW (1) key and number of
	 *                  values only; DEBUG_HIGH (2) full report for each byte parsed
	 */
	public VariableTokenizer(Object data, int verbosity) {
		if (data instanceof String) {
			bytes = ((String) data).getBytes();
		} else if (data instanceof byte[]) {
			bytes = (byte[]) data;
		} else if (data instanceof InputStream) {
			try {
				bytes = FileUtil.getLimitedStreamBytes((InputStream) data, Integer.MAX_VALUE, true);
			} catch (IOException e) {
				e.printStackTrace();
				bytes = new byte[0];
			}
		}
		readBlocks(bytes, blockMap, verbosity);
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
		currentBlock = blockMap.get(key);
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
		return (pt < len() ? new String(bytes, from(pt), len(pt)) : null);
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

	private boolean containsKey(String key) {
		int[] data = blockMap.get(key);
		return (data != null && data[0] != 0);
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
		return (dval == 0 ? 0 : sign * dval * decimalScale[tens]);
	}

	private final static double[] decimalScale = { 1., //
			0.1, //
			0.01, //
			0.001, //
			0.0001, //
			0.00001, //
			0.000001, //
			0.0000001, //
			0.00000001, //
			0.000000001, //
			0.0000000001, //
			0.00000000001, //
			0.000000000001, //
			0.0000000000001, //
			0.00000000000001, //
			0.000000000000001, //
	};

	private final static int STATE_NONE = 0;
	private final static int STATE_COMMENT = 1;
	private final static int STATE_EOL = 2;
	private final static int STATE_KEY = 3;
	private final static int STATE_VALUE = 4;

	private static void readBlocks(byte[] bytes, Map<String, int[]> map, int verbosity) {
		int dataStart = -1;
		int keyStart = -1;
		int[] pointers = null; // first point is reserved for count
		int pt = 0;
		int n = bytes.length;
		int state = STATE_EOL;
		boolean isEnd = false;
		String key = null;
		boolean isVerbose = (verbosity != QUIET);
		boolean verboseHigh = (verbosity == DEBUG_HIGH);
		for (int i = 0; i < n; i++) {
			byte b = bytes[i];
			if (verboseHigh) {
				System.out.println("i=" + i + " b=" + (char) b + " state=" + state);
			}
			switch (b) {
			case '!':
				if (state == STATE_EOL) {
					state = STATE_KEY;
					keyStart = i + 1;
				}
				continue;
			case ' ':
			case '\t':
				switch (state) {
				case STATE_KEY:
				case STATE_VALUE:
					isEnd = true;
					break;
				}
				break;
			case '\n':
			case '\r':
				switch (state) {
				case STATE_KEY:
				case STATE_VALUE:
					isEnd = true;
					break;
				default:
					state = STATE_EOL;
					break;
				}
				break;
			case '#':
				switch (state) {
				case STATE_KEY:
				case STATE_VALUE:
					isEnd = true;
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
				case STATE_NONE:
					// start value
					state = STATE_VALUE;
					dataStart = i;
					continue;
				case STATE_VALUE:
					// check for last char in stream closing value
					if (i == n - 1) {
						isEnd = true;
						i++; // will break out
						break;
					}
					continue;
				default:
					continue;
				}
			}
			if (isEnd) {
				// process end-of-key or end-of-value
				switch (state) {
				case STATE_KEY:
					if (keyStart != i) {
						// key must length or it is ignored
						key = new String(bytes, keyStart, i - keyStart);
					}
					pointers = null;
					break;
				case STATE_VALUE:
					// check for first value
					if (pointers == null) {
						pointers = new int[65]; // capacity for 32 values
						pt = 1;
						map.put(key, pointers);
					}

					if (isVerbose)
						System.out.println(key + "[" + pointers[0] + "](" + dataStart + "," + i + ")=>"
								+ new String(bytes, dataStart, i - dataStart));

					// check for need a larger array install it
					if (pt + 2 == pointers.length) {
						int[] p = new int[(pointers.length << 1) - 1];
						System.arraycopy(pointers, 0, p, 0, pointers.length);
						pointers = p;
						map.put(key, pointers);
					}
					// increment and set pointers
					pointers[0]++;
					pointers[pt++] = dataStart;
					pointers[pt++] = i;
					break;
				}
				// end of key or value
				state = (b == '#' ? STATE_COMMENT : STATE_NONE);
				isEnd = false;
			}
		}
	}

	/**
	 * For debugging.
	 * 
	 * @param bytes
	 * @param map
	 */
	private static void dumpMap(byte[] bytes, Map<String, int[]> map) {
		// String s = new String(bytes);
		// System.out.println("String was \n" + s + "<");
		for (String s1 : map.keySet()) {
			int[] data = map.get(s1);
			for (int i = 1, p = 0; p < data[0]; p++) {
				int a = data[i++];
				int b = data[i++];
				System.out.println(s1 + "[" + p + "]=>" + new String(bytes, a, b - a) + "<");
			}
		}
	}

	/**
	 * For debugging.
	 * 
	 * @param args
	 * 
	 * @j2sIgnore
	 */
	public static void main(String args[]) {
		String s = "!test1 0 0.0 2.5 -3.2 4.0001 1234567 123456.7890123456 123456789.01234567890123 3.56000000 #3 adf4\n 5 \n\n\n#!test2 testing  \n\n!test3 OK";
		VariableTokenizer vt = new VariableTokenizer(s, QUIET);// DEBUG_HIGH);
		dumpMap(vt.bytes, vt.blockMap);
		int nData = vt.setData("test1");
		for (int i = 0; i < nData; i++) {
			System.out.println(vt.getDouble(i));
		}

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

}
