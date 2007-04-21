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

package com.sun.opengl.util.texture;

import java.awt.Graphics;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;

import javax.media.opengl.*;
import javax.media.opengl.glu.*;
import com.sun.opengl.impl.Debug;
import com.sun.opengl.util.*;
import com.sun.opengl.util.texture.spi.*;

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
      DirectDraw Surface file. */
  public static final String DDS     = "dds";

  /** Constant which can be used as a file suffix to indicate an SGI
      RGB file. */
  public static final String SGI     = "sgi";

  /** Constant which can be used as a file suffix to indicate an SGI
      RGB file. */
  public static final String SGI_RGB = "rgb";

  /** Constant which can be used as a file suffix to indicate a GIF
      file. */
  public static final String GIF     = "gif";

  /** Constant which can be used as a file suffix to indicate a JPEG
      file. */
  public static final String JPG     = "jpg";

  /** Constant which can be used as a file suffix to indicate a PNG
      file. */
  public static final String PNG     = "png";

  /** Constant which can be used as a file suffix to indicate a Targa
      file. */
  public static final String TGA     = "tga";

  /** Constant which can be used as a file suffix to indicate a TIFF
      file. */
  public static final String TIFF    = "tiff";

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
  public static TextureData newTextureData(File file,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException {
    if (fileSuffix == null) {
      fileSuffix = FileUtil.getFileSuffix(file);
    }
    return newTextureDataImpl(file, 0, 0, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given stream. Does no OpenGL work.
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
   * @return the texture data from the stream, or null if none of the
   *         registered texture providers could read the stream
   * @throws IOException if an error occurred while reading the stream
   */
  public static TextureData newTextureData(InputStream stream,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException {
    return newTextureDataImpl(stream, 0, 0, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given URL. Does no OpenGL work.
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
   * @return the texture data from the URL, or null if none of the
   *         registered texture providers could read the URL
   * @throws IOException if an error occurred while reading the URL
   */
  public static TextureData newTextureData(URL url,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException {
    if (fileSuffix == null) {
      fileSuffix = FileUtil.getFileSuffix(url.getPath());
    }
    return newTextureDataImpl(url, 0, 0, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given BufferedImage. Does no
   * OpenGL work.
   *
   * @param image the BufferedImage containing the texture data
   * @param mipmap     whether mipmaps should be produced for this
   *                   texture by autogenerating them
   * @return the texture data from the image
   */
  public static TextureData newTextureData(BufferedImage image,
                                           boolean mipmap) {
    return newTextureDataImpl(image, 0, 0, mipmap);
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
  public static TextureData newTextureData(File file,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    if (fileSuffix == null) {
      fileSuffix = FileUtil.getFileSuffix(file);
    }

    return newTextureDataImpl(file, internalFormat, pixelFormat, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given stream, using the specified
   * OpenGL internal format and pixel format for the texture which
   * will eventually result. The internalFormat and pixelFormat must
   * be specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
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
  public static TextureData newTextureData(InputStream stream,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(stream, internalFormat, pixelFormat, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given URL, using the specified
   * OpenGL internal format and pixel format for the texture which
   * will eventually result. The internalFormat and pixelFormat must
   * be specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
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
  public static TextureData newTextureData(URL url,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    if (fileSuffix == null) {
      fileSuffix = FileUtil.getFileSuffix(url.getPath());
    }

    return newTextureDataImpl(url, internalFormat, pixelFormat, mipmap, fileSuffix);
  }

  /**
   * Creates a TextureData from the given BufferedImage, using the
   * specified OpenGL internal format and pixel format for the texture
   * which will eventually result. The internalFormat and pixelFormat
   * must be specified and may not be zero; to use default values, use
   * the variant of this method which does not take these
   * arguments. Does no OpenGL work.
   *
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
  public static TextureData newTextureData(BufferedImage image,
                                           int internalFormat,
                                           int pixelFormat,
                                           boolean mipmap) throws IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(image, internalFormat, pixelFormat, mipmap);
  }

  //----------------------------------------------------------------------
  // methods that *do* require a current context
  //

  /** 
   * Creates an OpenGL texture object from the specified TextureData
   * using the current OpenGL context. Does not automatically generate
   * mipmaps for the resulting texture.
   *
   * @param data the texture data to turn into an OpenGL texture
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   * @throws IllegalArgumentException if the passed TextureData was null
   */
  public static Texture newTexture(TextureData data) throws GLException, IllegalArgumentException {
    if (data == null) {
      throw new IllegalArgumentException("Null TextureData");
    }
    return new Texture(data);
  }

  /** 
   * Creates an OpenGL texture object from the specified file using
   * the current OpenGL context. Does not automatically generate
   * mipmaps for the resulting texture.
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
  public static Texture newTexture(File file, boolean mipmap) throws IOException, GLException {
    TextureData data = newTextureData(file, mipmap, FileUtil.getFileSuffix(file));
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified stream using
   * the current OpenGL context. Does not automatically generate
   * mipmaps for the resulting texture.
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
  public static Texture newTexture(InputStream stream, boolean mipmap, String fileSuffix) throws IOException, GLException {
    TextureData data = newTextureData(stream, mipmap, fileSuffix);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified URL using the
   * current OpenGL context. Does not automatically generate mipmaps
   * for the resulting texture.
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
  public static Texture newTexture(URL url, boolean mipmap, String fileSuffix) throws IOException, GLException {
    if (fileSuffix == null) {
      fileSuffix = FileUtil.getFileSuffix(url.getPath());
    }
    TextureData data = newTextureData(url, mipmap, fileSuffix);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified BufferedImage
   * using the current OpenGL context.
   *
   * @param image the BufferedImage from which to read the texture data
   * @param mipmap     whether mipmaps should be produced for this
   *                   texture by autogenerating them
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(BufferedImage image, boolean mipmap) throws GLException {
    TextureData data = newTextureData(image, mipmap);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object associated with the given OpenGL
   * texture target using the current OpenGL context. The texture has
   * no initial data. This is used, for example, to construct cube
   * maps out of multiple TextureData objects.
   *
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(int target) throws GLException {
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
   * @throws IOException if an error occurred during writing or no
   *   suitable writer was found
   * @throws GLException if no OpenGL context was current or an
   *   OpenGL-related error occurred
   */
  public static void write(Texture texture, File file) throws IOException, GLException {
    if (texture.getTarget() != GL.GL_TEXTURE_2D) {
      throw new GLException("Only GL_TEXTURE_2D textures are supported");
    }

    // First fetch the texture data
    GL gl = GLU.getCurrentGL();

    texture.bind();
    int internalFormat = glGetTexLevelParameteri(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_INTERNAL_FORMAT);
    int width  = glGetTexLevelParameteri(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_WIDTH);
    int height = glGetTexLevelParameteri(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_HEIGHT);
    int border = glGetTexLevelParameteri(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_BORDER);
    TextureData data = null;
    if (internalFormat == GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT ||
        internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT ||
        internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT ||
        internalFormat == GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT) {
      // Fetch using glGetCompressedTexImage
      int size   = glGetTexLevelParameteri(GL.GL_TEXTURE_2D, 0, GL.GL_TEXTURE_COMPRESSED_IMAGE_SIZE);
      ByteBuffer res = ByteBuffer.allocate(size);
      gl.glGetCompressedTexImage(GL.GL_TEXTURE_2D, 0, res);
      data = new TextureData(internalFormat, width, height, border, internalFormat, GL.GL_UNSIGNED_BYTE,
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
        case GL.GL_ABGR_EXT:
        case GL.GL_RGBA8:
          bytesPerPixel = 4;
          fetchedFormat = GL.GL_RGBA;
          break;
        default:
          throw new IOException("Unsupported texture internal format 0x" + Integer.toHexString(internalFormat));
      }

      // Fetch using glGetTexImage
      int packAlignment  = glGetInteger(GL.GL_PACK_ALIGNMENT);
      int packRowLength  = glGetInteger(GL.GL_PACK_ROW_LENGTH);
      int packSkipRows   = glGetInteger(GL.GL_PACK_SKIP_ROWS);
      int packSkipPixels = glGetInteger(GL.GL_PACK_SKIP_PIXELS);
      int packSwapBytes  = glGetInteger(GL.GL_PACK_SWAP_BYTES);

      gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
      gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, 0);
      gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, 0);
      gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, 0);
      gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, 0);

      ByteBuffer res = ByteBuffer.allocate((width + (2 * border)) *
                                           (height + (2 * border)) *
                                           bytesPerPixel);
      if (DEBUG) {
        System.out.println("Allocated buffer of size " + res.remaining() + " for fetched image (" +
                           ((fetchedFormat == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA") + ")");
      }
      gl.glGetTexImage(GL.GL_TEXTURE_2D, 0, fetchedFormat, GL.GL_UNSIGNED_BYTE, res);

      gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, packAlignment);
      gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, packRowLength);
      gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, packSkipRows);
      gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, packSkipPixels);
      gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, packSwapBytes);
      
      data = new TextureData(internalFormat, width, height, border, fetchedFormat, GL.GL_UNSIGNED_BYTE,
                             false, false, false, res, null);

      if (DEBUG) {
        System.out.println("data.getPixelFormat() = " +
                           ((data.getPixelFormat() == GL.GL_RGB) ? "GL_RGB" : "GL_RGBA"));
      }
    }

    for (Iterator iter = textureWriters.iterator(); iter.hasNext(); ) {
      TextureWriter writer = (TextureWriter) iter.next();
      if (writer.write(file, data)) {
        return;
      }
    }

    throw new IOException("No suitable texture writer found");
  }
  
  //----------------------------------------------------------------------
  // SPI support
  //

  /** Adds a TextureProvider to support reading of a new file
      format. */
  public static void addTextureProvider(TextureProvider provider) {
    // Must always add at the front so the ImageIO provider is last,
    // so we don't accidentally use it instead of a user's possibly
    // more optimal provider
    textureProviders.add(0, provider);
  }

  /** Adds a TextureWriter to support writing of a new file
      format. */
  public static void addTextureWriter(TextureWriter writer) {
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
  public static void setTexRectEnabled(boolean enabled) {
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

  private static List/*<TextureProvider>*/ textureProviders = new ArrayList/*<TextureProvider>*/();
  private static List/*<TextureWriter>*/   textureWriters   = new ArrayList/*<TextureWriter>*/();

  static {
    // ImageIO provider, the fall-back, must be the first one added
    addTextureProvider(new IIOTextureProvider());

    // Other special-case providers
    addTextureProvider(new DDSTextureProvider());
    addTextureProvider(new SGITextureProvider());
    addTextureProvider(new TGATextureProvider());

    // ImageIO writer, the fall-back, must be the first one added
    textureWriters.add(new IIOTextureWriter());

    // Other special-case writers
    addTextureWriter(new DDSTextureWriter());
    addTextureWriter(new SGITextureWriter());
    addTextureWriter(new TGATextureWriter());
  }

  // Implementation methods
  private static TextureData newTextureDataImpl(File file,
                                                int internalFormat,
                                                int pixelFormat,
                                                boolean mipmap,
                                                String fileSuffix) throws IOException {
    if (file == null) {
      throw new IOException("File was null");
    }

    fileSuffix = toLowerCase(fileSuffix);

    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(file,
                                                 internalFormat,
                                                 pixelFormat,
                                                 mipmap,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }

    throw new IOException("No suitable reader for given file");
  }

  private static TextureData newTextureDataImpl(InputStream stream,
                                                int internalFormat,
                                                int pixelFormat,
                                                boolean mipmap,
                                                String fileSuffix) throws IOException {
    if (stream == null) {
      throw new IOException("Stream was null");
    }

    fileSuffix = toLowerCase(fileSuffix);

    // Note: use of BufferedInputStream works around 4764639/4892246
    if (!(stream instanceof BufferedInputStream)) {
      stream = new BufferedInputStream(stream);
    }

    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(stream,
                                                 internalFormat,
                                                 pixelFormat,
                                                 mipmap,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }

    throw new IOException("No suitable reader for given stream");
  }

  private static TextureData newTextureDataImpl(URL url,
                                                int internalFormat,
                                                int pixelFormat,
                                                boolean mipmap,
                                                String fileSuffix) throws IOException {
    if (url == null) {
      throw new IOException("URL was null");
    }

    fileSuffix = toLowerCase(fileSuffix);

    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(url,
                                                 internalFormat,
                                                 pixelFormat,
                                                 mipmap,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }

    throw new IOException("No suitable reader for given URL");
  }

  private static TextureData newTextureDataImpl(BufferedImage image,
                                                int internalFormat,
                                                int pixelFormat,
                                                boolean mipmap) {
    return new TextureData(internalFormat, pixelFormat, mipmap, image);
  }

  //----------------------------------------------------------------------
  // Base provider - used last
  static class IIOTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      BufferedImage img = ImageIO.read(file);
      if (img == null) {
        return null;
      }
      if (DEBUG) {
        System.out.println("TextureIO.newTextureData(): BufferedImage type for " + file + " = " +
                           img.getType());
      }
      return new TextureData(internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(InputStream stream,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      BufferedImage img = ImageIO.read(stream);
      if (img == null) {
        return null;
      }
      if (DEBUG) {
        System.out.println("TextureIO.newTextureData(): BufferedImage type for stream = " +
                           img.getType());
      }
      return new TextureData(internalFormat, pixelFormat, mipmap, img);
    }

    public TextureData newTextureData(URL url,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      InputStream stream = url.openStream();
      try {
        return newTextureData(stream, internalFormat, pixelFormat, mipmap, fileSuffix);
      } finally {
        stream.close();
      }
    }
  }

  //----------------------------------------------------------------------
  // DDS provider -- supports files only for now
  static class DDSTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      if (DDS.equals(fileSuffix) ||
          DDS.equals(FileUtil.getFileSuffix(file))) {
        DDSImage image = DDSImage.read(file);
        return newTextureData(image, internalFormat, pixelFormat, mipmap);
      }

      return null;
    }

    public TextureData newTextureData(InputStream stream,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      if (DDS.equals(fileSuffix) ||
          DDSImage.isDDSImage(stream)) {
        byte[] data = StreamUtil.readAll(stream);
        ByteBuffer buf = ByteBuffer.wrap(data);
        DDSImage image = DDSImage.read(buf);
        return newTextureData(image, internalFormat, pixelFormat, mipmap);
      }

      return null;
    }

    public TextureData newTextureData(URL url,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      InputStream stream = new BufferedInputStream(url.openStream());
      try {
        return newTextureData(stream, internalFormat, pixelFormat, mipmap, fileSuffix);
      } finally {
        stream.close();
      }
    }

    private TextureData newTextureData(final DDSImage image,
                                       int internalFormat,
                                       int pixelFormat,
                                       boolean mipmap) {
      DDSImage.ImageInfo info = image.getMipMap(0);
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
      TextureData.Flusher flusher = new TextureData.Flusher() {
          public void flush() {
            image.close();
          }
        };
      TextureData data;
      if (mipmap && image.getNumMipMaps() > 0) {
        Buffer[] mipmapData = new Buffer[image.getNumMipMaps()];
        for (int i = 0; i < image.getNumMipMaps(); i++) {
          mipmapData[i] = image.getMipMap(i).getData();
        }
        data = new TextureData(internalFormat,
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
        data = new TextureData(internalFormat,
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
  // Base class for SGI RGB and TGA image providers
  static abstract class StreamBasedTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      InputStream inStream = new BufferedInputStream(new FileInputStream(file));
      try {
        // The SGIImage and TGAImage implementations use InputStreams
        // anyway so there isn't much point in having a separate code
        // path for files
        return newTextureData(inStream,
                              internalFormat,
                              pixelFormat,
                              mipmap,
                              ((fileSuffix != null) ? fileSuffix : FileUtil.getFileSuffix(file)));
      } finally {
        inStream.close();
      }
    }

    public TextureData newTextureData(URL url,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      InputStream stream = new BufferedInputStream(url.openStream());
      try {
        return newTextureData(stream, internalFormat, pixelFormat, mipmap, fileSuffix);
      } finally {
        stream.close();
      }
    }
  }

  //----------------------------------------------------------------------
  // SGI RGB image provider
  static class SGITextureProvider extends StreamBasedTextureProvider {
    public TextureData newTextureData(InputStream stream,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      if (SGI.equals(fileSuffix) ||
          SGI_RGB.equals(fileSuffix) ||
          SGIImage.isSGIImage(stream)) {
        SGIImage image = SGIImage.read(stream);
        if (pixelFormat == 0) {
          pixelFormat = image.getFormat();
        }
        if (internalFormat == 0) {
          internalFormat = image.getFormat();
        }
        return new TextureData(internalFormat,
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
    public TextureData newTextureData(InputStream stream,
                                      int internalFormat,
                                      int pixelFormat,
                                      boolean mipmap,
                                      String fileSuffix) throws IOException {
      if (TGA.equals(fileSuffix)) {
        TGAImage image = TGAImage.read(stream);
        if (pixelFormat == 0) {
          pixelFormat = image.getGLFormat();
        }
        if (internalFormat == 0) {
          internalFormat = GL.GL_RGBA8;
        }
        return new TextureData(internalFormat,
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
  // ImageIO texture writer
  //
  static class IIOTextureWriter implements TextureWriter {
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
        if (JPG.equals(FileUtil.getFileSuffix(file)) &&
            image.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
          BufferedImage tmpImage = new BufferedImage(image.getWidth(), image.getHeight(),
                                                     BufferedImage.TYPE_3BYTE_BGR);
          Graphics g = tmpImage.getGraphics();
          g.drawImage(image, 0, 0, null);
          g.dispose();
          image = tmpImage;
        }

        return ImageIO.write(image, FileUtil.getFileSuffix(file), file);
      }
      
      throw new IOException("ImageIO writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
    }
  }

  //----------------------------------------------------------------------
  // DDS texture writer
  //
  static class DDSTextureWriter implements TextureWriter {
    public boolean write(File file,
                         TextureData data) throws IOException {
      if (DDS.equals(FileUtil.getFileSuffix(file))) {
        // See whether the DDS writer can handle this TextureData
        int pixelFormat = data.getPixelFormat();
        int pixelType   = data.getPixelType();
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

        DDSImage image = DDSImage.createFromData(d3dFormat,
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
    public boolean write(File file,
                         TextureData data) throws IOException {
      String fileSuffix = FileUtil.getFileSuffix(file);
      if (SGI.equals(fileSuffix) ||
          SGI_RGB.equals(fileSuffix)) {
        // See whether the SGI writer can handle this TextureData
        int pixelFormat = data.getPixelFormat();
        int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {
          ByteBuffer buf = ((data.getBuffer() != null) ?
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

          SGIImage image = SGIImage.createFromData(data.getWidth(),
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
    public boolean write(File file,
                         TextureData data) throws IOException {
      if (TGA.equals(FileUtil.getFileSuffix(file))) {
        // See whether the TGA writer can handle this TextureData
        int pixelFormat = data.getPixelFormat();
        int pixelType   = data.getPixelType();
        if ((pixelFormat == GL.GL_RGB ||
             pixelFormat == GL.GL_RGBA) &&
            (pixelType == GL.GL_BYTE ||
             pixelType == GL.GL_UNSIGNED_BYTE)) {
          ByteBuffer buf = ((data.getBuffer() != null) ?
                            (ByteBuffer) data.getBuffer() :
                            (ByteBuffer) data.getMipmapData()[0]);
          // Must reverse order of red and blue channels to get correct results
          int skip = ((pixelFormat == GL.GL_RGB) ? 3 : 4);
          for (int i = 0; i < buf.remaining(); i += skip) {
            byte red  = buf.get(i + 0);
            byte blue = buf.get(i + 2);
            buf.put(i + 0, blue);
            buf.put(i + 2, red);
          }

          TGAImage image = TGAImage.createFromData(data.getWidth(),
                                                   data.getHeight(),
                                                   (pixelFormat == GL.GL_RGBA),
                                                   false,
                                                   ((data.getBuffer() != null) ?
                                                    (ByteBuffer) data.getBuffer() :
                                                    (ByteBuffer) data.getMipmapData()[0]));
          image.write(file);
          return true;
        }

        throw new IOException("TGA writer doesn't support this pixel format / type (only GL_RGB/A + bytes)");
      }

      return false;
    }    
  }

  //----------------------------------------------------------------------
  // Helper routines
  //

  private static int glGetInteger(int pname) {
    int[] tmp = new int[1];
    GL gl = GLU.getCurrentGL();
    gl.glGetIntegerv(pname, tmp, 0);
    return tmp[0];
  }

  private static int glGetTexLevelParameteri(int target, int level, int pname) {
    int[] tmp = new int[1];
    GL gl = GLU.getCurrentGL();
    gl.glGetTexLevelParameteriv(target, 0, pname, tmp, 0);
    return tmp[0];
  }

  private static String toLowerCase(String arg) {
    if (arg == null) {
      return null;
    }

    return arg.toLowerCase();
  }
}
