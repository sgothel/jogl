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
import java.net.*;
import com.jogamp.opengl.GLProfile;

import com.jogamp.opengl.util.texture.*;

/** Plug-in interface to TextureIO to support reading OpenGL textures
    from new file formats. For all methods, either internalFormat or
    pixelFormat may be 0 in which case they must be inferred as
    e.g. RGB or RGBA depending on the file contents.
*/

public interface TextureProvider {

    /**
     * Optional additional interface for {@link TextureProvider} implementation
     * exposing the supported {@link ImageType}s.
     * <p>
     * Use case: Mapping of {@link ImageType}s to {@link TextureProvider}.
     * </p>
     */
    public static interface SupportsImageTypes {
        /** Returns the supported {@link ImageType}s. */
        ImageType[] getImageTypes();
    }

    /**
     * Produces a TextureData object from a file, or returns null if the
     * file format was not supported by this TextureProvider. Does not
     * do any OpenGL-related work. The resulting TextureData can be
     * converted into an OpenGL texture in a later step.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param file         the file from which to read the texture data
     *
     * @param internalFormat the OpenGL internal format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param pixelFormat    the OpenGL pixel format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     *
     * @param fileSuffix     the file suffix to be used as a hint to the
     *                       provider to more quickly decide whether it
     *                       can handle the file, or null if the
     *                       provider should infer the type from the
     *                       file's contents
     *
     * @throws IOException if an error occurred while reading the file
     * @deprecated Use {@link #newTextureData(GLProfile, InputStream, int, int, boolean, String)
     */
    public TextureData newTextureData(GLProfile glp, File file,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException;

    /**
     * Produces a TextureData object from a stream, or returns null if
     * the file format was not supported by this TextureProvider. Does
     * not do any OpenGL-related work. The resulting TextureData can be
     * converted into an OpenGL texture in a later step.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param stream       the stream from which to read the texture data
     *
     * @param internalFormat the OpenGL internal format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param pixelFormat    the OpenGL pixel format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     *
     * @param fileSuffix     the file suffix to be used as a hint to the
     *                       provider to more quickly decide whether it
     *                       can handle the file, or null if the
     *                       provider should infer the type from the
     *                       file's contents
     *
     * @throws IOException if an error occurred while reading the stream
     */
    public TextureData newTextureData(GLProfile glp, InputStream stream,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException;

    /**
     * Produces a TextureData object from a URL, or returns null if the
     * file format was not supported by this TextureProvider. Does not
     * do any OpenGL-related work. The resulting TextureData can be
     * converted into an OpenGL texture in a later step.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param url          the URL from which to read the texture data
     *
     * @param internalFormat the OpenGL internal format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param pixelFormat    the OpenGL pixel format to be used for
     *                       the texture, or 0 if it should be inferred
     *                       from the file's contents
     *
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     *
     * @param fileSuffix     the file suffix to be used as a hint to the
     *                       provider to more quickly decide whether it
     *                       can handle the file, or null if the
     *                       provider should infer the type from the
     *                       file's contents
     *
     * @throws IOException if an error occurred while reading the URL
     * @deprecated Use {@link #newTextureData(GLProfile, InputStream, int, int, boolean, String)
     */
    public TextureData newTextureData(GLProfile glp, URL url,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException;
}
