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

class FFMPEGv08Natives implements FFMPEGNatives {
    @Override
    public native boolean initSymbols0(long[] symbols, int count);

    @Override
    public native int getAvUtilMajorVersionCC0();

    @Override
    public native int getAvFormatMajorVersionCC0();

    @Override
    public native int getAvCodecMajorVersionCC0();

    @Override
    public native int getAvResampleMajorVersionCC0();

    @Override
    public native int getSwResampleMajorVersionCC0();
    
    @Override
    public native boolean initIDs0();

    @Override
    public native long createInstance0(FFMPEGMediaPlayer upstream, boolean verbose);

    @Override
    public native void destroyInstance0(long moviePtr);

    @Override
    public native void setStream0(long moviePtr, String url, boolean isCameraInput, int vid, String sizes, int vWidth, int vHeight, int vRate, int aid, int aMaxChannelCount, int aPrefSampleRate);

    @Override
    public native void setGLFuncs0(long moviePtr, long procAddrGLTexSubImage2D, long procAddrGLGetError, long procAddrGLFlush, long procAddrGLFinish);

    @Override
    public native int getVideoPTS0(long moviePtr);

    @Override
    public native int getAudioPTS0(long moviePtr);

    @Override
    public native int readNextPacket0(long moviePtr, int texTarget, int texFmt, int texType);

    @Override
    public native int play0(long moviePtr);

    @Override
    public native int pause0(long moviePtr);

    @Override
    public native int seek0(long moviePtr, int position);
}
