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
 
package com.jogamp.opengl.cg;

import com.jogamp.common.jvm.JNILibLoaderBase;
import com.jogamp.common.os.DynamicLibraryBundleInfo;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.cache.TempJarCache;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;

public class CgDynamicLibraryBundleInfo implements DynamicLibraryBundleInfo {
    private static List<String> glueLibNames;
    static {
        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                Platform.initSingleton();
                
                if(TempJarCache.isInitialized()) {
                   // Cg class and natives are available in their single atomic JAR files only 
                   JNILibLoaderBase.addNativeJarLibs(CgDynamicLibraryBundleInfo.class, "jogl_cg", null);
                }
                return null;
            }
        });
        
        glueLibNames = new ArrayList<String>();
        // glueLibNames.addAll(getGlueLibNamesPreload());
        glueLibNames.add("jogl_cg");
    }

    public static final int getCgGlueLibIndex() {
        return glueLibNames.size()-1;
    }

    protected CgDynamicLibraryBundleInfo() {
        super();
    }

    /** Make Cg symbols available to CgGL */
    public boolean shallLinkGlobal() { return true; }

    /** default **/
    public boolean shallLookupGlobal() { return false; }

    /** Tool has none **/
    public final List<String> getToolGetProcAddressFuncNameList() {
        return null;
    }

    /** Tool has none **/
    public final long toolGetProcAddress(long toolGetProcAddressHandle, String funcName) {
        return 0;
    }
    
    public boolean useToolGetProcAdressFirst(String funcName) {
        return false;
    }

    public List<List<String>> getToolLibNames() {
        final List<List<String>> libsList = new ArrayList<List<String>>();
        final List<String> libsCg = new ArrayList<String>();
        libsCg.add("Cg");
        libsList.add(libsCg);
        
        final List<String> libsCgGL = new ArrayList<String>();
        libsCgGL.add("CgGL");
        libsList.add(libsCgGL);

        return libsList;
    }

    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }
}


