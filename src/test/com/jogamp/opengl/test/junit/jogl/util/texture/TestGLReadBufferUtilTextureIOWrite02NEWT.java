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

import com.jogamp.common.util.InterruptSource;
import com.jogamp.newt.opengl.GLWindow;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.jogl.offscreen.WindowUtilNEWT;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBufferUtilTextureIOWrite02NEWT extends UITestCase {
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

    private void testWritePNGWithResizeImpl(final boolean offscreen) throws InterruptedException {
        final GLReadBufferUtil screenshot = new GLReadBufferUtil(true, false);
        final GLCapabilities caps2 = offscreen ? WindowUtilNEWT.fixCaps(caps, false, true, false) : caps;
        final GLWindow glWindow = GLWindow.create(caps2);
        Assert.assertNotNull(glWindow);
        glWindow.setTitle("Shared Gears NEWT Test");
        glWindow.setSize(width, height);
        glWindow.addGLEventListener(new GearsES2(1));
        glWindow.addGLEventListener(new GLEventListener() {
            int i=0, dw_old=0, c=0;
            public void init(final GLAutoDrawable drawable) {
                System.err.println("XXX: init");
            }
            public void dispose(final GLAutoDrawable drawable) {
                System.err.println("XXX: dispose");
            }
            public void display(final GLAutoDrawable drawable) {
                final int dw = drawable.getSurfaceWidth();
                final int dh = drawable.getSurfaceHeight();
                final boolean sz_changed = dw_old != dw && dw <= 512;
                final boolean snap;
                if(sz_changed) {
                    c++;
                    snap = c>1; // only snap the 3rd image ..
                } else {
                    snap = false;
                }

                if(snap) {
                    System.err.println("XXX: ["+dw_old+"], "+dw+"x"+dh+", sz_changed "+sz_changed+", snap "+snap);
                    c=0;
                    snapshot(i++, null, drawable.getGL(), screenshot, TextureIO.PNG, null);
                    dw_old = dw;
                    new InterruptSource.Thread() {
                        @Override
                        public void run() {
                            glWindow.setSize(2*dw, 2*dh);
                        } }.start();
                }
            }
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        });
        final Animator animator = new Animator(glWindow);
        animator.setUpdateFPSFrames(60, null);

        glWindow.setVisible(true);
        animator.start();

        while(animator.getTotalFPSFrames() < 50) {
            Thread.sleep(60);
        }

        animator.stop();
        glWindow.destroy();
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
        org.junit.runner.JUnitCore.main(TestGLReadBufferUtilTextureIOWrite02NEWT.class.getName());
    }
}
