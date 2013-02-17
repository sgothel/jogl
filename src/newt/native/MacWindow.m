/*
 * Copyright (c) 2009 Sun Microsystems, Inc. All Rights Reserved.
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

#import <inttypes.h>

#import "jogamp_newt_driver_macosx_WindowDriver.h"
#import "NewtMacWindow.h"

#import "MouseEvent.h"
#import "KeyEvent.h"
#import "ScreenMode.h"

#import <ApplicationServices/ApplicationServices.h>

#import <stdio.h>

#ifdef DBG_PERF
    #include "timespec.h"
#endif

static const char * const ClazzNamePoint = "javax/media/nativewindow/util/Point";
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

static void setFrameTopLeftPoint(NSWindow* pWin, NewtMacWindow* mWin, jint x, jint y) {
    NSPoint pS = [mWin newtScreenWinPos2OSXScreenPos: NSMakePoint(x, y)];
    [mWin setFrameOrigin: pS];

    NSView* mView = [mWin contentView];
    [mWin invalidateCursorRectsForView: mView];
}

#ifdef VERBOSE_ON
static int getRetainCount(NSObject * obj) {
    return ( NULL == obj ) ? -1 : (int)([obj retainCount]) ;
}
#endif

static void changeContentView(JNIEnv *env, jobject javaWindowObject, NSView *pview, NewtMacWindow *win, NewtView *newView) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSView* oldNSView = [win contentView];
    NewtView* oldView = NULL;
#ifdef VERBOSE_ON
    int dbgIdx = 1;
#endif

    if( [oldNSView isMemberOfClass:[NewtView class]] ) {
        oldView = (NewtView *) oldNSView;
    }

    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d (%d) -> %p,%d), parent view %p\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), NULL!=oldView, newView, getRetainCount(newView), pview);

    if(NULL!=oldNSView) {
NS_DURING
        // Available >= 10.5 - Makes the menubar disapear
        BOOL iifs;
        if ( [oldNSView respondsToSelector:@selector(isInFullScreenMode)] ) {
            iifs = [oldNSView isInFullScreenMode];
        } else {
            iifs = NO;
        }
        if(iifs && [oldNSView respondsToSelector:@selector(exitFullScreenModeWithOptions:)] ) {
            [oldNSView exitFullScreenModeWithOptions: NULL];
        }
NS_HANDLER
NS_ENDHANDLER
        DBG_PRINT( "changeContentView.%d win %p, view (%p,%d (%d) -> %p,%d)\n", 
            dbgIdx++, win, oldNSView, getRetainCount(oldNSView), NULL!=oldView, newView, getRetainCount(newView));

        if( NULL != oldView ) {
            jobject globJavaWindowObject = [oldView getJavaWindowObject];
            (*env)->DeleteGlobalRef(env, globJavaWindowObject);
            [oldView setJavaWindowObject: NULL];
            [oldView setDestroyNotifySent: false];
        }
        [oldNSView removeFromSuperviewWithoutNeedingDisplay];
    }
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d, isHiddenOrHasHiddenAncestor: %d\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView), [newView isHidden], [newView isHiddenOrHasHiddenAncestor]);

    if(NULL!=newView) {
        jobject globJavaWindowObject = (*env)->NewGlobalRef(env, javaWindowObject);
        [newView setJavaWindowObject: globJavaWindowObject];
        [newView setDestroyNotifySent: false];
        {
            JavaVM *jvmHandle = NULL;
            int jvmVersion = 0;

            if(0 != (*env)->GetJavaVM(env, &jvmHandle)) {
                jvmHandle = NULL;
            } else {
                jvmVersion = (*env)->GetVersion(env);
            }
            [newView setJVMHandle: jvmHandle];
            [newView setJVMVersion: jvmVersion];
        }

        DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d)\n", 
            dbgIdx++, win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView));

        if(NULL!=pview) {
            [pview addSubview: newView positioned: NSWindowAbove relativeTo: nil];
        }
    }
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d, isHiddenOrHasHiddenAncestor: %d\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView), [newView isHidden], [newView isHiddenOrHasHiddenAncestor]);

    [win setContentView: newView];

    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d, isHiddenOrHasHiddenAncestor: %d\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView), [newView isHidden], [newView isHiddenOrHasHiddenAncestor]);

    // make sure the insets are updated in the java object
    [win updateInsets: env];

    DBG_PRINT( "changeContentView.X win %p, view (%p,%d -> %p,%d)\n", 
        win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView));

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_DisplayDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_initNSApplication0
  (JNIEnv *env, jclass clazz)
{
    static int initialized = 0;

    if(initialized) return JNI_TRUE;
    initialized = 1;

    // This little bit of magic is needed in order to receive mouse
    // motion events and allow key focus to be properly transferred.
    // FIXME: are these Carbon APIs? They come from the
    // ApplicationServices.framework.
    ProcessSerialNumber psn;
    if (GetCurrentProcess(&psn) == noErr) {
        TransformProcessType(&psn, kProcessTransformToForegroundApplication);
        SetFrontProcess(&psn);
    }

    // Initialize the shared NSApplication instance
    [NSApplication sharedApplication];

    // Need this when debugging, as it is necessary to attach gdb to
    // the running java process -- "gdb java" doesn't work
    //    printf("Going to sleep for 10 seconds\n");
    //    sleep(10);

    return (jboolean) JNI_TRUE;
}

/*
 * Class:     jogamp_newt_driver_macosx_DisplayDriver
 * Method:    runNSApplication0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_runNSApplication0
  (JNIEnv *env, jclass clazz)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    DBG_PRINT( "\nrunNSApplication0.0\n");

    [NSApp run];

    DBG_PRINT( "\nrunNSApplication0.X\n");
    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_DisplayDriver
 * Method:    stopNSApplication0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_stopNSApplication0
  (JNIEnv *env, jclass clazz)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    DBG_PRINT( "\nstopNSApplication0.0 nsApp.running %d\n", (NSApp && [NSApp isRunning]));

    if(NSApp && [NSApp isRunning]) {
        [NSApp performSelectorOnMainThread:@selector(stop:) withObject:nil waitUntilDone:YES];
        // [NSApp stop: nil];
        NSEvent* event = [NSEvent otherEventWithType: NSApplicationDefined
                                            location: NSMakePoint(0,0)
                                       modifierFlags: 0
                                           timestamp: 0.0
                                        windowNumber: 0
                                             context: nil
                                             subtype: 0
                                               data1: 0
                                               data2: 0];
        DBG_PRINT( "\nstopNSApplication0.1\n");
        [NSApp postEvent: event atStart: true];
    }
    /**
    DBG_PRINT( "\nstopNSApplication0.2\n");
    if(NSApp && [NSApp isRunning]) {
        DBG_PRINT( "\nstopNSApplication0.3\n");
        [NSApp terminate:nil];
    } */

    DBG_PRINT( "\nstopNSApplication0.X\n");
    [pool release];
}

static NSScreen * NewtScreen_getNSScreenByIndex(int screen_idx) {
    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    return (NSScreen *) [screens objectAtIndex: screen_idx];
}

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getWidthImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getWidthImpl0
  (JNIEnv *env, jclass clazz, jint screen_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)screen_idx);
    NSRect rect = [screen frame];

    [pool release];

    return (jint) (rect.size.width);
}

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getHeightImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getHeightImpl0
  (JNIEnv *env, jclass clazz, jint screen_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)screen_idx);
    NSRect rect = [screen frame];

    [pool release];

    return (jint) (rect.size.height);
}

static CGDirectDisplayID NewtScreen_getCGDirectDisplayIDByNSScreen(NSScreen *screen) {
    // Mind: typedef uint32_t CGDirectDisplayID; - however, we assume it's 64bit on 64bit ?!
    NSDictionary * dict = [screen deviceDescription];
    NSNumber * val = (NSNumber *) [dict objectForKey: @"NSScreenNumber"];
    // [NSNumber integerValue] returns NSInteger which is 32 or 64 bit native size
    return (CGDirectDisplayID) [val integerValue];
}

/**
 * Only in >= 10.6:
 *   CGDisplayModeGetWidth(mode)
 *   CGDisplayModeGetRefreshRate(mode)
 *   CGDisplayModeGetHeight(mode)
 */
static long GetDictionaryLong(CFDictionaryRef theDict, const void* key) 
{
    long value = 0;
    CFNumberRef numRef;
    numRef = (CFNumberRef)CFDictionaryGetValue(theDict, key); 
    if (numRef != NULL)
        CFNumberGetValue(numRef, kCFNumberLongType, &value);    
    return value;
}
#define CGDDGetModeWidth(mode) GetDictionaryLong((mode), kCGDisplayWidth)
#define CGDDGetModeHeight(mode) GetDictionaryLong((mode), kCGDisplayHeight)
#define CGDDGetModeRefreshRate(mode) GetDictionaryLong((mode), kCGDisplayRefreshRate)
#define CGDDGetModeBitsPerPixel(mode) GetDictionaryLong((mode), kCGDisplayBitsPerPixel)

// Duplicate each Mode by all possible rotations (4):
// For each real-mode: [mode, 0], [mode, 90], [mode, 180], [mode, 270]
#define ROTMODES_PER_REALMODE 4

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getScreenSizeMM0
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getScreenSizeMM0
  (JNIEnv *env, jobject obj, jint scrn_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

#ifdef DBG_PERF
    struct timespec t0, t1, td;
    long td_ms;
    timespec_now(&t0);
#endif

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)scrn_idx);
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getScreenSizeMM0.1: %ld ms\n", td_ms); fflush(NULL);
#endif

    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getScreenSizeMM0.2: %ld ms\n", td_ms); fflush(NULL);
#endif

    CGSize screenDim = CGDisplayScreenSize(display);
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getScreenSizeMM0.3: %ld ms\n", td_ms); fflush(NULL);
#endif

    jint prop[ 2 ];
    prop[0] = (jint) screenDim.width;
    prop[1] = (jint) screenDim.height;

    jintArray properties = (*env)->NewIntArray(env, 2);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size 2");
    }
    (*env)->SetIntArrayRegion(env, properties, 0, 2, prop);
    
    [pool release];

    return properties;
}

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getScreenMode0
 * Signature: (IIII)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getScreenMode0
  (JNIEnv *env, jobject obj, jint scrn_idx, jint mode_idx, jint widthMM, jint heightMM)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    int prop_num = NUM_SCREEN_MODE_PROPERTIES_ALL;
    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)scrn_idx);
    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);

    CFArrayRef availableModes = CGDisplayAvailableModes(display);
    CFIndex numberOfAvailableModes = CFArrayGetCount(availableModes);
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes; 
    CFDictionaryRef mode = NULL;
    int currentCCWRot = (int)CGDisplayRotation(display);
    jint ccwRot = 0;

#ifdef VERBOSE_ON
    if(0 >= mode_idx) {
        // only for current mode (-1) and first mode (scanning)
        DBG_PRINT( "getScreenMode0: scrn %d (%p, %p), mode %d, avail: %d/%d, current rot %d ccw\n",  
            (int)scrn_idx, screen, (void*)(intptr_t)display, (int)mode_idx, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots, currentCCWRot);
    }
#endif

    if(numberOfAvailableModesRots<=mode_idx) {
        // n/a - end of modes
        DBG_PRINT( "getScreenMode0: end of modes: mode %d, avail: %d/%d\n",
            (int)mode_idx, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots);
        [pool release];
        return (*env)->NewIntArray(env, 0);
    } else if(-1 < mode_idx) {
        // only at initialization time, where index >= 0
        prop_num++; // add 1st extra prop, mode_idx
        mode = (CFDictionaryRef)CFArrayGetValueAtIndex(availableModes, mode_idx / ROTMODES_PER_REALMODE);
        ccwRot = mode_idx % ROTMODES_PER_REALMODE * 90;
    } else {
        // current mode
        mode = CGDisplayCurrentMode(display);
        ccwRot = currentCCWRot;
    }
    // mode = CGDisplayModeRetain(mode); // 10.6 on CGDisplayModeRef

    int mWidth = CGDDGetModeWidth(mode);
    int mHeight = CGDDGetModeHeight(mode);

    // swap width and height, since OSX reflects rotated dimension, we don't
    if ( 90 == currentCCWRot || 270 == currentCCWRot ) {
        int tempWidth = mWidth;
        mWidth = mHeight;
        mHeight = tempWidth;
    }

    jint prop[ prop_num ];
    int propIndex = 0;
    int propIndexRes = 0;

    if( -1 < mode_idx ) {
        prop[propIndex++] = mode_idx;
    }
    int refreshRate = CGDDGetModeRefreshRate(mode);
    int fRefreshRate = ( 0 < refreshRate ) ? refreshRate : 60; // default .. (experienced on OSX 10.6.8)
    prop[propIndex++] = 0; // set later for verification of iterator
    propIndexRes = propIndex;
    prop[propIndex++] = mWidth;
    prop[propIndex++] = mHeight;
    prop[propIndex++] = CGDDGetModeBitsPerPixel(mode);
    prop[propIndex++] = widthMM;
    prop[propIndex++] = heightMM;
    prop[propIndex++] = fRefreshRate;
    prop[propIndex++] = ccwRot;
    prop[propIndex - NUM_SCREEN_MODE_PROPERTIES_ALL] = ( -1 < mode_idx ) ? propIndex-1 : propIndex ; // count == NUM_SCREEN_MODE_PROPERTIES_ALL

    DBG_PRINT( "getScreenMode0: Mode %d/%d (%d): %dx%d, %d bpp, %dx%d mm, %d / %d Hz, rot %d ccw\n",
        (int)mode_idx, (int)numberOfAvailableModesRots, (int)numberOfAvailableModes, 
        (int)prop[propIndexRes+0], (int)prop[propIndexRes+1], (int)prop[propIndexRes+2], 
        (int)prop[propIndexRes+3], (int)prop[propIndexRes+4], (int)prop[propIndexRes+5], refreshRate, (int)prop[propIndexRes+6]);

    jintArray properties = (*env)->NewIntArray(env, prop_num);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", prop_num);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, prop_num, prop);
    
    // CGDisplayModeRelease(mode); // 10.6 on CGDisplayModeRef
    [pool release];

    return properties;
}

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    setScreenMode0
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_setScreenMode0
  (JNIEnv *env, jobject object, jint scrn_idx, jint mode_idx)
{
    jboolean res = JNI_TRUE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)scrn_idx);
    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);

    CFArrayRef availableModes = CGDisplayAvailableModes(display);
#ifdef VERBOSE_ON
    CFIndex numberOfAvailableModes = CFArrayGetCount(availableModes);
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes;
#endif
    CFDictionaryRef mode = (CFDictionaryRef)CFArrayGetValueAtIndex(availableModes, mode_idx / ROTMODES_PER_REALMODE);
    // mode = CGDisplayModeRetain(mode); // 10.6 on CGDisplayModeRef

    int ccwRot = mode_idx % ROTMODES_PER_REALMODE * 90;
    DBG_PRINT( "setScreenMode0: scrn %d (%p, %p), mode %d, rot %d ccw, avail: %d/%d\n",  
        (int)scrn_idx, screen, (void*)(intptr_t)display, (int)mode_idx, ccwRot, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots);

    if(ccwRot!=0) {
        // FIXME: How to rotate the display/screen on OSX programmatically ?
        DBG_PRINT( "setScreenMode0: Don't know how to rotate screen on OS X: rot %d ccw\n", ccwRot);
        res = JNI_FALSE;
    }
    if(JNI_TRUE == res) {
        CGError err = CGDisplaySwitchToMode(display, mode);
        if(kCGErrorSuccess != err) {
            DBG_PRINT( "setScreenMode0: SetMode failed: %d\n", (int)err);
            res = JNI_FALSE;
        }
    }

    // CGDisplayModeRelease(mode); // 10.6 on CGDisplayModeRef
    [pool release];

    return res;
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_initIDs0
  (JNIEnv *env, jclass clazz)
{
    static int initialized = 0;

    if(initialized) return JNI_TRUE;
    initialized = 1;

    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    jclass c;
    c = (*env)->FindClass(env, ClazzNamePoint);
    if(NULL==c) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_WindowDriver_initIDs0: can't find %s", ClazzNamePoint);
    }
    pointClz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==pointClz) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_WindowDriver_initIDs0: can't use %s", ClazzNamePoint);
    }
    pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    if(NULL==pointCstr) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_WindowDriver_initIDs0: can't fetch %s.%s %s",
            ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    }

    // Need this when debugging, as it is necessary to attach gdb to
    // the running java process -- "gdb java" doesn't work
    //    printf("Going to sleep for 10 seconds\n");
    //    sleep(10);

    BOOL res =  [NewtMacWindow initNatives: env forClass: clazz];
    [pool release];

    return (jboolean) res;
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    createWindow0
 * Signature: (JIIIIZIIIJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_createWindow0
  (JNIEnv *env, jobject jthis, jlong parent, jint x, jint y, jint w, jint h, jboolean opaque, jboolean fullscreen, jint styleMask, 
   jint bufferingType, jint screen_idx, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtView* myView = (NewtView*) (intptr_t) jview ;

    DBG_PRINT( "createWindow0 - %p (this), %p (parent), %d/%d %dx%d, opaque %d, fs %d, style %X, buffType %X, screenidx %d, view %p (START)\n",
        (void*)(intptr_t)jthis, (void*)(intptr_t)parent, (int)x, (int)y, (int)w, (int)h, (int) opaque, (int)fullscreen, 
        (int)styleMask, (int)bufferingType, (int)screen_idx, myView);

    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    NSScreen *myScreen = (NSScreen *) [screens objectAtIndex: screen_idx];
    NSRect rect;

    if (fullscreen) {
        styleMask = NSBorderlessWindowMask;
        rect = [myScreen frame];
        x = 0;
        y = 0;
        w = (jint) (rect.size.width);
        h = (jint) (rect.size.height);
    } else {
        rect = NSMakeRect(x, y, w, h);
    }

    // Allocate the window
    NewtMacWindow* myWindow = [[NewtMacWindow alloc] initWithContentRect: rect
                                               styleMask: (NSUInteger) styleMask
                                               backing: (NSBackingStoreType) bufferingType
                                               defer: NO
                                               screen: myScreen
                                               isFullscreenWindow: fullscreen];
    [myWindow setReleasedWhenClosed: YES]; // default
    [myWindow setPreservesContentDuringLiveResize: NO];

    NSObject* nsParentObj = (NSObject*) ((intptr_t) parent);
    NSWindow* parentWindow = NULL;
    NSView* parentView = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSWindow class]] ) {
        parentWindow = (NSWindow*) nsParentObj;
        parentView = [parentWindow contentView];
        DBG_PRINT( "createWindow0 - Parent is NSWindow : %p (win) -> %p (view) \n", parentWindow, parentView);
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSView class]] ) {
        parentView = (NSView*) nsParentObj;
        parentWindow = [parentView window];
        DBG_PRINT( "createWindow0 - Parent is NSView : %p -(view) > %p (win) \n", parentView, parentWindow);
    } else {
        DBG_PRINT( "createWindow0 - Parent is neither NSWindow nor NSView : %p\n", nsParentObj);
    }
    DBG_PRINT( "createWindow0 - is visible.1: %d\n", [myWindow isVisible]);

    // Remove animations for child windows
    if(NULL != parentWindow) {
NS_DURING
        if ( [myWindow respondsToSelector:@selector(setAnimationBehavior:)] ) {
            // Available >= 10.7 - Removes default animations
            [myWindow setAnimationBehavior: NSWindowAnimationBehaviorNone];
        }
NS_HANDLER
NS_ENDHANDLER
    }

#ifdef VERBOSE_ON
    int dbgIdx = 1;
#endif
    if(opaque) {
        [myWindow setOpaque: YES];
        DBG_PRINT( "createWindow0.%d\n", dbgIdx++);
        if (!fullscreen) {
            [myWindow setShowsResizeIndicator: YES];
        }
        DBG_PRINT( "createWindow0.%d\n", dbgIdx++);
    } else {
        [myWindow setOpaque: NO];
        [myWindow setBackgroundColor: [NSColor clearColor]];
    }

    // specify we want mouse-moved events
    [myWindow setAcceptsMouseMovedEvents:YES];
    DBG_PRINT( "createWindow0.%d\n", dbgIdx++);

    // Use given NewtView or allocate an NewtView if NULL
    if(NULL == myView) {
        myView = [[NewtView alloc] initWithFrame: rect] ;
        DBG_PRINT( "createWindow0.%d - use new view: %p,%d\n", dbgIdx++, myView, getRetainCount(myView));
    } else {
        DBG_PRINT( "createWindow0.%d - use given view: %p,%d\n", dbgIdx++, myView, getRetainCount(myView));
    }

    DBG_PRINT( "createWindow0.%d - %p,%d view %p,%d, isVisible %d\n", 
        dbgIdx++, myWindow, getRetainCount(myWindow), myView, getRetainCount(myView), [myWindow isVisible]);

    // Set the content view
    changeContentView(env, jthis, parentView, myWindow, myView);

    DBG_PRINT( "createWindow0.%d - %p,%d view %p,%d, isVisible %d\n", 
        dbgIdx++, myWindow, getRetainCount(myWindow), myView, getRetainCount(myView), [myWindow isVisible]);

    if(NULL!=parentWindow) {
        [myWindow attachToParent: parentWindow];
    }

    // Immediately re-position the window based on an upper-left coordinate system
    setFrameTopLeftPoint(parentWindow, myWindow, x, y);

    // force surface creation
    [myView lockFocus];
    [myView unlockFocus];

NS_DURING
    // concurrent view rendering
    // Available >= 10.6 - Makes the menubar disapear
    if ( [myWindow respondsToSelector:@selector(setAllowsConcurrentViewDrawing:)] ) {
        [myWindow setAllowsConcurrentViewDrawing: YES];
    }
    if ( [myView respondsToSelector:@selector(setCanDrawConcurrently:)] ) {
        [myView setCanDrawConcurrently: YES];
    }
NS_HANDLER
NS_ENDHANDLER

    // visible on front
    [myWindow orderFront: myWindow];

NS_DURING
    // Available >= 10.5 - Makes the menubar disapear
    if(fullscreen) {
        if ( [myView respondsToSelector:@selector(enterFullScreenMode:withOptions:)] ) {
            [myView enterFullScreenMode: myScreen withOptions:NULL];
        }
    }
NS_HANDLER
NS_ENDHANDLER

    // Set the next responder to be the window so that we can forward
    // right mouse button down events
    [myView setNextResponder: myWindow];

    DBG_PRINT( "createWindow0 - %p (this), %p (parent): new window: %p, view %p,%d (END)\n",
        (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, myView, getRetainCount(myView));

    [pool release];

    return (jlong) ((intptr_t) myWindow);
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);
    NewtView* mView = (NewtView *)[mWin contentView];
    NSWindow* pWin = [mWin parentWindow];
    BOOL destroyNotifySent = (NULL != mView) ? [mView getDestroyNotifySent] : false;

    DBG_PRINT( "windowClose.0 - %p,%d, destroyNotifySent %d, view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), destroyNotifySent, mView, getRetainCount(mView), pWin);

    [mWin setUnrealized];

    if(NULL!=mView) {
        // cleanup view
        jobject javaWindowObject = [mView getJavaWindowObject];
        [mView setDestroyNotifySent: true];
        if(NULL!=javaWindowObject) {
            DBG_PRINT( "windowClose.0: Clear global javaWindowObject reference (%p)\n", javaWindowObject);
            (*env)->DeleteGlobalRef(env, javaWindowObject);
            [mView setJavaWindowObject: NULL];
        }
    }

NS_DURING
    if(NULL!=mView) {
        // Available >= 10.5 - Makes the menubar disapear
        BOOL iifs;
        if ( [mView respondsToSelector:@selector(isInFullScreenMode)] ) {
            iifs = [mView isInFullScreenMode];
        } else {
            iifs = NO;
        }
        if(iifs && [mView respondsToSelector:@selector(exitFullScreenModeWithOptions:)] ) {
            [mView exitFullScreenModeWithOptions: NULL];
        }
        // Note: mWin's release will also release it's mView!
    }
NS_HANDLER
NS_ENDHANDLER

    if(NULL!=pWin) {
        [mWin detachFromParent: pWin];
    }
    // [mWin performSelectorOnMainThread:@selector(orderOut:) withObject:mWin waitUntilDone:NO];
    [mWin orderOut: mWin];

    DBG_PRINT( "windowClose.1 - %p,%d view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), mView, getRetainCount(mView), pWin);

    // Only release window, if release is not yet in process.
    // E.g. destroyNotifySent:=true set by NewtMacWindow::windowWillClose(), i.e. window-close was clicked.
    if(!destroyNotifySent) { 
        // [mWin performSelectorOnMainThread:@selector(release) withObject:nil waitUntilDone:NO];
        [mWin release];
    }

    DBG_PRINT( "windowClose.X - %p,%d, released %d, view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), !destroyNotifySent, mView, getRetainCount(mView), pWin);

    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    lockSurface0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_lockSurface0
  (JNIEnv *env, jclass clazz, jlong window)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    if(NO == [mWin isRealized]) {
        return JNI_FALSE;
    }
    NewtView * mView = (NewtView *) [mWin contentView];
    return [mView softLock] == YES ? JNI_TRUE : JNI_FALSE;
    /** deadlocks, since we render independent of focus
    return [mView lockFocusIfCanDraw] == YES ? JNI_TRUE : JNI_FALSE; */
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    unlockSurface0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_unlockSurface0
  (JNIEnv *env, jclass clazz, jlong window)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    NewtView * mView = (NewtView *) [mWin contentView];
    [mView softUnlock];
    /** deadlocks, since we render independent of focus
    [mView unlockFocus]; */
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    requestFocus0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_requestFocus0
  (JNIEnv *env, jobject window, jlong w, jboolean force)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
#ifdef VERBOSE_ON
    BOOL hasFocus = [mWin isKeyWindow];
#endif

    DBG_PRINT( "requestFocus - window: %p, force %d, hasFocus %d (START)\n", mWin, force, hasFocus);

    [mWin makeFirstResponder: nil];
    [mWin performSelectorOnMainThread:@selector(orderFrontRegardless) withObject:nil waitUntilDone:NO];
    [mWin performSelectorOnMainThread:@selector(makeKeyWindow) withObject:nil waitUntilDone:NO];

    DBG_PRINT( "requestFocus - window: %p, force %d (END)\n", mWin, force);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    resignFocus0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_resignFocus0
  (JNIEnv *env, jobject window, jlong w)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
    NSWindow* pWin = [mWin parentWindow];
    BOOL hasFocus = [mWin isKeyWindow];

    DBG_PRINT( "requestFocusParent0 - window: %p, parent %p, hasFocus %d (START)\n", mWin, pWin, hasFocus );
    if( hasFocus ) {
        if(NULL != pWin) {
            // [mWin performSelectorOnMainThread:@selector(makeFirstResponder:) withObject:pWin waitUntilDone:NO];
            [pWin performSelectorOnMainThread:@selector(makeKeyWindow) withObject:nil waitUntilDone:NO];
        } else {
            [mWin performSelectorOnMainThread:@selector(resignKeyWindow) withObject:nil waitUntilDone:NO];
        }
    }
    DBG_PRINT( "requestFocusParent0 - window: %p (END)\n", mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    orderFront0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_orderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "orderFront0 - window: %p (START)\n", win);

    [win performSelectorOnMainThread:@selector(orderFrontRegardless) withObject:nil waitUntilDone:NO];

    DBG_PRINT( "orderFront0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    orderOut
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_orderOut0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) window);
    NSWindow* pWin = [mWin parentWindow];

    DBG_PRINT( "orderOut0 - window: (parent %p) %p (START)\n", pWin, mWin);

    if(NULL == pWin) {
        [mWin performSelectorOnMainThread:@selector(orderOut:) withObject:mWin waitUntilDone:NO];
    } else {
        [mWin performSelectorOnMainThread:@selector(orderBack:) withObject:mWin waitUntilDone:NO];
    }

    DBG_PRINT( "orderOut0 - window: (parent %p) %p (END)\n", pWin, mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setTitle0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setTitle0
  (JNIEnv *env, jobject unused, jlong window, jstring title)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "setTitle0 - window: %p (START)\n", win);

    NSString* str = jstringToNSString(env, title);
    [str autorelease];
    [win setTitle: str];

    DBG_PRINT( "setTitle0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    contentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_contentView0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "contentView0 - window: %p (START)\n", win);

    jlong res = (jlong) ((intptr_t) [win contentView]);

    DBG_PRINT( "contentView0 - window: %p (END)\n", win);

    [pool release];
    return res;
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    changeContentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_changeContentView0
  (JNIEnv *env, jobject jthis, jlong parentWindowOrView, jlong window, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NewtView* newView = (NewtView *) ((intptr_t) jview);
    NewtMacWindow* win = (NewtMacWindow*) ((intptr_t) window);
    NSView* oldNSView = [win contentView];
    NewtView* oldView = NULL;

    if( [oldNSView isMemberOfClass:[NewtView class]] ) {
        oldView = (NewtView *) oldNSView;
    }

    DBG_PRINT( "changeContentView0.0 -  win %p, view (%p,%d (%d) -> %p,%d)\n", 
        win, oldNSView, getRetainCount(oldNSView), NULL!=oldView, newView, getRetainCount(newView));

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parentWindowOrView);
    NSView* pView = NULL;
    if( NULL != nsParentObj ) {
        if( [nsParentObj isKindOfClass:[NSWindow class]] ) {
            NSWindow * pWin = (NSWindow*) nsParentObj;
            pView = [pWin contentView];
        } else if( [nsParentObj isKindOfClass:[NSView class]] ) {
            pView = (NSView*) nsParentObj;
        }
    }

    changeContentView(env, jthis, pView, win, newView);

    DBG_PRINT( "changeContentView0.X -  win %p, view (%p,%d (%d) -> %p,%d)\n", 
        win, oldNSView, getRetainCount(oldNSView), NULL!=oldView, newView, getRetainCount(newView));

    [pool release];

    return (jlong) ((intptr_t) oldView);
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setContentSize
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setContentSize0
  (JNIEnv *env, jobject unused, jlong window, jint w, jint h)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "setContentSize0 - window: %p (START)\n", win);

    NSSize sz = NSMakeSize(w, h);
    [win setContentSize: sz];

    DBG_PRINT( "setContentSize0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setFrameTopLeftPoint
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setFrameTopLeftPoint0
  (JNIEnv *env, jobject unused, jlong parent, jlong window, jint x, jint y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parent);
    NSWindow* pWin = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSWindow class]] ) {
        pWin = (NSWindow*) nsParentObj;
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSView class]] ) {
        NSView* pView = (NSView*) nsParentObj;
        pWin = [pView window];
    }

    DBG_PRINT( "setFrameTopLeftPoint0 - window: %p, parent %p (START)\n", mWin, pWin);

    setFrameTopLeftPoint(pWin, mWin, x, y);

    DBG_PRINT( "setFrameTopLeftPoint0 - window: %p, parent %p (END)\n", mWin, pWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setAlwaysOnTop0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setAlwaysOnTop0
  (JNIEnv *env, jobject unused, jlong window, jboolean atop)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "setAlwaysOnTop0 - window: %p (START)\n", win);

    if(atop) {
        [win setLevel:NSFloatingWindowLevel];
    } else {
        [win setLevel:NSNormalWindowLevel];
    }

    DBG_PRINT( "setAlwaysOnTop0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    getLocationOnScreen0
 * Signature: (JII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_getLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong win, jint src_x, jint src_y)
{
    NSObject *nsObj = (NSObject*) ((intptr_t) win);
    NewtMacWindow * mWin = NULL;

    if( [nsObj isKindOfClass:[NewtMacWindow class]] ) {
        mWin = (NewtMacWindow*) nsObj;
    } else {
        NewtCommon_throwNewRuntimeException(env, "not NewtMacWindow %p\n", nsObj);
    }

    NSPoint p0 = [mWin getLocationOnScreen: NSMakePoint(src_x, src_y)];
    return (*env)->NewObject(env, pointClz, pointCstr, (jint)p0.x, (jint)p0.y);
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    setPointerVisible0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong window, jboolean hasFocus, jboolean mouseVisible)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMouseVisible: ( JNI_TRUE == mouseVisible ) ? YES : NO 
                 hasFocus: ( JNI_TRUE == hasFocus ) ? YES : NO];
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    confinePointer0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_confinePointer0
  (JNIEnv *env, jclass clazz, jlong window, jboolean confine)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMouseConfined: ( JNI_TRUE == confine ) ? YES : NO];
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    warpPointer0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_warpPointer0
  (JNIEnv *env, jclass clazz, jlong window, jint x, jint y)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMousePosition: [mWin newtClientWinPos2OSXScreenPos: NSMakePoint(x, y)]];
}

