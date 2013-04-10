
package render.surface;

//import perlin.math.*;
import render.*;

public class CylinderZ extends Quadric
{
   public CylinderZ() {
      setCoefficients(1, 0, 1, 0, 0, eps, 0, 0, 0, -1);
   }
}

