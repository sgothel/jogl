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
 * @version $Id: LtshTable.java,v 1.1.1.1 2004-12-05 23:14:51 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class LtshTable implements Table {

    private DirectoryEntry de;
    private int version;
    private int numGlyphs;
    private int[] yPels;
    
    /** Creates new LtshTable */
    protected LtshTable(DirectoryEntry de, DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
        version = di.readUnsignedShort();
        numGlyphs = di.readUnsignedShort();
        yPels = new int[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
            yPels[i] = di.readUnsignedByte();
        }
    }

    /**
     * Get the table type, as a table directory value.
     * @return The table type
     */
    public int getType() {
        return LTSH;
    }
    
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("'LTSH' Table - Linear Threshold Table\n-------------------------------------")
            .append("\n 'LTSH' Version:       ").append(version)
            .append("\n Number of Glyphs:     ").append(numGlyphs)
            .append("\n\n   Glyph #   Threshold\n   -------   ---------\n");
        for (int i = 0; i < numGlyphs; i++) {
            sb.append("   ").append(i).append(".        ").append(yPels[i])
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
    public DirectoryEntry getDirectoryEntry() {
        return de;
    }
    
}
