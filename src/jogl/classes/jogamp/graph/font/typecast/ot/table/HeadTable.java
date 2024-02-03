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

import jogamp.graph.font.typecast.ot.Fixed;
import jogamp.graph.font.typecast.ot.LongDateTime;

/**
 * Font Header Table
 * 
 * This table gives global information about the font. The bounding box values
 * ({@link #getXMin()}, {@link #getXMax()}, {@link #getYMin()},
 * {@link #getYMax()}) should be computed using only glyphs that have contours.
 * Glyphs with no contours should be ignored for the purposes of these
 * calculations.
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class HeadTable implements Table {
    
    /**
     * @see #getMagicNumber()
     */
    public static final int MAGIC = 0x5F0F3CF5;

    /**
     * @see #getGlyphDataFormat()
     */
    public static final short GLYPH_DATA_FORMAT = 0;

    private int _versionNumber;
    
    private int _fontRevision;
    
    private int _checkSumAdjustment;
    
    private int _magicNumber = MAGIC;
    
    private short _flags;
    
    private short _unitsPerEm;
    
    private long _created;
    
    private long _modified;
    
    private short _xMin;
    
    private short _yMin;
    
    private short _xMax;
    
    private short _yMax;
    
    private short _macStyle;
    
    private short _lowestRecPPEM;
    
    private short _fontDirectionHint;
    
    private short _indexToLocFormat;
    
    private short _glyphDataFormat = GLYPH_DATA_FORMAT;

    /**
     * Creates a {@link HeadTable} from binary encoding.
     */
    public HeadTable(DataInput di) throws IOException {
        _versionNumber = di.readInt();
        _fontRevision = di.readInt();
        _checkSumAdjustment = di.readInt();
        _magicNumber = di.readInt();
        _flags = di.readShort();
        _unitsPerEm = di.readUnsignedShort();
        _created = di.readLong();
        _modified = di.readLong();
        _xMin = di.readShort();
        _yMin = di.readShort();
        _xMax = di.readShort();
        _yMax = di.readShort();
        _macStyle = di.readShort();
        _lowestRecPPEM = di.readShort();
        _fontDirectionHint = di.readShort();
        _indexToLocFormat = di.readShort();
        _glyphDataFormat = di.readShort();
    }

    @Override
    public int getType() {
        return head;
    }

    /**
     * uint16     majorVersion     Major version number of the font header table — set to 1.
     * uint16     minorVersion     Minor version number of the font header table — set to 0.
     */
    public int getVersionNumber() {
        return _versionNumber;
    }

    public long getCreated() {
        return _created;
    }

    public short getFlags() {
        return _flags;
    }

    /**
     * Fixed Set by font manufacturer.
     * 
     * For historical reasons, the fontRevision value contained in this table is
     * not used by Windows to determine the version of a font. Instead, Windows
     * evaluates the version string (ID 5) in the 'name' table.
     */
    public int getFontRevision(){
        return _fontRevision;
    }

    /**
     * uint32
     * 
     * To compute: set it to 0, sum the entire font as uint32, then store
     * 0xB1B0AFBA - sum.
     * 
     * If the font is used as a component in a font collection file, the value
     * of this field will be invalidated by changes to the file structure and
     * font table directory, and must be ignored.
     */
    public int getCheckSumAdjustment() {
        return _checkSumAdjustment;
    }
    
    /**
     * uint32     Set to {@link #MAGIC}.
     */
    public int getMagicNumber() {
        return _magicNumber;
    }

    /**
     * uint16
     * 
     * Bit 0: Baseline for font at y=0;
     * 
     * Bit 1: Left sidebearing point at x=0 (relevant only for TrueType
     * rasterizers) — see the note below regarding variable fonts;
     * 
     * Bit 2: Instructions may depend on point size;
     * 
     * Bit 3: Force ppem to integer values for all internal scaler math; may use
     * fractional ppem sizes if this bit is clear;
     * 
     * Bit 4: Instructions may alter advance width (the advance widths might not
     * scale linearly);
     * 
     * Bit 5: This bit is not used in OpenType, and should not be set in order
     * to ensure compatible behavior on all platforms. If set, it may result in
     * different behavior for vertical layout in some platforms. (See Apple’s
     * specification for details regarding behavior in Apple platforms.)
     * 
     * Bits 6–10: These bits are not used in Opentype and should always be
     * cleared. (See Apple’s specification for details regarding legacy used in
     * Apple platforms.)
     * 
     * Bit 11: Font data is “lossless” as a result of having been subjected to
     * optimizing transformation and/or compression (such as e.g. compression
     * mechanisms defined by ISO/IEC 14496-18, MicroType Express, WOFF 2.0 or
     * similar) where the original font functionality and features are retained
     * but the binary compatibility between input and output font files is not
     * guaranteed. As a result of the applied transform, the DSIG table may also
     * be invalidated.
     * 
     * Bit 12: Font converted (produce compatible metrics)
     * 
     * Bit 13: Font optimized for ClearType™. Note, fonts that rely on embedded
     * bitmaps (EBDT) for rendering should not be considered optimized for
     * ClearType, and therefore should keep this bit cleared.
     * 
     * Bit 14: Last Resort font. If set, indicates that the glyphs encoded in
     * the 'cmap' subtables are simply generic symbolic representations of code
     * point ranges and don’t truly represent support for those code points. If
     * unset, indicates that the glyphs encoded in the 'cmap' subtables
     * represent proper support for those code points.
     * 
     * Bit 15: Reserved, set to 0
     * 
     * Note that, in a variable font with TrueType outlines, the left side
     * bearing for each glyph must equal {@link #_xMin}, and bit 1 in the flags
     * field must be set. 
     * 
     * Also, bit 5 must be cleared in all variable fonts. For
     * general information on OpenType Font Variations, see the chapter,
     * OpenType Font Variations Overview.
     */
    public short getFlags() {
        return _flags;
    }

    /**
     * uint16
     * 
     * Set to a value from 16 to 16384. Any value in this range is valid. In
     * fonts that have TrueType outlines, a power of 2 is recommended as this
     * allows performance optimizations in some rasterizers.
     */
    public short getUnitsPerEm() {
        return _unitsPerEm;
    }

    /**
     * LONGDATETIME
     * 
     * Number of seconds since 12:00 midnight that started January 1st 1904 in
     * GMT/UTC time zone. 64-bit integer
     */
    public long getCreated() {
        return _created;
    }

    /**
     * LONGDATETIME
     * 
     * Number of seconds since 12:00 midnight that started January 1st 1904 in
     * GMT/UTC time zone. 64-bit integer
     */
    public long getModified() {
        return _modified;
    }

    /**
     * int16     
     * 
     * For all glyph bounding boxes.
     */
    public short getXMin() {
        return _xMin;
    }

    /**
     * int16     
     * 
     * For all glyph bounding boxes.
     */
    public short getYMin() {
        return _yMin;
    }

    /**
     * int16     
     * 
     * For all glyph bounding boxes.
     */
    public short getXMax() {
        return _xMax;
    }

    /**
     * int16     
     * 
     * For all glyph bounding boxes.
     */
    public short getYMax() {
        return _yMax;
    }

    /**
     * uint16
     * 
     * Bit 0: Bold (if set to 1);
     * 
     * Bit 1: Italic (if set to 1)
     * 
     * Bit 2: Underline (if set to 1)
     * 
     * Bit 3: Outline (if set to 1)
     * 
     * Bit 4: Shadow (if set to 1)
     * 
     * Bit 5: Condensed (if set to 1)
     * 
     * Bit 6: Extended (if set to 1)
     * 
     * Bits 7–15: Reserved (set to 0).
     * 
     * Note that the macStyle bits must agree with the OS/2 table fsSelection
     * bits. The fsSelection bits are used over the macStyle bits in Microsoft
     * Windows. The PANOSE values and 'post' table values are ignored for
     * determining bold or italic fonts.
     */
    public short getMacStyle() {
        return _macStyle;
    }

    /**
     * uint16     
     * 
     * Smallest readable size in pixels.
     */
    public short getLowestRecPPEM() {
        return _lowestRecPPEM;
    }

    /**
     * int16
     * 
     * Deprecated (Set to 2).
     * 
     * 0: Fully mixed directional glyphs;
     * 
     * 1: Only strongly left to right;
     * 
     * 2: Like 1 but also contains neutrals;
     * 
     * -1: Only strongly right to left;
     * 
     * -2: Like -1 but also contains neutrals.
     * 
     * (A neutral character has no inherent directionality; it is not a
     * character with zero (0) width. Spaces and punctuation are examples of
     * neutral characters. Non-neutral characters are those with inherent
     * directionality. For example, Roman letters (left-to-right) and Arabic
     * letters (right-to-left) have directionality. In a “normal” Roman font
     * where spaces and punctuation are present, the font direction hints should
     * be set to two (2).)
     */
    public short getFontDirectionHint() {
        return _fontDirectionHint;
    }

    /**
     * int16
     * 
     * 0 for short offsets (Offset16), 1 for long (Offset32).
     */
    public short getIndexToLocFormat() {
        return _indexToLocFormat;
    }

    /**
     * Whether short offsets (Offset16) are used.
     */
    public boolean useShortEntries() {
        return getIndexToLocFormat() == 0;
    }

    /**
     * int16
     * 
     * {@link #GLYPH_DATA_FORMAT} for current format.
     */
    public short getGlyphDataFormat() {
        return _glyphDataFormat;
    }

    @Override
    public String toString() {
        return "'head' Table - Font Header\n--------------------------" +
                "\n  'head' version:      " + Fixed.floatValue(_versionNumber) +
                "\n  fontRevision:        " + Fixed.roundedFloatValue(_fontRevision, 8) +
                "\n  checkSumAdjustment:  0x" + Integer.toHexString(_checkSumAdjustment).toUpperCase() +
                "\n  magicNumber:         0x" + Integer.toHexString(_magicNumber).toUpperCase() +
                "\n  flags:               0x" + Integer.toHexString(_flags).toUpperCase() +
                "\n  unitsPerEm:          " + _unitsPerEm +
                "\n  created:             " + LongDateTime.toDate(_created) +
                "\n  modified:            " + LongDateTime.toDate(_modified) +
                "\n  xMin:                " + _xMin +
                "\n  yMin:                " + _yMin +
                "\n  xMax:                " + _xMax +
                "\n  yMax:                " + _yMax +
                "\n  macStyle bits:       " + Integer.toHexString(_macStyle).toUpperCase() +
                "\n  lowestRecPPEM:       " + _lowestRecPPEM +
                "\n  fontDirectionHint:   " + _fontDirectionHint +
                "\n  indexToLocFormat:    " + _indexToLocFormat +
                "\n  glyphDataFormat:     " + _glyphDataFormat;
    }

}
