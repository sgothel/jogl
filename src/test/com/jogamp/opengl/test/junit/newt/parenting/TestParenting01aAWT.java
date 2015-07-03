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
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import java.awt.Button;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Frame;

import com.jogamp.opengl.*;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting01aAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static long waitReparent = 0;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() throws InterruptedException {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
        // Thread.sleep(10000);
    }

    @Test
    public void test01WindowParenting01CreateVisibleDestroy1() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        final Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);

        final Container container1 = new Container();
        container1.setLayout(new BorderLayout());
        container1.add(new Button("north"), BorderLayout.NORTH);
        container1.add(new Button("south"), BorderLayout.SOUTH);
        container1.add(new Button("east"), BorderLayout.EAST);
        container1.add(new Button("west"), BorderLayout.WEST);
        container1.add(newtCanvasAWT, BorderLayout.CENTER);

        frame1.add(container1, BorderLayout.CENTER);

        // visible test
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setSize(width, height);
               frame1.setVisible(true);
           }
        });
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow1, true));

        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setVisible(false);
           } } );
        Assert.assertEquals(true, glWindow1.isNativeValid());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setVisible(true);
           } } );
        Assert.assertEquals(true, glWindow1.isNativeValid());

        final boolean wasOnscreen = glWindow1.getChosenCapabilities().isOnscreen();

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.remove(newtCanvasAWT);
           } } );
        // Assert.assertNull(glWindow1.getParent());
        if( wasOnscreen ) {
            Assert.assertEquals(true, glWindow1.isNativeValid());
        } // else OK to be destroyed - due to offscreen/onscreen transition

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.dispose();
           } } );
        if( wasOnscreen ) {
            Assert.assertEquals(true, glWindow1.isNativeValid());
        } // else OK to be destroyed - due to offscreen/onscreen transition

        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
    }

    @Test
    public void test02WindowParenting02CreateVisibleDestroy2Defered() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());

        final Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);

        // visible test
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.setSize(width, height);
               frame.setVisible(true);
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.add(newtCanvasAWT);
               frame.validate();
           }
        });
        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.dispose();
           } } );
        glWindow1.destroy();
    }

    @Test
    public void test03WindowParenting02CreateVisibleDestroy3Odd() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        final Frame frame = new Frame("AWT Parent Frame");
        Assert.assertNotNull(frame);

        // visible test
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.setSize(width, height);
               frame.setVisible(true);
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.add(newtCanvasAWT);
               frame.validate();
           }
        });

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        Assert.assertEquals(true, animator1.isStarted());
        Assert.assertEquals(true, animator1.isAnimating());
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }

        Assert.assertEquals(true, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.dispose();
           } } );
        glWindow1.destroy();
    }

    @Test
    public void test04WindowParenting03ReparentNewtWin2Top() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        final Frame frame = new Frame("AWT Parent Frame");
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.setSize(width, height);
               frame.setLocation(640, 480);
               frame.setVisible(true);
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.add(newtCanvasAWT);
               frame.validate();
           }
        });

        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    System.err.println("Reparent CHILD -> TOP: "+glWindow1.reparentWindow(null, -1, -1, 0 /* hints */));
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertNull(glWindow1.getParent());
                    break;
                case 1:
                    System.err.println("Reparent TOP -> CHILD: "+glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow(), -1, -1, 0 /* hints */));
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.dispose();
           } } );
        glWindow1.destroy();
    }

    @Test
    public void test05WindowParenting04ReparentNewtWin2TopLayouted() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        final Frame frame = new Frame("AWT Parent Frame");
        frame.setLayout(new BorderLayout());
        frame.add(new Button("North"), BorderLayout.NORTH);
        frame.add(new Button("South"), BorderLayout.SOUTH);
        frame.add(new Button("East"), BorderLayout.EAST);
        frame.add(new Button("West"), BorderLayout.WEST);

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.setSize(width, height);
               frame.setLocation(640, 480);
               frame.setVisible(true);
           }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.add(newtCanvasAWT, BorderLayout.CENTER);
               frame.validate();
           }
        });

        Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    System.err.println("Reparent CHILD -> TOP: "+glWindow1.reparentWindow(null, -1, -1, 0 /* hints */));
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertNull(glWindow1.getParent());
                    break;
                case 1:
                    System.err.println("Reparent TOP -> CHILD: "+glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow(), -1, -1, 0 /* hints */));
                    Assert.assertEquals(true, glWindow1.isNativeValid());
                    Assert.assertEquals(newtCanvasAWT.getNativeWindow(),glWindow1.getParent());
                    break;
            }
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame.dispose();
           } } );
        glWindow1.destroy();
    }

    @Test
    public void test06WindowParenting05ReparentAWTWinHopFrame2Frame() throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setUndecorated(true);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);

        final Frame frame1 = new Frame("AWT Parent Frame");
        frame1.setLayout(new BorderLayout());
        frame1.add(new Button("North"), BorderLayout.NORTH);
        frame1.add(new Button("South"), BorderLayout.SOUTH);
        frame1.add(new Button("East"), BorderLayout.EAST);
        frame1.add(new Button("West"), BorderLayout.WEST);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame1.setSize(width, height);
               frame1.setLocation(0, 0);
               frame1.setVisible(true);
           }
        });

        final Frame frame2 = new Frame("AWT Parent Frame");
        frame2.setLayout(new BorderLayout());
        frame2.add(new Button("North"), BorderLayout.NORTH);
        frame2.add(new Button("South"), BorderLayout.SOUTH);
        frame2.add(new Button("East"), BorderLayout.EAST);
        frame2.add(new Button("West"), BorderLayout.WEST);
        SwingUtilities.invokeAndWait(new Runnable() {
           public void run() {
               frame2.setSize(width, height);
               frame2.setLocation(640, 480);
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

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();

        int state = 0;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<3*durationPerTest) {
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
            state++;
        }

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());

        SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame1.dispose();
                frame2.dispose();
            } } );
        glWindow1.destroy();
    }

    public static void setDemoFields(final GLEventListener demo, final GLWindow glWindow, final boolean debug) {
        Assert.assertNotNull(demo);
        Assert.assertNotNull(glWindow);
        if(debug) {
            MiscUtils.setFieldIfExists(demo, "glDebug", true);
            MiscUtils.setFieldIfExists(demo, "glTrace", true);
        }
        if(!MiscUtils.setFieldIfExists(demo, "window", glWindow.getDelegatedWindow())) {
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
        final String tstname = TestParenting01aAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
