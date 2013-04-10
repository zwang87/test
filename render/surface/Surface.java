
package render.surface;

import render.*;

public class Surface extends Geometry
{
   public Surface() {
   }

   public boolean isBlob(Geometry g) {
      Blob blob = (Blob)g.clientObject;
      return blob != null && blob.getSurface() == this;
   }

   protected Blob newBlob(int n, Geometry g) {
      if      (g.type.equals("sphere"))       return newBlob(n, g, 1, 1, 1);
      else if (g.type.equals("cylinderMesh")) return newBlob(n, g, 0, 0, 1);
      else if (g.type.equals("cube"))         return newBlob(n, g, 0, 0, 0);
      else
         return null;
   }

   protected Blob newBlob(int n, Geometry g, double x, double y, double z) {
      blobs[n] = new Blob(this);
      return populateBlob(n, g, x, y, z);
   }
   
   private Blob populateBlob(int n, Geometry g, double x, double y, double z) {
      maxBlob = Math.max(maxBlob, n);
      blobs[n].addQuadric();
      blobs[n].addQuadric();
      blobs[n].addQuadric();
      blobs[n].setShape(x, y, z);
      g.clientObject = blobs[n];
      return blobs[n];
   }

   public Blob getBlob(int n) { return blobs[n]; }

   public void deleteBlob(int n) { blobs[n] = null; }
   public void setUnit(double unit) { this.unit = unit; }
   public void setFuzz(double fuzz) { this.fuzz = fuzz; }
   public void setParameter(int p, double value) {
      for (int n = 0 ; n <= maxBlob ; n++)
         if (blobs[n] != null)
            blobs[n].setValue(p, value);
   }

   public double unit() { return unit; }

   double failPoints[][] = new double[1000][3];
   int nFailPoints = 0;

   public void update() {
      nFailPoints = 0;

      double xMin = 1000, xMax = -1000, yMin = 1000, yMax = -1000;

      for (int n = 0 ; n <= maxBlob ; n++) {
         if (blobs[n] == null)
	    continue;

         blobs[n].update();

         if (blobs[n].getXBounds(t) == 2) {
            xMin = Math.min(xMin, t[0]);
            xMax = Math.max(xMax, t[1]);
         }
            
         if (blobs[n].getYBounds(t) == 2) {
            yMin = Math.min(yMin, t[0]);
            yMax = Math.max(yMax, t[1]);
         }
      }

      xMin -= unit();
      xMax += unit();
      yMin -= unit();
      yMax += unit();

      NI = (int)((xMax - xMin) / unit() + 3);
      NJ = (int)((yMax - yMin) / unit() + 3);
      I0 = (int)((0    - xMin) / unit() + 1);
      J0 = (int)((0    - yMin) / unit() + 1);
      
      if (ff == null || ff[0].length < NJ || ff[0][0].length < NI) {
         ff = new int[2][NJ+1][NI+1];
         vi = new int[2][NJ+1][NI+1][3];
      }

      for (int kv = 0 ; kv < 2 ; kv++)
      for (int jv = 0 ; jv < NJ ; jv++)
      for (int iv = 0 ; iv < NI ; iv++) {
         ff[kv][jv][iv] = 0;
         for (int nn = 0 ; nn < 3 ; nn++)
            vi[kv][jv][iv][nn] = -100;
      }

      nV = nF = 0;

      int kLo = 1000, kHi = -1000;
      for (int n = 0 ; n <= maxBlob ; n++)
         if (blobs[n] != null && blobs[n].getZBounds(t) == 2) {
            kLo = Math.min(kLo, (int)Math.floor(t[0] / unit()));
            kHi = Math.max(kHi, (int)Math.floor(t[1] / unit()));
         }
      kLo--;
      kHi++;

      for (int k = kLo ; k <= kHi ; k++) {
         z = k * unit();
         pk = k & 1;

         int jLo = 1000, jHi = -1000;
         for (int n = 0 ; n <= maxBlob ; n++)
            if (blobs[n] != null && blobs[n].computeYBounds(z, t) == 2) {
               jLo = Math.min(jLo, (int)Math.floor(t[0] / unit()));
               jHi = Math.max(jHi, (int)Math.floor(t[1] / unit()));
            }
         jLo--;
         jHi++;

         for (int j = jLo ; j <= jHi ; j++) {
            y = j * unit();
            nActiveBlobs = 0;

            int iLo = 1000, iHi = -1000;
            for (int n = 0 ; n <= maxBlob ; n++)
               if (blobs[n] != null && blobs[n].computeXBounds(y, t) == 2) {
                  iLo = Math.min(iLo, (int)Math.floor(t[0] / unit()));
                  iHi = Math.max(iHi, (int)Math.floor(t[1] / unit()));
                  activeBlob[nActiveBlobs++] = blobs[n];
               }
            iLo--;
            iHi++;

            int ffjk[] = ff[pk][J0 + j];

            for (int i = iLo ; i <= iHi ; i += 2)
               ffjk[I0 + i] = evalVoxel(i);

            for (int i = iLo + 1 ; i <= iHi ; i += 2) {
               int a = ffjk[I0 + i - 1];
               int b = ffjk[I0 + i + 1];
               if ((a > 0) == (b > 0))
                  ffjk[I0 + i] = a + b >> 1;
               else
                  ffjk[I0 + i] = evalVoxel(i);
            }
         }

         for (int j = jLo ; j <= jHi ; j++) {
            y = j * unit();

            int iLo = 1000, iHi = -1000;
            for (int n = 0 ; n <= maxBlob ; n++)
               if (blobs[n] != null && blobs[n].computeXBounds(y, t) == 2) {
                  iLo = Math.min(iLo, (int)Math.floor(t[0] / unit()));
                  iHi = Math.max(iHi, (int)Math.floor(t[1] / unit()));
               }
            iLo--;
            iHi++;

            int ffjk[] = ff[pk][J0 + j];
            ff00 = ff[1-pk][J0 + j    ];
            ff01 = ff[1-pk][J0 + j + 1];
            ff10 = ff[  pk][J0 + j    ];
            ff11 = ff[  pk][J0 + j + 1];
            for (int i = iLo ; i <= iHi ; i++)
               voxelToTriangles(i, j, k);
         }
      }

      createSurface();
   }

   int evalVoxel(int i) {
      double x = i * unit(), sum = -1.0;
      for (int n = 0 ; n < nActiveBlobs ; n++)
         sum += activeBlob[n].f(x) * activeBlob[n].getValue(Blob.SHAPE_WEIGHT);
      return (int)((1<<16) * sum);
   }

   public void evalMaterialWeights(double x, double y, double z, double f[]) {
      double sum = 0;
      for (int n = 0 ; n <= maxBlob ; n++)
         if (blobs[n] != null) {
            double t = 0.5 + (blobs[n].f(x, y, z) - 0.5) / blobs[n].getValue(Blob.MATERIAL_BLENDING);
            sum += f[n] = Math.max(0, Math.min(1, t)) * blobs[n].getValue(Blob.MATERIAL_WEIGHT);
         }
      for (int n = 0 ; n <= maxBlob ; n++)
         f[n] /= sum;
   }

   static double epsilon = 0.0001;

   public double computeGradient(double x, double y, double z, double gradient[]) {
      double f = eval(x, y, z);
      gradient[0] = (eval(x + epsilon, y, z) - f) / epsilon;
      gradient[1] = (eval(x, y + epsilon, z) - f) / epsilon;
      gradient[2] = (eval(x, y, z + epsilon) - f) / epsilon;
      return f;
   }

   public double eval(double x, double y, double z) {
      double sum = 0;
      for (int n = 0 ; n <= maxBlob ; n++)
         if (blobs[n] != null)
	    sum += blobs[n].f(x, y, z) * blobs[n].getValue(Blob.SHAPE_WEIGHT);
      return sum;
   }

   public double evalBlob(int n, double x, double y, double z) {
      return blobs[n] == null ? 0 : blobs[n].f(x, y, z);
   }

   void voxelToTriangles(int i, int j, int k) {
      int ii = I0 + i;
      int jj = J0 + j;

      voxel[0] = ff00[ii]; voxel[1] = ff00[ii + 1];
      voxel[2] = ff01[ii]; voxel[3] = ff01[ii + 1];
      voxel[4] = ff10[ii]; voxel[5] = ff10[ii + 1];
      voxel[6] = ff11[ii]; voxel[7] = ff11[ii + 1];

      int state = 0;
      for (int b = 0 ; b < 8 ; b++)
         if (voxel[b] < 1)
            state += 1 << b;

      int data[] = MarchingCubes.data[state];

      for (int d = 0 ; d < data.length ; nF++) {
         boolean fail = false;
         for (int m = 0 ; m < 3 ; m++) {
            int off = data[d++];
            int dir = data[d++];

            int i0 = off    & 1;
            int j0 = off>>1 & 1;
            int k0 = off>>2    ;

            int kv = pk ^ k0;
            int jv = jj + j0;
            int iv = ii + i0;

            boolean isNewVertex = (off | (1 << dir)) == 7;
            if (isNewVertex)
               vi[kv][jv][iv][dir] = nV++;

            int nv = vi[kv][jv][iv][dir];
            if (nv < 0 || nv >= nV) {
               fail = true;
	       //System.err.println("oopsie");
            }

            if (! fail) {
               if (faceArray.length <= nF) {
                  int newSize = nF + 1000;

		  if (faceArray.length < newSize) {
		     int[][] newFaceArray = new int[newSize][3];
		     for (int _i = 0 ; _i < faceArray.length ; _i++)
		        for (int _j = 0 ; _j < 3 ; _j++)
		           newFaceArray[_i][_j] = faceArray[_i][_j];
	             faceArray = newFaceArray;
                  }

                  if (vertexArray.length < 3 * newSize) {
		     double[][] newVertexArray = new double[3 * newSize][3];
		     for (int _i = 0 ; _i < vertexArray.length ; _i++)
		        for (int _j = 0 ; _j < 3 ; _j++)
		           newVertexArray[_i][_j] = vertexArray[_i][_j];
		     vertexArray = newVertexArray;
		  }
               }
               
               faceArray[nF][m] = nv;
               
               if (isNewVertex) {
                  double v[] = vertexArray[nv];
                  v[0] = unit() * (i + i0);
                  v[1] = unit() * (j + j0);
                  v[2] = unit() * (k + k0);
                  v[dir] -= unit() * voxel[off] / (voxel[7] - voxel[off]);
               }
            }
         }

         if (fail)
            --nF;
      }
   }

   synchronized void createSurface() {

      if (faces == null || faces.length < nF)
         faces = new int[nF][3];

      if (vertices == null || vertices.length < nV)
         vertices = new double[nV][6];
      
      for (int i = 0 ; i < nF ; i++)
         for (int j = 0 ; j < 3 ; j++)
            faces[i][j] = faceArray[i][j];
      
      for (int i = 0 ; i < nV ; i++)
         for (int j = 0 ; j < 3 ; j++)
            vertices[i][j] = vertexArray[i][j];

      nFaces = nF;
      nVertices = nV;
      computeSurfaceNormals();
   }
   
   public int getMaxBlob() { return maxBlob; }
   
   public Blob getBlobAtIntersect(double cameraPoint[], double aimPoint[], double point[]) {
      int hit = -1;
      Blob hitBlob = null;

      double tMin = 10000;
      for (int i=0; i <= getMaxBlob(); i++) {
         Blob blob = getBlob(i);
         
         if (blob != null) {
            if (blob.rayIntersect(cameraPoint, aimPoint, roots))
             if (roots[0] < tMin) {
                hit = i;
                hitBlob = blob;
                tMin = roots[0];
             }
         }
      }
      
      if (hit >= 0) {
         for (int k = 0; k < 3; k++)
            point[k] = cameraPoint[k] + tMin * aimPoint[k];
      }
      
      return hit < 0 ? null : hitBlob;
   }

   protected double unit = 0.05;
   protected Blob blobs[] = new Blob[1000];

   private int NI, NJ, I0, J0;
   private double blobData[][];
   private double fuzz = 1.00;
   private double fatten = 0.0;
   private double t[] = new double[2];
   private Blob activeBlob[] = new Blob[1000];
   private int ff[][][], vi[][][][];
   private int pk = 0, maxBlob = 0, nActiveBlobs = 0, nV, nF;
   private int faceArray[][] = new int[0][3];
   private double vertexArray[][] = new double[3 * faceArray.length][3];
   private int ff00[], ff01[], ff10[], ff11[], voxel[] = new int[8];
   private double x, y, z;
   private double roots[] = new double[2];
   
}


