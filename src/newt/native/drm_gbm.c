/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

#include "drm_gbm.h"

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;

/**
 * Display
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "EGL_GBM.Display initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_DispatchMessages0
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_CreatePointerIcon0
  (JNIEnv *env, jclass clazz, jlong jgbmDevice, jobject jpixels, jint pixels_byte_offset, jboolean pixels_is_direct, 
   jint width, jint height, jint hotX, jint hotY)
{
    const uint32_t *pixels = NULL;
    struct gbm_device * gbmDevice = (struct gbm_device *) (intptr_t) jgbmDevice;
    struct gbm_bo *bo = NULL;
	uint32_t bo_handle;
    uint32_t buf[64 * 64];
    int i, j;

    DBG_PRINT("cursor.cstr %dx%d %d/%d\n", width, height, hotX, hotY);

    if ( NULL == jpixels ) {
        ERR_PRINT("CreateCursor: null icon pixels\n");
        return 0;
    }
    if( 0 >= width || width > 64 || 0 >= height || height > 64 ) {
        ERR_PRINT("CreateCursor: icon must be of size [1..64] x [1..64]\n");
        return 0;
    }
    {
        const char * c1 = (const char *) ( JNI_TRUE == pixels_is_direct ? 
                           (*env)->GetDirectBufferAddress(env, jpixels) : 
                           (*env)->GetPrimitiveArrayCritical(env, jpixels, NULL) );
        pixels = (uint32_t *) ( c1 + pixels_byte_offset );
    }

    bo = gbm_bo_create(gbmDevice, 64, 64, 
                       GBM_FORMAT_ARGB8888,
                       // GBM_FORMAT_BGRA8888,
                       GBM_BO_USE_CURSOR_64X64 | GBM_BO_USE_WRITE);
    if( NULL == bo ) {
        ERR_PRINT("cursor.cstr gbm_bo_create failed\n");
        return 0;
    }

    // align user data width x height -> 64 x 64
    memset(buf, 0, sizeof(buf)); // cleanup
    for(int i=0; i<height; i++) {
        memcpy(buf + i * 64, pixels + i * width, width * 4);
    }
    if ( gbm_bo_write(bo, buf, sizeof(buf)) < 0 ) {
        ERR_PRINT("cursor.cstr gbm_bo_write failed\n");
        gbm_bo_destroy(bo);
        return 0;
    }
    return (jlong) (intptr_t) bo;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_DestroyPointerIcon0
  (JNIEnv *env, jclass clazz, jlong jcursor)
{
    struct gbm_bo *bo = (struct gbm_bo *) (intptr_t) jcursor;

    if ( NULL == bo ) {
        ERR_PRINT("DestroyCursor: null cursor\n");
        return;
    }
    gbm_bo_destroy(bo);
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_SetPointerIcon0
  (JNIEnv *env, jobject obj, jint drmFd, jint jcrtc_id, jlong jcursor, jboolean enable, jint x, jint y)
{
    uint32_t crtc_id = (uint32_t)jcrtc_id;
    struct gbm_bo *bo = (struct gbm_bo *) (intptr_t) jcursor;
    uint32_t bo_handle = gbm_bo_get_handle(bo).u32;
    int ret;

    // int drmModeSetCursor(int fd, uint32_t crtcId, uint32_t bo_handle, uint32_t width, uint32_t height);
    // int drmModeSetCursor2(int fd, uint32_t crtcId, uint32_t bo_handle, uint32_t width, uint32_t height, int32_t hot_x, int32_t hot_y);
    if( enable ) {
        ret = drmModeSetCursor(drmFd, crtc_id, bo_handle, 64, 64);
        if( ret ) {
            ERR_PRINT("SetCursor enable failed: %d %s\n", ret, strerror(errno));
        } else {
            ret = drmModeMoveCursor(drmFd, crtc_id, x, y);
            if( ret ) {
                ERR_PRINT("SetCursor move failed: %d %s\n", ret, strerror(errno));
            }
        }
    } else {
        ret = drmModeSetCursor(drmFd, crtc_id, 0, 0, 0);
        if( ret ) {
            ERR_PRINT("SetCursor disable failed: %d %s\n", ret, strerror(errno));
        }
    }
    return ret ? JNI_FALSE : JNI_TRUE;
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_MovePointerIcon0
  (JNIEnv *env, jobject obj, jint drmFd, jint jcrtc_id, jint x, jint y)
{
    uint32_t crtc_id = (uint32_t)jcrtc_id;
	int ret;

    ret = drmModeMoveCursor(drmFd, crtc_id, x, y);
    if( ret ) {
        ERR_PRINT("cursor drmModeMoveCursor failed: %d %s\n", ret, strerror(errno));
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

/**
 * Screen
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_ScreenDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "EGL_GBM.Screen initIDs ok\n" );
    return JNI_TRUE;
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    if (sizeChangedID == NULL ||
        positionChangedID == NULL ||
        visibleChangedID == NULL ||
        windowDestroyNotifyID == NULL) {
        DBG_PRINT( "initIDs failed\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "EGL_GBM.Window initIDs ok\n" );
    return JNI_TRUE;
}

