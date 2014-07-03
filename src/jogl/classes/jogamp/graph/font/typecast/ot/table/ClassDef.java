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
 * @version $Id: ClassDef.java,v 1.1.1.1 2004-12-05 23:14:33 davidsch Exp $
 */
public abstract class ClassDef {

    public abstract int getFormat();

    protected static ClassDef read(final RandomAccessFile raf) throws IOException {
        ClassDef c = null;
        final int format = raf.readUnsignedShort();
        if (format == 1) {
            c = new ClassDefFormat1(raf);
        } else if (format == 2) {
            c = new ClassDefFormat2(raf);
        }
        return c;
    }
}
