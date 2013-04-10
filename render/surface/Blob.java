
package render.surface;

//import jmm.concurrent.ALock;
//import jmm.lang.*;
//import perlin.math.*;
import render.*;

public final class Blob 
{
   public final static int MATERIAL_BLENDING  = 0;
   public final static int MATERIAL_WEIGHT    = 1;
   public final static int SHAPE_ARTICULATION = 2;
   public final static int SHAPE_BLENDING     = 3;
   public final static int SHAPE_WEIGHT       = 4;
   public final static int NVALUES            = 5;

   // UNIT CUBE VERTICES, EDGES AND PLANES

   static double cv[][] = {{-1,-1,-1},{1,-1,-1},{-1,1,-1},{1,1,-1},{-1,-1,1},{1,-1,1},{-1,1,1},{1,1,1}};
   static int    cp[][] = {{-1,0,0,-1},{1,0,0,-1},{0,-1,0,-1},{0,1,0,-1},{0,0,-1,-1},{0,0,1,-1}};
   static int    ce[][] = {{0,1},{2,3},{4,5},{6,7}, {0,2},{1,3},{4,6},{5,7}, {0,4},{1,5},{2,6},{3,7}};

   public Blob(Surface surface) {
      this.surface = surface;
      this.blobId = nextBlobId++;

      for (int i = 0 ; i < NVALUES ; i++)
         values[i] = 1.0;
   }

   public Surface getSurface() {
      return surface;
   }
   
   protected final Quadric getIQuadric(int n) { return iQuadrics[n]; }
   protected final Quadric getOQuadric(int n) { return oQuadrics[n]; }
   
   public void addQuadric() {
      addQuadric(new Quadric());
   }
   
   public void addQuadric(Quadric quadric) {
      if ((nQuadrics+1) == iQuadrics.length) {
         Quadric old[] = iQuadrics;
         iQuadrics = new Quadric[old.length*2];
         System.arraycopy(old, 0, iQuadrics, 0, old.length);
         
         old = oQuadrics;
         oQuadrics = new Quadric[old.length*2];
         System.arraycopy(old, 0, oQuadrics, 0, old.length); 
         
         oValues = new double[old.length*2];
      }
      
      iQuadrics[nQuadrics] = quadric;
      oQuadrics[nQuadrics] = quadric.copy();
      
      nQuadrics++;
   }
   
   public void setRounded(double rounded) {
      for (int i=0; i < nQuadrics; i++) {
         iQuadrics[i].setRounded(rounded);
         oQuadrics[i].setRounded(rounded);
      }
   }

   public void setValue(int index, double value) {
      values[index] = value;
   }

   public double getValue(int n) {
      return values[n];
   }

   protected void setMatrix(Matrix m) {
      this.m.copy(m);
   }
   
   public Matrix getMatrix() { 
      return m;
   }

   protected double f(double x, double y, double z) {
      for (int i = 0 ; i < nQuadrics; i++)
         if ((oValues[i] = oQuadrics[i].eval(x, y, z)) >= 0)
            return 0;

      double a = 0, b = 0;
      for (int i = 0 ; i < nQuadrics; i++) {
         a = intersection(a, 1 + iQuadrics[i].eval(x, y, z));
         b = intersection(b, 1 + oValues[i]);
      }
      return combine(a, b);
   }

   protected double f(double x) {
      for (int i = 0; i < nQuadrics; i++)
         if ((oValues[i] = oQuadrics[i].eval(x)) >= 0)
            return 0;

      double a = 0, b = 0;
      for (int i = 0 ; i < nQuadrics; i++) {
         a = intersection(a, 1 + iQuadrics[i].eval(x));
         b = intersection(b, 1 + oValues[i]);
      }
      return combine(a, b);
   }

   protected void update() {
      m.svd(scales);
      double s = values[SHAPE_BLENDING];
      double sx = 1 + s / scales[0], sy = 1 + s / scales[1], sz = 1 + s / scales[2];

      mI.copy(m);
      mO.copy(m);
      mO.scale(sx, sy, sz);
      
      for (int n = 0 ; n < cv.length ; n++)
         mO.transform(cv[n][0], cv[n][1], cv[n][2], boundingVertices[n]);

      m.invert(mO);
      m.transpose();
      for (int n = 0 ; n < cp.length ; n++)
         m.transform(cp[n][0], cp[n][1], cp[n][2], cp[n][3], boundingPlanes[n]);

      for (int i=0; i < nQuadrics; i++) {
         iQuadrics[i].update(mI, 1.0 - values[SHAPE_ARTICULATION]);
         oQuadrics[i].update(mO, 1.0 - values[SHAPE_ARTICULATION]);
   
         iQuadrics[i].scaleCoefficients(1.0 / Math.sqrt(Math.max(Math.max(sx, sy), sz)));
      }
   }

   protected int getCubeYBounds(double z, double yb[]) {
      yb[0] = 10000;
      yb[1] = -yb[0];
      for (int n = 0 ; n < ce.length ; n++) {
         double a[] = boundingVertices[ce[n][0]];
         double b[] = boundingVertices[ce[n][1]];
         if ((a[2] >= z) != (b[2] >= z)) {
            double t = (z - a[2]) / (b[2] - a[2]);
            double y = a[1] + t * (b[1] - a[1]);
            yb[0] = Math.min(yb[0], y);
            yb[1] = Math.max(yb[1], y);
         }
      }
      return 2;
   }

   protected int getXBounds(double t[]) { 
//      if (isEllipsoid)
//         return oQuadrics[0].get3DXBounds(t);
//      else
         return getCubeBounds(0, t);
   }

   protected int getYBounds(double t[]) { 
//      if (isEllipsoid)
//         return oQuadrics[0].get3DYBounds(t);
//      else
         return getCubeBounds(1, t);
   }

   protected int getZBounds(double t[]) { 
//      if (isEllipsoid)
//         return oQuadrics[0].get3DZBounds(t);
//      else
         return getCubeBounds(2, t);
   }
   
   protected int computeYBounds(double z, double t[]) {
//      if (isEllipsoid) {
//         oQuadrics[0].setZ(z);
//         iQuadrics[0].setZ(z);
//         return oQuadrics[0].get2DYBounds(t);
//      }
//      else 
      {
         for (int n = 0 ; n < nQuadrics ; n++) {
            oQuadrics[n].setZ(z);
            iQuadrics[n].setZ(z);
         }
         return getCubeYBounds(z, t);
      }
   }

   // a x + b y + c z + d >= 0
   
   protected int computeXBoundsNew(double z, double y, double x[]) {
      x[0] = -1000.0;
      x[1] =  1000.0;
      for (int n = 0 ; n < boundingPlanes.length ; n++) {
         double p[] = boundingPlanes[n];
         double e = p[1] * y + p[2] * z + p[3];
         if (Math.abs(p[0]) > 0.001) {
            if (p[0] < 0)
               x[0] = Math.max(x[0], -e / p[0]);
            else
               x[1] = Math.min(x[1], -e / p[0]);
         }
      }
      if (x[0] >= x[1])
         return 0;

      for (int n = 0 ; n < nQuadrics ; n++) {
         oQuadrics[n].setY(y);
         iQuadrics[n].setY(y);
      }

      return 2;
   }

   protected int computeXBounds(double y, double t[]) {
      t[1] = 1000;
      t[0] = -t[1];
      for (int n = 0 ; n < nQuadrics ; n++) {
         oQuadrics[n].setY(y);
         iQuadrics[n].setY(y);
         if (oQuadrics[n].get1DXBounds(xb) == 0)
            return 0;
         t[0] = Math.max(t[0], xb[0]);
         t[1] = Math.min(t[1], xb[1]);
         if (t[0] >= t[1])
            return 0;
      }
      return 2;
   }

   private double combine(double a, double b) {
      double A = Math.sqrt(a);
      double B = Math.sqrt(b);
      double t = (1 - B) / (A - B);
      return t * t * t;
   }

   private double intersection(double a, double b) {
      if (a < .001)
         return b;
      double ratio = b / a;
      if (ratio <= r0) return a;
      if (ratio >= r1) return b;
      double t = (ratio - r0) / (r1 - r0);
      return Util.lerp(t, a, b) * (1 + (1 - r0) * t * (1 - t));
   }

   private int getCubeBounds(int axis, double t[]) {
      t[0] = 10000;
      t[1] = -t[0];
      for (int n = 0 ; n < boundingVertices.length ; n++) {
         t[0] = Math.min(t[0], boundingVertices[n][axis]);
         t[1] = Math.max(t[1], boundingVertices[n][axis]);
      }
      return 2;
   }
   
   private double ab[] = new double[3];
   private double roots2[] = new double[2];
   private double v[] = new double[3];
   private double w[] = new double[3];
   private double dot(double v[], double w[]) { return v[0]*w[0] + v[1]*w[1] + v[2]*w[2]; }

   
   public boolean rayIntersect(double a[], double b[], double roots[]) {
      mIinv.invert(mI);

      mIinv.transform(a[0], a[1], a[2], v);
      mIinv.transform(b[0], b[1], b[2], w);
      for (int i = 0 ; i < 3 ; i++)
         w[i] -= v[i];

      double A, B, C;
      
      switch (nQuadrics) {
      case 1:     // SPHERE  FixMe: This is a hard-coded hack
         A = dot(w, w);
         B = dot(v, w) * 2;
         C = dot(v, v) - 1.0;
         return quadraticRoots(A, B, C, roots);
      case 2:     // CYLINDER  FixMe: This is a hard-coded hack
         A =  w[0] * w[0] + w[1] * w[1];
         B = (v[0] * w[0] + v[1] * w[1]) * 2;
         C =  v[0] * v[0] + v[1] * v[1] - 1;
         if (quadraticRoots(A, B, C, roots))
            if (quadraticPlaneRoots(v[2], w[2], roots2))
               return intersect(roots, roots2);
         break;
      case 3:     // CUBE  FixMe: This is a hard-coded hack
         if (quadraticPlaneRoots(v[0], w[0], roots))
            if (quadraticPlaneRoots(v[1], w[1], roots2))
               if (intersect(roots, roots2))
                  if (quadraticPlaneRoots(v[2], w[2], roots2))
                     return intersect(roots, roots2);
         break;
      }
      return false;
   }
   
   private static boolean intersect(double a[], double b[]) {
      a[0] = Math.max(a[0], b[0]);
      a[1] = Math.min(a[1], b[1]);
      return a[0] < a[1];
   }
   
   private static boolean quadraticPlaneRoots(double v, double w, double roots[]) {
      double A = w * w;
      double B = v * w * 2;
      double C = v * v - 1;
      return quadraticRoots(A, B, C, roots);
   }

   private static boolean quadraticRoots(double A, double B, double C, double roots[]) {
      double dd = B * B - 4 * A * C;
      if (dd < 0)
         return false;

      double d = Math.sqrt(dd);
      roots[0] = (-B - d) / (2 * A);
      roots[1] = (-B + d) / (2 * A);
      return true;
   }
   
   public int getBlobId() { return blobId; }

   private double  boundingVertices[][] = new double[8][3];
   private double  boundingPlanes[][] = new double[6][4];
   private Quadric iQuadrics[] = new Quadric[3];
   private Matrix  m = new Matrix();
   private Matrix  mI = new Matrix();
   private Matrix  mO = new Matrix();
   private Matrix  mIinv = new Matrix();
   private int     nQuadrics = 0;
   private Quadric oQuadrics[] = new Quadric[3];
   private double  oValues[] = new double[3];
   private double  r0 = 0.7, r1 = 1.0 / r0;
   private double  scales[] = new double[3];
   private double  values[] = new double[NVALUES];
   private double  xb[] = new double[2];
   private Surface surface;
   private int blobId;
   private static int nextBlobId = 0;
   
   public void setShape(double x, double y, double z) {
      getIQuadric(0).setCoefficients(1, z, y);
      getIQuadric(1).setCoefficients(z, 1, x);
      getIQuadric(2).setCoefficients(y, x, 1);

      getOQuadric(0).setCoefficients(1, z, y);
      getOQuadric(1).setCoefficients(z, 1, x);
      getOQuadric(2).setCoefficients(y, x, 1);
   }
}
