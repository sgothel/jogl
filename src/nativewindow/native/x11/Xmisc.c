/*
 * Copyright (c) 2010 Sven Gothel. All Rights Reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name Sven Gothel or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SVEN GOTHEL HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
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
    (*env)->FatalError(env, buffer);
}

static const char * const ClazzNameInternalBufferUtil = "com/jogamp/nativewindow/impl/InternalBufferUtil";
static const char * const ClazzNameInternalBufferUtilStaticCstrName = "copyByteBuffer";
static const char * const ClazzNameInternalBufferUtilStaticCstrSignature = "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;";
static const char * const ClazzNameByteBuffer = "java/nio/ByteBuffer";
static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass clazzInternalBufferUtil = NULL;
static jmethodID cstrInternalBufferUtil = NULL;
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

    c = (*env)->FindClass(env, ClazzNameInternalBufferUtil);
    if(NULL==c) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't find %s", ClazzNameInternalBufferUtil);
    }
    clazzInternalBufferUtil = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==clazzInternalBufferUtil) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib: can't use %s", ClazzNameInternalBufferUtil);
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

    cstrInternalBufferUtil = (*env)->GetStaticMethodID(env, clazzInternalBufferUtil, 
                            ClazzNameInternalBufferUtilStaticCstrName, ClazzNameInternalBufferUtilStaticCstrSignature);
    if(NULL==cstrInternalBufferUtil) {
        _FatalError(env, "FatalError: Java_com_jogamp_nativewindow_impl_x11_X11Lib:: can't create %s.%s %s",
            ClazzNameInternalBufferUtil, ClazzNameInternalBufferUtilStaticCstrName, ClazzNameInternalBufferUtilStaticCstrSignature);
    }
}

static void _throwNewRuntimeException(Display * unlockDisplay, JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    if(NULL!=unlockDisplay) {
        XUnlockDisplay(unlockDisplay);
    }

    _initClazzAccess(env);

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    (*env)->ThrowNew(env, clazzRuntimeException, buffer);
}

static XIOErrorHandler origIOErrorHandler = NULL;
static JNIEnv * displayIOErrorHandlerJNIEnv = NULL;

static int displayIOErrorHandler(Display *dpy)
{
    _FatalError(displayIOErrorHandlerJNIEnv, "Nativewindow X11 IOError: Display %p not available", dpy);
    origIOErrorHandler(dpy);
    return 0;
}

static void displayIOErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origIOErrorHandler) {
            displayIOErrorHandlerJNIEnv = env;
            origIOErrorHandler = XSetIOErrorHandler(displayIOErrorHandler);
        }
    } else {
        XSetIOErrorHandler(origIOErrorHandler);
        origIOErrorHandler = NULL;
        displayIOErrorHandlerJNIEnv = NULL;
    }
}

JNIEXPORT void JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Util_installIOErrorHandler(JNIEnv *env, jclass _unused) {
    displayIOErrorHandlerEnable(1, env);
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_dlopen(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlopen(chars, RTLD_LAZY | RTLD_GLOBAL);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

JNIEXPORT jlong JNICALL 
Java_com_jogamp_nativewindow_impl_x11_X11Lib_dlsym(JNIEnv *env, jclass _unused, jstring name) {
  const jbyte* chars;
  void* res;
  chars = (*env)->GetStringUTFChars(env, name, NULL);
  res = dlsym(RTLD_DEFAULT, chars);
  (*env)->ReleaseStringUTFChars(env, name, chars);
  return (jlong) ((intptr_t) res);
}

/*   Java->C glue code:
 *   Java package: com.jogamp.nativewindow.impl.x11.X11Lib
 *    Java method: XVisualInfo XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, java.nio.IntBuffer arg3)
 *     C function: XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * );
 */
JNIEXPORT jobject JNICALL
Java_com_jogamp_nativewindow_impl_x11_X11Lib_XGetVisualInfoCopied1__JJLjava_nio_ByteBuffer_2Ljava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong arg0, jlong arg1, jobject arg2, jobject arg3, jint arg3_byte_offset) {
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

  _initClazzAccess(env);

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, count * sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env,
                                               clazzInternalBufferUtil, cstrInternalBufferUtil, jbyteSource);

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
        _throwNewRuntimeException(NULL, env, "invalid VisualID ..\n");
        return 0;
    }

    XLockDisplay(dpy) ;

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
        _throwNewRuntimeException(dpy, env, "could not query Visual by given VisualID, bail out!\n");
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

    XUnlockDisplay(dpy) ;

    DBG_PRINT2( "X11: [CreateWindow] created window %p on display %p\n", window, dpy);

    return (jlong) window;
}
 */


/*
 * Class:     com_jogamp_nativewindow_impl_x11_X11Lib
 * Method:    DestroyDummyWindow
 * Signature: (JJ)V
JNIEXPORT void JNICALL Java_com_jogamp_nativewindow_impl_x11_X11Lib_DestroyDummyWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    Display * dpy = (Display *)(intptr_t)display;
    Window      w = (Window) window;

    if(NULL==dpy) {
        _throwNewRuntimeException(NULL, env, "invalid display connection..\n");
        return;
    }
    XLockDisplay(dpy) ;

    XSync(dpy, False);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);
    XDestroyWindow(dpy, w);
    XSync(dpy, False);

    XUnlockDisplay(dpy) ;
}
 */

