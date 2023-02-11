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
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public abstract class KernSubtable {
    private final int version;
    private final int length;
    private final int coverage;

    /** Creates new KernSubtable */
    KernSubtable(final int version, final int length, final int coverage) {
        this.version = version;
        this.length = length;
        this.coverage = coverage;
    }

    /** Kern subtable version number */
    public final int getVersion() { return version; }
    /** Length of the subtable in bytes including the header */
    public final int getLength() { return length; }
    /** type of subtable information */
    public final int getCoverage() { return coverage; }
    /** Subtable format, i.e. 0 or 2 is supported here */
    public final int getSubtableFormat() { return coverage >> 8; }
    /** True if table is horizontal data, otherwise vertical */
    public final boolean isHorizontal() { return 0 != ( coverage & 0b0001 ); }
    /** True if table has kerning values, otherwise minimum values */
    public final boolean areKerningValues() { return 0 == ( coverage & 0b0010 ); }
    /** True if kerning is perpendicular to text flow, otherwise along with flow */
    public final boolean isCrossstream() { return 0 != ( coverage & 0b0100 ); }
    /** True if this table shall replace an accumulated value, otherwise keep */
    public final boolean isOverride() { return 0 != ( coverage & 0b1000 ); }

    public abstract int getKerningPairCount();

    public abstract KerningPair getKerningPair(int i);

    public abstract void clearKerningPairs();

    public static KernSubtable read(final DataInput di) throws IOException {
        KernSubtable table = null;
        final int version = di.readUnsignedShort();
        final int length = di.readUnsignedShort();
        final int coverage = di.readUnsignedShort();
        final int format = coverage >> 8;

        switch (format) {
        case 0:
            table = new KernSubtableFormat0(version, length, coverage, di);
            break;
        case 2:
            table = new KernSubtableFormat2(version, length, coverage, di);
            break;
        default:
            break;
        }
        return table;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'kern' Sub-Table\n--------------------------")
          .append("\n    version:  ").append(version)
          .append("\n    length:   ").append(length)
          .append("\n    coverage: 0x").append(Integer.toHexString(coverage)).append("[")
          .append("\n      format: ").append(getSubtableFormat())
          .append("\n      horizontal: ").append(isHorizontal())
          .append("\n      kerningVal: ").append(areKerningValues())
          .append("\n      crossstream: ").append(isCrossstream())
          .append("\n      override: ").append(isOverride()).append("]");
        return sb.toString();
    }
}
