package org.byu.isodistort0.local;

import java.awt.Component;
// import java.awt.Dialog;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
// import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

public class ImageSaver
{
	private static JFileChooser fc;
	private static String fileType = "png";
	
	public static String getExtension(File f)
	{
        String ext = null;
        String s = f.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1)
        {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
	
	public static void saveImageFile(Image im, Component parent)
	{
		if(fc == null)
		{
			fc = new JFileChooser();
			fc.addChoosableFileFilter(new FileFilter()
			{
				public String getDescription()
				{
					return "*."+fileType;
				}
				public boolean accept(File f)
				{
				    if (f.isDirectory())
				    {
				    	return true;
				    }
				    String extension = getExtension(f);
				    if (extension != null)
				    {
				    	if (extension.equals(fileType))
					        return true;
					} else
					    return false;
				    return false;
				}
			});
		}
		
		try
		{
//			renderer.refresh();
			BufferedImage bufferedImage = BufferedImageBuilder.bufferImage(im, BufferedImage.TYPE_INT_RGB);
			int okay = fc.showSaveDialog(parent);
			if(okay == JFileChooser.APPROVE_OPTION)
			{	
				File file = fc.getSelectedFile();
				//System.out.println("Extension = "+getExtension(file));
				if((getExtension(file)==null)||(!getExtension(file).equals(fileType)))
				{
					file = new File(file.getAbsolutePath()+"."+fileType);
				}
				if(file.exists())
				{
					okay = JOptionPane.showConfirmDialog(null, "File already exists. Overwrite?", "File already exists",
	                        JOptionPane.YES_NO_OPTION);
					if(okay == JOptionPane.YES_OPTION)
						ImageIO.write(bufferedImage, fileType, file);
				}
				else
					ImageIO.write(bufferedImage, fileType, file);
			}
		}
		catch(Exception e)
		{
			//System.out.println("Oops - png image output failed.");
		}
	}

}
