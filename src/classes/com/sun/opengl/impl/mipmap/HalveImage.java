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
import java.nio.*;

/**
 *
 * @author  Administrator
 */
public class HalveImage {
  
  private static final int BOX2 = 2;
  private static final int BOX4 = 4;
  private static final int BOX8 = 8;
  
  public static void halveImage( int components, int width, int height,
          ShortBuffer datain, ShortBuffer dataout ) {
    int i, j, k;
    int newwidth, newheight;
    int delta;
    int t = 0;
    short temp = 0;
    
    newwidth = width / 2;
    newheight = height /2;
    delta = width * components;
    
    // Piece of cake
    for( i = 0; i < newheight; i++ ) {
      for( j = 0; j < newwidth; j++ ) {
        for( k = 0; k < components; k++ ) {
          datain.position( t );
          temp = datain.get();
          datain.position( t + components );
          temp += datain.get();
          datain.position( t + delta );
          temp += datain.get();
          datain.position( t + delta + components );
          temp +=datain.get();
          temp += 2;
          temp /= 4;
          dataout.put( temp );
          t++;
        }
        t += components;
      }
      t += delta;
    }
  }
  
  public static void halveImage_ubyte( int components, int width, int height,
                                      ByteBuffer datain, ByteBuffer dataout,
                                      int element_size, int ysize, int group_size ) {
    int i, j, k;
    int newwidth, newheight;
    int s;
    int t;
    
    // Handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_ubyte( components, width, height, datain, dataout, element_size, ysize, group_size );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    s = 0;
    t = 0;
    
    int temp = 0;
    // piece of cake
    for( i = 0; i < newheight; i++ ) {
      for( j = 0; j < newwidth; j++ ) {
        for( k = 0; k < components; k++ ) {
          datain.position( t );
          temp = ( 0x000000FF & datain.get() );
          datain.position( t + group_size );
          temp += ( 0x000000FF & datain.get() );
          datain.position( t + ysize );
          temp += ( 0x000000FF & datain.get() );
          datain.position( t + ysize + group_size );
          temp += ( 0x000000FF & datain.get() ) + 2;
          dataout.put( (byte)(temp / 4) );
          t += element_size;
        }
        t += group_size;
      }
      t += ysize;
    }
  }
  
  public static void halve1Dimage_ubyte( int components, int width, int height,
                      ByteBuffer datain, ByteBuffer dataout, 
                      int element_size, int ysize, int group_size ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    int temp = 0;
    
    assert( width == 1 || height == 1 ); // Must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // widthxheight can't be 1x1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          datain.position( src );
          temp = ( 0x000000FF & datain.get() );
          datain.position( src + group_size );
          temp += ( 0x000000FF & datain.get() );
          temp /= 2;
          dataout.put( (byte)temp );
          /*
          dataout.setByte( (byte)(((0x000000FF & datain.setIndexInBytes(src).getByte()) + 
                    (0x000000FF & datain.setIndexInBytes( src + group_size ).getByte())) / 2 ) );
           */
          src += element_size;
          //dataout.plusPlus();
          dest++;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) { // 1 column
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 );
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          datain.position( src );
          temp = ( 0x000000FF & datain.get() );
          datain.position( src + ysize );
          temp += ( 0x000000FF & datain.get() );
          temp /= 2;
          dataout.put( (byte)temp );
          /*
          dataout.setByte( (byte)(((0x000000FF & datain.setIndexInBytes(src).getByte()) + 
                    (0x000000FF & datain.setIndexInBytes(src + ysize).getByte()) ) / 2 ) );
           */
          src += element_size;
          //dataout.plusPlus();
          dest++;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
    }
    assert( src == ysize * height );
    assert( dest == components * element_size * halfWidth * halfHeight );
  }
  
  public static void halveImage_byte( int components, int width, int height,
                    ByteBuffer datain, ByteBuffer dataout, int element_size,
                    int ysize, int group_size ) {
    int i, j, k;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    byte temp = (byte)0;
    
    // handle case where there is only 1 column
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) );
      halve1Dimage_byte( components, width, height, datain, dataout, element_size,
                                                            ysize, group_size );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    for( i = 0; i < newheight; i++ ) {
      for( j = 0; j < newwidth; j++ ) {
        for( k = 0; k < components; k++ ) {
          datain.position( t );
          temp = datain.get();
          datain.position( t + group_size );
          temp += datain.get();
          datain.position( t + ysize );
          temp += datain.get();
          datain.position( t + ysize + group_size );
          temp += datain.get();
          temp += 2;
          temp /= 4;
          dataout.put( temp );
          t += element_size;
        }
        t += group_size;
      }
      t += ysize;
    }
  }
  
  public static void halve1Dimage_byte( int components, int width, int height,
                      ByteBuffer datain, ByteBuffer dataout,
                      int element_size, int ysize, int group_size ) {
    int halfWidth = width / 2;
    int halfHeight = width / 2;
    int src = 0;
    int dest = 0;
    int jj;
    byte temp = (byte)0;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // widthxheight can't be 1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          datain.position( src );
          temp = datain.get();
          datain.position( src + group_size );
          temp += datain.get();
          temp /= 2;
          dataout.put( temp );
          src += element_size;
          dest++;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assert only
    } else if( width == 1 ) { // 1 column
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 ); // widthxheight can't be 1
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          datain.position( src );
          temp = datain.get();
          datain.position( src + ysize );
          temp += datain.get();
          temp /= 2;
          src += element_size;
          dest++;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == components * element_size * halfWidth * halfHeight );
  }
  
  public static void halveImage_ushort( int components, int width, int height,
                          ByteBuffer datain, ShortBuffer dataout, int element_size,
                          int ysize, int group_size, boolean myswap_bytes ) {
    int i, j, k, l;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    int temp = 0;
    // handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_ushort( components, width, height, datain, dataout, element_size,
                                ysize, group_size, myswap_bytes );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    // Piece of cake
    if( !myswap_bytes ) {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = ( 0x0000FFFF & datain.getShort() );
            datain.position( t + group_size );
            temp += ( 0x0000FFFF & datain.getShort() );
            datain.position( t + ysize );
            temp += ( 0x0000FFFF & datain.getShort() );
            datain.position( t + ysize + group_size );
            temp += ( 0x0000FFFF & datain.getShort() );
            dataout.put( (short)( ( temp + 2 ) / 4 ) );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    } else {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            datain.position( t + group_size );
            temp += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            datain.position( t + ysize );
            temp += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            datain.position( t + ysize + group_size );
            temp += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            dataout.put( (short)( ( temp + 2 ) / 4 ) );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    }
  }
  
  public static void halve1Dimage_ushort( int components, int width, int height,
                      ByteBuffer datain, ShortBuffer dataout, int element_size,
                      int ysize, int group_size, boolean myswap_bytes ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // widthxheight can't be 1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < halfHeight; kk++ ) {
          int[] ushort = new int[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            ushort[0] = ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            datain.position( src + group_size );
            ushort[1] = (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
          } else {
            datain.position( src );
            ushort[0] = (0x0000FFFF & datain.getShort() );
            datain.position( src + group_size );
            ushort[1] = (0x0000FFFF & datain.getShort() );
          }
          dataout.put( (short)( (ushort[0] + ushort[1]) / 2 ) );
          src += element_size;
          dest += 2;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) { // 1 column
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 ); // widthxheight can't be 1
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          int[] ushort = new int[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            ushort[0] = ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
            datain.position( src + ysize );
            ushort[0] = ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ) );
          } else {
            datain.position( src );
            ushort[0] = ( 0x0000FFFF & datain.getShort() );
            datain.position( src + ysize );
            ushort[1] = ( 0x0000FFFF & datain.getShort() );
          }
          dataout.put( (short)((ushort[0] + ushort[1]) / 2) );
          src += element_size;
          dest += 2;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == components * element_size * halfWidth * halfHeight );
  }
  
  public static void halveImage_short( int components, int width, int height,
                        ByteBuffer datain, ShortBuffer dataout, int element_size,
                        int ysize, int group_size, boolean myswap_bytes ) {
    int i, j, k, l;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    short temp = (short)0;
    // handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_short( components, width, height, datain, dataout, element_size,
                            ysize, group_size, myswap_bytes );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    // Piece of cake
    if( !myswap_bytes ) {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = datain.getShort();
            datain.position( t + group_size );
            temp += datain.getShort();
            datain.position( t + ysize );
            temp += datain.getShort();
            datain.position( t + ysize + group_size );
            temp += datain.getShort();
            temp += 2;
            temp /= 4;
            dataout.put( (short)temp );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    } else {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            short b;
            int buf;
            datain.position( t );
            temp = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            datain.position( t + group_size );
            temp += Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            datain.position( t + ysize );
            temp += Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            datain.position( t + ysize + group_size );
            temp += Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            temp += 2;
            temp /= 4;
            dataout.put( temp );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    }
  }
  
  public static void halve1Dimage_short( int components, int width, int height,
              ByteBuffer datain, ShortBuffer dataout, int element_size, int ysize,
              int group_size, boolean myswap_bytes ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // can't be 1x1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          short[] sshort = new short[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            sshort[0] = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            datain.position( src + group_size );
            sshort[1] = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
          } else {
            datain.position( src );
            sshort[0] = datain.getShort();
            datain.position( src + group_size );
            sshort[1] = datain.getShort();
          }
          dataout.put( (short)(( sshort[0] + sshort[1] ) / 2) );
          src += element_size;
          dest += 2;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) {
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 );
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          short[] sshort = new short[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            sshort[0] = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
            datain.position( src + ysize );
            sshort[1] = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
          } else {
            datain.position( src );
            sshort[0] = datain.getShort();
            datain.position( src + ysize );
            sshort[1] = datain.getShort();
          }
          dataout.put( (short)(( sshort[0] + sshort[1] ) / 2) );
          src += element_size;
          dest += 2;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == ( components * element_size * halfWidth * halfHeight ) );
  }
  
  public static void halveImage_uint( int components, int width, int height,
                          ByteBuffer datain, IntBuffer dataout, int element_size,
                          int ysize, int group_size, boolean myswap_bytes ) {
    int i, j, k, l;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    double temp = 0;
    
    // handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_uint( components, width, height, datain, dataout, element_size,
                                ysize, group_size, myswap_bytes );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    // Piece of cake
    if( !myswap_bytes ) {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = (0x000000007FFFFFFFL & datain.getInt() );
            datain.position( t + group_size );
            temp += (0x000000007FFFFFFFL & datain.getInt() );
            datain.position( t + ysize );
            temp += (0x000000007FFFFFFFL & datain.getInt() );
            datain.position( t + ysize + group_size );
            temp += (0x000000007FFFFFFFL & datain.getInt() );
            dataout.put( (int)( ( temp / 4 ) + 0.5 ) );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    } else {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            // need to cast to double to hold large unsigned ints
            double buf;
            datain.position( t );
            buf = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( t + group_size );
            buf += ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( t + ysize );
            buf += ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( t + ysize + group_size );
            buf += ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            temp /= 4;
            temp += 0.5;
            dataout.put( (int)temp );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    }
  }
  
  public static void halve1Dimage_uint( int components, int width, int height,
                      ByteBuffer datain, IntBuffer dataout, int element_size, int ysize,
                      int group_size, boolean myswap_bytes ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // widthxheight can't be 1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < halfHeight; kk++ ) {
          long[] uint = new long[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( src + group_size );
            uint[1] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
          } else {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & datain.getInt() );
            datain.position( src + group_size );
            uint[1] = (0x00000000FFFFFFFF & datain.getInt() );
          }
          dataout.put( (int)( ( uint[0] + uint[1] ) / 2.0 ) );
          src += element_size;
          dest += 4;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) { // 1 column
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 ); // widthxheight can't be 1
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          long[] uint = new long[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( src + group_size );
            uint[0] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
          } else {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & datain.getInt() );
            datain.position( src + ysize );
            uint[1] = ( 0x00000000FFFFFFFF & datain.getInt() );
          }
          dataout.put( (int)( ( uint[0] + uint[1] ) / 2.0 ) );
          src += element_size;
          dest += 4;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == components * element_size * halfWidth * halfHeight );
  }
  
  public static void halveImage_int( int components, int width, int height,
                        ByteBuffer datain, IntBuffer dataout, int element_size,
                        int ysize, int group_size, boolean myswap_bytes ) {
    int i, j, k, l;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    int temp = 0;
    
    // handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_int( components, width, height, datain, dataout, element_size,
                            ysize, group_size, myswap_bytes );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    // Piece of cake
    if( !myswap_bytes ) {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = datain.getInt();
            datain.position( t + group_size );
            temp += datain.getInt();
            datain.position( t + ysize );
            temp += datain.getInt();
            datain.position( t + ysize + group_size );
            temp += datain.getInt();
            temp = (int)( ( temp / 4.0f ) + 0.5f );
            dataout.put( temp );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    } else {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            long b;
            float buf;
            datain.position( t );
            b = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            buf = b;
            datain.position( t + group_size );
            b = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            buf += b;
            datain.position( t + ysize );
            b = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            buf += b;
            datain.position( t + ysize + group_size );
            b = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            buf += b;
            dataout.put( (int)( ( buf / 4.0f ) + 0.5f ) );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    }
  }
  
  public static void halve1Dimage_int( int components, int width, int height,
              ByteBuffer datain, IntBuffer dataout, int element_size, int ysize,
              int group_size, boolean myswap_bytes ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // can't be 1x1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          long[] uint = new long[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( src + group_size );
            uint[1] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
          } else {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & datain.getInt() );
            datain.position( src + group_size );
            uint[1] = ( 0x00000000FFFFFFFF & datain.getInt() );
          }
          dataout.put( (int)( ( (float)uint[0] + (float)uint[1] ) / 2.0f) );
          src += element_size;
          dest += 4;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) {
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 );
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          long[] uint = new long[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
            datain.position( src + ysize );
            uint[1] = ( 0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt() ) );
          } else {
            datain.position( src );
            uint[0] = ( 0x00000000FFFFFFFF & datain.getInt() );
            datain.position( src + ysize );
            uint[1] = ( 0x00000000FFFFFFFF & datain.getInt() );
          }
          dataout.put( (int)(( (float)uint[0] + (float)uint[1] ) / 2.0f) );
          src += element_size;
          dest += 4;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == ( components * element_size * halfWidth * halfHeight ) );
  }
  
  public static void halveImage_float( int components, int width, int height,
                    ByteBuffer datain, FloatBuffer dataout, int element_size,
                    int ysize, int group_size, boolean myswap_bytes ) {
    int i, j, k, l;
    int newwidth, newheight;
    int s = 0;
    int t = 0;
    float temp = 0.0f;
    // handle case where there is only 1 column/row
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) ); // can't be 1x1
      halve1Dimage_float( components, width, height, datain, dataout, element_size,
                                              ysize, group_size, myswap_bytes );
      return;
    }
    
    newwidth = width / 2;
    newheight = height / 2;
    
    // Piece of cake
    if( !myswap_bytes ) {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            datain.position( t );
            temp = datain.getFloat();
            datain.position( t + group_size );
            temp += datain.getFloat();
            datain.position( t + ysize );
            temp += datain.getFloat();
            datain.position( t + ysize + group_size );
            temp /= 4.0f;
            dataout.put( temp );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    } else {
      for( i = 0; i < newheight; i++ ) {
        for( j = 0; j < newwidth; j++ ) {
          for( k = 0; k < components; k++ ) {
            float buf;
            datain.position( t );
            buf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            datain.position( t + group_size );
            buf += Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            datain.position( t + ysize );
            buf += Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            datain.position( t + ysize + group_size );
            buf += Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            dataout.put( buf / 4.0f );
            t += element_size;
          }
          t += group_size;
        }
        t += ysize;
      }
    }
  }
  
  public static void halve1Dimage_float( int components, int width, int height,
              ByteBuffer datain, FloatBuffer dataout, int element_size, int ysize,
              int group_size, boolean myswap_bytes ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int dest = 0;
    int jj;
    
    assert( width == 1 || height == 1 ); // must be 1D
    assert( width != height ); // can't be square
    
    if( height == 1 ) { // 1 row
      assert( width != 1 ); // can't be 1x1
      halfHeight = 1;
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          float[] sfloat = new float[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            sfloat[0] = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            datain.position( src + group_size );
            sfloat[1] = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
          } else {
            datain.position( src );
            sfloat[0] = datain.getFloat();
            datain.position( src + group_size );
            sfloat[1] = datain.getFloat();
          }
          dataout.put( (sfloat[0] + sfloat[1]) / 2.0f );
          src += element_size;
          dest += 4;
        }
        src += group_size; // skip to next 2
      }
      int padBytes = ysize - ( width * group_size );
      src += padBytes; // for assertion only
    } else if( width == 1 ) {
      int padBytes = ysize - ( width * group_size );
      assert( height != 1 );
      halfWidth = 1;
      // one vertical column with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        int kk;
        for( kk = 0; kk < components; kk++ ) {
          float[] sfloat = new float[BOX2];
          if( myswap_bytes ) {
            datain.position( src );
            sfloat[0] = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
            datain.position( src + ysize );
            sfloat[1] = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
          } else {
            datain.position( src );
            sfloat[0] = datain.getFloat();
            datain.position( src + ysize );
            sfloat[1] = datain.getFloat();
          }
          dataout.put( ( sfloat[0] + sfloat[1] ) / 2.0f );
          src += element_size;
          dest += 4;
        }
        src += padBytes; // add pad bytes, if any, to get to end of row
        src += ysize;
      }
      assert( src == ysize * height );
    }
    assert( dest == ( components * element_size * halfWidth * halfHeight ) );
  }
  
  public static void halveImagePackedPixel( int components, Extract extract, int width, 
          int height, ByteBuffer datain, ByteBuffer dataout, 
          int pixelSizeInBytes, int rowSizeInBytes, boolean isSwap ) {
    if( width == 1 || height == 1 ) {
      assert( !( width == 1 && height == 1 ) );
      halve1DimagePackedPixel( components, extract, width, height, datain, dataout,
                          pixelSizeInBytes, rowSizeInBytes, isSwap );
      return;
    }
    int ii, jj;
    
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int padBytes = rowSizeInBytes - ( width * pixelSizeInBytes );
    int outIndex = 0;
    
    for( ii = 0; ii < halfHeight; ii++ ) {
      for( jj = 0; jj < halfWidth; jj++ ) {
        float totals[] = new float[4];
        float extractTotals[][] = new float[BOX4][4];
        int cc;
        
        datain.position( src );
        extract.extract( isSwap, datain, extractTotals[0] );
        datain.position( src + pixelSizeInBytes );
        extract.extract( isSwap, datain, extractTotals[1] );
        datain.position( src + rowSizeInBytes );
        extract.extract( isSwap, datain, extractTotals[2] );
        datain.position( src + rowSizeInBytes + pixelSizeInBytes );
        extract.extract( isSwap, datain, extractTotals[3] );
        for( cc = 0; cc < components; cc++ ) {
          int kk = 0;
          // grab 4 pixels to average
          totals[cc] = 0.0f;
          for( kk = 0; kk < BOX4; kk++ ) {
            totals[cc] += extractTotals[kk][cc];
          }
          totals[cc] /= BOX4;
        }
        extract.shove( totals, outIndex, dataout );
        outIndex++;
        src += pixelSizeInBytes + pixelSizeInBytes;
      }
      // skip past pad bytes, if any, to get to next row
      src += padBytes;
      src += rowSizeInBytes;
    }
    assert( src == rowSizeInBytes * height );
    assert( outIndex == halfWidth * halfHeight );
  }
  
  public static void halve1DimagePackedPixel( int components, Extract extract, int width,
              int height, ByteBuffer datain, ByteBuffer dataout,
              int pixelSizeInBytes, int rowSizeInBytes, boolean isSwap ) {
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int src = 0;
    int jj;
    
    assert( width == 1 || height == 1 );
    assert( width != height );
    
    if( height == 1 ) {
      int outIndex = 0;
      
      assert( width != 1 );
      halfHeight = 1;
      
      // one horizontal row with possible pad bytes
      
      for( jj = 0; jj < halfWidth; jj++ ) {
        float[] totals = new float[4];
        float[][] extractTotals = new float[BOX2][4];
        int cc;
        
        datain.position( src );
        extract.extract( isSwap, datain, extractTotals[0] );
        datain.position( src + pixelSizeInBytes );
        extract.extract( isSwap, datain, extractTotals[1] );
        for( cc = 0; cc < components; cc++ ) {
          int kk = 0;
          // grab 4 pixels to average
          totals[cc] = 0.0f;
          for( kk = 0; kk < BOX2; kk++ ) {
            totals[cc] += extractTotals[kk][cc];
          }
          totals[cc] /= BOX2;
        }
        extract.shove( totals, outIndex, dataout );
        outIndex++;
        // skip over to next group of 2
        src += pixelSizeInBytes + pixelSizeInBytes;
      }
      int padBytes = rowSizeInBytes - ( width * pixelSizeInBytes );
      src += padBytes;
      
      assert( src == rowSizeInBytes );
      assert( outIndex == halfWidth * halfHeight );
    } else if( width == 1 ) {
      int outIndex = 0;
      
      assert( height != 1 );
      halfWidth = 1;
      // one vertical volumn with possible pad bytes per row
      // average two at a time
      
      for( jj = 0; jj < halfHeight; jj++ ) {
        float[] totals = new float[4];
        float[][] extractTotals = new float[BOX2][4];
        int cc;
        // average two at a time, instead of four
        datain.position( src );
        extract.extract( isSwap, datain, extractTotals[0] );
        datain.position( src + rowSizeInBytes );
        extract.extract( isSwap, datain, extractTotals[1] );
        for( cc = 0; cc < components; cc++ ) {
          int kk = 0;
          // grab 4 pixels to average
          totals[cc] = 0.0f;
          for( kk = 0; kk < BOX2; kk++ ) {
            totals[cc] += extractTotals[kk][cc];
          }
          totals[cc] /= BOX2;
        }
        extract.shove( totals, outIndex, dataout );
        outIndex++;
        // skip over to next group of 2
        src += rowSizeInBytes + rowSizeInBytes;
      }
      assert( src == rowSizeInBytes );
      assert( outIndex == halfWidth * halfHeight );
    }
  }
  
  public static void halveImagePackedPixelSlice( int components, Extract extract,
          int width, int height, int depth, ByteBuffer dataIn,
          ByteBuffer dataOut, int pixelSizeInBytes, int rowSizeInBytes,
          int imageSizeInBytes, boolean isSwap ) {
    int ii, jj;
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int halfDepth = depth / 2;
    int src = 0;
    int padBytes = rowSizeInBytes - ( width * pixelSizeInBytes );
    int outIndex = 0;
    
    assert( (width == 1 || height == 1) && depth >= 2 );
    
    if( width == height ) {
      assert( width == 1 && height == 1 );
      assert( depth >= 2 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        float totals[] = new float[4];
        float extractTotals[][] = new float[BOX2][4];
        int cc;
        
        dataIn.position( src );
        extract.extract( isSwap, dataIn, extractTotals[0] );
        dataIn.position( src + imageSizeInBytes );
        extract.extract( isSwap, dataIn, extractTotals[1] );
        
        for( cc = 0; cc < components; cc++ ) {
          int kk;
          
          // average only 2 pixels since a column
          totals[cc]= 0.0f;
          for( kk = 0; kk < BOX2; kk++ ) {
            totals[cc] += extractTotals[kk][cc];
          }
          totals[cc] /= BOX2;
        } // for cc
        
        extract.shove( totals, outIndex, dataOut );
        outIndex++;
        // skip over to next group of 2
        src += imageSizeInBytes + imageSizeInBytes;
      } // for ii
    } else if( height == 1 ) {
      assert( width != 1 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        for( jj = 0; jj < halfWidth; jj++ ) {
          float totals[] = new float[4];
          float extractTotals[][] = new float[BOX4][4];
          int cc;
          
          dataIn.position( src );
          extract.extract( isSwap, dataIn, extractTotals[0] );
          dataIn.position( src + pixelSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[1] );
          dataIn.position( src + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[2] );
          dataIn.position( src + pixelSizeInBytes + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[3] );
          
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            
            // grab 4 pixels to average
            totals[cc] = 0.0f;
            for( kk = 0; kk < BOX4; kk++ ) {
              totals[cc]+= extractTotals[kk][cc];
            }
            totals[cc]/= (float)BOX4;
          }
          extract.shove( totals, outIndex, dataOut );
          outIndex++;
          // skip over to next horizontal square of 4
          src += imageSizeInBytes + imageSizeInBytes;
        }
      }
    } else if( width == 1 ) {
      assert( height != 1 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        for( jj = 0; jj < halfWidth; jj++ ) {
          float totals[] = new float[4];
          float extractTotals[][] = new float[BOX4][4];
          int cc;
          
          dataIn.position( src );
          extract.extract( isSwap, dataIn, extractTotals[0] );
          dataIn.position( src + rowSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[1] );
          dataIn.position( src + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[2] );
          dataIn.position( src + rowSizeInBytes + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[3] );
          
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            
            // grab 4 pixels to average
            totals[cc] = 0.0f;
            for( kk = 0; kk < BOX4; kk++ ) {
              totals[cc]+= extractTotals[kk][cc];
            }
            totals[cc]/= (float)BOX4;
          }
          extract.shove( totals, outIndex, dataOut );
          outIndex++;
          // skip over to next horizontal square of 4
          src += imageSizeInBytes + imageSizeInBytes;
        }
      }
    }
  }
  
  public static void halveImageSlice( int components, ExtractPrimitive extract, int width,
          int height, int depth, ByteBuffer dataIn, ByteBuffer dataOut,
          int elementSizeInBytes, int groupSizeInBytes, int rowSizeInBytes,
          int imageSizeInBytes, boolean isSwap ) {
    int ii, jj;
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int halfDepth = depth / 2;
    int src = 0;
    int padBytes = rowSizeInBytes - ( width * groupSizeInBytes );
    int outIndex = 0;
    
    assert( (width == 1 || height == 1) && depth >= 2 );
    
    if( width == height ) {
      assert( width == 1 && height == 1 );
      assert( depth >= 2 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        int cc;
        for( cc = 0; cc < components; cc++ ) {
          double[] totals = new double[4];
          double[][] extractTotals = new double[BOX2][4];
          int kk;
          
          dataIn.position( src );
          extractTotals[0][cc] = extract.extract( isSwap, dataIn );
          dataIn.position( src + imageSizeInBytes );
          extractTotals[1][cc] = extract.extract( isSwap, dataIn );
          
          // average 2 pixels since only a column
          totals[cc] = 0.0f;
          // totals[red] = extractTotals[0][red] + extractTotals[1][red];
          // totals[red] = red / 2;
          for( kk = 0; kk < BOX2; kk++ ) {
            totals[cc] += extractTotals[kk][cc];
          }
          totals[cc] /= (double)BOX2;
          
          extract.shove( totals[cc], outIndex, dataOut );
          outIndex++;
          src += elementSizeInBytes;
        } // for cc
        // skip over next group of 2
        src += rowSizeInBytes;
      } // for ii
      
      assert( src == rowSizeInBytes * height * depth );
      assert( outIndex == halfDepth * components );
    } else if( height == 1 ) {
      assert( width != 1 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        for( jj = 0; jj < halfWidth; jj++ ) {
          int cc;
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            double totals[] = new double[4];
            double extractTotals[][] = new double[BOX4][4];
            
            dataIn.position( src );
            extractTotals[0][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + groupSizeInBytes );
            extractTotals[1][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + imageSizeInBytes );
            extractTotals[2][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + imageSizeInBytes + groupSizeInBytes );
            extractTotals[3][cc] = extract.extract( isSwap, dataIn );
            
            // grab 4 pixels to average
            totals[cc] = 0.0f;
            // totals[red] = extractTotals[0][red] + extractTotals[1][red] +
            //               extractTotals[2][red] + extractTotals[3][red];
            // totals[red] /= (double)BOX4;
            for( kk = 0; kk < BOX4; kk++ ) {
              totals[cc] += extractTotals[kk][cc];
            }
            totals[cc] /= (double)BOX4;
            
            extract.shove( totals[cc], outIndex, dataOut );
            outIndex++;
            src += elementSizeInBytes;
          } // for cc
          // skip over to next horizontal square of 4
          src += elementSizeInBytes;
        } // for jj
        src += padBytes;
        src += rowSizeInBytes;
      } // for ii
      assert( src == rowSizeInBytes * height * depth );
      assert( outIndex == halfWidth * halfDepth * components );
    } else if( width == 1 ) {
      assert( height != 1 );
      
      for( ii = 0; ii < halfDepth; ii++ ) {
        for( jj = 0; jj < halfHeight; jj++ ) {
          int cc;
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            double totals[] = new double[4];
            double extractTotals[][] = new double[BOX4][4];
            
            dataIn.position( src );
            extractTotals[0][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + rowSizeInBytes );
            extractTotals[1][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + imageSizeInBytes );
            extractTotals[2][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + imageSizeInBytes + groupSizeInBytes );
            extractTotals[3][cc] = extract.extract( isSwap, dataIn );
            
            
            // grab 4 pixels to average
            totals[cc] = 0.0f;
            // totals[red] = extractTotals[0][red] + extractTotals[1][red] +
            //               extractTotals[2][red] + extractTotals[3][red];
            // totals[red] /= (double)BOX4;
            for( kk = 0; kk < BOX4; kk++ ) {
              totals[cc] += extractTotals[kk][cc];
            }
            totals[cc] /= (double)BOX4;
            
            extract.shove( totals[cc], outIndex, dataOut );
            outIndex++;
            src += elementSizeInBytes;
          } // for cc
          // skip over to next horizontal square of 4
          src += padBytes;
          src += rowSizeInBytes;
        } // for jj
        src += imageSizeInBytes;
      } // for ii
      assert( src == rowSizeInBytes * height * depth );
      assert( outIndex == halfWidth * halfDepth * components );
    }
  }
  
  public static void halveImage3D( int components, ExtractPrimitive extract,
          int width, int height, int depth, ByteBuffer dataIn, ByteBuffer dataOut,
          int elementSizeInBytes, int groupSizeInBytes, int rowSizeInBytes,
          int imageSizeInBytes, boolean isSwap ) {
    assert( depth > 1 );
    
    // horizontal/vertical/onecolumn slice viewed from top
    if( width == 1 || height == 1 ) {
      assert( 1 <= depth );
      
      halveImageSlice( components, extract, width, height, depth, dataIn, dataOut,
              elementSizeInBytes, groupSizeInBytes, rowSizeInBytes, imageSizeInBytes,
              isSwap );
      return;
    }
    
    int ii, jj, dd;
    
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int halfDepth = depth / 2;
    int src = 0;
    int padBytes = rowSizeInBytes - ( width * groupSizeInBytes );
    int outIndex = 0;
    
    for( dd = 0; dd < halfDepth; dd++ ) {
      for( ii = 0; ii < halfHeight; ii++ ) {
        for( jj = 0; jj < halfWidth; jj++ ) {
          int cc;
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            double totals[] = new double[4];
            double extractTotals[][] = new double[BOX8][4];
            
            dataIn.position( src );
            extractTotals[0][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + groupSizeInBytes );
            extractTotals[1][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + rowSizeInBytes );
            extractTotals[2][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + rowSizeInBytes + groupSizeInBytes );
            extractTotals[3][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + imageSizeInBytes );
            extractTotals[4][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + groupSizeInBytes + imageSizeInBytes );
            extractTotals[5][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + rowSizeInBytes + imageSizeInBytes );
            extractTotals[6][cc] = extract.extract( isSwap, dataIn );
            dataIn.position( src + rowSizeInBytes + imageSizeInBytes + groupSizeInBytes );
            extractTotals[7][cc] = extract.extract( isSwap, dataIn );
            
            totals[cc] = 0.0f;
            
            for( kk = 0; kk < BOX8; kk++ ) {
              totals[cc] += extractTotals[kk][cc];
            }
            totals[cc] /= (double)BOX8;
            
            extract.shove( totals[cc], outIndex, dataOut );
            outIndex++;
            
            src += elementSizeInBytes;
          } // for cc
          // skip over to next square of 4
          src += groupSizeInBytes;
        } // for jj
        // skip past pad bytes, if any, to get to next row
        src += padBytes;
        src += rowSizeInBytes;
      } // for ii
      src += imageSizeInBytes;
    } // for dd
    assert( src == rowSizeInBytes * height * depth );
    assert( outIndex == halfWidth * halfHeight * halfDepth * components );
  }
  
  public static void halveImagePackedPixel3D( int components, Extract extract,
          int width, int height, int depth, ByteBuffer dataIn, 
          ByteBuffer dataOut, int pixelSizeInBytes, int rowSizeInBytes,
          int imageSizeInBytes, boolean isSwap ) {
    if( depth == 1 ) {
      assert( 1 <= width && 1 <= height );
      
      halveImagePackedPixel( components, extract, width, height, dataIn, dataOut,
              pixelSizeInBytes, rowSizeInBytes, isSwap );
      return;
    } else if( width == 1 || height == 1 ) { // a horizontal or vertical slice viewed from top
      assert( 1 <= depth );
      
      halveImagePackedPixelSlice( components, extract, width, height, depth, dataIn,
              dataOut, pixelSizeInBytes, rowSizeInBytes, imageSizeInBytes, isSwap );
      return;
    }
    int ii, jj, dd;
    
    int halfWidth = width / 2;
    int halfHeight = height / 2;
    int halfDepth = depth / 2;
    int src = 0;
    int padBytes = rowSizeInBytes - ( width * pixelSizeInBytes );
    int outIndex = 0;
    
    for( dd = 0; dd < halfDepth; dd++ ) {
      for( ii = 0; ii < halfHeight; ii++ ) {
        for( jj = 0; jj < halfWidth; jj++ ) {
          float totals[] = new float[4]; // 4 is max components
          float extractTotals[][] = new float[BOX8][4];
          int cc;
          
          dataIn.position( src );
          extract.extract( isSwap, dataIn, extractTotals[0] );
          dataIn.position( src + pixelSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[1] );
          dataIn.position( src + rowSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[2] );
          dataIn.position( src + rowSizeInBytes + pixelSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[3] );
          dataIn.position( src + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[4] );
          dataIn.position( src + pixelSizeInBytes + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[5] );
          dataIn.position( src + rowSizeInBytes + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[6] );
          dataIn.position( src + rowSizeInBytes + pixelSizeInBytes + imageSizeInBytes );
          extract.extract( isSwap, dataIn, extractTotals[7] );
          
          for( cc = 0; cc < components; cc++ ) {
            int kk;
            // grab 8 pixels to average
            totals[cc] = 0.0f;
            for( kk = 0; kk < BOX8; kk++ ) {
              totals[cc] += extractTotals[kk][cc];
            }
            totals[cc] /= (float)BOX8;
          }
          extract.shove( totals, outIndex, dataOut );
          outIndex++;
          // skip over to next square of 4
          src += pixelSizeInBytes + pixelSizeInBytes;
        }
        // skip past pad bytes, if any, to get to next row
        src += padBytes;
        src += rowSizeInBytes;
      }
      src += imageSizeInBytes;
    }
    assert( src == rowSizeInBytes * height * depth );
    assert( outIndex == halfWidth * halfHeight * halfDepth );
  }
}
