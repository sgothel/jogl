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

#define IOS_TOUCH_COUNT 10

static jmethodID sendTouchScreenEventID = NULL;
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

@implementation NamedUITouch

- (id)initWithName:(UITouch*)t name:(short)n
{
    self->name = n;
    self->touch = [t retain];
    return self;
}
- (void) dealloc
{
    [touch release]; 
    [super dealloc];
}

- (BOOL)isEqual:(id)object
{
    // Ensure NSPointerFunctionsObjectPointerPersonality for NSArray
    return self == object;
}
- (NSUInteger)hash
{
    // Ensure NSPointerFunctionsObjectPointerPersonality for NSArray
    //
    // When building 32-bit applications, NSUInteger is a 32-bit unsigned integer. 
    // A 64-bit application treats NSUInteger as a 64-bit unsigned integer
    // https://developer.apple.com/documentation/objectivec/nsuinteger?language=objc
    return (NSUInteger)self;
}

@end

@implementation NewtUIView

- (id)initWithFrame:(CGRect)frameRect
{
    id res = [super initWithFrame:frameRect];
    CAEAGLLayer* l = (CAEAGLLayer*)[self layer];
    [l setOpaque: YES];
    l.drawableProperties = [NSDictionary dictionaryWithObjectsAndKeys: /* defaults */
                           [NSNumber numberWithBool:NO], kEAGLDrawablePropertyRetainedBacking, kEAGLColorFormatRGBA8, kEAGLDrawablePropertyColorFormat, nil];
    [self setMultipleTouchEnabled: YES]; // NEWT supports multitouch ..
    [self setExclusiveTouch: YES]; // NEWT touches shall keep with NEWT
    [self setUserInteractionEnabled: YES]; // Default ..

    // NSMapTable<UITouch*, NamedUITouch*>* activeTouchMap;
    // NSHashTable<NamedUITouch*>* activeTouches;
    activeTouchMap = [[NSMapTable alloc] initWithKeyOptions:NSMapTableStrongMemory|NSMapTableObjectPointerPersonality
                                         valueOptions:NSMapTableStrongMemory|NSMapTableObjectPointerPersonality
                                         capacity:IOS_TOUCH_COUNT];
    activeTouches = [[NSMutableArray alloc] initWithCapacity:IOS_TOUCH_COUNT];
    nextTouchName = 0;

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

    DBG_PRINT("NewtUIView::create: %p/%p, CAEAGLLayer %p (pixelScale %f, isCAEAGLLayer %d) (res refcnt %d)\n", 
        res, self, l, [l contentsScale], [l isKindOfClass:[CAEAGLLayer class]], (int)[res retainCount]);
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
    DBG_PRINT("NewtUIView::dealloc.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
#ifdef DBG_LIFECYCLE
    NSLog(@"%@",[NSThread callStackSymbols]);
#endif
    if( 0 < softLockCount ) {
        NSLog(@"NewtUIView::dealloc: softLock still hold @ dealloc!\n");
    }
    [activeTouchMap removeAllObjects];
    [activeTouchMap release];
    [activeTouches removeAllObjects];
    [activeTouches release];

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
    int err;
    if( 0 != ( err = pthread_mutex_lock(&softLockSync) ) ) {
        NSLog(@"NewtUIView::softLock failed: errCode %d - %@", err, [NSThread callStackSymbols]);
        return NO;
    }
    softLockCount++;
    // DBG_PRINT("*************** softLock: %p count %d\n", (void*)pthread_self(), softLockCount);
    return 0 < softLockCount;
}

- (BOOL) softUnlock
{
    softLockCount--;
    // DBG_PRINT("*************** softUnlock: %p count %d\n", (void*)pthread_self(), softLockCount);
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
    DBG_PRINT("*************** drawRect: dirtyRect: %p %lf/%lf %lfx%lf\n", 
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

- (void)touchesBegan:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event
{
    [self sendTouchEvent: touches withEvent:event eventState:1 newtEventType:(short)EVENT_MOUSE_PRESSED];
}
- (void)touchesMoved:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event
{
    // Note use of MOUSE_MOVED event type because mouse dragged events are synthesized by Java
    [self sendTouchEvent: touches withEvent:event eventState:0 newtEventType:(short)EVENT_MOUSE_MOVED];
}
- (void)touchesEnded:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event
{
    [self sendTouchEvent: touches withEvent:event eventState:-1 newtEventType:(short)EVENT_MOUSE_RELEASED];
}
- (void)touchesCancelled:(NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event
{
    [self sendTouchEvent: touches withEvent:event eventState:-1 newtEventType:(short)EVENT_MOUSE_RELEASED];
}
- (void)touchesEstimatedPropertiesUpdated:(NSSet<UITouch *> *)touches
{
}

- (void)sendTouchEvent: (NSSet<UITouch *> *)touches withEvent:(nullable UIEvent *)event 
                        eventState:(int)eventState newtEventType:(short)newtEventType
{
    if (javaWindowObject == NULL) {
        DBG_PRINT("sendTouchEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("sendTouchEvent: null JNIEnv\n");
        return;
    }
    jint newtEventModifiers = 0; // FIXME?
    jint touchTypes[IOS_TOUCH_COUNT];
    jshort pointerNames[IOS_TOUCH_COUNT];
    jint x[IOS_TOUCH_COUNT];
    jint y[IOS_TOUCH_COUNT];
    jfloat pressure[IOS_TOUCH_COUNT];
    jint actionIdx[IOS_TOUCH_COUNT];
    int activeTouchesNewIdx = -1;

    DBG_PRINT( "sendTouchEvent.0: Window %p, state %d, newtType %d, touches %d, activeTouches %d, nextTouchName %d\n", 
        (void*)javaWindowObject, eventState, (int)newtEventType, (int)[touches count], (int)[activeTouches count], nextTouchName);

    // merge new touches into activeTouchMap (unify) and 
    // add to end of activeTouches array 
    {
        NSEnumerator<UITouch*>* touchesEnum = [touches objectEnumerator];
        UITouch * t;
        while( (t = [touchesEnum nextObject]) ) {
            NamedUITouch *nt = (NamedUITouch*)[activeTouchMap objectForKey: t];
            if( nil == nt ) {
                if( 1 != eventState ) {
                    // Ooops, not 'touchesBegan' but UITouch not mapped!
                    NewtCommon_throwNewRuntimeException(env, "Internal Error: touch event (window %p) state %d, newtType %d not mapped", 
                        (void*)javaWindowObject, eventState, (int)newtEventType);
                }
                if( IOS_TOUCH_COUNT > [activeTouches count] ) {
                    nt = [[NamedUITouch alloc] initWithName:t name:nextTouchName++];
                    [activeTouchMap setObject:nt forKey:t];
                    [activeTouches addObject: nt];
                    if( 0 > activeTouchesNewIdx ) {
                        activeTouchesNewIdx = [activeTouches count] - 1;
                    }
                }
            }
        }
    }
#ifdef VERBOSE_ON
    {
        int activeTouchesNewCount = activeTouchesNewIdx < 0 ? 0 : [activeTouches count] - activeTouchesNewIdx;
        DBG_PRINT( "sendTouchEvent.1: Window %p, state %d, newtType %d, touches %d, activeTouches %d, nextTouchName %d, newActiveTouches %d\n", 
        (void*)javaWindowObject, eventState, (int)newtEventType, (int)[touches count], (int)[activeTouches count], nextTouchName, activeTouchesNewCount);
    }
#endif /* VERBOSE_ON */

    int cnt, actionCnt;
    for(cnt=0, actionCnt=0; cnt<[activeTouches count]; cnt++) {
        NamedUITouch *nt = [activeTouches objectAtIndex: cnt];
        switch( [nt->touch type] ) {
            case UITouchTypeDirect:
            case UITouchTypeIndirect: /* ??? */
                touchTypes[cnt] = POINTER_TYPE_TOUCHSCREEN;
                break;
            case UITouchTypePencil:
                touchTypes[cnt] = POINTER_TYPE_PEN;
                break;
            default:
                touchTypes[cnt] = POINTER_TYPE_UNDEF;
                break;
        }
        CGPoint loc = [nt->touch preciseLocationInView: self]; // [touch locationInView: self];
        x[cnt] = (jint)(loc.x);
        y[cnt] = (jint)(loc.y);
        pressure[cnt] = (jfloat)(nt->touch.force);
        pointerNames[cnt] = nt->name;
        if( [touches member: nt->touch] ) {
            actionIdx[actionCnt++] = cnt;
            DBG_PRINT( "sendTouchEvent.2: Window %p, action-touchid[%d]: name %d, idx %d, ptr: %d/%d\n",
                (void*)javaWindowObject, (actionCnt-1), nt->name, cnt, x[cnt], y[cnt]);
        } else {
            DBG_PRINT( "sendTouchEvent.2: Window %p, action-touchid[-1]: name %d, idx %d, ptr: %d/%d\n",
                (void*)javaWindowObject, nt->name, cnt, x[cnt], y[cnt]);
        }
    }
    if( 0 >= actionCnt || actionCnt != [touches count]) {
        NewtCommon_throwNewRuntimeException(env, "Internal Error: touch event (window %p) %d actionIds not matching %d/%d touches", 
            (void*)javaWindowObject, actionCnt, (int)[touches count], cnt);
    }
    if( -1 == eventState ) {
        NSEnumerator<UITouch*>* touchesEnum = [touches objectEnumerator];
        UITouch * t;
        while( (t = [touchesEnum nextObject]) ) {
            NamedUITouch *nt = (NamedUITouch*)[activeTouchMap objectForKey: t];
            if( nil == nt ) {
                // Ooops, 'touchesEnded' but UITouch not mapped!
                NewtCommon_throwNewRuntimeException(env, "Internal Error: touch event (window %p) state %d, newtType %d not mapped", 
                    (void*)javaWindowObject, eventState, (int)newtEventType);
            }
            [activeTouchMap removeObjectForKey: t];
            [activeTouches removeObject: nt];
        }
        if( 0 == [activeTouches count] ) {
            // all finger released ..
            nextTouchName = 0;
        }
    }
    DBG_PRINT( "sendTouchEvent.3: Window %p, state %d, newtType %d, touches %d, activeTouches %d, nextTouchName %d\n", 
        (void*)javaWindowObject, eventState, (int)newtEventType, (int)[touches count], (int)[activeTouches count], nextTouchName);

    jintArray jActionIdx = (*env)->NewIntArray(env, actionCnt);
    if (jActionIdx == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (names) of size %d", actionCnt);
    }
    (*env)->SetIntArrayRegion(env, jActionIdx, 0, actionCnt, actionIdx);

    jshortArray jNames = (*env)->NewShortArray(env, cnt);
    if (jNames == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate short array (names) of size %d", cnt);
    }
    (*env)->SetShortArrayRegion(env, jNames, 0, cnt, pointerNames);

    jintArray jTouchTypes = (*env)->NewIntArray(env, cnt);
    if (jTouchTypes == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (TouchTypes) of size %d", cnt);
    }
    (*env)->SetIntArrayRegion(env, jTouchTypes, 0, cnt, touchTypes);

    jintArray jX = (*env)->NewIntArray(env, cnt);
    if (jX == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (x) of size %d", cnt);
    }
    (*env)->SetIntArrayRegion(env, jX, 0, cnt, x);

    jintArray jY = (*env)->NewIntArray(env, cnt);
    if (jY == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate int array (y) of size %d", cnt);
    }
    (*env)->SetIntArrayRegion(env, jY, 0, cnt, y);

    jfloatArray jPressure = (*env)->NewFloatArray(env, cnt);
    if (jPressure == NULL) {
        NewtCommon_throwNewRuntimeException(env, "Could not allocate float array (pressure) of size %d", cnt);
    }
    (*env)->SetFloatArrayRegion(env, jPressure, 0, cnt, pressure);

    /** 
     * Pressure (force) "1.0 represents the force of an average touch (predetermined by the system, not user-specific".
     * So we pass 2.0f as the maxPressure value.
     */
    (*env)->CallVoidMethod(env, javaWindowObject, sendTouchScreenEventID,
            (jshort)newtEventType, (jint)newtEventModifiers, 
            jActionIdx, jNames, jTouchTypes, jX, jY, jPressure, (jfloat)2.0f); 

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

@end

@implementation NewtUIWindow

+ (BOOL) initNatives: (JNIEnv*) env forClass: (jclass) clazz
{
    sendTouchScreenEventID = (*env)->GetMethodID(env, clazz, "sendTouchScreenEvent", "(SI[I[S[I[I[I[FF)V");
    sizeChangedID = (*env)->GetMethodID(env, clazz, "sizeChanged", "(ZIIZ)V");
    updatePixelScaleID = (*env)->GetMethodID(env, clazz, "updatePixelScale", "(ZFFFZ)V");
    visibleChangedID = (*env)->GetMethodID(env, clazz, "visibleChanged", "(Z)V");
    insetsChangedID = (*env)->GetMethodID(env, clazz, "insetsChanged", "(IIII)V");
    sizeScreenPosInsetsChangedID = (*env)->GetMethodID(env, clazz, "sizeScreenPosInsetsChanged", "(ZIIIIIIIIZZ)V");
    screenPositionChangedID = (*env)->GetMethodID(env, clazz, "screenPositionChanged", "(ZII)V");
    focusChangedID = (*env)->GetMethodID(env, clazz, "focusChanged", "(ZZ)V");
    windowDestroyNotifyID = (*env)->GetMethodID(env, clazz, "windowDestroyNotify", "(Z)Z");
    windowRepaintID = (*env)->GetMethodID(env, clazz, "windowRepaint", "(ZIIII)V");
    requestFocusID = (*env)->GetMethodID(env, clazz, "requestFocus", "(Z)V");
    if (sendTouchScreenEventID && sizeChangedID && updatePixelScaleID && visibleChangedID && 
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
    id res = [super initWithFrame: contentRect];
    self.rootViewController = [[[UIViewController alloc] initWithNibName:nil bundle:nil] autorelease];
    isFullscreenWindow = NO; // TODO isfs;

    cachedInsets[0] = 0; // l
    cachedInsets[1] = 0; // r
    cachedInsets[2] = 0; // t
    cachedInsets[3] = 0; // b

    realized = YES;
    withinLiveResize = JNI_FALSE;
    contentNewtUIView = NULL;

    [[NSNotificationCenter defaultCenter] addObserver:self
                                          selector:@selector(becameVisible:)
                                          name:UIWindowDidBecomeVisibleNotification
                                          object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                          selector:@selector(becameHidden:)
                                          name:UIWindowDidBecomeHiddenNotification
                                          object:nil];

    DBG_PRINT("NewtWindow::create: %p, realized %d, (refcnt %d)\n", 
        res, realized, (int)[res retainCount]);
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

    [[NSNotificationCenter defaultCenter] removeObserver:self];

    if( NULL != contentNewtUIView ) {
        [contentNewtUIView removeFromSuperview];
        [contentNewtUIView release];
        contentNewtUIView=NULL;
    }
    [super dealloc];
    DBG_PRINT("NewtWindow::dealloc.X: %p\n", self);
}

- (void) setContentNewtUIView: (NewtUIView*)v
{
    DBG_PRINT( "NewtWindow::setContentNewtUIView.0: view %p -> %p\n", contentNewtUIView, v);
    if( NULL != contentNewtUIView ) {
        [contentNewtUIView removeFromSuperview];
        [contentNewtUIView release];
        contentNewtUIView=NULL;
    }
    contentNewtUIView = v;
    if( NULL != contentNewtUIView ) {
        [contentNewtUIView retain];
        [self addSubview: contentNewtUIView];
    }
}
- (NewtUIView*) getContentNewtUIView
{
    return contentNewtUIView;
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

- (void) updatePixelScale: (BOOL) defer
{
    NewtUIView* newtView = contentNewtUIView;
    DBG_PRINT( "updatePixelScale view %p, autoMaxPixelScale %d, defer %d\n", 
        newtView, useAutoMaxPixelScale, defer);
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
        DBG_PRINT("updatePixelScale: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("updatePixelScale: null JNIEnv\n");
        return;
    }

    CGFloat oldPixelScaleV = [newtView contentScaleFactor];
    CGFloat oldPixelScaleL = [[newtView layer] contentsScale];
    UIScreen* _screen = [self screen];
    CGFloat maxPixelScale = [_screen scale];
    CGFloat pixelScale;
    BOOL changeScale;
    if ( useAutoMaxPixelScale ) {
        pixelScale = maxPixelScale;
        changeScale = pixelScale != oldPixelScaleV || pixelScale != oldPixelScaleL;
    } else {
        pixelScale = oldPixelScaleV;
        changeScale = NO;
    }
    DBG_PRINT("updatePixelScale: PixelScale: autoMaxPixelScale %d, max %f, view %f, layer %f -> %f (change %d)\n", 
        useAutoMaxPixelScale, (float)maxPixelScale, (float)oldPixelScaleV, (float)oldPixelScaleL, (float)pixelScale, changeScale);
    if( changeScale ) {
        [newtView setContentScaleFactor: pixelScale];
        [[newtView layer] setContentsScale: pixelScale];
    }
    (*env)->CallVoidMethod(env, javaWindowObject, updatePixelScaleID, defer?JNI_TRUE:JNI_FALSE, 
                          (jfloat)oldPixelScaleV, (jfloat)pixelScale, (jfloat)maxPixelScale, (jboolean)changeScale);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}
- (void) setPixelScale: (CGFloat)reqPixelScale defer:(BOOL)defer
{
    NewtUIView* newtView = contentNewtUIView;
    useAutoMaxPixelScale = NewtCommon_isFloatZero(reqPixelScale);
    DBG_PRINT( "setPixelScale view %p, reqPixelScale %f, autoMaxPixelScale %d, defer %d\n", 
        newtView, reqPixelScale, useAutoMaxPixelScale, defer);
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
        DBG_PRINT("setPixelScale: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("setPixelScale: null JNIEnv\n");
        return;
    }

    CGFloat oldPixelScaleV = [newtView contentScaleFactor];
    CGFloat oldPixelScaleL = [[newtView layer] contentsScale];
    UIScreen* _screen = [self screen];
    {
         CGRect _bounds = [_screen bounds];
         CGRect _nativeBounds = [_screen nativeBounds];
         CGFloat _scale = [_screen scale];
         CGFloat _nativeScale = [_screen nativeScale];
         DBG_PRINT("setPixelScale: screen %p[native %f/%f %fx%f scale %f; logical %f/%f %fx%f scale %f]\n",
            _screen,
            _nativeBounds.origin.x, _nativeBounds.origin.y, _nativeBounds.size.width, _nativeBounds.size.height, _nativeScale,
            _bounds.origin.x, _bounds.origin.y, _bounds.size.width, _bounds.size.height, _scale);
    }
    CGFloat maxPixelScale = [_screen scale];
    CGFloat pixelScale;
    BOOL changeScale;
    if ( useAutoMaxPixelScale || maxPixelScale < reqPixelScale ) {
        pixelScale = maxPixelScale;
    } else if( 0 > reqPixelScale ) {
        pixelScale = 1.0f;
    } else {
        pixelScale = reqPixelScale;
    }
    changeScale = pixelScale != oldPixelScaleV || pixelScale != oldPixelScaleL;
    DBG_PRINT("setPixelScale: PixelScale: autoMaxPixelScale %d, max %f, view %f, layer %f, req %f -> %f (change %d)\n", 
        useAutoMaxPixelScale, (float)maxPixelScale, (float)oldPixelScaleV, (float)oldPixelScaleL, (float)reqPixelScale, (float)pixelScale, changeScale);

    if( changeScale ) {
        [newtView setContentScaleFactor: pixelScale];
        [[newtView layer] setContentsScale: pixelScale];
    }
    (*env)->CallVoidMethod(env, javaWindowObject, updatePixelScaleID, defer?JNI_TRUE:JNI_FALSE, 
                          (jfloat)oldPixelScaleV, (jfloat)pixelScale, (jfloat)maxPixelScale, (jboolean)changeScale);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
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
        (*env)->CallVoidMethod(env, javaWin, insetsChangedID, cachedInsets[0], cachedInsets[1], cachedInsets[2], cachedInsets[3]);
    }
}

- (void) updateSizePosInsets: (JNIEnv*) env jwin: (jobject) javaWin defer: (jboolean)defer
{
    // update insets on every window resize for lack of better hook place
    [self updateInsets: NULL jwin:NULL];

    CGRect frameRect = [self frame];

    UIScreen* _screen = [self screen];
    CGPoint pS = [self convertPoint: frameRect.origin toCoordinateSpace: _screen.fixedCoordinateSpace];

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
    DBG_PRINT( "attachToParent.1\n");
    [parent addSubview: self];
    // [self setwindowLevel: [parent windowLevel]+1.0f];
    // NSWindow: [parent addChildWindow: self ordered: UIWindowAbove];
    // NSWindow: [self setParentWindow: parent];
    DBG_PRINT( "attachToParent.X\n");
}

- (void) detachFromParent: (UIWindow*) parent
{
    DBG_PRINT( "detachFromParent.1\n");
    [self removeFromSuperview];
    /**
    // NSWindow:
    [self setParentWindow: nil];
    if(NULL != parent) {
        DBG_PRINT( "detachFromParent.2\n");
        [parent removeChildWindow: self];
    }
    */
    DBG_PRINT( "detachFromParent.X\n");
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
    UIScreen* _screen = [self screen];
    CGPoint pS = [self convertPoint: p toCoordinateSpace: _screen.fixedCoordinateSpace];

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
    NewtUIView* newtView = contentNewtUIView;
    DBG_PRINT( "focusChanged: gained %d, view %p\n", gained, newtView);
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
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

- (void) visibilityChanged: (BOOL) visible
{
    DBG_PRINT( "visibilityChanged: visible %d\n", visible);
    NewtUIView* newtView = contentNewtUIView;
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
        DBG_PRINT("visibilityChanged: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if(NULL==env) {
        DBG_PRINT("visibilityChanged: null JNIEnv\n");
        return;
    }
    (*env)->CallVoidMethod(env, javaWindowObject, visibleChangedID, (visible == YES) ? JNI_TRUE : JNI_FALSE);

    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (BOOL) canBecomeFirstResponder 
{
    return YES;
}
- (BOOL) becomeFirstResponder
{
    DBG_PRINT( "*************** Win.becomeFirstResponder\n");
    return [super becomeFirstResponder];
}

- (BOOL) canResignFirstResponder
{
    return YES;
}
- (BOOL) resignFirstResponder
{
    DBG_PRINT( "*************** Win.resignFirstResponder\n");
    return [super resignFirstResponder];
}

- (void) becomeKeyWindow
{
    DBG_PRINT( "*************** becomeKeyWindow\n");
    [super becomeKeyWindow];
    [self focusChanged: YES];
}

- (void) resignKeyWindow
{
    DBG_PRINT( "*************** resignKeyWindow: isFullscreen %d\n", (int)isFullscreenWindow);
    if(!isFullscreenWindow) {
        [super resignKeyWindow];
    }
    [self focusChanged: NO];
}

- (void) becameVisible: (NSNotification*)notice
{
    DBG_PRINT( "*************** becameVisible\n");
    [self visibilityChanged: YES];
}

- (void) becameHidden: (NSNotification*)notice
{
    DBG_PRINT( "*************** becameHidden\n");
    [self visibilityChanged: NO];
}

- (void) sendResizeEvent
{
    // FIXME: Needs to be called
    NewtUIView* newtView = contentNewtUIView;
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
        DBG_PRINT("sendResizeEvent: null javaWindowObject\n");
        return;
    }
    int shallBeDetached = 0;
    JNIEnv* env = NewtCommon_GetJNIEnv(1 /* asDaemon */, &shallBeDetached);
    if( NULL == env ) {
        DBG_PRINT("windowDidResize: null JNIEnv\n");
        return;
    }
    [self updateSizePosInsets: env jwin: javaWindowObject defer:JNI_TRUE];
    // detaching thread not required - daemon
    // NewtCommon_ReleaseJNIEnv(shallBeDetached);
}

- (void)windowDidMove: (NSNotification*) notification
{
    // FIXME: Needs to be called
    NewtUIView* newtView = contentNewtUIView;
    if( NULL == newtView ) {
        return;
    }
    jobject javaWindowObject = [newtView getJavaWindowObject];
    if ( NULL == javaWindowObject ) {
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

- (BOOL) windowClosingImpl: (BOOL) force
{
    // FIXME: Needs to be called
    jboolean closed = JNI_FALSE;

    NewtUIView* newtView = contentNewtUIView;
    if( NULL == newtView ) {
        return NO;
    }
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    if( false == [newtView getDestroyNotifySent] ) {
        jobject javaWindowObject = [newtView getJavaWindowObject];
        DBG_PRINT( "*************** windowWillClose.0: %p\n", (void *)(intptr_t)javaWindowObject);
        if ( NULL == javaWindowObject ) {
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

