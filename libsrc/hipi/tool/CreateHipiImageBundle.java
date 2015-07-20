package hipi.tool;

import hipi.image.HipiImageHeader.HipiImageFormat;
import hipi.imagebundle.AbstractImageBundle;
import hipi.imagebundle.HipiImageBundle;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

public class CreateHipiImageBundle {

  public static void main(String[] args) throws IOException  {

    if (args.length < 2) {
      System.out.println("Usage: hibimport <input directory on local file system> <output HIB file on HDFS>");
      System.exit(0);
    }

    File folder = new File(args[0]);
    File[] files = folder.listFiles();

    Configuration conf = new Configuration();
    HipiImageBundle hib = new HipiImageBundle(null, new Path(args[1]), conf);
    hib.open(AbstractImageBundle.FILE_MODE_WRITE, true);

    for (File file : files) {
      FileInputStream fis = new FileInputStream(file);
      String fileName = file.getName().toLowerCase();
      String suffix = fileName.substring(fileName.lastIndexOf('.'));
      if (suffix.compareTo(".jpg") == 0 || suffix.compareTo(".jpeg") == 0) {
	hib.addImage(fis, HipiImageFormat.JPEG);
      }
      else if (suffix.compareTo(".png") == 0) {
	hib.addImage(fis, HipiImageFormat.PNG);
      } 
      System.out.println(" ** added: " + fileName);
    }

    hib.close();
    
    System.out.println("Created: " + args[1] + " and " + args[1] + ".dat");
  }

}
