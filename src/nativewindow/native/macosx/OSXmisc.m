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
 
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>
#include <AppKit/AppKit.h>
#import <QuartzCore/QuartzCore.h>
#import "NativeWindowProtocols.h"

#include "NativewindowCommon.h"
#include "jogamp_nativewindow_macosx_OSXUtil.h"
#include "jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow.h"

#include <jawt_md.h>

// #define VERBOSE 1
//
#ifdef VERBOSE
    // #define DBG_PRINT(...) NSLog(@ ## __VA_ARGS__)
    #define DBG_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT(...)
#endif

// #define VERBOSE2 1
//
#ifdef VERBOSE2
    #define DBG_PRINT2(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define DBG_PRINT2(...)
#endif

// #define DBG_LIFECYCLE 1

static const char * const ClazzNameRunnable = "java/lang/Runnable";
static jmethodID runnableRunID = NULL;

static const char * const ClazzAnyCstrName = "<init>";

static const char * const ClazzNamePoint = "com/jogamp/nativewindow/util/Point";
static const char * const ClazzNamePointCstrSignature = "(II)V";
static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static const char * const ClazzNameInsets = "com/jogamp/nativewindow/util/Insets";
static const char * const ClazzNameInsetsCstrSignature = "(IIII)V";
static jclass insetsClz = NULL;
static jmethodID insetsCstr = NULL;

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_macosx_OSXUtil_initIDs0(JNIEnv *env, jclass _unused) {
    if( NativewindowCommon_init(env) ) {
        jclass c;
        c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNameInsets);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNameInsets);
        }
        insetsClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==insetsClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't use %s", ClazzNameInsets);
        }
        insetsCstr = (*env)->GetMethodID(env, insetsClz, ClazzAnyCstrName, ClazzNameInsetsCstrSignature);
        if(NULL==insetsCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.%s %s",
                ClazzNameInsets, ClazzAnyCstrName, ClazzNameInsetsCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNameRunnable);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNameRunnable);
        }
        runnableRunID = (*env)->GetMethodID(env, c, "run", "()V");
        if(NULL==runnableRunID) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.run()V", ClazzNameRunnable);
        }
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_macosx_OSXUtil_isNSView0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[NSView class]];
    DBG_PRINT( "isNSView(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_macosx_OSXUtil_isNSWindow0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[NSWindow class]];
    DBG_PRINT( "isNSWindow(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

static CGDirectDisplayID OSXUtil_getCGDirectDisplayIDByNSScreen(NSScreen *screen) {
    if( NULL == screen ) {
        return (CGDirectDisplayID)0;
    }
    // Mind: typedef uint32_t CGDirectDisplayID;
    NSDictionary * dict = [screen deviceDescription];
    NSNumber * val = (NSNumber *) [dict objectForKey: @"NSScreenNumber"];
    // [NSNumber integerValue] returns NSInteger which is 32 or 64 bit native size
    return (CGDirectDisplayID) [val integerValue];
}
static NSScreen * OSXUtil_getNSScreenByCGDirectDisplayID(CGDirectDisplayID displayID) {
    NSArray *screens = [NSScreen screens];
    int i;
    for(i=[screens count]-1; i>=0; i--) {
        NSScreen * screen = (NSScreen *) [screens objectAtIndex: i];
        CGDirectDisplayID dID = OSXUtil_getCGDirectDisplayIDByNSScreen(screen);
        if( dID == displayID ) {
            return screen;
        }
    }
    return NULL;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    getLocationOnScreen0
 * Signature: (JII)Lcom/jogamp/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong winOrView, jint src_x, jint src_y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    /**
     * return location in 0/0 top-left space,
     * OSX is 0/0 bottom-left space naturally
     */
    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    NSWindow* win = NULL;
    NSView* view = NULL;

    if( [nsObj isKindOfClass:[NSWindow class]] ) {
        win = (NSWindow*) nsObj;
        view = [win contentView];
    } else if( nsObj != NULL && [nsObj isKindOfClass:[NSView class]] ) {
        view = (NSView*) nsObj;
        win = [view window];
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }
    NSRect viewFrame = [view frame];

    NSRect r;
    r.origin.x = src_x;
    r.origin.y = viewFrame.size.height - src_y; // y-flip for 0/0 top-left
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectToScreen: r]; // 10.7
    NSPoint oS = [win convertBaseToScreen: r.origin]; // BL-screen

    NSScreen* screen = [win screen];
    CGDirectDisplayID display = OSXUtil_getCGDirectDisplayIDByNSScreen(screen);
    CGRect frameTL = CGDisplayBounds (display); // origin top-left
    NSRect frameBL = [screen frame]; // origin bottom-left
    oS.y = frameTL.origin.y + frameTL.size.height - ( oS.y - frameBL.origin.y ); // y-flip from BL-screen -> TL-screen

#ifdef VERBOSE
    NSRect winFrame = [win frame];
    DBG_PRINT( "GetLocationOnScreen0(window: %p):: point-in[%d/%d], winFrame[%d/%d %dx%d], viewFrame[%d/%d %dx%d], screen tl[%d/%d %dx%d] bl[%d/%d %dx%d] -> %d/%d\n",
        win, (int)src_x, (int)src_y,
        (int)winFrame.origin.x, (int)winFrame.origin.y, (int)winFrame.size.width, (int)winFrame.size.height,
        (int)viewFrame.origin.x, (int)viewFrame.origin.y, (int)viewFrame.size.width, (int)viewFrame.size.height,
        (int)frameTL.origin.x, (int)frameTL.origin.y, (int)frameTL.size.width, (int)frameTL.size.height,
        (int)frameBL.origin.x, (int)frameBL.origin.y, (int)frameBL.size.width, (int)frameBL.size.height,
        (int)oS.x, (int)oS.y);
#endif

    jobject res = (*env)->NewObject(env, pointClz, pointCstr, (jint)oS.x, (jint)oS.y);

    [pool release];

    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    getInsets0
 * Signature: (J)Lcom/jogamp/nativewindow/util/Insets;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetInsets0
  (JNIEnv *env, jclass unused, jlong winOrView)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    NSWindow* win = NULL;
    NSView* view = NULL;
    jint il,ir,it,ib;

    if( [nsObj isKindOfClass:[NSWindow class]] ) {
        win = (NSWindow*) nsObj;
        view = [win contentView];
    } else if( nsObj != NULL && [nsObj isKindOfClass:[NSView class]] ) {
        view = (NSView*) nsObj;
        win = [view window];
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }

    NSRect frameRect = [win frame];
    NSRect contentRect = [win contentRectForFrameRect: frameRect];

    // note: this is a simplistic implementation which doesn't take
    // into account DPI and scaling factor
    CGFloat l = contentRect.origin.x - frameRect.origin.x;
    il = (jint)l;                                                     // l
    ir = (jint)(frameRect.size.width - (contentRect.size.width + l)); // r
    it = (jint)(frameRect.size.height - contentRect.size.height);     // t
    ib = (jint)(contentRect.origin.y - frameRect.origin.y);           // b

    jobject res = (*env)->NewObject(env, insetsClz, insetsCstr, il, ir, it, ib);

    [pool release];

    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    GetPixelScale1
 * Signature: (I)D
 */
JNIEXPORT jdouble JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetPixelScale1
  (JNIEnv *env, jclass unused, jint displayID)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    CGFloat pixelScale = 1.0; // default
    NSScreen *screen =  OSXUtil_getNSScreenByCGDirectDisplayID((CGDirectDisplayID)displayID);
    if( NULL != screen ) {
NS_DURING
        // Available >= 10.7
        pixelScale = [screen backingScaleFactor]; // HiDPI scaling
NS_HANDLER
NS_ENDHANDLER
    }
    [pool release];

    return (jdouble)pixelScale;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    GetPixelScale1
 * Signature: (J)D
 */
JNIEXPORT jdouble JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetPixelScale2
  (JNIEnv *env, jclass unused, jlong winOrView)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    NSWindow* win = NULL;
    NSView* view = NULL;
    NSScreen *screen = NULL;

    if( [nsObj isKindOfClass:[NSWindow class]] ) {
        win = (NSWindow*) nsObj;
        view = [win contentView];
        screen = [win screen];
    } else if( nsObj != NULL && [nsObj isKindOfClass:[NSView class]] ) {
        view = (NSView*) nsObj;
        win = [view window];
        screen = [win screen];
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }

    CGFloat pixelScale = 1.0; // default
NS_DURING
    // Available >= 10.7
    pixelScale = [screen backingScaleFactor]; // HiDPI scaling
NS_HANDLER
NS_ENDHANDLER

    [pool release];

    return (jdouble)pixelScale;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    CreateNSWindow0
 * Signature: (IIIIZ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_CreateNSWindow0
  (JNIEnv *env, jclass unused, jint x, jint y, jint width, jint height)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSRect rect = NSMakeRect(x, y, width, height);

    // Allocate the window
    NSWindow* myWindow = [[NSWindow alloc] initWithContentRect: rect
                                           styleMask: NSBorderlessWindowMask
                                           backing: NSBackingStoreBuffered
                                           defer: NO]; // Bug 1087: Set default framebuffer, hence enforce NSView realization
    [myWindow setReleasedWhenClosed: YES]; // default
    [myWindow setPreservesContentDuringLiveResize: YES];
    // Remove animations
NS_DURING
    if ( [myWindow respondsToSelector:@selector(setAnimationBehavior:)] ) {
        // Available >= 10.7 - Removes default animations
        [myWindow setAnimationBehavior: NSWindowAnimationBehaviorNone];
    }
NS_HANDLER
NS_ENDHANDLER

    // invisible ..
    [myWindow setOpaque: NO];
    [myWindow setBackgroundColor: [NSColor clearColor]];

    // Bug 1087: Set default framebuffer, hence enforce NSView realization
    // However, using the NSWindow ctor w/ 'defer: NO' seems sufficient
    // and we are invisible - no focus!
    // NSView* myView = [myWindow contentView];
    // [myView lockFocus];
    // [myView unlockFocus];

    [pool release];

    return (jlong) ((intptr_t) myWindow);
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    DestroyNSWindow0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_DestroyNSWindow0
  (JNIEnv *env, jclass unused, jlong nsWindow)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) nsWindow);

    [mWin close]; // performs release!
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    GetNSView0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetNSView0
  (JNIEnv *env, jclass unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    jlong res = (jlong) ((intptr_t) [win contentView]);

    DBG_PRINT( "GetNSView(window: %p): %p\n", win, (void*) (intptr_t) res);

    [pool release];
    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    GetNSWindow0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetNSWindow0
  (JNIEnv *env, jclass unused, jlong view)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSView* v = (NSView*) ((intptr_t) view);

    jlong res = (jlong) ((intptr_t) [v window]);

    DBG_PRINT( "GetNSWindow(view: %p): %p\n", v, (void*) (intptr_t) res);

    [pool release];
    return res;
}

/**
 * Track lifecycle via DBG_PRINT messages, if VERBOSE is enabled!
 */
@interface MyCALayer: CALayer
{
@private
  BOOL fixedFrameSet;
  CGRect fixedFrame;
  float visibleOpacity;
  BOOL visibleOpacityZeroed;
}
- (id)init;
#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
- (void)dealloc;
#endif
- (id<CAAction>)actionForKey:(NSString *)key ;
- (void)layoutSublayers;
- (void)setFrame:(CGRect) frame;
- (void)fixCALayerLayout: (CALayer*) subLayer visible:(BOOL)visible x:(jint)x y:(jint)y width:(jint)width height:(jint)height caLayerQuirks:(jint)caLayerQuirks force:(jboolean) force;

@end

@implementation MyCALayer

- (id)init
{
    DBG_PRINT("MyCALayer::init.0\n");
    MyCALayer * o = [super init];
    o->fixedFrameSet = 0;
    o->fixedFrame = CGRectMake(0, 0, 0, 0);
    o->visibleOpacity = 1.0;
    o->visibleOpacityZeroed = 0;
    DBG_PRINT("MyCALayer::init.X: new %p\n", o);
    return o;
}

#ifdef DBG_LIFECYCLE

- (id)retain
{
    DBG_PRINT("MyCALayer::retain.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyCALayer::retain: %@",[NSThread callStackSymbols]);
    id o = [super retain];
    DBG_PRINT("MyCALayer::retain.X: %p (refcnt %d)\n", o, (int)[o retainCount]);
    return o;
}

- (oneway void)release
{
    DBG_PRINT("MyCALayer::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyCALayer::release: %@",[NSThread callStackSymbols]);
    [super release];
    // DBG_PRINT("MyCALayer::release.X: %p (refcnt %d)\n", self, (int)[self retainCount]);
}

- (void)dealloc
{
    DBG_PRINT("MyCALayer::dealloc.0 %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyCALayer::dealloc: %@",[NSThread callStackSymbols]);
    [super dealloc];
    // DBG_PRINT("MyCALayer.dealloc.X: %p\n", self);
}

#endif

- (id<CAAction>)actionForKey:(NSString *)key 
{
    DBG_PRINT("MyCALayer::actionForKey.0 %p key %s -> NIL\n", self, [key UTF8String]);
    return nil;
    // return [super actionForKey: key];
}

- (void)layoutSublayers
{
    if( fixedFrameSet ) {
        NSArray* subs = [self sublayers];
        if( NULL != subs ) {
            CGRect rFrame = [self frame];
            if( !CGRectEqualToRect(fixedFrame, rFrame) ) {
                #ifdef VERBOSE
                DBG_PRINT("CALayer::layoutSublayers.0: Root %p frame %lf/%lf %lfx%lf -> %lf/%lf %lfx%lf\n",
                    self,
                    rFrame.origin.x, rFrame.origin.y, rFrame.size.width, rFrame.size.height,
                    fixedFrame.origin.x, fixedFrame.origin.y, fixedFrame.size.width, fixedFrame.size.height);
                #endif
                [super setFrame: fixedFrame];
            }
            NSUInteger i = 0;
            for(i=0; i<[subs count]; i++) {
                CALayer* sub = [subs objectAtIndex: i];
                CGRect sFrame = [sub frame];
                CGRect sFrame2 = CGRectMake(0, 0, fixedFrame.size.width, fixedFrame.size.height);
                if( !CGRectEqualToRect(sFrame2, sFrame) ) {
                    #ifdef VERBOSE
                    DBG_PRINT("CALayer::layoutSublayers.1: Sub[%d] %p frame %lf/%lf %lfx%lf -> %lf/%lf %lfx%lf\n",
                        (int)i, sub,
                        sFrame.origin.x, sFrame.origin.y, sFrame.size.width, sFrame.size.height,
                        sFrame2.origin.x, sFrame2.origin.y, sFrame2.size.width, sFrame2.size.height);
                    #endif
                    [sub setFrame: sFrame2];
                }
                #ifdef VERBOSE
                DBG_PRINT("CALayer::layoutSublayers.X: Root %p . Sub[%d] %p : frame r: %lf/%lf %lfx%lf . s: %lf/%lf %lfx%lf\n",
                    self, (int)i, sub,
                    rFrame.origin.x, rFrame.origin.y, rFrame.size.width, rFrame.size.height,
                    sFrame.origin.x, sFrame.origin.y, sFrame.size.width, sFrame.size.height);
                #endif
            }
        }
    } else {
        [super layoutSublayers];
    }
}

- (void) setFrame:(CGRect) frame
{
    if( fixedFrameSet ) {
        [super setFrame: fixedFrame];
    } else {
        [super setFrame: frame];
    }
}

- (void)fixCALayerLayout: (CALayer*) subLayer visible:(BOOL)visible x:(jint)x y:(jint)y width:(jint)width height:(jint)height caLayerQuirks:(jint)caLayerQuirks force:(jboolean) force
{
    int loutQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_LAYOUT & caLayerQuirks );
    {
        CALayer* superLayer = [self superlayer];
        CGRect superFrame = [superLayer frame];
        CGRect lFrame = [self frame];
        if( visible ) {
            // Opacity must be 0 to see through the disabled CALayer
            [subLayer setOpacity: visibleOpacity];
            [self setOpacity: visibleOpacity];
            [self setHidden: NO];
            [subLayer setHidden: NO];
            visibleOpacityZeroed = 0;
        } else {
            [subLayer setHidden: YES];
            [self setHidden: YES];
            if( !visibleOpacityZeroed ) {
                visibleOpacity = [self opacity];
            }
            [subLayer setOpacity: 0.0];
            [self setOpacity: 0.0];
            visibleOpacityZeroed = 1;
        }
        int posQuirk  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & caLayerQuirks ) && ( lFrame.origin.x!=0 || lFrame.origin.y!=0 );
        int sizeQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & caLayerQuirks ) && ( lFrame.size.width!=width || lFrame.size.height!=height );
        if( !posQuirk || loutQuirk ) {
            // Use root layer position, sub-layer will be on 0/0,
            // Given AWT position is location on screen w/o insets and top/left origin!
            fixedFrame.origin.x = x;
            fixedFrame.origin.y = superFrame.size.height - height - y; // AWT's position top/left -> bottom/left
            posQuirk |= 8;
        } else {
            // Buggy super layer position, always use 0/0
            fixedFrame.origin.x = 0;
            fixedFrame.origin.y = 0;
        }
        if( !sizeQuirk ) {
            fixedFrame.size.width = lFrame.size.width;
            fixedFrame.size.height = lFrame.size.height;
        } else {
            fixedFrame.size.width = width;
            fixedFrame.size.height = height;
        }
        DBG_PRINT("CALayer::FixCALayerLayout0.0: Visible %d, Quirks [%d, pos %d, size %d, lout %d, force %d], Super %p frame %lf/%lf %lfx%lf, Root %p frame %lf/%lf %lfx%lf, usr %d/%d %dx%d -> %lf/%lf %lfx%lf\n",
            (int)visible, caLayerQuirks, posQuirk, sizeQuirk, loutQuirk, (int)force,
            superLayer, superFrame.origin.x, superFrame.origin.y, superFrame.size.width, superFrame.size.height, 
            self, lFrame.origin.x, lFrame.origin.y, lFrame.size.width, lFrame.size.height, 
            x, y, width, height, fixedFrame.origin.x, fixedFrame.origin.y, fixedFrame.size.width, fixedFrame.size.height);
        if( posQuirk || sizeQuirk || loutQuirk ) {
            fixedFrameSet = 1;
            [super setFrame: fixedFrame];
        }
    }
    if( NULL != subLayer ) {
        CGRect lFrame = [subLayer frame];
        int sizeQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & caLayerQuirks ) && ( lFrame.size.width!=width || lFrame.size.height!=height );
        CGFloat _x, _y, _w, _h;
        int posQuirk  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & caLayerQuirks ) && ( lFrame.origin.x!=0 || lFrame.origin.y!=0 );
        // Sub rel. to used root layer
        _x = 0;
        _y = 0;
        posQuirk |= 8;
        if( !sizeQuirk ) {
            _w = lFrame.size.width;
            _h = lFrame.size.height;
        } else {
            _w = width;
            _h = height;
        }
        DBG_PRINT("CALayer::FixCALayerLayout1.0: Visible %d, Quirks [%d, pos %d, size %d, lout %d, force %d], SubL %p frame %lf/%lf %lfx%lf, usr %dx%d -> %lf/%lf %lfx%lf\n",
            (int)visible, caLayerQuirks, posQuirk, sizeQuirk, loutQuirk, (int)force,
            subLayer, lFrame.origin.x, lFrame.origin.y, lFrame.size.width, lFrame.size.height,
            width, height, _x, _y, _w, _h);
        if( force || posQuirk || sizeQuirk || loutQuirk ) {
            lFrame.origin.x = _x;
            lFrame.origin.y = _y;
            lFrame.size.width = _w;
            lFrame.size.height = _h;
            if( [subLayer conformsToProtocol:@protocol(NWDedicatedFrame)] ) {
                CALayer <NWDedicatedFrame> * subLayerDS = (CALayer <NWDedicatedFrame> *) subLayer;
                [subLayerDS setDedicatedFrame: lFrame quirks: caLayerQuirks];
            } else {
                [subLayer setFrame: lFrame];
            }
        }
    }
}

@end

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    CreateCALayer0
 * Signature: (IIF)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_CreateCALayer0
  (JNIEnv *env, jclass unused, jint width, jint height, jfloat contentsScale)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    MyCALayer* layer = [[MyCALayer alloc] init];
    DBG_PRINT("CALayer::CreateCALayer.0: root %p 0/0 %dx%d @ scale %lf (refcnt %d)\n", layer, (int)width, (int)height, (double)contentsScale, (int)[layer retainCount]);
    // avoid zero size
    if(0 == width) { width = 32; }
    if(0 == height) { height = 32; }

NS_DURING
    // Available >= 10.7
    [layer setContentsScale: (CGFloat)contentsScale];
NS_HANDLER
NS_ENDHANDLER

    // initial dummy size !
    CGRect lFrame = [layer frame];
    lFrame.origin.x = 0;
    lFrame.origin.y = 0;
    lFrame.size.width = width;
    lFrame.size.height = height;
    [layer setFrame: lFrame];
    // no animations for add/remove/swap sublayers etc 
    // doesn't work: [layer removeAnimationForKey: kCAOnOrderIn, kCAOnOrderOut, kCATransition]
    [layer removeAllAnimations];
    // [layer addAnimation:nil forKey:kCATransition];
    [layer setAutoresizingMask: (kCALayerWidthSizable|kCALayerHeightSizable)];
    [layer setNeedsDisplayOnBoundsChange: YES];
    DBG_PRINT("CALayer::CreateCALayer.1: root %p %lf/%lf %lfx%lf\n", layer, lFrame.origin.x, lFrame.origin.y, lFrame.size.width, lFrame.size.height);
    [pool release];
    DBG_PRINT("CALayer::CreateCALayer.X: root %p (refcnt %d)\n", layer, (int)[layer retainCount]);

    return (jlong) ((intptr_t) layer);
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    AddCASublayer0
 * Signature: (JJIIIIIF)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_AddCASublayer0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jint x, jint y, jint width, jint height, jfloat contentsScale, jint caLayerQuirks)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
    CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    [rootLayer retain]; // Pairs w/ RemoveCASublayer
    [subLayer retain]; // Pairs w/ RemoveCASublayer

    CGRect lRectRoot = [rootLayer frame];

    // Available >= 10.7
    DBG_PRINT("CALayer::AddCASublayer0.0: Quirks %d, Root %p (refcnt %d), Sub %p (refcnt %d), frame0: %lf/%lf %lfx%lf scale %lf\n",
        caLayerQuirks, rootLayer, (int)[rootLayer retainCount], subLayer, (int)[subLayer retainCount],
        lRectRoot.origin.x, lRectRoot.origin.y, lRectRoot.size.width, lRectRoot.size.height, (float)contentsScale);

NS_DURING
    [subLayer setContentsScale: (CGFloat)contentsScale];
NS_HANDLER
NS_ENDHANDLER

    [subLayer setFrame:lRectRoot];
    [rootLayer addSublayer:subLayer];

    // no animations for add/remove/swap sublayers etc 
    // doesn't work: [layer removeAnimationForKey: kCAOnOrderIn, kCAOnOrderOut, kCATransition]
    [rootLayer removeAllAnimations];
    // [rootLayer addAnimation:nil forKey:kCATransition]; // JAU
    [rootLayer setAutoresizingMask: (kCALayerWidthSizable|kCALayerHeightSizable)];
    [rootLayer setNeedsDisplayOnBoundsChange: YES];
    [subLayer removeAllAnimations];
    // [subLayer addAnimation:nil forKey:kCATransition]; // JAU
    [subLayer setAutoresizingMask: (kCALayerWidthSizable|kCALayerHeightSizable)];
    [subLayer setNeedsDisplayOnBoundsChange: YES];

    if( 0 != caLayerQuirks ) {
        [rootLayer fixCALayerLayout: subLayer visible:1 x:x y:y width:width height:height caLayerQuirks:caLayerQuirks force:JNI_TRUE];
    }

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::AddCASublayer0.X: root %p (refcnt %d) .sub %p (refcnt %d)\n", 
        rootLayer, (int)[rootLayer retainCount], subLayer, (int)[subLayer retainCount]);
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    FixCALayerLayout0
 * Signature: (JJIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_FixCALayerLayout0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jboolean visible, jint x, jint y, jint width, jint height, jint caLayerQuirks)
{
    if( 0 != caLayerQuirks ) {
        NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
        MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
        if( NULL == rootLayer ) {
            NativewindowCommon_throwNewRuntimeException(env, "Argument \"rootLayer\" is null");
        }
        CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

        [CATransaction begin];
        [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

        [rootLayer fixCALayerLayout: subLayer visible:(BOOL)visible x:x y:y width:width height:height caLayerQuirks:caLayerQuirks force:JNI_FALSE];

        [CATransaction commit];

        [pool release];
    }
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    SetCALayerPixelScale0
 * Signature: (JJF)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_SetCALayerPixelScale0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jfloat contentsScale)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
    if( NULL == rootLayer ) {
        NativewindowCommon_throwNewRuntimeException(env, "Argument \"rootLayer\" is null");
    }
    CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

NS_DURING
    [rootLayer setContentsScale: (CGFloat)contentsScale];
    if( NULL != subLayer ) {
        [subLayer setContentsScale: (CGFloat)contentsScale];
    }
NS_HANDLER
NS_ENDHANDLER

    [CATransaction commit];

    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RemoveCASublayer0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RemoveCASublayer0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
    CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

    (void)rootLayer; // no warnings

    DBG_PRINT("CALayer::RemoveCASublayer0.0: root %p (refcnt %d) .sub %p (refcnt %d)\n", 
        rootLayer, (int)[rootLayer retainCount], subLayer, (int)[subLayer retainCount]);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    [subLayer removeFromSuperlayer];
    [subLayer release]; // Pairs w/ AddCASublayer
    [rootLayer release]; // Pairs w/ AddCASublayer

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::RemoveCASublayer0.X: root %p (refcnt %d) .sub %p (refcnt %d)\n", 
        rootLayer, (int)[rootLayer retainCount], subLayer, (int)[subLayer retainCount]);
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    DestroyCALayer0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_DestroyCALayer0
  (JNIEnv *env, jclass unused, jlong caLayer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyCALayer* layer = (MyCALayer*) ((intptr_t) caLayer);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    DBG_PRINT("CALayer::DestroyCALayer0.0: root %p (refcnt %d)\n", layer, (int)[layer retainCount]);
    [layer release]; // Trigger release and dealloc of root CALayer, it's child etc ..

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::DestroyCALayer0.X: root %p\n", layer);
}

/*
 * Class:     Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow
 * Method:    GetJAWTSurfaceLayersHandle0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow_GetJAWTSurfaceLayersHandle0
  (JNIEnv *env, jclass unused, jobject jawtDrawingSurfaceInfoBuffer)
{
    JAWT_DrawingSurfaceInfo* dsi = (JAWT_DrawingSurfaceInfo*) (*env)->GetDirectBufferAddress(env, jawtDrawingSurfaceInfoBuffer);
    if (NULL == dsi) {
        NativewindowCommon_throwNewRuntimeException(env, "Argument \"jawtDrawingSurfaceInfoBuffer\" was not a direct buffer");
        return 0;
    }
    NSObject *surfaceLayersObj = (NSObject*) dsi->platformInfo;
    [surfaceLayersObj retain]; // Pairs w/ Unset
    DBG_PRINT("CALayer::GetJAWTSurfaceLayersHandle: surfaceLayers %p (refcnt %d)\n", surfaceLayersObj, (int)[surfaceLayersObj retainCount]);

    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)surfaceLayersObj;
    return (jlong) (intptr_t) surfaceLayers;
}

/*
 * Class:     Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow
 * Method:    SetJAWTRootSurfaceLayer0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow_SetJAWTRootSurfaceLayer0
  (JNIEnv *env, jclass unused, jlong jawtSurfaceLayersHandle, jlong caLayer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    NSObject *surfaceLayersObj = (NSObject*) (intptr_t) jawtSurfaceLayersHandle;
    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)surfaceLayersObj;
    MyCALayer* layer = (MyCALayer*) (intptr_t) caLayer;
    DBG_PRINT("CALayer::SetJAWTRootSurfaceLayer.0: pre %p -> root %p (refcnt %d)\n", [surfaceLayers layer], layer, (int)[layer retainCount]);

    [surfaceLayers setLayer: [layer retain]]; // Pairs w/ Unset

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::SetJAWTRootSurfaceLayer.X: root %p (refcnt %d)\n", layer, (int)[layer retainCount]);
}

/*
 * Class:     Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow
 * Method:    UnsetJAWTRootSurfaceLayer0
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_jawt_macosx_MacOSXJAWTWindow_UnsetJAWTRootSurfaceLayer0
  (JNIEnv *env, jclass unused, jlong jawtSurfaceLayersHandle, jlong caLayer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    NSObject *surfaceLayersObj = (NSObject*) (intptr_t) jawtSurfaceLayersHandle;
    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)surfaceLayersObj;
    DBG_PRINT("CALayer::UnsetJAWTRootSurfaceLayer.0: surfaceLayers %p (refcnt %d)\n", surfaceLayersObj, (int)[surfaceLayersObj retainCount]);

    MyCALayer* layer = (MyCALayer*) (intptr_t) caLayer;
    if(NULL != layer) {
        if(layer != [surfaceLayers layer]) {
            NativewindowCommon_throwNewRuntimeException(env, "Attached layer %p doesn't match given layer %p\n", surfaceLayers.layer, layer);
            return;
        }
        DBG_PRINT("CALayer::UnsetJAWTRootSurfaceLayer.1: root %p (refcnt %d) -> nil\n", layer, (int)[layer retainCount]);
        [layer release]; // Pairs w/ Set
        [surfaceLayers setLayer: NULL]; // Pairs w/ Set
    }
    [surfaceLayersObj release]; // Pairs w/ Get

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::UnsetJAWTRootSurfaceLayer.X\n");
}

@interface MainRunnable : NSObject

{
    jobject runnableObj;
}

- (id) initWithRunnable: (jobject)runnable;
- (void) jRun;

#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
- (void)dealloc;
#endif


@end

@implementation MainRunnable

- (id) initWithRunnable: (jobject)runnable
{
    runnableObj = runnable;
    return [super init];
}

- (void) jRun
{
    int shallBeDetached = 0;
    JNIEnv* env = NativewindowCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    DBG_PRINT2("MainRunnable.1 env: %d\n", (int)(NULL!=env));
    if(NULL!=env) {
        DBG_PRINT2("MainRunnable.1.0\n");
        (*env)->CallVoidMethod(env, runnableObj, runnableRunID);
        DBG_PRINT2("MainRunnable.1.1\n");
        (*env)->DeleteGlobalRef(env, runnableObj);

        DBG_PRINT2("MainRunnable.1.3\n");
        // detaching thread not required - daemon
        // NativewindowCommon_ReleaseJNIEnv(shallBeDetached);
    }
    DBG_PRINT2("MainRunnable.X\n");
}

#ifdef DBG_LIFECYCLE

- (id)retain
{
    DBG_PRINT2("MainRunnable::retain.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    id o = [super retain];
    DBG_PRINT2("MainRunnable::retain.X: %p (refcnt %d)\n", o, (int)[o retainCount]);
    return o;
}

- (oneway void)release
{
    DBG_PRINT2("MainRunnable::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    [super release];
    // DBG_PRINT2("MainRunnable::release.X: %p (refcnt %d)\n", self, (int)[self retainCount]);
}

- (void)dealloc
{
    DBG_PRINT2("MainRunnable::dealloc.0 %p (refcnt %d)\n", self, (int)[self retainCount]);
    [super dealloc];
    // DBG_PRINT2("MainRunnable.dealloc.X: %p\n", self);
}

#endif

@end

static void RunOnThread (JNIEnv *env, jobject runnable, BOOL onMain, jint delayInMS)
{
    BOOL isMainThread = [NSThread isMainThread];
    BOOL forkOnMain = onMain && ( NO == isMainThread || 0 < delayInMS );

    DBG_PRINT2( "RunOnThread0: forkOnMain %d [onMain %d, delay %dms, isMainThread %d], NSApp %d, NSApp-isRunning %d\n", 
        (int)forkOnMain, (int)onMain, (int)delayInMS, (int)isMainThread, (int)(NULL!=NSApp), (int)([NSApp isRunning]));

    if ( forkOnMain ) {
        jobject runnableObj = (*env)->NewGlobalRef(env, runnable);

        DBG_PRINT2( "RunOnThread.1.0\n");
        MainRunnable * mr = [[MainRunnable alloc] initWithRunnable: runnableObj];

        if( onMain ) {
            [mr performSelectorOnMainThread:@selector(jRun) withObject:nil waitUntilDone:NO];
        } else {
            NSTimeInterval delay = (double)delayInMS/1000.0;
            [mr performSelector:@selector(jRun) withObject:nil afterDelay:delay];
        }
        DBG_PRINT2( "RunOnThread.1.1\n");

        [mr release];
        DBG_PRINT2( "RunOnThread.1.2\n");

    } else {
        DBG_PRINT2( "RunOnThread.2\n");
        (*env)->CallVoidMethod(env, runnable, runnableRunID);
    }
    DBG_PRINT2( "RunOnThread.X\n");
}

static void OSXUtil_KickNSApp() {
    NSEvent* event = [NSEvent otherEventWithType: NSApplicationDefined
                                        location: NSMakePoint(0,0)
                                   modifierFlags: 0
                                       timestamp: 0.0
                                    windowNumber: 0
                                         context: nil
                                         subtype: 0
                                           data1: 0
                                           data2: 0];
    [NSApp postEvent: event atStart: true];
}

/**
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunOnMainThread0
 * Signature: (ZLjava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RunOnMainThread0
  (JNIEnv *env, jclass unused, jboolean kickNSApp, jobject runnable)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, YES, 0);
    if( kickNSApp ) {
        OSXUtil_KickNSApp();
    }
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunLater0
 * Signature: (ZZLjava/lang/Runnable;I)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RunLater0
  (JNIEnv *env, jclass unused, jboolean onMain, jboolean kickNSApp, jobject runnable, jint delay)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, onMain ? YES : NO, delay);
    if( kickNSApp ) {
        OSXUtil_KickNSApp();
    }
    [pool release];
}

/**
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    KickNSApp0
 * Signature: (V)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_KickNSApp0
  (JNIEnv *env, jclass unused)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    OSXUtil_KickNSApp();
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    IsMainThread0
 * Signature: (V)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_IsMainThread0
  (JNIEnv *env, jclass unused)
{
    return ( [NSThread isMainThread] == YES ) ? JNI_TRUE : JNI_FALSE ;
}

/***
 * The following static functions are copied out of NEWT's OSX impl. <src/newt/native/MacWindow.m>
 * May need to push code to NativeWindow, to remove duplication.
 */
static NSScreen * NewtScreen_getNSScreenByIndex(int screen_idx) {
    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    return (NSScreen *) [screens objectAtIndex: screen_idx];
}
static CGDirectDisplayID NewtScreen_getCGDirectDisplayIDByNSScreen(NSScreen *screen) {
    // Mind: typedef uint32_t CGDirectDisplayID; - however, we assume it's 64bit on 64bit ?!
    NSDictionary * dict = [screen deviceDescription];
    NSNumber * val = (NSNumber *) [dict objectForKey: @"NSScreenNumber"];
    // [NSNumber integerValue] returns NSInteger which is 32 or 64 bit native size
    return (CGDirectDisplayID) [val integerValue];
}
static long GetDictionaryLong(CFDictionaryRef theDict, const void* key) 
{
    long value = 0;
    CFNumberRef numRef;
    numRef = (CFNumberRef)CFDictionaryGetValue(theDict, key); 
    if (numRef != NULL)
        CFNumberGetValue(numRef, kCFNumberLongType, &value);    
    return value;
}
#define CGDDGetModeRefreshRate(mode) GetDictionaryLong((mode), kCGDisplayRefreshRate)

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    GetScreenRefreshRate
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetScreenRefreshRate0
  (JNIEnv *env, jclass unused, jint scrn_idx)
{
    int res = 0;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)scrn_idx);
    DBG_PRINT("GetScreenRefreshRate.0: screen %p\n", (void *)screen);
    if(NULL != screen) {
        CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
        DBG_PRINT("GetScreenRefreshRate.1: display %p\n", (void *)(intptr_t)display);
        if(0 != display) {
            CFDictionaryRef mode = CGDisplayCurrentMode(display);
            DBG_PRINT("GetScreenRefreshRate.2: mode %p\n", (void *)mode);
            if(NULL != mode) {
                res = CGDDGetModeRefreshRate(mode);
                DBG_PRINT("GetScreenRefreshRate.3: res %d\n", res);
            }
        }
    }
    if(0 == res) {
        res = 60; // default .. (experienced on OSX 10.6.8)
    }
    DBG_PRINT("GetScreenRefreshRate.X: %d\n", (int)res);
    [pool release];
    return res;
}

