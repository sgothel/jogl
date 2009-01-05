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

static jmethodID sendMouseEventID  = NULL;
static jmethodID sendKeyEventID    = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID positionChangedID = NULL;
static jmethodID windowClosedID    = NULL;

// This is set while messages are being dispatched and cleared afterward
static JNIEnv* env = NULL;

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    sendMouseEventID  = (*env)->GetMethodID(env, clazz, "sendMouseEvent",  "(IIIII)V");
    sendKeyEventID    = (*env)->GetMethodID(env, clazz, "sendKeyEvent",    "(IIIC)V");
    sizeChangedID     = (*env)->GetMethodID(env, clazz, "sizeChanged",     "(II)V");
    positionChangedID = (*env)->GetMethodID(env, clazz, "positionChanged", "(II)V");
    windowClosedID    = (*env)->GetMethodID(env, clazz, "windowClosed",    "()V");
    if (sendMouseEventID && sendKeyEventID && sizeChangedID && positionChangedID && windowClosedID) {
        return YES;
    }
    return NO;
}

+ (void) setJNIEnv: (JNIEnv*) theEnv
{
    env = theEnv;
}

- (id) initWithContentRect: (NSRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSBackingStoreType) bufferingType
       defer: (BOOL) deferCreation
       javaWindowObject: (jobject) javaWindowObj
{
    id res = [super initWithContentRect: contentRect
                    styleMask: windowStyle
                    backing: bufferingType
                    defer: deferCreation];
    javaWindowObject = javaWindowObj;
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    [self setDelegate: self];
    return res;
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
    int i;
    jint keyCode = (jint) [event keyCode];
    NSString* chars = [event charactersIgnoringModifiers];
    int len = [chars length];
    jint javaMods = mods2JavaMods([event modifierFlags]);

    if (env == NULL) {
        return;
    }

    if (javaWindowObject == NULL) {
        return;
    }

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
}

- (void) sendMouseEvent: (NSEvent*) event eventType: (jint) evType
{
    jint javaMods = mods2JavaMods([event modifierFlags]);
    NSPoint location = [event locationInWindow];
    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    if (env == NULL) {
        return;
    }

    if (javaWindowObject == NULL) {
        return;
    }

    // 1-base the button number
    (*env)->CallVoidMethod(env, javaWindowObject, sendMouseEventID,
                           evType, javaMods,
                           (jint) location.x,
                           (jint) (contentRect.size.height - location.y),
                           (jint) (1 + [event buttonNumber]));
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
    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    if (env == NULL) {
        return;
    }

    if (javaWindowObject == NULL) {
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, sizeChangedID,
                           (jint) contentRect.size.width,
                           (jint) contentRect.size.height);
}

- (void)windowDidMove: (NSNotification*) notification
{
    NSRect rect = [self frame];

    if (env == NULL) {
        return;
    }

    if (javaWindowObject == NULL) {
        return;
    }

    // FIXME: this result isn't consistent with setFrameTopLeftPoint

    (*env)->CallVoidMethod(env, javaWindowObject, positionChangedID,
                           (jint) rect.origin.x,
                           (jint) rect.origin.y);
}

- (void)windowWillClose: (NSNotification*) notification
{
    if (env == NULL) {
        return;
    }

    if (javaWindowObject == NULL) {
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, windowClosedID);
}

@end
