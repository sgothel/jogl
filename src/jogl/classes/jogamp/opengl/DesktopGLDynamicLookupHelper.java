/**
 * Copyright 2010 JogAmp Community. All rights reserved.
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

package jogamp.opengl;

import com.jogamp.common.os.NativeLibrary;
import java.util.*;

public class DesktopGLDynamicLookupHelper extends GLDynamicLookupHelper {

    public DesktopGLDynamicLookupHelper(final DesktopGLDynamicLibraryBundleInfo info) {
        super(info);
    }

    public final DesktopGLDynamicLibraryBundleInfo getDesktopGLBundleInfo() { return (DesktopGLDynamicLibraryBundleInfo) getBundleInfo(); }

    @Override
    public final synchronized boolean loadGLULibrary() {
        /** hacky code .. where all platform GLU libs are tried ..*/
        if(null==gluLib) {
            final List<String> gluLibNames = new ArrayList<String>();
            gluLibNames.add("/System/Library/Frameworks/OpenGL.framework/Libraries/libGLU.dylib"); // osx
            gluLibNames.add("libGLU.so"); // unix
            gluLibNames.add("GLU32"); // windows
            gluLibNames.add("GLU"); // generic
            gluLib = loadFirstAvailable(gluLibNames, null, true);
            if(null != gluLib) {
                nativeLibraries.add(gluLib);
            }
        }
        return null != gluLib ;
    }
    NativeLibrary gluLib = null;
}

