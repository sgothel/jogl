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

/**
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public interface Table {

    // Table constants
    static final int BASE = 0x42415345; // Baseline data [OpenType]
    static final int CFF  = 0x43464620; // PostScript font program (compact font format) [PostScript]
    static final int COLR = 0x434f4c52; // Color Table
    static final int CPAL = 0x4350414c; // Color Palette Table
    static final int DSIG = 0x44534947; // Digital signature
    static final int EBDT = 0x45424454; // Embedded bitmap data
    static final int EBLC = 0x45424c43; // Embedded bitmap location data
    static final int EBSC = 0x45425343; // Embedded bitmap scaling data
    static final int GDEF = 0x47444546; // Glyph definition data [OpenType]
    static final int GPOS = 0x47504f53; // Glyph positioning data [OpenType]
    static final int GSUB = 0x47535542; // Glyph substitution data [OpenType]
    static final int JSTF = 0x4a535446; // Justification data [OpenType]
    static final int LTSH = 0x4c545348; // Linear threshold table
    static final int MMFX = 0x4d4d4658; // Multiple master font metrics [PostScript]
    static final int MMSD = 0x4d4d5344; // Multiple master supplementary data [PostScript]
    static final int OS_2 = 0x4f532f32; // OS/2 and Windows specific metrics [r]
    static final int PCLT = 0x50434c54; // PCL5
    static final int VDMX = 0x56444d58; // Vertical Device Metrics table
    static final int cmap = 0x636d6170; // character to glyph mapping [r]
    static final int cvt  = 0x63767420; // Control Value Table
    static final int fpgm = 0x6670676d; // font program
    static final int fvar = 0x66766172; // Apple's font variations table [PostScript]
    static final int gasp = 0x67617370; // grid-fitting and scan conversion procedure (grayscale)
    static final int glyf = 0x676c7966; // glyph data [r]
    static final int hdmx = 0x68646d78; // horizontal device metrics
    static final int head = 0x68656164; // font header [r]
    static final int hhea = 0x68686561; // horizontal header [r]
    static final int hmtx = 0x686d7478; // horizontal metrics [r]
    static final int kern = 0x6b65726e; // kerning
    static final int loca = 0x6c6f6361; // index to location [r]
    static final int maxp = 0x6d617870; // maximum profile [r]
    static final int name = 0x6e616d65; // naming table [r]
    static final int prep = 0x70726570; // CVT Program
    static final int post = 0x706f7374; // PostScript information [r]
    static final int sbix = 0x73626978; // Extended Bitmaps
    static final int vhea = 0x76686561; // Vertical Metrics header
    static final int vmtx = 0x766d7478; // Vertical Metrics

    /**
     * The type code of this {@link Table}.
     */
    int getType();
}
