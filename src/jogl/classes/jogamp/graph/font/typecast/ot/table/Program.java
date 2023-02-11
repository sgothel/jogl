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
abstract class Program {

    private short[] instructions;

    short[] getInstructions() {
        return instructions;
    }

    void readInstructions(DataInput di, int count) throws IOException {
        instructions = new short[count];
        for (int i = 0; i < count; i++) {
            instructions[i] = (short) di.readUnsignedByte();
        }
    }
/*
    protected void readInstructions(ByteArrayInputStream bais, int count) {
        instructions = new short[count];
        for (int i = 0; i < count; i++) {
            instructions[i] = (short) bais.read();
        }
    }
*/
}
