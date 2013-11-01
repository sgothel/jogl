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

#import "NewtMacWindow.h"
#import "InputEvent.h"
#import "KeyEvent.h"
#import "MouseEvent.h"

#include <CoreFoundation/CoreFoundation.h>
#include <Carbon/Carbon.h> /* For kVK_ constants, and TIS functions. */

#include <math.h>

static jfloat GetDelta(NSEvent *event, jint javaMods[]) {
    CGEventRef cgEvent = [event CGEvent];
    CGFloat deltaY = 0.0;
    CGFloat deltaX = 0.0;
    CGFloat delta = 0.0;

    if (CGEventGetIntegerValueField(cgEvent, kCGScrollWheelEventIsContinuous)) {
        // mouse pad case
        deltaX = CGEventGetIntegerValueField(cgEvent, kCGScrollWheelEventPointDeltaAxis2);
        deltaY = CGEventGetIntegerValueField(cgEvent, kCGScrollWheelEventPointDeltaAxis1);
        // fprintf(stderr, "WHEEL/PAD: %lf/%lf - 0x%X\n", (double)deltaX, (double)deltaY, javaMods[0]);
        if( fabsf(deltaX) > fabsf(deltaY) ) {
            javaMods[0] |= EVENT_SHIFT_MASK;
            delta = deltaX;
        } else {
            delta = deltaY;
        }
    } else {
        // traditional mouse wheel case
        deltaX = [event deltaX];
        deltaY = [event deltaY];
        // fprintf(stderr, "WHEEL/TRACK: %lf/%lf - 0x%X\n", (double)deltaX, (double)deltaY, javaMods[0]);
        if (deltaY == 0.0 && (javaMods[0] & EVENT_SHIFT_MASK) != 0) {
            // shift+vertical wheel scroll produces horizontal scroll
            // we convert it to vertical
            delta = deltaX;
        } else {
            delta = deltaY;
        }
        if (-1.0 < delta && delta < 1.0) {
            delta *= 10.0;
        } else {
            if (delta < 0.0) {
                delta = delta - 0.5f;
            } else {
                delta = delta + 0.5f;
            }
        }
    }
    // fprintf(stderr, "WHEEL/RES: %lf - 0x%X\n", (double)delta, javaMods[0]);
    return (jfloat) delta;
}

static jmethodID enqueueMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

static jmethodID insetsChangedID   = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID    = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;

// Need to enqueue all events to EDT,
// since we may operate on AWT-AppKit (Main Thread)
// and direct issuing 'requestFocus()' would deadlock:
//     AWT-AppKit
//     AWT-EventQueue-0

@implementation NewtView

- (id)initWithFrame:(NSRect)frameRect
{
    id res = [super initWithFrame:frameRect];
    javaWindowObject = NULL;

    jvmHandle = NULL;
    jvmVersion = 0;
    destroyNotifySent = NO;
    softLockCount = 0;

    pthread_mutexattr_t softLockSyncAttr;
    pthread_mutexattr_init(&softLockSyncAttr);
    pthread_mutexattr_settype(&softLockSyncAttr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&softLockSync, &softLockSyncAttr); // recursive

    ptrTrackingTag = 0;

    /**
    NSCursor crs = [NSCursor arrowCursor];
    NSImage crsImg = [crs image];
    NSPoint crsHot = [crs hotSpot];
    myCursor = [[NSCursor alloc] initWithImage: crsImg hotSpot:crsHot];
    */
    myCursor = NULL;

    DBG_PRINT("NewtView::create: %p (refcnt %d)\n", res, (int)[res retainCount]);
    return res;
}

#ifdef DBG_LIFECYCLE
- (void) release
{
    DBG_PRINT("NewtView::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    [super release];
}
#endif

- (void) dealloc
{
    DBG_PRINT("NewtView::dealloc.0: %p (refcnt %d), ptrTrackingTag %d\n", self, (int)[self retainCount], (int)ptrTrackingTag);
#ifdef DBG_LIFECYCLE
    NSLog(@"%@",[NSThread callStackSymbols]);
#endif
    if( 0 < softLockCount ) {
        NSLog(@"NewtView::dealloc: softLock still hold @ dealloc!\n");
    }
    if(0 != ptrTrackingTag) {
        // [self removeCursorRect: ptrRect cursor: myCursor];
        [self removeTrackingRect: ptrTrackingTag];
        ptrTrackingTag = 0;
    }
    pthread_mutex_destroy(&softLockSync);
    DBG_PRINT("NewtView::dealloc.X: %p\n", self);
    [super dealloc];
}

- (void) setJVMHandle: (JavaVM*) vm
{
    jvmHandle = vm;
}
- (JavaVM*) getJVMHandle
{
    return jvmHandle;
}

- (void) setJVMVersion: (int) ver
{
    jvmVersion = ver;
}

- (int) getJVMVersion
{
    return jvmVersion;
}

- (void) setJavaWindowObject: (jobject) javaWindowObj
{
    javaWindowObject = javaWindowObj;
}

- (jobject) getJavaWindowObject
{
    return javaWindowObject;
}

- (void) rightMouseDown: (NSEvent*) theEvent
{
    NSResponder* next = [self nextResponder];
    if (next != nil) {
        [next rightMouseDown: theEvent];
    }
}

- (void) resetCursorRects
{
    [super resetCursorRects];

    if(0 != ptrTrackingTag) {
        // [self removeCursorRect: ptrRect cursor: myCursor];
        [self removeTrackingRect: ptrTrackingTag];
        ptrTrackingTag = 0;
    }
    ptrRect = [self bounds]; 
    // [self addCursorRect: ptrRect cursor: myCursor];
    ptrTrackingTag = [self addTrackingRect: ptrRect owner: self userData: nil assumeInside: NO];
}

- (NSCursor *) cursor
{
    return myCursor;
}

- (void) setDestroyNotifySent: (BOOL) v
{
    destroyNotifySent = v;
}

- (BOOL) getDestroyNotifySent
{
    return destroyNotifySent;
}

- (BOOL) softLock
{
    // DBG_PRINT("*************** softLock.0: %p\n", (void*)pthread_self());
    int err;
    if( 0 != ( err = pthread_mutex_lock(&softLockSync) ) ) {
        NSLog(@"NewtView::softLock failed: errCode %d - %@", err, [NSThread callStackSymbols]);
        return NO;
    }
    softLockCount++;
    // DBG_PRINT("*************** softLock.X: %p\n", (void*)pthread_self());
    return 0 < softLockCount;
}

- (BOOL) softUnlock
{
    // DBG_PRINT("*************** softUnlock: %p\n", (void*)pthread_self());
    softLockCount--;
    int err;
    if( 0 != ( err = pthread_mutex_unlock(&softLockSync) ) ) {
        softLockCount++;
        NSLog(@"NewtView::softUnlock failed: Not locked by current thread - errCode %d -  %@", err, [NSThread callStackSymbols]);
        return NO;
    }
    return YES;
}

- (BOOL) needsDisplay
{
    return NO == destroyNotifySent && [super needsDisplay];
}

- (void) displayIfNeeded
{
    if( YES == [self needsDisplay] ) {
        [self softLock];
        [super displayIfNeeded];
        [self softUnlock];
    }
}

- (void) display
{
    if( NO == destroyNotifySent ) {
        [self softLock];
        [super display];
        [self softUnlock];
    }
}

- (void) drawRect:(NSRect)dirtyRect
{
    DBG_PRINT("*************** dirtyRect: %p %lf/%lf %lfx%lf\n", 
        javaWindowObject, dirtyRect.origin.x, dirtyRect.origin.y, dirtyRect.size.width, dirtyRect.size.height);

    if(NULL==javaWindowObject) {
        DBG_PRINT("drawRect: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, 1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("drawRect: null JNIEnv\n");
        return;
    }

    NSRect viewFrame = [self frame];

    (*env)->CallVoidMethod(env, javaWindowObject, windowRepaintID, JNI_TRUE, // defer ..
        dirtyRect.origin.x, viewFrame.size.height - dirtyRect.origin.y, 
        dirtyRect.size.width, dirtyRect.size.height);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */
}

- (void) viewDidHide
{
    if(NULL==javaWindowObject) {
        DBG_PRINT("viewDidHide: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, 1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidHide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_FALSE);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */

    [super viewDidHide];
}

- (void) viewDidUnhide
{
    if(NULL==javaWindowObject) {
        DBG_PRINT("viewDidUnhide: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, 1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidUnhide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_TRUE);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */

    [super viewDidUnhide];
}

- (BOOL) acceptsFirstResponder 
{
    return YES;
}

@end

static CFStringRef CKCH_CreateStringForKey(CGKeyCode keyCode, const UCKeyboardLayout *keyboardLayout) {
    UInt32 keysDown = 0;
    UniChar chars[4];
    UniCharCount realLength;

    UCKeyTranslate(keyboardLayout, keyCode,
                   kUCKeyActionDisplay, 0,
                   LMGetKbdType(), kUCKeyTranslateNoDeadKeysBit,
                   &keysDown, sizeof(chars) / sizeof(chars[0]), &realLength, chars);

    return CFStringCreateWithCharacters(kCFAllocatorDefault, chars, 1);
}

static CFMutableDictionaryRef CKCH_CreateCodeToCharDict(TISInputSourceRef keyboard) {
    CFDataRef layoutData = (CFDataRef) TISGetInputSourceProperty(keyboard, kTISPropertyUnicodeKeyLayoutData);
    const UCKeyboardLayout *keyboardLayout = (const UCKeyboardLayout *)CFDataGetBytePtr(layoutData);

    CFMutableDictionaryRef codeToCharDict = CFDictionaryCreateMutable(kCFAllocatorDefault, 128, NULL, NULL);
    if ( NULL != codeToCharDict ) {
        intptr_t i;
        for (i = 0; i < 128; ++i) {
            CFStringRef string = CKCH_CreateStringForKey((CGKeyCode)i, keyboardLayout);
            if( NULL != string ) {
                CFIndex stringLen = CFStringGetLength (string);
                if ( 0 < stringLen ) {
                    UniChar character = CFStringGetCharacterAtIndex(string, 0);
                    DBG_PRINT("CKCH: MAP 0x%X -> %c\n", (int)i, character);
                    CFDictionaryAddValue(codeToCharDict, (const void *)i, (const void *)(intptr_t)character);
                }
                CFRelease(string);
            }
        }
    }
    return codeToCharDict;
}

static CFMutableDictionaryRef CKCH_USCodeToNNChar = NULL;

static void CKCH_CreateDictionaries() {
    TISInputSourceRef currentKeyboard = TISCopyCurrentKeyboardInputSource();
    CKCH_USCodeToNNChar = CKCH_CreateCodeToCharDict(currentKeyboard);
    CFRelease(currentKeyboard);
}

static UniChar CKCH_CharForKeyCode(jshort keyCode) {
    UniChar rChar = 0;

    if ( NULL != CKCH_USCodeToNNChar ) {
        intptr_t code = (intptr_t) keyCode;
        intptr_t character = 0;

        if ( CFDictionaryGetValueIfPresent(CKCH_USCodeToNNChar, (void *)code, (const void **)&character) ) {
            rChar = (UniChar) character;
            DBG_PRINT("CKCH: OK 0x%X -> 0x%X\n", (int)keyCode, (int)rChar);
        }
    }
    return rChar;
}

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZSIIISF)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZSISCC)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged",     "(ZIIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "screenPositionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");
    if (enqueueMouseEventID && enqueueKeyEventID && sizeChangedID && visibleChangedID && insetsChangedID &&
        positionChangedID && focusChangedID && windowDestroyNotifyID && requestFocusID && windowRepaintID)
    {
        CKCH_CreateDictionaries();
        return YES;
    }
    return NO;
}

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       defer: (BOOL) deferCreation
       isFullscreenWindow:(BOOL)isfs
{
    id res = [super initWithContentRect: contentRect
                    styleMask: windowStyle
                    backing: bufferingType
                    defer: deferCreation];
    isFullscreenWindow = isfs;
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    [self setDelegate: self];
    cachedInsets[0] = 0; // l
    cachedInsets[1] = 0; // r
    cachedInsets[2] = 0; // t
    cachedInsets[3] = 0; // b
    modsDown[0] = NO; // shift
    modsDown[1] = NO; // ctrl
    modsDown[2] = NO; // alt
    modsDown[3] = NO; // win
    mouseConfined = NO;
    mouseVisible = YES;
    mouseInside = NO;
    cursorIsHidden = NO;
    realized = YES;
    DBG_PRINT("NewtWindow::create: %p, realized %d (refcnt %d)\n", res, realized, (int)[res retainCount]);
    return res;
}

#ifdef DBG_LIFECYCLE
- (void) release
{
    DBG_PRINT("NewtWindow::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"%@",[NSThread callStackSymbols]);
    [super release];
}
#endif

- (void) dealloc
{
    DBG_PRINT("NewtWindow::dealloc.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
#ifdef DBG_LIFECYCLE
    NSLog(@"%@",[NSThread callStackSymbols]);
#endif

    NewtView* mView = (NewtView *)[self contentView];
    if( NULL != mView ) {
        [mView release];
    }
    [super dealloc];
    DBG_PRINT("NewtWindow::dealloc.X: %p\n", self);
}

- (void) setRealized: (BOOL)v
{
    realized = v;
}

- (BOOL) isRealized
{
    return realized;
}

- (void) updateInsets: (JNIEnv*) env jwin: (jobject) javaWin
{
    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    // note: this is a simplistic implementation which doesn't take
    // into account DPI and scaling factor
    CGFloat l = contentRect.origin.x - frameRect.origin.x;
    cachedInsets[0] = (int)l;                                                     // l
    cachedInsets[1] = (int)(frameRect.size.width - (contentRect.size.width + l)); // r
    cachedInsets[2] = (jint)(frameRect.size.height - contentRect.size.height);    // t
    cachedInsets[3] = (jint)(contentRect.origin.y - frameRect.origin.y);          // b

    DBG_PRINT( "updateInsets: [ l %d, r %d, t %d, b %d ]\n", cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);

    if( NULL != env && NULL != javaWin ) {
        (*env)->CallVoidMethod(env, javaWin, insetsChangedID, JNI_FALSE, cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);
    }
}

- (void) attachToParent: (NSWindow*) parent
{
    DBG_PRINT( "attachToParent.1\n");
    [parent addChildWindow: self ordered: NSWindowAbove];
    DBG_PRINT( "attachToParent.2\n");
    [self setParentWindow: parent];
    DBG_PRINT( "attachToParent.X\n");
}

- (void) detachFromParent: (NSWindow*) parent
{
    DBG_PRINT( "detachFromParent.1\n");
    [self setParentWindow: nil];
    if(NULL != parent) {
        DBG_PRINT( "detachFromParent.2\n");
        [parent removeChildWindow: self];
    }
    DBG_PRINT( "detachFromParent.X\n");
}

/**
 * p abs screen position of client-area pos w/ top-left origin, using contentView's client NSSize
 * returns: abs screen position w/ bottom-left origin
 */
- (NSPoint) newtAbsClientTLWinPos2AbsBLScreenPos: (NSPoint) p
{
    NSView* mView = [self contentView];
    NSRect mViewFrame = [mView frame]; 
    return [self newtAbsClientTLWinPos2AbsBLScreenPos: p size: mViewFrame.size];
}

/**
 * p abs screen position of client-area pos w/ top-left origin, using given client NSSize
 * returns: abs screen position w/ bottom-left origin
 */
- (NSPoint) newtAbsClientTLWinPos2AbsBLScreenPos: (NSPoint) p size: (NSSize) nsz
{
    int totalHeight = nsz.height + cachedInsets[3]; // height + insets.bottom

    DBG_PRINT( "newtAbsClientTLWinPos2AbsBLScreenPos: given %d/%d %dx%d, insets bottom %d -> totalHeight %d\n", 
        (int)p.x, (int)p.y, (int)nsz.width, (int)nsz.height, cachedInsets[3], totalHeight);

    NSScreen* screen = [self screen];
    NSRect screenFrame = [screen frame];

    DBG_PRINT( "newtAbsClientTLWinPos2AbsBLScreenPos: screen %d/%d %dx%d\n", 
        (int)screenFrame.origin.x, (int)screenFrame.origin.y, (int)screenFrame.size.width, (int)screenFrame.size.height);

    NSPoint r = NSMakePoint(screenFrame.origin.x + p.x,
                            screenFrame.origin.y + screenFrame.size.height - p.y - totalHeight);

    DBG_PRINT( "newtAbsClientTLWinPos2AbsBLScreenPos: result %d/%d\n", (int)r.x, (int)r.y); 

    return r;
}

/**
 * p rel client window position w/ top-left origin
 * returns: abs screen position w/ bottom-left origin
 */
- (NSPoint) newtRelClientTLWinPos2AbsBLScreenPos: (NSPoint) p
{
    NSRect winFrame = [self frame];

    NSView* mView = [self contentView];
    NSRect mViewFrame = [mView frame]; 

    return NSMakePoint(winFrame.origin.x + p.x,
                       winFrame.origin.y + ( mViewFrame.size.height - p.y ) ); // y-flip in view
}

- (NSSize) newtClientSize2TLSize: (NSSize) nsz
{
    NSSize topSZ = { nsz.width, nsz.height + cachedInsets[2] + cachedInsets[3] }; // height + insets.top + insets.bottom
    return topSZ;
}

/**
 * y-flips input / output
 * p rel client window position w/ top-left origin
 * returns: location in 0/0 top-left space.
 */
- (NSPoint) getLocationOnScreen: (NSPoint) p
{
    NSScreen* screen = [self screen];
    NSRect screenRect = [screen frame];

    NSView* view = [self contentView];
    NSRect viewFrame = [view frame];

    NSRect r;
    r.origin.x = p.x;
    r.origin.y = viewFrame.size.height - p.y; // y-flip
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectToScreen: r]; // 10.7
    NSPoint oS = [self convertBaseToScreen: r.origin];
    oS.y = screenRect.origin.y + screenRect.size.height - oS.y;
    return oS;
}

- (NSPoint) screenPos2NewtClientWinPos: (NSPoint) p
{
    NSView* view = [self contentView];
    NSRect viewFrame = [view frame];

    NSRect r;
    r.origin.x = p.x;
    r.origin.y = p.y;
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectFromScreen: r]; // 10.7
    NSPoint oS = [self convertScreenToBase: r.origin];
    oS.y = viewFrame.size.height - oS.y; // y-flip
    return oS;
}

- (BOOL) isMouseInside
{
    NSView* view = [self contentView];
    NSRect viewFrame = [view frame];
    NSPoint l1 = [NSEvent mouseLocation];
    NSPoint l0 = [self screenPos2NewtClientWinPos: l1];
    return viewFrame.origin.x <= l0.x && l0.x < (viewFrame.origin.x+viewFrame.size.width) &&
           viewFrame.origin.y <= l0.y && l0.y < (viewFrame.origin.y+viewFrame.size.height) ;
}

- (void) setMouseVisible:(BOOL)v hasFocus:(BOOL)focus
{
    mouseVisible = v;
    mouseInside = [self isMouseInside];
    DBG_PRINT( "setMouseVisible: confined %d, visible %d (current: %d), mouseInside %d, hasFocus %d\n", 
        mouseConfined, mouseVisible, !cursorIsHidden, mouseInside, focus);
    if(YES == focus && YES == mouseInside) {
        [self cursorHide: !mouseVisible];
    }
}

- (void) cursorHide:(BOOL)v
{
    DBG_PRINT( "cursorHide: %d -> %d\n", cursorIsHidden, v);
    if(v) {
        if(!cursorIsHidden) {
            [NSCursor hide];
            cursorIsHidden = YES;
        }
    } else {
        if(cursorIsHidden) {
            [NSCursor unhide];
            cursorIsHidden = NO;
        }
    }
}

- (void) setMouseConfined:(BOOL)v
{
    mouseConfined = v;
    DBG_PRINT( "setMouseConfined: confined %d, visible %d\n", mouseConfined, mouseVisible);
}

- (void) setMousePosition:(NSPoint)p
{
    NSScreen* screen = [self screen];
    NSRect screenRect = [screen frame];

    CGPoint pt = { p.x, screenRect.size.height - p.y }; // y-flip (CG is top-left origin)
    CGEventRef ev = CGEventCreateMouseEvent (NULL, kCGEventMouseMoved, pt, kCGMouseButtonLeft);
    CGEventPost (kCGHIDEventTap, ev);
}

static jint mods2JavaMods(NSUInteger mods)
{
    int javaMods = 0;
    if (mods & NSShiftKeyMask) {
        javaMods |= EVENT_SHIFT_MASK;
    }
    if (mods & NSControlKeyMask) {
        javaMods |= EVENT_CTRL_MASK;
    }
    if (mods & NSCommandKeyMask) {
        javaMods |= EVENT_META_MASK;
    }
    if (mods & NSAlternateKeyMask) {
        javaMods |= EVENT_ALT_MASK;
    }
    return javaMods;
}

- (void) sendKeyEvent: (NSEvent*) event eventType: (jshort) evType
{
    jshort keyCode = (jshort) [event keyCode];
    NSString* chars = [event charactersIgnoringModifiers];
    NSUInteger mods = [event modifierFlags];
    [self sendKeyEvent: keyCode characters: chars modifiers: mods eventType: evType];
}

- (void) sendKeyEvent: (jshort) keyCode characters: (NSString*) chars modifiers: (NSUInteger)mods eventType: (jshort) evType
{
    NSView* nsview = [self contentView];
    if( ! [nsview isKindOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("sendKeyEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env;
    if( NULL != jvmHandle ) {
        env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
    } else {
        env = NULL;
    }
    if(NULL==env) {
        DBG_PRINT("sendKeyEvent: JVM %p JNIEnv %p\n", jvmHandle, env);
        return;
    }

    int i;
    int len = NULL != chars ? [chars length] : 0;
    jint javaMods = mods2JavaMods(mods);

    if(len > 0) {
        // printable chars
        for (i = 0; i < len; i++) {
            // Note: the key code in the NSEvent does not map to anything we can use
            UniChar keyChar = (UniChar) [chars characterAtIndex: i];
            UniChar keySymChar = CKCH_CharForKeyCode(keyCode);

            DBG_PRINT("sendKeyEvent: %d/%d code 0x%X, char 0x%X -> keySymChar 0x%X\n", i, len, (int)keyCode, (int)keyChar, (int)keySymChar);

            (*env)->CallVoidMethod(env, javaWindowObject, enqueueKeyEventID, JNI_FALSE,
                                   evType, javaMods, keyCode, (jchar)keyChar, (jchar)keySymChar);
        }
    } else {
        // non-printable chars
        jchar keyChar = (jchar) 0;

        DBG_PRINT("sendKeyEvent: code 0x%X\n", (int)keyCode);

        (*env)->CallVoidMethod(env, javaWindowObject, enqueueKeyEventID, JNI_FALSE,
                               evType, javaMods, keyCode, keyChar, keyChar);
    }

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */
}

- (void) sendMouseEvent: (NSEvent*) event eventType: (jshort) evType
{
    NSView* nsview = [self contentView];
    if( ! [nsview isKindOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("sendMouseEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env;
    if( NULL != jvmHandle ) {
        env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
    } else {
        env = NULL;
    }
    if(NULL==env) {
        DBG_PRINT("sendMouseEvent: JVM %p JNIEnv %p\n", jvmHandle, env);
        return;
    }
    jint javaMods[] = { 0 } ;
    javaMods[0] = mods2JavaMods([event modifierFlags]);

    // convert to 1-based button number (or use zero if no button is involved)
    // TODO: detect mouse button when mouse wheel scrolled  
    jshort javaButtonNum = 0;
    jfloat scrollDeltaY = 0.0f;
    switch ([event type]) {
    case NSScrollWheel: {
        scrollDeltaY = GetDelta(event, javaMods);
        javaButtonNum = 1;
        break;
    }
    case NSLeftMouseDown:
    case NSLeftMouseUp:
    case NSLeftMouseDragged:
        javaButtonNum = 1;
        break;
    case NSRightMouseDown:
    case NSRightMouseUp:
    case NSRightMouseDragged:
        javaButtonNum = 3;
        break;
    case NSOtherMouseDown:
    case NSOtherMouseUp:
    case NSOtherMouseDragged:
        javaButtonNum = 2;
        break;
    }

    if (evType == EVENT_MOUSE_WHEEL_MOVED && scrollDeltaY == 0) {
        // ignore 0 increment wheel scroll events
        return;
    }
    if (evType == EVENT_MOUSE_PRESSED) {
        (*env)->CallVoidMethod(env, javaWindowObject, requestFocusID, JNI_FALSE);
    }

    NSPoint location = [self screenPos2NewtClientWinPos: [NSEvent mouseLocation]];

    (*env)->CallVoidMethod(env, javaWindowObject, enqueueMouseEventID, JNI_FALSE,
                           evType, javaMods[0],
                           (jint) location.x, (jint) location.y,
                           javaButtonNum, scrollDeltaY);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */
}

- (void) focusChanged: (BOOL) gained
{
    DBG_PRINT( "focusChanged: gained %d\n", gained);
    NSView* nsview = [self contentView];
    if( ! [nsview isKindOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("focusChanged: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env;
    if( NULL != jvmHandle ) {
        env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
    } else {
        env = NULL;
    }
    if(NULL==env) {
        DBG_PRINT("focusChanged: JVM %p JNIEnv %p\n", jvmHandle, env);
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_FALSE, (gained == YES) ? JNI_TRUE : JNI_FALSE);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */
}

- (BOOL) becomeFirstResponder
{
    DBG_PRINT( "*************** becomeFirstResponder\n");
    return [super becomeFirstResponder];
}

- (BOOL) resignFirstResponder
{
    DBG_PRINT( "*************** resignFirstResponder\n");
    return [super resignFirstResponder];
}

- (BOOL) canBecomeKeyWindow
{
    // Even if the window is borderless, we still want it to be able
    // to become the key window to receive keyboard events
    return YES;
}

- (void) becomeKeyWindow
{
    DBG_PRINT( "*************** becomeKeyWindow\n");
    [super becomeKeyWindow];
}

- (void) resignKeyWindow
{
    DBG_PRINT( "*************** resignKeyWindow: isFullscreen %d\n", (int)isFullscreenWindow);
    if(!isFullscreenWindow) {
        [super resignKeyWindow];
    }
}

- (void) windowDidBecomeKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidBecomeKey\n");
    mouseInside = [self isMouseInside];
    if(YES == mouseInside) {
        [self cursorHide: !mouseVisible];
    }
    [self focusChanged: YES];
}

- (void) windowDidResignKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidResignKey\n");
    // Implicit mouse exit by OS X
    [self focusChanged: NO];
}

- (void) keyDown: (NSEvent*) theEvent
{
    [self sendKeyEvent: theEvent eventType: (jshort)EVENT_KEY_PRESSED];
}

- (void) keyUp: (NSEvent*) theEvent
{
    [self sendKeyEvent: theEvent eventType: (jshort)EVENT_KEY_RELEASED];
}

#define kVK_Shift     0x38
#define kVK_Option    0x3A
#define kVK_Control   0x3B
#define kVK_Command   0x37

- (void) handleFlagsChanged:(int) keyMask keyIndex: (int) keyIdx keyCode: (int) keyCode modifiers: (NSUInteger) mods
{
    if ( NO == modsDown[keyIdx] && 0 != ( mods & keyMask ) )  {
        modsDown[keyIdx] = YES;
        [self sendKeyEvent: (jshort)keyCode characters: NULL modifiers: mods|keyMask eventType: (jshort)EVENT_KEY_PRESSED];
    } else if ( YES == modsDown[keyIdx] && 0 == ( mods & keyMask ) )  {
        modsDown[keyIdx] = NO;
        [self sendKeyEvent: (jshort)keyCode characters: NULL modifiers: mods|keyMask eventType: (jshort)EVENT_KEY_RELEASED];
    }
}

- (void) flagsChanged:(NSEvent *) theEvent
{
    NSUInteger mods = [theEvent modifierFlags];

    // BOOL modsDown[4]; // shift, ctrl, alt/option, win/command

    [self handleFlagsChanged: NSShiftKeyMask keyIndex: 0 keyCode: kVK_Shift modifiers: mods];
    [self handleFlagsChanged: NSControlKeyMask keyIndex: 1 keyCode: kVK_Control modifiers: mods];
    [self handleFlagsChanged: NSAlternateKeyMask keyIndex: 2 keyCode: kVK_Option modifiers: mods];
    [self handleFlagsChanged: NSCommandKeyMask keyIndex: 3 keyCode: kVK_Command modifiers: mods];
}

- (void) mouseEntered: (NSEvent*) theEvent
{
    DBG_PRINT( "mouseEntered: confined %d, visible %d\n", mouseConfined, mouseVisible);
    mouseInside = YES;
    [self cursorHide: !mouseVisible];
    if(NO == mouseConfined) {
        [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_ENTERED];
    }
}

- (void) mouseExited: (NSEvent*) theEvent
{
    DBG_PRINT( "mouseExited: confined %d, visible %d\n", mouseConfined, mouseVisible);
    if(NO == mouseConfined) {
        mouseInside = NO;
        [self cursorHide: NO];
        [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_EXITED];
    } else {
        [self setMousePosition: lastInsideMousePosition];
    }
}

- (void) mouseMoved: (NSEvent*) theEvent
{
    lastInsideMousePosition = [NSEvent mouseLocation];
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_MOVED];
}

- (void) scrollWheel: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_WHEEL_MOVED];
}

- (void) mouseDown: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_PRESSED];
}

- (void) mouseDragged: (NSEvent*) theEvent
{
    lastInsideMousePosition = [NSEvent mouseLocation];
    // Note use of MOUSE_MOVED event type because mouse dragged events are synthesized by Java
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_MOVED];
}

- (void) mouseUp: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_RELEASED];
}

- (void) rightMouseDown: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_PRESSED];
}

- (void) rightMouseDragged: (NSEvent*) theEvent
{
    lastInsideMousePosition = [NSEvent mouseLocation];
    // Note use of MOUSE_MOVED event type because mouse dragged events are synthesized by Java
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_MOVED];
}

- (void) rightMouseUp: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_RELEASED];
}

- (void) otherMouseDown: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_PRESSED];
}

- (void) otherMouseDragged: (NSEvent*) theEvent
{
    lastInsideMousePosition = [NSEvent mouseLocation];
    // Note use of MOUSE_MOVED event type because mouse dragged events are synthesized by Java
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_MOVED];
}

- (void) otherMouseUp: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_RELEASED];
}

- (void)windowDidResize: (NSNotification*) notification
{
    JNIEnv* env = NULL;
    jobject javaWindowObject = NULL;
    int shallBeDetached = 0;
    JavaVM *jvmHandle = NULL;

    NSView* nsview = [self contentView];
    if( [nsview isKindOfClass:[NewtView class]] ) {
        NewtView* view = (NewtView *) nsview;
        javaWindowObject = [view getJavaWindowObject];
        if (javaWindowObject != NULL) {
            jvmHandle = [view getJVMHandle];
            if( NULL != jvmHandle ) {
                env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
            }
        }
    }

    // update insets on every window resize for lack of better hook place
    [self updateInsets: env jwin:javaWindowObject];

    if( NULL != env && NULL != javaWindowObject ) {
        NSRect frameRect = [self frame];
        NSRect contentRect = [self contentRectForFrameRect: frameRect];

        (*env)->CallVoidMethod(env, javaWindowObject, sizeChangedID, JNI_FALSE,
                               (jint) contentRect.size.width,
                               (jint) contentRect.size.height, JNI_FALSE);

        /* if (shallBeDetached) {
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        } */
    }
}

- (void)windowDidMove: (NSNotification*) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isKindOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("windowDidMove: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env;
    if( NULL != jvmHandle ) {
        env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
    } else {
        env = NULL;
    }
    if(NULL==env) {
        DBG_PRINT("windowDidMove: JVM %p JNIEnv %p\n", jvmHandle, env);
        return;
    }

    NSPoint p0 = { 0, 0 };
    p0 = [self getLocationOnScreen: p0];
    (*env)->CallVoidMethod(env, javaWindowObject, positionChangedID, JNI_FALSE, (jint) p0.x, (jint) p0.y);

    /* if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    } */
}

- (BOOL)windowShouldClose: (id) sender
{
    return [self windowClosingImpl: NO];
}

- (void)windowWillClose: (NSNotification*) notification
{
    [self windowClosingImpl: YES];
}

- (BOOL) windowClosingImpl: (BOOL) force
{
    jboolean closed = JNI_FALSE;
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    [self cursorHide: NO];

    NSView* nsview = [self contentView];
    if( ! [nsview isKindOfClass:[NewtView class]] ) {
        return NO;
    }
    NewtView* view = (NewtView *) nsview;

    if( false == [view getDestroyNotifySent] ) {
        jobject javaWindowObject = [view getJavaWindowObject];
        DBG_PRINT( "*************** windowWillClose.0: %p\n", (void *)(intptr_t)javaWindowObject);
        if (javaWindowObject == NULL) {
            DBG_PRINT("windowWillClose: null javaWindowObject\n");
            return NO;
        }
        int shallBeDetached = 0;
        JavaVM *jvmHandle = [view getJVMHandle];
        JNIEnv* env = NULL;
NS_DURING
        if( NULL != jvmHandle ) {
            env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], 1 /* asDaemon */, &shallBeDetached);
        }
NS_HANDLER
        jvmHandle = NULL;
        env = NULL;
        [view setJVMHandle: NULL];
        DBG_PRINT("windowWillClose: JVMHandler Exception\n");
NS_ENDHANDLER
        DBG_PRINT("windowWillClose: JVM %p JNIEnv %p\n", jvmHandle, env);
        if(NULL==env) {
            return NO;
        }

        [view setDestroyNotifySent: true]; // earmark assumption of being closed
        closed = (*env)->CallBooleanMethod(env, javaWindowObject, windowDestroyNotifyID, force ? JNI_TRUE : JNI_FALSE);
        if(!force && !closed) {
            // not closed on java side, not force -> clear flag
            [view setDestroyNotifySent: false];
        }

        /* if (shallBeDetached) {
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        } */
        DBG_PRINT( "*************** windowWillClose.X: %p, closed %d\n", (void *)(intptr_t)javaWindowObject, (int)closed);
    } else {
        DBG_PRINT( "*************** windowWillClose (skip)\n");
    }
    [pool release];
    return JNI_TRUE == closed ? YES : NO ;
}

@end

