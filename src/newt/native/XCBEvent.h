
#ifndef _XCBEvent_h
#define _XCBEvent_h

#include "X11Common.h"

extern void XCBSetEventQueueOwner(Display *dpy);
extern void XCBEventPoll(JNIEnv *env, jobject obj, Display *dpy, jlong javaObjectAtom, jlong wmDeleteAtom);

#endif /* _XCBDisplayXCBEvent_h */
