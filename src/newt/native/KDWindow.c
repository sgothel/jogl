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

#ifdef _WIN32
  #include <windows.h>
#endif

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <gluegen_stdint.h>

#include <KD/kd.h>

#include "jogamp_newt_driver_kd_KDWindow.h"

#include "MouseEvent.h"
#include "KeyEvent.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif

#ifdef VERBOSE_ON
    #ifdef _WIN32_WCE
        #define STDOUT_FILE "\\Storage Card\\stdout.txt"
        #define STDERR_FILE "\\Storage Card\\stderr.txt"
    #endif
#endif

#define JOGL_KD_USERDATA_MAGIC 0xDEADBEEF
typedef struct {
    long magic;
    KDWindow * kdWindow;
    jobject javaWindow;
} JOGLKDUserdata;

static jmethodID windowCreatedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

/**
 * Display
 */

JNIEXPORT void JNICALL Java_jogamp_newt_driver_kd_KDDisplay_DispatchMessages
  (JNIEnv *env, jobject obj)
{
    const KDEvent * evt;
    int numEvents = 0;

    // Periodically take a break
    while( numEvents<100 && NULL!=(evt=kdWaitEvent(0)) ) {
        KDWindow *kdWindow;
        jobject javaWindow;
        JOGLKDUserdata * userData = (JOGLKDUserdata *)(intptr_t)evt->userptr;
        if(NULL == userData || userData->magic!=JOGL_KD_USERDATA_MAGIC) {
            DBG_PRINT( "event unrelated: evt type: 0x%X\n", evt->type);
            continue;
        }
        kdWindow = userData->kdWindow;
        javaWindow = userData->javaWindow;
        DBG_PRINT( "[DispatchMessages]: userData %p, evt type: 0x%X\n", userData, evt->type);

        numEvents++;

        // FIXME: support resize and window re-positioning events

        switch(evt->type) {
            case KD_EVENT_WINDOW_FOCUS:
                {
                    KDboolean hasFocus;
                    kdGetWindowPropertybv(kdWindow, KD_WINDOWPROPERTY_FOCUS, &hasFocus);
                    DBG_PRINT( "event window focus : src: %p\n", userData);
                }
                break;
            case KD_EVENT_WINDOW_CLOSE:
                {
                    DBG_PRINT( "event window close : src: %p\n", userData);
                    (*env)->CallVoidMethod(env, javaWindow, windowDestroyNotifyID);
                }
                break;
            case KD_EVENT_WINDOWPROPERTY_CHANGE:
                {
                    const KDEventWindowProperty* prop = &evt->data.windowproperty;
                    switch (prop->pname) {
                        case KD_WINDOWPROPERTY_SIZE:
                            {
                                KDint32 v[2];
                                if(!kdGetWindowPropertyiv(kdWindow, KD_WINDOWPROPERTY_SIZE, v)) {
                                    DBG_PRINT( "event window size change : src: %p %dx%d\n", userData, v[0], v[1]);
                                    (*env)->CallVoidMethod(env, javaWindow, sizeChangedID, JNI_FALSE, (jint) v[0], (jint) v[1], JNI_FALSE);
                                } else {
                                    DBG_PRINT( "event window size change error: src: %p %dx%d\n", userData, v[0], v[1]);
                                }
                            }
                            break;
                        case KD_WINDOWPROPERTY_FOCUS:
                            DBG_PRINT( "event window focus: src: %p\n", userData);
                            break;
                        case KD_WINDOWPROPERTY_VISIBILITY:
                            {
                                KDboolean visible;
                                kdGetWindowPropertybv(kdWindow, KD_WINDOWPROPERTY_VISIBILITY, &visible);
                                DBG_PRINT( "event window visibility: src: %p, v:%d\n", userData, visible);
                                (*env)->CallVoidMethod(env, javaWindow, visibleChangedID, JNI_FALSE, visible?JNI_TRUE:JNI_FALSE);
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
                    if(KD_INPUT_POINTER_SELECT==ptr->index) {
                        DBG_PRINT( "event mouse click: src: %p, s:%d, (%d,%d)\n", userData, ptr->select, ptr->x, ptr->y);
                        (*env)->CallVoidMethod(env, javaWindow, sendMouseEventID, 
                                              (ptr->select==0) ? (jint) EVENT_MOUSE_RELEASED : (jint) EVENT_MOUSE_PRESSED, 
                                              (jint) 0,
                                              (jint) ptr->x, (jint) ptr->y, 1, 0);
                    } else {
                        DBG_PRINT( "event mouse: src: %d, s:%p, i:0x%X (%d,%d)\n", userData, ptr->select, ptr->index, ptr->x, ptr->y);
                        (*env)->CallVoidMethod(env, javaWindow, sendMouseEventID, (jint) EVENT_MOUSE_MOVED, 
                                              0,
                                              (jint) ptr->x, (jint) ptr->y, 0, 0);
                    }
                }
                break;
        }
    } 
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_kd_KDWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
#ifdef VERBOSE_ON
    #ifdef _WIN32_WCE
        _wfreopen(TEXT(STDOUT_FILE),L"w",stdout);
        _wfreopen(TEXT(STDERR_FILE),L"w",stderr);
    #endif
#endif
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(J)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify",    "()V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    if (windowCreatedID == NULL ||
        sizeChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL ||
        sendMouseEventID == NULL ||
        sendKeyEventID == NULL) {
        DBG_PRINT( "initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_kd_KDWindow_CreateWindow
  (JNIEnv *env, jobject obj, jlong display, jintArray jAttrs)
{
    jint * attrs = NULL;
    jsize attrsLen;
    EGLDisplay dpy  = (EGLDisplay)(intptr_t)display;
    KDWindow *window = 0;

    if(dpy==NULL) {
        fprintf(stderr, "[CreateWindow] invalid display connection..\n");
        return 0;
    }

    attrsLen = (*env)->GetArrayLength(env, jAttrs);
    if(0==attrsLen) {
        fprintf(stderr, "[CreateWindow] attribute array size 0..\n");
        return 0;
    }
    attrs = (*env)->GetIntArrayElements(env, jAttrs, 0);
    if(NULL==attrs) {
        fprintf(stderr, "[CreateWindow] attribute array NULL..\n");
        return 0;
    }

    JOGLKDUserdata * userData = kdMalloc(sizeof(JOGLKDUserdata));
    userData->magic = JOGL_KD_USERDATA_MAGIC;
    window = kdCreateWindow(dpy, attrs, (void *)userData);

    (*env)->ReleaseIntArrayElements(env, jAttrs, attrs, 0);

    if(NULL==window) {
        kdFree(userData);
        fprintf(stderr, "[CreateWindow] failed: 0x%X\n", kdGetError());
    } else {
        userData->javaWindow = (*env)->NewGlobalRef(env, obj);
        userData->kdWindow = window;
        (*env)->CallVoidMethod(env, obj, windowCreatedID, (jlong) (intptr_t) userData);
        DBG_PRINT( "[CreateWindow] ok: %p, userdata %p\n", window, userData);
    }
    return (jlong) (intptr_t) window;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_kd_KDWindow_RealizeWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    EGLNativeWindowType nativeWindow=0;

    jint res = kdRealizeWindow(w, &nativeWindow);
    if(res) {
        fprintf(stderr, "[RealizeWindow] failed: 0x%X, 0x%X\n", res, kdGetError());
        nativeWindow = 0;
    }
    DBG_PRINT( "[RealizeWindow] ok: %p\n", nativeWindow);
    return (jlong) (intptr_t) nativeWindow;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_kd_KDWindow_CloseWindow
  (JNIEnv *env, jobject obj, jlong window, jlong juserData)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    JOGLKDUserdata * userData = (JOGLKDUserdata*) (intptr_t) juserData;
    int res = kdDestroyWindow(w);
    (*env)->DeleteGlobalRef(env, userData->javaWindow);
    kdFree(userData);

    DBG_PRINT( "[CloseWindow] res: %d\n", res);
    return res;
}

/*
 * Class:     jogamp_newt_driver_kd_KDWindow
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_kd_KDWindow_setVisible0
  (JNIEnv *env, jobject obj, jlong window, jboolean visible)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = (visible==JNI_TRUE)?KD_TRUE:KD_FALSE;
    kdSetWindowPropertybv(w, KD_WINDOWPROPERTY_VISIBILITY, &v);
    DBG_PRINT( "[setVisible] v=%d\n", visible);
    (*env)->CallVoidMethod(env, obj, visibleChangedID, JNI_FALSE, visible); // FIXME: or defer=true ?
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_kd_KDWindow_setFullScreen0
  (JNIEnv *env, jobject obj, jlong window, jboolean fullscreen)
{
/** not supported, due to missing NV property ..
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDboolean v = fullscreen;

    int res = kdSetWindowPropertybv(w, KD_WINDOWPROPERTY_FULLSCREEN_NV, &v);
    DBG_PRINT( "[setFullScreen] v=%d, res=%d\n", fullscreen, res);
    (void)res;
*/
    (void)env;
    (void)obj;
    (void)window;
    (void)fullscreen;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_kd_KDWindow_setSize0
  (JNIEnv *env, jobject obj, jlong window, jint width, jint height)
{
    KDWindow *w = (KDWindow*) (intptr_t) window;
    KDint32 v[] = { width, height };

    int res = kdSetWindowPropertyiv(w, KD_WINDOWPROPERTY_SIZE, v);
    DBG_PRINT( "[setSize] v=%dx%d, res=%d\n", width, height, res);
    (void)res;

    (*env)->CallVoidMethod(env, obj, sizeChangedID, JNI_FALSE, (jint) width, (jint) height, JNI_FALSE);
}

