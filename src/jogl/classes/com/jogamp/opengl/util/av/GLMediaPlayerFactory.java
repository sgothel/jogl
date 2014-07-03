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
package com.jogamp.opengl.util.av;

import jogamp.opengl.util.av.NullGLMediaPlayer;

import com.jogamp.common.util.ReflectionUtil;

public class GLMediaPlayerFactory {
    private static final String AndroidGLMediaPlayerAPI14ClazzName = "jogamp.opengl.android.av.AndroidGLMediaPlayerAPI14";
    private static final String FFMPEGMediaPlayerClazzName = "jogamp.opengl.util.av.impl.FFMPEGMediaPlayer";
    private static final String OMXGLMediaPlayerClazzName = "jogamp.opengl.util.av.impl.OMXGLMediaPlayer";
    private static final String isAvailableMethodName = "isAvailable";

    public static GLMediaPlayer createDefault() {
        final ClassLoader cl = GLMediaPlayerFactory.class.getClassLoader();
        GLMediaPlayer sink = create(cl, OMXGLMediaPlayerClazzName);
        if( null == sink ) {
            sink = create(cl, AndroidGLMediaPlayerAPI14ClazzName);
        }
        if( null == sink ) {
            sink = create(cl, FFMPEGMediaPlayerClazzName);
        }
        if( null == sink ) {
            sink = createNull();
        }
        return sink;
    }
    public static GLMediaPlayer createNull() {
        return new NullGLMediaPlayer();
    }

    public static GLMediaPlayer create(final ClassLoader cl, final String implName) {
        try {
            if(((Boolean)ReflectionUtil.callStaticMethod(implName, isAvailableMethodName, null, null, cl)).booleanValue()) {
                return (GLMediaPlayer) ReflectionUtil.createInstance(implName, cl);
            }
        } catch (final Throwable t) { if(GLMediaPlayer.DEBUG) { System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage()); t.printStackTrace(); } }
        return null;
    }
}
