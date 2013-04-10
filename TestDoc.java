
import java.awt.*;
import java.awt.event.*;
import storage.*;
import util.*;
import test.*;
import interpreter.*;
import java.io.*;

public class TestDoc extends BufferedApplet implements KeyListener,
                                                       MouseListener,
                                                       MouseMotionListener,
                                                       MouseWheelListener
{
   Doc doc = new Doc(this);
   Graphics g;
   boolean isShift, isAlt, isControl, isCommand;
   int w, h, codeFontHeight = 14;
   Font codeFont = new Font("Courier", Font.PLAIN, codeFontHeight);
   Color codeBgColor = new Color(245, 250, 255);
   Color selectionColor = new Color(255, 0, 0, 32);
   FileIsModified fileIsModified = new FileIsModified();
   boolean isReadingFromFile;
   private boolean isRealMouseWheel;
   String inputFileName;
   java.net.URL inputURL;

   public void render(Graphics g) {
      this.g = g;
      if (w == 0) {

         String inputFile = getParameter("file");
	 if (inputFile != null && inputFile.length() == 0)
	    inputFile = null;
         if (inputFile != null) {
            inputFileName = inputFile + ".txt";
            try {
               inputURL = new java.net.URI(getDocumentBase().toExternalForm()).resolve("./"+inputFileName).toURL();
            }
            catch (java.net.MalformedURLException e) { }
            catch (java.net.URISyntaxException e) { }
         }

         addKeyListener(this);
         addMouseListener(this);
         addMouseMotionListener(this);
         addMouseWheelListener(this);
         doc.setSize(getWidth(), getHeight());
	 parse();
      }

      if (w != getWidth() || h != getHeight()) {
         doc.setSize(getWidth(), getHeight());
         doc.layout(g);
      }

      w = getWidth();
      h = getHeight();

      if (inputFileName != null && fileIsModified.isModified(new File(inputFileName)))
         parse();
      doc.redraw(g);
   }

   void parse() {
      String code = null;
      if (inputURL != null) {
         code = TextIO.load(inputURL);
      }
      if (code == null && inputFileName != null) {
         code = TextIO.load(inputFileName);
      }
      if (code == null) {
         code = Code.text;
      }

      doc.parse(code);
      doc.layout(g);
   }

   int index = 0;

   public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int keyChar = e.getKeyChar();

      if (keyChar == 65535) {
         switch (keyCode) {
         case  16: isShift   = true; return;
         case  17: isControl = true; return;
         case  18: isAlt     = true; return;
         case 524: // windows key
         case 157: isCommand = true; return;

         case 37: doc.specialKeyPress(Doc.LEFT ); return;
         case 38: doc.specialKeyPress(Doc.UP   ); return;
         case 39: doc.specialKeyPress(Doc.RIGHT); return;
         case 40: doc.specialKeyPress(Doc.DOWN ); return;
         }
      }

      boolean isSpecialKey = isControl || isCommand;

      System.err.println(isSpecialKey + " " + keyChar);
      if (isSpecialKey && keyChar == 's') {
         System.err.println("SAVE COMMAND");
	 return;
      }

      doc.keyEvent(Region.PRESS, keyChar | (isAlt ? 128 : 0) | (isSpecialKey ? 256 : 0));
      doc.keyEvent(Region.RELEASE, keyChar | (isAlt ? 128 : 0) | (isSpecialKey ? 256 : 0));
   }

   public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int keyChar = e.getKeyChar();

      // USER CAN HIT ESCAPE KEY TO FORCE A RELOAD FROM FILE.

      if (keyChar == 27) {
         parse();
         return;
      }

      if (keyChar == 65535) {
         switch (keyCode) {
         case  16: isShift   = false; return;
         case  17: isControl = false; return;
         case  18: isAlt     = false; return;
         case 524: // windows key
         case 157: isCommand = false; return;

         case 37: doc.specialKeyRelease(Doc.LEFT ); return;
         case 38: doc.specialKeyRelease(Doc.UP   ); return;
         case 39: doc.specialKeyRelease(Doc.RIGHT); return;
         case 40: doc.specialKeyRelease(Doc.DOWN ); return;
         }
      }

      //doc.keyEvent(Region.RELEASE, keyChar | (isAlt ? 128 : 0) | ((isControl||isCommand) ? 256 : 0));
   }

   public void keyTyped(KeyEvent e) { }

   public void mouseEntered (MouseEvent e) { }
   public void mouseExited  (MouseEvent e) { }
   public void mouseClicked (MouseEvent e) { doc.mouseEvent(Region.CLICK  ,e.getX(),e.getY()); }
   public void mouseMoved   (MouseEvent e) { doc.mouseEvent(Region.MOVE   ,e.getX(),e.getY()); }
   public void mousePressed (MouseEvent e) { doc.mouseEvent(Region.PRESS  ,e.getX(),e.getY()); }
   public void mouseDragged (MouseEvent e) { doc.mouseEvent(Region.DRAG   ,e.getX(),e.getY()); }
   public void mouseReleased(MouseEvent e) { doc.mouseEvent(Region.RELEASE,e.getX(),e.getY()); }

   public void mouseWheelMoved(MouseWheelEvent e) {
      isRealMouseWheel = true;
      doc.mouseWheelDeltaY(e.getUnitsToScroll());
   }

   // an applet may optionally get mousewheel events from JS in the browser
   // (a hack to fix scrolling in Safari)
   public void mouseWheelMovedFromBrowser(int deltaY) {
      if (!isRealMouseWheel) {
         int d = -3;
         if (deltaY < 0) {
            d = 3;
         }
         doc.mouseWheelDeltaY(d);
      }
   }
}

