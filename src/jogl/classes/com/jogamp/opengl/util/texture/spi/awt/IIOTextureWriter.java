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

package com.jogamp.opengl.util.texture.spi.awt;

import java.awt.Graphics;
import java.awt.image.*;
import java.io.*;
import java.nio.*;
import javax.imageio.*;

import javax.media.opengl.*;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.awt.*;
import com.jogamp.opengl.util.texture.*;
import com.jogamp.opengl.util.texture.spi.*;

public class IIOTextureWriter implements TextureWriter {
    public boolean write(File file,
                         TextureData data) throws IOException {
        int pixelFormat = data.getPixelFormat();
        int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {
            // Convert TextureData to appropriate BufferedImage
            // FIXME: almost certainly not obeying correct pixel order
            BufferedImage image = new BufferedImage(data.getWidth(), data.getHeight(),
                                                    (pixelFormat == GL.GL_RGB) ?
                                                    BufferedImage.TYPE_3BYTE_BGR :
                                                    BufferedImage.TYPE_4BYTE_ABGR);
            byte[] imageData = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
            ByteBuffer buf = (ByteBuffer) data.getBuffer();
            if (buf == null) {
                buf = (ByteBuffer) data.getMipmapData()[0];
            }
            buf.rewind();
            buf.get(imageData);
            buf.rewind();

            // Swizzle image components to be correct
            if (pixelFormat == GL.GL_RGB) {
                for (int i = 0; i < imageData.length; i += 3) {
                    byte red  = imageData[i + 0];
                    byte blue = imageData[i + 2];
                    imageData[i + 0] = blue;
                    imageData[i + 2] = red;
                }
            } else {
                for (int i = 0; i < imageData.length; i += 4) {
                    byte red   = imageData[i + 0];
                    byte green = imageData[i + 1];
                    byte blue  = imageData[i + 2];
                    byte alpha = imageData[i + 3];
                    imageData[i + 0] = alpha;
                    imageData[i + 1] = blue;
                    imageData[i + 2] = green;
                    imageData[i + 3] = red;
                }
            }

            // Flip image vertically for the user's convenience
            ImageUtil.flipImageVertically(image);

            // Happened to notice that writing RGBA images to JPEGS is broken
            if (TextureIO.JPG.equals(IOUtil.getFileSuffix(file)) &&
                image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
                BufferedImage tmpImage = new BufferedImage(image.getWidth(), image.getHeight(),
                                                           BufferedImage.TYPE_3BYTE_BGR);
                Graphics g = tmpImage.getGraphics();
                g.drawImage(image, 0, 0, null);
                g.dispose();
                image = tmpImage;
            }

            return ImageIO.write(image, IOUtil.getFileSuffix(file), file);
        }
      
        throw new IOException("ImageIO writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
    }
}
