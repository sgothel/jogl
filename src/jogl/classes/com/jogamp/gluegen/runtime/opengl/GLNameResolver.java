/*
 * Copyright (c) 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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
 * You acknowledge that this software is not designed or intended for use
 * in the design, construction, operation or maintenance of any nuclear
 * facility.
 *
 */
package com.jogamp.gluegen.runtime.opengl;

/** Runtime utility identify and resolve extension names, which may be subsumed to core. */
public class GLNameResolver {
    //GL_XYZ : GL_XYZ, GL_XYZ_GL2, GL_XYZ_ARB, GL_XYZ_OES, GL_XYZ_OML
    //GL_XYZ : GL_XYZ, GL_GL2_XYZ, GL_ARB_XYZ, GL_OES_XYZ, GL_OML_XYZ
    //
    // Pass-1 Unify ARB extensions with the same value
    // Pass-2 Unify vendor extensions,
    //        if exist as an ARB extension with the same value.
    // Pass-3 Emit

    private static final String[] extensionsARB = { "ARB", "GL2", "OES", "KHR", "OML" };
    private static final String[] extensionsVEN = { "3DFX",
                                                    "AMD",
                                                    "ANDROID",
                                                    "ANGLE",
                                                    "ARM",
                                                    "APPLE",
                                                    "ATI",
                                                    "EXT",
                                                    "FJ",
                                                    "HI",
                                                    "HP",
                                                    "IBM",
                                                    "IMG",
                                                    "INGR",
                                                    "INTEL",
                                                    "MESA",
                                                    "MESAX",
                                                    "NV",
                                                    "PGI",
                                                    "QCOM",
                                                    "SGI",
                                                    "SGIS",
                                                    "SGIX",
                                                    "SUN",
                                                    "VIV",
                                                    "WIN" };

    public static final boolean isGLFunction(final String str) {
        return str.startsWith("gl")  || /* str.startsWith("glu") || str.startsWith("glX") || */
               str.startsWith("egl") || str.startsWith("wgl") || str.startsWith("agl") ||
               str.startsWith("cgl") ;
    }

    public static final boolean isGLEnumeration(final String str) {
        return str.startsWith("GL_")  || str.startsWith("GLU_") || str.startsWith("GLX_") ||
               str.startsWith("EGL_") || str.startsWith("WGL_") || str.startsWith("AGL_") ||
               str.startsWith("CGL_") ;
    }

    public static final int getExtensionIdx(final String[] extensions, final String str, final boolean isGLFunc) {
        if(isGLFunc) {
            for(int i = extensions.length - 1 ; i>=0 ; i--) {
                if( str.endsWith(extensions[i]) ) {
                    return i;
                }
            }
        } else {
            for(int i = extensions.length - 1 ; i>=0 ; i--) {
                if( str.endsWith("_"+extensions[i]) ) {
                    return i;
                }
            }
        }
        return -1;
    }

    public static final boolean isExtension(final String[] extensions, final String str, final boolean isGLFunc) {
        return getExtensionIdx(extensions, str, isGLFunc)>=0;
    }

    public static final String getExtensionSuffix(final String str, final boolean isGLFunc) {
        int idx = getExtensionIdx(extensionsARB, str, isGLFunc);
        if(idx>=0) {
            return extensionsARB[idx];
        }
        idx = getExtensionIdx(extensionsVEN, str, isGLFunc);
        if(idx>=0) {
            return extensionsVEN[idx];
        }
        return null;
    }

    public static final String normalize(final String[] extensions, String str, final boolean isGLFunc) {
        boolean touched = false;
        for(int i = extensions.length - 1 ; !touched && i>=0 ; i--) {
            if(isGLFunc) {
                if(str.endsWith(extensions[i])) {
                    // functions
                    str = str.substring(0, str.length()-extensions[i].length());
                    touched=true;
                }
            } else {
                if(str.endsWith("_"+extensions[i])) {
                    // enums
                    str = str.substring(0, str.length()-1-extensions[i].length());
                    touched=true;
                }
            }
        }
        return str;
    }
    public static final String normalizeARB(final String str, final boolean isGLFunc) {
        return normalize(extensionsARB, str, isGLFunc);
    }
    public static final boolean isExtensionARB(final String str, final boolean isGLFunc) {
        return isExtension(extensionsARB, str, isGLFunc);
    }
    public static final String normalizeVEN(final String str, final boolean isGLFunc) {
        return normalize(extensionsVEN, str, isGLFunc);
    }
    public static final boolean isExtensionVEN(final String str, final boolean isGLFunc) {
        return isExtension(extensionsVEN, str, isGLFunc);
    }
    public static final String normalize(final String str, final boolean isGLFunc) {
        if (isExtensionARB(str, isGLFunc)) {
            return normalizeARB(str, isGLFunc);
        }
        if (isExtensionVEN(str, isGLFunc)) {
            return normalizeVEN(str, isGLFunc);
        }
        return str;
    }
    public static final boolean isExtension(final String str, final boolean isGLFunc) {
        return isExtension(extensionsARB, str, isGLFunc) ||
               isExtension(extensionsVEN, str, isGLFunc);
    }

    public static final int getFuncNamePermutationNumber(final String name) {
        if(isExtensionARB(name, true) || isExtensionVEN(name, true)) {
            // no name permutation, if it's already a known extension
            return 1;
        }
        return 1 + extensionsARB.length + extensionsVEN.length;
    }

    public static final String getFuncNamePermutation(final String name, int i) {
        // identity
        if(i==0) {
            return name;
        }
        if(0>i || i>=(1+extensionsARB.length + extensionsVEN.length)) {
            throw new RuntimeException("Index out of range [0.."+(1+extensionsARB.length+extensionsVEN.length-1)+"]: "+i);
        }
        // ARB
        i-=1;
        if(i<extensionsARB.length) {
            return name+extensionsARB[i];
        }
        // VEN
        i-=extensionsARB.length;
        return name+extensionsVEN[i];
    }
}

