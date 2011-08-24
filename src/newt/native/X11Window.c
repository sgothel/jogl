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

// #define VERBOSE_ON 1

#include "X11Window.h"
#include "X11WindowX11Event.h"
#include "X11WindowXCBEvent.h"

#include <X11/extensions/Xrandr.h>

#include "jogamp_newt_driver_x11_X11Screen.h"
#include "jogamp_newt_driver_x11_X11Display.h"
#include "jogamp_newt_driver_x11_X11Window.h"

#include "MouseEvent.h"
#include "InputEvent.h"
#include "KeyEvent.h"
#include "WindowEvent.h"
#include "ScreenMode.h"

#include "NewtCommon.h"

#include "X11Window.h"

#ifdef VERBOSE_ON

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

    #define DUMP_VISUAL_INFO(a,b)

#endif

/**
 * Keycode
 */

#define IS_WITHIN(k,a,b) ((a)<=(k)&&(k)<=(b))

jint X11KeySym2NewtVKey(KeySym keySym) {
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

jint X11InputState2NewtModifiers(unsigned int xstate) {
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

jmethodID sizeChangedID = NULL;
jmethodID positionChangedID = NULL;
jmethodID focusChangedID = NULL;
jmethodID visibleChangedID = NULL;
jmethodID windowDestroyNotifyID = NULL;
jmethodID windowRepaintID = NULL;
jmethodID windowReparentedID = NULL;
jmethodID enqueueMouseEventID = NULL;
jmethodID sendMouseEventID = NULL;
jmethodID enqueueKeyEventID = NULL;
jmethodID sendKeyEventID = NULL;
jmethodID focusActionID = NULL;
jmethodID enqueueRequestFocusID = NULL;

jmethodID displayCompletedID = NULL;


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

static int X11WindowDisplayErrorHandler(Display *dpy, XErrorEvent *e)
{
    fprintf(stderr, "Warning: NEWT X11 Error: DisplayDispatch %p, Code 0x%X, errno %s\n", dpy, e->error_code, strerror(errno));
    
    if (e->error_code == BadAtom) {
        fprintf(stderr, "         BadAtom (%p): Atom probably already removed\n", (void*)e->resourceid);
    } else if (e->error_code == BadWindow) {
        fprintf(stderr, "         BadWindow (%p): Window probably already removed\n", (void*)e->resourceid);
    } else {
        JNIEnv *curEnv = NULL;
        JNIEnv *newEnv = NULL;
        int envRes ;
        const char * errStr = strerror(errno);

        fprintf(stderr, "Info: NEWT X11 Error: Display %p, Code 0x%X, errno %s\n", dpy, e->error_code, errStr);

        // retrieve this thread's JNIEnv curEnv - or detect it's detached
        envRes = (*jvmHandle)->GetEnv(jvmHandle, (void **) &curEnv, jvmVersion) ;
        if( JNI_EDETACHED == envRes ) {
            // detached thread - attach to JVM
            if( JNI_OK != ( envRes = (*jvmHandle)->AttachCurrentThread(jvmHandle, (void**) &newEnv, NULL) ) ) {
                fprintf(stderr, "NEWT X11 Error: can't attach thread: %d\n", envRes);
                return;
            }
            curEnv = newEnv;
        } else if( JNI_OK != envRes ) {
            // oops ..
            fprintf(stderr, "NEWT X11 Error: can't GetEnv: %d\n", envRes);
            return;
        }
        NewtCommon_throwNewRuntimeException(curEnv, "Info: NEWT X11 Error: Display %p, Code 0x%X, errno %s", 
                                            dpy, e->error_code, errStr);

        if( NULL != newEnv ) {
            // detached attached thread
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        }
    }

    return 0;
}

void X11WindowDisplayErrorHandlerEnable(int onoff, JNIEnv * env) {
    if(onoff) {
        if(NULL==origErrorHandler) {
            setupJVMVars(env);
            origErrorHandler = XSetErrorHandler(X11WindowDisplayErrorHandler);
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

    javaObjectAtom = (jlong) XInternAtom(dpy, "JOGL_JAVA_OBJECT", False);
    if(None==javaObjectAtom) {
        NewtCommon_throwNewRuntimeException(env, "could not create Atom JOGL_JAVA_OBJECT, bail out!");
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

jobject X11WindowGetJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning) {
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
        NewtCommon_throwNewRuntimeException(env, "fetched Atom JOGL_JAVA_OBJECT window is not a NEWT Window: javaWindow 0x%X !", jwindow);
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
    return res;
}
static Window NewtWindows_getRoot (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return root_return;
    }
    return 0;
}
static Window NewtWindows_getParent (Display *dpy, Window w) {
    Window root_return;
    Window parent_return;
    if( 0 != NewtWindows_getRootAndParent(dpy, w, &root_return, &parent_return) ) {
        return parent_return;
    }
    return 0;
}


static void NewtWindows_requestFocus (JNIEnv *env, jobject window, Display *dpy, Window w, jboolean force) {
    XWindowAttributes xwa;
    Window focus_return;
    int revert_to_return;

    XGetInputFocus(dpy, &focus_return, &revert_to_return);
    if( JNI_TRUE==force || focus_return!=w) {
        if(  JNI_TRUE==force || JNI_FALSE == (*env)->CallBooleanMethod(env, window, focusActionID) ) {
            XRaiseWindow(dpy, w);
            // Avoid 'BadMatch' errors from XSetInputFocus, ie if window is not viewable
            XGetWindowAttributes(dpy, w, &xwa);
            if(xwa.map_state == IsViewable) {
                XSetInputFocus(dpy, w, RevertToParent, CurrentTime);
            }
        }
    }
    XSync(dpy, False);
}

#define MWM_HINTS_DECORATIONS   (1L << 1)
#define PROP_MWM_HINTS_ELEMENTS 5

static void NewtWindows_setDecorations (Display *dpy, Window w, Bool decorated) {
    unsigned long mwmhints[PROP_MWM_HINTS_ELEMENTS] = { 0, 0, 0, 0, 0 }; // flags, functions, decorations, input_mode, status
    Atom _MOTIF_WM_HINTS_DECORATIONS = XInternAtom( dpy, "_MOTIF_WM_HINTS", False );
    Atom _NET_WM_WINDOW_TYPE = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE", False );
    Atom types[3]={0};
    int ntypes=0;
    if(True==decorated) {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    } else {
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_POPUP_MENU", False );
        types[ntypes++] = XInternAtom( dpy, "_NET_WM_WINDOW_TYPE_NORMAL", False );
    }

    mwmhints[0] = MWM_HINTS_DECORATIONS;
    mwmhints[2] = decorated ;

    XChangeProperty( dpy, w, _MOTIF_WM_HINTS_DECORATIONS, _MOTIF_WM_HINTS_DECORATIONS, 32, PropModeReplace, (unsigned char *)&mwmhints, PROP_MWM_HINTS_ELEMENTS);
    XChangeProperty( dpy, w, _NET_WM_WINDOW_TYPE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
}

#define _NET_WM_STATE_REMOVE 0
#define _NET_WM_STATE_ADD 1

static void NewtWindows_setFullscreen (Display *dpy, Window root, Window w, Bool fullscreen) {
    Atom _NET_WM_STATE = XInternAtom( dpy, "_NET_WM_STATE", False );
    Atom _NET_WM_STATE_ABOVE = XInternAtom( dpy, "_NET_WM_STATE_ABOVE", False );
    Atom _NET_WM_STATE_FULLSCREEN = XInternAtom( dpy, "_NET_WM_STATE_FULLSCREEN", False );
    
    Atom types[2]={0};
    int ntypes=0;

    types[ntypes++] = _NET_WM_STATE_FULLSCREEN;
    types[ntypes++] = _NET_WM_STATE_ABOVE;

    XEvent xev;
    memset ( &xev, 0, sizeof(xev) );
    
    xev.type = ClientMessage;
    xev.xclient.window = w;
    xev.xclient.message_type = _NET_WM_STATE;
    xev.xclient.format = 32;
        
    if(True==fullscreen) {
        xev.xclient.data.l[0] = _NET_WM_STATE_ADD;
        xev.xclient.data.l[1] = _NET_WM_STATE_FULLSCREEN;
        xev.xclient.data.l[2] = _NET_WM_STATE_ABOVE;
        xev.xclient.data.l[3] = 1; //source indication for normal applications
    } else {
        xev.xclient.data.l[0] = _NET_WM_STATE_REMOVE;
        xev.xclient.data.l[1] = _NET_WM_STATE_FULLSCREEN;
        xev.xclient.data.l[2] = _NET_WM_STATE_ABOVE;
        xev.xclient.data.l[3] = 1; //source indication for normal applications
    }

    XChangeProperty( dpy, w, _NET_WM_STATE, XA_ATOM, 32, PropModeReplace, (unsigned char *)&types, ntypes);
    XSync(dpy, False);
    XSendEvent (dpy, root, False, SubstructureRedirectMask | SubstructureNotifyMask, &xev );
}

#define USE_SENDIO_DIRECT 1

/*
 * Class:     jogamp_newt_driver_x11_X11Display
 * Method:    DispatchMessages
 * Signature: (JIJJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Display_DispatchMessages0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    // X11WindowX11EventPoll(env, obj, dpy, javaObjectAtom, wmDeleteAtom);
    X11WindowXCBEventPoll(env, obj, dpy, javaObjectAtom, wmDeleteAtom);
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
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(IIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(Z)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "()V");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(IIII)V");
    windowReparentedID = (*env)->GetMethodID(env, clazz, "windowReparented", "(J)V");
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZIIIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZIIIC)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    enqueueRequestFocusID = (*env)->GetMethodID(env, clazz, "enqueueRequestFocus", "(Z)V");
    focusActionID = (*env)->GetMethodID(env, clazz, "focusAction", "()Z");

    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowRepaintID == NULL ||
        windowReparentedID == NULL ||
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

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CreateWindow
 * Signature: (JJIJIIII)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_X11Window_CreateWindow0
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
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    X11WindowXCBSetEventQueueOwner(dpy);

    if(visualID<0) {
        NewtCommon_throwNewRuntimeException(env, "invalid VisualID ..");
        return 0;
    }

    XSync(dpy, False);

    scrn = ScreenOfDisplay(dpy, scrn_idx);
    if(0==windowParent) {
        windowParent = XRootWindowOfScreen(scrn);
    }
    if( XRootWindowOfScreen(scrn) != XRootWindow(dpy, scrn_idx) ) {
        NewtCommon_FatalError(env, "XRoot Malfunction: %p != %p"+XRootWindowOfScreen(scrn), XRootWindow(dpy, scrn_idx));
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
    xswa.override_redirect = False; // use the window manager, always
    xswa.border_pixel = 0;
    xswa.background_pixmap = None;
    xswa.backing_store=NotUseful; /* NotUseful, WhenMapped, Always */
    xswa.backing_planes=0;        /* planes to be preserved if possible */
    xswa.backing_pixel=0;         /* value to use in restoring planes */
    xswa.event_mask  = ButtonPressMask | ButtonReleaseMask | PointerMotionMask ;
    xswa.event_mask |= KeyPressMask | KeyReleaseMask ;
    xswa.event_mask |= FocusChangeMask | SubstructureNotifyMask | StructureNotifyMask | ExposureMask ;

    xswa.colormap = XCreateColormap(dpy,
                                    windowParent,
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
        NewtCommon_throwNewRuntimeException(env, "could not create Window, bail out!");
        return 0;
    }

    wm_delete_atom = (Atom)windowDeleteAtom;
    XSetWMProtocols(dpy, window, &wm_delete_atom, 1);

    setJavaWindowProperty(env, dpy, window, javaObjectAtom, (*env)->NewGlobalRef(env, obj));

    NewtWindows_setDecorations(dpy, window, ( JNI_TRUE == undecorated ) ? False : True );
    XSync(dpy, False);

    DBG_PRINT( "X11: [CreateWindow] created window %p on display %p\n", (void*)window, dpy);
    return (jlong) window;
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jlong javaObjectAtom, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    jobject jwindow;

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    DBG_PRINT( "X11: CloseWindow START dpy %p, win %p\n", (void*)dpy, (void*)w);

    jwindow = X11WindowGetJavaWindowProperty(env, dpy, w, javaObjectAtom, True);
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

    // Drain all events related to this window ..
    Java_jogamp_newt_driver_x11_X11Display_DispatchMessages0(env, obj, display, javaObjectAtom, wmDeleteAtom);

    XDestroyWindow(dpy, w);
    XSync(dpy, False);

    (*env)->DeleteGlobalRef(env, jwindow);

    DBG_PRINT( "X11: CloseWindow END\n");
}

static void NewtWindows_setPosSize(Display *dpy, Window w, jint x, jint y, jint width, jint height)
{
    if(width>0 && height>0 || x>=0 && y>=0) { // resize/position if requested
        XWindowChanges xwc;
        unsigned int mod_flags = ( (x>=0)?CWX:0 ) | ( (y>=0)?CWY:0 ) | 
                                 ( (width>0)?CWWidth:0 ) | ( (height>0)?CWHeight:0 ) ;
        DBG_PRINT( "X11: reconfigureWindow0 pos/size mod: 0x%X\n", mod_flags);
        memset(&xwc, 0, sizeof(XWindowChanges));
        xwc.x=x;
        xwc.y=y;
        xwc.width=width;
        xwc.height=height;
        XConfigureWindow(dpy, w, mod_flags, &xwc);
        XSync(dpy, False);
    }
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    setVisible0
 * Signature: (JJZIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_setVisible0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean visible, jint x, jint y, jint width, jint height)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    DBG_PRINT( "X11: setVisible0 vis %d\n", visible);

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    if(visible==JNI_TRUE) {
        XMapRaised(dpy, w);
    } else {
        XUnmapWindow(dpy, w);
    }
    XSync(dpy, False);

    NewtWindows_setPosSize(dpy, w, x, y, width, height);
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    reconfigureWindow0
 * Signature: (JIJJIIIIZZII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_reconfigureWindow0
  (JNIEnv *env, jobject obj, jlong jdisplay, jint screen_index, jlong jparent, jlong jwindow, 
   jint x, jint y, jint width, jint height, jboolean isVisible, jboolean parentChange, jint fullscreenChange, jint decorationChange)
{
    Display * dpy = (Display *) (intptr_t) jdisplay;
    Screen * scrn = ScreenOfDisplay(dpy, (int)screen_index);
    Window w = (Window)jwindow;
    Window root = XRootWindowOfScreen(scrn);
    Window parent = (0!=jparent)?(Window)jparent:root;
    Window topParentParent;
    Window topParentWindow;
    Bool moveIntoParent = False;

    X11WindowDisplayErrorHandlerEnable(1, env);

    topParentParent = NewtWindows_getParent (dpy, parent);
    topParentWindow = NewtWindows_getParent (dpy, w);

    DBG_PRINT( "X11: reconfigureWindow0 dpy %p, scrn %d/%p, parent %p/%p (top %p), win %p (top %p), %d/%d %dx%d visible %d, parentChange %d, fullscreenChange %d, decorationChange %d\n", 
        (void*)dpy, screen_index, (void*)scrn, (void*) jparent, (void*)parent, (void*) topParentParent, (void*)w, (void*)topParentWindow,
        x, y, width, height, isVisible, parentChange, fullscreenChange, decorationChange);

    if(parentChange && JNI_TRUE == isVisible) { // unmap window if visible, reduce X11 internal signaling (WM unmap)
        XUnmapWindow(dpy, w);
        XSync(dpy, False);
    }

    if(0 > fullscreenChange ) { // FS off
        NewtWindows_setFullscreen(dpy, root, w, False );
        XSync(dpy, False);
    }

    if(parentChange) {
        if(0 != jparent) { // move into parent ..
            moveIntoParent = True;
            NewtWindows_setDecorations (dpy, w, False);
            XSync(dpy, False);
        }
        XReparentWindow( dpy, w, parent, x, y ); // actual reparent call
        XSync(dpy, False);
    }

    if(!moveIntoParent && 0!=decorationChange) {
        NewtWindows_setDecorations (dpy, w, (0 < decorationChange) ? True : False);
        XSync(dpy, False);
    }

    NewtWindows_setPosSize(dpy, w, x, y, width, height);

    if(0 < fullscreenChange ) { // FS on
        NewtWindows_setFullscreen(dpy, root, w, True );
        XSync(dpy, False);
    }
    
    if(parentChange && JNI_TRUE == isVisible) { // map window 
        XMapRaised(dpy, w);
        XSync(dpy, False);
    }

    X11WindowDisplayErrorHandlerEnable(0, env);

    DBG_PRINT( "X11: reconfigureWindow0 X\n");
}

/*
 * Class:     jogamp_newt_driver_x11_X11Window
 * Method:    requestFocus0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_X11Window_requestFocus0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jboolean force)
{
    NewtWindows_requestFocus ( env, obj, (Display *) (intptr_t) display, (Window)window, force ) ;
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

