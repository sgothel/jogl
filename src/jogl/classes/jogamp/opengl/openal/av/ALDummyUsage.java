package jogamp.opengl.openal.av;

import com.jogamp.openal.AL;
import com.jogamp.openal.JoalVersion;

/**
 * Demo JOAL usage w/ av dependency, i.e. FFMPEGMediaPlayer ..
 */
public class ALDummyUsage {
    static AL al;

    public static void main(final String args[]) {
        System.err.println("JOGL> Hello JOAL");
        System.err.println("JOAL: "+JoalVersion.getInstance().toString());
    }
}
