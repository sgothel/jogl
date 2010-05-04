
#ifndef NEWT_COMMON_H
#define NEWT_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

jchar* NewtCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

#endif
