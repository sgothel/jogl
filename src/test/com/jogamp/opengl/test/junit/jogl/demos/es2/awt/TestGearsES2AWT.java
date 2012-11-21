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

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import javax.media.opengl.awt.GLCanvas;

import com.jogamp.common.os.Platform;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.util.QuitAdapter;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.TextArea;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Test;

public class TestGearsES2AWT extends UITestCase {
    static int width, height;
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean shallUseOffscreenLayer = false;
    static boolean shallUseOffscreenPBufferLayer = false;
    static boolean useMSAA = false;
    static boolean useStencil = false;
    static boolean addComp = true;
    static boolean shutdownRemoveGLCanvas = true;
    static boolean shutdownDisposeFrame = true;
    static boolean shutdownSystemExit = false;
    static int swapInterval = 1;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
    }

    @AfterClass
    public static void releaseClass() {
    }

    protected void runTestGL(GLCapabilities caps) throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("GearsES2 AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        Dimension glc_sz = new Dimension(width, height);
        glCanvas.setMinimumSize(glc_sz);
        glCanvas.setPreferredSize(glc_sz);
        glCanvas.setSize(glc_sz);
        if(addComp) {
            final TextArea ta = new TextArea(2, 20);
            ta.append("0123456789");
            ta.append(Platform.getNewline());
            ta.append("Some Text");
            ta.append(Platform.getNewline());
            frame.add(ta, BorderLayout.SOUTH);
        }
        frame.add(glCanvas, BorderLayout.CENTER);
        frame.setTitle("Gears AWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval);

        glCanvas.addGLEventListener(new GearsES2(swapInterval));

        Animator animator = new Animator(glCanvas);
        QuitAdapter quitAdapter = new QuitAdapter();

        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter)).addTo(glCanvas);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }});
        animator.start();
        animator.setUpdateFPSFrames(60, System.err);

        while(!quitAdapter.shouldQuit() /* && animator.isAnimating() */ && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glCanvas);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        frame.setVisible(false);
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
        if(shallUseOffscreenLayer) {
            caps.setOnscreen(false);
        }
        if(shallUseOffscreenPBufferLayer) {
            caps.setPBuffer(true);
        }
        runTestGL(caps);
    }

    static long duration = 500; // ms

    public static void main(String args[]) {
        boolean waitForKey = false;
        
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            } else if(args[i].equals("-es2")) {
                forceES2 = true;
            } else if(args[i].equals("-gl3")) {
                forceGL3 = true;
            } else if(args[i].equals("-vsync")) {
                i++;
                swapInterval = MiscUtils.atoi(args[i], swapInterval);
            } else if(args[i].equals("-layered")) {
                shallUseOffscreenLayer = true;
            } else if(args[i].equals("-layeredPBuffer")) {
                shallUseOffscreenPBufferLayer = true;
            } else if(args[i].equals("-msaa")) {
                useMSAA = true;
            } else if(args[i].equals("-stencil")) {
                useStencil = true;
            } else if(args[i].equals("-wait")) {
                waitForKey = true;
            } else if(args[i].equals("-justGears")) {
                addComp = false;
            } else if(args[i].equals("-shutdownKeepGLCanvas")) {
                shutdownRemoveGLCanvas = false;
            } else if(args[i].equals("-shutdownKeepFrame")) {
                shutdownDisposeFrame = false;
            } else if(args[i].equals("-shutdownKeepAll")) {
                shutdownRemoveGLCanvas = false;
                shutdownDisposeFrame = false;
            } else if(args[i].equals("-shutdownSystemExit")) {
                shutdownSystemExit = true;
            }
        }
        System.err.println("forceES2 "+forceES2);
        System.err.println("forceGL3 "+forceGL3);
        System.err.println("swapInterval "+swapInterval);
        System.err.println("shallUseOffscreenLayer "+shallUseOffscreenLayer);
        
        if(waitForKey) {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestGearsES2AWT.class.getName());
    }
}
