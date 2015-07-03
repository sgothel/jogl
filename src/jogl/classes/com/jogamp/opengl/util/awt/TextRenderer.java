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
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


/**
 * Utility for rendering bitmapped Java 2D text into an OpenGL window.
 *
 * <p>Has high performance, full Unicode support, and a simple API.  Performs
 * appropriate caching of text rendering results in an OpenGL texture internally
 * to avoid repeated font rasterization. The caching is completely automatic,
 * does not require any user intervention, and has no visible controls in the
 * public API.
 *
 * <p>Using the TextRenderer is simple. Add a "{@code TextRenderer renderer;}"
 * field to your {@link com.jogamp.opengl.GLEventListener GLEventListener}. In
 * your {@link com.jogamp.opengl.GLEventListener#init init} method, add:
 *
 * <PRE>
 * renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
 * </PRE>
 *
 * <p>In the {@link com.jogamp.opengl.GLEventListener#display display} method
 * of your {@link com.jogamp.opengl.GLEventListener GLEventListener}, add:
 * <PRE>
 * renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
 * // optionally set the color
 * renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
 * renderer.draw("Text to draw", xPosition, yPosition);
 * // ... more draw commands, color changes, etc.
 * renderer.endRendering();
 * </PRE>
 *
 * <p>Unless you are sharing textures between OpenGL contexts, you do not need
 * to call the {@link #dispose dispose} method of the TextRenderer; the OpenGL
 * resources it uses internally will be cleaned up automatically when the OpenGL
 * context is destroyed.
 *
 * <p><b>Note</b> that the TextRenderer will cause the Vertex Array Object
 * binding to change, or to be unbound.
 *
 * <p>Internally, the renderer uses a rectangle packing algorithm to pack both
 * glyphs and full Strings' rendering results (which are variable size) onto a
 * larger OpenGL texture. The internal backing store is maintained using a
 * {@link com.jogamp.opengl.util.awt.TextureRenderer TextureRenderer}. A least
 * recently used (LRU) algorithm is used to discard previously rendered strings;
 * the specific algorithm is undefined, but is currently implemented by flushing
 * unused Strings' rendering results every few hundred rendering cycles, where a
 * rendering cycle is defined as a pair of calls to {@link #beginRendering
 * beginRendering} / {@link #endRendering endRendering}.
 *
 * @author John Burkey
 * @author Kenneth Russell
 */
/*@Notthreadsafe*/
public final class TextRenderer {

    // True to print debugging information
    static final boolean DEBUG = false;

    // Common instance of the default render delegate
    private static final RenderDelegate DEFAULT_RENDER_DELEGATE = new DefaultRenderDelegate();

    // Face, style, and size of text to render with
    private final Font font;

    // Delegate to store glyphs
    private final GlyphCache glyphCache;

    // Delegate to create glyphs
    private final GlyphProducer glyphProducer;

    // Delegate to draw glyphs
    private final GlyphRenderer glyphRenderer;

    // Observer coordinating components
    private Mediator mediator;

    /**
     * Constructs a text renderer from a font.
     *
     * <p>The resulting text renderer will use no antialiasing or fractional
     * metrics, and the default render delegate.  It will not attempt to use
     * OpenGL's automatic mipmap generation for better scaling.  All Unicode
     * characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @throws IllegalArgumentException if font is <tt>null</tt>
     */
    public TextRenderer(/*@Nonnull*/ final Font font) {
        this(font, false, false, null, false, null);
    }

    /**
     * Constructs a text renderer from a font with mipmapping support.
     *
     * <p>The resulting text renderer will use no antialiasing or fractional
     * metrics, and the default render delegate.  If mipmapping is requested,
     * the text renderer will attempt to use OpenGL's automatic mipmap
     * generation for better scaling.  All Unicode characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @throws IllegalArgumentException if font is <tt>null</tt>
     */
    public TextRenderer(/*@Nonnull*/ final Font font, final boolean mipmap) {
        this(font, false, false, null, mipmap, null);
    }

    /**
     * Constructs a text renderer from a font and text properties.
     *
     * <p>The resulting text renderer will use antialiasing and fractional
     * metrics if requested, and the default render delegate.  It will not
     * attempt to use OpenGL's automatic mipmap generation for better scaling.
     * All Unicode characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @param antialias Whether to smooth edges of text
     * @param subpixel Whether to use subpixel accuracy
     * @throws IllegalArgumentException if font is <tt>null</tt>
     */
    public TextRenderer(/*@Nonnull*/ final Font font, final boolean antialias, final boolean subpixel) {
        this(font, antialias, subpixel, null, false, null);
    }

    /**
     * Constructs a text renderer from a font, text properties, and a render delegate.
     *
     * <p>The resulting text renderer will use antialiasing and fractional
     * metrics if requested.  The optional render delegate provides more control
     * over the text rendered.  The text renderer will not attempt to use
     * OpenGL's automatic mipmap generation for better scaling.  All Unicode
     * characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @param antialias Whether to smooth edges of text
     * @param subpixel Whether to use subpixel accuracy
     * @param rd Optional controller of rendering details (nullable)
     * @throws IllegalArgumentException if font is <tt>null</tt>
     * @throws UnsupportedOperationException if render delegate wants full color
     */
    public TextRenderer(/*@Nonnull*/ final Font font,
                        final boolean antialias,
                        final boolean subpixel,
                        /*@Nullable*/ final RenderDelegate rd) {
        this(font, antialias, subpixel, rd, false, null);
    }

    /**
     * Constructs a text renderer from a font, text properties, a render delegate, and mipmapping.
     *
     * <p>The resulting text renderer will use antialiasing and fractional
     * metrics if requested.  The optional render delegate provides more control
     * over the text rendered.  If mipmapping is requested, the text renderer
     * will attempt to use OpenGL's automatic mipmap generation for better
     * scaling.  All Unicode characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @param antialias Whether to smooth edges of text
     * @param subpixel Whether to use subpixel accuracy
     * @param rd Optional controller of rendering details (nullable)
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @throws IllegalArgumentException if font is <tt>null</tt>
     * @throws UnsupportedOperationException if render delegate wants full color
     */
    public TextRenderer(/*@Nonnull*/ final Font font,
                        final boolean antialias,
                        final boolean subpixel,
                        /*Nullable*/ final RenderDelegate rd,
                        final boolean mipmap) {
        this(font, antialias, subpixel, rd, mipmap, null);
    }

    /**
     * Constructs a text renderer from a font, text properties, a render delegate, mipmapping, and a range.
     *
     * <p>The resulting text renderer will use antialiasing and fractional
     * metrics if requested.  The optional render delegate provides more control
     * over the text rendered.  If mipmapping is requested, the text renderer
     * will attempt to use OpenGL's automatic mipmap generation for better
     * scaling.  If a character range is specified, the text renderer will limit
     * itself to those characters to try to achieve better performance.
     * Otherwise all Unicode characters will be available.
     *
     * @param font Face, style, and size of text to render with (non-null)
     * @param antialias Whether to smooth edges of text
     * @param subpixel Whether to use subpixel accuracy
     * @param rd Optional controller of rendering details (nullable)
     * @param mipmap Whether to generate mipmaps to make the text scale better
     * @param ub Optional range of unicode characters to limit to, for better performance (nullable)
     * @throws IllegalArgumentException if font is <tt>null</tt>
     * @throws UnsupportedOperationException if render delegate wants full color
     * @throws UnsupportedOperationException if unicode block unsupported
     */
    public TextRenderer(/*@Nonnull*/ final Font font,
                        final boolean antialias,
                        final boolean subpixel,
                        /*@Nullable*/ RenderDelegate rd,
                        final boolean mipmap,
                        /*@Nullable*/ final UnicodeBlock ub) {

        if (font == null) {
            throw new IllegalArgumentException("Font is null!");
        } else if (rd == null) {
            rd = DEFAULT_RENDER_DELEGATE;
        }

        this.font = font;
        this.glyphCache = GlyphCache.newInstance(font, rd, antialias, subpixel, mipmap);
        this.glyphProducer = GlyphProducerFactory.createGlyphProducer(font, rd, glyphCache.getFontRenderContext(), ub);
        this.glyphRenderer = new GlyphRendererAdapter();
        this.mediator = null;
    }

   /**
    * Starts a render cycle.
    *
    * <p>Sets up a two-dimensional orthographic projection with (0,0) as the
    * lower-left coordinate and (width, height) as the upper-right coordinate.
    * Binds and enables the internal OpenGL texture object, sets the texture
    * environment mode to GL_MODULATE, and changes the current color to the last
    * color set with this text drawer via {@link #setColor}.
    *
    * <p>This method disables the depth test and is equivalent to
    * beginRendering(width, height, true).
    *
    * @param width Width of the current on-screen OpenGL drawable (non-negative)
    * @param height Height of the current on-screen OpenGL drawable (non-negative)
    * @throws GLException if an OpenGL context is not current
    */
    public void beginRendering(/*@Nonnegative*/ final int width, /*@Nonnegative*/ final int height) {
        beginRendering(true, width, height, true);
    }

   /**
    * Starts a render cycle.
    *
    * <p>Sets up a two-dimensional orthographic projection with (0,0) as the
    * lower-left coordinate and (width, height) as the upper-right coordinate.
    * Binds and enables the internal OpenGL texture object, sets the texture
    * environment mode to GL_MODULATE, and changes the current color to the last
    * color set with this text drawer via {@link #setColor}.
    *
    * <p>Disables the depth test if the disableDepthTest argument is true.
    *
    * @param width Width of the current on-screen OpenGL drawable (non-negative)
    * @param height Height of the current on-screen OpenGL drawable (non-negative)
    * @param disableDepthTest Whether to disable the depth test
    * @throws GLException if an OpenGL context is not current
    */
    public void beginRendering(/*@Nonnegative*/ final int width,
                               /*@Nonnegative*/ final int height,
                               final boolean disableDepthTest) {
        beginRendering(true, width, height, disableDepthTest);
    }

   /**
    * Starts a 3D render cycle.
    *
    * <p>Assumes the end user is responsible for setting up the modelview and
    * projection matrices, and will render text using the {@link #draw3D}
    * method.
    *
    * @throws GLException if an OpenGL context is not current
    */
    public void begin3DRendering() {
        beginRendering(false, 0, 0, false);
    }

    /**
     * Starts a render cycle.
     *
     * @param ortho <tt>true</tt> to use orthographic projection
     * @param width Width of the current OpenGL viewport (non-negative)
     * @param height Height of the current OpenGL viewport (non-negative)
     * @param disableDepthTest <tt>true</tt> to ignore depth values
     * @throws GLException if no OpenGL context is current, or is unexpected version
     * @throws IllegalArgumentException if width or height is negative
     */
    private void beginRendering(final boolean ortho,
                                /*@Nonnegative*/ final int width,
                                /*@Nonnegative*/ final int height,
                                final boolean disableDepthTest) {

        if (width < 0) {
            throw new IllegalArgumentException("Width is negative!");
        } else if (height < 0) {
            throw new IllegalArgumentException("Height is negative!");
        }

        // Get the current OpenGL context
        final GL gl = GLContext.getCurrentGL();

        // Make sure components are set up properly
        if (mediator == null) {
            mediator = new Mediator();
            glyphCache.addListener(mediator);
            glyphRenderer.addListener(mediator);
        }

        // Set up components
        glyphCache.beginRendering(gl);
        glyphRenderer.beginRendering(gl, ortho, width, height, disableDepthTest);
    }

   /**
    * Draws a character sequence at a location.
    *
    * <p>The baseline of the leftmost character is at position (x, y) specified
    * in OpenGL coordinates, where the origin is at the lower-left of the
    * drawable and the Y coordinate increases in the upward direction.
    *
    * @param str Text to draw (non-null)
    * @param x Position to draw on X axis
    * @param y Position to draw on Y axis
    * @throws GLException if an OpenGL context is not current, or is unexpected version
    */
    public void draw(/*@Nonnull*/ final CharSequence cs, final int x, final int y) {
        draw3D(cs, x, y, 0, 1);
    }

   /**
    * Draws a string at a location.
    *
    * <p>The baseline of the leftmost character is at position (x, y) specified
    * in OpenGL coordinates, where the origin is at the lower-left of the
    * drawable and the Y coordinate increases in the upward direction.
    *
    * @param str Text to draw (non-null)
    * @param x Position to draw on X axis
    * @param y Position to draw on Y axis
    * @throws GLException if an OpenGL context is not current, or is unexpected version
    */
    public void draw(/*@Nonnull*/ final String str, final int x, final int y) {
        draw3D(str, x, y, 0, 1);
    }

   /**
    * Draws a character sequence at a location in 3D space.
    *
    * <p>The baseline of the leftmost character is placed at position (x, y, z)
    * in the current coordinate system.
    *
    * @param str Text to draw (non-null)
    * @param x X coordinate at which to draw
    * @param y Y coordinate at which to draw
    * @param z Z coordinate at which to draw
    * @param scale Uniform scale applied to width and height of text
    * @throws GLException if an OpenGL context is not current, or is unexpected version
    */
    public void draw3D(/*@Nonnull*/ final CharSequence cs,
                       final float x, final float y, final float z,
                       final float scale) {
        draw3D(cs.toString(), x, y, z, scale);
    }

    /**
     * Draws text at a location in 3D space.
     *
     * <p>Uses the renderer's current color.  The baseline of the leftmost
     * character is placed at position (x, y, z) in the current coordinate
     * system.
     *
     * @param str Text to draw (non-null)
     * @param x Position to draw on X axis
     * @param y Position to draw on Y axis
     * @param z Position to draw on Z axis
     * @param scale Uniform scale applied to width and height of text
     * @throws GLException if no OpenGL context is current, or is unexpected version
     * @throws NullPointerException if text is <tt>null</tt>
     */
    public void draw3D(/*@Nonnull*/ final String str,
                       float x, final float y, final float z,
                       final float scale) {

        // Get the current OpenGL context
        final GL gl = GLContext.getCurrentGL();

        // Get all the glyphs for the string
        final List<Glyph> glyphs = glyphProducer.createGlyphs(str);

        // Render each glyph
        for (final Glyph glyph : glyphs) {
            if (glyph.location == null) {
                glyphCache.upload(glyph);
            }
            final TextureCoords coords = glyphCache.find(glyph);
            final float advance = glyphRenderer.drawGlyph(gl, glyph, x, y, z, scale, coords);
            x += advance * scale;
        }
    }

    /**
     * Finishes a render cycle.
     */
    public void endRendering() {

        // Get the current OpenGL context
        final GL gl = GLContext.getCurrentGL();

        // Tear down components
        glyphCache.endRendering(gl);
        glyphRenderer.endRendering(gl);
    }

    /**
     * Finishes a 3D render cycle.
     */
    public void end3DRendering() {
        endRendering();
    }

    /**
     * Forces all stored text to be rendered.
     *
     * <p>This should be called after each call to draw() if you are setting
     * OpenGL state such as the modelview matrix between calls to draw().
     *
     * @throws GLException if no OpenGL context is current, or is unexpected version
     * @throws IllegalStateException if not in a render cycle
     */
    public void flush() {

        // Get the current OpenGL context
        final GL gl = GLContext.getCurrentGL();

        // Make sure glyph cache is up to date
        glyphCache.update(gl);

        // Render outstanding glyphs
        glyphRenderer.flush(gl);
    }

    /**
     * Destroys resources used by the text renderer.
     *
     * @throws GLException if no OpenGL context is current, or is unexpected version
     */
    public void dispose() {

        // Get the current OpenGL context
        final GL gl = GLContext.getCurrentGL();

        // Destroy the glyph cache
        glyphCache.dispose(gl);

        // Destroy the glyph renderer
        glyphRenderer.dispose(gl);
    }

    //-----------------------------------------------------------------
    // Utilities
    //

    /**
     * Determines the bounding box of text.
     *
     * <p>Assumes it was rendered at the origin.
     *
     * <p>The coordinate system of the returned rectangle is Java 2D's, with
     * increasing Y coordinates in the downward direction.  The relative
     * coordinate (0,0) in the returned rectangle corresponds to the baseline of
     * the leftmost character of the rendered string, in similar fashion to the
     * results returned by, for example, {@link GlyphVector#getVisualBounds
     * getVisualBounds}.
     *
     * <p>Most applications will use only the width and height of the returned
     * Rectangle for the purposes of centering or justifying the String.  It is
     * not specified which Java 2D bounds ({@link GlyphVector#getVisualBounds
     * getVisualBounds}, {@link GlyphVector#getPixelBounds getPixelBounds}, etc.)
     * the returned bounds correspond to, although every effort is made to ensure
     * an accurate bound.
     *
     * @param cs Text to get bounding box for (non-null)
     * @return Rectangle surrounding the given string (non-null)
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(/*@Nonnull*/ final CharSequence cs) {
        return getBounds(cs.toString());
    }

    /**
     * Determines the bounding box of text.
     *
     * @param str Text to get bounding box for (non-null)
     * @return Rectangle surrounding the given string (non-null)
     */
    /*@Nonnull*/
    public Rectangle2D getBounds(/*@Nonnull*/ final String str) {
        return glyphProducer.findBounds(str);
    }

    /**
     * Determines the pixel width of a character.
     *
     * @param c Character to get pixel width of
     * @return Number of pixels required to advance past the character
     */
    public float getCharWidth(final char c) {
        return glyphProducer.findAdvance(c);
    }

    //------------------------------------------------------------------
    // Getters and setters
    //

    /**
     * Changes current color.
     *
     * @param color Color to use for rendering text (non-null)
     * @throws NullPointerException if color is <tt>null</tt>
     * @throws GLException if an OpenGL context is not current
     */
    public void setColor(/*@Nonnull*/ final Color color) {
        final float r = ((float) color.getRed()) / 255f;
        final float g = ((float) color.getGreen()) / 255f;
        final float b = ((float) color.getBlue()) / 255f;
        final float a = ((float) color.getAlpha()) / 255f;
        setColor(r, g, b, a);
    }

    /**
     * Changes current color.
     *
     * <p>Each component ranges from 0.0f to 1.0f. The alpha component, if
     * used, does not need to be premultiplied into the color channels as
     * described in the documentation for {@link
     * com.jogamp.opengl.util.texture.Texture Texture}, although premultiplied
     * colors are used internally.  The default color is opaque white.
     *
     * @param r Red component of the new color
     * @param g Green component of the new color
     * @param b Blue component of the new color
     * @param a Alpha component of the new color
     */
    public void setColor(final float r, final float g, final float b, final float a) {
        glyphRenderer.setColor(r, g, b, a);
    }

    /**
     * Determines the face, style, and size of text.
     *
     * @return Face, style, and size of text (non-null)
     */
    /*@Nonnull*/
    public Font getFont() {
        return font;
    }

    /**
     * Determines if the backing texture is using linear interpolation.
     *
     * @return <tt>true</tt> if the backing texture is using linear interpolation.
     */
    public boolean getSmoothing() {
        return glyphCache.getUseSmoothing();
    }

    /**
     * Changes whether the backing texture will use linear interpolation.
     *
     * <p>In other words, specifies the filtering of the texture that the text
     * renderer is using.  If smoothing is enabled, <tt>GL_LINEAR</tt> will be used.
     * Otherwise it uses <tt>GL_NEAREST</tt>.
     *
     * <p>Defaults to true.
     *
     * <p>A few graphics cards do not behave well when this is enabled,
     * resulting in fuzzy text.
     */
    public void setSmoothing(final boolean smoothing) {
        glyphCache.setUseSmoothing(smoothing);
    }

    /**
     * Changes the transformation matrix used for drawing text in 3D.
     *
     * @param matrix Transformation matrix in column-major order (non-null)
     * @throws NullPointerException if matrix is <tt>null</tt>
     * @throws IndexOutOfBoundsException if length of matrix is less than sixteen
     * @throws IllegalStateException if in orthographic mode
     */
    public void setTransform(/*@Nonnull*/ final float matrix[]) {
        glyphRenderer.setTransform(matrix, false);
    }

    /**
     * Returns <tt>true</tt> if vertex arrays are in use.
     *
     * <p>Indicates whether vertex arrays are being used internally for
     * rendering, or whether text is rendered using the OpenGL immediate mode
     * commands.  Defaults to <tt>true</tt>.
     */
    public boolean getUseVertexArrays() {
        return glyphRenderer.getUseVertexArrays();
    }

    /**
     * Changes whether vertex arrays are in use.
     *
     * <p>This is provided as a concession for certain graphics cards which have
     * poor vertex array performance.  If passed <tt>true</tt>, the text
     * renderer will use vertex arrays or a vertex buffer internally for
     * rendering.  Otherwise it will use immediate mode commands.  Defaults to
     * <tt>true</tt>.
     *
     * @param useVertexArrays <tt>true</tt> to render with vertex arrays
     */
    public void setUseVertexArrays(final boolean useVertexArrays) {
        glyphRenderer.setUseVertexArrays(useVertexArrays);
    }

    //------------------------------------------------------------------
    // Nested classes
    //

    /**
     * Utility supporting more full control over rendering the bitmapped text.
     *
     * <p>Allows customization of whether the backing
     * store text bitmap is full-color or intensity only, the size of
     * each individual rendered text rectangle, and the contents of
     * each individual rendered text string.
     */
    public static interface RenderDelegate {

        /**
         * Renders text into a graphics instance at a specific location.
         *
         * <p>The surrounding region will already have been cleared to the RGB
         * color (0, 0, 0) with zero alpha.  The initial drawing context of the
         * passed Graphics2D will be set to use AlphaComposite.Src, the color
         * white, the Font specified in the TextRenderer's constructor, and the
         * rendering hints specified in the TextRenderer constructor.  Changes
         * made by the end user may be visible in successive calls to this
         * method, but are not guaranteed to be preserved.  Implementations
         * should reset the Graphics2D's state to that desired each time this
         * method is called, in particular those states which are not the
         * defaults.
         *
         * @param g2d Graphics to render into
         * @param str Text to render
         * @param x Location on X axis to render at
         * @param y Location on Y axis to render at
         * @throws NullPointerException if graphics or text is <tt>null</tt>
         */
        void draw(/*@Nonnull*/ Graphics2D g2d, /*@Nonnull*/ String str, int x, int y);

        /**
         * Renders a glyph into a graphics instance at a specific location.
         *
         * <p>The surrounding region will already have been cleared to the RGB
         * color (0, 0, 0) with zero alpha.  The initial drawing context of the
         * passed Graphics2D will be set to use AlphaComposite.Src, the color
         * white, the Font specified in the TextRenderer's constructor, and the
         * rendering hints specified in the TextRenderer constructor.  Changes
         * made by the end user may be visible in successive calls to this
         * method, but are not guaranteed to be preserved.  Implementations
         * should reset the Graphics2D's state to that desired each time this
         * method is called, in particular those states which are not the
         * defaults.
         *
         * @param g2d Graphics to render into
         * @param gv Glyph to render
         * @param x Location on X axis to render at
         * @param y Location on Y axis to render at
         * @throws NullPointerException if graphics or glyph is <tt>null</tt>
         */
        void drawGlyphVector(/*@Nonnull*/ Graphics2D g2d, /*@Nonnull*/ GlyphVector gv, int x, int y);

        /**
         * Computes the bounds of a character sequence relative to the origin.
         *
         * @param cs Text to compute bounds of (non-null)
         * @param font Face, style, and size of font (non-null)
         * @param frc Device-dependent details of how text should be rendered (non-null)
         * @return Rectangle surrounding the text (non-null)
         * @throws NullPointerException if text, font, or font render context is <tt>null</tt>
         */
        Rectangle2D getBounds(/*@Nonnull*/ CharSequence cs,
                              /*@Nonnull*/ Font font,
                              /*@Nonnull*/ FontRenderContext frc);

        /**
         * Computes the bounds of a glyph relative to the origin.
         *
         * @param gv Glyph to compute bounds of (non-null)
         * @param frc Device-dependent details of how text should be rendered (non-null)
         * @return Rectangle surrounding the text (non-null)
         * @throws NullPointerException if glyph or font render context is <tt>null</tt>
         */
        Rectangle2D getBounds(/*@Nonnull*/ GlyphVector gv, /*@Nonnull*/ FontRenderContext frc);

        /**
         * Computes the bounds of a string relative to the origin.
         *
         * @param str Text to compute bounds of (non-null)
         * @param font Face, style, and size of font (non-null)
         * @param frc Device-dependent details of how text should be rendered (non-null)
         * @return Rectangle surrounding the text (non-null)
         * @throws NullPointerException if text, font, or font render context is <tt>null</tt>
         */
        Rectangle2D getBounds(/*@Nonnull*/ String str, /*@Nonnull*/ Font font, /*@Nonnull*/ FontRenderContext frc);

        /**
         * Indicates whether the backing store should be intensity-only or full-color.
         *
         * <p>Note that currently the text renderer does not support full-color.
         * It will throw an {@link UnsupportedOperationException} if the render
         * delegate requests full-color.
         *
         * @return <tt>true</tt> if the backing store should be intensity-only
         */
        boolean intensityOnly();
    }

    /**
     * Simple render delegate if one is not specified by the user.
     */
    public static class DefaultRenderDelegate implements RenderDelegate {

        @Override
        public void draw(final Graphics2D g2d, final String str, final int x, final int y) {
            g2d.drawString(str, x, y);
        }

        @Override
        public void drawGlyphVector(final Graphics2D g2d, final GlyphVector gv, final int x, final int y) {
            g2d.drawGlyphVector(gv, x, y);
        }

        @Override
        public Rectangle2D getBounds(final CharSequence cs, final Font font, final FontRenderContext frc) {
            return getBounds(cs.toString(), font, frc);
        }

        @Override
        public Rectangle2D getBounds(final GlyphVector gv, final FontRenderContext frc) {
            return gv.getVisualBounds();
        }

        @Override
        public Rectangle2D getBounds(final String str, final Font font, final FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc, str), frc);
        }

        @Override
        public boolean intensityOnly() {
            return true;
        }
    }

    /**
     * Utility for coordinating text renderer components.
     */
    private final class Mediator implements GlyphCache.EventListener, GlyphRenderer.EventListener {

        @Override
        public void onGlyphCacheEvent(final GlyphCache.EventType type, final Object data) {
            switch (type) {
            case REALLOCATE:
                flush();
                break;
            case CLEAR:
                glyphProducer.clearGlyphs();
                break;
            case CLEAN:
                glyphProducer.removeGlyph((Glyph) data);
                break;
            }
        }

        @Override
        public void onGlyphRendererEvent(final GlyphRenderer.EventType type) {
            switch (type) {
            case AUTOMATIC_FLUSH:
                final GL gl = GLContext.getCurrentGL();
                glyphCache.update(gl);
                break;
            }
        }
    }

    /**
     * <i>Adapter</i> for a glyph renderer.
     */
    private static final class GlyphRendererAdapter implements GlyphRenderer {

        // Delegate to actually render
        private GlyphRenderer delegate;

        // Listeners added before a delegate is chosen
        private final List<EventListener> listeners;

        // Red, green, blue, and alpha components of color
        private Float r;
        private Float g;
        private Float b;
        private Float a;

        // Transform and whether it's transposed or not
        private float[] transform;
        private Boolean transposed;

        // Whether to use vertex arrays or not
        private boolean useVertexArrays = true;

        GlyphRendererAdapter() {
            this.listeners = new ArrayList<EventListener>();
        }

        @Override
        public void addListener(EventListener listener) {
            if (delegate != null) {
                delegate.addListener(listener);
            } else {
                listeners.add(listener);
            }
        }

        @Override
        public void beginRendering(GL gl, boolean ortho, int width, int height, boolean disableDepthTest) {
            if (delegate == null) {

                // Create the glyph renderer
                delegate = GlyphRendererFactory.createGlyphRenderer(gl);

                // Add the event listeners
                for (EventListener listener : listeners) {
                    delegate.addListener(listener);
                }

                // Set the color
                if ((r != null) && (g != null) && (b != null) && (a != null)) {
                    delegate.setColor(r, g, b, a);
                }

                // Set the transform
                if ((transform != null) && (transposed != null)) {
                    delegate.setTransform(transform, transposed);
                }

                // Set whether to use vertex arrays or not
                delegate.setUseVertexArrays(useVertexArrays);
            }
            delegate.beginRendering(gl, ortho, width, height, disableDepthTest);
        }

        @Override
        public float drawGlyph(GL gl, Glyph glyph, float x, float y, float z, float scale, TextureCoords coords) {
            if (delegate != null) {
                return delegate.drawGlyph(gl, glyph, x, y, z, scale, coords);
            } else {
                throw new IllegalStateException("Must be in render cycle!");
            }
        }

        @Override
        public void endRendering(GL gl) {
            if (delegate != null) {
                delegate.endRendering(gl);
            } else {
                throw new IllegalStateException("Must be in render cycle!");
            }
        }

        @Override
        public void flush(GL gl) {
            if (delegate != null) {
                delegate.flush(gl);
            } else {
                throw new IllegalStateException("Must be in render cycle!");
            }
        }

        @Override
        public void dispose(GL gl) {
            if (delegate != null) {
                delegate.dispose(gl);
            }
        }

        @Override
        public void setColor(float r, float g, float b, float a) {
            if (delegate != null) {
                delegate.setColor(r, g, b, a);
            } else {
                this.r = r;
                this.g = g;
                this.b = b;
                this.a = a;
            }
        }

        @Override
        public void setTransform(float[] value, boolean transpose) {
            if (delegate != null) {
                delegate.setTransform(value, transpose);
            } else {
                this.transform = Arrays.copyOf(value, value.length);
                this.transposed = transpose;
            }
        }

        @Override
        public boolean getUseVertexArrays() {
            if (delegate != null) {
                return delegate.getUseVertexArrays();
            } else {
                return useVertexArrays;
            }
        }

        @Override
        public void setUseVertexArrays(boolean useVertexArrays) {
            if (delegate != null) {
                delegate.setUseVertexArrays(useVertexArrays);
            } else {
                this.useVertexArrays = useVertexArrays;
            }
        }
    }
}
