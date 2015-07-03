/* Linux headers don't work properly */
#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

#include <string.h>

/* We expect glXGetProcAddressARB to be defined */
extern void (*glXGetProcAddressARB(const GLubyte *procname))();

/**
 * Java->C glue code:
 *   Java package: jogamp.opengl.x11.glx.GLX
 *    Java method: int glXGetFBConfigAttributes(long dpy, long config, IntBuffer attributes, IntBuffer values)
 */
JNIEXPORT jint JNICALL 
Java_jogamp_opengl_x11_glx_GLX_dispatch_1glXGetFBConfigAttributes(JNIEnv *env, jclass _unused, jlong dpy, jlong config, jint attributeCount, jobject attributes, jint attributes_byte_offset, jobject values, jint values_byte_offset, jlong procAddress) {
  typedef int (APIENTRY*_local_PFNGLXGETFBCONFIGATTRIBPROC)(Display *  dpy, GLXFBConfig config, int attribute, int *  value);
  _local_PFNGLXGETFBCONFIGATTRIBPROC ptr_glXGetFBConfigAttrib = (_local_PFNGLXGETFBCONFIGATTRIBPROC) (intptr_t) procAddress;
  assert(ptr_glXGetFBConfigAttrib != NULL);

  int err = 0;
  if ( attributeCount > 0 && NULL != attributes ) {
    int i;
    int * attributes_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, attributes)) + attributes_byte_offset);
    int * values_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, values)) + values_byte_offset);
    for(i=0; 0 == err && i<attributeCount; i++) {
        err = (* ptr_glXGetFBConfigAttrib) ((Display *) (intptr_t) dpy, (GLXFBConfig) (intptr_t) config, attributes_ptr[i], &values_ptr[i]);
    }
    if( 0 != err ) {
        values_ptr[0] = i;
    }
  }
  return (jint)err;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.x11.glx.GLX
 *    Java method: XVisualInfo glXGetVisualFromFBConfig(long dpy, long config)
 *     C function: XVisualInfo *  glXGetVisualFromFBConfig(Display *  dpy, GLXFBConfig config);
 */
JNIEXPORT jobject JNICALL 
Java_jogamp_opengl_x11_glx_GLX_dispatch_1glXGetVisualFromFBConfig(JNIEnv *env, jclass _unused, jlong dpy, jlong config, jlong procAddress) {
  typedef XVisualInfo* (APIENTRY*_local_PFNGLXGETVISUALFROMFBCONFIG)(Display *  dpy, GLXFBConfig config);
  _local_PFNGLXGETVISUALFROMFBCONFIG ptr_glXGetVisualFromFBConfig;
  XVisualInfo *  _res;
  jobject jbyteCopy;
  ptr_glXGetVisualFromFBConfig = (_local_PFNGLXGETVISUALFROMFBCONFIG) (intptr_t) procAddress;
  assert(ptr_glXGetVisualFromFBConfig != NULL);
  _res = (* ptr_glXGetVisualFromFBConfig) ((Display *) (intptr_t) dpy, (GLXFBConfig) (intptr_t) config);
  if (_res == NULL) return NULL;

  jbyteCopy   = JVMUtil_NewDirectByteBufferCopy(env, _res, sizeof(XVisualInfo));
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.x11.glx.GLX
 *    Java method: com.jogamp.common.nio.PointerBuffer dispatch_glXChooseFBConfig(long dpy, int screen, java.nio.IntBuffer attribList, java.nio.IntBuffer nitems)
 *     C function: GLXFBConfig *  glXChooseFBConfig(Display *  dpy, int screen, const int *  attribList, int *  nitems);
 */
JNIEXPORT jobject JNICALL 
Java_jogamp_opengl_x11_glx_GLX_dispatch_1glXChooseFBConfig(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset, jobject nitems, jint nitems_byte_offset, jlong procAddress) {
  typedef GLXFBConfig *  (APIENTRY*_local_PFNGLXCHOOSEFBCONFIGPROC)(Display *  dpy, int screen, const int *  attribList, int *  nitems);
  _local_PFNGLXCHOOSEFBCONFIGPROC ptr_glXChooseFBConfig;
  int * _attribList_ptr = NULL;
  int * _nitems_ptr = NULL;
  GLXFBConfig *  _res;
  int count, i;
  jobject jbyteCopy;
    if ( NULL != attribList ) {
        _attribList_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, attribList)) + attribList_byte_offset);
    }
    if ( NULL != nitems ) {
        _nitems_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, nitems)) + nitems_byte_offset);
    }
  ptr_glXChooseFBConfig = (_local_PFNGLXCHOOSEFBCONFIGPROC) (intptr_t) procAddress;
  assert(ptr_glXChooseFBConfig != NULL);
  _res = (* ptr_glXChooseFBConfig) ((Display *) (intptr_t) dpy, (int) screen, (int *) _attribList_ptr, (int *) _nitems_ptr);
  count = _nitems_ptr[0];
  if (NULL == _res) return NULL;

  /**
   * Bug 961: Validate returned 'GLXFBConfig *', i.e. remove NULL pointer. 
   * Note: sizeof(GLXFBConfig) == sizeof(void*), a.k.a a pointer
   */
  // fprintf(stderr, "glXChooseFBConfig.0: Count %d\n", count);
  i=0;
  while( i < count ) {
    if( NULL == _res[i] ) {
       if( 0 < count-i-1 ) {
         memmove(_res+i, _res+i+1, (count-i-1)*sizeof(GLXFBConfig)); 
       }
       count--;
    } else {
       i++;
    }
  }
  // fprintf(stderr, "glXChooseFBConfig.X: Count %d\n", count);

  jbyteCopy   = JVMUtil_NewDirectByteBufferCopy(env, _res, count * sizeof(GLXFBConfig));
  XFree(_res);

  return jbyteCopy;
}

/*   Java->C glue code:
 *   Java package: jogamp.opengl.x11.glx.GLX
 *    Java method: com.jogamp.common.nio.PointerBuffer dispatch_glXGetFBConfigs(long dpy, int screen, java.nio.IntBuffer nelements)
 *     C function: GLXFBConfig *  glXGetFBConfigs(Display *  dpy, int screen, int *  nelements);
 */
JNIEXPORT jobject JNICALL 
Java_jogamp_opengl_x11_glx_GLX_dispatch_1glXGetFBConfigs(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject nelements, jint nelements_byte_offset, jlong procAddress) {
  typedef GLXFBConfig *  (APIENTRY*_local_PFNGLXGETFBCONFIGSPROC)(Display *  dpy, int screen, int *  nelements);
  _local_PFNGLXGETFBCONFIGSPROC ptr_glXGetFBConfigs;
  int * _nelements_ptr = NULL;
  GLXFBConfig *  _res;
  int count, i;
  jobject jbyteCopy;
    if ( NULL != nelements ) {
        _nelements_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, nelements)) + nelements_byte_offset);
    }
  ptr_glXGetFBConfigs = (_local_PFNGLXGETFBCONFIGSPROC) (intptr_t) procAddress;
  assert(ptr_glXGetFBConfigs != NULL);
  _res = (* ptr_glXGetFBConfigs) ((Display *) (intptr_t) dpy, (int) screen, (int *) _nelements_ptr);
  count = _nelements_ptr[0];
  if (NULL == _res) return NULL;

  /**
   * Bug 961: Validate returned 'GLXFBConfig *', i.e. remove NULL pointer. 
   * Note: sizeof(GLXFBConfig) == sizeof(void*), a.k.a a pointer
   */
  i=0;
  while( i < count ) {
    if( NULL == _res[i] ) {
       if( 0 < count-i-1 ) {
         memmove(_res+i, _res+i+1, (count-i-1)*sizeof(GLXFBConfig));
       }
       count--;
    } else {
       i++;
    }
  }

  jbyteCopy   = JVMUtil_NewDirectByteBufferCopy(env, _res, count * sizeof(GLXFBConfig));
  XFree(_res);

  return jbyteCopy;
}


/*   Java->C glue code:
 *   Java package: jogamp.opengl.x11.glx.GLX
 *    Java method: XVisualInfo dispatch_glXChooseVisual(long dpy, int screen, java.nio.IntBuffer attribList)
 *     C function: XVisualInfo *  glXChooseVisual(Display *  dpy, int screen, int *  attribList);
 */
JNIEXPORT jobject JNICALL 
Java_jogamp_opengl_x11_glx_GLX_dispatch_1glXChooseVisual(JNIEnv *env, jclass _unused, jlong dpy, jint screen, jobject attribList, jint attribList_byte_offset, jlong procAddress) {
  typedef XVisualInfo *  (APIENTRY*_local_PFNGLXCHOOSEVISUALPROC)(Display *  dpy, int screen, int *  attribList);
  _local_PFNGLXCHOOSEVISUALPROC ptr_glXChooseVisual;
  int * _attribList_ptr = NULL;
  XVisualInfo *  _res;
  jobject jbyteCopy;
    if ( NULL != attribList ) {
        _attribList_ptr = (int *) (((char*) (*env)->GetDirectBufferAddress(env, attribList)) + attribList_byte_offset);
    }
  ptr_glXChooseVisual = (_local_PFNGLXCHOOSEVISUALPROC) (intptr_t) procAddress;
  assert(ptr_glXChooseVisual != NULL);
  _res = (* ptr_glXChooseVisual) ((Display *) (intptr_t) dpy, (int) screen, (int *) _attribList_ptr);
  if (NULL == _res) return NULL;

  jbyteCopy   = JVMUtil_NewDirectByteBufferCopy(env, _res, sizeof(XVisualInfo));
  XFree(_res);

  return jbyteCopy;
}

