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

#ifndef _X11COMMON_H_
#define _X11COMMON_H_

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>

#include <gluegen_stdint.h>

#include <unistd.h>
#include <errno.h>
#include <X11/Xlib.h>
#include <X11/Xutil.h>
#include <X11/keysym.h>
#include <X11/Xatom.h>

#include <X11/extensions/Xrandr.h>

#include "jogamp_newt_driver_x11_X11Screen.h"
#include "jogamp_newt_driver_x11_X11Display.h"
#include "jogamp_newt_driver_x11_X11Window.h"

#include "Window.h"
#include "MouseEvent.h"
#include "InputEvent.h"
#include "KeyEvent.h"
#include "WindowEvent.h"
#include "ScreenMode.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 
#else
    #define DBG_PRINT(...)
#endif

extern jclass X11NewtWindowClazz;
extern jmethodID insetsChangedID;
extern jmethodID visibleChangedID;

jobject getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning);

void NewtDisplay_displayDispatchErrorHandlerEnable(int onoff, JNIEnv * env);
Status NewtWindows_getRootAndParent (Display *dpy, Window w, Window * root_return, Window * parent_return);
Status NewtWindows_updateInsets(JNIEnv *env, jobject jwindow, Display *dpy, Window window, int *left, int *right, int *top, int *bottom);

#endif /* _X11COMMON_H_ */

