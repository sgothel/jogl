/*
 * $Id: HdmxTable.java,v 1.2 2007-07-26 11:12:30 davidsch Exp $
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
 * The Horizontal Device Metrics table for TrueType outlines.  This stores
 * integer advance widths scaled to specific pixel sizes.
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: HdmxTable.java,v 1.2 2007-07-26 11:12:30 davidsch Exp $
 */
public class HdmxTable implements Table {
    
    public class DeviceRecord {
        
        private short _pixelSize;
        private short _maxWidth;
        private short[] _widths;

        protected DeviceRecord(int numGlyphs, DataInput di) throws IOException {
            _pixelSize = di.readByte();
            _maxWidth = di.readByte();
            _widths = new short[numGlyphs];
            for (int i = 0; i < numGlyphs; ++i) {
                _widths[i] = di.readByte();
            }
        }

        public short getPixelSize() {
            return _pixelSize;
        }
        
        public short getMaxWidth() {
            return _maxWidth;
        }
        
        public short[] getWidths() {
            return _widths;
        }

        public short getWidth(int glyphidx) {
            return _widths[glyphidx];
        }
        
    }
    
    private DirectoryEntry _de;
    private int _version;
    private short _numRecords;
    private int _sizeDeviceRecords;
    private DeviceRecord[] _records;

    /** Creates a new instance of HdmxTable */
    protected HdmxTable(DirectoryEntry de, DataInput di, MaxpTable maxp)
    throws IOException {
        _de = (DirectoryEntry) de.clone();
        _version = di.readUnsignedShort();
        _numRecords = di.readShort();
        _sizeDeviceRecords = di.readInt();
        _records = new DeviceRecord[_numRecords];
        
        // Read the device records
        for (int i = 0; i < _numRecords; ++i) {
            _records[i] = new DeviceRecord(maxp.getNumGlyphs(), di);
        }
    }

    public int getNumberOfRecords() {
        return _numRecords;
    }
    
    public DeviceRecord getRecord(int i) {
        return _records[i];
    }
    
    public int getType() {
        return hdmx;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("'hdmx' Table - Horizontal Device Metrics\n----------------------------------------\n");
        sb.append("Size = ").append(_de.getLength()).append(" bytes\n")
            .append("\t'hdmx' version:         ").append(_version).append("\n")
            .append("\t# device records:       ").append(_numRecords).append("\n")
            .append("\tRecord length:          ").append(_sizeDeviceRecords).append("\n");
        for (int i = 0; i < _numRecords; ++i) {
            sb.append("\tDevRec ").append(i)
                .append(": ppem = ").append(_records[i].getPixelSize())
                .append(", maxWid = ").append(_records[i].getMaxWidth())
                .append("\n");
            for (int j = 0; j < _records[i].getWidths().length; ++j) {
                sb.append("    ").append(j).append(".   ")
                    .append(_records[i].getWidths()[j]).append("\n");
            }
            sb.append("\n\n");
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
