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
 * @version $Id: SignatureBlock.java,v 1.1.1.1 2004-12-05 23:14:58 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class SignatureBlock {

    private final int reserved1;
    private final int reserved2;
    private final int signatureLen;
    private final byte[] signature;

    /** Creates new SignatureBlock */
    protected SignatureBlock(final DataInput di) throws IOException {
        reserved1 = di.readUnsignedShort();
        reserved2 = di.readUnsignedShort();
        signatureLen = di.readInt();
        signature = new byte[signatureLen];
        di.readFully(signature);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
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
