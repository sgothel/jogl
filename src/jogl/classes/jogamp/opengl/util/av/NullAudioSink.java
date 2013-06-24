package jogamp.opengl.util.av;

public class NullAudioSink implements AudioSink {

    @Override
    public int getDataAvailable() {
        return 0;
    }

    @Override
    public boolean isDataAvailable(int data_size) {
        return false;
    }

    @Override
    public void writeData(byte[] sampleData, int data_size) {
    }

    @Override
    public boolean isAudioSinkAvailable() {
        return true;
    }
}
