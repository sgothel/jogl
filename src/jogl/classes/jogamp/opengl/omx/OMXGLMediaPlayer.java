
package jogamp.opengl.omx;

import java.io.IOException;
import java.net.URL;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import com.jogamp.opengl.util.texture.TextureSequence;

import jogamp.opengl.egl.EGL;
import jogamp.opengl.util.av.EGLMediaPlayerImpl;

public class OMXGLMediaPlayer extends EGLMediaPlayerImpl {
    protected long moviePtr = 0;
    
    /**
     * Old stream values, before the last attributesUpdated)
     */
    protected int o_width = 0;
    protected int o_height = 0;
    protected int o_fps = 0;
    protected long o_bps = 0;
    protected long o_totalFrames = 0;
    protected long o_duration = 0;
        
    protected TextureSequence.TextureFrame lastTex = null;

    public OMXGLMediaPlayer() {
        super(TextureType.KHRImage, true);
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
    public synchronized long getCurrentPosition() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _getCurrentPosition(moviePtr);
    }

    @Override
    public synchronized void setPlaySpeed(float rate) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _setPlaySpeed(moviePtr, rate);
        playSpeed = rate;
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
    protected long seekImpl(long msec) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _seek(moviePtr, msec);
    }

    @Override
    public TextureSequence.TextureFrame getLastTexture() {
        return lastTex;
    }
    
    @Override
    public synchronized TextureSequence.TextureFrame getNextTexture(GL gl, boolean blocking) {
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
    
    protected void attributesUpdated() {
        int event_mask = 0;
        if( o_width != width || o_height != height ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_SIZE;
        }   
        if( o_fps != fps ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_FPS;
        }
        if( o_bps != bps ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_BPS;
        }
        if( o_totalFrames != totalFrames ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_LENGTH;
        }
        if(0==event_mask) {
            return;
        }
        super.attributesUpdated(event_mask);    
    }

    /**
     * Java callback method issued by the native OMX backend
     */
    private void saveAttributes() {
        o_width = width;
        o_height = height;
        o_fps = fps;
        o_bps = bps;
        o_totalFrames = totalFrames;
        o_duration = duration;
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

    //
    // OMXEventListener Support
    //

    native long _createInstance();    
    native void _destroyInstance(long moviePtr);
    
    native void _detachVideoRenderer(long moviePtr); // stop before
    native void _attachVideoRenderer(long moviePtr); // detach before
    native void _setStream(long moviePtr, int textureNum, String path);
    native void _activateStream(long moviePtr);
    
    native void _setStreamEGLImageTexture2D(long moviePtr, int i, int tex, long image, long sync);
    native long _seek(long moviePtr, long position);
    native void _setPlaySpeed(long moviePtr, float rate);
    native void _play(long moviePtr);
    native void _pause(long moviePtr);
    native void _stop(long moviePtr);
    native int  _getNextTextureID(long moviePtr, boolean blocking);
    native long _getCurrentPosition(long moviePtr);
}

