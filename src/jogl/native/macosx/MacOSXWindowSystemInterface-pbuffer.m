#import "MacOSXWindowSystemInterface.h"

@interface MyNSOpenGLLayer: NSOpenGLLayer
{
@protected
    NSOpenGLPixelBuffer* pbuffer;
    int width;
    int height;
    GLuint textureID;
@public
    volatile BOOL        shallDraw;
}

- (id) initWithContext: (NSOpenGLContext*) ctx
       pixelFormat: (NSOpenGLPixelFormat*) pfmt
       pbuffer: (NSOpenGLPixelBuffer*) p
       opaque: (Bool) opaque
       width: (int) width 
       height: (int) height;

@end

@implementation MyNSOpenGLLayer

- (id) initWithContext: (NSOpenGLContext*) _ctx
       pixelFormat: (NSOpenGLPixelFormat*) _fmt
       pbuffer: (NSOpenGLPixelBuffer*) p
       opaque: (Bool) opaque
       width: (int) _width 
       height: (int) _height;
{
    self = [super init];
    pbuffer = p;
    [pbuffer retain];

    [self setAsynchronous: NO];
    // [self setAsynchronous: YES]; // FIXME: JAU
    [self setNeedsDisplayOnBoundsChange: NO];
    [self setOpaque: opaque ? YES : NO];
    width = _width;
    height = _height;
    textureID = 0;
    shallDraw = NO;
    DBG_PRINT("MyNSOpenGLLayer::init %p, ctx %p, pfmt %p, pbuffer %p, opaque %d, %dx%d -> %dx%d\n", 
        self, _ctx, _fmt, pbuffer, opaque, width, height, [pbuffer pixelsWide], [pbuffer pixelsHigh]);
    return self;
}

- (void)dealloc
{
    [pbuffer release];
    DBG_PRINT("MyNSOpenGLLayer::dealloc %p\n", self);
    [super dealloc];
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

    [context makeCurrentContext];
   
    GLenum textureTarget = [pbuffer textureTarget];
    GLsizei pwidth = [pbuffer pixelsWide];
    GLsizei pheight = [pbuffer pixelsHigh];
    GLfloat tWidth = textureTarget == GL_TEXTURE_2D ? (GLfloat)width/(GLfloat)pwidth : pwidth;
    GLfloat tHeight = textureTarget == GL_TEXTURE_2D ? (GLfloat)height/(GLfloat)pheight : pheight;
   
    DBG_PRINT("MyNSOpenGLLayer::drawInOpenGLContext %p, ctx %p, pfmt %p %dx%d -> %fx%f 0x%X\n", 
        self, context, pixelFormat, width, height, tWidth, tHeight, textureTarget);

    if(0 == textureID) {
        glGenTextures(1, &textureID);
    }
    glBindTexture(textureTarget, textureID);

    [context setTextureImageToPixelBuffer: pbuffer colorBuffer: GL_FRONT];

    glTexParameteri(textureTarget, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
    glTexParameteri(textureTarget, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    glTexParameteri(textureTarget, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
    glTexParameteri(textureTarget, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
   
    glEnable(textureTarget);
   
    static GLfloat verts[] = {
    -1.0, -1.0,
    -1.0,  1.0,
     1.0,  1.0,
     1.0, -1.0
    };
   
    GLfloat tex[] = {
     0.0,    0.0,
     0.0,    tHeight,
     tWidth, tHeight,
     tWidth, 0.0
    };
   
    glEnableClientState(GL_VERTEX_ARRAY);
    glEnableClientState(GL_TEXTURE_COORD_ARRAY);
    glVertexPointer(2, GL_FLOAT, 0, verts);
    glTexCoordPointer(2, GL_FLOAT, 0, tex);
   
    glDrawArrays(GL_QUADS, 0, 4);
   
    glDisableClientState(GL_VERTEX_ARRAY);
    glDisableClientState(GL_TEXTURE_COORD_ARRAY);
   
    glDisable(textureTarget);
   
    [super drawInOpenGLContext: context pixelFormat: pixelFormat forLayerTime: timeInterval displayTime: timeStamp];
}

@end

NSOpenGLLayer* createNSOpenGLLayer(NSOpenGLContext* ctx, NSOpenGLPixelFormat* fmt, NSOpenGLPixelBuffer* p, Bool opaque, int width, int height) {
  return [[MyNSOpenGLLayer alloc] initWithContext:ctx pixelFormat: fmt pbuffer: p opaque: opaque width: width height: height];
}

void setNSOpenGLLayerNeedsDisplay(NSOpenGLLayer* layer) {
  MyNSOpenGLLayer* l = (MyNSOpenGLLayer*) layer;
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  l->shallDraw = YES;
  if ( [NSThread isMainThread] == YES ) {
      [l setNeedsDisplay];
  } else {
      // [l performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:nil waitUntilDone:YES];
      [l performSelectorOnMainThread:@selector(setNeedsDisplay) withObject:nil waitUntilDone:NO];
  }
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

