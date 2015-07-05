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
     * @throws NullPointerException if string is null
     */
    /*@Nonnull*/
    List<Glyph> createGlyphs(/*@Nonnull*/ String str);

    /**
     * Determines the distance to the next character after a glyph.
     *
     * @param c Character to find advance of
     * @return Distance to the next character after a glyph, which may be negative
     */
    /*@CheckForSigned*/
    float findAdvance(char c);

    /**
     * Determines the visual bounds of a string with padding added.
     *
     * @param str Text to find visual bounds of
     * @return Visual bounds of string with padding added, not null
     * @throws NullPointerException if string is null
     */
    /*@Nonnull*/
    Rectangle2D findBounds(/*@Nonnull*/ String str);

    /**
     * Deletes a single stored glyph.
     *
     * @param glyph Previously created glyph, ignored if null
     */
    void removeGlyph(/*@CheckForNull*/ Glyph glyph);
}


// TODO: Rename to `GlyphProducers`?
/**
 * Utility for creating glyph producers.
 */
/*@ThreadSafe*/
final class GlyphProducerFactory {

    /**
     * Prevents instantiation.
     */
    private GlyphProducerFactory() {
        // empty
    }

    /**
     * Creates correct glyph producer for a subset of Unicode.
     *
     * @param font Style of text
     * @param rd Controller of rendering details
     * @param frc Details on how fonts are rendered
     * @param ub Range of characters to support
     * @return Correct glyph producer for unicode block, not null
     * @throws UnsupportedOperationException if unicode block unsupported
     */
    /*@Nonnull*/
    static GlyphProducer createGlyphProducer(/*@Nonnull*/ final Font font,
                                             /*@Nonnull*/ final RenderDelegate rd,
                                             /*@Nonnull*/ final FontRenderContext frc,
                                             /*@CheckForNull*/ final UnicodeBlock ub) {
        if (ub == null) {
            return new UnicodeGlyphProducer(font, rd, frc);
        } else if (ub == UnicodeBlock.BASIC_LATIN) {
            return new AsciiGlyphProducer(font, rd, frc);
        } else {
            throw new UnsupportedOperationException("Unicode block unsupported!");
        }
    }
}


/**
 * Skeletal implementation of {@link GlyphProducer}.
 */
abstract class AbstractGlyphProducer implements GlyphProducer {

    /**
     * Reusable array for creating glyph vectors for a single character.
     */
    /*@Nonnull*/
    private final char[] characters = new char[1];

    /**
     * Font glyphs made from.
     */
    /*@Nonnull*/
    private final Font font;

    /**
     * Rendering controller.
     */
    /*@Nonnull*/
    private final RenderDelegate renderDelegate;

    /**
     * Font render details.
     */
    /*@Nonnull*/
    private final FontRenderContext fontRenderContext;

    /**
     * Cached glyph vectors.
     */
    /*@Nonnull*/
    private final Map<String, GlyphVector> glyphVectors = new HashMap<String, GlyphVector>();

    /**
     * Returned glyphs.
     */
    /*@Nonnull*/
    private final List<Glyph> output = new ArrayList<Glyph>();

    /**
     * View of glyphs.
     */
    /*@Nonnull*/
    private final List<Glyph> outputView = Collections.unmodifiableList(output);

    /**
     * Constructs an abstract glyph producer.
     *
     * @param font Font glyphs will be made from
     * @param rd Object for controlling rendering
     * @param frc Details on how to render fonts
     * @throws NullPointerException if font, render delegate, or font render context is null
     */
    AbstractGlyphProducer(/*@Nonnull*/ final Font font,
                          /*@Nonnull*/ final RenderDelegate rd,
                          /*@Nonnull*/ final FontRenderContext frc) {

        checkNotNull(font, "Font cannot be null");
        checkNotNull(rd, "Render delegate cannot be null");
        checkNotNull(frc, "Font render context cannot be null");

        this.font = font;
        this.renderDelegate = rd;
        this.fontRenderContext = frc;
    }

    /**
     * Adds outer space around a rectangle.
     *
     * <p>
     * This method was formally called "normalize."
     *
     * <p>
     * Give ourselves a boundary around each entity on the backing store in order to prevent
     * bleeding of nearby Strings due to the fact that we use linear filtering
     *
     * <p>
     * Note that this boundary is quite heuristic and is related to how far away in 3D we may view
     * the text -- heuristically, 1.5% of the font's height.
     *
     * @param src Original rectangle
     * @param font Font being used to create glyphs
     * @return Rectangle with margin added
     * @throws NullPointerException if rectangle is null
     * @throws NullPointerException if font is null
     */
    /*@Nonnull*/
    private static Rectangle2D addMarginTo(/*@Nonnull*/ final Rectangle2D src,
                                           /*@Nonnull*/ final Font font) {

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
     * <p>
     * This method was formally called "preNormalize."
     *
     * <p>
     * Need to round to integer coordinates.
     *
     * <p>
     * Also give ourselves a little slop around the reported bounds of glyphs because it looks like
     * neither the visual nor the pixel bounds works perfectly well.
     *
     * @param src Original rectangle
     * @return Rectangle with padding added
     * @throws NullPointerException if rectangle is null
     */
    /*@Nonnull*/
    private static Rectangle2D addPaddingTo(/*@Nonnull*/ final Rectangle2D src) {

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
     * @throws NullPointerException if glyph is null
     */
    protected final void addToOutput(/*@Nonnull*/ final Glyph glyph) {
        checkNotNull(glyph, "Glyph cannot be null");
        output.add(glyph);
    }

    /*@Nonnull*/
    private static <T> T checkNotNull(/*@Nullable*/ final T obj,
                                      /*@CheckForNull*/ final String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
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
     * @return Glyph vector for the character, not null
     */
    /*@Nonnull*/
    protected final GlyphVector createGlyphVector(final char c) {
        characters[0] = c;
        return font.createGlyphVector(fontRenderContext, characters);
    }

    /**
     * Makes a glyph vector for a string.
     *
     * @param font Style of text
     * @param frc Details on how to render font
     * @param str Text as a string
     * @return Glyph vector for the string, not null
     * @throws NullPointerException if string is null
     */
    /*@Nonnull*/
    protected final GlyphVector createGlyphVector(/*@Nonnull*/ final String str) {

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

    /*@CheckForSigned*/
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

    /*@Nonnull*/
    @Override
    public final Rectangle2D findBounds(/*@Nonnull*/ final String str) {

        final List<Glyph> glyphs = createGlyphs(str);

        // Check if already computed bounds
        if (glyphs.size() == 1) {
            final Glyph glyph = glyphs.get(0);
            return glyph.bounds;
        }

        // Otherwise just recompute it
        return addPaddingTo(renderDelegate.getBounds(str, font, fontRenderContext));
    }

    /**
     * Returns the font used to create glyphs.
     *
     * @return Font used to create glyphs, not null
     */
    /*@Nonnull*/
    protected final Font getFont() {
        return font;
    }

    /**
     * Returns a read-only view of this producer's reusable list for output.
     *
     * @return Read-only view of reusable list, not null
     */
    /*@Nonnull*/
    protected final List<Glyph> getOutput() {
        return outputView;
    }

    /**
     * Checks if any characters in a string require full layout.
     *
     * <p>
     * The process of creating and laying out glyph vectors is relatively complex and can slow down
     * text rendering significantly.  This method is intended to increase performance by not
     * creating glyph vectors for strings with characters that can be treated independently.
     *
     * <p>
     * Currently the decision is very simple.  It just treats any characters above the <i>IPA
     * Extensions</i> block as complex.  This is convenient because most Latin characters are
     * treated as simple but <i>Spacing Modifier Letters</i> and <i>Combining Diacritical Marks</i>
     * are not.  Ideally it would also be nice to have a few other blocks included, especially
     * <i>Greek</i> and maybe symbols, but that is perhaps best left for later work.
     *
     * <p>
     * A truly correct implementation may require a lot of research or developers with more
     * experience in the area.  However, the following Unicode blocks are known to require full
     * layout in some form:
     *
     * <ul>
     * <li>Spacing Modifier Letters (02B0-02FF)
     * <li>Combining Diacritical Marks (0300-036F)
     * <li>Hebrew (0590-05FF)
     * <li>Arabic (0600-06FF)
     * <li>Arabic Supplement (0750-077F)
     * <li>Combining Diacritical Marks Supplement (1DC0-1FFF)
     * <li>Combining Diacritical Marks for Symbols (20D0-20FF)
     * <li>Arabic Presentation Forms-A (FB50–FDFF)
     * <li>Combining Half Marks (FE20–FE2F)
     * <li>Arabic Presentation Forms-B (FE70–FEFF)
     * </ul>
     *
     * <p>
     * Asian scripts will also have letters that combine together, but it appears that the input
     * method may take care of that so it may not be necessary to check for them here.
     *
     * <p>
     * Finally, it should be noted that even Latin has characters that can combine into glyphs
     * called ligatures.  The classic example is an 'f' and an 'i'.  Java however will not make the
     * replacements itself so we do not need to consider that here.
     *
     * @param str Text of unknown character types
     * @return True if a complex character is found
     * @throws NullPointerException if string is null
     */
    protected static boolean hasComplexCharacters(/*@Nonnull*/ final String str) {
        final int len = str.length();
        for (int i = 0; i < len; ++i) {
            if (str.charAt(i) > 0x2AE) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a glyph vector is complex.
     *
     * @param gv Glyph vector to check, which may be null
     * @return True if glyph vector is complex
     */
    protected static boolean isComplex(/*@CheckForNull*/ final GlyphVector gv) {

        if (gv == null) {
            return false;
        }

        return gv.getLayoutFlags() != 0;
    }

    /**
     * Measures a glyph.
     *
     * <p>
     * Sets all the measurements in a glyph after it's created.
     *
     * @param glyph Visual representation of a character
     * @throws NullPointerException if glyph is null
     */
    protected final void measure(/*@Nonnull*/ final Glyph glyph) {

        // Compute visual boundary
        final Rectangle2D visualBox;
        if (glyph.str != null) {
            visualBox = renderDelegate.getBounds(glyph.str, font, fontRenderContext);
        } else {
            visualBox = renderDelegate.getBounds(glyph.glyphVector, fontRenderContext);
        }

        // Compute rectangles
        final Rectangle2D paddingBox = addPaddingTo(visualBox);
        final Rectangle2D marginBox = addMarginTo(paddingBox, font);

        // Set fields
        glyph.padding = new Glyph.Boundary(paddingBox, visualBox);
        glyph.margin = new Glyph.Boundary(marginBox, paddingBox);
        glyph.width = (float) paddingBox.getWidth();
        glyph.height = (float) paddingBox.getHeight();
        glyph.ascent = (float) paddingBox.getMinY() * -1;
        glyph.descent = (float) paddingBox.getMaxY();
        glyph.kerning = (float) paddingBox.getMinX();
        glyph.bounds = paddingBox;
    }
}


/**
 * {@link GlyphProducer} that creates glyphs in the ASCII range.
 */
/*@NotThreadSafe*/
final class AsciiGlyphProducer extends AbstractGlyphProducer {

    /**
     * Storage for glyphs.
     */
    /*@Nonnull*/
    private final Glyph[] inventory = new Glyph[128];

    /**
     * Constructs an {@link AsciiGlyphProducer}.
     *
     * @param font Font glyphs will be made from
     * @param rd Delegate for controlling rendering
     * @param frc Details on how to render fonts
     * @throws NullPointerException if font, render delegate, or font render context is null
     */
    AsciiGlyphProducer(/*@Nonnull*/ final Font font,
                       /*@Nonnull*/ final RenderDelegate rd,
                       /*@Nonnull*/ final FontRenderContext frc) {
        super(font, rd, frc);
    }

    @Override
    public void clearGlyphs() {
        // empty
    }

    /*@Nonnull*/
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

    /*@Nonnull*/
    public List<Glyph> createGlyphs(/*@Nonnull*/ final String str) {

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

    @Override
    public void removeGlyph(/*@CheckForNull*/ final Glyph glyph) {
        // empty
    }
}


/**
 * {@link GlyphProducer} for creating glyphs of all characters in the basic unicode block.
 */
/*@NotThreadSafe*/
final class UnicodeGlyphProducer extends AbstractGlyphProducer {

    /**
     * Storage for glyphs.
     */
    /*@Nonnull*/
    private final GlyphMap glyphMap = new GlyphMap();

    /**
     * Constructs a {@link UnicodeGlyphProducer}.
     *
     * @param font Font glyphs will be made of
     * @param rd Object for controlling rendering
     * @param frc Details on how to render fonts
     * @throws NullPointerException if font, render delegate, or font render context is null
     */
    UnicodeGlyphProducer(/*@Nonnull*/ final Font font,
                         /*@Nonnull*/ final RenderDelegate rd,
                         /*@Nonnull*/ final FontRenderContext frc) {
        super(font, rd, frc);
    }

    @Override
    public void clearGlyphs() {
        glyphMap.clear();
    }

    /**
     * Creates a single glyph from text with a complex layout.
     *
     * @param str Text with a complex layout
     * @param gv Glyph vector of entire text
     * @return Read-only pointer to list of glyphs valid until next call
     * @throws NullPointerException if string is null
     * @throws NullPointerException if glyph vector is null
     */
    /*@Nonnull*/
    private List<Glyph> createComplexGlyph(/*@Nonnull*/ final String str,
                                           /*@Nonnull*/ final GlyphVector gv) {

        clearOutput();

        // Create the glyph and add it to output
        Glyph glyph = glyphMap.get(str);
        if (glyph == null) {
            glyph = new Glyph(str, gv);
            measure(glyph);
            glyphMap.put(str, glyph);
        }
        addToOutput(glyph);

        return getOutput();
    }

    /*@Nonnull*/
    @Override
    public Glyph createGlyph(final char c) {
        Glyph glyph = glyphMap.get(c);
        if (glyph == null) {
            glyph = createGlyphImpl(c);
        }
        return glyph;
    }

    /*@Nonnull*/
    private Glyph createGlyphImpl(final char c) {

        // Create a glyph from the glyph vector
        final GlyphVector gv = createGlyphVector(c);
        final Glyph glyph = new Glyph(c, gv);

        // Measure and store it
        measure(glyph);
        glyphMap.put(c, glyph);

        return glyph;
    }

    /*@Nonnull*/
    @Override
    public List<Glyph> createGlyphs(/*@Nonnull*/ final String str) {
        if (!hasComplexCharacters(str)) {
            return createSimpleGlyphs(str);
        } else {
            final GlyphVector gv = createGlyphVector(str);
            return isComplex(gv) ?  createComplexGlyph(str, gv) : createSimpleGlyphs(str);
        }
    }

    /**
     * Creates multiple glyphs from text with a simple layout.
     *
     * @param str Text with a simple layout
     * @return Read-only pointer to list of glyphs valid until next call
     * @throws NullPointerException if string is null
     */
    /*@Nonnull*/
    private List<Glyph> createSimpleGlyphs(/*@Nonnull*/ final String str) {

        clearOutput();

        // Create the glyphs and add them to the output
        final int len = str.length();
        for (int i = 0; i < len; ++i) {
            final char c = str.charAt(i);
            final Glyph glyph = createGlyph(c);
            addToOutput(glyph);
        }

        return getOutput();
    }

    @Override
    public void removeGlyph(/*@CheckForNull*/ final Glyph glyph) {
        glyphMap.remove(glyph);
    }
}


/**
 * Utility for mapping text to glyphs.
 */
/*@NotThreadSafe*/
final class GlyphMap {

    /**
     * Fast map for ASCII chars.
     */
    /*@Nonnull*/
    private final Glyph[] ascii = new Glyph[128];

    /**
     * Map from char to code.
     */
    /*@Nonnull*/
    private final Map<Character, Integer> codes = new HashMap<Character, Integer>();

    /**
     * Map from code to glyph.
     */
    /*@Nonnull*/
    private final Map<Integer, Glyph> unicode = new HashMap<Integer, Glyph>();

    /**
     * Glyphs with layout flags.
     */
    /*@Nonnull*/
    private final Map<String, Glyph> complex = new HashMap<String, Glyph>();

    /**
     * Constructs a glyph map.
     */
    GlyphMap() {
        // empty
    }

    /*@Nonnull*/
    private static <T> T checkNotNull(/*@Nullable*/ final T obj,
                                      /*@CheckForNull*/ final String message) {
        if (obj == null) {
            throw new NullPointerException(message);
        }
        return obj;
    }


    /**
     * Deletes all glyphs stored in the map.
     */
    void clear() {
        Arrays.fill(ascii, null);
        codes.clear();
        unicode.clear();
        complex.clear();
    }

    /**
     * Returns a glyph for a character.
     *
     * @param c Character to get glyph for
     * @return Glyph for the character, or null if it wasn't found
     */
    /*@CheckForNull*/
    Glyph get(final char c) {
        return (c < 128) ? ascii[c] : unicode.get(codes.get(c));
    }

    /**
     * Returns a glyph for a string.
     *
     * @param str String to get glyph for, which may be null
     * @return Glyph for the string, or null if it wasn't found
     */
    /*@CheckForNull*/
    Glyph get(/*@CheckForNull*/ final String str) {
        return complex.get(str);
    }

    /**
     * Stores a simple glyph in the map.
     *
     * @param c Character glyph represents
     * @param glyph Glyph to store
     * @throws NullPointerException if glyph is null
     */
    void put(final char c, /*@Nonnull*/ final Glyph glyph) {

        checkNotNull(glyph, "Glyph cannot be null");

        if (c < 128) {
            ascii[c] = glyph;
        } else {
            codes.put(c, glyph.code);
            unicode.put(glyph.code, glyph);
        }
    }

    /**
     * Stores a complex glyph in the map.
     *
     * @param str String glyph represents
     * @param glyph Glyph to store
     * @throws NullPointerException if string or glyph is null
     */
    void put(/*@Nonnull*/ final String str, /*@Nonnull*/ final Glyph glyph) {

        checkNotNull(str, "String cannot be null");
        checkNotNull(glyph, "Glyph cannot be null");

        complex.put(str, glyph);
    }

    /**
     * Deletes a simple glyph from this {@link GlyphMap}.
     *
     * @param c Character of glyph to remove
     */
    private void remove(final char c) {
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
     * Deletes a single glyph from this {@link GlyphMap}.
     *
     * @param glyph Glyph to remove, ignored if null
     */
    void remove(/*@CheckForNull*/ final Glyph glyph) {

        if (glyph == null) {
            return;
        }

        if (glyph.str != null) {
            remove(glyph.str);
        } else {
            remove(glyph.character);
        }
    }

    /**
     * Deletes a complex glyph from this {@link GlyphMap}.
     *
     * @param str Text of glyph to remove, ignored if null
     */
    private void remove(/*@CheckForNull*/ final String str) {
        complex.remove(str);
    }
}

