package jogamp.opengl.util.av;

import java.nio.ByteBuffer;
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
    private final byte [] sampleData = new byte[BUFFER_SIZE];
    private boolean initialized = false;
    private AudioSink.AudioFormat chosenFormat = null;

    private volatile boolean playRequested = false;
    private float volume = 1.0f;

    static {
        boolean ok = false;
        try {
            AudioSystem.getAudioFileTypes();
            ok = true;
        } catch (final Throwable t) {

        }
        staticAvailable=ok;
    }

    @Override
    public String toString() {
        return "JavaSoundSink[init "+initialized+", dataLine "+info+", source "+auline+", bufferCount "+bufferCount+
               ", chosen "+chosenFormat+", jsFormat "+format;
    }

    @Override
    public final float getPlaySpeed() { return 1.0f; } // FIXME

    @Override
    public final boolean setPlaySpeed(final float rate) {
        return false; // FIXME
    }

    @Override
    public final float getVolume() {
        // FIXME
        return volume;
    }

    @Override
    public final boolean setVolume(final float v) {
        // FIXME
        volume = v;
        return true;
    }

    @Override
    public AudioSink.AudioFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public final int getMaxSupportedChannels() {
        return 2;
    }

    @Override
    public final boolean isSupported(final AudioSink.AudioFormat format) {
        return true;
    }

    @Override
    public boolean init(final AudioSink.AudioFormat requestedFormat, final float frameDuration, final int initialQueueSize, final int queueGrowAmount, final int queueLimit) {
        if( !staticAvailable ) {
            return false;
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
        } catch (final Exception e) {
            initialized=false;
        }
        return true;
    }

    @Override
    public final AudioFormat getChosenFormat() {
        return chosenFormat;
    }

    @Override
    public boolean isPlaying() {
        return playRequested && auline.isRunning();
    }

    @Override
    public void play() {
        if( null != auline ) {
            playRequested = true;
            playImpl();
        }
    }
    private void playImpl() {
        if( playRequested && !auline.isRunning() ) {
            auline.start();
        }
    }

    @Override
    public void pause() {
        if( null != auline ) {
            playRequested = false;
            auline.stop();
        }
    }

    @Override
    public void flush() {
        if( null != auline ) {
            playRequested = false;
            auline.stop();
            auline.flush();
        }
    }

    @Override
    public final int getEnqueuedFrameCount() {
        return 0; // FIXME
    }

    @Override
    public int getFrameCount() {
        return 1;
    }

    @Override
    public int getQueuedFrameCount() {
        return 0;
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

    @Override
    public AudioFrame enqueueData(final int pts, final ByteBuffer byteBuffer, final int byteCount) {
        final byte[] bytes = new byte[byteCount];
        final int p = byteBuffer.position();
        byteBuffer.get(bytes, 0, byteCount);
        byteBuffer.position(p);

        int written = 0;
        int len;
        int bytesLeft = byteCount;
        while (bytesLeft > 0) {
            len = auline.write(bytes, written, byteCount);
            bytesLeft -= len;
            written += len;
        }
        playImpl();
        return new AudioDataFrame(pts, chosenFormat.getBytesDuration(byteCount), byteBuffer, byteCount);
    }

    @Override
    public int getQueuedByteCount() {
        return auline.getBufferSize() - auline.available();
    }

    @Override
    public int getFreeFrameCount() {
        return auline.available();
    }

    @Override
    public int getQueuedTime() {
        return getQueuedTimeImpl( getQueuedByteCount() );
    }
    private final int getQueuedTimeImpl(final int byteCount) {
        final int bytesPerSample = chosenFormat.sampleSize >>> 3; // /8
        return byteCount / ( chosenFormat.channelCount * bytesPerSample * ( chosenFormat.sampleRate / 1000 ) );
    }

    @Override
    public final int getPTS() { return 0; } // FIXME
}
