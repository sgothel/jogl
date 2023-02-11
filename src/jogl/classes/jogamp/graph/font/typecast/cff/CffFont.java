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
import java.util.List;
import jogamp.graph.font.typecast.ot.table.CffTable;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CffFont {
    
    private final CffTable _table;
    private final Dict _topDict;
    private final Index _charStringsIndex;
    private final Dict _privateDict;
    private final Index _localSubrIndex;
    private final Charset _charset;
    private final Charstring[] _charstrings;

    public CffFont(
            CffTable table,
            int index,
            Dict topDict) throws IOException {
        _table = table;
        _topDict = topDict;

        // Charstrings INDEX
        // We load this before Charsets because we may need to know the number
        // of glyphs
        Integer charStringsOffset = (Integer) _topDict.getValue(17);
        DataInput di = _table.getDataInputForOffset(charStringsOffset);
        _charStringsIndex = new Index(di);
        int glyphCount = _charStringsIndex.getCount();

        // Private DICT
        List<Integer> privateSizeAndOffset = (List<Integer>) _topDict.getValue(18);
        di = _table.getDataInputForOffset(privateSizeAndOffset.get(1));
        _privateDict = new Dict(di, privateSizeAndOffset.get(0));

        // Local Subrs INDEX
        Integer localSubrsOffset = (Integer) _privateDict.getValue(19);
        if (localSubrsOffset != null) {
            di = table.getDataInputForOffset(privateSizeAndOffset.get(1) + localSubrsOffset);
            _localSubrIndex = new Index(di);
        } else {
            _localSubrIndex = null;
            //throw new Exception();
        }

        // Charsets
        Integer charsetOffset = (Integer) _topDict.getValue(15);
        di = table.getDataInputForOffset(charsetOffset);
        int format = di.readUnsignedByte();
        switch (format) {
            case 0:
                _charset = new CharsetFormat0(di, glyphCount);
                break;
            case 1:
                _charset = new CharsetFormat1(di, glyphCount);
                break;
            case 2:
                _charset = new CharsetFormat2(di, glyphCount);
                break;
            default:
                _charset = null;
                //throw new Exception();
        }

        // Create the charstrings
        _charstrings = new Charstring[glyphCount];
        for (int i = 0; i < glyphCount; ++i) {
            int offset = _charStringsIndex.getOffset(i) - 1;
            int len = _charStringsIndex.getOffset(i + 1) - offset - 1;
            _charstrings[i] = new CharstringType2(
                    this,
                    index,
                    table.getStringIndex().getString(_charset.getSID(i)),
                    _charStringsIndex.getData(),
                    offset,
                    len);
        }
    }

    public CffTable getTable() {
        return _table;
    }

    public Index getCharStringsIndex() {
        return _charStringsIndex;
    }

    public Dict getPrivateDict() {
        return _privateDict;
    }

    public Index getLocalSubrIndex() {
        return _localSubrIndex;
    }

    public Charset getCharset() {
        return _charset;
    }

    public Charstring getCharstring(int gid) {
        return _charstrings[gid];
    }
    
    public int getCharstringCount() {
        return _charstrings.length;
    }
}
