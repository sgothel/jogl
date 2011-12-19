/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
package com.jogamp.opengl.util;

import com.jogamp.common.nio.Buffers;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLException;

import java.nio.*;

/**
 * Utility routines for dealing with direct buffers.
 * 
 * @author Kenneth Russel, et.al.
 */
public class GLBuffers extends Buffers {

    /**
     * @param glType shall be one of 
     *              GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT,
     *              GL_UNSIGNED_INT, GL_INT, GL_HALF_FLOAT, GL_FLOAT, GL_DOUBLE, 
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV,
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, 
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, 
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV,
     *              GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV, 
     *              GL_UNSIGNED_INT_10_10_10_2, GL_UNSIGNED_INT_2_10_10_10_REV
     *              GL_UNSIGNED_INT_24_8, GL_UNSIGNED_INT_10F_11F_11F_REV,
     *              GL_UNSIGNED_INT_5_9_9_9_REV, GL_FLOAT_32_UNSIGNED_INT_24_8_REV,         
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV (27)         
     * @return -1 if glType is unhandled, otherwise the actual value > 0 
     */
    public static final int sizeOfGLType(int glType) {
        switch (glType) { // 25
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return SIZEOF_BYTE;
                
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                return SIZEOF_SHORT;
                                
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2GL3.GL_UNSIGNED_INT_10_10_10_2:
            case GL2GL3.GL_UNSIGNED_INT_2_10_10_10_REV:                
            case GL2GL3.GL_UNSIGNED_INT_24_8:
            case GL2GL3.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return SIZEOF_INT;
                
            case GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return SIZEOF_LONG;
                
            case GL.GL_FLOAT:
                return SIZEOF_FLOAT;
                
            case GL2.GL_DOUBLE:
                return SIZEOF_DOUBLE;
        }
        return -1;
    }
    
    /**
     * @param glType shall be one of 
     *              GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT,
     *              GL_UNSIGNED_INT, GL_INT, GL_HALF_FLOAT, GL_FLOAT, GL_DOUBLE, 
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV,
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, 
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, 
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV,
     *              GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV, 
     *              GL_UNSIGNED_INT_10_10_10_2, GL_UNSIGNED_INT_2_10_10_10_REV
     *              GL_UNSIGNED_INT_24_8, GL_UNSIGNED_INT_10F_11F_11F_REV,
     *              GL_UNSIGNED_INT_5_9_9_9_REV, GL_FLOAT_32_UNSIGNED_INT_24_8_REV,         
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV (27)         
     * @return null if glType is unhandled, otherwise the new Buffer object 
     */
    public static final Buffer newDirectGLBuffer(int glType, int numElements) {
        switch (glType) {
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return newDirectByteBuffer(numElements);
                
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                return newDirectShortBuffer(numElements);
                
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2GL3.GL_UNSIGNED_INT_10_10_10_2:
            case GL2GL3.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL2GL3.GL_UNSIGNED_INT_24_8:
            case GL2GL3.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return newDirectIntBuffer(numElements);
                
            case GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return newDirectLongBuffer(numElements);
                
            case GL.GL_FLOAT:
                return newDirectFloatBuffer(numElements);
                
            case GL2.GL_DOUBLE:
                return newDirectDoubleBuffer(numElements);
        }
        return null;
    }

    /**
     * @param glType shall be one of 
     *              GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT,
     *              GL_UNSIGNED_INT, GL_INT, GL_HALF_FLOAT, GL_FLOAT, GL_DOUBLE, 
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV,
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, 
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, 
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV,
     *              GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV, 
     *              GL_UNSIGNED_INT_10_10_10_2, GL_UNSIGNED_INT_2_10_10_10_REV
     *              GL_UNSIGNED_INT_24_8, GL_UNSIGNED_INT_10F_11F_11F_REV,
     *              GL_UNSIGNED_INT_5_9_9_9_REV, GL_FLOAT_32_UNSIGNED_INT_24_8_REV,      
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV (27)         
     * @return null if glType is unhandled or parent is null or bufLen is 0, otherwise the new Buffer object 
     */
    public static final Buffer sliceGLBuffer(ByteBuffer parent, int bytePos, int byteLen, int glType) {
        if (parent == null || byteLen == 0) {
            return null;
        }
        parent.position(bytePos);
        parent.limit(bytePos + byteLen);

        switch (glType) {
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return parent.slice();
                
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
                return parent.asShortBuffer();
                
            case GL.GL_FIXED:
            case GL2GL3.GL_INT:
            case GL2ES2.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2GL3.GL_UNSIGNED_INT_10_10_10_2:
            case GL2GL3.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL2GL3.GL_UNSIGNED_INT_24_8:
            case GL2GL3.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return parent.asIntBuffer();
                
            case GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return parent.asLongBuffer();
                
            case GL.GL_FLOAT:
                return parent.asFloatBuffer();
                
            case GL2.GL_DOUBLE:
                return parent.asDoubleBuffer();
        }
        return null;
    }

    private static final int glGetInteger(GL gl, int pname, int[] tmp) {
        gl.glGetIntegerv(pname, tmp, 0);
        return tmp[0];
    }
    
    /** 
     * Returns the number of bytes required to read/write a memory buffer via OpenGL
     * using the current GL pixel storage state and the given parameters.
     * 
     * <p>This method is security critical, hence it throws an exception (fail-fast)
     * in case of an invalid alignment. In case we forgot to handle 
     * proper values, please contact the maintainer.</p> 
     *   
     * @param gl the current GL object
     * 
     * @param tmp a pass through integer array of size >= 1 used to store temp data (performance)
     * 
     * @param bytesPerElement bytes per element
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param pack true for read mode GPU -> CPU (pack), otherwise false for write mode CPU -> GPU (unpack)  
     * @return required minimum size of the buffer in bytes
     * @throws GLException if alignment is invalid. Please contact the maintainer if this is our bug.
     */
    public static final int sizeof(GL gl, int tmp[], 
                                   int bytesPerElement, int width, int height, int depth, 
                                   boolean pack) {
        int rowLength = 0;
        int skipRows = 0;
        int skipPixels = 0;
        int alignment = 1;
        int imageHeight = 0;
        int skipImages = 0;
  
        if (pack) {          
          alignment = glGetInteger(gl, GL.GL_PACK_ALIGNMENT, tmp);
          if(gl.isGL2GL3()) {
              rowLength = glGetInteger(gl, GL2GL3.GL_PACK_ROW_LENGTH, tmp);
              skipRows = glGetInteger(gl, GL2GL3.GL_PACK_SKIP_ROWS, tmp);              
              skipPixels = glGetInteger(gl, GL2GL3.GL_PACK_SKIP_PIXELS, tmp);
              if (depth > 1) {                  
                  imageHeight = glGetInteger(gl, GL2GL3.GL_PACK_IMAGE_HEIGHT, tmp);                  
                  skipImages = glGetInteger(gl, GL2GL3.GL_PACK_SKIP_IMAGES, tmp);
              }
          }
        } else {          
          alignment = glGetInteger(gl, GL.GL_UNPACK_ALIGNMENT, tmp);
          if(gl.isGL2GL3 ()) {              
              rowLength = glGetInteger(gl, GL2GL3.GL_UNPACK_ROW_LENGTH, tmp);              
              skipRows = glGetInteger(gl, GL2GL3.GL_UNPACK_SKIP_ROWS, tmp);              
              skipPixels = glGetInteger(gl, GL2GL3.GL_UNPACK_SKIP_PIXELS, tmp);
              if (depth > 1) {                  
                  imageHeight = glGetInteger(gl, GL2GL3.GL_UNPACK_IMAGE_HEIGHT, tmp);                  
                  skipImages = glGetInteger(gl, GL2GL3.GL_UNPACK_SKIP_IMAGES, tmp);
               }
          }
        }
          
        // Try to deal somewhat correctly with potentially invalid values
        width       = Math.max(0, width );
        height      = Math.max(1, height); // min 1D
        depth       = Math.max(1, depth ); // min 1 * imageSize
        skipRows    = Math.max(0, skipRows);
        skipPixels  = Math.max(0, skipPixels);
        alignment   = Math.max(1, alignment);
        skipImages  = Math.max(0, skipImages);
  
        imageHeight = ( imageHeight > 0 ) ? imageHeight : height;
        rowLength   = ( rowLength   > 0 ) ? rowLength   : width;
  
        int rowLengthInBytes = rowLength  * bytesPerElement;
        int skipBytes        = skipPixels * bytesPerElement;
        
        switch(alignment) {
            case 1:
                break;
            case 2:
            case 4:
            case 8: {
                    // x % 2n == x & (2n - 1)
                    int remainder = rowLengthInBytes & ( alignment - 1 );
                    if (remainder > 0) {
                        rowLengthInBytes += alignment - remainder;
                    }
                    remainder = skipBytes & ( alignment - 1 );
                    if (remainder > 0) {
                        skipBytes += alignment - remainder;
                    }
                } 
                break;
            default:
                throw new GLException("Invalid alignment "+alignment+", must be 2**n (1,2,4,8). Pls notify the maintainer in case this is our bug.");        
        }
  
        /**
         * skipImages, depth, skipPixels and skipRows are static offsets.
         *
         * skipImages and depth are in multiples of image size.
         *
         * skipBytes and rowLengthInBytes are aligned
         * 
         * rowLengthInBytes is the aligned byte offset 
         * from line n to line n+1 at the same x-axis position.
         */
        return
            skipBytes +                                                  // aligned skipPixels * bpp
          ( skipImages + depth  - 1 ) * imageHeight * rowLengthInBytes + // aligned whole images 
          ( skipRows   + height - 1 ) * rowLengthInBytes +               // aligned lines
            width                     * bytesPerElement;                 // last line        
    }
  
    /** 
     * Returns the number of bytes required to read/write a memory buffer via OpenGL
     * using the current GL pixel storage state and the given parameters.
     * 
     * <p>This method is security critical, hence it throws an exception (fail-fast)
     * in case either the format, type or alignment is unhandled. In case we forgot to handle 
     * proper values, please contact the maintainer.</p> 
     *   
     * @param gl the current GL object
     * 
     * @param tmp a pass through integer array of size >= 1 used to store temp data (performance)
     * 
     * @param format must be one of 
     *              GL_COLOR_INDEX, GL_STENCIL_INDEX, GL_DEPTH_COMPONENT, GL_DEPTH_STENCIL,
     *              GL_RED, GL_GREEN, GL_BLUE, GL_ALPHA, GL_LUMINANCE,
     *              GL_RG, GL_LUMINANCE_ALPHA,
     *              GL_RGB, GL_BGR, GL_RGBA, GL_BGRA, GL_ABGR_EXT,
     *              GL_RED_INTEGER, GL_GREEN_INTEGER, GL_BLUE_INTEGER,
     *              GL_RG_INTEGER, GL_RGB_INTEGER, GL_BGR_INTEGER, 
     *              GL_RGBA_INTEGER, GL_BGRA_INTEGER, GL_HILO_NV, GL_SIGNED_HILO_NV (26)
     *           
     * @param type must be one of 
     *              GL_BITMAP, 
     *              GL_UNSIGNED_BYTE, GL_BYTE, GL_UNSIGNED_SHORT, GL_SHORT,
     *              GL_UNSIGNED_INT, GL_INT, GL_HALF_FLOAT, GL_FLOAT, GL_DOUBLE, 
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV,
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, 
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, 
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV,
     *              GL_UNSIGNED_INT_8_8_8_8, GL_UNSIGNED_INT_8_8_8_8_REV, 
     *              GL_UNSIGNED_INT_10_10_10_2, GL_UNSIGNED_INT_2_10_10_10_REV
     *              GL_UNSIGNED_INT_24_8, GL_UNSIGNED_INT_10F_11F_11F_REV,
     *              GL_UNSIGNED_INT_5_9_9_9_REV, GL_FLOAT_32_UNSIGNED_INT_24_8_REV,
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV (28)         
     * 
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param pack true for read mode GPU -> CPU, otherwise false for write mode CPU -> GPU  
     * @return required minimum size of the buffer in bytes
     * @throws GLException if format, type or alignment is not handled. Please contact the maintainer if this is our bug.
     */
    public static final int sizeof(GL gl, int tmp[], 
                                   int format, int type, int width, int height, int depth,
                                   boolean pack) throws GLException {
        int elements = 0;
        int esize = 0;

        if (width < 0) return 0;
        if (height < 0) return 0;
        if (depth < 0) return 0;
        
        switch (format) /* 24 */ {
            case GL2.GL_COLOR_INDEX:
            case GL2GL3.GL_STENCIL_INDEX:
            case GL2GL3.GL_DEPTH_COMPONENT:
            case GL2GL3.GL_DEPTH_STENCIL:
            case GL2GL3.GL_RED:
            case GL2GL3.GL_RED_INTEGER:
            case GL2GL3.GL_GREEN:
            case GL2GL3.GL_GREEN_INTEGER:
            case GL2GL3.GL_BLUE:
            case GL2GL3.GL_BLUE_INTEGER:
            case GL.GL_ALPHA:
            case GL.GL_LUMINANCE:
              elements = 1;
              break;
            case GL.GL_LUMINANCE_ALPHA:
            case GL2GL3.GL_RG:
            case GL2GL3.GL_RG_INTEGER:
            case GL2.GL_HILO_NV:
            case GL2.GL_SIGNED_HILO_NV:
              elements = 2;
              break;
            case GL.GL_RGB:
            case GL2GL3.GL_RGB_INTEGER:
            case GL2GL3.GL_BGR:
            case GL2GL3.GL_BGR_INTEGER: 
              elements = 3;
              break;
            case GL.GL_RGBA:
            case GL2GL3.GL_RGBA_INTEGER:
            case GL2GL3.GL_BGRA:
            case GL2GL3.GL_BGRA_INTEGER:
            case GL2.GL_ABGR_EXT:
              elements = 4;
              break;
            /* FIXME ?? 
             case GL.GL_HILO_NV:
              elements = 2;
              break; */              
            default:
              throw new GLException("format 0x"+Integer.toHexString(format)+" not supported [yet], pls notify the maintainer in case this is our bug.");
        }
                
        switch (type) /* 26 */ {
            case GL2.GL_BITMAP:
              if (GL2.GL_COLOR_INDEX == format || GL2GL3.GL_STENCIL_INDEX == format) {
                return (depth * (height * ((width+7)/8)));
              }
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:                
              esize = 1;
              break;
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_HALF_FLOAT:
              esize = 2;
              break;
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL.GL_FLOAT:
              esize = 4;
              break;
            case GL2GL3.GL_DOUBLE:
              esize = 8;
              break;
              
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
              esize = 1;
              elements = 1;
              break;
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL2GL3.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
              esize = 2;
              elements = 1;
              break;
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
              esize = 2;
              elements = 2;
              break;                
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2GL3.GL_UNSIGNED_INT_10_10_10_2:
            case GL2GL3.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL2GL3.GL_UNSIGNED_INT_24_8:
            case GL2GL3.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2GL3.GL_UNSIGNED_INT_5_9_9_9_REV:
              esize = 4;
              elements = 1;
              break;              
            case GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
              esize = 8;
              elements = 1;
              break;              
                
            default:
              throw new GLException("type 0x"+Integer.toHexString(type)+"/"+"format 0x"+Integer.toHexString(format)+" not supported [yet], pls notify the maintainer in case this is our bug.");
        }
        
        return sizeof(gl, tmp, elements * esize, width, height, depth, pack);
    }
    
    public static final int getNextPowerOf2(int number) {
        if (((number-1) & number) == 0) {
          //ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
          return number;
        }
        int power = 0;
        while (number > 0) {
          number = number>>1;
          power++;
        }
        return (1<<power);
    }    
      
    //----------------------------------------------------------------------
    // Conversion routines
    //
    public final static float[] getFloatArray(double[] source) {
        int i = source.length;
        float[] dest = new float[i--];
        while (i >= 0) {
            dest[i] = (float) source[i];
            i--;
        }
        return dest;
    }
}
