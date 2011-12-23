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

#import "jogamp_newt_driver_macosx_MacWindow.h"
#import "NewtMacWindow.h"

#import "MouseEvent.h"
#import "KeyEvent.h"
#import "ScreenMode.h"

#import <ApplicationServices/ApplicationServices.h>

#import <stdio.h>

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
        if([oldNSView isInFullScreenMode]) {
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
 * Class:     jogamp_newt_driver_macosx_MacDisplay
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacDisplay_initNSApplication0
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
 * Class:     jogamp_newt_driver_macosx_MacDisplay
 * Method:    runNSApplication0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacDisplay_runNSApplication0
  (JNIEnv *env, jclass clazz)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    DBG_PRINT( "\nrunNSApplication0.0\n");

    [NSApp run];

    DBG_PRINT( "\nrunNSApplication0.X\n");
    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacDisplay
 * Method:    stopNSApplication0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacDisplay_stopNSApplication0
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
 * Class:     jogamp_newt_driver_macosx_MacScreen
 * Method:    getWidthImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_macosx_MacScreen_getWidthImpl0
  (JNIEnv *env, jclass clazz, jint screen_idx)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)screen_idx);
    NSRect rect = [screen frame];

    [pool release];

    return (jint) (rect.size.width);
}

/*
 * Class:     jogamp_newt_driver_macosx_MacScreen
 * Method:    getHeightImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_jogamp_newt_driver_macosx_MacScreen_getHeightImpl0
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
 * Class:     jogamp_newt_driver_macosx_MacScreen
 * Method:    getScreenMode0
 * Signature: (II)[I
 */
JNIEXPORT jintArray JNICALL Java_jogamp_newt_driver_macosx_MacScreen_getScreenMode0
  (JNIEnv *env, jobject obj, jint scrn_idx, jint mode_idx)
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

    CGSize screenDim = CGDisplayScreenSize(display);
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
    prop[propIndex++] = 0; // set later for verification of iterator
    propIndexRes = propIndex;
    prop[propIndex++] = mWidth;
    prop[propIndex++] = mHeight;
    prop[propIndex++] = CGDDGetModeBitsPerPixel(mode);
    prop[propIndex++] = (jint) screenDim.width;
    prop[propIndex++] = (jint) screenDim.height;
    prop[propIndex++] = CGDDGetModeRefreshRate(mode);
    prop[propIndex++] = ccwRot;
    prop[propIndex - NUM_SCREEN_MODE_PROPERTIES_ALL] = ( -1 < mode_idx ) ? propIndex-1 : propIndex ; // count == NUM_SCREEN_MODE_PROPERTIES_ALL

    DBG_PRINT( "getScreenMode0: Mode %d/%d (%d): %dx%d, %d bpp, %dx%d mm, %d Hz, rot %d ccw\n",
        (int)mode_idx, (int)numberOfAvailableModesRots, (int)numberOfAvailableModes, 
        (int)prop[propIndexRes+0], (int)prop[propIndexRes+1], (int)prop[propIndexRes+2], 
        (int)prop[propIndexRes+3], (int)prop[propIndexRes+4], (int)prop[propIndexRes+5], (int)prop[propIndexRes+6]);

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
 * Class:     jogamp_newt_driver_macosx_MacScreen
 * Method:    setScreenMode0
 * Signature: (II)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacScreen_setScreenMode0
  (JNIEnv *env, jobject object, jint scrn_idx, jint mode_idx)
{
    jboolean res = JNI_TRUE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSScreen *screen = NewtScreen_getNSScreenByIndex((int)scrn_idx);
    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);

    CFArrayRef availableModes = CGDisplayAvailableModes(display);
    CFIndex numberOfAvailableModes = CFArrayGetCount(availableModes);
    CFIndex numberOfAvailableModesRots = ROTMODES_PER_REALMODE * numberOfAvailableModes;

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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacWindow_initIDs0
  (JNIEnv *env, jclass clazz)
{
    static int initialized = 0;

    if(initialized) return JNI_TRUE;
    initialized = 1;

    jclass c;
    c = (*env)->FindClass(env, ClazzNamePoint);
    if(NULL==c) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't find %s", ClazzNamePoint);
    }
    pointClz = (jclass)(*env)->NewGlobalRef(env, c);
    (*env)->DeleteLocalRef(env, c);
    if(NULL==pointClz) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't use %s", ClazzNamePoint);
    }
    pointCstr = (*env)->GetMethodID(env, pointClz, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    if(NULL==pointCstr) {
        NewtCommon_FatalError(env, "FatalError Java_jogamp_newt_driver_macosx_MacWindow_initIDs0: can't fetch %s.%s %s",
            ClazzNamePoint, ClazzAnyCstrName, ClazzNamePointCstrSignature);
    }

    // Need this when debugging, as it is necessary to attach gdb to
    // the running java process -- "gdb java" doesn't work
    //    printf("Going to sleep for 10 seconds\n");
    //    sleep(10);

    return (jboolean) [NewtMacWindow initNatives: env forClass: clazz];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    createWindow0
 * Signature: (JIIIIZIIIJ)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_MacWindow_createWindow0
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
                                               screen: myScreen];
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

    // concurrent view rendering
    [myWindow setAllowsConcurrentViewDrawing: YES];
    [myView setCanDrawConcurrently: YES];

    // visible on front
    [myWindow orderFront: myWindow];

NS_DURING
    // Available >= 10.5 - Makes the menubar disapear
    if(fullscreen) {
         [myView enterFullScreenMode: myScreen withOptions:NULL];
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
// Footnote: Our view handling produces random 'Assertion failure' even w/o parenting:
//
// [NSThemeFrame lockFocus], /SourceCache/AppKit/AppKit-1138.23/AppKit.subproj/NSView.m:6053
// [NSThemeFrame(0x7fe94bc72c80) lockFocus] failed with window=0x7fe94bc445a0, windowNumber=9425, [self isHiddenOrHasHiddenAncestor]=0
//                       ..
// AppKit                0x00007fff89621001 -[NSView lockFocus] + 250
// AppKit                0x00007fff8961eafa -[NSView _displayRectIgnoringOpacity:isVisibleRect:rectIsVisibleRectForView:] + 3780
// AppKit                0x00007fff8961793e -[NSView displayIfNeeded] + 1676
// AppKit                0x00007fff8961707d _handleWindowNeedsDisplayOrLayoutOrUpdateConstraints + 648
//                       


/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NewtMacWindow* mWin = (NewtMacWindow*) ((intptr_t) window);
    NewtView* mView = (NewtView *)[mWin contentView];
    NSWindow* pWin = [mWin parentWindow];
    DBG_PRINT( "windowClose.0 - %p,%d view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), mView, getRetainCount(mView), pWin);

NS_DURING
    if(NULL!=mView) {
        // Available >= 10.5 - Makes the menubar disapear
        if([mView isInFullScreenMode]) {
            [mView exitFullScreenModeWithOptions: NULL];
        }
        // Note: mWin's release will also release it's mView!
        // [mWin setContentView: nil];
        // [mView release];
    }
NS_HANDLER
NS_ENDHANDLER

    if(NULL!=pWin) {
        [mWin detachFromParent: pWin];
    }
    [mWin orderOut: mWin];

    DBG_PRINT( "windowClose.1 - %p,%d view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), mView, getRetainCount(mView), pWin);

    // '[mWin close]' causes a crash at exit.
    // This probably happens b/c it sends events to the main loop
    // but our resources are gone ?!
    // However, issuing a simple release seems to work quite well.
    [mWin release];

    DBG_PRINT( "windowClose.X - %p,%d view %p,%d, parent %p\n", 
        mWin, getRetainCount(mWin), mView, getRetainCount(mView), pWin);

    [pool release];
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_MacWindow
 * Method:    lockSurface0
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacWindow_lockSurface0
  (JNIEnv *env, jclass clazz, jlong window)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    NewtView * mView = (NewtView *) [mWin contentView];
    return [mView softLock] == YES ? JNI_TRUE : JNI_FALSE;
    /** deadlocks, since we render independent of focus
    return [mView lockFocusIfCanDraw] == YES ? JNI_TRUE : JNI_FALSE; */
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_MacWindow
 * Method:    unlockSurface0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_unlockSurface0
  (JNIEnv *env, jclass clazz, jlong window)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    NewtView * mView = (NewtView *) [mWin contentView];
    [mView softUnlock];
    /** deadlocks, since we render independent of focus
    [mView unlockFocus]; */
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    requestFocus0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_requestFocus0
  (JNIEnv *env, jobject window, jlong w, jboolean force)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
#ifdef VERBOSE_ON
    BOOL hasFocus = [mWin isKeyWindow];
#endif

    DBG_PRINT( "requestFocus - window: %p, force %d, hasFocus %d (START)\n", mWin, force, hasFocus);

    [mWin makeFirstResponder: nil];
    // [mWin performSelectorOnMainThread:@selector(orderFrontRegardless) withObject:nil waitUntilDone:YES];
    // [mWin performSelectorOnMainThread:@selector(makeKeyWindow) withObject:nil waitUntilDone:YES];
    [mWin orderFrontRegardless];
    [mWin makeKeyWindow];

    DBG_PRINT( "requestFocus - window: %p, force %d (END)\n", mWin, force);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    requestFocusParent0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_requestFocusParent0
  (JNIEnv *env, jobject window, jlong w)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) w);
    NSWindow* pWin = [mWin parentWindow];
#ifdef VERBOSE_ON
    BOOL hasFocus = [mWin isKeyWindow];
#endif

    DBG_PRINT( "requestFocusParent0 - window: %p, parent: %p, hasFocus %d (START)\n", mWin, pWin, hasFocus );
    if(NULL != pWin) {
        [pWin makeKeyWindow];
    }
    DBG_PRINT( "requestFocusParent0 - window: %p, parent: %p (END)\n", mWin, pWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    orderFront0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_orderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "orderFront0 - window: %p (START)\n", win);

    [win orderFrontRegardless];

    DBG_PRINT( "orderFront0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    orderOut
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_orderOut0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) window);
    NSWindow* pWin = [mWin parentWindow];

    DBG_PRINT( "orderOut0 - window: (parent %p) %p (START)\n", pWin, mWin);

    if(NULL == pWin) {
        [mWin orderOut: mWin];
    } else {
        [mWin orderBack: mWin];
    }

    DBG_PRINT( "orderOut0 - window: (parent %p) %p (END)\n", pWin, mWin);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    setTitle0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_setTitle0
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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    contentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_MacWindow_contentView0
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

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    changeContentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_jogamp_newt_driver_macosx_MacWindow_changeContentView0
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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    setContentSize
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_setContentSize0
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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    setFrameTopLeftPoint
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_setFrameTopLeftPoint0
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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    setAlwaysOnTop0
 * Signature: (JZ)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_setAlwaysOnTop0
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
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    getLocationOnScreen0
 * Signature: (JII)Ljavax/media/nativewindow/util/Point;
 */
JNIEXPORT jobject JNICALL Java_jogamp_newt_driver_macosx_MacWindow_getLocationOnScreen0
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
 * Class:     Java_jogamp_newt_driver_macosx_MacWindow
 * Method:    setPointerVisible0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacWindow_setPointerVisible0
  (JNIEnv *env, jclass clazz, jlong window, jboolean mouseVisible)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMouseVisible: ( JNI_TRUE == mouseVisible ) ? YES : NO];
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_MacWindow
 * Method:    confinePointer0
 * Signature: (JZ)Z
 */
JNIEXPORT jboolean JNICALL Java_jogamp_newt_driver_macosx_MacWindow_confinePointer0
  (JNIEnv *env, jclass clazz, jlong window, jboolean confine)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMouseConfined: ( JNI_TRUE == confine ) ? YES : NO];
    return JNI_TRUE;
}

/*
 * Class:     Java_jogamp_newt_driver_macosx_MacWindow
 * Method:    warpPointer0
 * Signature: (JJII)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_warpPointer0
  (JNIEnv *env, jclass clazz, jlong window, jint x, jint y)
{
    NewtMacWindow *mWin = (NewtMacWindow*) ((intptr_t) window);
    [mWin setMousePosition: [mWin newtClientWinPos2OSXScreenPos: NSMakePoint(x, y)]];
}

