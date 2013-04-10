
package render.surface;

//import perlin.math.*;
import render.*;

public class Ellipsoid extends Quadric
{
   public Ellipsoid() {
      setCoefficients(1, 0, 1, 0, 0, 1, 0, 0, 0, -1);
   }
}

