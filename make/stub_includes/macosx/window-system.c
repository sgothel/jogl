/* C routines encapsulating small amounts of Objective C code to allow
   nsContext creation and manipulation to occur from Java */

#include <jni.h>
typedef int Bool;

void* createContext(JNIEnv* env, jobject glCapabilities, void* shareContext, void* nsView);
Bool  makeCurrentContext(void* nsContext, void* nsView);
Bool  clearCurrentContext(void* nsContext, void* nsView);
Bool  deleteContext(void* nsContext, void* nsView);
Bool  flushBuffer(void* nsContext, void* nsView);
void  updateContext(void* nsContext, void* nsView);

void* updateContextRegister(void* nsContext, void* nsView);
void  updateContextUnregister(void* nsContext, void* nsView, void* updater);

void* createPBuffer(void* nsContext, int width, int height);
Bool destroyPBuffer(void* nsContext, void* pBuffer);
int bindPBuffer(void* nsContext, void* pBuffer);
void unbindPBuffer(void* nsContext, void* pBuffer, int pBufferTextureName);

void* getProcAddress(const char *procName);
