/* C routines encapsulating small amounts of Objective C code to allow
   nsContext creation and manipulation to occur from Java */

typedef int Bool;

void* createContext(void* nsView, void* shareContext);
Bool  makeCurrentContext(void* nsView, void* nsContext);
Bool  clearCurrentContext(void* nsView, void* nsContext);
void  updateContext(void* nsView, void* nsContext);
Bool  deleteContext(void* nsView, void* nsContext);
Bool  flushBuffer(void* nsView, void* nsContext);

void* getProcAddress(const char *procName);
