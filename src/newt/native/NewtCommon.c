
#include "NewtCommon.h"
#include <string.h>

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

void NewtCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if(NULL==_jvmHandle) {
        NewtCommon_FatalError(env, "NEWT: NULL JVM handle, call NewtCommon_init 1st\n");
        return;
    }

    if( NULL != msg ) {
        va_start(ap, msg);
        vsnprintf(buffer, sizeof(buffer), msg, ap);
        va_end(ap);

        if(NULL != env) {
            (*env)->ThrowNew(env, runtimeExceptionClz, buffer);
        }
    }
}

const char * NewtCommon_GetStaticStringMethod(JNIEnv *jniEnv, jclass clazz, jmethodID jGetStrID, char *dest, int destSize, const char *altText) {
    if(NULL != jniEnv && NULL != clazz && NULL != jGetStrID) {
        jstring jstr = (jstring) (*jniEnv)->CallStaticObjectMethod(jniEnv, clazz, jGetStrID);
        if(NULL != jstr) {
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

