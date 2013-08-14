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

import com.jogamp.opengl.util.av.AudioSink;
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

    /** Default texture count w/o threading, value {@value}. */
    protected static final int TEXTURE_COUNT_DEFAULT = 2;
    
    protected volatile State state;
    private Object stateLock = new Object();
    
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
    
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int vid = GLMediaPlayer.STREAM_ID_AUTO;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int aid = GLMediaPlayer.STREAM_ID_AUTO;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int width = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int height = 0;
    /** Video fps. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected float fps = 0;
    protected int frame_period = 0;
    /** Stream bps. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int bps_stream = 0;
    /** Video bps. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int bps_video = 0;
    /** Audio bps. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int bps_audio = 0;
    /** In frames. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int videoFrames = 0;
    /** In frames. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int audioFrames = 0;
    /** In ms. Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected int duration = 0;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected String acodec = unknown;
    /** Shall be set by the {@link #initGLStreamImpl(GL, int, int)} method implementation. */
    protected String vcodec = unknown;
    
    protected volatile int decodedFrameCount = 0;
    protected int presentedFrameCount = 0;
    protected volatile int video_pts_last = 0;
    
    /** See {@link #getAudioSink()}. Set by implementation if used from within {@link #initGLStreamImpl(GL, int, int)}! */
    protected AudioSink audioSink = null;
    protected boolean audioSinkPlaySpeedSet = false;
    
    /** System Clock Reference (SCR) of first audio PTS at start time. */
    private long audio_scr_t0 = 0;
    private boolean audioSCR_reset = true;
    
    /** System Clock Reference (SCR) of first video frame at start time. */
    private long video_scr_t0 = 0;
    /** System Clock Reference (SCR) PTS offset, i.e. first video PTS at start time. */
    private int video_scr_pts = 0;
    /** Cumulative video pts diff. */
    private float video_dpts_cum = 0;
    /** Cumulative video frames. */
    private int video_dpts_count = 0;
    /** Number of min frame count required for video cumulative sync. */ 
    private static final int VIDEO_DPTS_NUM = 20;
    /** Cumulative coefficient, value {@value}. */
    private static final float VIDEO_DPTS_COEFF = 0.7943282f; // (float) Math.exp(Math.log(0.01) / VIDEO_DPTS_NUM);
    /** Maximum valid video pts diff. */
    private static final int VIDEO_DPTS_MAX = 5000; // 5s max diff
    /** Trigger video PTS reset with given cause as bitfield. */
    private volatile int videoSCR_reset = 0;
    
    private final boolean isSCRCause(int bit) { return 0 != ( bit & videoSCR_reset); }
    /** SCR reset due to: Start, Resume, Seek, .. */ 
    private static final int SCR_RESET_FORCE = 1 << 0;
    /** SCR reset due to: PlaySpeed */
    private static final int SCR_RESET_SPEED = 1 << 1;
    
    /** Latched video PTS reset, to wait until valid pts after invalidation of cached ones. Currently [1..{@link #VIDEO_DPTS_NUM}] frames. */
    private int videoSCR_reset_latch = 0;
        
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
    public final int getDecodedFrameCount() { return decodedFrameCount; }
    
    @Override
    public final int getPresentedFrameCount() { return this.presentedFrameCount; }
    
    @Override
    public final int getVideoPTS() { return video_pts_last; }
    
    @Override
    public final int getAudioPTS() { 
        if( State.Uninitialized != state ) {
            return getAudioPTSImpl();
        }
        return 0;
    }
    /** Override if not using audioSink! */
    protected int getAudioPTSImpl() {
        if( null != audioSink ) {
            return audioSink.getPTS();
        } else {
            return 0;
        }
    }
    
    public final State getState() { return state; }
    
    public final State play() {
        synchronized( stateLock ) {
            switch( state ) {
                case Paused:
                    if( playImpl() ) {
                        resetAudioVideoSCR(SCR_RESET_FORCE);
                        resumeFramePusher();
                        if( null != audioSink ) {
                            audioSink.play();
                        }
                        state = State.Playing;
                    }
                default:
            }
            if(DEBUG) { System.err.println("Start: "+toString()); }
            return state;
        }
    }
    protected abstract boolean playImpl();
    
    public final State pause() {
        synchronized( stateLock ) {
            if( State.Playing == state ) {
                State _state = state;
                state = State.Paused;
                if( pauseImpl() ) {
                    _state = State.Paused;
                    pauseFramePusher();
                    if( null != audioSink ) {
                        audioSink.pause();
                    }
                }
                state = _state;
            }
            if(DEBUG) { System.err.println("Pause: "+toString()); }            
            return state;
        }
    }
    protected abstract boolean pauseImpl();
    
    public final int seek(int msec) {
        synchronized( stateLock ) {
            final int pts1;
            switch(state) {
                case Playing:
                case Paused:
                    final State _state = state;
                    state = State.Paused;
                    pauseFramePusher();
                    resetAudioVideoSCR(SCR_RESET_FORCE);
                    pts1 = seekImpl(msec);
                    if( null != audioSink ) {
                        audioSink.flush();
                        if( State.Playing == _state ) {
                            audioSink.play(); // cont. w/ new data
                        }
                    }
                    resumeFramePusher();
                    state = _state;
                    break;
                default:
                    pts1 = 0;
            }
            if(DEBUG) { System.err.println("Seek("+msec+"): "+toString()); }
            return pts1;
        }
    }
    protected abstract int seekImpl(int msec);
    
    @Override
    public final float getPlaySpeed() {
        return playSpeed;
    }
    
    @Override
    public final boolean setPlaySpeed(float rate) {
        synchronized( stateLock ) {
            boolean res = false;
            if(State.Uninitialized != state ) {
                if( rate > 0.01f ) {
                    if( Math.abs(1.0f - rate) < 0.01f ) {
                        rate = 1.0f;
                    }
                    if( setPlaySpeedImpl(rate) ) {
                        resetAudioVideoSCR(SCR_RESET_SPEED);
                        playSpeed = rate;
                        if(DEBUG) { System.err.println("SetPlaySpeed: "+toString()); }
                        res = true;
                    }
                }
            }
            return res;
        }
    }
    /** 
     * Override if not using AudioSink, or AudioSink's {@link AudioSink#setPlaySpeed(float)} is not sufficient!
     * <p>
     * AudioSink shall respect <code>!audioSinkPlaySpeedSet</code> to determine data_size 
     * at {@link AudioSink#enqueueData(com.jogamp.opengl.util.av.AudioSink.AudioFrame)}.
     * </p> 
     */
    protected boolean setPlaySpeedImpl(float rate) {
        if( null != audioSink ) {
            audioSinkPlaySpeedSet = audioSink.setPlaySpeed(rate);
        }
        // still true, even if audioSink rejects command since we deal w/ video sync 
        // and AudioSink w/ audioSinkPlaySpeedSet at enqueueData(..).
        return true;
    }

    @Override
    public final State initGLStream(GL gl, int reqTextureCount, URLConnection urlConn, int vid, int aid) throws IllegalStateException, GLException, IOException {
        synchronized( stateLock ) {
            if(State.Uninitialized != state) {
                throw new IllegalStateException("Instance not in state "+State.Uninitialized+", but "+state+", "+this);
            }
            decodedFrameCount = 0;
            presentedFrameCount = 0;
            this.urlConn = urlConn;
            if (this.urlConn != null) {
                try {                
                    if( null != gl ) {
                        removeAllTextureFrames(gl);
                        textureCount = validateTextureCount(reqTextureCount);
                        if( textureCount < TEXTURE_COUNT_DEFAULT ) {
                            throw new InternalError("Validated texture count < "+TEXTURE_COUNT_DEFAULT+": "+textureCount);
                        }
                        initGLStreamImpl(gl, vid, aid); // also initializes width, height, .. etc
                        videoFramesFree = new SyncedRingbuffer<TextureFrame>(createTexFrames(gl, textureCount), true /* full */);
                        if( TEXTURE_COUNT_DEFAULT < textureCount ) {
                            videoFramesDecoded = new SyncedRingbuffer<TextureFrame>(new TextureFrame[textureCount], false /* full */);
                            framePusher = new FramePusher(gl, requiresOffthreadGLCtx());
                            framePusher.doStart();
                        } else {
                            videoFramesDecoded = null;
                        }
                        lastFrame = videoFramesFree.getBlocking(false /* clearRef */ );
                        state = State.Paused;
                    }
                    return state;
                } catch (Throwable t) {
                    throw new GLException("Error initializing GL resources", t);
                }
            }
            return state;
        }
    }
    /**
     * Implementation shall set the following set of data here
     * @see #vid
     * @see #aid 
     * @see #width
     * @see #height
     * @see #fps
     * @see #bps_stream
     * @see #videoFrames
     * @see #audioFrames
     * @see #acodec
     * @see #vcodec
    */
    protected abstract void initGLStreamImpl(GL gl, int vid, int aid) throws IOException;
    
    /** 
     * Returns the validated number of textures to be handled.
     * <p>
     * Default is 2 textures w/o threading, last texture and the decoding texture. 
     * </p>
     * <p>
     * &gt; 2 textures is used for threaded decoding, a minimum of 4 textures seems reasonable in this case.
     * </p>
     */
    protected int validateTextureCount(int desiredTextureCount) {
        return TEXTURE_COUNT_DEFAULT;
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
        
    protected void destroyTexFrame(GL gl, TextureFrame frame) {
        frame.getTexture().destroy(gl);        
    }

    @Override
    public final TextureFrame getLastTexture() throws IllegalStateException {
        if(State.Uninitialized == state) {
            throw new IllegalStateException("Instance not initialized: "+this);
        }
        return lastFrame;
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
                System.err.println(Thread.currentThread().getName()+"> Clear TexFrame["+i+"]: "+frame+" -> null");            
            }        
        }
        textureCount=0;
    }
    
    @Override
    public final TextureFrame getNextTexture(GL gl, boolean blocking) throws IllegalStateException {
        synchronized( stateLock ) {
            if(State.Uninitialized == state) {
                throw new IllegalStateException("Instance not initialized: "+this);
            }
            if(State.Playing == state) {
                TextureFrame nextFrame = null;
                boolean ok = true;
                boolean dropFrame = false;
                try {
                    do { 
                        if( TEXTURE_COUNT_DEFAULT < textureCount ) {
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
                            presentedFrameCount++;
                            final int video_pts;
                            if( 0 != videoSCR_reset ) {
                                if( isSCRCause(SCR_RESET_FORCE) ) {
                                    videoSCR_reset_latch = VIDEO_DPTS_NUM / 2;
                                    resetVideoDPTS();
                                    resetAllVideoPTS();
                                } else {
                                    // SCR_RESET_SPEED
                                    videoSCR_reset_latch = 1;
                                }
                                videoSCR_reset = 0;
                                video_pts = TextureFrame.INVALID_PTS;
                            } else {
                                video_pts = nextFrame.getPTS();
                            }
                            if( video_pts != TextureFrame.INVALID_PTS ) {
                                final int frame_period_last = video_pts - video_pts_last; // rendering loop interrupted ?
                                if( videoSCR_reset_latch > 0 || frame_period_last > frame_period*10 ) {
                                    if( videoSCR_reset_latch > 0 ) {
                                        videoSCR_reset_latch--;
                                    }
                                    setFirstVideoPTS2SCR( video_pts );
                                }
                                final int scr_pts = video_scr_pts + 
                                                    (int) ( ( System.currentTimeMillis() - video_scr_t0 ) * playSpeed );
                                final int d_vpts = video_pts - scr_pts;
                                if( -VIDEO_DPTS_MAX > d_vpts || d_vpts > VIDEO_DPTS_MAX ) {
                                    if( DEBUG ) {
                                        System.err.println( getPerfStringImpl( scr_pts, video_pts, d_vpts, 0 ) );
                                    }
                                } else {
                                    video_dpts_count++;
                                    video_dpts_cum = d_vpts + VIDEO_DPTS_COEFF * video_dpts_cum;
                                    final int video_dpts_avg_diff = getVideoDPTSAvg();
                                    if( DEBUG ) {
                                        System.err.println( getPerfStringImpl( scr_pts, video_pts, d_vpts, video_dpts_avg_diff ) );
                                    }
                                    if( blocking && syncAVRequired() ) {
                                        if( !syncAV( (int) ( video_dpts_avg_diff / playSpeed + 0.5f ) ) ) {
                                            resetVideoDPTS();
                                            dropFrame = true;
                                        }
                                    }
                                    video_pts_last = video_pts;
                                }
                            }
                            final TextureFrame _lastFrame = lastFrame;
                            lastFrame = nextFrame;
                            videoFramesFree.putBlocking(_lastFrame);
                        }
                    } while( dropFrame );
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
    }
    protected abstract boolean getNextTextureImpl(GL gl, TextureFrame nextFrame, boolean blocking);
    protected boolean syncAVRequired() { return false; }
    
    /** 
     * {@inheritDoc}
     * <p>
     * Note: All {@link AudioSink} operations are performed from {@link GLMediaPlayerImpl},
     * i.e. {@link #play()}, {@link #pause()}, {@link #seek(int)}, {@link #setPlaySpeed(float)}, {@link #getAudioPTS()}.
     * </p>
     * <p>
     * Implementations using an {@link AudioSink} shall write it's instance to {@link #audioSink}
     * from within their {@link #initGLStreamImpl(GL, int, int)} implementation.
     * </p>
     */
    @Override
    public final AudioSink getAudioSink() { return audioSink; }
    
    /** 
     * To be called from implementation at 1st PTS after start
     * w/ current pts value in milliseconds.
     * @param audio_scr_t0
     */
    protected void setFirstAudioPTS2SCR(int pts) {
        if( audioSCR_reset ) {
            audio_scr_t0 = System.currentTimeMillis() - pts;
            audioSCR_reset = false;
        }
    }
    private void setFirstVideoPTS2SCR(int pts) {
        // video_scr_t0 = System.currentTimeMillis() - pts;
        video_scr_t0 = System.currentTimeMillis();
        video_scr_pts = pts;
    }
    private void resetAllVideoPTS() {
        if( null != videoFramesFree ) {
            final TextureFrame[] texFrames = videoFramesFree.getArray(); 
            for(int i=0; i<texFrames.length; i++) {
                final TextureFrame frame = texFrames[i];
                frame.setPTS(TextureFrame.INVALID_PTS);
            }        
        }        
    }
    private void resetVideoDPTS() {
        video_dpts_cum = 0;
        video_dpts_count = 0;        
    }
    private final int getVideoDPTSAvg() {
        if( video_dpts_count < VIDEO_DPTS_NUM ) {
            return 0;
        } else {
            return (int) ( video_dpts_cum * (1.0f - VIDEO_DPTS_COEFF) + 0.5f );
        }
    }
    
    private void resetAudioVideoSCR(int cause) {
        audioSCR_reset = true;
        videoSCR_reset |= cause;
    }
    
    /**
     * Synchronizes A-V.
     * <p>
     * https://en.wikipedia.org/wiki/Audio_to_video_synchronization
     * <pre>
     *   d_av = v_pts - a_pts;
     * </pre>
     * </p>
     * <p>
     * Recommendation of audio/video pts time lead/lag at production:
     * <ul>
     *   <li>Overall:    +40ms and -60ms  audio ahead video / audio after video</li>
     *   <li>Each stage:  +5ms and -15ms. audio ahead video / audio after video</li>
     * </ul>
     * </p>
     * <p>
     * Recommendation of av pts time lead/lag at presentation:
     * <ul>
     *   <li>TV:         +15ms and -45ms. audio ahead video / audio after video.</li>
     *   <li>Film:       +22ms and -22ms. audio ahead video / audio after video.</li>
     * </ul>
     * </p>
     * <p>
     * Maybe implemented as follows: 
     * <pre>
     *   d_av = vpts - apts;
     *   d_av < -22: audio after video == video ahead audio -> drop
     *   d_av >  22: audio ahead video == video after audio -> sleep(d_av - 10) 
     * </pre>
     * </p>
     * <p>
     * Returns true if audio is ahead of video, otherwise false (video is ahead of audio).
     * In case of the latter (false), the video frame shall be dropped!
     * </p>
     * @param frame
     * @return true if audio is ahead of video, otherwise false (video is ahead of audio)
     */
    protected boolean syncAV(int d_vpts) {
        if( d_vpts > 22 ) {
            if( DEBUG ) {
                System.err.println("V (sleep): "+(d_vpts - 22 / 2)+" ms");
            }
            try {
                Thread.sleep( d_vpts - 22 / 2 );
            } catch (InterruptedException e) { }
        }
        return true;
    }
    
    private final void newFrameAvailable(TextureFrame frame) {
        decodedFrameCount++;        
        synchronized(eventListenersLock) {
            for(Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().newFrameAvailable(this, frame, System.currentTimeMillis());
            }
        }
    }
    
    class FramePusher extends Thread {
        private volatile boolean isRunning = false;
        private volatile boolean isActive = false;
        private volatile boolean isBlocked = false;
        
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
                if( isBlocked && isActive ) {
                    this.interrupt();
                }
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
                if( isBlocked && isRunning ) {
                    this.interrupt();
                }
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
                                if( !shallPause ) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        isActive = true;
                        this.notify(); // wake-up doResume()
                    }
                }
                
                if( !shallStop ) {
                    TextureFrame nextFrame = null;
                    try {
                        isBlocked = true;
                        nextFrame = videoFramesFree.getBlocking(false /* clearRef */ );
                        isBlocked = false;
                        nextFrame.setPTS( TextureFrame.INVALID_PTS ); // mark invalid until processed!
                        if( getNextTextureImpl(gl, nextFrame, true) ) {
                            // gl.glFinish();
                            gl.glFlush(); // even better: sync object!
                            if( !videoFramesDecoded.put(nextFrame) ) { 
                                throw new InternalError("XXX: "+GLMediaPlayerImpl.this); 
                            }
                            final TextureFrame _nextFrame = nextFrame;
                            nextFrame = null;
                            newFrameAvailable(_nextFrame);
                        }
                    } catch (InterruptedException e) {
                        isBlocked = false;
                        if( !shallStop && !shallPause ) {
                            e.printStackTrace(); // oops
                            shallPause = false;
                            shallStop = true;
                        }
                    } finally {
                        if( null != nextFrame ) { // put back
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
    
    protected final void updateAttributes(int vid, int aid, int width, int height, int bps_stream, 
                                          int bps_video, int bps_audio, float fps, 
                                          int videoFrames, int audioFrames, int duration, String vcodec, String acodec) {
        int event_mask = 0;
        if( this.vid != vid ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_VID;
            this.vid = vid;
        }   
        if( this.aid != aid ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_AID;
            this.aid = aid;
        }   
        if( this.width != width || this.height != height ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_SIZE;
            this.width = width;
            this.height = height;
        }   
        if( this.fps != fps ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_FPS;
            this.fps = fps;
            this.frame_period = (int) ( 1000f / fps + 0.5f );
        }
        if( this.bps_stream != bps_stream || this.bps_video != bps_video || this.bps_audio != bps_audio ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_BPS;
            this.bps_stream = bps_stream;
            this.bps_video = bps_video;
            this.bps_audio = bps_audio;
        }
        if( this.videoFrames != videoFrames || this.audioFrames != audioFrames || this.duration != duration ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_LENGTH;
            this.videoFrames = videoFrames;
            this.audioFrames = audioFrames;
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
    public final State destroy(GL gl) {
        synchronized( stateLock ) {
            destroyFramePusher();
            destroyImpl(gl);
            removeAllTextureFrames(gl);
            state = State.Uninitialized;
            return state;
        }
    }
    protected abstract void destroyImpl(GL gl);

    @Override
    public final URLConnection getURLConnection() {
        return urlConn;
    }

    @Override
    public final int getVID() { return vid; }
    
    @Override
    public final int getAID() { return aid; }
    
    @Override
    public final String getVideoCodec() {
        return vcodec;
    }

    @Override
    public final String getAudioCodec() {
        return acodec;
    }

    @Override
    public final int getVideoFrames() {
        return videoFrames;
    }
    
    public final int getAudioFrames() {
        return audioFrames;
    }

    @Override
    public final int getDuration() {
        return duration;
    }
    
    @Override
    public final long getStreamBitrate() {
        return bps_stream;
    }

    @Override
    public final int getVideoBitrate() {
        return bps_video;
    }
    
    @Override
    public final int getAudioBitrate() {
        return bps_audio;
    }
    
    @Override
    public final float getFramerate() {
        return fps;
    }

    @Override
    public final int getWidth() {
        return width;
    }

    @Override
    public final int getHeight() {
        return height;
    }

    @Override
    public final String toString() {
        final float tt = getDuration() / 1000.0f;
        final String loc = ( null != urlConn ) ? urlConn.getURL().toExternalForm() : "<undefined stream>" ;
        final int freeVideoFrames = null != videoFramesFree ? videoFramesFree.size() : 0;
        final int decVideoFrames = null != videoFramesDecoded ? videoFramesDecoded.size() : 0;
        return "GLMediaPlayer["+state+", frames[p "+presentedFrameCount+", d "+decodedFrameCount+", t "+videoFrames+" ("+tt+" s)], "+
               "speed "+playSpeed+", "+bps_stream+" bps, "+
               "Texture[count "+textureCount+", free "+freeVideoFrames+", dec "+decVideoFrames+", target "+toHexString(textureTarget)+", format "+toHexString(textureFormat)+", type "+toHexString(textureType)+"], "+               
               "Video[id "+vid+", <"+vcodec+">, "+width+"x"+height+", "+fps+" fps, "+bps_video+" bps], "+
               "Audio[id "+aid+", <"+acodec+">, "+bps_audio+" bps, "+audioFrames+" frames], uri "+loc+"]";
    }
    
    @Override
    public final String getPerfString() {
        final int scr_pts = video_scr_pts + 
                            (int) ( ( System.currentTimeMillis() - video_scr_t0 ) * playSpeed );
        final int d_vpts = video_pts_last - scr_pts;
        return getPerfStringImpl( scr_pts, video_pts_last, d_vpts, getVideoDPTSAvg() );
    }
    private final String getPerfStringImpl(final int scr_pts, final int video_pts, final int d_vpts, final int video_dpts_avg_diff) {
        final float tt = getDuration() / 1000.0f;        
        final int audio_scr = (int) ( ( System.currentTimeMillis() - audio_scr_t0 ) * playSpeed );
        final int audio_pts = getAudioPTSImpl();
        final int d_apts = audio_pts - audio_scr;
        final String audioSinkInfo;
        final AudioSink audioSink = getAudioSink();
        if( null != audioSink ) {
            audioSinkInfo = "AudioSink[frames [d "+audioSink.getEnqueuedFrameCount()+", q "+audioSink.getQueuedFrameCount()+", f "+audioSink.getFreeFrameCount()+"], time "+audioSink.getQueuedTime()+", bytes "+audioSink.getQueuedByteCount()+"]";
        } else {
            audioSinkInfo = "";
        }
        final int freeVideoFrames = null != videoFramesFree ? videoFramesFree.size() : 0;
        final int decVideoFrames = null != videoFramesDecoded ? videoFramesDecoded.size() : 0;
        return state+", frames[p "+presentedFrameCount+", d "+decodedFrameCount+", t "+videoFrames+" ("+tt+" s)], "+
               "speed " + playSpeed+", vSCR "+scr_pts+", vpts "+video_pts+", dSCR["+d_vpts+", avrg "+video_dpts_avg_diff+"], "+
               "aSCR "+audio_scr+", apts "+audio_pts+" ( "+d_apts+" ), "+audioSinkInfo+
               ", Texture[count "+textureCount+", free "+freeVideoFrames+", dec "+decVideoFrames+"]";
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
    public final GLMediaEventListener[] getEventListeners() {
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