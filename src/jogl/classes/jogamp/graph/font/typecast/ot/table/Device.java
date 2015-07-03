/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

import java.io.IOException;
import java.io.RandomAccessFile;

/**
 *
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: Device.java,v 1.1.1.1 2004-12-05 23:14:37 davidsch Exp $
 */
public class Device extends Object {

    private final int startSize;
    private final int endSize;
    private final int deltaFormat;
    private final int[] deltaValues;

    /** Creates new Device */
    public Device(final RandomAccessFile raf) throws IOException {
        startSize = raf.readUnsignedShort();
        endSize = raf.readUnsignedShort();
        deltaFormat = raf.readUnsignedShort();
        int size = startSize - endSize;
        switch (deltaFormat) {
        case 1:
            size = (size % 8 == 0) ? size / 8 : size / 8 + 1;
            break;
        case 2:
            size = (size % 4 == 0) ? size / 4 : size / 4 + 1;
            break;
        case 3:
            size = (size % 2 == 0) ? size / 2 : size / 2 + 1;
            break;
        }
        deltaValues = new int[size];
        for (int i = 0; i < size; i++) {
            deltaValues[i] = raf.readUnsignedShort();
        }
    }


}
