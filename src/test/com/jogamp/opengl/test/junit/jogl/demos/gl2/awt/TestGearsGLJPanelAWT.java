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
 
package com.jogamp.opengl.test.junit.jogl.demos.gl2.awt;

import javax.media.opengl.*;

import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.util.FPSAnimator;
import javax.media.opengl.awt.GLJPanel;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGearsGLJPanelAWT extends UITestCase {
    static GLProfile glp;
    static int width, height;
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
            width  = 640;
            height = 480;
        } else {
            setTestSupported(false);
        }
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilities caps)
            throws AWTException, InterruptedException, InvocationTargetException
    {
        final JFrame frame = new JFrame("Swing GLJPanel");
        Assert.assertNotNull(frame);

        final GLJPanel glJPanel = new GLJPanel(caps);
        Assert.assertNotNull(glJPanel);
        Dimension glc_sz = new Dimension(width, height);
        glJPanel.setMinimumSize(glc_sz);
        glJPanel.setPreferredSize(glc_sz);
        glJPanel.setSize(glc_sz);
        glJPanel.addGLEventListener(new Gears(swapInterval));
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glJPanel.addGLEventListener(snap);

        final FPSAnimator animator = useAnimator ? new FPSAnimator(glJPanel, 60) : null;

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.getContentPane().add(glJPanel, BorderLayout.CENTER);
                    frame.getContentPane().validate();
                    frame.pack();
                    frame.setVisible(true);
                } } ) ;

        if( useAnimator ) {
            animator.setUpdateFPSFrames(60, System.err);
            animator.start();
            Assert.assertEquals(true, animator.isAnimating());
        }

        QuitAdapter quitAdapter = new QuitAdapter();

        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter)).addTo(glJPanel);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);
        
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
        Assert.assertNotNull(animator);

        if( useAnimator ) {
            animator.stop();
            Assert.assertEquals(false, animator.isAnimating());
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
        System.err.println("swapInterval "+swapInterval);
        System.err.println("useMSAA "+useMSAA);
        System.err.println("useAnimator "+useAnimator);
        System.err.println("shallUsePBuffer "+shallUsePBuffer);
        System.err.println("shallUseBitmap "+shallUseBitmap);
        System.err.println("manualTest "+manualTest);
        
        org.junit.runner.JUnitCore.main(TestGearsGLJPanelAWT.class.getName());
    }
}
