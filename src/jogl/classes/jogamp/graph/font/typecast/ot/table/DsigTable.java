/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included with this distribution in  * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

import java.io.IOException;
import java.io.DataInput;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class DsigTable implements Table {

    private int version;
    private int numSigs;
    private int flag;
    private DsigEntry[] dsigEntry;
    private SignatureBlock[] sigBlocks;

    /** Creates new DsigTable */
    protected DsigTable(DataInput di) throws IOException {
        version = di.readInt();
        numSigs = di.readUnsignedShort();
        flag = di.readUnsignedShort();
        dsigEntry = new DsigEntry[numSigs];
        sigBlocks = new SignatureBlock[numSigs];
        for (int i = 0; i < numSigs; i++) {
            dsigEntry[i] = new DsigEntry(di);
        }
        for (int i = 0; i < numSigs; i++) {
            sigBlocks[i] = new SignatureBlock(di);
        }
    }

    public String toString() {
        StringBuilder sb = new StringBuilder().append("DSIG\n");
        for (int i = 0; i < numSigs; i++) {
            sb.append(sigBlocks[i].toString());
        }
        return sb.toString();
    }
}
