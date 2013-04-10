// the glue between a Doc and the sharing interface

package test;

import java.awt.Component;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ConcurrentDoc extends Doc implements DocSync
{

   // 
   public ConcurrentDoc(Component component)
   {
      super(component);
   }

   synchronized public boolean registerChangeWatcherClient(DocSync.ChangeWatcherClient c) {
      if (cwc == null) {
         cwc = c;
         return true;
      }

      // already registered a watcher
      return false;
   }

   synchronized public void unregisterChangeWatcherClient(DocSync.ChangeWatcherClient c) {
      if (cwc == c) {
         cwc = null;
      }
   }

   @Override
   synchronized void replaceText(int lo, int hi, String src, boolean markForUndo) {
       // TODO: markForUndo
      super.replaceText(lo, hi, src, markForUndo);

      if (cwc != null) {
         cwc.docChanged(this, lo, hi, src, text);
      }
   }

   @Override
   synchronized void setCaret(int i) {
      super.setCaret(i);

      cwc.caretMoved(this, caret, selectionStart, selectionEnd, isVaryingNumber);
   }

   @Override synchronized void setSelection(int start, int end) {
      super.setSelection(start, end);

      cwc.caretMoved(this, caret, selectionStart, selectionEnd, isVaryingNumber);
   }

   public synchronized void replaceState(String s, int caret, int selectionStart, int selectionEnd, boolean isVaryingNumber) {
      this.caret = caret;
      this.selectionStart = selectionStart;
      this.selectionEnd = selectionEnd;
      this.isVaryingNumber = isVaryingNumber;
/*
      if (isSynchronizedScroll)
         this.indexAtTopOfScreen = indexAtTopOfScreen;
*/

      if (!text.equals(s)) {
         text = s;
         isDamage = true;
         root.isDamage = true;
      }
   }

   public boolean isTextApproved() {
      // TODO: get approval for a change (to suppress upload of invalid code)
      return true;
   }
    
    //wz
    public void updateCursorList(int[] caretStartInfo, int[] caretEndInfo, String[] colorInfo, String[] userIdInfo)
    {
        updateCaretList(caretStartInfo, caretEndInfo, colorInfo, userIdInfo);
    }
    ///wz
    
   DocSync.ChangeWatcherClient cwc;
}
