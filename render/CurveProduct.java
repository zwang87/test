
package render;

public class CurveProduct
{
   boolean isVerbose = false;
   double[] aVec = new double[3], bVec;
   double[] aVec2 = new double[3];
   double[] u = {0,0,1};
   double[] v = new double[3];
   double[] w = new double[3];

   public CurveProduct(Curve a, int na, Curve b, int nb) {
      this(a, null, na, b, null, nb);
   }

   public CurveProduct(Curve a, Matrix ma, int na, Curve b, Matrix mb, int nb) {
      data = new double[na][nb][3];
      a.update();
      b.update();

      for (int i = 0 ; i < na ; i++) {
         double t = (double)i * (a.nRows() - 1) / (na - 1);
	 t = Math.min(t, a.nRows() - 1 - epsilon);
         a.eval(t, aVec);
         a.eval(t + epsilon, aVec2);

         if (ma != null) {
	    ma.transform(aVec, aVec);
	    ma.transform(aVec2, aVec2);
         }

	 for (int k = 0 ; k < 3 ; k++)
	    w[k] = (aVec[k] - aVec2[k]) / epsilon;
	 Vec.cross(w, u, v);
         matrix.setOrientation(u, v, w);

	 for (int k = 0 ; k < 3 ; k++)
	    matrix.set(k, 3, aVec[k]);

	 for (int j = 0 ; j < nb ; j++) {
            bVec = b.eval((double)j * (b.nRows() - 1) / (nb - 1));

	    if (mb != null)
	       mb.transform(bVec, bVec);

	    matrix.transform(bVec, data[i][j]);
         }
      }
   }

   public double[] get(int i, int j) {
      return data[i][j];
   }

   double[][][] data;
   Matrix matrix = new Matrix();
   static double epsilon = 0.001;
}

