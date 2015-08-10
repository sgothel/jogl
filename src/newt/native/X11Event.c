
#include "X11Event.h"

void X11EventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong windowDeleteAtom) {
    Atom wm_delete_atom = (Atom)windowDeleteAtom;
    int num_events = 100;
    int autoRepeatModifiers = 0;

    if ( NULL == dpy ) {
        return;
    }

    // Periodically take a break
    while( num_events > 0 ) {
        JavaWindow *w = NULL;
        XEvent evt;
        KeySym keySym = 0;
        jint modifiers = 0;
        char keyChar = 0;
        char text[255];

        // XEventsQueued(dpy, X):
        //   QueuedAlready    == XQLength(): No I/O Flush or system call  doesn't work on some cards (eg ATI) ?) 
        //   QueuedAfterFlush == XPending(): I/O Flush only if no already queued events are available
        //   QueuedAfterReading            : QueuedAlready + if queue==0, attempt to read more ..
        // if ( 0 >= XPending(dpy) ) 
        if ( 0 >= XEventsQueued(dpy, QueuedAfterFlush) )
        {
            // DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 1\n", dpy); 
            return;
        }

        XNextEvent(dpy, &evt);
        num_events--;

        if(dpy!=evt.xany.display) {
            NewtCommon_throwNewRuntimeException(env, "wrong display, bail out!");
            return ;
        }

        if( 0==evt.xany.window ) {
            DBG_PRINT( "X11: DispatchMessages dpy %p, Event %d - Window NULL, ignoring\n", (void*)dpy, (int)evt.type);
            continue;
        }

        // DBG_PRINT( "X11: DispatchMessages dpy %p, win %p, Event %d\n", (void*)dpy, (void*)evt.xany.window, (int)evt.type);

        w = getJavaWindowProperty(env, dpy, evt.xany.window, javaObjectAtom,
        #ifdef VERBOSE_ON
                True
        #else
                False
        #endif
            );

        if(NULL==w) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d for X11 window %p\n", (void*)dpy, evt.type, (void*)evt.xany.window);
            continue;
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
                }
                // fall through intended
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
                modifiers |= X11InputState2NewtModifiers(evt.xkey.state) | autoRepeatModifiers;
                break;

            case ButtonPress:
            case ButtonRelease:
            case MotionNotify:
                modifiers |= X11InputState2NewtModifiers(evt.xbutton.state);
                break;

            default:
                break;
        }

        switch(evt.type) {
            case ButtonPress:
                (*env)->CallVoidMethod(env, w->jwindow, requestFocusID, JNI_FALSE);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0.0f /*rotation*/);
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_PRESSED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0.0f /*rotation*/);
                #endif
                break;
            case ButtonRelease:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0.0f /*rotation*/);
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_RELEASED, 
                                      modifiers,
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0.0f /*rotation*/);
                #endif
                break;
            case MotionNotify:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                      modifiers,
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0.0f /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_MOVED, 
                                      modifiers,
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0.0f /*rotation*/); 
                #endif
                break;
            case EnterNotify:
                DBG_PRINT( "X11: event . EnterNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_ENTERED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0.0f /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_ENTERED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0.0f /*rotation*/); 
                #endif
                break;
            case LeaveNotify:
                DBG_PRINT( "X11: event . LeaveNotify call %p %d/%d\n", (void*)evt.xcrossing.window, evt.xcrossing.x, evt.xcrossing.y);
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_EXITED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0.0f /*rotation*/); 
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_EXITED, 
                                      modifiers,
                                      (jint) evt.xcrossing.x, (jint) evt.xcrossing.y, (jint) 0, 0.0f /*rotation*/); 
                #endif
                break;
            case KeyPress:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, keySym, (jchar) -1);
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_PRESSED, 
                                      modifiers, keySym, (jchar) -1);
                #endif

                break;
            case KeyRelease:
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, keySym, (jchar) -1);
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, keySym, (jchar) -1);
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
                        NewtWindows_updateInsets(env, w->jwindow, dpy, evt.xany.window, &left, &right, &top, &bottom);
                    }
                    (*env)->CallVoidMethod(env, w->jwindow, sizeChangedID, JNI_FALSE,
                                            (jint) evt.xconfigure.width, (jint) evt.xconfigure.height, JNI_FALSE);
                    (*env)->CallVoidMethod(env, w->jwindow, positionChangedID, JNI_FALSE,
                                            (jint) evt.xconfigure.x, (jint) evt.xconfigure.y);
                }
                break;
            case ClientMessage:
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==wm_delete_atom) { // windowDeleteAtom
                    jboolean closed;
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X ..\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type);
                    closed = (*env)->CallBooleanMethod(env, w->jwindow, windowDestroyNotifyID, JNI_FALSE);
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X, closed: %d\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type, (int)closed);
                    // Called by Window.java: CloseWindow(); 
                    num_events = 0; // end loop in case of destroyed display
                }
                break;

            case FocusIn:
                DBG_PRINT( "X11: event . FocusIn call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, w->jwindow, focusChangedID, JNI_FALSE, JNI_TRUE);
                break;

            case FocusOut:
                DBG_PRINT( "X11: event . FocusOut call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, w->jwindow, focusChangedID, JNI_FALSE, JNI_FALSE);
                break;

            case Expose:
                DBG_PRINT( "X11: event . Expose call %p %d/%d %dx%d count %d\n", (void*)evt.xexpose.window,
                    evt.xexpose.x, evt.xexpose.y, evt.xexpose.width, evt.xexpose.height, evt.xexpose.count);

                if (evt.xexpose.count == 0 && evt.xexpose.width > 0 && evt.xexpose.height > 0) {
                    (*env)->CallVoidMethod(env, w->jwindow, windowRepaintID, JNI_FALSE,
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
                        NewtWindows_updateInsets(env, w->jwindow, dpy, evt.xany.window, &left, &right, &top, &bottom);
                    }
                    (*env)->CallVoidMethod(env, w->jwindow, visibleChangedID, JNI_FALSE, JNI_TRUE);
                }
                break;

            case UnmapNotify:
                DBG_PRINT( "X11: event . UnmapNotify call Event %p, Window %p, from_configure %d, child-event: %d\n", 
                    (void*)evt.xunmap.event, (void*)evt.xunmap.window, (int)evt.xunmap.from_configure,
                    evt.xunmap.event!=evt.xunmap.window);
                if( evt.xunmap.event == evt.xunmap.window ) {
                    // ignore child window notification
                    (*env)->CallVoidMethod(env, w->jwindow, visibleChangedID, JNI_FALSE, JNI_FALSE);
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
                    (*env)->CallVoidMethod(env, w->jwindow, reparentNotifyID, (jlong)evt.xreparent.parent);
                }
                break;

            // unhandled events .. yet ..

            default:
                DBG_PRINT("X11: event . unhandled %d 0x%X call %p\n", (int)evt.type, (unsigned int)evt.type, (void*)evt.xunmap.window);
        }
    }
}
