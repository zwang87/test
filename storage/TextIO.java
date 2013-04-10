package storage;

/*
   Save text to a local file.
   Load text from a URL, local file or input stream.
*/

import java.io.*;
import java.net.*;

public class TextIO
{
   public static boolean save(String fileName, String text) {
      try {
         File dir = new File(System.getProperty("user.dir"));
         FileOutputStream fout = new FileOutputStream(new File(dir, fileName));
         fout.write(text.getBytes());
         fout.close();
         return true;
      } catch (Exception e) { return false; }
   }

   public static String load(URL url) {
      try {
         return load(url.openStream());
      } catch (Exception e) { return null; }
   }

   public static String load(String fileName) {
      try {
         File dir  = new File(System.getProperty("user.dir"));
         File file = new File(dir, fileName);
         URL  url  = file.toURI().toURL();
         return load(url);
      } catch (Exception e) { return null; }
   }

   public static String load(InputStream in) {
      try {
         ByteArrayOutputStream out = new ByteArrayOutputStream();
         byte buf[] = new byte[1024];
         while (true) {
            int n = in.read(buf);
            if (n < 0)
               break;
            out.write(buf, 0, n);
         }
         return new String(out.toByteArray());
      } catch (Exception e) { return null; }
   }
}

