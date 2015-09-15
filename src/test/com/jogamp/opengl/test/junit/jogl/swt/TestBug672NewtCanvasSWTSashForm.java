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
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;

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
public class TestBug672NewtCanvasSWTSashForm extends UITestCase {
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
            }});
        swtNewtDisplay = NewtFactory.createDisplay(null, false); // no-reuse
    }

    @After
    public void release() {
        Assert.assertNotNull( display );
        Assert.assertNotNull( shell );
        Assert.assertNotNull( composite );
        Assert.assertNotNull( sash );
        try {
            display.syncExec(new Runnable() {
               public void run() {
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
    final WaitAction awtRobotWaitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    final WaitAction generalWaitAction = new WaitAction(10);

    protected void runTestGL(final GLCapabilitiesImmutable caps) throws InterruptedException, InvocationTargetException {
        final com.jogamp.newt.Screen screen = NewtFactory.createScreen(swtNewtDisplay, screenIdx);
        final GLWindow glWindow = GLWindow.create(screen, caps);
        Assert.assertNotNull(glWindow);

        final GearsES2 demo = new GearsES2(1);
        glWindow.addGLEventListener(demo);

        final Animator animator = new Animator();
        animator.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);

        final QuitAdapter quitAdapter = new QuitAdapter();
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

        animator.add(glWindow);
        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());
        animator.setUpdateFPSFrames(60, null);
        final NewtCanvasSWT canvas1 = NewtCanvasSWT.create( sash, 0, glWindow );
        Assert.assertNotNull( canvas1 );

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
        Assert.assertTrue("GLWindow didn't become visible natively!", AWTRobotUtil.waitForRealized(glWindow, awtRobotWaitAction, true));
        Assert.assertNotNull( canvas1.getNativeWindow() );

        System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
        System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        System.err.println("window pos/siz.0: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
        System.err.println("GLWindow LOS.0: "+glWindow.getLocationOnScreen(null));
        System.err.println("NewtCanvasSWT LOS.0: "+canvas1.getNativeWindow().getLocationOnScreen(null));

        if( null != rwsize ) {
            for(int i=0; i<50; i++) { // 500 ms dispatched delay
                generalWaitAction.run();
            }
            display.syncExec( new Runnable() {
               public void run() {
                  shell.setSize( rwsize.getWidth(), rwsize.getHeight() );
               }
            });
            System.err.println("window resize pos/siz.1: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
            System.err.println("GLWindow LOS.1: "+glWindow.getLocationOnScreen(null));
            System.err.println("NewtCanvasSWT LOS.1: "+canvas1.getNativeWindow().getLocationOnScreen(null));
        }

        final PointImmutable pSashRightClient = new Point(wsize.getWidth(), 0);
        final PointImmutable pNatWinLOS = canvas1.getNativeWindow().getLocationOnScreen(null);
        final PointImmutable pGLWinLOS = glWindow.getLocationOnScreen(null);

        System.err.println("GLWindow LOS: "+pGLWinLOS);
        System.err.println("NewtCanvasSWT LOS: "+pNatWinLOS);

        Assert.assertTrue( "NewtCanvasAWT LOS "+pNatWinLOS+" not >= sash-right "+pSashRightClient, pNatWinLOS.compareTo(pSashRightClient) >= 0 );
        Assert.assertTrue( "GLWindow LOS "+pGLWinLOS+" not >= sash-right "+pSashRightClient, pGLWinLOS.compareTo(pSashRightClient) >= 0 );

        while( !quitAdapter.shouldQuit() && animator.isAnimating() && animator.getTotalFPSDuration()<duration ) {
            generalWaitAction.run();
        }

        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());
        Assert.assertEquals(null, glWindow.getExclusiveContextThread());

        canvas1.dispose();
        glWindow.destroy();
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glWindow, false));
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        final GLProfile glp = GLProfile.getGL2ES2();
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
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

        org.junit.runner.JUnitCore.main(TestBug672NewtCanvasSWTSashForm.class.getName());
    }
}
