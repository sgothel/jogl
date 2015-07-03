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
package jogamp.opengl.util.av.impl;

import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/* pp */ abstract class FFMPEGNatives {

    private static final Object mutex_avcodec_openclose_jni = new Object();

    final boolean initSymbols0(final long[] symbols, final int count) {
        return initSymbols0(mutex_avcodec_openclose_jni, symbols, count);
    }
    abstract boolean initSymbols0(Object mutex_avcodec_openclose, long[] symbols, int count);
    abstract int getAvUtilMajorVersionCC0();
    abstract int getAvFormatMajorVersionCC0();
    abstract int getAvCodecMajorVersionCC0();
    abstract int getAvResampleMajorVersionCC0();
    abstract int getSwResampleMajorVersionCC0();

    abstract long createInstance0(FFMPEGMediaPlayer upstream, boolean enableAvResample, boolean enableSwResample, boolean verbose);
    abstract void destroyInstance0(long moviePtr);

    /**
     * Issues {@link #updateAttributes(int, int, int, int, int, int, int, float, int, int, String, String)}
     * and {@link #updateAttributes2(int, int, int, int, int, int, int, int, int, int)}.
     *
     * @param moviePtr
     * @param url
     * @param vid
     * @param sizes requested video size as string, i.e. 'hd720'. May be null to favor vWidth and vHeight.
     * @param vWidth requested video width (for camera mode)
     * @param vHeight requested video width (for camera mode)
     * @param vRate requested video framerate (for camera mode)
     * @param aid
     * @param aPrefSampleRate
     * @param aPrefChannelCount
     */
    abstract void setStream0(long moviePtr, String url, boolean isCameraInput,
                             int vid, String sizes, int vWidth, int vHeight,
                             int vRate, int aid, int aMaxChannelCount, int aPrefSampleRate);

    abstract void setGLFuncs0(long moviePtr, long procAddrGLTexSubImage2D, long procAddrGLGetError, long procAddrGLFlush, long procAddrGLFinish);

    abstract int getVideoPTS0(long moviePtr);

    abstract int getAudioPTS0(long moviePtr);

    /**
     * @return resulting current video PTS, or {@link TextureFrame#INVALID_PTS}
     */
    abstract int readNextPacket0(long moviePtr, int texTarget, int texFmt, int texType);

    abstract int play0(long moviePtr);
    abstract int pause0(long moviePtr);
    abstract int seek0(long moviePtr, int position);
}