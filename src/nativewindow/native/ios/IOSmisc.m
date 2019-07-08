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
 
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <stdarg.h>
#include <unistd.h>

#include "CAEAGLLayered.h"

#import "NativeWindowProtocols.h"

#include "jogamp_nativewindow_ios_IOSUtil.h"

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
Java_jogamp_nativewindow_ios_IOSUtil_initIDs0(JNIEnv *env, jclass _unused) {
    if( NativewindowCommon_init(env) ) {
        jclass c;
        c = (*env)->FindClass(env, ClazzNamePoint);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't find %s", ClazzNamePoint);
        }
        pointClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==pointClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't use %s", ClazzNamePoint);
        }
        pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        if(NULL==pointCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't fetch %s.%s %s",
                ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNameInsets);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't find %s", ClazzNameInsets);
        }
        insetsClz = (jclass)(*env)->NewGlobalRef(env, c);
        (*env)->DeleteLocalRef(env, c);
        if(NULL==insetsClz) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't use %s", ClazzNameInsets);
        }
        insetsCstr = (*env)->GetMethodID(env, insetsClz, ClazzAnyCstrName, ClazzNameInsetsCstrSignature);
        if(NULL==insetsCstr) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't fetch %s.%s %s",
                ClazzNameInsets, ClazzAnyCstrName, ClazzNameInsetsCstrSignature);
        }

        c = (*env)->FindClass(env, ClazzNameRunnable);
        if(NULL==c) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't find %s", ClazzNameRunnable);
        }
        runnableRunID = (*env)->GetMethodID(env, c, "run", "()V");
        if(NULL==runnableRunID) {
            NativewindowCommon_FatalError(env, "FatalError Java_jogamp_nativewindow_ios_IOSUtil_initIDs0: can't fetch %s.run()V", ClazzNameRunnable);
        }
    }
    return JNI_TRUE;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_ios_IOSUtil_isCAEAGLLayer0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[CAEAGLLayer class]];
    DBG_PRINT( "isEAGLCALayer(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_ios_IOSUtil_isCALayer0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[CALayer class]];
    DBG_PRINT( "isCALayer(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_ios_IOSUtil_isUIView0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[UIView class]];
    DBG_PRINT( "isUIView(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

JNIEXPORT jboolean JNICALL 
Java_jogamp_nativewindow_ios_IOSUtil_isUIWindow0(JNIEnv *env, jclass _unused, jlong object) {
    NSObject *nsObj = (NSObject*) (intptr_t) object;
    jboolean u = [nsObj isKindOfClass:[UIWindow class]];
    DBG_PRINT( "isUIWindow(obj: %p): %s -> %d\n", nsObj, [[nsObj description] UTF8String], u);
    return u;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    getLocationOnScreen0
 * Signature: (JII)Lcom/jogamp/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong winOrView, jint src_x, jint src_y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    /**
     * return location in 0/0 top-left space,
     * OSX NSView is 0/0 bottom-left space naturally
     * iOS UIView is 0/0 top-left space naturally
     */
    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    UIWindow* win = NULL;

    if( [nsObj isKindOfClass:[UIWindow class]] ) {
        win = (UIWindow*) nsObj;
    } else if( nsObj != NULL && [nsObj isKindOfClass:[UIView class]] ) {
        UIView* view = (UIView*) nsObj;
        win = [view window];
        if( NULL == win ) {
            NativewindowCommon_throwNewRuntimeException(env, "view has null window, view %p\n", nsObj);
        }
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }
    CGPoint p = CGPointMake(src_x, src_y);
    UIScreen* screen = [win screen];
    CGPoint pS = [win convertPoint: p toCoordinateSpace: screen.fixedCoordinateSpace];

#ifdef VERBOSE_ON
    CGRect winFrame = [self frame];
    DBG_PRINT( "GetLocationOnScreen0(window: %p):: point-in[%d/%d], winFrame[%d/%d %dx%d] -> %d/%d\n",
        win, 
        (int)p.x, (int)p.y,
        (int)winFrame.origin.x, (int)winFrame.origin.y, (int)winFrame.size.width, (int)winFrame.size.height,
        (int)pS.x, (int)pS.y);
#endif

    jobject res = (*env)->NewObject(env, pointClz, pointCstr, (jint)pS.x, (jint)pS.y);

    [pool release];

    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    getInsets0
 * Signature: (J)Lcom/jogamp/nativewindow/util/Insets;
 */
JNIEXPORT jobject JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetInsets0
  (JNIEnv *env, jclass unused, jlong winOrView)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    UIWindow* win = NULL;
    jint il,ir,it,ib;

    if( [nsObj isKindOfClass:[UIWindow class]] ) {
        win = (UIWindow*) nsObj;
    } else if( nsObj != NULL && [nsObj isKindOfClass:[UIView class]] ) {
        UIView* view = (UIView*) nsObj;
        win = [view window];
        if( NULL == win ) {
            NativewindowCommon_throwNewRuntimeException(env, "view has null window, view %p\n", nsObj);
        }
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }

    UIEdgeInsets uiInsets = [win safeAreaInsets];
    il = uiInsets.left;
    ir = uiInsets.right;
    it = uiInsets.top;
    ib = uiInsets.bottom;
    /**
    CGRect frameRect = [win frame];
    CGRect contentRect = [win contentRectForFrameRect: frameRect];

    // note: this is a simplistic implementation which doesn't take
    // into account DPI and scaling factor
    CGFloat l = contentRect.origin.x - frameRect.origin.x;
    il = (jint)l;                                                     // l
    ir = (jint)(frameRect.size.width - (contentRect.size.width + l)); // r
    it = (jint)(frameRect.size.height - contentRect.size.height);     // t
    ib = (jint)(contentRect.origin.y - frameRect.origin.y);           // b
    */

    jobject res = (*env)->NewObject(env, insetsClz, insetsCstr, il, ir, it, ib);

    [pool release];

    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetScreenPixelScale1
 * Signature: (I)F
 */
JNIEXPORT jfloat JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetScreenPixelScale1
  (JNIEnv *env, jclass unused, jint screenIdx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    CGFloat pixelScale = 1.0; // default

    NSArray *screens = [UIScreen screens];
    int count = [screens count];
    if( 0 <= screenIdx && screenIdx < count ) {
        UIScreen * screen = (UIScreen *) [screens objectAtIndex: screenIdx];
        if( NULL != screen ) {
            pixelScale = [screen scale];
        }
    }
    [pool release];

    return (jfloat)pixelScale;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetScreenPixelScale1
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetScreenPixelScale2
  (JNIEnv *env, jclass unused, jlong winOrView)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSObject *nsObj = (NSObject*) (intptr_t) winOrView;
    UIScreen *screen = NULL;

    if( [nsObj isKindOfClass:[UIWindow class]] ) {
        UIWindow* win = (UIWindow*) nsObj;
        screen = [win screen];
    } else if( nsObj != NULL && [nsObj isKindOfClass:[UIView class]] ) {
        UIView* view = (UIView*) nsObj;
        UIWindow* win = [view window];
        if( NULL == win ) {
            NativewindowCommon_throwNewRuntimeException(env, "view has null window, view %p\n", nsObj);
        }
        screen = [win screen];
    } else {
        NativewindowCommon_throwNewRuntimeException(env, "neither win nor view %p\n", nsObj);
    }

    CGFloat pixelScale = [screen scale];

    [pool release];

    return (jfloat)pixelScale;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    CreateUIWindow0
 * Signature: (IIIIZ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_ios_IOSUtil_CreateUIWindow0
  (JNIEnv *env, jclass unused, jint x, jint y, jint width, jint height)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [CATransaction begin];
    CGRect boundsWin = CGRectMake(x, y, width, height);
    CGRect boundsView = CGRectMake(0, 0, width, height);

    // Allocate the window
    UIWindow *myWindow = [[[[UIWindow alloc] initWithFrame:boundsWin] autorelease] retain];
    myWindow.rootViewController = [[[UIViewController alloc] initWithNibName:nil bundle:nil] autorelease];
    [myWindow setBackgroundColor: [UIColor redColor]];

    // n/a iOS [myWindow setPreservesContentDuringLiveResize: YES];

    // FIXME invisible .. (we keep it visible for testing)
    // FIXME [myWindow setOpaque: NO];
    // FIXME [myWindow setBackgroundColor: [UIColor clearColor]];
    [myWindow makeKeyAndVisible];

    CAEAGLUIView *uiView = [[CAEAGLUIView alloc] initWithFrame:boundsView];
    CAEAGLLayer* l = (CAEAGLLayer*)[uiView layer];
    [l setOpaque: YES];
    l.drawableProperties = [NSDictionary dictionaryWithObjectsAndKeys: /* defaults */
                           [NSNumber numberWithBool:NO], kEAGLDrawablePropertyRetainedBacking, kEAGLColorFormatRGBA8, kEAGLDrawablePropertyColorFormat, nil];


    [myWindow addSubview: uiView];

    [CATransaction commit];
    [pool release];
    return (jlong) ((intptr_t) myWindow);
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    DestroyUIWindow0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_DestroyUIWindow0
  (JNIEnv *env, jclass unused, jlong nsWindow)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [CATransaction begin];

NS_DURING
    UIWindow* mWin = (UIWindow*) ((intptr_t) nsWindow);
    [mWin resignKeyWindow];
    [mWin setHidden: YES];
    NSArray* subviews = [mWin subviews];
    if(NULL != subviews) {
        for(int i=0; i<[subviews count]; i++) {
            UIView* sub = [subviews objectAtIndex: i];
            [sub setHidden: YES];
            [sub release];
        }
    }
    [mWin release];
NS_HANDLER
    // On killing or terminating the process [UIWindow _close], rarely
    // throws an NSRangeException while ordering out menu items
NS_ENDHANDLER

    [CATransaction commit];
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetCALayer0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetCALayer0
  (JNIEnv *env, jclass unused, jlong view)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIView* uiView = (UIWindow*) ((intptr_t) view);

    jlong res = (jlong) ((intptr_t) (CALayer *) [uiView layer]);

    DBG_PRINT( "GetCALayer(view: %p): %p\n", uiView, (void*) (intptr_t) res);

    [pool release];
    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetCAEAGLLayer0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetCAEAGLLayer0
  (JNIEnv *env, jclass unused, jlong view)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIView* uiView = (UIWindow*) ((intptr_t) view);

    CALayer* l = [uiView layer];
    jboolean isRes = [l isKindOfClass:[CAEAGLLayer class]];
    jlong res = isRes ? (jlong) ((intptr_t) l) : 0;

    DBG_PRINT( "GetCAEAGLLayer(view: %p): CALayer %p, CAEAGLLayer %p (%d)\n", uiView, l, (void*) (intptr_t) res, isRes);

    [pool release];
    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetUIView0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetUIView0
  (JNIEnv *env, jclass unused, jlong window, jboolean onlyCAEAGLUIView)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIWindow* win = (UIWindow*) ((intptr_t) window);
    jlong res;

    if( onlyCAEAGLUIView ) {
        CAEAGLUIView* v0 = [CAEAGLUIView findCAEAGLUIView: win
                                         startIdx: 0];
        res = (jlong) ((intptr_t) v0);
    } else {
        UIView* v0 = [CAEAGLUIView getUIView: win startIdx: 0];
        res = (jlong) ((intptr_t) v0);
    }

    DBG_PRINT( "GetUIView(window: %p): %p\n", win, (void*) (intptr_t) res);

    [pool release];
    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetUIWindow0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetUIWindow0
  (JNIEnv *env, jclass unused, jlong view)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIView* v = (UIView*) ((intptr_t) view);

    jlong res = (jlong) ((intptr_t) [v window]);

    DBG_PRINT( "GetUIWindow(view: %p): %p\n", v, (void*) (intptr_t) res);

    [pool release];
    return res;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    SetUIViewPixelScale0
 * Signature: (JF)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_SetUIViewPixelScale0
  (JNIEnv *env, jclass unused, jlong view, jfloat contentScale)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIView* v = (UIView*) ((intptr_t) view);

NS_DURING
    if( NULL != v ) {
        [v setContentScaleFactor: (CGFloat)contentScale];
    }
NS_HANDLER
NS_ENDHANDLER

    [pool release];
}
/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetUIViewPixelScale0
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetUIViewPixelScale0
  (JNIEnv *env, jclass unused, jlong view)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIView* v = (UIView*) ((intptr_t) view);
    jfloat r = 0.0f;

NS_DURING
    if( NULL != v ) {
        r = [v contentScaleFactor];
    }
NS_HANDLER
NS_ENDHANDLER

    [pool release];
    return r;
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    SetCALayerPixelScale0
 * Signature: (JJF)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_SetCALayerPixelScale0
  (JNIEnv *env, jclass unused, jlong rootCALayer, jlong subCALayer, jfloat contentsScale)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    CALayer* rootLayer = (CALayer*) ((intptr_t) rootCALayer);
    CALayer* subLayer = (CALayer*) ((intptr_t) subCALayer);

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

NS_DURING
    if( NULL != rootLayer ) {
        [rootLayer setContentsScale: (CGFloat)contentsScale];
    }
    if( NULL != subLayer ) {
        [subLayer setContentsScale: (CGFloat)contentsScale];
    }
NS_HANDLER
NS_ENDHANDLER

    [CATransaction commit];

    [pool release];
}
/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    GetCALayerPixelScale0
 * Signature: (J)F
 */
JNIEXPORT jfloat JNICALL Java_jogamp_nativewindow_ios_IOSUtil_GetCALayerPixelScale0
  (JNIEnv *env, jclass unused, jlong caLayer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    CALayer* v = (CALayer*) ((intptr_t) caLayer);
    jfloat r = 0.0f;

NS_DURING
    if( NULL != v ) {
        r = [v contentsScale];
    }
NS_HANDLER
NS_ENDHANDLER

    [pool release];
    return r;
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

// #define UIApp [UIApplication sharedApplication]
// #define NSApp [NSApplication sharedApplication]

static void RunOnThread (JNIEnv *env, jobject runnable, BOOL onMain, jint delayInMS)
{
    BOOL isMainThread = [NSThread isMainThread];
    BOOL forkOnMain = onMain && ( NO == isMainThread || 0 < delayInMS );
    // UIApplication * UIApp = [UIApplication sharedApplication];

    DBG_PRINT2( "RunOnThread0: forkOnMain %d [onMain %d, delay %dms, isMainThread %d], UIApp %d, UIApp-isRunning %d\n", 
        (int)forkOnMain, (int)onMain, (int)delayInMS, (int)isMainThread, (int)(NULL!=UIApp), (int)([UIApp applicationState]));

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

static void IOSUtil_KickUIApp() {
    // UIApplication * UIApp = [UIApplication sharedApplication];
    /*** iOS-FIXME: UIEvent creation and post n/a !!!
    UIEvent* event = [UIEvent otherEventWithType: UIApplicationDefined
                                        location: NSMakePoint(0,0)
                                   modifierFlags: 0
                                       timestamp: 0.0
                                    windowNumber: 0
                                         context: nil
                                         subtype: 0
                                           data1: 0
                                           data2: 0];
    [UIApp postEvent: event atStart: true];
    */
}

/**
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    RunOnMainThread0
 * Signature: (ZLjava/lang/Runnable;)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_RunOnMainThread0
  (JNIEnv *env, jclass unused, jboolean kickUIApp, jobject runnable)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, YES, 0);
    if( kickUIApp ) {
        IOSUtil_KickUIApp();
    }
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    RunLater0
 * Signature: (ZZLjava/lang/Runnable;I)V
 */
JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_RunLater0
  (JNIEnv *env, jclass unused, jboolean onMain, jboolean kickUIApp, jobject runnable, jint delay)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    RunOnThread (env, runnable, onMain ? YES : NO, delay);
    if( kickUIApp ) {
        IOSUtil_KickUIApp();
    }
    [pool release];
}

/*
 * Class:     Java_jogamp_nativewindow_ios_IOSUtil
 * Method:    IsMainThread0
 * Signature: (V)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_nativewindow_ios_IOSUtil_IsMainThread0
  (JNIEnv *env, jclass unused)
{
    return ( [NSThread isMainThread] == YES ) ? JNI_TRUE : JNI_FALSE ;
}

#define EAGL_TEST 1

#ifdef EAGL_TEST
#include <OpenGLES/ES2/gl.h>
#include <OpenGLES/ES2/glext.h>

@interface GLView : UIView
{    
    CAEAGLLayer *glLayer;
    EAGLContext *glContext;
    GLuint renderbuffer;
}
@end
@implementation GLView
+ (Class)layerClass
{
    return [CAEAGLLayer class];
}

- (id)initWithFrame:(CGRect)frame
{
    if (self = [super initWithFrame:frame]) {
        [self initGL];
        [self render];
    }
    
    return self;
}

- (void)render
{
    glClearColor(150.0/255.0, 200.0/255.0, 255.0/255.0, 1.0);
    
    glClear(GL_COLOR_BUFFER_BIT);
    
    [glContext presentRenderbuffer:GL_RENDERBUFFER];
}

- (void)initGL
{    
    glLayer = (CAEAGLLayer *)self.layer;
    glLayer.opaque = YES;

    glContext = [[EAGLContext alloc] initWithAPI:kEAGLRenderingAPIOpenGLES2];
    if (!glContext) {
        NSLog(@"Unable to create EAGLContext");
        exit(1);
    }
    if (![EAGLContext setCurrentContext:glContext]) {
        NSLog(@"Unable to set current EAGLContext");
        exit(1);
    }
    glGenRenderbuffers(1, &renderbuffer);
    glBindRenderbuffer(GL_RENDERBUFFER, renderbuffer);
    [glContext renderbufferStorage:GL_RENDERBUFFER fromDrawable:glLayer];

    GLuint framebuffer;
    glGenFramebuffers(1, &framebuffer);
    glBindFramebuffer(GL_FRAMEBUFFER, framebuffer);
    glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_RENDERBUFFER, renderbuffer);
}
@end
#endif /* EAGL_TEST */

JNIEXPORT void JNICALL Java_jogamp_nativewindow_ios_IOSUtil_CreateGLViewDemoA0
  (JNIEnv *env, jclass unused)
{
#ifdef EAGL_TEST
    CGRect boundsW2 = CGRectMake(500, 10, 320, 320);
    CGRect boundsV2 = CGRectMake(0, 0, 320, 320);
    UIWindow* window2 = [[[[UIWindow alloc] initWithFrame:boundsW2] autorelease] retain];
    window2.rootViewController = [[[UIViewController alloc] initWithNibName:nil bundle:nil] autorelease];
    [window2 setBackgroundColor: [UIColor redColor]];
    [window2 makeKeyAndVisible];

    GLView *glView = [[GLView alloc] initWithFrame:boundsV2];
    [window2 addSubview:glView];
#endif /* EAGL_TEST */
}

