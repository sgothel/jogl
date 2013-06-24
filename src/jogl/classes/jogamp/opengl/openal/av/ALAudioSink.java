package jogamp.opengl.openal.av;

import java.nio.Buffer;
import java.nio.ByteBuffer;

import jogamp.opengl.util.av.AudioSink;

import com.jogamp.common.nio.Buffers;
import com.jogamp.openal.*;

public class ALAudioSink implements AudioSink {

    static ALC alc;
    static AL al;
    static ALCdevice device;
    static ALCcontext context;

    // AudioFormat parameters
    public  static final int     SAMPLE_RATE = 44100;

    // Chunk of audio processed at one time
    public static final int BUFFER_SIZE = 1000;
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    
    // Sample time values
    public static final double SAMPLE_TIME_IN_SECS = 1.0 / SAMPLE_RATE;
    public static final double BUFFER_TIME_IN_SECS = SAMPLE_TIME_IN_SECS * SAMPLES_PER_BUFFER;
    
    private static int NUM_BUFFERS = 5;
    private static int bufferNumber = 0;
    private static int[] buffers = new int[NUM_BUFFERS];
    private static int[] source = new int[1];
    private static boolean initBuffer = true;
    private static int frequency = 44100;
    private static int format = AL.AL_FORMAT_STEREO16;
    
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
            
            al = ALFactory.getAL();
            
            // Allocate buffers
            al.alGenBuffers(NUM_BUFFERS, buffers, 0);                       
            al.alGenSources(1, source, 0);
            
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new ALException("Error generating :(");                 
            }            
        
            System.out.println("OpenAL audio sink using device: " + deviceSpecifier);        
            available = true;
        }
    }
    
    @Override
    public boolean isDataAvailable(int data_size) {               
        return true;
    }
    
    @Override
    public void writeData(byte[] sampleData, int data_size) {
        // OpenAL consumes buffers in the background
        // we first need to initialize the OpenAL buffers then
        // start continous playback.
        alc.alcMakeContextCurrent(context);
        if(initBuffer) {

            ByteBuffer data = Buffers.newDirectByteBuffer(sampleData);
            al.alBufferData(buffers[bufferNumber], format, data, data_size, frequency);
            int error = al.alGetError();
            if(error != AL.AL_NO_ERROR) {
                System.out.println("bufferNumber"+bufferNumber+" Data "+sampleData+" size"+data_size);
                throw new ALException("Error loading :( error code: " + error);                
            }

            if(bufferNumber==NUM_BUFFERS-1){
                // all buffers queued
                al.alSourceQueueBuffers(source[0], NUM_BUFFERS, buffers, 0);
                // start playback
                al.alSourcePlay(source[0]);
                if(al.alGetError() != AL.AL_NO_ERROR) {
                    throw new ALException("Error starting :(");                 
                }
                initBuffer=false;
            }

            // update buffer number to fill
            bufferNumber=(bufferNumber+1)%NUM_BUFFERS;
        } else {
            // OpenAL is playing in the background.
            // one new frame with audio data is ready

            // first wait for openal to release one buffer
            int[] buffer=new int[1];
            int[] val=new int[1];
            do {
                al.alGetSourcei(source[0], AL.AL_BUFFERS_PROCESSED, val, 0);
            } while (val[0] <= 0);

            // fill and requeue the empty buffer
            al.alSourceUnqueueBuffers(source[0], 1, buffer , 0);
            Buffer data = Buffers.newDirectByteBuffer(sampleData);
            al.alBufferData(buffer[0], format, data, data_size, frequency);
            al.alSourceQueueBuffers(source[0], 1, buffer, 0);
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new ALException("Error buffering :(");                 
            }

            // Restart openal playback if needed
            al.alGetSourcei(source[0], AL.AL_SOURCE_STATE, val, 0);
            if(val[0] != al.AL_PLAYING) {
                al.alSourcePlay(source[0]);
            }
        }
    }

    @Override
    public int getDataAvailable() {
        int[] val=new int[1];       
        al.alGetSourcei(source[0], AL.AL_BUFFERS_PROCESSED, val, 0);
        return (NUM_BUFFERS-val[0])*4096;
    }

    public static boolean isAvailable() {
        return available;
    }
}
