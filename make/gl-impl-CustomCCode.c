/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.windows.WindowsGLImpl
 *    Java method: long dispatch_glMapBuffer(int target, int access)
 *     C function: LPVOID glMapBuffer(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_net_java_games_jogl_impl_windows_WindowsGLImpl_dispatch_1glMapBuffer(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERPROC ptr_glMapBuffer;
  LPVOID _res;
  ptr_glMapBuffer = (PFNGLMAPBUFFERPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBuffer != NULL);
  _res = (* ptr_glMapBuffer) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.windows.WindowsGLImpl
 *    Java method: long dispatch_glMapBufferARB(int target, int access)
 *     C function: LPVOID glMapBufferARB(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_net_java_games_jogl_impl_windows_WindowsGLImpl_dispatch_1glMapBufferARB(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERARBPROC ptr_glMapBufferARB;
  LPVOID _res;
  ptr_glMapBufferARB = (PFNGLMAPBUFFERARBPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBufferARB != NULL);
  _res = (* ptr_glMapBufferARB) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.x11.X11GLImpl
 *    Java method: long dispatch_glMapBuffer(int target, int access)
 *     C function: LPVOID glMapBuffer(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_net_java_games_jogl_impl_x11_X11GLImpl_dispatch_1glMapBuffer(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERPROC ptr_glMapBuffer;
  LPVOID _res;
  ptr_glMapBuffer = (PFNGLMAPBUFFERPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBuffer != NULL);
  _res = (* ptr_glMapBuffer) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.x11.X11GLImpl
 *    Java method: long dispatch_glMapBufferARB(int target, int access)
 *     C function: LPVOID glMapBufferARB(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL 
Java_net_java_games_jogl_impl_x11_X11GLImpl_dispatch_1glMapBufferARB(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress) {
  PFNGLMAPBUFFERARBPROC ptr_glMapBufferARB;
  LPVOID _res;
  ptr_glMapBufferARB = (PFNGLMAPBUFFERARBPROC) (intptr_t) glProcAddress;
  assert(ptr_glMapBufferARB != NULL);
  _res = (* ptr_glMapBufferARB) ((GLenum) target, (GLenum) access);
  return (jlong) (intptr_t) _res;
}

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.macosx.MacOSXGLImpl
 *    Java method: long dispatch_glMapBufferARB(int target, int access)
 *     C function: LPVOID glMapBufferARB(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL
Java_net_java_games_jogl_impl_macosx_MacOSXGLImpl_dispatch_1glMapBuffer( JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress)
{
    PFNGLMAPBUFFERPROC ptr_glMapBuffer;
    LPVOID _res;
    ptr_glMapBuffer = (PFNGLMAPBUFFERPROC) (intptr_t) glProcAddress;
    assert(ptr_glMapBuffer != NULL);
    _res = (* ptr_glMapBuffer) ((GLenum) target, (GLenum) access);
    return (jlong) (intptr_t) _res; 
}

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.macosx.MacOSXGLImpl
 *    Java method: long dispatch_glMapBuffer(int target, int access)
 *     C function: LPVOID glMapBuffer(GLenum target, GLenum access);
 */
JNIEXPORT jlong JNICALL
Java_net_java_games_jogl_impl_macosx_MacOSXGLImpl_dispatch_1glMapBufferARB(JNIEnv *env, jobject _unused, jint target, jint access, jlong glProcAddress)
{
    PFNGLMAPBUFFERARBPROC ptr_glMapBufferARB;
    LPVOID _res;
    ptr_glMapBufferARB = (PFNGLMAPBUFFERARBPROC) (intptr_t) glProcAddress;
    assert(ptr_glMapBufferARB != NULL);
    _res = (* ptr_glMapBufferARB) ((GLenum) target, (GLenum) access);
    return (jlong) (intptr_t) _res;
}
