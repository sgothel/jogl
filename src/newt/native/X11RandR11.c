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
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getAvailableScreenRotations0
 * Signature: (JI)I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR11_getAvailableScreenRotations0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)scrn_idx);
    int num_rotations = 0;
    Rotation cur_rotation, rotations_supported;
    int rotations[4];
    int major, minor;

    rotations_supported = XRRRotations (dpy, (int)scrn_idx, &cur_rotation);

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
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getNumScreenResolution0
 * Signature: (JI)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_RandR11_getNumScreenResolutions0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
#ifdef DBG_PERF
    struct timespec t0, t1, td;
    long td_ms;
    timespec_now(&t0);
#endif
    
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "X11Screen_getNumScreenResolution0.1: %ld ms\n", td_ms); fflush(NULL);
#endif

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions
    
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "X11Screen_getNumScreenResolution0.2 (XRRSizes): %ld ms\n", td_ms); fflush(NULL);
#endif

    DBG_PRINT("getNumScreenResolutions0: %p:%d -> %d\n", dpy, (int)scrn_idx, num_sizes);

    return num_sizes;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getScreenResolutions0
 * Signature: (JII)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR11_getScreenResolution0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx, jint resMode_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    
    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions

    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }
 
    // Fill the properties in temp jint array
    int propIndex = 0;
    jint prop[4];
    
    prop[propIndex++] = xrrs[(int)resMode_idx].width; 
    prop[propIndex++] = xrrs[(int)resMode_idx].height;
    prop[propIndex++] = xrrs[(int)resMode_idx].mwidth; 
    prop[propIndex++] = xrrs[(int)resMode_idx].mheight;
    
    jintArray properties = (*env)->NewIntArray(env, 4);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", 4);
    }
    
    // move from the temp structure to the java structure
    (*env)->SetIntArrayRegion(env, properties, 0, 4, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getScreenRates0
 * Signature: (JII)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_RandR11_getScreenRates0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx, jint resMode_idx)
{
    Display *dpy = (Display *) (intptr_t) display;
    
    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)scrn_idx, &num_sizes); //get possible screen resolutions

    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }
 
    int num_rates;
    short *rates = XRRRates(dpy, (int)scrn_idx, (int)resMode_idx, &num_rates);
 
    jint prop[num_rates];
    int i;
    for(i=0; i<num_rates; i++) {
        prop[i] = (int) rates[i];
        /** fprintf(stderr, "rate[%d, %d, %d/%d]: %d\n", (int)scrn_idx, resMode_idx, i, num_rates, prop[i]); */
    }
    
    jintArray properties = (*env)->NewIntArray(env, num_rates);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", num_rates);
    }
    
    // move from the temp structure to the java structure
    (*env)->SetIntArrayRegion(env, properties, 0, num_rates, prop);
    
    return properties;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getScreenConfiguration0
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_RandR11_getScreenConfiguration0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx) 
{
    Display *dpy = (Display *) (intptr_t) display;
    Window root = RootWindow(dpy, (int)screen_idx);
#ifdef DBG_PERF
    struct timespec t0, t1, td;
    long td_ms;
    timespec_now(&t0);
#endif

#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "X11Screen_getScreenConfiguration0.1: %ld ms\n", td_ms); fflush(NULL);
#endif

    // get current resolutions and frequencies
    XRRScreenConfiguration  *conf = XRRGetScreenInfo(dpy, root);
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "X11Screen_getScreenConfiguration0.2 (XRRGetScreenInfo): %ld ms\n", td_ms); fflush(NULL);
#endif

    return (jlong) (intptr_t) conf;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    freeScreenConfiguration0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_x11_RandR11_freeScreenConfiguration0
  (JNIEnv *env, jclass clazz, jlong screenConfiguration) 
{
    XRRFreeScreenConfigInfo( (XRRScreenConfiguration *) (intptr_t) screenConfiguration );
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getCurrentScreenRate0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_RandR11_getCurrentScreenRate0
  (JNIEnv *env, jclass clazz, jlong screenConfiguration) 
{
    XRRScreenConfiguration *conf = (XRRScreenConfiguration *) (intptr_t) screenConfiguration;
    
    short original_rate = XRRConfigCurrentRate(conf);
    DBG_PRINT("getCurrentScreenRate0: %d\n", (int)original_rate);

    return (jint) original_rate;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getCurrentScreenRotation0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_RandR11_getCurrentScreenRotation0
  (JNIEnv *env, jclass clazz, jlong screenConfiguration)
{
    XRRScreenConfiguration *conf = (XRRScreenConfiguration *) (intptr_t) screenConfiguration;
    Rotation rotation;

    XRRConfigCurrentConfiguration(conf, &rotation);

    return NewtScreen_XRotation2Degree(env, rotation);
}


/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    getCurrentScreenResolutionIndex0
 * Signature: (J)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_RandR11_getCurrentScreenResolutionIndex0
  (JNIEnv *env, jclass clazz, jlong screenConfiguration)
{
   XRRScreenConfiguration *conf = (XRRScreenConfiguration *) (intptr_t) screenConfiguration;
  
   short original_rate = XRRConfigCurrentRate(conf);
   
   Rotation original_rotation;
   SizeID original_size_id = XRRConfigCurrentConfiguration(conf, &original_rotation);
   
   DBG_PRINT("getCurrentScreenResolutionIndex0: %d\n", (int)original_size_id);
   return (jint)original_size_id; 
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    setCurrentScreenModeStart0
 * Signature: (JIJIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_RandR11_setCurrentScreenModeStart0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong screenConfiguration, jint resMode_idx, jint freq, jint rotation)
{
    Display *dpy = (Display *) (intptr_t) display;
    XRRScreenConfiguration *conf = (XRRScreenConfiguration *) (intptr_t) screenConfiguration;
    Window root = RootWindow(dpy, (int)screen_idx);

    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)screen_idx, &num_sizes); //get possible screen resolutions
    
    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }

    DBG_PRINT("X11Screen.setCurrentScreenMode0: CHANGED TO %d: %d x %d PIXELS, %d Hz, %d degree\n", 
        resMode_idx, xrrs[resMode_idx].width, xrrs[resMode_idx].height, (int)freq, rotation);

    XRRSelectInput (dpy, root, RRScreenChangeNotifyMask);
    Rotation xrot = NewtScreen_Degree2XRotation(env, rotation);
    XRRSetScreenConfigAndRate(dpy, conf, root, (int)resMode_idx, xrot, (short)freq, CurrentTime);   
    return JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_x11_RandR11
 * Method:    sendRRScreenChangeNotify0
 * Signature: (JIJIII)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_x11_RandR11_sendRRScreenChangeNotify0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_idx, jlong jevent, jint resMode_idx, jint freq, jint rotation)
{
    Display * dpy = (Display *) (intptr_t) display;
    XEvent *event = (XEvent*)(intptr_t)jevent;
    XRRUpdateConfiguration(event);
    DBG_PRINT("RandR11_sendRRScreenChangeNotify0: dpy %p, event %p\n", dpy, event);
    if( -1 == resMode_idx || -1 == screen_idx ) {
        // No validation requested
        return JNI_FALSE;
    }
    // Validate numbers ..
    XRRScreenChangeNotifyEvent * scn_event = (XRRScreenChangeNotifyEvent *) event;
    int num_sizes;   
    XRRScreenSize *xrrs = XRRSizes(dpy, (int)screen_idx, &num_sizes); //get possible screen resolutions
    if( 0 > resMode_idx || resMode_idx >= num_sizes ) {
        NewtCommon_throwNewRuntimeException(env, "Invalid resolution index: ! 0 < %d < %d", resMode_idx, num_sizes);
    }

    jboolean done = JNI_FALSE;
    if(0 < scn_event->rotation ) { // All valid values greater zero: 0, 90, 180, 270, ..
        int rot = NewtScreen_XRotation2Degree(env, scn_event->rotation);
        DBG_PRINT( "XRANDR: event . RRScreenChangeNotify call(OK) %p (root %p) resIdx %d rot %d %dx%d\n", 
                    (void*)scn_event->window, (void*)scn_event->root, 
                    (int)scn_event->size_index, rot, 
                    scn_event->width, scn_event->height);
        // done = scn_event->size_index == resMode_idx; // not reliable ..
        if( rot == rotation && 
            scn_event->width == xrrs[resMode_idx].width && 
            scn_event->height == xrrs[resMode_idx].height ) {
            done = JNI_TRUE;
        }
    } else { // invalid .. skip
        DBG_PRINT( "XRANDR: event . RRScreenChangeNotify call(SKIP) %p (root %p) resIdx %d %dx%d\n", 
                    (void*)scn_event->window, (void*)scn_event->root, 
                    (int)scn_event->size_index,
                    scn_event->width, scn_event->height);
    }
    return done;
}


