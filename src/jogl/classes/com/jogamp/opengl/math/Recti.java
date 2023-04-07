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
 * Rectangle with x, y, width and height integer components.
 */
public final class Recti {
    private int x;
    private int y;
    private int width;
    private int height;

    public Recti() {}

    public Recti(final Recti o) {
        set(o);
    }

    public Recti copy() {
        return new Recti(this);
    }

    public Recti(final int[/*4*/] xywh) {
        set(xywh);
    }

    public Recti(final int x, final int y, final int width, final int height) {
        set(x, y, width, height);
    }

    /** this = o, returns this. */
    public void set(final Recti o) {
        this.x = o.x;
        this.y = o.y;
        this.width = o.width;
        this.height= o.height;
    }

    /** this = { x, y, width, height }, returns this. */
    public void set(final int x, final int y, final int width, final int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height= height;
    }

    /** this = xywh, returns this. */
    public Recti set(final int[/*2*/] xywh) {
        this.x = xywh[0];
        this.y = xywh[1];
        this.width = xywh[2];
        this.height= xywh[3];
        return this;
    }

    /** xywh = this, returns xy. */
    public int[] get(final int[/*4*/] xywh) {
        xywh[0] = this.x;
        xywh[1] = this.y;
        xywh[2] = this.width;
        xywh[3] = this.height;
        return xywh;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int width() { return width; }
    public int height() { return height; }

    public void setX(final int x) { this.x = x; }
    public void setY(final int y) { this.y = y; }
    public void setWidth(final int width) { this.width = width; }
    public void setHeight(final int height) { this.height = height; }

    /** Return true if all components are zero. */
    public boolean isZero() {
        return 0 == x && 0 == y;
    }

    /**
     * Equals check.
     * @param o comparison value
     * @return true if all components are equal
     */
    public boolean isEqual(final Recti o) {
        if( this == o ) {
            return true;
        } else {
            return x == o.x && y == o.y &&
                   width == o.width && height == o.height;
        }
    }

    @Override
    public boolean equals(final Object o) {
        if( o instanceof Recti ) {
            return isEqual((Recti)o);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return x + " / " + y + "  " + width + " x " + height;
    }
}
