/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2015 David Schweinsberg
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

package jogamp.graph.font.typecast.cff;

/**
 * CFF Type 2 Charstring
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CharstringType2 extends Charstring {
    
    private static final String[] _oneByteOperators = {
        "-Reserved-",
        "hstem",
        "-Reserved-",
        "vstem",
        "vmoveto",
        "rlineto",
        "hlineto",
        "vlineto",
        "rrcurveto",
        "-Reserved-",
        "callsubr",
        "return",
        "escape",
        "-Reserved-",
        "endchar",
        "-Reserved-",
        "-Reserved-",
        "-Reserved-",
        "hstemhm",
        "hintmask",
        "cntrmask",
        "rmoveto",
        "hmoveto",
        "vstemhm",
        "rcurveline",
        "rlinecurve",
        "vvcurveto",
        "hhcurveto",
        "shortint",
        "callgsubr",
        "vhcurveto",
        "hvcurveto"
    };

    private static final String[] _twoByteOperators = {
        "-Reserved- (dotsection)",
        "-Reserved-",
        "-Reserved-",
        "and",
        "or",
        "not",
        "-Reserved-",
        "-Reserved-",
        "-Reserved-",
        "abs",
        "add",
        "sub",
        "div",
        "-Reserved-",
        "neg",
        "eq",
        "-Reserved-",
        "-Reserved-",
        "drop",
        "-Reserved-",
        "put",
        "get",
        "ifelse",
        "random",
        "mul",
        "-Reserved-",
        "sqrt",
        "dup",
        "exch",
        "index",
        "roll",
        "-Reserved-",
        "-Reserved-",
        "-Reserved-",
        "hflex",
        "flex",
        "hflex1",
        "flex1",
        "-Reserved-"
    };
    
    private final CffFont _font;
    private final int _index;
    private final String _name;
    private final int[] _data;
    private final int _offset;
    private final int _length;

    /** Creates a new instance of CharstringType2
     * @param font
     * @param index
     * @param name
     * @param data
     * @param offset
     * @param length */
    public CharstringType2(
            CffFont font,
            int index,
            String name,
            int[] data,
            int offset,
            int length) {
        _font = font;
        _index = index;
        _name = name;
        _data = data;
        _offset = offset;
        _length = length;
    }
    
    public CffFont getFont() {
        return _font;
    }
    
    @Override
    public int getIndex() {
        return _index;
    }

    @Override
    public String getName() {
        return _name;
    }
    
    private int disassemble(int ip, StringBuilder sb) {
        while (isOperandAtIndex(ip)) {
            Number operand = operandAtIndex(ip);
            sb.append(operand).append(" ");
            ip = nextOperandIndex(ip);
        }
        int operator = byteAtIndex(ip++);
        String mnemonic;
        if (operator == 12) {
            operator = byteAtIndex(ip++);
            
            // Check we're not exceeding the upper limit of our mnemonics
            if (operator > 38) {
                operator = 38;
            }
            mnemonic = _twoByteOperators[operator];
        } else {
            mnemonic = _oneByteOperators[operator];
        }
        sb.append(mnemonic);
        return ip;
    }
    
    public int getFirstIndex() {
        return _offset;
    }

    public boolean isOperandAtIndex(int ip) {
        int b0 = _data[ip];
        return (32 <= b0 && b0 <= 255) || b0 == 28;
    }

    public Number operandAtIndex(int ip) {
        int b0 = _data[ip];
        if (32 <= b0 && b0 <= 246) {

            // 1 byte integer
            return b0 - 139;
        } else if (247 <= b0 && b0 <= 250) {

            // 2 byte integer
            int b1 = _data[ip + 1];
            return (b0 - 247) * 256 + b1 + 108;
        } else if (251 <= b0 && b0 <= 254) {

            // 2 byte integer
            int b1 = _data[ip + 1];
            return -(b0 - 251) * 256 - b1 - 108;
        } else if (b0 == 28) {

            // 3 byte integer
            int b1 = (byte)_data[ip + 1];
            int b2 = _data[ip + 2];
            return b1 << 8 | b2;
        } else if (b0 == 255) {

            // 16-bit signed integer with 16 bits of fraction
            int b1 = (byte) _data[ip + 1];
            int b2 = _data[ip + 2];
            int b3 = _data[ip + 3];
            int b4 = _data[ip + 4];
            return (float) ((b1 << 8 | b2) + ((b3 << 8 | b4) / 65536.0));
        } else {
            return null;
        }
    }

    public int nextOperandIndex(int ip) {
        int b0 = _data[ip];
        if (32 <= b0 && b0 <= 246) {

            // 1 byte integer
            return ip + 1;
        } else if (247 <= b0 && b0 <= 250) {

            // 2 byte integer
            return ip + 2;
        } else if (251 <= b0 && b0 <= 254) {

            // 2 byte integer
            return ip + 2;
        } else if (b0 == 28) {

            // 3 byte integer
            return ip + 3;
        } else if (b0 == 255) {

            return ip + 5;
        } else {
            return ip;
        }
    }
    
    public int byteAtIndex(int ip) {
        return _data[ip];
    }
    
    public boolean moreBytes(int ip) {
        return ip < _offset + _length;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int ip = getFirstIndex();
        while (moreBytes(ip)) {
//            sb.append(ip);
//            sb.append(": ");
            ip = disassemble(ip, sb);
            sb.append("\n");
        }
        return sb.toString();
    }
}
