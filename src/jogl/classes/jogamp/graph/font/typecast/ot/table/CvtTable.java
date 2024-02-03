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
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class CvtTable implements Table {

    private final short[] values;

    protected CvtTable(DataInput di, int length) throws IOException {
        int len = length / 2;
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

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("'cvt ' Table - Control Value Table\n----------------------------------\n");
        sb.append("Size = ").append(0).append(" bytes, ").append(values.length).append(" entries\n");
        sb.append("        Values\n        ------\n");
        for (int i = 0; i < values.length; i++) {
            sb.append("        ").append(i).append(": ").append(values[i]).append("\n");
        }
        return sb.toString();
    }

}
