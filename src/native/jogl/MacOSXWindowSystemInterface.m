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

void* createContext( JNIEnv* env, jobject glCapabilities, void* shareContext, void* view)
{
        // fprintf( stderr, "Creating context \n");

	jclass clazz = (*env)->GetObjectClass( env, glCapabilities );
		
	
	jfieldID redSizeField = (*env)->GetFieldID( env, clazz, "redBits" , "I" );
	jfieldID greenSizeField = (*env)->GetFieldID( env, clazz, "greenBits" , "I" );
	jfieldID blueSizeField = (*env)->GetFieldID( env, clazz, "blueBits" , "I" );
	
	jfieldID alphaSizeField = (*env)->GetFieldID( env, clazz, "alphaBits", "I" );
	jfieldID depthSizeField = (*env)->GetFieldID( env, clazz, "depthBits", "I" );
	jfieldID stencilSizeField = (*env)->GetFieldID( env, clazz, "stencilBits", "I" );
	jfieldID accumRedSizeField = (*env)->GetFieldID( env, clazz, "accumRedBits", "I" );
	jfieldID accumGreenSizeField = (*env)->GetFieldID( env, clazz, "accumGreenBits", "I" );
	jfieldID accumBlueSizeField = (*env)->GetFieldID( env, clazz, "accumBlueBits", "I" );
	jfieldID accumAlphaSizeField = (*env)->GetFieldID( env, clazz, "accumAlphaBits", "I" );

	jint redSize = (*env)->GetIntField( env, glCapabilities, redSizeField );
	jint greenSize = (*env)->GetIntField( env, glCapabilities, greenSizeField );
	jint blueSize = (*env)->GetIntField( env, glCapabilities, blueSizeField );	
	jint colorSize = redSize + greenSize + blueSize;
	
	jint accumRedSize = (*env)->GetIntField( env, glCapabilities, accumRedSizeField );
	jint accumGreenSize = (*env)->GetIntField( env, glCapabilities, accumGreenSizeField );
	jint accumBlueSize = (*env)->GetIntField( env, glCapabilities, accumBlueSizeField );
	jint accumAlphaSize = (*env)->GetIntField( env, glCapabilities, accumAlphaSizeField );
	jint accumSize = accumRedSize + accumGreenSize + accumBlueSize + accumAlphaSize; 
	
	
	jint alphaSize = (*env)->GetIntField( env, glCapabilities, alphaSizeField );
	jint depthSize = (*env)->GetIntField( env, glCapabilities, depthSizeField );
	jint stencilSize = (*env)->GetIntField( env, glCapabilities, stencilSizeField );
	
	// fprintf(stderr, "Color %d, alpha %d, depth %d, stencil %d, accum %d", colorSize, alphaSize, depthSize, stencilSize, accumSize );
	
	
//fprintf(stderr, "createContext shareContext=%p view=%p\n", shareContext, view);
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
	
	// FIXME: hardcoded pixel format. Instead pass these attributes down
	// as arguments. There is really no way to enumerate the possible
	// pixel formats for a given window on Mac OS X, so we will assume
	// that we can match the requested capabilities and leave the
	// selection up to the built-in pixel format selection algorithm.
	
	NSOpenGLPixelFormatAttribute attribs[] =
	{
		NSOpenGLPFANoRecovery, YES,
		NSOpenGLPFAAccelerated, YES,
		NSOpenGLPFADoubleBuffer, YES,
		NSOpenGLPFAColorSize, colorSize,
		NSOpenGLPFAAlphaSize, alphaSize,
		NSOpenGLPFADepthSize, depthSize,
		NSOpenGLPFAStencilSize, stencilSize,
		NSOpenGLPFAAccumSize, accumSize,
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
}

void updateContextUnregister(void* context, void* view, void* updater)
{
//fprintf(stderr, "updateContextUnregister context=%p, view=%p\n", context, view);
	ContextUpdater *contextUpdater = (ContextUpdater *)updater;
	
	[contextUpdater release];
}

#ifndef USE_GL_TEXTURE_RECTANGLE_EXT
static int getNextPowerOf2(int number)
{
	if (((number-1) & number) == 0)
	{
		//ex: 8 -> 0b1000; 8-1=7 -> 0b0111; 0b1000&0b0111 == 0
		return number;
	}	
    int power = 0;
    while (number > 0)
    {
        number = number>>1;
        power++;
    }
    return (1<<power);
}
#endif

void* createPBuffer(void* context, int width, int height)
{
///fprintf(stderr, "createPBuffer context=%p width=%d height=%d\n", context, width, height);
#ifdef AVAILABLE_MAC_OS_X_VERSION_10_3_AND_LATER)
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	
#ifdef USE_GL_TEXTURE_RECTANGLE_EXT
	unsigned long taget = GL_TEXTURE_RECTANGLE_EXT;
#else
	unsigned long taget = GL_TEXTURE_2D; // texture size must be a multiple of power of 2
	
        width = getNextPowerOf2(width);
        height = getNextPowerOf2(height);
#endif
        
	NSOpenGLPixelBuffer* pBuffer = [[NSOpenGLPixelBuffer alloc] initWithTextureTarget:taget textureInternalFormat:GL_RGBA textureMaxMipMapLevel:0 pixelsWide:width pixelsHigh:height];
	
	[nsContext setPixelBuffer:pBuffer cubeMapFace:0 mipMapLevel:0 currentVirtualScreen:0];
	[nsContext update];
        
	return pBuffer;
#else
	return NULL;
#endif
}

Bool destroyPBuffer(void* context, void* buffer)
{
//fprintf(stderr, "destroyPBuffer context=%p, buffer=%p\n", context, buffer);
#ifdef AVAILABLE_MAC_OS_X_VERSION_10_3_AND_LATER)
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;
	
        if (nsContext != NULL)
        {
            [nsContext clearDrawable];
        }
	[pBuffer release];
	
	return true;
#else
	return false;
#endif
}

int bindPBuffer(void* context, void* buffer)
{
//fprintf(stderr, "bindPBuffer context=%p, buffer=%p\n", context, buffer);
#ifdef AVAILABLE_MAC_OS_X_VERSION_10_3_AND_LATER)
	NSOpenGLContext *nsContext = (NSOpenGLContext*)context;
	NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;
        
        glPushMatrix();
        glPushAttrib(GL_CURRENT_BIT|GL_ENABLE_BIT|GL_TEXTURE_BIT|GL_TRANSFORM_BIT);
        
	GLuint pBufferTextureName;
	glGenTextures(1, &pBufferTextureName);
	
	glBindTexture([pBuffer textureTarget], pBufferTextureName);
	
	glTexParameteri([pBuffer textureTarget], GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	glTexParameteri([pBuffer textureTarget], GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	
	[nsContext setTextureImageToPixelBuffer:pBuffer colorBuffer:GL_FRONT_LEFT];
	
	glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);
	glEnable([pBuffer textureTarget]);
	
#ifdef USE_GL_TEXTURE_RECTANGLE_EXT
        //GLint matrixMode;
        //glGetIntegerv(GL_MATRIX_MODE, &matrixMode);
        glMatrixMode(GL_TEXTURE);
        glLoadIdentity();
        glScalef([pBuffer pixelsWide], [pBuffer pixelsHigh], 1.0f); // GL_TEXTURE_RECTANGLE_EXT wants texture coordinates in texture image space (not 0 to 1)
        //glMatrixMode(matrixMode);
#endif
        
	return pBufferTextureName;
#else
	return 0;
#endif
}

void unbindPBuffer(void* context, void* buffer, int texture)
{
//fprintf(stderr, "unbindPBuffer context=%p, buffer=%p\n", context, buffer);
#ifdef AVAILABLE_MAC_OS_X_VERSION_10_3_AND_LATER)
	NSOpenGLPixelBuffer *pBuffer = (NSOpenGLPixelBuffer*)buffer;
	GLuint pBufferTextureName = (GLuint)texture;
	
	glDeleteTextures(1, &pBufferTextureName);
        
        glPopAttrib();
        glPopMatrix();
#endif
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
