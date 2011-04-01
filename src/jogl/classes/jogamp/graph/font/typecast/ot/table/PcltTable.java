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
 * @version $Id: PcltTable.java,v 1.1.1.1 2004-12-05 23:14:54 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class PcltTable implements Table {

    private DirectoryEntry de;
    private int version;
    private long fontNumber;
    private int pitch;
    private int xHeight;
    private int style;
    private int typeFamily;
    private int capHeight;
    private int symbolSet;
    private char[] typeface = new char[16];
    private short[] characterComplement = new short[8];
    private char[] fileName = new char[6];
    private short strokeWeight;
    private short widthType;
    private byte serifStyle;
    private byte reserved;

    /** Creates new PcltTable */
    protected PcltTable(DirectoryEntry de, DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
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

    /**
     * Get the table type, as a table directory value.
     * @return The table type
     */
    public int getType() {
        return PCLT;
    }
    
    public String toString() {
        return new StringBuffer()
            .append("'PCLT' Table - Printer Command Language Table\n---------------------------------------------")
            .append("\n        version:             0x").append(Integer.toHexString(version).toUpperCase())
            .append("\n        fontNumber:          ").append(fontNumber).append(" (0x").append(Long.toHexString(fontNumber).toUpperCase())
            .append(")\n        pitch:               ").append(pitch)
            .append("\n        xHeight:             ").append(xHeight)
            .append("\n        style:               0x").append(style)
            .append("\n        typeFamily:          0x").append(typeFamily>>12)
            .append(" ").append(typeFamily & 0xfff)
            .append("\n        capHeight:           ").append(capHeight)
            .append("\n        symbolSet:           ").append(symbolSet)
            .append("\n        typeFace:            ").append(new String(typeface))
            .append("\n        characterComplement  0x")
            .append(Integer.toHexString(characterComplement[0]).toUpperCase())
            .append("\n        fileName:            ").append(new String(fileName))
            .append("\n        strokeWeight:        ").append(strokeWeight)
            .append("\n        widthType:           ").append(widthType)
            .append("\n        serifStyle:          ").append(serifStyle)
            .append("\n        reserved:            ").append(reserved)
            .toString();
    }
    
    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry() {
        return de;
    }
    
}
