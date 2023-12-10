/*
 * The tracker package defines a set of video/image analysis tools
 * built on the Open Source Physics framework by Wolfgang Christian.
 *
 * Copyright (c) 2021 Douglas Brown, Wolfgang Christian, Robert M. Hanson
 *
 * Tracker is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tracker is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tracker; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at <http://www.gnu.org/copyleft/gpl.html>
 *
 * For additional Tracker information and documentation, please see
 * <http://physlets.org/tracker/>.
 */
package org.byu.isodistort.local;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;

import javax.swing.TransferHandler;

@SuppressWarnings("serial")
public static class FileDropHandler extends TransferHandler {

//	static DataFlavor uriListFlavor; // for Linux
	private IsoPanel panel;

	/**
	 * Constructor.
	 * 
	 */
	public FileDropHandler(IsoPanel applet) {
		this.panel = applet;
//		if (uriListFlavor == null)
//			try {
//				uriListFlavor = new DataFlavor("text/uri-list;class=java.lang.String");
//			} catch (ClassNotFoundException e) {
//				// not possible - it's java.lang.String
//			}
	}

	/**
	 * Check to see that we can import this file. It if is NOT a video-type
	 * file (mp4, jpg, etc) then set the drop action to COPY rather than MOVE.
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
