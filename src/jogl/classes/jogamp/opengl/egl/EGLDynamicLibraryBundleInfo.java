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

import com.jogamp.common.os.AndroidVersion;

import java.util.*;

import jogamp.opengl.*;

/**
 * Abstract implementation of the DynamicLookupHelper for EGL,
 * which decouples it's dependencies to EGLDrawable.
 *
 * Currently two implementations exist, one for ES1 and one for ES2.
 */
public abstract class EGLDynamicLibraryBundleInfo extends GLDynamicLibraryBundleInfo {
    static List<String> glueLibNames;
    static {
        glueLibNames = new ArrayList<String>();
        glueLibNames.addAll(GLDynamicLibraryBundleInfo.getGlueLibNamesPreload());
        glueLibNames.add("jogl_mobile");
    }

    protected EGLDynamicLibraryBundleInfo() {
        super();
    }

    /** Might be a desktop GL library, and might need to allow symbol access to subsequent libs */
    public boolean shallLinkGlobal() { return true; }
    
    public boolean shallLookupGlobal() {
        if ( AndroidVersion.isAvailable ) {
            // Android requires global symbol lookup
            return true;
        }
        // default behavior for other platforms
        return false;
    }
    
    public final List<String> getToolGetProcAddressFuncNameList() {
        List<String> res = new ArrayList<String>();
        res.add("eglGetProcAddress");
        return res;
    }

    public final long toolGetProcAddress(long toolGetProcAddressHandle, String funcName) {
        return EGL.eglGetProcAddress(toolGetProcAddressHandle, funcName);
    }

    public final boolean useToolGetProcAdressFirst(String funcName) {
        return false; // JAU / FIXME funcName.startsWith("egl");
    }
    
    protected List<String> getEGLLibNamesList() {
        List<String> eglLibNames = new ArrayList<String>();
        
        // try default generic names first 
        eglLibNames.add("EGL");        
        // for windows distributions using the 'unlike' lib prefix, 
        // where our tool does not add it.
        eglLibNames.add("libEGL");
        // this is the default EGL lib name, according to the spec 
        eglLibNames.add("libEGL.so.1");
        
        return eglLibNames;
    }
    
    public final List<String> getGlueLibNames() {
        return glueLibNames;
    }    
}
