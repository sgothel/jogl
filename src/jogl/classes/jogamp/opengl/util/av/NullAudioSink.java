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
    public AudioDataFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public AudioDataFormat initSink(AudioDataFormat requestedFormat, int initialFrameCount, int frameGrowAmount, int frameLimit) {
        return requestedFormat;
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
