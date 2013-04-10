// responsible for communicating with a Pad, and keeping several ConcurrentDoc instances synced with it


package test;

import java.net.URL;
import java.util.ArrayList;
import epl.*;

public class PadClient implements DocSync.ChangeWatcherClient
{
   public PadClient(URL baseUrl, String padName) {
      this.padName = padName;
      pad = null;
      badPad = false;

      caretMarker = -1;
      selectionStartMarker = -1;
      selectionEndMarker = -1;

      url = baseUrl;

      docs = new ArrayList<DocSync>();
   }

   public synchronized void registerDoc(DocSync d) throws Exception {
      if (!docs.contains(d)) {
         if (!d.registerChangeWatcherClient(this)) {
            throw new Exception("failed to register as ChangeWatcherClient with a DocSync");
         }
         docs.add(d);

         if (text != null) {
            d.replaceState(text, caret, selectionStart, selectionEnd, isVaryingNumber);
         }
      }
   }

   public synchronized void unregisterDoc(DocSync d) {
      docs.remove(d);
      d.unregisterChangeWatcherClient(this);
   }

   public synchronized void docChanged(DocSync changed, int lo, int hi, String src, String text) {
      this.text = text;

      // handle replacement via the pad interface
      if (pad != null) {
         try {
            pad.makeChange(lo, hi-lo, src);
         } catch (PadException e) {
            pad.logThrowableToServer(e);
            badPad = true;
            return;
         }
      }

      // broadcast to other docs
      for (DocSync d : docs) {
         if (d != changed) {
            d.replaceState(this.text, caret, selectionStart, selectionEnd, isVaryingNumber);
         }
      }
   }

   public synchronized void caretMoved(DocSync changed, int caret, int selectionStart, int selectionEnd, boolean isVaryingNumber) {
      this.caret = caret;
      this.selectionStart = selectionStart;
      this.selectionEnd = selectionEnd;
      this.isVaryingNumber = isVaryingNumber;
       //wz
       if (pad != null) {
           try {
               if(selectionStart == selectionEnd)
                   pad.broadcastCursor(caret, caret);
               else
                   pad.broadcastCursor(selectionStart, selectionEnd+1);
           } catch (PadException e) {
               pad.logThrowableToServer(e);
               badPad = true;
               return;
           }
       }
       ///wz

      for (DocSync d : docs) {
         if (d != changed) {
            d.replaceState(text, caret, selectionStart, selectionEnd, isVaryingNumber);
         }
      }
   }

   int registerMarker(int markerIdx, int loc, boolean before, boolean valid) {
      if (pad == null) {
         return -1;
      }

      // we keep using the same index as Pad isn't smart enough to throw them
      // out yet
      if (markerIdx != -1) {
         pad.reRegisterMarker(markerIdx, loc, before, valid);
         return markerIdx;
      } else {
         return pad.registerMarker(loc, before, valid);
      }
   }

   void registerCaretMarkers() {
      boolean selectionOK = (selectionStart < selectionEnd);
      caretMarker = registerMarker(caretMarker, caret, true, !selectionOK);
      selectionStartMarker = registerMarker(selectionStartMarker, selectionStart, true, selectionOK);
      selectionEndMarker = registerMarker(selectionEndMarker, selectionEnd-1, false, selectionOK);
   }

   int getMarkerPos(TextState ts, int idx) {
      if (idx < 0 || idx >= ts.client_markers.length) {
         return -1;
      }

      Marker m = ts.client_markers[idx];
      if (m.valid) {
         return m.pos;
      } else {
         return -1;
      }
   }

   void adjustCaretByMarkers(TextState ts) {
      int new_caret     = getMarkerPos(ts, caretMarker);
      int new_start     = getMarkerPos(ts, selectionStartMarker);
      int new_end       = getMarkerPos(ts, selectionEndMarker);

      if (new_caret != -1) {
         caret = new_caret;
         new_start = caret;
         new_end = caret;
      } else if (new_start != -1 && new_end != -1) {
         caret = new_start;
         selectionStart = new_start;
         selectionEnd = new_end+1;
      } else {
         if (selectionStart < selectionEnd) {
            caret = ts.client_markers[selectionStartMarker].pos;
            selectionStart = caret;
            selectionEnd = caret;
         } else {
            caret = ts.client_markers[caretMarker].pos;
         }
      }
   }

   public synchronized boolean getNews() {
      boolean news = false;

      if (pad == null && !badPad) {
         // try connect
         pad = new Pad(url, padName);
      }

      try {
         registerCaretMarkers();

          //wz
          /*
         if (!badPad && pad.update(true, true)) {
            TextState ts = pad.getState();
            adjustCaretByMarkers(ts);
            
            // broadcast
            text = ts.client_text;

            for (DocSync d : docs) {
               d.replaceState(text, caret, selectionStart, selectionEnd, isVaryingNumber);
            }

            news = true;
         }
           */
          
          if(!badPad){
              getClientInfo();
              
              if(listSize > 0){
                  for (DocSync d : docs) {
                        d.updateCursorList(caretStartInfo, caretEndInfo, colorInfo, userIdInfo);
                  }
              }
          }
          
          if (!badPad && pad.update(true, true)) {
              TextState ts = pad.getState();
              adjustCaretByMarkers(ts);
              // broadcast
              text = ts.client_text;
              
              for (DocSync d : docs) {
                  //d.updateCaretList(caretStartInfo, caretEndInfo);
                  d.replaceState(text, caret, selectionStart, selectionEnd, isVaryingNumber);
              }
              
              news = true;
          }
          
          ///wz
          
          
      } catch (PadException e) {
         pad.logThrowableToServer(e);
         badPad = true;
      }

      return news;
   }

   public String getConnectionStatusString() {
      if (pad != null) {
         // connection indicator

         if (badPad) {
            return "bad connection";
         } else if (!pad.isConnected()) {
            if (pad.isConnecting()) {
               return "connecting...";
            } else {
               return "not connected";
            }
         } else {
            //return "connected";
         }

         StringBuilder sb = new StringBuilder("connected");

         if (pad.isReadOnly()) {
            sb.append(", read-only");
         }

         if (pad.isAwaitingAck()) {
            sb.append(", waiting on server ack");
         }

         if (pad.isSendPending()) {
            sb.append(", local changes queued");
         }

         return sb.toString();
      }

      return "no connection";
   }

   public boolean isPadReadOnly() {
      if (pad == null) {
         return true;
      }

      return pad.isReadOnly();
   }

   public String getReadOnlyPadId() {
      if (pad == null) {
         return null;
      }

      return pad.getReadOnlyId();
   }

   Pad pad;
   String padName;
   URL url;
   boolean badPad;

   ArrayList<DocSync> docs;

   // the authoritative state (last we heard)
   String text;
   int caret, selectionStart, selectionEnd;

   int caretMarker;
   int selectionStartMarker, selectionEndMarker;
   boolean isVaryingNumber;
    
    //wz
    int[] caretStartInfo;
    int[] caretEndInfo;
    String[] colorInfo;
    String[] userIdInfo;
    int listSize = 0;
    public void getClientInfo(){
        listSize = pad.getCursors().length;
        if(listSize > 0){
            caretStartInfo = new int[listSize];
            caretEndInfo = new int[listSize];
            colorInfo = new String[listSize];
            userIdInfo = new String[listSize];
            Avatar[] avatar = pad.getCursors();
            for(int i = 0; i < listSize; i++){
                caretStartInfo[i] = avatar[i].getStartMarker().pos;
                caretEndInfo[i] = avatar[i].getEndMarker().pos;
                colorInfo[i] = avatar[i].getColor();
                userIdInfo[i] = avatar[i].getUserId();
            }
        }
    }
    ///wz
}
