
#ifndef JOGL_COMMON_H
#define JOGL_COMMON_H 1

#include <jni.h>
#include <stdlib.h>

void JoglCommon_init(JNIEnv *env);

/** Set by JoglCommon_init */
JavaVM *JoglCommon_GetJVMHandle();

/** Set by JoglCommon_init */
int JoglCommon_GetJVMVersion();

jchar* JoglCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str);

/** env may be NULL, in which case JoglCommon_GetJNIEnv() is being used. */
void JoglCommon_FatalError(JNIEnv *env, const char* msg, ...);

/** env may be NULL, in which case JoglCommon_GetJNIEnv() is being used. */
void JoglCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...);

/**
 *
 * 1) Store jvmHandle and jvmVersion is done by 'JoglCommon_init(JNIEnv*)'
 *    and internally used by 'JoglCommon_GetJNIEnv(..)' and 'JoglCommon_ReleaseJNIEnv(..)'.
 *
 * 2) Use current thread JNIEnv or attach current thread to JVM, generating new JNIEnv
 *
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(&shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("drawRect: null JNIEnv\n");
        return;
    }
    
 *
 * 3) Use JNIEnv ..
 *
    .. your JNIEnv code here ..

 *
 * 4) Detach thread from JVM, if required
 *
    JoglCommon_ReleaseJNIEnv (shallBeDetached);
 */
JNIEnv* JoglCommon_GetJNIEnv (int * shallBeDetached);
void JoglCommon_ReleaseJNIEnv (int shallBeDetached);

#endif
