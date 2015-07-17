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

/** 
 * See references in header file.
 */
#include "bcm_vc_iv.h"

#include "jogamp_newt_driver_bcm_vc_iv_DisplayDriver.h"
#include "jogamp_newt_driver_bcm_vc_iv_ScreenDriver.h"
#include "jogamp_newt_driver_bcm_vc_iv_WindowDriver.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

typedef struct {
 DISPMANX_ELEMENT_HANDLE_T handle; // magic BCM EGL position (EGL_DISPMANX_WINDOW_T)
 int width;                        // magic BCM EGL position (EGL_DISPMANX_WINDOW_T)
 int height;                       // magic BCM EGL position (EGL_DISPMANX_WINDOW_T)
 int x;
 int y;
 int32_t layer;
} BCM_ELEMENT_T;

typedef struct {
 DISPMANX_RESOURCE_HANDLE_T handle;
 VC_IMAGE_TYPE_T type;
 uint32_t native_image_handle;
} BCM_RESOURCE_T;

typedef struct {
   BCM_ELEMENT_T element;
   BCM_RESOURCE_T resource;
   int hotX, hotY;
} POINTER_ICON_T;

static jmethodID setScreenSizeID = NULL;

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;

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

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_OpenBCMDisplay0
  (JNIEnv *env, jclass clazz)
{
   DISPMANX_DISPLAY_HANDLE_T dispman_display = vc_dispmanx_display_open( 0 /* LCD */);
   DBG_PRINT( "BCM.Display Open %p\n", (void*)(intptr_t)dispman_display);
   return (jlong) (intptr_t) dispman_display;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_CloseBCMDisplay0
  (JNIEnv *env, jclass clazz, jlong display)
{
   DISPMANX_DISPLAY_HANDLE_T dispman_display = (DISPMANX_DISPLAY_HANDLE_T) (intptr_t) display;
   DBG_PRINT( "BCM.Display Close %p\n", (void*)(intptr_t)dispman_display);
   vc_dispmanx_display_close( dispman_display );
}


JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_DispatchMessages0
  (JNIEnv *env, jclass clazz)
{
}

static void bcm_moveTo(DISPMANX_ELEMENT_HANDLE_T element, uint32_t layer, int x, int y, int width, int height) {
   VC_RECT_T       src_rect;
   VC_RECT_T       dst_rect;
   uint32_t change_flags = DISPMANX_ELEMENT_CHANGE_DEST_RECT | DISPMANX_ELEMENT_CHANGE_SRC_RECT;
   DISPMANX_RESOURCE_HANDLE_T mask = 0;
   DISPMANX_TRANSFORM_T transform = 0;

   uint8_t opacity = 0;  // NOP

   dst_rect.x = x;
   dst_rect.y = y;
   dst_rect.width = width;
   dst_rect.height = height;

   src_rect.x = 0;
   src_rect.y = 0;
   src_rect.width = width << 16;
   src_rect.height = height << 16;

   DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
   vc_dispmanx_element_change_attributes( dispman_update,
                                          element,
                                          change_flags,
                                          layer,
                                          opacity,
                                          &dst_rect,
                                          &src_rect,
                                          mask,
                                          transform );
   vc_dispmanx_update_submit_sync( dispman_update );
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_CreatePointerIcon0
  (JNIEnv *env, jclass clazz, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, jint width, jint height, jint hotX, jint hotY)
{
    if( 0 == pixels ) {
        return 0;
    }
    int32_t success = 0;
    VC_RECT_T dst_rect;
    VC_RECT_T src_rect;
    int x = 0;
    int y = 0;
    int pitch = width * 4; // RGBA

    const unsigned char * pixelPtr = (const unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                            (*env)->GetDirectBufferAddress(env, pixels) : 
                                            (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );

    POINTER_ICON_T * p = calloc(1, sizeof(POINTER_ICON_T));
    p->hotX = hotX;
    p->hotY = hotY;
    p->element.layer = 2000;
    p->element.x = x;
    p->element.y = y;
    p->element.width = width;
    p->element.height = height;
    p->resource.type = VC_IMAGE_ARGB8888;   /* 32bpp with 8bit alpha at MS byte, with R, G, B (LS byte) */
    p->resource.handle = vc_dispmanx_resource_create( p->resource.type,
                                                     width,
                                                     height,
                                                     &(p->resource.native_image_handle) );

    dst_rect.x = x;
    dst_rect.y = y;
    dst_rect.width = width;
    dst_rect.height = height;
      
    vc_dispmanx_resource_write_data( p->resource.handle,
                                    p->resource.type,
                                    pitch,
                                    (void*)(intptr_t)(pixelPtr + pixels_byte_offset),
                                    &dst_rect );

    if ( JNI_FALSE == pixels_is_direct ) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
    }

    DBG_PRINT( "BCM.Display PointerIcon.Create PI %p, resource %p\n", p, (void*)(intptr_t)p->resource.handle);
    return (jlong) (intptr_t) p;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_DestroyPointerIcon0
  (JNIEnv *env, jclass clazz, jlong handle)
{
    POINTER_ICON_T * p = (POINTER_ICON_T *) (intptr_t) handle ;
    if( 0 == p ) {
        return; 
    }

    DBG_PRINT( "BCM.Display PointerIcon.Destroy.0 PI %p, resource %p, element %p\n", 
        p, (void*)(intptr_t)p->resource.handle, (void*)(intptr_t)p->element.handle);

    if( 0 != p->element.handle ) {
        DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
        vc_dispmanx_element_remove( dispman_update, p->element.handle );
        p->element.handle = 0;
        vc_dispmanx_update_submit_sync( dispman_update );
    }
    if( 0 != p->resource.handle ) {
        vc_dispmanx_resource_delete( p->resource.handle );
        p->resource.handle = 0;
    }
    free( p );
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_SetPointerIcon0
  (JNIEnv *env, jclass clazz, jlong display, jlong handle, jboolean enable, jint x, jint y)
{
    DISPMANX_DISPLAY_HANDLE_T dispman_display = (DISPMANX_DISPLAY_HANDLE_T) (intptr_t) display;
    POINTER_ICON_T * p = (POINTER_ICON_T *) (intptr_t) handle ;
    VC_RECT_T dst_rect;
    VC_RECT_T src_rect;

    if( 0 == dispman_display || NULL == p || 0 == p->resource.handle ) {
        return; 
    }

    DBG_PRINT( "BCM.Display PointerIcon.Set.0 %p, PI %p, resource %p, element %p - enable %d - %d/%d\n", 
        (void*)(intptr_t)display, p, (void*)(intptr_t)p->resource.handle, (void*)(intptr_t)p->element.handle, enable, x, y);

    if( enable ) {
        if( 0 != p->element.handle ) {
            return; 
        }

        p->element.x = x;
        p->element.y = y;
        dst_rect.x = p->element.x - p->hotX;
        dst_rect.y = p->element.y - p->hotY;
        dst_rect.width = p->element.width;
        dst_rect.height = p->element.height;
          
        src_rect.x = 0;
        src_rect.y = 0;
        src_rect.width = p->element.width << 16;
        src_rect.height = p->element.height << 16;   

        VC_DISPMANX_ALPHA_T dispman_alpha;
        memset(&dispman_alpha, 0x0, sizeof(VC_DISPMANX_ALPHA_T));
        dispman_alpha.flags = DISPMANX_FLAGS_ALPHA_FROM_SOURCE ;
        dispman_alpha.opacity = 0xFF;
        dispman_alpha.mask = 0xFF;

        DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
        p->element.handle = vc_dispmanx_element_add ( dispman_update, dispman_display,
                                              p->element.layer, &dst_rect, 
                                              p->resource.handle /*src*/,
                                              &src_rect, DISPMANX_PROTECTION_NONE, 
                                              &dispman_alpha /*alpha */, 0/*clamp*/, 0/*transform*/);
        vc_dispmanx_update_submit_sync( dispman_update );
    } else {
        // DISABLE
        if( 0 == p->element.handle ) {
            return; 
        }
        p->element.x = x;
        p->element.y = y;
        DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
        vc_dispmanx_element_remove( dispman_update, p->element.handle );
        p->element.handle = 0;
        vc_dispmanx_update_submit_sync( dispman_update );
    }
    DBG_PRINT( "BCM.Display PointerIcon.Set.X %p, PI %p, resource %p, element %p - enable %d - %d/%d\n", 
        (void*)(intptr_t)display, p, (void*)(intptr_t)p->resource.handle, (void*)(intptr_t)p->element.handle, enable, x, y);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_DisplayDriver_MovePointerIcon0
  (JNIEnv *env, jclass clazz, jlong handle, jint x, jint y)
{
    POINTER_ICON_T * p = (POINTER_ICON_T *) (intptr_t) handle ;

    if( NULL == p || 0 == p->element.handle ) {
        return; 
    }
    DBG_PRINT( "BCM.Display PointerIcon.Move.0 PI %p, resource %p, element %p - %d/%d\n", 
        p, (void*)(intptr_t)p->resource.handle, (void*)(intptr_t)p->element.handle, x, y);
    p->element.x = x;
    p->element.y = y;
    bcm_moveTo( p->element.handle, p->element.layer, p->element.x - p->hotX, p->element.y - p->hotY, p->element.width, p->element.height);
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
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL) {
        DBG_PRINT( "initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "BCM.Window initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, jlong display, jint layer, jint x, jint y, jint width, jint height, jboolean opaque, jint alphaBits)
{
   int32_t success = 0;
   VC_RECT_T dst_rect;
   VC_RECT_T src_rect;

   if( 0 == display ) {
       return 0;
   }
   dst_rect.x = x;
   dst_rect.y = y;
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

   DISPMANX_DISPLAY_HANDLE_T dispman_display = (DISPMANX_DISPLAY_HANDLE_T) (intptr_t) display;

   DBG_PRINT( "BCM.Display Window.Create.0 %p, %d/%d %dx%d, opaque %d, alphaBits %d, layer %d\n",
    (void*)(intptr_t)dispman_display, x, y, width, height, opaque, alphaBits, layer);

   BCM_ELEMENT_T * p = calloc(1, sizeof(BCM_ELEMENT_T));  
   DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
   p->layer = layer;
   p->x = x;
   p->y = y;
   p->width = width;
   p->height = height;
   p->handle = vc_dispmanx_element_add ( dispman_update, dispman_display,
                                         p->layer, &dst_rect, 0/*src*/,
                                         &src_rect, DISPMANX_PROTECTION_NONE, 
                                         &dispman_alpha /*alpha */, 0/*clamp*/, 0/*transform*/);

   vc_dispmanx_update_submit_sync( dispman_update );

   (*env)->CallVoidMethod(env, obj, visibleChangedID, JNI_FALSE, JNI_TRUE); // FIXME: or defer=true ?

   DBG_PRINT( "BCM.Display Window.Create.X %p, element %p\n", 
    (void*)(intptr_t)dispman_display, (void*)(intptr_t)p->handle);

   return (jlong) (intptr_t) p;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    DISPMANX_DISPLAY_HANDLE_T dispman_display = (DISPMANX_DISPLAY_HANDLE_T) (intptr_t) display;
    BCM_ELEMENT_T * p = (BCM_ELEMENT_T *) (intptr_t) window ;

    DBG_PRINT( "BCM.Display Window.Close %p, element %p\n", 
        (void*)(intptr_t)dispman_display, (void*)(intptr_t)p->handle);

    if( 0 == dispman_display || NULL == p || 0 == p->handle ) {
        return;
    }
    DISPMANX_UPDATE_HANDLE_T dispman_update = vc_dispmanx_update_start( 0 );
    vc_dispmanx_element_remove( dispman_update, p->handle );
    p->handle = 0;
    vc_dispmanx_update_submit_sync( dispman_update );
    free( p );
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_bcm_vc_iv_WindowDriver_reconfigure0
  (JNIEnv *env, jobject obj, jlong window, jint x, jint y, jint width, jint height, jint flags)
{
    BCM_ELEMENT_T * p = (BCM_ELEMENT_T *) (intptr_t) window ;

    if( NULL == p || 0 == p->handle ) {
        return; 
    }
    /***
        int isVisible = !TST_FLAG_CHANGE_VISIBILITY(flags) && TST_FLAG_IS_VISIBLE(flags) ;
        ...  
        see X11Window.c
     */

    int posChanged = p->x != x || p->y != y;
    int sizeChanged = p->width != width || p->height != height;
    p->x = x;
    p->y = y;
    p->width = width;
    p->height = height;

    DBG_PRINT( "BCM.Display Window.Reconfig %p, element %p - %d/%d %dx%d\n", 
        p, (void*)(intptr_t)p->handle, p->x, p->y, p->width, p->height);

    bcm_moveTo( p->handle, p->layer, p->x, p->y, p->width, p->height);
    if( posChanged ) {
        (*env)->CallVoidMethod(env, obj, positionChangedID, JNI_FALSE, x, y);
    }
    if( sizeChanged ) {
        (*env)->CallVoidMethod(env, obj, sizeChangedID, JNI_FALSE, width, height, JNI_FALSE);
    }
}

