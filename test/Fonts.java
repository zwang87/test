
package test;

import java.awt.*;

public class Fonts
{
   Font plain, bold, italic, boldItalic;
   Font code, boldCode, italicCode, boldItalicCode;
   int height = 30;
   boolean isDamage = true;
   boolean isTextView = false;
   { update(); }

   public int getHeight() {
      return height;
   }

   public void setHeight(int height) {
      height = Math.max(10, height);
      isDamage = height != this.height;
      this.height = height;
      update();
   }

   public int height() {
      return isTextView ? this.height * 58 / 100
                        : this.height * 80 / 100;
   }

   public void setTextView(boolean state) {
      if (state != isTextView) {
         isTextView = state;
         isDamage = true;
	 update();
      }
   }

   public Font plain() { return plain; }
   public Font bold() { return bold; }
   public Font italic() { return italic; }
   public Font boldItalic() { return boldItalic; }
   public Font code() { return code; }
   public Font boldCode() { return boldCode; }
   public Font italicCode() { return italicCode; }
   public Font boldItalicCode() { return boldItalicCode; }

   public void update() {
      if (isDamage) {
         plain = new Font("Sanserif", Font.PLAIN, height());
         bold = new Font("Sanserif", Font.BOLD, height());
         italic = new Font("Sanserif", Font.ITALIC, height());
         boldItalic = new Font("Sanserif", Font.BOLD | Font.ITALIC, height());

         code = new Font("Monospaced", Font.PLAIN, height() * 3 / 4);
         boldCode = new Font("Monospaced", Font.BOLD, height() * 3 / 4);
         italicCode = new Font("Monospaced", Font.ITALIC, height() * 3 / 4);
         boldItalicCode = new Font("Monospaced", Font.BOLD | Font.ITALIC, height() * 3 / 4);

	 isDamage = false;
      }
   }
}

