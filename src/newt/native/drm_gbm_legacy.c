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

WEAK uint64_t
gbm_bo_get_modifier(struct gbm_bo *bo);

WEAK int
gbm_bo_get_plane_count(struct gbm_bo *bo);

WEAK uint32_t
gbm_bo_get_stride_for_plane(struct gbm_bo *bo, int plane);

WEAK uint32_t
gbm_bo_get_offset(struct gbm_bo *bo, int plane);

typedef struct {
    struct gbm_bo *bo;
    uint32_t fb_id;
} DRM_FB;

static void page_flip_handler(int fd, unsigned int frame,
          unsigned int sec, unsigned int usec, void *data)
{
    /* suppress 'unused parameter' warnings */
    (void)fd, (void)frame, (void)sec, (void)usec;

    int *waiting_for_flip = data;
    *waiting_for_flip = 0;
}

static drmEventContext drm_event_ctx = {
            .version = DRM_EVENT_CONTEXT_VERSION,
            .page_flip_handler = page_flip_handler,
    };

static void drm_fb_destroy_callback(struct gbm_bo *bo, void *data)
{
    struct gbm_device * gbmDev = gbm_bo_get_device(bo);
	int drm_fd = gbm_device_get_fd(gbmDev);
	DRM_FB *fb = data;

	if (fb->fb_id) {
		drmModeRmFB(drm_fd, fb->fb_id);
        fb->fb_id = 0;
        fb->bo = NULL;
    }

	free(fb);
}

static DRM_FB * drm_fb_get_from_bo2(int drmFd, struct gbm_bo *bo)
{
	DRM_FB *fb = gbm_bo_get_user_data(bo);
	uint32_t width, height, format,
		 strides[4] = {0}, handles[4] = {0},
		 offsets[4] = {0}, flags = 0;
	int ret = -1;

	if (fb) {
		return fb;
    }

	fb = calloc(1, sizeof *fb);
	fb->bo = bo;

	width = gbm_bo_get_width(bo);
	height = gbm_bo_get_height(bo);
	format = gbm_bo_get_format(bo);

	if (gbm_bo_get_modifier && gbm_bo_get_plane_count &&
	    gbm_bo_get_stride_for_plane && gbm_bo_get_offset) {

		uint64_t modifiers[4] = {0};
		modifiers[0] = gbm_bo_get_modifier(bo);
		const int num_planes = gbm_bo_get_plane_count(bo);
		for (int i = 0; i < num_planes; i++) {
			strides[i] = gbm_bo_get_stride_for_plane(bo, i);
			handles[i] = gbm_bo_get_handle(bo).u32;
			offsets[i] = gbm_bo_get_offset(bo, i);
			modifiers[i] = modifiers[0];
		}

		if (modifiers[0]) {
			flags = DRM_MODE_FB_MODIFIERS;
			DBG_PRINT("Using modifier %" PRIx64 "\n", modifiers[0]);
        }

        ret = drmModeAddFB2WithModifiers(drmFd, width, height,
                format, handles, strides, offsets,
                modifiers, &fb->fb_id, flags);
        if(ret) {
            ERR_PRINT("drmModeAddFB2WithModifiers failed!\n");
        } else {
            DBG_PRINT("drmModeAddFB2WithModifiers OK!\n");
        }
	}

	if (ret) {
		memcpy(handles, (uint32_t [4]){gbm_bo_get_handle(bo).u32,0,0,0}, 16);
		memcpy(strides, (uint32_t [4]){gbm_bo_get_stride(bo),0,0,0}, 16);
		memset(offsets, 0, 16);
		ret = drmModeAddFB2(drmFd, width, height, format,
				handles, strides, offsets, &fb->fb_id, 0);
        if(ret) {
            ERR_PRINT("drmModeAddFB2 failed!\n");
        } else {
            DBG_PRINT("drmModeAddFB2 OK!\n");
        }
	}

	if (ret) {
		ERR_PRINT("failed to create fb: %s\n", strerror(errno));
		free(fb);
		return NULL;
	}

	gbm_bo_set_user_data(bo, fb, drm_fb_destroy_callback);

	return fb;
}

static DRM_FB * drm_fb_get_from_bo(int drmFd, struct gbm_bo *bo)
{
    DRM_FB *fb = gbm_bo_get_user_data(bo);
    uint32_t width, height, stride, handle;
    int ret;

    if (fb) {
        return fb;
    }

    fb = calloc(1, sizeof *fb);
    fb->bo = bo;

    width = gbm_bo_get_width(bo);
    height = gbm_bo_get_height(bo);
    stride = gbm_bo_get_stride(bo);
    handle = gbm_bo_get_handle(bo).u32;

    ret = drmModeAddFB(drmFd, width, height, 24, 32, stride, handle, &fb->fb_id);
    if (ret) {
        ERR_PRINT("failed to create fb: %s\n", strerror(errno));
        free(fb);
        return NULL;
    } else {
        DBG_PRINT("drmModeAddFB OK!\n");
    }

    gbm_bo_set_user_data(bo, fb, drm_fb_destroy_callback);

    return fb;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_FirstSwapSurface0
  (JNIEnv *env, jobject obj, jint drmFd, jint jcrtc_id, jint jsurfaceOffsetX, jint jsurfaceOffsetY, 
   jint jconnector_id, jobject jmode, jint jmode_byte_offset, jlong jgbmSurface)
{
    const uint32_t crtc_id = (uint32_t)jcrtc_id;
    const uint32_t surfaceOffsetX = (uint32_t)jsurfaceOffsetX;
    const uint32_t surfaceOffsetY = (uint32_t)jsurfaceOffsetY;
    uint32_t connector_id = (uint32_t)jconnector_id;
    drmModeModeInfo *drmMode = NULL;
    struct gbm_surface *gbmSurface = (struct gbm_surface *) (intptr_t) jgbmSurface;
    struct gbm_bo *nextBO = NULL;
    DRM_FB *fb = NULL;
    int ret;

    if ( NULL != jmode ) {
        const char * c1 = (const char *) (*env)->GetDirectBufferAddress(env, jmode);
        drmMode = (drmModeModeInfo *) (intptr_t) ( c1 + jmode_byte_offset );
    }
    nextBO = gbm_surface_lock_front_buffer(gbmSurface);
    fb = drm_fb_get_from_bo(drmFd, nextBO);
    if (!fb) {
        ERR_PRINT("Failed to get a new framebuffer BO (0)\n");
        return 0;
    }
    /**
     * Set Mode 
     *
     * Fails with x/y != 0: -28 No space left on device
     *   drmModeSetCrtc.0 failed to set mode: fd 26, crtc_id 0x27, fb_id 0x54, pos 10/10, conn_id 0x4d, curMode 1920x1080: -28 No space left on device
     *
     * See https://lists.freedesktop.org/archives/dri-devel/2014-February/053826.html:
     *
     * - The X,Y in drmModeSetCrtc does in fact specify the "source" offset into
     *   your framebuffer (not the destination on the CRTC) which is what I was looking for.
     *
     * - We were able to allocate both a Dumb buffer, and a GBM buffer the size of
     *   6 1920x1200 monitors in a 1x6 configuration, so basically we didn't have
     *   any memory issues with a single buffer that big.
     *
     * - drmModeSetCrtc worked with both Dumb and GBM buffers, and we didn't have
     *   to do anything special on our end, so it clearly is handling the tiling
     *   issues behind the scenes (woot).
     */
    ret = drmModeSetCrtc(drmFd, crtc_id, fb->fb_id, surfaceOffsetX, surfaceOffsetY,
                         &connector_id, 1, drmMode);
    if (ret) {
        ERR_PRINT("drmModeSetCrtc.0 failed to set mode: fd %d, crtc_id 0x%x, fb_id 0x%x (offset %d/%d), conn_id 0x%x, curMode %s: %d %s\n", 
            drmFd, crtc_id, fb->fb_id, surfaceOffsetX, surfaceOffsetY, connector_id, drmMode->name, ret, strerror(errno));
        return 0;
    }
    DBG_PRINT( "EGL_GBM.Window FirstSwapSurface0 nextBO %p, fd %d, crtc_id 0x%x, fb_id 0x%x (offset %d/%d), conn_id 0x%x, curMode %s\n", 
        nextBO, drmFd, crtc_id, fb->fb_id, surfaceOffsetX, surfaceOffsetY, connector_id, drmMode->name);
    return (jlong) (intptr_t) nextBO;
}

#ifdef VERBOSE_ON
static int nextSwapVerboseOnce = 1;
#endif

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_NextSwapSurface0
  (JNIEnv *env, jobject obj, jint drmFd, jint jcrtc_id, 
   jint jconnector_id, jobject jmode, jint jmode_byte_offset, 
   jlong jgbmSurface, jlong jlastBO, jint swapInterval)
{
    const uint32_t crtc_id = (uint32_t)jcrtc_id;
    const uint32_t connector_id = (uint32_t)jconnector_id;
    drmModeModeInfo *drmMode = NULL;
    struct gbm_surface *gbmSurface = (struct gbm_surface *) (intptr_t) jgbmSurface;
    struct gbm_bo *lastBO = (struct gbm_bo*) (intptr_t) jlastBO;
    struct gbm_bo *nextBO = NULL;
    DRM_FB *fbNext = NULL;
    int ret, waiting_for_flip = 1;
    fd_set fds;

    if ( NULL != jmode ) {
        const char * c1 = (const char *) (*env)->GetDirectBufferAddress(env, jmode);
        drmMode = (drmModeModeInfo *) (intptr_t) ( c1 + jmode_byte_offset );
    }
    nextBO = gbm_surface_lock_front_buffer(gbmSurface);
    fbNext = drm_fb_get_from_bo(drmFd, nextBO);
    if (!fbNext) {
        ERR_PRINT("Failed to get a new framebuffer BO (1)\n");
        return 0;
    }
    if( 0 != swapInterval) {
        // https://github.com/dvdhrm/docs/blob/master/drm-howto/modeset-vsync.c#L614
        ret = drmModePageFlip(drmFd, crtc_id, fbNext->fb_id,
                DRM_MODE_PAGE_FLIP_EVENT, &waiting_for_flip);
        if (ret) {
            ERR_PRINT("drmModePageFlip.1 failed to queue page flip: fd %d, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s: %p -> %p: %d %s\n", 
                drmFd, crtc_id, fbNext->fb_id, connector_id, drmMode->name, lastBO, nextBO, ret, strerror(errno));
            return 0;
        }

        while (waiting_for_flip) {
            FD_ZERO(&fds);
            FD_SET(drmFd, &fds);

            ret = select(drmFd + 1, &fds, NULL, NULL, NULL);
            if (ret < 0) {
                ERR_PRINT("drm.select: select err: %s\n", strerror(errno));
                return ret;
            } else if (ret == 0) {
                ERR_PRINT("drm.select: select timeout!\n");
                return -1;
            }
            drmHandleEvent(drmFd, &drm_event_ctx);
        }
    }

    /* release last buffer to render on again: */
    if( NULL != lastBO ) {
        gbm_surface_release_buffer(gbmSurface, lastBO);
    }

#ifdef VERBOSE_ON
    if( nextSwapVerboseOnce ) {
        nextSwapVerboseOnce = 0;
        DBG_PRINT( "EGL_GBM.Window NextSwapSurface0 swapInterval %d, bo %p -> %p, fd %d, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s\n", 
            swapInterval, lastBO, nextBO, drmFd, crtc_id, fbNext->fb_id, connector_id, drmMode->name);
    }
#endif
    return (jlong) (intptr_t) nextBO;
}

