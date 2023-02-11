/*
 * Copyright (c) David Schweinsberg
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

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class ColrTable implements Table {

    static class BaseGlyphRecord {

        private final int _gid;
        private final int _firstLayerIndex;
        private final int _numLayers;

        BaseGlyphRecord(DataInput di) throws IOException {
            _gid = di.readUnsignedShort();
            _firstLayerIndex = di.readUnsignedShort();
            _numLayers = di.readUnsignedShort();
        }

        int getGid() {
            return _gid;
        }

        int getFirstLayerIndex() {
            return _firstLayerIndex;
        }

        int getNumLayers() {
            return _numLayers;
        }
    }

    static class LayerRecord {

        private final int _gid;
        private final int _paletteIndex;

        LayerRecord(DataInput di) throws IOException {
            _gid = di.readUnsignedShort();
            _paletteIndex = di.readUnsignedShort();
        }

        int getGid() {
            return _gid;
        }

        int getPaletteIndex() {
            return _paletteIndex;
        }
    }

    private final int _version;
    private final int _numBaseGlyphRecords;
    private final int _offsetBaseGlyphRecord;
    private final int _offsetLayerRecord;
    private final int _numLayerRecords;
    private final BaseGlyphRecord[] _baseGlyphRecords;
    private final LayerRecord[] _layerRecords;

    protected ColrTable(DataInput di) throws IOException {
        _version = di.readUnsignedShort();
        _numBaseGlyphRecords = di.readUnsignedShort();
        _offsetBaseGlyphRecord = di.readInt();
        _offsetLayerRecord = di.readInt();
        _numLayerRecords = di.readUnsignedShort();

        int byteCount = 14;
        if (_offsetBaseGlyphRecord > byteCount) {
            di.skipBytes(byteCount - _offsetBaseGlyphRecord);
        }

        _baseGlyphRecords = new BaseGlyphRecord[_numBaseGlyphRecords];
        for (int i = 0; i < _numBaseGlyphRecords; ++i) {
            _baseGlyphRecords[i] = new BaseGlyphRecord(di);
            byteCount += 6;
        }

        if (_offsetLayerRecord > byteCount) {
            di.skipBytes(byteCount - _offsetLayerRecord);
        }

        _layerRecords = new LayerRecord[_numLayerRecords];
        for (int i = 0; i < _numLayerRecords; ++i) {
            _layerRecords[i] = new LayerRecord(di);
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'COLR' Table\n------------\nBase Glyph Records\n");
        for (BaseGlyphRecord record : _baseGlyphRecords) {
            sb.append(String.format("%d : %d, %d\n", record.getGid(),
                    record.getFirstLayerIndex(), record.getNumLayers()));
        }
        sb.append("\nLayer Records\n");
        for (LayerRecord record : _layerRecords) {
            sb.append(String.format("%d : %d\n", record.getGid(),
                    record.getPaletteIndex()));
        }
        return sb.toString();
    }

}
