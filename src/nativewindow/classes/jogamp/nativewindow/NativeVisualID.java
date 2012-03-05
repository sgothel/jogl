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

package jogamp.nativewindow;

/**
 * Specifies query to the native capabilities identification.
 * Semantics may differ depending on the native windowing system,
 * see {@link #getVisualID(int)}.
 */
public interface NativeVisualID {
    
    public enum NVIDType {
        GEN_ID(0), NATIVE_ID(1), 
        EGL_ConfigID(2), EGL_NativeVisualID(3), X11_XVisualID(4), X11_FBConfigID(5), WIN32_PFDID(6); 
        
        public final int id;

        NVIDType(int id){
            this.id = id;
        }
    }    
    
    /**
     * Returns the native identification of the given <code>type</code>.
     * <p> 
     * Depending on the native windowing system, this might be
     * <ul>
     *   <li>X11
     *     <ul>
     *       <li>GEN_ID: X11_XVisualID</li>
     *       <li>NATIVE_ID: X11_XVisualID</li>
     *       <li>X11_XVisualID</li>
     *       <li>X11FBConfigID</li>
     *     </ul></li>
     *   <li>Windows
     *     <ul>
     *       <li>GEN_ID: WIN32_PFDID</li>
     *       <li>NATIVE_ID: WIN32_PFDID</li>
     *       <li>WIN32_PFDID</li>
     *     </ul></li>
     *   <li>EGL
     *     <ul>
     *       <li>GEN_ID: EGL_ConfigID</li>
     *       <li>NATIVE_ID: EGL_NativeVisualID (X11_XVisualID, WIN32_PFDID, ..)</li>
     *       <li>EGL_ConfigID</li>
     *       <li>EGL_NativeVisualID</li>
     *     </ul></li>
     * </ul>
     * </p>
     */
    int getVisualID(NVIDType type);
}
