#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>
#import <OpenGL/CGLTypes.h>
#import <jni.h>
#import "ContextUpdater.h"

// see MacOSXPbufferGLContext.java createPbuffer
#define USE_GL_TEXTURE_RECTANGLE_EXT

#ifdef USE_GL_TEXTURE_RECTANGLE_EXT
    #ifndef GL_TEXTURE_RECTANGLE_EXT
            #define GL_TEXTURE_RECTANGLE_EXT 0x84F5
    #endif
#endif

// kCGLPFAColorFloat is equivalent to NSOpenGLPFAColorFloat, but the
// latter is only available on 10.4 and we need to compile under
// 10.3
#ifndef NSOpenGLPFAColorFloat
    #define NSOpenGLPFAColorFloat kCGLPFAColorFloat
#endif


typedef int Bool;

static Bool rendererInfoInitialized = false;
static int bufferDepthsLength = 17;
static int bufferDepths[] = {kCGL128Bit, kCGL96Bit, kCGL64Bit, kCGL48Bit, kCGL32Bit, kCGL24Bit, kCGL16Bit, kCGL12Bit, kCGL10Bit, kCGL8Bit, kCGL6Bit, kCGL5Bit, kCGL4Bit, kCGL3Bit, kCGL2Bit, kCGL1Bit, kCGL0Bit};
static int bufferDepthsBits[] = {128, 96, 64, 48, 32, 24, 16, 12, 10, 8, 6, 5, 4, 3, 2, 1, 0};
static int accRenderID = 0; // the ID of the accelerated renderer
static int maxColorSize = kCGL128Bit; // max depth of color buffer
static int maxDepthSize = kCGL128Bit; // max depth of depth buffer
static int maxAccumSize = kCGL128Bit; // max depth of accum buffer
static int maxStencilSize = kCGL128Bit; // max depth of stencil buffer
void getRendererInfo()
{
	if (rendererInfoInitialized == false)
	{
		rendererInfoInitialized = true;
		
		CGLRendererInfoObj info;
		long numRenderers = 0;
		CGLError err = CGLQueryRendererInfo(CGDisplayIDToOpenGLDisplayMask(kCGDirectMainDisplay), &info, &numRenderers);
		if (err == kCGLNoError)
		{
			CGLDescribeRenderer(info, 0, kCGLRPRendererCount, &numRenderers);
			long j;
			for (j=0; j<numRenderers; j++)
			{
				unsigned long accRenderer = 0;
				CGLDescribeRenderer (info, j, kCGLRPAccelerated, &accRenderer);
				if (accRenderer != 0)
				{
					// get the accelerated renderer ID
					CGLDescribeRenderer(info, j, kCGLRPRendererID, &accRenderID);
					
					// get the max color buffer depth
					unsigned long colorModes = 0;
					CGLDescribeRenderer(info, j, kCGLRPColorModes, &colorModes);
					int i;
					for (i=0; i<bufferDepthsLength; i++)
					{
						if ((colorModes & bufferDepths[i]) != 0)
						{
							maxColorSize = bufferDepthsBits[i];
							break;
						}
					}
					
					// get the max depth buffer depth
					unsigned long depthModes = 0;
					CGLDescribeRenderer(info, j, kCGLRPDepthModes, &depthModes);
					for (i=0; i<bufferDepthsLength; i++)
					{
						if ((depthModes & bufferDepths[i]) != 0)
						{
							maxDepthSize = bufferDepthsBits[i];
							break;
						}
					}
					
					// get the max accum buffer depth
					unsigned long accumModes = 0;
					CGLDescribeRenderer(info, j, kCGLRPAccumModes, &accumModes);
					for (i=0; i<bufferDepthsLength; i++)
					{
						if ((accumModes & bufferDepths[i]) != 0)
						{
							maxAccumSize = bufferDepthsBits[i];
							break;
						}
					}
					
					// get the max stencil buffer depth
					unsigned long stencilModes = 0;
					CGLDescribeRenderer(info, j, kCGLRPStencilModes, &stencilModes);
					for (i=0; i<bufferDepthsLength; i++)
					{
						if ((stencilModes & bufferDepths[i]) != 0)
						{
							maxStencilSize = bufferDepthsBits[i];
							break;
						}
					}
					
					break;
				}
			}
			//fprintf(stderr, "maxColorSize=%d\n", maxColorSize);
			//fprintf(stderr, "maxDepthSize=%d\n", maxDepthSize);
			//fprintf(stderr, "maxAccumSize=%d\n", maxAccumSize);
			//fprintf(stderr, "maxStencilSize=%d\n", maxStencilSize);
		}
		CGLDestroyRendererInfo (info);
	}
}

void* createContext(void* shareContext, void* view,
                    int doubleBuffer,
                    int stereo,
                    int redBits,
                    int greenBits,
                    int blueBits,
                    int alphaBits,
                    int depthBits,
                    int stencilBits,
                    int accumRedBits,
                    int accumGreenBits,
                    int accumBlueBits,
                    int accumAlphaBits,
                    int sampleBuffers,
                    int numSamples,
                    int pbuffer,
                    int floatingPoint,
                    int* viewNotReady)
{
  getRendererInfo();
  
  int colorSize = redBits + greenBits + blueBits;
  if (colorSize > maxColorSize) {
	colorSize = maxColorSize;
  }
  
  int accumSize = accumRedBits + accumGreenBits + accumBlueBits;
  if (accumSize > maxAccumSize) {
	accumSize = maxAccumSize;
  }
  
  if (depthBits > maxDepthSize) {
	depthBits = maxDepthSize;
  }
  
  if (stencilBits > maxStencilSize) {
	stencilBits = maxStencilSize;
  }
  
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];

  NSOpenGLContext *nsChareCtx = (NSOpenGLContext*)shareContext;
  NSView *nsView = (NSView*)view;
	
  if (nsView != NULL) {
    Bool viewReady = true;
    
    if ([nsView lockFocusIfCanDraw] == NO) {
      viewReady = false;
    } else {
      NSRect frame = [nsView frame];
      if ((frame.size.width == 0) || (frame.size.height == 0)) {
        [nsView unlockFocus];		
        viewReady = false;
      }
    }

    if (!viewReady) {
      if (viewNotReady != NULL) {
        *viewNotReady = 1;
      }
            
      // the view is not ready yet
      [pool release];
      return NULL;
    }
  }

  NSOpenGLPixelFormatAttribute attribs[256];
  int idx = 0;
  if (pbuffer)       attribs[idx++] = NSOpenGLPFAPixelBuffer;
  if (floatingPoint) attribs[idx++] = NSOpenGLPFAColorFloat;
  if (doubleBuffer)  attribs[idx++] = NSOpenGLPFADoubleBuffer;
  if (stereo)        attribs[idx++] = NSOpenGLPFAStereo;
  attribs[idx++] = NSOpenGLPFAColorSize;     attribs[idx++] = colorSize;
  attribs[idx++] = NSOpenGLPFAAlphaSize;     attribs[idx++] = alphaBits;
  attribs[idx++] = NSOpenGLPFADepthSize;     attribs[idx++] = depthBits;
  attribs[idx++] = NSOpenGLPFAStencilSize;   attribs[idx++] = stencilBits;
  attribs[idx++] = NSOpenGLPFAAccumSize;     attribs[idx++] = accumSize;
  if (sampleBuffers != 0) {
    attribs[idx++] = NSOpenGLPFASampleBuffers; attribs[idx++] = sampleBuffers;
    attribs[idx++] = NSOpenGLPFASamples;       attribs[idx++] = numSamples;
  }
  attribs[idx++] = 0;
	
  NSOpenGLPixelFormat* fmt = [[NSOpenGLPixelFormat alloc]
                               initWithAttributes:attribs];
  NSOpenGLContext* nsContext = [[NSOpenGLContext alloc]
                                 initWithFormat:fmt
                                 shareContext:nsChareCtx];
  [fmt release];
        
  if (nsView != nil) {
    [nsContext setView:nsView];
    [nsView unlockFocus];		
  }

  [pool release];
  return nsContext;
}

Bool makeCurrentContext(void* context, void* view) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext makeCurrentContext];
  [pool release];
  return true;
}

Bool clearCurrentContext(void* context, void* view) {
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [NSOpenGLContext clearCurrentContext];
  [pool release];
  return true;
}

Bool deleteContext(void* context, void* view) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext clearDrawable];
  [nsContext release];
  [pool release];
  return true;
}

Bool flushBuffer(void* context, void* view) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext flushBuffer];
  [pool release];
  return true;
}

void updateContext(void* context, void* view) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext update];
  [pool release];
}

void* updateContextRegister(void* context, void* view) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  NSView *nsView = (NSView*)view;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  ContextUpdater *contextUpdater = [[ContextUpdater alloc] init];
  [contextUpdater registerFor:nsContext with:nsView];
  [pool release];
  return NULL;
}

void updateContextUnregister(void* context, void* view, void* updater) {
  ContextUpdater *contextUpdater = (ContextUpdater *)updater;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [contextUpdater release];
  [pool release];
}

void* createPBuffer(int renderTarget, int internalFormat, int width, int height) {
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

Bool destroyPBuffer(void* context, void* buffer) {
  /* FIXME: not clear whether we need to perform the clearDrawable below */
  /* FIXME: remove the context argument -- don't need it any more */
  /*  NSOpenGLContext *nsContext = (NSOpenGLContext*)context; */
  NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;
	
  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  /*
  if (nsContext != NULL) {
    [nsContext clearDrawable];
  }
  */
  [pBuffer release];
  [pool release];
	
  return true;
}

void setContextPBuffer(void* context, void* buffer) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;

  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext setPixelBuffer: pBuffer
             cubeMapFace: 0
             mipMapLevel: 0
             currentVirtualScreen: [nsContext currentVirtualScreen]];
  [pool release];
}

void setContextTextureImageToPBuffer(void* context, void* buffer, int colorBuffer) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;

  NSAutoreleasePool* pool = [[NSAutoreleasePool alloc] init];
  [nsContext setTextureImageToPixelBuffer: pBuffer
             colorBuffer: (unsigned long) colorBuffer];
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
	
  if (NSIsSymbolNameDefinedInImage(libGLUImage, underscoreName) == YES)	{
    NSSymbol sym = NSLookupSymbolInImage(libGLUImage, underscoreName, options);
    return NSAddressOfSymbol(sym);
  }
	
  if (NSIsSymbolNameDefinedWithHint(underscoreName, "GL")) {
    NSSymbol sym = NSLookupAndBindSymbol(underscoreName);
    return NSAddressOfSymbol(sym);
  }
  
  return NULL;
}

void setSwapInterval(void* context, int interval) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  long swapInterval = interval;
  [nsContext setValues: &swapInterval forParameter: NSOpenGLCPSwapInterval];
}
