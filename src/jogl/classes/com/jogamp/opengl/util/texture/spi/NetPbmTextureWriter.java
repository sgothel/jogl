/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.jogamp.opengl.util.texture.spi;

import java.io.*;
import java.nio.*;

import javax.media.opengl.*;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.texture.*;

public class NetPbmTextureWriter implements TextureWriter {
    int magic;

    public NetPbmTextureWriter() {
        this(0); // auto
    }

    /**
     * supported magic values are:<br>
     * <pre>
     *   magic 0 - detect by file suffix (TextureIO compliant)
     *   magic 6 - PPM binary RGB
     *   magic 7 - PAM binary RGB or RGBA
     * </pre>
     */
    public NetPbmTextureWriter(int magic) {
        switch(magic) {
            case 0:
            case 6:
            case 7:
                break;
            default:
                throw new GLException("Unsupported magic: "+magic+", should be 0 (auto), 6 (PPM) or 7 (PAM)");
        }
        this.magic = magic;
    }

    public int getMagic() { return magic; }

    public static final String PPM     = "ppm";
    public static final String PAM     = "pam";

    public String getSuffix() { return (magic==6)?PPM:PAM; }

    public boolean write(File file,
                         TextureData data) throws IOException {

        // file suffix selection 
        if (0==magic) {
            if (PPM.equals(IOUtil.getFileSuffix(file))) {
                magic = 6;
            } else if (PAM.equals(IOUtil.getFileSuffix(file))) {
                magic = 7;
            } else {
                return false;
            }
        }

        final int pixelFormat = data.getPixelFormat();
        final int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {
    
            int comps = ( pixelFormat == GL.GL_RGBA ) ? 4 : 3 ;

            if(magic==6 && comps==4) {
                throw new IOException("NetPbmTextureWriter magic 6 (PPM) doesn't RGBA pixel format, use magic 7 (PAM)");
            }

            FileOutputStream fos = new FileOutputStream(file);

            StringBuffer header = new StringBuffer();
            header.append("P");
            header.append(magic);
            header.append("\n");
            if(7==magic) {
                header.append("WIDTH ");
            }
            header.append(data.getWidth());
            if(7==magic) {
                header.append("\nHEIGHT ");
            } else {
                header.append(" ");
            }
            header.append(data.getHeight());
            if(7==magic) {
                header.append("\nDEPTH ");
                header.append(comps);
                header.append("\nMAXVAL 255\nTUPLTYPE ");
                if(pixelFormat == GL.GL_RGBA) {
                    header.append("RGB_ALPHA");
                } else {
                    header.append("RGB");
                }
                header.append("\nENDHDR\n");
            } else {
                header.append("\n255\n");
            }

            fos.write(header.toString().getBytes());
                
            ByteBuffer buf = (ByteBuffer) data.getBuffer();
            if (buf == null) {
                buf = (ByteBuffer) data.getMipmapData()[0];
            }
            buf.rewind();

            byte[] bufArray = null;

            try {
                bufArray = buf.array();
            } catch (Throwable t) {}
            if(null==bufArray) {
                bufArray = new byte[data.getWidth()*data.getHeight()*comps];
                buf.get(bufArray);
                buf.rewind();
            }

            fos.write(bufArray);
            fos.close();

            return true;
        }
      
        throw new IOException("NetPbmTextureWriter writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
    }
}
