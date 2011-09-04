/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

public class Insets implements Cloneable, InsetsImmutable {
    static final InsetsImmutable zeroInsets = new Insets();
    public static final InsetsImmutable getZero() { return zeroInsets; }
    
    int l, r, t, b;

    public Insets() {
        this(0, 0, 0, 0);
    }

    public Insets(int left, int right, int top, int bottom) {
        this.l=left;
        this.r=right;
        this.t=top;
        this.b=bottom;
    }
    
    public Object cloneMutable() {
      return clone();
    }
  
    protected Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException ex) {
            throw new InternalError();
        }
    }

    public final int getLeftWidth() { return l; }
    public final int getRightWidth() { return r; }
    public final int getTotalWidth() { return l + r; }
    public final int getTopHeight() { return t; }
    public final int getBottomHeight() { return b; }
    public final int getTotalHeight() { return t + b; }

    public void setLeftWidth(int left) { l = left; }
    public void setRightWidth(int right) { r = right; }
    public void setTopHeight(int top) { t = top; }
    public void setBottomHeight(int bottom) { b = bottom; }
    
    public boolean equals(Object obj) {
        if(this == obj)  { return true; }
        if (obj instanceof Insets) {
            Insets insets = (Insets)obj;
            return (r == insets.r) && (l == insets.l) &&
                   (b == insets.b) && (t == insets.t);
        }
        return false;
    }

    public int hashCode() {
        int sum1 = l + b;
        int sum2 = t + r;
        int val1 = sum1 * (sum1 + 1)/2 + l;
        int val2 = sum2 * (sum2 + 1)/2 + r;
        int sum3 = val1 + val2;
        return sum3 * (sum3 + 1)/2 + val2;
    }

    public String toString() {
        return new String("[ l "+l+", r "+r+" - t "+t+", b "+b+" - "+getTotalWidth()+"x"+getTotalHeight()+"]");
    }
}

