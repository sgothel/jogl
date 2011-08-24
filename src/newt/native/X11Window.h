
#ifndef _X11Window_h
#define _X11Window_h

#include <jni.h>
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

#ifdef VERBOSE_ON

    #define VERBOSE_BOOL True 
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr) 

#else

    #define VERBOSE_BOOL False 
    #define DBG_PRINT(...)

#endif

extern jmethodID sizeChangedID;
extern jmethodID positionChangedID;
extern jmethodID focusChangedID;
extern jmethodID visibleChangedID;
extern jmethodID windowDestroyNotifyID;
extern jmethodID windowRepaintID;
extern jmethodID windowReparentedID;
extern jmethodID enqueueMouseEventID;
extern jmethodID sendMouseEventID;
extern jmethodID enqueueKeyEventID;
extern jmethodID sendKeyEventID;
extern jmethodID focusActionID;
extern jmethodID enqueueRequestFocusID;
extern jmethodID displayCompletedID;

extern jint X11InputState2NewtModifiers(unsigned int xstate);
extern jint X11KeySym2NewtVKey(KeySym keySym);

extern void X11WindowDisplayErrorHandlerEnable(int onoff, JNIEnv * env);

extern jobject X11WindowGetJavaWindowProperty(JNIEnv *env, Display *dpy, Window window, jlong javaObjectAtom, Bool showWarning);

#endif /* _X11Window_h */
