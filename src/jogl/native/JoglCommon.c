
#include "jogamp_opengl_GLContextImpl.h"
#include "JoglCommon.h"

#include <assert.h>
#include <KHR/khrplatform.h>

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

void JoglCommon_FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    fprintf(stderr, "%s\n", buffer);
    (*env)->FatalError(env, buffer);
}

void JoglCommon_throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    (*env)->ThrowNew(env, runtimeExceptionClz, buffer);
}

void JoglCommon_init(JNIEnv *env) {
    if(NULL==runtimeExceptionClz) {
        jclass c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            JoglCommon_FatalError(env, "JOGL: can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            JoglCommon_FatalError(env, "JOGL: can't use %s", ClazzNameRuntimeException);
        }
    }
}

jchar* JoglCommon_GetNullTerminatedStringChars(JNIEnv* env, jstring str)
{
    jchar* strChars = NULL;
    strChars = calloc((*env)->GetStringLength(env, str) + 1, sizeof(jchar));
    if (strChars != NULL) {
        (*env)->GetStringRegion(env, str, 0, (*env)->GetStringLength(env, str), strChars);
    }
    return strChars;
}

/*
 * Class:     jogamp_opengl_GLContextImpl
 * Method:    glGetStringInt
 * Signature: (IJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL 
Java_jogamp_opengl_GLContextImpl_glGetStringInt(JNIEnv *env, jclass _unused, jint name, jlong procAddress) {
  typedef const khronos_uint8_t *  (KHRONOS_APIENTRY*_local_PFNGLGETSTRINGPROC)(unsigned int name);
  _local_PFNGLGETSTRINGPROC ptr_glGetString;
  const khronos_uint8_t *  _res;
  ptr_glGetString = (_local_PFNGLGETSTRINGPROC) (intptr_t) procAddress;
  assert(ptr_glGetString != NULL);
  _res = (* ptr_glGetString) ((unsigned int) name);
  if (NULL == _res) return NULL;
  return (*env)->NewStringUTF(env, _res);
}

