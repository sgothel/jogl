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
 
#ifdef _WIN32
    #include <windows.h>
    // __declspec(dllimport) void __stdcall Sleep(unsigned long dwMilliseconds);

    #define usleep(t)    Sleep((t) / 1000)
#endif

#include <gluegen_stdint.h>
#include <gluegen_inttypes.h>
#include <gluegen_stddef.h>
#include <gluegen_stdint.h>

#include "jogamp_opengl_util_av_impl_FFMPEGStaticNatives.h"

#include "JoglCommon.h"

#include <GL/gl.h>

typedef unsigned (APIENTRYP AV_GET_VERSION)(void);

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_getAvUtilVersion0
  (JNIEnv *env, jclass clazz, jlong func) {
    if( 0 != func ) {
        return (jint) ((AV_GET_VERSION)func)();
    } else {
        return 0;
    }
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_getAvFormatVersion0
  (JNIEnv *env, jclass clazz, jlong func) {
    if( 0 != func ) {
        return (jint) ((AV_GET_VERSION)func)();
    } else {
        return 0;
    }
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_getAvCodecVersion0
  (JNIEnv *env, jclass clazz, jlong func) {
    if( 0 != func ) {
        return (jint) ((AV_GET_VERSION)func)();
    } else {
        return 0;
    }
}

JNIEXPORT jint JNICALL Java_jogamp_opengl_util_av_impl_FFMPEGStaticNatives_getAvResampleVersion0
  (JNIEnv *env, jclass clazz, jlong func) {
    if( 0 != func ) {
        return (jint) ((AV_GET_VERSION)func)();
    } else {
        return 0;
    }
}

