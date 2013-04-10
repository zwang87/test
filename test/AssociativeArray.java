
package test;

import java.util.*;

public class AssociativeArray
{
   public void set(String name, double value) { set(name, "" + value); }

   public void set(String name, String value) {
      for (int i = 0 ; i < domain.size() ; i++)
         if (((String)domain.get(i)).equals(name)) {
	    range.set(i, value);
	    return;
         }
      domain.add(name);
      range.add(value);
   }

   public boolean is(String name) {
      try {
         return Double.parseDouble(get(name)) != 0.0;
      } catch (Exception e) { return false; }
   }

   public int getI(String name) {
      try {
         return Integer.parseInt(get(name));
      } catch (Exception e) { return 0; }
   }

   public double getD(String name) {
      try {
         return Double.parseDouble(get(name));
      } catch (Exception e) { return 0.0; }
   }

   public String get(String name) {
      for (int i = 0 ; i < domain.size() ; i++)
         if (((String)domain.get(i)).equals(name))
	    return (String)range.get(i);
      return "";
   }

   ArrayList domain = new ArrayList();
   ArrayList range  = new ArrayList();
}

