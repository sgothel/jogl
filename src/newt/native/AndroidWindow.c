
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

#include <gluegen_stdint.h>

#include <unistd.h>
#include <errno.h>

#include "jogamp_newt_driver_android_AndroidWindow.h"

#include <android/native_window.h>
#include <android/native_window_jni.h>

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif


JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_android_AndroidWindow_getSurfaceHandle
    (JNIEnv *env, jclass clazz, jobject surface)
{
    ANativeWindow * anw = ANativeWindow_fromSurface(env, surface);
    return (jlong) (intptr_t) anw;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_android_AndroidWindow_getSurfaceVisualID
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    return (jint) ANativeWindow_getFormat(anw);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_AndroidWindow_setSurfaceVisualID
    (JNIEnv *env, jclass clazz, jlong surfaceHandle, jint nativeVisualID)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_setBuffersGeometry(anw, 0, 0, nativeVisualID);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_AndroidWindow_acquire
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_acquire(anw);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_android_AndroidWindow_release
    (JNIEnv *env, jclass clazz, jlong surfaceHandle)
{
    ANativeWindow * anw = (ANativeWindow *) (intptr_t) surfaceHandle;
    ANativeWindow_release(anw);
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_android_AndroidWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "initIDs ok\n" );
    return JNI_TRUE;
}

