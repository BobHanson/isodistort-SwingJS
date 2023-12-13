package org.byu.isodistort2.local;

import java.awt.*;

/*
 * Created on Jun 22, 2006 by Andrew Zimmerman
 * Modified by Branton Campbell
 */

public class Dash {
	private int x1, y1, x2, y2, thickness, type;
	private boolean vertical; // used to properly draw thickness

	/*
	 * possible values for color indicator
	 */
	public static final int PINK = 0;
	public static final int GREEN = 1;
	public static final int RED = 2;
	public static final int BLUE = 3;
	public static final int ORANGE = 4;

	public Dash(double cx, double cy, double dirx, double diry, double halflength, double thickness, int type) {
		double deltx = halflength*dirx;
		double delty = halflength*diry;
		x1 = (int)(cx - deltx);
		y1 = (int)(cy - delty);
		x2 = (int)(cx + deltx);
		y2 = (int)(cy + delty);
		this.thickness = (int)thickness;
		this.type = type;
		vertical = Math.abs(deltx) < Math.abs(delty);
	}
	
	public void draw(Graphics gr)
	{
		switch (type)
		{
			case PINK:
				gr.setColor(Color.pink);
				break;
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
		for (int count = 0, sign = 1, offset = 0; count < thickness; count++, sign *= -1, offset += count * sign)
			if (vertical)
				gr.drawLine(x1+offset, y1, x2+offset, y2);
			else
				gr.drawLine(x1, y1+offset, x2, y2+offset);
	}
}
