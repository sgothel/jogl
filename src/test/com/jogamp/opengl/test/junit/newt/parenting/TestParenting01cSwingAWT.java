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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Container;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting01cSwingAWT extends UITestCase {
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

    static class GLDisturbanceAction implements Runnable {
        public boolean isRunning = false;
        private volatile boolean shallStop = false;
        private final GLAutoDrawable glad;
        private final GLRunnable glRunnable;

        public GLDisturbanceAction(final GLAutoDrawable glad) {
            this.glad = glad;
            this.glRunnable = new GLRunnableDummy();
        }

        public void waitUntilRunning() {
            synchronized(this) {
                while(!isRunning) {
                    try {
                        this.wait();
                    } catch (final InterruptedException e) { e.printStackTrace(); }
                }
            }
        }

        public void stopAndWaitUntilDone() {
            shallStop = true;
            synchronized(this) {
                while(isRunning) {
                    try {
                        this.wait();
                    } catch (final InterruptedException e) { e.printStackTrace(); }
                }
            }
        }

        public void run() {
            synchronized(this) {
                isRunning = true;
                this.notifyAll();
                System.err.println("$");
            }
            while(!shallStop) {
               try {
                   glad.invoke(true, glRunnable);
                   Thread.sleep(100);
               } catch (final Throwable t) {}
            }
            synchronized(this) {
                isRunning = false;
                this.notifyAll();
            }
        }
    }

    @Test
    public void test01CreateVisibleDestroy1() throws InterruptedException, InvocationTargetException {
        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();

        final GLDisturbanceAction disturbanceAction = new GLDisturbanceAction(glWindow1);
        new InterruptSource.Thread(null, disturbanceAction).start();
        disturbanceAction.waitUntilRunning();

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        final Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        final JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(new Button("north"), BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        final JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        System.err.println("Demos: 1 - Visible");
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               jFrame1.setSize(width, height);
               jFrame1.validate();
               jFrame1.setVisible(true);
           }
        });
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glWindow1, true));
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, true));

        // visible test
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        System.err.println("Demos: 2 - StopAnimator");
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 3 - !Visible");
                    jFrame1.setVisible(false);
                } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, false));

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 4 - Visible");
                    jFrame1.setVisible(true);
                } });
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glWindow1, true));

        final boolean wasOnscreen = glWindow1.getChosenCapabilities().isOnscreen();

        // Always recommended to remove our native parented Window
        // from the AWT resources before destruction, since it could lead
        // to a BadMatch X11 error w/o.
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 5 - X Container");
                    jPanel1.remove(container1);
                    jFrame1.validate();
                } });
        if( wasOnscreen ) {
            Assert.assertEquals(true, glWindow1.isNativeValid());
        } // else OK to be destroyed - due to offscreen/onscreen transition

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 6 - X Frame");
                    jFrame1.dispose();
                } });
        if( wasOnscreen ) {
            Assert.assertEquals(true, glWindow1.isNativeValid());
        } // else OK to be destroyed - due to offscreen/onscreen transition

        System.err.println("Demos: 7 - X GLWindow");
        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());

        System.err.println("Demos: 8 - X DisturbanceThread");
        disturbanceAction.stopAndWaitUntilDone();
    }

    @Test
    public void test02AWTWinHopFrame2Frame() throws InterruptedException, InvocationTargetException {
        /**
         * JFrame . JPanel . Container . NewtCanvasAWT . GLWindow
         */
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        /*
        glWindow1.addGLEventListener(new GLEventListener() {
            @Override
            public void init(GLAutoDrawable drawable) {
                System.err.println("XXX init");
            }
            @Override
            public void dispose(GLAutoDrawable drawable) {
                System.err.println("XXX dispose");
                // Thread.dumpStack();
            }
            @Override
            public void display(GLAutoDrawable drawable) {}
            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
                System.err.println("XXX reshape");
                // Thread.dumpStack();
            }
        }); */
        glWindow1.addGLEventListener(demo1);
        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();

        final GLDisturbanceAction disturbanceAction = new GLDisturbanceAction(glWindow1);
        new InterruptSource.Thread(null, disturbanceAction).start();
        disturbanceAction.waitUntilRunning();

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        final Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        final JPanel jPanel1 = new JPanel();
        jPanel1.setLayout(new BorderLayout());
        jPanel1.add(new Button("north"), BorderLayout.NORTH);
        jPanel1.add(new Button("south"), BorderLayout.SOUTH);
        jPanel1.add(new Button("east"), BorderLayout.EAST);
        jPanel1.add(new Button("west"), BorderLayout.WEST);
        jPanel1.add(container1, BorderLayout.CENTER);

        final JFrame jFrame1 = new JFrame("Swing Parent JFrame");
        // jFrame1.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame1.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame1.setContentPane(jPanel1);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               jFrame1.setLocation(0, 0);
               jFrame1.setSize(width, height);
               jFrame1.setVisible(true);
           }
        });

        final JPanel jPanel2 = new JPanel();
        jPanel2.setLayout(new BorderLayout());
        jPanel2.add(new Button("north"), BorderLayout.NORTH);
        jPanel2.add(new Button("south"), BorderLayout.SOUTH);
        jPanel2.add(new Button("east"), BorderLayout.EAST);
        jPanel2.add(new Button("west"), BorderLayout.WEST);

        final JFrame jFrame2 = new JFrame("Swing Parent JFrame");
        // jFrame2.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        jFrame2.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE); // equivalent to Frame, use windowClosing event!
        jFrame2.setContentPane(jPanel2);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               jFrame2.setLocation(640, 480);
               jFrame2.setSize(width, height);
               jFrame2.setVisible(true);
           }
        });

        // visible test
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        final boolean wasOnscreen = glWindow1.getChosenCapabilities().isOnscreen();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                container1.remove(newtCanvasAWT);
                                jPanel2.add(newtCanvasAWT, BorderLayout.CENTER);
                                jFrame1.validate();
                                jFrame2.validate();
                            } });
                    break;
                case 1:
                    SwingUtilities.invokeAndWait(new Runnable() {
                            public void run() {
                                jPanel2.remove(newtCanvasAWT);
                                container1.add(newtCanvasAWT, BorderLayout.CENTER);
                                jFrame1.validate();
                                jFrame2.validate();
                            } });
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        /*
         * Always recommended to remove our native parented Window
         * from the AWT resources before destruction, since it could lead
         * to a BadMatch X11 error w/o (-> XAWT related).
         * Or ensure old/new parent is visible, see below.
         *
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 1 - X Container 1");
                    container1.remove(newtCanvasAWT);
                    jFrame1.validate();
                    System.err.println("Demos: 1 - X Container 2");
                    jPanel2.remove(newtCanvasAWT);
                    jFrame2.validate();
                } }); */
        /*
         * Invisible X11 windows may also case BadMatch (-> XAWT related)
         */
        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 2 - !visible");
                    jFrame1.setVisible(false);
                    System.err.println("Demos: 3 - !visible");
                    jFrame2.setVisible(false);
                } });
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    System.err.println("Demos: 4 - X frame");
                    jFrame1.dispose();
                    System.err.println("Demos: 5 - X frame");
                    jFrame2.dispose();
                } });
        if( wasOnscreen ) {
            Assert.assertEquals(true, glWindow1.isNativeValid());
        } // else OK to be destroyed - due to offscreen/onscreen transition

        System.err.println("Demos: 6 - X GLWindow");
        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());

        System.err.println("Demos: 7 - X DisturbanceThread");
        disturbanceAction.stopAndWaitUntilDone();
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        final Window window = glWindow.getDelegatedWindow();
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", window)) {
            MiscUtils.setFieldIfExists(demo, "glWindow", glWindow);
        }
    }

    static int atoi(final String a) {
        int i=0;
        try {
            i = Integer.parseInt(a);
        } catch (final Exception ex) { ex.printStackTrace(); }
        return i;
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-wait")) {
                waitReparent = atoi(args[++i]);
            }
        }
        System.err.println("durationPerTest "+durationPerTest);
        System.err.println("waitReparent "+waitReparent);
        org.junit.runner.JUnitCore.main(TestParenting01cSwingAWT.class.getName());
        /**
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
            "formatter=org.apache.tools.ant.taskdefs.optional.junit.XMLJUnitResultFormatter,TEST-"+tstname+".xml" } ); */
    }

}
