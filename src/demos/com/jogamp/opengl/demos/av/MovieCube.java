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

package com.jogamp.opengl.demos.av;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLRunnable;
import com.jogamp.common.net.Uri;
import com.jogamp.common.os.Clock;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.graph.curve.Region;
import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.font.Font;
import com.jogamp.graph.font.FontScale;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.demos.es2.TextureSequenceCubeES2;
import com.jogamp.opengl.demos.graph.TextRendererGLELBase;
import com.jogamp.opengl.demos.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Simple cube movie player w/ aspect ration true projection on a cube.
 */
public class MovieCube implements GLEventListener {
    public static final float zoom_def = -2.77f;
    private static boolean waitForKey = false;
    private final float zoom0, rotx, roty;
    private TextureSequenceCubeES2 cube=null;
    private GLMediaPlayer mPlayer=null;
    private int swapInterval = 1;
    private boolean swapIntervalSet = true;
    private long lastPerfPos = 0;
    private volatile boolean resetGLState = false;
    private volatile GLAutoDrawable autoDrawable = null;

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

    /**
     * Default constructor which also issues {@link #playStream(URI, int, int, int)} w/ default values
     * and polls until the {@link GLMediaPlayer} is {@link GLMediaPlayer.State#Initialized}.
     * If {@link GLMediaEventListener#EVENT_CHANGE_EOS} is reached, the stream is started over again.
     * <p>
     * This default constructor is merely useful for some <i>drop-in</i> test, e.g. using an applet.
     * </p>
     */
    public MovieCube() throws IOException, URISyntaxException {
        this(zoom_def, 0f, 0f, true);

        mPlayer.addEventListener(new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) { }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final GLMediaPlayer.EventMask eventMask, final long when) {
                System.err.println("MovieCube.0 AttributesChanges: "+eventMask+", when "+when);
                System.err.println("MovieCube.0 State: "+mp);
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.Size) ) {
                    resetGLState();
                }
                if( eventMask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            // loop for-ever ..
                            mPlayer.seek(0);
                            mPlayer.resume();
                        } }.start();
                }
            }
        });
        playStream(defURI, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
    }

    /**
     * Custom constructor, user needs to issue {@link #playStream(URI, int, int, int)} afterwards.
     */
    public MovieCube(final float zoom0, final float rotx, final float roty, final boolean showText) throws IOException {
        this.zoom0 = zoom0;
        this.rotx = rotx;
        this.roty = roty;
        this.showText = showText;
        screenshot = new GLReadBufferUtil(false, false);
        mPlayer = GLMediaPlayerFactory.createDefault();
    }

    public void playStream(final Uri streamLoc, final int vid, final int aid, final int textureCount) {
        mPlayer.playStream(streamLoc, vid, aid, textureCount);
        System.out.println("pC.1b "+mPlayer);
    }

    public void setSwapInterval(final int v) { this.swapInterval = v; }

    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }

    public void resetGLState() {
        resetGLState = true;
    }

    final int[] textSampleCount = { 4 };

    private final class InfoTextRendererGLELBase extends TextRendererGLELBase {
        private static final float z_diff = 0.001f;
        private final Font font = getFont(0, 0, 0);
        private final float fontSize1 = 12;
        private final float fontSize2 = 10;
        private final GLRegion regionFPS;
        private float pixelSize1, pixelSize2, underlineSize;

        InfoTextRendererGLELBase(final GLProfile glp, final int rmode) {
            // FIXME: Graph TextRenderer does not AA well w/o MSAA and FBO
            super(rmode, MovieCube.this.textSampleCount);
            this.setRendererCallbacks(RegionRenderer.defaultBlendEnable, RegionRenderer.defaultBlendDisable);
            regionFPS = GLRegion.create(glp, renderModes, null, 0, 0);
            System.err.println("RegionFPS "+Region.getRenderModeString(renderModes)+", sampleCount "+textSampleCount[0]+", class "+regionFPS.getClass().getName());
            staticRGBAColor[0] = 0.1f;
            staticRGBAColor[1] = 0.1f;
            staticRGBAColor[2] = 0.1f;
            staticRGBAColor[3] = 1.0f;
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            // non-exclusive mode!
            this.setSharedPMVMatrix(cube.pmvMatrix);
            super.init(drawable);

            autoDrawable = drawable;

            pixelSize1 = FontScale.toPixels(fontSize1, dpiH);
            pixelSize2 = FontScale.toPixels(fontSize2, dpiH);
            pixelScale = 1.0f / ( pixelSize1 * 20f );
            // underlineSize: 'underline' amount of pixel below 0/0 (Note: lineGap is negative)
            final Font.Metrics metrics = font.getMetrics();
            final float lineGap = pixelSize1 * metrics.getLineGap();
            final float descent = pixelSize1 * metrics.getDescent();
            underlineSize = lineGap - descent;
            System.err.println("XXX: dpiH "+dpiH+", fontSize "+fontSize1+", pixelSize "+pixelSize1+", pixelScale "+pixelScale+", fLG "+lineGap+", fDesc "+descent+", underlineSize "+underlineSize);
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            autoDrawable = null;
            screenshot.dispose(drawable.getGL());
            if( null != regionFPS ) {
                regionFPS.destroy(drawable.getGL().getGL2ES2());
            }
            super.dispose(drawable);
        }

        String text1_old = null;

        @Override
        public void display(final GLAutoDrawable drawable) {
            final GLAnimatorControl anim = drawable.getAnimator();
            final float lfps = null != anim ? anim.getLastFPS() : 0f;
            final float tfps = null != anim ? anim.getTotalFPS() : 0f;
            final float pts = mPlayer.getPTS().get(Clock.currentMillis()) / 1000f;

            // Note: MODELVIEW is from [ -1 .. 1 ]

            // dy: position right above video rectangle (bottom text line)
            final float aspect = (float)mPlayer.getWidth() / (float)mPlayer.getHeight();
            final float aspect_h = 1f/aspect;
            final float dy = 1f-aspect_h;

            // yoff1: position right above video rectangle (bottom text line)
            //        less than underlineSize, so 'underline' pixels are above video.
            final float yoff1 = dy-(pixelScale*underlineSize);

            // yoff2: position right below video rectangle (bottom text line)
            final float yoff2 = 2f-dy;

            /**
            System.err.println("XXX: a "+aspect+", aspect_h "+aspect_h+", dy "+dy+
                               "; underlineSize "+underlineSize+" "+(pixelScale*underlineSize)+
                               "; yoff "+yoff1+", yoff2 "+yoff2); */

            final GL2ES2 gl = drawable.getGL().getGL2ES2();
            final String ptsPrec = null != regionFPS ? "3.1" : "3.0";
            final String text1 = String.format("%0"+ptsPrec+"f/%0"+ptsPrec+"f s, %s (%01.2fx, vol %01.2f), a %01.2f, fps %02.1f -> %02.1f / %02.1f, swap %d",
                    pts, mPlayer.getDuration() / 1000f,
                    mPlayer.getState().toString().toLowerCase(), mPlayer.getPlaySpeed(), mPlayer.getAudioVolume(),
                    aspect, mPlayer.getFramerate(), lfps, tfps, drawable.getGL().getSwapInterval());
            final String text2 = String.format("audio: id %d, kbps %d, codec %s",
                    mPlayer.getAID(), mPlayer.getAudioBitrate()/1000, mPlayer.getAudioCodec());
            final String text3 = String.format("video: id %d, kbps %d, codec %s",
                    mPlayer.getVID(), mPlayer.getVideoBitrate()/1000, mPlayer.getVideoCodec());
            final String text4 = mPlayer.getUri().path.decode();
            if( displayOSD && null != renderer ) {
                gl.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
                if( !text1.equals(text1_old) ) {
                    renderString(drawable, font, pixelSize1, text1, 1 /* col */,  -1 /* row */, -1+z_diff, yoff1, 1f+z_diff, regionFPS.clear(gl)); // clear-cache
                    text1_old = text1;
                } else {
                    renderRegion(drawable, font, pixelSize1, 1 /* col */,  -1 /* row */, -1+z_diff, yoff1, 1f+z_diff, regionFPS);
                }
                renderString(drawable, font, pixelSize2, text2, 1 /* col */,  0 /* row */, -1+z_diff, yoff2, 1f+z_diff, true);
                renderString(drawable, font, pixelSize2, text3, 1 /* col */,  1 /* row */, -1+z_diff, yoff2, 1f+z_diff, true);
                renderString(drawable, font, pixelSize2, text4, 1 /* col */,  2 /* row */, -1+z_diff, yoff2, 1f+z_diff, true);
            }
        } };
    private InfoTextRendererGLELBase textRendererGLEL = null;
    final boolean showText;
    private boolean displayOSD = true;

    public void printScreen(final GLAutoDrawable drawable) throws GLException, IOException {
        final String filename = String.format("MovieCube-snap%02d-%03dx%03d.png", screenshot_num++, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
        if(screenshot.readPixels(drawable.getGL(), false)) {
            screenshot.write(new File(filename.toString()));
        }
    }
    private final GLReadBufferUtil screenshot;
    private int screenshot_num = 0;

    public void printScreenOnGLThread(final GLAutoDrawable drawable) {
        drawable.invoke(false, new GLRunnable() {
            @Override
            public boolean run(final GLAutoDrawable drawable) {
                try {
                    printScreen(drawable);
                } catch (final GLException e) {
                    e.printStackTrace();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        });
    }

    private final KeyListener keyAction = new KeyAdapter() {
        @Override
        public void keyReleased(final KeyEvent e)  {
            if( e.isAutoRepeat() ) {
                return;
            }
            System.err.println("MC "+e);
            final int pts0 = mPlayer.getPTS().get(Clock.currentMillis());

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
                case KeyEvent.VK_HOME:
                case KeyEvent.VK_BACK_SPACE: {
                    mPlayer.seek(0);
                    break;
                }
                case KeyEvent.VK_SPACE: {
                    if( GLMediaPlayer.State.Paused == mPlayer.getState() ) {
                        mPlayer.resume();
                    } else if(GLMediaPlayer.State.Uninitialized == mPlayer.getState()) {
                        playStream(mPlayer.getUri(), GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, 3 /* textureCount */);
                    } else if( e.isShiftDown() ) {
                        mPlayer.stop();
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
                case KeyEvent.VK_S:
                    if(null != autoDrawable) {
                        printScreenOnGLThread(autoDrawable);
                    }
                    break;
                case KeyEvent.VK_F4:
                case KeyEvent.VK_ESCAPE:
                case KeyEvent.VK_Q:
                    if(null != autoDrawable) {
                        MiscUtils.destroyWindow(autoDrawable);
                    }
                    break;
            }

            if( 0 != pts1 ) {
                mPlayer.seek(pts1);
            }
        }
    };

    @Override
    public void init(final GLAutoDrawable drawable) {
        if(null == mPlayer) {
            throw new InternalError("mPlayer null");
        }
        // final boolean hasVideo = GLMediaPlayer.STREAM_ID_NONE != mPlayer.getVID();
        resetGLState = false;

        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));

        cube = new TextureSequenceCubeES2(mPlayer, false, zoom0, rotx, roty);

        if(waitForKey) {
            MiscUtils.waitForKey("Init>");
        }

        try {
            mPlayer.initGL(gl);
        } catch (final Exception e) {
            e.printStackTrace();
            if(null != mPlayer) {
                mPlayer.destroy(gl);
                mPlayer = null;
            }
            throw new GLException(e);
        }
        cube.init(drawable);
        mPlayer.resume();
        System.out.println("play.0 "+mPlayer);

        boolean added;
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addKeyListener(keyAction);
            added = true;
        } else { added = false; }
        System.err.println("MC.init: kl-added "+added+", "+drawable.getClass().getName());

        if( showText ) {
            final int rmode = drawable.getChosenGLCapabilities().getSampleBuffers() ? 0 : Region.VBAA_RENDERING_BIT;
            textRendererGLEL = new InfoTextRendererGLELBase(gl.getGLProfile(), rmode);
            drawable.addGLEventListener(textRendererGLEL);
        }
    }

    @Override
    public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
        if(null == mPlayer) { return; }
        cube.reshape(drawable, x, y, width, height);
    }

    @Override
    public void dispose(final GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" MovieCube.dispose ... ");
        disposeImpl(drawable, true);
    }

    private void disposeImpl(final GLAutoDrawable drawable, final boolean disposePlayer) {
        if(null == mPlayer) { return; }
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeKeyListener(keyAction);
        }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( null != textRendererGLEL ) {
            drawable.disposeGLEventListener(textRendererGLEL, true);
            textRendererGLEL = null;
        }
        if( disposePlayer ) {
            mPlayer.destroy(gl);
            mPlayer=null;
        }
        cube.dispose(drawable);
        cube=null;
    }


    @Override
    public void display(final GLAutoDrawable drawable) {
        if( swapIntervalSet ) {
            final GL2ES2 gl = drawable.getGL().getGL2ES2();
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
            // System.err.println( mPlayer.getPerfString() );
            lastPerfPos = currentPos;
        }
        cube.display(drawable);
    }

    public static void main(final String[] args) throws IOException, InterruptedException, URISyntaxException {
        int swapInterval = 1;
        int width = 800;
        int height = 600;
        int textureCount = GLMediaPlayer.TEXTURE_COUNT_DEFAULT; // default - threaded

        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;
        int vid = GLMediaPlayer.STREAM_ID_AUTO;
        int aid = GLMediaPlayer.STREAM_ID_AUTO;
        final boolean origSize;

        String url_s=null;
        final String file_s=null;
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
                } else if(args[i].equals("-url")) {
                    i++;
                    url_s = args[i];
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
                } else if(args[i].equals("-wait")) {
                    waitForKey = true;
                }
            }
            origSize = _origSize;
        }
        Uri streamLoc = null;
        if( null != url_s ) {
            streamLoc = Uri.tryUriOrFile( url_s );
        }
        if( null == streamLoc ) {
            streamLoc = defURI;
        }
        System.err.println("url_s "+url_s);
        System.err.println("stream "+streamLoc);
        System.err.println("vid "+vid+", aid "+aid);
        System.err.println("textureCount "+textureCount);
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("swapInterval "+swapInterval);

        final MovieCube mc = new MovieCube(zoom_def, 0f, 0f, true);
        mc.setSwapInterval(swapInterval);

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
        final GLWindow window = GLWindow.create(caps);
        final Animator anim = new Animator(window);
        window.addWindowListener(new WindowAdapter() {
            @Override
            public void windowDestroyed(final WindowEvent e) {
                anim.stop();
            }
        });
        window.addGLEventListener(mc);
        window.setSize(width, height);
        window.setVisible(true);
        System.err.println("Chosen: "+window.getChosenGLCapabilities());
        anim.start();

        mc.mPlayer.addEventListener(new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(final GLMediaPlayer ts, final TextureFrame newFrame, final long when) {
            }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, final GLMediaPlayer.EventMask event_mask, final long when) {
                System.err.println("MovieCube.1 AttributesChanges: events_mask "+event_mask+", when "+when);
                System.err.println("MovieCube.1 State: "+mp);
                if( event_mask.isSet(GLMediaPlayer.EventMask.Bit.Size) ) {
                    if( origSize ) {
                        window.setSurfaceSize(mp.getWidth(), mp.getHeight());
                    }
                    // window.disposeGLEventListener(ms, false /* remove */ );
                }
                if( event_mask.isSet(GLMediaPlayer.EventMask.Bit.Init) ) {
                    anim.setUpdateFPSFrames(60, null);
                    anim.resetFPSCounter();
                    mc.resetGLState();
                }
                if( event_mask.isSet(GLMediaPlayer.EventMask.Bit.Play) ) {
                    anim.resetFPSCounter();
                }
                if( event_mask.isSet(GLMediaPlayer.EventMask.Bit.EOS) ) {
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            // loop for-ever ..
                            mc.mPlayer.seek(0);
                            mc.mPlayer.resume();
                        } }.start();
                }
                if( event_mask.isSet(GLMediaPlayer.EventMask.Bit.Error) ) {
                    final StreamException se = mc.mPlayer.getStreamException();
                    if( null != se ) {
                        se.printStackTrace();
                    }
                    new InterruptSource.Thread( () -> { window.destroy(); } ).start();
                }
            }
        });
        mc.playStream(streamLoc, vid, aid, textureCount);
    }
}

