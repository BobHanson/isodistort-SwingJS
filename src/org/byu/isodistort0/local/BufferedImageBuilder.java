package org.byu.isodistort0.local;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class BufferedImageBuilder
{

	public static BufferedImage bufferImage(Image image, int type) 
	{
		BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), type); 
		for(int i = 0; i < image.getWidth(null);i++) 
		for(int j = 0; j < image.getHeight(null);j++) 
		bufferedImage.setRGB(i, j, Color.WHITE.getRGB()); 
		Graphics2D g = bufferedImage.createGraphics();
		g.setBackground(Color.WHITE); 
		g.drawImage(image, null, null); 
		waitForImage(bufferedImage, image.getWidth(null), image.getHeight(null)); 
		return bufferedImage; 
	}
	
	private static void waitForImage(BufferedImage buf, int width, int height)
	{
		while ((buf.getHeight() != height)&&(buf.getWidth() != width))
		{
			try
			{
				System.out.println("Taking a quick nap");
				Thread.sleep(300);
				System.out.println("Took a quick nap");
			} catch (InterruptedException e)
			{

			}
		}
	}
}