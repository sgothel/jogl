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
import java.util.Iterator;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;

/**
 * After object creation an implementation may customize the behavior:
 * <ul>
 *   <li>{@link #setDesTextureCount(int)}</li>
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
    
    /** Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int width = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int height = 0;
    /** Video fps. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected float fps = 0;
    /** Stream bps. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int bps_stream = 0;
    /** Video bps. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int bps_video = 0;
    /** Audio bps. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int bps_audio = 0;
    /** In frames. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int totalFrames = 0;
    /** In ms. Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected int duration = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected String acodec = unknown;
    /** Shall be set by the {@link #initGLStreamImpl(GL)} method implementation. */
    protected String vcodec = unknown;
    
    protected int frameNumber = 0;
    protected int currentVideoPTS = 0;
    
    protected SyncedRingbuffer<TextureFrame> videoFramesFree =  null;
    protected SyncedRingbuffer<TextureFrame> videoFramesDecoded =  null;
    protected volatile TextureFrame lastFrame = null;

    private ArrayList<GLMediaEventListener> eventListeners = new ArrayList<GLMediaEventListener>();

    protected GLMediaPlayerImpl() {
        this.textureCount=0;
        this.textureTarget=GL.GL_TEXTURE_2D;
        this.textureFormat = GL.GL_RGBA;
        this.textureInternalFormat = GL.GL_RGBA;
        this.textureType = GL.GL_UNSIGNED_BYTE;        
        this.texUnit = 0;
        this.state = State.Uninitialized;
    }

    @Override
    public final void setTextureUnit(int u) { texUnit = u; }
    
    @Override
    public final int getTextureUnit() { return texUnit; }
    
    @Override
    public final int getTextureTarget() { return textureTarget; }
    
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
        switch( state ) {
            case Stopped:
                /** fall-through intended */
            case Paused:
                if( startImpl() ) {
                    resumeFramePusher();
                    state = State.Playing;
                }
            default:
        }
        if(DEBUG) { System.err.println("Start: "+toString()); }
        return state;
    }
    protected abstract boolean startImpl();
    
    public final State pause() {
        if( State.Playing == state && pauseImpl() ) {
            pauseFramePusher();
            state = State.Paused;
        }
        if(DEBUG) { System.err.println("Pause: "+toString()); }            
        return state;
    }
    protected abstract boolean pauseImpl();
    
    public final State stop() {
        switch( state ) {
            case Playing:
                /** fall-through intended */
            case Paused:
                if( stopImpl() ) {
                    pauseFramePusher();
                    state = State.Stopped;
                }
            default:
        }
        if(DEBUG) { System.err.println("Stop: "+toString()); }
        return state;
    }
    protected abstract boolean stopImpl();
    
    @Override
    public final int getCurrentPosition() {
        if( State.Uninitialized != state ) {
            return getCurrentPositionImpl();
        }
        return 0;
    }
    protected abstract int getCurrentPositionImpl();
    
    @Override
    public final int getVideoPTS() { return currentVideoPTS; }
    
    @Override
    public final int getAudioPTS() { 
        if( State.Uninitialized != state ) {
            return getAudioPTSImpl();
        }
        return 0;
    }    
    protected abstract int getAudioPTSImpl();
    
    public final int seek(int msec) {
        final int pts1;
        switch(state) {
            case Stopped:
            case Playing:
            case Paused:
                pauseFramePusher();
                pts1 = seekImpl(msec);
                currentVideoPTS=pts1;
                resumeFramePusher();
                break;
            default:
                pts1 = 0;
        }
        if(DEBUG) { System.err.println("Seek("+msec+"): "+toString()); }
        return pts1;        
    }
    protected abstract int seekImpl(int msec);
    
    public final State getState() { return state; }
    
    @Override
    public final State initGLStream(GL gl, int reqTextureCount, URLConnection urlConn) throws IllegalStateException, GLException, IOException {
        if(State.Uninitialized != state) {
            throw new IllegalStateException("Instance not in state "+State.Uninitialized+", but "+state+", "+this);
        }
        this.urlConn = urlConn;
        if (this.urlConn != null) {
            try {                
                if( null != gl ) {
                    removeAllTextureFrames(gl);
                    textureCount = validateTextureCount(reqTextureCount);
                    if( textureCount < 2 ) {
                        throw new InternalError("Validated texture count < 2: "+textureCount);
                    }
                    initGLStreamImpl(gl); // also initializes width, height, .. etc
                    videoFramesFree = new SyncedRingbuffer<TextureFrame>(createTexFrames(gl, textureCount), true /* full */);
                    if( 2 < textureCount ) {
                        videoFramesDecoded = new SyncedRingbuffer<TextureFrame>(new TextureFrame[textureCount], false /* full */);
                        framePusher = new FramePusher(gl, requiresOffthreadGLCtx());
                        framePusher.doStart();
                    } else {
                        videoFramesDecoded = null;
                    }
                    lastFrame = videoFramesFree.getBlocking(false /* clearRef */ );
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
     * Returns the validated number of textures to be handled.
     * <p>
     * Default is always 2 textures, last texture and the decoding texture. 
     * </p>
     */
    protected int validateTextureCount(int desiredTextureCount) {
        return 2;
    }
    protected boolean requiresOffthreadGLCtx() { return false; }
    
    private final TextureFrame[] createTexFrames(GL gl, final int count) {
        final int[] texNames = new int[count];
        gl.glGenTextures(count, texNames, 0);
        final int err = gl.glGetError();
        if( GL.GL_NO_ERROR != err ) {
            throw new RuntimeException("TextureNames creation failed (num: "+count+"): err "+toHexString(err));
        }
        final TextureFrame[] texFrames = new TextureFrame[count];
        for(int i=0; i<count; i++) {
            texFrames[i] = createTexImage(gl, texNames[i]);
        }
        return texFrames;
    }
    protected abstract TextureFrame createTexImage(GL gl, int texName);
    
    protected final Texture createTexImageImpl(GL gl, int texName, int tWidth, int tHeight, boolean mustFlipVertically) {
        if( 0 > texName ) {
            throw new RuntimeException("TextureName "+toHexString(texName)+" invalid.");
        }
        gl.glActiveTexture(GL.GL_TEXTURE0+getTextureUnit());
        gl.glBindTexture(textureTarget, texName);
        {
            final int err = gl.glGetError();
            if( GL.GL_NO_ERROR != err ) {
                throw new RuntimeException("Couldn't bind textureName "+toHexString(texName)+" to 2D target, err "+toHexString(err));
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
        
        return com.jogamp.opengl.util.texture.TextureIO.newTexture(
                     texName, textureTarget,
                     tWidth, tHeight,
                     width,  height,
                     mustFlipVertically);        
    }
        
    private final void removeAllTextureFrames(GL gl) {
        if( null != videoFramesFree ) {
            final TextureFrame[] texFrames = videoFramesFree.getArray(); 
            videoFramesFree = null;
            videoFramesDecoded = null;
            lastFrame = null;
            for(int i=0; i<texFrames.length; i++) {
                final TextureFrame frame = texFrames[i];
                if(null != frame) {
                    destroyTexFrame(gl, frame);
                    texFrames[i] = null;
                }
            }        
        }
        textureCount=0;
    }
    protected void destroyTexFrame(GL gl, TextureFrame frame) {
        frame.getTexture().destroy(gl);        
    }

    /**
     * Implementation shall set the following set of data here 
     * @param gl TODO
     * @see #width
     * @see #height
     * @see #fps
     * @see #bps_stream
     * @see #totalFrames
     * @see #acodec
     * @see #vcodec
    */
    protected abstract void initGLStreamImpl(GL gl) throws IOException;
    
    @Override
    public final TextureFrame getLastTexture() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        return lastFrame;
    }
    
    @Override
    public final synchronized TextureFrame getNextTexture(GL gl, boolean blocking) throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        if(State.Playing == state) {
            TextureFrame nextFrame = null;
            boolean ok = true;
            try {
                if( 2 < textureCount ) {
                    nextFrame = videoFramesDecoded.getBlocking(false /* clearRef */ );
                } else {
                    nextFrame = videoFramesFree.getBlocking(false /* clearRef */ );
                    if( getNextTextureImpl(gl, nextFrame, blocking) ) {
                        newFrameAvailable(nextFrame);
                    } else {
                        ok = false;
                    }
                }
                if( ok ) {
                    currentVideoPTS = nextFrame.getPTS();
                    if( blocking ) {
                        syncFrame2Audio(nextFrame);
                    }
                    final TextureFrame _lastFrame = lastFrame;
                    lastFrame = nextFrame;
                    videoFramesFree.putBlocking(_lastFrame);
                }
            } catch (InterruptedException e) {
                ok = false;
                e.printStackTrace();
            } finally {
                if( !ok && null != nextFrame ) { // put back
                    videoFramesFree.put(nextFrame);
                }
            }
        }
        return lastFrame;
    }
    protected abstract boolean getNextTextureImpl(GL gl, TextureFrame nextFrame, boolean blocking);
    protected abstract void syncFrame2Audio(TextureFrame frame);
    
    private final void newFrameAvailable(TextureFrame frame) {
        frameNumber++;        
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().newFrameAvailable(this, frame, System.currentTimeMillis());
            }
        }
    }
    
    class FramePusher extends Thread {
        private volatile boolean isRunning = false;
        private volatile boolean isActive = false;
        
        private volatile boolean shallPause = true;
        private volatile boolean shallStop = false;
        
        private final GL gl;
        private GLDrawable dummyDrawable = null;
        private GLContext sharedGLCtx = null;
        
        FramePusher(GL gl, boolean createSharedCtx) {
            setDaemon(true);
            this.gl = createSharedCtx ? createSharedGL(gl) : gl;
        }
        
        private GL createSharedGL(GL gl) {
            final GLContext glCtx = gl.getContext();
            final boolean glCtxCurrent = glCtx.isCurrent();
            final GLProfile glp = gl.getGLProfile();
            final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
            final AbstractGraphicsDevice device = glCtx.getGLDrawable().getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
            dummyDrawable = factory.createDummyDrawable(device, true, glp); // own device!
            dummyDrawable.setRealized(true);
            sharedGLCtx = dummyDrawable.createContext(glCtx);
            makeCurrent(sharedGLCtx);
            if( glCtxCurrent ) {
                makeCurrent(glCtx);
            } else {
                sharedGLCtx.release();
            }
            return sharedGLCtx.getGL();
        }
        private void makeCurrent(GLContext ctx) {
            if( GLContext.CONTEXT_NOT_CURRENT >= ctx.makeCurrent() ) {
                throw new GLException("Couldn't make ctx current: "+ctx);
            }
        }
        
        private void destroySharedGL() {
            if( null != sharedGLCtx ) {
                if( sharedGLCtx.isCreated() ) {
                    // Catch dispose GLExceptions by GLEventListener, just 'print' them
                    // so we can continue with the destruction.
                    try {
                        sharedGLCtx.destroy();
                    } catch (GLException gle) {
                        gle.printStackTrace();
                    }
                }
                sharedGLCtx = null;            
            }
            if( null != dummyDrawable ) {
                final AbstractGraphicsDevice device = dummyDrawable.getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
                dummyDrawable.setRealized(false);
                dummyDrawable = null;
                device.close();
            }            
        }
        
        public synchronized void doPause() {
            if( isActive ) {
                shallPause = true;
                while( isActive ) {
                    try {
                        this.wait(); // wait until paused
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        public synchronized void doResume() {
            if( isRunning && !isActive ) {
                shallPause = false;
                while( !isActive ) {
                    this.notify();  // wake-up pause-block
                    try {
                        this.wait(); // wait until resumed 
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        public synchronized void doStart() {
            start();
            while( !isRunning ) {
                try {
                    this.wait();  // wait until started
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        public synchronized void doStop() {
            if( isRunning ) {
                shallStop = true;
                while( isRunning ) {
                    this.notify();  // wake-up pause-block (opt)
                    try {
                        this.wait();  // wait until stopped
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        public boolean isRunning() { return isRunning; }
        public boolean isActive() { return isActive; }
        
        public void run() {
            setName(getName()+"-FramePusher_"+FramePusherInstanceId);
            FramePusherInstanceId++;
            
            synchronized ( this ) {
                if( null != sharedGLCtx ) {
                    makeCurrent( sharedGLCtx );
                }
                isRunning = true;
                this.notify();   // wake-up doStart()
            }
            
            while( !shallStop ){
                if( shallPause ) {
                    synchronized ( this ) {
                        while( shallPause && !shallStop ) {
                            isActive = false;
                            this.notify();   // wake-up doPause()
                            try {
                                this.wait(); // wait until resumed
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        isActive = true;
                        this.notify(); // wake-up doResume()
                    }
                }
                
                if( !shallStop ) {
                    TextureFrame nextFrame = null;
                    boolean ok = false;
                    try {
                        nextFrame = videoFramesFree.getBlocking(true /* clearRef */ );
                        if( getNextTextureImpl(gl, nextFrame, true) ) {
                            gl.glFinish();
                            videoFramesDecoded.putBlocking(nextFrame);
                            newFrameAvailable(nextFrame);
                            ok = true;
                        }
                    } catch (InterruptedException e) {
                        if( !shallStop && !shallPause ) {
                            e.printStackTrace(); // oops
                            shallPause = false;
                            shallStop = true;
                        }
                    } finally {
                        if( !ok && null != nextFrame ) { // put back
                            videoFramesFree.put(nextFrame);
                        }
                    }
                }
            }
            destroySharedGL();
            synchronized ( this ) {
                isRunning = false;
                isActive = false;
                this.notify(); // wake-up doStop()
            }
        }
    }    
    static int FramePusherInstanceId = 0;    
    private FramePusher framePusher = null;

    private final void pauseFramePusher() {
        if( null != framePusher ) {
            framePusher.doPause();
        }
    }
    private final void resumeFramePusher() {
        if( null != framePusher ) {
            framePusher.doResume();
        }
    }
    private final void destroyFramePusher() {
        if( null != framePusher ) {
            framePusher.doStop();
            framePusher = null;
        }
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
    
    @Override
    public final synchronized State destroy(GL gl) {
        destroyFramePusher();
        destroyImpl(gl);
        removeAllTextureFrames(gl);
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