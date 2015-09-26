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

#define PRINTF(...) NSLog(@ __VA_ARGS__)

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
        if( fabs(deltaX) > fabs(deltaY) ) {
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

#define kVK_Shift     0x38
#define kVK_Option    0x3A
#define kVK_Control   0x3B
#define kVK_Command   0x37

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
    if( NULL == layoutData ) {
        return NULL;
    }
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
    if( NULL != currentKeyboard ) {
        CKCH_USCodeToNNChar = CKCH_CreateCodeToCharDict(currentKeyboard);
        CFRelease(currentKeyboard);
    }
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

static jmethodID enqueueMouseEventID = NULL;
static jmethodID enqueueKeyEventID = NULL;
static jmethodID requestFocusID = NULL;

static jmethodID insetsChangedID   = NULL;
static jmethodID sizeChangedID     = NULL;
static jmethodID sizeScreenPosInsetsChangedID = NULL;
static jmethodID updatePixelScaleID = NULL;
static jmethodID visibleChangedID = NULL;
static jmethodID screenPositionChangedID = NULL;
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

    destroyNotifySent = NO;
    softLockCount = 0;

    pthread_mutexattr_t softLockSyncAttr;
    pthread_mutexattr_init(&softLockSyncAttr);
    pthread_mutexattr_settype(&softLockSyncAttr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&softLockSync, &softLockSyncAttr); // recursive

    ptrTrackingTag = 0;
    myCursor = NULL;

    modsDown[0] = NO; // shift
    modsDown[1] = NO; // ctrl
    modsDown[2] = NO; // alt
    modsDown[3] = NO; // win
    mouseConfined = NO;
    mouseVisible = YES;
    mouseInside = NO;
    cursorIsHidden = NO;

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
    [self removeCursorRects];
    [self removeMyCursor];

    pthread_mutex_destroy(&softLockSync);
    DBG_PRINT("NewtView::dealloc.X: %p\n", self);
    [super dealloc];
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
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("drawRect: null JNIEnv\n");
        return;
    }

    NSRect viewFrame = [self frame];

    (*env)->CallVoidMethod(env, javaWindowObject, windowRepaintID, JNI_TRUE, // defer ..
        (int)dirtyRect.origin.x, (int)viewFrame.size.height - (int)dirtyRect.origin.y, 
        (int)dirtyRect.size.width, (int)dirtyRect.size.height);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (void) viewDidHide
{
    if(NULL==javaWindowObject) {
        DBG_PRINT("viewDidHide: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidHide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_FALSE);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);

    [super viewDidHide];
}

- (void) viewDidUnhide
{
    if(NULL==javaWindowObject) {
        DBG_PRINT("viewDidUnhide: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidUnhide: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, JNI_FALSE, JNI_TRUE);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);

    [super viewDidUnhide];
}

- (BOOL) acceptsFirstResponder 
{
    return YES;
}

- (BOOL) becomeFirstResponder
{
    DBG_PRINT( "*************** View.becomeFirstResponder\n");
    return [super becomeFirstResponder];
}

- (BOOL) resignFirstResponder
{
    DBG_PRINT( "*************** View.resignFirstResponder\n");
    return [super resignFirstResponder];
}

- (void) removeCursorRects
{
    if(0 != ptrTrackingTag) {
        if(NULL != myCursor) {
            [self removeCursorRect: ptrRect cursor: myCursor];
        }
        [self removeTrackingRect: ptrTrackingTag];
        ptrTrackingTag = 0;
    }
}

- (void) addCursorRects
{
    ptrRect = [self bounds]; 
    if(NULL != myCursor) {
        [self addCursorRect: ptrRect cursor: myCursor];
    }
    ptrTrackingTag = [self addTrackingRect: ptrRect owner: self userData: nil assumeInside: NO];
}

- (void) removeMyCursor
{
    if(NULL != myCursor) {
        [myCursor release];
        myCursor = NULL;
    }
}

- (void) resetCursorRects
{
    [super resetCursorRects];

    [self removeCursorRects];
    [self addCursorRects];
}

- (void) setPointerIcon: (NSCursor*)c
{
    DBG_PRINT( "setPointerIcon: %p -> %p, top %p, mouseInside %d\n", myCursor, c, [NSCursor currentCursor], (int)mouseInside);
    if( c != myCursor ) {
        [self removeCursorRects];
        [self removeMyCursor];
        myCursor = c;
        if( NULL != myCursor ) {
            [myCursor retain];
        }
    }
    NSWindow* nsWin = [self window];
    if( NULL != nsWin ) {
        [nsWin invalidateCursorRectsForView: self];
    }
}

- (void) mouseEntered: (NSEvent*) theEvent
{
    DBG_PRINT( "mouseEntered: confined %d, visible %d, PointerIcon %p, top %p\n", mouseConfined, mouseVisible, myCursor, [NSCursor currentCursor]);
    mouseInside = YES;
    [self cursorHide: !mouseVisible enter: 1];
    if(NO == mouseConfined) {
        [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_ENTERED];
    }
    NSWindow* nsWin = [self window];
    if( NULL != nsWin ) {
        [nsWin makeFirstResponder: self];
    }
}

- (void) mouseExited: (NSEvent*) theEvent
{
    DBG_PRINT( "mouseExited: confined %d, visible %d, PointerIcon %p, top %p\n", mouseConfined, mouseVisible, myCursor, [NSCursor currentCursor]);
    if(NO == mouseConfined) {
        mouseInside = NO;
        [self cursorHide: NO enter: -1];
        [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_EXITED];
        [self resignFirstResponder];
    } else {
        [self setMousePosition: lastInsideMousePosition];
    }
}

/**
 * p abs screen position w/ bottom-left origin
 */
- (void) setMousePosition:(NSPoint)p
{
    NSWindow* nsWin = [self window];
    if( NULL != nsWin ) {
        NSScreen* screen = [nsWin screen];

        CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
        CGRect frameTL = CGDisplayBounds (display); // origin top-left
        NSRect frameBL = [screen frame]; // origin bottom-left
        CGPoint pt = { p.x, frameTL.origin.y + frameTL.size.height - ( p.y - frameBL.origin.y ) }; // y-flip from BL-screen -> TL-screen

        DBG_PRINT( "setMousePosition: point-in[%d/%d], screen tl[%d/%d %dx%d] bl[%d/%d %dx%d] -> %d/%d\n",
            (int)p.x, (int)p.y,
            (int)frameTL.origin.x, (int)frameTL.origin.y, (int)frameTL.size.width, (int)frameTL.size.height,
            (int)frameBL.origin.x, (int)frameBL.origin.y, (int)frameBL.size.width, (int)frameBL.size.height,
            (int)pt.x, (int)pt.y);

        CGEventRef ev = CGEventCreateMouseEvent (NULL, kCGEventMouseMoved, pt, kCGMouseButtonLeft);
        CGEventPost (kCGHIDEventTap, ev);
    }
}

- (BOOL) updateMouseInside
{
    NSRect viewFrame = [self frame];
    NSPoint l1 = [NSEvent mouseLocation];
    NSPoint l0 = [self screenPos2NewtClientWinPos: l1];
    mouseInside = viewFrame.origin.x <= l0.x && l0.x < (viewFrame.origin.x+viewFrame.size.width) &&
                  viewFrame.origin.y <= l0.y && l0.y < (viewFrame.origin.y+viewFrame.size.height) ;
    return mouseInside;
}

- (void) setMouseVisible:(BOOL)v hasFocus:(BOOL)focus
{
    mouseVisible = v;
    [self updateMouseInside];
    DBG_PRINT( "setMouseVisible: confined %d, visible %d (current: %d), mouseInside %d, hasFocus %d\n", 
        mouseConfined, mouseVisible, !cursorIsHidden, mouseInside, focus);
    if(YES == focus && YES == mouseInside) {
        [self cursorHide: !mouseVisible enter: 0];
    }
}
- (BOOL) isMouseVisible
{
    return mouseVisible;
}

- (void) cursorHide:(BOOL)v enter:(int)enterState
{
    DBG_PRINT( "cursorHide: %d -> %d, enter %d; PointerIcon: %p, top %p\n", 
        cursorIsHidden, v, enterState, myCursor, [NSCursor currentCursor]);
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

- (void) mouseMoved: (NSEvent*) theEvent
{
    if( mouseInside ) {
        NSCursor * currentCursor = [NSCursor currentCursor];
        BOOL setCursor = NULL != myCursor && NO == cursorIsHidden && currentCursor != myCursor;
        DBG_PRINT( "mouseMoved.set: %d; mouseInside %d, CursorHidden %d, PointerIcon: %p, top %p\n", 
            setCursor, mouseInside, cursorIsHidden, myCursor, currentCursor);
        if( setCursor ) {
            // FIXME: Workaround missing NSCursor update for 'fast moving' pointer 
            [myCursor set];
        }
        lastInsideMousePosition = [NSEvent mouseLocation];
        [self sendMouseEvent: theEvent eventType: EVENT_MOUSE_MOVED];
    }
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
    NSResponder* next = [self nextResponder];
    if (next != nil) {
        [next rightMouseDown: theEvent];
    }
    // FIXME: ^^ OR [super rightMouseDown: theEvent] ?
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

- (void) sendMouseEvent: (NSEvent*) event eventType: (jshort) evType
{
    if (javaWindowObject == NULL) {
        DBG_PRINT("sendMouseEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("sendMouseEvent: null JNIEnv\n");
        return;
    }
    jint javaMods[] = { 0 } ;
    javaMods[0] = mods2JavaMods([event modifierFlags]);

    // convert to 1-based button number (or use zero if no button is involved)
    // TODO: detect mouse button when mouse wheel scrolled  
    jshort javaButtonNum;
    jfloat scrollDeltaY = 0.0f;
    switch ([event type]) {
        case NSScrollWheel:
            scrollDeltaY = GetDelta(event, javaMods);
            javaButtonNum = 1;
            break;
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
        (*env)->CallVoidMethod(env, javaWindowObject, requestFocusID, JNI_FALSE);
    }

    NSPoint location = [self screenPos2NewtClientWinPos: [NSEvent mouseLocation]];

    (*env)->CallVoidMethod(env, javaWindowObject, enqueueMouseEventID, JNI_FALSE,
                           evType, javaMods[0],
                           (jint) location.x, (jint) location.y,
                           javaButtonNum, scrollDeltaY);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (NSPoint) screenPos2NewtClientWinPos: (NSPoint) p
{
    NSRect viewFrame = [self frame];

    NSRect r;
    r.origin.x = p.x;
    r.origin.y = p.y;
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [[self window] convertRectFromScreen: r]; // 10.7
    NSPoint oS = [[self window] convertScreenToBase: r.origin];
    oS.y = viewFrame.size.height - oS.y; // y-flip
    return oS;
}

- (void) handleFlagsChanged:(NSUInteger) mods
{
    [self handleFlagsChanged: NSShiftKeyMask keyIndex: 0 keyCode: kVK_Shift modifiers: mods];
    [self handleFlagsChanged: NSControlKeyMask keyIndex: 1 keyCode: kVK_Control modifiers: mods];
    [self handleFlagsChanged: NSAlternateKeyMask keyIndex: 2 keyCode: kVK_Option modifiers: mods];
    [self handleFlagsChanged: NSCommandKeyMask keyIndex: 3 keyCode: kVK_Command modifiers: mods];
}

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

- (void) sendKeyEvent: (NSEvent*) event eventType: (jshort) evType
{
    jshort keyCode = (jshort) [event keyCode];
    NSString* chars = [event charactersIgnoringModifiers];
    NSUInteger mods = [event modifierFlags];
    [self sendKeyEvent: keyCode characters: chars modifiers: mods eventType: evType];
}

- (void) sendKeyEvent: (jshort) keyCode characters: (NSString*) chars modifiers: (NSUInteger)mods eventType: (jshort) evType
{
    if (javaWindowObject == NULL) {
        DBG_PRINT("sendKeyEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("sendKeyEvent: null JNIEnv\n");
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

            DBG_PRINT("sendKeyEvent: %d/%d code 0x%X, char 0x%X, mods 0x%X/0x%X -> keySymChar 0x%X\n", i, len, (int)keyCode, (int)keyChar, 
                      (int)mods, (int)javaMods, (int)keySymChar);

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

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (void)viewDidChangeBackingProperties
{
    [super viewDidChangeBackingProperties];

    // HiDPI scaling
    BOOL useHiDPI = false;
    CGFloat maxPixelScale = 1.0;
    CGFloat winPixelScale = 1.0;
    NSWindow* window = [self window];
    NSScreen* screen = [window screen];
NS_DURING
    maxPixelScale = [screen backingScaleFactor];
    useHiDPI = [self wantsBestResolutionOpenGLSurface];
    if( useHiDPI ) {
        winPixelScale = [window backingScaleFactor];
    }
NS_HANDLER
NS_ENDHANDLER
    DBG_PRINT("viewDidChangeBackingProperties: PixelScale: HiDPI %d, max %f, window %f\n", useHiDPI, (float)maxPixelScale, (float)winPixelScale);
    [[self layer] setContentsScale: winPixelScale];

    if (javaWindowObject == NULL) {
        DBG_PRINT("viewDidChangeBackingProperties: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("viewDidChangeBackingProperties: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, updatePixelScaleID, JNI_TRUE, (jfloat)winPixelScale, (jfloat)maxPixelScale); // defer 

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}


@end

@implementation NewtMacWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZSIIISF)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZSISCC)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    updatePixelScaleID = (*env)->GetMethodID(env, clazz, "updatePixelScale", "(ZFF)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    sizeScreenPosInsetsChangedID = (*env)->GetMethodID(env, clazz, "sizeScreenPosInsetsChanged", "(ZIIIIIIIIZ)V");
    screenPositionChangedID = (*env)->GetMethodID(env, clazz, "screenPositionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");
    if (enqueueMouseEventID && enqueueKeyEventID && sizeChangedID && updatePixelScaleID && visibleChangedID && 
        insetsChangedID && sizeScreenPosInsetsChangedID &&
        screenPositionChangedID && focusChangedID && windowDestroyNotifyID && requestFocusID && windowRepaintID)
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
    // OSX 10.6
    if ( [NSApp respondsToSelector:@selector(currentSystemPresentationOptions)] &&
         [NSApp respondsToSelector:@selector(setPresentationOptions:)] ) {
        hasPresentationSwitch = YES;
        defaultPresentationOptions = [NSApp currentSystemPresentationOptions];
        fullscreenPresentationOptions = 
                // NSApplicationPresentationDefault|
                // NSApplicationPresentationAutoHideDock|
                NSApplicationPresentationHideDock|
                // NSApplicationPresentationAutoHideMenuBar|
                NSApplicationPresentationHideMenuBar|
                NSApplicationPresentationDisableAppleMenu|
                // NSApplicationPresentationDisableProcessSwitching|
                // NSApplicationPresentationDisableSessionTermination|
                NSApplicationPresentationDisableHideApplication|
                // NSApplicationPresentationDisableMenuBarTransparency|
                // NSApplicationPresentationFullScreen| // OSX 10.7
                0 ;
    } else {
        hasPresentationSwitch = NO;
        defaultPresentationOptions = 0;
        fullscreenPresentationOptions = 0; 
    }

    isFullscreenWindow = isfs;
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    [self setDelegate: self];

    cachedInsets[0] = 0; // l
    cachedInsets[1] = 0; // r
    cachedInsets[2] = 0; // t
    cachedInsets[3] = 0; // b

    realized = YES;
    DBG_PRINT("NewtWindow::create: %p, realized %d, hasPresentationSwitch %d[defaultOptions 0x%X, fullscreenOptions 0x%X], (refcnt %d)\n", 
        res, realized, (int)hasPresentationSwitch, (int)defaultPresentationOptions, (int)fullscreenPresentationOptions, (int)[res retainCount]);
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

- (void) setAlwaysOn: (BOOL)top bottom:(BOOL)bottom
{
    if( top ) {
        DBG_PRINT( "*************** setAlwaysOn -> top\n");
        [self setLevel: kCGMaximumWindowLevel];
    } else if ( bottom ) {
        DBG_PRINT( "*************** setAlwaysOn -> bottom\n");
        [self setLevel: kCGDesktopIconWindowLevel]; // w/ input
    } else {
        DBG_PRINT( "*************** setAlwaysOn -> normal\n");
        [self setLevel:NSNormalWindowLevel];
    }
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

- (void) updateSizePosInsets: (JNIEnv*) env jwin: (jobject) javaWin defer: (jboolean)defer
{
    // update insets on every window resize for lack of better hook place
    [self updateInsets: NULL jwin:NULL];

    NSRect frameRect = [self frame];
    NSRect contentRect = [self contentRectForFrameRect: frameRect];

    DBG_PRINT( "updateSize: [ w %d, h %d ]\n", (jint) contentRect.size.width, (jint) contentRect.size.height);

    NSPoint p0 = { 0, 0 };
    p0 = [self getLocationOnScreen: p0];

    DBG_PRINT( "updatePos: [ x %d, y %d ]\n", (jint) p0.x, (jint) p0.y);

    if( NULL != env && NULL != javaWin ) {
        (*env)->CallVoidMethod(env, javaWin, sizeScreenPosInsetsChangedID, defer,
                               (jint) p0.x, (jint) p0.y,
                               (jint) contentRect.size.width, (jint) contentRect.size.height,
                               cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3],
                               JNI_FALSE // force
                              );
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

    DBG_PRINT( "newtAbsClientTLWinPos2AbsBLScreenPos: point-in[%d/%d], size-in[%dx%d], insets bottom %d -> totalHeight %d\n", 
        (int)p.x, (int)p.y, (int)nsz.width, (int)nsz.height, cachedInsets[3], totalHeight);

    NSScreen* screen = [self screen];

    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
    CGRect frameTL = CGDisplayBounds (display); // origin top-left
    NSRect frameBL = [screen frame]; // origin bottom-left
    NSPoint r = NSMakePoint(p.x, frameBL.origin.y + frameBL.size.height - ( p.y - frameTL.origin.y ) - totalHeight); // y-flip from TL-screen -> BL-screen

    DBG_PRINT( "newtAbsClientTLWinPos2AbsBLScreenPos: screen tl[%d/%d %dx%d] bl[%d/%d %dx%d ->  %d/%d\n",
        (int)frameTL.origin.x, (int)frameTL.origin.y, (int)frameTL.size.width, (int)frameTL.size.height,
        (int)frameBL.origin.x, (int)frameBL.origin.y, (int)frameBL.size.width, (int)frameBL.size.height,
        (int)r.x, (int)r.y);

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
    NSPoint r = NSMakePoint(winFrame.origin.x + p.x,
                            winFrame.origin.y + ( mViewFrame.size.height - p.y ) ); // y-flip in view

    DBG_PRINT( "newtRelClientTLWinPos2AbsBLScreenPos: point-in[%d/%d], winFrame[%d/%d %dx%d], viewFrame[%d/%d %dx%d] -> %d/%d\n",
        (int)p.x, (int)p.y,
        (int)winFrame.origin.x, (int)winFrame.origin.y, (int)winFrame.size.width, (int)winFrame.size.height,
        (int)mViewFrame.origin.x, (int)mViewFrame.origin.y, (int)mViewFrame.size.width, (int)mViewFrame.size.height,
        (int)r.x, (int)r.y);

    return r;
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
    NSView* view = [self contentView];
    NSRect viewFrame = [view frame];
    NSRect r;
    r.origin.x = p.x;
    r.origin.y = viewFrame.size.height - p.y; // y-flip
    r.size.width = 0;
    r.size.height = 0;
    // NSRect rS = [self convertRectToScreen: r]; // 10.7
    NSPoint oS = [self convertBaseToScreen: r.origin]; // BL-screen

    NSScreen* screen = [self screen];
    CGDirectDisplayID display = NewtScreen_getCGDirectDisplayIDByNSScreen(screen);
    CGRect frameTL = CGDisplayBounds (display); // origin top-left
    NSRect frameBL = [screen frame]; // origin bottom-left
    oS.y = frameTL.origin.y + frameTL.size.height - ( oS.y - frameBL.origin.y ); // y-flip from BL-screen -> TL-screen

#ifdef VERBOSE_ON
    NSRect winFrame = [self frame];
    DBG_PRINT( "getLocationOnScreen: point-in[%d/%d], winFrame[%d/%d %dx%d], viewFrame[%d/%d %dx%d], screen tl[%d/%d %dx%d] bl[%d/%d %dx%d] -> %d/%d\n",
        (int)p.x, (int)p.y,
        (int)winFrame.origin.x, (int)winFrame.origin.y, (int)winFrame.size.width, (int)winFrame.size.height,
        (int)viewFrame.origin.x, (int)viewFrame.origin.y, (int)viewFrame.size.width, (int)viewFrame.size.height,
        (int)frameTL.origin.x, (int)frameTL.origin.y, (int)frameTL.size.width, (int)frameTL.size.height,
        (int)frameBL.origin.x, (int)frameBL.origin.y, (int)frameBL.size.width, (int)frameBL.size.height,
        (int)oS.x, (int)oS.y);
#endif

    return oS;
}

- (void) focusChanged: (BOOL) gained
{
    DBG_PRINT( "focusChanged: gained %d\n", gained);
    NewtView* newtView = (NewtView *) [self contentView];
    if( ! [newtView isKindOfClass:[NewtView class]] ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("focusChanged: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("focusChanged: null JNIEnv\n");
        return;
    }

    (*env)->CallVoidMethod(env, javaWindowObject, focusChangedID, JNI_FALSE, (gained == YES) ? JNI_TRUE : JNI_FALSE);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (void) keyDown: (NSEvent*) theEvent
{
    NewtView* newtView = (NewtView *) [self contentView];
    if( [newtView isKindOfClass:[NewtView class]] ) {
        [newtView sendKeyEvent: theEvent eventType: (jshort)EVENT_KEY_PRESSED];
    }
}

- (void) keyUp: (NSEvent*) theEvent
{
    NewtView* newtView = (NewtView *) [self contentView];
    if( [newtView isKindOfClass:[NewtView class]] ) {
        [newtView sendKeyEvent: theEvent eventType: (jshort)EVENT_KEY_RELEASED];
    }
}

- (void) flagsChanged:(NSEvent *) theEvent
{
    NSUInteger mods = [theEvent modifierFlags];
    NewtView* newtView = (NewtView *) [self contentView];
    if( [newtView isKindOfClass:[NewtView class]] ) {
        [newtView handleFlagsChanged: mods];
    }
}

- (BOOL) acceptsMouseMovedEvents
{
    return YES;
}

- (BOOL) acceptsFirstResponder 
{
    return YES;
}

- (BOOL) becomeFirstResponder
{
    DBG_PRINT( "*************** Win.becomeFirstResponder\n");
    return [super becomeFirstResponder];
}

- (BOOL) resignFirstResponder
{
    DBG_PRINT( "*************** Win.resignFirstResponder\n");
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
    NewtView* newtView = (NewtView *) [self contentView];
    if( [newtView isKindOfClass:[NewtView class]] ) {
        BOOL mouseInside = [newtView updateMouseInside];
        if(YES == mouseInside) {
            [newtView cursorHide: ![newtView isMouseVisible] enter: 1];
        }
    }
    [self focusChanged: YES];
}

- (void) windowDidResignKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidResignKey\n");
    // Implicit mouse exit by OS X
    [self focusChanged: NO];
}

- (void)windowDidResize: (NSNotification*) notification
{
    jobject javaWindowObject = NULL;
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);

    if( NULL == env ) {
        DBG_PRINT("windowDidResize: null JNIEnv\n");
        return;
    }
    NewtView* newtView = (NewtView *) [self contentView];
    if( [newtView isKindOfClass:[NewtView class]] ) {
        javaWindowObject = [newtView getJavaWindowObject];
    }
    if( NULL != javaWindowObject ) {
        [self updateSizePosInsets: env jwin: javaWindowObject defer:JNI_TRUE];
    }
    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (void)windowDidMove: (NSNotification*) notification
{
    NewtView* newtView = (NewtView *) [self contentView];
    if( ! [newtView isKindOfClass:[NewtView class]] ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if (javaWindowObject == NULL) {
        DBG_PRINT("windowDidMove: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("windowDidMove: null JNIEnv\n");
        return;
    }

    NSPoint p0 = { 0, 0 };
    p0 = [self getLocationOnScreen: p0];
    DBG_PRINT( "windowDidMove: [ x %d, y %d ]\n", (jint) p0.x, (jint) p0.y);
    (*env)->CallVoidMethod(env, javaWindowObject, screenPositionChangedID, JNI_TRUE, (jint) p0.x, (jint) p0.y);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
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

    NewtView* newtView = (NewtView *) [self contentView];
    if( ! [newtView isKindOfClass:[NewtView class]] ) {
        return NO;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    [newtView cursorHide: NO enter: -1];

    if( false == [newtView getDestroyNotifySent] ) {
        jobject javaWindowObject = [newtView getJavaWindowObject];
        DBG_PRINT( "*************** windowWillClose.0: %p\n", (void *)(intptr_t)javaWindowObject);
        if (javaWindowObject == NULL) {
            DBG_PRINT("windowWillClose: null javaWindowObject\n");
            [pool release];
            return NO;
        }
        int shallBeDetached = 0;
        JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
        if(NULL==env) {
            DBG_PRINT("windowWillClose: null JNIEnv\n");
            [pool release];
            return NO;
        }
        [newtView setDestroyNotifySent: true]; // earmark assumption of being closed
        closed = (*env)->CallBooleanMethod(env, javaWindowObject, windowDestroyNotifyID, force ? JNI_TRUE : JNI_FALSE);
        if(!force && !closed) {
            // not closed on java side, not force -> clear flag
            [newtView setDestroyNotifySent: false];
        }

        // detaching thread not required - daemon
        // NewtCommon_ReleaseJNIEnv(shallBeDetached);
        DBG_PRINT( "*************** windowWillClose.X: %p, closed %d\n", (void *)(intptr_t)javaWindowObject, (int)closed);
    } else {
        DBG_PRINT( "*************** windowWillClose (skip)\n");
    }
    [pool release];
    return JNI_TRUE == closed ? YES : NO ;
}

@end

