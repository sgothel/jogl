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

import com.jogamp.opengl.util.awt.TextRenderer.RenderDelegate;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphMetrics;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.lang.Character.UnicodeBlock;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Utility for creating glyphs.
 */
interface GlyphProducer {

    /**
     * Deletes all stored glyphs.
     */
    void clearGlyphs();

    /**
     * Makes a glyph for a single character.
     *
     * @param c Character
     * @return Reused instance of a glyph
     */
    Glyph createGlyph(char c);

    /**
     * Makes a glyph for each character in a string.
     *
     * @param str Text as a string
     * @return View of glyphs valid until next call
     * @throws NullPointerException if string is <tt>null</tt>
     */
    List<Glyph> createGlyphs(String str);

    /**
     * Determines the distance to the next character after a glyph.
     *
     * @param c Character to find advance of
     */
    float findAdvance(char c);

    /**
     * Determines the visual bounds of a string with padding added.
     *
     * @param str Text to find visual bounds of
     * @throws NullPointerException if string is <tt>null</tt>
     */
    Rectangle2D findBounds(String str);

    /**
     * Deletes a single stored glyph.
     *
     * @param glyph Previously created glyph
     */
    void removeGlyph(Glyph glyph);
}


/**
 * Utility for creating glyph producers.
 */
class GlyphProducerFactory {

    /**
     * Creates correct glyph producer for a subset of Unicode.
     *
     * @param font Style of text
     * @param rd Controller of rendering details
     * @param frc Details on how fonts are rendered
     * @param ub Range of characters to support
     * @return Correct glyph producer for unicode block
     * @throws UnsupportedOperationException if unicode block unsupported
     */
    static GlyphProducer createGlyphProducer(final Font font,
                                             final RenderDelegate rd,
                                             final FontRenderContext frc,
                                             final UnicodeBlock ub) {
        if (ub == null) {
            return new UnicodeGlyphProducer(font, rd, frc);
        } else if (ub == UnicodeBlock.BASIC_LATIN) {
            return new AsciiGlyphProducer(font, rd, frc);
        } else {
            throw new UnsupportedOperationException("Unicode block unsupported!");
        }
    }

    /**
     * Prevents instantiation.
     */
    private GlyphProducerFactory() {
        // pass
    }
}


/**
 * Skeletal implementation of {@link GlyphProducer}.
 */
abstract class AbstractGlyphProducer implements GlyphProducer {

    // Reusable array
    private final char[] character;

    // Font glyphs made from
    private final Font font;

    // Rendering controller
    private final RenderDelegate renderDelegate;

    // Font render details
    private final FontRenderContext fontRenderContext;

    // Cached glyph vectors
    private final Map<String,GlyphVector> glyphVectors;

    // Returned glyphs
    private final List<Glyph> output;

    // View of glyphs
    private final List<Glyph> outputView;

    /**
     * Constructs an abstract glyph producer.
     *
     * @param font Font glyphs will be made from
     * @param rd Object for controlling rendering
     * @param frc Details on how to render fonts
     * @throws AssertionError if font, render delegate, or font render context is <tt>null</tt>
     */
    AbstractGlyphProducer(final Font font, final RenderDelegate rd, final FontRenderContext frc) {

        assert (font != null);
        assert (rd != null);
        assert (frc != null);

        this.font = font;
        this.renderDelegate = rd;
        this.fontRenderContext = frc;
        this.character = new char[1];
        this.glyphVectors = new HashMap<String,GlyphVector>();
        this.output = new ArrayList<Glyph>();
        this.outputView = Collections.unmodifiableList(output);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final float findAdvance(final char c) {

        // Check producer's inventory first
        final Glyph glyph = createGlyph(c);
        if (glyph != null) {
            return glyph.advance;
        }

        // Otherwise create the glyph vector
        final GlyphVector gv = createGlyphVector(c);
        final GlyphMetrics gm = gv.getGlyphMetrics(0);
        return gm.getAdvance();
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    @Override
    public final Rectangle2D findBounds(final String str) {

        final List<Glyph> glyphs = createGlyphs(str);

        // Check if already computed bounds
        if (glyphs.size() == 1) {
            final Glyph glyph = glyphs.get(0);
            return glyph.bounds;
        }

        // Otherwise just recompute it
        return addPaddingTo(renderDelegate.getBounds(str, font, fontRenderContext));
    }

    //-----------------------------------------------------------------
    // Helpers
    //

    /**
     * Adds outer space around a rectangle.
     *
     * <p>Formally normalize().
     *
     * <p>Give ourselves a boundary around each entity on the backing store in
     * order to prevent bleeding of nearby Strings due to the fact that we use
     * linear filtering
     *
     * <p>NOTE that this boundary is quite heuristic and is related to how far
     * away in 3D we may view the text -- heuristically, 1.5% of the font's
     * height.
     *
     * @param src Original rectangle
     * @param font Font being used to create glyphs
     * @return Rectangle with margin added
     * @throws NullPointerException if rectangle is <tt>null</tt>
     * @throws NullPointerException if font is <tt>null</tt>
     */
    private static Rectangle2D addMarginTo(final Rectangle2D src, final Font font) {

        final int boundary = (int) Math.max(1, 0.015 * font.getSize());
        final int x = (int) Math.floor(src.getMinX() - boundary);
        final int y = (int) Math.floor(src.getMinY() - boundary);
        final int w = (int) Math.ceil(src.getWidth() + 2 * boundary);
        final int h = (int) Math.ceil(src.getHeight() + 2 * boundary);;

        return new Rectangle2D.Float(x, y, w, h);
    }

    /**
     * Adds inner space to a rectangle.
     *
     * <p>Formally preNormalize().
     *
     * <p>Need to round to integer coordinates.
     *
     * <p>Also give ourselves a little slop around the reported bounds of
     * glyphs because it looks like neither the visual nor the pixel bounds
     * works perfectly well.
     *
     * @param src Original rectangle
     * @return Rectangle with padding added
     * @throws NullPointerException if rectangle is <tt>null</tt>
     */
    private static Rectangle2D addPaddingTo(final Rectangle2D src) {

        final int minX = (int) Math.floor(src.getMinX()) - 1;
        final int minY = (int) Math.floor(src.getMinY()) - 1;
        final int maxX = (int) Math.ceil(src.getMaxX()) + 1;
        final int maxY = (int) Math.ceil(src.getMaxY()) + 1;

        return new Rectangle2D.Float(minX, minY, maxX - minX, maxY - minY);
    }

    /**
     * Adds a glyph to the reusable list for output.
     *
     * @param glyph Glyph to add to output
     * @throws AssertionError if glyph is <tt>null</tt>
     */
    protected final void addToOutput(final Glyph glyph) {
        assert (glyph != null);
        output.add(glyph);
    }

    /**
     * Clears the reusable list for output.
     */
    protected final void clearOutput() {
        output.clear();
    }

    /**
     * Makes a glyph vector for a character.
     *
     * @param c Character to create glyph vector from
     * @return Glyph vector for the character
     */
    protected final GlyphVector createGlyphVector(final char c) {
        character[0] = c;
        return font.createGlyphVector(fontRenderContext, character);
    }

    /**
     * Makes a glyph vector for a string.
     *
     * @param font Style of text
     * @param frc Details on how to render font
     * @param str Text as a string
     * @return Glyph vector for the string
     * @throws NullPointerException if string is <tt>null</tt>
     */
    protected final GlyphVector createGlyphVector(final String str) {

        GlyphVector gv = glyphVectors.get(str);

        // Check if already made
        if (gv != null) {
            return gv;
        }

        // Otherwise make and store it
        final char[] text = str.toCharArray();
        final int len = str.length();
        gv = font.layoutGlyphVector(fontRenderContext, text, 0, len, 0);
        glyphVectors.put(str, gv);
        return gv;
    }

    /**
     * Checks if any characters in a string require full layout.
     *
     * <p>The process of creating and laying out glyph vectors is relatively
     * complex and can slow down text rendering significantly.  This method is
     * intended to increase performance by not creating glyph vectors for
     * strings with characters that can be treated independently.
     *
     * <p>Currently the decision is very simple.  It just treats any characters
     * above the <i>IPA Extensions</i> block as complex.  This is convenient
     * because most Latin characters are treated as simple but <i>Spacing
     * Modifier Letters</i> and <i>Combining Diacritical Marks</i> are not.
     * Ideally it would also be nice to have a few other blocks included,
     * especially <i>Greek</i> and maybe symbols, but that is perhaps best left
     * for later work.
     *
     * <p>A truly correct implementation may require a lot of research or
     * developers with more experience in the area.  However, the following
     * Unicode blocks are known to require full layout in some form:
     *
     * <ul>
     *   <li>Spacing Modifier Letters (02B0-02FF)</li>
     *   <li>Combining Diacritical Marks (0300-036F)</li>
     *   <li>Hebrew (0590-05FF)</li>
     *   <li>Arabic (0600-06FF)</li>
     *   <li>Arabic Supplement (0750-077F)</li>
     *   <li>Combining Diacritical Marks Supplement (1DC0-1FFF)</li>
     *   <li>Combining Diacritical Marks for Symbols (20D0-20FF)</li>
     *   <li>Arabic Presentation Forms-A (FB50–FDFF)</li>
     *   <li>Combining Half Marks (FE20–FE2F)</li>
     *   <li>Arabic Presentation Forms-B (FE70–FEFF)</li>
     * </ul>
     *
     * <p>Asian scripts will also have letters that combine together, but it
     * appears that the input method may take care of that so it may not be
     * necessary to check for them here.
     *
     * <p>Finally, it should be noted that even Latin has characters that can
     * combine into glyphs called ligatures.  The classic example is an 'f' and
     * an 'i'.  Java however will not make the replacements itself so we do not
     * need to consider that here.
     *
     * @param str Text of unknown character types
     * @return <tt>true</tt> if a complex character is found
     * @throws NullPointerException if string is <tt>null</tt>
     */
    protected static boolean hasComplexCharacters(final String str) {
        final int len = str.length();
        for (int i = 0; i < len; ++i) {
            if (str.charAt(i) > 0x2AE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Measures a glyph.
     *
     * <p>Sets all the measurements in a glyph after it's created.
     *
     * @param glyph Visual representation of a character
     * @throws NullPointerException if glyph is <tt>null</tt>
     */
    protected final void measure(final Glyph glyph) {

        // Compute visual boundary
        final Rectangle2D visBox;
        if (glyph.str != null) {
            visBox = renderDelegate.getBounds(glyph.str, font, fontRenderContext);
        } else {
            visBox = renderDelegate.getBounds(glyph.glyphVector, fontRenderContext);
        }

        // Compute rectangles
        final Rectangle2D padBox = addPaddingTo(visBox);
        final Rectangle2D marBox = addMarginTo(padBox, font);

        // Set parameters
        glyph.padding = new Glyph.Boundary(padBox, visBox);
        glyph.margin = new Glyph.Boundary(marBox, padBox);
        glyph.width = (float) padBox.getWidth();
        glyph.height = (float) padBox.getHeight();
        glyph.ascent = (float) padBox.getMinY() * -1;
        glyph.descent = (float) padBox.getMaxY();
        glyph.kerning = (float) padBox.getMinX();
        glyph.bounds = padBox;
    }

    /**
     * Checks if a glyph vector is complex.
     *
     * @param gv Glyph vector to check
     * @return <tt>true</tt> if glyph vector is complex
     * @throws NullPointerException if glyph vector is <tt>null</tt>
     */
    protected static boolean isComplex(final GlyphVector gv) {
        return gv.getLayoutFlags() != 0;
    }

    //-----------------------------------------------------------------
    // Getters
    //

    /**
     * Returns font used to create glyphs.
     */
    protected final Font getFont() {
        return font;
    }

    /**
     * Returns read-only view of reusable list for output.
     */
    protected final List<Glyph> getOutput() {
        return outputView;
    }
}


/**
 * Producer that creates glyphs in the ASCII range.
 */
final class AsciiGlyphProducer extends AbstractGlyphProducer {

    // Storage of glyphs
    private final Glyph[] inventory;

    /**
     * Creates an ASCII glyph producer.
     *
     * @param font Font glyphs will be made of
     * @param rd Object for controlling rendering
     * @param frc Details on how to render fonts
     * @throws AssertionError if font, render delegate, or font render context is <tt>null</tt>
     */
    AsciiGlyphProducer(final Font font, final RenderDelegate rd, final FontRenderContext frc) {

        super(font, rd, frc);

        inventory = new Glyph[128];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clearGlyphs() {
        // pass
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Glyph createGlyph(char c) {

        // Check if out of range
        if (c > 128) {
            c = '_';
        }

        // Check if already created
        Glyph glyph = inventory[c];
        if (glyph != null) {
            return glyph;
        }

        // Create glyph
        GlyphVector gv = createGlyphVector(c);
        glyph = new Glyph(c, gv);
        measure(glyph);

        // Store and finish
        inventory[c] = glyph;
        return glyph;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public List<Glyph> createGlyphs(final String str) {

        // Clear the output
        clearOutput();

        // Add each glyph to the output
        final int len = str.length();
        for (int i = 0; i < len; ++i) {
            final char character = str.charAt(i);
            final Glyph glyph = createGlyph(character);
            addToOutput(glyph);
        }

        // Return the output
        return getOutput();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void removeGlyph(final Glyph glyph) {
        // pass
    }
}


/**
 * Producer for creating glyphs of all characters in basic unicode block.
 */
final class UnicodeGlyphProducer extends AbstractGlyphProducer {

    // Storage of glyphs
    private final GlyphMap glyphMap;

    /**
     * Constructs a unicode glyph producer.
     *
     * @param font Font glyphs will be made of
     * @param rd Object for controlling rendering
     * @param frc Details on how to render fonts
     * @throws AssertionError if font, render delegate, or font render context is <tt>null</tt>
     */
    UnicodeGlyphProducer(final Font font, final RenderDelegate rd, final FontRenderContext frc) {

        super(font, rd, frc);

        glyphMap = new GlyphMap();
    }

    /**
     * {@inheritDoc}
     */
    public void clearGlyphs() {
        glyphMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Glyph createGlyph(final char c) {
        Glyph glyph = glyphMap.get(c);
        if (glyph == null) {
            glyph = doCreateGlyph(c);
        }
        return glyph;
    }

    /**
     * Actually makes a glyph for a single character.
     *
     * @param c Character
     * @param frc Details on how to render fonts
     * @return Reused instance of a glyph
     */
    private Glyph doCreateGlyph(final char c) {

        // Create a glyph from the glyph vector
        final GlyphVector gv = createGlyphVector(c);
        final Glyph glyph = new Glyph(c, gv);

        // Measure and store it
        measure(glyph);
        glyphMap.put(c, glyph);

        return glyph;
    }

    /**
     * {@inheritDoc}
     *
     * @throws NullPointerException {@inheritDoc}
     */
    public List<Glyph> createGlyphs(final String str) {
        if (!hasComplexCharacters(str)) {
            return doCreateSimpleGlyphs(str);
        } else {
            final GlyphVector gv = createGlyphVector(str);
            return isComplex(gv) ?  doCreateComplexGlyph(str, gv) : doCreateSimpleGlyphs(str);
        }
    }

    /**
     * Creates a single glyph from text with a complex layout.
     *
     * @param str Text with a complex layout
     * @param gv Glyph vector of entire text
     * @return Read-only pointer to list of glyphs valid until next call
     * @throws NullPointerException if string is <tt>null</tt>
     * @throws NullPointerException if glyph vector is <tt>null</tt>
     */
    private List<Glyph> doCreateComplexGlyph(final String str, final GlyphVector gv) {

        // Clear the output
        clearOutput();

        // Create the glyph and add it to output
        Glyph glyph = glyphMap.get(str);
        if (glyph == null) {
            glyph = new Glyph(str, gv);
            measure(glyph);
            glyphMap.put(str, glyph);
        }
        addToOutput(glyph);

        // Return the output
        return getOutput();
    }

    /**
     * Creates multiple glyphs from text with a simple layout.
     *
     * @param str Text with a simple layout
     * @return Read-only pointer to list of glyphs valid until next call
     * @throws NullPointerException if string is <tt>null</tt>
     */
    private List<Glyph> doCreateSimpleGlyphs(final String str) {

        // Clear the output
        clearOutput();

        // Create the glyphs and add them to the output
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            final char c = str.charAt(i);
            final Glyph glyph = createGlyph(c);
            addToOutput(glyph);
        }

        // Return the output
        return getOutput();
    }

    /**
     * {@inheritDoc}
     */
    public void removeGlyph(final Glyph glyph) {
        glyphMap.remove(glyph);
    }
}


/**
 * Utility for mapping text to glyphs.
 */
final class GlyphMap {

    // Fast map for ASCII chars
    private final Glyph[] ascii;

    // Map from char to code
    private final Map<Character,Integer> codes;

    // Map from code to glyph
    private final Map<Integer,Glyph> unicode;

    // Glyphs with layout flags
    private final Map<String,Glyph> complex;

    /**
     * Constructs a glyph map.
     */
    GlyphMap() {
        ascii = new Glyph[128];
        codes = new HashMap<Character,Integer>();
        unicode = new HashMap<Integer,Glyph>();
        complex = new HashMap<String,Glyph>();
    }

    /**
     * Deletes all glyphs stored in the map.
     */
    void clear() {
        Arrays.fill(ascii, null);
        unicode.clear();
        complex.clear();
    }

    /**
     * Returns a glyph for a character.
     */
    Glyph get(final char c) {
        return (c < 128) ? ascii[c] : unicode.get(codes.get(c));
    }

    /**
     * Returns a glyph for a string.
     */
    Glyph get(final String str) {
        return complex.get(str);
    }

    /**
     * Stores a simple glyph in the map.
     */
    void put(final char c, final Glyph glyph) {
        if (c < 128) {
            ascii[c] = glyph;
        } else {
            codes.put(c, glyph.code);
            unicode.put(glyph.code, glyph);
        }
    }

    /**
     * Stores a complex glyph in the map.
     */
    void put(final String str, final Glyph glyph) {
        complex.put(str, glyph);
    }

    /**
     * Deletes a single glyph from the map.
     */
    void remove(final char c) {
        if (c < 128) {
            ascii[c] = null;
        } else {
            final Character character = c;
            final Integer code = codes.get(character);
            unicode.remove(code);
            codes.remove(character);
        }
    }

    /**
     * Deletes a single glyph from the map.
     */
    void remove(final String str) {
        complex.remove(str);
    }

    /**
     * Deletes a single glyph from the map.
     */
    void remove(final Glyph glyph) {
        if (glyph.str != null) {
            remove(glyph.str);
        } else {
            remove(glyph.character);
        }
    }
}

