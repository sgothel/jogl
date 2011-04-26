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

import java.lang.reflect.*;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Container;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import javax.media.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.event.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es1.RedSquare;

public class TestParenting01cSwingAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    @Test
    public void testWindowParenting01CreateVisibleDestroy1() throws InterruptedException, InvocationTargetException {
        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        
        final GLWindow _glWindow1 = glWindow1;
        final GLRunnable _glRunnable = new GLRunnableDummy();
        Thread disturbanceThread = new Thread(new Runnable() {
            public void run() {
                System.out.println("$");
                while(true) 
                {
                   try {
                       _glWindow1.invoke(true, _glRunnable);
                       Thread.yield();
                   } catch (Throwable t) {}
               }
            }
        });
        disturbanceThread.start();


        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(new Button("north"), BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        jFrame1.setSize(width, height);
        System.out.println("Demos: 1 - Visible");
        jFrame1.setVisible(true); // from here on, we need to run modifications on EDT

        final JFrame _jFrame1 = jFrame1;
        final JPanel _jPanel1 = jPanel1;
        final Container _container1 = container1;

        // visible test
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.out.println("Demos: 2 - StopAnimator");
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
            System.out.println("Demos: 3 - !Visible");
                    _jFrame1.setVisible(false);
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
            System.out.println("Demos: 4 - Visible");
                    _jFrame1.setVisible(true);
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
            System.out.println("Demos: 5 - X Container");
                    _jPanel1.remove(_container1);
                } });
        // Assert.assertNull(glWindow1.getParent());
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _jFrame1.dispose();
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
    }

    @Test
    public void testWindowParenting05ReparentAWTWinHopFrame2Frame() throws InterruptedException, InvocationTargetException {
        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        GLEventListener demo1 = new RedSquare();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        
        final GLWindow _glWindow1 = glWindow1;
        final GLRunnable _glRunnable = new GLRunnableDummy();
        Thread disturbanceThread = new Thread(new Runnable() {
            public void run() {
                System.out.println("$");
                while(true) 
                {
                   try {
                       _glWindow1.invoke(true, _glRunnable);
                       Thread.yield();
                   } catch (Throwable t) {}
               }
            }
        });
        disturbanceThread.start();

        NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(new Button("north"), BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        jFrame1.setLocation(0, 0);
        jFrame1.setSize(width, height);
        jFrame1.setVisible(true); // from here on, we need to run modifications on EDT

        JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(new Button("north"), BorderLayout.NORTH);
        jPanel2.add(new Button("south"), BorderLayout.SOUTH);
        jPanel2.add(new Button("east"), BorderLayout.EAST);
        jPanel2.add(new Button("west"), BorderLayout.WEST);

        JFrame jFrame2 = new JFrame("Swing Parent JFrame");
        // jFrame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame2.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame2.setContentPane(jPanel2);
        jFrame2.setLocation(640, 480);
        jFrame2.setSize(width, height);
        jFrame2.setVisible(true); // from here on, we need to run modifications on EDT

        final NewtCanvasAWT _newtCanvasAWT = newtCanvasAWT;
        final JFrame _jFrame1 = jFrame1;
        final Container _container1 = container1;
        final JFrame _jFrame2 = jFrame2;
        final JPanel _jPanel2 = jPanel2;

        // visible test
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                _container1.remove(_newtCanvasAWT);
                                _jPanel2.add(_newtCanvasAWT, BorderLayout.CENTER);
                            } });
                    break;
                case 1:
                    SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                _jPanel2.remove(_newtCanvasAWT);
                                _container1.add(_newtCanvasAWT, BorderLayout.CENTER);
                            } });
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _jFrame1.setVisible(false);
                    _jFrame2.setVisible(false);
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    _jFrame1.dispose();
                    _jFrame2.dispose();
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
    }

    public static void setDemoFields(GLEventListener demo, GLWindow glWindow, boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        Window window = glWindow.getWindow();
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
        System.out.println("durationPerTest "+durationPerTest);
        System.out.println("waitReparent "+waitReparent);
        String tstname = TestParenting01cSwingAWT.class.getName();
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
