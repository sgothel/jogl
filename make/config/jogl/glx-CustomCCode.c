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

static const char * clazzNameBuffers = "com/jogamp/common/nio/Buffers";
static const char * clazzNameBuffersStaticCstrName = "copyByteBuffer";
static const char * clazzNameBuffersStaticCstrSignature = "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;";
static const char * clazzNameByteBuffer = "java/nio/ByteBuffer";
static jclass clazzBuffers = NULL;
static jmethodID cstrBuffers = NULL;
static jclass clazzByteBuffer = NULL;

static void _initClazzAccess(JNIEnv *env) {
    jclass c;

    if(NULL!=cstrBuffers) return ;

    c = (*env)->FindClass(env, clazzNameBuffers);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_jogamp_opengl_impl_x11_glx_GLX: can't find %s\n", clazzNameBuffers);
        (*env)->FatalError(env, clazzNameBuffers);
    }
    clazzBuffers = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==clazzBuffers) {
        fprintf(stderr, "FatalError: Java_com_jogamp_opengl_impl_x11_glx_GLX: can't use %s\n", clazzNameBuffers);
        (*env)->FatalError(env, clazzNameBuffers);
    }
    c = (*env)->FindClass(env, clazzNameByteBuffer);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_jogamp_opengl_impl_x11_glx_GLX: can't find %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }
    clazzByteBuffer = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_jogamp_opengl_impl_x11_glx_GLX: can't use %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }

    cstrBuffers = (*env)->GetStaticMethodID(env, clazzBuffers, 
                            clazzNameBuffersStaticCstrName, clazzNameBuffersStaticCstrSignature);
    if(NULL==cstrBuffers) {
        fprintf(stderr, "FatalError: Java_com_jogamp_opengl_impl_x11_glx_GLX:: can't create %s.%s %s\n",
            clazzNameBuffers,
            clazzNameBuffersStaticCstrName, clazzNameBuffersStaticCstrSignature);
        (*env)->FatalError(env, clazzNameBuffersStaticCstrName);
    }
}

/*   Java->C glue code:
 *   Java package: com.jogamp.opengl.impl.x11.glx.GLX
 *    Java method: XVisualInfo glXGetVisualFromFBConfig(long dpy, long config)
 *     C function: XVisualInfo *  glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config);
 */
JNIEXPORT jobject JNICALL 
Java_com_jogamp_opengl_impl_x11_glx_GLX_dispatch_1glXGetVisualFromFBConfig(JNIEnv *env, jclass _unused, jlong dpy, jlong config, jlong procAddress) {
  typedef XVisualInfo* (APIENTRY*_local_PFNGLXGETVISUALFROMFBCONFIG)(Display *  dpy, GLXFBConfig config);
  _local_PFNGLXGETVISUALFROMFBCONFIG ptr_glXGetVisualFromFBConfig;
  XVisualInfo *  _res;
  jobject jbyteSource;
  jobject jbyteCopy;
  ptr_glXGetVisualFromFBConfig = (_local_PFNGLXGETVISUALFROMFBCONFIG) (intptr_t) procAddress;
  assert(ptr_glXGetVisualFromFBConfig != NULL);
  _res = (* ptr_glXGetVisualFromFBConfig) ((Display *) (intptr_t) dpy, (GLXFBConfig) (intptr_t) config);
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffers, jbyteSource);

  (*env)->DeleteLocalRef(env, jbyteSource);
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: com.jogamp.opengl.impl.x11.glx.GLX
 *    Java method: java.nio.LongBuffer glXChooseFBConfig(long dpy, int screen, java.nio.IntBuffer attribList, java.nio.IntBuffer nitems)
 *     C function: GLXFBConfig *  glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems);
 */
JNIEXPORT jobject JNICALL 
Java_com_jogamp_opengl_impl_x11_glx_GLX_dispatch_1glXChooseFBConfig(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset, jobject nitems, jint nitems_byte_offset, jlong procAddress) {
  typedef GLXFBConfig* (APIENTRY*_local_PFNGLXCHOOSEFBCONFIG)(Display *  dpy, int screen, const int *  attribList, int *  nitems);
  _local_PFNGLXCHOOSEFBCONFIG ptr_glXChooseFBConfig;
  int * _ptr2 = NULL;
  int * _ptr3 = NULL;
  GLXFBConfig *  _res;
  int count;
  jobject jbyteSource;
  jobject jbyteCopy;
  ptr_glXChooseFBConfig = (_local_PFNGLXCHOOSEFBCONFIG) (intptr_t) procAddress;
  assert(ptr_glXChooseFBConfig != NULL);
  if (attribList != NULL) {
    _ptr2 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, attribList, NULL)) + attribList_byte_offset);
  }
  if (nitems != NULL) {
    _ptr3 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, nitems, NULL)) + nitems_byte_offset);
  }
  _res = (*ptr_glXChooseFBConfig)((Display *) (intptr_t) dpy, (int) screen, (int *) _ptr2, (int *) _ptr3);
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
  jbyteCopy   = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffers, jbyteSource);
  (*env)->DeleteLocalRef(env, jbyteSource);
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: com.jogamp.opengl.impl.x11.glx.GLX
 *    Java method: XVisualInfo glXChooseVisual(long dpy, int screen, java.nio.IntBuffer attribList)
 *     C function: XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList);
 */
JNIEXPORT jobject JNICALL 
Java_com_jogamp_opengl_impl_x11_glx_GLX_dispatch_1glXChooseVisual(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset, jlong procAddress) {
  typedef XVisualInfo* (APIENTRY*_local_PFNGLXCHOOSEVISUAL)(Display *  dpy, int screen, int *  attribList);
  _local_PFNGLXCHOOSEVISUAL ptr_glXChooseVisual;
  int * _ptr2 = NULL;
  XVisualInfo *  _res;
  jobject jbyteSource;
  jobject jbyteCopy;
  ptr_glXChooseVisual = (_local_PFNGLXCHOOSEVISUAL) (intptr_t) procAddress;
  assert(ptr_glXChooseVisual != NULL);
  if (attribList != NULL) {
    _ptr2 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, attribList, NULL)) + attribList_byte_offset);
  }
  _res = (*ptr_glXChooseVisual)((Display *) (intptr_t) dpy, (int) screen, (int *) _ptr2);
  if (attribList != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, attribList, _ptr2, 0);
  }
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffers, jbyteSource);

  (*env)->DeleteLocalRef(env, jbyteSource);
  XFree(_res);

  return jbyteCopy;
}

