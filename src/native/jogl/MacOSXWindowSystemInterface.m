#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>
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

NSAutoreleasePool* gAutoreleasePool = NULL;

void* createContext(void* shareContext, void* view,
                    int doubleBuffer,
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
                    int numSamples)
{
        int colorSize = redBits + greenBits + blueBits;
        int accumSize = accumRedBits + accumGreenBits + accumBlueBits;
  
	NSOpenGLContext *nsChareCtx = (NSOpenGLContext*)shareContext;
	NSView *nsView = (NSView*)view;
	
        if (nsView != NULL)
        {
            NSRect frame = [nsView frame];
            if ((frame.size.width == 0) || (frame.size.height == 0))
            {
                fprintf(stderr, "Error: view width or height == 0at \"%s:%s:%d\"\n", __FILE__, __FUNCTION__, __LINE__);
                // the view is not ready yet
                return NULL;
            }
            else if ([nsView lockFocusIfCanDraw] == NO)
            {
                fprintf(stderr, "Error: view not ready, cannot lock focus at \"%s:%s:%d\"\n", __FILE__, __FUNCTION__, __LINE__);
                // the view is not ready yet
                return NULL;
            }
        }
                
	if (gAutoreleasePool == NULL)
	{
		gAutoreleasePool = [[NSAutoreleasePool alloc] init];
	}
	
	NSOpenGLPixelFormatAttribute attribs[] =
	{
		NSOpenGLPFANoRecovery, YES,
		NSOpenGLPFAAccelerated, YES,
		NSOpenGLPFADoubleBuffer, YES,
		NSOpenGLPFAColorSize, colorSize,
		NSOpenGLPFAAlphaSize, alphaBits,
		NSOpenGLPFADepthSize, depthBits,
		NSOpenGLPFAStencilSize, stencilBits,
		NSOpenGLPFAAccumSize, accumSize,
		NSOpenGLPFASampleBuffers, sampleBuffers,
		NSOpenGLPFASamples, numSamples,
		0
	};
	
	
	
	NSOpenGLPixelFormat* fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes:attribs];
	
	NSOpenGLContext* nsContext = [[NSOpenGLContext alloc] initWithFormat:fmt shareContext:nsChareCtx];
	
	[fmt release];
        
	if (nsView != nil)
	{
		[nsContext setView:nsView];
                
		[nsView unlockFocus];		
	}
                
	[nsContext retain];
	
//fprintf(stderr, "	nsContext=%p\n", nsContext);
	return nsContext;
}

Bool makeCurrentContext(void* context, void* view)
{
//fprintf(stderr, "makeCurrentContext context=%p, view=%p\n", context, view);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
	[nsContext makeCurrentContext];
	return true;
}

Bool clearCurrentContext(void* context, void* view)
{
//fprintf(stderr, "clearCurrentContext context=%p, view=%p\n", context, view);
	[NSOpenGLContext clearCurrentContext];
	return true;
}

Bool deleteContext(void* context, void* view)
{
//fprintf(stderr, "deleteContext context=%p, view=%p\n", context, view);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
	[nsContext clearDrawable];
	[nsContext release];
	return true;
}

Bool flushBuffer(void* context, void* view)
{
//fprintf(stderr, "flushBuffer context=%p, view=%p\n", context, view);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
	[nsContext flushBuffer];
	return true;
}

void updateContext(void* context, void* view)
{
//fprintf(stderr, "updateContext context=%p, view=%p\n", context, view);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
	[nsContext update];
}

void* updateContextRegister(void* context, void* view)
{
//fprintf(stderr, "updateContextRegister context=%p, view=%p\n", context, view);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	NSView *nsView = (NSView*)view;
	
	ContextUpdater *contextUpdater = [[ContextUpdater alloc] init];
	[contextUpdater registerFor:nsContext with:nsView];
        return NULL;
}

void updateContextUnregister(void* context, void* view, void* updater)
{
//fprintf(stderr, "updateContextUnregister context=%p, view=%p\n", context, view);
	ContextUpdater *contextUpdater = (ContextUpdater *)updater;
	
	[contextUpdater release];
}

void* createPBuffer(int renderTarget, int width, int height)
{
  //  fprintf(stderr, "createPBuffer renderTarget=%d width=%d height=%d\n", renderTarget, width, height);

  NSOpenGLPixelBuffer* pBuffer = [[NSOpenGLPixelBuffer alloc] initWithTextureTarget:renderTarget textureInternalFormat:GL_RGBA textureMaxMipMapLevel:0 pixelsWide:width pixelsHigh:height];

  return pBuffer;
}

Bool destroyPBuffer(void* context, void* buffer)
{
//fprintf(stderr, "destroyPBuffer context=%p, buffer=%p\n", context, buffer);
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;
	
        if (nsContext != NULL)
        {
            [nsContext clearDrawable];
        }
	[pBuffer release];
	
	return true;
}

void setContextPBuffer(void* context, void* buffer) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;

  [nsContext setPixelBuffer: pBuffer cubeMapFace: 0 mipMapLevel: 0 currentVirtualScreen: [nsContext currentVirtualScreen]];
}

void setContextTextureImageToPBuffer(void* context, void* buffer, int colorBuffer) {
  NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
  NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;

  [nsContext setTextureImageToPixelBuffer: pBuffer colorBuffer: (unsigned long) colorBuffer];
}

#include <mach-o/dyld.h>
Bool imagesInitialized = false;
static char libGLStr[] = "/System/Library/Frameworks/OpenGL.framework/Libraries/libGL.dylib";
static char libGLUStr[] = "/System/Library/Frameworks/OpenGL.framework/Libraries/libGLU.dylib";
static const struct mach_header *libGLImage;
static const struct mach_header *libGLUImage;
void* getProcAddress(const char *procname)
{
	if (imagesInitialized == false)
	{
		imagesInitialized = true;
		unsigned long options = NSADDIMAGE_OPTION_RETURN_ON_ERROR;
		libGLImage = NSAddImage(libGLStr, options);
		libGLUImage = NSAddImage(libGLUStr, options);
	}
	
	unsigned long options = NSLOOKUPSYMBOLINIMAGE_OPTION_BIND | NSLOOKUPSYMBOLINIMAGE_OPTION_RETURN_ON_ERROR;
	char underscoreName[512] = "_";
	strcat(underscoreName, procname);
	
	if (NSIsSymbolNameDefinedInImage(libGLImage, underscoreName) == YES)
	{
		NSSymbol sym = NSLookupSymbolInImage(libGLImage, underscoreName, options);
		return NSAddressOfSymbol(sym);
	}
	
	if (NSIsSymbolNameDefinedInImage(libGLUImage, underscoreName) == YES)
	{
		NSSymbol sym = NSLookupSymbolInImage(libGLUImage, underscoreName, options);
		return NSAddressOfSymbol(sym);
	}
	
	if (NSIsSymbolNameDefinedWithHint(underscoreName, "GL")) 
	{
		NSSymbol sym = NSLookupAndBindSymbol(underscoreName);
		return NSAddressOfSymbol(sym);
	}
	
	return NULL;
}
