/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.demos.es2.newt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.junit.util.JunitTracer;
import com.jogamp.newt.Display;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceMouseAdapter;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.opengl.util.NEWTDemoListener;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.test.junit.jogl.demos.GLClearOnInitReshape;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.LineSquareXDemoES2;
import com.jogamp.nativewindow.NativeWindowFactory;
import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import jogamp.newt.DefaultEDTUtil;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2NEWT extends UITestCase {
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize, rwsize=null;
    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static long duration = 500; // ms
    static boolean opaque = true;
    static int forceAlpha = -1;
    static boolean undecorated = false;
    static boolean alwaysOnTop = false;
    static boolean alwaysOnBottom = false;
    static boolean resizable = true;
    static boolean sticky = false;
    static boolean max_vert= false;
    static boolean max_horz= false;
    static boolean fullscreen = false;
    static int swapInterval = 1;
    static boolean waitForKey = false;
    static boolean mouseVisible = true;
    static boolean mouseConfined = false;
    static boolean setPointerIcon = false;
    static boolean showFPS = false;
    static int loops = 1;
    static boolean loop_shutdown = false;
    static boolean forceES2 = false;
    static boolean forceES3 = false;
    static boolean forceGL3 = false;
    static boolean forceGL2 = false;
    static int demoType = 1;
    static boolean traceMouse = false;
    static boolean manualTest = false;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;
    static boolean useMappedBuffers = false;
    static enum SysExit { none, testExit, testError, testEDTError, displayExit, displayError, displayEDTError };
    static SysExit sysExit = SysExit.none;

    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(640, 480);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(final GLCapabilitiesImmutable caps, final boolean undecorated) throws InterruptedException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        final Display dpy = NewtFactory.createDisplay(null);
        final Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);
        glWindow.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new float[2]);
        glWindow.setSize(wsize.getWidth(), wsize.getHeight());
        if(null != wpos) {
            glWindow.setPosition(wpos.getX(), wpos.getY());
        }
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setAlwaysOnBottom(alwaysOnBottom);
        glWindow.setResizable(resizable);
        glWindow.setSticky(sticky);
        glWindow.setMaximized(max_horz, max_vert);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);

        final GLEventListener demo;
        if( 2 == demoType ) {
            final LineSquareXDemoES2 demo2 = new LineSquareXDemoES2(false);
            demo = demo2;
        } else if( 1 == demoType ) {
            final GearsES2 gearsES2 = new GearsES2(swapInterval);
            gearsES2.setUseMappedBuffers(useMappedBuffers);
            gearsES2.setValidateBuffers(true);
            demo = gearsES2;
        } else if( 0 == demoType ) {
            demo = new GLClearOnInitReshape();
        } else {
            demo = null;
        }
        if( null != demo ) {
            glWindow.addGLEventListener(demo);
        }

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);
        if(waitForKey) {
            glWindow.addGLEventListener(new GLEventListener() {
                public void init(final GLAutoDrawable drawable) { }
                public void dispose(final GLAutoDrawable drawable) { }
                public void display(final GLAutoDrawable drawable) {
                    final GLAnimatorControl  actrl = drawable.getAnimator();
                    if(waitForKey && actrl.getTotalFPSFrames() == 60*3) {
                        JunitTracer.waitForKey("3s mark");
                        actrl.resetFPSCounter();
                        waitForKey = false;
                    }
                }
                public void reshape(final GLAutoDrawable drawable, final int x, final int y,
                        final int width, final int height) { }
            });
        }

        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(final WindowEvent e) {
                System.err.println("window resized: "+glWindow.getBounds()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                NEWTDemoListener.setTitle(glWindow);
            }
            public void windowMoved(final WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getBounds()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                NEWTDemoListener.setTitle(glWindow);
            }
        });

        final GLWindow[] glWindow2 = { null };

        final NEWTDemoListener newtDemoListener = new NEWTDemoListener(glWindow);
        newtDemoListener.quitAdapterEnable(true);
        glWindow.addKeyListener(newtDemoListener);
        if( traceMouse ) {
            glWindow.addMouseListener(new TraceMouseAdapter());
        }
        glWindow.addMouseListener(newtDemoListener);
        glWindow.addWindowListener(newtDemoListener);
        glWindow.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='n') {
                    if( null != glWindow2[0] && glWindow2[0].isNativeValid() ) {
                        glWindow2[0].destroy();
                        glWindow2[0] = null;
                    } else {
                        glWindow2[0] = GLWindow.create(screen, caps);
                        glWindow2[0].setTitle("GLWindow2");
                        glWindow2[0].setPosition(glWindow.getX()+glWindow.getWidth()+64, glWindow.getY());
                        glWindow2[0].setSize(glWindow.getWidth(), glWindow.getHeight());
                        glWindow2[0].addGLEventListener(new LineSquareXDemoES2(false));
                        final Animator animator2 = useAnimator ? new Animator(glWindow2[0]) : null;
                        if( null != animator2 ) {
                            animator2.start();
                        }
                        glWindow2[0].setVisible(true);
                    }
                }
            } } );

        if( useAnimator ) {
            animator.add(glWindow);
            animator.start();
            Assert.assertTrue(animator.isStarted());
            Assert.assertTrue(animator.isAnimating());
            Assert.assertEquals(exclusiveContext ? animator.getThread() : null, glWindow.getExclusiveContextThread());
        }

        if( SysExit.displayError == sysExit || SysExit.displayExit == sysExit || SysExit.displayEDTError == sysExit ) {
            glWindow.addGLEventListener(new GLEventListener() {
                @Override
                public void init(final GLAutoDrawable drawable) {}
                @Override
                public void dispose(final GLAutoDrawable drawable) { }
                @Override
                public void display(final GLAutoDrawable drawable) {
                    final GLAnimatorControl anim = drawable.getAnimator();
                    if( null != anim && anim.isAnimating() ) {
                        final long ms = anim.getTotalFPSDuration();
                        if( ms >= duration/2 || ms >= 3000 ) { // max 3s wait until provoking error
                            if( SysExit.displayError == sysExit ) {
                                throw new Error("test error send from GLEventListener.display - "+Thread.currentThread());
                            } else if ( SysExit.displayExit == sysExit ) {
                                System.err.println("exit(0) send from GLEventListener");
                                System.exit(0);
                            } else if ( SysExit.displayEDTError == sysExit ) {
                                final Object upstream = drawable.getUpstreamWidget();
                                System.err.println("EDT invokeAndWaitError: upstream type "+upstream.getClass().getName());
                                if( upstream instanceof Window ) {
                                    final EDTUtil edt = ((Window)upstream).getScreen().getDisplay().getEDTUtil();
                                    System.err.println("EDT invokeAndWaitError: edt type "+edt.getClass().getName());
                                    if( edt instanceof DefaultEDTUtil ) {
                                        newtDemoListener.doQuit();
                                        ((DefaultEDTUtil)edt).invokeAndWaitError(new Runnable() {
                                            public void run() {
                                                throw new RuntimeException("XXX Should never ever be seen! - "+Thread.currentThread());
                                            }
                                        });
                                    }
                                }
                            }
                        }
                    } else {
                        System.exit(0);
                    }
                }
                @Override
                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
            });
        }

        glWindow.setVisible(true);
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        }

        System.err.println("Window Current State   : "+glWindow.getStateMaskString());
        System.err.println("Window Supported States: "+glWindow.getSupportedStateMaskString());
        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        final float[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new float[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        NEWTDemoListener.setTitle(glWindow);

        snap.setMakeSnapshot();

        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay
            glWindow.setSize(rwsize.getWidth(), rwsize.getHeight());
            System.err.println("window resize pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
        }

        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!newtDemoListener.shouldQuit() && t1-t0<duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
            if( SysExit.testError == sysExit || SysExit.testExit == sysExit || SysExit.testEDTError == sysExit) {
                final long ms = t1-t0;
                if( ms >= duration/2 || ms >= 3000 ) { // max 3s wait until provoking error
                    if( SysExit.testError == sysExit ) {
                        throw new Error("test error send from test thread");
                    } else if ( SysExit.testExit == sysExit ) {
                        System.err.println("exit(0) send from test thread");
                        System.exit(0);
                    } else if ( SysExit.testEDTError == sysExit ) {
                        final EDTUtil edt = glWindow.getScreen().getDisplay().getEDTUtil();
                        System.err.println("EDT invokeAndWaitError: edt type "+edt.getClass().getName());
                        if( edt instanceof DefaultEDTUtil ) {
                            newtDemoListener.doQuit();
                            ((DefaultEDTUtil)edt).invokeAndWaitError(new Runnable() {
                                public void run() {
                                    throw new RuntimeException("XXX Should never ever be seen!");
                                }
                            });
                        }
                    }
                }
            }
        }

        if( useAnimator ) {
            Assert.assertEquals(exclusiveContext ? animator.getThread() : null, glWindow.getExclusiveContextThread());
            animator.stop();
            Assert.assertFalse(animator.isAnimating());
            Assert.assertFalse(animator.isStarted());
        }
        Assert.assertEquals(null, glWindow.getExclusiveContextThread());
        glWindow.destroy();
        if( null != glWindow2[0] && glWindow2[0].isNativeValid() ) {
            glWindow2[0].destroy();
            glWindow2[0] = null;
        }
        if( NativeWindowFactory.isAWTAvailable() ) {
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
        }
    }

    @Test
    public void test01_GL2ES2() throws InterruptedException {
        for(int i=1; i<=loops; i++) {
            System.err.println("Loop "+i+"/"+loops);
            final GLProfile glp;
            if(forceGL3) {
                glp = GLProfile.get(GLProfile.GL3);
            } else if(forceES3) {
                glp = GLProfile.get(GLProfile.GLES3);
            } else if(forceES2) {
                glp = GLProfile.get(GLProfile.GLES2);
            } else if(forceGL2) {
                glp = GLProfile.get(GLProfile.GL2);
            } else {
                glp = GLProfile.getGL2ES2();
            }
            final GLCapabilities caps = new GLCapabilities( glp );
            caps.setBackgroundOpaque(opaque);
            if(-1 < forceAlpha) {
                caps.setAlphaBits(forceAlpha);
            }
            runTestGL(caps, undecorated);
            if(loop_shutdown) {
                GLProfile.shutdown();
            }
        }
    }

    @Test
    public void test02_GLES2() throws InterruptedException {
        if(manualTest) return;

        if( !GLProfile.isAvailable(GLProfile.GLES2) ) {
            System.err.println("GLES2 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GLES2);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, undecorated);
    }

    @Test
    public void test03_GL3() throws InterruptedException {
        if(manualTest) return;

        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, undecorated);
    }

    @Test
    public void test99_PixelScale1_DefaultNorm() throws InterruptedException, InvocationTargetException {
        if( manualTest ) return;

        reqSurfacePixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
        reqSurfacePixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;

        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        runTestGL(caps, undecorated);
    }

    public static void main(final String args[]) throws IOException {
        int x=0, y=0, w=640, h=480, rw=-1, rh=-1;
        boolean usePos = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-translucent")) {
                opaque = false;
            } else if(args[i].equals("-forceAlpha")) {
                i++;
                forceAlpha = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-undecorated")) {
                undecorated = true;
            } else if(args[i].equals("-atop")) {
                alwaysOnTop = true;
            } else if(args[i].equals("-abottom")) {
                alwaysOnBottom = true;
            } else if(args[i].equals("-noresize")) {
                resizable = false;
            } else if(args[i].equals("-sticky")) {
                sticky = true;
            } else if(args[i].equals("-maxv")) {
                max_vert = true;
            } else if(args[i].equals("-maxh")) {
                max_horz = true;
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-es3")) {
                forceES3 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-gl2")) {
                forceGL2 = true;
            } else if(args[i].equals("-mappedBuffers")) {
                useMappedBuffers = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-mouseInvisible")) {
                mouseVisible = false;
            } else if(args[i].equals("-mouseConfine")) {
                mouseConfined = true;
            } else if(args[i].equals("-pointerIcon")) {
                setPointerIcon = true;
            } else if(args[i].equals("-showFPS")) {
                showFPS = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
            } else if(args[i].equals("-x")) {
                i++;
                x = MiscUtils.atoi(args[i], x);
                usePos = true;
            } else if(args[i].equals("-y")) {
                i++;
                y = MiscUtils.atoi(args[i], y);
                usePos = true;
            } else if(args[i].equals("-pixelScale")) {
                i++;
                final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[0] = pS;
                reqSurfacePixelScale[1] = pS;
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-loops")) {
                i++;
                loops = MiscUtils.atoi(args[i], 1);
            } else if(args[i].equals("-loop-shutdown")) {
                loop_shutdown = true;
            } else if(args[i].equals("-sysExit")) {
                i++;
                sysExit = SysExit.valueOf(args[i]);
            } else if(args[i].equals("-manual")) {
                manualTest = true;
            } else if(args[i].equals("-demo")) {
                i++;
                demoType = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-traceMouse")) {
                traceMouse = true;
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        if(usePos) {
            wpos = new Point(x, y);
        }
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);
        System.err.println("screen "+screenIdx);
        System.err.println("translucent "+(!opaque));
        System.err.println("forceAlpha "+forceAlpha);
        System.err.println("undecorated "+undecorated);
        System.err.println("atop "+alwaysOnTop);
        System.err.println("abottom "+alwaysOnBottom);
        System.err.println("resizable "+resizable);
        System.err.println("sticky "+sticky);
        System.err.println("max_vert "+max_vert);
        System.err.println("max_horz "+max_horz);
        System.err.println("fullscreen "+fullscreen);
        System.err.println("mouseVisible "+mouseVisible);
        System.err.println("mouseConfined "+mouseConfined);
        System.err.println("pointerIcon "+setPointerIcon);
        System.err.println("loops "+loops);
        System.err.println("loop shutdown "+loop_shutdown);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceES3 "+forceES3);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("forceGL2 "+forceGL2);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("sysExitWithin "+sysExit);
        System.err.println("mappedBuffers "+useMappedBuffers);
        System.err.println("demoType "+demoType);
        System.err.println("traceMouse "+traceMouse);

        if(waitForKey) {
            JunitTracer.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestGearsES2NEWT.class.getName());
    }
}
