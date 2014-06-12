#import "MacOSXWindowSystemInterface.h"
#import <QuartzCore/QuartzCore.h>
#import <pthread.h>
#import "NativeWindowProtocols.h"
#include "timespec.h"

#import <OpenGL/glext.h>

/** 
 * Partial include of gl3.h - which we can only expect and use 
 * in case of a GL3 core context at runtime.
 * Otherwise we would need to have 2 modules, one including GL2
 * and one inclusing GL3 headers.
 */
#ifndef GL_ARB_vertex_array_object
#define GL_VERTEX_ARRAY_BINDING           0x85B5
extern void glBindVertexArray (GLuint array);
extern void glDeleteVertexArrays (GLsizei n, const GLuint *arrays);
extern void glGenVertexArrays (GLsizei n, GLuint *arrays);
extern GLboolean glIsVertexArray (GLuint array);
#endif

// 
// CADisplayLink only available on iOS >= 3.1, sad, since it's convenient.
// Use CVDisplayLink otherwise.
//
// #define HAS_CADisplayLink 1
//

// lock/sync debug output
//
// #define DBG_SYNC 1
//
#ifdef DBG_SYNC
    // #define SYNC_PRINT(...) NSLog(@ ## __VA_ARGS__)
    #define SYNC_PRINT(...) fprintf(stderr, __VA_ARGS__); fflush(stderr)
#else
    #define SYNC_PRINT(...)
#endif

// fps debug output
//
// #define DBG_PERF 1

// #define DBG_LIFECYCLE 1

/**
 * Capture setView(NULL), which produces a 'invalid drawable' message
 *
 * Also track lifecycle via DBG_PRINT messages, if VERBOSE is enabled!
 */
@interface MyNSOpenGLContext: NSOpenGLContext
{
}
- (id)initWithFormat:(NSOpenGLPixelFormat *)format shareContext:(NSOpenGLContext *)share;
- (void)setView:(NSView *)view;
- (void)update;
#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
#endif
- (void)dealloc;

@end

@implementation MyNSOpenGLContext

- (id)initWithFormat:(NSOpenGLPixelFormat *)format shareContext:(NSOpenGLContext *)share
{
    DBG_PRINT("MyNSOpenGLContext::initWithFormat.0: format %p, share %p\n", format, share);
    MyNSOpenGLContext * o = [super initWithFormat:format shareContext:share];
    DBG_PRINT("MyNSOpenGLContext::initWithFormat.X: new %p\n", o);
    return o;
}

- (void)setView:(NSView *)view
{
    DBG_PRINT("MyNSOpenGLContext::setView: this.0 %p, view %p\n", self, view);
    // NSLog(@"MyNSOpenGLContext::setView: %@",[NSThread callStackSymbols]);
    if(NULL != view) {
        [super setView:view];
    } else {
        [self clearDrawable];
    }
    DBG_PRINT("MyNSOpenGLContext::setView.X\n");
}

- (void)update
{
    DBG_PRINT("MyNSOpenGLContext::update: this.0 %p, view %p\n", self, [self view]);
    [super update];
    DBG_PRINT("MyNSOpenGLContext::update.X\n");
}

#ifdef DBG_LIFECYCLE

- (id)retain
{
    DBG_PRINT("MyNSOpenGLContext::retain.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyNSOpenGLContext::retain: %@",[NSThread callStackSymbols]);
    id o = [super retain];
    DBG_PRINT("MyNSOpenGLContext::retain.X: %p (refcnt %d)\n", o, (int)[o retainCount]);
    return o;
}

- (oneway void)release
{
    DBG_PRINT("MyNSOpenGLContext::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    [super release];
    // DBG_PRINT("MyNSOpenGLContext::release.X: %p (refcnt %d)\n", self, (int)[self retainCount]);
}

#endif

- (void)dealloc
{
    DBG_PRINT("MyNSOpenGLContext::dealloc.0 %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyNSOpenGLContext::dealloc: %@",[NSThread callStackSymbols]);

    [self clearDrawable];

    [super dealloc];
    DBG_PRINT("MyNSOpenGLContext::dealloc.X\n");
}

@end

@interface MyNSOpenGLLayer: NSOpenGLLayer <NWDedicatedFrame>
{
@private
    GLfloat gl_texCoords[8];
    NSOpenGLContext* glContext;
    Bool isGLEnabled;

@protected
    GLuint gl3ShaderProgramName;
    GLuint vboBufVert;
    GLuint vboBufTexCoord;
    GLint vertAttrLoc;
    GLint texCoordAttrLoc;
    NSOpenGLPixelFormat* parentPixelFmt;
    int texWidth;
    int texHeight;
    volatile Bool dedicatedFrameSet;
    volatile CGRect dedicatedFrame;
    volatile NSOpenGLPixelBuffer* pbuffer;
    volatile GLuint textureID;
    volatile NSOpenGLPixelBuffer* newPBuffer;
#ifdef HAS_CADisplayLink
    CADisplayLink* displayLink;
#else
    CVDisplayLinkRef displayLink;
#endif
    int tc;
    struct timespec tStart;
@public
    struct timespec lastWaitTime;
    GLint swapInterval;
    GLint swapIntervalCounter;
    pthread_mutex_t renderLock;
    pthread_cond_t renderSignal;
    volatile Bool shallDraw;
}

- (id) setupWithContext: (NSOpenGLContext*) parentCtx
       gl3ShaderProgramName: (GLuint) gl3ShaderProgramName
       pixelFormat: (NSOpenGLPixelFormat*) pfmt
       pbuffer: (NSOpenGLPixelBuffer*) p
       texIDArg: (GLuint) texID
       opaque: (Bool) opaque
       texWidth: (int) texWidth 
       texHeight: (int) texHeight
       winWidth: (int)winWidth 
       winHeight: (int)winHeight;

- (void)releaseLayer;
- (void)deallocPBuffer;
- (void)disableAnimation;
- (void)pauseAnimation:(Bool)pause;
- (void)setSwapInterval:(int)interval;
- (void)tick;
- (void)waitUntilRenderSignal: (long) to_micros;
- (Bool)isGLSourceValid;

- (void) setGLEnabled: (Bool) enable;
- (Bool) validateTexSize: (int)newTexWidth height:(int)newTexHeight;
- (void) setTextureID: (int) _texID;

- (Bool) isSamePBuffer: (NSOpenGLPixelBuffer*) p;
- (void) setNewPBuffer: (NSOpenGLPixelBuffer*)p;
- (void) applyNewPBuffer;

- (void)setDedicatedFrame:(CGRect)frame quirks:(int)quirks; // @NWDedicatedFrame
- (void) setFrame:(CGRect) frame;
- (id<CAAction>)actionForKey:(NSString *)key ;
- (NSOpenGLPixelFormat *)openGLPixelFormatForDisplayMask:(uint32_t)mask;
- (NSOpenGLContext *)openGLContextForPixelFormat:(NSOpenGLPixelFormat *)pixelFormat;
- (BOOL)canDrawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp;
- (void)drawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp;

#ifdef DBG_LIFECYCLE
- (id)retain;
- (oneway void)release;
#endif
- (void)dealloc;

@end

#ifndef HAS_CADisplayLink

static CVReturn renderMyNSOpenGLLayer(CVDisplayLinkRef displayLink, 
                                      const CVTimeStamp *inNow, 
                                      const CVTimeStamp *inOutputTime, 
                                      CVOptionFlags flagsIn, 
                                      CVOptionFlags *flagsOut, 
                                      void *displayLinkContext)
{
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*)displayLinkContext;
    pthread_mutex_lock(&l->renderLock);
    if( 0 < l->swapInterval ) {
        l->swapIntervalCounter++;
        if( l->swapIntervalCounter >= l->swapInterval ) {
            SYNC_PRINT("<S %d/%d>", (int)l->swapIntervalCounter, l->swapInterval);
            l->swapIntervalCounter = 0;
            pthread_cond_signal(&l->renderSignal); // wake up vsync
        }
    }
    pthread_mutex_unlock(&l->renderLock);
    [pool release];
    return kCVReturnSuccess;
}

#endif

static const GLfloat gl_verts[] = {
    -1.0, -1.0,
    -1.0,  1.0,
     1.0,  1.0,
     1.0, -1.0
};

@implementation MyNSOpenGLLayer

- (id) setupWithContext: (NSOpenGLContext*) parentCtx
       gl3ShaderProgramName: (GLuint) _gl3ShaderProgramName
       pixelFormat: (NSOpenGLPixelFormat*) _parentPixelFmt
       pbuffer: (NSOpenGLPixelBuffer*) p
       texIDArg: (GLuint) texID
       opaque: (Bool) opaque
       texWidth: (int) _texWidth 
       texHeight: (int) _texHeight
       winWidth: (int) _winWidth 
       winHeight: (int) _winHeight
{
    pthread_mutexattr_t renderLockAttr;
    pthread_mutexattr_init(&renderLockAttr);
    pthread_mutexattr_settype(&renderLockAttr, PTHREAD_MUTEX_RECURSIVE);
    pthread_mutex_init(&renderLock, &renderLockAttr); // recursive
    pthread_cond_init(&renderSignal, NULL); // no attribute

    {
        int i;
        for(i=0; i<8; i++) {
            gl_texCoords[i] = 0.0f;
        }
    }
    /** 
     * Set via 
     *   - OSXUtil_SetCALayerPixelScale0
     *   - OSXUtil_AddCASublayer0 
NS_DURING
    // Available >= 10.7
    [self setContentsScale: (CGFloat)_texWidth/(CGFloat)_winWidth];
NS_HANDLER
NS_ENDHANDLER
    */

    parentPixelFmt = [_parentPixelFmt retain]; // until destruction
    glContext = [[MyNSOpenGLContext alloc] initWithFormat:parentPixelFmt shareContext:parentCtx];
    gl3ShaderProgramName = _gl3ShaderProgramName;
    vboBufVert = 0;
    vboBufTexCoord = 0;
    vertAttrLoc = 0;
    texCoordAttrLoc = 0;
    swapInterval = 1; // defaults to on (as w/ new GL profiles)
    swapIntervalCounter = 0;
    timespec_now(&lastWaitTime);
    shallDraw = NO;
    isGLEnabled = YES;
    dedicatedFrameSet = NO;
    dedicatedFrame = CGRectMake(0, 0, _winWidth, _winHeight);
    [self validateTexSize: _texWidth height:_texHeight];
    [self setTextureID: texID];

    newPBuffer = NULL;
    pbuffer = p;
    if(NULL != pbuffer) {
        [pbuffer retain];
    }

    {
        // no animations for add/remove/swap sublayers etc 
        // doesn't work: [self removeAnimationForKey: kCAOnOrderIn, kCAOnOrderOut, kCATransition]
        [self removeAllAnimations];
    }

    // instantiate a deactivated displayLink
#ifdef HAS_CADisplayLink
    displayLink = [[CVDisplayLink displayLinkWithTarget:self selector:@selector(setNeedsDisplay)] retain];
#else
    CVReturn cvres;
    {
        int allDisplaysMask = 0;
        int virtualScreen, accelerated, displayMask;
        for (virtualScreen = 0; virtualScreen < [parentPixelFmt  numberOfVirtualScreens]; virtualScreen++) {
            [parentPixelFmt getValues:&displayMask forAttribute:NSOpenGLPFAScreenMask forVirtualScreen:virtualScreen];
            [parentPixelFmt getValues:&accelerated forAttribute:NSOpenGLPFAAccelerated forVirtualScreen:virtualScreen];
            if (accelerated) {
                allDisplaysMask |= displayMask;
            }
        }
        cvres = CVDisplayLinkCreateWithOpenGLDisplayMask(allDisplaysMask, &displayLink);
        if(kCVReturnSuccess != cvres) {
            DBG_PRINT("MyNSOpenGLLayer::init %p, CVDisplayLinkCreateWithOpenGLDisplayMask %X failed: %d\n", self, allDisplaysMask, cvres);
            displayLink = NULL;
        }
    }
    if(NULL != displayLink) {
        CVReturn cvres;
        DBG_PRINT("MyNSOpenGLLayer::openGLContextForPixelFormat.1: setup DisplayLink %p\n", displayLink);
        cvres = CVDisplayLinkSetCurrentCGDisplayFromOpenGLContext(displayLink, [glContext CGLContextObj], [parentPixelFmt CGLPixelFormatObj]);
        if(kCVReturnSuccess != cvres) {
            DBG_PRINT("MyNSOpenGLLayer::init %p, CVDisplayLinkSetCurrentCGDisplayFromOpenGLContext failed: %d\n", self, cvres);
        }
    }
    if(NULL != displayLink) {
        cvres = CVDisplayLinkSetOutputCallback(displayLink, renderMyNSOpenGLLayer, self);
        if(kCVReturnSuccess != cvres) {
            DBG_PRINT("MyNSOpenGLLayer::init %p, CVDisplayLinkSetOutputCallback failed: %d\n", self, cvres);
            displayLink = NULL;
        }
    }
#endif
    [self pauseAnimation: YES];

    [self removeAllAnimations];
    [self setAutoresizingMask: (kCALayerWidthSizable|kCALayerHeightSizable)];
    [self setNeedsDisplayOnBoundsChange: YES];

    [self setOpaque: opaque ? YES : NO];

#ifdef VERBOSE_ON
    CGRect lRect = [self bounds];
    if(NULL != pbuffer) {
        DBG_PRINT("MyNSOpenGLLayer::init (pbuffer) %p, pctx %p, pfmt %p, pbuffer %p, ctx %p, opaque %d, pbuffer %dx%d -> tex %dx%d, bounds: %lf/%lf %lfx%lf, displayLink %p (refcnt %d)\n", 
            self, parentCtx, parentPixelFmt, pbuffer, glContext, opaque, [pbuffer pixelsWide], [pbuffer pixelsHigh], texWidth, texHeight,
            lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height, displayLink, (int)[self retainCount]);
    } else {
        DBG_PRINT("MyNSOpenGLLayer::init (texture) %p, pctx %p, pfmt %p, ctx %p, opaque %d, tex[id %d, %dx%d], bounds: %lf/%lf %lfx%lf, displayLink %p (refcnt %d)\n", 
            self, parentCtx, parentPixelFmt, glContext, opaque, (int)textureID, texWidth, texHeight,
            lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height, displayLink, (int)[self retainCount]);
    }
#endif
    return self;
}

- (void) setGLEnabled: (Bool) enable
{
    DBG_PRINT("MyNSOpenGLLayer::setGLEnabled: %p, %d -> %d\n", self, (int)isGLEnabled, (int)enable);
    isGLEnabled = enable;
}

- (Bool) validateTexSize: (int)newTexWidth height:(int)newTexHeight
{
    Bool changed;

    if( newTexHeight != texHeight || newTexWidth != texWidth ) {
        #ifdef VERBOSE_ON
        const int oldTexWidth = texWidth;
        const int oldTexHeight = texHeight;
        #endif
        texWidth = newTexWidth;
        texHeight = newTexHeight;
        changed = YES;

        GLfloat texCoordWidth, texCoordHeight;
        if(NULL != pbuffer) {
            GLenum textureTarget = [pbuffer textureTarget] ;
            GLsizei pwidth = [pbuffer pixelsWide];
            GLsizei pheight = [pbuffer pixelsHigh];
            if( GL_TEXTURE_2D == textureTarget ) {
                texCoordWidth  = (GLfloat)pwidth /(GLfloat)texWidth  ;
                texCoordHeight = (GLfloat)pheight/(GLfloat)texHeight ;
            } else {
                texCoordWidth  = pwidth;
                texCoordHeight = pheight;
            }
        } else {
            texCoordWidth  = (GLfloat)1.0f;
            texCoordHeight = (GLfloat)1.0f;
        }
        gl_texCoords[3] = texCoordHeight;
        gl_texCoords[5] = texCoordHeight;
        gl_texCoords[4] = texCoordWidth;
        gl_texCoords[6] = texCoordWidth;
        #ifdef VERBOSE_ON
NS_DURING
        // Available >= 10.7
        DBG_PRINT("MyNSOpenGLLayer::validateTexSize %p: tex %dx%d -> %dx%d, dedicatedFrame set:%d %lf/%lf %lfx%lf scale %lf\n", 
            self, oldTexWidth, oldTexHeight, newTexWidth, newTexHeight, 
            dedicatedFrameSet, dedicatedFrame.origin.x, dedicatedFrame.origin.y, dedicatedFrame.size.width, dedicatedFrame.size.height, 
            [self contentsScale]);
NS_HANDLER
NS_ENDHANDLER
        #endif
    } else {
        changed = NO;
    }
    return changed;
}

- (void) setTextureID: (int) _texID
{
    textureID = _texID;
}

- (Bool) isSamePBuffer: (NSOpenGLPixelBuffer*) p
{
    return pbuffer == p || newPBuffer == p;
}

- (void)setNewPBuffer: (NSOpenGLPixelBuffer*)p
{
    SYNC_PRINT("<NP-S %p -> %p>", pbuffer, p);
    newPBuffer = p;
    [newPBuffer retain];
}

- (void) applyNewPBuffer
{
    if( NULL != newPBuffer ) { // volatile OK
        SYNC_PRINT("<NP-A %p -> %p>", pbuffer, newPBuffer);

        if( 0 != textureID ) {
            glDeleteTextures(1, (GLuint *)&textureID);
            [self setTextureID: 0];
        }
        [pbuffer release];

        pbuffer = newPBuffer;
        newPBuffer = NULL;
    }
}

- (void)deallocPBuffer
{
    if(NULL != pbuffer) {
        NSOpenGLContext* context = [self openGLContext];
        if(NULL!=context) {
            [context makeCurrentContext];

            DBG_PRINT("MyNSOpenGLLayer::deallocPBuffer (with ctx) %p (refcnt %d) - context %p, pbuffer %p, texID %d\n", self, (int)[self retainCount], context, pbuffer, (int)textureID);

            if( 0 != textureID ) {
                glDeleteTextures(1, (GLuint *)&textureID);
                [self setTextureID: 0];
            }
            if(NULL != pbuffer) {
                [pbuffer release];
                pbuffer = NULL;
            }
            if(NULL != newPBuffer) {
                [newPBuffer release];
                newPBuffer = NULL;
            }

            [context clearDrawable];
        } else {
            DBG_PRINT("MyNSOpenGLLayer::deallocPBuffer (w/o ctx) %p (refcnt %d) - context %p, pbuffer %p, texID %d\n", self, (int)[self retainCount], context, pbuffer, (int)textureID);
        }
    }
}

- (void)disableAnimation
{
    DBG_PRINT("MyNSOpenGLLayer::disableAnimation.0: %p (refcnt %d) - displayLink %p\n", self, (int)[self retainCount], displayLink);
    [self setAsynchronous: NO];
    if(NULL != displayLink) {
#ifdef HAS_CADisplayLink
        [displayLink setPaused: YES];
        [displayLink release];
#else
        CVDisplayLinkStop(displayLink);
        CVDisplayLinkRelease(displayLink);
#endif
        displayLink = NULL;
    }
    DBG_PRINT("MyNSOpenGLLayer::disableAnimation.X: %p (refcnt %d) - displayLink %p\n", self, (int)[self retainCount], displayLink);
}

- (void)releaseLayer
{
    DBG_PRINT("MyNSOpenGLLayer::releaseLayer.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    [self setGLEnabled: NO];
    [self disableAnimation];
    pthread_mutex_lock(&renderLock);
    [self deallocPBuffer];
    if( NULL != glContext ) {
        [glContext release];
        glContext = NULL;
    }
    if( NULL != parentPixelFmt ) {
        [parentPixelFmt release];
        parentPixelFmt = NULL;
    }
    pthread_mutex_unlock(&renderLock);
    [self release];
    DBG_PRINT("MyNSOpenGLLayer::releaseLayer.X: %p\n", self);
}

#ifdef DBG_LIFECYCLE

- (id)retain
{
    DBG_PRINT("MyNSOpenGLLayer::retain.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyNSOpenGLLayer::retain: %@",[NSThread callStackSymbols]);
    id o = [super retain];
    DBG_PRINT("MyNSOpenGLLayer::retain.X: %p (refcnt %d)\n", o, (int)[o retainCount]);
    return o;
}

- (oneway void)release
{
    DBG_PRINT("MyNSOpenGLLayer::release.0: %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyNSOpenGLLayer::release: %@",[NSThread callStackSymbols]);
    [super release];
    // DBG_PRINT("MyNSOpenGLLayer::release.X: %p (refcnt %d)\n", self, (int)[self retainCount]);
}

#endif

- (void)dealloc
{
    DBG_PRINT("MyNSOpenGLLayer::dealloc.0 %p (refcnt %d)\n", self, (int)[self retainCount]);
    // NSLog(@"MyNSOpenGLLayer::dealloc: %@",[NSThread callStackSymbols]);
    [self disableAnimation];
    pthread_mutex_lock(&renderLock);
    [self deallocPBuffer];
    pthread_mutex_unlock(&renderLock);
    pthread_cond_destroy(&renderSignal);
    pthread_mutex_destroy(&renderLock);
    [super dealloc];
    // DBG_PRINT("MyNSOpenGLLayer::dealloc.X %p\n", self);
}

- (Bool)isGLSourceValid
{
    return NULL != pbuffer || NULL != newPBuffer || 0 != textureID ;
}

// @NWDedicatedFrame
- (void)setDedicatedFrame:(CGRect)dFrame quirks:(int)quirks {
    CGRect lRect = [self frame];
    Bool dedicatedFramePosSet  = 0 != ( NW_DEDICATEDFRAME_QUIRK_POSITION & quirks );
    Bool dedicatedFrameSizeSet = 0 != ( NW_DEDICATEDFRAME_QUIRK_SIZE & quirks );
    Bool dedicatedLayoutSet = 0 != ( NW_DEDICATEDFRAME_QUIRK_LAYOUT & quirks );
    dedicatedFrameSet  = dedicatedFramePosSet || dedicatedFrameSizeSet || dedicatedLayoutSet;
    dedicatedFrame = dFrame;

    DBG_PRINT("MyNSOpenGLLayer::setDedicatedFrame: Quirks [%d, pos %d, size %d, lout %d], %p, texSize %dx%d, %lf/%lf %lfx%lf -> %lf/%lf %lfx%lf\n",
        quirks, dedicatedFramePosSet, dedicatedFrameSizeSet, dedicatedLayoutSet, self, texWidth, texHeight,
        lRect.origin.x, lRect.origin.y, lRect.size.width, lRect.size.height,
        dFrame.origin.x, dFrame.origin.y, dFrame.size.width, dFrame.size.height);
    (void)lRect; // silence
    
    if( dedicatedFrameSet ) {
        [super setFrame: dedicatedFrame];
    }
}

- (void) setFrame:(CGRect) frame {
    if( dedicatedFrameSet ) {
        [super setFrame: dedicatedFrame];
    } else {
        [super setFrame: frame];
    }
}

- (id<CAAction>)actionForKey:(NSString *)key
{
    DBG_PRINT("MyNSOpenGLLayer::actionForKey.0 %p key %s -> NIL\n", self, [key UTF8String]);
    return nil;
    // return [super actionForKey: key];
}

- (NSOpenGLPixelFormat *)openGLPixelFormatForDisplayMask:(uint32_t)mask
{
    DBG_PRINT("MyNSOpenGLLayer::openGLPixelFormatForDisplayMask: %p (refcnt %d) - parent-pfmt %p -> new-pfmt %p\n", 
        self, (int)[self retainCount], parentPixelFmt, parentPixelFmt);
    // We simply take over ownership of parent PixelFormat until releaseLayer..
    return parentPixelFmt;
}

- (NSOpenGLContext *)openGLContextForPixelFormat:(NSOpenGLPixelFormat *)pixelFormat
{
    DBG_PRINT("MyNSOpenGLLayer::openGLContextForPixelFormat.0: %p (refcnt %d) - pfmt %p, ctx %p, DisplayLink %p\n",
        self, (int)[self retainCount], pixelFormat, glContext, displayLink);
    return glContext;
}

- (BOOL)canDrawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat 
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    SYNC_PRINT("<? %d, %d>", (int)shallDraw, (int)isGLEnabled);
    return shallDraw && isGLEnabled;
}

- (void)drawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat 
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    pthread_mutex_unlock(&renderLock);
    SYNC_PRINT("<* ");
    // NSLog(@"MyNSOpenGLLayer::DRAW: %@",[NSThread callStackSymbols]);

    if( isGLEnabled && shallDraw && ( NULL != pbuffer || NULL != newPBuffer || 0 != textureID ) ) {
        [context makeCurrentContext];

        if( NULL != newPBuffer ) { // volatile OK
            [self applyNewPBuffer];
        }

        GLenum textureTarget;

        CGRect texDim = dedicatedFrameSet ? dedicatedFrame : [self bounds];
        CGFloat _contentsScale = 1;
NS_DURING
        // Available >= 10.7
        _contentsScale = [self contentsScale];
NS_HANDLER
NS_ENDHANDLER
        Bool texSizeChanged = [self validateTexSize: (int)(texDim.size.width  * _contentsScale  + 0.5f) 
                                              height:(int)(texDim.size.height * _contentsScale  + 0.5f)];
        if( texSizeChanged ) {
            [context update];
        }

        if( NULL != pbuffer ) {
            if( texSizeChanged && 0 != textureID ) {
                glDeleteTextures(1, (GLuint *)&textureID);
                [self setTextureID: 0];
            }
            textureTarget = [pbuffer textureTarget];
            if( 0 != gl3ShaderProgramName ) {
                glUseProgram(gl3ShaderProgramName);
                glActiveTexture(GL_TEXTURE0);
            }
            if( 0 == textureID ) {
                glGenTextures(1, (GLuint *)&textureID);
                glBindTexture(textureTarget, textureID);
                glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
                glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
                glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
                glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            } else {
                glBindTexture(textureTarget, textureID);
            }
            [context setTextureImageToPixelBuffer: (NSOpenGLPixelBuffer*) pbuffer colorBuffer: GL_FRONT];
        } else {
            if( 0 != gl3ShaderProgramName ) {
                glUseProgram(gl3ShaderProgramName);
                glActiveTexture(GL_TEXTURE0);
            }
            textureTarget = GL_TEXTURE_2D;
            glBindTexture(textureTarget, textureID);
        }
        SYNC_PRINT(" %d gl3Prog %d/%d*>", (int)textureID, (int)gl3ShaderProgramName, (int)glIsProgram (gl3ShaderProgramName));

        if( 0 == vboBufVert ) { // Once: Init Data and Bind to Pointer
            if( 0 != gl3ShaderProgramName ) {
                // Install default VAO as required by GL 3.2 core!
                GLuint vaoBuf = 0;
                glGenVertexArrays(1, &vaoBuf);
                glBindVertexArray(vaoBuf);

                // Set texture-unit 0
                GLint texUnitLoc = glGetUniformLocation (gl3ShaderProgramName, "mgl_Texture0");
                glUniform1i (texUnitLoc, 0);
            }
            glGenBuffers( 1, &vboBufVert );
            glBindBuffer( GL_ARRAY_BUFFER, vboBufVert );
            glBufferData( GL_ARRAY_BUFFER, 4 * 2 * sizeof(GLfloat), gl_verts, GL_STATIC_DRAW);
            if( 0 != gl3ShaderProgramName ) {
                vertAttrLoc = glGetAttribLocation( gl3ShaderProgramName, "mgl_Vertex" );
                glVertexAttribPointer( vertAttrLoc, 2, GL_FLOAT, GL_FALSE, 0, NULL );
            } else {
                glVertexPointer(2, GL_FLOAT, 0, NULL);
            }

            glGenBuffers( 1, &vboBufTexCoord );
            glBindBuffer( GL_ARRAY_BUFFER, vboBufTexCoord );
            glBufferData( GL_ARRAY_BUFFER, 4 * 2 * sizeof(GLfloat), gl_texCoords, GL_STATIC_DRAW);
            if( 0 != gl3ShaderProgramName ) {
                texCoordAttrLoc = glGetAttribLocation( gl3ShaderProgramName, "mgl_MultiTexCoord" );
                glVertexAttribPointer( texCoordAttrLoc, 2, GL_FLOAT, GL_FALSE, 0, NULL );
            } else {
                glTexCoordPointer(2, GL_FLOAT, 0, NULL);
            }
        }
        if( texSizeChanged ) {
            glBindBuffer( GL_ARRAY_BUFFER, vboBufTexCoord );
            glBufferSubData( GL_ARRAY_BUFFER, 0, 4 * 2 * sizeof(GLfloat), gl_texCoords);
            if( 0 != gl3ShaderProgramName ) {
                glVertexAttribPointer( texCoordAttrLoc, 2, GL_FLOAT, GL_FALSE, 0, NULL );
            } else {
                glTexCoordPointer(2, GL_FLOAT, 0, NULL);
            }
        }
        if( 0 != gl3ShaderProgramName ) {
            glEnableVertexAttribArray( vertAttrLoc );
            glEnableVertexAttribArray( texCoordAttrLoc );
        } else {
            glEnable(textureTarget);
           
            glEnableClientState(GL_VERTEX_ARRAY);
            glEnableClientState(GL_TEXTURE_COORD_ARRAY);
        }

        glDrawArrays(GL_TRIANGLE_FAN, 0, 4);
           
        if( 0 != gl3ShaderProgramName ) {
            glDisableVertexAttribArray( vertAttrLoc );
            glDisableVertexAttribArray( texCoordAttrLoc );
            glUseProgram(0);
        } else {
            glDisableClientState(GL_VERTEX_ARRAY);
            glDisableClientState(GL_TEXTURE_COORD_ARRAY);
           
            glDisable(textureTarget);
        }

        glBindTexture(textureTarget, 0);

        [context clearDrawable];

        [super drawInOpenGLContext: context pixelFormat: pixelFormat forLayerTime: timeInterval displayTime: timeStamp];

    } else {
        // glClear(GL_COLOR_BUFFER_BIT);
        // glBlitFramebuffer(0, 0, texWidth, texHeight, 
        //                   0, 0, texWidth, texHeight,
        //                   GL_COLOR_BUFFER_BIT, GL_NEAREST);
        SYNC_PRINT(" 0*>");
    }

    #ifdef DBG_PERF
        [self tick];
    #endif
    shallDraw = NO;

    if( 0 >= swapInterval ) {
        pthread_cond_signal(&renderSignal); // wake up !vsync
        SYNC_PRINT("<s>");
    }
    SYNC_PRINT("<$>\n");
    pthread_mutex_unlock(&renderLock);
}

- (void)pauseAnimation:(Bool)pause
{
    DBG_PRINT("MyNSOpenGLLayer::pauseAnimation: %d\n", (int)pause);
    [self setAsynchronous: NO];
    if(pause) {
        if(NULL != displayLink) {
            #ifdef HAS_CADisplayLink
                [displayLink setPaused: YES];
            #else
                CVDisplayLinkStop(displayLink);
            #endif
        }
    } else {
        if(NULL != displayLink) {
            #ifdef HAS_CADisplayLink
                [displayLink setPaused: NO];
                [displayLink setFrameInterval: swapInterval];
            #else
                CVDisplayLinkStart(displayLink);
            #endif
        }
    }
    tc = 0;
    timespec_now(&tStart);
}

- (void)setSwapInterval:(int)interval
{
    /**
     * v-sync doesn't works w/ NSOpenGLLayer's context .. well :(
     * Using CVDisplayLink .. see setSwapInterval() below.
     *
        GLint si;
        [context getValues: &si forParameter: NSOpenGLCPSwapInterval];
        if(si != swapInterval) {
            DBG_PRINT("MyNSOpenGLLayer::drawInOpenGLContext %p setSwapInterval: %d -> %d\n", self, si, swapInterval);
            [context setValues: &swapInterval forParameter: NSOpenGLCPSwapInterval];
        }
    */

    pthread_mutex_lock(&renderLock);
    DBG_PRINT("MyNSOpenGLLayer::setSwapInterval.0: %d - displayLink %p\n", interval, displayLink);
    swapInterval = interval;
    swapIntervalCounter = 0;
    pthread_mutex_unlock(&renderLock);

    if(0 < swapInterval) {
        [self pauseAnimation: NO];
    } else {
        [self pauseAnimation: YES];
    }
    DBG_PRINT("MyNSOpenGLLayer::setSwapInterval.X: %d\n", interval);
}
 
-(void)tick
{
    tc++;
    if(tc%60==0) {
        struct timespec t1, td;
        timespec_now(&t1);
        timespec_subtract(&td, &t1, &tStart);
        long td_ms = timespec_milliseconds(&td);
        fprintf(stderr, "NSOpenGLLayer: %ld ms / %d frames, %ld ms / frame, %f fps\n",
            td_ms, tc, td_ms/tc, (tc * 1000.0) / (float)td_ms );
        fflush(NULL);
    }
}

- (void)waitUntilRenderSignal: (long) to_micros
{
    struct timespec t0, to_until;
    BOOL tooLate;
    int wr;
    if( 0 >= to_micros ) {
        to_micros = 16666 + 1000; // defaults to 1/60s + 1ms
        NSLog(@"MyNSOpenGLContext::waitUntilRenderSignal: to_micros was zero, using defaults");
    }
    pthread_mutex_lock(&renderLock);
    timespec_now(&t0);
    to_until = lastWaitTime;
    timespec_addmicros(&to_until, to_micros);
    tooLate = timespec_compare(&to_until, &t0) < 0;
    #ifdef DBG_SYNC
        struct timespec td_until;
        timespec_subtract(&td_until, &to_until, &t0);
        SYNC_PRINT("{W %ld ms, to %ld ms, late %d", to_micros/1000, timespec_milliseconds(&td_until), tooLate);
    #endif
    if( 0 < swapInterval ) {
        if( tooLate ) {
            // adjust!
            to_until = t0;
            timespec_addmicros(&to_until, to_micros);
        }
        wr = pthread_cond_timedwait(&renderSignal, &renderLock, &to_until);
        #ifdef DBG_SYNC
            struct timespec t1, td, td2;
            timespec_now(&t1);
            timespec_subtract(&td, &t1, &t0);
            timespec_subtract(&td2, &t1, &lastWaitTime);
            fprintf(stderr, "(%ld) / (%ld) ms", timespec_milliseconds(&td), timespec_milliseconds(&td2));
        #endif
    }
    SYNC_PRINT("-%d-%d}\n", shallDraw, wr);
    timespec_now(&lastWaitTime);
    pthread_mutex_unlock(&renderLock);
}

@end

NSOpenGLLayer* createNSOpenGLLayer(NSOpenGLContext* ctx, int gl3ShaderProgramName, NSOpenGLPixelFormat* fmt, NSOpenGLPixelBuffer* p, uint32_t texID, Bool opaque, int texWidth, int texHeight, int winWidth, int winHeight) {
  return [[[MyNSOpenGLLayer alloc] init] setupWithContext:ctx gl3ShaderProgramName: (GLuint)gl3ShaderProgramName pixelFormat: fmt pbuffer: p texIDArg: (GLuint)texID
                                                              opaque: opaque texWidth: texWidth texHeight: texHeight
                                                              winWidth: winWidth winHeight: winHeight];
}
 
void setNSOpenGLLayerEnabled(NSOpenGLLayer* layer, Bool enable) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
    [l setGLEnabled: enable];
    [pool release];
}

void setNSOpenGLLayerSwapInterval(NSOpenGLLayer* layer, int interval) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
    [l setSwapInterval: interval];
    [pool release];
}

void waitUntilNSOpenGLLayerIsReady(NSOpenGLLayer* layer, long to_micros) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
    [l waitUntilRenderSignal: to_micros];
    [pool release];
}

void setNSOpenGLLayerNeedsDisplayFBO(NSOpenGLLayer* layer, uint32_t texID) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
    Bool shallDraw;

    // volatile OK
    [l setTextureID: texID];
    shallDraw = [l isGLSourceValid];
    l->shallDraw = shallDraw;

    SYNC_PRINT("<! T %d>", (int)shallDraw);
    if(shallDraw) {
        if ( [NSThread isMainThread] == YES ) {
          [l setNeedsDisplay];
        } else {
          // don't wait - using doublebuffering
          [l performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:nil waitUntilDone:NO];
        }
    }
    // DBG_PRINT("MyNSOpenGLLayer::setNSOpenGLLayerNeedsDisplay %p\n", l);
    [pool release];
}

void setNSOpenGLLayerNeedsDisplayPBuffer(NSOpenGLLayer* layer, NSOpenGLPixelBuffer* p) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
    Bool shallDraw;

    if( NO == [l isSamePBuffer: p] ) {
        [l setNewPBuffer: p];
    }

    // volatile OK
    shallDraw = [l isGLSourceValid];
    l->shallDraw = shallDraw;

    SYNC_PRINT("<! T %d>", (int)shallDraw);
    if(shallDraw) {
        if ( [NSThread isMainThread] == YES ) {
          [l setNeedsDisplay];
        } else {
          // don't wait - using doublebuffering
          [l performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:nil waitUntilDone:NO];
        }
    }
    // DBG_PRINT("MyNSOpenGLLayer::setNSOpenGLLayerNeedsDisplay %p\n", l);
    [pool release];
}

void releaseNSOpenGLLayer(NSOpenGLLayer* layer) {
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
    MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;

    [CATransaction begin];
    [CATransaction setValue:(id)kCFBooleanTrue forKey:kCATransactionDisableActions];

    DBG_PRINT("MyNSOpenGLLayer::releaseNSOpenGLLayer.0: %p\n", l);
    [l releaseLayer];
    DBG_PRINT("MyNSOpenGLLayer::releaseNSOpenGLLayer.X: %p\n", l);

    [CATransaction commit];

    [pool release];
}

