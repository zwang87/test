
package storage;

public class StorageThread extends Thread
{
   public StorageThread() {
      super();
      if (isExitAction()) {
         Runtime.getRuntime().addShutdownHook(
            new Thread() { public void run() { onExitAction(); } }
         );
      }
   }

   public void run() {
      initialize();
      while (true) {
         update();
         try { sleep(200); } catch (Exception e) { }
      }
   }

   public void initialize() { }
   public boolean isExitAction() { return false; }
   public void onExitAction() { }
   public void update() { }
}

