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
 
package com.jogamp.opengl.impl.windows.wgl;

import com.jogamp.opengl.impl.*;
import com.jogamp.common.os.DynamicLookupHelper;
import com.jogamp.common.os.NativeLibrary;
import com.jogamp.common.os.Platform;
import java.util.*;
import java.security.*;
import javax.media.opengl.GLException;

public class WindowsWGLDynamicLibraryBundleInfo extends DesktopGLDynamicLibraryBundleInfo  {
    protected WindowsWGLDynamicLibraryBundleInfo() {
        super();
    }

    public List getToolLibNames() {
        List/*<String>*/ libNamesList = new ArrayList();

        libNamesList.add("OpenGL32");

        return libNamesList;
    }


    public final List getToolGetProcAddressFuncNameList() {
        List res = new ArrayList();
        res.add("wglGetProcAddress");
        return res;
    }

    public final long toolDynamicLookupFunction(long toolGetProcAddressHandle, String funcName) {
        return WGL.wglGetProcAddress(toolGetProcAddressHandle, funcName);
    }
}

