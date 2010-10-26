
#ifndef NEWT_COMMON_H
#define NEWT_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

void NewtCommon_init(JNIEnv *env);

jchar* NewtCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

void NewtCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

#endif
