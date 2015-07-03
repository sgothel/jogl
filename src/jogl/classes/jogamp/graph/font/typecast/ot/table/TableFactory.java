/*

 ============================================================================
                   The Apache Software License, Version 1.1
 ============================================================================

 Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.

 Redistribution and use in source and binary forms, with or without modifica-
 tion, are permitted provided that the following conditions are met:

 1. Redistributions of  source code must  retain the above copyright  notice,
    this list of conditions and the following disclaimer.

 2. Redistributions in binary form must reproduce the above copyright notice,
    this list of conditions and the following disclaimer in the documentation
    and/or other materials provided with the distribution.

 3. The end-user documentation included with the redistribution, if any, must
    include  the following  acknowledgment:  "This product includes  software
    developed  by the  Apache Software Foundation  (http://www.apache.org/)."
    Alternately, this  acknowledgment may  appear in the software itself,  if
    and wherever such third-party acknowledgments normally appear.

 4. The names "Batik" and  "Apache Software Foundation" must  not  be
    used to  endorse or promote  products derived from  this software without
    prior written permission. For written permission, please contact
    apache@apache.org.

 5. Products  derived from this software may not  be called "Apache", nor may
    "Apache" appear  in their name,  without prior written permission  of the
    Apache Software Foundation.

 THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 FITNESS  FOR A PARTICULAR  PURPOSE ARE  DISCLAIMED.  IN NO  EVENT SHALL  THE
 APACHE SOFTWARE  FOUNDATION  OR ITS CONTRIBUTORS  BE LIABLE FOR  ANY DIRECT,
 INDIRECT, INCIDENTAL, SPECIAL,  EXEMPLARY, OR CONSEQUENTIAL  DAMAGES (INCLU-
 DING, BUT NOT LIMITED TO, PROCUREMENT  OF SUBSTITUTE GOODS OR SERVICES; LOSS
 OF USE, DATA, OR  PROFITS; OR BUSINESS  INTERRUPTION)  HOWEVER CAUSED AND ON
 ANY  THEORY OF LIABILITY,  WHETHER  IN CONTRACT,  STRICT LIABILITY,  OR TORT
 (INCLUDING  NEGLIGENCE OR  OTHERWISE) ARISING IN  ANY WAY OUT OF THE  USE OF
 THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 This software  consists of voluntary contributions made  by many individuals
 on  behalf of the Apache Software  Foundation. For more  information on the
 Apache Software Foundation, please see <http://www.apache.org/>.

*/

package jogamp.graph.font.typecast.ot.table;

import java.io.DataInputStream;
import java.io.IOException;

import jogamp.graph.font.typecast.ot.OTFont;
import jogamp.graph.font.typecast.ot.OTFontCollection;

/**
 *
 * @version $Id: TableFactory.java,v 1.7 2007-02-05 12:39:51 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class TableFactory {

    public static Table create(
            final OTFontCollection fc,
            final OTFont font,
            final DirectoryEntry de,
            final DataInputStream dis) throws IOException {
        Table t = null;

        // First, if we have a font collection, look for the table there
        if (fc != null) {
            t = fc.getTable(de);
            if (t != null) {
                return t;
            }
        }

        // Create the table
        switch (de.getTag()) {
        case Table.BASE:
            t = new BaseTable(de, dis);
            break;
        case Table.CFF:
            t = new CffTable(de, dis);
            break;
        case Table.DSIG:
            t = new DsigTable(de, dis);
            break;
        case Table.EBDT:
            break;
        case Table.EBLC:
            break;
        case Table.EBSC:
            break;
        case Table.GDEF:
            break;
        case Table.GPOS:
            t = new GposTable(de, dis);
            break;
        case Table.GSUB:
            t = new GsubTable(de, dis);
            break;
        case Table.JSTF:
            break;
        case Table.LTSH:
            t = new LtshTable(de, dis);
            break;
        case Table.MMFX:
            break;
        case Table.MMSD:
            break;
        case Table.OS_2:
            t = new Os2Table(de, dis);
            break;
        case Table.PCLT:
            t = new PcltTable(de, dis);
            break;
        case Table.VDMX:
            t = new VdmxTable(de, dis);
            break;
        case Table.cmap:
            t = new CmapTable(de, dis);
            break;
        case Table.cvt:
            t = new CvtTable(de, dis);
            break;
        case Table.fpgm:
            t = new FpgmTable(de, dis);
            break;
        case Table.fvar:
            break;
        case Table.gasp:
            t = new GaspTable(de, dis);
            break;
        case Table.glyf:
            t = new GlyfTable(de, dis, font.getMaxpTable(), font.getLocaTable());
            break;
        case Table.hdmx:
            t = new HdmxTable(de, dis, font.getMaxpTable());
            break;
        case Table.head:
            t = new HeadTable(de, dis);
            break;
        case Table.hhea:
            t = new HheaTable(de, dis);
            break;
        case Table.hmtx:
            t = new HmtxTable(de, dis, font.getHheaTable(), font.getMaxpTable());
            break;
        case Table.kern:
            t = new KernTable(de, dis);
            break;
        case Table.loca:
            t = new LocaTable(de, dis, font.getHeadTable(), font.getMaxpTable());
            break;
        case Table.maxp:
            t = new MaxpTable(de, dis);
            break;
        case Table.name:
            t = new NameTable(de, dis);
            break;
        case Table.prep:
            t = new PrepTable(de, dis);
            break;
        case Table.post:
            t = new PostTable(de, dis);
            break;
        case Table.vhea:
            t = new VheaTable(de, dis);
            break;
        case Table.vmtx:
            t = new VmtxTable(de, dis, font.getVheaTable(), font.getMaxpTable());
            break;
        }

        // If we have a font collection, add this table to it
        if ((fc != null) && (t != null)) {
            fc.addTable(t);
        }
        return t;
    }
}
