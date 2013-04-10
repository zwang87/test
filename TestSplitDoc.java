// TODO: we should be able to use the pixel deltas directly from
// the browser mouse event to do the right flavor of scrolling

import java.awt.*;
import java.awt.event.*;
import storage.*;
import util.*;
import test.*;
import interpreter.*;
import java.io.*;
import java.util.ArrayList;
import java.net.URL;

public class TestSplitDoc extends BufferedApplet implements KeyListener,
                                                       MouseListener,
                                                       MouseMotionListener,
                                                       MouseWheelListener
{
   public TestSplitDoc() {
      super();
      isBrowser = true;
      try {
         System.getSecurityManager().checkPermission(new java.security.AllPermission());
         isBrowser = false;
      } catch (SecurityException e) { }
   }

   class PanelRegion extends Region {
       Doc d;

       public PanelRegion(Doc d) {
           this.d = d;
       }

       @Override
       public boolean mouseEvent(int eventType, int x, int y) {
           if (contains(x,y)) {
               focusDoc = d;

               if (nextFocusDoc != null) {
                  focusDoc = nextFocusDoc;
                  nextFocusDoc = null;
               }

               return d.mouseEvent(eventType, x-this.x, y-this.y);
           } else {
               return false;
           }
       }
   }

   Region rootRegion;
   PanelRegion leftRegion, rightRegion;
   Doc focusDoc, nextFocusDoc;
   ConcurrentDoc leftDoc;
   ConcurrentDoc rightDoc;
   boolean enableRightDoc;

   class LocalWatcher implements DocSync.ChangeWatcherClient {
      ArrayList<DocSync> docs;

      FileIsModified fileIsModified;
      String inputFileName;
      URL inputURL;

      String text;
      int caret, selectionStart, selectionEnd;
      boolean isVaryingNumber;
      boolean updated, loaded;

      private LocalWatcher() {
         text = "";
         updated = true;
         docs = new ArrayList<DocSync>();
         fileIsModified = new FileIsModified();
      }
      
      public LocalWatcher(String inputFileName) {
          this();
          this.inputFileName = inputFileName;
	  if (inputFileName.indexOf(".txt") < 0)
             this.inputFileName += ".txt";
          try {
              inputURL = new java.net.URI(getDocumentBase().toExternalForm()).resolve("./"+this.inputFileName).toURL();
          }
          catch (java.net.MalformedURLException e) { }
          catch (java.net.URISyntaxException e) { }
      }

      public synchronized void docChanged(DocSync d, int lo, int hi, String src, String new_text) {
         text = new_text;

         for (DocSync d_other : docs) {
            if (d_other != d) {
               d_other.replaceState(text, caret,
				          selectionStart,
				          selectionEnd,
				          isVaryingNumber);
            }
         }
      }

      public synchronized void caretMoved(DocSync d, int caret,
                                                     int selectionStart,
						     int selectionEnd,
						     boolean isVaryingNumber) {
         this.caret = caret;
         this.selectionStart = selectionStart;
         this.selectionEnd = selectionEnd;
         this.isVaryingNumber = isVaryingNumber;
         for (DocSync d_other : docs) {
            if (d_other != d) {
               d_other.replaceState(text, caret,
	                                  selectionStart,
					  selectionEnd,
					  isVaryingNumber);
            }
         }
      }

      public synchronized void registerDoc(DocSync d) {
         docs.add(d);
         d.replaceState(text, caret,
	                      selectionStart,
			      selectionEnd,
			      isVaryingNumber);
         d.registerChangeWatcherClient(this);
         updated = true;
      }

      public synchronized void unregisterDoc(DocSync d) {
          docs.remove(d);
          d.unregisterChangeWatcherClient(this);
      }

      private synchronized void checkForFileUpdate() {
         if (!loaded || (inputFileName != null && fileIsModified.isModified(new File(inputFileName)))) {
            loaded = true;
            reloadFile();
         }
      }


      public synchronized void reloadFile() {
            String new_text = null;

            if (inputURL != null) {
                new_text = TextIO.load(inputURL);
            }
            if (new_text == null && inputFileName != null) {
                new_text = TextIO.load(inputFileName);

		// CHECK ALSO IN THE assets/ FOLDER.

		if (new_text == null) {
		   String name = "assets/" + inputFileName;
                   new_text = TextIO.load(name);
		   if (new_text != null)
		      inputFileName = name;
		}
            }
            if (new_text == null) {
                new_text = Code.text;
            }

            if (!text.equals(new_text)) {
               updated = true;
               text = new_text;
               for (DocSync d : docs) {
                  d.replaceState(text, caret,
		                       selectionStart,
				       selectionEnd,
				       isVaryingNumber);
               }
            }
      }

      public synchronized boolean testAndClearUpdated() {
         checkForFileUpdate();

         boolean was_updated = updated;
         updated = false;
         return was_updated;
      }
   };


   String browser = "/Applications/Safari.app";
   LocalWatcher watcher;
   Graphics g;
   boolean isShift, isAlt, isControl, isCommand;
   int w, h, codeFontHeight = 14;
   Color codeBgColor = new Color(245, 250, 255);
   Color selectionColor = new Color(255, 0, 0, 32);
   boolean isRealMouseWheel;
   boolean isBrowser;

   boolean didSetup = false;

   public void render(Graphics g) {
      this.g = g;

      if (!didSetup) {
         // initial setup

         rootRegion = new Region();

         String browser = getParameter("browser");
	 if (browser != null)
	    this.browser = browser;

         watcher = new LocalWatcher(getParameter("file"));
         leftDoc = new ConcurrentDoc(this);
         leftDoc.setBrowser(browser);
         leftDoc.setName("leftDoc");
         leftDoc.setInBrowser(isBrowser);
         focusDoc = leftDoc;
         watcher.registerDoc(leftDoc);
         leftRegion = new PanelRegion(leftDoc);
         rootRegion.addChild(leftRegion);

         addKeyListener(this);
         addMouseListener(this);
         addMouseMotionListener(this);
         addMouseWheelListener(this);

         didSetup = true;
      }

      if (leftDoc.getIsTextView() && rightDoc == null) {
         rightDoc = new ConcurrentDoc(this);
         rightDoc.setBrowser(browser);
         rightDoc.setInBrowser(isBrowser);
         rightDoc.setName("rightDoc");
         try {
             watcher.registerDoc(rightDoc);
         } catch (Exception e) {
             e.printStackTrace();
         }
         rightRegion = new PanelRegion(rightDoc);
         rootRegion.addChild(rightRegion);
         w = 0;
      }
      else if (rightDoc != null && !leftDoc.getIsTextView()) {
          watcher.unregisterDoc(rightDoc);
          rootRegion.removeChild(rightRegion);
          focusDoc = leftDoc;
          rightRegion = null;
          rightDoc = null;
          w = 0;
      }

      Graphics leftG = g;
      Graphics rightG = null;
      if (rightDoc != null) {
          int w = getWidth()/2, h = getHeight(), x = 0, y = 0;
          leftG = g.create(x, y, w, h);
          leftRegion.setLocation(x, y);
          leftRegion.setSize(w, h);
          x += w;
          rightG = g.create(x, y, w, h);
          rightRegion.setLocation(x, y);
          rightRegion.setSize(w, h);
      }
      else {
          leftRegion.setLocation(0, 0);
          leftRegion.setSize(getWidth(), getHeight());
      }

      if (w != getWidth() || h != getHeight()) {
         if (rightDoc != null) {
             int w = getWidth()/2, h = getHeight();
             leftDoc.setSize(w, h);
             leftDoc.layout(leftG);
             rightDoc.setSize(w, h);
             rightDoc.layout(rightG);
         } else {
             leftDoc.setSize(getWidth(), getHeight());
             leftDoc.layout(leftG);
         }
      }

      w = getWidth();
      h = getHeight();

      watcher.testAndClearUpdated();
      leftDoc.redraw(leftG);
      if (rightDoc != null) {
          rightDoc.redraw(rightG);
      }
   }

   public void keyPressed(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int keyChar = e.getKeyChar();

      switch (keyChar) {
      case 11:
      case 27:
	 return;
      }

      if (keyChar == 65535) {
         switch (keyCode) {
         case  16: isShift   = true; return;
         case  17: isControl = true; return;
         case  18: isAlt     = true; return;
         case 524: // windows key
         case 157: isCommand = true; return;

         case 37: leftDoc.specialKeyPress(Doc.LEFT ); return;
         case 38: leftDoc.specialKeyPress(Doc.UP   ); return;
         case 39: leftDoc.specialKeyPress(Doc.RIGHT); return;
         case 40: leftDoc.specialKeyPress(Doc.DOWN ); return;
         }
      }

      int arg = keyChar | (isAlt ? 128 : 0) | ((isControl||isCommand) ? 256 : 0);
      focusDoc.keyEvent(Region.PRESS, arg);
      focusDoc.keyEvent(Region.RELEASE, arg);
   }

   public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int keyChar = e.getKeyChar();

      switch (keyChar) {
      case 11:
         if (rightDoc != null) {
            int i = focusDoc.getIndexAtTopOfScreen();
	    if (leftDoc == focusDoc)
	       rightDoc.setIndexAtTopOfScreen(i);
            else
	       leftDoc.setIndexAtTopOfScreen(i);
	    nextFocusDoc = focusDoc;
         }
	 return;
      case 27:
         watcher.reloadFile();
	 return;
      }

      if (keyChar == 65535) {
         switch (keyCode) {
         case  16: isShift   = false; return;
         case  17: isControl = false; return;
         case  18: isAlt     = false; return;
         case 524: // windows key
         case 157: isCommand = false; return;

         case 37: leftDoc.specialKeyRelease(Doc.LEFT ); return;
         case 38: leftDoc.specialKeyRelease(Doc.UP   ); return;
         case 39: leftDoc.specialKeyRelease(Doc.RIGHT); return;
         case 40: leftDoc.specialKeyRelease(Doc.DOWN ); return;
         }
      }
   }

   public void keyTyped(KeyEvent e) { }

   public void mouseEntered (MouseEvent e) { }
   public void mouseExited  (MouseEvent e) { }
   public void mouseClicked (MouseEvent e) { rootRegion.mouseEvent(Region.CLICK  ,e.getX(),e.getY()); }
   public void mouseMoved   (MouseEvent e) { rootRegion.mouseEvent(Region.MOVE   ,e.getX(),e.getY()); }
   public void mousePressed (MouseEvent e) { rootRegion.mouseEvent(Region.PRESS  ,e.getX(),e.getY()); }
   public void mouseDragged (MouseEvent e) { rootRegion.mouseEvent(Region.DRAG   ,e.getX(),e.getY()); }
   public void mouseReleased(MouseEvent e) { rootRegion.mouseEvent(Region.RELEASE,e.getX(),e.getY()); }

   public void mouseWheelMoved(MouseWheelEvent e) {
      isRealMouseWheel = true;
      focusDoc.mouseWheelDeltaY(e.getUnitsToScroll());
   }

   // an applet may optionally get mousewheel events from JS in the browser
   // (a hack to fix scrolling in Safari)
   public void mouseWheelMovedFromBrowser(int deltaY) {
      if (!isRealMouseWheel) {
         int d = -3;
         if (deltaY < 0) {
            d = 3;
         }
         focusDoc.mouseWheelDeltaY(d);
      }
   }

}

