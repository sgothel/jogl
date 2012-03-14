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

public class NEWTGearsES2ActivityLauncher extends LauncherUtil.BaseActivityLauncher {

    static String demo = "com.jogamp.opengl.test.android.NEWTGearsES2Activity";
    static String[] pkgs = new String[] { "com.jogamp.opengl.test" };
    
    @Override
    public void init() {
       final OrderedProperties props = getProperties();       
       // props.setProperty("jogamp.debug.JNILibLoader", "true");
       // props.setProperty("jogamp.debug.NativeLibrary", "true");
       // properties.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
       // properties.setProperty("jogamp.debug.IOUtil", "true");       
       // properties.setProperty("nativewindow.debug", "all");
       props.setProperty("nativewindow.debug.GraphicsConfiguration", "true");
       // properties.setProperty("jogl.debug", "all");
       // properties.setProperty("jogl.debug.GLProfile", "true");
       props.setProperty("jogl.debug.GLDrawable", "true");
       props.setProperty("jogl.debug.GLContext", "true");
       props.setProperty("jogl.debug.GLSLCode", "true");
       props.setProperty("jogl.debug.CapabilitiesChooser", "true");       
       // properties.setProperty("jogl.debug.GLSLState", "true");
       // properties.setProperty("jogl.debug.DebugGL", "true");
       // properties.setProperty("jogl.debug.TraceGL", "true");
       // properties.setProperty("newt.debug", "all");
       props.setProperty("newt.debug.Window", "true");
       // properties.setProperty("newt.debug.Window.MouseEvent", "true");
       // properties.setProperty("newt.debug.Window.KeyEvent", "true");
    }
    
    @Override
    public String getActivityName() {
        return demo;
    }
    @Override
    public List<String> getPackages() {
        return Arrays.asList(pkgs);
    }
}
