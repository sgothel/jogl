/* C routines encapsulating small amounts of Objective C code to allow
   nsContext creation and manipulation to occur from Java */

typedef int Bool;

void* createContext(void* shareContext, void* nsView,
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
                    int numSamples);
Bool  makeCurrentContext(void* nsContext, void* nsView);
Bool  clearCurrentContext(void* nsContext, void* nsView);
Bool  deleteContext(void* nsContext, void* nsView);
Bool  flushBuffer(void* nsContext, void* nsView);
void  updateContext(void* nsContext, void* nsView);

void* updateContextRegister(void* nsContext, void* nsView);
void  updateContextUnregister(void* nsContext, void* nsView, void* updater);

void* createPBuffer(int renderTarget, int width, int height);
Bool destroyPBuffer(void* nsContext, void* pBuffer);
void setContextPBuffer(void* nsContext, void* pBuffer);
void setContextTextureImageToPBuffer(void* nsContext, void* pBuffer, int colorBuffer);

void* getProcAddress(const char *procName);
