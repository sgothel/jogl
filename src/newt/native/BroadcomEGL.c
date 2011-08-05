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

#include <gluegen_stdint.h>

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "jogamp_newt_driver_broadcom_egl_Window.h"

#include "MouseEvent.h"
#include "KeyEvent.h"

#include <EGL/egl.h>

typedef unsigned int    GLuint;

EGLDisplay EGLUtil_CreateDisplayByNative( GLuint uiWidth, GLuint uiHeight );
void EGLUtil_DestroyDisplay( EGLDisplay eglDisplay );

EGLSurface EGLUtil_CreateWindowByNative( EGLDisplay eglDisplay, /* bool */ GLuint bChromakey, GLuint *puiWidth, GLuint *puiHeight );
void EGLUtil_DestroyWindow( EGLDisplay eglDisplay, EGLSurface eglSurface );
void EGLUtil_SwapWindow( EGLDisplay eglDisplay, EGLSurface eglSurface );

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif

static jmethodID windowCreatedID = NULL;

/**
 * Display
 */

JNIEXPORT void JNICALL Java_jogamp_newt_driver_broadcom_egl_Display_DispatchMessages
  (JNIEnv *env, jobject obj)
{
    // FIXME: n/a
    (void) env;
    (void) obj;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_broadcom_egl_Display_CreateDisplay
  (JNIEnv *env, jobject obj, jint width, jint height)
{
    (void) env;
    (void) obj;
    EGLDisplay dpy = EGLUtil_CreateDisplayByNative( (GLuint) width, (GLuint) height );
    if(NULL==dpy) {
        fprintf(stderr, "[CreateDisplay] failed: NULL\n");
    } else {
        DBG_PRINT( "[CreateDisplay] ok: %p, %ux%u\n", dpy, width, height);
    }
    return (jlong) (intptr_t) dpy;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_broadcom_egl_Display_DestroyDisplay
  (JNIEnv *env, jobject obj, jlong display)
{
    EGLDisplay dpy  = (EGLDisplay)(intptr_t)display;
    (void) env;
    (void) obj;
    DBG_PRINT( "[DestroyDisplay] dpy %p\n", dpy);

    EGLUtil_DestroyDisplay(dpy);

    DBG_PRINT( "[DestroyDisplay] X\n");
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_broadcom_egl_Window_initIDs
  (JNIEnv *env, jclass clazz)
{
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(III)V");
    if (windowCreatedID == NULL) {
        DBG_PRINT( "initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_broadcom_egl_Window_CreateWindow
  (JNIEnv *env, jobject obj, jlong display, jboolean chromaKey, jint width, jint height)
{
    EGLDisplay dpy  = (EGLDisplay)(intptr_t)display;
    EGLSurface window = 0;
    GLuint uiWidth=(GLuint)width, uiHeight=(GLuint)height;

    if(dpy==NULL) {
        fprintf(stderr, "[RealizeWindow] invalid display connection..\n");
        return 0;
    }
    DBG_PRINT( "[RealizeWindow.Create] dpy %p %ux%u\n", dpy, uiWidth, uiHeight);

    window = EGLUtil_CreateWindowByNative( dpy, chromaKey, &uiWidth, &uiHeight );

    if(NULL==window) {
        fprintf(stderr, "[RealizeWindow.Create] failed: NULL\n");
        return 0;
    }
    EGLint cfgID=0;
    if(EGL_FALSE==eglQuerySurface(dpy, window, EGL_CONFIG_ID, &cfgID)) {
        fprintf(stderr, "[RealizeWindow.ConfigID] failed: window %p\n", window);
        EGLUtil_DestroyWindow(dpy, window);
        return 0;
    }
    (*env)->CallVoidMethod(env, obj, windowCreatedID, (jint) cfgID, (jint)uiWidth, (jint)uiHeight);
    DBG_PRINT( "[RealizeWindow.Create] ok: win %p, cfgid %d, %ux%u\n", window, cfgID, uiWidth, uiHeight);

    // release and destroy already made context ..
    EGLContext ctx = eglGetCurrentContext();
    DBG_PRINT( "[RealizeWindow.Create] ctx %p - KEEP ALIVE \n", ctx);
    /*eglMakeCurrent(dpy,
                  EGL_NO_SURFACE,
                  EGL_NO_SURFACE,
                  EGL_NO_CONTEXT); */
    DBG_PRINT( "[RealizeWindow.Create] 2\n");
    // eglDestroyContext(dpy, ctx); // culprit ? FIXME ?
    DBG_PRINT( "[RealizeWindow.Create] 2 - eglDestroyContext - DISABLED - Duh ?\n");
    
    DBG_PRINT( "[RealizeWindow.Create] X\n");

    return (jlong) (intptr_t) window;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_broadcom_egl_Window_CloseWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    EGLDisplay dpy  = (EGLDisplay) (intptr_t) display;
    EGLSurface surf = (EGLSurface) (intptr_t) window;

    DBG_PRINT( "[CloseWindow] dpy %p, win %p\n", dpy, surf);

    EGLUtil_DestroyWindow(dpy, surf);

    DBG_PRINT( "[CloseWindow] X\n");
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_broadcom_egl_Window_SwapWindow
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    EGLDisplay dpy  = (EGLDisplay) (intptr_t) display;
    EGLSurface surf = (EGLSurface) (intptr_t) window;

    DBG_PRINT( "[SwapWindow] dpy %p, win %p\n", dpy, surf);

    EGLUtil_SwapWindow( dpy, surf );

    DBG_PRINT( "[SwapWindow] X\n");
}

