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

import com.jogamp.opengl.util.packrect.Rect;
import com.jogamp.opengl.util.texture.TextureCoords;

import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;


/**
 * Representation of one or multiple unicode characters to be drawn.
 *
 * <p>The reason for the dual behavior is so that we can take in a sequence of
 * unicode characters and partition them into runs of individual glyphs, but if
 * we encounter complex text and/or unicode sequences we don't understand, we
 * can render them using the string-by-string method.
 *
 * <p><b>Positioning</b>
 *
 * <p>In an effort to make positioning glyphs more intuitive for both Java2D's
 * and OpenGL's coordinate systems, <i>Glyph</i> now stores its measurements
 * differently.  This new way is patterned off of HTML's box model.
 *
 * <p>Of course, as expected each glyph maintains its width and height.  For
 * spacing however, rather than storing positions in Java2D space that must be
 * manipulated on a case-by-case basis, <i>Glyph</i> stores two separate
 * pre-computed boundaries representing space around the text.  Each of the
 * boundaries has separate top, bottom, left, and right components.  These
 * components should generally be considered positive, but negative values are
 * sometimes necessary in rare situations.
 *
 * <p>The first boundary is called <i>padding</i>.  Padding is the space between
 * the actual glyph itself and its border.  It is included in the width and
 * height of the glyph.  The second boundary that a glyph stores is called
 * <i>margin</i>, which is extra space around the glyph's border.  The margin is
 * generally used for separating the glyph from other glyphs when it's stored.
 *
 * <p>The diagram below shows the boundaries of a glyph and how they relate to
 * its width and height.  The inner rectangle is the glyph's boundary, and the
 * outer rectangle is the edge of the margin.
 *
 * <pre>
 * +--------------------------------------+
 * |             top margin               |
 * |                                      |
 * |        |------ WIDTH -------|        |
 * |     -  +--------------------+        |
 * |     |  |    top padding     |        |
 * |     |  | l    ________    r |        |
 * | l   |  | e   /        \   i |      r |
 * | e      | f  |          |  g |      i |
 * | f   H  | t  |          |  h |      g |
 * | t   E  |    |          |  t |      h |
 * |     I  | p  |     _____     |      t |
 * | m   G  | a  |          |  p |        |
 * | a   H  | d  |          |  a |      m |
 * | r   T  | d  |          |  d |      a |
 * | g      | i  |          |  d |      r |
 * | i   |  | n  |          |  i |      g |
 * | n   |  | g   \________/   n |      i |
 * |     |  |                  g |      n |
 * |     |  |   bottom padding   |        |
 * |     -  +--------------------+        |
 * |                                      |
 * |                                      |
 * |            bottom margin             |
 * +--------------------------------------+
 * </pre>
 *
 * <p>In addition, <i>Glyph</i> also keeps a few other measurements useful for
 * positioning.  <i>Ascent</i> is the distance between the baseline and the top
 * border, while <i>descent</i> is the distance between the baseline and the
 * bottom border.  <i>Kerning</i> is the distance between the vertical baseline
 * and the left border.  Note that in some cases some of these fields can match
 * up with padding components, but in general they should be considered
 * separate.
 *
 * <p>Below is a diagram showing ascent, descent, and kerning.
 *
 * <pre>
 * +--------------------+   -
 * |                    |   |
 * |      ________      |   |
 * |     /        \     |   |
 * |    |          |    |   |
 * |    |          |    |   |
 * |    |          |    |   |
 * |    |     _____     |   | ascent
 * |    |          |    |   |
 * |    |          |    |   |
 * |    |          |    |   |
 * |    |          |    |   |
 * |    |          |    |   |
 * |     \________/     |   -
 * |                    |   |
 * |                    |   | descent
 * +--------------------+   -
 *
 * |--| kerning
 * </pre>
 */
final class Glyph {

    // Unicode ID if character, else -1
    final int id;

    // Sequence if string, else null
    final String str;

    // Font's identifier of glyph
    final int code;

    // Distance to next glyph
    final float advance;

    // Actual character if a character
    final char character;

    // Width of text with inner padding
    float width;

    // Height of text with inner padding
    float height;

    // Length from baseline to top border
    float ascent;

    // Length from baseline to bottom border
    float descent;

    // Length from baseline to left padding
    float kerning;

    // Outer boundary excluded from size
    Boundary margin;

    // Inner boundary included in size
    Boundary padding;

    // Position of glyph in larger area
    Rect location;

    // Coordinates of glyph in larger area
    TextureCoords coordinates;

    // Java2D shape of glyph
    GlyphVector glyphVector;

    // Cached bounding box of glyph
    Rectangle2D bounds;

    /**
     * Constructs a glyph representing an individual Unicode character.
     *
     * @param id Unicode ID of character
     * @param gv Vector shape of character
     * @throws AssertionError if id is negative or glyph vector is <tt>null</tt>
     */
    Glyph(final int id, final GlyphVector gv) {

        assert (id >= 0);
        assert (gv != null);

        this.id = id;
        this.str = null;
        this.code = gv.getGlyphCode(0);
        this.advance = gv.getGlyphMetrics(0).getAdvance();
        this.glyphVector = gv;
        this.character = (char) id;
    }

    /**
     * Constructs a glyph representing a sequence of characters.
     *
     * @param str Sequence of characters
     * @param gv Vector shape of sequence
     * @throws AssertionError if string or glyph vector is <tt>null</tt>
     */
    Glyph(final String str, final GlyphVector gv) {

        assert (str != null);
        assert (gv != null);

        this.id = -1;
        this.str = str;
        this.code = -1;
        this.advance = 0;
        this.character = '\0';
        this.glyphVector = gv;
    }

    /**
     * Returns a string representation of the object.
     */
    @Override
    public String toString() {
        return (str != null) ? str : Character.toString(character);
    }

    //------------------------------------------------------------------
    // Nested classes
    //

    /**
     * Space around a rectangle.
     */
    static class Boundary {

        // Space above rectangle
        int top;

        // Space below rectangle
        int bottom;

        // Space beside rectangle to left
        int left;

        // Space beside rectangle to right
        int right;

        /**
         * Creates a boundary by computing distances between two rectangles.
         *
         * @param large Outer rectangle
         * @param small Inner rectangle
         * @throws NullPointerException if either rectangle is <tt>null</tt>
         */
        Boundary(final Rectangle2D large, final Rectangle2D small) {
            top = (int) (large.getMinY() - small.getMinY()) * -1;
            left = (int) (large.getMinX() - small.getMinX()) * -1;
            bottom = (int) (large.getMaxY() - small.getMaxY());
            right = (int) (large.getMaxX() - small.getMaxX());
        }
    }
}
