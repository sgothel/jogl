/**
 * Copyright 2013 JogAmp Community. All rights reserved.
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

import jogamp.opengl.util.av.NullAudioSink;

import com.jogamp.common.util.ReflectionUtil;

public class AudioSinkFactory {
    private static final String ALAudioSinkClazzName = "jogamp.opengl.openal.av.ALAudioSink";
    private static final String JavaAudioSinkClazzName = "jogamp.opengl.util.av.JavaSoundAudioSink";

    public static AudioSink createDefault() {
        final ClassLoader cl = GLMediaPlayerFactory.class.getClassLoader();
        AudioSink sink = create(cl, ALAudioSinkClazzName);
        if( null == sink ) {
            sink = create(cl, JavaAudioSinkClazzName);
        }
        if( null == sink ) {
            sink = createNull();
        }
        return sink;
    }
    public static AudioSink createNull() {
        return new NullAudioSink();
    }

    public static AudioSink create(final ClassLoader cl, final String implName) {
        final AudioSink audioSink;
        if(ReflectionUtil.isClassAvailable(implName, cl)){
            try {
                audioSink = (AudioSink) ReflectionUtil.createInstance(implName, cl);
                if( audioSink.isInitialized() ) {
                    return audioSink;
                }
            } catch (final Throwable t) {
                if(AudioSink.DEBUG) { System.err.println("Caught "+t.getClass().getName()+": "+t.getMessage()); t.printStackTrace(); }
            }
        }
        return null;
    }

}
