/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl;

/** 
 * GLRendererQuirks contains information of known bugs of various GL renderer. 
 * This information allows us to workaround them.
 * <p>
 * Using centralized quirk identifier enables us to
 * locate code dealing w/ it and hence eases it's maintenance.   
 * </p>
 */
public class GLRendererQuirks {
    /** 
     * Crashes XServer when using double buffered PBuffer with:
     * <ul>
     *  <li>Mesa DRI Intel(R) Sandybridge Desktop</li>
     *  <li>Mesa DRI Intel(R) Ivybridge Mobile - 3.0 Mesa 8.0.4</li>
     *  <li>Gallium 0.4 on AMD CYPRESS</li>
     * </ul>
     * For now, it is safe to disable it w/ hw-acceleration.
     */
    public static final int NoDoubleBufferedPBuffer = 0;
    
    /** On Windows no double buffered bitmaps are guaranteed to be available. */
    public static final int NoDoubleBufferedBitmap  = 1;

    /** Crashes application when trying to set EGL swap interval on Android 4.0.3 / Pandaboard ES / PowerVR SGX 540 */
    public static final int NoSetSwapInterval       = 2;
    
    /** No offscreen bitmap available, currently true for JOGL's OSX implementation. */
    public static final int NoOffscreenBitmap       = 3;
    
    /** SIGSEGV on setSwapInterval() after changing the context's drawable w/ 'Mesa 8.0.4' dri2SetSwapInterval/DRI2 (soft & intel) */
    public static final int NoSetSwapIntervalPostRetarget = 4;

    /** GLSL <code>discard</code> command leads to undefined behavior or won't get compiled if being used. Appears to happen on Nvidia Tegra2. FIXME: Constrain version. */
    public static final int GLSLBuggyDiscard = 5;
    
    /** Number of quirks known. */
    public static final int COUNT = 6;
    
    private static final String[] _names = new String[] { "NoDoubleBufferedPBuffer", "NoDoubleBufferedBitmap", "NoSetSwapInterval",
                                                          "NoOffscreenBitmap", "NoSetSwapIntervalPostRetarget", "GLSLBuggyDiscard"
                                                        };

    private final int _bitmask;

    /**
     * @param quirks an array of valid quirks
     * @param offset offset in quirks array to start reading
     * @param len number of quirks to read from offset within quirks array
     * @throws IllegalArgumentException if one of the quirks is out of range
     */
    public GLRendererQuirks(int[] quirks, int offset, int len) throws IllegalArgumentException {
        int bitmask = 0;
        if( !( 0 <= offset + len && offset + len < quirks.length ) ) {
            throw new IllegalArgumentException("offset and len out of bounds: offset "+offset+", len "+len+", array-len "+quirks.length);
        }
        for(int i=offset; i<offset+len; i++) {
            final int quirk = quirks[i];
            validateQuirk(quirk);
            bitmask |= 1 << quirk;
        }
        _bitmask = bitmask;
    }      

    /**
     * @param quirk the quirk to be tested
     * @return true if quirk exist, otherwise false
     * @throws IllegalArgumentException if quirk is out of range
     */
    public final boolean exist(int quirk) throws IllegalArgumentException {
        validateQuirk(quirk);
        return 0 != ( ( 1 << quirk )  & _bitmask );
    }

    public final StringBuilder toString(StringBuilder sb) {
        if(null == sb) {
            sb = new StringBuilder();
        }
        sb.append("[");
        boolean first=true;
        for(int i=0; i<COUNT; i++) {
            final int testmask = 1 << i;
            if( 0 != ( _bitmask & testmask ) ) {
                if(!first) { sb.append(", "); }
                sb.append(toString(i));
                first=false;
            }
        }
        sb.append("]");
        return sb;
    }
    
    public final String toString() {
        return toString(null).toString();
    }

    /**
     * @param quirk the quirk to be validated, i.e. whether it is out of range
     * @throws IllegalArgumentException if quirk is out of range
     */
    public static void validateQuirk(int quirk) throws IllegalArgumentException {
        if( !( 0 <= quirk && quirk < COUNT ) ) {
            throw new IllegalArgumentException("Quirks must be in range [0.."+COUNT+"[, but quirk: "+quirk);
        }        
    }

    /**
     * @param quirk the quirk to be converted to String
     * @return the String equivalent of this quirk
     * @throws IllegalArgumentException if quirk is out of range
     */
    public static final String toString(int quirk) throws IllegalArgumentException {
        validateQuirk(quirk);
        return _names[quirk];
    }
}
