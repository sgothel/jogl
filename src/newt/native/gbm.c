#include "jogamp_newt_driver_gbm_DisplayDriver.h"
#include "jogamp_newt_driver_gbm_ScreenDriver.h"
#include "jogamp_newt_driver_gbm_WindowDriver.h"

#include <xf86drm.h>
#include <xf86drmMode.h>
#include <gbm.h>

static struct {
	struct gbm_device *dev;
	struct gbm_surface *surface;
} gbm;

static struct {
	int fd;
	drmModeModeInfo *mode;
	uint32_t crtc_id;
	uint32_t connector_id;
} drm;

struct drm_fb {
	struct gbm_bo *bo;
	uint32_t fb_id;
};

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    initGbm
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_initGbm
  (JNIEnv *env, jobject this){
	static const char *modules[] = {
			"i915", "radeon", "nouveau", "vmwgfx", "omapdrm", "exynos", "msm"
	};
	drmModeRes *resources;
	drmModeConnector *connector = NULL;
	drmModeEncoder *encoder = NULL;
	int i, area;

	for (i = 0; i < ARRAY_SIZE(modules); i++) {
		printf("trying to load module %s...", modules[i]);
		drm.fd = drmOpen(modules[i], NULL);
		if (drm.fd < 0) {
			printf("failed.\n");
		} else {
			printf("success.\n");
			break;
		}
	}

	if (drm.fd < 0) {
		printf("could not open drm device\n");
		return -1;
	}

	resources = drmModeGetResources(drm.fd);
	if (!resources) {
		printf("drmModeGetResources failed: %s\n", strerror(errno));
		return -1;
	}

	/* find a connected connector: */
	for (i = 0; i < resources->count_connectors; i++) {
		connector = drmModeGetConnector(drm.fd, resources->connectors[i]);
		if (connector->connection == DRM_MODE_CONNECTED) {
			/* it's connected, let's use this! */
			break;
		}
		drmModeFreeConnector(connector);
		connector = NULL;
	}

	if (!connector) {
		/* we could be fancy and listen for hotplug events and wait for
		 * a connector..
		 */
		printf("no connected connector!\n");
		return -1;
	}

	/* find highest resolution mode: */
	for (i = 0, area = 0; i < connector->count_modes; i++) {
		drmModeModeInfo *current_mode = &connector->modes[i];
		int current_area = current_mode->hdisplay * current_mode->vdisplay;
		if (current_area > area) {
			drm.mode = current_mode;
			area = current_area;
		}
	}

	if (!drm.mode) {
		printf("could not find mode!\n");
		return -1;
	}

	/* find encoder: */
	for (i = 0; i < resources->count_encoders; i++) {
		encoder = drmModeGetEncoder(drm.fd, resources->encoders[i]);
		if (encoder->encoder_id == connector->encoder_id)
			break;
		drmModeFreeEncoder(encoder);
		encoder = NULL;
	}

	if (!encoder) {
		printf("no encoder!\n");
		return -1;
	}

	drm.crtc_id = encoder->crtc_id;
	drm.connector_id = connector->connector_id;

	gbm.dev = gbm_create_device(drm.fd);

	return gbm.dev;
  }

/*
 * Class:     jogamp_newt_driver_gbm_DisplayDriver
 * Method:    destroyDisplay
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_gbm_DisplayDriver_destroyDisplay
  (JNIEnv *env, jobject this){
  }

/*
 * Class:     jogamp_newt_driver_gbm_WindowDriver
 * Method:    createSurface
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_gbm_WindowDriver_createSurface
  (JNIEnv *env, jobject this){
      	gbm.surface = gbm_surface_create(gbm.dev,
      			drm.mode->hdisplay, drm.mode->vdisplay,
      			GBM_FORMAT_XRGB8888,
      			GBM_BO_USE_SCANOUT | GBM_BO_USE_RENDERING);
      	if (!gbm.surface) {
      		printf("failed to create gbm surface\n");
      		return -1;
      	}
      	return gbm.surface;
  }