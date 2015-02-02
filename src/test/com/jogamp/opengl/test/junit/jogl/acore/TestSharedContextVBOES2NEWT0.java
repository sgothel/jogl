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
import com.jogamp.opengl.GLAutoDrawable;
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
 * This is achieved by relying on the sequential creation
 * of the 3 GLWindows with their GLDrawable and GLContext.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2NEWT0 extends UITestCase {
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

    protected GLWindow runTestGL(final Animator animator, final int x, final int y, final GearsES2 gears, final GLAutoDrawable sharedDrawable) throws InterruptedException {
        final boolean useShared = null != sharedDrawable;
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setPosition(x, y);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared "+useShared);
        if(useShared) {
            glWindow.setSharedAutoDrawable(sharedDrawable);
        }
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(gears);

        animator.add(glWindow);
        glWindow.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, true));
        glWindow.display();
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(glWindow, true));
        Assert.assertTrue("Gears not initialized", gears.waitForInit(true));

        return glWindow;
    }

    @Test
    public void test01CommonAnimatorSharedCopyBuffer() throws InterruptedException {
        testCommonAnimatorSharedImpl(false);
    }
    @Test
    public void test02CommonAnimatorMapBuffer() throws InterruptedException {
        testCommonAnimatorSharedImpl(true);
    }
    private void testCommonAnimatorSharedImpl(final boolean useMappedBuffers) throws InterruptedException {
        final Animator animator = new Animator();

        //
        // 1st
        //
        final GearsES2 g1 = new GearsES2(0);
        g1.setUseMappedBuffers(useMappedBuffers);
        g1.setValidateBuffers(true);
        final GLWindow f1 = runTestGL(animator, 0, 0, g1, null);
        final GLContext ctx1 = f1.getContext();
        Assert.assertTrue("Ctx is shared before shared creation", !ctx1.isShared());
        final InsetsImmutable insets = f1.getInsets();

        MiscUtils.dumpSharedGLContext("XXX-C-1.1", ctx1);

        //
        // 2nd
        //
        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLWindow f2 = runTestGL(animator, f1.getX()+width+insets.getTotalWidth(),
                                                f1.getY()+0, g2, f1);
        final GLContext ctx2 = f2.getContext();
        Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
        Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());

        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-2.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-2.2", ctx2);

            Assert.assertEquals("Ctx1 has unexpected number of created shares", 1, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 1, ctx2Shares.size());
        }

        //
        // 3rd
        //
        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLWindow f3 = runTestGL(animator, f1.getX()+0,
                                                f1.getY()+height+insets.getTotalHeight(), g3, f1);

        final GLContext ctx3 = f3.getContext();
        Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
        Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
        Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());

        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-C-3.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-C-3.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-C-3.3", ctx3);

            Assert.assertEquals("Ctx1 has unexpected number of created shares", 2, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 2, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        animator.start();

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }

        f3.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f3, false));
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-D-0.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-D-0.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-D-0.3", ctx3);

            Assert.assertTrue("Ctx1 is shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 1, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 1, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 2, ctx3Shares.size());
        }
        try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

        f2.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, false));
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-D-1.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-D-1.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-D-1.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 0, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 1, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 1, ctx3Shares.size());
        }
        try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

        f1.destroy();
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, false));
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            final List<GLContext> ctx2Shares = ctx2.getCreatedShares();
            final List<GLContext> ctx3Shares = ctx3.getCreatedShares();
            MiscUtils.dumpSharedGLContext("XXX-D-2.1", ctx1);
            MiscUtils.dumpSharedGLContext("XXX-D-2.2", ctx2);
            MiscUtils.dumpSharedGLContext("XXX-D-2.3", ctx3);

            Assert.assertTrue("Ctx1 is not shared", !ctx1.isShared());
            Assert.assertTrue("Ctx2 is not shared", !ctx2.isShared());
            Assert.assertTrue("Ctx3 is not shared", !ctx3.isShared());
            Assert.assertEquals("Ctx1 has unexpected number of created shares", 0, ctx1Shares.size());
            Assert.assertEquals("Ctx2 has unexpected number of created shares", 0, ctx2Shares.size());
            Assert.assertEquals("Ctx3 has unexpected number of created shares", 0, ctx3Shares.size());
        }
        try { Thread.sleep(durationPostDestroy); } catch(final Exception e) { e.printStackTrace(); }

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
    }

    static long duration = 1000; // ms
    static long durationPostDestroy = 1000; // ms - ~60 frames post destroy

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
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT0.class.getName());
    }
}
