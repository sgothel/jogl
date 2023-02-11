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
public class Index {
    
    private final int _count;
    private final int _offSize;
    private final int[] _offset;
    private final int[] _data;

    public Index(DataInput di) throws IOException {
        _count = di.readUnsignedShort();
        _offset = new int[_count + 1];
        _offSize = di.readUnsignedByte();
        for (int i = 0; i < _count + 1; ++i) {
            int thisOffset = 0;
            for (int j = 0; j < _offSize; ++j) {
                thisOffset |= di.readUnsignedByte() << ((_offSize - j - 1) * 8);
            }
            _offset[i] = thisOffset;
        }
        _data = new int[getDataLength()];
        for (int i = 0; i < getDataLength(); ++i) {
            _data[i] = di.readUnsignedByte();
        }
    }

    public final int getCount() {
        return _count;
    }

    public final int getOffset(int index) {
        return _offset[index];
    }

    public final int getDataLength() {
        return _offset[_offset.length - 1] - 1;
    }

    public final int[] getData() {
        return _data;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("DICT\n");
        sb.append("count: ").append(_count).append("\n");
        sb.append("offSize: ").append(_offSize).append("\n");
        for (int i = 0; i < _count + 1; ++i) {
            sb.append("offset[").append(i).append("]: ").append(_offset[i]).append("\n");
        }
        sb.append("data:");
        for (int i = 0; i < _data.length; ++i) {
            if (i % 8 == 0) {
                sb.append("\n");
            } else {
                sb.append(" ");
            }
            sb.append(_data[i]);
        }
        sb.append("\n");
        return sb.toString();
    }
    
}
