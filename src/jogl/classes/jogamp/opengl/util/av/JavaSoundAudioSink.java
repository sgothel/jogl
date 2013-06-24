package jogamp.opengl.util.av;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

public class JavaSoundAudioSink implements AudioSink {

    // AudioFormat parameters
    public  static final int     SAMPLE_RATE = 44100;
    private static final int     SAMPLE_SIZE = 16;
    private static final int     CHANNELS = 2;
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;

    // Chunk of audio processed at one time
    public static final int BUFFER_SIZE = 1000;
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    
    // Sample time values
    public static final double SAMPLE_TIME_IN_SECS = 1.0 / SAMPLE_RATE;
    public static final double BUFFER_TIME_IN_SECS = SAMPLE_TIME_IN_SECS * SAMPLES_PER_BUFFER;
    
    private static AudioFormat format;
    private static DataLine.Info info;
    private static SourceDataLine auline;
    private static int bufferCount;
    private static byte [] sampleData = new byte[BUFFER_SIZE];  
    
    private static boolean available;
    
    static {
     // Create the audio format we wish to use
        format = new AudioFormat(SAMPLE_RATE, SAMPLE_SIZE, CHANNELS, SIGNED, BIG_ENDIAN);

        // Create dataline info object describing line format
        info = new DataLine.Info(SourceDataLine.class, format);

        // Clear buffer initially
        Arrays.fill(sampleData, (byte) 0);
        try{
            // Get line to write data to
            auline = (SourceDataLine) AudioSystem.getLine(info);
            auline.open(format);
            auline.start();
            System.out.println("JavaSound audio sink");
            available=true;
        } catch (Exception e) {
            available=false;
        }
    }   
    
    public void writeData(byte[] sampleData, int data_size) {
         int written = 0;
         int len;
         while (data_size > 0) {
            len = auline.write(sampleData, written, data_size);
            data_size -= len;
            written += len;
        }        
    }

    public int getDataAvailable() {
        return auline.available();
    }
    
    public boolean isDataAvailable(int data_size) {
        return auline.available()>=data_size;
    }

    @Override
    public boolean isAudioSinkAvailable() {
        return available;
    }

}
