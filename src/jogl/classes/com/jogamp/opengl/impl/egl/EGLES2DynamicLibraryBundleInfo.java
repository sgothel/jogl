/*
 * Copyright (c) 2010, Sven Gothel
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of Sven Gothel nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL Sven Gothel BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.jogamp.opengl.impl.egl;

import java.util.*;
import com.jogamp.opengl.impl.*;

public class EGLES2DynamicLibraryBundleInfo extends EGLDynamicLibraryBundleInfo {
    static List/*<String>*/ glueLibNames;
    static {
        glueLibNames = new ArrayList();
        glueLibNames.addAll(GLDynamicLibraryBundleInfo.getGlueLibNamesPreload());
        glueLibNames.add("jogl_es2");
    }
  
    protected EGLES2DynamicLibraryBundleInfo() {
        super();
    }

    public List getToolLibNames() {
        List/*<List>*/ libNames = new ArrayList();

        List/*<String>*/ glesLibNames = new ArrayList();
        glesLibNames.add("GLES20");
        glesLibNames.add("GLESv2");
        glesLibNames.add("GLESv2_CM");
        // for windows distributions using the 'unlike' lib prefix
        // where our tool does not add it.
        glesLibNames.add("libGLES20"); 
        glesLibNames.add("libGLESv2");
        glesLibNames.add("libGLESv2_CM");
        // last but not least, we may even use the desktop GL library,
        // which would be eg Mesa + Gallium EGL ..
        glesLibNames.add("libGL.so.1");
        glesLibNames.add("libGL.so");
        glesLibNames.add("GL");

        libNames.add(glesLibNames);
        libNames.add(getEGLLibNamesList());
        return libNames;
    }

    public List/*<String>*/ getGlueLibNames() {
        return glueLibNames;
    }
}

