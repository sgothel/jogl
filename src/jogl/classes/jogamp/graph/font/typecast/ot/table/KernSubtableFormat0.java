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
 * @version $Id: KernSubtableFormat0.java,v 1.1.1.1 2004-12-05 23:14:48 davidsch Exp $
 */
public class KernSubtableFormat0 extends KernSubtable {
    
    private int nPairs;
    private int searchRange;
    private int entrySelector;
    private int rangeShift;
    private KerningPair[] kerningPairs;

    /** Creates new KernSubtableFormat0 */
    protected KernSubtableFormat0(DataInput di) throws IOException {
        nPairs = di.readUnsignedShort();
        searchRange = di.readUnsignedShort();
        entrySelector = di.readUnsignedShort();
        rangeShift = di.readUnsignedShort();
        kerningPairs = new KerningPair[nPairs];
        for (int i = 0; i < nPairs; i++) {
            kerningPairs[i] = new KerningPair(di);
        }
    }

    public int getKerningPairCount() {
        return nPairs;
    }

    public KerningPair getKerningPair(int i) {
        return kerningPairs[i];
    }

}
