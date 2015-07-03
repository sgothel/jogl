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

import java.nio.Buffer;
import java.nio.ByteBuffer;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GL2ES2;
import com.jogamp.opengl.GL2ES3;
import com.jogamp.opengl.GL2GL3;
import com.jogamp.opengl.GLContext;
import com.jogamp.opengl.GLES2;
import com.jogamp.opengl.GLException;

import com.jogamp.common.nio.Buffers;

/**
 * Utility routines for dealing with direct buffers.
 *
 * @author Kenneth Russel, et.al.
 */
public class GLBuffers extends Buffers {

    /**
     * @param glType GL primitive type
     * @return false if one of GL primitive unsigned types, otherwise true
     *              GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_INT, <br/>
     *              GL_HILO16_NV <br/>
     */
    public static final boolean isSignedGLType(final int glType) {
        switch (glType) { // 29
            case GL.GL_UNSIGNED_BYTE:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_INT:
            case GL2.GL_HILO16_NV:
                return false;

        }
        return true;
    }

    /**
     * @param glType GL primitive type
     * @return false if one of GL primitive floating point types, otherwise true
     *              GL_FLOAT, <br/>
     *              GL_HALF_FLOAT, <br/>
     *              GL_HALF_FLOAT_OES, <br/>
     *              GL_DOUBLE <br/>
     */
    public static final boolean isGLTypeFixedPoint(final int glType) {
        switch(glType) {
            case GL.GL_FLOAT:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
            case GL2GL3.GL_DOUBLE:
                return false;

            default:
                return true;
        }
    }

    /**
     * @param glType shall be one of (31) <br/>
     *              GL_BYTE, GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, <br/>
     *              <br/>
     *              GL_SHORT, GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, <br/>
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_8_8_APPLE, GL_UNSIGNED_SHORT_8_8_REV_APPLE, <br/>
     *              GL.GL_HALF_FLOAT, GLES2.GL_HALF_FLOAT_OES: <br/>
     *              <br/>
     *              GL_FIXED, GL_INT <br/>
     *              GL_UNSIGNED_INT, GL_UNSIGNED_INT_8_8_8_8, <br/>
     *              GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, <br/>
     *              GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_24_8, <br/>
     *              GL_UNSIGNED_INT_10F_11F_11F_REV, GL_UNSIGNED_INT_5_9_9_9_REV <br/>
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV <br/>
     *              <br/>
     *              GL2GL3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV <br/>
     *              <br/>
     *              GL_FLOAT, GL_DOUBLE <br/>
     *
     * @return -1 if glType is unhandled, otherwise the actual value > 0
     */
    public static final int sizeOfGLType(final int glType) {
        switch (glType) { // 29
            // case GL2.GL_BITMAP:
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return SIZEOF_BYTE;

            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
            case GL2.GL_UNSIGNED_SHORT_8_8_APPLE:
            case GL2.GL_UNSIGNED_SHORT_8_8_REV_APPLE:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
                return SIZEOF_SHORT;

            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2ES2.GL_UNSIGNED_INT_10_10_10_2:
            case GL2ES2.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL.GL_UNSIGNED_INT_24_8:
            case GL.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2ES3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return SIZEOF_INT;

            case GL2ES3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return SIZEOF_LONG;

            case GL.GL_FLOAT:
                return SIZEOF_FLOAT;

            case GL2GL3.GL_DOUBLE:
                return SIZEOF_DOUBLE;
        }
        return -1;
    }

    /**
     * @param glType shall be one of (31) <br/>
     *              GL_BYTE, GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, <br/>
     *              <br/>
     *              GL_SHORT, GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, <br/>
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_8_8_APPLE, GL_UNSIGNED_SHORT_8_8_REV_APPLE, <br/>
     *              GL_HALF_FLOAT, GL_HALF_FLOAT_OES <br/>
     *              <br/>
     *              GL_FIXED, GL_INT <br/>
     *              GL_UNSIGNED_INT, GL_UNSIGNED_INT_8_8_8_8, <br/>
     *              GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, <br/>
     *              GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_24_8, <br/>
     *              GL_UNSIGNED_INT_10F_11F_11F_REV, GL_UNSIGNED_INT_5_9_9_9_REV <br/>
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV <br/>
     *              <br/>
     *              GL_FLOAT_32_UNSIGNED_INT_24_8_REV <br/>
     *              <br/>
     *              GL_FLOAT, GL_DOUBLE <br/>
     *
     * @return null if glType is unhandled, otherwise the new Buffer object
     */
    public static final Buffer newDirectGLBuffer(final int glType, final int numElements) {
        switch (glType) { // 29
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                return newDirectByteBuffer(numElements);

            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
            case GL2.GL_UNSIGNED_SHORT_8_8_APPLE:
            case GL2.GL_UNSIGNED_SHORT_8_8_REV_APPLE:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
                return newDirectShortBuffer(numElements);

            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2ES2.GL_UNSIGNED_INT_10_10_10_2:
            case GL2ES2.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL.GL_UNSIGNED_INT_24_8:
            case GL.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2ES3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                return newDirectIntBuffer(numElements);

            case GL2ES3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                return newDirectLongBuffer(numElements);

            case GL.GL_FLOAT:
                return newDirectFloatBuffer(numElements);

            case GL2GL3.GL_DOUBLE:
                return newDirectDoubleBuffer(numElements);
        }
        return null;
    }

    /**
     * @param glType shall be one of (31) <br/>
     *              GL_BYTE, GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, <br/>
     *              <br/>
     *              GL_SHORT, GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, <br/>
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_8_8_APPLE, GL_UNSIGNED_SHORT_8_8_REV_APPLE, <br/>
     *              GL_HALF_FLOAT, GL_HALF_FLOAT_OES <br/>
     *              <br/>
     *              GL_FIXED, GL_INT <br/>
     *              GL_UNSIGNED_INT, GL_UNSIGNED_INT_8_8_8_8, <br/>
     *              GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, <br/>
     *              GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_24_8, <br/>
     *              GL_UNSIGNED_INT_10F_11F_11F_REV, GL_UNSIGNED_INT_5_9_9_9_REV <br/>
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV <br/>
     *              <br/>
     *              GL_FLOAT_32_UNSIGNED_INT_24_8_REV <br/>
     *              <br/>
     *              GL_FLOAT, GL_DOUBLE <br/>
     * @return null if glType is unhandled or parent is null or bufLen is 0, otherwise the new Buffer object
     */
    public static final Buffer sliceGLBuffer(final ByteBuffer parent, final int bytePos, final int byteLen, final int glType) {
        if (parent == null || byteLen == 0) {
            return null;
        }
        final int parentPos = parent.position();
        final int parentLimit = parent.limit();

        parent.position(bytePos);
        parent.limit(bytePos + byteLen);
        Buffer res = null;

        switch (glType) { // 29
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
                res = parent.slice().order(parent.order()); // slice and duplicate may change byte order
                break;

            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
            case GL2.GL_UNSIGNED_SHORT_8_8_APPLE:
            case GL2.GL_UNSIGNED_SHORT_8_8_REV_APPLE:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
                res = parent.slice().order(parent.order()).asShortBuffer(); // slice and duplicate may change byte order
                break;

            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2ES2.GL_UNSIGNED_INT_10_10_10_2:
            case GL2ES2.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL.GL_UNSIGNED_INT_24_8:
            case GL.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2ES3.GL_UNSIGNED_INT_5_9_9_9_REV:
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
                res = parent.slice().order(parent.order()).asIntBuffer(); // slice and duplicate may change byte order
                break;

            case GL2ES3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
                res = parent.slice().order(parent.order()).asLongBuffer(); // slice and duplicate may change byte order
                break;

            case GL.GL_FLOAT:
                res = parent.slice().order(parent.order()).asFloatBuffer(); // slice and duplicate may change byte order
                break;

            case GL2GL3.GL_DOUBLE:
                res = parent.slice().order(parent.order()).asDoubleBuffer(); // slice and duplicate may change byte order
                break;
        }
        parent.position(parentPos).limit(parentLimit);
        return res;
    }

    private static final int glGetInteger(final GL gl, final int pname, final int[] tmp) {
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
     * @param bytesPerPixel bytes per pixel, i.e. via {@link #bytesPerPixel(int, int)}.
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param pack true for read mode GPU -> CPU (pack), otherwise false for write mode CPU -> GPU (unpack)
     * @return required minimum size of the buffer in bytes
     * @throws GLException if alignment is invalid. Please contact the maintainer if this is our bug.
     */
    public static final int sizeof(final GL gl, final int tmp[],
                                   final int bytesPerPixel, int width, int height, int depth,
                                   final boolean pack) {
        int rowLength = 0;
        int skipRows = 0;
        int skipPixels = 0;
        int alignment = 1;
        int imageHeight = 0;
        int skipImages = 0;

        if (pack) {
          alignment = glGetInteger(gl, GL.GL_PACK_ALIGNMENT, tmp);                   // es2, es3, gl3
          if( gl.isGL2ES3() ) {
              rowLength = glGetInteger(gl, GL2ES3.GL_PACK_ROW_LENGTH, tmp);          // es3, gl3
              skipRows = glGetInteger(gl, GL2ES3.GL_PACK_SKIP_ROWS, tmp);            // es3, gl3
              skipPixels = glGetInteger(gl, GL2ES3.GL_PACK_SKIP_PIXELS, tmp);        // es3, gl3
              if (depth > 1 && gl.isGL2GL3() && gl.getContext().getGLVersionNumber().compareTo(GLContext.Version1_2) >= 0 ) {
                  imageHeight = glGetInteger(gl, GL2GL3.GL_PACK_IMAGE_HEIGHT, tmp);  // gl3, GL_VERSION_1_2
                  skipImages = glGetInteger(gl, GL2GL3.GL_PACK_SKIP_IMAGES, tmp);    // gl3, GL_VERSION_1_2
              }
          }
        } else {
          alignment = glGetInteger(gl, GL.GL_UNPACK_ALIGNMENT, tmp);                 // es2, es3, gl3
          if( gl.isGL2ES3() ) {
              rowLength = glGetInteger(gl, GL2ES2.GL_UNPACK_ROW_LENGTH, tmp);        // es3, gl3
              skipRows = glGetInteger(gl, GL2ES2.GL_UNPACK_SKIP_ROWS, tmp);          // es3, gl3
              skipPixels = glGetInteger(gl, GL2ES2.GL_UNPACK_SKIP_PIXELS, tmp);      // es3, gl3
              if( depth > 1 &&
                  ( gl.isGL3ES3() ||
                    ( gl.isGL2GL3() && gl.getContext().getGLVersionNumber().compareTo(GLContext.Version1_2) >= 0 )
                  )
                ) {
                  imageHeight = glGetInteger(gl, GL2ES3.GL_UNPACK_IMAGE_HEIGHT, tmp);// es3, gl3, GL_VERSION_1_2
                  skipImages = glGetInteger(gl, GL2ES3.GL_UNPACK_SKIP_IMAGES, tmp);  // es3, gl3, GL_VERSION_1_2
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

        int rowLengthInBytes = rowLength  * bytesPerPixel;
        int skipBytes        = skipPixels * bytesPerPixel;

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
            width                     * bytesPerPixel;                   // last line
    }

    /**
     * Returns the number of bytes required to read/write a memory buffer via OpenGL
     * using the current GL pixel storage state and the given parameters.
     *
     * <p>This method is security critical, hence it throws an exception (fail-fast)
     * in case either the format, type or alignment is unhandled. In case we forgot to handle
     * proper values, please contact the maintainer.</p>
     *
     * <p> See {@link #bytesPerPixel(int, int)}. </p>
     *
     * @param gl the current GL object
     *
     * @param tmp a pass through integer array of size >= 1 used to store temp data (performance)
     *
     * @param format must be one of (27) <br/>
     *              GL_COLOR_INDEX GL_STENCIL_INDEX <br/>
     *              GL_DEPTH_COMPONENT GL_DEPTH_STENCIL <br/>
     *              GL_RED GL_RED_INTEGER <br/>
     *              GL_GREEN GL_GREEN_INTEGER <br/>
     *              GL_BLUE GL_BLUE_INTEGER <br/>
     *              GL_ALPHA GL_LUMINANCE (12) <br/>
     *              <br/>
     *              GL_LUMINANCE_ALPHA GL_RG <br/>
     *              GL_RG_INTEGER GL_HILO_NV <br/>
     *              GL_SIGNED_HILO_NV (5) <br/>
     *              <br/>
     *              GL_YCBCR_422_APPLE <br/>
     *              <br/>
     *              GL_RGB GL_RGB_INTEGER <br/>
     *              GL_BGR GL_BGR_INTEGER (4)<br/>
     *              <br/>
     *              GL_RGBA GL_RGBA_INTEGER <br/>
     *              GL_BGRA GL_BGRA_INTEGER <br/>
     *              GL_ABGR_EXT (5)<br/>
     *
     * @param type must be one of (32) <br/>
     *              GL_BITMAP, <br/>
     *              GL_BYTE, GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, <br/>
     *              <br/>
     *              GL_SHORT, GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, <br/>
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_8_8_APPLE, GL_UNSIGNED_SHORT_8_8_REV_APPLE, <br/>
     *              GL_HALF_FLOAT, GL_HALF_FLOAT_OES <br/>
     *              <br/>
     *              GL_FIXED, GL_INT <br/>
     *              GL_UNSIGNED_INT, GL_UNSIGNED_INT_8_8_8_8, <br/>
     *              GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, <br/>
     *              GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_24_8, <br/>
     *              GL_UNSIGNED_INT_10F_11F_11F_REV, GL_UNSIGNED_INT_5_9_9_9_REV <br/>
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV <br/>
     *              <br/>
     *              GL_FLOAT_32_UNSIGNED_INT_24_8_REV <br/>
     *              <br/>
     *              GL_FLOAT, GL_DOUBLE <br/>
     *
     * @param width in pixels
     * @param height in pixels
     * @param depth in pixels
     * @param pack true for read mode GPU -> CPU, otherwise false for write mode CPU -> GPU
     * @return required minimum size of the buffer in bytes
     * @throws GLException if format, type or alignment is not handled. Please contact the maintainer if this is our bug.
     */
    public static final int sizeof(final GL gl, final int tmp[],
                                   final int format, final int type, final int width, final int height, final int depth,
                                   final boolean pack) throws GLException {
        if (width < 0) return 0;
        if (height < 0) return 0;
        if (depth < 0) return 0;

        final int bytesPerPixel = bytesPerPixel(format, type);
        return sizeof(gl, tmp, bytesPerPixel, width, height, depth, pack);
    }

    /**
     * Returns the number of bytes required for one pixel with the the given OpenGL format and type.
     *
     * <p>This method is security critical, hence it throws an exception (fail-fast)
     * in case either the format, type or alignment is unhandled. In case we forgot to handle
     * proper values, please contact the maintainer.</p>
     *
     * <p> See {@link #componentCount(int)}. </p>
     *
     * @param format must be one of (27) <br/>
     *              GL_COLOR_INDEX GL_STENCIL_INDEX <br/>
     *              GL_DEPTH_COMPONENT GL_DEPTH_STENCIL <br/>
     *              GL_RED GL_RED_INTEGER <br/>
     *              GL_GREEN GL_GREEN_INTEGER <br/>
     *              GL_BLUE GL_BLUE_INTEGER <br/>
     *              GL_ALPHA GL_LUMINANCE (12) <br/>
     *              <br/>
     *              GL_LUMINANCE_ALPHA GL_RG <br/>
     *              GL_RG_INTEGER GL_HILO_NV <br/>
     *              GL_SIGNED_HILO_NV (5) <br/>
     *              <br/>
     *              GL_YCBCR_422_APPLE <br/>
     *              <br/>
     *              GL_RGB GL_RGB_INTEGER <br/>
     *              GL_BGR GL_BGR_INTEGER (4)<br/>
     *              <br/>
     *              GL_RGBA GL_RGBA_INTEGER <br/>
     *              GL_BGRA GL_BGRA_INTEGER <br/>
     *              GL_ABGR_EXT (5)<br/>
     *
     * @param type must be one of (32) <br/>
     *              GL_BITMAP, <br/>
     *              GL_BYTE, GL_UNSIGNED_BYTE, <br/>
     *              GL_UNSIGNED_BYTE_3_3_2, GL_UNSIGNED_BYTE_2_3_3_REV, <br/>
     *              <br/>
     *              GL_SHORT, GL_UNSIGNED_SHORT, <br/>
     *              GL_UNSIGNED_SHORT_5_6_5, GL_UNSIGNED_SHORT_5_6_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_4_4_4_4, GL_UNSIGNED_SHORT_4_4_4_4_REV, <br/>
     *              GL_UNSIGNED_SHORT_5_5_5_1, GL_UNSIGNED_SHORT_1_5_5_5_REV, <br/>
     *              GL_UNSIGNED_SHORT_8_8_APPLE, GL_UNSIGNED_SHORT_8_8_REV_APPLE, <br/>
     *              GL_HALF_FLOAT, GL_HALF_FLOAT_OES <br/>
     *              <br/>
     *              GL_FIXED, GL_INT <br/>
     *              GL_UNSIGNED_INT, GL_UNSIGNED_INT_8_8_8_8, <br/>
     *              GL_UNSIGNED_INT_8_8_8_8_REV, GL_UNSIGNED_INT_10_10_10_2, <br/>
     *              GL_UNSIGNED_INT_2_10_10_10_REV, GL_UNSIGNED_INT_24_8, <br/>
     *              GL_UNSIGNED_INT_10F_11F_11F_REV, GL_UNSIGNED_INT_5_9_9_9_REV <br/>
     *              GL_HILO16_NV, GL_SIGNED_HILO16_NV <br/>
     *              <br/>
     *              GL_FLOAT_32_UNSIGNED_INT_24_8_REV <br/>
     *              <br/>
     *              GL_FLOAT, GL_DOUBLE <br/>
     *
     * @return required size of one pixel in bytes
     * @throws GLException if format or type alignment is not handled. Please contact the maintainer if this is our bug.
     */
    public static final int bytesPerPixel(final int format, final int type) throws GLException {
        int compSize = 0;

        int compCount = componentCount(format);

        switch (type) /* 30 */ {
            case GL2.GL_BITMAP:
              if (GL2.GL_COLOR_INDEX == format || GL2ES2.GL_STENCIL_INDEX == format) {
                  compSize = 1;
              } else {
                  throw new GLException("BITMAP type only supported for format COLOR_INDEX and STENCIL_INDEX, not 0x"+Integer.toHexString(format));
              }
              break;
            case GL.GL_BYTE:
            case GL.GL_UNSIGNED_BYTE:
              compSize = 1;
              break;
            case GL.GL_SHORT:
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_HALF_FLOAT:
            case GLES2.GL_HALF_FLOAT_OES:
              compSize = 2;
              break;
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL.GL_UNSIGNED_INT:
            case GL.GL_FLOAT:
              compSize = 4;
              break;
            case GL2GL3.GL_DOUBLE:
              compSize = 8;
              break;

            case GL2GL3.GL_UNSIGNED_BYTE_3_3_2:
            case GL2GL3.GL_UNSIGNED_BYTE_2_3_3_REV:
              compSize = 1;
              compCount = 1;
              break;
            case GL.GL_UNSIGNED_SHORT_5_6_5:
            case GL2GL3.GL_UNSIGNED_SHORT_5_6_5_REV:
            case GL.GL_UNSIGNED_SHORT_4_4_4_4:
            case GL2GL3.GL_UNSIGNED_SHORT_4_4_4_4_REV:
            case GL.GL_UNSIGNED_SHORT_5_5_5_1:
            case GL2GL3.GL_UNSIGNED_SHORT_1_5_5_5_REV:
            case GL2.GL_UNSIGNED_SHORT_8_8_APPLE:
            case GL2.GL_UNSIGNED_SHORT_8_8_REV_APPLE:
              compSize = 2;
              compCount = 1;
              break;
            case GL2.GL_HILO16_NV:
            case GL2.GL_SIGNED_HILO16_NV:
              compSize = 2;
              compCount = 2;
              break;
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8:
            case GL2GL3.GL_UNSIGNED_INT_8_8_8_8_REV:
            case GL2ES2.GL_UNSIGNED_INT_10_10_10_2:
            case GL2ES2.GL_UNSIGNED_INT_2_10_10_10_REV:
            case GL.GL_UNSIGNED_INT_24_8:
            case GL.GL_UNSIGNED_INT_10F_11F_11F_REV:
            case GL2ES3.GL_UNSIGNED_INT_5_9_9_9_REV:
              compSize = 4;
              compCount = 1;
              break;
            case GL2ES3.GL_FLOAT_32_UNSIGNED_INT_24_8_REV:
              compSize = 8;
              compCount = 1;
              break;

            default:
              throw new GLException("type 0x"+Integer.toHexString(type)+"/"+"format 0x"+Integer.toHexString(format)+" not supported [yet], pls notify the maintainer in case this is our bug.");
        }
        return compCount * compSize;
    }

    /**
     * Returns the number of components required for the given OpenGL format.
     *
     * <p>This method is security critical, hence it throws an exception (fail-fast)
     * in case either the format, type or alignment is unhandled. In case we forgot to handle
     * proper values, please contact the maintainer.</p>
     *
     * @param format must be one of (27) <br/>
     *              GL_COLOR_INDEX GL_STENCIL_INDEX <br/>
     *              GL_DEPTH_COMPONENT GL_DEPTH_STENCIL <br/>
     *              GL_RED GL_RED_INTEGER <br/>
     *              GL_GREEN GL_GREEN_INTEGER <br/>
     *              GL_BLUE GL_BLUE_INTEGER <br/>
     *              GL_ALPHA GL_LUMINANCE (12) <br/>
     *              <br/>
     *              GL_LUMINANCE_ALPHA GL_RG <br/>
     *              GL_RG_INTEGER GL_HILO_NV <br/>
     *              GL_SIGNED_HILO_NV (5) <br/>
     *              <br/>
     *              GL_YCBCR_422_APPLE <br/>
     *              <br/>
     *              GL_RGB GL_RGB_INTEGER <br/>
     *              GL_BGR GL_BGR_INTEGER (4)<br/>
     *              <br/>
     *              GL_RGBA GL_RGBA_INTEGER <br/>
     *              GL_BGRA GL_BGRA_INTEGER <br/>
     *              GL_ABGR_EXT (5)<br/>
     *
     * @return number of components required for the given OpenGL format
     * @throws GLException if format is not handled. Please contact the maintainer if this is our bug.
     */
    public static final int componentCount(final int format) throws GLException {
        final int compCount;

        switch (format) /* 26 */ {
            case GL2.GL_COLOR_INDEX:
            case GL2ES2.GL_STENCIL_INDEX:
            case GL2ES2.GL_DEPTH_COMPONENT:
            case GL.GL_DEPTH_STENCIL:
            case GL2ES2.GL_RED:
            case GL2ES3.GL_RED_INTEGER:
            case GL2ES3.GL_GREEN:
            case GL2GL3.GL_GREEN_INTEGER:
            case GL2ES3.GL_BLUE:
            case GL2GL3.GL_BLUE_INTEGER:
            case GL.GL_ALPHA:
            case GL.GL_LUMINANCE:
              compCount = 1;
              break;
            case GL.GL_LUMINANCE_ALPHA:
            case GL2ES2.GL_RG:
            case GL2ES3.GL_RG_INTEGER:
            case GL2.GL_HILO_NV:
            case GL2.GL_SIGNED_HILO_NV:
              compCount = 2;
              break;
            case GL.GL_RGB:
            case GL2ES3.GL_RGB_INTEGER:
            case GL.GL_BGR:
            case GL2GL3.GL_BGR_INTEGER:
              compCount = 3;
              break;
            case GL2.GL_YCBCR_422_APPLE:
              compCount = 3;
              break;
            case GL.GL_RGBA:
            case GL2ES3.GL_RGBA_INTEGER:
            case GL.GL_BGRA:
            case GL2GL3.GL_BGRA_INTEGER:
            case GL2.GL_ABGR_EXT:
              compCount = 4;
              break;
            /* FIXME ??
             case GL.GL_HILO_NV:
              elements = 2;
              break; */
            default:
              throw new GLException("format 0x"+Integer.toHexString(format)+" not supported [yet], pls notify the maintainer in case this is our bug.");
        }
        return compCount;
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
    public final static float[] getFloatArray(final double[] source) {
        int i = source.length;
        final float[] dest = new float[i--];
        while (i >= 0) {
            dest[i] = (float) source[i];
            i--;
        }
        return dest;
    }
}
