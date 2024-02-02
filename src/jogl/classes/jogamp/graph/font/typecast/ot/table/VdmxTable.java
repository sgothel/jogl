/*
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

import java.io.DataInput;
import java.io.IOException;

/**
 * The Vertical Device Metrics table for TrueType outlines.
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class VdmxTable implements Table {

    private static class Ratio {

        private final byte _bCharSet;
        private final byte _xRatio;
        private final byte _yStartRatio;
        private final byte _yEndRatio;

        Ratio(final DataInput di) throws IOException {
            _bCharSet = di.readByte();
            _xRatio = di.readByte();
            _yStartRatio = di.readByte();
            _yEndRatio = di.readByte();
        }

        byte getBCharSet() {
            return _bCharSet;
        }

        byte getXRatio() {
            return _xRatio;
        }

        byte getYStartRatio() {
            return _yStartRatio;
        }

        byte getYEndRatio() {
            return _yEndRatio;
        }
    }

    private static class VTableRecord {

        private final int _yPelHeight;
        private final short _yMax;
        private final short _yMin;

        VTableRecord(final DataInput di) throws IOException {
            _yPelHeight = di.readUnsignedShort();
            _yMax = di.readShort();
            _yMin = di.readShort();
        }

        int getYPelHeight() {
            return _yPelHeight;
        }

        short getYMax() {
            return _yMax;
        }

        short getYMin() {
            return _yMin;
        }
    }

    private static class Group {

        private final int _recs;
        private final int _startsz;
        private final int _endsz;
        private final VTableRecord[] _entry;

        Group(final DataInput di) throws IOException {
            _recs = di.readUnsignedShort();
            _startsz = di.readUnsignedByte();
            _endsz = di.readUnsignedByte();
            _entry = new VTableRecord[_recs];
            for (int i = 0; i < _recs; ++i) {
                _entry[i] = new VTableRecord(di);
            }
        }

        int getRecs() {
            return _recs;
        }

        int getStartSZ() {
            return _startsz;
        }

        int getEndSZ() {
            return _endsz;
        }

        VTableRecord[] getEntry() {
            return _entry;
        }
    }

    private final int _version;
    private final int _numRecs;
    private final int _numRatios;
    private final Ratio[] _ratRange;
    private final int[] _offset;
    private final Group[] _groups;

    /** Creates a new instance of VdmxTable */
    public VdmxTable(final DataInput di) throws IOException {
        _version = di.readUnsignedShort();
        _numRecs = di.readUnsignedShort();
        _numRatios = di.readUnsignedShort();
        _ratRange = new Ratio[_numRatios];
        for (int i = 0; i < _numRatios; ++i) {
            _ratRange[i] = new Ratio(di);
        }
        _offset = new int[_numRatios];
        for (int i = 0; i < _numRatios; ++i) {
            _offset[i] = di.readUnsignedShort();
        }
        _groups = new Group[_numRecs];
        for (int i = 0; i < _numRecs; ++i) {
            _groups[i] = new Group(di);
        }
    }

    @Override
    public int getType() {
        return VDMX;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'VDMX' Table - Precomputed Vertical Device Metrics\n")
            .append("--------------------------------------------------\n")
            .append("  Version:                 ").append(_version).append("\n")
            .append("  Number of Hgt Records:   ").append(_numRecs).append("\n")
            .append("  Number of Ratio Records: ").append(_numRatios).append("\n");
        for (int i = 0; i < _numRatios; ++i) {
            sb.append("\n    Ratio Record #").append(i + 1).append("\n")
                .append("\tCharSetId     ").append(_ratRange[i].getBCharSet()).append("\n")
                .append("\txRatio        ").append(_ratRange[i].getXRatio()).append("\n")
                .append("\tyStartRatio   ").append(_ratRange[i].getYStartRatio()).append("\n")
                .append("\tyEndRatio     ").append(_ratRange[i].getYEndRatio()).append("\n")
                .append("\tRecord Offset ").append(_offset[i]).append("\n");
        }
        sb.append("\n   VDMX Height Record Groups\n")
            .append("   -------------------------\n");
        for (int i = 0; i < _numRecs; ++i) {
            final Group group = _groups[i];
            sb.append("   ").append(i + 1)
                .append(".   Number of Hgt Records  ").append(group.getRecs()).append("\n")
                .append("        Starting Y Pel Height  ").append(group.getStartSZ()).append("\n")
                .append("        Ending Y Pel Height    ").append(group.getEndSZ()).append("\n");
            for (int j = 0; j < group.getRecs(); ++j) {
                sb.append("\n            ").append(j + 1)
                    .append(". Pel Height= ").append(group.getEntry()[j].getYPelHeight()).append("\n")
                    .append("               yMax=       ").append(group.getEntry()[j].getYMax()).append("\n")
                    .append("               yMin=       ").append(group.getEntry()[j].getYMin()).append("\n");
            }
        }
        return sb.toString();
    }

}
