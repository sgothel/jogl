/*
 * $Id: CffTable.java,v 1.4 2007-07-26 11:15:06 davidsch Exp $
 *
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2007 David Schweinsberg
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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Compact Font Format Table
 * @version $Id: CffTable.java,v 1.4 2007-07-26 11:15:06 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CffTable implements Table {
    
    public class Dict {
        
        private Dictionary<Integer, Object> _entries = new Hashtable<Integer, Object>();
        private int[] _data;
        private int _index;
        
        protected Dict(int[] data, int offset, int length) {
            _data = data;
            _index = offset;
            while (_index < offset + length) {
                addKeyAndValueEntry();
            }
        }
        
        public Object getValue(int key) {
            return _entries.get(key);
        }
        
        private boolean addKeyAndValueEntry() {
            ArrayList<Object> operands = new ArrayList<Object>();
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
            return true;
        }
        
        private boolean isOperandAtIndex() {
            int b0 = _data[_index];
            if ((32 <= b0 && b0 <= 254)
                    || b0 == 28
                    || b0 == 29
                    || b0 == 30) {
                return true;
            }
            return false;
        }

        private boolean isOperatorAtIndex() {
            int b0 = _data[_index];
            if (0 <= b0 && b0 <= 21) {
                return true;
            }
            return false;
        }

        private Object nextOperand() {
            int b0 = _data[_index];
            if (32 <= b0 && b0 <= 246) {
                
                // 1 byte integer
                ++_index;
                return new Integer(b0 - 139);
            } else if (247 <= b0 && b0 <= 250) {
                
                // 2 byte integer
                int b1 = _data[_index + 1];
                _index += 2;
                return new Integer((b0 - 247) * 256 + b1 + 108);
            } else if (251 <= b0 && b0 <= 254) {
                
                // 2 byte integer
                int b1 = _data[_index + 1];
                _index += 2;
                return new Integer(-(b0 - 251) * 256 - b1 - 108);
            } else if (b0 == 28) {
                
                // 3 byte integer
                int b1 = _data[_index + 1];
                int b2 = _data[_index + 2];
                _index += 3;
                return new Integer(b1 << 8 | b2);
            } else if (b0 == 29) {
                
                // 5 byte integer
                int b1 = _data[_index + 1];
                int b2 = _data[_index + 2];
                int b3 = _data[_index + 3];
                int b4 = _data[_index + 4];
                _index += 5;
                return new Integer(b1 << 24 | b2 << 16 | b3 << 8 | b4);
            } else if (b0 == 30) {
                
                // Real number
                StringBuffer fString = new StringBuffer();
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
                return new Float(fString.toString());
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
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            Enumeration<Integer> keys = _entries.keys();
            while (keys.hasMoreElements()) {
                Integer key = keys.nextElement();
                if ((key.intValue() & 0xc00) == 0xc00) {
                    sb.append("12 ").append(key.intValue() & 0xff).append(": ");
                } else {
                    sb.append(key.toString()).append(": ");
                }
                sb.append(_entries.get(key).toString()).append("\n");
            }
            return sb.toString();
        }
    }
    
    public class Index {
        
        private int _count;
        private int _offSize;
        private int[] _offset;
        private int[] _data;
        
        protected Index(DataInput di) throws IOException {
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
        
        public int getCount() {
            return _count;
        }
        
        public int getOffset(int index) {
            return _offset[index];
        }
        
        public int getDataLength() {
            return _offset[_offset.length - 1] - 1;
        }
        
        public int[] getData() {
            return _data;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
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
    
    public class TopDictIndex extends Index {

        protected TopDictIndex(DataInput di) throws IOException {
            super(di);
        }
        
        public Dict getTopDict(int index) {
            int offset = getOffset(index) - 1;
            int len = getOffset(index + 1) - offset - 1;
            return new Dict(getData(), offset, len);
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < getCount(); ++i) {
                sb.append(getTopDict(i).toString()).append("\n");
            }
            return sb.toString();
        }
    }
    
    public class NameIndex extends Index {

        protected NameIndex(DataInput di) throws IOException {
            super(di);
        }
        
        public String getName(int index) {
            String name = null;
            int offset = getOffset(index) - 1;
            int len = getOffset(index + 1) - offset - 1;

            // Ensure the name hasn't been deleted
            if (getData()[offset] != 0) {
                StringBuffer sb = new StringBuffer();
                for (int i = offset; i < offset + len; ++i) {
                    sb.append((char) getData()[i]);
                }
                name = sb.toString();
            } else {
                name = "DELETED NAME";
            }
            return name;
        }
        
        public String toString() {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < getCount(); ++i) {
                sb.append(getName(i)).append("\n");
            }
            return sb.toString();
        }
    }

    public class StringIndex extends Index {

        protected StringIndex(DataInput di) throws IOException {
            super(di);
        }
        
        public String getString(int index) {
            if (index < CffStandardStrings.standardStrings.length) {
                return CffStandardStrings.standardStrings[index];
            } else {
                index -= CffStandardStrings.standardStrings.length;
                if (index >= getCount()) {
                    return null;
                }
                int offset = getOffset(index) - 1;
                int len = getOffset(index + 1) - offset - 1;

                StringBuffer sb = new StringBuffer();
                for (int i = offset; i < offset + len; ++i) {
                    sb.append((char) getData()[i]);
                }
                return sb.toString();
            }
        }
        
        public String toString() {
            int nonStandardBase = CffStandardStrings.standardStrings.length;
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < getCount(); ++i) {
                sb.append(nonStandardBase + i).append(": ");
                sb.append(getString(nonStandardBase + i)).append("\n");
            }
            return sb.toString();
        }
    }
    
    private class CharsetRange {
        
        private int _first;
        private int _left;
        
        public int getFirst() {
            return _first;
        }

        protected void setFirst(int first) {
            _first = first;
        }
        
        public int getLeft() {
            return _left;
        }

        protected void setLeft(int left) {
            _left = left;
        }
    }

    private class CharsetRange1 extends CharsetRange {
        
        protected CharsetRange1(DataInput di) throws IOException {
            setFirst(di.readUnsignedShort());
            setLeft(di.readUnsignedByte());
        }
    }
    
    private class CharsetRange2 extends CharsetRange {
        
        protected CharsetRange2(DataInput di) throws IOException {
            setFirst(di.readUnsignedShort());
            setLeft(di.readUnsignedShort());
        }
    }
    
    private abstract class Charset {
        
        public abstract int getFormat();
        
        public abstract int getSID(int gid);
    }
    
    private class CharsetFormat0 extends Charset {
        
        private int[] _glyph;
        
        protected CharsetFormat0(DataInput di, int glyphCount) throws IOException {
            _glyph = new int[glyphCount - 1];  // minus 1 because .notdef is omitted
            for (int i = 0; i < glyphCount - 1; ++i) {
                _glyph[i] = di.readUnsignedShort();
            }
        }
        
        public int getFormat() {
            return 0;
        }

        public int getSID(int gid) {
            if (gid == 0) {
                return 0;
            }
            return _glyph[gid - 1];
        }
    }
    
    private class CharsetFormat1 extends Charset {
        
        private ArrayList<CharsetRange> _charsetRanges = new ArrayList<CharsetRange>();
        
        protected CharsetFormat1(DataInput di, int glyphCount) throws IOException {
            int glyphsCovered = glyphCount - 1;  // minus 1 because .notdef is omitted
            while (glyphsCovered > 0) {
                CharsetRange range = new CharsetRange1(di);
                _charsetRanges.add(range);
                glyphsCovered -= range.getLeft() + 1;
            }
        }

        public int getFormat() {
            return 1;
        }

        public int getSID(int gid) {
            if (gid == 0) {
                return 0;
            }
            
            // Count through the ranges to find the one of interest
            int count = 0;
            for (CharsetRange range : _charsetRanges) {
                count += range.getLeft();
                if (gid < count) {
                    int sid = gid - count + range.getFirst();
                    return sid;
                }
            }
            return 0;
        }
    }

    private class CharsetFormat2 extends Charset {
        
        private ArrayList<CharsetRange> _charsetRanges = new ArrayList<CharsetRange>();
        
        protected CharsetFormat2(DataInput di, int glyphCount) throws IOException {
            int glyphsCovered = glyphCount - 1;  // minus 1 because .notdef is omitted
            while (glyphsCovered > 0) {
                CharsetRange range = new CharsetRange2(di);
                _charsetRanges.add(range);
                glyphsCovered -= range.getLeft() + 1;
            }
        }

        public int getFormat() {
            return 2;
        }

        public int getSID(int gid) {
            if (gid == 0) {
                return 0;
            }
            
            // Count through the ranges to find the one of interest
            int count = 0;
            for (CharsetRange range : _charsetRanges) {
                if (gid < range.getLeft() + count) {
                    int sid = gid - count + range.getFirst() - 1;
                    return sid;
                }
                count += range.getLeft();
            }
            return 0;
        }
    }
    
    private DirectoryEntry _de;
    private int _major;
    private int _minor;
    private int _hdrSize;
    private int _offSize;
    private NameIndex _nameIndex;
    private TopDictIndex _topDictIndex;
    private StringIndex _stringIndex;
    private Index _globalSubrIndex;
    private Index _charStringsIndexArray[];
    private Charset[] _charsets;
    private Charstring[][] _charstringsArray;

    private byte[] _buf;

    /** Creates a new instance of CffTable */
    protected CffTable(DirectoryEntry de, DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();

        // Load entire table into a buffer, and create another input stream
        _buf = new byte[de.getLength()];
        di.readFully(_buf);
        DataInput di2 = getDataInputForOffset(0);

        // Header
        _major = di2.readUnsignedByte();
        _minor = di2.readUnsignedByte();
        _hdrSize = di2.readUnsignedByte();
        _offSize = di2.readUnsignedByte();
        
        // Name INDEX
        di2 = getDataInputForOffset(_hdrSize);
        _nameIndex = new NameIndex(di2);
        
        // Top DICT INDEX
        _topDictIndex = new TopDictIndex(di2);

        // String INDEX
        _stringIndex = new StringIndex(di2);
        
        // Global Subr INDEX
        _globalSubrIndex = new Index(di2);
        
        // Encodings go here -- but since this is an OpenType font will this
        // not always be a CIDFont?  In which case there are no encodings
        // within the CFF data.
        
        // Load each of the fonts
        _charStringsIndexArray = new Index[_topDictIndex.getCount()];
        _charsets = new Charset[_topDictIndex.getCount()];
        _charstringsArray = new Charstring[_topDictIndex.getCount()][];
        for (int i = 0; i < _topDictIndex.getCount(); ++i) {

            // Charstrings INDEX
            // We load this before Charsets because we may need to know the number
            // of glyphs
            Integer charStringsOffset = (Integer) _topDictIndex.getTopDict(i).getValue(17);
            di2 = getDataInputForOffset(charStringsOffset);
            _charStringsIndexArray[i] = new Index(di2);
            int glyphCount = _charStringsIndexArray[i].getCount();
        
            // Charsets
            Integer charsetOffset = (Integer) _topDictIndex.getTopDict(i).getValue(15);
            di2 = getDataInputForOffset(charsetOffset);
            int format = di2.readUnsignedByte();
            switch (format) {
                case 0:
                    _charsets[i] = new CharsetFormat0(di2, glyphCount);
                    break;
                case 1:
                    _charsets[i] = new CharsetFormat1(di2, glyphCount);
                    break;
                case 2:
                    _charsets[i] = new CharsetFormat2(di2, glyphCount);
                    break;
            }

            // Create the charstrings
            _charstringsArray[i] = new Charstring[glyphCount];
            for (int j = 0; j < glyphCount; ++j) {
                int offset = _charStringsIndexArray[i].getOffset(j) - 1;
                int len = _charStringsIndexArray[i].getOffset(j + 1) - offset - 1;
                _charstringsArray[i][j] = new CharstringType2(
                        i,
                        _stringIndex.getString(_charsets[i].getSID(j)),
                        _charStringsIndexArray[i].getData(),
                        offset,
                        len,
                        null,
                        null);
            }
        }
    }
    
    private DataInput getDataInputForOffset(int offset) {
        return new DataInputStream(new ByteArrayInputStream(
                _buf, offset,
                _de.getLength() - offset));
    }

    public NameIndex getNameIndex() {
        return _nameIndex;
    }
    
    public Charset getCharset(int fontIndex) {
        return _charsets[fontIndex];
    }

    public Charstring getCharstring(int fontIndex, int gid) {
        return _charstringsArray[fontIndex][gid];
    }
    
    public int getCharstringCount(int fontIndex) {
        return _charstringsArray[fontIndex].length;
    }

    public int getType() {
        return CFF;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("'CFF' Table - Compact Font Format\n---------------------------------\n");
        sb.append("\nName INDEX\n");
        sb.append(_nameIndex.toString());
        sb.append("\nTop DICT INDEX\n");
        sb.append(_topDictIndex.toString());
        sb.append("\nString INDEX\n");
        sb.append(_stringIndex.toString());
        sb.append("\nGlobal Subr INDEX\n");
        sb.append(_globalSubrIndex.toString());
        for (int i = 0; i < _charStringsIndexArray.length; ++i) {
            sb.append("\nCharStrings INDEX ").append(i).append("\n");
            sb.append(_charStringsIndexArray[i].toString());
        }
        return sb.toString();
    }
    
    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }
}
