/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.awt;

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.awt.GLCanvas;
import javax.swing.BoundedRangeModel;
import javax.swing.BoxLayout;
import javax.swing.JFrame;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ScrollPaneConstants;

import com.jogamp.common.util.awt.AWTEDTExecutor;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Bug 816: OSX CALayer Positioning Bug.
 * <p>
 * Diff. OSX CALayer positioning w/ java6, [7uxx..7u40[, and >= 7u40
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug816OSXCALayerPos01AWT extends UITestCase {
    public enum FrameLayout { None, Flow, DoubleBorderCenterSurrounded, Box, Split };

    static long duration = 1600; // ms
    static final int width = 640, height = 480;

    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static int swapInterval = 1;
    static java.awt.Dimension rwsize = new Dimension(800, 600);

    static void setComponentSize(final Frame frame, final Component comp1, final java.awt.Dimension new_sz1, final Component comp2, final java.awt.Dimension new_sz2) {
        try {
            AWTEDTExecutor.singleton.invoke(true /* wait */, new Runnable() {
                public void run() {
                    comp1.setMinimumSize(new_sz1);
                    comp1.setPreferredSize(new_sz1);
                    comp1.setSize(new_sz1);
                    if( null != comp2 ) {
                        comp2.setMinimumSize(new_sz2);
                        comp2.setPreferredSize(new_sz2);
                        comp2.setSize(new_sz2);
                    }
                    if( null != frame ) {
                        frame.pack();
                    }
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }
    static void setFrameSize(final Frame frame, final boolean frameLayout, final java.awt.Dimension new_sz) {
        try {
            AWTEDTExecutor.singleton.invoke(true /* wait */, new Runnable() {
                public void run() {
                    frame.setSize(new_sz);
                    if( frameLayout ) {
                        frame.validate();
                    }
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    protected void runTestGL(final GLCapabilities caps, final FrameLayout frameLayout, final boolean twoCanvas, final boolean resizeByComp) throws InterruptedException, InvocationTargetException {
        final JFrame frame = new JFrame("Bug816: "+this.getTestMethodName());
        Assert.assertNotNull(frame);
        final Container framePane = frame.getContentPane();

        final GLCanvas glCanvas1 = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas1);
        final GLCanvas glCanvas2;
        if( twoCanvas ) {
            glCanvas2 = new GLCanvas(caps);
            Assert.assertNotNull(glCanvas2);
        } else {
            glCanvas2 = null;
        }

        final Dimension glcDim = new Dimension(width/2, height);
        final Dimension frameDim = new Dimension(twoCanvas ? width + 64: width/2 + 64, height + 64);

        setComponentSize(null, glCanvas1, glcDim, glCanvas2, glcDim);

        switch( frameLayout) {
            case None: {
                    framePane.add(glCanvas1);
                }
                break;
            case Flow: {
                    final Container c = new Container();
                    c.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
                    c.add(glCanvas1);
                    if( twoCanvas ) {
                        c.add(glCanvas2);
                    }
                    framePane.add(c);
                }
                break;
            case DoubleBorderCenterSurrounded: {
                    final Container c = new Container();
                    c.setLayout(new BorderLayout());
                    c.add(new Button("north"), BorderLayout.NORTH);
                    c.add(new Button("south"), BorderLayout.SOUTH);
                    c.add(new Button("east"), BorderLayout.EAST);
                    c.add(new Button("west"), BorderLayout.WEST);
                    if( twoCanvas ) {
                        final Container c2 = new Container();
                        c2.setLayout(new GridLayout(1, 2));
                        c2.add(glCanvas1);
                        c2.add(glCanvas2);
                        c.add(c2, BorderLayout.CENTER);
                    } else {
                        c.add(glCanvas1, BorderLayout.CENTER);
                    }
                    framePane.setLayout(new BorderLayout());
                    framePane.add(new Button("NORTH"), BorderLayout.NORTH);
                    framePane.add(new Button("SOUTH"), BorderLayout.SOUTH);
                    framePane.add(new Button("EAST"), BorderLayout.EAST);
                    framePane.add(new Button("WEST"), BorderLayout.WEST);
                    framePane.add(c, BorderLayout.CENTER);
                }
                break;
            case Box: {
                    final Container c = new Container();
                    c.setLayout(new BoxLayout(c, BoxLayout.X_AXIS));
                    c.add(glCanvas1);
                    if( twoCanvas ) {
                        c.add(glCanvas2);
                    }
                    framePane.add(c);
                }
                break;
            case Split: {
                    final Dimension sbDim = new Dimension(16, 16);
                    final JScrollPane vsp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
                    {
                        final JScrollBar vsb = vsp.getVerticalScrollBar();
                        vsb.setPreferredSize(sbDim);
                        final BoundedRangeModel model = vsb.getModel();
                        model.setMinimum(0);
                        model.setMaximum(100);
                        model.setValue(50);
                        model.setExtent(1);
                        vsb.setEnabled(true);
                    }
                    final JScrollPane hsp = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS);
                    {
                        final JScrollBar hsb = hsp.getHorizontalScrollBar();
                        hsb.setPreferredSize(sbDim);
                        final BoundedRangeModel model = hsb.getModel();
                        model.setMinimum(0);
                        model.setMaximum(100);
                        model.setValue(50);
                        model.setExtent(1);
                        hsb.setEnabled(true);
                    }
                    final JSplitPane horizontalSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, true,
                            twoCanvas ? glCanvas2 : vsp, glCanvas1 );
                    horizontalSplitPane.setResizeWeight(0.5);
                    final JSplitPane verticalSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                        true, horizontalSplitPane, hsp);
                    verticalSplitPane.setResizeWeight(0.5);
                    framePane.add(verticalSplitPane);
                }
                break;
        }
        final GearsES2 demo1 = new GearsES2(swapInterval);
        glCanvas1.addGLEventListener(demo1);
        if( twoCanvas ) {
            final RedSquareES2 demo2 = new RedSquareES2(swapInterval);
            glCanvas2.addGLEventListener(demo2);
        }

        final Animator animator = new Animator();
        animator.add(glCanvas1);
        if( twoCanvas ) {
            animator.add(glCanvas2);
        }
        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glCanvas1).addTo(frame);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
               if( resizeByComp ) {
                   frame.pack();
                } else {
                   setFrameSize(frame, true, frameDim);
                }
                frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas1, true));
        if( twoCanvas ) {
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas2, true));
        }

        animator.start();
        Assert.assertTrue(animator.isStarted());
        Assert.assertTrue(animator.isAnimating());

        System.err.println("canvas1 pos/siz: "+glCanvas1.getX()+"/"+glCanvas1.getY()+" "+glCanvas1.getSurfaceWidth()+"x"+glCanvas1.getSurfaceHeight());
        if( twoCanvas ) {
            System.err.println("canvas2 pos/siz: "+glCanvas2.getX()+"/"+glCanvas2.getY()+" "+glCanvas2.getSurfaceWidth()+"x"+glCanvas2.getSurfaceHeight());
        }

        Thread.sleep(Math.max(1000, duration/2));
        if( null != rwsize ) {
            final Dimension compRSizeHalf = new Dimension(rwsize.width/2, rwsize.height);
            final Dimension frameRSizeHalf = new Dimension(twoCanvas ? rwsize.width + 64: rwsize.width/2 + 64, rwsize.height + 64);
            if( resizeByComp ) {
               setComponentSize(frame, glCanvas1, compRSizeHalf, glCanvas2, compRSizeHalf);
            } else {
               setFrameSize(frame, true, frameRSizeHalf);
            }
            System.err.println("resize canvas1 pos/siz: "+glCanvas1.getX()+"/"+glCanvas1.getY()+" "+glCanvas1.getSurfaceWidth()+"x"+glCanvas1.getSurfaceHeight());
            if( twoCanvas ) {
                System.err.println("resize canvas2 pos/siz: "+glCanvas2.getX()+"/"+glCanvas2.getY()+" "+glCanvas2.getSurfaceWidth()+"x"+glCanvas2.getSurfaceHeight());
            }
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas1);
        if( twoCanvas ) {
            Assert.assertNotNull(glCanvas2);
        } else {
            Assert.assertNull(glCanvas2);
        }

        Assert.assertNotNull(animator);
        animator.stop();
        Assert.assertFalse(animator.isAnimating());
        Assert.assertFalse(animator.isStarted());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glCanvas1);
                if( twoCanvas ) {
                    frame.remove(glCanvas2);
                }
                frame.dispose();
            }});
    }

    static GLProfile getGLP() {
        return GLProfile.getMaxProgrammableCore(true);
    }

    @Test
    public void test00_Compo_None_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 0 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.None, false /* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test01_Compo_Flow_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 1 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Flow, false /* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test02_Compo_DblBrd_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 2 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.DoubleBorderCenterSurrounded, false /* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test03_Compo_Box_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 3 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Box, false /* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test04_Compo_Split_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 4 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Split, false /* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test05_Compo_Flow_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 5 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Flow, true/* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test06_Compo_DblBrd_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 6 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.DoubleBorderCenterSurrounded, true/* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test07_Compo_Box_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 7 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Box, true/* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test08_Compo_Split_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 8 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Split, true/* twoCanvas */, true /* resizeByComp */);
    }

    @Test
    public void test10_Frame_None_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 10 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.None, false /* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test11_Frame_Flow_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 11 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Flow, false /* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test12_Frame_DblBrd_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 12 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.DoubleBorderCenterSurrounded, false /* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test13_Frame_Box_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 13 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Box, false /* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test14_Frame_Split_One() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 14) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Split, false /* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test15_Frame_Flow_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 15 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Flow, true/* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test16_Frame_DblBrd_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 16 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.DoubleBorderCenterSurrounded, true/* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test17_Frame_Box_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 17 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Box, true/* twoCanvas */, false /* resizeByComp */);
    }

    @Test
    public void test18_Frame_Split_Two() throws InterruptedException, InvocationTargetException {
        if( testNum != -1 && testNum != 18 ) { return ; }
        final GLCapabilities caps = new GLCapabilities(getGLP());
        runTestGL(caps, FrameLayout.Split, true/* twoCanvas */, false /* resizeByComp */);
    }

    static int testNum = -1;

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-test")) {
                i++;
                testNum = MiscUtils.atoi(args[i], 0);
            } else if(args[i].equals("-noresize")) {
                rwsize = null;
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            }
        }

        System.err.println("resize "+rwsize);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);

        org.junit.runner.JUnitCore.main(TestBug816OSXCALayerPos01AWT.class.getName());
    }
}
