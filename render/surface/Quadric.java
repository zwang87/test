
/*
   This class scan-converts an ellipsoid, defined by a matrix transformation of the unit sphere xx+yy+zz=1.

   First getZbounds to find the back and front of the ellipsoid.
      Then setZ, to fix an ellipse in a particular Z plane.
         Then getYbounds to find the bottom and top of this ellipse.
            Then setY to fix a line segment in a particular Y line.
               Then getXbounds to find the left and right of this line segment.

   The getbounds methods return 2 if you are looking within the ellipsoid, otherwise the return 0.
*/

package render.surface;

//import jmm.lang.Math;
//import perlin.math.Matrix;
import render.*;

public class Quadric
{
   Matrix inv = new Matrix();
   Matrix tmp = new Matrix();
   Matrix eqn = new Matrix();
   double eps = 0.01;
   
   Matrix originalEqn;
   
   public void setCoefficients(double xx, double yy, double zz) {
      xx = Math.max(eps, xx);
      yy = Math.max(eps, yy);
      zz = Math.max(eps, zz);
      double rr = Math.max(xx, Math.max(yy, zz));
      setCoefficients(xx, 0, yy, 0, 0, zz, 0, 0, 0, -rr);
   }

   protected void setCoefficients(double a,double b,double c,double d,double e,
                                  double f,double g,double h,double i,double j) {
      this.a = a;
      this.b = b;
      this.c = c;
      this.d = d;
      this.e = e;
      this.f = f;
      this.g = g;
      this.h = h;
      this.i = i;
      this.j = j;
      
      for (int _i = 0 ; _i < 4 ; _i++)
      for (int _j = 0 ; _j < 4 ; _j++)
         eqn.set(_i, _j, 0);

      eqn.set(0, 0, a);
      eqn.set(0, 1, b);
      eqn.set(1, 1, c);
      eqn.set(0, 2, d);
      eqn.set(1, 2, e);
      eqn.set(2, 2, f);
      eqn.set(0, 3, g);
      eqn.set(1, 3, h);
      eqn.set(2, 3, i);
      eqn.set(3, 3, j);
      
      originalEqn = new Matrix(eqn.getData());
   }
   
   public void setRounded(double rounded) {
      rounded = eps + rounded * (1.0 - eps);
      
      for (int i=0; i < 2; i++) {
         if (originalEqn.get(i, i) == eps)
            eqn.set(i, i, rounded);
      }
   }

   public void update(Matrix m, double roundedness) {
      tmp.copy(eqn);

      double data[] = tmp.getData();
      for (int i = 0 ; i < data.length ; i++)
         if (data[i] == eps)
            data[i] = Math.max(eps, roundedness);

      inv.invert(m);
      tmp.preMultiply(inv);
      inv.transpose();
      tmp.postMultiply(inv);

      a = tmp.get(0, 0);
      b = tmp.get(0, 1) + tmp.get(1, 0);
      c = tmp.get(1, 1);
      d = tmp.get(0, 2) + tmp.get(2, 0);
      e = tmp.get(1, 2) + tmp.get(2, 1);
      f = tmp.get(2, 2);
      g = tmp.get(0, 3) + tmp.get(3, 0);
      h = tmp.get(1, 3) + tmp.get(3, 1);
      i = tmp.get(2, 3) + tmp.get(3, 2);
      j = tmp.get(3, 3);
   }

   public int get3DXBounds(double xBounds[]) {
      double discr = e * e - 4 * c * f;
      double A = (2 * b * f - d * e) / discr;
      double B = (2 * f * h - e * i) / discr;
      double C = (2 * c * d - b * e) / discr;
      double D = (2 * c * i - e * h) / discr;

      double P = a + b * A +     c * A * A + d * C + e *  A * C          +     f * C * C;
      double Q =     b * B + 2 * c * A * B + d * D + e * (A * D + B * C) + 2 * f * C * D + g + h * A + i * C;
      double R =                 c * B * B +         e *  B * D          +     f * D * D +     h * B + i * D + j;

      return findQuadraticRoots(P, Q, R, xBounds);
   }

   public int get3DYBounds(double yBounds[]) {
      double discr = d * d - 4 * a * f;
      double A = (2 * b * f - d * e) / discr;
      double B = (2 * f * g - d * i) / discr;
      double C = (2 * a * e - b * d) / discr;
      double D = (2 * a * i - d * g) / discr;

      double P =     a * A * A + b * A + c + d * A * C           + e * C +     f * C * C;
      double Q = 2 * a * A * B + b * B     + d * (A * D + B * C) + e * D + 2 * f * C * D + g * A + h + i * C;
      double R =     a * B * B             + d * B * D                   +     f * D * D + g * B     + i * D + j;

      return findQuadraticRoots(P, Q, R, yBounds);
   }

   public int get3DZBounds(double zBounds[]) {
      double discr = b * b - 4 * a * c;
      double A = (2*a*e - b*d) / discr;
      double B = (2*a*h - b*g) / discr;
      double C = (2*c*d - b*e) / discr;
      double D = (2*c*g - b*h) / discr;

      double P =     a * C * C + b *  C * A +              c * A * A + d * C + e * A + f;
      double Q = 2 * a * C * D + b * (C * B + D * A) + 2 * c * A * B + d * D + e * B +     g * C + h * A + i;
      double R =     a * D * D + b *  D * B +              c * B * B +                     g * D + h * B +     j;

      return findQuadraticRoots(P, Q, R, zBounds);
   }

   public void setZ(double z) {
      A = a;
      B = b;
      C = c;
      D = d * z + g;
      E = e * z + h;
      F = f * z * z + i * z + j;
   }

   public int get2DYBounds(double yBounds[]) {
      double I = -B / (2 * A);
      double J = -D / (2 * A);

      double P =     A * I * I + B * I + C;
      double Q = 2 * A * I * J + B * J +    D * I + E;
      double R =     A * J * J +            D * J +     F;

      return findQuadraticRoots(P, Q, R, yBounds);
   }

   public void setY(double y) {
      P = A;
      Q = B * y + D;
      R = C * y * y + E * y + F;
   }

   public int get1DXBounds(double xBounds[]) {
      return findQuadraticRoots(P, Q, R, xBounds);
   }

   public double eval(double x, double y, double z) {
      return x * (a * x + b * y + d * z + g) + y * (c * y + e * z + h) + z * (f * z + i) + j;
   }

   public double eval(double x, double y) {
      return x * (A * x + B * y + D) + y * (C * y + E) + F;
   }

   public double eval(double x) {
      return x * (P * x + Q) + R;
   }

   public void scaleCoefficients(double s) {
      a *= s;
      b *= s;
      c *= s;
      d *= s;
      e *= s;
      f *= s;
      g *= s;
      h *= s;
      i *= s;
      j *= s;
   }

   int findQuadraticRoots(double A, double B, double C, double z[]) {
      double discr = B * B - 4 * A * C;
      if (discr < 0)
         return 0;

      discr = Math.sqrt(discr);
      z[0] = (-B - discr) / (2 * A);
      z[1] = (-B + discr) / (2 * A);
      return 2;
   }
   
   Quadric copy() {
      Quadric other = new Quadric();
      other.setCoefficients(a, b, c, d, e, f, g, h, i, j);
      other.A = A;
      other.B = B;
      other.C = C;
      other.D = D;
      other.E = E;
      other.F = F;
      other.P = P;
      other.Q = Q;
      other.R = R;
      return other;
   }

   double a, b, c, d, e, f, g, h, i, j;
   double A, B, C, D, E, F;
   double P, Q, R;
}

