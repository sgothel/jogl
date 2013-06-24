package jogamp.opengl.util.av;

public interface AudioSink {

    boolean isAudioSinkAvailable();
    
    int getDataAvailable();

    boolean isDataAvailable(int data_size);

    void writeData(byte[] sampleData, int data_size);
    
}
