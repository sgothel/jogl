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

import jogamp.graph.font.typecast.ot.Fixed;

/**
 * @version $Id: HheaTable.java,v 1.2 2010-08-10 11:44:02 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class HheaTable implements Table {

    private DirectoryEntry de;
    private int version;
    private short ascender;
    private short descender;
    private short lineGap;
    private short advanceWidthMax;
    private short minLeftSideBearing;
    private short minRightSideBearing;
    private short xMaxExtent;
    private short caretSlopeRise;
    private short caretSlopeRun;
    private short metricDataFormat;
    private int numberOfHMetrics;

    protected HheaTable(DirectoryEntry de, DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
        version = di.readInt();
        ascender = di.readShort();
        descender = di.readShort();
        lineGap = di.readShort();
        advanceWidthMax = di.readShort();
        minLeftSideBearing = di.readShort();
        minRightSideBearing = di.readShort();
        xMaxExtent = di.readShort();
        caretSlopeRise = di.readShort();
        caretSlopeRun = di.readShort();
        for (int i = 0; i < 5; i++) {
            di.readShort();
        }
        metricDataFormat = di.readShort();
        numberOfHMetrics = di.readUnsignedShort();
    }

    public short getAdvanceWidthMax() {
        return advanceWidthMax;
    }

    public short getAscender() {
        return ascender;
    }

    public short getCaretSlopeRise() {
        return caretSlopeRise;
    }

    public short getCaretSlopeRun() {
        return caretSlopeRun;
    }

    public short getDescender() {
        return descender;
    }

    public short getLineGap() {
        return lineGap;
    }

    public short getMetricDataFormat() {
        return metricDataFormat;
    }

    public short getMinLeftSideBearing() {
        return minLeftSideBearing;
    }

    public short getMinRightSideBearing() {
        return minRightSideBearing;
    }

    public int getNumberOfHMetrics() {
        return numberOfHMetrics;
    }

    public int getType() {
        return hhea;
    }

    public short getXMaxExtent() {
        return xMaxExtent;
    }

    public String toString() {
        return new StringBuffer()
            .append("'hhea' Table - Horizontal Header\n--------------------------------")
            .append("\n        'hhea' version:       ").append(Fixed.floatValue(version))
            .append("\n        yAscender:            ").append(ascender)
            .append("\n        yDescender:           ").append(descender)
            .append("\n        yLineGap:             ").append(lineGap)
            .append("\n        advanceWidthMax:      ").append(advanceWidthMax)
            .append("\n        minLeftSideBearing:   ").append(minLeftSideBearing)
            .append("\n        minRightSideBearing:  ").append(minRightSideBearing)
            .append("\n        xMaxExtent:           ").append(xMaxExtent)
            .append("\n        horizCaretSlopeNum:   ").append(caretSlopeRise)
            .append("\n        horizCaretSlopeDenom: ").append(caretSlopeRun)
            .append("\n        reserved0:            0")
            .append("\n        reserved1:            0")
            .append("\n        reserved2:            0")
            .append("\n        reserved3:            0")
            .append("\n        reserved4:            0")
            .append("\n        metricDataFormat:     ").append(metricDataFormat)
            .append("\n        numOf_LongHorMetrics: ").append(numberOfHMetrics)
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
