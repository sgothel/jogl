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
#include <unistd.h>
#include <termios.h>

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;

/**
 * Display
 */

static int saved_stdin;

static void setNullStdin()
{
    saved_stdin = dup(fileno(stdin));
    if( 0 > saved_stdin ) {
        ERR_PRINT("setNullStdin dup failed: %d %s\n", saved_stdin, strerror(errno));
        return;
    }
    freopen("/dev/null", "r", stdin); // fake null stdin, closes stdin
    DBG_PRINT("setNullStdin done\n");
}

static void restoreStdin()
{
    int null_stdin = dup(fileno(stdin)); // copy to close after restore
    if( 0 > null_stdin ) {
        ERR_PRINT("restoreStdin.1 dup failed: %d %s\n", null_stdin, strerror(errno));
        return;
    }
    // restore
    int restored_stdin = dup2(saved_stdin, fileno(stdin));
    if( 0 > restored_stdin ) {
        ERR_PRINT("restoreStdin.2 dup2 failed: %d %s\n", restored_stdin, strerror(errno));
        return;
    }
    saved_stdin = -1;

    // cleanup stdin before it gets executed on the console
    tcdrain(restored_stdin);
    tcflush(restored_stdin, TCIFLUSH);

    close(null_stdin); // close fake null stdin
    close(restored_stdin); 
    DBG_PRINT("restoreStdin done\n");
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    setNullStdin();
    DBG_PRINT( "EGL_GBM.Display initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_Shutdown0
  (JNIEnv *env, jclass clazz)
{
    restoreStdin();
    DBG_PRINT( "EGL_GBM.Display shutdown ok\n" );
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_DispatchMessages0
  (JNIEnv *env, jclass clazz)
{
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_CreatePointerIcon0
  (JNIEnv *env, jclass clazz, jlong jgbmDevice, jobject jpixels, jint pixels_byte_offset, jboolean pixels_is_direct, 
   jint width, jint height, jint hotX, jint hotY)
{
    void *pixels0 = NULL;
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
        pixels0 = ( JNI_TRUE == pixels_is_direct ? 
                    (*env)->GetDirectBufferAddress(env, jpixels) : 
                    (*env)->GetPrimitiveArrayCritical(env, jpixels, NULL) );
        pixels  = (uint32_t *) ( ((char *) pixels0) + pixels_byte_offset );
    }

    bo = gbm_bo_create(gbmDevice, 64, 64, 
                       GBM_FORMAT_ARGB8888,
                       // GBM_FORMAT_BGRA8888,
                       GBM_BO_USE_CURSOR_64X64 | GBM_BO_USE_WRITE);
    if( NULL == bo ) {
        ERR_PRINT("cursor.cstr gbm_bo_create failed\n");
    } else {
        // align user data width x height -> 64 x 64
        memset(buf, 0, sizeof(buf)); // cleanup
        for(int i=0; i<height; i++) {
            memcpy(buf + i * 64, pixels + i * width, width * 4);
        }
        if ( gbm_bo_write(bo, buf, sizeof(buf)) < 0 ) {
            ERR_PRINT("cursor.cstr gbm_bo_write failed\n");
            gbm_bo_destroy(bo);
            bo = NULL;
        }
    }
    if ( JNI_FALSE == pixels_is_direct && NULL != jpixels ) {
        (*env)->ReleasePrimitiveArrayCritical(env, jpixels, pixels0, JNI_ABORT);  
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
  (JNIEnv *env, jobject obj, jint drmFd, jint jcrtc_id, jlong jcursor, jboolean enable, jint hotX, jint hotY, jint x, jint y)
{
    uint32_t crtc_id = (uint32_t)jcrtc_id;
    struct gbm_bo *bo = (struct gbm_bo *) (intptr_t) jcursor;
    uint32_t bo_handle = gbm_bo_get_handle(bo).u32;
    int ret;

    DBG_PRINT( "EGL_GBM.Screen SetPointerIcon0.0: bo %p, enable %d, hot %d/%d, pos %d/%d\n", bo, enable, hotX, hotY, x, y);
    if( enable ) {
        ret = drmModeSetCursor2(drmFd, crtc_id, bo_handle, 64, 64, hotX, hotY);
        if( ret ) {
            ERR_PRINT("SetCursor enable failed: %d %s\n", ret, strerror(errno));
        } else {
            ret = drmModeMoveCursor(drmFd, crtc_id, x, y);
            if( ret ) {
                ERR_PRINT("SetCursor move failed: %d %s\n", ret, strerror(errno));
            }
        }
    } else {
        ret = drmModeSetCursor2(drmFd, crtc_id, 0, 0, 0, 0, 0);
        if( ret ) {
            ERR_PRINT("SetCursor disable failed: %d %s\n", ret, strerror(errno));
        }
    }
    DBG_PRINT( "EGL_GBM.Screen SetPointerIcon0.X: bo %p, enable %d, hot %d/%d, pos %d/%d\n", bo, enable, hotX, hotY, x, y);
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

