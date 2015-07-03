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

package com.jogamp.nativewindow;

import com.jogamp.common.GlueGenVersion;
import com.jogamp.common.util.JogampVersion;
import com.jogamp.common.util.VersionUtil;

import java.util.jar.Manifest;

public class NativeWindowVersion extends JogampVersion {

    protected static volatile NativeWindowVersion jogampCommonVersionInfo;

    protected NativeWindowVersion(final String packageName, final Manifest mf) {
        super(packageName, mf);
    }

    public static NativeWindowVersion getInstance() {
        if(null == jogampCommonVersionInfo) { // volatile: ok
            synchronized(NativeWindowVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageName1 = "com.jogamp.nativewindow"; // atomic packaging - and identity
                    final String packageName2 = "com.jogamp.opengl"; // all packaging
                    final Manifest mf = VersionUtil.getManifest(NativeWindowVersion.class.getClassLoader(), new String[]{ packageName1, packageName2 } );
                    jogampCommonVersionInfo = new NativeWindowVersion(packageName1, mf);
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    public static void main(final String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        System.err.println(NativeWindowVersion.getInstance());
    }
}
