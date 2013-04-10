
package test;

import java.awt.*;

public class Control extends Rect implements MouseHandler
{
   public boolean mouseDown (Event e, int x, int y) { return false; }
   public boolean mouseDrag (Event e, int x, int y) { return false; }
   public boolean mouseEnter(Event e, int x, int y) { return false; }
   public boolean mouseExit (Event e, int x, int y) { return false; }
   public boolean mouseMove (Event e, int x, int y) { return false; }
   public boolean mouseUp   (Event e, int x, int y) { return false; }
   public void doAction() { }
   public boolean parse(String str) { return false; }
   public String unparse() { return ""; }

   public boolean isVisible() {
      return isVisible;
   }

   public void setVisible(boolean isVisible) {
      this.isVisible = isVisible;
   }

   public void setScrollY(int scrollY) {
      this.scrollY = scrollY;
   }

   public void setLocation(int x, int y) {
      this.x = x;
      this.y = y;
   }

   public void setSize(int width, int height) {
      this.width = width;
      this.height = height;
   }

   public String name = "", valueString = "";
   boolean isOver, isPressed, isVerbose;
   int scrollY = 0;

   static String round(double value) {
      return "" + ((int)(100 * value) / 100.);
   }

   public static int stringWidth(Graphics g, String s) {
      return g == null ? 0 : g.getFontMetrics().stringWidth(s);
   }

   public static int fontHeight(Graphics g) {
      return g == null ? 0 : g.getFontMetrics().getHeight();
   }

   public void update(Graphics g) { }

   public void drawInfo(Graphics g) {
      int y = this.y - scrollY;

      Color saveColor = g.getColor();
      Font saveFont = g.getFont();

      if (isVerbose && isOver) {
         g.setFont(infoFont);
         int sw = stringWidth(g, name);
         int sx = x + (width - sw) / 2;
         int sy = y + height + fontHeight(g) / 4;
         int sh = fontHeight(g) - 1;
         drawInfo(g, name, x + width / 2, sy);
      }

      g.setFont(saveFont);
      g.setColor(saveColor);
   }

   static void drawInfo(Graphics g, String s, int x, int y) {
      drawInfo(g, s, x, y, -1, -1);
   }

   static void drawInfo(Graphics g, String s, int x, int y, int mx, int my) {
      Color saveColor = g.getColor();
      Font saveFont = g.getFont();
      g.setFont(infoFont);

      int fh = fontHeight(g);
      int w = stringWidth(g, s) + 3;
      int h = fh - 1;
      x -= w / 2;
      boolean focus = x <= mx && mx < x + w && y < my && my < y + h;
      g.setColor(focus ? infoBgMedium : infoBgLight);
      g.fillRect(x, y, w, h);
      g.setColor(infoBgDark);
      g.drawLine(x, y + h, x + w, y + h);
      g.drawLine(x + w, y, x + w, y + h);
      g.setColor(infoBgMedium);
      g.drawLine(x, y, x + w, y);
      g.drawLine(x, y, x, y + h);
      g.setColor(infoTextColor);
      g.drawString(s, x + 2, y + fh * 3 / 4);

      g.setFont(saveFont);
      g.setColor(saveColor);
   }

   boolean isVisible = true;

   static Color infoBgLight   = new Color(255    , 240    , 240    );
   static Color infoBgMedium  = new Color(255*4/5, 240*4/5, 240*4/5);
   static Color infoBgDark    = new Color(255*2/5, 240*2/5, 240*2/5);
   static Color infoTextColor = new Color(92, 0, 0);

   static Font infoFont = new Font("Courier", Font.BOLD, 15);

   static Color darkEdgeColor  = new Color(  0 * 18/20,   0 * 19/20,   0 * 20/20);
   static Color edgeColor      = new Color( 64 * 18/20,  64 * 19/20,  64 * 20/20);
   static Color lightEdgeColor = new Color(220 * 18/20, 220 * 19/20, 220 * 20/20);

   static Color darkBgColor    = new Color(180 * 14/16, 180 * 15/16, 180 * 16/16);
   static Color bgColor        = new Color(230 * 14/16, 230 * 15/16, 230 * 16/16);
   static Color lightBgColor   = new Color(250 * 38/40, 250 * 39/40, 250 * 40/40);
}

