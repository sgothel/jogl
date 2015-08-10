
#define VERBOSE_ON 1

#include "XCBEvent.h"

#include <xcb/xcb.h>
#include <xcb/xcb_event.h>
#include <xcb/xproto.h>
#include <xcb/xcb_keysyms.h>
#include <X11/Xlib-xcb.h>

void XCBSetEventQueueOwner(Display *dpy) {
    XSetEventQueueOwner(dpy, XCBOwnsEventQueue);
}

void XCBEventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong wmDeleteAtom) {
    int num_events = 100;
    xcb_connection_t *conn = NULL;

    if ( NULL == dpy ) {
        return;
    }
    conn = XGetXCBConnection(dpy);

    // Periodically take a break
    while( num_events > 0 ) {
        JavaWindow *w = NULL;
        xcb_generic_event_t *evt;
        // KeySym keySym = 0;
        jint modifiers = 0;
        char keyChar = 0;
        // char text[255];

        evt = xcb_poll_for_event(conn);
        if(NULL == evt) {
            // DBG_PRINT( "X11: DispatchMessages 0x%X - Leave 1\n", dpy); 
            return;
        }
        num_events--;

        /*if( 0==evt.xany.window ) {
            free(evt);
            NewtCommon_throwNewRuntimeException(env, "event window NULL, bail out!");
            return ;
        }

        if(dpy!=evt.xany.display) {
            free(evt);
            NewtCommon_throwNewRuntimeException(env, "wrong display, bail out!");
            return ;
        }*/

        // DBG_PRINT( "X11: DispatchMessages dpy %p, win %p, Event %d\n", (void*)dpy, (void*)evt.xany.window, evt.type);

        // X11WindowDisplayErrorHandlerEnable(1, env);

        // w = X11WindowGetJavaWindowProperty(env, dpy, evt.xany.window, javaObjectAtom, VERBOSE_BOOL);

        //X11WindowDisplayErrorHandlerEnable(0, env);

        /*if(NULL==w) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d for X11 window %p\n", 
                (void*)dpy, evt.type, (void*)evt.xany.window);
            continue;
        }*/
 
        uint8_t xcb_event_type = evt->response_type & ~0x80;
        xcb_window_t event_window = 0;

        switch( xcb_event_type ) {
            case XCB_BUTTON_PRESS:
            case XCB_BUTTON_RELEASE:
                event_window = ((xcb_button_press_event_t *)evt)->event;
                modifiers = X11InputState2NewtModifiers(((xcb_button_press_event_t *)evt)->state);
                break;
            case XCB_MOTION_NOTIFY:
                event_window = ((xcb_motion_notify_event_t *)evt)->event;
                break;
            case XCB_KEY_PRESS:
            case XCB_KEY_RELEASE: {
                    xcb_key_press_event_t *_evt = (xcb_key_press_event_t *)evt;
                    event_window = _evt->event;
                    /*
                    xcb_keycode_t   detail = _evt->detail;
                    if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                        KeySym lower_return = 0, upper_return = 0;
                        keyChar=text[0];
                        XConvertCase(keySym, &lower_return, &upper_return);
                        // always return upper case, set modifier masks (SHIFT, ..)
                        keySym = upper_return;
                        modifiers = X11InputState2NewtModifiers(evt.xkey.state);
                    } else {
                        keyChar=0;
                    }*/
                }
                break;
            case  XCB_EXPOSE:
                event_window = ((xcb_expose_event_t *)evt)->window;
                break;
            case XCB_MAP_NOTIFY:
                event_window = ((xcb_map_notify_event_t *)evt)->window;
                break;
            case XCB_UNMAP_NOTIFY:
                event_window = ((xcb_unmap_notify_event_t *)evt)->window;
                break;
        } 
        if(0==event_window) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d, no X11 window associated\n", 
                (void*)dpy, xcb_event_type);
            continue;
        }
        w = getJavaWindowProperty(env, dpy, event_window, javaObjectAtom,
        #ifdef VERBOSE_ON
                True
        #else
                False
        #endif
            );
        if(NULL==w) {
            fprintf(stderr, "Warning: NEWT X11 DisplayDispatch %p, Couldn't handle event %d for X11 window %p\n", 
                (void*)(intptr_t)dpy, xcb_event_type, (void*)(intptr_t)event_window);
            continue;
        }

        switch( xcb_event_type ) {
            case XCB_BUTTON_PRESS: {
                    xcb_button_press_event_t *_evt = (xcb_button_press_event_t *)evt;
                    (*env)->CallVoidMethod(env, w->jwindow, requestFocusID, JNI_FALSE);
                    #ifdef USE_SENDIO_DIRECT
                    (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint) _evt->state, 0.0f /*rotation*/);
                    #else
                    (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_PRESSED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint) _evt->state, 0.0f /*rotation*/);
                    #endif
                } break;
            case XCB_BUTTON_RELEASE: {
                    xcb_button_release_event_t *_evt = (xcb_button_release_event_t *)evt;
                    #ifdef USE_SENDIO_DIRECT
                    (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint) _evt->state, 0.0f /*rotation*/);
                    #else
                    (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_RELEASED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint) _evt->state, 0.0f /*rotation*/);
                    #endif
                } break;
            case XCB_MOTION_NOTIFY: {
                    xcb_motion_notify_event_t *_evt = (xcb_motion_notify_event_t *)evt;
                    #ifdef USE_SENDIO_DIRECT
                    (*env)->CallVoidMethod(env, w->jwindow, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint)0, 0.0f /*rotation*/);
                    #else
                    (*env)->CallVoidMethod(env, w->jwindow, enqueueMouseEventID, JNI_FALSE, (jint) EVENT_MOUSE_MOVED, 
                                          modifiers,
                                          (jint) _evt->event_x, (jint) _evt->event_y, (jint)0, 0.0f /*rotation*/);
                    #endif
                } break;
            case XCB_KEY_PRESS: {
                    xcb_key_press_event_t *_evt = (xcb_key_press_event_t *)evt;
                    #ifdef USE_SENDIO_DIRECT
                    (*env)->CallVoidMethod(env, w->jwindow, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                          modifiers, X11KeySym2NewtVKey(_evt->state), (jchar) keyChar);
                    #else
                    (*env)->CallVoidMethod(env, w->jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_PRESSED, 
                                          modifiers, X11KeySym2NewtVKey(_evt->state), (jchar) keyChar);
                    #endif
                } break;
            case XCB_KEY_RELEASE: {
                    xcb_key_release_event_t *_evt = (xcb_key_release_event_t *)evt;
                event_window = ((xcb_key_release_event_t *)evt)->event;
                #ifdef USE_SENDIO_DIRECT
                (*env)->CallVoidMethod(env, w->jwindow, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, X11KeySym2NewtVKey(_evt->state), (jchar) keyChar);
                #else
                (*env)->CallVoidMethod(env, w->jwindow, enqueueKeyEventID, JNI_FALSE, (jint) EVENT_KEY_RELEASED, 
                                      modifiers, X11KeySym2NewtVKey(_evt->state), (jchar) keyChar);
                #endif

                } break;
                /*
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
                    (*env)->CallVoidMethod(env, w->jwindow, sizeChangedID, 
                                            (jint) evt.xconfigure.width, (jint) evt.xconfigure.height, JNI_FALSE);
                    (*env)->CallVoidMethod(env, w->jwindow, positionChangedID, 
                                            (jint) evt.xconfigure.x, (jint) evt.xconfigure.y);
                }
                break;
            case ClientMessage:
                if (evt.xclient.send_event==True && evt.xclient.data.l[0]==(Atom)wmDeleteAtom) {
                    DBG_PRINT( "X11: event . ClientMessage call %p type 0x%X !!!\n", 
                        (void*)evt.xclient.window, (unsigned int)evt.xclient.message_type);
                    (*env)->CallVoidMethod(env, w->jwindow, windowDestroyNotifyID);
                    // Called by Window.java: CloseWindow(); 
                    num_events = 0; // end loop in case of destroyed display
                }
                break;

            case FocusIn:
                DBG_PRINT( "X11: event . FocusIn call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, w->jwindow, focusChangedID, JNI_TRUE);
                break;

            case FocusOut:
                DBG_PRINT( "X11: event . FocusOut call %p\n", (void*)evt.xvisibility.window);
                (*env)->CallVoidMethod(env, w->jwindow, focusChangedID, JNI_FALSE);
                break;
                */

            case  XCB_EXPOSE: {
                    xcb_expose_event_t *_evt = (xcb_expose_event_t *)evt;
                    DBG_PRINT( "X11: event . Expose call %p %d/%d %dx%d count %d\n", (void*)(intptr_t)_evt->window,
                        _evt->x, _evt->y, _evt->width, _evt->height, _evt->count);

                    if (_evt->count == 0 && _evt->width > 0 && _evt->height > 0) {
                        (*env)->CallVoidMethod(env, w->jwindow, windowRepaintID, 
                            _evt->x, _evt->y, _evt->width, _evt->height);
                    }
                } break;

            case XCB_MAP_NOTIFY: {
                    xcb_map_notify_event_t *_evt = (xcb_map_notify_event_t *)evt;
                    DBG_PRINT( "X11: event . MapNotify call Event %p, Window %p, override_redirect %d, child-event: %d\n", 
                        (void*)(intptr_t)_evt->event, (void*)(intptr_t)_evt->window, (int)_evt->override_redirect,
                        _evt->event!=_evt->window);
                    if( _evt->event == _evt->window ) {
                        // ignore child window notification
                        (*env)->CallVoidMethod(env, w->jwindow, visibleChangedID, JNI_TRUE);
                    }
                } break;

            case XCB_UNMAP_NOTIFY: {
                    xcb_unmap_notify_event_t *_evt = (xcb_unmap_notify_event_t *)evt;
                    DBG_PRINT( "X11: event . UnmapNotify call Event %p, Window %p, child-event: %d\n", 
                        (void*)(intptr_t)_evt->event, (void*)(intptr_t)_evt->window,
                        _evt->event!=_evt->window);
                    if( _evt->event == _evt->window ) {
                        // ignore child window notification
                        (*env)->CallVoidMethod(env, w->jwindow, visibleChangedID, JNI_FALSE);
                    }
                } break;
                /*

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
                        DBG_PRINT( "X11: event . ReparentNotify: call OldParent %p (root %p, top %p), NewParent %p (root %p, top %p), Window %p (root %p, top %p)\n", 
                            (void*)evt.xreparent.event, (void*)oldParentRoot, (void*)oldParentTopParent,
                            (void*)evt.xreparent.parent, (void*)parentRoot, (void*)parentTopParent,
                            (void*)evt.xreparent.window, (void*)winRoot, (void*)winTopParent);
                    #endif

                    (*env)->CallVoidMethod(env, w->jwindow, windowReparentedID, parentResult);
                }
                break;
                */

            // unhandled events .. yet ..

            default:
                DBG_PRINT("XCB: event . unhandled %d 0x%X call %p\n", (int)xcb_event_type, (unsigned int)xcb_event_type, (void*)(intptr_t)event_window);
        }
        free(evt);
    }
}


