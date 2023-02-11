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
class CpalTable implements Table {

    static class ColorRecord {

        private final short _blue;
        private final short _green;
        private final short _red;
        private final short _alpha;

        ColorRecord(DataInput di) throws IOException {
            _blue = (short) di.readUnsignedByte();
            _green = (short) di.readUnsignedByte();
            _red = (short) di.readUnsignedByte();
            _alpha = (short) di.readUnsignedByte();
        }

        short getBlue() {
            return _blue;
        }

        short getGreen() {
            return _green;
        }

        short getRed() {
            return _red;
        }

        short getAlpha() {
            return _alpha;
        }
    }

    private final int _version;
    private final int _numPalettesEntries;
    private final int _numPalette;
    private final int _numColorRecords;
    private final int _offsetFirstColorRecord;
    private final int[] _colorRecordIndices;
    private final int _offsetPaletteTypeArray;
    private final int _offsetPaletteLabelArray;
    private final int _offsetPaletteEntryLabelArray;
    private final ColorRecord[] _colorRecords;

    protected CpalTable(DataInput di) throws IOException {
        _version = di.readUnsignedShort();
        _numPalettesEntries = di.readUnsignedShort();
        _numPalette = di.readUnsignedShort();
        _numColorRecords = di.readUnsignedShort();
        _offsetFirstColorRecord = di.readInt();

        int byteCount = 12;
        _colorRecordIndices = new int[_numPalette];
        for (int i = 0; i < _numPalette; ++i) {
            _colorRecordIndices[i] = di.readUnsignedShort();
            byteCount += 2;
        }
        if (_version == 1) {
            _offsetPaletteTypeArray = di.readInt();
            _offsetPaletteLabelArray = di.readInt();
            _offsetPaletteEntryLabelArray = di.readInt();
            byteCount += 12;
        } else {
            _offsetPaletteTypeArray = -1;
            _offsetPaletteLabelArray = -1;
            _offsetPaletteEntryLabelArray = -1;
        }

        if (_offsetFirstColorRecord > byteCount) {
            di.skipBytes(byteCount - _offsetFirstColorRecord);
        }

        _colorRecords = new ColorRecord[_numColorRecords];
        for (int i = 0; i < _numColorRecords; ++i) {
            _colorRecords[i] = new ColorRecord(di);
        }

        if (_version == 1) {
            // TODO find some sample version 1 content
        }
    }

    public int getNumPalettesEntries() {
        return _numPalettesEntries;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'CPAL' Table\n------------\nColor Record Indices\n");
        int i = 0;
        for (int index : _colorRecordIndices) {
            sb.append(String.format("%d: %d\n", i++, index));
        }
        sb.append("\nColor Records\n");
        i = 0;
        for (ColorRecord record : _colorRecords) {
            sb.append(String.format("%d: B: %3d, G: %3d, R: %3d, A: %3d\n",
                    i++, record.getBlue(), record.getGreen(), record.getRed(),
                    record.getAlpha()));
        }
        return sb.toString();
    }

}
