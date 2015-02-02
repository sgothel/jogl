/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.test.junit.jogl.util.texture;

import java.awt.Dimension;
import java.awt.Frame;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;

import jogamp.nativewindow.jawt.JAWTUtil;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.test.junit.util.AWTRobotUtil;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBufferUtilTextureIOWrite01AWT extends UITestCase {
    static GLProfile glp;
    static GLCapabilities caps;
    static int width, height;

    @BeforeClass
    public static void initClass() {
        glp = GLProfile.getDefault();
        Assert.assertNotNull(glp);
        caps = new GLCapabilities(glp);
        Assert.assertNotNull(caps);
        caps.setAlphaBits(1); // req. alpha channel
        width  = 256;
        height = 256;
    }

    protected void testWritePNG_Impl(final boolean offscreenLayer) throws InterruptedException {
        final GLReadBufferUtil screenshotRGB = new GLReadBufferUtil(false, false);
        final GLReadBufferUtil screenshotRGBA = new GLReadBufferUtil(true, false);

        if(!offscreenLayer && JAWTUtil.isOffscreenLayerRequired()) {
            System.err.println("onscreen layer n/a");
            return;
        }
        if(offscreenLayer && !JAWTUtil.isOffscreenLayerSupported()) {
            System.err.println("offscreen layer n/a");
            return;
        }
        final GLCanvas glc = new GLCanvas(caps);
        glc.setShallUseOffscreenLayer(offscreenLayer); // trigger offscreen layer - if supported
        final Dimension glc_sz = new Dimension(width, height);
        glc.setMinimumSize(glc_sz);
        glc.setPreferredSize(glc_sz);
        final Frame frame = new Frame(getSimpleTestName("."));
        Assert.assertNotNull(frame);
        frame.add(glc);

        glc.setSize(width, height);
        glc.addGLEventListener(new GearsES2(1));
        glc.addGLEventListener(new GLEventListener() {
            int f = 0;
            public void init(final GLAutoDrawable drawable) {}
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                snapshot(f, null, drawable.getGL(), screenshotRGBA, TextureIO.PNG, null);
                snapshot(f, null, drawable.getGL(),  screenshotRGB, TextureIO.PNG, null);
                f++;
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });

        final Animator animator = new Animator(glc);
        animator.setUpdateFPSFrames(60, null);

        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.pack();
                    frame.setVisible(true);
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        Assert.assertEquals(true, AWTRobotUtil.waitForRealized(glc, true));
        Assert.assertEquals(true, AWTRobotUtil.waitForVisible(glc, true));
        Assert.assertEquals(JAWTUtil.isOffscreenLayerSupported() && offscreenLayer,
                            glc.isOffscreenLayerSurfaceEnabled());
        animator.start();

        while(animator.getTotalFPSFrames() < 2) {
            Thread.sleep(60);
        }

        animator.stop();
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.remove(glc);
                    frame.dispose();
                }});
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
    }

    @Test
    public void testOnscreenWritePNG() throws InterruptedException {
        testWritePNG_Impl(false);
    }

    @Test
    public void testOffscreenWritePNG() throws InterruptedException {
        testWritePNG_Impl(true);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite01AWT.class.getName());
    }
}
