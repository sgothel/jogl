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
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class GlyfTable implements Table {

    private final GlyfDescript[] _descript;

    public GlyfTable(
            final DataInput di,
            final int length,
            final MaxpTable maxp,
            final LocaTable loca) throws IOException {
        _descript = new GlyfDescript[maxp.getNumGlyphs()];

        // Buffer the whole table so we can randomly access it
        final byte[] buf = new byte[length];
        di.readFully(buf);
        final ByteArrayInputStream bais = new ByteArrayInputStream(buf);

        // Process all the simple glyphs
        for (int i = 0; i < maxp.getNumGlyphs(); i++) {
            final int len = loca.getOffset(i + 1) - loca.getOffset(i);
            if (len > 0) {
                bais.reset();
                bais.skip(loca.getOffset(i));
                final DataInputStream dis = new DataInputStream(bais);
                final short numberOfContours = dis.readShort();
                if (numberOfContours >= 0) {
                    _descript[i] = new GlyfSimpleDescript(this, i, numberOfContours, dis);
                }
            } else {
                _descript[i] = null;
            }
        }

        // Now do all the composite glyphs
        for (int i = 0; i < maxp.getNumGlyphs(); i++) {
            final int len = loca.getOffset(i + 1) - loca.getOffset(i);
            if (len > 0) {
                bais.reset();
                bais.skip(loca.getOffset(i));
                final DataInputStream dis = new DataInputStream(bais);
                final short numberOfContours = dis.readShort();
                if (numberOfContours < 0) {
                    _descript[i] = new GlyfCompositeDescript(this, i, dis);
                }
            }
        }
    }

    public GlyfDescript getDescription(int i) {
        if (i < _descript.length) {
            return _descript[i];
        } else {
            return null;
        }
    }

}
