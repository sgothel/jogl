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

/**
 *
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 * @version $Id: LigatureSubstFormat1.java,v 1.2 2007-01-24 09:47:47 davidsch Exp $
 */
public class LigatureSubstFormat1 extends LigatureSubst {

    private final int _coverageOffset;
    private final int _ligSetCount;
    private final int[] _ligatureSetOffsets;
    private final Coverage _coverage;
    private final LigatureSet[] _ligatureSets;

    /** Creates new LigatureSubstFormat1 */
    protected LigatureSubstFormat1(
            final DataInputStream dis,
            final int offset) throws IOException {
        _coverageOffset = dis.readUnsignedShort();
        _ligSetCount = dis.readUnsignedShort();
        _ligatureSetOffsets = new int[_ligSetCount];
        _ligatureSets = new LigatureSet[_ligSetCount];
        for (int i = 0; i < _ligSetCount; i++) {
            _ligatureSetOffsets[i] = dis.readUnsignedShort();
        }
        dis.reset();
        dis.skipBytes(offset + _coverageOffset);
        _coverage = Coverage.read(dis);
        for (int i = 0; i < _ligSetCount; i++) {
            _ligatureSets[i] = new LigatureSet(dis, offset + _ligatureSetOffsets[i]);
        }
    }

    public int getFormat() {
        return 1;
    }

    @Override
    public String getTypeAsString() {
        return "LigatureSubstFormat1";
    }
}
