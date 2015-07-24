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

import com.jogamp.common.GlueGenVersion;

import com.jogamp.common.os.Platform;
import com.jogamp.common.util.VersionUtil;
import com.jogamp.common.util.JogampVersion;

import java.util.List;
import java.util.jar.Manifest;
import com.jogamp.nativewindow.AbstractGraphicsDevice;

public class JoglVersion extends JogampVersion {

    protected static volatile JoglVersion jogampCommonVersionInfo;

    protected JoglVersion(final String packageName, final Manifest mf) {
        super(packageName, mf);
    }

    public static JoglVersion getInstance() {
        if(null == jogampCommonVersionInfo) { // volatile: ok
            synchronized(JoglVersion.class) {
                if( null == jogampCommonVersionInfo ) {
                    final String packageName = "com.jogamp.opengl";
                    final Manifest mf = VersionUtil.getManifest(JoglVersion.class.getClassLoader(), packageName);
                    jogampCommonVersionInfo = new JoglVersion(packageName, mf);
                }
            }
        }
        return jogampCommonVersionInfo;
    }

    public StringBuilder toString(final GL gl, StringBuilder sb) {
        sb = super.toString(sb).append(Platform.getNewline());
        getGLInfo(gl, sb);
        return sb;
    }

    public String toString(final GL gl) {
        return toString(gl, null).toString();
    }

    public static StringBuilder getAvailableCapabilitiesInfo(final GLDrawableFactory factory, final AbstractGraphicsDevice device, StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        boolean done = false;
        if(null!=factory) {
            try {
                final List<GLCapabilitiesImmutable> availCaps = factory.getAvailableCapabilities(device);
                if(null != availCaps && availCaps.size()>0) {
                    for(int i=0; i<availCaps.size(); i++) {
                        sb.append("\t").append(availCaps.get(i)).append(Platform.getNewline());
                    }
                    done = true;
                }
            } catch (final GLException gle) { /* n/a */ }
        }
        if(!done) {
            sb.append("\tnone").append(Platform.getNewline());
        }
        sb.append(Platform.getNewline());
        return sb;
    }

    public static StringBuilder getAllAvailableCapabilitiesInfo(AbstractGraphicsDevice device, StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        if(null == device) {
            device = GLProfile.getDefaultDevice();
        }
        sb.append(Platform.getNewline()).append(Platform.getNewline());
        sb.append("Desktop Capabilities: ").append(Platform.getNewline());
        getAvailableCapabilitiesInfo(GLDrawableFactory.getDesktopFactory(), device, sb);
        sb.append("EGL Capabilities: ").append(Platform.getNewline());
        getAvailableCapabilitiesInfo(GLDrawableFactory.getEGLFactory(), device, sb);
        return sb;
    }

    public static StringBuilder getDefaultOpenGLInfo(AbstractGraphicsDevice device, StringBuilder sb, final boolean withCapabilitiesInfo) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        if(null == device) {
            device = GLProfile.getDefaultDevice();
        }
        sb.append("GLProfiles on device ").append(device).append(Platform.getNewline());
        if(null!=device) {
            GLProfile.glAvailabilityToString(device, sb, "\t", 1);
        } else {
            sb.append("none");
        }
        if(withCapabilitiesInfo) {
            sb = getAllAvailableCapabilitiesInfo(device, sb);
        }
        return sb;
    }

    public static StringBuilder getGLInfo(final GL gl, final StringBuilder sb) {
        return getGLInfo(gl, sb, false);
    }
    public static StringBuilder getGLInfo(final GL gl, final StringBuilder sb, final boolean withCapabilitiesAndExtensionInfo) {
        return getGLInfo(gl, sb, true, withCapabilitiesAndExtensionInfo, withCapabilitiesAndExtensionInfo);
    }

    public static StringBuilder getGLInfo(final GL gl, StringBuilder sb,
                                          final boolean withAvailabilityInfo,
                                          final boolean withCapabilitiesInfo,
                                          final boolean withExtensionInfo) {
        final AbstractGraphicsDevice device = gl.getContext().getGLDrawable().getNativeSurface()
                                            .getGraphicsConfiguration().getScreen().getDevice();
        if(null==sb) {
            sb = new StringBuilder();
        }

        sb.append(VersionUtil.SEPERATOR).append(Platform.getNewline());
        sb.append(device.getClass().getSimpleName()).append("[type ")
                .append(device.getType()).append(", connection ").append(device.getConnection()).append("]: ").append(Platform.getNewline());
        if( withAvailabilityInfo ) {
            GLProfile.glAvailabilityToString(device, sb, "\t", 1);
        }
        sb.append(Platform.getNewline());

        sb = getGLStrings(gl, sb, withExtensionInfo);

        if( withCapabilitiesInfo ) {
            sb = getAllAvailableCapabilitiesInfo(device, sb);
        }
        return sb;
    }

    public static StringBuilder getGLStrings(final GL gl, final StringBuilder sb) {
        return getGLStrings(gl, sb, true);
    }

    public static StringBuilder getGLStrings(final GL gl, StringBuilder sb, final boolean withExtensions) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        final GLContext ctx = gl.getContext();
        sb.append("Swap Interval  ").append(gl.getSwapInterval());
        sb.append(Platform.getNewline());
        sb.append("GL Profile     ").append(gl.getGLProfile());
        sb.append(Platform.getNewline());
        sb.append("GL Version     ").append(ctx.getGLVersion()).append(" [GL ").append(ctx.getGLVersionNumber()).append(", vendor ").append(ctx.getGLVendorVersionNumber()).append("]");
        sb.append(Platform.getNewline());
        sb.append("Quirks         ").append(ctx.getRendererQuirks());
        sb.append(Platform.getNewline());
        sb.append("Impl. class    ").append(gl.getClass().getCanonicalName());
        sb.append(Platform.getNewline());
        sb.append("GL_VENDOR      ").append(gl.glGetString(GL.GL_VENDOR));
        sb.append(Platform.getNewline());
        sb.append("GL_RENDERER    ").append(gl.glGetString(GL.GL_RENDERER));
        sb.append(Platform.getNewline());
        sb.append("GL_VERSION     ").append(gl.glGetString(GL.GL_VERSION));
        sb.append(Platform.getNewline());
        sb.append("GLSL           ").append(gl.hasGLSL()).append(", has-compiler-func: ").append(gl.isFunctionAvailable("glCompileShader"));
        if(gl.hasGLSL()) {
            sb.append(", version: ").append(gl.glGetString(GL2ES2.GL_SHADING_LANGUAGE_VERSION)).append(" / ").append(ctx.getGLSLVersionNumber());
        }
        sb.append(Platform.getNewline());
        sb.append("GL FBO: basic ").append(gl.hasBasicFBOSupport()).append(", full ").append(gl.hasFullFBOSupport());
        sb.append(Platform.getNewline());
        sb.append("GL_EXTENSIONS  ").append(ctx.getGLExtensionCount());
        sb.append(Platform.getNewline());
        if( withExtensions ) {
            sb.append("               ").append(ctx.getGLExtensionsString());
            sb.append(Platform.getNewline());
        }
        sb.append("GLX_EXTENSIONS ").append(ctx.getPlatformExtensionCount());
        sb.append(Platform.getNewline());
        if( withExtensions ) {
            sb.append("               ").append(ctx.getPlatformExtensionsString());
            sb.append(Platform.getNewline());
        }
        sb.append(VersionUtil.SEPERATOR);

        return sb;
    }

    public StringBuilder getBriefOSGLBuildInfo(final GL gl, StringBuilder sb) {
        if(null==sb) {
            sb = new StringBuilder();
        }
        sb.append("OS: ").append(Platform.getOSName()).append(", version ").append(Platform.getOSVersion()).append(", arch ").append(Platform.getArchName());
        sb.append(Platform.getNewline());
        sb.append("GL_VENDOR     ").append(gl.glGetString(GL.GL_VENDOR));
        sb.append(Platform.getNewline());
        sb.append("GL_RENDERER   ").append(gl.glGetString(GL.GL_RENDERER));
        sb.append(Platform.getNewline());
        sb.append("GL_VERSION    ").append(gl.glGetString(GL.GL_VERSION));
        sb.append(Platform.getNewline());
        sb.append("JOGL GIT sha1 ").append(getImplementationCommit());
        sb.append(Platform.getNewline());
        return sb;
    }

    public static void main(final String args[]) {
        System.err.println(VersionUtil.getPlatformInfo());
        System.err.println(GlueGenVersion.getInstance());
        // System.err.println(NativeWindowVersion.getInstance());
        System.err.println(JoglVersion.getInstance());
    }
}

