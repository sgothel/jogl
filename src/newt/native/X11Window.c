/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
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
 * Neither the name of Sun Microsystems, Inc. or the names of
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
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
// Building on obsolete platform on SPARC right now
#ifdef __sparc
  #include <inttypes.h>
#else
  #include <stdint.h>
#endif
#include <unistd.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>

#include "com_jogamp_newt_impl_x11_X11Window.h"

#include "EventListener.h"
#include "MouseEvent.h"
#include "KeyEvent.h"
#include "WindowEvent.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 

    #define DUMP_VISUAL_INFO(a,b) _dumpVisualInfo((a),(b))

    static void _dumpVisualInfo(const char * msg, XVisualInfo *pVisualQuery) {
        if(pVisualQuery!=NULL) {
            fprintf(stderr, "%s: screen %d, visual: %p, visual-id: 0x%X, depth: %d, class %d, cmap sz: %d, bpp: 3x%d, rgb 0x%X 0x%X 0x%X\n",
                msg,
                pVisualQuery->screen,
                pVisualQuery->visual,
                (int)pVisualQuery->visualid,
                pVisualQuery->depth,
                pVisualQuery->class,
                pVisualQuery->colormap_size,
                pVisualQuery->bits_per_rgb,
                (int)pVisualQuery->red_mask,
                (int)pVisualQuery->green_mask,
                (int)pVisualQuery->blue_mask
            );
        } else {
            fprintf(stderr, "%s: NULL XVisualInfo\n", msg);
        }
    }

#else

    #define DBG_PRINT(...)

    #define DUMP_VISUAL_INFO(a,b)

#endif

/**
 * Keycode
 */

#define IS_WITHIN(k,a,b) ((a)<=(k)&&(k)<=(b))

static jint X11KeySym2NewtVKey(KeySym keySym) {
    if(IS_WITHIN(keySym,XK_F1,XK_F12)) 
        return (keySym-XK_F1)+J_VK_F1;

    switch(keySym) {
        case XK_Alt_L:
        case XK_Alt_R:
            return J_VK_ALT;

        case XK_Left:
            return J_VK_LEFT;
        case XK_Right:
            return J_VK_RIGHT;
        case XK_Up:
            return J_VK_UP;
        case XK_Down:
            return J_VK_DOWN;
        case XK_Page_Up:
            return J_VK_PAGE_UP;
        case XK_Page_Down:
            return J_VK_PAGE_DOWN;
        case XK_Shift_L:
        case XK_Shift_R:
            return J_VK_SHIFT;
        case XK_Control_L:
        case XK_Control_R:
            return J_VK_CONTROL;
        case XK_Escape:
            return J_VK_ESCAPE;
        case XK_Delete:
            return J_VK_DELETE;
    }
    return keySym;
}

static void _FatalError(JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    fprintf(stderr, "%s\n", buffer);
    (*env)->FatalError(env, buffer);
}

static const char * const ClazzNameRuntimeException = "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

static const char * const ClazzNameNewtWindow = 
                            "com/jogamp/newt/Window";
static jclass    newtWindowClz=NULL;

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID windowCreatedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

static jmethodID displayCompletedID = NULL;

static void _throwNewRuntimeException(Display * unlockDisplay, JNIEnv *env, const char* msg, ...)
{
    char buffer[512];
    va_list ap;

    va_start(ap, msg);
    vsnprintf(buffer, sizeof(buffer), msg, ap);
    va_end(ap);

    (*env)->ThrowNew(env, runtimeExceptionClz, buffer);
}

/**
 * Display
 */

static JNIEnv * x11ErrorHandlerJNIEnv = NULL;
static XErrorHandler origErrorHandler = NULL ;

static int displayDispatchErrorHandler(Display *dpy, XErrorEvent *e)
{
    fprintf(stderr, "Warning: NEWT X11 Error: DisplayDispatch %p, Code 0x%X\n", dpy, e->error_code);
    
    if (e->error_code == BadAtom)
    {
        fprintf(stderr, "         BadAtom (%p): Atom probably already removed\n", (void*)e->resourceid);
    } else if (e->error_code == BadWindow)
    {
        fprintf(stderr, "         BadWindow (%p): Window probably already removed\n", (void*)e->resourceid);
    } else {
        _throwNewRuntimeException(NULL, x11ErrorHandlerJNIEnv, "NEWT X11 Error: Display %p, Code 0x%X", dpy, e->error_code);
    }

    return 0;
}

static void displayDispatchErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origErrorHandler) {
            x11ErrorHandlerJNIEnv = env;
            origErrorHandler = XSetErrorHandler(displayDispatchErrorHandler);
        }
    } else {
        XSetErrorHandler(origErrorHandler);
        origErrorHandler = NULL;
    }
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Display
 * Method:    initIDs
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_x11_X11Display_initIDs0
  (JNIEnv *env, jclass clazz)
{
    jclass c;

    displayCompletedID = (*env)->GetMethodID(env, clazz, "displayCompleted", "(JJ)V");
    if (displayCompletedID == NULL) {
        return JNI_FALSE;
    }

    if(NULL==newtWindowClz) {
        c = (*env)->FindClass(env, ClazzNameNewtWindow);
        if(NULL==c) {
            _FatalError(env, "NEWT X11Window: can't find %s", ClazzNameNewtWindow);
        }
        newtWindowClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==newtWindowClz) {
            _FatalError(env, "NEWT X11Window: can't use %s", ClazzNameNewtWindow);
        }
    }

    if(NULL==runtimeExceptionClz) {
        c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            _FatalError(env, "NEWT X11Window: can't find %s", ClazzNameRuntimeException);
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            _FatalError(env, "NEWT X11Window: can't use %s", ClazzNameRuntimeException);
        }
    }

    return JNI_TRUE;
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Display
 * Method:    CompleteDisplay
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Display_CompleteDisplay0
  (JNIEnv *env, jobject obj, jlong display)
{
    Display * dpy = (Display *)(intptr_t)display;
    jlong javaObjectAtom;
    jlong windowDeleteAtom;

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    javaObjectAtom = (jlong) XInternAtom(dpy, "JOGL_JAVA_OBJECT", False);
    if(None==javaObjectAtom) {
        _throwNewRuntimeException(dpy, env, "could not create Atom JOGL_JAVA_OBJECT, bail out!");
        return;
    }

    windowDeleteAtom = (jlong) XInternAtom(dpy, "WM_DELETE_WINDOW", False);
    if(None==windowDeleteAtom) {
        _throwNewRuntimeException(dpy, env, "could not create Atom WM_DELETE_WINDOW, bail out!");
        return;
    }

    // XSetCloseDownMode(dpy, RetainTemporary); // Just a try ..

    DBG_PRINT("X11: X11Display_completeDisplay dpy %p\n", dpy);

    (*env)->CallVoidMethod(env, obj, displayCompletedID, javaObjectAtom, windowDeleteAtom);
}

/**
 * Window
 */

#define WINDOW_EVENT_MASK ( FocusChangeMask | StructureNotifyMask | ExposureMask )

static int putPtrIn32Long(unsigned long * dst, uintptr_t src) {
    int i=0;
        dst[i++] = (unsigned long) ( ( src >>  0 ) & 0xFFFFFFFF ) ;
    if(sizeof(uintptr_t) == 8) {
        dst[i++] = (unsigned long) ( ( src >> 32 ) & 0xFFFFFFFF ) ;
    }
    return i;
}

static uintptr_t getPtrOut32Long(unsigned long * src) {
    uintptr_t  res = ( (uintptr_t) ( src[0] & 0xFFFFFFFF ) )  <<  0 ;
    if(sizeof(uintptr_t) == 8) {
              res |= ( (uintptr_t) ( src[1] & 0xFFFFFFFF ) )  << 32 ;
    }
    return res;
}

static void setJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, jobject jwindow) {
    unsigned long jogl_java_object_data[2]; // X11 is based on 'unsigned long'
    int nitems_32 = putPtrIn32Long( jogl_java_object_data, (uintptr_t) jwindow);

    {
        jobject test = (jobject) getPtrOut32Long(jogl_java_object_data);
        if( ! (jwindow==test) ) {
            _FatalError(env, "Internal Error .. Encoded Window ref not the same %p != %p !", jwindow, test);
        }
    }

    XChangeProperty( dpy, window, (Atom)javaObjectAtom, (Atom)javaObjectAtom, 32, PropModeReplace, 
                                     (unsigned char *)&jogl_java_object_data, nitems_32);
}

static jobject getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning) {
    Atom actual_type_return;
    int actual_format_return;
    int nitems_32 = ( sizeof(uintptr_t) == 8 ) ? 2 : 1 ;
    unsigned char * jogl_java_object_data_pp = NULL;
    jobject jwindow;

    {
        unsigned long nitems_return = 0;
        unsigned long bytes_after_return = 0;
        jobject jwindow = NULL;
        int res;

        res = XGetWindowProperty(dpy, window, (Atom)javaObjectAtom, 0, nitems_32, False, 
                                 (Atom)javaObjectAtom, &actual_type_return, &actual_format_return, 
                                 &nitems_return, &bytes_after_return, &jogl_java_object_data_pp);

        if ( Success != res ) {
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Could not fetch Atom JOGL_JAVA_OBJECT window property (res %d) nitems_return %ld, bytes_after_return %ld, result 0!\n", res, nitems_return, bytes_after_return);
            }
            return NULL;
        }

        if(actual_type_return!=(Atom)javaObjectAtom || nitems_return<nitems_32 || NULL==jogl_java_object_data_pp) {
            XFree(jogl_java_object_data_pp);
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Fetched invalid Atom JOGL_JAVA_OBJECT window property (res %d) nitems_return %ld, bytes_after_return %ld, actual_type_return %ld, JOGL_JAVA_OBJECT %ld, result 0!\n", 
                res, nitems_return, bytes_after_return, (long)actual_type_return, javaObjectAtom);
            }
            return NULL;
        }
    }

    jwindow = (jobject) getPtrOut32Long( (unsigned long *) jogl_java_object_data_pp ) ;
    XFree(jogl_java_object_data_pp);

#ifdef VERBOSE_ON
    if(JNI_FALSE == (*env)->IsInstanceOf(env, jwindow, newtWindowClz)) {
        _throwNewRuntimeException(dpy, env, "fetched Atom JOGL_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !", jwindow);
    }
#endif
    return jwindow;
}

static void NewtWindows_requestFocus0 (Display *dpy, Window w, XWindowAttributes *xwa) {
    // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
    if(xwa->map_state == IsViewable) {
        XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
    }
}

static void NewtWindows_requestFocus1 (Display *dpy, Window w) {
    XWindowAttributes xwa;

    XGetWindowAttributes(dpy, w, &xwa);
    NewtWindows_requestFocus0 (dpy, w, &xwa);
    XSync(dpy, False);
}

/** 
 * Changing this attribute while a window is established,
 * returns the previous value.
 */
static Bool NewtWindows_setOverrideRedirect0 (Display *dpy, Window w, XWindowAttributes *xwa, Bool newVal) {
    Bool oldVal = xwa->override_redirect;
    XSetWindowAttributes xswa;

    if(oldVal != newVal) {
        memset(&xswa, 0, sizeof(XSetWindowAttributes));
        xswa.override_redirect = newVal; 
        XChangeWindowAttributes(dpy, w, CWOverrideRedirect, &xswa);
    }
    return oldVal;
}

static Bool NewtWindows_setOverrideRedirect1 (Display *dpy, Window w, Bool newVal) {
    XWindowAttributes xwa;
    Bool oldVal;

    XSync(dpy, False);
    XGetWindowAttributes(dpy, w, &xwa);

    return NewtWindows_setOverrideRedirect0 (dpy, w, &xwa, newVal);
}

#define MWM_HINTS_DECORATIONS   (1L << 1)
#define PROP_MWM_HINTS_ELEMENTS 5

static void NewtWindows_setDecorations (Display *dpy, Window w, Bool val) {
    unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { 0, 0, 0, 0, 0 }; // flags, functions, decorations, input_mode, status
    Atom prop;

    mwmhints[0] = MWM_HINTS_DECORATIONS;
    mwmhints[2] = val ;
    prop = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
    XChangeProperty( dpy, w, prop, prop, 32, PropModeReplace, (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Display
 * Method:    DispatchMessages
 * Signature: (JIJJ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Display_DispatchMessages0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    int num_events = 100;

    if ( NULL == dpy ) {
        return;
    }

    // Periodically take a break
    while( num_events > 0 ) {
        jobject jwindow = NULL;
        XEvent evt;
        KeySym keySym;
        char keyChar;
        char text[255];

        // num_events = XPending(dpy); // I/O Flush ..
        // num_events = XEventsQueued(dpy, QueuedAfterFlush); // I/O Flush only of no already queued events are available
        // num_events = XEventsQueued(dpy, QueuedAlready); // no I/O Flush at all, doesn't work on some cards (eg ATI)
        if ( 0 >= XEventsQueued(dpy, QueuedAfterFlush) ) {
            // DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 1\n", dpy); 
            return;
        }

        XNextEvent(dpy, &evt);
        num_events--;

        if( 0==evt.xany.window ) {
            _throwNewRuntimeException(dpy, env, "event window NULL, bail out!");
            return ;
        }

        if(dpy!=evt.xany.display) {
            _throwNewRuntimeException(dpy, env, "wrong display, bail out!");
            return ;
        }

        DBG_PRINT( "X11: DispatchMessages dpy %p, win %p, Event %d\n", (void*)dpy, (void*)evt.xany.window, evt.type);

        displayDispatchErrorHandlerEnable(1, env);

        jwindow = getJavaWindowProperty(env, dpy, evt.xany.window, javaObjectAtom,
        #ifdef VERBOSE_ON
                True
        #else
                False
        #endif
            );

        displayDispatchErrorHandlerEnable(0, env);

        if(NULL==jwindow) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d for X11 window %p\n", 
                (void*)dpy, evt.type, (void*)evt.xany.window);
            continue;
        }
 
        switch(evt.type) {
            case KeyRelease:
            case KeyPress:
                if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                    keyChar=text[0];
                } else {
                    keyChar=0;
                }
                break;
            default:
                break;
        }

        switch(evt.type) {
            case ButtonPress:
                NewtWindows_requestFocus1 ( dpy, evt.xany.window );
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, 
                                      (jint) EVENT_MOUSE_PRESSED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                break;
            case ButtonRelease:
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, 
                                      (jint) EVENT_MOUSE_RELEASED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                break;
            case MotionNotify:
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, 
                                      (jint) EVENT_MOUSE_MOVED, 
                                      (jint) evt.xmotion.state, 
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0 /*rotation*/); 
                break;
            case KeyPress:
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, 
                                      (jint) EVENT_KEY_PRESSED, 
                                      (jint) evt.xkey.state, 
                                      X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                break;
            case KeyRelease:
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, 
                                      (jint) EVENT_KEY_RELEASED, 
                                      (jint) evt.xkey.state, 
                                      X11KeySym2NewtVKey(keySym), (jchar) keyChar);

                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, 
                                      (jint) EVENT_KEY_TYPED, 
                                      (jint) evt.xkey.state, 
                                      (jint) -1, (jchar) keyChar);
                break;
            case DestroyNotify:
                DBG_PRINT( "X11: event . DestroyNotify call 0x%X\n", (unsigned int)evt.xdestroywindow.window);
                (*env)->CallVoidMethod(env, jwindow, windowDestroyedID);
                break;
            case CreateNotify:
                DBG_PRINT( "X11: event . CreateNotify call 0x%X\n", (unsigned int)evt.xcreatewindow.window);
                (*env)->CallVoidMethod(env, jwindow, windowCreatedID);
                break;
            case ConfigureNotify:
                DBG_PRINT( "X11: event . ConfigureNotify call 0x%X (parent 0x%X, above 0x%X) %d/%d %dx%d %d\n", 
                            (unsigned int)evt.xconfigure.window, (unsigned int)evt.xconfigure.event, (unsigned int)evt.xconfigure.above,
                            evt.xconfigure.x, evt.xconfigure.y, evt.xconfigure.width, evt.xconfigure.height, 
                            evt.xconfigure.override_redirect);
                (*env)->CallVoidMethod(env, jwindow, sizeChangedID, 
                                        (jint) evt.xconfigure.width, (jint) evt.xconfigure.height);
                (*env)->CallVoidMethod(env, jwindow, positionChangedID, 
                                        (jint) evt.xconfigure.x, (jint) evt.xconfigure.y);
                break;
            case ClientMessage:
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==(Atom)wmDeleteAtom) {
                    DBG_PRINT( "X11: event . ClientMessage call 0x%X type 0x%X !!!\n", (unsigned int)evt.xclient.window, (unsigned int)evt.xclient.message_type);
                    (*env)->CallVoidMethod(env, jwindow, windowDestroyNotifyID);
                    // Called by Window.java: CloseWindow(); 
                }
                break;

            case FocusIn:
                DBG_PRINT( "X11: event . FocusIn call 0x%X\n", (unsigned int)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, jwindow, focusChangedID, JNI_TRUE);
                break;

            case FocusOut:
                DBG_PRINT( "X11: event . FocusOut call 0x%X\n", (unsigned int)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, jwindow, focusChangedID, JNI_FALSE);
                break;

            case Expose:
                DBG_PRINT( "X11: event . Expose call 0x%X %d/%d %dx%d\n", (unsigned int)evt.xexpose.window,
                    evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height);

                if (evt.xexpose.width > 0 && evt.xexpose.height > 0) {
                    (*env)->CallVoidMethod(env, jwindow, windowRepaintID, 
                        evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height);
                }
                break;

            case MapNotify:
                DBG_PRINT( "X11: event . MapNotify call 0x%X\n", (unsigned int)evt.xunmap.window);
                (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_TRUE);
                break;

            case UnmapNotify:
                DBG_PRINT( "X11: event . UnmapNotify call 0x%X\n", (unsigned int)evt.xunmap.window);
                (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_FALSE);
                break;

            // unhandled events .. yet ..

            default:
                DBG_PRINT("X11: event . unhandled %d 0x%X call 0x%X\n", evt.type, evt.type, (unsigned int)evt.xunmap.window);
        }
    }
}


/**
 * Screen
 */

/*
 * Class:     com_jogamp_newt_impl_x11_X11Screen
 * Method:    GetScreen
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_x11_X11Screen_GetScreen0
  (JNIEnv *env, jobject obj, jlong display, jint screen_index)
{
    Display * dpy = (Display *)(intptr_t)display;
    Screen  * scrn= NULL;

    DBG_PRINT("X11: X11Screen_GetScreen0 dpy %p START\n", dpy);

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    scrn = ScreenOfDisplay(dpy,screen_index);
    if(scrn==NULL) {
        scrn=DefaultScreenOfDisplay(dpy);
    }
    if(scrn==NULL) {
        fprintf(stderr, "couldn't get screen ..\n");
    }
    DBG_PRINT("X11: X11Screen_GetScreen0 scrn %p DONE\n", scrn);
    return (jlong) (intptr_t) scrn;
}

JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_x11_X11Screen_getWidth0
  (JNIEnv *env, jobject obj, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayWidth( dpy, scrn_idx);
}

JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_x11_X11Screen_getHeight0
  (JNIEnv *env, jobject obj, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayHeight( dpy, scrn_idx);
}


/**
 * Window
 */

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_x11_X11Window_initIDs0
  (JNIEnv *env, jclass clazz)
{
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(II)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(Z)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(IIII)V");
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(J)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");

    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowDestroyedID == NULL ||
        windowRepaintID == NULL ||
        windowCreatedID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    CreateWindow
 * Signature: (JJIJIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_x11_X11Window_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jint screen_index, 
                             jlong visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height,
                             jboolean undecorated)
{
    Display * dpy  = (Display *)(intptr_t)display;
    int       scrn_idx = (int)screen_index;
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
    Atom wm_delete_atom;

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    if(visualID<0) {
        _throwNewRuntimeException(NULL, env, "invalid VisualID ..");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, scrn_idx);
    if(0==windowParent) {
        windowParent = XRootWindowOfScreen(scrn);
    }
    DBG_PRINT( "X11: CreateWindow dpy %p, parent %p, %x/%d %dx%d, undeco %d\n", 
        (void*)dpy, (void*)windowParent, x, y, width, height, undecorated);

    // try given VisualID on screen
    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.screen = scrn_idx;
    visualTemplate.visualid = (VisualID)visualID;
    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask|VisualScreenMask, &visualTemplate,&n);
    DUMP_VISUAL_INFO("Given VisualID,ScreenIdx", pVisualQuery);
    if(pVisualQuery!=NULL) {
        visual   = pVisualQuery->visual;
        depth    = pVisualQuery->depth;
        visualID = (jlong)pVisualQuery->visualid;
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }
    DBG_PRINT( "X11: [CreateWindow] trying given (dpy %p, screen %d, visualID: %d, parent %p) found: %p\n", 
        dpy, scrn_idx, (int)visualID, (void*)windowParent, visual);

    if (visual==NULL)
    { 
        _throwNewRuntimeException(dpy, env, "could not query Visual by given VisualID, bail out!");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }

    attrMask  = ( CWBackingStore | CWBackingPlanes | CWBackingPixel | CWBackPixel | 
                  CWBorderPixel | CWColormap | CWOverrideRedirect ) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = ( 0 != parent ) ? True : False ;
    xswa.border_pixel = 0;
    xswa.background_pixel = 0;
    xswa.backing_store=NotUseful; /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;        /* planes to be preserved if possible */
    xswa.backing_pixel=0;         /* value to use in restoring planes */

    xswa.colormap = XCreateColormap(dpy,
                                    windowParent, // XRootWindow(dpy, scrn_idx),
                                    visual,
                                    AllocNone);

    window = XCreateWindow(dpy,
                           windowParent,
                           x, y,
                           width, height,
                           0, // border width
                           depth,
                           InputOutput,
                           visual,
                           attrMask,
                           &xswa);

    if(0==window) {
        _throwNewRuntimeException(dpy, env, "could not create Window, bail out!");
        return 0;
    }

    wm_delete_atom = (Atom)windowDeleteAtom;
    XSetWMProtocols(dpy, window, &wm_delete_atom, 1);

    setJavaWindowProperty(env, dpy, window, javaObjectAtom, (*env)->NewGlobalRef(env, obj));

    // XClearWindow(dpy, window);
    XSync(dpy, False);

    {
        long xevent_mask = 0;
        xevent_mask |= ButtonPressMask | ButtonReleaseMask | PointerMotionMask;
        xevent_mask |= KeyPressMask | KeyReleaseMask;
        xevent_mask |= WINDOW_EVENT_MASK ;
        XSelectInput(dpy, window, xevent_mask);
    }

    NewtWindows_setDecorations(dpy, window, ( JNI_TRUE == undecorated ) ? False : True );
    XSync(dpy, False);

    DBG_PRINT( "X11: [CreateWindow] created window 0x%X on display %p\n", (unsigned int)window, dpy);
    (*env)->CallVoidMethod(env, obj, windowCreatedID, (jlong) window);

    return (jlong) window;
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    DBG_PRINT( "X11: CloseWindow START dpy %p, win %p\n", (void*)dpy, (void*)w);

    jwindow = getJavaWindowProperty(env, dpy, w, javaObjectAtom, True);
    if(NULL==jwindow) {
        _throwNewRuntimeException(dpy, env, "could not fetch Java Window object, bail out!");
        return;
    }
    if ( JNI_FALSE == (*env)->IsSameObject(env, jwindow, obj) ) {
        _throwNewRuntimeException(dpy, env, "Internal Error .. Window global ref not the same!");
        return;
    }

    XSync(dpy, False);
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);

    // Drain all events related to this window ..
    JNICALL Java_com_jogamp_newt_impl_x11_X11Display_DispatchMessages0(env, obj, display, javaObjectAtom, wmDeleteAtom);

    XDestroyWindow(dpy, w);
    XSync(dpy, False);

    (*env)->DeleteGlobalRef(env, jwindow);

    DBG_PRINT( "X11: CloseWindow END\n");

    (*env)->CallVoidMethod(env, obj, windowDestroyedID);
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_setVisible0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean visible)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    DBG_PRINT( "X11: setVisible0 vis %d\n", visible);

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    if(visible==JNI_TRUE) {
        XMapRaised(dpy, w);
    } else {
        XUnmapWindow(dpy, w);
    }
    XSync(dpy, False);
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    setSize0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_setSize0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jint width, jint height)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    XWindowChanges xwc;

    DBG_PRINT( "X11: setSize0 %dx%d\n", width, height);

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.width=width;
    xwc.height=height;
    XConfigureWindow(dpy, w, CWWidth|CWHeight, &xwc);

    XSync(dpy, False);
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    setPosition0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_setPosition0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jlong window, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    XWindowChanges xwc;

    DBG_PRINT( "X11: setPos0 . XConfigureWindow %d/%d\n", x, y);
    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.x=x;
    xwc.y=y;
    XConfigureWindow(dpy, w, CWX|CWY, &xwc);
    XSync(dpy, False);
}

static void NewtWindows_reparentWindow
  (Display * dpy, Screen * scrn, Window w, XWindowAttributes *xwa, jlong jparent, jint x, jint y, jboolean undecorated, jboolean isVisible)
{
    Window parent = (0!=jparent)?(Window)jparent:XRootWindowOfScreen(scrn);

    DBG_PRINT( "X11: reparentWindow dpy %p, parent %p/%p, win %p, %d/%d undec %d\n", 
        (void*)dpy, (void*) jparent, (void*)parent, (void*)w, x, y, undecorated);

    // don't propagate events during reparenting
    // long orig_xevent_mask = xwa->your_event_mask ;
    /* XSelectInput(dpy, w, orig_xevent_mask & ~ ( StructureNotifyMask ) );
       XSync(dpy, False); */

    if(0 != jparent) {
        // move into parent ..
        NewtWindows_setDecorations (dpy, w, False);
        XSync(dpy, False);
        NewtWindows_setOverrideRedirect0 (dpy, w, xwa, True);
        XSync(dpy, False);
    }

    if(JNI_TRUE == isVisible) {
        XUnmapWindow(dpy, w);
        XSync(dpy, False);
    }

    XReparentWindow( dpy, w, parent, x, y );
    XSync(dpy, False);

    if(0 == jparent) 
    {
        // move out of parent ..
        NewtWindows_setOverrideRedirect0 (dpy, w, xwa, (0 == jparent) ? False : True);
        XSync(dpy, False);
        NewtWindows_setDecorations (dpy, w, (JNI_TRUE == undecorated) ? False : True);
        XSync(dpy, False);
    }

    if(JNI_TRUE == isVisible) {
        XRaiseWindow(dpy, w);
        XSync(dpy, False);

        XMapWindow(dpy, w);
        XSync(dpy, False);
    }

    /* XSelectInput(dpy, w, orig_xevent_mask);
       XSync(dpy, False); */

    if(JNI_TRUE == isVisible) {
        NewtWindows_requestFocus0 ( dpy, w, xwa );
        XSync(dpy, False);
    }

    DBG_PRINT( "X11: reparentWindow X\n");
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    setPosSizeDecor0
 * Signature: (JJIJIIIIZ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_setPosSizeDecor0
  (JNIEnv *env, jobject obj, jlong jparent, jlong display, jint screen_index, jlong window, 
   jint x, jint y, jint width, jint height, jboolean undecorated, jboolean isVisible) 
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    Screen * scrn = ScreenOfDisplay(dpy, (int)screen_index);

    XWindowChanges xwc;
    XWindowAttributes xwa;

    DBG_PRINT( "X11: setPosSizeDecor0 dpy %p, parent %p, win %p, %d/%d %dx%d undec %d, visible %d\n", 
        (void*)dpy, (void*) jparent, (void*)w, x, y, width, height, undecorated, isVisible);

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    XSync(dpy, False);
    XGetWindowAttributes(dpy, w, &xwa);

    NewtWindows_reparentWindow(dpy, scrn, w, &xwa, jparent, x, y, undecorated, isVisible);
    XSync(dpy, False);

    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.x=x;
    xwc.y=y;
    xwc.width=width;
    xwc.height=height;
    XConfigureWindow(dpy, w, CWX|CWY|CWWidth|CWHeight, &xwc);
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    reparentWindow0
 * Signature: (JJIJIIZ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_reparentWindow0
  (JNIEnv *env, jobject obj, jlong jparent, jlong display, jint screen_index, jlong window, jint x, jint y, 
   jboolean undecorated, jboolean isVisible)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    Screen * scrn = ScreenOfDisplay(dpy, (int)screen_index);
    XWindowAttributes xwa;

    DBG_PRINT( "X11: reparentWindow0 dpy %p, parent %p, win %p, %d/%d undec %d, visible %d\n", 
        (void*)dpy, (void*) jparent, (void*)w, x, y, undecorated, isVisible);

    if(dpy==NULL) {
        _FatalError(env, "invalid display connection..");
    }

    XSync(dpy, False);
    XGetWindowAttributes(dpy, w, &xwa);

    NewtWindows_reparentWindow(dpy, scrn, w, &xwa, jparent, x, y, undecorated, isVisible);
    XSync(dpy, False);

    DBG_PRINT( "X11: reparentWindow0 X\n");
}

/*
 * Class:     com_jogamp_newt_impl_x11_X11Window
 * Method:    requestFocus0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_requestFocus0
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    NewtWindows_requestFocus1 ( (Display *) (intptr_t) display, (Window)window ) ;
}

/*
 * Class:     Java_com_jogamp_newt_impl_x11_X11Window
 * Method:    setTitle0
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_x11_X11Window_setTitle0
  (JNIEnv *env, jclass clazz, jlong display, jlong window, jstring title)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

#if 1
    const char* title_str;
    if (NULL != title) {
        title_str = (*env)->GetStringUTFChars(env, title, NULL);
        if(NULL != title_str) {
            DBG_PRINT( "X11: setTitle: <%s> SET\n", title_str);
            XStoreName(dpy, w, title_str);
            (*env)->ReleaseStringUTFChars(env, title, title_str);
        } else {
            DBG_PRINT( "X11: setTitle: NULL - NOT SET (1)\n");
        }
    } else {
        DBG_PRINT( "X11: setTitle: NULL TITLE\n");
    }
#else
    char *str_list[] = { NULL };
    XTextProperty text_prop;
    if (NULL != title) {
        str_list[0] = (char *) NewtCommon_GetNullTerminatedStringChars(env, title);
        if (str_list[0] != NULL) {
            memset(&text_prop, 0, sizeof(XTextProperty));
            if ( Success != XmbTextListToTextProperty(dpy, str_list, 1, XStringStyle, &text_prop) ) {
                DBG_PRINT( "X11: setTitle.XmbTextListToTextProperty not completly successfull\n");
                fprintf(stderr, "X11: setTitle.XmbTextListToTextProperty not completly successfull\n");
            }
            if(NULL!=text_prop.value) {
                DBG_PRINT( "X11: setTitle: <%s> SET\n", str_list[0]);
                XSetWMName(dpy, w, &text_prop);
                XFree(text_prop.value);
            } else {
                DBG_PRINT( "X11: setTitle: <%s> NOT SET (1)\n", str_list[0]);
            }
            free(str_list[0]);
        } else {
            DBG_PRINT( "X11: setTitle: NULL\n");
        }
    }
#endif
}


