
package render.surface;

//import perlin.math.*;

public class SlabY extends Quadric
{
   public SlabY() {
      setCoefficients(eps, 0, 1, 0, 0, eps, 0, 0, 0, -1);
   }
}

