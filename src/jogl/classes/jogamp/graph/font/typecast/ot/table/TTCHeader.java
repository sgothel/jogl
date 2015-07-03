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
 * @version $Id: TTCHeader.java,v 1.1.1.1 2004-12-05 23:15:01 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class TTCHeader {

    public static final int ttcf = 0x74746366;

    private final int ttcTag;
    private final int version;
    private final int directoryCount;
    private final int[] tableDirectory;
    private int dsigTag;
    private final int dsigLength;
    private final int dsigOffset;

    /** Creates new TTCHeader */
    public TTCHeader(final DataInput di) throws IOException {
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

    public int getTableDirectory(final int i) {
        return tableDirectory[i];
    }

    public static boolean isTTC(final DataInput di) throws IOException {
        final int ttcTag = di.readInt();
        return ttcTag == ttcf;
    }
}
