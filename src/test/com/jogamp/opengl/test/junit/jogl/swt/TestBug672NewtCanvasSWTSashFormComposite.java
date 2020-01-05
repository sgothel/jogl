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

package com.jogamp.opengl.test.junit.jogl.swt;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.swt.GLCanvas;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug672NewtCanvasSWTSashFormComposite extends UITestCase {
    static int screenIdx = 0;
    static PointImmutable wpos;
    static DimensionImmutable wsize, rwsize = null;

    static long duration = 500; // ms

    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(640, 480);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    SashForm sash = null;
    Composite innerComposite = null;
    com.jogamp.newt.Display swtNewtDisplay = null;

    @Before
    public void init() {
        SWTAccessor.invoke(true, new Runnable() {
            public void run() {
                display = new Display();
                Assert.assertNotNull( display );
            }});
        display.syncExec(new Runnable() {
            public void run() {
                shell = new Shell( display );
                Assert.assertNotNull( shell );
                shell.setLayout( new FillLayout() );
                composite = new Composite( shell, SWT.NONE );
                composite.setLayout( new FillLayout() );
                Assert.assertNotNull( composite );
                sash = new SashForm(composite, SWT.NONE);
                Assert.assertNotNull( sash );
                final org.eclipse.swt.widgets.Label c = new org.eclipse.swt.widgets.Label(sash, SWT.NONE);
                c.setText("Left cell");
                innerComposite = new Composite(sash, SWT.NONE);
                Assert.assertNotNull( innerComposite );
                innerComposite.setLayout( new FillLayout() );
            }});
        swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        Assert.assertNotNull( sash );
        Assert.assertNotNull( innerComposite );
        try {
            display.syncExec(new Runnable() {
               public void run() {
                innerComposite.dispose();
                sash.dispose();
                composite.dispose();
                shell.dispose();
               }});
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                display.dispose();
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        swtNewtDisplay = null;
        display = null;
        shell = null;
        composite = null;
        sash = null;
        innerComposite = null;
    }

    class WaitAction implements Runnable {
        private final long sleepMS;

        WaitAction(final long sleepMS) {
            this.sleepMS = sleepMS;
        }
        public void run() {
            if( !display.readAndDispatch() ) {
                // blocks on linux .. display.sleep();
                try {
                    Thread.sleep(sleepMS);
                } catch (final InterruptedException e) { }
            }
        }
    }
    final WaitAction waitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    final WaitAction generalWaitAction = new WaitAction(10);

    protected void runTestGL(final boolean useNewtCanvasSWT, final GLCapabilitiesImmutable caps) throws InterruptedException, InvocationTargetException {
        final com.jogamp.newt.Screen screen = NewtFactory.createScreen(swtNewtDisplay, screenIdx);
        final GLWindow glWindow;
        final GLCanvas glCanvas;
        final GLAutoDrawable glad;
        final NewtCanvasSWT newtCanvasSWT;
        if( useNewtCanvasSWT ) {
            glWindow = GLWindow.create(screen, caps);
            glad = glWindow;
            glCanvas = null;
            Assert.assertNotNull(glWindow);
            newtCanvasSWT = NewtCanvasSWT.create( innerComposite, 0, glWindow );
            Assert.assertNotNull( newtCanvasSWT );
        } else {
            glCanvas = GLCanvas.create( innerComposite, 0, caps, null);
            glad = glCanvas;
            glWindow = null;
            Assert.assertNotNull(glCanvas);
            newtCanvasSWT = null;
        }
        Assert.assertNotNull(glad);

        final GearsES2 demo = new GearsES2(1);
        glad.addGLEventListener(demo);

        final Animator animator = new Animator();
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);

        final QuitAdapter quitAdapter = new QuitAdapter();
        if( useNewtCanvasSWT ) {
            //glWindow.addKeyListener(new TraceKeyAdapter(quitAdapter));
            //glWindow.addWindowListener(new TraceWindowAdapter(quitAdapter));
            glWindow.addKeyListener(quitAdapter);
            glWindow.addWindowListener(quitAdapter);

            glWindow.addWindowListener(new WindowAdapter() {
                public void windowResized(final WindowEvent e) {
                    System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                }
                public void windowMoved(final WindowEvent e) {
                    System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                }
            });

            glWindow.addKeyListener(new KeyAdapter() {
                public void keyReleased(final KeyEvent e) {
                    if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                        return;
                    }
                    if(e.getKeyChar()=='f') {
                        glWindow.invokeOnNewThread(null, false, new Runnable() {
                            public void run() {
                                final Thread t = glWindow.setExclusiveContextThread(null);
                                System.err.println("[set fullscreen  pre]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                                glWindow.setFullscreen(!glWindow.isFullscreen());
                                System.err.println("[set fullscreen post]: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", f "+glWindow.isFullscreen()+", a "+glWindow.isAlwaysOnTop()+", "+glWindow.getInsets());
                                glWindow.setExclusiveContextThread(t);
                        } } );
                    }
                }
            });
        }

        animator.add(glad);
        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());
        animator.setUpdateFPSFrames(60, null);

        display.syncExec( new Runnable() {
           public void run() {
              shell.setText( getSimpleTestName(".") );
              shell.setSize( wsize.getWidth(), wsize.getHeight() );
              if( null != wpos ) {
                  shell.setLocation( wpos.getX(), wpos.getY() );
              }
              shell.open();
           }
        });
        if( useNewtCanvasSWT ) {
            Assert.assertTrue("GLWindow didn't become visible natively!", NewtTestUtil.waitForRealized(glWindow, true, waitAction));
            Assert.assertNotNull( newtCanvasSWT.getNativeWindow() );
            System.err.println("NewtCanvasSWT LOS.0: "+newtCanvasSWT.getNativeWindow().getLocationOnScreen(null));
            System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
            System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
            System.err.println("window pos/siz.0: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
            System.err.println("GLWindow LOS.0: "+glWindow.getLocationOnScreen(null));
        } else {
            System.err.println("GL chosen: "+glCanvas.getChosenGLCapabilities());
            System.err.println("GLCanvas pixel-units  pos/siz.0: pos "+SWTAccessor.getLocationInPixels(glCanvas)+", size "+SWTAccessor.getSizeInPixels(glCanvas));
            System.err.println("GLCanvas window-units pos/siz.0: pos "+glCanvas.getLocation()+", size "+glCanvas.getSize());
            System.err.println("GLCanvas LOS.0: "+SWTAccessor.getLocationOnScreen(new Point(), glCanvas));
        }
        Assert.assertEquals(true,  GLTestUtil.waitForRealized(glad, true, waitAction));

        if( null != rwsize ) {
            for(int i=0; i<50; i++) { // 500 ms dispatched delay
                generalWaitAction.run();
            }
            display.syncExec( new Runnable() {
               public void run() {
                  shell.setSize( rwsize.getWidth(), rwsize.getHeight() );
               }
            });
            if( useNewtCanvasSWT ) {
                System.err.println("window resize pos/siz.1: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
                System.err.println("GLWindow LOS.1: "+glWindow.getLocationOnScreen(null));
                System.err.println("NewtCanvasSWT LOS.1: "+newtCanvasSWT.getNativeWindow().getLocationOnScreen(null));
            } else {
                System.err.println("GLCanvas pixel-units  pos/siz.1: pos "+SWTAccessor.getLocationInPixels(glCanvas)+", size "+SWTAccessor.getSizeInPixels(glCanvas));
                System.err.println("GLCanvas window-units pos/siz.1: pos "+glCanvas.getLocation()+", size "+glCanvas.getSize());
                System.err.println("GLCanvas LOS.1: "+SWTAccessor.getLocationOnScreen(new Point(), glCanvas));
            }
        }

        {
            final PointImmutable pSashRightClient = new Point(wsize.getWidth(), 0);
            final PointImmutable pGLWinLOS;
            if( useNewtCanvasSWT ) {
                final PointImmutable pNatWinLOS = newtCanvasSWT.getNativeWindow().getLocationOnScreen(null);
                pGLWinLOS = glWindow.getLocationOnScreen(null);
                System.err.println("GLWindow LOS: "+pGLWinLOS);
                System.err.println("NewtCanvasSWT LOS: "+pNatWinLOS);
                Assert.assertTrue( "NewtCanvasAWT LOS "+pNatWinLOS+" not >= sash-right "+pSashRightClient, pNatWinLOS.compareTo(pSashRightClient) >= 0 );
            } else {
                pGLWinLOS = SWTAccessor.getLocationOnScreen(new Point(), glCanvas);
                System.err.println("GLCanvas LOS: "+pGLWinLOS);
            }
            Assert.assertTrue( "GLWindow LOS "+pGLWinLOS+" not >= sash-right "+pSashRightClient, pGLWinLOS.compareTo(pSashRightClient) >= 0 );
        }

        while( !quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration ) {
            generalWaitAction.run();
        }

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());

        if( useNewtCanvasSWT ) {
            newtCanvasSWT.dispose();
            glWindow.destroy();
            Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow, false, null));
        } else {
            glCanvas.dispose();
        }
    }

    @Test
    public void test01_SashFormNewtCanvasSWT() throws InterruptedException, InvocationTargetException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(true /* NewtCanvasSWT */, caps);
    }

    @Test
    public void test02_SashFormGLCanvas() throws InterruptedException, InvocationTargetException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(false /* NewtCanvasSWT */, caps);
    }

    public static void main(final String args[]) throws IOException {
        int x=0, y=0, w=640, h=480, rw=-1, rh=-1;
        boolean usePos = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
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
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-screen")) {
                i++;
                screenIdx = MiscUtils.atoi(args[i], 0);
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

        org.junit.runner.JUnitCore.main(TestBug672NewtCanvasSWTSashFormComposite.class.getName());
    }
}
