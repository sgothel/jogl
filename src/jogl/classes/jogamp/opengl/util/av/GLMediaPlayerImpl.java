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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.jogamp.nativewindow.AbstractGraphicsDevice;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.Debug;

import com.jogamp.common.net.UriQueryProps;
import com.jogamp.common.ExceptionUtils;
import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.common.util.InterruptedRuntimeException;
import com.jogamp.common.util.LFRingbuffer;
import com.jogamp.common.util.Ringbuffer;
import com.jogamp.common.util.SourcedInterruptedException;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.util.TimeFrameI;
import com.jogamp.opengl.util.av.AudioSink;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

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
    private static final int STREAM_WORKER_DELAY = Debug.getIntProperty("jogl.debug.GLMediaPlayer.StreamWorker.delay", false, 0);

    private static final String unknown = "unknown";

    private volatile State state;
    private final Object stateLock = new Object();

    private int textureCount;
    private int textureTarget;
    private int textureFormat;
    private int textureInternalFormat;
    private int textureType;
    private int texUnit;

    private int textureFragmentShaderHashCode;

    private final int[] texMinMagFilter = { GL.GL_NEAREST, GL.GL_NEAREST };
    private final int[] texWrapST = { GL.GL_CLAMP_TO_EDGE, GL.GL_CLAMP_TO_EDGE };

    /** User requested URI stream location. */
    private Uri streamLoc = null;

    /**
     * In case {@link #streamLoc} is a {@link GLMediaPlayer#CameraInputScheme},
     * {@link #cameraPath} holds the URI's path portion
     * as parsed in {@link #initStream(Uri, int, int, int)}.
     * @see #cameraProps
     */
    protected Uri.Encoded cameraPath = null;
    /** Optional camera properties, see {@link #cameraPath}. */
    protected Map<String, String> cameraProps = null;

    private volatile float playSpeed = 1.0f;
    private float audioVolume = 1.0f;

    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int vid = GLMediaPlayer.STREAM_ID_AUTO;
    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int aid = GLMediaPlayer.STREAM_ID_AUTO;
    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int width = 0;
    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int height = 0;
    /** Video avg. fps. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private float fps = 0;
    /** Video avg. frame duration in ms. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private float frame_duration = 0f;
    /** Stream bps. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int bps_stream = 0;
    /** Video bps. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int bps_video = 0;
    /** Audio bps. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int bps_audio = 0;
    /** In frames. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int videoFrames = 0;
    /** In frames. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int audioFrames = 0;
    /** In ms. Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private int duration = 0;
    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private String acodec = unknown;
    /** Shall be set by the {@link #initStreamImpl(int, int)} method implementation. */
    private String vcodec = unknown;

    private volatile int decodedFrameCount = 0;
    private int presentedFrameCount = 0;
    private int displayedFrameCount = 0;
    private volatile int video_pts_last = 0;

    /**
     * Help detect EOS, limit is {@link #MAX_FRAMELESS_MS_UNTIL_EOS}.
     * To be used either by getNextTexture(..) or StreamWorker for audio-only.
     */
    private int nullFrameCount = 0;
    private int maxNullFrameCountUntilEOS = 0;
    /**
     * Help detect EOS, limit {@value} milliseconds without a valid frame.
     */
    private static final int MAX_FRAMELESS_MS_UNTIL_EOS = 5000;
    private static final int MAX_FRAMELESS_UNTIL_EOS_DEFAULT =  MAX_FRAMELESS_MS_UNTIL_EOS / 30; // default value assuming 30fps

    /** See {@link #getAudioSink()}. Set by implementation if used from within {@link #initStreamImpl(int, int)}! */
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
    private boolean videoSCR_reset = false;

    private TextureFrame[] videoFramesOrig = null;
    private Ringbuffer<TextureFrame> videoFramesFree =  null;
    private Ringbuffer<TextureFrame> videoFramesDecoded =  null;
    private volatile TextureFrame lastFrame = null;
    /**
     * @see #isGLOriented()
     */
    private boolean isInGLOrientation = false;

    private final ArrayList<GLMediaEventListener> eventListeners = new ArrayList<GLMediaEventListener>();

    protected GLMediaPlayerImpl() {
        this.textureCount=0;
        this.textureTarget=GL.GL_TEXTURE_2D;
        this.textureFormat = GL.GL_RGBA;
        this.textureInternalFormat = GL.GL_RGBA;
        this.textureType = GL.GL_UNSIGNED_BYTE;
        this.texUnit = 0;
        this.textureFragmentShaderHashCode = 0;
        this.state = State.Uninitialized;
    }

    @Override
    public final void setTextureUnit(final int u) { texUnit = u; }

    @Override
    public final int getTextureUnit() { return texUnit; }

    @Override
    public final int getTextureTarget() { return textureTarget; }

    protected final int getTextureFormat() { return textureFormat; }

    protected final int getTextureType() { return textureType; }

    @Override
    public final int getTextureCount() { return textureCount; }

    protected final void setTextureTarget(final int target) { textureTarget=target; }
    protected final void setTextureFormat(final int internalFormat, final int format) {
        textureInternalFormat=internalFormat;
        textureFormat=format;
    }
    protected final void setTextureType(final int t) { textureType=t; }

    @Override
    public final void setTextureMinMagFilter(final int[] minMagFilter) { texMinMagFilter[0] = minMagFilter[0]; texMinMagFilter[1] = minMagFilter[1];}
    @Override
    public final int[] getTextureMinMagFilter() { return texMinMagFilter; }

    @Override
    public final void setTextureWrapST(final int[] wrapST) { texWrapST[0] = wrapST[0]; texWrapST[1] = wrapST[1];}
    @Override
    public final int[] getTextureWrapST() { return texWrapST; }

    private final void checkGLInit() {
        if(State.Uninitialized == state || State.Initialized == state ) {
            throw new IllegalStateException("GL not initialized: "+this);
        }
    }

    @Override
    public String getRequiredExtensionsShaderStub() throws IllegalStateException {
        checkGLInit();
        if(GLES2.GL_TEXTURE_EXTERNAL_OES == textureTarget) {
            return ShaderCode.createExtensionDirective(GLExtensions.OES_EGL_image_external, ShaderCode.ENABLE);
        }
        return "";
    }

    @Override
    public String getTextureSampler2DType() throws IllegalStateException {
        checkGLInit();
        switch(textureTarget) {
            case GL.GL_TEXTURE_2D:
            case GL2GL3.GL_TEXTURE_RECTANGLE:
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
    public String getTextureLookupFunctionName(final String desiredFuncName) throws IllegalStateException {
        checkGLInit();
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
        checkGLInit();
        return "";
    }

    @Override
    public final int getTextureFragmentShaderHashCode() {
        if( !isTextureAvailable() ) {
            textureFragmentShaderHashCode = 0;
            return 0;
        } else if( 0 == textureFragmentShaderHashCode ) {
            int hash = 31 + getTextureLookupFragmentShaderImpl().hashCode();
            hash = ((hash << 5) - hash) + getTextureSampler2DType().hashCode();
            textureFragmentShaderHashCode = hash;
        }
        return textureFragmentShaderHashCode;
    }

    @Override
    public final int getDecodedFrameCount() { return decodedFrameCount; }

    @Override
    public final int getPresentedFrameCount() { return presentedFrameCount; }

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

    @Override
    public final State getState() { return state; }

    protected final void setState(final State s) { state=s; }

    @Override
    public final State play() {
        synchronized( stateLock ) {
            final State preState = state;
            switch( state ) {
                case Paused:
                    if( playImpl() ) {
                        resetAVPTS();
                        if( null != audioSink ) {
                            audioSink.play(); // cont. w/ new data
                        }
                        if( null != streamWorker ) {
                            streamWorker.doResume();
                        }
                        changeState(0, State.Playing);
                    }
                default:
            }
            if(DEBUG) { System.err.println("Play: "+preState+" -> "+state+", "+toString()); }
            return state;
        }
    }
    protected abstract boolean playImpl();

    @Override
    public final State pause(final boolean flush) {
        return pauseImpl(flush, 0);
    }
    private final State pauseImpl(final boolean flush, int event_mask) {
        synchronized( stateLock ) {
            final State preState = state;
            if( State.Playing == state ) {
                event_mask = addStateEventMask(event_mask, GLMediaPlayer.State.Paused);
                setState( State.Paused );
                if( null != streamWorker ) {
                    streamWorker.doPause(true);
                }
                if( flush ) {
                    resetAVPTSAndFlush();
                } else if( null != audioSink ) {
                    audioSink.pause();
                }
                attributesUpdated( event_mask );
                if( !pauseImpl() ) {
                    play();
                }
            }
            if(DEBUG) { System.err.println("Pause: "+preState+" -> "+state+", "+toString()); }
            return state;
        }
    }
    protected abstract boolean pauseImpl();

    @Override
    public final State destroy(final GL gl) {
        return destroyImpl(gl, 0);
    }
    private final State destroyImpl(final GL gl, final int event_mask) {
        synchronized( stateLock ) {
            if( null != streamWorker ) {
                streamWorker.doStop();
                streamWorker = null;
            }
            destroyImpl(gl);
            removeAllTextureFrames(gl);
            textureCount=0;
            changeState(event_mask, State.Uninitialized);
            attachedObjects.clear();
            return state;
        }
    }
    protected abstract void destroyImpl(GL gl);

    @Override
    public final int seek(int msec) {
        synchronized( stateLock ) {
            final State preState = state;
            final int pts1;
            switch(state) {
                case Playing:
                case Paused:
                    final State _state = state;
                    setState( State.Paused );
                    if( null != streamWorker ) {
                        streamWorker.doPause(true);
                    }
                    // Adjust target ..
                    if( msec >= duration ) {
                        msec = duration - (int)Math.floor(frame_duration);
                    } else if( msec < 0 ) {
                        msec = 0;
                    }
                    pts1 = seekImpl(msec);
                    resetAVPTSAndFlush();
                    if( null != audioSink && State.Playing == _state ) {
                        audioSink.play(); // cont. w/ new data
                    }
                    if(DEBUG) {
                        System.err.println("Seek("+msec+"): "+getPerfString());
                    }
                    if( null != streamWorker ) {
                        streamWorker.doResume();
                    }
                    setState( _state );
                    break;
                default:
                    pts1 = 0;
            }
            if(DEBUG) { System.err.println("Seek("+msec+"): "+preState+" -> "+state+", "+toString()); }
            return pts1;
        }
    }
    protected abstract int seekImpl(int msec);

    @Override
    public final float getPlaySpeed() { return playSpeed; }

    @Override
    public final boolean setPlaySpeed(float rate) {
        synchronized( stateLock ) {
            final float preSpeed = playSpeed;
            boolean res = false;
            if(State.Uninitialized != state ) {
                if( rate > 0.01f ) {
                    if( Math.abs(1.0f - rate) < 0.01f ) {
                        rate = 1.0f;
                    }
                    if( setPlaySpeedImpl(rate) ) {
                        resetAVPTS();
                        playSpeed = rate;
                        res = true;
                    }
                }
            }
            if(DEBUG) { System.err.println("setPlaySpeed("+rate+"): "+state+", "+preSpeed+" -> "+playSpeed+", "+toString()); }
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
    protected boolean setPlaySpeedImpl(final float rate) {
        if( null != audioSink ) {
            audioSinkPlaySpeedSet = audioSink.setPlaySpeed(rate);
        }
        // still true, even if audioSink rejects command since we deal w/ video sync
        // and AudioSink w/ audioSinkPlaySpeedSet at enqueueData(..).
        return true;
    }

    @Override
    public final float getAudioVolume() {
        getAudioVolumeImpl();
        return audioVolume;
    }
    /**
     * Override if not using AudioSink, or AudioSink's {@link AudioSink#getVolume()} is not sufficient!
     */
    protected void getAudioVolumeImpl() {
        if( null != audioSink ) {
            audioVolume = audioSink.getVolume();
        }
    }

    @Override
    public boolean setAudioVolume(float v) {
        synchronized( stateLock ) {
            final float preVolume = audioVolume;
            boolean res = false;
            if(State.Uninitialized != state ) {
                if( Math.abs(v) < 0.01f ) {
                    v = 0.0f;
                } else if( Math.abs(1.0f - v) < 0.01f ) {
                    v = 1.0f;
                }
                if( setAudioVolumeImpl(v) ) {
                    audioVolume = v;
                    res = true;
                }
            }
            if(DEBUG) { System.err.println("setAudioVolume("+v+"): "+state+", "+preVolume+" -> "+audioVolume+", "+toString()); }
            return res;
        }
    }
    /**
     * Override if not using AudioSink, or AudioSink's {@link AudioSink#setVolume(float)} is not sufficient!
     */
    protected boolean setAudioVolumeImpl(final float v) {
        if( null != audioSink ) {
            return audioSink.setVolume(v);
        }
        // still true, even if audioSink rejects command ..
        return true;
    }

    @Override
    public final void initStream(final Uri streamLoc, final int vid, final int aid, final int reqTextureCount) throws IllegalStateException, IllegalArgumentException {
        synchronized( stateLock ) {
            if(State.Uninitialized != state) {
                throw new IllegalStateException("Instance not in state unintialized: "+this);
            }
            if(null == streamLoc) {
                throw new IllegalArgumentException("streamLock is null");
            }
            if( STREAM_ID_NONE != vid ) {
                textureCount = validateTextureCount(reqTextureCount);
                if( textureCount < TEXTURE_COUNT_MIN ) {
                    throw new InternalError("Validated texture count < "+TEXTURE_COUNT_MIN+": "+textureCount);
                }
            } else {
                textureCount = 0;
            }
            decodedFrameCount = 0;
            presentedFrameCount = 0;
            displayedFrameCount = 0;
            nullFrameCount = 0;
            maxNullFrameCountUntilEOS = MAX_FRAMELESS_UNTIL_EOS_DEFAULT;
            this.streamLoc = streamLoc;

            // Pre-parse for camera-input scheme
            cameraPath = null;
            cameraProps = null;
            final Uri.Encoded streamLocScheme = streamLoc.scheme;
            if( null != streamLocScheme && streamLocScheme.equals(CameraInputScheme) ) {
                final Uri.Encoded rawPath = streamLoc.path;
                if( null != rawPath && rawPath.length() > 0 ) {
                    // cut-off root fwd-slash
                    cameraPath = rawPath.substring(1);
                    final UriQueryProps props = UriQueryProps.create(streamLoc, ';');
                    cameraProps = props.getProperties();
                } else {
                    throw new IllegalArgumentException("Camera path is empty: "+streamLoc.toString());
                }
            }

            this.vid = vid;
            this.aid = aid;
            new InterruptSource.Thread() {
                public void run() {
                    try {
                        // StreamWorker may be used, see API-doc of StreamWorker
                        initStreamImpl(vid, aid);
                    } catch (final Throwable t) {
                        streamErr = new StreamException(t.getClass().getSimpleName()+" while initializing: "+GLMediaPlayerImpl.this.toString(), t);
                        changeState(GLMediaEventListener.EVENT_CHANGE_ERR, GLMediaPlayer.State.Uninitialized);
                    } // also initializes width, height, .. etc
                }
            }.start();
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
    protected abstract void initStreamImpl(int vid, int aid) throws Exception;

    @Override
    public final StreamException getStreamException() {
        final StreamException e;
        synchronized( stateLock ) {
            e = streamErr;
            streamErr = null;
        }
        return e;
    }

    @Override
    public final void initGL(final GL gl) throws IllegalStateException, StreamException, GLException {
        synchronized( stateLock ) {
            if(State.Initialized != state ) {
                throw new IllegalStateException("Stream not in state initialized: "+this);
            }
            if( null != streamWorker ) {
                final StreamException streamInitErr = getStreamException();
                if( null != streamInitErr ) {
                    streamWorker = null; // already terminated!
                    destroy(null);
                    throw streamInitErr;
                }
            }
            try {
                if( STREAM_ID_NONE != vid ) {
                    removeAllTextureFrames(gl);
                    initGLImpl(gl);
                    if(DEBUG) {
                        System.err.println("initGLImpl.X "+this);
                    }
                    videoFramesOrig = createTexFrames(gl, textureCount);
                    if( TEXTURE_COUNT_MIN == textureCount ) {
                        videoFramesFree = null;
                        videoFramesDecoded = null;
                        lastFrame = videoFramesOrig[0];
                    } else {
                        videoFramesFree = new LFRingbuffer<TextureFrame>(videoFramesOrig);
                        videoFramesDecoded = new LFRingbuffer<TextureFrame>(TextureFrame[].class, textureCount);
                        lastFrame = videoFramesFree.getBlocking( );
                    }
                    if( null != streamWorker ) {
                        streamWorker.initGL(gl);
                    }
                } else {
                    removeAllTextureFrames(null);
                    initGLImpl(null);
                    setTextureFormat(-1, -1);
                    setTextureType(-1);
                    videoFramesOrig = null;
                    videoFramesFree = null;
                    videoFramesDecoded = null;
                    lastFrame = null;
                }
                changeState(0, State.Paused);
            } catch (final Throwable t) {
                destroyImpl(gl, GLMediaEventListener.EVENT_CHANGE_ERR); // -> GLMediaPlayer.State.Uninitialized
                throw new GLException("Error initializing GL resources", t);
            }
        }
    }
    /**
     * Shall initialize all GL related resources, if not audio-only.
     * <p>
     * Shall also take care of {@link AudioSink} initialization if appropriate.
     * </p>
     * @param gl null for audio-only, otherwise a valid and current GL object.
     * @throws IOException
     * @throws GLException
     */
    protected abstract void initGLImpl(GL gl) throws IOException, GLException;

    /**
     * Returns the validated number of textures to be handled.
     * <p>
     * Default is {@link #TEXTURE_COUNT_DEFAULT} minimum textures, if <code>desiredTextureCount</code>
     * is < {@link #TEXTURE_COUNT_MIN}, {@link #TEXTURE_COUNT_MIN} is returned.
     * </p>
     * <p>
     * Implementation must at least return a texture count of {@link #TEXTURE_COUNT_MIN}, <i>two</i>, the last texture and the decoding texture.
     * </p>
     */
    protected int validateTextureCount(final int desiredTextureCount) {
        return desiredTextureCount < TEXTURE_COUNT_MIN ? TEXTURE_COUNT_MIN : desiredTextureCount;
    }

    protected TextureFrame[] createTexFrames(final GL gl, final int count) {
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

    protected final Texture createTexImageImpl(final GL gl, final int texName, final int tWidth, final int tHeight) {
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
                    throw new RuntimeException("Couldn't create TexImage2D RGBA "+tWidth+"x"+tHeight+", target "+toHexString(textureTarget)+
                                   ", ifmt "+toHexString(textureInternalFormat)+", fmt "+toHexString(textureFormat)+", type "+toHexString(textureType)+
                                   ", err "+toHexString(err));
                }
            }
            if(DEBUG) {
                System.err.println("Created TexImage2D RGBA "+tWidth+"x"+tHeight+", target "+toHexString(textureTarget)+
                                   ", ifmt "+toHexString(textureInternalFormat)+", fmt "+toHexString(textureFormat)+", type "+toHexString(textureType));
            }
        }
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MIN_FILTER, texMinMagFilter[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_MAG_FILTER, texMinMagFilter[1]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_S, texWrapST[0]);
        gl.glTexParameteri(textureTarget, GL.GL_TEXTURE_WRAP_T, texWrapST[1]);

        return new Texture(texName, textureTarget,
                           tWidth, tHeight, width,  height, !isInGLOrientation);
    }

    protected void destroyTexFrame(final GL gl, final TextureFrame frame) {
        frame.getTexture().destroy(gl);
    }

    @Override
    public final boolean isTextureAvailable() {
        return State.Paused == state || State.Playing == state;
    }

    @Override
    public final TextureFrame getLastTexture() throws IllegalStateException {
        if( State.Paused != state && State.Playing != state ) {
            throw new IllegalStateException("Instance not paused or playing: "+this);
        }
        return lastFrame;
    }

    private final void removeAllTextureFrames(final GL gl) {
        final TextureFrame[] texFrames = videoFramesOrig;
        videoFramesOrig = null;
        videoFramesFree = null;
        videoFramesDecoded = null;
        lastFrame = null;
        if( null != texFrames ) {
            for(int i=0; i<texFrames.length; i++) {
                final TextureFrame frame = texFrames[i];
                if(null != frame) {
                    if( null != gl ) {
                        destroyTexFrame(gl, frame);
                    }
                    texFrames[i] = null;
                }
                if( DEBUG ) {
                    System.err.println(Thread.currentThread().getName()+"> Clear TexFrame["+i+"]: "+frame+" -> null");
                }
            }
        }
    }

    private TextureFrame cachedFrame = null;
    private long lastTimeMillis = 0;

    private final boolean[] stGotVFrame = { false };

    @Override
    public final TextureFrame getNextTexture(final GL gl) throws IllegalStateException {
        synchronized( stateLock ) {
            if( State.Paused != state && State.Playing != state ) {
                throw new IllegalStateException("Instance not paused or playing: "+this);
            }
            if(State.Playing == state) {
                boolean dropFrame = false;
                try {
                    do {
                        final boolean droppedFrame;
                        if( dropFrame ) {
                            presentedFrameCount--;
                            dropFrame = false;
                            droppedFrame = true;
                        } else {
                            droppedFrame = false;
                        }
                        final boolean playCached = null != cachedFrame;
                        final int video_pts;
                        final boolean hasVideoFrame;
                        TextureFrame nextFrame;
                        if( playCached ) {
                            nextFrame = cachedFrame;
                            cachedFrame = null;
                            presentedFrameCount--;
                            video_pts = nextFrame.getPTS();
                            hasVideoFrame = true;
                        } else {
                            if( null != videoFramesDecoded ) {
                                // multi-threaded and video available
                                nextFrame = videoFramesDecoded.get();
                                if( null != nextFrame ) {
                                    video_pts = nextFrame.getPTS();
                                    hasVideoFrame = true;
                                } else {
                                    video_pts = TimeFrameI.INVALID_PTS;
                                    hasVideoFrame = false;
                                }
                            } else {
                                // single-threaded or audio-only
                                video_pts = getNextSingleThreaded(gl, lastFrame, stGotVFrame);
                                nextFrame = lastFrame;
                                hasVideoFrame = stGotVFrame[0];
                            }
                        }
                        final long currentTimeMillis = Platform.currentTimeMillis();

                        if( TimeFrameI.END_OF_STREAM_PTS == video_pts ||
                            ( duration > 0 && duration <= video_pts ) || maxNullFrameCountUntilEOS <= nullFrameCount )
                        {
                            // EOS
                            if( DEBUG ) {
                                System.err.println( "AV-EOS (getNextTexture): EOS_PTS "+(TimeFrameI.END_OF_STREAM_PTS == video_pts)+", "+this);
                            }
                            pauseImpl(true, GLMediaEventListener.EVENT_CHANGE_EOS);

                        } else if( TimeFrameI.INVALID_PTS == video_pts ) { // no audio or video frame
                            if( null == videoFramesDecoded || !videoFramesDecoded.isEmpty() ) {
                                nullFrameCount++;
                            }
                            if( DEBUG ) {
                                final int audio_pts = getAudioPTSImpl();
                                final int audio_scr = (int) ( ( currentTimeMillis - audio_scr_t0 ) * playSpeed );
                                final int d_apts;
                                if( audio_pts != TimeFrameI.INVALID_PTS ) {
                                    d_apts = audio_pts - audio_scr;
                                } else {
                                    d_apts = 0;
                                }
                                final int video_scr = video_scr_pts + (int) ( ( currentTimeMillis - video_scr_t0 ) * playSpeed );
                                final int d_vpts = video_pts - video_scr;
                                System.err.println( "AV~: dT "+(currentTimeMillis-lastTimeMillis)+", nullFrames "+nullFrameCount+
                                        getPerfStringImpl( video_scr, video_pts, d_vpts, audio_scr, audio_pts, d_apts, 0 ) + ", droppedFrame "+droppedFrame);
                            }
                        } else { // valid pts: has audio or video frame
                            nullFrameCount=0;

                            if( hasVideoFrame ) { // has video frame
                                presentedFrameCount++;

                                final int audio_pts = getAudioPTSImpl();
                                final int audio_scr = (int) ( ( currentTimeMillis - audio_scr_t0 ) * playSpeed );
                                final int d_apts;
                                if( audio_pts != TimeFrameI.INVALID_PTS ) {
                                    d_apts = audio_pts - audio_scr;
                                } else {
                                    d_apts = 0;
                                }

                                final int frame_period_last = video_pts - video_pts_last; // rendering loop interrupted ?
                                if( videoSCR_reset || frame_period_last > frame_duration*10 ) {
                                    videoSCR_reset = false;
                                    video_scr_t0 = currentTimeMillis;
                                    video_scr_pts = video_pts;
                                }
                                final int video_scr = video_scr_pts + (int) ( ( currentTimeMillis - video_scr_t0 ) * playSpeed );
                                final int d_vpts = video_pts - video_scr;
                                // final int d_avpts = d_vpts - d_apts;
                                if( -VIDEO_DPTS_MAX > d_vpts || d_vpts > VIDEO_DPTS_MAX ) {
                                // if( -VIDEO_DPTS_MAX > d_avpts || d_avpts > VIDEO_DPTS_MAX ) {
                                    if( DEBUG ) {
                                        System.err.println( "AV*: dT "+(currentTimeMillis-lastTimeMillis)+", "+
                                                getPerfStringImpl( video_scr, video_pts, d_vpts, audio_scr, audio_pts, d_apts, 0 ) + ", "+nextFrame+", playCached " + playCached+ ", dropFrame "+dropFrame);
                                    }
                                } else {
                                    final int dpy_den = displayedFrameCount > 0 ? displayedFrameCount : 1;
                                    final int avg_dpy_duration = ( (int) ( currentTimeMillis - video_scr_t0 ) ) / dpy_den ; // ms/f
                                    final int maxVideoDelay = Math.min(avg_dpy_duration, MAXIMUM_VIDEO_ASYNC);
                                    video_dpts_count++;
                                    // video_dpts_cum = d_avpts + VIDEO_DPTS_COEFF * video_dpts_cum;
                                    video_dpts_cum = d_vpts + VIDEO_DPTS_COEFF * video_dpts_cum;
                                    final int video_dpts_avg_diff = video_dpts_count >= VIDEO_DPTS_NUM ? getVideoDPTSAvg() : 0;
                                    final int dt = (int) ( video_dpts_avg_diff / playSpeed + 0.5f );
                                    // final int dt = (int) ( d_vpts  / playSpeed + 0.5f );
                                    // final int dt = (int) ( d_avpts / playSpeed + 0.5f );
                                    final TextureFrame _nextFrame = nextFrame;
                                    if( dt > maxVideoDelay ) {
                                        cachedFrame = nextFrame;
                                        nextFrame = null;
                                    } else if ( !droppedFrame && dt < -maxVideoDelay && null != videoFramesDecoded && videoFramesDecoded.size() > 0 ) {
                                        // only drop if prev. frame has not been dropped and
                                        // frame is too late and one decoded frame is already available.
                                        dropFrame = true;
                                    }
                                    video_pts_last = video_pts;
                                    if( DEBUG ) {
                                        System.err.println( "AV_: dT "+(currentTimeMillis-lastTimeMillis)+", "+
                                                getPerfStringImpl( video_scr, video_pts, d_vpts,
                                                                   audio_scr, audio_pts, d_apts,
                                                                   video_dpts_avg_diff ) +
                                                                   ", avg dpy-fps "+avg_dpy_duration+" ms/f, maxD "+maxVideoDelay+" ms, "+_nextFrame+", playCached " + playCached + ", dropFrame "+dropFrame);
                                    }
                                }
                            } // has video frame
                        } // has audio or video frame

                        if( null != videoFramesFree && null != nextFrame ) {
                            // Had frame and not single threaded ? (TEXTURE_COUNT_MIN < textureCount)
                            final TextureFrame _lastFrame = lastFrame;
                            lastFrame = nextFrame;
                            if( null != _lastFrame ) {
                                videoFramesFree.putBlocking(_lastFrame);
                            }
                        }
                        lastTimeMillis = currentTimeMillis;
                    } while( dropFrame );
                } catch (final InterruptedException e) {
                    e.printStackTrace();
                }
            }
            displayedFrameCount++;
            return lastFrame;
        }
    }
    protected void preNextTextureImpl(final GL gl) {}
    protected void postNextTextureImpl(final GL gl) {}
    /**
     * Process stream until the next video frame, i.e. {@link TextureFrame}, has been reached.
     * Audio frames, i.e. {@link AudioSink.AudioFrame}, shall be handled in the process.
     * <p>
     * Video frames shall be ignored, if {@link #getVID()} is {@link #STREAM_ID_NONE}.
     * </p>
     * <p>
     * Audio frames shall be ignored, if {@link #getAID()} is {@link #STREAM_ID_NONE}.
     * </p>
     * <p>
     * Method may be invoked on the <a href="#streamworker"><i>StreamWorker</i> decoding thread</a>.
     * </p>
     * <p>
     * Implementation shall care of OpenGL synchronization as required, e.g. glFinish()/glFlush()!
     * </p>
     * @param gl valid and current GL instance, shall be <code>null</code> for audio only.
     * @param nextFrame the {@link TextureFrame} to store the video PTS and texture data,
     *                  shall be <code>null</code> for audio only.
     * @return the last processed video PTS value, maybe {@link TimeFrameI#INVALID_PTS} if video frame is invalid or n/a.
     *         Will be {@link TimeFrameI#END_OF_STREAM_PTS} if end of stream reached.
     * @throws InterruptedException if waiting for next frame fails
     */
    protected abstract int getNextTextureImpl(GL gl, TextureFrame nextFrame) throws InterruptedException;

    protected final int getNextSingleThreaded(final GL gl, final TextureFrame nextFrame, final boolean[] gotVFrame) throws InterruptedException {
        final int pts;
        if( STREAM_ID_NONE != vid ) {
            preNextTextureImpl(gl);
            pts = getNextTextureImpl(gl, nextFrame);
            postNextTextureImpl(gl);
            if( TimeFrameI.INVALID_PTS != pts ) {
                newFrameAvailable(nextFrame, Platform.currentTimeMillis());
                gotVFrame[0] = true;
            } else {
                gotVFrame[0] = false;
            }
        } else {
            // audio only
            pts = getNextTextureImpl(null, null);
            gotVFrame[0] = false;
        }
        return pts;
    }


    /**
     * {@inheritDoc}
     * <p>
     * Note: All {@link AudioSink} operations are performed from {@link GLMediaPlayerImpl},
     * i.e. {@link #play()}, {@link #pause(boolean)}, {@link #seek(int)}, {@link #setPlaySpeed(float)}, {@link #getAudioPTS()}.
     * </p>
     * <p>
     * Implementations using an {@link AudioSink} shall write it's instance to {@link #audioSink}
     * from within their {@link #initStreamImpl(int, int)} implementation.
     * </p>
     */
    @Override
    public final AudioSink getAudioSink() { return audioSink; }

    /**
     * To be called from implementation at 1st PTS after start
     * w/ current pts value in milliseconds.
     * @param audio_scr_t0
     */
    protected void setFirstAudioPTS2SCR(final int pts) {
        if( audioSCR_reset ) {
            audio_scr_t0 = Platform.currentTimeMillis() - pts;
            audioSCR_reset = false;
        }
    }
    private void flushAllVideoFrames() {
        if( null != videoFramesFree ) {
            videoFramesFree.resetFull(videoFramesOrig);
            lastFrame = videoFramesFree.get();
            if( null == lastFrame ) { throw new InternalError("XXX"); }
            videoFramesDecoded.clear();
        }
        cachedFrame = null;
    }
    private void resetAVPTSAndFlush() {
        video_dpts_cum = 0;
        video_dpts_count = 0;
        resetAVPTS();
        flushAllVideoFrames();
        if( null != audioSink ) {
            audioSink.flush();
        }
    }
    private void resetAVPTS() {
        nullFrameCount = 0;
        presentedFrameCount = 0;
        displayedFrameCount = 0;
        decodedFrameCount = 0;
        audioSCR_reset = true;
        videoSCR_reset = true;
    }
    private final int getVideoDPTSAvg() {
        return (int) ( video_dpts_cum * (1.0f - VIDEO_DPTS_COEFF) + 0.5f );
    }

    private final void newFrameAvailable(final TextureFrame frame, final long currentTimeMillis) {
        decodedFrameCount++; // safe: only written-to either from stream-worker or user thread
        if( 0 == frame.getDuration() ) { // patch frame duration if not set already
            frame.setDuration( (int) frame_duration );
        }
        synchronized(eventListenersLock) {
            for(final Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                i.next().newFrameAvailable(this, frame, currentTimeMillis);
            }
        }
    }

    /**
     * After {@link GLMediaPlayerImpl#initStreamImpl(int, int) initStreamImpl(..)} is completed via
     * {@link GLMediaPlayerImpl#updateAttributes(int, int, int, int, int, int, int, float, int, int, int, String, String) updateAttributes(..)},
     * the latter decides whether StreamWorker is being used.
     */
    class StreamWorker extends InterruptSource.Thread {
        private volatile boolean isRunning = false;
        private volatile boolean isActive = false;
        private volatile boolean isBlocked = false;

        private volatile boolean shallPause = true;
        private volatile boolean shallStop = false;

        private volatile GLContext sharedGLCtx = null;
        private boolean sharedGLCtxCurrent = false;
        private GLDrawable dummyDrawable = null;

        /**
         * Starts this daemon thread,
         * <p>
         * This thread pauses after it's started!
         * </p>
         **/
        StreamWorker() {
            setDaemon(true);
            synchronized(this) {
                start();
                try {
                    this.notifyAll();  // wake-up startup-block
                    while( !isRunning && !shallStop ) {
                        this.wait();  // wait until started
                    }
                } catch (final InterruptedException e) {
                    throw new InterruptedRuntimeException(e);
                }
            }
        }

        private void makeCurrent(final GLContext ctx) {
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
                    } catch (final GLException gle) {
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

        public final synchronized void initGL(final GL gl) {
            final GLContext glCtx = gl.getContext();
            final boolean glCtxCurrent = glCtx.isCurrent();
            final GLProfile glp = gl.getGLProfile();
            final GLDrawableFactory factory = GLDrawableFactory.getFactory(glp);
            final AbstractGraphicsDevice device = glCtx.getGLDrawable().getNativeSurface().getGraphicsConfiguration().getScreen().getDevice();
            dummyDrawable = factory.createDummyDrawable(device, true, glCtx.getGLDrawable().getChosenGLCapabilities(), null); // own device!
            dummyDrawable.setRealized(true);
            sharedGLCtx = dummyDrawable.createContext(glCtx);
            makeCurrent(sharedGLCtx);
            if( glCtxCurrent ) {
                makeCurrent(glCtx);
            } else {
                sharedGLCtx.release();
            }
        }
        public final synchronized void doPause(final boolean waitUntilDone) {
            if( isActive ) {
                shallPause = true;
                if( java.lang.Thread.currentThread() != this ) {
                    if( isBlocked && isActive ) {
                        this.interrupt();
                    }
                    if( waitUntilDone ) {
                        try {
                            while( isActive && isRunning ) {
                                this.wait(); // wait until paused
                            }
                        } catch (final InterruptedException e) {
                            throw new InterruptedRuntimeException(e);
                        }
                    }
                }
            }
        }
        public final synchronized void doResume() {
            if( isRunning && !isActive ) {
                shallPause = false;
                if( java.lang.Thread.currentThread() != this ) {
                    try {
                        this.notifyAll();  // wake-up pause-block
                        while( !isActive && !shallPause && isRunning ) {
                            this.wait(); // wait until resumed
                        }
                    } catch (final InterruptedException e) {
                        final InterruptedException e2 = SourcedInterruptedException.wrap(e);
                        doPause(false);
                        throw new InterruptedRuntimeException(e2);
                    }
                }
            }
        }
        public final synchronized void doStop() {
            if( isRunning ) {
                shallStop = true;
                if( java.lang.Thread.currentThread() != this ) {
                    if( isBlocked && isRunning ) {
                        this.interrupt();
                    }
                    try {
                        this.notifyAll();  // wake-up pause-block (opt)
                        while( isRunning ) {
                            this.wait();  // wait until stopped
                        }
                    } catch (final InterruptedException e) {
                        throw new InterruptedRuntimeException(e);
                    }
                }
            }
        }
        public final boolean isRunning() { return isRunning; }
        public final boolean isActive() { return isActive; }

        @Override
        public final void run() {
            setName(getName()+"-StreamWorker_"+StreamWorkerInstanceId);
            StreamWorkerInstanceId++;

            synchronized ( this ) {
                isRunning = true;
                this.notifyAll(); // wake-up ctor()
            }

            while( !shallStop ) {
                TextureFrame nextFrame = null;
                try {
                    if( shallPause ) {
                        synchronized ( this ) {
                            if( sharedGLCtxCurrent ) {
                                postNextTextureImpl(sharedGLCtx.getGL());
                                sharedGLCtx.release();
                            }
                            while( shallPause && !shallStop ) {
                                isActive = false;
                                this.notifyAll(); // wake-up doPause()
                                try {
                                    this.wait();  // wait until resumed
                                } catch (final InterruptedException e) {
                                    if( !shallPause ) {
                                        throw SourcedInterruptedException.wrap(e);
                                    }
                                }
                            }
                            if( sharedGLCtxCurrent ) {
                                makeCurrent(sharedGLCtx);
                                preNextTextureImpl(sharedGLCtx.getGL());
                            }
                            isActive = true;
                            this.notifyAll(); // wake-up doResume()
                        }
                    }
                    if( !sharedGLCtxCurrent && null != sharedGLCtx ) {
                        synchronized ( this ) {
                            if( null != sharedGLCtx ) {
                                makeCurrent( sharedGLCtx );
                                preNextTextureImpl(sharedGLCtx.getGL());
                                sharedGLCtxCurrent = true;
                            }
                            if( null == videoFramesFree ) {
                                throw new InternalError("XXX videoFramesFree is null");
                            }
                        }
                    }

                    if( !shallStop ) {
                        isBlocked = true;
                        final GL gl;
                        if( STREAM_ID_NONE != vid ) {
                            nextFrame = videoFramesFree.getBlocking();
                            nextFrame.setPTS( TimeFrameI.INVALID_PTS ); // mark invalid until processed!
                            gl = sharedGLCtx.getGL();
                        } else {
                            gl = null;
                        }
                        isBlocked = false;
                        final int vPTS = getNextTextureImpl(gl, nextFrame);
                        boolean audioEOS = false;
                        if( TimeFrameI.INVALID_PTS != vPTS ) {
                            if( null != nextFrame ) {
                                if( STREAM_WORKER_DELAY > 0 ) {
                                    java.lang.Thread.sleep(STREAM_WORKER_DELAY);
                                }
                                if( !videoFramesDecoded.put(nextFrame) ) {
                                    throw new InternalError("XXX: free "+videoFramesFree+", decoded "+videoFramesDecoded+", "+GLMediaPlayerImpl.this);
                                }
                                newFrameAvailable(nextFrame, Platform.currentTimeMillis());
                                nextFrame = null;
                            } else {
                                // audio only
                                if( TimeFrameI.END_OF_STREAM_PTS == vPTS || ( duration > 0 && duration < vPTS ) ) {
                                    audioEOS = true;
                                } else {
                                    nullFrameCount = 0;
                                }
                            }
                        } else if( null == nextFrame ) {
                            // audio only
                            audioEOS = maxNullFrameCountUntilEOS <= nullFrameCount;
                            if( null == audioSink || 0 == audioSink.getEnqueuedFrameCount() ) {
                                nullFrameCount++;
                            }
                        }
                        if( audioEOS ) {
                            // state transition incl. notification
                            synchronized ( this ) {
                                shallPause = true;
                                isActive = false;
                                this.notifyAll(); // wake-up potential do*()
                            }
                            if( DEBUG ) {
                                System.err.println( "AV-EOS (StreamWorker): EOS_PTS "+(TimeFrameI.END_OF_STREAM_PTS == vPTS)+", "+GLMediaPlayerImpl.this);
                            }
                            pauseImpl(true, GLMediaEventListener.EVENT_CHANGE_EOS);
                        }
                    }
                } catch (final InterruptedException e) {
                    if( !isBlocked ) { // !shallStop && !shallPause
                        streamErr = new StreamException("InterruptedException while decoding: "+GLMediaPlayerImpl.this.toString(),
                                                        SourcedInterruptedException.wrap(e));
                    }
                    isBlocked = false;
                } catch (final Throwable t) {
                    streamErr = new StreamException(t.getClass().getSimpleName()+" while decoding: "+GLMediaPlayerImpl.this.toString(), t);
                } finally {
                    if( null != nextFrame ) { // put back
                        videoFramesFree.put(nextFrame);
                    }
                    if( null != streamErr ) {
                        if( DEBUG ) {
                            ExceptionUtils.dumpThrowable("handled", streamErr);
                        }
                        // state transition incl. notification
                        synchronized ( this ) {
                            shallPause = true;
                            isActive = false;
                            this.notifyAll(); // wake-up potential do*()
                        }
                        pauseImpl(true, GLMediaEventListener.EVENT_CHANGE_ERR);
                    }
                }
            }
            synchronized ( this ) {
                if( sharedGLCtxCurrent ) {
                    postNextTextureImpl(sharedGLCtx.getGL());
                }
                destroySharedGL();
                isRunning = false;
                isActive = false;
                this.notifyAll(); // wake-up doStop()
            }
        }
    }
    static int StreamWorkerInstanceId = 0;
    private volatile StreamWorker streamWorker = null;
    private volatile StreamException streamErr = null;

    protected final int addStateEventMask(int event_mask, final State newState) {
        if( state != newState ) {
            switch( newState ) {
                case Uninitialized:
                    event_mask |= GLMediaEventListener.EVENT_CHANGE_UNINIT;
                    break;
                case Initialized:
                    event_mask |= GLMediaEventListener.EVENT_CHANGE_INIT;
                    break;
                case Playing:
                    event_mask |= GLMediaEventListener.EVENT_CHANGE_PLAY;
                    break;
                case Paused:
                    event_mask |= GLMediaEventListener.EVENT_CHANGE_PAUSE;
                    break;
            }
        }
        return event_mask;
    }

    protected final void attributesUpdated(final int event_mask) {
        if( 0 != event_mask ) {
            final long now = Platform.currentTimeMillis();
            synchronized(eventListenersLock) {
                for(final Iterator<GLMediaEventListener> i = eventListeners.iterator(); i.hasNext(); ) {
                    i.next().attributesChanged(this, event_mask, now);
                }
            }
        }
    }

    protected final void changeState(int event_mask, final State newState) {
        event_mask = addStateEventMask(event_mask, newState);
        if( 0 != event_mask ) {
            setState( newState );
            if( !isTextureAvailable() ) {
                textureFragmentShaderHashCode = 0;
            }
            attributesUpdated( event_mask );
        }
    }

    /**
     * Called initially by {@link #initStreamImpl(int, int)}, which
     * is called off-thread by {@link #initStream(Uri, int, int, int)}.
     * <p>
     * The latter catches an occurring exception and set the state delivers the error events.
     * </p>
     * <p>
     * Further calls are issues off-thread by the decoder implementation.
     * </p>
     */
    protected final void updateAttributes(int vid, final int aid, final int width, final int height, final int bps_stream,
                                          final int bps_video, final int bps_audio, final float fps,
                                          final int videoFrames, final int audioFrames, final int duration, final String vcodec, final String acodec) {
        int event_mask = 0;
        final boolean wasUninitialized = state == State.Uninitialized;

        if( wasUninitialized ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_INIT;
            setState( State.Initialized );
        }
        if( STREAM_ID_AUTO == vid ) {
            vid = STREAM_ID_NONE;
        }
        if( this.vid != vid ) {
            event_mask |= GLMediaEventListener.EVENT_CHANGE_VID;
            this.vid = vid;
        }
        if( STREAM_ID_AUTO == vid ) {
            vid = STREAM_ID_NONE;
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
            if( 0 != fps ) {
                this.frame_duration = 1000f / fps;
                final int fdurI = (int)this.frame_duration;
                if( 0 < fdurI ) {
                    this.maxNullFrameCountUntilEOS = MAX_FRAMELESS_MS_UNTIL_EOS / fdurI;
                } else {
                    this.maxNullFrameCountUntilEOS = MAX_FRAMELESS_UNTIL_EOS_DEFAULT;
                }
            } else {
                this.frame_duration = 0;
                this.maxNullFrameCountUntilEOS = MAX_FRAMELESS_UNTIL_EOS_DEFAULT;
            }
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
        if( wasUninitialized ) {
            if( null != streamWorker ) {
                throw new InternalError("XXX: StreamWorker not null - "+this);
            }
            if( TEXTURE_COUNT_MIN < textureCount || STREAM_ID_NONE == vid ) { // Enable StreamWorker for 'audio only' as well (Bug 918).
                streamWorker = new StreamWorker();
            }
            if( DEBUG ) {
                System.err.println("XXX Initialize @ updateAttributes: "+this);
            }
        }
        attributesUpdated(event_mask);
    }

    protected void setIsGLOriented(final boolean isGLOriented) {
        if( isInGLOrientation != isGLOriented ) {
            if( DEBUG ) {
                System.err.println("XXX gl-orient "+isInGLOrientation+" -> "+isGLOriented);
            }
            isInGLOrientation = isGLOriented;
            if( null != videoFramesOrig ) {
                for(int i=0; i<videoFramesOrig.length; i++) {
                    videoFramesOrig[i].getTexture().setMustFlipVertically(!isGLOriented);
                }
                attributesUpdated(GLMediaEventListener.EVENT_CHANGE_SIZE);
            }
        }
    }

    @Override
    public final Uri getUri() { return streamLoc; }

    @Override
    public final int getVID() { return vid; }

    @Override
    public final int getAID() { return aid; }

    @Override
    public final String getVideoCodec() { return vcodec; }

    @Override
    public final String getAudioCodec() { return acodec; }

    @Override
    public final int getVideoFrames() { return videoFrames; }

    @Override
    public final int getAudioFrames() { return audioFrames; }

    @Override
    public final int getDuration() { return duration; }

    @Override
    public final long getStreamBitrate() { return bps_stream; }

    @Override
    public final int getVideoBitrate() { return bps_video; }

    @Override
    public final int getAudioBitrate() { return bps_audio; }

    @Override
    public final float getFramerate() { return fps; }

    @Override
    public final boolean isGLOriented() { return isInGLOrientation; }

    @Override
    public final int getWidth() { return width; }

    @Override
    public final int getHeight() { return height; }

    @Override
    public final String toString() {
        final float tt = getDuration() / 1000.0f;
        final String loc = ( null != streamLoc ) ? streamLoc.toString() : "<undefined stream>" ;
        final int freeVideoFrames = null != videoFramesFree ? videoFramesFree.size() : 0;
        final int decVideoFrames = null != videoFramesDecoded ? videoFramesDecoded.size() : 0;
        final int video_scr = video_scr_pts + (int) ( ( Platform.currentTimeMillis() - video_scr_t0 ) * playSpeed );
        final String camPath = null != cameraPath ? ", camera: "+cameraPath : "";
        return getClass().getSimpleName()+"["+state+", vSCR "+video_scr+", frames[p "+presentedFrameCount+", d "+decodedFrameCount+", t "+videoFrames+" ("+tt+" s), z "+nullFrameCount+" / "+maxNullFrameCountUntilEOS+"], "+
               "speed "+playSpeed+", "+bps_stream+" bps, hasSW "+(null!=streamWorker)+
               ", Texture[count "+textureCount+", free "+freeVideoFrames+", dec "+decVideoFrames+", tagt "+toHexString(textureTarget)+", ifmt "+toHexString(textureInternalFormat)+", fmt "+toHexString(textureFormat)+", type "+toHexString(textureType)+"], "+
               "Video[id "+vid+", <"+vcodec+">, "+width+"x"+height+", glOrient "+isInGLOrientation+", "+fps+" fps, "+frame_duration+" fdur, "+bps_video+" bps], "+
               "Audio[id "+aid+", <"+acodec+">, "+bps_audio+" bps, "+audioFrames+" frames], uri "+loc+camPath+"]";
    }

    @Override
    public final String getPerfString() {
        final long currentTimeMillis = Platform.currentTimeMillis();
        final int video_scr = video_scr_pts + (int) ( ( currentTimeMillis - video_scr_t0 ) * playSpeed );
        final int d_vpts = video_pts_last - video_scr;
        final int audio_scr = (int) ( ( currentTimeMillis - audio_scr_t0 ) * playSpeed );
        final int audio_pts = getAudioPTSImpl();
        final int d_apts = audio_pts - audio_scr;
        return getPerfStringImpl( video_scr, video_pts_last, d_vpts, audio_scr, audio_pts, d_apts, getVideoDPTSAvg() );
    }
    private final String getPerfStringImpl(final int video_scr, final int video_pts, final int d_vpts,
                                           final int audio_scr, final int audio_pts, final int d_apts,
                                           final int video_dpts_avg_diff) {
        final float tt = getDuration() / 1000.0f;
        final String audioSinkInfo;
        final AudioSink audioSink = getAudioSink();
        if( null != audioSink ) {
            audioSinkInfo = "AudioSink[frames [p "+audioSink.getEnqueuedFrameCount()+", q "+audioSink.getQueuedFrameCount()+", f "+audioSink.getFreeFrameCount()+", c "+audioSink.getFrameCount()+"], time "+audioSink.getQueuedTime()+", bytes "+audioSink.getQueuedByteCount()+"]";
        } else {
            audioSinkInfo = "";
        }
        final int freeVideoFrames, decVideoFrames;
        if( null != videoFramesFree ) {
            freeVideoFrames = videoFramesFree.size();
            decVideoFrames = videoFramesDecoded.size();
        } else {
            freeVideoFrames = 0;
            decVideoFrames = 0;
        }
        return state+", frames[(p "+presentedFrameCount+", d "+decodedFrameCount+") / "+videoFrames+", "+tt+" s, z "+nullFrameCount+" / "+maxNullFrameCountUntilEOS+"], "+
               "speed " + playSpeed+", dAV "+( d_vpts - d_apts )+", vSCR "+video_scr+", vpts "+video_pts+", dSCR["+d_vpts+", avrg "+video_dpts_avg_diff+"], "+
               "aSCR "+audio_scr+", apts "+audio_pts+" ( "+d_apts+" ), "+audioSinkInfo+
               ", Texture[count "+textureCount+", free "+freeVideoFrames+", dec "+decVideoFrames+"]";
    }

    @Override
    public final void addEventListener(final GLMediaEventListener l) {
        if(l == null) {
            return;
        }
        synchronized(eventListenersLock) {
            eventListeners.add(l);
        }
    }

    @Override
    public final void removeEventListener(final GLMediaEventListener l) {
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

    private final Object eventListenersLock = new Object();

    @Override
    public final Object getAttachedObject(final String name) {
        return attachedObjects.get(name);
    }

    @Override
    public final Object attachObject(final String name, final Object obj) {
        return attachedObjects.put(name, obj);
    }

    @Override
    public final Object detachObject(final String name) {
        return attachedObjects.remove(name);
    }

    private final HashMap<String, Object> attachedObjects = new HashMap<String, Object>();

    protected static final String toHexString(final long v) {
        return "0x"+Long.toHexString(v);
    }
    protected static final String toHexString(final int v) {
        return "0x"+Integer.toHexString(v);
    }
    protected static final int getPropIntVal(final Map<String, String> props, final String key) {
        final String val = props.get(key);
        try {
            return Integer.parseInt(val);
        } catch (final NumberFormatException nfe) {
            if(DEBUG) {
                System.err.println("Not a valid integer for <"+key+">: <"+val+">");
            }
        }
        return 0;
    }
}
