package jogamp.opengl.util.av;


import java.nio.ByteBuffer;

import com.jogamp.opengl.util.av.AudioSink;

public class NullAudioSink implements AudioSink {

    @Override
    public boolean isInitialized() {
        return true;
    }

    private volatile float playSpeed = 1.0f;
    private volatile boolean playRequested = false;
    private float volume = 1.0f;
        
    @Override
    public final float getPlaySpeed() { return playSpeed; }
    
    @Override
    public final boolean setPlaySpeed(float rate) { 
        if( Math.abs(1.0f - rate) < 0.01f ) {
            rate = 1.0f;
        }
        playSpeed = rate; 
        return true;
    }
    
    @Override
    public final float getVolume() {
        // FIXME
        return volume;        
    }
    
    @Override
    public final boolean setVolume(float v) {
        // FIXME
        volume = v;        
        return true;
    }
    
    @Override
    public AudioFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public final int getMaxSupportedChannels() {
        return 8;
    }
    
    @Override
    public final boolean isSupported(AudioFormat format) {
        return true;
    }
    
    @Override
    public boolean init(AudioFormat requestedFormat, float frameDuration, int initialQueueSize, int queueGrowAmount, int queueLimit) {
        return true;
    }
    
    @Override
    public boolean isPlaying() {
        return playRequested;
    }
    
    @Override
    public void play() {
        playRequested = true;
    }
    
    @Override
    public void pause() {
        playRequested = false;
    }
    
    @Override
    public void flush() {        
    }
    
    @Override
    public void destroy() {
    }
    
    @Override
    public final int getEnqueuedFrameCount() {
        return 0;
    }
    
    @Override
    public int getFrameCount() {
        return 0;
    }
    
    @Override
    public int getQueuedFrameCount() {
        return 0;
    }
    
    @Override
    public int getQueuedByteCount() {
        return 0;
    }
    
    @Override
    public int getQueuedTime() {
        return 0;
    }
    
    @Override
    public final int getPTS() { return 0; }
    
    @Override
    public int getFreeFrameCount() {
        return 1;        
    }
    
    @Override
    public AudioFrame enqueueData(AudioDataFrame audioDataFrame) {
        return null;
    }

    @Override
    public AudioFrame enqueueData(int pts, ByteBuffer bytes, int byteCount) {
        return null;
    }    
}
