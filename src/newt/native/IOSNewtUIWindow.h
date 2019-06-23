/**
 * Copyright 2019 JogAmp Community. All rights reserved.
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

#import <UIKit/UIKit.h>
#import <QuartzCore/QuartzCore.h>
#import <pthread.h>
#import "jni.h"

#include "NewtCommon.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) NSLog(@ __VA_ARGS__) ; fflush(stderr)
    // #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

// #define DBG_LIFECYCLE 1

@interface NewtUIView : UIView
{
    jobject javaWindowObject;

    volatile BOOL destroyNotifySent;
    volatile int softLockCount;
    pthread_mutex_t softLockSync;

    BOOL modsDown[4]; // shift, ctrl, alt/option, win/command
}

- (id)initWithFrame:(CGRect)frameRect;

#ifdef DBG_LIFECYCLE
- (void) release;
#endif
- (void) dealloc;

/* Register or deregister (NULL) the java Window object, 
   ie, if NULL, no events are send */
- (void) setJavaWindowObject: (jobject) javaWindowObj;
- (jobject) getJavaWindowObject;

- (void) setDestroyNotifySent: (BOOL) v;
- (BOOL) getDestroyNotifySent;

- (BOOL) softLock;
- (BOOL) softUnlock;

- (void) drawRect:(CGRect)dirtyRect;
- (BOOL) acceptsFirstResponder;
- (BOOL) becomeFirstResponder;
- (BOOL) resignFirstResponder;

- (void) sendMouseEvent: (UIEvent*) event eventType: (jshort) evType;
- (CGPoint) screenPos2NewtClientWinPos: (CGPoint) p;

- (void) handleFlagsChanged:(NSUInteger) mods;
- (void) handleFlagsChanged:(int) keyMask keyIndex: (int) keyIdx keyCode: (int) keyCode modifiers: (NSUInteger) mods;
- (void) sendKeyEvent: (UIEvent*) event eventType: (jshort) evType;
- (void) sendKeyEvent: (jshort) keyCode characters: (NSString*) chars modifiers: (NSUInteger)mods eventType: (jshort) evType;

@end

@interface NewtUIWindow : UIWindow
{
    BOOL realized;
    jboolean withinLiveResize;
@public
    BOOL hasPresentationSwitch;
    NSUInteger defaultPresentationOptions;
    NSUInteger fullscreenPresentationOptions;
    BOOL isFullscreenWindow;
    int cachedInsets[4]; // l, r, t, b
}

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jobject) clazz;

- (id) initWithFrame: (CGRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSUInteger) bufferingType
       defer: (BOOL) deferCreation
       isFullscreenWindow:(BOOL)isfs;
#ifdef DBG_LIFECYCLE
- (void) release;
#endif
- (void) dealloc;
- (void) setRealized: (BOOL)v;
- (BOOL) isRealized;

- (void) setAlwaysOn: (BOOL)top bottom:(BOOL)bottom;

- (void) updateInsets: (JNIEnv*) env jwin: (jobject) javaWin;
- (void) updateSizePosInsets: (JNIEnv*) env jwin: (jobject) javaWin defer: (jboolean)defer;
- (void) attachToParent: (UIWindow*) parent;
- (void) detachFromParent: (UIWindow*) parent;

- (CGPoint) newtRelClientTLWinPos2AbsTLScreenPos: (CGPoint) p;
- (CGSize) newtClientSize2TLSize: (CGSize) nsz;
- (CGPoint) getLocationOnScreen: (CGPoint) p;

- (void) focusChanged: (BOOL) gained;

- (void) flagsChanged: (UIEvent *) theEvent;
- (BOOL) acceptsMouseMovedEvents;
- (BOOL) acceptsFirstResponder;
- (BOOL) becomeFirstResponder;
- (BOOL) resignFirstResponder;
- (BOOL) canBecomeKeyWindow;
- (void) becomeKeyWindow;
- (void) resignKeyWindow;
- (void) windowDidBecomeKey: (NSNotification *) notification;
- (void) windowDidResignKey: (NSNotification *) notification;

- (void) windowWillStartLiveResize: (NSNotification *) notification;
- (void) windowDidEndLiveResize: (NSNotification *) notification;
- (CGSize) windowWillResize: (UIWindow *)sender toSize:(CGSize)frameSize;
- (void) windowDidResize: (NSNotification*) notification;
- (void) sendResizeEvent;

- (void) windowDidMove: (NSNotification*) notification;
- (BOOL) windowClosingImpl: (BOOL) force;
- (BOOL) windowShouldClose: (id) sender;
- (void) windowWillClose: (NSNotification*) notification;

@end
