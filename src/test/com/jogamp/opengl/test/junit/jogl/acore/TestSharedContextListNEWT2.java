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

import java.io.IOException;

import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.nativewindow.util.InsetsImmutable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextListNEWT2 extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    GLWindow sharedDrawable;
    Gears sharedGears;

    @BeforeClass
    public static void initClass() {
        if(GLProfile.isAvailable(GLProfile.GL2)) {
            glp = GLProfile.get(GLProfile.GL2);
            Assert.assertNotNull(glp);
            caps = new GLCapabilities(glp);
            Assert.assertNotNull(caps);
            width  = 256;
            height = 256;
        } else {
            setTestSupported(false);
        }
    }

    private void initShared() {
    	sharedDrawable = GLWindow.create(caps);
        Assert.assertNotNull(sharedDrawable);
        sharedGears = new Gears(0);
        Assert.assertNotNull(sharedGears);
        sharedDrawable.addGLEventListener(sharedGears);
        sharedDrawable.setSize(width, height);
        sharedDrawable.setVisible(true);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
    }

    private void releaseShared() {
        Assert.assertNotNull(sharedDrawable);
        sharedDrawable.destroy();
        sharedDrawable = null;
    }

    protected GLWindow runTestGL(final Animator animator, final int x, final int y, final boolean useShared, final boolean vsync) throws InterruptedException {
        final GLWindow glWindow = GLWindow.create(caps);

        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test: "+x+"/"+y+" shared "+useShared);
        if(useShared) {
            glWindow.setSharedAutoDrawable(sharedDrawable);
        }

        glWindow.setSize(width, height);

        final Gears gears = new Gears(vsync ? 1 : 0);
        if(useShared) {
            gears.setSharedGears(sharedGears);
        }
        glWindow.addGLEventListener(gears);

        animator.add(glWindow);
        animator.start();
        glWindow.setVisible(true);

        Assert.assertTrue(AWTRobotUtil.waitForRealized(glWindow, true));
        Assert.assertTrue(AWTRobotUtil.waitForVisible(glWindow, true));
        glWindow.setPosition(x, y);

        return glWindow;
    }

    @Test(timeout=10000)
    public void test01() throws InterruptedException {
        initShared();

        final GLWindow f1 = runTestGL(new Animator(), 0, 0, true, false);
        final InsetsImmutable insets = f1.getInsets();
        final GLWindow f2 = runTestGL(new Animator(), f1.getX()+width+insets.getTotalWidth(),
                                      f1.getY()+0, true, false);
        final GLWindow f3 = runTestGL(new Animator(), f1.getX()+0,
                                      f1.getY()+height+insets.getTotalHeight(), true, false);

        try {
			Thread.sleep(duration);
		} catch(final Exception e) {
			e.printStackTrace();
		}

        f1.destroy();
        f2.destroy();
        f3.destroy();

        // f1.getAnimator().stop();
        // f2.getAnimator().stop();
        // f3.getAnimator().stop();

        releaseShared();
    }

    static long duration = 2000; // ms

    public static void main(final String args[]) throws IOException {
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
        org.junit.runner.JUnitCore.main(TestSharedContextListNEWT2.class.getName());
    }
}
