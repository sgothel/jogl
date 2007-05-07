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
 */

package com.sun.opengl.util.texture;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.*;
import java.awt.image.*;
import java.nio.*;

import javax.media.opengl.*;
import com.sun.opengl.util.*;

/**
 * Represents the data for an OpenGL texture. This is separated from
 * the notion of a Texture to support things like streaming in of
 * textures in a background thread without requiring an OpenGL context
 * to be current on that thread.
 *
 * @author Chris Campbell
 * @author Kenneth Russell
 */

public class TextureData {
  private int width;
  private int height;
  private int border;
  private int pixelFormat;
  private int pixelType;
  private int internalFormat; // perhaps inferred from pixelFormat?
  private boolean mipmap; // indicates whether mipmaps should be generated
                          // (ignored if mipmaps are supplied from the file)
  private boolean dataIsCompressed;
  private boolean mustFlipVertically; // Must flip texture coordinates
                                      // vertically to get OpenGL output
                                      // to look correct
  private Buffer buffer; // the actual data...
  private Buffer[] mipmapData; // ...or a series of mipmaps
  private Flusher flusher;
  private int rowLength;
  private int alignment; // 1, 2, or 4 bytes
  private int estimatedMemorySize;

  // Mechanism for lazily converting input BufferedImages with custom
  // ColorModels to standard ones for uploading to OpenGL, as well as
  // backing off from the optimizations of hoping that either
  // GL_EXT_abgr or OpenGL 1.2 are present
  private BufferedImage imageForLazyCustomConversion;
  private boolean expectingEXTABGR;
  private boolean haveEXTABGR;
  private boolean expectingGL12;
  private boolean haveGL12;

  private static final ColorModel rgbaColorModel =
    new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            new int[] {8, 8, 8, 8}, true, true, 
                            Transparency.TRANSLUCENT,
                            DataBuffer.TYPE_BYTE);
  private static final ColorModel rgbColorModel =
    new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                            new int[] {8, 8, 8, 0}, false, false,
                            Transparency.OPAQUE,
                            DataBuffer.TYPE_BYTE);

  /** 
   * Constructs a new TextureData object with the specified parameters
   * and data contained in the given Buffer. The optional Flusher can
   * be used to clean up native resources associated with this
   * TextureData when processing is complete; for example, closing of
   * memory-mapped files that might otherwise require a garbage
   * collection to reclaim and close.
   *
   * @param internalFormat the OpenGL internal format for the
   *                       resulting texture; must be specified, may
   *                       not be 0
   * @param width          the width in pixels of the texture
   * @param height         the height in pixels of the texture
   * @param border         the number of pixels of border this texture
   *                       data has (0 or 1)
   * @param pixelFormat    the OpenGL pixel format for the
   *                       resulting texture; must be specified, may
   *                       not be 0
   * @param pixelType      the OpenGL type of the pixels of the texture
   * @param mipmap         indicates whether mipmaps should be
   *                       autogenerated (using GLU) for the resulting
   *                       texture. Currently if mipmap is true then
   *                       dataIsCompressed may not be true.
   * @param dataIsCompressed indicates whether the texture data is in
   *                       compressed form
   *                       (e.g. GL_COMPRESSED_RGB_S3TC_DXT1_EXT)
   * @param mustFlipVertically indicates whether the texture
   *                           coordinates must be flipped vertically
   *                           in order to properly display the
   *                           texture
   * @param buffer         the buffer containing the texture data
   * @param flusher        optional flusher to perform cleanup tasks
   *                       upon call to flush()
   *
   * @throws IllegalArgumentException if any parameters of the texture
   *   data were invalid, such as requesting mipmap generation for a
   *   compressed texture
   */
  public TextureData(int internalFormat,
                     int width,
                     int height,
                     int border,
                     int pixelFormat,
                     int pixelType,
                     boolean mipmap,
                     boolean dataIsCompressed,
                     boolean mustFlipVertically,
                     Buffer buffer,
                     Flusher flusher) throws IllegalArgumentException {
    if (mipmap && dataIsCompressed) {
      throw new IllegalArgumentException("Can not generate mipmaps for compressed textures");
    }

    this.width = width;
    this.height = height;
    this.border = border;
    this.pixelFormat = pixelFormat;
    this.pixelType = pixelType;
    this.internalFormat = internalFormat;
    this.mipmap = mipmap;
    this.dataIsCompressed = dataIsCompressed;
    this.mustFlipVertically = mustFlipVertically;
    this.buffer = buffer;
    this.flusher = flusher;
    alignment = 1;  // FIXME: is this correct enough in all situations?
    estimatedMemorySize = estimatedMemorySize(buffer);
  }

  /** 
   * Constructs a new TextureData object with the specified parameters
   * and data for multiple mipmap levels contained in the given array
   * of Buffers. The optional Flusher can be used to clean up native
   * resources associated with this TextureData when processing is
   * complete; for example, closing of memory-mapped files that might
   * otherwise require a garbage collection to reclaim and close.
   *
   * @param internalFormat the OpenGL internal format for the
   *                       resulting texture; must be specified, may
   *                       not be 0
   * @param width          the width in pixels of the topmost mipmap
   *                       level of the texture
   * @param height         the height in pixels of the topmost mipmap
   *                       level of the texture
   * @param border         the number of pixels of border this texture
   *                       data has (0 or 1)
   * @param pixelFormat    the OpenGL pixel format for the
   *                       resulting texture; must be specified, may
   *                       not be 0
   * @param pixelType      the OpenGL type of the pixels of the texture
   * @param dataIsCompressed indicates whether the texture data is in
   *                       compressed form
   *                       (e.g. GL_COMPRESSED_RGB_S3TC_DXT1_EXT)
   * @param mustFlipVertically indicates whether the texture
   *                           coordinates must be flipped vertically
   *                           in order to properly display the
   *                           texture
   * @param mipmapData     the buffers containing all mipmap levels
   *                       of the texture's data
   * @param flusher        optional flusher to perform cleanup tasks
   *                       upon call to flush()
   *
   * @throws IllegalArgumentException if any parameters of the texture
   *   data were invalid, such as requesting mipmap generation for a
   *   compressed texture
   */
  public TextureData(int internalFormat,
                     int width,
                     int height,
                     int border,
                     int pixelFormat,
                     int pixelType,
                     boolean dataIsCompressed,
                     boolean mustFlipVertically,
                     Buffer[] mipmapData,
                     Flusher flusher) throws IllegalArgumentException {
    this.width = width;
    this.height = height;
    this.border = border;
    this.pixelFormat = pixelFormat;
    this.pixelType = pixelType;
    this.internalFormat = internalFormat;
    this.dataIsCompressed = dataIsCompressed;
    this.mustFlipVertically = mustFlipVertically;
    this.mipmapData = (Buffer[]) mipmapData.clone();
    this.flusher = flusher;
    alignment = 1;  // FIXME: is this correct enough in all situations?
    for (int i = 0; i < mipmapData.length; i++) {
      estimatedMemorySize += estimatedMemorySize(mipmapData[i]);
    }
  }

  /** 
   * Constructs a new TextureData object with the specified parameters
   * and data contained in the given BufferedImage. The resulting
   * TextureData "wraps" the contents of the BufferedImage, so if a
   * modification is made to the BufferedImage between the time the
   * TextureData is constructed and when a Texture is made from the
   * TextureData, that modification will be visible in the resulting
   * Texture.
   *
   * @param internalFormat the OpenGL internal format for the
   *                       resulting texture; may be 0, in which case
   *                       it is inferred from the image's type
   * @param pixelFormat    the OpenGL internal format for the
   *                       resulting texture; may be 0, in which case
   *                       it is inferred from the image's type (note:
   *                       this argument is currently always ignored)
   * @param mipmap         indicates whether mipmaps should be
   *                       autogenerated (using GLU) for the resulting
   *                       texture
   * @param image          the image containing the texture data
   */
  public TextureData(int internalFormat,
                     int pixelFormat,
                     boolean mipmap,
                     BufferedImage image) {
    if (internalFormat == 0) {
      this.internalFormat = image.getColorModel().hasAlpha() ? GL.GL_RGBA : GL.GL_RGB;
    } else {
      this.internalFormat = internalFormat;
    }
    createFromImage(image);
    this.mipmap = mipmap;
    estimatedMemorySize = estimatedMemorySize(buffer);
  }

  /** Returns the width in pixels of the texture data. */
  public int getWidth() { return width; }
  /** Returns the height in pixels of the texture data. */
  public int getHeight() { return height; }
  /** Returns the border in pixels of the texture data. */
  public int getBorder() { return border; }
  /** Returns the intended OpenGL pixel format of the texture data. */
  public int getPixelFormat() {
    if (imageForLazyCustomConversion != null) {
      if (!((expectingEXTABGR && haveEXTABGR) ||
            (expectingGL12    && haveGL12))) {
        revertPixelFormatAndType();
      }
    }
    return pixelFormat;
  }
  /** Returns the intended OpenGL pixel type of the texture data. */
  public int getPixelType() {
    if (imageForLazyCustomConversion != null) {
      if (!((expectingEXTABGR && haveEXTABGR) ||
            (expectingGL12    && haveGL12))) {
        revertPixelFormatAndType();
      }
    }
    return pixelType;
  }
  /** Returns the intended OpenGL internal format of the texture data. */
  public int getInternalFormat() { return internalFormat; }
  /** Returns whether mipmaps should be generated for the texture data. */
  public boolean getMipmap() { return mipmap; }
  /** Indicates whether the texture data is in compressed form. */
  public boolean isDataCompressed() { return dataIsCompressed; }
  /** Indicates whether the texture coordinates must be flipped
      vertically for proper display. */
  public boolean getMustFlipVertically() { return mustFlipVertically; }
  /** Returns the texture data, or null if it is specified as a set of mipmaps. */
  public Buffer getBuffer() {
    if (imageForLazyCustomConversion != null) {
      if (!((expectingEXTABGR && haveEXTABGR) ||
            (expectingGL12    && haveGL12))) {
        revertPixelFormatAndType();
        // Must present the illusion to the end user that we are simply
        // wrapping the input BufferedImage
        createFromCustom(imageForLazyCustomConversion);
      }
    }
    return buffer;
  }
  /** Returns all mipmap levels for the texture data, or null if it is
      specified as a single image. */
  public Buffer[] getMipmapData() { return mipmapData; }
  /** Returns the required byte alignment for the texture data. */
  public int getAlignment() { return alignment; }
  /** Returns the row length needed for correct GL_UNPACK_ROW_LENGTH
      specification. This is currently only supported for
      non-mipmapped, non-compressed textures. */
  public int getRowLength() { return rowLength; }

  /** Sets the width in pixels of the texture data. */
  public void setWidth(int width) { this.width = width; }
  /** Sets the height in pixels of the texture data. */
  public void setHeight(int height) { this.height = height; }
  /** Sets the border in pixels of the texture data. */
  public void setBorder(int border) { this.border = border; }
  /** Sets the intended OpenGL pixel format of the texture data. */
  public void setPixelFormat(int pixelFormat) { this.pixelFormat = pixelFormat; }
  /** Sets the intended OpenGL pixel type of the texture data. */
  public void setPixelType(int pixelType) { this.pixelType = pixelType; }
  /** Sets the intended OpenGL internal format of the texture data. */
  public void setInternalFormat(int internalFormat) { this.internalFormat = internalFormat; }
  /** Sets whether mipmaps should be generated for the texture data. */
  public void setMipmap(boolean mipmap) { this.mipmap = mipmap; }
  /** Sets whether the texture data is in compressed form. */
  public void setIsDataCompressed(boolean compressed) { this.dataIsCompressed = compressed; }
  /** Sets whether the texture coordinates must be flipped vertically
      for proper display. */
  public void setMustFlipVertically(boolean mustFlipVertically) { this.mustFlipVertically = mustFlipVertically; }
  /** Sets the texture data. */
  public void setBuffer(Buffer buffer) { this.buffer = buffer; }
  /** Sets the required byte alignment for the texture data. */
  public void setAlignment(int alignment) { this.alignment = alignment; }
  /** Sets the row length needed for correct GL_UNPACK_ROW_LENGTH
      specification. This is currently only supported for
      non-mipmapped, non-compressed textures. */
  public void setRowLength(int rowLength) { this.rowLength = rowLength; }
  /** Indicates to this TextureData whether the GL_EXT_abgr extension
      is available. Used for optimization along some code paths to
      avoid data copies. */
  public void setHaveEXTABGR(boolean haveEXTABGR) {
    this.haveEXTABGR = haveEXTABGR;
  }
  /** Indicates to this TextureData whether OpenGL version 1.2 is
      available. If not, falls back to relatively inefficient code
      paths for several input data types (several kinds of packed
      pixel formats, in particular). */
  public void setHaveGL12(boolean haveGL12) {
    this.haveGL12 = haveGL12;
  }

  /** Returns an estimate of the amount of memory in bytes this
      TextureData will consume once uploaded to the graphics card. It
      should only be treated as an estimate; most applications should
      not need to query this but instead let the OpenGL implementation
      page textures in and out as necessary. */
  public int getEstimatedMemorySize() {
    return estimatedMemorySize;
  }

  /** Flushes resources associated with this TextureData by calling
      Flusher.flush(). */
  public void flush() {
    if (flusher != null) {
      flusher.flush();
      flusher = null;
    }
  }

  /** Defines a callback mechanism to allow the user to explicitly
      deallocate native resources (memory-mapped files, etc.)
      associated with a particular TextureData. */
  public static interface Flusher {
    /** Flushes any native resources associated with this
        TextureData. */
    public void flush();
  }

  //----------------------------------------------------------------------
  // Internals only below this point
  //

  private void createNIOBufferFromImage(BufferedImage image) {
    //
    // Note: Grabbing the DataBuffer will defeat Java2D's image
    // management mechanism (as of JDK 5/6, at least).  This shouldn't
    // be a problem for most JOGL apps, but those that try to upload
    // the image into an OpenGL texture and then use the same image in
    // Java2D rendering might find the 2D rendering is not as fast as
    // it could be.
    //

    DataBuffer data = image.getRaster().getDataBuffer();
    if (data instanceof DataBufferByte) {
      buffer = ByteBuffer.wrap(((DataBufferByte) data).getData());
    } else if (data instanceof DataBufferDouble) {
      throw new RuntimeException("DataBufferDouble rasters not supported by OpenGL");
    } else if (data instanceof DataBufferFloat) {
      buffer = FloatBuffer.wrap(((DataBufferFloat) data).getData());
    } else if (data instanceof DataBufferInt) {
      buffer = IntBuffer.wrap(((DataBufferInt) data).getData());
    } else if (data instanceof DataBufferShort) {
      buffer = ShortBuffer.wrap(((DataBufferShort) data).getData());
    } else if (data instanceof DataBufferUShort) {
      buffer = ShortBuffer.wrap(((DataBufferUShort) data).getData());
    } else {
      throw new RuntimeException("Unexpected DataBuffer type?");
    }
  }

  private void createFromImage(BufferedImage image) {
    pixelType = 0; // Determine from image
    mustFlipVertically = true;

    width = image.getWidth();
    height = image.getHeight();

    int scanlineStride;
    SampleModel sm = image.getRaster().getSampleModel();
    if (sm instanceof SinglePixelPackedSampleModel) {
      scanlineStride =
        ((SinglePixelPackedSampleModel)sm).getScanlineStride();
    } else if (sm instanceof MultiPixelPackedSampleModel) {
      scanlineStride =
        ((MultiPixelPackedSampleModel)sm).getScanlineStride();
    } else if (sm instanceof ComponentSampleModel) {
      scanlineStride =
        ((ComponentSampleModel)sm).getScanlineStride();
    } else {
      // This will only happen for TYPE_CUSTOM anyway
      setupLazyCustomConversion(image);
      return;
    }

    switch (image.getType()) {
      case BufferedImage.TYPE_INT_RGB:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        rowLength = scanlineStride;
        alignment = 4;
        expectingGL12 = true;
        setupLazyCustomConversion(image);
        break;
      case BufferedImage.TYPE_INT_ARGB_PRE:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        rowLength = scanlineStride;
        alignment = 4;
        expectingGL12 = true;
        setupLazyCustomConversion(image);
        break;
      case BufferedImage.TYPE_INT_BGR:
        pixelFormat = GL.GL_RGBA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        rowLength = scanlineStride;
        alignment = 4;
        expectingGL12 = true;
        setupLazyCustomConversion(image);
        break;
      case BufferedImage.TYPE_3BYTE_BGR:
        {
          // we can pass the image data directly to OpenGL only if
          // we have an integral number of pixels in each scanline
          if ((scanlineStride % 3) == 0) {
            pixelFormat = GL.GL_BGR;
            pixelType = GL.GL_UNSIGNED_BYTE;
            rowLength = scanlineStride / 3;
            alignment = 1;
          } else {
            setupLazyCustomConversion(image);
            return;
          }
        }
        break;
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        {
          // we can pass the image data directly to OpenGL only if
          // we have an integral number of pixels in each scanline
          // and only if the GL_EXT_abgr extension is present

          // NOTE: disabling this code path for now as it appears it's
          // buggy at least on some NVidia drivers and doesn't perform
          // the necessary byte swapping (FIXME: needs more
          // investigation)
          if ((scanlineStride % 4) == 0 && false) {
            pixelFormat = GL.GL_ABGR_EXT;
            pixelType = GL.GL_UNSIGNED_BYTE;
            rowLength = scanlineStride / 4;
            alignment = 4;

            // Store a reference to the original image for later in
            // case it turns out that we don't have GL_EXT_abgr at the
            // time we're going to do the texture upload to OpenGL
            setupLazyCustomConversion(image);
            expectingEXTABGR = true;
            break;
          } else {
            setupLazyCustomConversion(image);
            return;
          }
        }
      case BufferedImage.TYPE_USHORT_565_RGB:
        pixelFormat = GL.GL_RGB;
        pixelType = GL.GL_UNSIGNED_SHORT_5_6_5;
        rowLength = scanlineStride;
        alignment = 2;
        expectingGL12 = true;
        setupLazyCustomConversion(image);
        break;
      case BufferedImage.TYPE_USHORT_555_RGB:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_SHORT_1_5_5_5_REV;
        rowLength = scanlineStride;
        alignment = 2;
        expectingGL12 = true;
        setupLazyCustomConversion(image);
        break;
      case BufferedImage.TYPE_BYTE_GRAY:
        pixelFormat = GL.GL_LUMINANCE;
        pixelType = GL.GL_UNSIGNED_BYTE;
        rowLength = scanlineStride;
        alignment = 1;
        break;
      case BufferedImage.TYPE_USHORT_GRAY:
        pixelFormat = GL.GL_LUMINANCE;
        pixelType = GL.GL_UNSIGNED_SHORT;
        rowLength = scanlineStride;
        alignment = 2;
        break;
      // Note: TYPE_INT_ARGB and TYPE_4BYTE_ABGR images go down the
      // custom code path to satisfy the invariant that images with an
      // alpha channel always go down with premultiplied alpha.
      case BufferedImage.TYPE_INT_ARGB:
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_BYTE_BINARY:
      case BufferedImage.TYPE_BYTE_INDEXED:
      case BufferedImage.TYPE_CUSTOM:
      default:
        ColorModel cm = image.getColorModel();
        if (cm.equals(rgbColorModel)) {
          pixelFormat = GL.GL_RGB;
          pixelType = GL.GL_UNSIGNED_BYTE;
          rowLength = scanlineStride / 3;
          alignment = 1;
        } else if (cm.equals(rgbaColorModel)) {
          pixelFormat = GL.GL_RGBA;
          pixelType = GL.GL_UNSIGNED_BYTE;
          rowLength = scanlineStride / 4; // FIXME: correct?
          alignment = 4;
        } else {
          setupLazyCustomConversion(image);
          return;
        }
        break;
    }

    createNIOBufferFromImage(image);
  }

  private void setupLazyCustomConversion(BufferedImage image) {
    imageForLazyCustomConversion = image;
    boolean hasAlpha = image.getColorModel().hasAlpha();
    if (pixelFormat == 0) {
        pixelFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
    }
    alignment = 1; // FIXME: do we need better?
    rowLength = width; // FIXME: correct in all cases?

    // Allow previously-selected pixelType (if any) to override that
    // we can infer from the DataBuffer
    DataBuffer data = image.getRaster().getDataBuffer();
    if (data instanceof DataBufferByte || isPackedInt(image)) {
      // Don't use GL_UNSIGNED_INT for BufferedImage packed int images
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_BYTE;
    } else if (data instanceof DataBufferDouble) {
      throw new RuntimeException("DataBufferDouble rasters not supported by OpenGL");
    } else if (data instanceof DataBufferFloat) {
      if (pixelType == 0) pixelType = GL.GL_FLOAT;
    } else if (data instanceof DataBufferInt) {
      // FIXME: should we support signed ints?
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_INT;
    } else if (data instanceof DataBufferShort) {
      if (pixelType == 0) pixelType = GL.GL_SHORT;
    } else if (data instanceof DataBufferUShort) {
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_SHORT;
    } else {
      throw new RuntimeException("Unexpected DataBuffer type?");
    }
  }

  private void createFromCustom(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();

    // create a temporary image that is compatible with OpenGL
    boolean hasAlpha = image.getColorModel().hasAlpha();
    ColorModel cm = null;
    int dataBufferType = image.getRaster().getDataBuffer().getDataType();
    // Don't use integer components for packed int images
    if (isPackedInt(image)) {
      dataBufferType = DataBuffer.TYPE_BYTE;
    }
    if (dataBufferType == DataBuffer.TYPE_BYTE) {
      cm = hasAlpha ? rgbaColorModel : rgbColorModel;
    } else {
      if (hasAlpha) {
        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                     null, true, true,
                                     Transparency.TRANSLUCENT,
                                     dataBufferType);
      } else {
        cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                     null, false, false,
                                     Transparency.OPAQUE,
                                     dataBufferType);
      }
    }

    boolean premult = cm.isAlphaPremultiplied();
    WritableRaster raster =
      cm.createCompatibleWritableRaster(width, height);
    BufferedImage texImage = new BufferedImage(cm, raster, premult, null);

    // copy the source image into the temporary image
    Graphics2D g = texImage.createGraphics();
    g.setComposite(AlphaComposite.Src);
    g.drawImage(image, 0, 0, null);
    g.dispose();

    // Wrap the buffer from the temporary image
    createNIOBufferFromImage(texImage);
  }

  private boolean isPackedInt(BufferedImage image) {
    int imgType = image.getType();
    return (imgType == BufferedImage.TYPE_INT_RGB ||
            imgType == BufferedImage.TYPE_INT_BGR ||
            imgType == BufferedImage.TYPE_INT_ARGB ||
            imgType == BufferedImage.TYPE_INT_ARGB_PRE);
  }

  private void revertPixelFormatAndType() {
    // Knowing we don't have e.g. OpenGL 1.2 functionality available,
    // and knowing we're in the process of doing the fallback code
    // path, re-infer a vanilla pixel format and type compatible with
    // OpenGL 1.1
    pixelFormat = 0;
    pixelType = 0;
    setupLazyCustomConversion(imageForLazyCustomConversion);
  }

  private int estimatedMemorySize(Buffer buffer) {
    if (buffer == null) {
      return 0;
    }
    int capacity = buffer.capacity();
    if (buffer instanceof ByteBuffer) {
      return capacity;
    } else if (buffer instanceof IntBuffer) {
      return capacity * BufferUtil.SIZEOF_INT;
    } else if (buffer instanceof FloatBuffer) {
      return capacity * BufferUtil.SIZEOF_FLOAT;
    } else if (buffer instanceof ShortBuffer) {
      return capacity * BufferUtil.SIZEOF_SHORT;
    } else if (buffer instanceof LongBuffer) {
      return capacity * BufferUtil.SIZEOF_LONG;
    } else if (buffer instanceof DoubleBuffer) {
      return capacity * BufferUtil.SIZEOF_DOUBLE;
    }
    throw new RuntimeException("Unexpected buffer type " +
                               buffer.getClass().getName());
  }
}
