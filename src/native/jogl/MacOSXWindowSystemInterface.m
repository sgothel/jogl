#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>

typedef int Bool;

void* createContext(void* nsView, void* shareContext) {
  NSView *view = nsView;
  NSOpenGLContext *share = shareContext;

  // FIXME: hardcoded pixel format. Instead pass these attributes down
  // as arguments. There is really no way to enumerate the possible
  // pixel formats for a given window on Mac OS X, so we will assume
  // that we can match the requested capabilities and leave the
  // selection up to the built-in pixel format selection algorithm.
  GLuint attribs[] = {
    NSOpenGLPFANoRecovery,
    NSOpenGLPFAWindow,
    NSOpenGLPFAAccelerated,
    NSOpenGLPFADoubleBuffer,
    NSOpenGLPFAColorSize, 32,
    NSOpenGLPFAAlphaSize, 8,
    NSOpenGLPFADepthSize, 24,
    NSOpenGLPFAStencilSize, 8,
    NSOpenGLPFAAccumSize, 0,
    0
  };

  NSOpenGLPixelFormat* fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes: (NSOpenGLPixelFormatAttribute*) attribs];

  NSOpenGLContext* context = [[NSOpenGLContext alloc] initWithFormat: [fmt autorelease] shareContext: share];

  if (view != nil) {
    [context setView: view];

    [context makeCurrentContext];
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    [context update];
    [context flushBuffer];
  }

  return context;
}

Bool makeCurrentContext(void* nsView, void* nsContext) {
  NSOpenGLContext *context = nsContext;

  [context makeCurrentContext];
  return true;
}

Bool clearCurrentContext(void* nsView, void* nsContext) {
  NSView *view = nsView;
  NSOpenGLContext *context = nsContext;

  [NSOpenGLContext clearCurrentContext];
  return true;
}

void updateContext(void* nsView, void* nsContext) {
  NSView *view = nsView;
  NSOpenGLContext *context = nsContext;

  [context update];
}

Bool deleteContext(void* nsView, void* nsContext) {
  NSOpenGLContext *context = nsContext;

  [context setView: nil];
  [context release];
  return true;
}

Bool flushBuffer(void* nsView, void* nsContext) {
  NSOpenGLContext *context = nsContext;

  [context flushBuffer];
  return true;
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

  if (NSIsSymbolNameDefinedInImage(libGLUImage, underscoreName) == YES) {
    NSSymbol sym = NSLookupSymbolInImage(libGLUImage, underscoreName, options);
    return NSAddressOfSymbol(sym);
  }

  if (NSIsSymbolNameDefinedWithHint(underscoreName, "GL")) {
    NSSymbol sym = NSLookupAndBindSymbol(underscoreName);
    return NSAddressOfSymbol(sym);
  }

  return NULL;
}
