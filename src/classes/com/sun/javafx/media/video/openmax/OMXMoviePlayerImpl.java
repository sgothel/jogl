
package com.sun.javafx.media.video.openmax;

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

public class OMXMoviePlayerImpl /* extends MoviePlayerImpl */ {

    static final public int PREVIOUS = -1;
    static final public int NEXT = 1;

    public boolean isTransparent() {
        return false;
    }
    
    long totalFrames = 0;
    int width = 0;
    int height = 0;
    float fps = 0.0f;
    
    int pixelBytes = 0;
    int pixelType = 0;
    
    EGLExt eglExt = null;
    long eglSurface=0;
    long eglDisplay=0;
    long eglContext=0;
    int sWidth=0, sHeight=0;
    int textureNum = 4;

    static class EGLImageTexture {
        public EGLImageTexture(com.sun.opengl.util.texture.Texture t, long i, long s) {
            System.out.println("EGLImageTexture " + t + ", image " + i + ", sync "+s);
            texture = t; image = i; sync = s;
        }
        protected com.sun.opengl.util.texture.Texture texture;
        protected long image;
        protected long sync;
    }
    EGLImageTexture[] eglImgTexs=null;
    HashMap eglImgTexsMap = new HashMap();
    
    long moviePtr = 0;
    String path = null;
    URL url;
    String name = null;
    String nameClean = null;
    int loopCount = 0;
    long position = 0;
    float volume = 1.0f;
    float rate = 1.0f;
    boolean playing = false;
    boolean paused = false;
    
    native long _createInstance(int numTextures);
    native void _setStream(long moviePtr, String path);
    native void _updateStreamInfo(long moviePtr);
    native void _setEGLImageTexture2D(long moviePtr, int i, int tex, long image, long sync);
    native void _activateInstance(long moviePtr);
    native void _deactivateInstance(long moviePtr);

    native void _play(long moviePtr, long position, float rate, int loopCount);
    native long _stop(long moviePtr);
    native void _setVolume(long moviePtr, float volume);
    native long _getCurrentPosition(long moviePtr);
    native long _setCurrentPosition(long moviePtr, long position);
    native long _getCurrentLoaded(long moviePtr);
    native long _step(long moviePtr, int direction, long position);
    native void _setRate(long moviePtr, float rate);
    native int _getTextureID(long moviePtr);
    native int _getWidth(long moviePtr);
    native int _getHeight(long moviePtr);
    native long _getDuration(long moviePtr);

    native void _destroyInstance(long moviePtr);
    
    public OMXMoviePlayerImpl() {
        System.out.println("OMXMoviePlayerImpl (null)");
    }

    public OMXMoviePlayerImpl(URL url) {
        System.out.println("OMXMoviePlayerImpl ("+url+")");
        setURL(url);
    }
    
    public void update() {
        update(null);
    }

    /**
    public int moduleTest() {
        return _moduleTest();
    }
    native int _moduleTest();
    */

    public void update(GL gl) {
        if (moviePtr == 0) {
            return;
        }
        System.out.println("update: totalFrames "+totalFrames);
        totalFrames = _getDuration(moviePtr);
        int w = _getWidth(moviePtr);
        int h = _getHeight(moviePtr);
        if (h != height || w!=width) {
            System.out.println("update: size change "+width+"x"+height+" -> "+w+"x"+h);
            width = w;
            height = h;
            // FIXME: TODO
            //   [1] EventListener model to notify clients of dimension change
            //   [2] Reallocate ressources (Textures, EGLImage, ..)
            if(null==gl) {
                gl = GLU.getCurrentGL();
            }
            try { 
                setAllEGLImageTexture2D(gl);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        } else {
            System.out.println("update: none");
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

    public synchronized void setURL(URL u) {
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

        GL gl = initGLData();

        if(0!=moviePtr) {
           dispose();
        }
        moviePtr = _createInstance(textureNum);
        if (moviePtr == 0) {
            throw new RuntimeException("Couldn't establish native playback: "+url);
        }
        _setStream(moviePtr, path);
        width  = _getWidth(moviePtr);
        height = _getHeight(moviePtr);
        if(width==0 || height==0) {
            throw new RuntimeException("Illegal media dimension "+width+"x"+height);
        }
        System.out.println("movie size: "+width+"x"+height);

        setAllEGLImageTexture2D(gl);

        _activateInstance(moviePtr);

        position = 0;
    }

    private GL initGLData() {
        GL gl = GLU.getCurrentGL();
        if(null==gl) {
            throw new RuntimeException("No current GL");
        }
        /*
        if(gl.isGLES2()) {
            GLES2 gles2 = gl.getGLES2();

            // Debug ..
            //DebugGLES2 gldbg = new DebugGLES2(gles2);
            //gles2.getContext().setGL(gldbg);
            //gles2 = gldbg;

            // Trace ..
            TraceGLES2 gltrace = new TraceGLES2(gles2, System.out);
            gles2.getContext().setGL(gltrace);
            gl = gltrace;
        }*/
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

    private void removeAllEGLImageTexture2D() {
        System.out.println("removeAllEGLImg: mp "+moviePtr+", textureNum "+textureNum);
        if (moviePtr != 0) {
            texture = null;
            for(int i=0; i<textureNum; i++) {
                eglExt.eglDestroyImage(
                    eglDisplay,
                    eglImgTexs[i].image);
                eglExt.eglDestroySync(eglImgTexs[i].sync);
                // JAU eglImgTexs[i].texture.dispose();
                eglImgTexs[i]=null;
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

    private void setAllEGLImageTexture2D(GL gl) {
        if (moviePtr == 0) {
            return;
        }
        if(null!=eglImgTexs) {
            removeAllEGLImageTexture2D();
        } else {
            eglImgTexs = new EGLImageTexture[textureNum];
        }

        int[] tmp = new int[1];
        int tex, e;

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
            image = eglExt.eglCreateImage(
                        eglDisplay,
                        eglContext,
                        eglExt.EGL_GL_TEXTURE_2D,
                        tex,
                        tmp, 0);
            if (0==image) {
                throw new RuntimeException("EGLImage creation failed: "+EGL.eglGetError());
            }

            // Create sync object so that we can be sure that gl has finished
            // rendering the EGLImage texture before we tell OpenMAX to fill
            // it with a new frame.
            tmp[0] = EGL.EGL_NONE;
            sync = eglExt.eglCreateFenceSync(
                    eglDisplay,
                    eglExt.EGL_SYNC_PRIOR_COMMANDS_COMPLETE, tmp, 0);

            _setEGLImageTexture2D(moviePtr, i, tex, image, sync);

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
    }
    
    public boolean isValid() {
        return (moviePtr != 0);
    }
    
    public String getPath() {
        return path;
    }

    public URL getURL() {
        return url;
    }
    
    // start playing from the beginning (user frame)
    public void play() {
        paused = false;
        if (moviePtr == 0) {
            return;
        }
        playing = true;
        _play(moviePtr, position, rate, loopCount);
    }

    public boolean isPlaying() {
        return playing;
    }    

    public long pause() {
        paused = true;
        if (moviePtr == 0) {
            return 0;
        }
        if (height != 0 && width != 0) {
            position = stop();
        }
        return position;
    }

    // returns the last played frame
    public long stop() {
        if (moviePtr == 0) {
            return 0;
        }
        playing = false;
        position = _stop(moviePtr);
        return position;
    }

    public long step(int direction) {
        if (moviePtr == 0) {
            return 0;
        }
        if (isPlaying()) {
            stop();
        }
        position = _step(moviePtr, direction, position);
        return position;
    }
	
    // 0 (no volume) to 1 (max volume) 
    public void setVolume(float volume) {
        if (moviePtr == 0) {
            return;
        }
        
        if (volume < 0.0f) {
            volume = 0.0f;
        }
        volume = volume;
        _setVolume(moviePtr, volume);
    }

    public float getVolume() {
        return volume;
    }
	
    public void setLoopCount(int loopCount) {
        loopCount = loopCount;
    }

    public int getLoopCount()  {
        return loopCount;
    }
    
    // in frames
    public void setPosition(long position)  {
        if (position < 0) {
            position = 0;
        } else if (position <= totalFrames) {
            position = position;
        }
        else {
            position = 0;
        }
        _setCurrentPosition(moviePtr, position);
    }

    public long getLoaded() {
        if (moviePtr == 0) {
            return 0;
        } 
        return _getCurrentLoaded(moviePtr);
    }
	
    public long getPosition() {
        if (moviePtr == 0) {
            position = 0;
        } else if (playing == true) {
            long pos = _getCurrentPosition(moviePtr);
            //System.out.println("getPosition " + pos + " " + position);
            position = pos;
        } 
        return position;
    }

    public long getEndPosition() {
        return totalFrames;
    }

    public void setRate(float rate) {
        setPlaybackRate(rate);
    }

    public float getRate() {
        return getPlaybackRate();
    }
    
    // -1.0f    - backwards normal speed
    // 0.5f     - 1/2x speed
    // 1.0f     - normal speed
    // 2.0f     - 2x speed
    public void setPlaybackRate(float r) {
        if (moviePtr == 0) {
            return;
        }
        rate = r;
        _setRate(moviePtr, rate);
    }
	
    public float getPlaybackRate() {
        return rate;
    }
    
    public int getWidth() {
        return width;
    }
    
    public int getHeight() {
        return height;
    }

    
    public synchronized void dispose() {
        removeAllEGLImageTexture2D();
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
            dispose();
        }
    }

    // *** Texture impl

    com.sun.opengl.util.texture.Texture texture = null; // holds the last fetched texture

    public void unlockTexture() {
        // FIXME: EGLSync notification ?
    }

    public Texture lockTexture() {
        texture=null;
        if (moviePtr == 0) {
            return null;
        }
        EGLImageTexture eglImgTex = (EGLImageTexture) eglImgTexsMap.get(new Integer(_getTextureID(moviePtr)));
        if(null!=eglImgTex) {
            texture = eglImgTex.texture;
        }
        return texture;
    }
}

