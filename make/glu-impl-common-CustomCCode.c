/* Include the OpenGL GLU header */
#include <GL/glu.h>

/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, byte[] data)
 *     C function: GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmapLevels__IIIIIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jint level, jint base, jint max, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPLEVELSPROC ptr_gluBuild1DMipmapLevels;
  void * _ptr8 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmapLevels = (PFNGLUBUILD1DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr8 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr8);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, short[] data)
 *     C function: GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmapLevels__IIIIIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jint level, jint base, jint max, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPLEVELSPROC ptr_gluBuild1DMipmapLevels;
  void * _ptr8 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmapLevels = (PFNGLUBUILD1DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr8 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr8);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, int[] data)
 *     C function: GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmapLevels__IIIIIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jint level, jint base, jint max, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPLEVELSPROC ptr_gluBuild1DMipmapLevels;
  void * _ptr8 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmapLevels = (PFNGLUBUILD1DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr8 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr8);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, float[] data)
 *     C function: GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmapLevels__IIIIIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jint level, jint base, jint max, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPLEVELSPROC ptr_gluBuild1DMipmapLevels;
  void * _ptr8 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmapLevels = (PFNGLUBUILD1DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr8 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr8);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmapLevels(int target, int internalFormat, int width, int format, int type, int level, int base, int max, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild1DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmapLevels__IIIIIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jint level, jint base, jint max, jobject data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPLEVELSPROC ptr_gluBuild1DMipmapLevels;
  void * _ptr8 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr8 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr8 = NULL;
    }
  ptr_gluBuild1DMipmapLevels = (PFNGLUBUILD1DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmapLevels != NULL);
  _res = (* ptr_gluBuild1DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr8);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, byte[] data)
 *     C function: GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmaps__IIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPSPROC ptr_gluBuild1DMipmaps;
  void * _ptr5 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmaps = (PFNGLUBUILD1DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmaps != NULL);
  if (data != NULL) {
    _ptr5 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (void *) _ptr5);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr5, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, short[] data)
 *     C function: GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmaps__IIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPSPROC ptr_gluBuild1DMipmaps;
  void * _ptr5 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmaps = (PFNGLUBUILD1DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmaps != NULL);
  if (data != NULL) {
    _ptr5 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (void *) _ptr5);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr5, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, int[] data)
 *     C function: GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmaps__IIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPSPROC ptr_gluBuild1DMipmaps;
  void * _ptr5 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmaps = (PFNGLUBUILD1DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmaps != NULL);
  if (data != NULL) {
    _ptr5 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (void *) _ptr5);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr5, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, float[] data)
 *     C function: GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmaps__IIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPSPROC ptr_gluBuild1DMipmaps;
  void * _ptr5 = NULL;
  GLint _res;
  ptr_gluBuild1DMipmaps = (PFNGLUBUILD1DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmaps != NULL);
  if (data != NULL) {
    _ptr5 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild1DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (void *) _ptr5);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr5, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild1DMipmaps(int target, int internalFormat, int width, int format, int type, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild1DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild1DMipmaps__IIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint format, jint type, jobject data, jlong glProcAddress) {
  PFNGLUBUILD1DMIPMAPSPROC ptr_gluBuild1DMipmaps;
  void * _ptr5 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr5 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr5 = NULL;
    }
  ptr_gluBuild1DMipmaps = (PFNGLUBUILD1DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild1DMipmaps != NULL);
  _res = (* ptr_gluBuild1DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLenum) format, (GLenum) type, (void *) _ptr5);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, byte[] data)
 *     C function: GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmapLevels__IIIIIIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jint level, jint base, jint max, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPLEVELSPROC ptr_gluBuild2DMipmapLevels;
  void * _ptr9 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmapLevels = (PFNGLUBUILD2DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr9 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr9);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr9, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, short[] data)
 *     C function: GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmapLevels__IIIIIIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jint level, jint base, jint max, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPLEVELSPROC ptr_gluBuild2DMipmapLevels;
  void * _ptr9 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmapLevels = (PFNGLUBUILD2DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr9 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr9);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr9, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, int[] data)
 *     C function: GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmapLevels__IIIIIIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jint level, jint base, jint max, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPLEVELSPROC ptr_gluBuild2DMipmapLevels;
  void * _ptr9 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmapLevels = (PFNGLUBUILD2DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr9 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr9);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr9, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, float[] data)
 *     C function: GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmapLevels__IIIIIIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jint level, jint base, jint max, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPLEVELSPROC ptr_gluBuild2DMipmapLevels;
  void * _ptr9 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmapLevels = (PFNGLUBUILD2DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr9 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr9);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr9, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmapLevels(int target, int internalFormat, int width, int height, int format, int type, int level, int base, int max, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild2DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmapLevels__IIIIIIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jint level, jint base, jint max, jobject data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPLEVELSPROC ptr_gluBuild2DMipmapLevels;
  void * _ptr9 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr9 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr9 = NULL;
    }
  ptr_gluBuild2DMipmapLevels = (PFNGLUBUILD2DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmapLevels != NULL);
  _res = (* ptr_gluBuild2DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr9);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, byte[] data)
 *     C function: GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmaps__IIIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPSPROC ptr_gluBuild2DMipmaps;
  void * _ptr6 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmaps = (PFNGLUBUILD2DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmaps != NULL);
  if (data != NULL) {
    _ptr6 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (void *) _ptr6);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr6, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, short[] data)
 *     C function: GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmaps__IIIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPSPROC ptr_gluBuild2DMipmaps;
  void * _ptr6 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmaps = (PFNGLUBUILD2DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmaps != NULL);
  if (data != NULL) {
    _ptr6 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (void *) _ptr6);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr6, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, int[] data)
 *     C function: GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmaps__IIIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPSPROC ptr_gluBuild2DMipmaps;
  void * _ptr6 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmaps = (PFNGLUBUILD2DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmaps != NULL);
  if (data != NULL) {
    _ptr6 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (void *) _ptr6);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr6, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, float[] data)
 *     C function: GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmaps__IIIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPSPROC ptr_gluBuild2DMipmaps;
  void * _ptr6 = NULL;
  GLint _res;
  ptr_gluBuild2DMipmaps = (PFNGLUBUILD2DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmaps != NULL);
  if (data != NULL) {
    _ptr6 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild2DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (void *) _ptr6);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr6, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild2DMipmaps(int target, int internalFormat, int width, int height, int format, int type, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild2DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild2DMipmaps__IIIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint format, jint type, jobject data, jlong glProcAddress) {
  PFNGLUBUILD2DMIPMAPSPROC ptr_gluBuild2DMipmaps;
  void * _ptr6 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr6 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr6 = NULL;
    }
  ptr_gluBuild2DMipmaps = (PFNGLUBUILD2DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild2DMipmaps != NULL);
  _res = (* ptr_gluBuild2DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLenum) format, (GLenum) type, (void *) _ptr6);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, byte[] data)
 *     C function: GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmapLevels__IIIIIIIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jint level, jint base, jint max, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPLEVELSPROC ptr_gluBuild3DMipmapLevels;
  void * _ptr10 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmapLevels = (PFNGLUBUILD3DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr10 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr10);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr10, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, short[] data)
 *     C function: GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmapLevels__IIIIIIIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jint level, jint base, jint max, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPLEVELSPROC ptr_gluBuild3DMipmapLevels;
  void * _ptr10 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmapLevels = (PFNGLUBUILD3DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr10 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr10);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr10, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, int[] data)
 *     C function: GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmapLevels__IIIIIIIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jint level, jint base, jint max, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPLEVELSPROC ptr_gluBuild3DMipmapLevels;
  void * _ptr10 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmapLevels = (PFNGLUBUILD3DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr10 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr10);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr10, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, float[] data)
 *     C function: GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmapLevels__IIIIIIIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jint level, jint base, jint max, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPLEVELSPROC ptr_gluBuild3DMipmapLevels;
  void * _ptr10 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmapLevels = (PFNGLUBUILD3DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmapLevels != NULL);
  if (data != NULL) {
    _ptr10 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr10);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr10, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmapLevels(int target, int internalFormat, int width, int height, int depth, int format, int type, int level, int base, int max, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild3DMipmapLevels(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, GLint level, GLint base, GLint max, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmapLevels__IIIIIIIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jint level, jint base, jint max, jobject data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPLEVELSPROC ptr_gluBuild3DMipmapLevels;
  void * _ptr10 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr10 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr10 = NULL;
    }
  ptr_gluBuild3DMipmapLevels = (PFNGLUBUILD3DMIPMAPLEVELSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmapLevels != NULL);
  _res = (* ptr_gluBuild3DMipmapLevels) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (GLint) level, (GLint) base, (GLint) max, (void *) _ptr10);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, byte[] data)
 *     C function: GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmaps__IIIIIII_3BJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jbyteArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPSPROC ptr_gluBuild3DMipmaps;
  void * _ptr7 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmaps = (PFNGLUBUILD3DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmaps != NULL);
  if (data != NULL) {
    _ptr7 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (void *) _ptr7);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr7, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, short[] data)
 *     C function: GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmaps__IIIIIII_3SJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jshortArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPSPROC ptr_gluBuild3DMipmaps;
  void * _ptr7 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmaps = (PFNGLUBUILD3DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmaps != NULL);
  if (data != NULL) {
    _ptr7 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (void *) _ptr7);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr7, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, int[] data)
 *     C function: GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmaps__IIIIIII_3IJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jintArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPSPROC ptr_gluBuild3DMipmaps;
  void * _ptr7 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmaps = (PFNGLUBUILD3DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmaps != NULL);
  if (data != NULL) {
    _ptr7 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (void *) _ptr7);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr7, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, float[] data)
 *     C function: GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmaps__IIIIIII_3FJ(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jfloatArray data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPSPROC ptr_gluBuild3DMipmaps;
  void * _ptr7 = NULL;
  GLint _res;
  ptr_gluBuild3DMipmaps = (PFNGLUBUILD3DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmaps != NULL);
  if (data != NULL) {
    _ptr7 = (void *) (*env)->GetPrimitiveArrayCritical(env, data, NULL);
  }
  _res = (* ptr_gluBuild3DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (void *) _ptr7);
  if (data != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, data, _ptr7, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluBuild3DMipmaps(int target, int internalFormat, int width, int height, int depth, int format, int type, java.nio.ByteBuffer data)
 *     C function: GLint gluBuild3DMipmaps(GLenum target, GLint internalFormat, GLsizei width, GLsizei height, GLsizei depth, GLenum format, GLenum type, const void *  data);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluBuild3DMipmaps__IIIIIIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint target, jint internalFormat, jint width, jint height, jint depth, jint format, jint type, jobject data, jlong glProcAddress) {
  PFNGLUBUILD3DMIPMAPSPROC ptr_gluBuild3DMipmaps;
  void * _ptr7 = NULL;
  GLint _res;
    if (data != NULL) {
      _ptr7 = (void *) (*env)->GetDirectBufferAddress(env, data);
    } else {
      _ptr7 = NULL;
    }
  ptr_gluBuild3DMipmaps = (PFNGLUBUILD3DMIPMAPSPROC) (intptr_t) glProcAddress;
  assert(ptr_gluBuild3DMipmaps != NULL);
  _res = (* ptr_gluBuild3DMipmaps) ((GLenum) target, (GLint) internalFormat, (GLsizei) width, (GLsizei) height, (GLsizei) depth, (GLenum) format, (GLenum) type, (void *) _ptr7);
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, byte[] dataIn, int wOut, int hOut, int typeOut, byte[] dataOut)
 *     C function: GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluScaleImage__IIII_3BIII_3BJ(JNIEnv *env, jobject _unused, jint format, jint wIn, jint hIn, jint typeIn, jbyteArray dataIn, jint wOut, jint hOut, jint typeOut, jbyteArray dataOut, jlong glProcAddress) {
  PFNGLUSCALEIMAGEPROC ptr_gluScaleImage;
  void * _ptr4 = NULL;
  GLvoid * _ptr8 = NULL;
  GLint _res;
  ptr_gluScaleImage = (PFNGLUSCALEIMAGEPROC) (intptr_t) glProcAddress;
  assert(ptr_gluScaleImage != NULL);
  if (dataIn != NULL) {
    _ptr4 = (void *) (*env)->GetPrimitiveArrayCritical(env, dataIn, NULL);
  }
  if (dataOut != NULL) {
    _ptr8 = (GLvoid *) (*env)->GetPrimitiveArrayCritical(env, dataOut, NULL);
  }
  _res = (* ptr_gluScaleImage) ((GLenum) format, (GLsizei) wIn, (GLsizei) hIn, (GLenum) typeIn, (void *) _ptr4, (GLsizei) wOut, (GLsizei) hOut, (GLenum) typeOut, (GLvoid *) _ptr8);
  if (dataIn != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataIn, _ptr4, JNI_ABORT);
  }
  if (dataOut != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataOut, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, short[] dataIn, int wOut, int hOut, int typeOut, short[] dataOut)
 *     C function: GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluScaleImage__IIII_3SIII_3SJ(JNIEnv *env, jobject _unused, jint format, jint wIn, jint hIn, jint typeIn, jshortArray dataIn, jint wOut, jint hOut, jint typeOut, jshortArray dataOut, jlong glProcAddress) {
  PFNGLUSCALEIMAGEPROC ptr_gluScaleImage;
  void * _ptr4 = NULL;
  GLvoid * _ptr8 = NULL;
  GLint _res;
  ptr_gluScaleImage = (PFNGLUSCALEIMAGEPROC) (intptr_t) glProcAddress;
  assert(ptr_gluScaleImage != NULL);
  if (dataIn != NULL) {
    _ptr4 = (void *) (*env)->GetPrimitiveArrayCritical(env, dataIn, NULL);
  }
  if (dataOut != NULL) {
    _ptr8 = (GLvoid *) (*env)->GetPrimitiveArrayCritical(env, dataOut, NULL);
  }
  _res = (* ptr_gluScaleImage) ((GLenum) format, (GLsizei) wIn, (GLsizei) hIn, (GLenum) typeIn, (void *) _ptr4, (GLsizei) wOut, (GLsizei) hOut, (GLenum) typeOut, (GLvoid *) _ptr8);
  if (dataIn != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataIn, _ptr4, JNI_ABORT);
  }
  if (dataOut != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataOut, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, int[] dataIn, int wOut, int hOut, int typeOut, int[] dataOut)
 *     C function: GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluScaleImage__IIII_3IIII_3IJ(JNIEnv *env, jobject _unused, jint format, jint wIn, jint hIn, jint typeIn, jintArray dataIn, jint wOut, jint hOut, jint typeOut, jintArray dataOut, jlong glProcAddress) {
  PFNGLUSCALEIMAGEPROC ptr_gluScaleImage;
  void * _ptr4 = NULL;
  GLvoid * _ptr8 = NULL;
  GLint _res;
  ptr_gluScaleImage = (PFNGLUSCALEIMAGEPROC) (intptr_t) glProcAddress;
  assert(ptr_gluScaleImage != NULL);
  if (dataIn != NULL) {
    _ptr4 = (void *) (*env)->GetPrimitiveArrayCritical(env, dataIn, NULL);
  }
  if (dataOut != NULL) {
    _ptr8 = (GLvoid *) (*env)->GetPrimitiveArrayCritical(env, dataOut, NULL);
  }
  _res = (* ptr_gluScaleImage) ((GLenum) format, (GLsizei) wIn, (GLsizei) hIn, (GLenum) typeIn, (void *) _ptr4, (GLsizei) wOut, (GLsizei) hOut, (GLenum) typeOut, (GLvoid *) _ptr8);
  if (dataIn != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataIn, _ptr4, JNI_ABORT);
  }
  if (dataOut != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataOut, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, float[] dataIn, int wOut, int hOut, int typeOut, float[] dataOut)
 *     C function: GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluScaleImage__IIII_3FIII_3FJ(JNIEnv *env, jobject _unused, jint format, jint wIn, jint hIn, jint typeIn, jfloatArray dataIn, jint wOut, jint hOut, jint typeOut, jfloatArray dataOut, jlong glProcAddress) {
  PFNGLUSCALEIMAGEPROC ptr_gluScaleImage;
  void * _ptr4 = NULL;
  GLvoid * _ptr8 = NULL;
  GLint _res;
  ptr_gluScaleImage = (PFNGLUSCALEIMAGEPROC) (intptr_t) glProcAddress;
  assert(ptr_gluScaleImage != NULL);
  if (dataIn != NULL) {
    _ptr4 = (void *) (*env)->GetPrimitiveArrayCritical(env, dataIn, NULL);
  }
  if (dataOut != NULL) {
    _ptr8 = (GLvoid *) (*env)->GetPrimitiveArrayCritical(env, dataOut, NULL);
  }
  _res = (* ptr_gluScaleImage) ((GLenum) format, (GLsizei) wIn, (GLsizei) hIn, (GLenum) typeIn, (void *) _ptr4, (GLsizei) wOut, (GLsizei) hOut, (GLenum) typeOut, (GLvoid *) _ptr8);
  if (dataIn != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataIn, _ptr4, JNI_ABORT);
  }
  if (dataOut != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, dataOut, _ptr8, JNI_ABORT);
  }
  return _res;
}


/*   Java->C glue code:
 *   Java package: net.java.games.jogl.impl.GLUImpl
 *    Java method: int dispatch_gluScaleImage(int format, int wIn, int hIn, int typeIn, java.nio.ByteBuffer dataIn, int wOut, int hOut, int typeOut, java.nio.ByteBuffer dataOut)
 *     C function: GLint gluScaleImage(GLenum format, GLsizei wIn, GLsizei hIn, GLenum typeIn, const void *  dataIn, GLsizei wOut, GLsizei hOut, GLenum typeOut, GLvoid *  dataOut);
 */
JNIEXPORT jint JNICALL 
Java_net_java_games_jogl_impl_GLUImpl_dispatch_1gluScaleImage__IIIILjava_nio_Buffer_2IIILjava_nio_Buffer_2J(JNIEnv *env, jobject _unused, jint format, jint wIn, jint hIn, jint typeIn, jobject dataIn, jint wOut, jint hOut, jint typeOut, jobject dataOut, jlong glProcAddress) {
  PFNGLUSCALEIMAGEPROC ptr_gluScaleImage;
  void * _ptr4 = NULL;
  GLvoid * _ptr8 = NULL;
  GLint _res;
    if (dataIn != NULL) {
      _ptr4 = (void *) (*env)->GetDirectBufferAddress(env, dataIn);
    } else {
      _ptr4 = NULL;
    }
    if (dataOut != NULL) {
      _ptr8 = (GLvoid *) (*env)->GetDirectBufferAddress(env, dataOut);
    } else {
      _ptr8 = NULL;
    }
  ptr_gluScaleImage = (PFNGLUSCALEIMAGEPROC) (intptr_t) glProcAddress;
  assert(ptr_gluScaleImage != NULL);
  _res = (* ptr_gluScaleImage) ((GLenum) format, (GLsizei) wIn, (GLsizei) hIn, (GLenum) typeIn, (void *) _ptr4, (GLsizei) wOut, (GLsizei) hOut, (GLenum) typeOut, (GLvoid *) _ptr8);
  return _res;
}
