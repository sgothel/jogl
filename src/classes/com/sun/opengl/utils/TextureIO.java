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

package com.sun.opengl.utils;

import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.nio.*;
import java.util.*;
import javax.imageio.*;

import javax.media.opengl.*;

/** <P> Provides input and output facilities for both loading OpenGL
    textures from disk and streams as well as writing textures already
    in memory back to disk. </P>

    <P> The TextureIO class supports an arbitrary number of plug-in
    TextureProviders which know how to produce TextureData objects
    from files, InputStreams and URLs. The TextureData class
    represents the raw data of the texture before it has been
    converted to an OpenGL texture object. The Texture class
    represents the OpenGL texture object and provides easy facilities
    for using the texture. </P>

    <P> There are several built-in TextureProviders supplied with the
    TextureIO implementation. The most basic provider uses the
    platform's Image I/O facilities to read in a BufferedImage and
    convert it to a texture. This is the baseline provider and is
    registered so that it is the last one consulted. All others are
    asked first to open a given file. </P>

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

  //----------------------------------------------------------------------
  // methods that *do not* require a current context
  // These methods assume RGB or RGBA textures.
  // Some texture providers may not recognize the file format unless
  // the fileSuffix is specified, so it is strongly recommended to
  // specify it wherever it is known.
  // Some texture providers may also only support one kind of input,
  // i.e., reading from a file as opposed to a stream.

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given file. Does no OpenGL work.
   *
   * @param file the file from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
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
                                           int mipmapLevel,
                                           String fileSuffix) throws IOException {
    return newTextureDataImpl(file, mipmapLevel, 0, 0, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given stream. Does no OpenGL work.
   *
   * @param stream the stream from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
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
                                           int mipmapLevel,
                                           String fileSuffix) throws IOException {
    return newTextureDataImpl(stream, mipmapLevel, 0, 0, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given URL. Does no OpenGL work.
   *
   * @param url the URL from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
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
                                           int mipmapLevel,
                                           String fileSuffix) throws IOException {
    return newTextureDataImpl(url, mipmapLevel, 0, 0, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given BufferedImage. Does no OpenGL work.
   *
   * @param image the BufferedImage containing the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
   * @return the texture data from the image
   */
  public static TextureData newTextureData(BufferedImage image,
                                           int mipmapLevel) {
    return newTextureDataImpl(image, mipmapLevel, 0, 0);
  }

  //----------------------------------------------------------------------
  // These methods make no assumption about the OpenGL internal format
  // or pixel format of the texture; they must be specified by the
  // user. It is not allowed to supply 0 (indicating no preference)
  // for either the internalFormat or the pixelFormat;
  // IllegalArgumentException will be thrown in this case.

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given file, using the specified OpenGL
   * internal format and pixel format for the texture which will
   * eventually result. The internalFormat and pixelFormat must be
   * specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
   * @param file the file from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
   * @param internalFormat the OpenGL internal format of the texture
   *                   which will eventually result from the TextureData
   * @param pixelFormat the OpenGL pixel format of the texture
   *                    which will eventually result from the TextureData
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
                                           int mipmapLevel,
                                           int internalFormat,
                                           int pixelFormat,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(file, mipmapLevel, internalFormat, pixelFormat, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given stream, using the specified OpenGL
   * internal format and pixel format for the texture which will
   * eventually result. The internalFormat and pixelFormat must be
   * specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
   * @param stream the stream from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
   * @param internalFormat the OpenGL internal format of the texture
   *                   which will eventually result from the TextureData
   * @param pixelFormat the OpenGL pixel format of the texture
   *                    which will eventually result from the TextureData
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
                                           int mipmapLevel,
                                           int internalFormat,
                                           int pixelFormat,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(stream, mipmapLevel, internalFormat, pixelFormat, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given URL, using the specified OpenGL
   * internal format and pixel format for the texture which will
   * eventually result. The internalFormat and pixelFormat must be
   * specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
   * @param url the URL from which to read the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
   * @param internalFormat the OpenGL internal format of the texture
   *                   which will eventually result from the TextureData
   * @param pixelFormat the OpenGL pixel format of the texture
   *                    which will eventually result from the TextureData
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
                                           int mipmapLevel,
                                           int internalFormat,
                                           int pixelFormat,
                                           String fileSuffix) throws IOException, IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(url, mipmapLevel, internalFormat, pixelFormat, fileSuffix);
  }

  /**
   * Creates a TextureData representing the specified mipmap level of
   * a texture from the given BufferedImage, using the specified
   * OpenGL internal format and pixel format for the texture which
   * will eventually result. The internalFormat and pixelFormat must
   * be specified and may not be zero; to use default values, use the
   * variant of this method which does not take these arguments. Does
   * no OpenGL work.
   *
   * @param image the BufferedImage containing the texture data
   * @param mipmapLevel the mipmap level this data represents (FIXME:
   *                    not currently used, needs to be rethought)
   * @param internalFormat the OpenGL internal format of the texture
   *                   which will eventually result from the TextureData
   * @param pixelFormat the OpenGL pixel format of the texture
   *                    which will eventually result from the TextureData
   * @return the texture data from the image
   * @throws IllegalArgumentException if either internalFormat or
   *                                  pixelFormat was 0
   */
  public static TextureData newTextureData(BufferedImage image,
                                           int mipmapLevel,
                                           int internalFormat,
                                           int pixelFormat) throws IllegalArgumentException {
    if ((internalFormat == 0) || (pixelFormat == 0)) {
      throw new IllegalArgumentException("internalFormat and pixelFormat must be non-zero");
    }

    return newTextureDataImpl(image, mipmapLevel, internalFormat, pixelFormat);
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
   */
  public static Texture newTexture(TextureData data) throws GLException {
    if (data == null) {
      return null;
    }
    return new Texture(data);
  }

  /** 
   * Creates an OpenGL texture object from the specified file using
   * the current OpenGL context.
   *
   * @param file the file from which to read the texture data
   * @throws IOException if an error occurred while reading the file
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(File file) throws IOException, GLException {
    TextureData data = newTextureData(file, 0, getFileSuffix(file));
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified stream using
   * the current OpenGL context.
   *
   * @param stream the stream from which to read the texture data
   * @throws IOException if an error occurred while reading the stream
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(InputStream stream) throws IOException, GLException {
    TextureData data = newTextureData(stream, 0, null);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified URL using
   * the current OpenGL context.
   *
   * @param url the URL from which to read the texture data
   * @throws IOException if an error occurred while reading the URL
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(URL url) throws IOException, GLException {
    TextureData data = newTextureData(url, 0, null);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  /** 
   * Creates an OpenGL texture object from the specified BufferedImage
   * using the current OpenGL context.
   *
   * @param image the BufferedImage from which to read the texture data
   * @throws GLException if no OpenGL context is current or if an
   *                     OpenGL error occurred
   */
  public static Texture newTexture(BufferedImage image) throws GLException {
    TextureData data = newTextureData(image, 0);
    Texture texture = newTexture(data);
    data.flush();
    return texture;
  }

  // FIXME: add texture writing capabilities
  //  public void writeTextureToFile(Texture texture, File file, boolean saveUncompressed) throws IOException, GLException;

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

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private static List/*<TextureProvider>*/ textureProviders = new ArrayList/*<TextureProvider>*/();

  static {
    // ImageIO provider, the fall-back, must be the first one added
    addTextureProvider(new IIOTextureProvider());

    // Other special-case providers
    addTextureProvider(new DDSTextureProvider());
    addTextureProvider(new SGITextureProvider());
    addTextureProvider(new TGATextureProvider());
  }

  // Implementation methods
  private static TextureData newTextureDataImpl(File file,
                                                int mipmapLevel,
                                                int internalFormat,
                                                int pixelFormat,
                                                String fileSuffix) throws IOException {
    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(file,
                                                 mipmapLevel,
                                                 internalFormat,
                                                 pixelFormat,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }
    return null;
  }

  private static TextureData newTextureDataImpl(InputStream stream,
                                                int mipmapLevel,
                                                int internalFormat,
                                                int pixelFormat,
                                                String fileSuffix) throws IOException {
    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(stream,
                                                 mipmapLevel,
                                                 internalFormat,
                                                 pixelFormat,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }

    return null;
  }

  private static TextureData newTextureDataImpl(URL url,
                                                int mipmapLevel,
                                                int internalFormat,
                                                int pixelFormat,
                                                String fileSuffix) throws IOException {
    for (Iterator iter = textureProviders.iterator(); iter.hasNext(); ) {
      TextureProvider provider = (TextureProvider) iter.next();
      TextureData data = provider.newTextureData(url,
                                                 mipmapLevel,
                                                 internalFormat,
                                                 pixelFormat,
                                                 fileSuffix);
      if (data != null) {
        return data;
      }
    }

    return null;
  }

  private static TextureData newTextureDataImpl(BufferedImage image,
                                                int mipmapLevel,
                                                int internalFormat,
                                                int pixelFormat) {
    return new TextureData(mipmapLevel, internalFormat, pixelFormat, image);
  }

  //----------------------------------------------------------------------
  // Base provider - used last
  static class IIOTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      BufferedImage img = ImageIO.read(file);
      if (img == null) {
        return null;
      }
      return new TextureData(mipmapLevel, internalFormat, pixelFormat, img);
    }

    public TextureData newTextureData(InputStream stream,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      BufferedImage img = ImageIO.read(stream);
      if (img == null) {
        return null;
      }
      return new TextureData(mipmapLevel, internalFormat, pixelFormat, img);
    }

    public TextureData newTextureData(URL url,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      InputStream stream = url.openStream();
      try {
        return newTextureData(stream, mipmapLevel, internalFormat, pixelFormat, fileSuffix);
      } finally {
        stream.close();
      }
    }
  }

  //----------------------------------------------------------------------
  // DDS provider -- supports files only for now
  static class DDSTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      if (DDS.equals(fileSuffix) ||
          DDS.equals(getFileSuffix(file))) {
        final DDSReader reader = new DDSReader();
        reader.loadFile(file);
        // FIXME: handle case where all mipmaps are requested -- this
        // will require API changes
        DDSReader.ImageInfo info = reader.getMipMap(mipmapLevel);
        if (pixelFormat == 0) {
          switch (reader.getPixelFormat()) {
            case DDSReader.D3DFMT_R8G8B8:
              pixelFormat = GL.GL_RGB;
              break;
            default:
              pixelFormat = GL.GL_RGBA;
              break;
          }
        }
        if (info.isCompressed()) {
          switch (info.getCompressionFormat()) {
            case DDSReader.D3DFMT_DXT1:
              internalFormat = GL.GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
              break;
            case DDSReader.D3DFMT_DXT3:
              internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT3_EXT;
              break;
            case DDSReader.D3DFMT_DXT5:
              internalFormat = GL.GL_COMPRESSED_RGBA_S3TC_DXT5_EXT;
              break;
            default:
              throw new RuntimeException("Unsupported DDS compression format \"" +
                                         DDSReader.getCompressionFormatName(info.getCompressionFormat()) + "\"");
          }
        }
        if (internalFormat == 0) {
          switch (reader.getPixelFormat()) {
            case DDSReader.D3DFMT_R8G8B8:
              pixelFormat = GL.GL_RGB;
              break;
            default:
              pixelFormat = GL.GL_RGBA;
              break;
          }
        }
        TextureData.Flusher flusher = new TextureData.Flusher() {
            public void flush() {
              reader.close();
            }
          };
        TextureData data = new TextureData(mipmapLevel,
                                           internalFormat,
                                           info.getWidth(),
                                           info.getHeight(),
                                           0,
                                           pixelFormat,
                                           GL.GL_UNSIGNED_BYTE,
                                           info.isCompressed(),
                                           true,
                                           info.getData(),
                                           flusher);
        return data;
      }

      return null;
    }

    public TextureData newTextureData(InputStream stream,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      return null;
    }

    public TextureData newTextureData(URL url,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      return null;
    }
  }

  //----------------------------------------------------------------------
  // Base class for SGI RGB and TGA image providers
  static abstract class StreamBasedTextureProvider implements TextureProvider {
    public TextureData newTextureData(File file,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      InputStream inStream = new BufferedInputStream(new FileInputStream(file));
      try {
        // The SGIImage and TGAImage implementations use InputStreams
        // anyway so there isn't much point in having a separate code
        // path for files
        return newTextureData(inStream,
                              mipmapLevel,
                              internalFormat,
                              pixelFormat,
                              ((fileSuffix != null) ? fileSuffix : getFileSuffix(file)));
      } finally {
        inStream.close();
      }
    }

    public TextureData newTextureData(URL url,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      InputStream stream = url.openStream();
      try {
        return newTextureData(stream, mipmapLevel, internalFormat, pixelFormat, fileSuffix);
      } finally {
        stream.close();
      }
    }
  }

  //----------------------------------------------------------------------
  // SGI RGB image provider
  static class SGITextureProvider extends StreamBasedTextureProvider {
    public TextureData newTextureData(InputStream stream,
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
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
        return new TextureData(mipmapLevel,
                               internalFormat,
                               image.getWidth(),
                               image.getHeight(),
                               0,
                               pixelFormat,
                               GL.GL_UNSIGNED_BYTE,
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
                                      int mipmapLevel,
                                      int internalFormat,
                                      int pixelFormat,
                                      String fileSuffix) throws IOException {
      if (TGA.equals(fileSuffix)) {
        TGAImage image = TGAImage.read(stream);
        if (pixelFormat == 0) {
          pixelFormat = image.getGLFormat();
        }
        if (internalFormat == 0) {
          internalFormat = GL.GL_RGBA8;
        }
        return new TextureData(mipmapLevel,
                               internalFormat,
                               image.getWidth(),
                               image.getHeight(),
                               0,
                               pixelFormat,
                               GL.GL_UNSIGNED_BYTE,
                               false,
                               false,
                               ByteBuffer.wrap(image.getData()),
                               null);
      }

      return null;
    }
  }

  //----------------------------------------------------------------------
  // Helper function for above TextureProviders
  private static String getFileSuffix(File file) {
    String name = file.getName().toLowerCase();

    int lastDot = name.lastIndexOf('.');
    if (lastDot < 0) {
      return null;
    }
    return name.substring(lastDot + 1);
  }
}
