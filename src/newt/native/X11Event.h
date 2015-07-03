
#ifndef _X11Event_h
#define _X11Event_h

#include "X11Common.h"

extern void X11EventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong wmDeleteAtom);

#endif /* _X11Event_h */
