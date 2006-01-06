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

package com.sun.opengl.utils;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.*;
import java.awt.image.*;
import java.nio.*;

import javax.media.opengl.*;

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
  private int mipmapLevel;
  private boolean dataIsCompressed;
  private boolean mustFlipVertically; // Must flip texture coordinates
                                      // vertically to get OpenGL output
                                      // to look correct
  private Buffer buffer; // the actual data
  private Flusher flusher;
  private int alignment; // 1, 2, or 4 bytes

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
   * @param mipmapLevel    the mipmap level for the resulting texture
   *                       this data represents (FIXME: needs
   *                       rethinking, currently unused)
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
   */
  public TextureData(int mipmapLevel,
                     int internalFormat,
                     int width,
                     int height,
                     int border,
                     int pixelFormat,
                     int pixelType,
                     boolean dataIsCompressed,
                     boolean mustFlipVertically,
                     Buffer buffer,
                     Flusher flusher) {
    this.width = width;
    this.height = height;
    this.border = border;
    this.pixelFormat = pixelFormat;
    this.pixelType = pixelType;
    this.internalFormat = internalFormat;
    this.mipmapLevel = mipmapLevel;
    this.dataIsCompressed = dataIsCompressed;
    this.mustFlipVertically = mustFlipVertically;
    this.buffer = buffer;
    this.flusher = flusher;
    alignment = 1;  // FIXME: is this correct enough in all situations?
  }

  /** 
   * Constructs a new TextureData object with the specified parameters
   * and data contained in the given BufferedImage.
   *
   * @param mipmapLevel    the mipmap level for the resulting texture
   *                       this data represents (FIXME: needs
   *                       rethinking, currently unused)
   * @param internalFormat the OpenGL internal format for the
   *                       resulting texture; may be 0, in which case
   *                       it is inferred from the image's type
   * @param pixelFormat    the OpenGL internal format for the
   *                       resulting texture; may be 0, in which case
   *                       it is inferred from the image's type (note:
   *                       this argument is currently always ignored)
   * @param image          the image containing the texture data
   */
  public TextureData(int mipmapLevel,
                     int internalFormat,
                     int pixelFormat,
                     BufferedImage image) {
    if (internalFormat == 0) {
      this.internalFormat = image.getColorModel().hasAlpha() ? GL.GL_RGBA : GL.GL_RGB;
    } else {
      this.internalFormat = internalFormat;
    }
    createFromImage(image);
    this.mipmapLevel = mipmapLevel;
  }

  /** Returns the width in pixels of the texture data. */
  public int getWidth() { return width; }
  /** Returns the height in pixels of the texture data. */
  public int getHeight() { return height; }
  /** Returns the border in pixels of the texture data. */
  public int getBorder() { return border; }
  /** Returns the intended OpenGL pixel format of the texture data. */
  public int getPixelFormat() { return pixelFormat; }
  /** Returns the intended OpenGL pixel type of the texture data. */
  public int getPixelType() { return pixelType; }
  /** Returns the intended OpenGL internal format of the texture data. */
  public int getInternalFormat() { return internalFormat; }
  /** Returns the intended mipmap level of the texture data. */
  public int getMipmapLevel() { return mipmapLevel; }
  /** Indicates whether the texture data is in compressed form. */
  public boolean isDataCompressed() { return dataIsCompressed; }
  /** Indicates whether the texture coordinates must be flipped
      vertically for proper display. */
  public boolean getMustFlipVertically() { return mustFlipVertically; }
  /** Returns the texture data. */
  public Buffer getBuffer() { return buffer; }
  /** Returns the required byte alignment for the texture data. */
  public int getAlignment() { return alignment; }

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
  /** Sets the intended mipmap level of the texture data. */
  public void setMipmapLevel(int mipmapLevel) { this.mipmapLevel = mipmapLevel; }
  /** Sets whether the texture data is in compressed form. */
  public void setIsDataCompressed(boolean compressed) { this.dataIsCompressed = compressed; }
  /** Sets whether the texture coordinates must be flipped vertically
      for proper display. */
  public void setMustFlipVertically(boolean mustFlipVertically) { this.mustFlipVertically = mustFlipVertically; }
  /** Sets the texture data. */
  public void setBuffer(Buffer buffer) { this.buffer = buffer; }
  /** Sets the required byte alignment for the texture data. */
  public void setAlignment(int alignment) { this.alignment = alignment; }

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

  private void createNIOBufferFromImage(BufferedImage image, boolean flipVertically) {
    if (flipVertically) {
      WritableRaster raster = image.getRaster();
      Object scanline1 = null;
      Object scanline2 = null;
      
      for (int i = 0; i < image.getHeight() / 2; i++) {
        scanline1 = raster.getDataElements(0, i, image.getWidth(), 1, scanline1);
        scanline2 = raster.getDataElements(0, image.getHeight() - i - 1, image.getWidth(), 1, scanline2);
        raster.setDataElements(0, i, image.getWidth(), 1, scanline2);
        raster.setDataElements(0, image.getHeight() - i - 1, image.getWidth(), 1, scanline1);
      }
    }

    //
    // Note: Grabbing the DataBuffer will defeat Java2D's image
    // management mechanism (as of JDK 5/6, at least).  This shouldn't
    // be a problem for most JOGL apps, but those that try to upload
    // the image into an OpenGL texture and then use the same image in
    // Java2D rendering might find the 2D rendering is not as fast as
    // it could be.
    //

    // Allow previously-selected pixelType (if any) to override that
    // we can infer from the DataBuffer
    DataBuffer data = image.getRaster().getDataBuffer();
    if (data instanceof DataBufferByte) {
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_BYTE;
      buffer = ByteBuffer.wrap(((DataBufferByte) data).getData());
    } else if (data instanceof DataBufferDouble) {
      throw new RuntimeException("DataBufferDouble rasters not supported by OpenGL");
    } else if (data instanceof DataBufferFloat) {
      if (pixelType == 0) pixelType = GL.GL_FLOAT;
      buffer = FloatBuffer.wrap(((DataBufferFloat) data).getData());
    } else if (data instanceof DataBufferInt) {
      // FIXME: should we support signed ints?
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_INT;
      buffer = IntBuffer.wrap(((DataBufferInt) data).getData());
    } else if (data instanceof DataBufferShort) {
      if (pixelType == 0) pixelType = GL.GL_SHORT;
      buffer = ShortBuffer.wrap(((DataBufferShort) data).getData());
    } else if (data instanceof DataBufferUShort) {
      if (pixelType == 0) pixelType = GL.GL_UNSIGNED_SHORT;
      buffer = ShortBuffer.wrap(((DataBufferShort) data).getData());
    } else {
      throw new RuntimeException("Unexpected DataBuffer type?");
    }
  }

  private void createFromImage(BufferedImage image) {
    pixelType = 0; // Determine from image

    width = image.getWidth();
    height = image.getHeight();

    switch (image.getType()) {
      case BufferedImage.TYPE_INT_RGB:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        alignment = 4;
        break;
      case BufferedImage.TYPE_INT_ARGB_PRE:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        alignment = 4;
        break;
      case BufferedImage.TYPE_INT_BGR:
        pixelFormat = GL.GL_RGBA;
        pixelType = GL.GL_UNSIGNED_INT_8_8_8_8_REV;
        alignment = 4;
        break;
      case BufferedImage.TYPE_3BYTE_BGR:
        {
          Raster raster = image.getRaster();
          ComponentSampleModel csm =
            (ComponentSampleModel)raster.getSampleModel();
          // we can pass the image data directly to OpenGL only if
          // the raster is tightly packed (i.e. there is no extra
          // space at the end of each scanline)
          if ((csm.getScanlineStride() / 3) == csm.getWidth()) {
            pixelFormat = GL.GL_BGR;
            pixelType = GL.GL_UNSIGNED_BYTE;
            alignment = 1;
          } else {
            createFromCustom(image);
            return;
          }
        }
        break;
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        {
          Raster raster = image.getRaster();
          ComponentSampleModel csm =
            (ComponentSampleModel)raster.getSampleModel();
          // we can pass the image data directly to OpenGL only if
          // the raster is tightly packed (i.e. there is no extra
          // space at the end of each scanline) and only if the
          // GL_EXT_abgr extension is present

          // FIXME: with the way this is currently organized we can't
          // probe for the existence of the GL_EXT_abgr extension
          // here; disable this code path for now
          if (((csm.getScanlineStride() / 4) == csm.getWidth()) &&
              /* gl.isExtensionAvailable("GL_EXT_abgr") */ false)
            {
              pixelFormat = GL.GL_ABGR_EXT;
              pixelType = GL.GL_UNSIGNED_BYTE;
              alignment = 4;
            } else {
              createFromCustom(image);
              return;
            }
        }
        break;
      case BufferedImage.TYPE_USHORT_565_RGB:
        pixelFormat = GL.GL_RGB;
        pixelType = GL.GL_UNSIGNED_SHORT_5_6_5;
        alignment = 2;
        break;
      case BufferedImage.TYPE_USHORT_555_RGB:
        pixelFormat = GL.GL_BGRA;
        pixelType = GL.GL_UNSIGNED_SHORT_1_5_5_5_REV;
        alignment = 2;
        break;
      case BufferedImage.TYPE_BYTE_GRAY:
        pixelFormat = GL.GL_LUMINANCE;
        pixelType = GL.GL_UNSIGNED_BYTE;
        alignment = 1;
        break;
      case BufferedImage.TYPE_USHORT_GRAY:
        pixelFormat = GL.GL_LUMINANCE;
        pixelType = GL.GL_UNSIGNED_SHORT;
        alignment = 2;
        break;
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
          alignment = 1;
        } else if (cm.equals(rgbaColorModel)) {
          pixelFormat = GL.GL_RGBA;
          pixelType = GL.GL_UNSIGNED_BYTE;
          alignment = 4;
        } else {
          createFromCustom(image);
          return;
        }
        break;
    }

    createNIOBufferFromImage(image, true);
  }

  private void createFromCustom(BufferedImage image) {
    int width = image.getWidth();
    int height = image.getHeight();

    // create a temporary image that is compatible with OpenGL
    boolean hasAlpha = image.getColorModel().hasAlpha();
    ColorModel cm = null;
    int dataBufferType = image.getRaster().getDataBuffer().getDataType();
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
    // Flip image vertically as long as we're at it
    g.drawImage(image,
                0, height, width, 0,
                0, 0, width, height,
                Color.BLACK,
                null);
    g.dispose();

    // Wrap the buffer from the temporary image
    createNIOBufferFromImage(texImage, false);
    pixelFormat = hasAlpha ? GL.GL_RGBA : GL.GL_RGB;
    alignment = 1; // FIXME: do we need better?
  }
}
