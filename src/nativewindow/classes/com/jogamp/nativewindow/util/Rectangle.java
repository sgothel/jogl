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

package com.jogamp.nativewindow.util;

import java.util.List;

public class Rectangle implements Cloneable, RectangleImmutable {
    int x;
    int y;
    int width;
    int height;

    public Rectangle() {
        this(0, 0, 0, 0);
    }

    public Rectangle(final int x, final int y, final int width, final int height) {
        this.x=x;
        this.y=y;
        this.width=width;
        this.height=height;
    }
    public Rectangle(final RectangleImmutable s) {
        set(s);
    }

    @Override
    public Object cloneMutable() {
      return clone();
    }

    @Override
    protected Object clone() {
        try {
            return super.clone();
        } catch (final CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    @Override
    public final int getX() { return x; }
    @Override
    public final int getY() { return y; }
    @Override
    public final int getWidth() { return width; }
    @Override
    public final int getHeight() { return height; }

    public final void set(final int x, final int y, final int width, final int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }
    public final void set(final Rectangle s) {
        this.x = s.x;
        this.y = s.y;
        this.width = s.width;
        this.height = s.height;
    }
    public final void set(final RectangleImmutable s) {
        this.x = s.getX();
        this.y = s.getY();
        this.width = s.getWidth();
        this.height = s.getHeight();
    }
    public final void setX(final int x) { this.x = x; }
    public final void setY(final int y) { this.y = y; }
    public final void setWidth(final int width) { this.width = width; }
    public final void setHeight(final int height) { this.height = height; }

    @Override
    public final RectangleImmutable union(final RectangleImmutable r) {
        return union(r.getX(), r.getY(), r.getX() + r.getWidth(), r.getY() + r.getHeight());
    }
    @Override
    public final RectangleImmutable union(final int rx1, final int ry1, final int rx2, final int ry2) {
        final int x1 = Math.min(x, rx1);
        final int y1 = Math.min(y, ry1);
        final int x2 = Math.max(x + width, rx2);
        final int y2 = Math.max(y + height, ry2);
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }
    /**
     * Calculates the union of the given rectangles, stores it in this instance and returns this instance.
     * @param rectangles given list of rectangles
     * @return this instance holding the union of given rectangles.
     */
    public final Rectangle union(final List<RectangleImmutable> rectangles) {
        int x1=Integer.MAX_VALUE, y1=Integer.MAX_VALUE;
        int x2=Integer.MIN_VALUE, y2=Integer.MIN_VALUE;
        for(int i=rectangles.size()-1; i>=0; i--) {
            final RectangleImmutable vp = rectangles.get(i);
            x1 = Math.min(x1, vp.getX());
            x2 = Math.max(x2, vp.getX() + vp.getWidth());
            y1 = Math.min(y1, vp.getY());
            y2 = Math.max(y2, vp.getY() + vp.getHeight());
        }
        set(x1, y1, x2 - x1, y2 - y1);
        return this;
    }

    @Override
    public final RectangleImmutable intersection(final RectangleImmutable r) {
        return intersection(r.getX(), r.getY(), r.getX() + r.getWidth(), r.getY() + r.getHeight());
    }
    @Override
    public final RectangleImmutable intersection(final int rx1, final int ry1, final int rx2, final int ry2) {
        final int x1 = Math.max(x, rx1);
        final int y1 = Math.max(y, ry1);
        final int x2 = Math.min(x + width, rx2);
        final int y2 = Math.min(y + height, ry2);
        final int ix, iy, iwidth, iheight;
        if( x2 < x1 ) {
            ix = 0;
            iwidth = 0;
        } else {
            ix = x1;
            iwidth = x2 - x1;
        }
        if( y2 < y1 ) {
            iy = 0;
            iheight = 0;
        } else {
            iy = y1;
            iheight = y2 - y1;
        }
        return new Rectangle (ix, iy, iwidth, iheight);
    }
    @Override
    public final float coverage(final RectangleImmutable r) {
        final RectangleImmutable isect = intersection(r);
        final float sqI = isect.getWidth()*isect.getHeight();
        final float sqT = width*height;
        return sqI / sqT;
    }

    /**
     * Scale this instance's components,
     * i.e. multiply them by the given scale factors.
     * @param sx scale factor for x
     * @param sy scale factor for y
     * @return this instance for scaling
     */
    public final Rectangle scale(final int sx, final int sy) {
        x *= sx ;
        y *= sy ;
        width *= sx ;
        height *= sy ;
        return this;
    }

    /**
     * Inverse scale this instance's components,
     * i.e. divide them by the given scale factors.
     * @param sx inverse scale factor for x
     * @param sy inverse scale factor for y
     * @return this instance for scaling
     */
    public final Rectangle scaleInv(final int sx, final int sy) {
        x /= sx ;
        y /= sy ;
        width /= sx ;
        height /= sy ;
        return this;
    }

    @Override
    public int compareTo(final RectangleImmutable d) {
        {
            final int sq = width*height;
            final int xsq = d.getWidth()*d.getHeight();

            if(sq > xsq) {
                return 1;
            } else if(sq < xsq) {
                return -1;
            }
        }
        {
            // FIXME: Invalid, position needs to be compared differently
            final int sq = x*y;
            final int xsq = d.getX()*d.getY();

            if(sq > xsq) {
                return 1;
            } else if(sq < xsq) {
                return -1;
            }
        }
        return 0;
    }

    @Override
    public boolean equals(final Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof Rectangle) {
            final Rectangle rect = (Rectangle)obj;
            return (y == rect.y) && (x == rect.x) &&
                   (height == rect.height) && (width == rect.width);
        }
        return false;
    }

    @Override
    public int hashCode() {
        final int sum1 = x + height;
        final int sum2 = width + y;
        final int val1 = sum1 * (sum1 + 1)/2 + x;
        final int val2 = sum2 * (sum2 + 1)/2 + y;
        final int sum3 = val1 + val2;
        return sum3 * (sum3 + 1)/2 + val2;
    }

    @Override
    public String toString() {
        return "[ "+x+" / "+y+"  "+width+" x "+height+" ]";
    }
}

