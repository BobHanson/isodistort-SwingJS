package org.byu.isodistort.server;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;

import javax.swing.SwingUtilities;

import org.byu.isodistort.local.IsoApp;

import javajs.http.HttpClient;
import javajs.http.HttpClient.HttpRequest;
import javajs.http.HttpClient.HttpResponse;
import javajs.http.HttpClientFactory;
import javajs.util.JSJSONParser;

public class ServerUtil {

	private ServerUtil() {
		// no instance; static only
	}

	// <form action="isodistortuploadfile.php" method="POST"
	// enctype="multipart/form-data"

	static String publicServerURL = "https://iso.byu.edu/iso/isodistortform.php";
	static String testServerURL = "https://isotest.byu.edu/iso/isodistortform.php";

	@SuppressWarnings("unchecked")
	public static void fetch(IsoApp app, Object mapFormData, Consumer<String> callback) {
		boolean testing = true;
		String url = (testing ? testServerURL : publicServerURL);

		SwingUtilities.invokeLater(() -> {
			try {
			/**
			 * @j2sNative
			 * 
			 * 			var info = { type: "post", url: url, data: mapFormData, dataType:
			 *            "text", encode: true, }; $.ajax(info)
			 *            .done(function(data){consumer.accept$S}) .fail(function
			 *            (data){consumer.accept$S});
			 * 
			 */
			{
				URI uri = new URI(url);
				HttpClient client = HttpClientFactory.getClient(null);
				HttpRequest request = client.post(uri);
				for (Entry<String, Object> e : ((Map<String, Object>) mapFormData).entrySet()) {
					request.addFormPart(e.getKey(), e.getValue().toString());
				}
				HttpResponse r = request.execute();
				callback.accept(r.getText());
			}
			} catch (Exception e) {
				e.printStackTrace();
				callback.accept(null);
			}
		});
	}

	/**
	 * Get a value from the form data. In Java this will be a java.util.Map; in
	 * JavaScript it will be a JavaScript associative array (probably).
	 * 
	 * @param mapFormData
	 * @param key
	 * @return the value or null
	 */
	@SuppressWarnings("unchecked")
	public static String getFormData(Object mapFormData, String key) {
		if (mapFormData instanceof Map) {
			return (String) ((Map<String, Object>) mapFormData).get(key);
		}
		/**
		 * @j2sNative
		 * 
		 * 			return mapFormData[key] || null;
		 */
		{
			return null;
		}
	}

	/**
	 * Get a value from the form data. In Java this will be a java.util.Map; in
	 * JavaScript it will be a JavaScript associative array (probably).
	 * 
	 * @param mapFormData
	 * @param key
	 * @return the value or null
	 */
	@SuppressWarnings("unchecked")
	public static void setFormData(Object mapFormData, String key, String value) {
		if (mapFormData instanceof Map) {
			((Map<String, Object>) mapFormData).put(key, value);
		}
		/**
		 * @j2sNative
		 * 
		 * 			mapFormData[key] = value;
		 */
		{
		}
	}

	/**
	 * Conver the form data into a Map if it is a String, or just return it if it is
	 * not.
	 * 
	 * @param formDataStr
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static Object json2Map(Object formDataStr) {
		if (formDataStr instanceof String) {
			return (Map<String, Object>) new JSJSONParser().parse((String) formDataStr, false);
		}
		/**
		 * @j2sNative
		 * 
		 * 			newFormData = {}; for (var key in formDataStr) { newFormData[key]
		 *            = formDataStr[key]; } return newFormData;
		 * 
		 */
		{
			return (Map<String, Object>) ((HashMap<String, Object>) formDataStr).clone();

		}
	}

	public final static String testFormData = "{" + "\"input\":\"displaydistort\"," + "\"spacegroup\":\"221 Pm-3m      Oh-1\","
			+ "\"settingaxesm\":\"a(b)c               \"," + "\"settingcell\":\"1\"," + "\"settingorigin\":\"2\","
			+ "\"settingaxesh\":\"h\"," + "\"settingaxeso\":\"abc                 \"," + "\"settingssg\":\"standard\","
			+ "\"parentsetting\":\"323\"," + "\"parentsettingstring\":\"\"," + "\"lattparam\":\"a=4.2\","
			+ "\"dlattparam\":\"   4.2000000000000002        4.2000000000000002        4.2000000000000002        90.000000000000000        90.000000000000000        90.000000000000000\","
			+ "\"wycount\":\"  3\"," + "\"wypointer001\":\"1627\"," + "\"wynumber001\":\" 2\","
			+ "\"wytype001\":\" 1\"," + "\"wyckoff001\":\"1b (1/2,1/2,1/2)\"," + "\"wyatom001\":\"Sr\","
			+ "\"wyatomtype001\":\"Sr\","
			+ "\"wyparam001\":\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\","
			+ "\"wyocc001\":\"   1.0000000000000000\"," + "\"wypointer002\":\"1626\"," + "\"wynumber002\":\" 1\","
			+ "\"wytype002\":\" 2\"," + "\"wyckoff002\":\"1a (0,0,0)\"," + "\"wyatom002\":\"Ti\","
			+ "\"wyatomtype002\":\"Ti\","
			+ "\"wyparam002\":\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\","
			+ "\"wyocc002\":\"   1.0000000000000000\"," + "\"wypointer003\":\"1629\"," + "\"wynumber003\":\" 4\","
			+ "\"wytype003\":\" 3\"," + "\"wyckoff003\":\"3d (1/2,0,0)\"," + "\"wyatom003\":\"O\","
			+ "\"wyatomtype003\":\"O\","
			+ "\"wyparam003\":\"   0.0000000000000000        0.0000000000000000        0.0000000000000000\","
			+ "\"wyocc003\":\"   1.0000000000000000\"," + "\"includedisplacive001\":\"true\","
			+ "\"includedisplacive002\":\"true\"," + "\"includedisplacive003\":\"true\","
			+ "\"includestrain\":\"true\"," + "\"irrepcount\":\"2\"," + "\"kvec1\":\"GM, k12 (0,0,0)\","
			+ "\"kvecnumber1\":\"  1\"," + "\"kparam1\":\"    0    0    0    1\"," + "\"nmodstar1\":\"0\","
			+ "\"irrep1\":\"GM4-, k12t10\"," 
			+ "\"irrpointer1\":\" 9790\"," 
			+ "\"kvec2\":\"R, k13 (1/2,1/2,1/2)\","
			+ "\"kvecnumber2\":\"  5\"," 
			+ "\"kparam2\":\"    0    0    0    1\"," + "\"nmodstar2\":\"0\","
			+ "\"irrep2\":\"R4+, k13t9\"," 
			+ "\"irrpointer2\":\" 9807\"," 
			+ "\"isofilename\":\"s0979000.iso\","
			+ "\"orderparam\":\" P3(1)P3(1) (a,a,a|b,b,b) 161 R3c, basis={(1,0,-1),(0,-1,1),(-2,-2,-2)}, origin=(0,0,0), s=2, i=16, k-active= (0,0,0);(1/2,1/2,1/2)\","
			+ "\"isosubgroup\":\"    1\"," + "\"subgroupsym\":\"   161\"," + "\"subgroupsetting\":\" 254\","
			+ "\"subgroupsettingstring\":\"\"," + "\"modesfilename\":\"isodistort_70013.iso \","
			+ "\"atomsfilename\":\"                     \","
			+ "\"lattparamsubstring\":\"a=5.93970, b=5.93970, c=14.54923, alpha=90.00000, beta=90.00000, gamma=120.00000\","
			+ "\"origintype\":\"isovizdistortion\"," + "\"\":\"1\"," + "\"inputvalues\":\"false\","
			+ "\"mode001001\":\".1\"," 
			+ "\"mode002001\":\".2\"," 
			+ "\"mode003001\":\".05\","
			+ "\"mode003002\":\".15\"," 
			+ "\"mode003003\":\"0\"," 
			+ "\"strain1\":\"0\"," 
			+ "\"strain2\":\"0\","
			+ "\"atomicradius\":\"0.4\"," + "\"bondlength\":\"2.50\"," + "\"appletwidth\":\"1024\","
			+ "\"supercellxmin\":\"0.000\"," + "\"supercellxmax\":\"1.000\"," + "\"supercellymin\":\"0.000\","
			+ "\"supercellymax\":\"1.000\"," + "\"supercellzmin\":\"0.000\"," + "\"supercellzmax\":\"1.000\","
			+ "\"modeamplitude\":\"1.0\"," + "\"strainamplitude\":\"0.1\"," + "\"settingwrt\":\"parent\","
			+ "\"basist11\":\"2 \"," + "\"basist12\":\"0\"," + "\"basist13\":\"0\"," + "\"basist21\":\"0\","
			+ "\"basist22\":\"2 \"," + "\"basist23\":\"0\"," + "\"basist31\":\"0\"," + "\"basist32\":\"0\","
			+ "\"basist33\":\"2 \"," + "\"origint1\":\"0\"," + "\"origint2\":\"0\"," + "\"origint3\":\"0\","
			+ "\"ampmincifmovie\":\"0\"," + "\"ampmaxcifmovie\":\"1\"," + "\"nframescifmovie\":\"10\","
			+ "\"varcifmovie\":\"linear\"," + "\"periodscifmovie\":\"1\","
			+ "\"isoDistort0_RadioButtonUI_107_107\":\"on\"," + "\"isoDistort0_CheckBoxUI_100_100\":\"on\","
			+ "\"isoDistort0_CheckBoxUI_101_101\":\"on\"," + "\"isoDistort0_CheckBoxUI_102_102\":\"on\","
			+ "\"isoDistort0_RadioButtonUI_112_112\":\"on\"" + "}";
	
	// gives

	//...
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


}