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
 * @version $Id: DsigEntry.java,v 1.1.1.1 2004-12-05 23:14:37 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class DsigEntry {

    private final int format;
    private final int length;
    private final int offset;

    /** Creates new DsigEntry */
    protected DsigEntry(final DataInput di) throws IOException {
        format = di.readInt();
        length = di.readInt();
        offset = di.readInt();
    }

    public int getFormat() {
        return format;
    }

    public int getLength() {
        return length;
    }

    public int getOffset() {
        return offset;
    }
}
