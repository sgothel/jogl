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

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLProfile;

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
 * and synchronizing via GLSharedContextSetter to postpone creation
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

    protected GLWindow createGLWindow(final int x, final int y, final GearsES2 gears) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setPosition(x, y);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared true");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(gears);

        return glWindow;
    }

    @Test
    public void test01SyncedOneAnimatorCleanDtorOrderCopyBuffer() throws InterruptedException {
        syncedOneAnimator(true, false);
    }
    @Test
    public void test02SyncedOneAnimatorCleanDtorOrderMapBuffer() throws InterruptedException {
        syncedOneAnimator(true, true);
    }

    @Test
    public void test03SyncedOneAnimatorDirtyDtorOrderCopyBuffer() throws InterruptedException {
        syncedOneAnimator(false, false);
    }
    @Test
    public void test04SyncedOneAnimatorDirtyDtorOrderMapBuffer() throws InterruptedException {
        syncedOneAnimator(false, true);
    }

    public void syncedOneAnimator(final boolean destroyCleanOrder, final boolean useMappedBuffers) throws InterruptedException {
        final Animator animator = new Animator();
        animator.start();

        final GearsES2 g1 = new GearsES2(0);
        g1.setUseMappedBuffers(useMappedBuffers);
        g1.setValidateBuffers(true);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        animator.add(f1);
        final InsetsImmutable insets = f1.getInsets();

        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLWindow f2 = createGLWindow(f1.getX()+width+insets.getTotalWidth(),
                                           f1.getY()+0, g2);
        f2.setSharedAutoDrawable(f1);
        animator.add(f2);
        f2.setVisible(true); // shall wait until f1 is ready

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, false));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, false));

        f1.setVisible(true); // kicks off f1 GLContext .. and hence gears of f2 + f3 completion

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLWindow f3 = createGLWindow(f1.getX()+0,
                                           f1.getY()+height+insets.getTotalHeight(), g3);
        f3.setSharedAutoDrawable(f1);
        animator.add(f3);
        f3.setVisible(true); // shall wait until f1 is ready

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
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
            Assert.assertEquals("Ctx1 Master Context is different", ctx1, ctx1.getSharedMaster());
            Assert.assertEquals("Ctx2 Master Context is different", ctx1, ctx2.getSharedMaster());
            Assert.assertEquals("Ctx3 Master Context is different", ctx1, ctx3.getSharedMaster());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        if( destroyCleanOrder ) {
            System.err.println("XXX Destroy in clean order NOW");
            f3.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
            f2.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
            f1.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
        } else {
            System.err.println("XXX Destroy in creation order NOW - Driver Impl. Ma trigger driver Bug i.e. not postponing GL ctx destruction after releasing all refs.");
            animator.pause();
            f1.destroy();
            animator.resume();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

            animator.pause();
            f2.destroy();
            animator.resume();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

            f3.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
        }
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));

        animator.stop();
    }

    @Test
    public void test11ASyncEachAnimatorCleanDtorOrderCopyBuffer() throws InterruptedException {
        asyncEachAnimator(true, false);
    }
    @Test
    public void test12ASyncEachAnimatorCleanDtorOrderMapBuffer() throws InterruptedException {
        asyncEachAnimator(true, true);
    }

    @Test
    public void test13AsyncEachAnimatorDirtyDtorOrderCopyBuffers() throws InterruptedException {
        asyncEachAnimator(false, false);
    }
    @Test
    public void test14AsyncEachAnimatorDirtyDtorOrderMapBuffers() throws InterruptedException {
        asyncEachAnimator(false, true);
    }

    public void asyncEachAnimator(final boolean destroyCleanOrder, final boolean useMappedBuffers) throws InterruptedException {
        final Animator a1 = new Animator();
        final GearsES2 g1 = new GearsES2(0);
        g1.setSyncObjects(g1); // this is master, since rendered we must use it as sync
        g1.setUseMappedBuffers(useMappedBuffers);
        g1.setValidateBuffers(true);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        a1.add(f1);
        a1.start();

        final InsetsImmutable insets = f1.getInsets();

        final Animator a2 = new Animator();
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1); // also uses master g1 as sync, if required
        final GLWindow f2 = createGLWindow(f1.getX()+width+insets.getTotalWidth(),
                                           f1.getY()+0, g2);
        f2.setSharedAutoDrawable(f1);
        a2.add(f2);
        a2.start();
        f2.setVisible(true);

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, false));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, false));

        f1.setVisible(true); // test pending creation of f2

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, true));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, true));
        Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));

        final Animator a3 = new Animator();
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1); // also uses master g1 as sync, if required
        final GLWindow f3 = createGLWindow(f1.getX()+0,
                                           f1.getY()+height+insets.getTotalHeight(), g3);
        f3.setSharedAutoDrawable(f1);
        a3.add(f3);
        a3.start();
        f3.setVisible(true);

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
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
            Assert.assertEquals("Ctx1 Master Context is different", ctx1, ctx1.getSharedMaster());
            Assert.assertEquals("Ctx2 Master Context is different", ctx1, ctx2.getSharedMaster());
            Assert.assertEquals("Ctx3 Master Context is different", ctx1, ctx3.getSharedMaster());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        if( destroyCleanOrder ) {
            System.err.println("XXX Destroy in clean order NOW");
            a3.stop();
            f3.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
            a2.stop();
            f2.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
            a1.stop();
            f1.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
        } else {
            System.err.println("XXX Destroy in creation order NOW - Driver Impl. May trigger driver Bug i.e. not postponing GL ctx destruction after releasing all refs.");
            a1.stop();
            a2.pause();
            a3.pause();
            f1.destroy();
            a2.resume();
            a3.resume();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

            a2.stop();
            a3.pause();
            f2.destroy();
            a3.resume();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

            a3.stop();
            f3.destroy();
            try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }
        }
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));
    }

    static long duration = 1000; // ms
    static long durationPostDestroy = 1000; // ms - ~60 frames post destroy
    static boolean mainRun = false;

    public static void main(final String args[]) {
        mainRun = true;
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
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT3.class.getName());
    }
}
