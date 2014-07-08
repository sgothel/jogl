/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.math;

/**
 * Quaternion implementation supporting
 * <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q34">Gimbal-Lock</a> free rotations.
 * <p>
 * All matrix operation provided are in column-major order,
 * as specified in the OpenGL fixed function pipeline, i.e. compatibility profile.
 * See {@link FloatUtil}.
 * </p>
 * <p>
 * See <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html">Matrix-FAQ</a>
 * </p>
 * <p>
 * See <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/index.htm">euclideanspace.com-Quaternion</a>
 * </p>
 */
public class Quaternion {
    private float x, y, z, w;

    /**
     * Quaternion Epsilon, used with equals method to determine if two Quaternions are close enough to be considered equal.
     * <p>
     * Using {@value}, which is ~10 times {@link FloatUtil#EPSILON}.
     * </p>
     */
    public static final float ALLOWED_DEVIANCE = 1.0E-6f; // FloatUtil.EPSILON == 1.1920929E-7f; double ALLOWED_DEVIANCE: 1.0E-8f

    public Quaternion() {
        x = y = z = 0; w = 1;
    }

    public Quaternion(final Quaternion q) {
        set(q);
    }

    public Quaternion(final float x, final float y, final float z, final float w) {
        set(x, y, z, w);
    }

    /**
     * See {@link #magnitude()} for special handling of {@link FloatUtil#EPSILON epsilon},
     * which is not applied here.
     * @return the squared magnitude of this quaternion.
     */
    public final float magnitudeSquared() {
        return w*w + x*x + y*y + z*z;
    }

    /**
     * Return the magnitude of this quaternion, i.e. sqrt({@link #magnitude()})
     * <p>
     * A magnitude of zero shall equal {@link #isIdentity() identity},
     * as performed by {@link #normalize()}.
     * </p>
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> returns 0f if {@link #magnitudeSquared()} is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     *   <li> returns 1f if {@link #magnitudeSquared()} is {@link FloatUtil#isEqual(float, float, float) equals 1f} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     */
    public final float magnitude() {
        final float magnitudeSQ = magnitudeSquared();
        if ( FloatUtil.isZero(magnitudeSQ, FloatUtil.EPSILON) ) {
            return 0f;
        }
        if ( FloatUtil.isEqual(1f, magnitudeSQ, FloatUtil.EPSILON) ) {
            return 1f;
        }
        return FloatUtil.sqrt(magnitudeSQ);
    }

    public final float getW() {
        return w;
    }

    public final void setW(final float w) {
        this.w = w;
    }

    public final float getX() {
        return x;
    }

    public final void setX(final float x) {
        this.x = x;
    }

    public final float getY() {
        return y;
    }

    public final void setY(final float y) {
        this.y = y;
    }

    public final float getZ() {
        return z;
    }

    public final void setZ(final float z) {
        this.z = z;
    }

    /**
     * Returns the dot product of this quaternion with the given x,y,z and w components.
     */
    public final float dot(final float x, final float y, final float z, final float w) {
        return this.x * x + this.y * y + this.z * z + this.w * w;
    }

    /**
     * Returns the dot product of this quaternion with the given quaternion
     */
    public final float dot(final Quaternion quat) {
        return dot(quat.getX(), quat.getY(), quat.getZ(), quat.getW());
    }

    /**
     * Returns <code>true</code> if this quaternion has identity.
     * <p>
     * Implementation uses {@link FloatUtil#EPSILON epsilon} to compare
     * {@link #getW() W} {@link FloatUtil#isEqual(float, float, float) against 1f} and
     * {@link #getX() X}, {@link #getY() Y} and {@link #getZ() Z}
     * {@link FloatUtil#isZero(float, float) against zero}.
     * </p>
     */
    public final boolean isIdentity() {
        return FloatUtil.isEqual(1f, w, FloatUtil.EPSILON) && VectorUtil.isZero(x, y, z, FloatUtil.EPSILON);
        // return w == 1f && x == 0f && y == 0f && z == 0f;
    }

    /***
     * Set this quaternion to identity (x=0,y=0,z=0,w=1)
     * @return this quaternion for chaining.
     */
    public final Quaternion setIdentity() {
        x = y = z = 0f; w = 1f;
        return this;
    }

    /**
     * Normalize a quaternion required if to be used as a rotational quaternion.
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if {@link #magnitude()} is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @return this quaternion for chaining.
     */
    public final Quaternion normalize() {
        final float norm = magnitude();
        if ( FloatUtil.isZero(norm, FloatUtil.EPSILON) ) {
            setIdentity();
        } else {
            final float invNorm = 1f/norm;
            w *= invNorm;
            x *= invNorm;
            y *= invNorm;
            z *= invNorm;
        }
        return this;
    }

    /**
     * Conjugates this quaternion <code>[-x, -y, -z, w]</code>.
     * @return this quaternion for chaining.
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q49">Matrix-FAQ Q49</a>
     */
    public Quaternion conjugate() {
        x = -x;
        y = -y;
        z = -z;
        return this;
    }

    /**
     * Invert the quaternion If rotational, will produce a the inverse rotation
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #conjugate() conjugates} if {@link #magnitudeSquared()} is is {@link FloatUtil#isEqual(float, float, float) equals 1f} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @return this quaternion for chaining.
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q50">Matrix-FAQ Q50</a>
     */
    public final Quaternion invert() {
        final float magnitudeSQ = magnitudeSquared();
        if ( FloatUtil.isEqual(1.0f, magnitudeSQ, FloatUtil.EPSILON) ) {
            conjugate();
        } else {
            final float invmsq = 1f/magnitudeSQ;
            w *= invmsq;
            x = -x * invmsq;
            y = -y * invmsq;
            z = -z * invmsq;
        }
        return this;
    }

    /**
     * Set all values of this quaternion using the given src.
     * @return this quaternion for chaining.
     */
    public final Quaternion set(final Quaternion src) {
        this.x = src.x;
        this.y = src.y;
        this.z = src.z;
        this.w = src.w;
        return this;
    }

    /**
     * Set all values of this quaternion using the given components.
     * @return this quaternion for chaining.
     */
    public final Quaternion set(final float x, final float y, final float z, final float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /**
     * Add a quaternion
     *
     * @param q quaternion
     * @return this quaternion for chaining.
     * @see <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm#add">euclideanspace.com-QuaternionAdd</a>
     */
    public final Quaternion add(final Quaternion q) {
        x += q.x;
        y += q.y;
        z += q.z;
        w += q.w;
        return this;
    }

    /**
     * Subtract a quaternion
     *
     * @param q quaternion
     * @return this quaternion for chaining.
     * @see <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm#add">euclideanspace.com-QuaternionAdd</a>
     */
    public final Quaternion subtract(final Quaternion q) {
        x -= q.x;
        y -= q.y;
        z -= q.z;
        w -= q.w;
        return this;
    }

    /**
     * Multiply this quaternion by the param quaternion
     *
     * @param q a quaternion to multiply with
     * @return this quaternion for chaining.
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q53">Matrix-FAQ Q53</a>
     * @see <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm#mul">euclideanspace.com-QuaternionMul</a>
     */
    public final Quaternion mult(final Quaternion q) {
        return set( w * q.x + x * q.w + y * q.z - z * q.y,
                    w * q.y - x * q.z + y * q.w + z * q.x,
                    w * q.z + x * q.y - y * q.x + z * q.w,
                    w * q.w - x * q.x - y * q.y - z * q.z );
    }

    /**
     * Scale this quaternion by a constant
     *
     * @param n a float constant
     * @return this quaternion for chaining.
     * @see <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/code/index.htm#scale">euclideanspace.com-QuaternionScale</a>
     */
    public final Quaternion scale(final float n) {
        x *= n;
        y *= n;
        z *= n;
        w *= n;
        return this;
    }

    /**
     * Rotate this quaternion by the given angle and axis.
     * <p>
     * The axis must be a normalized vector.
     * </p>
     * <p>
     * A rotational quaternion is made from the given angle and axis.
     * </p>
     *
     * @param angle in radians
     * @param axisX x-coord of rotation axis
     * @param axisY y-coord of rotation axis
     * @param axisZ z-coord of rotation axis
     * @return this quaternion for chaining.
     */
    public Quaternion rotateByAngleNormalAxis(final float angle, final float axisX, final float axisY, final float axisZ) {
        if( VectorUtil.isZero(axisX, axisY, axisZ, FloatUtil.EPSILON) ) {
            // no change
            return this;
        }
        final float halfAngle = 0.5f * angle;
        final float sin = FloatUtil.sin(halfAngle);
        final float qw = FloatUtil.cos(halfAngle);
        final float qx = sin * axisX;
        final float qy = sin * axisY;
        final float qz = sin * axisZ;
        return set( x * qw + y * qz - z * qy + w * qx,
                   -x * qz + y * qw + z * qx + w * qy,
                    x * qy - y * qx + z * qw + w * qz,
                   -x * qx - y * qy - z * qz + w * qw);
    }

    /**
     * Rotate this quaternion around X axis with the given angle in radians
     *
     * @param angle in radians
     * @return this quaternion for chaining.
     */
    public Quaternion rotateByAngleX(final float angle) {
        final float halfAngle = 0.5f * angle;
        final float sin = FloatUtil.sin(halfAngle);
        final float cos = FloatUtil.cos(halfAngle);
        return set( x * cos + w * sin,
                    y * cos + z * sin,
                   -y * sin + z * cos,
                   -x * sin + w * cos);
    }

    /**
     * Rotate this quaternion around Y axis with the given angle in radians
     *
     * @param angle in radians
     * @return this quaternion for chaining.
     */
    public Quaternion rotateByAngleY(final float angle) {
        final float halfAngle = 0.5f * angle;
        final float sin = FloatUtil.sin(halfAngle);
        final float cos = FloatUtil.cos(halfAngle);
        return set( x * cos - z * sin,
                    y * cos + w * sin,
                    x * sin + z * cos,
                   -y * sin + w * cos);
    }

    /**
     * Rotate this quaternion around Z axis with the given angle in radians
     *
     * @param angle in radians
     * @return this quaternion for chaining.
     */
    public Quaternion rotateByAngleZ(final float angle) {
        final float halfAngle = 0.5f * angle;
        final float sin = FloatUtil.sin(halfAngle);
        final float cos = FloatUtil.cos(halfAngle);
        return set( x * cos + y * sin,
                   -x * sin + y * cos,
                    z * cos + w * sin,
                   -z * sin + w * cos);
    }

    /**
     * Rotates this quaternion from the given Euler rotation array <code>angradXYZ</code> in radians.
     * <p>
     * The <code>angradXYZ</code> array is laid out in natural order:
     * <ul>
     *  <li>x - bank</li>
     *  <li>y - heading</li>
     *  <li>z - attitude</li>
     * </ul>
     * </p>
     * For details see {@link #rotateByEuler(float, float, float)}.
     * @param angradXYZ euler angel array in radians
     * @return this quaternion for chaining.
     * @see #rotateByEuler(float, float, float)
     */
    public final Quaternion rotateByEuler(final float[] angradXYZ) {
        return rotateByEuler(angradXYZ[0], angradXYZ[1], angradXYZ[2]);
    }

    /**
     * Rotates this quaternion from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order and using chained rotation per axis:
     * <ul>
     *  <li>y - heading  - {@link #rotateByAngleY(float)}</li>
     *  <li>z - attitude - {@link #rotateByAngleZ(float)}</li>
     *  <li>x - bank     - {@link #rotateByAngleX(float)}</li>
     * </ul>
     * </p>
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> NOP if all angles are {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     *   <li> result is {@link #normalize()}ed</li>
     * </ul>
     * </p>
     * @param bankX the Euler pitch angle in radians. (rotation about the X axis)
     * @param headingY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
     * @return this quaternion for chaining.
     * @see #rotateByAngleY(float)
     * @see #rotateByAngleZ(float)
     * @see #rotateByAngleX(float)
     * @see #setFromEuler(float, float, float)
     */
    public final Quaternion rotateByEuler(final float bankX, final float headingY, final float attitudeZ) {
        if ( VectorUtil.isZero(bankX, headingY, attitudeZ, FloatUtil.EPSILON) ) {
            return this;
        } else {
            // setFromEuler muls: ( 8 + 4 ) , + quat muls 24 = 36
            // this:  8  + 8 + 8 + 4 = 28 muls
            return rotateByAngleY(headingY).rotateByAngleZ(attitudeZ).rotateByAngleX(bankX).normalize();
        }
    }

    /***
     * Rotate the given vector by this quaternion
     *
     * @param vecOut result float[3] storage for rotated vector, maybe equal to vecIn for in-place rotation
     * @param vecOutOffset offset in result storage
     * @param vecIn float[3] vector to be rotated
     * @param vecInOffset offset in vecIn
     * @return the given vecOut store for chaining
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q63">Matrix-FAQ Q63</a>
     */
    public final float[] rotateVector(final float[] vecOut, final int vecOutOffset, final float[] vecIn, final int vecInOffset) {
        if ( VectorUtil.isVec3Zero(vecIn, vecInOffset, FloatUtil.EPSILON) ) {
            vecOut[0+vecOutOffset] = 0f;
            vecOut[1+vecOutOffset] = 0f;
            vecOut[2+vecOutOffset] = 0f;
        } else {
            final float vecX = vecIn[0+vecInOffset];
            final float vecY = vecIn[1+vecInOffset];
            final float vecZ = vecIn[2+vecInOffset];
            final float x_x = x*x;
            final float y_y = y*y;
            final float z_z = z*z;
            final float w_w = w*w;

            vecOut[0+vecOutOffset] =   w_w * vecX
                                     + x_x * vecX
                                     - z_z * vecX
                                     - y_y * vecX
                                     + 2f * ( y*w*vecZ - z*w*vecY + y*x*vecY + z*x*vecZ );
                                     ;

            vecOut[1+vecOutOffset] =   y_y * vecY
                                     - z_z * vecY
                                     + w_w * vecY
                                     - x_x * vecY
                                     + 2f * ( x*y*vecX + z*y*vecZ + w*z*vecX - x*w*vecZ );
                                     ;

            vecOut[2+vecOutOffset] =   z_z * vecZ
                                     - y_y * vecZ
                                     - x_x * vecZ
                                     + w_w * vecZ
                                     + 2f * ( x*z*vecX + y*z*vecY - w*y*vecX + w*x*vecY );
                                     ;
        }
        return vecOut;
    }

    /**
     * Set this quaternion to a spherical linear interpolation
     * between the given start and end quaternions by the given change amount.
     * <p>
     * Note: Method <i>does not</i> normalize this quaternion!
     * </p>
     *
     * @param a start quaternion
     * @param b end  quaternion
     * @param changeAmnt float between 0 and 1 representing interpolation.
     * @return this quaternion for chaining.
     * @see <a href="http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/quaternions/slerp/">euclideanspace.com-QuaternionSlerp</a>
     */
    public final Quaternion setSlerp(final Quaternion a, final Quaternion b, final float changeAmnt) {
        // System.err.println("Slerp.0: A "+a+", B "+b+", t "+changeAmnt);
        if (changeAmnt == 0.0f) {
            set(a);
        } else if (changeAmnt == 1.0f) {
            set(b);
        } else {
            float bx = b.x;
            float by = b.y;
            float bz = b.z;
            float bw = b.w;

            // Calculate angle between them (quat dot product)
            float cosHalfTheta = a.x * bx + a.y * by + a.z * bz + a.w * bw;

            final float scale0, scale1;

            if( cosHalfTheta >= 0.95f ) {
                // quaternions are close, just use linear interpolation
                scale0 = 1.0f - changeAmnt;
                scale1 = changeAmnt;
                // System.err.println("Slerp.1: Linear Interpol; cosHalfTheta "+cosHalfTheta);
            } else if ( cosHalfTheta <= -0.99f ) {
                // the quaternions are nearly opposite,
                // we can pick any axis normal to a,b to do the rotation
                scale0 = 0.5f;
                scale1 = 0.5f;
                // System.err.println("Slerp.2: Any; cosHalfTheta "+cosHalfTheta);
            } else {
                // System.err.println("Slerp.3: cosHalfTheta "+cosHalfTheta);
                if( cosHalfTheta <= -FloatUtil.EPSILON ) { // FIXME: .. or shall we use the upper bound 'cosHalfTheta < FloatUtil.EPSILON' ?
                    // Negate the second quaternion and the result of the dot product (Inversion)
                    bx *= -1f;
                    by *= -1f;
                    bz *= -1f;
                    bw *= -1f;
                    cosHalfTheta *= -1f;
                    // System.err.println("Slerp.4: Inverted cosHalfTheta "+cosHalfTheta);
                }
                final float halfTheta = FloatUtil.acos(cosHalfTheta);
                final float sinHalfTheta = FloatUtil.sqrt(1.0f - cosHalfTheta*cosHalfTheta);
                // if theta = 180 degrees then result is not fully defined
                // we could rotate around any axis normal to qa or qb
                if ( Math.abs(sinHalfTheta) < 0.001f ){ // fabs is floating point absolute
                    scale0 = 0.5f;
                    scale1 = 0.5f;
                    // throw new InternalError("XXX"); // FIXME should not be reached due to above inversion ?
                } else {
                    // Calculate the scale for q1 and q2, according to the angle and
                    // it's sine value
                    scale0 = FloatUtil.sin((1f - changeAmnt) * halfTheta) / sinHalfTheta;
                    scale1 = FloatUtil.sin(changeAmnt * halfTheta) / sinHalfTheta;
                }
            }

            x = a.x * scale0 + bx * scale1;
            y = a.y * scale0 + by * scale1;
            z = a.z * scale0 + bz * scale1;
            w = a.w * scale0 + bw * scale1;
        }
        // System.err.println("Slerp.X: Result "+this);
        return this;
    }

    /**
     * Set this quaternion to equal the rotation required
     * to point the z-axis at <i>direction</i> and the y-axis to <i>up</i>.
     * <p>
     * Implementation generates a 3x3 matrix
     * and is equal with ProjectFloat's lookAt(..).<br/>
     * </p>
     * Implementation Details:
     * <ul>
     *   <li> result is {@link #normalize()}ed</li>
     * </ul>
     * </p>
     * @param directionIn where to <i>look</i> at
     * @param upIn a vector indicating the local <i>up</i> direction.
     * @param xAxisOut vector storing the <i>orthogonal</i> x-axis of the coordinate system.
     * @param yAxisOut vector storing the <i>orthogonal</i> y-axis of the coordinate system.
     * @param zAxisOut vector storing the <i>orthogonal</i> z-axis of the coordinate system.
     * @return this quaternion for chaining.
     * @see <a href="http://www.euclideanspace.com/maths/algebra/vectors/lookat/index.htm">euclideanspace.com-LookUp</a>
     */
    public Quaternion setLookAt(final float[] directionIn, final float[] upIn,
                                final float[] xAxisOut, final float[] yAxisOut, final float[] zAxisOut) {
        // Z = norm(dir)
        VectorUtil.normalizeVec3(zAxisOut, directionIn);

        // X = upIn x Z
        //     (borrow yAxisOut for upNorm)
        VectorUtil.normalizeVec3(yAxisOut, upIn);
        VectorUtil.crossVec3(xAxisOut, yAxisOut, zAxisOut);
        VectorUtil.normalizeVec3(xAxisOut);

        // Y = Z x X
        //
        VectorUtil.crossVec3(yAxisOut, zAxisOut, xAxisOut);
        VectorUtil.normalizeVec3(yAxisOut);

        /**
            final float m00 = xAxisOut[0];
            final float m01 = yAxisOut[0];
            final float m02 = zAxisOut[0];
            final float m10 = xAxisOut[1];
            final float m11 = yAxisOut[1];
            final float m12 = zAxisOut[1];
            final float m20 = xAxisOut[2];
            final float m21 = yAxisOut[2];
            final float m22 = zAxisOut[2];
         */
        return setFromAxes(xAxisOut, yAxisOut, zAxisOut).normalize();
    }

    //
    // Conversions
    //

    /**
     * Initialize this quaternion from two vectors
     * <pre>
     *   q = (s,v) = (v1•v2 , v1 × v2),
     *     angle = angle(v1, v2) = v1•v2
     *      axis = normal(v1 x v2)
     * </pre>
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if square vector-length is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @param v1 not normalized
     * @param v2 not normalized
     * @param tmpPivotVec float[3] temp storage for cross product
     * @param tmpNormalVec float[3] temp storage to normalize vector
     * @return this quaternion for chaining.
     */
    public final Quaternion setFromVectors(final float[] v1, final float[] v2, final float[] tmpPivotVec, final float[] tmpNormalVec) {
        final float factor = VectorUtil.normVec3(v1) * VectorUtil.normVec3(v2);
        if ( FloatUtil.isZero(factor, FloatUtil.EPSILON ) ) {
            return setIdentity();
        } else {
            final float dot = VectorUtil.dotVec3(v1, v2) / factor; // normalize
            final float theta = FloatUtil.acos(Math.max(-1.0f, Math.min(dot, 1.0f))); // clipping [-1..1]

            VectorUtil.crossVec3(tmpPivotVec, v1, v2);

            if ( dot < 0.0f && FloatUtil.isZero( VectorUtil.normVec3(tmpPivotVec), FloatUtil.EPSILON ) ) {
                // Vectors parallel and opposite direction, therefore a rotation of 180 degrees about any vector
                // perpendicular to this vector will rotate vector a onto vector b.
                //
                // The following guarantees the dot-product will be 0.0.
                int dominantIndex;
                if (Math.abs(v1[0]) > Math.abs(v1[1])) {
                    if (Math.abs(v1[0]) > Math.abs(v1[2])) {
                        dominantIndex = 0;
                    } else {
                        dominantIndex = 2;
                    }
                } else {
                    if (Math.abs(v1[1]) > Math.abs(v1[2])) {
                        dominantIndex = 1;
                    } else {
                        dominantIndex = 2;
                    }
                }
                tmpPivotVec[dominantIndex]           = -v1[(dominantIndex + 1) % 3];
                tmpPivotVec[(dominantIndex + 1) % 3] =  v1[dominantIndex];
                tmpPivotVec[(dominantIndex + 2) % 3] =  0f;
            }
            return setFromAngleAxis(theta, tmpPivotVec, tmpNormalVec);
        }
    }

    /**
     * Initialize this quaternion from two normalized vectors
     * <pre>
     *   q = (s,v) = (v1•v2 , v1 × v2),
     *     angle = angle(v1, v2) = v1•v2
     *      axis = v1 x v2
     * </pre>
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if square vector-length is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @param v1 normalized
     * @param v2 normalized
     * @param tmpPivotVec float[3] temp storage for cross product
     * @return this quaternion for chaining.
     */
    public final Quaternion setFromNormalVectors(final float[] v1, final float[] v2, final float[] tmpPivotVec) {
        final float factor = VectorUtil.normVec3(v1) * VectorUtil.normVec3(v2);
        if ( FloatUtil.isZero(factor, FloatUtil.EPSILON ) ) {
            return setIdentity();
        } else {
            final float dot = VectorUtil.dotVec3(v1, v2) / factor; // normalize
            final float theta = FloatUtil.acos(Math.max(-1.0f, Math.min(dot, 1.0f))); // clipping [-1..1]

            VectorUtil.crossVec3(tmpPivotVec, v1, v2);

            if ( dot < 0.0f && FloatUtil.isZero( VectorUtil.normVec3(tmpPivotVec), FloatUtil.EPSILON ) ) {
                // Vectors parallel and opposite direction, therefore a rotation of 180 degrees about any vector
                // perpendicular to this vector will rotate vector a onto vector b.
                //
                // The following guarantees the dot-product will be 0.0.
                int dominantIndex;
                if (Math.abs(v1[0]) > Math.abs(v1[1])) {
                    if (Math.abs(v1[0]) > Math.abs(v1[2])) {
                        dominantIndex = 0;
                    } else {
                        dominantIndex = 2;
                    }
                } else {
                    if (Math.abs(v1[1]) > Math.abs(v1[2])) {
                        dominantIndex = 1;
                    } else {
                        dominantIndex = 2;
                    }
                }
                tmpPivotVec[dominantIndex]           = -v1[(dominantIndex + 1) % 3];
                tmpPivotVec[(dominantIndex + 1) % 3] =  v1[dominantIndex];
                tmpPivotVec[(dominantIndex + 2) % 3] =  0f;
            }
            return setFromAngleNormalAxis(theta, tmpPivotVec);
        }
    }

    /***
     * Initialize this quaternion with given non-normalized axis vector and rotation angle
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if axis is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @param angle rotation angle (rads)
     * @param vector axis vector not normalized
     * @param tmpV3f float[3] temp storage to normalize vector
     * @return this quaternion for chaining.
     *
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q56">Matrix-FAQ Q56</a>
     * @see #toAngleAxis(float[])
     */
    public final Quaternion setFromAngleAxis(final float angle, final float[] vector, final float[] tmpV3f) {
        VectorUtil.normalizeVec3(tmpV3f, vector);
        return setFromAngleNormalAxis(angle, tmpV3f);
    }

    /***
     * Initialize this quaternion with given normalized axis vector and rotation angle
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if axis is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     * @param angle rotation angle (rads)
     * @param vector axis vector normalized
     * @return this quaternion for chaining.
     *
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q56">Matrix-FAQ Q56</a>
     * @see #toAngleAxis(float[])
     */
    public final Quaternion setFromAngleNormalAxis(final float angle, final float[] vector) {
        if ( VectorUtil.isVec3Zero(vector, 0, FloatUtil.EPSILON) ) {
            setIdentity();
        } else {
            final float halfangle = angle * 0.5f;
            final float sin = FloatUtil.sin(halfangle);
            x = vector[0] * sin;
            y = vector[1] * sin;
            z = vector[2] * sin;
            w = FloatUtil.cos(halfangle);
        }
        return this;
    }

    /**
     * Transform the rotational quaternion to axis based rotation angles
     *
     * @param axis float[3] storage for computed axis
     * @return the rotation angle in radians
     * @see #setFromAngleAxis(float, float[], float[])
     */
    public final float toAngleAxis(final float[] axis) {
        final float sqrLength = x*x + y*y + z*z;
        float angle;
        if ( FloatUtil.isZero(sqrLength, FloatUtil.EPSILON) ) { // length is ~0
            angle = 0.0f;
            axis[0] = 1.0f;
            axis[1] = 0.0f;
            axis[2] = 0.0f;
        } else {
            angle = FloatUtil.acos(w) * 2.0f;
            final float invLength = 1.0f / FloatUtil.sqrt(sqrLength);
            axis[0] = x * invLength;
            axis[1] = y * invLength;
            axis[2] = z * invLength;
        }
        return angle;
    }

    /**
     * Initializes this quaternion from the given Euler rotation array <code>angradXYZ</code> in radians.
     * <p>
     * The <code>angradXYZ</code> array is laid out in natural order:
     * <ul>
     *  <li>x - bank</li>
     *  <li>y - heading</li>
     *  <li>z - attitude</li>
     * </ul>
     * </p>
     * For details see {@link #setFromEuler(float, float, float)}.
     * @param angradXYZ euler angel array in radians
     * @return this quaternion for chaining.
     * @see #setFromEuler(float, float, float)
     */
    public final Quaternion setFromEuler(final float[] angradXYZ) {
        return setFromEuler(angradXYZ[0], angradXYZ[1], angradXYZ[2]);
    }

    /**
     * Initializes this quaternion from the given Euler rotation angles in radians.
     * <p>
     * The rotations are applied in the given order:
     * <ul>
     *  <li>y - heading</li>
     *  <li>z - attitude</li>
     *  <li>x - bank</li>
     * </ul>
     * </p>
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> {@link #setIdentity()} if all angles are {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     *   <li> result is {@link #normalize()}ed</li>
     * </ul>
     * </p>
     * @param bankX the Euler pitch angle in radians. (rotation about the X axis)
     * @param headingY the Euler yaw angle in radians. (rotation about the Y axis)
     * @param attitudeZ the Euler roll angle in radians. (rotation about the Z axis)
     * @return this quaternion for chaining.
     *
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q60">Matrix-FAQ Q60</a>
     * @see <a href="http://vered.rose.utoronto.ca/people/david_dir/GEMS/GEMS.html">Gems</a>
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/eulerToQuaternion/index.htm">euclideanspace.com-eulerToQuaternion</a>
     * @see #toEuler(float[])
     */
    public final Quaternion setFromEuler(final float bankX, final float headingY, final float attitudeZ) {
        if ( VectorUtil.isZero(bankX, headingY, attitudeZ, FloatUtil.EPSILON) ) {
            return setIdentity();
        } else {
            float angle = headingY * 0.5f;
            final float sinHeadingY = FloatUtil.sin(angle);
            final float cosHeadingY = FloatUtil.cos(angle);
            angle = attitudeZ * 0.5f;
            final float sinAttitudeZ = FloatUtil.sin(angle);
            final float cosAttitudeZ = FloatUtil.cos(angle);
            angle = bankX * 0.5f;
            final float sinBankX = FloatUtil.sin(angle);
            final float cosBankX = FloatUtil.cos(angle);

            // variables used to reduce multiplication calls.
            final float cosHeadingXcosAttitude = cosHeadingY * cosAttitudeZ;
            final float sinHeadingXsinAttitude = sinHeadingY * sinAttitudeZ;
            final float cosHeadingXsinAttitude = cosHeadingY * sinAttitudeZ;
            final float sinHeadingXcosAttitude = sinHeadingY * cosAttitudeZ;

            w = cosHeadingXcosAttitude * cosBankX - sinHeadingXsinAttitude * sinBankX;
            x = cosHeadingXcosAttitude * sinBankX + sinHeadingXsinAttitude * cosBankX;
            y = sinHeadingXcosAttitude * cosBankX + cosHeadingXsinAttitude * sinBankX;
            z = cosHeadingXsinAttitude * cosBankX - sinHeadingXcosAttitude * sinBankX;
            return normalize();
        }
    }

    /**
     * Transform this quaternion to Euler rotation angles in radians (pitchX, yawY and rollZ).
     *
     * @param result the float[] array storing the computed angles.
     * @return the double[] array, filled with heading, attitude and bank in that order..
     * @see <a href="http://www.euclideanspace.com/maths/geometry/rotations/conversions/quaternionToEuler/index.htm">euclideanspace.com-quaternionToEuler</a>
     * @see #setFromEuler(float, float, float)
     */
    public float[] toEuler(final float[] result) {
        final float sqw = w*w;
        final float sqx = x*x;
        final float sqy = y*y;
        final float sqz = z*z;
        final float unit = sqx + sqy + sqz + sqw; // if normalized is one, otherwise
        // is correction factor
        final float test = x*y + z*w;

        if (test > 0.499f * unit) { // singularity at north pole
            result[0] =  0f;
            result[1] =  2f * FloatUtil.atan2(x, w);
            result[2] =  FloatUtil.HALF_PI;
        } else if (test < -0.499f * unit) { // singularity at south pole
            result[0] =  0f;
            result[1] = -2 * FloatUtil.atan2(x, w);
            result[2] = -FloatUtil.HALF_PI;
        } else {
            result[0] = FloatUtil.atan2(2f * x * w - 2 * y * z, -sqx + sqy - sqz + sqw);
            result[1] = FloatUtil.atan2(2f * y * w - 2 * x * z,  sqx - sqy - sqz + sqw);
            result[2] = FloatUtil.asin( 2f * test / unit);
        }
        return result;
    }

    /**
     * Initializes this quaternion from a 4x4 column rotation matrix
     * <p>
     * See <a href="ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z">Graphics Gems Code</a>,<br/>
     * <a href="http://mathworld.wolfram.com/MatrixTrace.html">MatrixTrace</a>.
     * </p>
     * <p>
     * Buggy <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q55">Matrix-FAQ Q55</a>
     * </p>
     *
     * @param m 4x4 column matrix
     * @return this quaternion for chaining.
     * @see #toMatrix(float[], int)
     */
    public final Quaternion setFromMatrix(final float[] m, final int m_off) {
        return setFromMatrix(m[0+0*4+m_off], m[0+1*4+m_off], m[0+2*4+m_off],
                             m[1+0*4+m_off], m[1+1*4+m_off], m[1+2*4+m_off],
                             m[2+0*4+m_off], m[2+1*4+m_off], m[2+2*4+m_off]);
    }

    /**
     * Compute the quaternion from a 3x3 column rotation matrix
     * <p>
     * See <a href="ftp://ftp.cis.upenn.edu/pub/graphics/shoemake/quatut.ps.Z">Graphics Gems Code</a>,<br/>
     * <a href="http://mathworld.wolfram.com/MatrixTrace.html">MatrixTrace</a>.
     * </p>
     * <p>
     * Buggy <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q55">Matrix-FAQ Q55</a>
     * </p>
     *
     * @return this quaternion for chaining.
     * @see #toMatrix(float[], int)
     */
    public Quaternion setFromMatrix(final float m00, final float m01, final float m02,
                                    final float m10, final float m11, final float m12,
                                    final float m20, final float m21, final float m22) {
        // Note: Other implementations uses 'T' w/o '+1f' and compares 'T >= 0' while adding missing 1f in sqrt expr.
        //       However .. this causes setLookAt(..) to fail and actually violates the 'trace definition'.

        // The trace T is the sum of the diagonal elements; see
        // http://mathworld.wolfram.com/MatrixTrace.html
        final float T = m00 + m11 + m22 + 1f;
        // System.err.println("setFromMatrix.0 T "+T+", m00 "+m00+", m11 "+m11+", m22 "+m22);
        if ( T > 0f ) {
            // System.err.println("setFromMatrix.1");
            final float S = 0.5f / FloatUtil.sqrt(T);  // S = 1 / ( 2 t )
            w = 0.25f / S;                             // w = 1 / ( 4 S ) = t / 2
            x = ( m21 - m12 ) * S;
            y = ( m02 - m20 ) * S;
            z = ( m10 - m01 ) * S;
        } else if ( m00 > m11 && m00 > m22) {
            // System.err.println("setFromMatrix.2");
            final float S = 0.5f / FloatUtil.sqrt(1.0f + m00 - m11 - m22); // S=4*qx
            w = ( m21 - m12 ) * S;
            x = 0.25f / S;
            y = ( m10 + m01 ) * S;
            z = ( m02 + m20 ) * S;
        } else if ( m11 > m22 ) {
            // System.err.println("setFromMatrix.3");
            final float S = 0.5f / FloatUtil.sqrt(1.0f + m11 - m00 - m22); // S=4*qy
            w = ( m02 - m20 ) * S;
            x = ( m20 + m01 ) * S;
            y = 0.25f / S;
            z = ( m21 + m12 ) * S;
        } else {
            // System.err.println("setFromMatrix.3");
            final float S = 0.5f / FloatUtil.sqrt(1.0f + m22 - m00 - m11); // S=4*qz
            w = ( m10 - m01 ) * S;
            x = ( m02 + m20 ) * S;
            y = ( m21 + m12 ) * S;
            z = 0.25f / S;
        }
        return this;
    }

    /**
     * Transform this quaternion to a normalized 4x4 column matrix representing the rotation.
     * <p>
     * Implementation Details:
     * <ul>
     *   <li> makes identity matrix if {@link #magnitudeSquared()} is {@link FloatUtil#isZero(float, float) is zero} using {@link FloatUtil#EPSILON epsilon}</li>
     * </ul>
     * </p>
     *
     * @param matrix float[16] store for the resulting normalized column matrix 4x4
     * @param mat_offset
     * @return the given matrix store
     * @see <a href="http://web.archive.org/web/20041029003853/http://www.j3d.org/matrix_faq/matrfaq_latest.html#Q54">Matrix-FAQ Q54</a>
     * @see #setFromMatrix(float[], int)
     */
    public final float[] toMatrix(final float[] matrix, final int mat_offset) {
        // pre-multiply scaled-reciprocal-magnitude to reduce multiplications
        final float norm = magnitudeSquared();
        if ( FloatUtil.isZero(norm, FloatUtil.EPSILON) ) {
            // identity matrix -> srecip = 0f
            return FloatUtil.makeIdentity(matrix, mat_offset);
        }
        final float srecip;
        if ( FloatUtil.isEqual(1f, norm, FloatUtil.EPSILON) ) {
            srecip = 2f;
        } else {
            srecip = 2.0f / norm;
        }

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

        matrix[0+0*4+mat_offset]  = 1f - ( yy + zz );
        matrix[0+1*4+mat_offset]  =      ( xy - zw );
        matrix[0+2*4+mat_offset]  =      ( xz + yw );
        matrix[0+3*4+mat_offset]  = 0f;

        matrix[1+0*4+mat_offset]  =      ( xy + zw );
        matrix[1+1*4+mat_offset]  = 1f - ( xx + zz );
        matrix[1+2*4+mat_offset]  =      ( yz - xw );
        matrix[1+3*4+mat_offset]  = 0f;

        matrix[2+0*4+mat_offset]  =      ( xz - yw );
        matrix[2+1*4+mat_offset]  =      ( yz + xw );
        matrix[2+2*4+mat_offset]  = 1f - ( xx + yy );
        matrix[2+3*4+mat_offset]  = 0f;

        matrix[3+0*4+mat_offset]  = 0f;
        matrix[3+1*4+mat_offset]  = 0f;
        matrix[3+2*4+mat_offset]  = 0f;
        matrix[3+3*4+mat_offset]  = 1f;
        return matrix;
    }

    /**
     * @param index the 3x3 rotation matrix column to retrieve from this quaternion (normalized). Must be between 0 and 2.
     * @param result the vector object to store the result in.
     * @return the result column-vector for chaining.
     */
    public float[] copyMatrixColumn(final int index, final float[] result, final int resultOffset) {
        // pre-multipliy scaled-reciprocal-magnitude to reduce multiplications
        final float norm = magnitudeSquared();
        final float srecip;
        if ( FloatUtil.isZero(norm, FloatUtil.EPSILON) ) {
            srecip= 0f;
        } else if ( FloatUtil.isEqual(1f, norm, FloatUtil.EPSILON) ) {
            srecip= 2f;
        } else {
            srecip= 2.0f / norm;
        }

        // compute xs/ys/zs first to save 6 multiplications, since xs/ys/zs
        // will be used 2-4 times each.
        final float xs = x * srecip;
        final float ys = y * srecip;
        final float zs = z * srecip;
        final float xx = x * xs;
        final float xy = x * ys;
        final float xz = x * zs;
        final float xw = w * xs;
        final float yy = y * ys;
        final float yz = y * zs;
        final float yw = w * ys;
        final float zz = z * zs;
        final float zw = w * zs;

        // using s=2/norm (instead of 1/norm) saves 3 multiplications by 2 here
        switch (index) {
        case 0:
            result[0+resultOffset] = 1.0f - (yy + zz);
            result[1+resultOffset] = xy + zw;
            result[2+resultOffset] = xz - yw;
            break;
        case 1:
            result[0+resultOffset] = xy - zw;
            result[1+resultOffset] = 1.0f - (xx + zz);
            result[2+resultOffset] = yz + xw;
            break;
        case 2:
            result[0+resultOffset] = xz + yw;
            result[1+resultOffset] = yz - xw;
            result[2+resultOffset] = 1.0f - (xx + yy);
            break;
        default:
            throw new IllegalArgumentException("Invalid column index. " + index);
        }
        return result;
    }

    /**
     * Initializes this quaternion to represent a rotation formed by the given three <i>orthogonal</i> axes.
     * <p>
     * No validation whether the axes are <i>orthogonal</i> is performed.
     * </p>
     *
     * @param xAxis vector representing the <i>orthogonal</i> x-axis of the coordinate system.
     * @param yAxis vector representing the <i>orthogonal</i> y-axis of the coordinate system.
     * @param zAxis vector representing the <i>orthogonal</i> z-axis of the coordinate system.
     * @return this quaternion for chaining.
     */
    public final Quaternion setFromAxes(final float[] xAxis, final float[] yAxis, final float[] zAxis) {
        return setFromMatrix(xAxis[0], yAxis[0], zAxis[0],
                             xAxis[1], yAxis[1], zAxis[1],
                             xAxis[2], yAxis[2], zAxis[2]);
    }

    /**
     * Extracts this quaternion's <i>orthogonal</i> rotation axes.
     *
     * @param xAxis vector representing the <i>orthogonal</i> x-axis of the coordinate system.
     * @param yAxis vector representing the <i>orthogonal</i> y-axis of the coordinate system.
     * @param zAxis vector representing the <i>orthogonal</i> z-axis of the coordinate system.
     * @param tmpMat4 temporary float[4] matrix, used to transform this quaternion to a matrix.
     */
    public void toAxes(final float[] xAxis, final float[] yAxis, final float[] zAxis, final float[] tmpMat4) {
        toMatrix(tmpMat4, 0);
        FloatUtil.copyMatrixColumn(tmpMat4, 0, 2, zAxis, 0);
        FloatUtil.copyMatrixColumn(tmpMat4, 0, 1, yAxis, 0);
        FloatUtil.copyMatrixColumn(tmpMat4, 0, 0, xAxis, 0);
    }

    /**
     * Check if the the 3x3 matrix (param) is in fact an affine rotational
     * matrix
     *
     * @param m 3x3 column matrix
     * @return true if representing a rotational matrix, false otherwise
     */
    public final boolean isRotationMatrix3f(final float[] m) {
        final float epsilon = 0.01f; // margin to allow for rounding errors
        if (FloatUtil.abs(m[0] * m[3] + m[3] * m[4] + m[6] * m[7]) > epsilon)
            return false;
        if (FloatUtil.abs(m[0] * m[2] + m[3] * m[5] + m[6] * m[8]) > epsilon)
            return false;
        if (FloatUtil.abs(m[1] * m[2] + m[4] * m[5] + m[7] * m[8]) > epsilon)
            return false;
        if (FloatUtil.abs(m[0] * m[0] + m[3] * m[3] + m[6] * m[6] - 1) > epsilon)
            return false;
        if (FloatUtil.abs(m[1] * m[1] + m[4] * m[4] + m[7] * m[7] - 1) > epsilon)
            return false;
        if (FloatUtil.abs(m[2] * m[2] + m[5] * m[5] + m[8] * m[8] - 1) > epsilon)
            return false;
        return (FloatUtil.abs(determinant3f(m) - 1) < epsilon);
    }

    private final float determinant3f(final float[] m) {
        return m[0] * m[4] * m[8] + m[3] * m[7] * m[2] + m[6] * m[1] * m[5]
             - m[0] * m[7] * m[5] - m[3] * m[1] * m[8] - m[6] * m[4] * m[2];
    }

    //
    // std java overrides
    //

    /**
     * @param o the object to compare for equality
     * @return true if this quaternion and the provided quaternion have roughly the same x, y, z and w values.
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Quaternion)) {
            return false;
        }
        final Quaternion comp = (Quaternion) o;
        return Math.abs(x - comp.getX()) <= ALLOWED_DEVIANCE &&
               Math.abs(y - comp.getY()) <= ALLOWED_DEVIANCE &&
               Math.abs(z - comp.getZ()) <= ALLOWED_DEVIANCE &&
               Math.abs(w - comp.getW()) <= ALLOWED_DEVIANCE;
    }
    @Override
    public final int hashCode() {
        throw new InternalError("hashCode not designed");
    }

    public String toString() {
        return "Quaternion[x "+x+", y "+y+", z "+z+", w "+w+"]";
    }
}
