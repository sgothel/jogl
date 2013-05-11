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
 * <p>
 * <i>Some</i> <code>GL_VENDOR</code> and <code>GL_RENDERER</code> strings are
 * listed here <http://feedback.wildfiregames.com/report/opengl/feature/GL_VENDOR>. 
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

    /** GLSL <code>discard</code> command leads to undefined behavior or won't get compiled if being used. Appears to <i>have</i> happened on Nvidia Tegra2, but seems to be fine now. FIXME: Constrain version. */
    public static final int GLSLBuggyDiscard = 5;
    
    /** 
     * Non compliant GL context due to a buggy implementation not suitable for use.
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
    
    /** 
     * Closing X11 displays may cause JVM crashes or X11 errors with some buggy drivers
     * while being used in concert w/ OpenGL.
     * <p>
     * Some drivers may require X11 displays to be closed in the same order as they were created,
     * some may not allow them to be closed at all while resources are being used somehow.
     * </p>
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>Mesa &lt; 8.0 _with_ X11 software renderer <code>Mesa X11</code>, not with GLX/DRI renderer.</li>
     *   <li>ATI proprietary Catalyst X11 driver versions:
     *     <ul>
     *       <li>8.78.6</li>
     *       <li>8.881</li>
     *       <li>8.911</li>
     *       <li>9.01.8</li>
     *     </ul></li>
     * </ul>
     * </p>
     * <p>
     * See Bug 515 - https://jogamp.org/bugzilla/show_bug.cgi?id=515
     * and {@link jogamp.nativewindow.x11.X11Util#ATI_HAS_XCLOSEDISPLAY_BUG}.
     * </p>
     * <p>
     * See Bug 705 - https://jogamp.org/bugzilla/show_bug.cgi?id=705
     * </p>
     */
    public static final int DontCloseX11Display = 8;
    
    /**
     * Need current GL Context when calling new ARB <i>pixel format query</i> functions, 
     * otherwise driver crashes the VM.
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>ATI proprietary Catalyst driver on Windows version &le; XP. 
     *       TODO: Validate if bug actually relates to 'old' ATI Windows drivers for old GPU's like X300
     *             regardless of the Windows version.</li>
     * </ul>
     * <p>
     * See Bug 480 - https://jogamp.org/bugzilla/show_bug.cgi?id=480
     * </p>
     */
    public static final int NeedCurrCtx4ARBPixFmtQueries = 9;
    
    /**
     * Need current GL Context when calling new ARB <i>CreateContext</i> function,
     * otherwise driver crashes the VM.
     * <p>
     * Drivers known exposing such bug:
     * <ul>
     *   <li>ATI proprietary Catalyst Windows driver on laptops with a driver version as reported in <i>GL_VERSION</i>:
     *     <ul>
     *       <li> <i>null</i> </li>
     *       <li> &lt; <code>12.102.3.0</code> ( <i>amd_catalyst_13.5_mobility_beta2</i> ) </li>
     *     </ul></li>
     * </ul>
     * </p>
     * <p>
     * See Bug 706 - https://jogamp.org/bugzilla/show_bug.cgi?id=706<br/>
     * See Bug 520 - https://jogamp.org/bugzilla/show_bug.cgi?id=520
     * </p>
     */
    public static final int NeedCurrCtx4ARBCreateContext = 10;
    
    
    /** Number of quirks known. */
    public static final int COUNT = 11;
    
    private static final String[] _names = new String[] { "NoDoubleBufferedPBuffer", "NoDoubleBufferedBitmap", "NoSetSwapInterval",
                                                          "NoOffscreenBitmap", "NoSetSwapIntervalPostRetarget", "GLSLBuggyDiscard",
                                                          "GLNonCompliant", "GLFlushBeforeRelease", "DontCloseX11Display",
                                                          "NeedCurrCtx4ARBPixFmtQueries", "NeedCurrCtx4ARBCreateContext"
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
