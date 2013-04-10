
package test;

import java.awt.*;
import render.*;

public class ExampleRenderableRegion extends RenderableRegion implements Runnable
{
   public boolean keyRelease(int key) {
      if (key == ' ') {
         System.err.println("aha");
         return true;
      }
      return false;
   }

   public void update(Graphics g) {
      g.setFont(font);
      g.setColor(Color.orange);
      g.drawString("Hello there", width / 2, height * 2 / 5);
      g.drawImage(image, 0, 0, null);
      g.drawString("and goodbye", width / 2, height * 3 / 5);
   }

   public void render(int[] pix) {
      if (render == null) {
         render = new Renderer();
         renderRgb = render.init(width, height);

         render.addLight( 1, 1, 1, 1, 1, 1);
         render.addLight(-1,-1,-1, 1, 1, 1);
	 Material m = new Material();
	 m.setAmbient(.2,0,0);
	 m.setDiffuse(.8,0,0);
	 m.setSpecular(1,1,1,10);
         Geometry world = render.getWorld();
	 //shape = world.add().globe(32, 16);
	 shape = world.add().cylinder(12);
	 //shape.superquadric(8,0);
	 shape.setMaterial(m);
      }

      Matrix m = shape.getMatrix();
      m.identity();
      m.rotateY(theta += 0.03);
      render.render();
      System.arraycopy(renderRgb, 0, pix, 0, renderRgb.length);
   }

   Font font = new Font("Helvetica", Font.BOLD, 20);
   Renderer render;
   int[] renderRgb;
   Geometry shape;
   double theta = 0.0;
}

