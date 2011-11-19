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
 
package com.jogamp.opengl.test.junit.newt.parenting;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Frame;

import javax.media.opengl.*;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.FPSAnimator;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

public class TestParenting01bAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting05ReparentAWTWinHopFrame2FrameFPS25Animator() throws InterruptedException, InvocationTargetException {
        testWindowParenting05ReparentAWTWinHopFrame2FrameImpl(25);
    }

    @Test
    public void testWindowParenting05ReparentAWTWinHopFrame2FrameStdAnimator() throws InterruptedException, InvocationTargetException {
        testWindowParenting05ReparentAWTWinHopFrame2FrameImpl(0);
    }

    public void testWindowParenting05ReparentAWTWinHopFrame2FrameImpl(int fps) throws InterruptedException, InvocationTargetException {
        GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUndecorated(true);
        GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        
        final Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);
        frame1.setSize(width, height);
        frame1.setLocation(0, 0);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setVisible(true);               
           }
        });

        final Frame frame2 = new Frame("AWT Parent Frame");
        frame2.setLayout(new BorderLayout());
        frame2.add(new Button("North"), BorderLayout.NORTH);
        frame2.add(new Button("South"), BorderLayout.SOUTH);
        frame2.add(new Button("East"), BorderLayout.EAST);
        frame2.add(new Button("West"), BorderLayout.WEST);
        frame2.setSize(width, height);
        frame2.setLocation(640, 480);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame2.setVisible(true);               
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.add(newtCanvasAWT, BorderLayout.CENTER);
               frame1.validate();
           }
        });
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        GLAnimatorControl animator1;
        if(fps>0) {
            animator1 = new FPSAnimator(glWindow1, fps);
        } else {
            animator1 = new Animator(glWindow1);
        }
        animator1.start();

        int state;
        for(state=0; state<3; state++) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            frame1.remove(newtCanvasAWT);
                            frame2.add(newtCanvasAWT, BorderLayout.CENTER);
                            frame1.validate();
                            frame2.validate();
                        }
                    });                    
                    break;
                case 1:
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            frame2.remove(newtCanvasAWT);
                            frame1.add(newtCanvasAWT, BorderLayout.CENTER);
                            frame2.validate();
                            frame1.validate();
                        }
                    });                    
                    break;
            }
        }

        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.dispose();
                frame2.dispose();
            } } );
        glWindow1.destroy();
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        String tstname = TestParenting01bAWT.class.getName();
        org.apache.tools.ant.taskdefs.optional.junit.JUnitTestRunner.main(new String[] {
            tstname,
            "filtertrace=true",
            "haltOnError=false",
            "haltOnFailure=false",
            "showoutput=true",
            "outputtoformatters=true",
            "logfailedtests=true",
            "logtestlistenerevents=true",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.PlainJUnitResultFormatter",
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } );
    }

}
