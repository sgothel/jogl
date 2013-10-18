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

public class Dimension implements Cloneable, DimensionImmutable {
    int width;
    int height;

    public Dimension() {
        this(0, 0);
    }

    public Dimension(int width, int height) {
        if(width<0 || height<0) {
            throw new IllegalArgumentException("width and height must be within: ["+0+".."+Integer.MAX_VALUE+"]");
        }
        this.width=width;
        this.height=height;
    }

    public Object cloneMutable() {
      return clone();
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    @Override
    public final int getWidth() { return width; }
    @Override
    public final int getHeight() { return height; }

    public final void set(int width, int height) {
        this.width = width;
        this.height = height;
    }
    public final void setWidth(int width) {
        this.width = width;
    }
    public final void setHeight(int height) {
        this.height = height;
    }
    public final Dimension scale(int s) {
        width *= s;
        height *= s;
        return this;
    }
    public final Dimension add(Dimension pd) {
        width += pd.width ;
        height += pd.height ;
        return this;
    }

    @Override
    public String toString() {
        return new String(width+" x "+height);
    }

    @Override
    public int compareTo(final DimensionImmutable d) {
        final int tsq = width*height;
        final int xsq = d.getWidth()*d.getHeight();

        if(tsq > xsq) {
            return 1;
        } else if(tsq < xsq) {
            return -1;
        }
        return 0;
    }

    @Override
    public boolean equals(Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof Dimension) {
            Dimension p = (Dimension)obj;
            return height == p.height &&
                   width == p.width ;
        }
        return false;
    }

    @Override
    public int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + width;
        return ((hash << 5) - hash) + height;
    }
}

