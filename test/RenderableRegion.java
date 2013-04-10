
package test;

import java.util.*;
import java.awt.*;
import java.awt.image.*;

public class RenderableRegion extends Region implements Runnable {
   public void init(int width, int height, Component component) {
      this.width = width;
      this.height = height;
      this.component = component;
      pix = new int[width * height];
      mis = new MemoryImageSource(width, height, pix, 0, width);
      mis.setAnimated(true);
      image = component.createImage(mis);
      thread.start();
   }

   public void setOnScreen(boolean state) { isOnScreen = state; }

   public boolean isAssigned(String var) {
      String s = var + " = ";
      int i = code.indexOf(s);
      return i >= 0;
   }
   public int assignedValueStart(String var) {
      String s = var + " = ";
      return code.indexOf(s) + s.length();
   }
   public int assignedValueEnd(String var) {
      String s = var + " = ";
      int i = code.indexOf(s) + s.length();
      if (code.charAt(i) == '-')
         i++;
      while (Parse.isNumeric(code.charAt(i)))
         i++;
      return i;
   }

   public void updateEvals(int lo[], int hi[], int n) {
      evals.text = text;
      evals.update(lo, hi, n);
   }

   public void setNShared(int nShared) {
      this.nShared = nShared;
   }

   public int nControls() { return 0; }
   public int getControlLo(int n) { return 0; }
   public int getControlHi(int n) { return 0; }
   public int getControlType(int n) { return 0; }
   public String getControlCode(int n) { return ""; }
   public String getControlData(int n) { return ""; }
   public String getControlLabel(int n) { return ""; }
   public int getCodeIndex() { return codeIndex; }

   public void setTime(double time) { this.time = time; }
   public void setCommonCode(String commonCode) {
      if (! commonCode.equals(this.commonCode))
         isCommonCodeDamage = true;
      this.commonCode = commonCode;
   }
   public void setCode(String code) { this.code = code; }
   public void setCodeIndex(int i) { codeIndex = i; }
   public void render(int[] pix) { }
   public void update(Graphics g) {
      g.drawImage(image, 0, 0, null);
   }
   public Image getImage() { return image; }
   public int[] getPix() { return pix; }
   public void run() {
      while (true) {
         mis.newPixels(0, 0, width, height, true);
         render(pix);
         try { thread.sleep(30); } catch (Exception e) { }
      }
   }
   int pix[];
   Image image;
   String commonCode = "", code = "", text;
   int codeIndex;
   double time;
   Component component;
   MemoryImageSource mis;
   Thread thread = new Thread(this);
   boolean isCommonCodeDamage = true;
   boolean isSharedDamage = true;
   boolean isOnScreen = false;
   Evals evals = new Evals();
   Images images;
   draw.Draw draw = new draw.Draw();
   Font font;
   int nShared;
   String[] shared;
}

