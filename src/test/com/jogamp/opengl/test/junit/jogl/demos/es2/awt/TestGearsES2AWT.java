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

public class TestGearsES2AWT extends UITestCase {
    public enum FrameLayout { None, TextOnBottom, BorderCenterSurrounded, DoubleBorderCenterSurrounded };
    public enum ResizeBy { Component, Frame };
    
    static long duration = 500; // ms    
    static int width, height;
    static FrameLayout frameLayout = FrameLayout.None;
    static ResizeBy resizeBy = ResizeBy.Component;
    
    static boolean forceES2 = false;
    static boolean forceGL3 = false;
    static boolean mainRun = false;
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
        width  = 640;
        height = 480;
        try {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    awtEDT = Thread.currentThread();
                } } );
        } catch (Exception e) {
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
        } catch( Throwable throwable ) {
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
        } catch( Throwable throwable ) {
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
    
    protected void runTestGL(GLCapabilities caps, final ResizeBy resizeBy, FrameLayout frameLayout) throws InterruptedException, InvocationTargetException {
        final Frame frame = new Frame("GearsES2 AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        setSize(resizeBy, frame, false, glCanvas, new Dimension(width, height));
        
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
                Container c = new Container();
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
        frame.setTitle("Gears AWT Test (translucent "+!caps.isBackgroundOpaque()+"), swapInterval "+swapInterval);

        final GearsES2 demo = new GearsES2(swapInterval);
        glCanvas.addGLEventListener(demo);
        
        final SnapshotGLEventListener snap = new SnapshotGLEventListener();
        glCanvas.addGLEventListener(snap);

        final Animator animator = useAnimator ? new Animator(glCanvas) : null;
        if( useAnimator && exclusiveContext ) {
            animator.setExclusiveContext(awtEDT);
        }
        QuitAdapter quitAdapter = new QuitAdapter();

        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter)).addTo(glCanvas);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter)).addTo(frame);

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
        
        if( useAnimator ) {
            animator.start();
            Assert.assertTrue(animator.isStarted());
            Assert.assertTrue(animator.isAnimating());
            Assert.assertEquals(exclusiveContext ? awtEDT : null, glCanvas.getExclusiveContextThread());
            animator.setUpdateFPSFrames(60, System.err);
        }
        
        System.err.println("canvas pos/siz: "+glCanvas.getX()+"/"+glCanvas.getY()+" "+glCanvas.getWidth()+"x"+glCanvas.getHeight());

        snap.setMakeSnapshot();
        
        if( null != rwsize ) {
            Thread.sleep(500); // 500ms delay 
            setSize(resizeBy, frame, true, glCanvas, rwsize);
            System.err.println("window resize pos/siz: "+glCanvas.getX()+"/"+glCanvas.getY()+" "+glCanvas.getWidth()+"x"+glCanvas.getHeight());
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
        if(mainRun) return;
        
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
        if(mainRun) return;
        
        if( !GLProfile.isAvailable(GLProfile.GL3) ) {
            System.err.println("GL3 n/a");
            return;
        }
        final GLProfile glp = GLProfile.get(GLProfile.GL3);
        final GLCapabilities caps = new GLCapabilities( glp );
        runTestGL(caps, resizeBy, frameLayout);
    }
    
    public static void main(String args[]) {
        boolean waitForKey = false;
        int rw=-1, rh=-1;
        
        mainRun = true;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-rwidth")) {
                i++;
                rw = MiscUtils.atoi(args[i], rw);
            } else if(args[i].equals("-rheight")) {
                i++;
                rh = MiscUtils.atoi(args[i], rh);
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
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
            System.err.println("Press enter to continue");
            try {
                System.err.println(stdin.readLine());
            } catch (IOException e) { }
        }
        org.junit.runner.JUnitCore.main(TestGearsES2AWT.class.getName());
    }
}
