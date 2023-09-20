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

package com.jogamp.opengl.test.junit.math;

import java.nio.FloatBuffer;

import com.jogamp.math.FloatUtil;
import com.jogamp.math.FovHVHalves;
import com.jogamp.math.Quaternion;
import com.jogamp.math.Ray;
import com.jogamp.math.Vec3f;
import com.jogamp.math.Vec4f;
import com.jogamp.math.VectorUtil;
import com.jogamp.math.geom.AABBox;
import com.jogamp.math.geom.Frustum;
import com.jogamp.math.geom.Frustum.Plane;

/**
 * Basic 4x4 float matrix implementation using fields for intensive use-cases (host operations).
 * <p>
 * Implementation covers {@link FloatUtil} matrix functionality, exposed in an object oriented manner.
 * </p>
 * <p>
 * Unlike {@link com.jogamp.opengl.util.PMVMatrix PMVMatrix}, this class only represents one single matrix
 * without a complete {@link com.jogamp.opengl.fixedfunc.GLMatrixFunc GLMatrixFunc} implementation.
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
         m[0+3*4] = tx;                  m[0+3*4] = tx;
         m[1+3*4] = ty;                  m[1+3*4] = ty;
         m[2+3*4] = tz;                  m[2+3*4] = tz;

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
 * @see com.jogamp.opengl.util.PMVMatrix
 * @see FloatUtil
 */
public class Matrix4fb {

    /**
     * Creates a new identity matrix.
     */
    public Matrix4fb() {
        loadIdentity();
    }

    /**
     * Creates a new matrix copying the values of the given {@code src} matrix.
     */
    public Matrix4fb(final Matrix4fb src) {
        load(src);
    }

    /**
     * Creates a new matrix based on given float[4*4] column major order.
     * @param m 4x4 matrix in column-major order
     */
    public Matrix4fb(final float[] m) {
        load(m);
    }

    /**
     * Creates a new matrix based on given float[4*4] column major order.
     * @param m 4x4 matrix in column-major order
     * @param m_off offset for matrix {@code m}
     */
    public Matrix4fb(final float[] m, final int m_off) {
        load(m, m_off);
    }

    //
    // Write to Matrix via load(..)
    //

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
    public final Matrix4fb loadIdentity() {
       m[0+0*4] = m[1+1*4] = m[2+2*4] = m[3+3*4] = 1.0f;
       m[0+1*4] = m[0+2*4] = m[0+3*4] =
       m[1+0*4] = m[1+2*4] = m[1+3*4] =
       m[2+0*4] = m[2+1*4] = m[2+3*4] =
       m[3+0*4] = m[3+1*4] = m[3+2*4] = 0.0f;
       return this;
    }

    /**
     * Load the values of the given matrix {@code b} to this matrix.
     * @param src the source values
     * @return this matrix for chaining
     */
    public Matrix4fb load(final Matrix4fb src) {
        System.arraycopy(src.m, 0, m, 0, 16);
        return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * @param src 4x4 matrix float[16] in column-major order
     * @return this matrix for chaining
     */
    public Matrix4fb load(final float[] src) {
        System.arraycopy(src, 0, m, 0, 16);
        return this;
    }

    /**
     * Load the values of the given matrix {@code src} to this matrix.
     * @param src 4x4 matrix float[16] in column-major order
     * @param src_off offset for matrix {@code src}
     * @return this matrix for chaining
     */
    public Matrix4fb load(final float[] src, final int src_off) {
        System.arraycopy(src, src_off, m, 0, 16);
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
    public Matrix4fb load(final FloatBuffer src) {
        src.get(m, 0, 16);
        return this;
    }

    //
    // Read out Matrix via get(..)
    //

    /** Gets the ith component, 0 <= i < 16 */
    public float get(final int i) {
        return m[i];
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
        System.arraycopy(m, 0, dst, dst_off, 16);
        return dst;
    }

    /**
     * Get this matrix into the given float[16] array in column major order.
     *
     * @param dst float[16] array storage in column major order
     * @return {@code dst} for chaining
     */
    public float[] get(final float[] dst) {
        System.arraycopy(m, 0, dst, 0, 16);
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
        dst.put(m, 0, 16);
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
        ret += m[0+0*4] * ( + m[1+1*4]*(m[2+2*4]*m[3+3*4] - m[2+3*4]*m[3+2*4]) - m[1+2*4]*(m[2+1*4]*m[3+3*4] - m[2+3*4]*m[3+1*4]) + m[1+3*4]*(m[2+1*4]*m[3+2*4] - m[2+2*4]*m[3+1*4]));
        ret -= m[0+1*4] * ( + m[1+0*4]*(m[2+2*4]*m[3+3*4] - m[2+3*4]*m[3+2*4]) - m[1+2*4]*(m[2+0*4]*m[3+3*4] - m[2+3*4]*m[3+0*4]) + m[1+3*4]*(m[2+0*4]*m[3+2*4] - m[2+2*4]*m[3+0*4]));
        ret += m[0+2*4] * ( + m[1+0*4]*(m[2+1*4]*m[3+3*4] - m[2+3*4]*m[3+1*4]) - m[1+1*4]*(m[2+0*4]*m[3+3*4] - m[2+3*4]*m[3+0*4]) + m[1+3*4]*(m[2+0*4]*m[3+1*4] - m[2+1*4]*m[3+0*4]));
        ret -= m[0+3*4] * ( + m[1+0*4]*(m[2+1*4]*m[3+2*4] - m[2+2*4]*m[3+1*4]) - m[1+1*4]*(m[2+0*4]*m[3+2*4] - m[2+2*4]*m[3+0*4]) + m[1+2*4]*(m[2+0*4]*m[3+1*4] - m[2+1*4]*m[3+0*4]));
        return ret;
    }

    /**
     * Transpose this matrix.
     *
     * @return this matrix for chaining
     */
    public final Matrix4fb transpose() {
        float tmp;

        tmp = m[1+0*4];
        m[1+0*4] = m[0+1*4];
        m[0+1*4] = tmp;

        tmp = m[2+0*4];
        m[2+0*4] = m[0+2*4];
        m[0+2*4] = tmp;

        tmp = m[3+0*4];
        m[3+0*4] = m[0+3*4];
        m[0+3*4] = tmp;

        tmp = m[2+1*4];
        m[2+1*4] = m[1+2*4];
        m[1+2*4] = tmp;

        tmp = m[3+1*4];
        m[3+1*4] = m[1+3*4];
        m[1+3*4] = tmp;

        tmp = m[3+2*4];
        m[3+2*4] = m[2+3*4];
        m[2+3*4] = tmp;

        return this;
    }

    /**
     * Transpose the given {@code src} matrix into this matrix.
     *
     * @param src source 4x4 matrix
     * @return this matrix (result) for chaining
     */
    public final Matrix4fb transpose(final Matrix4fb src) {
        if( src == this ) {
            return transpose();
        }
        m[0+0*4] = src.m[0+0*4];
        m[1+0*4] = src.m[0+1*4];
        m[2+0*4] = src.m[0+2*4];
        m[3+0*4] = src.m[0+3*4];

        m[0+1*4] = src.m[1+0*4];
        m[1+1*4] = src.m[1+1*4];
        m[2+1*4] = src.m[1+2*4];
        m[3+1*4] = src.m[1+3*4];

        m[0+2*4] = src.m[2+0*4];
        m[1+2*4] = src.m[2+1*4];
        m[2+2*4] = src.m[2+2*4];
        m[3+2*4] = src.m[2+3*4];

        m[0+3*4] = src.m[3+0*4];
        m[1+3*4] = src.m[3+1*4];
        m[2+3*4] = src.m[3+2*4];
        m[3+3*4] = src.m[3+3*4];
        return this;
    }

    /**
     * Invert this matrix.
     * @return false if this matrix is singular and inversion not possible, otherwise true
     */
    public boolean invert() {
        final float scale;
        {
            float max = Math.abs(m[0]);

            for( int i = 1; i < 16; i++ ) {
                final float a = Math.abs(m[i]);
                if( a > max ) max = a;
            }
            if( 0 == max ) {
                return false;
            }
            scale = 1.0f/max;
        }

        final float a00 = m[0+0*4]*scale;
        final float a10 = m[1+0*4]*scale;
        final float a20 = m[2+0*4]*scale;
        final float a30 = m[3+0*4]*scale;

        final float a01 = m[0+1*4]*scale;
        final float a11 = m[1+1*4]*scale;
        final float a21 = m[2+1*4]*scale;
        final float a31 = m[3+1*4]*scale;

        final float a02 = m[0+2*4]*scale;
        final float a12 = m[1+2*4]*scale;
        final float a22 = m[2+2*4]*scale;
        final float a32 = m[3+2*4]*scale;

        final float a03 = m[0+3*4]*scale;
        final float a13 = m[1+3*4]*scale;
        final float a23 = m[2+3*4]*scale;
        final float a33 = m[3+3*4]*scale;

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

        m[0+0*4] = b00 * invdet;
        m[1+0*4] = b01 * invdet;
        m[2+0*4] = b02 * invdet;
        m[3+0*4] = b03 * invdet;

        m[0+1*4] = b10 * invdet;
        m[1+1*4] = b11 * invdet;
        m[2+1*4] = b12 * invdet;
        m[3+1*4] = b13 * invdet;

        m[0+2*4] = b20 * invdet;
        m[1+2*4] = b21 * invdet;
        m[2+2*4] = b22 * invdet;
        m[3+2*4] = b23 * invdet;

        m[0+3*4] = b30 * invdet;
        m[1+3*4] = b31 * invdet;
        m[2+3*4] = b32 * invdet;
        m[3+3*4] = b33 * invdet;
        return true;
    }

    /**
     * Invert the {@code src} matrix values into this matrix
     * @param src the source matrix, which values are to be inverted
     * @return false if {@code src} matrix is singular and inversion not possible, otherwise true
     */
    public boolean invert(final Matrix4fb src) {
        final float scale;
        {
            float max = Math.abs(src.m[0]);

            for( int i = 1; i < 16; i++ ) {
                final float a = Math.abs(src.m[i]);
                if( a > max ) max = a;
            }
            if( 0 == max ) {
                return false;
            }
            scale = 1.0f/max;
        }

        final float a00 = src.m[0+0*4]*scale;
        final float a10 = src.m[1+0*4]*scale;
        final float a20 = src.m[2+0*4]*scale;
        final float a30 = src.m[3+0*4]*scale;

        final float a01 = src.m[0+1*4]*scale;
        final float a11 = src.m[1+1*4]*scale;
        final float a21 = src.m[2+1*4]*scale;
        final float a31 = src.m[3+1*4]*scale;

        final float a02 = src.m[0+2*4]*scale;
        final float a12 = src.m[1+2*4]*scale;
        final float a22 = src.m[2+2*4]*scale;
        final float a32 = src.m[3+2*4]*scale;

        final float a03 = src.m[0+3*4]*scale;
        final float a13 = src.m[1+3*4]*scale;
        final float a23 = src.m[2+3*4]*scale;
        final float a33 = src.m[3+3*4]*scale;

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

        m[0+0*4] = b00 * invdet;
        m[1+0*4] = b01 * invdet;
        m[2+0*4] = b02 * invdet;
        m[3+0*4] = b03 * invdet;

        m[0+1*4] = b10 * invdet;
        m[1+1*4] = b11 * invdet;
        m[2+1*4] = b12 * invdet;
        m[3+1*4] = b13 * invdet;

        m[0+2*4] = b20 * invdet;
        m[1+2*4] = b21 * invdet;
        m[2+2*4] = b22 * invdet;
        m[3+2*4] = b23 * invdet;

        m[0+3*4] = b30 * invdet;
        m[1+3*4] = b31 * invdet;
        m[2+3*4] = b32 * invdet;
        m[3+3*4] = b33 * invdet;
        return true;
    }

    /**
     * Multiply matrix: [this] = [this] x [b]
     * <p>
     * Roughly 15% slower than {@link #mul(Matrix4fb, Matrix4fb)}
     * Roughly  3% slower than {@link FloatUtil#multMatrix(float[], float[])}
     * </p>
     * @param b 4x4 matrix
     * @return this matrix for chaining
     * @see #mul(Matrix4fb, Matrix4fb)
     */
    public final Matrix4fb mul(final Matrix4fb b) {
        final float b00 = b.m[0+0*4];
        final float b10 = b.m[1+0*4];
        final float b20 = b.m[2+0*4];
        final float b30 = b.m[3+0*4];
        final float b01 = b.m[0+1*4];
        final float b11 = b.m[1+1*4];
        final float b21 = b.m[2+1*4];
        final float b31 = b.m[3+1*4];
        final float b02 = b.m[0+2*4];
        final float b12 = b.m[1+2*4];
        final float b22 = b.m[2+2*4];
        final float b32 = b.m[3+2*4];
        final float b03 = b.m[0+3*4];
        final float b13 = b.m[1+3*4];
        final float b23 = b.m[2+3*4];
        final float b33 = b.m[3+3*4];

        float ai0=m[0+0*4]; // row-0, m[0+0*4]
        float ai1=m[0+1*4];
        float ai2=m[0+2*4];
        float ai3=m[0+3*4];
        m[0+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[0+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[0+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[0+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=m[1+0*4]; //row-1, m[1+0*4]
        ai1=m[1+1*4];
        ai2=m[1+2*4];
        ai3=m[1+3*4];
        m[1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=m[2+0*4]; // row-2, m[2+0*4]
        ai1=m[2+1*4];
        ai2=m[2+2*4];
        ai3=m[2+3*4];
        m[2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=m[3+0*4]; // row-3, m[3+0*4]
        ai1=m[3+1*4];
        ai2=m[3+2*4];
        ai3=m[3+3*4];
        m[3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;
        return this;
    }

    /**
     * Multiply matrix: [this] = [a] x [b]
     * <p>
     * Roughly 13% faster than {@link #mul(Matrix4fb)}
     * Roughly 11% faster than {@link FloatUtil#multMatrix(float[], float[])}
     * </p>
     * @param a 4x4 matrix
     * @param b 4x4 matrix
     * @return this matrix for chaining
     * @see #mul(Matrix4fb)
     */
    public final Matrix4fb mul(final Matrix4fb a, final Matrix4fb b) {
        final float b00 = b.m[0+0*4];
        final float b10 = b.m[1+0*4];
        final float b20 = b.m[2+0*4];
        final float b30 = b.m[3+0*4];
        final float b01 = b.m[0+1*4];
        final float b11 = b.m[1+1*4];
        final float b21 = b.m[2+1*4];
        final float b31 = b.m[3+1*4];
        final float b02 = b.m[0+2*4];
        final float b12 = b.m[1+2*4];
        final float b22 = b.m[2+2*4];
        final float b32 = b.m[3+2*4];
        final float b03 = b.m[0+3*4];
        final float b13 = b.m[1+3*4];
        final float b23 = b.m[2+3*4];
        final float b33 = b.m[3+3*4];

        float ai0=a.m[0+0*4]; // row-0, m[0+0*4]
        float ai1=a.m[0+1*4];
        float ai2=a.m[0+2*4];
        float ai3=a.m[0+3*4];
        m[0+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[0+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[0+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[0+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=a.m[1+0*4]; //row-1, m[1+0*4]
        ai1=a.m[1+1*4];
        ai2=a.m[1+2*4];
        ai3=a.m[1+3*4];
        m[1+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[1+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[1+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[1+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=a.m[2+0*4]; // row-2, m[2+0*4]
        ai1=a.m[2+1*4];
        ai2=a.m[2+2*4];
        ai3=a.m[2+3*4];
        m[2+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[2+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[2+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[2+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;

        ai0=a.m[3+0*4]; // row-3, m[3+0*4]
        ai1=a.m[3+1*4];
        ai2=a.m[3+2*4];
        ai3=a.m[3+3*4];
        m[3+0*4] = ai0 * b00  +  ai1 * b10  +  ai2 * b20  +  ai3 * b30 ;
        m[3+1*4] = ai0 * b01  +  ai1 * b11  +  ai2 * b21  +  ai3 * b31 ;
        m[3+2*4] = ai0 * b02  +  ai1 * b12  +  ai2 * b22  +  ai3 * b32 ;
        m[3+3*4] = ai0 * b03  +  ai1 * b13  +  ai2 * b23  +  ai3 * b33 ;
        return this;
    }

    /**
     * @param v_in 4-component column-vector
     * @param v_out this * v_in
     * @returns v_out for chaining
     */
    public final float[] mulVec4f(final float[/*4*/] v_in, final float[/*4*/] v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in[0], y = v_in[1], z = v_in[2], w = v_in[3];
        v_out[0] = x * m[0+0*4] + y * m[0+1*4] + z * m[0+2*4] + w * m[0+3*4];
        v_out[1] = x * m[1+0*4] + y * m[1+1*4] + z * m[1+2*4] + w * m[1+3*4];
        v_out[2] = x * m[2+0*4] + y * m[2+1*4] + z * m[2+2*4] + w * m[2+3*4];
        v_out[3] = x * m[3+0*4] + y * m[3+1*4] + z * m[3+2*4] + w * m[3+3*4];
        return v_out;
    }

    /**
     * @param v_in 4-component column-vector
     * @param v_out this * v_in
     * @returns v_out for chaining
     */
    public final Vec4f mulVec4f(final Vec4f v_in, final Vec4f v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in.x(), y = v_in.y(), z = v_in.z(), w = v_in.w();
        v_out.set( x * m[0+0*4] + y * m[0+1*4] + z * m[0+2*4] + w * m[0+3*4],
                   x * m[1+0*4] + y * m[1+1*4] + z * m[1+2*4] + w * m[1+3*4],
                   x * m[2+0*4] + y * m[2+1*4] + z * m[2+2*4] + w * m[2+3*4],
                   x * m[3+0*4] + y * m[3+1*4] + z * m[3+2*4] + w * m[3+3*4] );
        return v_out;
    }

    /**
     * Affine 3f-vector transformation by 4x4 matrix
     *
     * 4x4 matrix multiplication with 3-component vector,
     * using {@code 1} for for {@code v_in[3]} and dropping {@code v_out[3]},
     * which shall be {@code 1}.
     *
     * @param v_in 3-component column-vector
     * @param v_out m_in * v_in, 3-component column-vector
     * @returns v_out for chaining
     */
    public final float[] mulVec3f(final float[/*3*/] v_in, final float[/*3*/] v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in[0], y = v_in[1], z = v_in[2];
        v_out[0] = x * m[0+0*4] + y * m[0+1*4] + z * m[0+2*4] + 1f * m[0+3*4];
        v_out[1] = x * m[1+0*4] + y * m[1+1*4] + z * m[1+2*4] + 1f * m[1+3*4];
        v_out[2] = x * m[2+0*4] + y * m[2+1*4] + z * m[2+2*4] + 1f * m[2+3*4];
        return v_out;
    }

    /**
     * Affine 3f-vector transformation by 4x4 matrix
     *
     * 4x4 matrix multiplication with 3-component vector,
     * using {@code 1} for for {@code v_in.w()} and dropping {@code v_out.w()},
     * which shall be {@code 1}.
     *
     * @param v_in 3-component column-vector {@link Vec3f}
     * @param v_out m_in * v_in, 3-component column-vector {@link Vec3f}
     * @returns v_out for chaining
     */
    public final Vec3f mulVec3f(final Vec3f v_in, final Vec3f v_out) {
        // (one matrix row in column-major order) X (column vector)
        final float x = v_in.x(), y = v_in.y(), z = v_in.z();
        v_out.set( x * m[0+0*4] + y * m[0+1*4] + z * m[0+2*4] + 1f * m[0+3*4],
                   x * m[1+0*4] + y * m[1+1*4] + z * m[1+2*4] + 1f * m[1+3*4],
                   x * m[2+0*4] + y * m[2+1*4] + z * m[2+2*4] + 1f * m[2+3*4] );
        return v_out;
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
    public final Matrix4fb setToTranslation(final float x, final float y, final float z) {
        m[0+0*4] = m[1+1*4] = m[2+2*4] = m[3+3*4] = 1.0f;
        m[0+3*4] = x;
        m[1+3*4] = y;
        m[2+3*4] = z;
        m[0+1*4] = m[0+2*4] =
        m[1+0*4] = m[1+2*4] =
        m[2+0*4] = m[2+1*4] =
        m[3+0*4] = m[3+1*4] = m[3+2*4] = 0.0f;
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
    public final Matrix4fb setToTranslation(final Vec3f t) {
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
    public final Matrix4fb setToScale(final float x, final float y, final float z) {
        m[3+3*4] = 1.0f;
        m[0+0*4] = x;
        m[1+1*4] = y;
        m[2+2*4] = z;
        m[0+1*4] = m[0+2*4] = m[0+3*4] =
        m[1+0*4] = m[1+2*4] = m[1+3*4] =
        m[2+0*4] = m[2+1*4] = m[2+3*4] =
        m[3+0*4] = m[3+1*4] = m[3+2*4] = 0.0f;
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
     * @param x x of rotation axis
     * @param y y of rotation axis
     * @param z z of rotation axis
     * @return this matrix for chaining
     */
    public final Matrix4fb setToRotationAxis(final float ang_rad, float x, float y, float z) {
        final float c = FloatUtil.cos(ang_rad);
        final float ic= 1.0f - c;
        final float s = FloatUtil.sin(ang_rad);

        final float[] tmpVec3f = { x, y, z };
        VectorUtil.normalizeVec3(tmpVec3f);
        x = tmpVec3f[0]; y = tmpVec3f[1]; z = tmpVec3f[2];

        final float xy = x*y;
        final float xz = x*z;
        final float xs = x*s;
        final float ys = y*s;
        final float yz = y*z;
        final float zs = z*s;
        m[0+0*4] = x*x*ic+c;
        m[1+0*4] = xy*ic+zs;
        m[2+0*4] = xz*ic-ys;
        m[3+0*4] = 0;

        m[0+1*4] = xy*ic-zs;
        m[1+1*4] = y*y*ic+c;
        m[2+1*4] = yz*ic+xs;
        m[3+1*4] = 0;

        m[0+2*4] = xz*ic+ys;
        m[1+2*4] = yz*ic-xs;
        m[2+2*4] = z*z*ic+c;
        m[3+2*4] = 0;

        m[0+3*4] = 0f;
        m[1+3*4] = 0f;
        m[2+3*4] = 0f;
        m[3+3*4] = 1f;

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
    public final Matrix4fb setToRotationAxis(final float ang_rad, final Vec3f axis) {
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
    public Matrix4fb setToRotationEuler(final float bankX, final float headingY, final float attitudeZ) {
        // Assuming the angles are in radians.
        final float ch = FloatUtil.cos(headingY);
        final float sh = FloatUtil.sin(headingY);
        final float ca = FloatUtil.cos(attitudeZ);
        final float sa = FloatUtil.sin(attitudeZ);
        final float cb = FloatUtil.cos(bankX);
        final float sb = FloatUtil.sin(bankX);

        m[0+0*4] =  ch*ca;
        m[1+0*4] =  sa;
        m[2+0*4] = -sh*ca;
        m[3+0*4] =  0;

        m[0+1*4] =  sh*sb    - ch*sa*cb;
        m[1+1*4] =  ca*cb;
        m[2+1*4] =  sh*sa*cb + ch*sb;
        m[3+1*4] =  0;

        m[0+2*4] =  ch*sa*sb + sh*cb;
        m[1+2*4] = -ca*sb;
        m[2+2*4] = -sh*sa*sb + ch*cb;
        m[3+2*4] =  0;

        m[0+3*4] =  0;
        m[1+3*4] =  0;
        m[2+3*4] =  0;
        m[3+3*4] =  1;

        return this;
    }

    /**
     * Set this matrix to rotation using the given Quaternion.
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> makes identity matrix if {@link #magnitudeSquared()} is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     *   <li> The fields [m[0+0*4] .. m[2+2*4]] define the rotation</li>
     * </ul>
     * </p>
     *
     * @param q the Quaternion representing the rotation
     * @return this matrix for chaining
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q54">Matrix-FAQ Q54</a>
     * @see Quaternion#toMatrix(float[])
     * @see #getRotation()
     */
    public final Matrix4fb setToRotation(final Quaternion q) {
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

        m[0+0*4] = 1f - ( yy + zz );
        m[0+1*4] =      ( xy - zw );
        m[0+2*4] =      ( xz + yw );
        m[0+3*4] = 0f;

        m[1+0*4] =      ( xy + zw );
        m[1+1*4] = 1f - ( xx + zz );
        m[1+2*4] =      ( yz - xw );
        m[1+3*4] = 0f;

        m[2+0*4] =      ( xz - yw );
        m[2+1*4] =      ( yz + xw );
        m[2+2*4] = 1f - ( xx + yy );
        m[2+3*4] = 0f;

        m[3+0*4] = m[3+1*4] = m[3+2*4] = 0f;
        m[3+3*4] = 1f;
        return this;
    }

    /**
     * Returns the rotation [m[0+0*4] .. m[2+2*4]] fields converted to a Quaternion.
     * @param res resulting Quaternion
     * @return the resulting Quaternion for chaining.
     * @see Quaternion#setFromMatrix(float, float, float, float, float, float, float, float, float)
     * @see #setToRotation(Quaternion)
     */
    public final Quaternion getRotation(final Quaternion res) {
        res.setFromMatrix(m[0+0*4], m[0+1*4], m[0+2*4], m[1+0*4], m[1+1*4], m[1+2*4], m[2+0*4], m[2+1*4], m[2+2*4]);
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
    public Matrix4fb setToOrtho(final float left, final float right,
                               final float bottom, final float top,
                               final float zNear, final float zFar) {
        {
            // m[0+0*4] = m[1+1*4] = m[2+2*4] = m[3+3*4] = 1f;
            m[1+0*4] = m[2+0*4] = m[3+0*4] = 0f;
            m[0+1*4] = m[2+1*4] = m[3+1*4] = 0f;
            m[0+2*4] = m[1+2*4] = m[3+2*4] = 0f;
            // m[0+3*4] = m[1+3*4] = m[2+3*4] = 0f;
        }
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float tx=-1.0f*(right+left)/dx;
        final float ty=-1.0f*(top+bottom)/dy;
        final float tz=-1.0f*(zFar+zNear)/dz;

        m[0+0*4] =  2.0f/dx;
        m[1+1*4] =  2.0f/dy;
        m[2+2*4] = -2.0f/dz;

        m[0+3*4] = tx;
        m[1+3*4] = ty;
        m[2+3*4] = tz;
        m[3+3*4] = 1f;

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
    public Matrix4fb setToFrustum(final float left, final float right,
                                 final float bottom, final float top,
                                 final float zNear, final float zFar) throws IllegalArgumentException {
        if( zNear <= 0.0f || zFar <= zNear ) {
            throw new IllegalArgumentException("Requirements zNear > 0 and zFar > zNear, but zNear "+zNear+", zFar "+zFar);
        }
        if( left == right || top == bottom) {
            throw new IllegalArgumentException("GL_INVALID_VALUE: top,bottom and left,right must not be equal");
        }
        {
            // m[0+0*4] = m[1+1*4] = m[2+2*4] = m[3+3*4] = 1f;
            m[1+0*4] = m[2+0*4] = m[3+0*4] = 0f;
            m[0+1*4] = m[2+1*4] = m[3+1*4] = 0f;
            m[0+3*4] = m[1+3*4] = 0f;
        }
        final float zNear2 = 2.0f*zNear;
        final float dx=right-left;
        final float dy=top-bottom;
        final float dz=zFar-zNear;
        final float A=(right+left)/dx;
        final float B=(top+bottom)/dy;
        final float C=-1.0f*(zFar+zNear)/dz;
        final float D=-2.0f*(zFar*zNear)/dz;

        m[0+0*4] = zNear2/dx;
        m[1+1*4] = zNear2/dy;

        m[0+2*4] = A;
        m[1+2*4] = B;
        m[2+2*4] = C;
        m[3+2*4] = -1.0f;

        m[2+3*4] = D;
        m[3+3*4] = 0f;

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
    public Matrix4fb setToPerspective(final float fovy_rad, final float aspect, final float zNear, final float zFar) throws IllegalArgumentException {
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
     * @see Frustum#updateByFovDesc(float[], int, boolean, Frustum.FovDesc)
     */
    public Matrix4fb setToPerspective(final FovHVHalves fovhv, final float zNear, final float zFar) throws IllegalArgumentException {
        final FovHVHalves fovhvTan = fovhv.toTangents();  // use tangent of half-fov !
        final float top    =         fovhvTan.top    * zNear;
        final float bottom = -1.0f * fovhvTan.bottom * zNear;
        final float left   = -1.0f * fovhvTan.left   * zNear;
        final float right  =         fovhvTan.right  * zNear;
        return setToFrustum(left, right, bottom, top, zNear, zFar);
    }

    /**
     * Calculate the frustum planes in world coordinates
     * using the passed float[16] as premultiplied P*MV (column major order).
     * <p>
     * Frustum plane's normals will point to the inside of the viewing frustum,
     * as required by this class.
     * </p>
     */
    public void updateFrustumPlanes(final Frustum frustum) {
        // Left:   a = m41 + m[1+1*4], b = m42 + m[1+2*4], c = m43 + m[1+3*4], d = m44 + m14  - [1..4] column-major
        // Left:   a = m[3+0*4] + m[0+0*4], b = m[3+1*4] + m[0+1*4], c = m[3+2*4] + m[0+2*4], d = m[3+3*4] + m[0+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.LEFT];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] + m[0+0*4],
                     m[3+1*4] + m[0+1*4],
                     m[3+2*4] + m[0+2*4] );
            p.d    = m[3+3*4] + m[0+3*4];
        }

        // Right:  a = m41 - m[1+1*4], b = m42 - m[1+2*4], c = m43 - m[1+3*4], d = m44 - m14  - [1..4] column-major
        // Right:  a = m[3+0*4] - m[0+0*4], b = m[3+1*4] - m[0+1*4], c = m[3+2*4] - m[0+2*4], d = m[3+3*4] - m[0+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.RIGHT];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] - m[0+0*4],
                     m[3+1*4] - m[0+1*4],
                     m[3+2*4] - m[0+2*4] );
            p.d    = m[3+3*4] - m[0+3*4];
        }

        // Bottom: a = m41m21, b = m42m22, c = m43m[2+3*4], d = m44m24  - [1..4] column-major
        // Bottom: a = m30m10, b = m31m11, c = m32m12, d = m[3+3*4]m[1+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.BOTTOM];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] + m[1+0*4],
                     m[3+1*4] + m[1+1*4],
                     m[3+2*4] + m[1+2*4] );
            p.d    = m[3+3*4] + m[1+3*4];
        }

        // Top:   a = m41 - m[2+1*4], b = m42 - m[2+2*4], c = m43 - m[2+3*4], d = m44 - m24  - [1..4] column-major
        // Top:   a = m[3+0*4] - m[1+0*4], b = m[3+1*4] - m[1+1*4], c = m[3+2*4] - m[1+2*4], d = m[3+3*4] - m[1+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.TOP];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] - m[1+0*4],
                     m[3+1*4] - m[1+1*4],
                     m[3+2*4] - m[1+2*4] );
            p.d    = m[3+3*4] - m[1+3*4];
        }

        // Near:  a = m41m31, b = m42m32, c = m43m[3+3*4], d = m44m34  - [1..4] column-major
        // Near:  a = m30m20, b = m31m21, c = m32m22, d = m[3+3*4]m[2+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.NEAR];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] + m[2+0*4],
                     m[3+1*4] + m[2+1*4],
                     m[3+2*4] + m[2+2*4] );
            p.d    = m[3+3*4] + m[2+3*4];
        }

        // Far:   a = m41 - m[3+1*4], b = m42 - m[3+2*4], c = m43 - m[3+3*4], d = m44 - m34  - [1..4] column-major
        // Far:   a = m[3+0*4] - m[2+0*4], b = m[3+1*4] - m[2+1*4], c = m32m22, d = m[3+3*4]m[2+3*4]  - [0..3] column-major
        {
            final Frustum.Plane p = frustum.getPlanes()[Frustum.FAR];
            final Vec3f p_n = p.n;
            p_n.set( m[3+0*4] - m[2+0*4],
                     m[3+1*4] - m[2+1*4],
                     m[3+2*4] - m[2+2*4] );
            p.d    = m[3+3*4] - m[2+3*4];
        }

        // Normalize all planes
        for (int i = 0; i < 6; ++i) {
            final Plane p = frustum.getPlanes()[i];
            final Vec3f p_n = p.n;
            final float invLen = 1f / p_n.length();
            p_n.scale(invLen);
            p.d *= invLen;
        }
    }

    /**
     * Make given matrix the <i>look-at</i> matrix based on given parameters.
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
    public Matrix4fb setToLookAt(final Vec3f eye, final Vec3f center, final Vec3f up, final Matrix4fb tmp) {
        // normalized forward!
        final Vec3f fwd = new Vec3f( center.x() - eye.x(),
                                     center.y() - eye.y(),
                                     center.z() - eye.z() ).normalize();

        /* Side = forward x up, normalized */
        final Vec3f side = fwd.cross(up).normalize();

        /* Recompute up as: up = side x forward */
        final Vec3f up2 = side.cross(fwd);

        m[0+0*4] = side.x();
        m[1+0*4] = up2.x();
        m[2+0*4] = -fwd.x();
        m[3+0*4] = 0;

        m[0+1*4] = side.y();
        m[1+1*4] = up2.y();
        m[2+1*4] = -fwd.y();
        m[3+1*4] = 0;

        m[0+2*4] = side.z();
        m[1+2*4] = up2.z();
        m[2+2*4] = -fwd.z();
        m[3+2*4] = 0;

        m[0+3*4] = 0;
        m[1+3*4] = 0;
        m[2+3*4] = 0;
        m[3+3*4] = 1;

        return mul( tmp.setToTranslation( -eye.x(), -eye.y(), -eye.z() ) );
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
    public final Matrix4fb rotate(final float ang_rad, final float x, final float y, final float z, final Matrix4fb tmp) {
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
    public final Matrix4fb rotate(final float ang_rad, final Vec3f axis, final Matrix4fb tmp) {
        return mul( tmp.setToRotationAxis(ang_rad, axis) );
    }

    /**
     * Rotate this matrix with the given {@link Quaternion}, i.e. multiply by {@link #setToRotation(Quaternion) Quaternion's rotation matrix}.
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4fb rotate(final Quaternion quat, final Matrix4fb tmp) {
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
    public final Matrix4fb translate(final float x, final float y, final float z, final Matrix4fb tmp) {
        return mul( tmp.setToTranslation(x, y, z) );
    }

    /**
     * Translate this matrix, i.e. multiply by {@link #setToTranslation(Vec3f) translation matrix}.
     * @param t translation Vec3f
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4fb translate(final Vec3f t, final Matrix4fb tmp) {
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
    public final Matrix4fb scale(final float x, final float y, final float z, final Matrix4fb tmp) {
        return mul( tmp.setToScale(x, y, z) );
    }

    /**
     * Scale this matrix, i.e. multiply by {@link #setToScale(float, float, float) scale matrix}.
     * @param s scale for x-, y- and z-axis
     * @param tmp temporary Matrix4f used for multiplication
     * @return this matrix for chaining
     */
    public final Matrix4fb scale(final float s, final Matrix4fb tmp) {
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
    public boolean isEqual(final Matrix4fb o, final float epsilon) {
        if( this == o ) {
            return true;
        } else {
            for(int i=0; i<16; ++i) {
                if( !FloatUtil.isEqual(m[i], o.m[i], epsilon) ) {
                    return false;
                }
            }
            return true;
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
    public boolean isEqual(final Matrix4fb o) {
        return isEqual(o, FloatUtil.EPSILON);
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Matrix4fb ) {
            return isEqual((Matrix4fb)o, FloatUtil.EPSILON);
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
     * @param viewport 4 component viewport vector
     * @param winPos 3 component window coordinate, the result
     * @return true if successful, otherwise false (z is 1)
     */
    public static boolean mapObjToWin(final Vec3f obj, final Matrix4fb mMv, final Matrix4fb mP,
                                      final int[] viewport, final float[] winPos)
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
        winPos[0] = rawWinPos.x() * viewport[2] + viewport[0];
        winPos[1] = rawWinPos.y() * viewport[3] + viewport[1];
        winPos[2] = rawWinPos.z();

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
     * @param viewport 4 component viewport vector
     * @param winPos 3 component window coordinate, the result
     * @return true if successful, otherwise false (z is 1)
     */
    public static boolean mapObjToWin(final Vec3f obj, final Matrix4fb mPMv,
                                      final int[] viewport, final float[] winPos)
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
        winPos[0] = rawWinPos.x() * viewport[2] + viewport[0];
        winPos[1] = rawWinPos.y() * viewport[3] + viewport[1];
        winPos[2] = rawWinPos.z();

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
     * @param viewport 4 component viewport vector
     * @param objPos 3 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz,
                                      final Matrix4fb mMv, final Matrix4fb mP,
                                      final int[] viewport,
                                      final Vec3f objPos,
                                      final Matrix4fb mat4Tmp)
    {
        // invPMv = Inv(P x Mv)
        final Matrix4fb invPMv = mat4Tmp.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        final Vec4f winPos = new Vec4f(winx, winy, winz, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport[0], -viewport[1], 0f, 0f).scale(1f/viewport[2], 1f/viewport[3], 1f, 1f);

        // Map to range -1 to 1
        winPos.scale(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

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
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv)
     * @param viewport 4 component viewport vector
     * @param objPos 3 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz,
                                      final Matrix4fb invPMv,
                                      final int[] viewport,
                                      final Vec3f objPos,
                                      final Matrix4fb mat4Tmp)
    {
        final Vec4f winPos = new Vec4f(winx, winy, winz, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport[0], -viewport[1], 0f, 0f).scale(1f/viewport[2], 1f/viewport[3], 1f, 1f);

        // Map to range -1 to 1
        winPos.scale(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

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
     * @param invPMv inverse [projection] x [modelview] matrix, i.e. Inv(P x Mv)
     * @param viewport 4 component viewport vector
     * @param objPos1 3 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj(final float winx, final float winy, final float winz1, final float winz2,
                                      final Matrix4fb invPMv,
                                      final int[] viewport,
                                      final Vec3f objPos1, final Vec3f objPos2,
                                      final Matrix4fb mat4Tmp)
    {
        final Vec4f winPos = new Vec4f(winx, winy, winz1, 1f);

        // Map x and y from window coordinates
        winPos.add(-viewport[0], -viewport[1], 0f, 0f).scale(1f/viewport[2], 1f/viewport[3], 1f, 1f);

        // Map to range -1 to 1
        winPos.scale(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

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
     * @param viewport 4 component viewport vector
     * @param near
     * @param far
     * @param obj_pos 4 component object coordinate, the result
     * @param mat4Tmp 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes infinity due to zero z)
     */
    public static boolean mapWinToObj4(final float winx, final float winy, final float winz, final float clipw,
                                       final Matrix4fb mMv, final Matrix4fb mP,
                                       final int[] viewport,
                                       final float near, final float far,
                                       final Vec4f objPos,
                                       final Matrix4fb mat4Tmp)
    {
        // invPMv = Inv(P x Mv)
        final Matrix4fb invPMv = mat4Tmp.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        final Vec4f winPos = new Vec4f(winx, winy, winz, clipw);

        // Map x and y from window coordinates
        winPos.add(-viewport[0], -viewport[1], -near, 0f).scale(1f/viewport[2], 1f/viewport[3], 1f/(far-near), 1f);

        // Map to range -1 to 1
        winPos.scale(2f, 2f, 2f, 1f).add(-1f, -1f, -1f, 0f);

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
     * using a {@link AABBox#getRayIntersection(Ray, float[]) bounding box}.
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
     * @param viewport 4 component viewport vector
     * @param ray storage for the resulting {@link Ray}
     * @param mat4Tmp1 16 component matrix for temp storage
     * @param mat4Tmp2 16 component matrix for temp storage
     * @return true if successful, otherwise false (failed to invert matrix, or becomes z is infinity)
     */
    public static boolean mapWinToRay(final float winx, final float winy, final float winz0, final float winz1,
                                      final Matrix4fb mMv,
                                      final Matrix4fb mP,
                                      final int[] viewport,
                                      final Ray ray,
                                      final Matrix4fb mat4Tmp1, final Matrix4fb mat4Tmp2) {
        // invPMv = Inv(P x Mv)
        final Matrix4fb invPMv = mat4Tmp1.mul(mP, mMv);
        if( !invPMv.invert() ) {
            return false;
        }

        if( mapWinToObj(winx, winy, winz0, winz1, invPMv, viewport,
                        ray.orig, ray.dir, mat4Tmp2) ) {
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

    private final float[] m = new float[16];

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

        public final Matrix4fb push(final Matrix4fb src) throws IndexOutOfBoundsException {
            growIfNecessary(16);
            src.get(buffer, position);
            position += 16;
            return src;
        }

        public final Matrix4fb pop(final Matrix4fb dest) throws IndexOutOfBoundsException {
            position -= 16;
            dest.load(buffer, position);
            return dest;
        }
    }
}
