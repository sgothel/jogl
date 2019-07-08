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

#include "CAEAGLLayered.h"
#include "NewtCommon.h"

// #define VERBOSE_ON 1

#ifdef VERBOSE_ON
    #define DBG_PRINT(...) NSLog(@ __VA_ARGS__) ; fflush(stderr)
    // #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

// #define DBG_LIFECYCLE 1

NS_ASSUME_NONNULL_BEGIN

@interface NamedUITouch : NSObject
{
    @public
    short name;
    UITouch *touch;

    @protected
}

- (id)initWithName:(UITouch*)t name:(short)n;

- (void) dealloc;

/** Ensure NSPointerFunctionsObjectPointerPersonality for NSArray */
- (BOOL)isEqual:(id)object;

/** Ensure NSPointerFunctionsObjectPointerPersonality for NSArray */
- (NSUInteger)hash;

@end

@interface NewtUIView : CAEAGLUIView
{
    jobject javaWindowObject;

    volatile BOOL destroyNotifySent;
    volatile int softLockCount;
    pthread_mutex_t softLockSync;

    NSMapTable<UITouch*, NamedUITouch*>* activeTouchMap;
    NSMutableArray<NamedUITouch*>* activeTouches;
    short nextTouchName;
    BOOL modsDown[4]; // shift, ctrl, alt/option, win/command
}

- (id)initWithFrame:(CGRect)frameRect;

#ifdef DBG_LIFECYCLE
- (void) release;
#endif
- (void) dealloc;

/* Register or deregister (NULL) the java Window object, 
   ie, if NULL, no events are send */
- (void) setJavaWindowObject: (nullable jobject) javaWindowObj;
- (jobject) getJavaWindowObject;

- (void) setDestroyNotifySent: (BOOL) v;
- (BOOL) getDestroyNotifySent;

- (BOOL) softLock;
- (BOOL) softUnlock;

- (void) drawRect:(CGRect)dirtyRect;
- (BOOL) becomeFirstResponder;
- (BOOL) resignFirstResponder;

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event;
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event;
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event;
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event;
- (void)touchesEstimatedPropertiesUpdated:(NSSet<UITouch *> *)touches;
- (void)sendTouchEvent: (NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event 
                        eventState:(int)eventState newtEventType:(short)newtEventType;

- (CGPoint) screenPos2NewtClientWinPos: (CGPoint) p;

@end

@interface NewtUIWindow : UIWindow
{
@protected
    BOOL realized;
    BOOL useAutoMaxPixelScale;
    BOOL withinLiveResize;
    NewtUIView* contentNewtUIView;
@public
    BOOL isFullscreenWindow;
    int cachedInsets[4]; // l, r, t, b
}

+ (BOOL) initNatives: (_Nonnull JNIEnv* _Nonnull) env forClass: (jobject) clazz;

- (id) initWithFrame: (CGRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSUInteger) bufferingType
       defer: (BOOL) deferCreation
       isFullscreenWindow:(BOOL)isfs;
#ifdef DBG_LIFECYCLE
- (void) release;
#endif
- (void) dealloc;
- (void) setContentNewtUIView: (nullable NewtUIView*)v;
- (NewtUIView*) getContentNewtUIView;
- (void) setRealized: (BOOL)v;
- (BOOL) isRealized;

- (void) setAlwaysOn: (BOOL)top bottom:(BOOL)bottom;

- (void) setPixelScale: (CGFloat)reqPixelScale defer:(BOOL)defer;
- (void) updatePixelScale: (BOOL) defer;

- (void) updateInsets: (_Nullable JNIEnv* _Nullable) env jwin: (nullable jobject) javaWin;
- (void) updateSizePosInsets: (_Nullable JNIEnv* _Nullable) env jwin: (nullable jobject) javaWin defer: (jboolean)defer;
- (void) attachToParent: (UIWindow*) parent;
- (void) detachFromParent: (UIWindow*) parent;

- (CGPoint) newtRelClientTLWinPos2AbsTLScreenPos: (CGPoint) p;
- (CGSize) newtClientSize2TLSize: (CGSize) nsz;
- (CGPoint) getLocationOnScreen: (CGPoint) p;

- (void) focusChanged: (BOOL) gained;
- (void) visibilityChanged: (BOOL) visible;

- (BOOL) canBecomeFirstResponder;
- (BOOL) becomeFirstResponder;
- (BOOL) canResignFirstResponder;
- (BOOL) resignFirstResponder;

- (void) becomeKeyWindow;
- (void) resignKeyWindow;

- (void) becameVisible: (NSNotification*)notice;
- (void) becameHidden: (NSNotification*)notice;

- (void) sendResizeEvent;
- (void) windowDidMove: (NSNotification*) notification;
- (BOOL) windowClosingImpl: (BOOL) force;

@end

NS_ASSUME_NONNULL_END

