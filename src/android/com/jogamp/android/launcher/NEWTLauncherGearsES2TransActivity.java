package com.jogamp.android.launcher;

public class NEWTLauncherGearsES2TransActivity extends NEWTLauncherActivity {
    static String demo = "com.jogamp.opengl.test.android.NEWTGearsES2TransActivity";
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
