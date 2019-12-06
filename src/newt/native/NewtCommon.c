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
#include "NewtCommon.h"
#include <string.h>
#include <math.h>

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

static JavaVM *_jvmHandle = NULL;
static int _jvmVersion = 0;

void NewtCommon_init(JNIEnv *env) {
    if(NULL==_jvmHandle) {
        if(0 != (*env)->GetJavaVM(env, &_jvmHandle)) {
            NewtCommon_FatalError(env, "NEWT: Can't fetch JavaVM handle");
        } else {
            _jvmVersion = (*env)->GetVersion(env);
        }
        jclass c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            NewtCommon_FatalError(env, "NEWT: Can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            NewtCommon_FatalError(env, "NEWT: Can't use %s", ClazzNameRuntimeException);
        }
    }
}

void NewtCommon_FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if( NULL != msg ) {
        va_start(ap, msg);
        vsnprintf(buffer, sizeof(buffer), msg, ap);
        va_end(ap);

        fprintf(stderr, "%s\n", buffer);
        if(NULL != env) {
            (*env)->FatalError(env, buffer);
        }
    }
}

static void NewtCommon_throwNewRuntimeExceptionVA(JNIEnv *env, const char* msg, va_list ap)
{
    char buffer[512];

    if(NULL==_jvmHandle) {
        NewtCommon_FatalError(env, "NEWT: NULL JVM handle, call NewtCommon_init 1st\n");
        return;
    }

    vsnprintf(buffer, sizeof(buffer), msg, ap);

    if(NULL != env) {
        (*env)->ThrowNew(env, runtimeExceptionClz, buffer);
    }
}

void NewtCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    va_list ap;

    if( NULL != msg ) {
        va_start(ap, msg);
        NewtCommon_throwNewRuntimeExceptionVA(env, msg, ap);
        va_end(ap);
    }
}

jboolean NewtCommon_ExceptionCheck0(JNIEnv *env)
{
    if( (*env)->ExceptionCheck(env) ) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

jboolean NewtCommon_ExceptionCheck1_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    va_list ap;

    if( (*env)->ExceptionCheck(env) ) {
        (*env)->ExceptionDescribe(env);
        (*env)->ExceptionClear(env);
        va_start(ap, msg);
        NewtCommon_throwNewRuntimeExceptionVA(env, msg, ap);
        va_end(ap);
        return JNI_TRUE;
    } else {
        return JNI_FALSE;
    }
}

const char * NewtCommon_GetStaticStringMethod(JNIEnv *jniEnv, jclass clazz, jmethodID jGetStrID, char *dest, int destSize, const char *altText) {
    if(NULL != jniEnv && NULL != clazz && NULL != jGetStrID) {
        jstring jstr = (jstring) (*jniEnv)->CallStaticObjectMethod(jniEnv, clazz, jGetStrID);
        if( !NewtCommon_ExceptionCheck0(jniEnv) && NULL != jstr) {
            const char * str = (*jniEnv)->GetStringUTFChars(jniEnv, jstr, NULL);
            if( NULL != str) {
                strncpy(dest, str, destSize-1);
                dest[destSize-1] = 0; // EOS
                (*jniEnv)->ReleaseStringUTFChars(jniEnv, jstr, str);
                return dest;
            }
        }
    }
    strncpy(dest, altText, destSize-1);
    dest[destSize-1] = 0; // EOS
    return dest;
}

jchar* NewtCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str)
{
    jchar* strChars = NULL;
    if( NULL != env && 0 != str ) {
        strChars = calloc((*env)->GetStringLength(env, str) + 1, sizeof(jchar));
        if (strChars != NULL) {
            (*env)->GetStringRegion(env, str, 0, (*env)->GetStringLength(env, str), strChars);
        }
    }
    return strChars;
}

JNIEnv* NewtCommon_GetJNIEnv(int asDaemon, int * shallBeDetached) {
    JNIEnv* curEnv = NULL;
    JNIEnv* newEnv = NULL;
    int envRes;

    if(NULL==_jvmHandle) {
        fprintf(stderr, "NEWT GetJNIEnv: NULL JVM handle, call NewtCommon_init 1st\n");
        return NULL;
    }

    // retrieve this thread's JNIEnv curEnv - or detect it's detached
    envRes = (*_jvmHandle)->GetEnv(_jvmHandle, (void **) &curEnv, _jvmVersion) ;
    if( JNI_EDETACHED == envRes ) {
        // detached thread - attach to JVM
        if( asDaemon ) {
            envRes = (*_jvmHandle)->AttachCurrentThreadAsDaemon(_jvmHandle, (void**) &newEnv, NULL);
        } else {
            envRes = (*_jvmHandle)->AttachCurrentThread(_jvmHandle, (void**) &newEnv, NULL);
        }
        if( JNI_OK != envRes ) {
            fprintf(stderr, "NEWT GetJNIEnv: Can't attach thread: %d\n", envRes);
            return NULL;
        }
        curEnv = newEnv;
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "NEWT GetJNIEnv: Can't GetEnv: %d\n", envRes);
        return NULL;
    }
    if (curEnv==NULL) {
        fprintf(stderr, "NEWT GetJNIEnv: env is NULL\n");
        return NULL;
    }
    *shallBeDetached = NULL != newEnv;
    return curEnv;
}

void NewtCommon_ReleaseJNIEnv (int shallBeDetached) {
    if(NULL == _jvmHandle) {
        fprintf(stderr, "NEWT ReleaseJNIEnv: No JavaVM handle registered, call NewtCommon_init(..) 1st");
    } else if(shallBeDetached) {
        (*_jvmHandle)->DetachCurrentThread(_jvmHandle);
    }
}

int NewtCommon_isFloatZero(float f) {
    // EPSILON = 1.1920929E-7f; // Float.MIN_VALUE == 1.4e-45f ; double EPSILON 2.220446049250313E-16d
    return fabsf(f) < 1.1920929E-7f;
}
