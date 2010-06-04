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

import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.NativeLibrary;
import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.jogamp.opengl.impl.*;
import java.security.*;

/**
 * Abstract implementation of the DynamicLookupHelper for EGL,
 * which decouples it's dependencies to EGLDrawable.
 *
 * Currently two implementations exist, one for ES1 and one for ES2.
 */
public abstract class EGLDynamicLookupHelper extends GLDynamicLookupHelper {
    private static final EGLDynamicLookupHelper eglES1DynamicLookupHelper;
    private static final EGLDynamicLookupHelper eglES2DynamicLookupHelper;

    static {
        EGLDynamicLookupHelper tmp=null;
        try {
            tmp = new EGLES1DynamicLookupHelper();
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        eglES1DynamicLookupHelper = tmp;

        tmp=null;
        try {
            tmp = new EGLES2DynamicLookupHelper();
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        eglES2DynamicLookupHelper = tmp;
    }

    public static EGLDynamicLookupHelper getEGLDynamicLookupHelper(GLProfile glp) {
        if (glp.usesNativeGLES2()) {
            return getEGLDynamicLookupHelper(2);
        } else if (glp.usesNativeGLES1()) {
            return getEGLDynamicLookupHelper(1);
        } else {
            throw new GLException("Unsupported: "+glp);
        }
    }

    public static EGLDynamicLookupHelper getEGLDynamicLookupHelper(int esProfile) {
        if (2==esProfile) {
            if(null==eglES2DynamicLookupHelper) {
                throw new GLException("EGLDynamicLookupHelper for ES2 not available");
            }
            return eglES2DynamicLookupHelper;
        } else if (1==esProfile) {
            if(null==eglES1DynamicLookupHelper) {
                throw new GLException("EGLDynamicLookupHelper for ES1 not available");
            }
            return eglES1DynamicLookupHelper;
        } else {
            throw new GLException("Unsupported: ES"+esProfile);
        }
    }

    protected EGLDynamicLookupHelper() {
        super();
        EGL.resetProcAddressTable(this);
    }

    protected boolean hasESBinding = false;
    public boolean hasESBinding() { return hasESBinding; }

    protected final List/*<String>*/ getGLXLibNames() {
        List/*<String>*/ eglLibNames = new ArrayList();

        // EGL
        eglLibNames.add("EGL");
        // for windows distributions using the 'unlike' lib prefix, 
        // where our tool does not add it.
        eglLibNames.add("libEGL");

        return eglLibNames;
    }

    protected final String getGLXGetProcAddressFuncName() {
        return "eglGetProcAddress" ;
    }

    protected final long dynamicLookupFunctionOnGLX(long glxGetProcAddressHandle, String glFuncName) {
        return EGL.eglGetProcAddress(glxGetProcAddressHandle, glFuncName);
    }
}

