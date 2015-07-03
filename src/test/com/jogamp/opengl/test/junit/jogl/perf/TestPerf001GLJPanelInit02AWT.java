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
package com.jogamp.opengl.test.junit.jogl.perf;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.awt.NewtCanvasAWT;
import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;

/**
 * Tests multiple JFrames each with a [GLJPanels, GLCanvas or NewtCanvasAWT]
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestPerf001GLJPanelInit02AWT extends UITestCase {
    final long INIT_TIMEOUT = 10L*1000L; // 10s

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    static enum CanvasType { NOP_T, GLCanvas_T, GLJPanel_T, NewtCanvasAWT_T };

    static class GLADComp {
        GLADComp(final GLAutoDrawable glad, final Component comp) {
            this.glad = glad;
            this.comp = comp;
        }
        final GLAutoDrawable glad;
        final Component comp;
    }

    public void test(final GLCapabilitiesImmutable caps, final boolean useGears, final boolean skipGLOrientationVerticalFlip, final int width,
                     final int height, final int frameCount, final boolean initMT,
                     final boolean useSwingDoubleBuffer, final CanvasType canvasType, final boolean useAnim, final boolean overlap) {
        final GLAnimatorControl animator;
        if( useAnim ) {
            animator = new Animator();
        } else {
            animator = null;
        }
        final int eWidth, eHeight;
        {
            final int cols = (int)Math.round(Math.sqrt(frameCount));
            final int rows = frameCount / cols;
            eWidth = width/cols-32;
            eHeight = height/rows-32;
        }
        System.err.println("Frame size: "+width+"x"+height+" -> "+frameCount+" x "+eWidth+"x"+eHeight+", overlap "+overlap);
        System.err.println("SkipGLOrientationVerticalFlip "+skipGLOrientationVerticalFlip+", useGears "+useGears+", initMT "+initMT+", useAnim "+useAnim);
        final JFrame[] frame = new JFrame[frameCount];
        final List<NewtCanvasAWT> newtCanvasAWTList = new ArrayList<NewtCanvasAWT>();

        final long[] t = new long[10];
        if( wait ) {
            UITestCase.waitForKey("Pre-Init");
        }
        System.err.println("INIT START");
        initCount.set(0);
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    t[0] = Platform.currentTimeMillis();
                    int x = 32, y = 32;
                    for(int i=0; i<frameCount; i++) {
                        frame[i] = new JFrame(i+"/"+frameCount);
                        frame[i].setLocation(x, y);
                        if(!overlap) {
                            x+=eWidth+32;
                            if(x>=width) {
                                x=32;
                                y+=eHeight+32;
                            }
                        }
                        final JPanel panel = new JPanel();
                        panel.setLayout(new BorderLayout());
                        panel.setDoubleBuffered(useSwingDoubleBuffer);
                        // panel.setBounds(0, 0, width, height);
                        final Dimension eSize = new Dimension(eWidth, eHeight);
                        final GLADComp gladComp;
                        switch(canvasType) {
                            case GLCanvas_T:
                                gladComp = createGLCanvas(caps, useGears, animator, eSize);
                                break;
                            case GLJPanel_T:
                                gladComp = createGLJPanel(initMT, useSwingDoubleBuffer, caps, useGears, skipGLOrientationVerticalFlip, animator, eSize);
                                break;
                            case NewtCanvasAWT_T:
                                gladComp = createNewtCanvasAWT(caps, useGears, animator, eSize);
                                newtCanvasAWTList.add((NewtCanvasAWT)gladComp.comp);
                                break;
                            case NOP_T:
                                gladComp = null;
                                break;
                            default: throw new InternalError("XXX");
                        }

                        if( null != gladComp ) {
                            gladComp.glad.addGLEventListener(new GLEventListener() {
                                @Override
                                public void init(final GLAutoDrawable drawable) {
                                    initCount.incrementAndGet();
                                }
                                @Override
                                public void dispose(final GLAutoDrawable drawable) {}
                                @Override
                                public void display(final GLAutoDrawable drawable) {}
                                @Override
                                public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
                            });
                            panel.add(gladComp.comp);
                        } else {
                            @SuppressWarnings("serial")
                            final JTextArea c = new JTextArea("area "+i) {
                                boolean initialized = false, added = false;
                                int reshapeWidth=0, reshapeHeight=0;
                                @Override
                                public void addNotify() {
                                    added = true;
                                    super.addNotify();
                                }
                                @SuppressWarnings("deprecation")
                                @Override
                                public void reshape(final int x, final int y, final int width, final int height) {
                                    super.reshape(x, y, width, height);
                                    reshapeWidth = width; reshapeHeight = height;
                                }
                                @Override
                                protected void paintComponent(final Graphics g) {
                                    super.paintComponent(g);
                                    if( !initialized && added && reshapeWidth > 0 && reshapeHeight > 0 && isDisplayable() ) {
                                        initialized = true;
                                        initCount.incrementAndGet();
                                    }
                                }
                            };
                            c.setEditable(false);
                            c.setSize(eSize);
                            c.setPreferredSize(eSize);
                            panel.add(c);
                        }
                        frame[i].getContentPane().add(panel);

                        // frame.validate();
                        frame[i].pack();
                    }
                    t[1] = Platform.currentTimeMillis();
                    for(int i=0; i<frameCount; i++) {
                        frame[i].setVisible(true);
                    }
                    t[2] = Platform.currentTimeMillis();
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( frameCount > initCount.get() && INIT_TIMEOUT > t1 - t0 ) {
            try {
                Thread.sleep(100);
                System.err.println("Sleep initialized: "+initCount+"/"+frameCount);
            } catch (final InterruptedException e1) {
                e1.printStackTrace();
            }
            t1 = System.currentTimeMillis();
        }
        t[3] = Platform.currentTimeMillis();
        final double panelCountF = initCount.get();
        System.err.printf("P: %d %s%s:%n\tctor\t%6d/t %6.2f/1%n\tvisible\t%6d/t %6.2f/1%n\tsum-i\t%6d/t %6.2f/1%n",
                initCount.get(),
                canvasType, initMT?" (mt)":" (01)",
                t[1]-t[0], (t[1]-t[0])/panelCountF,
                t[3]-t[1], (t[3]-t[1])/panelCountF,
                t[3]-t[0], (t[3]-t[0])/panelCountF);

        System.err.println("INIT END: "+initCount+"/"+frameCount);
        if( wait ) {
            UITestCase.waitForKey("Post-Init");
        }
        if( null != animator ) {
            animator.start();
        }
        try {
            Thread.sleep(duration);
        } catch (final InterruptedException e1) {
            e1.printStackTrace();
        }
        if( null != animator ) {
            animator.stop();
        }
        t[4] = Platform.currentTimeMillis();
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        while( !newtCanvasAWTList.isEmpty() ) {
                            newtCanvasAWTList.remove(0).destroy(); // removeNotify does not destroy GLWindow
                        }
                        for(int i=0; i<frameCount; i++) {
                            frame[i].dispose();
                        }
                    } } );
        } catch (final Exception e1) {
            e1.printStackTrace();
        }

        final long ti_net = (t[4]-t[0])-duration;
        System.err.printf("T: duration %d %d%n\ttotal-d\t%6d/t %6.2f/1%n\ttotal-i\t%6d/t %6.2f/1%n",
                duration, t[4]-t[3],
                t[4]-t[0], (t[4]-t[0])/panelCountF,
                ti_net, ti_net/panelCountF);
        System.err.println("Total: "+(t[4]-t[0]));
    }

    private GLADComp createNewtCanvasAWT(final GLCapabilitiesImmutable caps, final boolean useGears, final GLAnimatorControl anim, final Dimension size) {
        final GLWindow window = GLWindow.create(caps);
        final NewtCanvasAWT canvas = new NewtCanvasAWT(window);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        if( useGears ) {
            final GearsES2 g = new GearsES2(0);
            g.setVerbose(false);
            window.addGLEventListener(g);
        }
        if( null != anim ) {
            anim.add(window);
        }
        return new GLADComp(window, canvas);
    }
    private GLADComp createGLCanvas(final GLCapabilitiesImmutable caps, final boolean useGears, final GLAnimatorControl anim, final Dimension size) {
        final GLCanvas canvas = new GLCanvas(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        if( useGears ) {
            final GearsES2 g = new GearsES2(0);
            g.setVerbose(false);
            canvas.addGLEventListener(g);
        }
        if( null != anim ) {
            anim.add(canvas);
        }
        return new GLADComp(canvas, canvas);
    }
    private GLADComp createGLJPanel(final boolean initMT, final boolean useSwingDoubleBuffer, final GLCapabilitiesImmutable caps, final boolean useGears, final boolean skipGLOrientationVerticalFlip, final GLAnimatorControl anim, final Dimension size) {
        final GLJPanel canvas = new GLJPanel(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        canvas.setDoubleBuffered(useSwingDoubleBuffer);
        if( skipGLOrientationVerticalFlip ) { // don't fiddle w/ default ..
            canvas.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        }
        if( useGears ) {
            final GearsES2 g = new GearsES2(0);
            g.setVerbose(false);
            g.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            canvas.addGLEventListener(g);
        }
        if( null != anim ) {
            anim.add(canvas);
        }
        if( initMT ) {
            canvas.initializeBackend(true /* offthread */);
        }
        return new GLADComp(canvas, canvas);
    }

    static GLCapabilitiesImmutable caps = null;

    //
    // NOP
    //

    @Test
    public void test00NopNoGLDefGrid() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.NOP_T, false /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test01NopGLCanvasDefGrid() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLCanvas_T, false /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test02NopGLJPanelDefGridSingleAutoFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test03NopGLJPanelDefGridSingleManualFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, true /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test04NopGLJPanelDefGridMTManualFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, true /*skipGLOrientationVerticalFlip*/, width , height, frameCount, true  /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test05NopNewtCanvasAWTDefGrid() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.NewtCanvasAWT_T, false /*useAnim*/, false /* overlap */);
    }

    //
    // Gears
    //

    @Test
    public void test11GearsGLCanvasDefGrid() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), true /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLCanvas_T, true /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test12GearsGLJPanelDefGridSingleAutoFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), true /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, true /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test13GearsGLJPanelDefGridSingleManualFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), true /*useGears*/, true /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, true /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test14GearsGLJPanelDefGridMTManualFlip() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), true /*useGears*/, true /*skipGLOrientationVerticalFlip*/, width , height, frameCount, true  /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, true /*useAnim*/, false /* overlap */);
    }

    @Test
    public void test15GearsNewtCanvasAWTDefGrid() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), true /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.NewtCanvasAWT_T, true /*useAnim*/, false /* overlap */);
    }

    //
    // Overlap + NOP
    //


    @Test
    public void test20NopNoGLDefOverlap() throws InterruptedException, InvocationTargetException {
        test(null, false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.NOP_T, false /*useAnim*/, true /* overlap */);
    }

    @Test
    public void test21NopGLCanvasDefOverlap() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLCanvas_T, false /*useAnim*/, true /* overlap */);
    }

    @Test
    public void test22NopGLJPanelDefOverlapSingle() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, true /* overlap */);
    }

    @Test
    public void test23NopGLJPanelDefOverlapMT() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, true  /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, true /* overlap */);
    }

    @Test
    public void test25NopNewtCanvasAWTDefOverlap() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.NewtCanvasAWT_T, false /*useAnim*/, true /* overlap */);
    }

    // @Test
    public void testXXNopGLJPanelDefOverlapSingle() throws InterruptedException, InvocationTargetException {
        test(new GLCapabilities(null), false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, true /* overlap */);
    }

    // @Test
    public void testXXNopGLJPanelBitmapGridSingle() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        caps.setBitmap(true);
        test(caps, false /*useGears*/, false /*skipGLOrientationVerticalFlip*/, width , height, frameCount, false /* initMT */,
             false /*useSwingDoubleBuffer*/, CanvasType.GLJPanel_T, false /*useAnim*/, false);
    }

    static long duration = 0; // ms
    static boolean wait = false;
    static int width = 800, height = 600, frameCount = 25;

    AtomicInteger initCount = new AtomicInteger(0);

    public static void main(final String[] args) {
        boolean manual=false;
        boolean waitMain = false;
        CanvasType canvasType = CanvasType.GLJPanel_T;
        boolean initMT = false, useSwingDoubleBuffer=false;
        boolean useGears = false, skipGLOrientationVerticalFlip=false, useAnim = false;
        boolean overlap = false;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                width = MiscUtils.atoi(args[++i], width);
            } else if(args[i].equals("-height")) {
                height = MiscUtils.atoi(args[++i], height);
            } else if(args[i].equals("-count")) {
                frameCount = MiscUtils.atoi(args[++i], frameCount);
            } else if(args[i].equals("-initMT")) {
                initMT = true;
                manual = true;
            } else if(args[i].equals("-type")) {
                i++;
                canvasType = CanvasType.valueOf(args[i]);
                manual = true;
            } else if(args[i].equals("-swingDoubleBuffer")) {
                useSwingDoubleBuffer = true;
            } else if(args[i].equals("-gears")) {
                useGears = true;
            } else if(args[i].equals("-anim")) {
                useAnim = true;
            } else if(args[i].equals("-userVertFlip")) {
                skipGLOrientationVerticalFlip = true;
            } else if(args[i].equals("-overlap")) {
                overlap = true;
            } else if(args[i].equals("-wait")) {
                wait = true;
                manual = true;
            } else if(args[i].equals("-waitMain")) {
                waitMain = true;
                manual = true;
            } else if(args[i].equals("-manual")) {
                manual = true;
            }
        }
        if( waitMain ) {
            UITestCase.waitForKey("Main-Start");
        }
        if( manual ) {
            GLProfile.initSingleton();
            final TestPerf001GLJPanelInit02AWT demo = new TestPerf001GLJPanelInit02AWT();
            demo.test(null, useGears, skipGLOrientationVerticalFlip, width, height, frameCount,
                      initMT, useSwingDoubleBuffer, canvasType, useAnim, overlap);
        } else {
            org.junit.runner.JUnitCore.main(TestPerf001GLJPanelInit02AWT.class.getName());
        }
    }

}
