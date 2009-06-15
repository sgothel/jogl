
package com.sun.openmax;

import javax.media.opengl.*;
import javax.media.opengl.glu.GLU;
import com.sun.opengl.util.texture.*;

import com.sun.opengl.impl.egl.EGL;
import com.sun.opengl.impl.egl.EGLContext;
import com.sun.opengl.impl.egl.EGLDrawable;
import com.sun.opengl.impl.egl.EGLExt;

import java.net.URL;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;

public class OMXInstance {
    private long moviePtr = 0;

    protected String path   = null;
    protected URL url   = null;
    
    // *** Texture impl
    protected Texture texture = null; // holds the last fetched texture

    protected float playSpeed = 1.0f;

    /** 
     * The following data is set by the setStream function,
     * and may be set by the native OMX implementation,
     * in case the stream attributes changes (see attributesUpdated)
     */
    protected int width = 0;
    protected int height = 0;
    protected int fps = 0; // frames per seconds
    protected long bps = 0; // bits per seconds
    protected long totalFrames = 0; // duration in frames
    protected String acodec = null;
    protected String vcodec = null;

    /**
     * Old stream values, before the last attributesUpdated)
     */
    protected int o_width = 0;
    protected int o_height = 0;
    protected int o_fps = 0; // frames per seconds
    protected long o_bps = 0; // bits per seconds
    protected long o_totalFrames = 0; // duration in frames
    
    static class EGLImageTexture {
        public EGLImageTexture(com.sun.opengl.util.texture.Texture t, long i, long s) {
            texture = t; image = i; sync = s;
        }
        public String toString() {
            return "EGLImageTexture[" + texture + ", image " + image + ", sync "+sync+"]";
        }
        protected com.sun.opengl.util.texture.Texture texture;
        protected long image;
        protected long sync;
    }
    private EGLImageTexture[] eglImgTexs=null;
    private HashMap eglImgTexsMap = new HashMap();
    protected int textureNum;
    
    private EGLExt eglExt = null;
    private long eglSurface = 0;
    private long eglDisplay = 0;
    private long eglContext = 0;
    private int sWidth=0, sHeight=0;

    private GL initGLData(GL gl) {
        if(null==gl) {
            throw new RuntimeException("No current GL");
        }
        EGLContext eglCtx = (EGLContext) gl.getContext();
        if(null==eglCtx) {
            throw new RuntimeException("No current EGL context");
        }
        EGLDrawable eglDrawable = (EGLDrawable) eglCtx.getGLDrawable();
        if(null==eglDrawable) {
            throw new RuntimeException("No valid drawable");
        }
        eglContext = eglCtx.getContext();
        eglDisplay = eglDrawable.getDisplay();
        eglSurface = eglDrawable.getSurface();
        eglExt = eglCtx.getEGLExt();
        if(null==eglExt) {
            throw new RuntimeException("No valid EGLExt");
        }

        int iTmp[] = new int[1];
        EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_WIDTH, iTmp, 0);
        sWidth=iTmp[0];
        EGL.eglQuerySurface(eglDisplay, eglSurface, EGL.EGL_HEIGHT, iTmp, 0);
        sHeight=iTmp[0];
        System.out.println("surface size: "+width+"x"+height);
        System.out.println(eglDrawable);
        System.out.println(eglCtx);
        System.out.println("EGL Extensions : "+EGL.eglQueryString(eglDisplay, EGL.EGL_EXTENSIONS));
        System.out.println("EGL CLIENT APIs: "+EGL.eglQueryString(eglDisplay, EGL.EGL_CLIENT_APIS));
        return gl;
    }

    public OMXInstance() {
        moviePtr = _createInstance();
        if(0==moviePtr) {
            throw new GLException("Couldn't create OMXInstance");
        }
    }
    native long _createInstance();

    public synchronized void setStream(int textureNum, URL u) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        this.textureNum=textureNum;
        url = u;
        if (url == null) {
            System.out.println("setURL (null)");
            stop();
            return;
        }
        path=null;
        if (url.getProtocol() == null || "file".equals(url.getProtocol())) {
            // CV only accepts absolute paths
            try {
                File file = new File(url.getPath());
                if (!file.exists()) {
                    throw new RuntimeException(new FileNotFoundException(file.toString()));
                }
                path = file.getCanonicalPath();
                System.out.println("setURL: path "+path);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
        path = replaceAll(path, "\\", "/").trim();
        if(null==path) {
            throw new RuntimeException("Couldn't parse stream URL: "+url);
        }
        System.out.println("setURL: clean path "+path);

        System.out.println("setURL: p1 "+this);
        _setStream(moviePtr, textureNum, path);
        System.out.println("setURL: p2 "+this);
    }
    native void _setStream(long moviePtr, int textureNum, String path);

    public synchronized void setStreamAllEGLImageTexture2D(GL gl) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        if(null==vcodec) {
            return;
        }
        gl = initGLData(gl);

        if(null!=eglImgTexs) {
            removeAllEGLImageTexture2D(gl);
        } else {
            eglImgTexs = new EGLImageTexture[textureNum];
        }

        int[] tmp = new int[1];
        int tex, e;

        errorCheckGL(gl, "i.1");
        gl.glEnable(gl.GL_TEXTURE_2D);
        errorCheckGL(gl, "i.2");

        for(int i=0; i<textureNum; i++) {
            String s0 = String.valueOf(i);
            gl.glGenTextures(1, tmp, 0);
            tex=tmp[0];
            if( (e=gl.glGetError()) != GL.GL_NO_ERROR || 0>tex ) {
                throw new RuntimeException("TextureName creation failed: "+e);
            }
            gl.glBindTexture(gl.GL_TEXTURE_2D, tex);

            // create space for buffer with a texture
            gl.glTexImage2D(
                    gl.GL_TEXTURE_2D, // target
                    0,                // level
                    gl.GL_RGBA,       // internal format
                    width,            // width
                    height,           // height
                    0,                // border
                    gl.GL_RGBA,       // format
                    gl.GL_UNSIGNED_BYTE, // type
                    null);            // pixels -- will be provided later
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MIN_FILTER, gl.GL_NEAREST);
            gl.glTexParameteri(gl.GL_TEXTURE_2D, gl.GL_TEXTURE_MAG_FILTER, gl.GL_NEAREST);

            long image=0, sync=0;

            // create EGLImage from texture
            tmp[0] = EGL.EGL_NONE;
            image = eglExt.eglCreateImageKHR(
                        eglDisplay,
                        eglContext,
                        eglExt.EGL_GL_TEXTURE_2D_KHR,
                        tex,
                        tmp, 0);
            if (0==image) {
                throw new RuntimeException("EGLImage creation failed: "+EGL.eglGetError()+", dpy "+eglDisplay+", ctx "+eglContext+", tex "+tex);
            }

            // Create sync object so that we can be sure that gl has finished
            // rendering the EGLImage texture before we tell OpenMAX to fill
            // it with a new frame.
            tmp[0] = EGL.EGL_NONE;
            sync = eglExt.eglCreateFenceSyncKHR(
                    eglDisplay,
                    eglExt.EGL_SYNC_PRIOR_COMMANDS_COMPLETE_KHR, tmp, 0);

            _setStreamEGLImageTexture2D(moviePtr, i, tex, image, sync);

            eglImgTexs[i] = new EGLImageTexture(
                com.sun.opengl.util.texture.TextureIO.newTexture(tex,
                                                                 javax.media.opengl.GL2.GL_TEXTURE_2D,
                                                                 width,
                                                                 height,
                                                                 width,
                                                                 height,
                                                                 true),
                image, sync);
            eglImgTexsMap.put(new Integer(tex), eglImgTexs[i]);
        }
        gl.glDisable(gl.GL_TEXTURE_2D);
    }
    native void _setStreamEGLImageTexture2D(long moviePtr, int i, int tex, long image, long sync);

    public synchronized void activateStream() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _activateStream(moviePtr);
    }
    native void _activateStream(long moviePtr);

    public synchronized void detachVideoRenderer() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _detachVideoRenderer(moviePtr);
    }
    native void _detachVideoRenderer(long moviePtr); // stop before

    public synchronized void attachVideoRenderer() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _attachVideoRenderer(moviePtr);
    }
    native void _attachVideoRenderer(long moviePtr); // detach before

    public synchronized void setPlaySpeed(float rate) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        _setPlaySpeed(moviePtr, rate);
        playSpeed = rate;
    }
    public synchronized float getPlaySpeed() {
        return playSpeed;
    }
    native void _setPlaySpeed(long moviePtr, float rate);

    /** @return time position after issuing the command */
    public synchronized float play() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _play(moviePtr);
    }
    native float _play(long moviePtr);

    /** @return time position after issuing the command */
    public synchronized float pause() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _pause(moviePtr);
    }
    native float _pause(long moviePtr);

    /** @return time position after issuing the command */
    public synchronized float stop() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _stop(moviePtr);
    }
    native float _stop(long moviePtr);

    /** @return time position after issuing the command */
    public synchronized float seek(float pos) {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _seek(moviePtr, pos);
    }
    native float _seek(long moviePtr, float position);

    public synchronized Texture getLastTextureID() {
        return texture;
    }
    public synchronized Texture getNextTextureID() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        texture=null;
        EGLImageTexture eglImgTex = (EGLImageTexture) eglImgTexsMap.get(new Integer(_getNextTextureID(moviePtr)));
        if(null!=eglImgTex) {
            texture = eglImgTex.texture;
        }
        return texture;
    }
    native int  _getNextTextureID(long moviePtr);

    public synchronized float getCurrentPosition() {
        if(0==moviePtr) {
            throw new GLException("OMX native instance null");
        }
        return _getCurrentPosition(moviePtr);
    }
    native float _getCurrentPosition(long moviePtr);

    public synchronized void destroy(GL gl) {
        removeAllEGLImageTexture2D(gl);
        if (moviePtr != 0) {
            long ptr = moviePtr;
            moviePtr = 0;
            _destroyInstance(ptr);

            eglExt=null;
            eglSurface=0;
            eglDisplay=0;
            eglContext=0;
        }
    }
    protected synchronized void finalize() {
        if (moviePtr != 0) {
            destroy(null);
        }
    }
    native void _destroyInstance(long moviePtr);

    public synchronized boolean isValid() {
        return (moviePtr != 0);
    }
    public synchronized String getPath() {
        return path;
    }
    public synchronized URL getURL() {
        return url;
    }
    public synchronized String getVideoCodec() {
        return vcodec;
    }
    public synchronized String getAudioCodec() {
        return acodec;
    }
    public synchronized long getTotalFrames() {
        return totalFrames;
    }
    public synchronized long getBitrate() {
        return bps;
    }
    public synchronized int getFramerate() {
        return fps;
    }
    public synchronized int getWidth() {
        return width;
    }
    public synchronized int getHeight() {
        return height;
    }
    public synchronized String toString() {
        return "OMXInstance [ stream [ video [ "+vcodec+", "+width+"x"+height+", "+fps+"fps, "+bps+"bsp, "+totalFrames+"f ] ] ]";
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
    }

    private void attributesUpdated() {
        int event_mask = 0;
        if( o_width != width || o_height != height ) {
            event_mask |= OMXEventListener.EVENT_CHANGE_SIZE;
        }   
        if( o_fps != fps ) {
            event_mask |= OMXEventListener.EVENT_CHANGE_FPS;
        }
        if( o_bps != bps ) {
            event_mask |= OMXEventListener.EVENT_CHANGE_BPS;
        }
        if( o_totalFrames != totalFrames ) {
            event_mask |= OMXEventListener.EVENT_CHANGE_LENGTH;
        }
        if(0==event_mask) {
            return;
        }

        ArrayList listeners = null;
        synchronized(this) {
            listeners = eventListeners;
        }
        for(Iterator i = listeners.iterator(); i.hasNext(); ) {
            OMXEventListener l = (OMXEventListener) i.next();
            l.changedAttributes(this, event_mask);
        }
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

    private void removeAllEGLImageTexture2D(GL gl) {
        if (moviePtr != 0) {
            if(null==eglExt) {
                throw new RuntimeException("No valid EGLExt");
            }

            texture = null;
            for(int i=0; i<textureNum; i++) {
                if(null!=eglImgTexs[i]) {
                    if(0!=eglImgTexs[i].image) {
                        eglExt.eglDestroyImageKHR(
                            eglDisplay,
                            eglImgTexs[i].image);
                    }
                    if(0!=eglImgTexs[i].sync) {
                        eglExt.eglDestroySyncKHR(eglImgTexs[i].sync);
                    }
                    if(null!=gl) {
                        eglImgTexs[i].texture.destroy(gl);
                    }
                    eglImgTexs[i]=null;
                }
            }
            eglImgTexsMap.clear();
        }
    }

    private void errorCheckGL(GL gl, String s) {
        int e;
        if( (e=gl.glGetError()) != GL.GL_NO_ERROR ) {
            System.out.println("GL Error: ("+s+"): "+e);
        }
    }

    private void errorCheckEGL(String s) {
        int e;
        if( (e=EGL.eglGetError()) != EGL.EGL_SUCCESS ) {
            System.out.println("EGL Error: ("+s+"): "+e);
        }
    }

    
    //
    // OMXEventListener Support
    //

    public synchronized void addEventListener(OMXEventListener l) {
        if(l == null) {
            return;
        }
        ArrayList newEventListeners = (ArrayList) eventListeners.clone();
        newEventListeners.add(l);
        eventListeners = newEventListeners;
    }

    public synchronized void removeEventListener(OMXEventListener l) {
        if (l == null) {
            return;
        }
        ArrayList newEventListeners = (ArrayList) eventListeners.clone();
        newEventListeners.remove(l);
        eventListeners = newEventListeners;
    }

    public synchronized OMXEventListener[] getEventListeners() {
        return (OMXEventListener[]) eventListeners.toArray();
    }

    private ArrayList eventListeners = new ArrayList();

}

