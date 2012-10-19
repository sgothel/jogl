/**
 * Copyright 2012 JogAmp Community. All rights reserved.
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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include "bcm_vc_iv.h"

#include "jogamp_newt_driver_bcm_vc_iv_DisplayDriver.h"
#include "jogamp_newt_driver_bcm_vc_iv_ScreenDriver.h"
#include "jogamp_newt_driver_bcm_vc_iv_WindowDriver.h"

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif

static jmethodID setScreenSizeID = NULL;

static jmethodID windowCreatedID = NULL;
static jmethodID sizeChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID sendKeyEventID = NULL;

/**
 * Display
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    bcm_host_init();
    // TODO:  bcm_host_deinit();
    DBG_PRINT( "BCM.Display initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_DispatchMessages
  (JNIEnv *env, jobject obj)
{
}

/**
 * Screen
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_bcm_vc_iv_ScreenDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    uint32_t screen_width;
    uint32_t screen_height;
    int32_t success = 0;

    setScreenSizeID = (*env)->GetMethodID(env, clazz, "setScreenSize", "(II)V");
    if (setScreenSizeID == NULL) {
        DBG_PRINT( "BCM.Screen initIDs FALSE\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "BCM.Screen initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_ScreenDriver_initNative
  (JNIEnv *env, jobject obj)
{
    uint32_t screen_width;
    uint32_t screen_height;
    int32_t success = 0;

    if( graphics_get_display_size(0 /* LCD */, &screen_width, &screen_height) >= 0 ) {
        DBG_PRINT( "BCM.Screen initNative ok %dx%d\n", screen_width, screen_height );
        (*env)->CallVoidMethod(env, obj, setScreenSizeID, (jint) screen_width, (jint) screen_height);
    } else {
        DBG_PRINT( "BCM.Screen initNative failed\n" );
    }
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    windowCreatedID = (*env)->GetMethodID(env, clazz, "windowCreated", "(J)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
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
    DBG_PRINT( "BCM.Window initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_CreateWindow
  (JNIEnv *env, jobject obj, jint width, jint height, jboolean opaque, jint alphaBits)
{
   int32_t success = 0;
   VC_RECT_T dst_rect;
   VC_RECT_T src_rect;

   dst_rect.x = 0;
   dst_rect.y = 0;
   dst_rect.width = width;
   dst_rect.height = height;
      
   src_rect.x = 0;
   src_rect.y = 0;
   src_rect.width = width << 16;
   src_rect.height = height << 16;   

   VC_DISPMANX_ALPHA_T dispman_alpha;
   memset(&dispman_alpha, 0x0, sizeof(VC_DISPMANX_ALPHA_T));

   if( JNI_TRUE == opaque ) {
       dispman_alpha.flags = DISPMANX_FLAGS_ALPHA_FIXED_ALL_PIXELS ;
       dispman_alpha.opacity = 0xFF;
       dispman_alpha.mask = 0;
   } else {
       dispman_alpha.flags = DISPMANX_FLAGS_ALPHA_FROM_SOURCE ;
       dispman_alpha.opacity = 0xFF;
       dispman_alpha.mask = 0xFF;
   }

   DISPMANX_DISPLAY_HANDLE_T dispman_display = vc_dispmanx_display_open( 0 /* LCD */);
   DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
   DISPMANX_ELEMENT_HANDLE_T dispman_element = vc_dispmanx_element_add ( dispman_update, dispman_display,
                                                                         0/*layer*/, &dst_rect, 0/*src*/,
                                                                         &src_rect, DISPMANX_PROTECTION_NONE, 
                                                                         &dispman_alpha /*alpha */, 0/*clamp*/, 0/*transform*/);

   EGL_DISPMANX_WINDOW_T * nativeWindowPtr = calloc(1, sizeof(EGL_DISPMANX_WINDOW_T));  
   nativeWindowPtr->element = dispman_element;
   nativeWindowPtr->width = width;
   nativeWindowPtr->height = height;

   vc_dispmanx_update_submit_sync( dispman_update );

   (*env)->CallVoidMethod(env, obj, visibleChangedID, JNI_FALSE, JNI_TRUE); // FIXME: or defer=true ?

   return (jlong) (intptr_t) nativeWindowPtr;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_RealizeWindow
  (JNIEnv *env, jobject obj, jlong window)
{
    return (jlong) (intptr_t) 0;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_CloseWindow
  (JNIEnv *env, jobject obj, jlong window, jlong juserData)
{
    EGL_DISPMANX_WINDOW_T * nativeWindowPtr = (EGL_DISPMANX_WINDOW_T *) (intptr_t) window ;
    free( nativeWindowPtr );
    return 0;
}

/*
 * Class:     jogamp_newt_driver_bcm_vc_iv_WindowDriver
 * Method:    setVisible0
 * Signature: (JJZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_setVisible0
  (JNIEnv *env, jobject obj, jlong window, jboolean visible)
{
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_setFullScreen0
  (JNIEnv *env, jobject obj, jlong window, jboolean fullscreen)
{
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_setSize0
  (JNIEnv *env, jobject obj, jlong window, jint width, jint height)
{
    // FIXME RESIZE (*env)->CallVoidMethod(env, obj, sizeChangedID, JNI_FALSE, (jint) width, (jint) height, JNI_FALSE);
}


