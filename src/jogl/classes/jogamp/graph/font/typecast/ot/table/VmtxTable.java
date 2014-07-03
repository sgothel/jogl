/*
 * $Id: VmtxTable.java,v 1.1 2007-01-31 01:18:04 davidsch Exp $
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
 * Vertical Metrics Table
 * @version $Id: VmtxTable.java,v 1.1 2007-01-31 01:18:04 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class VmtxTable implements Table {

    private final DirectoryEntry _de;
    private int[] _vMetrics = null;
    private short[] _topSideBearing = null;

    protected VmtxTable(
            final DirectoryEntry de,
            final DataInput di,
            final VheaTable vhea,
            final MaxpTable maxp) throws IOException {
        _de = (DirectoryEntry) de.clone();
        _vMetrics = new int[vhea.getNumberOfLongVerMetrics()];
        for (int i = 0; i < vhea.getNumberOfLongVerMetrics(); ++i) {
            _vMetrics[i] =
                    di.readUnsignedByte()<<24
                    | di.readUnsignedByte()<<16
                    | di.readUnsignedByte()<<8
                    | di.readUnsignedByte();
        }
        final int tsbCount = maxp.getNumGlyphs() - vhea.getNumberOfLongVerMetrics();
        _topSideBearing = new short[tsbCount];
        for (int i = 0; i < tsbCount; ++i) {
            _topSideBearing[i] = di.readShort();
        }
    }

    public int getAdvanceHeight(final int i) {
        if (_vMetrics == null) {
            return 0;
        }
        if (i < _vMetrics.length) {
            return _vMetrics[i] >> 16;
        } else {
            return _vMetrics[_vMetrics.length - 1] >> 16;
        }
    }

    public short getTopSideBearing(final int i) {
        if (_vMetrics == null) {
            return 0;
        }
        if (i < _vMetrics.length) {
            return (short)(_vMetrics[i] & 0xffff);
        } else {
            return _topSideBearing[i - _vMetrics.length];
        }
    }

    @Override
    public int getType() {
        return vmtx;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'vmtx' Table - Vertical Metrics\n-------------------------------\n");
        sb.append("Size = ").append(_de.getLength()).append(" bytes, ")
            .append(_vMetrics.length).append(" entries\n");
        for (int i = 0; i < _vMetrics.length; i++) {
            sb.append("        ").append(i)
                .append(". advHeight: ").append(getAdvanceHeight(i))
                .append(", TSdBear: ").append(getTopSideBearing(i))
                .append("\n");
        }
        for (int i = 0; i < _topSideBearing.length; i++) {
            sb.append("        TSdBear ").append(i + _vMetrics.length)
                .append(": ").append(_topSideBearing[i])
                .append("\n");
        }
        return sb.toString();
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    @Override
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }
}
