package jogamp.opengl.openal.av;

import jogamp.opengl.util.av.impl.FFMPEGMediaPlayer;

import com.jogamp.openal.AL;
import com.jogamp.openal.JoalVersion;

/** 
 * Demo JOAL usage w/ av dependency, i.e. FFMPEGMediaPlayer ..
 */
public class ALDummyUsage {
    static AL al;
    static FFMPEGMediaPlayer.PixelFormat pfmt;
    
    public static void main(String args[]) {
        System.err.println("JOGL> Hello JOAL");
        System.err.println("JOAL: "+JoalVersion.getInstance().toString());
    }
}
