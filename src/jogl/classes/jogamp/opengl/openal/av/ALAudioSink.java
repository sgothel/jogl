/**
 * Copyright 2013 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package jogamp.opengl.openal.av;


import jogamp.opengl.util.av.SyncedRingbuffer;

import com.jogamp.openal.AL;
import com.jogamp.openal.ALC;
import com.jogamp.openal.ALCcontext;
import com.jogamp.openal.ALCdevice;
import com.jogamp.openal.ALFactory;
import com.jogamp.opengl.util.av.AudioSink;

/***
 * OpenAL Audio Sink
 */
public class ALAudioSink implements AudioSink {

    /** Chunk of audio processed at one time. FIXME: Parameterize .. */
    public static final int BUFFER_SIZE = 4096;
    public static final int SAMPLES_PER_BUFFER = BUFFER_SIZE / 2;
    
    private static final ALC alc;
    private static final AL al;
    private static final boolean staticAvailable;    
    
    private String deviceSpecifier;
    private ALCdevice device;
    private ALCcontext context;

    /** Sample period in seconds */
    public float samplePeriod;
    /** Buffer period in seconds */
    public float bufferPeriod;
    
    static class ActiveBuffer {
        ActiveBuffer(Integer name, int size) {
            this.name = name;
            this.size = size;
        }
        public final Integer name;
        public final int size;
        public String toString() { return "ABuffer[name "+name+", size "+size+"]"; }
    }
    
    int[] alBuffers = null;
    private SyncedRingbuffer<Integer> alBufferAvail = null;
    private SyncedRingbuffer<ActiveBuffer> alBufferPlaying = null;
    private int alBufferBytesQueued = 0;

    private int[] alSource = null;
    private AudioDataFormat chosenFormat;
    private int alFormat;    
    private boolean initialized;

    static {
        ALC _alc = null;
        AL _al = null;
        try {
            _alc = ALFactory.getALC();            
            _al = ALFactory.getAL();
        } catch(Throwable t) {
            if( DEBUG ) {
                System.err.println("ALAudioSink: Catched "+t.getClass().getName()+": "+t.getMessage());
                t.printStackTrace();
            }
        }
        alc = _alc;
        al = _al;
        staticAvailable = null != alc && null != al;
    }
    
    public ALAudioSink() {
        initialized = false;
        chosenFormat = null;
        
        if( !staticAvailable ) {
            return;
        }
        
        try {
            // Get handle to default device.
            device = alc.alcOpenDevice(null);
            if (device == null) {
                throw new RuntimeException("ALAudioSink: Error opening default OpenAL device");
            }
        
            // Get the device specifier.
            deviceSpecifier = alc.alcGetString(device, ALC.ALC_DEVICE_SPECIFIER);
            if (deviceSpecifier == null) {
                throw new RuntimeException("ALAudioSink: Error getting specifier for default OpenAL device");
            }
                
            // Create audio context.
            context = alc.alcCreateContext(device, null);
            if (context == null) {
                throw new RuntimeException("ALAudioSink: Error creating OpenAL context");
            }
        
            // Set active context.
            alc.alcMakeContextCurrent(context);
        
            // Check for an error.
            if ( alc.alcGetError(device) != ALC.ALC_NO_ERROR ) {
                throw new RuntimeException("ALAudioSink: Error making OpenAL context current");
            }
            
            // Create source
            {
                alSource = new int[1];
                al.alGenSources(1, alSource, 0);
                final int err = al.alGetError();
                if( err != AL.AL_NO_ERROR ) {
                    alSource = null;
                    throw new RuntimeException("ALAudioSink: Error generating Source: 0x"+Integer.toHexString(err));
                }       
            }
            
            if( DEBUG ) {                
                System.err.println("ALAudioSink: Using device: " + deviceSpecifier);
            }
            initialized = true;
            return;
        } catch ( Exception e ) {
            if( DEBUG ) {
                System.err.println(e.getMessage());
            }
            destroy();
        }
    }
    
    @Override
    public String toString() {
        final int alSrcName = null != alSource ? alSource[0] : 0;
        final int alBuffersLen = null != alBuffers ? alBuffers.length : 0;
        return "ALAudioSink[init "+initialized+", device "+deviceSpecifier+", ctx "+context+", alSource "+alSrcName+
               ", chosen "+chosenFormat+", alFormat "+toHexString(alFormat)+
               ", buffers[total "+alBuffersLen+", avail "+alBufferAvail.size()+", "+alBufferPlaying.getFreeSlots()+
               ", queued[bufferCount "+alBufferPlaying.size()+", "+getQueuedTime() + " ms, " + alBufferBytesQueued+" bytes]";
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
        samplePeriod = 1.0f / requestedFormat.sampleRate;
        bufferPeriod = samplePeriod * SAMPLES_PER_BUFFER;
        switch( requestedFormat.channelCount ) {
            case 1: { 
                switch ( requestedFormat.sampleSize ) {
                    case 8: 
                        alFormat = AL.AL_FORMAT_MONO8; break;
                    case 16:
                        alFormat = AL.AL_FORMAT_MONO16; break;
                    default:
                        return null;
                }
            } break;
            case 2: 
                switch ( requestedFormat.sampleSize ) {
                    case 8: 
                        alFormat = AL.AL_FORMAT_STEREO8; break;
                    case 16:
                        alFormat = AL.AL_FORMAT_STEREO16; break;
                    default:
                        return null;
                }
        }
        // Allocate buffers
        destroyBuffers();
        {
            alBuffers = new int[bufferCount];
            al.alGenBuffers(bufferCount, alBuffers, 0);
            final int err = al.alGetError();
            if( err != AL.AL_NO_ERROR ) {
                alBuffers = null;
                throw new RuntimeException("ALAudioSink: Error generating Buffers: 0x"+Integer.toHexString(err));
            }
            final Integer[] alBufferRingArray = new Integer[bufferCount];
            for(int i=0; i<bufferCount; i++) {
                alBufferRingArray[i] = Integer.valueOf(alBuffers[i]);
            }
            alBufferAvail = new SyncedRingbuffer<Integer>(alBufferRingArray, true /* full */);
            alBufferPlaying = new SyncedRingbuffer<ActiveBuffer>(new ActiveBuffer[bufferCount], false /* full */);
        }
        
        
        chosenFormat = requestedFormat;
        return chosenFormat;
    }
    
    private void destroyBuffers() {
        if( !staticAvailable ) {
            return;
        }
        if( null != alBuffers ) {
            try {
                al.alDeleteBuffers(alBufferAvail.capacity(), alBuffers, 0);
            } catch (Throwable t) {
                if( DEBUG ) {
                    System.err.println("Catched "+t.getClass().getName()+": "+t.getMessage());
                    t.printStackTrace();
                }
            }
            alBufferAvail.clear(true);
            alBufferAvail = null;
            alBufferPlaying.clear(true);
            alBufferPlaying = null;
            alBufferBytesQueued = 0;
            alBuffers = null;
        }
    }
    
    @Override
    public void destroy() {
        initialized = false;
        if( !staticAvailable ) {
            return;
        }
        if( null != alSource ) {
            try {
                al.alDeleteSources(1, alSource, 0);
            } catch (Throwable t) {
                if( DEBUG ) {
                    System.err.println("Catched "+t.getClass().getName()+": "+t.getMessage());
                    t.printStackTrace();
                }
            }
            alSource = null;
        }
        
        destroyBuffers();

        if( null != context ) {
            try {
                alc.alcDestroyContext(context);
            } catch (Throwable t) {
                if( DEBUG ) {
                    System.err.println("Catched "+t.getClass().getName()+": "+t.getMessage());
                    t.printStackTrace();
                }
            }
            context = null;            
        }
        if( null != device ) {
            try {
                alc.alcCloseDevice(device);
            } catch (Throwable t) {
                if( DEBUG ) {
                    System.err.println("Catched "+t.getClass().getName()+": "+t.getMessage());
                    t.printStackTrace();
                }
            }
            device = null;            
        }
        chosenFormat = null;
    }
    
    @Override
    public boolean isInitialized() {
        return initialized;
    }
    
    private final void dequeueBuffer(boolean wait) {
        int alErr = AL.AL_NO_ERROR;
        final int[] val=new int[1];
        do {
            al.alGetSourcei(alSource[0], AL.AL_BUFFERS_PROCESSED, val, 0);
            alErr = al.alGetError();
            if( AL.AL_NO_ERROR != alErr ) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while quering processed buffers at source. "+this);                 
            }
            if( wait && val[0] <= 0 ) {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e){
                }
            }
        } while (val[0] <= 0);
        final int processedBuffers = val[0];

        if( processedBuffers > 0 ) {
            int[] buffers=new int[processedBuffers];
            al.alSourceUnqueueBuffers(alSource[0], processedBuffers, buffers, 0);
            alErr = al.alGetError();
            if( AL.AL_NO_ERROR != alErr ) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while dequeueing "+processedBuffers+" processed buffers. "+this);                 
            }
            for ( int i=0; i<processedBuffers; i++ ) {
                final ActiveBuffer releasedBuffer = alBufferPlaying.get(true /* clearRef */);
                if( null == releasedBuffer ) {
                    throw new InternalError("Internal Error: "+this);
                }
                if( releasedBuffer.name.intValue() != buffers[i] ) {                    
                    throw new InternalError("Buffer name mismatch: dequeued: "+buffers[i]+", released "+releasedBuffer);
                    // System.err.println("XXX ["+i+"]: dequeued: "+buffers[i]+", released "+releasedBuffer);
                }
                alBufferBytesQueued -= releasedBuffer.size;
                if( !alBufferAvail.put(releasedBuffer.name) ) {
                    throw new InternalError("Internal Error: "+this);
                }
                if( DEBUG ) {
                    System.err.println("ALAudioSink: Dequeued "+processedBuffers+", wait "+wait+", "+this);
                }                
            }
        }
    }
    
    private static final String toHexString(int v) {
        return "0x"+Integer.toHexString(v);
    }
        
    @Override
    public void writeData(AudioFrame audioFrame) {
        if( !initialized || null == chosenFormat ) {
            return;
        }
        int alErr = AL.AL_NO_ERROR;
            
        // OpenAL consumes buffers in the background
        // we first need to initialize the OpenAL buffers then
        // start continuous playback.
        alc.alcMakeContextCurrent(context);
        alErr = al.alGetError();
        if(al.alGetError() != AL.AL_NO_ERROR) {
            throw new RuntimeException("ALError "+toHexString(alErr)+" while makeCurrent. "+this);
        }
        
        if( alBufferAvail.isEmpty() ) {
            dequeueBuffer(true);
        }
        
        final Integer alBufferName = alBufferAvail.get(true /* clearRef */);
        if( null == alBufferName ) {
            throw new InternalError("Internal Error: "+this);
        }
        if( !alBufferPlaying.put( new ActiveBuffer(alBufferName, audioFrame.dataSize) ) ) {
            throw new InternalError("Internal Error: "+this);
        }
        al.alBufferData(alBufferName.intValue(), alFormat, audioFrame.data, audioFrame.dataSize, chosenFormat.sampleRate);
        final int[] alBufferNames = new int[] { alBufferName.intValue() };
        al.alSourceQueueBuffers(alSource[0], 1, alBufferNames, 0);
        alErr = al.alGetError();
        if(al.alGetError() != AL.AL_NO_ERROR) {
            throw new RuntimeException("ALError "+toHexString(alErr)+" while queueing buffer "+toHexString(alBufferNames[0])+". "+this);
        }
        alBufferBytesQueued += audioFrame.dataSize;
        
        // Restart openal playback if needed
        {
            int[] val = new int[1];
            al.alGetSourcei(alSource[0], AL.AL_SOURCE_STATE, val, 0);
            if(val[0] != AL.AL_PLAYING) {
                if( DEBUG ) {
                    System.err.println("ALAudioSink: Start playing: "+this);
                }
                al.alSourcePlay(alSource[0]);
                alErr = al.alGetError();
                if(al.alGetError() != AL.AL_NO_ERROR) {
                    throw new RuntimeException("ALError "+toHexString(alErr)+" while start playing. "+this);
                }
            }
        }
    }

    @Override
    public int getQueuedByteCount() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return alBufferBytesQueued;
    }
    
    @Override
    public int getQueuedTime() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        final int bps = chosenFormat.sampleSize / 8;
        return alBufferBytesQueued / ( chosenFormat.channelCount * bps * ( chosenFormat.sampleRate / 1000 ) );
    }
    
    @Override
    public int getWritableBufferCount() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return alBufferPlaying.getFreeSlots();
    }
    
    @Override
    public boolean isDataAvailable(int data_size) {               
        return initialized && null != chosenFormat;
    }
    
}
