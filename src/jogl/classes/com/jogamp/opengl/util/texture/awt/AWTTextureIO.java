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

package com.jogamp.opengl.util.texture.awt;

import java.awt.image.*;

import com.jogamp.opengl.*;
import com.jogamp.opengl.util.texture.*;

public class AWTTextureIO extends TextureIO {
    /**
     * Creates a TextureData from the given BufferedImage. Does no
     * OpenGL work.
     * We assume a desktop GLProfile GL2GL3, otherwise use the other factory.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param image the BufferedImage containing the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture by autogenerating them
     * @return the texture data from the image
     *
     * @see #newTextureData(GLProfile, BufferedImage, boolean)
     */
    public static TextureData newTextureData(final GLProfile glp, final BufferedImage image,
                                             final boolean mipmap) {
        return newTextureDataImpl(glp, image, 0, 0, mipmap);
    }

    /**
     * Creates a TextureData from the given BufferedImage, using the
     * specified OpenGL internal format and pixel format for the texture
     * which will eventually result. The internalFormat and pixelFormat
     * must be specified and may not be zero; to use default values, use
     * the variant of this method which does not take these
     * arguments. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param image the BufferedImage containing the texture data
     * @param internalFormat the OpenGL internal format of the texture
     *                   which will eventually result from the TextureData
     * @param pixelFormat the OpenGL pixel format of the texture
     *                    which will eventually result from the TextureData
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @return the texture data from the image
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     */
    public static TextureData newTextureData(final GLProfile glp, final BufferedImage image,
                                             final int internalFormat,
                                             final int pixelFormat,
                                             final boolean mipmap) throws IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        return newTextureDataImpl(glp, image, internalFormat, pixelFormat, mipmap);
    }

    /**
     * Creates an OpenGL texture object from the specified BufferedImage
     * using the current OpenGL context.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param image the BufferedImage from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture by autogenerating them
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Texture newTexture(final GLProfile glp, final BufferedImage image, final boolean mipmap) throws GLException {
        final TextureData data = newTextureData(glp, image, mipmap);
        final Texture texture = newTexture(data);
        data.flush();
        return texture;
    }

    private static TextureData newTextureDataImpl(final GLProfile glp,
                                                  final BufferedImage image,
                                                  final int internalFormat,
                                                  final int pixelFormat,
                                                  final boolean mipmap) {
        return new AWTTextureData(glp, internalFormat, pixelFormat, mipmap, image);
    }
}
