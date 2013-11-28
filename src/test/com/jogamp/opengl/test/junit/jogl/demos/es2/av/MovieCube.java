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
import java.net.URI;
import java.net.URISyntaxException;

import javax.media.opengl.GL2ES2;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLException;
import javax.media.opengl.GLProfile;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.JoglVersion;
import com.jogamp.opengl.test.junit.jogl.demos.es2.TextureSequenceCubeES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.av.GLMediaPlayer;
import com.jogamp.opengl.util.av.GLMediaPlayer.GLMediaEventListener;
import com.jogamp.opengl.util.av.GLMediaPlayer.StreamException;
import com.jogamp.opengl.util.av.GLMediaPlayerFactory;
import com.jogamp.opengl.util.texture.TextureSequence.TextureFrame;

/**
 * Simple cube movie player w/ aspect ration true projection on a cube.
 */
public class MovieCube implements GLEventListener {
    private static boolean waitForKey = false;
    private final float zoom0, rotx, roty;
    private TextureSequenceCubeES2 cube=null;
    private GLMediaPlayer mPlayer=null;
    private int swapInterval = 1;
    private long lastPerfPos = 0;
    private volatile boolean resetGLState = false;

    /** Blender's Big Buck Bunny Trailer: 24f 640p VP8, Vorbis 44100Hz mono, WebM/Matroska Stream. */
    public static final URI defURI;
    static {
        URI _defURI = null;
        try {
            _defURI = new URI("http://video.webmfiles.org/big-buck-bunny_trailer.webm");
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        defURI = _defURI;
    }

    /**
     * Default constructor which also issues {@link #initStream(URI, int, int, int)} w/ default values
     * and polls until the {@link GLMediaPlayer} is {@link GLMediaPlayer.State#Initialized}.
     * If {@link GLMediaEventListener#EVENT_CHANGE_EOS} is reached, the stream is started over again.
     * <p>
     * This default constructor is merely useful for some <i>drop-in</i> test, e.g. using an applet.
     * </p>
     */
    public MovieCube() throws IOException, URISyntaxException {
        this(-2.3f, 0f, 0f);

        mPlayer.addEventListener(new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(GLMediaPlayer ts, TextureFrame newFrame, long when) { }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, int event_mask, long when) {
                System.err.println("MovieCube AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                System.err.println("MovieCube State: "+mp);
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_EOS & event_mask ) ) {
                    // loop for-ever ..
                    mPlayer.seek(0);
                    mPlayer.play();
                }
            }
        });
        initStream(defURI, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.STREAM_ID_AUTO, GLMediaPlayer.TEXTURE_COUNT_DEFAULT);
        StreamException se = null;
        while( null == se && GLMediaPlayer.State.Initialized != mPlayer.getState() ) {
            try {
                Thread.sleep(16);
            } catch (InterruptedException e) { }
            se = mPlayer.getStreamException();
        }
        if( null != se ) {
            se.printStackTrace();
            throw new RuntimeException(se);
        }
    }

    /** Custom constructor, user needs to issue {@link #initStream(URI, int, int, int)} afterwards. */
    public MovieCube(float zoom0, float rotx, float roty) throws IOException {
        this.zoom0 = zoom0;
        this.rotx = rotx;
        this.roty = roty;
        mPlayer = GLMediaPlayerFactory.createDefault();
    }

    public void initStream(URI streamLoc, int vid, int aid, int textureCount) {
        mPlayer.initStream(streamLoc, vid, aid, textureCount);
        System.out.println("pC.1b "+mPlayer);
    }

    public void setSwapInterval(int v) { this.swapInterval = v; }

    public GLMediaPlayer getGLMediaPlayer() { return mPlayer; }

    public void resetGLState() {
        resetGLState = true;
    }

    private final KeyListener keyAction = new KeyAdapter() {
        public void keyReleased(KeyEvent e)  {
            if( e.isAutoRepeat() ) {
                return;
            }
            System.err.println("MC "+e);
            int pts0 = mPlayer.getVideoPTS();
            int pts1 = 0;
            switch(e.getKeyCode()) {
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
        }
    };

    @Override
    public void init(GLAutoDrawable drawable) {
        if(null == mPlayer) {
            throw new InternalError("mPlayer null");
        }
        if( GLMediaPlayer.State.Uninitialized == mPlayer.getState() ) {
            throw new IllegalStateException("mPlayer in uninitialized state: "+mPlayer);
        }
        if( GLMediaPlayer.STREAM_ID_NONE == mPlayer.getVID() ) {
            // throw new IllegalStateException("mPlayer has no VID/stream selected: "+mPlayer);
        }
        resetGLState = false;

        GL2ES2 gl = drawable.getGL().getGL2ES2();
        System.err.println(JoglVersion.getGLInfo(gl, null));

        cube = new TextureSequenceCubeES2(mPlayer, false, zoom0, rotx, roty);

        if(waitForKey) {
            UITestCase.waitForKey("Init>");
        }

        if( GLMediaPlayer.State.Initialized == mPlayer.getState() ) {
            try {
                mPlayer.initGL(gl);
            } catch (Exception e) {
                e.printStackTrace();
                if(null != mPlayer) {
                    mPlayer.destroy(gl);
                    mPlayer = null;
                }
                throw new GLException(e);
            }
        }
        cube.init(drawable);
        mPlayer.play();
        System.out.println("play.0 "+mPlayer);

        boolean added;
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.addKeyListener(keyAction);
            added = true;
        } else { added = false; }
        System.err.println("MC.init: kl-added "+added+", "+drawable.getClass().getName());
    }

    @Override
    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if(-1 != swapInterval) {
            gl.setSwapInterval(swapInterval); // in case switching the drawable (impl. may bound attribute there)
        }
        if(null == mPlayer) { return; }
        cube.reshape(drawable, x, y, width, height);
    }

    @Override
    public void dispose(GLAutoDrawable drawable) {
        System.err.println(Thread.currentThread()+" MovieCube.dispose ... ");
        disposeImpl(drawable, true);
    }

    private void disposeImpl(GLAutoDrawable drawable, boolean disposePlayer) {
        if(null == mPlayer) { return; }
        final Object upstreamWidget = drawable.getUpstreamWidget();
        if (upstreamWidget instanceof Window) {
            final Window window = (Window) upstreamWidget;
            window.removeKeyListener(keyAction);
        }
        final GL2ES2 gl = drawable.getGL().getGL2ES2();
        if( disposePlayer ) {
            mPlayer.destroy(gl);
            mPlayer=null;
        }
        cube.dispose(drawable);
        cube=null;
    }


    @Override
    public void display(GLAutoDrawable drawable) {
        if(null == mPlayer) { return; }

        if( resetGLState ) {
            resetGLState = false;
            System.err.println("XXX resetGLState");
            disposeImpl(drawable, false);
            init(drawable);
            reshape(drawable, 0, 0, drawable.getWidth(), drawable.getHeight());
        }

        final long currentPos = System.currentTimeMillis();
        if( currentPos - lastPerfPos > 2000 ) {
            System.err.println( mPlayer.getPerfString() );
            lastPerfPos = currentPos;
        }
        cube.display(drawable);
    }

    public static void main(String[] args) throws IOException, InterruptedException, URISyntaxException {
        int swapInterval = 1;
        int width = 510;
        int height = 300;
        int textureCount = GLMediaPlayer.TEXTURE_COUNT_DEFAULT; // default - threaded

        boolean forceES2 = false;
        boolean forceES3 = false;
        boolean forceGL3 = false;
        boolean forceGLDef = false;
        int vid = GLMediaPlayer.STREAM_ID_AUTO;
        int aid = GLMediaPlayer.STREAM_ID_AUTO;
        final boolean origSize;

        String url_s=null, file_s=null;
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
                } else if(args[i].equals("-file")) {
                    i++;
                    file_s = args[i];
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
        final URI streamLoc;
        if( null != url_s ) {
            streamLoc = new URI(url_s);
        } else if( null != file_s ) {
            streamLoc = IOUtil.toURISimple(new File(file_s));
        } else {
            streamLoc = defURI;
        }
        System.err.println("url_s "+url_s);
        System.err.println("file_s "+file_s);
        System.err.println("stream "+streamLoc);
        System.err.println("vid "+vid+", aid "+aid);
        System.err.println("textureCount "+textureCount);
        System.err.println("forceES2   "+forceES2);
        System.err.println("forceES3   "+forceES3);
        System.err.println("forceGL3   "+forceGL3);
        System.err.println("forceGLDef "+forceGLDef);
        System.err.println("swapInterval "+swapInterval);

        final MovieCube mc = new MovieCube(-2.3f, 0f, 0f);
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
        final GLWindow window = GLWindow.create(new GLCapabilities(glp));
        final Animator anim = new Animator(window);
        window.addWindowListener(new WindowAdapter() {
            public void windowDestroyed(WindowEvent e) {
                anim.stop();
            }
        });
        window.setSize(width, height);
        window.setVisible(true);
        anim.start();

        mc.mPlayer.addEventListener(new GLMediaEventListener() {
            @Override
            public void newFrameAvailable(GLMediaPlayer ts, TextureFrame newFrame, long when) {
            }

            @Override
            public void attributesChanged(final GLMediaPlayer mp, int event_mask, long when) {
                System.err.println("MovieCube AttributesChanges: events_mask 0x"+Integer.toHexString(event_mask)+", when "+when);
                System.err.println("MovieCube State: "+mp);
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_SIZE & event_mask ) ) {
                    if( origSize ) {
                        window.setSize(mp.getWidth(), mp.getHeight());
                    }
                    // window.disposeGLEventListener(ms, false /* remove */ );
                    mc.resetGLState();
                }
                if( 0 != ( GLMediaEventListener.EVENT_CHANGE_INIT & event_mask ) ) {
                    window.addGLEventListener(mc);
                    anim.setUpdateFPSFrames(60, System.err);
                    anim.resetFPSCounter();
                }
                if( 0 != ( ( GLMediaEventListener.EVENT_CHANGE_ERR | GLMediaEventListener.EVENT_CHANGE_EOS ) & event_mask ) ) {
                    final StreamException se = mc.mPlayer.getStreamException();
                    if( null != se ) {
                        se.printStackTrace();
                    }
                    new Thread() {
                        public void run() {
                            window.destroy();
                        } }.start();
                }
            }
        });
        mc.initStream(streamLoc, vid, aid, textureCount);
    }
}

