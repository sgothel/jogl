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

/**
 * Glyph description.
 *
 * <p>
 * Note: The glyph descriptions do not include side bearing information.
 * {@link HmtxTable#getLeftSideBearing(int) Left side bearings} are provided in
 * the {@link HmtxTable 'hmtx'} table, and right side bearings are inferred from
 * the {@link HmtxTable#getAdvanceWidth(int) advance width} (also provided in
 * the 'hmtx' table) and the bounding box coordinates (see
 * {@link #getXMinimum()}, {@link #getYMinimum()}, {@link #getXMaximum()},
 * {@link #getYMaximum()}) provided in the {@link GlyfTable 'glyf'} table. For
 * vertical layout, {@link VmtxTable#getTopSideBearing(int) top side bearings}
 * are provided in the {@link VmtxTable 'vmtx'} table, and bottom side bearings
 * are inferred. The rasterizer will generate a representation of side bearings
 * in the form of “phantom” points, which are added as four additional points at
 * the end of the glyph description and which can be referenced and manipulated
 * by glyph instructions. See the chapter Instructing TrueType Glyphs for more
 * background on phantom points.
 * </p>
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 *
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/glyf"
 */
public abstract class GlyfDescript extends Program implements GlyphDescription {

    /**
     * Bit 0: If set, the point is on the curve; otherwise, it is off the curve.
     */
    public static final byte ON_CURVE_POINT = 0x01;

    /**
     * Bit 1: If set, the corresponding x-coordinate is 1 byte long. If not set,
     * it is two bytes long. For the sign of this value, see the description of
     * the {@link #X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR} flag.
     */
    public static final byte X_SHORT_VECTOR = 0x02;

    /**
     * Bit 2: If set, the corresponding y-coordinate is 1 byte long. If not set,
     * it is two bytes long. For the sign of this value, see the description of
     * the {@link #Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR} flag.
     */
    public static final byte Y_SHORT_VECTOR = 0x04;

    /**
     * Bit 3: If set, the next byte (read as unsigned) specifies the number of
     * additional times this flag byte is to be repeated in the logical flags
     * array — that is, the number of additional logical flag entries inserted
     * after this entry. (In the expanded logical array, this bit is ignored.)
     * In this way, the number of flags listed can be smaller than the number of
     * points in the glyph description.
     */
    public static final byte REPEAT_FLAG = 0x08;

    /**
     * Bit 4: This flag has two meanings, depending on how the
     * {@link #X_SHORT_VECTOR} flag is set. If {@link #X_SHORT_VECTOR} is set,
     * this bit describes the sign of the value, with 1 equaling positive and 0
     * negative. If {@link #X_SHORT_VECTOR} is not set and this bit is set, then
     * the current x-coordinate is the same as the previous x-coordinate. If
     * {@link #X_SHORT_VECTOR} is not set and this bit is also not set, the
     * current x-coordinate is a signed 16-bit delta vector.
     */
    public static final byte X_IS_SAME_OR_POSITIVE_X_SHORT_VECTOR = 0x10;

    /**
     * Bit 5: This flag has two meanings, depending on how the
     * {@link #Y_SHORT_VECTOR} flag is set. If {@link #Y_SHORT_VECTOR} is set,
     * this bit describes the sign of the value, with 1 equaling positive and 0
     * negative. If Y_SHORT_VECTOR is not set and this bit is set, then the
     * current y-coordinate is the same as the previous y-coordinate. If
     * {@link #Y_SHORT_VECTOR} is not set and this bit is also not set, the
     * current y-coordinate is a signed 16-bit delta vector.
     */
    public static final byte Y_IS_SAME_OR_POSITIVE_Y_SHORT_VECTOR = 0x20;

    /**
     * Bit 6: If set, contours in the glyph description may overlap. Use of this
     * flag is not required in OpenType — that is, it is valid to have contours
     * overlap without having this flag set. It may affect behaviors in some
     * platforms, however. (See the discussion of “Overlapping contours” in
     * Apple’s specification for details regarding behavior in Apple platforms.)
     * When used, it must be set on the first flag byte for the glyph. See
     * additional details below.
     *
     * <p>
     * A non-zero-fill algorithm is needed to avoid dropouts when contours
     * overlap. The {@link #OVERLAP_SIMPLE} flag is used by some rasterizer
     * implementations to ensure that a non-zero-fill algorithm is used rather
     * than an even-odd-fill algorithm. Implementations that always use a
     * non-zero-fill algorithm will ignore this flag. Note that some
     * implementations might check this flag specifically in non-variable fonts,
     * but always use a non-zero-fill algorithm for variable fonts. This flag
     * can be used in order to provide broad interoperability of fonts —
     * particularly non-variable fonts — when glyphs have overlapping contours.
     * </p>
     *
     * <p>
     * Note that variable fonts often make use of overlapping contours. This has
     * implications for tools that generate static-font data for a specific
     * instance of a variable font, if broad interoperability of the derived
     * font is desired: if a glyph has overlapping contours in the given
     * instance, then the tool should either set this flag in the derived glyph
     * data, or else should merge contours to remove overlap of separate
     * contours.
     * </p>
     *
     * <p>
     * Note: The OVERLAP_COMPOUND flag, described below, has a similar purpose
     * in relation to composite glyphs. The same considerations described for
     * the OVERLAP_SIMPLE flag also apply to the OVERLAP_COMPOUND flag.
     * </p>
     */
    public static final byte OVERLAP_SIMPLE = 0x40;

    final GlyfTable _parentTable;
    private final int _glyphIndex;
    private final int _numberOfContours;
    private final short _xMin;
    private final short _yMin;
    private final short _xMax;
    private final short _yMax;

    GlyfDescript(
            final GlyfTable parentTable,
            final int glyphIndex,
            final short numberOfContours,
            final DataInput di) throws IOException {
        _glyphIndex = glyphIndex;
        _parentTable = parentTable;
        _numberOfContours = numberOfContours;
        _xMin = di.readShort();
        _yMin = di.readShort();
        _xMax = di.readShort();
        _yMax = di.readShort();
    }

    /**
     * The {@link GlyfTable} this {@link GlyfDescript} belongs to.
     */
    public GlyfTable getParentTable() {
        return _parentTable;
    }

    @Override
    public int getGlyphIndex() {
        return _glyphIndex;
    }

    @Override
    public int getNumberOfContours() {
        return _numberOfContours;
    }

    @Override
    public short getXMinimum() {
        return _xMin;
    }

    @Override
    public short getYMinimum() {
        return _yMin;
    }

    @Override
    public short getXMaximum() {
        return _xMax;
    }

    @Override
    public short getYMaximum() {
        return _yMax;
    }

    @Override
    public String toString() {
        return "          numberOfContours: " + _numberOfContours +
                "\n          xMin:             " + _xMin +
                "\n          yMin:             " + _yMin +
                "\n          xMax:             " + _xMax +
                "\n          yMax:             " + _yMax;
    }
}
