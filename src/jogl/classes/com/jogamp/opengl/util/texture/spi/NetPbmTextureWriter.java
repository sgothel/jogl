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
import java.nio.channels.FileChannel;

import com.jogamp.opengl.*;
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
    public NetPbmTextureWriter(final int magic) {
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

    /** @see TextureIO#PPM */
    public static final String PPM     = TextureIO.PPM;
    /** @see TextureIO#PAM */
    public static final String PAM     = TextureIO.PAM;

    public String getSuffix() { return (magic==6)?PPM:PAM; }

    @Override
    public boolean write(final File file, final TextureData data) throws IOException {
        boolean res;
        final int magic_old = magic;

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
        try {
            res = writeImpl(file, data);
        } finally {
            magic = magic_old;
        }
        return res;
    }

    private boolean writeImpl(final File file, final TextureData data) throws IOException {
        int pixelFormat = data.getPixelFormat();
        final int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA ||
             pixelFormat == GL.GL_BGR ||
             pixelFormat == GL.GL_BGRA ) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {

            ByteBuffer buf = (ByteBuffer) data.getBuffer();
            if (null == buf ) {
                buf = (ByteBuffer) data.getMipmapData()[0];
            }
            buf.rewind();

            final int comps = ( pixelFormat == GL.GL_RGBA || pixelFormat == GL.GL_BGRA ) ? 4 : 3 ;

            if( pixelFormat == GL.GL_BGR || pixelFormat == GL.GL_BGRA ) {
                // Must reverse order of red and blue channels to get correct results
                for (int i = 0; i < buf.remaining(); i += comps) {
                    final byte red  = buf.get(i + 0);
                    final byte blue = buf.get(i + 2);
                    buf.put(i + 0, blue);
                    buf.put(i + 2, red);
                }
                pixelFormat = ( 4 == comps ) ? GL.GL_RGBA : GL.GL_RGB;
                data.setPixelFormat(pixelFormat);
            }

            if(magic==6 && comps==4) {
                throw new IOException("NetPbmTextureWriter magic 6 (PPM) doesn't RGBA pixel format, use magic 7 (PAM)");
            }

            final FileOutputStream fos = IOUtil.getFileOutputStream(file, true);

            final StringBuilder header = new StringBuilder();
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

            final FileChannel fosc = fos.getChannel();
            fosc.write(buf);
            fosc.force(true);
            fosc.close();
            fos.close();
            buf.rewind();

            return true;
        }
        throw new IOException("NetPbmTextureWriter writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
    }
}
