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
 
#include "NativewindowCommon.h"
#include "Xmisc.h"
#include "jogamp_nativewindow_x11_X11Lib.h"
#include "jogamp_nativewindow_x11_X11Util.h"

#include <X11/Xatom.h>

#include <X11/extensions/Xrender.h>

/** Remove memcpy GLIBC > 2.4 dependencies */
#include <glibc-compat-symbols.h>

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args);
#else
    #define DBG_PRINT(args...)
#endif


/* Linux headers don't work properly */
#define __USE_GNU
#include <dlfcn.h>
#undef __USE_GNU

#include "Xmisc.h"

/* Current versions of Solaris don't expose the XF86 extensions,
   although with the recent transition to Xorg this will probably
   happen in an upcoming release */
#if defined(__sun_obsolete) || defined(_HPUX)
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
#endif /* defined(__sun_obsolete) || defined(_HPUX) */

/* HP-UX doesn't define RTLD_DEFAULT. */
#if defined(_HPUX) && !defined(RTLD_DEFAULT)
#define RTLD_DEFAULT NULL
#endif

#define X11_MOUSE_EVENT_MASK (ButtonPressMask | ButtonReleaseMask | PointerMotionMask | EnterWindowMask | LeaveWindowMask)

static const char * const ClazzNameBuffers = "com/jogamp/common/nio/Buffers";
static const char * const ClazzNameBuffersStaticCstrName = "copyByteBuffer";
static const char * const ClazzNameBuffersStaticCstrSignature = "(Ljava/nio/ByteBuffer;)Ljava/nio/ByteBuffer;";
static const char * const ClazzNameByteBuffer = "java/nio/ByteBuffer";
static const char * const ClazzNamePoint = "com/jogamp/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";
static jclass X11UtilClazz = NULL;
static jmethodID getCurrentThreadNameID = NULL;
static jmethodID dumpStackID = NULL;
static jclass clazzBuffers = NULL;
static jmethodID cstrBuffers = NULL;
static jclass clazzByteBuffer = NULL;
static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static void _initClazzAccess(JNIEnv *env) {
    jclass c;

    if( NativewindowCommon_init(env) ) {
        getCurrentThreadNameID = (*env)->GetStaticMethodID(env, X11UtilClazz, "getCurrentThreadName", "()Ljava/lang/String;");
        if(NULL==getCurrentThreadNameID) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't get method getCurrentThreadName");
        }
        dumpStackID = (*env)->GetStaticMethodID(env, X11UtilClazz, "dumpStack", "()V");
        if(NULL==dumpStackID) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't get method dumpStack");
        }

        c = (*env)->FindClass(env, ClazzNameBuffers);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't find %s", ClazzNameBuffers);
        }
        clazzBuffers = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==clazzBuffers) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't use %s", ClazzNameBuffers);
        }
        c = (*env)->FindClass(env, ClazzNameByteBuffer);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't find %s", ClazzNameByteBuffer);
        }
        clazzByteBuffer = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't use %s", ClazzNameByteBuffer);
        }

        cstrBuffers = (*env)->GetStaticMethodID(env, clazzBuffers, 
                                ClazzNameBuffersStaticCstrName, ClazzNameBuffersStaticCstrSignature);
        if(NULL==cstrBuffers) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't create %s.%s %s",
                ClazzNameBuffers, ClazzNameBuffersStaticCstrName, ClazzNameBuffersStaticCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_x11_X11Lib: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }
    }
}

static XErrorHandler origErrorHandler = NULL ;
static int errorHandlerQuiet = 1 ;
static int errorHandlerDebug = 0 ;
static int errorHandlerThrowException = 0;

static int x11ErrorHandler(Display *dpy, XErrorEvent *e)
{
    if( !errorHandlerQuiet || errorHandlerDebug ) {
        const char * errnoStr = strerror(errno);
        char errCodeStr[80];
        char reqCodeStr[80];
        int shallBeDetached = 0;
        JNIEnv *jniEnv = NULL;

        snprintf(errCodeStr, sizeof(errCodeStr), "%d", e->request_code);
        XGetErrorDatabaseText(dpy, "XRequest", errCodeStr, "Unknown", reqCodeStr, sizeof(reqCodeStr));
        XGetErrorText(dpy, e->error_code, errCodeStr, sizeof(errCodeStr));

        fprintf(stderr, "Info: Nativewindow X11 Error: %d - %s, dpy %p, id %x, # %d: %d:%d %s\n",
            e->error_code, errCodeStr, e->display, (int)e->resourceid, (int)e->serial,
            (int)e->request_code, (int)e->minor_code, reqCodeStr);
        fflush(stderr);

        if( errorHandlerDebug || errorHandlerThrowException ) {
            jniEnv = NativewindowCommon_GetJNIEnv(0 /* asDaemon */, &shallBeDetached);
            if(NULL == jniEnv) {
                fprintf(stderr, "Nativewindow X11 Error: null JNIEnv");
                fflush(stderr);
            }
        }

        if( NULL != jniEnv ) {
            if( errorHandlerDebug ) {
                (*jniEnv)->CallStaticVoidMethod(jniEnv, X11UtilClazz, dumpStackID);
            }

            if(errorHandlerThrowException) {
                NativewindowCommon_throwNewRuntimeException(jniEnv, "Nativewindow X11 Error: %d - %s, dpy %p, id %x, # %d: %d:%d %s\n",
                                                            e->error_code, errCodeStr, e->display, (int)e->resourceid, (int)e->serial,
                                                            (int)e->request_code, (int)e->minor_code, reqCodeStr);
            }
            NativewindowCommon_ReleaseJNIEnv(shallBeDetached);
        }
    }

    return 0;
}

static void NativewindowCommon_x11ErrorHandlerEnable(JNIEnv * env, Display *dpy, int force, int onoff, int quiet, int sync) {
    errorHandlerQuiet = quiet;
    if(onoff) {
        if(force || NULL==origErrorHandler) {
            XErrorHandler prevErrorHandler;
            prevErrorHandler = XSetErrorHandler(x11ErrorHandler);
            if(x11ErrorHandler != prevErrorHandler) { // if forced don't overwrite w/ orig w/ our handler
                origErrorHandler = prevErrorHandler;
            }
            if(sync && NULL!=dpy) {
                XSync(dpy, False);
            }
        }
    } else {
        if(NULL!=origErrorHandler) {
            if(sync && NULL!=dpy) {
                XSync(dpy, False);
            }
            XSetErrorHandler(origErrorHandler);
            origErrorHandler = NULL;
        }
    }
}

static XIOErrorHandler origIOErrorHandler = NULL;

static int x11IOErrorHandler(Display *dpy)
{
    const char * dpyName = XDisplayName(NULL);
    const char * errnoStr = strerror(errno);
    int shallBeDetached = 0;
    JNIEnv *jniEnv = NULL;

    fprintf(stderr, "Nativewindow X11 IOError: Display %p (%s): %s\n", dpy, dpyName, errnoStr);
    fflush(stderr);

    jniEnv = NativewindowCommon_GetJNIEnv(0 /* asDaemon */, &shallBeDetached);
    if (NULL != jniEnv) {
        NativewindowCommon_FatalError(jniEnv, "Nativewindow X11 IOError: Display %p (%s): %s", dpy, dpyName, errnoStr);
        NativewindowCommon_ReleaseJNIEnv(shallBeDetached);
    }
    if(NULL!=origIOErrorHandler) {
        origIOErrorHandler(dpy);
    }
    return 0;
}

static void x11IOErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origIOErrorHandler) {
            origIOErrorHandler = XSetIOErrorHandler(x11IOErrorHandler);
        }
    } else {
        XSetIOErrorHandler(origIOErrorHandler);
        origIOErrorHandler = NULL;
    }
}

static int _initialized = 0;

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_x11_X11Util_initialize0(JNIEnv *env, jclass clazz, jboolean debug) {
    if( 0 == _initialized ) {
        if(debug) {
            errorHandlerDebug = 1;
        }
        X11UtilClazz = (jclass)(*env)->NewGlobalRef(env, clazz);

        _initClazzAccess(env);
        x11IOErrorHandlerEnable(1, env);
        NativewindowCommon_x11ErrorHandlerEnable(env, NULL, 1, 1, debug ? 0 : 1, 0 /* no dpy, force, no sync */);
        _initialized=1;
        if(JNI_TRUE == debug) {
            fprintf(stderr, "Info: NativeWindow native init passed\n");
        }
    }
    return JNI_TRUE;
}

JNIEXPORT void JNICALL 
Java_jogamp_nativewindow_x11_X11Util_shutdown0(JNIEnv *env, jclass _unused) {
    NativewindowCommon_x11ErrorHandlerEnable(env, NULL, 0, 0, errorHandlerQuiet, 0 /* no dpy, no sync */);
    x11IOErrorHandlerEnable(0, env);
}

JNIEXPORT void JNICALL 
Java_jogamp_nativewindow_x11_X11Util_setX11ErrorHandler0(JNIEnv *env, jclass _unused, jboolean onoff, jboolean quiet) {
    NativewindowCommon_x11ErrorHandlerEnable(env, NULL, 1, onoff ? 1 : 0, quiet ? 1 : 0, 0 /* no dpy, force, no sync */);
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.x11.X11Lib
 *    Java method: boolean XRenderFindVisualFormat(long dpy, long visual, XRenderPictFormat dest)
 */
JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_XRenderFindVisualFormat1(JNIEnv *env, jclass _unused, jlong dpy, jlong visual, jobject xRenderPictFormat) {
  XRenderPictFormat * dest = (XRenderPictFormat *) (*env)->GetDirectBufferAddress(env, xRenderPictFormat);
  XRenderPictFormat * src = XRenderFindVisualFormat((Display *) (intptr_t) dpy, (Visual *) (intptr_t) visual);
  if (NULL == src) return JNI_FALSE;
  memcpy(dest, src, sizeof(XRenderPictFormat));
  return JNI_TRUE;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.x11.X11Lib
 *    Java method: XVisualInfo XGetVisualInfo(long arg0, long arg1, XVisualInfo arg2, java.nio.IntBuffer arg3)
 *     C function: XVisualInfo *  XGetVisualInfo(Display * , long, XVisualInfo * , int * );
 */
JNIEXPORT jobject JNICALL
Java_jogamp_nativewindow_x11_X11Lib_XGetVisualInfo1__JJLjava_nio_ByteBuffer_2Ljava_lang_Object_2I(JNIEnv *env, jclass _unused, jlong arg0, jlong arg1, jobject arg2, jobject arg3, jint arg3_byte_offset) {
  XVisualInfo * _ptr2 = NULL;
  int * _ptr3 = NULL;
  XVisualInfo *  _res = NULL;
  int count = 0;
  jobject jbyteSource = NULL;
  jobject jbyteCopy = NULL;
  if( 0 == arg0 || 0 == arg2 || 0 == arg3 ) {
    NativewindowCommon_FatalError(env, "invalid display connection, vinfo_template or nitems_return");
    return NULL;
  }
  _ptr2 = (XVisualInfo *) (((char*) (*env)->GetDirectBufferAddress(env, arg2)) + 0);
  if( NULL != _ptr2 ) {
      _ptr3 = (int *) (((char*) (*env)->GetPrimitiveArrayCritical(env, arg3, NULL)) + arg3_byte_offset);
      if( NULL != _ptr3 ) {
          NativewindowCommon_x11ErrorHandlerEnable(env, (Display *) (intptr_t) arg0, 0, 1, errorHandlerQuiet, 0);
          _res = XGetVisualInfo((Display *) (intptr_t) arg0, (long) arg1, (XVisualInfo *) _ptr2, (int *) _ptr3);
          // NativewindowCommon_x11ErrorHandlerEnable(env, (Display *) (intptr_t) arg0, 0, 0, errorHandlerQuiet, 0);
          count = _ptr3[0];
          (*env)->ReleasePrimitiveArrayCritical(env, arg3, _ptr3, 0);
      }
  }
  if (_res == NULL) return NULL;

  jbyteSource = (*env)->NewDirectByteBuffer(env, _res, count * sizeof(XVisualInfo));
  jbyteCopy   = (*env)->CallStaticObjectMethod(env, clazzBuffers, cstrBuffers, jbyteSource);
  (*env)->DeleteLocalRef(env, jbyteSource);

  XFree(_res);

  return jbyteCopy;
}

JNIEXPORT jint JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_GetVisualIDFromWindow(JNIEnv *env, jclass _unused, jlong display, jlong window) {
    Display * dpy = (Display *)(intptr_t)display;
    Window      w = (Window) window;
    XWindowAttributes xwa;
    jlong r = 0; // undefinded

    if(NULL==dpy) {
        NativewindowCommon_throwNewRuntimeException(env, "invalid display connection..");
        return 0;
    }

    NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 1, errorHandlerQuiet, 1);
    memset(&xwa, 0, sizeof(XWindowAttributes));
    XGetWindowAttributes(dpy, w, &xwa);
    if(NULL != xwa.visual) {
        r = (jint) XVisualIDFromVisual( xwa.visual );
    } else {
        r = 0;
    }
    // NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 0, errorHandlerQuiet, 1);

    return r;
}


JNIEXPORT jint JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_DefaultVisualID(JNIEnv *env, jclass _unused, jlong display, jint screen) {
  jlong r;
    if(0==display) {
        NativewindowCommon_FatalError(env, "invalid display connection..");
    }
  NativewindowCommon_x11ErrorHandlerEnable(env, (Display *) (intptr_t) display, 0, 1, errorHandlerQuiet, 0);
  r = (jint) XVisualIDFromVisual( DefaultVisual( (Display*) (intptr_t) display, screen ) );
  // NativewindowCommon_x11ErrorHandlerEnable(env, (Display *) (intptr_t) display, 0, 0, errorHandlerQuiet, 0);
  return r;
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.x11.X11Lib
 *    Java method: void XLockDisplay(long display)
 *     C function: void XLockDisplay(Display *  display);
 */
JNIEXPORT void JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_XLockDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  if(0==display) {
      NativewindowCommon_FatalError(env, "invalid display connection..");
  }
  XLockDisplay((Display *) (intptr_t) display);
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.x11.X11Lib
 *    Java method: void XUnlockDisplay(long display)
 *     C function: void XUnlockDisplay(Display *  display);
 */
JNIEXPORT void JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_XUnlockDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  if(0==display) {
      NativewindowCommon_FatalError(env, "invalid display connection..");
  }
  XUnlockDisplay((Display *) (intptr_t) display);
}

/*   Java->C glue code:
 *   Java package: jogamp.nativewindow.x11.X11Lib
 *    Java method: int XCloseDisplay(long display)
 *     C function: int XCloseDisplay(Display *  display);
 */
JNIEXPORT jint JNICALL 
Java_jogamp_nativewindow_x11_X11Lib_XCloseDisplay__J(JNIEnv *env, jclass _unused, jlong display) {
  int _res;
  if(0==display) {
      NativewindowCommon_FatalError(env, "invalid display connection..");
  }
  NativewindowCommon_x11ErrorHandlerEnable(env, NULL, 0, 1, errorHandlerQuiet, 0);
  _res = XCloseDisplay((Display *) (intptr_t) display);
  // NativewindowCommon_x11ErrorHandlerEnable(env, NULL, 0, 0, errorHandlerQuiet, 0);
  return _res;
}

static void NativewindowX11_setNormalWindowEWMH (Display *dpy, Window w) {
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[1]={0};
    types[0] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, 1);
    XSync(dpy, False);
}

#define DECOR_USE_MWM 1     // works for known WMs
// #define DECOR_USE_EWMH 1 // haven't seen this to work (NORMAL->POPUP, never gets undecorated)

/* see <http://tonyobryan.com/index.php?article=9> */
#define MWM_HINTS_DECORATIONS   (1L << 1)
#define PROP_MWM_HINTS_ELEMENTS 5

static void NativewindowX11_setDecorations (Display *dpy, Window w, Bool decorated) {

#ifdef DECOR_USE_MWM
    unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { MWM_HINTS_DECORATIONS, 0, decorated, 0, 0 }; // flags, functions, decorations, input_mode, status
    Atom _MOTIF_WM_HINTS = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
#endif

#ifdef DECOR_USE_EWMH
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[3]={0};
    int ntypes=0;
    if(True==decorated) {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    } else {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_POPUP_MENU", False );
    }
#endif

#ifdef DECOR_USE_MWM
    XChangeProperty( dpy, w, _MOTIF_WM_HINTS, _MOTIF_WM_HINTS, 32, PropModeReplace, (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
#endif

#ifdef DECOR_USE_EWMH
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
#endif

    XSync(dpy, False);
}

/*
 * Class:     jogamp_nativewindow_x11_X11Lib
 * Method:    CreateWindow
 * Signature: (JJIIIIZZ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_x11_X11Lib_CreateWindow
  (JNIEnv *env, jclass unused, jlong parent, jlong display, jint screen_index, jint visualID, jint width, jint height, jboolean input, jboolean visible)
{
    Display * dpy  = (Display *)(intptr_t)display;
    int       scrn_idx = (int)screen_index;
    Window root = RootWindow(dpy, scrn_idx);
    Window  windowParent = (Window) parent;
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
        NativewindowCommon_FatalError(env, "invalid display connection..");
        return 0;
    }

    if(visualID<0) {
        NativewindowCommon_throwNewRuntimeException(env, "invalid VisualID ..");
        return 0;
    }

    NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 1, errorHandlerQuiet, 0);

    scrn = ScreenOfDisplay(dpy, scrn_idx);
    if(0==windowParent) {
        windowParent = root;
    }

    // try given VisualID on screen
    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.screen = scrn_idx;
    visualTemplate.visualid = (VisualID)visualID;
    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask|VisualScreenMask, &visualTemplate,&n);
    if(pVisualQuery!=NULL) {
        visual   = pVisualQuery->visual;
        depth    = pVisualQuery->depth;
        visualID = (jint)pVisualQuery->visualid;
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }
    DBG_PRINT( "X11: [CreateWindow] trying given (dpy %p, screen %d, visualID: %d, parent %p) found: %p\n", dpy, scrn_idx, (int)visualID, windowParent, visual);

    if (visual==NULL)
    { 
        // NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 0, errorHandlerQuiet, 1);
        NativewindowCommon_throwNewRuntimeException(env, "could not query Visual by given VisualID, bail out!");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }


    attrMask  = ( CWBackingStore | CWBackingPlanes | CWBackingPixel | CWBackPixmap | 
                  CWBorderPixel | CWColormap | CWOverrideRedirect ) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = False; // use the window manager, always
    xswa.border_pixel = 0;
    xswa.background_pixmap = None;
    xswa.backing_store=NotUseful; /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;        /* planes to be preserved if possible */
    xswa.backing_pixel=0;         /* value to use in restoring planes */
    if( input ) {
        xswa.event_mask  = X11_MOUSE_EVENT_MASK;
        xswa.event_mask |= KeyPressMask | KeyReleaseMask ;
    }
    if( visible ) {
        xswa.event_mask |= FocusChangeMask | SubstructureNotifyMask | StructureNotifyMask | ExposureMask ;
    }

    xswa.colormap = XCreateColormap(dpy,
                                    windowParent,
                                    visual,
                                    AllocNone);

    window = XCreateWindow(dpy,
                           windowParent,
                           0, 0, // only a hint, WM most likely will override
                           width, height,
                           0, // border width
                           depth,
                           InputOutput,
                           visual,
                           attrMask,
                           &xswa);
    if(0==window) {
        NativewindowCommon_throwNewRuntimeException(env, "could not create Window, bail out!");
        return 0;
    }

    NativewindowX11_setNormalWindowEWMH(dpy, window);
    NativewindowX11_setDecorations(dpy, window, False);

    if( visible ) {
        XEvent event;

        XMapWindow(dpy, window);
    }

    XSync(dpy, False);

    if( !input ) {
        XSelectInput(dpy, window, 0); // no events
    }

    // NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 0, errorHandlerQuiet, 1);

    DBG_PRINT( "X11: [CreateWindow] created window %p on display %p\n", window, dpy);

    return (jlong) window;
}


/*
 * Class:     jogamp_nativewindow_x11_X11Lib
 * Method:    DestroyWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_x11_X11Lib_DestroyWindow
  (JNIEnv *env, jclass unused, jlong display, jlong window)
{
    Display * dpy = (Display *)(intptr_t)display;
    Window      w = (Window) window;
    XWindowAttributes xwa;

    if(NULL==dpy) {
        NativewindowCommon_throwNewRuntimeException(env, "invalid display connection..");
        return;
    }

    NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 1, errorHandlerQuiet, 0);
    XSync(dpy, False);
    memset(&xwa, 0, sizeof(XWindowAttributes));
    XGetWindowAttributes(dpy, w, &xwa); // prefetch colormap to be destroyed after window destruction
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);
    XDestroyWindow(dpy, w);
    if( None != xwa.colormap ) {
        XFreeColormap(dpy, xwa.colormap);
    }
    // NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 0, errorHandlerQuiet, 1);
}

JNIEXPORT void JNICALL Java_jogamp_nativewindow_x11_X11Lib_SetWindowPosSize
  (JNIEnv *env, jclass unused, jlong display, jlong window, jint x, jint y, jint width, jint height) {
    Display * dpy = (Display *)(intptr_t)display;
    Window      w = (Window) window;
    XWindowChanges xwc;
    int flags = 0;
    
    memset(&xwc, 0, sizeof(XWindowChanges));

    if(0<=x && 0<=y) {
        flags |= CWX | CWY;
        xwc.x=x;
        xwc.y=y;
    }

    if(0<width && 0<height) {
        flags |= CWWidth | CWHeight;
        xwc.width=width;
        xwc.height=height;
    }
    XConfigureWindow(dpy, w, flags, &xwc);
    XSync(dpy, False);
}

/*
 * Class:     jogamp_nativewindow_x11_X11Lib
 * Method:    GetRelativeLocation
 * Signature: (JIJJII)Lcom/jogamp/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_x11_X11Lib_GetRelativeLocation0
  (JNIEnv *env, jclass unused, jlong jdisplay, jint screen_index, jlong jsrc_win, jlong jdest_win, jint src_x, jint src_y)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    Screen * scrn = ScreenOfDisplay(dpy, (int)screen_index);
    Window root = XRootWindowOfScreen(scrn);
    Window src_win = (Window)jsrc_win;
    Window dest_win = (Window)jdest_win;
    int dest_x=-1;
    int dest_y=-1;
    Window child;
    Bool res;

    if( 0 == jdest_win ) { dest_win = root; }
    if( 0 == jsrc_win ) { src_win = root; }

    NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 1, errorHandlerQuiet, 0);

    res = XTranslateCoordinates(dpy, src_win, dest_win, src_x, src_y, &dest_x, &dest_y, &child);

    // NativewindowCommon_x11ErrorHandlerEnable(env, dpy, 0, 0, errorHandlerQuiet, 0);

    DBG_PRINT( "X11: GetRelativeLocation0: %p %d/%d -> %p %d/%d - ok: %d\n",
        (void*)src_win, src_x, src_y, (void*)dest_win, dest_x, dest_y, (int)res);

    return (*env)->NewObject(env, pointClz, pointCstr, (jint)dest_x, (jint)dest_y);
}

/*
 * Class:     jogamp_nativewindow_x11_X11Lib
 * Method:    QueryExtension0
 * Signature: (JLjava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_x11_X11Lib_QueryExtension0
  (JNIEnv *env, jclass unused, jlong jdisplay, jstring jextensionName)
{
    int32_t major_opcode, first_event, first_error;
    jboolean res = JNI_FALSE;
    Display * display = (Display *) (intptr_t) jdisplay;
    const char* extensionName = NULL;

    if(NULL==display) {
        NativewindowCommon_throwNewRuntimeException(env, "NULL argument \"display\"");
        return res;
    }
    if ( NULL == jextensionName ) {
        NativewindowCommon_throwNewRuntimeException(env, "NULL argument \"extensionName\"");
        return res;
    }
    extensionName = (*env)->GetStringUTFChars(env, jextensionName, (jboolean*)NULL);
    if ( NULL == extensionName ) {
        NativewindowCommon_throwNewRuntimeException(env, "Failed to get UTF-8 chars for argument \"extensionName\"");
        return res;
    }

    res = True == XQueryExtension(display, extensionName, &major_opcode, &first_event, &first_error) ? JNI_TRUE : JNI_FALSE;

    if ( NULL != jextensionName ) {
        (*env)->ReleaseStringUTFChars(env, jextensionName, extensionName);
    }
    return res;
}

