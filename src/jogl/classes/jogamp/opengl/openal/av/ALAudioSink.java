package jogamp.opengl.openal.av;

import java.util.Arrays;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.SourceDataLine;

import jogamp.opengl.util.av.AudioSink;

import com.jogamp.openal.*;
import com.jogamp.openal.util.*;

public class ALAudioSink implements AudioSink {

    static ALC alc;
    static AL al;
    static ALCdevice device;
    static ALCcontext context;

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
    
    private static boolean available = false;
    
    static {
        
        boolean joalFound = false;
        try {
            Class.forName("com.jogamp.openal.ALFactory");
            joalFound = true;
        } catch(ClassNotFoundException e){
            // Joal not found on classpath
        }            
            
        if(joalFound) {
        
            alc = ALFactory.getALC();
            al = ALFactory.getAL();
            String deviceSpecifier;

            // Get handle to default device.
            device = alc.alcOpenDevice(null);
            if (device == null) {
                throw new ALException("Error opening default OpenAL device");
            }
        
            // Get the device specifier.
            deviceSpecifier = alc.alcGetString(device, ALC.ALC_DEVICE_SPECIFIER);
            if (deviceSpecifier == null) {
                throw new ALException("Error getting specifier for default OpenAL device");
            }
                
            // Create audio context.
            context = alc.alcCreateContext(device, null);
            if (context == null) {
                throw new ALException("Error creating OpenAL context");
            }
        
            // Set active context.
            alc.alcMakeContextCurrent(context);
        
            // Check for an error.
            if (alc.alcGetError(device) != ALC.ALC_NO_ERROR) {
                throw new ALException("Error making OpenAL context current");
            }
        
            System.out.println("OpenAL audio sink using device: " + deviceSpecifier);        
            available = true;
        }
    }
    
    @Override
    public boolean isDataAvailable(int data_size) {
        return false;
    }
    
    @Override
    public void writeData(byte[] sampleData, int data_size) {
       
    }

    @Override
    public int getDataAvailable() {
        return 0;
    }

    public static boolean isAvailable() {
        return available;
    }
}
