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

package javax.media.nativewindow.util;

public class Point implements Cloneable, PointImmutable {
    int x;
    int y;

    public Point(int x, int y) {
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
        } catch (CloneNotSupportedException ex) {
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
    public boolean equals(Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof Point) {
            Point p = (Point)obj;
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
        return new String( x + " / " + y );
    }

    public final void set(int x, int y) { this.x = x; this.y = y; }
    public final void setX(int x) { this.x = x; }
    public final void setY(int y) { this.y = y; }

    public final Point translate(Point pd) {
        x += pd.x ;
        y += pd.y ;
        return this;
    }

    public final Point translate(int dx, int dy) {
        x += dx ;
        y += dy ;
        return this;
    }

    public final Point scale(int sx, int sy) {
        x *= sx ;
        y *= sy ;
        return this;
    }

}
