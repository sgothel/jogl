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
    } else {
        // traditional mouse wheel case
        deltaY = [event deltaY];
        if (deltaY == 0.0 && (javaMods & EVENT_SHIFT_MASK) != 0) {
            // shift+vertical wheel scroll produces horizontal scroll
            // we convert it to vertical
            deltaY = [event deltaX];
        }
        if (deltaY < 1.0  && deltaY > -1.0) {
            deltaY *= 10.0;
        } else {
            if (deltaY < 0.0) {
                deltaY = deltaY - 0.5f;
            } else {
                deltaY = deltaY + 0.5f;
            }
        }
    }

    if (deltaY > 0) {
        return (NSInteger)deltaY;
    } else if (deltaY < 0) {
        return -(NSInteger)deltaY;
    }

    return 0;
}

static jmethodID enqueueMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID enqueueRequestFocusID = NULL;

static jmethodID insetsChangedID   = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID    = NULL;
static jmethodID windowDestroyNotifyID = NULL;

@implementation NewtView

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

- (void) setDestroyNotifySent: (BOOL) v
{
    destroyNotifySent = v;
}

- (BOOL) getDestroyNotifySent
{
    return destroyNotifySent;
}

- (void) rightMouseDown: (NSEvent*) theEvent
{
    NSResponder* next = [self nextResponder];
    if (next != nil) {
        [next rightMouseDown: theEvent];
    }
}

- (void)viewWillDraw
{
    DBG_PRINT("*************** viewWillDraw: 0x%p\n", javaWindowObject);
    [super viewWillDraw];
}

- (void)viewDidHide
{
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL==env) {
        NSLog(@"viewDidHide: null JNIEnv");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_TRUE, JNI_FALSE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }

    [super viewDidHide];
}

- (void)viewDidUnhide
{
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, jvmVersion, &shallBeDetached);
    if(NULL==env) {
        NSLog(@"viewDidHide: null JNIEnv");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_TRUE, JNI_TRUE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }

    [super viewDidUnhide];
}

@end

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZIIIIII)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZIIIC)V");
    sizeChangedID     = (*env)->GetMethodID(env, clazz, "sizeChanged",     "(ZIIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    insetsChangedID     = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID    = (*env)->GetMethodID(env, clazz, "windowDestroyNotify",    "()V");
    enqueueRequestFocusID = (*env)->GetMethodID(env, clazz, "enqueueRequestFocus", "(Z)V");
    if (enqueueMouseEventID && enqueueKeyEventID && sizeChangedID && visibleChangedID && insetsChangedID &&
        positionChangedID && focusChangedID && windowDestroyNotifyID && enqueueRequestFocusID)
    {
        return YES;
    }
    return NO;
}

- (NSPoint) getLocationOnScreen: (NSPoint) p
{
    /**
     * return location in 0/0 top-left space,
     * OSX is 0/0 bottom-left space naturally
     */
    NSScreen* screen = [self screen];
    NSRect screenRect = [screen frame];

    NSView* view = [self contentView];
    NSRect viewFrame = [view frame];

    NSRect r;
    r.origin.x = p.x;
    r.origin.y = viewFrame.size.height - p.y; // y-flip for 0/0 top-left
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [win convertRectToScreen: r]; // 10.7
    NSPoint oS = [self convertBaseToScreen: r.origin];
    oS.y = screenRect.origin.y + screenRect.size.height - oS.y;
    return oS;
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

    (*env)->CallVoidMethod(env, javaWindowObject, insetsChangedID, JNI_TRUE, cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);
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
    return res;
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
        NSLog(@"sendKeyEvent: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"sendKeyEvent: null JNIEnv");
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

        (*env)->CallVoidMethod(env, javaWindowObject, enqueueKeyEventID, JNI_FALSE,
                               evType, javaMods, keyCode, keyChar);
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
        NSLog(@"sendMouseEvent: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"sendMouseEvent: null JNIEnv");
        return;
    }

    jint javaMods = mods2JavaMods([event modifierFlags]);
    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];
    // NSPoint location = [event locationInWindow];
    // The following computation improves the behavior of mouse drag
    // events when they also affect the location of the window, but it
    // still isn't perfect
    NSPoint curLocation = [NSEvent mouseLocation];
    NSPoint location = NSMakePoint(curLocation.x - frameRect.origin.x,
                                   curLocation.y - frameRect.origin.y);

    // convert to 1-based button number (or use zero if no button is involved)
    // TODO: detect mouse button when mouse wheel scrolled  
    jint javaButtonNum = 0;
    jint scrollDeltaY = 0;
    switch ([event type]) {
    case NSScrollWheel: {
        scrollDeltaY = GetDeltaY(event, javaMods);
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
    default:
        javaButtonNum = 0;
        break;
    }

    if (evType == EVENT_MOUSE_WHEEL_MOVED && scrollDeltaY == 0) {
        // ignore 0 increment wheel scroll events
        return;
    }
    if (evType == EVENT_MOUSE_PRESSED) {
        (*env)->CallVoidMethod(env, javaWindowObject, enqueueRequestFocusID, JNI_FALSE);
    }
    (*env)->CallVoidMethod(env, javaWindowObject, enqueueMouseEventID, JNI_FALSE,
                           evType, javaMods,
                           (jint) location.x,
                           (jint) (contentRect.size.height - location.y),
                           javaButtonNum, scrollDeltaY);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void) mouseEntered: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_ENTERED];
}

- (void) mouseExited: (NSEvent*) theEvent
{
    [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_EXITED];
}

- (void) mouseMoved: (NSEvent*) theEvent
{
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
        NSLog(@"windowDidResize: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"windowDidResize: null JNIEnv");
        return;
    }

    // update insets on every window resize for lack of better hook place
    [self updateInsets: env];

    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    (*env)->CallVoidMethod(env, javaWindowObject, sizeChangedID, JNI_TRUE,
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
        NSLog(@"windowDidMove: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"windowDidMove: null JNIEnv");
        return;
    }

    NSPoint p0 = { 0, 0 };
    p0 = [self getLocationOnScreen: p0];
    (*env)->CallVoidMethod(env, javaWindowObject, positionChangedID, JNI_TRUE, (jint) p0.x, (jint) p0.y);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void)windowWillClose: (NSNotification*) notification
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;

    if( false == [view getDestroyNotifySent] ) {
        jobject javaWindowObject = [view getJavaWindowObject];
        DBG_PRINT( "*************** windowWillClose.0: 0x%p\n", (void *)(intptr_t)javaWindowObject);
        if (javaWindowObject == NULL) {
            NSLog(@"windowWillClose: null javaWindowObject");
            return;
        }
        int shallBeDetached = 0;
        JavaVM *jvmHandle = [view getJVMHandle];
        JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
        if(NULL==env) {
            NSLog(@"windowWillClose: null JNIEnv");
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
        DBG_PRINT( "*************** windowWillClose.X: 0x%p\n", (void *)(intptr_t)javaWindowObject);
    } else {
        DBG_PRINT( "*************** windowWillClose (skip)\n");
    }
    [pool release];
}

- (void) windowDidBecomeKey: (NSNotification *) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        NSLog(@"windowDidBecomeKey: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"windowDidBecomeKey: null JNIEnv");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_TRUE, JNI_TRUE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

- (void) windowDidResignKey: (NSNotification *) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    if (javaWindowObject == NULL) {
        NSLog(@"windowDidResignKey: null javaWindowObject");
        return;
    }
    int shallBeDetached = 0;
    JavaVM *jvmHandle = [view getJVMHandle];
    JNIEnv* env = NewtCommon_GetJNIEnv(jvmHandle, [view getJVMVersion], &shallBeDetached);
    if(NULL==env) {
        NSLog(@"windowDidResignKey: null JNIEnv");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_TRUE, JNI_FALSE);

    if (shallBeDetached) {
        (*jvmHandle)->DetachCurrentThread(jvmHandle);
    }
}

@end
