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

package jogamp.graph.font.typecast.ot.table;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import jogamp.graph.font.typecast.cff.CffFont;
import jogamp.graph.font.typecast.cff.Index;
import jogamp.graph.font.typecast.cff.NameIndex;
import jogamp.graph.font.typecast.cff.StringIndex;
import jogamp.graph.font.typecast.cff.TopDictIndex;

/**
 * Compact Font Format Table
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CffTable implements Table {
    
    private final int _major;
    private final int _minor;
    private final int _hdrSize;
    private final int _offSize;
    private final NameIndex _nameIndex;
    private final TopDictIndex _topDictIndex;
    private final StringIndex _stringIndex;
    private final Index _globalSubrIndex;
    private final CffFont[] _fonts;

    private final byte[] _buf;

    /** Creates a new instance of CffTable
     * @param di
     * @param length
     * @throws java.io.IOException */
    protected CffTable(DataInput di, int length) throws IOException {

        // Load entire table into a buffer, and create another input stream
        _buf = new byte[length];
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
        
        // TESTING
//        Charstring gscs = new CharstringType2(
//                null,
//                0,
//                "Global subrs",
//                _globalSubrIndex.getData(),
//                _globalSubrIndex.getOffset(0) - 1,
//                _globalSubrIndex.getDataLength());
//        System.out.println(gscs.toString());

        // Encodings go here -- but since this is an OpenType font will this
        // not always be a CIDFont?  In which case there are no encodings
        // within the CFF data.
        
        // Load each of the fonts
        _fonts = new CffFont[_topDictIndex.getCount()];
        for (int i = 0; i < _topDictIndex.getCount(); ++i) {
            _fonts[i] = new CffFont(this, i, _topDictIndex.getTopDict(i));
        }
    }
    
    public final DataInput getDataInputForOffset(int offset) {
        return new DataInputStream(new ByteArrayInputStream(
                _buf, offset,
                _buf.length - offset));
    }

    public NameIndex getNameIndex() {
        return _nameIndex;
    }

    public StringIndex getStringIndex() {
        return _stringIndex;
    }
    
    public Index getGlobalSubrIndex() {
        return _globalSubrIndex;
    }

    public CffFont getFont(int fontIndex) {
        return _fonts[fontIndex];
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'CFF' Table - Compact Font Format\n---------------------------------\n");
        sb.append("\nName INDEX\n");
        sb.append(_nameIndex.toString());
        sb.append("\nTop DICT INDEX\n");
        sb.append(_topDictIndex.toString());
        sb.append("\nString INDEX\n");
        sb.append(_stringIndex.toString());
        sb.append("\nGlobal Subr INDEX\n");
        sb.append(_globalSubrIndex.toString());
        for (int i = 0; i < _fonts.length; ++i) {
            sb.append("\nCharStrings INDEX ").append(i).append("\n");
            sb.append(_fonts[i].getCharStringsIndex().toString());
        }
        return sb.toString();
    }

}
