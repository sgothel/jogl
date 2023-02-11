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
public class KernSubtableFormat2 extends KernSubtable {

    @SuppressWarnings("unused")
    private final int rowWidth;
    @SuppressWarnings("unused")
    private final int leftClassTable;
    @SuppressWarnings("unused")
    private final int rightClassTable;
    @SuppressWarnings("unused")
    private final int array;

    /** Creates new KernSubtableFormat2 */
    KernSubtableFormat2(final int version, final int length, final int coverage, final DataInput di) throws IOException {
        super(version, length, coverage);
        rowWidth = di.readUnsignedShort();
        leftClassTable = di.readUnsignedShort();
        rightClassTable = di.readUnsignedShort();
        array = di.readUnsignedShort();
    }

    @Override
    public void clearKerningPairs() {
    }

    @Override
    public int getKerningPairCount() {
        return 0;
    }

    @Override
    public KerningPair getKerningPair(final int i) {
        return null;
    }

}
