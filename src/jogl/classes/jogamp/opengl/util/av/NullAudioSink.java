package jogamp.opengl.util.av;


import com.jogamp.opengl.util.av.AudioSink;

public class NullAudioSink implements AudioSink {

    @Override
    public boolean isInitialized() {
        return true;
    }

    @Override
    public AudioDataFormat getPreferredFormat() {
        return DefaultFormat;
    }

    @Override
    public AudioDataFormat initSink(AudioDataFormat requestedFormat, int bufferCount) {
        return requestedFormat;
    }
    
    @Override
    public void destroy() {
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
    public int getWritableBufferCount() {
        return 1;        
    }
    
    @Override
    public boolean isDataAvailable(int data_size) {
        return false;
    }

    @Override
    public void writeData(AudioFrame audioFrame) {
    }
}
