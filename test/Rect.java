
package test;

public class Rect
{
   public void setX(int x) { this.x = x; }
   public void setY(int y) { this.y = y; }
   public void setWidth(int width) { this.width = width; }
   public void setHeight(int height) { this.height = height; }

   public int getX() { return x; }
   public int getY() { return y; }
   public int getWidth() { return width; }
   public int getHeight() { return height; }

   public boolean contains(int x, int y) {
      return x >= this.x && x < this.x + width &&
             y >= this.y && y < this.y + height ;
   }

   public String toString() {
      return "{" + x + "," + y + "," + width + "," + height + "}";
   }

   public boolean isScrollable() {
      return isScrollable;
   }

   public void setScrollable(boolean isScrollable) {
      this.isScrollable = isScrollable;
   }

   public int x, y, width, height;
   public boolean isDamage, isScrollable = true;
}

