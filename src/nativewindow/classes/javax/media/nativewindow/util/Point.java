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

public class Point {
    int x;
    int y;

    public Point() {
        this(0, 0);
    }

    public Point(int x, int y) {
        this.x=x;
        this.y=y;
    }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setX(int x) { this.x = x; }
    public void setY(int y) { this.y = y; }

    public Point translate(Point pd) {
        x += pd.x ;
        y += pd.y ;
        return this;
    }

    public Point translate(int dx, int dy) {
        x += dx ;
        y += dy ;
        return this;
    }

    public String toString() {
        return new String("Point["+x+"/"+y+"]");
    }

    /**
     * Checks whether two points objects are equal. Two instances
     * of <code>Point</code> are equal if the four integer values
     * of the fields <code>y</code> and <code>x</code>
     * are equal.
     * @return      <code>true</code> if the two points are equal;
     *                          otherwise <code>false</code>.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Point) {
            Point p = (Point)obj;
            return (y == p.y) && (x == p.x);
        }
        return false;
    }

    /**
     * Returns the hash code for this Point.
     *
     * @return    a hash code for this Point.
     */
    public int hashCode() {
        int sum1 = x + y;
        return sum1 * (sum1 + 1)/2 + x;
    }

}

