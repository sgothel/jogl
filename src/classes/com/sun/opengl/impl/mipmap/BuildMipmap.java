/*
 * License Applicability. Except to the extent portions of this file are
 * made subject to an alternative license as permitted in the SGI Free
 * Software License B, Version 1.1 (the "License"), the contents of this
 * file are subject only to the provisions of the License. You may not use
 * this file except in compliance with the License. You may obtain a copy
 * of the License at Silicon Graphics, Inc., attn: Legal Services, 1600
 * Amphitheatre Parkway, Mountain View, CA 94043-1351, or at:
 * 
 * http://oss.sgi.com/projects/FreeB
 * 
 * Note that, as provided in the License, the Software is distributed on an
 * "AS IS" basis, with ALL EXPRESS AND IMPLIED WARRANTIES AND CONDITIONS
 * DISCLAIMED, INCLUDING, WITHOUT LIMITATION, ANY IMPLIED WARRANTIES AND
 * CONDITIONS OF MERCHANTABILITY, SATISFACTORY QUALITY, FITNESS FOR A
 * PARTICULAR PURPOSE, AND NON-INFRINGEMENT.
 * 
 * NOTE:  The Original Code (as defined below) has been licensed to Sun
 * Microsystems, Inc. ("Sun") under the SGI Free Software License B
 * (Version 1.1), shown above ("SGI License").   Pursuant to Section
 * 3.2(3) of the SGI License, Sun is distributing the Covered Code to
 * you under an alternative license ("Alternative License").  This
 * Alternative License includes all of the provisions of the SGI License
 * except that Section 2.2 and 11 are omitted.  Any differences between
 * the Alternative License and the SGI License are offered solely by Sun
 * and not by SGI.
 *
 * Original Code. The Original Code is: OpenGL Sample Implementation,
 * Version 1.2.1, released January 26, 2000, developed by Silicon Graphics,
 * Inc. The Original Code is Copyright (c) 1991-2000 Silicon Graphics, Inc.
 * Copyright in any portions created by third parties is as indicated
 * elsewhere herein. All Rights Reserved.
 * 
 * Additional Notice Provisions: The application programming interfaces
 * established by SGI in conjunction with the Original Code are The
 * OpenGL(R) Graphics System: A Specification (Version 1.2.1), released
 * April 1, 1999; The OpenGL(R) Graphics System Utility Library (Version
 * 1.3), released November 4, 1998; and OpenGL(R) Graphics with the X
 * Window System(R) (Version 1.3), released October 19, 1998. This software
 * was created using the OpenGL(R) version 1.2.1 Sample Implementation
 * published by SGI, but has not been independently verified as being
 * compliant with the OpenGL(R) version 1.2.1 Specification.
 */

package com.sun.opengl.impl.mipmap;

import javax.media.opengl.GL;
import javax.media.opengl.glu.GLU;
import com.sun.opengl.impl.Debug;
import java.nio.*;
import java.io.*;

/**
 *
 * @author  Administrator
 */
public class BuildMipmap {

  private static final boolean DEBUG = Debug.debug("BuildMipmap");
  private static final boolean VERBOSE = Debug.verbose();

  /** Creates a new instance of BuildMipmap */
  public BuildMipmap() {
  }
  
  public static int gluBuild1DMipmapLevelsCore( GL gl, int target, int internalFormat,
                  int width, int widthPowerOf2, int format, int type, int userLevel,
                  int baseLevel, int maxLevel, ByteBuffer data ) {
    int newwidth;
    int level, levels;
    ShortBuffer newImage = null;
    int newImage_width;
    ShortBuffer otherImage = null;
    ShortBuffer imageTemp = null;
    int memreq;
    int maxsize;
    int cmpts;
    PixelStorageModes psm = new PixelStorageModes();
    
    assert( Mipmap.checkMipmapArgs( internalFormat, format, type ) == 0 );
    assert( width >= 1 );
    
    newwidth = widthPowerOf2;
    levels = Mipmap.computeLog( newwidth );
    
    levels += userLevel;
    
    Mipmap.retrieveStoreModes( gl, psm );
    try {
      newImage = ByteBuffer.allocateDirect( Mipmap.image_size( width, 1, format, 
            GL.GL_UNSIGNED_SHORT ) ).order( ByteOrder.nativeOrder() ).asShortBuffer();
    } catch( OutOfMemoryError ome ) {
      return( GLU.GLU_OUT_OF_MEMORY );
    }
    newImage_width = width;
    
    Image.fill_image( psm, width, 1, format, type, Mipmap.is_index( format ), data, newImage );
    cmpts = Mipmap.elements_per_group( format, type );
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, 2 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, 0 );
    
    // if swap_bytes was set, swapping occurred in fill_image
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE );
    
    for( level = userLevel; level <= levels; level++ ) {
      if( newImage_width == newwidth ) {
        // user newimage for this level
        if( baseLevel <= level && level <= maxLevel ) {
          gl.glTexImage1D( target, level, internalFormat, newImage_width, 0, format,
                        GL.GL_UNSIGNED_SHORT, newImage );
        }
      } else {
        if( otherImage == null ) {
          memreq = Mipmap.image_size( newwidth, 1, format, GL.GL_UNSIGNED_SHORT );
          try {
            otherImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() ).asShortBuffer();
          } catch( OutOfMemoryError ome ) {
            gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
            gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
            gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
            return( GLU.GLU_OUT_OF_MEMORY );
          }
        }
        ScaleInternal.scale_internal( cmpts, newImage_width, 1, newImage, newwidth, 1, otherImage );
        // swap newImage and otherImage
        imageTemp = otherImage;
        otherImage = newImage;
        newImage = imageTemp;
        
        newImage_width = newwidth;
        if( baseLevel <= level && level <= maxLevel ) {
          gl.glTexImage1D( target, level, internalFormat, newImage_width, 0, 
                            format, GL.GL_UNSIGNED_SHORT, newImage );
        }
      }
      if( newwidth > 1 ) {
        newwidth /= 2;
      }
    }
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
    
    return( 0 );
  }
  
  public static int bitmapBuild2DMipmaps( GL gl, int target, int internalFormat,
            int width, int height, int format, int type, ByteBuffer data ) {
    int newwidth[] = new int[1];
    int newheight[] = new int[1];
    int level, levels;
    ShortBuffer newImage = null;
    int newImage_width;
    int newImage_height;
    ShortBuffer otherImage = null;
    ShortBuffer tempImage = null;
    int memreq;
    int maxsize;
    int cmpts;
    PixelStorageModes psm = new PixelStorageModes();
    
    Mipmap.retrieveStoreModes( gl, psm );
    
    Mipmap.closestFit( gl, target, width, height, internalFormat, format, type, newwidth, newheight );
    
    levels = Mipmap.computeLog( newwidth[0] );
    level = Mipmap.computeLog( newheight[0] );
    if( level > levels ) {
      levels = level;
    }
    
    try {
      newImage = ByteBuffer.allocateDirect( Mipmap.image_size( width, height, 
            format, GL.GL_UNSIGNED_SHORT ) ).order( ByteOrder.nativeOrder() ).asShortBuffer();
    } catch( OutOfMemoryError ome ) {
      return( GLU.GLU_OUT_OF_MEMORY );
    }
    newImage_width = width;
    newImage_height = height;
    
    Image.fill_image( psm, width, height, format, type, Mipmap.is_index( format ), data, newImage );
    
    cmpts = Mipmap.elements_per_group( format, type );
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, 2 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, 0 );
    
    // if swap_bytes is set, swapping occurred in fill_image
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE );
    
    for( level = 0; level < levels; level++ ) {
      if( newImage_width == newwidth[0] && newImage_height == newheight[0] ) {
        newImage.rewind();
        gl.glTexImage2D( target, level, internalFormat, newImage_width,
            newImage_height, 0, format, GL.GL_UNSIGNED_SHORT, newImage );
      } else {
        if( otherImage == null ) {
          memreq = Mipmap.image_size( newwidth[0], newheight[0], format, GL.GL_UNSIGNED_SHORT );
          try {
            otherImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() ).asShortBuffer();
          } catch( OutOfMemoryError ome ) {
            gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
            gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
            gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
            return( GLU.GLU_OUT_OF_MEMORY );
          }
        }
        ScaleInternal.scale_internal( cmpts, newImage_width, newImage_height, 
                              newImage, newwidth[0], newheight[0], otherImage );
        // swap newImage and otherImage
        tempImage = otherImage;
        otherImage = newImage;
        newImage = tempImage;
        
        newImage_width = newwidth[0];
        newImage_height = newheight[0];
        newImage.rewind();
        gl.glTexImage2D( target, level, internalFormat, newImage_width, newImage_height,
                                    0, format, GL.GL_UNSIGNED_SHORT, newImage );
      }
      if( newheight[0] > 1 ) {
        newwidth[0] /= 2;
      }
      if( newheight[0] > 1 ) {
        newheight[0] /= 2;
      }
    }
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
    
    return( 0 );
  }
  
  public static int gluBuild2DMipmapLevelsCore( GL gl, int target, int internalFormat,
                int width, int height, int widthPowerOf2, int heightPowerOf2,
                int format, int type, int userLevel, int baseLevel, int maxLevel,
                ByteBuffer data ) { // PointerWrapper data
    int newwidth;
    int newheight;
    int level, levels;
    int usersImage;
    ByteBuffer srcImage = null;
    ByteBuffer dstImage = null;
    ByteBuffer tempImage = null;
    int newImage_width;
    int newImage_height;
    short[] SWAP_IMAGE = null;
    int memreq;
    int maxsize;
    int cmpts;
    
    boolean myswap_bytes;
    int groups_per_line, element_size, group_size;
    int rowsize, padding;
    PixelStorageModes psm = new PixelStorageModes();
    
    assert( Mipmap.checkMipmapArgs( internalFormat, format, type ) == 0 );
    assert( width >= 1 && height >= 1 );
    
    if( type == GL.GL_BITMAP ) {
      return( bitmapBuild2DMipmaps( gl, target, internalFormat, width, height, format, type, data ) );
    }
    
    newwidth = widthPowerOf2;
    newheight = heightPowerOf2;
    levels = Mipmap.computeLog( newwidth );
    level = Mipmap.computeLog( newheight );
    if( level > levels ) {
      levels = level;
    }
    
    levels += userLevel;
    
    Mipmap.retrieveStoreModes( gl, psm );
    myswap_bytes = psm.getUnpackSwapBytes();
    cmpts = Mipmap.elements_per_group( format, type );
    if( psm.getUnpackRowLength() > 0 ) {
      groups_per_line = psm.getUnpackRowLength();
    } else {
      groups_per_line = width;
    }
    
    element_size = Mipmap.bytes_per_element( type );
    group_size = element_size * cmpts;
    if( element_size == 1 ) {
      myswap_bytes = false;
    }
    
    rowsize = groups_per_line * group_size;
    padding = ( rowsize % psm.getUnpackAlignment() );
    if( padding != 0 ) {
      rowsize += psm.getUnpackAlignment() - padding;
    }
    
    data.position( psm.getUnpackSkipRows() * rowsize + psm.getUnpackSkipPixels() * group_size );
    data.mark();
    
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, 0 );
    
    level = userLevel;
    
    // already power of two square
    if( width == newwidth && height == newheight ) {
      // use usersImage for level userLevel
      if( baseLevel <= level && level <= maxLevel ) {
        data.rewind();
        gl.glTexImage2D( target, level, internalFormat, width, height, 0, format, type, data );
      }
      if( levels == 0 ) { /* we're done. clean up and return */
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
        return( 0 );
      }
      int nextWidth = newwidth / 2;
      int nextHeight = newheight / 2;
      
      // clamp to 1
      if( nextWidth < 1 ) {
        nextWidth = 1;
      }
      if( nextHeight < 1 ) {
        nextHeight = 1;
      }
      memreq = Mipmap.image_size( nextWidth, nextHeight, format, type );
      
      try {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError ome ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      if( dstImage != null ) {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
            HalveImage.halveImage_ubyte( cmpts, width, height, data, dstImage, element_size, rowsize, group_size );
            break;
          case( GL.GL_BYTE ):
            HalveImage.halveImage_byte( cmpts, width, height, data, dstImage, element_size, rowsize, group_size );
            break;
          case( GL.GL_UNSIGNED_SHORT ):
            HalveImage.halveImage_ushort( cmpts, width, height, data, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
            break;
          case( GL.GL_SHORT ):
            HalveImage.halveImage_short( cmpts, width, height, data, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_INT ):
            HalveImage.halveImage_uint( cmpts, width, height, data, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
            break;
          case( GL.GL_INT ):
            HalveImage.halveImage_int( cmpts, width, height, data, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
            break;
          case( GL.GL_FLOAT ):
            HalveImage.halveImage_float( cmpts, width, height, data, dstImage.asFloatBuffer(), element_size, rowsize, group_size, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
            assert( format == GL.GL_RGB );
            HalveImage.halveImagePackedPixel( 3, new Extract332(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
            assert( format == GL.GL_RGB );
            HalveImage.halveImagePackedPixel( 3, new Extract233rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
            HalveImage.halveImagePackedPixel( 3, new Extract565(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
            HalveImage.halveImagePackedPixel( 3, new Extract565rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
            HalveImage.halveImagePackedPixel( 4, new Extract4444(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
            HalveImage.halveImagePackedPixel( 4, new Extract4444rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
            HalveImage.halveImagePackedPixel( 4, new Extract5551(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
            HalveImage.halveImagePackedPixel( 4, new Extract1555rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
            HalveImage.halveImagePackedPixel( 4, new Extract8888(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
            HalveImage.halveImagePackedPixel( 4, new Extract8888rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
            HalveImage.halveImagePackedPixel( 4, new Extract1010102(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            HalveImage.halveImagePackedPixel( 4, new Extract2101010rev(), width, height, data, dstImage, element_size, rowsize, myswap_bytes );
            break;
          default:
            assert( false );
            break;
        }
      }
      newwidth = width / 2;
      newheight = height / 2;
      // clamp to 1
      if( newwidth < 1 ) {
        newwidth = 1;
      }
      if( newheight < 1 ) {
        newheight = 1;
      }
      
      myswap_bytes = false;
      rowsize = newwidth * group_size;
      memreq = Mipmap.image_size( newwidth, newheight, format, type );
      // swap srcImage and dstImage
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      try {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError ome ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      // level userLevel+1 is in srcImage; level userLevel already saved
      level = userLevel + 1;
    } else { // user's image is not nice powerof2 size square
      memreq = Mipmap.image_size( newwidth, newheight, format, type );
      try { 
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError ome ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      data.reset();
      switch( type ) {
        case( GL.GL_UNSIGNED_BYTE ):
          ScaleInternal.scale_internal_ubyte( cmpts, width, height, data, 
                newwidth, newheight, dstImage, element_size, rowsize, group_size );
          break;
        case( GL.GL_BYTE ):
          ScaleInternal.scale_internal_byte( cmpts, width, height, data, newwidth, 
                  newheight, dstImage, element_size, rowsize, group_size );
          break;
        case( GL.GL_UNSIGNED_SHORT ):
          ScaleInternal.scale_internal_ushort( cmpts, width, height, data, newwidth, 
                  newheight, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_SHORT ):
          ScaleInternal.scale_internal_ushort( cmpts, width, height, data, newwidth,
                  newheight, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT ):
          ScaleInternal.scale_internal_uint( cmpts, width, height, data, newwidth,
                  newheight, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_INT ):
          ScaleInternal.scale_internal_int( cmpts, width, height, data, newwidth,
                  newheight, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_FLOAT ):
          ScaleInternal.scale_internal_float( cmpts, width, height, data, newwidth,
                  newheight, dstImage.asFloatBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          ScaleInternal.scaleInternalPackedPixel( 3, new Extract332(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          ScaleInternal.scaleInternalPackedPixel( 3, new Extract233rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          ScaleInternal.scaleInternalPackedPixel( 3, new Extract565(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          ScaleInternal.scaleInternalPackedPixel( 3, new Extract565rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract4444(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract4444rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract5551(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract1555rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract8888(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract8888rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract1010102(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
          ScaleInternal.scaleInternalPackedPixel( 4, new Extract2101010rev(), width, height, data, newwidth,
              newheight, dstImage, element_size, rowsize, myswap_bytes );
          break;
        default:
          assert( false );
          break;
      }
      myswap_bytes = false;
      rowsize = newwidth * group_size;
      // swap dstImage and srcImage
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      
      if( levels != 0 ) { // use as little memory as possible
        int nextWidth = newwidth / 2;
        int nextHeight = newheight / 2;
        if( nextWidth < 1 ) {
          nextWidth = 1;
        }
        if( nextHeight < 1 ) {
          nextHeight = 1;
        }
        
        memreq = Mipmap.image_size( nextWidth, nextHeight, format, type );
        try {
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE ):
            case( GL.GL_BYTE ):
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_INT ):
            case( GL.GL_FLOAT ):
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              dstImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() );
              break;
            default:
              return( GLU.GLU_INVALID_ENUM );
          }
        } catch( OutOfMemoryError ome ) {
          gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
          gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
          gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
          return( GLU.GLU_OUT_OF_MEMORY );
        }
      }
      // level userLevel is in srcImage; nothing saved yet
      level = userLevel;
    }
    
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE );
    if( baseLevel <= level && level <= maxLevel ) {
      srcImage.rewind();
      gl.glTexImage2D( target, level, internalFormat, newwidth, newheight, 0, format, type, srcImage );
      if (DEBUG) {
        System.err.println("GL Error(" + level + "): " + gl.glGetError() );
        if (VERBOSE) {
          srcImage.limit( Mipmap.image_size( newwidth, newheight, format, type ) );
          writeTargaFile("glu2DMipmapJ" + level + ".tga",
                         srcImage, newwidth, newheight);
          srcImage.clear();
        }
      }
    }
    
    level++;  // update current level for the loop
    for( ; level <= levels; level++ ) {
      srcImage.rewind();
      dstImage.rewind();
      switch( type ) {
        case( GL.GL_UNSIGNED_BYTE ):
          HalveImage.halveImage_ubyte( cmpts, newwidth, newheight, srcImage, dstImage, element_size, rowsize, group_size );
          break;
        case( GL.GL_BYTE ):
          HalveImage.halveImage_byte( cmpts, newwidth, newheight, srcImage, dstImage, element_size, rowsize, group_size );
          break;
        case( GL.GL_UNSIGNED_SHORT ):
          HalveImage.halveImage_ushort( cmpts, newwidth, newheight, srcImage, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_SHORT ):
          HalveImage.halveImage_short( cmpts, newwidth, newheight, srcImage, dstImage.asShortBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_INT ):
          HalveImage.halveImage_uint( cmpts, newwidth, newheight, srcImage, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_INT ):
          HalveImage.halveImage_int( cmpts, newwidth, newheight, srcImage, dstImage.asIntBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_FLOAT ):
          HalveImage.halveImage_float( cmpts, newwidth, newheight, srcImage, dstImage.asFloatBuffer(), element_size, rowsize, group_size, myswap_bytes );
          break;
        case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          assert( format == GL.GL_RGB );
          HalveImage.halveImagePackedPixel( 3, new Extract332(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          assert( format == GL.GL_RGB );
          HalveImage.halveImagePackedPixel( 3, new Extract233rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          HalveImage.halveImagePackedPixel( 3, new Extract565(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          HalveImage.halveImagePackedPixel( 3, new Extract565rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          HalveImage.halveImagePackedPixel( 4, new Extract4444(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          HalveImage.halveImagePackedPixel( 4, new Extract4444rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          HalveImage.halveImagePackedPixel( 4, new Extract5551(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          HalveImage.halveImagePackedPixel( 4, new Extract1555rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          HalveImage.halveImagePackedPixel( 4, new Extract8888(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          HalveImage.halveImagePackedPixel( 4, new Extract8888rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          HalveImage.halveImagePackedPixel( 4, new Extract1010102(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
          HalveImage.halveImagePackedPixel( 4, new Extract2101010rev(), newwidth, newheight, srcImage, dstImage, element_size, rowsize, myswap_bytes );
            break;
        default:
          assert( false );
          break;
      }
      
      // swap dstImage and srcImage
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      
      if( newwidth > 1 ) {
        newwidth /= 2;
        rowsize /= 2;
      }
      if( newheight > 1 ) {
        newheight /= 2;
      }
      // compute amount to pad per row if any
      int rowPad = rowsize % psm.getUnpackAlignment();
      
      // should row be padded
      if( rowPad == 0 ) {
        // call teximage with srcImage untouched since its not padded
        if( baseLevel <= level && level <= maxLevel ) {
          srcImage.rewind();
          gl.glTexImage2D( target, level, internalFormat, newwidth, newheight, 0, format, type, srcImage );
          if (DEBUG) {
            System.err.println("GL Error(" + level + "): " + gl.glGetError() );
            if (VERBOSE) {
              srcImage.limit( Mipmap.image_size( newwidth, newheight, format, type ) );
              writeTargaFile("glu2DMipmapJ" + level + ".tga",
                             srcImage, newwidth, newheight);
              srcImage.clear();
            }
          }
        }
      } else {
        // compute length of new row in bytes, including padding
        int newRowLength = rowsize + psm.getUnpackAlignment() - rowPad;
        int ii, jj;
        int dstTrav;
        int srcTrav;
        
        // allocate new image for mipmap of size newRowLength x newheight
        ByteBuffer newMipmapImage = null;
        try {
          newMipmapImage = ByteBuffer.allocateDirect( newRowLength * newheight );
        } catch( OutOfMemoryError ome ) {
          gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
          gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
          gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
          return( GLU.GLU_OUT_OF_MEMORY );
        }
        srcImage.rewind();
        // copy image from srcImage into newMipmapImage by rows
        for( ii = 0; ii < newheight; ii++ ) {
          newMipmapImage.position(newRowLength * ii);
          for( jj = 0; jj < rowsize; jj++ ) {
            newMipmapImage.put( srcImage.get() );
          }
        }
        
        // and use this new image for mipmapping instead
        if( baseLevel <= level && level <= maxLevel ) {
          newMipmapImage.rewind();
          gl.glTexImage2D( target, level, internalFormat, newwidth, newheight, 0, format, type, newMipmapImage );
          if (DEBUG) {
            System.err.println("GL Error(" + level + " padded): " + gl.glGetError() );
            if (VERBOSE) {
              writeTargaFile("glu2DMipmapJ" + level + ".tga",
                             newMipmapImage, newwidth, newheight);
            }
          }
        }
      }
    }
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, (psm.getUnpackSwapBytes() ? 1 : 0) );
    
    return( 0 );
  }
  
  public static int fastBuild2DMipmaps( GL gl, PixelStorageModes psm, int target,
          int components, int width, int height, int format, int type, ByteBuffer data ) {
    int[] newwidth = new int[1];
    int[] newheight = new int[1];
    int level, levels;
    ByteBuffer newImage;
    int newImage_width;
    int newImage_height;
    ByteBuffer otherImage;
    ByteBuffer imageTemp;
    int memreq;
    int maxsize;
    int cmpts;
    
    Mipmap.closestFit( gl, target, width, height, components, format, type, newwidth, 
            newheight );
    
    levels = Mipmap.computeLog( newwidth[0] );
    level = Mipmap.computeLog( newheight[0] );
    if( level > levels ) {
      levels = level;
    }
    
    cmpts = Mipmap.elements_per_group( format, type );
    
    otherImage = null;
    //  No need to copy the user data if its packed correctly.
    //  Make sure that later routines don't change that data.
    
    if( psm.getUnpackSkipRows() == 0 && psm.getUnpackSkipPixels() == 0 ) {
      newImage = data;
      newImage_width = width;
      newImage_height = height;
    } else {
      int rowsize;
      int group_per_line;
      int elements_per_line;
      int start;
      int iter;
      int iter2;
      int i, j;
      
      try {
        newImage = ByteBuffer.allocateDirect( Mipmap.image_size( 
              width, height, format, GL.GL_UNSIGNED_BYTE ) ).order( ByteOrder.nativeOrder() );
      } catch( OutOfMemoryError err ) {
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      newImage_width = width;
      newImage_height = height;

      // Abbreviated version of fill_image for the restricted case.
      if( psm.getUnpackRowLength() > 0 ) {
        group_per_line = psm.getUnpackRowLength();
      } else {
        group_per_line = width;
      }
      rowsize = group_per_line * cmpts;
      elements_per_line = width * cmpts;
      start = psm.getUnpackSkipRows() * rowsize + psm.getUnpackSkipPixels() * cmpts;
      
      for( i = 0; i < height; i++ ) {
        iter = start;
        data.position( iter );
        for( j = 0; j < elements_per_line; j++ ) {
          newImage.put( data.get() );
        }
        start += rowsize;
      }
    }
    
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, 1 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE );
    
    for( level = 0; level <= levels; level++ ) {
      if( newImage_width == newwidth[0] && newImage_height == newheight[0] ) {
        // use newImage for this level
        newImage.rewind();
        gl.glTexImage2D( target, level, components, newImage_width, newImage_height,
                0, format, GL.GL_UNSIGNED_BYTE, newImage );
      } else {
        if( otherImage == null ) {
          memreq = Mipmap.image_size( newwidth[0], newheight[0], format, GL.GL_UNSIGNED_BYTE );
          try {
            otherImage = ByteBuffer.allocateDirect( memreq ).order( ByteOrder.nativeOrder() );
          } catch( OutOfMemoryError err ) {
            gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
            gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
            gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
            gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, ( psm.getUnpackSwapBytes() ? 1 : 0 ) ) ;
            return( GLU.GLU_OUT_OF_MEMORY );
          }
        }
        // swap newImage and otherImage
        imageTemp = otherImage;
        otherImage = newImage;
        newImage = imageTemp;
        
        newImage_width = newwidth[0];
        newImage_height = newheight[0];
        newImage.rewind();
        gl.glTexImage2D( target, level, components, newImage_width, newImage_height,
                0, format, GL.GL_UNSIGNED_BYTE, newImage );
      }
      if( newwidth[0] > 1 ) {
        newwidth[0] /= 2;
      }
      if( newheight[0] > 1 ) {
        newheight[0] /= 2;
      }
    }
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, ( psm.getUnpackSwapBytes() ? 1 : 0 ) ) ;
    
    return( 0 );
  }
  
  public static int gluBuild3DMipmapLevelsCore( GL gl, int target, int internalFormat,
          int width, int height, int depth, int widthPowerOf2, int heightPowerOf2,
          int depthPowerOf2, int format, int type, int userLevel, int baseLevel,
          int maxLevel, ByteBuffer data ) {
    int newWidth;
    int newHeight;
    int newDepth;
    int level, levels;
    ByteBuffer usersImage;
    ByteBuffer srcImage, dstImage, tempImage;
    int newImageWidth;
    int newImageHeight;
    int newImageDepth;
    int memReq;
    int maxSize;
    int cmpts;
    
    boolean myswapBytes;
    int groupsPerLine, elementSize, groupSize;
    int rowsPerImage, imageSize;
    int rowSize, padding;
    PixelStorageModes psm = new PixelStorageModes();
    
    assert( Mipmap.checkMipmapArgs( internalFormat, format, type ) == 0 );
    assert( width >= 1 && height >= 1 && depth >= 1 );
    assert( type != GL.GL_BITMAP );
    
    srcImage = dstImage = null;
    
    newWidth = widthPowerOf2;
    newHeight = heightPowerOf2;
    newDepth = depthPowerOf2;
    levels = Mipmap.computeLog( newWidth );
    level = Mipmap.computeLog( newHeight );
    if( level > levels ) {
      levels = level;
    }
    level = Mipmap.computeLog( newDepth );
    if( level > levels ) {
      levels = level;
    }
    
    levels += userLevel;
    
    Mipmap.retrieveStoreModes3D( gl, psm );
    myswapBytes = psm.getUnpackSwapBytes();
    cmpts = Mipmap.elements_per_group( format, type );
    if( psm.getUnpackRowLength() > 0 ) {
      groupsPerLine = psm.getUnpackRowLength();
    } else {
      groupsPerLine = width;
    }
    
    elementSize = Mipmap.bytes_per_element( type );
    groupSize = elementSize * cmpts;
    if( elementSize == 1 ) {
      myswapBytes = false;
    }
    
    // 3dstuff
    if( psm.getUnpackImageHeight() > 0 ) {
      rowsPerImage = psm.getUnpackImageHeight();
    } else {
      rowsPerImage = height;
    }
    
    rowSize = groupsPerLine * groupSize;
    padding = ( rowSize % psm.getUnpackAlignment() );
    if( padding != 0 ) {
      rowSize += psm.getUnpackAlignment() - padding;
    }
    
    imageSize = rowsPerImage * rowSize;
    
    usersImage = data.duplicate();
    usersImage.position( psm.getUnpackSkipRows() * rowSize +
                         psm.getUnpackSkipPixels() * groupSize +
                         psm.getUnpackSkipImages() * imageSize );
    usersImage.mark();
    
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, 0 );
    gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, 0 );
    
    level = userLevel;
    
    if( width == newWidth && height == newHeight && depth == newDepth ) {
      // use usersImage for level userlevel
      if( baseLevel <= level && level <= maxLevel ) {
        gl.glTexImage3D( target, level, internalFormat, width, height, depth,
                0, format, type, usersImage );
      }
      if( levels == 0 ) { /* we're done. clean up and return */
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
        gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
        return( 0 );
      }
      int nextWidth = newWidth / 2;
      int nextHeight = newHeight / 2;
      int nextDepth = newDepth / 2;
      
      // clamp to one
      if( nextWidth < 1 ) {
        nextWidth = 1;
      }
      if( nextHeight < 1 ) {
        nextHeight = 1;
      }
      if( nextDepth < 1 ) {
        nextDepth = 1;
      }
      memReq = Mipmap.imageSize3D( nextWidth, nextHeight, nextDepth, format, type );
      try {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memReq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError err ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
        gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      
      if( dstImage != null ) {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractUByte(), width, height, depth,
                      usersImage, dstImage, elementSize, 
                      groupSize, rowSize, imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_ubyte( cmpts, width, height, usersImage,
                      dstImage, elementSize, rowSize, groupSize );
            }
            break;
          case( GL.GL_BYTE ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractSByte(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_byte( cmpts, width, height, usersImage,
                      dstImage, elementSize, rowSize, groupSize );
            }
            break;
          case( GL.GL_UNSIGNED_SHORT ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractUShort(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_ushort( cmpts, width, height, usersImage, 
                      dstImage.asShortBuffer(), elementSize, rowSize, groupSize, myswapBytes );
            }
            break;
          case( GL.GL_SHORT ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractSShort(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_short( cmpts, width, height, usersImage,
                      dstImage.asShortBuffer(), elementSize, rowSize, groupSize, myswapBytes );
            }
            break;
          case( GL.GL_UNSIGNED_INT ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractUInt(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_uint( cmpts, width, height, usersImage,
                      dstImage.asIntBuffer(), elementSize, rowSize, groupSize, myswapBytes );
            }
            break;
          case( GL.GL_INT ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractSInt(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_int( cmpts, width, height, usersImage,
                      dstImage.asIntBuffer(), elementSize, rowSize, groupSize, myswapBytes );
            }
            break;
          case( GL.GL_FLOAT ):
            if( depth > 1 ) {
              HalveImage.halveImage3D( cmpts, new ExtractFloat(), width, height, depth,
                      usersImage, dstImage, elementSize, groupSize, rowSize,
                      imageSize, myswapBytes );
            } else {
              HalveImage.halveImage_float( cmpts, width, height, usersImage,
                      dstImage.asFloatBuffer(), elementSize, rowSize, groupSize, myswapBytes );
            }
            break;
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
            assert( format == GL.GL_RGB );
            HalveImage.halveImagePackedPixel3D( 3, new Extract332(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
            assert( format == GL.GL_RGB );
            HalveImage.halveImagePackedPixel3D( 3, new Extract233rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
            HalveImage.halveImagePackedPixel3D( 3, new Extract565(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
            HalveImage.halveImagePackedPixel3D( 3, new Extract565rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract4444(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract4444rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract5551(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract1555rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract8888(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract8888rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract1010102(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            HalveImage.halveImagePackedPixel3D( 4, new Extract2101010rev(), width, height, depth, usersImage,
                    dstImage, elementSize, rowSize, imageSize, myswapBytes );
            break;
          default:
            assert( false );
            break;
        }
      }
      newWidth = width / 2;
      newHeight = height / 2;
      newDepth = depth / 2;
      // clamp to 1
      if( newWidth < 1 ) {
        newWidth = 1;
      }
      if( newHeight < 1 ) {
        newHeight = 1;
      }
      if( newDepth < 1 ) {
        newDepth = 1;
      }
      
      myswapBytes = false;
      rowSize = newWidth * groupSize;
      imageSize = rowSize * newHeight;
      memReq = Mipmap.imageSize3D( newWidth, newHeight, newDepth, format, type );
      // swap srcImage and dstImage
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      try {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memReq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError err ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
        gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      
      // level userLevel + 1 is in srcImage; level userLevel already saved
      level = userLevel + 1;
    } else {
      memReq = Mipmap.imageSize3D( newWidth, newHeight, newDepth, format, type );
      try {
        switch( type ) {
          case( GL.GL_UNSIGNED_BYTE ):
          case( GL.GL_BYTE ):
          case( GL.GL_UNSIGNED_SHORT ):
          case( GL.GL_SHORT ):
          case( GL.GL_UNSIGNED_INT ):
          case( GL.GL_INT ):
          case( GL.GL_FLOAT ):
          case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
            dstImage = ByteBuffer.allocateDirect( memReq ).order( ByteOrder.nativeOrder() );
            break;
          default:
            return( GLU.GLU_INVALID_ENUM );
        }
      } catch( OutOfMemoryError err ) {
        gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
        gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
        gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
        gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
        gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
        return( GLU.GLU_OUT_OF_MEMORY );
      }
      
      ScaleInternal.gluScaleImage3D( gl, format, width, height, depth, type,
              usersImage, newWidth, newHeight, newDepth, type, dstImage );
      
      myswapBytes = false;
      rowSize = newWidth * groupSize;
      imageSize = rowSize * newHeight;
      // swap dstImage and srcImage
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      
      if( levels != 0 ) {
        int nextWidth = newWidth / 2;
        int nextHeight = newHeight / 2;
        int nextDepth = newDepth / 2;
        if( nextWidth < 1 ) {
          nextWidth = 1;
        }
        if( nextHeight < 1 ) {
          nextHeight = 1;
        }
        if( nextDepth < 1 ) {
          nextDepth = 1;
        }
        memReq = Mipmap.imageSize3D( nextWidth, nextHeight, nextDepth, format, type );
        try {
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE ):
            case( GL.GL_BYTE ):
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_INT ):
            case( GL.GL_FLOAT ):
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              dstImage = ByteBuffer.allocateDirect( memReq ).order( ByteOrder.nativeOrder() );
              break;
            default:
              return( GLU.GLU_INVALID_ENUM );
          }
        } catch( OutOfMemoryError err ) {
          gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
          gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
          gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
          gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
          gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
          return( GLU.GLU_OUT_OF_MEMORY );
        }
      }
      // level userLevel is in srcImage; nothing saved yet
      level = userLevel;
    }
    
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, GL.GL_FALSE );
    if( baseLevel <= level && level <= maxLevel ) {
      usersImage.reset();
      gl.glTexImage3D( target, level, internalFormat, width, height, depth,
              0, format, type, usersImage );
    }
    level++;
    for( ; level <= levels; level++ ) {
      switch( type ) {
        case( GL.GL_UNSIGNED_BYTE ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractUByte(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_ubyte( cmpts, width, height, usersImage,
                    dstImage, elementSize, rowSize, groupSize );
          }
          break;
        case( GL.GL_BYTE ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractSByte(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_byte( cmpts, width, height, usersImage,
                    dstImage, elementSize, rowSize, groupSize );
          }
          break;
        case( GL.GL_UNSIGNED_SHORT ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractUShort(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_ushort( cmpts, width, height, usersImage,
                    dstImage.asShortBuffer(), elementSize, rowSize, groupSize, myswapBytes );
          }
          break;
        case( GL.GL_SHORT ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractSShort(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_short( cmpts, width, height, usersImage,
                    dstImage.asShortBuffer(), elementSize, rowSize, groupSize, myswapBytes );
          }
          break;
        case( GL.GL_UNSIGNED_INT ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractUInt(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_uint( cmpts, width, height, usersImage,
                    dstImage.asIntBuffer(), elementSize, rowSize, groupSize, myswapBytes );
          }
          break;
        case( GL.GL_INT ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractSInt(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_int( cmpts, width, height, usersImage,
                    dstImage.asIntBuffer(), elementSize, rowSize, groupSize, myswapBytes );
          }
          break;
        case( GL.GL_FLOAT ):
          if( depth > 1 ) {
            HalveImage.halveImage3D( cmpts, new ExtractFloat(), width, height, depth,
                    usersImage, dstImage, elementSize, groupSize, rowSize,
                    imageSize, myswapBytes );
          } else {
            HalveImage.halveImage_float( cmpts, width, height, usersImage,
                    dstImage.asFloatBuffer(), elementSize, rowSize, groupSize, myswapBytes );
          }
          break;
        case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
          HalveImage.halveImagePackedPixel3D( 3, new Extract332(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
          HalveImage.halveImagePackedPixel3D( 3, new Extract233rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
          HalveImage.halveImagePackedPixel3D( 3, new Extract565(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
          HalveImage.halveImagePackedPixel3D( 3, new Extract565rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract4444(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract4444rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract5551(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract1555rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract8888(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract8888rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract1010102(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
          HalveImage.halveImagePackedPixel3D( 4, new Extract2101010rev(), width, height, depth, usersImage,
                  dstImage, elementSize, rowSize, imageSize, myswapBytes );
          break;
        default:
          assert( false );
          break;
      }
      
      tempImage = srcImage;
      srcImage = dstImage;
      dstImage = tempImage;
      
      if( newWidth > 1 ) {
        newWidth /= 2;
        rowSize /= 2;
      }
      if( newHeight > 1 ) {
        newHeight /= 2;
        imageSize = rowSize * newHeight;
      }
      if( newDepth > 1 ) {
        newDepth /= 2;
      }
      if( baseLevel <= level && level <= maxLevel ) {
        usersImage.reset();
        gl.glTexImage3D( target, level, internalFormat, width, height, depth,
                0, format, type, usersImage );
      }
    }
    gl.glPixelStorei( GL.GL_UNPACK_ALIGNMENT, psm.getUnpackAlignment() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_ROWS, psm.getUnpackSkipRows() );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_PIXELS, psm.getUnpackSkipPixels() );
    gl.glPixelStorei( GL.GL_UNPACK_ROW_LENGTH, psm.getUnpackRowLength() );
    gl.glPixelStorei( GL.GL_UNPACK_SWAP_BYTES, psm.getUnpackSwapBytes() ? 1 : 0 );
    gl.glPixelStorei( GL.GL_UNPACK_SKIP_IMAGES, psm.getUnpackSkipImages() );
    gl.glPixelStorei( GL.GL_UNPACK_IMAGE_HEIGHT, psm.getUnpackImageHeight() );
    return( 0 );
  }

  private static final int TARGA_HEADER_SIZE = 18;
  private static void writeTargaFile(String filename, ByteBuffer data,
                                     int width, int height) {
    try {
      FileOutputStream fos = new FileOutputStream(new File(filename));
      ByteBuffer header = ByteBuffer.allocate(TARGA_HEADER_SIZE);
      header.put(0, (byte) 0).put(1, (byte) 0);
      header.put(2, (byte) 2); // uncompressed type
      header.put(12, (byte) (width & 0xFF)); // width
      header.put(13, (byte) (width >> 8)); // width
      header.put(14, (byte) (height & 0xFF)); // height
      header.put(15, (byte) (height >> 8)); // height
      header.put(16, (byte) 24); // pixel size
      fos.getChannel().write(header);
      fos.getChannel().write(data);
      data.clear();
      fos.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
