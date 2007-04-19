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
import java.nio.*;

/**
 *
 * @author  Administrator
 */
public class ScaleInternal {
  
  public static final float UINT_MAX = (float)(0x00000000FFFFFFFF);
  
  public static void scale_internal( int components, int widthin, int heightin,
          ShortBuffer datain, int widthout, int heightout, ShortBuffer dataout ) {
    float x, lowx, highx, convx, halfconvx;
    float y, lowy, highy, convy, halfconvy;
    float xpercent, ypercent;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, yint, xint, xindex, yindex;
    int temp;
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage( components, widthin, heightin, datain, dataout );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    halfconvx = convx / 2;
    halfconvy = convy / 2;
    for( i = 0; i < heightout; i++ ) {
      y = convy * ( i + 0.5f );
      if( heightin > heightout ) {
        highy = y + halfconvy;
        lowy = y - halfconvy;
      } else {
        highy = y + 0.5f;
        lowy = y - 0.5f;
      }
      for( j = 0; j < widthout; j++ ) {
        x = convx * ( j + 0.5f );
        if( widthin > widthout ) {
          highx = x + halfconvx;
          lowx = x - halfconvx;
        } else {
          highx = x + 0.5f;
          lowx = x - 0.5f;
        }
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        area = 0.0f;
        
        y = lowy;
        yint = (int)Math.floor( y );
        while( y < highy ) {
          yindex = ( yint + heightin ) % heightin;
          if( highy < yint + 1 ) {
            ypercent = highy - y;
          } else {
            ypercent = yint + 1 - y;
          }
          
          x = lowx;
          xint = (int)Math.floor( x );
          
          while( x < highx ) {
            xindex = ( xint + widthin ) % widthin;
            if( highx < xint + 1 ) {
              xpercent = highx -x;
            } else {
              xpercent = xint + 1 - x;
            }
            
            percent = xpercent * ypercent;
            area += percent;
            temp = ( xindex + ( yindex * widthin) ) * components;
            for( k = 0; k < components; k++ ) {
              totals[k] += datain.get( temp + k ) * percent; 
            }
            
            xint++;
            x = xint;
          }
          yint++;
          y = yint;
        }
        
        temp = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          // totals[] should be rounded in the case of enlarging an RGB
          // ramp when the type is 332 or 4444
          dataout.put(  temp + k, (short)((totals[k] + 0.5f) / area) );
        }
      }
    }
  }
  
  public static void scale_internal_ubyte( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              ByteBuffer dataout, int element_size, int ysize, int group_size ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_ubyte( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;

        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += ( 0x000000FF & datain.get() ) * y_percent;
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += ( 0x000000FF & datain.get() ) * y_percent;
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              float f = 0.0f;
              datain.position( left );
              f = ( 0x000000FF & datain.get() ) * ( 1.0f - lowx_float );
              datain.position( right );
              f += ( 0x000000FF & datain.get() ) * highx_float;
              totals[k] += f;
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += ( 0x000000FF & datain.get() ) * x_percent;
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += ( 0x000000FF & datain.get() ) * y_percent;
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += ( 0x000000FF & datain.get() ) * percent;
          }
        }

        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += ( 0x000000FF & datain.get() );
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (byte)(totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_byte( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              ByteBuffer dataout, int element_size, int ysize,
                              int group_size ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_byte( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += datain.get() * y_percent;
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += datain.get() * y_percent;
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              float f = 0.0f;
              datain.position( left );
              f = datain.get() * ( 1 - lowx_float );
              datain.position( right );
              f += datain.get() * highx_float;
              totals[k] += f;
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += datain.get() * x_percent;
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += datain.get() * y_percent;
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            totals[k] += datain.get() * percent;
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              totals[k] += datain.get();
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (byte)(totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_ushort( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              ShortBuffer dataout, int element_size, int ysize, 
                              int group_size, boolean myswap_bytes ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_ushort( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size, myswap_bytes );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += ( 0x0000FFFF & ((int)Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ))) * percent;
            } else {
              totals[k] += ( 0x0000FFFF & datain.getShort() ) * percent;
            }
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += ( 0x0000FFFF & ((int)Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ))) * y_percent;
              } else {
                totals[k] += ( 0x0000FFFF & datain.getShort()) * y_percent;
              }
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += ( 0x0000FFFF & (Mipmap.GLU_SWAP_2_BYTES( datain.getShort() ))) * percent;
            } else {
              totals[k] += ( 0x0000FFFF & datain.getShort()) * percent;
            }
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * percent;
            } else {
              totals[k] += ( 0x0000FFFF & datain.getShort() ) * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * y_percent;
              } else {
                totals[k] += ( 0x0000FFFF & datain.getShort()) * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += ( 0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * percent;
            } else {
              totals[k] += ( 0x0000FFFF & datain.getShort()) * percent;
            }
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              if( myswap_bytes ) {
                datain.position( left );
                float f = (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES(datain.getShort())) * ( 1 - lowx_float );
                datain.position( right );
                f += ((0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES(datain.getShort())) * highx_float);
                totals[k] += f;
              } else {
                datain.position( left );
                float f = ((0x0000FFFF & datain.getShort()) * ( 1 - lowx_float ));
                datain.position( right );
                f += ((0x0000FFFF & datain.getShort()) * highx_float);
                totals[k] += f;
              }
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() )) * percent;
            } else {
              totals[k] += (0x0000FFFF & datain.getShort()) * percent;
            }
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * x_percent;
              } else {
                totals[k] += (0x0000FFFF & datain.getShort()) * x_percent;
              }
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort() )) * percent;
            } else {
              totals[k] += (0x0000FFFF & datain.getShort()) * percent;
            }
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * percent;
            } else {
              totals[k] += (0x0000FFFF & datain.getShort()) * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * y_percent;
              } else {
                totals[k] += (0x0000FFFF & datain.getShort()) * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * percent;
            } else {
              totals[k] += (0x0000FFFF & datain.getShort()) * percent;
            }
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()) ) * percent;
            } else {
              totals[k] += (0x0000FFFF & datain.getShort()) * percent;
            }
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x0000FFFF & Mipmap.GLU_SWAP_2_BYTES( datain.getShort()));
              } else {
                totals[k] += (0x0000FFFF & datain.getShort());
              }
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (short)(totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_short( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout,
                              ShortBuffer dataout, int element_size, int ysize, 
                              int group_size, boolean myswap_bytes ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    int swapbuf; // unsigned buffer
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_short( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size, myswap_bytes );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getShort() * y_percent;
              }
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getShort() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              if( myswap_bytes ) {
                datain.position( left );
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf * ( 1 - lowx_float );
                datain.position( right );
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf * highx_float;
              } else {
                datain.position( left );
                totals[k] += datain.getShort() * ( 1 - lowx_float );
                datain.position( right );
                totals[k] += datain.getShort() * highx_float;
              }
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort());
                totals[k] += swapbuf * x_percent;
              } else {
                totals[k] += datain.getShort() * x_percent;
              }
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getShort() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getShort() * percent;
            }
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_2_BYTES( datain.getShort() );
                totals[k] += swapbuf;
              } else {
                totals[k] += datain.getShort();
              }
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (short)(totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_uint( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              IntBuffer dataout, int element_size, int ysize, 
                              int group_size, boolean myswap_bytes ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_uint( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size, myswap_bytes );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * y_percent;
              } else {
                totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * y_percent;
              }
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * y_percent;
              } else {
                totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()) ) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              if( myswap_bytes ) {
                datain.position( left );
                totals[k] += ((0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES(datain.getInt())) * ( 1 - lowx_float ));
                datain.position( right );
                totals[k] += ((0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES(datain.getInt())) * highx_float);
              } else {
                datain.position( left );
                totals[k] += ((0x00000000FFFFFFFF & datain.getInt()) * ( 1 - lowx_float ));
                datain.position( right );
                totals[k] += ((0x00000000FFFFFFFF & datain.getInt()) * highx_float);
              }
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * x_percent;
              } else {
                totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * x_percent;
              }
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * y_percent;
              } else {
                totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            long tempInt0 = ( 0xFFFFFFFFL & datain.getInt( temp_index ) );
            datain.position( temp_index );
            long tempInt1 = ( 0xFFFFFFFFL & datain.getInt() );
            datain.position( temp_index );
            if( myswap_bytes ) {
              totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt())) * percent;
            } else {
              totals[k] += (0x00000000FFFFFFFF & datain.getInt()) * percent;
            }
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                totals[k] += (0x00000000FFFFFFFF & Mipmap.GLU_SWAP_4_BYTES( datain.getInt()));
              } else {
                totals[k] += (0x00000000FFFFFFFF & datain.getInt());
              }
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        float value = 0.0f;
        for( k = 0; k < components; k++ ) {
          value = totals[k] / area;
          dataout.position( outindex + k );
          if( value >= UINT_MAX ) {
            dataout.put( (int)value );
          } else {
            dataout.put( (int)(totals[k] / area) );
          }
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_int( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              IntBuffer dataout, int element_size, int ysize, 
                              int group_size, boolean myswap_bytes ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    long swapbuf; // unsigned buffer
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_int( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size, myswap_bytes );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getInt() * y_percent;
              }
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getInt() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              if( myswap_bytes ) {
                datain.position( left );
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * ( 1 - lowx_float );
                datain.position( right );
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * highx_float;
              } else {
                datain.position( left );
                totals[k] += (datain.getInt() * ( 1 - lowx_float ));
                datain.position( right );
                totals[k] += (datain.getInt() * highx_float);
              }
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * x_percent;
              } else {
                totals[k] += datain.getInt() * x_percent;
              }
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getInt() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getInt() * percent;
            }
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getInt() );
                totals[k] += swapbuf;
              } else {
                totals[k] += datain.getInt();
              }
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (int)(totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scale_internal_float( int components, int widthin, int heightin,
                              ByteBuffer datain, int widthout, int heightout, 
                              FloatBuffer dataout, int element_size, int ysize, 
                              int group_size, boolean myswap_bytes ) {
    float x, convx;
    float y, convy;
    float percent;
    // Max components in a format is 4, so...
    float[] totals = new float[4];
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outindex;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    float swapbuf; // unsigned buffer
    
    if( (widthin == (widthout * 2)) && (heightin == (heightout * 2)) ) {
      HalveImage.halveImage_float( components, widthin, heightin, datain, dataout, 
                        element_size, ysize, group_size, myswap_bytes );
      return;
    }
    convy = (float)heightin / heightout;
    convx = (float)widthin / widthout;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convy_float;
    
    for( i = 0; i < heightout; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightin)
        highy_int = heightin - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthout; j++ ) {
        
        // Ok, now apply box filter to box that goes from (lowx, lowy)
        // to (highx, highy) on input data into this pixel on output
        // data.
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // caulate the value for pixels in the 1st row
        xindex = lowx_int * group_size;
        if( ( highy_int > lowy_int ) && ( highx_int > lowx_int ) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * ysize;
          percent = y_percent * ( 1 - lowx_float );
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getFloat() * y_percent;
              }
            }
          }
          temp += group_size;
          right = temp;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          
          // calculate the value for pixels in the last row
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getFloat() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          
          // calculate the value for the pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += ysize;
            right += ysize;
            for( k = 0; k < components; k++, left += element_size, right += element_size ) {
              if( myswap_bytes ) {
                datain.position( left );
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * ( 1 - lowx_float );
                datain.position( right );
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * highx_float;
              } else {
                datain.position( left );
                totals[k] += (datain.getFloat() * ( 1 - lowx_float ));
                datain.position( right );
                totals[k] += (datain.getFloat() * highx_float);
              }
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float) * x_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += ysize;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * x_percent;
              } else {
                totals[k] += datain.getFloat() * x_percent;
              }
            }
          }
          percent = x_percent * highy_float;
          temp += ysize;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += group_size;
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf * y_percent;
              } else {
                totals[k] += datain.getFloat() * y_percent;
              }
            }
          }
          temp += group_size;
          percent = y_percent * highx_float;
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + (lowy_int * ysize);
          for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
            datain.position( temp_index );
            if( myswap_bytes ) {
              swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
              totals[k] += swapbuf * percent;
            } else {
              totals[k] += datain.getFloat() * percent;
            }
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + group_size + ( lowy_int + 1 ) * ysize;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            for( k = 0, temp_index = temp; k < components; k++, temp_index += element_size ) {
              datain.position( temp_index );
              if( myswap_bytes ) {
                swapbuf = Mipmap.GLU_SWAP_4_BYTES( datain.getFloat() );
                totals[k] += swapbuf;
              } else {
                totals[k] += datain.getFloat();
              }
            }
            temp += group_size;
          }
          temp0 += ysize;
        }
        
        outindex = ( j + ( i * widthout ) ) * components;
        for( k = 0; k < components; k++ ) {
          dataout.position( outindex + k );
          dataout.put( (totals[k] / area) );
        }
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthin - 1) {
          int delta = (highx_int - widthin + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
  }
  
  public static void scaleInternalPackedPixel( int components, Extract extract, 
              int widthIn, int heightIn, ByteBuffer dataIn, int widthOut,
              int heightOut, ByteBuffer dataOut, int pixelSizeInBytes,
              int rowSizeInBytes, boolean isSwap ) {
    float x, convx;
    float y, convy;
    float percent;
    
    // max components in a format is 4, so
    float[] totals = new float[4];
    float[] extractTotals = new float[4];
    float[] extractMoreTotals = new float[4];
    float[] shoveTotals = new float[4];
    
    float area;
    int i, j, k, xindex;
    
    int temp, temp0;
    int temp_index;
    int outIndex = 0;
    
    int lowx_int, highx_int, lowy_int, highy_int;
    float x_percent, y_percent;
    float lowx_float, highx_float, lowy_float, highy_float;
    float convy_float, convx_float;
    int convy_int, convx_int;
    int l, m;
    int left, right;
    
    if( widthIn == widthOut * 2 && heightIn == heightOut * 2 ) {
      HalveImage.halveImagePackedPixel( components, extract, widthIn, heightIn, dataIn, dataOut,
                              pixelSizeInBytes, rowSizeInBytes, isSwap );
      return;
    }
    convy = (float)heightIn / (float)heightOut;
    convx = (float)widthIn / (float)widthOut;
    convy_int = (int)Math.floor( convy );
    convy_float = convy - convy_int;
    convx_int = (int)Math.floor( convx );
    convx_float = convx - convx_int;
    
    area = convx * convy;
    
    lowy_int = 0;
    lowy_float = 0.0f;
    highy_int = convy_int;
    highy_float = convx_float;
    
    for( i = 0; i < heightOut; i++ ) {
      // Clamp here to be sure we don't read beyond input buffer.
      if (highy_int >= heightIn)
        highy_int = heightIn - 1;
      lowx_int = 0;
      lowx_float = 0.0f;
      highx_int = convx_int;
      highx_float = convx_float;
      
      for( j = 0; j < widthOut; j++ ) {
        // ok now apply box filter to box that goes from( lowx, lowy )
        // to ( highx, highy ) on input data into this pixel on output data
        totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
        
        // calculate that value for pixels in the 1st row
        xindex = lowx_int * pixelSizeInBytes;
        if( (highy_int > lowy_int) && (highx_int > lowx_int) ) {
          
          y_percent = 1 - lowy_float;
          temp = xindex + lowy_int * rowSizeInBytes;
          percent = y_percent * ( 1 - lowx_float );
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          left = temp;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += pixelSizeInBytes;
            dataIn.position( temp );
            extract.extract( isSwap, dataIn, extractTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += extractTotals[k] * y_percent;
            }
          }
          temp += pixelSizeInBytes;
          right = temp;
          percent = y_percent * highx_float;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          // calculate the value for pixels in the last row
          
          y_percent = highy_float;
          percent = y_percent * ( 1 - lowx_float );
          temp = xindex + highy_int * rowSizeInBytes;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += pixelSizeInBytes;
            dataIn.position( temp );
            extract.extract( isSwap, dataIn, extractTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += extractTotals[k] * y_percent;
            }
          }
          temp += pixelSizeInBytes;
          percent = y_percent * highx_float;
          dataIn.position( temp );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          
          // calculate the value for pixels in the 1st and last column
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            left += rowSizeInBytes;
            right += rowSizeInBytes;
            dataIn.position( left );
            extract.extract( isSwap, dataIn, extractTotals );
            dataIn.position( right );
            extract.extract( isSwap, dataIn, extractMoreTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += ( extractTotals[k] * ( 1 - lowx_float ) + extractMoreTotals[k] * highx_float );
            }
          }
        } else if( highy_int > lowy_int ) {
          x_percent = highx_float - lowx_float;
          percent = ( 1 - lowy_float ) * x_percent;
          temp = xindex + lowy_int * rowSizeInBytes;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          for( m = lowy_int + 1; m < highy_int; m++ ) {
            temp += rowSizeInBytes;
            dataIn.position( temp );
            extract.extract( isSwap, dataIn, extractTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += extractTotals[k] * x_percent;
            }
          }
          percent = x_percent * highy_float;
          temp += rowSizeInBytes;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
        } else if( highx_int > lowx_int ) {
          y_percent = highy_float - lowy_float;
          percent = ( 1 - lowx_float ) * y_percent;
          temp = xindex + lowy_int * rowSizeInBytes;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            temp += pixelSizeInBytes;
            dataIn.position( temp );
            extract.extract( isSwap, dataIn, extractTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += extractTotals[k] * y_percent;
            }
          }
          temp += pixelSizeInBytes;
          percent = y_percent * highx_float;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
        } else {
          percent = ( highy_float - lowy_float ) * ( highx_float - lowx_float );
          temp = xindex + lowy_int * rowSizeInBytes;
          dataIn.position( temp );
          extract.extract( isSwap, dataIn, extractTotals );
          for( k = 0; k < components; k++ ) {
            totals[k] += extractTotals[k] * percent;
          }
        }
        
        // this is for the pixels in the body
        temp0 = xindex + pixelSizeInBytes + ( lowy_int + 1 ) * rowSizeInBytes;
        for( m = lowy_int + 1; m < highy_int; m++ ) {
          temp = temp0;
          for( l = lowx_int + 1; l < highx_int; l++ ) {
            dataIn.position( temp );
            extract.extract( isSwap, dataIn, extractTotals );
            for( k = 0; k < components; k++ ) {
              totals[k] += extractTotals[k] * percent;
            }
            temp += pixelSizeInBytes;
          }
          temp0 += rowSizeInBytes;
        }
        
        outIndex = ( j + ( i * widthOut ) );
        for( k = 0; k < components; k++ ) {
          shoveTotals[k] = totals[k] / area;
        }
        extract.shove( shoveTotals, outIndex, dataOut );
        lowx_int = highx_int;
        lowx_float = highx_float;
        highx_int += convx_int;
        highx_float += convx_float;
        if( highx_float > 1.0f ) {
          highx_float -= 1.0f;
          highx_int++;
        }

        // Clamp to make sure we don't run off the right edge
        if (highx_int > widthIn - 1) {
          int delta = (highx_int - widthIn + 1);
          lowx_int -= delta;
          highx_int -= delta;
        }
      }
      lowy_int = highy_int;
      lowy_float = highy_float;
      highy_int += convy_int;
      highy_float += convy_float;
      if( highy_float > 1.0f ) {
        highy_float -= 1.0f;
        highy_int++;
      }
    }
    assert( outIndex == ( widthOut * heightOut - 1) );
  }
  
  public static void scaleInternal3D( int components, int widthIn, int heightIn,
          int depthIn, ShortBuffer dataIn, int widthOut, int heightOut,
          int depthOut, ShortBuffer dataOut ) {
    float x, lowx, highx, convx, halfconvx;
    float y, lowy, highy, convy, halfconvy;
    float z, lowz, highz, convz, halfconvz;
    float xpercent, ypercent, zpercent;
    float percent;
    // max compnents in a format is 4
    float[] totals = new float[4];
    float volume;
    int i, j, d, k, zint, yint, xint, xindex, yindex, zindex;
    int temp;
    
    lowy = highy = lowx = highx = 0.0f;
    
    convz = (float)depthIn / depthOut;
    convy = (float)heightIn / heightOut;
    convx = (float)widthIn / widthOut;
    halfconvz = convz / 2.0f;
    halfconvy = convy / 2.0f;
    halfconvx = convx / 2.0f;
    for( d = 0; d < depthOut; d++ ) {
      z = convz * ( d + 0.5f );
      if( depthIn > depthOut ) {
        highz = z + halfconvz;
        lowz = z - halfconvz;
      } else {
        highz = z + 0.5f;
        lowz = z - 0.5f;
      }
      for( i = 0; i < heightOut; i++ ) {
        y = convy * ( i + 0.5f );
        if( heightIn > heightOut ) {
          highz = y + halfconvy;
          lowz = y - halfconvy;
        } else {
          highz = y + 0.5f;
          lowz = y - 0.5f;
        }
        for( j = 0; j < widthOut; j++ ) {
          x = convx * ( j + 0.5f );
          if( depthIn > depthOut ) {
            highz = x + halfconvx;
            lowz = x - halfconvx;
          } else {
            highz = x + 0.5f;
            lowz = x - 0.5f;
          }
          
          // Ok, now apply box filter to box that goes from ( lowx, lowy, lowz )
          // to ( highx, highy, highz ) on input data into this pixel on output data
          
          totals[0] = totals[1] = totals[2] = totals[3] = 0.0f;
          volume = 0.0f;
          
          z = lowz;
          zint = (int)(Math.floor( z ) );
          while( z < highz ) {
            zindex = ( zint + depthIn ) % depthIn;
            if( highz < zint + 1 ) {
              zpercent = highz - z;
            } else {
              zpercent = zint + 1 - z;
            }
            
            y = lowy;
            yint = (int)(Math.floor( y ) );
            while( y < highy ) {
              yindex = ( yint + heightIn ) % heightIn;
              if( highy < yint + 1 ) {
                ypercent = highy - y;
              } else {
                ypercent = yint + 1 - y;
              }
              
              x = lowx;
              xint = (int)(Math.floor( x ) );
              
              while( x < highx ) {
                xindex = (xint + widthIn ) % widthIn;
                if( highx < xint + 1 ) {
                  xpercent = highx - x;
                } else {
                  xpercent = xint + 1 - x;
                }
                
                percent = xpercent * ypercent * zpercent;
                volume += percent;
                
                temp = (xindex + ( yindex *widthIn) + (zindex * widthIn *heightIn)) * components;
                for( k = 0; k < components; k++ ) {
                  assert( 0 <= (temp+k) && (temp+k) < (widthIn * heightIn * depthIn * components) );
                  totals[k] += dataIn.get( temp + k ) * percent;
                }
                xint++;
                x = xint;
              } // while x
              yint++;
              y = yint;
            } // while y
            zint++;
            z = zint;
          } // while z
          
          temp = ( j + ( i * widthOut ) + (d * widthOut * heightOut ) ) * components;
          for( k = 0; k < components; k++ ) {
            // totals should be rounded in the case of enlarging an rgb ramp when the type is 332 or 4444
            assert( 0 <= ( temp + k ) && ( temp + k ) < (widthOut * heightOut* depthOut * components) );
            dataOut.put( temp + k, (short)((totals[k] + 0.5f) / volume ) );
          }
        }
      }
    }
  }
  
  public static int gluScaleImage3D( GL gl, int format, int widthIn, int heightIn, 
          int depthIn, int typeIn, ByteBuffer dataIn, int widthOut, int heightOut, 
          int depthOut, int typeOut, ByteBuffer dataOut ) {
    int components;
    ShortBuffer beforeImage, afterImage;
    PixelStorageModes psm = new PixelStorageModes();
    
    if( widthIn == 0 || heightIn == 0 || depthIn == 0 ||
            widthOut == 0 || heightOut == 0 || depthOut == 0 ) {
      return( 0 );
    }
    
    if( widthIn < 0 || heightIn < 0 || depthIn < 0 ||
            widthOut < 0 || heightOut < 0 || depthOut < 0 ) {
      return( GLU.GLU_INVALID_VALUE );
    }
    
    if( !Mipmap.legalFormat(format) || !Mipmap.legalType(typeIn) || 
            !Mipmap.legalType(typeOut) || typeIn == GL.GL_BITMAP ||
            typeOut == GL.GL_BITMAP ) {
      return( GLU.GLU_INVALID_ENUM );
    }
    
    if( !Mipmap.isLegalFormatForPackedPixelType( format, typeIn ) ) {
      return( GLU.GLU_INVALID_OPERATION );
    }
    
    if( !Mipmap.isLegalFormatForPackedPixelType( format, typeOut ) ) {
      return( GLU.GLU_INVALID_OPERATION );
    }
    
    try {
    beforeImage = ByteBuffer.allocateDirect( Mipmap.imageSize3D( widthIn, 
            heightIn, depthIn, format, GL.GL_UNSIGNED_SHORT ) ).order( 
            ByteOrder.nativeOrder() ).asShortBuffer();
    afterImage = ByteBuffer.allocateDirect( Mipmap.imageSize3D( widthIn, 
            heightIn, depthIn, format, GL.GL_UNSIGNED_SHORT ) ).order( 
            ByteOrder.nativeOrder() ).asShortBuffer();
    } catch( OutOfMemoryError err ) {
      return( GLU.GLU_OUT_OF_MEMORY );
    }
    Mipmap.retrieveStoreModes3D( gl, psm );
    
    Image.fillImage3D( psm, widthIn, heightIn, depthIn, format, typeIn,
            Mipmap.is_index( format ), dataIn, beforeImage );
    components = Mipmap.elements_per_group( format, 0 );
    ScaleInternal.scaleInternal3D( components, widthIn, heightIn, depthIn,
            beforeImage, widthOut, heightOut, depthOut, afterImage );
    Image.emptyImage3D( psm, widthOut, heightOut, depthOut, format, typeOut,
            Mipmap.is_index( format ), afterImage, dataOut );
    
    return( 0 );
  }
}
