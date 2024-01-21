package org.byu.isodistort.local;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

public class FileUtil {

	public final static int FILE_TYPE_UNKNOWN        = 0;
	public final static int FILE_TYPE_DISTORTION_UPLOAD = 1;
	public final static int FILE_TYPE_ISOVIZ         = 2;
	public final static int FILE_TYPE_FORMDATA_JSON  = 3;
	public final static int FILE_TYPE_DISTORTION_TXT = 4;
	public final static int FILE_TYPE_FULLPROF_PCR   = 5;
	public final static int FILE_TYPE_CIF            = 6;
	public final static int FILE_TYPE_TOPAS_STR      = 7;
	public final static int FILE_TYPE_PAGE_HTML      = 8;
	public static final int FILE_TYPE_SUBGROUP_TREE  = 9;

	private static JFileChooser fc;

	/**
	 * Get a file's final dot-extension and possibly match it against an extension
	 * or the beginning of an extension.
	 * 
	 * @param f
	 * @param fileType null (just retrieve extension) or "xxx" (must match) or
	 *                 "xxx*" (must begin with xxx); must be lower-case if not null
	 * @return extension or empty string if fileType is null, otherwise lower-case
	 *         extension if a match or null if not
	 */
	public static String getExtension(File f, String fileType) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');
		if (i < 0)
			return (fileType == null ? "" : null);
		ext = s.substring(i + 1).toLowerCase();
		if (fileType == null)
			return ext;
		String test = (fileType.endsWith("*") ? fileType.substring(0, fileType.length() - 1) : null);
		boolean isOK = (test == null ? fileType.equalsIgnoreCase(ext) : ext.startsWith(test));
		return (isOK ? ext : null);
	}

	public static void saveDataFile(Component parent, Object data, String fileType, boolean isSilent) {
		getFileFromDialog(parent, fileType, (file) -> {
			write(parent, file, data, isSilent);
		}, isSilent);
	}

	private static void getFileFromDialog(Component parent, String fileType, Consumer<File> saver, boolean isSilent) {
		if (fc == null) {
			fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileFilter() {
				@Override
				public String getDescription() {
					return "*." + fileType;
				}

				@Override
				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					return (getExtension(f, fileType) != null);
				}
			});
		}

		int okay = fc.showSaveDialog(parent);
		if (okay == JFileChooser.APPROVE_OPTION) {
			File file = getSaveSelectedFile(fileType, isSilent);
			if (file != null) {
				saver.accept(file);
			}
		}
	}

	private static File getSaveSelectedFile(String fileType, boolean isSilent) {
		File file = fc.getSelectedFile();
		String ext = getExtension(file, fileType);
		if (ext == null) {
			file = new File(file.getAbsolutePath() + "." + fileType);
		}
		if (!isSilent && file.exists()) {
			if (JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?", "File already exists",
					JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
				return null;
		}
		return file;
	}

	private static void write(Component parent, File file, Object data, boolean isSilent) {
		if (data == null || file == null)
			return;
		try {
			if (data instanceof BufferedImage) {
				ImageIO.write((BufferedImage) data, "png", file);
			} else {
				FileOutputStream fos = new FileOutputStream(file);
				fos.write(data instanceof String ? ((String) data).getBytes() : (byte[]) data);
				fos.close();
			}
			if (!isSilent)
				JOptionPane.showMessageDialog(parent, "File " + file + " has been saved");
		} catch (Exception e) {
			if (!isSilent)
				JOptionPane.showMessageDialog(parent, "File " + file + " could not be saved: " + e.getMessage());
		}
	}

	@SuppressWarnings("serial")
	public static class FileDropHandler extends TransferHandler {

//		static DataFlavor uriListFlavor; // for Linux
		private IsoApp app;

		/**
		 * Constructor.
		 * 
		 */
		public FileDropHandler(IsoApp app) {
			this.app = app;
//			if (uriListFlavor == null)
//				try {
//					uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
//				} catch (ClassNotFoundException e) {
//					// not possible - it's java.lang.String
//				}
		}

		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			if (!canImport(support))
				return false;
			File f = getFileObject(support.getTransferable());
			new Thread(() -> {
				app.loadDroppedFile(f);
			}, "isodistort_file_dropper").start();
			return true;
		}

		/**
		 * Gets the file list from a Transferable.
		 * 
		 * Since Java 7 there is no issue with Linux.
		 * 
		 * @param t the Transferable
		 * @return a List of files
		 */
		@SuppressWarnings("unchecked")
		private File getFileObject(Transferable t) {
			try {
				return ((List<File>) t.getTransferData(DataFlavor.javaFileListFlavor)).get(0);

			} catch (Exception e) {
				return null;
			}
		}

	}

	public static class HTMLScraper {
	
		private final String html;
	
		private final Map<String, Object> map = new LinkedHashMap<>();

		private IsoApp app;
	
		HTMLScraper(IsoApp app, String html) {
			// <FORM...> ...... </FORM>
			this.app = app;
			this.html = getInnerHTML(html, "FORM");
		}
	
		Map<String, Object> scrape() {
			if (html == null)
				return null;
			String[] inputs = html.replace("<input", "<INPUT").split("<INPUT");
			if (inputs != null)
			for (int i = 1; i < inputs.length; i++) {
				addEntry(inputs[i]);
			}
			String[] selects = html.replace("select>", "SELECT>").split("SELECT");
			if (selects != null)
				for (int i = 1; i < selects.length; i += 2) {
				addSelect(selects[i]);
			}
			return map;
		}
	
		private String getInnerHTML(String html, String tag) {
			System.out.println(html);
			String[] parts = html.split(tag.toUpperCase());
			if (parts.length == 2) {
				app.addStatus("FileUtil.getInnerHTML for </" + tag + "> not found!");
			}
			return (parts.length > 1 ? parts[1] : null);
		}
	
		private void addEntry(String line) {
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
			map.put(name, value.replace('\n', ' ').replace('\r', ' '));
		}
	
		private void addSelect(String line) {
			line.replace("option",  "OPTION");
			String[] values = line.split("<OPTION");
			String value = null;
			for (int i = 1; i < values.length; i++) {
				String v = getHTMLAttr(values[i], "VALUE");
				if (value == null || values[i].indexOf("SELECTED") >= 0) {
					value = v;
				}			
			}
			if (value == null)
				return;
			String name = getHTMLAttr(line, "NAME");
			map.put(name, value);
		}
	}

	public static String toString(BufferedInputStream bis, boolean doClose) throws IOException {

		return new String(getLimitedStreamBytes(bis, 0, doClose));
	}

	/**
	 * Read a stream and deliver its associated byte[] array.
	 * 
	 * @param is      the InputStream; note that you cannot use
	 *                InputStream.available() to reliably read zip data from the
	 *                web.
	 * 
	 * @param n       if positive and not Integer.MAX_VALUE, the number of bytes to
	 *                limit the read to, which is also then the size of the byte
	 *                array for reading; if negative or Integer.MAX_VALUE, then the
	 *                return array length is limited only to the stream's source.
	 * 
	 * @param doClose TRUE to close the stream
	 * @return the byte array read
	 * @throws IOException
	 */
	public static byte[] getLimitedStreamBytes(InputStream is, long n, boolean doClose) throws IOException {

		int buflen = (n > 0 && n < 1024 * 512 ? (int) n : 1024 * 512);
		byte[] buf = new byte[buflen];
		byte[] bytes = new byte[n < 0 || n == Integer.MAX_VALUE ? 4096 : (int) n];
		int len = 0;
		int totalLen = 0;
		if (n < 0)
			n = Integer.MAX_VALUE;
		while (totalLen < n && (len = is.read(buf, 0, buflen)) > 0) {
			totalLen += len;
			if (totalLen > bytes.length) {
				bytes = Arrays.copyOf(bytes, totalLen * 2);
			}
			System.arraycopy(buf, 0, bytes, totalLen - len, len);
			if (n != Integer.MAX_VALUE && totalLen + buflen > bytes.length)
				buflen = bytes.length - totalLen;

		}
		if (totalLen == bytes.length)
			return bytes;
		buf = new byte[totalLen];
		System.arraycopy(bytes, 0, buf, 0, totalLen);
		return buf;
	}

	
	final static byte[] IRREP_SSGNum = "!irrepSSGNum".getBytes();
	
	public static boolean checkIncommensurateDFile(byte[] data) {
	  return bytesContain(data, IRREP_SSGNum);		
	}
	
	public static boolean checkIncommensurateFormData(Map<String, Object> data) {
		Object o = data.get("irrepcount");
		int nrep = (o == null ? 0 : Integer.valueOf(o.toString()));
		for (int i = 1; i <= nrep; i++) {
			o = data.get("nmodstar" + i);
			if (o != null && o.toString().charAt(0) != '0')
				return true;
		}
		return false;
	}
		
	public static int getIsoFileTypeFromContents(byte[] data) {
		int type = FILE_TYPE_UNKNOWN;
		for (int pt = 0, i = 0, n = Math.min(data.length - 20, 500); i < n; i++) {
			switch (data[i]) {
			case 0:
			case -1:
				i = n;
				break;
			case '{':
				if (pt == 0)
					return FILE_TYPE_FORMDATA_JSON;
			case '\n':
			case '\r':
				pt = i + 1;
				break;
			case '!':
				if (i == pt) {
					if (data[i + 8] == 'i') {
						//012345678 
						//!isoversion
						//!begin distortionFile
						String tag = new String(data, i + 1, 20);
						if (tag.equals("begin distortionFile")) {
							type = FILE_TYPE_DISTORTION_TXT;
						} else if (tag.startsWith("isoversion")) {
							type = FILE_TYPE_ISOVIZ;
						}
						break;
					}
				}
				i = n;
				break;
			}
		}
		return type;
	}

	// boolean asInputStream = true;
		public static byte[] readFileData(IsoApp app, String path) {
			app.addStatus("FileUtil.readFileData " + path);
			try {
				long t = System.currentTimeMillis();
				// 33 ms to read 8 MB
				BufferedInputStream bis = null;
				try {	
					File f = new File(path);
					bis = new BufferedInputStream(new FileInputStream(f));
				} catch (Exception e) {
					InputStream is = (InputStream) app.getClass().getResource("/" + path).getContent();
					if (is == null) {
						app.addStatus("FileUtil.readFileData could not find resource " + path);
						return null;					
					}
					bis = new BufferedInputStream(is);
				}
				byte[] bytes = getLimitedStreamBytes(bis, Integer.MAX_VALUE, true);
				app.addStatus("FileUtil.readFileData " + (System.currentTimeMillis() - t) + " ms for " + bytes.length + " bytes");
				return bytes;
	
	//			// String concatenation (200K+ lines) was 600000+ ms (over 10 minutes) to read 8 MB
	//			// StringBuffer was 121 ms to read 8 MB
	//			// getLimitedStreamReader was 33 ms to read 8 MB
				
			} catch (IOException exception) {
				exception.printStackTrace();
				System.out.println("Oops. File not found. " + path);
			}
			return null;
		}

	public static void openURL(IsoApp app, String surl) {
		try {
			/**
			 * @j2sNative
			 * 
			 * 			window.open(surl, "_blank");
			 */
			{
				Class<?> c = Class.forName("java.awt.Desktop");
				Method getDesktop = c.getMethod("getDesktop", new Class[] {});
				Object deskTop = getDesktop.invoke(null, new Object[] {});
				Method browse = c.getMethod("browse", new Class[] { URI.class });
				Object arguments[] = { new URI(surl) };
				browse.invoke(deskTop, arguments);
			}
		} catch (Throwable e) {
			app.addStatus("FileUtil.openURL could not open url " + surl + ": " + e.getMessage());
			e.printStackTrace();
		}
	}

	public static void showHTML(IsoApp app, String html) {
		/**
		 * @j2sNative
		 * 
		 * 	  window.open(URL.createObjectURL(new Blob([html], { type: "text/html" })));
		 *    return;
		 */
		{
			try {
				File f = File.createTempFile("isodistort", ".html");
				write(null, f, html, true);
				JOptionPane.showMessageDialog(app.frame, html.length() + " bytes received. Check your browser if it does not pop up immediately.");
				openURL(app, "file://" + f.getAbsolutePath().replace('\\', '/'));				
			} catch (IOException e) {
				app.addStatus("temp file creation failed: " + e.getMessage());
				e.printStackTrace();
			}
		}
	}

	/**
	 * Extract all the INPUT data from the HTML page.
	 * 
	 * @param html
	 * @param app 
	 * @return
	 */
	public static Map<String, Object> scrapeHTML(IsoApp app, String html) {
		return new HTMLScraper(app, html).scrape();
	}

	public static String getHTMLAttr(String line, String attr) {
		String key = attr + "=\"";
		int pt = line.indexOf(key);
		if (pt < 0)
			pt = line.indexOf(key.toLowerCase());
	
		if (pt < 0 || (pt = pt + key.length()) > line.length())
			return null;
		int pt1 = line.indexOf("\"", pt);
		return (pt1 < 0 ? null : line.substring(pt, pt1).trim());
	}

	public static boolean bytesContain(byte[] bytes, byte[] b) {
	
		int nb = bytes.length, n = b.length;
		if (nb < n || nb == 0) {
			return false;
		}
		byte b0 = b[0];
		int i0 = 0;
		int i1 = nb - n;
		// 012345678901
		// ......abc...
		// 0......ababc..
		// ....... ^ pt = 2, i0 =
		for (int pt = 0, i = 0; i - pt <= i1; i++) {
			if (bytes[i] == b[pt++]) {
				if (pt == n)
					return true;
				if (i0 == 0 && bytes[i + 1] == b0) {
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

}
