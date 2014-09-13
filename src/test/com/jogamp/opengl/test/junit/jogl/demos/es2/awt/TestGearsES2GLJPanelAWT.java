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

import javax.media.nativewindow.ScalableSurface;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
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
    static boolean forceGL3 = false;
    static boolean forceGLFFP = false;
    static boolean shallUsePBuffer = false;
    static boolean shallUseBitmap = false;
    static boolean useMSAA = false;
    static int swapInterval = 0;
    static boolean useAnimator = true;
    static boolean manualTest = false;
    static boolean skipGLOrientationVerticalFlip = false;
    static int xpos = 10, ypos = 10;
    static int[] reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };

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
        frame.setTitle("GLJPanel["+capsA+"], swapI "+swapInterval+", win: ["+b.x+"/"+b.y+" "+b.width+"x"+b.height+"], pix: "+glc.getSurfaceWidth()+"x"+glc.getSurfaceHeight());
    }

    protected void runTestGL(final GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);

        final GLJPanel glJPanel = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel);
        glJPanel.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        glJPanel.setMinimumSize(wsize);
        glJPanel.setPreferredSize(wsize);
        glJPanel.setSize(wsize);
        glJPanel.setSurfaceScale(reqSurfacePixelScale);
        final int[] valReqSurfacePixelScale = glJPanel.getRequestedSurfaceScale(new int[2]);
        if( caps.isBitmap() || caps.getGLProfile().isGL2() ) {
            final Gears gears = new Gears(swapInterval);
            gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glJPanel.addGLEventListener(gears);
        } else {
            final GearsES2 gears = new GearsES2(swapInterval);
            gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glJPanel.addGLEventListener(gears);
        }
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glJPanel.addGLEventListener(snap);
        setTitle(frame, glJPanel, caps);
        frame.setLocation(xpos, ypos);

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

        final FPSAnimator animator = useAnimator ? new FPSAnimator(glJPanel, 60) : null;

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
                    frame.getContentPane().validate();
                    frame.pack();
                    frame.setVisible(true);
                } } ) ;
        Assert.assertEquals(true,  AWTRobotUtil.waitForVisible(frame, true));
        Assert.assertEquals(true,  AWTRobotUtil.waitForRealized(glJPanel, true));

        final int[] hasSurfacePixelScale0 = glJPanel.getNativeSurface().convertToPixelUnits(new int[] { 1, 1 });
        final int[] hasSurfacePixelScale1 = glJPanel.getCurrentSurfaceScale(new int[2]);
        System.err.println("HiDPI PixelScale: "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                           valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                           hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
        setTitle(frame, glJPanel, caps);
        Assert.assertArrayEquals(hasSurfacePixelScale0, hasSurfacePixelScale1);

        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glJPanel).addTo(glJPanel);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glJPanel).addTo(frame);

        final com.jogamp.newt.event.KeyListener kl = new com.jogamp.newt.event.KeyAdapter() {
            @Override
            public void keyPressed(final KeyEvent e) {
                if( e.isAutoRepeat() ) {
                    return;
                }
                if(e.getKeyChar()=='x') {
                    final int[] hadSurfacePixelScale = glJPanel.getCurrentSurfaceScale(new int[2]);
                    final int[] reqSurfacePixelScale;
                    if( hadSurfacePixelScale[0] == ScalableSurface.IDENTITY_PIXELSCALE ) {
                        reqSurfacePixelScale = new int[] { ScalableSurface.AUTOMAX_PIXELSCALE, ScalableSurface.AUTOMAX_PIXELSCALE };
                    } else {
                        reqSurfacePixelScale = new int[] { ScalableSurface.IDENTITY_PIXELSCALE, ScalableSurface.IDENTITY_PIXELSCALE };
                    }
                    System.err.println("[set PixelScale pre]: had "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" -> req "+reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]);
                    glJPanel.setSurfaceScale(reqSurfacePixelScale);
                    final int[] valReqSurfacePixelScale = glJPanel.getRequestedSurfaceScale(new int[2]);
                    final int[] hasSurfacePixelScale0 = glJPanel.getNativeSurface().convertToPixelUnits(new int[] { 1, 1 });
                    final int[] hasSurfacePixelScale1 = glJPanel.getCurrentSurfaceScale(new int[2]);
                    System.err.println("[set PixelScale post]: "+hadSurfacePixelScale[0]+"x"+hadSurfacePixelScale[1]+" (had) -> "+
                                       reqSurfacePixelScale[0]+"x"+reqSurfacePixelScale[1]+" (req) -> "+
                                       valReqSurfacePixelScale[0]+"x"+valReqSurfacePixelScale[1]+" (val) -> "+
                                       hasSurfacePixelScale1[0]+"x"+hasSurfacePixelScale1[1]+" (has)");
                    setTitle(frame, glJPanel, caps);
                    Assert.assertArrayEquals(hasSurfacePixelScale0, hasSurfacePixelScale1);
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
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.getContentPane().remove(glJPanel);
                    frame.remove(glJPanel);
                    glJPanel.destroy();
                    frame.dispose();
                } } );
    }

    @Test
    public void test01_DefaultNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLProfile glp;
        if(forceGL3) {
            glp = GLProfile.get(GLProfile.GL3);
        } else if(forceES2) {
            glp = GLProfile.get(GLProfile.GLES2);
        } else if(forceGLFFP) {
            glp = GLProfile.getMaxFixedFunc(true);
        } else {
            glp = GLProfile.getGL2ES2();
        }
        final GLCapabilities caps = new GLCapabilities( glp );
        if(useMSAA) {
            caps.setNumSamples(4);
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
    public void test30_GL3()
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
                final int pS = MiscUtils.atoi(args[i], reqSurfacePixelScale[0]);
                reqSurfacePixelScale[0] = pS;
                reqSurfacePixelScale[1] = pS;
            } else if(args[i].equals("-userVFlip")) {
                skipGLOrientationVerticalFlip = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-pbuffer")) {
                shallUsePBuffer = true;
            } else if(args[i].equals("-bitmap")) {
                shallUseBitmap = true;
            } else if(args[i].equals("-manual")) {
                manualTest = true;
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
        System.err.println("useMSAA "+useMSAA);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("shallUsePBuffer "+shallUsePBuffer);
        System.err.println("shallUseBitmap "+shallUseBitmap);
        System.err.println("manualTest "+manualTest);

        org.junit.runner.JUnitCore.main(TestGearsES2GLJPanelAWT.class.getName());
    }
}
