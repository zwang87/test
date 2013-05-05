
/*
   Allow better drag-select of figures.
   Scroll when drag-selecting off screen.
*/

package test;

import bsh.Interpreter;
import java.io.*;
import javax.imageio.ImageIO;
import java.net.URL;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import util.*;
import draw.Draw;

import java.awt.image.*;

public class Doc extends BlockRegion implements Types
{
   public Doc(Component component) {
      this.component = component;
      setName("root");
      images.setApplet((java.applet.Applet)component);
   }

   public void setBrowser(String browser) {
      this.browser = browser;
   }

   public void setInBrowser(boolean state) {
      isInBrowser = state;
   }

   public int getIndexAtTopOfScreen() {
      return caretAtPosition(0, 0);
   }

   public void setIndexAtTopOfScreen(int i) {
      if (i >= 0) {
         indexAtTopOfScreen = i;
         layout(graphics);
      }
   }

   public void setSize(int width, int height) {
      super.setSize(width, height);
      overlayRegion.isVisible = false;
   }

   public void launch(String address) {
      try {
         if (isInBrowser) {
            java.applet.Applet applet = (java.applet.Applet)component;
            URL url = new java.net.URI(applet.getDocumentBase().toExternalForm()).resolve(address).toURL();
            applet.getAppletContext().showDocument(url, "2");
         }
         else
            Runtime.getRuntime().exec( "open -a " + browser + " " + address );
      } catch (Exception e) { System.err.println(e); }
   }

   public void scrollToText(String s) {
      int i = caretAtPosition(0, lineHeight);
      i = text.indexOf(s, i);
      if (i < 0)
         i = text.indexOf(s);
      if (i >= 0)
         setIndexAtTopOfScreen(text.indexOf(s, i));
   }

   public void scrollBy(int dy) {
      scroll.setScrollY(scroll.getScrollY() + dy);

      if (overlayRegion.isVisible)
         switch (overlayRegion.mode) {
         case OverlayRegion.BUTTON:
            if (! (overlayRegion.region instanceof MenuRegion))
               overlayRegionDy -= dy;
            break;
         }
   }

   public void mouseWheelDeltaY(int deltaY) {
      int dy = (int)(2 * scroll.getScale() * deltaY);
      dy = Math.max(-10, Math.min(10, dy));
      scrollBy(dy);
   }

   public class Element {
      public String getTypeString() {
         return isEnd ? "END " + Types.types[type] : Types.types[type];
      }

      public String getTextString() {
         return "\"" + Parse.escape(text.substring(iLo, iHi)) + "\"";
      }

      public String toString() {
         return getTypeString() + " " + getTextString();
      }

      int type, iLo = 0, iHi = 0;
      boolean isEnd;
   }

   public Element e(int n) { return (Element)elements.get(n); }
   public ArrayList elements = new ArrayList();

   public void setFontHeight(int h) {
      fonts.setHeight(h);
      smallFont = null;
      isDamage = true;
   }
   public int getFontHeight() { return fonts.getHeight(); }

   public int getCaret() { return caret; }
   public int getSelectionStart() { return selectionStart; }
   public int getSelectionEnd() { return selectionEnd; }

   public int docWidth() {
      return width - scroll.getWidth();
   }

   public void parse() {
      elements.clear();

      int i = Parse.findTag(text, 0);
      if (i < 0) {
         addElement(0, text.length(), TEXT, false);
         return;
      }
      else if (i > 0) {
         addElement(0, i, TEXT, false);
      }
         
      for ( ; (i = Parse.findTag(text, i)) >= 0 ; i++) {
         int j = text.indexOf('>', i);
         if (j < 0)
            break;

         int jj = text.indexOf('<', i + 1);
         if (jj > i && jj < j) {
            addElement(i, jj, TEXT, false);
            i = jj + 1;
         }

         boolean isEnd = text.charAt(i + 1) == '/';
         int type = findTagType(text, i + (isEnd ? 2 : 1));
         addElement(i, j + 1, type, isEnd);

         if (type == ANCHOR && ! isEnd) {
            String tag = text.substring(i + 1, j);
            int index = Parse.nextWord(tag, 0);
            String anchorName = tag.substring(index, tag.length());
            if (anchorName.indexOf("anchor") == 0) {
               int n = Parse.intValue(anchorName.substring(6, anchorName.length()));
               nextAnchorId = n + 1;
            }
         }

         int n = elements.size();

         // DO NOT PARSE TEXT BETWEEN "<figure>" AND "</figure>"

         if (n > 0 && ! e(n-1).isEnd && e(n-1).type == FIGURE) {
            int ii = text.indexOf("</figure>", j);
            if (ii >= 0) {
               addElement(j + 1, ii, TEXT, false);
               j = ii - 1;
            }
         }

         // DO NOT PARSE TEXT BETWEEN "<eval>" AND "</eval>"

         if (n > 0 && ! e(n-1).isEnd && e(n-1).type == EVAL) {
            int ii = text.indexOf("</eval>", j);
            if (ii >= 0) {
               addElement(j + 1, ii, TEXT, false);
               j = ii - 1;
            }
         }

         // DO NOT PARSE TEXT BETWEEN "<common>" AND "</common>"

         if (n > 0 && ! e(n-1).isEnd && e(n-1).type == COMMON) {
            int ii = text.indexOf("</common>", j);
            if (ii >= 0) {
               addElement(j + 1, ii, TEXT, false);
               j = ii - 1;
            }
         }

         if (text.charAt(j + 1) != '<') {
            int ii = text.indexOf('<', j);
            if (ii < 0)
               ii = text.length();
            addElement(j + 1, ii, TEXT, false);
            j = ii - 1;
         }

         i = j;
      }
   }

   class OverlayRegion extends Region {
      final static int BUTTON = 0;
      final static int FIND = 1;
      final static int COLOR = 2;

      Region region;
      int mode = 0;
      int index;

      public OverlayRegion() {
         super();
         setName("overlay");
         isVisible = false;
      }

      public void mouseEnter(int x, int y) {
         regionAtCursor = this;
         switch (mode) {
         case BUTTON:
            index = y / region.height;
            break;
         }
      }

      public void mouseExit(int x, int y) {
         isVisible = false;
         root.isDamage = true;
      }

      public boolean mouseMove(int x, int y) {
         switch (mode) {
         case BUTTON:
            index = y / region.height;
            break;
         }
         return true;
      }

      public boolean mousePress(int x, int y) {
         regionAtCursor = this;
         switch (overlayRegion.mode) {
         case COLOR:
            if (contains(gx + x, gy + y)) {
               isVaryingColor = true;
               setRGB(x, y);
            }
            break;
         }
         return true;
      }

      public void mouseDrag(int x, int y) {
         switch (overlayRegion.mode) {
         case COLOR:
            if (isVaryingColor) {
               setRGB(x, y);
            }
            break;
         }
      }

      public void mouseRelease(int x, int y) {
         if (contains(gx + x, gy + y))
            switch (overlayRegion.mode) {
            case BUTTON:
               ButtonRegion br = (ButtonRegion)region;
               int newState = index == 0 ? br.state + 1 : br.state;
               br.setState((index + newState) % br.nStates);
               break;
            }
      }

      public void setVisible(boolean state) {
         super.setVisible(state);

         // The following line forces creating a new Graphics object.

         // It is needed because overlayRegion may not change size
         // when the applet resizes, and its old Graphics object
         // would then become invalid.

         G = null;
      }

      void setRGB(int x, int y) {
         int n = x < width * 5 / 14 ? 0 : x < width * 9 / 14 ? 1 : 2;
         int r = width / 7;
         int y0 = 4 * r + r/2;
         int y1 = height - r - r/2;
         rgb[n] = Math.max(0, Math.min(255, 255 - 255 * (y - y0) / (y1 - y0)));
         rgbColor = new Color(rgb[0], rgb[1], rgb[2]);
         setTextRGB();
      }

      char hexDigit(int n) {
         if (n < 10)
            return (char)('0' + n);
         else
            return (char)('A' + (n - 10));
      }

      public void overlay(Graphics g) {
         switch (mode) {
         case COLOR:
            g.setColor(colorScrim);
            g.fill3DRect(0, 0, width, height, true);
            int r = width / 7;
            int y0 = 4 * r;
            g.setColor(rgbColor);
            g.fillRect(r, r, width - 2 * r, y0 - 2 * r);
            for (int n = 0 ; n < 3 ; n++) {
               int x = width * (2 * n + 1) / 7;
               g.setColor(Color.white);
               g.fill3DRect(x + r/4, y0, r/2, height - y0 - r, false);
               int y = y0 + r/2 + (height - y0 - 2 * r) * (255 - rgb[n]) / 255;
               g.setColor(n==0 ? Color.red : n==1 ? green : Color.blue);
               g.fill3DRect(x, y - r / 2, r, r, true);
               g.setColor(Color.white);
               g.setFont(rgbFont);
               String hex = "" + hexDigit(rgb[n] >> 4) + hexDigit(rgb[n] & 0xf);
               g.drawString(hex, x + 2, y + r / 4);
            }
            break;

         case FIND:
            int b = 10;
            g.setColor(findBorderColor);
            g.fill3DRect(0, 0, width, height, true);
            g.fill3DRect(6*b, b, width-7*b, height-2*b, false);
            g.setColor(findBgColor);
            g.fillRect(6*b+1, b+1, width-7*b-2, height-2*b-2);
            g.setColor(Color.black);
            g.setFont(findPromptFont);
            g.drawString("FIND:", b, height*2/3 - 1);
            g.setFont(findTextFont);
            g.setColor(Color.red);
            g.drawString(findText + "_", 6*b+b*3/4, height*2/3 - 2);
            g.setColor(Color.black);
            g.drawString(findText, 6*b+b*3/4, height*2/3 - 2);
            break;
         case BUTTON:
            if (region != null && region instanceof ButtonRegion) {
               if (isTextView)
                  return;

               ButtonRegion br = (ButtonRegion)region;
               g.setColor(br.offColor);
               g.fill3DRect(0, 0, width, height, true);
               g.setColor(br.onColor);
               g.fill3DRect(0, br.height * index, width, br.height, true);

               g.setColor(Color.black);
               for (int i = 0 ; i < br.nStates ; i++) {
                  int n = (i + br.state) % br.nStates;
                  String s = br.getText(n);
                  int sy = br.height * i + br.height * 3 / 4;
                  g.setFont(br.font);
                  if (br instanceof MenuRegion) {
                     g.setColor(Color.black);
                     g.drawString(s.substring(6, s.length()), 0, sy);
                     g.setFont(((MenuRegion)br).cmdFont);
                     g.drawString(s.substring(0, 6), width * 21 / 30, sy - br.height / 20);
                  }
                  else
                     g.drawString(s, 0, sy);
               }
            }
            break;
         }
      }

      void setBoundsFromButton() {
         ButtonRegion br = (ButtonRegion)region;
         x = br.gx;
         y = br.gy + overlayRegionDy;
         width = 0;
         Font saveFont = graphics.getFont();
         graphics.setFont(br.font);
         for (int i = 0 ; i < br.nStates ; i++) {
            int n = (i + br.state) % br.nStates;
            width = Math.max(width, Parse.stringWidth(graphics, br.getText(n)));
         }
         x = Math.min(x, docWidth() - width);
         graphics.setFont(saveFont);
         height = br.height * br.nStates;
      }

      boolean isVaryingColor = false;
      Font rgbFont = new Font("Monospaced", Font.BOLD, 12);
      Color green = new Color(0, 180, 0);
      Color rgbColor = new Color(rgb[0], rgb[1], rgb[2]);
      Color colorScrim  = new Color(200, 200, 200, 128);
      Color scrim  = new Color(255, 255, 255, 128);
      Color bgColor  = new Color(160, rb2g(160, 255), 255, 160);

      Color findBgColor = new Color(250, 250, 250);
      Color findBorderColor = new Color(240, 240, 240);
      Font findPromptFont = new Font("Sanserif", Font.BOLD, 14);
      Font findTextFont = new Font("Sanserif", Font.PLAIN, 15);
      String findText = "";
   }
   int rgb[] = {0, 128, 255};
   OverlayRegion overlayRegion = new OverlayRegion();

   void getTextRGB() {
      if (selectionEnd == selectionStart)
         return;

      int lo = selectionStart;
      while (lo-1 < selectionEnd && text.charAt(lo-1) != '(')
         lo++;

      int hi = lo;
      while (hi < selectionEnd && text.charAt(hi) != ')')
         hi++;

      String[] rgbInText = text.substring(lo, hi).split(",");
      if (rgbInText.length == 3)
         for (int n = 0 ; n < 3 ; n++)
            rgb[n] = Parse.intValue(rgbInText[n]);
   }

   void setTextRGB() {
      if (selectionEnd == selectionStart)
         return;

      int lo = selectionStart;
      while (lo-1 < selectionEnd && text.charAt(lo-1) != '(')
         lo++;

      int hi = lo;
      while (hi < selectionEnd && text.charAt(hi) != ')')
         hi++;

      if (lo < hi) {
         String newRGB = rgb[0] + "," + rgb[1] + "," + rgb[2];
         int dif = newRGB.length() - (hi - lo);
         replaceText(lo, hi, newRGB);
         setSelection(selectionStart, selectionEnd + dif);
      }
   }

   class NotesRegion extends Region {
       public void mouseEnter(int x, int y) {
          docNotes = notes;
       }

       public void mouseExit(int x, int y) {
          docNotes = null;
       }

       public NotesRegion(String notes) {
          this.notes = notes;
          graphics.setFont(labelFont);
          width = Parse.stringWidth(graphics, label);
          height = Parse.fontHeight(graphics);
       }

       public void overlay(Graphics g) {
          g.setFont(labelFont);
          g.setColor(labelColor);
          g.drawRoundRect(0, 0, width-1, height-1, height/2, height/2);
          g.drawString(label, 0, height * 5 / 6);
      }

      String notes;
      String label = " NOTES ";
      Font labelFont = new Font("Sanserif", Font.BOLD, 15);
      Color labelColor = new Color(128, 128, 255);
   }

   class ImageRegion extends Region {
      Image image;

      public void update(Graphics g) {
         super.update(g);
         g.drawImage(image, 0, 0, width, height, component);
      }
   }

   class BlockToggleRegion extends Region {
      public void overlay(Graphics g) {
         g.setColor(BlockRegion.edgeColor);
         int t = height/12, r = width/5, x = width - lineHeight*3/5, y = height/2;

         if (! block.isOpen && blockOpen[block.index] == 0.0) {
            g.fillRect(x-r,y-t,2*r,2*t);
            g.fillRect(x-t,y-r,2*t,r-t);
            g.fillRect(x-t,y+t,2*t,r-t);
         }

         if (block.isOpen && blockOpen[block.index] == 1.0) {
            g.fillRect(x-r,y-t,2*r,2*t);
         }
      }

      public boolean mousePress(int x, int y) {
         return true;
      }

      public void mouseRelease(int x, int y) {
         Region lineRegion = block.child(0);
         if (lineRegion.nChildren() == 0)
            return;
         TextRegion textRegion = (TextRegion)lineRegion.child(0);

         for (int hi = textRegion.iLo ; hi >= 0 ; ) {

            while (hi >= 0 && text.charAt(hi) != '>')
               hi--;
            int lo = hi - 1;
            while (lo >= 1 && text.charAt(lo - 1) != '<')
               lo--;

            if (text.indexOf(keyword, lo) == lo) {
               block.isOpen = ! block.isOpen;
               replaceText(lo, hi, keyword + (block.isOpen ? " open" : ""), true);
               break;
            }

            hi = lo - 1;
         }
      }

      DocBlockRegion block;
      int X[] = new int[7], Y[] = new int[7];
      String keyword = "block";
   }

   class TextRegion extends Region {
      public TextRegion(int x, int y, int iLo, int iHi, String tagData) {
         super();
         this.x = x;
         this.y = y;
         String label = text.substring(iLo, iHi);
         setFont(boldLevel > 0, italicLevel > 0, codeLevel > 0);
         this.width = Parse.stringWidth(graphics, label);
         if (yShiftIndicator != 0)
            this.width = this.width * 3 / 5;
         this.iLo = iLo;
         this.iHi = iHi;
         font = graphics.getFont();
         this.height = lineHeight;
         color = colorStack[colorStackTop];
         yLo = 0;
         yHi = lineHeight() - 1;
      }

      public boolean mousePress(int x, int y) {
         if (anchorId < 0)
            return super.mousePress(x, y);
          return true;
      }

      public void mouseDrag(int x, int y) {
         if (anchorId < 0)
            super.mouseDrag(x, y);
      }

      public void mouseRelease(int x, int y) {
         if (anchorId < 0) {
            super.mouseRelease(x, y);
            return;
         }

         if (anchorName[anchorId] != null) {
            if (anchorName[anchorId].charAt(0) == '#') {
               scrollBy(anchorY[anchorId] - root.gy - scroll.getScrollY());
               root.isDamage = true;
            }
            else
               launch(anchorName[anchorId]);
         }
      }

      public int textY(Graphics g) {
         return height * 3 / 4 + yShift;
      }

      public void overlay(Graphics g) {
         super.overlay(g);

         boolean isChangingFont = isEvalText && font != fonts.code;
         if (isChangingFont)
            font = fonts.code();
         g.setFont(font);
         if (isChangingFont)
            width = Parse.stringWidth(g, text.substring(iLo, iHi));

         g.setColor(color);
         if (yShift != 0)
            g.setFont(smallFont());
         g.drawString(getText(), 0, textY(g));

         if (anchorId >= 0) {
            g.setColor(Color.blue);
            g.fillRect(0, height * 3 / 4, width, 1);
         }

         highlightSelection(g);
      }

      void highlightSelection(Graphics g) {
          //wz
          Color nextSelectionColor = selectionColor;
          if (selectionStart < iHi && selectionEnd > iLo) {
              int i0 = Math.max(iLo, selectionStart);
              int i1 = Math.min(iHi, selectionEnd);
              int x0 = Parse.stringWidth(g, text.substring(iLo, i0));
              int x1 = Parse.stringWidth(g, text.substring(iLo, i1));
              g.setColor(selectionColor);
              g.fillRect(x0, 0, x1 - x0, height);
          }
          
          /*
          if(caretStartList == null){
              if (selectionStart < iHi && selectionEnd > iLo) {
                  int i0 = Math.max(iLo, selectionStart);
                  int i1 = Math.min(iHi, selectionEnd);
                  int x0 = Parse.stringWidth(g, text.substring(iLo, i0));
                  int x1 = Parse.stringWidth(g, text.substring(iLo, i1));
                  g.setColor(selectionColor);
                  g.fillRect(x0, 0, x1 - x0, height);
              }
          }else 
           *///if(caretStartList.length > 0){
          if(caretStartList != null){
            for(int i = 0; i < caretStartList.length; i++){
                  if(i != curCaretPos){
                  nextSelectionColor = selectionColor;
                  if (caretStartList[i] < iHi && caretEndList[i] > iLo) {
                      int i0 = Math.max(iLo, caretStartList[i]);
                      int i1 = Math.min(iHi, caretEndList[i]);
                      int x0 = Parse.stringWidth(g, text.substring(iLo, i0));
                      int x1 = Parse.stringWidth(g, text.substring(iLo, i1));
                      nextSelectionColor = selectionColor;
                      if(colorList[i] != null){
                          nextSelectionColor = new Color(colorList[i].getRed(), colorList[i].getGreen(), colorList[i].getBlue(), 64);
                      }
                      g.setColor(nextSelectionColor);
                      g.fillRect(x0, 0, x1 - x0, height);
                  }
                  }
              }
          }
           //*/
          ///wz
      }

      public String getText() {
         return text.substring(iLo, iHi);
      }

      boolean isReservedWord(String s) {
         for (int n = 0 ; n < reservedWords.length ; n++)
            if (s.equals(reservedWords[n]))
               return true;
         return false;
      }
      String reservedWords[] = {"duration"};

      void setTagValue(String s) {
         for (int i = iLo - 1 ; i > 0 ; i--) {
            int c = text.charAt(i);
            if (c == '<') {
               replace(iLo - 1, iLo - 1, " " + s);
               return;
            }
            else if (c == '=') {

               // IF THIS IS A VARIABLE IN A FIGURE, CHANGE VALUE IN FIGURE.

               int j = i;
               while (j > 0 && ! Parse.isSpace(text.charAt(j-1)))
                  j--;

               String var = text.substring(j, i);
               if (isReservedWord(var))
                  continue;

               int n = var.indexOf('.');
               if (n >= 0)
                 setVar(var.substring(0, n), var.substring(n + 1, var.length()), s);

               for (j = i + 1 ; j < iLo - 1 && text.charAt(j) != ' ' ; j++)
                  ;
               replace(i + 1, j, s);
               return;
            }
         }
      }

      void replace(int lo, int hi, String s) {
         if (! s.equals(text.substring(lo, hi)) || markForUndo) {
            int d = s.length() - (hi - lo);
            replaceText(lo, hi, s, markForUndo);
            iLo += d;
            iHi += d;
         }
      }

      int iLo, iHi, yLo, yHi;
      Font font, smallFont;
      Color color = Color.black;
      boolean isVarText = false;
      boolean isEvalText = false;
      boolean isSharedEval = false;
      boolean markForUndo = true;
      boolean isNarration = false;
      int anchorId = -1;
      int yShift = 0;
   }

   Font smallFont() {
      if (smallFont == null)
         smallFont = new Font("Sanserif", Font.PLAIN, fonts.getHeight() / 2);
      return smallFont;
   }

   Font smallFont;

   class MenuRegion extends ButtonRegion {
      MenuRegion(int x, int y, int lo, int hi, String s) {
         super(x, y, lo, hi, s);
         onColor = new Color(220, rb2g(220, 255), 255, 240);
         offColor = new Color(240, rb2g(240, 255), 255, 240);
         nStates = choice.length;
      }
      public void overlay(Graphics g) {
         if (fontHeight != lineHeight / 2) {
            fontHeight = lineHeight * 3 / 5;
            font = new Font("Sanserif", Font.PLAIN, fontHeight);
            cmdFont = new Font("Sanserif", Font.BOLD, fontHeight * 75 / 100);
         }
         super.overlay(g);
      }
      public void setState(int s) {
         handleControlKey(choice[s].charAt(5));
      }
      public String getText() {
         return " menu";
      }
      public String getText(int s) {
         return choice[s];
      }
      String choice[] = {
         " cmd [ smaller figs ",
         " cmd ] larger figs ",
         " cmd - smaller text ",
         " cmd + larger text ",
         " cmd b bold ",
         " cmd c copy ",
         " cmd d down ",
         " cmd e eval ",
         " cmd f find ",
         " cmd g grid ",
         " cmd i italic ",
         " cmd l create link ",
         " cmd r rgb ",
         " cmd u up ",
         " cmd v paste ",
         " cmd w whiteboard ",
         " cmd x cut ",
         " cmd z undo ",
      };
      Font cmdFont;
   };
   MenuRegion menu;

   int nAnchorNames = 0;
   String[] anchorName = new String[100];
   int[] anchorY = new int[100];

   int findAnchorName(String name) {
      for (int n = 0 ; n < nAnchorNames ; n++)
         if (anchorName[n].equals(name))
            return n;
      anchorName[nAnchorNames] = name;
      anchorY[nAnchorNames] = -1;
      return nAnchorNames++;
   }

   int nextAnchorId = 0;

   String generateAnchorName() {
      return "anchor" + nextAnchorId++;
   }


   class CharacterRegion extends TextRegion {
      public CharacterRegion(int x, int y, int iLo, int iHi, String tagData) {
         super(x, y, iLo, iHi, tagData);
         String label = text.substring(iLo, iHi);
         for (int n = 0 ; n < unicode.length ; n++)
            if (label.equals(unicode[n][0])) {
               this.n = n;
               break;
            }
         width = Parse.stringWidth(graphics, getText());
      }
      public String getText() {
         return isTextView ? "" : unicode[n][1];
      }
      void highlightSelection(Graphics g) {
         if (selectionStart < iHi && selectionEnd > iLo) {
            String s = text.substring(iLo);
            int x0 = Parse.stringWidth(g, s);
            int x1 = Parse.stringWidth(g, s + getText());
            g.setColor(selectionColor);
            g.fillRect(x0, 0, x1 - x0, height);
         }
      }

      int n = 0;
   }

   class TagRegion extends TextRegion {
      public TagRegion(int x,int y,int lo,int hi,String tagData) {
         super(x,y,lo,hi,tagData);
         color = tagColor;
         graphics.setFont(font = fonts.code());
         width = Parse.stringWidth(graphics, text.substring(iLo, iHi));
      }
   }

   class EvalRegion extends TextRegion {
      Interpreter interpreter = new Interpreter();

      public EvalRegion(int x,int y,int lo,int hi,String tagData) {
         super(x,y,lo,hi,tagData);
         int width = Parse.stringWidth(graphics, getText());
         if (isDamage = width > this.width)
            this.width = width;
      }

      public String getText() {
         if (isTextView)
            return text.substring(iLo, iHi);
         try {
            interpreter.set("time", currentTime / 1000.0);
            Object result = interpreter.eval(text.substring(iLo, iHi));
            return result == null ? "" : result.toString();
         } catch (Exception e) { return ""; }
      }

      public void update(Graphics g) {
         super.update(g);
         g.setColor(outlineColor);
         g.drawRect(0, 0, width - 1, height * 4 / 5 - 1);
      }

      public void overlay(Graphics g) {
         super.overlay(g);
         g.setColor(color);
         g.setFont(font);
         String result = getText();
         g.drawString(result, 0, fontHeight * 4 / 5);
         width = Parse.stringWidth(g, result);

         if (selectionStart < iHi && selectionEnd > iLo) {
            g.setColor(selectionColor);
            g.fillRect(0, 0, width, height);
         }
      }

      Color outlineColor = new Color(160, 160, 160);
   }

   class HighlightRegion extends TextRegion {
      public HighlightRegion(int x,int y,int lo,int hi,String tagData) {
         super(x,y,lo,hi,tagData);

         setName("highlight");
      }

      public void update(Graphics g) {
         super.update(g);
         int b = isOver() ? 1 : 0;
         g.setColor(isOver() && ! overlayRegion.isOver() ? highlightColor : bgColor);
         g.fillRect(0, yLo - b, width - 1, yHi - yLo + 2 * b);
      }

      public void mouseEnter(int x, int y) {
         if (overlayRegion.isOver())
            return;
         super.mouseEnter(x, y);
         setTagValue("true");
         regionAtCursor = this;
      }

      public void mouseExit(int x, int y) {
         super.mouseExit(x, y);
         setTagValue("false");
      }

      int state = 0;
      Color bgColor = new Color(255, 255, 210);
      Color highlightColor = new Color(255, 255, 0);
   }

   class ButtonRegion extends TextRegion {
      public ButtonRegion(int x,int y,int lo,int hi,String tagData) {
         super(x,y,lo,hi,tagData);
         String choices = text.substring(iLo, iHi);
         String[] labels = choices.split("[|]");
         nStates = labels.length;
         int i = tagData.indexOf('=');
         state = Parse.intValue(i >= 0 ? tagData.substring(i + 1, tagData.length())
                                       : tagData);
         width = Parse.stringWidth(graphics, isTextView ? choices : labels[state]);

         setName("button");
      }

      public void mouseEnter(int x, int y) {
         super.mouseEnter(x, y);
         if (! overlayRegion.isVisible) {
            overlayRegionDy = 0;
            overlayRegion.region = this;
            overlayRegion.mode = OverlayRegion.BUTTON;
            overlayRegion.setBoundsFromButton();
            overlayRegion.setVisible(true);
            root.isDamage = true;
         }
      }

      public void mouseExit(int x, int y) {
         super.mouseExit(x, y);
         if (! overlayRegion.isOver()) {
            overlayRegion.setVisible(false);
            overlayRegion.region = null;
         }
      }

      public boolean mousePress(int x, int y) {
         if (! overlayRegion.isVisible) {
            regionAtCursor = this;
            return isPressed = true;
         }
         return false;
      }

      public void mouseRelease(int x, int y) {
         isPressed = false;
         if (isOver())
            setState((state + 1) % nStates);
      }

      public void setState(int state) {
         this.state = state;
         setTagValue("" + state);
      }

      public void update(Graphics g) {
         super.update(g);
         if (isTextView) {
            g.setFont(font);
            int x0 = Parse.stringWidth(g, text.substring(iLo, getStart()));
            int x1 = Parse.stringWidth(g, text.substring(iLo, getEnd()));

            if (isOver()) {
               g.setColor(offOverColor);
               g.fill3DRect(0, yLo - 1, width, yHi - yLo + 2, ! mouseDown);
               if (! mouseDown) {
                  g.setColor(onColor);
                  g.fillRect(x0, yLo, x1 - x0, yHi - yLo);
               }
            }
            else {
               g.setColor(offColor);
               g.fillRect(0, yLo, width, yHi - yLo);
               g.setColor(onColor);
               g.fillRect(x0, yLo, x1 - x0, yHi - yLo);
            }
         }
         else {
            if (isOver()) {
               g.setColor(onOverColor);
               g.fill3DRect(0, yLo - 1, width, yHi - yLo + 2, ! mouseDown);
            }
            else {
               g.setColor(onColor);
               g.fillRect(0, yLo, width, yHi - yLo);
            }
         }
         super.update(g);
      }

      public int getStart() {
         return getStart(state);
      }

      public int getEnd() {
         return getEnd(state);
      }

      public int getStart(int state) {
         int i0 = iLo, i1 = i0;
         for (int n = 1 ; (i1 = text.indexOf('|',i0)) >= 0 && i1 <= iHi && n <= state ; n++)
            i0 = i1 + 1;
         return i0;
      }

      public int getEnd(int state) {
         int i0 = iLo, i1 = i0;
         for (int n = 1 ; (i1 = text.indexOf('|',i0)) >= 0 && i1 <= iHi && n <= state ; n++)
            i0 = i1 + 1;
         return i1 == -1 ? iHi : Math.min(i1, iHi);
      }

      public String getText() {
         if (isTextView)
            return text.substring(iLo, iHi);
         return getText(state);
      }

      public String getText(int state) {
         return text.substring(getStart(state), getEnd(state));
      }

      boolean isPressed = false;
      int state = 0, nStates = 2;
      Color onColor      = new Color(200, rb2g(200, 255), 255);
      Color onOverColor  = new Color(160, rb2g(160, 255), 255);
      Color offColor     = new Color(230, rb2g(230, 255), 255);
      Color offOverColor = new Color(230, rb2g(230, 255), 255);
   }

   class SliderRegion extends TextRegion {
      public SliderRegion(int x,int y,int lo,int hi,String tagData) {
         super(x,y,lo,hi,tagData);
         int i = tagData.indexOf('=');
         value = Parse.doubleValue(i >= 0 ? tagData.substring(i+1, tagData.length())
                                          : tagData);
      }

      public boolean mousePress(int x, int y) {
         markForUndo = true;
         regionAtCursor = this;
         if (enableSetValueOnSliderPress)
            setValue(min + x * (max - min) / width);
         enableSetValueOnSliderPress = false;
         return true;
      }

      public void mouseDrag(int x, int y) {
         markForUndo = false;
         regionAtCursor = this;
         mouseX = gx + Math.max(0, Math.min(x, width-1));
         mouseY = gy + Math.max(0, Math.min(y, height-1));
         setValue(min + x * (max - min) / width);
      }

      void setValue(double value) {
         this.value = value = Math.max(min, Math.min(max, value));
         setTagValue(Parse.round(value));
      }

      public void update(Graphics g) {
         super.update(g);
         int w = (int)(width * (value - min) / (max - min));
         if (isOver()) {
            g.setColor(offOverColor);
            g.fill3DRect(0, yLo - 1, width, yHi - yLo + 2, true);
            g.setColor(onOverColor);
            g.fillRect(1, yLo, w - 1, yHi - yLo);
         }
         else {
            g.setColor(offColor);
            g.fillRect(0, yLo, width, yHi - yLo);
            g.setColor(onColor);
            g.fillRect(0, yLo, w, yHi - yLo);
         }
         super.update(g);
      }

      Color onColor      = new Color(200, rb2g(200, 255), 255);
      Color onOverColor  = new Color(160, rb2g(160, 255), 255);
      Color offColor     = new Color(230, rb2g(230, 255), 255);
      Color offOverColor = new Color(220, rb2g(220, 255), 255);
      double value = 0.5, min = 0.0, max = 1.0;
   }

   int nrr = 0;
   RenderableRegion[] rr = new RenderableRegion[100];

   RenderableRegion getRenderableRegion(int i, Class subclass, int width, int height) {
      if (rr[i] == null || rr[i].getClass() != subclass ||
         rr[i].getWidth() != width || rr[i].getHeight() != height) {
         try {
            rr[i] = (RenderableRegion)(subclass.newInstance());
         } catch (Exception e) { System.err.println(e); }
         rr[i].init(width, height, component);
      }
      return rr[i];
   }

   class LineRegion extends Region {
      public void overlay(Graphics g) {
         if (nChildren > 0) {
            if (child(0) instanceof TextRegion) {
               TextRegion tr = (TextRegion)child(0);
               if (tr.isSharedEval && tr.iHi > tr.iLo) {
                  g.setColor(scrim);
                  g.fillRect(lineHeight, 0, width - 2 * lineHeight, height * 3 / 4);
               }
            }
         }
      }
      Color scrim = new Color(0, 255, 0, 16);
   }

   LineRegion addLineRegion(int y) {
      LineRegion lineRegion = new LineRegion();
      lineRegion.x = 0;
      lineRegion.y = y;
      lineRegion.width = docWidth();
      currentBlock().addChild(lineRegion);
      return lineRegion;
   }

   int centerFlag = 0;
   boolean isCentered = false;

   int bsTop = 0;
   BlockRegion blockStack[] = new BlockRegion[100];

   int nBlocks = 0;
   double blockOpen[] = new double[100];

   public BlockRegion currentBlock() {
      return blockStack[bsTop];
   }

   class DocBlockRegion extends BlockRegion {
      public void update(Graphics g) {
         super.update(g);

         if (isOpen && blockOpen[index] < 1.0) {
            blockOpen[index] = Math.min(1.0, blockOpen[index] + 0.05);
            root.isDamage = true;
         }
         else if (! isOpen && blockOpen[index] > 0.0) {
            blockOpen[index] = Math.max(0.0, blockOpen[index] - 0.05);
            root.isDamage = true;
         }

      }

      int index = 0;
   }

   int notesFontHeight = 18;
   Font notesFont = new Font("Sanserif", Font.PLAIN, notesFontHeight);
   Color notesScrim = new Color(255, 255, 255, 192);

   public void layout(Graphics g) {
      scroll.setX(docWidth());
      scroll.setHeight(height);

      nBlocks = 0;
      bsTop = 0;
      blockStack[bsTop] = this;

      g.setFont(fonts.plain());
      this.graphics = g;
      fontHeight = fonts.height();
      lineHeight = fontHeight * 4 / 3;
      border = lineHeight;
      colorStackTop = 0;
      clearChildren();
      addChild(scroll);
      int x = border, y = border - scroll.getScrollY();
      boldLevel = italicLevel = codeLevel = 0;
      nrr = 0;
      nvq = 0;
      nShared = 0;
      isSharedDamage = false;
      nEvals = 0;
      boolean isVarText = false;
      isEvalText = false;
      boolean isSharedEval = false;
      boolean isBulleted = false;
      int leftMargin = border;
      int rightMargin = border;

      LineRegion lineRegion = addLineRegion(y);

      for (int n = 0 ; n < elements.size() ; n++) {
         String tagData = "";
         if (text.charAt(e(n).iLo) == '<') {
            int i = text.indexOf(' ', e(n).iLo);
            if (i >= e(n).iLo && i < e(n).iHi)
               tagData = text.substring(i+1, e(n).iHi-1);
         }

         if (e(n).type == ANCHOR)
            anchorY[findAnchorName("#" + tagData)] = y;

         if (isTextView && e(n).type != TEXT) {
            g.setFont(fonts.code());
            String str = text.substring(e(n).iLo, e(n).iHi);
            int w = Parse.stringWidth(g, str);
            if (x + w > docWidth() - rightMargin) {
               y += lineHeight();
               x = leftMargin;
               adjustLine(lineRegion);
               lineRegion = addLineRegion(y);
            }
            int xx = x;
            if (isTextView && str.equals("</indent>"))
               xx -= lineHeight;
            TagRegion tagRegion = new TagRegion(xx, 0, e(n).iLo, e(n).iHi, tagData);
            lineRegion.addChild(tagRegion);
            x += w;
         }

         // Don't start layout out of range.

         if (x >= docWidth() - rightMargin) {
            x = leftMargin;
            y += lineHeight();
            adjustLine(lineRegion);
            lineRegion = addLineRegion(y);
         }

         switch (e(n).type) {
         case BR:
         case P:
            x = leftMargin;
            y += lineHeight() * (e(n).type == BR ? 1 : 2);
            adjustLine(lineRegion);
            lineRegion = addLineRegion(y);
            break;

         case SUB:
            if (! e(n).isEnd)
              yShiftIndicator += fontHeight / 6;
            else
              yShiftIndicator -= fontHeight / 6;
            break;

         case SUP:
            if (! e(n).isEnd)
              yShiftIndicator -= fontHeight / 2;
            else
              yShiftIndicator += fontHeight / 2;
            break;

         case BOLD:
            boldLevel = Math.max(0, boldLevel + (e(n).isEnd ? -1 : 1));
            break;

         case COLOR:
            if (e(n).isEnd) {
               if (colorStackTop > 0)
                  colorStackTop--;
            }
            else if (tagData.length() > 0)
               colorStack[++colorStackTop] = Parse.parseColor(tagData);
            break;

         case IMAGE:
            if (tagData.length() > 0) {
               String data = tagData;

               boolean isSlide = data.indexOf("slide ") == 0;
               if (isSlide)
                  data = data.substring(6, data.length());
                  
               String imageName = data;
               String sizeData = "";
               int ii = data.indexOf(' ');
               if (ii > 0) {
                  imageName = data.substring(0, ii);
                  sizeData = data.substring(ii + 1, data.length());
               }
               Image image = images.get(imageName);

               double size = 1.0;
               if (sizeData.length() > 0)
                  size = Parse.doubleValue(sizeData);

               if (image == null)
                  break;
               ImageRegion imageRegion = new ImageRegion();
               imageRegion.image = image;
               imageRegion.width = (int)(size * figureWidth * docWidth());
               imageRegion.x = (docWidth() - imageRegion.width) / 2;
               imageRegion.y = y;
               imageRegion.height = imageRegion.width * image.getHeight(component)
                                                      / image.getWidth(component);

               y += imageRegion.height - lineHeight();
               currentBlock().addChild((Region)imageRegion);
               if (isSlide) {
                  imageRegion.y -= imageRegion.height / 2;
                  y += 2 * height - imageRegion.height;
               }
            }
            break;

         case BLOCK:
            if (! e(n).isEnd) {
               DocBlockRegion block = new DocBlockRegion();

               block.index = nBlocks++;

               block.colorStackTop = colorStackTop;
               block.italicLevel = italicLevel;
               block.boldLevel = boldLevel;

               block.y = y;
               block.isOpen = tagData.indexOf("open") >= 0;

               if (nLayouts < 2)
                  blockOpen[block.index] = block.isOpen ? 1.0 : 0.0;

               block.border = leftMargin;
               block.width = docWidth();

               BlockRegion cb = currentBlock();
               cb.addChild(block);
               block.level = cb.level + 1;

               blockStack[++bsTop] = block;

               y = 0;
               if (! isTextView)
                  y -= lineHeight;

               leftMargin += BlockRegion.X_SHIFT;
            }
            else {
               leftMargin -= BlockRegion.X_SHIFT;

               DocBlockRegion block = (DocBlockRegion)currentBlock();

               colorStackTop = block.colorStackTop;
               italicLevel = block.italicLevel;
               boldLevel = block.boldLevel;

               BlockToggleRegion toggle = new BlockToggleRegion();
               toggle.block = block;
               toggle.height = lineHeight;
               toggle.width = block.border + BlockRegion.X_SHIFT;
               block.addChild(toggle);

               y = (int)(blockOpen[block.index] * y);

               if (isTextView)
                  y += lineHeight;
               else
                  y = Math.max(y, lineHeight);

               block.height = y;
               y = block.y + block.height - lineHeight;

               bsTop--;

               // ALL REMAINING UNSHARED EVALS IN BLOCK ARE ADDED TO ITS PARENT BLOCK

               for (int j = 0 ; j < block.nEvals ; j++)
                  currentBlock().addEval(block.evalLo[j], block.evalHi[j]);

               block.clearEvals();
            }
            break;

         case CENTER:
            centerFlag = e(n).isEnd ? -1 : 1;
            if (! isTextView)
               y -= lineHeight;
            break;

         case INDENT:
            leftMargin += e(n).isEnd ? -lineHeight : lineHeight;
            if (! isTextView)
               y -= lineHeight;
            break;

         case ITALIC:
            italicLevel = Math.max(0, italicLevel + (e(n).isEnd ? -1 : 1));
            break;

         case CODE:
            codeLevel = Math.max(0, codeLevel + (e(n).isEnd ? -1 : 1));
            break;

         case SHIFT:
            y += Parse.intValue(tagData) * lineHeight;
            break;

         case LINK:
            isInLink = ! e(n).isEnd;
            if (isInLink) 
               anchorId = findAnchorName(tagData);
            break;

         case HIGHLIGHT:
         case BUTTON:
         case SLIDER:
            if (n < elements.size() - 2 && e(n+1).type == TEXT &&
                e(n+2).type == e(n).type && ! e(n).isEnd && e(n+2).isEnd) {
               lineRegion.addChild(
                  e(n).type == HIGHLIGHT ?
                     (Region)new HighlightRegion(x, 0, e(n+1).iLo, e(n+1).iHi, tagData) :
                  e(n).type == BUTTON ?
                     (Region)new ButtonRegion(x, 0, e(n+1).iLo, e(n+1).iHi, tagData) :
                  e(n).type == SLIDER ?
                     (Region)new SliderRegion(x, 0, e(n+1).iLo, e(n+1).iHi, tagData) :
                     (Region)new EvalRegion(x, 0, e(n+1).iLo, e(n+1).iHi, tagData) ) ;
               n += isTextView ? 1 : 2;
               x += lineRegion.child(lineRegion.nChildren-1).width;
            }
            break;

         case EVAL:
            isInEval = ! e(n).isEnd;

            if (! e(n).isEnd) {
               String tag = text.substring(e(n).iLo, e(n).iHi);
               isSharedEval = tag.indexOf("shared") >= 0;
               if (tag.indexOf("ignore") == -1) {
                  if (isSharedEval) {

                     String sharedText = text.substring(e(n+1).iLo, e(n+1).iHi);
                     if ( sharedLo[nShared] != e(n+1).iLo ||
                          sharedHi[nShared] != e(n+1).iHi ||
                          ! sharedText.equals(shared[nShared])) {
                        sharedLo[nShared] = e(n+1).iLo;
                        sharedHi[nShared] = e(n+1).iHi;
                        shared[nShared] = sharedText;
                        isSharedDamage = true;
                     }
                     nShared++;
                  }
                  else
                     currentBlock().addEval(e(n+1).iLo, e(n+1).iHi);
               }
               isEvalText = true;
            }
            else {
               isSharedEval = false;
               isEvalText = false;
            }
            if (! isTextView && e(n+1).type == TEXT &&
                 text.charAt(e(n+1).iLo) == '\n' )
               y -= lineHeight;
            break;

         case VAR:
            if (! e(n).isEnd) {
               String src = text.substring(e(n+1).iLo, e(n+1).iHi);
               addToSetVarQueue(tagData, src);
               isVarText = true;
            }
            break;

         case CHARACTER:
            lineRegion.addChild(new CharacterRegion(x, 0, e(n).iLo + 1, e(n).iHi - 1, null));
            x += lineRegion.child(lineRegion.nChildren-1).width;
            break;

         case TEXT:

            if (! isTextView && (isInFigure || isInCommon || isInComment || isInNotes))
               break;
              
            if (isInEval)
               y += lineHeight / 4;

            // BREAK UP TEXT WHEREVER THERE ARE NEWLINE CHARACTERS.

            for (int i = e(n).iLo ; i < e(n).iHi ; ) {

               int j = text.indexOf('\n', i);
               if (j < 0 || j > e(n).iHi)
                  j = e(n).iHi;

               // BREAK UP TEXT AS NEEDED FOR LINE WRAPPING.

               setFont(boldLevel > 0, italicLevel > 0, codeLevel > 0);
               while (x + Parse.stringWidth(g, text.substring(i, j)) > docWidth() - rightMargin) {

                  // SHORTEN FIRST TO LAST CHAR THAT FITS, AND THEN TO A WORD BOUNDARY.

                  int ij = j;
                  while (x + Parse.stringWidth(g, text.substring(i, ij)) > docWidth() - rightMargin)
                     ij--;
                  while (ij > i && ! Parse.isSpace(text.charAt(ij)))
                     ij--;


                  boolean breakingLineAtWord = ij > i;
                  if (breakingLineAtWord) {
                     lineRegion.addChild(newTextRegion(x, 0, i, ij));
                     i = ij + 1;
                  }

                  x = leftMargin;
                  y += lineHeight();
                  adjustLine(lineRegion);
                  lineRegion = addLineRegion(y);

                  if (! breakingLineAtWord)
                     break;
               }

               // ALWAYS CREATE AT LEAST ONE TEXT REGION.

               TextRegion textRegion = newTextRegion(x, 0, i, j);
               lineRegion.addChild(textRegion);
               textRegion.isVarText = isVarText;
               textRegion.isEvalText = isEvalText;
               textRegion.isSharedEval = isSharedEval;
               isVarText = false;
               if (j == e(n).iHi)
                  x += lineRegion.child(lineRegion.nChildren-1).width;
               else {
                  x = leftMargin;
                  y += lineHeight();
                  adjustLine(lineRegion);
                  lineRegion = addLineRegion(y);
               }
               i = j + 1;
            }
            break;

         case COMMENT:
            isInComment = ! e(n).isEnd;
            if (isInComment && ! isTextView)
               y -= lineHeight;
            break;

         case COMMON:
            isInCommon = ! e(n).isEnd;

            if (isInCommon) {
               commonCode = text.substring(e(n+1).iLo, e(n+1).iHi);
               if (! isTextView)
                  y -= lineHeight;
            }

            break;

         case NOTES:
            isInNotes = ! e(n).isEnd;
            if (isInNotes) {
               if (! isTextView)
                  y -= lineHeight;
               NotesRegion r = new NotesRegion(text.substring(e(n+1).iLo, e(n+1).iHi));
               r.x = docWidth() - r.width;
               r.y = y + lineHeight - r.height / 2;
               currentBlock().addChild(r);
            }

            break;

         case FIGURE:
            y = layoutFigure(n, y);
            break;
         }
      }

      scroll.setScale((double)(y + scroll.getScrollY()) / height);
      redraw(graphics);

      for (int n = 0 ; n < nvq ; n++)
         setVar(varq[n], valq[n]);

      menu = new MenuRegion(0, 0, 0, 0, "");
      menu.width = 2 * lineHeight;
      menu.height = lineHeight * 4 / 5;
      menu.x = docWidth() - menu.width;
      menu.y = 0;

      addChild(menu);

      addChild(overlayRegion);

      if (! isVaryingNumber)
         mouseEvent(mouseDown ? Region.PRESS : Region.MOVE, mouseX, mouseY);

      // FORCE A SCROLL TO A PARTICULAR INDEX IN THE DOC

      if (indexAtTopOfScreen >= 0) {
         Region r = findRegionAt(root, indexAtTopOfScreen);
         if (r != null)
            scrollBy(r.gy);
         indexAtTopOfScreen = -1;
      }

      nLayouts++;
   }

   class FigureRegion extends Region {
      public void mouseEnter(int x, int y) {
         super.mouseEnter(x, y);
         for (int n = 1 ; n < nChildren ; n++)
            child(n).setVisible(true);
      }
      public void mouseExit(int x, int y) {
         super.mouseExit(x, y);
         for (int n = 1 ; n < nChildren ; n++)
            child(n).setVisible(false);
      }
   }

   int layoutFigure(int n, int y) {

      isInFigure = ! e(n).isEnd;

      if (isInFigure) {
         
         if (child(nChildren-1).nChildren == 0)
            removeChild(child(nChildren-1));

         if (! isTextView) {
            FigureRegion figureRegion = new FigureRegion();
            double aspect = 4.0 / 3.0;
            String tag = text.substring(e(n).iLo, e(n).iHi);
            int i = Parse.nextWord(tag, 0);
            if (i >= 0 && i < tag.length() - 1) {
               String[][] tags = Parse.parseTags(tag.substring(i, tag.length() - 1));
               for (int ti = 0 ; ti < tags.length ; ti++) {
                  if (tags[ti][0].equals("name"))
                     figureRegion.setName(tags[ti][1]);
                  else if (tags[ti][0].equals("aspect"))
                     aspect = Parse.doubleValue(tags[ti][1]);
               }
            }

            currentBlock().addChild((Region)figureRegion);

            figureRegion.x = 0;
            figureRegion.y = y;
            figureRegion.width = docWidth();

            int w = (int)(figureWidth * docWidth()), h = (int)(w / aspect);
            RenderableRegion r = getRenderableRegion(nrr++, RenderedRegion.class, w, h);
            r.x = Math.max(0, (docWidth() - w) / 2);
            r.y = 0;
            r.setCommonCode(commonCode);
            r.text = text;
            r.font = fonts.code();
            r.images = images;
            r.updateEvals(currentBlock().evalLo,
                          currentBlock().evalHi,
                          currentBlock().nEvals);
            currentBlock().clearEvals();
            r.setNShared(nShared);
            r.shared = shared;
            r.setCodeIndex(e(n+1).iLo);
            r.setCode(text.substring(e(n+1).iLo, e(n+1).iHi));

            r.isSharedDamage = isSharedDamage;

            figureRegion.addChild((Region)r);
            figureRegion.height = r.height;

            int fy = 0;
            for (Region pr = figureRegion ; pr != root ; pr = pr.parent)
               fy += pr.y;
            boolean isMouseOverFigure = mouseY >= fy && mouseY < fy + figureRegion.height;

            isInFigure = false;
            for (int nc = 0 ; nc < r.nControls() ; nc++) {
               int type = r.getControlType(nc);
               int rx = r.x + r.width + lineHeight() / 2;
               int ry = nc * lineHeight();
               int iLo = e(n+1).iLo + r.getControlLo(nc);
               int iHi = e(n+1).iLo + r.getControlHi(nc);
               String data = r.getControlData(nc);

               figureRegion.addChild((Region)(
                  type == HIGHLIGHT ? new HighlightRegion(rx, ry, iLo, iHi, data) :
                  type == BUTTON    ? new ButtonRegion   (rx, ry, iLo, iHi, data) :
                                      new SliderRegion   (rx, ry, iLo, iHi, data) ));
               if (! isMouseOverFigure)
                  figureRegion.child(nc + 1).setVisible(false);
            }
            isInFigure = true;

            y += figureRegion.height - lineHeight();
         }
      }

      else {
      }

      return y;
   }

   void adjustLine(Region lineRegion) {
      int h = 0;
      for (int n = 0 ; n < lineRegion.nChildren ; n++)
         h = Math.max(h, lineRegion.child(n).height);
      if (h == 0)
         h = lineHeight();
      lineRegion.height = h;

      if (centerFlag == -1) {
         isCentered = false;
         centerFlag = 0;
      }

      // A LINE CONSISTING ONLY OF EVALS SHOULD BE LESS HIGH.

      boolean isAllEvalText = true;
      for (int n = 0 ; n < lineRegion.nChildren && isAllEvalText ; n++) {
         Region r = lineRegion.child(n);
         if (r instanceof TextRegion && ! ((TextRegion)r).isEvalText)
            isAllEvalText = false;
      }
      if (isAllEvalText)
         for (int n = 0 ; n < lineRegion.nChildren ; n++) {
            Region r = lineRegion.child(n);
            if (r instanceof TextRegion)
               ((TextRegion)r).height = lineHeight * 3 / 4;
         }

      if (lineRegion.nChildren > 0 && isCentered) {
         int xL = lineRegion.child(0).x;
         Region last = lineRegion.child(lineRegion.nChildren - 1);
         int xR = last.x + last.width;
         int dx = docWidth() / 2 - (xL + xR) / 2;
         for (int n = 0 ; n < lineRegion.nChildren ; n++)
            lineRegion.child(n).x += dx;
      }

      if (centerFlag == 1) {
         isCentered = true;
         centerFlag = 0;
      }
   }

   int nvq = 0;
   String varq[] = new String[100];
   String valq[] = new String[100];

   void addToSetVarQueue(String var, String value) {
      varq[nvq] = var;
      valq[nvq] = value;
      nvq++;
   }

   public boolean mouseEvent(int type, int x, int y) {
      switch (type) {
      case PRESS: mouseDown = true; break;
      case RELEASE: mouseDown = false; break;
      }
      mouseX = x;
      mouseY = y;
      return super.mouseEvent(type, x, y);
   }

   TextRegion newTextRegion(int x, int y, int iLo, int iHi) {
      TextRegion textRegion = new TextRegion(x, y, iLo, iHi, null);
      if (isInLink)
         textRegion.anchorId = anchorId;
      textRegion.yShift = yShiftIndicator;
      textRegion.isEvalText = isEvalText;
      return textRegion;
   }

   boolean isParsingCode() {
      return isInCommon || isInFigure || isInEval;
   }

   int lineHeight() {
      return graphics.getFont() == fonts.code() ? lineHeight * 3 / 4 : lineHeight;
   }

   int fontHeight, lineHeight, border;
   int infoFontHeight = 15;
   Font infoFont = new Font("Courier", Font.PLAIN, infoFontHeight);
   Color[] colorStack = new Color[100];
   int colorStackTop = 0;
   { colorStack[0] = Color.black; }

   public String getText() {
      return text;
   }

   public void update(Graphics g) {
      Instrument.update();

      long previousCurrentTime = currentTime;

      currentTime = System.currentTimeMillis() - startTime;
      if (startTime == 0) {
         startTime = currentTime;
         currentTime = 0L;
      }

      elapsed = (currentTime - previousCurrentTime) / 1000.0;

      if (scroll.getScrollY() != scrollY) {
         scrollY = scroll.getScrollY();
         regionAtCursor = null;
         layout(graphics);
      }

      for (int n = 0 ; n < nrr ; n++) {
         Evals evals = rr[n].evals;
         for (int j = 0 ; j < evals.size ; j++) {
            String target = evals.targetEvals[j];
            if (target != null && ! target.equals(evals.evals[j])) {
              replaceText(evals.lo[j], evals.hi[j], target);
              evals.hi[j] = evals.lo[j] + target.length();
              evals.targetEvals[j] = null;
            }
         }
      }

      if (isDamage) {
         isDamage = false;
         parse();
         layout(graphics);
      }

      for (int n = 0 ; n < nrr ; n++) {
         Region figure = rr[n].parent;
         int yLo = figure.gy;
         int yHi = figure.gy + figure.height;
         rr[n].setOnScreen(yLo < height && yHi > 0);
         if (rr[n].isOnScreen)
            rr[n].setTime(currentTime / 1000.0);
      }

      if (! mouseDown)
         enableSetValueOnSliderPress = true;

      g.setColor(Color.white);
      g.fillRect(0, 0, docWidth(), height);
   }

   String[][] unicode = {
      { "bullet", "\u25CF", },
      { "dot", "\u2219", },
      { "empty", "\u2205", },
      { "exist", "\u2203", },
      { "forall", "\u2200", },
      { "gt", "\u003E", },
      { "infin", "\u221E", },
      { "integral", "\u222B", },
      { "isin", "\u2208", },
      { "larr", "\u2190", },
      { "le", "\u2264", },
      { "lt", "\u003C", },
      { "ne", "\u2260", },
      { "notin", "\u2209", },
      { "phi", "\u03D5", },
      { "pi", "\u03C0", },
      { "rarr", "\u2192", },
      { "sum", "\u2211", },
      { "theta", "\u03B8", },
      { "times", "\u00D7", },
   };

   Color drawingScrim = new Color(255, 255, 255, 160);

   public void overlay(Graphics g) {
      if (isWhiteboardMode) {
         g.setColor(drawingScrim);
         g.fillRect(0, 0, docWidth(), height);
         g.setColor(Color.black);
         drawing().draw(g);
         g.setFont(fonts.code());
         int h = fonts.getHeight();
         g.drawString((drawings.index+1) + "/" + drawings.size(), h / 2, h);
         return;
      }

      if (docNotes != null) {
         g.setColor(notesScrim);
         g.fillRect(0, 0, docWidth(), height);
         g.setColor(Color.black);
         g.setFont(notesFont);
         int line = 0;
         int lineHeight = notesFontHeight * 8 / 7;
         for (int i = 0 ; i < docNotes.length() ; i++) {
            int j = docNotes.indexOf('\n', i);
            g.drawString(docNotes.substring(i, j), lineHeight, lineHeight * ++line);
            i = j;
         }
         return;
      }

      // IF IN DEBUG MODE, SHOW BOUNDS OF REGION CONTAINING THE MOUSE

      if (showRegions) {
         if (regionAtCursor != null) {
            g.setColor(Color.red);
            Region r = regionAtCursor;
            g.drawRect(r.gx, r.gy, r.width-1, r.height-1);
         }
      }

/*
   The following logic should be moved into TextRegion
   so that the caret does not show up in front of the
   overlayRegion.
*/

      // ONLY DRAW CARET IF THERE IS NO SELECTION REGION

      if (selectionStart == selectionEnd) {

         // FIND TEXT INSERTION CARET
          TextRegion tr = null;
         
         for (int n = 0 ; n < nChildren ; n++)
            if ((tr = findRegionAtCaret(child(n))) != null)
               break;

         // DRAW TEXT INSERTION CARET

         if (tr != null) {
            g.setColor(Color.blue);
            g.fillRect(findCaretX(tr), tr.gy, 1, lineHeight);
         }
      }
       
        //wz
          TextRegion tr2 = null;
          if(caretEndList != null && caretEndList.length > 0){
              for(int m = 0; m < caretEndList.length; m++){
                  tr2 = null;
                  for (int n = 0 ; n < nChildren ; n++)
                      if ((tr2 = findRegions(child(n), caretEndList[m])) != null)
                          break;
                  
                  
                  if(tr2 != null){
                      if(colorList[m] != null){
                          g.setColor(colorList[m]);
                          g.fillRect(findCaretX(tr2, caretEndList[m]), tr2.gy, 1, lineHeight);
                          //print userId
                          //if(userIdList[m] !=  null){
                          //    g.drawString(userIdList[m], findCaretX(tr2, caretEndList[m]), tr2.gy + 5);
                          //}
                      }
                      else{
                          curCaretPos = m;
                      }
                  }
              }
          }
          ///wz
   }
    
    //wz
    public void updateCaretList(int[] caretStartInfo, int[] caretEndInfo, String[] colorInfo, String[] userIdInfo){
        caretStartList = null;
        caretEndList = null;
        colorList = null;
        userIdList = null;
        this.caretStartList = caretStartInfo;
        this.caretEndList = caretEndInfo;
        //this.colorList = colorInfo;
        colorList = new Color[colorInfo.length];
        for(int i = 0; i < colorInfo.length; i++){
            if(colorInfo[i] != null)
                colorList[i] = new Color(Integer.parseInt(colorInfo[i].substring(1), 16));
        }
        
        this.userIdList = userIdInfo;
    }
    
    int findCaretX(TextRegion tr, int caret) {
        int start = tr.iLo;
        int i = caret;
        if (! isTextView && tr instanceof ButtonRegion) {
            ButtonRegion br = (ButtonRegion)tr;
            start = br.getStart();
            int end = br.getEnd();
            i = Math.max(start, Math.min(end, i));
        }
        graphics.setFont(tr.font);
        return tr.gx + Parse.stringWidth(graphics, text.substring(start, i));
    }
    
    TextRegion findRegions(Region r, int caret) {
        TextRegion tr = null;
        
        for (int n = 0 ; n < r.nChildren ; n++)
            if ((tr = findRegions(r.child(n), caret)) != null)
                break;
        
        if (tr == null && r instanceof TextRegion) {
            TextRegion _tr = (TextRegion)r;
            if (_tr.iLo <= caret && _tr.iHi >= caret)
                tr = _tr;
        }
        
        return tr;
    }
    
    ///wz


   TextRegion findRegionAtCaret(Region r) {
      return findRegionAt(r, caret);
   }

   TextRegion findRegionAt(Region r, int caret) {
      TextRegion tr = null;

      for (int n = 0 ; n < r.nChildren ; n++)
         if ((tr = findRegionAt(r.child(n), caret)) != null)
            break;

      if (tr == null && r instanceof TextRegion) {
         TextRegion _tr = (TextRegion)r;
         if (_tr.iLo <= caret && _tr.iHi >= caret)
            tr = _tr;
      }

      return tr;
   }
   
   int findCaretX(TextRegion tr) {
      int start = tr.iLo;
      int i = caret;
      if (! isTextView && tr instanceof ButtonRegion) {
         ButtonRegion br = (ButtonRegion)tr;
         start = br.getStart();
         int end = br.getEnd();
         i = Math.max(start, Math.min(end, i));
      }
      graphics.setFont(tr.font);
      return tr.gx + Parse.stringWidth(graphics, text.substring(start, i));
   }

   void setFont(boolean isBold, boolean isItalic, boolean isCode) {
      graphics.setFont(isParsingCode() ? fonts.code() :
              isCode ? isBold ? isItalic ? fonts.boldItalicCode() : fonts.boldCode()
                              : isItalic ? fonts.italicCode() : fonts.code()
                     : isBold ? isItalic ? fonts.boldItalic() : fonts.bold()
                              : isItalic ? fonts.italic() : fonts.plain());
   }

   void addElement(int iLo, int iHi, int type, boolean isEnd) {
      Element el = new Element();
      el.iLo = iLo;
      el.iHi = iHi;
      el.type = type;
      el.isEnd = isEnd;
      elements.add(el);
   }

   int findTagType(String text, int i) {
      int j = text.indexOf('>', i);

      if (j >= 0) {
         String tagName = text.substring(i, j);
         int ii = tagName.indexOf(' ');
         if (ii > 0)
            tagName = tagName.substring(0, ii);

         for (int n = 0 ; n < Types.name.length ; n++)
            if (tagName.equals(Types.name[n]))
               return n;

         for (int n = 0 ; n < unicode.length ; n++)
            if (tagName.equals(unicode[n][0]))
               return CHARACTER;
      }

      return UNKNOWN;
   }

   public boolean mouseMove(int x, int y) {
      mouseX = x;
      mouseY = y;
      figureAtMouse = null;
      for (int n = 0 ; n < nrr ; n++)
         if (y >= rr[n].gy && y < rr[n].gy + rr[n].height) {
            figureAtMouse = rr[n];
            return true;
         }
      return true;
   }

   int mx;
   TextRegion tr = null;

   public boolean mousePress(int x, int y) {
      if (isWhiteboardMode) {
         drawing().startLine(x, y);
         return true;
      }
       
      int i = caretAtPosition(x, y);

      if (isVaryingNumber && (tr != null || selectionStart <= i && selectionEnd > i)) {
         mx = x;
         return true;
      }
       
      isVaryingNumber = false;
      setCaret(i);
      caretAtMousePress = caret;
      setSelection(caret, caret);
      return true;
   }

   public void mouseDrag(int x, int y) {
      if (isWhiteboardMode) {
         drawing().addPoint(x, y);
         return;
      }

      if (isVaryingNumber) {
         if (x != mx) {
            String str = tr != null
               ? text.substring(tr.iLo, tr.iHi)
               : text.substring(selectionStart, selectionEnd);

            if (x > mx + 5) {
               mx = x;
               str = Parse.increment(str);
            }
            else if (x < mx - 5) {
               mx = x;
               str = Parse.decrement(str);
            }
            else
               return;

            if (tr != null)
               replaceText(tr.iLo, tr.iHi, str);
            else {
               replaceText(selectionStart, selectionEnd, str);
               setSelection(selectionStart, selectionStart + str.length());
            }
         }
         return;
      }
      setCaretToPosition(x, y);
       
      setSelection(Math.min(caret, caretAtMousePress),
                   Math.max(caret, caretAtMousePress));
   }

   public void mouseRelease(int x, int y) {
      if (isWhiteboardMode) {
         drawing().endLine(x, y);
         return;
      }
      isVaryingNumber = false;
      tr = null;
   }

   public void mouseClick(int x, int y) {
      if (isVaryingNumber) {
         System.err.println("varying number mouse click");
         return;
      }
      if (currentTime - clickTime > 500)
         clickCount = 0;
      clickTime = currentTime;
      switch (++clickCount) {
      case 1: break;
      case 2: selectWord(); break;
      case 3: selectLine(); break;
      }
   }

   void selectWord() {
      selectionStart = Parse.findStartOfWord(text, caret);
      selectionEnd = Parse.findEndOfWord(text, caret);
      if (! isVaryingNumber) {
         String selection = text.substring(selectionStart, selectionEnd);
         isVaryingNumber = Parse.isIntValue(selection) || Parse.isDoubleValue(selection);
      }
      setSelection(selectionStart, selectionEnd);
   }

   void selectLine() {
      selectionStart = Parse.findChar(text, '\n', caret, -1);
      if (text.charAt(selectionStart) == '\n')
         selectionStart++;

      selectionEnd = Parse.findChar(text, '\n', caret, 1);
      if (selectionEnd < text.length())
         selectionEnd++;
      setSelection(selectionStart, selectionEnd);

      String selection = text.substring(selectionStart, selectionEnd);
      if (selection.indexOf("setColor") >= 0)
         showColorEditOverlay();
   }

   void setCaretToPosition(int x, int y) {
     setCaret(caretAtPosition(x, y));
   }

   void adjustForChangedTextView() {
   }

   int caretAtPosition(int x, int y) {
      Region r = findRegion(x, y);
      if (r instanceof TextRegion)
         return caretInTextRegion((TextRegion)r, x);
      else {
         for (int n = 0 ; n < nChildren ; n++) {
            r = child(n);
            if (r.nChildren>0 && r.gx<=x && r.gy<=y && r.gx+r.width > x && r.gy+r.height>y) {
                if (r.child(0) instanceof TextRegion) {
                  TextRegion tr = (TextRegion)r.child(0);

                  // POSITION CARET AT START OF LINE BEFORE ANY TEXT REGION?

                  if (x < tr.gx)
                     return caretInTextRegion(tr, tr.gx);
                  else {
                     tr = (TextRegion)r.child(r.nChildren - 1);
                     int caret = caretInTextRegion(tr, tr.gx + tr.width);

                     // POSITION CARET AT END OF LINE BEFORE ANY <...>

                     if (caret >= 1 && text.charAt(caret - 1) == '>')
                        while (caret > 0 && text.charAt(caret) != '<')
                           caret--;

                     return caret;
                  }
               }
               return caret;
            }
         }
      } 
      return caret;
   }

   void setCaretInTextRegion(TextRegion tr, int x) {
      setCaret(caretInTextRegion(tr, x));
   }

   int caretInTextRegion(TextRegion tr, int x) {
      graphics.setFont(tr.font);
      x -= tr.gx;
      int i = tr.iHi;
      for ( ; Parse.stringWidth(graphics,text.substring(tr.iLo,i)) > x + fontHeight/4 ; i--)
          ;
      return i;
   }

   public void specialKeyPress(int type) {
   }

   public void specialKeyRelease(int type) {
      if (isWhiteboardMode) {
         switch (type) {
         case LEFT:
            drawings.previousDrawing();
            break;
         case RIGHT:
            drawings.nextDrawing();
            break;
         }
         return;
      }
      switch (type) {
      case LEFT:
         setCaret(caret - 1);
         break;
      case UP:
         moveCaretLine(-1);
         break;
      case RIGHT:
         setCaret(caret + 1);
         break;
      case DOWN:
         moveCaretLine(1);
         break;
      }
   }

   public boolean keyPress(int key) {
      return true;
   }

   boolean handleControlKey(int key) {
      switch (key) {
      case '{':
      case '[':
         figureWidth = Math.max(0.3, figureWidth - 0.1);
         isDamage = true;
         return true;
      case '}':
      case ']':
         figureWidth = Math.min(0.9, figureWidth + 0.1);
         isDamage = true;
         return true;
      case '-':
         setFontHeight(fonts.getHeight() - 1);
         return true;
      case '=':
      case '+':
         setFontHeight(fonts.getHeight() + 1);
         return true;
      case 'b':
         toggleTags("b");
         return true;
      case 'c':
         copy();
         return true;
      case 'd':
         scrollBy(2 * height);
         return true;
      case 'e':
         toggleTags("eval");
         return true;
      case 'f':
         if (overlayRegion.isVisible && overlayRegion.mode == OverlayRegion.FIND) {
            overlayRegion.setVisible(false);
            return true;
         }
         overlayRegion.mode = OverlayRegion.FIND;
         overlayRegion.x = docWidth() / 2 - 200;
         overlayRegion.y = 32;
         overlayRegion.width = 400;
         overlayRegion.height = 48;
         overlayRegion.setVisible(true);
         return true;
      case 'g':
         showRegions = ! showRegions;
         return true;
      case 'i':
         toggleTags("i");
         return true;
      case 'l':
         if (selectionStart < selectionEnd) {
            int index = caretAtPosition(0, 0);
            String anchorName = generateAnchorName();
            String newText = "<link #" + anchorName + ">" +
                             text.substring(selectionStart, selectionEnd) +
                             "</link>";
            replaceText(selectionStart, selectionEnd, newText, true);
            if (index > selectionStart)
               index += newText.length() - (selectionEnd - selectionStart);
            replaceText(index, index, "<a " + anchorName + ">");
            setSelection(selectionStart, selectionStart);
            setCaret(selectionStart);
         }
         return true;
      case 'r':
         if (overlayRegion.isVisible && overlayRegion.mode == OverlayRegion.COLOR) {
            overlayRegion.setVisible(false);
            return true;
         }
         showColorEditOverlay();
         return true;
      case 't':
         indexAtTopOfScreen = caretAtPosition(0, 0);
         fonts.setTextView(isTextView = ! isTextView);
         layout(graphics);
         return true;
      case 'u':
         scrollBy(-2 * height);
         return true;
      case 'v':
         paste();
         return true;
      case 'w':
         isWhiteboardMode = ! isWhiteboardMode;
         if (isWhiteboardMode)
            drawings.index++;
         return true;
      case 'x':
         cut();
         return true;
      case 'z':
         undo();
         return true;
      }
      return false;
   }

   void showColorEditOverlay() {
      getTextRGB();
      overlayRegion.mode = OverlayRegion.COLOR;
      overlayRegion.x = docWidth() / 2 - 64;
      overlayRegion.y = height / 2 - 128;
      overlayRegion.width = 128;
      overlayRegion.height = 256;
      overlayRegion.setVisible(true);
   }

   public boolean keyRelease(int key) {
      boolean isAlt = (key & 128) != 0;
      boolean isCommand = (key & 256) != 0;
      key &= 127;

      // IGNORE DELETE
      if ( key == 127 )
         return true;

      // COMMAND AND CONTROL KEYS
       
      if ( isCommand && handleControlKey(key) ||
           key < ' ' && handleControlKey(key + ('a' - 1)) )
         return true;

      if (overlayRegion.isVisible)
         switch (overlayRegion.mode) {
         case OverlayRegion.FIND:
            String s = overlayRegion.findText;
            if (key == 10) {
               scrollToText(overlayRegion.findText);
               overlayRegion.setVisible(false);
            }
            else if (key != 8)
               s += (char)key;
            else if (s.length() > 0)
               s = s.substring(0, s.length() - 1);
            overlayRegion.findText = s;
            return true;
         }

      // INSERT A CHARACTER

      if (key != 8) {
         if (selectionStart < selectionEnd) {
            replaceText(selectionStart, selectionEnd, "" + (char)key, true);
            setSelection(selectionStart, selectionStart);
            setCaret(selectionStart + 1);
         }
         else {
            replaceText(caret, caret, "" + (char)key, true);
            setCaret(caret + 1);
         }
      }

      // DELETE A CHARACTER

      else if (selectionStart < selectionEnd || caret > 0) {
         if (selectionStart < selectionEnd) {
            replaceText(selectionStart, selectionEnd, "", true);
            setSelection(selectionStart, selectionStart);
            setCaret(selectionStart);
         }
         else {
            if (! isTextView && caret > 0 && text.charAt(caret - 1) == '>')
               caret = Math.max(1, Parse.findChar(text, '<', caret, -1));
            replaceText(caret - 1, caret, "", true);
            setCaret(caret - 1);
         }
      }

      return true;
   }

   void copy() {
      if (selectionStart < selectionEnd)
         Parse.copyToClipboard(text.substring(selectionStart, selectionEnd));
   }

   void cut() {
      if (selectionStart < selectionEnd) {
         copy();
         replaceText(selectionStart, selectionEnd, "", true);
         setSelection(selectionStart, selectionStart);
         setCaret(selectionStart);
      }
   }

   void paste() {

      // FIRST SEARCH FOR FIGURE REGIONS

      for (int n = 0 ; n < nChildren ; n++) {
         Region r = child(n);
         if ( r.nChildren > 0 && r.child(0) instanceof RenderableRegion &&
              r.gy <= mouseY && r.gy + r.height > mouseY ) {
            for (int i = 1 ; i < r.nChildren ; i++) {
               Region rc = r.child(i);
               if ( rc.gx <= mouseX && rc.gx + rc.width  > mouseX &&
                    rc.gy <= mouseY && rc.gy + rc.height > mouseY ) {

                  RenderableRegion rr = (RenderableRegion)r.child(0);
                  String code = rr.getControlCode(i-1);
                  int j = Parse.nextWord(code, 0);
                  code = code.substring(0,j) + r.getName() + "." + code.substring(j,code.length());
                  replaceText(caret, caret, code);
                  return;
               }
            }
         }
      }

      String selection = Parse.getFromClipboard();
      if (selectionStart < selectionEnd) {
         replaceText(selectionStart, selectionEnd, selection, true);
         setSelection(selectionStart, selectionStart);
         setCaret(selectionStart + selection.length());
      }
      else {
         replaceText(caret, caret, selection, true);
         setCaret(caret + selection.length());
      }
   }

   void setVar(String figName, String var, String value) {
      for (int n = 0 ; n < nrr ; n++) {
         RenderableRegion r = rr[n];
         Region figure = r.parent;
         if (figure.getName().equals(figName)) {
            if (r.isAssigned(var)) {
               int iLo = r.assignedValueStart(var);
               int iHi = r.assignedValueEnd(var);
               int codeIndex = r.getCodeIndex();
               replaceText(codeIndex + iLo, codeIndex + iHi, value, true);
               return;
            }
            for (int i = 0 ; i < r.nControls() ; i++)
               if (r.getControlData(i).indexOf(var) == 0) {
                  ((TextRegion)figure.child(i + 1)).setTagValue(value);
                  return;
               }
            return;
         }
      }
   }

   class Change {
      int index, caret, selectionStart, selectionEnd, scrollY;
      String oldText, newText;
      boolean markForUndo;
   }

   int changeIndex = 0;
   ArrayList changes = new ArrayList();

   void recordChangeForUndo(int index, String oldText, String newText, boolean markForUndo) {
      if (changeIndex == changes.size())
         changes.add(new Change());
      Change change = change(changeIndex++);
      change.index = index;
      change.caret = caret;
      change.selectionStart = selectionStart;
      change.selectionEnd = selectionEnd;
      change.oldText = oldText;
      change.newText = newText;
      change.scrollY = scroll.getScrollY();
      change.markForUndo = markForUndo;
   }

   void undo() {
      while (changeIndex > 0) {
         isDamage = true;
         Change change = change(--changeIndex);
         int lo = change.index;
         int hi = lo + change.newText.length();
         text = text.substring(0, lo) +
                change.oldText +
                text.substring(hi, text.length());
         caret = change.caret;
         selectionStart = change.selectionStart;
         selectionEnd = change.selectionEnd;
         scroll.setScrollY(change.scrollY);
         if (change.markForUndo)
            break;
      }
   }

   Change change() { return change(changeIndex); }

   Change change(int i) { return (Change)changes.get(i); }

   void replaceText(int lo, int hi, String src) {
      replaceText(lo, hi, src, false);
   }

   void replaceText(int lo, int hi, String src, boolean markForUndo) {
      String oldText = text.substring(lo, hi);
      recordChangeForUndo(lo, oldText, src, markForUndo);
      text = text.substring(0, lo) + src + text.substring(hi, text.length());
      isDamage = true;
   }

   void setCaret(int i) {

      int prevCaret = caret;

      // CARET MUST BE BETWEEN 0 AND LENGTH OF TEXT.

      caret = Math.max(0, Math.min(text.length(), i));

      if (isTextView)
         return;

      // WHEN CARET MOVES BACK TO A '>', SKIP BACK TO BEFORE PRECEDING '<'.

      while (prevCaret == caret + 1 && text.charAt(caret) == '>') {
         while (caret > 0 && text.charAt(caret) != '<')
            caret--;
         prevCaret = caret;
         if (caret > 0)
            caret--;
      }

      // WHEN CARET MOVES FORWARD PAST A '<', SKIP FORWARD TO AFTER NEXT '>'.

      while (prevCaret == caret - 1 && caret < text.length() && text.charAt(caret - 1) == '<') {
         while (caret < text.length() && text.charAt(caret - 1) != '>')
            caret++;
         prevCaret = caret;
         if (caret < text.length())
            caret++;
      }
   }

   void setSelection(int start, int end) {
      selectionStart = start;
      selectionEnd = end;

      // WHEN NOT IN TEXT VIEW, ADJUST FOR START AND END TAGS

      if (! isTextView && ! isVaryingNumber) {
         selectionStart = Parse.moveToStartOfStartTag(text, selectionStart);
         selectionStart = Parse.moveToEndOfEndTag(text, selectionStart);
         selectionEnd = Parse.moveToStartOfStartTag(text, selectionEnd);
         selectionEnd = Parse.moveToEndOfEndTag(text, selectionEnd);
      }
   }

   void toggleTags(String tagName) {
      String startTag = "<" + tagName + ">";
      String endTag = "</" + tagName + ">";
      String selectedText = text.substring(selectionStart, selectionEnd);

      int a = startTag.length();
      int b = endTag.length();
      int c = selectedText.length();

      if (c >= a + b && selectedText.substring(0, a).equals(startTag) &&
                        selectedText.substring(c - b, c).equals(endTag)) {

         // REMOVE SURROUNDING TAGS

         replaceText(selectionStart, selectionEnd, selectedText.substring(a, c - b));
         setSelection(selectionStart, selectionEnd - a - b);
      }
      else {

         // ADD SURROUNDING TAGS

         replaceText(selectionStart, selectionEnd, startTag + selectedText + endTag);
         setSelection(selectionStart, selectionEnd + a + b);
      }
   }

   void moveCaretLine(int sign) {
      TextRegion tr = findRegionAtCaret(root);
      if (tr != null)
         setCaretToPosition(findCaretX(tr), tr.gy + sign * lineHeight);
   }

   public boolean getIsTextView() {
      return isTextView;
   }

   void setVar(String var, String value) {
      int n = var.indexOf('.');
      if (n >= 0)
        setVar(var.substring(0, n), var.substring(n + 1, var.length()), value);
   }

   int rb2g(int r, int b) { return (2 * r + b) / 3; }

   Region root = this, regionAtCursor = root;
   Color selectionColor = new Color(255, 0, 0, 32);
   Fonts fonts = new Fonts();
   Graphics graphics;
   boolean mouseDown = false, isTextView = false;
   int caretAtMousePress;
   int mouseX, mouseY;
   ScrollRegion scroll = new ScrollRegion();
   int boldLevel = 0, italicLevel = 0, codeLevel = 0;
   int yShiftIndicator = 0;
   Component component;
   int anchorId = -1;
   boolean isInEval, isInFigure, isInCommon, isInComment, isInLink, isInNotes;
   boolean enableSetValueOnSliderPress = true;
   int clickCount = 0;
   long startTime, currentTime, clickTime;
   double elapsed;
   Color tagColor = new Color(0, 0, 192);
   double figureWidth = 0.5;
   String commonCode = "";
   RenderableRegion figureAtMouse;
   Images images = new Images();
   int scrollY = 0;
   String browser = "/Applications/Safari.app";
   int overlayRegionDy = 0;
   int nLayouts = 0;
   boolean isInBrowser = false;
   boolean isSharedDamage = false;
   int nShared = 0;
   int sharedLo[] = new int[100];
   int sharedHi[] = new int[100];
   String shared[] = new String[100];
   boolean isWhiteboardMode = false;
   boolean isEvalText = false;
   String docNotes = null;

   // FIELDS THAT MIGHT REMAIN SYNCHRONIZED ACROSS VIEWS OF THE SAME USER.

   int indexAtTopOfScreen = -1;

   // FIELDS THAT MUST REMAIN SYNCHRONIZED ACROSS VIEWS OF THE SAME USER.

   boolean isSynchronizingScroll = false;
   String text = "";
   boolean isVaryingNumber;
   int caret = 0, selectionStart = 0, selectionEnd = 0;

   class Point {
      Point(int x, int y) {
         this.x = x;
         this.y = y;
      }
      int x, y;
   }

   class Line extends ArrayList {
   }

   class Drawing extends ArrayList {
      void startLine(int x, int y) {
         line = new Line();
         add(line);
      }
      void addPoint(int x, int y) {
         line.add(new Point(x, y + scroll.getScrollY()));
      }
      void endLine(int x, int y) {
      }
      void draw(Graphics g) {
         g.setColor(Color.black);
         for (int n = 0 ; n < size() ; n++) {
            Line line = (Line)get(n);
            for (int i = 0 ; i < line.size() - 1 ; i++) {
               Point p0 = (Point)line.get(i);
               Point p1 = (Point)line.get(i + 1);
               int dy = scroll.getScrollY();
               for (int u = -1 ; u <= 1 ; u++)
               for (int v = -1 ; v <= 1 ; v++)
                  g.drawLine(p0.x + u, p0.y - dy + v, p1.x + u, p1.y - dy + v);
            }
         }
      }
      Line line;
   }

   Drawing drawing() {
      return drawings.drawing();
   }

   // NEED TO CREATE AN ARRAY OF DRAWINGS
   // THAT WE CAN TRAVERSE VIA LEFT/RIGHT ARROW KEYS
   // WHEN WE ARE IN DRAWING MODE.

   // ALSO, SHOULD BE ABLE TO SHOW SMALL SNAPSHOTS OF HISTORY OF DRAWINGS.

   // ALSO, DRAWINGS SHOULD BE SAVED SO THEY ARE AVAILABLE AT NEXT SESSION.

   // ALSO, SHOULD BE ABLE TO CLEAR A DRAWING.

   class Drawings extends ArrayList {
      Drawing drawing() {
         return drawing(index);
      }

      Drawing drawing(int n) {
         while (n >= size())
            add(new Drawing());
         return (Drawing)get(n);
      }

      Drawing nextDrawing() {
         if (index == size() - 1 && drawing().size() == 0)
            return drawing();
         return drawing(++index);
      }

      Drawing previousDrawing() {
         index = Math.max(0, index - 1);
         return drawing(index);
      }
      int index = -1;
   }
   Drawings drawings = new Drawings();
    
    
    //wz
    int listSize = 0;
    int curCaretPos = -1;
    int[] caretStartList;
    int[] caretEndList;
    Color[] colorList;
    String[] userIdList;
    Color[] selectionColorList;
    ///wz
}

