
package interpreter;

import java.io.*;

public class FileIsModified
{
   long time = 0L;

   public boolean isModified(File file) {
      try {
         long time = file.lastModified();
         boolean isModified = time > this.time;
         this.time = time;
         return isModified;
      } catch (SecurityException e) {
         return false;
      }
   }
}

