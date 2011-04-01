/*
 * $Id: VdmxTable.java,v 1.1 2007-01-30 05:25:35 davidsch Exp $
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

import java.io.DataInput;
import java.io.IOException;

/**
 * The Vertical Device Metrics table for TrueType outlines.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: VdmxTable.java,v 1.1 2007-01-30 05:25:35 davidsch Exp $
 */
public class VdmxTable implements Table {

    private class Ratio {
        
        private byte _bCharSet;
        private byte _xRatio;
        private byte _yStartRatio;
        private byte _yEndRatio;
        
        protected Ratio(DataInput di) throws IOException {
            _bCharSet = di.readByte();
            _xRatio = di.readByte();
            _yStartRatio = di.readByte();
            _yEndRatio = di.readByte();
        }

        public byte getBCharSet() {
            return _bCharSet;
        }
        
        public byte getXRatio() {
            return _xRatio;
        }
        
        public byte getYStartRatio() {
            return _yStartRatio;
        }
        
        public byte getYEndRatio() {
            return _yEndRatio;
        }
    }
    
    private class VTableRecord {
        
        private int _yPelHeight;
        private short _yMax;
        private short _yMin;
        
        protected VTableRecord(DataInput di) throws IOException {
            _yPelHeight = di.readUnsignedShort();
            _yMax = di.readShort();
            _yMin = di.readShort();
        }

        public int getYPelHeight() {
            return _yPelHeight;
        }
        
        public short getYMax() {
            return _yMax;
        }
        
        public short getYMin() {
            return _yMin;
        }
    }
    
    private class Group {
        
        private int _recs;
        private int _startsz;
        private int _endsz;
        private VTableRecord[] _entry;
        
        protected Group(DataInput di) throws IOException {
            _recs = di.readUnsignedShort();
            _startsz = di.readUnsignedByte();
            _endsz = di.readUnsignedByte();
            _entry = new VTableRecord[_recs];
            for (int i = 0; i < _recs; ++i) {
                _entry[i] = new VTableRecord(di);
            }
        }

        public int getRecs() {
            return _recs;
        }
        
        public int getStartSZ() {
            return _startsz;
        }
        
        public int getEndSZ() {
            return _endsz;
        }
        
        public VTableRecord[] getEntry() {
            return _entry;
        }
    }
    
    private DirectoryEntry _de;
    private int _version;
    private int _numRecs;
    private int _numRatios;
    private Ratio[] _ratRange;
    private int _offset[];
    private Group[] _groups;
    
    /** Creates a new instance of VdmxTable */
    protected VdmxTable(DirectoryEntry de, DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();
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
    
    public int getType() {
        return VDMX;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
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
            Group group = _groups[i];
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
