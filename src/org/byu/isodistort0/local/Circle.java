package org.byu.isodistort0.local;

import java.awt.*;

/*
 * Created on May 20, 2006 by Andrew Zimmerman
 * Modified by Branton Campbell
 */

public class Circle {
	
	/*
	 * possible values for the type of circle
	 */
	public static final int GREEN = 1;
	public static final int RED = 2;
	public static final int BLUE = 3;
	public static final int ORANGE = 4;
	
	/*
	 * possible values for color indicator 
	 */
	
	private int x, y, radius, type;
	private boolean tooBig = false; // whether or not the circle is too big for the display
	
	public Circle(double x, double y, double radius, double max, int type) {
		this.radius = (int)Math.rint(radius);
		if (this.radius >= (int)Math.rint(max)) {
			this.radius = (int) Math.rint(max);
			tooBig = true;
		}
		this.x = (int)x;
		this.y = (int)y;
		this.type = type;
	}
	
	public void draw(Graphics gr) {
		
		if (tooBig)
			gr.setColor(Color.yellow);
		else
			gr.setColor(Color.white);
		gr.fillOval(x - radius, y - radius, radius * 2, radius * 2);
		
		switch (type) {
			case GREEN:
				gr.setColor(Color.green);
				break;
			case RED:
				gr.setColor(Color.red);
				break;
			case BLUE:
				gr.setColor(Color.blue);
				break;
			case ORANGE:
				gr.setColor(Color.orange);
				break;
		}
		gr.drawOval(x - 5, y - 5, 10, 10);
		gr.drawOval(x - 6, y - 6, 12, 12);
	}
	
}
