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
 
package jogamp.opengl.macosx.cgl;

import jogamp.opengl.*;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import java.util.*;
import java.security.*;
import javax.media.opengl.GLException;

public class MacOSXCGLDynamicLibraryBundleInfo extends DesktopGLDynamicLibraryBundleInfo  {
    protected MacOSXCGLDynamicLibraryBundleInfo() {
        super();
    }

    public List getToolLibNames() {
        List/*<List>*/ libNamesList = new ArrayList();

        List/*<String>*/ glesLibNames = new ArrayList();

        glesLibNames.add("/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib");
        glesLibNames.add("GL");

        libNamesList.add(glesLibNames);

        return libNamesList;
    }

    public final List getToolGetProcAddressFuncNameList() {
        return null; 
        /** OSX manual says: NSImage use is discouraged
        List res = new ArrayList();
        res.add("GetProcAddress"); // dummy
        return res; */
    }

    public final long toolDynamicLookupFunction(long toolGetProcAddressHandle, String funcName) {
        return 0;
        /** OSX manual says: NSImage use is discouraged
            return CGL.getProcAddress(glFuncName); // manual implementation 
         */
    }
}

