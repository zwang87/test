
package test;

import java.awt.*;

public class ColorSelector extends Control
{
   public void setLocation(int x, int y) {
      super.setLocation(x, y);
      y0 = y + height - b;
      y1 = y + b;
   }

   public void setSize(int width, int height) {
      super.setSize(width, height);
      b = width / 4;
      y0 = y + height - b;
      y1 = y + b;
   }

   public boolean mouseMove(Event e, int x, int y) {
      return false;
   }
   public boolean mouseDown(Event e, int x, int y) {
      if (isVisible && (isPressed = contains(x, y))) {
         i = 3 * (x - this.x) / width;
         setValue(i, (double)(y - y0) / (y1 - y0));
         return true;
      }
      return false;
   }
   public boolean mouseDrag(Event e, int x, int y) {
      if (isPressed)
         setValue(i, (double)(y - y0) / (y1 - y0));
      return isPressed;
   }
   public boolean mouseUp(Event e, int x, int y) {
      boolean wasPressed = isPressed;
      isPressed = false;
      if (wasPressed)
         setValue(i, (double)(y - y0) / (y1 - y0));
      return wasPressed;
   }

   public void setValue(int i, double value) {
      color[i] = Math.max(0.0, Math.min(1.0, value));
      doAction();
   }

   public String unparse() {
      String str = "";
      switch (type) {
      case SET_COLOR:
         str = "setColor(" + round(color[0]) + ","
                           + round(color[1]) + ","
                           + round(color[2]) ;
         break;
      case NEW_COLOR:
         str = "new Color(" + (int)(255 * color[0]) + ","
                            + (int)(255 * color[1]) + ","
                            + (int)(255 * color[2]) ;
         break;
      default:
         return "";
      }
      int length = str.length();

      text = text.substring(0, lo) + str + text.substring(hi, text.length());
      hi = lo + length;

      return text;
   }

   public boolean parse(String src) {
      isVisible = false;
      text = src;

      if ((lo = src.indexOf("setColor(")) >= 0) {
         if ((hi = src.indexOf(")", lo)) == -1)
            return false;
         String[] args = src.substring(lo + 9, hi).split(",");
         if (args.length != 3)
	    return false;

         try {
            for (int i = 0 ; i < 3 ; i++) {
               color[i] = java.lang.Double.parseDouble(args[i]);
               color[i] = Math.max(0.0, Math.min(1.0, color[i]));
            }
         } catch (Exception e) { return false; }

         type = SET_COLOR;
         return isVisible = true;
      }

      if ((lo = src.indexOf("new Color(")) >= 0) {
         if ((hi = src.indexOf(")", lo)) == -1)
            return false;

         String[] args = src.substring(lo + 10, hi).split(",");
         if (args.length != 3)
	    return false;

         try {
            for (int i = 0 ; i < 3 ; i++) {
               color[i] = java.lang.Integer.parseInt(args[i]) / 255.0;
               color[i] = Math.max(0.0, Math.min(1.0, color[i]));
            }
         } catch (Exception e) { return false; }

         type = NEW_COLOR;
         return isVisible = true;
      }

      return false;
   }

   public void update(Graphics g) {
      if (isVisible) {
         g.setColor(bgColor);
         g.fill3DRect(x, y, width, height, true);

         int r = b / 2;
         for (int i = 0 ; i < 3 ; i++) {
            int lx = x + width / 2 + (i - 1) * (width / 2 - b);

            int cy = (int)(y0 + color[i] * (y1 - y0));

            g.setColor(i==0 ? red : i==1 ? grn : blu);
            g.fillRect(lx - r, cy - r, 2 * r, 2 * r);

            g.setColor(grooveColor);
            g.fillRect(lx - 1, y1, 3, (cy - r) - y1);
            g.fillRect(lx - 1, cy + r, 3, y0 - (cy + r));
         }
      }
   }

   static final int SET_COLOR = 0;
   static final int NEW_COLOR = 1;

   String text;
   boolean isPressed;
   int i, b, y0, y1, type, lo, hi;
   double[] color = new double[3];
   Color grooveColor = new Color(0, 0, 0, 160);
   Color bgColor = new Color(240, 240, 240, 160);
   Color red = new Color(255,0,0,92);
   Color grn = new Color(0,255,0,92);
   Color blu = new Color(0,0,255,92);
}

