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

typedef int Bool;

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
  int colorSize = redBits + greenBits + blueBits;
  int accumSize = accumRedBits + accumGreenBits + accumBlueBits;
  
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
  // kCGLPFAColorFloat is equivalent to NSOpenGLPFAColorFloat, but the
  // latter is only available on 10.4 and we need to compile under
  // 10.3
  if (floatingPoint) attribs[idx++] = kCGLPFAColorFloat;
  if (doubleBuffer)  attribs[idx++] = NSOpenGLPFADoubleBuffer;
  if (stereo)        attribs[idx++] = NSOpenGLPFAStereo;
  attribs[idx++] = NSOpenGLPFAColorSize;     attribs[idx++] = colorSize;
  attribs[idx++] = NSOpenGLPFAAlphaSize;     attribs[idx++] = alphaBits;
  attribs[idx++] = NSOpenGLPFADepthSize;     attribs[idx++] = depthBits;
  attribs[idx++] = NSOpenGLPFAStencilSize;   attribs[idx++] = stencilBits;
  attribs[idx++] = NSOpenGLPFAAccumSize;     attribs[idx++] = accumSize;
  attribs[idx++] = NSOpenGLPFASampleBuffers; attribs[idx++] = sampleBuffers;
  attribs[idx++] = NSOpenGLPFASamples;       attribs[idx++] = numSamples;
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
