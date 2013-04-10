package test;

public interface DocSync {

   static public class DocState {
      int changeType;
      String text;
      int caret;
      int selectionStart;
      int selectionEnd;
      String newSelection;
      boolean isVaryingNumber;
   }

   public interface ChangeWatcherClient {
      void docChanged(DocSync d, int lo, int hi, String src, String text);
      void caretMoved(DocSync d, int caret,
                                 int selectionStart,
				 int selectionEnd,
				 boolean isVaryingNumber);
/* TO IMPLEMENT:
      void docChanged(DocSync d, DocState s);
*/
   }

   public boolean registerChangeWatcherClient(ChangeWatcherClient c);
   public void unregisterChangeWatcherClient(ChangeWatcherClient c);

   // replace the doc's local state
   public void replaceState(String s, int caret,
                                      int selectionStart,
				      int selectionEnd,
				      boolean isVaryingNumber);

   public boolean isTextApproved();
    
    //wz
    public void updateCursorList(int[] caretStartInfo, int[] caretEndInfo, String[] colorInfo, String[] userIdInfo);
    ///wz
}
