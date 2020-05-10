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
 * Encoding record.
 * 
 * <p>
 * The array of encoding records specifies particular encodings and the offset
 * to the subtable for each encoding.
 * </p>
 * 
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class CmapIndexEntry implements Comparable<CmapIndexEntry> {

    /**
     * @see #getPlatformId()
     */
    private int _platformId;

    /**
     * @see #getEncodingId()
     */
    private int _encodingId;

    /**
     * @see #getOffset()
     */
    private int _offset;
    
    /**
     * @see #getFormat()
     */
    private CmapFormat _format;

    CmapIndexEntry(DataInput di) throws IOException {
        _platformId = di.readUnsignedShort();
        _encodingId = di.readUnsignedShort();
        _offset = di.readInt();
    }

    /**
     * uint16
     * 
     * Platform ID.
     * 
     * <p>
     * Complete details on platform IDs and platform-specific encoding and
     * language IDs are provided in the {@link NameTable}.
     * </p>
     * 
     * @see xxxx
     */
    public int getPlatformId() {
        return _platformId;
    }

    /**
     * uint16
     * 
     * Platform-specific encoding ID.
     * 
     * <p>
     * The platform ID and platform-specific encoding ID in the encoding record
     * are used to specify a particular character encoding. In the case of the
     * Macintosh platform, a language field within the mapping subtable is also
     * used for this purpose.
     * </p>
     */
    public int getEncodingId() {
        return _encodingId;
    }

    /**
     * Offset32
     * 
     * Byte offset from beginning of table to the subtable for this encoding.
     */
    public int getOffset() {
        return _offset;
    }

    public CmapFormat getFormat() {
        return _format;
    }
    
    public void setFormat(CmapFormat format) {
        _format = format;
    }

    @Override
    public String toString() {
        return 
            "    Index entry\n"+
            "    -----------\n"+
            "    platformId:     " + _platformId + " (" + ID.getPlatformName((short) _platformId) + ")\n" + 
            "    encodingId:     " + _encodingId + " (" + ID.getEncodingName((short) _platformId, (short) _encodingId) + ")\n" + 
            "    offset:         " + _offset + "\n" +
            _format;
    }

    @Override
    public int compareTo(CmapIndexEntry entry) {
        return Integer.compare(getOffset(), entry.getOffset());
    }
}
