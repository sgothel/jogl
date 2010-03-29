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

import com.jogamp.gluegen.runtime.Buffers;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import java.nio.*;

/**
 * Utility routines for dealing with direct buffers.
 * @author Kenneth Russel
 * @author Michael Bien
 */
public class BufferUtil extends Buffers {

    public static final int sizeOfGLType(int glType) {
        switch (glType) {
            case GL.GL_UNSIGNED_BYTE:
                return SIZEOF_BYTE;
            case GL.GL_BYTE:
                return SIZEOF_BYTE;
            case GL.GL_UNSIGNED_SHORT:
                return SIZEOF_SHORT;
            case GL.GL_SHORT:
                return SIZEOF_SHORT;
            case GL.GL_FLOAT:
                return SIZEOF_FLOAT;
            case GL.GL_FIXED:
                return SIZEOF_INT;
            case GL2ES2.GL_INT:
                return SIZEOF_INT;
            case GL2ES2.GL_UNSIGNED_INT:
                return SIZEOF_INT;
            case GL2.GL_DOUBLE:
                return SIZEOF_DOUBLE;
        }
        return -1;
    }

    public static final int sizeOfBufferElem(Buffer buffer) {
        if (buffer == null) {
            return 0;
        }
        if (buffer instanceof ByteBuffer) {
            return BufferUtil.SIZEOF_BYTE;
        } else if (buffer instanceof IntBuffer) {
            return BufferUtil.SIZEOF_INT;
        } else if (buffer instanceof ShortBuffer) {
            return BufferUtil.SIZEOF_SHORT;
        } else if (buffer instanceof FloatBuffer) {
            return BufferUtil.SIZEOF_FLOAT;
        } else if (buffer instanceof DoubleBuffer) {
            return BufferUtil.SIZEOF_DOUBLE;
        }
        throw new RuntimeException("Unexpected buffer type "
                + buffer.getClass().getName());
    }

    public static final Buffer newDirectGLBuffer(int glType, int numElements) {
        switch (glType) {
            case GL.GL_UNSIGNED_BYTE:
            case GL.GL_BYTE:
                return newDirectByteBuffer(numElements);
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_SHORT:
                return newDirectShortBuffer(numElements);
            case GL.GL_FLOAT:
                return newDirectFloatBuffer(numElements);
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL2ES2.GL_UNSIGNED_INT:
                return newDirectIntBuffer(numElements);
            case GL2.GL_DOUBLE:
                return newDirectDoubleBuffer(numElements);
        }
        return null;
    }

    public static final Buffer sliceGLBuffer(ByteBuffer parent, int bytePos, int byteLen, int glType) {
        if (parent == null || byteLen == 0) {
            return null;
        }
        parent.position(bytePos);
        parent.limit(bytePos + byteLen);

        switch (glType) {
            case GL.GL_UNSIGNED_BYTE:
            case GL.GL_BYTE:
                return parent.slice();
            case GL.GL_UNSIGNED_SHORT:
            case GL.GL_SHORT:
                return parent.asShortBuffer();
            case GL.GL_FLOAT:
                return parent.asFloatBuffer();
            case GL.GL_FIXED:
            case GL2ES2.GL_INT:
            case GL2ES2.GL_UNSIGNED_INT:
                return parent.asIntBuffer();
            case GL2.GL_DOUBLE:
                return parent.asDoubleBuffer();
        }
        return null;
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
