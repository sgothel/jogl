/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
 

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>
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
#endif /* defined(__sun) || defined(_HPUX) */

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

#include "com_jogamp_nativewindow_impl_x11_X11Lib.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    // Workaround for ancient compiler on Solaris/SPARC
    // #define DBG_PRINT(args...) fprintf(stderr, args);
    #define DBG_PRINT0(str) fprintf(stderr, str);
    #define DBG_PRINT1(str, arg1) fprintf(stderr, str, arg1);
    #define DBG_PRINT2(str, arg1, arg2) fprintf(stderr, str, arg1, arg2);
    #define DBG_PRINT3(str, arg1, arg2, arg3) fprintf(stderr, str, arg1, arg2, arg3);
    #define DBG_PRINT4(str, arg1, arg2, arg3, arg4) fprintf(stderr, str, arg1, arg2, arg3, arg4);
    #define DBG_PRINT5(str, arg1, arg2, arg3, arg4, arg5) fprintf(stderr, str, arg1, arg2, arg3, arg4, arg5);
    #define DBG_PRINT6(str, arg1, arg2, arg3, arg4, arg5, arg6) fprintf(stderr, str, arg1, arg2, arg3, arg4, arg5, arg6);
    #define DBG_PRINT7(str, arg1, arg2, arg3, arg4, arg5, arg6, arg7) fprintf(stderr, str, arg1, arg2, arg3, arg4, arg5, arg6, arg7);
    #define DBG_PRINT8(str, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8) fprintf(stderr, str, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8);

#else

    // Workaround for ancient compiler on Solaris/SPARC
    // #define DBG_PRINT(args...)
    #define DBG_PRINT0(str)
    #define DBG_PRINT1(str, arg1)
    #define DBG_PRINT2(str, arg1, arg2)
    #define DBG_PRINT3(str, arg1, arg2, arg3)
    #define DBG_PRINT4(str, arg1, arg2, arg3, arg4)
    #define DBG_PRINT5(str, arg1, arg2, arg3, arg4, arg5)
    #define DBG_PRINT6(str, arg1, arg2, arg3, arg4, arg5, arg6)
    #define DBG_PRINT7(str, arg1, arg2, arg3, arg4, arg5, arg6, arg7)
    #define DBG_PRINT8(str, arg1, arg2, arg3, arg4, arg5, arg6, arg7, arg8)

#endif

/* Need to pull this in as we don't have a stub header for it */
extern Bool XineramaEnabled(Display* display);

static void _FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    fprintf(stderr, buffer);
    fprintf(stderr, "\n");
    (*env)->FatalError(env, buffer);
}

static const char * const ClazzNameBuffers = "com/jogamp/common/nio/Buffers";
static const char * const ClazzNameBuffersStaticCstrName = "copyByteBuffer";
static const char * const ClazzNameBuffersStaticCstrSignature = "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;";
static const char * const ClazzNameByteBuffer = "java/nio/ByteBuffer";
static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass clazzBuffers = NULL;
static jmethodID cstrBuffers = NULL;
static jclass clazzByteBuffer = NULL;
static jclass clazzRuntimeException=NULL;

static void _initClazzAccess(JNIEnv *env) {
    jclass c;

    if(NULL!=clazzRuntimeException) return ;

    c = (*env)->FindClass(env, ClazzNameRuntimeException);
    if(NULL==c) {
        _FatalError(env, "Nativewindow X11Lib: can't find %s", ClazzNameRuntimeException);
    }
    clazzRuntimeException = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==clazzRuntimeException) {
        _FatalError(env, "FatalError: NEWT X11Window: can't use %s", ClazzNameRuntimeException);
    }

    c = (*env)->FindClass(env, ClazzNameBuffers);
    if(NULL==c) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't find %s", ClazzNameBuffers);
    }
    clazzBuffers = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==clazzBuffers) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't use %s", ClazzNameBuffers);
    }
    c = (*env)->FindClass(env, ClazzNameByteBuffer);
    if(NULL==c) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't find %s", ClazzNameByteBuffer);
    }
    clazzByteBuffer = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==c) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't use %s", ClazzNameByteBuffer);
    }

    cstrBuffers = (*env)->GetStaticMethodID(env, clazzBuffers, 
                            ClazzNameBuffersStaticCstrName, ClazzNameBuffersStaticCstrSignature);
    if(NULL==cstrBuffers) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib:: can't create %s.%s %s",
            ClazzNameBuffers, ClazzNameBuffersStaticCstrName, ClazzNameBuffersStaticCstrSignature);
    }
}

static void _throwNewRuntimeException(Display * unlockDisplay, JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if(NULL!=unlockDisplay) {
        XUnlockDisplay(unlockDisplay);
    }

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    (*env)->ThrowNew(env, clazzRuntimeException, buffer);
}

static JNIEnv * x11ErrorHandlerJNIEnv = NULL;
static XErrorHandler origErrorHandler = NULL ;

static int x11ErrorHandler(Display *dpy, XErrorEvent *e)
{
    _throwNewRuntimeException(NULL, x11ErrorHandlerJNIEnv, "Nativewindow X11 Error: Display %p, Code 0x%X", dpy, e->error_code);

    return 0;
}

static void x11ErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origErrorHandler) {
            x11ErrorHandlerJNIEnv = env;
            origErrorHandler = XSetErrorHandler(x11ErrorHandler);
        }
    } else {
        XSetErrorHandler(origErrorHandler);
        origErrorHandler = NULL;
    }
}


static XIOErrorHandler origIOErrorHandler = NULL;

static int x11IOErrorHandler(Display *dpy)
{
    _FatalError(x11ErrorHandlerJNIEnv, "Nativewindow X11 IOError: Display %p not available", dpy);
    origIOErrorHandler(dpy);
    return 0;
}

static void x11IOErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origIOErrorHandler) {
            x11ErrorHandlerJNIEnv = env;
            origIOErrorHandler = XSetIOErrorHandler(x11IOErrorHandler);
        }
    } else {
        XSetIOErrorHandler(origIOErrorHandler);
        origIOErrorHandler = NULL;
    }
}

static int _initialized=0;

JNIEXPORT void JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Util_initialize(JNIEnv *env, jclass _unused, jboolean initConcurrentThreadSupport) {
    if(0==_initialized) {
        if( JNI_TRUE == initConcurrentThreadSupport ) {
            if( 0 == XInitThreads() ) {
                fprintf(stderr, "Warning: XInitThreads() failed\n");
            } else {
                fprintf(stderr, "Info: XInitThreads() called for concurrent Thread support\n");
            }
        } else {
            fprintf(stderr, "Info: XInitThreads() _not_ called for concurrent Thread support\n");
        }

        _initClazzAccess(env);
        x11ErrorHandlerEnable(1, env);
        x11IOErrorHandlerEnable(1, env);
        _initialized=1;
    }
}

/*   Java->C glue code:
 *   Java package: com.jogamp.nativewindow.impl.x11.X11Lib
 *    Java method: XVisualInfo XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, java.nio.IntBuffer arg3)
 *     C function: XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * );
 */
JNIEXPORT jobject JNICALL
Java_com_jogamp_nativewindow_impl_x11_X11Lib_XGetVisualInfo1__JJLjava_nio_ByteBuffer_2Ljava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong arg0, jlong arg1, jobject arg2, jobject arg3, jint arg3_byte_offset) {
  XVisualInfo * _ptr2 = NULL;
  int * _ptr3 = NULL;
  XVisualInfo *  _res;
  int count;
  jobject jbyteSource;
  jobject jbyteCopy;
    if(0==arg0) {
        _FatalError(env, "invalid display connection..");
    }
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

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, count * sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffers, jbyteSource);

  XFree(_res);

  return jbyteCopy;
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_DefaultVisualID(JNIEnv *env, jclass _unused, jlong display, jint screen) {
  jlong r;
    if(0==display) {
        _FatalError(env, "invalid display connection..");
    }
  r = (jlong) XVisualIDFromVisual( DefaultVisual( (Display*) (intptr_t) display, screen ) );
  return r;
}

/*   Java->C glue code:
 *   Java package: com.jogamp.nativewindow.impl.x11.X11Lib
 *    Java method: void XLockDisplay(long display)
 *     C function: void XLockDisplay(Display *  display);
 */
JNIEXPORT void JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_XLockDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  if(0==display) {
      _FatalError(env, "invalid display connection..");
  }
  XLockDisplay((Display *) (intptr_t) display);
}

/*   Java->C glue code:
 *   Java package: com.jogamp.nativewindow.impl.x11.X11Lib
 *    Java method: void XUnlockDisplay(long display)
 *     C function: void XUnlockDisplay(Display *  display);
 */
JNIEXPORT void JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_XUnlockDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  if(0==display) {
      _FatalError(env, "invalid display connection..");
  }
  XUnlockDisplay((Display *) (intptr_t) display);
}

/*   Java->C glue code:
 *   Java package: com.jogamp.nativewindow.impl.x11.X11Lib
 *    Java method: int XCloseDisplay(long display)
 *     C function: int XCloseDisplay(Display *  display);
 */
JNIEXPORT jint JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_XCloseDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  int _res;
  if(0==display) {
      _FatalError(env, "invalid display connection..");
  }
  _res = XCloseDisplay((Display *) (intptr_t) display);
  return _res;
}

/*
 * Class:     com_jogamp_nativewindow_impl_x11_X11Lib
 * Method:    CreateDummyWindow
 * Signature: (JIJ)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_nativewindow_impl_x11_X11Lib_CreateDummyWindow
  (JNIEnv *env, jobject obj, jlong display, jint screen_index, jlong visualID)
{
    Display * dpy  = (Display *)(intptr_t)display;
    int       scrn_idx = (int)screen_index;
    Window  windowParent = 0;
    Window  window = 0;

    XVisualInfo visualTemplate;
    XVisualInfo *pVisualQuery = NULL;
    Visual *visual = NULL;
    int depth;

    XSetWindowAttributes xswa;
    unsigned long attrMask;
    int n;

    Screen* scrn;

    if(NULL==dpy) {
        _FatalError(env, "invalid display connection..");
        return 0;
    }

    if(visualID<0) {
        _throwNewRuntimeException(NULL, env, "invalid VisualID ..");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, scrn_idx);

    // try given VisualID on screen
    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.screen = scrn_idx;
    visualTemplate.visualid = (VisualID)visualID;
    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask|VisualScreenMask, &visualTemplate,&n);
    if(pVisualQuery!=NULL) {
        visual   = pVisualQuery->visual;
        depth    = pVisualQuery->depth;
        visualID = (jlong)pVisualQuery->visualid;
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }
    DBG_PRINT5( "X11: [CreateWindow] trying given (dpy %p, screen %d, visualID: %d, parent %p) found: %p\n", dpy, scrn_idx, (int)visualID, windowParent, visual);

    if (visual==NULL)
    { 
        _throwNewRuntimeException(dpy, env, "could not query Visual by given VisualID, bail out!");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }

    if(0==windowParent) {
        windowParent = XRootWindowOfScreen(scrn);
    }

    attrMask  = (CWBackPixel | CWBorderPixel | CWColormap | CWOverrideRedirect) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = True; // not decorated
    xswa.border_pixel = 0;
    xswa.background_pixel = 0;
    xswa.event_mask = 0 ; // no events
    xswa.colormap = XCreateColormap(dpy,
                                    XRootWindow(dpy, scrn_idx),
                                    visual,
                                    AllocNone);

    window = XCreateWindow(dpy,
                           windowParent,
                           0, 0,
                           64, 64,
                           0, // border width
                           depth,
                           InputOutput,
                           visual,
                           attrMask,
                           &xswa);

    XSync(dpy, False);

    DBG_PRINT2( "X11: [CreateWindow] created window %p on display %p\n", window, dpy);

    return (jlong) window;
}


/*
 * Class:     com_jogamp_nativewindow_impl_x11_X11Lib
 * Method:    DestroyDummyWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_nativewindow_impl_x11_X11Lib_DestroyDummyWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    Display * dpy = (Display *)(intptr_t)display;
    Window      w = (Window) window;

    if(NULL==dpy) {
        _throwNewRuntimeException(NULL, env, "invalid display connection..");
        return;
    }

    XSync(dpy, False);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);
    XDestroyWindow(dpy, w);
    XSync(dpy, False);
}
