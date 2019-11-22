#include <stdlib.h>
#include <errno.h>
#include <string.h>

#include "jogamp_newt_driver_egl_gbm_DisplayDriver.h"
#include "jogamp_newt_driver_egl_gbm_ScreenDriver.h"
#include "jogamp_newt_driver_egl_gbm_WindowDriver.h"

#include <xf86drm.h>
#include <xf86drmMode.h>
#include <gbm.h>

#define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif
#define ERR_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 

typedef struct {
    int fd; // drmClose
    drmModeRes *resources; // drmModeFreeResources
    drmModeConnector *connector; // drmModeFreeConnector
    drmModeEncoder *encoder; // drmModeFreeEncoder
    drmModeModeInfo *current_mode;
} DRM_HANDLE;

typedef struct {
    struct gbm_bo *bo;
    uint32_t fb_id;
} DRM_GBM_FB;

static jmethodID notifyScreenModeID = NULL;

/**
 * Display
 */

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
        if( NULL != drm->resources ) {
            drmModeFreeResources(drm->resources);
            drm->resources = NULL;
        }
        if( 0 <= drm->fd ) {
            drmClose(drm->fd);
            drm->fd = -1;
        }
        free(drm);
    }
}

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "EGL_GBM.Display initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_initDrm
  (JNIEnv *env, jclass clazz, jboolean verbose)
{
    static const char *modules[] = {
            "/dev/dri/card0", "i915", "radeon", "nouveau", "vmwgfx", "omapdrm", "exynos", "msm"
    };
    int module_count = sizeof(modules) / sizeof(const char*);
    const char * module_used = NULL;
    int i, j, area;
    DRM_HANDLE *drm = calloc(1, sizeof(DRM_HANDLE));

#ifdef VERBOSE_ON
    verbose = JNI_TRUE;
#endif

    if( verbose ) {
        ERR_PRINT( "EGL_GBM.Display initDrm start (testing %d modules)\n", module_count );
    }

    for (i = 0; i < module_count; i++) {
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
            break;
        }
    }
    if (drm->fd < 0) {
        ERR_PRINT("EGL_GBM.Display could not open drm device\n");
        goto error;
    }

    drm->resources = drmModeGetResources(drm->fd);
    if ( NULL == drm->resources ) {
        ERR_PRINT("EGL_GBM.Display drmModeGetResources failed on module %s: %s\n", 
            module_used, strerror(errno));
        goto error;
    }

    if( verbose ) {
        for (i = 0; i < drm->resources->count_connectors; i++) {
            drmModeConnector * c = drmModeGetConnector(drm->fd, drm->resources->connectors[i]);
            int chosen = DRM_MODE_CONNECTED == c->connection;
            ERR_PRINT( "EGL_GBM.Display Connector %d/%d chosen %d: id[con 0x%X, enc 0x%X], type %d[id 0x%X], connection %d, dim %dx%x mm, modes %d, encoders %d\n",
                i, drm->resources->count_connectors, chosen,
                c->connector_id, c->encoder_id, c->connector_type, c->connector_type_id, 
                c->connection, c->mmWidth, c->mmHeight, c->count_modes, c->count_encoders);
            drmModeFreeConnector(c);
        }
    }
    /* find a connected connector: */
    for (i = 0; i < drm->resources->count_connectors; i++) {
        drm->connector = drmModeGetConnector(drm->fd, drm->resources->connectors[i]);
        if( DRM_MODE_CONNECTED == drm->connector->connection ) {
            break;
        } else {
            drmModeFreeConnector(drm->connector);
            drm->connector = NULL;
        }
    }
    if( i >= drm->resources->count_connectors ) {
        /* we could be fancy and listen for hotplug events and wait for
         * a connector..
         */
        ERR_PRINT("EGL_GBM.Display no connected connector (connector count %d, module %s)!\n", 
            drm->resources->count_connectors, module_used);
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
        for (i = 0; i < drm->resources->count_encoders; i++) {
            drmModeEncoder * e = drmModeGetEncoder(drm->fd, drm->resources->encoders[i]);
            int chosen = e->encoder_id == drm->connector->encoder_id;
            ERR_PRINT( "EGL_GBM.Display Encoder %d/%d chosen %d: id 0x%X, type %d, crtc_id 0x%X, possible[crtcs %d, clones %d]\n",
                i, drm->resources->count_encoders, chosen,
                e->encoder_id, e->encoder_type, e->crtc_id,
                e->possible_crtcs, e->possible_clones);
            drmModeFreeEncoder(e);
        }
    }
    /* find encoder: */
    for (i = 0; i < drm->resources->count_encoders; i++) {
        drm->encoder = drmModeGetEncoder(drm->fd, drm->resources->encoders[i]);
        if( drm->encoder->encoder_id == drm->connector->encoder_id ) {
            break;
        } else {
            drmModeFreeEncoder(drm->encoder);
            drm->encoder = NULL;
        }
    }
    if ( i >= drm->resources->count_encoders ) {
        ERR_PRINT("EGL_GBM.Display no encoder (module %s)!\n", module_used);
        goto error;
    }

    if( verbose ) {
        DBG_PRINT( "EGL_GBM.Display initDrm end.X0 OK\n");
    }

    // drm->crtc_id = encoder->crtc_id;
    // drm->connector_id = connector->connector_id;
    return (jlong) (intptr_t) drm;

error:
    if( verbose ) {
        DBG_PRINT( "EGL_GBM.Display initDrm end.X2 ERROR\n");
    }
    freeDrm(drm);
    return 0;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_freeDrm
  (JNIEnv *env, jclass clazz, jlong jdrm) {
    freeDrm( (DRM_HANDLE*) (intptr_t) jdrm );
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_OpenGBMDisplay0
  (JNIEnv *env, jclass clazz, jlong jdrm)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    struct gbm_device * dev = gbm_create_device(drm->fd);
    return (jlong) (intptr_t) dev;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_CloseGBMDisplay0
  (JNIEnv *env, jclass clazz, jlong jgbm)
{
    struct gbm_device * dev = (struct gbm_device *) (intptr_t) jgbm;
    gbm_device_destroy(dev);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_DispatchMessages0
  (JNIEnv *env, jclass clazz)
{
}

/**
 * Screen
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_ScreenDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    notifyScreenModeID = (*env)->GetMethodID(env, clazz, "notifyScreenMode", "(III)V");
    if (notifyScreenModeID == NULL) {
        DBG_PRINT( "EGL_GBM.Screen initIDs FALSE\n" );
        return JNI_FALSE;
    }
    DBG_PRINT( "EGL_GBM.Screen initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_ScreenDriver_initNative
  (JNIEnv *env, jobject obj, jlong jdrm)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    uint32_t screen_width = 0;
    uint32_t screen_height = 0;
    uint32_t screen_vrefresh = 0;
    int32_t success = 0;

    if( NULL != drm ) {
        /**
        connector.modes.hdisplay; // width
        connector.modes.vdisplay; // height
        connector.modes.flags; // flags
        encoder.crtc_id; // crt_idx
        */
        screen_width = drm->current_mode->hdisplay;
        screen_height = drm->current_mode->vdisplay;
        screen_vrefresh = drm->current_mode->vrefresh;

        DBG_PRINT( "EGL_GBM.Screen initNative ok %dx%d @ %d\n", screen_width, screen_height, screen_vrefresh );
        (*env)->CallVoidMethod(env, obj, notifyScreenModeID, (jint) screen_width, (jint) screen_height, (jint) screen_vrefresh);
    } else {
        DBG_PRINT( "BCM.Screen initNative failed\n" );
    }
}

/**
 * Window
 */

JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_initIDs
  (JNIEnv *env, jclass clazz)
{
    DBG_PRINT( "EGL_GBM.Window initIDs ok\n" );
    return JNI_TRUE;
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, jlong jdrm, jlong jgbm, jint x, jint y, jint width, jint height, jboolean opaque, jint alphaBits)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    struct gbm_device *dev = (struct gbm_device *) (intptr_t) jgbm;

    struct gbm_surface *surface = gbm_surface_create(dev,
              drm->current_mode->hdisplay, drm->current_mode->vdisplay,
              opaque ? GBM_FORMAT_XRGB8888 : GBM_BO_FORMAT_ARGB8888,
              GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
    if ( NULL == surface ) {
        DBG_PRINT("failed to create gbm surface\n");
        return -1;
    }
    return (jlong) (intptr_t) surface;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    struct gbm_surface *surface = (struct gbm_surface *) (intptr_t) window;
    gbm_surface_destroy(surface);
}

