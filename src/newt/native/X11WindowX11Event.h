
#ifndef _X11WindowX11Event_h
#define _X11WindowX11Event_h

#include "X11Window.h"

extern void X11WindowX11EventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong wmDeleteAtom);

#endif /* _X11WindowX11Event_h */
