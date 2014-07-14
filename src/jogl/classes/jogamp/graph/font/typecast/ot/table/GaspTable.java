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
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: GaspTable.java,v 1.1.1.1 2004-12-05 23:14:39 davidsch Exp $
 */
public class GaspTable implements Table {

    private final DirectoryEntry de;
    private final int version;
    private final int numRanges;
    private final GaspRange[] gaspRange;

    /** Creates new GaspTable */
    protected GaspTable(final DirectoryEntry de, final DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
        version = di.readUnsignedShort();
        numRanges = di.readUnsignedShort();
        gaspRange = new GaspRange[numRanges];
        for (int i = 0; i < numRanges; i++) {
            gaspRange[i] = new GaspRange(di);
        }
    }

    @Override
    public int getType() {
        return gasp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'gasp' Table - Grid-fitting And Scan-conversion Procedure\n---------------------------------------------------------");
        sb.append("\n  'gasp' version:      ").append(version);
        sb.append("\n  numRanges:           ").append(numRanges);
        for (int i = 0; i < numRanges; i++) {
            sb.append("\n\n  gasp Range ").append(i).append("\n");
            sb.append(gaspRange[i].toString());
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
