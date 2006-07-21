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
import javax.media.opengl.GLException;
import java.nio.*;

/**
 *
 * @author  Administrator
 */
public class Mipmap {
  
  /** Creates a new instance of Mipmap */
  public Mipmap() {
  }
  
  public static int computeLog( int value ) {
    int i = 0;
    // Error
    if( value == 0 ) {
      return( -1 );
    }
    for( ;; ) {
      if( (value & 1) >= 1 ) {
        if( value != 1 ) {
          return( -1 );
        }
        return( i );
      }
      value = value >> 1;
      i++;
    }
  }
  
  /* Compute the nearest power of 2 number.  This algorithm is a little strange
   * but it works quite well.
   */
  public static int nearestPower( int value ) {
    int i = 1;
    // Error!
    if( value == 0 ) {
      return( -1 );
    }
    for( ;; ) {
      if( value == 1 ) {
        return( i );
      } else if( value == 3 ) {
        return( i * 4 );
      }
      value = value >> 1;
      i *= 2;
    }
  }
  
  public static short GLU_SWAP_2_BYTES( short s ) {
    byte b = 0;
    b = (byte)( s >>> 8 );
    s = (short)( s << 8 );
    s = (short)( s | (0x00FF & b) );
    return( s );
  }
  
  public static int GLU_SWAP_4_BYTES( int i ) {
    int t = i << 24;
    t |= 0x00FF0000 & ( i << 8 );
    t |= 0x0000FF00 & ( i >>> 8 );
    t |= 0x000000FF & ( i >>> 24 );
    return( t );
  }
  
  public static float GLU_SWAP_4_BYTES( float f ) {
    int i = Float.floatToRawIntBits( f );
    float temp = Float.intBitsToFloat( i );
    return( temp );
  }
  
  public static int checkMipmapArgs( int internalFormat, int format, int type ) {
    if( !legalFormat( format ) || !legalType( type ) ) {
      return( GLU.GLU_INVALID_ENUM );
    }
    if( format == GL.GL_STENCIL_INDEX ) {
      return( GLU.GLU_INVALID_ENUM );
    }
    if( !isLegalFormatForPackedPixelType( format, type ) ) {
      return( GLU.GLU_INVALID_OPERATION );
    }
    return( 0 );
  }
  
  public static boolean legalFormat( int format ) {
    switch( format ) {
      case( GL.GL_COLOR_INDEX ):
      case( GL.GL_STENCIL_INDEX ):
      case( GL.GL_DEPTH_COMPONENT ):
      case( GL.GL_RED ):
      case( GL.GL_GREEN ):
      case( GL.GL_BLUE ):
      case( GL.GL_ALPHA ):
      case( GL.GL_RGB ):
      case( GL.GL_RGBA ):
      case( GL.GL_LUMINANCE ):
      case( GL.GL_LUMINANCE_ALPHA ):
      case( GL.GL_BGR ):
      case( GL.GL_BGRA ):
        return( true );
      default:
        return( false );
    }
  }
  
  public static boolean legalType( int type ) {
    switch( type ) {
      case( GL.GL_BITMAP ):
      case( GL.GL_BYTE ):
      case( GL.GL_UNSIGNED_BYTE ):
      case( GL.GL_SHORT ):
      case( GL.GL_UNSIGNED_SHORT ):
      case( GL.GL_INT ):
      case( GL.GL_UNSIGNED_INT ):
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
        return( true );
      default:
        return( false );
    }
  }
  
  public static boolean isTypePackedPixel( int type ) {
    assert( legalType( type ) );
    
    if( type == GL.GL_UNSIGNED_BYTE_3_3_2 ||
        type == GL.GL_UNSIGNED_BYTE_2_3_3_REV ||
        type == GL.GL_UNSIGNED_SHORT_5_6_5 ||
        type == GL.GL_UNSIGNED_SHORT_5_6_5_REV ||
        type == GL.GL_UNSIGNED_SHORT_4_4_4_4 ||
        type == GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ||
        type == GL.GL_UNSIGNED_SHORT_5_5_5_1 ||
        type == GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ||
        type == GL.GL_UNSIGNED_INT_8_8_8_8 ||
        type == GL.GL_UNSIGNED_INT_8_8_8_8_REV ||
        type == GL.GL_UNSIGNED_INT_10_10_10_2 ||
        type == GL.GL_UNSIGNED_INT_2_10_10_10_REV ) {
          return( true );
    }
    return( false );
  }
  
  public static boolean isLegalFormatForPackedPixelType( int format, int type ) {
    // if not a packed pixel type then return true
    if( isTypePackedPixel( type ) ) {
      return( true );
    }
    
    // 3_3_2/2_3_3_REV & 5_6_5/5_6_5_REV are only compatible with RGB
    if( (type == GL.GL_UNSIGNED_BYTE_3_3_2 || type == GL.GL_UNSIGNED_BYTE_2_3_3_REV ||
        type == GL.GL_UNSIGNED_SHORT_5_6_5 || type == GL.GL_UNSIGNED_SHORT_5_6_5_REV )
        & format != GL.GL_RGB ) {
          return( false );
    }
    
    // 4_4_4_4/4_4_4_4_REV & 5_5_5_1/1_5_5_5_REV & 8_8_8_8/8_8_8_8_REV &
    // 10_10_10_2/2_10_10_10_REV are only campatible with RGBA, BGRA & ARGB_EXT
    if( ( type == GL.GL_UNSIGNED_SHORT_4_4_4_4 ||
          type == GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ||
          type == GL.GL_UNSIGNED_SHORT_5_5_5_1 ||
          type == GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ||
          type == GL.GL_UNSIGNED_INT_8_8_8_8 ||
          type == GL.GL_UNSIGNED_INT_8_8_8_8_REV ||
          type == GL.GL_UNSIGNED_INT_10_10_10_2 ||
          type == GL.GL_UNSIGNED_INT_2_10_10_10_REV ) &&
          (format != GL.GL_RGBA && format != GL.GL_BGRA) ) {
            return( false );
    }
    return( true );
  }
  
  public static boolean isLegalLevels( int userLevel, int baseLevel, int maxLevel,
                                            int totalLevels ) {
    if( (baseLevel < 0) || (baseLevel < userLevel) || (maxLevel < baseLevel) ||
                                                  (totalLevels < maxLevel) ) {
      return( false );
    }
    return( true );
  }
  
  /* Given user requested textures size, determine if it fits. If it doesn't then
   * halve both sides and make the determination again until it does fit ( for
   * IR only ).
   * Note that proxy textures are not implemented in RE* even though they
   * advertise the texture extension.
   * Note that proxy textures are implemented but not according to spec in IMPACT*
   */
  public static void closestFit( GL gl, int target, int width, int height, int internalFormat,
                                int format, int type, int[] newWidth, int[] newHeight ) {
    // Use proxy textures if OpenGL version >= 1.1
    if( Double.parseDouble( gl.glGetString( GL.GL_VERSION ).trim().substring( 0, 3 ) ) >= 1.1 ) {
      int widthPowerOf2 = nearestPower( width );
      int heightPowerOf2 = nearestPower( height );
      int[] proxyWidth = new int[1];
      boolean noProxyTextures = false;
      
      // Some drivers (in particular, ATI's) seem to set a GL error
      // when proxy textures are used even though this is in violation
      // of the spec. Guard against this and interactions with the
      // DebugGL by watching for GLException.
      try {
        do {
          // compute level 1 width & height, clamping each at 1
          int widthAtLevelOne = ( ( width > 1 ) ? (widthPowerOf2 >> 1) : widthPowerOf2 );
          int heightAtLevelOne = ( ( height > 1 ) ? (heightPowerOf2 >> 1) : heightPowerOf2 );
          int proxyTarget;
        
          assert( widthAtLevelOne > 0 );
          assert( heightAtLevelOne > 0 );
        
          // does width x height at level 1 & all their mipmaps fit?
          if( target == GL.GL_TEXTURE_2D || target == GL.GL_PROXY_TEXTURE_2D ) {
            proxyTarget = GL.GL_PROXY_TEXTURE_2D;
            gl.glTexImage2D( proxyTarget, 1, internalFormat, widthAtLevelOne,
                             heightAtLevelOne, 0, format, type, null );
          } else if( (target == GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X) || 
                     (target == GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X) || 
                     (target == GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y) || 
                     (target == GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y) || 
                     (target == GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z) || 
                     (target == GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z) ) {
            proxyTarget = GL.GL_PROXY_TEXTURE_CUBE_MAP;
            gl.glTexImage2D( proxyTarget, 1, internalFormat, widthAtLevelOne,
                             heightAtLevelOne, 0, format, type, null );
          } else {
            assert( target == GL.GL_TEXTURE_1D || target == GL.GL_PROXY_TEXTURE_1D );
            proxyTarget = GL.GL_PROXY_TEXTURE_1D;
            gl.glTexImage1D( proxyTarget, 1, internalFormat, widthAtLevelOne, 
                             0, format, type, null );
          }
          gl.glGetTexLevelParameteriv( proxyTarget, 1, GL.GL_TEXTURE_WIDTH, proxyWidth, 0 );
          // does it fit?
          if( proxyWidth[0] == 0 ) { // nope, so try again with theses sizes
            if( widthPowerOf2 == 1 && heightPowerOf2 == 1 ) {
              /* A 1x1 texture couldn't fit for some reason so break out.  This
               * should never happen.  But things happen.  The disadvantage with
               * this if-statement is that we will never be aware of when this
               * happens since it will silently branch out.
               */
              noProxyTextures = true;
              break;
            }
            widthPowerOf2 = widthAtLevelOne;
            heightPowerOf2 = heightAtLevelOne;
          }
          // else it does fit
        } while( proxyWidth[0] == 0 );
      } catch (GLException e) {
        noProxyTextures = true;
      }
      // loop must terminate
      // return the width & height at level 0 that fits
      if( !noProxyTextures ) {
        newWidth[0] = widthPowerOf2;
        newHeight[0] = heightPowerOf2;
        return;
      }
    }
    int[] maxsize = new int[1];
    gl.glGetIntegerv( GL.GL_MAX_TEXTURE_SIZE, maxsize , 0);
    // clamp user's texture sizes to maximum sizes, if necessary
    newWidth[0] = nearestPower( width );
    if( newWidth[0] > maxsize[0] ) {
      newWidth[0] = maxsize[0];
    }
    newHeight[0] = nearestPower( height );
    if( newHeight[0] > maxsize[0] ) {
      newHeight[0] = maxsize[0];
    }
  }
  
  public static void closestFit3D( GL gl, int target, int width, int height, int depth, 
          int internalFormat, int format, int type, int[] newWidth, int[] newHeight,
          int[] newDepth ) {
    int widthPowerOf2 = nearestPower( width );
    int heightPowerOf2 = nearestPower( height );
    int depthPowerOf2 = nearestPower( depth );
    int[] proxyWidth = new int[1];
    
    do {
      // compute level 1 width & height & depth, clamping each at 1
      int widthAtLevelOne = (widthPowerOf2 > 1) ? widthPowerOf2 >> 1 : widthPowerOf2;
      int heightAtLevelOne = (heightPowerOf2 > 1) ? heightPowerOf2 >> 1 : heightPowerOf2;
      int depthAtLevelOne = (depthPowerOf2 > 1) ? depthPowerOf2 >> 1 : depthPowerOf2;
      int proxyTarget = 0;
      assert( widthAtLevelOne > 0 );
      assert( heightAtLevelOne > 0 );
      assert( depthAtLevelOne > 0 );
      
      // does width x height x depth at level 1 & all their mipmaps fit?
      if( target == GL.GL_TEXTURE_3D || target == GL.GL_PROXY_TEXTURE_3D ) {
        proxyTarget = GL.GL_PROXY_TEXTURE_3D;
        gl.glTexImage3D( proxyTarget, 1, internalFormat, widthAtLevelOne,
                heightAtLevelOne, depthAtLevelOne, 0, format, type, null );
      }
      gl.glGetTexLevelParameteriv( proxyTarget, 1, GL.GL_TEXTURE_WIDTH, proxyWidth, 0 );
      // does it fit
      if( proxyWidth[0] == 0 ) {
        if( widthPowerOf2 == 1 && heightPowerOf2 == 1 && depthPowerOf2 == 1 ) {
          newWidth[0] = newHeight[0] = newDepth[0] = 1;
          return;
        }
        widthPowerOf2 = widthAtLevelOne;
        heightPowerOf2 = heightAtLevelOne;
        depthPowerOf2 = depthAtLevelOne;
      }
    } while( proxyWidth[0] == 0 );
    // loop must terminate
    
    // return the width & height at level 0 that fits
    newWidth[0] = widthPowerOf2;
    newHeight[0] = heightPowerOf2;
    newDepth[0] = depthPowerOf2;
  }
  
  public static int elements_per_group( int format, int type ) {
    // Return the number of elements per grtoup of a specified gromat
    
    // If the type is packedpixels then answer is 1
    if( type == GL.GL_UNSIGNED_BYTE_3_3_2 ||
        type == GL.GL_UNSIGNED_BYTE_2_3_3_REV ||
        type == GL.GL_UNSIGNED_SHORT_5_6_5 ||
        type == GL.GL_UNSIGNED_SHORT_5_6_5_REV ||
        type == GL.GL_UNSIGNED_SHORT_4_4_4_4 ||
        type == GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ||
        type == GL.GL_UNSIGNED_SHORT_5_5_5_1 ||
        type == GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ||
        type == GL.GL_UNSIGNED_INT_8_8_8_8 ||
        type == GL.GL_UNSIGNED_INT_8_8_8_8_REV ||
        type == GL.GL_UNSIGNED_INT_10_10_10_2 ||
        type == GL.GL_UNSIGNED_INT_2_10_10_10_REV ) {
          return( 1 );
    }
    
    // Types are not packed pixels so get elements per group
    switch( format ) {
      case( GL.GL_RGB ):
      case( GL.GL_BGR ):
        return( 3 );
      case( GL.GL_LUMINANCE_ALPHA ):
        return( 2 );
      case( GL.GL_RGBA ):
      case( GL.GL_BGRA ):
        return( 4 );
      default:
        return( 1 );
    }
  }
  
  public static int bytes_per_element( int type ) {
    // return the number of bytes per element, based on the element type
    
    switch( type ) {
      case( GL.GL_BITMAP ):
      case( GL.GL_BYTE ):
      case( GL.GL_UNSIGNED_BYTE ):
      case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
      case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
        return( 1 );
      case( GL.GL_SHORT ):
      case( GL.GL_UNSIGNED_SHORT ):
      case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
      case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
      case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
      case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
        return( 2 );
      case( GL.GL_INT ):
      case( GL.GL_UNSIGNED_INT ):
      case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
      case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
      case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
      case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
      case( GL.GL_FLOAT ):
        return( 4 );
      default:
        return( 4 );
    }
  }
  
  public static boolean is_index( int format ) {
    return( format == GL.GL_COLOR_INDEX || format == GL.GL_STENCIL_INDEX );
  }
  
  /* Compute memory required for internal packed array of data of given type and format. */
  
  public static int image_size( int width, int height, int format, int type ) {
    int bytes_per_row;
    int components;
    
    assert( width > 0 );
    assert( height > 0 );
    components = elements_per_group( format, type );
    if( type == GL.GL_BITMAP ) {
      bytes_per_row = (width + 7) / 8;
    } else {
      bytes_per_row = bytes_per_element( type ) * width;
    }
    return( bytes_per_row * height * components );
  }
  
  public static int imageSize3D( int width, int height, int depth, int format, int type ) {
    int components = elements_per_group( format, type );
    int bytes_per_row = bytes_per_element( type ) * width;
    
    assert( width > 0 && height > 0 && depth > 0 );
    assert( type != GL.GL_BITMAP );
    
    return( bytes_per_row * height * depth * components );
  }
  
  public static void retrieveStoreModes( GL gl, PixelStorageModes psm ) {
    int[] a = new int[1];
    gl.glGetIntegerv( GL.GL_UNPACK_ALIGNMENT, a, 0);
    psm.setUnpackAlignment( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_ROW_LENGTH, a, 0);
    psm.setUnpackRowLength( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_SKIP_ROWS, a, 0);
    psm.setUnpackSkipRows( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_SKIP_PIXELS, a, 0);
    psm.setUnpackSkipPixels( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_LSB_FIRST, a, 0);
    psm.setUnpackLsbFirst( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_UNPACK_SWAP_BYTES, a, 0);
    psm.setUnpackSwapBytes( ( a[0] == 1 ) );
    
    gl.glGetIntegerv( GL.GL_PACK_ALIGNMENT, a, 0);
    psm.setPackAlignment( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_ROW_LENGTH, a, 0);
    psm.setPackRowLength( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_SKIP_ROWS, a, 0);
    psm.setPackSkipRows( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_SKIP_PIXELS, a, 0);
    psm.setPackSkipPixels( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_LSB_FIRST, a, 0);
    psm.setPackLsbFirst( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_PACK_SWAP_BYTES, a, 0);
    psm.setPackSwapBytes( ( a[0] == 1 ) );
  }
  
  public static void retrieveStoreModes3D( GL gl, PixelStorageModes psm ) {
    int[] a = new int[1];
    gl.glGetIntegerv( GL.GL_UNPACK_ALIGNMENT, a, 0);
    psm.setUnpackAlignment( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_ROW_LENGTH, a, 0);
    psm.setUnpackRowLength( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_SKIP_ROWS, a, 0);
    psm.setUnpackSkipRows( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_SKIP_PIXELS, a, 0);
    psm.setUnpackSkipPixels( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_LSB_FIRST, a, 0);
    psm.setUnpackLsbFirst( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_UNPACK_SWAP_BYTES, a, 0);
    psm.setUnpackSwapBytes( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_UNPACK_SKIP_IMAGES, a, 0);
    psm.setUnpackSkipImages( a[0] );
    gl.glGetIntegerv( GL.GL_UNPACK_IMAGE_HEIGHT, a, 0);
    psm.setUnpackImageHeight( a[0] );
    
    gl.glGetIntegerv( GL.GL_PACK_ALIGNMENT, a, 0);
    psm.setPackAlignment( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_ROW_LENGTH, a, 0);
    psm.setPackRowLength( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_SKIP_ROWS, a, 0);
    psm.setPackSkipRows( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_SKIP_PIXELS, a, 0 );
    psm.setPackSkipPixels( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_LSB_FIRST, a, 0 );
    psm.setPackLsbFirst( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_PACK_SWAP_BYTES, a, 0 );
    psm.setPackSwapBytes( ( a[0] == 1 ) );
    gl.glGetIntegerv( GL.GL_PACK_SKIP_IMAGES, a, 0 );
    psm.setPackSkipImages( a[0] );
    gl.glGetIntegerv( GL.GL_PACK_IMAGE_HEIGHT, a, 0 );
    psm.setPackImageHeight( a[0] );
  }
  
  public static int gluScaleImage( GL gl, int format, int widthin, int heightin, 
          int typein, ByteBuffer datain, int widthout, int heightout, 
          int typeout, ByteBuffer dataout ) {
    int datainPos = datain.position();
    int dataoutPos = dataout.position();
    try {

      int components;
      ByteBuffer beforeimage;
      ByteBuffer afterimage;
      PixelStorageModes psm = new PixelStorageModes();
    
      if( (widthin == 0)  || (heightin == 0) || (widthout == 0) || (heightout == 0) ) {
        return( 0 );
      }
      if( (widthin < 0) || (heightin < 0) || (widthout < 0) || (heightout < 0) ) {
        return( GLU.GLU_INVALID_VALUE );
      }
      if( !legalFormat( format ) || !legalType( typein ) || !legalType( typeout ) ) {
        return( GLU.GLU_INVALID_ENUM );
      }
      if( !isLegalFormatForPackedPixelType( format, typein ) ) {
        return( GLU.GLU_INVALID_OPERATION );
      }
      if( !isLegalFormatForPackedPixelType( format, typeout ) ) {
        return( GLU.GLU_INVALID_OPERATION );
      }
      beforeimage = ByteBuffer.allocateDirect( image_size( widthin, heightin, format, GL.GL_UNSIGNED_SHORT ) ).order( ByteOrder.nativeOrder() );
      afterimage = ByteBuffer.allocateDirect( image_size( widthout, heightout, format, GL.GL_UNSIGNED_SHORT ) ).order( ByteOrder.nativeOrder() );
      if( beforeimage == null || afterimage == null ) {
        return( GLU.GLU_OUT_OF_MEMORY );
      }
    
      retrieveStoreModes( gl, psm );
      Image.fill_image( psm, widthin, heightin, format, typein, is_index( format ), datain, beforeimage.asShortBuffer() );
      components = elements_per_group( format, 0 );
      ScaleInternal.scale_internal( components, widthin, heightin, beforeimage.asShortBuffer(), widthout, heightout, afterimage.asShortBuffer() );
      Image.empty_image( psm, widthout, heightout, format, typeout, is_index( format ), afterimage.asShortBuffer(), dataout );
    
      return( 0 );
    } finally {
      datain.position(datainPos);
      dataout.position(dataoutPos);
    }
  }
  
  public static int gluBuild1DMipmapLevels( GL gl, int target, int internalFormat,
                          int width, int format, int type, int userLevel, int baseLevel,
                          int maxLevel, ByteBuffer data ) {
    int dataPos = data.position();
    try {

      int levels;
    
      int rc = checkMipmapArgs( internalFormat, format, type );
      if( rc != 0 ) {
        return( rc );
      }
    
      if( width < 1 ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      levels = computeLog( width );
    
      levels += userLevel;
      if( !isLegalLevels( userLevel, baseLevel, maxLevel, levels ) ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      return( BuildMipmap.gluBuild1DMipmapLevelsCore( gl, target, internalFormat, width,
              width, format, type, userLevel, baseLevel, maxLevel, data ) );
    } finally {
      data.position(dataPos);
    }
  }
  
  public static int gluBuild1DMipmaps( GL gl, int target, int internalFormat, int width,
              int format, int type, ByteBuffer data ) {
    int dataPos = data.position();

    try {
      int[] widthPowerOf2 = new int[1];
      int levels;
      int[] dummy = new int[1];
    
      int rc = checkMipmapArgs( internalFormat, format, type );
      if( rc != 0 ) {
        return( rc );
      }
    
      if( width < 1 ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      closestFit( gl, target, width, 1, internalFormat, format, type, widthPowerOf2, dummy );
      levels = computeLog( widthPowerOf2[0] );
    
      return( BuildMipmap.gluBuild1DMipmapLevelsCore( gl, target, internalFormat, 
                    width, widthPowerOf2[0], format, type, 0, 0, levels, data ) );
    } finally {
      data.position(dataPos);
    }
  }

 
  public static int gluBuild2DMipmapLevels( GL gl, int target, int internalFormat,
          int width, int height, int format, int type, int userLevel,
          int baseLevel, int maxLevel, Object data ) {
    int dataPos = 0;

    int level, levels;
    
    int rc = checkMipmapArgs( internalFormat, format, type );
    if( rc != 0 ) {
      return( rc );
    }
    
    if( width < 1 || height < 1 ) {
      return( GLU.GLU_INVALID_VALUE );
    }
    
    levels = computeLog( width );
    level = computeLog( height );
    if( level > levels ) {
      levels = level;
    }
    
    levels += userLevel;
    if( !isLegalLevels( userLevel, baseLevel, maxLevel, levels ) ) {
      return( GLU.GLU_INVALID_VALUE );
    }
    
    //PointerWrapper pointer = PointerWrapperFactory.getPointerWrapper( data );
    ByteBuffer buffer = null;
    if( data instanceof ByteBuffer ) {
        buffer = (ByteBuffer)data;
        dataPos = buffer.position();
    } else if( data instanceof byte[] ) {
      byte[] array = (byte[])data;
      buffer = ByteBuffer.allocateDirect(array.length);
      buffer.put(array);
    } else if( data instanceof short[] ) {
        short[] array = (short[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 2 );
        ShortBuffer sb = buffer.asShortBuffer();
        sb.put( array );
    } else if( data instanceof int[] ) {
        int[] array = (int[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 4 );
        IntBuffer ib = buffer.asIntBuffer();
        ib.put( array );
    } else if( data instanceof float[] ) {
        float[] array = (float[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 4 );
        FloatBuffer fb = buffer.asFloatBuffer();
        fb.put( array );
    }
    
    try {
      return( BuildMipmap.gluBuild2DMipmapLevelsCore( gl, target, internalFormat,
              width, height, width, height, format, type, userLevel, baseLevel,
              maxLevel, buffer ) );
    } finally {
      buffer.position(dataPos);
    }
  }

 
  public static int gluBuild2DMipmaps( GL gl, int target, int internalFormat,
          int width, int height, int format, int type, Object data ) {
    int dataPos = 0;

    int[] widthPowerOf2 = new int[1];
    int[] heightPowerOf2 = new int[1];
    int level, levels;
    
    int rc = checkMipmapArgs( internalFormat, format, type );
    if( rc != 0 ) {
      return( rc );
    }
    
    if( width < 1 || height < 1 ) {
      return( GLU.GLU_INVALID_VALUE );
    }
    
    closestFit( gl, target, width, height, internalFormat, format, type, 
            widthPowerOf2, heightPowerOf2 );
    
    levels = computeLog( widthPowerOf2[0] );
    level = computeLog( heightPowerOf2[0] );
    if( level > levels ) {
      levels = level;
    }
    
    //PointerWrapper pointer = PointerWrapperFactory.getPointerWrapper( data );
    ByteBuffer buffer = null;
    if( data instanceof ByteBuffer ) {
        buffer = (ByteBuffer)data;
        dataPos = buffer.position();
    } else if( data instanceof byte[] ) {
      byte[] array = (byte[])data;
      buffer = ByteBuffer.allocateDirect(array.length);
      buffer.put(array);
    } else if( data instanceof short[] ) {
        short[] array = (short[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 2 );
        ShortBuffer sb = buffer.asShortBuffer();
        sb.put( array );
    } else if( data instanceof int[] ) {
        int[] array = (int[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 4 );
        IntBuffer ib = buffer.asIntBuffer();
        ib.put( array );
    } else if( data instanceof float[] ) {
        float[] array = (float[])data;
        buffer = ByteBuffer.allocateDirect( array.length * 4 );
        FloatBuffer fb = buffer.asFloatBuffer();
        fb.put( array );
    }
    
    try {
      return( BuildMipmap.gluBuild2DMipmapLevelsCore( gl, target, internalFormat, 
              width, height, widthPowerOf2[0], heightPowerOf2[0], format, type, 0, 
              0, levels, buffer ) );
    } finally {
      buffer.position(dataPos);
    }
  }
 
 
  public static int gluBuild3DMipmaps( GL gl, int target, int internalFormat,
          int width, int height, int depth, int format, int type, ByteBuffer data ) {
    int dataPos = data.position();
    try {

      int[] widthPowerOf2 = new int[1];
      int[] heightPowerOf2 = new int[1];
      int[] depthPowerOf2 = new int[1];
      int level, levels;
    
      int rc = checkMipmapArgs( internalFormat, format, type );
      if( rc != 0 ) {
        return( rc );
      }
    
      if( width < 1 || height < 1 || depth < 1 ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      if( type == GL.GL_BITMAP ) {
        return( GLU.GLU_INVALID_ENUM );
      }
    
      closestFit3D( gl, target, width, height, depth, internalFormat, format,
                    type, widthPowerOf2, heightPowerOf2, depthPowerOf2 );
    
      levels = computeLog( widthPowerOf2[0] );
      level = computeLog( heightPowerOf2[0] );
      if( level > levels ) {
        levels = level;
      }
      level = computeLog( depthPowerOf2[0] );
      if( level > levels ) {
        levels = level;
      }
    
      return( BuildMipmap.gluBuild3DMipmapLevelsCore( gl, target, internalFormat, width,
              height, depth, widthPowerOf2[0], heightPowerOf2[0], depthPowerOf2[0],
              format, type, 0, 0, levels, data ) );
    } finally {
      data.position(dataPos);
    }
  }
  
  public static int gluBuild3DMipmapLevels( GL gl, int target, int internalFormat,
          int width, int height, int depth, int format, int type, int userLevel,
          int baseLevel, int maxLevel, ByteBuffer data ) {
    int dataPos = data.position();
    try {
      int level, levels;
    
      int rc = checkMipmapArgs( internalFormat, format, type );
      if( rc != 0 ) {
        return( rc );
      }
    
      if( width < 1 || height < 1 || depth < 1 ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      if( type == GL.GL_BITMAP ) {
        return( GLU.GLU_INVALID_ENUM );
      }
    
      levels = computeLog( width );
      level = computeLog( height );
      if( level > levels ) {
        levels = level;
      }
      level = computeLog( depth );
      if( level > levels ) {
        levels = level;
      }
    
      levels += userLevel;
      if( !isLegalLevels( userLevel, baseLevel, maxLevel, levels ) ) {
        return( GLU.GLU_INVALID_VALUE );
      }
    
      return( BuildMipmap.gluBuild3DMipmapLevelsCore( gl, target, internalFormat, width,
              height, depth, width, height, depth, format, type, userLevel,
              baseLevel, maxLevel, data ) );
    } finally {
      data.position(dataPos);
    }
  }
}
