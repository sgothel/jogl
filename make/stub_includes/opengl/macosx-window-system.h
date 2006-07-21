/* C routines encapsulating small amounts of Objective C code to allow
   nsContext creation and manipulation to occur from Java

   It's unfortunate this couldn't be placed in the macosx
   stub_includes directory, but due to the presence of the jni.h stub
   headers in that directory, if that is in the include path during
   compilation then the build fails.
*/

typedef int Bool;

void* createContext(void* shareContext, void* nsView,
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
                    int* viewNotReady);
Bool  makeCurrentContext(void* nsContext);
Bool  clearCurrentContext(void* nsContext);
Bool  deleteContext(void* nsContext);
Bool  flushBuffer(void* nsContext);
void  updateContext(void* nsContext);

void* updateContextRegister(void* nsContext, void* nsView);
void  updateContextUnregister(void* nsContext, void* nsView, void* updater);

void* createPBuffer(int renderTarget, int internalFormat, int width, int height);
Bool destroyPBuffer(void* nsContext, void* pBuffer);
void setContextPBuffer(void* nsContext, void* pBuffer);
void setContextTextureImageToPBuffer(void* nsContext, void* pBuffer, int colorBuffer);

void* getProcAddress(const char *procName);

void setSwapInterval(void* nsContext, int interval);

/* Gamma-related functionality */
Bool setGammaRamp(int tableSize, float* redRamp, float* greenRamp, float* blueRamp);
void resetGammaRamp();

/****************************************************************************************/
/* Java2D/JOGL bridge support; need to be able to create pbuffers and
   contexts using the CGL APIs to be able to share textures, etc. with
   contexts created by Java2D/JOGL bridge, which are CGLContextObjs */

/* Pick up copies of CGL signatures from Mac OS X stub_includes/window-system-build directory */
#include <OpenGL/OpenGL.h>
