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
class LtshTable implements Table {

    private int version;
    private int numGlyphs;
    private int[] yPels;
    
    /** Creates new LtshTable */
    protected LtshTable(DataInput di) throws IOException {
        version = di.readUnsignedShort();
        numGlyphs = di.readUnsignedShort();
        yPels = new int[numGlyphs];
        for (int i = 0; i < numGlyphs; i++) {
            yPels[i] = di.readUnsignedByte();
        }
    }

    @Override
    public int getType() {
        return LTSH;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
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

}
