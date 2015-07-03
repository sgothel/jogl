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

package jogamp.opengl.egl;

import java.util.ArrayList;
import java.util.List;

import com.jogamp.opengl.egl.EGL;

import jogamp.opengl.GLDynamicLibraryBundleInfo;

/**
 * Implementation of the DynamicLookupHelper for Desktop ES2 (AMD, ..)
 * where EGL and ES2 functions reside within the desktop OpenGL library.
 */
public final class DesktopES2DynamicLibraryBundleInfo extends GLDynamicLibraryBundleInfo {
    static final List<String> glueLibNames;
    static {
        glueLibNames = new ArrayList<String>();
        glueLibNames.add("jogl_mobile");
    }

    protected DesktopES2DynamicLibraryBundleInfo() {
        super();
    }

    @Override
    public final List<String> getToolGetProcAddressFuncNameList() {
        final List<String> res = new ArrayList<String>();
        res.add("eglGetProcAddress");
        return res;
    }

    @Override
    public final long toolGetProcAddress(final long toolGetProcAddressHandle, final String funcName) {
        return EGLContext.eglGetProcAddress(toolGetProcAddressHandle, funcName);
    }

    @Override
    public final boolean useToolGetProcAdressFirst(final String funcName) {
        return true;
    }

    @Override
    public final List<List<String>> getToolLibNames() {
        final List<List<String>> libsList = new ArrayList<List<String>>();
        final List<String> libsGL = new ArrayList<String>();

        // Be aware that on DRI systems, eg ATI fglrx, etc,
        // you have to set LIBGL_DRIVERS_PATH env variable.
        // Eg on Ubuntu 64bit systems this is:
        //    export LIBGL_DRIVERS_PATH=/usr/lib/fglrx/dri:/usr/lib32/fglrx/dri
        //

        // X11: this is the default lib name, according to the spec
        libsGL.add("libGL.so.1");

        // X11: try this one as well, if spec fails
        libsGL.add("libGL.so");

        // Windows default
        libsGL.add("OpenGL32");

        // OSX (guess ES2 on OSX will never happen)
        libsGL.add("/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib");

        // last but not least .. the generic one
        libsGL.add("GL");

        libsList.add(libsGL);
        return libsList;
    }

    @Override
    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }
}
