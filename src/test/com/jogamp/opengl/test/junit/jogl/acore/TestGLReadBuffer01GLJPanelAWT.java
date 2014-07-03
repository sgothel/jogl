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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilitiesImmutable;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLJPanel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

import com.jogamp.opengl.test.junit.jogl.demos.es2.GearsES2;
import com.jogamp.opengl.test.junit.util.MiscUtils;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLDrawableUtil;
import com.jogamp.opengl.util.awt.AWTGLReadBufferUtil;
import com.jogamp.opengl.util.texture.TextureIO;

/**
 * Multiple GLJPanels in a JFrame
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestGLReadBuffer01GLJPanelAWT extends GLReadBuffer00Base {

    @BeforeClass
    public static void initClass() {
        GLProfile.initSingleton();
    }

    @Override
    public void test(final GLCapabilitiesImmutable caps, final boolean useSwingDoubleBuffer, final boolean skipGLOrientationVerticalFlip) {
        final AWTGLReadBufferUtil awtGLReadBufferUtil = new AWTGLReadBufferUtil(caps.getGLProfile(), false);
        final JFrame frame = new JFrame();
        final Dimension d = new Dimension(320, 240);
        final GLJPanel glad = createGLJPanel(skipGLOrientationVerticalFlip, useSwingDoubleBuffer, caps, d);
        final TextRendererGLEL textRendererGLEL = new TextRendererGLEL();
        final SnapshotGLELAWT snapshotGLEL = doSnapshot ? new SnapshotGLELAWT(textRendererGLEL, awtGLReadBufferUtil, skipGLOrientationVerticalFlip) : null;
        try {
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                public void run() {
                    frame.setLocation(64, 64);
                    final JPanel panel = new JPanel();
                    panel.setLayout(new BorderLayout());
                    panel.setDoubleBuffered(useSwingDoubleBuffer);
                    frame.getContentPane().add(panel);

                    final GearsES2 gears = new GearsES2(1);
                    gears.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                    gears.setVerbose(false);
                    glad.addGLEventListener(gears);
                    textRendererGLEL.setFlipVerticalInGLOrientation(skipGLOrientationVerticalFlip);
                    glad.addGLEventListener(textRendererGLEL);
                    if( doSnapshot ) {
                        glad.addGLEventListener(snapshotGLEL);
                    }
                    panel.add(glad);
                    frame.pack();
                    frame.setVisible(true);
                } } );
        } catch( final Throwable throwable ) {
            throwable.printStackTrace();
            Assume.assumeNoException( throwable );
        }
        glad.display(); // trigger initialization to get chosen-caps!
        final Dimension size0 = frame.getSize();
        final Dimension size1 = new Dimension(size0.width+100, size0.height+100);
        final Dimension size2 = new Dimension(size0.width-100, size0.height-100);
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
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size1);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size2);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }
            javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.setSize(size0);
                        frame.validate();
                    } } );
            try { Thread.sleep(duration); } catch (final InterruptedException e) { }

            if( doSnapshot ) {
                glad.disposeGLEventListener(snapshotGLEL, true /* remove */);
            }
            final Animator anim = new Animator(glad);
            anim.start();
            try { Thread.sleep(2*duration); } catch (final InterruptedException e) { }
            anim.stop();
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
        try {
            SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        frame.dispose();
                    } } );
        } catch (final Exception e1) {
            e1.printStackTrace();
        }
    }

    private GLJPanel createGLJPanel(final boolean skipGLOrientationVerticalFlip, final boolean useSwingDoubleBuffer, final GLCapabilitiesImmutable caps, final Dimension size) {
        final GLJPanel canvas = new GLJPanel(caps);
        canvas.setSize(size);
        canvas.setPreferredSize(size);
        canvas.setMinimumSize(size);
        canvas.setDoubleBuffered(useSwingDoubleBuffer);
        canvas.setSkipGLOrientationVerticalFlip(skipGLOrientationVerticalFlip);
        return canvas;
    }

    private class SnapshotGLELAWT implements GLEventListener {
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

    static GLCapabilitiesImmutable caps = null;
    static boolean doSnapshot = true;
    static boolean keyFrame = false;

    public static void main(final String[] args) {
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
        org.junit.runner.JUnitCore.main(TestGLReadBuffer01GLJPanelAWT.class.getName());
    }

}
