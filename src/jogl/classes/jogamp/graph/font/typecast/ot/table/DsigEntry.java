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
class DsigEntry {

    private int format;
    private int length;
    private int offset;
    
    /** Creates new DsigEntry */
    DsigEntry(DataInput di) throws IOException {
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
