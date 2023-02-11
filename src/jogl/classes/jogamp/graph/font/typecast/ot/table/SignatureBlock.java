/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- * 
 * This software is published under the terms of the Apache Software License * 
 * version 1.1, a copy of which has been included with this distribution in  * 
 * the LICENSE file.                                                         * 
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

import java.io.IOException;
import java.io.DataInput;

/**
 *
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
class SignatureBlock {

    private int reserved1;
    private int reserved2;
    private int signatureLen;
    private byte[] signature;
    
    /** Creates new SignatureBlock */
    SignatureBlock(DataInput di) throws IOException {
        reserved1 = di.readUnsignedShort();
        reserved2 = di.readUnsignedShort();
        signatureLen = di.readInt();
        signature = new byte[signatureLen];
        di.readFully(signature);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < signatureLen; i += 16) {
            if (signatureLen - i >= 16) {
                sb.append(new String(signature, i, 16)).append("\n");
            } else {
                sb.append(new String(signature, i, signatureLen - i)).append("\n");
            }
        }
        return sb.toString();
    }
}
