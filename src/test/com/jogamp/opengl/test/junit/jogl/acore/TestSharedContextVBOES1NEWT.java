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

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.GLAutoDrawableDelegate;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es1.GearsES1;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

/**
 * Sharing the VBO of 3 GearsES1 instances, each in their own GLWindow.
 * <p>
 * This is achieved by creating a <i>master</i> GLContext to an offscreen invisible GLAutoDrawable,
 * which is then shared by the 3 GLContext of the three GLWindow instances.
 * </p>
 * <p>
 * The original VBO is created by attaching a GearsES1 instance to
 * the <i>master</i> GLAutoDrawable and initializing it.
 * </p>
 * <p>
 * Above method allows random creation of all GLWindow instances.
 * </p>
 * <p>
 * One animator is being used, hence the GLWindow, GLDrawable and GLContext
 * creation of all 3 GLWindows is sequential.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextVBOES1NEWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    GLAutoDrawable sharedDrawable;
    GearsES1 sharedGears;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2ES1)) {
            glp = GLProfile.get(GLProfile.GL2ES1);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
            width  = 256;
            height = 256;
        } else {
            setTestSupported(false);
        }
    }

    private void initShared() throws InterruptedException {
        final GLDrawable dummyDrawable = GLDrawableFactory.getFactory(glp).createDummyDrawable(null, true /* createNewDevice */, caps, null);
        dummyDrawable.setRealized(true);
        sharedDrawable = new GLAutoDrawableDelegate(dummyDrawable, null, null, true /*ownDevice*/, null) { };
        Assert.assertNotNull(sharedDrawable);
        Assert.assertTrue(AWTRobotUtil.waitForRealized(sharedDrawable, true));

        sharedGears = new GearsES1();
        Assert.assertNotNull(sharedGears);
        sharedDrawable.addGLEventListener(sharedGears);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
        final GLContext ctxM = sharedDrawable.getContext();
        Assert.assertTrue("Master ctx not created", AWTRobotUtil.waitForContextCreated(sharedDrawable, true));
        Assert.assertTrue("Master Ctx is shared before shared creation", !ctxM.isShared());
        Assert.assertTrue("Master Gears is shared", !sharedGears.usesSharedGears());
    }

    private void releaseShared() {
        Assert.assertNotNull(sharedDrawable);
        sharedDrawable.destroy();
    }

    protected GLWindow runTestGL(final Animator animator, final int x, final int y, final boolean useShared, final boolean vsync) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);
        Assert.assertNotNull(glWindow);
        glWindow.setPosition(x, y);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared "+useShared);
        if(useShared) {
            glWindow.setSharedAutoDrawable(sharedDrawable);
        }

        glWindow.setSize(width, height);

        final GearsES1 gears = new GearsES1(vsync ? 1 : 0);
        if(useShared) {
            gears.setSharedGears(sharedGears);
        }
        glWindow.addGLEventListener(gears);

        animator.add(glWindow);

        glWindow.setVisible(true);
        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForContextCreated(glWindow, true));

        MiscUtils.dumpSharedGLContext("Master Context", sharedDrawable.getContext());
        MiscUtils.dumpSharedGLContext("New    Context", glWindow.getContext());
        if( useShared ) {
            Assert.assertEquals("Master Context not shared as expected", true, sharedDrawable.getContext().isShared());
            Assert.assertEquals("Master Context is different", sharedDrawable.getContext(), glWindow.getContext().getSharedMaster());
        } else {

        }
        Assert.assertEquals("New    Context not shared as expected", useShared, glWindow.getContext().isShared());
        Assert.assertEquals("Gears is not shared as expected", useShared, gears.usesSharedGears());
        return glWindow;
    }

    @Test
    public void test01() throws InterruptedException {
        initShared();
        final Animator animator = new Animator();
        final GLWindow f1 = runTestGL(animator, 0, 0, true, false);
        final InsetsImmutable insets = f1.getInsets();
        final GLWindow f2 = runTestGL(animator, f1.getX()+width+insets.getTotalWidth(),
                                          f1.getY()+0, true, false);
        final GLWindow f3 = runTestGL(animator, f1.getX()+0,
                                          f1.getY()+height+insets.getTotalHeight(), false, true);
        animator.setUpdateFPSFrames(1, null);
        animator.start();
        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        animator.stop();

        f1.destroy();
        f2.destroy();
        f3.destroy();

        releaseShared();
    }

    static long duration = 500; // ms

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestSharedContextVBOES1NEWT.class.getName());
    }
}
