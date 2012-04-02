package jogamp.opengl.av;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import javax.media.opengl.GL;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLES2;

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

    protected int textureCount;
    protected int textureTarget;
    
    private int sWidth = 0;
    private int sHeight = 0;
    protected URL url = null;
    
    protected Texture texture = null;
    protected float playSpeed = 1.0f;
    
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected int width = 0;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected int height = 0;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected int fps = 0;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected long bps = 0;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected long totalFrames = 0;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected String acodec = null;
    /** Shall be set by the {@link #setStreamImpl()} method implementation. */
    protected String vcodec = null;

    protected long frameNumber = 0;
    
    private TextureFrame[] texFrames = null;
    protected HashMap<Integer, TextureFrame> texFrameMap = new HashMap<Integer, TextureFrame>();
    private ArrayList<GLMediaEventListener> eventListeners = new ArrayList<GLMediaEventListener>();

    protected GLMediaPlayerImpl() {
        this.textureCount=3;
        this.textureTarget=GL.GL_TEXTURE_2D;
    }

    protected final void setTextureCount(int textureCount) {
        this.textureCount=textureCount;
    }
    protected final void setTextureTarget(int textureTarget) {
        this.textureTarget=textureTarget;
    }
    
    @Override
    public final void setStream(GL gl, URL url) throws IOException {
        this.url = url;
        if (this.url == null) {
            System.out.println("setURL (null)");
            stop();
            return;
        }
        setStreamImpl();
        init(gl);
    }
    
    /**
     * Implementation shall set the following set of data: 
     * @see #width
     * @see #height
     * @see #fps
     * @see #bps
     * @see #totalFrames
     * @see #acodec
     * @see #vcodec
    */
    protected abstract void setStreamImpl() throws IOException;

    protected final void init(GL gl) {
        final GLContext ctx = gl.getContext();
        if(!ctx.isCurrent()) {
            throw new RuntimeException("Not current: "+ctx);
        }
        
        final GLDrawable drawable = ctx.getGLDrawable();
        sWidth = drawable.getWidth();
        sHeight = drawable.getHeight();        
        System.out.println("surface size: "+sWidth+"x"+sHeight);
        System.out.println("Platform Extensions : "+ctx.getPlatformExtensionsString());
    
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
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, GL.GL_NEAREST);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, GL.GL_NEAREST);        
        // Clamp to edge is only option.
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, GL.GL_CLAMP_TO_EDGE);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, GL.GL_CLAMP_TO_EDGE);
        
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
        texture = null;
        for(int i=0; i<textureCount; i++) {
            final TextureFrame imgTex = texFrames[i]; 
            destroyTexImage(ctx, imgTex);
            texFrames[i] = null;
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
    public synchronized Texture getLastTextureID() {
        return texture;
    }

    @Override
    public synchronized void destroy(GL gl) {
        destroyImpl(gl);
        removeAllImageTextures(gl.getContext());
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
        return "GLMediaPlayer [ stream [ video [ "+vcodec+", "+width+"x"+height+", "+fps+"fps, "+bps+"bsp, "+totalFrames+"f ] ] ]";
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