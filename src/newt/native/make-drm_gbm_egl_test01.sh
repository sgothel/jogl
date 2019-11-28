#!/bin/sh

rm -f drm_gbm_egl_test01

# if defined(USE_EGL_DEFAULT_DISPLAY) && !defined(USE_SURFACELESS) -> ERROR
# -DUSE_SURFACELESS \
# -DUSE_EGL_DEFAULT_DISPLAY \
#

gcc \
    -DUSE_SURFACELESS \
    -I/usr/lib/jvm/java-11-openjdk-amd64/include/ \
    -I/usr/lib/jvm/java-11-openjdk-amd64/include/linux/ \
    -o drm_gbm_egl_test01 \
    drm_gbm_egl_test01.c \
    `pkg-config --cflags --libs libdrm` -lgbm -lEGL -lGLESv2

