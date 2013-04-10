
package render.surface;

//import perlin.math.*;
import render.*;

public class CylinderY extends Quadric
{
   public CylinderY() {
      setCoefficients(1, 0, eps, 0, 0, 1, 0, 0, 0, -1);
   }
}

