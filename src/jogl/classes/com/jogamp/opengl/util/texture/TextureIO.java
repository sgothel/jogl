/*
 * Copyright (c) 2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
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

package com.jogamp.opengl.util.texture;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.jogamp.nativewindow.util.Dimension;
import com.jogamp.nativewindow.util.DimensionImmutable;
import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLException;
import com.jogamp.opengl.GLProfile;

import jogamp.opengl.Debug;

import com.jogamp.common.util.IOUtil;
import com.jogamp.opengl.util.GLPixelStorageModes;
import com.jogamp.opengl.util.PNGPixelRect;
import com.jogamp.opengl.util.GLPixelBuffer.GLPixelAttributes;
import com.jogamp.opengl.util.texture.ImageType;
import com.jogamp.opengl.util.texture.spi.DDSImage;
import com.jogamp.opengl.util.texture.spi.JPEGImage;
import com.jogamp.opengl.util.texture.spi.NetPbmTextureWriter;
import com.jogamp.opengl.util.texture.spi.SGIImage;
import com.jogamp.opengl.util.texture.spi.TGAImage;
import com.jogamp.opengl.util.texture.spi.TextureProvider;
import com.jogamp.opengl.util.texture.spi.TextureWriter;

/** <P> Provides input and output facilities for both loading OpenGL
    textures from disk and streams as well as writing textures already
    in memory back to disk. </P>

    <P> The TextureIO class supports an arbitrary number of plug-in
    readers and writers via TextureProviders and TextureWriters.
    TextureProviders know how to produce TextureData objects from
    files, InputStreams and URLs. TextureWriters know how to write
    TextureData objects to disk in various file formats. The
    TextureData class represents the raw data of the texture before it
    has been converted to an OpenGL texture object. The Texture class
    represents the OpenGL texture object and provides easy facilities
    for using the texture. </P>

    <P> There are several built-in TextureProviders and TextureWriters
    supplied with the TextureIO implementation. The most basic
    provider uses the platform's Image I/O facilities to read in a
    BufferedImage and convert it to a texture. This is the baseline
    provider and is registered so that it is the last one consulted.
    All others are asked first to open a given file. </P>

    <P> There are three other providers registered by default as of
    the time of this writing. One handles SGI RGB (".sgi", ".rgb")
    images from both files and streams. One handles DirectDraw Surface
    (".dds") images read from files, though can not read these images
    from streams. One handles Targa (".tga") images read from both
    files and streams. These providers are executed in an arbitrary
    order. Some of these providers require the file's suffix to either
    be specified via the newTextureData methods or for the file to be
    named with the appropriate suffix. In general a file suffix should
    be provided to the newTexture and newTextureData methods if at all
    possible. </P>

    <P> Note that additional TextureProviders, if reading images from
    InputStreams, must use the mark()/reset() methods on InputStream
    when probing for e.g. magic numbers at the head of the file to
    make sure not to disturb the state of the InputStream for
    downstream TextureProviders. </P>

    <P> There are analogous TextureWriters provided for writing
    textures back to disk if desired. As of this writing, there are
    four TextureWriters registered by default: one for Targa files,
    one for SGI RGB files, one for DirectDraw surface (.dds) files,
    and one for ImageIO-supplied formats such as .jpg and .png.  Some
    of these writers have certain limitations such as only being able
    to write out textures stored in GL_RGB or GL_RGBA format. The DDS
    writer supports fetching and writing to disk of texture data in
    DXTn compressed format. Whether this will occur is dependent on
    whether the texture's internal format is one of the DXTn
    compressed formats and whether the target file is .dds format.
*/

public class TextureIO {
    /** Constant which can be used as a file suffix to indicate a
        DirectDraw Surface file, value {@value}.
        <p>Alias for {@link ImageType#T_DDS}.</p>
     */
    public static final String DDS     = ImageType.T_DDS;

    /**
     * Constant which can be used as a file suffix to indicate an SGI RGB file, value {@value}.
     * <p>
     * Same semantics as {@link ImageType#SGI_RGB} and {@link #SGI_RGB}.
     * </p>
     */
    public static final String SGI     = "sgi";

    /** Constant which can be used as a file suffix to indicate an SGI RGB file, value {@value}.
        <p>Alias for {@link ImageType#T_SGI_RGB}. </p>
     */
    public static final String SGI_RGB = ImageType.T_SGI_RGB;

    /** Constant which can be used as a file suffix to indicate a GIF file, value {@value}.
        <p>Alias for {@link ImageType#T_GIF}.</p>
     */
    public static final String GIF     = ImageType.T_GIF;

    /** Constant which can be used as a file suffix to indicate a JPEG file, value {@value}.
        <p>Alias for {@link ImageType#T_JPG}.</p>
     */
    public static final String JPG     = ImageType.T_JPG;

    /** Constant which can be used as a file suffix to indicate a PNG file, value {@value}.
        <p>Alias for {@link ImageType#T_PNG}.</p>
     */
    public static final String PNG     = ImageType.T_PNG;

    /** Constant which can be used as a file suffix to indicate a Targa file, value {@value}.
        <p>Alias for {@link ImageType#T_TGA}.</p>
     */
    public static final String TGA     = ImageType.T_TGA;

    /** Constant which can be used as a file suffix to indicate a TIFF file, value {@value}.
        <p>Alias for {@link ImageType#T_TIFF}.</p>
     */
    public static final String TIFF    = ImageType.T_TIFF;

    /** Constant which can be used as a file suffix to indicate a PAM
        file, NetPbm magic 7 - binary RGB and RGBA. Write support only, value {@value}.
        <p>Alias for {@link ImageType#T_PAM}.</p>
     */
    public static final String PAM     = ImageType.T_PAM;

    /** Constant which can be used as a file suffix to indicate a PAM
        file, NetPbm magic 6 - binary RGB. Write support only, value {@value}.
        <p>Alias for {@link ImageType#T_PPM}.</p>
     */
    public static final String PPM     = ImageType.T_PPM;

    private static final boolean DEBUG = Debug.debug("TextureIO");

    // For manually disabling the use of the texture rectangle
    // extensions so you know the texture target is GL_TEXTURE_2D; this
    // is useful for shader writers (thanks to Chris Campbell for this
    // observation)
    private static boolean texRectEnabled = true;

    //----------------------------------------------------------------------
    // methods that *do not* require a current context
    // These methods assume RGB or RGBA textures.
    // Some texture providers may not recognize the file format unless
    // the fileSuffix is specified, so it is strongly recommended to
    // specify it wherever it is known.
    // Some texture providers may also only support one kind of input,
    // i.e., reading from a file as opposed to a stream.

    /**
     * Creates a TextureData from the given file. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param file the file from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the file, or null if none of the
     *         registered texture providers could read the file
     * @throws IOException if an error occurred while reading the file
     */
    public static TextureData newTextureData(final GLProfile glp, final File file,
                                             final boolean mipmap,
                                             String fileSuffix) throws IOException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(file);
        }
        return newTextureDataImpl(glp, file, 0, 0, mipmap, fileSuffix);
    }

    /**
     * Creates a TextureData from the given stream. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param stream the stream from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the stream, or null if none of the
     *         registered texture providers could read the stream
     * @throws IOException if an error occurred while reading the stream
     */
    public static TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                             final boolean mipmap,
                                             final String fileSuffix) throws IOException {
        return newTextureDataImpl(glp, stream, 0, 0, mipmap, fileSuffix);
    }

    /**
     * Creates a TextureData from the given URL. Does no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param url the URL from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the URL, or null if none of the
     *         registered texture providers could read the URL
     * @throws IOException if an error occurred while reading the URL
     */
    public static TextureData newTextureData(final GLProfile glp, final URL url,
                                             final boolean mipmap,
                                             String fileSuffix) throws IOException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }
        return newTextureDataImpl(glp, url, 0, 0, mipmap, fileSuffix);
    }

    //----------------------------------------------------------------------
    // These methods make no assumption about the OpenGL internal format
    // or pixel format of the texture; they must be specified by the
    // user. It is not allowed to supply 0 (indicating no preference)
    // for either the internalFormat or the pixelFormat;
    // IllegalArgumentException will be thrown in this case.

    /**
     * Creates a TextureData from the given file, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param file the file from which to read the texture data
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
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the file, or null if none of the
     *         registered texture providers could read the file
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the file
     */
    public static TextureData newTextureData(final GLProfile glp, final File file,
                                             final int internalFormat,
                                             final int pixelFormat,
                                             final boolean mipmap,
                                             String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(file);
        }

        return newTextureDataImpl(glp, file, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    /**
     * Creates a TextureData from the given stream, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param stream the stream from which to read the texture data
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
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the stream, or null if none of the
     *         registered texture providers could read the stream
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the stream
     */
    public static TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                             final int internalFormat,
                                             final int pixelFormat,
                                             final boolean mipmap,
                                             final String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        return newTextureDataImpl(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    /**
     * Creates a TextureData from the given URL, using the specified
     * OpenGL internal format and pixel format for the texture which
     * will eventually result. The internalFormat and pixelFormat must
     * be specified and may not be zero; to use default values, use the
     * variant of this method which does not take these arguments. Does
     * no OpenGL work.
     *
     * @param glp the OpenGL Profile this texture data should be
     *                  created for.
     * @param url the URL from which to read the texture data
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
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @return the texture data from the URL, or null if none of the
     *         registered texture providers could read the URL
     * @throws IllegalArgumentException if either internalFormat or
     *                                  pixelFormat was 0
     * @throws IOException if an error occurred while reading the URL
     */
    public static TextureData newTextureData(final GLProfile glp, final URL url,
                                             final int internalFormat,
                                             final int pixelFormat,
                                             final boolean mipmap,
                                             String fileSuffix) throws IOException, IllegalArgumentException {
        if ((internalFormat == 0) || (pixelFormat == 0)) {
            throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
        }

        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }

        return newTextureDataImpl(glp, url, internalFormat, pixelFormat, mipmap, fileSuffix);
    }

    //----------------------------------------------------------------------
    // methods that *do* require a current context
    //

    /**
     * Creates an OpenGL texture object from the specified TextureData
     * using the current OpenGL context.
     *
     * @param data the texture data to turn into an OpenGL texture
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     * @throws IllegalArgumentException if the passed TextureData was null
     */
    public static Texture newTexture(final TextureData data) throws GLException, IllegalArgumentException {
        return newTexture(GLContext.getCurrentGL(), data);
    }

    /**
     * Creates an OpenGL texture object from the specified TextureData
     * using the given OpenGL context.
     *
     * @param data the texture data to turn into an OpenGL texture
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     * @throws IllegalArgumentException if the passed TextureData was null
     */
    public static Texture newTexture(final GL gl, final TextureData data) throws GLException, IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("Null TextureData");
        }
        return new Texture(gl, data);
    }

    /**
     * Creates an OpenGL texture object from the specified file using
     * the current OpenGL context.
     *
     * @param file the file from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @throws IOException if an error occurred while reading the file
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Texture newTexture(final File file, final boolean mipmap) throws IOException, GLException {
        final GL gl = GLContext.getCurrentGL();
        final GLProfile glp = gl.getGLProfile();
        final TextureData data = newTextureData(glp, file, mipmap, IOUtil.getFileSuffix(file));
        final Texture texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object from the specified stream using
     * the current OpenGL context.
     *
     * @param stream the stream from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @throws IOException if an error occurred while reading the stream
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Texture newTexture(final InputStream stream, final boolean mipmap, final String fileSuffix) throws IOException, GLException {
        final GL gl = GLContext.getCurrentGL();
        final GLProfile glp = gl.getGLProfile();
        final TextureData data = newTextureData(glp, stream, mipmap, fileSuffix);
        final Texture texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object from the specified URL using the
     * current OpenGL context.
     *
     * @param url the URL from which to read the texture data
     * @param mipmap     whether mipmaps should be produced for this
     *                   texture either by autogenerating them or
     *                   reading them from the file. Some file formats
     *                   support multiple mipmaps in a single file in
     *                   which case those mipmaps will be used rather
     *                   than generating them.
     * @param fileSuffix the suffix of the file name to be used as a
     *                   hint of the file format to the underlying
     *                   texture provider, or null if none and should be
     *                   auto-detected (some texture providers do not
     *                   support this)
     * @throws IOException if an error occurred while reading the URL
     * @throws GLException if no OpenGL context is current or if an
     *                     OpenGL error occurred
     */
    public static Texture newTexture(final URL url, final boolean mipmap, String fileSuffix) throws IOException, GLException {
        if (fileSuffix == null) {
            fileSuffix = IOUtil.getFileSuffix(url.getPath());
        }
        final GL gl = GLContext.getCurrentGL();
        final GLProfile glp = gl.getGLProfile();
        final TextureData data = newTextureData(glp, url, mipmap, fileSuffix);
        final Texture texture = newTexture(gl, data);
        data.flush();
        return texture;
    }

    /**
     * Creates an OpenGL texture object associated with the given OpenGL
     * texture target. The texture has
     * no initial data. This is used, for example, to construct cube
     * maps out of multiple TextureData objects.
     *
     * @param target the OpenGL target type, eg GL.GL_TEXTURE_2D,
     *               GL.GL_TEXTURE_RECTANGLE_ARB
     */
    public static Texture newTexture(final int target) {
        return new Texture(target);
    }

    /**
     * Writes the given texture to a file. The type of the file is
     * inferred from its suffix. An OpenGL context must be current in
     * order to fetch the texture data back from the OpenGL pipeline.
     * This method causes the specified Texture to be bound to the
     * GL_TEXTURE_2D state. If no suitable writer for the requested file
     * format was found, throws an IOException. <P>
     *
     * Reasonable attempts are made to produce good results in the
     * resulting images. The Targa, SGI and ImageIO writers produce
     * results in the correct vertical orientation for those file
     * formats. The DDS writer performs no vertical flip of the data,
     * even in uncompressed mode. (It is impossible to perform such a
     * vertical flip with compressed data.) Applications should keep
     * this in mind when using this routine to save textures to disk for
     * later re-loading. <P>
     *
     * Any mipmaps for the specified texture are currently discarded
     * when it is written to disk, regardless of whether the underlying
     * file format supports multiple mipmaps in a given file.
     *
     * <p>
     * Method required a {@link GL2GL3} {@link GLProfile#GL2GL3 profile}.
     * </p>
     *
     * @throws IOException if an error occurred during writing or no
     *   suitable writer was found
     * @throws GLException if no OpenGL context was current or an
     *   OpenGL-related error occurred
     */
    public static void write(final Texture texture, final File file) throws IOException, GLException {
        if (texture.getTarget() != GL.GL_TEXTURE_2D) {
            throw new GLException("Only GL_TEXTURE_2D textures are supported");
        }

        // First fetch the texture data
        final GL _gl = GLContext.getCurrentGL();
        if (!_gl.isGL2GL3()) {
            throw new GLException("Implementation only supports GL2GL3 (Use GLReadBufferUtil and the TextureData variant), have: " + _gl);
        }
        final GL2GL3 gl = _gl.getGL2GL3();

        texture.bind(gl);
        final int internalFormat = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2ES3.GL_TEXTURE_INTERNAL_FORMAT);
        final int width  = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2ES3.GL_TEXTURE_WIDTH);
        final int height = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2ES3.GL_TEXTURE_HEIGHT);
        final int border = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2.GL_TEXTURE_BORDER);
        TextureData data = null;
        if (internalFormat == GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT ||
            internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT) {
            // Fetch using glGetCompressedTexImage
            final int size   = glGetTexLevelParameteri(gl, GL.GL_TEXTURE_2D, 0, GL2GL3.GL_TEXTURE_COMPRESSED_IMAGE_SIZE);
            final ByteBuffer res = ByteBuffer.allocate(size);
            gl.glGetCompressedTexImage(GL.GL_TEXTURE_2D, 0, res);
            data = new TextureData(gl.getGLProfile(), internalFormat, width, height, border, internalFormat, GL.GL_UNSIGNED_BYTE,
                                   false, true, true, res, null);
        } else {
            int bytesPerPixel = 0;
            int fetchedFormat = 0;
            switch (internalFormat) {
            case GL.GL_RGB:
            case GL.GL_BGR:
            case GL.GL_RGB8:
                bytesPerPixel = 3;
                fetchedFormat = GL.GL_RGB;
                break;
            case GL.GL_RGBA:
            case GL.GL_BGRA:
            case GL2.GL_ABGR_EXT:
            case GL.GL_RGBA8:
                bytesPerPixel = 4;
                fetchedFormat = GL.GL_RGBA;
                break;
            default:
                throw new IOException("Unsupported texture internal format 0x" + Integer.toHexString(internalFormat));
            }

            // Fetch using glGetTexImage
            final GLPixelStorageModes psm = new GLPixelStorageModes();
            psm.setPackAlignment(gl, 1);

            final ByteBuffer res = ByteBuffer.allocate((width + (2 * border)) *
                                                 (height + (2 * border)) *
                                                 bytesPerPixel);
            if (DEBUG) {
                System.out.println("Allocated buffer of size " + res.remaining() + " for fetched image (" +
                                   ((fetchedFormat == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA") + ")");
            }
            gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, fetchedFormat, GL.GL_UNSIGNED_BYTE, res);

            psm.restore(gl);

            data = new TextureData(gl.getGLProfile(), internalFormat, width, height, border, fetchedFormat, GL.GL_UNSIGNED_BYTE,
                                   false, false, false, res, null);

            if (DEBUG) {
                System.out.println("data.getPixelFormat() = " +
                                   ((data.getPixelFormat() == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA"));
            }
        }

        write(data, file);
    }

    public static void write(final TextureData data, final File file) throws IOException, GLException {
        for (final Iterator<TextureWriter> iter = textureWriters.iterator(); iter.hasNext(); ) {
            final TextureWriter writer = iter.next();
            if (writer.write(file, data)) {
                return;
            }
        }

        throw new IOException("No suitable texture writer found for "+file.getAbsolutePath());
    }

    //----------------------------------------------------------------------
    // SPI support
    //

    /**
     * Adds a {@link TextureProvider} to support reading of a new file format.
     * <p>
     * The last provider added, will be the first provider to be tested.
     * </p>
     * <p>
     * In case the {@link TextureProvider} also implements {@link TextureProvider.SupportsImageTypes},
     * the {@link TextureProvider} is being mapped to its supporting {@link ImageType}s
     * allowing an O(1) association.
     * </p>
     */
    public static void addTextureProvider(final TextureProvider provider) {
        // Must always add at the front so the ImageIO provider is last,
        // so we don't accidentally use it instead of a user's possibly
        // more optimal provider
        textureProviders.add(0, provider);

        if( provider instanceof TextureProvider.SupportsImageTypes ) {
            final ImageType[] imageTypes = ((TextureProvider.SupportsImageTypes)provider).getImageTypes();
            if( null != imageTypes ) {
                for(int i=0; i<imageTypes.length; i++) {
                    imageType2TextureProvider.put(imageTypes[i], provider);
                }
            }
        }
    }

    /**
     * Adds a TextureWriter to support writing of a new file format.
     * <p>
     * The last provider added, will be the first provider to be tested.
     * </p>
     */
    public static void addTextureWriter(final TextureWriter writer) {
        // Must always add at the front so the ImageIO writer is last,
        // so we don't accidentally use it instead of a user's possibly
        // more optimal writer
        textureWriters.add(0, writer);
    }

    //---------------------------------------------------------------------------
    // Global disabling of texture rectangle extension
    //

    /** Toggles the use of the GL_ARB_texture_rectangle extension by the
        TextureIO classes. By default, on hardware supporting this
        extension, the TextureIO classes may use the
        GL_ARB_texture_rectangle extension for non-power-of-two
        textures. (If the hardware supports the
        GL_ARB_texture_non_power_of_two extension, that one is
        preferred.) In some situations, for example when writing
        shaders, it is advantageous to force the texture target to
        always be GL_TEXTURE_2D in order to have one version of the
        shader, even at the expense of texture memory in the case where
        NPOT textures are not supported. This method allows the use of
        the GL_ARB_texture_rectangle extension to be turned off globally
        for this purpose. The default is that the use of the extension
        is enabled. */
    public static void setTexRectEnabled(final boolean enabled) {
        texRectEnabled = enabled;
    }

    /** Indicates whether the GL_ARB_texture_rectangle extension is
        allowed to be used for non-power-of-two textures; see {@link
        #setTexRectEnabled setTexRectEnabled}. */
    public static boolean isTexRectEnabled() {
        return texRectEnabled;
    }

    //----------------------------------------------------------------------
    // Internals only below this point
    //

    private static List<TextureProvider> textureProviders = new ArrayList<TextureProvider>();
    private static Map<ImageType,TextureProvider> imageType2TextureProvider = new HashMap<ImageType,TextureProvider>();
    private static List<TextureWriter>   textureWriters   = new ArrayList<TextureWriter>();

    static {
        // ImageIO provider, the fall-back, must be the first one added
        if(GLProfile.isAWTAvailable()) {
            try {
                // Use reflection to avoid compile-time dependencies on AWT-related classes
                final TextureProvider provider = (TextureProvider)
                    Class.forName("com.jogamp.opengl.util.texture.spi.awt.IIOTextureProvider").newInstance();
                addTextureProvider(provider);
            } catch (final Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        // Other special-case providers
        addTextureProvider(new DDSTextureProvider());
        addTextureProvider(new SGITextureProvider());
        addTextureProvider(new TGATextureProvider());
        addTextureProvider(new JPGTextureProvider());
        addTextureProvider(new PNGTextureProvider());

        // ImageIO writer, the fall-back, must be the first one added
        if(GLProfile.isAWTAvailable()) {
            try {
                // Use reflection to avoid compile-time dependencies on AWT-related classes
                final TextureWriter writer = (TextureWriter)
                    Class.forName("com.jogamp.opengl.util.texture.spi.awt.IIOTextureWriter").newInstance();
                addTextureWriter(writer);
            } catch (final Exception e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            } catch (final Error e) {
                if (DEBUG) {
                    e.printStackTrace();
                }
            }
        }

        // Other special-case writers
        addTextureWriter(new DDSTextureWriter());
        addTextureWriter(new SGITextureWriter());
        addTextureWriter(new TGATextureWriter());
        addTextureWriter(new NetPbmTextureWriter());
        addTextureWriter(new PNGTextureWriter());
    }

    // Implementation methods
    private static TextureData newTextureDataImpl(final GLProfile glp, InputStream stream,
                                                  final int internalFormat,
                                                  final int pixelFormat,
                                                  final boolean mipmap,
                                                  String fileSuffix) throws IOException {
        if (stream == null) {
            throw new IOException("Stream was null");
        }

        // Note: use of BufferedInputStream works around 4764639/4892246
        if (!(stream instanceof BufferedInputStream)) {
            stream = new BufferedInputStream(stream);
        }

        // First attempt to use an ImageType mapped TextureProvider for O(1)
        // using stream parsed data, ignoring the given fileSuffix!
        try {
            final ImageType imageType = new ImageType(stream);
            if( imageType.isDefined() ) {
                final TextureProvider mappedProvider = imageType2TextureProvider.get(imageType);
                if( null != mappedProvider ) {
                    final TextureData data = mappedProvider.newTextureData(glp, stream,
                                                                           internalFormat,
                                                                           pixelFormat,
                                                                           mipmap,
                                                                           imageType.type);
                    if (data != null) {
                        data.srcImageType = imageType;
                        return data;
                    }
                }
            }
        } catch (final IOException ioe) {
            if(DEBUG) {
                System.err.println("Caught "+ioe.getMessage());
                ioe.printStackTrace();
            }
        }

        fileSuffix = toLowerCase(fileSuffix);

        for (final Iterator<TextureProvider> iter = textureProviders.iterator(); iter.hasNext(); ) {
            final TextureProvider provider = iter.next();
            final TextureData data = provider.newTextureData(glp, stream,
                                                             internalFormat,
                                                             pixelFormat,
                                                             mipmap,
                                                             fileSuffix);
            if (data != null) {
                if( provider instanceof TextureProvider.SupportsImageTypes ) {
                    data.srcImageType = ((TextureProvider.SupportsImageTypes)provider).getImageTypes()[0];
                }
                return data;
            }
        }

        throw new IOException("No suitable reader for given stream");
    }
    private static TextureData newTextureDataImpl(final GLProfile glp, final File file,
                                                  final int internalFormat,
                                                  final int pixelFormat,
                                                  final boolean mipmap,
                                                  final String fileSuffix) throws IOException {
        if (file == null) {
            throw new IOException("File was null");
        }
        final InputStream stream = new BufferedInputStream(new FileInputStream(file));
        try {
            return newTextureDataImpl( glp, stream, internalFormat, pixelFormat, mipmap,
                                       (fileSuffix != null) ? fileSuffix : IOUtil.getFileSuffix(file) );
        } catch(final IOException ioe) {
            throw new IOException(ioe.getMessage()+", given file "+file.getAbsolutePath(), ioe);
        } finally {
            stream.close();
        }
    }
    private static TextureData newTextureDataImpl(final GLProfile glp, final URL url,
                                                  final int internalFormat,
                                                  final int pixelFormat,
                                                  final boolean mipmap,
                                                  final String fileSuffix) throws IOException {
        if (url == null) {
            throw new IOException("URL was null");
        }
        final InputStream stream = new BufferedInputStream(url.openStream());
        try {
            return newTextureDataImpl(glp, stream, internalFormat, pixelFormat, mipmap, fileSuffix);
        } catch(final IOException ioe) {
            throw new IOException(ioe.getMessage()+", given URL "+url, ioe);
        } finally {
            stream.close();
        }
    }

    //----------------------------------------------------------------------
    // Base class for internal image providers, only providing stream based data!
    static abstract class StreamBasedTextureProvider implements TextureProvider, TextureProvider.SupportsImageTypes {
        @Override
        public final TextureData newTextureData(final GLProfile glp, final File file,
                                          final int internalFormat,
                                          final int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            throw new UnsupportedOperationException("Only stream is supported");
        }

        @Override
        public final TextureData newTextureData(final GLProfile glp, final URL url,
                                          final int internalFormat,
                                          final int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            throw new UnsupportedOperationException("Only stream is supported");
        }
    }

    //----------------------------------------------------------------------
    // DDS image provider
    static class DDSTextureProvider extends StreamBasedTextureProvider {
        private static final ImageType[] imageTypes = new ImageType[] { new ImageType(ImageType.T_DDS) };
        @Override
        public final ImageType[] getImageTypes() {
            return imageTypes;
        }

        @Override
        public TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                          final int internalFormat,
                                          final int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            if (ImageType.T_DDS.equals(fileSuffix) ||
                ImageType.T_DDS.equals(ImageType.Util.getFileSuffix(stream))) {
                final byte[] data = IOUtil.copyStream2ByteArray(stream);
                final ByteBuffer buf = ByteBuffer.wrap(data);
                final DDSImage image = DDSImage.read(buf);
                return newTextureData(glp, image, internalFormat, pixelFormat, mipmap);
            }

            return null;
        }

        private TextureData newTextureData(final GLProfile glp, final DDSImage image,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap) {
            final DDSImage.ImageInfo info = image.getMipMap(0);
            if (pixelFormat == 0) {
                switch (image.getPixelFormat()) {
                case DDSImage.D3DFMT_R8G8B8:
                    pixelFormat = GL.GL_RGB;
                    break;
                default:
                    pixelFormat = GL.GL_RGBA;
                    break;
                }
            }
            if (info.isCompressed()) {
                switch (info.getCompressionFormat()) {
                case DDSImage.D3DFMT_DXT1:
                    internalFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
                    break;
                case DDSImage.D3DFMT_DXT3:
                    internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
                    break;
                case DDSImage.D3DFMT_DXT5:
                    internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
                    break;
                default:
                    throw new RuntimeException("Unsupported DDS compression format \"" +
                                               DDSImage.getCompressionFormatName(info.getCompressionFormat()) + "\"");
                }
            }
            if (internalFormat == 0) {
                switch (image.getPixelFormat()) {
                case DDSImage.D3DFMT_R8G8B8:
                    pixelFormat = GL.GL_RGB;
                    break;
                default:
                    pixelFormat = GL.GL_RGBA;
                    break;
                }
            }
            final TextureData.Flusher flusher = new TextureData.Flusher() {
                    @Override
                    public void flush() {
                        image.close();
                    }
                };
            TextureData data;
            if (mipmap && image.getNumMipMaps() > 0) {
                final Buffer[] mipmapData = new Buffer[image.getNumMipMaps()];
                for (int i = 0; i < image.getNumMipMaps(); i++) {
                    mipmapData[i] = image.getMipMap(i).getData();
                }
                data = new TextureData(glp, internalFormat,
                                       info.getWidth(),
                                       info.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       info.isCompressed(),
                                       true,
                                       mipmapData,
                                       flusher);
            } else {
                // Fix this up for the end user because we can't generate
                // mipmaps for compressed textures
                mipmap = false;
                data = new TextureData(glp, internalFormat,
                                       info.getWidth(),
                                       info.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       info.isCompressed(),
                                       true,
                                       info.getData(),
                                       flusher);
            }
            return data;
        }
    }

    //----------------------------------------------------------------------
    // SGI RGB image provider
    static class SGITextureProvider extends StreamBasedTextureProvider {
        private static final ImageType[] imageTypes = new ImageType[] { new ImageType(ImageType.T_SGI_RGB) };
        @Override
        public final ImageType[] getImageTypes() {
            return imageTypes;
        }

        @Override
        public TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            if (SGI.equals(fileSuffix) ||
                ImageType.T_SGI_RGB.equals(fileSuffix) ||
                SGI.equals(ImageType.Util.getFileSuffix(stream)) ||
                ImageType.T_SGI_RGB.equals(ImageType.Util.getFileSuffix(stream))) {
                final SGIImage image = SGIImage.read(stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getFormat();
                }
                if (internalFormat == 0) {
                    internalFormat = image.getFormat();
                }
                return new TextureData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       false,
                                       false,
                                       ByteBuffer.wrap(image.getData()),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // TGA (Targa) image provider
    static class TGATextureProvider extends StreamBasedTextureProvider {
        private static final ImageType[] imageTypes = new ImageType[] { new ImageType(ImageType.T_TGA) };
        @Override
        public final ImageType[] getImageTypes() {
            return imageTypes;
        }

        @Override
        public TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            if (ImageType.T_TGA.equals(fileSuffix)) {
                final TGAImage image = TGAImage.read(glp, stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getGLFormat();
                }
                if (internalFormat == 0) {
                    if(glp.isGL2ES3()) {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA8:GL.GL_RGB8;
                    } else {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                    }
                }
                return new TextureData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       GL.GL_UNSIGNED_BYTE,
                                       mipmap,
                                       false,
                                       false,
                                       image.getData(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // PNG image provider
    static class PNGTextureProvider extends StreamBasedTextureProvider {
        private static final ImageType[] imageTypes = new ImageType[] { new ImageType(ImageType.T_PNG) };
        @Override
        public final ImageType[] getImageTypes() {
            return imageTypes;
        }

        @Override
        public TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            if (ImageType.T_PNG.equals(fileSuffix) ||
            	ImageType.T_PNG.equals(ImageType.Util.getFileSuffix(stream))) {
                final PNGPixelRect image = PNGPixelRect.read(stream, null, true /* directBuffer */, 0 /* destMinStrideInBytes */, true /* destIsGLOriented */);
                final GLPixelAttributes glpa = new GLPixelAttributes(glp, image.getPixelformat(), false /* pack */);
                if ( 0 == pixelFormat ) {
                    pixelFormat = glpa.format;
                }  // else FIXME: Actually not supported w/ preset pixelFormat!
                if ( 0 == internalFormat ) {
                    final boolean hasAlpha = 4 == glpa.pfmt.comp.bytesPerPixel();
                    if(glp.isGL2ES3()) {
                        internalFormat = hasAlpha ? GL.GL_RGBA8 : GL.GL_RGB8;
                    } else {
                        internalFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
                    }
                }
                return new TextureData(glp, internalFormat,
                                       image.getSize().getWidth(),
                                       image.getSize().getHeight(),
                                       0,
                                       pixelFormat,
                                       glpa.type,
                                       mipmap,
                                       false,
                                       false,
                                       image.getPixels(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // JPEG image provider
    static class JPGTextureProvider extends StreamBasedTextureProvider {
        private static final ImageType[] imageTypes = new ImageType[] { new ImageType(ImageType.T_JPG) };
        @Override
        public final ImageType[] getImageTypes() {
            return imageTypes;
        }

        @Override
        public TextureData newTextureData(final GLProfile glp, final InputStream stream,
                                          int internalFormat,
                                          int pixelFormat,
                                          final boolean mipmap,
                                          final String fileSuffix) throws IOException {
            if (ImageType.T_JPG.equals(fileSuffix) ||
            	ImageType.T_JPG.equals(ImageType.Util.getFileSuffix(stream))) {
                final JPEGImage image = JPEGImage.read(/*glp, */ stream);
                if (pixelFormat == 0) {
                    pixelFormat = image.getGLFormat();
                }
                if (internalFormat == 0) {
                    if(glp.isGL2ES3()) {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA8:GL.GL_RGB8;
                    } else {
                        internalFormat = (image.getBytesPerPixel()==4)?GL.GL_RGBA:GL.GL_RGB;
                    }
                }
                return new TextureData(glp, internalFormat,
                                       image.getWidth(),
                                       image.getHeight(),
                                       0,
                                       pixelFormat,
                                       image.getGLType(),
                                       mipmap,
                                       false,
                                       false,
                                       image.getData(),
                                       null);
            }

            return null;
        }
    }

    //----------------------------------------------------------------------
    // DDS texture writer
    //
    static class DDSTextureWriter implements TextureWriter {
        @Override
        public boolean write(final File file,
                             final TextureData data) throws IOException {
            if (ImageType.T_DDS.equals(IOUtil.getFileSuffix(file))) {
                // See whether the DDS writer can handle this TextureData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if (pixelType != GL.GL_BYTE &&
                    pixelType != GL.GL_UNSIGNED_BYTE) {
                    throw new IOException("DDS writer only supports byte / unsigned byte textures");
                }

                int d3dFormat = 0;
                // FIXME: some of these are probably not completely correct and would require swizzling
                switch (pixelFormat) {
                    case GL.GL_RGB:                        d3dFormat = DDSImage.D3DFMT_R8G8B8; break;
                    case GL.GL_RGBA:                       d3dFormat = DDSImage.D3DFMT_A8R8G8B8; break;
                    case GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT:  d3dFormat = DDSImage.D3DFMT_DXT1; break;
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT: throw new IOException("RGBA DXT1 not yet supported");
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT: d3dFormat = DDSImage.D3DFMT_DXT3; break;
                    case GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT: d3dFormat = DDSImage.D3DFMT_DXT5; break;
                    default: throw new IOException("Unsupported pixel format 0x" + Integer.toHexString(pixelFormat) + " by DDS writer");
                }

                ByteBuffer[] mipmaps = null;
                if (data.getMipmapData() != null) {
                    mipmaps = new ByteBuffer[data.getMipmapData().length];
                    for (int i = 0; i < mipmaps.length; i++) {
                        mipmaps[i] = (ByteBuffer) data.getMipmapData()[i];
                    }
                } else {
                    mipmaps = new ByteBuffer[] { (ByteBuffer) data.getBuffer() };
                }

                final DDSImage image = DDSImage.createFromData(d3dFormat,
                                                         data.getWidth(),
                                                         data.getHeight(),
                                                         mipmaps);
                image.write(file);
                return true;
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // SGI (rgb) texture writer
    //
    static class SGITextureWriter implements TextureWriter {
        @Override
        public boolean write(final File file,
                             final TextureData data) throws IOException {
            final String fileSuffix = IOUtil.getFileSuffix(file);
            if (SGI.equals(fileSuffix) ||
                ImageType.T_SGI_RGB.equals(fileSuffix)) {
                // See whether the SGI writer can handle this TextureData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if ((pixelFormat == GL.GL_RGB ||
                     pixelFormat == GL.GL_RGBA) &&
                    (pixelType == GL.GL_BYTE ||
                     pixelType == GL.GL_UNSIGNED_BYTE)) {
                    final ByteBuffer buf = ((data.getBuffer() != null) ?
                                      (ByteBuffer) data.getBuffer() :
                                      (ByteBuffer) data.getMipmapData()[0]);
                    byte[] bytes;
                    if (buf.hasArray()) {
                        bytes = buf.array();
                    } else {
                        buf.rewind();
                        bytes = new byte[buf.remaining()];
                        buf.get(bytes);
                        buf.rewind();
                    }

                    final SGIImage image = SGIImage.createFromData(data.getWidth(),
                                                             data.getHeight(),
                                                             (pixelFormat == GL.GL_RGBA),
                                                             bytes);
                    image.write(file, false);
                    return true;
                }

                throw new IOException("SGI writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // TGA (Targa) texture writer

    static class TGATextureWriter implements TextureWriter {
        @Override
        public boolean write(final File file,
                             final TextureData data) throws IOException {
            if (ImageType.T_TGA.equals(IOUtil.getFileSuffix(file))) {
                // See whether the TGA writer can handle this TextureData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                if ((pixelFormat == GL.GL_RGB ||
                     pixelFormat == GL.GL_RGBA ||
                     pixelFormat == GL.GL_BGR ||
                     pixelFormat == GL.GL_BGRA ) &&
                    (pixelType == GL.GL_BYTE ||
                     pixelType == GL.GL_UNSIGNED_BYTE)) {

                    ByteBuffer buf = (ByteBuffer) data.getBuffer();
                    if (null == buf) {
                        buf = (ByteBuffer) data.getMipmapData()[0];
                    }
                    buf.rewind();

                    if( pixelFormat == GL.GL_RGB || pixelFormat == GL.GL_RGBA ) {
                        // Must reverse order of red and blue channels to get correct results
                        final int skip = ((pixelFormat == GL.GL_RGB) ? 3 : 4);
                        for (int i = 0; i < buf.remaining(); i += skip) {
                            final byte red  = buf.get(i + 0);
                            final byte blue = buf.get(i + 2);
                            buf.put(i + 0, blue);
                            buf.put(i + 2, red);
                        }
                    }

                    final TGAImage image = TGAImage.createFromData(data.getWidth(),
                                                             data.getHeight(),
                                                             (pixelFormat == GL.GL_RGBA || pixelFormat == GL.GL_BGRA),
                                                             false, buf);
                    image.write(file);
                    return true;
                }
                throw new IOException("TGA writer doesn't support this pixel format 0x"+Integer.toHexString(pixelFormat)+
                                      " / type 0x"+Integer.toHexString(pixelFormat)+" (only GL_RGB/A, GL_BGR/A + bytes)");
            }

            return false;
        }
    }

    //----------------------------------------------------------------------
    // PNG texture writer

    static class PNGTextureWriter implements TextureWriter {
        @Override
        public boolean write(final File file, final TextureData data) throws IOException {
            if (ImageType.T_PNG.equals(IOUtil.getFileSuffix(file))) {
                // See whether the PNG writer can handle this TextureData
                final GLPixelAttributes pixelAttribs = data.getPixelAttributes();
                final int pixelFormat = pixelAttribs.format;
                final int pixelType   = pixelAttribs.type;
                final int bytesPerPixel = pixelAttribs.pfmt.comp.bytesPerPixel();
                final PixelFormat pixFmt = pixelAttribs.pfmt;
                if ( ( 1 == bytesPerPixel || 3 == bytesPerPixel || 4 == bytesPerPixel) &&
                     ( pixelType == GL.GL_BYTE || pixelType == GL.GL_UNSIGNED_BYTE)) {
                    Buffer buf0 = data.getBuffer();
                    if (null == buf0) {
                        buf0 = data.getMipmapData()[0];
                    }
                    if( null == buf0 ) {
                        throw new IOException("Pixel storage buffer is null");
                    }
                    final DimensionImmutable size = new Dimension(data.getWidth(), data.getHeight());
                    if( buf0 instanceof ByteBuffer ) {
                        final ByteBuffer buf = (ByteBuffer) buf0;
                        buf.rewind();
                        final PNGPixelRect image = new PNGPixelRect(pixFmt, size,
                                                                    0 /* stride */, !data.getMustFlipVertically() /* isGLOriented */, buf /* pixels */,
                                                                    -1f, -1f);
                        final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(file, true /* allowOverwrite */));
                        image.write(outs, true /* close */);
                        return true;
                    } else if( buf0 instanceof IntBuffer ) {
                        final IntBuffer buf = (IntBuffer) buf0;
                        buf.rewind();
                        final OutputStream outs = new BufferedOutputStream(IOUtil.getFileOutputStream(file, true /* allowOverwrite */));
                        PNGPixelRect.write(pixFmt, size,
                                           0 /* stride */, !data.getMustFlipVertically() /* isGLOriented */, buf /* pixels */,
                                           -1f, -1f, outs, true /* closeOutstream */);
                        return true;
                    } else {
                        throw new IOException("PNG writer doesn't support pixel storage buffer of type "+buf0.getClass().getName());
                    }
                }
                throw new IOException("PNG writer doesn't support this pixel format 0x"+Integer.toHexString(pixelFormat)+
                                      " / type 0x"+Integer.toHexString(pixelFormat)+" (only GL_RGB/A, GL_BGR/A + bytes)");
            }
            return false;
        }
    }

    //----------------------------------------------------------------------
    // Helper routines
    //

    private static int glGetTexLevelParameteri(final GL2GL3 gl, final int target, final int level, final int pname) {
        final int[] tmp = new int[1];
        gl.glGetTexLevelParameteriv(target, 0, pname, tmp, 0);
        return tmp[0];
    }

    private static String toLowerCase(final String arg) {
        if (arg == null) {
            return null;
        }

        return arg.toLowerCase();
    }
}
