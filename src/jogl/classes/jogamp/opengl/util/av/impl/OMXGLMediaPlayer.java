
package jogamp.opengl.util.av.impl;

import java.io.IOException;
import java.net.URL;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.opengl.egl.EGL;
import jogamp.opengl.util.av.EGLMediaPlayerImpl;

public class OMXGLMediaPlayer extends EGLMediaPlayerImpl {
    static final boolean available;
    
    static {
        // OMX binding is included in jogl_desktop and jogl_mobile     
        GLProfile.initSingleton();
        available = initIDs0();
    }
    
    public static final boolean isAvailable() { return available; }
    
    protected long moviePtr = 0;
    
    protected TextureSequence.TextureFrame lastTex = null;

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
    protected TextureSequence.TextureFrame createTexImage(GL gl, int idx, int[] tex) {
        final EGLTextureFrame eglTex = (EGLTextureFrame) super.createTexImage(gl, idx, tex);
        _setStreamEGLImageTexture2D(moviePtr, idx, tex[idx], eglTex.getImage(), eglTex.getSync());
        lastTex = eglTex;
        return eglTex;
    }
    
    @Override
    protected void destroyTexImage(GL gl, TextureSequence.TextureFrame imgTex) {
        lastTex = null;
        super.destroyTexImage(gl, imgTex);        
    }
    
    @Override
    protected void destroyImpl(GL gl) {
        _detachVideoRenderer(moviePtr);
        if (moviePtr != 0) {
            _destroyInstance(moviePtr);
            moviePtr = 0;
        }
    }
    
    @Override
    protected void initGLStreamImpl(GL gl, int[] texNames) throws IOException {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        final URL url = urlConn.getURL();
        if(!url.getProtocol().equals("file")) {
            throw new IOException("Only file URLs are allowed: "+url);            
        }
        final String path=url.getPath();
        System.out.println("setURL: clean path "+path);
    
        System.out.println("setURL: p1 "+this);
        _setStream(moviePtr, textureCount, path);
        System.out.println("setURL: p2 "+this);        
    }
    
    @Override
    protected int getCurrentPositionImpl() {
        return 0!=moviePtr ? _getCurrentPosition(moviePtr) : 0;
    }

    @Override
    protected boolean setPlaySpeedImpl(float rate) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _setPlaySpeed(moviePtr, rate);
        return true;
    }

    @Override
    public synchronized boolean startImpl() {
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
    public synchronized boolean stopImpl() {
        if(0==moviePtr) {
            return false;
        }
        _stop(moviePtr);
        return true;
    }

    /** @return time position after issuing the command */
    @Override
    protected int seekImpl(int msec) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _seek(moviePtr, msec);
    }

    @Override
    protected TextureSequence.TextureFrame getLastTextureImpl() {
        return lastTex;
    }
    
    @Override
    protected TextureSequence.TextureFrame getNextTextureImpl(GL gl, boolean blocking) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        final int nextTex = _getNextTextureID(moviePtr, blocking);
        if(0 < nextTex) {
            final TextureSequence.TextureFrame eglImgTex = texFrameMap.get(new Integer(_getNextTextureID(moviePtr, blocking)));
            if(null!=eglImgTex) {
                lastTex = eglImgTex;
            }
        }
        return lastTex;
    }
    
    private String replaceAll(String orig, String search, String repl) {
        String dest=null;
        // In case replaceAll / java.util.regex.* is not supported (-> CVM)
        int i=0,j;
        dest = new String();
        while((j=orig.indexOf(search, i))>=0) {
            dest=dest.concat(orig.substring(i, j));
            dest=dest.concat(repl);
            i=j+1;
        }
        return dest.concat(orig.substring(i, orig.length()));
    }

    private void errorCheckEGL(String s) {
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
    
    private native void _setStreamEGLImageTexture2D(long moviePtr, int i, int tex, long image, long sync);
    private native int  _seek(long moviePtr, int position);
    private native void _setPlaySpeed(long moviePtr, float rate);
    private native void _play(long moviePtr);
    private native void _pause(long moviePtr);
    private native void _stop(long moviePtr);
    private native int  _getNextTextureID(long moviePtr, boolean blocking);
    private native int  _getCurrentPosition(long moviePtr);
}

