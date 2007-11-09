/*
 * Copyright (c) 2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */
package com.sun.opengl.util.j2d;

import com.sun.opengl.impl.*;
import com.sun.opengl.impl.packrect.*;
import com.sun.opengl.util.*;
import com.sun.opengl.util.texture.*;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;

// For debugging purposes
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;

import java.nio.*;

import java.text.*;

import java.util.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;


/** Renders bitmapped Java 2D text into an OpenGL window with high
    performance, full Unicode support, and a simple API. Performs
    appropriate caching of text rendering results in an OpenGL texture
    internally to avoid repeated font rasterization. The caching is
    completely automatic, does not require any user intervention, and
    has no visible controls in the public API. <P>

    Using the {@link TextRenderer TextRenderer} is simple. Add a
    "<code>TextRenderer renderer;</code>" field to your {@link
    javax.media.opengl.GLEventListener GLEventListener}. In your {@link
    javax.media.opengl.GLEventListener#init init} method, add:

    <PRE>
    renderer = new TextRenderer(new Font("SansSerif", Font.BOLD, 36));
    </PRE>

    <P> In the {@link javax.media.opengl.GLEventListener#display display} method of your
    {@link javax.media.opengl.GLEventListener GLEventListener}, add:
    <PRE>
    renderer.beginRendering(drawable.getWidth(), drawable.getHeight());
    // optionally set the color
    renderer.setColor(1.0f, 0.2f, 0.2f, 0.8f);
    renderer.draw("Text to draw", xPosition, yPosition);
    // ... more draw commands, color changes, etc.
    renderer.endRendering();
    </PRE>

    Unless you are sharing textures and display lists between OpenGL
    contexts, you do not need to call the {@link #dispose dispose}
    method of the TextRenderer; the OpenGL resources it uses
    internally will be cleaned up automatically when the OpenGL
    context is destroyed. <P>

    <b>Note</b> that the TextRenderer may cause the vertex and texture
    coordinate array buffer bindings to change, or to be unbound. This
    is important to note if you are using Vertex Buffer Objects (VBOs)
    in your application. <P>

    Internally, the renderer uses a rectangle packing algorithm to
    pack both glyphs and full Strings' rendering results (which are
    variable size) onto a larger OpenGL texture. The internal backing
    store is maintained using a {@link
    com.sun.opengl.util.j2d.TextureRenderer TextureRenderer}. A least
    recently used (LRU) algorithm is used to discard previously
    rendered strings; the specific algorithm is undefined, but is
    currently implemented by flushing unused Strings' rendering
    results every few hundred rendering cycles, where a rendering
    cycle is defined as a pair of calls to {@link #beginRendering
    beginRendering} / {@link #endRendering endRendering}.

    @author John Burkey
    @author Kenneth Russell
*/
public class TextRenderer {
    private static final boolean DEBUG = Debug.debug("TextRenderer");
    static final int kSize = 256;

    // Every certain number of render cycles, flush the strings which
    // haven't been used recently
    private static final int CYCLES_PER_FLUSH = 100;

    // The amount of vertical dead space on the backing store before we
    // force a compaction
    private static final float MAX_VERTICAL_FRAGMENTATION = 0.7f;
    static final int kQuadsPerBuffer = 100;
    static final int kCoordsPerVertVerts = 3;
    static final int kCoordsPerVertTex = 2;
    static final int kVertsPerQuad = 4;
    static final int kTotalBufferSizeVerts = kQuadsPerBuffer * kVertsPerQuad;
    static final int kTotalBufferSizeCoordsVerts = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertVerts;
    static final int kTotalBufferSizeCoordsTex = kQuadsPerBuffer * kVertsPerQuad * kCoordsPerVertTex;
    static final int kTotalBufferSizeBytesVerts = kTotalBufferSizeCoordsVerts * 4;
    static final int kTotalBufferSizeBytesTex = kTotalBufferSizeCoordsTex * 4;
    static final int kSizeInBytes_OneVertices_VertexData = kCoordsPerVertVerts * 4;
    static final int kSizeInBytes_OneVertices_TexData = kCoordsPerVertTex * 4;
    private Font font;
    private boolean antialiased;
    private boolean useFractionalMetrics;

    // Whether we're attempting to use automatic mipmap generation support
    private boolean mipmap;
    private RectanglePacker packer;
    private boolean haveMaxSize;
    private RenderDelegate renderDelegate;
    private TextureRenderer cachedBackingStore;
    private Graphics2D cachedGraphics;
    private FontRenderContext cachedFontRenderContext;
    private Map /*<String,Rect>*/ stringLocations = new HashMap /*<String,Rect>*/();
    private GlyphProducer mGlyphProducer;

    // Support tokenization of space-separated words
    // NOTE: not using this at the present time as we aren't producing
    // identical rendering results; may ultimately yield more efficient
    // use of the backing store
    // private boolean splitAtSpaces = !Debug.isPropertyDefined("jogl.TextRenderer.nosplit");
    private boolean splitAtSpaces = false;
    private int spaceWidth = -1;
    private java.util.List tokenizationResults = new ArrayList /*<String>*/();
    private int numRenderCycles;

    // Need to keep track of whether we're in a beginRendering() /
    // endRendering() cycle so we can re-enter the exact same state if
    // we have to reallocate the backing store
    private boolean inBeginEndPair;
    private boolean isOrthoMode;
    private int beginRenderingWidth;
    private int beginRenderingHeight;
    private boolean beginRenderingDepthTestDisabled;

    // For resetting the color after disposal of the old backing store
    private boolean haveCachedColor;
    private float cachedR;
    private float cachedG;
    private float cachedB;
    private float cachedA;
    private Color cachedColor;
    private boolean needToResetColor;

    // For debugging only
    private Frame dbgFrame;

    // Debugging purposes only
    private boolean debugged;
    Pipelined_QuadRenderer mPipelinedQuadRenderer;

    /** Creates a new TextRenderer with the given font, using no
        antialiasing or fractional metrics, and the default
        RenderDelegate. Equivalent to <code>TextRenderer(font, false,
        false)</code>.

        @param font the font to render with
    */
    public TextRenderer(Font font) {
        this(font, false, false, null, false);
    }

    /** Creates a new TextRenderer with the given font, using no
        antialiasing or fractional metrics, and the default
        RenderDelegate. If <CODE>mipmap</CODE> is true, attempts to use
        OpenGL's automatic mipmap generation for better smoothing when
        rendering the TextureRenderer's contents at a distance.
        Equivalent to <code>TextRenderer(font, false, false)</code>.

        @param font the font to render with
        @param mipmap whether to attempt use of automatic mipmap generation
    */
    public TextRenderer(Font font, boolean mipmap) {
        this(font, false, false, null, mipmap);
    }

    /** Creates a new TextRenderer with the given Font, specified font
        properties, and default RenderDelegate. The
        <code>antialiased</code> and <code>useFractionalMetrics</code>
        flags provide control over the same properties at the Java 2D
        level. No mipmap support is requested. Equivalent to
        <code>TextRenderer(font, antialiased, useFractionalMetrics,
        null)</code>.

        @param font the font to render with
        @param antialiased whether to use antialiased fonts
        @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
    */
    public TextRenderer(Font font, boolean antialiased,
                        boolean useFractionalMetrics) {
        this(font, antialiased, useFractionalMetrics, null, false);
    }

    /** Creates a new TextRenderer with the given Font, specified font
        properties, and given RenderDelegate. The
        <code>antialiased</code> and <code>useFractionalMetrics</code>
        flags provide control over the same properties at the Java 2D
        level. The <code>renderDelegate</code> provides more control
        over the text rendered. No mipmap support is requested.

        @param font the font to render with
        @param antialiased whether to use antialiased fonts
        @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
        @param renderDelegate the render delegate to use to draw the
        text's bitmap, or null to use the default one
    */
    public TextRenderer(Font font, boolean antialiased,
                        boolean useFractionalMetrics, RenderDelegate renderDelegate) {
        this(font, antialiased, useFractionalMetrics, renderDelegate, false);
    }

    /** Creates a new TextRenderer with the given Font, specified font
        properties, and given RenderDelegate. The
        <code>antialiased</code> and <code>useFractionalMetrics</code>
        flags provide control over the same properties at the Java 2D
        level. The <code>renderDelegate</code> provides more control
        over the text rendered. If <CODE>mipmap</CODE> is true, attempts
        to use OpenGL's automatic mipmap generation for better smoothing
        when rendering the TextureRenderer's contents at a distance.

        @param font the font to render with
        @param antialiased whether to use antialiased fonts
        @param useFractionalMetrics whether to use fractional font
        metrics at the Java 2D level
        @param renderDelegate the render delegate to use to draw the
        text's bitmap, or null to use the default one
        @param mipmap whether to attempt use of automatic mipmap generation
    */
    public TextRenderer(Font font, boolean antialiased,
                        boolean useFractionalMetrics, RenderDelegate renderDelegate,
                        boolean mipmap) {
        this.font = font;
        this.antialiased = antialiased;
        this.useFractionalMetrics = useFractionalMetrics;
        this.mipmap = mipmap;

        // FIXME: consider adjusting the size based on font size
        // (it will already automatically resize if necessary)
        packer = new RectanglePacker(new Manager(), kSize, kSize);

        if (renderDelegate == null) {
            renderDelegate = new DefaultRenderDelegate();
        }

        this.renderDelegate = renderDelegate;

        mGlyphProducer = new GlyphProducer(getFontRenderContext(),
                                           font.getNumGlyphs());
    }

    /** Returns the bounding rectangle of the given String, assuming it
        was rendered at the origin. See {@link #getBounds(CharSequence)
        getBounds(CharSequence)}. */
    public Rectangle2D getBounds(String str) {
        return getBounds((CharSequence) str);
    }

    /** Returns the bounding rectangle of the given CharSequence,
        assuming it was rendered at the origin. The coordinate system of
        the returned rectangle is Java 2D's, with increasing Y
        coordinates in the downward direction. The relative coordinate
        (0, 0) in the returned rectangle corresponds to the baseline of
        the leftmost character of the rendered string, in similar
        fashion to the results returned by, for example, {@link
        java.awt.font.GlyphVector#getVisualBounds}. Most applications
        will use only the width and height of the returned Rectangle for
        the purposes of centering or justifying the String. It is not
        specified which Java 2D bounds ({@link
        java.awt.font.GlyphVector#getVisualBounds getVisualBounds},
        {@link java.awt.font.GlyphVector#getPixelBounds getPixelBounds},
        etc.) the returned bounds correspond to, although every effort
        is made to ensure an accurate bound. */
    public Rectangle2D getBounds(CharSequence str) {
        // FIXME: this doesn't hit the cache if tokenization is enabled --
        // needs more work
        // Prefer a more optimized approach
        Rect r = null;

        if ((r = (Rect) stringLocations.get(str)) != null) {
            TextData data = (TextData) r.getUserData();

            // Reconstitute the Java 2D results based on the cached values
            return new Rectangle2D.Double(-data.origin().x, -data.origin().y,
                                          r.w(), r.h());
        }

        // Must return a Rectangle compatible with the layout algorithm --
        // must be idempotent
        return normalize(renderDelegate.getBounds(str, font,
                                                  getFontRenderContext()));
    }

    /** Returns the Font this renderer is using. */
    public Font getFont() {
        return font;
    }

    /** Returns a FontRenderContext which can be used for external
        text-related size computations. This object should be considered
        transient and may become invalidated between {@link
        #beginRendering beginRendering} / {@link #endRendering
        endRendering} pairs. */
    public FontRenderContext getFontRenderContext() {
        if (cachedFontRenderContext == null) {
            cachedFontRenderContext = getGraphics2D().getFontRenderContext();
        }

        return cachedFontRenderContext;
    }

    /** Begins rendering with this {@link TextRenderer TextRenderer}
        into the current OpenGL drawable, pushing the projection and
        modelview matrices and some state bits and setting up a
        two-dimensional orthographic projection with (0, 0) as the
        lower-left coordinate and (width, height) as the upper-right
        coordinate. Binds and enables the internal OpenGL texture
        object, sets the texture environment mode to GL_MODULATE, and
        changes the current color to the last color set with this
        TextRenderer via {@link #setColor setColor}. This method
        disables the depth test and is equivalent to
        beginRendering(width, height, true).

        @param width the width of the current on-screen OpenGL drawable
        @param height the height of the current on-screen OpenGL drawable
        @throws javax.media.opengl.GLException If an OpenGL context is not current when this method is called
    */
    public void beginRendering(int width, int height) throws GLException {
        beginRendering(width, height, true);
    }

    /** Begins rendering with this {@link TextRenderer TextRenderer}
        into the current OpenGL drawable, pushing the projection and
        modelview matrices and some state bits and setting up a
        two-dimensional orthographic projection with (0, 0) as the
        lower-left coordinate and (width, height) as the upper-right
        coordinate. Binds and enables the internal OpenGL texture
        object, sets the texture environment mode to GL_MODULATE, and
        changes the current color to the last color set with this
        TextRenderer via {@link #setColor setColor}. Disables the depth
        test if the disableDepthTest argument is true.

        @param width the width of the current on-screen OpenGL drawable
        @param height the height of the current on-screen OpenGL drawable
        @param disableDepthTest whether to disable the depth test
        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void beginRendering(int width, int height, boolean disableDepthTest)
        throws GLException {
        beginRendering(true, width, height, disableDepthTest);
    }

    /** Begins rendering of 2D text in 3D with this {@link TextRenderer
        TextRenderer} into the current OpenGL drawable. Assumes the end
        user is responsible for setting up the modelview and projection
        matrices, and will render text using the {@link #draw3D draw3D}
        method. This method pushes some OpenGL state bits, binds and
        enables the internal OpenGL texture object, sets the texture
        environment mode to GL_MODULATE, and changes the current color
        to the last color set with this TextRenderer via {@link
        #setColor setColor}.

        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void begin3DRendering() throws GLException {
        beginRendering(false, 0, 0, false);
    }

    /** Changes the current color of this TextRenderer to the supplied
        one. The default color is opaque white.

        @param color the new color to use for rendering text
        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void setColor(Color color) throws GLException {
        boolean noNeedForFlush = (haveCachedColor && (cachedColor != null) &&
                                  color.equals(cachedColor));

        if (!noNeedForFlush) {
            flushGlyphPipeline();
        }

        getBackingStore().setColor(color);
        haveCachedColor = true;
        cachedColor = color;
    }

    /** Changes the current color of this TextRenderer to the supplied
        one, where each component ranges from 0.0f - 1.0f. The alpha
        component, if used, does not need to be premultiplied into the
        color channels as described in the documentation for {@link
        com.sun.opengl.util.texture.Texture Texture}, although
        premultiplied colors are used internally. The default color is
        opaque white.

        @param r the red component of the new color
        @param g the green component of the new color
        @param b the blue component of the new color
        @param a the alpha component of the new color, 0.0f = completely
        transparent, 1.0f = completely opaque
        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void setColor(float r, float g, float b, float a)
        throws GLException {
        boolean noNeedForFlush = (haveCachedColor && (cachedColor == null) &&
                                  (r == cachedR) && (g == cachedG) && (b == cachedB) &&
                                  (a == cachedA));

        if (!noNeedForFlush) {
            flushGlyphPipeline();
        }

        getBackingStore().setColor(r, g, b, a);
        haveCachedColor = true;
        cachedR = r;
        cachedG = g;
        cachedB = b;
        cachedA = a;
        cachedColor = null;
    }

    /** Draws the supplied CharSequence at the desired location using
        the renderer's current color. The baseline of the leftmost
        character is at position (x, y) specified in OpenGL coordinates,
        where the origin is at the lower-left of the drawable and the Y
        coordinate increases in the upward direction.

        @param str the string to draw
        @param x the x coordinate at which to draw
        @param y the y coordinate at which to draw
        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void draw(CharSequence str, int x, int y) throws GLException {
        draw3D(str, x, y, 0, 1);
    }

    /** Draws the supplied String at the desired location using the
        renderer's current color. See {@link #draw(CharSequence, int,
        int) draw(CharSequence, int, int)}. */
    public void draw(String str, int x, int y) throws GLException {
        draw3D(str, x, y, 0, 1);
    }

    /** Draws the supplied CharSequence at the desired 3D location using
        the renderer's current color. The baseline of the leftmost
        character is placed at position (x, y, z) in the current
        coordinate system.

        @param str the string to draw
        @param x the x coordinate at which to draw
        @param y the y coordinate at which to draw
        @param z the z coordinate at which to draw
        @param scaleFactor a uniform scale factor applied to the width and height of the drawn rectangle
        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void draw3D(CharSequence str, float x, float y, float z,
                       float scaleFactor) {
        internal_draw3D(str, x, y, z, scaleFactor);
    }

    /** Draws the supplied String at the desired 3D location using the
        renderer's current color. See {@link #draw3D(CharSequence,
        float, float, float, float) draw3D(CharSequence, float, float,
        float, float)}. */
    public void draw3D(String str, float x, float y, float z, float scaleFactor) {
        internal_draw3D(str, x, y, z, scaleFactor);
    }

    /** Returns the pixel width of the given character. */
    public float getCharWidth(char inChar) {
        return mGlyphProducer.getGlyphPixelWidth(inChar);
    }

    /** Causes the TextRenderer to flush any internal caches it may be
        maintaining and draw its rendering results to the screen. This
        should be called after each call to draw() if you are setting
        OpenGL state such as the modelview matrix between calls to
        draw(). */
    public void flush() {
        flushGlyphPipeline();
    }

    /** Ends a render cycle with this {@link TextRenderer TextRenderer}.
        Restores the projection and modelview matrices as well as
        several OpenGL state bits. Should be paired with {@link
        #beginRendering beginRendering}.

        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void endRendering() throws GLException {
        endRendering(true);
    }

    /** Returns the width of the ASCII space character, in pixels, drawn
        in this TextRenderer's font when no scaling or rotation has been
        applied. This is the horizontal advance of the space character.

        @return the width of the space character in the TextRenderer's font
    */
    private int getSpaceWidth() {
        if (spaceWidth < 0) {
            Graphics2D g = getGraphics2D();

            FontRenderContext frc = getFontRenderContext();
            GlyphVector gv = font.createGlyphVector(frc, " ");
            GlyphMetrics metrics = gv.getGlyphMetrics(0);
            spaceWidth = (int) metrics.getAdvanceX();
        }

        return spaceWidth;
    }

    /** Ends a 3D render cycle with this {@link TextRenderer TextRenderer}.
        Restores several OpenGL state bits. Should be paired with {@link
        #begin3DRendering begin3DRendering}.

        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void end3DRendering() throws GLException {
        endRendering(false);
    }

    /** Disposes of all resources this TextRenderer is using. It is not
        valid to use the TextRenderer after this method is called.

        @throws GLException If an OpenGL context is not current when this method is called
    */
    public void dispose() throws GLException {
        packer.dispose();
        packer = null;
        cachedBackingStore = null;
        cachedGraphics = null;
        cachedFontRenderContext = null;

        if (dbgFrame != null) {
            dbgFrame.dispose();
        }
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //
    private static Rectangle2D normalize(Rectangle2D src) {
        // Give ourselves a one-pixel boundary around each string in order
        // to prevent bleeding of nearby Strings due to the fact that we
        // use linear filtering
        return new Rectangle2D.Double((int) Math.floor(src.getMinX() - 1),
                                      (int) Math.floor(src.getMinY() - 1),
                                      (int) Math.ceil(src.getWidth() + 2),
                                      (int) Math.ceil(src.getHeight()) + 2);
    }

    private TextureRenderer getBackingStore() {
        TextureRenderer renderer = (TextureRenderer) packer.getBackingStore();

        if (renderer != cachedBackingStore) {
            // Backing store changed since last time; discard any cached Graphics2D
            if (cachedGraphics != null) {
                cachedGraphics.dispose();
                cachedGraphics = null;
                cachedFontRenderContext = null;
            }

            cachedBackingStore = renderer;
        }

        return cachedBackingStore;
    }

    private Graphics2D getGraphics2D() {
        TextureRenderer renderer = getBackingStore();

        if (cachedGraphics == null) {
            cachedGraphics = renderer.createGraphics();

            // Set up composite, font and rendering hints
            cachedGraphics.setComposite(AlphaComposite.Src);
            cachedGraphics.setColor(Color.WHITE);
            cachedGraphics.setFont(font);
            cachedGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                            (antialiased ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                                             : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF));
            cachedGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                                            (useFractionalMetrics
                                             ? RenderingHints.VALUE_FRACTIONALMETRICS_ON
                                             : RenderingHints.VALUE_FRACTIONALMETRICS_OFF));
        }

        return cachedGraphics;
    }

    private void beginRendering(boolean ortho, int width, int height,
                                boolean disableDepthTestForOrtho) {
        if (DEBUG && !debugged) {
            debug();
        }

        inBeginEndPair = true;
        isOrthoMode = ortho;
        beginRenderingWidth = width;
        beginRenderingHeight = height;
        beginRenderingDepthTestDisabled = disableDepthTestForOrtho;

        if (ortho) {
            getBackingStore().beginOrthoRendering(width, height,
                                                  disableDepthTestForOrtho);
        } else {
            getBackingStore().begin3DRendering();
        }

        GL gl = GLU.getCurrentGL();

        // Push client attrib bits used by the pipelined quad renderer
        gl.glPushClientAttrib((int) GL.GL_ALL_CLIENT_ATTRIB_BITS);

        if (!haveMaxSize) {
            // Query OpenGL for the maximum texture size and set it in the
            // RectanglePacker to keep it from expanding too large
            int[] sz = new int[1];
            gl.glGetIntegerv(GL.GL_MAX_TEXTURE_SIZE, sz, 0);
            packer.setMaxSize(sz[0], sz[0]);
            haveMaxSize = true;
        }

        if (needToResetColor && haveCachedColor) {
            if (cachedColor == null) {
                getBackingStore().setColor(cachedR, cachedG, cachedB, cachedA);
            } else {
                getBackingStore().setColor(cachedColor);
            }

            needToResetColor = false;
        }

        // Disable future attempts to use mipmapping if TextureRenderer
        // doesn't support it
        if (mipmap && !getBackingStore().isUsingAutoMipmapGeneration()) {
            if (DEBUG) {
                System.err.println("Disabled mipmapping in TextRenderer");
            }

            mipmap = false;
        }
    }

    private void endRendering(boolean ortho) throws GLException {
        flushGlyphPipeline();

        inBeginEndPair = false;

        GL gl = GLU.getCurrentGL();

        // Pop client attrib bits used by the pipelined quad renderer
        gl.glPopClientAttrib();

        // The OpenGL spec is unclear about whether this changes the
        // buffer bindings, so preemptively zero out the GL_ARRAY_BUFFER
        // binding
        if (gl.isExtensionAvailable("GL_VERSION_1_5")) {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
        }

        if (ortho) {
            getBackingStore().endOrthoRendering();
        } else {
            getBackingStore().end3DRendering();
        }

        if (++numRenderCycles >= CYCLES_PER_FLUSH) {
            numRenderCycles = 0;

            if (DEBUG) {
                System.err.println("Clearing unused entries in endRendering()");
            }

            clearUnusedEntries();
        }
    }

    private void tokenize(CharSequence str) {
        // Avoid lots of little allocations per render
        tokenizationResults.clear();

        if (!splitAtSpaces) {
            tokenizationResults.add(str.toString());
        } else {
            int startChar = 0;
            char c = (char) 0;
            int len = str.length();
            int i = 0;

            while (i < len) {
                if (str.charAt(i) == ' ') {
                    // Terminate any substring
                    if (startChar < i) {
                        tokenizationResults.add(str.subSequence(startChar, i)
                                                .toString());
                    } else {
                        tokenizationResults.add(null);
                    }

                    startChar = i + 1;
                }

                ++i;
            }

            // Add on any remaining (all?) characters
            if (startChar == 0) {
                tokenizationResults.add(str);
            } else if (startChar < len) {
                tokenizationResults.add(str.subSequence(startChar, len)
                                        .toString());
            }
        }
    }

    private void clearUnusedEntries() {
        final java.util.List deadRects = new ArrayList /*<Rect>*/();

        // Iterate through the contents of the backing store, removing
        // text strings that haven't been used recently
        packer.visit(new RectVisitor() {
                public void visit(Rect rect) {
                    TextData data = (TextData) rect.getUserData();

                    if (data.used()) {
                        data.clearUsed();
                    } else {
                        deadRects.add(rect);
                    }
                }
            });

        for (Iterator iter = deadRects.iterator(); iter.hasNext();) {
            Rect r = (Rect) iter.next();
            packer.remove(r);
            stringLocations.remove(((TextData) r.getUserData()).string());

            int unicodeToClearFromCache = ((TextData) r.getUserData()).unicodeID;

            if (unicodeToClearFromCache > 0) {
                mGlyphProducer.clearCacheEntry(unicodeToClearFromCache);
            }

            //      if (DEBUG) {
            //        Graphics2D g = getGraphics2D();
            //        g.setComposite(AlphaComposite.Clear);
            //        g.fillRect(r.x(), r.y(), r.w(), r.h());
            //        g.setComposite(AlphaComposite.Src);
            //      }
        }

        // If we removed dead rectangles this cycle, try to do a compaction
        float frag = packer.verticalFragmentationRatio();

        if (!deadRects.isEmpty() && (frag > MAX_VERTICAL_FRAGMENTATION)) {
            if (DEBUG) {
                System.err.println(
                                   "Compacting TextRenderer backing store due to vertical fragmentation " +
                                   frag);
            }

            packer.compact();
        }

        if (DEBUG) {
            getBackingStore().markDirty(0, 0, getBackingStore().getWidth(),
                                        getBackingStore().getHeight());
        }
    }

    private void internal_draw3D(CharSequence str, float x, float y, float z,
                                 float scaleFactor) {
        int drawingState = DrawingState.fast;

        while (drawingState != DrawingState.finished) {
            GlyphsList glyphs = mGlyphProducer.getGlyphs(str);

            if (drawingState == DrawingState.fast) {
                x += drawGlyphs(glyphs, x, y, z, scaleFactor);
                str = glyphs.remaining;
                drawingState = glyphs.nextState;
            } else if (drawingState == DrawingState.robust) {
                this.draw3D_ROBUST(str, x, y, z, scaleFactor);
                drawingState = DrawingState.finished;
            }
        }
    }

    private void flushGlyphPipeline() {
        if (mPipelinedQuadRenderer != null) {
            mPipelinedQuadRenderer.draw();
        }
    }

    private float drawGlyphs(GlyphsList inGlyphs, float inX, float inY,
                             float z, float scaleFactor) {
        float xOffset = 0;

        try {
            if (mPipelinedQuadRenderer == null) {
                mPipelinedQuadRenderer = new Pipelined_QuadRenderer();
            }

            TextureRenderer renderer = getBackingStore();
            // Handles case where NPOT texture is used for backing store
            TextureCoords wholeImageTexCoords = renderer.getTexture().getImageTexCoords();
            float xScale = wholeImageTexCoords.right();
            float yScale = wholeImageTexCoords.bottom();

            for (int i = 0; i < inGlyphs.length; i++) {
                Rect rect = inGlyphs.textureSourceRect[i];
                TextData data = (TextData) rect.getUserData();
                data.markUsed();

                float x = (inX + xOffset) - (scaleFactor * data.origin().x);
                float y = inY - (scaleFactor * (rect.h() - data.origin().y));

                int texturex = rect.x(); // avoid overpump of textureUpload path by not triggering sync every quad, instead doing it on flushGlyphPipeline
                int texturey = renderer.getHeight() - rect.y() - rect.h();
                int width = rect.w();
                int height = rect.h();

                float tx1 = xScale * (float) texturex / (float) renderer.getWidth();
                float ty1 = yScale * (1.0f -
                                      ((float) texturey / (float) renderer.getHeight()));
                float tx2 = xScale * (float) (texturex + width) / (float) renderer.getWidth();
                float ty2 = yScale * (1.0f -
                                      ((float) (texturey + height) / (float) renderer.getHeight()));

                mPipelinedQuadRenderer.glTexCoord2f(tx1, ty1);
                mPipelinedQuadRenderer.glVertex3f(x, y, z);
                mPipelinedQuadRenderer.glTexCoord2f(tx2, ty1);
                mPipelinedQuadRenderer.glVertex3f(x + (width * scaleFactor), y,
                                                  z);
                mPipelinedQuadRenderer.glTexCoord2f(tx2, ty2);
                mPipelinedQuadRenderer.glVertex3f(x + (width * scaleFactor),
                                                  y + (height * scaleFactor), z);
                mPipelinedQuadRenderer.glTexCoord2f(tx1, ty2);
                mPipelinedQuadRenderer.glVertex3f(x,
                                                  y + (height * scaleFactor), z);

                xOffset += (inGlyphs.advances[i] * scaleFactor); // note the advances.. I had to use this to get proper kerning.
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return xOffset;
    }

    private float drawGlyphsSIMPLE(GlyphsList inGlyphs, float x, float y,
                                   float z, float scaleFactor) // unused, for reference, debugging
    {
        TextureRenderer renderer = getBackingStore();

        int xOffset = 0;

        for (int i = 0; i < inGlyphs.length; i++) {
            Rect rect = inGlyphs.textureSourceRect[i];

            if (rect != null) {
                TextData data = (TextData) rect.getUserData();
                data.markUsed();

                renderer.draw3DRect((x + xOffset) -
                                    (scaleFactor * data.origin().x), // forces upload every new glyph
                                    y - (scaleFactor * (rect.h() - data.origin().y)), z,
                                    rect.x(), renderer.getHeight() - rect.y() - rect.h(),
                                    rect.w(), rect.h(), scaleFactor);

                xOffset += (int) (((inGlyphs.advances[i]) * scaleFactor) +
                                  0.5f); // note the advances.. I had to use this to get proper kerning.
            }
        }

        return xOffset;
    }

    private void draw3D_ROBUST(CharSequence str, float x, float y, float z,
                               float scaleFactor) {
        // Split up the string into space-separated pieces
        tokenize(str);

        int xOffset = 0;

        for (Iterator iter = tokenizationResults.iterator(); iter.hasNext();) {
            String curStr = (String) iter.next(); // no tokenization needed, because it was done to shrink # of uniques

            if (curStr != null) {
                // Look up the string on the backing store
                Rect rect = (Rect) stringLocations.get(curStr);

                if (rect == null) {
                    // Rasterize this string and place it on the backing store
                    Graphics2D g = getGraphics2D();
                    Rectangle2D bbox = normalize(renderDelegate.getBounds(curStr, font, getFontRenderContext()));
                    Point origin = new Point((int) -bbox.getMinX(),
                                             (int) -bbox.getMinY());
                    rect = new Rect(0, 0, (int) bbox.getWidth(),
                                    (int) bbox.getHeight(),
                                    new TextData(curStr, origin, -1));

                    packer.add(rect);
                    stringLocations.put(curStr, rect);

                    // Re-fetch the Graphics2D in case the addition of the rectangle
                    // caused the old backing store to be thrown away
                    g = getGraphics2D();

                    // OK, should now have an (x, y) for this rectangle; rasterize
                    // the String
                    // FIXME: need to verify that this causes the String to be
                    // rasterized fully into the bounding rectangle
                    int strx = rect.x() + origin.x;
                    int stry = rect.y() + origin.y;

                    // Clear out the area we're going to draw into
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
                    g.setComposite(AlphaComposite.Src);

                    // Draw the string
                    renderDelegate.draw(g, curStr, strx, stry);

                    // Mark this region of the TextureRenderer as dirty
                    getBackingStore().markDirty(rect.x(), rect.y(), rect.w(),
                                                rect.h());
                }

                // OK, now draw the portion of the backing store to the screen
                TextureRenderer renderer = getBackingStore();

                // NOTE that the rectangles managed by the packer have their
                // origin at the upper-left but the TextureRenderer's origin is
                // at its lower left!!!
                TextData data = (TextData) rect.getUserData();
                data.markUsed();

                // Align the leftmost point of the baseline to the (x, y, z) coordinate requested
                renderer.draw3DRect((x + xOffset) -
                                    (scaleFactor * data.origin().x),
                                    y - (scaleFactor * (rect.h() - data.origin().y)), z,
                                    rect.x(), renderer.getHeight() - rect.y() - rect.h(),
                                    rect.w(), rect.h(), scaleFactor);
                xOffset += (rect.w() * scaleFactor);
            }

            xOffset += (getSpaceWidth() * scaleFactor);
        }
    }

    //----------------------------------------------------------------------
    // Debugging functionality
    //
    private void debug() {
        dbgFrame = new Frame("TextRenderer Debug Output");

        GLCanvas dbgCanvas = new GLCanvas(new GLCapabilities(), null,
                                          GLContext.getCurrent(), null);
        dbgCanvas.addGLEventListener(new DebugListener(dbgFrame));
        dbgFrame.add(dbgCanvas);

        final FPSAnimator anim = new FPSAnimator(dbgCanvas, 10);
        dbgFrame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    // Run this on another thread than the AWT event queue to
                    // make sure the call to Animator.stop() completes before
                    // exiting
                    new Thread(new Runnable() {
                            public void run() {
                                anim.stop();
                            }
                        }).start();
                }
            });
        dbgFrame.setSize(kSize, kSize);
        dbgFrame.setVisible(true);
        anim.start();
        debugged = true;
    }

    /** Class supporting more full control over the process of rendering
        the bitmapped text. Allows customization of whether the backing
        store text bitmap is full-color or intensity only, the size of
        each individual rendered text rectangle, and the contents of
        each individual rendered text string. The default implementation
        of this interface uses an intensity-only texture, a
        closely-cropped rectangle around the text, and renders text
        using the color white, which is modulated by the set color
        during the rendering process. */
    public static interface RenderDelegate {
        /** Indicates whether the backing store of this TextRenderer
            should be intensity-only (the default) or full-color. */
        public boolean intensityOnly();

        /** Computes the bounds of the given String relative to the
            origin. */
        public Rectangle2D getBounds(String str, Font font,
                                     FontRenderContext frc);

        /** Computes the bounds of the given character sequence relative
            to the origin. */
        public Rectangle2D getBounds(CharSequence str, Font font,
                                     FontRenderContext frc);

        /** Computes the bounds of the given GlyphVector, already
            assumed to have been created for a particular Font,
            relative to the origin. */
        public Rectangle2D getBounds(GlyphVector gv, FontRenderContext frc);

        /** Render the passed character sequence at the designated
            location using the supplied Graphics2D instance. The
            surrounding region will already have been cleared to the RGB
            color (0, 0, 0) with zero alpha. The initial drawing context
            of the passed Graphics2D will be set to use
            AlphaComposite.Src, the color white, the Font specified in the
            TextRenderer's constructor, and the rendering hints specified
            in the TextRenderer constructor.  Changes made by the end user
            may be visible in successive calls to this method, but are not
            guaranteed to be preserved.  Implementors of this method
            should reset the Graphics2D's state to that desired each time
            this method is called, in particular those states which are
            not the defaults. */
        public void draw(Graphics2D graphics, String str, int x, int y);

        /** Render the passed GlyphVector at the designated location using
            the supplied Graphics2D instance. The surrounding region will
            already have been cleared to the RGB color (0, 0, 0) with zero
            alpha. The initial drawing context of the passed Graphics2D
            will be set to use AlphaComposite.Src, the color white, the
            Font specified in the TextRenderer's constructor, and the
            rendering hints specified in the TextRenderer constructor.
            Changes made by the end user may be visible in successive
            calls to this method, but are not guaranteed to be preserved.
            Implementors of this method should reset the Graphics2D's
            state to that desired each time this method is called, in
            particular those states which are not the defaults. */
        public void drawGlyphVector(Graphics2D graphics, GlyphVector str,
                                    int x, int y);
    }

    private static class MapCharSequenceToGlyphVector
        implements CharacterIterator {
        CharSequence mSequence;
        int mLength;
        int mCurrentIndex;

        MapCharSequenceToGlyphVector() {
        }

        MapCharSequenceToGlyphVector(CharSequence sequence) {
            initFromCharSequence(sequence);
        }

        public void initFromCharSequence(CharSequence sequence) {
            mSequence = sequence;
            mLength = mSequence.length();
            mCurrentIndex = 0;
        }

        public char last() {
            mCurrentIndex = Math.max(0, mLength - 1);

            return current();
        }

        public char current() {
            if ((mLength == 0) || (mCurrentIndex >= mLength)) {
                return CharacterIterator.DONE;
            }

            return mSequence.charAt(mCurrentIndex);
        }

        public char next() {
            mCurrentIndex++;

            return current();
        }

        public char previous() {
            mCurrentIndex = Math.max(mCurrentIndex - 1, 0);

            return current();
        }

        public char setIndex(int position) {
            mCurrentIndex = position;

            return current();
        }

        public int getBeginIndex() {
            return 0;
        }

        public int getEndIndex() {
            return mLength;
        }

        public int getIndex() {
            return mCurrentIndex;
        }

        public Object clone() {
            MapCharSequenceToGlyphVector iter = new MapCharSequenceToGlyphVector(mSequence);
            iter.mCurrentIndex = mCurrentIndex;

            return iter;
        }

        public char first() {
            if (mLength == 0) {
                return CharacterIterator.DONE;
            }

            mCurrentIndex = 0;

            return current();
        }
    }

    // Data associated with each rectangle of text
    static class TextData {
        int unicodeID;
        private String str; // Back-pointer to String this TextData describes

        // The following must be defined and used VERY precisely. This is
        // the offset from the upper-left corner of this rectangle (Java
        // 2D coordinate system) at which the string must be rasterized in
        // order to fit within the rectangle -- the leftmost point of the
        // baseline.
        private Point origin;
        private boolean used; // Whether this text was used recently

        TextData(String str, Point origin, int unicodeID) {
            this.str = str;
            this.origin = origin;
            this.unicodeID = unicodeID;
        }

        String string() {
            return str;
        }

        Point origin() {
            return origin;
        }

        boolean used() {
            return used;
        }

        void markUsed() {
            used = true;
        }

        void clearUsed() {
            used = false;
        }
    }

    class Manager implements BackingStoreManager {
        private Graphics2D g;

        public Object allocateBackingStore(int w, int h) {
            // FIXME: should consider checking Font's attributes to see
            // whether we're likely to need to support a full RGBA backing
            // store (i.e., non-default Paint, foreground color, etc.), but
            // for now, let's just be more efficient
            TextureRenderer renderer;

            if (renderDelegate.intensityOnly()) {
                renderer = TextureRenderer.createAlphaOnlyRenderer(w, h, mipmap);
            } else {
                renderer = new TextureRenderer(w, h, true, mipmap);
            }

            if (DEBUG) {
                System.err.println(" TextRenderer allocating backing store " +
                                   w + " x " + h);
            }

            return renderer;
        }

        public void deleteBackingStore(Object backingStore) {
            ((TextureRenderer) backingStore).dispose();
        }

        public boolean preExpand(Rect cause, int attemptNumber) {
            // Only try this one time; clear out potentially obsolete entries
            // NOTE: this heuristic and the fact that it clears the used bit
            // of all entries seems to cause cycling of entries in some
            // situations, where the backing store becomes small compared to
            // the amount of text on the screen (see the TextFlow demo) and
            // the entries continually cycle in and out of the backing
            // store, decreasing performance. If we added a little age
            // information to the entries, and only cleared out entries
            // above a certain age, this behavior would be eliminated.
            // However, it seems the system usually stabilizes itself, so
            // for now we'll just keep things simple. Note that if we don't
            // clear the used bit here, the backing store tends to increase
            // very quickly to its maximum size, at least with the TextFlow
            // demo when the text is being continually re-laid out.
            if (attemptNumber == 0) {
                if (DEBUG) {
                    System.err.println(
                                       "Clearing unused entries in preExpand(): attempt number " +
                                       attemptNumber);
                }

                clearUnusedEntries();

                return true;
            }

            return false;
        }

        public void additionFailed(Rect cause, int attemptNumber) {
            // Heavy hammer -- might consider doing something different
            packer.clear();
            stringLocations.clear();
            mGlyphProducer.clearAllCacheEntries();

            if (DEBUG) {
                System.err.println(
                                   " *** Cleared all text because addition failed ***");
            }
        }

        public void beginMovement(Object oldBackingStore, Object newBackingStore) {
            // Exit the begin / end pair if necessary
            if (inBeginEndPair) {
                // Draw any outstanding glyphs
                flush();

                GL gl = GLU.getCurrentGL();

                // Pop client attrib bits used by the pipelined quad renderer
                gl.glPopClientAttrib();

                // The OpenGL spec is unclear about whether this changes the
                // buffer bindings, so preemptively zero out the GL_ARRAY_BUFFER
                // binding
                if (gl.isExtensionAvailable("GL_VERSION_1_5")) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
                }

                if (isOrthoMode) {
                    ((TextureRenderer) oldBackingStore).endOrthoRendering();
                } else {
                    ((TextureRenderer) oldBackingStore).end3DRendering();
                }
            }

            TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
            g = newRenderer.createGraphics();
        }

        public void move(Object oldBackingStore, Rect oldLocation,
                         Object newBackingStore, Rect newLocation) {
            TextureRenderer oldRenderer = (TextureRenderer) oldBackingStore;
            TextureRenderer newRenderer = (TextureRenderer) newBackingStore;

            if (oldRenderer == newRenderer) {
                // Movement on the same backing store -- easy case
                g.copyArea(oldLocation.x(), oldLocation.y(), oldLocation.w(),
                           oldLocation.h(), newLocation.x() - oldLocation.x(),
                           newLocation.y() - oldLocation.y());
            } else {
                // Need to draw from the old renderer's image into the new one
                Image img = oldRenderer.getImage();
                g.drawImage(img, newLocation.x(), newLocation.y(),
                            newLocation.x() + newLocation.w(),
                            newLocation.y() + newLocation.h(), oldLocation.x(),
                            oldLocation.y(), oldLocation.x() + oldLocation.w(),
                            oldLocation.y() + oldLocation.h(), null);
            }
        }

        public void endMovement(Object oldBackingStore, Object newBackingStore) {
            g.dispose();

            // Sync the whole surface
            TextureRenderer newRenderer = (TextureRenderer) newBackingStore;
            newRenderer.markDirty(0, 0, newRenderer.getWidth(),
                                  newRenderer.getHeight());

            // Re-enter the begin / end pair if necessary
            if (inBeginEndPair) {
                if (isOrthoMode) {
                    ((TextureRenderer) newBackingStore).beginOrthoRendering(beginRenderingWidth,
                                                                            beginRenderingHeight, beginRenderingDepthTestDisabled);
                } else {
                    ((TextureRenderer) newBackingStore).begin3DRendering();
                }

                // Push client attrib bits used by the pipelined quad renderer
                GL gl = GLU.getCurrentGL();
                gl.glPushClientAttrib((int) GL.GL_ALL_CLIENT_ATTRIB_BITS);

                if (haveCachedColor) {
                    if (cachedColor == null) {
                        ((TextureRenderer) newBackingStore).setColor(cachedR,
                                                                     cachedG, cachedB, cachedA);
                    } else {
                        ((TextureRenderer) newBackingStore).setColor(cachedColor);
                    }
                }
            } else {
                needToResetColor = true;
            }
        }
    }

    public static class DefaultRenderDelegate implements RenderDelegate {
        public boolean intensityOnly() {
            return true;
        }

        public Rectangle2D getBounds(CharSequence str, Font font,
                                     FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc,
                                                    new MapCharSequenceToGlyphVector(str)),
                             frc);
        }

        public Rectangle2D getBounds(String str, Font font,
                                     FontRenderContext frc) {
            return getBounds(font.createGlyphVector(frc, str), frc);
        }

        public Rectangle2D getBounds(GlyphVector gv, FontRenderContext frc) {
            return gv.getPixelBounds(frc, 0, 0);
        }

        public void drawGlyphVector(Graphics2D graphics, GlyphVector str,
                                    int x, int y) {
            graphics.drawGlyphVector(str, x, y);
        }

        public void draw(Graphics2D graphics, String str, int x, int y) {
            graphics.drawString(str, x, y);
        }
    }

    //----------------------------------------------------------------------
    // Glyph-by-glyph rendering support
    //
    private static class DrawingState {
        public static final int fast = 1;
        public static final int robust = 2;
        public static final int finished = 3;
    }

    class GlyphsUploadList {
        int numberOfNewGlyphs;
        GlyphVector[] glyphVector;
        Rectangle2D[] glyphBounds;
        int[] renderIndex;
        int[] newGlyphs;
        char[] newUnicodes;

        void prepGlyphForUpload(char inUnicodeID, int inGlyphID,
                                Rectangle2D inBounds, int inI, GlyphVector inGv) {
            int slot = this.numberOfNewGlyphs;

            this.newUnicodes[slot] = inUnicodeID;
            this.newGlyphs[slot] = inGlyphID;
            this.glyphBounds[slot] = inBounds;
            this.renderIndex[slot] = inI;
            this.glyphVector[slot] = inGv;
            this.numberOfNewGlyphs++;
        }

        void uploadAnyNewGlyphs(GlyphsList outList, GlyphProducer mapper) {
            for (int i = 0; i < this.numberOfNewGlyphs; i++) {
                if (mapper.unicodes2Glyphs[this.newUnicodes[i]] == mapper.undefined) {
                    Rectangle2D bbox = normalize(this.glyphBounds[i]);
                    Point origin = new Point((int) -bbox.getMinX(),
                                             (int) -bbox.getMinY());
                    Rect rect = new Rect(0, 0, (int) bbox.getWidth(),
                                         (int) bbox.getHeight(),
                                         new TextData(null, origin, this.newUnicodes[i]));
                    GlyphVector gv = this.glyphVector[i];
                    this.glyphVector[i] = null; // <--- dont need this anymore, so null it

                    packer.add(rect);

                    mapper.glyphRectForTextureMapping[this.newGlyphs[i]] = rect;
                    outList.textureSourceRect[this.renderIndex[i]] = rect;
                    mapper.unicodes2Glyphs[this.newUnicodes[i]] = this.newGlyphs[i]; // i do this here, so if i get two upload requests for same glyph, we handle it correctly

                    Graphics2D g = getGraphics2D();

                    // OK, should now have an (x, y) for this rectangle; rasterize
                    // the String
                    // FIXME: need to verify that this causes the String to be
                    // rasterized fully into the bounding rectangle
                    int strx = rect.x() + origin.x;
                    int stry = rect.y() + origin.y;

                    // ---1st frame performance gating factor---
                    // Clear out the area we're going to draw into
                    // //-- only if we reuse backing store.  Do we do this?  If so, we should have a flag that says we need to clear? or do it in clearSpace itself
                    g.setComposite(AlphaComposite.Clear);
                    g.fillRect(rect.x(), rect.y(), rect.w(), rect.h());
                    g.setComposite(AlphaComposite.Src);

                    // Draw the string
                    renderDelegate.drawGlyphVector(g, gv, strx, stry);

                    // Mark this region of the TextureRenderer as dirty
                    getBackingStore().markDirty(rect.x(), rect.y(), rect.w(),
                                                rect.h());
                } else {
                    outList.textureSourceRect[this.renderIndex[i]] = mapper.glyphRectForTextureMapping[this.newGlyphs[i]];
                }
            }

            this.numberOfNewGlyphs = 0;
        }

        public void allocateSpace(int inLength) {
            int allocLength = Math.max(inLength, 100);

            if ((glyphVector == null) || (glyphVector.length < allocLength)) {
                glyphVector = new GlyphVector[allocLength];
                glyphBounds = new Rectangle2D[allocLength];
                renderIndex = new int[allocLength];
                newGlyphs = new int[allocLength];
                newUnicodes = new char[allocLength];
            }
        }
    }

    static class GlyphsList {
        int /* DrawingState */ nextState;
        CharSequence remaining;
        float[] advances;
        float totalAdvance;
        Rect[] textureSourceRect;
        int length;

        public void allocateSpace(int inLength) {
            int allocLength = Math.max(inLength, 100);

            if ((advances == null) || (advances.length < allocLength)) {
                advances = new float[allocLength];
                textureSourceRect = new Rect[allocLength];
            }
        }
    }

    class GlyphProducer {
        final int undefined = -2;
        final int needComplex = -1;
        FontRenderContext fontRenderContext;
        GlyphsList glyphsOutput = new GlyphsList();
        GlyphsUploadList glyphsToUpload = new GlyphsUploadList();
        char[] unicodes;
        int[] unicodes2Glyphs;
        char[] singleUnicode;
        Rect[] glyphRectForTextureMapping;
        float[] advances;
        MapCharSequenceToGlyphVector iter = new MapCharSequenceToGlyphVector();
        char[] tempChars = new char[1];

        GlyphProducer(FontRenderContext frc, int fontLengthInGlyphs) {
            fontRenderContext = frc;

            if (advances == null) {
                advances = new float[fontLengthInGlyphs];
                glyphRectForTextureMapping = new Rect[fontLengthInGlyphs];
                unicodes2Glyphs = new int[512];
                singleUnicode = new char[1];
                clearAllCacheEntries();
            }
        }

        public void clearCacheEntry(int unicodeID) {
            unicodes2Glyphs[unicodeID] = undefined;
        }

        public void clearAllCacheEntries() {
            for (int i = 0; i < unicodes2Glyphs.length; i++) {
                unicodes2Glyphs[i] = undefined;
            }
        }

        public void allocateSpace(int length) {
            length = Math.max(length, 100);

            if ((unicodes == null) || (unicodes.length < length)) {
                unicodes = new char[length];
            }

            glyphsToUpload.allocateSpace(length);
            glyphsOutput.allocateSpace(length);
        }

        float getGlyphPixelWidth(char unicodeID) {
            int glyphID = undefined;

            if (unicodeID < unicodes2Glyphs.length) // <--- could support the rare high unicode better later
                {
                    glyphID = unicodes2Glyphs[unicodeID]; // Check to see if we have already encountered this unicode
                }

            if (glyphID != undefined) // if we haven't, we must get some its attributes, and prep for upload
                {
                    return advances[glyphID];
                } else {
                    tempChars[0] = unicodeID;

                    GlyphVector fullRunGlyphVector = font.createGlyphVector(fontRenderContext,
                                                                            tempChars);

                    return fullRunGlyphVector.getGlyphMetrics(0).getAdvance();
                }

            //            return -1;
        }

        GlyphsList puntToRobust(CharSequence inString) {
            glyphsOutput.nextState = DrawingState.robust;
            glyphsOutput.remaining = inString;
            // Reset the glyph uploader
            glyphsToUpload.numberOfNewGlyphs = 0;
            // Reset the glyph list
            glyphsOutput.length = 0;
            glyphsOutput.totalAdvance = 0;

            return glyphsOutput;
        }

        GlyphsList getGlyphs(CharSequence inString) {
            float fontSize = font.getSize();

            if (fontSize > 128) {
                glyphsOutput.nextState = DrawingState.robust;
                glyphsOutput.remaining = inString;
            }

            int length = inString.length();
            allocateSpace(length);

            iter.initFromCharSequence(inString);

            GlyphVector fullRunGlyphVector = font.createGlyphVector(fontRenderContext,
                                                                    iter);
            boolean complex = (fullRunGlyphVector.getLayoutFlags() != 0);
            int lengthInGlyphs = fullRunGlyphVector.getNumGlyphs();

            if (complex) {
                return puntToRobust(inString);
            }

            TextureRenderer renderer = getBackingStore();

            float totalAdvanceUploaded = 0;
            float cacheSize = renderer.getWidth() * renderer.getHeight();

            for (int i = 0; i < lengthInGlyphs; i++) {
                float advance;

                char unicodeID = inString.charAt(i);

                if (unicodeID >= unicodes2Glyphs.length) { // <-- -could support these better
                    return puntToRobust(inString);
                }

                int glyphID = unicodes2Glyphs[unicodeID]; // Check to see if we have already encountered this unicode

                if (glyphID == undefined) { // if we haven't, we must get some its attributes, and prep for upload
                    GlyphMetrics metrics = fullRunGlyphVector.getGlyphMetrics(i);
                    singleUnicode[0] = unicodeID;

                    GlyphVector gv = font.createGlyphVector(fontRenderContext,
                                                            singleUnicode); // need this to get single bitmaps
                    glyphID = gv.getGlyphCode(0);
                    // Have seen huge glyph codes (65536) coming out of some fonts in some Unicode situations
                    if (glyphID >= advances.length) {
                        return puntToRobust(inString);
                    }
                    advance = metrics.getAdvance();
                    advances[glyphID] = advance;

                    glyphsToUpload.prepGlyphForUpload(unicodeID, glyphID,
                                                      renderDelegate.getBounds(gv, fontRenderContext), i, gv);

                    totalAdvanceUploaded += advance;
                } else {
                    Rect r = glyphRectForTextureMapping[glyphID];
                    glyphsOutput.textureSourceRect[i] = r;

                    TextData data = (TextData) r.getUserData();
                    data.markUsed();

                    advance = advances[glyphID];
                }

                glyphsOutput.advances[i] = advance;
                glyphsOutput.totalAdvance += advance;

                if ((totalAdvanceUploaded * fontSize) > (0.25f * cacheSize)) // note -- if the incoming string is bigger than 1/4 the total font cache, start segmenting glyph stream into bite sized pieces
                    {
                        glyphsToUpload.uploadAnyNewGlyphs(glyphsOutput, this);
                        glyphsOutput.length = i + 1;
                        glyphsOutput.remaining = inString.subSequence(i + 1, length);
                        glyphsOutput.nextState = DrawingState.fast;

                        return glyphsOutput;
                    }
            }

            glyphsOutput.length = lengthInGlyphs;
            glyphsToUpload.uploadAnyNewGlyphs(glyphsOutput, this);
            glyphsOutput.nextState = DrawingState.finished;

            return glyphsOutput;
        }
    }

    class Pipelined_QuadRenderer {
        int mOutstandingGlyphsVerticesPipeline = 0;
        FloatBuffer mTexCoords;
        FloatBuffer mVertCoords;
        boolean usingVBOs;
        int mVBO_For_ResuableTileVertices;
        int mVBO_For_ResuableTileTexCoords;

        Pipelined_QuadRenderer() {
            GL gl = GLU.getCurrentGL();
            mVertCoords = BufferUtil.newFloatBuffer(kTotalBufferSizeCoordsVerts);
            mTexCoords = BufferUtil.newFloatBuffer(kTotalBufferSizeCoordsTex);

            usingVBOs = (gl.isExtensionAvailable("GL_VERSION_1_5"));

            if (usingVBOs) {
                int[] vbos = new int[2];
                gl.glGenBuffers(2, IntBuffer.wrap(vbos));

                mVBO_For_ResuableTileVertices = vbos[0];
                mVBO_For_ResuableTileTexCoords = vbos[1];

                gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
                                mVBO_For_ResuableTileVertices);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, kTotalBufferSizeBytesVerts,
                                null, GL.GL_STREAM_DRAW); // stream draw because this is a single quad use pipeline

                gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
                                mVBO_For_ResuableTileTexCoords);
                gl.glBufferData(GL.GL_ARRAY_BUFFER, kTotalBufferSizeBytesTex,
                                null, GL.GL_STREAM_DRAW); // stream draw because this is a single quad use pipeline
            }
        }

        public void glTexCoord2f(float v, float v1) {
            mTexCoords.put(v);
            mTexCoords.put(v1);
        }

        public void glVertex3f(float inX, float inY, float inZ) {
            mVertCoords.put(inX);
            mVertCoords.put(inY);
            mVertCoords.put(inZ);

            mOutstandingGlyphsVerticesPipeline++;

            if (mOutstandingGlyphsVerticesPipeline >= kTotalBufferSizeVerts) {
                this.draw();
            }
        }

        private void draw() {
            if (mOutstandingGlyphsVerticesPipeline > 0) {
                GL gl = GLU.getCurrentGL();

                TextureRenderer renderer = getBackingStore();
                Texture texture = renderer.getTexture(); // triggers texture uploads.  Maybe this should be more obvious?

                mVertCoords.rewind();
                mTexCoords.rewind();

                gl.glEnableClientState(GL.GL_VERTEX_ARRAY);

                if (usingVBOs) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
                                    mVBO_For_ResuableTileVertices);
                    gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                                       mOutstandingGlyphsVerticesPipeline * kSizeInBytes_OneVertices_VertexData,
                                       mVertCoords); // upload only the new stuff
                    gl.glVertexPointer(3, GL.GL_FLOAT, 0, 0);
                } else {
                    gl.glVertexPointer(3, GL.GL_FLOAT, 0, mVertCoords);
                }

                gl.glClientActiveTexture(GL.GL_TEXTURE0);
                gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);

                if (usingVBOs) {
                    gl.glBindBuffer(GL.GL_ARRAY_BUFFER,
                                    mVBO_For_ResuableTileTexCoords);
                    gl.glBufferSubData(GL.GL_ARRAY_BUFFER, 0,
                                       mOutstandingGlyphsVerticesPipeline * kSizeInBytes_OneVertices_TexData,
                                       mTexCoords); // upload only the new stuff
                    gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, 0);
                } else {
                    gl.glTexCoordPointer(2, GL.GL_FLOAT, 0, mTexCoords);
                }

                gl.glDrawArrays(GL.GL_QUADS, 0,
                                mOutstandingGlyphsVerticesPipeline);

                mVertCoords.rewind();
                mTexCoords.rewind();
                mOutstandingGlyphsVerticesPipeline = 0;
            }
        }

        private void drawIMMEDIATE() {
            if (mOutstandingGlyphsVerticesPipeline > 0) {
                TextureRenderer renderer = getBackingStore();
                Texture texture = renderer.getTexture(); // triggers texture uploads.  Maybe this should be more obvious?

                GL gl = GLU.getCurrentGL();
                gl.glBegin(GL.GL_QUADS);

                try {
                    int numberOfQuads = mOutstandingGlyphsVerticesPipeline / 4;
                    mVertCoords.rewind();
                    mTexCoords.rewind();

                    for (int i = 0; i < numberOfQuads; i++) {
                        gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
                        gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
                                      mVertCoords.get());

                        gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
                        gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
                                      mVertCoords.get());

                        gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
                        gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
                                      mVertCoords.get());

                        gl.glTexCoord2f(mTexCoords.get(), mTexCoords.get());
                        gl.glVertex3f(mVertCoords.get(), mVertCoords.get(),
                                      mVertCoords.get());
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    gl.glEnd();
                    mVertCoords.rewind();
                    mTexCoords.rewind();
                    mOutstandingGlyphsVerticesPipeline = 0;
                }
            }
        }
    }

    class DebugListener implements GLEventListener {
        private GLU glu = new GLU();
        private Frame frame;

        DebugListener(Frame frame) {
            this.frame = frame;
        }

        public void display(GLAutoDrawable drawable) {
            GL gl = drawable.getGL();
            gl.glClear(GL.GL_DEPTH_BUFFER_BIT | GL.GL_COLOR_BUFFER_BIT);

            if (packer == null) {
                return;
            }

            TextureRenderer rend = getBackingStore();
            final int w = rend.getWidth();
            final int h = rend.getHeight();
            rend.beginOrthoRendering(w, h);
            rend.drawOrthoRect(0, 0);
            rend.endOrthoRendering();

            if ((frame.getWidth() != w) || (frame.getHeight() != h)) {
                EventQueue.invokeLater(new Runnable() {
                        public void run() {
                            frame.setSize(w, h);
                        }
                    });
            }
        }

        // Unused methods
        public void init(GLAutoDrawable drawable) {
        }

        public void reshape(GLAutoDrawable drawable, int x, int y, int width,
                            int height) {
        }

        public void displayChanged(GLAutoDrawable drawable,
                                   boolean modeChanged, boolean deviceChanged) {
        }
    }
}
