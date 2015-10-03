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

package com.jogamp.opengl.test.junit.jogl.demos.es2.av;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLUniformData;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.junit.util.JunitTracer;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.graph.TextRendererGLELBase;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Simple planar movie player w/ orthogonal 1:1 projection.
 */
public class MovieSimple implements GLEventListener {
    public static final int EFFECT_NORMAL                  =    0;
    public static final int EFFECT_GRADIENT_BOTTOM2TOP     = 1<<1;
    public static final int EFFECT_TRANSPARENT             = 1<<3;

    public static final String WINDOW_KEY = "window";
    public static final String PLAYER = "player";

    private static boolean waitForKey = false;
    private int surfWidth, surfHeight;
    private int prevMouseX; // , prevMouseY;
    private int rotate = 0;
    private boolean  orthoProjection = true;
    private float nearPlaneNormalized;
    private float zoom0;
    private float zoom1;
    private float zoom;
    private long startTime;
    private int effects = EFFECT_NORMAL;
    private float alpha = 1.0f;
    private int swapInterval = 1;
    private boolean swapIntervalSet = true;

    private GLMediaPlayer mPlayer;
    private final boolean mPlayerShared;
    private boolean mPlayerScaleOrig;
    private float[] verts = null;
    private GLArrayDataServer interleavedVBO;
    private volatile boolean resetGLState = false;

    private ShaderState st;
    private PMVMatrix pmvMatrix;
    private GLUniformData pmvMatrixUniform;
    private static final String shaderBasename = "texsequence_xxx";
    private static final String myTextureLookupName = "myTexture2D";

    /** Blender's Big Buck Bunny: 24f 416p H.264,  AAC 48000 Hz, 2 ch, mpeg stream. */
    public static final Uri defURI;
    static {
        Uri _defURI = null;
        try {
            // Blender's Big Buck Bunny Trailer: 24f 640p VP8, Vorbis 44100Hz mono, WebM/Matroska Stream.
            // _defURI = new URI("http://video.webmfiles.org/big-buck-bunny_trailer.webm");
            _defURI = Uri.cast("http://archive.org/download/BigBuckBunny_328/BigBuckBunny_512kb.mp4");
        } catch (final URISyntaxException e) {
            e.printStackTrace();
        }
        defURI = _defURI;
    }

    final int[] textSampleCount = { 4 };

    private final class InfoTextRendererGLELBase extends TextRendererGLELBase {
        private final Font font = getFont(0, 0, 0);
        private final float fontSize = 10f;
        private final GLRegion regionFPS;

        InfoTextRendererGLELBase(final int rmode, final boolean lowPerfDevice) {
            // FIXME: Graph TextRenderer does not AA well w/o MSAA and FBO
            super(rmode, textSampleCount);
            this.setRendererCallbacks(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
            if( lowPerfDevice ) {
                regionFPS = null;
            } else {
                regionFPS = GLRegion.create(renderModes, null);
                System.err.println("RegionFPS "+Region.getRenderModeString(renderModes)+", sampleCount "+textSampleCount[0]+", class "+regionFPS.getClass().getName());
            }
            staticRGBAColor[0] = 0.9f;
            staticRGBAColor[1] = 0.9f;
            staticRGBAColor[2] = 0.9f;
            staticRGBAColor[3] = 1.0f;
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            super.init(drawable);
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            if( null != regionFPS ) {
                regionFPS.destroy(drawable.getGL().getGL2ES2());
            }
            super.dispose(drawable);
        }

        @Override
        public void display(final GLAutoDrawable drawable) {
            final GLAnimatorControl anim = drawable.getAnimator();
            final float lfps = null != anim ? anim.getLastFPS() : 0f;
            final float tfps = null != anim ? anim.getTotalFPS() : 0f;
            final boolean hasVideo = GLMediaPlayer.STREAM_ID_NONE != mPlayer.getVID();
            final float pts = ( hasVideo ? mPlayer.getVideoPTS() : mPlayer.getAudioPTS() ) / 1000f;

            // Note: MODELVIEW is from [ 0 .. height ]

            final int height = drawable.getSurfaceHeight();

            final float aspect = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();

            final String ptsPrec = null != regionFPS ? "3.1" : "3.0";
            final String text1 = String.format("%0"+ptsPrec+"f/%0"+ptsPrec+"f s, %s (%01.2fx, vol %01.2f), a %01.2f, fps %02.1f -> %02.1f / %02.1f, v-sync %b",
                    pts, mPlayer.getDuration() / 1000f,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(),
                    aspect, mPlayer.getFramerate(), lfps, tfps, swapIntervalSet);
            final String text2 = String.format("audio: id %d, kbps %d, codec %s",
                    mPlayer.getAID(), mPlayer.getAudioBitrate()/1000, mPlayer.getAudioCodec());
            final String text3 = String.format("video: id %d, kbps %d, codec %s",
                    mPlayer.getVID(), mPlayer.getVideoBitrate()/1000, mPlayer.getVideoCodec());
            final String text4 = mPlayer.getUri().path.decode();
            if( displayOSD && null != renderer ) {
                // We share ClearColor w/ MovieSimple's init !
                final float pixelSize = font.getPixelSize(fontSize, dpiH);
                if( null != regionFPS ) {
                    renderString(drawable, font, pixelSize, text1, 1 /* col */,  1 /* row */, 0,      0, -1, regionFPS); // no-cache
                } else {
                    renderString(drawable, font, pixelSize, text1, 1 /* col */,  1 /* row */, 0,      0, -1, true);
                }
                renderString(drawable, font, pixelSize, text2, 1 /* col */, -4 /* row */, 0, height, -1, true);
                renderString(drawable, font, pixelSize, text3, 1 /* col */, -3 /* row */, 0, height, -1, true);
                renderString(drawable, font, pixelSize, text4, 1 /* col */, -2 /* row */, 0, height, -1, true);
            }
        } };
    private InfoTextRendererGLELBase textRendererGLEL = null;
    private boolean displayOSD = true;

    private final MouseListener mouseAction = new MouseAdapter() {
        public void mousePressed(final MouseEvent e) {
            if(e.getY()<=surfHeight/2 && null!=mPlayer && 1 == e.getClickCount()) {
                if(GLMediaPlayer.State.Playing == mPlayer.getState()) {
                    mPlayer.pause(false);
                } else {
                    mPlayer.play();
                }
            }
        }
        public void mouseReleased(final MouseEvent e) {
            if(e.getY()<=surfHeight/2) {
                rotate = -1;
                zoom = zoom0;
                System.err.println("zoom: "+zoom);
            }
        }
        public void mouseMoved(final MouseEvent e) {
            prevMouseX = e.getX();
            // prevMouseY = e.getY();
        }
        public void mouseDragged(final MouseEvent e) {
            final int x = e.getX();
            final int y = e.getY();

            if(y>surfHeight/2) {
                final float dp  = (float)(x-prevMouseX)/(float)surfWidth;
                final int pts0 = GLMediaPlayer.STREAM_ID_NONE != mPlayer.getVID() ? mPlayer.getVideoPTS() : mPlayer.getAudioPTS();
                mPlayer.seek(pts0 + (int) (mPlayer.getDuration() * dp));
            } else {
                mPlayer.play();
                rotate = 1;
                zoom = zoom1;
            }

            prevMouseX = x;
            // prevMouseY = y;
        }
        public void mouseWheelMoved(final MouseEvent e) {
            if( !e.isShiftDown() ) {
                zoom += e.getRotation()[1]/10f; // vertical: wheel
                System.err.println("zoom: "+zoom);
            }
        } };

    private final KeyListener keyAction = new KeyAdapter() {
        public void keyReleased(final KeyEvent e)  {
            if( e.isAutoRepeat() ) {
                return;
            }
            System.err.println("MC "+e);
            final int pts0 = GLMediaPlayer.STREAM_ID_NONE != mPlayer.getVID() ? mPlayer.getVideoPTS() : mPlayer.getAudioPTS();
            int pts1 = 0;
            switch(e.getKeySymbol()) {
                case KeyEvent.VK_V: {
                    switch(swapInterval) {
                        case  0: swapInterval = -1; break;
                        case -1: swapInterval =  1; break;
                        case  1: swapInterval =  0; break;
                        default: swapInterval =  1; break;
                    }
                    swapIntervalSet = true;
                    break;
                }
                case KeyEvent.VK_O:          displayOSD = !displayOSD; break;
                case KeyEvent.VK_RIGHT:      pts1 = pts0 +  1000; break;
                case KeyEvent.VK_UP:         pts1 = pts0 + 10000; break;
                case KeyEvent.VK_PAGE_UP:    pts1 = pts0 + 30000; break;
                case KeyEvent.VK_LEFT:       pts1 = pts0 -  1000; break;
                case KeyEvent.VK_DOWN:       pts1 = pts0 - 10000; break;
                case KeyEvent.VK_PAGE_DOWN:  pts1 = pts0 - 30000; break;
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_HOME:
                case KeyEvent.VK_BACK_SPACE: {
                    mPlayer.seek(0);
                    break;
                }
                case KeyEvent.VK_SPACE: {
                    if(GLMediaPlayer.State.Paused == mPlayer.getState()) {
                        mPlayer.play();
                    } else {
                        mPlayer.pause(false);
                    }
                    break;
                }
                case KeyEvent.VK_MULTIPLY:
                      mPlayer.setPlaySpeed(1.0f);
                      break;
                case KeyEvent.VK_SUBTRACT: {
                      float playSpeed = mPlayer.getPlaySpeed();
                      if( e.isShiftDown() ) {
                          playSpeed /= 2.0f;
                      } else {
                          playSpeed -= 0.1f;
                      }
                      mPlayer.setPlaySpeed(playSpeed);
                    } break;
                case KeyEvent.VK_ADD: {
                      float playSpeed = mPlayer.getPlaySpeed();
                      if( e.isShiftDown() ) {
                          playSpeed *= 2.0f;
                      } else {
                          playSpeed += 0.1f;
                      }
                      mPlayer.setPlaySpeed(playSpeed);
                    } break;
                case KeyEvent.VK_M: {
                      float audioVolume = mPlayer.getAudioVolume();
                      if( audioVolume > 0.5f ) {
                          audioVolume = 0f;
                      } else {
                          audioVolume = 1f;
                      }
                      mPlayer.setAudioVolume(audioVolume);
                    } break;
            }

            if( 0 != pts1 ) {
                mPlayer.seek(pts1);
            }
        } };

    /**
     * Default constructor which also issues {@link #initStream(URI, int, int, int)} w/ default values
     * and polls until the {@link GLMediaPlayer} is {@link GLMediaPlayer.State#Initialized}.
     * If {@link GLMediaEventListener#EVENT_CHANGE_EOS} is reached, the stream is started over again.
     * <p>
     * This default constructor is merely useful for some <i>drop-in</i> test, e.g. using an applet.
     * </p>
     */
    public MovieSimple() {
        this(null);

        mPlayer.addEventListener(new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
                System.err.println("MovieCube AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                System.err.println("MovieCube State: "+mp);
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    new InterruptSource.Thread() {
                        public void run() {
                            // loop for-ever ..
                            mPlayer.seek(0);
                            mPlayer.play();
                        } }.start();
                }
            }
        });
        initStream(defURI, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 3 /* textureCount */);
        StreamException se = null;
        while( null == se && GLMediaPlayer.State.Initialized != mPlayer.getState() ) {
            try {
                Thread.sleep(16);
            } catch (final InterruptedException e) { }
            se = mPlayer.getStreamException();
        }
        if( null != se ) {
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }

    /** Custom constructor, user needs to issue {@link #initStream(URI, int, int, int)} afterwards. */
    public MovieSimple(final GLMediaPlayer sharedMediaPlayer) throws IllegalStateException {
        mPlayer = sharedMediaPlayer;
        mPlayerScaleOrig = false;
        mPlayerShared = null != mPlayer;
        if( !mPlayerShared ) {
            mPlayer = GLMediaPlayerFactory.createDefault();
            mPlayer.attachObject(PLAYER, this);
        }
        System.out.println("pC.1a shared "+mPlayerShared+", "+mPlayer);
    }

    public void initStream(final Uri streamLoc, final int vid, final int aid, final int textureCount) {
        mPlayer.initStream(streamLoc, vid, aid, textureCount);
        System.out.println("pC.1b "+mPlayer);
    }

    public void setSwapInterval(final int v) { this.swapInterval = v; }

    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }

    public void setScaleOrig(final boolean v) {
        mPlayerScaleOrig = v;
    }

    /** defaults to true */
    public void setOrthoProjection(final boolean v) { orthoProjection=v; }
    public boolean getOrthoProjection() { return orthoProjection; }

    public boolean hasEffect(final int e) { return 0 != ( effects & e ) ; }
    public void setEffects(final int e) { effects = e; };
    public void setTransparency(final float alpha) {
        this.effects |= EFFECT_TRANSPARENT;
        this.alpha = alpha;
    }

    public void resetGLState() {
        resetGLState = true;
    }

    private void initShader(final GL2ES2 gl) {
        // Create & Compile the shader objects
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, MovieSimple.class,
                                            "../shader", "../shader/bin", shaderBasename, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, MovieSimple.class,
                                            "../shader", "../shader/bin", shaderBasename, true);

        boolean preludeGLSLVersion = true;
        if( GLES2.GL_TEXTURE_EXTERNAL_OES == mPlayer.getTextureTarget() ) {
            if( !gl.isExtensionAvailable(GLExtensions.OES_EGL_image_external) ) {
                throw new GLException(GLExtensions.OES_EGL_image_external+" requested but not available");
            }
            if( Platform.OSType.ANDROID == Platform.getOSType() && gl.isGLES3() ) {
                // Bug on Nexus 10, ES3 - Android 4.3, where
                // GL_OES_EGL_image_external extension directive leads to a failure _with_ '#version 300 es' !
                //   P0003: Extension 'GL_OES_EGL_image_external' not supported
                preludeGLSLVersion = false;
            }
        }
        rsVp.defaultShaderCustomization(gl, preludeGLSLVersion, true);

        int rsFpPos = preludeGLSLVersion ? rsFp.addGLSLVersion(gl) : 0;
        rsFpPos = rsFp.insertShaderSource(0, rsFpPos, mPlayer.getRequiredExtensionsShaderStub());
        rsFp.addDefaultShaderPrecision(gl, rsFpPos);

        final String texLookupFuncName = mPlayer.getTextureLookupFunctionName(myTextureLookupName);
        rsFp.replaceInShaderSource(myTextureLookupName, texLookupFuncName);

        // Inject TextureSequence shader details
        final StringBuilder sFpIns = new StringBuilder();
        sFpIns.append("uniform ").append(mPlayer.getTextureSampler2DType()).append(" mgl_ActiveTexture;\n");
        sFpIns.append(mPlayer.getTextureLookupFragmentShaderImpl());
        rsFp.insertShaderSource(0, "TEXTURE-SEQUENCE-CODE-BEGIN", 0, sFpIns);

        // Create & Link the shader program
        final ShaderProgram sp = new ShaderProgram();
        sp.add(rsVp);
        sp.add(rsFp);
        if(!sp.link(gl, System.err)) {
            throw new GLException("Couldn't link program: "+sp);
        }

        // Let's manage all our states using ShaderState.
        st = new ShaderState();
        st.attachShaderProgram(gl, sp, false);
    }

    @Override
    public void init(final GLAutoDrawable drawable) {
        if(null == mPlayer) {
            throw new InternalError("mPlayer null");
        }
        if( GLMediaPlayer.State.Uninitialized == mPlayer.getState() ) {
            throw new IllegalStateException("mPlayer in uninitialized state: "+mPlayer);
        }
        final boolean hasVideo = GLMediaPlayer.STREAM_ID_NONE != mPlayer.getVID();
        resetGLState = false;

        zoom0 =  orthoProjection ? 0f : -2.5f;
        zoom1 = orthoProjection ? 0f : -5f;
        zoom = zoom0;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        System.err.println("Alpha: "+alpha+", opaque "+drawable.getChosenGLCapabilities().isBackgroundOpaque()+
                           ", "+drawable.getClass().getName()+", "+drawable);

        if(waitForKey) {
            JunitTracer.waitForKey("Init>");
        }
        final Texture tex;
        try {
            System.out.println("p0 "+mPlayer+", shared "+mPlayerShared);
            if(!mPlayerShared && GLMediaPlayer.State.Initialized == mPlayer.getState() ) {
                mPlayer.initGL(gl);
            }
            System.out.println("p1 "+mPlayer+", shared "+mPlayerShared);
            final TextureFrame frame = mPlayer.getLastTexture();
            if( null != frame ) {
                if( !hasVideo ) {
                    throw new InternalError("XXX: "+mPlayer);
                }
                tex = frame.getTexture();
                if( null == tex ) {
                    throw new InternalError("XXX: "+mPlayer);
                }
            } else {
                tex = null;
                if( hasVideo ) {
                    throw new InternalError("XXX: "+mPlayer);
                }
            }
            if(!mPlayerShared) {
                mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_LINEAR } );
            }
        } catch (final Exception glex) {
            glex.printStackTrace();
            if(!mPlayerShared && null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(glex);
        }

        if( hasVideo ) {
            initShader(gl);

            // Push the 1st uniform down the path
            st.useProgram(gl, true);

            final int[] viewPort = new int[] { 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight()};
            pmvMatrix = new PMVMatrix();
            reshapePMV(viewPort[2], viewPort[3]);
            pmvMatrixUniform = new GLUniformData("mgl_PMVMatrix", 4, 4, pmvMatrix.glGetPMvMatrixf());
            if(!st.uniform(gl, pmvMatrixUniform)) {
                throw new GLException("Error setting PMVMatrix in shader: "+st);
            }
            if(!st.uniform(gl, new GLUniformData("mgl_ActiveTexture", mPlayer.getTextureUnit()))) {
                throw new GLException("Error setting mgl_ActiveTexture in shader: "+st);
            }

            final float dWidth = drawable.getSurfaceWidth();
            final float dHeight = drawable.getSurfaceHeight();
            final float mWidth = mPlayer.getWidth();
            final float mHeight = mPlayer.getHeight();
            final float mAspect = mWidth/mHeight;
            System.err.println("XXX0: mov aspect: "+mAspect);
            float xs, ys;
            if(orthoProjection) {
                if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
                    xs   = mWidth/2f;                ys   = xs / mAspect;
                } else {
                    xs   = dWidth/2f;                ys   = xs / mAspect; // w>h
                }
            } else {
                if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
                    xs   = mAspect * ( mWidth / dWidth ) ; ys   =  xs / mAspect ;
                } else {
                    xs   = mAspect; ys   = 1f; // b>h
                }
            }
            verts = new float[] { -1f*xs, -1f*ys, 0f, // LB
                                   1f*xs,  1f*ys, 0f  // RT
                                };
            {
                System.err.println("XXX0: pixel  LB: "+verts[0]+", "+verts[1]+", "+verts[2]);
                System.err.println("XXX0: pixel  RT: "+verts[3]+", "+verts[4]+", "+verts[5]);
                final float[] winLB = new float[3];
                final float[] winRT = new float[3];
                pmvMatrix.gluProject(verts[0], verts[1], verts[2], viewPort, 0, winLB, 0);
                pmvMatrix.gluProject(verts[3], verts[4], verts[5], viewPort, 0, winRT, 0);
                System.err.println("XXX0: win   LB: "+winLB[0]+", "+winLB[1]+", "+winLB[2]);
                System.err.println("XXX0: win   RT: "+winRT[0]+", "+winRT[1]+", "+winRT[2]);
            }

            interleavedVBO = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
            {
                interleavedVBO.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
                interleavedVBO.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
                interleavedVBO.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            }
            updateInterleavedVBO(gl, tex);

            st.ownAttribute(interleavedVBO, true);
            gl.glClearColor(0.3f, 0.3f, 0.3f, 0.3f);

            gl.glEnable(GL.GL_DEPTH_TEST);

            st.useProgram(gl, false);

            // Let's show the completed shader state ..
            System.out.println("iVBO: "+interleavedVBO);
            System.out.println(st);
        }

        if(!mPlayerShared) {
            mPlayer.play();
            System.out.println("play.0 "+mPlayer);
        }
        startTime = System.currentTimeMillis();

        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addMouseListener(mouseAction);
            window.addKeyListener(keyAction);
            surfWidth = window.getSurfaceWidth();
            surfHeight = window.getSurfaceHeight();
        }
        final int rmode = drawable.getChosenGLCapabilities().getSampleBuffers() ? 0 : Region.VBAA_RENDERING_BIT;
        final boolean lowPerfDevice = gl.isGLES();
        textRendererGLEL = new InfoTextRendererGLELBase(rmode, lowPerfDevice);
        drawable.addGLEventListener(textRendererGLEL);
    }

    protected void updateInterleavedVBO(final GL gl, final Texture tex) {
        final float ss = 1f, ts = 1f; // scale tex-coord
        final boolean wasEnabled = interleavedVBO.enabled();
        interleavedVBO.seal(gl, false);
        interleavedVBO.rewind();
        {
            final FloatBuffer ib = (FloatBuffer)interleavedVBO.getBuffer();
            final TextureCoords tc = tex.getImageTexCoords();
            System.err.println("XXX0: "+tc);
            System.err.println("XXX0: tex aspect: "+tex.getAspectRatio());
            System.err.println("XXX0: tex y-flip: "+tex.getMustFlipVertically());

             // left-bottom
            ib.put(verts[0]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.left()   *ss);  ib.put( tc.bottom() *ts);

             // right-bottom
            ib.put(verts[3]);  ib.put(verts[1]);  ib.put(verts[2]);
            if( hasEffect(EFFECT_GRADIENT_BOTTOM2TOP) ) {
                ib.put( 0);    ib.put( 0);     ib.put( 0);    ib.put(alpha);
            } else {
                ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            }
            ib.put( tc.right()  *ss);  ib.put( tc.bottom() *ts);

             // left-top
            ib.put(verts[0]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            ib.put( tc.left()   *ss);  ib.put( tc.top()    *ts);

             // right-top
            ib.put(verts[3]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            ib.put( tc.right()  *ss);  ib.put( tc.top()    *ts);
        }
        interleavedVBO.seal(gl, true);
        if( !wasEnabled ) {
            interleavedVBO.enableBuffer(gl, false);
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(null == mPlayer) { return; }
        surfWidth = width;
        surfHeight = height;

        if(null != st) {
            reshapePMV(width, height);
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }

        System.out.println("pR "+mPlayer);
    }

    private final float zNear = 1f;
    private final float zFar = 10f;

    private void reshapePMV(final int width, final int height) {
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        if(orthoProjection) {
            final float fw = width / 2f;
            final float fh = height/ 2f;
            pmvMatrix.glOrthof(-fw, fw, -fh, fh, -1.0f, 1.0f);
            nearPlaneNormalized = 0f;
        } else {
            pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, zNear, zFar);
            nearPlaneNormalized = 1f/(10f-1f);
        }
        System.err.println("XXX0: Perspective nearPlaneNormalized: "+nearPlaneNormalized);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom0);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        drawable.disposeGLEventListener(textRendererGLEL, true);
        textRendererGLEL = null;
        disposeImpl(drawable, true);
    }

    private void disposeImpl(final GLAutoDrawable drawable, final boolean disposePlayer) {
        if(null == mPlayer) { return; }

        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeMouseListener(mouseAction);
            window.removeKeyListener(keyAction);
        }

        System.out.println("pD.1 "+mPlayer+", disposePlayer "+disposePlayer);
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( disposePlayer ) {
            if(!mPlayerShared) {
                mPlayer.destroy(gl);
            }
            System.out.println("pD.X "+mPlayer);
            mPlayer=null;
        }
        pmvMatrixUniform = null;
        if(null != pmvMatrix) {
            pmvMatrix=null;
        }
        if(null != st) {
            st.destroy(gl);
            st=null;
        }
    }

    long lastPerfPos = 0;

    @Override
    public void display(final GLAutoDrawable drawable) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( swapIntervalSet ) {
            final int _swapInterval = swapInterval;
            gl.setSwapInterval(_swapInterval); // in case switching the drawable (impl. may bound attribute there)
            drawable.getAnimator().resetFPSCounter();
            swapInterval = gl.getSwapInterval();
            System.err.println("Swap Interval: "+_swapInterval+" -> "+swapInterval);
            swapIntervalSet = false;
        }
        if(null == mPlayer) { return; }

        if( resetGLState ) {
            resetGLState = false;
            System.err.println("XXX resetGLState");
            disposeImpl(drawable, false);
            init(drawable);
            reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        }

        final long currentPos = System.currentTimeMillis();
        if( currentPos - lastPerfPos > 2000 ) {
            System.err.println( mPlayer.getPerfString() );
            lastPerfPos = currentPos;
        }

        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if(null == st) {
            return;
        }

        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom);
        if(rotate > 0) {
            final float ang = ((System.currentTimeMillis() - startTime) * 360.0f) / 8000.0f;
            pmvMatrix.glRotatef(ang, 0, 0, 1);
        } else {
            rotate = 0;
        }
        st.uniform(gl, pmvMatrixUniform);
        interleavedVBO.enableBuffer(gl, true);
        Texture tex = null;
        if(null!=mPlayer) {
            final TextureSequence.TextureFrame texFrame;
            if( mPlayerShared ) {
                texFrame=mPlayer.getLastTexture();
            } else {
                texFrame=mPlayer.getNextTexture(gl);
            }
            if(null != texFrame) {
                tex = texFrame.getTexture();
                gl.glActiveTexture(GL.GL_TEXTURE0+mPlayer.getTextureUnit());
                tex.enable(gl);
                tex.bind(gl);
            }
        }
        gl.glDrawArrays(GL.GL_TRIANGLE_STRIP, 0, 4);
        if(null != tex) {
            tex.disable(gl);
        }
        interleavedVBO.enableBuffer(gl, false);
        st.useProgram(gl, false);
    }

    static class MyGLMediaEventListener implements GLMediaEventListener {
            void destroyWindow(final Window window) {
                new InterruptSource.Thread() {
                    public void run() {
                        window.destroy();
                    } }.start();
            }

            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) {
            }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final int event_mask, final long when) {
                System.err.println("MovieSimple AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                System.err.println("MovieSimple State: "+mp);
                final GLWindow window = (GLWindow) mp.getAttachedObject(WINDOW_KEY);
                final MovieSimple ms = (MovieSimple)mp.getAttachedObject(PLAYER);
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    System.err.println("MovieSimple State: CHANGE_SIZE");
                    if( origSize ) {
                        window.setSurfaceSize(mp.getWidth(), mp.getHeight());
                    }
                    // window.disposeGLEventListener(ms, false /* remove */ );
                    ms.resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                    System.err.println("MovieSimple State: INIT");
                    // Use GLEventListener in all cases [A+V, V, A]
                    window.addGLEventListener(ms);
                    final GLAnimatorControl anim = window.getAnimator();
                    anim.setUpdateFPSFrames(60, null);
                    anim.resetFPSCounter();
                    /**
                     * Kick off player w/o GLEventListener, i.e. for audio only.
                     *
                        new InterruptSource.Thread() {
                            public void run() {
                                try {
                                    mp.initGL(null);
                                    if ( GLMediaPlayer.State.Paused == mp.getState() ) { // init OK
                                        mp.play();
                                    }
                                    System.out.println("play.1 "+mp);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    destroyWindow();
                                    return;
                                }
                            }
                        }.start();
                    */
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_PLAY & event_mask ) ) {
                    window.getAnimator().resetFPSCounter();
                }

                boolean destroy = false;
                Throwable err = null;

                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    err = ms.mPlayer.getStreamException();
                    if( null != err ) {
                        System.err.println("MovieSimple State: EOS + Exception");
                        destroy = true;
                    } else {
                        System.err.println("MovieSimple State: EOS");
                        if( loopEOS ) {
                            new InterruptSource.Thread() {
                                public void run() {
                                    mp.setPlaySpeed(1f);
                                    mp.seek(0);
                                    mp.play();
                                }
                            }.start();
                        } else {
                            destroy = true;
                        }
                    }
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_ERR & event_mask ) ) {
                    err = ms.mPlayer.getStreamException();
                    if( null != err ) {
                        System.err.println("MovieSimple State: ERR + Exception");
                    } else {
                        System.err.println("MovieSimple State: ERR");
                    }
                    destroy = true;
                }
                if( destroy ) {
                    if( null != err ) {
                        err.printStackTrace();
                    }
                    destroyWindow(window);
                }
            }
        };
    public final static MyGLMediaEventListener myGLMediaEventListener = new MyGLMediaEventListener();

    static boolean loopEOS = false;
    static boolean origSize;

    public static void main(final String[] args) throws IOException, URISyntaxException {
        int swapInterval = 1;
        int width = 800;
        int height = 600;
        int textureCount = 3; // default - threaded
        boolean ortho = true;
        boolean zoom = false;

        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;
        int vid = GLMediaPlayer.STREAM_ID_AUTO;
        int aid = GLMediaPlayer.STREAM_ID_AUTO;

        final int windowCount;
        {
            int _windowCount = 1;
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-windows")) {
                    i++;
                    _windowCount = MiscUtils.atoi(args[i], _windowCount);
                }
            }
            windowCount = _windowCount;
        }
        final String[] urls_s = new String[windowCount];
        String file_s1=null, file_s2=null;
        {
            boolean _origSize = false;
            for(int i=0; i<args.length; i++) {
                if(args[i].equals("-vid")) {
                    i++;
                    vid = MiscUtils.atoi(args[i], vid);
                } else if(args[i].equals("-aid")) {
                    i++;
                    aid = MiscUtils.atoi(args[i], aid);
                } else if(args[i].equals("-width")) {
                    i++;
                    width = MiscUtils.atoi(args[i], width);
                } else if(args[i].equals("-height")) {
                    i++;
                    height = MiscUtils.atoi(args[i], height);
                } else if(args[i].equals("-osize")) {
                    _origSize = true;
                } else if(args[i].equals("-textureCount")) {
                    i++;
                    textureCount = MiscUtils.atoi(args[i], textureCount);
                } else if(args[i].equals("-es2")) {
                    forceES2 = true;
                } else if(args[i].equals("-es3")) {
                    forceES3 = true;
                } else if(args[i].equals("-gl3")) {
                    forceGL3 = true;
                } else if(args[i].equals("-gldef")) {
                    forceGLDef = true;
                } else if(args[i].equals("-vsync")) {
                    i++;
                    swapInterval = MiscUtils.atoi(args[i], swapInterval);
                } else if(args[i].equals("-projection")) {
                    ortho=false;
                } else if(args[i].equals("-zoom")) {
                    zoom=true;
                } else if(args[i].equals("-loop")) {
                    loopEOS=true;
                } else if(args[i].equals("-urlN")) {
                    i++;
                    final int n = MiscUtils.atoi(args[i], 0);
                    i++;
                    urls_s[n] = args[i];
                } else if(args[i].equals("-url")) {
                    i++;
                    urls_s[0] = args[i];
                } else if(args[i].equals("-file1")) {
                    i++;
                    file_s1 = args[i];
                } else if(args[i].equals("-file2")) {
                    i++;
                    file_s2 = args[i];
                } else if(args[i].equals("-wait")) {
                    waitForKey = true;
                }
            }
            origSize = _origSize;
        }
        final Uri streamLoc0;
        if( null != urls_s[0] ) {
            streamLoc0 = Uri.cast( urls_s[0] );
        } else if( null != file_s1 ) {
            final File movieFile = new File(file_s1);
            streamLoc0 = Uri.valueOf(movieFile);
        } else if( null != file_s2 ) {
            streamLoc0 = Uri.valueOf(new File(file_s2));
        } else {
            streamLoc0 = defURI;
        }
        System.err.println("url_s "+urls_s[0]);
        System.err.println("file_s 1: "+file_s1+", 2: "+file_s2);
        System.err.println("stream0 "+streamLoc0);
        System.err.println("vid "+vid+", aid "+aid);
        System.err.println("textureCount "+textureCount);
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("swapInterval "+swapInterval);

        final GLProfile glp;
        if(forceGLDef) {
            glp = GLProfile.getDefault();
        } else if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        System.err.println("GLProfile: "+glp);
        final GLCapabilities caps = new GLCapabilities(glp);
        // caps.setAlphaBits(4); // NOTE_ALPHA_BLENDING: We go w/o alpha and blending!

        final MovieSimple[] mss = new MovieSimple[windowCount];
        final GLWindow[] windows = new GLWindow[windowCount];
        for(int i=0; i<windowCount; i++) {
            final Animator anim = new Animator();
            anim.start();
            windows[i] = GLWindow.create(caps);
            windows[i].addWindowListener(new WindowAdapter() {
                public void windowDestroyed(final WindowEvent e) {
                    anim.stop();
                }
            });
            mss[i] = new MovieSimple(null);
            mss[i].setSwapInterval(swapInterval);
            mss[i].setScaleOrig(!zoom);
            mss[i].setOrthoProjection(ortho);
            mss[i].mPlayer.attachObject(WINDOW_KEY, windows[i]);
            mss[i].mPlayer.addEventListener(myGLMediaEventListener);

            windows[i].setTitle("Player "+i);
            windows[i].setSize(width, height);
            windows[i].setVisible(true);
            anim.add(windows[i]);

            final Uri streamLocN;
            if( 0 == i ) {
                streamLocN = streamLoc0;
            } else {
                if( null != urls_s[i] ) {
                    streamLocN = Uri.cast(urls_s[i]);
                } else {
                    streamLocN = defURI;
                }
            }
            System.err.println("Win #"+i+": stream "+streamLocN);
            mss[i].initStream(streamLocN, vid, aid, textureCount);
        }
    }

}
