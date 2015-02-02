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

package com.jogamp.nativewindow;

import java.util.Comparator;

/**
 * Visual ID holder interface.
 * <p>
 * Allows queries of different types of native visual IDs,
 * see {@link #getVisualID(int)}.
 * </p>
 */
public interface VisualIDHolder {

    public enum VIDType {
        // Generic Values
        INTRINSIC(0), NATIVE(1),
        // EGL Values
        EGL_CONFIG(10),
        // X11 Values
        X11_XVISUAL(20), X11_FBCONFIG(21),
        // Windows Values
        WIN32_PFD(30);

        public final int id;

        VIDType(final int id){
            this.id = id;
        }
    }

    /**
     * Returns the native visual ID of the given <code>type</code>
     * if supported, or {@link #VID_UNDEFINED} if not supported.
     * <p>
     * Depending on the native windowing system, <code>type</code> is handled as follows:
     * <ul>
     *   <li>X11 throws NativeWindowException on <code>EGL_CONFIG</code>, <code>WIN32_PFD</code>
     *     <ul>
     *       <li><code>INTRINSIC</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>NATIVE</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>X11_XVISUAL</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>X11_FBCONFIG</code>: <code>VID_UNDEFINED</code></li>
     *     </ul></li>
     *   <li>X11/GL throws NativeWindowException on <code>EGL_CONFIG</code>, <code>WIN32_PFD</code>
     *     <ul>
     *       <li><code>INTRINSIC</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>NATIVE</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>X11_XVISUAL</code>: <i>X11 XVisual ID</i></li>
     *       <li><code>X11_FBCONFIG</code>: <i>X11 FBConfig ID</i> or <code>VID_UNDEFINED</code></li>
     *     </ul></li>
     *   <li>Windows/GL throws NativeWindowException on <code>EGL_CONFIG</code>, <code>X11_XVISUAL</code>, <code>X11_FBCONFIG</code>
     *     <ul>
     *       <li><code>INTRINSIC</code>: <i>Win32 PIXELFORMATDESCRIPTOR ID</i></li>
     *       <li><code>NATIVE</code>: <i>Win32 PIXELFORMATDESCRIPTOR ID</i></li>
     *       <li><code>WIN32_PFD</code>: <i>Win32 PIXELFORMATDESCRIPTOR ID</i></li>
     *     </ul></li>
     *   <li>EGL/GL throws NativeWindowException on <code>X11_XVISUAL</code>, <code>X11_FBCONFIG</code>, <code>WIN32_PFD</code>
     *     <ul>
     *       <li><code>INTRINSIC</code>: <i>EGL Config ID</i></li>
     *       <li><code>NATIVE</code>: <i>EGL NativeVisual ID</i> (<i>X11 XVisual ID</i>, <i>Win32 PIXELFORMATDESCRIPTOR ID</i>, ...)</li>
     *       <li><code>EGL_CONFIG</code>: <i>EGL Config ID</i></li>
     *     </ul></li>
     * </ul>
     * </p>
     * Note: <code>INTRINSIC</code> and <code>NATIVE</code> are always handled,
     *       but may result in {@link #VID_UNDEFINED}. The latter is true if
     *       the native value are actually undefined or the corresponding object is not
     *       mapped to a native visual object.
     *
     * @throws NativeWindowException if <code>type</code> is neither
     *         <code>INTRINSIC</code> nor <code>NATIVE</code>
     *         and does not match the native implementation.
     */
    int getVisualID(VIDType type) throws NativeWindowException ;

    /**
     * {@link #getVisualID(VIDType)} result indicating an undefined value,
     * which could be cause by an unsupported query.
     * <p>
     * We assume the const value <code>0</code> doesn't reflect a valid native visual ID
     * and is interpreted as <i>no value</i> on all platforms.
     * This is currently true for Android, X11 and Windows.
     * </p>
     */
    static final int VID_UNDEFINED = 0;

    /** Comparing {@link VIDType#NATIVE} */
    public static class VIDComparator implements Comparator<VisualIDHolder> {
        private final VIDType type;

        public VIDComparator(final VIDType type) {
            this.type = type;
        }

        @Override
        public int compare(final VisualIDHolder vid1, final VisualIDHolder vid2) {
            final int id1 = vid1.getVisualID(type);
            final int id2 = vid2.getVisualID(type);

            if(id1 > id2) {
                return 1;
            } else if(id1 < id2) {
                return -1;
            }
            return 0;
        }
    }
}
