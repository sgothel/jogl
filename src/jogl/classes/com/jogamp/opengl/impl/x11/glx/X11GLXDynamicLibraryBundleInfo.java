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
import java.util.*;

public class X11GLXDynamicLibraryBundleInfo extends DesktopGLDynamicLibraryBundleInfo  {
    protected X11GLXDynamicLibraryBundleInfo() {
        super();
    }

    public List getToolLibNames() {
        List/*<List>*/ libNamesList = new ArrayList();

        List/*<String>*/ glesLibNames = new ArrayList();

        // Be aware that on DRI systems, eg ATI fglrx, etc, 
        // you have to set LIBGL_DRIVERS_PATH env variable.
        // Eg on Ubuntu 64bit systems this is:
        //    export LIBGL_DRIVERS_PATH=/usr/lib/fglrx/dri:/usr/lib32/fglrx/dri
        //

        // this is the default GL lib name, according to the spec
        glesLibNames.add("libGL.so.1");

        // try this one as well, if spec fails
        glesLibNames.add("libGL.so");

        // last but not least .. the generic one
        glesLibNames.add("GL");

        libNamesList.add(glesLibNames);

        return libNamesList;
    }

    /** 
     * This respects old DRI requirements:<br>
     * <pre>
     * http://dri.sourceforge.net/doc/DRIuserguide.html
     * </pre>
     */
    public boolean shallLinkGlobal() { return true; }

    public final List getToolGetProcAddressFuncNameList() {
        List res = new ArrayList();
        res.add("glXGetProcAddressARB");
        res.add("glXGetProcAddress");
        return res;
    }

    public final long toolDynamicLookupFunction(long toolGetProcAddressHandle, String funcName) {
        return GLX.glXGetProcAddress(toolGetProcAddressHandle, funcName);
    }
}


