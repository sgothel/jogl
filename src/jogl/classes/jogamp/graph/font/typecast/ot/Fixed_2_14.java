/*
 * Copyright (c) 2020 Business Operation Systems GmbH. All Rights Reserved.
 */
package jogamp.graph.font.typecast.ot;

import java.io.DataInput;
import java.io.IOException;

/**
 * Utilities to convert 2.14 fixed floating point format.
 *
 * @author <a href="mailto:haui@haumacher.de">Bernhard Haumacher</a>
 */
public class Fixed_2_14 {

    /** 
     * Reads a value in fixed 2.14 floating point format.
     */
    public static double read(DataInput di) throws IOException {
        return toDouble(di.readShort());
    }

    private static double toDouble(int i) {
        return (double) i / (double) 0x4000;
    }

}
