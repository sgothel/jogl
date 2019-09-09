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

#import <inttypes.h>

#import "jogamp_newt_driver_ios_WindowDriver.h"
#import "IOSNewtUIWindow.h"

#import "MouseEvent.h"
#import "KeyEvent.h"
#import "ScreenMode.h"

#import <stdio.h>

#ifdef DBG_PERF
    #include "timespec.h"
#endif

static const char * const ClazzNamePoint = "com/jogamp/nativewindow/util/Point";
static const char * const ClazzAnyCstrName = "<init>";
static const char * const ClazzNamePointCstrSignature = "(II)V";
static jclass pointClz = NULL;
static jmethodID pointCstr = NULL;

static NSString* jstringToNSString(JNIEnv* env, jstring jstr)
{
    const jchar* jstrChars = (*env)->GetStringChars(env, jstr, NULL);
    NSString* str = [[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, jstr)];
    (*env)->ReleaseStringChars(env, jstr, jstrChars);
    return str;
}

static void setWindowClientTopLeftPoint(NewtUIWindow* mWin, jint x, jint y, BOOL doDisplay) {
    DBG_PRINT( "setWindowClientTopLeftPoint.0 - window: %p %d/%d, display %d\n", mWin, (int)x, (int)y, (int)doDisplay);
    CGPoint pS = CGPointMake(x, y);
    CGRect rect = [mWin frame];
    rect.origin = pS;

    [mWin setFrame: rect];
    DBG_PRINT( "setWindowClientTopLeftPoint.X: %d/%d\n", (int)pS.x, (int)pS.y);

    if( doDisplay ) {
        [mWin setNeedsDisplay];
    }
}

static void setWindowClientTopLeftPointAndSize(NewtUIWindow* mWin, jint x, jint y, jint width, jint height, BOOL doDisplay) {
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.0 - window: %p %d/%d %dx%d, display %d\n", mWin, (int)x, (int)y, (int)width, (int)height, (int)doDisplay);
    CGRect rect = CGRectMake(x, y, width, height);
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.1: %d/%d %dx%d\n", (int)rect.origin.x, (int)rect.origin.y, (int)rect.size.width, (int)rect.size.height);

    [mWin setFrame: rect];
    if( doDisplay ) {
        [mWin setNeedsDisplay];
    }
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.X: %d/%d %dx%d\n", (int)rect.origin.x, (int)rect.origin.y, (int)rect.size.width, (int)rect.size.height);

    // if( doDisplay ) {
    //     [mWin setNeedsDisplay];
    // }
}

#ifdef VERBOSE_ON
static int getRetainCount(NSObject * obj) {
    return ( NULL == obj ) ? -1 : (int)([obj retainCount]) ;
}
#endif

static void setJavaWindowObject(JNIEnv *env, jobject newJavaWindowObject, NewtUIView *view) {
    DBG_PRINT( "setJavaWindowObject.0: View %p\n", view);
    if( NULL == newJavaWindowObject ) {
        // disable
        jobject globJavaWindowObject = [view getJavaWindowObject];
        if( NULL != globJavaWindowObject ) {
            DBG_PRINT( "setJavaWindowObject.1: View %p - Clear old javaWindowObject %p\n", view, globJavaWindowObject);
            (*env)->DeleteGlobalRef(env, globJavaWindowObject);
            [view setJavaWindowObject: NULL];
        }
    } else {
        // enable
        DBG_PRINT( "setJavaWindowObject.2: View %p - Set new javaWindowObject %p\n", view, newJavaWindowObject);
        jobject globJavaWindowObject = (*env)->NewGlobalRef(env, newJavaWindowObject);
        [view setJavaWindowObject: globJavaWindowObject];
    }
    DBG_PRINT( "setJavaWindowObject.X: View %p\n", view);
}

static void changeContentView(JNIEnv *env, jobject javaWindowObject, UIView *pview, NewtUIWindow *win, NewtUIView *newView, BOOL setJavaWindow) {
    NewtUIView* oldNewtUIView = [win getContentNewtUIView];
#ifdef VERBOSE_ON
    int dbgIdx = 1;
#endif
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), parent view %p\n", 
        dbgIdx++, win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView), pview);

    if( NULL != oldNewtUIView ) {
        [oldNewtUIView setDestroyNotifySent: false];
        setJavaWindowObject(env, NULL, oldNewtUIView);
        [win setContentNewtUIView: NULL]; // includes [oldNewtUIView removeFromSuperview];
    }
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d\n", 
        dbgIdx++, win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView), [newView isHidden]);

    if( NULL!=newView ) {
        [newView setDestroyNotifySent: false];
        if( setJavaWindow ) {
            setJavaWindowObject(env, javaWindowObject, newView);
        }
        DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d)\n", 
            dbgIdx++, win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView));

        [win setContentNewtUIView: newView]; // includes [win addSubview: newView];
        // FIXME: Done in initWindow0: [pview addSubview: newView] 

        DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d\n", 
            dbgIdx++, win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView), [newView isHidden]);
    }
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d\n", 
        dbgIdx++, win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView), [newView isHidden]);

    // make sure the insets are updated in the java object
    [win updateInsets: env jwin:javaWindowObject];

    DBG_PRINT( "changeContentView.X win %p, view (%p,%d -> %p,%d)\n", 
        win, oldNewtUIView, getRetainCount(oldNewtUIView), newView, getRetainCount(newView));
}

/*
 * Class:     jogamp_newt_driver_ios_DisplayDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_ios_DisplayDriver_initUIApplication0
  (JNIEnv *env, jclass clazz)
{
    static int initialized = 0;

    if(initialized) return JNI_TRUE;
    initialized = 1;

    NewtCommon_init(env);

    // Initialize the shared NSApplication instance
    [UIApplication sharedApplication];

    // Need this when debugging, as it is necessary to attach gdb to
    // the running java process -- "gdb java" doesn't work
    //    printf("Going to sleep for 10 seconds\n");
    //    sleep(10);

    return (jboolean) JNI_TRUE;
}

static void NewtScreen_dump() {
#ifdef VERBOSE_ON
    NSArray *screens = [UIScreen screens];
    int i;
    for(i=0; i<[screens count]; i++) {
        UIScreen * screen = (UIScreen *) [screens objectAtIndex: i];
        CGRect screenPxFrame = [screen nativeBounds];
        CGRect screenPtFrame = [screen bounds];
        CGFloat pixelScale = [screen scale];
        CGFloat nativeScale = [screen nativeScale];
        int depth = 32; // [screen depth]; // an (int) value!
        DBG_PRINT( "UIScreen #%d (%p): Frame %lf/%lf %lfx%lf px (%lf/%lf %lfx%lf pt), scale %lf (%lf native), depth %d\n",
            i, screen,
            screenPxFrame.origin.x, screenPxFrame.origin.y, screenPxFrame.size.width, screenPxFrame.size.height,
            screenPtFrame.origin.x, screenPtFrame.origin.y, screenPtFrame.size.width, screenPtFrame.size.height,
            pixelScale, nativeScale, depth);
    }
#endif
}

// Duplicate each Mode by all possible rotations (4):
// For each real-mode: [mode, 0], [mode, 90], [mode, 180], [mode, 270]
#define ROTMODES_PER_REALMODE 1

/*
 * Class:     jogamp_newt_driver_ios_ScreenDriver
 * Method:    getMonitorDeviceIds0
 * Signature: ()I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_ios_ScreenDriver_getMonitorDeviceIds0
  (JNIEnv *env, jobject obj)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSArray *screens = [UIScreen screens];
    int count = [screens count];
    int32_t displayIDs[count];
    int i;
    for(i=0; i<count; i++) {
        // UIScreen * screen = (UIScreen *) [screens objectAtIndex: i];
        displayIDs[i] = i; // TODO no unique screen name?
    }
    jintArray properties = (*env)->NewIntArray(env, count);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", count);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, count, displayIDs);
    [pool release];
    return properties;
}

/*
 * Class:     jogamp_newt_driver_ios_ScreenDriver
 * Method:    getMonitorProps0
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_ios_ScreenDriver_getMonitorProps0
  (JNIEnv *env, jobject obj, jint crt_id)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

#ifdef DBG_PERF
    struct timespec t0, t1, td;
    long td_ms;
    timespec_now(&t0);
#endif

#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getMonitorProps0.1: %ld ms\n", td_ms); fflush(NULL);
#endif
    NSArray *screens = [UIScreen screens];
    // int count = [screens count];
    UIScreen * screen = (UIScreen *) [screens objectAtIndex: crt_id];
    if( NULL == screen ) {
        [pool release];
        return NULL;
    }
    BOOL isPrimary = 0 == crt_id;
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getMonitorProps0.2: %ld ms\n", td_ms); fflush(NULL);
#endif

    // UIScreenMode * screenMode = [screen currentMode];
    CGSize sizeMM = CGSizeMake(161.0, 228.0); // TODO ???
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getMonitorProps0.3: %ld ms\n", td_ms); fflush(NULL);
#endif

    CGRect bounds = [screen bounds];
#ifdef VERBOSE_ON
    {
        CGRect nativeBounds = [screen nativeBounds];
        CGFloat nativeScale = [screen nativeScale];
        CGFloat scale = [screen scale];
        DBG_PRINT( "getMonitorProps0: crt_id 0x%X (prim %d) %p[\n  native %f/%f %fx%f scale %f; logical %f/%f %fx%f scale %f]\n",
                   (int)crt_id, isPrimary, screen,
                   nativeBounds.origin.x, nativeBounds.origin.y, nativeBounds.size.width, nativeBounds.size.height, nativeScale,
                   bounds.origin.x, bounds.origin.y, bounds.size.width, bounds.size.height, scale);
    }
#endif

    jsize propCount = MIN_MONITOR_DEVICE_PROPERTIES - 1 - NUM_MONITOR_MODE_PROPERTIES;
    jint prop[ propCount ];
    int offset = 0;
    prop[offset++] = propCount;
    prop[offset++] = crt_id;
    prop[offset++] = 0; // isClone
    prop[offset++] = isPrimary ? 1 : 0; // isPrimary
    prop[offset++] = (jint) sizeMM.width;
    prop[offset++] = (jint) sizeMM.height;
    prop[offset++] = (jint) bounds.origin.x;    // rotated viewport x      (pixel units, will be fixed in java code)
    prop[offset++] = (jint) bounds.origin.y;    // rotated viewport y      (pixel units, will be fixed in java code)
    prop[offset++] = (jint) bounds.size.width;  // rotated viewport width  (pixel units, will be fixed in java code)
    prop[offset++] = (jint) bounds.size.height; // rotated viewport height (pixel units, will be fixed in java code)
    prop[offset++] = (jint) bounds.origin.x;    // rotated viewport x      (window units, will be fixed in java code)
    prop[offset++] = (jint) bounds.origin.y;    // rotated viewport y      (window units, will be fixed in java code)
    prop[offset++] = (jint) bounds.size.width;  // rotated viewport width  (window units, will be fixed in java code)
    prop[offset++] = (jint) bounds.size.height; // rotated viewport height (window units, will be fixed in java code)

    jintArray properties = (*env)->NewIntArray(env, propCount);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", propCount);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, propCount, prop);
    
    [pool release];

    return properties;
}

/*
 * Class:     jogamp_newt_driver_ios_ScreenDriver
 * Method:    getMonitorMode0
 * Signature: (II)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_ios_ScreenDriver_getMonitorMode0
  (JNIEnv *env, jobject obj, jint crt_id, jint mode_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSArray<UIScreen*> *screens = [UIScreen screens];
    // int count = [screens count];
    UIScreen * screen = (UIScreen *) [screens objectAtIndex: crt_id];
    if( NULL == screen ) {
        [pool release];
        return NULL;
    }
    NSArray<UIScreenMode*> *availableModes = [screen availableModes];
    int numberOfAvailableModes = [availableModes count];
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes; 
    int currentCCWRot = 0;
    jint ccwRot = 0;
    int nativeId = 0;
    UIScreenMode * mode = NULL;

#ifdef VERBOSE_ON
    if(0 >= mode_idx) {
        // only for current mode (-1) and first mode (scanning)
        CGFloat pixelScale = [screen scale];
        DBG_PRINT( "getScreenMode0: crtID 0x%X (s %p, pscale %lf), mode %d, avail: %d/%d, current rot %d ccw\n",  
            crt_id, screen, pixelScale, (int)mode_idx, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots, currentCCWRot);
    }
#endif

    if(numberOfAvailableModesRots<=mode_idx) {
        // n/a - end of modes
        DBG_PRINT( "getScreenMode0: end of modes: mode %d, avail: %d/%d\n",
            (int)mode_idx, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots);
        [pool release];
        return NULL;
    } else if(-1 < mode_idx) {
        // only at initialization time, where index >= 0
        nativeId = mode_idx / ROTMODES_PER_REALMODE;
        ccwRot = mode_idx % ROTMODES_PER_REALMODE * 90;
        mode = (UIScreenMode*) [availableModes objectAtIndex: nativeId];
    } else {
        // current mode
        mode = [screen currentMode];
        ccwRot = 0;
        nativeId = 0;
    }
    // mode = CGDisplayModeRetain(mode); // 10.6 on CGDisplayModeRef

    CGSize mSize = [mode size];
    int mWidth = (int)mSize.width;
    int mHeight = (int)mSize.height;

    // swap width and height, since OSX reflects rotated dimension, we don't
    if ( 90 == currentCCWRot || 270 == currentCCWRot ) {
        int tempWidth = mWidth;
        mWidth = mHeight;
        mHeight = tempWidth;
    }

    jint prop[ NUM_MONITOR_MODE_PROPERTIES_ALL ];
    int propIndex = 0;

    int refreshRate = 60; // TODO
    int fRefreshRate = ( 0 < refreshRate ) ? refreshRate : 60; // default .. (experienced on OSX 10.6.8)
    prop[propIndex++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
    prop[propIndex++] = mWidth;
    prop[propIndex++] = mHeight;
    prop[propIndex++] = 32; // TODO CGDDGetModeBitsPerPixel(mode);
    prop[propIndex++] = fRefreshRate * 100; // Hz*100
    prop[propIndex++] = 0; // flags
    prop[propIndex++] = nativeId;
    prop[propIndex++] = ccwRot;
 
    DBG_PRINT( "getScreenMode0: Mode %d/%d (%d): %dx%d, %d bpp, %d / %d Hz, nativeId %d, rot %d ccw\n",
        (int)mode_idx, (int)numberOfAvailableModesRots, (int)numberOfAvailableModes, 
        (int)prop[1], (int)prop[2], (int)prop[3],
        (int)prop[4], refreshRate, (int)prop[6], (int)prop[7]);

    jintArray properties = (*env)->NewIntArray(env, NUM_MONITOR_MODE_PROPERTIES_ALL);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", NUM_MONITOR_MODE_PROPERTIES_ALL);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, NUM_MONITOR_MODE_PROPERTIES_ALL, prop);
    
    // CGDisplayModeRelease(mode); // 10.6 on CGDisplayModeRef
    [pool release];

    return properties;
}

/*
 * Class:     jogamp_newt_driver_ios_ScreenDriver
 * Method:    setMonitorMode0
 * Signature: (III)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_ios_ScreenDriver_setMonitorMode0
  (JNIEnv *env, jobject object, jint crt_id, jint nativeId, jint ccwRot)
{
    return false;
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_ios_WindowDriver_initIDs0
  (JNIEnv *env, jclass clazz)
{
    static int initialized = 0;

    if(initialized) return JNI_TRUE;
    initialized = 1;

    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NewtScreen_dump();

    jclass c;
    c = (*env)->FindClass(env, ClazzNamePoint);
    if(NULL==c) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_ios_WindowDriver_initIDs0: can't find %s", ClazzNamePoint);
    }
    pointClz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==pointClz) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_ios_WindowDriver_initIDs0: can't use %s", ClazzNamePoint);
    }
    pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    if(NULL==pointCstr) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_ios_WindowDriver_initIDs0: can't fetch %s.%s %s",
            ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    }

    // Need this when debugging, as it is necessary to attach gdb to
    // the running java process -- "gdb java" doesn't work
    //    printf("Going to sleep for 10 seconds\n");
    //    sleep(10);

    BOOL res =  [NewtUIWindow initNatives: env forClass: clazz];
    [pool release];

    return (jboolean) res;
}

#ifdef VERBOSE_ON
    #define DBG_PRINT_CREATEWIN1(n) NSLog(@"createWindow1.%d - parent(win %p scale %f, view %p scale %f), window %p (scale %f, superview %p, hidden %d), view: %p (scale %f, superview %p, hidden %d), CAEAGLLayer %p (opaque %d, scale %f, isCAEAGLLayer %d, hidden %d)\n",  \
        (n), parentWindow, (NULL!=parentWindow?[parentWindow contentScaleFactor]:0.0f), parentView, (NULL!=parentView?[parentView contentScaleFactor]:0.0f),  \
        myWindow, [myWindow contentScaleFactor], [myWindow superview], [myWindow isHidden],  \
        myView, [myView contentScaleFactor], [myView superview], [myView isHidden], \
        l, [l isOpaque], [l contentsScale], [l isKindOfClass:[CAEAGLLayer class]], [l isHidden]); fflush(stderr)
#else
    #define DBG_PRINT_CREATEWIN1(n)
#endif

/**
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    createWindow1
 * Signature: (JJIIIIFZZZZJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_ios_WindowDriver_createWindow1
  (JNIEnv *env, jobject jthis, jlong joldwin, jlong jparent, jint x, jint y, jint w, jint h, jfloat reqPixelScale, 
   jboolean fullscreen, jint styleMask, jint bufferingType, 
   jboolean opaque, jboolean atop, jboolean abottom, jboolean visible, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [CATransaction begin];

    NewtUIWindow* oldWindow = (NewtUIWindow*) (intptr_t) joldwin ;
    NewtUIView* myView = (NewtUIView*) (intptr_t) jview ;

    NSObject* nsParentObj = (NSObject*) ((intptr_t) jparent);
    UIWindow* parentWindow = NULL;
    UIView* parentView = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[UIWindow class]] ) {
        parentWindow = (UIWindow*) nsParentObj;
        parentView = (UIView*)nsParentObj;
        DBG_PRINT( "createWindow1 - Parent is UIWindow : %p (win) -> %p (view) \n", parentWindow, parentView);
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[UIView class]] ) {
        parentView = (UIView*) nsParentObj;
        parentWindow = [parentView window];
        DBG_PRINT( "createWindow1 - Parent is UIView : %p -(view) > %p (win) \n", parentView, parentWindow);
    } else {
        DBG_PRINT( "createWindow1 - Parent is neither UIWindow nor UIView : %p\n", nsParentObj);
    }
    DBG_PRINT( "createWindow1.0 - %p (this), oldWin %p, parent(win %p, view %p), oldView %p, %d/%d %dx%d, reqPixelScale %f, fullscreen %d, opaque %d, atop %d, abottom %d, visible %d (START)\n",
        (void*)(intptr_t)jthis, oldWindow, parentWindow, parentView, myView, (int)x, (int)y, (int)w, (int)h, (float)reqPixelScale,
        fullscreen, opaque, atop, abottom, visible);

    if( NULL != oldWindow ) {
        if( NULL == myView ) {
            NewtCommon_throwNewRuntimeException(env, "oldWindow %p given but no view %p (this %p)", oldWindow, myView, (void*)(intptr_t)jthis);
        }
        changeContentView(env, jthis, parentView, oldWindow, NULL, NO);
        Java_jogamp_newt_driver_ios_WindowDriver_close0(env, NULL, joldwin);
    } else if( NULL != myView ) {
        NewtCommon_throwNewRuntimeException(env, "view %p given but no oldWindow %p (this %p)", oldWindow, myView, (void*)(intptr_t)jthis);
    }
    CGRect rectWin = CGRectMake(x, y, w, h);
    NewtUIWindow* myWindow = [[[[NewtUIWindow alloc] initWithFrame: rectWin
                                               styleMask: (NSUInteger) styleMask
                                               backing: 0 // TODO (NSBackingStoreType) bufferingType
                                               defer: YES
                                               isFullscreenWindow: fullscreen] autorelease] retain];
    if(opaque) {
        [myWindow setOpaque: YES];
#ifdef VERBOSE_ON
        [myWindow setBackgroundColor: [UIColor redColor]];
#endif
    } else {
        [myWindow setOpaque: NO];
        [myWindow setBackgroundColor: [UIColor clearColor]];
    }
    if( visible ) {
        // Only if calling this before adding the view, the view receives touch events.
        // Another 'funny' iOS API nightmare?
        [myWindow makeKeyAndVisible];
    }
    DBG_PRINT( "createWindow1.1 - window %p, isHidden %d, rootViewController %p\n", myWindow, [myWindow isHidden], myWindow.rootViewController);

    if( NULL == myView ) {
        CGRect rectView = CGRectMake(0, 0, w, h);
        myView = [[NewtUIView alloc] initWithFrame: rectView] ;
    }
    CAEAGLLayer* l = (CAEAGLLayer*)[myView layer];
    if(opaque) {
        [myView setOpaque: YES];
        [l setOpaque: YES];
    } else {
        [myView setOpaque: NO];
        [myView setBackgroundColor: [UIColor clearColor]];
        [l setOpaque: NO];
        [l setBackgroundColor: [UIColor clearColor]];
    }
    DBG_PRINT_CREATEWIN1(2);

    changeContentView(env, jthis, parentView, myWindow, myView, NO);
    DBG_PRINT_CREATEWIN1(3);

    if(NULL!=parentWindow) {
        [myWindow attachToParent: parentWindow];
    }
    DBG_PRINT_CREATEWIN1(4);

    // Immediately re-position this window based on an upper-left coordinate system
    setWindowClientTopLeftPointAndSize(myWindow, x, y, w, h, NO);
    // SEE comment above at makeKeyAndVisible call
    // if( visible ) {
    //     [myWindow makeKeyAndVisible];
    // }
    DBG_PRINT_CREATEWIN1(5);

    [myView setDestroyNotifySent: false];
    setJavaWindowObject(env, jthis, myView);
    DBG_PRINT_CREATEWIN1(6);

    [myWindow setPixelScale: (CGFloat)reqPixelScale defer:NO];
    DBG_PRINT_CREATEWIN1(88);

    [CATransaction commit];
    [pool release];

    return (jlong) (intptr_t) myWindow;
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_ios_WindowDriver_getDisplayID0(JNIEnv *env, jobject jthis, jlong window) {
    NewtUIWindow* myWindow = (NewtUIWindow*) ((intptr_t) window);
    if( NULL == myWindow ) {
        DBG_PRINT( "getDisplayID0 - NULL NEWT win - abort\n");
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    // UIScreen *screen =  [myWindow screen];
    int32_t displayID = 0; // TODO (int32_t)NewtScreen_getCGDirectDisplayIDByUIScreen(screen);
    [pool release];
    return (jint) displayID;
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    setPixelScale0
 * Signature: (JJF)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_setPixelScale0
  (JNIEnv *env, jobject jthis, jlong window, jlong view, jfloat reqPixelScale)
{
    NewtUIWindow* myWindow = (NewtUIWindow*) ((intptr_t) window);
    if( NULL == myWindow ) {
        DBG_PRINT( "setPixelScale0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [myWindow setPixelScale: (CGFloat)reqPixelScale defer:YES];

    [pool release];
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NewtUIWindow* mWin = (NewtUIWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "windowClose.0 - NULL NEWT win - abort\n");
        return;
    }
    BOOL isNSWin = [mWin isKindOfClass:[UIWindow class]];
    BOOL isNewtWin = [mWin isKindOfClass:[NewtUIWindow class]];
    UIWindow *pWin;
    {
        UIView *superview = [mWin superview];
        if( [superview isKindOfClass:[UIWindow class]] ) {
            pWin = (UIWindow*)superview;
        } else {
            pWin = NULL;
        }
    }
    DBG_PRINT( "windowClose.0 - %p [isUIWindow %d, isNewtWin %d], parent %p\n", mWin, isNSWin, isNewtWin, pWin);
    (void)isNSWin; // silence
    if( !isNewtWin ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtUIWindow %p", mWin);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtUIView* mView = [mWin getContentNewtUIView];
    BOOL fullscreen = mWin->isFullscreenWindow;
    BOOL destroyNotifySent, isUIView, isNewtUIView;
    if( NULL != mView ) {
        isUIView = [mView isKindOfClass:[UIView class]];
        isNewtUIView = [mView isKindOfClass:[NewtUIView class]];
        destroyNotifySent = isNewtUIView ? [mView getDestroyNotifySent] : false;
    } else {
        isUIView = false;
        isNewtUIView = false;
        destroyNotifySent = false;
    }

    DBG_PRINT( "windowClose.0 - %p, destroyNotifySent %d, view %p [isUIView %d, isNewtUIView %d], fullscreen %d, parent %p\n", 
        mWin, destroyNotifySent, mView, isUIView, isNewtUIView, (int)fullscreen, pWin);

    [mWin setRealized: NO];

    if( isNewtUIView ) {
        // cleanup view
        [mView setDestroyNotifySent: true];
        setJavaWindowObject(env, NULL, mView);
    }

    // Note: mWin's release will also release it's mView!
    DBG_PRINT( "windowClose.1a - %p view %p, fullscreen %d\n", 
        mWin, mView, (int)fullscreen);

    if(NULL!=pWin) {
        [mWin detachFromParent: pWin];
    }
    // TODO [mWin orderOut: mWin];
    [mWin setHidden: YES]; // no release, close n/a, .. well: ref count counts :-(

    DBG_PRINT( "windowClose.2 - %p view %p, parent %p\n", mWin, mView, pWin);

    [mWin release];

    DBG_PRINT( "windowClose.Xp\n");

    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_ios_WindowDriver
 * Method:    lockSurface0
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_ios_WindowDriver_lockSurface0
  (JNIEnv *env, jclass clazz, jlong window, jlong view)
{
    NewtUIWindow *mWin = (NewtUIWindow*) ((intptr_t) window);
    if(NO == [mWin isRealized]) {
        return JNI_FALSE;
    }
    NewtUIView * mView = (NewtUIView *) ((intptr_t) view);
    return [mView softLock] == YES ? JNI_TRUE : JNI_FALSE;
    /** deadlocks, since we render independent of focus
    return [mView lockFocusIfCanDraw] == YES ? JNI_TRUE : JNI_FALSE; */
}

/*
 * Class:     Java_jogamp_newt_driver_ios_WindowDriver
 * Method:    unlockSurface0
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_ios_WindowDriver_unlockSurface0
  (JNIEnv *env, jclass clazz, jlong window, jlong view)
{
    // NewtUIWindow *mWin = (NewtUIWindow*) ((intptr_t) window);
    (void) window;
    NewtUIView * mView = (NewtUIView *) ((intptr_t) view);
    return [mView softUnlock] == YES ? JNI_TRUE : JNI_FALSE;
    /** deadlocks, since we render independent of focus
    [mView unlockFocus]; */
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    requestFocus0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_requestFocus0
  (JNIEnv *env, jobject window, jlong w, jboolean force)
{
    UIWindow* mWin = (UIWindow*) ((intptr_t) w);
    if( NULL == mWin ) {
        DBG_PRINT( "requestFocus - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#ifdef VERBOSE_ON
    BOOL hasFocus = [mWin isKeyWindow];
#endif
    DBG_PRINT( "requestFocus - window: %p, force %d, hasFocus %d (START)\n", mWin, force, hasFocus);

    // TODO [mWin setAcceptsMouseMovedEvents: YES];
    // TODO [mWin makeFirstResponder: nil];
    // TODO [mWin orderFrontRegardless];
    [mWin makeKeyWindow];

    DBG_PRINT( "requestFocus - window: %p, force %d (END)\n", mWin, force);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    resignFocus0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_resignFocus0
  (JNIEnv *env, jobject window, jlong w)
{
    UIWindow* mWin = (UIWindow*) ((intptr_t) w);
    if( NULL == mWin ) {
        DBG_PRINT( "resignFocus0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIWindow* pWin = NULL; // TODO [mWin parentWindow];
    BOOL hasFocus = [mWin isKeyWindow];

    DBG_PRINT( "resignFocus0 - window: %p, parent %p, hasFocus %d (START)\n", mWin, pWin, hasFocus );
    if( hasFocus ) {
        if(NULL != pWin) {
            // [mWin makeFirstResponder: pWin];
            [pWin makeKeyWindow];
        } else {
            [pWin resignKeyWindow];
        }
    }
    DBG_PRINT( "resignFocus0 - window: %p (END)\n", mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    orderFront0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_orderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    UIWindow* mWin = (UIWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "orderFront0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIWindow* pWin = NULL; // TODO [mWin parentWindow];

    DBG_PRINT( "orderFront0 - window: (parent %p) %p hidden %d (START)\n", pWin, mWin, [mWin isHidden]);

    if( NULL == pWin ) {
        // TODO [mWin orderFrontRegardless];
    } else {
        // TODO [mWin orderWindow: UIWindowAbove relativeTo: [pWin windowNumber]];
    }

    DBG_PRINT( "orderFront0 - window: (parent %p) %p (END)\n", pWin, mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    orderOut
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_orderOut0
  (JNIEnv *env, jobject unused, jlong window)
{
    UIWindow* mWin = (UIWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "orderOut0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    UIWindow* pWin = NULL; // TODO [mWin parentWindow];

    DBG_PRINT( "orderOut0 - window: (parent %p) %p hidden %d (START)\n", pWin, mWin, [mWin isHidden]);

    if( NULL == pWin ) {
        // TODO [mWin orderOut: mWin];
    } else {
        // TODO [mWin orderWindow: UIWindowOut relativeTo: [pWin windowNumber]];
    }

    DBG_PRINT( "orderOut0 - window: (parent %p) %p (END)\n", pWin, mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    setTitle0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_setTitle0
  (JNIEnv *env, jobject unused, jlong window, jstring title)
{
    UIWindow* win = (UIWindow*) ((intptr_t) window);
    if( NULL == win ) {
        DBG_PRINT( "setTitle0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT( "setTitle0 - window: %p (START)\n", win);

    NSString* str = jstringToNSString(env, title);
    [str autorelease];
    // TODO [win setTitle: str];

    DBG_PRINT( "setTitle0 - window: %p (END)\n", win);

    [pool release];
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    changeContentView
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_changeContentView0
  (JNIEnv *env, jobject jthis, jlong parentWindowOrView, jlong window, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NewtUIView* newView = (NewtUIView *) ((intptr_t) jview);
    NewtUIWindow* win = (NewtUIWindow*) ((intptr_t) window);

    DBG_PRINT( "changeContentView0.0 -  win %p, view (%p,%d)\n", 
        win, newView, getRetainCount(newView));

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parentWindowOrView);
    UIView* pView = NULL;
    if( NULL != nsParentObj ) {
        if( [nsParentObj isKindOfClass:[UIWindow class]] ) {
            UIWindow * pWin = (UIWindow*) nsParentObj;
            pView = (UIView*)pWin;
        } else if( [nsParentObj isKindOfClass:[UIView class]] ) {
            pView = (UIView*) nsParentObj;
        }
    }

    changeContentView(env, jthis, pView, win, newView, YES);

    DBG_PRINT( "changeContentView0.X\n");

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    updateSizePosInsets0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_updateSizePosInsets0
  (JNIEnv *env, jobject jthis, jlong window, jboolean defer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtUIWindow* mWin = (NewtUIWindow*) ((intptr_t) window);

    DBG_PRINT( "updateSizePosInsets - window: %p, defer %d (START)\n", mWin, (int)defer);

    [mWin updateSizePosInsets: env jwin:jthis defer:defer];

    DBG_PRINT( "updateSizePosInsets - window: %p, defer %d (END)\n", mWin, (int)defer);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    setWindowClientTopLeftPointAndSize0
 * Signature: (JIIIIZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_setWindowClientTopLeftPointAndSize0
  (JNIEnv *env, jobject unused, jlong window, jint x, jint y, jint w, jint h, jboolean display)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtUIWindow* mWin = (NewtUIWindow*) ((intptr_t) window);

    DBG_PRINT( "setWindowClientTopLeftPointAndSize - window: %p (START)\n", mWin);

    setWindowClientTopLeftPointAndSize(mWin, x, y, w, h, display);

    DBG_PRINT( "setWindowClientTopLeftPointAndSize - window: %p (END)\n", mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    setWindowClientTopLeftPoint0
 * Signature: (JIIZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_ios_WindowDriver_setWindowClientTopLeftPoint0
  (JNIEnv *env, jobject unused, jlong window, jint x, jint y, jboolean display)
{
    NewtUIWindow* mWin = (NewtUIWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "setWindowClientTopLeftPoint - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT( "setWindowClientTopLeftPoint - window: %p (START)\n", mWin);

    setWindowClientTopLeftPoint(mWin, x, y, display);

    DBG_PRINT( "setWindowClientTopLeftPoint - window: %p (END)\n", mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_ios_WindowDriver
 * Method:    getLocationOnScreen0
 * Signature: (JII)Lcom/jogamp/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_newt_driver_ios_WindowDriver_getLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong win, jint src_x, jint src_y)
{
    NewtUIWindow *mWin = (NewtUIWindow*) (intptr_t) win;
    if( NULL == mWin ) {
        DBG_PRINT( "getLocationOnScreen0 - NULL NEWT win - abort\n");
        return NULL;
    }
    if( ![mWin isKindOfClass:[NewtUIWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtUIWindow %p", mWin);
        return NULL;
    }
    CGPoint p0 = [mWin getLocationOnScreen: CGPointMake(src_x, src_y)];
    return (*env)->NewObject(env, pointClz, pointCstr, (jint)p0.x, (jint)p0.y);
}

