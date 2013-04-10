// TODO: we should be able to use the pixel deltas directly from
// the browser mouse event to do the right flavor of scrolling

import java.awt.*;
import java.awt.event.*;
import storage.*;
import util.*;
import test.*;
import interpreter.*;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URI;
import netscape.javascript.JSObject;
import netscape.javascript.JSException;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.HashMap;

public class TestSharedDoc extends BufferedApplet implements KeyListener,
                                                       MouseListener,
                                                       MouseMotionListener,
                                                       MouseWheelListener
{
   class PanelRegion extends Region {
       Doc d;

       public PanelRegion(Doc d) {
           this.d = d;
       }

       @Override
       public boolean mouseEvent(int eventType, int x, int y) {
           if (contains(x,y)) {
               focusDoc = d;

               return d.mouseEvent(eventType, x-this.x, y-this.y);
           } else {
               return false;
           }
       }
   }

   Region rootRegion;
   PanelRegion leftRegion, rightRegion;
   Doc focusDoc;
   ConcurrentDoc leftDoc;
   ConcurrentDoc rightDoc;
   boolean enableRightDoc;
   PadClient client;
   Graphics g;
   boolean isShift, isAlt, isControl, isCommand;
   int w, h, codeFontHeight = 14;
   Color codeBgColor = new Color(245, 250, 255);
   Color selectionColor = new Color(255, 0, 0, 32);
   boolean isRealMouseWheel;
   URL serverURL;
   URI serverURI;

   boolean setEPLMessage = false;
   boolean setROMessage = false;

   String padName = null;

   boolean didSetup = false;

   public void render(Graphics g) {
      this.g = g;

      if (!didSetup) {
         // initial setup

         rootRegion = new Region();

         URL docUrl = getDocumentBase();
         int serverPort = docUrl.getDefaultPort();
         if (docUrl.getPort() != -1) {
            serverPort = docUrl.getPort();
         }
         try {
            serverURI = new URI(docUrl.getProtocol(), null, docUrl.getHost(), serverPort, "", null, null);
         } catch (URISyntaxException e) { }

         try {
            serverURL = serverURI.toURL();
         } catch (MalformedURLException e) { }

         padName = getParameter("pad");
         client = new PadClient(serverURL, padName);
         leftDoc = new ConcurrentDoc(this);
         leftDoc.setName("leftDoc");
         focusDoc = leftDoc;
         try {
            client.registerDoc(leftDoc);
         } catch (Exception e) { client = null; }
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
         rightDoc.setName("rightDoc");
         try {
             client.registerDoc(rightDoc);
         } catch (Exception e) {
             e.printStackTrace();
         }
         rightRegion = new PanelRegion(rightDoc);
         rootRegion.addChild(rightRegion);
         
         w = 0;
      } else if (rightDoc != null && !leftDoc.getIsTextView()) {
          client.unregisterDoc(rightDoc);
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

      //statusMessages();

      if (client.getNews()) {
          leftDoc.layout(leftG);
          if (rightDoc != null) {
              rightDoc.layout(rightG);
          }
      }

      leftDoc.redraw(leftG);
      if (rightDoc != null) {
          rightDoc.redraw(rightG);
      }
   }

   void statusMessages() {
      if (!setEPLMessage) {
         String epl_url = serverURI.resolve("/p/"+padName).toString();
         addMenuLink("raw text (new window)", epl_url, "View in the Etherpad Lite text editor");
         setEPLMessage = true;
      }

      if (!setROMessage) {
         if (!client.isPadReadOnly()) {
            String ro_url = serverURI.resolve("/d/"+client.getReadOnlyPadId()).toString();
            addMenuLink("sandbox (new window)", ro_url, "Open a view that does not send changes to the shared document");

            setROMessage = true;
         }
      }

      if (client.isPadReadOnly()) {
         updateStatusBar("sandbox mode", "changes are not sent to the shared document");
      } else {
         updateStatusBar("sandbox mode", "");
      }
      updateStatusBar("connection", client.getConnectionStatusString());
   }

   void addMenuLink(String text, String url, String title) {
       try {
           JSObject window = JSObject.getWindow(this);
           JSONObject o = new JSONObject();
           o.put("text", text);
           o.put("url", url);
           o.put("title", title);

           window.eval("MenuBar.addLink(" + o.toString() + ")");
       }
       catch (JSException e) { }
       catch (JSONException e) { }
   }

   HashMap<String, String> statusBarText = new HashMap<String, String>();

   void updateStatusBar(String category, String newText) {
       if (!statusBarText.containsKey(category) ||
           !statusBarText.get(category).equals(newText)) {
           statusBarText.put(category, newText);
           try {
               JSObject window = JSObject.getWindow(this);
               JSONObject o = new JSONObject();
               o.put("category", category);
               o.put("text", newText);

               window.eval("StatusBar.setStatus(" + o.toString() + ")");
           }
           catch (JSException e) { }
           catch (JSONException e) { }
       }
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

         case 37: leftDoc.specialKeyPress(Doc.LEFT ); return;
         case 38: leftDoc.specialKeyPress(Doc.UP   ); return;
         case 39: leftDoc.specialKeyPress(Doc.RIGHT); return;
         case 40: leftDoc.specialKeyPress(Doc.DOWN ); return;
         }
      }

      focusDoc.keyPress(keyChar | (isAlt ? 128 : 0) | ((isControl||isCommand) ? 256 : 0));
   }

   public void keyReleased(KeyEvent e) {
      int keyCode = e.getKeyCode();
      int keyChar = e.getKeyChar();

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

      focusDoc.keyRelease(keyChar | (isAlt ? 128 : 0) | ((isControl||isCommand) ? 256 : 0));
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

