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

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.nativewindow.ScalableSurface;
import com.jogamp.nativewindow.util.Rectangle;
import com.jogamp.nativewindow.util.RectangleImmutable;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jogamp.newt.awt.NewtFactoryAWT;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.MonitorDevice;
import com.jogamp.newt.Screen;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.GLClearOnInitReshape;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.FPSAnimator;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2GLJPanelAWT extends UITestCase {
    static Dimension wsize, rwsize=null;
    static boolean forceES2 = false;
    static boolean forceES3 = false;
    static boolean forceGL3 = false;
    static boolean forceGLFFP = false;
    static int demoType = 1;
    static boolean shallUsePBuffer = false;
    static boolean shallUseBitmap = false;
    static boolean useMSAA = false;
    static int msaaNumSamples = 4;
    static int swapInterval = 0;
    static boolean useAnimator = true;
    static boolean manualTest = false;
    static boolean skipGLOrientationVerticalFlip = false;
    static int xpos = 10, ypos = 10;
    static float[] reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

    @BeforeClass
    public static void initClass() {
        if(null == wsize) {
            wsize = new Dimension(640, 480);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    static void setFrameSize(final JFrame frame, final boolean frameLayout, final java.awt.Dimension new_sz) {
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

    private void setTitle(final JFrame frame, final GLJPanel glc, final GLCapabilitiesImmutable caps) {
        final String capsA = caps.isBackgroundOpaque() ? "opaque" : "transl";
        final java.awt.Rectangle b = glc.getBounds();

        final float[] minSurfacePixelScale = glc.getMinimumSurfaceScale(new float[2]);
        final float[] maxSurfacePixelScale = glc.getMaximumSurfaceScale(new float[2]);
        final float[] reqSurfacePixelScale = glc.getRequestedSurfaceScale(new float[2]);
        final float[] hasSurfacePixelScale = glc.getCurrentSurfaceScale(new float[2]);
        frame.setTitle("GLJPanel["+capsA+"], swapI "+swapInterval+", win: ["+b.x+"/"+b.y+" "+b.width+"x"+b.height+"], pix: "+glc.getSurfaceWidth()+"x"+glc.getSurfaceHeight()+
                ", scale[min "+minSurfacePixelScale[0]+"x"+minSurfacePixelScale[1]+", max "+
                maxSurfacePixelScale[0]+"x"+maxSurfacePixelScale[1]+", req "+
                reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" -> has "+
                hasSurfacePixelScale[0]+"x"+hasSurfacePixelScale[1]+"]");
    }

    protected GLEventListener createDemo(final GLCapabilities caps) {
        final GLEventListener demo;
        if( 1 == demoType ) {
            if( caps.isBitmap() || caps.getGLProfile().isGL2() ) {
                final Gears gears = new Gears(swapInterval);
                gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                demo = gears;
            } else {
                final GearsES2 gears = new GearsES2(swapInterval);
                gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                demo = gears;
            }
        } else if( 0 == demoType ) {
            demo = new GLClearOnInitReshape();
        } else {
            demo = null;
        }
        return demo;
    }

    protected GLJPanel newGLJPanel(final JFrame frame, final GLCapabilities caps, final FPSAnimator animator, final SnapshotGLEventListener snap)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLJPanel glJPanel = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel);
        glJPanel.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        glJPanel.setMinimumSize(wsize);
        glJPanel.setPreferredSize(wsize);
        glJPanel.setSize(wsize);
        glJPanel.setSurfaceScale(reqSurfacePixelScale);
        {
            final GLEventListener demo = createDemo(caps);
            if( null != demo ) {
                glJPanel.addGLEventListener(demo);
            }
        }
        if( null != snap ) {
            glJPanel.addGLEventListener(snap);
        }
        glJPanel.addGLEventListener(new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) { }
            @Override
            public void dispose(final GLAutoDrawable drawable) { }
            @Override
            public void display(final GLAutoDrawable drawable) { }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {
                setTitle(frame, glJPanel, caps);
            }

        });
        setTitle(frame, glJPanel, caps);

        frame.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(final ComponentEvent e) {
                setTitle(frame, glJPanel, caps);
            }

            @Override
            public void componentMoved(final ComponentEvent e) {
                setTitle(frame, glJPanel, caps);
            }

            @Override
            public void componentShown(final ComponentEvent e) { }

            @Override
            public void componentHidden(final ComponentEvent e) { }
        });

        if( SwingUtilities.isEventDispatchThread() ) {
            frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
            frame.getContentPane().validate();
            frame.pack();
            frame.setVisible(true);
        } else {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
                        frame.getContentPane().validate();
                        frame.pack();
                        frame.setVisible(true);
                    } } ) ;
            Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
            Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glJPanel, true));

            final float[] minSurfacePixelScale = glJPanel.getMinimumSurfaceScale(new float[2]);
            final float[] maxSurfacePixelScale = glJPanel.getMaximumSurfaceScale(new float[2]);
            final float[] valReqSurfacePixelScale = glJPanel.getRequestedSurfaceScale(new float[2]);
            final float[] hasSurfacePixelScale = glJPanel.getCurrentSurfaceScale(new float[2]);
            System.err.println("HiDPI PixelScale: min "+
                               minSurfacePixelScale[0]+"x"+minSurfacePixelScale[1]+", max "+
                               maxSurfacePixelScale[0]+"x"+maxSurfacePixelScale[1]+", req "+
                               reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" -> val "+
                               valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" -> has "+
                               hasSurfacePixelScale[0]+"x"+hasSurfacePixelScale[1]);
            setTitle(frame, glJPanel, caps);
        }

        if( null != animator ) {
            animator.add(glJPanel);
            animator.setUpdateFPSFrames(60, System.err);
        }
        return glJPanel;
    }

    protected void destroy(final JFrame frame, final GLJPanel glJPanel) {
        try {
            if( SwingUtilities.isEventDispatchThread() ) {
                if( null != frame ) {
                    frame.setVisible(false);
                    if( null != glJPanel ) {
                        frame.getContentPane().remove(glJPanel);
                    }
                    frame.remove(glJPanel);
                }
                if( null != glJPanel ) {
                    glJPanel.destroy();
                }
                if( null != frame ) {
                    frame.dispose();
                }
            } else {
                SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            if( null != frame ) {
                                frame.setVisible(false);
                                if( null != glJPanel ) {
                                    frame.getContentPane().remove(glJPanel);
                                }
                                frame.remove(glJPanel);
                            }
                            if( null != glJPanel ) {
                                glJPanel.destroy();
                            }
                            if( null != frame ) {
                                frame.dispose();
                            }
                        } } );
            }
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }

    protected void runTestGL(final GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final JFrame frame = new JFrame("Swing GLJPanel");
        frame.setLocation(xpos, ypos);
        Assert.assertNotNull(frame);

        final FPSAnimator animator = useAnimator ? new FPSAnimator(60) : null;
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        final GLJPanel glJPanel = newGLJPanel(frame, caps, animator, snap);
        if( null != animator ) {
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }
        final Screen screen = NewtFactoryAWT.createScreen(glJPanel, true);
        screen.addReference(); // initial native creation - keep alive!
        System.err.println("GetPixelScale: AWT -> Screen: "+screen);

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel).addTo(glJPanel);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glJPanel).addTo(frame);

        final JFrame[] frame2 = { null };
        final GLJPanel[] glJPanel2 = { null };

        final com.jogamp.newt.event.KeyListener kl = new com.jogamp.newt.event.KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if( e.getKeySymbol() == KeyEvent.VK_P ) {
                    System.err.println();
                    {
                        // Just for manual validation!
                        final java.awt.Point los = glJPanel.getLocationOnScreen();
                        final RectangleImmutable r = new Rectangle(los.x, los.y, glJPanel.getWidth(), glJPanel.getHeight());
                        System.err.printf("GetPixelScale: Panel Bounds: %s window-units%n", r.toString());
                        System.err.printf("GetPixelScale: Panel Resolution: %d x %d pixel-units%n", glJPanel.getSurfaceWidth(), glJPanel.getSurfaceHeight());
                    }
                    final MonitorDevice monitor = NewtFactoryAWT.getMonitorDevice(screen, glJPanel);
                    System.err.printf("GetPixelScale: %s%n", monitor.toString());
                    final float[] pixelPerMM;
                    final boolean cached;
                    if( e.isShiftDown() ) {
                        // SHIFT: query current mode!
                        pixelPerMM = monitor.getPixelsPerMM(monitor.queryCurrentMode(), new float[2]);
                        cached = false;
                    } else {
                        // Default: Use cached mode!
                        pixelPerMM = monitor.getPixelsPerMM(new float[2]);
                        cached = true;
                    }
                    System.err.println("  pixel/mm ["+pixelPerMM[0]+", "+pixelPerMM[1]+"], cached-mode "+cached);
                    System.err.println("  pixel/in ["+pixelPerMM[0]*25.4f+", "+pixelPerMM[1]*25.4f+"], cached-mode "+cached);
                    System.err.println();
                } else if(e.getKeyChar()=='x') {
                    final float[] hadSurfacePixelScale = glJPanel.getCurrentSurfaceScale(new float[2]);
                    final float[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new float[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new float[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    glJPanel.setSurfaceScale(reqSurfacePixelScale);
                    final float[] valReqSurfacePixelScale = glJPanel.getRequestedSurfaceScale(new float[2]);
                    final float[] hasSurfacePixelScale1 = glJPanel.getCurrentSurfaceScale(new float[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                                       reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                                       valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                                       hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                    setTitle(frame, glJPanel, caps);
                } else if(e.getKeyChar()=='m') {
                    final GLCapabilitiesImmutable capsPre = glJPanel.getChosenGLCapabilities();
                    final GLCapabilities capsNew = new GLCapabilities(capsPre.getGLProfile());
                    capsNew.copyFrom(capsPre);
                    final boolean msaa;
                    if( capsPre.getSampleBuffers() ) {
                        capsNew.setSampleBuffers(false);
                        msaa = false;
                    } else {
                        capsNew.setSampleBuffers(true);
                        capsNew.setNumSamples(4);
                        msaa = true;
                    }
                    System.err.println("[set MSAA "+msaa+" Caps had]: "+capsPre);
                    System.err.println("[set MSAA "+msaa+" Caps new]: "+capsNew);
                    System.err.println("XXX-A1: "+animator.toString());
                    glJPanel.setRequestedGLCapabilities(capsNew);
                    System.err.println("XXX-A2: "+animator.toString());
                    System.err.println("XXX: "+glJPanel.toString());
                } else if(e.getKeyChar()=='n') {
                    System.err.println("XXX: frame2: "+frame2[0]);
                    if( null != frame2[0] ) {
                        System.err.println("XXX: frame2.isShowing: "+frame2[0].isShowing());
                    }
                    System.err.println("XXX: glJPanel2: "+glJPanel2[0]);
                    if( null != frame2[0] && frame2[0].isShowing() ) {
                        destroy(frame2[0], glJPanel2[0]);
                        frame2[0] = null;
                        glJPanel2[0] = null;
                    } else {
                        frame2[0] = new JFrame("GLJPanel2");
                        frame2[0].setLocation(frame.getX()+frame.getWidth()+64, frame.getY());
                        final FPSAnimator animator2 = useAnimator ? new FPSAnimator(60) : null;
                        if( null != animator2 ) {
                            animator2.start();
                        }
                        final SnapshotGLEventListener snap2 = new SnapshotGLEventListener();
                        try {
                            glJPanel2[0] = newGLJPanel(frame2[0], caps, animator2, snap2);
                        } catch (final Exception e2) {
                            e2.printStackTrace();
                            destroy(frame2[0], glJPanel2[0]);
                            frame2[0] = null;
                            glJPanel2[0] = null;
                        }
                    }
                }
            } };
        new AWTKeyAdapter(kl, glJPanel).addTo(glJPanel);

        snap.setMakeSnapshot();

        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay
            setFrameSize(frame, true, rwsize);
            System.err.println("window resize pos/siz: "+glJPanel.getX()+"/"+glJPanel.getY()+" "+glJPanel.getSurfaceWidth()+"x"+glJPanel.getSurfaceHeight());
        }

        snap.setMakeSnapshot();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        boolean triggerSnap = false;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
            snap.getDisplayCount();
            if( !triggerSnap && snap.getDisplayCount() > 1 ) {
                // Snapshot only after one frame has been rendered to suite FBO MSAA!
                snap.setMakeSnapshot();
                triggerSnap = true;
            }
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glJPanel);

        if( useAnimator ) {
            Assert.assertNotNull(animator);
            animator.stop();
            Assert.assertEquals(false, animator.isAnimating());
        } else {
            Assert.assertNull(animator);
        }

        screen.removeReference(); // final native destroy
        destroy(frame, glJPanel);
        if( null != frame2[0] ) {
            destroy(frame2[0], glJPanel2[0]);
        }
    }

    @Test
    public void test01_DefaultNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES3) {
            glp = GLProfile.get(GLProfile.GLES3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else if(forceGLFFP) {
            glp = GLProfile.getMaxFixedFunc(true);
        } else {
            glp = GLProfile.getDefault();
        }
        final GLCapabilities caps = new GLCapabilities( glp );
        if(useMSAA) {
            caps.setNumSamples(msaaNumSamples);
            caps.setSampleBuffers(true);
        }
        if(shallUsePBuffer) {
            caps.setPBuffer(true);
        }
        if(shallUseBitmap) {
            caps.setBitmap(true);
        }
        runTestGL(caps);
    }

    @Test
    public void test02_DefaultMsaa()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setNumSamples(4);
        caps.setSampleBuffers(true);
        runTestGL(caps);
    }

    @Test
    public void test03_PbufferNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setPBuffer(true);
        runTestGL(caps);
    }

    @Test
    public void test04_PbufferMsaa()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setNumSamples(4);
        caps.setSampleBuffers(true);
        caps.setPBuffer(true);
        runTestGL(caps);
    }

    @Test
    public void test05_BitmapNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setBitmap(true);
        runTestGL(caps);
    }

    @Test
    public void test06_BitmapMsaa()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setNumSamples(4);
        caps.setSampleBuffers(true);
        caps.setBitmap(true);
        runTestGL(caps);
    }

    @Test
    public void test20_GLES2()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }

        if( !GLProfile.isAvailable(GLProfile.GLES2) ) {
            System.err.println("GLES2 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GLES2);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    @Test
    public void test30_GLES3()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }

        if( !GLProfile.isAvailable(GLProfile.GLES3) ) {
            System.err.println("GLES3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GLES3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    @Test
    public void test40_GL3()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }

        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps);
    }

    @Test
    public void test99_PixelScale1_DefaultNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( manualTest ) {
            return;
        }
        reqSurfacePixelScale[0] = ScalableSurface.IDENTITY_PIXELSCALE;
        reqSurfacePixelScale[1] = ScalableSurface.IDENTITY_PIXELSCALE;

        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        int w=640, h=480, rw=-1, rh=-1;

        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-es3")) {
                forceES3 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-glFFP")) {
                forceGLFFP = true;
            } else if(args[i].equals("-width")) {
                i++;
                w = MiscUtils.atoi(args[i], w);
            } else if(args[i].equals("-height")) {
                i++;
                h = MiscUtils.atoi(args[i], h);
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
            } else if(args[i].equals("-userVFlip")) {
                skipGLOrientationVerticalFlip = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-msaa")) {
                i++;
                useMSAA = true;
                msaaNumSamples = MiscUtils.atoi(args[i], msaaNumSamples);
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-pbuffer")) {
                shallUsePBuffer = true;
            } else if(args[i].equals("-bitmap")) {
                shallUseBitmap = true;
            } else if(args[i].equals("-manual")) {
                manualTest = true;
            } else if(args[i].equals("-demo")) {
                i++;
                demoType = MiscUtils.atoi(args[i], 0);
            }
        }
        wsize = new Dimension(w, h);
        if( 0 < rw && 0 < rh ) {
            rwsize = new Dimension(rw, rh);
        }

        System.err.println("size "+wsize);
        System.err.println("resize "+rwsize);
        System.err.println("userVFlip "+skipGLOrientationVerticalFlip);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("forceGLFFP "+forceGLFFP);
        System.err.println("useMSAA "+useMSAA+", msaaNumSamples "+msaaNumSamples);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("shallUsePBuffer "+shallUsePBuffer);
        System.err.println("shallUseBitmap "+shallUseBitmap);
        System.err.println("manualTest "+manualTest);
        System.err.println("demoType "+demoType);

        org.junit.runner.JUnitCore.main(TestGearsES2GLJPanelAWT.class.getName());
    }
}
