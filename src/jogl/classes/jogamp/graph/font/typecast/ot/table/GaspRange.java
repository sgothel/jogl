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
 * @version $Id: GaspRange.java,v 1.1.1.1 2004-12-05 23:14:38 davidsch Exp $
 */
public class GaspRange {

    public static final int GASP_GRIDFIT = 1;
    public static final int GASP_DOGRAY = 2;
    
    private int rangeMaxPPEM;
    private int rangeGaspBehavior;
    
    /** Creates new GaspRange */
    protected GaspRange(DataInput di) throws IOException {
        rangeMaxPPEM = di.readUnsignedShort();
        rangeGaspBehavior = di.readUnsignedShort();
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("  rangeMaxPPEM:        ").append(rangeMaxPPEM)
            .append("\n  rangeGaspBehavior:   0x").append(rangeGaspBehavior);
        if ((rangeGaspBehavior & GASP_GRIDFIT) != 0) {
            sb.append("- GASP_GRIDFIT ");
        }
        if ((rangeGaspBehavior & GASP_DOGRAY) != 0) {
            sb.append("- GASP_DOGRAY");
        }
        return sb.toString();
    }
}
