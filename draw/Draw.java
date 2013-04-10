
package draw;

import java.awt.*;

public class Draw {
   public void setGraphics(Graphics g) {
      this.g = g;
      Rectangle bounds = g.getClipBounds();

      if (bounds != null) {
         width = g.getClipBounds().width;
         height = g.getClipBounds().height;
      }
   }

   public void setColor(Color color) {
      g.setColor(color);
   }

   public void setFont(Font font) {
      g.setFont(font);
   }

   public int s(double t) { return (int)(height * 0.5 * t); }
   public int y(double t) { return (int)(height * (-0.5 * t + 0.5)); }
   public int x(double t) { return (int)(height * ( 0.5 * t + 0.5 * width / height)); }

   public double fx(int x) { return ((double)x / height - 0.5 * width / height) / 0.5; }
   public double fy(int y) { return ((double)y / height - 0.5) / -0.5; }

   public void draw(Object object, double x, double y, double width) {
      draw(object, x, y, width, 0);
   }

   public void draw(Object object, double x, double y, double width, double height) {
      if (object instanceof Image) {
         Image image = (Image)object;
         if (width == 0)
            width = height * image.getWidth(null) / image.getHeight(null);
         else if (height == 0)
            height = width * image.getHeight(null) / image.getWidth(null);
         int rw = s(width);
         int rh = s(height);
         g.drawImage(image, x(x) - rw, y(y) - rh, 2 * rw, 2 * rh, null);
      }
   }

   public void drawImage(Image image, int x, int y, int width, int height) {
      g.drawImage(image, x, y, width, height, null);
   }

   public void drawDisk(double x, double y, double r) {
      drawDisk(x, y, r, r);
   }

   public void drawDisk(double x, double y, double rx, double ry) {
      int rw = s(rx);
      int rh = s(ry);
      g.drawOval(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh);
   }

   public void fillDisk(double x, double y, double r) {
      fillDisk(x, y, r, r);
   }

   public void fillDisk(double x, double y, double rx, double ry) {
      int rw = s(rx);
      int rh = s(ry);
      g.fillOval(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh);
   }

   public void test() {
   }

   public void drawArrow(double ax, double ay, double bx, double by, double r) {
      drawArrow(x(ax), y(ay), x(bx), y(by), s(r));
   }

   public void fillArrow(double ax, double ay, double bx, double by, double r) {
      fillArrow(x(ax), y(ay), x(bx), y(by), s(r));
   }

   public void drawThickLine(double ax, double ay, double bx, double by, double r) {
      drawThickLine(x(ax), y(ay), x(bx), y(by), s(r));
   }

   public void fillThickLine(double ax, double ay, double bx, double by, double r) {
      fillThickLine(x(ax), y(ay), x(bx), y(by), s(r));
   }

   public void drawBox(double x, double y, double r) {
      drawBox(x, y, r, r);
   }

   public void drawBox(double x, double y, double rx, double ry) {
      drawBox(x, y, rx, ry, 0);
   }

   public void drawBox(double x, double y, double rx, double ry, double e) {
      drawBox(x, y, rx, ry, e, e);
   }

   public void drawBox(double x, double y, double rx, double ry, double ex, double ey) {
      int rw = s(rx);
      int rh = s(ry);
      if (ex == 0)
         g.drawRect(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh);
      else
         g.drawRoundRect(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh, 2 * s(ex), 2 * s(ey));
   }

   public void fillBox(double x, double y, double r) {
      fillBox(x, y, r, r);
   }

   public void fillBox(double x, double y, double rx, double ry) {
      fillBox(x, y, rx, ry, 0);
   }

   public void fillBox(double x, double y, double rx, double ry, double e) {
      fillBox(x, y, rx, ry, e, e);
   }

   public void fillBox(double x, double y, double rx, double ry, double ex, double ey) {
      int rw = s(rx);
      int rh = s(ry);
      if (ex == 0)
         g.fillRect(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh);
      else
         g.fillRoundRect(x(x) - rw, y(y) - rh, 2 * rw, 2 * rh, 2 * s(ex), 2 * s(ey));
   }

   public void drawText(String s, double x, double y) {
      g.drawString(s, x(x) - stringWidth(s) / 2, y(y) + fontHeight() / 3);
   }

   public void drawString(String s, int x, int y) {
      g.drawString(s, x, y);
   }

   public void drawLine(int ax, int ay, int bx, int by) {
      g.drawLine(ax, ay, bx, by);
   }

   public void drawRect(int x, int y, int width, int height) {
      g.drawRect(x, y, width, height);
   }

   public void fillRect(int x, int y, int width, int height) {
      g.fillRect(x, y, width, height);
   }

   public void drawRoundRect(int x, int y, int width, int height, int rx, int ry) {
      g.drawRoundRect(x, y, width, height, rx, ry);
   }

   public void fillRoundRect(int x, int y, int width, int height, int rx, int ry) {
      g.fillRoundRect(x, y, width, height, rx, ry);
   }

   public void drawOval(int x, int y, int width, int height) {
      g.drawOval(x, y, width, height);
   }

   public void fillOval(int x, int y, int width, int height) {
      g.fillOval(x, y, width, height);
   }

   public void fill3DRect(int x, int y, int width, int height, boolean state) {
      g.fill3DRect(x, y, width, height, state);
   }

   public void drawPolygon(int[] X, int[] Y, int n) {
      g.drawPolygon(X, Y, n);
   }

   public void fillPolygon(int[] X, int[] Y, int n) {
      g.fillPolygon(X, Y, n);
   }

   public void drawThickLine(int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= w * w)
         g.drawPolygon(X, Y, createThickLine(ax, ay, bx, by, w, X, Y));
   }

   public void fillThickLine(int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= w * w)
         g.fillPolygon(X, Y, createThickLine(ax, ay, bx, by, w, X, Y));
   }

   public void drawArrow(int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= 16 * w * w)
         g.drawPolygon(X, Y, createArrow(ax, ay, bx, by, w, X, Y));
   }

   public void fillArrow(int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= 16 * w * w)
         g.fillPolygon(X, Y, createArrow(ax, ay, bx, by, w, X, Y));
   }

   public void drawTextBubble(String label, int x, int y, int width, int height) {
      Color color = g.getColor();
      int r = 20;
      g.fillRoundRect(x - width / 2, y - height / 2, width, height, r, r);
      g.setColor(Color.black);
      g.drawRoundRect(x - width / 2, y - height / 2, width, height, r, r);
      g.drawString(label, x - stringWidth(label) / 2, y + fontHeight() / 4);
      g.setColor(color);
   }

   public static int createThickLine(int ax, int ay, int bx, int by, int w, int X[], int Y[]) {
      double theta = Math.atan2(by - ay, bx - ax);
      double cos = Math.cos(theta);
      double sin = Math.sin(theta);

      X[0] = (int)(ax - w * sin);
      X[1] = (int)(bx - w * sin);
      X[2] = (int)(bx + w * sin);
      X[3] = (int)(ax + w * sin);

      Y[0] = (int)(ay + w * cos);
      Y[1] = (int)(by + w * cos);
      Y[2] = (int)(by - w * cos);
      Y[3] = (int)(ay - w * cos);

      return 4;
   }

   public static int createArrow(int ax, int ay, int bx, int by, int w, int X[], int Y[]) {
      double theta = Math.atan2(by - ay, bx - ax);
      double cos = Math.cos(theta);
      double sin = Math.sin(theta);

      X[0] = (int)(ax - w * sin);
      X[1] = (int)(bx - w * sin     - w * cos * 4);
      X[2] = (int)(bx - w * sin * 3 - w * cos * 4);
      X[3] = (int)(bx);
      X[4] = (int)(bx + w * sin * 3 - w * cos * 4);
      X[5] = (int)(bx + w * sin     - w * cos * 4);
      X[6] = (int)(ax + w * sin);

      Y[0] = (int)(ay + w * cos);
      Y[1] = (int)(by + w * cos     - w * sin * 4);
      Y[2] = (int)(by + w * cos * 3 - w * sin * 4);
      Y[3] = (int)(by);
      Y[4] = (int)(by - w * cos * 3 - w * sin * 4);
      Y[5] = (int)(by - w * cos     - w * sin * 4);
      Y[6] = (int)(ay - w * cos);

      return 7;
   }

   public int stringWidth(String s) {
      return g.getFontMetrics().stringWidth(s);
   }

   public int fontHeight() {
      return g.getFontMetrics().getHeight();
   }

   int X[] = new int[7];
   int Y[] = new int[7];
   int width, height;
   Graphics g;
}


