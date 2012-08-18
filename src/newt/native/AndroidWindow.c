
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

