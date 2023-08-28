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
         * Distance from baseline of highest ascender, a positive value.
         * @return ascent in font-units, sourced from `hhea' table.
         */
        int getAscentFU();

        /**
         * Distance from baseline of highest ascender, a positive value.
         * @return ascent in font em-size [0..1], sourced from `hhea' table.
         */
        float getAscent();

        /**
         * Distance from baseline of lowest descender, a negative value.
         * @return descent in font-units, sourced from `hhea' table.
         */
        int getDescentFU();

        /**
         * Distance from baseline of lowest descender, a negative value.
         * @return descend in font em-size [0..1], sourced from `hhea' table.
         */
        float getDescent();

        /**
         * Typographic line gap, a positive value.
         * @return line-gap in font-units, sourced from `hhea' table.
         */
        int getLineGapFU();

        /**
         * Typographic line gap, a positive value.
         * @return line-gap in font em-size [0..1], sourced from `hhea' table.
         */
        float getLineGap();

        /**
         * max(lsb + (xMax-xMin)), a positive value.
         * @return max-extend in font-units, sourced from `hhea' table.
         */
        int getMaxExtendFU();

        /**
         * max(lsb + (xMax-xMin)), a positive value.
         * @return max-extend in font em-size [0..1], sourced from `hhea' table.
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
        AABBox getBoundsFU(final AABBox dest);

        /**
         * @param dest AABBox instance set to this metrics boundary in font em-size [0..1]
         * @return the given and set AABBox 'dest' in font units
         */
        AABBox getBounds(final AABBox dest);
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

        /** Return the {@link Font} owning this {@link Glyph}. */
        Font getFont();

        /** Return this glyph's ID */
        int getID();

        /** Return the glyph's name, source from `post` table */
        String getName();

        /**
         * Return true if the glyph is a whitespace, otherwise false.
         * <p>
         * A whitespace glyph has no {@link #getShape()}, but a valid {@link #getBounds()} and {@link #getAdvance()}.
         * </p>
         * Being a whitespace glyph excludes {@link #isUndefined()}.
         * @see #isUndefined()
         * @see #isNonContour()
         */
        boolean isWhitespace();

        /**
         * Return true if the Glyph denotes an undefined {@link #getID()} symbol as follows
         * <ul>
         *   <li>it's glyph index is {@link #ID_UNKNOWN}, i.e. {@code 0x00}</li>
         *   <li>has the {@link #getName() name} `.notdef`, `NULL`, `null` or `.null`</li>
         *   <li>has no original underlying shape</li>
         * </ul>
         * <p>
         * An undefined glyph has no {@link #getShape()}  if glyph index is not {@link #ID_UNKNOWN}.
         * </p>
         * <p>
         * An undefined glyph has a default {@link #getBounds()} and {@link #getAdvance()}.
         * </p>
         * Being an undefined shape excludes {@link #isWhitespace()}.
         * @see #isWhitespace()
         * @see #isNonContour()
         */
        boolean isUndefined();

        /**
         * Returns true if {@link #isWhitespace()} or {@link #isUndefined()}.
         * @see #isWhitespace()
         * @see #isUndefined()
         */
        boolean isNonContour();

        /**
         * Return the AABBox in font-units, borrowing internal instance.
         */
        AABBox getBoundsFU();

        /**
         * Return the AABBox in font-units, copying into given dest.
         * @param dest AABBox instance set to this metrics boundary in font-units
         * @return the given and set AABBox 'dest' in font-units
         */
        AABBox getBoundsFU(final AABBox dest);

        /**
         * Return the AABBox in font em-size [0..1], copying into given dest.
         * @param dest AABBox instance set to this metrics boundary in font em-size [0..1]
         * @return the given and set AABBox 'dest' in font em-size [0..1]
         */
        AABBox getBounds(final AABBox dest);

        /**
         * Return the AABBox in font em-size [0..1], creating a new copy.
         */
        AABBox getBounds();

        /** Return advance in font units, sourced from `hmtx` table. */
        int getAdvanceFU();

        /** Return advance in font em-size [0..1], sourced from `hmtx` table. */
        float getAdvance();

        /** Return leftSideBearings in font units, sourced from `hmtx` table. */
        int getLeftSideBearingsFU();

        /** Return leftSideBearings in font em-size [0..1], sourced from `hmtx` table. */
        float getLeftSideBearings();

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

    /**
     * General purpose {@link Font.Glyph} visitor.
     */
    public static interface GlyphVisitor {
        /**
         * Visiting the given {@link Font.Glyph} having an {@link OutlineShape} with it's corresponding {@link AffineTransform}.
         * @param symbol the character symbol matching the given glyph
         * @param glyph {@link Font.Glyph} which contains an {@link OutlineShape} via {@link Font.Glyph#getShape()}.
         * @param t may be used immediately as is, otherwise a copy shall be made if stored.
         */
        public void visit(final char symbol, final Glyph glyph, final AffineTransform t);
    }

    /**
     * Constrained {@link Font.Glyph} visitor w/o {@link AffineTransform}.
     */
    public static interface GlyphVisitor2 {
        /**
         * Visiting the given {@link Font.Glyph} having an {@link OutlineShape}.
         * @param symbol the character symbol matching the given glyph
         * @param glyph {@link Font.Glyph} which contains an {@link OutlineShape} via {@link Font.Glyph#getShape()}.
         */
        public void visit(final char symbol, final Glyph glyph);
    }


    String getName(final int nameIndex);

    /** Shall return the family and subfamily name, separated a dash.
     * <p>{@link #getName(StringBuilder, int)} w/ {@link #NAME_FAMILY} and {@link #NAME_SUBFAMILY}</p>
     * <p>Example: "{@code Ubuntu-Regular}"</p>  */
    String getFullFamilyName();

    StringBuilder getAllNames(final StringBuilder string, final String separator);

    /**
     * Returns the hash code based on {@link #NAME_UNIQUNAME}.
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    int hashCode();

    /**
     * Returns true if other instance is of same type and {@link #NAME_UNIQUNAME} is equal.
     * <p>
     * {@inheritDoc}
     * </p>
     */
    @Override
    boolean equals(final Object o);

    /**
     * Return advance-width of given glyphID in font-units, sourced from `hmtx` table.
     * @param glyphID
     */
    int getAdvanceWidthFU(final int glyphID);

    /**
     * Return advance-width of given glyphID in font em-size [0..1], sourced from `hmtx` table.
     * @param glyphID
     */
    float getAdvanceWidth(final int glyphID);

    Metrics getMetrics();

    /** Return the {@link Glyph} ID mapped to given `symbol`, usually UTF16 unicode. Returned ID can be used to retrieve the {@link Glyph} via {@link #getGlyph(int)}. */
    int getGlyphID(final char symbol);

    /** Return number of {@link Glyph} IDs available, i.e. retrievable via {@link #getGlyph(int)} [0..count). */
    int getGlyphCount();

    /** Return the {@link Glyph} using given ID, see {@link #getGlyphCount()}. */
    Glyph getGlyph(final int glyph_id);

    int getNumGlyphs();

    /**
     * Return line height, baseline-to-baseline in font-units, composed from `hhea' table entries.
     * <pre>
     *   return ascent - descent + linegap;
     * </pre>
     * or
     * <pre>
     *   // lineGap positive value
     *   // descent negative value
     *   // ascent positive value
     *   return ascent - descent + linegap;
     * </pre>
     * @see Metrics#getAscentFU()
     * @see Metrics#getDescentFU()
     * @see Metrics#getLineGapFU()
     */
    int getLineHeightFU();

    /**
     * Return line height, baseline-to-baseline in em-size [0..1], composed from `hhea' table entries.
     * <pre>
     *   return ascent - descent + linegap;
     * </pre>
     * or
     * <pre>
     *   // lineGap positive value
     *   // descent negative value
     *   // ascent positive value
     *   return ascent - descent + linegap;
     * </pre>
     * @see Metrics#getAscent()
     * @see Metrics#getDescent()
     * @see Metrics#getLineGap()
     */
    float getLineHeight();

    /**
     * Returns metric-bounds in font-units.
     * <p>
     * Metric bounds is based on the `hmtx` table's advance of each glyph and `hhea' composed line height.
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
     * Metric bounds is based on the `hmtx` table's advance of each glyph and `hhea' composed line height.
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
     * Try using {@link #getGlyphBounds(CharSequence, AffineTransform, AffineTransform)} to reuse {@link AffineTransform} instances.
     */
    AABBox getGlyphBounds(final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font em-sized bounding box into account.
     * <p>
     * Glyph bounds is based on each glyph's bounding box and `hhea' composed line height.
     * </p>
     * @param string string text
     * @param tmp1 temp {@link AffineTransform} to be reused
     * @param tmp2 temp {@link AffineTransform} to be reused
     * @return the bounding box of the given string in font em-size [0..1]
     * @see #getGlyphBoundsFU(CharSequence)
     * @see #getGlyphShapeBounds(CharSequence)
     * @see #getMetricBounds(CharSequence)
     */
    AABBox getGlyphBounds(final CharSequence string, final AffineTransform tmp1, final AffineTransform tmp2);

    /**
     * Try using {@link #getGlyphBoundsFU(CharSequence, AffineTransform, AffineTransform)} to reuse {@link AffineTransform} instances.
     */
    AABBox getGlyphBoundsFU(final CharSequence string);

    /**
     * Returns accurate bounding box by taking each glyph's font-units sized bounding box into account.
     * <p>
     * Glyph bounds is based on each glyph's bounding box and `hhea' composed line height.
     * </p>
     * @param string string text
     * @param tmp1 temp {@link AffineTransform} to be reused
     * @param tmp2 temp {@link AffineTransform} to be reused
     * @return the bounding box of the given string in font-units [0..1]
     * @see #getGlyphBounds(CharSequence)
     */
    AABBox getGlyphBoundsFU(final CharSequence string, final AffineTransform tmp1, final AffineTransform tmp2);

    /**
     * Returns accurate bounding box by taking each glyph's font em-sized {@link OutlineShape} into account.
     * <p>
     * Glyph shape bounds is based on each glyph's {@link OutlineShape} and `hhea' composed line height.
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
     * Glyph shape bounds is based on each glyph's {@link OutlineShape} and `hhea' composed line height.
     * </p>
     * <p>
     * This method is only exposed to validate the produced {@link OutlineShape} against {@link #getGlyphBounds(CharSequence)}.
     * </p>
     * @param transform optional given transform
     * @param string string text
     * @param tmp1 temp {@link AffineTransform} to be reused
     * @param tmp2 temp {@link AffineTransform} to be reused
     * @return the bounding box of the given string in font-units [0..1]
     * @see #getGlyphShapeBounds(CharSequence)
     * @see #getGlyphBounds(CharSequence)
     * @see #getMetricBounds(CharSequence)
     */
    AABBox getGlyphShapeBounds(final AffineTransform transform, final CharSequence string, final AffineTransform tmp1, final AffineTransform tmp2);

    boolean isPrintableChar(final char c);

    /**
     * Try using {@link #processString(GlyphVisitor, AffineTransform, CharSequence, AffineTransform, AffineTransform)}
     * to reuse {@link AffineTransform} instances.
     * @see #processString(GlyphVisitor, AffineTransform, CharSequence, AffineTransform, AffineTransform)
     */
    AABBox processString(final Font.GlyphVisitor visitor, final AffineTransform transform,
                         final CharSequence string);

    /**
     * Visit each {@link Glyph} and perhaps its {@link OutlineShape} of the string with the {@link Font.GlyphVisitor}
     * while passing the progressed {@link AffineTransform}.
     * <p>
     * The processed shapes are in font em-size [0..1], but can be adjusted with the given transform, progressed and passed to the visitor.
     * </p>
     * @param visitor handling each {@link Font.Glyph} and perhaps its {@link OutlineShape} in font em-size [0..1] and the given {@link AffineTransform}
     * @param transform optional given transform for size and position
     * @param font the target {@link Font}
     * @param string string text
     * @param temp1 temporary AffineTransform storage, mandatory
     * @param temp2 temporary AffineTransform storage, mandatory
     * @return the bounding box of the given string by taking each glyph's font em-sized [0..1] {@link OutlineShape} into account.
     * @see #processString(GlyphVisitor, AffineTransform, CharSequence)
     */
    AABBox processString(final Font.GlyphVisitor visitor, final AffineTransform transform,
                         final CharSequence string,
                         final AffineTransform temp1, final AffineTransform temp2);

    /**
     * Visit each {@link Glyph} and perhaps its {@link OutlineShape} of the string with the constrained {@link Font.GlyphVisitor2}.
     * <p>
     * The processed shapes are in font em-size [0..1].
     * </p>
     * @param visitor handling each {@link Font.Glyph} and perhaps its {@link OutlineShape} in font em-size [0..1]
     * @param string string text
     */
    void processString(final Font.GlyphVisitor2 visitor, final CharSequence string);

    /** Returns {@link #getFullFamilyName()} */
    @Override
    public String toString();

    /** Return all font details as string. */
    String fullString();
}
