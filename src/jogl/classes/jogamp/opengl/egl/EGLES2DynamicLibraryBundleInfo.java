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

package jogamp.opengl.egl;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Covering ES3 and ES2.
 * </p>
 */
public final class EGLES2DynamicLibraryBundleInfo extends EGLDynamicLibraryBundleInfo {
    protected EGLES2DynamicLibraryBundleInfo() {
        super();
    }

    @Override
    public final List<List<String>> getToolLibNames() {
        final List<List<String>> libsList = new ArrayList<List<String>>();
        {
            final List<String> libsGL = new ArrayList<String>();

            // ES3: This is the default lib name, according to the spec
            libsGL.add("libGLESv3.so.3");

            // ES3: Try these as well, if spec fails
            libsGL.add("libGLESv3.so");
            libsGL.add("GLESv3");

            // ES3: Alternative names
            libsGL.add("GLES30");

            // ES3: For windows distributions using the 'unlike' lib prefix
            // where our tool does not add it.
            libsGL.add("libGLESv3");
            libsGL.add("libGLES30");

            // ES2: This is the default lib name, according to the spec
            libsGL.add("libGLESv2.so.2");

            // ES2: Try these as well, if spec fails
            libsGL.add("libGLESv2.so");
            libsGL.add("GLESv2");

            // ES2: Alternative names
            libsGL.add("GLES20");
            libsGL.add("GLESv2_CM");

            // ES2: For windows distributions using the 'unlike' lib prefix
            // where our tool does not add it.
            libsGL.add("libGLESv2");
            libsGL.add("libGLESv2_CM");
            libsGL.add("libGLES20");

            libsList.add(libsGL);
        }
        libsList.add(getEGLLibNamesList());

        return libsList;
    }

}

