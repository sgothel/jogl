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
 * @version $Id: KernSubtable.java,v 1.1.1.1 2004-12-05 23:14:47 davidsch Exp $
 */
public abstract class KernSubtable {

    /** Creates new KernSubtable */
    protected KernSubtable() {
    }
    
    public abstract int getKerningPairCount();

    public abstract KerningPair getKerningPair(int i);

    public static KernSubtable read(DataInput di) throws IOException {
        KernSubtable table = null;
        int version = di.readUnsignedShort();
        int length = di.readUnsignedShort();
        int coverage = di.readUnsignedShort();
        int format = coverage >> 8;
        
        switch (format) {
        case 0:
            table = new KernSubtableFormat0(di);
            break;
        case 2:
            table = new KernSubtableFormat2(di);
            break;
        default:
            break;
        }
        return table;
    }

}
