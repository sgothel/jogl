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

package com.jogamp.opengl.test.junit.jogl.acore.anim;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.awt.GLJPanel;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestAnimatorGLJPanel01AWT extends UITestCase {
    static final int width = 640;
    static final int height = 480;

    protected GLJPanel createGLJPanel(final GLCapabilities caps, final Frame frame, final int x, final int y, final GearsES2 gears) throws InterruptedException {
        final GLJPanel glCanvas = new GLJPanel(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(gears);
        frame.add(glCanvas);
        frame.setLocation(x, y);
        frame.setSize(width, height);
        frame.setTitle("GLJPanel: "+x+"/"+y);
        return glCanvas;
    }

    static void pauseAnimator(final Animator animator, final boolean pause) {
        if(pause) {
            animator.pause();
            Assert.assertEquals(true, animator.isStarted());
            Assert.assertEquals(true, animator.isPaused());
            Assert.assertEquals(false, animator.isAnimating());
        } else {
            animator.resume();
            Assert.assertEquals(true, animator.isStarted());
            Assert.assertEquals(false, animator.isPaused());
            Assert.assertEquals(true, animator.isAnimating());
        }
    }
    static void stopAnimator(final Animator animator) {
        animator.stop();
        Assert.assertEquals(false, animator.isStarted());
        Assert.assertEquals(false, animator.isPaused());
        Assert.assertEquals(false, animator.isAnimating());
    }

    @Test
    public void test01SyncedOneAnimator() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        final Frame f1 = new Frame();
        final Animator animator = new Animator();
        animator.start();
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(true, animator.isPaused());
        Assert.assertEquals(false, animator.isAnimating());

        final GearsES2 g1 = new GearsES2(0);
        final GLJPanel c1 = createGLJPanel(caps, f1, 0, 0, g1);
        animator.add(c1);
        Assert.assertEquals(true, animator.isStarted());
        Assert.assertEquals(false, animator.isPaused());
        Assert.assertEquals(true, animator.isAnimating());

        final Frame f2 = new Frame();
        final GearsES2 g2 = new GearsES2(0);
        final GLJPanel c2 = createGLJPanel(caps, f2, f1.getX()+width,
                                           f1.getY()+0, g2);
        animator.add(c2);

        final Frame f3 = new Frame();
        final GearsES2 g3 = new GearsES2(0);
        final GLJPanel c3 = createGLJPanel(caps, f3, f1.getX()+0,
                                           f1.getY()+height, g3);
        animator.add(c3);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f1.setVisible(true);
                f2.setVisible(true);
                f3.setVisible(true);
            } } );

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c3, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c3, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c3, true));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        pauseAnimator(animator, true);

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        pauseAnimator(animator, false);

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        stopAnimator(animator);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    f1.dispose();
                    f2.dispose();
                    f3.dispose();
                } catch (final Throwable t) {
                    throw new RuntimeException(t);
                }
            }});

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c3, false));
    }

    @Test
    public void test02AsyncEachAnimator() throws InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        final Frame f1 = new Frame();
        final Animator a1 = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        final GLJPanel c1 = createGLJPanel(caps, f1, 0, 0, g1);
        a1.add(c1);
        a1.start();
        Assert.assertEquals(true, a1.isStarted());
        Assert.assertEquals(false, a1.isPaused());
        Assert.assertEquals(true, a1.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f1.setVisible(true);
            } } );

        final Frame f2 = new Frame();
        final Animator a2 = new Animator();
        final GearsES2 g2 = new GearsES2(0);
        final GLJPanel c2 = createGLJPanel(caps, f2, f1.getX()+width, f1.getY()+0, g2);
        a2.add(c2);
        a2.start();
        Assert.assertEquals(true, a2.isStarted());
        Assert.assertEquals(false, a2.isPaused());
        Assert.assertEquals(true, a2.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f2.setVisible(true);
            } } );

        final Frame f3 = new Frame();
        final Animator a3 = new Animator();
        final GearsES2 g3 = new GearsES2(0);
        final GLJPanel c3 = createGLJPanel(caps, f3, f1.getX()+0, f1.getY()+height, g3);
        a3.add(c3);
        a3.start();
        Assert.assertEquals(true, a3.isStarted());
        Assert.assertEquals(false, a3.isPaused());
        Assert.assertEquals(true, a3.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f3.setVisible(true);
            } } );

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c3, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(c3, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(c3, true));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        pauseAnimator(a1, true);
        pauseAnimator(a2, true);
        pauseAnimator(a3, true);

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        pauseAnimator(a1, false);
        pauseAnimator(a2, false);
        pauseAnimator(a3, false);

        try {
            Thread.sleep(duration/3);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        stopAnimator(a1);
        stopAnimator(a2);
        stopAnimator(a3);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    f1.dispose();
                    f2.dispose();
                    f3.dispose();
                } catch (final Throwable t) {
                    throw new RuntimeException(t);
                }
            }});

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c3, false));
    }

    static long duration = 3*500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestAnimatorGLJPanel01AWT.class.getName());
    }
}
