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
 * @version $Id: DsigTable.java,v 1.1.1.1 2004-12-05 23:14:37 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class DsigTable implements Table {

    private DirectoryEntry de;
    private int version;
    private int numSigs;
    private int flag;
    private DsigEntry[] dsigEntry;
    private SignatureBlock[] sigBlocks;

    /** Creates new DsigTable */
    protected DsigTable(DirectoryEntry de, DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
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

    /**
     * Get the table type, as a table directory value.
     * @return The table type
     */
    public int getType() {
        return DSIG;
    }
    
    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry() {
        return de;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer().append("DSIG\n");
        for (int i = 0; i < numSigs; i++) {
            sb.append(sigBlocks[i].toString());
        }
        return sb.toString();
    }
}
