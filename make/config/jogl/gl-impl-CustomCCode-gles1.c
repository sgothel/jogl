/*   Java->C glue code:
 *   Java package: jogamp.opengl.es1.GLES1Impl
 *    Java method: void dispatch_glBufferData(int target, long size, java.nio.Buffer data, int usage)
 *     C function: void glBufferData(GLenum target, GLsizeiptr size, const GLvoid *  data, GLenum usage);
 */
JNIEXPORT void JNICALL
Java_jogamp_opengl_es1_GLES1Impl_dispatch_1glBufferData(JNIEnv *env, jobject _unused, jint target, jlong size, jobject data, jint data_byte_offset, jboolean data_is_nio, jint usage, jlong procAddress) {
  typedef void (GL_APIENTRY*_local_PFNGLBUFFERDATAPROC)(GLenum target, GLsizeiptr size, const GLvoid *  data, GLenum usage);
  _local_PFNGLBUFFERDATAPROC ptr_glBufferData;
  GLvoid * _data_ptr = NULL;
  if ( NULL != data ) {
    _data_ptr = (GLvoid *) ( JNI_TRUE == data_is_nio ?  (*env)->GetDirectBufferAddress(env, data) :  (*env)->GetPrimitiveArrayCritical(env, data, NULL) );  }
  ptr_glBufferData = (_local_PFNGLBUFFERDATAPROC) (intptr_t) procAddress;
  assert(ptr_glBufferData != NULL);
  (* ptr_glBufferData) ((GLenum) target, (GLsizeiptr) size, (GLvoid *) (((char *) _data_ptr) + data_byte_offset), (GLenum) usage);
  if ( JNI_FALSE == data_is_nio && NULL != data ) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _data_ptr, JNI_ABORT);  }
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.es1.GLES1Impl
 *    Java method: boolean dispatch_glUnmapBuffer(int target)
 *     C function: GLboolean glUnmapBufferOES(GLenum target);
 */
JNIEXPORT jboolean JNICALL
Java_jogamp_opengl_es1_GLES1Impl_dispatch_1glUnmapBuffer(JNIEnv *env, jobject _unused, jint target, jlong procAddress) {
  typedef GLboolean (GL_APIENTRY*_local_PFNGLUNMAPBUFFEROESPROC)(GLenum target);
  _local_PFNGLUNMAPBUFFEROESPROC ptr_glUnmapBufferOES;
  GLboolean _res;
  ptr_glUnmapBufferOES = (_local_PFNGLUNMAPBUFFEROESPROC) (intptr_t) procAddress;
  assert(ptr_glUnmapBufferOES != NULL);
  _res = (* ptr_glUnmapBufferOES) ((GLenum) target);
  return _res;
}

typedef GLvoid* (GL_APIENTRY* PFNGLMAPBUFFERPROC) (GLenum target, GLenum access);
/*   Java->C glue code:
 *   Java package: jogamp.opengl.es1.GLES1Impl
 *    Java method: long dispatch_glMapBuffer(int target, int access)
 *     C function: void * glMapBuffer(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_jogamp_opengl_es1_GLES1Impl_dispatch_1glMapBuffer(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERPROC ptr_glMapBuffer;
  void * _res;
  ptr_glMapBuffer = (PFNGLMAPBUFFERPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBuffer != NULL);
  _res = (* ptr_glMapBuffer) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.es1.GLES1Impl
 *    Java method: java.nio.ByteBuffer dispatch_glMapBufferRange(int target, long offset, long length, int access)
 *     C function: void *  glMapBufferRange(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access);
 */
JNIEXPORT jlong JNICALL
Java_jogamp_opengl_es1_GLES1Impl_dispatch_1glMapBufferRange(JNIEnv *env, jobject _unused, jint target, jlong offset, jlong length, jint access, jlong procAddress) {
  typedef void *  (GL_APIENTRY*_local_PFNGLMAPBUFFERRANGEPROC)(GLenum target, GLintptr offset, GLsizeiptr length, GLbitfield access);
  _local_PFNGLMAPBUFFERRANGEPROC ptr_glMapBufferRange;
  void *  _res;
  ptr_glMapBufferRange = (_local_PFNGLMAPBUFFERRANGEPROC) (intptr_t) procAddress;
  assert(ptr_glMapBufferRange != NULL);
  _res = (* ptr_glMapBufferRange) ((GLenum) target, (GLintptr) offset, (GLsizeiptr) length, (GLbitfield) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.es1.GLES1Impl
 *    Java method: ByteBuffer newDirectByteBuffer(long addr, long capacity);
 *     C function: jobject newDirectByteBuffer(jlong addr, jlong capacity);
 */
JNIEXPORT jobject JNICALL
Java_jogamp_opengl_es1_GLES1Impl_newDirectByteBuffer(JNIEnv *env, jobject _unused, jlong addr, jlong capacity) {
  return (*env)->NewDirectByteBuffer(env, (void*) (intptr_t) addr, capacity);
}
