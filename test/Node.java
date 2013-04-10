
package test;

import java.awt.*;

class Node extends Rect implements Types
{
   public Node(int i0, String src, Object data) {
      height = lineHeight;
      this.i0 = i0;

      if (src == null) {
         type = TEXT;
         this.data = data;
      }
      else if (src.length() == 0) {
         type = UNKNOWN;
      }
      else {
         if (isEnd = src.charAt(0) == '/')
            src = src.substring(1, src.length());

         String words[] = src.split("[ \t]");
         String nodeName = words[0];

              if (nodeName.equals("a"        )) type = ANCHOR;
         else if (nodeName.equals("b"        )) type = BOLD;
         else if (nodeName.equals("button"   )) type = BUTTON;
         else if (nodeName.equals("eval"     )) type = EVAL;
         else if (nodeName.equals("highlight")) type = HIGHLIGHT;
         else if (nodeName.equals("i"        )) type = ITALIC;
         else if (nodeName.equals("br"       )) type = BR;
         else if (nodeName.equals("slider"   )) type = SLIDER;
         else                                   type = UNKNOWN;

         if (words.length > 1) {
            String nodeData = src.substring(words[0].length() + 1, src.length());
            switch (type) {
            case ANCHOR:
               this.data = nodeData;
               break;
            case EVAL:
               this.data = nodeData;
               break;
            default:
               this.data = Parse.parseTags(nodeData);
               break;
            }
         }
      }
   }

   public String toString() {
      String str = x + "," + y + "\t" + types[type] + "\t";
      if (type == TEXT)
         str += (String)data;
      else {
         if (isEnd)
            str += "[END] ";
         if (data != null) {
            String[][] tags = (String[][]) data;
            for (int i = 0 ; i < tags.length ; i++)
               str += "[" + tags[i][0] + " = " + tags[i][1] + "] ";
         }
      }
      return str;
   }

   public String getText() {
      return (String)data;
   }

   int type, modifiers, i0;
   boolean isEnd, isVisible;
   Object data;
   Font font;

   static int lineHeight;
}

