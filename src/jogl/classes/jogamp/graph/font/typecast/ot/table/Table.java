/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2016 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jogamp.graph.font.typecast.ot.table;

import java.io.IOException;
import java.io.Writer;

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public interface Table {

    // Table constants
    int BASE = 0x42415345; // Baseline data [OpenType]
    int CFF  = 0x43464620; // PostScript font program (compact font format) [PostScript]
    int COLR = 0x434f4c52; // Color Table
    int CPAL = 0x4350414c; // Color Palette Table
    int DSIG = 0x44534947; // Digital signature
    int EBDT = 0x45424454; // Embedded bitmap data
    int EBLC = 0x45424c43; // Embedded bitmap location data
    int EBSC = 0x45425343; // Embedded bitmap scaling data
    int GDEF = 0x47444546; // Glyph definition data [OpenType]
    int GPOS = 0x47504f53; // Glyph positioning data [OpenType]
    int GSUB = 0x47535542; // Glyph substitution data [OpenType]
    int JSTF = 0x4a535446; // Justification data [OpenType]
    int LTSH = 0x4c545348; // Linear threshold table
    int MMFX = 0x4d4d4658; // Multiple master font metrics [PostScript]
    int MMSD = 0x4d4d5344; // Multiple master supplementary data [PostScript]
    int OS_2 = 0x4f532f32; // OS/2 and Windows specific metrics [r]
    int PCLT = 0x50434c54; // PCL5
    int VDMX = 0x56444d58; // Vertical Device Metrics table
    int cmap = 0x636d6170; // character to glyph mapping [r]
    int cvt  = 0x63767420; // Control Value Table
    int fpgm = 0x6670676d; // font program
    int fvar = 0x66766172; // Apple's font variations table [PostScript]
    int gasp = 0x67617370; // grid-fitting and scan conversion procedure (grayscale)
    int glyf = 0x676c7966; // glyph data [r]
    int hdmx = 0x68646d78; // horizontal device metrics
    int head = 0x68656164; // font header [r]
    int hhea = 0x68686561; // horizontal header [r]
    int hmtx = 0x686d7478; // horizontal metrics [r]
    int kern = 0x6b65726e; // kerning
    int loca = 0x6c6f6361; // index to location [r]
    int maxp = 0x6d617870; // maximum profile [r]
    int name = 0x6e616d65; // naming table [r]
    int prep = 0x70726570; // CVT Program
    int post = 0x706f7374; // PostScript information [r]
    int sbix = 0x73626978; // Extended Bitmaps
    int vhea = 0x76686561; // Vertical Metrics header
    int vmtx = 0x766d7478; // Vertical Metrics
    int svg  = TableDirectory.fromStringTag("SVG "); // SVG outlines
    
    /**
     * The type code of this {@link Table}.
     */
    int getType();

    /** 
     * Writes full debug information to the given writer.
     */
    default void dump(Writer out) throws IOException {
        out.write(toString());
    }
}
