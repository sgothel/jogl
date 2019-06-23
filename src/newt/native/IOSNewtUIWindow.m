/**
 * Copyright 2019 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 * 
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 * 
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * 
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */

#import "IOSNewtUIWindow.h"
#import "InputEvent.h"
#import "KeyEvent.h"
#import "MouseEvent.h"

#include <CoreFoundation/CoreFoundation.h>

#include <math.h>

#define PRINTF(...) NSLog(@ __VA_ARGS__)

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

@implementation NewtUIView

- (id)initWithFrame:(CGRect)frameRect
{
    id res = [super initWithFrame:frameRect];
    javaWindowObject = NULL;

    destroyNotifySent = NO;
    softLockCount = 0;

    pthread_mutexattr_t softLockSyncAttr;
    pthread_mutexattr_init(&softLockSyncAttr);
    pthread_mutexattr_settype(&softLockSyncAttr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&softLockSync, &softLockSyncAttr); // recursive

    modsDown[0] = NO; // shift
    modsDown[1] = NO; // ctrl
    modsDown[2] = NO; // alt
    modsDown[3] = NO; // win

    DBG_PRINT("NewtUIView::create: %p (refcnt %d)\n", res, (int)[res retainCount]);
    return res;
}

#ifdef DBG_LIFECYCLE
- (void) release
{
    DBG_PRINT("NewtUIView::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    [super release];
}
#endif

- (void) dealloc
{
    DBG_PRINT("NewtUIView::dealloc.0: %p (refcnt %d), ptrTrackingTag %d\n", self, (int)[self retainCount], (int)ptrTrackingTag);
#ifdef DBG_LIFECYCLE
    NSLog(@"%@",[NSThread callStackSymbols]);
#endif
    if( 0 < softLockCount ) {
        NSLog(@"NewtUIView::dealloc: softLock still hold @ dealloc!\n");
    }

    pthread_mutex_destroy(&softLockSync);
    DBG_PRINT("NewtUIView::dealloc.X: %p\n", self);
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
        NSLog(@"NewtUIView::softLock failed: errCode %d - %@", err, [NSThread callStackSymbols]);
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
        NSLog(@"NewtUIView::softUnlock failed: Not locked by current thread - errCode %d -  %@", err, [NSThread callStackSymbols]);
        return NO;
    }
    return YES;
}

- (void) drawRect:(CGRect)dirtyRect
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

    CGRect viewFrame = [self frame];

    (*env)->CallVoidMethod(env, javaWindowObject, windowRepaintID, JNI_TRUE, // defer ..
        (int)dirtyRect.origin.x, (int)viewFrame.size.height - (int)dirtyRect.origin.y, 
        (int)dirtyRect.size.width, (int)dirtyRect.size.height);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
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

- (void) sendMouseEvent: (UIEvent*) event eventType: (jshort) evType
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
    javaMods[0] = 0; // TODO mods2JavaMods([event modifierFlags]);

    // convert to 1-based button number (or use zero if no button is involved)
    // TODO: detect mouse button when mouse wheel scrolled  
    jshort javaButtonNum = 1;
    jfloat scrollDeltaY = 0.0f;
    /**
    switch ([event type]) {
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
    } */
    CGPoint location = CGPointMake(0,0); // TODO [self screenPos2NewtClientWinPos: [UIEvent mouseLocation]];

    (*env)->CallVoidMethod(env, javaWindowObject, enqueueMouseEventID, JNI_FALSE,
                           evType, javaMods[0],
                           (jint) location.x, (jint) location.y,
                           javaButtonNum, scrollDeltaY);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (CGPoint) screenPos2NewtClientWinPos: (CGPoint) p
{
    CGRect viewFrame = [self frame];

    CGRect r;
    r.origin.x = p.x;
    r.origin.y = p.y;
    r.size.width = 0;
    r.size.height = 0;
    // CGRect rS = [[self window] convertRectFromScreen: r]; // 10.7
    CGPoint oS = r.origin; // TODO [[self window] convertScreenToBase: r.origin];
    oS.y = viewFrame.size.height - oS.y; // y-flip
    return oS;
}

- (void) handleFlagsChanged:(NSUInteger) mods
{
    // TODO [self handleFlagsChanged: NSShiftKeyMask keyIndex: 0 keyCode: kVK_Shift modifiers: mods];
    // TODO [self handleFlagsChanged: NSControlKeyMask keyIndex: 1 keyCode: kVK_Control modifiers: mods];
    // TODO [self handleFlagsChanged: NSAlternateKeyMask keyIndex: 2 keyCode: kVK_Option modifiers: mods];
    // TODO [self handleFlagsChanged: NSCommandKeyMask keyIndex: 3 keyCode: kVK_Command modifiers: mods];
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

- (void) sendKeyEvent: (UIEvent*) event eventType: (jshort) evType
{
    jshort keyCode = 0; // TODO (jshort) [event keyCode];
    NSString* chars = NULL; // TODO [event charactersIgnoringModifiers];
    NSUInteger mods = 0; // TODO [event modifierFlags];
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
    jint javaMods = 0; // TODO mods2JavaMods(mods);

    if(len > 0) {
        // printable chars
        for (i = 0; i < len; i++) {
            // Note: the key code in the UIEvent does not map to anything we can use
            UniChar keyChar = (UniChar) [chars characterAtIndex: i];
            UniChar keySymChar = 0; // TODO CKCH_CharForKeyCode(keyCode);

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

@end

@implementation NewtUIWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    enqueueMouseEventID = (*env)->GetMethodID(env, clazz, "enqueueMouseEvent", "(ZSIIISF)V");
    enqueueKeyEventID = (*env)->GetMethodID(env, clazz, "enqueueKeyEvent", "(ZSISCC)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    updatePixelScaleID = (*env)->GetMethodID(env, clazz, "updatePixelScale", "(ZFF)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(ZZ)V");
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(ZIIII)V");
    sizeScreenPosInsetsChangedID = (*env)->GetMethodID(env, clazz, "sizeScreenPosInsetsChanged", "(ZIIIIIIIIZZ)V");
    screenPositionChangedID = (*env)->GetMethodID(env, clazz, "screenPositionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");
    if (enqueueMouseEventID && enqueueKeyEventID && sizeChangedID && updatePixelScaleID && visibleChangedID && 
        insetsChangedID && sizeScreenPosInsetsChangedID &&
        screenPositionChangedID && focusChangedID && windowDestroyNotifyID && requestFocusID && windowRepaintID)
    {
        // TODO CKCH_CreateDictionaries();
        return YES;
    }
    return NO;
}

- (id) initWithFrame: (CGRect) contentRect
       styleMask: (NSUInteger) windowStyle
       backing: (NSUInteger) bufferingType
       defer: (BOOL) deferCreation
       isFullscreenWindow:(BOOL)isfs
{
    /**
    id res = [super initWithContentRect: contentRect
                    styleMask: windowStyle
                    backing: bufferingType
                    defer: deferCreation];
     */
    id res = [super initWithFrame: contentRect];
    // OSX 10.6
    /** TODO
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
    */
        hasPresentationSwitch = NO;
        defaultPresentationOptions = 0;
        fullscreenPresentationOptions = 0; 
    // }

    isFullscreenWindow = NO; // TODO isfs;
    // Why is this necessary? Without it we don't get any of the
    // delegate methods like resizing and window movement.
    // TODO [self setDelegate: self];

    cachedInsets[0] = 0; // l
    cachedInsets[1] = 0; // r
    cachedInsets[2] = 0; // t
    cachedInsets[3] = 0; // b

    realized = YES;
    withinLiveResize = JNI_FALSE;
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

    /**
    NewtUIView* mView = (NewtUIView *)self; // TODO [self contentView];
    if( NULL != mView ) {
        [mView release];
    }
    */
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
    /**
    if( top ) {
        DBG_PRINT( "*************** setAlwaysOn -> top\n");
        [self setLevel: kCGMaximumWindowLevel];
    } else if ( bottom ) {
        DBG_PRINT( "*************** setAlwaysOn -> bottom\n");
        [self setLevel: kCGDesktopIconWindowLevel]; // w/ input
    } else {
        DBG_PRINT( "*************** setAlwaysOn -> normal\n");
        [self setLevel:NSNormalWindowLevel];
    } */
}

- (void) updateInsets: (JNIEnv*) env jwin: (jobject) javaWin
{
    /**
    CGRect frameRect = [self frame];
    CGRect contentRect = [self contentRectForFrameRect: frameRect];

    // note: this is a simplistic implementation which doesn't take
    // into account DPI and scaling factor
    CGFloat l = contentRect.origin.x - frameRect.origin.x;
    cachedInsets[0] = (int)l;                                                     // l
    cachedInsets[1] = (int)(frameRect.size.width - (contentRect.size.width + l)); // r
    cachedInsets[2] = (jint)(frameRect.size.height - contentRect.size.height);    // t
    cachedInsets[3] = (jint)(contentRect.origin.y - frameRect.origin.y);          // b
    */
    UIEdgeInsets uiInsets = [self safeAreaInsets];
    cachedInsets[0] = uiInsets.left;
    cachedInsets[1] = uiInsets.right;
    cachedInsets[2] = uiInsets.top;
    cachedInsets[3] = uiInsets.bottom;
    DBG_PRINT( "updateInsets: [ l %d, r %d, t %d, b %d ]\n", cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);

    if( NULL != env && NULL != javaWin ) {
        (*env)->CallVoidMethod(env, javaWin, insetsChangedID, JNI_FALSE, cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);
    }
}

- (void) updateSizePosInsets: (JNIEnv*) env jwin: (jobject) javaWin defer: (jboolean)defer
{
    // update insets on every window resize for lack of better hook place
    [self updateInsets: NULL jwin:NULL];

    CGRect frameRect = [self frame];

    UIScreen* screen = [self screen];
    CGPoint pS = [self convertPoint: frameRect.origin toCoordinateSpace: screen.fixedCoordinateSpace];

    DBG_PRINT( "updateSize: [ w %d, h %d ], liveResize %d\n", (jint) frameRect.size.width, (jint) frameRect.size.height, (jint)withinLiveResize);
    DBG_PRINT( "updatePos: [ x %d, y %d ]\n", (jint) pS.x, (jint) pS.y);

    if( NULL != env && NULL != javaWin ) {
        (*env)->CallVoidMethod(env, javaWin, sizeScreenPosInsetsChangedID, defer,
                               (jint) pS.x, (jint) pS.y,
                               (jint) frameRect.size.width, (jint) frameRect.size.height,
                               cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3],
                               JNI_FALSE, // force
                               withinLiveResize
                              );
    }
}


- (void) attachToParent: (UIWindow*) parent
{
    /** TODO 
    DBG_PRINT( "attachToParent.1\n");
    [parent addChildWindow: self ordered: UIWindowAbove];
    DBG_PRINT( "attachToParent.2\n");
    [self setParentWindow: parent];
    DBG_PRINT( "attachToParent.X\n");
    */
}

- (void) detachFromParent: (UIWindow*) parent
{
    /** TODO 
    DBG_PRINT( "detachFromParent.1\n");
    [self setParentWindow: nil];
    if(NULL != parent) {
        DBG_PRINT( "detachFromParent.2\n");
        [parent removeChildWindow: self];
    }
    DBG_PRINT( "detachFromParent.X\n");
    */
}

/**
 * p rel client window position w/ top-left origin
 * returns: abs screen position w/ bottom-left origin
 */
- (CGPoint) newtRelClientTLWinPos2AbsTLScreenPos: (CGPoint) p
{
    return [self getLocationOnScreen: p];
}

- (CGSize) newtClientSize2TLSize: (CGSize) nsz
{
    CGSize topSZ = { nsz.width, nsz.height + cachedInsets[2] + cachedInsets[3] }; // height + insets.top + insets.bottom
    return topSZ;
}

/**
 * p rel client window position w/ top-left origin
 * returns: location in 0/0 top-left space.
 */
- (CGPoint) getLocationOnScreen: (CGPoint) p
{
    UIScreen* screen = [self screen];
    CGPoint pS = [self convertPoint: p toCoordinateSpace: screen.fixedCoordinateSpace];

#ifdef VERBOSE_ON
    CGRect winFrame = [self frame];
    DBG_PRINT( "getLocationOnScreen: point-in[%d/%d], winFrame[%d/%d %dx%d] -> %d/%d\n",
        (int)p.x, (int)p.y,
        (int)winFrame.origin.x, (int)winFrame.origin.y, (int)winFrame.size.width, (int)winFrame.size.height,
        (int)pS.x, (int)pS.y);
#endif

    return pS;
}

- (void) focusChanged: (BOOL) gained
{
    DBG_PRINT( "focusChanged: gained %d\n", gained);
    NewtUIView* newtView = (NewtUIView *) self; // TODO [self contentView];
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

- (void) flagsChanged:(UIEvent *) theEvent
{
    NSUInteger mods = [theEvent modifierFlags];
    NewtUIView* newtView = (NewtUIView *) [self contentView];
    if( [newtView isKindOfClass:[NewtUIView class]] ) {
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

- (void) windowDidResignKey: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidResignKey\n");
    // Implicit mouse exit by OS X
    [self focusChanged: NO];
}

- (void) windowWillStartLiveResize: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowWillStartLiveResize\n");
    withinLiveResize = JNI_TRUE;
}
- (void) windowDidEndLiveResize: (NSNotification *) notification
{
    DBG_PRINT( "*************** windowDidEndLiveResize\n");
    withinLiveResize = JNI_FALSE;
    [self sendResizeEvent];
}
- (CGSize) windowWillResize: (UIWindow *)sender toSize:(CGSize)frameSize
{
    DBG_PRINT( "*************** windowWillResize %lfx%lf\n", frameSize.width, frameSize.height);
    return frameSize;
}
- (void)windowDidResize: (NSNotification*) notification
{
    DBG_PRINT( "*************** windowDidResize\n");
    [self sendResizeEvent];
}

- (void) sendResizeEvent
{
    jobject javaWindowObject = NULL;
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);

    if( NULL == env ) {
        DBG_PRINT("windowDidResize: null JNIEnv\n");
        return;
    }
    NewtUIView* newtView = (NewtUIView *) [self contentView];
    if( [newtView isKindOfClass:[NewtUIView class]] ) {
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
    NewtUIView* newtView = (NewtUIView *) [self contentView];
    if( ! [newtView isKindOfClass:[NewtUIView class]] ) {
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

    CGPoint p0 = { 0, 0 };
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

    NewtUIView* newtView = (NewtUIView *) [self contentView];
    if( ! [newtView isKindOfClass:[NewtUIView class]] ) {
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

