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
 * @version $Id: MaxpTable.java,v 1.1.1.1 2004-12-05 23:14:52 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class MaxpTable implements Table {

    private final DirectoryEntry de;
    private final int versionNumber;
    private int numGlyphs;
    private int maxPoints;
    private int maxContours;
    private int maxCompositePoints;
    private int maxCompositeContours;
    private int maxZones;
    private int maxTwilightPoints;
    private int maxStorage;
    private int maxFunctionDefs;
    private int maxInstructionDefs;
    private int maxStackElements;
    private int maxSizeOfInstructions;
    private int maxComponentElements;
    private int maxComponentDepth;

    protected MaxpTable(final DirectoryEntry de, final DataInput di) throws IOException {
        this.de = (DirectoryEntry) de.clone();
        versionNumber = di.readInt();

        // CFF fonts use version 0.5, TrueType fonts use version 1.0
        if (versionNumber == 0x00005000) {
            numGlyphs = di.readUnsignedShort();
        } else if (versionNumber == 0x00010000) {
            numGlyphs = di.readUnsignedShort();
            maxPoints = di.readUnsignedShort();
            maxContours = di.readUnsignedShort();
            maxCompositePoints = di.readUnsignedShort();
            maxCompositeContours = di.readUnsignedShort();
            maxZones = di.readUnsignedShort();
            maxTwilightPoints = di.readUnsignedShort();
            maxStorage = di.readUnsignedShort();
            maxFunctionDefs = di.readUnsignedShort();
            maxInstructionDefs = di.readUnsignedShort();
            maxStackElements = di.readUnsignedShort();
            maxSizeOfInstructions = di.readUnsignedShort();
            maxComponentElements = di.readUnsignedShort();
            maxComponentDepth = di.readUnsignedShort();
        }
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public int getMaxComponentDepth() {
        return maxComponentDepth;
    }

    public int getMaxComponentElements() {
        return maxComponentElements;
    }

    public int getMaxCompositeContours() {
        return maxCompositeContours;
    }

    public int getMaxCompositePoints() {
        return maxCompositePoints;
    }

    public int getMaxContours() {
        return maxContours;
    }

    public int getMaxFunctionDefs() {
        return maxFunctionDefs;
    }

    public int getMaxInstructionDefs() {
        return maxInstructionDefs;
    }

    public int getMaxPoints() {
        return maxPoints;
    }

    public int getMaxSizeOfInstructions() {
        return maxSizeOfInstructions;
    }

    public int getMaxStackElements() {
        return maxStackElements;
    }

    public int getMaxStorage() {
        return maxStorage;
    }

    public int getMaxTwilightPoints() {
        return maxTwilightPoints;
    }

    public int getMaxZones() {
        return maxZones;
    }

    public int getNumGlyphs() {
        return numGlyphs;
    }

    @Override
    public int getType() {
        return maxp;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("'maxp' Table - Maximum Profile\n------------------------------")
            .append("\n        'maxp' version:         ").append(Fixed.floatValue(versionNumber))
            .append("\n        numGlyphs:              ").append(numGlyphs);
        if (versionNumber == 0x00010000) {
            sb.append("\n        maxPoints:              ").append(maxPoints)
                .append("\n        maxContours:            ").append(maxContours)
                .append("\n        maxCompositePoints:     ").append(maxCompositePoints)
                .append("\n        maxCompositeContours:   ").append(maxCompositeContours)
                .append("\n        maxZones:               ").append(maxZones)
                .append("\n        maxTwilightPoints:      ").append(maxTwilightPoints)
                .append("\n        maxStorage:             ").append(maxStorage)
                .append("\n        maxFunctionDefs:        ").append(maxFunctionDefs)
                .append("\n        maxInstructionDefs:     ").append(maxInstructionDefs)
                .append("\n        maxStackElements:       ").append(maxStackElements)
                .append("\n        maxSizeOfInstructions:  ").append(maxSizeOfInstructions)
                .append("\n        maxComponentElements:   ").append(maxComponentElements)
                .append("\n        maxComponentDepth:      ").append(maxComponentDepth);
        } else {
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    @Override
    public DirectoryEntry getDirectoryEntry() {
        return de;
    }

}
