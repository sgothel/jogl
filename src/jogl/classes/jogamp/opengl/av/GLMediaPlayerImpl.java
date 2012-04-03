package jogamp.opengl.av;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLES2;
import javax.media.opengl.GLException;

import com.jogamp.opengl.av.GLMediaPlayer;
import com.jogamp.opengl.av.GLMediaEventListener;
import com.jogamp.opengl.util.texture.Texture;

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

    protected State state;
    protected int textureCount;
    protected int textureTarget;
    
    protected int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    protected int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };
    
    protected URL url = null;
    
    protected float playSpeed = 1.0f;
    
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected int width = 0;
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected int height = 0;
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected int fps = 0;
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected long bps = 0;
    /** In frames. Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected long totalFrames = 0;
    /** In ms. Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected long duration = 0;
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected String acodec = null;
    /** Shall be set by the {@link #initStreamImplPreGL()} method implementation. */
    protected String vcodec = null;

    protected long frameNumber = 0;
    
    private TextureFrame[] texFrames = null;
    protected HashMap<Integer, TextureFrame> texFrameMap = new HashMap<Integer, TextureFrame>();
    private ArrayList<GLMediaEventListener> eventListeners = new ArrayList<GLMediaEventListener>();

    protected GLMediaPlayerImpl() {
        this.textureCount=3;
        this.textureTarget=GL.GL_TEXTURE_2D;
        this.state = State.UninitializedStream;
    }

    protected final void setTextureCount(int textureCount) {
        this.textureCount=textureCount;
    }
    public final int getTextureCount() { return textureCount; }
    
    protected final void setTextureTarget(int textureTarget) {
        this.textureTarget=textureTarget;
    }
    public final int getTextureTarget() { return textureTarget; }

    public final void setTextureMinMagFilter(int[] minMagFilter) { texMinMagFilter[0] = minMagFilter[0]; texMinMagFilter[1] = minMagFilter[1];}
    public final int[] getTextureMinMagFilter() { return texMinMagFilter; }
    
    public final void setTextureWrapST(int[] wrapST) { texWrapST[0] = wrapST[0]; texWrapST[1] = wrapST[1];}
    public final int[] getTextureWrapST() { return texWrapST; }

    public final State start() {
        switch(state) {
            case Stopped:
            case Paused:
                if(startImpl()) {
                    state = State.Playing;
                }
        }
        return state;
    }
    protected abstract boolean startImpl();
    
    public final State pause() {
        if(State.Playing == state && pauseImpl()) {
            state = State.Paused;
        }
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
        return state;
    }
    protected abstract boolean stopImpl();
    
    public final long seek(long msec) {
        final long cp;
        switch(state) {
            case Stopped:
            case Playing:
            case Paused:
                cp = seekImpl(msec);
                break;
            default:
                cp = 0;
        }
        return cp;        
    }
    protected abstract long seekImpl(long msec);
    
    public final State getState() { return state; }
    
    @Override
    public final State initStream(URL url) throws IllegalStateException, IOException {
        if(State.UninitializedStream != state) {
            throw new IllegalStateException("Instance not in state "+State.UninitializedStream+", but "+state);
        }
        this.url = url;
        if (this.url != null) {
            initStreamImplPreGL();
            state = State.UninitializedGL;
        }
        return state;
    }
    
    /**
     * Implementation shall set the following set of data here or at {@link #setStreamImplPostGL()} 
     * @see #width
     * @see #height
     * @see #fps
     * @see #bps
     * @see #totalFrames
     * @see #acodec
     * @see #vcodec
    */
    protected abstract void initStreamImplPreGL() throws IOException;

    @Override
    public final State initGL(GL gl) throws IllegalStateException, GLException {
        if(State.UninitializedGL != state) {
            throw new IllegalStateException("Instance not in state "+State.UninitializedGL+", but "+state);
        }
        final GLContext ctx = gl.getContext();
        if(!ctx.isCurrent()) {
            throw new GLException("Not current: "+ctx);
        }

        try {
            if(null!=texFrames) {
                removeAllImageTextures(ctx);
            } else {
                texFrames = new TextureFrame[textureCount];
            }
        
            final int[] tex = new int[textureCount];
            {
                gl.glGenTextures(textureCount, tex, 0);
                final int err = gl.glGetError();
                if( GL.GL_NO_ERROR != err ) {
                    throw new RuntimeException("TextureNames creation failed (num: "+textureCount+"): err "+toHexString(err));
                }
            }
            
            for(int i=0; i<textureCount; i++) {
                final TextureFrame tf = createTexImage(ctx, i, tex); 
                texFrames[i] = tf;
                texFrameMap.put(tex[i], tf);
            }
            state = State.Stopped;
            return state;
        } catch (Throwable t) {
            throw new GLException("Error initializing GL resources", t);
        }
    }
    
    protected TextureFrame createTexImage(GLContext ctx, int idx, int[] tex) {
        return new TextureFrame( createTexImageImpl(ctx, idx, tex, true) );
    }
    
    protected Texture createTexImageImpl(GLContext ctx, int idx, int[] tex, boolean mustFlipVertically) {
        final GL gl = ctx.getGL();
        if( 0 > tex[idx] ) {
            throw new RuntimeException("TextureName "+toHexString(tex[idx])+" invalid.");
        }
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
                    GL.GL_RGBA,       // internal format
                    width,            // width
                    height,           // height
                    0,                // border
                    GL.GL_RGBA,       // format
                    GL.GL_UNSIGNED_BYTE, // type
                    null);            // pixels -- will be provided later
            {
                final int err = gl.glGetError();
                if( GL.GL_NO_ERROR != err ) {
                    throw new RuntimeException("Couldn't create TexImage2D RGBA "+width+"x"+height+", err "+toHexString(err));
                }
            }
        }
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, texMinMagFilter[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, texMinMagFilter[0]);        
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, texWrapST[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, texWrapST[1]);
        
        return com.jogamp.opengl.util.texture.TextureIO.newTexture(tex[idx],
                     textureTarget,
                     width, height,
                     width, height,
                     mustFlipVertically);        
    }
    
    protected void destroyTexImage(GLContext ctx, TextureFrame imgTex) {
        imgTex.getTexture().destroy(ctx.getGL());        
    }
    
    protected void removeAllImageTextures(GLContext ctx) {
        if(null != texFrames) {
            for(int i=0; i<textureCount; i++) {
                final TextureFrame imgTex = texFrames[i]; 
                destroyTexImage(ctx, imgTex);
                texFrames[i] = null;
            }
        }
        texFrameMap.clear();
    }

    protected void attributesUpdated(int event_mask) {
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().attributesChanges(this, event_mask);
            }
        }
    }
    protected void newFrameAvailable(TextureFrame frame) {
        frameNumber++;        
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().newFrameAvailable(this, frame);
            }
        }
    }
    
    @Override
    public synchronized float getPlaySpeed() {
        return playSpeed;
    }

    @Override
    public synchronized State destroy(GL gl) {
        destroyImpl(gl);
        removeAllImageTextures(gl.getContext());
        state = State.UninitializedStream;
        return state;
    }
    protected abstract void destroyImpl(GL gl);

    @Override
    public synchronized URL getURL() {
        return url;
    }

    @Override
    public synchronized String getVideoCodec() {
        return vcodec;
    }

    @Override
    public synchronized String getAudioCodec() {
        return acodec;
    }

    @Override
    public synchronized long getTotalFrames() {
        return totalFrames;
    }

    @Override
    public synchronized long getDuration() {
        return duration;
    }
    
    @Override
    public synchronized long getBitrate() {
        return bps;
    }

    @Override
    public synchronized int getFramerate() {
        return fps;
    }

    @Override
    public synchronized int getWidth() {
        return width;
    }

    @Override
    public synchronized int getHeight() {
        return height;
    }

    @Override
    public synchronized String toString() {
        final float ct = getCurrentPosition() / 1000.0f, tt = getDuration() / 1000.0f;
        return "GLMediaPlayer ["+state+", "+frameNumber+"/"+totalFrames+" frames, "+ct+"/"+tt+"s, stream [video ["+vcodec+", "+width+"x"+height+", "+fps+"fps, "+bps+"bsp], "+url.toExternalForm()+"]]";
    }

    @Override
    public void addEventListener(GLMediaEventListener l) {
        if(l == null) {
            return;
        }
        synchronized(eventListenersLock) {
            eventListeners.add(l);
        }
    }

    @Override
    public void removeEventListener(GLMediaEventListener l) {
        if (l == null) {
            return;
        }
        synchronized(eventListenersLock) {
            eventListeners.remove(l);
        }
    }

    @Override
    public synchronized GLMediaEventListener[] getEventListeners() {
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