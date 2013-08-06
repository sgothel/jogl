package jogamp.opengl.util.av;

import java.util.Arrays;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import com.jogamp.opengl.util.av.AudioSink;

/***
 * JavaSound Audio Sink
 * <p>
 * FIXME: Parameterize .. all configs .. best via an init-method, passing requested
 * audio capabilities
 * </p>
 */
public class JavaSoundAudioSink implements AudioSink {

    // Chunk of audio processed at one time
    public static final int BUFFER_SIZE = 1000;
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    private static final boolean staticAvailable;
    
    // Sample time values
    // public static final double SAMPLE_TIME_IN_SECS = 1.0 / DEFAULT_SAMPLE_RATE;
    // public static final double BUFFER_TIME_IN_SECS = SAMPLE_TIME_IN_SECS * SAMPLES_PER_BUFFER;
    
    private javax.sound.sampled.AudioFormat format;
    private DataLine.Info info;
    private SourceDataLine auline;
    private int bufferCount;
    private byte [] sampleData = new byte[BUFFER_SIZE];  
    private boolean initialized = false;
    private AudioDataFormat chosenFormat = null;
    
    static {
        boolean ok = false;
        try {
            AudioSystem.getAudioFileTypes();
            ok = true;
        } catch (Throwable t) {
            
        }
        staticAvailable=ok;
    }   
    
    @Override
    public String toString() {
        return "JavaSoundSink[init "+initialized+", dataLine "+info+", source "+auline+", bufferCount "+bufferCount+
               ", chosen "+chosenFormat+", jsFormat "+format;
    }
    
    @Override
    public AudioDataFormat getPreferredFormat() {
        return DefaultFormat;
    }
    
    @Override
    public AudioDataFormat initSink(AudioDataFormat requestedFormat, int bufferCount) {
        if( !staticAvailable ) {
            return null;
        }
        // Create the audio format we wish to use
        format = new javax.sound.sampled.AudioFormat(requestedFormat.sampleRate, requestedFormat.sampleSize, requestedFormat.channelCount, requestedFormat.signed, !requestedFormat.littleEndian);

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
            initialized=true;
            chosenFormat = requestedFormat;
        } catch (Exception e) {
            initialized=false;
        }
        return chosenFormat;
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }

    @Override
    public void destroy() {
        initialized = false;
        chosenFormat = null;
        // FIXEM: complete code!
    }
    
    public void writeData(AudioFrame audioFrame) {
        int data_size = audioFrame.dataSize;
        final byte[] lala = new byte[data_size];
        final int p = audioFrame.data.position();
        audioFrame.data.get(lala, 0, data_size);
        audioFrame.data.position(p);
        
        int written = 0;
        int len;
        while (data_size > 0) {
            // Nope: We don't make compromises for this crappy API !
            len = auline.write(lala, written, data_size);
            data_size -= len;
            written += len;
        }        
    }

    @Override
    public int getQueuedByteCount() {
        return auline.available();
    }
    
    @Override
    public int getQueuedTime() {
        return 0; // FIXME
    }

    
    @Override
    public int getWritableBufferCount() {
        return 1;
    }
    
    public boolean isDataAvailable(int data_size) {
        return auline.available()>=data_size;
    }

}
