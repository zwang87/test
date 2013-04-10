
package test;

import java.awt.*;

public class ScrollRegion extends Region
{
   { width = 15; }

   public void mouseEnter(int x, int y) {
      hasFocus = true;
   }
   public void mouseExit(int x, int y) {
      hasFocus = false;
   }
   public boolean mouseMove(int x, int y) {
      return false;
   }
   public boolean mousePress(int x, int y) {
      if (hasFocus = contains(this.x + x, this.y + y))
         my = y;
      return hasFocus;
   }
   public void mouseDrag(int x, int y) {
      if (hasFocus) {
         setScrollY(scrollY - (int)((my - y) * scale));
	 my = y;
      }
   }
   public void mouseRelease(int x, int y) {
      boolean hadFocus = hasFocus;
      hasFocus = false;
   }

   public void setScale(double scale) { this.scale = scale; }
   public double getScale() { return scale; }

   public void setScrollY(int scrollY) {
      this.scrollY = Math.max(0, Math.min((int)((scale - 1) * height), scrollY));
   }

   public int getScrollY() { return scrollY; }

   public void update(Graphics g) {
      int sw = width - 2 * border - 3;
      int sh = (int)(height / scale) - border - 4;
      int sy = border + (int)(scrollY / scale) + 1;
      sh = Math.max(sh, sw);

      g.setColor(Color.white);
      g.fillRect(0, 0, width, height);
      g.setColor(bgColor);
      g.fillRect(0, 0, width, height);
      if (scale > 1.0) {
         g.setColor(hasFocus ? focusColor : fgColor);
         g.fillRoundRect(border + 2, sy, sw, sh, sw + 1, sw + 1);
      }
   }

   boolean hasFocus;
   int border = 2, scrollY = 0, my = 0;
   double scale = 1.0;
   Color focusColor = new Color(0, 0, 0, 100);
   Color fgColor = new Color(100, 100, 100, 80);
   Color bgColor = new Color(200, 200, 200, 128);
}

