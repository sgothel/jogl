
#include "jogamp_opengl_GLContextImpl.h"
#include "JoglCommon.h"

#include <assert.h>
#include <KHR/khrplatform.h>

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
  return (*env)->NewStringUTF(env, (const char *)_res);
}

/*
 * Class:     jogamp_opengl_GLContextImpl
 * Method:    glGetIntegervInt
 * Signature: (ILjava/lang/Object;I)V
 */
JNIEXPORT void JNICALL 
Java_jogamp_opengl_GLContextImpl_glGetIntegervInt(JNIEnv *env, jclass _unused, jint pname, jobject params, jint params_byte_offset, jlong procAddress) {
  typedef void (KHRONOS_APIENTRY*_local_PFNGLGETINTEGERVPROC)(unsigned int pname, int * params);

  _local_PFNGLGETINTEGERVPROC ptr_glGetIntegerv;
  int * _params_ptr = NULL;
  if ( NULL != params ) {
    _params_ptr = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, params, NULL) ) + params_byte_offset);
  }
  ptr_glGetIntegerv = (_local_PFNGLGETINTEGERVPROC) (intptr_t) procAddress;
  assert(ptr_glGetIntegerv != NULL);
  (* ptr_glGetIntegerv) ((unsigned int) pname, (int *) _params_ptr);
  if ( NULL != params ) {
    (*env)->ReleasePrimitiveArrayCritical(env, params, _params_ptr, 0);
  }
}

