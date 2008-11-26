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

#include <EGL/egl.h>
#include <KD/kd.h>
#include <KD/NV_extwindowprops.h>

#include "com_sun_javafx_newt_kd_KDWindow.h"

#include "EventListener.h"
#include "MouseEvent.h"
#include "KeyEvent.h"

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(args...) fprintf(stderr, args)

    #define DUMP_VISUAL_INFO(a,b)
    /*
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
    */

#else

    #define DBG_PRINT(args...)

    #define DUMP_VISUAL_INFO(a,b)

#endif

/**
 * Window
 */

static jmethodID sizeChangedID = NULL;
static jmethodID windowClosedID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_kd_KDWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(II)V");
    windowClosedID    = (*env)->GetMethodID(env, clazz, "windowClosed",    "()V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    if (sizeChangedID == NULL ||
        windowClosedID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_kd_KDWindow_CreateWindow
  (JNIEnv *env, jobject obj, jlong display, jlong eglConfig, jint eglRenderableType)
{
    EGLint configAttribs[] = {
        EGL_RED_SIZE,           1,
        EGL_GREEN_SIZE,         1,
        EGL_BLUE_SIZE,          1,
        EGL_ALPHA_SIZE,         EGL_DONT_CARE,
        EGL_DEPTH_SIZE,         1,
        EGL_STENCIL_SIZE,       EGL_DONT_CARE,
        EGL_SURFACE_TYPE,       EGL_WINDOW_BIT,
        EGL_RENDERABLE_TYPE,    -1,
        EGL_NONE
    };
    int i;
    EGLDisplay dpy  = (EGLDisplay)(intptr_t)display;
    EGLConfig  cfg  = (EGLConfig)(intptr_t)eglConfig;
    KDWindow *window = 0;

    if(dpy==NULL) {
        fprintf(stderr, "[CreateWindow] invalid display connection..\n");
        return 0;
    }

    i=1;
    eglGetConfigAttrib(dpy, cfg, EGL_RED_SIZE, &configAttribs[i]);
    i+=2;
    eglGetConfigAttrib(dpy, cfg, EGL_GREEN_SIZE, &configAttribs[i]);
    i+=2;
    eglGetConfigAttrib(dpy, cfg, EGL_BLUE_SIZE, &configAttribs[i]);
    i+=2;
    eglGetConfigAttrib(dpy, cfg, EGL_ALPHA_SIZE, &configAttribs[i]);
    i+=2;
    eglGetConfigAttrib(dpy, cfg, EGL_DEPTH_SIZE, &configAttribs[i]);
    i+=2;
    configAttribs[i] = EGL_WINDOW_BIT;
    i+=2;
    eglGetConfigAttrib(dpy, cfg, EGL_STENCIL_SIZE, &configAttribs[i]);
    i+=2;
    configAttribs[i] = eglRenderableType;

    /* passing the KDWindow instance for the eventuserptr */
    window = kdCreateWindow(dpy, configAttribs, (void *)(intptr_t)obj);

    if(NULL==window) {
        fprintf(stderr, "[CreateWindow] failed: 0x%X\n", kdGetError());
    }
    return (jlong) (intptr_t) window;
}

JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_kd_KDWindow_RealizeWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    EGLNativeWindowType nativeWindow=0;

    jint res = kdRealizeWindow(w, &nativeWindow);
    if(res) {
        fprintf(stderr, "[RealizeWindow] failed: 0x%X, 0x%X\n", res, kdGetError());
        nativeWindow = NULL;
    }
    return (jlong) (intptr_t) nativeWindow;
}

JNIEXPORT jint JNICALL Java_com_sun_javafx_newt_kd_KDWindow_CloseWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;

    return kdDestroyWindow(w);
}

/*
 * Class:     com_sun_javafx_newt_kd_KDWindow
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setVisible0
  (JNIEnv *env, jobject obj, jlong window, jboolean visible)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = (visible==JNI_TRUE)?KD_TRUE:KD_FALSE;
    kdSetWindowPropertybv(w, KD_WINDOWPROPERTY_VISIBILITY, &v);
}

/*
 * Class:     com_sun_javafx_newt_kd_KDWindow
 * Method:    DispatchMessages
 * Signature: (JJI)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_DispatchMessages
  (JNIEnv *env, jobject obj, jlong window, jint eventMask)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;

    // Periodically take a break
    const KDEvent * evt;
    while( NULL!=(evt=kdWaitEvent(0)) ) {
        jobject src_obj = (jobject)(intptr_t)evt->userptr;
        if(src_obj != obj) {
            DBG_PRINT( "event unrelated: src: %p, caller: %p\n", src_obj, obj);
            continue;
        }

        switch(evt->type) {
            case KD_EVENT_INPUT_POINTER:
                if( ! ( eventMask & EVENT_MOUSE ) ) {
                    DBG_PRINT( "event mouse ignored: src: %p\n", obj);
                    continue;
                }
                break;
            /*
            case KeyPress:
            case KeyRelease:
                if( ! ( eventMask & EVENT_KEY ) ) {
                    DBG_PRINT( "event key ignored: src: %p\n", obj);
                    continue;
                }
                break;
                */
            case KD_EVENT_WINDOW_FOCUS:
            case KD_EVENT_WINDOW_CLOSE:
            case KD_EVENT_WINDOWPROPERTY_CHANGE:
            case KD_EVENT_WINDOW_REDRAW:
                if( ! ( eventMask & EVENT_WINDOW ) ) {
                    DBG_PRINT( "event window ignored: src: %p\n", obj);
                    continue;
                }
                break;
        }
        
        // FIXME: support resize and window re-positioning events

        switch(evt->type) {
            case KD_EVENT_WINDOW_FOCUS:
                {
                    KDboolean hasFocus;
                    kdGetWindowPropertybv(w, KD_WINDOWPROPERTY_FOCUS, &hasFocus);
                    DBG_PRINT( "event window focus : src: %p\n", obj);
                }
                break;
            case KD_EVENT_WINDOW_CLOSE:
                {
                    (*env)->CallVoidMethod(env, obj, windowClosedID);
                    DBG_PRINT( "event window close : src: %p\n", obj);
                }
                break;
            case KD_EVENT_WINDOWPROPERTY_CHANGE:
                {
                    const KDEventWindowProperty* prop = &evt->data.windowproperty;
                    switch (prop->pname) {
                        case KD_WINDOWPROPERTY_SIZE:
                            {
                                KDint32 v[2];
                                if(!kdGetWindowPropertyiv(w, KD_WINDOWPROPERTY_SIZE, v)) {
                                    DBG_PRINT( "event window size change : src: %p %dx%x\n", obj, v[0], v[1]);
                                    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) v[0], (jint) v[1]);
                                } else {
                                    DBG_PRINT( "event window size change error: src: %p\n", obj);
                                }
                            }
                            break;
                        case KD_WINDOWPROPERTY_FOCUS:
                            DBG_PRINT( "event window focus: src: %p\n", obj);
                            break;
                        case KD_WINDOWPROPERTY_VISIBILITY:
                            {
                                KDboolean visible;
                                kdGetWindowPropertybv(w, KD_WINDOWPROPERTY_VISIBILITY, &visible);
                                DBG_PRINT( "event window visibility: src: %p, v:%d\n", obj, visible);
                            }
                            break;
                        default:
                            break;
                    }
                }
                break;
            case KD_EVENT_INPUT_POINTER:
                {
                    const KDEventInputPointer* ptr = &(evt->data.inputpointer);
                    // button idx: evt->data.input.index
                    // pressed = ev->data.input.value.i
                    // time = ev->timestamp
                    (*env)->CallVoidMethod(env, obj, sendMouseEventID, 
                                          (ptr->select==0) ? (jint) EVENT_MOUSE_RELEASED : (jint) EVENT_MOUSE_PRESSED, 
                                          (jint) 0,
                                          (jint) ptr->x, (jint) ptr->y, (jint) ptr->index);
                }
                break;
            /*
            case MotionNotify:
                if(evt.xmotion.window==w) {
                    (*env)->CallVoidMethod(env, obj, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                          (jint) evt.xmotion.state, 
                                          (jint) evt.xmotion.x, (jint) evt.xmotion.y, (jint) 0);
                }
                break;
            */
        }
    } 
}

JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setFullscreen0
  (JNIEnv *env, jobject obj, jlong window, jboolean fullscreen)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = fullscreen;

    int res = kdSetWindowPropertyiv(w, KD_WINDOWPROPERTY_FULLSCREEN_NV, &v);

    DBG_PRINT( "setFullscreen0 . fullscreen call: %d\n", res);
}

JNIEXPORT void JNICALL Java_com_sun_javafx_newt_kd_KDWindow_setSize0
  (JNIEnv *env, jobject obj, jlong window, jint width, jint height)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDint32 v[] = { width, height };

    int res = kdSetWindowPropertyiv(w, KD_WINDOWPROPERTY_SIZE, v);

    DBG_PRINT( "setSize0 . sizeChangedID call: %d\n", res);
    (*env)->CallVoidMethod(env, obj, sizeChangedID, (jint) width, (jint) height);
}

