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

#include "jogamp_newt_driver_intel_gdl_Display.h"
#include "jogamp_newt_driver_intel_gdl_Screen.h"
#include "jogamp_newt_driver_intel_gdl_Window.h"

#include "MouseEvent.h"
#include "KeyEvent.h"

#include <gdl.h>
#include <gdl_version.h>

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stdout, "*** INTEL-GDL: " __VA_ARGS__)
#else
    #define DBG_PRINT(...)
#endif

static jmethodID screenCreatedID = NULL;
static jmethodID updateBoundsID = NULL;

#define NUM_PLANES 5
static jobject newtWindows[NUM_PLANES] = { NULL, NULL, NULL, NULL, NULL } ;
static gdl_plane_id_t planes[NUM_PLANES]  = { GDL_PLANE_ID_UPP_A, GDL_PLANE_ID_UPP_B, GDL_PLANE_ID_UPP_C, GDL_PLANE_ID_UPP_D, GDL_PLANE_ID_UPP_E };

static int getWindowIdx(jobject win) {
    int i;
    for(i=0; i<NUM_PLANES && newtWindows[i]!=win; i++) ;
    return (i<NUM_PLANES)?i:-1;
}
static int getPlaneIdx(gdl_plane_id_t plane) {
    int i;
    for(i=0; i<NUM_PLANES && planes[i]!=plane; i++) ;
    return (i<NUM_PLANES)?i:-1;
}

static jobject getNewtWindow(gdl_plane_id_t plane) {
    int idx = getPlaneIdx(plane);
    if(idx>0) {
        return newtWindows[idx];
    }
    return NULL;
}
static gdl_plane_id_t getPlane(jobject win) {
    int idx = getWindowIdx(win);
    if(idx>0) {
        return planes[idx];
    }
    return GDL_PLANE_ID_UNDEFINED;
}

static gdl_plane_id_t allocPlane(JNIEnv *env, jobject newtWindow) {
    int i = getWindowIdx(NULL);
    if (i<NUM_PLANES) {
        newtWindows[i] = (*env)->NewGlobalRef(env, newtWindow);
        return planes[i];
    }
    return GDL_PLANE_ID_UNDEFINED;
}
static void freePlane(JNIEnv *env, gdl_plane_id_t plane) {
    int i = getPlaneIdx(plane);
    if (i<NUM_PLANES) {
        if(NULL!=newtWindows[i]) {
            (*env)->DeleteGlobalRef(env, newtWindows[i]);
            newtWindows[i] = NULL;;
        }
    }
}

static void JNI_ThrowNew(JNIEnv *env, const char *throwable, const char* message) {
    jclass throwableClass = (*env)->FindClass(env, throwable);
    if (throwableClass == NULL) {
        (*env)->FatalError(env, "Failed to load throwable class");
    }

    if ((*env)->ThrowNew(env, throwableClass, message) != 0) {
        (*env)->FatalError(env, "Failed to throw throwable");
    }
}


/**
 * Display
 */

JNIEXPORT void JNICALL Java_jogamp_newt_driver_intel_gdl_Display_DispatchMessages
  (JNIEnv *env, jobject obj, jlong displayHandle, jobject focusedWindow)
{
    // FIXME: n/a
    (void) env;
    (void) obj;
    (void) displayHandle;
    /**
    gdl_driver_info_t * p_driver_info = (gdl_driver_info_t *) (intptr_t) displayHandle;
    jobject newtWin = getNewtWindow(plane);
    if(NULL!=newtWin) {
        // here we can dispatch messages .. etc
    } */
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_intel_gdl_Display_CreateDisplay
  (JNIEnv *env, jobject obj)
{
    gdl_ret_t retval;
    gdl_driver_info_t * p_driver_info = NULL;

    (void) env;
    (void) obj;
    
    DBG_PRINT("[CreateDisplay]\n");

    retval = gdl_init(0);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_init");
        return (jlong)0;
    }

    p_driver_info = calloc(sizeof(gdl_driver_info_t), 1);
    retval = gdl_get_driver_info(p_driver_info);
    if (retval != GDL_SUCCESS) {
        free(p_driver_info);
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_get_driver_info");
        return (jlong)0;
    }
    DBG_PRINT("[gdl_get_driver_info: major %d minor %d vers %d build %d flags %x name %s size %d avail %d]\n",
              p_driver_info->header_version_major, p_driver_info->header_version_minor,
              p_driver_info->gdl_version, p_driver_info->build_tag, p_driver_info->flags,
              p_driver_info->name, p_driver_info->mem_size, p_driver_info->mem_avail);


    return (jlong) (intptr_t) p_driver_info;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_intel_gdl_Display_DestroyDisplay
  (JNIEnv *env, jobject obj, jlong displayHandle)
{
    gdl_driver_info_t * p_driver_info = (gdl_driver_info_t *) (intptr_t) displayHandle;
    (void) env;
    (void) obj;

    if(NULL!=p_driver_info) {
        gdl_close();
        free(p_driver_info);
    }

    DBG_PRINT("[DestroyDisplay] X\n");
}

/**
 * Screen
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_intel_gdl_Screen_initIDs
  (JNIEnv *env, jclass clazz)
{
    screenCreatedID = (*env)->GetMethodID(env, clazz, "screenCreated", "(II)V");
    if (screenCreatedID == NULL) {
        DBG_PRINT("initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT("initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_intel_gdl_Screen_GetScreenInfo
  (JNIEnv *env, jobject obj, jlong displayHandle, jint idx)
{
    gdl_driver_info_t * p_driver_info = (gdl_driver_info_t *) (intptr_t) displayHandle;
    gdl_display_info_t display_info;
    gdl_display_id_t id;
    gdl_ret_t retval;

    switch(idx) {
        case 1:     
            id = GDL_DISPLAY_ID_1; 
            break;
        default: 
            id = GDL_DISPLAY_ID_0;
    }

    retval = gdl_get_display_info(id, &display_info);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_get_display_info");
        return;
    }

    DBG_PRINT("[gdl_get_display_info: width %d height %d]\n",
              display_info.tvmode.width, display_info.tvmode.height);

    (*env)->CallVoidMethod(env, obj, screenCreatedID, (jint)display_info.tvmode.width, (jint)display_info.tvmode.height);
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_intel_gdl_Window_initIDs
  (JNIEnv *env, jclass clazz)
{
    updateBoundsID = (*env)->GetMethodID(env, clazz, "updateBounds", "(IIII)V");
    if (updateBoundsID == NULL) {
        DBG_PRINT("initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT("initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_intel_gdl_Window_CreateSurface
  (JNIEnv *env, jobject obj, jlong displayHandle, jint scr_width, jint scr_height, jint x, jint y, jint width, jint height) {

    gdl_driver_info_t * p_driver_info = (gdl_driver_info_t *) (intptr_t) displayHandle;
    gdl_ret_t retval;
    gdl_pixel_format_t pixelFormat = GDL_PF_ARGB_32;
    gdl_color_space_t colorSpace = GDL_COLOR_SPACE_RGB;
    gdl_rectangle_t srcRect, dstRect;

    (void) env;
    (void) obj;
    
    gdl_plane_id_t plane = allocPlane(env, obj);
    if(plane == GDL_PLANE_ID_UNDEFINED) {
        DBG_PRINT("CreateSurface failed, couldn't alloc plane\n" );
        return 0;
    }

    DBG_PRINT("[CreateSurface: screen %dx%d, win %d/%d %dx%d plane %d]\n", 
        scr_width, scr_height, x, y, width, height, plane);

    /** Overwrite - TEST - Check semantics of dstRect!
    x = 0; 
    y = 0;
    width = scr_width;
    height = scr_height; */

    srcRect.origin.x = x;
    srcRect.origin.y = y;
    srcRect.width = width;
    srcRect.height = height;

    dstRect.origin.x = x;
    dstRect.origin.y = y;
    dstRect.width = width;
    dstRect.height = height;

    retval = gdl_plane_reset(plane);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_reset");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_config_begin(plane);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_config_begin");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_SRC_COLOR_SPACE, &colorSpace);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr color space");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_PIXEL_FORMAT, &pixelFormat);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr  pixel format");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_DST_RECT, &dstRect);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr dstRect");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_SRC_RECT, &srcRect);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr srcRect");
        freePlane(env, plane);
        return (jlong)0;
    }

    retval = gdl_plane_config_end(GDL_FALSE);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_config_end");
        freePlane(env, plane);
        return (jlong)0;
    }

    (*env)->CallVoidMethod(env, obj, updateBoundsID, (jint)x, (jint)y, (jint)width, (jint)height);

    DBG_PRINT("[CreateSurface] returning plane %d\n", plane);
    
    return (jlong) (intptr_t) plane;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_intel_gdl_Window_CloseSurface
  (JNIEnv *env, jobject obj, jlong display, jlong surface)
{
    gdl_plane_id_t plane = (gdl_plane_id_t) (intptr_t) surface ;
    freePlane(env, plane);
    
    DBG_PRINT("[CloseSurface] plane %d\n", plane);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_intel_gdl_Window_SetBounds0
  (JNIEnv *env, jobject obj, jlong surface, jint scr_width, jint scr_height, jint x, jint y, jint width, jint height) {

    gdl_plane_id_t plane = (gdl_plane_id_t) (intptr_t) surface ;
    gdl_ret_t retval;
    gdl_rectangle_t srcRect, dstRect;

    (void) env;
    (void) obj;
    
    DBG_PRINT("[SetBounds0: screen %dx%d, win %d/%d %dx%d plane %d]\n", 
        scr_width, scr_height, x, y, width, height, plane);

    srcRect.origin.x = x;
    srcRect.origin.y = y;
    srcRect.width = width;
    srcRect.height = height;

    dstRect.origin.x = x;
    dstRect.origin.y = y;
    dstRect.width = width;
    dstRect.height = height;

    retval = gdl_plane_config_begin(plane);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_config_begin");
        return;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_DST_RECT, &dstRect);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr dstRect");
        return;
    }

    retval = gdl_plane_set_attr(GDL_PLANE_SRC_RECT, &srcRect);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_set_attr srcRect");
        return;
    }

    retval = gdl_plane_config_end(GDL_FALSE);
    if (retval != GDL_SUCCESS) {
        JNI_ThrowNew(env, "java/lang/IllegalStateException", "gdl_plane_config_end");
        return;
    }

    (*env)->CallVoidMethod(env, obj, updateBoundsID, (jint)x, (jint)y, (jint)width, (jint)height);

    DBG_PRINT("[SetBounds0] returning plane %d\n", plane);
}

