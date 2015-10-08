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

#include "jogamp_newt_driver_x11_DisplayDriver.h"
#include "jogamp_newt_driver_x11_ScreenDriver.h"
#include "jogamp_newt_driver_x11_RandR11.h"
#include "jogamp_newt_driver_x11_RandR13.h"
#include "jogamp_newt_driver_x11_WindowDriver.h"

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
extern jmethodID insetsVisibleChangedID;

typedef struct {
    Window window;
    jobject jwindow;
    Atom * allAtoms;
    Atom javaObjectAtom;
    Atom windowDeleteAtom;
    uint32_t supportedAtoms;
    uint32_t lastDesktop;
    Bool maxHorz;
    Bool maxVert;
    /** flag whether window is mapped */
    Bool isMapped;
} JavaWindow;

JavaWindow * getJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning);

Status NewtWindows_getRootAndParent (Display *dpy, Window w, Window * root_return, Window * parent_return);
Bool NewtWindows_updateInsets(Display *dpy, JavaWindow * w, int *left, int *right, int *top, int *bottom);
Bool NewtWindows_updateMaximized(Display *dpy, JavaWindow * w, uint32_t netWMState);

#define _MASK_NET_WM_STATE                   ( 1 <<  0 )
#define _MASK_NET_WM_STATE_MODAL             ( 1 <<  1 )
#define _MASK_NET_WM_STATE_STICKY            ( 1 <<  2 )
#define _MASK_NET_WM_STATE_MAXIMIZED_VERT    ( 1 <<  3 )
#define _MASK_NET_WM_STATE_MAXIMIZED_HORZ    ( 1 <<  4 )
#define _MASK_NET_WM_STATE_SHADED            ( 1 <<  5 )
#define _MASK_NET_WM_STATE_HIDDEN            ( 1 <<  8 )
#define _MASK_NET_WM_STATE_FULLSCREEN        ( 1 <<  9 )
#define _MASK_NET_WM_STATE_ABOVE             ( 1 << 10 )
#define _MASK_NET_WM_STATE_BELOW             ( 1 << 11 )
#define _MASK_NET_WM_STATE_DEMANDS_ATTENTION ( 1 << 12 )
#define _MASK_NET_WM_STATE_FOCUSED           ( 1 << 13 )
#define _MASK_NET_WM_BYPASS_COMPOSITOR       ( 1 << 14 )
#define _MASK_NET_WM_DESKTOP                 ( 1 << 15 )
#define _MASK_NET_CURRENT_DESKTOP            ( 1 << 16 )
#define _MASK_NET_WM_WINDOW_TYPE             ( 1 << 17 )
#define _MASK_NET_WM_WINDOW_TYPE_NORMAL      ( 1 << 18 )
#define _MASK_NET_WM_WINDOW_TYPE_POPUP_MENU  ( 1 << 19 )
#define _MASK_NET_FRAME_EXTENTS              ( 1 << 20 )
#define _MASK_NET_SUPPORTED                  ( 1 << 21 )
#define _MASK_NET_ACTIVE_WINDOW              ( 1 << 22 )
#define _MASK_WM_CHANGE_STATE                ( 1 << 23 )
#define _MASK_MOTIF_WM_HINTS                 ( 1 << 24 )

#define _NET_WM_STATE_IDX 0
#define _NET_WM_STATE_MODAL_IDX 1
#define _NET_WM_STATE_STICKY_IDX 2
#define _NET_WM_STATE_MAXIMIZED_VERT_IDX 3
#define _NET_WM_STATE_MAXIMIZED_HORZ_IDX 4
#define _NET_WM_STATE_SHADED_IDX 5
#define _NET_WM_STATE_SKIP_TASKBAR_IDX 6
#define _NET_WM_STATE_SKIP_PAGER_IDX 7
#define _NET_WM_STATE_HIDDEN_IDX 8
#define _NET_WM_STATE_FULLSCREEN_IDX 9
#define _NET_WM_STATE_ABOVE_IDX 10
#define _NET_WM_STATE_BELOW_IDX 11
#define _NET_WM_STATE_DEMANDS_ATTENTION_IDX 12
#define _NET_WM_STATE_FOCUSED_IDX 13
#define _NET_WM_BYPASS_COMPOSITOR_IDX 14
#define _NET_WM_DESKTOP_IDX 15
#define _NET_CURRENT_DESKTOP_IDX 16
#define _NET_WM_WINDOW_TYPE_IDX 17
#define _NET_WM_WINDOW_TYPE_NORMAL_IDX 18
#define _NET_WM_WINDOW_TYPE_POPUP_MENU_IDX 19
#define _NET_FRAME_EXTENTS_IDX 20
#define _NET_SUPPORTED_IDX 21
#define _NET_ACTIVE_WINDOW_IDX 22
#define _WM_CHANGE_STATE_IDX 23
#define _MOTIF_WM_HINTS_IDX 24

void NewtWindows_setUrgency(Display *dpy, Window window, Bool enable);
void NewtWindows_sendNET_WM_STATE(Display *dpy, Window root, JavaWindow *w, int prop1Idx, int prop2Idx, Bool enable);
uint32_t NewtWindows_getNET_WM_STATE(Display *dpy, JavaWindow *w);

#endif /* _X11COMMON_H_ */

