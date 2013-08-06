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

public class Quaternion {
    protected float x, y, z, w;

    public Quaternion() {
        setIdentity();
    }
    
    public Quaternion(Quaternion q) {
        x = q.x;
        y = q.y;
        z = q.z;
        w = q.w;
    }

    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**
     * Constructor to create a rotation based quaternion from two vectors
     * 
     * @param vector1
     * @param vector2
     */
    public Quaternion(float[] vector1, float[] vector2) {
        final float theta = FloatUtil.acos(VectorUtil.dot(vector1, vector2));
        final float[] cross = VectorUtil.cross(vector1, vector2);
        fromAxis(cross, theta);
    }
    
    /***
     * Constructor to create a rotation based quaternion from axis vector and angle
     * @param vector axis vector
     * @param angle rotation angle (rads)
     * @see #fromAxis(float[], float)
     */
    public Quaternion(float[] vector, float angle) {
        fromAxis(vector, angle);
    }
    
    /***
     * Initialize this quaternion with given axis vector and rotation angle
     * 
     * @param vector axis vector
     * @param angle rotation angle (rads)
     */
    public void fromAxis(float[] vector, float angle) {
        final float halfangle = angle * 0.5f;
        final float sin = FloatUtil.sin(halfangle);
        final float[] nv = VectorUtil.normalize(vector);
        x = (nv[0] * sin);
        y = (nv[1] * sin);
        z = (nv[2] * sin);
        w = FloatUtil.cos(halfangle);
    }

    /**
     * Transform the rotational quaternion to axis based rotation angles
     * 
     * @return new float[4] with ,theta,Rx,Ry,Rz
     */
    public float[] toAxis() {
        final float[] vec = new float[4];
        final float scale = FloatUtil.sqrt(x * x + y * y + z * z);
        vec[0] = FloatUtil.acos(w) * 2.0f;
        vec[1] = x / scale;
        vec[2] = y / scale;
        vec[3] = z / scale;
        return vec;
    }

    public float getW() {
        return w;
    }

    public void setW(float w) {
        this.w = w;
    }

    public float getX() {
        return x;
    }

    public void setX(float x) {
        this.x = x;
    }

    public float getY() {
        return y;
    }

    public void setY(float y) {
        this.y = y;
    }

    public float getZ() {
        return z;
    }

    public void setZ(float z) {
        this.z = z;
    }

    /**
     * Add a quaternion
     * 
     * @param q quaternion
     */
    public void add(Quaternion q) {
        x += q.x;
        y += q.y;
        z += q.z;
    }

    /**
     * Subtract a quaternion
     * 
     * @param q quaternion
     */
    public void subtract(Quaternion q) {
        x -= q.x;
        y -= q.y;
        z -= q.z;
    }

    /**
     * Divide a quaternion by a constant
     * 
     * @param n a float to divide by
     */
    public void divide(float n) {
        x /= n;
        y /= n;
        z /= n;
    }

    /**
     * Multiply this quaternion by the param quaternion
     * 
     * @param q a quaternion to multiply with
     */
    public void mult(Quaternion q) {
        final float w1 = w * q.w - x * q.x - y * q.y - z * q.z;

        final float x1 = w * q.x + x * q.w + y * q.z - z * q.y;
        final float y1 = w * q.y - x * q.z + y * q.w + z * q.x;
        final float z1 = w * q.z + x * q.y - y * q.x + z * q.w;

        w = w1;
        x = x1;
        y = y1;
        z = z1;
    }

    /**
     * Multiply a quaternion by a constant
     * 
     * @param n a float constant
     */
    public void mult(float n) {
        x *= n;
        y *= n;
        z *= n;
    }
    
    /***
     * Rotate given vector by this quaternion
     * 
     * @param vector input vector
     * @return rotated vector
     */
    public float[] mult(float[] vector) {
        // TODO : optimize
        final float[] res = new float[3];
        final Quaternion a = new Quaternion(vector[0], vector[1], vector[2], 0.0f);
        final Quaternion b = new Quaternion(this);
        final Quaternion c = new Quaternion(this);
        b.inverse();
        a.mult(b);
        c.mult(a);
        res[0] = c.x;
        res[1] = c.y;
        res[2] = c.z;
        return res;
    }

    /**
     * Normalize a quaternion required if to be used as a rotational quaternion
     */
    public void normalize() {
        final float norme = (float) FloatUtil.sqrt(w * w + x * x + y * y + z * z);
        if (norme == 0.0f) {
            setIdentity();
        } else {
            final float recip = 1.0f / norme;

            w *= recip;
            x *= recip;
            y *= recip;
            z *= recip;
        }
    }

    /**
     * Invert the quaternion If rotational, will produce a the inverse rotation
     */
    public void inverse() {
        final float norm = w * w + x * x + y * y + z * z;

        final float recip = 1.0f / norm;

        w *= recip;
        x = -1 * x * recip;
        y = -1 * y * recip;
        z = -1 * z * recip;
    }

    /**
     * Transform this quaternion to a 4x4 column matrix representing the
     * rotation
     * 
     * @return new float[16] column matrix 4x4
     */
    public float[] toMatrix() {
        final float[] matrix = new float[16];
        matrix[0] = 1.0f - 2 * y * y - 2 * z * z;
        matrix[1] = 2 * x * y + 2 * w * z;
        matrix[2] = 2 * x * z - 2 * w * y;
        matrix[3] = 0;

        matrix[4] = 2 * x * y - 2 * w * z;
        matrix[5] = 1.0f - 2 * x * x - 2 * z * z;
        matrix[6] = 2 * y * z + 2 * w * x;
        matrix[7] = 0;

        matrix[8] = 2 * x * z + 2 * w * y;
        matrix[9] = 2 * y * z - 2 * w * x;
        matrix[10] = 1.0f - 2 * x * x - 2 * y * y;
        matrix[11] = 0;

        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;
        return matrix;
    }

    /**
     * Set this quaternion from a Sphereical interpolation of two param
     * quaternion, used mostly for rotational animation.
     * <p>
     * Note: Method does not normalize this quaternion!
     * </p>
     * <p>
     * See http://www.euclideanspace.com/maths/algebra/realNormedAlgebra/
     * quaternions/slerp/
     * </p>
     * 
     * @param a initial quaternion
     * @param b target quaternion
     * @param t float between 0 and 1 representing interp.
     */
    public void slerp(Quaternion a, Quaternion b, float t) {
        final float cosom = a.x * b.x + a.y * b.y + a.z * b.z + a.w * b.w;
        final float t1 = 1.0f - t;

        // if the two quaternions are close, just use linear interpolation
        if (cosom >= 0.95f) {
            x = a.x * t1 + b.x * t;
            y = a.y * t1 + b.y * t;
            z = a.z * t1 + b.z * t;
            w = a.w * t1 + b.w * t;
            return;
        }

        // the quaternions are nearly opposite, we can pick any axis normal to
        // a,b
        // to do the rotation
        if (cosom <= -0.99f) {
            x = 0.5f * (a.x + b.x);
            y = 0.5f * (a.y + b.y);
            z = 0.5f * (a.z + b.z);
            w = 0.5f * (a.w + b.w);
            return;
        }

        // cosom is now withion range of acos, do a SLERP
        final float sinom = FloatUtil.sqrt(1.0f - cosom * cosom);
        final float omega = FloatUtil.acos(cosom);

        final float scla = FloatUtil.sin(t1 * omega) / sinom;
        final float sclb = FloatUtil.sin(t * omega) / sinom;

        x = a.x * scla + b.x * sclb;
        y = a.y * scla + b.y * sclb;
        z = a.z * scla + b.z * sclb;
        w = a.w * scla + b.w * sclb;
    }

    /**
     * Check if this quaternion represents an identity matrix for rotation,
     * , ie (0,0,0,1).
     * 
     * @return true if it is an identity rep., false otherwise
     */
    public boolean isIdentity() {
        return w == 1 && x == 0 && y == 0 && z == 0;
    }
    
    /***
     * Set this quaternion to identity (x=0,y=0,z=0,w=1)
     */
    public void setIdentity() {
        x = y = z = 0;
        w = 1;
    }

    /**
     * compute the quaternion from a 3x3 column matrix
     * 
     * @param m 3x3 column matrix
     */
    public void setFromMatrix(float[] m) {
        final float T = m[0] + m[4] + m[8] + 1;
        if (T > 0) {
            final float S = 0.5f / (float) FloatUtil.sqrt(T);
            w = 0.25f / S;
            x = (m[5] - m[7]) * S;
            y = (m[6] - m[2]) * S;
            z = (m[1] - m[3]) * S;
        } else {
            if ((m[0] > m[4]) & (m[0] > m[8])) {
                final float S = FloatUtil.sqrt(1.0f + m[0] - m[4] - m[8]) * 2f; // S=4*qx
                w = (m[7] - m[5]) / S;
                x = 0.25f * S;
                y = (m[3] + m[1]) / S;
                z = (m[6] + m[2]) / S;
            } else if (m[4] > m[8]) {
                final float S = FloatUtil.sqrt(1.0f + m[4] - m[0] - m[8]) * 2f; // S=4*qy
                w = (m[6] - m[2]) / S;
                x = (m[3] + m[1]) / S;
                y = 0.25f * S;
                z = (m[7] + m[5]) / S;
            } else {
                final float S = FloatUtil.sqrt(1.0f + m[8] - m[0] - m[4]) * 2f; // S=4*qz
                w = (m[3] - m[1]) / S;
                x = (m[6] + m[2]) / S;
                y = (m[7] + m[5]) / S;
                z = 0.25f * S;
            }
        }
    }

    /**
     * Check if the the 3x3 matrix (param) is in fact an affine rotational
     * matrix
     * 
     * @param m 3x3 column matrix
     * @return true if representing a rotational matrix, false otherwise
     */
    public boolean isRotationMatrix(float[] m) {
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
        return (FloatUtil.abs(determinant(m) - 1) < epsilon);
    }

    private float determinant(float[] m) {
        return m[0] * m[4] * m[8] + m[3] * m[7] * m[2] + m[6] * m[1] * m[5]
             - m[0] * m[7] * m[5] - m[3] * m[1] * m[8] - m[6] * m[4] * m[2];
    }
}
