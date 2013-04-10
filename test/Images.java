
package test;

import java.io.File;
import javax.imageio.ImageIO;
import java.util.ArrayList;
import java.awt.Image;
import java.net.URL;

public class Images {
   public void setApplet(java.applet.Applet applet) {
      this.applet = applet;
   }

   public Image get(String name) {
      for (int i = 0 ; i < images.size() ; i++)
         if (name.equals((String)names.get(i)))
             return (Image)images.get(i);

      Image image = null;
      try {
         image = ImageIO.read(new File(name));
      } catch (Exception e1) {
      try {
         image = ImageIO.read(new File("assets/" + name));
      } catch (Exception e2) {
      try {
         URL url = new java.net.URI(applet.getDocumentBase().toExternalForm()).resolve("./" + name).toURL();
         image = ImageIO.read(url);
      } catch (Exception e3) {
      try {
         URL url = new java.net.URI(applet.getDocumentBase().toExternalForm()).resolve("assets/" + name).toURL();
         image = ImageIO.read(url);
      } catch (Exception e) {
         System.err.println(e);
      }}}}
      images.add(image);
      names.add(name);
      return image;
   }

   int size = 0;
   ArrayList images = new ArrayList();
   ArrayList names = new ArrayList();
   java.applet.Applet applet;
}

