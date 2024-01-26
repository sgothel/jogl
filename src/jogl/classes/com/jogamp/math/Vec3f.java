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
 * 3D Vector based upon three float components.
 *
 * Implementation borrowed from [gfxbox2](https://jausoft.com/cgit/cs_class/gfxbox2.git/tree/include/pixel/pixel3f.hpp#n29)
 * and its data layout from JOAL's Vec3f.
 */
public final class Vec3f {
    public static final Vec3f ONE = new Vec3f(1f, 1f, 1f);
    public static final Vec3f UNIT_X = new Vec3f(1f, 0f, 0f);
    public static final Vec3f UNIT_X_NEG = new Vec3f(-1f, 0f, 0f);
    public static final Vec3f UNIT_Y = new Vec3f(0f, 1f, 0f);
    public static final Vec3f UNIT_Y_NEG = new Vec3f(0f, -1f, 0f);
    public static final Vec3f UNIT_Z = new Vec3f(0f, 0f, 1f);
    public static final Vec3f UNIT_Z_NEG = new Vec3f(0f, 0f, -1f);

    private float x;
    private float y;
    private float z;

    public Vec3f() {}

    public Vec3f(final Vec3f o) {
        set(o);
    }

    /** Creating new Vec3f using Vec4f, dropping w. */
    public Vec3f(final Vec4f o) {
        set(o);
    }

    /** Creating new Vec3f using { Vec2f, z}. */
    public Vec3f(final Vec2f o, final float z) {
        set(o, z);
    }

    public Vec3f copy() {
        return new Vec3f(this);
    }

    public Vec3f(final float[/*3*/] xyz) {
        set(xyz);
    }

    public Vec3f(final float x, final float y, final float z) {
        set(x, y, z);
    }

    /** this = o, returns this. */
    public Vec3f set(final Vec3f o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        return this;
    }

    /** this = { o, z }, returns this. */
    public Vec3f set(final Vec2f o, final float z) {
        this.x = o.x();
        this.y = o.y();
        this.z = z;
        return this;
    }

    /** this = o while dropping w, returns this. */
    public Vec3f set(final Vec4f o) {
        this.x = o.x();
        this.y = o.y();
        this.z = o.z();
        return this;
    }

    /** this = { x, y, z }, returns this. */
    public Vec3f set(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** this = xyz, returns this. */
    public Vec3f set(final float[/*3*/] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
        return this;
    }

    /** Sets the ith component, 0 <= i < 3 */
    public void set(final int i, final float val) {
        switch (i) {
            case 0: x = val; break;
            case 1: y = val; break;
            case 2: z = val; break;
            default: throw new IndexOutOfBoundsException();
        }
    }

    /** xyz = this, returns xyz. */
    public float[] get(final float[/*3*/] xyz) {
        xyz[0] = this.x;
        xyz[1] = this.y;
        xyz[2] = this.z;
        return xyz;
    }

    /** Gets the ith component, 0 <= i < 3 */
    public float get(final int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            case 2: return z;
            default: throw new IndexOutOfBoundsException();
        }
    }

    public float x() { return x; }
    public float y() { return y; }
    public float z() { return z; }

    public void setX(final float x) { this.x = x; }
    public void setY(final float y) { this.y = y; }
    public void setZ(final float z) { this.z = z; }

    /** this = max(this, m), returns this. */
    public Vec3f max(final Vec3f m) {
        this.x = Math.max(this.x, m.x);
        this.y = Math.max(this.y, m.y);
        this.z = Math.max(this.z, m.z);
        return this;
    }
    /** this = min(this, m), returns this. */
    public Vec3f min(final Vec3f m) {
        this.x = Math.min(this.x, m.x);
        this.y = Math.min(this.y, m.y);
        this.z = Math.min(this.z, m.z);
        return this;
    }

    /** Returns this * val; creates new vector */
    public Vec3f mul(final float val) {
        return new Vec3f(this).scale(val);
    }

    /** this = a * b, returns this. */
    public Vec3f mul(final Vec3f a, final Vec3f b) {
        x = a.x * b.x;
        y = a.y * b.y;
        z = a.z * b.z;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec3f mul(final Vec3f s) { return mul(s.x, s.y, s.z); }

    /** this = this * { sx, sy, sz }, returns this. */
    public Vec3f mul(final float sx, final float sy, final float sz) {
        x *= sx;
        y *= sy;
        z *= sz;
        return this;
    }

    /** this = a / b, returns this. */
    public Vec3f div(final Vec3f a, final Vec3f b) {
        x = a.x / b.x;
        y = a.y / b.y;
        z = a.z / b.z;
        return this;
    }

    /** this = this / a, returns this. */
    public Vec3f div(final Vec3f a) {
        x /= a.x;
        y /= a.y;
        z /= a.z;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec3f scale(final float s) {
        x *= s;
        y *= s;
        z *= s;
        return this;
    }

    /** Returns this + arg; creates new vector */
    public Vec3f plus(final Vec3f arg) {
        return new Vec3f(this).add(arg);
    }

    /** this = a + b, returns this. */
    public Vec3f plus(final Vec3f a, final Vec3f b) {
        x = a.x + b.x;
        y = a.y + b.y;
        z = a.z + b.z;
        return this;
    }

    /** this = this + { dx, dy, dz }, returns this. */
    public Vec3f add(final float dx, final float dy, final float dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    /** this = this + b, returns this. */
    public Vec3f add(final Vec3f b) {
        x += b.x;
        y += b.y;
        z += b.z;
        return this;
    }

    /** Returns this - arg; creates new vector */
    public Vec3f minus(final Vec3f arg) {
        return new Vec3f(this).sub(arg);
    }

    /** this = a - b, returns this. */
    public Vec3f minus(final Vec3f a, final Vec3f b) {
        x = a.x - b.x;
        y = a.y - b.y;
        z = a.z - b.z;
        return this;
    }

    /** this = this - b, returns this. */
    public Vec3f sub(final Vec3f b) {
        x -= b.x;
        y -= b.y;
        z -= b.z;
        return this;
    }

    /** Return true if all components are zero, i.e. it's absolute value < {@link #EPSILON}. */
    public boolean isZero() {
        return FloatUtil.isZero(x) && FloatUtil.isZero(y) && FloatUtil.isZero(z);
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
        return x*x + y*y + z*z;
    }

    /**
     * Normalize this vector in place
     */
    public Vec3f normalize() {
        final float lengthSq = lengthSq();
        if ( FloatUtil.isZero( lengthSq ) ) {
            x = 0.0f;
            y = 0.0f;
            z = 0.0f;
        } else {
            final float invSqr = 1.0f / (float)Math.sqrt(lengthSq);
            x *= invSqr;
            y *= invSqr;
            z *= invSqr;
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
    public float distSq(final Vec3f o) {
        final float dx = x - o.x;
        final float dy = y - o.y;
        final float dz = z - o.z;
        return dx*dx + dy*dy + dz*dz;
    }

    /**
     * Return the distance between this vector and the given one.
     */
    public float dist(final Vec3f o) {
        return (float)Math.sqrt(distSq(o));
    }


    /**
     * Return the dot product of this vector and the given one
     * @return the dot product as float
     */
    public float dot(final Vec3f o) {
        return x*o.x + y*o.y + z*o.z;
    }

    /** Returns this cross arg; creates new vector */
    public Vec3f cross(final Vec3f arg) {
        return new Vec3f().cross(this, arg);
    }

    /** this = a cross b. NOTE: "this" must be a different vector than
      both a and b. */
    public Vec3f cross(final Vec3f a, final Vec3f b) {
        x = a.y * b.z - a.z * b.y;
        y = a.z * b.x - a.x * b.z;
        z = a.x * b.y - a.y * b.x;
        return this;
    }

    /**
     * Return the cosine of the angle between two vectors using {@link #dot(Vec3f)}
     */
    public float cosAngle(final Vec3f o) {
        return dot(o) / ( length() * o.length() ) ;
    }

    /**
     * Return the angle between two vectors in radians using {@link Math#acos(double)} on {@link #cosAngle(Vec3f)}.
     */
    public float angle(final Vec3f o) {
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
    public boolean isEqual(final Vec3f o, final float epsilon) {
        if( this == o ) {
            return true;
        } else {
            return FloatUtil.isEqual(x, o.x, epsilon) &&
                   FloatUtil.isEqual(y, o.y, epsilon) &&
                   FloatUtil.isEqual(z, o.z, epsilon);
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
    public boolean isEqual(final Vec3f o) {
        if( this == o ) {
            return true;
        } else {
            return FloatUtil.isEqual(x, o.x) &&
                   FloatUtil.isEqual(y, o.y) &&
                   FloatUtil.isEqual(z, o.z);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Vec3f ) {
            return isEqual((Vec3f)o);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return x + " / " + y + " / " + z;
    }
}
