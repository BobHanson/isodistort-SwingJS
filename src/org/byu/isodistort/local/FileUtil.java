package org.byu.isodistort.local;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.util.List;
import java.util.function.Consumer;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.TransferHandler;
import javax.swing.filechooser.FileFilter;

public class FileUtil {
	private static JFileChooser fc;

	/**
	 * Get a file's final dot-extension and possibly match it against an extension or the beginning of an extension. 
	 * 
	 * @param f
	 * @param fileType null (just retrieve extension) or "xxx" (must match) or "xxx*" (must begin with xxx); must be lower-case if not null
	 * @return extension or empty string if fileType is null, otherwise lower-case extension if a match or null if not
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
		boolean isOK = (test == null ? fileType.equals(ext) : ext.startsWith(test));
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

	public static class FileDropHandler extends TransferHandler {

//		static DataFlavor uriListFlavor; // for Linux
		private IsoApp panel;

		/**
		 * Constructor.
		 * 
		 */
		public FileDropHandler(IsoApp applet) {
			this.panel = applet;
//			if (uriListFlavor == null)
//				try {
//					uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
//				} catch (ClassNotFoundException e) {
//					// not possible - it's java.lang.String
//				}
		}

		/**
		 * Check to see that we can import this file. It if is NOT a video-type file
		 * (mp4, jpg, etc) then set the drop action to COPY rather than MOVE.
		 * 
		 */
		@Override
		public boolean canImport(TransferHandler.TransferSupport support) {
			return (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor));
		}

		@Override
		public boolean importData(TransferHandler.TransferSupport support) {
			return (canImport(support) && panel.loadDroppedFile(getFileObject(support.getTransferable())));
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

}
