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

static void setWindowClientTopLeftPoint(NewtMacWindow* mWin, jint x, jint y, BOOL doDisplay) {
    DBG_PRINT( "setWindowClientTopLeftPoint.0 - window: %p %d/%d, display %d\n", mWin, (int)x, (int)y, (int)doDisplay);
    NSPoint pS = [mWin newtAbsClientTLWinPos2AbsBLScreenPos: NSMakePoint(x, y)];
    DBG_PRINT( "setWindowClientTopLeftPoint.1: %d/%d\n", (int)pS.x, (int)pS.y);

    [mWin setFrameOrigin: pS];
    DBG_PRINT( "setWindowClientTopLeftPoint.X: %d/%d\n", (int)pS.x, (int)pS.y);

    if( doDisplay ) {
        NSView* mView = [mWin contentView];
        [mWin invalidateCursorRectsForView: mView];
    }
}

static void setWindowClientTopLeftPointAndSize(NewtMacWindow* mWin, jint x, jint y, jint width, jint height, BOOL doDisplay) {
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.0 - window: %p %d/%d %dx%d, display %d\n", mWin, (int)x, (int)y, (int)width, (int)height, (int)doDisplay);
    NSSize clientSZ = NSMakeSize(width, height);
    NSPoint pS = [mWin newtAbsClientTLWinPos2AbsBLScreenPos: NSMakePoint(x, y) size: clientSZ];
    NSSize topSZ = [mWin newtClientSize2TLSize: clientSZ];
    NSRect rect = { pS, topSZ };
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.1: %d/%d %dx%d\n", (int)rect.origin.x, (int)rect.origin.y, (int)rect.size.width, (int)rect.size.height);

    [mWin setFrame: rect display:doDisplay];
    DBG_PRINT( "setWindowClientTopLeftPointAndSize.X: %d/%d %dx%d\n", (int)rect.origin.x, (int)rect.origin.y, (int)rect.size.width, (int)rect.size.height);

    // -> display:YES
    // if( doDisplay ) {
    //   NSView* mView = [mWin contentView];
    //   [mWin invalidateCursorRectsForView: mView];
    // }
}

#ifdef VERBOSE_ON
static int getRetainCount(NSObject * obj) {
    return ( NULL == obj ) ? -1 : (int)([obj retainCount]) ;
}
#endif

static void setJavaWindowObject(JNIEnv *env, jobject newJavaWindowObject, NewtView *view, BOOL enable) {
    DBG_PRINT( "setJavaWindowObject.0: View %p\n", view);
    if( !enable) {
        jobject globJavaWindowObject = [view getJavaWindowObject];
        if( NULL != globJavaWindowObject ) {
            DBG_PRINT( "setJavaWindowObject.1: View %p - Clear old javaWindowObject %p\n", view, globJavaWindowObject);
            (*env)->DeleteGlobalRef(env, globJavaWindowObject);
            [view setJavaWindowObject: NULL];
        }
    } else if( NULL != newJavaWindowObject ) {
        DBG_PRINT( "setJavaWindowObject.2: View %p - Set new javaWindowObject %p\n", view, newJavaWindowObject);
        jobject globJavaWindowObject = (*env)->NewGlobalRef(env, newJavaWindowObject);
        [view setJavaWindowObject: globJavaWindowObject];
    }
    DBG_PRINT( "setJavaWindowObject.X: View %p\n", view);
}

static void changeContentView(JNIEnv *env, jobject javaWindowObject, NSView *pview, NewtMacWindow *win, NewtView *newView, BOOL setJavaWindow) {
    NSView* oldNSView = [win contentView];
    NewtView* oldNewtView = NULL;
#ifdef VERBOSE_ON
    int dbgIdx = 1;
#endif

    if( [oldNSView isKindOfClass:[NewtView class]] ) {
        oldNewtView = (NewtView *) oldNSView;
    }

    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d (%d) -> %p,%d), parent view %p\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), NULL!=oldNewtView, newView, getRetainCount(newView), pview);

    if( NULL!=oldNSView ) {
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
            dbgIdx++, win, oldNSView, getRetainCount(oldNSView), NULL!=oldNewtView, newView, getRetainCount(newView));

        if( NULL != oldNewtView ) {
            [oldNewtView setDestroyNotifySent: false];
            setJavaWindowObject(env, NULL, oldNewtView, NO);
        }
        [oldNSView removeFromSuperviewWithoutNeedingDisplay];
    }
    DBG_PRINT( "changeContentView.%d win %p, view (%p,%d -> %p,%d), isHidden %d, isHiddenOrHasHiddenAncestor: %d\n", 
        dbgIdx++, win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView), [newView isHidden], [newView isHiddenOrHasHiddenAncestor]);

    if( NULL!=newView ) {
        [newView setDestroyNotifySent: false];
        if( setJavaWindow ) {
            setJavaWindowObject(env, javaWindowObject, newView, YES);
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
    [win updateInsets: env jwin:javaWindowObject];

    DBG_PRINT( "changeContentView.X win %p, view (%p,%d -> %p,%d)\n", 
        win, oldNSView, getRetainCount(oldNSView), newView, getRetainCount(newView));
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

    NewtCommon_init(env);

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

static NSImage * createNSImageFromData(JNIEnv *env, unsigned char * iconData, jint jiconWidth, jint jiconHeight) {
    if( NULL != iconData ) {
        NSInteger iconWidth = (NSInteger) jiconWidth;
        NSInteger iconHeight = (NSInteger) jiconHeight;
        const NSInteger bpc = 8 /* bits per component */, spp=4 /* RGBA */, bpp = bpc * spp;
        const NSBitmapFormat bfmt = NSAlphaNonpremultipliedBitmapFormat;
        const BOOL hasAlpha = YES;

        NSBitmapImageRep* bir = [[NSBitmapImageRep alloc] initWithBitmapDataPlanes: &iconData
                                    pixelsWide: iconWidth
                                    pixelsHigh: iconHeight
                                    bitsPerSample: bpc
                                    samplesPerPixel: spp
                                    hasAlpha: hasAlpha
                                    isPlanar: NO
                                    colorSpaceName: NSCalibratedRGBColorSpace
                                    bitmapFormat: bfmt
                                    bytesPerRow: iconWidth*4
                                    bitsPerPixel: bpp];
        [bir autorelease];
        NSImage* nsImage = [[NSImage alloc] initWithCGImage: [bir CGImage] size:NSZeroSize];
        return nsImage;
    }
    return NULL;
}

/*
 * Class:     jogamp_newt_driver_macosx_DisplayDriver
 * Method:    setAppIcon0
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_setAppIcon0
  (JNIEnv *env, jobject unused, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, jint width, jint height)
{
    if( 0 == pixels ) {
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    // NOTE: MUST BE DIRECT BUFFER, since NSBitmapImageRep uses buffer directly!
    unsigned char * pixelPtr = (unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                            (*env)->GetDirectBufferAddress(env, pixels) : 
                                            (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );
    NSImage * nsImage = createNSImageFromData(env, pixelPtr + pixels_byte_offset, width, height);
    if( NULL != nsImage ) {
        [nsImage autorelease];
        [NSApp setApplicationIconImage: nsImage];
    }
    if ( JNI_FALSE == pixels_is_direct ) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
    }
    [pool release];
}

JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_createPointerIcon0
  (JNIEnv *env, jobject unused, jobject pixels, jint pixels_byte_offset, jboolean pixels_is_direct, jint width, jint height, jint hotX, jint hotY)
{
    if( 0 == pixels ) {
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    unsigned char * pixelPtr = (unsigned char *) ( JNI_TRUE == pixels_is_direct ? 
                                            (*env)->GetDirectBufferAddress(env, pixels) : 
                                            (*env)->GetPrimitiveArrayCritical(env, pixels, NULL) );
    NSImage * nsImage = createNSImageFromData(env, pixelPtr + pixels_byte_offset, width, height);
    NSCursor * res = NULL;
    if( NULL != nsImage ) {
        [nsImage autorelease];
        NSPoint hotP = { hotX, hotY };
        res = [[NSCursor alloc] initWithImage: nsImage hotSpot: hotP];
    }
    if ( JNI_FALSE == pixels_is_direct ) {
        (*env)->ReleasePrimitiveArrayCritical(env, pixels, (void*)pixelPtr, JNI_ABORT);  
    }
    [pool release];
    DBG_PRINT( "createPointerIcon0 %p\n", res);
    return (jlong) (intptr_t) res;
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_DisplayDriver_destroyPointerIcon0
  (JNIEnv *env, jobject unused, jlong handle)
{
    NSCursor * c = (NSCursor*) (intptr_t) handle ;
    if( NULL != c && NO == [c isKindOfClass:[NSCursor class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NSCursor %p", c);
        return;
    }
    DBG_PRINT( "destroyPointerIcon0 %p\n", c);
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [c release];
    [pool release];
}

#if 0
static NSScreen * NewtScreen_getNSScreenByIndex(int screen_idx) {
    NSArray *screens = [NSScreen screens];
    if( screen_idx<0 || screen_idx>=[screens count] ) {
        return NULL;
    }
    return (NSScreen *) [screens objectAtIndex: screen_idx];
}
static NSScreen * NewtScreen_getNSScreenByCoord(int x, int y) {
    NSArray *screens = [NSScreen screens];
    int i;
    for(i=[screens count]-1; i>=0; i--) {
        NSScreen * screen = (NSScreen *) [screens objectAtIndex: i];
        NSRect frame = [screen frame];
        if( x >= frame.origin.x && 
            y >= frame.origin.y &&
            x <  frame.origin.x + frame.size.width &&
            y <  frame.origin.y + frame.size.height ) {
            return screen;
        }
    }
    return NULL;
}
#endif
static void NewtScreen_dump() {
#ifdef VERBOSE_ON
    NSArray *screens = [NSScreen screens];
    int i;
    for(i=0; i<[screens count]; i++) {
        NSScreen * screen = (NSScreen *) [screens objectAtIndex: i];
        NSRect screenFrame = [screen frame];
        NSRect screenVisibleFrame = [screen visibleFrame];
        CGFloat pixelScale = 1.0; // default
NS_DURING
        // Available >= 10.7
        pixelScale = [screen backingScaleFactor]; // HiDPI scaling
NS_HANDLER
NS_ENDHANDLER
        NSWindowDepth depth = [screen depth]; // an (int) value!
        DBG_PRINT( "NSScreen #%d (%p): Frame %lf/%lf %lfx%lf (vis %lf/%lf %lfx%lf), scale %lf, depth %d\n",
            i, screen,
            screenFrame.origin.x, screenFrame.origin.y, screenFrame.size.width, screenFrame.size.height,
            screenVisibleFrame.origin.x, screenVisibleFrame.origin.y, screenVisibleFrame.size.width, screenVisibleFrame.size.height,
            pixelScale, depth);
    }
#endif
}


CGDirectDisplayID NewtScreen_getCGDirectDisplayIDByNSScreen(NSScreen *screen) {
    if( NULL == screen ) {
        return (CGDirectDisplayID)0;
    }
    // Mind: typedef uint32_t CGDirectDisplayID;
    NSDictionary * dict = [screen deviceDescription];
    NSNumber * val = (NSNumber *) [dict objectForKey: @"NSScreenNumber"];
    // [NSNumber integerValue] returns NSInteger which is 32 or 64 bit native size
    return (CGDirectDisplayID) [val integerValue];
}
static NSScreen * NewtScreen_getNSScreenByCGDirectDisplayID(CGDirectDisplayID displayID) {
    NSArray *screens = [NSScreen screens];
    int i;
    for(i=[screens count]-1; i>=0; i--) {
        NSScreen * screen = (NSScreen *) [screens objectAtIndex: i];
        CGDirectDisplayID dID = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
        if( dID == displayID ) {
            return screen;
        }
    }
    return NULL;
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
 * Method:    getMonitorDeviceIds0
 * Signature: ()I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getMonitorDeviceIds0
  (JNIEnv *env, jobject obj)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSArray *screens = [NSScreen screens];
    int count = [screens count];
    int32_t displayIDs[count];
    int i;
    for(i=0; i<count; i++) {
        NSScreen * screen = (NSScreen *) [screens objectAtIndex: i];
        displayIDs[i] = (int32_t)NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
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
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getMonitorProps0
 * Signature: (I)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getMonitorProps0
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
    CGDirectDisplayID displayID = (CGDirectDisplayID)crt_id;
    NSScreen *screen =  NewtScreen_getNSScreenByCGDirectDisplayID(displayID);
    if( NULL == screen ) {
        [pool release];
        return NULL;
    }
    CGDirectDisplayID mainDisplayID = CGMainDisplayID();
    BOOL isPrimary = mainDisplayID == displayID;
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getMonitorProps0.2: %ld ms\n", td_ms); fflush(NULL);
#endif

    CGSize sizeMM = CGDisplayScreenSize(displayID);
#ifdef DBG_PERF
    timespec_now(&t1); timespec_subtract(&td, &t1, &t0); td_ms = timespec_milliseconds(&td);
    fprintf(stderr, "MacScreen_getMonitorProps0.3: %ld ms\n", td_ms); fflush(NULL);
#endif

    CGRect dBounds = CGDisplayBounds (displayID); // origin top-left
#ifdef VERBOSE_ON
    BOOL usesGL = CGDisplayUsesOpenGLAcceleration(displayID);
    NSRect sFrame = [screen frame]; // origin bottom-left
    DBG_PRINT( "getMonitorProps0: crt_id 0x%X (prim %d), top-left displayBounds[%d/%d %dx%d], bottom-left screenFrame[%d/%d %dx%d], usesGL %d\n", 
                 (int)crt_id, isPrimary,
                 (int)dBounds.origin.x, (int)dBounds.origin.y, (int)dBounds.size.width, (int)dBounds.size.height,
                 (int)sFrame.origin.x, (int)sFrame.origin.y, (int)sFrame.size.width, (int)sFrame.size.height,
                 (int)usesGL);
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
    prop[offset++] = (jint) dBounds.origin.x;    // rotated viewport x      (pixel units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.origin.y;    // rotated viewport y      (pixel units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.size.width;  // rotated viewport width  (pixel units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.size.height; // rotated viewport height (pixel units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.origin.x;    // rotated viewport x      (window units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.origin.y;    // rotated viewport y      (window units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.size.width;  // rotated viewport width  (window units, will be fixed in java code)
    prop[offset++] = (jint) dBounds.size.height; // rotated viewport height (window units, will be fixed in java code)

    jintArray properties = (*env)->NewIntArray(env, propCount);
    if (properties == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array of size %d", propCount);
    }
    (*env)->SetIntArrayRegion(env, properties, 0, propCount, prop);
    
    [pool release];

    return properties;
}

/*
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    getMonitorMode0
 * Signature: (II)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_getMonitorMode0
  (JNIEnv *env, jobject obj, jint crt_id, jint mode_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    CGDirectDisplayID displayID = (CGDirectDisplayID)crt_id;
    NSScreen *screen =  NewtScreen_getNSScreenByCGDirectDisplayID(displayID);
    if( NULL == screen ) {
        [pool release];
        return NULL;
    }
    CGFloat pixelScale = 1.0; // default
NS_DURING
    // Available >= 10.7
    pixelScale = [screen backingScaleFactor]; // HiDPI scaling
NS_HANDLER
NS_ENDHANDLER

    CFArrayRef availableModes = CGDisplayAvailableModes(displayID);
    CFIndex numberOfAvailableModes = CFArrayGetCount(availableModes);
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes; 
    CFDictionaryRef mode = NULL;
    int currentCCWRot = (int)CGDisplayRotation(displayID);
    jint ccwRot = 0;
    int nativeId = 0;

#ifdef VERBOSE_ON
    if(0 >= mode_idx) {
        // only for current mode (-1) and first mode (scanning)
        DBG_PRINT( "getScreenMode0: crtID 0x%X (s %p, pscale %lf), mode %d, avail: %d/%d, current rot %d ccw\n",  
            (uint32_t)displayID, screen, pixelScale, (int)mode_idx, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots, currentCCWRot);
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
        mode = (CFDictionaryRef)CFArrayGetValueAtIndex(availableModes, nativeId);
    } else {
        // current mode
        mode = CGDisplayCurrentMode(displayID);
        ccwRot = currentCCWRot;
        CFRange range = CFRangeMake (0, numberOfAvailableModes);
        nativeId = CFArrayGetFirstIndexOfValue(availableModes, range, (CFDictionaryRef)mode);
    }
    // mode = CGDisplayModeRetain(mode); // 10.6 on CGDisplayModeRef

    int mWidth = CGDDGetModeWidth(mode);
    int mHeight = CGDDGetModeHeight(mode);
    if( -1 == mode_idx ) {
        mWidth *= (int)pixelScale;   // accomodate HiDPI
        mHeight *= (int)pixelScale; // accomodate HiDPI
    }

    // swap width and height, since OSX reflects rotated dimension, we don't
    if ( 90 == currentCCWRot || 270 == currentCCWRot ) {
        int tempWidth = mWidth;
        mWidth = mHeight;
        mHeight = tempWidth;
    }

    jint prop[ NUM_MONITOR_MODE_PROPERTIES_ALL ];
    int propIndex = 0;

    int refreshRate = CGDDGetModeRefreshRate(mode);
    int fRefreshRate = ( 0 < refreshRate ) ? refreshRate : 60; // default .. (experienced on OSX 10.6.8)
    prop[propIndex++] = NUM_MONITOR_MODE_PROPERTIES_ALL;
    prop[propIndex++] = mWidth;
    prop[propIndex++] = mHeight;
    prop[propIndex++] = CGDDGetModeBitsPerPixel(mode);
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
 * Class:     jogamp_newt_driver_macosx_ScreenDriver
 * Method:    setMonitorMode0
 * Signature: (III)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_ScreenDriver_setMonitorMode0
  (JNIEnv *env, jobject object, jint crt_id, jint nativeId, jint ccwRot)
{
    jboolean res = JNI_TRUE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    CGDirectDisplayID displayID = (CGDirectDisplayID)crt_id;
    NSScreen *screen =  NewtScreen_getNSScreenByCGDirectDisplayID(displayID);
    if( NULL == screen ) {
        [pool release];
        return JNI_FALSE;
    }
    CFArrayRef availableModes = CGDisplayAvailableModes(displayID);
    CFIndex numberOfAvailableModes = CFArrayGetCount(availableModes);
#ifdef VERBOSE_ON
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes;
#endif

    DBG_PRINT( "setScreenMode0: crtID 0x%X (%p), nativeID %d, rot %d ccw, avail: %d/%d\n",  
        (uint32_t)displayID, screen, (int)nativeId, ccwRot, (int)numberOfAvailableModes, (int)numberOfAvailableModesRots);

    CFDictionaryRef mode = NULL;

    if( 0 != ccwRot ) {
        // FIXME: How to rotate the display/screen on OSX programmatically ?
        DBG_PRINT( "setScreenMode0: Don't know how to rotate screen on OS X: rot %d ccw\n", ccwRot);
        res = JNI_FALSE;
    } else {
        if( numberOfAvailableModes <= nativeId ) {
            res = JNI_FALSE;
        } else {
            mode = (CFDictionaryRef)CFArrayGetValueAtIndex(availableModes, nativeId);
            // mode = CGDisplayModeRetain(mode); // 10.6 on CGDisplayModeRef
        }
    }

    if( NULL != mode ) {
        CGError err = CGDisplaySwitchToMode(displayID, mode);
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

    NewtScreen_dump();

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
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    createView0
 * Signature: (IIII)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_createView0
  (JNIEnv *env, jobject jthis, jint x, jint y, jint w, jint h)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT( "createView0 - %p (this), %d/%d %dx%d (START)\n",
        (void*)(intptr_t)jthis, (int)x, (int)y, (int)w, (int)h);

    NSRect rectView = NSMakeRect(0, 0, w, h);
    NewtView *myView = [[NewtView alloc] initWithFrame: rectView] ;
    DBG_PRINT( "createView0.X - new view: %p\n", myView);

    [pool release];

    return (jlong) (intptr_t) myView;
}

/**
 * Method creates a deferred un-initialized Window, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    createWindow0
 * Signature: (IIIIZIIJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_createWindow0
  (JNIEnv *env, jobject jthis, jint x, jint y, jint w, jint h,
   jboolean fullscreen, jint styleMask, jint bufferingType, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtView* myView = (NewtView*) (intptr_t) jview ;

    DBG_PRINT( "createWindow0 - %p (this), %d/%d %dx%d, fs %d, style %X, buffType %X, view %p (START)\n",
        (void*)(intptr_t)jthis, (int)x, (int)y, (int)w, (int)h, (int)fullscreen, 
        (int)styleMask, (int)bufferingType, myView);
    (void)myView;

    if (fullscreen) {
        styleMask = NSBorderlessWindowMask;
    }
    NSRect rectWin = NSMakeRect(x, y, w, h);

    // Allocate the window
    NewtMacWindow* myWindow = [[NewtMacWindow alloc] initWithContentRect: rectWin
                                               styleMask: (NSUInteger) styleMask
                                               backing: (NSBackingStoreType) bufferingType
                                               defer: YES
                                               isFullscreenWindow: fullscreen];
    // DBG_PRINT( "createWindow0.1 - %p, isVisible %d\n", myWindow, [myWindow isVisible]);

    DBG_PRINT( "createWindow0.X - %p, isVisible %d\n", myWindow, [myWindow isVisible]);

    [pool release];

    return (jlong) ((intptr_t) myWindow);
}

JNIEXPORT jint JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_getDisplayID0(JNIEnv *env, jobject jthis, jlong window) {
    NewtMacWindow* myWindow = (NewtMacWindow*) ((intptr_t) window);
    if( NULL == myWindow ) {
        DBG_PRINT( "getDisplayID0 - NULL NEWT win - abort\n");
        return 0;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSScreen *screen =  [myWindow screen];
    int32_t displayID = (int32_t)NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
    [pool release];
    return (jint) displayID;
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    initWindow0
 * Signature: (JJIIIIFZZZJ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_initWindow0
  (JNIEnv *env, jobject jthis, jlong parent, jlong window, jint x, jint y, jint w, jint h, jfloat reqPixelScale,
   jboolean opaque, jboolean atop, jboolean abottom, jboolean visible, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* myWindow = (NewtMacWindow*) ((intptr_t) window);
    NewtView* myView = (NewtView*) (intptr_t) jview ;
    BOOL fullscreen = myWindow->isFullscreenWindow;

    DBG_PRINT( "initWindow0 - %p (this), %p (parent), %p (window), %d/%d %dx%d, reqPixScale %f, opaque %d, atop %d, abottom %d, fs %d, visible %d, view %p (START)\n",
        (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, (int)x, (int)y, (int)w, (int)h, (float)reqPixelScale,
        (int) opaque, (int)atop, (int)abottom, (int)fullscreen, (int)visible, myView);

NS_DURING
    // HiDPI scaling: Setup - Available >= 10.7
    if( 1.0 == reqPixelScale ) {
        [myView setWantsBestResolutionOpenGLSurface: NO];
    } else {
        [myView setWantsBestResolutionOpenGLSurface: YES];
    }
NS_HANDLER
NS_ENDHANDLER

    [myWindow setReleasedWhenClosed: NO]; // We control NSWindow destruction!
    [myWindow setPreservesContentDuringLiveResize: NO];
NS_DURING
        if ( [myWindow respondsToSelector:@selector(setRestorable:)] ) {
            // Available >= 10.7 - Removes restauration 'feature', really close
            [myWindow setRestorable: NO];
        }
NS_HANDLER
NS_ENDHANDLER

    NSObject* nsParentObj = (NSObject*) ((intptr_t) parent);
    NSWindow* parentWindow = NULL;
    NSView* parentView = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSWindow class]] ) {
        parentWindow = (NSWindow*) nsParentObj;
        parentView = [parentWindow contentView];
        DBG_PRINT( "initWindow0 - Parent is NSWindow : %p (win) -> %p (view) \n", parentWindow, parentView);
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSView class]] ) {
        parentView = (NSView*) nsParentObj;
        parentWindow = [parentView window];
        DBG_PRINT( "initWindow0 - Parent is NSView : %p -(view) > %p (win) \n", parentView, parentWindow);
    } else {
        DBG_PRINT( "initWindow0 - Parent is neither NSWindow nor NSView : %p\n", nsParentObj);
    }
    DBG_PRINT( "initWindow0 - is visible.1: %d\n", [myWindow isVisible]);

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
        DBG_PRINT( "initWindow0.%d\n", dbgIdx++);
        if (!fullscreen) {
            [myWindow setShowsResizeIndicator: YES];
        }
        DBG_PRINT( "initWindow0.%d\n", dbgIdx++);
    } else {
        [myWindow setOpaque: NO];
        [myWindow setBackgroundColor: [NSColor clearColor]];
    }
    [myWindow setAlwaysOn: atop bottom:abottom];

    // specify we want mouse-moved events
    [myWindow setAcceptsMouseMovedEvents:YES];

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

    // Set the content view
    changeContentView(env, jthis, parentView, myWindow, myView, NO);
    [myWindow setInitialFirstResponder: myView];

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

    if(NULL!=parentWindow) {
        [myWindow attachToParent: parentWindow];
    }

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d, visible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible], visible);

    // Immediately re-position this window based on an upper-left coordinate system
    setWindowClientTopLeftPointAndSize(myWindow, x, y, w, h, NO);

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

NS_DURING
    // concurrent view rendering
    // Available >= 10.6 - Makes the menubar disapear
    if ( [myWindow respondsToSelector:@selector(setAllowsConcurrentViewDrawing:)] ) {
        [myWindow setAllowsConcurrentViewDrawing: YES];
    }

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

    if ( [myView respondsToSelector:@selector(setCanDrawConcurrently:)] ) {
        [myView setCanDrawConcurrently: YES];
    }
NS_HANDLER
NS_ENDHANDLER

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

    // visible on front
    if( visible ) {
        [myWindow orderFront: myWindow];
    }

    DBG_PRINT( "initWindow0.%d - %p view %p, isVisible %d\n", 
        dbgIdx++, myWindow, myView, [myWindow isVisible]);

    // force surface creation
    // [myView lockFocus];
    // [myView unlockFocus];

    // Set the next responder to be the window so that we can forward
    // right mouse button down events
    [myView setNextResponder: myWindow];

    DBG_PRINT( "initWindow0.%d - %p (this), %p (parent): new window: %p, view %p\n",
        dbgIdx++, (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, myView);

    [myView setDestroyNotifySent: false];
    setJavaWindowObject(env, jthis, myView, YES);

    DBG_PRINT( "initWindow0.%d - %p (this), %p (parent): new window: %p, view %p\n",
        dbgIdx++, (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, myView);

NS_DURING
    if( fullscreen ) {
        /** 
         * See Bug 914: We don't use exclusive fullscreen anymore (capturing display)
         * allowing ALT-TAB to allow process/app switching!
         * Shall have no penalty on modern GPU and is also recommended, see bottom box @
         * <https://developer.apple.com/library/mac/documentation/graphicsimaging/Conceptual/QuartzDisplayServicesConceptual/Articles/DisplayCapture.html>
         *
        NSScreen *myScreen =  NewtScreen_getNSScreenByCoord(x, y);
        if( NULL != myScreen ) {
            if ( [myView respondsToSelector:@selector(enterFullScreenMode:withOptions:)] ) {
                // Available >= 10.5 - Makes the menubar disapear
                [myView enterFullScreenMode: myScreen withOptions:NULL];
            } 
        }
        */
        if( myWindow->hasPresentationSwitch ) {
            DBG_PRINT( "initWindow0.%d - %p view %p, setPresentationOptions 0x%X\n", 
                dbgIdx++, myWindow, myView, (int)myWindow->fullscreenPresentationOptions);
            [NSApp setPresentationOptions: myWindow->fullscreenPresentationOptions];
        }
    }
NS_HANDLER
NS_ENDHANDLER

    DBG_PRINT( "initWindow0.%d - %p (this), %p (parent): new window: %p, view %p\n",
        dbgIdx++, (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, myView);

    [pool release];
    DBG_PRINT( "initWindow0.X - %p (this), %p (parent): new window: %p, view %p\n",
        (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow, myView);
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setPixelScale0
 * Signature: (JJF)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setPixelScale0
  (JNIEnv *env, jobject jthis, jlong window, jlong view, jfloat reqPixelScale)
{
    NewtMacWindow* myWindow = (NewtMacWindow*) ((intptr_t) window);
    if( NULL == myWindow ) {
        DBG_PRINT( "setPixelScale0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtView* myView = (NewtView*) (intptr_t) view ;
#ifdef VERBOSE_ON
    int dbgIdx = 1;
#endif
    DBG_PRINT( "setPixelScale0 - %p (this), %p (window), view %p, reqPixScale %f (START)\n",
        (void*)(intptr_t)jthis, myWindow, myView, (float)reqPixelScale);
    (void)myWindow;

NS_DURING
    // HiDPI scaling: Setup - Available >= 10.7
    if( 1.0 == reqPixelScale ) {
        [myView setWantsBestResolutionOpenGLSurface: NO];
    } else {
        [myView setWantsBestResolutionOpenGLSurface: YES];
    }
NS_HANDLER
NS_ENDHANDLER

    DBG_PRINT( "setPixelScale0.%d - %p (this), window: %p, view %p\n",
        dbgIdx++, (void*)(intptr_t)jthis, myWindow, myView);

    [pool release];
    DBG_PRINT( "setPixelScale0.X - %p (this), window: %p, view %p\n",
        (void*)(intptr_t)jthis, myWindow, myView);
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
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "windowClose.0 - NULL NEWT win - abort\n");
        return;
    }
    BOOL isNSWin = [mWin isKindOfClass:[NSWindow class]];
    BOOL isNewtWin = [mWin isKindOfClass:[NewtMacWindow class]];
    NSWindow *pWin = [mWin parentWindow];
    DBG_PRINT( "windowClose.0 - %p [isNSWindow %d, isNewtWin %d], parent %p\n", mWin, isNSWin, isNewtWin, pWin);
    (void)isNSWin; // silence
    if( !isNewtWin ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtView* mView = (NewtView *)[mWin contentView];
    BOOL fullscreen = mWin->isFullscreenWindow;
    BOOL destroyNotifySent, isNSView, isNewtView;
    if( NULL != mView ) {
        isNSView = [mView isKindOfClass:[NSView class]];
        isNewtView = [mView isKindOfClass:[NewtView class]];
        destroyNotifySent = isNewtView ? [mView getDestroyNotifySent] : false;
    } else {
        isNSView = false;
        isNewtView = false;
        destroyNotifySent = false;
    }

    DBG_PRINT( "windowClose.0 - %p, destroyNotifySent %d, view %p [isNSView %d, isNewtView %d], fullscreen %d, parent %p\n", 
        mWin, destroyNotifySent, mView, isNSView, isNewtView, (int)fullscreen, pWin);

    [mWin setRealized: NO];

    if( isNewtView ) {
        // cleanup view
        [mView setDestroyNotifySent: true];
        setJavaWindowObject(env, NULL, mView, NO);
    }

NS_DURING
    /** 
     * See Bug 914: We don't use exclusive fullscreen anymore (capturing display)
     * See initWindow0(..) above ..
    if(NULL!=mView) {
        BOOL iifs;
        if ( [mView respondsToSelector:@selector(isInFullScreenMode)] ) {
            iifs = [mView isInFullScreenMode];
        } else {
            iifs = NO;
        }
        if(iifs && [mView respondsToSelector:@selector(exitFullScreenModeWithOptions:)] ) {
            [mView exitFullScreenModeWithOptions: NULL];
        }
    } */
    // Note: mWin's release will also release it's mView!
    DBG_PRINT( "windowClose.1a - %p view %p, fullscreen %d, hasPresSwitch %d, defaultPresentationOptions 0x%X\n", 
        mWin, mView, (int)fullscreen, (int)mWin->hasPresentationSwitch, (int)mWin->defaultPresentationOptions);

    if( fullscreen && mWin->hasPresentationSwitch ) {
        DBG_PRINT( "windowClose.1b - %p view %p, setPresentationOptions 0x%X\n", 
            mWin, mView, (int)mWin->defaultPresentationOptions);
        [NSApp setPresentationOptions: mWin->defaultPresentationOptions];
    }
NS_HANDLER
NS_ENDHANDLER

    if(NULL!=pWin) {
        [mWin detachFromParent: pWin];
    }
    [mWin orderOut: mWin];

    DBG_PRINT( "windowClose.2 - %p view %p, parent %p\n", mWin, mView, pWin);

    [mWin release];

    DBG_PRINT( "windowClose.Xp\n");

    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    lockSurface0
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_lockSurface0
  (JNIEnv *env, jclass clazz, jlong window, jlong view)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    if(NO == [mWin isRealized]) {
        return JNI_FALSE;
    }
    NewtView * mView = (NewtView *) ((intptr_t) view);
    return [mView softLock] == YES ? JNI_TRUE : JNI_FALSE;
    /** deadlocks, since we render independent of focus
    return [mView lockFocusIfCanDraw] == YES ? JNI_TRUE : JNI_FALSE; */
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    unlockSurface0
 * Signature: (JJ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_unlockSurface0
  (JNIEnv *env, jclass clazz, jlong window, jlong view)
{
    // NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    (void) window;
    NewtView * mView = (NewtView *) ((intptr_t) view);
    return [mView softUnlock] == YES ? JNI_TRUE : JNI_FALSE;
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
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
    if( NULL == mWin ) {
        DBG_PRINT( "requestFocus - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
#ifdef VERBOSE_ON
    BOOL hasFocus = [mWin isKeyWindow];
#endif
    DBG_PRINT( "requestFocus - window: %p, force %d, hasFocus %d (START)\n", mWin, force, hasFocus);

    [mWin setAcceptsMouseMovedEvents: YES];
    [mWin makeFirstResponder: nil];
    [mWin orderFrontRegardless];
    [mWin makeKeyWindow];

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
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
    if( NULL == mWin ) {
        DBG_PRINT( "resignFocus0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* pWin = [mWin parentWindow];
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
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    orderFront0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_orderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSWindow* mWin = (NSWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "orderFront0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* pWin = [mWin parentWindow];

    DBG_PRINT( "orderFront0 - window: (parent %p) %p visible %d (START)\n", pWin, mWin, [mWin isVisible]);

    if( NULL == pWin ) {
        [mWin orderFrontRegardless];
    } else {
        [mWin orderWindow: NSWindowAbove relativeTo: [pWin windowNumber]];
    }

    DBG_PRINT( "orderFront0 - window: (parent %p) %p (END)\n", pWin, mWin);

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
    NSWindow* mWin = (NSWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "orderOut0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* pWin = [mWin parentWindow];

    DBG_PRINT( "orderOut0 - window: (parent %p) %p visible %d (START)\n", pWin, mWin, [mWin isVisible]);

    if( NULL == pWin ) {
        [mWin orderOut: mWin];
    } else {
        [mWin orderWindow: NSWindowOut relativeTo: [pWin windowNumber]];
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
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    if( NULL == win ) {
        DBG_PRINT( "setTitle0 - NULL NEWT win - abort\n");
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    DBG_PRINT( "setTitle0 - window: %p (START)\n", win);

    NSString* str = jstringToNSString(env, title);
    [str autorelease];
    [win setTitle: str];

    DBG_PRINT( "setTitle0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    contentView0
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_contentView0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSView* nsView = [win contentView];
    NewtView* newtView = NULL;

    if( [nsView isKindOfClass:[NewtView class]] ) {
        newtView = (NewtView *) nsView;
    }

    DBG_PRINT( "contentView0 - window: %p, view: %p, newtView %p\n", win, nsView, newtView);

    jlong res = (jlong) ((intptr_t) nsView);

    [pool release];
    return res;
}

/**
 * Method is called on Main-Thread, hence no special invocation required inside method.
 *
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    changeContentView
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_changeContentView0
  (JNIEnv *env, jobject jthis, jlong parentWindowOrView, jlong window, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NewtView* newView = (NewtView *) ((intptr_t) jview);
    NewtMacWindow* win = (NewtMacWindow*) ((intptr_t) window);

    DBG_PRINT( "changeContentView0.0 -  win %p, view (%p,%d)\n", 
        win, newView, getRetainCount(newView));

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

    changeContentView(env, jthis, pView, win, newView, YES);

    DBG_PRINT( "changeContentView0.X\n");

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    updateSizePosInsets0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_updateSizePosInsets0
  (JNIEnv *env, jobject jthis, jlong window, jboolean defer)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);

    DBG_PRINT( "updateSizePosInsets - window: %p, defer %d (START)\n", mWin, (int)defer);

    [mWin updateSizePosInsets: env jwin:jthis defer:defer];

    DBG_PRINT( "updateSizePosInsets - window: %p, defer %d (END)\n", mWin, (int)defer);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setWindowClientTopLeftPointAndSize0
 * Signature: (JIIIIZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setWindowClientTopLeftPointAndSize0
  (JNIEnv *env, jobject unused, jlong window, jint x, jint y, jint w, jint h, jboolean display)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);

    DBG_PRINT( "setWindowClientTopLeftPointAndSize - window: %p (START)\n", mWin);

    setWindowClientTopLeftPointAndSize(mWin, x, y, w, h, display);

    DBG_PRINT( "setWindowClientTopLeftPointAndSize - window: %p (END)\n", mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    setWindowClientTopLeftPoint0
 * Signature: (JIIZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setWindowClientTopLeftPoint0
  (JNIEnv *env, jobject unused, jlong window, jint x, jint y, jboolean display)
{
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);
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
 * Class:     jogamp_newt_driver_macosx_WindowDriver
 * Method:    getLocationOnScreen0
 * Signature: (JII)Lcom/jogamp/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_getLocationOnScreen0
  (JNIEnv *env, jclass unused, jlong win, jint src_x, jint src_y)
{
    NewtMacWindow *mWin = (NewtMacWindow*) (intptr_t) win;
    if( NULL == mWin ) {
        DBG_PRINT( "getLocationOnScreen0 - NULL NEWT win - abort\n");
        return NULL;
    }
    if( ![mWin isKindOfClass:[NewtMacWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return NULL;
    }
    NSPoint p0 = [mWin getLocationOnScreen: NSMakePoint(src_x, src_y)];
    return (*env)->NewObject(env, pointClz, pointCstr, (jint)p0.x, (jint)p0.y);
}

JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setPointerIcon0
  (JNIEnv *env, jobject unused, jlong window, jlong handle)
{
    NewtMacWindow *mWin = (NewtMacWindow*) (intptr_t) window;
    if( NULL == mWin ) {
        DBG_PRINT( "setPointerIcon0 - NULL NEWT win - abort\n");
        return;
    }
    NSCursor *c = (NSCursor*) (intptr_t) handle ;
    if ( NULL != c && NO == [c isKindOfClass:[NSCursor class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NSCursor %p", c);
        return;
    }
    if( ! [mWin isKindOfClass:[NewtMacWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return;
    }
    NewtView* nView = (NewtView *) [mWin contentView];
    if( ! [nView isKindOfClass:[NewtView class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtView %p", nView);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nView setPointerIcon: c];
    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    setPointerVisible0
 * Signature: (JZ)Z
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong window, jboolean hasFocus, jboolean mouseVisible)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "setPointerVisible0 - NULL NEWT win - abort\n");
        return;
    }
    if( ! [mWin isKindOfClass:[NewtMacWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return;
    }
    NewtView* nView = (NewtView *) [mWin contentView];
    if( ! [nView isKindOfClass:[NewtView class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtView %p", nView);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nView setMouseVisible: ( JNI_TRUE == mouseVisible ) ? YES : NO 
                  hasFocus: ( JNI_TRUE == hasFocus ) ? YES : NO];
    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_WindowDriver
 * Method:    confinePointer0
 * Signature: (JZ)Z
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_WindowDriver_confinePointer0
  (JNIEnv *env, jclass clazz, jlong window, jboolean confine)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    if( NULL == mWin ) {
        DBG_PRINT( "confinePointer0 - NULL NEWT win - abort\n");
        return;
    }
    if( ! [mWin isKindOfClass:[NewtMacWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return;
    }
    NewtView* nView = (NewtView *) [mWin contentView];
    if( ! [nView isKindOfClass:[NewtView class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtView %p", nView);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nView setMouseConfined: ( JNI_TRUE == confine ) ? YES : NO];
    [pool release];
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
    if( NULL == mWin ) {
        DBG_PRINT( "warpPointer0 - NULL NEWT win - abort\n");
        return;
    }
    if( ! [mWin isKindOfClass:[NewtMacWindow class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtMacWindow %p", mWin);
        return;
    }
    NewtView* nView = (NewtView *) [mWin contentView];
    if( ! [nView isKindOfClass:[NewtView class]] ) {
        NewtCommon_throwNewRuntimeException(env, "Not a NewtView %p", nView);
        return;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [nView setMousePosition: [mWin newtRelClientTLWinPos2AbsBLScreenPos: NSMakePoint(x, y)]];
    [pool release];
}

