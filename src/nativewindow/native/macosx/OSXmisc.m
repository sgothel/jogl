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

static const char * const ClazzNamePoint = "javax/media/nativewindow/util/Point";
static const char * const ClazzNamePointCstrSignature = "(II)V";
static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static const char * const ClazzNameInsets = "javax/media/nativewindow/util/Insets";
static const char * const ClazzNameInsetsCstrSignature = "(IIII)V";
static jclass insetsClz = NULL;
static jmethodID insetsCstr = NULL;

static int _initialized=0;

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_macosx_OSXUtil_initIDs0(JNIEnv *env, jclass _unused) {
    if(0==_initialized) {
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
        _initialized=1;
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

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    getLocationOnScreen0
 * Signature: (JII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_GetLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong winOrView, jint src_x, jint src_y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    /**
     * return location in 0/0 top-left space,
     * OSX is 0/0 bottom-left space naturally
     */
    NSRect r;
    int dest_x=-1;
    int dest_y=-1;

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
        NativewindowCommon_throwNewRuntimeException(env, "neither win not view %p\n", nsObj);
    }
    NSScreen* screen = [win screen];
    NSRect screenRect = [screen frame];
    NSRect winFrame = [win frame];

    r.origin.x = src_x;
    r.origin.y = winFrame.size.height - src_y; // y-flip for 0/0 top-left
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectToScreen: r]; // 10.7
    NSPoint oS = [win convertBaseToScreen: r.origin];
    /**
    NSLog(@"LOS.1: (bottom-left) %d/%d, screen-y[0: %d, h: %d], (top-left) %d/%d\n", 
        (int)oS.x, (int)oS.y, (int)screenRect.origin.y, (int) screenRect.size.height,
        (int)oS.x, (int)(screenRect.origin.y + screenRect.size.height - oS.y)); */

    dest_x = (int) oS.x;
    dest_y = (int) screenRect.origin.y + screenRect.size.height - oS.y;

    jobject res = (*env)->NewObject(env, pointClz, pointCstr, (jint)dest_x, (jint)dest_y);

    [pool release];

    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    getInsets0
 * Signature: (J)Ljavax/media/nativewindow/util/Insets;
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
        NativewindowCommon_throwNewRuntimeException(env, "neither win not view %p\n", nsObj);
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
                                           defer: YES];
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
}
- (id)init;
#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
- (void)dealloc;
#endif
- (id<CAAction>)actionForKey:(NSString *)key ;

@end

@implementation MyCALayer

- (id)init
{
    DBG_PRINT("MyCALayer::init.0\n");
    MyCALayer * o = [super init];
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

@end

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    CreateCALayer0
 * Signature: (II)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_CreateCALayer0
  (JNIEnv *env, jclass unused, jint width, jint height)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    MyCALayer* layer = [[MyCALayer alloc] init];
    DBG_PRINT("CALayer::CreateCALayer.0: root %p 0/0 %dx%d (refcnt %d)\n", layer, (int)width, (int)height, (int)[layer retainCount]);
    // avoid zero size
    if(0 == width) { width = 32; }
    if(0 == height) { height = 32; }

    // initial dummy size !
    CGRect lRect = [layer frame];
    lRect.origin.x = 0;
    lRect.origin.y = 0;
    lRect.size.width = width;
    lRect.size.height = height;
    [layer setFrame: lRect];
    // no animations for add/remove/swap sublayers etc 
    // doesn't work: [layer removeAnimationForKey: kCAOnOrderIn, kCAOnOrderOut, kCATransition]
    [layer removeAllAnimations];
    // [layer addAnimation:nil forKey:kCATransition];
    [layer setAutoresizingMask: (kCALayerWidthSizable|kCALayerHeightSizable)];
    [layer setNeedsDisplayOnBoundsChange: YES];
    DBG_PRINT("CALayer::CreateCALayer.1: root %p %lf/%lf %lfx%lf\n", layer, lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height);
    [pool release];
    DBG_PRINT("CALayer::CreateCALayer.X: root %p (refcnt %d)\n", layer, (int)[layer retainCount]);

    return (jlong) ((intptr_t) layer);
}

static void FixCALayerLayout0(MyCALayer* rootLayer, CALayer* subLayer, jint width, jint height, jint caLayerQuirks, jboolean force) {
    if( NULL != rootLayer ) {
        CGRect lRect = [rootLayer frame];
        int posQuirk  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & caLayerQuirks ) && ( lRect.origin.x!=0 || lRect.origin.y!=0 );
        int sizeQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & caLayerQuirks ) && ( lRect.size.width!=width || lRect.size.height!=height );
        CGFloat _x, _y, _w, _h;
        // force root -> 0/0
        _x = 0;
        _y = 0;
        posQuirk |= 8;
        if( sizeQuirk ) {
            _w = width;
            _h = height;
        } else {
            _w = lRect.size.width;
            _h = lRect.size.height;
        }
        DBG_PRINT("CALayer::FixCALayerLayout0.0: Quirks [%d, pos %d, size %d], Root %p frame %lf/%lf %lfx%lf, usr %dx%d -> %lf/%lf %lfx%lf\n",
            caLayerQuirks, posQuirk, sizeQuirk, rootLayer, lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height, 
            width, height, _x, _y, _w, _h);
        if( posQuirk || sizeQuirk ) {
            lRect.origin.x = _x;
            lRect.origin.y = _y;
            lRect.size.width = _w;
            lRect.size.height = _h;
            [rootLayer setFrame: lRect];
        }
    }
    if( NULL != subLayer ) {
        CGRect lRect = [subLayer frame];
        int sizeQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & caLayerQuirks ) && ( lRect.size.width!=width || lRect.size.height!=height );
        CGFloat _x, _y, _w, _h;
        int posQuirk  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & caLayerQuirks ) && ( lRect.origin.x!=0 || lRect.origin.y!=0 );
        if( posQuirk ) {
            _x = 0;
            _y = 0;
        } else {
            // sub always rel to root
            _x = lRect.origin.x;
            _y = lRect.origin.y;
        }
        if( sizeQuirk ) {
            _w = width;
            _h = height;
        } else {
            _w = lRect.size.width;
            _h = lRect.size.height;
        }
        DBG_PRINT("CALayer::FixCALayerLayout1.0: Quirks [%d, pos %d, size %d], SubL %p frame %lf/%lf %lfx%lf, usr %dx%d -> %lf/%lf %lfx%lf\n",
            caLayerQuirks, posQuirk, sizeQuirk, subLayer, lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height,
            width, height, _x, _y, _w, _h);
        if( force || posQuirk || sizeQuirk ) {
            lRect.origin.x = _x;
            lRect.origin.y = _y;
            lRect.size.width = _w;
            lRect.size.height = _h;
            if( [subLayer conformsToProtocol:@protocol(NWDedicatedFrame)] ) {
                CALayer <NWDedicatedFrame> * subLayerDS = (CALayer <NWDedicatedFrame> *) subLayer;
                [subLayerDS setDedicatedFrame: lRect quirks: caLayerQuirks];
            } else {
                [subLayer setFrame: lRect];
            }
        }
    }
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    AddCASublayer0
 * Signature: (JJIIIII)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_AddCASublayer0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jint width, jint height, jint caLayerQuirks)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
    CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    [rootLayer retain]; // Pairs w/ RemoveCASublayer
    [subLayer retain]; // Pairs w/ RemoveCASublayer

    CGRect lRectRoot = [rootLayer frame];
    int posQuirk  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & caLayerQuirks );
    int sizeQuirk = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & caLayerQuirks );

    DBG_PRINT("CALayer::AddCASublayer0.0: Quirks [%d, pos %d, size %d], Root %p (refcnt %d), Sub %p (refcnt %d), frame0: %lf/%lf %lfx%lf\n",
        caLayerQuirks, posQuirk, sizeQuirk, rootLayer, (int)[rootLayer retainCount], subLayer, (int)[subLayer retainCount],
        lRectRoot.origin.x, lRectRoot.origin.y, lRectRoot.size.width, lRectRoot.size.height);

    CGPoint origin = lRectRoot.origin; // save
    // force root to 0/0
    lRectRoot.origin.x = 0;
    lRectRoot.origin.y = 0;
    [rootLayer setFrame: lRectRoot];

    // simple 1:1 layout rel. to root-layer !
    if( !posQuirk ) {
        lRectRoot.origin = origin;
    }
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
        FixCALayerLayout0(rootLayer, subLayer, width, height, caLayerQuirks, JNI_TRUE);
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
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jint width, jint height, jint caLayerQuirks)
{
    if( 0 != caLayerQuirks ) {
        NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
        MyCALayer* rootLayer = (MyCALayer*) ((intptr_t) rootCALayer);
        CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

        [CATransaction begin];
        [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

        FixCALayerLayout0(rootLayer, subLayer, width, height, caLayerQuirks, JNI_FALSE);

        [CATransaction commit];

        [pool release];
    }
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
    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)dsi->platformInfo;
    return (jlong) ((intptr_t) surfaceLayers);
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

    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)(intptr_t)jawtSurfaceLayersHandle;
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

    id <JAWT_SurfaceLayers> surfaceLayers = (id <JAWT_SurfaceLayers>)(intptr_t)jawtSurfaceLayersHandle;
    MyCALayer* layer = (MyCALayer*) (intptr_t) caLayer;
    if(layer != [surfaceLayers layer]) {
        NativewindowCommon_throwNewRuntimeException(env, "Attached layer %p doesn't match given layer %p\n", surfaceLayers.layer, layer);
        return;
    }
    DBG_PRINT("CALayer::UnsetJAWTRootSurfaceLayer.0: root %p (refcnt %d) -> nil\n", layer, (int)[layer retainCount]);
    [layer release]; // Pairs w/ Set
    [surfaceLayers setLayer: NULL];

    [CATransaction commit];

    [pool release];
    DBG_PRINT("CALayer::UnsetJAWTRootSurfaceLayer.X: root %p (refcnt %d) -> nil\n", layer, (int)[layer retainCount]);
}

@interface MainRunnable : NSObject

{
    JavaVM *jvmHandle;
    int jvmVersion;
    jobject runnableObj;
}

- (id) initWithRunnable: (jobject)runnable jvmHandle: (JavaVM*)jvm jvmVersion: (int)jvmVers;
- (void) jRun;

#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
- (void)dealloc;
#endif


@end

@implementation MainRunnable

- (id) initWithRunnable: (jobject)runnable jvmHandle: (JavaVM*)jvm jvmVersion: (int)jvmVers
{
    jvmHandle = jvm;
    jvmVersion = jvmVers;
    runnableObj = runnable;
    return [super init];
}

- (void) jRun
{
    int shallBeDetached = 0;
    JNIEnv* env = NativewindowCommon_GetJNIEnv(jvmHandle, jvmVersion, 1 /* asDaemon */, &shallBeDetached);
    DBG_PRINT2("MainRunnable.1 env: %d\n", (int)(NULL!=env));
    if(NULL!=env) {
        DBG_PRINT2("MainRunnable.1.0\n");
        (*env)->CallVoidMethod(env, runnableObj, runnableRunID);
        DBG_PRINT2("MainRunnable.1.1\n");
        (*env)->DeleteGlobalRef(env, runnableObj);

        if (shallBeDetached) {
            DBG_PRINT2("MainRunnable.1.3\n");
            // Keep attached on main thread !
            // (*jvmHandle)->DetachCurrentThread(jvmHandle);
        }
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
    DBG_PRINT2( "RunOnThread0: isMainThread %d, NSApp %d, NSApp-isRunning %d, onMain %d, delay %dms\n", 
        (int)([NSThread isMainThread]), (int)(NULL!=NSApp), (int)([NSApp isRunning]), (int)onMain, (int)delayInMS);

    if ( !onMain || NO == [NSThread isMainThread] ) {
        jobject runnableObj = (*env)->NewGlobalRef(env, runnable);

        JavaVM *jvmHandle = NULL;
        int jvmVersion = 0;

        if(0 != (*env)->GetJavaVM(env, &jvmHandle)) {
            jvmHandle = NULL;
        } else {
            jvmVersion = (*env)->GetVersion(env);
        }

        DBG_PRINT2( "RunOnThread.1.0\n");
        MainRunnable * mr = [[MainRunnable alloc] initWithRunnable: runnableObj jvmHandle: jvmHandle jvmVersion: jvmVersion];

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

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunOnMainThread0
 * Signature: (ZLjava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RunOnMainThread0
  (JNIEnv *env, jclass unused, jobject runnable)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, YES, 0);
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_macosx_OSXUtil
 * Method:    RunLater0
 * Signature: (ZLjava/lang/Runnable;I)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_macosx_OSXUtil_RunLater0
  (JNIEnv *env, jclass unused, jobject runnable, jint delay)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, NO, delay);
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

