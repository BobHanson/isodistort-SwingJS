package org.byu.isodistort.local;

import java.io.IOException;
import java.io.InputStream;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * The VariableTokenier class reads an ISOVIZ file as a single byte array and
 * then reads through that, creating a map of keys and just marking start and
 * stop signals for data items. The data items are not parsed until a request is
 * made for a value. Then they are parsed as int, double, boolean, String, or
 * BitSet 1/0 values.
 * 
 * Reading:
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
 * in the blockData String, int[] map. This integer array takes the form:
 * 
 * [n, s1,e1, s2,e2, s3,e3, ... sn,en]
 * 
 * where s1 and e1 are the start and (end + 1) pointers in the byte array where
 * n is the number of s/e pairs (i.e. the data item count)
 * 
 * Parsing:
 * 
 * Parsing is done on the fly, upon request for a value. No new arrays or
 * buffers are involved. The process starts with a call to setData(key), which
 * returns the number of values. Calls are then made to request a single
 * Boolean, int, double, String, for a specified value. In addtion, the
 * getBitSet() method will parse for single-character '1' or '0' values.
 * 
 * @author Bob Hanson
 *
 */
class VariableTokenizer {

	private byte[] bytes;

	public byte[] getBytes() {
		return bytes;
	}

	@SuppressWarnings("unused")
	private boolean isVerbose;

	private String currentKey;
	private int[] currentBlock;

	private Map<String, int[]> blockMap = new TreeMap<>();

	public void dispose() {
		bytes = null;
		blockMap = null;
		currentBlock = null;
	}

	VariableTokenizer(Object data, boolean isVerbose) {
		this.isVerbose = isVerbose;
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
		readBlocks(bytes, blockMap, false);
	}

	/**
	 * Retrieve the pointer array for the given key and set up .
	 * 
	 * @param key
	 * @return
	 */
	int setData(String key) {
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
	String getCurrentTag() {
		return currentKey;
	}

	String getString(int pt) {
		return (len() < pt ? null : (new String(bytes, from(pt), len(pt))));
	}

	/**
	 * Get an integer value
	 * 
	 * @param pt
	 * @return the value or Integer.MIN_VALUE if this element is not an integer
	 */
	int getInt(int pt) {
		return (len() < pt ? Integer.MIN_VALUE : bytesToInt(pt));
	}

	double getDouble(int pt) {
		return (len() < pt ? Double.NaN : bytesToDouble(pt));
	}

	Boolean getBoolean(int pt) {
		if (len() < pt)
			return null;
		byte b = bytes[from(pt)];
		return (b == 't' || b == 'T' ? Boolean.TRUE : Boolean.FALSE);
	}

	BitSet getBitSet(String key) {
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
	 * For debugging.
	 * 
	 * @param bytes
	 * @param map
	 */
	private static void dumpMap(byte[] bytes, Map<String, int[]> map) {
		String s = new String(bytes);
		System.out.println("String was \n" + s + "<");
		for (String s1 : map.keySet()) {
			int[] data = map.get(s1);
			System.out.println(s1 + " -------- data[0] is " + data[0]);
			for (int i = 1, p = 0; p < data[0]; p++) {
				int a = data[i++];
				int b = data[i++];
				System.out.println(s1 + "[" + p + "]=>" + new String(bytes, a, b - a) + "<");
			}
		}
	}

	private static void readBlocks(byte[] bytes, Map<String, int[]> map, boolean isVerbose) {
		int lineStart = -1;
		int whitespace = -1;
		int dataStart = -1;
		int keyStart = -1;
		boolean isComment = false;
		String key = null;
		int[] pointers = null; // first point is reserved for count
		int pt = 0;
		int n = bytes.length;
		boolean isData = false;
		// String s = new String(bytes);
		// System.out.println(s);
		for (int i = 0; i <= n; i++) {
			switch (i == n ? ' ' : bytes[i]) {
			case '!':
				if (isComment)
					continue;
				if (lineStart == i - 1) {
					keyStart = i + 1;
					pointers = null;
					dataStart = -1;
				}
				continue;
			case ' ':
			case '\t':
				if (isComment)
					continue;
				whitespace = i;
				break;
			case '\n':
			case '\r':
				isComment = false;
				lineStart = i;
				whitespace = i;
				break;
			case '#':
				if (isComment)
					continue;
				isComment = true;
				whitespace = i;
			default:
				break;
			}
			if (whitespace == i) {
				// 1) end of key
				// 2) end of data value
				// 3) between key and data
				// 4) continuing whitespace
				if (keyStart >= 0) {
					// 1) end of key
					if (keyStart != i) {
						// key must length or it is ignored
						key = new String(bytes, keyStart, i - keyStart);
						dataStart = i + 1;
						pointers = new int[64];
						map.put(key, pointers);
						pt = 1;
						pointers[pt] = i + 1;
					}
					keyStart = -1;
				} else if (dataStart > 0) {
					if (isData) {
						// 2) end of data value
						if (pt + 3 > pointers.length) {
							int[] p = new int[pt * 2];
							System.arraycopy(pointers, 0, p, 0, pointers.length);
							map.put(key, pointers = p);
						}
						// mark end of data value
						pointers[++pt] = i;
						// increment the length
						pointers[0]++;
						if (isVerbose)
							System.out.println(
									"data is " + pt + " " + new String(bytes, pointers[pt - 1], i - pointers[pt - 1]));

						// initialize a pointer to the next value
						// (will be ignored if this is the end of data)
						pointers[++pt] = dataStart = i + 1;
						isData = false;
					} else {
						// 3) between key and data -- advance start
						pointers[pt] = i + 1;
					}
				} else {
					// 4) continuing whitespace
				}
			} else if (!isComment) {
				if (dataStart > 0)
					// new or continuing data value
					isData = true;
			}
		}
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

	private int from(int pt) {
		return currentBlock[(pt << 1) + 1];
	}

	private int to(int pt) {
		return currentBlock[(++pt << 1)];
	}

	private int len(int pt) {
		return to(pt) - from(pt);
	}

	private boolean containsKey(String key) {
		int[] data = blockMap.get(key);
		return (data != null && data[0] != 0);
	}

	private int bytesToInt(int pt) {
		int from = from(pt);
		byte b = bytes[from];
		int sign;
		if (b == '-') {
			sign = -1;
			from++;
		} else {
			sign = 1;
		}
		int ival = 0;
		for (int i = from, n = to(pt); i < n; i++) {
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
	 * of 10. Does NOT allow for exponential notation. Allows for a maximum of
	 * 2147483647 before dividing by ten. (Could use long here.)
	 * 
	 * 
	 * digit
	 * 
	 * @param pt
	 * @return
	 */
	private double bytesToDouble(int pt) {
		int from = from(pt);
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
		
		int to = to(pt);
		int ptDot = -1;
		for (int i = to; --i >= from;) {
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
		for (int i = from; i < to; i++) {
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

	// "public" methods

	/**
	 * For debugging.
	 * 
	 * @param args
	 */
	public static void main(String args[]) {
		Map<String, int[]> map = new TreeMap<>();
		String s = "!test1 0 0 0 2.5 -3.2 4.0001 123456.7890123456 123456789.01234567890123 3.56000000 #3 adf4\n 5 \n\n\n#!test2 testing  \n\n!test3 OK";
		VariableTokenizer vt = new VariableTokenizer(s, true);
		dumpMap(vt.bytes, map);
		int nData = vt.setData("test1");
		for (int i = 0; i < nData; i++) {
			System.out.println(vt.getDouble(i));
		}
	}

}
