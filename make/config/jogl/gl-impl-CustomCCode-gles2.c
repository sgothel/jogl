typedef GLvoid* (GL_APIENTRY* PFNGLMAPBUFFERPROC) (GLenum target, GLenum access);
/*   Java->C glue code:
 *   Java package: jogamp.opengl.es2.GLES2Impl
 *    Java method: long dispatch_glMapBuffer(int target, int access)
 *     C function: void * glMapBuffer(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_jogamp_opengl_es2_GLES2Impl_dispatch_1glMapBuffer(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERPROC ptr_glMapBuffer;
  void * _res;
  ptr_glMapBuffer = (PFNGLMAPBUFFERPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBuffer != NULL);
  _res = (* ptr_glMapBuffer) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.es2.GLES2Impl
 *    Java method: ByteBuffer newDirectByteBuffer(long addr, long capacity);
 *     C function: jobject newDirectByteBuffer(jlong addr, jlong capacity);
 */
JNIEXPORT jobject JNICALL
Java_jogamp_opengl_es2_GLES2Impl_newDirectByteBuffer(JNIEnv *env, jobject _unused, jlong addr, jlong capacity) {
  return (*env)->NewDirectByteBuffer(env, (void*) (intptr_t) addr, capacity);
}
