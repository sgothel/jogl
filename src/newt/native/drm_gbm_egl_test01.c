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

#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <errno.h>
#include <string.h>
#include <fcntl.h>
#include <inttypes.h>

#include <xf86drm.h>
#include <xf86drmMode.h>
#include <gbm.h>

#define WEAK __attribute__((weak))

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif
#define ERR_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 


#define GL_GLEXT_PROTOTYPES 1
#include <GLES2/gl2.h>
#include <GLES2/gl2ext.h>
#include <EGL/egl.h>
#include <EGL/eglext.h>
#include <assert.h>

typedef struct {
    int fd; // drmClose
    drmModeConnector *connector; // drmModeFreeConnector
    drmModeEncoder *encoder; // drmModeFreeEncoder
    int crtc_index;

    drmModeModeInfo *current_mode;
} DRM_HANDLE;

static void freeDrm(DRM_HANDLE *drm) {
    if( NULL != drm ) {
        if( NULL != drm->encoder ) {
            drmModeFreeEncoder(drm->encoder);
            drm->encoder = NULL;
        }
        if( NULL != drm->connector ) {
            drmModeFreeConnector(drm->connector);
            drm->connector = NULL;
        }
        if( 0 <= drm->fd ) {
            drmClose(drm->fd);
            drm->fd = -1;
        }
        free(drm);
    }
}

static DRM_HANDLE * initDrm(int verbose)
{
    static const char *linux_dri_card0 = "/dev/dri/card0";
    static const char *modules[] = {
            "i915", "radeon", "nouveau", "vmwgfx", "omapdrm", "exynos", "msm"
    };
    int module_count = sizeof(modules) / sizeof(const char*);
    const char * module_used = NULL;
    int ret, i, j, area;
    DRM_HANDLE *drm = calloc(1, sizeof(DRM_HANDLE));
    drmModeRes *resources = NULL;

#ifdef VERBOSE_ON
    verbose = 1;
#endif

    if( verbose ) {
        ERR_PRINT( "EGL_GBM.Display initDrm start\n");
    }

    drm->fd = -1;

#if 1
    // try linux_dri_card0 first
    drm->fd = open(linux_dri_card0, O_RDWR);
    ERR_PRINT("EGL_GBM.Display trying to open '%s': success %d\n",
        linux_dri_card0, 0<=drm->fd);
#endif

    // try drm modules
    for (i = 0; 0>drm->fd && i < module_count; i++) {
        if( verbose ) {
            ERR_PRINT("EGL_GBM.Display trying to load module[%d/%d] %s...",
                i, module_count, modules[i]);
        }
        drm->fd = drmOpen(modules[i], NULL);
        if (drm->fd < 0) {
            if( verbose ) {
                ERR_PRINT("failed.\n");
            }
        } else {
            if( verbose ) {
                ERR_PRINT("success.\n");
            }
            module_used = modules[i];
        }
    }
    if (drm->fd < 0) {
        ERR_PRINT("EGL_GBM.Display could not open drm device\n");
        goto error;
    }

#if 0
    ret = drmSetMaster(drm->fd);
    if(ret) {
        //drmDropMaster(int fd);
        DBG_PRINT( "EGL_GBM.Display drmSetMaster fd %d: FAILED: %d %s\n",
            drm->fd, ret, strerror(errno));
    } else {
        DBG_PRINT( "EGL_GBM.Display drmSetMaster fd %d: OK\n", drm->fd);
    }
#endif

    resources = drmModeGetResources(drm->fd);
    if ( NULL == resources ) {
        ERR_PRINT("EGL_GBM.Display drmModeGetResources failed on module %s: %s\n",
            module_used, strerror(errno));
        goto error;
    }

    if( verbose ) {
        for (i = 0; i < resources->count_connectors; i++) {
            drmModeConnector * c = drmModeGetConnector(drm->fd, resources->connectors[i]);
            int chosen = DRM_MODE_CONNECTED == c->connection;
            ERR_PRINT( "EGL_GBM.Display Connector %d/%d chosen %d: id[con 0x%x, enc 0x%x], type %d[id 0x%x], connection %d, dim %dx%x mm, modes %d, encoders %d\n",
                i, resources->count_connectors, chosen,
                c->connector_id, c->encoder_id, c->connector_type, c->connector_type_id,
                c->connection, c->mmWidth, c->mmHeight, c->count_modes, c->count_encoders);
            drmModeFreeConnector(c);
        }
    }
    /* find a connected connector: */
    for (i = 0; i < resources->count_connectors; i++) {
        drm->connector = drmModeGetConnector(drm->fd, resources->connectors[i]);
        if( DRM_MODE_CONNECTED == drm->connector->connection ) {
            break;
        } else {
            drmModeFreeConnector(drm->connector);
            drm->connector = NULL;
        }
    }
    if( i >= resources->count_connectors ) {
        /* we could be fancy and listen for hotplug events and wait for
         * a connector..
         */
        ERR_PRINT("EGL_GBM.Display no connected connector (connector count %d, module %s)!\n",
            resources->count_connectors, module_used);
        goto error;
    }

    /* find highest resolution mode: */
    for (i = 0, j = -1, area = 0; i < drm->connector->count_modes; i++) {
        drmModeModeInfo *current_mode = &drm->connector->modes[i];
        int current_area = current_mode->hdisplay * current_mode->vdisplay;
        if (current_area > area) {
            drm->current_mode = current_mode;
            area = current_area;
            j = i;
        }
        if( verbose ) {
            ERR_PRINT( "EGL_GBM.Display Mode %d/%d (max-chosen %d): clock %d, %dx%d @ %d Hz, type %d, name <%s>\n",
                i, drm->connector->count_modes, j,
                current_mode->clock, current_mode->hdisplay, current_mode->vdisplay, current_mode->vrefresh,
                current_mode->type, current_mode->name);
        }
    }
    if ( NULL == drm->current_mode ) {
        ERR_PRINT("EGL_GBM.Display could not find mode (module %s)!\n", module_used);
        goto error;
    }

    if( verbose ) {
        for (i = 0; i < resources->count_encoders; i++) {
            drmModeEncoder * e = drmModeGetEncoder(drm->fd, resources->encoders[i]);
            int chosen = e->encoder_id == drm->connector->encoder_id;
            ERR_PRINT( "EGL_GBM.Display Encoder %d/%d chosen %d: id 0x%x, type %d, crtc_id 0x%x, possible[crtcs %d, clones %d]\n",
                i, resources->count_encoders, chosen,
                e->encoder_id, e->encoder_type, e->crtc_id,
                e->possible_crtcs, e->possible_clones);
            drmModeFreeEncoder(e);
        }
    }
    /* find encoder: */
    for (i = 0; i < resources->count_encoders; i++) {
        drm->encoder = drmModeGetEncoder(drm->fd, resources->encoders[i]);
        if( drm->encoder->encoder_id == drm->connector->encoder_id ) {
            break;
        } else {
            drmModeFreeEncoder(drm->encoder);
            drm->encoder = NULL;
        }
    }
    if ( i >= resources->count_encoders ) {
        ERR_PRINT("EGL_GBM.Display no encoder (module %s)!\n", module_used);
        goto error;
    }
    for (i = 0; i < resources->count_crtcs; i++) {
        if (resources->crtcs[i] == drm->encoder->crtc_id) {
            drm->crtc_index = i;
            break;
        }
    }

    drmModeFreeResources(resources);
    resources = NULL;

    if( verbose ) {
        DBG_PRINT( "EGL_GBM.Display initDrm end.X0 OK: fd %d, enc_id 0x%x, crtc_id 0x%x, conn_id 0x%x, curMode %s\n",
            drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, drm->connector->connector_id, drm->current_mode->name);
    }

    // drm->crtc_id = encoder->crtc_id;
    // drm->connector_id = connector->connector_id;
    return drm;

error:
    if( verbose ) {
        DBG_PRINT( "EGL_GBM.Display initDrm end.X2 ERROR\n");
    }
    drmModeFreeResources(resources);
    resources = NULL;
    freeDrm(drm);
    return NULL;
}

static int
match_config_to_visual(EGLDisplay egl_display,
               EGLint visual_id,
               EGLConfig *configs,
               int count)
{
    int i;

    ERR_PRINT("match_config_to_visual: visual_id 0x%x\n", visual_id);

    for (i = 0; i < count; ++i) {
        EGLint id;

        if (!eglGetConfigAttrib(egl_display,
                configs[i], EGL_NATIVE_VISUAL_ID,
                &id))
            continue;

        if (id == visual_id)
            return i;
    }

    return -1;
}
static EGLConfig egl_choose_config(EGLDisplay egl_display, EGLint visual_id)
{
    EGLint count = 0;
    EGLint matched = 0;
    EGLConfig *configs;
    EGLConfig res = NULL;
    int config_index = -1;

    EGLint n;
    EGLConfig config;

    static const EGLint attribs[] = {
        EGL_SURFACE_TYPE, EGL_WINDOW_BIT,
        EGL_RED_SIZE, 8,
        EGL_GREEN_SIZE, 8,
        EGL_BLUE_SIZE, 8,
        // EGL_ALPHA_SIZE, 0,
        EGL_DEPTH_SIZE, 16,
        // EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
        EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT_KHR,
        EGL_NONE
    };

    ERR_PRINT("egl_choose_config: visual_id 0x%x\n", visual_id);

    if (!eglGetConfigs(egl_display, NULL, 0, &count) || count < 1) {
        ERR_PRINT("No EGL configs to choose from.\n");
        return NULL;
    }
    configs = calloc(count, sizeof(EGLConfig));
    if (!configs) {
        return NULL;
    }

    if (!eglChooseConfig(egl_display, attribs, configs,
                  count, &matched) || !matched) {
        ERR_PRINT("No EGL configs with appropriate attributes.\n");
        goto out;
    }

    if (!visual_id) {
        config_index = 0;
    }

    if (config_index == -1) {
        config_index = match_config_to_visual(egl_display,
                              visual_id,
                              configs,
                              matched);
    }

    if (config_index != -1) {
        EGLint v;
        res = configs[config_index];

        if (eglGetConfigAttrib(egl_display, res, EGL_CONFIG_ID, &v)) {
            ERR_PRINT("eglConfig[vid 0x%x]: cfgID 0x%x\n", visual_id, v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_RENDERABLE_TYPE, &v)) {
            ERR_PRINT(".. EGL_RENDERABLE_TYPE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_RED_SIZE, &v)) {
            ERR_PRINT(".. EGL_RED_SIZE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_GREEN_SIZE, &v)) {
            ERR_PRINT(".. EGL_GREEN_SIZE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_BLUE_SIZE, &v)) {
            ERR_PRINT(".. EGL_BLUE_SIZE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_ALPHA_SIZE, &v)) {
            ERR_PRINT(".. EGL_ALPHA_SIZE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_STENCIL_SIZE, &v)) {
            ERR_PRINT(".. EGL_STENCIL_SIZE 0x%x\n", v);
        }
        if (eglGetConfigAttrib(egl_display, res, EGL_DEPTH_SIZE, &v)) {
            ERR_PRINT(".. EGL_DEPTH_SIZE 0x%x\n", v);
        }
    }

out:
    free(configs);
    ERR_PRINT("eglConfig[vid 0x%x]: config %p\n", visual_id, (void*)res);
    return res;
}

static EGLDisplay getPlatformEGLDisplay(struct gbm_device * gbmDevice) {
    PFNEGLGETPLATFORMDISPLAYEXTPROC get_platform_display = NULL;
    get_platform_display =
        (void *) eglGetProcAddress("eglGetPlatformDisplayEXT");
    assert(get_platform_display != NULL);

    return get_platform_display(EGL_PLATFORM_GBM_KHR, gbmDevice, NULL);
}

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

	if (fb->fb_id)
		drmModeRmFB(drm_fd, fb->fb_id);

	free(fb);
}

static DRM_FB * drm_fb_get_from_bo2(int drmFd, struct gbm_bo *bo)
{
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

        ret = drmModeAddFB2WithModifiers(drmFd, width, height,
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
		ret = drmModeAddFB2(drmFd, width, height, format,
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
        ERR_PRINT("drmModeAddFB OK!\n");
    }

    gbm_bo_set_user_data(bo, fb, drm_fb_destroy_callback);

    return fb;
}

typedef struct {
    uint32_t crtc_id;
    struct gbm_bo *bo;
	uint32_t bo_handle;
    int x, y;
} DRM_CURSOR;

static DRM_CURSOR *drm_create_cursor(int drmFd, struct gbm_device *gbmDevice, uint32_t crtc_id) 
{
    DRM_CURSOR * c = calloc(1, sizeof(DRM_CURSOR));
	uint32_t buf[64 * 64];
    int ret;

    c->crtc_id = crtc_id;
    c->bo = gbm_bo_create(gbmDevice, 64, 64, 
                          GBM_FORMAT_ARGB8888,
                          GBM_BO_USE_CURSOR_64X64 | GBM_BO_USE_WRITE);
    if( NULL == c->bo ) {
        ERR_PRINT("cursor.cstr gbm_bo_create failed\n");
        return NULL;
    }
    c->bo_handle = gbm_bo_get_handle(c->bo).u32;

    memset(buf, 255, sizeof buf); // white for now
    if ( gbm_bo_write(c->bo, buf, sizeof(buf)) < 0 ) {
        ERR_PRINT("cursor.cstr gbm_bo_write failed\n");
        return NULL;
    }
    ret = drmModeSetCursor(drmFd, c->crtc_id, c->bo_handle, 64, 64);
    if( ret ) {
        ERR_PRINT("cursor.cstr drmModeSetCursor failed: %d %s\n", ret, strerror(errno));
        return NULL;
    }

    return c;
}
static void drm_destroy_cursor(int drmFd, DRM_CURSOR *c) 
{
    int crtc_id = c->crtc_id;
    struct gbm_bo *bo = c->bo;
    int ret;
    c->crtc_id=-1;
    c->bo_handle=0;
    c->bo=NULL;
    ret = drmModeSetCursor(drmFd, crtc_id, 0, 0, 0);
    if( ret ) {
        ERR_PRINT("cursor.dstr drmModeSetCursor failed: %d %s\n", ret, strerror(errno));
    }
    gbm_bo_destroy(bo);
    free(c);
}
static int drm_move_cursor(int drmFd, DRM_CURSOR *c, int x, int y) 
{
	int ret;

    if( c->x != x || c->y != y ) {
        c->x = x;
        c->y = y;
        ret = drmModeMoveCursor(drmFd, c->crtc_id, x, y);
        if( ret ) {
            ERR_PRINT("cursor drmModeMoveCursor failed: %d %s\n", ret, strerror(errno));
            return ret;
        }
    }
    return 0;
}

// drm_public int drmModeSetCursor(int fd, uint32_t crtcId, uint32_t bo_handle, uint32_t width, uint32_t height);
// drm_public int drmModeSetCursor2(int fd, uint32_t crtcId, uint32_t bo_handle, uint32_t width, uint32_t height, int32_t hot_x, int32_t hot_y);
// drm_public int drmModeMoveCursor(int fd, uint32_t crtcId, int x, int y);

// #define USE_SURFACELESS 1
#ifdef USE_SURFACELESS
    #warning Using KHR_SURFACELESS
#endif /* USE_SURFACELESS */

// #define USE_EGL_DEFAULT_DISPLAY 1
#ifdef USE_EGL_DEFAULT_DISPLAY
    #warning Using EGL_DEFAULT_DISPLAY causing issues with Mesa GBM
#endif /* USE_EGL_DEFAULT_DISPLAY */

#if defined(USE_EGL_DEFAULT_DISPLAY) && !defined(USE_SURFACELESS)
    #error USE_EGL_DEFAULT_DISPLAY requires USE_SURFACELESS
#endif

int main(int argc, char *argv[])
{
	fd_set fds;
    DRM_HANDLE *drm = NULL;
    struct gbm_device * gbmDevice = NULL;
    uint32_t visualID = GBM_FORMAT_XRGB8888;
    struct gbm_surface *gbmSurface = NULL;
    EGLDisplay eglDisplay = NULL;
    EGLint major, minor;
    EGLConfig eglConfig = NULL;
    EGLSurface eglSurface = NULL;
    EGLContext glContext = NULL;
	struct gbm_bo *bo = NULL;
    DRM_FB * fb = NULL;
    DRM_CURSOR * cursor = NULL;
    int cursorX=0, cursorY=0;
	uint32_t i = 0;
	int ret;

    static const EGLint context_attribs[] = {
        EGL_CONTEXT_CLIENT_VERSION, 2,
        EGL_NONE
    };

#ifdef USE_SURFACELESS
    ERR_PRINT("Compiled _with__ USE_SURFACELESS\n");
#else
    ERR_PRINT("Compiled without USE_SURFACELESS\n");
#endif /* USE_SURFACELESS */

#ifdef USE_EGL_DEFAULT_DISPLAY
    ERR_PRINT("Compiled _with__ USE_EGL_DEFAULT_DISPLAY\n");
#else
    ERR_PRINT("Compiled without USE_EGL_DEFAULT_DISPLAY\n");
#endif /* USE_EGL_DEFAULT_DISPLAY */

    drm = initDrm(1);
	if (NULL == drm) {
		ERR_PRINT("failed to initialize DRM\n");
		return -1;
	}

    {
		ERR_PRINT("JOGL SharedResource GL Scanning Emulation.START\n");

#ifdef USE_EGL_DEFAULT_DISPLAY
        eglDisplay  = getPlatformEGLDisplay(EGL_DEFAULT_DISPLAY);
#else /* USE_EGL_DEFAULT_DISPLAY */
        gbmDevice = gbm_create_device(drm->fd);
        if (NULL == gbmDevice) {
            ERR_PRINT("failed to create GBM Device\n");
            return -1;
        }
        eglDisplay  = getPlatformEGLDisplay(gbmDevice);
#endif /* USE_EGL_DEFAULT_DISPLAY */

        if (!eglInitialize(eglDisplay, &major, &minor)) {
            ERR_PRINT("failed to initialize\n");
            return -1;
        }

        eglConfig = egl_choose_config(eglDisplay, visualID);
        if (NULL == eglConfig) {
            ERR_PRINT("failed to chose EGLConfig\n");
            return ret;
        }

#ifndef USE_SURFACELESS
        #ifdef USE_EGL_DEFAULT_DISPLAY
            #error USE_EGL_DEFAULT_DISPLAY requires USE_SURFACELESS
        #endif
        gbmSurface = gbm_surface_create(gbmDevice,
                drm->current_mode->hdisplay, drm->current_mode->vdisplay,
                visualID, GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
        if (NULL == gbmSurface) {
            ERR_PRINT("failed to create GBM Surface\n");
            return -1;
        }

        eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, gbmSurface, NULL);
        if (eglSurface == EGL_NO_SURFACE) {
            ERR_PRINT("failed to create egl surface\n");
            return -1;
        }
#endif /* USE_SURFACELESS */

        if (!eglBindAPI(EGL_OPENGL_ES_API)) {
            ERR_PRINT("failed to bind api EGL_OPENGL_ES_API\n");
            return -1;
        }
        glContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, context_attribs);
        if (glContext == NULL) {
            ERR_PRINT("failed to create glContext\n");
            return -1;
        }
#ifndef USE_SURFACELESS
        eglMakeCurrent(eglDisplay, eglSurface, eglSurface, glContext);
#else /* USE_SURFACELESS */
        eglMakeCurrent(eglDisplay, NULL, NULL, glContext);
#endif /* USE_SURFACELESS */

        ERR_PRINT("GL Version.0 \"%s\"\n", glGetString(GL_VERSION));
        ERR_PRINT("GL Renderer.0 \"%s\"\n", glGetString(GL_RENDERER));

        eglMakeCurrent(eglDisplay, NULL, NULL, NULL);
        eglDestroyContext(eglDisplay, glContext);

#ifndef USE_SURFACELESS
        eglDestroySurface(eglDisplay, eglSurface);
        gbm_surface_destroy(gbmSurface);
#endif /* USE_SURFACELESS */

        eglTerminate(eglDisplay);
#ifndef USE_EGL_DEFAULT_DISPLAY
        gbm_device_destroy(gbmDevice);
#endif /* USE_EGL_DEFAULT_DISPLAY */

        glContext = NULL;
        eglConfig = NULL;
        eglSurface = NULL;
        gbmSurface = NULL;
        eglDisplay = NULL;
        gbmDevice = NULL;
		ERR_PRINT("JOGL SharedResource GL Scanning Emulation.END\n");
    }

	FD_ZERO(&fds);
	FD_SET(0, &fds);
	FD_SET(drm->fd, &fds);

    gbmDevice = gbm_create_device(drm->fd);
	if (NULL == gbmDevice) {
		ERR_PRINT("failed to create GBM Device\n");
		return -1;
	}

    cursor = drm_create_cursor(drm->fd, gbmDevice, drm->encoder->crtc_id);
    drm_move_cursor(drm->fd, cursor, cursorX, cursorY);

    {
        eglDisplay  = getPlatformEGLDisplay(gbmDevice);

        if (!eglInitialize(eglDisplay, &major, &minor)) {
            ERR_PRINT("failed to initialize\n");
            return -1;
        }

        ERR_PRINT("Using display %p with EGL version %d.%d\n",
                eglDisplay, major, minor);

        ERR_PRINT("EGL Version \"%s\"\n", eglQueryString(eglDisplay, EGL_VERSION));
        ERR_PRINT("EGL Vendor \"%s\"\n", eglQueryString(eglDisplay, EGL_VENDOR));
        // ERR_PRINT("EGL Extensions \"%s\"\n", eglQueryString(eglDisplay, EGL_EXTENSIONS));
    }

    eglConfig = egl_choose_config(eglDisplay, visualID);
	if (NULL == eglConfig) {
		ERR_PRINT("failed to chose EGLConfig\n");
		return ret;
	}

    gbmSurface = gbm_surface_create(gbmDevice,
            drm->current_mode->hdisplay, drm->current_mode->vdisplay,
            visualID, GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
	if (NULL == gbmSurface) {
		ERR_PRINT("failed to create GBM Surface\n");
		return -1;
	}

    eglSurface = eglCreateWindowSurface(eglDisplay, eglConfig, gbmSurface, NULL);
    if (eglSurface == EGL_NO_SURFACE) {
        ERR_PRINT("failed to create egl surface\n");
        return -1;
    }

    if (!eglBindAPI(EGL_OPENGL_ES_API)) {
        ERR_PRINT("failed to bind api EGL_OPENGL_ES_API\n");
        return -1;
    }
    glContext = eglCreateContext(eglDisplay, eglConfig, EGL_NO_CONTEXT, context_attribs);
    if (glContext == NULL) {
        ERR_PRINT("failed to create glContext\n");
        return -1;
    }

    /* connect the glContext to the surface */
    eglMakeCurrent(eglDisplay, eglSurface, eglSurface, glContext);

    ERR_PRINT("GL Version \"%s\"\n", glGetString(GL_VERSION));
    ERR_PRINT("GL Renderer \"%s\"\n", glGetString(GL_RENDERER));
    // ERR_PRINT("GL Extensions: \"%s\"\n", glGetString(GL_EXTENSIONS));

	/* clear the color buffer */
	glClearColor(0.5, 0.5, 0.5, 1.0);
	glClear(GL_COLOR_BUFFER_BIT);

	eglSwapBuffers(eglDisplay, eglSurface);
    bo = gbm_surface_lock_front_buffer(gbmSurface);
    fb = drm_fb_get_from_bo(drm->fd, bo);
    if (!fb) {
        ERR_PRINT("Failed to get a new framebuffer BO\n");
        return -1;
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
        return -1;
    }
    DBG_PRINT( "drmModeSetCrtc OK bo %p, fd %d, enc_id 0x%x, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s\n", 
        bo,
        drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, fb->fb_id, 
        drm->connector->connector_id, drm->current_mode->name);

	while (1) {
		struct gbm_bo *next_bo;
		int waiting_for_flip = 1;

        cursorX += 1;
        cursorY += 1;
        if( cursorX >= drm->current_mode->hdisplay ) {
            cursorX = 0;
        }
        if( cursorY >= drm->current_mode->vdisplay ) {
            cursorY = 0;
        }
        drm_move_cursor(drm->fd, cursor, cursorX, cursorY);

        if( ++i > 255 ) { i = 0; }
        glClearColor(0.2f, 0.3f, (float)i/255.0f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT);

		eglSwapBuffers(eglDisplay, eglSurface);
		next_bo = gbm_surface_lock_front_buffer(gbmSurface);
		fb = drm_fb_get_from_bo(drm->fd, next_bo);
        if (!fb) {
            ERR_PRINT("Failed to get a new framebuffer next_bo\n");
            return -1;
        }

		/*
		 * Here you could also update drm plane layers if you want
		 * hw composition
		 */
        ret = drmModePageFlip(drm->fd, drm->encoder->crtc_id, fb->fb_id,
                DRM_MODE_PAGE_FLIP_EVENT, &waiting_for_flip);
        if (ret) {
            ERR_PRINT("drmModePageFlip failed to queue page flip: fd %d, enc_id 0x%x, crtc_id 0x%x, fb_id 0x%x, conn_id 0x%x, curMode %s: %p -> %p: %d %s\n", 
                drm->fd, drm->encoder->encoder_id, drm->encoder->crtc_id, fb->fb_id, 
                drm->connector->connector_id, drm->current_mode->name, 
                bo, next_bo, ret, strerror(errno));
            return -1;
        }

		while (waiting_for_flip) {
			ret = select(drm->fd + 1, &fds, NULL, NULL, NULL);
			if (ret < 0) {
				ERR_PRINT("select err: %s\n", strerror(errno));
				return ret;
			} else if (ret == 0) {
				ERR_PRINT("select timeout!\n");
				return -1;
			} else if (FD_ISSET(0, &fds)) {
				ERR_PRINT("user interrupted!\n");
				break;
			}
			drmHandleEvent(drm->fd, &drm_event_ctx);
		}

		/* release last buffer to render on again: */
		gbm_surface_release_buffer(gbmSurface, bo);
		bo = next_bo;
	}

    drm_destroy_cursor(drm->fd, cursor);
    cursor = NULL;

	return ret;
}


