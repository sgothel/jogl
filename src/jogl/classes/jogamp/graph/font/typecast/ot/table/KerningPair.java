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
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: KerningPair.java,v 1.1.1.1 2004-12-05 23:14:47 davidsch Exp $
 */
public class KerningPair {

    private final int left; // uint16
    private final int right; // uint16
    private final short value; // sint16 in FUnits

    /** Creates new KerningPair */
    protected KerningPair(final DataInput di) throws IOException {
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
