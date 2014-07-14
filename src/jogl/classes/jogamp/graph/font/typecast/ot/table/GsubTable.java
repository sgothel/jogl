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

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 *
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: GsubTable.java,v 1.3 2007-01-24 09:47:46 davidsch Exp $
 */
public class GsubTable implements Table, LookupSubtableFactory {

    private final DirectoryEntry _de;
    private final ScriptList _scriptList;
    private final FeatureList _featureList;
    private final LookupList _lookupList;

    protected GsubTable(final DirectoryEntry de, final DataInput di) throws IOException {
        _de = (DirectoryEntry) de.clone();

        // Load into a temporary buffer, and create another input stream
        final byte[] buf = new byte[de.getLength()];
        di.readFully(buf);
        final DataInputStream dis = new DataInputStream(new ByteArrayInputStream(buf));

        // GSUB Header
        /* final int version = */ dis.readInt();
        final int scriptListOffset = dis.readUnsignedShort();
        final int featureListOffset = dis.readUnsignedShort();
        final int lookupListOffset = dis.readUnsignedShort();

        // Script List
        _scriptList = new ScriptList(dis, scriptListOffset);

        // Feature List
        _featureList = new FeatureList(dis, featureListOffset);

        // Lookup List
        _lookupList = new LookupList(dis, lookupListOffset, this);
    }

    /**
     * 1 - Single - Replace one glyph with one glyph
     * 2 - Multiple - Replace one glyph with more than one glyph
     * 3 - Alternate - Replace one glyph with one of many glyphs
     * 4 - Ligature - Replace multiple glyphs with one glyph
     * 5 - Context - Replace one or more glyphs in context
     * 6 - Chaining - Context Replace one or more glyphs in chained context
     */
    @Override
    public LookupSubtable read(
            final int type,
            final DataInputStream dis,
            final int offset) throws IOException {
        LookupSubtable s = null;
        switch (type) {
        case 1:
            s = SingleSubst.read(dis, offset);
            break;
        case 2:
//            s = MultipleSubst.read(dis, offset);
            break;
        case 3:
//            s = AlternateSubst.read(dis, offset);
            break;
        case 4:
            s = LigatureSubst.read(dis, offset);
            break;
        case 5:
//            s = ContextSubst.read(dis, offset);
            break;
        case 6:
//            s = ChainingSubst.read(dis, offset);
            break;
        }
        return s;
    }

    /** Get the table type, as a table directory value.
     * @return The table type
     */
    @Override
    public int getType() {
        return GSUB;
    }

    public ScriptList getScriptList() {
        return _scriptList;
    }

    public FeatureList getFeatureList() {
        return _featureList;
    }

    public LookupList getLookupList() {
        return _lookupList;
    }

    @Override
    public String toString() {
        return "GSUB";
    }

    public static String lookupTypeAsString(final int type) {
        switch (type) {
        case 1:
            return "Single";
        case 2:
            return "Multiple";
        case 3:
            return "Alternate";
        case 4:
            return "Ligature";
        case 5:
            return "Context";
        case 6:
            return "Chaining";
        }
        return "Unknown";
    }

    /**
     * Get a directory entry for this table.  This uniquely identifies the
     * table in collections where there may be more than one instance of a
     * particular table.
     * @return A directory entry
     */
    @Override
    public DirectoryEntry getDirectoryEntry() {
        return _de;
    }

}
