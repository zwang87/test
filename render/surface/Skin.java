
package render.surface;

//import jmm.lang.SysOut;
//import perlin.math.Matrix;
//import perlin.model.Geometry;
import render.*;

public class Skin extends Surface
{
   public Skin() {
      nGeometries = 0;
      geometries = new Geometry[100];
      f = new double[100];
      materialsMap = new int[100];
      materialWeights = new double[100];
   }

   public Blob addBlob(Geometry g) {
      geometries[nGeometries] = g;
      return newBlob(nGeometries++, g);
   }

   public void removeBlob(Geometry g) {
      for (int n = 0 ; n < nGeometries ; n++)
         if (geometries[n] == g) {
	    Blob blob = (Blob)g.clientObject;
	    g.clientObject = null;
	    --nGeometries;
	    geometries[n] = geometries[nGeometries];
	    blobs[n] = blobs[nGeometries];

	    geometries[nGeometries] = null;
	    blobs[nGeometries] = null;

	    break;
	 }
   }

   public double unit() {
      return isBaked ? unit : 1.5 * unit;
   }

   public int nGeometries() { return nGeometries; }

   public Geometry getGeometry(int n) { return geometries[n]; }
   
   public Geometry getGeometry(Blob blob) {
      for(int i=0; i <= nGeometries; i++) {
         if (blob == blobs[i])
            return geometries[i];
      }
      
      return null;
   }

   synchronized public void setRubber(boolean tf) { isBaked = tf; }

   public boolean isRubber() { return isBaked; }

   synchronized public void update() {
      if (isBaked) {
         if (! wasBaked)
            bake();
         for (int n = 0 ; n < nGeometries ; n++) {
            bones[n].getMatrix().copy(references[n]);
            for (Geometry g = getGeometry(n) ; g != null ; g = g.getParent())
               bones[n].getMatrix().postMultiply(g.getMatrix());
            
            bones[n].globalMatrix.copy(bones[n].getMatrix());
         }
      }
      else {
         unsetBones();
         updateSurface();
      }
      wasBaked = isBaked;
   }

   public static final void setValue(Geometry g, int index, double value) {
      getBlob(g).setValue(index, value);
   }
   
   public static final double getValue(Geometry g, int index, double value) {
      return getBlob(g).getValue(index);
   }
   
   public static final Blob getBlob(Geometry g) {
      return (Blob) g.clientObject;
   }

   synchronized private void updateSurface() {

      for (int n = 0 ; n < nGeometries ; n++) {
         m.identity();
         for (Geometry g = geometries[n] ; g != null ; g = g.getParent())
            m.postMultiply(g.getMatrix());
         getBlob(n).setMatrix(m);
      }

      super.update();

      for (int n = 0 ; n < nGeometries ; n++)
         setMaterial(n, geometries[n].material);

      for (int i = 0 ; i < nVertices() ; i++) {
         double v[] = vertices[i];
         evalMaterialWeights(v[0], v[1], v[2], f);
         for (int n = 0 ; n < nGeometries ; n++)
            setMaterialWeight(n, i, f[n]);
      }
   }

   public void forceBake() {
      bake();
      wasBaked = true;
   }

   void bake() {
      updateSurface();

      if (bones == null) {
         bones = new Geometry[nGeometries];
         references = new Matrix[nGeometries];
         for (int n = 0 ; n < nGeometries ; n++) {
            bones[n] = new Geometry();
            references[n] = new Matrix();
         }
      }
      for (int n = 0 ; n < nGeometries ; n++) {
         references[n].identity();
         for (Geometry g = getGeometry(n) ; g != null ; g = g.getParent())
            references[n].postMultiply(g.getMatrix());
         references[n].invert(references[n]);
      }

      for (int i = 0 ; i < nVertices() ; i++) {
         double v[] = vertices[i];

         double totalWeight = 0;
         int k = 0;
         for (int n = 0 ; n < nGeometries ; n++) {
            double f = evalBlob(n, v[0], v[1], v[2]);
            if (f > 0) {
               boneG[k] = bones[n];
               boneW[k] = f;
               totalWeight += f;
               k++;
            }
         }

         for (int _k = 0 ; _k < k ; _k++)
            boneW[_k] /= totalWeight;

         setBones(i, k, boneG, boneW);
      }
   }

   Matrix m = new Matrix();
   Matrix references[];
   boolean isBaked = false, wasBaked = false;
   Geometry geometries[], bones[], boneG[] = new Geometry[100];
   double f[], boneW[] = new double[100];
   int nGeometries = 0;
   int materialsMap[];
   double materialWeights[];
}

