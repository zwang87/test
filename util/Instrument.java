
package util;

public class Instrument
{
   class Note {
      int n;
      int pitch;
      double timeOff;
   }

   int n = 0;

   public Instrument(int i) {
      this(name(i));
   }

   public Instrument(String name) {
      for (n = 0 ; n < nNames ; n++)
         if (names[n] != null && name.equals(names[n]))
	    break;
      if (n == nNames) {
         names[n] = name;
	 nNames++;
	 midiSynth.setInstrument(n, name);
      }
   }

   public void press(int pitch, double loudness) {
      press(n, pitch, loudness);
   }

   public void release(int pitch) {
      release(n, pitch);
   }

   public void play(int pitch, double loudness, double duration) {
      press(n, pitch, loudness);
      if (queue[nq] == null)
         queue[nq] = new Note();
      Note note = queue[nq++];
      note.n = n;
      note.pitch = pitch;
      note.timeOff = time + duration;
   }

   static public String name(int i) {
      return midiSynth.getInstrumentName(i);
   }

   static public void update() {
      time = (System.currentTimeMillis() - startTime) / 1000.0;
      for (int i = 0 ; i < nq ; i++) {
         Note note = queue[i];
	 if (note.timeOff < time) {
	    release(note.n, note.pitch);
	    for (int j = i ; j < nq ; j++) 
	       queue[j] = queue[j+1];
            --nq;
	 }
      }
   }

   static public void press(int n, int pitch, double loudness) {
      midiSynth.noteOn(n, pitch, (int)(64 * loudness));
   }

   static public void release(int n, int pitch) {
      midiSynth.noteOff(n, pitch, 0);
   }

   static int nq = 0;
   static long startTime = System.currentTimeMillis();
   static double time = 0;
   static Note queue[] = new Note[100];
   static int nNames = 0;
   static String names[] = new String[100];
   static MidiSynth midiSynth = new MidiSynth();
}

