/**
 * Copyright 2022-2023 JogAmp Community. All rights reserved.
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

/**
 * 4D Vector based upon four float components.
 *
 * Implementation borrowed from [gfxbox2](https://jausoft.com/cgit/cs_class/gfxbox2.git/tree/include/pixel/pixel3f.hpp#n29)
 * and its data layout from JOAL's Vec3f.
 */
public final class Vec4f {
    private float x;
    private float y;
    private float z;
    private float w;

    public Vec4f() {}

    public Vec4f(final Vec4f o) {
        set(o);
    }

    /** Creating new Vec4f using { o, w }. */
    public Vec4f(final Vec3f o, final float w) {
        set(o, w);
    }

    public Vec4f copy() {
        return new Vec4f(this);
    }

    public Vec4f(final float[/*4*/] xyzw) {
        set(xyzw);
    }

    public Vec4f(final float x, final float y, final float z, final float w) {
        set(x, y, z, w);
    }

    /** this = o, returns this. */
    public Vec4f set(final Vec4f o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        this.w = o.w;
        return this;
    }

    /** this = { o, w }, returns this. */
    public Vec4f set(final Vec3f o, final float w) {
        this.x = o.x();
        this.y = o.y();
        this.z = o.z();
        this.w = w;
        return this;
    }

    /** this = { x, y, z, w }, returns this. */
    public Vec4f set(final float x, final float y, final float z, final float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
        return this;
    }

    /** this = xyzw, returns this. */
    public Vec4f set(final float[/*4*/] xyzw) {
        this.x = xyzw[0];
        this.y = xyzw[1];
        this.z = xyzw[2];
        this.w = xyzw[3];
        return this;
    }

    /** Sets the ith component, 0 <= i < 4 */
    public void set(final int i, final float val) {
        switch (i) {
            case 0: x = val; break;
            case 1: y = val; break;
            case 2: z = val; break;
            case 3: w = val; break;
            default: throw new IndexOutOfBoundsException();
        }
    }

    /** xyzw = this, returns xyzw. */
    public float[] get(final float[/*4*/] xyzw) {
        xyzw[0] = this.x;
        xyzw[1] = this.y;
        xyzw[2] = this.z;
        xyzw[3] = this.w;
        return xyzw;
    }

    /** Gets the ith component, 0 <= i < 4 */
    public float get(final int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            case 2: return z;
            case 3: return w;
            default: throw new IndexOutOfBoundsException();
        }
    }

    public float x() { return x; }
    public float y() { return y; }
    public float z() { return z; }
    public float w() { return w; }

    public void setX(final float x) { this.x = x; }
    public void setY(final float y) { this.y = y; }
    public void setZ(final float z) { this.z = z; }
    public void setW(final float w) { this.w = w; }

    /** this = max(this, m), returns this. */
    public Vec4f max(final Vec4f m) {
        this.x = Math.max(this.x, m.x);
        this.y = Math.max(this.y, m.y);
        this.z = Math.max(this.z, m.z);
        this.w = Math.max(this.w, m.w);
        return this;
    }
    /** this = min(this, m), returns this. */
    public Vec4f min(final Vec4f m) {
        this.x = Math.min(this.x, m.x);
        this.y = Math.min(this.y, m.y);
        this.z = Math.min(this.z, m.z);
        this.w = Math.min(this.w, m.w);
        return this;
    }

    /** Returns this * val; creates new vector */
    public Vec4f mul(final float val) {
        return new Vec4f(this).scale(val);
    }

    /** this = a * b, returns this. */
    public Vec4f mul(final Vec4f a, final Vec4f b) {
        x = a.x * b.x;
        y = a.y * b.y;
        z = a.z * b.z;
        w = a.w * b.w;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec4f mul(final Vec4f s) { return mul(s.x, s.y, s.z, s.w); }

    /** this = this * { sx, sy, sz, sw }, returns this. */
    public Vec4f mul(final float sx, final float sy, final float sz, final float sw) {
        x *= sx;
        y *= sy;
        z *= sz;
        w *= sw;
        return this;
    }

    /** this = a / b, returns this. */
    public Vec4f div(final Vec4f a, final Vec4f b) {
        x = a.x / b.x;
        y = a.y / b.y;
        z = a.z / b.z;
        w = a.w / b.w;
        return this;
    }

    /** this = this / a, returns this. */
    public Vec4f div(final Vec4f a) {
        x /= a.x;
        y /= a.y;
        z /= a.z;
        w /= a.w;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec4f scale(final float s) {
        x *= s;
        y *= s;
        z *= s;
        w *= s;
        return this;
    }

    /** Returns this + arg; creates new vector */
    public Vec4f plus(final Vec4f arg) {
        return new Vec4f(this).add(arg);
    }

    /** this = a + b, returns this. */
    public Vec4f plus(final Vec4f a, final Vec4f b) {
        x = a.x + b.x;
        y = a.y + b.y;
        z = a.z + b.z;
        w = a.w + b.w;
        return this;
    }

    /** this = this + { dx, dy, dz, dw }, returns this. */
    public Vec4f add(final float dx, final float dy, final float dz, final float dw) {
        x += dx;
        y += dy;
        z += dz;
        w += dw;
        return this;
    }

    /** this = this + b, returns this. */
    public Vec4f add(final Vec4f b) {
        x += b.x;
        y += b.y;
        z += b.z;
        w += b.w;
        return this;
    }

    /** Returns this - arg; creates new vector */
    public Vec4f minus(final Vec4f arg) {
        return new Vec4f(this).sub(arg);
    }

    /** this = a - b, returns this. */
    public Vec4f minus(final Vec4f a, final Vec4f b) {
        x = a.x - b.x;
        y = a.y - b.y;
        z = a.z - b.z;
        w = a.w - b.w;
        return this;
    }

    /** this = this - b, returns this. */
    public Vec4f sub(final Vec4f b) {
        x -= b.x;
        y -= b.y;
        z -= b.z;
        w -= b.w;
        return this;
    }

    /** Return true if all components are zero, i.e. it's absolute value < {@link #EPSILON}. */
    public boolean isZero() {
        return FloatUtil.isZero(x) && FloatUtil.isZero(y) && FloatUtil.isZero(z) && FloatUtil.isZero(w);
    }

    /**
     * Return the length of this vector, a.k.a the <i>norm</i> or <i>magnitude</i>
     */
    public float length() {
        return (float) Math.sqrt(lengthSq());
    }

    /**
     * Return the squared length of this vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public float lengthSq() {
        return x*x + y*y + z*z + w*w;
    }

    /**
     * Normalize this vector in place
     */
    public Vec4f normalize() {
        final float lengthSq = lengthSq();
        if ( FloatUtil.isZero( lengthSq ) ) {
            x = 0.0f;
            y = 0.0f;
            z = 0.0f;
            w = 0.0f;
        } else {
            final float invSqr = 1.0f / (float)Math.sqrt(lengthSq);
            x *= invSqr;
            y *= invSqr;
            z *= invSqr;
            w *= invSqr;
        }
        return this;
    }

    /**
     * Return the squared distance between this vector and the given one.
     * <p>
     * When comparing the relative distance between two points it is usually sufficient to compare the squared
     * distances, thus avoiding an expensive square root operation.
     * </p>
     */
    public float distSq(final Vec4f o) {
        final float dx = x - o.x;
        final float dy = y - o.y;
        final float dz = z - o.z;
        final float dw = w - o.w;
        return dx*dx + dy*dy + dz*dz + dw*dw;
    }

    /**
     * Return the distance between this vector and the given one.
     */
    public float dist(final Vec4f o) {
        return (float)Math.sqrt(distSq(o));
    }


    /**
     * Return the dot product of this vector and the given one
     * @return the dot product as float
     */
    public float dot(final Vec4f o) {
        return x*o.x + y*o.y + z*o.z + w*o.w;
    }

    /**
     * Return the cosines of the angle between two vectors
     */
    public float cosAngle(final Vec4f o) {
        return dot(o) / ( length() * o.length() ) ;
    }

    /**
     * Return the angle between two vectors in radians
     */
    public float angle(final Vec4f o) {
        return (float) Math.acos( cosAngle(o) );
    }

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
    public boolean isEqual(final Vec4f o, final float epsilon) {
        if( this == o ) {
            return true;
        } else {
            return FloatUtil.isEqual(x, o.x, epsilon) &&
                   FloatUtil.isEqual(y, o.y, epsilon) &&
                   FloatUtil.isEqual(z, o.z, epsilon) &&
                   FloatUtil.isEqual(w, o.w, epsilon);
        }
    }

    /**
     * Equals check using {@link FloatUtil#EPSILON} in {@link FloatUtil#isEqual(float, float)}.
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
    public boolean isEqual(final Vec4f o) {
        if( this == o ) {
            return true;
        } else {
            return FloatUtil.isEqual(x, o.x) &&
                   FloatUtil.isEqual(y, o.y) &&
                   FloatUtil.isEqual(z, o.z) &&
                   FloatUtil.isEqual(w, o.w);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Vec4f ) {
            return isEqual((Vec4f)o);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return x + " / " + y + " / " + z + " / " + w;
    }
}
