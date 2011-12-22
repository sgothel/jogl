/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
 * Copyright (c) 2011 JogAmp Community. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 * - Redistribution of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 * 
 * - Redistribution in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 * 
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES,
 * INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN
 * MICROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL NOT BE LIABLE FOR
 * ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF USING, MODIFYING OR
 * DISTRIBUTING THIS SOFTWARE OR ITS DERIVATIVES. IN NO EVENT WILL SUN OR
 * ITS LICENSORS BE LIABLE FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR
 * DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE
 * DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY,
 * ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF
 * SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 * 
 */

#import <AppKit/AppKit.h>
#import <pthread.h>
#import "jni.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) NSLog(@ __VA_ARGS__)
    // #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

@interface NewtView : NSView
{
    jobject javaWindowObject;

    // This is set while messages are being dispatched and cleared afterward
    JavaVM *jvmHandle;
    int jvmVersion;

    volatile BOOL destroyNotifySent;
    volatile BOOL softLocked;
    pthread_mutex_t softLockSync;

    NSTrackingRectTag ptrTrackingTag;
    NSRect ptrRect;
    NSCursor * myCursor;
}

- (id)initWithFrame:(NSRect)frameRect;
- (void) dealloc;

/* Set during event dispatching cycle */
- (void) setJVMHandle: (JavaVM*) vm;
- (JavaVM*) getJVMHandle;
- (void) setJVMVersion: (int) ver;
- (int) getJVMVersion;

/* Register or deregister (NULL) the java Window object, 
   ie, if NULL, no events are send */
- (void) setJavaWindowObject: (jobject) javaWindowObj;
- (jobject) getJavaWindowObject;

- (void) rightMouseDown: (NSEvent*) theEvent;
- (void) resetCursorRects;
- (NSCursor *) cursor;

- (void) setDestroyNotifySent: (BOOL) v;
- (BOOL) getDestroyNotifySent;

- (BOOL) softLock;
- (void) softUnlock;

- (BOOL) needsDisplay;
- (void) displayIfNeeded;
- (void) display;
- (void) drawRect:(NSRect)dirtyRect;
- (void) viewDidHide;
- (void) viewDidUnhide;
- (BOOL) acceptsFirstResponder;

@end

#if defined(MAC_OS_X_VERSION_10_6) && MAC_OS_X_VERSION_MAX_ALLOWED >= MAC_OS_X_VERSION_10_6
@interface NewtMacWindow : NSWindow <NSWindowDelegate>
#else
@interface NewtMacWindow : NSWindow 
#endif
{
    BOOL mouseConfined;
    BOOL mouseVisible;
    BOOL mouseInside;
    BOOL cursorIsHidden;
    NSPoint lastInsideMousePosition;
@public
    int cachedInsets[4]; // l, r, t, b
}

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jobject) clazz;

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       defer: (BOOL) deferCreation
       screen:(NSScreen *)screen;

- (void) updateInsets: (JNIEnv*) env;
- (void) attachToParent: (NSWindow*) parent;
- (void) detachFromParent: (NSWindow*) parent;

- (NSPoint) newtScreenWinPos2OSXScreenPos: (NSPoint) p;
- (NSPoint) newtClientWinPos2OSXScreenPos: (NSPoint) p;
- (NSPoint) getLocationOnScreen: (NSPoint) p;
- (NSPoint) screenPos2NewtClientWinPos: (NSPoint) p;

- (void) cursorHide:(BOOL)v;
- (void) setMouseVisible:(BOOL)v;
- (void) setMouseConfined:(BOOL)v;
- (void) setMousePosition:(NSPoint)p;

- (BOOL) becomeFirstResponder;
- (BOOL) resignFirstResponder;
- (void) becomeKeyWindow;
- (void) resignKeyWindow;
- (void) windowDidBecomeKey: (NSNotification *) notification;
- (void) windowDidResignKey: (NSNotification *) notification;
- (void) focusChanged: (BOOL) gained;

@end
