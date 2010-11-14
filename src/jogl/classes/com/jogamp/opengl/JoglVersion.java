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
 
package com.jogamp.opengl;

import javax.media.opengl.*;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.JogampVersion;
import java.util.jar.Manifest;
import javax.media.nativewindow.AbstractGraphicsDevice;

public class JoglVersion extends JogampVersion {

    protected static JoglVersion jogampCommonVersionInfo;

    protected JoglVersion(String packageName, Manifest mf) {
        super(packageName, mf);
    }

    public static JoglVersion getInstance() {
        if(null == jogampCommonVersionInfo) {
            synchronized(JoglVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageName = "javax.media.opengl";
                    final String fullClazzName = "javax.media.opengl.GL";
                    final Manifest mf = VersionUtil.getManifest(JoglVersion.class.getClassLoader(), fullClazzName);
                    jogampCommonVersionInfo = new JoglVersion(packageName, mf);
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    public StringBuffer toStringBuffer(GL gl, StringBuffer sb) {
        sb = super.toStringBuffer(sb);

        getGLInfo(gl, sb);
        sb.append("-----------------------------------------------------------------------------------------------------");
        sb.append(Platform.getNewline());

        return sb;
    }

    public String toString(GL gl) {
        return toStringBuffer(gl, null).toString();
    }

    public static StringBuffer getGLInfo(GL gl, StringBuffer sb) {
        if(null==sb) {
            sb = new StringBuffer();
        }

        sb.append("Default Desktop ").append(GLProfile.getDefaultDesktopDevice().getConnection()).append(": ").append(GLProfile.glAvailabilityToString(GLProfile.getDefaultDesktopDevice()));
        sb.append(Platform.getNewline());
        sb.append("Default EGL ").append(GLProfile.getDefaultEGLDevice().getConnection()).append(": ").append(GLProfile.glAvailabilityToString(GLProfile.getDefaultEGLDevice()));
        sb.append(Platform.getNewline());
        sb.append("Swap Interval ").append(gl.getSwapInterval());
        sb.append(Platform.getNewline());
        sb.append("GL Profile    ").append(gl.getGLProfile());
        sb.append(Platform.getNewline());
        sb.append("CTX VERSION   ").append(gl.getContext().getGLVersion());
        sb.append(Platform.getNewline());
        sb.append("GL            ").append(gl);
        sb.append(Platform.getNewline());
        sb.append("GL_VERSION    ").append(gl.glGetString(gl.GL_VERSION));
        sb.append(Platform.getNewline());
        sb.append("GL_EXTENSIONS ");
        sb.append(Platform.getNewline());
        sb.append("              ").append(gl.glGetString(gl.GL_EXTENSIONS));
        sb.append(Platform.getNewline());

        return sb;
    }

    public static void main(String args[]) {
        System.err.println(JoglVersion.getInstance());
    }
}

