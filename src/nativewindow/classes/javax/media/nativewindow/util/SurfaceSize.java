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

/** Immutable SurfaceSize Class, consisting of it's read only components:<br>
 * <ul>
 *  <li>{@link javax.media.nativewindow.util.DimensionImmutable} size in pixels</li>
 *  <li><code>bits per pixel</code></li>
 * </ul>
 */
public class SurfaceSize {
    DimensionImmutable resolution;
    int bitsPerPixel;

    public SurfaceSize(DimensionImmutable resolution, int bitsPerPixel) {
        if(null==resolution || bitsPerPixel<=0) {
            throw new IllegalArgumentException("resolution must be set and bitsPerPixel greater 0");
        }
        this.resolution=resolution;
        this.bitsPerPixel=bitsPerPixel;
    }

    public final DimensionImmutable getResolution() {
        return resolution;
    }

    public final int getBitsPerPixel() {
        return bitsPerPixel;
    }

    public final String toString() {
        return new String("[ "+resolution+" x "+bitsPerPixel+" bpp ]");
    }

    /**
     * Checks whether two size objects are equal. Two instances
     * of <code>SurfaceSize</code> are equal if the two components
     * <code>resolution</code> and <code>bitsPerPixel</code>
     * are equal.
     * @return  <code>true</code> if the two dimensions are equal;
     *          otherwise <code>false</code>.
     */
    public final boolean equals(Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof SurfaceSize) {
            SurfaceSize p = (SurfaceSize)obj;
            return getResolution().equals(p.getResolution()) &&
                   getBitsPerPixel() == p.getBitsPerPixel();
        }
        return false;
    }

    public final int hashCode() {
        // 31 * x == (x << 5) - x
        int hash = 31 + getResolution().hashCode();
        hash = ((hash << 5) - hash) + getBitsPerPixel();
        return hash;
    }
}

