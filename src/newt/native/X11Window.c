/*
 * Copyright (c) 2008 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2010 JogAmp Community. All rights reserved.
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

#include <gluegen_stdint.h>

#include <unistd.h>
#include <errno.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>
#include <X11/Xatom.h>

#include <X11/extensions/Xrandr.h>

#include "jogamp_newt_driver_x11_X11Screen.h"
#include "jogamp_newt_driver_x11_X11Display.h"
#include "jogamp_newt_driver_x11_X11Window.h"

#include "Window.h"
#include "MouseEvent.h"
#include "InputEvent.h"
#include "KeyEvent.h"
#include "WindowEvent.h"
#include "ScreenMode.h"

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

static jint X11InputState2NewtModifiers(unsigned int xstate) {
    jint modifiers = 0;
    if ((ControlMask & xstate) != 0) {
        modifiers |= EVENT_CTRL_MASK;
    }
    if ((ShiftMask & xstate) != 0) {
        modifiers |= EVENT_SHIFT_MASK;
    }
    if ((Mod1Mask & xstate) != 0) {
        modifiers |= EVENT_ALT_MASK;
    }
    if ((Button1Mask & xstate) != 0) {
        modifiers |= EVENT_BUTTON1_MASK;
    }
    if ((Button2Mask & xstate) != 0) {
        modifiers |= EVENT_BUTTON2_MASK;
    }
    if ((Button3Mask & xstate) != 0) {
        modifiers |= EVENT_BUTTON3_MASK;
    }

    return modifiers;
}

static const char * const ClazzNameNewtWindow = "com/jogamp/newt/Window";

static jclass    newtWindowClz=NULL;

static jmethodID insetsChangedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID reparentNotifyID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID enqueueMouseEventID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID focusActionID = NULL;
static jmethodID enqueueRequestFocusID = NULL;

static jmethodID displayCompletedID = NULL;


/**
 * Display
 */

static JavaVM *jvmHandle = NULL;
static int jvmVersion = 0;

static void setupJVMVars(JNIEnv * env) {
    if(0 != (*env)->GetJavaVM(env, &jvmHandle)) {
        jvmHandle = NULL;
    }
    jvmVersion = (*env)->GetVersion(env);
}

static XErrorHandler origErrorHandler = NULL ;

static int displayDispatchErrorHandler(Display *dpy, XErrorEvent *e)
{
    fprintf(stderr, "Warning: NEWT X11 Error: DisplayDispatch %p, Code 0x%X, errno %s\n", dpy, e->error_code, strerror(errno));
    
    if (e->error_code == BadAtom) {
        fprintf(stderr, "         BadAtom (%p): Atom probably already removed\n", (void*)e->resourceid);
    } else if (e->error_code == BadWindow) {
        fprintf(stderr, "         BadWindow (%p): Window probably already removed\n", (void*)e->resourceid);
    } else {
        int shallBeDetached = 0;
        JNIEnv *jniEnv = NULL;
        const char * errStr = strerror(errno);

        fprintf(stderr, "Info: NEWT X11 Error: Display %p, Code 0x%X, errno %s\n", dpy, e->error_code, errStr);

        jniEnv = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
        if(NULL==jniEnv) {
            fprintf(stderr, "NEWT X11 Error: null JNIEnv");
            return;
        }

        NewtCommon_throwNewRuntimeException(jniEnv, "Info: NEWT X11 Error: Display %p, Code 0x%X, errno %s", 
                                            dpy, e->error_code, errStr);

        if (shallBeDetached) {
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        }
    }

    return 0;
}

static void displayDispatchErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origErrorHandler) {
            setupJVMVars(env);
            origErrorHandler = XSetErrorHandler(displayDispatchErrorHandler);
        }
    } else {
        if(NULL!=origErrorHandler) {
            XSetErrorHandler(origErrorHandler);
            origErrorHandler = NULL;
        }
    }
}

/*
 * Class:     jogamp_newt_driver_x11_X11Display
 * Method:    initIDs
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Display_initIDs0
  (JNIEnv *env, jclass clazz)
{
    jclass c;

    NewtCommon_init(env);

    displayCompletedID = (*env)->GetMethodID(env, clazz, "displayCompleted", "(JJ)V");
    if (displayCompletedID == NULL) {
        return JNI_FALSE;
    }

    if(NULL==newtWindowClz) {
        c = (*env)->FindClass(env, ClazzNameNewtWindow);
        if(NULL==c) {
            NewtCommon_FatalError(env, "NEWT X11Window: can't find %s", ClazzNameNewtWindow);
        }
        newtWindowClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==newtWindowClz) {
            NewtCommon_FatalError(env, "NEWT X11Window: can't use %s", ClazzNameNewtWindow);
        }
    }

    return JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Display
 * Method:    CompleteDisplay
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Display_CompleteDisplay0
  (JNIEnv *env, jobject obj, jlong display)
{
    Display * dpy = (Display *)(intptr_t)display;
    jlong javaObjectAtom;
    jlong windowDeleteAtom;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    javaObjectAtom = (jlong) XInternAtom(dpy, "NEWT_JAVA_OBJECT", False);
    if(None==javaObjectAtom) {
        NewtCommon_throwNewRuntimeException(env, "could not create Atom NEWT_JAVA_OBJECT, bail out!");
        return;
    }

    windowDeleteAtom = (jlong) XInternAtom(dpy, "WM_DELETE_WINDOW", False);
    if(None==windowDeleteAtom) {
        NewtCommon_throwNewRuntimeException(env, "could not create Atom WM_DELETE_WINDOW, bail out!");
        return;
    }

    // XSetCloseDownMode(dpy, RetainTemporary); // Just a try ..

    DBG_PRINT("X11: X11Display_completeDisplay dpy %p\n", dpy);

    (*env)->CallVoidMethod(env, obj, displayCompletedID, javaObjectAtom, windowDeleteAtom);
}

/*
 * Class:     jogamp_newt_driver_x11_X11Display
 * Method:    DisplayRelease0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Display_DisplayRelease0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong windowDeleteAtom)
{
    Display * dpy = (Display *)(intptr_t)display;
    Atom wm_javaobject_atom = (Atom)javaObjectAtom;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    // nothing to do to free the atoms !
    (void) wm_javaobject_atom;
    (void) wm_delete_atom;

    XSync(dpy, True); // discard all pending events
    DBG_PRINT("X11: X11Display_DisplayRelease dpy %p\n", dpy);
}


/**
 * Window
 */

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
            NewtCommon_FatalError(env, "Internal Error .. Encoded Window ref not the same %p != %p !", jwindow, test);
        }
    }

    XChangeProperty( dpy, window, (Atom)javaObjectAtom, (Atom)javaObjectAtom, 32, PropModeReplace, 
                                     (unsigned char *)&jogl_java_object_data, nitems_32);
}

static jobject getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning) {
    Atom actual_type;
    int actual_format;
    int nitems_32 = ( sizeof(uintptr_t) == 8 ) ? 2 : 1 ;
    unsigned char * jogl_java_object_data_pp = NULL;
    jobject jwindow;

    {
        unsigned long nitems= 0;
        unsigned long bytes_after= 0;
        jobject jwindow = NULL;
        int res;

        res = XGetWindowProperty(dpy, window, (Atom)javaObjectAtom, 0, nitems_32, False, 
                                 (Atom)javaObjectAtom, &actual_type, &actual_format, 
                                 &nitems, &bytes_after, &jogl_java_object_data_pp);

        if ( Success != res ) {
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Could not fetch Atom NEWT_JAVA_OBJECT window property (res %d) nitems %ld, bytes_after %ld, result 0!\n", res, nitems, bytes_after);
            }
            return NULL;
        }

        if(actual_type!=(Atom)javaObjectAtom || nitems<nitems_32 || NULL==jogl_java_object_data_pp) {
            XFree(jogl_java_object_data_pp);
            if(True==showWarning) {
                fprintf(stderr, "Warning: NEWT X11Window: Fetched invalid Atom NEWT_JAVA_OBJECT window property (res %d) nitems %ld, bytes_after %ld, actual_type %ld, NEWT_JAVA_OBJECT %ld, result 0!\n", 
                res, nitems, bytes_after, (long)actual_type, (long)javaObjectAtom);
            }
            return NULL;
        }
    }

    jwindow = (jobject) getPtrOut32Long( (unsigned long *) jogl_java_object_data_pp ) ;
    XFree(jogl_java_object_data_pp);

#ifdef VERBOSE_ON
    if(JNI_FALSE == (*env)->IsInstanceOf(env, jwindow, newtWindowClz)) {
        NewtCommon_throwNewRuntimeException(env, "fetched Atom NEWT_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !", jwindow);
    }
#endif
    return jwindow;
}

/** @return zero if fails, non zero if OK */
static Status NewtWindows_getRootAndParent (Display *dpy, Window w, Window * root_return, Window * parent_return) {
    Window *children_return=NULL;
    unsigned int nchildren_return=0;

    Status res = XQueryTree(dpy, w, root_return, parent_return, &children_return, &nchildren_return);
    if(NULL!=children_return) {
        XFree(children_return);
    }
    return res; // 0 == res -> Error
}
static Window NewtWindows_getRoot (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return root_return;
    }
    return 0; // Error
}
static Window NewtWindows_getParent (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return parent_return;
    }
    return 0; // Error
}
static Status NewtWindows_getParentPosition (Display *dpy, Window w, int *x_return, int *y_return) {
    Window root_return;
    unsigned int width_return, height_return;
    unsigned int border_width_return;
    unsigned int depth_return;
    Window parent = NewtWindows_getParent(dpy, w);

    if(0 != parent) {
        XGetGeometry(dpy, parent, &root_return, x_return, y_return, &width_return, 
                               &height_return, &border_width_return, &depth_return);
        return 1; // OK
    }
    return 0; // Error
}
static Status NewtWindows_getFrameExtends(Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    Atom actual_type;
    int actual_format;
    int nitems_32 = 4; // l, r, t, b
    unsigned char * frame_extends_data_pp = NULL;

    {
        Atom _NET_FRAME_EXTENTS = XInternAtom( dpy, "_NET_FRAME_EXTENTS", False );
        unsigned long nitems = 0;
        unsigned long bytes_after = 0;
        int res;

        res = XGetWindowProperty(dpy, window, _NET_FRAME_EXTENTS, 0, nitems_32, False, 
                                 AnyPropertyType, &actual_type, &actual_format, 
                                 &nitems, &bytes_after, &frame_extends_data_pp);

        if ( Success != res ) {
            fprintf(stderr, "Error: NEWT X11Window: Could not fetch Atom _NET_FRAME_EXTENTS window property (res %d) nitems %ld, bytes_after %ld, result 0!\n", res, nitems, bytes_after);
            return 0; // Error
        }

        if(nitems<nitems_32 || NULL==frame_extends_data_pp) {
            XFree(frame_extends_data_pp);
            // DBG_PRINT( "Warning: NEWT X11Window: Fetched invalid Atom _NET_FRAME_EXTENTS window property (res %d) nitems %ld, bytes_after %ld, actual_type %ld, actual_format %d, _NET_FRAME_EXTENTS %ld, result 0!\n", 
            //     res, nitems, bytes_after, (long)actual_type, actual_format, _NET_FRAME_EXTENTS);
            return 0; // Error, but ok - ie window not mapped
        }
    }
    long * extends = (long*) frame_extends_data_pp;
    *left = (int) *(extends + 0);
    *right = (int) *(extends + 1);
    *top = (int) *(extends + 2);
    *bottom = (int) *(extends + 3);

    // DBG_PRINT( "X11: _NET_FRAME_EXTENTS: window %p insets [ l %d, r %d, t %d, b %d ]\n",
    //     (void*)window, *left, *right, *top, *bottom);
        
    XFree(frame_extends_data_pp);

    return 1; // Ok
}
static Status NewtWindows_updateInsets(JNIEnv *env, jobject jwindow, Display *dpy, Window window, int *left, int *right, int *top, int *bottom) {
    if(0 != NewtWindows_getFrameExtends(dpy, window, left, right, top, bottom)) {
        DBG_PRINT( "NewtWindows_updateInsets: insets by _NET_FRAME_EXTENTS [ l %d, r %d, t %d, b %d ]\n",
            *left, *right, *top, *bottom);
        (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
        return 1; // OK
    } else if(0 != NewtWindows_getParentPosition (dpy, window, left, top)) {
        *right = *left; *bottom = *left;
        DBG_PRINT( "NewtWindows_updateInsets: insets by parent position [ l %d, r %d, t %d, b %d ]\n",
            *left, *right, *top, *bottom);
        (*env)->CallVoidMethod(env, jwindow, insetsChangedID, JNI_FALSE, *left, *right, *top, *bottom);
        return 1; // OK
    }
    return 0; // Error
}

static void NewtWindows_setCWAbove(Display *dpy, Window w) {
    XWindowChanges xwc;
    memset(&xwc, 0, sizeof(XWindowChanges));
    xwc.stack_mode = Above;
    XConfigureWindow(dpy, w, CWStackMode, &xwc);
    XSync(dpy, False);
}

static void NewtWindows_requestFocus (JNIEnv *env, jobject window, Display *dpy, Window w, jboolean force) {
    XWindowAttributes xwa;
    Window focus_return;
    int revert_to_return;

    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d\n", dpy, (void*)w, force);

    XGetInputFocus(dpy, &focus_return, &revert_to_return);
    if( JNI_TRUE==force || focus_return!=w) {
        if( JNI_TRUE==force || JNI_FALSE == (*env)->CallBooleanMethod(env, window, focusActionID) ) {
            DBG_PRINT( "X11: XRaiseWindow dpy %p, win %p\n", dpy, (void*)w);
            XRaiseWindow(dpy, w);
            NewtWindows_setCWAbove(dpy, w);
            // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
            XGetWindowAttributes(dpy, w, &xwa);
            if(xwa.map_state == IsViewable) {
                DBG_PRINT( "X11: XSetInputFocus dpy %p,win %pd\n", dpy, (void*)w);
                XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
            }
        }
    }
    DBG_PRINT( "X11: requestFocus dpy %p,win %p, force %d - FIN\n", dpy, (void*)w, force);
    XSync(dpy, False);
}

#define DECOR_USE_MWM 1     // works for known WMs
// #define DECOR_USE_EWMH 1 // haven't seen this to work (NORMAL->POPUP, never gets undecorated)

/* see <http://tonyobryan.com/index.php?article=9> */
#define MWM_HINTS_DECORATIONS   (1L << 1)
#define PROP_MWM_HINTS_ELEMENTS 5

static void NewtWindows_setDecorations (Display *dpy, Window w, Bool decorated) {

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

static void NewtWindows_setNormalWindowEWMH (Display *dpy, Window w) {
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[1]={0};
    types[0] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, 1);
    XSync(dpy, False);
}

#define _NET_WM_STATE_REMOVE 0
#define _NET_WM_STATE_ADD 1

#define _NET_WM_FULLSCREEN ( 1 << 0 )
#define _NET_WM_ABOVE      ( 1 << 1 )

/**
 * Set fullscreen using Extended Window Manager Hints (EWMH)
 *
 * Fullscreen on:
 *   Be aware that _NET_WM_STATE_FULLSCREEN requires a mapped window
 *   which shall be on the top of the stack to work reliable.
 *
 * The WM will internally save the size and position when entering FS
 * and resets it when leaving FS.
 * The same is assumed for the decoration state.
 */
static int NewtWindows_isFullscreenEWMHSupported (Display *dpy, Window w) {
    Atom _NET_WM_ALLOWED_ACTIONS = XInternAtom( dpy, "_NET_WM_ALLOWED_ACTIONS", False );
    Atom _NET_WM_ACTION_FULLSCREEN = XInternAtom( dpy, "_NET_WM_ACTION_FULLSCREEN", False );
    Atom _NET_WM_ACTION_ABOVE = XInternAtom( dpy, "_NET_WM_ACTION_ABOVE", False );
    Atom * actions;
    Atom type;
    unsigned long action_len, remain;
    int res = 0, form, i;
    Status s;

    if ( Success == (s = XGetWindowProperty(dpy, w, _NET_WM_ALLOWED_ACTIONS, 0, 1024, False, AnyPropertyType,
                                            &type, &form, &action_len, &remain, (unsigned char**)&actions)) ) {
        for(i=0; i<action_len; i++) {
            if(_NET_WM_ACTION_FULLSCREEN == actions[i]) {
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_FULLSCREEN (*)\n", i);
                res |= _NET_WM_FULLSCREEN ;
            } else if(_NET_WM_ACTION_ABOVE == actions[i]) {
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: _NET_WM_ACTION_ABOVE (*)\n", i);
                res |= _NET_WM_ABOVE ;
            }
#ifdef VERBOSE_ON
            else {
                char * astr = XGetAtomName(dpy, actions[i]);
                DBG_PRINT( "**************** X11: FS EWMH CHECK[%d]: %s (unused)\n", i, astr);
                XFree(astr);
            }
#endif
        }
        DBG_PRINT( "**************** X11: FS EWMH CHECK: 0x%X\n", res);
    } else {
        DBG_PRINT( "**************** X11: FS EWMH CHECK: XGetWindowProperty failed: %d\n", s);
    }
    // above code doesn't work reliable on KDE4 ...
    res = _NET_WM_FULLSCREEN | _NET_WM_ABOVE ;
    return res;
}

static Bool NewtWindows_setFullscreenEWMH (Display *dpy, Window root, Window w, int ewmhFlags, Bool isVisible, Bool enable) {
    Atom _NET_WM_STATE = XInternAtom( dpy, "_NET_WM_STATE", False );
    Atom _NET_WM_STATE_ABOVE = XInternAtom( dpy, "_NET_WM_STATE_ABOVE", False );
    Atom _NET_WM_STATE_FULLSCREEN = XInternAtom( dpy, "_NET_WM_STATE_FULLSCREEN", False );
    int ewmhMask = NewtWindows_isFullscreenEWMHSupported(dpy, w);
    Bool res = False;

    if(0 == ewmhMask) { 
        return res;
    }

    if(!isVisible && True==enable) {
        Atom types[2]={0};
        int ntypes=0;

        if( 0 != ( ( _NET_WM_FULLSCREEN & ewmhMask ) & ewmhFlags ) )  {
            types[ntypes++] = _NET_WM_STATE_FULLSCREEN;
        }
        if( 0 != ( ( _NET_WM_ABOVE & ewmhMask ) & ewmhFlags ) )  {
            types[ntypes++] = _NET_WM_STATE_ABOVE;
        }
        if(ntypes>0) {
            XChangeProperty( dpy, w, _NET_WM_STATE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
            XSync(dpy, False);
            res = True;
        }
    } else {
        if(enable) {
            NewtWindows_setCWAbove(dpy, w);
        }
        XEvent xev;
        long mask = SubstructureNotifyMask | SubstructureRedirectMask ;
        int i=0;
        
        memset ( &xev, 0, sizeof(xev) );
        
        xev.type = ClientMessage;
        xev.xclient.window = w;
        xev.xclient.message_type = _NET_WM_STATE;
        xev.xclient.format = 32;
            
        xev.xclient.data.l[i++] = ( True == enable ) ? _NET_WM_STATE_ADD : _NET_WM_STATE_REMOVE ;
        if( 0 != ( ( _NET_WM_FULLSCREEN & ewmhMask ) & ewmhFlags ) )  {
            xev.xclient.data.l[i++] = _NET_WM_STATE_FULLSCREEN;
        }
        if( 0 != ( ( _NET_WM_ABOVE & ewmhMask ) & ewmhFlags ) )  {
            xev.xclient.data.l[i++] = _NET_WM_STATE_ABOVE;
        }
        xev.xclient.data.l[3] = 1; //source indication for normal applications

        if(i>0) {
            XSendEvent ( dpy, root, False, mask, &xev );
            res = True;
        }
    }
    XSync(dpy, False);
    DBG_PRINT( "X11: reconfigureWindow0 FULLSCREEN EWMH ON %d, ewmhMask 0x%X, ewmhFlags 0x%X, visible %d: %d\n", 
        enable, ewmhMask, ewmhFlags, isVisible, res);
    return res;
}

#define USE_SENDIO_DIRECT 1

/*
 * Class:     jogamp_newt_driver_x11_X11Display
 * Method:    DispatchMessages
 * Signature: (JIJJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Display_DispatchMessages0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong windowDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    int num_events = 100;

    if ( NULL == dpy ) {
        return;
    }

    // Periodically take a break
    while( num_events > 0 ) {
        jobject jwindow = NULL;
        XEvent evt;
        KeySym keySym = 0;
        jint modifiers = 0;
        char keyChar = 0;
        char text[255];

        // XEventsQueued(dpy, X):
        //   QueuedAlready                 : No I/O Flush or system call  doesn't work on some cards (eg ATI) ?) 
        //   QueuedAfterFlush == XPending(): I/O Flush only if no already queued events are available
        //   QueuedAfterReading            : QueuedAlready + if queue==0, attempt to read more ..
        if ( 0 >= XPending(dpy) ) {
            // DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 1\n", dpy); 
            return;
        }

        XNextEvent(dpy, &evt);
        num_events--;

        if( 0==evt.xany.window ) {
            NewtCommon_throwNewRuntimeException(env, "event window NULL, bail out!");
            return ;
        }

        if(dpy!=evt.xany.display) {
            NewtCommon_throwNewRuntimeException(env, "wrong display, bail out!");
            return ;
        }

        // DBG_PRINT( "X11: DispatchMessages dpy %p, win %p, Event %d\n", (void*)dpy, (void*)evt.xany.window, (int)evt.type);

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
                    KeySym lower_return = 0, upper_return = 0;
                    keyChar=text[0];
                    XConvertCase(keySym, &lower_return, &upper_return);
                    // always return upper case, set modifier masks (SHIFT, ..)
                    keySym = upper_return;
                    modifiers = X11InputState2NewtModifiers(evt.xkey.state);
                } else {
                    keyChar=0;
                }
                break;

            case ButtonPress:
            case ButtonRelease:
            case MotionNotify:
                modifiers = X11InputState2NewtModifiers(evt.xbutton.state);
                break;

            default:
                break;
        }

        switch(evt.type) {
            case ButtonPress:
                (*env)->CallVoidMethod(env, jwindow, enqueueRequestFocusID, JNI_FALSE);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_PRESSED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                #endif
                break;
            case ButtonRelease:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_RELEASED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                #endif
                break;
            case MotionNotify:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                      modifiers,
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0 /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_MOVED, 
                                      modifiers,
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0 /*rotation*/); 
                #endif
                break;
            case KeyPress:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                #endif

                break;
            case KeyRelease:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, X11KeySym2NewtVKey(keySym), (jchar) keyChar);

                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_TYPED, 
                                      modifiers, (jint) -1, (jchar) keyChar);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, X11KeySym2NewtVKey(keySym), (jchar) keyChar);

                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_TYPED, 
                                      modifiers, (jint) -1, (jchar) keyChar);
                #endif

                break;
            case DestroyNotify:
                DBG_PRINT( "X11: event . DestroyNotify call %p, parent %p, child-event: %d\n", 
                    (void*)evt.xdestroywindow.window, (void*)evt.xdestroywindow.event, evt.xdestroywindow.window != evt.xdestroywindow.event);
                if ( evt.xdestroywindow.window == evt.xdestroywindow.event ) {
                    // ignore child destroy notification
                }
                break;
            case CreateNotify:
                DBG_PRINT( "X11: event . CreateNotify call %p, parent %p, child-event: 1\n", 
                    (void*)evt.xcreatewindow.window, (void*) evt.xcreatewindow.parent);
                break;
            case ConfigureNotify:
                DBG_PRINT( "X11: event . ConfigureNotify call %p (parent %p, above %p) %d/%d %dx%d %d, child-event: %d\n", 
                            (void*)evt.xconfigure.window, (void*)evt.xconfigure.event, (void*)evt.xconfigure.above,
                            evt.xconfigure.x, evt.xconfigure.y, evt.xconfigure.width, evt.xconfigure.height, 
                            evt.xconfigure.override_redirect, evt.xconfigure.window != evt.xconfigure.event);
                if ( evt.xconfigure.window == evt.xconfigure.event ) {
                    // ignore child window change notification
                    {
                        // update insets
                        int left, right, top, bottom;
                        NewtWindows_updateInsets(env, jwindow, dpy, evt.xany.window, &left, &right, &top, &bottom);
                    }
                    (*env)->CallVoidMethod(env, jwindow, sizeChangedID, JNI_FALSE,
                                            (jint) evt.xconfigure.width, (jint) evt.xconfigure.height, JNI_FALSE);
                    (*env)->CallVoidMethod(env, jwindow, positionChangedID, JNI_FALSE,
                                            (jint) evt.xconfigure.x, (jint) evt.xconfigure.y);
                }
                break;
            case ClientMessage:
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==wm_delete_atom) { // windowDeleteAtom
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X !!!\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type);
                    (*env)->CallVoidMethod(env, jwindow, windowDestroyNotifyID);
                    // Called by Window.java: CloseWindow(); 
                    num_events = 0; // end loop in case of destroyed display
                }
                break;

            case FocusIn:
                DBG_PRINT( "X11: event . FocusIn call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, jwindow, focusChangedID, JNI_FALSE, JNI_TRUE);
                break;

            case FocusOut:
                DBG_PRINT( "X11: event . FocusOut call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, jwindow, focusChangedID, JNI_FALSE, JNI_FALSE);
                break;

            case Expose:
                DBG_PRINT( "X11: event . Expose call %p %d/%d %dx%d count %d\n", (void*)evt.xexpose.window,
                    evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height, evt.xexpose.count);

                if (evt.xexpose.count == 0 && evt.xexpose.width > 0 && evt.xexpose.height > 0) {
                    (*env)->CallVoidMethod(env, jwindow, windowRepaintID, JNI_FALSE,
                        evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height);
                }
                break;

            case MapNotify:
                DBG_PRINT( "X11: event . MapNotify call Event %p, Window %p, override_redirect %d, child-event: %d\n", 
                    (void*)evt.xmap.event, (void*)evt.xmap.window, (int)evt.xmap.override_redirect,
                    evt.xmap.event!=evt.xmap.window);
                if( evt.xmap.event == evt.xmap.window ) {
                    // ignore child window notification
                    {
                        // update insets
                        int left, right, top, bottom;
                        NewtWindows_updateInsets(env, jwindow, dpy, evt.xany.window, &left, &right, &top, &bottom);
                    }
                    (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_FALSE, JNI_TRUE);
                }
                break;

            case UnmapNotify:
                DBG_PRINT( "X11: event . UnmapNotify call Event %p, Window %p, from_configure %d, child-event: %d\n", 
                    (void*)evt.xunmap.event, (void*)evt.xunmap.window, (int)evt.xunmap.from_configure,
                    evt.xunmap.event!=evt.xunmap.window);
                if( evt.xunmap.event == evt.xunmap.window ) {
                    // ignore child window notification
                    (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_FALSE, JNI_FALSE);
                }
                break;

            case ReparentNotify:
                {
                    jlong parentResult; // 0 if root, otherwise proper value
                    Window winRoot, winTopParent;
                    #ifdef VERBOSE_ON
                        Window oldParentRoot, oldParentTopParent;
                        Window parentRoot, parentTopParent;
                        if( 0 == NewtWindows_getRootAndParent(dpy, evt.xreparent.event, &oldParentRoot, &oldParentTopParent) ) {
                            oldParentRoot=0; oldParentTopParent = 0;
                        }
                        if( 0 == NewtWindows_getRootAndParent(dpy, evt.xreparent.parent, &parentRoot, &parentTopParent) ) {
                            parentRoot=0; parentTopParent = 0;
                        }
                    #endif
                    if( 0 == NewtWindows_getRootAndParent(dpy, evt.xreparent.window, &winRoot, &winTopParent) ) {
                        winRoot=0; winTopParent = 0;
                    }
                    if(evt.xreparent.parent == winRoot) {
                        parentResult = 0; // our java indicator for root window
                    } else {
                        parentResult = (jlong) (intptr_t) evt.xreparent.parent;
                    }
                    #ifdef VERBOSE_ON
                        DBG_PRINT( "X11: event . ReparentNotify: call %d/%d OldParent %p (root %p, top %p), NewParent %p (root %p, top %p), Window %p (root %p, top %p)\n", 
                            evt.xreparent.x, evt.xreparent.y, 
                            (void*)evt.xreparent.event, (void*)oldParentRoot, (void*)oldParentTopParent,
                            (void*)evt.xreparent.parent, (void*)parentRoot, (void*)parentTopParent,
                            (void*)evt.xreparent.window, (void*)winRoot, (void*)winTopParent);
                    #endif
                    (*env)->CallVoidMethod(env, jwindow, reparentNotifyID, (jlong)evt.xreparent.parent);
                }
                break;

            // unhandled events .. yet ..

            default:
                DBG_PRINT("X11: event . unhandled %d 0x%X call %p\n", (int)evt.type, (unsigned int)evt.type, (void*)evt.xunmap.window);
        }
    }
}


/**
 * Screen
 */

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    GetScreen
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Screen_GetScreen0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_index)
{
    Display * dpy = (Display *)(intptr_t)display;
    Screen  * scrn= NULL;

    DBG_PRINT("X11: X11Screen_GetScreen0 dpy %p START\n", dpy);

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    scrn = ScreenOfDisplay(dpy, screen_index);
    if(scrn==NULL) {
        fprintf(stderr, "couldn't get screen idx %d\n", screen_index);
    }
    DBG_PRINT("X11: X11Screen_GetScreen0 idx %d -> scrn %p DONE\n", screen_index, scrn);
    return (jlong) (intptr_t) scrn;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getWidth0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayWidth( dpy, scrn_idx);
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getHeight0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) XDisplayHeight( dpy, scrn_idx);
}


static Bool NewtScreen_getRANDRVersion(Display *dpy, int *major, int *minor) {
    if( 0 == XRRQueryVersion(dpy, major, minor) ) {
        return False;
    }
    return True;
}

static Bool NewtScreen_hasRANDR(Display *dpy) {
    int major, minor;
    return NewtScreen_getRANDRVersion(dpy, &major, &minor);
}

static int NewtScreen_XRotation2Degree(JNIEnv *env, int xrotation) {
    int rot;
    if(xrotation == RR_Rotate_0) {
      rot = 0;
    }
    else if(xrotation == RR_Rotate_90) {
      rot = 90;
    }
    else if(xrotation == RR_Rotate_180) {
      rot = 180;
    }
    else if(xrotation == RR_Rotate_270) {
      rot = 270;
    } else {
      NewtCommon_throwNewRuntimeException(env, "invalid native rotation: %d", xrotation);
    }
    return rot;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getAvailableScreenModeRotations0
 * Signature: (JI)I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_X11Screen_getAvailableScreenModeRotations0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    int num_rotations = 0;
    Rotation cur_rotation, rotations_supported;
    int rotations[4];
    int major, minor;

    if(False == NewtScreen_getRANDRVersion(dpy, &major, &minor)) {
        fprintf(stderr, "RANDR not available\n");
        return (*env)->NewIntArray(env, 0);
    }

    rotations_supported = XRRRotations (dpy, (int)scrn_idx, &cur_rotation);

    if(0 != (rotations_supported & RR_Rotate_0)) {
      rotations[num_rotations++] = 0;
    }
    if(0 != (rotations_supported & RR_Rotate_90)) {
      rotations[num_rotations++] = 90;
    }
    if(0 != (rotations_supported & RR_Rotate_180)) {
      rotations[num_rotations++] = 180;
    }
    if(0 != (rotations_supported & RR_Rotate_270)) {
      rotations[num_rotations++] = 270;
    }
    
    jintArray properties = NULL;

    if(num_rotations>0) {
        properties = (*env)->NewIntArray(env, num_rotations);
        if (properties == NULL) {
            NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", num_rotations);
        }
        
        // move from the temp structure to the java structure
        (*env)->SetIntArrayRegion(env, properties, 0, num_rotations, rotations);
    }
        
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getNumScreenModeResolution0
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getNumScreenModeResolutions0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    
    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getNumScreenModeResolutions0: RANDR not available\n");
        return 0;
    }

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions
    
    return num_sizes;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getScreenModeResolutions0
 * Signature: (JII)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_X11Screen_getScreenModeResolution0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx, jint resMode_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    
    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getScreenModeResolution0: RANDR not available\n");
        return (*env)->NewIntArray(env, 0);
    }

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions

    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }
 
    // Fill the properties in temp jint array
    int propIndex = 0;
    jint prop[4];
    
    prop[propIndex++] = xrrs[(int)resMode_idx].width; 
    prop[propIndex++] = xrrs[(int)resMode_idx].height;
    prop[propIndex++] = xrrs[(int)resMode_idx].mwidth; 
    prop[propIndex++] = xrrs[(int)resMode_idx].mheight;
    
    jintArray properties = (*env)->NewIntArray(env, 4);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", 4);
    }
    
    // move from the temp structure to the java structure
    (*env)->SetIntArrayRegion(env, properties, 0, 4, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getScreenModeRates0
 * Signature: (JII)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_X11Screen_getScreenModeRates0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx, jint resMode_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    
    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getScreenModeRates0: RANDR not available\n");
        return (*env)->NewIntArray(env, 0);
    }

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions

    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }
 
    int num_rates;
    short *rates = XRRRates(dpy, (int)scrn_idx, (int)resMode_idx, &num_rates);
 
    jint prop[num_rates];
    int i;
    for(i=0; i<num_rates; i++) {
        prop[i] = (int) rates[i];
        /** fprintf(stderr, "rate[%d, %d, %d/%d]: %d\n", (int)scrn_idx, resMode_idx, i, num_rates, prop[i]); */
    }
    
    jintArray properties = (*env)->NewIntArray(env, num_rates);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", num_rates);
    }
    
    // move from the temp structure to the java structure
    (*env)->SetIntArrayRegion(env, properties, 0, num_rates, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getCurrentScreenRate0
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenRate0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx) 
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    
    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenRate0: RANDR not available\n");
        return -1;
    }

    // get current resolutions and frequencies
    XRRScreenConfiguration  *conf = XRRGetScreenInfo(dpy, root);
    short original_rate = XRRConfigCurrentRate(conf);

    //free
    XRRFreeScreenConfigInfo(conf);
    
    return (jint) original_rate;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getCurrentScreenRotation0
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenRotation0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    
    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenRotation0: RANDR not available\n");
        return -1;
    }

    //get current resolutions and frequencies
    XRRScreenConfiguration  *conf = XRRGetScreenInfo(dpy, root);
    
    Rotation rotation;
    XRRConfigCurrentConfiguration(conf, &rotation);

    //free
    XRRFreeScreenConfigInfo(conf);
    
    return NewtScreen_XRotation2Degree(env, rotation);
}


/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    getCurrentScreenResolutionIndex0
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenResolutionIndex0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
   Display *dpy = (Display *) (intptr_t) display;
   Window root = RootWindow(dpy, (int)scrn_idx);
  
   if(False == NewtScreen_hasRANDR(dpy)) {
       DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_getCurrentScreenResolutionIndex0: RANDR not available\n");
       return -1;
   }

   // get current resolutions and frequency configuration
   XRRScreenConfiguration  *conf = XRRGetScreenInfo(dpy, root);
   short original_rate = XRRConfigCurrentRate(conf);
   
   Rotation original_rotation;
   SizeID original_size_id = XRRConfigCurrentConfiguration(conf, &original_rotation);
   
   //free
   XRRFreeScreenConfigInfo(conf);
   
   return (jint)original_size_id;   
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    setCurrentScreenModeStart0
 * Signature: (JIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Screen_setCurrentScreenModeStart0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jint resMode_idx, jint freq, jint rotation)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);

    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_setCurrentScreenModeStart0: RANDR not available\n");
        return JNI_FALSE;
    }

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)screen_idx, &num_sizes); //get possible screen resolutions
    XRRScreenConfiguration *conf;
    int rot;
    
    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }

    conf = XRRGetScreenInfo(dpy, root);
   
    switch(rotation) {
        case   0:
            rot = RR_Rotate_0; 
            break;
        case  90:
            rot = RR_Rotate_90; 
            break;
        case 180:
            rot = RR_Rotate_180; 
            break;
        case 270:
            rot = RR_Rotate_270; 
            break;
        default:
            NewtCommon_throwNewRuntimeException(env, "Invalid rotation: %d", rotation);
    }
    
    DBG_PRINT("X11Screen.setCurrentScreenMode0: CHANGED TO %d: %d x %d PIXELS, %d Hz, %d degree\n", 
        resMode_idx, xrrs[resMode_idx].width, xrrs[resMode_idx].height, (int)freq, rotation);

    XRRSelectInput (dpy, root, RRScreenChangeNotifyMask);

    XSync(dpy, False);
    XRRSetScreenConfigAndRate(dpy, conf, root, (int)resMode_idx, rot, (short)freq, CurrentTime);   
    XSync(dpy, False);

    //free
    XRRFreeScreenConfigInfo(conf);
    XSync(dpy, False);

    return JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Screen
 * Method:    setCurrentScreenModePollEnd0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Screen_setCurrentScreenModePollEnd0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jint resMode_idx, jint freq, jint rotation)
{
    Display *dpy = (Display *) (intptr_t) display;
    int randr_event_base, randr_error_base;
    XEvent evt;
    XRRScreenChangeNotifyEvent * scn_event = (XRRScreenChangeNotifyEvent *) &evt;

    if(False == NewtScreen_hasRANDR(dpy)) {
        DBG_PRINT("Java_jogamp_newt_driver_x11_X11Screen_setCurrentScreenModePollEnd0: RANDR not available\n");
        return JNI_FALSE;
    }

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)screen_idx, &num_sizes); //get possible screen resolutions
    XRRScreenConfiguration *conf;
    
    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }

    XRRQueryExtension(dpy, &randr_event_base, &randr_error_base);

    int done = 0;
    int rot;
    do {
        if ( 0 >= XEventsQueued(dpy, QueuedAfterFlush) ) {
            return;
        }
        XNextEvent(dpy, &evt);

        switch (evt.type - randr_event_base) {
            case RRScreenChangeNotify:
                rot = NewtScreen_XRotation2Degree(env, (int)scn_event->rotation);
                DBG_PRINT( "XRANDR: event . RRScreenChangeNotify call %p (root %p) resIdx %d rot %d %dx%d\n", 
                            (void*)scn_event->window, (void*)scn_event->root, 
                            (int)scn_event->size_index, rot, 
                            scn_event->width, scn_event->height);
                // done = scn_event->size_index == resMode_idx; // not reliable ..
                done = rot == rotation && 
                       scn_event->width == xrrs[resMode_idx].width && 
                       scn_event->height == xrrs[resMode_idx].height;
                break;
            default:
                DBG_PRINT("RANDR: event . unhandled %d 0x%X call %p\n", (int)evt.type, (int)evt.type, (void*)evt.xany.window);
        }
        XRRUpdateConfiguration(&evt);
    } while(!done);

    XSync(dpy, False);

}

/**
 * Window
 */

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_X11Window_initIDs0
  (JNIEnv *env, jclass clazz)
{
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    reparentNotifyID = (*env)->GetMethodID(env, clazz, "reparentNotify", "(J)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "()V");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZIIIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZIIIC)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    enqueueRequestFocusID = (*env)->GetMethodID(env, clazz, "enqueueRequestFocus", "(Z)V");
    focusActionID = (*env)->GetMethodID(env, clazz, "focusAction", "()Z");

    if (insetsChangedID == NULL ||
        sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        reparentNotifyID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowRepaintID == NULL ||
        enqueueMouseEventID == NULL ||
        sendMouseEventID == NULL ||
        enqueueKeyEventID == NULL ||
        sendKeyEventID == NULL ||
        focusActionID == NULL ||
        enqueueRequestFocusID == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static Bool WaitForMapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == MapNotify) && (event->xmap.window == (Window) arg);
}

static Bool WaitForUnmapNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == UnmapNotify) && (event->xmap.window == (Window) arg);
}

static void NewtWindows_setPosSize(Display *dpy, Window w, jint x, jint y, jint width, jint height) {
    if(width>0 && height>0 || x>=0 && y>=0) { // resize/position if requested
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
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CreateWindow
 * Signature: (JJIJIIII)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Window_CreateWindow0
  (JNIEnv *env, jobject obj, jlong parent, jlong display, jint screen_index, 
                             jlong visualID, 
                             jlong javaObjectAtom, jlong windowDeleteAtom, 
                             jint x, jint y, jint width, jint height, int flags)
{
    Display * dpy = (Display *)(intptr_t)display;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    int       scrn_idx = (int)screen_index;
    Window root = RootWindow(dpy, scrn_idx);
    Window  windowParent = (Window) parent;
    Window  window = 0;
    jobject jwindow = 0;

    XVisualInfo visualTemplate;
    XVisualInfo *pVisualQuery = NULL;
    Visual *visual = NULL;
    int depth;

    XSetWindowAttributes xswa;
    unsigned long attrMask;
    int n;

    Screen* scrn;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    if(visualID<0) {
        NewtCommon_throwNewRuntimeException(env, "invalid VisualID ..");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, scrn_idx);
    if(0==windowParent) {
        windowParent = root;
    }
    DBG_PRINT( "X11: CreateWindow dpy %p, parent %p, %x/%d %dx%d, undeco %d, alwaysOnTop %d\n", 
        (void*)dpy, (void*)windowParent, x, y, width, height,
        TST_FLAG_IS_UNDECORATED(flags), TST_FLAG_IS_ALWAYSONTOP(flags));

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
        NewtCommon_throwNewRuntimeException(env, "could not query Visual by given VisualID, bail out!");
        return 0;
    } 

    if(pVisualQuery!=NULL) {
        XFree(pVisualQuery);
        pVisualQuery=NULL;
    }

    attrMask  = ( CWBackingStore | CWBackingPlanes | CWBackingPixel | CWBackPixmap |
                  CWBorderPixel | CWColormap | CWOverrideRedirect | CWEventMask ) ;

    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = False; // use the window manager, always (default)
    xswa.border_pixel = 0;
    xswa.background_pixmap = None;
    xswa.backing_store=NotUseful;  /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;         /* planes to be preserved if possible */
    xswa.backing_pixel=0;          /* value to use in restoring planes */
    xswa.event_mask  = ButtonPressMask | ButtonReleaseMask | PointerMotionMask ;
    xswa.event_mask |= KeyPressMask | KeyReleaseMask ;
    xswa.event_mask |= FocusChangeMask | SubstructureNotifyMask | StructureNotifyMask | ExposureMask ;

    xswa.colormap = XCreateColormap(dpy,
                                    windowParent,
                                    visual,
                                    AllocNone);

    {
        int _x = x, _y = y; // pos for CreateWindow, might be tweaked
        if(0>_x || 0>_y) {
            // user didn't requested specific position, use WM default
            _x = 0;
            _y = 0;
        }
        window = XCreateWindow(dpy,
                               windowParent,
                               _x, _y, // only a hint, WM most likely will override
                               width, height,
                               0, // border width
                               depth,
                               InputOutput,
                               visual,
                               attrMask,
                               &xswa);
    }

    if(0==window) {
        NewtCommon_throwNewRuntimeException(env, "could not create Window, bail out!");
        return 0;
    }

    XSetWMProtocols(dpy, window, &wm_delete_atom, 1); // windowDeleteAtom
    jwindow = (*env)->NewGlobalRef(env, obj);
    setJavaWindowProperty(env, dpy, window, javaObjectAtom, jwindow);

    NewtWindows_setNormalWindowEWMH(dpy, window);
    NewtWindows_setDecorations(dpy, window, TST_FLAG_IS_UNDECORATED(flags) ? False : True );

    // since native creation happens at setVisible(true) .. 
    // we can pre-map the window here to be able to gather the insets and position.
    {
        XEvent event;
        int left, right, top, bottom;
        Bool userPos = 0<=x && 0<=y ;

        XMapWindow(dpy, window);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) window ); // wait to get proper insets values

        // send insets before visibility, allowing java code a proper sync point!
        NewtWindows_updateInsets(env, jwindow, dpy, window, &left, &right, &top, &bottom);
        (*env)->CallVoidMethod(env, jwindow, visibleChangedID, JNI_FALSE, JNI_TRUE);

        if(!userPos) {
            // get position from WM
            int dest_x, dest_y;
            Window child;
            XTranslateCoordinates(dpy, window, windowParent, 0, 0, &dest_x, &dest_y, &child);
            x = (int)dest_x; y = (int)dest_y;
        }
        DBG_PRINT("X11: [CreateWindow]: client: %d/%d %dx%d (is user-pos %d)\n", x, y, width, height, userPos);

        x -= left; // top-level
        y -= top;  // top-level
        if(0>x) { x = 0; }
        if(0>y) { y = 0; }
        DBG_PRINT("X11: [CreateWindow]: top-level: %d/%d\n", x, y);
        NewtWindows_setPosSize(dpy, window, x, y, width, height);

        if( TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            NewtWindows_setFullscreenEWMH(dpy, root, window, _NET_WM_ABOVE, True, True);
        }
    }

    DBG_PRINT( "X11: [CreateWindow] created window %p on display %p\n", (void*)window, dpy);
    return (jlong) window;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom, jlong windowDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    DBG_PRINT( "X11: CloseWindow START dpy %p, win %p\n", (void*)dpy, (void*)w);

    jwindow = getJavaWindowProperty(env, dpy, w, javaObjectAtom, True);
    if(NULL==jwindow) {
        NewtCommon_throwNewRuntimeException(env, "could not fetch Java Window object, bail out!");
        return;
    }
    if ( JNI_FALSE == (*env)->IsSameObject(env, jwindow, obj) ) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error .. Window global ref not the same!");
        return;
    }

    XSync(dpy, False);
    XSelectInput(dpy, w, 0);
    XUnmapWindow(dpy, w);
    XSync(dpy, False);

    // Drain all events related to this window ..
    Java_jogamp_newt_driver_x11_X11Display_DispatchMessages0(env, obj, display, javaObjectAtom, windowDeleteAtom);

    XDestroyWindow(dpy, w);
    XSync(dpy, False);

    (*env)->DeleteGlobalRef(env, jwindow);

    DBG_PRINT( "X11: CloseWindow END\n");
}

#if 0
static Bool WaitForReparentNotify( Display *dpy, XEvent *event, XPointer arg ) {
    return (event->type == ReparentNotify) && (event->xreparent.window == (Window) arg);
}
#endif

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    reconfigureWindow0
 * Signature: (JIJJIIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong jdisplay, jint screen_index, jlong jparent, jlong jwindow, 
   jint x, jint y, jint width, jint height, jint flags)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    Window w = (Window)jwindow;
    Window root = RootWindow(dpy, screen_index);
    Window parent = (0!=jparent)?(Window)jparent:root;
    XEvent event;
    Bool isVisible = !TST_FLAG_CHANGE_VISIBILITY(flags) && TST_FLAG_IS_VISIBLE(flags) ;
    Bool tempInvisible = ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_PARENTING(flags) ) && isVisible ;
    int fsEWMHFlags = 0;
    if( TST_FLAG_CHANGE_FULLSCREEN(flags) ) {
        fsEWMHFlags |= _NET_WM_FULLSCREEN;
        if( TST_FLAG_IS_FULLSCREEN(flags) ) {
            fsEWMHFlags |= _NET_WM_ABOVE; // fs & above on
        } else if( !TST_FLAG_IS_ALWAYSONTOP(flags) ) {
            fsEWMHFlags |= _NET_WM_ABOVE; // fs & above off
        } /* else { } */                  // fs off, keep above
    } else if( TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) {
        fsEWMHFlags |= _NET_WM_ABOVE; // toggle above only
    }

    displayDispatchErrorHandlerEnable(1, env);

    DBG_PRINT( "X11: reconfigureWindow0 dpy %p, scrn %d, parent %p/%p, win %p, %d/%d %dx%d, parentChange %d, hasParent %d, decorationChange %d, undecorated %d, fullscreenChange %d, fullscreen %d, alwaysOnTopChange %d, alwaysOnTop %d, visibleChange %d, visible %d, tempInvisible %d, fsEWMHFlags %d\n",
        (void*)dpy, screen_index, (void*) jparent, (void*)parent, (void*)w,
        x, y, width, height, 
        TST_FLAG_CHANGE_PARENTING(flags),   TST_FLAG_HAS_PARENT(flags),
        TST_FLAG_CHANGE_DECORATION(flags),  TST_FLAG_IS_UNDECORATED(flags),
        TST_FLAG_CHANGE_FULLSCREEN(flags),  TST_FLAG_IS_FULLSCREEN(flags),
        TST_FLAG_CHANGE_ALWAYSONTOP(flags), TST_FLAG_IS_ALWAYSONTOP(flags),
        TST_FLAG_CHANGE_VISIBILITY(flags),  TST_FLAG_IS_VISIBLE(flags), tempInvisible, fsEWMHFlags);

    // FS Note: To toggle FS, utilizing the _NET_WM_STATE_FULLSCREEN WM state shall be enough.
    //          However, we have to consider other cases like reparenting and WM which don't support it.

    if( fsEWMHFlags && !TST_FLAG_CHANGE_PARENTING(flags) && isVisible &&
        ( TST_FLAG_CHANGE_FULLSCREEN(flags) || TST_FLAG_CHANGE_ALWAYSONTOP(flags) ) ) {
        Bool enable = TST_FLAG_CHANGE_FULLSCREEN(flags) ? TST_FLAG_IS_FULLSCREEN(flags) : TST_FLAG_IS_ALWAYSONTOP(flags) ;
        if( NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, enable) ) {
            displayDispatchErrorHandlerEnable(0, env);
            return;
        }
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE OFF\n");
        XUnmapWindow(dpy, w);
        XIfEvent( dpy, &event, WaitForUnmapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && !TST_FLAG_IS_FULLSCREEN(flags) ) || 
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && !TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // FS off
        NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, False);
    }

    if( TST_FLAG_CHANGE_PARENTING(flags) && !TST_FLAG_HAS_PARENT(flags) ) {
        // TOP: in -> out
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING in->out\n");
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        // XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        XSync(dpy, False);
    }

    if( TST_FLAG_CHANGE_DECORATION(flags) ) {
        DBG_PRINT( "X11: reconfigureWindow0 DECORATIONS %d\n", !TST_FLAG_IS_UNDECORATED(flags));
        NewtWindows_setDecorations (dpy, w, TST_FLAG_IS_UNDECORATED(flags) ? False : True);
    }

    DBG_PRINT( "X11: reconfigureWindow0 setPosSize %d/%d %dx%d\n", x, y, width, height);
    NewtWindows_setPosSize(dpy, w, x, y, width, height);

    if( TST_FLAG_CHANGE_PARENTING(flags) && TST_FLAG_HAS_PARENT(flags) ) {
        // CHILD: out -> in
        DBG_PRINT( "X11: reconfigureWindow0 PARENTING out->in\n");
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        // XIfEvent( dpy, &event, WaitForReparentNotify, (XPointer) w );
        XSync(dpy, False);
    }

    if( tempInvisible ) {
        DBG_PRINT( "X11: reconfigureWindow0 TEMP VISIBLE ON\n");
        XMapRaised(dpy, w);
        XIfEvent( dpy, &event, WaitForMapNotify, (XPointer) w );
        // no need to notify the java side .. just temp change
    }

    if( TST_FLAG_CHANGE_VISIBILITY(flags) ) {
        if( TST_FLAG_IS_VISIBLE(flags) ) {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE ON\n");
            XMapRaised(dpy, w);
        } else {
            DBG_PRINT( "X11: reconfigureWindow0 VISIBLE OFF\n");
            XUnmapWindow(dpy, w);
        }
        XSync(dpy, False);
    }

    if( fsEWMHFlags && ( ( TST_FLAG_CHANGE_FULLSCREEN(flags)  && TST_FLAG_IS_FULLSCREEN(flags) ) || 
                         ( TST_FLAG_CHANGE_ALWAYSONTOP(flags) && TST_FLAG_IS_ALWAYSONTOP(flags) ) ) ) { // FS on
        NewtWindows_setFullscreenEWMH(dpy, root, w, fsEWMHFlags, isVisible, True);
    }

    displayDispatchErrorHandlerEnable(0, env);

    DBG_PRINT( "X11: reconfigureWindow0 X\n");
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    requestFocus0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_requestFocus0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean force)
{
    NewtWindows_requestFocus ( env, obj, (Display *) (intptr_t) display, (Window)window, force ) ;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    getParentWindow0
 * Signature: (JJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Window_getParentWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    return (jlong) NewtWindows_getParent ((Display *) (intptr_t) display, (Window)window);
}

/*
 * Class:     Java_jogamp_newt_driver_x11_X11Window
 * Method:    setTitle0
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_setTitle0
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

