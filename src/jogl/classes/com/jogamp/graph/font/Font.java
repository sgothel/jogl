/**
 * Copyright 2010-2023 JogAmp Community. All rights reserved.
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
package com.jogamp.graph.font;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.geom.plane.AffineTransform;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * Interface wrapper for font implementation.
 * <p>
 * TrueType Font Specification:
 * <ul>
 *   <li>http://www.freetype.org/freetype2/documentation.html</li>
 *   <li>https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6.html</li>
 *   <li>http://www.microsoft.com/typography/SpecificationsOverview.mspx</li>
 *   <li>http://www.microsoft.com/typography/otspec/</li>
 * </ul>
 * </p>
 * <p>
 * TrueType Font Table Introduction:
 * <ul>
 *   <li>http://scripts.sil.org/cms/scripts/page.php?item_id=IWS-Chapter08</li>
 * </ul>
 * </p>
 * <p>
 * Misc.:
 * <ul>
 *   <li>Treatis on Font <code>Rasterization https://freddie.witherden.org/pages/font-rasterisation/</code></li>
 *   <li>Glyph Hell <code>http://walon.org/pub/ttf/ttf_glyphs.htm</code></li>
 * </ul>
 * </p>
 */

public interface Font {

    /** font name indices for name table */
    public static final int NAME_COPYRIGHT = 0;
    public static final int NAME_FAMILY = 1;
    public static final int NAME_SUBFAMILY = 2;
    public static final int NAME_UNIQUNAME = 3;
    public static final int NAME_FULLNAME = 4;
    public static final int NAME_VERSION = 5;
    public static final int NAME_MANUFACTURER = 8;
    public static final int NAME_DESIGNER = 9;


    /**
     * Metrics for font
     *
     * Depending on the font's direction, horizontal or vertical,
     * the following tables shall be used:
     *
     * Vertical https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6vhea.html
     * Horizontal https://developer.apple.com/fonts/TrueType-Reference-Manual/RM06/Chap6hhea.html
     */
    public interface Metrics {
        /**
         * @return ascent in font-units, sourced from `hheaTable' table.
         */
        int getAscentFU();

        /**
         * @return ascent in font em-size [0..1], sourced from `hheaTable' table.
         */
        float getAscent();

        /**
         * @return descent in font-units, sourced from `hheaTable' table.
         */
        int getDescentFU();

        /**
         * @return descend in font em-size [0..1], sourced from `hheaTable' table.
         */
        float getDescent();

        /**
         * @return line-gap in font-units, sourced from `hheaTable' table.
         */
        int getLineGapFU();

        /**
         * @return line-gap in font em-size [0..1], sourced from `hheaTable' table.
         */
        float getLineGap();

        /**
         * @return max-extend in font-units, sourced from `hheaTable' table.
         */
        int getMaxExtendFU();

        /**
         * @return max-extend in font em-size [0..1], sourced from `hheaTable' table.
         */
        float getMaxExtend();

        /** Returns the font's units per EM from the 'head' table. One em square covers one glyph. */
        int getUnitsPerEM();

        /**
         * Return fractional font em-size [0..1], i.e. funits divided by {@link #getUnitsPerEM()}, i.e.
         * <pre>
         *    return funits / head.unitsPerEM;
         * </pre>
         * @param funits smallest font unit, where {@link #getUnitsPerEM()} square covers whole glyph
         * @return fractional font em-size [0..1]
         */
        float getScale(final int funits);

        /**
         * @param dest AABBox instance set to this metrics boundary in font-units
         * @return the given and set AABBox 'dest' in font units
         */
        AABBox getBBoxFU(final AABBox dest);

        /**
         * @param dest AABBox instance set to this metrics boundary in font em-size [0..1]
         * @return the given and set AABBox 'dest' in font units
         */
        AABBox getBBox(final AABBox dest, final float[] tmpV3);
    }

    /**
     * Glyph for font
     *
     * http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6cmap.html
     * http://developer.apple.com/textfonts/TTRefMan/RM06/Chap6glyf.html
     * http://www.microsoft.com/typography/otspec/glyf.htm
     */
    public interface Glyph {
        // reserved special glyph IDs
        // http://scripts.sil.org/cms/scripts/page.php?item_id=IWS-Chapter08#ba57949e
        public static final int ID_UNKNOWN = 0;
        public static final int ID_CR = 2;
        public static final int ID_SPACE = 3;

        Font getFont();

        /** Return this glyph's ID */
        int getID();

        /**
         * Return fractional font em-size [0..1], i.e. funits divided by {@link #getUnitsPerEM()}, i.e.
         * <pre>
         *    return funits / head.unitsPerEM;
         * </pre>
         * @param funits smallest font unit, where {@link #getUnitsPerEM()} square covers whole glyph
         * @return fractional font em-size [0..1]
         */
        float getScale(final int funits);

        /**
         * Return the AABBox in font-units, borrowing internal instance.
         */
        AABBox getBBoxFU();

        /**
         * Return the AABBox in font-units, copying into given dest.
         * @param dest AABBox instance set to this metrics boundary in font-units
         * @return the given and set AABBox 'dest' in font-units
         */
        AABBox getBBoxFU(final AABBox dest);

        /**
         * Return the AABBox in font em-size [0..1], copying into given dest.
         * @param dest AABBox instance set to this metrics boundary in font em-size [0..1]
         * @param tmpV3 caller provided temporary 3-component vector
         * @return the given and set AABBox 'dest' in font em-size [0..1]
         */
        AABBox getBBox(final AABBox dest, float[] tmpV3);

        /**
         * Return the AABBox in font em-size [0..1], creating a new copy.
         */
        AABBox getBBox();

        /** Return advance in font units, sourced from `hmtx` table. */
        int getAdvanceFU();

        /** Return advance in font em-size [0..1] */
        float getAdvance();

        /** True if kerning values are horizontal, otherwise vertical */
        boolean isKerningHorizontal();
        /** True if kerning values are perpendicular to text flow, otherwise along with flow */
        boolean isKerningCrossstream();

        /** Return the number of kerning values stored for this glyph, associated to a right hand glyph. */
        int getKerningPairCount();

        /**
         * Returns the optional kerning inter-glyph distance within words between this glyph and the given right glyph_id in font-units.
         *
         * @param right_glyphid right glyph code id
         * @return font-units
         */
        int getKerningFU(final int right_glyphid);

        /**
         * Returns the optional kerning inter-glyph distance within words between this glyph and the given right glyph_id in fractional font em-size [0..1].
         *
         * @param right_glyphid right glyph code id
         * @return fractional font em-size distance [0..1]
         */
        float getKerning(final int right_glyphid);

        OutlineShape getShape();

        @Override
        int hashCode();

        @Override
        String toString();

        /** Return all glyph details as string. */
        String fullString();
    }

    String getName(final int nameIndex);

    /** Shall return the family and subfamily name, separated a dash.
     * <p>{@link #getName(StringBuilder, int)} w/ {@link #NAME_FAMILY} and {@link #NAME_SUBFAMILY}</p>
     * <p>Example: "{@code Ubuntu-Regular}"</p>  */
    String getFullFamilyName();

    StringBuilder getAllNames(final StringBuilder string, final String separator);

    /**
     * Return advance-width of given glyphID in font-units, sourced from `hmtx` table.
     * @param glyphID
     */
    int getAdvanceWidthFU(final int glyphID);

    /**
     * Return advance-width of given glyphID in font em-size [0..1]
     * @param glyphID
     */
    float getAdvanceWidth(final int glyphID);

    Metrics getMetrics();

    int getGlyphID(final char symbol);

    Glyph getGlyph(final int glyph_id);

    int getNumGlyphs();

    /**
     * Return line height in font-units, composed from `hheaTable' table entries.
     * <pre>
     *   return abs(lineGap) + abs(descent) abs(ascent);
     * </pre>
     * or
     * <pre>
     *   // lineGap negative value
     *   // descent positive value
     *   // ascent negative value
     *   return -1 * ( lineGap - descent + ascent );
     * </pre>
     */
    int getLineHeightFU();

    /**
     * Return line height in font em-size [0..1]
     */
    float getLineHeight();

    /**
     * Returns metric-bounds in font-units.
     * <p>
     * Metric bounds is based on the `hmtx` table's advance of each glyph and `hheaTable' composed line height.
     * </p>
     * <p>
     * For accurate layout consider using {@link #getGlyphBoundsFU(CharSequence)}.
     * </p>
     * @see #getMetricBounds(CharSequence)
     * @see #getGlyphBoundsFU(CharSequence)
     */
    AABBox getMetricBoundsFU(final CharSequence string);

    /**
     * Returns metric-bounds in font em-size.
     * <p>
     * Metric bounds is based on the `hmtx` table's advance of each glyph and `hheaTable' composed line height.
     * </p>
     * <p>
     * For accurate layout consider using {@link #getGlyphBounds(CharSequence)}.
     * </p>
     * @see #getMetricBoundsFU(CharSequence)
     * @see #getGlyphBounds(CharSequence)
     * @see #getGlyphShapeBounds(CharSequence)
     */
    AABBox getMetricBounds(final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font em-sized bounding box into account.
     * <p>
     * Glyph bounds is based on each glyph's bounding box and `hheaTable' composed line height.
     * </p>
     * @param string string text
     * @return the bounding box of the given string in font em-size [0..1]
     * @see #getGlyphBoundsFU(CharSequence)
     * @see #getGlyphShapeBounds(CharSequence)
     * @see #getMetricBounds(CharSequence)
     */
    AABBox getGlyphBounds(final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font-units sized bounding box into account.
     * <p>
     * Glyph bounds is based on each glyph's bounding box and `hheaTable' composed line height.
     * </p>
     * @param string string text
     * @return the bounding box of the given string in font-units [0..1]
     * @see #getGlyphBounds(CharSequence)
     */
    AABBox getGlyphBoundsFU(final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font em-sized {@link OutlineShape} into account.
     * <p>
     * Glyph shape bounds is based on each glyph's {@link OutlineShape} and `hheaTable' composed line height.
     * </p>
     * <p>
     * This method is only exposed to validate the produced {@link OutlineShape} against {@link #getGlyphBounds(CharSequence)}.
     * </p>
     * @param transform optional given transform
     * @param string string text
     * @return the bounding box of the given string in font-units [0..1]
     * @see #getGlyphShapeBounds(CharSequence)
     * @see #getGlyphBounds(CharSequence)
     * @see #getMetricBounds(CharSequence)
     */
    AABBox getGlyphShapeBounds(final AffineTransform transform, final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font em-sized {@link OutlineShape} into account.
     * <p>
     * Glyph shape bounds is based on each glyph's {@link OutlineShape} and `hheaTable' composed line height.
     * </p>
     * <p>
     * This method is only exposed to validate the produced {@link OutlineShape} against {@link #getGlyphBounds(CharSequence)}.
     * </p>
     * @param string string text
     * @return the bounding box of the given string in font-units [0..1]
     * @see #getGlyphShapeBounds(AffineTransform, CharSequence)
     * @see #getGlyphBounds(CharSequence)
     * @see #getMetricBounds(CharSequence)
     */
    AABBox getGlyphShapeBounds(final CharSequence string);

    boolean isPrintableChar(final char c);

    /**
     * Visit each {@link Glyph}'s {@link OutlineShape} of the string with the {@link OutlineShape.Visitor}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The produced shapes are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * @param visitor handling each glyph's outline shape in font em-size [0..1] and the given {@link AffineTransform}
     * @param transform optional given transform
     * @param font the target {@link Font}
     * @param string string text
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] {@link OutlineShape} into account.
     */
    AABBox processString(final OutlineShape.Visitor visitor, final AffineTransform transform,
                         final CharSequence string);

    /**
     * Visit each {@link Glyph}'s {@link OutlineShape} of the string with the {@link OutlineShape.Visitor}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The produced shapes are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * @param visitor handling each glyph's outline shape in font em-size [0..1] and the given {@link AffineTransform}
     * @param transform optional given transform
     * @param font the target {@link Font}
     * @param string string text
     * @param temp1 temporary AffineTransform storage, mandatory
     * @param temp2 temporary AffineTransform storage, mandatory
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] {@link OutlineShape} into account.
     */
    AABBox processString(final OutlineShape.Visitor visitor, final AffineTransform transform,
                         final CharSequence string,
                         final AffineTransform temp1, final AffineTransform temp2);

    /** Returns {@link #getFullFamilyName()} */
    @Override
    public String toString();

    /** Return all font details as string. */
    String fullString();
}
