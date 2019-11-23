#include "egl_gbm.h"

#include <EGL/egl.h>

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
            .version = 2,
            .page_flip_handler = page_flip_handler,
    };

static void drm_fb_destroy_callback(struct gbm_bo *bo, void *data)
{
    struct gbm_device * gbmDev = gbm_bo_get_device(bo);
	int drm_fd = gbm_device_get_fd(gbmDev);
	DRM_FB *fb = data;

	if (fb->fb_id)
		drmModeRmFB(drm_fd, fb->fb_id);

	free(fb);
}

static DRM_FB * drm_fb_get_from_bo(struct gbm_bo *bo)
{
    struct gbm_device * gbmDev = gbm_bo_get_device(bo);
	int drm_fd = gbm_device_get_fd(gbmDev);
	DRM_FB *fb = gbm_bo_get_user_data(bo);
	uint32_t width, height, format,
		 strides[4] = {0}, handles[4] = {0},
		 offsets[4] = {0}, flags = 0;
	int ret = -1;

	if (fb)
		return fb;

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

        ret = drmModeAddFB2WithModifiers(drm_fd, width, height,
                format, handles, strides, offsets,
                modifiers, &fb->fb_id, flags);
        if(ret) {
            ERR_PRINT("drmModeAddFB2WithModifiers failed!\n");
        } else {
            ERR_PRINT("drmModeAddFB2WithModifiers OK!\n");
        }
	}

	if (ret) {
		memcpy(handles, (uint32_t [4]){gbm_bo_get_handle(bo).u32,0,0,0}, 16);
		memcpy(strides, (uint32_t [4]){gbm_bo_get_stride(bo),0,0,0}, 16);
		memset(offsets, 0, 16);
		ret = drmModeAddFB2(drm_fd, width, height, format,
				handles, strides, offsets, &fb->fb_id, 0);
        if(ret) {
            ERR_PRINT("drmModeAddFB2 failed!\n");
        } else {
            ERR_PRINT("drmModeAddFB2 OK!\n");
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

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_FirstSwapSurface0
  (JNIEnv *env, jobject obj, jlong jdrm, jlong jgbmSurface, jlong jeglDisplay, jlong jeglSurface)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    struct gbm_surface *gbmSurface = (struct gbm_surface *) (intptr_t) jgbmSurface;
    EGLDisplay eglDisplay  = (EGLDisplay) (intptr_t) jeglDisplay;
    EGLSurface eglSurface = (EGLSurface) (intptr_t) jeglSurface;
    struct gbm_bo *nextBO = NULL;
    DRM_FB *fb = NULL;
    int ret;

    if ( EGL_TRUE != eglSwapBuffers(eglDisplay, eglSurface) ) {
        EGLint err = eglGetError(); 
        ERR_PRINT("Failed eglSwapBuffers, err 0x%x\n", err);
        return 0;
    }
    nextBO = gbm_surface_lock_front_buffer(gbmSurface);
    fb = drm_fb_get_from_bo(nextBO);
    if (!fb) {
        ERR_PRINT("Failed to get a new framebuffer BO\n");
        return 0;
    }
    /* set mode: */
    ret = drmModeSetCrtc(drm->fd, drm->encoder->crtc_id, fb->fb_id, 0, 0,
            &drm->connector->connector_id, 1, drm->current_mode);
            /**
            int drmModeSetCrtc(int fd, uint32_t crtcId, uint32_t bufferId,
                   uint32_t x, uint32_t y, uint32_t *connectors, int count,
                   drmModeModeInfoPtr mode);
              */

    if (ret) {
        ERR_PRINT("drmModeSetCrtc failed to set mode: fd %d, enc_id 0x%x, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s: %d %s\n", 
            drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, fb->fb_id, 
            drm->connector->connector_id, drm->current_mode->name, ret, strerror(errno));
        return 0;
    }
    DBG_PRINT( "EGL_GBM.Window FirstSwapSurface0 nextBO %p, fd %d, enc_id 0x%x, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s\n", 
        nextBO,
        drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, fb->fb_id, 
        drm->connector->connector_id, drm->current_mode->name);
    return (jlong) (intptr_t) nextBO;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_NextSwapSurface0
  (JNIEnv *env, jobject obj, jlong jdrm, jlong jgbmSurface, jlong jlastBO, jlong jeglDisplay, jlong jeglSurface)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    struct gbm_surface *gbmSurface = (struct gbm_surface *) (intptr_t) jgbmSurface;
    struct gbm_bo *lastBO = (struct gbm_bo*) (intptr_t) jlastBO, *nextBO = NULL;
    EGLDisplay eglDisplay  = (EGLDisplay) (intptr_t) jeglDisplay;
    EGLSurface eglSurface = (EGLSurface) (intptr_t) jeglSurface;
    DRM_FB *fb = NULL;
    int ret, waiting_for_flip = 1;
    fd_set fds;

    if ( EGL_TRUE != eglSwapBuffers(eglDisplay, eglSurface) ) {
        EGLint err = eglGetError(); 
        ERR_PRINT("Failed eglSwapBuffers, err 0x%x\n", err);
        return 0;
    }
    nextBO = gbm_surface_lock_front_buffer(gbmSurface);
    fb = drm_fb_get_from_bo(nextBO);
    if (!fb) {
        ERR_PRINT("Failed to get a new framebuffer BO\n");
        return 0;
    }

    /**
     * Here you could also update drm plane layers if you want
     * hw composition
     */
    ret = drmModePageFlip(drm->fd, drm->encoder->crtc_id, fb->fb_id,
            DRM_MODE_PAGE_FLIP_EVENT, &waiting_for_flip);
    if (ret) {
        ERR_PRINT("drmModePageFlip failed to queue page flip: fd %d, enc_id 0x%x, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s: %p -> %p: %d %s\n", 
            drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, fb->fb_id, 
            drm->connector->connector_id, drm->current_mode->name, 
            lastBO, nextBO, ret, strerror(errno));
        return 0;
    }

    while (waiting_for_flip) {
        FD_ZERO(&fds);
        FD_SET(0, &fds);
        FD_SET(drm->fd, &fds);

        ret = select(drm->fd + 1, &fds, NULL, NULL, NULL);
        if (ret < 0) {
            ERR_PRINT("select err: %s\n", strerror(errno));
            return ret;
        } else if (ret == 0) {
            ERR_PRINT("select timeout!\n");
            return -1;
        } else if (FD_ISSET(0, &fds)) {
            ERR_PRINT("user interrupted!\n");
            return 0;
        }
        drmHandleEvent(drm->fd, &drm_event_ctx);
    }

    /* release last buffer to render on again: */
    if( NULL != lastBO ) {
        gbm_surface_release_buffer(gbmSurface, lastBO);
    }

    DBG_PRINT( "EGL_GBM.Window NextSwapSurface0 %p -> %p\n", lastBO, nextBO);
    return (jlong) (intptr_t) nextBO;
}

