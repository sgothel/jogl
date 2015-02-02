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
import com.jogamp.opengl.Threading;
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
public class TestGLReadBufferUtilTextureIOWrite02AWT extends UITestCase {
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
        width  = 64;
        height = 64;
    }

    protected void testWritePNGWithResizeImpl(final boolean offscreenLayer) throws InterruptedException {
        if(!offscreenLayer && JAWTUtil.isOffscreenLayerRequired()) {
            System.err.println("onscreen layer n/a");
            return;
        }
        if(offscreenLayer && !JAWTUtil.isOffscreenLayerSupported()) {
            System.err.println("offscreen layer n/a");
            return;
        }
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLCanvas glc = new GLCanvas(caps);
        glc.setShallUseOffscreenLayer(offscreenLayer); // trigger offscreen layer - if supported
        final Dimension glc_sz = new Dimension(width, height);
        glc.setMinimumSize(glc_sz);
        glc.setPreferredSize(glc_sz);
        glc.setSize(glc_sz);
        final Frame frame = new Frame(getSimpleTestName("."));
        Assert.assertNotNull(frame);
        frame.add(glc);

        glc.addGLEventListener(new GearsES2(1));
        glc.addGLEventListener(new GLEventListener() {
            int i=0, fw_old=0, dw_old=0, c=0;
            public void init(final GLAutoDrawable drawable) {}
            public void dispose(final GLAutoDrawable drawable) {}
            public void display(final GLAutoDrawable drawable) {
                final int fw = frame.getWidth();
                final int fh = frame.getHeight();
                final int dw = drawable.getSurfaceWidth();
                final int dh = drawable.getSurfaceHeight();
                final boolean sz_changed = fw_old != fw && dw_old != dw && dw <= 512; // need to check both sizes [frame + drawable], due to async resize of AWT!
                final boolean snap;
                if(sz_changed) {
                    c++;
                    snap = c>3; // only snap the 3rd image ..
                } else {
                    snap = false;
                }

                if(snap) {
                    System.err.println("XXX: ["+fw_old+", "+dw_old+"], "+fw+"x"+fh+", "+dw+"x"+dh+", sz_changed "+sz_changed+", snap "+snap);
                    c=0;
                    snapshot(i++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                    dw_old = dw;
                    fw_old = fw;
                    Threading.invoke(true, new Runnable() {
                        public void run() {
                            final Dimension new_sz = new Dimension(2*dw, 2*dh);
                            glc.setMinimumSize(new_sz);
                            glc.setPreferredSize(new_sz);
                            glc.setSize(new_sz);
                            frame.pack();
                            frame.validate();
                        } }, glc.getTreeLock());
                }
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

        while(animator.getTotalFPSFrames() < 30) {
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
    public void testOnscreenWritePNGWithResize() throws InterruptedException {
        testWritePNGWithResizeImpl(false);
    }

    @Test
    public void testOffscreenWritePNGWithResize() throws InterruptedException {
        testWritePNGWithResizeImpl(true);
    }

    public static void main(final String args[]) {
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite02AWT.class.getName());
    }
}
