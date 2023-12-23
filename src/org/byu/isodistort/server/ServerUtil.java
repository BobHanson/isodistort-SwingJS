package org.byu.isodistort.server;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.Timer;

import org.byu.isodistort.local.FileUtil;
import org.byu.isodistort.local.IsoApp;

import javajs.http.HttpClient;
import javajs.http.HttpClient.HttpRequest;
import javajs.http.HttpClient.HttpResponse;
import javajs.http.HttpClientFactory;
import javajs.util.JSJSONParser;

/**
 * The ServerUtil class handles all transactions with the iso.byu server and
 * maintains a few useful utility methods.
 * 
 * Public methods include:
 * 
 * public static boolean fetch(IsoApp app, int type, Map<String, Object>
 * mapFormData, Consumer<byte[]> consumer, int delay)
 * 
 * This is the main method for initiating a transaction. All transactions are asynchronous, 
 * utilizing java.util.function.Consumer to effect a callback to the app.
 * 
 * 
 * public static Map<String, Object> json2Map(Object formData, boolean asClone)
 * 
 * This method ensures that maps from JavaScrpt and Java, might be in the form of 
 * actual Java HashMap or LinkedHashMap are compatible with JavaScript's simple associative
 * array idea. 
 * 
 * 
 * 
 * 
 * @author Bob Hanson
 *
 */
public class ServerUtil {

	private ServerUtil() {
		// no instance; static only
	}
	// bh test platform "https://jmol.stolaf.edu/jmol/test/t.php";
	final static String publicServerURL = "https://iso.byu.edu/iso/";
	final static String testServerURL = "https://isotest.byu.edu/iso/";

	/**
	 * Fetch a result from the server. This method handles all such requests.
	 */

	public static boolean fetch(IsoApp app, int type, Map<String, Object> mapFormData, Consumer<byte[]> consumer, int delay) {

		boolean testing = false;

		byte[] fileData = (byte[]) mapFormData.remove("toProcess");
		String fileName = (String) mapFormData.remove("fileName");
		String service = (String) mapFormData.remove("_service");

		if (service == null) {
			switch (type) {
			case FileUtil.FILE_TYPE_DISTORTION:
				service = "isodistortuploadfile.php";
				break;
			case FileUtil.FILE_TYPE_ISOVIZ:
				service = "isodistortform.php";
				break;
			default:
				return false;
			}
		}

		String url = (testing ? testServerURL : publicServerURL) + service;
		System.out.println("ServerUtil.fetch " + url + " " + mapFormData.get("origintype"));
		new Thread(() -> {
			try {
				URI uri = new URI(url);
				HttpClient client = HttpClientFactory.getClient(null);
				HttpRequest request = client.post(uri);
				if (fileData == null)
					request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				request.addHeader("Accept", "text/plain, */*; q=0.01");
				for (Entry<String, Object> e : mapFormData.entrySet()) {
					request.addFormPart(e.getKey(), e.getValue().toString());
					// System.out.println(e.getKey() + " = " + e.getValue());
				}
				if (fileData != null)
					request.addFilePart("toProcess", new ByteArrayInputStream(fileData), "text/plain",
							(fileName == null ? "iso.txt" : fileName));

				HttpResponse r = request.execute();
				byte[] bytes = FileUtil.getLimitedStreamBytes(r.getContent(), Integer.MAX_VALUE, true);
				// temporary fix for garbage in wyck line
				cleanBytes(bytes);
				//System.out.println(new String(bytes));
				System.out.println("ServerUtil.fetch received " + bytes.length + " bytes");

				if (bytes.length > 100 && bytes.length < 1000) {
					ServerUtil.getTempFile(app, type, bytes, consumer, delay);
				} else {
					consumer.accept(bytes);
				}
			} catch (Exception e) {
				e.printStackTrace();
				consumer.accept(null);
			}
		}, "serverUtil_fetch").start();
		return true;
	}

	private static void cleanBytes(byte[] bytes) {
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			if (b != 9 && b < 0x0A || b > 127)
				bytes[i] = ' ';
		}
	}

	private final static byte[] SET_TIMEOUT = "setTimeout".getBytes();
	
	private static void getTempFile(IsoApp app, int type, byte[] bytes, Consumer<byte[]> consumer, int delay) {

// about 340 bytes:
//
//		<html><head><title>ISODISTORT</title></head>
//		<BODY BGCOLOR="#FFFFFF" onload="setTimeout ('document.forms[0].submit()', 1000)">
//		<FORM ACTION="isodistortform.php" METHOD="POST">
//		<INPUT TYPE="hidden" NAME="input" VALUE="distort">
//		<INPUT TYPE="hidden" NAME="origintype" VALUE="dfile">
//		<INPUT TYPE="hidden" NAME="filename" VALUE="/tmp/pwd_public/isodistort_67727.iso">
//		</FORM>
//		</BODY>
//		</HTML>
		
		if (!bytesContain(bytes, SET_TIMEOUT)) {
			consumer.accept(bytes);
			return;
		}
		String html = new String(bytes); 
		Map<String, Object> map = scrapeHTML(html);
		String service = getHTMLAttr(html, "ACTION");
		if (map == null || service == null) {
			throw new RuntimeException("ServerUtil.getTempFile cannot process server html: " + html);
		}
		System.out.println("ServerUtil.getTempFile " + map.get("filename") + " delay " + delay + " ms");
		map.put("_service", service);
		
		Timer tempFileTimer = new Timer(1000, new ActionListener() {

			/**
			 * this will go 1, 4, 16, 64, 256, then 512, 1024, 2048, etc. ms
			 */
			@Override
			public void actionPerformed(ActionEvent e) {
				fetch(app, type, map, consumer, (delay < 500 ? (delay << 2) : (delay << 1)));
			}
		});
		tempFileTimer.setRepeats(false);
		tempFileTimer.start();
	}

	private static boolean bytesContain(byte[] bytes, byte[] b) {

		int nb = bytes.length, n = b.length;
		if (nb < n || nb == 0) {
			return false;
		}
		byte b0 = b[0];
		int i0 = 0;
		int i1 = nb - n;   
		// 012345678901
		// ......abc...
		//0......ababc..
		// ....... ^ pt = 2, i0 = 
		for (int pt = 0, i = 0; i - pt <= i1; i++) {
			if (bytes[i] == b[pt++]) {
				if (pt == n)
					return true;
				if (i0 == 0 && bytes[i + 1] == b0){
					i0 = i + 1;
				}
				continue;
			} 
			if (i0 > 0) {
				i = i0 - 1;
				i0 = 0;
			}
			pt = 0;
		}
		return false;
	}

	/**
	 * Conver the form data into a Map if it is a String, or just return it if it is
	 * not.
	 * 
	 * @param formData
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> json2Map(Object formData, boolean asClone) {

		if (formData instanceof Map) {
			return (Map<String, Object>) (asClone ? ((HashMap<String, Object>) formData).clone() : formData);
		}
		if (!(formData instanceof String)) {
			/**
			 * must be JSON; just deal with it as a string
			 * 
			 * @j2sNative
			 * 
			 * 			formDataStr = JSON.stringify(formDataStr);
			 * 
			 */
		}
		return new JSJSONParser().parseMap(formData.toString(), false);
	}

	/**
	 * A simple JSON producer. 
	 * 
	 * @param map
	 * @return
	 */
	public static String toJSON(Map<String, Object> map) {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		String sep = "";
		for (String key : map.keySet()) {
			sb.append(sep).append(key + ":\"" + map.get(key) + "\"\n");
			sep = ",";
		}
		sb.append("}");
		return sb.toString();
	}

	/**
	 * Extract all the INPUT data from the HTML page.
	 * 
	 * @param html
	 * @return
	 */
	public static Map<String, Object> scrapeHTML(String html) {
		Map<String, Object> map = new LinkedHashMap<>();

		// <FORM...> ...... </FORM>
		html = getInnerHTML(html, "FORM");
		if (html == null)
			return null;
		String[] inputs = html.replace("<input","<INPUT").split("<INPUT");
		for (int i = 1; i < inputs.length; i++) {
			addEntry(map, inputs[i]);
		}
		return map;
	}

	private static String getInnerHTML(String html, String tag) {
		String[] parts = html.split(tag.toUpperCase());
		return (parts.length > 2 ? parts[1] : null);
	}

	private static void addEntry(Map<String, Object> map, String line) {
		String type = getHTMLAttr(line, "TYPE");
		if (type == null)
			return;
		switch (type) {
		case "radio":
		case "checkbox":
			if (line.indexOf("CHECKED") < 0)
				return;
			break;
		case "text":
		case "hidden":
			break;
		default:
			return;
		}
		String value = getHTMLAttr(line, "VALUE");
		String name = getHTMLAttr(line, "NAME");
		map.put(name, value);
	}

	private static String getHTMLAttr(String line, String attr) {
		String key = attr + "=\"";
		int pt = line.indexOf(key);
		if (pt < 0)
			pt = line.indexOf(key.toLowerCase());

		if (pt < 0 || (pt = pt + key.length()) > line.length())
			return null;
		int pt1 = line.indexOf("\"", pt);
		return (pt1 < 0 ? null : line.substring(pt, pt1).trim());
	}


//	public final static String testFormData = "{\r\n" + "  \"input\": \"displaydistort\",\r\n"
//			+ "  \"spacegroup\": \"221 Pm-3m      Oh-1\",\r\n" + "  \"settingaxesm\": \"a(b)c               \",\r\n"
//			+ "  \"settingcell\": \"1\",\r\n" + "  \"settingorigin\": \"2\",\r\n" + "  \"settingaxesh\": \"h\",\r\n"
//			+ "  \"settingaxeso\": \"abc                 \",\r\n" + "  \"settingssg\": \"standard\",\r\n"
//			+ "  \"parentsetting\": \"323\",\r\n" + "  \"parentsettingstring\": \"\",\r\n"
//			+ "  \"lattparam\": \"a=4.2\",\r\n"
//			+ "  \"dlattparam\": \"   4.2000000000000002        4.2000000000000002        4.2000000000000002        90.000000000000000        90.000000000000000        90.000000000000000\",\r\n"
//			+ "  \"wycount\": \"  3\",\r\n" + "  \"wypointer001\": \"1627\",\r\n" + "  \"wynumber001\": \" 2\",\r\n"
//			+ "  \"wytype001\": \" 1\",\r\n" + "  \"wyckoff001\": \"1b (1/2,1/2,1/2)\",\r\n"
//			+ "  \"wyatom001\": \"Sr\",\r\n" + "  \"wyatomtype001\": \"Sr\",\r\n"
//			+ "  \"wyparam001\": \"   0.0000000000000000        0.0000000000000000        0.0000000000000000\",\r\n"
//			+ "  \"wyocc001\": \"   1.0000000000000000\",\r\n" + "  \"wypointer002\": \"1626\",\r\n"
//			+ "  \"wynumber002\": \" 1\",\r\n" + "  \"wytype002\": \" 2\",\r\n"
//			+ "  \"wyckoff002\": \"1a (0,0,0)\",\r\n" + "  \"wyatom002\": \"Ti\",\r\n"
//			+ "  \"wyatomtype002\": \"Ti\",\r\n"
//			+ "  \"wyparam002\": \"   0.0000000000000000        0.0000000000000000        0.0000000000000000\",\r\n"
//			+ "  \"wyocc002\": \"   1.0000000000000000\",\r\n" + "  \"wypointer003\": \"1629\",\r\n"
//			+ "  \"wynumber003\": \" 4\",\r\n" + "  \"wytype003\": \" 3\",\r\n"
//			+ "  \"wyckoff003\": \"3d (1/2,0,0)\",\r\n" + "  \"wyatom003\": \"O\",\r\n"
//			+ "  \"wyatomtype003\": \"O\",\r\n"
//			+ "  \"wyparam003\": \"   0.0000000000000000        0.0000000000000000        0.0000000000000000\",\r\n"
//			+ "  \"wyocc003\": \"   1.0000000000000000\",\r\n" + "  \"includedisplacive001\": \"true\",\r\n"
//			+ "  \"includedisplacive002\": \"true\",\r\n" + "  \"includedisplacive003\": \"true\",\r\n"
//			+ "  \"includestrain\": \"true\",\r\n" + "  \"irrepcount\": \"1\",\r\n"
//			+ "  \"kvec1\": \"R, k13 (1/2,1/2,1/2)\",\r\n" + "  \"kvecnumber1\": \"  5\",\r\n"
//			+ "  \"kparam1\": \"    0    0    0    1\",\r\n" + "  \"nmodstar1\": \"0\",\r\n"
//			+ "  \"irrep1\": \"R4+, k13t9\",\r\n" + "  \"irrpointer1\": \" 9807\",\r\n"
//			+ "  \"isofilename\": \"            \",\r\n"
//			+ "  \"orderparam\": \"P1      (a,0,0) 140 I4/mcm, basis={(1,1,0),(-1,1,0),(0,0,2)}, origin=(0,0,0), s=2, i=6, k-active= (1/2,1/2,1/2)\",\r\n"
//			+ "  \"isosubgroup\": \"12431\",\r\n" + "  \"subgroupsym\": \"   140\",\r\n"
//			+ "  \"subgroupsetting\": \" 225\",\r\n" + "  \"subgroupsettingstring\": \"\",\r\n"
//			+ "  \"modesfilename\": \"isodistort_04491.iso \",\r\n"
//			+ "  \"atomsfilename\": \"                     \",\r\n"
//			+ "  \"lattparamsubstring\": \"a=5.93970, b=5.93970, c=8.40000, alpha=90.00000, beta=90.00000, gamma=90.00000\",\r\n"
//			+ "  \"origintype\": \"isovizdistortion\",\r\n" + "  \"\": \"OK\",\r\n" + "  \"inputvalues\": \"true\",\r\n"
//			+ "  \"mode003001\": \" 0.00000\",\r\n" + "  \"strain1\": \" 0.00000\",\r\n"
//			+ "  \"strain2\": \" 0.00000\",\r\n" + "  \"atomicradius\": \"0.400\",\r\n"
//			+ "  \"bondlength\": \"2.500\",\r\n" + "  \"appletwidth\": \" 1024\",\r\n"
//			+ "  \"supercellxmin\": \"0.000\",\r\n" + "  \"supercellxmax\": \"1.000\",\r\n"
//			+ "  \"supercellymin\": \"0.000\",\r\n" + "  \"supercellymax\": \"1.000\",\r\n"
//			+ "  \"supercellzmin\": \"0.000\",\r\n" + "  \"supercellzmax\": \"1.000\",\r\n"
//			+ "  \"modeamplitude\": \"1.000\",\r\n" + "  \"strainamplitude\": \"0.100\",\r\n"
//			+ "  \"settingwrt\": \"parent\",\r\n" + "  \"basist11\": \"2 \",\r\n" + "  \"basist12\": \"0\",\r\n"
//			+ "  \"basist13\": \"0\",\r\n" + "  \"basist21\": \"0\",\r\n" + "  \"basist22\": \"2 \",\r\n"
//			+ "  \"basist23\": \"0\",\r\n" + "  \"basist31\": \"0\",\r\n" + "  \"basist32\": \"0\",\r\n"
//			+ "  \"basist33\": \"2 \",\r\n" + "  \"origint1\": \"0\",\r\n" + "  \"origint2\": \"0\",\r\n"
//			+ "  \"origint3\": \"0\",\r\n" + "  \"ampmincifmovie\": \"0\",\r\n" + "  \"ampmaxcifmovie\": \"1\",\r\n"
//			+ "  \"nframescifmovie\": \"10\",\r\n" + "  \"varcifmovie\": \"linear\",\r\n"
//			+ "  \"periodscifmovie\": \"1\"\r\n" + "}";

	// gives

	// ...
//	#irrepnum/irreplabel_for_each_contributing_irrep 
//	!irreplist 
//	  1 GM1+     
//	  2 GM5+     
//	  3 GM4-     
//	  4 R4+      
//
//	#strainmodenum/amp/maxamp/irrepnum/modelabel/modevector_for_each_mode 
//	!strainmodelist 
//	  1    0.00000   0.10000    1 GM1+strain(a) 
//	   0.57735   0.57735   0.57735   0.00000   0.00000   0.00000 
//	  2    0.00000   0.10000    2 GM5+strain(a) 
//	  -0.00000  -0.00000  -0.00000   0.81650  -0.81650  -0.81650 
//
//	#parentatom/dispmodenum/amp/maxamp/irrepnum/modelabel/(modevector_for_each_subatom)_for_each_mode 
//	!displacivemodelist 
//	    1    1   0.00000   1.41421    3 GM4-[Sr:b:dsp]T1u(a) 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	    2    1   0.10000   1.41421    3 GM4-[Ti:a:dsp]T1u(a) 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	   0.00000   0.00000  -0.04860 
//	    3    1  -0.05010   2.44949    3 GM4-[O:d:dsp]A2u(a) 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	   0.06480   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240   0.03240  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	  -0.03240  -0.06480  -0.01620 
//	    3    2   0.00851   2.44949    3 GM4-[O:d:dsp]Eu(a) 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	  -0.04582  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291  -0.02291  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	   0.02291   0.04582  -0.02291 
//	    3    3  -0.00007   2.44949    4 R4+[O:d:dsp]Eu(a) 
//	   0.00000   0.06873   0.00000 
//	   0.00000  -0.06873   0.00000 
//	   0.00000  -0.06873   0.00000 
//	   0.00000   0.06873   0.00000 
//	   0.00000   0.06873   0.00000 
//	   0.00000   0.06873   0.00000 
//	   0.00000  -0.06873   0.00000 
//	   0.00000  -0.06873   0.00000 
//	   0.06873   0.06873   0.00000 
//	  -0.06873  -0.06873   0.00000 
//	  -0.06873  -0.06873   0.00000 
//	   0.06873   0.06873   0.00000 
//	   0.06873   0.06873   0.00000 
//	  -0.06873  -0.06873   0.00000 
//	  -0.06873   0.00000   0.00000 
//	   0.06873   0.00000   0.00000 
//	   0.06873   0.00000   0.00000 
//	   0.06873   0.00000   0.00000 
//	  -0.06873   0.00000   0.00000 
//	  -0.06873   0.00000   0.00000 
//	  -0.06873   0.00000   0.00000 
//	   0.06873   0.00000   0.00000 
//
//	"
//

//	<INPUT TYPE="hidden" NAME="input" VALUE="displaydistort">
//	<INPUT TYPE="hidden" NAME="spacegroup" VALUE="221 Pm-3m      Oh-1">
//	<INPUT TYPE="hidden" NAME="settingaxesm" VALUE="a(b)c               ">
//	<INPUT TYPE="hidden" NAME="settingcell" VALUE="1">
//	<INPUT TYPE="hidden" NAME="settingorigin" VALUE="2">
//	<INPUT TYPE="hidden" NAME="settingaxesh" VALUE="h">
//	<INPUT TYPE="hidden" NAME="settingaxeso" VALUE="abc                 ">
//	<INPUT TYPE="hidden" NAME="settingssg" VALUE="standard">
//	<INPUT TYPE="hidden" NAME="parentsetting" VALUE="323">
//	<INPUT TYPE="hidden" NAME="parentsettingstring" VALUE="">
//	<INPUT TYPE="hidden" NAME="lattparam" VALUE="a=4.2">
//	<INPUT TYPE="hidden" NAME="dlattparam" VALUE="   4.2000000000000002        4.2000000000000002        4.2000000000000002        90.000000000000000        90.000000000000000        90.000000000000000">
//	<INPUT TYPE="hidden" NAME="wycount" VALUE="  3">
//	<INPUT TYPE="hidden" NAME="wypointer001" VALUE="1627">
//	<INPUT TYPE="hidden" NAME="wynumber001" VALUE=" 2">
//	<INPUT TYPE="hidden" NAME="wytype001" VALUE=" 1">
//	<INPUT TYPE="hidden" NAME="wyckoff001" VALUE="1b (1/2,1/2,1/2)">
//	<INPUT TYPE="hidden" NAME="wyatom001" VALUE="Sr">
//	<INPUT TYPE="hidden" NAME="wyatomtype001" VALUE="Sr">
//	<INPUT TYPE="hidden" NAME="wyparam001" VALUE="   0.0000000000000000        0.0000000000000000        0.0000000000000000">
//	<INPUT TYPE="hidden" NAME="wyocc001" VALUE="   1.0000000000000000">
//	<INPUT TYPE="hidden" NAME="wypointer002" VALUE="1626">
//	<INPUT TYPE="hidden" NAME="wynumber002" VALUE=" 1">
//	<INPUT TYPE="hidden" NAME="wytype002" VALUE=" 2">
//	<INPUT TYPE="hidden" NAME="wyckoff002" VALUE="1a (0,0,0)">
//	<INPUT TYPE="hidden" NAME="wyatom002" VALUE="Ti">
//	<INPUT TYPE="hidden" NAME="wyatomtype002" VALUE="Ti">
//	<INPUT TYPE="hidden" NAME="wyparam002" VALUE="   0.0000000000000000        0.0000000000000000        0.0000000000000000">
//	<INPUT TYPE="hidden" NAME="wyocc002" VALUE="   1.0000000000000000">
//	<INPUT TYPE="hidden" NAME="wypointer003" VALUE="1629">
//	<INPUT TYPE="hidden" NAME="wynumber003" VALUE=" 4">
//	<INPUT TYPE="hidden" NAME="wytype003" VALUE=" 3">
//	<INPUT TYPE="hidden" NAME="wyckoff003" VALUE="3d (1/2,0,0)">
//	<INPUT TYPE="hidden" NAME="wyatom003" VALUE="O">
//	<INPUT TYPE="hidden" NAME="wyatomtype003" VALUE="O">
//	<INPUT TYPE="hidden" NAME="wyparam003" VALUE="   0.0000000000000000        0.0000000000000000        0.0000000000000000">
//	<INPUT TYPE="hidden" NAME="wyocc003" VALUE="   1.0000000000000000">
//	<INPUT TYPE="hidden" NAME="includedisplacive001" VALUE="true">
//	<INPUT TYPE="hidden" NAME="includedisplacive002" VALUE="true">
//	<INPUT TYPE="hidden" NAME="includedisplacive003" VALUE="true">
//	<INPUT TYPE="hidden" NAME="includestrain" VALUE="true">
//	<INPUT TYPE="hidden" NAME="irrepcount" VALUE="1">
//	<INPUT TYPE="hidden" NAME="kvec1" VALUE="R, k13 (1/2,1/2,1/2)">
//	<INPUT TYPE="hidden" NAME="kvecnumber1" VALUE="  5">
//	<INPUT TYPE="hidden" NAME="kparam1" VALUE="    0    0    0    1">
//	<INPUT TYPE="hidden" NAME="nmodstar1" VALUE="0">
//	<INPUT TYPE="hidden" NAME="irrep1" VALUE="R4+, k13t9">
//	<INPUT TYPE="hidden" NAME="irrpointer1" VALUE=" 9807">
//	<INPUT TYPE="hidden" NAME="isofilename" VALUE="            ">
//	<INPUT TYPE="hidden" NAME="orderparam" VALUE="P1      (a,0,0) 140 I4/mcm, basis={(1,1,0),(-1,1,0),(0,0,2)}, origin=(0,0,0), s=2, i=6, k-active= (1/2,1/2,1/2)">
//	<INPUT TYPE="hidden" NAME="isosubgroup" VALUE="12431">
//	<INPUT TYPE="hidden" NAME="subgroupsym" VALUE="   140">
//	<INPUT TYPE="hidden" NAME="subgroupsetting" VALUE=" 225">
//	<INPUT TYPE="hidden" NAME="subgroupsettingstring" VALUE="">
//	<INPUT TYPE="hidden" NAME="modesfilename" VALUE="isodistort_04491.iso ">
//	<INPUT TYPE="hidden" NAME="atomsfilename" VALUE="                     ">
//	<INPUT TYPE="hidden" NAME="lattparamsubstring" VALUE="a=5.93970, b=5.93970, c=8.40000, alpha=90.00000, beta=90.00000, gamma=90.00000">
//	<INPUT TYPE="radio" NAME="origintype" VALUE="isovizdistortion" CHECKED> Save interactive distortion
//	<INPUT TYPE="radio" NAME="origintype" VALUE="isovizdiffraction"> Save interactive diffraction
//	<INPUT TYPE="radio" NAME="origintype" VALUE="structurefile"> CIF file
//	<INPUT TYPE="radio" NAME="origintype" VALUE="distortionfile"> Distortion file
//	<INPUT TYPE="radio" NAME="origintype" VALUE="domains"> Domains
//	<INPUT TYPE="radio" NAME="origintype" VALUE="primary"> Primary order parameters
//	<INPUT TYPE="radio" NAME="origintype" VALUE="modesdetails"> Modes details
//	<INPUT TYPE="radio" NAME="origintype" VALUE="completemodesdetails"> Complete modes details
//	<INPUT TYPE="radio" NAME="origintype" VALUE="topas"> TOPAS.STR
//	<INPUT TYPE="radio" NAME="origintype" VALUE="fullprof"> FULLPROF.pcr
//	<INPUT TYPE="radio" NAME="origintype" VALUE="irreps"> IR matrices
//	<INPUT TYPE="radio" NAME="origintype" VALUE="tree"> Subgroup tree
//	<INPUT CLASS="btn btn-primary" TYPE="submit" VALUE="OK"><p>
//	<INPUT TYPE="hidden" NAME="inputvalues" VALUE="true">
//	<br><INPUT TYPE="checkbox" NAME="zeromodes" VALUE="true"> Zero all mode and strain amplitudes for all output from this page
//	<br><INPUT TYPE="checkbox" NAME="topasstrain" VALUE="true">
//	<br><INPUT TYPE="checkbox" NAME="treetopas" VALUE="true">
//	<br><INPUT TYPE="checkbox" NAME="treecif" VALUE="true">
//	<br><INPUT TYPE="checkbox" NAME="nonstandardsetting" VALUE="true"> Use alternate (possibly nonstandard) setting in CIF output (matrix S<sup>-1t</sup>)<br>
//	<INPUT TYPE="radio" NAME="settingwrt" VALUE="parent" CHECKED> parent
//	<INPUT TYPE="radio" NAME="settingwrt" VALUE="subgroup"> subgroup<br>
//	<tr><td>a' = </td><td><INPUT TYPE="text" NAME="basist11" class="span1" SIZE=3 VALUE="2 ">a +</td><td><INPUT TYPE="text" NAME="basist12" class="span1" SIZE=3 VALUE="0">b +</td><td><INPUT TYPE="text" NAME="basist13" class="span1" SIZE=3 VALUE="0">c</td><td width="40%"></td></tr>
//	<tr><td>b' = </td><td><INPUT TYPE="text" NAME="basist21" class="span1" SIZE=3 VALUE="0">a +</td><td><INPUT TYPE="text" NAME="basist22" class="span1" SIZE=3 VALUE="2 ">b +</td><td><INPUT TYPE="text" NAME="basist23" class="span1" SIZE=3 VALUE="0">c</td></tr>
//	<tr><td>c' = </td><td><INPUT TYPE="text" NAME="basist31" class="span1" SIZE=3 VALUE="0">a +</td><td><INPUT TYPE="text" NAME="basist32" class="span1" SIZE=3 VALUE="0">b +</td><td><INPUT TYPE="text" NAME="basist33" class="span1" SIZE=3 VALUE="2 ">c</td></tr>
//	<tr><td>&#964;' = </td><td><INPUT TYPE="text" NAME="origint1" class="span1" SIZE=3 VALUE="0">a +</td><td><INPUT TYPE="text" NAME="origint2" class="span1" SIZE=3 VALUE="0">b +</td><td><INPUT TYPE="text" NAME="origint3" class="span1" SIZE=3 VALUE="0">c</td></tr></table>
//	<br><INPUT TYPE="checkbox" NAME="cifmovie" VALUE="true"> Make CIF movie<br>
//	<INPUT TYPE="radio" NAME="varcifmovie" VALUE="linear" CHECKED> linear
//	<INPUT TYPE="radio" NAME="varcifmovie" VALUE="sine"> sine-wave<br>

	final static String htmlTest = "<!DOCTYPE html>\r\n" + "<html lang=\"en\">\r\n" + "<HEAD>\r\n"
			+ "<meta charset=\"utf-8\">\r\n" + "<TITLE>ISODISTORT: distortion</TITLE>\r\n"
			+ "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\r\n"
			+ "<meta name=\"description\" content=\"\">\r\n" + "<meta name=\"author\" content=\"\">\r\n"
			+ "<link rel=\"stylesheet\" href=\"bootstrap.css\">\r\n" + "<link rel=\"stylesheet\" href=\"docs.css\">\r\n"
			+ "<style>\r\n" + "  body {\r\n" + "	padding-top: 0px;\r\n" + "  }\r\n" + "  div.pad {\r\n"
			+ "	padding-right: 50px;\r\n" + "	padding-left: 50px;\r\n" + "  }\r\n" + "</style>\r\n" + "</HEAD>\r\n"
			+ "<body>\r\n" + "<div class=\"navbar navbar-static-top\">\r\n" + "  <div class=\"navbar-inner\">\r\n"
			+ "     <div class=\"container\">\r\n"
			+ "       <a class=\"btn btn-navbar\" data-toggle=\"collapse\" data-target=\".nav-collapse\">\r\n"
			+ "         <span class=\"icon-bar\"></span>\r\n" + "         <span class=\"icon-bar\"></span>\r\n"
			+ "	 <span class=\"icon-bar\"></span>\r\n" + "       </a>\r\n"
			+ "       <a class=\"brand\" href=\"isodistort.php\">ISODISTORT</a>\r\n"
			+ "      <div class=\"nav-collapse\">\r\n" + "        <ul class=\"nav\">\r\n"
			+ "         <li><a href=\"isotropy.php\">SUITE</a></li>\r\n"
			+ "                   <li><a href=\"isodistorthelp.php\" TARGET=\"_blank\">HELP</a></li>\r\n"
			+ "	         </ul>\r\n" + "      </div>\r\n" + "    </div>\r\n" + "  </div>\r\n" + "</div>\r\n"
			+ "<div class=\"pad\">\r\n" + "<H1>ISODISTORT: distortion</H1><p>\r\n"
			+ "Space Group: 221 Pm-3m      Oh-1,\r\n" + "Lattice parameters: a=4.2<br>\r\n"
			+ "Default space-group preferences: monoclinic axes a(b)c, monoclinic cell choice 1, orthorhombic axes abc, origin choice 2, hexagonal axes, SSG standard setting<br>\r\n"
			+ "Sr 1b (1/2,1/2,1/2),\r\n" + "Ti 1a (0,0,0),\r\n" + "O 3d (1/2,0,0)<br>\r\n"
			+ "Include strain, displacive distortions<br>\r\n" + "k point: R, k13 (1/2,1/2,1/2)<br>\r\n"
			+ "IR: R4+, k13t9<br>\r\n"
			+ "P1      (a,0,0) 140 I4/mcm, basis={(1,1,0),(-1,1,0),(0,0,2)}, origin=(0,0,0), s=2, i=6, k-active= (1/2,1/2,1/2)<br>\r\n"
			+ "Lattice parameters of undistorted supercell: a=5.93970, b=5.93970, c=8.40000, alpha=90.00000, beta=90.00000, gamma=90.00000\r\n"
			+ "<FORM ACTION=\"isodistortform.php\" METHOD=\"POST\" target=\"_blank\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"input\" VALUE=\"displaydistort\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"spacegroup\" VALUE=\"221 Pm-3m      Oh-1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingaxesm\" VALUE=\"a(b)c               \">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingcell\" VALUE=\"1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingorigin\" VALUE=\"2\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingaxesh\" VALUE=\"h\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingaxeso\" VALUE=\"abc                 \">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"settingssg\" VALUE=\"standard\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"parentsetting\" VALUE=\"323\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"parentsettingstring\" VALUE=\"\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"lattparam\" VALUE=\"a=4.2\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"dlattparam\" VALUE=\"   4.2000000000000002        4.2000000000000002        4.2000000000000002        90.000000000000000        90.000000000000000        90.000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wycount\" VALUE=\"  3\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wypointer001\" VALUE=\"1627\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wynumber001\" VALUE=\" 2\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wytype001\" VALUE=\" 1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyckoff001\" VALUE=\"1b (1/2,1/2,1/2)\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatom001\" VALUE=\"Sr\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatomtype001\" VALUE=\"Sr\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyparam001\" VALUE=\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyocc001\" VALUE=\"   1.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wypointer002\" VALUE=\"1626\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wynumber002\" VALUE=\" 1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wytype002\" VALUE=\" 2\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyckoff002\" VALUE=\"1a (0,0,0)\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatom002\" VALUE=\"Ti\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatomtype002\" VALUE=\"Ti\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyparam002\" VALUE=\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyocc002\" VALUE=\"   1.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wypointer003\" VALUE=\"1629\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wynumber003\" VALUE=\" 4\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wytype003\" VALUE=\" 3\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyckoff003\" VALUE=\"3d (1/2,0,0)\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatom003\" VALUE=\"O\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyatomtype003\" VALUE=\"O\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyparam003\" VALUE=\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"wyocc003\" VALUE=\"   1.0000000000000000\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"includedisplacive001\" VALUE=\"true\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"includedisplacive002\" VALUE=\"true\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"includedisplacive003\" VALUE=\"true\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"includestrain\" VALUE=\"true\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"irrepcount\" VALUE=\"1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"kvec1\" VALUE=\"R, k13 (1/2,1/2,1/2)\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"kvecnumber1\" VALUE=\"  5\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"kparam1\" VALUE=\"    0    0    0    1\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"nmodstar1\" VALUE=\"0\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"irrep1\" VALUE=\"R4+, k13t9\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"irrpointer1\" VALUE=\" 9807\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"isofilename\" VALUE=\"            \">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"orderparam\" VALUE=\"P1      (a,0,0) 140 I4/mcm, basis={(1,1,0),(-1,1,0),(0,0,2)}, origin=(0,0,0), s=2, i=6, k-active= (1/2,1/2,1/2)\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"isosubgroup\" VALUE=\"12431\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"subgroupsym\" VALUE=\"   140\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"subgroupsetting\" VALUE=\" 225\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"subgroupsettingstring\" VALUE=\"\">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"modesfilename\" VALUE=\"isodistort_04491.iso \">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"atomsfilename\" VALUE=\"                     \">\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"lattparamsubstring\" VALUE=\"a=5.93970, b=5.93970, c=8.40000, alpha=90.00000, beta=90.00000, gamma=90.00000\">\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"isovizdistortion\" CHECKED> Save interactive distortion\r\n"
			+ "<a href=\"isodistorthelp.php#savedist\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"isovizdiffraction\"> Save interactive diffraction\r\n"
			+ "<a href=\"isodistorthelp.php#viewdiff\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"structurefile\"> CIF file\r\n"
			+ "<a href=\"isodistorthelp.php#cifsub\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"distortionfile\"> Distortion file\r\n"
			+ "<a href=\"isodistorthelp.php#dfile\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"domains\"> Domains\r\n"
			+ "<a href=\"isodistorthelp.php#domains\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"primary\"> Primary order parameters\r\n"
			+ "<a href=\"isodistorthelp.php#setsprimary\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"modesdetails\"> Modes details\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"completemodesdetails\"> Complete modes details\r\n"
			+ "<a href=\"isodistorthelp.php#modesdetails\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"topas\"> TOPAS.STR\r\n"
			+ "<a href=\"isodistorthelp.php#topas\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"fullprof\"> FULLPROF.pcr\r\n"
			+ "<a href=\"isodistorthelp.php#fullprof\" target=\"_blank\"><img src=help.jpg></a>\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"irreps\"> IR matrices\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"origintype\" VALUE=\"tree\"> Subgroup tree\r\n"
			+ "<INPUT CLASS=\"btn btn-primary\" TYPE=\"submit\" VALUE=\"OK\"><p>\r\n"
			+ "<INPUT TYPE=\"hidden\" NAME=\"inputvalues\" VALUE=\"true\">\r\n"
			+ "Enter mode and strain amplitudes:\r\n"
			+ "<a href=\"isodistorthelp.php#modeamp\" target=\"_blank\"><img src=help.jpg></a><br>\r\n" + "<p>\r\n"
			+ "Pm-3m[1/2,1/2,1/2]R4+ (a,0,0) 140 I4/mcm, basis={(-1,1,0),(-1,-1,0),(0,0,2)}, origin=(0,0,0), s=2, i=6, k-active= (1/2,1/2,1/2)<br>\r\n"
			+ "<input type=\"text\" name=\"mode003001\" value=\" 0.00000\" class=\"span1\" size=9>[O:d:dsp]Eu(a)<br>\r\n"
			+ "<p>\r\n"
			+ "Pm-3m[0,0,0]GM1+ (a) 221 Pm-3m, basis={(0,-1,0),(-1,0,0),(0,0,-1)}, origin=(0,0,0), s=1, i=1, k-active= (0,0,0)<br>\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"strain1\" value=\" 0.00000\" size=9>strain(a)<br>\r\n"
			+ "<p>\r\n"
			+ "Pm-3m[0,0,0]GM3+ (a,0) 123 P4/mmm, basis={(1,0,0),(0,1,0),(0,0,1)}, origin=(0,0,0), s=1, i=3, k-active= (0,0,0)<br>\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"strain2\" value=\" 0.00000\" size=9>strain(a)<br>\r\n"
			+ "<br><INPUT TYPE=\"checkbox\" NAME=\"zeromodes\" VALUE=\"true\"> Zero all mode and strain amplitudes for all output from this page\r\n"
			+ "<p>Parameters:\r\n"
			+ "<a href=\"isodistorthelp.php#modeparams\" target=\"_blank\"><img src=help.jpg></a><br>\r\n"
			+ "\"Save interactive distortion\":<br>\r\n" + "Atomic radius:\r\n"
			+ "<input type=\"text\" name=\"atomicradius\" value=\"0.400\" class=\"span1\" size=5> Angstroms<br>\r\n"
			+ "Maximum bond length:\r\n"
			+ "<input type=\"text\" name=\"bondlength\" value=\"2.500\" class=\"span1\" size=5> Angstroms<br>\r\n"
			+ "Applet width:\r\n"
			+ "<input type=\"text\" name=\"appletwidth\" value=\" 1024\" class=\"span1\" size=5> pixels<br>\r\n"
			+ "Viewing range:\r\n" + "xmin\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellxmin\" value=\"0.000\" size=5>\r\n" + "xmax\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellxmax\" value=\"1.000\" size=5>\r\n" + "ymin\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellymin\" value=\"0.000\" size=5>\r\n" + "ymax\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellymax\" value=\"1.000\" size=5>\r\n" + "zmin\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellzmin\" value=\"0.000\" size=5>\r\n" + "zmax\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"supercellzmax\" value=\"1.000\" size=5><br>\r\n"
			+ "\"Save interactivie distortion\" and \"Save interactive diffraction\":<br>\r\n"
			+ "Maximum displacement per mode:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"modeamplitude\" value=\"1.000\" size=5> Angstroms<br>\r\n"
			+ "Maximum strain per mode:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"strainamplitude\" value=\"0.100\" size=5><br>\r\n"
			+ "<br><INPUT TYPE=\"checkbox\" NAME=\"topasstrain\" VALUE=\"true\">\r\n"
			+ "Include strain modes in TOPAS.STR<br>\r\n"
			+ "<br><INPUT TYPE=\"checkbox\" NAME=\"treetopas\" VALUE=\"true\">\r\n"
			+ "Generate TOPAS.STR output for subgroup tree<br>\r\n"
			+ "<br><INPUT TYPE=\"checkbox\" NAME=\"treecif\" VALUE=\"true\">\r\n"
			+ "Generate CIF output for subgroup tree<br>\r\n" + "<br>Number of decimal places in CIF file:\r\n"
			+ "<SELECT NAME=\"cifdec\" class=\"span1\">\r\n" + "<OPTION VALUE=\" 5\"> 5</OPTION>\r\n"
			+ "<OPTION VALUE=\" 5\"> 5</OPTION>\r\n" + "<OPTION VALUE=\" 6\"> 6</OPTION>\r\n"
			+ "<OPTION VALUE=\" 7\"> 7</OPTION>\r\n" + "<OPTION VALUE=\" 8\"> 8</OPTION>\r\n"
			+ "<OPTION VALUE=\" 9\"> 9</OPTION>\r\n" + "<OPTION VALUE=\"10\">10</OPTION>\r\n"
			+ "<OPTION VALUE=\"11\">11</OPTION>\r\n" + "<OPTION VALUE=\"12\">12</OPTION>\r\n"
			+ "<OPTION VALUE=\"13\">13</OPTION>\r\n" + "<OPTION VALUE=\"14\">14</OPTION>\r\n"
			+ "<OPTION VALUE=\"15\">15</OPTION>\r\n" + "<OPTION VALUE=\"16\">16</OPTION>\r\n" + "</SELECT><br>\r\n"
			+ "<br><INPUT TYPE=\"checkbox\" NAME=\"nonstandardsetting\" VALUE=\"true\"> Use alternate (possibly nonstandard) setting in CIF output (matrix S<sup>-1t</sup>)<br>\r\n"
			+ "Relative to \r\n" + "<INPUT TYPE=\"radio\" NAME=\"settingwrt\" VALUE=\"parent\" CHECKED> parent\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"settingwrt\" VALUE=\"subgroup\"> subgroup<br>\r\n"
			+ "Basis vectors of subgroup lattice (rational numbers):<br><table>\r\n"
			+ "<tr><td>a' = </td><td><INPUT TYPE=\"text\" NAME=\"basist11\" class=\"span1\" SIZE=3 VALUE=\"2 \">a +</td><td><INPUT TYPE=\"text\" NAME=\"basist12\" class=\"span1\" SIZE=3 VALUE=\"0\">b +</td><td><INPUT TYPE=\"text\" NAME=\"basist13\" class=\"span1\" SIZE=3 VALUE=\"0\">c</td><td width=\"40%\"></td></tr>\r\n"
			+ "<tr><td>b' = </td><td><INPUT TYPE=\"text\" NAME=\"basist21\" class=\"span1\" SIZE=3 VALUE=\"0\">a +</td><td><INPUT TYPE=\"text\" NAME=\"basist22\" class=\"span1\" SIZE=3 VALUE=\"2 \">b +</td><td><INPUT TYPE=\"text\" NAME=\"basist23\" class=\"span1\" SIZE=3 VALUE=\"0\">c</td></tr>\r\n"
			+ "<tr><td>c' = </td><td><INPUT TYPE=\"text\" NAME=\"basist31\" class=\"span1\" SIZE=3 VALUE=\"0\">a +</td><td><INPUT TYPE=\"text\" NAME=\"basist32\" class=\"span1\" SIZE=3 VALUE=\"0\">b +</td><td><INPUT TYPE=\"text\" NAME=\"basist33\" class=\"span1\" SIZE=3 VALUE=\"2 \">c</td></tr>\r\n"
			+ "<tr><td colspan=7>Origin of subgroup (either rational or decimal numbers):</td></tr>\r\n"
			+ "<tr><td>&#964;' = </td><td><INPUT TYPE=\"text\" NAME=\"origint1\" class=\"span1\" SIZE=3 VALUE=\"0\">a +</td><td><INPUT TYPE=\"text\" NAME=\"origint2\" class=\"span1\" SIZE=3 VALUE=\"0\">b +</td><td><INPUT TYPE=\"text\" NAME=\"origint3\" class=\"span1\" SIZE=3 VALUE=\"0\">c</td></tr></table>\r\n"
			+ "<p>\r\n" + "<br><INPUT TYPE=\"checkbox\" NAME=\"cifmovie\" VALUE=\"true\"> Make CIF movie<br>\r\n"
			+ "minimum master amplitude:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"ampmincifmovie\" value=\"0\" size=5><br>\r\n"
			+ "maximum master amplitude:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"ampmaxcifmovie\" value=\"1\" size=5><br>\r\n"
			+ "number of frames:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"nframescifmovie\" value=\"10\" size=5><br>\r\n"
			+ "variation of the master amplitude:\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"varcifmovie\" VALUE=\"linear\" CHECKED> linear\r\n"
			+ "<INPUT TYPE=\"radio\" NAME=\"varcifmovie\" VALUE=\"sine\"> sine-wave<br>\r\n"
			+ "fractional number of complete periods:\r\n"
			+ "<input type=\"text\" class=\"span1\" name=\"periodscifmovie\" value=\"1\" size=5><br>\r\n"
			+ "</FORM><p>\r\n" + "</div>\r\n" + "<script src=\"swingjs/swingjs2.js\"></script>\r\n"
			+ "<script src=\"isodistort.js\"></script>\r\n" + "</BODY>\r\n" + "</HTML>\r\n" + "";

	
	/**
	 * @j2sIgnore
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
//
//		System.out.println(bytesContain("....abc...".getBytes(), "abc".getBytes()));
//		
//		System.out.println(bytesContain("....abc...".getBytes(), "ab".getBytes()));
//		System.out.println(bytesContain("....abc".getBytes(), "ab".getBytes()));
//		
//		System.out.println(bytesContain("abc".getBytes(), "abc".getBytes()));
//
//		System.out.println(bytesContain("abc...".getBytes(), "abc".getBytes()));
//
//		System.out.println(bytesContain("ababc".getBytes(), "abc".getBytes()));
//
//		System.out.println(!bytesContain("abab".getBytes(), "abc".getBytes()));

		//		Map<String, Object> map = scrapeHTML(htmlTest);
//		System.out.println(toJSON(map));

	}

}