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

/**
 * High-byte mapping through table cmap format.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapFormat2 extends CmapFormat {

    private static class SubHeader {
        int _firstCode;
        int _entryCount;
        short _idDelta;
        int _idRangeOffset;
        int _arrayIndex;
    }
    
    private final int _length;
    private final int _language;
    private final int[] _subHeaderKeys = new int[256];
    private final SubHeader[] _subHeaders;
    private final int[] _glyphIndexArray;

    CmapFormat2(DataInput di) throws IOException {
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
        int subHeaderCount = highest / 8 + 1;
        _subHeaders = new SubHeader[subHeaderCount];
        
        // Read the subheaders, once again noting the highest glyphIndexArray
        // index range.
        int indexArrayOffset = 8 * subHeaderCount + 518;
        highest = 0;
        for (int i = 0; i < _subHeaders.length; ++i) {
            SubHeader sh = new SubHeader();
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
    public Range getRange(int index) throws ArrayIndexOutOfBoundsException {
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
    public int mapCharCode(int charCode) {
        
        // Get the appropriate subheader
        int index = 0;
        int highByte = charCode >> 8;
        if (highByte != 0) {
            index = _subHeaderKeys[highByte] / 8;
        }
        SubHeader sh = _subHeaders[index];
        
        // Is the charCode out-of-range?
        int lowByte = charCode & 0xff;
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
}
