package com.jogamp.opengl.av;

import com.jogamp.common.os.AndroidVersion;
import com.jogamp.common.os.Platform;
import com.jogamp.common.util.ReflectionUtil;

public class GLMediaPlayerFactory {
    private static final String AndroidGLMediaPlayerAPI14ClazzName = "jogamp.opengl.android.av.AndroidGLMediaPlayerAPI14";
    
    public static GLMediaPlayer create() {
        if(Platform.OS_TYPE.equals(Platform.OSType.ANDROID)) {
            if(AndroidVersion.SDK_INT >= 14) {
                return (GLMediaPlayer) ReflectionUtil.createInstance(AndroidGLMediaPlayerAPI14ClazzName, GLMediaPlayerFactory.class.getClassLoader());
            }
        }
        return null;
    }
}
