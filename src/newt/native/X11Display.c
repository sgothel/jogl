/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

#define USE_SENDIO_DIRECT 1

jclass X11NewtWindowClazz = NULL;
jmethodID insetsChangedID = NULL;
jmethodID visibleChangedID = NULL;

static const char * const ClazzNameX11NewtWindow = "jogamp/newt/driver/x11/X11Window";

static jmethodID displayCompletedID = NULL;

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID = NULL;
static jmethodID reparentNotifyID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;
static jmethodID enqueueMouseEventID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

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

void NewtDisplay_displayDispatchErrorHandlerEnable(int onoff, JNIEnv * env) {
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

/**
 * Keycode
 */

#define IS_WITHIN(k,a,b) ((a)<=(k)&&(k)<=(b))

static jint X11KeySym2NewtVKey(KeySym keySym) {
    if(IS_WITHIN(keySym,XK_F1,XK_F12)) 
        return (keySym-XK_F1)+J_VK_F1;
    if(IS_WITHIN(keySym,XK_KP_0,XK_KP_9)) 
        return (keySym-XK_KP_0)+J_VK_NUMPAD0;

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
        case XK_Clear:
            return J_VK_CLEAR;
        case XK_Shift_L:
        case XK_Shift_R:
            return J_VK_SHIFT;
        case XK_Control_L:
        case XK_Control_R:
            return J_VK_CONTROL;
        case XK_Alt_L:
        case XK_Alt_R:
            return J_VK_ALT;
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


/**
 * Keycode
 */

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

    if(NULL==X11NewtWindowClazz) {
        c = (*env)->FindClass(env, ClazzNameX11NewtWindow);
        if(NULL==c) {
            NewtCommon_FatalError(env, "NEWT X11Window: can't find %s", ClazzNameX11NewtWindow);
        }
        X11NewtWindowClazz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==X11NewtWindowClazz) {
            NewtCommon_FatalError(env, "NEWT X11Window: can't use %s", ClazzNameX11NewtWindow);
        }
    }

    displayCompletedID = (*env)->GetMethodID(env, clazz, "displayCompleted", "(JJ)V");
    insetsChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "insetsChanged", "(ZIIII)V");
    sizeChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sizeChanged", "(ZIIZ)V");
    positionChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "focusChanged", "(ZZ)V");
    visibleChangedID = (*env)->GetMethodID(env, X11NewtWindowClazz, "visibleChanged", "(ZZ)V");
    reparentNotifyID = (*env)->GetMethodID(env, X11NewtWindowClazz, "reparentNotify", "(J)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, X11NewtWindowClazz, "windowDestroyNotify", "()V");
    windowRepaintID = (*env)->GetMethodID(env, X11NewtWindowClazz, "windowRepaint", "(ZIIII)V");
    enqueueMouseEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "enqueueMouseEvent", "(ZIIIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendMouseEvent", "(IIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "enqueueKeyEvent", "(ZIIIC)V");
    sendKeyEventID = (*env)->GetMethodID(env, X11NewtWindowClazz, "sendKeyEvent", "(IIIC)V");
    requestFocusID = (*env)->GetMethodID(env, X11NewtWindowClazz, "requestFocus", "(Z)V");

    if (displayCompletedID == NULL ||
        insetsChangedID == NULL ||
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
        requestFocusID == NULL) {
        return JNI_FALSE;
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

        NewtDisplay_displayDispatchErrorHandlerEnable(1, env);

        jwindow = getJavaWindowProperty(env, dpy, evt.xany.window, javaObjectAtom,
        #ifdef VERBOSE_ON
                True
        #else
                False
        #endif
            );

        NewtDisplay_displayDispatchErrorHandlerEnable(0, env);

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
                    keySym = X11KeySym2NewtVKey(upper_return);
                } else {
                    keyChar=0;
                    keySym = X11KeySym2NewtVKey(keySym);
                }
                modifiers = X11InputState2NewtModifiers(evt.xkey.state);
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
                (*env)->CallVoidMethod(env, jwindow, requestFocusID, JNI_FALSE);
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
            case EnterNotify:
                DBG_PRINT( "X11: event . EnterNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_ENTERED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0 /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_ENTERED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0 /*rotation*/); 
                #endif
                break;
            case LeaveNotify:
                DBG_PRINT( "X11: event . LeaveNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendMouseEventID, (jint) EVENT_MOUSE_EXITED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0 /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_EXITED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0 /*rotation*/); 
                #endif
                break;
            case KeyPress:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, keySym, (jchar) -1);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, keySym, (jchar) -1);
                #endif

                break;
            case KeyRelease:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, keySym, (jchar) -1);

                (*env)->CallVoidMethod(env, jwindow, sendKeyEventID, (jint) EVENT_KEY_TYPED, 
                                      modifiers, keySym, (jchar) keyChar);
                #else
                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, keySym, (jchar) -1);

                (*env)->CallVoidMethod(env, jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_TYPED, 
                                      modifiers, keySym, (jchar) keyChar);
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


