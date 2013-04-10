
package test;

import java.awt.*;

public class BlockRegion extends Region
{
   static final int X_SHIFT = 4;

   public boolean isOpen = true;

   public void update(Graphics g) {
      super.update(g);

      int x = X_SHIFT * level - 1;
      int b = border/2;

      g.setColor(edgeColor);
      g.drawOval(x, 0, b, b);
      g.drawOval(x, height-1 - b, b, b);

      g.setColor(Color.white);
      g.fillRect(x, b/2, border-x, height - b);
      g.fillRect(x+b/2, 0, border-(x+b/2), height);
   }

   public void overlay(Graphics g) {
      super.overlay(g);

      int x = X_SHIFT * level - 1;
      int b = border/2;

      g.setColor(edgeColor);
      g.fillRect(x+b/2, 0       , width-b/2, 1);
      g.fillRect(x+b/2, height-1, width-b/2, 1);
      g.drawLine(x, b/2, x, height - b/2);
   }

   void clearEvals() {
      nEvals = 0;
   }

   void addEval(int lo, int hi) {
      evalLo[nEvals] = lo;
      evalHi[nEvals] = hi;
      nEvals++;
   }

   int nEvals = 0, evalLo[] = new int[100], evalHi[] = new int[100];

   int border = 0;
   int level = 0;
   int colorStackTop = 0, italicLevel = 0, boldLevel = 0;

   static Color edgeColor = new Color(128,128,128);
}

