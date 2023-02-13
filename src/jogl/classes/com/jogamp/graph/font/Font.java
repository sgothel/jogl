/**
// * Copyright 2010 JogAmp Community. All rights reserved.
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
         * @return ascent in font-units to be divided by {@link #getUnitsPerEM()}
         */
        int getAscentFU();

        /**
         * @return ascent in font em-size [0..1]
         */
        float getAscent();

        /**
         * @return descent in font-units to be divided by {@link #getUnitsPerEM()}
         */
        int getDescentFU();

        /**
         * @return descend in font em-size [0..1]
         */
        float getDescent();

        /**
         * @return line-gap in font-units to be divided by {@link #getUnitsPerEM()}
         */
        int getLineGapFU();

        /**
         * @return line-gap in font em-size [0..1]
         */
        float getLineGap();

        /**
         * @return max-extend in font-units to be divided by {@link #getUnitsPerEM()}
         */
        int getMaxExtendFU();

        /**
         * @return max-extend in font em-size [0..1]
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
        char getSymbol();

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
         * Return the AABBox in font-units to be divided by unitsPerEM
         */
        AABBox getBBoxFU();

        /**
         * Return the AABBox in font-units to be divided by unitsPerEM
         * @param dest AABBox instance set to this metrics boundary in font-units
         * @return the given and set AABBox 'dest' in font-units
         */
        AABBox getBBoxFU(final AABBox dest);

        /**
         * @param dest AABBox instance set to this metrics boundary in font em-size [0..1]
         * @param tmpV3 caller provided temporary 3-component vector
         * @return the given and set AABBox 'dest' in font em-size [0..1]
         */
        AABBox getBBox(final AABBox dest, float[] tmpV3);

        /** Return advance in font units to be divided by unitsPerEM */
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
         * Returns the optional kerning inter-glyph distance within words between this glyph and the given right glyph_id in font-units to be divided by unitsPerEM
         *
         * @param right_glyphid right glyph code id
         * @return font-units to be divided by unitsPerEM
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
     * Return advance-width of given glyphID in font-units to be divided by unitsPerEM
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

    Glyph getGlyph(final char symbol);

    int getNumGlyphs();

    /**
     * Return line height in font-units to be divided by unitsPerEM
     */
    int getLineHeightFU();

    /**
     * Return line height in font em-size [0..1]
     */
    float getLineHeight();

    /** Return metric-width in font-units */
    int getMetricWidthFU(final CharSequence string);

    /** Return metric-width in font em-size */
    float getMetricWidth(final CharSequence string);

    /** Return metric-height in font-units */
    int getMetricHeightFU(final CharSequence string);

    /** Return metric-height in font em-size */
    float getMetricHeight(final CharSequence string);

    /** Return layout metric-bounds in font-units, see {@link #getMetricBounds(CharSequence, float)} */
    AABBox getMetricBoundsFU(final CharSequence string);

    /** Return layout metric-bounds in font em-size, see {@link #getMetricBounds(CharSequence, float)} */
    AABBox getMetricBounds(final CharSequence string);

    /**
     * Return the bounding box by taking each glyph's font-unit sized bounding box into account.
     * @param transform optional given transform
     * @param string string text
     * @return the bounding box of the given string in font-units [0..1]
     */
    AABBox getPointsBoundsFU(final AffineTransform transform, final CharSequence string);

    /**
     * Return the bounding box by taking each glyph's font em-sized bounding box into account.
     * @param transform optional given transform
     * @param string string text
     * @return the bounding box of the given string in font em-size [0..1]
     */
    AABBox getPointsBounds(final AffineTransform transform, final CharSequence string);

    boolean isPrintableChar(final char c);

    /** Shall return {@link #getFullFamilyName()} */
    @Override
    public String toString();

    /** Return all font details as string. */
    String fullString();
}
