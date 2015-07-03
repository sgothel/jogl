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
 * @version $Id: CvtTable.java,v 1.1.1.1 2004-12-05 23:14:36 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CvtTable implements Table {

    private final DirectoryEntry de;
    private final short[] values;

    protected CvtTable(final DirectoryEntry de, final DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
        final int len = de.getLength() / 2;
        values = new short[len];
        for (int i = 0; i < len; i++) {
            values[i] = di.readShort();
        }
    }

    @Override
    public int getType() {
        return cvt;
    }

    public short[] getValues() {
        return values;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'cvt ' Table - Control Value Table\n----------------------------------\n");
        sb.append("Size = ").append(0).append(" bytes, ").append(values.length).append(" entries\n");
        sb.append("        Values\n        ------\n");
        for (int i = 0; i < values.length; i++) {
            sb.append("        ").append(i).append(": ").append(values[i]).append("\n");
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
        return de;
    }

}
