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


import com.jogamp.common.util.LFRingbuffer;
import com.jogamp.common.util.Ringbuffer;
import com.jogamp.common.util.locks.LockFactory;
import com.jogamp.common.util.locks.RecursiveLock;
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

    private static final ALC alc;
    private static final AL al;
    private static final boolean staticAvailable;    
    
    private String deviceSpecifier;
    private ALCdevice device;
    private ALCcontext context;
    private final RecursiveLock lock = LockFactory.createRecursiveLock();

    /** Playback speed, range [0.5 - 2.0], default 1.0. */
    private float playSpeed;
    
    static class ActiveBuffer {
        ActiveBuffer(Integer name, int pts, int size) {
            this.name = name;
            this.pts = pts;
            this.size = size;
        }
        public final Integer name;
        public final int pts;
        public final int size;
        public String toString() { return "ABuffer[name "+name+", pts "+pts+", size "+size+"]"; }
    }
    
    private int[] alBuffers = null;
    private int frameGrowAmount = 0;
    private int frameLimit = 0;
        
    private Ringbuffer<Integer> alBufferAvail = null;
    private Ringbuffer<ActiveBuffer> alBufferPlaying = null;
    private volatile int alBufferBytesQueued = 0;
    private volatile int playingPTS = AudioFrame.INVALID_PTS;
    private volatile int enqueuedFrameCount;

    private int[] alSource = null;
    private AudioDataFormat chosenFormat;
    private int alFormat;    
    private boolean initialized;
    
    private volatile boolean playRequested = false;

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
    
    private static Ringbuffer.AllocEmptyArray<Integer> rbAllocIntArray = new Ringbuffer.AllocEmptyArray<Integer>() {
        @Override
        public Integer[] newArray(int size) {
            return new Integer[size];
        }        
    };
    private static Ringbuffer.AllocEmptyArray<ActiveBuffer> rbAllocActiveBufferArray = new Ringbuffer.AllocEmptyArray<ActiveBuffer>() {
        @Override
        public ActiveBuffer[] newArray(int size) {
            return new ActiveBuffer[size];
        }        
    };
    
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
        
            lockContext();
            try {
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
            } finally {
                unlockContext();
            }
            return;
        } catch ( Exception e ) {
            if( DEBUG ) {
                System.err.println(e.getMessage());
            }
            destroy();
        }
    }
    
    private final void lockContext() {
        lock.lock();
        alc.alcMakeContextCurrent(context);
    }
    private final void unlockContext() {
        alc.alcMakeContextCurrent(null);
        lock.unlock();
    }
    private final void destroyContext() {
        lock.lock();
        try {
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
            // unroll lock !
            while(lock.getHoldCount() > 1) {
                lock.unlock();
            }
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public final String toString() {
        final int alSrcName = null != alSource ? alSource[0] : 0;
        final int alBuffersLen = null != alBuffers ? alBuffers.length : 0;
        final int ctxHash = context != null ? context.hashCode() : 0; 
        return "ALAudioSink[init "+initialized+", playRequested "+playRequested+", device "+deviceSpecifier+", ctx "+toHexString(ctxHash)+", alSource "+alSrcName+
               ", chosen "+chosenFormat+", alFormat "+toHexString(alFormat)+
               ", playSpeed "+playSpeed+", buffers[total "+alBuffersLen+", avail "+alBufferAvail.size()+", "+
               "queued["+alBufferPlaying.size()+", apts "+getPTS()+", "+getQueuedTime() + " ms, " + alBufferBytesQueued+" bytes]";
    }
    public final String getPerfString() {
        final int alBuffersLen = null != alBuffers ? alBuffers.length : 0;
        return "Play [buffer "+alBufferPlaying.size()+"/"+alBuffersLen+", apts "+getPTS()+", "+getQueuedTime() + " ms, " + alBufferBytesQueued+" bytes]";
    }
    
    @Override
    public final AudioDataFormat getPreferredFormat() {
        return DefaultFormat;
    }
    
    @Override
    public final AudioDataFormat initSink(AudioDataFormat requestedFormat, int initialFrameCount, int frameGrowAmount, int frameLimit) {
        if( !staticAvailable ) {
            return null;
        }
        if( !requestedFormat.fixedP ||
            !requestedFormat.littleEndian ||
            ( 1 != requestedFormat.channelCount && requestedFormat.channelCount != 2 ) ||
            ( 8 != requestedFormat.sampleSize  && requestedFormat.sampleSize != 16 )
          ) {
            return null; // not supported w/ OpenAL
        }
        // final float samplePeriod = 1.0f / requestedFormat.sampleRate;
        switch( requestedFormat.channelCount ) {
            case 1: { 
                switch ( requestedFormat.sampleSize ) {
                    case 8: 
                        alFormat = AL.AL_FORMAT_MONO8; break;
                    case 16:
                        alFormat = AL.AL_FORMAT_MONO16; break;
                }
            } break;
            case 2: 
                switch ( requestedFormat.sampleSize ) {
                    case 8: 
                        alFormat = AL.AL_FORMAT_STEREO8; break;
                    case 16:
                        alFormat = AL.AL_FORMAT_STEREO16; break;
                }
        }
        lockContext();
        try {
            // Allocate buffers
            destroyBuffers();
            {
                alBuffers = new int[initialFrameCount];
                al.alGenBuffers(initialFrameCount, alBuffers, 0);
                final int err = al.alGetError();
                if( err != AL.AL_NO_ERROR ) {
                    alBuffers = null;
                    throw new RuntimeException("ALAudioSink: Error generating Buffers: 0x"+Integer.toHexString(err));
                }
                final Integer[] alBufferRingArray = new Integer[initialFrameCount];
                for(int i=0; i<initialFrameCount; i++) {
                    alBufferRingArray[i] = Integer.valueOf(alBuffers[i]);
                }
                alBufferAvail = new LFRingbuffer<Integer>(alBufferRingArray, rbAllocIntArray);
                alBufferPlaying = new LFRingbuffer<ActiveBuffer>(initialFrameCount, rbAllocActiveBufferArray);
                this.frameGrowAmount = frameGrowAmount;
                this.frameLimit = frameLimit;
            }
        } finally {
            unlockContext();
        }
        
        chosenFormat = requestedFormat;
        return chosenFormat;
    }
    
    private boolean growBuffers() {
        if( !alBufferAvail.isEmpty() || !alBufferPlaying.isFull() ) {
            throw new InternalError("Buffers: Avail is !empty "+alBufferAvail+", Playing is !full "+alBufferPlaying);
        }
        if( alBufferAvail.capacity() >= frameLimit || alBufferPlaying.capacity() >= frameLimit ) {
            if( DEBUG ) {
                System.err.println(getThreadName()+": ALAudioSink.growBuffers: Frame limit "+frameLimit+" reached: Avail "+alBufferAvail+", Playing "+alBufferPlaying);
            }
            return false;
        }
        
        final int[] newElems = new int[frameGrowAmount];
        al.alGenBuffers(frameGrowAmount, newElems, 0);
        final int err = al.alGetError();
        if( err != AL.AL_NO_ERROR ) {
            if( DEBUG ) {
                System.err.println(getThreadName()+": ALAudioSink.growBuffers: Error generating "+frameGrowAmount+" new Buffers: 0x"+Integer.toHexString(err));
            }
            return false;
        }
        final Integer[] newElemsI = new Integer[frameGrowAmount];
        for(int i=0; i<frameGrowAmount; i++) {
            newElemsI[i] = Integer.valueOf(newElems[i]);
        }
        
        final int oldSize = alBuffers.length;
        final int newSize = oldSize + frameGrowAmount;
        final int[] newBuffers = new int[newSize];
        System.arraycopy(alBuffers, 0, newBuffers,       0, oldSize);
        System.arraycopy(newElems,  0, newBuffers, oldSize, frameGrowAmount);
        alBuffers = newBuffers;

        alBufferAvail.growBuffer(newElemsI, frameGrowAmount, rbAllocIntArray);
        alBufferPlaying.growBuffer(null, frameGrowAmount, rbAllocActiveBufferArray);
        if( alBufferAvail.isEmpty() || alBufferPlaying.isFull() ) {
            throw new InternalError("Buffers: Avail is empty "+alBufferAvail+", Playing is full "+alBufferPlaying);
        }
        if( DEBUG ) {
            System.err.println(getThreadName()+": ALAudioSink: Buffer grown "+frameGrowAmount+": Avail "+alBufferAvail+", playing "+alBufferPlaying);
        }
        return true;
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
            alBufferAvail.clear();
            alBufferAvail = null;
            alBufferPlaying.clear();
            alBufferPlaying = null;
            alBufferBytesQueued = 0;
            alBuffers = null;
        }
    }
    
    @Override
    public final void destroy() {
        initialized = false;
        if( !staticAvailable ) {
            return;
        }
        if( null != context ) {
            lockContext();
        }
        try {
            stopImpl();
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
        } finally {
            destroyContext();
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
    public final boolean isInitialized() {
        return initialized;
    }
    
    private final int dequeueBuffer(boolean flush, boolean wait) {
        int alErr = AL.AL_NO_ERROR;
        final int releaseBufferCount;
        if( flush ) {
            releaseBufferCount = alBufferPlaying.size();
        } else if( alBufferBytesQueued > 0 ) {
            final int releaseBufferLimes = Math.max(1, alBufferPlaying.size() / 4 );
            final int[] val=new int[1];
            int i=0;
            do {
                al.alGetSourcei(alSource[0], AL.AL_BUFFERS_PROCESSED, val, 0);
                alErr = al.alGetError();
                if( AL.AL_NO_ERROR != alErr ) {
                    throw new RuntimeException("ALError "+toHexString(alErr)+" while quering processed buffers at source. "+this);
                }
                if( wait && val[0] < releaseBufferLimes ) {
                    i++;
                    // clip wait at [2 .. 100] ms
                    final int avgBufferDura = getQueuedTimeImpl( alBufferBytesQueued / alBufferPlaying.size() );
                    final int sleep = Math.max(2, Math.min(100, releaseBufferLimes * avgBufferDura));
                    if( DEBUG ) {
                        System.err.println(getThreadName()+": ALAudioSink: Dequeue.wait["+i+"]: avgBufferDura "+avgBufferDura+", releaseBufferLimes "+releaseBufferLimes+", sleep "+sleep+" ms, playImpl "+isPlayingImpl1()+", processed "+val[0]+", "+this);
                    }                
                    unlockContext();
                    try {
                        Thread.sleep( sleep - 1 );
                    } catch (InterruptedException e) {
                    } finally {
                        lockContext();
                    }
                }
            } while ( wait && val[0] < releaseBufferLimes && alBufferBytesQueued > 0 );
            releaseBufferCount = val[0];
        } else {
            releaseBufferCount = 0;
        }

        if( releaseBufferCount > 0 ) {
            int[] buffers=new int[releaseBufferCount];
            al.alSourceUnqueueBuffers(alSource[0], releaseBufferCount, buffers, 0);
            alErr = al.alGetError();
            if( AL.AL_NO_ERROR != alErr ) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while dequeueing "+releaseBufferCount+" buffers. "+this);                 
            }
            for ( int i=0; i<releaseBufferCount; i++ ) {
                final ActiveBuffer releasedBuffer = alBufferPlaying.get();
                if( null == releasedBuffer ) {
                    throw new InternalError("Internal Error: "+this);
                }
                if( releasedBuffer.name.intValue() != buffers[i] ) {
                    alBufferAvail.dump(System.err, "Avail-deq02-post");
                    alBufferPlaying.dump(System.err, "Playi-deq02-post");                    
                    throw new InternalError("Buffer name mismatch: dequeued: "+buffers[i]+", released "+releasedBuffer+", "+this);
                }
                alBufferBytesQueued -= releasedBuffer.size;
                if( !alBufferAvail.put(releasedBuffer.name) ) {
                    throw new InternalError("Internal Error: "+this);
                }
            }
            if( flush && ( !alBufferAvail.isFull() || !alBufferPlaying.isEmpty() ) ) {
                alBufferAvail.dump(System.err, "Avail-deq03-post");
                alBufferPlaying.dump(System.err, "Playi-deq03-post");
                throw new InternalError("Flush failure: "+this);
            }
        }
        return releaseBufferCount;
    }
    
    private final int dequeueBuffer(boolean wait, AudioFrame inAudioFrame) {
        final int dequeuedBufferCount = dequeueBuffer( false /* flush */, wait );        
        final ActiveBuffer currentBuffer = alBufferPlaying.peek();
        if( null != currentBuffer ) {
            playingPTS = currentBuffer.pts;
        } else {
            playingPTS = inAudioFrame.pts;
        }
        if( DEBUG ) {
            if( dequeuedBufferCount > 0 ) {
                System.err.println(getThreadName()+": ALAudioSink: Write "+inAudioFrame.pts+", "+getQueuedTimeImpl(inAudioFrame.dataSize)+" ms, dequeued "+dequeuedBufferCount+", wait "+wait+", "+getPerfString());
            }
        }
        return dequeuedBufferCount;
    }
    
    @Override
    public final void enqueueData(AudioFrame audioFrame) {
        if( !initialized || null == chosenFormat ) {
            return;
        }
        int alErr = AL.AL_NO_ERROR;
            
        // OpenAL consumes buffers in the background
        // we first need to initialize the OpenAL buffers then
        // start continuous playback.
        lockContext();
        try {
            alErr = al.alGetError();
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while makeCurrent. "+this);
            }
            
            final boolean dequeueDone;
            if( alBufferAvail.isEmpty() ) {
                // try to dequeue first
                dequeueDone = dequeueBuffer(false, audioFrame) > 0;
                if( alBufferAvail.isEmpty() ) {
                    // try to grow
                    growBuffers();
                }
            } else {
                dequeueDone = false;
            }
            if( !dequeueDone && alBufferPlaying.size() > 0 ) { // dequeue only possible if playing ..
                final boolean wait = isPlayingImpl0() && alBufferAvail.isEmpty(); // possible if grow failed or already exceeds it's limit!
                dequeueBuffer(wait, audioFrame);
            }
            
            final Integer alBufferName = alBufferAvail.get();
            if( null == alBufferName ) {
                alBufferAvail.dump(System.err, "Avail");
                throw new InternalError("Internal Error: avail.get null "+alBufferAvail+", "+this);
            }
            if( !alBufferPlaying.put( new ActiveBuffer(alBufferName, audioFrame.pts, audioFrame.dataSize) ) ) {
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
            enqueuedFrameCount++;
            
            playImpl(); // continue playing, fixes issue where we ran out of enqueued data!
        } finally {
            unlockContext();
        }
    }

    @Override
    public final boolean isPlaying() {
        if( !initialized || null == chosenFormat ) {
            return false;
        }
        if( playRequested ) {
            lockContext();
            try {
                return isPlayingImpl0();
            } finally {
                unlockContext();
            }                            
        } else {
            return false;
        }
    }
    private final boolean isPlayingImpl0() {
        if( playRequested ) {
            return isPlayingImpl1();
        } else {
            return false;
        }
    }
    private final boolean isPlayingImpl1() {
        final int[] val = new int[1];
        al.alGetSourcei(alSource[0], AL.AL_SOURCE_STATE, val, 0);
        final int alErr = al.alGetError();
        if(al.alGetError() != AL.AL_NO_ERROR) {
            throw new RuntimeException("ALError "+toHexString(alErr)+" while querying isPlaying. "+this);
        }
        return val[0] == AL.AL_PLAYING;
    }
    
    @Override
    public final void play() {
        if( !initialized || null == chosenFormat ) {
            return;
        }
        playRequested = true;
        lockContext();
        try {
            playImpl();
            if( DEBUG ) {
                System.err.println(getThreadName()+": ALAudioSink: PLAY playImpl "+isPlayingImpl1()+", "+this);
            }        
        } finally {
            unlockContext();
        }                
    }
    private final void playImpl() {
        if( playRequested && !isPlayingImpl1() ) {
            al.alSourcePlay(alSource[0]);
            final int alErr = al.alGetError();
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while start playing. "+this);
            }
        }        
    }
    
    @Override
    public final void pause() {
        if( !initialized || null == chosenFormat ) {
            return;
        }
        if( playRequested ) {
            lockContext();
            try {
                pauseImpl();
                if( DEBUG ) {
                    System.err.println(getThreadName()+": ALAudioSink: PAUSE playImpl "+isPlayingImpl1()+", "+this);
                }        
            } finally {
                unlockContext();
            }
        }
    }
    private final void pauseImpl() {
        if( isPlayingImpl0() ) {
            playRequested = false;
            al.alSourcePause(alSource[0]);
            final int alErr = al.alGetError();
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while pausing. "+this);
            }
        }
    }
    private final void stopImpl() {
        if( isPlayingImpl0() ) {
            playRequested = false;
            al.alSourceStop(alSource[0]);
            final int alErr = al.alGetError();
            if(al.alGetError() != AL.AL_NO_ERROR) {
                throw new RuntimeException("ALError "+toHexString(alErr)+" while pausing. "+this);
            }
        }
    }
    
    @Override
    public final float getPlaySpeed() { return playSpeed; }
    
    @Override
    public final boolean setPlaySpeed(float rate) { 
        if( !initialized || null == chosenFormat ) {
            return false;
        }
        lockContext();
        try {
            if( Math.abs(1.0f - rate) < 0.01f ) {
                rate = 1.0f;
            }
            if( 0.5f <= rate && rate <= 2.0f ) { // OpenAL limits 
                playSpeed = rate;
                al.alSourcef(alSource[0], AL.AL_PITCH, playSpeed);
                return true;
            } 
        } finally {
            unlockContext();
        }
        return false; 
    }
    
    @Override
    public final void flush() {
        if( !initialized || null == chosenFormat ) {
            return;
        }
        lockContext();
        try {
            // pauseImpl();
            stopImpl();
            dequeueBuffer( true /* flush */, false /* wait */ );
            if( alBuffers.length != alBufferAvail.size() || alBufferPlaying.size() != 0 ) {
                throw new InternalError("XXX: "+this);
            }
            if( DEBUG ) {
                System.err.println(getThreadName()+": ALAudioSink: FLUSH playImpl "+isPlayingImpl1()+", "+this);
            }        
        } finally {
            unlockContext();
        }                
    }
    
    @Override
    public final int getEnqueuedFrameCount() {
        return enqueuedFrameCount;
    }
    
    @Override
    public final int getFrameCount() {
        return null != alBuffers ? alBuffers.length : 0;
    }
    
    @Override
    public final int getQueuedFrameCount() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return alBufferPlaying.size();
    }
    
    @Override
    public final int getFreeFrameCount() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return alBufferAvail.size();
    }
    
    @Override
    public final int getQueuedByteCount() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return alBufferBytesQueued;
    }
    
    @Override
    public final int getQueuedTime() {
        if( !initialized || null == chosenFormat ) {
            return 0;
        }
        return getQueuedTimeImpl(alBufferBytesQueued);
    }
    private final int getQueuedTimeImpl(int byteCount) {
        final int bytesPerSample = chosenFormat.sampleSize >>> 3; // /8
        return byteCount / ( chosenFormat.channelCount * bytesPerSample * ( chosenFormat.sampleRate / 1000 ) );        
    }
    
    @Override
    public final int getPTS() { return playingPTS; }
    
    private static final String toHexString(int v) { return "0x"+Integer.toHexString(v); }
    private static final String getThreadName() { return Thread.currentThread().getName(); }        
}
