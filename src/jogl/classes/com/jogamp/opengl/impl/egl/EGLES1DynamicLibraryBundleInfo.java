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
 
package com.jogamp.opengl.impl.egl;

import java.util.*;
import com.jogamp.opengl.impl.*;

public class EGLES1DynamicLibraryBundleInfo extends EGLDynamicLibraryBundleInfo {
    static List/*<String>*/ glueLibNames;
    static {
        glueLibNames = new ArrayList();
        glueLibNames.addAll(GLDynamicLibraryBundleInfo.getGlueLibNamesPreload());
        glueLibNames.add("jogl_es1");
    }

    protected EGLES1DynamicLibraryBundleInfo() {
        super();
    }

    public List getToolLibNames() {
        List/*<List>*/ libNames = new ArrayList();

        List/*<String>*/ glesLibNames = new ArrayList();
        glesLibNames.add("GLES_CM");
        glesLibNames.add("GLES_CL");
        glesLibNames.add("GLESv1_CM");
        // for windows distributions using the 'unlike' lib prefix, 
        // where our tool does not add it.
        glesLibNames.add("libGLES_CM");
        glesLibNames.add("libGLES_CL");
        glesLibNames.add("libGLESv1_CM");
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

