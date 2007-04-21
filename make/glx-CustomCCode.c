#include <inttypes.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <GL/glx.h>
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
Java_com_sun_opengl_impl_x11_GLX_DefaultScreen(JNIEnv *env, jclass _unused, jlong display) {
  return DefaultScreen((Display*) (intptr_t) display);
}
JNIEXPORT jlong JNICALL 
Java_com_sun_opengl_impl_x11_GLX_RootWindow(JNIEnv *env, jclass _unused, jlong display, jint screen) {
  return RootWindow((Display*) (intptr_t) display, screen);
}

JNIEXPORT jlong JNICALL 
Java_com_sun_opengl_impl_x11_GLX_dlopen(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlopen(chars, RTLD_LAZY | RTLD_GLOBAL);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

JNIEXPORT jlong JNICALL 
Java_com_sun_opengl_impl_x11_GLX_dlsym(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlsym(RTLD_DEFAULT, chars);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

/* We expect glXGetProcAddressARB to be defined */
extern __GLXextFuncPtr glXGetProcAddressARB (const GLubyte *);

/* Need to pull this in as we don't have a stub header for it */
extern Bool XineramaEnabled(Display* display);
