package org.byu.isodistort2;

import java.applet.Applet;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import javax.swing.Timer;

import org.byu.isodistort2.local.CommonStuff;

/**
 * 
 * common abstract class for IsoDistortApplet and IsoDiffractApplet
 * 
 * @author Bob Hanson
 *
 */
public abstract class ISOApplet extends Applet implements Runnable {

	abstract protected void runImpl();
	public abstract void updateDisplay();
	
	public int initializing = 5; // lead needed to get first display. Why?

	private static final long serialVersionUID = -4185666763090053134L;

	/**restabit rests the thread for the indicated number of milliseconds.*/
	int restabit = (/**@j2sNative true ? 50 :*/ 100);

	protected boolean isRunning;

	/** An instance of the LoadVariables class that holds all the input data */
	protected CommonStuff rd;
	/** A string containing all of the input data */
	protected String dataString = "";
	/** False for datafile and True for html file */
	protected boolean readMode = false;
	/** The datafile to use when readMode is false */
	protected String whichdatafile = "data/t.txt";

	/**
	 * This data reader has two modes of operation. For data files, it uses a
	 * buffered reader to get the data into a single string. The alternative is to
	 * read a single-string data sequence directly from the html file that calls the
	 * applet (injected as isoData). The LoadVariables class then has a method that
	 * parses this string.
	 */
	protected void readFile() {
		if (readMode)
			dataString = getParameter("isoData");
		else {
			try {
				String path = IsoDistortApplet.class.getName();
				path = path.substring(0, path.lastIndexOf('.') + 1).replace('.','/');
				BufferedReader br = new BufferedReader(new FileReader(path + whichdatafile));// this reads the data
				dataString = br.readLine() + "\n";// scrap the first data line of text
				while (br.ready()) // previously used `for (int i=1;br.ready();i++)
					dataString += br.readLine() + "\n";
				br.close();
			} // close try
			catch (IOException exception) {
				exception.printStackTrace();
				System.out.println("Oops. File not found.");
			}
		}
	}

	public String getParameter(String key) {
		switch (key) {
		case "isoData":
			break;
		}
		return "";
	}
	

	/**
	 * Starts the renderer thread.
	 */

	private Timer timer;

	protected void setTimer() {
		if (timer == null) {
			timer = new Timer(restabit, new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					run();
				}

			});
			timer.setRepeats(true);
		}
	}

	public void start() {
		this.setVisible(true);
		this.requestFocus();
		this.requestFocusInWindow();
		this.requestFocus();
		setTimer();
		isRunning = true;
		timer.start();
	}

	public void run() {
		if (!isRunning)
			return;
		runImpl();
	}
	/**
	 * Stops the renderer thread.
	 */
	public void stop() {
		if (timer != null) {
			isRunning = false;
			timer.stop();
		}
		timer = null;
	}


}
