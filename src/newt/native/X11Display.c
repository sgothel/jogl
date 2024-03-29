/**
 * Copyright 2011-2023 JogAmp Community. All rights reserved.
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

#include "X11Common.h"

#include <X11/Xcursor/Xcursor.h>

#include <X11/XKBlib.h>

jclass X11NewtWindowClazz = NULL;
jmethodID insetsChangedID = NULL;
jmethodID visibleChangedID = NULL;
jmethodID insetsVisibleChangedID = NULL;

static const char * const ClazzNameX11NewtWindow = "jogamp/newt/driver/x11/WindowDriver";

static jmethodID displayCompletedID = NULL;
static jmethodID isNativeValidID = NULL;
static jmethodID sendRRScreenChangeNotifyID = NULL;

static jmethodID getCurrentThreadNameID = NULL;
static jmethodID dumpStackID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusVisibleChangedID = NULL;
static jmethodID reparentNotifyID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID sendMouseEventRequestFocusID = NULL;
static jmethodID visibleChangedWindowRepaintID = NULL;
static jmethodID visibleChangedSendMouseEventID = NULL;
static jmethodID sizePosMaxInsetsVisibleChangedID = NULL;
static jmethodID sendTouchScreenEventID = NULL;

/**
 * Keycode
 */

// #define DEBUG_KEYS 1

#define IS_WITHIN(k,a,b) ((a)<=(k)&&(k)<=(b))

/**
 * QT Reference:
 *   <http://qt.gitorious.org/qt/qt/blobs/4.7/src/gui/kernel/qkeymapper_x11.cpp#line879>
 */
static short X11KeySym2NewtVKey(KeySym keySym) {
    if( IS_WITHIN( keySym, XK_a, XK_z ) ) {
        return ( keySym - XK_a ) + J_VK_A ;
    }
    if( IS_WITHIN( keySym, XK_0, XK_9 ) ) {
        return ( keySym - XK_0 ) + J_VK_0 ;
    }
    if( IS_WITHIN( keySym, XK_KP_0, XK_KP_9 ) ) {
        return ( keySym - XK_KP_0 ) + J_VK_NUMPAD0 ;
    }
    if( IS_WITHIN( keySym, XK_F1, XK_F12 ) ) {
        return ( keySym - XK_F1 ) + J_VK_F1 ;
    }

    switch(keySym) {
        case XK_Return:
        case XK_KP_Enter:
            return J_VK_ENTER;
        case XK_BackSpace:
            return J_VK_BACK_SPACE;
        case XK_Tab:
        case XK_KP_Tab:
        case XK_ISO_Left_Tab:
            return J_VK_TAB;
        case XK_Cancel:
            return J_VK_CANCEL;
        case XK_Shift_L:
        case XK_Shift_R:
            return J_VK_SHIFT;
        case XK_Control_L:
        case XK_Control_R:
            return J_VK_CONTROL;
        case XK_Alt_L:
            return J_VK_ALT;
        case XK_Alt_R:
        case XK_ISO_Level3_Shift:
            return J_VK_ALT_GRAPH;
        case XK_Super_L:
        case XK_Super_R:
            return J_VK_WINDOWS;
        case XK_Menu:
            return J_VK_CONTEXT_MENU;
        case XK_Pause:
            return J_VK_PAUSE;
        case XK_Caps_Lock:
            return J_VK_CAPS_LOCK;
        case XK_Escape:
            return J_VK_ESCAPE;
        case XK_space:
        case XK_KP_Space:
            return J_VK_SPACE;
        case XK_Page_Up:
        case XK_KP_Page_Up:
            return J_VK_PAGE_UP;
        case XK_Page_Down:
        case XK_KP_Page_Down:
            return J_VK_PAGE_DOWN;
        case XK_End:
        case XK_KP_End:
            return J_VK_END;
        case XK_Begin:
            return J_VK_BEGIN;
        case XK_KP_Begin: // NumPad 5 - equal behavior w/ QT/Windows
            return J_VK_CLEAR;
        case XK_Home:
        case XK_KP_Home:
            return J_VK_HOME;
        case XK_Left:
        case XK_KP_Left:
            return J_VK_LEFT;
        case XK_Up:
        case XK_KP_Up:
            return J_VK_UP;
        case XK_Right:
        case XK_KP_Right:
            return J_VK_RIGHT;
        case XK_Down:
        case XK_KP_Down:
            return J_VK_DOWN;
        case XK_KP_Multiply:
            return J_VK_MULTIPLY;
        case XK_KP_Add:
            return J_VK_ADD;
        case XK_KP_Separator:
            return J_VK_SEPARATOR;
        case XK_KP_Subtract:
            return J_VK_SUBTRACT;
        case XK_KP_Decimal:
            return J_VK_DECIMAL;
        case XK_KP_Divide:
            return J_VK_DIVIDE;
        case XK_Clear: // equal behavior w/ QT
        case XK_Delete:
        case XK_KP_Delete:
            return J_VK_DELETE;
        case XK_Num_Lock:
            return J_VK_NUM_LOCK;
        case XK_Scroll_Lock:
            return J_VK_SCROLL_LOCK;
        case XK_Print:
            return J_VK_PRINTSCREEN;
        case XK_Insert:
        case XK_KP_Insert:
            return J_VK_INSERT;
        case XK_Help:
            return J_VK_HELP;
        case XK_grave:
            return J_VK_BACK_QUOTE;
        case XK_apostrophe:
            return J_VK_QUOTE;
    }
    return keySym;
}

#define ShiftCtrlModMask ( ShiftMask | ControlMask | Mod1Mask | Mod2Mask | Mod3Mask | Mod4Mask | Mod5Mask )

static jboolean altGraphDown = JNI_FALSE;

static jint X11InputState2NewtModifiers(unsigned int xstate, jshort javaVKey, jboolean keyDown) {
    jint modifiers = 0;
    if ( (ControlMask & xstate) != 0 || J_VK_CONTROL == javaVKey ) {
        modifiers |= EVENT_CTRL_MASK;
    }
    if ( (ShiftMask & xstate) != 0 || J_VK_SHIFT == javaVKey ) {
        modifiers |= EVENT_SHIFT_MASK;
    }
    if ( J_VK_ALT == javaVKey ) {
        altGraphDown = JNI_FALSE;
        modifiers |= EVENT_ALT_MASK;
    } else if ( (short)J_VK_ALT_GRAPH == javaVKey ) {
        altGraphDown = keyDown;
        modifiers |= EVENT_ALT_GRAPH_MASK;
    } else if ( (Mod1Mask & xstate) != 0 ) {
        // XK_Alt_L or XK_Alt_R
        modifiers |= altGraphDown ? EVENT_ALT_GRAPH_MASK : EVENT_ALT_MASK;
    }
    if ( (Button1Mask & xstate) != 0 ) {
        modifiers |= EVENT_BUTTON1_MASK;
    }
    if ( (Button2Mask & xstate) != 0 ) {
        modifiers |= EVENT_BUTTON2_MASK;
    }
    if ( (Button3Mask & xstate) != 0 ) {
        modifiers |= EVENT_BUTTON3_MASK;
    }

    return modifiers;
}


/**
 * Keycode
 */

/*
 * Class:     jogamp_newt_driver_x11_DisplayDriver
 * Method:    initIDs0
 * Signature: (Z)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_initIDs0
  (JNIEnv *env, jclass clazz, jboolean debug)
{
    jclass c;

    NewtCommon_init(env);

    if(NULL==X11NewtWindowClazz) {
        c = (*env)->FindClass(env, ClazzNameX11NewtWindow);
        if(NULL==c) {
            NewtCommon_FatalError(env, "NEWT X11Display: can't find %s", ClazzNameX11NewtWindow);
        }
        X11NewtWindowClazz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==X11NewtWindowClazz) {
            NewtCommon_FatalError(env, "NEWT X11Display: can't use %s", ClazzNameX11NewtWindow);
        }
    }

    // displayCompletedID = (*env)->GetMethodID(env, clazz, "displayCompleted", "(JJJIII)V"); // Variant using XKB
    displayCompletedID = (*env)->GetMethodID(env, clazz, "displayCompleted", "(JJIII)V");
    isNativeValidID = (*env)->GetMethodID(env, clazz, "isNativeValidAsync", "()Z");
    sendRRScreenChangeNotifyID = (*env)->GetMethodID(env, clazz, "sendRRScreenChangeNotify", "(J)Z");
    getCurrentThreadNameID = (*env)->GetStaticMethodID(env, X11NewtWindowClazz, "getCurrentThreadName", "()Ljava/lang/String;");
    dumpStackID = (*env)->GetStaticMethodID(env, X11NewtWindowClazz, "dumpStack", "()V");
    insetsChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "insetsChanged", "(ZIIII)Z");
    sizeChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sizeChanged", "(ZZIIZ)Z");
    positionChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "positionChanged", "(ZZII)Z");
    focusVisibleChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "focusVisibleChanged", "(ZII)Z");
    visibleChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "visibleChanged", "(Z)Z");
    insetsVisibleChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "insetsVisibleChanged", "(ZIIIII)Z");
    sizePosMaxInsetsVisibleChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sizePosMaxInsetsVisibleChanged", "(ZZIIIIIIIIIIIZ)Z");
    reparentNotifyID = (*env)->GetMethodID(env, X11NewtWindowClazz, "reparentNotify", "(J)Z");
    windowDestroyNotifyID = (*env)->GetMethodID(env, X11NewtWindowClazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, X11NewtWindowClazz, "windowRepaint", "(ZIIII)Z");
    visibleChangedWindowRepaintID = (*env)->GetMethodID(env, X11NewtWindowClazz, "visibleChangedWindowRepaint", "(ZIIIII)Z");
    sendMouseEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendMouseEvent", "(SIIISF)Z");
    sendMouseEventRequestFocusID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendMouseEventRequestFocus", "(SIIISF)Z");
    visibleChangedSendMouseEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "visibleChangedSendMouseEvent", "(ZISIIISF)Z");
    sendTouchScreenEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendTouchScreenEvent", "(SII[S[I[I[FF)Z");
    sendKeyEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendKeyEvent", "(SISSCLjava/lang/String;)Z");

    if (displayCompletedID == NULL ||
        isNativeValidID == NULL ||
        sendRRScreenChangeNotifyID == NULL ||
        getCurrentThreadNameID == NULL ||
        dumpStackID == NULL ||
        insetsChangedID == NULL ||
        sizeChangedID == NULL ||
        positionChangedID == NULL ||
        focusVisibleChangedID == NULL ||
        visibleChangedID == NULL ||
        insetsVisibleChangedID == NULL ||
        sizePosMaxInsetsVisibleChangedID == NULL ||
        reparentNotifyID == NULL ||
        windowDestroyNotifyID == NULL ||
        windowRepaintID == NULL ||
        visibleChangedWindowRepaintID == NULL ||
        sendMouseEventID == NULL ||
        sendMouseEventRequestFocusID == NULL ||
        visibleChangedSendMouseEventID == NULL ||
        sendTouchScreenEventID == NULL ||
        sendKeyEventID == NULL) {
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_x11_DisplayDriver
 * Method:    CompleteDisplay0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_CompleteDisplay0
  (JNIEnv *env, jobject obj, jlong display)
{
    Display * dpy = (Display *)(intptr_t)display;
    jlong javaObjectAtom;
    jlong windowDeleteAtom;
    // jlong kbdHandle; // XKB disabled for now

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
    // kbdHandle = (jlong) (intptr_t) XkbGetKeyboard(dpy, XkbAllComponentsMask, XkbUseCoreKbd); // XKB disabled for now

    int randr_event_base, randr_error_base;
    XRRQueryExtension(dpy, &randr_event_base, &randr_error_base);

    int xi_opcode = -1, event, error;
    XQueryExtension(dpy, "XInputExtension", &xi_opcode, &event, &error);

    DBG_PRINT("X11: X11Display_completeDisplay dpy %p\n", dpy);

    (*env)->CallVoidMethod(env, obj, displayCompletedID, javaObjectAtom, windowDeleteAtom /*, kbdHandle*/, // XKB disabled for now
                                     randr_event_base, randr_error_base, xi_opcode);
    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.CompleteDisplay0: Exception occured at displayCompleted(..)");
}

/*
 * Class:     jogamp_newt_driver_x11_DisplayDriver
 * Method:    DisplayRelease0
 * Signature: (JJJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_DisplayRelease0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong windowDeleteAtom /*, jlong kbdHandle*/)
{
    Display * dpy = (Display *)(intptr_t)display;
    Atom wm_javaobject_atom = (Atom)javaObjectAtom;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    // XkbDescPtr kbdDesc = (XkbDescPtr)(intptr_t)kbdHandle; // XKB disabled for now

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    // nothing to do to free the atoms !
    (void) wm_javaobject_atom;
    (void) wm_delete_atom;

    // XkbFreeKeyboard(kbdDesc, XkbAllNamesMask, True); // XKB disabled for now

    XSync(dpy, True); // discard all pending events
    DBG_PRINT("X11: X11Display_DisplayRelease dpy %p\n", dpy);
}

static int NewtWindows_updateVisibility(JNIEnv *env, Display *dpy, JavaWindow *jw, uint32_t netWMState, const char *dbgs) {
    int visibleChange;
    if( jw->isMapped && 0 != ( _MASK_NET_WM_STATE_HIDDEN & jw->supportedAtoms ) ) {
        if( 0 != ( _MASK_NET_WM_STATE_HIDDEN & netWMState ) ) {
            visibleChange = 0;
        } else {
            visibleChange = 1;
        }
    } else {
        visibleChange = -1;
    }
    #ifdef VERBOSE_ON
        XWindowAttributes xwa;
        memset(&xwa, 0, sizeof(XWindowAttributes));
        XGetWindowAttributes(dpy, jw->window, &xwa);

        // map_state: IsUnmapped(0), IsUnviewable(1), IsViewable(2)
        DBG_PRINT( "X11: event . %s call %p - isMapped %d, visibleChanged %d, map_state %d\n", 
            dbgs, (void*)jw->window, jw->isMapped, visibleChange, xwa.map_state);
    #endif
    return visibleChange;
}

static jboolean sendTouchScreenEvent(JNIEnv *env, JavaWindow *jw,
                                     short eventType,  // MouseEvent.EVENT_MOUSE_PRESSED, MouseEvent.EVENT_MOUSE_RELEASED, MouseEvent.EVENT_MOUSE_MOVED
                                     int modifiers, // 0!
                                     int actionId) //  index of multiple-pointer arrays representing the pointer which triggered the event
{
    jshort pointerNames[XI_TOUCHCOORD_COUNT];
    jint x[XI_TOUCHCOORD_COUNT];
    jint y[XI_TOUCHCOORD_COUNT];
    jfloat pressure[] =  {1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f, 1.0f};
    jint actionIdx = -1;
    int i, cnt;

    for(i = 0, cnt = 0; i < XI_TOUCHCOORD_COUNT; i++) {
        if( -1 != jw->xiTouchCoords[i].id ) {
            x[cnt] = jw->xiTouchCoords[i].x;
            y[cnt] = jw->xiTouchCoords[i].y;
            pointerNames[cnt] = (jshort)jw->xiTouchCoords[i].id;
            if (jw->xiTouchCoords[i].id == actionId) {
                actionIdx = cnt;
            }
            cnt++;
        }
    }
    if( 0 > actionIdx ) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error: XI event (window %p) actionId %d not found in %d xiTouchCoords", 
            (void*)jw->window, actionId, cnt);
    }
    DBG_PRINT( "X11: XI event - sendTouchScreenEvent: Window %p, action-touchid[%d] %d of %d ptr: %d/%d\n",
        (void*)jw->window, actionIdx, actionId, cnt, x[actionIdx], y[actionIdx]);

    jshortArray jNames = (*env)->NewShortArray(env, cnt);
    if (jNames == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate short array (names) of size %d", cnt);
    }
    (*env)->SetShortArrayRegion(env, jNames, 0, cnt, pointerNames);

    jintArray jX = (*env)->NewIntArray(env, cnt);
    if (jX == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (x) of size %d", cnt);
    }
    (*env)->SetIntArrayRegion(env, jX, 0, cnt, x);

    jintArray jY = (*env)->NewIntArray(env, cnt);
    if (jY == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (y) of size %d", cnt);
    }
    (*env)->SetIntArrayRegion(env, jY, 0, cnt, y);

    jfloatArray jPressure = (*env)->NewFloatArray(env, cnt);
    if (jPressure == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate float array (pressure) of size %d", cnt);
    }
    (*env)->SetFloatArrayRegion(env, jPressure, 0, cnt, pressure);

    jboolean res = (*env)->CallBooleanMethod(env, jw->jwindow, sendTouchScreenEventID,
                                             (jshort)eventType, (jint)modifiers, (jint)actionIdx,
                                             jNames, jX, jY, jPressure, (jfloat)1.0f);
    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: XI: Exception occured at sendTouchScreenEvent(..)");
    return res;
}

/*
 * Class:     jogamp_newt_driver_x11_DisplayDriver
 * Method:    DispatchMessages0
 * Signature: (JJJIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_DispatchMessages0
  (JNIEnv *env, jobject obj, jlong display, jlong javaObjectAtom, jlong windowDeleteAtom /*, jlong kbdHandle*/,
                             jint randr_event_base, jint randr_error_base, jint xi_opcode)
{
    Display * dpy = (Display *) (intptr_t) display;
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    // XkbDescPtr kbdDesc = (XkbDescPtr)(intptr_t)kbdHandle; // XKB disabled for now
    int num_events = 100;
    int autoRepeatModifiers = 0;

    if ( NULL == dpy ) {
        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 0\n", dpy); 
        return;
    }

    /** XKB disabled for now
    if( NULL == kbdDesc) {
        NewtCommon_throwNewRuntimeException(env, "NULL kbd handle, bail out!");
        return;
    } */

    // Periodically take a break
    while( num_events > 0 ) {
        JavaWindow *jw = NULL;
        XEvent evt;
        KeySym keySym = 0;
        KeyCode keyCode = 0;
        jshort javaVKeyUS = 0;
        jshort javaVKeyNN = 0;
        jint modifiers = 0;
        uint16_t keyChar = 0;
        jstring keyString = NULL;
        char text[255];

        // Don't use explicit Java callback, we simply break loop if regular Java callback on event returns JNI_FALSE
        #if 0
            if( JNIInvalidRefType == (*env)->GetObjectRefType(env, obj) ) {
                DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 1, invalidated DisplayDriver Object %p\n", dpy, obj); 
                return;
            }  
            if( JNI_FALSE == (*env)->CallBooleanMethod(env, obj, isNativeValidID) ) {
                DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 2, closed DisplayDriver %p\n", dpy, obj); 
                return;
            }
            NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: Exception occured at isNativeValid()");
        #endif

        // XEventsQueued(dpy, X):
        //   QueuedAlready    == XQLength(): No I/O Flush or system call  doesn't work on some cards (eg ATI) ?) 
        //   QueuedAfterFlush == XPending(): I/O Flush only if no already queued events are available
        //   QueuedAfterReading            : QueuedAlready + if queue==0, attempt to read more ..
        if ( 0 >= XEventsQueued(dpy, QueuedAfterFlush) ) {
            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 3\n", dpy); 
            return;
        }

        XNextEvent(dpy, &evt);
        num_events--;

        if(dpy!=evt.xany.display) {
            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 4, wrong display\n", dpy); 
            NewtCommon_throwNewRuntimeException(env, "wrong display, bail out!");
            return ;
        }

        if( randr_event_base > 0 && RRScreenChangeNotify == ( evt.type - randr_event_base ) ) {
            DBG_PRINT( "X11: DispatchMessages dpy %p, Event RRScreenChangeNotify %p\n", (void*)dpy, (void*)&evt);
            if( JNI_FALSE == (*env)->CallBooleanMethod(env, obj, sendRRScreenChangeNotifyID, (jlong)(intptr_t)&evt) ) {
                DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, RRScreen\n", dpy); 
                num_events = 0; // end loop in case of destroyed display
            }
            NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: RR: Exception occured at sendRRScreenChangeNotify(..)");
            continue; // next event
        }

        if( 0==evt.xany.window ) {
            DBG_PRINT( "X11: DispatchMessages dpy %p, Event %d - Window NULL, ignoring\n", (void*)dpy, (int)evt.type);
            continue; // next event
        }

        // Valid registered XI Event w/ cookie data (incl. the event Window name)?
        // Here: https://www.x.org/wiki/Development/Documentation/Multitouch/
        XGenericEventCookie *evtCookie = &evt.xcookie; // hacks: https://keithp.com/blogs/Cursor_tracking/
        int isXiEvent = GenericEvent == evtCookie->type && xi_opcode == evtCookie->extension && XGetEventData(dpy, evtCookie);
        XIDeviceEvent *xiDevEv;
        int xiDetail;
        Window windowPointer;
        if( !isXiEvent ) {
            xiDevEv = NULL;
            xiDetail = 0;
            windowPointer = evt.xany.window;
            DBG_PRINT( "X11: DispatchMessages dpy %p, win %p, Event %d\n", (void*)dpy, (void*)windowPointer, (int)evt.type);
        } else {
            xiDevEv = evtCookie->data;
            xiDetail = xiDevEv->detail; 
            windowPointer = xiDevEv->event;
        }

        jw = getJavaWindowProperty(env, dpy, windowPointer, javaObjectAtom,
        #ifdef VERBOSE_ON
                True
        #else
                False
        #endif
            );

        if(NULL==jw) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d for X11 window %p\n", (void*)dpy, evt.type, (void*)windowPointer);
            continue; // next event
        }

        if ( isXiEvent ) {
            if( xiDevEv->deviceid != jw->xiTouchDeviceId) {
                DBG_PRINT( "X11: XI event - dpy %p, win %p, Event %d: DeviceID not matching: Window %d, this %d\n", (void*)dpy, (void*)windowPointer, (int)evt.type, xiDevEv->deviceid, jw->xiTouchDeviceId);
            } else {
                int i;
                switch (xiDevEv->evtype) {
                    case XI_TouchBegin:
                        for (i = 0; i < XI_TOUCHCOORD_COUNT; i++) {
                            if (jw->xiTouchCoords[i].id == -1) {
                                jw->xiTouchCoords[i].id = xiDevEv->detail % 32767;
                                jw->xiTouchCoords[i].x = xiDevEv->event_x;
                                jw->xiTouchCoords[i].y = xiDevEv->event_y;
                                break;
                            }
                        }
                        DBG_PRINT( "X11: XI event - XI_TouchBegin Window %p, devid %d, touchid[%d] %d @ %d/%d\n", (void*)windowPointer, xiDevEv->deviceid, 
                            i, jw->xiTouchCoords[i].id, jw->xiTouchCoords[i].x, jw->xiTouchCoords[i].y);
                        XFreeEventData(dpy, evtCookie);
                        if( JNI_FALSE == sendTouchScreenEvent(env, jw, EVENT_MOUSE_PRESSED, 0, xiDetail % 32767) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, touch-begin\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        break;

                    case XI_TouchUpdate:
                        for (i = 0; i < XI_TOUCHCOORD_COUNT; i++) {
                            if (jw->xiTouchCoords[i].id == xiDevEv->detail  % 32767) {
                                jw->xiTouchCoords[i].x = xiDevEv->event_x;
                                jw->xiTouchCoords[i].y = xiDevEv->event_y;
                                break;
                            }
                        }
                        DBG_PRINT( "X11: XI event - XI_TouchUpdate: Window %p, devid %d, touchid[%d] %d @ %d/%d\n", (void*)windowPointer, xiDevEv->deviceid, 
                            i, jw->xiTouchCoords[i].id, jw->xiTouchCoords[i].x, jw->xiTouchCoords[i].y);
                        XFreeEventData(dpy, evtCookie);
                        if( JNI_FALSE == sendTouchScreenEvent(env, jw, EVENT_MOUSE_MOVED, 0, xiDetail % 32767) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, touch-update\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        break;

                    case XI_TouchEnd:
                        for (i = 0; i < XI_TOUCHCOORD_COUNT; i++) {
                            if (jw->xiTouchCoords[i].id == xiDevEv->detail % 32767) {
                                break;
                            }
                        }
                        DBG_PRINT( "X11: XI event - XI_TouchEnd: Window %p, devid %d, touchid[%d] %d @ %d/%d\n", (void*)windowPointer, xiDevEv->deviceid, 
                            i, jw->xiTouchCoords[i].id, jw->xiTouchCoords[i].x, jw->xiTouchCoords[i].y);
                        XFreeEventData(dpy, evtCookie);
                        if( JNI_FALSE == sendTouchScreenEvent(env, jw, EVENT_MOUSE_RELEASED, 0, xiDetail % 32767) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, touch-end\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        if ( i < XI_TOUCHCOORD_COUNT ) {
                            jw->xiTouchCoords[i].id = -1;
                        }
                        break;

                    default:
                        XFreeEventData(dpy, evtCookie);
                }
            }
            continue; // next event, skip evt.type handling below
        }
        
        switch(evt.type) {
            case KeyRelease:
                if (XEventsQueued(dpy, QueuedAfterReading)) {
                  XEvent nevt;
                  XPeekEvent(dpy, &nevt);

                  if (nevt.type == KeyPress && nevt.xkey.time == evt.xkey.time &&
                      nevt.xkey.keycode == evt.xkey.keycode)
                  {
                    autoRepeatModifiers |= EVENT_AUTOREPEAT_MASK;
                  } else {
                    autoRepeatModifiers &= ~EVENT_AUTOREPEAT_MASK;
                  }
                } else {
                    autoRepeatModifiers &= ~EVENT_AUTOREPEAT_MASK;
                }
                // fall through intended
            case KeyPress: {
                    KeySym shiftedKeySym; // layout depending keySym w/ SHIFT
                    KeySym unShiftedKeySym; // layout depending keySym w/o SHIFT
                    unsigned int xkey_state = evt.xkey.state;

                    keyCode = evt.xkey.keycode;

                    // Layout depending keySym w/o SHIFT,
                    // using fixed group 0 (US default layout)
                    //
                    // unsigned int mods_rtrn = 0;
                    // Bool res = XkbTranslateKeyCode (kbdDesc, keyCode, 0, &mods_rtrn, &keySym); // XKB disabled for now
                    // if( !res ) {
                        keySym = XkbKeycodeToKeysym(dpy, keyCode, 0 /* group */, 0 /* shift level */);
                    // }

                    text[0] = 0; text[1] = 0; text[2] = 0;
                    int charCount = XLookupString(&evt.xkey, text, 2, &shiftedKeySym, NULL);
                    if( 1 == charCount ) {
                        keyChar = 0x00FF & (uint16_t) (text[0]);
                    } else if( 2 == charCount ) {
                        // Example: UTF-16: 00DF, UTF-8: c3 9f, LATIN SMALL LETTER SHARP S
                        keyChar = ( 0x00FF & (uint16_t)(text[0]) ) << 8 | ( 0x00FF & (uint16_t)(text[1]) ); // UTF-16BE
                        keyString = (*env)->NewStringUTF(env, text);
                    }

                    #ifdef DEBUG_KEYS
                    fprintf(stderr, "NEWT X11 Key.0: keyCode 0x%X keySym 0x%X, (shifted: 0x%X)\n",
                        (int)keyCode, (int)keySym, (int) shiftedKeySym);
                    #endif
                    if( IS_WITHIN( shiftedKeySym, XK_KP_Space, XK_KP_9 ) ) {
                        // Use modded keySym for keypad for US and NN
                        keySym = shiftedKeySym;
                        unShiftedKeySym = shiftedKeySym;
                    } else if( 0 == keyChar ) {
                        // Use keyCode's keySym for dead-key (aka modifiers, etc)
                        unShiftedKeySym = keySym;
                    } else if( 0 == ( evt.xkey.state & ShiftCtrlModMask ) ) {
                        // Use non modded keySym
                        unShiftedKeySym = shiftedKeySym;
                    } else {
                        evt.xkey.state = evt.xkey.state & ~ShiftCtrlModMask; // clear shift, ctrl and Mod*
                        XLookupString(&evt.xkey, text, 0, &unShiftedKeySym, NULL);
                        // unShiftedKeySym = XLookupKeysym(&evt.xkey, 0 /* index ? */);
                    }

                    javaVKeyNN = X11KeySym2NewtVKey(unShiftedKeySym);
                    javaVKeyUS = X11KeySym2NewtVKey(keySym);
                    modifiers |= X11InputState2NewtModifiers(xkey_state, javaVKeyNN, evt.type == KeyPress) | autoRepeatModifiers;

                    #ifdef DEBUG_KEYS
                    fprintf(stderr, "NEWT X11 Key.X: keyCode 0x%X keySym 0x%X, (0x%X, shifted: 0x%X), keyChar '%c' 0x%X %d, javaVKey[US 0x%X, NN 0x%X], xstate 0x%X %u, jmods 0x%X\n",
                        (int)keyCode, (int)keySym, (int) unShiftedKeySym, (int)shiftedKeySym, keyChar, keyChar, charCount,
                        (int)javaVKeyUS, (int)javaVKeyNN,
                        (int)xkey_state, (int)xkey_state, (int)modifiers);
                    #endif
                }
                break;

            case ButtonPress:
            case ButtonRelease:
            case MotionNotify:
                modifiers |= X11InputState2NewtModifiers(evt.xbutton.state, 0, JNI_FALSE);
                break;

            default:
                break;
        }

        switch(evt.type) {
            case ButtonPress:
                if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sendMouseEventRequestFocusID, (jshort) EVENT_MOUSE_PRESSED, 
                                                           modifiers,
                                                           (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jshort) evt.xbutton.button, 0.0f /*rotation*/) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, press-mouseEvent\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                }
                NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: ButtonPress: Exception occured at sendMouseEventRequestFocus(..)");
                break;
            case ButtonRelease:
                if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sendMouseEventID, (jshort) EVENT_MOUSE_RELEASED, 
                                                           modifiers,
                                                           (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jshort) evt.xbutton.button, 0.0f /*rotation*/) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, release-mouseEvent\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                }
                NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: ButtonRelease: Exception occured at sendMouseEvent(..)");
                break;
            case MotionNotify:
                if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sendMouseEventID, (jshort) EVENT_MOUSE_MOVED, 
                                                           modifiers,
                                                           (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jshort) 0, 0.0f /*rotation*/) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, motion-mouseEvent\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                }
                NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: MotionNotify: Exception occured at sendMouseEvent(..)");
                break;
            case EnterNotify:
                DBG_PRINT( "X11: event . EnterNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                {
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "EnterNotify");
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedSendMouseEventID, JNI_FALSE, (jint)visibleChange, 
                                                              (jshort) EVENT_MOUSE_ENTERED, modifiers,
                                                              (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jshort) 0, 0.0f /*rotation*/) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, enter-mouseEvent\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: EnterNotify: Exception occured at visibleChangedSendMouseEvent(..)");
                }
                break;
            case LeaveNotify:
                DBG_PRINT( "X11: event . LeaveNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                {
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "LeaveNotify");
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedSendMouseEventID, JNI_FALSE, (jint)visibleChange, 
                                                               (jshort) EVENT_MOUSE_EXITED, modifiers,
                                                               (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jshort) 0, 0.0f /*rotation*/) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, leave-mouseEvent\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: LeaveNotify: Exception occured at visibleChangedSendMouseEvent(..)");
                }
                break;
            case MappingNotify:
                DBG_PRINT( "X11: event . MappingNotify call %p type %d\n", (void*)evt.xmapping.window, evt.xmapping.type);
                XRefreshKeyboardMapping(&evt.xmapping);
                break;
            case KeyPress:
                if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sendKeyEventID, (jshort) EVENT_KEY_PRESSED, 
                                                           modifiers, javaVKeyUS, javaVKeyNN, (jchar) keyChar, keyString) ) {
                    DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, press-keyEvent\n", dpy); 
                    num_events = 0; // end loop in case of destroyed display
                }
                NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: KeyPress: Exception occured at sendKeyEvent(..)");
                break;
            case KeyRelease:
                if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sendKeyEventID, (jshort) EVENT_KEY_RELEASED, 
                                                           modifiers, javaVKeyUS, javaVKeyNN, (jchar) keyChar, keyString) ) {
                    DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, release-keyEvent\n", dpy); 
                    num_events = 0; // end loop in case of destroyed display
                }
                NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: KeyRelease: Exception occured at sendKeyEvent(..)");
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
                    // insets: negative values are ignored
                    int x_pos = evt.xconfigure.x;
                    int y_pos = evt.xconfigure.y;
                    {
                        Window winRoot, winTopParent;
                        Bool translated = False;
                        if( 0 != NewtWindows_getRootAndParent(dpy, evt.xconfigure.window, &winRoot, &winTopParent) ) {
                            DBG_PRINT( "X11: event . ConfigureNotify call %p (root %p, top %p)\n", 
                                (void*)evt.xconfigure.window, (void*)winRoot, (void*)winTopParent);
                            int x_return=-1, y_return=-1;
                            Window winDest = ( winTopParent == jw->parentWindow ) ? winTopParent : winRoot;
                            Window child;
                            if( True == XTranslateCoordinates(dpy, evt.xconfigure.window, winDest, 0, 0, &x_return, &y_return, &child) ) {
                                DBG_PRINT( "X11: event . ConfigureNotify call %p of dest %p POS (Xtrans) %d/%d -> %d/%d (child %p)\n", 
                                    (void*)evt.xconfigure.window, winDest, x_pos, y_pos, x_return, y_return, child);
                                x_pos = x_return;
                                y_pos = y_return;
                                translated = True;
                            }
                        }
                        if( False == translated ) {
                            DBG_PRINT( "X11: event . ConfigureNotify call %p POS (raw) %d/%d\n", 
                                (void*)evt.xconfigure.window, x_pos, y_pos);
                        }
                    }
                    int left=-1, right=-1, top=-1, bottom=-1;
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "ConfigureNotify");
                    NewtWindows_updateInsets(dpy, jw, False /* wait */, &left, &right, &top, &bottom);
                    Bool maxChanged = NewtWindows_updateMaximized(dpy, jw, netWMState);
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, sizePosMaxInsetsVisibleChangedID, JNI_FALSE, JNI_FALSE,
                                                               (jint) x_pos, (jint) y_pos,
                                                               (jint) evt.xconfigure.width, (jint) evt.xconfigure.height,
                                                               (jint)(maxChanged ? ( jw->maxHorz ? 1 : 0 ) : -1), 
                                                               (jint)(maxChanged ? ( jw->maxVert ? 1 : 0 ) : -1),
                                                               (jint)left, (jint)right, (jint)top, (jint)bottom,
                                                               (jint)visibleChange,
                                                               JNI_FALSE) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, sizePosMaxInsetsVis\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: ConfigureNotify: Exception occured at sizePosMaxInsetsVisibleChanged(..)");
                }
                break;
            case ClientMessage:
                DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X, sendEvent %d\n", 
                    (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type, evt.xclient.send_event);
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==wm_delete_atom) { // windowDeleteAtom
                    jboolean closed;
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X ..\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type);
                    closed = (*env)->CallBooleanMethod(env, jw->jwindow, windowDestroyNotifyID, JNI_FALSE);
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X, closed: %d\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type, (int)closed);
                    DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 5, windowDeleteAtom received\n", dpy); 
                    // Called by Window.java: CloseWindow(); 
                    num_events = 0; // end loop in case of destroyed display
                }
                break;

            case FocusIn:
                DBG_PRINT( "X11: event . FocusIn call %p\n", (void*)evt.xfocus.window);
                {
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "FocusIn");
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, focusVisibleChangedID, JNI_FALSE, (jint)1, (jint)visibleChange) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, focus-in\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: FocusIn: Exception occured at focusVisibleChanged(..)");
                }
                break;

            case FocusOut:
                DBG_PRINT( "X11: event . FocusOut call %p\n", (void*)evt.xfocus.window);
                {
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "FocusOut");
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, focusVisibleChangedID, JNI_FALSE, (jint)0, (jint)visibleChange) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, focus-out\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: FocusOut: Exception occured at focusVisibleChanged(..)");
                }
                break;

            case  VisibilityNotify:
                DBG_PRINT( "X11: event .  VisibilityNotify call %p\n", (void*)evt.xvisibility.window);
                {
                    #if 0
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "VisibilityNotify");
                    if( 0 <= visibleChange ) {
                        if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedID, 0 < visibleChange ? JNI_TRUE : JNI_FALSE) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, vis-notify\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: VisibilityNotify: Exception occured at visibleChanged(..)");
                    }
                    #endif
                }
                break;


            case Expose:
                DBG_PRINT( "X11: event . Expose call %p %d/%d %dx%d count %d\n", (void*)evt.xexpose.window,
                    evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height, evt.xexpose.count);
                if (evt.xexpose.count == 0 && evt.xexpose.width > 0 && evt.xexpose.height > 0) {
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, windowRepaintID, JNI_FALSE,
                                                               evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, expose\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: Expose: Exception occured at windowRepaint(..)");
                    #if 0
                    uint32_t netWMState = NewtWindows_getNET_WM_STATE(dpy, jw);
                    int visibleChange = NewtWindows_updateVisibility(env, dpy, jw, netWMState, "Expose");
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedWindowRepaintID, JNI_FALSE, (jint)visibleChange,
                                                               evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, focus-in-vis-repaint\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    #endif
                }
                break;

            case MapNotify:
                DBG_PRINT( "X11: event . MapNotify call Event %p, Window %p, isMapped %d -> 1, override_redirect %d, child-event: %d\n", 
                    (void*)evt.xmap.event, (void*)evt.xmap.window, jw->isMapped, (int)evt.xmap.override_redirect,
                    evt.xmap.event!=evt.xmap.window);
                if( evt.xmap.event == evt.xmap.window ) {
                    // ignore child window notification
                    jw->isMapped = True;
                    // insets: negative values are ignored
                    int left=-1, right=-1, top=-1, bottom=-1;
                    if( NewtWindows_updateInsets(dpy, jw, False /* wait */, &left, &right, &top, &bottom) ) {
                        if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, insetsVisibleChangedID, JNI_FALSE, left, right, top, bottom, 1) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, map-insets\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: MapNotify: Exception occured at insetsVisibleChanged(..)");
                    } else {
                        if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedID, JNI_TRUE) ) {
                            DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, map\n", dpy); 
                            num_events = 0; // end loop in case of destroyed display
                        }
                        NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: MapNotify: Exception occured at visibleChanged(..)");
                    }
                }
                break;

            case UnmapNotify:
                DBG_PRINT( "X11: event . UnmapNotify call Event %p, Window %p, isMapped %d -> 0, from_configure %d, child-event: %d\n", 
                    (void*)evt.xunmap.event, (void*)evt.xunmap.window, jw->isMapped, (int)evt.xunmap.from_configure,
                    evt.xunmap.event!=evt.xunmap.window);
                if( evt.xunmap.event == evt.xunmap.window ) {
                    // ignore child window notification
                    jw->isMapped = False;
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, visibleChangedID, JNI_FALSE) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, unmap\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: UnmapNotify: Exception occured at visibleChanged(..)");
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
                    if( JNI_FALSE == (*env)->CallBooleanMethod(env, jw->jwindow, reparentNotifyID, (jlong)evt.xreparent.parent) ) {
                        DBG_PRINT( "X11: DispatchMessages 0x%X - Leave J, reparent-notify\n", dpy); 
                        num_events = 0; // end loop in case of destroyed display
                    }
                    NewtCommon_ExceptionCheck1_throwNewRuntimeException(env, "X11Display.DispatchMessages0: ReparentNotify: Exception occured at reparentNotify(..)");
                }
                break;

            // unhandled events .. yet ..

            default:
                DBG_PRINT("X11: event . unhandled %d 0x%X call %p\n", (int)evt.type, (unsigned int)evt.type, (void*)evt.xunmap.window);
        }
    }
}

/*
 * Class:     Java_jogamp_newt_driver_x11_DisplayDriver
 * Method:    createPointerIcon0
 * Signature: (JJIZIIII)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_createPointerIcon0
  (JNIEnv *env, jclass clazz, jlong display, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, jint width, jint height, jint hotX, jint hotY)
{
    Cursor c;

    if( 0 != pixels ) {
        Display * dpy = (Display *) (intptr_t) display;
        const unsigned char * pixelPtr = (const unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                                (*env)->GetDirectBufferAddress(env, pixels) : 
                                                (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );
        XcursorImage ci;
        ci.version = 1; // XCURSOR_IMAGE_VERSION;
        ci.size = width; // nominal size (assume square ..)
        ci.width = width;
        ci.height = height;
        ci.xhot = hotX;
        ci.yhot = hotY;
        ci.delay = 0;
        ci.pixels = (XcursorPixel *)(intptr_t)(pixelPtr + pixels_byte_offset);

        c = XcursorImageLoadCursor (dpy, &ci);

        if ( JNI_FALSE == pixels_is_direct ) {
            (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
        }
        DBG_PRINT( "X11: createPointerIcon0: %p %dx%d %d/%d -> %p\n", (pixelPtr+pixels_byte_offset), width, height, hotX, hotY, (void *)c);

    } else {
        c = 0;
    }
    return (jlong) (intptr_t) c;
}

/*
 * Class:     Java_jogamp_newt_driver_x11_DisplayDriver
 * Method:    destroyPointerIcon0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_DisplayDriver_destroyPointerIcon0
  (JNIEnv *env, jclass clazz, jlong display, jlong handle)
{
    Display * dpy = (Display *) (intptr_t) display;

    if( 0 != handle ) {
        Cursor c = (Cursor) (intptr_t) handle;
        DBG_PRINT( "X11: destroyPointerIcon0: %p\n", (void *)c);
        XFreeCursor(dpy, c);
    }
}


