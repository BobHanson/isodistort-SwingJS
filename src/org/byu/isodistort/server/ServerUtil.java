package org.byu.isodistort.server;

import java.awt.Cursor;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.HashMap;
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
 * This is the main method for initiating a transaction. All transactions are
 * asynchronous, utilizing java.util.function.Consumer to effect a callback to
 * the app.
 * 
 * 
 * public static Map<String, Object> json2Map(Object formData, boolean asClone)
 * 
 * This method ensures that maps from JavaScrpt and Java, might be in the form
 * of actual Java HashMap or LinkedHashMap are compatible with JavaScript's
 * simple associative array idea.
 * 
 * 
 * 
 * 
 * @author Bob Hanson
 *
 */
public class ServerUtil {

	/**
	 * just getting fewer bonds by dividing this by 10; if the FORTRAN gets 
	 * modified to not send a bondlist, then there is no problem, and we can remove this.
	 */
	private static boolean fixBondLength = true;

	private ServerUtil() {
		// no instance; static only
	}

	static {
		/**
		 * @j2sNative
		 * 
		 * J2S.addDirectDatabaseCall("byu.edu");
		 */
	}
	// bh test platform "https://jmol.stolaf.edu/jmol/test/t.php";
	private final static String publicServerURL = "https://iso.byu.edu/";
	private final static String testServerURL = "https://isotest.byu.edu/";

	static boolean isJS = (/** @j2sNative 1 ? true : */ false);

	public static String isoUrl = (IsoApp.testing ? testServerURL : publicServerURL);
	
	static {
		String ref = "";
		if (isJS) {
			/**
			 * will be blank for file://
			 * 
			 * @j2sNative
			 *   
			 *   ref = document.location.host;
			 */
			if (ref.length() > 0)
				isoUrl = "https://" + ref + "/";
		}
		System.err.println("ServerUtil.isoUrl is set to " + isoUrl);
	}

	static int ntest = 0;

	/**
	 * Fetch a result from the server. This method handles all such requests.
	 */

	public static boolean fetch(IsoApp app, int type, Map<String, Object> mapFormData, Consumer<byte[]> consumer,
			int delay) {

		byte[] fileData = (byte[]) mapFormData.remove("toProcess");
		String fileName = (String) mapFormData.remove("fileName");
		String service = (String) mapFormData.remove("_service");

		if (service == null) {
			switch (type) {
			case FileUtil.FILE_TYPE_DISTORTION_UPLOAD:
				service = "isodistortuploadfile.php";
				break;
			case FileUtil.FILE_TYPE_SUBGROUP_TREE:
			case FileUtil.FILE_TYPE_PAGE_HTML:
			case FileUtil.FILE_TYPE_DISTORTION_TXT:
			case FileUtil.FILE_TYPE_FULLPROF_PCR:
			case FileUtil.FILE_TYPE_TOPAS_STR:
			case FileUtil.FILE_TYPE_CIF:
			case FileUtil.FILE_TYPE_ISOVIZ:
				service = "isodistortform.php";
				break;
			default:
				return false;
			}
		}

		String url = isoUrl + service;
		app.addStatus("ServerUtil.fetch " + url + " " + mapFormData.get("origintype"));
		app.setCursor(Cursor.WAIT_CURSOR);
		new Thread(() -> {
			try {
				URI uri = new URI(url);
				HttpClient client = HttpClientFactory.getClient(null);
				HttpRequest request = client.post(uri);
				if (fileData == null)
					request.addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
				request.addHeader("Accept", "text/plain, */*; q=0.01");
				
//				if (testing)
//					FileUtil.write(null, new File("c:/temp/test" + "_out_" + ++ntest + ".json"), toJSON(mapFormData), true);

				for (Entry<String, Object> e : mapFormData.entrySet()) {
					String key = e.getKey();
					String val = fixFormValue(key, e.getValue());
					request.addFormPart(key, val);
				}
				if (fileData != null)
					request.addFilePart("toProcess", new ByteArrayInputStream(fileData), "text/plain",
							(fileName == null ? "iso.txt" : fileName));

				HttpResponse r = request.execute();
				byte[] bytes = FileUtil.getLimitedStreamBytes(r.getContent(), Integer.MAX_VALUE, true);
				// temporary fix for garbage in wyck line
				cleanBytes(bytes);
				app.addStatus("ServerUtil.fetch received " + bytes.length + " bytes");
				if (bytes.length > 100 && bytes.length < 1000) {
					ServerUtil.getTempFile(app, type, bytes, consumer, delay);
				} else {
					consumer.accept(bytes);
				}
			} catch (Exception e) {
				app.addStatus("ServerUtil connection failure: " + e);
				e.printStackTrace();
				consumer.accept(null);
			} finally {				
				app.setCursor(0);
			}
		}, "serverUtil_fetch").start();
		return true;
	}

	/**
	 * dividing the bondlength value by 10 to return no bonds
	 * 
	 * @param key
	 * @param value
	 * @return
	 */
	private static String fixFormValue(String key, Object value) {
		return (fixBondLength  && key.equals("bondlength") 
				? "" + Double.parseDouble(value.toString()) / 10
				: value.toString());
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

		//System.out.println(new String(bytes));
		if (!FileUtil.bytesContain(bytes, SET_TIMEOUT)) {
			consumer.accept(bytes);
			return;
		}
		String html = new String(bytes);
		Map<String, Object> map = FileUtil.scrapeHTML(app, html);
		String service = FileUtil.getHTMLAttr(html, "ACTION");
		if (map == null || service == null) {
			throw new RuntimeException("ServerUtil.getTempFile cannot process server html: " + html);
		}
		app.addStatus("ServerUtil.getTempFile " + map.get("filename") + " delay " + delay + " ms");
		map.put("_service", service);

		Timer tempFileTimer = new Timer(delay, new ActionListener() {

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

	

	

//	/**
//	 * @j2sIgnore
//	 * 
//	 * @param args
//	 */
//	public static void main(String[] args) {
//
//	}

	/**
	 * Convert the form data into a Map if it is a String, or just return it if it is
	 * not.
	 * 
	 * @param formData
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Map<String, Object> json2Map(Object formData, boolean asClone) {
		if (formData == null)
			return null;
		if (formData instanceof Map) {
			return (Map<String, Object>) (asClone ? ((HashMap<String, Object>) formData).clone() : formData);
		}
		if (formData instanceof byte[]) {
			formData = new String((byte[]) formData);
		}
		if (!(formData instanceof String)) {
			/**
			 * must be JSON; just deal with it as a string
			 * 
			 * @j2sNative
			 * 
			 * 			formData = JSON.stringify(formData);
			 * 
			 */
		}
//		if (testing)
//			FileUtil.write(null, new File("c:/temp/test" + "_int_" + ++ntest + ".json"), formData.toString(), true);

		return new JSJSONParser().parseMap(formData.toString(), false);
	}

	/**
	 * A simple JSON producer. All string values
	 * 
	 * @param map
	 * @return
	 */
	public static String toJSON(Map<String, Object> map) {
		StringBuffer sb = new StringBuffer();
		sb.append("{\n");
		String sep = "";
		for (String key : map.keySet()) {
			String data = map.get(key).toString().replace('\n', ' ').replace('\r', ' ');		
			sb.append(sep).append('"').append(key).append("\":\"").append(data).append("\"\n");
			sep = ",";
		}
		sb.append("}");
		return sb.toString();
	}

	static boolean alerted;
	
//	/**
//	 * Select the desired radio button, then press the submit button.
//	 * 
//	 * @param document
//	 * @param originType
//	 */
//	public static void gotoIsoPage(String originType) {
//		@SuppressWarnings("unused")
//		boolean haveAlerted = alerted;
//		alerted = true;
//			/**
//			 * @j2sNative
//			 *   
//haveAlerted || alert("This will open a new tab in your browser.");
//$("input[name='origintype']").each(function(a,b) { 
//  if (b.value == originType) { 
//    b.checked=true;
//    $($("input[type='submit']")[0]).click();
//    return false;
//  }
//});
//			 *   
//			 */
//		
//	}
//
	public static String setIsoBase(String html) {
		return html.replace("<HEAD>",  "<HEAD><base href=" + isoUrl + ">");
	}

	/**
	 * Get the HTML for a page from the server, add the <BASE> tag to direct it back to the server, 
	 * and then push that to a browser, either by doing that directly in JavaScript or (Java)
	 * by putting it into a temp file and opening that local file in a brower.
	 * 
	 * @param app
	 * @param map
	 */
	public static void displayIsoPage(IsoApp app, Map<String, Object> map) {
		fetch(app, FileUtil.FILE_TYPE_PAGE_HTML, map, new Consumer<byte[]>() {

			@Override
			public void accept(byte[] b) {
				String html = setIsoBase(new String(b));
				FileUtil.showHTML(app, html);
			}

		}, 10);
	}

}