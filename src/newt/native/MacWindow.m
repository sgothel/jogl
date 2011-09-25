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

static NSString* jstringToNSString(JNIEnv* env, jstring jstr)
{
    const jchar* jstrChars = (*env)->GetStringChars(env, jstr, NULL);
    NSString* str = [[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, jstr)];
    (*env)->ReleaseStringChars(env, jstr, jstrChars);
    return str;
}

static void setFrameTopLeftPoint(NSWindow* pWin, NSWindow* mWin, jint x, jint y) {
    NSScreen* screen = [NSScreen mainScreen];
    NSRect screenRect = [screen frame];

    DBG_PRINT( "setFrameTopLeftPoint screen %lf/%lf %lfx%lf\n", 
        screenRect.origin.x,
        screenRect.origin.y,
        screenRect.size.width,
        screenRect.size.height);

    NSPoint pt = NSMakePoint(screenRect.origin.x + x, screenRect.origin.y + screenRect.size.height - y);

    DBG_PRINT( "setFrameTopLeftPoint -> %lf/%lf\n", pt.x, pt.y);
    [mWin setFrameTopLeftPoint: pt];
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

    DBG_PRINT( "createWindow0 - %p (this), %p (parent), %d/%d %dx%d, opaque %d, fs %d, style %X, buffType %X, screenidx %d, view %p\n",
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
    NSWindow* myWindow = [[[NewtMacWindow alloc] initWithContentRect: rect
                                               styleMask: (NSUInteger) styleMask
                                               backing: (NSBackingStoreType) bufferingType
                                               screen: myScreen] retain];

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
    } else {
        [myWindow setOpaque: NO];
        [myWindow setBackgroundColor: [NSColor clearColor]];
    }

    /**
    if (fullscreen) {
        [myWindow setOpaque: YES];
    } else {
        // If the window is undecorated, assume we want the possibility of
        // a shaped window, so make it non-opaque and the background color clear
        if ((styleMask & NSTitledWindowMask) == 0) {
            [myWindow setOpaque: NO];
            [myWindow setBackgroundColor: [NSColor clearColor]];
        }
    } */

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
    [win performSelectorOnMainThread:@selector(makeKeyAndOrderFront:) withObject:win waitUntilDone:NO];
    // [win makeKeyAndOrderFront: win];
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
    [win performSelectorOnMainThread:@selector(makeKeyWindow:) withObject:nil waitUntilDone:NO];
    // [win makeKeyWindow];
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
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win performSelectorOnMainThread:@selector(orderOut:) withObject:win waitUntilDone:NO];
    // [win orderOut: win];
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
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSView* view = [win contentView];
    DBG_PRINT( "*************** windowClose.0: 0x%p\n", (void *)win);
NS_DURING
    if(NULL!=view) {
        // Available >= 10.5 - Makes the menubar disapear
        if([view isInFullScreenMode]) {
            [view exitFullScreenModeWithOptions: NULL];
        }
    }
NS_HANDLER
NS_ENDHANDLER
    DBG_PRINT( "*************** windowClose.2: 0x%p\n", (void *)win);

    [win performSelectorOnMainThread:@selector(close:) withObject:nil waitUntilDone:NO];
    // [win close]

    DBG_PRINT( "*************** windowClose.X: 0x%p\n", (void *)win);
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
    NSString* str = jstringToNSString(env, title);
    [str autorelease];
    [win performSelectorOnMainThread:@selector(setTitle:) withObject:str waitUntilDone:NO];
    // [win setTitle: str];
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
    jlong res = (jlong) ((intptr_t) [win contentView]);
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

    NSWindow* win = (NewtMacWindow*) ((intptr_t) window);
    NewtView* newView = (NewtView *) ((intptr_t) jview);

    NewtView* oldView = changeContentView(env, jthis, pWin, pView, win, newView);

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
    NSSize sz = NSMakeSize(w, h);
    [win setContentSize: sz];
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
    NSWindow* pwin = (NSWindow*) ((intptr_t) parent);
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    setFrameTopLeftPoint(pwin, win, x, y);
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
    if(atop) {
        [win setLevel:NSFloatingWindowLevel];
    } else {
        [win setLevel:NSNormalWindowLevel];
    }
    [pool release];
}


