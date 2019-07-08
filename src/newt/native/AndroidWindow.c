/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

#include <gluegen_stdint.h>

#include <unistd.h>
#include <errno.h>

#include "jogamp_newt_driver_android_WindowDriver.h"

#include <android/native_window.h>
#include <android/native_window_jni.h>

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif


JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_android_WindowDriver_getSurfaceHandle0
    (JNIEnv *env, jclass clazz, jobject surface)
{
    ANativeWindow * anw = ANativeWindow_fromSurface(env, surface);
    return (jlong) (intptr_t) anw;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_android_WindowDriver_getSurfaceVisualID0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    return (jint) ANativeWindow_getFormat(anw);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_WindowDriver_setSurfaceVisualID0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle, jint nativeVisualID)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_setBuffersGeometry(anw, 0, 0, nativeVisualID);
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_android_WindowDriver_getWidth0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    return (jint) ANativeWindow_getWidth(anw);
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_android_WindowDriver_getHeight0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    return (jint) ANativeWindow_getHeight(anw);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_WindowDriver_acquire0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_acquire(anw);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_WindowDriver_release0
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_release(anw);
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_android_WindowDriver_initIDs0
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "initIDs ok\n" );
    return JNI_TRUE;
}

