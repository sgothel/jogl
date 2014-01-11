
#include "JoglCommon.h"

#include <assert.h>
#include <KHR/khrplatform.h>

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

static JavaVM *_jvmHandle = NULL;
static int _jvmVersion = 0;

void JoglCommon_init(JNIEnv *env) {
    if(NULL==_jvmHandle) {
        if(0 != (*env)->GetJavaVM(env, &_jvmHandle)) {
            JoglCommon_FatalError(env, "JOGL: Can't fetch JavaVM handle");
        } else {
            _jvmVersion = (*env)->GetVersion(env);
        }
        jclass c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            JoglCommon_FatalError(env, "JOGL: Can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            JoglCommon_FatalError(env, "JOGL: Can't use %s", ClazzNameRuntimeException);
        }
    }
}

void JoglCommon_FatalError(JNIEnv *env, const char* msg, ...)
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

void JoglCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if(NULL==_jvmHandle) {
        JoglCommon_FatalError(env, "JOGL: NULL JVM handle, call JoglCommon_init 1st\n");
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

jchar* JoglCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str)
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

JNIEnv* JoglCommon_GetJNIEnv (int asDaemon, int * shallBeDetached) {
    JNIEnv* curEnv = NULL;
    JNIEnv* newEnv = NULL;
    int envRes;

    if(NULL==_jvmHandle) {
        fprintf(stderr, "JOGL GetJNIEnv: NULL JVM handle, call JoglCommon_init 1st\n");
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
            fprintf(stderr, "JOGL GetJNIEnv: Can't attach thread: %d\n", envRes);
            return NULL;
        }
        curEnv = newEnv;
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "JOGL GetJNIEnv: Can't GetEnv: %d\n", envRes);
        return NULL;
    }
    if (curEnv==NULL) {
        fprintf(stderr, "JOGL GetJNIEnv: env is NULL\n");
        return NULL;
    }
    *shallBeDetached = NULL != newEnv;
    return curEnv;
}

void JoglCommon_ReleaseJNIEnv (int shallBeDetached) {
    if(NULL == _jvmHandle) {
        fprintf(stderr, "JOGL ReleaseJNIEnv: No JavaVM handle registered, call JoglCommon_init(..) 1st");
    } else if(shallBeDetached) {
        (*_jvmHandle)->DetachCurrentThread(_jvmHandle);
    }
}

