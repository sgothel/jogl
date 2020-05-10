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
 * Format 6: Trimmed table mapping
 * 
 * @see "https://docs.microsoft.com/en-us/typography/opentype/spec/cmap#format-6-trimmed-table-mapping"
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapFormat6 extends CmapFormat {

    private final int _length;
    private final int _language;
    private final int _firstCode;
    private final int _entryCount;
    private final int[] _glyphIdArray;

    CmapFormat6(DataInput di) throws IOException {
        _length = di.readUnsignedShort();
        _language = di.readUnsignedShort();
        _firstCode = di.readUnsignedShort();
        _entryCount = di.readUnsignedShort();
        _glyphIdArray = new int[_entryCount];
        for (int i = 0; i < _entryCount; i++) {
            _glyphIdArray[i] = di.readUnsignedShort();
        }
    }

    @Override
    public int getFormat() {
        return 6;
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
        return 1;
    }
    
    @Override
    public Range getRange(int index) throws ArrayIndexOutOfBoundsException {
        if (index != 0) {
            throw new ArrayIndexOutOfBoundsException();
        }
        return new Range(_firstCode, _entryCount);
    }

    @Override
    public int mapCharCode(int charCode) {
        if (_firstCode <= charCode && charCode < _firstCode + _entryCount) {
            return _glyphIdArray[charCode - _firstCode];
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return super.toString() +
            "    format:         " + getFormat() + "\n" +
            "    language:       " + getLanguage() + "\n" +
            "    firstCode:      " + _firstCode + "\n" +
            "    entryCount:     " + _entryCount + "\n" +
            "    glyphIdArray:   " + Arrays.toString(_glyphIdArray) + "\n";
    }
}
