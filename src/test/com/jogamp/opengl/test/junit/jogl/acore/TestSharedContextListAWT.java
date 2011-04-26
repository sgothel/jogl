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

import javax.media.opengl.FPSCounter;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.gears.Gears;
import com.jogamp.opengl.test.junit.util.AWTRobotUtil;

import java.awt.Frame;
import javax.swing.SwingUtilities;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class TestSharedContextListAWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;
    GLPbuffer sharedDrawable;
    Gears sharedGears;

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton(true);
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        width  = 512;
        height = 512;
    }

    private void initShared() {
        sharedDrawable = GLDrawableFactory.getFactory(glp).createGLPbuffer(null, caps, null, width, height, null);
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
    protected Frame createFrame(int x, int y, boolean useShared) {
        return new Frame("Shared Gears AWT Test: "+x+"/"+y+" shared "+useShared);
    }

    protected GLCanvas runTestGL(final Frame frame, final Animator animator, final int x, final int y, final boolean useShared)
            throws InterruptedException
    {
        final GLCanvas glCanvas = new GLCanvas(caps, useShared ? sharedDrawable.getContext() : null);        
        Assert.assertNotNull(glCanvas);
        frame.add(glCanvas);
        frame.setLocation(x, y);
        frame.setSize(width, height);
        
        Gears gears = new Gears();
        if(useShared) {
            gears.setGears(sharedGears.getGear1(), sharedGears.getGear2(), sharedGears.getGear3());
        }
        glCanvas.addGLEventListener(gears);

        animator.add(glCanvas);

        frame.setVisible(true);
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glCanvas, true));

        return glCanvas;
    }

    @Test
    public void test01() throws InterruptedException {
        initShared();

        Frame f1 = createFrame(0, 0, true);
        Frame f2 = createFrame(width, 0, true);
        Frame f3 = createFrame(0, height, false);

        Animator animator = new Animator();

        GLCanvas glc1 = runTestGL(f1, animator, 0,     0,      true);
        GLCanvas glc2 = runTestGL(f2, animator, width, 0,      true);
        GLCanvas glc3 = runTestGL(f3, animator, 0,     height, false);

        animator.setUpdateFPSFrames(1, null);        
        animator.start();
        while(animator.isAnimating() && animator.getTotalFPSDuration()<duration) {
            Thread.sleep(100);
        }
        animator.stop();

        // here we go again: On AMD/X11 the create/destroy sequence must be the same
        // even though this is agains the chicken/egg logic here ..
        releaseShared();

        f1.dispose();
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc1, false));

        f2.dispose();
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc2, false));

        f3.dispose();
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc3, false));
        
        // see above ..
        //releaseShared();
    }

    static long duration = 500; // ms

    public static void main(String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestSharedContextListAWT.class.getName());
    }
}
