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
import java.util.ArrayList;

/**
 *
 * @author dschweinsberg
 */
public class CharsetFormat1 extends Charset {

    private final ArrayList<CharsetRange> _charsetRanges = new ArrayList<>();

    public CharsetFormat1(DataInput di, int glyphCount) throws IOException {
        int glyphsCovered = glyphCount - 1;  // minus 1 because .notdef is omitted
        while (glyphsCovered > 0) {
            CharsetRange range = new CharsetRange1(di);
            _charsetRanges.add(range);
            glyphsCovered -= range.getLeft() + 1;
        }
    }

    @Override
    public int getFormat() {
        return 1;
    }

    @Override
    public int getSID(int gid) {
        if (gid == 0) {
            return 0;
        }

        // Count through the ranges to find the one of interest
        int count = 1;
        for (CharsetRange range : _charsetRanges) {
            if (gid <= range.getLeft() + count) {
                return gid - count + range.getFirst();
            }
            count += range.getLeft() + 1;
        }
        return 0;
    }
}
