
package render.surface;

//import perlin.math.*;

public class SlabZ extends Quadric
{
   public SlabZ() {
      setCoefficients(eps, 0, eps, 0, 0, 1, 0, 0, 0, -1);
   }
}

