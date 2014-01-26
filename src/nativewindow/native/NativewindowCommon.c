
#include "NativewindowCommon.h"
#include <string.h>
#include <sys/time.h>

// #define STDERR_TO_FILE 1

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

static JavaVM *_jvmHandle = NULL;
static int _jvmVersion = 0;

int NativewindowCommon_init(JNIEnv *env) {
    if(NULL==_jvmHandle) {
        if(0 != (*env)->GetJavaVM(env, &_jvmHandle)) {
            NativewindowCommon_FatalError(env, "Nativewindow: Can't fetch JavaVM handle");
        } else {
            _jvmVersion = (*env)->GetVersion(env);
        }
        jclass c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "Nativewindow: Can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            NativewindowCommon_FatalError(env, "Nativewindow: Can't use %s", ClazzNameRuntimeException);
        }
        #ifdef STDERR_TO_FILE
            FILE * old_stderr = stderr; 
            FILE * new_stderr = freopen("jogamp_stderr.log", "w", stderr);
            fprintf(stderr, "STDERR_TO_FILE: %p -> %p\n", old_stderr, new_stderr);
        #endif
        return 1;
    }
    return 0;
}

void NativewindowCommon_FatalError(JNIEnv *env, const char* msg, ...)
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

void NativewindowCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if(NULL==_jvmHandle) {
        NativewindowCommon_FatalError(env, "Nativewindow: NULL JVM handle, call NativewindowCommon_init 1st\n");
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

const char * NativewindowCommon_GetStaticStringMethod(JNIEnv *jniEnv, jclass clazz, jmethodID jGetStrID, char *dest, int destSize, const char *altText) {
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

jchar* NativewindowCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str)
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

JNIEnv* NativewindowCommon_GetJNIEnv (int asDaemon, int * shallBeDetached) {
    JNIEnv* curEnv = NULL;
    JNIEnv* newEnv = NULL;
    int envRes;

    if(NULL==_jvmHandle) {
        fprintf(stderr, "Nativewindow GetJNIEnv: NULL JVM handle, call NativewindowCommon_init 1st\n");
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
            fprintf(stderr, "Nativewindow GetJNIEnv: Can't attach thread: %d\n", envRes);
            return NULL;
        }
        curEnv = newEnv;
    } else if( JNI_OK != envRes ) {
        // oops ..
        fprintf(stderr, "Nativewindow GetJNIEnv: Can't GetEnv: %d\n", envRes);
        return NULL;
    }
    if (curEnv==NULL) {
        fprintf(stderr, "Nativewindow GetJNIEnv: env is NULL\n");
        return NULL;
    }
    *shallBeDetached = NULL != newEnv;
    return curEnv;
}

void NativewindowCommon_ReleaseJNIEnv (int shallBeDetached) {
    if(NULL == _jvmHandle) {
        fprintf(stderr, "Nativewindow ReleaseJNIEnv: No JavaVM handle registered, call NativewindowCommon_init(..) 1st");
    } else if(shallBeDetached) {
        (*_jvmHandle)->DetachCurrentThread(_jvmHandle);
    }
}

int64_t NativewindowCommon_CurrentTimeMillis() {
    struct timeval tv;
    gettimeofday(&tv,NULL);
    return (int64_t)tv.tv_sec * 1000 + tv.tv_usec / 1000;
}

