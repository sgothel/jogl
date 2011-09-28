package com.jogamp.android.launcher;

public class NEWTLauncherGraphUIActivity extends NEWTLauncherActivity {
    static String demo = "com.jogamp.opengl.test.android.NEWTGraphUIActivity";

    @Override
    public String getDownstreamActivityName() {
        return demo;
    }

}
