/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2015 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jogamp.graph.font.typecast.cff;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author dschweinsberg
 */
public class CharsetFormat0 extends Charset {
    
    private final int[] _glyph;

    public CharsetFormat0(DataInput di, int glyphCount) throws IOException {
        _glyph = new int[glyphCount - 1]; // minus 1 because .notdef is omitted
        for (int i = 0; i < glyphCount - 1; ++i) {
            _glyph[i] = di.readUnsignedShort();
        }
    } // minus 1 because .notdef is omitted

    @Override
    public int getFormat() {
        return 0;
    }

    @Override
    public int getSID(int gid) {
        if (gid == 0) {
            return 0;
        }
        return _glyph[gid - 1];
    }
    
}
