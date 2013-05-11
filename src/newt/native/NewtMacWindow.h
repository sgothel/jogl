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

// #define DBG_LIFECYCLE 1

@interface NewtView : NSView
{
    jobject javaWindowObject;

    // This is set while messages are being dispatched and cleared afterward
    JavaVM *jvmHandle;
    int jvmVersion;

    volatile BOOL destroyNotifySent;
    volatile int softLockCount;
    pthread_mutex_t softLockSync;

    volatile NSTrackingRectTag ptrTrackingTag;
    NSRect ptrRect;
    NSCursor * myCursor;
}

- (id)initWithFrame:(NSRect)frameRect;

#ifdef DBG_LIFECYCLE
- (void) release;
#endif
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
- (BOOL) softUnlock;

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
    BOOL isFullscreenWindow;
    BOOL mouseConfined;
    BOOL mouseVisible;
    BOOL mouseInside;
    BOOL cursorIsHidden;
    BOOL realized;
    BOOL modsDown[4]; // shift, ctrl, alt/option, win/command
    NSPoint lastInsideMousePosition;
@public
    int cachedInsets[4]; // l, r, t, b
}

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jobject) clazz;

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       defer: (BOOL) deferCreation
       isFullscreenWindow:(BOOL)isfs;
#ifdef DBG_LIFECYCLE
- (void) release;
#endif
- (void) dealloc;
- (void) setRealized: (BOOL)v;
- (BOOL) isRealized;

- (void) updateInsets: (JNIEnv*) env jwin: (jobject) javaWin;
- (void) attachToParent: (NSWindow*) parent;
- (void) detachFromParent: (NSWindow*) parent;

- (NSPoint) newtAbsClientTLWinPos2AbsBLScreenPos: (NSPoint) p;
- (NSPoint) newtAbsClientTLWinPos2AbsBLScreenPos: (NSPoint) p size: (NSSize) nsz;
- (NSPoint) newtRelClientTLWinPos2AbsBLScreenPos: (NSPoint) p;
- (NSSize) newtClientSize2TLSize: (NSSize) nsz;
- (NSPoint) getLocationOnScreen: (NSPoint) p;
- (NSPoint) screenPos2NewtClientWinPos: (NSPoint) p;

- (BOOL) isMouseInside;
- (void) cursorHide:(BOOL)v;
- (void) setMouseVisible:(BOOL)v hasFocus:(BOOL)focus;
- (void) setMouseConfined:(BOOL)v;
- (void) setMousePosition:(NSPoint)p;

- (void) sendKeyEvent: (NSEvent*) event eventType: (jshort) evType;
- (void) sendKeyEvent: (jshort) keyCode characters: (NSString*) chars modifiers: (NSUInteger)mods eventType: (jshort) evType;
- (void) sendMouseEvent: (NSEvent*) event eventType: (jshort) evType;
- (void) focusChanged: (BOOL) gained;

- (BOOL) becomeFirstResponder;
- (BOOL) resignFirstResponder;
- (BOOL) canBecomeKeyWindow;
- (void) becomeKeyWindow;
- (void) resignKeyWindow;
- (void) windowDidBecomeKey: (NSNotification *) notification;
- (void) windowDidResignKey: (NSNotification *) notification;
- (void) keyDown: (NSEvent*) theEvent;
- (void) keyUp: (NSEvent*) theEvent;
- (void) handleFlagsChanged:(int) keyMask keyIndex: (int) keyIdx keyCode: (int) keyCode modifiers: (NSUInteger) mods;
- (void) flagsChanged: (NSEvent *) theEvent;
- (void) mouseEntered: (NSEvent*) theEvent;
- (void) mouseExited: (NSEvent*) theEvent;
- (void) mouseMoved: (NSEvent*) theEvent;
- (void) scrollWheel: (NSEvent*) theEvent;
- (void) mouseDown: (NSEvent*) theEvent;
- (void) mouseDragged: (NSEvent*) theEvent;
- (void) mouseUp: (NSEvent*) theEvent;
- (void) rightMouseDown: (NSEvent*) theEvent;
- (void) rightMouseDragged: (NSEvent*) theEvent;
- (void) rightMouseUp: (NSEvent*) theEvent;
- (void) otherMouseDown: (NSEvent*) theEvent;
- (void) otherMouseUp: (NSEvent*) theEvent;
- (void) windowDidResize: (NSNotification*) notification;
- (void) windowDidMove: (NSNotification*) notification;
- (BOOL) windowClosingImpl: (BOOL) force;
- (BOOL) windowShouldClose: (id) sender;
- (void) windowWillClose: (NSNotification*) notification;

@end
