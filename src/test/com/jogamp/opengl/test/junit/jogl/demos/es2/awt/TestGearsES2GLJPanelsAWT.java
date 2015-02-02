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
import java.awt.Component;
import java.awt.Container;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;

import com.jogamp.opengl.GLAnimatorControl;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.opengl.util.awt.AWTGLPixelBuffer.SingleAWTGLPixelBufferProvider;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsES2GLJPanelsAWT extends UITestCase {
    static int demoCount = 4;
    static boolean jOpaque = false; // flicker-less w/o opaque, opaque leads to overdraw w/ mixed clipRects -> flicker - due to JComponent _paintImmediately(..) (?)
    static boolean glOpaque = true; // can be either ..
    static float glAlpha = 0.3f;
    static boolean jZOrder = false;
    static GLProfile glp;
    static boolean shallUsePBuffer = false;
    static boolean shallUseBitmap = false;
    static boolean useMSAA = false;
    static int swapInterval = 0;
    static boolean useAnimator = true;
    static boolean manualTest = false;
    static boolean initSingleBuffer = false;

    /**
     * Even though GLJPanel uses a SingleAWTGLPixelBufferProvider per default,
     * we like to initialize it's size to a common maximum to ensure
     * only one {@link AWTGLPixelBuffer} gets allocated.
     */
    static SingleAWTGLPixelBufferProvider singleAWTGLPixelBufferProvider;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
        } else {
            setTestSupported(false);
        }

        if( initSingleBuffer ) {
            singleAWTGLPixelBufferProvider = new SingleAWTGLPixelBufferProvider( glp.isGL2ES3() /* allowRowStride */);
            singleAWTGLPixelBufferProvider.initSingleton(null, 4, true, 600, 600, 1);
        } else {
            singleAWTGLPixelBufferProvider = null;
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    final static boolean useInterPanel = true;

    /** Adds new JPanel to frame's content pane at index 0 */
    private JComponent addPanel(final GLCapabilitiesImmutable caps, final GLAnimatorControl anim, final JFrame frame, final boolean opaque, final int x, final int y, final int w, final int h, final FloatBuffer color, final float[] clearColor)
            throws InterruptedException, InvocationTargetException
    {
        final GLJPanel canvas = new GLJPanel(caps);
        if( initSingleBuffer ) {
            canvas.setPixelBufferProvider( singleAWTGLPixelBufferProvider );
        }
        canvas.setOpaque(opaque);
        if ( !useInterPanel ) {
            canvas.setBounds(x, y, w, h);
        }
        final GLEventListener demo;
        if( caps.isBitmap() ) {
            demo = new Gears(swapInterval);
        } else {
            final GearsES2 gdemo = new GearsES2(swapInterval);
            gdemo.setIgnoreFocus(true);
            gdemo.setGearsColors(color, color, color);
            gdemo.setClearColor(clearColor);
            demo = gdemo;
        }
        canvas.addGLEventListener(demo);
        if( null != anim ) {
            anim.add(canvas);
        }

        final JPanel panel;
        final JTextField text;
        if ( useInterPanel ) {
            panel = new JPanel(new BorderLayout());
            panel.setBounds(x, y, w, h);
            panel.setOpaque(opaque);
            text = new JTextField(x+"/"+y+" "+w+"x"+h);
            text.setOpaque(true);
        } else {
            panel = null;
            text = null;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    if ( useInterPanel ) {
                        panel.add(text, BorderLayout.NORTH);
                        panel.add(canvas, BorderLayout.CENTER);
                        frame.getContentPane().add(panel, 0);
                    } else {
                        frame.getContentPane().add(canvas, 0);
                    }
                } } ) ;
        return useInterPanel ? panel : canvas;
    }

    public static final FloatBuffer red =    Buffers.newDirectFloatBuffer( new float[] { 1.0f, 0.0f, 0.0f, 1.0f } );
    public static final FloatBuffer green =  Buffers.newDirectFloatBuffer( new float[] { 0.0f, 1.0f, 0.0f, 1.0f } );
    public static final FloatBuffer blue =   Buffers.newDirectFloatBuffer( new float[] { 0.0f, 0.0f, 1.0f, 1.0f } );
    public static final FloatBuffer yellow = Buffers.newDirectFloatBuffer( new float[] { 1.0f, 1.0f, 0.0f, 1.0f } );
    public static final FloatBuffer grey   = Buffers.newDirectFloatBuffer( new float[] { 0.5f, 0.5f, 0.5f, 1.0f } );
    public static final float grayf = 0.3f;
    public static final float[] redish    = new float[] { grayf, 0.0f,  0.0f,  glAlpha };
    public static final float[] greenish  = new float[] { 0.0f,  grayf, 0.0f,  glAlpha };
    public static final float[] blueish   = new float[] { 0.0f,  0.0f,  grayf, glAlpha };
    public static final float[] yellowish = new float[] { grayf, grayf, 0.0f,  glAlpha };
    public static final float[] greyish   = new float[] { grayf, grayf, grayf,  glAlpha };

    protected void relayout(final Container cont, final float oW, final float oH) {
        final int count = cont.getComponentCount();
        final int nW = cont.getWidth();
        final int nH = cont.getHeight();
        for(int i = 0 ; i < count; i++ ) {
            final Component comp = cont.getComponent(i);
            final float fx = comp.getX() / oW;
            final float fy = comp.getY() / oH;
            final float fw = comp.getWidth() / oW;
            final float fh = comp.getHeight() / oH;
            comp.setBounds( (int)(fx * nW), (int)(fy * nH), (int)(fw * nW), (int)(fh * nH) );
        }
    }

    protected void runTestGL(final GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( !glOpaque ) {
            caps.setAlphaBits(caps.getRedBits());
        }

        final JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);

        final FPSAnimator animator = useAnimator ? new FPSAnimator(60) : null;

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.getContentPane().setLayout(null);
                } } );

        final float[] oldSize = new float[] { 600f, 600f };

        frame.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(final ComponentEvent e) {
                final int count = frame.getComponentCount();
                for(int i = 0 ; i < count; i++ ) {
                    relayout(frame.getContentPane(), oldSize[0], oldSize[1]);
                }
                frame.getContentPane().invalidate();
                frame.getContentPane().validate();
                // frame.pack();
                oldSize[0] = frame.getContentPane().getWidth();
                oldSize[1] = frame.getContentPane().getHeight();
            }
        } ) ;

        if( demoCount > 0 ) {
            addPanel(caps, animator, frame, jOpaque,  50,  50, 300, 300, red, redish); // A
        }
        if( demoCount > 1 ) {
            addPanel(caps, animator, frame, jOpaque,   0, 250, 300, 300, blue, blueish); // C
        }
        if( demoCount > 2 ) {
            addPanel(caps, animator, frame, jOpaque, 300,   0, 150, 150, green, greenish); // B
        }
        if( demoCount > 3 ) {
            addPanel(caps, animator, frame, jOpaque, 300, 300, 100, 100, yellow, yellowish); // D
        }
        if( jZOrder ) {
            final Container cont = frame.getContentPane();
            final int count = cont.getComponentCount();
            for(int i = 0 ; i < count; i++ ) {
                cont.setComponentZOrder(cont.getComponent(i), count - 1 - i);
            }
        }

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setSize((int)oldSize[0], (int)oldSize[1]);
                    frame.getContentPane().validate();
                    // frame.pack();
                    frame.setVisible(true);
                } } ) ;

        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }

        final QuitAdapter quitAdapter = new QuitAdapter();

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while(!quitAdapter.shouldQuit() && t1 - t0 < duration) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(animator);

        if( useAnimator ) {
            animator.stop();
            Assert.assertEquals(false, animator.isAnimating());
        }
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    // frame.getContentPane().removeAll();
                    // frame.removeAll();
                    frame.dispose();
                } } );
    }

    @Test
    public void test01_DefaultNorm()
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-jOpaque")) {
                i++;
                jOpaque = MiscUtils.atob(args[i], jOpaque);
            } else if(args[i].equals("-glOpaque")) {
                i++;
                glOpaque = MiscUtils.atob(args[i], glOpaque);
            } else if(args[i].equals("-alpha")) {
                i++;
                glAlpha = MiscUtils.atof(args[i], glAlpha);
            } else if(args[i].equals("-initSingleBuffer")) {
                i++;
                initSingleBuffer = MiscUtils.atob(args[i], initSingleBuffer);
            } else if(args[i].equals("-jZOrder")) {
                jZOrder = true;
            } else if(args[i].equals("-noanim")) {
                useAnimator  = false;
            } else if(args[i].equals("-pbuffer")) {
                shallUsePBuffer = true;
            } else if(args[i].equals("-bitmap")) {
                shallUseBitmap = true;
            } else if(args[i].equals("-manual")) {
                manualTest = true;
            } else if(args[i].equals("-demos")) {
                i++;
                demoCount = MiscUtils.atoi(args[i], demoCount);
            }
        }
        System.err.println("swapInterval "+swapInterval);
        System.err.println("opaque gl "+glOpaque+", java/gljpanel "+jOpaque);
        System.err.println("alpha "+glAlpha);
        System.err.println("jZOrder "+jZOrder);
        System.err.println("demos "+demoCount);
        System.err.println("useMSAA "+useMSAA);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("shallUsePBuffer "+shallUsePBuffer);
        System.err.println("shallUseBitmap "+shallUseBitmap);
        System.err.println("manualTest "+manualTest);
        System.err.println("useSingleBuffer "+initSingleBuffer);

        org.junit.runner.JUnitCore.main(TestGearsES2GLJPanelsAWT.class.getName());
    }
}
