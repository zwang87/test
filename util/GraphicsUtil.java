
package util;

import java.awt.*;

public class GraphicsUtil
{
   public void drawArrow(Graphics g, int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= 16 * w * w)
         g.drawPolygon(X, Y, createArrow(ax, ay, bx, by, w, X, Y));
   }

   public void fillArrow(Graphics g, int ax, int ay, int bx, int by, int w) {
      if ((bx - ax) * (bx - ax) + (by - ay) * (by - ay) >= 16 * w * w)
         g.fillPolygon(X, Y, createArrow(ax, ay, bx, by, w, X, Y));
   }

   int createArrow(int ax, int ay, int bx, int by, int w, int X[], int Y[]) {
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

   int X[] = new int[7];
   int Y[] = new int[7];
}

