
package test;

public class Evals
{
   int size;
   int[] lo = new int[100];
   int[] hi = new int[100];
   String text;
   String[] evals = new String[100];
   String[] targetEvals = new String[100];
   boolean isDamage;

   public int size() {
      return size;
   }

   public String get(int i) {
      return evals[i];
   }

   public void set(int i, String value) {
      targetEvals[i] = value;
   }

   void update(int lo[], int hi[], int n) {
      if (n == size) {
         boolean isDamage = false;
         for (int i = 0 ; i < size ; i++)
            if ( lo[i] != this.lo[i] ||
	         hi[i] != this.hi[i] ||
	         ! text.substring(lo[i], hi[i]).equals(evals[i]) ) {
	       isDamage = true;
	       break;
            }
	 if (! isDamage)
	    return;
      }

      size = n;
      for (n = 0 ; n < size ; n++) {
         this.lo[n] = lo[n];
         this.hi[n] = hi[n];
	 evals[n] = text.substring(lo[n], hi[n]);
      }
      isDamage = true;
   }
}

