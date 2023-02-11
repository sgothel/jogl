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
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author dschweinsberg
 */
public class Dict {
    
    private final Map<Integer, Object> _entries = new HashMap<>();
    private final int[] _data;
    private int _index;

    public Dict(int[] data, int offset, int length) {
        _data = data;
        _index = offset;
        while (_index < offset + length) {
            addKeyAndValueEntry();
        }
    }

    public Dict(DataInput di, int length) throws IOException {
        _data = new int[length];
        for (int i = 0; i < length; ++i) {
            _data[i] = di.readUnsignedByte();
        }
        _index = 0;
        while (_index < length) {
            addKeyAndValueEntry();
        }
    }

    public Object getValue(int key) {
        return _entries.get(key);
    }

    private void addKeyAndValueEntry() {
        ArrayList<Object> operands = new ArrayList<>();
        Object operand = null;
        while (isOperandAtIndex()) {
            operand = nextOperand();
            operands.add(operand);
        }
        int operator = _data[_index++];
        if (operator == 12) {
            operator <<= 8;
            operator |= _data[_index++];
        }
        if (operands.size() == 1) {
            _entries.put(operator, operand);
        } else {
            _entries.put(operator, operands);
        }
    }

    private boolean isOperandAtIndex() {
        int b0 = _data[_index];
        return (32 <= b0 && b0 <= 254) || b0 == 28 || b0 == 29 || b0 == 30;
    }

    //        private boolean isOperatorAtIndex() {
    //            int b0 = _data[_index];
    //            return 0 <= b0 && b0 <= 21;
    //        }
    private Object nextOperand() {
        int b0 = _data[_index];
        if (32 <= b0 && b0 <= 246) {
            // 1 byte integer
            ++_index;
            return b0 - 139;
        } else if (247 <= b0 && b0 <= 250) {
            // 2 byte integer
            int b1 = _data[_index + 1];
            _index += 2;
            return (b0 - 247) * 256 + b1 + 108;
        } else if (251 <= b0 && b0 <= 254) {
            // 2 byte integer
            int b1 = _data[_index + 1];
            _index += 2;
            return -(b0 - 251) * 256 - b1 - 108;
        } else if (b0 == 28) {
            // 3 byte integer
            int b1 = _data[_index + 1];
            int b2 = _data[_index + 2];
            _index += 3;
            return b1 << 8 | b2;
        } else if (b0 == 29) {
            // 5 byte integer
            int b1 = _data[_index + 1];
            int b2 = _data[_index + 2];
            int b3 = _data[_index + 3];
            int b4 = _data[_index + 4];
            _index += 5;
            return b1 << 24 | b2 << 16 | b3 << 8 | b4;
        } else if (b0 == 30) {
            // Real number
            StringBuilder fString = new StringBuilder();
            int nibble1 = 0;
            int nibble2 = 0;
            ++_index;
            while ((nibble1 != 0xf) && (nibble2 != 0xf)) {
                nibble1 = _data[_index] >> 4;
                nibble2 = _data[_index] & 0xf;
                ++_index;
                fString.append(decodeRealNibble(nibble1));
                fString.append(decodeRealNibble(nibble2));
            }
            return Float.valueOf(fString.toString());
        } else {
            return null;
        }
    }

    private String decodeRealNibble(int nibble) {
        if (nibble < 0xa) {
            return Integer.toString(nibble);
        } else if (nibble == 0xa) {
            return ".";
        } else if (nibble == 0xb) {
            return "E";
        } else if (nibble == 0xc) {
            return "E-";
        } else if (nibble == 0xe) {
            return "-";
        }
        return "";
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Integer key : _entries.keySet()) {
            if ((key & 0xc00) == 0xc00) {
                sb.append("12 ").append(key & 0xff).append(": ");
            } else {
                sb.append(key.toString()).append(": ");
            }
            sb.append(_entries.get(key).toString()).append("\n");
        }
        return sb.toString();
    }
    
}
