/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.windows.WindowsGLImpl
 *    Java method: java.nio.ByteBuffer dispatch_glMapBufferARB(int target, int access)
 *     C function: LPVOID glMapBufferARB(GLenum target, GLenum access);
 */
JNIEXPORT jobject JNICALL 
Java_net_java_games_jogl_impl_windows_WindowsGLImpl_dispatch_1glMapBufferARB(JNIEnv *env, jobject _unused, jint target, jint access, jint size, jlong glProcAddress) {
  PFNGLMAPBUFFERARBPROC ptr_glMapBufferARB;
  LPVOID _res;
  ptr_glMapBufferARB = (PFNGLMAPBUFFERARBPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBufferARB != NULL);
  _res = (* ptr_glMapBufferARB) ((GLenum) target, (GLenum) access);
  if (_res == NULL) return NULL;
  return (*env)->NewDirectByteBuffer(env, _res, size);
}
