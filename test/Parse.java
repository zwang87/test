
package test;

import java.util.*;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;

public class Parse
{
   static String[][] parseTags(String src) {
      String[] words = src.split("[ \t\n]");
      String[][] tags = new String[words.length][2];
      for (int n = 0 ; n < words.length ; n++) {
         int iEq = words[n].indexOf('=');
         if (iEq < 0) {
            tags[n][0] = words[n];
            tags[n][1] = "";
         }
         else {
            tags[n][0] = words[n].substring(0, iEq);
            tags[n][1] = words[n].substring(iEq + 1, words[n].length());
         }
      }
      return tags;
   }

   static String colorNames[] = (
      "pink red orange yellow green cyan blue " +
      "magenta black gray white"
   ).split(" ");

   static Color colors[] = {
      Color.pink, Color.red, Color.orange, Color.yellow, Color.green, Color.cyan, Color.blue,
      Color.magenta, Color.black, Color.gray, Color.white,
   };

   static Color parseColor(String src) {
      if (src.indexOf('#') == 0 && src.length() == 7)
         return new Color(hex(src.substring(1,3)), 
                          hex(src.substring(3,5)), 
                          hex(src.substring(5,7)));
      else
         for (int n = 0 ; n < colorNames.length ; n++)
            if (src.equals(colorNames[n]))
               return colors[n];
      return Color.black;
   }
   
   public static String setTagValue(String text, String type, String name, String value) {
      for (int i = 0 ; (i = text.indexOf('<', i)) >= 0 ; i++) {
         int i1 = ++i + type.length();
         i1 = Math.min(i1, text.length());
         if (text.substring(i, i1).equals(type)) {
            int j = text.indexOf('>', i);
            String src = text.substring(i, j);
            int a = src.indexOf(name);
            if (a >= 0) {
               int b = a + name.length();
               if (b == src.length() || src.charAt(b) == ' ')
                  src = src.substring(0, b) + "=" + value + src.substring(b, src.length());
               else if (src.charAt(b) == '=') {
                  int c = src.indexOf(' ', b);
                  if (c == -1)
                     c = src.length();
                  src = src.substring(0, b) + "=" + value + src.substring(c, src.length());
               }
               else
                  continue;
               text = text.substring(0, i) + src + text.substring(j, text.length());
            }
         }
      }
      return text;
   }

   public static int nextWord(String s, int i) {
      return nextNonspace(s, nextSpace(s, i));
   }

   public static int nextSpace(String s, int i) {
      for ( ; i < s.length() && ! isSpace(s.charAt(i)) ; i++) ;
      return i;
   }

   public static int nextNonspace(String s, int i) {
      for ( ; i < s.length() && isSpace(s.charAt(i)) ; i++) ;
      return i;
   }

   public static boolean isSpace(int c) {
      return c == ' ' || c == '\t' || c == '\n';
   }

   public static boolean isAlphanumeric(int c) {
      return c >= 'a' && c <= 'z' ||
             c >= 'A' && c <= 'Z' ||
             c >= '0' && c <= '9' || c == '_';
   }

   public static boolean isNumeric(int c) {
      return c >= '0' && c <= '9' || c == '.';
   }

   public static boolean isDigit(int c) {
      return c >= '0' && c <= '9';
   }

   public static boolean isIntValue(String s) {
      try {
         Integer.parseInt(s.trim());
         return true;
      } catch (Exception e) { return false; }
   }

   public static boolean booleanValue(String s) {
      return s.equals("true");
   }

   public static int intValue(String s) {
      try {
         return Integer.parseInt(s);
      } catch (Exception e) { return 0; }
   }

   public static boolean isDoubleValue(String s) {
      try {
         Double.parseDouble(s);
         return true;
      } catch (Exception e) { return false; }
   }

   public static double doubleValue(String s) {
      try {
         return Double.parseDouble(s);
      } catch (Exception e) { return 0.0; }
   }

   public static int stringWidth(Graphics g, String s) {
      return g == null ? 0 : g.getFontMetrics().stringWidth(s);
   }

   public static int fontHeight(Graphics g) {
      return g == null ? 0 : g.getFontMetrics().getHeight();
   }

   public static int findEndOfInt(String text, int i) {
      int j = i;
      while (j < text.length() && Parse.isDigit(text.charAt(j)))
         j++;
      return j;
   }

   public static int intValue(String text, int i, int j) {
      int value = 0;
      for (int k = i ; k < j ; k++)
         value = 10 * value + (text.charAt(k) - '0');
      return value;
   }

   public static int surroundingTagLength(String text, int lo, int hi) {
      if ( lo > 0 && text.charAt(lo - 1) == '>' &&
           hi < text.length() - 2 && text.charAt(hi) == '<' &&
                                     text.charAt(hi + 1) == '/' ) {
         String startTag = "";
         for (int i = lo - 2 ; i >= 0 && text.charAt(i) != '<' ; i--)
            startTag = text.charAt(i) + startTag;
         if (startTag.length() > 0) {
            String endTag = "";
            for (int i = hi + 2 ; i < text.length() && text.charAt(i) != '>' ; i++)
               endTag += text.charAt(i);
            if (startTag.equals(endTag))
               return startTag.length() + 2;
         }
      }
      return 0;
   }

   public static String escape(String src) {
      String dst = "";
      for (int i = 0 ; i < src.length() ; i++) {
         int c = src.charAt(i);
         switch (c) {
         case '\t': dst += "\\t"; break;
         case '\n': dst += "\\n"; break;
         default  : dst += (char)c;
         }
      }
      return dst;
   }

   public static void copyToClipboard(String str) {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Clipboard clipboard = toolkit.getSystemClipboard();
      StringSelection strSel = new StringSelection(str);
      clipboard.setContents(strSel, null);
   }

   public static String getFromClipboard() {
      Toolkit toolkit = Toolkit.getDefaultToolkit();
      Clipboard clipboard = toolkit.getSystemClipboard();
      try {
         return (String) clipboard.getData(DataFlavor.stringFlavor);
      } catch (Exception e) { }
      return null;
   }

   static String round(double value) {
      String s = "" + ((int)(100 * value) / 100.);
      int i = s.indexOf('.');
      if (i < 0)
         return s + ".00";
      int n = s.length() - i;
      switch (n) {
      case  1: return s + "00";
      case  2: return s + "0";
      }
      return s;
   }

   static int hex(String src) {
      return hexChar(src.charAt(0)) << 4 | hexChar(src.charAt(1));
   }

   static int hexChar(int ch) {
      return ch >= '0' && ch <= '9' ? ch - '0' :
             ch >= 'a' && ch <= 'f' ? ch - 'a' + 10 :
             ch >= 'A' && ch <= 'F' ? ch - 'A' + 10 : 0;
   }

   static int findStartOfWord(String src, int i) {
      if (isNumeric(src.charAt(i))) {
         while (i > 0 && isNumeric(src.charAt(i-1)))
            i--;
         if (i > 0 && src.charAt(i-1) == '-')
	    i--;
         return i;
      }
      while (i > 0 && Parse.isAlphanumeric(src.charAt(i - 1)))
         i--;
      return moveToStartOfStartTag(src, i);
   }

   static int findEndOfWord(String src, int i) {
      if (isNumeric(src.charAt(i))) {
         if (i < src.length() && src.charAt(i) == '-')
	    i++;
         while (i < src.length() && isNumeric(src.charAt(i)))
	    i++;
         return i;
      }
      while (i < src.length() && Parse.isAlphanumeric(src.charAt(i)))
         i++;
      return moveToEndOfEndTag(src, i);
   }

   static int moveToStartOfStartTag(String src, int i) {
      if (i - 1 >= 0 && src.charAt(i - 1) == '>') {
         int j = findChar(src, '<', i, -1);
         if (src.charAt(j + 1) != '/')
            i = j;
      }
      return i;
   }

   static int moveToEndOfEndTag(String src, int i) {
      if (i + 1 < src.length() && src.charAt(i) == '<' && src.charAt(i + 1) == '/')
         i = Math.min(src.length(), findChar(src, '>', i, 1) + 1);
      return i;
   }

   static int findTag(String src, int i) {
      while (i < src.length()) {
         int a = src.indexOf('<', i);
         if (a < 0)
            break;
         int b = src.indexOf('>', a + 1);
         if (b < 0)
            break;
         int c = src.indexOf('<', a + 1);
         if (c < 0 || b < c)
            return a;
         i = c;
      }
      return src.length();
   }

   static int findChar(String src, int ch, int i, int di) {
      while (i > 0 && i < src.length() && src.charAt(i) != ch)
         i += di;
      return i;
   }

   static boolean isInTag(String src, int i) {
       int loL = findChar(src, '<', i, -1);
       int loR = findChar(src, '>', i, -1);
       int hiL = findChar(src, '<', i, 1);
       int hiR = findChar(src, '>', i, 1);
       return loL >= loL && loR <= loL;
   }
   
   static String increment(String src) {
      if (src.indexOf('.') < 0)
         return "" + (intValue(src) + 1);

      return varyDouble(src, 1);
   }

   static String decrement(String src) {
      if (src.indexOf('.') < 0)
         return "" + (intValue(src) - 1);

      return varyDouble(src, -1);
   }

   static String varyDouble(String src, int incr) {
      String s = src;

      boolean isNegative = s.charAt(0) == '-';
      if (isNegative)
         s = s.substring(1, s.length());

      int d = s.indexOf('.');
      String sInt = s.substring(0, d);
      String sDec = s.substring(d+1, s.length());
      int digits = sDec.length();

      s = sInt + sDec;

      int len = s.length();
      s = "" + (intValue(s) + (isNegative ? -incr : incr));

      if (s.charAt(0) == '-') {
         isNegative = ! isNegative;
         s = s.substring(1, s.length());
      }

      while (s.length() < len)
         s = "0" + s;

      if (s.length() == digits)
         s = "0" + s;

      if (s.charAt(0) == '0' && s.charAt(1) != '.')
         s = s.substring(1, s.length());

      d = s.length() - digits;
      return (isNegative ? "-" : "") +
             s.substring(0, d) + "." + s.substring(d, s.length());
   }
}

