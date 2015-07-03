/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.caps;

import java.io.File;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLCapabilitiesImmutable;
import com.jogamp.opengl.GLDrawableFactory;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.fixedfunc.GLMatrixFunc;

import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.image.BufferedImage;

import org.junit.Assert;
import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.util.UITestCase;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestBug605FlippedImageAWT extends UITestCase {
    class FlippedImageTest implements GLEventListener {
        AWTGLReadBufferUtil screenshot;

        public void display(final GLAutoDrawable drawable) {
            final GL2 gl = drawable.getGL().getGL2();

            gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT | GL2.GL_ACCUM_BUFFER_BIT );

            gl.glMatrixMode(GLMatrixFunc.GL_PROJECTION);
            gl.glLoadIdentity();
            gl.glMatrixMode(GLMatrixFunc.GL_MODELVIEW);
            gl.glLoadIdentity();

            // red below
            gl.glColor3f(1, 0, 0);
            gl.glRectf(-1, -1, 1, 0);

            // green above
            gl.glColor3f(0, 1, 0);
            gl.glRectf(-1, 0, 1, 1);
            gl.glFinish();

            final GLCapabilitiesImmutable caps = drawable.getChosenGLCapabilities();
            if(caps.getAccumGreenBits() > 0) {
                gl.glAccum(GL2.GL_ACCUM, 1.0f);
                gl.glAccum(GL2.GL_RETURN, 1.0f);
            }
            gl.glFinish();

            final int width = drawable.getSurfaceWidth();
            final int height = drawable.getSurfaceHeight();

            final String fname = getSnapshotFilename(0, null, caps, width, height, false, TextureIO.PNG, null);
            final BufferedImage image;
            try {
                image = screenshot.readPixelsToBufferedImage(gl, 0, 0, width, height, true /* awtOrientation */);
                screenshot.write(new File(fname));
            } catch (final GLException e) {
                throw e;
            }
            testFlipped(image, width, height);
        }

        public void init(final GLAutoDrawable drawable) {
            final GL gl = drawable.getGL();
            System.err.println("GL_RENDERER: "+gl.glGetString(GL.GL_RENDERER));
            System.err.println("GL_VERSION: "+gl.glGetString(GL.GL_VERSION));
            screenshot = new AWTGLReadBufferUtil(drawable.getGLProfile(), false);
        }
        public void reshape(final GLAutoDrawable glDrawable, final int x, final int y, final int w, final int h) {}
        public void displayChanged(final GLAutoDrawable drawable, final boolean modeChanged, final boolean deviceChanged) {}
        public void dispose(final GLAutoDrawable drawable) {
            screenshot.dispose(drawable.getGL());
        }
    }

    static final int green = 0x0000ff00; // above
    static final int red   = 0x00ff0000; // below

    private void testFlipped(final BufferedImage image, final int width, final int height) {
        // Default origin 0/0 is lower left corner, so is the memory layout
        // However AWT origin 0/0 is upper left corner
        final int below = image.getRGB(0, height-1) & 0x00ffffff;
        System.err.println("below: 0x"+Integer.toHexString(below));

        final int above = image.getRGB(0, 0) & 0x00ffffff;
        System.err.println("above: 0x"+Integer.toHexString(above));

        if (above == green && below == red) {
            System.out.println("Image right side up");
        } else if (above == red && below == green) {
            Assert.assertTrue("Image is flipped", false);
        } else {
            Assert.assertTrue("Error in test", false);
        }
    }

    private void test(final GLCapabilitiesImmutable caps) {

        final GLDrawableFactory glFactory = GLDrawableFactory.getFactory(caps.getGLProfile());
        final GLAutoDrawable glad = glFactory.createOffscreenAutoDrawable(null, caps, null, 256, 256);
        final FlippedImageTest tglel = new FlippedImageTest();
        glad.addGLEventListener(tglel);

        // 1 frame incl. snapshot to memory & file
        glad.display();
        System.err.println("XXX "+glad.getChosenGLCapabilities());
        System.err.println("XXX "+glad.getContext().getGLVersion());

        glad.destroy();
    }

    @Test
    public void test01DefaultFBO() {
        final GLProfile glp = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setFBO(true);
        test(caps);
    }

    @Test
    public void test01StencilFBO() {
        final GLProfile glp = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setStencilBits(8);
        caps.setFBO(true);
        test(caps);
    }

    @Test
    public void test01DefaultPBuffer() {
        final GLProfile glp = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setPBuffer(true);
        test(caps);
    }

    @Test
    public void test01AccumStencilPBuffer() {
        final GLProfile glp = GLProfile.get(GLProfile.GL2);
        final GLCapabilities caps = new GLCapabilities(glp);
        caps.setAccumRedBits(16);
        caps.setAccumGreenBits(16);
        caps.setAccumBlueBits(16);
        caps.setStencilBits(8);
        caps.setPBuffer(true);
        test(caps);
    }

    public static void main(final String[] args) {
        org.junit.runner.JUnitCore.main(TestBug605FlippedImageAWT.class.getName());
    }
}
