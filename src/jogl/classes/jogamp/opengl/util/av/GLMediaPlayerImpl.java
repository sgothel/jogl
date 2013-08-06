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
package jogamp.opengl.util.av;

import java.io.IOException;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLException;

import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * After object creation an implementation may customize the behavior:
 * <ul>
 *   <li>{@link #setTextureCount(int)}</li>
 *   <li>{@link #setTextureTarget(int)}</li>
 *   <li>{@link EGLMediaPlayerImpl#setEGLTexImageAttribs(boolean, boolean)}.</li>
 * </ul>
 * 
 * <p>
 * See {@link GLMediaPlayer}.
 * </p>
 */
public abstract class GLMediaPlayerImpl implements GLMediaPlayer {

    protected static final String unknown = "unknown";

    protected State state;
    protected int textureCount;
    protected int textureTarget;
    protected int textureFormat;
    protected int textureInternalFormat; 
    protected int textureType;
    protected int texUnit;
    
    
    protected int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    protected int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };
    
    protected URLConnection urlConn = null;
    
    protected volatile float playSpeed = 1.0f;
    
    /** Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int width = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int height = 0;
    /** Video fps. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected float fps = 0;
    /** Stream bps. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int bps_stream = 0;
    /** Video bps. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int bps_video = 0;
    /** Audio bps. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int bps_audio = 0;
    /** In frames. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int totalFrames = 0;
    /** In ms. Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected int duration = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected String acodec = unknown;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int[])} method implementation. */
    protected String vcodec = unknown;
    
    protected int frameNumber = 0;
    
    protected TextureSequence.TextureFrame[] texFrames = null;
    protected HashMap<Integer, TextureSequence.TextureFrame> texFrameMap = new HashMap<Integer, TextureSequence.TextureFrame>();
    private ArrayList<GLMediaEventListener> eventListeners = new ArrayList<GLMediaEventListener>();

    protected GLMediaPlayerImpl() {
        this.textureCount=3;
        this.textureTarget=GL.GL_TEXTURE_2D;
        this.textureFormat = GL.GL_RGBA;
        this.textureInternalFormat = GL.GL_RGBA;
        this.textureType = GL.GL_UNSIGNED_BYTE;        
        this.texUnit = 0;
        this.state = State.Uninitialized;
    }

    @Override
    public void setTextureUnit(int u) { texUnit = u; }
    
    @Override
    public int getTextureUnit() { return texUnit; }
    
    protected final void setTextureCount(int textureCount) {
        this.textureCount=textureCount;
    }
    @Override
    public final int getTextureCount() { return textureCount; }
    
    protected final void setTextureTarget(int target) { textureTarget=target; }
    protected final void setTextureFormat(int internalFormat, int format) { 
        textureInternalFormat=internalFormat; 
        textureFormat=format; 
    }    
    protected final void setTextureType(int t) { textureType=t; }

    public final void setTextureMinMagFilter(int[] minMagFilter) { texMinMagFilter[0] = minMagFilter[0]; texMinMagFilter[1] = minMagFilter[1];}
    public final int[] getTextureMinMagFilter() { return texMinMagFilter; }
    
    public final void setTextureWrapST(int[] wrapST) { texWrapST[0] = wrapST[0]; texWrapST[1] = wrapST[1];}
    public final int[] getTextureWrapST() { return texWrapST; }

    @Override
    public final TextureSequence.TextureFrame getLastTexture() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        return getLastTextureImpl();
    }
    protected abstract TextureSequence.TextureFrame getLastTextureImpl();
    
    @Override
    public final synchronized TextureSequence.TextureFrame getNextTexture(GL gl, boolean blocking) throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        if(State.Playing == state) {
            final TextureSequence.TextureFrame f = getNextTextureImpl(gl, blocking);
            return f;
        }
        return getLastTextureImpl();        
    }
    protected abstract TextureSequence.TextureFrame getNextTextureImpl(GL gl, boolean blocking);
    
    @Override
    public String getRequiredExtensionsShaderStub() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        if(GLES2.GL_TEXTURE_EXTERNAL_OES == textureTarget) {
            return TextureSequence.GL_OES_EGL_image_external_Required_Prelude;
        }
        return "";
    }
        
    @Override
    public String getTextureSampler2DType() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        switch(textureTarget) {
            case GL.GL_TEXTURE_2D:
            case GL2.GL_TEXTURE_RECTANGLE: 
                return TextureSequence.sampler2D;
            case GLES2.GL_TEXTURE_EXTERNAL_OES:
                return TextureSequence.samplerExternalOES;
            default:
                throw new GLException("Unsuported texture target: "+toHexString(textureTarget));            
        }
    }
    
    /**
     * {@inheritDoc}
     * 
     * This implementation simply returns the build-in function name of <code>texture2D</code>,
     * if not overridden by specialization.
     */
    @Override
    public String getTextureLookupFunctionName(String desiredFuncName) throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        return "texture2D";
    }
    
    /**
     * {@inheritDoc}
     * 
     * This implementation simply returns an empty string since it's using 
     * the build-in function <code>texture2D</code>,
     * if not overridden by specialization.
     */
    @Override
    public String getTextureLookupFragmentShaderImpl() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        return ""; 
    }
    
    @Override
    public final float getPlaySpeed() {
        return playSpeed;
    }
    
    @Override
    public final synchronized void setPlaySpeed(float rate) {
        if(State.Uninitialized != state && setPlaySpeedImpl(rate)) {
            playSpeed = rate;
        }
        if(DEBUG) { System.err.println("SetPlaySpeed: "+toString()); }
    }
    protected abstract boolean setPlaySpeedImpl(float rate);

    public final State start() {
        switch(state) {
            case Stopped:
            case Paused:
                if(startImpl()) {
                    state = State.Playing;
                }
        }
        if(DEBUG) { System.err.println("Start: "+toString()); }
        return state;
    }
    protected abstract boolean startImpl();
    
    public final State pause() {
        if(State.Playing == state && pauseImpl()) {
            state = State.Paused;
        }
        if(DEBUG) { System.err.println("Pause: "+toString()); }            
        return state;
    }
    protected abstract boolean pauseImpl();
    
    public final State stop() {
        switch(state) {
            case Playing:
            case Paused:
                if(stopImpl()) {
                    state = State.Stopped;
                }
        }
        if(DEBUG) { System.err.println("Stop: "+toString()); }
        return state;
    }
    protected abstract boolean stopImpl();
    
    @Override
    public final int getCurrentPosition() {
        if(State.Uninitialized != state) {
            return getCurrentPositionImpl();
        }
        return 0;
    }
    protected abstract int getCurrentPositionImpl();
    
    public final int seek(int msec) {
        final int cp;
        switch(state) {
            case Stopped:
            case Playing:
            case Paused:
                cp = seekImpl(msec);
                break;
            default:
                cp = 0;
        }
        if(DEBUG) { System.err.println("Seek("+msec+"): "+toString()); }
        return cp;        
    }
    protected abstract int seekImpl(int msec);
    
    public final State getState() { return state; }
    
    @Override
    public final State initGLStream(GL gl, URLConnection urlConn) throws IllegalStateException, GLException, IOException {
        if(State.Uninitialized != state) {
            throw new IllegalStateException("Instance not in state "+State.Uninitialized+", but "+state+", "+this);
        }
        this.urlConn = urlConn;
        if (this.urlConn != null) {
            try {                
                if(null != gl) {
                    if(null!=texFrames) {
                        // re-init ..
                        removeAllImageTextures(gl);
                    } else {
                        texFrames = new TextureSequence.TextureFrame[textureCount];
                    }
                    final int[] tex = new int[textureCount];
                    {
                        gl.glGenTextures(textureCount, tex, 0);
                        final int err = gl.glGetError();
                        if( GL.GL_NO_ERROR != err ) {
                            throw new RuntimeException("TextureNames creation failed (num: "+textureCount+"): err "+toHexString(err));
                        }
                    }
                    initGLStreamImpl(gl, tex);
                    
                    for(int i=0; i<textureCount; i++) {
                        final TextureSequence.TextureFrame tf = createTexImage(gl, i, tex); 
                        texFrames[i] = tf;
                        texFrameMap.put(tex[i], tf);
                    }
                }
                state = State.Stopped;
                return state;
            } catch (Throwable t) {
                throw new GLException("Error initializing GL resources", t);
            }
        }
        return state;        
    }
    
    /**
     * Implementation shall set the following set of data here 
     * @param gl TODO
     * @param texNames TODO
     * @see #width
     * @see #height
     * @see #fps
     * @see #bps_stream
     * @see #totalFrames
     * @see #acodec
     * @see #vcodec
    */
    protected abstract void initGLStreamImpl(GL gl, int[] texNames) throws IOException;
    
    protected TextureSequence.TextureFrame createTexImage(GL gl, int idx, int[] tex) {
        return new TextureSequence.TextureFrame( createTexImageImpl(gl, idx, tex, width, height, false) );
    }
    
    protected Texture createTexImageImpl(GL gl, int idx, int[] tex, int tWidth, int tHeight, boolean mustFlipVertically) {
        if( 0 > tex[idx] ) {
            throw new RuntimeException("TextureName "+toHexString(tex[idx])+" invalid.");
        }
        gl.glActiveTexture(GL.GL_TEXTURE0+getTextureUnit());
        gl.glBindTexture(textureTarget, tex[idx]);
        {
            final int err = gl.glGetError();
            if( GL.GL_NO_ERROR != err ) {
                throw new RuntimeException("Couldn't bind textureName "+toHexString(tex[idx])+" to 2D target, err "+toHexString(err));
            }
        }

        if(GLES2.GL_TEXTURE_EXTERNAL_OES != textureTarget) {
            // create space for buffer with a texture
            gl.glTexImage2D(
                    textureTarget,    // target
                    0,                // level
                    textureInternalFormat, // internal format
                    tWidth,           // width
                    tHeight,          // height
                    0,                // border
                    textureFormat,
                    textureType,
                    null);            // pixels -- will be provided later
            {
                final int err = gl.glGetError();
                if( GL.GL_NO_ERROR != err ) {
                    throw new RuntimeException("Couldn't create TexImage2D RGBA "+tWidth+"x"+tHeight+", err "+toHexString(err));
                }
            }
            if(DEBUG) {
                System.err.println("Created TexImage2D RGBA "+tWidth+"x"+tHeight+", target "+toHexString(textureTarget)+
                                   ", ifmt "+toHexString(GL.GL_RGBA)+", fmt "+toHexString(textureFormat)+", type "+toHexString(textureType));
            }
        }
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, texMinMagFilter[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, texMinMagFilter[1]);        
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, texWrapST[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, texWrapST[1]);
        
        return com.jogamp.opengl.util.texture.TextureIO.newTexture(tex[idx],
                     textureTarget,
                     tWidth, tHeight,
                     width,  height,
                     mustFlipVertically);        
    }
    
    protected void destroyTexImage(GL gl, TextureSequence.TextureFrame imgTex) {
        imgTex.getTexture().destroy(gl);        
    }
    
    protected void removeAllImageTextures(GL gl) {
        if(null != texFrames) {
            for(int i=0; i<textureCount; i++) {
                final TextureSequence.TextureFrame imgTex = texFrames[i];
                if(null != imgTex) {
                    destroyTexImage(gl, imgTex);
                    texFrames[i] = null;
                }
            }
        }
        texFrameMap.clear();
    }

    protected final void updateAttributes(int width, int height, int bps_stream, int bps_video, int bps_audio, 
                                          float fps, int totalFrames, int duration, 
                                          String vcodec, String acodec) {
        int event_mask = 0;
        if( this.width != width || this.height != height ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_SIZE;
            this.width = width;
            this.height = height;
        }   
        if( this.fps != fps ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_FPS;
            this.fps = fps;
        }
        if( this.bps_stream != bps_stream || this.bps_video != bps_video || this.bps_audio != bps_audio ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_BPS;
            this.bps_stream = bps_stream;
            this.bps_video = bps_video;
            this.bps_audio = bps_audio;
        }
        if( this.totalFrames != totalFrames || this.duration != duration ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_LENGTH;
            this.totalFrames = totalFrames;
            this.duration = duration;
        }
        if( (null!=acodec && acodec.length()>0 && !this.acodec.equals(acodec)) ) { 
            event_mask |= GLMediaEventListener.EVENT_CHANGE_CODEC;
            this.acodec = acodec;
        }
        if( (null!=vcodec && vcodec.length()>0 && !this.vcodec.equals(vcodec)) ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_CODEC;
            this.vcodec = vcodec;
        }
        if(0==event_mask) {
            return;
        }
        attributesUpdated(event_mask);    
    }

    protected final void attributesUpdated(int event_mask) {
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().attributesChanges(this, event_mask, System.currentTimeMillis());
            }
        }
    }
    protected final void newFrameAvailable() {
        frameNumber++;        
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().newFrameAvailable(this, System.currentTimeMillis());
            }
        }
    }
    
    @Override
    public final synchronized State destroy(GL gl) {
        destroyImpl(gl);
        removeAllImageTextures(gl);
        state = State.Uninitialized;
        return state;
    }
    protected abstract void destroyImpl(GL gl);

    @Override
    public final synchronized URLConnection getURLConnection() {
        return urlConn;
    }

    @Override
    public final synchronized String getVideoCodec() {
        return vcodec;
    }

    @Override
    public final synchronized String getAudioCodec() {
        return acodec;
    }

    @Override
    public final synchronized long getTotalFrames() {
        return totalFrames;
    }

    @Override
    public final synchronized int getDuration() {
        return duration;
    }
    
    @Override
    public final synchronized long getStreamBitrate() {
        return bps_stream;
    }

    @Override
    public final synchronized int getVideoBitrate() {
        return bps_video;
    }
    
    @Override
    public final synchronized int getAudioBitrate() {
        return bps_audio;
    }
    
    @Override
    public final synchronized float getFramerate() {
        return fps;
    }

    @Override
    public final synchronized int getWidth() {
        return width;
    }

    @Override
    public final synchronized int getHeight() {
        return height;
    }

    @Override
    public final synchronized String toString() {
        final float ct = getCurrentPosition() / 1000.0f, tt = getDuration() / 1000.0f;
        final String loc = ( null != urlConn ) ? urlConn.getURL().toExternalForm() : "<undefined stream>" ;
        return "GLMediaPlayer["+state+", "+frameNumber+"/"+totalFrames+" frames, "+ct+"/"+tt+"s, speed "+playSpeed+", "+bps_stream+" bps, "+
                "Texture[count "+textureCount+", target "+toHexString(textureTarget)+", format "+toHexString(textureFormat)+", type "+toHexString(textureType)+"], "+               
               "Stream[Video[<"+vcodec+">, "+width+"x"+height+", "+fps+" fps, "+bps_video+" bsp], "+
               "Audio[<"+acodec+">, "+bps_audio+" bsp]], "+loc+"]";
    }

    @Override
    public final void addEventListener(GLMediaEventListener l) {
        if(l == null) {
            return;
        }
        synchronized(eventListenersLock) {
            eventListeners.add(l);
        }
    }

    @Override
    public final void removeEventListener(GLMediaEventListener l) {
        if (l == null) {
            return;
        }
        synchronized(eventListenersLock) {
            eventListeners.remove(l);
        }
    }

    @Override
    public final synchronized GLMediaEventListener[] getEventListeners() {
        synchronized(eventListenersLock) {
            return eventListeners.toArray(new GLMediaEventListener[eventListeners.size()]);
        }
    }

    private Object eventListenersLock = new Object();

    protected static final String toHexString(long v) {
        return "0x"+Long.toHexString(v);
    }
    protected static final String toHexString(int v) {
        return "0x"+Integer.toHexString(v);
    }
        
}