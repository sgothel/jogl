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
import java.lang.reflect.InvocationTargetException;
import java.nio.FloatBuffer;

import javax.media.opengl.GLAnimatorControl;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jogamp.common.nio.Buffers;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.FPSAnimator;

public class TestGearsES2GLJPanelsAWT extends UITestCase {
    static int demoCount = 4;
    static boolean opaque = false; // always faster and flicker-less w/o opaque, i.e. w/ alpha channel due to JComponent _paintImmediately(..)
    static float alpha = 0.3f;
    static GLProfile glp;
    static boolean shallUsePBuffer = false;
    static boolean shallUseBitmap = false;
    static boolean useMSAA = false;
    static int swapInterval = 0;
    static boolean useAnimator = true;
    static boolean manualTest = false;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
        } else {
            setTestSupported(false);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }
    
    private void addPanel(GLCapabilitiesImmutable caps, GLAnimatorControl anim, final JFrame frame, boolean opaque, int x, int y, int w, int h, FloatBuffer color, float[] clearColor) 
            throws InterruptedException, InvocationTargetException 
    {
        final GLJPanel canvas = new GLJPanel(caps);
        canvas.setOpaque(opaque);
        final Dimension glc_sz = new Dimension(w, h);
        canvas.setMinimumSize(glc_sz);
        canvas.setPreferredSize(glc_sz);
        canvas.setSize(glc_sz);
        GearsES2 demo = new GearsES2(swapInterval);
        demo.setIgnoreFocus(true);
        demo.setGearsColors(color, color, color);
        demo.setClearColor(clearColor);
        canvas.addGLEventListener(demo);
        if( null != anim ) {
            anim.add(canvas);
        }

        final JPanel panel = new JPanel(new BorderLayout());
        panel.setBounds(x, y, w, h);
        panel.setOpaque(opaque);
        
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    panel.add(canvas, BorderLayout.CENTER);
                    frame.getContentPane().add(panel);
                } } ) ;
    }

    public static final FloatBuffer red =    Buffers.newDirectFloatBuffer( new float[] { 1.0f, 0.0f, 0.0f, 1.0f } );    
    public static final FloatBuffer green =  Buffers.newDirectFloatBuffer( new float[] { 0.0f, 1.0f, 0.0f, 1.0f } );
    public static final FloatBuffer blue =   Buffers.newDirectFloatBuffer( new float[] { 0.0f, 0.0f, 1.0f, 1.0f } );
    public static final FloatBuffer yellow = Buffers.newDirectFloatBuffer( new float[] { 1.0f, 1.0f, 0.0f, 1.0f } );
    public static final float grayf = 0.3f;
    public static final float[] redish    = new float[] { grayf, 0.0f,  0.0f,  alpha };
    public static final float[] greenish  = new float[] { 0.0f,  grayf, 0.0f,  alpha };
    public static final float[] blueish   = new float[] { 0.0f,  0.0f,  grayf, alpha };
    public static final float[] yellowish = new float[] { grayf, grayf, 0.0f,  alpha };
    
    protected void runTestGL(GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        if( !opaque ) {
            caps.setAlphaBits(caps.getRedBits());
        }
        
        final JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);
        
        final FPSAnimator animator = useAnimator ? new FPSAnimator(60) : null;

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.getContentPane().setLayout(null);
                } } );
        
        if( demoCount > 0 ) {
            addPanel(caps, animator, frame, opaque,  50,  50, 300, 300, red, redish); // A
        }
        if( demoCount > 1 ) {
            addPanel(caps, animator, frame, opaque, 200,   0, 150, 150, green, greenish); // B
        }
        if( demoCount > 2 ) {
            addPanel(caps, animator, frame, opaque,   0, 250, 300, 300, blue, blueish); // C
        }
        if( demoCount > 3 ) {
            addPanel(caps, animator, frame, opaque, 300, 300, 100, 100, yellow, yellowish); // D
        }
          
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setSize(600, 600);
                    frame.getContentPane().validate();
                    // frame.pack();
                    frame.setVisible(true);
                } } ) ;

        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }

        QuitAdapter quitAdapter = new QuitAdapter();

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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
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
        GLCapabilities caps = new GLCapabilities(GLProfile.getDefault());
        caps.setNumSamples(4);
        caps.setSampleBuffers(true);
        caps.setBitmap(true);
        runTestGL(caps);
    }
    
    static long duration = 500; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-opaque")) {
                opaque = true;
            } else if(args[i].equals("-alpha")) {
                i++;
                alpha = MiscUtils.atof(args[i], alpha);
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
        System.err.println("opaque "+opaque);
        System.err.println("alpha "+alpha);
        System.err.println("demos "+demoCount);
        System.err.println("useMSAA "+useMSAA);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("shallUsePBuffer "+shallUsePBuffer);
        System.err.println("shallUseBitmap "+shallUseBitmap);
        System.err.println("manualTest "+manualTest);
        
        org.junit.runner.JUnitCore.main(TestGearsES2GLJPanelsAWT.class.getName());
    }
}
