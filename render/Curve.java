
package render;

public class Curve {

   public Curve() { }

   public Curve(double[][] data) {
      clearValues();
      for (int n = 0 ; n < data.length ; n++)
         for (int j = 0 ; j < data[n].length ; j++)
	    setValue(n, j, data[n][j]);
   }

// PUBLIC METHODS

   public Curve copy() {
      Curve c = new Curve();
      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < nCols() ; j++)
            c.setValue(i, j, dd[i][j]);
      return c;
   }

   public Curve scale(double s) {
      int n = Math.min(vec.length, nCols());
      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < n ; j++)
	    dd[i][j] *= s;
      isChanged = true;
      return this;
   }

   public Curve center() {
      for (int j = 0 ; j < nCols ; j++)
         vec[j] = 0;

      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < nCols() ; j++)
	    vec[j] += dd[i][j];

      for (int j = 0 ; j < nCols ; j++)
         vec[j] /= nRows();

      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < nCols() ; j++)
	    dd[i][j] -= vec[j];

      return this;
   }

   public Curve translate(double vec[]) {
      int n = Math.min(vec.length, nCols());
      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < n ; j++)
	    dd[i][j] += vec[j];
      isChanged = true;
      return this;
   }

   public boolean equals(Curve c) {
      if (c == null || nRows() != c.nRows() || nCols() != c.nCols())
         return false;
      for (int i = 0 ; i < nRows() ; i++)
         for (int j = 0 ; j < nCols() ; j++)
            if (dd[i][j] != c.dd[i][j])
	       return false;
      return true;
   }

   public void enableFunction(boolean tf) {
      isFunctionEnabled = tf;
   }

   public boolean isFunction() {
      return isFunctionEnabled && isFunction;
   }

   boolean isLoop() { return isLoop; }

   public int nRows() { return nRows; }

   public int nCols() { return nCols; }

   public void clearValues() {
      nRows = 0;
      nCols = 0;
      isChanged = true;
   }

   public void setValue(int i, int j, double value) {
      dd[i][j] = value;
      nRows = Math.max(nRows, i + 1);
      nCols = Math.max(nCols, j + 1);
      isChanged = true;
   }

   public void findMidpoint(int n, double dst[]) {
      update();
      eval(n + 0.5, dst);
   }

   public double[] eval(double t) {
      if (result == null || result.length < nCols)
         result = new double[nCols];
      eval(t, result);
      return result;
   }

   public void eval(double t, double dst[]) {
      for (int j = 0 ; j < dst.length ; j++)
         dst[j] = eval(t, j);
   }

   public double eval(double t, int j) {
      update();
      if (nRows < 2)
        return dd[0][j];

      int n = 0;
      if (isLoop) {
         if (t < 0) {
	    n = nRows - 1 + ((int)t - 1) % (nRows - 1);
            t = t % 1.0 + 1.0;
         }
	 else {
            n = (int)t % (nRows - 1);
            t %= 1.0;
         }
      }
      else {
         n = (int)t;
         t %= 1.0;
         if (t < 0) {
            n = 0;
	    t = 0;
         }
         else if (n >= nRows - 1) {
            n = nRows - 2;
	    t = 0.9999;
         }
      }
      double[] C = this.C[n][j];
      return t * (t * (t * C[0] + C[1]) + C[2]) + C[3];
   }

// PRIVATE METHODS AND DATA

   void update() {
      if (isChanged) {
         isLoop = nCols > 1;
	 if (isLoop)
	    for (int j = 0 ; j < dd[0].length ; j++)
	       if (Math.abs(dd[0][j] - dd[nRows-1][j]) > 0.001) {
	          isLoop = false;
	          break;
	       }
         computeTangentVectors();
         for (int n = 0 ; n < nRows - 1 ; n++)
            computeCubicCoefficients(n, C[n]);
         isChanged = false;
      }
   }

   void computeTangentVectors() {
       double[][] src = dd;
       double[][] dst = ddd;

       if (nRows < 2) {
          for (int j = 0 ; j < nCols ; j++)
             dst[0][j] = j == 0 ? 1.0 : 0.0;
          isFunction = true;
          return;
       }

       for (int n = 0 ; n < nRows - 1 ; n++) {
          double normSquared = 0;
          for (int j = 0 ; j < nCols ; j++) {
             dst[n][j] = src[n + 1][j] - src[n][j];
             normSquared += dst[n][j] * dst[n][j];
          }
          for (int j = 0 ; j < nCols ; j++)
             dst[n][j] /= normSquared;
       }

       for (int j = 0 ; j < nCols ; j++)
          dst[nRows - 1][j] = dst[nRows - 2][j];

       for (int n = nRows - (isLoop ? 3 : 2) ; n >= 0 ; n--)
          for (int j = 0 ; j < nCols ; j++)
             dst[n + 1][j] += dst[n][j];

       if (isLoop)
          for (int j = 0 ; j < nCols ; j++)
             dst[0][j] = dst[nRows - 1][j] = dst[0][j] + dst[nRows - 1][j];

       if (nCols > 1)
          for (int n = 0 ; n < nRows ; n++)
             normalize(dst[n], nCols);
       else
          for (int n = 0 ; n < nRows ; n++)
             dst[n][0] = 1.0;

       isFunction = nCols < 2;

       if (! isLoop) {
          reflect(dst[1], dst[0], dst[0], nCols);
          reflect(dst[nRows - 2], dst[nRows - 1], dst[nRows - 1], nCols);

	  if (nCols > 1) {
             for (int n = 0 ; n < nRows - 1 ; n++)
                if (src[n+1][0] < src[n][0])
                   return;

             for (int n = 0 ; n < nRows ; n++)
                dst[n][0] = Math.max(dst[n][0], 0.0);
             isFunction = true;
          }
       }
   }

   double dot(double a[], double b[], int n) {
      double product = 0;
      for (int i = 0 ; i < n ; i++)
         product += a[i] * b[i];
      return product;
   }

   double norm(double v[], int n) {
      return Math.sqrt(dot(v, v, n));
   }

   void normalize(double v[], int n) {
      double norm = norm(v, n);
      for (int i = 0 ; i < n ; i++)
         v[i] /= norm;
   }

   void reflect(double src[], double normal[], double dst[], int n) {
      double innerProduct = dot(src, normal, n);
      for (int i = 0 ; i < n ; i++)
         dst[i] = 2 * innerProduct * normal[i] - src[i];
   }

   void computeCubicCoefficients(int n, double[][] C) {
      double[][] H = Geometry.Hermite;

      //for (int j = 0 ; j < dd[n].length ; j++)
      for (int j = 0 ; j < nCols ; j++)
         vec[j] = dd[n+1][j] - dd[n][j];
      //double cordLength = norm(vec, dd[n].length);
      double cordLength = norm(vec, nCols);

      double scale = 1.0;
      if (isFunction())
         scale = Math.max(0.25, Math.min(1.0, vec[0] / (Math.abs(vec[1]) + 0.01)));

      //for (int j = 0 ; j < dd[n].length ; j++) {
      for (int j = 0 ; j < nCols ; j++) {
         double scaleTangent = 1.0;

	 if (nCols == 1) {
	    scaleTangent = 0.15;
	 }
	 else {
            scaleTangent = cordLength * 1.15;
	    switch (nRows) {
	    case 3: scaleTangent *= 1.20; break;
	    case 4: scaleTangent *= 1.15; break;
	    case 6: scaleTangent *= 0.95; break;
	    }
            if (isFunction())
               scaleTangent *= scale;
         }

         double p0 = dd[n][j];
         double p1 = dd[n+1][j];

         double r0 = scaleTangent * ddd[n][j];
         double r1 = scaleTangent * ddd[n+1][j];

         for (int i = 0 ; i < 4 ; i++)
            C[j][i] = H[i][0] * p0 + H[i][1] * p1 + H[i][2] * r0 + H[i][3] * r1;
      }
   }

   public static final int MAXROWS = 100;
   public static final int MAXCOLS = 6;
   protected int nRows = 0, nCols = 0;
   public double[][] dd = new double[MAXROWS][MAXCOLS];
   //public double[] d0 = new double[MAXCOLS];
   //public double[] ds = new double[MAXCOLS];
   protected double[] vec = new double[MAXCOLS];
   protected double[] result = new double[MAXCOLS];
   protected double[][] ddd = new double[MAXROWS][MAXCOLS];
   protected double C[][][] = new double[MAXROWS][MAXCOLS][4];

   boolean isFunction, isFunctionEnabled, isLoop, isChanged;
}

