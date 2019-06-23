package com.jogamp.opengl.demos.ios;

import com.jogamp.opengl.GLProfile;

import jogamp.nativewindow.ios.IOSUtil;

public class Hello0 {
    public static void main(final String[] args) {
        System.out.println("Hello JogAmp World");
        GLProfile.initSingleton();
        {
            IOSUtil.RunOnMainThread(true, false, new Runnable() {
                @Override
                public void run() {
                    IOSUtil.CreateGLViewDemoA();
                } } );
        }
    }

}
