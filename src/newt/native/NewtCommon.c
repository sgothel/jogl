
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

