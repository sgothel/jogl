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

import jogamp.graph.geom.plane.AffineTransform;

import com.jogamp.graph.curve.OutlineShape;
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
        float getAscent(final float pixelSize);
        float getDescent(final float pixelSize);
        float getLineGap(final float pixelSize);
        float getMaxExtend(final float pixelSize);
        float getScale(final float pixelSize);
        /**
         * @param dest AABBox instance set to this metrics boundary w/ given pixelSize
         * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link Font#getPixelSize(float, float)}
         * @param tmpV3 caller provided temporary 3-component vector
         * @return the given and set AABBox 'dest'
         */
        AABBox getBBox(final AABBox dest, final float pixelSize, final float[] tmpV3);
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
        /**
         *
         * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link Font#getPixelSize(float, float)}
         * @return
         */
        public float getScale(final float pixelSize);
        /**
         * @param dest AABBox instance set to this metrics boundary w/ given pixelSize
         * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link Font#getPixelSize(float, float)}
         * @param tmpV3 caller provided temporary 3-component vector
         * @return the given and set AABBox 'dest'
         */
        public AABBox getBBox(final AABBox dest, final float pixelSize, float[] tmpV3);
        /**
         *
         * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link Font#getPixelSize(float, float)}
         * @param useFrationalMetrics
         * @return
         */
        public float getAdvance(final float pixelSize, boolean useFrationalMetrics);
        public OutlineShape getShape();
        public int hashCode();
    }


    public String getName(final int nameIndex);
    public StringBuilder getName(final StringBuilder string, final int nameIndex);

    /** Shall return the family and subfamily name, separated a dash.
     * <p>{@link #getName(StringBuilder, int)} w/ {@link #NAME_FAMILY} and {@link #NAME_SUBFAMILY}</p>
     * <p>Example: "{@code Ubuntu-Regular}"</p>  */
    public StringBuilder getFullFamilyName(final StringBuilder buffer);

    public StringBuilder getAllNames(final StringBuilder string, final String separator);

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
    public float getPixelSize(final float fontSize /* points per inch */, final float resolution);

    /**
     *
     * @param glyphID
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     * @return
     */
    public float getAdvanceWidth(final int glyphID, final float pixelSize);
    public Metrics getMetrics();
    public Glyph getGlyph(final char symbol);
    public int getNumGlyphs();

    /**
     *
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     * @return
     */
    public float getLineHeight(final float pixelSize);
    /**
     *
     * @param string
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     * @return
     */
    public float getMetricWidth(final CharSequence string, final float pixelSize);
    /**
     *
     * @param string
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     * @param tmp
     * @return
     */
    public float getMetricHeight(final CharSequence string, final float pixelSize, final AABBox tmp);
    /**
     * Return the <i>layout</i> bounding box as computed by each glyph's metrics.
     * The result is not pixel correct, bit reflects layout specific metrics.
     * <p>
     * See {@link #getPointsBounds(AffineTransform, CharSequence, float, AffineTransform, AffineTransform)} for pixel correct results.
     * </p>
     * @param string string text
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     */
    public AABBox getMetricBounds(final CharSequence string, final float pixelSize);

    /**
     * Return the bounding box by taking each glyph's point-based bounding box into account.
     * @param transform optional given transform
     * @param string string text
     * @param pixelSize Use <code>pointSize * resolution</code> for resolution correct pixel-size, see {@link #getPixelSize(float, float)}
     * @param temp1 temporary AffineTransform storage, mandatory
     * @param temp2 temporary AffineTransform storage, mandatory
     */
    public AABBox getPointsBounds(final AffineTransform transform, final CharSequence string, final float pixelSize,
                                  final AffineTransform temp1, final AffineTransform temp2);

    public boolean isPrintableChar(final char c);

    /** Shall return {@link #getFullFamilyName()} */
    @Override
    public String toString();
}
