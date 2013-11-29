
#ifndef NATIVEWINDOW_COMMON_H
#define NATIVEWINDOW_COMMON_H 1

#include <jni.h>
#include <stdlib.h>
#include <gluegen_stdint.h>

int NativewindowCommon_init(JNIEnv *env);

const char * NativewindowCommon_GetStaticStringMethod(JNIEnv *jniEnv, jclass clazz, jmethodID jGetStrID, char *dest, int destSize, const char *altText);
jchar* NativewindowCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

void NativewindowCommon_FatalError(JNIEnv *env, const char* msg, ...);
void NativewindowCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

JNIEnv* NativewindowCommon_GetJNIEnv (JavaVM * jvmHandle, int jvmVersion, int asDaemon, int * shallBeDetached);

int64_t NativewindowCommon_CurrentTimeMillis();

#endif
