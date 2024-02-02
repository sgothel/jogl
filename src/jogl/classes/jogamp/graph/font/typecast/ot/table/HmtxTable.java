/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;

import jogamp.graph.font.typecast.ot.Fmt;

/**
 * Horizontal Metrics Table
 *
 * <p>
 * Glyph metrics used for horizontal text layout include glyph advance widths,
 * side bearings and X-direction min and max values (xMin, xMax). These are
 * derived using a combination of the glyph outline data ('glyf', 'CFF ' or
 * CFF2) and the horizontal metrics table. The horizontal metrics ('hmtx') table
 * provides glyph advance widths and left side bearings.
 * </p>
 *
 * <p>
 * In a font with TrueType outline data, the 'glyf' table provides xMin and xMax
 * values, but not advance widths or side bearings. The advance width is always
 * obtained from the 'hmtx' table. In some fonts, depending on the state of
 * flags in the 'head' table, the left side bearings may be the same as the xMin
 * values in the 'glyf' table, though this is not true for all fonts. (See the
 * description of bit 1 of the {@link HeadTable#getFlags() flags} field in the
 * 'head' table.) For this reason, left side bearings are provided in the 'hmtx'
 * table. The right side bearing is always derived using advance width and left
 * side bearing values from the 'hmtx' table, plus bounding-box information in
 * the glyph description — see below for more details.
 * </p>
 *
 * <p>
 * In a variable font with TrueType outline data, the left side bearing value in
 * the 'hmtx' table must always be equal to xMin (bit 1 of the 'head'
 * {@link HeadTable#getFlags() flags} field must be set). Hence, these values
 * can also be derived directly from the 'glyf' table. Note that these values
 * apply only to the default instance of the variable font: non-default
 * instances may have different side bearing values. These can be derived from
 * interpolated “phantom point” coordinates using the 'gvar' table (see below
 * for additional details), or by applying variation data in the HVAR table to
 * default-instance values from the 'glyf' or 'hmtx' table.
 * </p>
 *
 * <p>
 * In a font with CFF version 1 outline data, the 'CFF ' table does include
 * advance widths. These values are used by PostScript processors, but are not
 * used in OpenType layout. In an OpenType context, the 'hmtx' table is required
 * and must be used for advance widths. Note that fonts in a Font Collection
 * file that share a 'CFF ' table may specify different advance widths in
 * font-specific 'hmtx' tables for a particular glyph index. Also note that the
 * CFF2 table does not include advance widths. In addition, for either CFF or
 * CFF2 data, there are no explicit xMin and xMax values; side bearings are
 * implicitly contained within the CharString data, and can be obtained from the
 * the CFF / CFF2 rasterizer. Some layout engines may use left side bearing
 * values in the 'hmtx' table, however; hence, font production tools should
 * ensure that the lsb values in the 'hmtx' table match the implicit xMin values
 * reflected in the CharString data. In a variable font with CFF2 outline data,
 * left side bearing and advance width values for non-default instances should
 * be obtained by combining information from the 'hmtx' and HVAR tables.
 * </p>
 *
 * <p>
 * The table uses a longHorMetric record to give the advance width and left side
 * bearing of a glyph. Records are indexed by glyph ID. As an optimization, the
 * number of records can be less than the number of glyphs, in which case the
 * advance width value of the last record applies to all remaining glyph IDs.
 * This can be useful in monospaced fonts, or in fonts that have a large number
 * of glyphs with the same advance width (provided the glyphs are ordered
 * appropriately). The number of longHorMetric records is determined by the
 * numberOfHMetrics field in the 'hhea' table.
 * </p>
 *
 * <p>
 * If the longHorMetric array is less than the total number of glyphs, then that
 * array is followed by an array for the left side bearing values of the
 * remaining glyphs. The number of elements in the left side bearing will be
 * derived from numberOfHMetrics plus the numGlyphs field in the 'maxp' table.
 * </p>
 *
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/hmtx"
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class HmtxTable implements Table {

    private final int[] _hMetrics;
    private final short[] _leftSideBearing;
    private final int _length;

    /**
     * Creates a {@link HmtxTable}.
     *
     * @param di
     *        The reader to read from.
     * @param length
     *        The length of the table in bytes.
     * @param hhea
     *        The corresponding {@link HheaTable}.
     * @param maxp
     *        The corresponding {@link MaxpTable}.
     */
    public HmtxTable(
            final DataInput di,
            final int length,
            final HheaTable hhea,
            final MaxpTable maxp) throws IOException {

        // Paired advance width and left side bearing values for each glyph.
        // Records are indexed by glyph ID.
        _hMetrics = new int[hhea.getNumberOfHMetrics()];
        for (int i = 0; i < hhea.getNumberOfHMetrics(); ++i) {
            _hMetrics[i] =
                    di.readUnsignedByte()<<24
                    | di.readUnsignedByte()<<16
                    | di.readUnsignedByte()<<8
                    | di.readUnsignedByte();
        }

        // Left side bearings for glyph IDs greater than or equal to
        // numberOfHMetrics.
        final int lsbCount = maxp.getNumGlyphs() - hhea.getNumberOfHMetrics();
        _leftSideBearing = new short[lsbCount];
        for (int i = 0; i < lsbCount; ++i) {
            _leftSideBearing[i] = di.readShort();
        }
        _length = length;
    }

    @Override
    public int getType() {
        return hmtx;
    }

    /**
     * uint16
     *
     * Advance width, in font design units.
     *
     * <p>
     * The baseline is an imaginary line that is used to ‘guide’ glyphs when
     * rendering text. It can be horizontal (e.g., Latin, Cyrillic, Arabic) or
     * vertical (e.g., Chinese, Japanese, Mongolian). Moreover, to render text,
     * a virtual point, located on the baseline, called the pen position or
     * origin, is used to locate glyphs.
     * </p>
     *
     * <p>
     * The distance between two successive pen positions is glyph-specific and
     * is called the advance width. Note that its value is always positive, even
     * for right-to-left oriented scripts like Arabic. This introduces some
     * differences in the way text is rendered.
     * </p>
     *
     * @param i
     *        The glyph index, see {@link GlyfTable#getNumGlyphs()}.
     *
     * @see "https://www.freetype.org/freetype2/docs/glyphs/glyphs-3.html"
     */
    public int getAdvanceWidth(final int i) {
        if (_hMetrics == null) {
            return 0;
        }
        if (i < _hMetrics.length) {
            return _hMetrics[i] >>> 16;
        } else {
            return _hMetrics[_hMetrics.length - 1] >>> 16;
        }
    }

    /**
     * int16
     *
     * Glyph left side bearing, in font design units.
     *
     * <p>
     * The horizontal distance from the current pen position to the glyph's left
     * bbox edge. It is positive for horizontal layouts, and in most cases
     * negative for vertical ones.
     * </p>
     *
     * @param i
     *        The glyph index, see {@link GlyfTable#getNumGlyphs()}.
     *
     * @see "https://www.freetype.org/freetype2/docs/glyphs/glyphs-3.html"
     */
    public short getLeftSideBearing(final int i) {
        if (_hMetrics == null) {
            return 0;
        }
        if (i < _hMetrics.length) {
            return (short)(_hMetrics[i] & 0xffff);
        } else {
            return _leftSideBearing[i - _hMetrics.length];
        }
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'hmtx' Table - Horizontal Metrics\n");
        sb.append("---------------------------------\n");
        sb.append("        Size:   ").append(_length).append(" bytes\n");
        sb.append("        Length: ").append(_hMetrics.length).append(" entries\n");
        for (int i = 0; i < _hMetrics.length; i++) {
            sb.append("        ").append(Fmt.pad(6, i)).append(": ");
            sb.append("adv=").append(getAdvanceWidth(i));
            sb.append(", lsb=").append(getLeftSideBearing(i));
            sb.append("\n");
        }
        for (int i = 0; i < _leftSideBearing.length; i++) {
            sb.append("        ").append(Fmt.pad(6, i + _hMetrics.length)).append(": ");
            sb.append("lsb=").append(_leftSideBearing[i]);
            sb.append("\n");
        }
        return sb.toString();
    }

}
