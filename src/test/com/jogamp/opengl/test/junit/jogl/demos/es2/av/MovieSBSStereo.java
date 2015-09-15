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

import java.net.URISyntaxException;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;
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
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GLExtensions;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.Quaternion;
import com.jogamp.opengl.math.VectorUtil;
import com.jogamp.opengl.test.junit.graph.TextRendererGLELBase;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.CustomGLEventListener;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.PMVMatrix;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.glsl.ShaderCode;
import com.jogamp.opengl.util.glsl.ShaderProgram;
import com.jogamp.opengl.util.glsl.ShaderState;
import com.jogamp.opengl.util.stereo.EyeParameter;
import com.jogamp.opengl.util.stereo.ViewerPose;
import com.jogamp.opengl.util.stereo.StereoClientRenderer;
import com.jogamp.opengl.util.stereo.StereoGLEventListener;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureCoords;
import com.jogamp.opengl.util.texture.TextureSequence;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Side-By-Side (SBS) 3D Movie Player for {@link StereoClientRenderer}
 * <p>
 * The movie is assumed to be symmetrical SBS,
 * the left-eye receives the left-part of the texture
 * and the right-eye the right-part.
 * </p>
 */
public class MovieSBSStereo implements StereoGLEventListener {
    public static final String WINDOW_KEY = "window";
    public static final String STEREO_RENDERER_KEY = "stereo";
    public static final String PLAYER = "player";

    private static boolean waitForKey = false;
    private int surfWidth, surfHeight;
    private int prevMouseX; // , prevMouseY;
    private int rotate = 0;
    private float zoom0;
    private float zoom1;
    private float zoom;
    private long startTime;
    private final float alpha = 1.0f;

    private GLMediaPlayer mPlayer;
    private boolean mPlayerScaleOrig;
    private float[] verts = null;
    private GLArrayDataServer interleavedVBOLeft;
    private GLArrayDataServer interleavedVBORight;
    private volatile boolean resetGLState = false;
    private StereoClientRenderer stereoClientRenderer;

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
        private final float fontSize = 1f; // 0.01f;
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

            final int height = 0; // drawable.getSurfaceHeight();

            final float aspect = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();

            final String ptsPrec = null != regionFPS ? "3.1" : "3.0";
            final String text1 = String.format("%0"+ptsPrec+"f/%0"+ptsPrec+"f s, %s (%01.2fx, vol %01.2f), a %01.2f, fps %02.1f -> %02.1f / %02.1f",
                    pts, mPlayer.getDuration() / 1000f,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(),
                    aspect, mPlayer.getFramerate(), lfps, tfps);
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
                renderString(drawable, font, pixelSize, text3, 1 /* col */, -3 /* row */, 0, height,  0, true);
                renderString(drawable, font, pixelSize, text4, 1 /* col */, -2 /* row */, 0, height,  1, true);
            }
        } };
    private final boolean enableTextRendererGLEL = false;
    private InfoTextRendererGLELBase textRendererGLEL = null;
    private boolean displayOSD = false;

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

    /** user needs to issue {@link #initStream(URI, int, int, int)} afterwards. */
    public MovieSBSStereo() throws IllegalStateException {
        mPlayerScaleOrig = false;
        mPlayer = GLMediaPlayerFactory.createDefault();
        mPlayer.attachObject(PLAYER, this);
        System.out.println("pC.1a "+mPlayer);
    }

    public void initStream(final Uri streamLoc, final int vid, final int aid, final int textureCount) {
        mPlayer.initStream(streamLoc, vid, aid, textureCount);
        System.out.println("pC.1b "+mPlayer);
    }

    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }

    public void setScaleOrig(final boolean v) {
        mPlayerScaleOrig = v;
    }

    public void setStereoClientRenderer(final StereoClientRenderer scr) {
        stereoClientRenderer = scr;
    }
    public StereoClientRenderer getStereoClientRenderer() { return stereoClientRenderer; }

    public void resetGLState() {
        resetGLState = true;
    }

    private void initShader(final GL2ES2 gl) {
        // Create & Compile the shader objects
        final ShaderCode rsVp = ShaderCode.create(gl, GL2ES2.GL_VERTEX_SHADER, MovieSBSStereo.class,
                                            "../shader", "../shader/bin", shaderBasename, true);
        final ShaderCode rsFp = ShaderCode.create(gl, GL2ES2.GL_FRAGMENT_SHADER, MovieSBSStereo.class,
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

        zoom0 =  -2.1f;
        zoom1 = -5f;
        zoom = 0f;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));
        System.err.println("Alpha: "+alpha+", opaque "+drawable.getChosenGLCapabilities().isBackgroundOpaque()+
                           ", "+drawable.getClass().getName()+", "+drawable);

        if(waitForKey) {
            JunitTracer.waitForKey("Init>");
        }
        final Texture tex;
        try {
            System.out.println("p0 "+mPlayer);
            if(GLMediaPlayer.State.Initialized == mPlayer.getState() ) {
                mPlayer.initGL(gl);
            }
            System.out.println("p1 "+mPlayer);
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
            mPlayer.setTextureMinMagFilter( new int[] { GL.GL_NEAREST, GL.GL_NEAREST } );
        } catch (final Exception glex) {
            glex.printStackTrace();
            if(null != mPlayer) {
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
            if(mPlayerScaleOrig && mWidth < dWidth && mHeight < dHeight) {
                xs   = mAspect * ( mWidth / dWidth ) ; ys   =  xs / mAspect ;
            } else {
                xs   = mAspect; ys   = 1f; // b>h
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

            interleavedVBOLeft = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
            {
                interleavedVBOLeft.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
                interleavedVBOLeft.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
                interleavedVBOLeft.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            }
            interleavedVBORight = GLArrayDataServer.createGLSLInterleaved(3+4+2, GL.GL_FLOAT, false, 3*4, GL.GL_STATIC_DRAW);
            {
                interleavedVBORight.addGLSLSubArray("mgl_Vertex",        3, GL.GL_ARRAY_BUFFER);
                interleavedVBORight.addGLSLSubArray("mgl_Color",         4, GL.GL_ARRAY_BUFFER);
                interleavedVBORight.addGLSLSubArray("mgl_MultiTexCoord", 2, GL.GL_ARRAY_BUFFER);
            }
            updateInterleavedVBO(gl, interleavedVBOLeft, tex, 0);
            updateInterleavedVBO(gl, interleavedVBORight, tex, 1);

            st.ownAttribute(interleavedVBOLeft, true);
            st.ownAttribute(interleavedVBORight, true);
            gl.glClearColor(0.3f, 0.3f, 0.3f, 0.3f);

            gl.glEnable(GL.GL_DEPTH_TEST);

            st.useProgram(gl, false);

            // Let's show the completed shader state ..
            System.out.println("iVBOLeft : "+interleavedVBOLeft);
            System.out.println("iVBORight: "+interleavedVBORight);
            System.out.println(st);
        }

        mPlayer.play();
        System.out.println("play.0 "+mPlayer);
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
        if( enableTextRendererGLEL ) {
            textRendererGLEL = new InfoTextRendererGLELBase(rmode, lowPerfDevice);
            textRendererGLEL.init(drawable);
        } else {
            textRendererGLEL = null;
        }
    }

    protected void updateInterleavedVBO(final GL gl, final GLArrayDataServer iVBO, final Texture tex, final int eyeNum) {
        final boolean wasEnabled = iVBO.enabled();
        iVBO.seal(gl, false);
        iVBO.rewind();
        {
            final FloatBuffer ib = (FloatBuffer)iVBO.getBuffer();
            final TextureCoords tc = tex.getImageTexCoords();
            final float texHalfWidth = tc.right()/2f;
            System.err.println("XXX0: "+tc+", texHalfWidth "+texHalfWidth);
            System.err.println("XXX0: tex aspect: "+tex.getAspectRatio());
            System.err.println("XXX0: tex y-flip: "+tex.getMustFlipVertically());

             // left-bottom
            ib.put(verts[0]);  ib.put(verts[1]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            if( 0 == eyeNum ) {
                ib.put( tc.left() );  ib.put( tc.bottom() );
            } else {
                ib.put( tc.left() + texHalfWidth );  ib.put( tc.bottom() );
            }

             // right-bottom
            ib.put(verts[3]);  ib.put(verts[1]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            if( 0 == eyeNum ) {
                ib.put( texHalfWidth );  ib.put( tc.bottom() );
            } else {
                ib.put( tc.right() );  ib.put( tc.bottom() );
            }

             // left-top
            ib.put(verts[0]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            if( 0 == eyeNum ) {
                ib.put( tc.left() );  ib.put( tc.top() );
            } else {
                ib.put( tc.left() + texHalfWidth );  ib.put( tc.top() );
            }

             // right-top
            ib.put(verts[3]);  ib.put(verts[4]);  ib.put(verts[2]);
            ib.put( 1);    ib.put( 1);     ib.put( 1);    ib.put(alpha);
            if( 0 == eyeNum ) {
                ib.put( texHalfWidth );  ib.put( tc.top() );
            } else {
                ib.put( tc.right() );  ib.put( tc.top() );
            }
        }
        iVBO.seal(gl, true);
        if( !wasEnabled ) {
            iVBO.enableBuffer(gl, false);
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        surfWidth = width;
        surfHeight = height;

        if(null == mPlayer) { return; }

        if(null != st) {
            reshapePMV(width, height);
            st.useProgram(gl, true);
            st.uniform(gl, pmvMatrixUniform);
            st.useProgram(gl, false);
        }

        System.out.println("pR "+mPlayer);
        if( null != textRendererGLEL ) {
            textRendererGLEL.reshape(drawable, 0, 0, width, height);
        }
    }

    private final float zNear = 0.1f;
    private final float zFar = 10000f;

    private void reshapePMV(final int width, final int height) {
        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.gluPerspective(45.0f, (float)width / (float)height, zNear, zFar);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glLoadIdentity();
        pmvMatrix.glTranslatef(0, 0, zoom0);
    }

    private final float[] mat4Tmp1 = new float[16];
    private final float[] mat4Tmp2 = new float[16];
    private final float[] vec3Tmp1 = new float[3];
    private final float[] vec3Tmp2 = new float[3];
    private final float[] vec3Tmp3 = new float[3];

    GLArrayDataServer interleavedVBOCurrent = null;

    private static final float[] vec3ScalePos = new float[] { 4f, 4f, 4f };

    @Override
    public void reshapeForEye(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height,
                              final EyeParameter eyeParam, final ViewerPose viewerPose) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        interleavedVBOCurrent = 0 == eyeParam.number ? interleavedVBOLeft : interleavedVBORight;

        surfWidth = drawable.getSurfaceWidth();
        surfHeight = drawable.getSurfaceHeight();

        if(null == mPlayer) { return; }
        if(null == st) { return; }

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
        final float[] mat4Projection = FloatUtil.makePerspective(mat4Tmp1, 0, true, eyeParam.fovhv, zNear, zFar);
        pmvMatrix.glLoadMatrixf(mat4Projection, 0);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        final Quaternion rollPitchYaw = new Quaternion();
        final float[] shiftedEyePos = rollPitchYaw.rotateVector(vec3Tmp1, 0, viewerPose.position, 0);
        VectorUtil.scaleVec3(shiftedEyePos, shiftedEyePos, vec3ScalePos); // amplify viewerPose position
        VectorUtil.addVec3(shiftedEyePos, shiftedEyePos, eyeParam.positionOffset);

        rollPitchYaw.mult(viewerPose.orientation);
        final float[] up = rollPitchYaw.rotateVector(vec3Tmp2, 0, VectorUtil.VEC3_UNIT_Y, 0);
        final float[] forward = rollPitchYaw.rotateVector(vec3Tmp3, 0, VectorUtil.VEC3_UNIT_Z_NEG, 0);
        final float[] center = VectorUtil.addVec3(forward, shiftedEyePos, forward);

        final float[] mLookAt = FloatUtil.makeLookAt(mat4Tmp1, 0, shiftedEyePos, 0, center, 0, up, 0, mat4Tmp2);
        final float[] mViewAdjust = FloatUtil.makeTranslation(mat4Tmp2, true, eyeParam.distNoseToPupilX, eyeParam.distMiddleToPupilY, eyeParam.eyeReliefZ);
        final float[] mat4Modelview = FloatUtil.multMatrix(mViewAdjust, mLookAt);
        pmvMatrix.glLoadMatrixf(mat4Modelview, 0);
        pmvMatrix.glTranslatef(0, 0, zoom0);
        st.useProgram(gl, true);
        st.uniform(gl, pmvMatrixUniform);
        st.useProgram(gl, false);
        if( null != textRendererGLEL ) {
            textRendererGLEL.reshape(drawable, 0, 0, width, height);
        }
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        if( null != textRendererGLEL ) {
            textRendererGLEL.dispose(drawable);
            textRendererGLEL = null;
        }
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
            mPlayer.destroy(gl);
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
        display(drawable, 0);
    }

    @Override
    public void display(final GLAutoDrawable drawable, final int flags) {
        // TODO Auto-generated method stub
        final boolean repeatedFrame = 0 != ( CustomGLEventListener.DISPLAY_REPEAT & flags );
        final boolean dontClear = 0 != ( CustomGLEventListener.DISPLAY_DONTCLEAR & flags );
        final GLArrayDataServer iVBO = null != interleavedVBOCurrent ? interleavedVBOCurrent : interleavedVBOLeft;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
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

        if( !dontClear ) {
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);
        }

        if(null == st) {
            return;
        }

        st.useProgram(gl, true);

        pmvMatrix.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
        pmvMatrix.glPushMatrix();
        pmvMatrix.glTranslatef(0, 0, zoom);
        if( rotate > 0) {
            final float ang = ((System.currentTimeMillis() - startTime) * 360.0f) / 8000.0f;
            pmvMatrix.glRotatef(ang, 0, 0, 1);
        } else {
            rotate = 0;
        }
        st.uniform(gl, pmvMatrixUniform);
        iVBO.enableBuffer(gl, true);
        Texture tex = null;
        if(null!=mPlayer) {
            final TextureSequence.TextureFrame texFrame;
            if( repeatedFrame ) {
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
        iVBO.enableBuffer(gl, false);
        st.useProgram(gl, false);
        pmvMatrix.glPopMatrix();

        if( null != textRendererGLEL ) {
            textRendererGLEL.display(drawable);
        }
    }

    static class StereoGLMediaEventListener implements GLMediaEventListener {
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
                final MovieSBSStereo ms = (MovieSBSStereo)mp.getAttachedObject(PLAYER);
                final StereoClientRenderer stereoClientRenderer = (StereoClientRenderer) mp.getAttachedObject(STEREO_RENDERER_KEY);

                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    System.err.println("MovieSimple State: CHANGE_SIZE");
                    // window.disposeGLEventListener(ms, false /* remove */ );
                    ms.resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                    System.err.println("MovieSimple State: INIT");
                    // Use GLEventListener in all cases [A+V, V, A]
                    stereoClientRenderer.addGLEventListener(ms);
                    final GLAnimatorControl anim = window.getAnimator();
                    anim.setUpdateFPSFrames(60, null);
                    anim.resetFPSCounter();
                    ms.setStereoClientRenderer(stereoClientRenderer);
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
                        new InterruptSource.Thread() {
                            public void run() {
                                mp.setPlaySpeed(1f);
                                mp.seek(0);
                                mp.play();
                            }
                        }.start();
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
    public final static StereoGLMediaEventListener stereoGLMediaEventListener = new StereoGLMediaEventListener();
}
