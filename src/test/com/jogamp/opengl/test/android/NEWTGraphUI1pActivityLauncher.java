package com.jogamp.opengl.test.android;

import java.util.Arrays;
import java.util.List;

import com.jogamp.opengl.test.android.LauncherUtil.OrderedProperties;

public class NEWTGraphUI1pActivityLauncher extends LauncherUtil.BaseActivityLauncher {
    static String demo = "com.jogamp.opengl.test.android.NEWTGraphUI1pActivity";
    static String[] sys_pkgs = new String[] { "com.jogamp.common", "com.jogamp.opengl" };
    static String[] usr_pkgs = new String[] { "com.jogamp.opengl.test" };

    @Override
    public void init() {
       final OrderedProperties props = getProperties();
       // props.setProperty("jogamp.debug.JNILibLoader", "true");
       // props.setProperty("jogamp.debug.NativeLibrary", "true");
       // props.setProperty("jogamp.debug.NativeLibrary.Lookup", "true");
       // props.setProperty("jogamp.debug.IOUtil", "true");
       // props.setProperty("nativewindow.debug", "all");
       props.setProperty("nativewindow.debug.GraphicsConfiguration", "true");
       // props.setProperty("jogl.debug", "all");
       // props.setProperty("jogl.debug.GLProfile", "true");
       props.setProperty("jogl.debug.GLDrawable", "true");
       props.setProperty("jogl.debug.GLContext", "true");
       props.setProperty("jogl.debug.GLSLCode", "true");
       props.setProperty("jogl.debug.CapabilitiesChooser", "true");
       // props.setProperty("jogl.debug.GLSLState", "true");
       // props.setProperty("jogl.debug.DebugGL", "true");
       // props.setProperty("jogl.debug.TraceGL", "true");
       // props.setProperty("newt.debug", "all");
       props.setProperty("newt.debug.Window", "true");
       // props.setProperty("newt.debug.Window.MouseEvent", "true");
       // props.setProperty("newt.debug.Window.KeyEvent", "true");
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
