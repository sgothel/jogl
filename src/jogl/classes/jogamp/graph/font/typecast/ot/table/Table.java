/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included with this distribution in  *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package jogamp.graph.font.typecast.ot.table;

/**
 * @version $Id: Table.java,v 1.1.1.1 2004-12-05 23:14:59 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public interface Table {

    // Table constants
    public static final int BASE = 0x42415345; // Baseline data [OpenType]
    public static final int CFF  = 0x43464620; // PostScript font program (compact font format) [PostScript]
    public static final int DSIG = 0x44534947; // Digital signature
    public static final int EBDT = 0x45424454; // Embedded bitmap data
    public static final int EBLC = 0x45424c43; // Embedded bitmap location data
    public static final int EBSC = 0x45425343; // Embedded bitmap scaling data
    public static final int GDEF = 0x47444546; // Glyph definition data [OpenType]
    public static final int GPOS = 0x47504f53; // Glyph positioning data [OpenType]
    public static final int GSUB = 0x47535542; // Glyph substitution data [OpenType]
    public static final int JSTF = 0x4a535446; // Justification data [OpenType]
    public static final int LTSH = 0x4c545348; // Linear threshold table
    public static final int MMFX = 0x4d4d4658; // Multiple master font metrics [PostScript]
    public static final int MMSD = 0x4d4d5344; // Multiple master supplementary data [PostScript]
    public static final int OS_2 = 0x4f532f32; // OS/2 and Windows specific metrics [r]
    public static final int PCLT = 0x50434c54; // PCL5
    public static final int VDMX = 0x56444d58; // Vertical Device Metrics table
    public static final int cmap = 0x636d6170; // character to glyph mapping [r]
    public static final int cvt  = 0x63767420; // Control Value Table
    public static final int fpgm = 0x6670676d; // font program
    public static final int fvar = 0x66766172; // Apple's font variations table [PostScript]
    public static final int gasp = 0x67617370; // grid-fitting and scan conversion procedure (grayscale)
    public static final int glyf = 0x676c7966; // glyph data [r]
    public static final int hdmx = 0x68646d78; // horizontal device metrics
    public static final int head = 0x68656164; // font header [r]
    public static final int hhea = 0x68686561; // horizontal header [r]
    public static final int hmtx = 0x686d7478; // horizontal metrics [r]
    public static final int kern = 0x6b65726e; // kerning
    public static final int loca = 0x6c6f6361; // index to location [r]
    public static final int maxp = 0x6d617870; // maximum profile [r]
    public static final int name = 0x6e616d65; // naming table [r]
    public static final int prep = 0x70726570; // CVT Program
    public static final int post = 0x706f7374; // PostScript information [r]
    public static final int vhea = 0x76686561; // Vertical Metrics header
    public static final int vmtx = 0x766d7478; // Vertical Metrics

    public static final String notAvailable = "n/a";

    /**
     * Get the table type, as a table directory value.
     * @return The table type
     */
    public int getType();

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    public DirectoryEntry getDirectoryEntry();

}
