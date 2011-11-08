/* C routines encapsulating small amounts of Objective C code to allow
   nsContext creation and manipulation to occur from Java

   It's unfortunate this couldn't be placed in the macosx
   stub_includes directory, but due to the presence of the jni.h stub
   headers in that directory, if that is in the include path during
   compilation then the build fails.
*/

#include <AppKit/NSView.h>
#include <AppKit/NSOpenGL.h>
#include <AppKit/NSOpenGLView.h>
#include <AppKit/NSOpenGLLayer.h>
#include <OpenGL/CGLDevice.h>
#include <OpenGL/OpenGL.h>

typedef int Bool;

// CGL ..
void CGLQueryPixelFormat(CGLPixelFormatObj fmt, int* iattrs, int niattrs, int* ivalues);

// NS ..
NSOpenGLPixelFormat* createPixelFormat(int* iattrs, int niattrs, int* ivalues);
void queryPixelFormat(NSOpenGLPixelFormat* fmt, int* iattrs, int niattrs, int* ivalues);
void deletePixelFormat(NSOpenGLPixelFormat* fmt);

// NS ..
NSOpenGLContext* getCurrentContext(void);
CGLContextObj getCGLContext(NSOpenGLContext* ctx);
NSView* getNSView(NSOpenGLContext* ctx);

NSOpenGLContext* createContext(NSOpenGLContext* shareContext,
                    NSView* nsView,
                    Bool isBackingLayerView,
                    NSOpenGLPixelFormat* pixelFormat,
                    Bool opaque,
                    int* viewNotReady);
Bool  makeCurrentContext(NSOpenGLContext* ctx);
Bool  clearCurrentContext(NSOpenGLContext *ctx);
Bool  deleteContext(NSOpenGLContext* ctx, Bool releaseOnMainThread);
Bool  flushBuffer(NSOpenGLContext* ctx);
void  setContextOpacity(NSOpenGLContext* ctx, int opacity);
void  updateContext(NSOpenGLContext* ctx);
void  copyContext(NSOpenGLContext* dest, NSOpenGLContext* src, int mask);

void* updateContextRegister(NSOpenGLContext* ctx, NSView* view);
Bool updateContextNeedsUpdate(void* updater);
void  updateContextUnregister(void* updater);

NSOpenGLPixelBuffer* createPBuffer(int renderTarget, int internalFormat, int width, int height);
Bool destroyPBuffer(NSOpenGLPixelBuffer* pBuffer);
void setContextPBuffer(NSOpenGLContext* ctx, NSOpenGLPixelBuffer* pBuffer);
void setContextTextureImageToPBuffer(NSOpenGLContext* ctx, NSOpenGLPixelBuffer* pBuffer, GLenum colorBuffer);

// NSOpenGLLayer* createNSOpenGLLayer(NSOpenGLContext* ctx, NSOpenGLPixelFormat* fmt, NSView* view, Bool opaque);
NSOpenGLLayer* createNSOpenGLLayer(NSOpenGLContext* ctx, NSOpenGLPixelFormat* fmt, NSOpenGLPixelBuffer* pbuffer, Bool opaque, int texWidth, int texHeight);
void setNSOpenGLLayerSwapInterval(NSOpenGLLayer* layer, int interval);
void waitUntilNSOpenGLLayerIsReady(NSOpenGLLayer* layer, long to_ms);
void setNSOpenGLLayerNeedsDisplay(NSOpenGLLayer* glLayer);
void releaseNSOpenGLLayer(NSOpenGLLayer *glLayer);

void* getProcAddress(const char *procName);

void setSwapInterval(NSOpenGLContext* ctx, int interval);

/* Gamma-related functionality */
Bool setGammaRamp(int tableSize, float* redRamp, float* greenRamp, float* blueRamp);
void resetGammaRamp();

