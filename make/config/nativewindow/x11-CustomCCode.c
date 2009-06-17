#include <inttypes.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
/* Linux headers don't work properly */
#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

/* Current versions of Solaris don't expose the XF86 extensions,
   although with the recent transition to Xorg this will probably
   happen in an upcoming release */
#if !defined(__sun) && !defined(_HPUX)
#include <X11/extensions/xf86vmode.h>
#else
/* Need to provide stubs for these */
Bool XF86VidModeGetGammaRampSize(
    Display *display,
    int screen,
    int* size)
{
  return False;
}

Bool XF86VidModeGetGammaRamp(
    Display *display,
    int screen,
    int size,
    unsigned short *red_array,
    unsigned short *green_array,
    unsigned short *blue_array) {
  return False;
}
Bool XF86VidModeSetGammaRamp(
    Display *display,
    int screen,
    int size,
    unsigned short *red_array,
    unsigned short *green_array,
    unsigned short *blue_array) {
  return False;
}
#endif

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

/* Need to expose DefaultScreen and RootWindow macros to Java */
JNIEXPORT jlong JNICALL 
Java_com_sun_nativewindow_impl_x11_X11Lib_DefaultScreen(JNIEnv *env, jclass _unused, jlong display) {
  return DefaultScreen((Display*) (intptr_t) display);
}

JNIEXPORT jlong JNICALL 
Java_com_sun_nativewindow_impl_x11_X11Lib_DefaultVisualID(JNIEnv *env, jclass _unused, jlong display, jint screen) {
  return (jlong) XVisualIDFromVisual( DefaultVisual( (Display*) (intptr_t) display, screen ) );
}

JNIEXPORT jlong JNICALL 
Java_com_sun_nativewindow_impl_x11_X11Lib_RootWindow(JNIEnv *env, jclass _unused, jlong display, jint screen) {
  return RootWindow((Display*) (intptr_t) display, screen);
}

JNIEXPORT jlong JNICALL 
Java_com_sun_nativewindow_impl_x11_X11Lib_dlopen(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlopen(chars, RTLD_LAZY | RTLD_GLOBAL);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

JNIEXPORT jlong JNICALL 
Java_com_sun_nativewindow_impl_x11_X11Lib_dlsym(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlsym(RTLD_DEFAULT, chars);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

/* Need to pull this in as we don't have a stub header for it */
extern Bool XineramaEnabled(Display* display);

static const char * clazzNameInternalBufferUtil = "com/sun/nativewindow/impl/InternalBufferUtil";
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
        fprintf(stderr, "FatalError: Java_com_sun_nativewindow_impl_x11_X11Lib: can't find %s\n", clazzNameInternalBufferUtil);
        (*env)->FatalError(env, clazzNameInternalBufferUtil);
    }
    clazzInternalBufferUtil = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==clazzInternalBufferUtil) {
        fprintf(stderr, "FatalError: Java_com_sun_nativewindow_impl_x11_X11Lib: can't use %s\n", clazzNameInternalBufferUtil);
        (*env)->FatalError(env, clazzNameInternalBufferUtil);
    }
    c = (*env)->FindClass(env, clazzNameByteBuffer);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_sun_nativewindow_impl_x11_X11Lib: can't find %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }
    clazzByteBuffer = (jclass)(*env)->NewGlobalRef(env, c);
    if(NULL==c) {
        fprintf(stderr, "FatalError: Java_com_sun_nativewindow_impl_x11_X11Lib: can't use %s\n", clazzNameByteBuffer);
        (*env)->FatalError(env, clazzNameByteBuffer);
    }

    cstrInternalBufferUtil = (*env)->GetStaticMethodID(env, clazzInternalBufferUtil, 
                            clazzNameInternalBufferUtilStaticCstrName, clazzNameInternalBufferUtilStaticCstrSignature);
    if(NULL==cstrInternalBufferUtil) {
        fprintf(stderr, "FatalError: Java_com_sun_nativewindow_impl_x11_X11Lib:: can't create %s.%s %s\n",
            clazzNameInternalBufferUtil,
            clazzNameInternalBufferUtilStaticCstrName, clazzNameInternalBufferUtilStaticCstrSignature);
        (*env)->FatalError(env, clazzNameInternalBufferUtilStaticCstrName);
    }
}

/*   Java->C glue code:
 *   Java package: com.sun.nativewindow.impl.x11.X11Lib
 *    Java method: XVisualInfo XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, java.nio.IntBuffer arg3)
 *     C function: XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * );
 */
JNIEXPORT jobject JNICALL
Java_com_sun_nativewindow_impl_x11_X11Lib_XGetVisualInfoCopied1__JJLjava_nio_ByteBuffer_2Ljava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong arg0, jlong arg1, jobject arg2, jobject arg3, jint arg3_byte_offset) {
  XVisualInfo * _ptr2 = NULL;
  int * _ptr3 = NULL;
  XVisualInfo *  _res;
  int count;
  jobject jbyteSource;
  jobject jbyteCopy;
    if (arg2 != NULL) {
        _ptr2 = (XVisualInfo *) (((char*) (*env)->GetDirectBufferAddress(env, arg2)) + 0);
    }
  if (arg3 != NULL) {
    _ptr3 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, arg3, NULL)) + arg3_byte_offset);
  }
  _res = XGetVisualInfo((Display *) (intptr_t) arg0, (long) arg1, (XVisualInfo *) _ptr2, (int *) _ptr3);
  count = _ptr3[0];
  if (arg3 != NULL) {
    (*env)->ReleasePrimitiveArrayCritical(env, arg3, _ptr3, 0);
  }
  if (_res == NULL) return NULL;

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, count * sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env,
                                               clazzInternalBufferUtil, cstrInternalBufferUtil, jbyteSource);

  // FIXME: remove reference/gc jbyteSource ?? 
  XFree(_res);

  return jbyteCopy;
}

