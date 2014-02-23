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

import java.io.File;

import javax.media.nativewindow.util.Dimension;
import javax.media.nativewindow.util.DimensionImmutable;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.GLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBuffer01GLWindowNEWT extends GLReadBuffer00Base {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    public void test(final GLCapabilitiesImmutable caps, final boolean useSwingDoubleBuffer, final boolean skipGLOrientationVerticalFlip) {
        if( skipGLOrientationVerticalFlip || useSwingDoubleBuffer ) {
            return; // NOP
        }
        final GLReadBufferUtil glReadBufferUtil = new GLReadBufferUtil(false, false);
        final GLWindow glad= GLWindow.create(caps);
        final TextRendererGLEL textRendererGLEL = new TextRendererGLEL();
        final SnapshotGLEL snapshotGLEL = doSnapshot ? new SnapshotGLEL(textRendererGLEL, glReadBufferUtil) : null;
        try {
            glad.setPosition(64, 64);
            glad.setSize(320, 240);
            final GearsES2 gears = new GearsES2(1);
            gears.setVerbose(false);
            glad.addGLEventListener(gears);
            textRendererGLEL.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
            glad.addGLEventListener(textRendererGLEL);
            if( doSnapshot ) {
                glad.addGLEventListener(snapshotGLEL);
            }
            glad.setVisible(true);
        } catch( Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        final DimensionImmutable size0 = new Dimension(glad.getWidth(), glad.getHeight());
        final DimensionImmutable size1 = new Dimension(size0.getWidth()+100, size0.getHeight()+100);
        final DimensionImmutable size2 = new Dimension(size0.getWidth()-100, size0.getHeight()-100);
        try {
            for(int i=0; i<3; i++) {
                final String str = "Frame# "+textRendererGLEL.frameNo+", user #"+(i+1);
                System.err.println(str);
                if( keyFrame ) {
                    waitForKey(str);
                }
                textRendererGLEL.userCounter = i + 1;
                glad.display();
            }
            try { Thread.sleep(duration); } catch (InterruptedException e) { }
            glad.setSize(size1.getWidth(), size1.getHeight());
            try { Thread.sleep(duration); } catch (InterruptedException e) { }
            glad.setSize(size2.getWidth(), size2.getHeight());
            try { Thread.sleep(duration); } catch (InterruptedException e) { }
            glad.setSize(size0.getWidth(), size0.getHeight());
            try { Thread.sleep(duration); } catch (InterruptedException e) { }

            if( doSnapshot ) {
                glad.disposeGLEventListener(snapshotGLEL, true /* remove */);
            }
            final Animator anim = new Animator(glad);
            anim.start();
            try { Thread.sleep(2*duration); } catch (InterruptedException e) { }
            anim.stop();
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        glad.destroy();
    }

    private class SnapshotGLEL implements GLEventListener {
        final TextRendererGLEL textRendererGLEL;
        final GLReadBufferUtil glReadBufferUtil;
        boolean defAutoSwapMode;
        boolean swapBuffersBeforeRead;
        int i;

        SnapshotGLEL(final TextRendererGLEL textRendererGLEL, final GLReadBufferUtil glReadBufferUtil) {
            this.textRendererGLEL = textRendererGLEL;
            this.glReadBufferUtil = glReadBufferUtil;
            this.defAutoSwapMode = true;
            this.swapBuffersBeforeRead = false;
            i = 0;
        }

        @Override
        public void init(GLAutoDrawable drawable) {
            defAutoSwapMode = drawable.getAutoSwapBufferMode();
            swapBuffersBeforeRead = GLDrawableUtil.swapBuffersBeforeRead(drawable.getChosenGLCapabilities());
            drawable.setAutoSwapBufferMode( !swapBuffersBeforeRead );
        }
        @Override
        public void dispose(GLAutoDrawable drawable) {
            drawable.setAutoSwapBufferMode( defAutoSwapMode );
        }
        @Override
        public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) { }
        @Override
        public void display(GLAutoDrawable drawable) {
            snapshot(i++, drawable, TextureIO.PNG, null);
        }
        public void snapshot(int sn, GLAutoDrawable drawable, String fileSuffix, String destPath) {
            final GL gl = drawable.getGL();
            final String postSNDetail = String.format("jgl-usr%03d", textRendererGLEL.userCounter);
            final String filenameJGL = getSnapshotFilename(sn, postSNDetail,
                                                           drawable.getChosenGLCapabilities(), drawable.getWidth(), drawable.getHeight(),
                                                           glReadBufferUtil.hasAlpha(), fileSuffix, destPath);
            if( swapBuffersBeforeRead ) {
                drawable.swapBuffers();
                // Just to test whether we use the right buffer,
                // i.e. back-buffer shall no more be required ..
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);
            } else {
                gl.glFinish(); // just make sure rendering finished ..
            }
            final boolean mustFlipVertically = !drawable.isGLOriented();
            System.err.println(Thread.currentThread().getName()+": ** screenshot: v-flip "+mustFlipVertically+", swapBuffersBeforeRead "+swapBuffersBeforeRead+", "+filenameJGL);

            if(glReadBufferUtil.readPixels(gl, mustFlipVertically)) {
                glReadBufferUtil.write(new File(filenameJGL));
            }
        }
    };

    static GLCapabilitiesImmutable caps = null;
    static boolean doSnapshot = true;
    static boolean keyFrame = false;

    public static void main(String[] args) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                duration = MiscUtils.atol(args[i], duration);
            } else if(args[i].equals("-keyFrame")) {
                keyFrame = true;
            } else if(args[i].equals("-noSnapshot")) {
                doSnapshot = false;
            }
        }
        org.junit.runner.JUnitCore.main(TestGLReadBuffer01GLWindowNEWT.class.getName());
    }

}
