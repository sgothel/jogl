#import <Cocoa/Cocoa.h>
#import <OpenGL/gl.h>

typedef int Bool;

void* createContext(void* nsView) {
  NSView *view = nsView;
        
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
    
  NSRect rect = [view frame];
    
  NSOpenGLPixelFormat* fmt = [[NSOpenGLPixelFormat alloc] initWithAttributes: (NSOpenGLPixelFormatAttribute*) attribs]; 
    
  NSOpenGLContext* context = [[NSOpenGLContext alloc] initWithFormat: [fmt autorelease] shareContext: nil];
    
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
void* getProcAddress(const char *procname) {
  void *funcPtr = NULL;
  static char underscoreName[256];
  strcpy(underscoreName, "_");
  strcat(underscoreName, procname);
  if (NSIsSymbolNameDefined(underscoreName)) {
    NSSymbol sym = NSLookupAndBindSymbol(underscoreName);
    funcPtr = (void *)NSAddressOfSymbol(sym);
  }
  return funcPtr;
}
