/**
 * Copyright 2011 JogAmp Community. All rights reserved.
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

#include "X11Common.h"

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getScreenResources0
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_RandR13_getScreenResources0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx) 
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);

    XRRScreenResources *res = XRRGetScreenResourcesCurrent( dpy, root); // 1.3
    // XRRScreenResources *res = XRRGetScreenResources( dpy, root); // 1.2

    return (jlong) (intptr_t) res;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    freeScreenResources0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_RandR13_freeScreenResources0
  (JNIEnv *env, jclass clazz, jlong screenResources) 
{
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    if( NULL != resources ) {
        XRRFreeScreenResources( resources );
    }
}

#define SAFE_STRING(s) (NULL==s?"":s)

static void dumpOutputs(const char *prefix, Display *dpy, XRRScreenResources *resources, int noutput, RROutput * outputs) {
    int i, j;
    fprintf(stderr, "%s %p: Output count %d\n", prefix, resources, noutput);
    for(i=0; i<noutput; i++) {
        RROutput output = outputs[i];
        XRROutputInfo * xrrOutputInfo = XRRGetOutputInfo (dpy, resources, output);
        fprintf(stderr, "  Output[%d]: id %#lx, crtx 0x%X, name %s (%d), %lux%lu, ncrtc %d, nclone %d, nmode %d (preferred %d)\n", 
            i, output, xrrOutputInfo->crtc, SAFE_STRING(xrrOutputInfo->name), xrrOutputInfo->nameLen, 
            xrrOutputInfo->mm_width, xrrOutputInfo->mm_height,
            xrrOutputInfo->ncrtc, xrrOutputInfo->nclone, xrrOutputInfo->nmode, xrrOutputInfo->npreferred);
        for(j=0; j<xrrOutputInfo->ncrtc; j++) {
            fprintf(stderr, "    Output[%d].Crtc[%d].id %#lx\n", i, j, xrrOutputInfo->crtcs[j]);
        }
        for(j=0; j<xrrOutputInfo->nclone; j++) {
            fprintf(stderr, "    Output[%d].Clones[%d].id %#lx\n", i, j, xrrOutputInfo->clones[j]);
        }
        for(j=0; j<xrrOutputInfo->nmode; j++) {
            fprintf(stderr, "    Output[%d].Mode[%d].id %#lx\n", i, j, xrrOutputInfo->modes[j]);
        }
        XRRFreeOutputInfo (xrrOutputInfo);
    }
}

/** Returns vertical refresh rate in hertz */
static float getVRefresh(XRRModeInfo *mode) {
    float rate;
    unsigned int vTotal = mode->vTotal;

    if (mode->modeFlags & RR_DoubleScan) {
        /* doublescan doubles the number of lines */
        vTotal *= 2;
    }

    if (mode->modeFlags & RR_Interlace) {
        /* interlace splits the frame into two fields */
        /* the field rate is what is typically reported by monitors */
        vTotal /= 2;
    }

    if (mode->hTotal && vTotal) {
        rate = ( (float) mode->dotClock /
                 ( (float) mode->hTotal * (float) vTotal )
               );
    } else {
        rate = 0;
    }
    return rate;
}


JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_RandR13_dumpInfo0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenResources)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    int pos[] = { 0, 0 } ;
    int i, j, minWidth, minHeight, maxWidth, maxHeight;

    int vs_width = DisplayWidth(dpy, screen_idx);
    int vs_height = DisplayHeight(dpy, screen_idx);
    int vs_width_mm = DisplayWidthMM(dpy, screen_idx);
    int vs_height_mm = DisplayHeightMM(dpy, screen_idx);
    fprintf(stderr, "ScreenVirtualSize: %dx%d %dx%d mm\n", vs_width, vs_height, vs_width_mm, vs_height_mm);

    XRRGetScreenSizeRange (dpy, root, &minWidth, &minHeight, &maxWidth, &maxHeight);
    fprintf(stderr, "XRRGetScreenSizeRange: %dx%d .. %dx%d\n", minWidth, minHeight, maxWidth, maxHeight);

    if( NULL == resources ) {
        fprintf(stderr, "XRRScreenResources NULL\n");
        return;
    }
    fprintf(stderr, "XRRScreenResources %p: Crtc count %d\n", resources, resources->ncrtc);
    for(i=0; i<resources->ncrtc; i++) {
        RRCrtc crtc = resources->crtcs[i];
        XRRCrtcInfo *xrrCrtcInfo = XRRGetCrtcInfo (dpy, resources, crtc);
        fprintf(stderr, "Crtc[%d]: %d/%d %dx%d, rot 0x%X, mode.id %#lx\n", 
            i, xrrCrtcInfo->x, xrrCrtcInfo->y, xrrCrtcInfo->width, xrrCrtcInfo->height, xrrCrtcInfo->rotations, xrrCrtcInfo->mode);
        for(j=0; j<xrrCrtcInfo->noutput; j++) {
            fprintf(stderr, "    Crtc[%d].Output[%d].id %#lx\n", i, j, xrrCrtcInfo->outputs[j]);
        }
        XRRFreeCrtcInfo(xrrCrtcInfo);
    }

    dumpOutputs("XRRScreenResources.outputs", dpy, resources, resources->noutput, resources->outputs);

    fprintf(stderr, "XRRScreenResources %p: Mode count %d\n", resources, resources->nmode);
    for(i=0; i<resources->nmode; i++) {
        XRRModeInfo *mode = &resources->modes[i];

        unsigned int dots = mode->hTotal * mode->vTotal;
        float refresh = getVRefresh(mode);
        fprintf(stderr, "Mode[%d, id %#lx]: %ux%u@%f, name %s\n", i, mode->id, mode->width, mode->height, refresh, SAFE_STRING(mode->name));
    }
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorDeviceCount0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorDeviceCount0
  (JNIEnv *env, jclass clazz, jlong screenResources)
{
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    return ( NULL != resources ) ? resources->ncrtc : 0;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorInfoHandle0
 * Signature: (JIJI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorInfoHandle0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenResources, jint crt_idx) 
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;

    if( NULL == resources || crt_idx >= resources->ncrtc ) {
        return 0;
    }
    RRCrtc crtc = resources->crtcs[crt_idx];
    XRRCrtcInfo *xrrCrtcInfo = XRRGetCrtcInfo (dpy, resources, crtc);

    return (jlong) (intptr_t) xrrCrtcInfo;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    freeMonitorInfoHandle0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_RandR13_freeMonitorInfoHandle0
  (JNIEnv *env, jclass clazz, jlong monitorInfo)
{
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;
    if( NULL != xrrCrtcInfo ) {
        XRRFreeCrtcInfo( xrrCrtcInfo );
    }
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getAvailableRotations0
 * Signature: (J)I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getAvailableRotations0
  (JNIEnv *env, jclass clazz, jlong monitorInfo)
{
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;
    if( NULL == xrrCrtcInfo ) {
        return NULL;
    }
    Rotation rotations_supported = xrrCrtcInfo->rotations;

    int num_rotations = 0;
    int rotations[4];
    if(0 != (rotations_supported & RR_Rotate_0)) {
      rotations[num_rotations++] = 0;
    }
    if(0 != (rotations_supported & RR_Rotate_90)) {
      rotations[num_rotations++] = 90;
    }
    if(0 != (rotations_supported & RR_Rotate_180)) {
      rotations[num_rotations++] = 180;
    }
    if(0 != (rotations_supported & RR_Rotate_270)) {
      rotations[num_rotations++] = 270;
    }

    jintArray properties = NULL;

    if(num_rotations>0) {
        properties = (*env)->NewIntArray(env, num_rotations);
        if (properties == NULL) {
            NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", num_rotations);
        }
        
        // move from the temp structure to the java structure
        (*env)->SetIntArrayRegion(env, properties, 0, num_rotations, rotations);
    }
        
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorViewport0
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorViewport0
  (JNIEnv *env, jclass clazz, jlong monitorInfo)
{
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;

    if( NULL == xrrCrtcInfo ) {
        // n/a
        return NULL;
    }

    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        return NULL;
    }

    jsize propCount = 4;
    jint prop[ propCount ];
    int propIndex = 0;

    prop[propIndex++] = xrrCrtcInfo->x;
    prop[propIndex++] = xrrCrtcInfo->y;
    prop[propIndex++] = xrrCrtcInfo->width;
    prop[propIndex++] = xrrCrtcInfo->height;

    jintArray properties = (*env)->NewIntArray(env, propCount);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", propCount);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, propCount, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorMode0
 * Signature: (JI)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorMode0
  (JNIEnv *env, jclass clazz, jlong screenResources, jint mode_idx)
{
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;

    if( NULL == resources || mode_idx >= resources->nmode ) {
        return NULL;
    }

    XRRModeInfo *mode = &resources->modes[mode_idx];
    unsigned int dots = mode->hTotal * mode->vTotal;
    int refresh = (int) ( getVRefresh(mode) * 100.0f ); // Hz * 100
    int flags = 0;
    if (mode->modeFlags & RR_Interlace) {
        flags |= FLAG_INTERLACE;
    }
    if (mode->modeFlags & RR_DoubleScan) {
        flags |= FLAG_DOUBLESCAN;
    }

    jint prop[ NUM_MONITOR_MODE_PROPERTIES_ALL ];
    int propIndex = 0;

    prop[propIndex++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
    prop[propIndex++] = mode->width;
    prop[propIndex++] = mode->height;
    prop[propIndex++] = 32; // TODO: XRandR > 1.4 may support bpp
    prop[propIndex++] = refresh;
    prop[propIndex++] = flags;
    prop[propIndex++] = mode->id;
    prop[propIndex++] = -1; // rotation placeholder

    jintArray properties = (*env)->NewIntArray(env, NUM_MONITOR_MODE_PROPERTIES_ALL);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", NUM_MONITOR_MODE_PROPERTIES_ALL);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, NUM_MONITOR_MODE_PROPERTIES_ALL, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorCurrentMode0
 * Signature: (JJ)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorCurrentMode0
  (JNIEnv *env, jclass clazz, jlong screenResources, jlong monitorInfo)
{
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;

    if( NULL == resources || NULL == xrrCrtcInfo ) {
        // n/a
        return NULL;
    }

    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        return NULL;
    }

    int modeId = xrrCrtcInfo->mode;
    XRRModeInfo *mode = NULL;
    int i;
    for(i=0; i<resources->nmode; i++) {
        XRRModeInfo *imode = &resources->modes[i];
        if( imode->id == modeId ) {
            mode = imode;
            break;
        }
    }
    if( NULL == mode ) {
        // oops ..
        return NULL;
    }

    unsigned int dots = mode->hTotal * mode->vTotal;
    int refresh = (int) ( getVRefresh(mode) * 100.0f ); // Hz * 100
    int flags = 0;
    if (mode->modeFlags & RR_Interlace) {
        flags |= FLAG_INTERLACE;
    }
    if (mode->modeFlags & RR_DoubleScan) {
        flags |= FLAG_DOUBLESCAN;
    }

    jint prop[ NUM_MONITOR_MODE_PROPERTIES_ALL ];
    int propIndex = 0;

    prop[propIndex++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
    prop[propIndex++] = mode->width;
    prop[propIndex++] = mode->height;
    prop[propIndex++] = 32; // TODO: XRandR > 1.4 may support bpp
    prop[propIndex++] = refresh;
    prop[propIndex++] = flags;
    prop[propIndex++] = mode->id;
    prop[propIndex++] = NewtScreen_XRotation2Degree(env, xrrCrtcInfo->rotation);

    jintArray properties = (*env)->NewIntArray(env, NUM_MONITOR_MODE_PROPERTIES_ALL);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", NUM_MONITOR_MODE_PROPERTIES_ALL);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, NUM_MONITOR_MODE_PROPERTIES_ALL, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorDevice0
 * Signature: (JJJJ)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorDevice0
  (JNIEnv *env, jclass clazz, jlong display, jlong screenResources, jlong monitorInfo, jint crt_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;

    if( NULL == resources || NULL == xrrCrtcInfo || crt_idx >= resources->ncrtc ) {
        // n/a
        return NULL;
    }

    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        return NULL;
    }

    RROutput output = xrrCrtcInfo->outputs[0];
    XRROutputInfo * xrrOutputInfo = XRRGetOutputInfo (dpy, resources, output);
    int numModes = xrrOutputInfo->nmode;

    jsize propCount = MIN_MONITOR_DEVICE_PROPERTIES - 1 + numModes;
    jint prop[ propCount ];
    int propIndex = 0;

    prop[propIndex++] = propCount;
    prop[propIndex++] = crt_idx;
    prop[propIndex++] = 0; // is_clone, does not work: 0 < xrrOutputInfo->nclone ? 1 : 0;
    prop[propIndex++] = xrrOutputInfo->mm_width;
    prop[propIndex++] = xrrOutputInfo->mm_height;
    prop[propIndex++] = xrrCrtcInfo->x;      // rotated viewport pixel units
    prop[propIndex++] = xrrCrtcInfo->y;      // rotated viewport pixel units
    prop[propIndex++] = xrrCrtcInfo->width;  // rotated viewport pixel units
    prop[propIndex++] = xrrCrtcInfo->height; // rotated viewport pixel units
    prop[propIndex++] = xrrCrtcInfo->x;      // rotated viewport window units (same)
    prop[propIndex++] = xrrCrtcInfo->y;      // rotated viewport window units (same)
    prop[propIndex++] = xrrCrtcInfo->width;  // rotated viewport window units (same)
    prop[propIndex++] = xrrCrtcInfo->height; // rotated viewport window units (same)
    prop[propIndex++] = xrrCrtcInfo->mode; // current mode id
    prop[propIndex++] = NewtScreen_XRotation2Degree(env, xrrCrtcInfo->rotation);
    int i;
    for(i=0; i<numModes; i++) {
        // avail modes ..
        prop[propIndex++] = xrrOutputInfo->modes[i];
    }

    XRRFreeOutputInfo (xrrOutputInfo);

    jintArray properties = (*env)->NewIntArray(env, propCount);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", propCount);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, propCount, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    setMonitorMode0
 * Signature: (JJJIIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_RandR13_setMonitorMode0
  (JNIEnv *env, jclass clazz, jlong display, jlong screenResources, jlong monitorInfo, jint crt_idx, jint modeId, jint rotation, jint x, jint y)
{
    Display * dpy = (Display *) (intptr_t) display;
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;
    jboolean res = JNI_FALSE;

    if( NULL == resources || NULL == xrrCrtcInfo || crt_idx >= resources->ncrtc ) {
        // n/a
        return res;
    }

    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        return res;
    }

    if( 0 >= modeId ) {
        // oops ..
        return res;
    }

    if( 0 > x || 0 > y ) {
        x = xrrCrtcInfo->x;
        y = xrrCrtcInfo->y;
    }

    Status status = XRRSetCrtcConfig( dpy, resources, resources->crtcs[crt_idx], CurrentTime, 
                                      x, y, modeId, NewtScreen_Degree2XRotation(env, rotation),
                                      xrrCrtcInfo->outputs, xrrCrtcInfo->noutput );
    res = status == RRSetConfigSuccess;

    return res;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    setScreenViewport0
 * Signature: (JIJIIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_RandR13_setScreenViewport0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenResources, jint x, jint y, jint width, jint height)
{
    Display * dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    jboolean res = JNI_FALSE;

    if( NULL == resources ) {
        // n/a
        return JNI_FALSE;
    }

    XRRSetScreenSize (dpy, root, width, height, DisplayWidthMM(dpy, screen_idx), DisplayHeightMM(dpy, screen_idx));
    return JNI_TRUE;
}


