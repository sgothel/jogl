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

import java.io.DataInput;
import java.io.IOException;

/**
 * @version $Id: CmapIndexEntry.java,v 1.2 2004-12-21 10:22:56 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public class CmapIndexEntry implements Comparable {

    private int _platformId;
    private int _encodingId;
    private int _offset;
    private CmapFormat _format;

    protected CmapIndexEntry(DataInput di) throws IOException {
        _platformId = di.readUnsignedShort();
        _encodingId = di.readUnsignedShort();
        _offset = di.readInt();
    }

    public int getPlatformId() {
        return _platformId;
    }

    public int getEncodingId() {
        return _encodingId;
    }

    public int getOffset() {
        return _offset;
    }

    public CmapFormat getFormat() {
        return _format;
    }
    
    public void setFormat(CmapFormat format) {
        _format = format;
    }

    public String toString() {
        return new StringBuffer()
            .append("platform id: ")
            .append(_platformId)
            .append(" (")
            .append(ID.getPlatformName((short) _platformId))
            .append("), encoding id: ")
            .append(_encodingId)
            .append(" (")
            .append(ID.getEncodingName((short) _platformId, (short) _encodingId))
            .append("), offset: ")
            .append(_offset).toString();
    }

    public int compareTo(java.lang.Object obj) {
        CmapIndexEntry entry = (CmapIndexEntry) obj;
        if (getOffset() < entry.getOffset()) {
            return -1;
        } else if (getOffset() > entry.getOffset()) {
            return 1;
        } else {
            return 0;
        }
    }
}
