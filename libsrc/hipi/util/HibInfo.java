package hipi.util;

import hipi.image.HipiImageHeader.HipiImageFormat;
import hipi.image.HipiImageFactory;
import hipi.image.HipiImage;
import hipi.image.HipiImageHeader;
import hipi.image.PixelArray;
import hipi.image.RasterImage;
import hipi.imagebundle.AbstractImageBundle;
import hipi.imagebundle.HipiImageBundle;

import org.apache.commons.io.FilenameUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;

import javax.imageio.stream.ImageOutputStream;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

public class HibInfo {

  private static void usage() {
      System.out.println("Usage: hibinfo <input HIB> [#index [--extract file.jpg]]");
      System.exit(0);
  }    
  
  public static void main(String[] args) throws IOException  {

    if (!(args.length == 1 || args.length == 2 || args.length == 4)) {
      usage();
    }

    int imageIndex = -1;
    int argsIdx = 1;
    
    if (args.length == 2 || args.length == 4) {
      // decode image index
      try {
	imageIndex = Integer.parseInt(args[argsIdx++]);
      } catch (NumberFormatException ex) {
	System.err.println("Unrecognized image index: " + args[1]);
	usage();
	System.exit(0);
      }
    }

    String extractImagePath = null;
    
    if (args.length == 4) {
      if (!args[argsIdx++].equals("--extract")) {
	usage();
      }
      extractImagePath = args[argsIdx++];
    }

    HipiImageBundle hib = null;
    try {
      hib = new HipiImageBundle(HipiImageFactory.getByteImageFactory(), new Path(args[0]), new Configuration());
      hib.open(AbstractImageBundle.FILE_MODE_READ, false);
    } catch (FileNotFoundException ex) {
      System.err.println("Did not find HIB file: " + args[0]);
      System.exit(0);
    }

    int count = 0;
    while (hib.hasNext()) {

      if (imageIndex == -1) {
	System.out.println("IMAGE: " + (count+1));
      }
      
      HipiImageHeader header = hib.next();
      
      if (imageIndex == -1 || imageIndex == (count+1)) {

	System.out.println(String.format("   %d x %d", header.getWidth(), header.getHeight()));
	
	HashMap<String, String> metaData = header.getAllMetaData();
	System.out.println("   meta: " + metaData);
	
	if (extractImagePath != null) {

	  String imageExtension = FilenameUtils.getExtension(extractImagePath);
	  if (imageExtension == null) {
	    System.err.println(String.format("Failed to determine image type based on extension [%s]. Please provide a valid path with complete extension.", extractImagePath));
	    System.exit(0);
	  }
	  
	  ImageOutputStream ios = null;
	  try {
	    ios = ImageIO.createImageOutputStream(new File(extractImagePath));
	  } catch (IOException ex) {
	    System.err.println(String.format("Failed to open image file for writing [%s]", extractImagePath));
	    System.exit(0);
	  }
	  Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(imageExtension);
	  if (writers == null) {
	    System.err.println(String.format("Failed to locate encoder for image extension [%s]", imageExtension));
	    System.exit(0);
	  }	    
	  ImageWriter writer = writers.next();
	  if (writer == null) {
	    System.err.println(String.format("Failed to locate encoder for image extension [%s]", imageExtension));
	    System.exit(0);
	  }
	  System.out.println("Using image encoder: " + writer);
	  writer.setOutput(ios);

	  HipiImage image = hib.getCurrentImage();
	  
	  int w = image.getWidth();
	  int h = image.getHeight();

	  BufferedImage bufferedImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

	  PixelArray pa = ((RasterImage)image).getPixelArray();
	  int[] rgb = new int[w*h];
	  for (int i=0; i<w*h; i++) {

	    int r = pa.getElemNonLinSRGB(i*3+0);
	    int g = pa.getElemNonLinSRGB(i*3+1);
	    int b = pa.getElemNonLinSRGB(i*3+2);

	    rgb[i] = (r << 16) | (g << 8) | b;
	  }
	  bufferedImage.setRGB(0, 0, w, h, rgb, 0, w);

	  ImageWriteParam param = writer.getDefaultWriteParam();
	  //	  param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
	  //	  param.setCompressionQuality(0.95F); // highest JPEG quality = 1.0F

	  IIOImage iioImage = new IIOImage(bufferedImage, null, null);
	  writer.write(null, iioImage, param);

	  System.out.println(String.format("Wrote [%s]", extractImagePath));
	}

	if (imageIndex != -1) {
	  hib.close();
	  System.exit(0);
	}
	
      }

      count++;
	
    }

    hib.close();

    if (imageIndex == -1) {
      System.out.println(String.format("Found [%d] images.", count));
    } else {
      System.out.println(String.format("Failed to find image with index [%d]. Found only %d images in HIB", imageIndex, count));
    }
      

  }

}
