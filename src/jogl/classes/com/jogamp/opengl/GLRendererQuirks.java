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
     * Crashes XServer when using double buffered PBuffer with GL_RENDERER:
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
    
    /** 
     * Non compliant GL context, e.g. due to a buggy implementation rendering it not suitable for use.
     * <p>
     * Mesa >= 9.0 (?), Intel driver, OpenGL 3.1 compatibility context is not compliant: 
     * <pre>
     * GL_RENDERER: Mesa DRI Intel(R) Sandybridge Desktop 
     * </pre>
     * </p>
     */
    public static final int GLNonCompliant = 6;
    
    /**
     * The OpenGL Context needs a <code>glFlush()</code> before releasing it, otherwise driver may freeze:
     * <ul>
     *   <li>OSX < 10.7.3 - NVidia Driver. Bug 533 and Bug 548 @ https://jogamp.org/bugzilla/.</li>
     * </ul>  
     */
    public static final int GLFlushBeforeRelease = 7;
    
    // 
    // The JVM for the following system crashes on the second call to glXDestroyContext after
    // XCloseDisplay has been called once.
    //
    // The following will crash the system:
    //   XOpenDisplay(A), glXCreateNewContext(A), XOpenDisplay(B), glXCreateNewContext(B), 
    //   glXDestroyContext(A/B), XCloseDisplay(A/B), glXDestroyContext(B/A) (crash)
    //
    // Dell Latitude D520
    // Intel(R) Core(TM)2 CPU T7200
    // i810 Monitor driver
    // Platform       LINUX / Linux 2.6.18.8-0.3-default (os), i386 (arch), GENERIC_ABI, 2 cores
    // Platform       Java Version: 1.6.0_18, VM: Java HotSpot(TM) Server VM, Runtime: Java(TM) SE Runtime Environment
    // Platform       Java Vendor: Sun Microsystems Inc., http://java.sun.com/, JavaSE: true, Java6: true, AWT enabled: true
    // GL Profile     GLProfile[GL2/GL2.sw]
    // CTX VERSION    2.1 (Compatibility profile, FBO, software) - 2.1 Mesa 7.8.2
    // GL             jogamp.opengl.gl4.GL4bcImpl@472d48
    // GL_VENDOR      Brian Paul
    // GL_RENDERER    Mesa X11
    // GL_VERSION     2.1 Mesa 7.8.2
    //
    // The error can be reproduced using a C code, thus the error is indpendent of Java and JOGL.
    // The work around is to close all the X11 displays upon exit for a "Mesa X11" version < 8.
    // At this moment, it is unknown if the error exists in versions greater than 7.
    //
    // Martin C. Hegedus, March 30, 2013
    //
    public static final int DontCloseX11DisplayConnection = 8;
    
    /** Number of quirks known. */
    public static final int COUNT = 9;
    
    private static final String[] _names = new String[] { "NoDoubleBufferedPBuffer", "NoDoubleBufferedBitmap", "NoSetSwapInterval",
                                                          "NoOffscreenBitmap", "NoSetSwapIntervalPostRetarget", "GLSLBuggyDiscard",
                                                          "GLNonCompliant", "GLFlushBeforeRelease", "DontCloseX11DisplayConnection"
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
