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

import java.util.List;

import com.jogamp.newt.opengl.GLWindow;

import javax.media.nativewindow.util.InsetsImmutable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

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
 * Sharing the VBO of 3 GearsES2 instances, each in their own GLWindow.
 * <p>
 * This is achieved by using the 1st GLWindow as the <i>master</i>
 * and using the build-in blocking mechanism to postpone creation
 * of the 2nd and 3rd GLWindow until the 1st GLWindow's GLContext becomes created.
 * </p>
 * <p>
 * Above method allows random creation of the 1st GLWindow, which triggers
 * creation of the <i>dependent</i> other GLWindow sharing it's GLContext.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2NEWT3 extends UITestCase {
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

    protected GLWindow createGLWindow(int x, int y, GearsES2 gears) throws InterruptedException {
        GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setPosition(x, y);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared true");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(gears);

        return glWindow;
    }

    @Test
    public void test01SyncedOneAnimatorCleanDtorOrder() throws InterruptedException {
        syncedOneAnimator(true);
    }

    @Test
    public void test02SyncedOneAnimatorDirtyDtorOrder() throws InterruptedException {
        syncedOneAnimator(false);
    }

    public void syncedOneAnimator(boolean destroyCleanOrder) throws InterruptedException {
        final Animator animator = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        animator.add(f1);
        InsetsImmutable insets = f1.getInsets();

        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLWindow f2 = createGLWindow(f1.getX()+width+insets.getTotalWidth(),
                                           f1.getY()+0, g2);
        f2.setSharedAutoDrawable(f1);
        animator.add(f2);

        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLWindow f3 = createGLWindow(f1.getX()+0,
                                           f1.getY()+height+insets.getTotalHeight(), g3);
        f3.setSharedAutoDrawable(f1);
        animator.add(f3);

        f2.setVisible(true); // shall wait until f1 is ready
        f1.setVisible(true); // master ..
        f3.setVisible(true); // shall wait until f1 is ready
        animator.start(); // kicks off GLContext .. and hence gears of f2 + f3 completion

        Thread.sleep(1000/60*10); // wait ~10 frames giving a chance to create (blocking until master share is valid)

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f3, true));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        final GLContext ctx1 = f1.getContext();
        final GLContext ctx2 = f2.getContext();
        final GLContext ctx3 = f3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            System.err.println("XXX-C-3.1:");
            MiscUtils.dumpSharedGLContext(ctx1);
            System.err.println("XXX-C-3.2:");
            MiscUtils.dumpSharedGLContext(ctx2);
            System.err.println("XXX-C-3.3:");
            MiscUtils.dumpSharedGLContext(ctx3);

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
        } catch(Exception e) {
            e.printStackTrace();
        }
        // Stopped animator allows native windowing system 'repaint' event
        // to trigger GLAD 'display'
        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());

        if( destroyCleanOrder ) {
            System.err.println("XXX Destroy in clean order NOW");
            f3.destroy();
            f2.destroy();
            f1.destroy();
        } else {
            System.err.println("XXX Destroy in creation order NOW - Driver Impl. Ma trigger driver Bug i.e. not postponing GL ctx destruction after releasing all refs.");
            f1.destroy();
            f2.destroy();
            f3.destroy();
        }
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));
    }

    @Test
    public void test11ASyncEachAnimatorCleanDtorOrder() throws InterruptedException {
        asyncEachAnimator(true);
    }

    @Test
    public void test12AsyncEachAnimatorDirtyDtorOrder() throws InterruptedException {
        asyncEachAnimator(false);
    }

    public void asyncEachAnimator(boolean destroyCleanOrder) throws InterruptedException {
        final Animator a1 = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        a1.add(f1);
        a1.start();
        // f1.setVisible(true); // we do this post f2 .. to test pending creation!

        InsetsImmutable insets = f1.getInsets();

        final Animator a2 = new Animator();
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLWindow f2 = createGLWindow(f1.getX()+width+insets.getTotalWidth(),
                                           f1.getY()+0, g2);
        f2.setSharedAutoDrawable(f1);
        a2.add(f2);
        a2.start();
        f2.setVisible(true);

        f1.setVisible(true); // test pending creation of f2

        final Animator a3 = new Animator();
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLWindow f3 = createGLWindow(f1.getX()+0,
                                           f1.getY()+height+insets.getTotalHeight(), g3);
        f3.setSharedAutoDrawable(f1);
        a3.add(f3);
        a3.start();
        f3.setVisible(true);

        Thread.sleep(1000/60*10); // wait ~10 frames giving a chance to create (blocking until master share is valid)

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f3, true));
        Assert.assertTrue("Gears3 not initialized", g3.waitForInit(true));

        final GLContext ctx1 = f1.getContext();
        final GLContext ctx2 = f2.getContext();
        final GLContext ctx3 = f3.getContext();
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            System.err.println("XXX-C-3.1:");
            MiscUtils.dumpSharedGLContext(ctx1);
            System.err.println("XXX-C-3.2:");
            MiscUtils.dumpSharedGLContext(ctx2);
            System.err.println("XXX-C-3.3:");
            MiscUtils.dumpSharedGLContext(ctx3);

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
        } catch(Exception e) {
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

        if( destroyCleanOrder ) {
            System.err.println("XXX Destroy in clean order NOW");
            f3.destroy();
            f2.destroy();
            f1.destroy();
        } else {
            System.err.println("XXX Destroy in creation order NOW - Driver Impl. May trigger driver Bug i.e. not postponing GL ctx destruction after releasing all refs.");
            f1.destroy();
            f2.destroy();
            f3.destroy();
        }
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));
    }

    static long duration = 1000; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        /**
        BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));
        System.err.println("Press enter to continue");
        System.err.println(stdin.readLine()); */
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT3.class.getName());
    }
}
