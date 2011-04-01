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
 * @version $Id: ClassDefFormat2.java,v 1.1.1.1 2004-12-05 23:14:33 davidsch Exp $
 */
public class ClassDefFormat2 extends ClassDef {

    private int classRangeCount;
    private RangeRecord[] classRangeRecords;

    /** Creates new ClassDefFormat2 */
    public ClassDefFormat2(RandomAccessFile raf) throws IOException {
        classRangeCount = raf.readUnsignedShort();
        classRangeRecords = new RangeRecord[classRangeCount];
        for (int i = 0; i < classRangeCount; i++) {
            classRangeRecords[i] = new RangeRecord(raf);
        }
    }

    public int getFormat() {
        return 2;
    }

}
