
package test;

import bsh.Interpreter;
import java.util.*;
import java.awt.*;
import render.*;

public class RenderedRegion extends RenderableRegion implements Runnable
{
   interface RenderedCallbacks {
      void setup();
      void draw();
      void update();
      void overlay();
   }

   public void setVerbose(boolean state) {
      isVerbose = state;
   }

   synchronized public void setCode(String code) {
      if ( ! code.equals(this.code) ||
           isCommonCodeDamage ||
           isSharedDamage ||
           evals.isDamage) {
         interpreter = new Interpreter();

         parseControls(code);

         if (render == null) {
             render = new Renderer();
             renderRgb = render.init(width, height);
         }

         eval(
              "import java.awt.Color;" +
              "import java.lang.String;" +
              "import util.*;" +
              "import draw.*;" +
	      "import render.*;" +
	      "import test.*;" +
	      "width=" + width + ";" +
	      "height=" + height + ";" +
	      "time=" + time + ";"
         );

         try {
            interpreter.set("images", images);
            interpreter.set("evals", evals);
            interpreter.set("draw", draw);
            interpreter.set("render", render);
         } catch (Exception e) { }

         eval(commonCode + code.substring(iStart, code.length()));

         for (int n = 0 ; n < nShared ; n++)
            eval(shared[n]);

         for (int n = 0 ; n < evals.size ; n++)
            eval(evals.evals[n]);

         try {
            callbacks = (RenderedCallbacks)interpreter.getInterface(RenderedCallbacks.class);

            vars.clear();
            for (int n = 0 ; n < nControls ; n++)
	       if (controlType[n] == Types.BUTTON || controlType[n] == Types.HIGHLIGHT) {

	          String tags[][] = Parse.parseTags(controlData[n]);
	          double duration = 1.0;
	          Var var = null;

		  for (int nt = 0 ; nt < tags.length ; nt++) {
		     String name  = tags[nt][0];
		     String value = tags[nt][1];

		     if (name.equals("duration"))
		        duration = Parse.doubleValue(value);

                     else if (value.length() > 0) {

		        var = new Var();
		        var.name = name;
		        var.type = controlType[n];

	                switch (var.type) {
	                case Types.BUTTON:
		           var.value = Parse.intValue(value);
		           eval(var.name + "(n) { return " + var.name + "__signal[n]; }");
			   break;
	                case Types.HIGHLIGHT:
		           var.isTrue = Parse.booleanValue(value);
		           eval(var.name + "() { return " + var.name + "__signal[0]; }");
			   break;
                        }

		        if (signals[vars.size()] == null) {
		           signals[vars.size()] = new double[20];

			   switch (var.type) {
			   case Types.BUTTON:
			      signals[vars.size()][var.value] = 1.0;
			      break;
			   case Types.HIGHLIGHT:
			      signals[vars.size()][0] = var.isTrue ? 1.0 : 0.0;
			      break;
			   }
                        }
		        var.signal = signals[vars.size()];
			for (int j = 0 ; j < var.signal.length ; j++)
			   var.sigma[j] = sCurve(var.signal[j]);
		        interpreter.set(var.name + "__signal", var.sigma);

		        vars.add(var);
		     }
		  }

		  if (var != null)
		     var.rate = 1.0 / Math.max(0.01, duration);
	       }
         } catch (Exception e) { }

         this.code = code;

         for (int n = 0 ; n < nControls ; n++)
            eval(controlData[n]);

	 evals.isDamage = false;
	 isCommonCodeDamage = false;
	 isSharedDamage = false;

	 isFirstTime = true;
      }
   }

   double previousTime;

   public void update(Graphics g) {
      double elapsed = time - previousTime;
      previousTime = time;
      g.setFont(font);
      g.setColor(Color.black);

      if (! isOnScreen)
         return;

      for (int n = 0 ; n < vars.size() ; n++)
         var(n).update(elapsed);

      draw.setGraphics(g);
      try {
         interpreter.set("time", time);
      } catch (Exception e) { }
      try {
         callbacks.draw();
      } catch (Exception e) { }

      g.drawImage(image, 0, 0, null);

      try {
         callbacks.overlay();
      } catch (Exception e) { }
   }

   synchronized public void render(int[] pix) {
      if (isOnScreen && render != null) {
          try {
	     if (isFirstTime) {
                render.setFL(10);
                render.setFOV(1.0);
                render.setBgColor(1,1,1,0);
                render.nLights = 0;
                Geometry world = render.getWorld();
                for (int n = world.nChildren() - 1 ; n >= 0 ; n--)
                   world.delete(n);
                callbacks.setup();
                isFirstTime = false;
             }

             callbacks.update();

          } catch (Exception e) { }

          render.refresh();
          render.render();
          System.arraycopy(renderRgb, 0, pix, 0, renderRgb.length);
      }
   }

   public boolean keyPress(int key) {
      if (key == 'p' + 256)
	 return true;
      return eval("keyPress(" + key + ")");
   }

   public boolean keyRelease(int key) {
      if (key == 'p' + 256) {
         System.err.println("creating print.stl file");
	 STL.saveSTL(render.getWorld(), "print");
	 return true;
      }
      return eval("keyRelease(" + key + ")");
   }

   public boolean mouseMove(int x, int y) {
      return eval("mouseMove(" + x + "," + y + ")");
   }

   public boolean mousePress(int x, int y) {
      eval("mousePress(" + x + "," + y + ")");
      return true;
   }

   public void mouseDrag(int x, int y) {
      eval("mouseDrag(" + x + "," + y + ")");
   }

   public void mouseRelease(int x, int y) {
      eval("mouseRelease(" + x + "," + y + ")");
   }

   boolean eval(String cmd) {
      if (interpreter != null)
         try {
            interpreter.eval(cmd);
         } catch (Exception e) {
	    if (isVerbose)
	       System.err.println("eval exception " + e);
	    return false;
	 }
      return true;
   }

   public int nControls() { return nControls; }
   public int getControlLo(int n) { return controlLo[n]; }
   public int getControlHi(int n) { return controlHi[n]; }
   public int getControlType(int n) { return controlType[n]; }
   public String getControlCode(int n) { return controlCode[n]; }
   public String getControlData(int n) { return controlData[n]; }
   public String getControlLabel(int n) { return controlLabel[n]; }

   int nControls;

   int controlLo[] = new int[100];
   int controlHi[] = new int[100];
   int controlType[] = new int[100];
   String controlCode[] = new String[100];
   String controlData[] = new String[100];
   String controlLabel[] = new String[100];

   String controlTypeNames[] = {"highlight","button","slider"};
   int controlTypes[] = { Types.HIGHLIGHT, Types.BUTTON, Types.SLIDER };

   void parseControls(String code) {
      nControls = 0;
      iStart = 0;
      int i = 0;
      for ( ; (i = code.indexOf('<', i)) >= 0 ; i++) {
         int j = code.indexOf('>', i);
         if (j == -1)
            break;
         String tag = code.substring(i+1, j);
         for (int n = 0 ; n < controlTypes.length ; n++) {
            String typeName = controlTypeNames[n];
            if (tag.indexOf(typeName) == 0) {
               int ii = Parse.nextWord(code, i);
               if (ii < j) {
                  int k = code.indexOf('<', j);
                  if (k >= 0) {
                     int kk = code.indexOf('>', k);
                     if (kk > k && typeName.equals(code.substring(k+2,kk))) {
                        controlCode[nControls] = code.substring(i, kk + 1);
                        controlLo[nControls] = j + 1;
                        controlHi[nControls] = k;
                        controlType[nControls] = controlTypes[n];
                        controlData[nControls] = code.substring(ii, j);
                        controlLabel[nControls] = code.substring(j + 1, k);
                        nControls++;
                        j = kk;
                        iStart = kk + 1;
                        break;
                     }
                  }
               }
            }
         }
         i = j;
      }
   }

   int nBlocks = 0;
   int blockLo[] = new int[100];
   int blockHi[] = new int[100];
   String blockName[] = new String[100];

   void parseBlocks(String code) {
      nBlocks = 0;
      int i = 0;
      for ( ; (i = code.indexOf("//<block", i)) >= 0 ; i++) {
         int j = code.indexOf('>', i);
         if (j == -1)
            break;
         int ii = Parse.nextWord(code, i);
	 i = code.indexOf("//</block>");
	 if (i == -1)
	    break;
	 blockName[nBlocks] = code.substring(ii, j);
         blockLo[nBlocks] = j + 1;
         blockHi[nBlocks] = i;

System.err.println("BLOCK " + blockName[nBlocks] + ":");
System.err.println(code.substring(blockLo[nBlocks], blockHi[nBlocks]));

	 nBlocks++;
      }
   }

   Renderer render;
   int[] renderRgb;
   Interpreter interpreter = null;
   RenderedCallbacks callbacks = null;
   int iStart = 0;
   boolean isVerbose;
   boolean isFirstTime;

   ArrayList vars = new ArrayList();
   Var var(int n) { return (Var)vars.get(n); }
   double signals[][] = new double[100][];

   class Var {
      double rate = 1.0;
      double[] signal;
      double[] sigma = new double[20];
      boolean isTrue;
      String name;
      int value;
      int type;

      void update(double elapsed) {
         switch (type) {
	 case Types.BUTTON:
            for (int n = 0 ; n < signal.length ; n++)
	       update(n, n == value, elapsed);
	    break;
	 case Types.HIGHLIGHT:
	    update(0, isTrue, elapsed);
	    break;
         }
      }

      void update(int n, boolean isTrue, double elapsed) {
	 if (isTrue ? signal[n] < 1.0 : signal[n] > 0.0) {
            signal[n] = isTrue ? Math.min(1.0, signal[n] + rate * elapsed)
                               : Math.max(0.0, signal[n] - rate * elapsed);
            sigma[n] = sCurve(signal[n]);
         }
      }
   }

   double sCurve(double t) { return .5 - .5 * Math.cos(t * Math.PI); }
}

