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

#import "com_jogamp_newt_impl_macosx_MacWindow.h"
#import "NewtMacWindow.h"

#import "MouseEvent.h"
#import "KeyEvent.h"

#import <ApplicationServices/ApplicationServices.h>

#import <stdio.h>

NSString* jstringToNSString(JNIEnv* env, jstring jstr)
{
    const jchar* jstrChars = (*env)->GetStringChars(env, jstr, NULL);
    NSString* str = [[NSString alloc] initWithCharacters: jstrChars length: (*env)->GetStringLength(env, jstr)];
    (*env)->ReleaseStringChars(env, jstr, jstrChars);
    return str;
}

void setFrameTopLeftPoint(NSWindow* pwin, NSWindow* win, jint x, jint y)
{
    NSScreen* screen = [NSScreen mainScreen];

    // this allows for better compatibility with awt behavior
    NSRect visibleRect; // either screen or parent-window 
    NSPoint pt;
    int d_pty=0; // parent titlebar height
    int d_ptx=0; 
    
    if(NULL==pwin) {
        visibleRect = [screen frame];
    } else {
        visibleRect = [pwin frame];
        NSView* pview = [pwin contentView];
        NSRect viewRect = [pview frame];
        d_pty = visibleRect.size.height - viewRect.size.height;
        (void) d_ptx;
        //d_pty = visibleRect.origin.y - viewRect.size.height;
        //d_ptx = visibleRect.size.height - viewRect.size.height;
        fprintf(stderr, "pwin %lf/%lf %lfx%lf, pview %lf/%lf %lfx%lf -> %d/%d\n", 
            visibleRect.origin.x,
            visibleRect.origin.y,
            visibleRect.size.width,
            visibleRect.size.height, 
            viewRect.origin.x,
            viewRect.origin.y,
            viewRect.size.width,
            viewRect.size.height, 
            (int)x, (int)y);

    }

    pt = NSMakePoint(visibleRect.origin.x + x, visibleRect.origin.y + visibleRect.size.height - y - d_pty);

    [win setFrameTopLeftPoint: pt];
}

static NewtView * changeContentView(JNIEnv *env, jobject javaWindowObject, NSWindow *pwin, NSWindow *win, NewtView *newView) {
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
        }
        /** FIXME: Tried child window: auto clip or message reception ..
        if(NULL!=pwin) {
            [oldView removeFromSuperview];
        } */
    }
    if(NULL!=newView) {
        jobject globJavaWindowObject = (*env)->NewGlobalRef(env, javaWindowObject);
        [newView setJavaWindowObject: globJavaWindowObject];
        [newView setJNIEnv: env];

        /** FIXME: Tried child window: auto clip or message reception ..
        if(NULL!=pwin) {
            NSView* pview = [pwin contentView];
            [pview addSubview: newView];
        } */
    }
    [win setContentView: newView];

    // make sure the insets are updated in the java object
    NewtMacWindow* newtw = (NewtMacWindow*)win;
    [newtw updateInsets: env];

    return oldView;
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacDisplay
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_macosx_MacDisplay_initNSApplication0
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
 * Class:     com_jogamp_newt_impl_macosx_MacDisplay
 * Method:    dispatchMessages0
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacDisplay_dispatchMessages0
  (JNIEnv *env, jobject unused)
{
    NSEvent* event = NULL;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

NS_DURING

    int num_events = 0;

    // Periodically take a break
    do {
        // FIXME: ignoring event mask for the time being
        event = [NSApp nextEventMatchingMask: NSAnyEventMask
                       untilDate: [NSDate distantPast]
                       inMode: NSDefaultRunLoopMode
                       dequeue: YES];
        if (event != NULL) {
            [NSApp sendEvent: event];

            num_events++;
        }
    } while (num_events<100 && event != NULL);

NS_HANDLER
    
    // just ignore it ..

NS_ENDHANDLER

    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacScreen
 * Method:    getWidthImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_macosx_MacScreen_getWidthImpl0
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
 * Class:     com_jogamp_newt_impl_macosx_MacScreen
 * Method:    getHeightImpl
 * Signature: (I)I
 */
JNIEXPORT jint JNICALL Java_com_jogamp_newt_impl_macosx_MacScreen_getHeightImpl0
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
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_initIDs0
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
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    createWindow0
 * Signature: (JIIIIZIIIJ)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_createWindow0
  (JNIEnv *env, jobject jthis, jlong parent, jint x, jint y, jint w, jint h, jboolean fullscreen, jint styleMask, 
   jint bufferingType, jint screen_idx, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSRect rect = NSMakeRect(x, y, w, h);

    NSArray *screens = [NSScreen screens];
    if(screen_idx<0) screen_idx=0;
    if(screen_idx>=[screens count]) screen_idx=0;
    NSScreen *screen = (NSScreen *) [screens objectAtIndex: screen_idx];

    if (fullscreen) {
        styleMask = NSBorderlessWindowMask;
        NSRect rect = [screen frame];
        w = (jint) (rect.size.width);
        h = (jint) (rect.size.height);
    }

    // Allocate the window
    NSWindow* window = [[[NewtMacWindow alloc] initWithContentRect: rect
                                               styleMask: (NSUInteger) styleMask
                                               backing: (NSBackingStoreType) bufferingType
                                               screen: screen] retain];

    NSObject *nsParentObj = (NSObject*) ((intptr_t) parent);
    NSWindow* parentWindow = NULL;
    if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSWindow class]] ) {
        parentWindow = (NSWindow*) nsParentObj;
    } else if( nsParentObj != NULL && [nsParentObj isKindOfClass:[NSView class]] ) {
        NSView* view = (NSView*) nsParentObj;
        parentWindow = [view window];
        fprintf(stderr, "createWindow0 - Parent is NSView : %p -> %p (win) \n", nsParentObj, parentWindow);
    } else {
        fprintf(stderr, "createWindow0 - Parent is neither NSWindow nor NSView : %p\n", nsParentObj);
    }
    if(NULL!=parentWindow) {
        [parentWindow addChildWindow: window ordered: NSWindowAbove];
        [window setParentWindow: parentWindow];
    }

    if (fullscreen) {
        [window setOpaque: YES];
    } else {
        // If the window is undecorated, assume we want the possibility of
        // a shaped window, so make it non-opaque and the background color clear
        if ((styleMask & NSTitledWindowMask) == 0) {
            [window setOpaque: NO];
            [window setBackgroundColor: [NSColor clearColor]];
        }
    }

    // Immediately re-position the window based on an upper-left coordinate system
    setFrameTopLeftPoint(parentWindow, window, x, y);

    // specify we want mouse-moved events
    [window setAcceptsMouseMovedEvents:YES];

    // Use given NewtView or allocate an NewtView if NULL
    NewtView* view = (0==jview)? [[NewtView alloc] initWithFrame: rect] : (NewtView*) ((intptr_t) jview) ;

    // Set the content view
    (void) changeContentView(env, jthis, parentWindow, window, view);

NS_DURING
    // Available >= 10.5 - Makes the menubar disapear
    if(fullscreen) {
         [view enterFullScreenMode: screen withOptions:NULL];
    }
NS_HANDLER
NS_ENDHANDLER

    // Set the next responder to be the window so that we can forward
    // right mouse button down events
    [view setNextResponder: window];

    [pool release];

    return (jlong) ((intptr_t) window);
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    makeKeyAndOrderFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_makeKeyAndOrderFront0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win makeKeyAndOrderFront: win];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    makeKey
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_makeKey0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win makeKeyWindow];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    orderOut
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_orderOut0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win orderOut: win];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSView* view = [win contentView];
    [win orderOut: win];
NS_DURING
    if(NULL!=view) {
        // Available >= 10.5 - Makes the menubar disapear
        if([view isInFullScreenMode]) {
            [view exitFullScreenModeWithOptions: NULL];
        }
    }
NS_HANDLER
NS_ENDHANDLER
    [win close];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    setTitle0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_setTitle0
  (JNIEnv *env, jobject unused, jlong window, jstring title)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSString* str = jstringToNSString(env, title);
    [str autorelease];
    [win setTitle: str];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    contentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_contentView0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    jlong res = (jlong) ((intptr_t) [win contentView]);
    [pool release];
    return res;
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    changeContentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_changeContentView0
  (JNIEnv *env, jobject jthis, jlong parent, jlong window, jlong jview)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* pwin = (NewtMacWindow*) ((intptr_t) parent);
    NSWindow* win = (NewtMacWindow*) ((intptr_t) window);
    NewtView* newView = (NewtView *) ((intptr_t) jview);

    NewtView* oldView = changeContentView(env, jthis, pwin, win, newView);

    [pool release];

    return (jlong) ((intptr_t) oldView);
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    setContentSize
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_setContentSize0
  (JNIEnv *env, jobject unused, jlong window, jint w, jint h)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSSize sz = NSMakeSize(w, h);
    [win setContentSize: sz];
    [pool release];
}

/*
 * Class:     com_jogamp_newt_impl_macosx_MacWindow
 * Method:    setFrameTopLeftPoint
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_jogamp_newt_impl_macosx_MacWindow_setFrameTopLeftPoint0
  (JNIEnv *env, jobject unused, jlong parent, jlong window, jint x, jint y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* pwin = (NSWindow*) ((intptr_t) parent);
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    setFrameTopLeftPoint(pwin, win, x, y);
    [pool release];
}

