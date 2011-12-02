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

jint GetDeltaY(NSEvent *event, jint javaMods) {
    CGFloat deltaY = 0.0;
    CGEventRef cgEvent = [event CGEvent];

    if (CGEventGetIntegerValueField(cgEvent, kCGScrollWheelEventIsContinuous)) {
        // mouse pad case
        deltaY =
            CGEventGetIntegerValueField(cgEvent, kCGScrollWheelEventPointDeltaAxis1);
        // fprintf(stderr, "WHEEL/PAD: %lf\n", (double)deltaY);
    } else {
        // traditional mouse wheel case
        deltaY = [event deltaY];
        // fprintf(stderr, "WHEEL/TRAD: %lf\n", (double)deltaY);
        if (deltaY == 0.0 && (javaMods & EVENT_SHIFT_MASK) != 0) {
            // shift+vertical wheel scroll produces horizontal scroll
            // we convert it to vertical
            deltaY = [event deltaX];
        }
        if (-1.0 < deltaY && deltaY < 1.0) {
            deltaY *= 10.0;
        } else {
            if (deltaY < 0.0) {
                deltaY = deltaY - 0.5f;
            } else {
                deltaY = deltaY + 0.5f;
            }
        }
    }
    // fprintf(stderr, "WHEEL/res: %d\n", (int)deltaY);
    return (jint) deltaY;
}

static jmethodID enqueueMouseEventID = NULL;
static jmethodID sendMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID sendKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

static jmethodID insetsChangedID   = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID    = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowRepaintID = NULL;

// Can't use USE_SENDIO_DIRECT, ie w/o enqueueing to EDT,
// since we may operate on AWT-AppKit (Main Thread)
// and direct issuing 'requestFocus()' would deadlock:
//     AWT-AppKit
//     AWT-EventQueue-0
//
// #define USE_SENDIO_DIRECT 1

@implementation NewtView

- (id)initWithFrame:(NSRect)frameRect
{
    javaWindowObject = NULL;

    jvmHandle = NULL;
    jvmVersion = 0;
    destroyNotifySent = NO;
    softLocked = NO;

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

    return [super initWithFrame:frameRect];
}

- (void) dealloc
{
    if(softLocked) {
        NSLog(@"NewtView::dealloc: softLock still hold @ dealloc!\n");
    }
    pthread_mutex_destroy(&softLockSync);
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
    // NSLog(@"NewtView::softLock: %@",[NSThread callStackSymbols]);
    pthread_mutex_lock(&softLockSync);
    softLocked = YES;
    // DBG_PRINT("*************** softLock.X: %p\n", (void*)pthread_self());
    return softLocked;
}

- (void) softUnlock
{
    // DBG_PRINT("*************** softUnlock: %p\n", (void*)pthread_self());
    softLocked = NO;
    pthread_mutex_unlock(&softLockSync);
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

    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidHide: null JNIEnv\n");
        return;
    }

    NSRect viewFrame = [self frame];

    (*env)->CallVoidMethod(env, javaWindowObject, windowRepaintID, JNI_TRUE, // defer ..
        dirtyRect.origin.x, viewFrame.size.height - dirtyRect.origin.y, 
        dirtyRect.size.width, dirtyRect.size.height);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void) viewDidHide
{
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidHide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_FALSE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }

    [super viewDidHide];
}

- (void) viewDidUnhide
{
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidHide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_TRUE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }

    [super viewDidUnhide];
}

- (BOOL) acceptsFirstResponder 
{
    return YES;
}

@end

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZIIIIII)V");
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZIIIC)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged",     "(ZIIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID    = (*env)->GetMethodID(env, clazz, "windowDestroyNotify",    "()V");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");
    if (enqueueMouseEventID && sendMouseEventID && enqueueKeyEventID && sendKeyEventID && sizeChangedID && visibleChangedID && insetsChangedID &&
        positionChangedID && focusChangedID && windowDestroyNotifyID && requestFocusID && windowRepaintID)
    {
        return YES;
    }
    return NO;
}

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       defer: (BOOL) deferCreation
       screen:(NSScreen *)screen
{
    id res = [super initWithContentRect: contentRect
                    styleMask: windowStyle
                    backing: bufferingType
                    defer: deferCreation
                    screen: screen];
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    [self setDelegate: self];
    cachedInsets[0] = 0; // l
    cachedInsets[1] = 0; // r
    cachedInsets[2] = 0; // t
    cachedInsets[3] = 0; // b
    mouseConfined = NO;
    mouseVisible = YES;
    mouseInside = NO;
    cursorIsHidden = NO;
    return res;
}

- (void) updateInsets: (JNIEnv*) env
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }
    
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

    (*env)->CallVoidMethod(env, javaWindowObject, insetsChangedID, JNI_FALSE, cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);
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
    DBG_PRINT( "detachFromParent.2\n");
    [parent removeChildWindow: self];
    DBG_PRINT( "detachFromParent.X\n");
}

/**
 * p abs screen position w/ top-left origin
 * returns: abs screen position w/ bottom-left origin
 */
- (NSPoint) newtScreenWinPos2OSXScreenPos: (NSPoint) p
{
    NSView* mView = [self contentView];
    NSRect mViewFrame = [mView frame]; 
    int totalHeight = mViewFrame.size.height + cachedInsets[2] + cachedInsets[3]; // height + insets[top+bottom]

    NSScreen* screen = [self screen];
    NSRect screenFrame = [screen frame];

    return NSMakePoint(screenFrame.origin.x + p.x + cachedInsets[0],
                       screenFrame.origin.y + screenFrame.size.height - p.y - totalHeight);
}

/**
 * p rel client window position w/ top-left origin
 * returns: abs screen position w/ bottom-left origin
 */
- (NSPoint) newtClientWinPos2OSXScreenPos: (NSPoint) p
{
    NSRect winFrame = [self frame];

    NSView* mView = [self contentView];
    NSRect mViewFrame = [mView frame]; 

    return NSMakePoint(winFrame.origin.x + p.x,
                       winFrame.origin.y + ( mViewFrame.size.height - p.y ) ); // y-flip in view
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

- (BOOL) canBecomeKeyWindow
{
    // Even if the window is borderless, we still want it to be able
    // to become the key window to receive keyboard events
    return YES;
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

- (void) sendKeyEvent: (NSEvent*) event eventType: (jint) evType
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
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
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("sendKeyEvent: null JNIEnv\n");
        return;
    }

    int i;
    jint keyCode = (jint) [event keyCode];
    NSString* chars = [event charactersIgnoringModifiers];
    int len = [chars length];
    jint javaMods = mods2JavaMods([event modifierFlags]);

    for (i = 0; i < len; i++) {
        // Note: the key code in the NSEvent does not map to anything we can use
        jchar keyChar = (jchar) [chars characterAtIndex: i];

        DBG_PRINT("sendKeyEvent: %d/%d char 0x%X, code 0x%X\n", i, len, (int)keyChar, (int)keyCode);

        #ifdef USE_SENDIO_DIRECT
        (*env)->CallVoidMethod(env, javaWindowObject, sendKeyEventID,
                               evType, javaMods, keyCode, keyChar);
        #else
        (*env)->CallVoidMethod(env, javaWindowObject, enqueueKeyEventID, JNI_FALSE,
                               evType, javaMods, keyCode, keyChar);
        #endif
    }

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void) keyDown: (NSEvent*) theEvent
{
    [self sendKeyEvent: theEvent eventType: EVENT_KEY_PRESSED];
}

- (void) keyUp: (NSEvent*) theEvent
{
    [self sendKeyEvent: theEvent eventType: EVENT_KEY_RELEASED];
    [self sendKeyEvent: theEvent eventType: EVENT_KEY_TYPED];
}

- (void) sendMouseEvent: (NSEvent*) event eventType: (jint) evType
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
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
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("sendMouseEvent: null JNIEnv\n");
        return;
    }
    jint javaMods = mods2JavaMods([event modifierFlags]);

    // convert to 1-based button number (or use zero if no button is involved)
    // TODO: detect mouse button when mouse wheel scrolled  
    jint javaButtonNum = 0;
    jint scrollDeltaY = 0;
    switch ([event type]) {
    case NSScrollWheel: {
        scrollDeltaY = GetDeltaY(event, javaMods);
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

    #ifdef USE_SENDIO_DIRECT
    (*env)->CallVoidMethod(env, javaWindowObject, sendMouseEventID,
                           evType, javaMods,
                           (jint) location.x, (jint) location.y,
                           javaButtonNum, scrollDeltaY);
    #else
    (*env)->CallVoidMethod(env, javaWindowObject, enqueueMouseEventID, JNI_FALSE,
                           evType, javaMods,
                           (jint) location.x, (jint) location.y,
                           javaButtonNum, scrollDeltaY);
    #endif

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void) setMouseVisible:(BOOL)v
{
    mouseVisible = v;
    DBG_PRINT( "setMouseVisible: confined %d, visible %d\n", mouseConfined, mouseVisible);
    if(YES == mouseInside) {
        [self cursorHide: !mouseVisible];
    }
}

- (void) cursorHide:(BOOL)v
{
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
    NSPoint l0 = [NSEvent mouseLocation];
    [self screenPos2NewtClientWinPos: l0];
}

- (void) mouseEntered: (NSEvent*) theEvent
{
    DBG_PRINT( "mouseEntered: confined %d, visible %d\n", mouseConfined, mouseVisible);
    mouseInside = YES;
    [self setMouseVisible: mouseVisible];
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
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("windowDidResize: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("windowDidResize: null JNIEnv\n");
        return;
    }

    // update insets on every window resize for lack of better hook place
    [self updateInsets: env];

    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    (*env)->CallVoidMethod(env, javaWindowObject, sizeChangedID, JNI_FALSE,
                           (jint) contentRect.size.width,
                           (jint) contentRect.size.height, JNI_FALSE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void)windowDidMove: (NSNotification*) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
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
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("windowDidMove: null JNIEnv\n");
        return;
    }

    NSPoint p0 = { 0, 0 };
    p0 = [self getLocationOnScreen: p0];
    (*env)->CallVoidMethod(env, javaWindowObject, positionChangedID, JNI_FALSE, (jint) p0.x, (jint) p0.y);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void)windowWillClose: (NSNotification*) notification
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    [self cursorHide: NO];

    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;

    if( false == [view getDestroyNotifySent] ) {
        jobject javaWindowObject = [view getJavaWindowObject];
        DBG_PRINT( "*************** windowWillClose.0: %p\n", (void *)(intptr_t)javaWindowObject);
        if (javaWindowObject == NULL) {
            DBG_PRINT("windowWillClose: null javaWindowObject\n");
            return;
        }
        int shallBeDetached = 0;
        JavaVM *jvmHandle = [view getJVMHandle];
        JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
        if(NULL==env) {
            DBG_PRINT("windowWillClose: null JNIEnv\n");
            return;
        }

        [view setDestroyNotifySent: true];
        (*env)->CallVoidMethod(env, javaWindowObject, windowDestroyNotifyID);
        // Can't issue call here - locked window state, done from Java method

        // EOL ..
        (*env)->DeleteGlobalRef(env, javaWindowObject);
        [view setJavaWindowObject: NULL];

        if (shallBeDetached) {
            (*jvmHandle)->DetachCurrentThread(jvmHandle);
        }
        DBG_PRINT( "*************** windowWillClose.X: %p\n", (void *)(intptr_t)javaWindowObject);
    } else {
        DBG_PRINT( "*************** windowWillClose (skip)\n");
    }
    [pool release];
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

- (void) becomeKeyWindow
{
    DBG_PRINT( "*************** becomeKeyWindow\n");
    [super becomeKeyWindow];
}

- (void) resignKeyWindow
{
    DBG_PRINT( "*************** resignKeyWindow\n");
    [super resignKeyWindow];
}

- (void) windowDidBecomeKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidBecomeKey\n");
    [self focusChanged: YES];
}

- (void) windowDidResignKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidResignKey\n");
    [self focusChanged: NO];
}

- (void) focusChanged: (BOOL) gained
{
    DBG_PRINT( "focusChanged: gained %d\n", gained);
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
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
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("focusChanged: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_FALSE, (gained == YES) ? JNI_TRUE : JNI_FALSE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

@end
