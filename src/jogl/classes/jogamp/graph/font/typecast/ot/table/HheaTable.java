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
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class HheaTable implements Table {

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

    public HheaTable(DataInput di) throws IOException {
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

    public short getXMaxExtent() {
        return xMaxExtent;
    }

    public String toString() {
        return "'hhea' Table - Horizontal Header\n--------------------------------" +
                "\n        'hhea' version:       " + Fixed.floatValue(version) +
                "\n        yAscender:            " + ascender +
                "\n        yDescender:           " + descender +
                "\n        yLineGap:             " + lineGap +
                "\n        advanceWidthMax:      " + advanceWidthMax +
                "\n        minLeftSideBearing:   " + minLeftSideBearing +
                "\n        minRightSideBearing:  " + minRightSideBearing +
                "\n        xMaxExtent:           " + xMaxExtent +
                "\n        horizCaretSlopeNum:   " + caretSlopeRise +
                "\n        horizCaretSlopeDenom: " + caretSlopeRun +
                "\n        reserved0:            0" +
                "\n        reserved1:            0" +
                "\n        reserved2:            0" +
                "\n        reserved3:            0" +
                "\n        reserved4:            0" +
                "\n        metricDataFormat:     " + metricDataFormat +
                "\n        numOf_LongHorMetrics: " + numberOfHMetrics;
    }

}
