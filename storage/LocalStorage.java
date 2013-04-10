package storage;

/*
   This library does everything that a persistent 
   servlet would do, only it saves to local files.
*/

import java.util.*;
import java.net.URLEncoder;
import java.net.URLDecoder;

public class LocalStorage implements StorageInterface
{
   public LocalStorage(StorageClient client) {
      if (client != null)
         ( new LocalStorageThread(client) ).start();
   }

   int updateInterval = 300;

   public void setUpdateInterval(int interval) {
      updateInterval = interval;
   }

   class LocalStorageThread extends Thread {
      public LocalStorageThread(StorageClient client) {
         this.client = client;
      }

      public void run() {
         while (true) {
            try {
               sleep(updateInterval);
            } catch (Exception e) { }
            client.updateStorageClient();
         }
      }

      StorageClient client;
   }

   String storedKeyValue(String key, String value) {
      return "(dp1\n" +
             "S'" + key + "'\n" +
	     "p2\n" +
	     "S'" + value + "'\n" +
	     "p3\n";
   }

   public long getTime() {
      return System.currentTimeMillis();
   }

   public String write(String filename, String key, String value) {
      filename = URLEncoder.encode(filename);
      key = URLEncoder.encode(key);
      value = URLEncoder.encode(value);

      String newData = "";
      boolean isNewKey = true;

      try {
         String data = TextIO.load("dbdicts/" + filename);
         for (StringTokenizer st = new StringTokenizer(data, "\n") ; st.hasMoreTokens() ; ) {
	    String p1 = st.nextToken();
	    if (p1.equals("s."))
	       break;

	    String keyLine = st.nextToken();
	    String p2 = st.nextToken();
	    String valueLine = st.nextToken();
	    String p3 = st.nextToken();
            String aKey = keyLine.substring(2, keyLine.length() - 1);
            String aValue = valueLine.substring(2, valueLine.length() - 1);

	    if (aKey.equals(key)) {
	       aValue = value;
	       isNewKey = false;
            }
            newData += storedKeyValue(aKey, aValue);
         }
      } catch (Exception e) { }

      if (isNewKey)
         newData += storedKeyValue(key, value);

      newData += "s.";

      TextIO.save("dbdicts/" + filename, newData);

      return null;
   }

   public String read(String filename, String key) {
      filename = URLEncoder.encode(filename);
      key = URLEncoder.encode(key);

      String data = TextIO.load("dbdicts/" + filename);
      if (data != null)
         for (StringTokenizer st = new StringTokenizer(data, "\n") ; st.hasMoreTokens() ; ) {
	    String p1 = st.nextToken();
	    if (p1.equals("s."))
	       break;

	    String keyLine = st.nextToken();
	    String p2 = st.nextToken();
	    String valueLine = st.nextToken();
	    String p3 = st.nextToken();
	    //String endLine = st.nextToken();
            String aKey = keyLine.substring(2, keyLine.length() - 1);
            String aValue = valueLine.substring(2, valueLine.length() - 1);

	    if (aKey.equals(key))
	       return URLDecoder.decode(aValue);
         }

      return null;
   }
}

