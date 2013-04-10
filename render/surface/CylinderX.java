
package render.surface;

//import perlin.math.*;
import render.*;

public class CylinderX extends Quadric
{
   public CylinderX() {
      setCoefficients(eps, 0, 1, 0, 0, 1, 0, 0, 0, -1);
   }
}

