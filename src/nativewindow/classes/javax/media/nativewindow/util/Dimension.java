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

public class Dimension {
    int width;
    int height;

    public Dimension() {
        this(0, 0);
    }

    public Dimension(int width, int height) {
        this.width=width;
        this.height=height;
    }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public void setWidth(int width) { this.width = width; }
    public void setHeight(int height) { this.height = height; }

    public Dimension scale(int s) {
        width *= s;
        height *= s;
        return this;
    }

    public Dimension add(Dimension pd) {
        width += pd.width ;
        height += pd.height ;
        return this;
    }

    public String toString() {
        return new String("Dimension["+width+"x"+height+"]");
    }

    /**
     * Checks whether two dimensions objects are equal. Two instances
     * of <code>Dimension</code> are equal if the four integer values
     * of the fields <code>height</code> and <code>width</code>
     * are equal.
     * @return      <code>true</code> if the two dimensions are equal;
     *                          otherwise <code>false</code>.
     */
    public boolean equals(Object obj) {
        if (obj instanceof Dimension) {
            Dimension p = (Dimension)obj;
            return (height == p.height) && (width == p.width) &&
                   (height == p.height) && (width == p.width);
        }
        return false;
    }

    /**
     * Returns the hash code for this Dimension.
     *
     * @return    a hash code for this Dimension.
     */
    public int hashCode() {
        int sum1 = width + height;
        return sum1 * (sum1 + 1)/2 + width;
    }

}

