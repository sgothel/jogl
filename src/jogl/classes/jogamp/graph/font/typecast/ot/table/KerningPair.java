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
public class KerningPair {

    private final int left; // uint16
    private final int right; // uint16
    private final short value; // sint16 in FUnits

    /** Creates new KerningPair */
    KerningPair(final DataInput di) throws IOException {
        left = di.readUnsignedShort();
        right = di.readUnsignedShort();
        value = di.readShort();
    }

    /** left glyph index */
    public int getLeft() {
        return left;
    }

    /** right glyph index */
    public int getRight() {
        return right;
    }

    /** sint16 in FUnits between left and right glyph within a word */
    public short getValue() {
        return value;
    }

}
