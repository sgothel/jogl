/**
 * Copyright 2020 JogAmp Community. All rights reserved.
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

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.swt.GLCanvas;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.io.IOException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.nativewindow.swt.SWTAccessor;
import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.Point;
import com.jogamp.nativewindow.util.PointImmutable;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.WindowAdapter;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.swt.NewtCanvasSWT;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.AnimatorBase;

/**
 * Test for Bug 1421, Bug 1358, Bug 969 and Bug 672.
 * <p>
 * High-DPI scaling impact on MacOS and
 * SWT child window positioning on MacOS.
 * </p>
 * <p>
 * Testing the TabFolder and a SashForm in the 2nd tab
 * covering both SWT layout use cases on
 * both our SWT support classes SWT GLCanvas and NewtCanvasSWT.
 * </p>
 * <p>
 * Bug 1421 {@link #test01_tabFolderParent()} shows that the
 * inner child NEWT GLWindow is position wrongly.
 * It's position is shifted down abo0ut the height of the
 * parent TabFolder and right about the width of the same.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLCanvasSWTNewtCanvasSWTPosInTabs extends UITestCase {

    static int duration = 250;

    Display display = null;
    Shell shell = null;
    Composite composite = null;
    CTabFolder tabFolder = null;
    CTabItem tabItem1 = null;
    CTabItem tabItem2 = null;
    SashForm sash = null;

    static PointImmutable wpos = null;
    static DimensionImmutable wsize = new Dimension(640, 480), rwsize = null;

    @BeforeClass
    public static void startup() {
        GLProfile.initSingleton();
    }

    @After
    public void release() {
        try {
            if( null != display ) {
                display.syncExec(new Runnable() {
                   public void run() {
                    if( null != sash ) {
                        sash.dispose();
                    }
                    if( null != tabFolder ) {
                        tabFolder.dispose();
                    }
                    if( null != composite ) {
                        composite.dispose();
                    }
                    if( null != shell ) {
                        shell.dispose();
                    }
                   }});
            }
            SWTAccessor.invoke(true, new Runnable() {
               public void run() {
                if( null != display ) {
                    display.dispose();
                }
               }});
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        display = null;
        shell = null;
        composite = null;
        tabFolder = null;
        tabItem1 = null;
        tabItem2 = null;
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
    final WaitAction waitAction = new WaitAction(AWTRobotUtil.TIME_SLICE);
    final WaitAction generalWaitAction = new WaitAction(10);

    protected void runTestInLayout(final boolean focusOnTab1, final boolean useNewtCanvasSWT, final boolean addComposite, final GLCapabilitiesImmutable caps)
            throws InterruptedException
    {
        display = new Display();
        Assert.assertNotNull( display );

        shell = new Shell( display );
        shell.setText( getSimpleTestName(".") );
        shell.setLayout(new FillLayout());
        shell.setSize( wsize.getWidth(), wsize.getHeight() );
        if( null != wpos ) {
            shell.setLocation(wpos.getX(), wpos.getY());
        }

        composite = new Composite(shell, SWT.NONE);
        composite.setLayout(new FillLayout());
        tabFolder = new CTabFolder(composite, SWT.TOP);
        tabFolder.setBorderVisible(true);
        tabFolder.setLayoutData(new FillLayout());
        tabItem1 = new CTabItem(tabFolder, SWT.NONE, 0);
        tabItem1.setText("PlainGL");
        tabItem2 = new CTabItem(tabFolder, SWT.NONE, 1);
        tabItem2.setText("SashGL");
        final Composite tab1Comp;
        if( addComposite ) {
            tab1Comp = new Composite(tabFolder, SWT.NONE);
            tab1Comp.setLayout(new FillLayout());
            tabItem1.setControl(tab1Comp);
        } else {
            tab1Comp = null;
        }

        final GLWindow glWindow1;
        final NewtCanvasSWT newtCanvasSWT1;
        final GLCanvas glCanvas1;
        final Canvas canvas1;
        final GLAutoDrawable glad1;
        if( useNewtCanvasSWT ) {
            glCanvas1 = null;
            glWindow1 = GLWindow.create(caps);
            glad1 = glWindow1;
            Assert.assertNotNull(glWindow1);
            newtCanvasSWT1 = NewtCanvasSWT.create( addComposite ? tab1Comp : tabFolder, 0, glWindow1 );
            Assert.assertNotNull( newtCanvasSWT1 );
            canvas1 = newtCanvasSWT1;
        } else {
            glWindow1 = null;
            newtCanvasSWT1 = null;
            glCanvas1 = GLCanvas.create( addComposite ? tab1Comp : tabFolder, 0, caps, null);
            glad1 = glCanvas1;
            Assert.assertNotNull(glCanvas1);
            canvas1 = glCanvas1;
        }
        Assert.assertNotNull(canvas1);
        Assert.assertNotNull(glad1);
        final GearsES2 demo1 = new GearsES2(1);
        glad1.addGLEventListener(demo1);
        if( !addComposite ) {
            tabItem1.setControl(canvas1);
        }

        sash = new SashForm(tabFolder, SWT.NONE);
        Assert.assertNotNull( sash );
        final org.eclipse.swt.widgets.Label c = new org.eclipse.swt.widgets.Label(sash, SWT.NONE);
        c.setText("Left cell");
        final Composite sashRight;
        if( addComposite ) {
            sashRight = new Composite(sash, SWT.NONE);
            sashRight.setLayout(new FillLayout());
        } else {
            sashRight = null;
        }
        tabItem2.setControl(sash);

        final Animator animator1 = new Animator();
        animator1.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator1.add(glad1);

        final GLWindow glWindow2;
        final GLCanvas glCanvas2;
        final NewtCanvasSWT newtCanvasSWT2;
        final Canvas canvas2;
        final GLAutoDrawable glad2;
        if( useNewtCanvasSWT ) {
            glWindow2 = GLWindow.create(caps);
            glad2 = glWindow2;
            glCanvas2 = null;
            Assert.assertNotNull(glWindow2);
            newtCanvasSWT2 = NewtCanvasSWT.create( addComposite ? sashRight : sash, 0, glWindow2 );
            Assert.assertNotNull( newtCanvasSWT2 );
            canvas2 = newtCanvasSWT2;
        } else {
            glCanvas2 = GLCanvas.create( addComposite ? sashRight : sash, 0, caps, null);
            glad2 = glCanvas2;
            glWindow2 = null;
            Assert.assertNotNull(glCanvas2);
            newtCanvasSWT2 = null;
            canvas2 = glCanvas2;
        }
        Assert.assertNotNull(canvas2);
        Assert.assertNotNull(glad2);
        final RedSquareES2 demo2 = new RedSquareES2(1);
        glad2.addGLEventListener(demo2);

        final Animator animator2 = new Animator();
        animator2.setModeBits(false, AnimatorBase.MODE_EXPECT_AWT_RENDERING_THREAD);
        animator2.add(glad2);

        if( focusOnTab1 ) {
            canvas1.setFocus();
            tabFolder.setSelection(0);
        } else {
            canvas2.setFocus();
            tabFolder.setSelection(1);
        }

        final QuitAdapter quitAdapter = new QuitAdapter();
        if( useNewtCanvasSWT ) {
            glWindow1.addKeyListener(quitAdapter);
            glWindow1.addWindowListener(quitAdapter);
            glWindow2.addKeyListener(quitAdapter);
            glWindow2.addWindowListener(quitAdapter);

            final WindowListener wl = new WindowAdapter() {
                public void windowResized(final WindowEvent e) {
                    final GLWindow glWindow = ( e.getSource() instanceof GLWindow ) ? (GLWindow)e.getSource() : null;
                    if( null != glWindow ) {
                        System.err.println("window resized: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                    }
                }
                public void windowMoved(final WindowEvent e) {
                    final GLWindow glWindow = ( e.getSource() instanceof GLWindow ) ? (GLWindow)e.getSource() : null;
                    if( null != glWindow ) {
                        System.err.println("window moved:   "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight());
                    }
                }
            };
            glWindow1.addWindowListener(wl);
            glWindow2.addWindowListener(wl);

            final KeyListener kl = new KeyAdapter() {
                public void keyReleased(final KeyEvent e) {
                    if( !e.isPrintableKey() || e.isAutoRepeat() ) {
                        return;
                    }
                    final GLWindow glWindow = ( e.getSource() instanceof GLWindow ) ? (GLWindow)e.getSource() : null;
                    if( null != glWindow ) {
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
                }
            };
            glWindow1.addKeyListener(kl);
            glWindow2.addKeyListener(kl);
        }

        animator1.start();
        Assert.assertTrue(animator1.isStarted());
        Assert.assertTrue(animator1.isAnimating());
        animator1.setUpdateFPSFrames(60, null);

        animator2.start();
        Assert.assertTrue(animator2.isStarted());
        Assert.assertTrue(animator2.isAnimating());
        animator2.setUpdateFPSFrames(60, null);

        shell.open(); // from here on, manipulation of SWT elements might be thread sensitive

        Assert.assertEquals(true,  GLTestUtil.waitForRealized( focusOnTab1 ? glad1 : glad2, true, waitAction));
        {
            final Canvas canvas = focusOnTab1 ? canvas1 : canvas2;
            System.err.println("Canvas pixel-units  pos/siz.0: pos "+SWTAccessor.getLocationInPixels(canvas)+", size "+SWTAccessor.getSizeInPixels(canvas));
            System.err.println("Canvas window-units pos/siz.0: pos "+canvas.getLocation()+", size "+canvas.getSize());
            System.err.println("Canvas LOS.0: "+canvas.toDisplay(0, 0));
        }
        if( useNewtCanvasSWT ) {
            final GLWindow glWindow = focusOnTab1 ? glWindow1 : glWindow2;
            final NewtCanvasSWT newtCanvasSWT = focusOnTab1 ? newtCanvasSWT1 : newtCanvasSWT2;
            Assert.assertNotNull( newtCanvasSWT.getNativeWindow() );
            System.err.println("NewtCanvasSWT LOS.0: "+newtCanvasSWT.getNativeWindow().getLocationOnScreen(null));
            System.err.println("GLWindow LOS.0: "+glWindow.getLocationOnScreen(null));
            System.err.println("GLWindow pos/siz.0: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
            System.err.println("NW chosen: "+glWindow.getDelegatedWindow().getChosenCapabilities());
            System.err.println("GL chosen: "+glWindow.getChosenCapabilities());
        } else {
            final GLCanvas glCanvas = focusOnTab1 ? glCanvas1: glCanvas2;
            System.err.println("GL chosen: "+glCanvas.getChosenGLCapabilities());
        }

        if( null != rwsize ) {
            for(int i=0; i<50; i++) { // 500 ms dispatched delay
                generalWaitAction.run();
            }
            display.syncExec( new Runnable() {
               public void run() {
                  shell.setSize( rwsize.getWidth(), rwsize.getHeight() );
               }
            });
            {
                final Canvas canvas = focusOnTab1 ? canvas1 : canvas2;
                System.err.println("Canvas pixel-units  pos/siz.1: pos "+SWTAccessor.getLocationInPixels(canvas)+", size "+SWTAccessor.getSizeInPixels(canvas));
                System.err.println("Canvas window-units pos/siz.1: pos "+canvas.getLocation()+", size "+canvas.getSize());
                System.err.println("Canvas LOS.1: "+canvas.toDisplay(0, 0));
            }
            if( useNewtCanvasSWT ) {
                final GLWindow glWindow = focusOnTab1 ? glWindow1 : glWindow2;
                final NewtCanvasSWT newtCanvasSWT = focusOnTab1 ? newtCanvasSWT1 : newtCanvasSWT2;
                System.err.println("NewtCanvasSWT LOS.1: "+newtCanvasSWT.getNativeWindow().getLocationOnScreen(null));
                System.err.println("GLWindow LOS.1: "+glWindow.getLocationOnScreen(null));
                System.err.println("window resize pos/siz.1: "+glWindow.getX()+"/"+glWindow.getY()+" "+glWindow.getSurfaceWidth()+"x"+glWindow.getSurfaceHeight()+", "+glWindow.getInsets());
            }
        }

        if( !focusOnTab1 ) {
            final PointImmutable pSashRightClient = new Point(wsize.getWidth(), 0);
            final PointImmutable pGLWinLOS;
            if( useNewtCanvasSWT ) {
                final PointImmutable pNatWinLOS = newtCanvasSWT2.getNativeWindow().getLocationOnScreen(null);
                pGLWinLOS = glWindow2.getLocationOnScreen(null);
                System.err.println("GLWindow2 LOS: "+pGLWinLOS);
                System.err.println("NewtCanvasSWT2 LOS: "+pNatWinLOS);
                Assert.assertTrue( "NewtCanvasAWT2 LOS "+pNatWinLOS+" not >= sash-right "+pSashRightClient, pNatWinLOS.compareTo(pSashRightClient) >= 0 );
            } else {
                final org.eclipse.swt.graphics.Point los = glCanvas2.toDisplay(0, 0);
                pGLWinLOS = new Point(los.x, los.y);
                System.err.println("GLCanvas2 LOS: "+pGLWinLOS);
            }
            Assert.assertTrue( "GLWindow2 LOS "+pGLWinLOS+" not >= sash-right "+pSashRightClient, pGLWinLOS.compareTo(pSashRightClient) >= 0 );
        }

        while( animator1.isAnimating() || animator2.isAnimating() ) {
            final boolean keepGoing = !quitAdapter.shouldQuit() &&
                                      ( animator1.isAnimating() || animator2.isAnimating() ) &&
                                      ( animator1.getTotalFPSDuration()<duration || animator2.getTotalFPSDuration()<duration );
            if( !keepGoing ) {
                new Thread() {
                    @Override
                    public void run() {
                        animator1.stop();
                        animator2.stop();
                    }
                }.start();
            }
            generalWaitAction.run();
        }

        Assert.assertFalse(animator1.isAnimating());
        Assert.assertFalse(animator1.isStarted());
        Assert.assertFalse(animator2.isAnimating());
        Assert.assertFalse(animator2.isStarted());

        try {
            if( useNewtCanvasSWT ) {
                newtCanvasSWT1.dispose();
                glWindow1.destroy();
                Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow1, false, null));
                newtCanvasSWT2.dispose();
                glWindow2.destroy();
                Assert.assertEquals(true,  NewtTestUtil.waitForRealized(glWindow2, false, null));
            } else {
                glCanvas1.dispose();
                glCanvas2.dispose();
            }
        }
        catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
	}

    @Test
    public void test01_GLCanvasTabPlainGLDirect() throws InterruptedException {
        if( 0 != manualTest && 1 != manualTest ) {
            return;
        }
        runTestInLayout(true /* focusOnTab1 */, false /* useNewtCanvasSWT */, false /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test02_GLCanvasTabSashGLDirect() throws InterruptedException {
        if( 0 != manualTest && 2 != manualTest ) {
            return;
        }
        runTestInLayout(false /* focusOnTab1 */, false /* useNewtCanvasSWT */, false /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test11_GLCanvasTabPlainGLWComp() throws InterruptedException {
        if( 0 != manualTest && 11 != manualTest ) {
            return;
        }
        runTestInLayout(true /* focusOnTab1 */, false /* useNewtCanvasSWT */, true /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test12_GLCanvasTabSashGLWComp() throws InterruptedException {
        if( 0 != manualTest && 12 != manualTest ) {
            return;
        }
        runTestInLayout(false /* focusOnTab1 */, false /* useNewtCanvasSWT */, true /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }

    @Test
    public void test21_NewtCanvasSWTTabPlainGLDirect() throws InterruptedException {
        if( 0 != manualTest && 21 != manualTest ) {
            return;
        }
        runTestInLayout(true /* focusOnTab1 */, true  /* useNewtCanvasSWT */, false /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test22_NewtCanvasSWTTabSashGLDirect() throws InterruptedException {
        if( 0 != manualTest && 22 != manualTest ) {
            return;
        }
        runTestInLayout(false /* focusOnTab1 */, true /* useNewtCanvasSWT */, false /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test31_NewtCanvasSWTTabPlainGLWComp() throws InterruptedException {
        if( 0 != manualTest && 31 != manualTest ) {
            return;
        }
        runTestInLayout(true /* focusOnTab1 */, true  /* useNewtCanvasSWT */, true /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }
    @Test
    public void test32_NewtCanvasSWTTabSashGLWComp() throws InterruptedException {
        if( 0 != manualTest && 32 != manualTest ) {
            return;
        }
        runTestInLayout(false /* focusOnTab1 */, true /* useNewtCanvasSWT */, true /* addComposite */, new GLCapabilities(GLProfile.getGL2ES2()));
    }

    static int manualTest = 0;

    public static void main(final String args[]) throws IOException {
        int x=0, y=0, w=640, h=480, rw=-1, rh=-1;
        boolean usePos = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-test")) {
                i++;
                manualTest = MiscUtils.atoi(args[i], manualTest);
            } else if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atoi(args[i], duration);
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
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        if(usePos) {
            wpos = new Point(x, y);
        }
        System.out.println("manualTest: "+manualTest);
        System.out.println("durationPerTest: "+duration);
        System.err.println("position "+wpos);
        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);

        org.junit.runner.JUnitCore.main(TestGLCanvasSWTNewtCanvasSWTPosInTabs.class.getName());
    }
}
