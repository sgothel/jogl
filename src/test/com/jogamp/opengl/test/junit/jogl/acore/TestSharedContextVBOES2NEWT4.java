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
import java.util.concurrent.atomic.AtomicBoolean;

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.GLAnimatorControl;
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
 * Test sharing w/ different shared-master context.
 * <p>
 * This is achieved by using the 1st GLWindow as the <i>master</i>
 * and synchronizing via GLSharedContextSetter to postpone creation
 * of the 2nd and 3rd GLWindow until the 1st GLWindow's GLContext becomes created.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2NEWT4 extends UITestCase {
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
    public void test01() throws InterruptedException {
        final Animator animator = new Animator();
        animator.start();

        final GearsES2 g1 = new GearsES2(0);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        animator.add(f1);
        final InsetsImmutable insets = f1.getInsets();

        final GearsES2 g2 = new GearsES2(0);
        g2.setSharedGears(g1);
        final GLWindow f2 = createGLWindow(f1.getX()+width+insets.getTotalWidth(),
                                           f1.getY()+0, g2);
        f2.setSharedAutoDrawable(f1);
        animator.add(f2);
        f2.setVisible(true);

        final GearsES2 g3 = new GearsES2(0);
        g3.setSharedGears(g1);
        final GLWindow f3 = createGLWindow(f1.getX()+0,
                                           f1.getY()+height+insets.getTotalHeight(), g3);
        f3.setSharedAutoDrawable(f2); // Mixed master!
        animator.add(f3);
        final AtomicBoolean gotAnimException = new AtomicBoolean(false);
        final AtomicBoolean gotOtherException = new AtomicBoolean(false);
        animator.setUncaughtExceptionHandler(new GLAnimatorControl.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(final GLAnimatorControl _animator, final GLAutoDrawable _drawable, final Throwable _cause) {
                if( _animator == animator && _drawable == f3 && _cause instanceof RuntimeException ) {
                    System.err.println("Caught expected exception: "+_cause.getMessage());
                    gotAnimException.set(true);
                } else {
                    System.err.println("Caught unexpected exception: "+_cause.getMessage());
                    _cause.printStackTrace();
                    gotOtherException.set(true);
                }
            }
        });
        f3.setVisible(true);

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f1, false));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f2, false));

        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(f3, false));

        f1.setVisible(true); // kick off f1 GLContext .. and hence allow f2 + f3 creation

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

        Assert.assertFalse("Unexpected exception (animator) caught", gotAnimException.get());
        Assert.assertFalse("Unexpected exception (other) caught", gotOtherException.get());

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
            Assert.assertEquals("Ctx3 Master Context is different", ctx2, ctx3.getSharedMaster()); // Mixed master!
        }

        Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
        Assert.assertTrue("Gears2 is not shared", g2.usesSharedGears());
        Assert.assertTrue("Gears3 is not shared", g3.usesSharedGears());

        try {
            Thread.sleep(duration);
        } catch(final Exception e) {
            e.printStackTrace();
        }
        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());

        System.err.println("XXX Destroy in clean order NOW");
        f3.destroy();
        f2.destroy();
        f1.destroy();

        Assert.assertTrue(AWTRobotUtil.waitForVisible(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f1, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f2, false));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(f3, false));
        Assert.assertTrue(AWTRobotUtil.waitForRealized(f3, false));
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
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT4.class.getName());
    }
}
