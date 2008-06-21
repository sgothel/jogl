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

public interface TextureData {
  /** Returns the width in pixels of the texture data. */
  public int getWidth();
  /** Returns the height in pixels of the texture data. */
  public int getHeight();
  /** Returns the border in pixels of the texture data. */
  public int getBorder();
  /** Returns the intended OpenGL pixel format of the texture data. */
  public int getPixelFormat();
  /** Returns the intended OpenGL pixel type of the texture data. */
  public int getPixelType();
  /** Returns the intended OpenGL internal format of the texture data. */
  public int getInternalFormat();
  /** Returns whether mipmaps should be generated for the texture data. */
  public boolean getMipmap();
  /** Indicates whether the texture data is in compressed form. */
  public boolean isDataCompressed();
  /** Indicates whether the texture coordinates must be flipped
      vertically for proper display. */
  public boolean getMustFlipVertically();
  /** Returns the texture data, or null if it is specified as a set of mipmaps. */
  public Buffer getBuffer();
  /** Returns all mipmap levels for the texture data, or null if it is
      specified as a single image. */
  public Buffer[] getMipmapData();
  /** Returns the required byte alignment for the texture data. */
  public int getAlignment();
  /** Returns the row length needed for correct GL_UNPACK_ROW_LENGTH
      specification. This is currently only supported for
      non-mipmapped, non-compressed textures. */
  public int getRowLength();

  /** Sets the width in pixels of the texture data. */
  public void setWidth(int width);
  /** Sets the height in pixels of the texture data. */
  public void setHeight(int height);
  /** Sets the border in pixels of the texture data. */
  public void setBorder(int border);
  /** Sets the intended OpenGL pixel format of the texture data. */
  public void setPixelFormat(int pixelFormat);
  /** Sets the intended OpenGL pixel type of the texture data. */
  public void setPixelType(int pixelType);
  /** Sets the intended OpenGL internal format of the texture data. */
  public void setInternalFormat(int internalFormat);
  /** Sets whether mipmaps should be generated for the texture data. */
  public void setMipmap(boolean mipmap);
  /** Sets whether the texture data is in compressed form. */
  public void setIsDataCompressed(boolean compressed);
  /** Sets whether the texture coordinates must be flipped vertically
      for proper display. */
  public void setMustFlipVertically(boolean mustFlipVertically);
  /** Sets the texture data. */
  public void setBuffer(Buffer buffer);
  /** Sets the required byte alignment for the texture data. */
  public void setAlignment(int alignment) ;
  /** Sets the row length needed for correct GL_UNPACK_ROW_LENGTH
      specification. This is currently only supported for
      non-mipmapped, non-compressed textures. */
  public void setRowLength(int rowLength) ;
  /** Indicates to this TextureData whether the GL_EXT_abgr extension
      is available. Used for optimization along some code paths to
      avoid data copies. */
  public void setHaveEXTABGR(boolean haveEXTABGR) ;
  /** Indicates to this TextureData whether OpenGL version 1.2 is
      available. If not, falls back to relatively inefficient code
      paths for several input data types (several kinds of packed
      pixel formats, in particular). */
  public void setHaveGL12(boolean haveGL12) ;

  /** Returns an estimate of the amount of memory in bytes this
      TextureData will consume once uploaded to the graphics card. It
      should only be treated as an estimate; most applications should
      not need to query this but instead let the OpenGL implementation
      page textures in and out as necessary. */
  public int getEstimatedMemorySize() ;

  /** Flushes resources associated with this TextureData by calling
      Flusher.flush(). */
  public void flush() ;

  /** Defines a callback mechanism to allow the user to explicitly
      deallocate native resources (memory-mapped files, etc.)
      associated with a particular TextureData. */
  public static interface Flusher {
    /** Flushes any native resources associated with this
        TextureData. */
    public void flush();
  }
}
