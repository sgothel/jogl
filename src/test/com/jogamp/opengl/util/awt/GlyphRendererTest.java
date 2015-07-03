/*
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
package com.jogamp.opengl.util.awt;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;
import java.nio.ByteBuffer;

import javax.swing.JFrame;

import org.junit.Test;


/**
 * Test for {@link GlyphRenderer}.
 */
public class GlyphRendererTest {

    // Amount of time to wait before closing window
    private static final int WAIT_TIME = 1000;

    // Font to render
    private static Font FONT = new Font("Sans-serif", Font.PLAIN, 256);

    // Glyph to render
    private final Glyph glyph;

    // Glyph renderer implementation
    private GlyphRenderer glyphRenderer;

    // Utility for making canvases
    private final GLCanvasFactory canvasFactory = new GLCanvasFactory();

    /**
     * Creates a glyph renderer test.
     */
    public GlyphRendererTest() {
        glyph = createGlyph('G');
    }

    /**
     * Test for {@link GlyphRendererGL2}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Creates a glyph from the letter 'G'
     *   <li>Sets its width and height
     *   <li>Gives it texture coordinates with Y axis flipped
     *   <li>Creates a GlyphRenderer and adds the test as a listener
     *   <li>Draws 'G' into a texture using Java2D
     *   <li>Clears the screen to cyan
     *   <li>Draws the glyph to the screen using two different colors
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>Letter 'G' is drawn twice, one magenta and one yellow
     *   <li>"Received GlyphRenderer event!" is printed out each frame
     * </ul>
     *
     * <p><em>Notes:</em>
     * <ul>
     *   <li>If texture not set up glyph would be completely transparent
     * </ul>
     */
    @Test
    public void testGlyphRendererGL2() throws Exception {

        final JFrame frame = new JFrame("testGlyphRendererGL2");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL2");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL2EventAdapter() {

            @Override
            public void doInit(final GL2 gl) {

                // Set up glyph renderer
                glyphRenderer = new GlyphRendererGL2(gl);
                glyphRenderer.addListener(new GlyphRenderer.EventListener() {
                    @Override
                    public void onGlyphRendererEvent(GlyphRenderer.EventType type) {
                        if (type == GlyphRenderer.EventType.AUTOMATIC_FLUSH) {
                            System.out.println("Automatically flushing!");
                        }
                    }
                });

                // Set up texture
                final GlyphTexture texture = new GlyphTexture(gl);
                texture.bind(gl);
                texture.upload(gl);
            }

            @Override
            public void doDisplay(final GL2 gl) {

                // View
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL.GL_COLOR_BUFFER_BIT);

                // Draw glyph
                final TextureCoords coordinates = new TextureCoords(0, 1, 1, 0);
                glyphRenderer.beginRendering(gl, true, 512, 512, true);
                glyphRenderer.setColor(1, 0, 1, 1); // magenta
                glyphRenderer.drawGlyph(gl, glyph, 40, 80, 0, 1.0f, coordinates);
                glyphRenderer.setColor(1, 1, 0, 1); // yellow
                glyphRenderer.drawGlyph(gl, glyph, 260, 80, 0, 1.0f, coordinates);
                glyphRenderer.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    /**
     * Test case for {@link GlyphRendererGL3}.
     *
     * <p><em>Performs the following:</em>
     * <ul>
     *   <li>Creates a glyph from the letter 'G'
     *   <li>Sets its width and height
     *   <li>Gives it texture coordinates with Y axis flipped
     *   <li>Creates a GlyphRenderer and adds the test as a listener
     *   <li>Draws 'G' into a texture using Java2D
     *   <li>Clears the screen to cyan
     *   <li>Draws the glyph to the screen using two different colors
     * </ul>
     *
     * <p><em>Results:</em>
     * <ul>
     *   <li>Letter 'G' is drawn twice, one magenta and one yellow
     *   <li>"Received GlyphRenderer event!" is printed out each frame
     * </ul>
     *
     * <p><em>Notes:</em>
     * <ul>
     *   <li>If texture not set up would be completely transparent
     * </ul>
     */
    @Test
    public void testGlyphRendererGL3() throws Exception {

        final JFrame frame = new JFrame("testGlyphRendererGL3");
        final GLCanvas canvas = canvasFactory.createGLCanvas("GL3");

        frame.add(canvas);
        canvas.addGLEventListener(new DebugGL3EventAdapter() {

            @Override
            public void doInit(final GL3 gl) {

                // Set up glyph renderer
                glyphRenderer = new GlyphRendererGL3(gl);
                glyphRenderer.addListener(new GlyphRenderer.EventListener() {
                    public void onGlyphRendererEvent(GlyphRenderer.EventType type) {
                        if (type == GlyphRenderer.EventType.AUTOMATIC_FLUSH) {
                            System.out.println("Automatically flushing!");
                        }
                    }
                });

                // Set up texture
                final GlyphTexture texture = new GlyphTexture(gl);
                texture.bind(gl);
                texture.upload(gl);
            }

            @Override
            public void doDisplay(final GL3 gl) {

                // Clear
                gl.glClearColor(0, 1, 1, 1);
                gl.glClear(GL3.GL_COLOR_BUFFER_BIT);

                // Draw glyph
                final TextureCoords coordinates = new TextureCoords(0, 1, 1, 0);
                glyphRenderer.beginRendering(gl, true, 512, 512, true);
                glyphRenderer.setColor(1, 0, 1, 1); // magenta
                glyphRenderer.drawGlyph(gl, glyph, 40, 80, 0, 1.0f, coordinates);
                glyphRenderer.setColor(1, 1, 0, 1); // yellow
                glyphRenderer.drawGlyph(gl, glyph, 260, 80, 0, 1.0f, coordinates);
                glyphRenderer.endRendering(gl);
            }
        });
        TestRunner.run(frame, WAIT_TIME);
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Returns a glyph for a character.
     */
    private static Glyph createGlyph(final char c) {
        final GlyphVector gv = createGlyphVector(c);
        final Glyph glyph = new Glyph(c, gv);
        glyph.width = 54;
        glyph.height = 72;
        return glyph;
    }

    /**
     * Returns a glyph vector for a character.
     */
    private static GlyphVector createGlyphVector(final char c) {
        final FontRenderContext frc = createFontRenderContext();
        final char[] chars = new char[] { c };
        return FONT.createGlyphVector(frc, chars);
    }

    /**
     * Returns a blank font render context.
     */
    private static FontRenderContext createFontRenderContext() {
        return new FontRenderContext(new AffineTransform(), false, false);
    }

    //-----------------------------------------------------------------
    // Nested classes
    //

    /**
     * Texture with a giant 'G' in it.
     */
    private static class GlyphTexture {

        // Width and height of the texture
        private static final int SIZE = 256;

        // Internal OpenGL identifier for texture
        private int handle;

        GlyphTexture(final GL gl) {
            handle = createHandle(gl);
        }

        void bind(final GL gl) {
            gl.glActiveTexture(GL.GL_TEXTURE0);
            gl.glBindTexture(GL.GL_TEXTURE_2D, handle);
        }

        void upload(final GL gl) {

            final BufferedImage image = createBufferedImage();
            final Graphics2D graphics = image.createGraphics();
            final ByteBuffer buffer = createByteBuffer(image);

            // Draw character into image
            graphics.setFont(FONT);
            graphics.drawString("G", 0.09f * SIZE, 0.80f * SIZE);

            // Upload it to the texture
            final GLProfile profile = gl.getGLProfile();
            gl.glPixelStorei(GL.GL_UNPACK_ALIGNMENT, 1);
            gl.glTexImage2D(
                    GL.GL_TEXTURE_2D,
                    0,
                    profile.isGL3() ? GL3.GL_RED : GL2.GL_INTENSITY,
                    SIZE, SIZE,
                    0,
                    profile.isGL3() ? GL3.GL_RED : GL2.GL_LUMINANCE,
                    GL.GL_UNSIGNED_BYTE,
                    buffer);
            setParameter(gl, GL.GL_TEXTURE_MAG_FILTER, GL.GL_LINEAR);
            setParameter(gl, GL.GL_TEXTURE_MIN_FILTER, GL.GL_LINEAR);
        }

        private static BufferedImage createBufferedImage() {
            return new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_BYTE_GRAY);
        }

        private static ByteBuffer createByteBuffer(final BufferedImage image) {
            final Raster raster = image.getRaster();
            final DataBufferByte dbb = (DataBufferByte) raster.getDataBuffer();
            final byte[] array = dbb.getData();
            return ByteBuffer.wrap(array);
        }

        private static int createHandle(final GL gl) {
            final int[] handles = new int[1];
            gl.glGenTextures(1, handles, 0);
            return handles[0];
        }

        private static void setParameter(final GL gl, final int name, final int value) {
            gl.glTexParameteri(GL.GL_TEXTURE_2D, name, value);
        }
    }
}

