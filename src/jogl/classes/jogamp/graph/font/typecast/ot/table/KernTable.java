/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInput;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class KernTable implements Table {

    private final int version;
    private final int nTables;
    private final KernSubtable[] tables;
    private final KernSubtableFormat0 table0;

    /** Creates new KernTable */
    public KernTable(final DataInput di) throws IOException {
        version = di.readUnsignedShort();
        nTables = di.readUnsignedShort();
        tables = new KernSubtable[nTables];
        KernSubtableFormat0 _table0 = null;
        for (int i = 0; i < nTables; i++) {
            tables[i] = KernSubtable.read(di);
            if( null == _table0 && 0 == tables[i].getSubtableFormat() ) {
                _table0 = (KernSubtableFormat0)tables[i];
            }
        }
        table0 = _table0;
    }

    public int getSubtableCount() {
        return nTables;
    }

    public KernSubtable getSubtable(final int i) {
        return tables[i];
    }

    public KernSubtableFormat0 getSubtable0() {
        return table0;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'kern' Table\n--------------------------")
          .append("\n  version:   ").append(version)
          .append("\n  subtables: ").append(nTables);
        for (int i = 0; i < nTables; i++) {
            sb.append("\n  ");
            sb.append(tables[i].toString());
        }
        return sb.toString();
    }
}
