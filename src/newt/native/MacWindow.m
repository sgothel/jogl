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

    NSScreen* screen = [mWin screen];
    NSRect screenTotal = [screen frame];

    NSView* mView = [mWin contentView];
    NSRect mViewFrame = [mView frame]; 
    int totalHeight = mViewFrame.size.height + mWin->cachedInsets[2] + mWin->cachedInsets[3]; // height + insets[top+bottom]

    NSPoint pS = NSMakePoint(screenTotal.origin.x + x, screenTotal.origin.y + screenTotal.size.height - y - totalHeight);

#ifdef VERBOSE_ON
    NSMenu * menu = [NSApp mainMenu];
    int menuHeight = [NSMenu menuBarVisible] ? (int) [menu menuBarHeight] : 0;

    DBG_PRINT( "setFrameTopLeftPoint screen %lf/%lf %lfx%lf, menuHeight %d, win top-left %d/%d totalHeight %d -> scrn bottom-left %lf/%lf\n", 
        screenTotal.origin.x, screenTotal.origin.y, screenTotal.size.width, screenTotal.size.height, menuHeight,
        (int)x, (int)y, (int)totalHeight, pS.x, pS.y);

    if(NULL != pWin) {
        NSView* pView = [pWin contentView];
        NSRect pViewFrame = [pView frame]; 
        DBG_PRINT( "setFrameTopLeftPoint pViewFrame %lf/%lf %lfx%lf\n", 
            pViewFrame.origin.x, pViewFrame.origin.y, pViewFrame.size.width, pViewFrame.size.height);

        NSPoint pS0;
        pS0.x = 0; pS0.y = 0;
        // pS = [win convertRectToScreen: r]; // 10.7
        pS0 = [pWin convertBaseToScreen: pS0];
        DBG_PRINT( "setFrameTopLeftPoint (parent) base 0/0 -> screen: %lf/%lf\n", pS0.x, pS0.y);
    }
#endif

    [mWin setFrameOrigin: pS];
}

static NewtView * changeContentView(JNIEnv *env, jobject javaWindowObject, NSWindow *pwin, NSView *pview, NSWindow *win, NewtView *newView) {
    NSView* oldNSView = [win contentView];
    NewtView* oldView = NULL;

    if(NULL!=oldNSView) {
NS_DURING
        // Available >= 10.5 - Makes the menubar disapear
        if([oldNSView isInFullScreenMode]) {
            [oldNSView exitFullScreenModeWithOptions: NULL];
        }
NS_HANDLER
NS_ENDHANDLER
        if( [oldNSView isMemberOfClass:[NewtView class]] ) {
            oldView = (NewtView *) oldNSView;

            jobject globJavaWindowObject = [oldView getJavaWindowObject];
            (*env)->DeleteGlobalRef(env, globJavaWindowObject);
            [oldView setJavaWindowObject: NULL];
            [oldView setDestroyNotifySent: false];
        }
        /** FIXME: Tried child window: auto clip or message reception ..
        if(NULL!=pwin) {
            [oldView removeFromSuperview];
        } */
    }
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

        /** FIXME: Tried child window: auto clip or message reception ..
        if(NULL!=pwin) {
            [pview addSubview: newView positioned: NSWindowAbove relativeTo: nil];
        } */
    }
    [win setContentView: newView];

    // make sure the insets are updated in the java object
    NewtMacWindow* newtw = (NewtMacWindow*)win;
    [newtw updateInsets: env];

    return oldView;
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
    // NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    DBG_PRINT( "\nrunNSApplication0.0\n");

    [NSApp run];

    DBG_PRINT( "\nrunNSApplication0.X\n");
    // [pool release];
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

    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    NSScreen *screen = (NSScreen *) [screens objectAtIndex: screen_idx];
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

    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    NSScreen *screen = (NSScreen *) [screens objectAtIndex: screen_idx];
    NSRect rect = [screen frame];

    [pool release];

    return (jint) (rect.size.height);
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

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parent);
    NSWindow* parentWindow = NULL;
    NSView* parentView = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSWindow class]] ) {
        parentWindow = (NSWindow*) nsParentObj;
        parentView = [parentWindow contentView];
        DBG_PRINT( "createWindow0 - Parent is NSWindow : %p (view) -> %p (win) \n", parentView, parentWindow);
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSView class]] ) {
        parentView = (NSView*) nsParentObj;
        parentWindow = [parentView window];
        DBG_PRINT( "createWindow0 - Parent is NSView : %p -(view) > %p (win) \n", parentView, parentWindow);
    } else {
        DBG_PRINT( "createWindow0 - Parent is neither NSWindow nor NSView : %p\n", nsParentObj);
    }
    if(NULL!=parentWindow) {
        [parentWindow addChildWindow: myWindow ordered: NSWindowAbove];
        [myWindow setParentWindow: parentWindow];
    }

    if(opaque) {
        [myWindow setOpaque: YES];
        if (!fullscreen) {
            [myWindow setShowsResizeIndicator: YES];
        }
    } else {
        [myWindow setOpaque: NO];
        [myWindow setBackgroundColor: [NSColor clearColor]];
    }

    // specify we want mouse-moved events
    [myWindow setAcceptsMouseMovedEvents:YES];

    // Use given NewtView or allocate an NewtView if NULL
    if(NULL == myView) {
        myView = [[NewtView alloc] initWithFrame: rect] ;
        DBG_PRINT( "createWindow0 - new own view: %p\n", myView);
    }

    // Set the content view
    (void) changeContentView(env, jthis, parentWindow, parentView, myWindow, myView);

    // Immediately re-position the window based on an upper-left coordinate system
    setFrameTopLeftPoint(parentWindow, myWindow, x, y);

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

    DBG_PRINT( "createWindow0 - %p (this), %p (parent): new window: %p (END)\n",
        (void*)(intptr_t)jthis, (void*)(intptr_t)parent, myWindow);

    [pool release];

    return (jlong) ((intptr_t) myWindow);
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    makeKeyAndOrderFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_makeKeyAndOrderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "makeKeyAndOrderFront0 - window: %p (START)\n", win);

    // [win performSelectorOnMainThread:@selector(makeKeyAndOrderFront:) withObject:win waitUntilDone:YES];
    [win makeKeyAndOrderFront: win];

    DBG_PRINT( "makeKeyAndOrderFront0 - window: %p (END)\n", win);

    [pool release];
}

/*
 * Class:     jogamp_newt_driver_macosx_MacWindow
 * Method:    makeKey
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_makeKey0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);

    DBG_PRINT( "makeKey0 - window: %p (START)\n", win);

    // [win performSelectorOnMainThread:@selector(makeKeyWindow:) withObject:nil waitUntilDone:YES];
    [win makeKeyWindow];

    DBG_PRINT( "makeKey0 - window: %p (END)\n", win);

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

    // [mWin performSelectorOnMainThread:@selector(orderOut:) withObject:mWin waitUntilDone:NO];
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
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_jogamp_newt_driver_macosx_MacWindow_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* mWin = (NSWindow*) ((intptr_t) window);
    NSView* mView = [mWin contentView];
    NSWindow* pWin = [mWin parentWindow];
    DBG_PRINT( "*************** windowClose.0: %p (view %p, parent %p)\n", mWin, mView, pWin);
NS_DURING
    if(NULL!=mView) {
        // Available >= 10.5 - Makes the menubar disapear
        if([mView isInFullScreenMode]) {
            [mView exitFullScreenModeWithOptions: NULL];
        }
        [mWin setContentView: nil];
        [mView release];
    }
NS_HANDLER
NS_ENDHANDLER

    if(NULL!=pWin) {
        [mWin setParentWindow: nil];
        [pWin removeChildWindow: mWin];
    }
    [mWin orderOut: mWin];

    // [mWin performSelectorOnMainThread:@selector(close:) withObject:nil waitUntilDone:NO];
    [mWin close]; // performs release!

    DBG_PRINT( "*************** windowClose.X: %p (parent %p)\n", mWin, pWin);

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
    // [win performSelectorOnMainThread:@selector(setTitle:) withObject:str waitUntilDone:NO];
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

    NSWindow* win = (NewtMacWindow*) ((intptr_t) window);
    NewtView* newView = (NewtView *) ((intptr_t) jview);

    DBG_PRINT( "changeContentView0 - window: %p (START)\n", win);

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parentWindowOrView);
    NSWindow* pWin = NULL;
    NSView* pView = NULL;
    if( NULL != nsParentObj ) {
        if( [nsParentObj isKindOfClass:[NSWindow class]] ) {
            pWin = (NSWindow*) nsParentObj;
            pView = [pWin contentView];
        } else if( [nsParentObj isKindOfClass:[NSView class]] ) {
            pView = (NSView*) nsParentObj;
            pWin = [pView window];
        }
    }

    NewtView* oldView = changeContentView(env, jthis, pWin, pView, win, newView);

    DBG_PRINT( "changeContentView0 - window: %p (END)\n", win);

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

    NSPoint p0 = { src_x, src_y };
    p0 = [mWin getLocationOnScreen: p0];
    return (*env)->NewObject(env, pointClz, pointCstr, (jint)p0.x, (jint)p0.y);
}

