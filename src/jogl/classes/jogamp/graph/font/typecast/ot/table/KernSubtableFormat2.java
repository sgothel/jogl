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
 * @version $Id: KernSubtableFormat2.java,v 1.1.1.1 2004-12-05 23:14:48 davidsch Exp $
 */
public class KernSubtableFormat2 extends KernSubtable {

    private final int rowWidth;
    private final int leftClassTable;
    private final int rightClassTable;
    private final int array;

    /** Creates new KernSubtableFormat2 */
    protected KernSubtableFormat2(final DataInput di) throws IOException {
        rowWidth = di.readUnsignedShort();
        leftClassTable = di.readUnsignedShort();
        rightClassTable = di.readUnsignedShort();
        array = di.readUnsignedShort();
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
