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

#include "MouseEvent.h"
#include "KeyEvent.h"

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID windowClosedID = NULL;
static jmethodID windowDestroyedID = NULL;
static jmethodID windowCreatedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

// This is set by DispatchMessages, below, and cleared when it exits
static JNIEnv* env = NULL;

#if 0
static LRESULT CALLBACK wndProc(HWND wnd, UINT message,
                                WPARAM wParam, LPARAM lParam)
{
    RECT rc;
    int useDefWindowProc = 0;
    jobject window = NULL;

    window = (jobject) GetWindowLongPtr(wnd, GWLP_USERDATA);
    if (window == NULL || env == NULL) {
        // Shouldn't happen
        return DefWindowProc(wnd, message, wParam, lParam);
    }
    
    switch (message) {
    case WM_CLOSE:
        (*env)->CallVoidMethod(env, window, windowClosedID);
        DestroyWindow(wnd);
        break;

    case WM_DESTROY:
        (*env)->CallVoidMethod(env, window, windowDestroyedID);
        break;

    case WM_KEYDOWN:
        (*env)->CallVoidMethod(env, window, keyDownID, (jlong) wParam);
        useDefWindowProc = 1;
        break;

    case WM_KEYUP:
        (*env)->CallVoidMethod(env, window, keyUpID, (jlong) wParam);
        useDefWindowProc = 1;
        break;

    case WM_SIZE:
        GetClientRect(wnd, &rc);
        (*env)->CallVoidMethod(env, window, sizeChangedID, (jint) rc.right, (jint) rc.bottom);
        break;

    default:
        useDefWindowProc = 1;
    }

    if (useDefWindowProc)
        return DefWindowProc(wnd, message, wParam, lParam);
    return 0;
}
#endif

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
    windowClosedID    = (*env)->GetMethodID(env, clazz, "windowClosed",    "()V");
    windowDestroyedID = (*env)->GetMethodID(env, clazz, "windowDestroyed", "()V");
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(JJJ)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        windowClosedID == NULL ||
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
 * Signature: (JIIII)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_x11_X11Window_CreateWindow
  (JNIEnv *env, jobject obj, jlong visualID,
                             jint x, jint y, jint width, jint height)
{
    Display * dpy;
    Screen  * scrn;
    Window  windowParent = 0;
    Window  window = 0;

    XVisualInfo visualTemplate;
    XVisualInfo *pVisualQuery = NULL;
    XSetWindowAttributes xswa;
    unsigned long attrMask;

    int n;

    dpy = XOpenDisplay(NULL);
    if(dpy==NULL) {
        fprintf(stderr, "could not open X display ..\n");
        return 0;
    }

    memset(&visualTemplate, 0, sizeof(XVisualInfo));
    visualTemplate.visualid = (VisualID)visualID;

    pVisualQuery = XGetVisualInfo(dpy, VisualIDMask, &visualTemplate,&n);
    if (pVisualQuery==NULL)
    {
        fprintf(stderr, "could not query XVisualInfo ..\n");
        return 0;
    }

    scrn = ScreenOfDisplay(dpy, pVisualQuery->screen);

    windowParent = XRootWindowOfScreen(scrn);

    attrMask = (CWBackPixel | CWBorderPixel | CWColormap | CWEventMask | CWOverrideRedirect);
    memset(&xswa, 0, sizeof(xswa));
    xswa.override_redirect = True;
    xswa.border_pixel = 0;
    xswa.background_pixel = 0;
    xswa.event_mask = ExposureMask | StructureNotifyMask | KeyPressMask | KeyReleaseMask;
    xswa.colormap = XCreateColormap(dpy,
                                    RootWindow(dpy, pVisualQuery->screen),
                                    pVisualQuery->visual,
                                    AllocNone);

    window = XCreateWindow(dpy,
                           windowParent,
                           x, y,
                           width, height,
                           0, // border width
                           pVisualQuery->depth,
                           InputOutput,
                           pVisualQuery->visual,
                           attrMask,
                           &xswa);
    XFree(pVisualQuery);

    (*env)->CallVoidMethod(env, obj, windowCreatedID, (jlong) (intptr_t) dpy, (jlong) (intptr_t) scrn, (jlong) window);

    return (jlong) window;
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
    if(visible==JNI_TRUE) {
        XMapWindow(dpy, window);

        XSelectInput(dpy, window, ButtonPressMask|ButtonRelease|PointerMotionMask|
                                  KeyPressMask|KeyReleaseMask);
    } else {
        XSelectInput(dpy, window, 0);
        XUnmapWindow(dpy, window);
    }
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    DispatchMessages
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_DispatchMessages
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;

    // Periodically take a break
    while( XPending(dpy)>0 ) {
        int eventType;
        XEvent evt;
        int i;

        XNextEvent(dpy, &evt);

        switch(evt.type) {
            case ButtonPress:
                (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_PRESSED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button);
                break;
            case ButtonRelease:
                (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_RELEASED, 
                                      (jint) evt.xbutton.state, 
                                      (jint) evt.xbutton.x, (jint) evt.xbutton.y, (jint) evt.xbutton.button);
                break;
            case MotionNotify:
                (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                      (jint) evt.xmotion.state, 
                                      (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0);
                break;
            case KeyPress:
                i = evt.xkey.keycode;
                (*env)->CallVoidMethod(env, obj, sendKeyEventID, (jint) EVENT_KEY_PRESSED, 
                                      (jint) evt.xkey.state, 
                                      (jint) i, (jchar) ( isascii(i)?i:0 ) );
                // evt.xkey
                break;
            case KeyRelease:
                // evt.xkey
                break;
            case FocusIn:
                // evt.xfocus
                break;
            case FocusOut:
                // evt.xfocus
                break;
        }
    } 
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    setSize0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_x11_X11Window_setSize0
  (JNIEnv *env, jobject obj, jlong display, jlong window, jint width, jint height)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window w = (Window)window;
    XResizeWindow(dpy, w, (unsigned)width, (unsigned)height);
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
    XMoveWindow(dpy, w, (unsigned)x, (unsigned)y);
    // (*env)->CallVoidMethod(env, obj, positionChangedID, (jint) width, (jint) height);
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    getDisplayWidth0
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_x11_X11Window_getDisplayWidth0
  (JNIEnv *env, jobject obj, jlong display, jlong screen)
{
    Display * dpy = (Display *) (intptr_t) display;
    Screen * scrn = (Screen *) (intptr_t) screen;
    return (jint) XDisplayWidth( dpy, scrn);
}

/*
 * Class:     com_sun_javafx_newt_x11_X11Window
 * Method:    getDisplayHeight0
 * Signature: (JJ)I
 */
JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_x11_X11Window_getDisplayHeight0
  (JNIEnv *env, jobject obj, jlong display, jlong screen)
{
    Display * dpy = (Display *) (intptr_t) display;
    Screen * scrn = (Screen *) (intptr_t) screen;
    return (jint) XDisplayHeight( dpy, scrn);
}


