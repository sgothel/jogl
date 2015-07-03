/**
 * Copyright 2014 JogAmp Community. All rights reserved.
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

import com.jogamp.common.os.Platform;

/**
 * <p>
 * Covering Desktop GL
 * </p>
 */
public final class EGLGLnDynamicLibraryBundleInfo extends EGLDynamicLibraryBundleInfo {
    private static final List<String> glueLibNames;
    static {
        glueLibNames = new ArrayList<String>();
        glueLibNames.add("jogl_desktop");
    }

    protected EGLGLnDynamicLibraryBundleInfo() {
        super();
    }

    @Override
    public final List<List<String>> getToolLibNames() {
        final List<List<String>> libsList = new ArrayList<List<String>>();
        {
            final List<String> libsGL = new ArrayList<String>();

            final Platform.OSType osType = Platform.getOSType();
            if( Platform.OSType.MACOS == osType ) {
                libsGL.add("/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib");
                libsGL.add("GL");
            } else if( Platform.OSType.WINDOWS == Platform.getOSType() ) {
                libsGL.add("OpenGL32");
            } else {
                // this is the default lib name, according to the spec
                libsGL.add("libGL.so.1");

                // try this one as well, if spec fails
                libsGL.add("libGL.so");

                // last but not least .. the generic one
                libsGL.add("GL");
            }

            libsList.add(libsGL);
        }
        libsList.add(getEGLLibNamesList());

        return libsList;
    }

}

