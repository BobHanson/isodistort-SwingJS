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

	public static String getExtension(File f) {
		String ext = null;
		String s = f.getName();
		int i = s.lastIndexOf('.');

		if (i > 0 && i < s.length() - 1) {
			ext = s.substring(i + 1).toLowerCase();
		}
		return ext;
	}

	public static void saveDataFile(Component parent, Object data, String fileType, boolean isSilent) {
		getFileFromDialog(parent, fileType, (file) -> {
			write(parent, file, data, isSilent);
		}, isSilent);
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

	private static void getFileFromDialog(Component parent, String fileType, Consumer<File> saver, boolean isSilent) {
		if (fc == null) {
			fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileFilter() {
				public String getDescription() {
					return "*." + fileType;
				}

				public boolean accept(File f) {
					if (f.isDirectory()) {
						return true;
					}
					String extension = getExtension(f);
					if (extension != null) {
						if (extension.equals(fileType))
							return true;
					} else
						return false;
					return false;
				}
			});
		}

		int okay = fc.showSaveDialog(parent);
		if (okay == JFileChooser.APPROVE_OPTION) {
			File file = getSelectedFile(fileType, isSilent);
			if (file != null) {
				saver.accept(file);
			}
		}
	}

	private static File getSelectedFile(String fileType, boolean isSilent) {
		File file = fc.getSelectedFile();
		if (getExtension(file) == null || !getExtension(file).equals(fileType)) {
			file = new File(file.getAbsolutePath() + "." + fileType);
		}
		if (!isSilent && file.exists()) {
			if (JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?", "File already exists",
					JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
				return null;
		}
		return file;
	}

	@SuppressWarnings("serial")
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
