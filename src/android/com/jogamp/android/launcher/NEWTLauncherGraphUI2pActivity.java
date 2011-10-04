package com.jogamp.android.launcher;

public class NEWTLauncherGraphUI2pActivity extends NEWTLauncherActivity {
    static String demo = "com.jogamp.opengl.test.android.NEWTGraphUI2pActivity";
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
