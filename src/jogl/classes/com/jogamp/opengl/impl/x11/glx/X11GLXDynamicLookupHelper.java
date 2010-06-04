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

package com.jogamp.opengl.impl.x11.glx;

import com.jogamp.opengl.impl.*;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import java.util.*;
import java.security.*;
import javax.media.opengl.GLException;

public class X11GLXDynamicLookupHelper extends DesktopGLDynamicLookupHelper {
    private static final X11GLXDynamicLookupHelper x11GLXDynamicLookupHelper;

    static {
        X11GLXDynamicLookupHelper tmp = null;
        try {
            tmp = new X11GLXDynamicLookupHelper();
        } catch (GLException gle) {
            if(DEBUG) {
                gle.printStackTrace();
            }
        }
        x11GLXDynamicLookupHelper = tmp;
    }

    public static X11GLXDynamicLookupHelper getX11GLXDynamicLookupHelper() {
        return x11GLXDynamicLookupHelper;
    }

    protected X11GLXDynamicLookupHelper() {
        super();
        GLX.getGLXProcAddressTable().reset(this);
    }

    public synchronized void loadGLULibrary() {
        if(null==gluLib) {
            List/*<String>*/ gluLibNames = new ArrayList();
            gluLibNames.add("libGLU.so");
            if(Platform.is32Bit()) {
                gluLibNames.add("/usr/lib32/libGLU.so");
            } else {
                gluLibNames.add("/usr/lib64/libGLU.so");
            }
            gluLibNames.add("GLU");
            gluLib = loadFirstAvailable(gluLibNames, null, true);
            if(null != gluLib) {
                glLibraries.add(gluLib);
            }
        }
    }
    NativeLibrary gluLib = null;

    protected final List/*<String>*/ getGLLibNames() {
        List/*<String>*/ glesLibNames = new ArrayList();

        // first reassemble old DRIHack order, ie using hardcoded names ..
        glesLibNames.add("libGL.so.1");
        if(Platform.is32Bit()) {
            glesLibNames.add("/usr/lib32/libGL.so.1");
        } else {
            glesLibNames.add("/usr/lib64/libGL.so.1");
        }

        // at last .. the generic one, should be default!
        glesLibNames.add("GL");
        return glesLibNames;
    }

    protected final List/*<String>*/ getGLXLibNames() {
        return null;
    }

    protected boolean shallGLLibLoadedGlobal() { return true; }

    protected boolean shallGLXLibLoadedGlobal() { return true; }

    protected final String getGLXGetProcAddressFuncName() {
        return "glXGetProcAddressARB" ;
    }

    protected long dynamicLookupFunctionOnGLX(long glxGetProcAddressHandle, String glFuncName) {
        return GLX.glXGetProcAddressARB(glFuncName);
    }
}


