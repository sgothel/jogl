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
 * @version $Id: Program.java,v 1.1.1.1 2004-12-05 23:14:57 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public abstract class Program {

    private short[] instructions;

    public short[] getInstructions() {
        return instructions;
    }

    protected void readInstructions(final DataInput di, final int count) throws IOException {
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
