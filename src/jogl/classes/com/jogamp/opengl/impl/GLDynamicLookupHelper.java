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

package com.jogamp.opengl.impl;

import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.NativeLibrary;
import java.util.*;
import java.security.*;
import javax.media.opengl.GLException;

/**
 * Abstract implementation of the DynamicLookupHelper for GL,
 * which decouples it's dependencies to EGLDrawableFactory.
 *
 * Currently two implementations exist, one for ES1 and one for ES2.
 */
public abstract class GLDynamicLookupHelper implements DynamicLookupHelper {
    protected static final boolean DEBUG = com.jogamp.opengl.impl.Debug.debug("GL");
    protected static final boolean DEBUG_LOOKUP;

    static {
        AccessControlContext localACC=AccessController.getContext();
        DEBUG_LOOKUP = com.jogamp.opengl.impl.Debug.isPropertyDefined("jogl.debug.DynamicLookup", true, localACC);
    }

    protected List/*<NativeLibrary>*/ glLibraries;
    private long glxGetProcAddressHandle;
    private String glxGetProcAddressFuncName;

    protected GLDynamicLookupHelper() {
        glLibraries = new ArrayList();
        loadGLLibrary();
        glxGetProcAddressFuncName = getGLXGetProcAddressFuncName();
        glxGetProcAddressHandle = getGLXGetProcAddressHandle();
    }

    /** Must return at least one OpenGL library name, eg GL, OpenGL32, .. */
    protected abstract List/*<String>*/ getGLLibNames();

    /** May return OpenGL Platform library name(s), eg EGL, GLX, .. */
    protected abstract List/*<String>*/ getGLXLibNames();

    protected boolean shallGLLibLoadedGlobal() { return false; }

    protected boolean shallGLXLibLoadedGlobal() { return false; }

    /** Shall return the OpenGL Platform function name to lookup function pointer, eg eglGetProcAddress */
    protected abstract String getGLXGetProcAddressFuncName() ; 

    protected abstract long dynamicLookupFunctionOnGLX(long glxGetProcAddressHandle, String glFuncName);

    /** Shall load the JNI binding */
    protected abstract void loadGLJNILibrary();

    /** May load the native GLU library, default: None **/
    public void loadGLULibrary() { }

    protected long getGLXGetProcAddressHandle() {
        long aptr = dynamicLookupFunctionOnLibs(glxGetProcAddressFuncName);
        if(0==aptr) {
            GLException e = new GLException("Couldn't find "+glxGetProcAddressFuncName+" function entry");
            if(DEBUG) {
                e.printStackTrace();
            }
            throw e;
        }
        return aptr;
    }

    protected NativeLibrary loadFirstAvailable(List/*<String>*/ libNames, ClassLoader loader, boolean global) {
        for (Iterator iter = libNames.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = NativeLibrary.open((String) iter.next(), loader, global);
            if (lib != null) {
                return lib;
            }
        }
        return null;
    }

    private boolean loadGLXLibrary(ClassLoader loader, List/*<String>*/ osLibNames) {
        if(null!=osLibNames && osLibNames.size()>0) {
            NativeLibrary lib = loadFirstAvailable(osLibNames, loader, shallGLXLibLoadedGlobal());
            if ( null != lib ) {
                glLibraries.add(lib);
            }
            return null!=lib;
        }
        return true; // none is ok
    }

    private void loadGLLibrary() {
        List/*<String>*/ glLibNames = getGLLibNames();
        List/*<String>*/ osLibNames = getGLXLibNames();

        ClassLoader loader = getClass().getClassLoader();
        NativeLibrary lib = null;

        // GL libraries ..
        lib = loadFirstAvailable(glLibNames, loader, shallGLLibLoadedGlobal());
        if ( null == lib ) {
            throw new GLException("Unable to dynamically load OpenGL library: "+getClass().getName());
        }
        glLibraries.add(lib);

        // GL Platform libraries ..
        if ( !loadGLXLibrary(loader, osLibNames) ) {
            throw new GLException("Unable to dynamically load GL Platform library: " + getClass().getName());
        }

        loadGLJNILibrary();
    }

    private long dynamicLookupFunctionOnLibs(String glFuncName) {
        String funcName=glFuncName;
        long addr = dynamicLookupFunctionOnLibsImpl(funcName);
        if(DEBUG_LOOKUP) {
            if(0!=addr) {
                System.err.println("Lookup-Native: "+glFuncName+" / "+funcName+" 0x"+Long.toHexString(addr));
            } else {
                System.err.println("Lookup-Native: "+glFuncName+" / "+funcName+" ** FAILED ** ");
            }
        }
        return addr;
    }

    private long dynamicLookupFunctionOnLibsImpl(String glFuncName) {
        // Look up this function name in all known libraries
        for (Iterator iter = glLibraries.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = (NativeLibrary) iter.next();
            long addr = lib.dynamicLookupFunction(glFuncName);
            if (addr != 0) {
                return addr;
            }
        }
        return 0;
    }

    public long dynamicLookupFunction(String glFuncName) {
        if(null==glFuncName) {
            return 0;
        }

        if(glFuncName.equals(glxGetProcAddressFuncName)) {
            return glxGetProcAddressHandle;
        }

        long addr = dynamicLookupFunctionOnGLX(glxGetProcAddressHandle, glFuncName);
        if(DEBUG_LOOKUP) {
            if(0!=addr) {
                System.err.println("Lookup: <"+glFuncName+"> 0x"+Long.toHexString(addr));
            }
        }
        if(0==addr) {
            addr = dynamicLookupFunctionOnLibs(glFuncName);
        }
        return addr;
    }
}

