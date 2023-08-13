/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

import jogamp.opengl.GLContextShareSet;

import com.jogamp.opengl.test.junit.util.GLTestUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.NewtTestUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Analyze Bug 1312: Test potential memory leak in {@link GLContextShareSet}
 * due to its usage of hard references.
 * <p>
 * Test uses the asynchronous one animator per instance and GL buffer mapping path only.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES2NEWT5 extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static final int width=128, height=128;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES2)) {
            glp = GLProfile.get(GLProfile.GL2ES2);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
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
    public void test01CleanDtorOrder() throws InterruptedException {
        asyncEachAnimator(true, 3);
    }

    // @Test
    public void test02DirtyDtorOrder() throws InterruptedException {
        asyncEachAnimator(false, 3);
    }

    public void asyncEachAnimator(final boolean destroyCleanOrder, final int loops) throws InterruptedException {
        // master
        final Animator a1 = new Animator(0 /* w/o AWT */);
        final GearsES2 g1 = new GearsES2(0);
        g1.setVerbose(false);
        g1.setSyncObjects(g1); // this is master, since rendered we must use it as sync
        g1.setUseMappedBuffers(true);
        g1.setValidateBuffers(true);
        final GLWindow f1 = createGLWindow(0, 0, g1);
        a1.add(f1);
        a1.start();

        f1.setVisible(true);
        Assert.assertTrue(NewtTestUtil.waitForRealized(f1, true, null));
        Assert.assertTrue(NewtTestUtil.waitForVisible(f1, true, null));
        Assert.assertTrue(GLTestUtil.waitForContextCreated(f1, true, null));
        Assert.assertTrue("Gears1 not initialized", g1.waitForInit(true));
        System.err.println("XXX-0-C-M - GLContextShareSet.Map");
        GLContextShareSet.printMap(System.err);
        final InsetsImmutable insets = f1.getInsets();
        final GLContext ctx1 = f1.getContext();

        // slaves
        final int slaveCount = 10;
        final int slavesPerRow=4;
        for(int j=0; j<loops; j++) {
            final Animator[] sa = new Animator[slaveCount];
            final GearsES2[] sg = new GearsES2[slaveCount];
            final GLWindow[] sf = new GLWindow[slaveCount];
            final GLContext[] sc = new GLContext[slaveCount];
            for(int i=0; i<slaveCount; i++) {
                final Animator a2 = new Animator(0 /* w/o AWT */);
                final GearsES2 g2 = new GearsES2(0);
                g2.setVerbose(false);
                g2.setSharedGears(g1); // also uses master g1 as sync, if required
                final int y = 1 + i/slavesPerRow;
                final int x = i%slavesPerRow;
                final GLWindow f2 = createGLWindow(width*x,
                                                   insets.getTotalHeight()+height*y, g2);
                f2.setUndecorated(true);
                f2.setSharedAutoDrawable(f1);
                a2.add(f2);
                a2.start();
                f2.setVisible(true);

                Assert.assertTrue(NewtTestUtil.waitForRealized(f2, true, null));
                Assert.assertTrue(NewtTestUtil.waitForVisible(f2, true, null));
                Assert.assertTrue(GLTestUtil.waitForContextCreated(f2, true, null));
                Assert.assertTrue("Gears2 not initialized", g2.waitForInit(true));
                sa[i] = a2;
                sg[i] = g2;
                sf[i] = f2;
                sc[i] = f2.getContext();
            }

            {
                final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
                Assert.assertTrue("Gears1 is shared", !g1.usesSharedGears());
                Assert.assertTrue("Ctx1 is not shared", ctx1.isShared());
                Assert.assertEquals("Ctx1 has unexpected number of created shares", slaveCount, ctx1Shares.size());
                Assert.assertEquals("Ctx1 Master Context is different", ctx1, ctx1.getSharedMaster());
            }
            for(int i=0; i<slaveCount; i++) {
                final List<GLContext> ctxSShares = sc[i].getCreatedShares();
                Assert.assertTrue("Gears2 is not shared", sg[i].usesSharedGears());
                Assert.assertTrue("CtxS["+i+"] is not shared", sc[i].isShared());
                Assert.assertEquals("CtxS["+i+"] has unexpected number of created shares", slaveCount, ctxSShares.size());
                Assert.assertEquals("CtxS["+i+"] Master Context is different", ctx1, sc[i].getSharedMaster());
            }
            System.err.println("XXX-"+j+"-C - GLContextShareSet.Map");
            GLContextShareSet.printMap(System.err);

            try {
                Thread.sleep(duration);
            } catch(final Exception e) {
                e.printStackTrace();
            }

            if( destroyCleanOrder ) {
                System.err.println("XXX Destroy in clean order");
                for(int i=slaveCount-1; 0<=i; i--) {
                    sa[i].stop();
                    sf[i].destroy();
                    Assert.assertTrue(NewtTestUtil.waitForVisible(sf[i], false, null));
                    Assert.assertTrue(NewtTestUtil.waitForRealized(sf[i], false, null));
                }
            } else {
                System.err.println("XXX Destroy in creation order (but Master) - Driver Impl. May trigger driver Bug i.e. not postponing GL ctx destruction after releasing all refs.");
                for(int i=0; i<slaveCount; i++) {
                    sa[i].stop();
                    sf[i].destroy();
                    Assert.assertTrue(NewtTestUtil.waitForVisible(sf[i], false, null));
                    Assert.assertTrue(NewtTestUtil.waitForRealized(sf[i], false, null));
                }
            }
            System.err.println("XXX-"+j+"-X-SX1 - GLContextShareSet.Map");
            GLContextShareSet.printMap(System.err);
            Assert.assertEquals("GLContextShareSet ctx1.createdCount is not 1", 1, GLContextShareSet.getCreatedShareCount(ctx1));
            Assert.assertEquals("GLContextShareSet ctx1.destroyedCount is not slaveCount", slaveCount, GLContextShareSet.getDestroyedShareCount(ctx1));
            for(int i=0; i<slaveCount; i++) {
                sa[i] = null;
                sg[i] = null;
                sf[i] = null;
                sc[i] = null;
            }
            {
                // Ensure nulled objects got destroyed and taken from the GLContextShareSet map.
                System.gc();
                try { Thread.sleep(100); } catch (final InterruptedException ie) {}
                System.gc();
                try { Thread.sleep(100); } catch (final InterruptedException ie) {}
            }
            System.err.println("XXX-"+j+"-X-SX2 - GLContextShareSet.Map");
            GLContextShareSet.printMap(System.err);
            Assert.assertEquals("GLContextShareSet ctx1.createdCount is not 1", 1, GLContextShareSet.getCreatedShareCount(ctx1));
            Assert.assertEquals("GLContextShareSet ctx1.destroyedCount is not 0", 0, GLContextShareSet.getDestroyedShareCount(ctx1));
        }

        // stop master
        System.gc();
        System.err.println("XXX-X-X-M1 - GLContextShareSet.Map");
        GLContextShareSet.printMap(System.err);
        Assert.assertEquals("GLContextShareSet ctx1.createdCount is not 1", 1, GLContextShareSet.getCreatedShareCount(ctx1));
        Assert.assertEquals("GLContextShareSet ctx1.destroyedCount is not 0", 0, GLContextShareSet.getDestroyedShareCount(ctx1));
        Assert.assertEquals("GLContextShareSet is not 1", 1, GLContextShareSet.getSize());
        a1.stop();
        f1.destroy();
        System.err.println("XXX-X-X-M2 - GLContextShareSet.Map");
        GLContextShareSet.printMap(System.err);
        Assert.assertTrue(NewtTestUtil.waitForVisible(f1, false, null));
        Assert.assertTrue(NewtTestUtil.waitForRealized(f1, false, null));
        Assert.assertEquals("GLContextShareSet ctx1.createdCount is not 0", 0, GLContextShareSet.getCreatedShareCount(ctx1));
        Assert.assertEquals("GLContextShareSet ctx1.destroyedCount is not 0", 0, GLContextShareSet.getDestroyedShareCount(ctx1));
        {
            final List<GLContext> ctx1Shares = ctx1.getCreatedShares();
            Assert.assertFalse("Ctx1 is still shared", ctx1.isShared());
            Assert.assertEquals("Ctx1 still has created shares", 0, ctx1Shares.size());
            Assert.assertEquals("Ctx1 Master Context is not null", null, ctx1.getSharedMaster());
        }
        Assert.assertEquals("GLContextShareSet is not 0", 0, GLContextShareSet.getSize());
    }

    static long duration = 1000; // ms - ~60 frames
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
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES2NEWT5.class.getName());
    }
}
