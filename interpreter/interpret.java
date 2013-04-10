
package interpreter;

import render.*;
import bsh.Interpreter;
import java.io.*;

public class interpret extends RenderApplet implements Runnable
{
   FileIsModified fileIsModified = new FileIsModified();
   Thread thread = new Thread(this);
   Object lock = new Object();
   Interpreter interpreter;
   String className;

   public void run() {
      while (true) {
	 synchronized (lock) {
            if (fileIsModified.isModified(new File(className + ".java")))
	       load(className);
	 }
         try { Thread.sleep(100); } catch(Exception e) { }
      }
   }

   void load(String className) {
      try {
         interpreter = new Interpreter();
         String code = Util.load(className + ".java")
                           .replaceFirst(" extends RenderApplet", "")
                           .replaceFirst("[{]", "{Renderer render;");
         interpreter.eval(code);
         interpreter.eval("instance = new " + className + "()");
         interpreter.set("instance.render", getRenderer());
	 Geometry world = getRenderer().getWorld();
	 for (int n = world.nChildren() - 1 ; n >= 0 ; n--)
	    world.delete(n);
	 getRenderer().nLights = 0;
	 getRenderer().refresh();
         interpreter.eval("instance.initialize()");
      } catch (Exception e) { System.err.println(e); }
   }

   public void initialize() {
      className = getParameter("className");
      load(className);
      thread.start();
   }

   public void animate(double time) {
      try {
         synchronized(lock) {
            interpreter.eval("instance.animate(" + time + ")");
         }
      } catch (Exception e) { System.err.println(e); }
   }
}

