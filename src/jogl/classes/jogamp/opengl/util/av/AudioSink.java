package jogamp.opengl.util.av;

public interface AudioSink {

    int getDataAvailable();

    boolean isDataAvailable(int data_size);

    void writeData(byte[] sampleData, int data_size);
    
}
