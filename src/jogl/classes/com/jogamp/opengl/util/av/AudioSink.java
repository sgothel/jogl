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
package com.jogamp.opengl.util.av;

import java.nio.ByteBuffer;

import com.jogamp.opengl.util.TimeFrameI;

import jogamp.opengl.Debug;

public interface AudioSink {
    public static final boolean DEBUG = Debug.debug("AudioSink");
    
    /** Default frame duration in millisecond, i.e. 1 frame per {@value} ms. */
    public static final int DefaultFrameDuration = 32;
        
    /** Initial audio queue size in milliseconds. {@value} ms, i.e. 16 frames per 32 ms. See {@link #init(AudioDataFormat, float, int, int, int)}.*/
    public static final int DefaultInitialQueueSize = 16 * 32; // 512 ms
    /** Audio queue grow size in milliseconds. {@value} ms, i.e. 16 frames per 32 ms. See {@link #init(AudioDataFormat, float, int, int, int)}.*/
    public static final int DefaultQueueGrowAmount = 16 * 32; // 512 ms
    /** Audio queue limit w/ video in milliseconds. {@value} ms, i.e. 96 frames per 32 ms. See {@link #init(AudioDataFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitWithVideo =  96 * 32; // 3072 ms
    /** Audio queue limit w/o video in milliseconds. {@value} ms, i.e. 32 frames per 32 ms. See {@link #init(AudioDataFormat, float, int, int, int)}.*/
    public static final int DefaultQueueLimitAudioOnly =  32 * 32; // 1024 ms
    
    /** Specifies the audio data type. Currently only PCM is supported. */
    public static enum AudioDataType { PCM };
    
    /**
     * Specifies the audio data format.
     */
    public static class AudioDataFormat {
        public AudioDataFormat(AudioDataType dataType, int sampleRate, int sampleSize, int channelCount, boolean signed, boolean fixedP, boolean littleEndian) {
            this.dataType = dataType;
            this.sampleRate = sampleRate;
            this.sampleSize = sampleSize;
            this.channelCount = channelCount;
            this.signed = signed;
            this.fixedP = fixedP;
            this.littleEndian = littleEndian;
        }
        /** Audio data type. */
        public final AudioDataType dataType;
        /** Sample rate in Hz (1/s). */
        public final int sampleRate;
        /** Sample size in bits. */
        public final int sampleSize;
        /** Number of channels. */
        public final int channelCount;
        public final boolean signed;
        /** Fixed or floating point values. Floating point 'float' has {@link #sampleSize} 32, 'double' has {@link #sampleSize} 64, */
        public final boolean fixedP;
        public final boolean littleEndian;
        
        /** 
         * Returns the byte size of the given milliseconds 
         * according to {@link #sampleSize}, {@link #channelCount} and {@link #sampleRate}. 
         */
        public final int getByteSize(int millisecs) {
            final int bytesPerSample = sampleSize >>> 3; // /8
            return millisecs * ( channelCount * bytesPerSample * ( sampleRate / 1000 ) );
        }
        
        /** 
         * Returns the duration in milliseconds of the given byte count 
         * according to {@link #sampleSize}, {@link #channelCount} and {@link #sampleRate}. 
         */
        public final int getBytesDuration(int byteCount) {
            final int bytesPerSample = sampleSize >>> 3; // /8
            return byteCount / ( channelCount * bytesPerSample * ( sampleRate / 1000 ) );        
        }
        
        /**
         * Returns the duration in milliseconds of the given and sample count per frame and channel
         * according to the {@link #sampleRate}, i.e.
         * <pre>
         *    ( 1000f * sampleCount ) / sampleRate
         * </pre>
         * @param sampleCount sample count per frame and channel
         */
        public final float getSamplesDuration(int sampleCount) {
            return ( 1000f * (float) sampleCount ) / (float)sampleRate;
        }
        
        /**
         * Returns the rounded frame count of the given milliseconds and frame duration.
         * <pre>
         *     Math.max( 1, millisecs / frameDuration + 0.5f )
         * </pre>
         * <p>
         * Note: <code>frameDuration</code> can be derived by <i>sample count per frame and channel</i>
         * via {@link #getSamplesDuration(int)}.
         * </p>
         * @param millisecs time in milliseconds
         * @param frameDuration duration per frame in milliseconds.
         */
        public final int getFrameCount(int millisecs, float frameDuration) {
            return Math.max(1, (int) ( (float)millisecs / frameDuration + 0.5f ));
        }
        
        /**
         * Returns the byte size of given sample count
         * according to the {@link #sampleSize}, i.e.: 
         * <pre>
         *  sampleCount * ( sampleSize / 8 )
         * </pre>
         * <p>
         * Note: To retrieve the byte size for all channels, you need to pre-multiply <code>sampleCount</code>
         * with {@link #channelCount}.
         * </p>
         * @param sampleCount sample count
         */
        public final int getSamplesByteSize(int sampleCount) {
            return sampleCount * ( sampleSize >>> 3 );
        }
        
        public String toString() { 
            return "AudioDataFormat[type "+dataType+", sampleRate "+sampleRate+", sampleSize "+sampleSize+", channelCount "+channelCount+
                   ", signed "+signed+", fixedP "+fixedP+", "+(littleEndian?"little":"big")+"endian]"; }
    }
    /** Default {@link AudioDataFormat}, [type PCM, sampleRate 44100, sampleSize 16, channelCount 2, signed, fixedP, littleEndian]. */    
    public static final AudioDataFormat DefaultFormat = new AudioDataFormat(AudioDataType.PCM, 44100, 16, 2, true /* signed */, true /* fixed point */, true /* littleEndian */);
    
    public static abstract class AudioFrame extends TimeFrameI {
        protected int byteSize;
        
        public AudioFrame() {
            this.byteSize = 0;
        }
        public AudioFrame(int pts, int duration, int byteCount) {
            super(pts, duration);
            this.byteSize=byteCount;
        }
        
        /** Get this frame's size in bytes. */
        public final int getByteSize() { return byteSize; }
        /** Set this frame's size in bytes. */
        public final void setByteSize(int size) { this.byteSize=size; }
        
        public String toString() { 
            return "AudioFrame[pts " + pts + " ms, l " + duration + " ms, "+byteSize + " bytes]";
        }
    }
    public static class AudioDataFrame extends AudioFrame {
        protected final ByteBuffer data;
        
        public AudioDataFrame(int pts, int duration, ByteBuffer bytes, int byteCount) {
            super(pts, duration, byteCount);
            if( byteCount > bytes.remaining() ) {
                throw new IllegalArgumentException("Give size "+byteCount+" exceeds remaining bytes in ls "+bytes+". "+this);
            }
            this.data=bytes;
        }
        
        /** Get this frame's data. */
        public final ByteBuffer getData() { return data; }
        
        public String toString() { 
            return "AudioDataFrame[pts " + pts + " ms, l " + duration + " ms, "+byteSize + " bytes, " + data + "]";
        }
    }
    
    /** 
     * Returns the <code>initialized state</code> of this instance.
     * <p>
     * The <code>initialized state</code> is affected by this instance
     * overall availability, i.e. after instantiation,
     * as well as by {@link #destroy()}.
     * </p> 
     */
    public boolean isInitialized();

    /** Returns the playback speed. */
    public float getPlaySpeed();
    
    /** 
     * Sets the playback speed.
     * <p>
     * Play speed is set to <i>normal</i>, i.e. <code>1.0f</code>
     * if <code> abs(1.0f - rate) < 0.01f</code> to simplify test.
     * </p>
     * @return true if successful, otherwise false, i.e. due to unsupported value range of implementation. 
     */
    public boolean setPlaySpeed(float s);
    
    /** 
     * Returns the preferred {@link AudioDataFormat} by this sink.
     * <p>
     * The preferred format shall reflect this sinks most native format,
     * i.e. best performance w/o data conversion. 
     * </p>
     * @see #initSink(AudioDataFormat) 
     */
    public AudioDataFormat getPreferredFormat();
    
    /**
     * Initializes the sink.
     * <p>
     * Implementation shall try to match the given <code>requestedFormat</code> {@link AudioDataFormat}
     * as close as possible, regarding it's capabilities.
     * </p>
     * <p>
     * A user may consider {@link #getPreferredFormat()} and pass this value
     * to utilize best performance and <i>behavior</i>. 
     * </p>
     * The {@link #DefaultFormat} <i>should be</i> supported by all implementations.
     * </p>
     * @param requestedFormat the requested {@link AudioDataFormat}. 
     * @param frameDuration average or fixed frame duration in milliseconds
     *                      helping a caching {@link AudioFrame} based implementation to determine the frame count in the queue.
     *                      See {@link #DefaultFrameDuration}.
     * @param initialQueueSize initial time in milliseconds to queue in this sink, see {@link #DefaultInitialQueueSize}.
     * @param queueGrowAmount time in milliseconds to grow queue if full, see {@link #DefaultQueueGrowAmount}.
     * @param queueLimit maximum time in milliseconds the queue can hold (and grow), see {@link #DefaultQueueLimitWithVideo} and {@link #DefaultQueueLimitAudioOnly}.
     * @return if successful the chosen AudioDataFormat based on the <code>requestedFormat</code> and this sinks capabilities, otherwise <code>null</code>.
     */
    public AudioDataFormat init(AudioDataFormat requestedFormat, float frameDuration, 
                                int initialQueueSize, int queueGrowAmount, int queueLimit);
    
    /**
     * Returns true, if {@link #play()} has been requested <i>and</i> the sink is still playing,
     * otherwise false.
     */
    public boolean isPlaying();
    
    /** 
     * Play buffers queued via {@link #enqueueData(AudioFrame)} from current internal position.
     * If no buffers are yet queued or the queue runs empty, playback is being continued when buffers are enqueued later on.
     * @see #enqueueData(AudioFrame)
     * @see #pause() 
     */
    public void play();
    
    /** 
     * Pause playing buffers while keeping enqueued data incl. it's internal position.
     * @see #play()
     * @see #flush()
     * @see #enqueueData(AudioFrame)
     */
    public void pause();
    
    /**
     * Flush all queued buffers, implies {@link #pause()}.
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     * @see #play()
     * @see #pause()
     * @see #enqueueData(AudioFrame)
     */
    public void flush();
    
    /** Destroys this instance, i.e. closes all streams and devices allocated. */
    public void destroy();
    
    /** 
     * Returns the number of allocated buffers as requested by 
     * {@link #init(AudioDataFormat, float, int, int, int)}.
     */
    public int getFrameCount();

    /** @return the current enqueued frames count since {@link #init(AudioDataFormat, float, int, int, int)}. */
    public int getEnqueuedFrameCount();
    
    /** 
     * Returns the current number of frames queued for playing.
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     */
    public int getQueuedFrameCount();
    
    /** 
     * Returns the current number of bytes queued for playing.
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     */
    public int getQueuedByteCount();

    /** 
     * Returns the current queued frame time in milliseconds for playing.
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     */
    public int getQueuedTime();
    
    /** 
     * Return the current audio presentation timestamp (PTS) in milliseconds.
     */
    public int getPTS();
    
    /** 
     * Returns the current number of frames in the sink available for writing.
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     */
    public int getFreeFrameCount();
    
    /** 
     * Enqueue the remaining bytes of the given {@link AudioDataFrame}'s direct ByteBuffer to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioDataFormat} as returned by {@link #initSink(AudioDataFormat)}.
     * </p>
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     * @returns the enqueued internal {@link AudioFrame}, which may differ from the input <code>audioDataFrame</code>.
     * @deprecated User shall use {@link #enqueueData(int, ByteBuffer, int)}, which allows implementation
     *             to reuse specialized {@link AudioFrame} instances.
     */
    public AudioFrame enqueueData(AudioDataFrame audioDataFrame);
    
    /** 
     * Enqueue <code>byteCount</code> bytes of the remaining bytes of the given NIO {@link ByteBuffer} to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioDataFormat} as returned by {@link #initSink(AudioDataFormat)}.
     * </p>
     * <p>
     * {@link #init(AudioDataFormat, float, int, int, int)} must be called first.
     * </p>
     * @returns the enqueued internal {@link AudioFrame}.
     */
    public AudioFrame enqueueData(int pts, ByteBuffer bytes, int byteCount);
}
