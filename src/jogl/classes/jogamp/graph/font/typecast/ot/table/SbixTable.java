/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
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
package jogamp.graph.font.typecast.ot.table;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * This table provides access to bitmap data in a standard graphics format
 * (such as PNG, JPEG, TIFF).
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class SbixTable implements Table {

    private static final boolean DEBUG = false;
    
    public static class GlyphDataRecord {
        private final short _originOffsetX;
        private final short _originOffsetY;
        private final int _graphicType;
        private final byte[] _data;
        
        private static final int PNG = 0x706E6720;
        
        GlyphDataRecord(DataInput di, int dataLength) throws IOException {
            _originOffsetX = di.readShort();
            _originOffsetY = di.readShort();
            _graphicType = di.readInt();
            
            // Check the graphicType is valid
            if (_graphicType != PNG) {
                System.err.printf("SbixTable: Invalid graphicType: %d%n", _graphicType);
                _data = null;
                return;
            }

            _data = new byte[dataLength];
            try {
                di.readFully(_data);
            } catch (IOException e) {
                System.err.println("SbixTable: Reading too much data: "+e.getMessage());
            }
        }
        
        public int getGraphicType() {
            return _graphicType;
        }
        
        public byte[] getData() {
            return _data;
        }
    }
    
    public static class Strike {
        private final int _ppem;
        private final int _resolution;
        private final long[] _glyphDataOffset;
        private final GlyphDataRecord[] _glyphDataRecord;
        
        Strike(ByteArrayInputStream bais, int numGlyphs) throws IOException {
            DataInput di = new DataInputStream(bais);
            _ppem = di.readUnsignedShort();
            _resolution = di.readUnsignedShort();
            _glyphDataOffset = new long[numGlyphs + 1];
            for (int i = 0; i < numGlyphs + 1; ++i) {
                _glyphDataOffset[i] = di.readInt();
            }

            _glyphDataRecord = new GlyphDataRecord[numGlyphs];
            for (int i = 0; i < numGlyphs; ++i) {
                int dataLength = (int)(_glyphDataOffset[i + 1] - _glyphDataOffset[i]);
                if (dataLength == 0)
                    continue;
                bais.reset();
                if( DEBUG ) {
                    System.err.printf("SbixTable: Skip: %d%n", _glyphDataOffset[i]);
                }
                bais.skip(_glyphDataOffset[i]);
                _glyphDataRecord[i] = new GlyphDataRecord(new DataInputStream(bais), dataLength);
            }
            if( DEBUG ) {
                System.err.printf("SbixTable: Loaded Strike: ppem = %d, resolution = %d%n", _ppem, _resolution);
            }
        }
        
        public GlyphDataRecord[] getGlyphDataRecords() {
            return _glyphDataRecord;
        }
        
        @Override
        public String toString() {
            return String.format("ppem: %d, resolution: %d", _ppem, _resolution);
        }
    }
    
    private final int _version;
    private final int _flags;
    private final int _numStrikes;
    private final int[] _strikeOffset;
    private final Strike[] _strikes;

    private SbixTable(DataInput di, int length, MaxpTable maxp) throws IOException {

        // Load entire table into a buffer, and create another input stream
        byte[] buf = new byte[length];
        di.readFully(buf);
        DataInput di2 = new DataInputStream(getByteArrayInputStreamForOffset(buf, 0));

        _version = di2.readUnsignedShort();
        _flags = di2.readUnsignedShort();
        _numStrikes = di2.readInt();
        _strikeOffset = new int[_numStrikes];
        for (int i = 0; i < _numStrikes; ++i) {
            _strikeOffset[i] = di2.readInt();
        }
        
        _strikes = new Strike[_numStrikes];
        for (int i = 0; i < _numStrikes; ++i) {
            ByteArrayInputStream bais = getByteArrayInputStreamForOffset(buf, _strikeOffset[i]);
            _strikes[i] = new Strike(bais, maxp.getNumGlyphs());
        }
    }

    private ByteArrayInputStream getByteArrayInputStreamForOffset(byte[] buf, int offset) {
        return new ByteArrayInputStream(
                buf, offset,
                buf.length - offset);
    }
    
    @Override
    public int getType() {
        return sbix;
    }

    public Strike[] getStrikes() {
        return _strikes;
    }

}
