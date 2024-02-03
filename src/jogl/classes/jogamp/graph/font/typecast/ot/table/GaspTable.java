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
public class GaspTable implements Table {

    private int version;
    private int numRanges;
    private GaspRange[] gaspRange;
    
    /** Creates new GaspTable */
    public GaspTable(DataInput di) throws IOException {
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'gasp' Table - Grid-fitting And Scan-conversion Procedure\n---------------------------------------------------------");
        sb.append("\n  'gasp' version:      ").append(version);
        sb.append("\n  numRanges:           ").append(numRanges);
        for (int i = 0; i < numRanges; i++) {
            sb.append("\n\n  gasp Range ").append(i).append("\n");
            sb.append(gaspRange[i].toString());
        }
        return sb.toString();
    }

}
