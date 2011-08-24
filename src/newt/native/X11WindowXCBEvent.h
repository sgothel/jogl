
#ifndef _X11WindowXCBEvent_h
#define _X11WindowXCBEvent_h

#include "X11Window.h"

extern void X11WindowXCBSetEventQueueOwner(Display *dpy);
extern void X11WindowXCBEventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong wmDeleteAtom);

#endif /* _XCBWindowXCBEvent_h */
