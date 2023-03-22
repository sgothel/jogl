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

package com.jogamp.opengl.math;

/**
 * 3D Vector based upon three float components.
 *
 * Implementation borrowed from [gfxbox2](https://jausoft.com/cgit/cs_class/gfxbox2.git/tree/include/pixel/pixel3f.hpp#n29)
 * and its layout from OpenAL's Vec3f.
 */
public final class Vec3f {
    private float x;
    private float y;
    private float z;

    public Vec3f() {}

    public Vec3f(final Vec3f o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
    }

    public Vec3f copy() {
        return new Vec3f(this);
    }

    public Vec3f(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public void set(final Vec3f o) {
        this.x = o.x;
        this.y = o.y;
        this.z = o.z;
    }

    public void set(final float x, final float y, final float z) {
        this.x = x;
        this.y = y;
        this.z = z;
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

    /** Returns this * val; creates new vector */
    public Vec3f mul(final float val) {
        return new Vec3f(this).scale(val);
    }

    /** this = this * val */
    public Vec3f scale(final float val) {
        x *= val;
        y *= val;
        z *= val;
        return this;
    }

    /** Returns this + arg; creates new vector */
    public Vec3f plus(final Vec3f arg) {
        return new Vec3f(this).add(arg);
    }

    /** this = this + b */
    public Vec3f add(final Vec3f b) {
        x += b.x;
        y += b.y;
        z += b.z;
        return this;
    }

    /** Returns this + s * arg; creates new vector */
    public Vec3f plusScaled(final float s, final Vec3f arg) {
        return new Vec3f(this).addScaled(s, arg);
    }

    /** this = this + s * b */
    public Vec3f addScaled(final float s, final Vec3f b) {
        x += s * b.x;
        y += s * b.y;
        z += s * b.z;
        return this;
    }

    /** Returns this - arg; creates new vector */
    public Vec3f minus(final Vec3f arg) {
        return new Vec3f(this).sub(arg);
    }

    /** this = this - b */
    public Vec3f sub(final Vec3f b) {
        x -= b.x;
        y -= b.y;
        z -= b.z;
        return this;
    }

    public boolean is_zero() {
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
     * Return the cosines of the angle between two vectors
     */
    public float cosAngle(final Vec3f o) {
        return dot(o) / ( length() * o.length() ) ;
    }

    /**
     * Return the angle between two vectors in radians
     */
    public float angle(final Vec3f o) {
        return (float) Math.acos( cosAngle(o) );
    }

    public boolean intersects(final Vec3f o) {
        if( Math.abs(x-o.x) >= FloatUtil.EPSILON || Math.abs(y-o.y) >= FloatUtil.EPSILON || Math.abs(z-o.z) >= FloatUtil.EPSILON ) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        return x + " / " + y + " / " + z;
    }
}
