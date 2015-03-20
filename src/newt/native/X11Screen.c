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

// #define VERBOSE_ON 1
// #define DBG_PERF 1

#include "X11Screen.h"

#ifdef DBG_PERF
    #include "timespec.h"
#endif

/*
 * Class:     jogamp_newt_driver_x11_ScreenDriver
 * Method:    GetScreen
 * Signature: (JI)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_x11_ScreenDriver_GetScreen0
  (JNIEnv *env, jclass clazz, jlong display, jint screen_index)
{
    Display * dpy = (Display *)(intptr_t)display;
    Screen  * scrn= NULL;

    DBG_PRINT("X11: X11Screen_GetScreen0 dpy %p START\n", dpy);

    if(dpy==NULL) {
        NewtCommon_FatalError(env, "invalid display connection..");
    }

    scrn = ScreenOfDisplay(dpy, screen_index);
    if(scrn==NULL) {
        fprintf(stderr, "couldn't get screen idx %d\n", screen_index);
    }
    DBG_PRINT("X11: X11Screen_GetScreen0 idx %d -> scrn %p DONE\n", screen_index, scrn);
    return (jlong) (intptr_t) scrn;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_ScreenDriver_getWidth0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) DisplayWidth( dpy, scrn_idx);
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_x11_ScreenDriver_getHeight0
  (JNIEnv *env, jclass clazz, jlong display, jint scrn_idx)
{
    Display * dpy = (Display *) (intptr_t) display;
    return (jint) DisplayHeight( dpy, scrn_idx);
}

int NewtScreen_XRotation2Degree(JNIEnv *env, Rotation xrotation) {
    int degree;
    if(xrotation == RR_Rotate_0) {
      degree = 0;
    }
    else if(xrotation == RR_Rotate_90) {
      degree = 90;
    }
    else if(xrotation == RR_Rotate_180) {
      degree = 180;
    }
    else if(xrotation == RR_Rotate_270) {
      degree = 270;
    } else {
      NewtCommon_throwNewRuntimeException(env, "invalid native rotation: %d", xrotation);
    }
    return degree;
}

Rotation NewtScreen_Degree2XRotation(JNIEnv *env, int degree) {
    Rotation xrot;
    if(degree == 0) {
      xrot = RR_Rotate_0;
    }
    else if(degree == 90) {
      xrot = RR_Rotate_90;
    }
    else if(degree == 180) {
      xrot = RR_Rotate_180;
    }
    else if(degree == 270) {
      xrot = RR_Rotate_270;
    } else {
      NewtCommon_throwNewRuntimeException(env, "invalid degree: %d", degree);
    }
    return xrot;
}

/*
 * Class:     jogamp_newt_driver_x11_ScreenDriver
 * Method:    GetRandRVersion0
 * Signature: (J)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_x11_ScreenDriver_getRandRVersion0
  (JNIEnv *env, jclass clazz, jlong display) 
{
    Display * dpy = (Display *)(intptr_t)display;
    jint version[2];
    if( 0 == XRRQueryVersion(dpy, &version[0], &version[1] ) ) {
        version[0] = 0;
        version[1] = 0;
    }
    jintArray jversion = (*env)->NewIntArray(env, 2);
    if (jversion == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size 2");
    }
    
    // move from the temp structure to the java structure
    (*env)->SetIntArrayRegion(env, jversion, 0, 2, version);
    
    return jversion;
}

