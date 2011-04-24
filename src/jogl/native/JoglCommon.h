
#ifndef JOGL_COMMON_H
#define JOGL_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

void JoglCommon_init(JNIEnv *env);

jchar* JoglCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

void JoglCommon_FatalError(JNIEnv *env, const char* msg, ...);
void JoglCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

#endif
