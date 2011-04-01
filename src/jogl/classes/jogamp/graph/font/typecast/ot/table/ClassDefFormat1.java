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
 * @version $Id: ClassDefFormat1.java,v 1.1.1.1 2004-12-05 23:14:33 davidsch Exp $
 */
public class ClassDefFormat1 extends ClassDef {

    private int startGlyph;
    private int glyphCount;
    private int[] classValues;

    /** Creates new ClassDefFormat1 */
    public ClassDefFormat1(RandomAccessFile raf) throws IOException {
        startGlyph = raf.readUnsignedShort();
        glyphCount = raf.readUnsignedShort();
        classValues = new int[glyphCount];
        for (int i = 0; i < glyphCount; i++) {
            classValues[i] = raf.readUnsignedShort();
        }
    }

    public int getFormat() {
        return 1;
    }

}
