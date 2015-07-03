/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

import java.io.IOException;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLException;

import com.jogamp.opengl.egl.EGL;
import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.opengl.util.av.EGLMediaPlayerImpl;

/**
 * OpenMAX IL implementation. This implementation is currently not tested
 * due to lack of an available device or working <i>software</i> implementation.
 * It is kept alive through all changes in the hope of a later availability though.
 */
public class OMXGLMediaPlayer extends EGLMediaPlayerImpl {
    static final boolean available;

    static {
        available = false;
        /** FIXME!
        // OMX binding is included in jogl_desktop and jogl_mobile
        GLProfile.initSingleton();
        available = initIDs0(); */
    }

    public static final boolean isAvailable() { return available; }

    protected long moviePtr = 0;

    public OMXGLMediaPlayer() {
        super(TextureType.KHRImage, true);
        if(!available) {
            throw new RuntimeException("OMXGLMediaPlayer not available");
        }
        initOMX();
    }

    protected void initOMX() {
        moviePtr = _createInstance();
        if(0==moviePtr) {
            throw new GLException("Couldn't create OMXInstance");
        }
    }

    @Override
    protected TextureSequence.TextureFrame createTexImage(final GL gl, final int texName) {
        final EGLTextureFrame eglTex = (EGLTextureFrame) super.createTexImage(gl, texName);
        _setStreamEGLImageTexture2D(moviePtr, texName, eglTex.getImage(), eglTex.getSync());
        return eglTex;
    }

    @Override
    protected void destroyTexFrame(final GL gl, final TextureSequence.TextureFrame imgTex) {
        super.destroyTexFrame(gl, imgTex);
    }

    @Override
    protected void destroyImpl(final GL gl) {
        if (moviePtr != 0) {
            _stop(moviePtr);
            _detachVideoRenderer(moviePtr);
            _destroyInstance(moviePtr);
            moviePtr = 0;
        }
    }

    @Override
    protected void initStreamImpl(final int vid, final int aid) throws IOException {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        if( !getUri().isFileScheme() ) {
            throw new IOException("Only file schemes are allowed: "+getUri());
        }
        final String path=getUri().path.decode();
        if(DEBUG) {
            System.out.println("initGLStream: clean path "+path);
        }

        if(DEBUG) {
            System.out.println("initGLStream: p1 "+this);
        }
        _setStream(moviePtr, getTextureCount(), path);
        if(DEBUG) {
            System.out.println("initGLStream: p2 "+this);
        }
    }
    @Override
    protected final void initGLImpl(final GL gl) throws IOException, GLException {
        // NOP
        setIsGLOriented(true);
    }

    @Override
    protected int getAudioPTSImpl() {
        return 0!=moviePtr ? _getCurrentPosition(moviePtr) : 0;
    }

    @Override
    protected boolean setPlaySpeedImpl(final float rate) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _setPlaySpeed(moviePtr, rate);
        return true;
    }

    @Override
    public synchronized boolean playImpl() {
        if(0==moviePtr) {
            return false;
        }
        _play(moviePtr);
        return true;
    }

    /** @return time position after issuing the command */
    @Override
    public synchronized boolean pauseImpl() {
        if(0==moviePtr) {
            return false;
        }
        _pause(moviePtr);
        return true;
    }

    /** @return time position after issuing the command */
    @Override
    protected int seekImpl(final int msec) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _seek(moviePtr, msec);
    }

    @Override
    protected int getNextTextureImpl(final GL gl, final TextureFrame nextFrame) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        final int nextTex = _getNextTextureID(moviePtr, true);
        if(0 < nextTex) {
            // FIXME set pts !
            /* FIXME
            final TextureSequence.TextureFrame eglImgTex =
                    texFrameMap.get(new Integer(_getNextTextureID(moviePtr, blocking)));
            if(null!=eglImgTex) {
                lastTex = eglImgTex;
            } */
        }
        return 0; // FIXME: return pts
    }

    private String replaceAll(final String orig, final String search, final String repl) {
        final StringBuilder dest = new StringBuilder();
        // In case replaceAll / java.util.regex.* is not supported (-> CVM)
        int i=0,j;
        while((j=orig.indexOf(search, i))>=0) {
            dest.append(orig.substring(i, j));
            dest.append(repl);
            i=j+1;
        }
        return dest.append(orig.substring(i, orig.length())).toString();
    }

    private void errorCheckEGL(final String s) {
        int e;
        if( (e=EGL.eglGetError()) != EGL.EGL_SUCCESS ) {
            System.out.println("EGL Error: ("+s+"): 0x"+Integer.toHexString(e));
        }
    }

    private static native boolean initIDs0();
    private native long _createInstance();
    private native void _destroyInstance(long moviePtr);

    private native void _detachVideoRenderer(long moviePtr); // stop before
    private native void _attachVideoRenderer(long moviePtr); // detach before
    private native void _setStream(long moviePtr, int textureNum, String path);
    private native void _activateStream(long moviePtr);

    private native void _setStreamEGLImageTexture2D(long moviePtr, int tex, long image, long sync);
    private native int  _seek(long moviePtr, int position);
    private native void _setPlaySpeed(long moviePtr, float rate);
    private native void _play(long moviePtr);
    private native void _pause(long moviePtr);
    private native void _stop(long moviePtr);
    private native int  _getNextTextureID(long moviePtr, boolean blocking);
    private native int  _getCurrentPosition(long moviePtr);
}

