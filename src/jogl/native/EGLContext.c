#include <jni.h>
#include <stdlib.h>
#include <string.h>

#include <assert.h>

#include <stdio.h> /* android */
#include <gluegen_stdint.h>
#include <gluegen_stddef.h>
#include <EGL/egl.h>

/*   Java->C glue code:
 *   Java package: jogamp.opengl.egl.EGLContext
 *    Java method: long dispatch_eglGetProcAddress(java.lang.String procname)
 *     C function: __EGLFuncPtr eglGetProcAddress(const char *  procname)
 */
JNIEXPORT jlong JNICALL 
Java_jogamp_opengl_egl_EGLContext_dispatch_1eglGetProcAddress0__Ljava_lang_String_2J(JNIEnv *env, jclass _unused, jstring procname, jlong procAddress) {
  typedef void (* EGLAPIENTRY _local_EGLFuncPtr)(void);
  typedef _local_EGLFuncPtr (EGLAPIENTRY*_local_PFNEGLGETPROCADDRESSPROC)(const char *  procname);
  _local_PFNEGLGETPROCADDRESSPROC ptr_eglGetProcAddress;
  const char* _strchars_procname = NULL;
  _local_EGLFuncPtr _res;
  if ( NULL != procname ) {
    _strchars_procname = (*env)->GetStringUTFChars(env, procname, (jboolean*)NULL);
  if ( NULL == _strchars_procname ) {
      (*env)->ThrowNew(env, (*env)->FindClass(env, "java/lang/OutOfMemoryError"),
                       "Failed to get UTF-8 chars for argument \"procname\" in native dispatcher for \"dispatch_eglGetProcAddress\"");
      return 0;
    }
  }
  ptr_eglGetProcAddress = (_local_PFNEGLGETPROCADDRESSPROC) (intptr_t) procAddress;
  assert(ptr_eglGetProcAddress != NULL);
  _res = (* ptr_eglGetProcAddress) ((const char *) _strchars_procname);
  if ( NULL != procname ) {
    (*env)->ReleaseStringUTFChars(env, procname, _strchars_procname);
  }
  return (jlong) (intptr_t) _res;
}

