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

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLOffscreenAutoDrawable;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;

import java.awt.Frame;
import java.lang.reflect.InvocationTargetException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestSharedContextListAWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    GLOffscreenAutoDrawable sharedDrawable;
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
        sharedDrawable = GLDrawableFactory.getFactory(glp).createOffscreenAutoDrawable(null, caps, null, width, height);
        Assert.assertNotNull(sharedDrawable);
        sharedGears = new Gears();
        Assert.assertNotNull(sharedGears);
        sharedDrawable.addGLEventListener(sharedGears);
        // init and render one frame, which will setup the Gears display lists
        sharedDrawable.display();
    }

    private void releaseShared() {
        Assert.assertNotNull(sharedDrawable);
        sharedDrawable.destroy();
    }

    protected void setFrameTitle(final Frame f, final boolean useShared)
            throws InterruptedException, InvocationTargetException {
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                f.setTitle("Shared Gears AWT Test: "+f.getX()+"/"+f.getY()+" shared "+useShared);
            }
        });
    }

    protected GLCanvas runTestGL(final Frame frame, final Animator animator, final int x, final int y, final boolean useShared, final boolean vsync)
            throws InterruptedException, InvocationTargetException
    {
        final GLCanvas glCanvas = new GLCanvas(caps);
        Assert.assertNotNull(glCanvas);
        glCanvas.setSharedAutoDrawable(sharedDrawable);
        frame.add(glCanvas);
        frame.setLocation(x, y);
        frame.setSize(width, height);

        final Gears gears = new Gears(vsync ? 1 : 0);
        if(useShared) {
            gears.setSharedGears(sharedGears);
        }
        glCanvas.addGLEventListener(gears);

        animator.add(glCanvas);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(true);
            } } );
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glCanvas, true));

        return glCanvas;
    }

    @Test
    public void test01() throws InterruptedException, InvocationTargetException {
        initShared();
        final Frame f1 = new Frame();
        final Frame f2 = new Frame();
        final Frame f3 = new Frame();
        final Animator animator = new Animator();

        final GLCanvas glc1 = runTestGL(f1, animator, 0,     0,      true, false);
        final int x0 = f1.getX();
        final int y0 = f1.getY();

        final GLCanvas glc2 = runTestGL(f2, animator,
                                        x0+width,
                                        y0+0,
                                        true, false);

        final GLCanvas glc3 = runTestGL(f3, animator,
                                        x0+0,
                                        y0+height,
                                        false, true);

        setFrameTitle(f1, true);
        setFrameTitle(f2, true);
        setFrameTitle(f3, false);

        animator.setUpdateFPSFrames(1, null);
        animator.start();
        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        animator.stop();

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    try {
                        f1.dispose();
                        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc1, false));
                        f2.dispose();
                        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc2, false));
                        f3.dispose();
                        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc3, false));
                    } catch (final Throwable t) {
                        throw new RuntimeException(t);
                    }
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }

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
        org.junit.runner.JUnitCore.main(TestSharedContextListAWT.class.getName());
    }
}
