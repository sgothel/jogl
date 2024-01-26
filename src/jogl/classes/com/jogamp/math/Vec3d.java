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
 * 3D Vector based upon three double components.
 *
 * Implementation borrowed from [gfxbox2](https://jausoft.com/cgit/cs_class/gfxbox2.git/tree/include/pixel/pixel3f.hpp#n29)
 * and its data layout from JOAL's Vec3f.
 */
public final class Vec3d {
    private double x;
    private double y;
    private double z;

    public Vec3d() {}

    public Vec3d(final Vec3d o) {
        set(o);
    }

    /** Creating new Vec3f using Vec4f, dropping w. */
    public Vec3d(final Vec4f o) {
        set(o);
    }

    /** Creating new Vec3f using { Vec2f, z}. */
    public Vec3d(final Vec2f o, final double z) {
        set(o, z);
    }

    public Vec3d copy() {
        return new Vec3d(this);
    }

    public Vec3d(final double[/*3*/] xyz) {
        set(xyz);
    }

    public Vec3d(final double x, final double y, final double z) {
        set(x, y, z);
    }

    /** this = o, returns this. */
    public Vec3d set(final Vec3d o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
        return this;
    }

    /** this = { o, z }, returns this. */
    public Vec3d set(final Vec2f o, final double z) {
        this.x = o.x();
        this.y = o.y();
        this.z = z;
        return this;
    }

    /** this = o while dropping w, returns this. */
    public Vec3d set(final Vec4f o) {
        this.x = o.x();
        this.y = o.y();
        this.z = o.z();
        return this;
    }

    /** this = { x, y, z }, returns this. */
    public Vec3d set(final double x, final double y, final double z) {
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    /** this = xyz, returns this. */
    public Vec3d set(final double[/*3*/] xyz) {
        this.x = xyz[0];
        this.y = xyz[1];
        this.z = xyz[2];
        return this;
    }

    /** Sets the ith component, 0 <= i < 3 */
    public void set(final int i, final double val) {
        switch (i) {
            case 0: x = val; break;
            case 1: y = val; break;
            case 2: z = val; break;
            default: throw new IndexOutOfBoundsException();
        }
    }

    /** xyz = this, returns xyz. */
    public double[] get(final double[/*3*/] xyz) {
        xyz[0] = this.x;
        xyz[1] = this.y;
        xyz[2] = this.z;
        return xyz;
    }

    /** Gets the ith component, 0 <= i < 3 */
    public double get(final int i) {
        switch (i) {
            case 0: return x;
            case 1: return y;
            case 2: return z;
            default: throw new IndexOutOfBoundsException();
        }
    }

    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }

    public void setX(final double x) { this.x = x; }
    public void setY(final double y) { this.y = y; }
    public void setZ(final double z) { this.z = z; }

    /** this = max(this, m), returns this. */
    public Vec3d max(final Vec3d m) {
        this.x = Math.max(this.x, m.x);
        this.y = Math.max(this.y, m.y);
        this.z = Math.max(this.z, m.z);
        return this;
    }
    /** this = min(this, m), returns this. */
    public Vec3d min(final Vec3d m) {
        this.x = Math.min(this.x, m.x);
        this.y = Math.min(this.y, m.y);
        this.z = Math.min(this.z, m.z);
        return this;
    }

    /** Returns this * val; creates new vector */
    public Vec3d mul(final double val) {
        return new Vec3d(this).scale(val);
    }

    /** this = a * b, returns this. */
    public Vec3d mul(final Vec3d a, final Vec3d b) {
        x = a.x * b.x;
        y = a.y * b.y;
        z = a.z * b.z;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec3d mul(final Vec3d s) { return mul(s.x, s.y, s.z); }

    /** this = this * { sx, sy, sz }, returns this. */
    public Vec3d mul(final double sx, final double sy, final double sz) {
        x *= sx;
        y *= sy;
        z *= sz;
        return this;
    }

    /** this = a / b, returns this. */
    public Vec3d div(final Vec3d a, final Vec3d b) {
        x = a.x / b.x;
        y = a.y / b.y;
        z = a.z / b.z;
        return this;
    }

    /** this = this / a, returns this. */
    public Vec3d div(final Vec3d a) {
        x /= a.x;
        y /= a.y;
        z /= a.z;
        return this;
    }

    /** this = this * s, returns this. */
    public Vec3d scale(final double s) {
        x *= s;
        y *= s;
        z *= s;
        return this;
    }

    /** Returns this + arg; creates new vector */
    public Vec3d plus(final Vec3d arg) {
        return new Vec3d(this).add(arg);
    }

    /** this = a + b, returns this. */
    public Vec3d plus(final Vec3d a, final Vec3d b) {
        x = a.x + b.x;
        y = a.y + b.y;
        z = a.z + b.z;
        return this;
    }

    /** this = this + { dx, dy, dz }, returns this. */
    public Vec3d add(final double dx, final double dy, final double dz) {
        x += dx;
        y += dy;
        z += dz;
        return this;
    }

    /** this = this + b, returns this. */
    public Vec3d add(final Vec3d b) {
        x += b.x;
        y += b.y;
        z += b.z;
        return this;
    }

    /** Returns this - arg; creates new vector */
    public Vec3d minus(final Vec3d arg) {
        return new Vec3d(this).sub(arg);
    }

    /** this = a - b, returns this. */
    public Vec3d minus(final Vec3d a, final Vec3d b) {
        x = a.x - b.x;
        y = a.y - b.y;
        z = a.z - b.z;
        return this;
    }

    /** this = this - b, returns this. */
    public Vec3d sub(final Vec3d b) {
        x -= b.x;
        y -= b.y;
        z -= b.z;
        return this;
    }

    /** Return true if all components are zero, i.e. it's absolute value < {@link #EPSILON}. */
    public boolean isZero() {
        return DoubleUtil.isZero(x) && DoubleUtil.isZero(y) && DoubleUtil.isZero(z);
    }

    /**
     * Return the length of this vector, a.k.a the <i>norm</i> or <i>magnitude</i>
     */
    public double length() {
        return Math.sqrt(lengthSq());
    }

    /**
     * Return the squared length of this vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public double lengthSq() {
        return x*x + y*y + z*z;
    }

    /**
     * Normalize this vector in place
     */
    public Vec3d normalize() {
        final double lengthSq = lengthSq();
        if ( DoubleUtil.isZero( lengthSq ) ) {
            x = 0.0f;
            y = 0.0f;
            z = 0.0f;
        } else {
            final double invSqr = 1.0f / Math.sqrt(lengthSq);
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
    public double distSq(final Vec3d o) {
        final double dx = x - o.x;
        final double dy = y - o.y;
        final double dz = z - o.z;
        return dx*dx + dy*dy + dz*dz;
    }

    /**
     * Return the distance between this vector and the given one.
     */
    public double dist(final Vec3d o) {
        return Math.sqrt(distSq(o));
    }


    /**
     * Return the dot product of this vector and the given one
     * @return the dot product as double
     */
    public double dot(final Vec3d o) {
        return x*o.x + y*o.y + z*o.z;
    }

    /** Returns this cross arg; creates new vector */
    public Vec3d cross(final Vec3d arg) {
        return new Vec3d().cross(this, arg);
    }

    /** this = a cross b. NOTE: "this" must be a different vector than
      both a and b. */
    public Vec3d cross(final Vec3d a, final Vec3d b) {
        x = a.y * b.z - a.z * b.y;
        y = a.z * b.x - a.x * b.z;
        z = a.x * b.y - a.y * b.x;
        return this;
    }

    /**
     * Return the cosine of the angle between two vectors using {@link #dot(Vec3d)}
     */
    public double cosAngle(final Vec3d o) {
        return dot(o) / ( length() * o.length() ) ;
    }

    /**
     * Return the angle between two vectors in radians using {@link Math#acos(double)} on {@link #cosAngle(Vec3d)}.
     */
    public double angle(final Vec3d o) {
        return Math.acos( cosAngle(o) );
    }

    /**
     * Equals check using a given {@link DoubleUtil#EPSILON} value and {@link DoubleUtil#isEqual(double, double, double)}.
     * <p>
     * Implementation considers following corner cases:
     * <ul>
     *    <li>NaN == NaN</li>
     *    <li>+Inf == +Inf</li>
     *    <li>-Inf == -Inf</li>
     * </ul>
     * @param o comparison value
     * @param epsilon consider using {@link DoubleUtil#EPSILON}
     * @return true if all components differ less than {@code epsilon}, otherwise false.
     */
    public boolean isEqual(final Vec3d o, final double epsilon) {
        if( this == o ) {
            return true;
        } else {
            return DoubleUtil.isEqual(x, o.x, epsilon) &&
                   DoubleUtil.isEqual(y, o.y, epsilon) &&
                   DoubleUtil.isEqual(z, o.z, epsilon);
        }
    }

    /**
     * Equals check using {@link DoubleUtil#EPSILON} in {@link DoubleUtil#isEqual(double, double)}.
     * <p>
     * Implementation considers following corner cases:
     * <ul>
     *    <li>NaN == NaN</li>
     *    <li>+Inf == +Inf</li>
     *    <li>-Inf == -Inf</li>
     * </ul>
     * @param o comparison value
     * @return true if all components differ less than {@link DoubleUtil#EPSILON}, otherwise false.
     */
    public boolean isEqual(final Vec3d o) {
        if( this == o ) {
            return true;
        } else {
            return DoubleUtil.isEqual(x, o.x) &&
                   DoubleUtil.isEqual(y, o.y) &&
                   DoubleUtil.isEqual(z, o.z);
        }
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Vec3d ) {
            return isEqual((Vec3d)o);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return x + " / " + y + " / " + z;
    }
}
