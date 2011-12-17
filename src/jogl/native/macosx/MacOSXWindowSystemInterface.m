/* Note: usage of AvailabilityMacros.h to detect whether we're
   building on OS X 10.3 does not work because the header #defines
   MAC_OS_X_VERSION_10_4 even though the machine is a 10.3 machine

#include <AvailabilityMacros.h>

#ifndef MAC_OS_X_VERSION_10_3
    #error building JOGL requires Mac OS X 10.3 or greater
#endif

#ifndef MAC_OS_X_VERSION_10_4
  #define NSOpenGLPFAColorFloat kCGLPFAColorFloat
  #define kCGLNoError 0
#endif
*/

#import "MacOSXWindowSystemInterface.h"

#import "ContextUpdater.h"

// see MacOSXPbufferGLContext.java createPbuffer
#define USE_GL_TEXTURE_RECTANGLE_EXT

#ifdef USE_GL_TEXTURE_RECTANGLE_EXT
    #ifndef GL_TEXTURE_RECTANGLE_EXT
            #define GL_TEXTURE_RECTANGLE_EXT 0x84F5
    #endif
#endif

// Workarounds for compiling on 10.3
#ifndef kCGLRGBA16161616Bit
#define kCGLRGBA16161616Bit 0x00800000  /* 64 argb bit/pixel,   R=63:48, G=47:32, B=31:16, A=15:0 */
#define kCGLRGBFloat64Bit   0x01000000  /* 64 rgb bit/pixel,    half float                        */
#define kCGLRGBAFloat64Bit  0x02000000  /* 64 argb bit/pixel,   half float                        */
#define kCGLRGBFloat128Bit  0x04000000  /* 128 rgb bit/pixel,   ieee float                        */
#define kCGLRGBAFloat128Bit 0x08000000  /* 128 argb bit/pixel,  ieee float                        */
#define kCGLRGBFloat256Bit  0x10000000  /* 256 rgb bit/pixel,   ieee double                       */
#define kCGLRGBAFloat256Bit 0x20000000  /* 256 argb bit/pixel,  ieee double                       */
#endif

struct _RendererInfo
{
    GLint id;                // kCGLRPRendererID
    GLint displayMask;        // kCGLRPDisplayMask
    
    GLint accelerated;        // kCGLRPAccelerated
    
    GLint window;            // kCGLRPWindow
    GLint fullscreen;        // kCGLRPFullScreen
    GLint multiscreen;        // kCGLRPMultiScreen
    GLint offscreen;            // kCGLRPOffScreen
    GLint floatPixels;        // see kCGLRPColorModes
    GLint stereo;            // kCGLRPBufferModes
    
    GLint auxBuffers;        // kCGLRPMaxAuxBuffers
    GLint sampleBuffers;        // kCGLRPMaxSampleBuffers
    GLint samples;            // kCGLRPMaxSamples
    GLint samplesModes;        // kCGLRPSampleModes
    GLint multiSample;        // see kCGLRPSampleModes
    GLint superSample;        // see kCGLRPSampleModes
    GLint alphaSample;        // kCGLRPSampleAlpha
    
    GLint colorModes;        // kCGLRPColorModes
    GLint colorRGBSizeMAX;
    GLint colorASizeMAX;
    GLint colorFloatRGBSizeMAX;
    GLint colorFloatASizeMAX;
    GLint colorFloatRGBSizeMIN;
    GLint colorFloatASizeMIN;
    GLint colorModesCount;
    GLint colorFloatModesCount;
    GLint depthModes;        // kCGLRPDepthModes
    GLint depthSizeMAX;
    GLint depthModesCount;
    GLint stencilModes;        // kCGLRPStencilModes
    GLint stencilSizeMAX;
    GLint stencilModesCount;
    GLint accumModes;        // kCGLRPAccumModes
    GLint accumRGBSizeMAX;
    GLint accumASizeMAX;
    GLint accumModesCount;
}
typedef RendererInfo;

RendererInfo *gRenderers = NULL;
GLint gRenderersCount = 0;

long depthModes[] = {
                    kCGL0Bit,
                    kCGL1Bit,
                    kCGL2Bit,
                    kCGL3Bit,
                    kCGL4Bit,
                    kCGL5Bit,
                    kCGL6Bit,
                    kCGL8Bit,
                    kCGL10Bit,
                    kCGL12Bit,
                    kCGL16Bit,
                    kCGL24Bit,
                    kCGL32Bit,
                    kCGL48Bit,
                    kCGL64Bit,
                    kCGL96Bit,
                    kCGL128Bit,
                    0
                    };
long depthModesBits[] = {0, 1, 2, 3, 4, 5, 6, 8, 10, 12, 16, 24, 32, 48, 64, 96, 128};
long colorModes[] = {
                    kCGLRGB444Bit,
                    kCGLARGB4444Bit,
                    kCGLRGB444A8Bit,
                    kCGLRGB555Bit,
                    kCGLARGB1555Bit,
                    kCGLRGB555A8Bit,
                    kCGLRGB565Bit,
                    kCGLRGB565A8Bit,
                    kCGLRGB888Bit,
                    kCGLARGB8888Bit,
                    kCGLRGB888A8Bit,
                    kCGLRGB101010Bit,
                    kCGLARGB2101010Bit,
                    kCGLRGB101010_A8Bit,
                    kCGLRGB121212Bit,
                    kCGLARGB12121212Bit,
                    kCGLRGB161616Bit,
                    kCGLRGBA16161616Bit,
                    kCGLRGBFloat64Bit,
                    kCGLRGBAFloat64Bit,
                    kCGLRGBFloat128Bit,
                    kCGLRGBAFloat128Bit,
                    kCGLRGBFloat256Bit,
                    kCGLRGBAFloat256Bit,
                    0
                    };
long colorModesBitsRGB[] =    {4, 4, 4, 5, 5, 5, 5, 5, 8, 8, 8, 10, 10, 10, 12, 12, 16, 16, 16, 16, 32, 32, 64, 64};
long colorModesBitsA[] =    {0, 4, 8, 0, 1, 8, 0, 8, 0, 8, 8,  0,  2,  8,  0, 12,  0, 16,  0, 16,  0, 32,  0, 64};

void getRendererInfo()
{
    if (gRenderersCount == 0)
    {        
        CGLRendererInfoObj info;
        CGLError err = CGLQueryRendererInfo(CGDisplayIDToOpenGLDisplayMask(kCGDirectMainDisplay), &info, &gRenderersCount);
        if (err == 0 /* kCGLNoError */)
        {
            // how many renderers are available?
            CGLDescribeRenderer(info, 0, kCGLRPRendererCount, &gRenderersCount);
            
            // allocate our global renderers info
            gRenderers = (RendererInfo*)malloc(gRenderersCount*sizeof(RendererInfo));
            memset(gRenderers, 0x00, gRenderersCount*sizeof(RendererInfo));
            
            // iterate through the renderers checking for their features
            long j;
            for (j=0; j<gRenderersCount; j++)
            {
                RendererInfo *renderer = &gRenderers[j];
                int i;
                
                CGLDescribeRenderer(info, j, kCGLRPRendererID, &(renderer->id));
                CGLDescribeRenderer(info, j, kCGLRPDisplayMask, &(renderer->displayMask));
                
                CGLDescribeRenderer(info, j, kCGLRPAccelerated, &(renderer->accelerated));
                
                CGLDescribeRenderer(info, j, kCGLRPWindow, &(renderer->window));
                CGLDescribeRenderer(info, j, kCGLRPFullScreen, &(renderer->fullscreen));
                CGLDescribeRenderer(info, j, kCGLRPMultiScreen, &(renderer->multiscreen));
                CGLDescribeRenderer(info, j, kCGLRPOffScreen, &(renderer->offscreen));
                CGLDescribeRenderer(info, j, kCGLRPColorModes, &(renderer->floatPixels));
                if ((renderer->floatPixels >= kCGLRGBFloat64Bit) != 0)
                {
                    renderer->floatPixels = 1;
                }
                else
                {
                    renderer->floatPixels = 0;
                }
                CGLDescribeRenderer(info, j, kCGLRPBufferModes, &(renderer->stereo));
                if ((renderer->stereo & kCGLStereoscopicBit) != 0)
                {
                    renderer->stereo = 1;
                }
                else
                {
                    renderer->stereo = 0;
                }
                
                CGLDescribeRenderer(info, j, kCGLRPMaxAuxBuffers, &(renderer->auxBuffers));
                CGLDescribeRenderer(info, j, kCGLRPMaxSampleBuffers, &(renderer->sampleBuffers));
                CGLDescribeRenderer(info, j, kCGLRPMaxSamples, &(renderer->samples));
                // The following queries are only legal on 10.4
                // FIXME: should figure out a way to enable them dynamically
#ifdef kCGLRPSampleModes
                CGLDescribeRenderer(info, j, kCGLRPSampleModes, &(renderer->samplesModes));
                if ((renderer->samplesModes & kCGLSupersampleBit) != 0)
                {
                    renderer->multiSample = 1;
                }
                if ((renderer->samplesModes & kCGLMultisampleBit) != 0)
                {
                    renderer->superSample = 1;
                }
                CGLDescribeRenderer(info, j, kCGLRPSampleAlpha, &(renderer->alphaSample));
#endif
                CGLDescribeRenderer(info, j, kCGLRPColorModes, &(renderer->colorModes));
                i=0;
                int floatPixelFormatInitialized = 0;
                while (colorModes[i] != 0)
                {
                    if ((renderer->colorModes & colorModes[i]) != 0)
                    {
                        // non-float color model
                        if (colorModes[i] < kCGLRGBFloat64Bit)
                        {
                            // look for max color and alpha values - prefer color models that have alpha
                            if ((colorModesBitsRGB[i] >= renderer->colorRGBSizeMAX) && (colorModesBitsA[i] >= renderer->colorASizeMAX))
                            {
                                renderer->colorRGBSizeMAX = colorModesBitsRGB[i];
                                renderer->colorASizeMAX = colorModesBitsA[i];
                            }
                            renderer->colorModesCount++;
                        }
                        // float-color model
                        if (colorModes[i] >= kCGLRGBFloat64Bit)
                        {
                            if (floatPixelFormatInitialized == 0)
                            {
                                floatPixelFormatInitialized = 1;
                                
                                renderer->colorFloatASizeMAX = colorModesBitsA[i];
                                renderer->colorFloatRGBSizeMAX = colorModesBitsRGB[i];
                                renderer->colorFloatASizeMIN = colorModesBitsA[i];
                                renderer->colorFloatRGBSizeMIN = colorModesBitsRGB[i];
                            }
                            // look for max color and alpha values - prefer color models that have alpha
                            if ((colorModesBitsRGB[i] >= renderer->colorFloatRGBSizeMAX) && (colorModesBitsA[i] >= renderer->colorFloatASizeMAX))
                            {
                                renderer->colorFloatRGBSizeMAX = colorModesBitsRGB[i];
                                renderer->colorFloatASizeMAX = colorModesBitsA[i];
                            }
                            // find min color
                            if (colorModesBitsA[i] < renderer->colorFloatASizeMIN)
                            {
                                renderer->colorFloatASizeMIN = colorModesBitsA[i];
                            }
                            // find min alpha color
                            if (colorModesBitsA[i] < renderer->colorFloatRGBSizeMIN)
                            {
                                renderer->colorFloatRGBSizeMIN = colorModesBitsRGB[i];
                            }
                            renderer->colorFloatModesCount++;
                        }
                    }
                    i++;
                }
                CGLDescribeRenderer(info, j, kCGLRPDepthModes, &(renderer->depthModes));
                i=0;
                while (depthModes[i] != 0)
                {
                    if ((renderer->depthModes & depthModes[i]) != 0)
                    {
                        renderer->depthSizeMAX = depthModesBits[i];
                        renderer->depthModesCount++;
                    }
                    i++;
                }
                CGLDescribeRenderer(info, j, kCGLRPStencilModes, &(renderer->stencilModes));
                i=0;
                while (depthModes[i] != 0)
                {
                    if ((renderer->stencilModes & depthModes[i]) != 0)
                    {
                        renderer->stencilSizeMAX = depthModesBits[i];
                        renderer->stencilModesCount++;
                    }
                    i++;
                }
                CGLDescribeRenderer(info, j, kCGLRPAccumModes, &(renderer->accumModes));
                i=0;
                while (colorModes[i] != 0)
                {
                    if ((renderer->accumModes & colorModes[i]) != 0)
                    {
                        if ((colorModesBitsRGB[i] >= renderer->accumRGBSizeMAX) && (colorModesBitsA[i] >= renderer->accumASizeMAX))
                        {
                            renderer->accumRGBSizeMAX = colorModesBitsRGB[i];
                            renderer->accumASizeMAX = colorModesBitsA[i];
                        }
                        renderer->accumModesCount++;
                    }
                    i++;
                }
            }
        }
        CGLDestroyRendererInfo (info);
    }
    
#if 0
    fprintf(stderr, "gRenderersCount=%ld\n", gRenderersCount);
    int j;
    for (j=0; j<gRenderersCount; j++)
    {
        RendererInfo *renderer = &gRenderers[j];
        fprintf(stderr, "    id=%ld\n", renderer->id);
        fprintf(stderr, "    displayMask=%ld\n", renderer->displayMask);
        
        fprintf(stderr, "        accelerated=%ld\n", renderer->accelerated);
        
        fprintf(stderr, "        window=%ld\n", renderer->window);
        fprintf(stderr, "        fullscreen=%ld\n", renderer->fullscreen);
        fprintf(stderr, "        multiscreen=%ld\n", renderer->multiscreen);
        fprintf(stderr, "        offscreen=%ld\n", renderer->offscreen);
        fprintf(stderr, "        floatPixels=%ld\n", renderer->floatPixels);
        fprintf(stderr, "        stereo=%ld\n", renderer->stereo);
        
        fprintf(stderr, "        auxBuffers=%ld\n", renderer->auxBuffers);
        fprintf(stderr, "        sampleBuffers=%ld\n", renderer->sampleBuffers);
        fprintf(stderr, "        samples=%ld\n", renderer->samples);
        fprintf(stderr, "        samplesModes=%ld\n", renderer->samplesModes);
        fprintf(stderr, "        multiSample=%ld\n", renderer->superSample);
        fprintf(stderr, "        superSample=%ld\n", renderer->superSample);
        fprintf(stderr, "        alphaSample=%ld\n", renderer->alphaSample);
        
        fprintf(stderr, "        colorModes=%ld\n", renderer->colorModes);
        fprintf(stderr, "            colorRGBSizeMAX=%ld\n", renderer->colorRGBSizeMAX);
        fprintf(stderr, "            colorASizeMAX=%ld\n", renderer->colorASizeMAX);
        fprintf(stderr, "            colorFloatRGBSizeMAX=%ld\n", renderer->colorFloatRGBSizeMAX);
        fprintf(stderr, "            colorFloatASizeMAX=%ld\n", renderer->colorFloatASizeMAX);
        fprintf(stderr, "            colorFloatRGBSizeMIN=%ld\n", renderer->colorFloatRGBSizeMIN);
        fprintf(stderr, "            colorFloatASizeMIN=%ld\n", renderer->colorFloatASizeMIN);
        fprintf(stderr, "            colorModesCount=%ld\n", renderer->colorModesCount);
        fprintf(stderr, "            colorFloatModesCount=%ld\n", renderer->colorFloatModesCount);
        fprintf(stderr, "        depthModes=%ld\n", renderer->depthModes);
        fprintf(stderr, "            depthSizeMAX=%ld\n", renderer->depthSizeMAX);
        fprintf(stderr, "            depthModesCount=%ld\n", renderer->depthModesCount);
        fprintf(stderr, "        stencilModes=%ld\n", renderer->stencilModes);
        fprintf(stderr, "            stencilSizeMAX=%ld\n", renderer->stencilSizeMAX);
        fprintf(stderr, "            stencilModesCount=%ld\n", renderer->stencilModesCount);
        fprintf(stderr, "        accumModes=%ld\n", renderer->accumModes);
        fprintf(stderr, "            accumRGBSizeMAX=%ld\n", renderer->accumRGBSizeMAX);
        fprintf(stderr, "            accumASizeMAX=%ld\n", renderer->accumASizeMAX);
        fprintf(stderr, "            accumModesCount=%ld\n", renderer->accumModesCount);
        fprintf(stderr, "\n");
    }
#endif
}

long validateParameter(NSOpenGLPixelFormatAttribute attribute, long value)
{
  int i;
  for (i=0; i<gRenderersCount; i++) {
    RendererInfo* renderer = &gRenderers[i];
    if (renderer->accelerated != 0) {
      switch (attribute) {
        case NSOpenGLPFAStereo:
          return renderer->stereo;

        case NSOpenGLPFAStencilSize:
          return MIN(value, renderer->stencilSizeMAX);

        default:
          break;
      }
    }
  }
    
  return value;
}

NSOpenGLPixelFormat* createPixelFormat(int* iattrs, int niattrs, int* ivalues) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

  getRendererInfo();

  // http://developer.apple.com/documentation/Cocoa/Reference/ApplicationKit/ObjC_classic/Classes/NSOpenGLPixelFormat.html
  NSOpenGLPixelFormatAttribute attribs[256];

  int idx = 0;
  int i;
  for (i = 0; i < niattrs && iattrs[i]>0; i++) {
    int attr = iattrs[i];
    switch (attr) {
      case NSOpenGLPFANoRecovery:
        if (ivalues[i] != 0) {
          attribs[idx++] = NSOpenGLPFANoRecovery;
        }
        break;

      case NSOpenGLPFAAccelerated:
        if (ivalues[i] != 0) {
          attribs[idx++] = NSOpenGLPFAAccelerated;
        }
        break;

      case NSOpenGLPFAPixelBuffer:
        if (ivalues[i] != 0) {
          attribs[idx++] = NSOpenGLPFAPixelBuffer;
        }
        break;

      case kCGLPFAColorFloat:
        if (ivalues[i] != 0) {
          attribs[idx++] = kCGLPFAColorFloat;
        }
        break;
        
      case NSOpenGLPFADoubleBuffer:
        if (ivalues[i] != 0) {
          attribs[idx++] = NSOpenGLPFADoubleBuffer;
        }
        break;

      case NSOpenGLPFAStereo:
        if (ivalues[i] != 0 && (validateParameter(NSOpenGLPFAStereo, 0 /* dummy */) != 0)) {
          attribs[idx++] = NSOpenGLPFAStereo;
        }
        break;

#ifdef CGL_VERSION_1_3
      case kCGLPFAOpenGLProfile:
#endif
      case NSOpenGLPFAColorSize:
      case NSOpenGLPFAAlphaSize:
      case NSOpenGLPFADepthSize:
      case NSOpenGLPFAAccumSize:
      case NSOpenGLPFASampleBuffers:
      case NSOpenGLPFASamples:
        attribs[idx++] = attr;
        attribs[idx++] = ivalues[i];
        break;

      case NSOpenGLPFAStencilSize:
        attribs[idx++] = attr;
        attribs[idx++] = validateParameter(NSOpenGLPFAStencilSize, ivalues[i]);
        break;

      default:
        // Need better way to signal to caller
        return nil;
    }
  }

  // Zero-terminate
  attribs[idx++] = 0;

  NSOpenGLPixelFormat* fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes:attribs];
  if (fmt == nil) {
    // should we fallback to defaults or not?
    fmt = [NSOpenGLView defaultPixelFormat];
  }

  [pool release];
  return fmt;
}

void queryPixelFormat(NSOpenGLPixelFormat* fmt, int* iattrs, int niattrs, int* ivalues) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  GLint tmp;
  // FIXME: think about how specifying this might affect the API
  GLint virtualScreen = 0;

  int i;
  for (i = 0; i < niattrs && iattrs[i]>0; i++) {
    [fmt getValues: &tmp
         forAttribute: (NSOpenGLPixelFormatAttribute) iattrs[i]
         forVirtualScreen: virtualScreen];
    ivalues[i] = (int) tmp;
  }
  [pool release];
}
  
void deletePixelFormat(NSOpenGLPixelFormat* fmt) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [fmt release];
  [pool release];
}

NSOpenGLContext* getCurrentContext() {
  NSOpenGLContext *ctx = NULL;
    
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  ctx = [NSOpenGLContext currentContext];
  [pool release];
  return ctx;
}

CGLContextObj getCGLContext(NSOpenGLContext* ctx) {
  void * cglContext = NULL;
    
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  cglContext = [ctx CGLContextObj];
  [pool release];
  return cglContext;
}

NSView* getNSView(NSOpenGLContext* ctx) {
  NSView* view = NULL;
    
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  view = [ctx view];
  [pool release];
  return view;
}

NSOpenGLContext* createContext(NSOpenGLContext* share,
                    NSView* view,
                    Bool isBackingLayerView,
                    NSOpenGLPixelFormat* fmt,
                    Bool opaque,
                    int* viewNotReady)
{
    getRendererInfo();
    
    NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

    if (view != NULL) {
        Bool viewReady = true;

        if(!isBackingLayerView) {
            if ([view lockFocusIfCanDraw] == NO) {
                DBG_PRINT("createContext [view lockFocusIfCanDraw] failed\n");
                viewReady = false;
            }
        }
        if(viewReady) {
            NSRect frame = [view frame];
            if ((frame.size.width == 0) || (frame.size.height == 0)) {
                if(!isBackingLayerView) {
                    [view unlockFocus];
                }
                DBG_PRINT("createContext view.frame size %dx%d\n", (int)frame.size.width, (int)frame.size.height);
                viewReady = false;
            }
        }

        if (!viewReady)
        {
            if (viewNotReady != NULL)
            {
                *viewNotReady = 1;
            }

            // the view is not ready yet
            [pool release];
            return NULL;
        }
    }
    
    NSOpenGLContext* ctx = [[NSOpenGLContext alloc] initWithFormat:fmt shareContext:share];
        
    if (ctx != nil) {
      if (view != nil) {
        if(!opaque) {
            GLint zeroOpacity = 0;
            [ctx setValues:&zeroOpacity forParameter:NSOpenGLCPSurfaceOpacity];
        }
        [ctx setView:view];
        if(!isBackingLayerView) {
            [view unlockFocus];        
        }
      }
    }

    [pool release];
    return ctx;
}

Bool makeCurrentContext(NSOpenGLContext* ctx) {
#if 0
  // we issue the CGL Lock from Java upfront!
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  CGLError cglError = CGLLockContext([ctx CGLContextObj]);
  if(0 == cglError) {
      [ctx makeCurrentContext];
  }
  [pool release];
  return 0 == cglError;
#else
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx makeCurrentContext];
  [pool release];
  return true;
#endif
}

Bool clearCurrentContext(NSOpenGLContext* ctx) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  NSOpenGLContext *currentNSContext = [NSOpenGLContext currentContext];
  if( currentNSContext != ctx ) {
      [ctx makeCurrentContext];
  }
  [NSOpenGLContext clearCurrentContext];
#if 0
  // we issue the CGL Lock from Java upfront!
  CGLUnlockContext([ctx CGLContextObj]);
#endif
  [pool release];
  return true;
}

Bool deleteContext(NSOpenGLContext* ctx, Bool releaseOnMainThread) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx clearDrawable];
  if(releaseOnMainThread && NO == [NSThread isMainThread]) {
      [ctx performSelectorOnMainThread:@selector(release) withObject:nil waitUntilDone:NO];
  } else {
      // would hangs for ~10s for 1 of a shared context set or offscreen context, set releaseOnMainThread=true !
      [ctx release]; 
  }
  [pool release];
  return true;
}

Bool flushBuffer(NSOpenGLContext* ctx) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx flushBuffer];
  [pool release];
  return true;
}

void setContextOpacity(NSOpenGLContext* ctx, int opacity) {
  [ctx setValues:&opacity forParameter:NSOpenGLCPSurfaceOpacity];
}

void updateContext(NSOpenGLContext* ctx) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx update];
  [pool release];
}

void copyContext(NSOpenGLContext* dest, NSOpenGLContext* src, int mask) {
  [dest copyAttributesFromContext: src withMask: mask];
}

void* updateContextRegister(NSOpenGLContext* ctx, NSView* view) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  ContextUpdater *contextUpdater = [[ContextUpdater alloc] initWithContext: ctx view: view];
  [pool release];
  return contextUpdater;
}

Bool updateContextNeedsUpdate(void* updater) {
  ContextUpdater *contextUpdater = (ContextUpdater *)updater;
  BOOL res;
    
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  res = [contextUpdater needsUpdate];
  [pool release];

  return ( res == TRUE ) ? true : false;
}

void updateContextUnregister(void* updater) {
  ContextUpdater *contextUpdater = (ContextUpdater *)updater;
    
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [contextUpdater release];
  [pool release];
}

NSOpenGLPixelBuffer* createPBuffer(int renderTarget, int internalFormat, int width, int height) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  NSOpenGLPixelBuffer* pBuffer = [[NSOpenGLPixelBuffer alloc]
                                   initWithTextureTarget:renderTarget
                                   textureInternalFormat:internalFormat
                                   textureMaxMipMapLevel:0
                                   pixelsWide:width
                                   pixelsHigh:height];
  [pool release];
  return pBuffer;
}

Bool destroyPBuffer(NSOpenGLPixelBuffer* pBuffer) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [pBuffer release];
  [pool release];
  return true;
}

void setContextPBuffer(NSOpenGLContext* ctx, NSOpenGLPixelBuffer* pBuffer) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx setPixelBuffer: pBuffer
             cubeMapFace: 0
             mipMapLevel: 0
             currentVirtualScreen: [ctx currentVirtualScreen]];
  [pool release];
}

void setContextTextureImageToPBuffer(NSOpenGLContext* ctx, NSOpenGLPixelBuffer* pBuffer, GLenum colorBuffer) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [ctx setTextureImageToPixelBuffer: pBuffer colorBuffer: colorBuffer];
  [pool release];
}

#include <mach-o/dyld.h>
Bool imagesInitialized = false;
static char libGLStr[] = "/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib";
static char libGLUStr[] = "/System/Library/Frameworks/OpenGL.framework/Libraries/libGLU.dylib";
static const struct mach_header *libGLImage;
static const struct mach_header *libGLUImage;
void* getProcAddress(const char *procname) {
  if (imagesInitialized == false) {
    imagesInitialized = true;
    unsigned long options = NSADDIMAGE_OPTION_RETURN_ON_ERROR;
    libGLImage = NSAddImage(libGLStr, options);
    libGLUImage = NSAddImage(libGLUStr, options);
  }
    
  unsigned long options = NSLOOKUPSYMBOLINIMAGE_OPTION_BIND | NSLOOKUPSYMBOLINIMAGE_OPTION_RETURN_ON_ERROR;
  char underscoreName[512] = "_";
  strcat(underscoreName, procname);
    
  if (NSIsSymbolNameDefinedInImage(libGLImage, underscoreName) == YES) {
    NSSymbol sym = NSLookupSymbolInImage(libGLImage, underscoreName, options);
    return NSAddressOfSymbol(sym);
  }
    
  if (NSIsSymbolNameDefinedInImage(libGLUImage, underscoreName) == YES)    {
    NSSymbol sym = NSLookupSymbolInImage(libGLUImage, underscoreName, options);
    return NSAddressOfSymbol(sym);
  }
    
  if (NSIsSymbolNameDefinedWithHint(underscoreName, "GL")) {
    NSSymbol sym = NSLookupAndBindSymbol(underscoreName);
    return NSAddressOfSymbol(sym);
  }
  
  return NULL;
}

void setSwapInterval(NSOpenGLContext* ctx, int interval) {
  GLint swapInterval = interval;
  [ctx setValues: &swapInterval forParameter: NSOpenGLCPSwapInterval];
}

Bool setGammaRamp(int tableSize, float* redRamp, float* greenRamp, float* blueRamp) {
  CGDisplayErr err = CGSetDisplayTransferByTable(kCGDirectMainDisplay, tableSize, redRamp, greenRamp, blueRamp);
  return (err == CGDisplayNoErr);
}

void resetGammaRamp() {
  CGDisplayRestoreColorSyncSettings();
}
