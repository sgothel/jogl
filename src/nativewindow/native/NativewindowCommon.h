
#ifndef NATIVEWINDOW_COMMON_H
#define NATIVEWINDOW_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

int NativewindowCommon_init(JNIEnv *env);

jchar* NativewindowCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

void NativewindowCommon_FatalError(JNIEnv *env, const char* msg, ...);
void NativewindowCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

JNIEnv* NativewindowCommon_GetJNIEnv (JavaVM * jvmHandle, int jvmVersion, int * shallBeDetached);

#endif
