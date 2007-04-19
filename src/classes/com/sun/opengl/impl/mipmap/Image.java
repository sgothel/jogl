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
public class Image {
  
  /** Creates a new instance of Image */
  public Image() {
  }
  
  public static short getShortFromByteArray( byte[] array, int index ) {
    short s;
    s = (short)(array[index] << 8 );
    s |= (short)(0x00FF & array[index+1]);
    return( s );
  }
  
  public static int getIntFromByteArray( byte[] array, int index ) {
    int i;
    i = ( array[index] << 24 ) & 0xFF000000;
    i |= ( array[index+1] << 16 ) & 0x00FF0000;
    i |= ( array[index+2] << 8 ) & 0x0000FF00;
    i |= ( array[index+3] ) & 0x000000FF;
    return( i );
  }
  
  public static float getFloatFromByteArray( byte[] array, int index ) {
    int i = getIntFromByteArray( array, index );
    return( Float.intBitsToFloat(i) );
  }
  
  /*
   *  Extract array from user's data applying all pixel store modes.
   *  The internal format used is an array of unsigned shorts.
   */
  public static void fill_image( PixelStorageModes psm, int width, int height,
                  int format, int type, boolean index_format, ByteBuffer userdata,
                  ShortBuffer newimage ) {
    int components;
    int element_size;
    int rowsize;
    int padding;
    int groups_per_line;
    int group_size;
    int elements_per_line;
    int start;
    int iter = 0;
    int iter2;
    int i, j, k;
    boolean myswap_bytes;
    
    // Create a Extract interface object
    Extract extract = null;
    switch( type ) {
      case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
        extract = new Extract332();
        break;
      case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
        extract = new Extract233rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
        extract = new Extract565();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
        extract = new Extract565rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
        extract = new Extract4444();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
        extract = new Extract4444rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
        extract = new Extract5551();
        break;
      case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
        extract = new Extract1555rev();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
        extract = new Extract8888();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
        extract = new Extract8888rev();
        break;
      case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
        extract = new Extract1010102();
        break;
      case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
        extract = new Extract2101010rev();
        break;
    }
    
    myswap_bytes = psm.getUnpackSwapBytes();
    components = Mipmap.elements_per_group( format, type );
    if( psm.getUnpackRowLength() > 0 ) {
      groups_per_line = psm.getUnpackRowLength();
    } else {
      groups_per_line = width;
    }
    
    // All formats except GL_BITMAP fall out trivially
    if( type == GL.GL_BITMAP ) {
      int bit_offset;
      int current_bit;
      
      rowsize = ( groups_per_line * components + 7 ) / 8;
      padding = ( rowsize % psm.getUnpackAlignment() );
      if( padding != 0 ) {
        rowsize += psm.getUnpackAlignment() - padding;
      }
      start = psm.getUnpackSkipRows() * rowsize + ( psm.getUnpackSkipPixels() * components / 8 );
      elements_per_line = width * components;
      iter2 = 0;
      for( i = 0; i < height; i++ ) {
        iter = start;
        userdata.position( iter ); // ****************************************
        bit_offset = (psm.getUnpackSkipPixels() * components) % 8;
        for( j = 0; j < elements_per_line; j++ ) {
          // retrieve bit
          if( psm.getUnpackLsbFirst() ) {
            userdata.position( iter );
            current_bit = ( userdata.get() & 0x000000FF ) & ( 1 << bit_offset );//userdata[iter] & ( 1 << bit_offset );
          } else {
            current_bit = ( userdata.get() & 0x000000FF ) & ( 1 << ( 7 - bit_offset ) );
          }
          if( current_bit != 0 ) {
            if( index_format ) {
              newimage.position( iter2 );
              newimage.put( (short)1 );
            } else {
              newimage.position( iter2 );
              newimage.put( (short)65535 );
            }
          } else {
            newimage.position( iter2 );
            newimage.put( (short)0 );
          }
          bit_offset++;
          if( bit_offset == 8 ) {
            bit_offset = 0;
            iter++;
          }
          iter2++;
        }
        start += rowsize;
      }
    } else {
      element_size = Mipmap.bytes_per_element( type );
      group_size = element_size * components;
      if( element_size == 1 ) {
        myswap_bytes = false;
      }
      
      rowsize = groups_per_line * group_size;
      padding = ( rowsize % psm.getUnpackAlignment() );
      if( padding != 0 ) {
        rowsize += psm.getUnpackAlignment() - padding;
      }
      start = psm.getUnpackSkipRows() * rowsize + psm.getUnpackSkipPixels() * group_size;
      elements_per_line = width * components;
      
      iter2 = 0;
      for( i = 0; i < height; i++ ) {
        iter = start;
        userdata.position( iter ); //***************************************
        for( j = 0; j < elements_per_line; j++ ) {
          Type_Widget widget = new Type_Widget();
          float[] extractComponents = new float[4];
          userdata.position( iter );
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
              extract.extract( false, userdata /*userdata[iter]*/, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
              extract.extract( false, userdata /*userdata[iter]*/, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_BYTE ):
              if( index_format ) {
                newimage.put( iter2++, (short)( 0x000000FF & userdata.get() ) );//userdata[iter];
              } else {
                newimage.put( iter2++, (short)( 0x000000FF & userdata.get()/*userdata[iter]*/ * 257 ) );
              }
              break;
            case( GL.GL_BYTE ):
              if( index_format ) {
                newimage.put( iter2++, userdata.get() ); //userdata[iter];
              } else {
                newimage.put( iter2++, (short)(userdata.get()/*userdata[iter]*/ * 516 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
              extract.extract( myswap_bytes, userdata/*userdata[iter]*/, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)(extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
              if( myswap_bytes ) {
                widget.setUB1( userdata.get() );
                widget.setUB0( userdata.get() );
              } else {
                widget.setUB0( userdata.get() );
                widget.setUB1( userdata.get() );
              }
              if( type == GL.GL_SHORT ) {
                if( index_format ) {
                  newimage.put( iter2++, widget.getS0() );
                } else {
                  newimage.put( iter2++, (short)(widget.getS0() * 2) );
                }
              } else {
                newimage.put( iter2++, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              extract.extract( myswap_bytes, userdata, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newimage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_INT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_FLOAT ):
              if( myswap_bytes ) {
                widget.setUB3( userdata.get() );
                widget.setUB2( userdata.get() );
                widget.setUB1( userdata.get() );
                widget.setUB0( userdata.get() );
              } else {
                widget.setUB0( userdata.get() );
                widget.setUB1( userdata.get() );
                widget.setUB2( userdata.get() );
                widget.setUB3( userdata.get() );
              }
              if( type == GL.GL_FLOAT ) {
                if( index_format ) {
                  newimage.put( iter2++, (short)widget.getF() );
                } else {
                  newimage.put( iter2++, (short)(widget.getF() * 65535 ) );
                }
              } else if( type == GL.GL_UNSIGNED_INT ) {
                if( index_format ) {
                  newimage.put( iter2++, (short)( widget.getUI() ) );
                } else {
                  newimage.put( iter2++, (short)( widget.getUI() >> 16 ) );
                }
              } else {
                if( index_format ) {
                  newimage.put( iter2++, (short)( widget.getI() ) );
                } else {
                  newimage.put( iter2++, (short)( widget.getI() >> 15 ) );
                }
              }
              break;
          }
          iter += element_size;
        }  // for j
        start += rowsize;
        // want iter pointing at start, not within, row for assertion purposes
        iter = start;
      } // for i
      
      // iterators should be one byte past end
      if( !Mipmap.isTypePackedPixel( type ) ) {
        assert( iter2 == ( width * height * components ) );
      } else {
        assert( iter2 == ( width * height * Mipmap.elements_per_group( format, 0 ) ) );
      }
      assert( iter == ( rowsize * height + psm.getUnpackSkipRows() * rowsize + psm.getUnpackSkipPixels() * group_size ) );
    }
  }
  
  /*
   *  Insert array into user's data applying all pixel store modes.
   *  Theinternal format is an array of unsigned shorts.
   *  empty_image() because it is the opposet of fill_image().
   */
  public static void empty_image( PixelStorageModes psm, int width, int height, 
                                  int format, int type, boolean index_format, 
                                  ShortBuffer oldimage, ByteBuffer userdata ) {
    
    int components;
    int element_size;
    int rowsize;
    int padding;
    int groups_per_line;
    int group_size;
    int elements_per_line;
    int start;
    int iter = 0;
    int iter2;
    int i, j, k;
    boolean myswap_bytes;
    
    // Create a Extract interface object
    Extract extract = null;
    switch( type ) {
      case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
        extract = new Extract332();
        break;
      case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
        extract = new Extract233rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
        extract = new Extract565();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
        extract = new Extract565rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
        extract = new Extract4444();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
        extract = new Extract4444rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
        extract = new Extract5551();
        break;
      case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
        extract = new Extract1555rev();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
        extract = new Extract8888();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
        extract = new Extract8888rev();
        break;
      case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
        extract = new Extract1010102();
        break;
      case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
        extract = new Extract2101010rev();
        break;
    }
    
    myswap_bytes = psm.getPackSwapBytes();
    components = Mipmap.elements_per_group( format, type );
    if( psm.getPackRowLength() > 0 ) {
      groups_per_line = psm.getPackRowLength();
    } else {
      groups_per_line = width;
    }
    
    // all formats except GL_BITMAP fall out trivially
    if( type == GL.GL_BITMAP ) {
      int bit_offset;
      int current_bit;
      
      rowsize = ( groups_per_line * components + 7 ) / 8;
      padding = ( rowsize % psm.getPackAlignment() );
      if( padding != 0 ) {
        rowsize += psm.getPackAlignment() - padding;
      }
      start = psm.getPackSkipRows() * rowsize + psm.getPackSkipPixels() * components / 8;
      elements_per_line = width * components;
      iter2 = 0;
      for( i = 0; i < height; i++ ) {
        iter = start;
        bit_offset = ( psm.getPackSkipPixels() * components ) % 8;
        for( j = 0; j < elements_per_line; j++ ) {
          if( index_format ) {
            current_bit = oldimage.get( iter2 ) & 1;
          } else {
            if( oldimage.get( iter2 ) < 0 ) { // must check for negative rather than 32767
              current_bit = 1;
            } else {
              current_bit = 0;
            }
          }
          
          if( current_bit != 0 ) {
            if( psm.getPackLsbFirst() ) {
              userdata.put( iter, (byte)( ( userdata.get( iter ) | ( 1 << bit_offset ) ) ) );
            } else {
              userdata.put( iter, (byte)( ( userdata.get( iter ) | ( 7 - bit_offset ) ) ) );
            }
          } else {
            if( psm.getPackLsbFirst() ) {
              //userdata[iter] &= ~( 1 << bit_offset );
              userdata.put( iter, (byte)( ( userdata.get( iter ) & ~( 1 << bit_offset ) ) ) );
            } else {
              //userdata[iter] &= ~( 1 << ( 7 - bit_offset ) );
              userdata.put( iter, (byte)( ( userdata.get( iter ) & ~( 7 - bit_offset ) ) ) );
            }
          }
          
          bit_offset++;
          if( bit_offset == 8 ) {
            bit_offset = 0;
            iter++;
          }
          iter2++;
        }
        start += rowsize;
      }
    } else {
      float shoveComponents[] = new float[4];
      
      element_size = Mipmap.bytes_per_element( type );
      group_size = element_size * components;
      if( element_size == 1 ) {
        myswap_bytes = false;
      }
      
      rowsize = groups_per_line * group_size;
      padding = ( rowsize % psm.getPackAlignment() );
      if( padding != 0 ) {
        rowsize += psm.getPackAlignment() - padding;
      }
      start = psm.getPackSkipRows() * rowsize + psm.getPackSkipPixels() * group_size;
      elements_per_line = width * components;
      
      iter2 = 0;
      for( i = 0; i < height; i++ ) {
        iter = start;
        for( j = 0; j < elements_per_line; j++ ) {
          Type_Widget widget = new Type_Widget();
          
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, userdata );
              break;
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldimage.get(iter2++) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, userdata );
              break;
            case( GL.GL_UNSIGNED_BYTE ):
              if( index_format ) {
                //userdata[iter] = (byte)oldimage[iter2++];
                userdata.put( iter, (byte)oldimage.get(iter2++) );
              } else {
                //userdata[iter] = (byte)( oldimage[iter2++] >> 8 );
                userdata.put( iter, (byte)( oldimage.get(iter2++) ) );
              }
              break;
            case( GL.GL_BYTE ):
              if( index_format ) {
                //userdata[iter] = (byte)oldimage[iter2++];
                userdata.put( iter, (byte)oldimage.get(iter2++) );
              } else {
                //userdata[iter] = (byte)( oldimage[iter2++] >> 9 );
                userdata.put( iter, (byte)( oldimage.get(iter2++) ) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldimage.get(iter2++) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1,widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldimage.get(iter2++) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get(iter2++) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter+1] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
              if( type == GL.GL_SHORT ) {
                if( index_format ) {
                  widget.setS0( oldimage.get( iter2++ ) );
                } else {
                  widget.setS0( (short)(oldimage.get( iter2++ ) >> 1) );
                }
              } else {
                widget.setUS0( oldimage.get( iter2++ ) );
              }
              if( myswap_bytes ) {
                //userdata[iter] = widget.getUB1();
                //userdata[iter+1] = widget.getUB0();
                userdata.put( iter, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB0() );
              } else {
                //userdata[iter] = widget.getUB0();
                //userdata[iter] = widget.getUB1();
                userdata.put( iter, widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter+3] = widget.getUB0();
                //userdata[iter+2] = widget.getUB1();
                //userdata[iter+1] = widget.getUB2();
                //userdata[iter  ] = widget.getUB3();
                userdata.put( iter + 3, widget.getUB0() );
                userdata.put( iter + 2, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB2() );
                userdata.put( iter    , widget.getUB3() );
              } else {
                userdata.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter+3] = widget.getUB0();
                //userdata[iter+2] = widget.getUB1();
                //userdata[iter+1] = widget.getUB2();
                //userdata[iter  ] = widget.getUB3();
                userdata.put( iter + 3, widget.getUB0() );
                userdata.put( iter + 2, widget.getUB1() );
                userdata.put( iter + 2, widget.getUB2() );
                userdata.put( iter    , widget.getUB3() );
              } else {
                userdata.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter+3] = widget.getUB0();
                //userdata[iter+2] = widget.getUB1();
                //userdata[iter+1] = widget.getUB2();
                //userdata[iter  ] = widget.getUB3();
                userdata.put( iter + 3, widget.getUB0() );
                userdata.put( iter + 2, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB2() );
                userdata.put( iter    , widget.getUB3() );
              } else {
                userdata.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldimage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswap_bytes ) {
                //userdata[iter+3] = widget.getUB0();
                //userdata[iter+2] = widget.getUB1();
                //userdata[iter+1] = widget.getUB2();
                //userdata[iter  ] = widget.getUB3();
                userdata.put( iter + 3, widget.getUB0() );
                userdata.put( iter + 2, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB2() );
                userdata.put( iter    , widget.getUB3() );
              } else {
                userdata.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_INT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_FLOAT ):
              if( type == GL.GL_FLOAT ) {
                if( index_format ) {
                  widget.setF( oldimage.get( iter2++ ) );
                } else {
                  widget.setF( oldimage.get( iter2++ ) / 65535.0f );
                }
              } else if( type == GL.GL_UNSIGNED_INT ) {
                if( index_format ) {
                  widget.setUI( oldimage.get( iter2++ ) );
                } else {
                  widget.setUI( oldimage.get( iter2++ ) * 65537 );
                }
              } else {
                if( index_format ) {
                  widget.setI( oldimage.get( iter2++ ) );
                } else {
                  widget.setI( (oldimage.get( iter2++ ) * 65537) / 2 );
                }
              }
              if( myswap_bytes ) {
                userdata.put( iter + 3, widget.getUB0() );
                userdata.put( iter + 2, widget.getUB1() );
                userdata.put( iter + 1, widget.getUB2() );
                userdata.put( iter    , widget.getUB3() );
              } else {
                userdata.put( iter    , widget.getUB0() );
                userdata.put( iter + 1, widget.getUB1() );
                userdata.put( iter + 2, widget.getUB2() );
                userdata.put( iter + 3, widget.getUB3() );
              }
              break;
          }
          iter += element_size;
        } // for j
        start += rowsize;
        // want iter pointing at start, not within, row for assertion purposes
        iter = start;
      } // for i
      // iterators should be one byte past end
      if( !Mipmap.isTypePackedPixel( type ) ) {
        assert( iter2 == width * height * components );
      } else {
        assert( iter2 == width * height * Mipmap.elements_per_group( format, 0 ) );
      }
      assert( iter == rowsize * height + psm.getPackSkipRows() * rowsize + psm.getPackSkipPixels() * group_size );
    }
  }
  
  public static void fillImage3D( PixelStorageModes psm, int width, int height,
          int depth, int format, int type, boolean indexFormat, ByteBuffer userImage,
          ShortBuffer newImage ) {
    boolean myswapBytes;
    int components;
    int groupsPerLine;
    int elementSize;
    int groupSize;
    int rowSize;
    int padding;
    int elementsPerLine;
    int rowsPerImage;
    int imageSize;
    int start, rowStart;
    int iter = 0;
    int iter2 = 0;
    int ww, hh, dd, k;
    Type_Widget widget = new Type_Widget();
    float extractComponents[] = new float[4];
    
    // Create a Extract interface object
    Extract extract = null;
    switch( type ) {
      case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
        extract = new Extract332();
        break;
      case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
        extract = new Extract233rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
        extract = new Extract565();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
        extract = new Extract565rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
        extract = new Extract4444();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
        extract = new Extract4444rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
        extract = new Extract5551();
        break;
      case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
        extract = new Extract1555rev();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
        extract = new Extract8888();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
        extract = new Extract8888rev();
        break;
      case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
        extract = new Extract1010102();
        break;
      case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
        extract = new Extract2101010rev();
        break;
    }
    
    myswapBytes = psm.getUnpackSwapBytes();
    components = Mipmap.elements_per_group( format, type );
    if( psm.getUnpackRowLength() > 0 ) {
      groupsPerLine = psm.getUnpackRowLength();
    } else {
      groupsPerLine = width;
    }
    elementSize = Mipmap.bytes_per_element( type );
    groupSize = elementSize * components;
    if( elementSize == 1 ) {
      myswapBytes = false;
    }
    
    // 3dstuff begin
    if( psm.getUnpackImageHeight() > 0 ) {
      rowsPerImage = psm.getUnpackImageHeight();
    } else {
      rowsPerImage = height;
    }
    // 3dstuff end
    
    rowSize = groupsPerLine * groupSize;
    padding = rowSize % psm.getUnpackAlignment();
    if( padding != 0 ) {
      rowSize += psm.getUnpackAlignment() - padding;
    }
    
    imageSize = rowsPerImage * rowSize; // 3dstuff
    
    start = psm.getUnpackSkipRows() * rowSize + 
            psm.getUnpackSkipPixels() * groupSize + 
            psm.getUnpackSkipImages() * imageSize;
    elementsPerLine = width * components;
    
    iter2 = 0;
    for( dd = 0; dd < depth; dd++ ) {
      rowStart = start;
      for( hh = 0; hh < height; hh++ ) {
        iter = rowStart;
        for( ww = 0; ww < elementsPerLine; ww++ ) {
          
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE ):
              if( indexFormat ) {
                newImage.put( iter2++, (short)(0x000000FF & userImage.get( iter ) ) );
              } else {
                newImage.put( iter2++, (short)((0x000000FF & userImage.get( iter ) ) * 257 ) );
              }
              break;
            case( GL.GL_BYTE ):
              if( indexFormat ) {
                newImage.put( iter2++, userImage.get( iter ) );
              } else {
                newImage.put( iter2++, (short)(userImage.get( iter ) * 516 ) );
              }
              break;
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
              userImage.position( iter );
              extract.extract( false, userImage, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
              userImage.position( iter );
              extract.extract( false, userImage, extractComponents );
              for( k = 0; k < 3; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)(extractComponents[k] * 65535) );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
              if( myswapBytes ) {
                widget.setUB0( userImage.get( iter + 1 ) );
                widget.setUB1( userImage.get( iter ) );
              } else {
                widget.setUB0( userImage.get( iter ) );
                widget.setUB1( userImage.get( iter + 1 ) );
              }
              if( type == GL.GL_SHORT ) {
                if( indexFormat ) {
                  newImage.put( iter2++, widget.getUS0() );
                } else {
                  newImage.put( iter2++, (short)(widget.getUS0() * 2) );
                }
              } else {
                newImage.put( iter2++, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
              userImage.position( iter );
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              extract.extract( myswapBytes, userImage, extractComponents );
              for( k = 0; k < 4; k++ ) {
                newImage.put( iter2++, (short)( extractComponents[k] * 65535 ) );
              }
              break;
            case( GL.GL_INT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_FLOAT ):
              if( myswapBytes ) {
                widget.setUB0( userImage.get( iter + 3 ) );
                widget.setUB1( userImage.get( iter + 2 ) );
                widget.setUB2( userImage.get( iter + 1 ) );
                widget.setUB3( userImage.get( iter     ) );
              } else {
                widget.setUB0( userImage.get( iter     ) );
                widget.setUB1( userImage.get( iter + 1 ) );
                widget.setUB2( userImage.get( iter + 2 ) );
                widget.setUB3( userImage.get( iter + 3 ) );
              }
              if( type == GL.GL_FLOAT ) {
                if( indexFormat ) {
                  newImage.put( iter2++, (short)widget.getF() );
                } else {
                  newImage.put( iter2++, (short)( widget.getF() * 65535.0f ) );
                }
              } else if( type == GL.GL_UNSIGNED_INT ) {
                if( indexFormat ) {
                  newImage.put( iter2++, (short)widget.getUI() );
                } else {
                  newImage.put( iter2++, (short)(widget.getUI() >> 16) );
                }
              } else {
                if( indexFormat ) {
                  newImage.put( iter2++, (short)widget.getI() );
                } else {
                  newImage.put( iter2++, (short)(widget.getI() >> 15) );
                }
              }
              break;
            default:
              assert( false );
          }
          iter += elementSize;
        } // for ww
        rowStart += rowSize;
        iter = rowStart; // for assert
      } // for hh
      start += imageSize;
    }// for dd
    
    // iterators should be one byte past end
    if( !Mipmap.isTypePackedPixel( type ) ) {
      assert( iter2 == width * height * depth * components );
    } else {
      assert( iter2 == width * height * depth * Mipmap.elements_per_group( format, 0 ) );
    }
    assert( iter == rowSize * height * depth + psm.getUnpackSkipRows() * rowSize + 
            psm.getUnpackSkipPixels() * groupSize +
            psm.getUnpackSkipImages() * imageSize );
  }
  
  public static void emptyImage3D( PixelStorageModes psm, int width, int height, int depth,
          int format, int type, boolean indexFormat, ShortBuffer oldImage, ByteBuffer userImage ) {
    boolean myswapBytes;
    int components;
    int groupsPerLine;
    int elementSize;
    int groupSize;
    int rowSize;
    int padding;
    int start, rowStart, iter;
    int elementsPerLine;
    int iter2;
    int ii, jj, dd, k;
    int rowsPerImage;
    int imageSize;
    Type_Widget widget = new Type_Widget();
    float[] shoveComponents = new float[4];
    
    // Create a Extract interface object
    Extract extract = null;
    switch( type ) {
      case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
        extract = new Extract332();
        break;
      case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
        extract = new Extract233rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
        extract = new Extract565();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
        extract = new Extract565rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
        extract = new Extract4444();
        break;
      case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
        extract = new Extract4444rev();
        break;
      case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
        extract = new Extract5551();
        break;
      case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
        extract = new Extract1555rev();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
        extract = new Extract8888();
        break;
      case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
        extract = new Extract8888rev();
        break;
      case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
        extract = new Extract1010102();
        break;
      case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
        extract = new Extract2101010rev();
        break;
    }
    
    iter = 0;
    
    myswapBytes = psm.getPackSwapBytes();
    components = Mipmap.elements_per_group( format, type );
    if( psm.getPackRowLength() > 0 ) {
      groupsPerLine = psm.getPackRowLength();
    } else {
      groupsPerLine = width;
    }
    
    elementSize = Mipmap.bytes_per_element( type );
    groupSize = elementSize * components;
    if( elementSize == 1 ) {
      myswapBytes = false;
    }
    
    // 3dstuff begin
    if( psm.getPackImageHeight() > 0 ) {
      rowsPerImage = psm.getPackImageHeight();
    } else {
      rowsPerImage = height;
    }
    
    // 3dstuff end
    
    rowSize = groupsPerLine * groupSize;
    padding = rowSize % psm.getPackAlignment();
    if( padding != 0 ) {
      rowSize += psm.getPackAlignment() - padding;
    }
    
    imageSize = rowsPerImage * rowSize;
    
    start = psm.getPackSkipRows() * rowSize +
            psm.getPackSkipPixels() * groupSize +
            psm.getPackSkipImages() * imageSize;
    elementsPerLine = width * components;
    
    iter2 = 0;
    for( dd = 0; dd < depth; dd++ ) {
      rowStart = start;
      
      for( ii = 0; ii < height; ii++ ) {
        iter = rowStart;
        
        for( jj = 0; jj < elementsPerLine; jj++ ) {
          
          switch( type ) {
            case( GL.GL_UNSIGNED_BYTE ):
              if( indexFormat ) {
                userImage.put( iter, (byte)(oldImage.get( iter2++ ) ) );
              } else {
                userImage.put( iter, (byte)(oldImage.get( iter2++ ) >> 8 ) );
              }
              break;
            case( GL.GL_BYTE ):
              if( indexFormat ) {
                userImage.put( iter, (byte)(oldImage.get(iter2++) ) );
              } else {
                userImage.put( iter, (byte)(oldImage.get(iter2++) >> 9) );
              }
              break;
            case( GL.GL_UNSIGNED_BYTE_3_3_2 ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, userImage );
              break;
            case( GL.GL_UNSIGNED_BYTE_2_3_3_REV ):
              for( k = 0; k < 3; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, userImage );
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.putShort( iter, widget.getUB1() );
                userImage.putShort( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_6_5_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_4_4_4_4_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_5_5_5_1 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT_1_5_5_5_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.putShort( iter, widget.getUS0() );
              }
              break;
            case( GL.GL_UNSIGNED_SHORT ):
            case( GL.GL_SHORT ):
              if( type == GL.GL_SHORT ) {
                if( indexFormat ) {
                  widget.setS0( (short)oldImage.get( iter2++ ) );
                } else {
                  widget.setS0( (short)(oldImage.get( iter2++ ) >> 1) );
                }
              } else {
                widget.setUS0( (short)oldImage.get( iter2++ ) );
              }
              if( myswapBytes ) {
                userImage.put( iter, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB0() );
              } else {
                userImage.put( iter, widget.getUB0() );
                userImage.put( iter + 1, widget.getUB1() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter + 3, widget.getUB0() );
                userImage.put( iter + 2, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB2() );
                userImage.put( iter    , widget.getUB3() );
              } else {
                userImage.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_8_8_8_8_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter + 3, widget.getUB0() );
                userImage.put( iter + 2, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB2() );
                userImage.put( iter    , widget.getUB3() );
              } else {
                userImage.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_10_10_10_2 ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter + 3, widget.getUB0() );
                userImage.put( iter + 2, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB2() );
                userImage.put( iter    ,widget.getUB3() );
              } else {
                userImage.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_UNSIGNED_INT_2_10_10_10_REV ):
              for( k = 0; k < 4; k++ ) {
                shoveComponents[k] = oldImage.get( iter2++ ) / 65535.0f;
              }
              extract.shove( shoveComponents, 0, widget.getBuffer() );
              if( myswapBytes ) {
                userImage.put( iter + 3, widget.getUB0() );
                userImage.put( iter + 2, widget.getUB2() );
                userImage.put( iter + 1, widget.getUB1() );
                userImage.put( iter    , widget.getUB0() );
              } else {
                userImage.putInt( iter, widget.getUI() );
              }
              break;
            case( GL.GL_INT ):
            case( GL.GL_UNSIGNED_INT ):
            case( GL.GL_FLOAT ):
              if( type == GL.GL_FLOAT ) {
                if( indexFormat ) {
                  widget.setF( oldImage.get( iter2++ ) );
                } else {
                  widget.setF( oldImage.get( iter2++ ) / 65535.0f );
                }
              } else if( type == GL.GL_UNSIGNED_INT ) {
                if( indexFormat ) {
                  widget.setUI( oldImage.get( iter2++ ) );
                } else {
                  widget.setUI( oldImage.get( iter2++ ) * 65537 );
                }
              } else {
                if( indexFormat ) {
                  widget.setI( oldImage.get( iter2++ ) );
                } else {
                  widget.setI( ( oldImage.get( iter2++ ) * 65535 ) / 2 );
                }
              }
              if( myswapBytes ) {
                userImage.put( iter + 3, widget.getUB0() );
                userImage.put( iter + 2, widget.getUB1() );
                userImage.put( iter + 1, widget.getUB2() );
                userImage.put( iter    , widget.getUB3() );
              } else {
                userImage.put( iter    , widget.getUB0() );
                userImage.put( iter + 1, widget.getUB1() );
                userImage.put( iter + 2, widget.getUB2() );
                userImage.put( iter + 3, widget.getUB3() );
              }
              break;
            default:
              assert( false );
          }
          
          iter += elementSize;
        } // for jj
        rowStart += rowSize;
      } // for ii
      start += imageSize;
    } // for dd
    
    if( !Mipmap.isTypePackedPixel( type ) ) {
      assert( iter2 == width * height * depth * components );
    } else {
      assert( iter2 == width * height * depth * Mipmap.elements_per_group( format, 0 ) );
    }
    assert( iter == rowSize * height * depth + 
                    psm.getUnpackSkipRows() * rowSize +
                    psm.getUnpackSkipPixels() * groupSize +
                    psm.getUnpackSkipImages() * imageSize );
  }
}
