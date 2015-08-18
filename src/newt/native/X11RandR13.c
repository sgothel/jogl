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

#include "X11Screen.h"

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

    /* Bug 1183
     * XRRGetScreenResourcesCurrent (or XRRGetScreenResources)
     * _occasionally_ reports empty data
     * unless XRRGetScreenSizeRange has been called once.
     */
    int minWidth, minHeight, maxWidth, maxHeight;
    XRRGetScreenSizeRange ( dpy, root, &minWidth, &minHeight, &maxWidth, &maxHeight);

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

static void dumpOutput(const char *prefix, Display *dpy, int screen_idx, XRRScreenResources *resources, int outputIdx, RROutput output) {
    int i, j, primIdx=0;
    Window root = RootWindow(dpy, screen_idx);
    RROutput pxid = XRRGetOutputPrimary (dpy, root);
    int isPrim =0;
    if ( None != pxid && pxid == output ) {
        primIdx = i;
        isPrim = 1;
    }
    XRROutputInfo * xrrOutputInfo = XRRGetOutputInfo (dpy, resources, output);
    fprintf(stderr, "%s: Output[%d]: id %#lx, crtx 0x%lX, name %s (%d), %lux%lu, ncrtc %d, nclone %d, nmode %d (preferred %d), primary %d\n", 
        prefix, outputIdx, output, xrrOutputInfo->crtc, SAFE_STRING(xrrOutputInfo->name), xrrOutputInfo->nameLen, 
        xrrOutputInfo->mm_width, xrrOutputInfo->mm_height,
        xrrOutputInfo->ncrtc, xrrOutputInfo->nclone, xrrOutputInfo->nmode, xrrOutputInfo->npreferred, isPrim);
    for(j=0; j<xrrOutputInfo->ncrtc; j++) {
        fprintf(stderr, "%s: Output[%d].Crtc[%d].id %#lx\n", prefix, i, j, xrrOutputInfo->crtcs[j]);
    }
    for(j=0; j<xrrOutputInfo->nclone; j++) {
        fprintf(stderr, "%s: Output[%d].Clones[%d].id %#lx\n", prefix, i, j, xrrOutputInfo->clones[j]);
    }
    for(j=0; j<xrrOutputInfo->nmode; j++) {
        fprintf(stderr, "%s: Output[%d].Mode[%d].id %#lx\n", prefix, i, j, xrrOutputInfo->modes[j]);
    }
    XRRFreeOutputInfo (xrrOutputInfo);
}
static void dumpOutputs(const char *prefix, Display *dpy, int screen_idx, XRRScreenResources *resources, int noutput, RROutput * outputs) {
    int i;
    for(i=0; i<noutput; i++) {
        dumpOutput(prefix, dpy, screen_idx, resources, i, outputs[i]);
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
static RRCrtc findRRCrtc(XRRScreenResources *resources, RRCrtc crtc) {
    if( NULL != resources ) {
        int i;
        for(i=resources->ncrtc-1; i>=0; i--) {
            if( resources->crtcs[i] == crtc ) {
                return crtc;
            }
        }
    }
    return 0;
}
static XRRCrtcInfo* getXRRCrtcInfo(Display *dpy, XRRScreenResources *resources, RRCrtc _crtc) {
    RRCrtc crtc = findRRCrtc( resources, _crtc );
    if( 0 == crtc ) {
        return NULL;
    } else {
        return XRRGetCrtcInfo (dpy, resources, crtc);
    }
}
static XRRModeInfo* findMode(XRRScreenResources *resources, RRMode modeId) {
    if( NULL != resources ) {
        int i;
        for(i=resources->nmode-1; i>=0; i--) {
            XRRModeInfo *imode = &resources->modes[i];
            if( imode->id == modeId ) {
                return imode;
            }
        }
    }
    return NULL;
}

#include "xrandr_utils.c"

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
        fprintf(stderr, "Crtc[%d] %#lx: %d/%d %dx%d, rot 0x%X, mode.id %#lx\n", 
            i, crtc, xrrCrtcInfo->x, xrrCrtcInfo->y, xrrCrtcInfo->width, xrrCrtcInfo->height, xrrCrtcInfo->rotations, xrrCrtcInfo->mode);
        for(j=0; j<xrrCrtcInfo->noutput; j++) {
            fprintf(stderr, "    Crtc[%d].Output[%d].id %#lx\n", i, j, xrrCrtcInfo->outputs[j]);
            dumpOutput("        ", dpy, screen_idx, resources, j, xrrCrtcInfo->outputs[j]);
        }
        XRRFreeCrtcInfo(xrrCrtcInfo);
    }

    dumpOutputs("XRRScreenResources.outputs", dpy, (int)screen_idx, resources, resources->noutput, resources->outputs);

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
 * Method:    getMonitorDeviceIds0
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorDeviceIds0
  (JNIEnv *env, jclass clazz, jlong screenResources)
{
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    int ncrtc = ( NULL != resources ) ? resources->ncrtc : 0;
    jintArray properties = NULL;
    if( 0 < ncrtc ) {
        int crtcs[ncrtc];
        int i;
        for(i=0; i<ncrtc; i++) {
            crtcs[i] = (int)(intptr_t)resources->crtcs[i];
        }
        properties = (*env)->NewIntArray(env, ncrtc);
        if (properties == NULL) {
            NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", ncrtc);
        }
        (*env)->SetIntArrayRegion(env, properties, 0, ncrtc, crtcs);
    }
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    getMonitorInfoHandle0
 * Signature: (JIJI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_RandR13_getMonitorInfoHandle0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenResources, jint crt_id) 
{
    XRRCrtcInfo *xrrCrtcInfo = getXRRCrtcInfo((Display *)(intptr_t)display, 
                                              (XRRScreenResources *)(intptr_t)screenResources, 
                                              (RRCrtc)(intptr_t)crt_id );
    return (jlong)(intptr_t)xrrCrtcInfo;
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
 * Signature: (J)[I
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

    RRMode modeId = xrrCrtcInfo->mode;
    XRRModeInfo *mode = findMode(resources, modeId);
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
  (JNIEnv *env, jclass clazz, jlong display, jlong screenResources, jlong monitorInfo, jint crt_id)
{
    Display * dpy = (Display *) (intptr_t) display;
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    RRCrtc crtc = findRRCrtc( resources, (RRCrtc)(intptr_t)crt_id );
    if( 0 == crtc ) {
        // n/a
        return NULL;
    }
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;
    if( NULL == xrrCrtcInfo ) {
        // n/a
        return NULL;
    }
    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        return NULL;
    }

    Window root = RootWindow(dpy, 0); // FIXME screen_idx);
    RROutput pxid = XRRGetOutputPrimary (dpy, root);
    int isPrimary = 0;

    RROutput output = xrrCrtcInfo->outputs[0];
    if ( None != pxid && pxid == output ) {
        isPrimary = 1;
    }
    XRROutputInfo * xrrOutputInfo = XRRGetOutputInfo (dpy, resources, output);
    int numModes = xrrOutputInfo->nmode;

    jsize propCount = MIN_MONITOR_DEVICE_PROPERTIES - 1 + numModes;
    jint prop[ propCount ];
    int propIndex = 0;

    prop[propIndex++] = propCount;
    prop[propIndex++] = crt_id;
    prop[propIndex++] = 0; // isClone, does not work: 0 < xrrOutputInfo->nclone ? 1 : 0;
    prop[propIndex++] = isPrimary;
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
    prop[propIndex++] = xrrCrtcInfo->mode;   // current mode id
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
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenResources, 
                              jlong monitorInfo, jint crt_id, 
                              jint jmode_id, jint rotation, jint x, jint y)
{
    jboolean res = JNI_FALSE;
    Display * dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);
    XRRScreenResources *resources = (XRRScreenResources *) (intptr_t) screenResources;
    RRCrtc crtc = findRRCrtc( resources, (RRCrtc)(intptr_t)crt_id );
    if( 0 == crtc ) {
        // n/a
        DBG_PRINT("RandR13_setMonitorMode0.0: n/a: resources %p (%d), crt_id %#lx \n", 
            resources, (NULL == resources ? 0 : resources->ncrtc), (RRCrtc)(intptr_t)crt_id);
        return res;
    }
    XRRCrtcInfo *xrrCrtcInfo = (XRRCrtcInfo *) (intptr_t) monitorInfo;
    if( NULL == xrrCrtcInfo ) {
        // n/a
        DBG_PRINT("RandR13_setMonitorMode0.1: n/a: resources %p (%d), xrrCrtcInfo %p, crtc %#lx\n", 
            resources, (NULL == resources ? 0 : resources->ncrtc), xrrCrtcInfo, crtc);
        return res;
    }
    if( None == xrrCrtcInfo->mode || 0 == xrrCrtcInfo->noutput ) {
        // disabled
        DBG_PRINT("RandR13_setMonitorMode0: disabled: mode %d, noutput %d\n", xrrCrtcInfo->mode, xrrCrtcInfo->noutput);
        return res;
    }
    if( 0 >= jmode_id ) {
        // oops ..
        DBG_PRINT("RandR13_setMonitorMode0: inv. modeId: modeId %d\n", jmode_id);
        return res;
    }

    RRMode mode_id = (RRMode)(intptr_t)jmode_id;
    XRRModeInfo *mode_info = findMode(resources, mode_id);
    if( NULL == mode_info ) {
        // oops ..
        DBG_PRINT("RandR13_setMonitorMode0: inv. mode_id: mode_id %#lx\n", mode_id);
        return res;
    }
    if( 0 > x || 0 > y ) {
        x = xrrCrtcInfo->x;
        y = xrrCrtcInfo->y;
    }

    Rotation xrotation = NewtScreen_Degree2XRotation(env, rotation);
    int rot_change = xrrCrtcInfo->rotation != xrotation;
    DBG_PRINT("RandR13_setMonitorMode0: crt %#lx, noutput %d -> 0x%X, mode %#lx -> %#lx, pos %d / %d, rotation %d -> %d (change %d)\n", 
        crtc, xrrCrtcInfo->noutput, xrrCrtcInfo->outputs[0], xrrCrtcInfo->mode, mode_id,
        x, y, (int)xrrCrtcInfo->rotation, (int)xrotation, rot_change);

    XRRSelectInput (dpy, root, RRScreenChangeNotifyMask);
    Status status = RRSetConfigSuccess;
    int pre_fb_width=0, pre_fb_height=0;
    int fb_width=0, fb_height=0;
    int fb_width_mm=0, fb_height_mm=0;

    crtc_t *root_crtc = get_screen_size1(dpy, root, &fb_width, &fb_height,
                                         resources, crtc, xrrCrtcInfo, xrotation, x, y, mode_info);

    Bool fb_change = get_screen_sizemm(dpy, screen_idx, fb_width, fb_height,
                                       &fb_width_mm, &fb_height_mm,
                                       &pre_fb_width, &pre_fb_height);

    DBG_PRINT("RandR13_setMonitorMode0: crt %#lx, fb[change %d: %d x %d -> %d x %d [%d x %d mm]\n", 
        crtc, fb_change, pre_fb_width, pre_fb_height, fb_width, fb_height, fb_width_mm, fb_height_mm);
    if(fb_change) {
        // Disable CRTC first, since new size differs from current
        // and we shall avoid invalid intermediate configuration (see spec)!
        #if 0
        {
            // Disable all CRTCs (Not required!)
            crtc_t * iter_crtc;
            for(iter_crtc=root_crtc; RRSetConfigSuccess == status && NULL!=iter_crtc; iter_crtc=iter_crtc->next) {
                if( None == iter_crtc->mode_id || NULL == iter_crtc->mode_info || 0 == iter_crtc->crtc_info->noutput ) {
                    // disabled
                    continue;
                }
                status = XRRSetCrtcConfig (dpy, resources, iter_crtc->crtc_id, CurrentTime,
                                           0, 0, None, RR_Rotate_0, NULL, 0);
            }
        }
        #else
        status = XRRSetCrtcConfig (dpy, resources, crtc, CurrentTime,
                                   0, 0, None, RR_Rotate_0, NULL, 0);
        #endif
        DBG_PRINT("RandR13_setMonitorMode0: crt %#lx disable: %d -> %d\n", crtc, status, RRSetConfigSuccess == status);
        if( RRSetConfigSuccess == status ) {
            XRRSetScreenSize (dpy, root, fb_width, fb_height,
                              fb_width_mm, fb_height_mm);
            DBG_PRINT("RandR13_setMonitorMode0: crt %#lx screen-size\n", crtc);
        }
    }
    if( RRSetConfigSuccess == status ) {
        #if 0
        {
            // Enable/Set all CRTCs (Not required!)
            crtc_t * iter_crtc;
            for(iter_crtc=root_crtc; RRSetConfigSuccess == status && NULL!=iter_crtc; iter_crtc=iter_crtc->next) {
                if( None == iter_crtc->mode_id || NULL == iter_crtc->mode_info || 0 == iter_crtc->crtc_info->noutput ) {
                    // disabled
                    continue;
                }
                status = XRRSetCrtcConfig( dpy, resources, iter_crtc->crtc_id, CurrentTime, 
                                           iter_crtc->x, iter_crtc->y, iter_crtc->mode_id, iter_crtc->rotation,
                                           iter_crtc->crtc_info->outputs, iter_crtc->crtc_info->noutput );
            }
        }
        #else
        status = XRRSetCrtcConfig( dpy, resources, crtc, CurrentTime, 
                                   x, y, mode_id, xrotation,
                                   xrrCrtcInfo->outputs, xrrCrtcInfo->noutput );
        #endif
        DBG_PRINT("RandR13_setMonitorMode0: crt %#lx set-config: %d -> %d\n", crtc, status, RRSetConfigSuccess == status);
    }

    res = status == RRSetConfigSuccess;
    DBG_PRINT("RandR13_setMonitorMode0: FIN: %d -> ok %d\n", status, res);

    destroyCrtcChain(root_crtc, crtc);
    root_crtc=NULL;

    return res;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR13
 * Method:    sendRRScreenChangeNotify0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_RandR13_sendRRScreenChangeNotify0
  (JNIEnv *env, jclass clazz, jlong display, jlong jevent)
{
    Display * dpy = (Display *) (intptr_t) display;
    XEvent *event = (XEvent*)(intptr_t)jevent;
    XRRUpdateConfiguration(event);
    DBG_PRINT("RandR13_sendRRScreenChangeNotify0: dpy %p, event %p\n", dpy, event);
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

