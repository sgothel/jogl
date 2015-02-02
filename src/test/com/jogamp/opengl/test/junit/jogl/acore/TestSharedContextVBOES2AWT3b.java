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

package com.jogamp.opengl.test.junit.jogl.acore;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Sharing the VBO of 3 GearsES2 instances, each in their own AWT GLJPanel.
 * <p>
 * This is achieved by using the 1st GLJPanel as the <i>master</i>
 * and using the build-in blocking mechanism to postpone creation
 * of the 2nd and 3rd GLJPanel until the 1st GLJPanel 's GLContext becomes created.
 * </p>
 * <p>
 * Above method allows random creation of the 1st GLJPanel, which triggers
 * creation of the <i>dependent</i> other GLJPanel sharing it's GLContext.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2AWT3b extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
            width  = 256;
            height = 256;
        } else {
            setTestSupported(false);
        }
    }

    protected GLJPanel createGLJPanel(final Frame frame, final int x, final int y, final GearsES2 gears) throws InterruptedException {
        final GLJPanel glCanvas = new GLJPanel(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.addGLEventListener(gears);
        frame.add(glCanvas);
        frame.setLocation(x, y);
        frame.setSize(width, height);
        frame.setTitle("AWT GLJPanel Shared Gears Test: "+x+"/"+y+" shared true");
        return glCanvas;
    }

    @Test
    public void test01SyncedOneAnimator() throws InterruptedException, InvocationTargetException {
        final Frame f1 = new Frame();
        final Animator animator = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        final GLJPanel c1 = createGLJPanel(f1, 0, 0, g1);
        animator.add(c1);

        final Frame f2 = new Frame();
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLJPanel c2 = createGLJPanel(f2, f1.getX()+width,
                                           f1.getY()+0, g2);
        c2.setSharedAutoDrawable(c1);
        animator.add(c2);

        final Frame f3 = new Frame();
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLJPanel c3 = createGLJPanel(f3, f1.getX()+0,
                                           f1.getY()+height, g3);
        c3.setSharedAutoDrawable(c1);
        animator.add(c3);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f2.setVisible(true); // shall wait until f1 is ready
                f1.setVisible(true); // master ..
                f3.setVisible(true); // shall wait until f1 is ready
            } } );
        animator.start(); // kicks off GLContext .. and hence gears of f2 + f3 completion

        Thread.sleep(1000/60*10); // wait ~10 frames giving a chance to create (blocking until master share is valid)

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

        final GLContext ctx1 = c1.getContext();
        final GLContext ctx2 = c2.getContext();
        final GLContext ctx3 = c3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    f3.dispose();
                    f2.dispose();
                    f1.dispose();
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
        final Frame f1 = new Frame();
        final Animator a1 = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        final GLJPanel c1 = createGLJPanel(f1, 0, 0, g1);
        a1.add(c1);
        a1.start();
        // f1.setVisible(true); // we do this post f2 .. to test pending creation!

        final Frame f2 = new Frame();
        final Animator a2 = new Animator();
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLJPanel c2 = createGLJPanel(f2, f1.getX()+width, f1.getY()+0, g2);
        c2.setSharedAutoDrawable(c1);
        a2.add(c2);
        a2.start();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f2.setVisible(true);
            } } );

        Thread.sleep(200); // wait a while ..

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f1.setVisible(true); // test pending creation of f2
            } } );

        final Frame f3 = new Frame();
        final Animator a3 = new Animator();
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLJPanel c3 = createGLJPanel(f3, f1.getX()+0, f1.getY()+height, g3);
        c3.setSharedAutoDrawable(c1);
        a3.add(c3);
        a3.start();
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f3.setVisible(true);
            } } );

        Thread.sleep(1000/60*10); // wait ~10 frames giving a chance to create (blocking until master share is valid)

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

        final GLContext ctx1 = c1.getContext();
        final GLContext ctx2 = c2.getContext();
        final GLContext ctx3 = c3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        a1.stop();
        Assert.assertEquals(false, a1.isAnimating());
        a2.stop();
        Assert.assertEquals(false, a2.isAnimating());
        a3.stop();
        Assert.assertEquals(false, a3.isAnimating());

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                try {
                    f3.dispose();
                    f2.dispose();
                    f1.dispose();
                } catch (final Throwable t) {
                    throw new RuntimeException(t);
                }
            }});

        Assert.assertTrue(AWTRobotUtil.waitForRealized(c1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(c3, false));
    }

    static long duration = 1000; // ms

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
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2AWT3b.class.getName());
    }
}
