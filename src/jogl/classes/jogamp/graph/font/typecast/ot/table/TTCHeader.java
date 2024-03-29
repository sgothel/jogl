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
public class TTCHeader {
    
    private static final int ttcf = 0x74746366;

    private int ttcTag;
    private int version;
    private int directoryCount;
    private int[] tableDirectory;
    private int dsigTag;
    private int dsigLength;
    private int dsigOffset;

    /** Creates new TTCHeader */
    public TTCHeader(DataInput di) throws IOException {
        ttcTag = di.readInt();
        version = di.readInt();
        directoryCount = di.readInt();
        tableDirectory = new int[directoryCount];
        for (int i = 0; i < directoryCount; i++) {
            tableDirectory[i] = di.readInt();
        }
        if (version == 0x00010000) {
            dsigTag = di.readInt();
        }
        dsigLength = di.readInt();
        dsigOffset = di.readInt();
    }

    public int getDirectoryCount() {
        return directoryCount;
    }
    
    public int getTableDirectory(int i) {
        return tableDirectory[i];
    }

    public static boolean isTTC(DataInput di) throws IOException {
        int ttcTag = di.readInt();
        return ttcTag == ttcf;
    }
}
