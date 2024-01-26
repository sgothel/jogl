/**
 * Copyright 2014-2023 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

package com.jogamp.math;

import java.nio.FloatBuffer;

import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.geom.Frustum.Plane;

/**
 * Basic 4x4 float matrix implementation using fields for intensive use-cases (host operations).
 * <p>
 * Implementation covers {@link FloatUtil} matrix functionality, exposed in an object oriented manner.
 * </p>
 * <p>
 * Unlike {@link com.jogamp.math.util.PMVMatrix4f PMVMatrix4f}, this class only represents one single matrix.
 * </p>
 * <p>
 * For array operations the layout is expected in column-major order
 * matching OpenGL's implementation, illustration:
 * <pre>
    Row-Major                       Column-Major (OpenGL):

        |  0   1   2  tx |
        |                |
        |  4   5   6  ty |
    M = |                |
        |  8   9  10  tz |
        |                |
        | 12  13  14  15 |

           R   C                      R   C
         m[0*4+3] = tx;             m[0+4*3] = tx;
         m[1*4+3] = ty;             m[1+4*3] = ty;
         m[2*4+3] = tz;             m[2+4*3] = tz;

          RC (std subscript order)   RC (std subscript order)
         m03 = tx;                  m03 = tx;
         m13 = ty;                  m13 = ty;
         m23 = tz;                  m23 = tz;

 * </pre>
 * </p>
 * <p>
 * <ul>
 *   <li><a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix-FAQ</a></li>
 *   <li><a href="https://en.wikipedia.org/wiki/Matrix_%28mathematics%29">Wikipedia-Matrix</a></li>
 *   <li><a href="http://www.euclideanspace.com/maths/algebra/matrix/index.htm">euclideanspace.com-Matrix</a></li>
 * </ul>
 * </p>
 * <p>
 * Implementation utilizes unrolling of small vertices and matrices wherever possible
 * while trying to access memory in a linear fashion for performance reasons, see:
 * <ul>
 *   <li><a href="https://lessthanoptimal.github.io/Java-Matrix-Benchmark/">java-matrix-benchmark</a></li>
 *   <li><a href="https://github.com/lessthanoptimal/ejml">EJML Efficient Java Matrix Library</a></li>
 * </ul>
 * </p>
 * @see com.jogamp.math.util.PMVMatrix4f
 * @see FloatUtil
 */
public class Matrix4f {

    /**
     * Creates a new identity matrix.
     */
    public Matrix4f() {
        m00 = m11 = m22 = m33 = 1.0f;
        // remaining fields have default init to zero
    }

    /**
     * Creates a new matrix copying the values of the given {@code src} matrix.
     */
    public Matrix4f(final Matrix4f src) {
        load(src);
    }

    /**
     * Creates a new matrix based on given float[4*4] column major order.
     * @param m 4x4 matrix in column-major order
     */
    public Matrix4f(final float[] m) {
        load(m);
    }

    /**
     * Creates a new matrix based on given float[4*4] column major order.
     * @param m 4x4 matrix in column-major order
     * @param m_off offset for matrix {@code m}
     */
    public Matrix4f(final float[] m, final int m_off) {
        load(m, m_off);
    }

    /**
     * Creates a new matrix based on given {@link FloatBuffer} 4x4 column major order.
     * @param m 4x4 matrix in column-major order
     */
    public Matrix4f(final FloatBuffer m) {
        load(m);
    }

    //
    // Write to Matrix via set(..) or load(..)
    //

    /** Sets the {@code i}th component with float {@code v} 0 <= i < 16 */
    public void set(final int i, final float v) {
        switch (i) {
            case 0+4*0: m00 = v; break;
            case 1+4*0: m10 = v; break;
            case 2+4*0: m20 = v; break;
            case 3+4*0: m30 = v; break;

            case 0+4*1: m01 = v; break;
            case 1+4*1: m11 = v; break;
            case 2+4*1: m21 = v; break;
            case 3+4*1: m31 = v; break;

            case 0+4*2: m02 = v; break;
            case 1+4*2: m12 = v; break;
            case 2+4*2: m22 = v; break;
            case 3+4*2: m32 = v; break;

            case 0+4*3: m03 = v; break;
            case 1+4*3: m13 = v; break;
            case 2+4*3: m23 = v; break;
            case 3+4*3: m33 = v; break;
            default: throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Set this matrix to identity.
     * <pre>
      Translation matrix (Column Order):
      1 0 0 0
      0 1 0 0
      0 0 1 0
      0 0 0 1
     * </pre>
     * @return this matrix for chaining
     */
    public final Matrix4f loadIdentity() {
       m00 = m11 = m22 = m33 = 1.0f;
       m01 = m02 = m03 =
       m10 = m12 = m13 =
       m20 = m21 = m23 =
       m30 = m31 = m32 = 0.0f;
       return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * @param src the source values
     * @return this matrix for chaining
     */
    public Matrix4f load(final Matrix4f src) {
        m00 = src.m00; m10 = src.m10; m20 = src.m20; m30 = src.m30;
        m01 = src.m01; m11 = src.m11; m21 = src.m21; m31 = src.m31;
        m02 = src.m02; m12 = src.m12; m22 = src.m22; m32 = src.m32;
        m03 = src.m03; m13 = src.m13; m23 = src.m23; m33 = src.m33;
        return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * @param src 4x4 matrix float[16] in column-major order
     * @return this matrix for chaining
     */
    public Matrix4f load(final float[] src) {
        m00 = src[0+0*4]; // column 0
        m10 = src[1+0*4];
        m20 = src[2+0*4];
        m30 = src[3+0*4];
        m01 = src[0+1*4]; // column 1
        m11 = src[1+1*4];
        m21 = src[2+1*4];
        m31 = src[3+1*4];
        m02 = src[0+2*4]; // column 2
        m12 = src[1+2*4];
        m22 = src[2+2*4];
        m32 = src[3+2*4];
        m03 = src[0+3*4]; // column 3
        m13 = src[1+3*4];
        m23 = src[2+3*4];
        m33 = src[3+3*4];
        return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * @param src 4x4 matrix float[16] in column-major order
     * @param src_off offset for matrix {@code src}
     * @return this matrix for chaining
     */
    public Matrix4f load(final float[] src, final int src_off) {
        m00 = src[src_off+0+0*4];
        m10 = src[src_off+1+0*4];
        m20 = src[src_off+2+0*4];
        m30 = src[src_off+3+0*4];
        m01 = src[src_off+0+1*4];
        m11 = src[src_off+1+1*4];
        m21 = src[src_off+2+1*4];
        m31 = src[src_off+3+1*4];
        m02 = src[src_off+0+2*4];
        m12 = src[src_off+1+2*4];
        m22 = src[src_off+2+2*4];
        m32 = src[src_off+3+2*4];
        m03 = src[src_off+0+3*4];
        m13 = src[src_off+1+3*4];
        m23 = src[src_off+2+3*4];
        m33 = src[src_off+3+3*4];
        return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * <p>
     * Implementation uses relative {@link FloatBuffer#get()},
     * hence caller may want to issue {@link FloatBuffer#reset()} thereafter.
     * </p>
     * @param src 4x4 matrix {@link FloatBuffer} in column-major order
     * @return this matrix for chaining
     */
    public Matrix4f load(final FloatBuffer src) {
        m00 = src.get();
        m10 = src.get();
        m20 = src.get();
        m30 = src.get();
        m01 = src.get();
        m11 = src.get();
        m21 = src.get();
        m31 = src.get();
        m02 = src.get();
        m12 = src.get();
        m22 = src.get();
        m32 = src.get();
        m03 = src.get();
        m13 = src.get();
        m23 = src.get();
        m33 = src.get();
        return this;
    }

    //
    // Read out Matrix via get(..)
    //

    /** Gets the {@code i}th component, 0 <= i < 16 */
    public float get(final int i) {
        switch (i) {
            case 0+4*0: return m00;
            case 1+4*0: return m10;
            case 2+4*0: return m20;
            case 3+4*0: return m30;

            case 0+4*1: return m01;
            case 1+4*1: return m11;
            case 2+4*1: return m21;
            case 3+4*1: return m31;

            case 0+4*2: return m02;
            case 1+4*2: return m12;
            case 2+4*2: return m22;
            case 3+4*2: return m32;

            case 0+4*3: return m03;
            case 1+4*3: return m13;
            case 2+4*3: return m23;
            case 3+4*3: return m33;

            default: throw new IndexOutOfBoundsException();
        }
    }

    /**
     * Get the named column of the given column-major matrix to v_out.
     * @param column named column to copy
     * @param v_out the column-vector storage
     * @return given result vector <i>v_out</i> for chaining
     */
    public Vec4f getColumn(final int column, final Vec4f v_out) {
        v_out.set( get(0+column*4),
                   get(1+column*4),
                   get(2+column*4),
                   get(3+column*4) );
        return v_out;
    }

    /**
     * Get the named column of the given column-major matrix to v_out.
     * @param column named column to copy
     * @param v_out the column-vector storage
     * @return given result vector <i>v_out</i> for chaining
     */
    public Vec3f getColumn(final int column, final Vec3f v_out) {
        v_out.set( get(0+column*4),
                   get(1+column*4),
                   get(2+column*4) );
        return v_out;
    }

    /**
     * Get the named row of the given column-major matrix to v_out.
     * @param row named row to copy
     * @param v_out the row-vector storage
     * @return given result vector <i>v_out</i> for chaining
     */
    public Vec4f getRow(final int row, final Vec4f v_out) {
        v_out.set( get(row+0*4),
                   get(row+1*4),
                   get(row+2*4),
                   get(row+3*4) );
        return v_out;
    }

    /**
     * Get the named row of the given column-major matrix to v_out.
     * @param row named row to copy
     * @param v_out the row-vector storage
     * @return given result vector <i>v_out</i> for chaining
     */
    public Vec3f getRow(final int row, final Vec3f v_out) {
        v_out.set( get(row+0*4),
                   get(row+1*4),
                   get(row+2*4) );
        return v_out;
    }

    /**
     * Get this matrix into the given float[16] array at {@code dst_off} in column major order.
     *
     * @param dst float[16] array storage in column major order
     * @param dst_off offset
     * @return {@code dst} for chaining
     */
    public float[] get(final float[] dst, final int dst_off) {
        dst[dst_off+0+0*4] = m00;
        dst[dst_off+1+0*4] = m10;
        dst[dst_off+2+0*4] = m20;
        dst[dst_off+3+0*4] = m30;
        dst[dst_off+0+1*4] = m01;
        dst[dst_off+1+1*4] = m11;
        dst[dst_off+2+1*4] = m21;
        dst[dst_off+3+1*4] = m31;
        dst[dst_off+0+2*4] = m02;
        dst[dst_off+1+2*4] = m12;
        dst[dst_off+2+2*4] = m22;
        dst[dst_off+3+2*4] = m32;
        dst[dst_off+0+3*4] = m03;
        dst[dst_off+1+3*4] = m13;
        dst[dst_off+2+3*4] = m23;
        dst[dst_off+3+3*4] = m33;
        return dst;
    }

    /**
     * Get this matrix into the given float[16] array in column major order.
     *
     * @param dst float[16] array storage in column major order
     * @return {@code dst} for chaining
     */
    public float[] get(final float[] dst) {
        dst[0+0*4] = m00;
        dst[1+0*4] = m10;
        dst[2+0*4] = m20;
        dst[3+0*4] = m30;
        dst[0+1*4] = m01;
        dst[1+1*4] = m11;
        dst[2+1*4] = m21;
        dst[3+1*4] = m31;
        dst[0+2*4] = m02;
        dst[1+2*4] = m12;
        dst[2+2*4] = m22;
        dst[3+2*4] = m32;
        dst[0+3*4] = m03;
        dst[1+3*4] = m13;
        dst[2+3*4] = m23;
        dst[3+3*4] = m33;
        return dst;
    }

    /**
     * Get this matrix into the given {@link FloatBuffer} in column major order.
     * <p>
     * Implementation uses relative {@link FloatBuffer#put(float)},
     * hence caller may want to issue {@link FloatBuffer#reset()} thereafter.
     * </p>
     *
     * @param dst {@link FloatBuffer} array storage in column major order
     * @return {@code dst} for chaining
     */
    public FloatBuffer get(final FloatBuffer dst) {
        dst.put( m00 );
        dst.put( m10 );
        dst.put( m20 );
        dst.put( m30 );
        dst.put( m01 );
        dst.put( m11 );
        dst.put( m21 );
        dst.put( m31 );
        dst.put( m02 );
        dst.put( m12 );
        dst.put( m22 );
        dst.put( m32 );
        dst.put( m03 );
        dst.put( m13 );
        dst.put( m23 );
        dst.put( m33 );
        return dst;
    }

    //
    // Basic matrix operations
    //

    /**
     * Returns the determinant of this matrix
     * @return the matrix determinant
     */
    public float determinant() {
        float ret = 0;
        ret += m00 * ( + m11*(m22*m33 - m23*m32) - m12*(m21*m33 - m23*m31) + m13*(m21*m32 - m22*m31));
        ret -= m01 * ( + m10*(m22*m33 - m23*m32) - m12*(m20*m33 - m23*m30) + m13*(m20*m32 - m22*m30));
        ret += m02 * ( + m10*(m21*m33 - m23*m31) - m11*(m20*m33 - m23*m30) + m13*(m20*m31 - m21*m30));
        ret -= m03 * ( + m10*(m21*m32 - m22*m31) - m11*(m20*m32 - m22*m30) + m12*(m20*m31 - m21*m30));
        return ret;
    }

    /**
     * Transpose this matrix.
     *
     * @return this matrix for chaining
     */
    public final Matrix4f transpose() {
        float tmp;

        tmp = m10;
        m10 = m01;
        m01 = tmp;

        tmp = m20;
        m20 = m02;
        m02 = tmp;

        tmp = m30;
        m30 = m03;
        m03 = tmp;

        tmp = m21;
        m21 = m12;
        m12 = tmp;

        tmp = m31;
        m31 = m13;
        m13 = tmp;

        tmp = m32;
        m32 = m23;
        m23 = tmp;

        return this;
    }

    /**
     * Transpose the given {@code src} matrix into this matrix.
     *
     * @param src source 4x4 matrix
     * @return this matrix (result) for chaining
     */
    public final Matrix4f transpose(final Matrix4f src) {
        if( src == this ) {
            return transpose();
        }
        m00 = src.m00;
        m10 = src.m01;
        m20 = src.m02;
        m30 = src.m03;

        m01 = src.m10;
        m11 = src.m11;
        m21 = src.m12;
        m31 = src.m13;

        m02 = src.m20;
        m12 = src.m21;
        m22 = src.m22;
        m32 = src.m23;

        m03 = src.m30;
        m13 = src.m31;
        m23 = src.m32;
        m33 = src.m33;
        return this;
    }

    /**
     * Invert this matrix.
     * @return false if this matrix is singular and inversion not possible, otherwise true
     */
    public boolean invert() {
        final float scale;
        try {
            scale = mulScale();
        } catch(final ArithmeticException aex) {
            return false; // max was 0
        }
        final float a00 = m00*scale;
        final float a10 = m10*scale;
        final float a20 = m20*scale;
        final float a30 = m30*scale;

        final float a01 = m01*scale;
        final float a11 = m11*scale;
        final float a21 = m21*scale;
        final float a31 = m31*scale;

        final float a02 = m02*scale;
        final float a12 = m12*scale;
        final float a22 = m22*scale;
        final float a32 = m32*scale;

        final float a03 = m03*scale;
        final float a13 = m13*scale;
        final float a23 = m23*scale;
        final float a33 = m33*scale;

        final float b00 = + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31);
        final float b01 = -( + a10*(a22*a33 - a23*a32) - a12*(a20*a33 - a23*a30) + a13*(a20*a32 - a22*a30));
        final float b02 = + a10*(a21*a33 - a23*a31) - a11*(a20*a33 - a23*a30) + a13*(a20*a31 - a21*a30);
        final float b03 = -( + a10*(a21*a32 - a22*a31) - a11*(a20*a32 - a22*a30) + a12*(a20*a31 - a21*a30));

        final float b10 = -( + a01*(a22*a33 - a23*a32) - a02*(a21*a33 - a23*a31) + a03*(a21*a32 - a22*a31));
        final float b11 = + a00*(a22*a33 - a23*a32) - a02*(a20*a33 - a23*a30) + a03*(a20*a32 - a22*a30);
        final float b12 = -( + a00*(a21*a33 - a23*a31) - a01*(a20*a33 - a23*a30) + a03*(a20*a31 - a21*a30));
        final float b13 = + a00*(a21*a32 - a22*a31) - a01*(a20*a32 - a22*a30) + a02*(a20*a31 - a21*a30);

        final float b20 = + a01*(a12*a33 - a13*a32) - a02*(a11*a33 - a13*a31) + a03*(a11*a32 - a12*a31);
        final float b21 = -( + a00*(a12*a33 - a13*a32) - a02*(a10*a33 - a13*a30) + a03*(a10*a32 - a12*a30));
        final float b22 = + a00*(a11*a33 - a13*a31) - a01*(a10*a33 - a13*a30) + a03*(a10*a31 - a11*a30);
        final float b23 = -( + a00*(a11*a32 - a12*a31) - a01*(a10*a32 - a12*a30) + a02*(a10*a31 - a11*a30));

        final float b30 = -( + a01*(a12*a23 - a13*a22) - a02*(a11*a23 - a13*a21) + a03*(a11*a22 - a12*a21));
        final float b31 = + a00*(a12*a23 - a13*a22) - a02*(a10*a23 - a13*a20) + a03*(a10*a22 - a12*a20);
        final float b32 = -( + a00*(a11*a23 - a13*a21) - a01*(a10*a23 - a13*a20) + a03*(a10*a21 - a11*a20));
        final float b33 = + a00*(a11*a22 - a12*a21) - a01*(a10*a22 - a12*a20) + a02*(a10*a21 - a11*a20);

        final float det = (a00*b00 + a01*b01 + a02*b02 + a03*b03) / scale;
        if( 0 == det ) {
            return false;
        }
        final float invdet = 1.0f / det;

        m00 = b00 * invdet;
        m10 = b01 * invdet;
        m20 = b02 * invdet;
        m30 = b03 * invdet;

        m01 = b10 * invdet;
        m11 = b11 * invdet;
        m21 = b12 * invdet;
        m31 = b13 * invdet;

        m02 = b20 * invdet;
        m12 = b21 * invdet;
        m22 = b22 * invdet;
        m32 = b23 * invdet;

        m03 = b30 * invdet;
        m13 = b31 * invdet;
        m23 = b32 * invdet;
        m33 = b33 * invdet;
        return true;
    }

    /**
     * Invert the {@code src} matrix values into this matrix
     * @param src the source matrix, which values are to be inverted
     * @return false if {@code src} matrix is singular and inversion not possible, otherwise true
     */
    public boolean invert(final Matrix4f src) {
        final float scale;
        try {
            scale = src.mulScale();
        } catch(final ArithmeticException aex) {
            return false; // max was 0
        }
        final float a00 = src.m00*scale;
        final float a10 = src.m10*scale;
        final float a20 = src.m20*scale;
        final float a30 = src.m30*scale;

        final float a01 = src.m01*scale;
        final float a11 = src.m11*scale;
        final float a21 = src.m21*scale;
        final float a31 = src.m31*scale;

        final float a02 = src.m02*scale;
        final float a12 = src.m12*scale;
        final float a22 = src.m22*scale;
        final float a32 = src.m32*scale;

        final float a03 = src.m03*scale;
        final float a13 = src.m13*scale;
        final float a23 = src.m23*scale;
        final float a33 = src.m33*scale;

        final float b00 = + a11*(a22*a33 - a23*a32) - a12*(a21*a33 - a23*a31) + a13*(a21*a32 - a22*a31);
        final float b01 = -( + a10*(a22*a33 - a23*a32) - a12*(a20*a33 - a23*a30) + a13*(a20*a32 - a22*a30));
        final float b02 = + a10*(a21*a33 - a23*a31) - a11*(a20*a33 - a23*a30) + a13*(a20*a31 - a21*a30);
        final float b03 = -( + a10*(a21*a32 - a22*a31) - a11*(a20*a32 - a22*a30) + a12*(a20*a31 - a21*a30));

        final float b10 = -( + a01*(a22*a33 - a23*a32) - a02*(a21*a33 - a23*a31) + a03*(a21*a32 - a22*a31));
        final float b11 = + a00*(a22*a33 - a23*a32) - a02*(a20*a33 - a23*a30) + a03*(a20*a32 - a22*a30);
        final float b12 = -( + a00*(a21*a33 - a23*a31) - a01*(a20*a33 - a23*a30) + a03*(a20*a31 - a21*a30));
        final float b13 = + a00*(a21*a32 - a22*a31) - a01*(a20*a32 - a22*a30) + a02*(a20*a31 - a21*a30);

        final float b20 = + a01*(a12*a33 - a13*a32) - a02*(a11*a33 - a13*a31) + a03*(a11*a32 - a12*a31);
        final float b21 = -( + a00*(a12*a33 - a13*a32) - a02*(a10*a33 - a13*a30) + a03*(a10*a32 - a12*a30));
        final float b22 = + a00*(a11*a33 - a13*a31) - a01*(a10*a33 - a13*a30) + a03*(a10*a31 - a11*a30);
        final float b23 = -( + a00*(a11*a32 - a12*a31) - a01*(a10*a32 - a12*a30) + a02*(a10*a31 - a11*a30));

        final float b30 = -( + a01*(a12*a23 - a13*a22) - a02*(a11*a23 - a13*a21) + a03*(a11*a22 - a12*a21));
        final float b31 = + a00*(a12*a23 - a13*a22) - a02*(a10*a23 - a13*a20) + a03*(a10*a22 - a12*a20);
        final float b32 = -( + a00*(a11*a23 - a13*a21) - a01*(a10*a23 - a13*a20) + a03*(a10*a21 - a11*a20));
        final float b33 = + a00*(a11*a22 - a12*a21) - a01*(a10*a22 - a12*a20) + a02*(a10*a21 - a11*a20);

        final float det = (a00*b00 + a01*b01 + a02*b02 + a03*b03) / scale;

        if( 0 == det ) {
            return false;
        }
        final float invdet = 1.0f / det;

        m00 = b00 * invdet;
        m10 = b01 * invdet;
        m20 = b02 * invdet;
        m30 = b03 * invdet;

        m01 = b10 * invdet;
        m11 = b11 * invdet;
        m21 = b12 * invdet;
        m31 = b13 * invdet;

        m02 = b20 * invdet;
        m12 = b21 * invdet;
        m22 = b22 * invdet;
        m32 = b23 * invdet;

        m03 = b30 * invdet;
        m13 = b31 * invdet;
        m23 = b32 * invdet;
        m33 = b33 * invdet;
        return true;
    }

    private final float mulScale() {
        /**
        // No Hotspot intrinsic Math.* optimization for at least Math.max(),
        // hence this chunk is slower.
        float max = Math.abs(m00);

        max = Math.max(max, Math.abs(m01));
        max = Math.max(max, Math.abs(m02));
        ... etc
        */
        float a = Math.abs(m00);
        float max = a;
        a = Math.abs(m01); if( a > max ) max = a;
        a = Math.abs(m02); if( a > max ) max = a;
        a = Math.abs(m03); if( a > max ) max = a;

        a = Math.abs(m10); if( a > max ) max = a;
        a = Math.abs(m11); if( a > max ) max = a;
        a = Math.abs(m12); if( a > max ) max = a;
        a = Math.abs(m13); if( a > max ) max = a;

        a = Math.abs(m20); if( a > max ) max = a;
        a = Math.abs(m21); if( a > max ) max = a;
        a = Math.abs(m22); if( a > max ) max = a;
        a = Math.abs(m23); if( a > max ) max = a;

        a = Math.abs(m30); if( a > max ) max = a;
        a = Math.abs(m31); if( a > max ) max = a;
        a = Math.abs(m32); if( a > max ) max = a;
        a = Math.abs(m33); if( a > max ) max = a;

        return 1.0f/max;
    }

    /**
     * Multiply matrix: [this] = [this] x [b]
     * @param b 4x4 matrix
     * @return this matrix for chaining
     * @see #mul(Matrix4f, Matrix4f)
     */
    public final Matrix4f mul(final Matrix4f b) {
        // return mul(new Matrix4f(this), b); // <- roughly half speed
        float ai0=m00; // row-0, m[0+0*4]
        float ai1=m01;
        float ai2=m02;
        float ai3=m03;
        m00 = ai0 * b.m00  +  ai1 * b.m10  +  ai2 * b.m20  +  ai3 * b.m30 ;
        m01 = ai0 * b.m01  +  ai1 * b.m11  +  ai2 * b.m21  +  ai3 * b.m31 ;
        m02 = ai0 * b.m02  +  ai1 * b.m12  +  ai2 * b.m22  +  ai3 * b.m32 ;
        m03 = ai0 * b.m03  +  ai1 * b.m13  +  ai2 * b.m23  +  ai3 * b.m33 ;

        ai0=m10; //row-1, m[1+0*4]
        ai1=m11;
        ai2=m12;
        ai3=m13;
        m10 = ai0 * b.m00  +  ai1 * b.m10  +  ai2 * b.m20  +  ai3 * b.m30 ;
        m11 = ai0 * b.m01  +  ai1 * b.m11  +  ai2 * b.m21  +  ai3 * b.m31 ;
        m12 = ai0 * b.m02  +  ai1 * b.m12  +  ai2 * b.m22  +  ai3 * b.m32 ;
        m13 = ai0 * b.m03  +  ai1 * b.m13  +  ai2 * b.m23  +  ai3 * b.m33 ;

        ai0=m20; // row-2, m[2+0*4]
        ai1=m21;
        ai2=m22;
        ai3=m23;
        m20 = ai0 * b.m00  +  ai1 * b.m10  +  ai2 * b.m20  +  ai3 * b.m30 ;
        m21 = ai0 * b.m01  +  ai1 * b.m11  +  ai2 * b.m21  +  ai3 * b.m31 ;
        m22 = ai0 * b.m02  +  ai1 * b.m12  +  ai2 * b.m22  +  ai3 * b.m32 ;
        m23 = ai0 * b.m03  +  ai1 * b.m13  +  ai2 * b.m23  +  ai3 * b.m33 ;

        ai0=m30; // row-3, m[3+0*4]
        ai1=m31;
        ai2=m32;
        ai3=m33;
        m30 = ai0 * b.m00  +  ai1 * b.m10  +  ai2 * b.m20  +  ai3 * b.m30 ;
        m31 = ai0 * b.m01  +  ai1 * b.m11  +  ai2 * b.m21  +  ai3 * b.m31 ;
        m32 = ai0 * b.m02  +  ai1 * b.m12  +  ai2 * b.m22  +  ai3 * b.m32 ;
        m33 = ai0 * b.m03  +  ai1 * b.m13  +  ai2 * b.m23  +  ai3 * b.m33 ;
        return this;
    }

    /**
     * Multiply matrix: [this] = [a] x [b]
     * @param a 4x4 matrix, can't be this matrix
     * @param b 4x4 matrix, can't be this matrix
     * @return this matrix for chaining
     * @see #mul(Matrix4f)
     */
    public final Matrix4f mul(final Matrix4f a, final Matrix4f b) {
        // row-0, m[0+0*4]
        m00 = a.m00 * b.m00  +  a.m01 * b.m10  +  a.m02 * b.m20  +  a.m03 * b.m30 ;
        m01 = a.m00 * b.m01  +  a.m01 * b.m11  +  a.m02 * b.m21  +  a.m03 * b.m31 ;
        m02 = a.m00 * b.m02  +  a.m01 * b.m12  +  a.m02 * b.m22  +  a.m03 * b.m32 ;
        m03 = a.m00 * b.m03  +  a.m01 * b.m13  +  a.m02 * b.m23  +  a.m03 * b.m33 ;

        //row-1, m[1+0*4]
        m10 = a.m10 * b.m00  +  a.m11 * b.m10  +  a.m12 * b.m20  +  a.m13 * b.m30 ;
        m11 = a.m10 * b.m01  +  a.m11 * b.m11  +  a.m12 * b.m21  +  a.m13 * b.m31 ;
        m12 = a.m10 * b.m02  +  a.m11 * b.m12  +  a.m12 * b.m22  +  a.m13 * b.m32 ;
        m13 = a.m10 * b.m03  +  a.m11 * b.m13  +  a.m12 * b.m23  +  a.m13 * b.m33 ;

        // row-2, m[2+0*4]
        m20 = a.m20 * b.m00  +  a.m21 * b.m10  +  a.m22 * b.m20  +  a.m23 * b.m30 ;
        m21 = a.m20 * b.m01  +  a.m21 * b.m11  +  a.m22 * b.m21  +  a.m23 * b.m31 ;
        m22 = a.m20 * b.m02  +  a.m21 * b.m12  +  a.m22 * b.m22  +  a.m23 * b.m32 ;
        m23 = a.m20 * b.m03  +  a.m21 * b.m13  +  a.m22 * b.m23  +  a.m23 * b.m33 ;

        // row-3, m[3+0*4]
        m30 = a.m30 * b.m00  +  a.m31 * b.m10  +  a.m32 * b.m20  +  a.m33 * b.m30 ;
        m31 = a.m30 * b.m01  +  a.m31 * b.m11  +  a.m32 * b.m21  +  a.m33 * b.m31 ;
        m32 = a.m30 * b.m02  +  a.m31 * b.m12  +  a.m32 * b.m22  +  a.m33 * b.m32 ;
        m33 = a.m30 * b.m03  +  a.m31 * b.m13  +  a.m32 * b.m23  +  a.m33 * b.m33 ;

        return this;
    }

    /**
     * @param v_in 4-component column-vector, can be v_out for in-place transformation
     * @param v_out this * v_in
     * @returns v_out for chaining
     */
    public final Vec4f mulVec4f(final Vec4f v_in, final Vec4f v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in.x(), y = v_in.y(), z = v_in.z(), w = v_in.w();
        v_out.set( x * m00 + y * m01 + z * m02 + w * m03,
                   x * m10 + y * m11 + z * m12 + w * m13,
                   x * m20 + y * m21 + z * m22 + w * m23,
                   x * m30 + y * m31 + z * m32 + w * m33 );
        return v_out;
    }

    /**
     * @param v_inout 4-component column-vector input and output, i.e. in-place transformation
     * @returns v_inout for chaining
     */
    public final Vec4f mulVec4f(final Vec4f v_inout) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_inout.x(), y = v_inout.y(), z = v_inout.z(), w = v_inout.w();
        v_inout.set( x * m00 + y * m01 + z * m02 + w * m03,
                     x * m10 + y * m11 + z * m12 + w * m13,
                     x * m20 + y * m21 + z * m22 + w * m23,
                     x * m30 + y * m31 + z * m32 + w * m33 );
        return v_inout;
    }

    /**
     * Affine 3f-vector transformation by 4x4 matrix
     *
     * 4x4 matrix multiplication with 3-component vector,
     * using {@code 1} for for {@code v_in.w()} and dropping {@code v_out.w()},
     * which shall be {@code 1}.
     *
     * @param v_in 3-component column-vector {@link Vec3f}, can be v_out for in-place transformation
     * @param v_out m_in * v_in, 3-component column-vector {@link Vec3f}
     * @returns v_out for chaining
     */
    public final Vec3f mulVec3f(final Vec3f v_in, final Vec3f v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in.x(), y = v_in.y(), z = v_in.z();
        v_out.set( x * m00 + y * m01 + z * m02 + 1f * m03,
                   x * m10 + y * m11 + z * m12 + 1f * m13,
                   x * m20 + y * m21 + z * m22 + 1f * m23 );
        return v_out;
    }

    /**
     * Affine 3f-vector transformation by 4x4 matrix
     *
     * 4x4 matrix multiplication with 3-component vector,
     * using {@code 1} for for {@code v_inout.w()} and dropping {@code v_inout.w()},
     * which shall be {@code 1}.
     *
     * @param v_inout 3-component column-vector {@link Vec3f} input and output, i.e. in-place transformation
     * @returns v_inout for chaining
     */
    public final Vec3f mulVec3f(final Vec3f v_inout) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_inout.x(), y = v_inout.y(), z = v_inout.z();
        v_inout.set( x * m00 + y * m01 + z * m02 + 1f * m03,
                     x * m10 + y * m11 + z * m12 + 1f * m13,
                     x * m20 + y * m21 + z * m22 + 1f * m23 );
        return v_inout;
    }

    //
    // Matrix setTo...(), affine + basic
    //

    /**
     * Set this matrix to translation.
     * <pre>
      Translation matrix (Column Order):
      1 0 0 0
      0 1 0 0
      0 0 1 0
      x y z 1
     * </pre>
     * @param x x-axis translate
     * @param y y-axis translate
     * @param z z-axis translate
     * @return this matrix for chaining
     */
    public final Matrix4f setToTranslation(final float x, final float y, final float z) {
        m00 = m11 = m22 = m33 = 1.0f;
        m03 = x;
        m13 = y;
        m23 = z;
        m01 = m02 =
        m10 = m12 =
        m20 = m21 =
        m30 = m31 = m32 = 0.0f;
        return this;
    }

    /**
     * Set this matrix to translation.
     * <pre>
      Translation matrix (Column Order):
      1 0 0 0
      0 1 0 0
      0 0 1 0
      x y z 1
     * </pre>
     * @param t translate Vec3f
     * @return this matrix for chaining
     */
    public final Matrix4f setToTranslation(final Vec3f t) {
        return setToTranslation(t.x(), t.y(), t.z());
    }

    /**
     * Set this matrix to scale.
     * <pre>
      Scale matrix (Any Order):
      x 0 0 0
      0 y 0 0
      0 0 z 0
      0 0 0 1
     * </pre>
     * @param x x-axis scale
     * @param y y-axis scale
     * @param z z-axis scale
     * @return this matrix for chaining
     */
    public final Matrix4f setToScale(final float x, final float y, final float z) {
        m33 = 1.0f;
        m00 = x;
        m11 = y;
        m22 = z;
        m01 = m02 = m03 =
        m10 = m12 = m13 =
        m20 = m21 = m23 =
        m30 = m31 = m32 = 0.0f;
        return this;
    }

    /**
     * Set this matrix to scale.
     * <pre>
      Scale matrix (Any Order):
      x 0 0 0
      0 y 0 0
      0 0 z 0
      0 0 0 1
     * </pre>
     * @param s scale Vec3f
     * @return this matrix for chaining
     */
    public final Matrix4f setToScale(final Vec3f s) {
        return setToScale(s.x(), s.y(), s.z());
    }

    /**
     * Set this matrix to rotation from the given axis and angle in radians.
     * <pre>
        Rotation matrix (Column Order):
        xx(1-c)+c  xy(1-c)+zs xz(1-c)-ys 0
        xy(1-c)-zs yy(1-c)+c  yz(1-c)+xs 0
        xz(1-c)+ys yz(1-c)-xs zz(1-c)+c  0
        0          0          0          1
     * </pre>
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
     * @param ang_rad angle in radians
     * @param x x of rotation axis
     * @param y y of rotation axis
     * @param z z of rotation axis
     * @return this matrix for chaining
     */
    public final Matrix4f setToRotationAxis(final float ang_rad, float x, float y, float z) {
        final float c = FloatUtil.cos(ang_rad);
        final float ic= 1.0f - c;
        final float s = FloatUtil.sin(ang_rad);

        final Vec3f tmp = new Vec3f(x, y, z).normalize();
        x = tmp.x(); y = tmp.y(); z = tmp.z();

        final float xy = x*y;
        final float xz = x*z;
        final float xs = x*s;
        final float ys = y*s;
        final float yz = y*z;
        final float zs = z*s;
        m00 = x*x*ic+c;
        m10 = xy*ic+zs;
        m20 = xz*ic-ys;
        m30 = 0;

        m01 = xy*ic-zs;
        m11 = y*y*ic+c;
        m21 = yz*ic+xs;
        m31 = 0;

        m02 = xz*ic+ys;
        m12 = yz*ic-xs;
        m22 = z*z*ic+c;
        m32 = 0;

        m03 = 0f;
        m13 = 0f;
        m23 = 0f;
        m33 = 1f;

        return this;
    }

    /**
     * Set this matrix to rotation from the given axis and angle in radians.
     * <pre>
        Rotation matrix (Column Order):
        xx(1-c)+c  xy(1-c)+zs xz(1-c)-ys 0
        xy(1-c)-zs yy(1-c)+c  yz(1-c)+xs 0
        xz(1-c)+ys yz(1-c)-xs zz(1-c)+c  0
        0          0          0          1
     * </pre>
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
     * @param ang_rad angle in radians
     * @param axis rotation axis
     * @return this matrix for chaining
     */
    public final Matrix4f setToRotationAxis(final float ang_rad, final Vec3f axis) {
        return setToRotationAxis(ang_rad, axis.x(), axis.y(), axis.z());
    }

    /**
     * Set this matrix to rotation from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order:
     * <ul>
     *  <li>y - heading</li>
     *  <li>z - attitude</li>
     *  <li>x - bank</li>
     * </ul>
     * </p>
     * @param bankX the Euler pitch angle in radians. (rotation about the X axis)
     * @param headingY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
     * @return this matrix for chaining
     * <p>
     * Implementation does not use Quaternion and hence is exposed to
     * <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q34">Gimbal-Lock</a>,
     * consider using {@link #setToRotation(Quaternion)}.
     * </p>
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q36">Matrix-FAQ Q36</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm">euclideanspace.com-eulerToMatrix</a>
     * @see #setToRotation(Quaternion)
     */
    public Matrix4f setToRotationEuler(final float bankX, final float headingY, final float attitudeZ) {
        // Assuming the angles are in radians.
        final float ch = FloatUtil.cos(headingY);
        final float sh = FloatUtil.sin(headingY);
        final float ca = FloatUtil.cos(attitudeZ);
        final float sa = FloatUtil.sin(attitudeZ);
        final float cb = FloatUtil.cos(bankX);
        final float sb = FloatUtil.sin(bankX);

        m00 =  ch*ca;
        m10 =  sa;
        m20 = -sh*ca;
        m30 =  0;

        m01 =  sh*sb    - ch*sa*cb;
        m11 =  ca*cb;
        m21 =  sh*sa*cb + ch*sb;
        m31 =  0;

        m02 =  ch*sa*sb + sh*cb;
        m12 = -ca*sb;
        m22 = -sh*sa*sb + ch*cb;
        m32 =  0;

        m03 =  0;
        m13 =  0;
        m23 =  0;
        m33 =  1;

        return this;
    }

    /**
     * Set this matrix to rotation from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order:
     * <ul>
     *  <li>y - heading</li>
     *  <li>z - attitude</li>
     *  <li>x - bank</li>
     * </ul>
     * </p>
     * @param angradXYZ euler angle vector in radians holding x-bank, y-heading and z-attitude
     * @return this quaternion for chaining.
     * <p>
     * Implementation does not use Quaternion and hence is exposed to
     * <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q34">Gimbal-Lock</a>,
     * consider using {@link #setToRotation(Quaternion)}.
     * </p>
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q36">Matrix-FAQ Q36</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToMatrix/index.htm">euclideanspace.com-eulerToMatrix</a>
     * @see #setToRotation(Quaternion)
     */
    public Matrix4f setToRotationEuler(final Vec3f angradXYZ) {
        return setToRotationEuler(angradXYZ.x(), angradXYZ.y(), angradXYZ.z());
    }

    /**
     * Set this matrix to rotation using the given Quaternion.
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> makes identity matrix if {@link #magnitudeSquared()} is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     *   <li> The fields [m00 .. m22] define the rotation</li>
     * </ul>
     * </p>
     *
     * @param q the Quaternion representing the rotation
     * @return this matrix for chaining
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q54">Matrix-FAQ Q54</a>
     * @see Quaternion#toMatrix(float[])
     * @see #getRotation()
     */
    public final Matrix4f setToRotation(final Quaternion q) {
        // pre-multiply scaled-reciprocal-magnitude to reduce multiplications
        final float norm = q.magnitudeSquared();
        if ( FloatUtil.isZero(norm) ) {
            // identity matrix -> srecip = 0f
            loadIdentity();
            return this;
        }
        final float srecip;
        if ( FloatUtil.isEqual(1f, norm) ) {
            srecip = 2f;
        } else {
            srecip = 2.0f / norm;
        }

        final float x = q.x();
        final float y = q.y();
        final float z = q.z();
        final float w = q.w();

        final float xs = srecip * x;
        final float ys = srecip * y;
        final float zs = srecip * z;

        final float xx = x  * xs;
        final float xy = x  * ys;
        final float xz = x  * zs;
        final float xw = xs * w;
        final float yy = y  * ys;
        final float yz = y  * zs;
        final float yw = ys * w;
        final float zz = z  * zs;
        final float zw = zs * w;

        m00 = 1f - ( yy + zz );
        m01 =      ( xy - zw );
        m02 =      ( xz + yw );
        m03 = 0f;

        m10 =      ( xy + zw );
        m11 = 1f - ( xx + zz );
        m12 =      ( yz - xw );
        m13 = 0f;

        m20 =      ( xz - yw );
        m21 =      ( yz + xw );
        m22 = 1f - ( xx + yy );
        m23 = 0f;

        m30 = m31 = m32 = 0f;
        m33 = 1f;
        return this;
    }

    /**
     * Returns the rotation [m00 .. m22] fields converted to a Quaternion.
     * @param res resulting Quaternion
     * @return the resulting Quaternion for chaining.
     * @see Quaternion#setFromMatrix(float, float, float, float, float, float, float, float, float)
     * @see #setToRotation(Quaternion)
     */
    public final Quaternion getRotation(final Quaternion res) {
        res.setFromMatrix(m00, m01, m02, m10, m11, m12, m20, m21, m22);
        return res;
    }

    /**
     * Set this matrix to orthogonal projection.
     * <pre>
      Ortho matrix (Column Order):
      2/dx  0     0    0
      0     2/dy  0    0
      0     0     2/dz 0
      tx    ty    tz   1
     * </pre>
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param zNear
     * @param zFar
     * @return this matrix for chaining
     */
    public Matrix4f setToOrtho(final float left, final float right,
                               final float bottom, final float top,
                               final float zNear, final float zFar) {
        {
            // m00 = m11 = m22 = m33 = 1f;
            m10 = m20 = m30 = 0f;
            m01 = m21 = m31 = 0f;
            m02 = m12 = m32 = 0f;
            // m03 = m13 = m23 = 0f;
        }
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float tx=-1.0f*(right+left)/dx;
        final float ty=-1.0f*(top+bottom)/dy;
        final float tz=-1.0f*(zFar+zNear)/dz;

        m00 =  2.0f/dx;
        m11 =  2.0f/dy;
        m22 = -2.0f/dz;

        m03 = tx;
        m13 = ty;
        m23 = tz;
        m33 = 1f;

        return this;
    }

    /**
     * Set this matrix to frustum.
     * <pre>
      Frustum matrix (Column Order):
      2*zNear/dx   0          0   0
      0            2*zNear/dy 0   0
      A            B          C  -1
      0            0          D   0
     * </pre>
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param zNear
     * @param zFar
     * @return this matrix for chaining
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     *                                  or {@code left == right}, or {@code bottom == top}.
     */
    public Matrix4f setToFrustum(final float left, final float right,
                                 final float bottom, final float top,
                                 final float zNear, final float zFar) throws IllegalArgumentException {
        if( zNear <= 0.0f || zFar <= zNear ) {
            throw new IllegalArgumentException("Requirements zNear > 0 and zFar > zNear, but zNear "+zNear+", zFar "+zFar);
        }
        if( left == right || top == bottom) {
            throw new IllegalArgumentException("GL_INVALID_VALUE: top,bottom and left,right must not be equal");
        }
        {
            // m00 = m11 = m22 = m33 = 1f;
            m10 = m20 = m30 = 0f;
            m01 = m21 = m31 = 0f;
            m03 = m13 = 0f;
        }
        final float zNear2 = 2.0f*zNear;
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float A=(right+left)/dx;
        final float B=(top+bottom)/dy;
        final float C=-1.0f*(zFar+zNear)/dz;
        final float D=-2.0f*(zFar*zNear)/dz;

        m00 = zNear2/dx;
        m11 = zNear2/dy;

        m02 = A;
        m12 = B;
        m22 = C;
        m32 = -1.0f;

        m23 = D;
        m33 = 0f;

        return this;
    }

    /**
     * Set this matrix to perspective {@link #setToFrustum(float, float, float, float, float, float) frustum} projection.
     *
     * @param fovy_rad angle in radians
     * @param aspect aspect ratio width / height
     * @param zNear
     * @param zFar
     * @return this matrix for chaining
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see #setToFrustum(float, float, float, float, float, float)
     */
    public Matrix4f setToPerspective(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws IllegalArgumentException {
        final float top    =  FloatUtil.tan(fovy_rad/2f) * zNear; // use tangent of half-fov !
        final float bottom =  -1.0f * top;    //          -1f * fovhvTan.top * zNear
        final float left   = aspect * bottom; // aspect * -1f * fovhvTan.top * zNear
        final float right  = aspect * top;    // aspect * fovhvTan.top * zNear
        return setToFrustum(left, right, bottom, top, zNear, zFar);
    }

    /**
     * Set this matrix to perspective {@link #setToFrustum(float, float, float, float, float, float) frustum} projection.
     *
     * @param fovhv {@link FovHVHalves} field of view in both directions, may not be centered, either in radians or tangent
     * @param zNear
     * @param zFar
     * @return this matrix for chaining
     * @throws IllegalArgumentException if {@code zNear <= 0} or {@code zFar <= zNear}
     * @see #setToFrustum(float, float, float, float, float, float)
     * @see Frustum#updateByFovDesc(Matrix4f, com.jogamp.math.geom.Frustum.FovDesc)
     */
    public Matrix4f setToPerspective(final FovHVHalves fovhv, final float zNear, final float zFar) throws IllegalArgumentException {
        final FovHVHalves fovhvTan = fovhv.toTangents();  // use tangent of half-fov !
        final float top    =         fovhvTan.top    * zNear;
        final float bottom = -1.0f * fovhvTan.bottom * zNear;
        final float left   = -1.0f * fovhvTan.left   * zNear;
        final float right  =         fovhvTan.right  * zNear;
        return setToFrustum(left, right, bottom, top, zNear, zFar);
    }

    /**
     * Calculate the frustum planes in world coordinates
     * using this premultiplied P*MV (column major order) matrix.
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by the {@link Frustum} class.
     * </p>
     * @see Frustum#updateFrustumPlanes(Matrix4f)
     */
    public Frustum updateFrustumPlanes(final Frustum frustum) {
        // Left:   a = m41 + m11, b = m42 + m12, c = m43 + m13, d = m44 + m14  - [1..4] column-major
        // Left:   a = m30 + m00, b = m31 + m01, c = m32 + m02, d = m33 + m03  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.LEFT];
            final Vec3f p_n = p.n;
            p_n.set( m30 + m00,
                     m31 + m01,
                     m32 + m02 );
            p.d    = m33 + m03;
        }

        // Right:  a = m41 - m11, b = m42 - m12, c = m43 - m13, d = m44 - m14  - [1..4] column-major
        // Right:  a = m30 - m00, b = m31 - m01, c = m32 - m02, d = m33 - m03  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.RIGHT];
            final Vec3f p_n = p.n;
            p_n.set( m30 - m00,
                     m31 - m01,
                     m32 - m02 );
            p.d    = m33 - m03;
        }

        // Bottom: a = m41m21, b = m42m22, c = m43m23, d = m44m24  - [1..4] column-major
        // Bottom: a = m30m10, b = m31m11, c = m32m12, d = m33m13  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.BOTTOM];
            final Vec3f p_n = p.n;
            p_n.set( m30 + m10,
                     m31 + m11,
                     m32 + m12 );
            p.d    = m33 + m13;
        }

        // Top:   a = m41 - m21, b = m42 - m22, c = m43 - m23, d = m44 - m24  - [1..4] column-major
        // Top:   a = m30 - m10, b = m31 - m11, c = m32 - m12, d = m33 - m13  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.TOP];
            final Vec3f p_n = p.n;
            p_n.set( m30 - m10,
                     m31 - m11,
                     m32 - m12 );
            p.d    = m33 - m13;
        }

        // Near:  a = m41m31, b = m42m32, c = m43m33, d = m44m34  - [1..4] column-major
        // Near:  a = m30m20, b = m31m21, c = m32m22, d = m33m23  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.NEAR];
            final Vec3f p_n = p.n;
            p_n.set( m30 + m20,
                     m31 + m21,
                     m32 + m22 );
            p.d    = m33 + m23;
        }

        // Far:   a = m41 - m31, b = m42 - m32, c = m43 - m33, d = m44 - m34  - [1..4] column-major
        // Far:   a = m30 - m20, b = m31 - m21, c = m32m22, d = m33m23  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.FAR];
            final Vec3f p_n = p.n;
            p_n.set( m30 - m20,
                     m31 - m21,
                     m32 - m22 );
            p.d    = m33 - m23;
        }

        // Normalize all planes
        for (int i = 0; i < 6; ++i) {
            final Plane p = frustum.getPlanes()[i];
            final Vec3f p_n = p.n;
            final float invLen = 1f / p_n.length();
            p_n.scale(invLen);
            p.d *= invLen;
        }
        return frustum;
    }

    /**
     * Set this matrix to the <i>look-at</i> matrix based on given parameters.
     * <p>
     * Consist out of two matrix multiplications:
     * <pre>
     *   <b>R</b> = <b>L</b> x <b>T</b>,
     *   with <b>L</b> for <i>look-at</i> matrix and
     *        <b>T</b> for eye translation.
     *
     *   Result <b>R</b> can be utilized for <i>projection or modelview</i> multiplication, i.e.
     *          <b>M</b> = <b>M</b> x <b>R</b>,
     *          with <b>M</b> being the <i>projection or modelview</i> matrix.
     * </pre>
     * </p>
     * @param eye 3 component eye vector
     * @param center 3 component center vector
     * @param up 3 component up vector
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public Matrix4f setToLookAt(final Vec3f eye, final Vec3f center, final Vec3f up, final Matrix4f tmp) {
        // normalized forward!
        final Vec3f fwd = new Vec3f( center.x() - eye.x(),
                                     center.y() - eye.y(),
                                     center.z() - eye.z() ).normalize();

        /* Side = forward x up, normalized */
        final Vec3f side = fwd.cross(up).normalize();

        /* Recompute up as: up = side x forward */
        final Vec3f up2 = side.cross(fwd);

        m00 = side.x();
        m10 = up2.x();
        m20 = -fwd.x();
        m30 = 0;

        m01 = side.y();
        m11 = up2.y();
        m21 = -fwd.y();
        m31 = 0;

        m02 = side.z();
        m12 = up2.z();
        m22 = -fwd.z();
        m32 = 0;

        m03 = 0;
        m13 = 0;
        m23 = 0;
        m33 = 1;

        return mul( tmp.setToTranslation( -eye.x(), -eye.y(), -eye.z() ) );
    }

    /**
     * Set this matrix to the <i>pick</i> matrix based on given parameters.
     * <p>
     * Traditional <code>gluPickMatrix</code> implementation.
     * </p>
     * <p>
     * Consist out of two matrix multiplications:
     * <pre>
     *   <b>R</b> = <b>T</b> x <b>S</b>,
     *   with <b>T</b> for viewport translation matrix and
     *        <b>S</b> for viewport scale matrix.
     *
     *   Result <b>R</b> can be utilized for <i>projection</i> multiplication, i.e.
     *          <b>P</b> = <b>P</b> x <b>R</b>,
     *          with <b>P</b> being the <i>projection</i> matrix.
     * </pre>
     * </p>
     * <p>
     * To effectively use the generated pick matrix for picking,
     * call {@link #setToPick(float, float, float, float, Recti, Matrix4f) setToPick(..)}
     * and multiply a {@link #setToPerspective(float, float, float, float) custom perspective matrix}
     * by this pick matrix. Then you may load the result onto the perspective matrix stack.
     * </p>
     * @param x the center x-component of a picking region in window coordinates
     * @param y the center y-component of a picking region in window coordinates
     * @param deltaX the width of the picking region in window coordinates.
     * @param deltaY the height of the picking region in window coordinates.
     * @param viewport Rect4i viewport
     * @param mat4Tmp temp storage
     * @return this matrix for chaining or {@code null} if either delta value is <= zero.
     */
    public Matrix4f setToPick(final float x, final float y, final float deltaX, final float deltaY,
                              final Recti viewport, final Matrix4f mat4Tmp) {
        if (deltaX <= 0 || deltaY <= 0) {
            return null;
        }
        /* Translate and scale the picked region to the entire window */
        setToTranslation( ( viewport.width()  - 2 * ( x - viewport.x() ) ) / deltaX,
                          ( viewport.height() - 2 * ( y - viewport.y() ) ) / deltaY,
                          0);
        mat4Tmp.setToScale( viewport.width() / deltaX, viewport.height() / deltaY, 1.0f );
        return mul(mat4Tmp);
    }

    //
    // Matrix affine operations using setTo..()
    //

    /**
     * Rotate this matrix about give axis and angle in radians, i.e. multiply by {@link #setToRotationAxis(float, float, float, float) axis-rotation matrix}.
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
     * @param angrad angle in radians
     * @param x x of rotation axis
     * @param y y of rotation axis
     * @param z z of rotation axis
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f rotate(final float ang_rad, final float x, final float y, final float z, final Matrix4f tmp) {
        return mul( tmp.setToRotationAxis(ang_rad, x, y, z) );
    }

    /**
     * Rotate this matrix about give axis and angle in radians, i.e. multiply by {@link #setToRotationAxis(float, Vec3f) axis-rotation matrix}.
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q38">Matrix-FAQ Q38</a>
     * @param angrad angle in radians
     * @param axis rotation axis
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f rotate(final float ang_rad, final Vec3f axis, final Matrix4f tmp) {
        return mul( tmp.setToRotationAxis(ang_rad, axis) );
    }

    /**
     * Rotate this matrix with the given {@link Quaternion}, i.e. multiply by {@link #setToRotation(Quaternion) Quaternion's rotation matrix}.
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f rotate(final Quaternion quat, final Matrix4f tmp) {
        return mul( tmp.setToRotation(quat) );
    }

    /**
     * Translate this matrix, i.e. multiply by {@link #setToTranslation(float, float, float) translation matrix}.
     * @param x x translation
     * @param y y translation
     * @param z z translation
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f translate(final float x, final float y, final float z, final Matrix4f tmp) {
        return mul( tmp.setToTranslation(x, y, z) );
    }

    /**
     * Translate this matrix, i.e. multiply by {@link #setToTranslation(Vec3f) translation matrix}.
     * @param t translation Vec3f
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f translate(final Vec3f t, final Matrix4f tmp) {
        return mul( tmp.setToTranslation(t) );
    }

    /**
     * Scale this matrix, i.e. multiply by {@link #setToScale(float, float, float) scale matrix}.
     * @param x x scale
     * @param y y scale
     * @param z z scale
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f scale(final float x, final float y, final float z, final Matrix4f tmp) {
        return mul( tmp.setToScale(x, y, z) );
    }

    /**
     * Scale this matrix, i.e. multiply by {@link #setToScale(float, float, float) scale matrix}.
     * @param s scale for x-, y- and z-axis
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4f scale(final float s, final Matrix4f tmp) {
        return mul( tmp.setToScale(s, s, s) );
    }

    //
    // Matrix Stack
    //

    /**
     * Push the matrix to it's stack, while preserving this matrix values.
     * @see #pop()
     */
    public final void push() {
        stack.push(this);
    }

    /**
     * Pop the current matrix from it's stack, replacing this matrix values.
     * @see #push()
     */
    public final void pop() {
        stack.pop(this);
    }

    //
    // equals
    //

    /**
     * Equals check using a given {@link FloatUtil#EPSILON} value and {@link FloatUtil#isEqual(float, float, float)}.
     * <p>
     * Implementation considers following corner cases:
     * <ul>
     *    <li>NaN == NaN</li>
     *    <li>+Inf == +Inf</li>
     *    <li>-Inf == -Inf</li>
     * </ul>
     * @param o comparison value
     * @param epsilon consider using {@link FloatUtil#EPSILON}
     * @return true if all components differ less than {@code epsilon}, otherwise false.
     */
    public boolean isEqual(final Matrix4f o, final float epsilon) {
        if( this == o ) {
            return true;
        } else {
            return FloatUtil.isEqual(m00, o.m00, epsilon) &&
                   FloatUtil.isEqual(m01, o.m01, epsilon) &&
                   FloatUtil.isEqual(m02, o.m02, epsilon) &&
                   FloatUtil.isEqual(m03, o.m03, epsilon) &&
                   FloatUtil.isEqual(m10, o.m10, epsilon) &&
                   FloatUtil.isEqual(m11, o.m11, epsilon) &&
                   FloatUtil.isEqual(m12, o.m12, epsilon) &&
                   FloatUtil.isEqual(m13, o.m13, epsilon) &&
                   FloatUtil.isEqual(m20, o.m20, epsilon) &&
                   FloatUtil.isEqual(m21, o.m21, epsilon) &&
                   FloatUtil.isEqual(m22, o.m22, epsilon) &&
                   FloatUtil.isEqual(m23, o.m23, epsilon) &&
                   FloatUtil.isEqual(m30, o.m30, epsilon) &&
                   FloatUtil.isEqual(m31, o.m31, epsilon) &&
                   FloatUtil.isEqual(m32, o.m32, epsilon) &&
                   FloatUtil.isEqual(m33, o.m33, epsilon);
        }
    }

    /**
     * Equals check using {@link FloatUtil#EPSILON} value and {@link FloatUtil#isEqual(float, float, float)}.
     * <p>
     * Implementation considers following corner cases:
     * <ul>
     *    <li>NaN == NaN</li>
     *    <li>+Inf == +Inf</li>
     *    <li>-Inf == -Inf</li>
     * </ul>
     * @param o comparison value
     * @return true if all components differ less than {@link FloatUtil#EPSILON}, otherwise false.
     */
    public boolean isEqual(final Matrix4f o) {
        return isEqual(o, FloatUtil.EPSILON);
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Matrix4f ) {
            return isEqual((Matrix4f)o, FloatUtil.EPSILON);
        } else {
            return false;
        }
    }

    //
    // Static multi Matrix ops
    //

    /**
     * Map object coordinates to window coordinates.
     * <p>
     * Traditional <code>gluProject</code> implementation.
     * </p>
     *
     * @param obj object position, 3 component vector
     * @param mMv modelview matrix
     * @param mP projection matrix
     * @param viewport Rect4i viewport
     * @param winPos 3 component window coordinate, the result
     * @return true if successful, otherwise false (z is 1)
     */
    public static boolean mapObjToWin(final Vec3f obj, final Matrix4f mMv, final Matrix4f mP,
                                      final Recti viewport, final Vec3f winPos)
    {
        final Vec4f vec4Tmp1 = new Vec4f(obj, 1f);

        // vec4Tmp2 = Mv * o
        // rawWinPos = P  * vec4Tmp2
        // rawWinPos = P * ( Mv * o )
        // rawWinPos = P * Mv * o
        final Vec4f vec4Tmp2 = mMv.mulVec4f(vec4Tmp1, new Vec4f());
        final Vec4f rawWinPos = mP.mulVec4f(vec4Tmp2, vec4Tmp1);

        if (rawWinPos.w() == 0.0f) {
            return false;
        }

        final float s = ( 1.0f / rawWinPos.w() ) * 0.5f;

        // Map x, y and z to range 0-1 (w is ignored)
        rawWinPos.scale(s).add(0.5f, 0.5f, 0.5f, 0f);

        // Map x,y to viewport
        winPos.set( rawWinPos.x() * viewport.width() +  viewport.x(),
                    rawWinPos.y() * viewport.height() + viewport.y(),
                    rawWinPos.z() );

        return true;
    }

    /**
     * Map object coordinates to window coordinates.
     * <p>
     * Traditional <code>gluProject</code> implementation.
     * </p>
     *
     * @param obj object position, 3 component vector
     * @param mPMv [projection] x [modelview] matrix, i.e. P x Mv
     * @param viewport Rect4i viewport
     * @param winPos 3 component window coordinate, the result
     * @return true if successful, otherwise false (z is 1)
     */
    public static boolean mapObjToWin(final Vec3f obj, final Matrix4f mPMv,
                                      final Recti viewport, final Vec3f winPos)
    {
        final Vec4f vec4Tmp2 = new Vec4f(obj, 1f);

        // rawWinPos = P * Mv * o
        final Vec4f rawWinPos = mPMv.mulVec4f(vec4Tmp2, new Vec4f());

        if (rawWinPos.w() == 0.0f) {
            return false;
        }

        final float s = ( 1.0f / rawWinPos.w() ) * 0.5f;

        // Map x, y and z to range 0-1 (w is ignored)
        rawWinPos.scale(s).add(0.5f, 0.5f, 0.5f, 0f);

        // Map x,y to viewport
        winPos.set( rawWinPos.x() * viewport.width() +  viewport.x(),
                    rawWinPos.y() * viewport.height() + viewport.y(),
                    rawWinPos.z() );

        return true;
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param mMv 4x4 modelview matrix
     * @param mP 4x4 projection matrix
     * @param viewport Rect4i viewport
     * @param objPos 3 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz,
                                      final Matrix4f mMv, final Matrix4f mP,
                                      final Recti viewport,
                                      final Vec3f objPos,
                                      final Matrix4f mat4Tmp)
    {
        // invPMv = Inv(P x Mv)
        final Matrix4f invPMv = mat4Tmp.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        final Vec4f winPos = new Vec4f(winx, winy, winz, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport.x(), -viewport.y(), 0f, 0f).mul(1f/viewport.width(), 1f/viewport.height(), 1f, 1f);

        // Map to range -1 to 1
        winPos.mul(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

        // rawObjPos = Inv(P x Mv) *  winPos
        final Vec4f rawObjPos = invPMv.mulVec4f(winPos, new Vec4f());

        if ( rawObjPos.w() == 0.0f ) {
            return false;
        }
        objPos.set( rawObjPos.scale( 1f / rawObjPos.w() ) );

        return true;
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv), if null method returns false
     * @param viewport Rect4i viewport
     * @param objPos 3 component object coordinate, the result
     * @return true if successful, otherwise false (null invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz,
                                      final Matrix4f invPMv,
                                      final Recti viewport,
                                      final Vec3f objPos)
    {
        if( null == invPMv ) {
            return false;
        }
        final Vec4f winPos = new Vec4f(winx, winy, winz, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport.x(), -viewport.y(), 0f, 0f).mul(1f/viewport.width(), 1f/viewport.height(), 1f, 1f);

        // Map to range -1 to 1
        winPos.mul(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

        // rawObjPos = Inv(P x Mv) *  winPos
        final Vec4f rawObjPos = invPMv.mulVec4f(winPos, new Vec4f());

        if ( rawObjPos.w() == 0.0f ) {
            return false;
        }
        objPos.set( rawObjPos.scale( 1f / rawObjPos.w() ) );

        return true;
    }

    /**
     * Map two window coordinates to two object coordinates,
     * distinguished by their z component.
     * <p>
     * Traditional <code>gluUnProject</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz1
     * @param winz2
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv), if null method returns false
     * @param viewport Rect4i viewport vector
     * @param objPos1 3 component object coordinate, the result
     * @return true if successful, otherwise false (null invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz1, final float winz2,
                                      final Matrix4f invPMv,
                                      final Recti viewport,
                                      final Vec3f objPos1, final Vec3f objPos2)
    {
        if( null == invPMv ) {
            return false;
        }
        final Vec4f winPos = new Vec4f(winx, winy, winz1, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport.x(), -viewport.y(), 0f, 0f).mul(1f/viewport.width(), 1f/viewport.height(), 1f, 1f);

        // Map to range -1 to 1
        winPos.mul(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

        // rawObjPos = Inv(P x Mv) *  winPos1
        final Vec4f rawObjPos = invPMv.mulVec4f(winPos, new Vec4f());

        if ( rawObjPos.w() == 0.0f ) {
            return false;
        }
        objPos1.set( rawObjPos.scale( 1f / rawObjPos.w() ) );

        //
        // winz2
        //
        // Map Z to range -1 to 1
        winPos.setZ( winz2 * 2f - 1f );

        // rawObjPos = Inv(P x Mv) *  winPos2
        invPMv.mulVec4f(winPos, rawObjPos);

        if ( rawObjPos.w() == 0.0f ) {
            return false;
        }
        objPos2.set( rawObjPos.scale( 1f / rawObjPos.w() ) );

        return true;
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject4</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param clipw
     * @param mMv 4x4 modelview matrix
     * @param mP 4x4 projection matrix
     * @param viewport Rect4i viewport vector
     * @param near
     * @param far
     * @param obj_pos 4 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj4(final float winx, final float winy, final float winz, final float clipw,
                                       final Matrix4f mMv, final Matrix4f mP,
                                       final Recti viewport,
                                       final float near, final float far,
                                       final Vec4f objPos,
                                       final Matrix4f mat4Tmp)
    {
        // invPMv = Inv(P x Mv)
        final Matrix4f invPMv = mat4Tmp.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        final Vec4f winPos = new Vec4f(winx, winy, winz, clipw);

        // Map x and y from window coordinates
        winPos.add(-viewport.x(), -viewport.y(), -near, 0f).mul(1f/viewport.width(), 1f/viewport.height(), 1f/(far-near), 1f);

        // Map to range -1 to 1
        winPos.mul(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

        // objPos = Inv(P x Mv) *  winPos
        invPMv.mulVec4f(winPos, objPos);

        if ( objPos.w() == 0.0f ) {
            return false;
        }
        return true;
    }

    /**
     * Map window coordinates to object coordinates.
     * <p>
     * Traditional <code>gluUnProject4</code> implementation.
     * </p>
     *
     * @param winx
     * @param winy
     * @param winz
     * @param clipw
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv), if null method returns false
     * @param viewport Rect4i viewport vector
     * @param near
     * @param far
     * @param obj_pos 4 component object coordinate, the result
     * @return true if successful, otherwise false (null invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj4(final float winx, final float winy, final float winz, final float clipw,
                                       final Matrix4f invPMv,
                                       final Recti viewport,
                                       final float near, final float far,
                                       final Vec4f objPos)
    {
        if( null == invPMv ) {
            return false;
        }
        final Vec4f winPos = new Vec4f(winx, winy, winz, clipw);

        // Map x and y from window coordinates
        winPos.add(-viewport.x(), -viewport.y(), -near, 0f).mul(1f/viewport.width(), 1f/viewport.height(), 1f/(far-near), 1f);

        // Map to range -1 to 1
        winPos.mul(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

        // objPos = Inv(P x Mv) *  winPos
        invPMv.mulVec4f(winPos, objPos);

        if ( objPos.w() == 0.0f ) {
            return false;
        }
        return true;
    }

    /**
     * Map two window coordinates w/ shared X/Y and distinctive Z
     * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
     * using a {@link AABBox#getRayIntersection(Vec3f, Ray, float, boolean)}.
     * <p>
     * Notes for picking <i>winz0</i> and <i>winz1</i>:
     * <ul>
     *   <li>see {@link FloatUtil#getZBufferEpsilon(int, float, float)}</li>
     *   <li>see {@link FloatUtil#getZBufferValue(int, float, float, float)}</li>
     *   <li>see {@link FloatUtil#getOrthoWinZ(float, float, float)}</li>
     * </ul>
     * </p>
     * @param winx
     * @param winy
     * @param winz0
     * @param winz1
     * @param mMv 4x4 modelview matrix
     * @param mP 4x4 projection matrix
     * @param viewport Rect4i viewport
     * @param ray storage for the resulting {@link Ray}
     * @param mat4Tmp1 16 component matrix for temp storage
     * @param mat4Tmp2 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
     */
    public static boolean mapWinToRay(final float winx, final float winy, final float winz0, final float winz1,
                                      final Matrix4f mMv, final Matrix4f mP,
                                      final Recti viewport,
                                      final Ray ray,
                                      final Matrix4f mat4Tmp1, final Matrix4f mat4Tmp2) {
        // invPMv = Inv(P x Mv)
        final Matrix4f invPMv = mat4Tmp1.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        if( mapWinToObj(winx, winy, winz0, winz1, invPMv, viewport, ray.orig, ray.dir) ) {
            ray.dir.sub(ray.orig).normalize();
            return true;
        } else {
            return false;
        }
    }

    /**
     * Map two window coordinates w/ shared X/Y and distinctive Z
     * to a {@link Ray}. The resulting {@link Ray} maybe used for <i>picking</i>
     * using a {@link AABBox#getRayIntersection(Vec3f, Ray, float, boolean)}.
     * <p>
     * Notes for picking <i>winz0</i> and <i>winz1</i>:
     * <ul>
     *   <li>see {@link FloatUtil#getZBufferEpsilon(int, float, float)}</li>
     *   <li>see {@link FloatUtil#getZBufferValue(int, float, float, float)}</li>
     *   <li>see {@link FloatUtil#getOrthoWinZ(float, float, float)}</li>
     * </ul>
     * </p>
     * @param winx
     * @param winy
     * @param winz0
     * @param winz1
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv), if null method returns false
     * @param viewport Rect4i viewport
     * @param ray storage for the resulting {@link Ray}
     * @return true if successful, otherwise false (null invert matrix, or becomes z is infinity)
     */
    public static boolean mapWinToRay(final float winx, final float winy, final float winz0, final float winz1,
                                      final Matrix4f invPMv,
                                      final Recti viewport,
                                      final Ray ray) {
        if( mapWinToObj(winx, winy, winz0, winz1, invPMv, viewport, ray.orig, ray.dir) ) {
            ray.dir.sub(ray.orig).normalize();
            return true;
        } else {
            return false;
        }
    }

    //
    // String and internals
    //

    /**
     * @param sb optional passed StringBuilder instance to be used
     * @param rowPrefix optional prefix for each row
     * @param f the format string of one floating point, i.e. "%10.5f", see {@link java.util.Formatter}
     * @return matrix string representation
     */
    public StringBuilder toString(final StringBuilder sb, final String rowPrefix, final String f) {
        final float[] tmp = new float[16];
        this.get(tmp);
        return FloatUtil.matrixToString(sb, rowPrefix, f,tmp, 0, 4, 4, false /* rowMajorOrder */);
    }

    @Override
    public String toString() {
        return toString(null, null, "%10.5f").toString();
    }

    private float m00, m10, m20, m30;
    private float m01, m11, m21, m31;
    private float m02, m12, m22, m32;
    private float m03, m13, m23, m33;

    final Stack stack = new Stack(0, 16*16); // start w/ zero size, growSize is half GL-min size (32)

    private static class Stack {
        private int position;
        private float[] buffer;
        private final int growSize;

        /**
         * @param initialSize initial size
         * @param growSize grow size if {@link #position()} is reached, maybe <code>0</code>
         *        in which case an {@link IndexOutOfBoundsException} is thrown.
         */
        public Stack(final int initialSize, final int growSize) {
            this.position = 0;
            this.growSize = growSize;
            this.buffer = new float[initialSize];
        }

        private final void growIfNecessary(final int length) throws IndexOutOfBoundsException {
            if( position + length > buffer.length ) {
                if( 0 >= growSize ) {
                    throw new IndexOutOfBoundsException("Out of fixed stack size: "+this);
                }
                final float[] newBuffer =
                        new float[buffer.length + growSize];
                System.arraycopy(buffer, 0, newBuffer, 0, position);
                buffer = newBuffer;
            }
        }

        public final Matrix4f push(final Matrix4f src) throws IndexOutOfBoundsException {
            growIfNecessary(16);
            src.get(buffer, position);
            position += 16;
            return src;
        }

        public final Matrix4f pop(final Matrix4f dest) throws IndexOutOfBoundsException {
            position -= 16;
            dest.load(buffer, position);
            return dest;
        }
    }
}
