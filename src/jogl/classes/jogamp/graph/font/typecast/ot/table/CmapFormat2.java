/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;
import java.util.Arrays;

/**
 * Format 2: High-byte mapping through table.
 *
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#format-2-high-byte-mapping-through-table"
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapFormat2 extends CmapFormat {

    private static class SubHeader {
        /**
         * uint16
         *
         * First valid low byte for this SubHeader.
         *
         * @see #_entryCount
         */
        int _firstCode;

        /**
         * uint16
         *
         * Number of valid low bytes for this SubHeader.
         *
         * <p>
         * The {@link #_firstCode} and {@link #_entryCount} values specify a
         * subrange that begins at {@link #_firstCode} and has a length equal to
         * the value of {@link #_entryCount}. This subrange stays within the
         * 0-255 range of the byte being mapped. Bytes outside of this subrange
         * are mapped to glyph index 0 (missing glyph). The offset of the byte
         * within this subrange is then used as index into a corresponding
         * subarray of {@link #_glyphIndexArray}. This subarray is also of
         * length {@link #_entryCount}. The value of the {@link #_idRangeOffset}
         * is the number of bytes past the actual location of the
         * {@link #_idRangeOffset} word where the {@link #_glyphIndexArray}
         * element corresponding to {@link #_firstCode} appears.
         * </p>
         * <p>
         * Finally, if the value obtained from the subarray is not 0 (which
         * indicates the missing glyph), you should add {@link #_idDelta} to it
         * in order to get the glyphIndex. The value {@link #_idDelta} permits
         * the same subarray to be used for several different subheaders. The
         * {@link #_idDelta} arithmetic is modulo 65536.
         * </p>
         */
        int _entryCount;

        /**
         * @see #_entryCount
         */
        short _idDelta;

        /**
         * @see #_entryCount
         */
        int _idRangeOffset;

        int _arrayIndex;
    }

    /**
     * uint16
     *
     * @see #getLength()
     */
    private final int _length;

    /**
     * uint16
     *
     * @see #getLanguage()
     */
    private final int _language;

    /**
     * uint16[256]
     *
     * Array that maps high bytes to subHeaders: value is subHeader index × 8.
     */
    private final int[] _subHeaderKeys = new int[256];

    /**
     * Variable-length array of SubHeader records.
     */
    private final SubHeader[] _subHeaders;

    /**
     * uint16
     *
     * Variable-length array containing subarrays used for mapping the low byte
     * of 2-byte characters.
     */
    private final int[] _glyphIndexArray;

    CmapFormat2(final DataInput di) throws IOException {
        _length = di.readUnsignedShort();
        _language = di.readUnsignedShort();

        int pos = 6;

        // Read the subheader keys, noting the highest value, as this will
        // determine the number of subheaders to read.
        int highest = 0;
        for (int i = 0; i < 256; ++i) {
            _subHeaderKeys[i] = di.readUnsignedShort();
            highest = Math.max(highest, _subHeaderKeys[i]);
            pos += 2;
        }
        final int subHeaderCount = highest / 8 + 1;
        _subHeaders = new SubHeader[subHeaderCount];

        // Read the subheaders, once again noting the highest glyphIndexArray
        // index range.
        final int indexArrayOffset = 8 * subHeaderCount + 518;
        highest = 0;
        for (int i = 0; i < _subHeaders.length; ++i) {
            final SubHeader sh = new SubHeader();
            sh._firstCode = di.readUnsignedShort();
            sh._entryCount = di.readUnsignedShort();
            sh._idDelta = di.readShort();
            sh._idRangeOffset = di.readUnsignedShort();

            // Calculate the offset into the _glyphIndexArray
            pos += 8;
            sh._arrayIndex =
                    (pos - 2 + sh._idRangeOffset - indexArrayOffset) / 2;

            // What is the highest range within the glyphIndexArray?
            highest = Math.max(highest, sh._arrayIndex + sh._entryCount);

            _subHeaders[i] = sh;
        }

        // Read the glyphIndexArray
        _glyphIndexArray = new int[highest];
        for (int i = 0; i < _glyphIndexArray.length; ++i) {
            _glyphIndexArray[i] = di.readUnsignedShort();
        }
    }

    @Override
    public int getFormat() {
        return 2;
    }

    @Override
    public int getLength() {
        return _length;
    }

    @Override
    public int getLanguage() {
        return _language;
    }

    @Override
    public int getRangeCount() {
        return _subHeaders.length;
    }

    @Override
    public Range getRange(final int index) throws ArrayIndexOutOfBoundsException {
        if (index < 0 || index >= _subHeaders.length) {
            throw new ArrayIndexOutOfBoundsException();
        }

        // Find the high-byte (if any)
        int highByte = 0;
        if (index != 0) {
            for (int i = 0; i < 256; ++i) {
                if (_subHeaderKeys[i] / 8 == index) {
                    highByte = i << 8;
                    break;
                }
            }
        }

        return new Range(
                highByte | _subHeaders[index]._firstCode,
                highByte | (_subHeaders[index]._firstCode +
                        _subHeaders[index]._entryCount - 1));
    }

    @Override
    public int mapCharCode(final int charCode) {

        // Get the appropriate subheader
        int index = 0;
        final int highByte = charCode >> 8;
        if (highByte != 0) {
            index = _subHeaderKeys[highByte] / 8;
        }
        final SubHeader sh = _subHeaders[index];

        // Is the charCode out-of-range?
        final int lowByte = charCode & 0xff;
        if (lowByte < sh._firstCode ||
                lowByte >= (sh._firstCode + sh._entryCount)) {
            return 0;
        }

        // Now calculate the glyph index
        int glyphIndex =
                _glyphIndexArray[sh._arrayIndex + (lowByte - sh._firstCode)];
        if (glyphIndex != 0) {
            glyphIndex += sh._idDelta;
            glyphIndex %= 65536;
        }
        return glyphIndex;
    }

    @Override
    public String toString() {
        return super.toString() +
            "    format:         " + getFormat() + "\n" +
            "    language:       " + getLanguage() + "\n" +
            "    subHeaderKeys:  " + Arrays.toString(_subHeaderKeys) + "\n" +
            "    glyphIndexArray:" + Arrays.toString(_glyphIndexArray) + "\n";
    }
}
