/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
import com.jogamp.graph.curve.opengl.TextRegionUtil.ShapeVisitor;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * Interface wrapper for font implementation.
 * <p>
 * TrueType Font Specification:
 * <ul>
 *   <li>http://www.freetype.org/freetype2/documentation.html</li>
 *   <li>http://developer.apple.com/fonts/ttrefman/rm06/Chap6.html</li>
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
     * Vertical http://developer.apple.com/fonts/TTRefMan/RM06/Chap6vhea.html
     * Horizontal http://developer.apple.com/fonts/TTRefMan/RM06/Chap6hhea.html
     */
    public interface Metrics {
        float getAscent(float pixelSize);
        float getDescent(float pixelSize);
        float getLineGap(float pixelSize);
        float getMaxExtend(float pixelSize);
        float getScale(float pixelSize);
        /**
         * @param pixelSize
         * @param tmpV3 caller provided temporary 3-component vector
         * @return
         */
        AABBox getBBox(float pixelSize, float[] tmpV3);
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

        public Font getFont();
        public char getSymbol();
        public short getID();
        public AABBox getBBox();
        public float getScale(float pixelSize);
        /**
         * @param pixelSize
         * @param tmpV3 caller provided temporary 3-component vector
         * @return
         */
        public AABBox getBBox(float pixelSize, float[] tmpV3);
        public float getAdvance(float pixelSize, boolean useFrationalMetrics);
        public OutlineShape getShape();
        public int hashCode();
    }


    public String getName(int nameIndex);
    public StringBuilder getName(StringBuilder string, int nameIndex);

    /** Shall return the family and subfamily name, separated a dash.
     * <p>{@link #getName(StringBuilder, int)} w/ {@link #NAME_FAMILY} and {@link #NAME_SUBFAMILY}</p>
     * <p>Example: "{@code Ubuntu-Regular}"</p>  */
    public StringBuilder getFullFamilyName(StringBuilder buffer);

    public StringBuilder getAllNames(StringBuilder string, String separator);

    /**
     * <pre>
        Font Scale Formula:
         inch: 25.4 mm
         pointSize: [point] = [1/72 inch]

         [1]      Scale := pointSize * resolution / ( 72 points per inch * units_per_em )
         [2]  PixelSize := pointSize * resolution / ( 72 points per inch )
         [3]      Scale := PixelSize / units_per_em
     * </pre>
     * @param fontSize in point-per-inch
     * @param resolution display resolution in dots-per-inch
     * @return pixel-per-inch, pixelSize scale factor for font operations.
     */
    public float getPixelSize(float fontSize /* points per inch */, float resolution);

    public float getAdvanceWidth(int glyphID, float pixelSize);
    public Metrics getMetrics();
    public Glyph getGlyph(char symbol);
    public int getNumGlyphs();

    public float getLineHeight(float pixelSize);
    public float getMetricWidth(CharSequence string, float pixelSize);
    public float getMetricHeight(CharSequence string, float pixelSize);
    /**
     * Return the <i>layout</i> bounding box as computed by each glyph's metrics.
     * The result is not pixel correct, bit reflects layout specific metrics.
     * <p>
     * See {@link #getPointsBounds(AffineTransform, CharSequence, float)} for pixel correct results.
     * </p>
     * @param string string text
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     */
    public AABBox getMetricBounds(CharSequence string, float pixelSize);

    /**
     * Return the bounding box by taking each glyph's point-based bounding box into account.
     * @param transform optional given transform
     * @param string string text
     * @param pixelSize Use {@link Font#getPixelSize(float, float)} for resolution correct pixel-size.
     */
    public AABBox getPointsBounds(final AffineTransform transform, CharSequence string, float pixelSize);

    public boolean isPrintableChar( char c );

    /** Shall return {@link #getFullFamilyName()} */
    @Override
    public String toString();
}
