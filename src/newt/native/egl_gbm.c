#include "egl_gbm.h"

static jmethodID notifyScreenModeID = NULL;

static jmethodID sizeChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID windowDestroyNotifyID = NULL;

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
    verbose = JNI_TRUE;
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

#if 1
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
    return (jlong) (intptr_t) drm;

error:
    if( verbose ) {
        DBG_PRINT( "EGL_GBM.Display initDrm end.X2 ERROR\n");
    }
    drmModeFreeResources(resources);
    resources = NULL;
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
    DBG_PRINT( "EGL_GBM.Display OpenGBMDisplay0 handle %p\n", dev);
    return (jlong) (intptr_t) dev;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_DisplayDriver_CloseGBMDisplay0
  (JNIEnv *env, jclass clazz, jlong jgbm)
{
    struct gbm_device * dev = (struct gbm_device *) (intptr_t) jgbm;
    DBG_PRINT( "EGL_GBM.Display CloseGBMDisplay0 handle %p\n", dev);
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

#ifndef DRM_FORMAT_MOD_LINEAR
    #define DRM_FORMAT_MOD_LINEAR 0
#endif

WEAK struct gbm_surface *
gbm_surface_create_with_modifiers(struct gbm_device *gbm,
                                  uint32_t width, uint32_t height,
                                  uint32_t format,
                                  const uint64_t *modifiers,
                                  const unsigned int count);

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_CreateWindow0
  (JNIEnv *env, jobject obj, jlong jdrm, jlong jgbm, jint x, jint y, jint width, jint height, jint visual_id)
{
    DRM_HANDLE *drm = (DRM_HANDLE*) (intptr_t) jdrm;
    struct gbm_device *dev = (struct gbm_device *) (intptr_t) jgbm;
    uint64_t modifier = DRM_FORMAT_MOD_LINEAR;
    struct gbm_surface *surface = NULL;

    if( gbm_surface_create_with_modifiers ) {
        surface = gbm_surface_create_with_modifiers(dev, width, height, visual_id, &modifier, 1);
    }
    if( NULL == surface ) {
        if( gbm_surface_create_with_modifiers ) {
            DBG_PRINT( "EGL_GBM.Window CreateWindow0 gbm_surface_create_with_modifiers failed\n");
        }
        surface = gbm_surface_create(dev, width, height, visual_id, GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
    }
    if ( NULL == surface ) {
        DBG_PRINT( "EGL_GBM.Window CreateWindow0 gbm_surface_create failed\n");
        return 0;
    }

    // Done in Java code ..
    // (*env)->CallVoidMethod(env, obj, visibleChangedID, JNI_TRUE);

    DBG_PRINT( "EGL_GBM.Window CreateWindow0 handle %p\n", surface);
    return (jlong) (intptr_t) surface;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_egl_gbm_WindowDriver_CloseWindow0
  (JNIEnv *env, jobject obj, jlong display, jlong window)
{
    struct gbm_surface *surface = (struct gbm_surface *) (intptr_t) window;
    DBG_PRINT( "EGL_GBM.Window CloseWindow0 handle %p\n", surface);
    gbm_surface_destroy(surface);
}

