/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLDrawable;
import com.jogamp.opengl.GLEventListener;

import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Test synchronous GLAutoDrawable display, swap-buffer and read-pixels
 * including non-MSAA and MSAA framebuffer.
 * <p>
 * See {@link GLReadBuffer00Base} for related bugs and further details.
 * </p>
 */
public abstract class GLReadBuffer00BaseAWT extends GLReadBuffer00Base {

    protected class SnapshotGLELAWT implements GLEventListener {
        final TextRendererGLEL textRendererGLEL;
        final AWTGLReadBufferUtil glReadBufferUtil;
        final boolean skipGLOrientationVerticalFlip;
        boolean defAutoSwapMode;
        boolean swapBuffersBeforeRead;
        int i;

        SnapshotGLELAWT(final TextRendererGLEL textRendererGLEL, final AWTGLReadBufferUtil glReadBufferUtil, final boolean skipGLOrientationVerticalFlip) {
            this.textRendererGLEL = textRendererGLEL;
            this.glReadBufferUtil = glReadBufferUtil;
            this.skipGLOrientationVerticalFlip = skipGLOrientationVerticalFlip;
            this.defAutoSwapMode = true;
            this.swapBuffersBeforeRead = false;
            i = 0;
        }

        @Override
        public void init(final GLAutoDrawable drawable) {
            defAutoSwapMode = drawable.getAutoSwapBufferMode();
            swapBuffersBeforeRead = GLDrawableUtil.swapBuffersBeforeRead(drawable.getChosenGLCapabilities());
            drawable.setAutoSwapBufferMode( !swapBuffersBeforeRead );
        }
        @Override
        public void dispose(final GLAutoDrawable drawable) {
            drawable.setAutoSwapBufferMode( defAutoSwapMode );
        }
        @Override
        public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) { }
        @Override
        public void display(final GLAutoDrawable drawable) {
            snapshot(i++, drawable.getGL(), TextureIO.PNG, null);
        }
        public void snapshot(final int sn, final GL gl, final String fileSuffix, final String destPath) {
            final GLDrawable drawable = gl.getContext().getGLReadDrawable();
            final String postSNDetail = String.format("awt-usr%03d", textRendererGLEL.userCounter);
            final String filenameAWT = getSnapshotFilename(sn, postSNDetail,
                                                           drawable.getChosenGLCapabilities(), drawable.getSurfaceWidth(), drawable.getSurfaceHeight(),
                                                           glReadBufferUtil.hasAlpha(), fileSuffix, destPath);
            if( swapBuffersBeforeRead ) {
                drawable.swapBuffers();
                // Just to test whether we use the right buffer,
                // i.e. back-buffer shall no more be required ..
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            } else {
                gl.glFinish(); // just make sure rendering finished ..
            }

            final boolean awtOrientation = !( drawable.isGLOriented() && skipGLOrientationVerticalFlip );
            System.err.println(Thread.currentThread().getName()+": ** screenshot: awtOrient/v-flip "+awtOrientation+", swapBuffersBeforeRead "+swapBuffersBeforeRead+", "+filenameAWT);

            final BufferedImage image = glReadBufferUtil.readPixelsToBufferedImage(gl, awtOrientation);
            final File fout = new File(filenameAWT);
            try {
                ImageIO.write(image, "png", fout);
            } catch (final IOException e) {
                e.printStackTrace();
            }
            /**
            final String filenameJGL = getSnapshotFilename(sn, "jgl",
                                                           drawable.getChosenGLCapabilities(), drawable.getWidth(), drawable.getHeight(),
                                                           glReadBufferUtil.hasAlpha(), fileSuffix, destPath);
            glReadBufferUtil.write(new File(filenameJGL));
            */
        }
    };

}
