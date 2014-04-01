/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
/**
 * @author Denis M. Kishenko
 */
package jogamp.graph.geom.plane;

// import jogamp.opengl.util.HashCode;

import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.geom.AABBox;

public class AffineTransform implements Cloneable {

    static final String determinantIsZero = "Determinant is zero";

    public static final int TYPE_IDENTITY = 0;
    public static final int TYPE_TRANSLATION = 1;
    public static final int TYPE_UNIFORM_SCALE = 2;
    public static final int TYPE_GENERAL_SCALE = 4;
    public static final int TYPE_QUADRANT_ROTATION = 8;
    public static final int TYPE_GENERAL_ROTATION = 16;
    public static final int TYPE_GENERAL_TRANSFORM = 32;
    public static final int TYPE_FLIP = 64;
    public static final int TYPE_MASK_SCALE = TYPE_UNIFORM_SCALE | TYPE_GENERAL_SCALE;
    public static final int TYPE_MASK_ROTATION = TYPE_QUADRANT_ROTATION | TYPE_GENERAL_ROTATION;

    /**
     * The <code>TYPE_UNKNOWN</code> is an initial type value
     */
    static final int TYPE_UNKNOWN = -1;

    /**
     * The min value equivalent to zero. If absolute value less then ZERO it considered as zero.
     */
    static final float ZERO = (float) 1E-10;

    /**
     * The values of transformation matrix
     */
    float m00;
    float m10;
    float m01;
    float m11;
    float m02;
    float m12;

    /**
     * The transformation <code>type</code>
     */
    transient int type;

    public AffineTransform() {
        setToIdentity();
    }

    public AffineTransform(AffineTransform t) {
        this.type = t.type;
        this.m00 = t.m00;
        this.m10 = t.m10;
        this.m01 = t.m01;
        this.m11 = t.m11;
        this.m02 = t.m02;
        this.m12 = t.m12;
    }

    public AffineTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        this.type = TYPE_UNKNOWN;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public AffineTransform(float[] matrix) {
        this.type = TYPE_UNKNOWN;
        m00 = matrix[0];
        m10 = matrix[1];
        m01 = matrix[2];
        m11 = matrix[3];
        if (matrix.length > 4) {
            m02 = matrix[4];
            m12 = matrix[5];
        }
    }

    /*
     * Method returns type of affine transformation.
     *
     * Transform matrix is
     *   m00 m01 m02
     *   m10 m11 m12
     *
     * According analytic geometry new basis vectors are (m00, m01) and (m10, m11),
     * translation vector is (m02, m12). Original basis vectors are (1, 0) and (0, 1).
     * Type transformations classification:
     *   TYPE_IDENTITY - new basis equals original one and zero translation
     *   TYPE_TRANSLATION - translation vector isn't zero
     *   TYPE_UNIFORM_SCALE - vectors length of new basis equals
     *   TYPE_GENERAL_SCALE - vectors length of new basis doesn't equal
     *   TYPE_FLIP - new basis vector orientation differ from original one
     *   TYPE_QUADRANT_ROTATION - new basis is rotated by 90, 180, 270, or 360 degrees
     *   TYPE_GENERAL_ROTATION - new basis is rotated by arbitrary angle
     *   TYPE_GENERAL_TRANSFORM - transformation can't be inversed
     */
    public int getType() {
        if (type != TYPE_UNKNOWN) {
            return type;
        }

        int type = 0;

        if (m00 * m01 + m10 * m11 != 0.0) {
            type |= TYPE_GENERAL_TRANSFORM;
            return type;
        }

        if (m02 != 0.0 || m12 != 0.0) {
            type |= TYPE_TRANSLATION;
        } else
            if (m00 == 1.0 && m11 == 1.0 && m01 == 0.0 && m10 == 0.0) {
                type = TYPE_IDENTITY;
                return type;
            }

        if (m00 * m11 - m01 * m10 < 0.0) {
            type |= TYPE_FLIP;
        }

        float dx = m00 * m00 + m10 * m10;
        float dy = m01 * m01 + m11 * m11;
        if (dx != dy) {
            type |= TYPE_GENERAL_SCALE;
        } else
            if (dx != 1.0) {
                type |= TYPE_UNIFORM_SCALE;
            }

        if ((m00 == 0.0 && m11 == 0.0) ||
            (m10 == 0.0 && m01 == 0.0 && (m00 < 0.0 || m11 < 0.0)))
        {
            type |= TYPE_QUADRANT_ROTATION;
        } else
            if (m01 != 0.0 || m10 != 0.0) {
                type |= TYPE_GENERAL_ROTATION;
            }

        return type;
    }

    public final float getScaleX() {
        return m00;
    }

    public final float getScaleY() {
        return m11;
    }

    public final float getShearX() {
        return m01;
    }

    public final float getShearY() {
        return m10;
    }

    public final float getTranslateX() {
        return m02;
    }

    public final float getTranslateY() {
        return m12;
    }

    public final boolean isIdentity() {
        return getType() == TYPE_IDENTITY;
    }

    public final void getMatrix(float[] matrix) {
        matrix[0] = m00;
        matrix[1] = m10;
        matrix[2] = m01;
        matrix[3] = m11;
        if (matrix.length > 4) {
            matrix[4] = m02;
            matrix[5] = m12;
        }
    }

    public final float getDeterminant() {
        return m00 * m11 - m01 * m10;
    }

    public final void setTransform(float m00, float m10, float m01, float m11, float m02, float m12) {
        this.type = TYPE_UNKNOWN;
        this.m00 = m00;
        this.m10 = m10;
        this.m01 = m01;
        this.m11 = m11;
        this.m02 = m02;
        this.m12 = m12;
    }

    public final void setTransform(AffineTransform t) {
        type = t.type;
        setTransform(t.m00, t.m10, t.m01, t.m11, t.m02, t.m12);
    }

    public final void setToIdentity() {
        type = TYPE_IDENTITY;
        m00 = m11 = 1.0f;
        m10 = m01 = m02 = m12 = 0.0f;
    }

    public final void setToTranslation(float mx, float my) {
        m00 = m11 = 1.0f;
        m01 = m10 = 0.0f;
        m02 = mx;
        m12 = my;
        if (mx == 0.0f && my == 0.0f) {
            type = TYPE_IDENTITY;
        } else {
            type = TYPE_TRANSLATION;
        }
    }

    public final void setToScale(float scx, float scy) {
        m00 = scx;
        m11 = scy;
        m10 = m01 = m02 = m12 = 0.0f;
        if (scx != 1.0f || scy != 1.0f) {
            type = TYPE_UNKNOWN;
        } else {
            type = TYPE_IDENTITY;
        }
    }

    public final void setToShear(float shx, float shy) {
        m00 = m11 = 1.0f;
        m02 = m12 = 0.0f;
        m01 = shx;
        m10 = shy;
        if (shx != 0.0f || shy != 0.0f) {
            type = TYPE_UNKNOWN;
        } else {
            type = TYPE_IDENTITY;
        }
    }

    public final void setToRotation(float angle) {
        float sin = FloatUtil.sin(angle);
        float cos = FloatUtil.cos(angle);
        if (FloatUtil.abs(cos) < ZERO) {
            cos = 0.0f;
            sin = sin > 0.0f ? 1.0f : -1.0f;
        } else
            if (FloatUtil.abs(sin) < ZERO) {
                sin = 0.0f;
                cos = cos > 0.0f ? 1.0f : -1.0f;
            }
        m00 = m11 = cos;
        m01 = -sin;
        m10 = sin;
        m02 = m12 = 0.0f;
        type = TYPE_UNKNOWN;
    }

    public final void setToRotation(float angle, float px, float py) {
        setToRotation(angle);
        m02 = px * (1.0f - m00) + py * m10;
        m12 = py * (1.0f - m00) - px * m10;
        type = TYPE_UNKNOWN;
    }

    public static AffineTransform getTranslateInstance(float mx, float my) {
        AffineTransform t = new AffineTransform();
        t.setToTranslation(mx, my);
        return t;
    }

    public static AffineTransform getScaleInstance(float scx, float scY) {
        AffineTransform t = new AffineTransform();
        t.setToScale(scx, scY);
        return t;
    }

    public static AffineTransform getShearInstance(float shx, float shy) {
        AffineTransform t = new AffineTransform();
        t.setToShear(shx, shy);
        return t;
    }

    public static AffineTransform getRotateInstance(float angle) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(angle);
        return t;
    }

    public static AffineTransform getRotateInstance(float angle, float x, float y) {
        AffineTransform t = new AffineTransform();
        t.setToRotation(angle, x, y);
        return t;
    }

    public final AffineTransform translate(float mx, float my) {
        return concatenate(AffineTransform.getTranslateInstance(mx, my));
    }

    public final AffineTransform scale(float scx, float scy) {
        return concatenate(AffineTransform.getScaleInstance(scx, scy));
    }

    public final AffineTransform shear(float shx, float shy) {
        return concatenate(AffineTransform.getShearInstance(shx, shy));
    }

    public final AffineTransform rotate(float angle) {
        return concatenate(AffineTransform.getRotateInstance(angle));
    }

    public final AffineTransform rotate(float angle, float px, float py) {
        return concatenate(AffineTransform.getRotateInstance(angle, px, py));
    }

    /**
     * Multiply matrix of two AffineTransform objects.
     * @param tL - the AffineTransform object is a multiplicand (left argument)
     * @param tR - the AffineTransform object is a multiplier (right argument)
     *
     * @return A new AffineTransform object containing the result of [tL] X [tR].
     */
    public final static AffineTransform multiply(final AffineTransform tL, final AffineTransform tR) {
        return new AffineTransform(
                tR.m00 * tL.m00 + tR.m10 * tL.m01,          // m00
                tR.m00 * tL.m10 + tR.m10 * tL.m11,          // m10
                tR.m01 * tL.m00 + tR.m11 * tL.m01,          // m01
                tR.m01 * tL.m10 + tR.m11 * tL.m11,          // m11
                tR.m02 * tL.m00 + tR.m12 * tL.m01 + tL.m02, // m02
                tR.m02 * tL.m10 + tR.m12 * tL.m11 + tL.m12);// m12
    }

    /**
     * Concatenates the given matrix to this.
     * <p>
     * Implementations performs the matrix multiplication:
     * <pre>
     *   [this] = [this] X [tR]
     * </pre>
     * </p>
     * @param tR the right-argument of the matrix multiplication
     * @return this transform for chaining
     */
    public final AffineTransform concatenate(AffineTransform tR) {
        // setTransform(multiply(this, tR));
        type = TYPE_UNKNOWN;
        setTransform(
                tR.m00 * m00 + tR.m10 * m01,       // m00
                tR.m00 * m10 + tR.m10 * m11,       // m10
                tR.m01 * m00 + tR.m11 * m01,       // m01
                tR.m01 * m10 + tR.m11 * m11,       // m11
                tR.m02 * m00 + tR.m12 * m01 + m02, // m02
                tR.m02 * m10 + tR.m12 * m11 + m12);// m12
        return this;
    }

    /**
     * Pre-concatenates the given matrix to this.
     * <p>
     * Implementations performs the matrix multiplication:
     * <pre>
     *   [this] = [tL] X [this]
     * </pre>
     * </p>
     * @param tL the left-argument of the matrix multiplication
     * @return this transform for chaining
     */
    public final AffineTransform preConcatenate(AffineTransform tL) {
        // setTransform(multiply(tL, this));
        type = TYPE_UNKNOWN;
        setTransform(
                m00 * tL.m00 + m10 * tL.m01,          // m00
                m00 * tL.m10 + m10 * tL.m11,          // m10
                m01 * tL.m00 + m11 * tL.m01,          // m01
                m01 * tL.m10 + m11 * tL.m11,          // m11
                m02 * tL.m00 + m12 * tL.m01 + tL.m02, // m02
                m02 * tL.m10 + m12 * tL.m11 + tL.m12);// m12
        return this;
    }

    public final AffineTransform createInverse() throws NoninvertibleTransformException {
        float det = getDeterminant();
        if (FloatUtil.abs(det) < ZERO) {
            throw new NoninvertibleTransformException(determinantIsZero);
        }
        return new AffineTransform(
                 m11 / det, // m00
                -m10 / det, // m10
                -m01 / det, // m01
                 m00 / det, // m11
                (m01 * m12 - m11 * m02) / det, // m02
                (m10 * m02 - m00 * m12) / det  // m12
        );
    }

    /**
     *
     * @param src
     * @param dst
     * @return dst for chaining
     */
    public final AABBox transform(final AABBox src, final AABBox dst) {
        final float[] srcLo = src.getLow();
        final float[] srcHi = src.getHigh();
        dst.setSize(srcLo[0] * m00 + srcLo[1] * m01 + m02, srcLo[0] * m10 + srcLo[1] * m11 + m12, srcLo[2],
                    srcHi[0] * m00 + srcHi[1] * m01 + m02, srcHi[0] * m10 + srcHi[1] * m11 + m12, srcHi[2]);
        return dst;
    }

    /**
     * @param src
     * @param dst
     * @return dst for chaining
     */
    public final Vertex transform(final Vertex src, final Vertex dst) {
        final float x = src.getX();
        final float y = src.getY();
        dst.setCoord(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12, src.getZ());
        return dst;
    }

    public final void transform(final Vertex[] src, int srcOff, final Vertex[] dst, int dstOff, int length) {
        while (--length >= 0) {
            Vertex srcPoint = src[srcOff++];
            Vertex dstPoint = dst[dstOff];
            if (dstPoint == null) {
                throw new IllegalArgumentException("dst["+dstOff+"] is null");
            }
            final float x = srcPoint.getX();
            final float y = srcPoint.getY();
            dstPoint.setCoord(x * m00 + y * m01 + m02, x * m10 + y * m11 + m12, srcPoint.getZ());
            dst[dstOff++] = dstPoint;
        }
    }

    /**
     * @param src float[2] source of transformation
     * @param dst float[2] destination of transformation, maybe be equal to <code>src</code>
     * @return dst for chaining
     */
    public final float[] transform(final float[] src, final float[] dst) {
        final float x = src[0];
        final float y = src[1];
        dst[0] = x * m00 + y * m01 + m02;
        dst[1] = x * m10 + y * m11 + m12;
        return dst;
    }

    public final void transform(final float[] src, final int srcOff, final float[] dst, final int dstOff) {
        final float x = src[srcOff + 0];
        final float y = src[srcOff + 1];
        dst[dstOff + 0] = x * m00 + y * m01 + m02;
        dst[dstOff + 1] = x * m10 + y * m11 + m12;
    }

    public final void transform(final float[] src, int srcOff, final float[] dst, int dstOff, int length) {
        int step = 2;
        if (src == dst && srcOff < dstOff && dstOff < srcOff + length * 2) {
            srcOff = srcOff + length * 2 - 2;
            dstOff = dstOff + length * 2 - 2;
            step = -2;
        }
        while (--length >= 0) {
            final float x = src[srcOff + 0];
            final float y = src[srcOff + 1];
            dst[dstOff + 0] = x * m00 + y * m01 + m02;
            dst[dstOff + 1] = x * m10 + y * m11 + m12;
            srcOff += step;
            dstOff += step;
        }
    }

    /**
     *
     * @param src
     * @param dst
     * @return return dst for chaining
     */
    public final Vertex deltaTransform(final Vertex src, final Vertex dst) {
        final float x = src.getX();
        final float y = src.getY();
        dst.setCoord(x * m00 + y * m01, x * m10 + y * m11, src.getZ());
        return dst;
    }

    public final void deltaTransform(final float[] src, int srcOff, final float[] dst, int dstOff, int length) {
        while (--length >= 0) {
            float x = src[srcOff++];
            float y = src[srcOff++];
            dst[dstOff++] = x * m00 + y * m01;
            dst[dstOff++] = x * m10 + y * m11;
        }
    }

    /**
     *
     * @param src
     * @param dst
     * @return return dst for chaining
     * @throws NoninvertibleTransformException
     */
    public final Vertex inverseTransform(final Vertex src, final Vertex dst) throws NoninvertibleTransformException {
        float det = getDeterminant();
        if (FloatUtil.abs(det) < ZERO) {
            throw new NoninvertibleTransformException(determinantIsZero);
        }
        final float x = src.getX() - m02;
        final float y = src.getY() - m12;
        dst.setCoord((x * m11 - y * m01) / det, (y * m00 - x * m10) / det, src.getZ());
        return dst;
    }

    public final void inverseTransform(final float[] src, int srcOff, final float[] dst, int dstOff, int length)
        throws NoninvertibleTransformException
    {
        float det = getDeterminant();
        if (FloatUtil.abs(det) < ZERO) {
            throw new NoninvertibleTransformException(determinantIsZero);
        }

        while (--length >= 0) {
            float x = src[srcOff++] - m02;
            float y = src[srcOff++] - m12;
            dst[dstOff++] = (x * m11 - y * m01) / det;
            dst[dstOff++] = (y * m00 - x * m10) / det;
        }
    }

    public final Path2D createTransformedShape(Path2D src) {
        if (src == null) {
            return null;
        }
        if (src instanceof Path2D) {
            return src.createTransformedShape(this);
        }
        PathIterator path = src.iterator(this);
        Path2D dst = new Path2D(path.getWindingRule());
        dst.append(path, false);
        return dst;
    }

    @Override
    public final String toString() {
        return
            getClass().getName() +
            "[[" + m00 + ", " + m01 + ", " + m02 + "], [" //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
                + m10 + ", " + m11 + ", " + m12 + "]]"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    }

    @Override
    public final AffineTransform clone() {
        try {
            return (AffineTransform) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /** @Override
    public int hashCode() {
        HashCode hash = new HashCode();
        hash.append(m00);
        hash.append(m01);
        hash.append(m02);
        hash.append(m10);
        hash.append(m11);
        hash.append(m12);
        return hash.hashCode();
    } */

    @Override
    public final boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof AffineTransform) {
            AffineTransform t = (AffineTransform)obj;
            return
                m00 == t.m00 && m01 == t.m01 &&
                m02 == t.m02 && m10 == t.m10 &&
                m11 == t.m11 && m12 == t.m12;
        }
        return false;
    }
}

