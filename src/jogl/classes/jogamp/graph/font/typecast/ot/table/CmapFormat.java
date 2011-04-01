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
 * @version $Id: CmapFormat.java,v 1.3 2004-12-21 16:56:35 davidsch Exp $
 * @author <a href="mailto:davidsch@dev.java.net">David Schweinsberg</a>
 */
public abstract class CmapFormat {
    
    public class Range {
        
        private int _startCode;
        private int _endCode;
        
        protected Range(int startCode, int endCode) {
            _startCode = startCode;
            _endCode = endCode;
        }
        
        public int getStartCode() {
            return _startCode;
        }
        
        public int getEndCode() {
            return _endCode;
        }
    }

    protected int _format;
    protected int _length;
    protected int _language;

    protected CmapFormat(DataInput di) throws IOException {
        _length = di.readUnsignedShort();
        _language = di.readUnsignedShort();
    }

    protected static CmapFormat create(int format, DataInput di)
    throws IOException {
        switch(format) {
            case 0:
                return new CmapFormat0(di);
            case 2:
                return new CmapFormat2(di);
            case 4:
                return new CmapFormat4(di);
            case 6:
                return new CmapFormat6(di);
            default:
                return new CmapFormatUnknown(format, di);
        }
    }

    public int getFormat() {
        return _format;
    }

    public int getLength() {
        return _length;
    }

    public int getLanguage() {
        return _language;
    }

    public abstract int getRangeCount();
    
    public abstract Range getRange(int index)
        throws ArrayIndexOutOfBoundsException;

    public abstract int mapCharCode(int charCode);
    
    public String toString() {
        return new StringBuffer()
        .append("format: ")
        .append(_format)
        .append(", length: ")
        .append(_length)
        .append(", language: ")
        .append(_language).toString();
    }
}
