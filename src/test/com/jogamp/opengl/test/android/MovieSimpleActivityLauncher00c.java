/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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
package com.jogamp.opengl.test.android;

import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.test.android.LauncherUtil.OrderedProperties;

public class MovieSimpleActivityLauncher00c extends LauncherUtil.BaseActivityLauncher {

    static String demo = "com.jogamp.opengl.test.android.MovieSimpleActivity0";
    static String[] sys_pkgs = new String[] { "com.jogamp.common", "com.jogamp.opengl" };
    static String[] usr_pkgs = new String[] { "com.jogamp.opengl.test" };

    @Override
    public void init() {
       final OrderedProperties props = getProperties();
       props.setProperty("jnlp.media0_url0", "camera:/0");
       props.setProperty("jnlp.media0_url1", "");
       props.setProperty("jnlp.media0_url2", "");
       // props.setProperty("jogamp.debug.JNILibLoader", "true");
       // props.setProperty("jogamp.debug.NativeLibrary", "true");
       // props.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
       // props.setProperty("jogamp.debug.IOUtil", "true");
       // props.setProperty("nativewindow.debug", "all");
       // props.setProperty("nativewindow.debug.GraphicsConfiguration", "true");
       // props.setProperty("jogl.debug", "all");
       // props.setProperty("jogl.debug.GLProfile", "true");
       // props.setProperty("jogl.debug.GLDrawable", "true");
       props.setProperty("jogl.debug.GLContext", "true");
       props.setProperty("jogl.debug.GLMediaPlayer", "true");
       props.setProperty("jogl.debug.GLSLCode", "true");
       // props.setProperty("jogl.debug.CapabilitiesChooser", "true");
       // props.setProperty("jogl.debug.GLSLState", "true");
       // props.setProperty("jogl.debug.DebugGL", "true");
       // props.setProperty("jogl.debug.TraceGL", "true");
       // props.setProperty("newt.debug", "all");
       // props.setProperty("newt.debug.Window", "true");
       // props.setProperty("newt.debug.Window.MouseEvent", "true");
       // props.setProperty("newt.debug.Window.KeyEvent", "true");
       // props.setProperty("jogamp.debug.IOUtil", "true");
    }

    @Override
    public String getActivityName() {
        return demo;
    }
    @Override
    public List<String> getSysPackages() {
        return Arrays.asList(sys_pkgs);
    }

    @Override
    public List<String> getUsrPackages() {
        return Arrays.asList(usr_pkgs);
    }
}
