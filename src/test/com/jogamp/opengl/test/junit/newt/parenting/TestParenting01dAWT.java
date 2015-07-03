/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import com.jogamp.newt.Window;
import com.jogamp.newt.opengl.*;
import com.jogamp.newt.awt.NewtCanvasAWT;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.test.junit.util.*;
import com.jogamp.opengl.test.junit.jogl.demos.es2.RedSquareES2;

/**
 * Test GL preservation case for reparenting.
 * <p>
 * Also simulates adding and attaching an already created GLWindow
 * to a NewtCanvasAWT in recreation mode, where the GL state shall be preserved.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestParenting01dAWT extends UITestCase {
    static int width, height;
    static long durationPerTest = 800;
    static GLCapabilities glCaps;

    @BeforeClass
    public static void initClass() throws InterruptedException {
        width  = 640;
        height = 480;
        glCaps = new GLCapabilities(null);
        // Thread.sleep(10000);
    }

    static class MyGLEventListenerCounter extends GLEventListenerCounter {
        @Override
        public void init(final GLAutoDrawable drawable) {
            super.init(drawable);
            System.err.println("MyGLEventListenerCounter.init: "+this);
            // Thread.dumpStack();
        }

        @Override
        public void dispose(final GLAutoDrawable drawable) {
            super.dispose(drawable);
            System.err.println("MyGLEventListenerCounter.dispose: "+this);
            // Thread.dumpStack();
        }
    }

    @Test
    public void test01GLWindowReparentRecreateNoPreserve() throws InterruptedException, InvocationTargetException {
        testGLWindowInvisibleReparentRecreateImpl(false /* triggerPreserveGLState */);
    }

    @Test
    public void test02GLWindowReparentRecreateGLPreserve() throws InterruptedException, InvocationTargetException {
        testGLWindowInvisibleReparentRecreateImpl(true /* triggerPreserveGLState */);
    }

    private void testGLWindowInvisibleReparentRecreateImpl(final boolean triggerPreserveGLState) throws InterruptedException, InvocationTargetException {
        final GLWindow glWindow1 = GLWindow.create(glCaps);
        Assert.assertNotNull(glWindow1);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        glWindow1.setTitle("testWindowParenting01CreateVisibleDestroy");
        final MyGLEventListenerCounter glelCounter = new MyGLEventListenerCounter();
        glWindow1.addGLEventListener(glelCounter);
        final GLEventListener demo1 = new RedSquareES2();
        glWindow1.addGLEventListener(demo1);
        Assert.assertEquals("Init Counter Invalid "+glelCounter, 0, glelCounter.initCount);

        final NewtCanvasAWT newtCanvasAWT = new NewtCanvasAWT(glWindow1);
        Assert.assertNotNull(newtCanvasAWT);
        Assert.assertEquals(false, glWindow1.isVisible());
        Assert.assertEquals(false, glWindow1.isNativeValid());
        Assert.assertNull(glWindow1.getParent());
        Assert.assertEquals("Init Counter Invalid "+glelCounter, 0, glelCounter.initCount);

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

        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow1, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow1, true));
        glWindow1.display();
        Assert.assertEquals("Init Counter Invalid "+glelCounter, 1, glelCounter.initCount);
        Assert.assertEquals("Dispose Counter Invalid "+glelCounter, 0, glelCounter.disposeCount);

        final int reparentingHints = Window.REPARENT_HINT_FORCE_RECREATION |
                                     ( triggerPreserveGLState ? Window.REPARENT_HINT_BECOMES_VISIBLE : 0 );

        //
        // Even though the hint REPARENT_HINT_BECOMES_VISIBLE is not set (triggerPrerveGLState == false),
        // since GLWindow is visible already the GL state shall be preserved!
        //
        System.err.println(getSimpleTestName(".")+": Start Reparent #1");
        final Window.ReparentOperation rop1 = glWindow1.reparentWindow(null, -1, -1, reparentingHints);
        System.err.println(getSimpleTestName(".")+": Result Reparent #1: "+rop1);
        Assert.assertEquals(Window.ReparentOperation.ACTION_NATIVE_CREATION, rop1);
        glWindow1.display();
        Assert.assertEquals("Init Counter Invalid (Preserve Failed 1) "+glelCounter, 1, glelCounter.initCount);
        Assert.assertEquals("Dispose Counter Invalid (Preserve Failed 1) "+glelCounter, 0, glelCounter.disposeCount);

        //
        // The following step is equivalent with adding and attaching an already created GLWindow
        // to a NewtCanvasAWT in recreation mode if REPARENT_HINT_BECOMES_VISIBLE hint is set (triggerPrerveGLState == true).
        // GL state shall be preserved!
        //
        glWindow1.setVisible(false);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow1, false));
        System.err.println(getSimpleTestName(".")+": Start Reparent #2");
        final Window.ReparentOperation rop2 = glWindow1.reparentWindow(newtCanvasAWT.getNativeWindow(), -1, -1, reparentingHints);
        System.err.println(getSimpleTestName(".")+": Result Reparent #2: "+rop2);
        Assert.assertEquals(Window.ReparentOperation.ACTION_NATIVE_CREATION, rop2);
        glWindow1.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow1, true));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow1, true));
        glWindow1.display();
        if( triggerPreserveGLState ) {
            Assert.assertEquals("Init Counter Invalid (Preserve Failed 2) "+glelCounter, 1, glelCounter.initCount);
            Assert.assertEquals("Dispose Counter Invalid (Preserve Failed 2) "+glelCounter, 0, glelCounter.disposeCount);
        } else {
            Assert.assertEquals("Init Counter Invalid (Preserve Failed 2) "+glelCounter, 2, glelCounter.initCount);
            Assert.assertEquals("Dispose Counter Invalid (Preserve Failed 2) "+glelCounter, 1, glelCounter.disposeCount);
        }

        final long t0 = System.currentTimeMillis();
        long t1 = t0;
        while( t1 - t0 < durationPerTest ) {
            Thread.sleep(100);
            t1 = System.currentTimeMillis();
        }

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
        if( triggerPreserveGLState ) {
            Assert.assertEquals("Init Counter Invalid (Preserve Failed 1) "+glelCounter, 1, glelCounter.initCount);
            Assert.assertEquals("Dispose Counter Invalid (Preserve Failed 1) "+glelCounter, 1, glelCounter.disposeCount);
        } else {
            Assert.assertEquals("Init Counter Invalid (Preserve Failed 1) "+glelCounter, 2, glelCounter.initCount);
            Assert.assertEquals("Dispose Counter Invalid (Preserve Failed 1) "+glelCounter, 2, glelCounter.disposeCount);
        }
    }

    public static void main(final String args[]) throws IOException {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                durationPerTest = MiscUtils.atol(args[++i], durationPerTest);
            }
        }
        final String tstname = TestParenting01dAWT.class.getName();
        org.junit.runner.JUnitCore.main(tstname);
    }

}
