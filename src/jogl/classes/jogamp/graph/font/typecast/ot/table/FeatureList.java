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
 * @version $Id: FeatureList.java,v 1.3 2007-01-24 09:54:44 davidsch Exp $
 */
public class FeatureList {

    private final int _featureCount;
    private final FeatureRecord[] _featureRecords;
    private final Feature[] _features;

    /** Creates new FeatureList */
    public FeatureList(final DataInputStream dis, final int offset) throws IOException {

        // Ensure we're in the right place
        dis.reset();
        dis.skipBytes(offset);

        // Start reading
        _featureCount = dis.readUnsignedShort();
        _featureRecords = new FeatureRecord[_featureCount];
        _features = new Feature[_featureCount];
        for (int i = 0; i < _featureCount; i++) {
            _featureRecords[i] = new FeatureRecord(dis);
        }
        for (int i = 0; i < _featureCount; i++) {
            dis.reset();
            dis.skipBytes(offset + _featureRecords[i].getOffset());
            _features[i] = new Feature(dis);
        }
    }

    public int getFeatureCount() {
        return _featureCount;
    }

    public FeatureRecord getFeatureRecord(final int i) {
        return _featureRecords[i];
    }

    public Feature getFeature(final int i) {
        return _features[i];
    }

    public Feature findFeature(final LangSys langSys, final String tag) {
        if (tag.length() != 4) {
            return null;
        }
        final int tagVal = ((tag.charAt(0)<<24)
            | (tag.charAt(1)<<16)
            | (tag.charAt(2)<<8)
            | tag.charAt(3));
        for (int i = 0; i < _featureCount; i++) {
            if (_featureRecords[i].getTag() == tagVal) {
                if (langSys.isFeatureIndexed(i)) {
                    return _features[i];
                }
            }
        }
        return null;
    }

}
