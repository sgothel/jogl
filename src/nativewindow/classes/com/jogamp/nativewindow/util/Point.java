/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

package com.jogamp.nativewindow.util;

public class Point implements Cloneable, PointImmutable {
    int x;
    int y;

    public Point(final int x, final int y) {
        this.x=x;
        this.y=y;
    }

    public Point() {
        this(0, 0);
    }

    @Override
    public Object cloneMutable() {
      return clone();
    }

    @Override
    public Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    @Override
    public int compareTo(final PointImmutable d) {
        final int sq = x*y;
        final int xsq = d.getX()*d.getY();

        if(sq > xsq) {
            return 1;
        } else if(sq < xsq) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof Point) {
            final Point p = (Point)obj;
            return y == p.y && x == p.x;
        }
        return false;
    }

    @Override
    public final int getX() {
        return x;
    }

    @Override
    public final int getY() {
        return y;
    }

    @Override
    public int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + x;
        hash = ((hash << 5) - hash) + y;
        return hash;
    }

    @Override
    public String toString() {
        return x + " / " + y;
    }

    public final void set(final int x, final int y) { this.x = x; this.y = y; }
    public final void setX(final int x) { this.x = x; }
    public final void setY(final int y) { this.y = y; }

    /**
     * Translate this instance's x- and y-components,
     * i.e. add the values of the given delta point to them.
     * @param pd delta point
     * @return this instance for scaling
     */
    public final Point translate(final Point pd) {
        x += pd.x ;
        y += pd.y ;
        return this;
    }

    /**
     * Translate this instance's x- and y-components,
     * i.e. add the given deltas to them.
     * @param dx delta for x
     * @param dy delta for y
     * @return this instance for scaling
     */
    public final Point translate(final int dx, final int dy) {
        x += dx ;
        y += dy ;
        return this;
    }

    /**
     * Scale this instance's x- and y-components,
     * i.e. multiply them by the given scale factors.
     * @param sx scale factor for x
     * @param sy scale factor for y
     * @return this instance for scaling
     */
    public final Point scale(final int sx, final int sy) {
        x *= sx ;
        y *= sy ;
        return this;
    }

    /**
     * Scale this instance's x- and y-components,
     * i.e. multiply them by the given scale factors.
     * <p>
     * The product is rounded back to integer.
     * </p>
     * @param sx scale factor for x
     * @param sy scale factor for y
     * @return this instance for scaling
     */
    public final Point scale(final float sx, final float sy) {
        x = (int)(x * sx + 0.5f);
        y = (int)(y * sy + 0.5f);
        return this;
    }

    /**
     * Inverse scale this instance's x- and y-components,
     * i.e. divide them by the given scale factors.
     * @param sx inverse scale factor for x
     * @param sy inverse scale factor for y
     * @return this instance for scaling
     */
    public final Point scaleInv(final int sx, final int sy) {
        x /= sx ;
        y /= sy ;
        return this;
    }
    /**
     * Inverse scale this instance's x- and y-components,
     * i.e. divide them by the given scale factors.
     * <p>
     * The product is rounded back to integer.
     * </p>
     * @param sx inverse scale factor for x
     * @param sy inverse scale factor for y
     * @return this instance for scaling
     */
    public final Point scaleInv(final float sx, final float sy) {
        x = (int)(x / sx + 0.5f);
        y = (int)(y / sy + 0.5f);
        return this;
    }
}
