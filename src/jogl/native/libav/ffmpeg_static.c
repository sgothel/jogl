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
 
#include "ffmpeg_static.h"

#include "JoglCommon.h"

#include <GL/gl-platform.h>

static const char * const ClazzNameFFMPEGMediaPlayer = "jogamp/opengl/util/av/impl/FFMPEGMediaPlayer";

static jclass ffmpegMediaPlayerClazz = NULL;
jmethodID ffmpeg_jni_mid_pushSound = NULL;
jmethodID ffmpeg_jni_mid_updateAttributes = NULL;
jmethodID ffmpeg_jni_mid_setIsGLOriented = NULL;
jmethodID ffmpeg_jni_mid_setupFFAttributes = NULL;
jmethodID ffmpeg_jni_mid_isAudioFormatSupported = NULL;

typedef unsigned (APIENTRYP AV_GET_VERSION)(void);

JNIEXPORT jboolean JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_initIDs0
  (JNIEnv *env, jclass clazz)
{
    jboolean res = JNI_TRUE;
    JoglCommon_init(env);

    jclass c;
    if (ffmpegMediaPlayerClazz != NULL) {
        return JNI_FALSE;
    }

    c = (*env)->FindClass(env, ClazzNameFFMPEGMediaPlayer);
    if(NULL==c) {
        JoglCommon_FatalError(env, "JOGL FFMPEG: can't find %s", ClazzNameFFMPEGMediaPlayer);
    }
    ffmpegMediaPlayerClazz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==ffmpegMediaPlayerClazz) {
        JoglCommon_FatalError(env, "JOGL FFMPEG: can't use %s", ClazzNameFFMPEGMediaPlayer);
    }

    ffmpeg_jni_mid_pushSound = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "pushSound", "(Ljava/nio/ByteBuffer;II)V");
    ffmpeg_jni_mid_updateAttributes = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "updateAttributes", "(IIIIIIIFIIILjava/lang/String;Ljava/lang/String;)V");
    ffmpeg_jni_mid_setIsGLOriented = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "setIsGLOriented", "(Z)V");
    ffmpeg_jni_mid_setupFFAttributes = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "setupFFAttributes", "(IIIIIIIIIIIIIII)V");
    ffmpeg_jni_mid_isAudioFormatSupported = (*env)->GetMethodID(env, ffmpegMediaPlayerClazz, "isAudioFormatSupported", "(III)Z");

    if(ffmpeg_jni_mid_pushSound == NULL ||
       ffmpeg_jni_mid_updateAttributes == NULL ||
       ffmpeg_jni_mid_setIsGLOriented == NULL ||
       ffmpeg_jni_mid_setupFFAttributes == NULL ||
       ffmpeg_jni_mid_isAudioFormatSupported == NULL) {
        return JNI_FALSE;
    }
    return res;
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_getAvVersion0
  (JNIEnv *env, jclass clazz, jlong func) {
    if( 0 != func ) {
        return (jint) ((AV_GET_VERSION) (intptr_t) func)();
    } else {
        return 0;
    }
}

