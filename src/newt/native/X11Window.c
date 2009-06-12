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
    #define DBG_PRINT(args...) fprintf(stderr, args)

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

    #define DBG_PRINT(args...)

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

/**
 * Display
 */

/*
 * Class:     com_sun_javafx_newt_x11_X11Display
 * Method:    CreateDisplay
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_x11_X11Display_CreateDisplay
  (JNIEnv *env, jobject obj, jstring displayName)
{
    Display * dpy = NULL;
    const char * _displayName = NULL;
    if(displayName!=0) {
        _displayName = (*env)->GetStringUTFChars(env, displayName, 0);
    }
    dpy = XOpenDisplay(_displayName);
    if(_displayName!=0) {
        (*env)->ReleaseStringChars(env, displayName, (const jchar *)_displayName);
    }
    if(dpy==NULL) {
        fprintf(stderr, "couldn't open display connection..\n");
    }
    return (jlong) (intptr_t) dpy;
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

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID windowCreatedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

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
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(JJ)V");
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

    DBG_PRINT( "CreateWindow %x/%d %dx%d\n", x, y, width, height);

    if(dpy==NULL) {
        fprintf(stderr, "[CreateWindow] invalid display connection..\n");
        return 0;
    }

    if(visualID<0) {
        fprintf(stderr, "[CreateWindow] invalid VisualID ..\n");
        return 0;
    }

    XSync(dpy, False);

    Screen * scrn = ScreenOfDisplay(dpy, screen_index);

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
    DBG_PRINT( "trying given (screen %d, visualID: %d) found: %p\n", scrn_idx, (int)visualID, visual);

    if (visual==NULL)
    { 
        fprintf(stderr, "could not query Visual by given VisualID, bail out!\n");
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
    Atom wm_delete_window = XInternAtom(dpy, "WM_DELETE_WINDOW", False);
    XSetWMProtocols(dpy, window, &wm_delete_window, 1);
    XClearWindow(dpy, window);
    XSync(dpy, False);

    (*env)->CallVoidMethod(env, obj, windowCreatedID, (jlong) window, (jlong)wm_delete_window);

    return (jlong) window;
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    CloseWindow
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_CloseWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

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
    DBG_PRINT( "setVisible0 vis %d\n", visible);
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

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    DispatchMessages
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_DispatchMessages
  (JNIEnv *env, jobject obj, jlong display, jlong window, jint eventMask, jlong wmDeleteAtom)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    Atom wm_delete_window = (Atom)wmDeleteAtom;

    if(eventMask<0) {
        long xevent_mask_key = 0;
        long xevent_mask_ptr = 0;
        long xevent_mask_win = 0;
        eventMask *= -1;
        if( 0 != ( eventMask & EVENT_MOUSE ) ) {
            xevent_mask_ptr |= ButtonPressMask|ButtonReleaseMask|PointerMotionMask;
        }
        if( 0 != ( eventMask & EVENT_KEY ) ) {
            xevent_mask_key |= KeyPressMask|KeyReleaseMask;
        }
        if( 0 != ( eventMask & EVENT_WINDOW ) ) {
            xevent_mask_win |= ExposureMask | StructureNotifyMask | SubstructureNotifyMask | VisibilityNotify ;
        }

        XSelectInput(dpy, w, xevent_mask_win|xevent_mask_key|xevent_mask_ptr);

        /**
        if(0!=xevent_mask_ptr) {
            XGrabPointer(dpy, w, True, xevent_mask_ptr,
                        GrabModeAsync, GrabModeAsync, w, None, CurrentTime);
        } 
        if(0!=xevent_mask_key) {
            XGrabKeyboard(dpy, w, True, GrabModeAsync, GrabModeAsync, CurrentTime);
        } */

    }

    // Periodically take a break
    while( XPending(dpy)>0 ) {
        XEvent evt;
        KeySym keySym;
        char keyChar;
        char text[255];

        XNextEvent(dpy, &evt);
 
        switch(evt.type) {
            case ButtonPress:
            case ButtonRelease:
            case MotionNotify:
                if( ! ( eventMask & EVENT_MOUSE ) ) {
                    continue;
                }
                break;
            case KeyPress:
            case KeyRelease:
                if( ! ( eventMask & EVENT_KEY ) ) {
                    continue;
                }
                break;
            case FocusIn:
            case FocusOut:
                break;
            case DestroyNotify:
            case CreateNotify:
            case VisibilityNotify:
            case Expose:
            case UnmapNotify:
                if( ! ( eventMask & EVENT_WINDOW ) ) {
                    continue;
                }
                break;
        }
        
        // FIXME: support resize and window re-positioning events

        switch(evt.type) {
            case ButtonPress:
                if(evt.xbutton.window==w) {
                    (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                          (jint) evt.xbutton.state, 
                                          (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                }
                break;
            case ButtonRelease:
                if(evt.xbutton.window==w) {
                    (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                          (jint) evt.xbutton.state, 
                                          (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button, 0 /*rotation*/);
                }
                break;
            case MotionNotify:
                if(evt.xmotion.window==w) {
                    (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                          (jint) evt.xmotion.state, 
                                          (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0, 0 /*rotation*/);
                }
                break;
            case KeyPress:
                if(evt.xkey.window==w) {
                    if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                        keyChar=text[0];
                    } else {
                        keyChar=0;
                    }
                    (*env)->CallVoidMethod(env, obj, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                          (jint) evt.xkey.state, 
                                          X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                }
                break;
            case KeyRelease:
                if(evt.xkey.window==w) {
                    if(XLookupString(&evt.xkey,text,255,&keySym,0)==1) {
                        keyChar=text[0];
                    } else {
                        keyChar=0;
                    }
                    (*env)->CallVoidMethod(env, obj, sendKeyEventID, (jint) EVENT_KEY_RELEASED, 
                                          (jint) evt.xkey.state, 
                                          X11KeySym2NewtVKey(keySym), (jchar) keyChar);
                }
                break;
            case FocusIn:
            case FocusOut:
                if(evt.xfocus.window==w) {
                }
                break;
            case DestroyNotify:
                if(evt.xdestroywindow.window==w) {
                    DBG_PRINT( "event . DestroyNotify call 0x%X\n", evt.xdestroywindow.window);
                    (*env)->CallVoidMethod(env, obj, windowDestroyedID);
                }
                break;
            case CreateNotify:
                if(evt.xcreatewindow.window==w) {
                    DBG_PRINT( "event . DestroyNotify call 0x%X\n", evt.xcreatewindow.window);
                    (*env)->CallVoidMethod(env, obj, windowCreatedID);
                }
                break;
            case VisibilityNotify:
                if(evt.xvisibility.window==w) {
                    DBG_PRINT( "event . VisibilityNotify call 0x%X\n", evt.xvisibility.window);
                }
                break;
            case Expose:
                if(evt.xexpose.window==w) {
                    DBG_PRINT( "event . Expose call 0x%X\n", evt.xexpose.window);
                    /* FIXME: Might want to send a repaint event .. */
                }
                break;
            case UnmapNotify:
                if(evt.xunmap.window==w) {
                    DBG_PRINT( "event . UnmapNotify call 0x%X\n", evt.xunmap.window);
                }
                break;
            case ClientMessage:
                if (evt.xclient.window==w && evt.xclient.send_event==True && evt.xclient.data.l[0]==wm_delete_window) {
                    DBG_PRINT( "event . ClientMessage call 0x%X type 0x%X !!!\n", evt.xclient.window, evt.xclient.message_type);
                    (*env)->CallVoidMethod(env, obj, windowDestroyNotifyID);
                    // Called by Window.java: CloseWindow(); 
                }
                break;

        }
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

    DBG_PRINT( "setSize0 %dx%d, dec %d, vis %d\n", width, height, decorationToggle, isVisible);

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
    DBG_PRINT( "setSize0 . XConfigureWindow\n");
    XWindowChanges xwc;
    xwc.width=width;
    xwc.height=height;
    XConfigureWindow(dpy, w, CWWidth|CWHeight, &xwc);
    XReparentWindow( dpy, w, parent, x, y );

    XSync(dpy, False);

    /** if(isVisible==JNI_TRUE) {
        XSetInputFocus(dpy, w, RevertToNone, CurrentTime);
    } */

    DBG_PRINT( "setSize0 . sizeChangedID call\n");
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

    DBG_PRINT( "setPos0 . XConfigureWindow\n");
    XWindowChanges xwc;
    xwc.x=x;
    xwc.y=y;
    XConfigureWindow(dpy, w, CWX|CWY, &xwc);
    XSync(dpy, False);

    // (*env)->CallVoidMethod(env, obj, positionChangedID, (jint) width, (jint) height);
}

