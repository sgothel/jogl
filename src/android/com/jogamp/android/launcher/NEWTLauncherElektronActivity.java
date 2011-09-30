package com.jogamp.android.launcher;

public class NEWTLauncherElektronActivity extends NEWTLauncherActivity {
    static String demo = "com.jogamp.opengl.test.android.NEWTElektronActivity";
    static String pkg = "com.jogamp.opengl.test";
    
    @Override
    public String getUserActivityName() {
        return demo;
    }
    @Override
    public String getUserPackageName() {
        return pkg;
    }
}
