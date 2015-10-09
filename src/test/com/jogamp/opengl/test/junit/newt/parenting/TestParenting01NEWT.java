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

import com.jogamp.opengl.*;

import com.jogamp.opengl.util.Animator;
import com.jogamp.newt.*;
import com.jogamp.newt.opengl.*;

import java.io.IOException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting01NEWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 600;
    static boolean manual = false;
    static int loopVisibleToggle = 10;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
    }

    private static void waitForFrames(final String waitFor, final int prefixIdx,
                                      final GLWindow glWindow1, final GLWindow glWindow2,
                                      final long TO, final boolean doAssert) {
        final long t0 = System.currentTimeMillis();
        int a, b;
        long t1;
        do {
            try { Thread.sleep(16); } catch (final InterruptedException e) { }
            if( null != glWindow1 ) {
                a = glWindow1.getTotalFPSFrames();
            } else {
                a = -1;
            }
            if( null != glWindow2 ) {
                b = glWindow2.getTotalFPSFrames();
            } else {
                b = -1;
            }
            t1 = System.currentTimeMillis();
        } while ( ( 0 == a || 0 == b ) && TO > ( t1 - t0 ) );
        System.err.println("Frames for "+waitFor+": A"+prefixIdx+": "+a+", B"+prefixIdx+": "+b);
        if( doAssert ) {
            if( null != glWindow1 ) {
                Assert.assertTrue("No frames."+prefixIdx+" displayed on window1 during "+TO+"ms", 0 < a);
            }
            if( null != glWindow2 ) {
                Assert.assertTrue("No frames."+prefixIdx+" displayed on window2 during "+TO+"ms", 0 < b);
            }
        }
    }

    @Test
    public void test01CreateVisibleDestroy() throws InterruptedException {
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Display display = null;
        Screen screen = null;

        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        screen = glWindow1.getScreen();
        display = screen.getDisplay();
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        glWindow1.setTitle("test01CreateVisibleDestroy");
        glWindow1.setSize(640, 480);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        final GLWindow glWindow2 = GLWindow.create(glWindow1, glCaps);
        Assert.assertNotNull(glWindow2);
        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());
        Assert.assertSame(glWindow1,glWindow2.getParent());
        Assert.assertSame(screen,glWindow2.getScreen());
        Assert.assertSame(display,glWindow2.getScreen().getDisplay());
        glWindow2.setSize(320, 240);
        final GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning()); // GLWindow -> invoke ..
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // visible test
        for(int i=1; i<=loopVisibleToggle; i++) {
            Assert.assertEquals(0, glWindow1.getTotalFPSFrames());
            Assert.assertEquals(0, glWindow2.getTotalFPSFrames());
            System.err.println("XXX VISIBLE."+i+" -> TRUE");
            glWindow1.setVisible(true);
            Assert.assertEquals(true, glWindow1.isVisible());
            Assert.assertEquals(true, glWindow1.isNativeValid());
            Assert.assertEquals(true, glWindow2.isVisible());
            Assert.assertEquals(true, glWindow2.isNativeValid());
            Assert.assertEquals(1,display.getReferenceCount());
            Assert.assertEquals(true,display.isNativeValid());
            Assert.assertNotNull(display.getEDTUtil());
            Assert.assertEquals(true,display.getEDTUtil().isRunning());
            Assert.assertEquals(2,screen.getReferenceCount());
            Assert.assertEquals(true,screen.isNativeValid());
            Assert.assertEquals(1,Display.getActiveDisplayNumber());
            waitForFrames("window1.setVisible(true)", 1, glWindow1, glWindow2, 2000, true);

            System.err.println("XXX VISIBLE."+i+" -> FALSE");
            glWindow1.setVisible(false);
            Assert.assertEquals(false, glWindow1.isVisible());
            Assert.assertEquals(true, glWindow1.isNativeValid());
            Assert.assertEquals(false, glWindow2.isVisible());
            Assert.assertEquals(true, glWindow2.isNativeValid());

            glWindow1.resetFPSCounter();
            glWindow2.resetFPSCounter();
        }
        Assert.assertEquals(0, glWindow1.getTotalFPSFrames());
        Assert.assertEquals(0, glWindow2.getTotalFPSFrames());
        System.err.println("XXX VISIBLE.3 -> TRUE");
        glWindow1.setVisible(true);
        Assert.assertEquals(true, glWindow1.isVisible());
        Assert.assertEquals(true, glWindow1.isNativeValid());
        Assert.assertEquals(true, glWindow2.isVisible());
        Assert.assertEquals(true, glWindow2.isNativeValid());
        waitForFrames("window1.setVisible(true)", 2, glWindow1, glWindow2, 2000, true);

        glWindow1.resetFPSCounter();
        glWindow2.resetFPSCounter();
        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());
        final Animator animator2 = new Animator(glWindow2);
        animator2.setUpdateFPSFrames(1, null);
        animator2.start();
        Assert.assertEquals(true, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertNotNull(animator2.getThread());
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<durationPerTest) {
            Thread.sleep(100);
        }
        waitForFrames("animator.start()", 3, glWindow1, glWindow2, 2000, true);

        Assert.assertEquals(true, animator1.pause());
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(true, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());
        Assert.assertEquals(true, animator2.pause());
        Assert.assertEquals(false, animator2.isAnimating());
        Assert.assertEquals(true, animator2.isPaused());
        Assert.assertNotNull(animator2.getThread());

        glWindow1.resetFPSCounter();
        glWindow2.resetFPSCounter();
        Assert.assertEquals(true, animator1.resume());
        Assert.assertEquals(true, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertNotNull(animator1.getThread());
        Assert.assertEquals(true, animator2.resume());
        Assert.assertEquals(true, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertNotNull(animator2.getThread());
        waitForFrames("animator.resume()", 4, glWindow1, glWindow2, 2000, true);

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertEquals(null, animator2.getThread());

        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        glWindow2.destroy(); // can be recreated, refs are hold
        Assert.assertEquals(true,  glWindow1.isVisible());
        Assert.assertEquals(true,  glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());

        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        glWindow1.destroy(); // can be recreated, refs are hold
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());

        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // recreation ..
        glWindow1.resetFPSCounter();
        glWindow2.resetFPSCounter();
        Assert.assertEquals(0, glWindow1.getTotalFPSFrames());
        Assert.assertEquals(0, glWindow2.getTotalFPSFrames());
        System.err.println("XXX VISIBLE.4 -> TRUE");
        glWindow1.setVisible(true);
        Assert.assertEquals(true, glWindow1.isVisible());
        Assert.assertEquals(true, glWindow1.isNativeValid());
        Assert.assertEquals(true, glWindow2.isVisible());
        Assert.assertEquals(true, glWindow2.isNativeValid());
        waitForFrames("window1.setVisible(true) recreate", 5, glWindow1, glWindow2, 2000, true);

        Assert.assertEquals(1,display.getReferenceCount());
        Assert.assertEquals(true,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(true,display.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen.getReferenceCount());
        Assert.assertEquals(true,screen.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        // chain glwindow1 -> glwindow2 ; can be recreated ..
        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isNativeValid());
        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        // test double destroy/invalidate ..
        glWindow2.destroy();

        Assert.assertEquals(0,display.getReferenceCount());
        Assert.assertEquals(false,display.isNativeValid());
        Assert.assertNotNull(display.getEDTUtil());
        Assert.assertEquals(false,display.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen.getReferenceCount());
        Assert.assertEquals(false,screen.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    @Test
    public void test02aReparentTop2WinReparentRecreate() throws InterruptedException {
        if( manual ) {
            return;
        }
        test02ReparentTop2WinImpl(true);
    }

    @Test
    public void test02bReparentTop2WinReparentNative() throws InterruptedException {
        if( manual ) {
            return;
        }
        test02ReparentTop2WinImpl(false);
    }

    /**
     * @param reparentRecreate true, if the followup reparent should utilize destroy/create, instead of native reparenting
     */
    protected void test02ReparentTop2WinImpl(final boolean reparentRecreate) throws InterruptedException {
        final int reparentHints = reparentRecreate ? Window.REPARENT_HINT_FORCE_RECREATION : 0;

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Display display1 = null;
        Screen screen1 = null;

        final GLWindow glWindow1 = GLWindow.create(glCaps);
        glWindow1.setTitle("test02ReparentTop2Win");
        glWindow1.setSize(640, 480);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);
        screen1 = glWindow1.getScreen();
        display1 = screen1.getDisplay();

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        final GLWindow glWindow2 = GLWindow.create(glCaps);
        glWindow2.setSize(320, 240);
        final GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);
        Assert.assertSame(screen1, glWindow2.getScreen());
        Assert.assertSame(display1, glWindow2.getScreen().getDisplay());

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        Assert.assertEquals(0, glWindow1.getTotalFPSFrames());
        glWindow1.setVisible(true);
        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(true, glWindow1.isVisible());
        waitForFrames("window1.setVisible(true)", 1, glWindow1, null, 2000, true);
        Assert.assertEquals(0, glWindow2.getTotalFPSFrames());

        glWindow2.setVisible(true);
        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        Assert.assertEquals(true, glWindow2.isVisible());
        waitForFrames("window2.setVisible(true)", 2, glWindow1, glWindow2, 2000, true);

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        final Animator animator2 = new Animator(glWindow2);
        animator2.setUpdateFPSFrames(1, null);
        animator2.start();

        int state = 0;
        Window.ReparentOperation reparentAction;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<7*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    // top-level glWindow2 hide
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.setVisible(false);
                    Assert.assertEquals(false, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 1:
                    // top-level glWindow2 show
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(false, glWindow2.isVisible());
                    glWindow2.setVisible(true);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 2:
                    // glWindow2 -- child --> glWindow1: compatible
                    Assert.assertEquals(true, glWindow2.isVisible());
                    System.err.println("Frames(1) "+glWindow2.getTotalFPSFrames());
                    reparentAction = glWindow2.reparentWindow(glWindow1, -1, -1, reparentHints);
                    System.err.println("Frames(2) "+glWindow2.getTotalFPSFrames());
                    Assert.assertTrue(Window.ReparentOperation.ACTION_INVALID != reparentAction);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeValid());
                    Assert.assertSame(glWindow1,glWindow2.getParent());
                    waitForFrames("reparentWindow.child(parent, "+reparentRecreate+"), "+reparentAction, 10, glWindow1, glWindow2, 2000, true);

                    Assert.assertEquals(1,display1.getReferenceCount());
                    Assert.assertEquals(true,display1.isNativeValid());
                    Assert.assertNotNull(display1.getEDTUtil());
                    Assert.assertEquals(true,display1.getEDTUtil().isRunning());
                    Assert.assertEquals(true,screen1.isNativeValid());
                    Assert.assertSame(screen1,glWindow2.getScreen());
                    Assert.assertSame(display1,glWindow2.getScreen().getDisplay());
                    Assert.assertEquals(2,screen1.getReferenceCount());
                    Assert.assertEquals(1,Display.getActiveDisplayNumber());

                    break;

                case 3:
                    // child glWindow2 hide
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.setVisible(false);
                    Assert.assertEquals(false, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 4:
                    // child glWindow2 show
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(false, glWindow2.isVisible());
                    glWindow2.setVisible(true);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 5:
                    // glWindow2 --> top
                    Assert.assertEquals(true, glWindow2.isVisible());

                    reparentAction = glWindow2.reparentWindow(null, -1, -1, reparentHints);
                    Assert.assertTrue(Window.ReparentOperation.ACTION_INVALID != reparentAction);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeValid());
                    Assert.assertNull(glWindow2.getParent());
                    waitForFrames("reparentWindow.top(parent, "+reparentRecreate+"), "+reparentAction, 11, glWindow1, glWindow2, 2000, true);

                    Assert.assertEquals(1,display1.getReferenceCount());
                    Assert.assertEquals(true,display1.isNativeValid());
                    Assert.assertNotNull(display1.getEDTUtil());
                    Assert.assertEquals(true,display1.getEDTUtil().isRunning());
                    Assert.assertEquals(true,screen1.isNativeValid());
                    Assert.assertSame(screen1,glWindow2.getScreen());
                    Assert.assertSame(display1,glWindow2.getScreen().getDisplay());
                    Assert.assertEquals(2,screen1.getReferenceCount());
                    Assert.assertEquals(1,Display.getActiveDisplayNumber());

                    break;
            }
            state++;
        }
        //
        // both windows are now top level
        //

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertEquals(null, animator2.getThread());

        // pre-destroy check (both valid and running)
        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        // destroy glWindow2
        glWindow2.destroy();
        Assert.assertEquals(true,  glWindow1.isNativeValid());
        Assert.assertEquals(true,  glWindow1.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());
        Assert.assertEquals(false, glWindow2.isVisible());

        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(1,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());

        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        // destroy glWindow1
        glWindow1.destroy();
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());
        Assert.assertEquals(false, glWindow2.isVisible());

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(false,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
    }

    @Test
    public void test03aReparentWin2TopReparentRecreate() throws InterruptedException {
        if( manual ) {
            return;
        }
        test03ReparentWin2TopImpl(true);
    }

    @Test
    public void test03bReparentWin2TopReparentNative() throws InterruptedException {
        if( manual ) {
            return;
        }
        test03ReparentWin2TopImpl(false);
    }

    protected void test03ReparentWin2TopImpl(final boolean reparentRecreate) throws InterruptedException {
        final int reparentHints = reparentRecreate ? Window.REPARENT_HINT_FORCE_RECREATION : 0;

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
        Display display1 = null;
        Screen screen1 = null;
        Display display2 = null;
        Screen screen2 = null;

        final GLWindow glWindow1 = GLWindow.create(glCaps);
        screen1 = glWindow1.getScreen();
        display1 = screen1.getDisplay();
        glWindow1.setTitle("test03ReparentWin2Top");
        glWindow1.setSize(640, 480);
        final GLEventListener demo1 = new RedSquareES2();
        setDemoFields(demo1, glWindow1, false);
        glWindow1.addGLEventListener(demo1);

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        final GLWindow glWindow2 = GLWindow.create(glWindow1, glCaps);
        screen2 = glWindow2.getScreen();
        display2 = screen2.getDisplay();
        glWindow2.setSize(320, 240);
        final GLEventListener demo2 = new GearsES2();
        setDemoFields(demo2, glWindow2, false);
        glWindow2.addGLEventListener(demo2);

        Assert.assertEquals(0,display2.getReferenceCount());
        Assert.assertEquals(false,display2.isNativeValid());
        Assert.assertNotNull(display2.getEDTUtil());
        Assert.assertEquals(true,display2.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen2.getReferenceCount());
        Assert.assertEquals(false,screen2.isNativeValid());
        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        Assert.assertSame(screen1,glWindow2.getScreen());
        Assert.assertSame(display1,glWindow2.getScreen().getDisplay());

        Assert.assertEquals(0, glWindow1.getTotalFPSFrames());
        Assert.assertEquals(0, glWindow2.getTotalFPSFrames());
        glWindow1.setVisible(true);
        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());
        Assert.assertSame(glWindow1,glWindow2.getParent());
        Assert.assertSame(screen1,glWindow2.getScreen());
        Assert.assertEquals(1,Display.getActiveDisplayNumber());
        waitForFrames("window1.setVisible(true)", 1, glWindow1, glWindow2, 2000, true);

        final Animator animator1 = new Animator(glWindow1);
        animator1.setUpdateFPSFrames(1, null);
        animator1.start();
        final Animator animator2 = new Animator(glWindow2);
        animator2.setUpdateFPSFrames(1, null);
        animator2.start();

        int state = 0;
        Window.ReparentOperation reparentAction;
        while(animator1.isAnimating() && animator1.getTotalFPSDuration()<7*durationPerTest) {
            Thread.sleep(durationPerTest);
            switch(state) {
                case 0:
                    // child glWindow2 hide
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.setVisible(false);
                    Assert.assertEquals(false, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 1:
                    // child glWindow2 show
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(false, glWindow2.isVisible());
                    glWindow2.setVisible(true);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 2:
                    // glWindow2 --> top
                    Assert.assertEquals(true, glWindow2.isVisible());
                    reparentAction = glWindow2.reparentWindow(null, -1, -1, reparentHints);
                    Assert.assertTrue(Window.ReparentOperation.ACTION_INVALID != reparentAction);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeValid());
                    waitForFrames("reparentWindow.top(parent, "+reparentRecreate+"), "+reparentAction, 10, glWindow1, glWindow2, 2000, true);

                    Assert.assertNull(glWindow2.getParent());
                    Assert.assertSame(screen1,glWindow2.getScreen());
                    Assert.assertSame(display1,glWindow2.getScreen().getDisplay());
                    Assert.assertEquals(1,Display.getActiveDisplayNumber());
                    break;

                case 3:
                    // top-level glWindow2 hide
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(true, glWindow2.isVisible());
                    glWindow2.setVisible(false);
                    Assert.assertEquals(false, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 4:
                    // top-level glWindow2 show
                    Assert.assertEquals(true, glWindow1.isVisible());
                    Assert.assertEquals(false, glWindow2.isVisible());
                    glWindow2.setVisible(true);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow1.isVisible());
                    break;

                case 5:
                    // glWindow2 -- child --> glWindow1: compatible
                    Assert.assertEquals(true, glWindow2.isVisible());
                    reparentAction = glWindow2.reparentWindow(glWindow1, -1, -1, reparentHints);
                    Assert.assertTrue(Window.ReparentOperation.ACTION_INVALID != reparentAction);
                    Assert.assertEquals(true, glWindow2.isVisible());
                    Assert.assertEquals(true, glWindow2.isNativeValid());
                    waitForFrames("reparentWindow.child(parent, "+reparentRecreate+"), "+reparentAction, 11, glWindow1, glWindow2, 2000, true);

                    Assert.assertSame(glWindow1,glWindow2.getParent());
                    Assert.assertSame(screen1,glWindow2.getScreen());
                    Assert.assertSame(display1,glWindow2.getScreen().getDisplay());
                    Assert.assertEquals(1,Display.getActiveDisplayNumber());
                    break;
            }
            state++;
        }
        //
        // glwindow2 is child of glwindow1
        //

        animator1.stop();
        Assert.assertEquals(false, animator1.isAnimating());
        Assert.assertEquals(false, animator1.isPaused());
        Assert.assertEquals(null, animator1.getThread());
        animator2.stop();
        Assert.assertEquals(false, animator2.isAnimating());
        Assert.assertEquals(false, animator2.isPaused());
        Assert.assertEquals(null, animator2.getThread());

        Assert.assertEquals(1,display1.getReferenceCount());
        Assert.assertEquals(true,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(true,display1.getEDTUtil().isRunning());
        Assert.assertEquals(2,screen1.getReferenceCount());
        Assert.assertEquals(true,screen1.isNativeValid());
        Assert.assertSame(glWindow1,glWindow2.getParent());
        Assert.assertSame(screen1,glWindow2.getScreen());

        Assert.assertEquals(1,Display.getActiveDisplayNumber());

        glWindow1.destroy(); // should destroy both windows, actually, since glWindow2 is a child
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow2.isNativeValid());
        Assert.assertEquals(false, glWindow2.isVisible());

        Assert.assertEquals(0,display1.getReferenceCount());
        Assert.assertEquals(false,display1.isNativeValid());
        Assert.assertNotNull(display1.getEDTUtil());
        Assert.assertEquals(false,display1.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen1.getReferenceCount());
        Assert.assertEquals(false,screen1.isNativeValid());

        Assert.assertEquals(0,display2.getReferenceCount());
        Assert.assertEquals(false,display2.isNativeValid());
        Assert.assertNotNull(display2.getEDTUtil());
        Assert.assertEquals(false,display2.getEDTUtil().isRunning());
        Assert.assertEquals(0,screen2.getReferenceCount());
        Assert.assertEquals(false,screen2.isNativeValid());

        Assert.assertEquals(0,Display.getActiveDisplayNumber());

        glWindow2.destroy(); // dbl destroy check ..
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isNativeValid());

        glWindow1.destroy(); // parent -> child
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertEquals(false, glWindow2.isNativeValid());

        Assert.assertEquals(0,Display.getActiveDisplayNumber());
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
        boolean asMain = false;
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = atoi(args[++i]);
            } else if(args[i].equals("-loopvt")) {
                loopVisibleToggle = atoi(args[++i]);
            } else if(args[i].equals("-manual")) {
                manual = true;
            } else if(args[i].equals("-asMain")) {
                asMain = true;
            }
        }
        System.err.println("durationPerTest: "+durationPerTest);
        if( asMain ) {
            try {
                TestParenting01NEWT.initClass();
                final TestParenting01NEWT m = new TestParenting01NEWT();
                m.test01CreateVisibleDestroy();
                m.test02aReparentTop2WinReparentRecreate();
            } catch (final Throwable t ) {
                t.printStackTrace();
            }
        } else {
            final String tstname = TestParenting01NEWT.class.getName();
            org.junit.runner.JUnitCore.main(tstname);
        }
    }

}

