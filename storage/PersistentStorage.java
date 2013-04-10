package storage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.net.URLDecoder;

public class PersistentStorage {

   StorageClient client;
   LocalStorage ls;
   boolean isLocal = false;

   public void setLocal(boolean state) {
      isLocal = state;
   }

   public String localWrite(String filename, String key, String value) {
      if (ls == null)
         ls = new LocalStorage(client);
      return ls.write(filename, key, value);
   }

   public String localRead(String filename, String key) {
      if (ls == null)
         ls = new LocalStorage(client);
      return ls.read(filename, key);
   }

   public long localTime() {
      return System.currentTimeMillis();
   }
   
   public PersistentStorage(StorageClient client) {
      this.client = client;
      if (client != null)
         ( new PersistentStorageThread(client) ).start();
   }

   int updateInterval = 300;

   public void setUpdateInterval(int interval) {
      updateInterval = interval;
   }

   class PersistentStorageThread extends Thread {
      public PersistentStorageThread(StorageClient client) {
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


   public PersistentStorage() {
   }
   
   String get_time = "http://www.cims.nyu.edu/~perlin/cgi-bin/gettime.cgi";
   String write_url = "http://www.cims.nyu.edu/~perlin/cgi-bin/dictwrite.cgi";
   String read_url = "http://www.cims.nyu.edu/~perlin/cgi-bin/dictread.cgi";

   long startTime = 0, localStartTime = 0;

   public long getTime() {
      if (isLocal)
         return localTime();

      if (startTime == 0) {
         startTime = Long.parseLong(sendRequest(get_time, ""));
	 localStartTime = localTime();
      }

      return startTime + (localTime() - localStartTime);
   }
   
   public String write(String filename, String key, String value) {
      if (isLocal)
         return localWrite(filename, key, value);

      String f = URLEncoder.encode(filename);
      String k = URLEncoder.encode(key);
      String v = URLEncoder.encode(value);
      return sendRequest(write_url,"filename=" + f + "&key=" + k + "&value=" + v);
   }
   
   public String read(String filename, String key) {
      if (isLocal)
         return localRead(filename, key);

      String f = URLEncoder.encode(filename);
      String k = URLEncoder.encode(key);
      return sendRequest(read_url,"filename=" + f + "&key=" + k);
   }

   public String sendRequest(String urlString, String data) {

      //Build parameter string

      try {
          // Send the request
          URL url = new URL(urlString);
          URLConnection conn = url.openConnection();

          OutputStreamWriter writer = null;
	  if (data != null && data.length() > 0) {
             conn.setDoOutput(true);
             writer = new OutputStreamWriter(conn.getOutputStream());
          
             //write parameters
             writer.write(data);
             writer.flush();
          }
          
          // Get the response
          StringBuffer answer = new StringBuffer();
          BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
          String line;

          if ((line = reader.readLine()) != null)
             answer.append(line);

          while ((line = reader.readLine()) != null)
             answer.append("\n" + line);

          if (writer != null)
             writer.close();
          reader.close();
          
          //Output the response
          //System.out.println(URLDecoder.decode(answer.toString(), "UTF-8"));
          return answer == null ? null : (answer.toString());
       
      }
      catch (MalformedURLException ex) {
          ex.printStackTrace();
      }
      catch (IOException ex) {
          isLocal = true;
          //ex.printStackTrace();
      }

       return new String("Error.  Nothing returned");
   }
}

