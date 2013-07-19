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

import jogamp.opengl.Debug;

public interface AudioSink {
    public static final boolean DEBUG = Debug.debug("AudioSink");
    
    /** Specifies the audio data type. Currently only PCM is supported. */
    public static enum AudioDataType { PCM };
    
    /**
     * Specifies the audio data format.
     */
    public static class AudioDataFormat {
        public AudioDataFormat(AudioDataType dataType, int sampleRate, int sampleSize, int channelCount, boolean signed, boolean littleEndian) {
            this.dataType = dataType;
            this.sampleRate = sampleRate;
            this.sampleSize = sampleSize;
            this.channelCount = channelCount;
            this.signed = signed;
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
        public final boolean littleEndian;
        
        public String toString() { 
            return "AudioDataFormat[type "+dataType+", sampleRate "+sampleRate+", sampleSize "+sampleSize+", channelCount "+channelCount+
                   ", signed "+signed+", "+(littleEndian?"little":"big")+"endian]"; }
    }
    /** Default {@link AudioDataFormat}, [type PCM, sampleRate 44100, sampleSize 16, channelCount 2, signed, littleEndian]. */    
    public static final AudioDataFormat DefaultFormat = new AudioDataFormat(AudioDataType.PCM, 44100, 16, 2, true /* signed */, true /* littleEndian */);
    
    public static class AudioFrame {
        public final ByteBuffer data;
        public final int dataSize;
        public final int audioPTS;
        
        public AudioFrame(ByteBuffer data, int dataSize, int audioPTS) {
            if( dataSize > data.remaining() ) {
                throw new IllegalArgumentException("Give size "+dataSize+" exceeds remaining bytes in ls "+data+". "+this);
            }
            this.data=data;
            this.dataSize=dataSize;
            this.audioPTS=audioPTS;
        }
        
        public String toString() { return "AudioFrame[apts "+audioPTS+", data "+data+", payloadSize "+dataSize+"]"; }
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
     * @param bufferCount number of buffers for sink
     * @return if successful the chosen AudioDataFormat based on the <code>requestedFormat</code> and this sinks capabilities, otherwise <code>null</code>. 
     */
    public AudioDataFormat initSink(AudioDataFormat requestedFormat, int bufferCount);

    
    /** Destroys this instance, i.e. closes all streams and devices allocated. */
    public void destroy();
    
    /** 
     * Returns the number of bytes queued for playing.
     * <p>
     * {@link #initSink(AudioDataFormat)} must be called first.
     * </p>
     */
    public int getQueuedByteCount();

    /** 
     * Returns the queued buffer time in milliseconds for playing.
     * <p>
     * {@link #initSink(AudioDataFormat)} must be called first.
     * </p>
     */
    public int getQueuedTime();
    
    /** 
     * Returns the number of buffers in the sink available for writing.
     * <p>
     * {@link #initSink(AudioDataFormat)} must be called first.
     * </p>
     */
    public int getWritableBufferCount();
    
    /** 
     * Returns true if data is available to be written in the sink.
     * <p>
     * {@link #initSink(AudioDataFormat)} must be called first.
     * </p>
     */
    public boolean isDataAvailable(int data_size);

    /** 
     * Writes the remaining bytes of the given direct ByteBuffer to this sink.
     * <p>
     * The data must comply with the chosen {@link AudioDataFormat} as returned by {@link #initSink(AudioDataFormat)}.
     * </p>
     */
    public void writeData(AudioFrame audioFrame);
}
