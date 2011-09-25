
#include "NewtCommon.h"

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

void NewtCommon_FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    fprintf(stderr, "%s\n", buffer);
    (*env)->FatalError(env, buffer);
}

void NewtCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    (*env)->ThrowNew(env, runtimeExceptionClz, buffer);
}

void NewtCommon_init(JNIEnv *env) {
    if(NULL==runtimeExceptionClz) {
        jclass c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            NewtCommon_FatalError(env, "NEWT: can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            NewtCommon_FatalError(env, "NEWT: can't use %s", ClazzNameRuntimeException);
        }
    }
}

jchar* NewtCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str)
{
    jchar* strChars = NULL;
    strChars = calloc((*env)->GetStringLength(env, str) + 1, sizeof(jchar));
    if (strChars != NULL) {
        (*env)->GetStringRegion(env, str, 0, (*env)->GetStringLength(env, str), strChars);
    }
    return strChars;
}

JNIEnv* NewtCommon_GetJNIEnv(JavaVM * jvmHandle, int jvmVersion, int * shallBeDetached) {
    JNIEnv* curEnv = NULL;
    JNIEnv* newEnv = NULL;
    int envRes;

    // retrieve this thread's JNIEnv curEnv - or detect it's detached
    envRes = (*jvmHandle)->GetEnv(jvmHandle, (void **) &curEnv, jvmVersion) ;
    if( JNI_EDETACHED == envRes ) {
        // detached thread - attach to JVM
        if( JNI_OK != ( envRes = (*jvmHandle)->AttachCurrentThread(jvmHandle, (void**) &newEnv, NULL) ) ) {
            fprintf(stderr, "JNIEnv: can't attach thread: %d\n", envRes);
            return NULL;
        }
        curEnv = newEnv;
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "can't GetEnv: %d\n", envRes);
        return NULL;
    }
    if (curEnv==NULL) {
        fprintf(stderr, "env is NULL\n");
        return NULL;
    }
    *shallBeDetached = NULL != newEnv;
    return curEnv;
}

