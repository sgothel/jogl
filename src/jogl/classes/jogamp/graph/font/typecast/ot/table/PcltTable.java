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
class PcltTable implements Table {

    private int version;
    private long fontNumber;
    private int pitch;
    private int xHeight;
    private int style;
    private int typeFamily;
    private int capHeight;
    private int symbolSet;
    private final char[] typeface = new char[16];
    private final short[] characterComplement = new short[8];
    private final char[] fileName = new char[6];
    private short strokeWeight;
    private short widthType;
    private byte serifStyle;
    private byte reserved;

    /** Creates new PcltTable */
    protected PcltTable(DataInput di) throws IOException {
        version = di.readInt();
        fontNumber = di.readInt();
        pitch = di.readUnsignedShort();
        xHeight = di.readUnsignedShort();
        style = di.readUnsignedShort();
        typeFamily = di.readUnsignedShort();
        capHeight = di.readUnsignedShort();
        symbolSet = di.readUnsignedShort();
        for (int i = 0; i < 16; i++) {
            typeface[i] = (char) di.readUnsignedByte();
        }
        for (int i = 0; i < 8; i++) {
            characterComplement[i] = (short) di.readUnsignedByte();
        }
        for (int i = 0; i < 6; i++) {
            fileName[i] = (char) di.readUnsignedByte();
        }
        strokeWeight = (short) di.readUnsignedByte();
        widthType = (short) di.readUnsignedByte();
        serifStyle = di.readByte();
        reserved = di.readByte();
    }

    public String toString() {
        return "'PCLT' Table - Printer Command Language Table\n---------------------------------------------" +
                "\n        version:             0x" + Integer.toHexString(version).toUpperCase() +
                "\n        fontNumber:          " + fontNumber + " (0x" + Long.toHexString(fontNumber).toUpperCase() +
                ")\n        pitch:               " + pitch +
                "\n        xHeight:             " + xHeight +
                "\n        style:               0x" + style +
                "\n        typeFamily:          0x" + (typeFamily >> 12) +
                " " + (typeFamily & 0xfff) +
                "\n        capHeight:           " + capHeight +
                "\n        symbolSet:           " + symbolSet +
                "\n        typeFace:            " + new String(typeface) +
                "\n        characterComplement  0x" +
                Integer.toHexString(characterComplement[0]).toUpperCase() +
                "\n        fileName:            " + new String(fileName) +
                "\n        strokeWeight:        " + strokeWeight +
                "\n        widthType:           " + widthType +
                "\n        serifStyle:          " + serifStyle +
                "\n        reserved:            " + reserved;
    }

}
