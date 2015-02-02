/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.junit.jogl.tile;

import com.jogamp.newt.event.TraceKeyAdapter;
import com.jogamp.newt.event.TraceWindowAdapter;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTWindowAdapter;
import com.jogamp.opengl.test.junit.jogl.demos.gl2.Gears;
import com.jogamp.opengl.test.junit.util.QuitAdapter;
import com.jogamp.opengl.test.junit.util.UITestCase;
import com.jogamp.opengl.util.Animator;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.RandomTileRenderer;
import com.jogamp.opengl.util.TileRendererBase;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import java.awt.Dimension;
import java.awt.Frame;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import org.junit.Assert;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

/**
 * Demos an onscreen AWT {@link GLCanvas} being used for
 * {@link RandomTileRenderer} rendering to produce a PNG file.
 * <p>
 * {@link RandomTileRenderer} is being kicked off from the main thread.
 * </p>
 * <p>
 * {@link RandomTileRenderer} setup and finishing is performed
 * within the pre- and post {@link GLEventListener}
 * set via {@link TileRendererBase#setGLEventListener(GLEventListener, GLEventListener)}
 * on the animation thread.
 * </p>
 * <p>
 * At tile rendering finish, the viewport and
 * and the original {@link GLEventListener}'s PMV matrix as well.
 * The latter is done by calling it's {@link GLEventListener#reshape(GLAutoDrawable, int, int, int, int) reshape} method.
 * </p>
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TestRandomTiledRendering3GL2AWT extends UITestCase {
    static long duration = 3500; // ms
    static int width  = 512;
    static int height = 512;

    @Test
    public void test01_aa0() throws IOException, InterruptedException, InvocationTargetException {
        doTest(0);
    }
    @Test
    public void test02_aa8() throws IOException, InterruptedException, InvocationTargetException {
        doTest(8);
    }

    void doTest(final int msaaCount) throws IOException, InterruptedException, InvocationTargetException {
        final GLCapabilities caps = new GLCapabilities(null);
        if( msaaCount > 0 ) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(msaaCount);
        }

        final Frame frame = new Frame("Gears AWT Test");
        Assert.assertNotNull(frame);

        final GLCanvas glad = new GLCanvas(caps);
        Assert.assertNotNull(glad);
        final Dimension glc_sz = new Dimension(width, height);
        glad.setMinimumSize(glc_sz);
        glad.setPreferredSize(glc_sz);
        glad.setSize(glc_sz);
        frame.add(glad);

        final Gears gears = new Gears();
        glad.addGLEventListener( gears );

        final QuitAdapter quitAdapter = new QuitAdapter();
        new AWTKeyAdapter(new TraceKeyAdapter(quitAdapter), glad).addTo(glad);
        new AWTWindowAdapter(new TraceWindowAdapter(quitAdapter), glad).addTo(frame);

        // Fix the image size for now
        final int maxTileSize = 64;
        final int imageWidth = 256 * 6;
        final int imageHeight = 256 * 4;

        // Initialize the tile rendering library
        final RandomTileRenderer renderer = new RandomTileRenderer();
        renderer.setImageSize(imageWidth, imageHeight);
        final GLPixelBuffer.GLPixelBufferProvider pixelBufferProvider = GLPixelBuffer.defaultProviderWithRowStride;
        final boolean[] flipVertically = { false };
        final boolean[] rendererActive = { true };

        final GLEventListener preTileGLEL = new GLEventListener() {
            final int w = maxTileSize, h = maxTileSize;
            int dx = 0, dy = 0;

            @Override
            public void init(final GLAutoDrawable drawable) {
                final GL gl = drawable.getGL();
                final PixelFormat.Composition hostPixelComp = pixelBufferProvider.getHostPixelComp(gl.getGLProfile(), 3);
                final GLPixelAttributes pixelAttribs = pixelBufferProvider.getAttributes(gl, 3, true);
                final GLPixelBuffer pixelBuffer = pixelBufferProvider.allocate(gl, hostPixelComp, pixelAttribs, true, imageWidth, imageHeight, 1, 0);
                renderer.setImageBuffer(pixelBuffer);
                if( drawable.isGLOriented() ) {
                    flipVertically[0] = false;
                } else {
                    flipVertically[0] = true;
                }
                System.err.println("XXX pre-init: "+renderer);
            }
            @Override
            public void dispose(final GLAutoDrawable drawable) {}
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( dx+w <= imageWidth && dy+h <= imageHeight ) {
                    renderer.setTileRect(dx, dy, w, h);
                    dx+=w+w/2;
                    if( dx + w > imageWidth ) {
                        dx = 0;
                        dy+=h+h/2;
                    }
                } else if( rendererActive[0] ) {
                    rendererActive[0] = false;
                }
                System.err.println("XXX pre-display: "+renderer);
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
        };
        final GLEventListener postTileGLEL = new GLEventListener() {
            @Override
            public void init(final GLAutoDrawable drawable) {}
            @Override
            public void dispose(final GLAutoDrawable drawable) {}
            @Override
            public void display(final GLAutoDrawable drawable) {
                if( !rendererActive[0] ) {
                    final GLPixelBuffer imageBuffer = renderer.getImageBuffer();
                    imageBuffer.clear(); // full size available
                    System.err.println("XXX !active -> save");
                    System.err.println("XXX post-display: "+renderer);
                    final TextureData textureData = new TextureData(
                            caps.getGLProfile(),
                            0 /* internalFormat */,
                            imageWidth, imageHeight,
                            0,
                            imageBuffer.pixelAttributes,
                            false, false,
                            flipVertically[0],
                            imageBuffer.buffer,
                            null /* Flusher */);
                    try {
                        final String filename = getSnapshotFilename(0, "-tile", glad.getChosenGLCapabilities(), imageWidth, imageHeight, false, TextureIO.PNG, null);
                        final File file = new File(filename);
                        TextureIO.write(textureData, file);
                    } catch (final IOException e) {
                        e.printStackTrace();
                    }
                    renderer.detachAutoDrawable();
                    System.err.println("XXX post-display detached: "+renderer);
                    drawable.getGL().glViewport(0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                    glad.getGLEventListener(0).reshape(drawable, 0, 0, drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
                }
            }
            @Override
            public void reshape(final GLAutoDrawable drawable, final int x, final int y, final int width, final int height) {}
        };
        renderer.setGLEventListener(preTileGLEL, postTileGLEL);

        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.pack();
                frame.setVisible(true);
            }});

        final Animator animator = new Animator(glad);
        animator.setUpdateFPSFrames(60, System.err);
        animator.start();

        boolean signalTileRenderer = true;

        while(!quitAdapter.shouldQuit() && animator.isAnimating() &&
              ( rendererActive[0] || animator.getTotalFPSDuration()<duration ) )
        {
            if( signalTileRenderer && animator.getTotalFPSDuration() > 90 ) {
                signalTileRenderer = false;
                // tile rendering !
                System.err.println("XXX START TILE RENDERING");
                renderer.attachAutoDrawable(glad);
            }
            Thread.sleep(100);
        }

        Assert.assertNotNull(frame);
        Assert.assertNotNull(glad);
        Assert.assertNotNull(animator);

        animator.stop();
        Assert.assertEquals(false, animator.isAnimating());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.setVisible(false);
            }});
        Assert.assertEquals(false, frame.isVisible());
        javax.swing.SwingUtilities.invokeAndWait(new Runnable() {
            public void run() {
                frame.remove(glad);
                frame.dispose();
            }});
    }

    public static void main(final String args[]) {
        for(int i=0; i<args.length; i++) {
            if(args[i].equals("-time")) {
                i++;
                try {
                    duration = Integer.parseInt(args[i]);
                } catch (final Exception ex) { ex.printStackTrace(); }
            }
        }
        org.junit.runner.JUnitCore.main(TestRandomTiledRendering3GL2AWT.class.getName());
    }
}
