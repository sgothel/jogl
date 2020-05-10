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
 * Format 4: Segment mapping to delta values
 * 
 * <p>
 * This is the standard character-to-glyph-index mapping table for the Windows
 * platform for fonts that support Unicode BMP characters.
 * </p>
 * 
 * <p>
 * This format is used when the character codes for the characters represented
 * by a font fall into several contiguous ranges, possibly with holes in some or
 * all of the ranges (that is, some of the codes in a range may not have a
 * representation in the font). The format-dependent data is divided into three
 * parts, which must occur in the following order:
 * </p>
 * <ol>
 * <li>A four-word header gives parameters for an optimized search of the
 * segment list;
 * <li>Four parallel arrays describe the segments (one segment for each
 * contiguous range of codes);
 * <li>A variable-length array of glyph IDs (unsigned words).
 * </ol>
 * 
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#format-4-segment-mapping-to-delta-values"
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapFormat4 extends CmapFormat {

    /**
     * uint16   length  This is the length in bytes of the subtable.
     */
    private final int _length;
    
    /**
     * uint16   language
     */
    private final int _language;
    
    /**
     * uint16   segCountX2  2 × segCount.
     */
    private final int _segCountX2;
    
    /**
     * uint16   searchRange     2 × (2**floor(log2(segCount)))
     */
    private final int _searchRange;
    
    /**
     * uint16   entrySelector   log2(searchRange/2)
     */
    private final int _entrySelector;
    
    /**
     * uint16   rangeShift  2 × segCount - searchRange
     */
    private final int _rangeShift;
    
    /**
     * uint16   endCode[segCount]   End characterCode for each segment, last=0xFFFF.
     */
    private final int[] _endCode;
    
    /**
     * uint16   startCode[segCount]     Start character code for each segment.
     */
    private final int[] _startCode;
    
    /**
     * int16    idDelta[segCount]   Delta for all character codes in segment.
     */
    private final int[] _idDelta;
    
    /**
     * uint16   idRangeOffset[segCount]     Offsets into glyphIdArray or 0
     */
    private final int[] _idRangeOffset;
    
    /**
     * uint16   glyphIdArray[ ]     Glyph index array (arbitrary length)
     */
    private final int[] _glyphIdArray;
    
    /**
     * @see #_segCountX2
     */
    private final int _segCount;

    CmapFormat4(DataInput di) throws IOException {
        _length = di.readUnsignedShort();
        _language = di.readUnsignedShort();
        _segCountX2 = di.readUnsignedShort(); // +2 (8)
        _segCount = _segCountX2 / 2;
        _endCode = new int[_segCount];
        _startCode = new int[_segCount];
        _idDelta = new int[_segCount];
        _idRangeOffset = new int[_segCount];
        _searchRange = di.readUnsignedShort(); // +2 (10)
        _entrySelector = di.readUnsignedShort(); // +2 (12)
        _rangeShift = di.readUnsignedShort(); // +2 (14)
        for (int i = 0; i < _segCount; i++) {
            _endCode[i] = di.readUnsignedShort();
        } // + 2*segCount (2*segCount + 14)
        di.readUnsignedShort(); // reservePad  +2 (2*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _startCode[i] = di.readUnsignedShort();
        } // + 2*segCount (4*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _idDelta[i] = di.readUnsignedShort();
        } // + 2*segCount (6*segCount + 16)
        for (int i = 0; i < _segCount; i++) {
            _idRangeOffset[i] = di.readUnsignedShort();
        } // + 2*segCount (8*segCount + 16)

        // Whatever remains of this header belongs in glyphIdArray
        int count = (_length - (8*_segCount + 16)) / 2;
        _glyphIdArray = new int[count];
        for (int i = 0; i < count; i++) {
            _glyphIdArray[i] = di.readUnsignedShort();
        } // + 2*count (8*segCount + 2*count + 18)
        
        // Are there any padding bytes we need to consume?
//        int leftover = length - (8*segCount + 2*count + 18);
//        if (leftover > 0) {
//            di.skipBytes(leftover);
//        }
    }

    @Override
    public int getFormat() {
        return 4;
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
        return _segCount;
    }
    
    @Override
    public Range getRange(int index) throws ArrayIndexOutOfBoundsException {
        if (index < 0 || index >= _segCount) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new Range(_startCode[index], _endCode[index]);
    }

    @Override
    public int mapCharCode(int charCode) {
        try {
            for (int i = 0; i < _segCount; i++) {
                if (_endCode[i] >= charCode) {
                    if (_startCode[i] <= charCode) {
                        if (_idRangeOffset[i] > 0) {
                            return _glyphIdArray[_idRangeOffset[i]/2 + (charCode - _startCode[i]) - (_segCount - i)];
                        } else {
                            return (_idDelta[i] + charCode) % 65536;
                        }
                    } else {
                        break;
                    }
                }
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            System.err.println("error: Array out of bounds - " + e.getMessage());
        }
        return 0;
    }

    @Override
    public String toString() {
        return super.toString() +
                ", segCountX2: " +
                _segCountX2 +
                ", searchRange: " +
                _searchRange +
                ", entrySelector: " +
                _entrySelector +
                ", rangeShift: " +
                _rangeShift +
                ", endCode: " +
                Arrays.toString(_endCode) +
                ", startCode: " +
                Arrays.toString(_endCode) +
                ", idDelta: " +
                Arrays.toString(_idDelta) +
                ", idRangeOffset: " +
                Arrays.toString(_idRangeOffset);
    }
}
