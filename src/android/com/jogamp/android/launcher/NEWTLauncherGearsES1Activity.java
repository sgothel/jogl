package com.jogamp.android.launcher;

public class NEWTLauncherGearsES1Activity extends NEWTLauncherActivity {
    static String demo = "com.jogamp.opengl.test.android.NEWTGearsES1Activity";
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
