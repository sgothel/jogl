
@interface MyNSOpenGLLayer: NSOpenGLLayer
{
@protected
    NSOpenGLContext*     ctxShared;
    NSView*              view;
    NSOpenGLPixelFormat* fmt;
@public
    volatile BOOL        shallDraw;
}

- (id) initWithContext: (NSOpenGLContext*) ctx
       pixelFormat: (NSOpenGLPixelFormat*) pfmt
       view: (NSView*) v
       opaque: (Bool) opaque;

@end

@implementation MyNSOpenGLLayer

- (id) initWithContext: (NSOpenGLContext*) _ctx
       pixelFormat: (NSOpenGLPixelFormat*) _fmt
       view: (NSView*) _view
       opaque: (Bool) opaque
{
    self = [super init];
    ctxShared = _ctx;
    [ctxShared retain];

    fmt = _fmt;
    [fmt retain];

    view = _view;
    [view retain];
    [self setView: view];
    [view setLayer: self];
    [view setWantsLayer: YES];

    [self setAsynchronous: NO];
    // [self setAsynchronous: YES]; // FIXME: JAU
    [self setNeedsDisplayOnBoundsChange: NO];
    [self setOpaque: opaque ? YES : NO];
    shallDraw = NO;
    textureID = 0;
    DBG_PRINT("MyNSOpenGLLayer::init %p, ctx %p, pfmt %p, view %p, opaque %d\n", self, ctx, fmt, view, opaque);
    return self;
}

- (void)dealloc
{
    [fmt release];
    [ctxShared release];
    [view release];
    DBG_PRINT("MyNSOpenGLLayer::dealloc %p\n", self);
    [super dealloc];
}

- (void) setOpenGLContext: (NSOpenGLContext*) _ctx
{
    DBG_PRINT("MyNSOpenGLLayer::setOpenGLContext: %p %p -> %p\n", self, [self openGLContext], _ctx);
    [super setOpenGLContext: _ctx];
}

- (void) setOpenGLPixelFormat: (NSOpenGLPixelFormat*) _fmt
{
    DBG_PRINT("MyNSOpenGLLayer::setOpenGLPixelFormat %p %p -> %p\n", self, fmt, _fmt);
    [super setOpenGLPixelFormat: fmt];
}

- (NSOpenGLPixelFormat *) openGLPixelFormat
{
    return fmt;
}

- (void) setView: (NSView*) v
{
    DBG_PRINT("MyNSOpenGLLayer::setView %p %p -> %p (ignored/propagated)\n", self, view, v);
    [super setView: view]; // propagate
}

- (NSOpenGLPixelFormat *)openGLPixelFormatForDisplayMask:(uint32_t)mask
{
    DBG_PRINT("MyNSOpenGLLayer::openGLPixelFormatForDisplayMask %p %d -> %p\n", self, mask, fmt);
    return fmt;
}

- (NSOpenGLContext *)openGLContextForPixelFormat:(NSOpenGLPixelFormat *)pixelFormat
{
    NSOpenGLContext* ctx = NULL;
    if(NULL == ctx) {
        int viewNotReady[] = { 0 };
        ctx = createContext(ctxShared, view, true, fmt, [self isOpaque], viewNotReady);
    }
    DBG_PRINT("MyNSOpenGLLayer::openGLContextForPixelFormat %p, fmt %p/%p, view %p, shared %p -> %p\n", 
        self, fmt, pixelFormat, view, ctxShared, ctx);
    return ctx;
}

- (BOOL)canDrawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat 
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    DBG_PRINT("MyNSOpenGLLayer::canDrawInOpenGLContext %p: %d\n", self, self->shallDraw);
    return self->shallDraw;
}

- (void)drawInOpenGLContext:(NSOpenGLContext *)context pixelFormat:(NSOpenGLPixelFormat *)pixelFormat 
        forLayerTime:(CFTimeInterval)timeInterval displayTime:(const CVTimeStamp *)timeStamp
{
    self->shallDraw = NO;

    DBG_PRINT("MyNSOpenGLLayer::drawInOpenGLContext %p, ctx %p, pfmt %p\n", self, context, pixelFormat);

    [super drawInOpenGLContext: context pixelFormat: pixelFormat forLayerTime: timeInterval displayTime: timeStamp];
}

@end

NSOpenGLLayer* createNSOpenGLLayer(NSOpenGLContext* ctx, NSOpenGLPixelFormat* fmt, NSView* view, Bool opaque) {
  return [[MyNSOpenGLLayer alloc] initWithContext:ctx pixelFormat: fmt view: view opaque: opaque];
}

void setNSOpenGLLayerNeedsDisplay(NSOpenGLLayer* layer) {
  MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  l->shallDraw = YES;
  [l performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:nil waitUntilDone:YES];
  // NSView* view = [l view];
  // [view setNeedsDisplay: YES]; // FIXME: JAU
  // [view performSelectorOnMainThread:@selector(setNeedsDisplay:) withObject:YES waitUntilDone:YES];
  // [view performSelectorOnMainThread:@selector(display) withObject:nil waitUntilDone:YES];
  DBG_PRINT("MyNSOpenGLLayer::setNSOpenGLLayerNeedsDisplay %p\n", l);
  [pool release];
}

void releaseNSOpenGLLayer(NSOpenGLLayer* l) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [l release];
  [pool release];
}

