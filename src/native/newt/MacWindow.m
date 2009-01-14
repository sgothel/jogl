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

#import "com_sun_javafx_newt_macosx_MacWindow.h"
#import "NewtMacWindow.h"

#import "EventListener.h"
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

static BOOL initializedMenuHeight = NO;
static CGFloat menuHeight = 0;
static BOOL DEBUG = NO;

void setFrameTopLeftPoint(NSWindow* win, jint x, jint y)
{
    NSScreen* screen = [NSScreen mainScreen];
    NSRect screenRect = [screen frame];
    NSPoint pt;

    if (!initializedMenuHeight) {
        NSMenu* menu = [NSApp mainMenu];
        BOOL mustRelease = NO;

        if (menu == nil) {
            if (DEBUG) {
                printf("main menu was nil, trying services menu\n");
            }
            menu = [NSApp servicesMenu];
        }

        if (menu == nil) {
            if (DEBUG) {
                printf("services menu was nil, trying an empty menu instance\n");
            }
            menu = [[[NSMenu alloc] initWithTitle: @"Foo"] retain];
            mustRelease = YES;
        }

        menuHeight = [menu menuBarHeight];

        if (mustRelease) {
            [menu release];
        }

        initializedMenuHeight = YES;
    }

    pt = NSMakePoint(x, screenRect.origin.y + screenRect.size.height - menuHeight - y);
    [win setFrameTopLeftPoint: pt];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    initIDs
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_initIDs
  (JNIEnv *env, jclass clazz)
{
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

    return (jboolean) [NewtMacWindow initNatives: env forClass: clazz];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    createWindow
 * Signature: (IIIIIIZ)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_createWindow
  (JNIEnv *env, jobject jthis, jint x, jint y, jint w, jint h, jint styleMask, jint bufferingType, jboolean deferCreation)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSRect rect = NSMakeRect(x, y, w, h);
    jobject windowObj = (*env)->NewGlobalRef(env, jthis);

    // Allocate the window
    NSWindow* window = [[[NewtMacWindow alloc] initWithContentRect: rect
                                               styleMask: (NSUInteger) styleMask
                                               backing: (NSBackingStoreType) bufferingType
                                               defer: YES
                                               javaWindowObject: windowObj] retain];

    // If the window is undecorated, assume we want the possibility of
    // a shaped window, so make it non-opaque and the background color clear
    if ((styleMask & NSTitledWindowMask) == 0) {
        [window setOpaque: NO];
        [window setBackgroundColor: [NSColor clearColor]];
    }

    // Immediately re-position the window based on an upper-left coordinate system
    setFrameTopLeftPoint(window, x, y);

    // Allocate an NSView
    NSView* view = [[NSView alloc] initWithFrame: rect];

    // specify we want mouse-moved events
    [window setAcceptsMouseMovedEvents:YES];

    // Set the content view
    [window setContentView: view];

    [pool release];

    return (jlong) ((intptr_t) window);
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    makeKeyAndOrderFront
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_makeKeyAndOrderFront
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win makeKeyAndOrderFront: win];
    [pool release];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    orderOut
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_orderOut
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win orderOut: win];
    [pool release];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    close0
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_close0
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    [win close];
    [pool release];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    setTitle0
 * Signature: (JLjava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_setTitle0
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
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    dispatchMessages
 * Signature: (I)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_dispatchMessages
  (JNIEnv *env, jobject unused, jint eventMask)
{
    NSEvent* event = NULL;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    [NewtMacWindow setJNIEnv: env];
    do {
        // FIXME: ignoring event mask for the time being
        event = [NSApp nextEventMatchingMask: NSAnyEventMask
                       untilDate: [NSDate distantPast]
                       inMode: NSDefaultRunLoopMode
                       dequeue: YES];
        if (event != NULL) {
            [NSApp sendEvent: event];
        }
    } while (event != NULL);
    [NewtMacWindow setJNIEnv: NULL];
    [pool release];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    contentView
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_contentView
  (JNIEnv *env, jobject unused, jlong window)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    jlong res = (jlong) ((intptr_t) [win contentView]);
    [pool release];
    return res;
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    setContentSize
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_setContentSize
  (JNIEnv *env, jobject unused, jlong window, jint w, jint h)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    NSSize sz = NSMakeSize(w, h);
    [win setContentSize: sz];
    [pool release];
}

/*
 * Class:     com_sun_javafx_newt_macosx_MacWindow
 * Method:    setFrameTopLeftPoint
 * Signature: (JII)V
 */
JNIEXPORT void JNICALL Java_com_sun_javafx_newt_macosx_MacWindow_setFrameTopLeftPoint
  (JNIEnv *env, jobject unused, jlong window, jint x, jint y)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    NSWindow* win = (NSWindow*) ((intptr_t) window);
    setFrameTopLeftPoint(win, x, y);
    [pool release];
}
