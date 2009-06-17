#include <inttypes.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
/* Linux headers don't work properly */
#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

/* We expect glXGetProcAddressARB to be defined */
extern void (*glXGetProcAddressARB(const GLubyte *procname))();

static const char * clazzNameInternalBufferUtil = "com/sun/opengl/impl/InternalBufferUtil";
static const char * clazzNameInternalBufferUtilStaticCstrName = "copyByteBuffer";
static const char * clazzNameInternalBufferUtilStaticCstrSignature = "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;";
static const char * clazzNameByteBuffer = "java/nio/ByteBuffer";
static jclass clazzInternalBufferUtil = NULL;
static jmethodID cstrInternalBufferUtil = NULL;
static jclass clazzByteBuffer = NULL;

static void _initClazzAccess(JNIEnv *env) {
    jclass c;

    if(NULL!=cstrInternalBufferUtil) return ;

    c = (*env)->FindClass(env, clazzNameInternalBufferUtil);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_sun_opengl_impl_x11_glx_GLX: can't find %s\n", clazzNameInternalBufferUtil);
        (*env)->FatalError(env, clazzNameInternalBufferUtil);
    }
    clazzInternalBufferUtil = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==clazzInternalBufferUtil) {
        fprintf(stderr, "FatalError: Java_com_sun_opengl_impl_x11_glx_GLX: can't use %s\n", clazzNameInternalBufferUtil);
        (*env)->FatalError(env, clazzNameInternalBufferUtil);
    }
    c = (*env)->FindClass(env, clazzNameByteBuffer);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_sun_opengl_impl_x11_glx_GLX: can't find %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }
    clazzByteBuffer = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_sun_opengl_impl_x11_glx_GLX: can't use %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }

    cstrInternalBufferUtil = (*env)->GetStaticMethodID(env, clazzInternalBufferUtil, 
                            clazzNameInternalBufferUtilStaticCstrName, clazzNameInternalBufferUtilStaticCstrSignature);
    if(NULL==cstrInternalBufferUtil) {
        fprintf(stderr, "FatalError: Java_com_sun_opengl_impl_x11_glx_GLX:: can't create %s.%s %s\n",
            clazzNameInternalBufferUtil,
            clazzNameInternalBufferUtilStaticCstrName, clazzNameInternalBufferUtilStaticCstrSignature);
        (*env)->FatalError(env, clazzNameInternalBufferUtilStaticCstrName);
    }
}

/*   Java->C glue code:
 *   Java package: com.sun.opengl.impl.x11.glx.GLX
 *    Java method: XVisualInfo glXGetVisualFromFBConfig(long dpy, long config)
 *     C function: XVisualInfo *  glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config);
 */
JNIEXPORT jobject JNICALL 
Java_com_sun_opengl_impl_x11_glx_GLX_glXGetVisualFromFBConfigCopied0__JJ(JNIEnv *env, jclass _unused, jlong dpy, jlong config) {
  XVisualInfo *  _res;
  jobject jbyteSource;
  jobject jbyteCopy;
  _res = glXGetVisualFromFBConfig((Display *) (intptr_t) dpy, (GLXFBConfig) (intptr_t) config);
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env,
                                               clazzInternalBufferUtil, cstrInternalBufferUtil, jbyteSource);

  // FIXME: remove reference/gc jbyteSource ?? 
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: com.sun.opengl.impl.x11.glx.GLX
 *    Java method: java.nio.LongBuffer glXChooseFBConfig(long dpy, int screen, java.nio.IntBuffer attribList, java.nio.IntBuffer nitems)
 *     C function: GLXFBConfig *  glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems);
 */
JNIEXPORT jobject JNICALL 
Java_com_sun_opengl_impl_x11_glx_GLX_glXChooseFBConfigCopied1__JILjava_lang_Object_2ILjava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset, jobject nitems, jint nitems_byte_offset) {
  int * _ptr2 = NULL;
  int * _ptr3 = NULL;
  GLXFBConfig *  _res;
  int count;
  jobject jbyteSource;
  jobject jbyteCopy;
  if (attribList != NULL) {
    _ptr2 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, attribList, NULL)) + attribList_byte_offset);
  }
  if (nitems != NULL) {
    _ptr3 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, nitems, NULL)) + nitems_byte_offset);
  }
  _res = glXChooseFBConfig((Display *) (intptr_t) dpy, (int) screen, (int *) _ptr2, (int *) _ptr3);
  count = _ptr3[0];
  if (attribList != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, attribList, _ptr2, 0);
  }
  if (nitems != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, nitems, _ptr3, 0);
  }
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, count * sizeof(GLXFBConfig));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env,
                                               clazzInternalBufferUtil, cstrInternalBufferUtil, jbyteSource);

  // FIXME: remove reference/gc jbyteSource ?? 
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: com.sun.opengl.impl.x11.glx.GLX
 *    Java method: XVisualInfo glXChooseVisual(long dpy, int screen, java.nio.IntBuffer attribList)
 *     C function: XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList);
 */
JNIEXPORT jobject JNICALL 
Java_com_sun_opengl_impl_x11_glx_GLX_glXChooseVisualCopied1__JILjava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset) {
  int * _ptr2 = NULL;
  XVisualInfo *  _res;
  jobject jbyteSource;
  jobject jbyteCopy;
  if (attribList != NULL) {
    _ptr2 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, attribList, NULL)) + attribList_byte_offset);
  }
  _res = glXChooseVisual((Display *) (intptr_t) dpy, (int) screen, (int *) _ptr2);
  if (attribList != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, attribList, _ptr2, 0);
  }
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env,
                                               clazzInternalBufferUtil, cstrInternalBufferUtil, jbyteSource);

  // FIXME: remove reference/gc jbyteSource ?? 
  XFree(_res);

  return jbyteCopy;
}

