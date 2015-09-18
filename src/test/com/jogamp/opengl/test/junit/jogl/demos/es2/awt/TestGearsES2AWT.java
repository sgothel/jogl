/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.demos.es2.awt;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2AWT extends UITestCase {
    public enum FrameLayout { None, TextOnBottom, BorderCenterSurrounded, DoubleBorderCenterSurrounded };
    public enum ResizeBy { Component, Frame };

    static long duration = 500; // ms
    static int width = 640, height = 480;
    static int xpos = 10, ypos = 10;
    static FrameLayout frameLayout = FrameLayout.None;
    static ResizeBy resizeBy = ResizeBy.Component;
    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean manualTest = false;
    static boolean shallUseOffscreenFBOLayer = false;
    static boolean shallUseOffscreenPBufferLayer = false;
    static boolean useMSAA = false;
    static boolean useStencil = false;
    static boolean shutdownRemoveGLCanvas = true;
    static boolean shutdownDisposeFrame = true;
    static boolean shutdownSystemExit = false;
    static int swapInterval = 1;
    static boolean exclusiveContext = false;
    static boolean useAnimator = true;
    static Thread awtEDT;
    static java.awt.Dimension rwsize = null;

    @BeforeClass
    public static void initClass() {
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    awtEDT = Thread.currentThread();
                } } );
        } catch (final Exception e) {
            e.printStackTrace();
            Assert.assertNull(e);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    static void setComponentSize(final Frame frame, final Component comp, final java.awt.Dimension new_sz) {
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    comp.setMinimumSize(new_sz);
                    comp.setPreferredSize(new_sz);
                    comp.setSize(new_sz);
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
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
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

    static void setSize(final ResizeBy resizeBy, final Frame frame, final boolean frameLayout, final Component comp, final java.awt.Dimension new_sz) {
        switch( resizeBy ) {
            case Component:
                setComponentSize(frameLayout ? frame : null, comp, new_sz);
                break;
            case Frame:
                setFrameSize(frame, frameLayout, new_sz);
                break;
        }
    }

    private void setTitle(final Frame frame, final GLCanvas glc, final GLCapabilitiesImmutable caps) {
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        final java.awt.Rectangle b = glc.getBounds();
        final float[] minSurfacePixelScale = glc.getMinimumSurfaceScale(new float[2]);
        final float[] maxSurfacePixelScale = glc.getMaximumSurfaceScale(new float[2]);
        final float[] reqSurfacePixelScale = glc.getRequestedSurfaceScale(new float[2]);
        final float[] hasSurfacePixelScale = glc.getCurrentSurfaceScale(new float[2]);
        frame.setTitle("GLCanvas["+capsA+"], swapI "+swapInterval+", win: ["+b.x+"/"+b.y+" "+b.width+"x"+b.height+"], pix: "+glc.getSurfaceWidth()+"x"+glc.getSurfaceHeight()+
                ", scale[min "+minSurfacePixelScale[0]+"x"+minSurfacePixelScale[1]+", max "+
                maxSurfacePixelScale[0]+"x"+maxSurfacePixelScale[1]+", req "+
                reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" -> has "+
                hasSurfacePixelScale[0]+"x"+hasSurfacePixelScale[1]+"]");
    }

    protected void runTestGL(final GLCapabilities caps, final ResizeBy resizeBy, final FrameLayout frameLayout) throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("GearsES2 AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        setSize(resizeBy, frame, false, glCanvas, new Dimension(width, height));
        glCanvas.setSurfaceScale(reqSurfacePixelScale);
        final float[] valReqSurfacePixelScale = glCanvas.getRequestedSurfaceScale(new float[2]);
        frame.setLocation(xpos, ypos);

        switch( frameLayout) {
            case None:
                frame.add(glCanvas);
                break;
            case TextOnBottom:
                final TextArea ta = new TextArea(2, 20);
                ta.append("0123456789");
                ta.append(Platform.getNewline());
                ta.append("Some Text");
                ta.append(Platform.getNewline());
                frame.setLayout(new BorderLayout());
                frame.add(ta, BorderLayout.SOUTH);
                frame.add(glCanvas, BorderLayout.CENTER);
                break;
            case BorderCenterSurrounded:
                frame.setLayout(new BorderLayout());
                frame.add(new Button("NORTH"), BorderLayout.NORTH);
                frame.add(new Button("SOUTH"), BorderLayout.SOUTH);
                frame.add(new Button("EAST"), BorderLayout.EAST);
                frame.add(new Button("WEST"), BorderLayout.WEST);
                frame.add(glCanvas, BorderLayout.CENTER);
                break;
            case DoubleBorderCenterSurrounded:
                final Container c = new Container();
                c.setLayout(new BorderLayout());
                c.add(new Button("north"), BorderLayout.NORTH);
                c.add(new Button("south"), BorderLayout.SOUTH);
                c.add(new Button("east"), BorderLayout.EAST);
                c.add(new Button("west"), BorderLayout.WEST);
                c.add(glCanvas, BorderLayout.CENTER);

                frame.setLayout(new BorderLayout());
                frame.add(new Button("NORTH"), BorderLayout.NORTH);
                frame.add(new Button("SOUTH"), BorderLayout.SOUTH);
                frame.add(new Button("EAST"), BorderLayout.EAST);
                frame.add(new Button("WEST"), BorderLayout.WEST);
                frame.add(c, BorderLayout.CENTER);
                break;
        }
        setTitle(frame, glCanvas, caps);

        final GearsES2 demo = new GearsES2(swapInterval);
        glCanvas.addGLEventListener(demo);

        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glCanvas.addGLEventListener(snap);
        glCanvas.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) { }
            @Override
            public void dispose(final GLAutoDrawable drawable) { }
            @Override
            public void display(final GLAutoDrawable drawable) { }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                setTitle(frame, glCanvas, caps);
            }
        });

        final Animator animator = useAnimator ? new Animator(glCanvas) : null;
        if( useAnimator && exclusiveContext ) {
            animator.setExclusiveContext(awtEDT);
        }
        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glCanvas).addTo(glCanvas);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glCanvas).addTo(frame);

        final com.jogamp.newt.event.KeyListener kl = new com.jogamp.newt.event.KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='x') {
                    final float[] hadSurfacePixelScale = glCanvas.getCurrentSurfaceScale(new float[2]);
                    final float[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    glCanvas.setSurfaceScale(reqSurfacePixelScale);
                    final float[] valReqSurfacePixelScale = glCanvas.getRequestedSurfaceScale(new float[2]);
                    final float[] hasSurfacePixelScale1 = glCanvas.getCurrentSurfaceScale(new float[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                                       reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                                       valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                                       hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                    setTitle(frame, glCanvas, caps);
                }
            } };
        new AWTKeyAdapter(kl, glCanvas).addTo(glCanvas);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
               if( ResizeBy.Frame == resizeBy ) {
                   frame.validate();
               } else {
                   frame.pack();
               }
               frame.setVisible(true);
            }});
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glCanvas, true));

        final float[] hasSurfacePixelScale1 = glCanvas.getCurrentSurfaceScale(new float[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        setTitle(frame, glCanvas, caps);

        if( useAnimator ) {
            animator.start();
            Assert.assertTrue(animator.isStarted());
            Assert.assertTrue(animator.isAnimating());
            Assert.assertEquals(exclusiveContext ? awtEDT : null, glCanvas.getExclusiveContextThread());
            animator.setUpdateFPSFrames(60, System.err);
        }

        System.err.println("canvas pos/siz: "+glCanvas.getX()+"/"+glCanvas.getY()+" "+glCanvas.getSurfaceWidth()+"x"+glCanvas.getSurfaceHeight());

        snap.setMakeSnapshot();

        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay
            setSize(resizeBy, frame, true, glCanvas, rwsize);
            System.err.println("window resize pos/siz: "+glCanvas.getX()+"/"+glCanvas.getY()+" "+glCanvas.getSurfaceWidth()+"x"+glCanvas.getSurfaceHeight());
        }

        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);

        if( useAnimator ) {
            Assert.assertNotNull(animator);
            Assert.assertEquals(exclusiveContext ? awtEDT : null, glCanvas.getExclusiveContextThread());
            animator.stop();
            Assert.assertFalse(animator.isAnimating());
            Assert.assertFalse(animator.isStarted());
            Assert.assertEquals(null, glCanvas.getExclusiveContextThread());
        }

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                if(shutdownRemoveGLCanvas) {
                    frame.remove(glCanvas);
                }
                if(shutdownDisposeFrame) {
                    frame.dispose();
                }
                if(shutdownSystemExit) {
                    System.exit(0);
                }
            }});
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        final GLCapabilities caps = new GLCapabilities(glp);
        if(useMSAA) {
            caps.setNumSamples(4);
            caps.setSampleBuffers(true);
        }
        if(useStencil) {
            caps.setStencilBits(1);
        }
        if(shallUseOffscreenFBOLayer) {
            caps.setOnscreen(false);
        }
        if(shallUseOffscreenPBufferLayer) {
            caps.setPBuffer(true);
        }
        runTestGL(caps, resizeBy, frameLayout);
    }

    @Test
    public void test02_GLES2() throws InterruptedException, InvocationTargetException {
        if(manualTest) return;

        if( !GLProfile.isAvailable(GLProfile.GLES2) ) {
            System.err.println("GLES2 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GLES2);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, resizeBy, frameLayout);
    }

    @Test
    public void test03_GL3() throws InterruptedException, InvocationTargetException {
        if(manualTest) return;

        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, resizeBy, frameLayout);
    }

    @Test
    public void test99_PixelScale1_DefaultNorm() throws InterruptedException, InvocationTargetException {
        if( manualTest ) return;

        reqSurfacePixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
        reqSurfacePixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;

        final GLCapabilities caps = new GLCapabilities(GLProfile.getGL2ES2());
        runTestGL(caps, resizeBy, frameLayout);
    }

    public static void main(final String args[]) {
        boolean waitForKey = false;
        int rw=-1, rh=-1;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-width")) {
                i++;
                width = MiscUtils.atoi(args[i], width);
            } else if(args[i].equals("-height")) {
                i++;
                height = MiscUtils.atoi(args[i], height);
            } else if(args[i].equals("-x")) {
                i++;
                xpos = MiscUtils.atoi(args[i], xpos);
            } else if(args[i].equals("-y")) {
                i++;
                ypos = MiscUtils.atoi(args[i], ypos);
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
            } else if(args[i].equals("-pixelScale")) {
                i++;
                final float pS = MiscUtils.atof(args[i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[0] = pS;
                reqSurfacePixelScale[1] = pS;
            } else if(args[i].equals("-layout")) {
                i++;
                frameLayout = FrameLayout.valueOf(args[i]);
            } else if(args[i].equals("-resizeBy")) {
                i++;
                resizeBy = ResizeBy.valueOf(args[i]);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-exclctx")) {
                exclusiveContext = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-layeredFBO")) {
                shallUseOffscreenFBOLayer = true;
            } else if(args[i].equals("-layeredPBuffer")) {
                shallUseOffscreenPBufferLayer = true;
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-stencil")) {
                useStencil = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-shutdownKeepGLCanvas")) {
                shutdownRemoveGLCanvas = false;
            } else if(args[i].equals("-shutdownKeepFrame")) {
                shutdownDisposeFrame = false;
            } else if(args[i].equals("-shutdownKeepAll")) {
                shutdownRemoveGLCanvas = false;
                shutdownDisposeFrame = false;
            } else if(args[i].equals("-shutdownSystemExit")) {
                shutdownSystemExit = true;
            } else if(args[i].equals("-manual")) {
                manualTest = true;
            }
        }
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        System.err.println("resize "+rwsize);
        System.err.println("frameLayout "+frameLayout);
        System.err.println("resizeBy "+resizeBy);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("exclusiveContext "+exclusiveContext);
        System.err.println("useMSAA "+useMSAA);
        System.err.println("useAnimator "+useAnimator);

        System.err.println("shallUseOffscreenFBOLayer     "+shallUseOffscreenFBOLayer);
        System.err.println("shallUseOffscreenPBufferLayer "+shallUseOffscreenPBufferLayer);

        if(waitForKey) {
            final BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (final IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestGearsES2AWT.class.getName());
    }
}
