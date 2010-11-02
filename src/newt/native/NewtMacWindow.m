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

static jmethodID sendMouseEventID  = NULL;
static jmethodID sendKeyEventID    = NULL;
static jmethodID insetsChangedID   = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID focusChangedID    = NULL;
static jmethodID windowDestroyNotifyID = NULL;
static jmethodID windowDestroyedID = NULL;

@implementation NewtView
- (void) setJNIEnv: (JNIEnv*) theEnv
{
    env = theEnv;
}
- (JNIEnv*) getJNIEnv
{
    return env;
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

- (void)viewWillDraw
{
    fprintf(stderr, "*************** viewWillDraw: 0x%p", javaWindowObject); fflush(stderr);
    [super viewWillDraw];
}

- (void)viewDidHide
{
    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE);
    [super viewDidHide];
}

- (void)viewDidUnhide
{
    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_TRUE);
    [super viewDidUnhide];
}

@end

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    sendMouseEventID = (*env)->GetMethodID(env, clazz, "sendMouseEvent", "(IIIIII)V");
    sendKeyEventID = (*env)->GetMethodID(env, clazz, "sendKeyEvent", "(IIIC)V");
    sizeChangedID     = (*env)->GetMethodID(env, clazz, "sizeChanged",     "(IIZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    insetsChangedID     = (*env)->GetMethodID(env, clazz, "insetsChanged", "(IIII)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(Z)V");
    windowDestroyNotifyID    = (*env)->GetMethodID(env, clazz, "windowDestroyNotify",    "()V");
    windowDestroyedID    = (*env)->GetMethodID(env, clazz, "windowDestroyed",    "()V");
    if (sendMouseEventID && sendKeyEventID && sizeChangedID && visibleChangedID && insetsChangedID &&
        positionChangedID && focusChangedID && windowDestroyedID && windowDestroyNotifyID)
    {
        return YES;
    }
    return NO;
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
    jint top = (jint)(frameRect.size.height - contentRect.size.height);
    jint left = (jint)l;
    jint bottom = (jint)(contentRect.origin.y - frameRect.origin.y);
    jint right = (jint)(frameRect.size.width - (contentRect.size.width + l));

    (*env)->CallVoidMethod(env, javaWindowObject, insetsChangedID,
                           left, top, right, bottom);
}

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       screen:(NSScreen *)screen
{
    id res = [super initWithContentRect: contentRect
                    styleMask: windowStyle
                    backing: bufferingType
                    defer: YES
                    screen: screen];
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    [self setDelegate: self];
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
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
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

        (*env)->CallVoidMethod(env, javaWindowObject, sendKeyEventID,
                               evType, javaMods, keyCode, keyChar);
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
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
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
    (*env)->CallVoidMethod(env, javaWindowObject, sendMouseEventID,
                           evType, javaMods,
                           (jint) location.x,
                           (jint) (contentRect.size.height - location.y),
                           javaButtonNum, scrollDeltaY);
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
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }

    // update insets on every window resize for lack of better hook place
    [self updateInsets: env];

    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    (*env)->CallVoidMethod(env, javaWindowObject, sizeChangedID,
                           (jint) contentRect.size.width,
                           (jint) contentRect.size.height, JNI_FALSE);
}

- (void)windowDidMove: (NSNotification*) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }

    NSRect rect = [self frame];
    NSScreen* screen = NULL;
    NSRect screenRect;
    NSPoint pt;

    screen = [self screen];
    // this allows for better compatibility with awt behavior
    screenRect = [screen frame];
    pt = NSMakePoint(rect.origin.x, screenRect.origin.y + screenRect.size.height - rect.origin.y - rect.size.height);

    (*env)->CallVoidMethod(env, javaWindowObject, positionChangedID,
                           (jint) pt.x, (jint) pt.y);
}

- (void)windowWillClose: (NSNotification*) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, windowDestroyNotifyID);
    // Can't issue call here - locked window state, done from Java method
    // (*env)->CallVoidMethod(env, javaWindowObject, windowDestroyedID); // No OSX hook for DidClose, so do it here 

    // EOL ..
    (*env)->DeleteGlobalRef(env, javaWindowObject);
    [view setJavaWindowObject: NULL];
}

- (void) windowDidBecomeKey: (NSNotification *) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_TRUE);
}

- (void) windowDidResignKey: (NSNotification *) notification
{
    NSView* nsview = [self contentView];
    if( ! [nsview isMemberOfClass:[NewtView class]] ) {
        return;
    }
    NewtView* view = (NewtView *) nsview;
    jobject javaWindowObject = [view getJavaWindowObject];
    JNIEnv* env = [view getJNIEnv];
    if (env==NULL || javaWindowObject == NULL) {
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_FALSE);
}

@end
