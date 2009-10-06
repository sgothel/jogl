/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 * Sun gratefully acknowledges that this software was originally authored
 * and developed by Kenneth Bradley Russell and Christopher John Kline.
 */

package com.sun.opengl.impl.egl;

import java.util.*;
import javax.media.nativewindow.*;
import javax.media.opengl.*;
import com.sun.opengl.impl.*;
import com.sun.nativewindow.impl.*;
import com.sun.gluegen.runtime.NativeLibrary;
import com.sun.gluegen.runtime.DynamicLookupHelper;
import java.security.*;

/**
 * Abstract implementation of the DynamicLookupHelper for EGL,
 * which decouples it's dependencies to EGLDrawableFactory.
 *
 * Currently two implementations exist, one for ES1 and one for ES2.
 */
public abstract class EGLDynamicLookupHelper implements DynamicLookupHelper {
    protected static final boolean DEBUG = com.sun.opengl.impl.Debug.debug("EGL");
    protected static final boolean DEBUG_LOOKUP;

    private static final EGLDynamicLookupHelper eglES1DynamicLookupHelper;
    private static final EGLDynamicLookupHelper eglES2DynamicLookupHelper;
    private List/*<NativeLibrary>*/ glesLibraries;

    static {
        AccessControlContext localACC=AccessController.getContext();
        DEBUG_LOOKUP = com.sun.opengl.impl.Debug.isPropertyDefined("jogl.debug.DynamicLookup", true, localACC);

        EGLDynamicLookupHelper tmp=null;
        try {
            tmp = new EGLES1DynamicLookupHelper();
        } catch (Throwable t) {
            if(DEBUG) {
                t.printStackTrace();
            }
        }
        eglES1DynamicLookupHelper = tmp;

        tmp=null;
        try {
            tmp = new EGLES2DynamicLookupHelper();
        } catch (Throwable t) {
            if(DEBUG) {
                t.printStackTrace();
            }
        }
        eglES2DynamicLookupHelper = tmp;
    }

    public static EGLDynamicLookupHelper getDynamicLookupHelper(GLProfile glp) {
        if (glp.usesNativeGLES2()) {
            if(null==eglES2DynamicLookupHelper) {
                throw new GLException("EGLDynamicLookupHelper for ES2 not available");
            }
            return eglES2DynamicLookupHelper;
        } else if (glp.usesNativeGLES1()) {
            if(null==eglES1DynamicLookupHelper) {
                throw new GLException("EGLDynamicLookupHelper for ES1 not available");
            }
            return eglES1DynamicLookupHelper;
        } else {
            throw new GLException("Unsupported: "+glp);
        }
    }

    public static EGLDynamicLookupHelper getDynamicLookupHelper(int esProfile) {
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
        loadGLESLibrary(getESProfile());
        EGL.resetProcAddressTable(this);
    }

    /** Must return the proper ES profile number, 1 for ES1 and 2 for ES2 */
    protected abstract int getESProfile();

    /** Must return at least one OpenGL ES library name */
    protected abstract List/*<String>*/ getGLESLibNames();

    /** May return OpenGL ES library name(s) */
    protected List/*<String>*/ getEGLLibNames() {
        List/*<String>*/ eglLibNames = new ArrayList();

        // EGL
        eglLibNames.add("EGL");
        // for windows distributions using the 'unlike' lib prefix, 
        // where our tool does not add it.
        eglLibNames.add("libEGL");

        return eglLibNames;
    }

    private NativeLibrary loadFirstAvailable(List/*<String>*/ libNames, ClassLoader loader) {
        for (Iterator iter = libNames.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = NativeLibrary.open((String) iter.next(), loader, false /*global*/);
            if (lib != null) {
                return lib;
            }
        }
        return null;
    }

    private boolean loadEGLLibrary(ClassLoader loader, List/*<String>*/ eglLibNames) {
        NativeLibrary lib = null;
        if(null!=eglLibNames && eglLibNames.size()>0) {
            // EGL libraries ..
            lib = loadFirstAvailable(eglLibNames, loader);
            if ( null != lib ) {
                glesLibraries.add(lib);
            }
        }
        return null!=lib;
    }

    private void loadGLESLibrary(int esProfile) {
        List/*<String>*/ glesLibNames = getGLESLibNames();
        List/*<String>*/ eglLibNames = getEGLLibNames();
        boolean eglLoaded = false;

        ClassLoader loader = getClass().getClassLoader();
        NativeLibrary lib = null;

        glesLibraries = new ArrayList();

        // ES libraries ..
        lib = loadFirstAvailable(glesLibNames, loader);
        if ( null == lib ) {
            /*** FIXME: Have to think about this ..
            // try again with EGL loaded first ..
            if ( !eglLoaded && loadEGLLibrary(loader, eglLibNames) ) {
                eglLoaded = true ;
                lib = loadFirstAvailable(glesLibNames, loader);
            }
            if ( null == lib ) {
                throw new GLException("Unable to dynamically load OpenGL ES library for profile ES" + esProfile);
            } */
            throw new GLException("Unable to dynamically load OpenGL ES library for profile ES" + esProfile);
        }
        glesLibraries.add(lib);

        if ( !eglLoaded && !loadEGLLibrary(loader, eglLibNames) ) {
            throw new GLException("Unable to dynamically load EGL library for profile ES" + esProfile);
        }

        if (esProfile==2) {
            NativeLibLoader.loadES2();
        } else if (esProfile==1) {
            NativeLibLoader.loadES1();
        } else {
            throw new GLException("Unsupported: ES"+esProfile);
        }
    }

    private long dynamicLookupFunctionOnLibs(String glFuncName) {
        String funcName=glFuncName;
        long addr = dynamicLookupFunctionOnLibsImpl(funcName);
        if( 0==addr && NativeWindowFactory.TYPE_WINDOWS.equals(NativeWindowFactory.getNativeWindowType(false)) ) {
            // Hack: try some C++ decoration here for Imageon's emulation libraries ..
            final int argAlignment=4;  // 4 byte alignment of each argument
            final int maxArguments=12; // experience ..
            for(int arg=0; 0==addr && arg<=maxArguments; arg++) {
                funcName = "_"+glFuncName+"@"+(arg*argAlignment);
                addr = dynamicLookupFunctionOnLibsImpl(funcName);
            }
        }
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
        for (Iterator iter = glesLibraries.iterator(); iter.hasNext(); ) {
            NativeLibrary lib = (NativeLibrary) iter.next();
            long addr = lib.lookupFunction(glFuncName);
            if (addr != 0) {
                return addr;
            }
        }
        return 0;
    }

    private long eglGetProcAddressHandle = 0;

    public long dynamicLookupFunction(String glFuncName) {
        if(null==glFuncName) {
            return 0;
        }

        // bootstrap eglGetProcAddress
        if(0==eglGetProcAddressHandle) {
            eglGetProcAddressHandle = dynamicLookupFunctionOnLibs("eglGetProcAddress");
            if(0==eglGetProcAddressHandle) {
                GLException e = new GLException("Couldn't find eglGetProcAddress function entry");
                if(DEBUG) {
                    e.printStackTrace();
                }
                throw e;
            }
        }

        if(glFuncName.equals("eglGetProcAddress")) {
            return eglGetProcAddressHandle;
        }

        long addr = EGL.eglGetProcAddress(eglGetProcAddressHandle, glFuncName);
        if(DEBUG_LOOKUP) {
            if(0!=addr) {
                System.err.println("Lookup-EGL: <"+glFuncName+"> 0x"+Long.toHexString(addr));
            }
        }
        if(0==addr) {
            addr = dynamicLookupFunctionOnLibs(glFuncName);
        }
        return addr;
    }
}
