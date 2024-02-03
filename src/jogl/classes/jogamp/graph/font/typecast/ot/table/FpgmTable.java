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
import jogamp.graph.font.typecast.ot.Disassembler;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class FpgmTable extends Program implements Table {

    protected FpgmTable(DataInput di, int length) throws IOException {
        readInstructions(di, length);
    }

    @Override
    public int getType() {
        return fpgm;
    }

    public String toString() {
        return Disassembler.disassemble(getInstructions(), 0);
    }
    
}
