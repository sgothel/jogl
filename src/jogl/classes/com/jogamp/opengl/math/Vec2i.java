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
 * 2D Vector based upon two integer components.
 */
public final class Vec2i {
    private int x;
    private int y;

    public Vec2i() {}

    public Vec2i(final Vec2i o) {
        set(o);
    }

    public Vec2i copy() {
        return new Vec2i(this);
    }

    public Vec2i(final int[/*2*/] xy) {
        set(xy);
    }

    public Vec2i(final int x, final int y) {
        set(x, y);
    }

    /** this = o, returns this. */
    public void set(final Vec2i o) {
        this.x = o.x;
        this.y = o.y;
    }

    /** this = { x, y }, returns this. */
    public void set(final int x, final int y) {
        this.x = x;
        this.y = y;
    }

    /** this = xy, returns this. */
    public Vec2i set(final int[/*2*/] xy) {
        this.x = xy[0];
        this.y = xy[1];
        return this;
    }

    /** xy = this, returns xy. */
    public int[] get(final int[/*2*/] xy) {
        xy[0] = this.x;
        xy[1] = this.y;
        return xy;
    }

    public int x() { return x; }
    public int y() { return y; }

    public void setX(final int x) { this.x = x; }
    public void setY(final int y) { this.y = y; }

    /** Return true if all components are zero. */
    public boolean isZero() {
        return 0 == x && 0 == y;
    }

    /**
     * Return the length of this vector, a.k.a the <i>norm</i> or <i>magnitude</i>
     */
    public int length() {
        return (int) Math.sqrt(lengthSq());
    }

    /**
     * Return the squared length of this vector, a.k.a the squared <i>norm</i> or squared <i>magnitude</i>
     */
    public int lengthSq() {
        return x*x + y*y;
    }

    /**
     * Return the squared distance between this vector and the given one.
     * <p>
     * When comparing the relative distance between two points it is usually sufficient to compare the squared
     * distances, thus avoiding an expensive square root operation.
     * </p>
     */
    public int distSq(final Vec2i o) {
        final int dx = x - o.x;
        final int dy = y - o.y;
        return dx*dx + dy*dy;
    }

    /**
     * Return the distance between this vector and the given one.
     */
    public int dist(final Vec2i o) {
        return (int)Math.sqrt(distSq(o));
    }

    /**
     * Equals check.
     * @param o comparison value
     * @return true if all components are equal
     */
    public boolean isEqual(final Vec2i o) {
        if( this == o ) {
            return true;
        } else {
            return x == o.x && y == o.y;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Vec2i ) {
            return isEqual((Vec2i)o);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return x + " / " + y;
    }
}
