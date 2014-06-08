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
import java.net.URLConnection;

import com.jogamp.common.util.IOUtil;
import com.jogamp.newt.Display;
import com.jogamp.newt.Display.PointerIcon;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Screen;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.util.EDTUtil;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.PNGPixelRect;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import javax.media.nativewindow.NativeWindowFactory;
import javax.media.nativewindow.ScalableSurface;
import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.Point;
import javax.media.nativewindow.util.PointImmutable;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

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
    static int[] reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static long duration = 500; // ms
    static boolean opaque = true;
    static int forceAlpha = -1;
    static boolean undecorated = false;
    static boolean alwaysOnTop = false;
    static boolean fullscreen = false;
    static boolean pmvUseBackingArray = true;
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

    private void setTitle(final Window win, final GLCapabilitiesImmutable caps) {
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        win.setTitle("GLWindow["+capsA+"], swapI "+swapInterval+", win: "+win.getBounds()+", pix: "+win.getSurfaceWidth()+"x"+win.getSurfaceHeight());
    }
    protected void runTestGL(final GLCapabilitiesImmutable caps, boolean undecorated) throws InterruptedException {
        System.err.println("requested: vsync "+swapInterval+", "+caps);
        Display dpy = NewtFactory.createDisplay(null);
        Screen screen = NewtFactory.createScreen(dpy, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);
        glWindow.setSurfaceScale(reqSurfacePixelScale);
        final int[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new int[2]);
        glWindow.setSize(wsize.getWidth(), wsize.getHeight());
        if(null != wpos) {
            glWindow.setPosition(wpos.getX(), wpos.getY());
        }
        glWindow.setUndecorated(undecorated);
        glWindow.setAlwaysOnTop(alwaysOnTop);
        glWindow.setFullscreen(fullscreen);
        glWindow.setPointerVisible(mouseVisible);
        glWindow.confinePointer(mouseConfined);
        setTitle(glWindow, caps);

        final GearsES2 demo = new GearsES2(swapInterval);
        demo.setPMVUseBackingArray(pmvUseBackingArray);
        demo.setUseMappedBuffers(useMappedBuffers);
        demo.setValidateBuffers(true);
        glWindow.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glWindow.addGLEventListener(snap);
        if(waitForKey) {
            glWindow.addGLEventListener(new GLEventListener() {
                public void init(GLAutoDrawable drawable) { }
                public void dispose(GLAutoDrawable drawable) { }
                public void display(GLAutoDrawable drawable) {
                    GLAnimatorControl  actrl = drawable.getAnimator();
                    if(waitForKey && actrl.getTotalFPSFrames() == 60*3) {
                        UITestCase.waitForKey("3s mark");
                        actrl.resetFPSCounter();
                        waitForKey = false;
                    }
                }
                public void reshape(GLAutoDrawable drawable, int x, int y,
                        int width, int height) { }
            });
        }

        final Animator animator = useAnimator ? new Animator() : null;
        if( useAnimator ) {
            animator.setModeBits(false, Animator.MODE_EXPECT_AWT_RENDERING_THREAD);
            animator.setExclusiveContext(exclusiveContext);
        }

        final QuitAdapter quitAdapter = new QuitAdapter();
        //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
        //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
        glWindow.addKeyListener(quitAdapter);
        glWindow.addWindowListener(quitAdapter);

        glWindow.addWindowListener(new WindowAdapter() {
            public void windowResized(WindowEvent e) {
                System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                setTitle(glWindow, caps);
            }
            public void windowMoved(WindowEvent e) {
                System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                setTitle(glWindow, caps);
            }
        });

        final PointerIcon[] pointerIcons = { null, null, null };
        {
            final Display disp = glWindow.getScreen().getDisplay();
            disp.createNative();
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/cross-grey-alpha-16x16.png" } );
                try {
                    _pointerIcon = disp.createPointerIcon(res, 8, 8);
                    System.err.println("Create PointerIcon #01: "+_pointerIcon);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[0] = _pointerIcon;
            }
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "newt/data/pointer-grey-alpha-16x24.png" } );
                try {
                    _pointerIcon = disp.createPointerIcon(res, 0, 0);
                    System.err.println("Create PointerIcon #02: "+_pointerIcon);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[1] = _pointerIcon;
            }
            {
                PointerIcon _pointerIcon = null;
                final IOUtil.ClassResources res = new IOUtil.ClassResources(glWindow.getClass(), new String[] { "jogamp-pointer-64x64.png" } );
                try {
                    final URLConnection urlConn = res.resolve(0);
                    final PNGPixelRect image = PNGPixelRect.read(urlConn.getInputStream(), null, false /* directBuffer */, 0 /* destMinStrideInBytes */, false /* destIsGLOriented */);
                    System.err.println("Create PointerIcon #03: "+image);
                    _pointerIcon = disp.createPointerIcon(image, 32, 0);
                    System.err.println("Create PointerIcon #03: "+_pointerIcon);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                pointerIcons[2] = _pointerIcon;
            }
        }
        if( setPointerIcon ) {
            glWindow.setPointerIcon(pointerIcons[0]);
            System.err.println("Set PointerIcon: "+glWindow.getPointerIcon());
        }

        glWindow.addKeyListener(new KeyAdapter() {
            int pointerIconIdx = 0;

            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='f') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            if( glWindow.isFullscreen() ) {
                                glWindow.setFullscreen( false );
                            } else {
                                if( e.isAltDown() ) {
                                    glWindow.setFullscreen( null );
                                } else {
                                    glWindow.setFullscreen( true );
                                }
                            }
                            System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='a') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set alwaysontop pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setAlwaysOnTop(!glWindow.isAlwaysOnTop());
                            System.err.println("[set alwaysontop post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='d') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            // while( null != glWindow.getExclusiveContextThread() ) ;
                            System.err.println("[set undecorated  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                            glWindow.setUndecorated(!glWindow.isUndecorated());
                            System.err.println("[set undecorated post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", d "+glWindow.isUndecorated()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='s') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set position  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
                            glWindow.setPosition(100, 100);
                            System.err.println("[set position post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='c') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set pointer-icon pre]");
                            final PointerIcon currentPI = glWindow.getPointerIcon();
                            final PointerIcon newPI;
                            if( pointerIconIdx >= pointerIcons.length ) {
                                newPI=null;
                                pointerIconIdx=0;
                            } else {
                                newPI=pointerIcons[pointerIconIdx++];
                            }
                            glWindow.setPointerIcon( newPI );
                            System.err.println("[set pointer-icon post] "+currentPI+" -> "+glWindow.getPointerIcon());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='i') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse visible pre]: "+glWindow.isPointerVisible());
                            glWindow.setPointerVisible(!glWindow.isPointerVisible());
                            System.err.println("[set mouse visible post]: "+glWindow.isPointerVisible());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='j') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            if(!glWindow.isPointerConfined()) {
                                demo.setConfinedFixedCenter(false);
                            }
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='J') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse confined pre]: "+glWindow.isPointerConfined());
                            glWindow.confinePointer(!glWindow.isPointerConfined());
                            System.err.println("[set mouse confined post]: "+glWindow.isPointerConfined());
                            demo.setConfinedFixedCenter(glWindow.isPointerConfined());
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='w') {
                    new Thread() {
                        public void run() {
                            final Thread t = glWindow.setExclusiveContextThread(null);
                            System.err.println("[set mouse pos pre]");
                            glWindow.warpPointer(glWindow.getSurfaceWidth()/2, glWindow.getSurfaceHeight()/2);
                            System.err.println("[set mouse pos post]");
                            glWindow.setExclusiveContextThread(t);
                    } }.start();
                } else if(e.getKeyChar()=='x') {
                    final int[] hadSurfacePixelScale = glWindow.getCurrentSurfaceScale(new int[2]);
                    final int[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new int[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    glWindow.setSurfaceScale(reqSurfacePixelScale);
                    final int[] valReqSurfacePixelScale = glWindow.getRequestedSurfaceScale(new int[2]);
                    final int[] hasSurfacePixelScale0 = glWindow.convertToPixelUnits(new int[] { 1, 1 });
                    final int[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new int[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                                       reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                                       valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                                       hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                    setTitle(glWindow, caps);
                    Assert.assertArrayEquals(hasSurfacePixelScale0, hasSurfacePixelScale1);
                }
            }
        });
        glWindow.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2 && e.getPointerCount() == 1) {
                    glWindow.setFullscreen(!glWindow.isFullscreen());
                    System.err.println("setFullscreen: "+glWindow.isFullscreen());
                }
            }
         });

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
                public void init(GLAutoDrawable drawable) {}
                @Override
                public void dispose(GLAutoDrawable drawable) { }
                @Override
                public void display(GLAutoDrawable drawable) {
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
                                        quitAdapter.doQuit();
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
                public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
            });
        }

        glWindow.setVisible(true);
        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, showFPS ? System.err : null);
        }

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());

        final int[] hasSurfacePixelScale0 = glWindow.convertToPixelUnits(new int[] { 1, 1 });
        final int[] hasSurfacePixelScale1 = glWindow.getCurrentSurfaceScale(new int[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        setTitle(glWindow, caps);
        Assert.assertArrayEquals(hasSurfacePixelScale0, hasSurfacePixelScale1);

        snap.setMakeSnapshot();

        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay
            glWindow.setSize(rwsize.getWidth(), rwsize.getHeight());
            System.err.println("window resize pos/siz: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
        }

        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1-t0<duration) {
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
                            quitAdapter.doQuit();
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

        GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        runTestGL(caps, undecorated);
    }

    public static void main(String args[]) throws IOException {
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
            } else if(args[i].equals("-fullscreen")) {
                fullscreen = true;
            } else if(args[i].equals("-pmvDirect")) {
                pmvUseBackingArray = false;
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
                final int pS = MiscUtils.atoi(args[i], reqSurfacePixelScale[0]);
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
        System.err.println("fullscreen "+fullscreen);
        System.err.println("pmvDirect "+(!pmvUseBackingArray));
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

        if(waitForKey) {
            UITestCase.waitForKey("Start");
        }
        org.junit.runner.JUnitCore.main(TestGearsES2NEWT.class.getName());
    }
}
