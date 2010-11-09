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
 
package com.jogamp.opengl.util;

import javax.media.opengl.*;
import com.jogamp.common.os.Platform;

import java.io.IOException;

public class VersionInfo {

    public static StringBuffer getInfo(StringBuffer sb, String prefix, GL gl) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append(Platform.getNewline());
        sb.append("-----------------------------------------------------------------------------------------------------");
        sb.append(Platform.getNewline());
        getPackageInfo(sb, prefix, "javax.media.opengl", "GL");
        sb.append(Platform.getNewline());
        sb.append(Platform.getNewline());
        getPlatformInfo(sb, prefix);
        sb.append(Platform.getNewline());
        sb.append(Platform.getNewline());
        getOpenGLInfo(sb, prefix, gl);
        sb.append(Platform.getNewline());
        sb.append(Platform.getNewline());
        sb.append("-----------------------------------------------------------------------------------------------------");
        sb.append(Platform.getNewline());

        return sb;
    }

    public static StringBuffer getPlatformInfo(StringBuffer sb, String prefix) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append(prefix+" Platform: " + Platform.getOS() + " " + Platform.getOSVersion() + " (os), " + Platform.getArch() + " (arch)");
        sb.append(Platform.getNewline());
        sb.append(prefix+" Platform: littleEndian " + Platform.isLittleEndian() + ", 32Bit "+Platform.is32Bit() + ", a-ptr bit-size "+Platform.getPointerSizeInBits());
        sb.append(Platform.getNewline());
        sb.append(prefix+" Platform: Java " + Platform.getJavaVersion()+", "
            +Platform.getJavaVendor()+", "+Platform.getJavaVendorURL()+", is JavaSE: "+Platform.isJavaSE());

        return sb;
    }

    public static StringBuffer getOpenGLInfo(StringBuffer sb, String prefix, GL gl) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append(prefix+" "+GLProfile.glAvailabilityToString());
        sb.append(Platform.getNewline());
        sb.append(prefix+" Swap Interval " + gl.getSwapInterval());
        sb.append(Platform.getNewline());
        sb.append(prefix+" GL Profile    " + gl.getGLProfile());
        sb.append(Platform.getNewline());
        sb.append(prefix+" CTX VERSION   " + gl.getContext().getGLVersion());
        sb.append(Platform.getNewline());
        sb.append(prefix+" GL            " + gl);
        sb.append(Platform.getNewline());
        sb.append(prefix+" GL_VERSION    " + gl.glGetString(gl.GL_VERSION));
        sb.append(Platform.getNewline());
        sb.append(prefix+" GL_EXTENSIONS ");
        sb.append(Platform.getNewline());
        sb.append(prefix+"               " + gl.glGetString(gl.GL_EXTENSIONS));

        return sb;
    }

    public static StringBuffer getPackageInfo(StringBuffer sb, String prefix,
                                              String pkgName, String className) {
        try {
            ClassLoader classLoader = VersionInfo.class.getClassLoader();
            classLoader.loadClass(pkgName + "." + className);

            if(null==sb) {
                sb = new StringBuffer();
            }

            Package p = Package.getPackage(pkgName);
            if (p == null) {
                sb.append(prefix+" WARNING: Package.getPackage(" + pkgName + ") is null");
                sb.append(Platform.getNewline());
            }
            else {
                sb.append(prefix+" "+p);
                sb.append(Platform.getNewline());
                sb.append(prefix+" Specification Title = " + p.getSpecificationTitle());
                sb.append(Platform.getNewline());
                sb.append(prefix+" Specification Vendor = " + p.getSpecificationVendor());
                sb.append(Platform.getNewline());
                sb.append(prefix+" Specification Version = " + p.getSpecificationVersion());
                sb.append(Platform.getNewline());

                sb.append(prefix+" Implementation Vendor = " + p.getImplementationVendor());
                sb.append(Platform.getNewline());
                sb.append(prefix+" Implementation Version = " + p.getImplementationVersion());
            }
        }
        catch (ClassNotFoundException e) {
            sb.append(Platform.getNewline());
            sb.append(prefix+" Unable to load " + pkgName);
        }
        return sb;
    }
}

