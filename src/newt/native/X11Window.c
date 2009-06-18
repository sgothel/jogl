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

#include "com_sun_javafx_newt_x11_X11Window.h"

#include "EventListener.h"
#include "MouseEvent.h"
#include "KeyEvent.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    // Workaround for ancient compiler on Solaris/SPARC
    // #define DBG_PRINT(args...) fprintf(stderr, args); fflush(stderr)
    #define DBG_PRINT0(str) fprintf(stderr, str); flush(stderr)
    #define DBG_PRINT1(str, arg1) fprintf(stderr, str, arg1); flush(stderr)
    #define DBG_PRINT2(str, arg1, arg2) fprintf(stderr, str, arg1, arg2); flush(stderr)
    #define DBG_PRINT3(str, arg1, arg2, arg3) fprintf(stderr, str, arg1, arg2, arg3); flush(stderr)
    #define DBG_PRINT4(str, arg1, arg2, arg3, arg4) fprintf(stderr, str, arg1, arg2, arg3, arg4); flush(stderr)

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

    // Workaround for ancient compiler on Solaris/SPARC
    // #define DBG_PRINT(args...)
    #define DBG_PRINT0(str)
    #define DBG_PRINT1(str, arg1)
    #define DBG_PRINT2(str, arg1, arg2)
    #define DBG_PRINT3(str, arg1, arg2, arg3)
    #define DBG_PRINT4(str, arg1, arg2, arg3, arg4)

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

static const char * const ClazzNameRuntimeException = 
                            "java/lang/RuntimeException";
static jclass    runtimeExceptionClz=NULL;

static const char * const ClazzNameNewtWindow = 
                            "com/sun/javafx/newt/Window";
static jclass    newtWindowClz=NULL;

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID windowCreatedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

static jmethodID displayCompletedID = NULL;

static void _throwNewRuntimeException(JNIEnv *env, const char* msg, ...)
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

/*
 * Class:     com_sun_javafx_newt_x11_X11Display
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_x11_X11Display_initIDs
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
            fprintf(stderr, "FatalError: NEWT X11Window: can't find %s\n", ClazzNameNewtWindow);
            return JNI_FALSE;
        }
        newtWindowClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==newtWindowClz) {
            fprintf(stderr, "FatalError: NEWT X11Window: can't use %s\n", ClazzNameNewtWindow);
            return JNI_FALSE;
        }
    }

    if(NULL==runtimeExceptionClz) {
        c = (*env)->FindClass(env, ClazzNameRuntimeException);
        if(NULL==c) {
            fprintf(stderr, "FatalError: NEWT X11Window: can't find %s\n", ClazzNameRuntimeException);
            return JNI_FALSE;
        }
        runtimeExceptionClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==runtimeExceptionClz) {
            fprintf(stderr, "FatalError: NEWT X11Window: can't use %s\n", ClazzNameRuntimeException);
            return JNI_FALSE;
        }
    }

    return JNI_TRUE;
}


/*
 * Class:     com_sun_javafx_newt_x11_X11Display
 * Method:    CompleteDisplay
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Display_CompleteDisplay
  (JNIEnv *env, jobject obj, jlong display)
{
    Display * dpy = (Display *)(intptr_t)display;
    jlong javaObjectAtom;
    jlong windowDeleteAtom;
    if(dpy==NULL) {
        _throwNewRuntimeException(env, "given display connection is NULL\n");
    }

    javaObjectAtom = (jlong) XInternAtom(dpy, "JOGL_JAVA_OBJECT", False);
    if(None==javaObjectAtom) {
        _throwNewRuntimeException(env, "could not create Atom JOGL_JAVA_OBJECT, bail out!\n");
        return;
    }

    windowDeleteAtom = (jlong) XInternAtom(dpy, "WM_DELETE_WINDOW", False);
    if(None==windowDeleteAtom) {
        _throwNewRuntimeException(env, "could not create Atom WM_DELETE_WINDOW, bail out!\n");
        return;
    }

    DBG_PRINT1("X11Display_completeDisplay dpy %p\n", dpy);

    (*env)->CallVoidMethod(env, obj, displayCompletedID, javaObjectAtom, windowDeleteAtom);
}

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
    unsigned long jogl_java_object_data[2];
    int nitems_32 = putPtrIn32Long( jogl_java_object_data, (uintptr_t) jwindow);

    {
        jobject test = (jobject) getPtrOut32Long(jogl_java_object_data);
        if( ! (jwindow==test) ) {
            _throwNewRuntimeException(env, "Internal Error .. Encoded Window ref not the same %p != %p !\n", jwindow, test);
        }
    }

    XChangeProperty( dpy, window, (Atom)javaObjectAtom, (Atom)javaObjectAtom, 32, PropModeReplace, 
                                     (unsigned char *)&jogl_java_object_data, nitems_32);
}

static jobject getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom) {
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
            _throwNewRuntimeException(env, "could not fetch Atom JOGL_JAVA_OBJECT window property (res %d) nitems_return %ld, bytes_after_return %ld, bail out!\n",
                res, nitems_return, bytes_after_return);
            return NULL;
        }

        if(actual_type_return!=(Atom)javaObjectAtom || nitems_return<nitems_32 || NULL==jogl_java_object_data_pp) {
            XFree(jogl_java_object_data_pp);
            _throwNewRuntimeException(env, "could not fetch Atom JOGL_JAVA_OBJECT window property (res %d) nitems_return %ld, bytes_after_return %ld, actual_type_return %ld, JOGL_JAVA_OBJECT %ld, bail out!\n",
                res, nitems_return, bytes_after_return, (long)actual_type_return, javaObjectAtom);
            return NULL;
        }
    }
    jwindow = (jobject) getPtrOut32Long( (unsigned long *) jogl_java_object_data_pp ) ;
    XFree(jogl_java_object_data_pp);

#ifdef VERBOSE_ON
    if(JNI_FALSE == (*env)->IsInstanceOf(env, jwindow, newtWindowClz)) {
        _throwNewRuntimeException(env, "fetched Atom JOGL_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !\n", jwindow);
    }
#endif
    return jwindow;
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Display
 * Method:    DispatchMessages
 * Signature: (JIJJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Display_DispatchMessages
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    int num_events = 0;

    // Periodically take a break
    while( num_events<100 && XPending(dpy)>0 ) {
        jobject jwindow = NULL;
        XEvent evt;
        KeySym keySym;
        char keyChar;
        char text[255];

        XNextEvent(dpy, &evt);
        num_events++;

        if( 0==evt.xany.window ) {
            _throwNewRuntimeException(env, "event window NULL, bail out!\n");
        }

        if(dpy!=evt.xany.display) {
            _throwNewRuntimeException(env, "wrong display");
            continue;
        }
        jwindow = getJavaWindowProperty(env, dpy, evt.xany.window, javaObjectAtom);
        if(NULL==jwindow) {
            // just leave .. _throwNewRuntimeException(env, "could not fetch Java Window object, bail out!\n");
            return;
        }
 
        // FIXME: support resize and window re-positioning events

        switch(evt.type) {
            case ButtonPress:
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                break;
            case ButtonRelease:
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                break;
            case MotionNotify:
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                      (jint) evt.xmotion.state, 
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0 /*rotation*/); 
                break;
            case KeyPress:
                if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                    keyChar=text[0];
                } else {
                    keyChar=0;
                }
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                      (jint) evt.xkey.state, 
                                      X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                break;
            case KeyRelease:
                if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                    keyChar=text[0];
                } else {
                    keyChar=0;
                }
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                      (jint) evt.xkey.state, 
                                      X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                break;
            case FocusIn:
            case FocusOut:
                break;
            case DestroyNotify:
                DBG_PRINT1( "event . DestroyNotify call 0x%X\n", evt.xdestroywindow.window);
                (*env)->CallVoidMethod(env, jwindow, windowDestroyedID);
                break;
            case CreateNotify:
                DBG_PRINT1( "event . CreateNotify call 0x%X\n", evt.xcreatewindow.window);
                (*env)->CallVoidMethod(env, jwindow, windowCreatedID);
                break;
            case VisibilityNotify:
                DBG_PRINT1( "event . VisibilityNotify call 0x%X\n", evt.xvisibility.window);
                break;
            case Expose:
                DBG_PRINT1( "event . Expose call 0x%X\n", evt.xexpose.window);
                /* FIXME: Might want to send a repaint event .. */
                break;
            case UnmapNotify:
                DBG_PRINT1( "event . UnmapNotify call 0x%X\n", evt.xunmap.window);
                break;
            case ClientMessage:
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==(Atom)wmDeleteAtom) {
                    DBG_PRINT2( "event . ClientMessage call 0x%X type 0x%X !!!\n", evt.xclient.window, evt.xclient.message_type);
                    (*env)->CallVoidMethod(env, jwindow, windowDestroyNotifyID);
                    // Called by Window.java: CloseWindow(); 
                }
                break;
        }
    } 
}


/**
 * Screen
 */

/*
 * Class:     com_sun_javafx_newt_x11_X11Screen
 * Method:    GetScreen
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_x11_X11Screen_GetScreen
  (JNIEnv *env, jobject obj, jlong display, jint screen_index)
{
    Display * dpy = (Display *)(intptr_t)display;
    Screen  * scrn= NULL;
    if(dpy==NULL) {
        fprintf(stderr, "[GetScreen] invalid display connection..\n");
        return 0;
    }
    scrn = ScreenOfDisplay(dpy,screen_index);
    if(scrn==NULL) {
        scrn=DefaultScreenOfDisplay(dpy);
    }
    if(scrn==NULL) {
        fprintf(stderr, "couldn't get screen ..\n");
    }
    return (jlong) (intptr_t) scrn;
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_x11_X11Screen_getWidth0
  (JNIEnv *env, jobject obj, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayWidth( dpy, scrn_idx);
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_x11_X11Screen_getHeight0
  (JNIEnv *env, jobject obj, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayHeight( dpy, scrn_idx);
}


/**
 * Window
 */

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_x11_X11Window_initIDs
  (JNIEnv *env, jclass clazz)
{
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(II)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(J)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");

    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowDestroyedID == NULL ||
        windowCreatedID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    CreateWindow
 * Signature: (JIJIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_x11_X11Window_CreateWindow
  (JNIEnv *env, jobject obj, jlong display, jint screen_index, 
                             jlong visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height)
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
    Atom wm_delete_atom;

    DBG_PRINT4( "CreateWindow %x/%d %dx%d\n", x, y, width, height);

    if(dpy==NULL) {
        fprintf(stderr, "[CreateWindow] invalid display connection..\n");
        return 0;
    }

    if(visualID<0) {
        fprintf(stderr, "[CreateWindow] invalid VisualID ..\n");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, screen_index);

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
    DBG_PRINT4( "[CreateWindow] trying given (dpy %p, screen %d, visualID: %d) found: %p\n", dpy, scrn_idx, (int)visualID, visual);

    if (visual==NULL)
    { 
        _throwNewRuntimeException(env, "could not query Visual by given VisualID, bail out!\n");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }

    windowParent = XRootWindowOfScreen(scrn);

    attrMask  = (CWBackPixel | CWBorderPixel | CWColormap | CWEventMask | CWOverrideRedirect) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = False; // decorated
    xswa.border_pixel = 0;
    xswa.background_pixel = 0;
    xswa.event_mask = ExposureMask | StructureNotifyMask | KeyPressMask | KeyReleaseMask;
    xswa.colormap = XCreateColormap(dpy,
                                    XRootWindow(dpy, scrn_idx),
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

    wm_delete_atom = (Atom)windowDeleteAtom;
    XSetWMProtocols(dpy, window, &wm_delete_atom, 1);

    setJavaWindowProperty(env, dpy, window, javaObjectAtom, (*env)->NewGlobalRef(env, obj));

    XClearWindow(dpy, window);
    XSync(dpy, False);

    {
        long xevent_mask = 0;
        xevent_mask |= ButtonPressMask|ButtonReleaseMask|PointerMotionMask;
        xevent_mask |= KeyPressMask|KeyReleaseMask;
        xevent_mask |= ExposureMask | StructureNotifyMask | SubstructureNotifyMask | VisibilityNotify ;
        XSelectInput(dpy, window, xevent_mask);

        /**
            XGrabPointer(dpy, window, True, ButtonPressMask|ButtonReleaseMask|PointerMotionMask,
                        GrabModeAsync, GrabModeAsync, window, None, CurrentTime);
            XGrabKeyboard(dpy, window, True, GrabModeAsync, GrabModeAsync, CurrentTime);
        */
    }

    DBG_PRINT2( "[CreateWindow] created window %p on display %p\n", window, dpy);
    (*env)->CallVoidMethod(env, obj, windowCreatedID, (jlong) window);

    return (jlong) window;
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_CloseWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;

    jwindow = getJavaWindowProperty(env, dpy, w, javaObjectAtom);
    if(NULL==jwindow) {
        _throwNewRuntimeException(env, "could not fetch Java Window object, bail out!\n");
        return;
    }
    if ( JNI_FALSE == (*env)->IsSameObject(env, jwindow, obj) ) {
        _throwNewRuntimeException(env, "Internal Error .. Window global ref not the same!\n");
    }
    (*env)->DeleteGlobalRef(env, jwindow);

    XSync(dpy, False);
    /**
     XUngrabPointer(dpy, CurrentTime);
     XUngrabKeyboard(dpy, CurrentTime);
     */
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);
    XDestroyWindow(dpy, w);
    XSync(dpy, False);
    (*env)->CallVoidMethod(env, obj, windowDestroyedID);
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_setVisible0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean visible)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    DBG_PRINT1( "setVisible0 vis %d\n", visible);
    XSync(dpy, False);
    if(visible==JNI_TRUE) {
        XMapRaised(dpy, w);
        XSync(dpy, False);

        // XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
        // XSync(dpy, False);

    } else {
        /**
         XUngrabPointer(dpy, CurrentTime);
         XUngrabKeyboard(dpy, CurrentTime);
         */
        XUnmapWindow(dpy, w);
        XSync(dpy, False);
    }
}

#define MWM_FULLSCREEN 1

#ifdef MWM_FULLSCREEN
    #define MWM_HINTS_DECORATIONS   (1L << 1)
    #define PROP_MWM_HINTS_ELEMENTS 5
#endif

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    setSize0
 * Signature: (JIJIIIIIZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_setSize0
  (JNIEnv *env, jobject obj, jlong display, jint screen_index, jlong window, jint x, jint y, jint width, jint height, jint decorationToggle, jboolean isVisible)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    Screen * scrn = ScreenOfDisplay(dpy, (int)screen_index);
    Window parent = XRootWindowOfScreen(scrn);

    XWindowChanges xwc;

    DBG_PRINT4( "setSize0 %dx%d, dec %d, vis %d\n", width, height, decorationToggle, isVisible);

    XSync(dpy, False);

    if(0!=decorationToggle) {
#ifdef MWM_FULLSCREEN
        unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { 0, 0, 0, 0, 0 }; // flags, functions, decorations, input_mode, status
        Atom prop;

        mwmhints[0] = MWM_HINTS_DECORATIONS;
        mwmhints[2] = (decorationToggle<0)?False:True;
        prop = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
        XChangeProperty( dpy, w, prop, prop, 32, PropModeReplace, (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
#else
        XSetWindowAttributes xswa;
        unsigned long attrMask=CWOverrideRedirect;

        if(decorationToggle<0) {
            /* undecorated  */
            xswa.override_redirect = True; 
        } else {
            /* decorated  */
            xswa.override_redirect = False;
        }
        XChangeWindowAttributes(dpy, w, attrMask, &xswa);
        XReparentWindow( dpy, w, parent, x, y );
#endif
    }
    XSync(dpy, False);
    DBG_PRINT0( "setSize0 . XConfigureWindow\n");
    xwc.width=width;
    xwc.height=height;
    XConfigureWindow(dpy, w, CWWidth|CWHeight, &xwc);
    XReparentWindow( dpy, w, parent, x, y );

    XSync(dpy, False);

    /** if(isVisible==JNI_TRUE) {
        // this ..
        XSetInputFocus(dpy, w, RevertToNone, CurrentTime);

        // or this ..
        unsigned long wmleader[1] = { (unsigned long) w};
        Atom prop = XInternAtom( dpy, "WM_CLIENT_LEADER", False );
        XChangeProperty( dpy, w, prop, prop, 32, PropModeReplace, (unsigned char *)&wmleader, 1);
    } */

    DBG_PRINT0( "setSize0 . sizeChangedID call\n");
    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height);
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    setPosition0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_setPosition0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    XWindowChanges xwc;

    DBG_PRINT0( "setPos0 . XConfigureWindow\n");
    xwc.x=x;
    xwc.y=y;
    XConfigureWindow(dpy, w, CWX|CWY, &xwc);
    XSync(dpy, False);

    // (*env)->CallVoidMethod(env, obj, positionChangedID, (jint) width, (jint) height);
}

